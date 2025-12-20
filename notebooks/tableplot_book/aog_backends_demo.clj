;; # AlgebraOfGraphics: Backend Comparison & Demo
;;
;; This notebook demonstrates the AlgebraOfGraphics (AoG) API across different backends,
;; showing how the same grammar works with Plotly.js and thi.ng/geom.
;;
;; **Key Features:**
;; - Algebraic composition with `*` (merge) and `+` (overlay)
;; - Backend-agnostic IR (Entry format)
;; - Pipeline transparency (Layer → ProcessedLayer → Entry → Backend)
;; - Multiple rendering backends

(ns tableplot-book.aog-backends-demo
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.processing :as proc]
            [scicloj.tableplot.v1.aog.thing-geom :as thing-geom]
            [scicloj.tableplot.v1.aog.plotly :as aog-plotly]
            [scicloj.tableplot.v1.aog.ir :as ir]
            [scicloj.tableplot.v1.aog.scales :as scales]
            [tablecloth.api :as tc]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

;; # Part 1: Introduction to AlgebraOfGraphics
;;
;; The AoG API provides a composable way to build visualizations using algebraic operations:
;;
;; - `*` (multiplication) - Merge layer specifications
;; - `+` (addition) - Overlay multiple layers
;; - `data` - Attach data to a layer
;; - `mapping` - Specify aesthetic mappings
;; - `scatter`, `line`, `bar`, etc. - Specify plot types
;; - `linear`, `smooth`, `density`, `histogram` - Statistical transformations
;; - `draw` - Render the specification

;; ## Basic Example: Simple Scatter Plot

(def simple-data
  {:x [1 2 3 4 5]
   :y [2 4 3 5 6]})

;; Create a scatter plot by composing layers with `*`:

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/scatter)))

;; ## Adding Visual Attributes

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/scatter {:alpha 0.5})))

;; ## Aesthetic Mappings

(def colored-data
  {:x [1 2 3 4 5 6]
   :y [2 4 3 5 6 4]
   :species ["A" "A" "B" "B" "C" "C"]})

