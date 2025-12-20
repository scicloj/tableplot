;; # AlgebraOfGraphics + thi.ng/geom: Architecture Guide
;;
;; **A comprehensive guide to Tableplot's AoG API and thi.ng/geom backend**
;;
;; This notebook explores the complete architecture of the AlgebraOfGraphics (AoG) API
;; and its thi.ng/geom backend, focusing on the four-layer pipeline and internal
;; representation layers.

(ns tableplot-book.aog-architecture
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.ir :as ir]
            [scicloj.tableplot.v1.aog.processing :as processing]
            [scicloj.tableplot.v1.aog.thing-geom :as tg]
            [scicloj.tableplot.v1.aog.scales :as scales]
            [scicloj.tableplot.v1.aog.transforms :as transforms]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]))

;; ## Overview
;;
;; The AoG API provides a **grammar of graphics** for Clojure, inspired by Julia's
;; AlgebraOfGraphics.jl. The system separates **what to plot** (the grammar) from
;; **how to render it** (the backend).
;;
;; Key innovations:
;;
;; 1. **Algebraic composition** - Use `*` (merge) and `+` (overlay) to build visualizations
;; 2. **Backend-agnostic IR** - Entry format works with Vega-Lite, Plotly.js, or thi.ng/geom
;; 3. **Pure Clojure rendering** - thi.ng/geom backend needs no JavaScript
;; 4. **Malli validation** - Every layer is schema-validated for correctness

;; ## Why thi.ng/geom?
;;
;; The thi.ng/geom backend offers unique advantages:
;;
;; - ✅ **Pure Clojure** - No JavaScript runtime required
;; - ✅ **Native polar coordinates** - True polar rendering (not simulated)
;; - ✅ **SVG output** - Static, embeddable graphics for papers/reports
;; - ✅ **Mathematical precision** - Excellent for parametric curves
;; - ✅ **Server-side rendering** - Generate visualizations without a browser
;;
;; **Best for**: Scientific papers, polar plots, embedded SVGs, server-side rendering
;;
;; **Not ideal for**: Interactive dashboards, 3D visualizations, real-time updates

;; # Architecture: The Four-Layer Pipeline
;;
;; The system uses a four-layer architecture that cleanly separates concerns:
;;
;; ```
;; Layer 1: USER API (AlgebraOfGraphics)
;;   • data(dataset), mapping(:x :y), scatter({:alpha 0.5})
;;   • * (multiply) - Merge specifications
;;   • + (add) - Overlay layers
;;          ↓
;; Layer 2: INTERMEDIATE REPRESENTATION (IR)
;;   • Layer - User-facing spec (core schema)
;;   • ProcessedLayer - After transformations + grouping
;;   • Entry - Backend-agnostic plot spec (THE IR!)
;;   • AxisEntries - Entry + scales + axis config
;;          ↓
;; Layer 3: PROCESSING PIPELINE
;;   • normalize-data, apply-transform, extract-column
;;   • identify-categorical, generate-labels
;;   • layer->processed-layer, processed-layer->entry
;;          ↓
;; Layer 4: BACKEND RENDERING (thi.ng/geom)
;;   • entry->thing-spec, build-axis, positional->thing
;;   • svg-plot2d-cartesian or svg-plot2d-polar
;;   • Generate SVG + Kindly wrap
;; ```

;; # Part 1: User API (AlgebraOfGraphics)

;; ## Core Constructors

;; ### data(dataset)
;;
;; Attach data to a layer. Accepts multiple formats.

;; Map of vectors
(def data-map {:x [1 2 3 4 5]
               :y [2 4 3 5 6]})

(def layer-with-data
  (aog/data data-map))

;; Inspect the layer structure
layer-with-data

;; Vector of maps
(def data-vec
  [{:x 1 :y 2}
   {:x 2 :y 4}
   {:x 3 :y 3}
   {:x 4 :y 5}
   {:x 5 :y 6}])

(aog/data data-vec)

;; ### mapping(positional... named)
;;
;; Specify aesthetic mappings (columns → visual properties).

;; Positional only
(aog/mapping :x :y)

;; Named only (for color aesthetic)
(aog/mapping {:color :species})

;; Mixed
(aog/mapping :bill-length :bill-depth {:color :species :size :body-mass})

;; ## Algebraic Operators

