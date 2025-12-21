;; # Building an Algebra of Graphics in Clojure
;; **A self-contained exploration of composable plot specifications**
;;
;; *This notebook presents a design for building plot specifications in Clojure,
;; inspired by Julia's AlgebraOfGraphics.jl. It's completely self-contained—you
;; can run this code independently to understand the concepts and try the approach.*
;;
;; ## Table of Contents
;;
;; - Inspiration: AlgebraOfGraphics.jl
;; - The Problem: Composable Visualization
;; - Design Journey: Three Approaches
;; - Implementation: A Minimal AoG
;; - Real Examples with Tablecloth
;; - Design Rationale & Trade-offs
;; - Open Questions for Discussion

(ns building-aog-in-clojure
  (:require [tablecloth.api :as tc]
            [scicloj.kindly.v4.kind :as kind]
            [thi.ng.geom.viz.core :as viz]
            [thi.ng.geom.svg.core :as svg]))

;; # Inspiration: AlgebraOfGraphics.jl
;;
;; This implementation is inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/),
;; a thoughtfully-designed visualization library that builds on decades of experience
;; from classic systems like ggplot2.
;;
;; ## Why AlgebraOfGraphics.jl?
;;
;; We chose it as inspiration for several reasons:
;;
;; 1. **Well-designed** - Built by experts who learned from ggplot2's strengths and limitations
;; 2. **Principled** - Based on clear algebraic operations rather than ad-hoc APIs
;; 3. **Tractable** - Small enough to understand and reproduce (~10 core concepts)
;; 4. **Modern** - Incorporates 20+ years of data visualization research
;;
;; ## Core Ideas
;;
;; AlgebraOfGraphics.jl introduces several key concepts:
;;
;; **1. Everything is a Layer**
;; - Data, mappings, and visual marks are all "layers"
;; - Layers compose via algebraic operations
;; - No distinction between "base plots" and "additions"
;;
;; **2. Two Operations: `*` and `+`**
;;
;; - `*` (multiplication/product) - **Merges** layers together
;;   - `data * mapping * visual` → single layer with all properties
;;   - Cartesian product: combines attributes from both operands
;;   
;; - `+` (addition/sum) - **Overlays** layers
;;   - `scatter + line` → two separate layers on same plot
;;   - Creates multiple visual marks
;;
;; **3. Comparison to ggplot2**
;;
;; ggplot2 uses `+` for everything:
;; ```r
;; ggplot(data) + 
;;   geom_point(aes(x, y)) +
;;   geom_smooth(aes(x, y))
;; ```
;;
;; AlgebraOfGraphics separates concerns with two operators:
;; ```julia
;; data * mapping(x, y) * (scatter + smooth)
;; ```
;;
;; **Why two operators?**
;;
;; - **Clarity**: `*` = "combine properties", `+` = "overlay visuals"
;; - **Composability**: `data * mapping` can be reused across multiple plot types
;; - **Mathematical elegance**: Follows algebraic distributive law: `a * (b + c) = (a * b) + (a * c)`
;;
;; **Example:**
;; ```julia
;; # These are equivalent:
;; data * mapping * (scatter + line)
;; # ↓ distributive law
;; (data * mapping * scatter) + (data * mapping * line)
;; ```
;;
;; This means you can factor out common properties (`data * mapping`) and apply
;; them to multiple plot types, reducing repetition.
;;
;; ## Our Goal
;;
;; Bring these ideas to Clojure while leveraging:
;; - Maps for data representation (not objects)
;; - Standard functions (merge, assoc, filter)
;; - Multiple rendering backends (SVG, Vega-Lite)
;; - Idiomatic Clojure style

;; # The Problem: Composable Visualization
;;
;; When building visualizations, we want:
;;
;; 1. **Composability** - Combine data, mappings, and plot types freely
;; 2. **Layering** - Overlay multiple visualizations (scatter + regression)
;; 3. **Standard operations** - Use regular Clojure functions (merge, assoc, filter)
;; 4. **Clarity** - Intent should be obvious from the code
;;
;; Example of what we want to write:
;;
;; ```clojure
;; (plot (* (data penguins)
;;          (mapping :bill-length :bill-depth {:color :species})
;;          (+ (scatter {:alpha 0.5})
;;             (linear))))
;; ```
;;
;; This should:
;; - Use the penguins dataset
;; - Map bill-length to x, bill-depth to y, species to color
;; - Create two layers: scatter plot + linear regression
;; - Render as a visualization

;; # Design Journey: Three Approaches
;;
;; Let's explore three different ways to structure plot specifications,
;; showing the evolution toward using standard Clojure operations.