(aog/draw
 (aog/* (aog/data colored-data)
        (aog/mapping :x :y {:color :species})
        (aog/scatter {:alpha 0.7}))
 {:layout {:title "Colored by Species"}})

;; ## Overlaying Layers with `+`

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/line {:width 2}))))

;; # Part 2: Working with Tablecloth Datasets

(def penguins-data
  (tc/dataset {:bill-length [39.1 39.5 40.3 36.7 39.3 38.9 39.2 34.1 42.0 37.8
                             41.1 38.6 34.6 36.6 37.3 35.7 41.3 37.6 41.1 36.4]
               :bill-depth [18.7 17.4 18.0 19.3 20.6 17.8 19.6 18.1 20.2 17.1
                            17.6 21.2 21.1 17.8 19.7 16.9 21.1 19.3 19.0 17.0]
               :species ["Adelie" "Adelie" "Adelie" "Adelie" "Adelie"
                         "Adelie" "Adelie" "Adelie" "Adelie" "Adelie"
                         "Chinstrap" "Chinstrap" "Chinstrap" "Chinstrap" "Chinstrap"
                         "Gentoo" "Gentoo" "Gentoo" "Gentoo" "Gentoo"]}))

;; Simple scatter with tablecloth:

(aog/draw
 (aog/* (aog/data penguins-data)
        (aog/mapping :bill-length :bill-depth)
        (aog/scatter))
 {:layout {:title "Penguin Bill Measurements"
           :xaxis {:title "Bill Length (mm)"}
           :yaxis {:title "Bill Depth (mm)"}}})

;; Color by species:

(aog/draw
 (aog/* (aog/data penguins-data)
        (aog/mapping :bill-length :bill-depth {:color :species})
        (aog/scatter {:alpha 0.7}))
 {:layout {:title "Penguins by Species"}})

;; # Part 3: Statistical Transformations

;; ## Linear Regression

(aog/draw
 (aog/* (aog/data penguins-data)
        (aog/mapping :bill-length :bill-depth)
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear)))
 {:layout {:title "Scatter with Linear Fit"}})

;; ## Grouped Linear Regression
;;
;; The real power: transformations respect grouping by categorical aesthetics!

(aog/draw
 (aog/* (aog/data penguins-data)
        (aog/mapping :bill-length :bill-depth {:color :species})
        (aog/+ (aog/scatter {:alpha 0.6})
               (aog/linear)))
 {:layout {:title "Linear Regression by Species"}})

;; ## Smoothing

(aog/draw
 (aog/* (aog/data penguins-data)
        (aog/mapping :bill-length :bill-depth {:color :species})
        (aog/+ (aog/scatter {:alpha 0.4})
               (aog/smooth)))
 {:layout {:title "Smooth Curves by Species"}})

;; ## Density Estimation

(aog/draw
 (aog/* (aog/data penguins-data)
        (aog/mapping :bill-length)
        (aog/density))
 {:layout {:title "Density of Bill Length"}})

;; ## Histogram

(aog/draw
 (aog/* (aog/data penguins-data)
        (aog/mapping :bill-length)
        (aog/histogram {:bins 15}))
 {:layout {:title "Histogram of Bill Length"}})

;; # Part 4: Composition Example

(def base
  (aog/* (aog/data penguins-data)
         (aog/mapping :bill-length :bill-depth)))

(def with-color
  (aog/* base
         (aog/mapping {:color :species})))

(def with-layers
  (aog/* with-color
         (aog/+ (aog/scatter {:alpha 0.5})
                (aog/linear))))

;; Draw the final composition:
(aog/draw with-layers
          {:layout {:title "Composed Visualization"}})

;; # Part 5: Pipeline Inspection
;;
;; We can inspect intermediate representations to understand the pipeline:

;; ## Layer Structure

(def simple-layer
  (aog/* (aog/data simple-data)
         (aog/mapping :x :y)
         (aog/scatter {:alpha 0.5})))

simple-layer

;; ## ProcessedLayer (with scale information)

(def processed-layer
  (proc/layer->processed-layer simple-layer))

processed-layer

;; ## Entry IR (Backend-Agnostic)

(def my-entry
  (proc/layer->entry simple-layer))

my-entry

;; ## Plotly Trace Specification

(def my-trace
  (aog-plotly/entry->plotly-trace my-entry))

my-trace

;; ## Scale Inference

(def my-layer-with-color
  (aog/* (aog/data penguins-data)
         (aog/mapping :bill-length :bill-depth {:color :species})
         (aog/scatter {:alpha 0.7})))

(def axis-entries (proc/layers->axis-entries [my-layer-with-color]))

;; View the inferred categorical scales:
(:categorical-scales axis-entries)

;; # Part 6: thi.ng/geom Backend

(md "## Why thi.ng/geom?

The thi.ng/geom backend offers unique advantages:

- ✅ **Pure Clojure** - No JavaScript runtime required
- ✅ **Native polar coordinates** - True polar rendering (not simulated)
- ✅ **SVG output** - Static, embeddable graphics for papers/reports
- ✅ **Server-side rendering** - Generate visualizations without a browser

**Best for**: Scientific papers, polar plots, embedded SVGs, server-side rendering")

;; ## Basic Plots with thi.ng/geom
;;
;; Using the Entry IR directly with thi.ng/geom backend:

;; ### Scatter Plot

(thing-geom/entry->svg
 {:plottype :scatter
  :positional [[1 2 3 4 5]
               [2 4 3 5 6]]
  :named {}}
 {:width 600 :height 400})

;; ### Line Plot (Sine Wave)

(let [x (range 0 10 0.1)
      y (map #(Math/sin %) x)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec x) (vec y)]
    :named {:stroke "#0af"
            :stroke-width 2}}))

;; ### Bar Chart

(thing-geom/entry->svg
 {:plottype :bar
  :positional [[1 2 3 4 5]
               [23 45 34 56 42]]
  :named {:fill "#5E9B4D"
          :bar-width 30}})

;; ## AoG Integration with thi.ng/geom

(let [layer (aog/* (aog/data simple-data)
                   (aog/mapping :x :y)
                   (aog/scatter))
      entries (proc/layers->entries [layer])]
  (thing-geom/entries->svg entries))

;; ## Multi-layer Composition

(let [data {:x [1 2 3 4 5]
            :y [2 4 3 5 6]}
      layers [(aog/* (aog/data data)
                     (aog/mapping :x :y)
                     (aog/scatter))
              (aog/* (aog/data data)
                     (aog/mapping :x :y)
                     (aog/line))]
      entries (proc/layers->entries layers)]
  (thing-geom/entries->svg entries))

;; # Part 7: Polar Coordinates
;;
;; This is where thi.ng/geom truly shines!

(md "## Polar Coordinate Excellence

Native polar coordinate support with automatic polar axes.")

;; ## Rose Diagram (3-petaled)

(let [theta (range 0 (* 2 Math/PI) 0.1)
      r (map (fn [t] (+ 5 (* 3 (Math/sin (* 3 t))))) theta)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) (vec r)]
    :named {:stroke "#0af"
            :stroke-width 2}}
   {:polar true
    :width 600
    :height 600}))

;; ## Radar Chart

(let [categories [0 1 2 3 4]
      values [0.8 0.6 0.9 0.7 0.5]
      theta (map #(* % (/ (* 2 Math/PI) 5)) categories)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) values]
    :named {:stroke "#5E9B4D"
            :stroke-width 3
            :fill "#5E9B4D"
            :opacity 0.3}}
   {:polar true
    :width 600
    :height 600}))

;; ## 5-Petaled Rose

(let [theta (range 0 (* 2 Math/PI) 0.01)
      r (map (fn [t] (* 8 (Math/cos (* 5 t)))) theta)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) (vec r)]
    :named {:stroke "#e74c3c"
            :stroke-width 2}}
   {:polar true
    :width 600
    :height 600}))

;; ## Spiral

(let [theta (range 0 (* 4 Math/PI) 0.1)
      r theta]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) (vec r)]
    :named {:stroke "#9b59b6"
            :stroke-width 2}}
   {:polar true
    :width 600
    :height 600}))

;; ## Cardioid (Heart Shape)

(let [theta (range 0 (* 2 Math/PI) 0.01)
      r (map #(* 5 (+ 1 (Math/cos %))) theta)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) (vec r)]
    :named {:stroke "#d35400"
            :stroke-width 2}}
   {:polar true
    :width 600
    :height 600}))

;; # Part 8: Backend Comparison

(md "## Backend Comparison Table

| Feature | Plotly.js | thi.ng/geom |
|---------|-----------|-------------|
| **Interactivity** | ✅ Excellent (zoom, pan, hover) | ❌ Static SVG only |
| **Polar coordinates** | ⚠️ Limited | ✅ **Excellent** ✨ |
| **3D plots** | ✅ Full support | ❌ 2D only |
| **Dependencies** | cljplotly (JS) | Pure Clojure |
| **Output format** | HTML + JS | SVG |
| **Server-side** | ⚠️ Requires browser | ✅ **Pure Clojure** ✨ |
| **File size** | Large (JS libs) | Small (SVG) |
| **Best for** | Dashboards, exploration | Papers, reports, polar plots |

## When to Use Which Backend

### Use **Plotly** for:
- Interactive dashboards
- Data exploration
- 3D visualizations
- Web applications
- Real-time updates

### Use **thi.ng/geom** for:
- Scientific papers (static SVG)
- **Polar coordinate plots** (rose diagrams, radar charts)
- Server-side rendering (no JS)
- Embedded graphics
- PDF generation
- Mathematical visualizations")

;; # Part 9: Design Validation

(md "## Key Design Principles

This demo validates the following design principles:

1. **Plain maps + Malli validation** - Flexible and type-checked
2. **Backend-agnostic IR** - Entry format works with any backend
3. **Algebraic composition** - `*` distributes over `+`
4. **Multi-format data support** - Works with maps and tablecloth
5. **Pipeline transparency** - Can inspect each stage
6. **Grouped transformations** - Statistical transforms respect aesthetics
7. **Automatic scale inference** - Categorical and continuous scales")

;; # Summary
;;
;; This notebook demonstrates the AlgebraOfGraphics API across two backends:
;;
;; **Plotly Backend:**
;; - Interactive visualizations
;; - Full feature support (scatter, line, bar, regression, density, etc.)
;; - Grouped transformations
;; - Automatic scale inference
;; - Best for dashboards and exploration
;;
;; **thi.ng/geom Backend:**
;; - Pure Clojure SVG generation
;; - **Excellent polar coordinate support** (rose diagrams, radar charts, spirals)
;; - Static output for papers/reports
;; - Server-side rendering
;; - Best for scientific papers and polar plots
;;
;; Both backends use the same AoG grammar and Entry IR, demonstrating true
;; backend independence!

(kind/md "**Demo Complete!** ✅")