;; ### * - Merge (Cartesian Product)
;;
;; Combines layer specifications by merging their fields.

(def combined-layer
  (aog/* (aog/data data-map)
         (aog/mapping :x :y)
         (aog/scatter {:alpha 0.7})))

;; Inspect the combined layer
combined-layer

;; Notice how all fields are merged:
;; - :data from first layer
;; - :positional from second layer
;; - :plottype and :attributes from third layer

;; ### + - Overlay
;;
;; Creates a vector of layers to be drawn on the same plot.

(def overlaid-layers
  (aog/+ (aog/scatter {:alpha 0.5})
         (aog/line {:width 2})))

;; This is a vector of two layers
overlaid-layers

(count overlaid-layers)

;; ### Combining * and +
;;
;; The power of algebraic composition: distribute data and mappings
;; across multiple visual types.

(def full-composition
  (aog/* (aog/data data-map)
         (aog/mapping :x :y)
         (aog/+ (aog/scatter {:alpha 0.6})
                (aog/line {:width 2}))))

;; This results in a vector of two complete layers
full-composition

(count full-composition)

;; Each layer has data and mappings
(first full-composition)
(second full-composition)

;; # Part 2: Intermediate Representations (IR)

;; ## Schema 1: Layer (Core)
;;
;; The core Layer schema has 4 required fields.
;; Note: The AoG API adds additional fields (:plottype, :attributes)
;; that are used during processing but aren't part of the core schema.

(def example-layer
  {:transformation nil
   :data {:x [1 2 3] :y [4 5 6]}
   :positional [:x :y]
   :named {}})

;; Validate against schema
(ir/valid? ir/Layer example-layer)

;; ## Schema 2: ProcessedLayer
;;
;; Layer after transformations and grouping.

(def example-processed-layer
  {:primary {:color ["A" "B" "C"]}
   :positional [[1 2 3] [4 5 6]]
   :named {:marker-size [5 6 7]}
   :labels {:x "x" :y "y"}
   :scale-mapping {:color :color-scale-1}
   :plottype :scatter
   :attributes {:alpha 0.7}})

;; Validate
(ir/valid? ir/ProcessedLayer example-processed-layer)

;; ## Schema 3: Entry ⭐
;;
;; THE INTERMEDIATE REPRESENTATION - Backend-agnostic plot specification.

(def example-entry
  {:plottype :scatter
   :positional [[1 2 3] [4 5 6]]
   :named {:marker-color [0 0 1 1 2 2]
           :marker-size 10
           :opacity 0.3}})

;; Validate
(ir/valid? ir/Entry example-entry)

;; Create entry with constructor (includes validation)
(def validated-entry
  (ir/entry :scatter
            [[1 2 3] [4 5 6]]
            {:opacity 0.7}))

validated-entry

;; Try invalid entry (will throw)
(try
  (ir/entry :invalid-plottype
            [[1 2 3] [4 5 6]]
            {})
  (catch Exception e
    (kind/hiccup [:div {:style {:color "red"}}
                  [:strong "Validation Error: "]
                  (.getMessage e)])))

;; ## Schema Validation Helpers

;; Get human-readable validation errors
(ir/explain ir/Entry
            {:plottype :scatter
             ;; Missing :positional - should fail
             :named {}})

;; All schemas available for inspection
(keys ir/schemas)

;; # Part 3: Processing Pipeline

;; ## Data Normalization

;; The processing pipeline starts by normalizing various data formats
;; to a common tablecloth dataset.

;; Map of vectors → dataset
(def normalized-map
  (#'processing/normalize-data {:x [1 2 3] :y [4 5 6]}))

normalized-map

;; Vector of maps → dataset
(def normalized-vec
  (#'processing/normalize-data [{:x 1 :y 4} {:x 2 :y 5}]))

normalized-vec

;; ## Column Extraction

;; Extract columns as vectors
(#'processing/extract-column normalized-map :x)

(#'processing/extract-column normalized-map :y)

;; ## Simple Layer Processing
;;
;; Let's walk through the complete pipeline for a simple scatter plot.

(def simple-layer
  (aog/* (aog/data {:x [1 2 3 4 5]
                    :y [2 4 3 5 6]})
         (aog/mapping :x :y)
         (aog/scatter {:alpha 0.7})))

;; Inspect the layer
simple-layer

;; Convert to ProcessedLayer
(def simple-processed
  (processing/layer->processed-layer simple-layer))

;; Inspect processed layer
simple-processed

;; Notice what happened:
;; - :positional is now actual data arrays: [[1 2 3 4 5] [2 4 3 5 6]]
;; - :labels were generated: {:x "x", :y "y"}
;; - :plottype is resolved: :scatter

;; Convert to Entry
(def simple-entry
  (processing/processed-layer->entry simple-processed))

;; Inspect entry
simple-entry

;; Entry is minimal - just what's needed for rendering!

;; ## Rendering with thi.ng/geom

;; Now render the entry to SVG
(tg/entry->svg simple-entry {:width 600 :height 400})

;; # Part 4: Statistical Transformations

;; ## Linear Regression

(def regression-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2.5 4.8 5.9 8.2 10.1 11.7 14.3 15.8 18.1 19.9]})

(def regression-layers
  (aog/* (aog/data regression-data)
         (aog/mapping :x :y)
         (aog/+ (aog/scatter {:alpha 0.5})
                (aog/linear))))

;; This creates TWO layers
regression-layers

;; First layer: scatter (no transformation)
(first regression-layers)

;; Second layer: linear regression
(second regression-layers)

;; Convert to entries
(def regression-entries
  (processing/layers->entries regression-layers))

;; First entry: scatter points (original data)
(first regression-entries)

;; Second entry: line (fitted data)
(second regression-entries)

;; Render both layers
(tg/entries->svg regression-entries {:width 600 :height 400})

;; ## Histogram

(def histogram-data
  {:values [1.2 1.5 1.8 2.1 2.3 2.5 2.7 2.9 3.1 3.3
            3.5 3.7 3.9 4.1 4.3 4.5 4.7 4.9 5.1 5.3
            5.5 5.7 5.9 6.1 6.3 6.5 6.7 6.9 7.1 7.3]})

(def histogram-layer
  (aog/* (aog/data histogram-data)
         (aog/mapping :values)
         (aog/histogram {:bins 10})))

;; Inspect layer
histogram-layer

;; Notice :transformation is [:histogram {:bins 10}]

;; Process to see what happens
(def histogram-processed
  (processing/layer->processed-layer histogram-layer))

;; The histogram transformation created bin centers and counts
histogram-processed

;; Convert to entry
(def histogram-entry
  (processing/processed-layer->entry histogram-processed))

histogram-entry

;; Render
(tg/entry->svg histogram-entry {:width 600 :height 400})

;; ## Density Estimation

(def density-data
  {:values [1 1.2 1.5 2 2.3 2.5 3 3.2 3.5 4
            4.2 4.5 5 5.5 6 6.5 7 7.5 8 8.5]})

(def density-layer
  (aog/* (aog/data density-data)
         (aog/mapping :values)
         (aog/density)))

density-layer

;; Process
(def density-processed
  (processing/layer->processed-layer density-layer))

;; Density creates smooth curve with many points
density-processed

;; Entry
(def density-entry
  (processing/processed-layer->entry density-processed))

;; Render
(tg/entry->svg density-entry {:width 600 :height 400})

;; # Part 5: Polar Coordinates

;; ## Circle in Polar Coordinates
;;
;; r = constant (radius = 5 for all angles)

(let [theta (vec (range 0 (* 2 Math/PI) 0.1))
      r (vec (repeat (count theta) 5))
      layer (aog/* (aog/data {:theta theta :r r})
                   (aog/mapping :theta :r)
                   (aog/line {:stroke "#3498db" :stroke-width 2}))
      entry (processing/layer->entry layer)]
  (tg/entry->svg entry {:polar true :width 600 :height 600}))

;; ## Spiral
;;
;; r = θ (radius increases linearly with angle)

(let [theta (vec (range 0 (* 4 Math/PI) 0.1))
      r theta
      layer (aog/* (aog/data {:theta theta :r r})
                   (aog/mapping :theta :r)
                   (aog/line {:stroke "#e74c3c" :stroke-width 2}))
      entry (processing/layer->entry layer)]
  (tg/entry->svg entry {:polar true :width 600 :height 600}))

;; ## Rose Curve (3 petals)
;;
;; r = 5 × cos(3θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (Math/cos (* 3 %))) theta))
      layer (aog/* (aog/data {:theta theta :r r})
                   (aog/mapping :theta :r)
                   (aog/line {:stroke "#9b59b6" :stroke-width 2}))
      entry (processing/layer->entry layer)]
  (tg/entry->svg entry {:polar true :width 600 :height 600}))

;; ## Rose Curve (4 petals, actually 8)
;;
;; r = 6 × sin(2θ)
;; Even k gives 2k petals

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 6 (Math/sin (* 2 %))) theta))
      layer (aog/* (aog/data {:theta theta :r r})
                   (aog/mapping :theta :r)
                   (aog/line {:stroke "#2ecc71" :stroke-width 2}))
      entry (processing/layer->entry layer)]
  (tg/entry->svg entry {:polar true :width 600 :height 600}))

;; ## Cardioid
;;
;; r = a × (1 + cos(θ))
;; Named for its heart-like shape

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (+ 1 (Math/cos %))) theta))
      layer (aog/* (aog/data {:theta theta :r r})
                   (aog/mapping :theta :r)
                   (aog/line {:stroke "#d35400" :stroke-width 2}))
      entry (processing/layer->entry layer)]
  (tg/entry->svg entry {:polar true :width 600 :height 600}))

;; ## Radar Chart (Pentagon)
;;
;; Radar charts display multivariate data as polygons.
;; Close the polygon by repeating the first value.

(let [n 5
      theta (vec (map #(* % (/ (* 2.0 Math/PI) n)) (range (+ n 1))))
      r [0.8 0.6 0.9 0.7 0.5 0.8] ; First value repeated at end
      layer (aog/* (aog/data {:theta theta :r r})
                   (aog/mapping :theta :r)
                   (aog/line {:stroke "#2980b9"
                              :stroke-width 3
                              :fill "#2980b9"
                              :opacity 0.3}))
      entry (processing/layer->entry layer)]
  (tg/entry->svg entry {:polar true :width 600 :height 600}))

;; # Part 6: Scale Transformations

;; ## Logarithmic Scale

(def log-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1 10 100 1000 10000 100 1000 10000 100000 1000000]})

(def log-layer
  (aog/* (aog/data log-data)
         (aog/mapping :x :y)
         (aog/log-scale :y {:base 10})
         (aog/line {:stroke "#3498db" :stroke-width 2})))

;; Inspect layer - notice :y-scale in :named
log-layer

;; Process and render
(let [entry (processing/layer->entry log-layer)]
  (tg/entry->svg entry {:width 600 :height 400}))

;; ## Square Root Scale

(def sqrt-data
  {:x [1 4 9 16 25 36 49 64 81 100]
   :y [1 2 3 4 5 6 7 8 9 10]})

(def sqrt-layer
  (aog/* (aog/data sqrt-data)
         (aog/mapping :x :y)
         (aog/sqrt-scale :x)
         (aog/scatter {:color "#e74c3c" :opacity 0.7})))

sqrt-layer

;; Render
(let [entry (processing/layer->entry sqrt-layer)]
  (tg/entry->svg entry {:width 600 :height 400}))

;; ## Custom Domain

(def domain-data
  {:x [5 10 15 20 25 30 35 40 45 50]
   :y [100 150 200 250 300 350 400 450 500 550]})

(def domain-layer
  (aog/* (aog/data domain-data)
         (aog/mapping :x :y)
         (aog/scale-domain :x [0 60])
         (aog/scale-domain :y [0 600])
         (aog/line {:stroke "#16a085" :stroke-width 2})))

domain-layer

;; Render
(let [entry (processing/layer->entry domain-layer)]
  (tg/entry->svg entry {:width 600 :height 400}))

;; # Part 7: Grouped Data and Scales

;; ## Automatic Grouping by Color

(def grouped-data
  {:x [1 2 3 4 5 6 7 8 9 10
       1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6
       3 5 4 6 7 5 8 6 9 7]
   :group ["A" "A" "A" "A" "A" "A" "A" "A" "A" "A"
           "B" "B" "B" "B" "B" "B" "B" "B" "B" "B"]})

(def grouped-layer
  (aog/* (aog/data grouped-data)
         (aog/mapping :x :y {:color :group})
         (aog/scatter {:alpha 0.7})))

;; Inspect layer
grouped-layer

;; Process to see grouping
(def grouped-processed
  (processing/layer->processed-layer grouped-layer))

;; Notice :primary contains the categorical aesthetic
(:primary grouped-processed)

;; Scale mapping
(:scale-mapping grouped-processed)

;; Convert to entry
(def grouped-entry
  (processing/processed-layer->entry grouped-processed))

grouped-entry

;; ## Scale Inference
;;
;; Let's see how scales are inferred from entries.

(def scale-info
  (scales/infer-scales [grouped-entry]))

scale-info

;; Categorical scales (color)
(:categorical-scales scale-info)

;; The system automatically assigns colors from a default palette
;; to the unique values in :group

;; # Part 8: Backend Internals

;; ## Entry → thi.ng Spec Conversion

(def demo-entry
  {:plottype :scatter
   :positional [[1 2 3 4 5]
                [2 4 3 5 6]]
   :named {:color "#e74c3c"
           :opacity 0.7}})

;; Convert to thi.ng/geom spec (internal function)
(def thing-spec
  (#'tg/entry->thing-spec demo-entry))

;; Inspect the spec
thing-spec

;; Notice:
;; - :x-axis with inferred domain [1 5] and range [50 550]
;; - :y-axis with inferred domain [2 6] and range [350 50] (inverted)
;; - :data with values converted to [[1 2] [2 4] [3 3] [4 5] [5 6]]

;; ## Positional Data Conversion

;; AoG format: [[x...] [y...]]
(def aog-positional [[1 2 3] [4 5 6]])

;; thi.ng format: [[x y] [x y] [x y]]
(def thing-positional
  (#'tg/positional->thing-values aog-positional))

thing-positional

;; ## Axis Building

;; Build X axis from data
(def x-axis
  (#'tg/build-axis [1 2 3 4 5] :x :linear))

x-axis

;; Notice:
;; - :domain [1 5] (min to max)
;; - :range [50 550] (pixel coordinates with margins)
;; - :pos 350 (Y position for X axis, at bottom)
;; - :major and :minor for tick marks

;; Build Y axis from data
(def y-axis
  (#'tg/build-axis [2 4 3 5 6] :y :linear))

y-axis

;; Notice:
;; - :range [350 50] (inverted for SVG coordinates)
;; - :pos 50 (X position for Y axis, at left)

;; ## Multi-Layer Rendering

(def multi-entries
  [{:plottype :scatter
    :positional [[1 2 3 4 5]
                 [2 4 3 5 6]]
    :named {:color "#3498db" :opacity 0.7}}
   {:plottype :line
    :positional [[1 2 3 4 5]
                 [2 4 3 5 6]]
    :named {:stroke "#e74c3c" :stroke-width 2}}])

;; Render multiple entries
(tg/entries->svg multi-entries {:width 600 :height 400})

;; The backend combines data specs from both entries on one plot

;; # Part 9: Complete Examples

;; ## Example 1: Simple Workflow

;; Step-by-step from user code to SVG

(def ex1-data {:x [1 2 3 4 5]
               :y [2 4 3 5 6]})

;; Step 1: Create layer
(def ex1-layer
  (aog/* (aog/data ex1-data)
         (aog/mapping :x :y)
         (aog/scatter {:alpha 0.7})))

;; Step 2: Convert to entry
(def ex1-entry
  (processing/layer->entry ex1-layer))

;; Step 3: Render
(tg/entry->svg ex1-entry {:width 600 :height 400})

;; ## Example 2: Scatter + Trend Line

(def ex2-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2.1 3.9 6.2 7.8 10.1 12.3 13.9 16.2 17.8 20.1]})

;; Create two layers with +
(def ex2-layers
  (aog/* (aog/data ex2-data)
         (aog/mapping :x :y)
         (aog/+ (aog/scatter {:alpha 0.6})
                (aog/linear))))

;; Convert to entries
(def ex2-entries
  (processing/layers->entries ex2-layers))

;; Render
(tg/entries->svg ex2-entries {:width 600 :height 400})

;; ## Example 3: Polar Rose with Multiple Petals

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r1 (vec (map #(* 5 (Math/cos (* 3 %))) theta))
      r2 (vec (map #(* 4 (Math/sin (* 5 %))) theta))
      layer1 (aog/* (aog/data {:theta theta :r r1})
                    (aog/mapping :theta :r)
                    (aog/line {:stroke "#e74c3c" :stroke-width 2 :opacity 0.7}))
      layer2 (aog/* (aog/data {:theta theta :r r2})
                    (aog/mapping :theta :r)
                    (aog/line {:stroke "#3498db" :stroke-width 2 :opacity 0.7}))
      entries (processing/layers->entries [layer1 layer2])]
  (tg/entries->svg entries {:polar true :width 600 :height 600}))

;; ## Example 4: Histogram with Distribution

(def ex4-data
  {:values (vec (concat (repeatedly 50 #(+ 50 (* 10 (rand))))
                        (repeatedly 50 #(+ 80 (* 10 (rand))))))})

(def ex4-layer
  (aog/* (aog/data ex4-data)
         (aog/mapping :values)
         (aog/histogram {:bins 15})))

(let [entry (processing/layer->entry ex4-layer)]
  (tg/entry->svg entry {:width 600 :height 400}))

;; ## Example 5: Multi-Layer with Different Types

(let [x [1 2 3 4 5 6 7 8 9 10]
      y-bars [23 45 34 56 42 38 51 48 52 47]
      avg (double (/ (reduce + y-bars) (count y-bars)))
      avg-line (vec (repeat (count x) avg))
      layer1 (aog/* (aog/data {:x x :y y-bars})
                    (aog/mapping :x :y)
                    (aog/bar {:fill "#3498db" :opacity 0.7}))
      layer2 (aog/* (aog/data {:x x :y avg-line})
                    (aog/mapping :x :y)
                    (aog/line {:stroke "#e74c3c" :stroke-width 3}))
      entries (processing/layers->entries [layer1 layer2])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; # Part 10: Validation and Debugging

;; ## Schema Validation

;; Valid layer (core schema fields only)
(def valid-layer
  {:transformation nil
   :data {:x [1 2 3]}
   :positional [:x]
   :named {}})

(ir/valid? ir/Layer valid-layer)

;; Invalid layer (missing required keys)
(def invalid-layer
  {:data {:x [1 2 3]}})

(ir/valid? ir/Layer invalid-layer)

;; Get explanation
(ir/explain ir/Layer invalid-layer)

;; ## Entry Validation

;; Valid entry
(ir/valid? ir/Entry {:plottype :scatter
                     :positional [[1 2 3]]
                     :named {}})

;; Invalid plottype
(ir/valid? ir/Entry {:plottype :invalid-type
                     :positional [[1 2 3]]
                     :named {}})

;; Get explanation for invalid entry
(ir/explain ir/Entry {:plottype :invalid-type
                      :positional [[1 2 3]]
                      :named {}})

;; ## Backend Validation

;; Check if entry can be rendered with thi.ng/geom
(tg/validate-entry {:plottype :scatter
                    :positional [[1 2 3] [4 5 6]]
                    :named {}})

;; Unsupported plottype (3D not supported by thi.ng/geom-viz 2D)
(tg/validate-entry {:plottype :surface
                    :positional [[1 2 3] [4 5 6]]
                    :named {}})

;; # Summary
;;
;; We've explored the complete AoG + thi.ng/geom architecture:
;;
;; ## Layer 1: User API
;; - Algebraic composition with `*` and `+`
;; - Data, mapping, and visual constructors
;; - Statistical and scale transformations
;;
;; ## Layer 2: IR Schemas
;; - Layer → ProcessedLayer → Entry → AxisEntries
;; - Malli validation at every step
;; - Backend-agnostic Entry format
;;
;; ## Layer 3: Processing Pipeline
;; - Data normalization and column extraction
;; - Statistical transformations with grouping
;; - Label and scale generation
;;
;; ## Layer 4: thi.ng/geom Backend
;; - Entry → thi.ng spec conversion
;; - Automatic axis inference
;; - Cartesian and polar rendering
;; - SVG generation with Kindly wrapping
;;
;; ## Key Strengths
;; - ✅ Compositional and declarative
;; - ✅ Backend independence
;; - ✅ Type safety (Malli schemas)
;; - ✅ Pure Clojure (thi.ng/geom)
;; - ✅ Polar coordinate excellence
;;
;; ## Next Steps
;; - Explore the 60+ test examples in thing_geom_*.clj notebooks
;; - Experiment with polar plots and mathematical curves
;; - Try the complete reference: thing_geom_complete_reference.clj
;; - Integrate with your own datasets