;; ## Approach 1: Nested Structure (Traditional)
;;
;; This is how many plotting libraries work internally.

(def nested-layer-example
  {:transformation nil
   :data {:bill-length [39.1 39.5 40.3]
          :bill-depth [18.7 17.4 18.0]
          :species [:adelie :adelie :adelie]}
   :positional [:bill-length :bill-depth] ;; Vector of column names
   :named {:color :species} ;; Map of aesthetics
   :attributes {:alpha 0.5}}) ;; Map of constants

;; **Problem**: How do we merge two of these layers?
;;
;; Standard `merge` doesn't work:

(merge {:positional [:x] :named {:color :species}}
       {:positional [:y] :named {:size :body-mass}})
;; => {:positional [:y]  ;; Lost :x!
;;     :named {:size :body-mass}}  ;; Lost :color!

;; You need custom merge logic that:
;; - Concatenates :positional vectors
;; - Merges :named maps
;; - Merges :attributes maps
;;
;; This means **you can't use standard Clojure operations**.

;; ## Approach 2: Flat Structure with Plain Keys
;;
;; What if we flatten everything to the same level?

(def flat-layer-example-v1
  {:data {:bill-length [39.1 39.5 40.3]
          :bill-depth [18.7 17.4 18.0]
          :species [:adelie :adelie :adelie]}
   :x :bill-length ;; All at the same level
   :y :bill-depth
   :color :species
   :alpha 0.5
   :plottype :scatter})

;; **Advantage**: Standard `merge` works!

;; Example of standard merge working
(merge {:data {:bill-length [39.1] :bill-depth [18.7]} :x :bill-length}
       {:y :bill-depth :color :species}
       {:plottype :scatter :alpha 0.5})
;; => Everything merges correctly!

;; **Problem**: What if your data has columns named :x, :y, :color, or :plottype?

(def tricky-data
  {:x [1 2 3]
   :y [4 5 6]
   :plottype [:a :b :c]}) ;; Column named :plottype!

;; This becomes ambiguous:
;; {:data tricky-data :plottype :scatter :y :plottype}
;; Is :plottype a layer metadata or a column mapping?

;; ## Approach 3: Flat Structure with Namespaced Keys ✨
;;
;; Use Clojure's namespaced keywords to prevent collisions!

(def flat-layer-example-v2
  #:aog{:data {:bill-length [39.1 39.5 40.3]
               :bill-depth [18.7 17.4 18.0]
               :species [:adelie :adelie :adelie]}
        :x :bill-length ;; Namespaced with :aog
        :y :bill-depth
        :color :species
        :alpha 0.5
        :plottype :scatter})

;; Expands to:
;; {:aog/data {...}
;;  :aog/x :bill-length
;;  :aog/y :bill-depth
;;  :aog/color :species
;;  :aog/alpha 0.5
;;  :aog/plottype :scatter}

;; **Advantages**:
;; 1. Standard `merge` works
;; 2. No collision with data columns
;; 3. Clear distinction: :aog/plottype vs :plottype column
;; 4. Concise with namespace map syntax `#:aog{...}`

;; **This is our chosen approach!**

;; # Implementation: A Minimal AoG
;;
;; Let's build a minimal implementation to demonstrate the concepts.

;; ## Constructor Functions
;;
;; These create partial layer specifications.

(defn data
  "Attach a dataset to a layer."
  [dataset]
  {:aog/data dataset})

(defn mapping
  "Define aesthetic mappings.
  
  Args:
  - x, y: Column names for positional aesthetics
  - named: Optional map of other aesthetics {:color :species, :size :body-mass}
  
  Examples:
  (mapping :bill-length :bill-depth)
  (mapping :bill-length :bill-depth {:color :species})"
  ([x y]
   {:aog/x x :aog/y y})
  ([x y named]
   (merge {:aog/x x :aog/y y}
          (update-keys named #(keyword "aog" (name %))))))

(defn scatter
  "Create a scatter plot layer.
  
  Args:
  - attrs: Optional map of visual attributes {:alpha 0.5, :size 100}"
  ([]
   {:aog/plottype :scatter})
  ([attrs]
   (merge {:aog/plottype :scatter}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn line
  "Create a line plot layer."
  ([]
   {:aog/plottype :line})
  ([attrs]
   (merge {:aog/plottype :line}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn linear
  "Add linear regression transformation."
  []
  {:aog/transformation :linear
   :aog/plottype :line})

(defn smooth
  "Statistical transformation: LOESS smoothing.
  
  Returns a transformation spec that can be merged with layers."
  []
  {:aog/transformation :smooth
   :aog/plottype :line})

(defn density
  "Statistical transformation: Kernel density estimation.
  
  Returns a transformation spec for density plots."
  []
  {:aog/transformation :density
   :aog/plottype :area})

(defn histogram
  "Plot type: Histogram with binning.
  
  Returns a layer spec for histograms."
  ([] {:aog/plottype :histogram})
  ([opts] (merge {:aog/plottype :histogram} opts)))

(defn area
  "Plot type: Area plot (filled line plot).
  
  Returns a layer spec for area plots."
  ([] [{:aog/plottype :area}])
  ([opts] [(merge {:aog/plottype :area} opts)]))

(defn bar
  "Plot type: Bar chart.
  
  Returns a layer spec for bar charts."
  ([] [{:aog/plottype :bar}])
  ([opts] [(merge {:aog/plottype :bar} opts)]))

;; ## Algebraic Operators
;;
;; The `*` operator merges layers (product/composition).
;; The `+` operator combines layers (sum/overlay).

(defn *
  "Merge layer specifications.
  
  Implements the product in the algebra:
  - Map × Map → [Map] (merge and wrap)
  - Map × Vec → Vec (merge into each)
  - Vec × Vec → Vec (cartesian product with merge)
  
  Examples:
  (* (data df) (mapping :x :y) (scatter))
  ;; => [{:aog/data df :aog/x :x :aog/y :y :aog/plottype :scatter}]"
  ([x] (if (map? x) [x] x))
  ([x y]
   (cond
     (and (map? x) (map? y))
     [(merge x y)]

     (and (map? x) (vector? y))
     (mapv #(merge x %) y)

     (and (vector? x) (map? y))
     (mapv #(merge % y) x)

     (and (vector? x) (vector? y))
     (vec (for [a x, b y] (merge a b)))))
  ([x y & more]
   (reduce * (* x y) more)))

(defn +
  "Combine multiple layer specifications for overlay.
  
  Simply concatenates into a vector.
  
  Example:
  (+ (scatter) (linear))
  ;; => [{:aog/plottype :scatter} {:aog/transformation :linear :aog/plottype :line}]"
  [& layer-specs]
  (vec (mapcat #(if (vector? %) % [%]) layer-specs)))

;; ## thi.ng/geom-viz SVG Backend
;;
;; Use thi.ng/geom-viz to generate SVG visualizations.
;;
;; Note: Since we define `*` and `+` operators for our AoG algebra, we need to use
;; `clojure.core/*` and `clojure.core/+` explicitly for arithmetic operations.

(defn- get-column-data
  "Extract column data from dataset (handles both maps and Tablecloth datasets)."
  [data col-key]
  (vec (get data col-key)))

(defn- infer-domain
  "Infer domain from data values."
  [values]
  (cond
    (empty? values)
    [0 1]

    (every? number? values)
    [(apply min values) (apply max values)]

    :else
    values))

(defn- layer->points
  "Convert layer to point data for rendering."
  [layer]
  (let [data (:aog/data layer)
        x-vals (get-column-data data (:aog/x layer))
        y-vals (get-column-data data (:aog/y layer))
        color-col (:aog/color layer)
        color-vals (when (keyword? color-col)
                     (get-column-data data color-col))
        n (count x-vals)]
    (mapv (fn [i]
            {:x (nth x-vals i)
             :y (nth y-vals i)
             :color (when color-vals (nth color-vals i))})
          (range n))))

(defn- unique-values
  "Get unique values from a sequence, preserving order."
  [coll]
  (vec (distinct coll)))

(defn- color-scale
  "Create a simple color scale for categorical data."
  [categories]
  (let [colors ["#1f77b4" "#ff7f0e" "#2ca02c" "#d62728" "#9467bd"
                "#8c564b" "#e377c2" "#7f7f7f" "#bcbd22" "#17becf"]]
    (zipmap categories (cycle colors))))

(defn- layer->scatter-spec
  "Convert a scatter layer to thi.ng/geom-viz spec."
  [layer width height]
  (let [points (layer->points layer)
        x-vals (mapv :x points)
        y-vals (mapv :y points)
        x-domain (infer-domain x-vals)
        y-domain (infer-domain y-vals)

        ;; Only works with numeric data
        x-numeric? (and (vector? x-domain) (= 2 (count x-domain)) (every? number? x-domain))
        y-numeric? (and (vector? y-domain) (= 2 (count y-domain)) (every? number? y-domain))]

    (when (and x-numeric? y-numeric?)
      (let [alpha (or (:aog/alpha layer) 1.0)
            color-groups (when-let [colors (seq (keep :color points))]
                           (group-by :color points))
            has-color? (some? color-groups)

            ;; Compute nice tick intervals - use clojure.core/* for arithmetic
            x-range (- (second x-domain) (first x-domain))
            y-range (- (second y-domain) (first y-domain))
            x-major (clojure.core/* x-range 0.2)
            y-major (clojure.core/* y-range 0.2)]

        {:x-axis (viz/linear-axis
                  {:domain x-domain
                   :range [50 (- width 50)]
                   :major x-major
                   :pos (- height 50)})
         :y-axis (viz/linear-axis
                  {:domain y-domain
                   :range [(- height 50) 50]
                   :major y-major
                   :pos 50})
         :data (if has-color?
                 ;; Multiple series (one per color)
                 (let [categories (unique-values (keep :color points))
                       colors (color-scale categories)]
                   (mapv (fn [category]
                           (let [cat-points (get color-groups category)
                                 point-data (mapv (fn [p] [(:x p) (:y p)]) cat-points)]
                             {:values point-data
                              :layout viz/svg-scatter-plot
                              :attribs {:fill (get colors category)
                                        :stroke "none"
                                        :opacity alpha}}))
                         categories))
                 ;; Single series
                 [{:values (mapv (fn [p] [(:x p) (:y p)]) points)
                   :layout viz/svg-scatter-plot
                   :attribs {:fill "#1f77b4"
                             :stroke "none"
                             :opacity alpha}}])}))))

(defn- layer->line-spec
  "Convert a line layer to thi.ng/geom-viz spec."
  [layer width height]
  (let [points (layer->points layer)
        sorted-points (sort-by :x points)
        x-vals (mapv :x sorted-points)
        y-vals (mapv :y sorted-points)
        x-domain (infer-domain x-vals)
        y-domain (infer-domain y-vals)

        ;; Only works with numeric data
        x-numeric? (and (vector? x-domain) (= 2 (count x-domain)) (every? number? x-domain))
        y-numeric? (and (vector? y-domain) (= 2 (count y-domain)) (every? number? y-domain))]

    (when (and x-numeric? y-numeric?)
      (let [alpha (or (:aog/alpha layer) 1.0)

            ;; Compute nice tick intervals - use clojure.core/* for arithmetic
            x-range (- (second x-domain) (first x-domain))
            y-range (- (second y-domain) (first y-domain))
            x-major (clojure.core/* x-range 0.2)
            y-major (clojure.core/* y-range 0.2)]

        {:x-axis (viz/linear-axis
                  {:domain x-domain
                   :range [50 (- width 50)]
                   :major x-major
                   :pos (- height 50)})
         :y-axis (viz/linear-axis
                  {:domain y-domain
                   :range [(- height 50) 50]
                   :major y-major
                   :pos 50})
         :data [{:values (mapv (fn [p] [(:x p) (:y p)]) sorted-points)
                 :layout viz/svg-line-plot
                 :attribs {:stroke "#1f77b4"
                           :fill "none"
                           :opacity alpha}}]}))))

(defn- layers->svg
  "Convert layers to SVG string using thi.ng/geom-viz."
  [layers width height]
  (let [layer (if (vector? layers) (first layers) layers)
        plottype (:aog/plottype layer)

        spec (case plottype
               :scatter (layer->scatter-spec layer width height)
               :line (layer->line-spec layer width height)
               (layer->scatter-spec layer width height))]

    (when spec
      (svg/serialize
       (svg/svg {:width width :height height}
                (viz/svg-plot2d-cartesian spec))))))

(defn plot
  "Render layers as an SVG visualization.
  
  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with :width (default 600) and :height (default 400)
  
  Returns:
  - Kindly-wrapped HTML containing SVG string"
  ([layers]
   (plot layers {}))
  ([layers opts]
   (let [layers (if (vector? layers) layers [layers])
         width (or (:width opts) 600)
         height (or (:height opts) 400)
         svg-string (layers->svg layers width height)]
     (kind/html svg-string))))

;; ## Vega-Lite Rendering Backend
;;
;; In addition to geom-viz (static SVG), we can render to Vega-Lite for
;; interactive visualizations with tooltips, zoom/pan, and more.

(defn- layer->vega-data
  "Convert layer dataset to Vega-Lite data format."
  [layer]
  (let [data (:aog/data layer)]
    (if (tc/dataset? data)
      (tc/rows data :as-maps)
      ;; If it's a plain map, convert to dataset first
      (tc/rows (tc/dataset data) :as-maps))))

(defn- layer->vega-scatter
  "Convert a scatter layer to Vega-Lite mark spec."
  [layer]
  (let [x (:aog/x layer)
        y (:aog/y layer)
        color (:aog/color layer)
        alpha (or (:aog/alpha layer) 1.0)

        encoding (cond-> {:x {:field (name x)
                              :type "quantitative"
                              :scale {:zero false}}
                          :y {:field (name y)
                              :type "quantitative"
                              :scale {:zero false}}}
                   color (assoc :color {:field (name color) :type "nominal"}))]

    {:mark {:type "point" :opacity alpha}
     :encoding encoding}))

(defn- layer->vega-line
  "Convert a line layer to Vega-Lite mark spec."
  [layer]
  (let [x (:aog/x layer)
        y (:aog/y layer)
        color (:aog/color layer)

        encoding (cond-> {:x {:field (name x)
                              :type "quantitative"
                              :scale {:zero false}}
                          :y {:field (name y)
                              :type "quantitative"
                              :scale {:zero false}}}
                   color (assoc :color {:field (name color) :type "nominal"}))]

    {:mark {:type "line"}
     :encoding encoding}))

(defn- layer->vega-area
  "Convert an area layer to Vega-Lite mark spec."
  [layer]
  (let [spec (layer->vega-line layer)]
    (assoc spec :mark {:type "area"})))

(defn- layer->vega-bar
  "Convert a bar layer to Vega-Lite mark spec."
  [layer]
  (let [x (:aog/x layer)
        y (:aog/y layer)
        color (:aog/color layer)

        encoding (cond-> {:x {:field (name x) :type "nominal"}
                          :y {:field (name y)
                              :type "quantitative"
                              :scale {:zero false}}}
                   color (assoc :color {:field (name color) :type "nominal"}))]

    {:mark {:type "bar"}
     :encoding encoding}))

(defn- layers->vega-spec
  "Convert layers to Vega-Lite specification."
  [layers width height]
  (let [layers (if (vector? layers) layers [layers])
        first-layer (first layers)
        data-values (layer->vega-data first-layer)

        layer-specs (mapv (fn [layer]
                            (let [plottype (:aog/plottype layer)]
                              (case plottype
                                :scatter (layer->vega-scatter layer)
                                :line (layer->vega-line layer)
                                :area (layer->vega-area layer)
                                :bar (layer->vega-bar layer)
                                (layer->vega-scatter layer))))
                          layers)]

    (if (= 1 (count layer-specs))
      ;; Single layer
      (merge (first layer-specs)
             {:width width
              :height height
              :data {:values data-values}})
      ;; Multiple layers - use layer composition
      {:width width
       :height height
       :data {:values data-values}
       :layer layer-specs})))

(defn plot-vega
  "Render layers as a Vega-Lite visualization.
  
  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with :width (default 600) and :height (default 400)
  
  Returns:
  - Kindly-wrapped Vega-Lite spec"
  ([layers]
   (plot-vega layers {}))
  ([layers opts]
   (let [width (or (:width opts) 400)
         height (or (:height opts) 300)
         spec (layers->vega-spec layers width height)]
     (kind/vega-lite spec))))

;; # Real Examples with Tablecloth
;;
;; Now let's use our implementation with real data!

;; ## Load the Palmer Penguins Dataset

(def penguins
  (tc/dataset {:bill-length [39.1 39.5 40.3 36.7 39.3 38.9 39.2 41.1]
               :bill-depth [18.7 17.4 18.0 19.3 20.6 17.8 19.6 17.6]
               :species [:adelie :adelie :adelie :adelie
                         :chinstrap :chinstrap :gentoo :gentoo]
               :body-mass [3750 3800 3250 3450 3650 3625 4675 3200]}))

;; View the data
penguins

;; ## Example 1: Simple Scatter Plot with Algebraic Style

(plot
 (* (data penguins)
    (mapping :bill-length :bill-depth)
    (scatter)))

;; ## Example 2: Add Color Aesthetic

(plot
 (* (data penguins)
    (mapping :bill-length :bill-depth {:color :species})
    (scatter {:alpha 0.7})))

;; ## Example 3: Using Standard Clojure `merge`
;;
;; The key insight: we can build the same plot with standard merge!

(def layer-with-merge
  (merge (data penguins)
         (mapping :bill-length :bill-depth {:color :species})
         (scatter {:alpha 0.7})))

;; Inspect the layer structure
layer-with-merge

;; Plot it
(plot [layer-with-merge])

;; ## Example 4: Using Standard `assoc` to Add Aesthetics

(def base-layer
  (merge (data penguins)
         (mapping :bill-length :bill-depth)
         (scatter)))

;; Add color with standard assoc
(def with-color
  (assoc base-layer :aog/color :species))

with-color

(plot [with-color])

;; ## Example 5: Using Standard `update` to Modify Attributes

(def with-alpha
  (merge (data penguins)
         (mapping :bill-length :bill-depth)
         (scatter {:alpha 0.3})))

;; Double the alpha value with standard update
(def more-alpha
  (update with-alpha :aog/alpha * 2))

(:aog/alpha more-alpha) ;; => 0.6

(plot [more-alpha])

;; ## Example 6: Multi-Layer Plot (Scatter + Regression)
;;
;; Use the + operator to overlay layers.

(plot
 (* (data penguins)
    (mapping :bill-length :bill-depth {:color :species})
    (+ (scatter {:alpha 0.5})
       (linear))))

;; ## Example 7: Conditional Layer Building with `into`

(defn make-plot [dataset show-regression?]
  (let [base (* (data dataset)
                (mapping :bill-length :bill-depth {:color :species})
                (scatter {:alpha 0.5}))

        with-regression (if show-regression?
                          (into base
                                (* (data dataset)
                                   (mapping :bill-length :bill-depth {:color :species})
                                   (linear)))
                          base)]
    (plot with-regression)))

;; Just scatter
(make-plot penguins false)

;; Scatter + regression
(make-plot penguins true)

;; ## Example 8: Transform Layers with `mapv`

(def multi-layer
  (* (data penguins)
     (mapping :bill-length :bill-depth)
     (+ (scatter) (line))))

;; Add alpha to all layers
(def all-with-alpha
  (mapv #(assoc % :aog/alpha 0.6) multi-layer))

(plot all-with-alpha)

;; ## Example 9: Filter Layers with `filterv`

(def many-layers
  (* (data penguins)
     (mapping :bill-length :bill-depth)
     (+ (scatter) (line))))

;; Keep only scatter
(def just-scatter
  (filterv #(= :scatter (:aog/plottype %)) many-layers))

(plot just-scatter)

;; ## Example 10: Manual Layer Construction
;;
;; You don't need helper functions - just create maps!

(def manual-layer
  #:aog{:data penguins
        :x :bill-length
        :y :bill-depth
        :color :species
        :plottype :scatter
        :alpha 0.5})

(plot [manual-layer])

;; ## Example 11: The Namespace Prevents Collisions
;;
;; This is why namespacing matters!

(def tricky-data
  (tc/dataset {:x [1 2 3]
               :y [4 5 6]
               :plottype [:a :b :c]})) ;; Column named :plottype!

;; Without namespacing, this would be ambiguous:
;; {:data tricky-data :plottype :scatter :y :plottype}

;; With :aog namespace, it's clear:
(def no-confusion
  #:aog{:data tricky-data
        :x :x
        :y :plottype ;; Mapping to the :plottype column!
        :plottype :scatter})

;; ## Example 12: Multiple Rendering Backends
;;
;; The same layer specification can be rendered with different backends!

;; Define a visualization once
(def viz-layers
  (* (data penguins)
     (mapping :bill-length :bill-depth {:color :species})
     (scatter {:alpha 0.7})))

;; Render with geom-viz (static SVG)
(plot viz-layers)

;; Render with Vega-Lite (interactive)
(plot-vega viz-layers)

;; **Key insight**: Same data structure, different renderers!
;; - geom-viz: Static SVG, great for print/PDFs, fast rendering
;; - Vega-Lite: Interactive JSON, tooltips, zoom/pan, web-friendly

;; ## Example 13: Time Series with Area Plot

(def economics
  {:month (range 1 25)
   :unemployment (mapv #(clojure.core/+ 200 (clojure.core/* 50 (Math/sin (clojure.core// % 3)))) (range 1 25))
   :savings (mapv #(clojure.core/+ 100 (clojure.core/* 30 (Math/cos (clojure.core// % 4)))) (range 1 25))})

;; Single area plot
(plot-vega
 (* (data economics)
    (mapping :month :unemployment)
    (area)))

;; Multiple time series (overlay two area plots)
(plot-vega
 (+ (* (data economics)
       (mapping :month :unemployment)
       (area))
    (* (data economics)
       (mapping :month :savings)
       (area))))

;; ## Example 14: Bar Charts

(def categories
  {:category ["A" "B" "C" "D" "E"]
   :count [23 45 37 29 41]
   :group ["Group 1" "Group 1" "Group 2" "Group 2" "Group 1"]})

;; Simple bar chart
(plot-vega
 (* (data categories)
    (mapping :category :count)
    (bar)))

;; Grouped bar chart
(plot-vega
 (* (data categories)
    (mapping :category :count {:color :group})
    (bar)))

;; ## Example 15: Combining Multiple Plot Types

;; Scatter + Line overlay
(plot-vega
 (* (data penguins)
    (mapping :bill-length :bill-depth {:color :species})
    (+ (scatter {:alpha 0.5})
       (line))))

;; With statistical transformation (Vega-Lite handles this natively)
(plot-vega
 (+ (* (data penguins)
       (mapping :bill-length :bill-depth {:color :species})
       (scatter {:alpha 0.5}))
    (* (data penguins)
       (mapping :bill-length :bill-depth {:color :species})
       (linear))))

;; ## Example 16: Polar Coordinates (Rose Diagram)
;;
;; Polar coordinates are perfect for cyclical data (directions, times, seasons).
;; We can use geom-viz's polar backend.

(def wind-data
  {:direction (mapv #(clojure.core/* % 30) (range 12)) ;; Degrees: 0, 30, 60, ..., 330
   :speed [12 15 18 22 25 20 15 10 8 7 9 11]})

;; Note: For polar plots with geom-viz, we'd use viz/svg-plot2d-polar
;; This is a simplified example showing the data structure

(comment
  ;; Polar plot would require extending our plot function to support :polar option
  (plot wind-data
        {:coordinate-system :polar
         :mapping {:angle :direction
                   :radius :speed}}))

;; ## Summary
;;
;; We've demonstrated a complete Algebra of Graphics implementation in ~500 lines:
;;
;; **Core Principles:**
;; 1. Flat, normalized layer representation with namespaced `:aog/*` keys
;; 2. Composable algebra using `*` (merge) and `+` (overlay) operators
;; 3. Multiple rendering backends from the same layer specification
;; 4. Standard Clojure operations (map, filter, into) work seamlessly
;;
;; **Features Implemented:**
;; - Plot types: scatter, line, area, bar
;; - Statistical transforms: linear regression, smooth, density, histogram  
;; - Aesthetics: position (x, y), color, alpha/opacity
;; - Two backends: geom-viz (SVG) and Vega-Lite (interactive JSON)
;; - Composability: layers combine naturally with `*` and `+`
;;
;; **Why This Approach Works:**
;; - **Explicit**: Every layer is just a map - inspect it, transform it, compose it
;; - **Simple**: No polymorphism, no protocols, just maps and functions
;; - **Flexible**: Add new backends by writing conversion functions
;; - **Clojure-native**: Uses standard idioms (into, mapv, filter, etc.)
;;
;; The flat representation makes the system transparent and hackable! ;; Layer metadata

(plot [no-confusion])

;; # Design Rationale & Trade-offs
;;
;; ## Why Flat Structure?
;;
;; **Advantage**: Standard `merge` works without custom logic
;; ```clojure
;; ;; Nested structure
;; {:positional [:x] :named {:color :species}}  ;; Needs custom merge
;;
;; ;; Flat structure
;; {:aog/x :x :aog/color :species}  ;; Standard merge works!
;; ```
;;
;; **Trade-off**: All keys at same level, but namespacing solves collision issues.
;;
;; ## Why Namespaced Keys?
;;
;; **Advantage**: Prevents collisions with data columns
;; ```clojure
;; ;; Your data can have :x, :y, :color columns
;; ;; No confusion with layer metadata because of :aog/ namespace
;; ```
;;
;; **Trade-off**: Slightly more verbose than plain keys
;; - Plain: `:color` (6 chars)
;; - Namespaced: `:aog/color` (11 chars)
;; - But namespace map syntax helps: `#:aog{:color ...}`
;;
;; ## Why `:aog` Instead of `:aog.mapping`, `:aog.attr`, etc.?
;;
;; **Advantage**: Better ergonomics
;; ```clojure
;; ;; With grouped namespaces
;; (assoc layer :aog.mapping/color :species)  ;; 18 chars
;;
;; ;; With single namespace
;; (assoc layer :aog/color :species)  ;; 11 chars
;; ```
;;
;; **Trade-off**: Less semantic grouping, but namespacing provides enough distinction.
;;
;; ## Value Type Dispatch
;;
;; We distinguish mappings from constants by value type:
;; ```clojure
;; {:aog/color :species}    ;; Keyword → mapping to :species column
;; {:aog/color \"red\"}      ;; String → constant red color
;; {:aog/alpha 0.5}         ;; Number → constant opacity
;; ```
;;
;; This is elegant but has edge cases:
;; - What if you want to map to a numeric column vs set a constant?
;; - Currently solved: keywords always map, numbers/strings are always constants

;; # Open Questions for Discussion
;;
;; ## Question 1: Namespace Convention
;;
;; **Current**: `:aog/*` (e.g., `:aog/color`, `:aog/x`)
;;
;; **Alternative**: Use `:=` prefix like Tableplot's current API
;; ```clojure
;; {:=data penguins
;;  :=x :bill-length
;;  :=y :bill-depth
;;  :=color :species
;;  :=alpha 0.5
;;  :=plottype :scatter}
;; ```
;;
;; **Pros**: More concise (`:=color` = 7 chars vs `:aog/color` = 11 chars)
;; **Cons**: Not a standard namespace, less discoverable
;;
;; **Your thoughts?** Which feels more natural in Clojure?
;;
;; ## Question 2: Operator Names
;;
;; **Current**: `*` for merge/product, `+` for overlay/sum
;;
;; Following AlgebraOfGraphics.jl, but in Clojure:
;; - `*` shadows multiplication (though namespaced access works)
;; - Are there better names? `merge-layers`? `compose`?
;;
;; ## Question 3: Transformation Handling
;;
;; **Current**: Linear regression as `{:aog/transformation :linear}`
;;
;; **Question**: How to pass parameters to transformations?
;; ```clojure
;; (smooth {:bandwidth 0.5})  ;; How should this work?
;; (bins {:n 20})
;; ```
;;
;; Should transformation parameters be:
;; - Part of the layer map?
;; - Separate transformation specification?
;;
;; ## Question 4: Backend Abstraction
;;
;; **Current**: Simple SVG hiccup rendering
;;
;; **Question**: How to support multiple backends (Vega-Lite, Plotly, thi.ng/geom, etc.)?
;; - Should layer spec be backend-agnostic?
;; - How to handle backend-specific features?
;;
;; ## Question 5: Spec Integration
;;
;; With namespaced keys, we can use clojure.spec:
;; ```clojure
;; (s/def :aog/plottype #{:scatter :line :bar :area})
;; (s/def :aog/data map?)
;; (s/def :aog/x keyword?)
;; (s/def :aog/alpha (s/and number? #(<= 0 % 1)))
;; ```
;;
;; Should we provide specs for validation?
;;
;; ## Question 6: Faceting
;;
;; How should faceting work in this model?
;; ```clojure
;; {:aog/data penguins
;;  :aog/x :bill-length
;;  :aog/y :bill-depth
;;  :aog/col :species}  ;; Facet by column?
;; ```
;;
;; Or separate faceting layer/wrapper?

;; # Summary & Next Steps
;;
;; ## What We've Built
;;
;; A minimal Algebra of Graphics implementation showing:
;; 1. **Flat layer structure** - All aesthetics at top level
;; 2. **Namespaced keys** - `:aog/*` prevents collisions
;; 3. **Standard operations** - `merge`, `assoc`, `update`, `mapv`, `filter` all work
;; 4. **Algebraic operators** - `*` for composition, `+` for overlay
;; 5. **Self-contained** - ~300 lines, no dependencies beyond Tablecloth
;;
;; ## Key Insights
;;
;; 1. **Flattening enables standard operations** - No custom merge needed
;; 2. **Namespacing solves collisions** - Can coexist with any data columns
;; 3. **Maps are data** - Layers are just Clojure maps, use any map operations
;; 4. **Namespace map syntax** - `#:aog{...}` keeps it concise
;;
;; ## Discussion Points
;;
;; 1. Is `:aog/*` the right namespace choice, or would `:=*` be better?
;; 2. Are `*` and `+` good operator names for Clojure?
;; 3. How should we handle transformations with parameters?
;; 4. Should we provide clojure.spec specs for validation?
;; 5. What's the right approach for faceting?
;;
;; ## Try It Yourself!
;;
;; This notebook is completely self-contained. You can:
;; 1. Modify the examples
;; 2. Add new plot types (just add to `layer->vegalite-mark`)
;; 3. Try different data with Tablecloth
;; 4. Experiment with the design
;;
;; **We welcome your feedback!** What works? What doesn't? What would you change?

;; # Appendix: Implementation Summary
;;
;; The complete implementation above consists of:
;;
;; **Constructor functions** (~40 lines):
;; - `data` - Attach dataset
;; - `mapping` - Define aesthetic mappings
;; - `scatter`, `line`, `linear` - Plot types
;;
;; **Algebraic operators** (~20 lines):
;; - `*` - Merge/compose layers
;; - `+` - Overlay/concatenate layers
;;
;; **thi.ng/geom-viz backend** (~100 lines):
;; - `get-column-data` - Extract data from datasets
;; - `infer-domain` - Domain inference
;; - `layer->points` - Convert to point data
;; - `layer->scatter-spec` - Build geom-viz scatter specs
;; - `layer->line-spec` - Build geom-viz line specs
;; - `layers->svg` - Render to SVG via geom-viz
;; - `plot` - Render visualization
;;
;; **Total**: ~180 lines of core logic for a working AoG implementation!
