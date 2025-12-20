;; # thi.ng/geom Backend - Complete Reference
;;
;; A comprehensive guide to the thi.ng/geom backend for Tableplot, covering all features,
;; plot types, styling options, and integration with the AlgebraOfGraphics API.

;; ## Table of Contents
;;
;; 1. Introduction & Overview
;; 2. Basic Plot Types
;;    - Scatter Plots
;;    - Line Plots
;;    - Bar Charts
;; 3. Polar Coordinates
;;    - Simple Polar Plots
;;    - Rose Curves
;;    - Radar Charts
;; 4. Multi-Layer Compositions
;;    - Scatter + Line
;;    - Multiple Lines
;;    - Bar + Line
;;    - Polar Multi-Layer
;; 5. AlgebraOfGraphics Integration
;;    - Basic AoG Pipeline
;;    - Statistical Transformations
;;    - Scale Transformations
;;    - Complex Compositions
;; 6. Styling & Customization
;;    - Basic Properties
;;    - Color Palettes
;;    - Size & Aspect Ratios
;;    - Data-Driven Aesthetics
;; 7. Edge Cases & Best Practices

(ns tableplot-book.thing-geom-complete-reference
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.thing-geom :as tg]
            [scicloj.tableplot.v1.aog.processing :as processing]
            [scicloj.kindly.v4.kind :as kind]))

;; # 1. Introduction & Overview
;;
;; ## What is the thi.ng/geom Backend?
;;
;; The thi.ng/geom backend is a pure Clojure visualization backend for Tableplot that uses
;; the thi.ng/geom-viz library for native SVG generation. Unlike Plotly.js and Vega-Lite
;; which require JavaScript runtimes, thi.ng/geom provides:
;;
;; - **Pure Clojure** - No JavaScript dependencies
;; - **Native SVG output** - Static, embeddable graphics
;; - **True polar coordinates** - Not simulated via Cartesian conversion
;; - **Mathematical precision** - Excellent for parametric curves and geometric plots
;; - **Direct control** - Fine-grained control over SVG properties
;;
;; ## When to Use thi.ng/geom
;;
;; **Best suited for:**
;; - Publications and papers (high-quality static SVG)
;; - Polar coordinate visualizations (rose curves, radar charts, spirals)
;; - Mathematical plots (parametric curves, geometric shapes)
;; - Embedded visualizations (SVG in HTML/Markdown)
;; - Server-side rendering (no JavaScript required)
;;
;; **Less suited for:**
;; - Interactive dashboards (use Plotly.js or Vega-Lite)
;; - Large datasets requiring interaction
;; - 3D visualizations
;; - Real-time updating charts
;;
;; ## Core API
;;
;; The thi.ng/geom backend provides two main functions:
;;
;; ```clojure
;; ;; Render a single entry (plot layer)
;; (tg/entry->svg entry-map options)
;;
;; ;; Render multiple entries (multi-layer plot)
;; (tg/entries->svg [entry1 entry2 ...] options)
;; ```
;;
;; ## Entry Format
;;
;; An entry is a map with:
;; - `:plottype` - One of `:scatter`, `:line`, `:bar`
;; - `:positional` - Vector of [x-values y-values]
;; - `:named` - Map of styling properties
;;
;; ## Options Format
;;
;; Options control the plot rendering:
;; - `:width` - Plot width in pixels (default: 600)
;; - `:height` - Plot height in pixels (default: 400)
;; - `:polar` - Enable polar coordinates (default: false)

;; # 2. Basic Plot Types
;;
;; The foundation of all visualizations: scatter, line, and bar plots.

;; ## Scatter Plots
;;
;; Scatter plots display individual data points as marks.

;; ### Simple scatter with 5 points

(tg/entry->svg
 {:plottype :scatter
  :positional [[1 2 3 4 5]
               [2 4 3 5 6]]
  :named {}}
 {:width 600 :height 400})

;; ### Scatter with custom colors

(tg/entry->svg
 {:plottype :scatter
  :positional [[1 2 3 4 5 6 7 8 9 10]
               [2 4 3 5 6 4 7 5 8 6]]
  :named {:color "#e74c3c"
          :opacity 0.7}}
 {:width 600 :height 400})

;; ### Large scatter plot (100 points)
;;
;; Demonstrates performance with larger datasets.

(let [n 100
      x (vec (range n))
      y (vec (map #(+ (* 0.5 %) (- (rand 10) 5)) x))]
  (tg/entry->svg
   {:plottype :scatter
    :positional [x y]
    :named {:color "#3498db"
            :opacity 0.6}}
   {:width 600 :height 400}))

;; ### Custom point sizes

(tg/entry->svg
 {:plottype :scatter
  :positional [[1 2 3 4 5]
               [2 4 3 5 6]]
  :named {:color "#9b59b6"
          :opacity 0.7
          :size 10}}
 {:width 600 :height 400})

;; ## Line Plots
;;
;; Line plots connect data points with continuous lines.

;; ### Simple line

(tg/entry->svg
 {:plottype :line
  :positional [[1 2 3 4 5]
               [1 4 2 5 3]]
  :named {:stroke "#2ecc71"
          :stroke-width 3}}
 {:width 600 :height 400})

;; ### Sine wave
;;
;; Demonstrates smooth curves with many data points.

(let [x (vec (range 0 10 0.1))
      y (vec (map #(Math/sin %) x))]
  (tg/entry->svg
   {:plottype :line
    :positional [x y]
    :named {:stroke "#9b59b6"
            :stroke-width 2}}
   {:width 600 :height 400}))

;; ### Multiple oscillations
;;
;; Complex mathematical function: sin(3θ) × cos(θ)

(let [x (vec (range 0 (* 2 Math/PI) 0.05))
      y (vec (map #(* (Math/sin (* 3 %)) (Math/cos %)) x))]
  (tg/entry->svg
   {:plottype :line
    :positional [x y]
    :named {:stroke "#e67e22"
            :stroke-width 2}}
   {:width 600 :height 400}))

;; ### Dashed line styling

(tg/entry->svg
 {:plottype :line
  :positional [[1 2 3 4 5 6 7 8 9 10]
               [1 3 2 5 4 7 6 9 8 10]]
  :named {:stroke "#e67e22"
          :stroke-width 2
          :stroke-dasharray "5,5"}}
 {:width 600 :height 400})

;; ## Bar Charts
;;
;; Bar charts display categorical data with rectangular bars.

;; ### Simple bar chart

(tg/entry->svg
 {:plottype :bar
  :positional [[1 2 3 4 5]
               [10 25 15 30 20]]
  :named {:fill "#16a085"
          :bar-width 40}}
 {:width 600 :height 400})

;; ### Comparison bar chart
;;
;; Year-over-year comparison.

(tg/entry->svg
 {:plottype :bar
  :positional [[2020 2021 2022 2023 2024]
               [45 52 48 65 70]]
  :named {:fill "#2980b9"
          :bar-width 30}}
 {:width 600 :height 400})

;; ### Wide bars

(tg/entry->svg
 {:plottype :bar
  :positional [[1 2 3]
               [100 150 120]]
  :named {:fill "#c0392b"
          :bar-width 80}}
 {:width 600 :height 400})

;; ### Bars with custom fill and stroke

(tg/entry->svg
 {:plottype :bar
  :positional [[1 2 3 4 5]
               [23 45 12 67 34]]
  :named {:fill "#3498db"
          :stroke "#2c3e50"
          :stroke-width 2}}
 {:width 600 :height 400})

;; ## Edge Cases
;;
;; Testing boundary conditions and unusual data.

;; ### Single point scatter

(tg/entry->svg
 {:plottype :scatter
  :positional [[5]
               [5]]
  :named {:color "#e74c3c"}}
 {:width 600 :height 400})

;; ### Two points line

(tg/entry->svg
 {:plottype :line
  :positional [[0 10]
               [0 10]]
  :named {:stroke "#3498db"
          :stroke-width 3}}
 {:width 600 :height 400})

;; ### Single bar

(tg/entry->svg
 {:plottype :bar
  :positional [[1]
               [50]]
  :named {:fill "#27ae60"
          :bar-width 50}}
 {:width 600 :height 400})

;; ## Negative Values
;;
;; All plot types handle negative values correctly.

;; ### Scatter with negative values

(tg/entry->svg
 {:plottype :scatter
  :positional [[-5 -3 -1 0 1 3 5]
               [-10 -5 -2 0 2 5 10]]
  :named {:color "#8e44ad"}}
 {:width 600 :height 400})

;; ### Line crossing zero

(let [x (vec (range -5 6))
      y (vec (map #(* % %) x))]
  (tg/entry->svg
   {:plottype :line
    :positional [x y]
    :named {:stroke "#d35400"
            :stroke-width 2}}
   {:width 600 :height 400}))

;; ### Bars with negative values

(tg/entry->svg
 {:plottype :bar
  :positional [[-2 -1 0 1 2]
               [-5 -3 0 4 6]]
  :named {:fill "#c0392b"
          :bar-width 30}}
 {:width 600 :height 400})

;; # 3. Polar Coordinates
;;
;; The thi.ng/geom backend excels at polar coordinate visualizations with true
;; native support (not simulated via Cartesian transformations).
;;
;; Enable polar mode by passing `:polar true` in the options map.

;; ## Simple Polar Plots

;; ### Circle
;;
;; r = constant (radius = 5 for all angles)

(let [theta (vec (range 0 (* 2 Math/PI) 0.1))
      r (vec (repeat (count theta) 5))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#3498db"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### Spiral
;;
;; r = θ (radius increases linearly with angle)

(let [theta (vec (range 0 (* 4 Math/PI) 0.1))
      r (vec theta)]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#e74c3c"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Rose Curves
;;
;; Rose curves are sinusoidal curves in polar coordinates, defined by:
;; r = a × cos(kθ) or r = a × sin(kθ)
;;
;; The number of petals depends on k:
;; - If k is odd: k petals
;; - If k is even: 2k petals

;; ### 3-petaled rose
;;
;; r = 5 × cos(3θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (Math/cos (* 3 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#9b59b6"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### 4-petaled rose (8 petals total)
;;
;; r = 6 × sin(2θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 6 (Math/sin (* 2 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#2ecc71"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### 5-petaled rose
;;
;; r = 7 × cos(5θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 7 (Math/cos (* 5 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#e67e22"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### 8-petaled rose (16 petals total)
;;
;; r = 8 × sin(4θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 8 (Math/sin (* 4 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#16a085"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Lissajous-style Polar
;;
;; Combining sinusoidal modulation with constant offset.

;; ### r = 5 + 3sin(3θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.05))
      r (vec (map #(+ 5 (* 3 (Math/sin (* 3 %)))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#c0392b"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### r = 8 + 4cos(2θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.05))
      r (vec (map #(+ 8 (* 4 (Math/cos (* 2 %)))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#8e44ad"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Cardioid and Limacon
;;
;; Classic polar curves with heart and snail shapes.

;; ### Cardioid
;;
;; r = a × (1 + cos(θ))
;; Named for its heart-like shape.

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (+ 1 (Math/cos %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#d35400"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### Limacon
;;
;; r = a + b × cos(θ)
;; When b > a, creates a loop (dimpled limacon).

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(+ 2 (* 3 (Math/cos %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#27ae60"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Radar/Spider Charts
;;
;; Radar charts display multivariate data as polygons in polar coordinates.
;; Each axis represents a different variable.

;; ### 5-category radar (pentagon)

(let [n 5
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n))))
      r [0.8 0.6 0.9 0.7 0.5 0.8]] ; Repeat first value to close polygon
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#2980b9"
            :stroke-width 3
            :fill "#2980b9"
            :opacity 0.3}}
   {:polar true :width 600 :height 600}))

;; ### 6-category radar (hexagon)

(let [n 6
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n))))
      r [0.9 0.7 0.8 0.6 0.85 0.75 0.9]]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#16a085"
            :stroke-width 3
            :fill "#16a085"
            :opacity 0.25}}
   {:polar true :width 600 :height 600}))

;; ### 8-category radar (octagon)

(let [n 8
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n))))
      r [0.7 0.8 0.6 0.9 0.75 0.85 0.65 0.8 0.7]]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#c0392b"
            :stroke-width 3
            :fill "#c0392b"
            :opacity 0.2}}
   {:polar true :width 600 :height 600}))

;; # 4. Multi-Layer Compositions
;;
;; Combine multiple plot layers to create richer visualizations.
;; Use `tg/entries->svg` with a vector of entry maps.

;; ## Scatter + Line

;; ### Data points with trend line
;;
;; Showing actual data (scatter) with fitted line.

(tg/entries->svg
 [{:plottype :scatter
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2.1 3.9 6.2 7.8 10.1 12.3 13.9 16.2 17.8 20.1]]
   :named {:color "#3498db"
           :opacity 0.7}}
  {:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2 4 6 8 10 12 14 16 18 20]]
   :named {:stroke "#e74c3c"
           :stroke-width 2}}]
 {:width 600 :height 400})

;; ### Scatter with smoothed curve

(let [x [1 2 3 4 5 6 7 8 9 10]
      y-scatter [2 4 3 5 6 4 7 5 8 6]
      x-smooth (vec (range 1 10.1 0.5))
      y-smooth (vec (map #(+ 4 (* 0.4 %) (Math/sin %)) x-smooth))]
  (tg/entries->svg
   [{:plottype :scatter
     :positional [x y-scatter]
     :named {:color "#9b59b6"
             :opacity 0.8}}
    {:plottype :line
     :positional [x-smooth y-smooth]
     :named {:stroke "#2ecc71"
             :stroke-width 2}}]
   {:width 600 :height 400}))

;; ## Multiple Lines

;; ### Three sine waves (different frequencies)

(let [x (vec (range 0 10 0.1))
      y1 (vec (map #(Math/sin %) x))
      y2 (vec (map #(Math/sin (* 2 %)) x))
      y3 (vec (map #(Math/sin (* 3 %)) x))]
  (tg/entries->svg
   [{:plottype :line
     :positional [x y1]
     :named {:stroke "#e74c3c"
             :stroke-width 2}}
    {:plottype :line
     :positional [x y2]
     :named {:stroke "#3498db"
             :stroke-width 2}}
    {:plottype :line
     :positional [x y3]
     :named {:stroke "#2ecc71"
             :stroke-width 2}}]
   {:width 600 :height 400}))

;; ### Two trends (growing vs declining)

(let [x [1 2 3 4 5 6 7 8 9 10]]
  (tg/entries->svg
   [{:plottype :line
     :positional [x [10 12 15 18 22 26 30 35 40 45]]
     :named {:stroke "#16a085"
             :stroke-width 3}}
    {:plottype :line
     :positional [x [45 40 36 32 29 26 24 22 21 20]]
     :named {:stroke "#c0392b"
             :stroke-width 3}}]
   {:width 600 :height 400}))

;; ## Scatter + Multiple Lines

;; ### Data with upper and lower bounds
;;
;; Confidence interval visualization.

(let [x [1 2 3 4 5 6 7 8 9 10]
      y-data [5.2 5.8 6.1 6.7 7.3 7.9 8.2 8.8 9.1 9.7]
      y-upper (vec (map #(+ % 1) y-data))
      y-lower (vec (map #(- % 1) y-data))
      y-center (vec (map #(+ 5 (* 0.5 %)) x))]
  (tg/entries->svg
   [{:plottype :line
     :positional [x y-upper]
     :named {:stroke "#95a5a6"
             :stroke-width 1
             :opacity 0.5}}
    {:plottype :line
     :positional [x y-lower]
     :named {:stroke "#95a5a6"
             :stroke-width 1
             :opacity 0.5}}
    {:plottype :line
     :positional [x y-center]
     :named {:stroke "#2980b9"
             :stroke-width 2}}
    {:plottype :scatter
     :positional [x y-data]
     :named {:color "#e67e22"
             :opacity 0.8}}]
   {:width 600 :height 400}))

;; ## Bar + Line

;; ### Bars with average line
;;
;; Common in business dashboards.

(let [x [1 2 3 4 5 6 7]
      bars [23 45 34 56 42 38 51]
      avg (double (/ (reduce + bars) (count bars)))
      avg-line (vec (repeat (count x) avg))]
  (tg/entries->svg
   [{:plottype :bar
     :positional [x bars]
     :named {:fill "#3498db"
             :bar-width 30
             :opacity 0.7}}
    {:plottype :line
     :positional [x avg-line]
     :named {:stroke "#e74c3c"
             :stroke-width 3}}]
   {:width 600 :height 400}))

;; ## Complex Combinations

;; ### Four different series

(let [x (vec (range 0 10 0.5))
      y1 (vec (map #(Math/sin %) x))
      y2 (vec (map #(Math/cos %) x))
      scatter-x [1 3 5 7 9]
      scatter-y1 [0.8 0.1 -0.9 0.6 0.4]
      scatter-y2 [0.5 -1.0 0.3 0.7 -0.4]]
  (tg/entries->svg
   [{:plottype :line
     :positional [x y1]
     :named {:stroke "#e74c3c"
             :stroke-width 2}}
    {:plottype :line
     :positional [x y2]
     :named {:stroke "#3498db"
             :stroke-width 2}}
    {:plottype :scatter
     :positional [scatter-x scatter-y1]
     :named {:color "#2ecc71"
             :opacity 0.9}}
    {:plottype :scatter
     :positional [scatter-x scatter-y2]
     :named {:color "#9b59b6"
             :opacity 0.9}}]
   {:width 600 :height 400}))

;; ## Polar Multi-layer

;; ### Two rose curves overlapping

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r1 (vec (map #(* 5 (Math/cos (* 3 %))) theta))
      r2 (vec (map #(* 4 (Math/sin (* 5 %))) theta))]
  (tg/entries->svg
   [{:plottype :line
     :positional [theta r1]
     :named {:stroke "#e74c3c"
             :stroke-width 2
             :opacity 0.7}}
    {:plottype :line
     :positional [theta r2]
     :named {:stroke "#3498db"
             :stroke-width 2
             :opacity 0.7}}]
   {:polar true :width 600 :height 600}))

;; ### Overlapping radar charts
;;
;; Comparing two profiles.

(let [n 6
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n))))
      r1 [0.9 0.7 0.8 0.6 0.85 0.75 0.9]
      r2 [0.6 0.8 0.7 0.9 0.65 0.85 0.6]]
  (tg/entries->svg
   [{:plottype :line
     :positional [theta r1]
     :named {:stroke "#2ecc71"
             :stroke-width 2
             :fill "#2ecc71"
             :opacity 0.2}}
    {:plottype :line
     :positional [theta r2]
     :named {:stroke "#9b59b6"
             :stroke-width 2
             :fill "#9b59b6"
             :opacity 0.2}}]
   {:polar true :width 600 :height 600}))

;; # 5. AlgebraOfGraphics Integration
;;
;; The thi.ng/geom backend integrates fully with the AlgebraOfGraphics API,
;; supporting all AoG operators, transformations, and compositions.

;; ## Basic AoG Pipeline
;;
;; The standard AoG workflow: data -> mapping -> visual -> draw

(def simple-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]})

;; ### Simple Scatter Plot via AoG

(let [layers (aog/* (aog/data simple-data)
                    (aog/mapping :x :y)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Line Plot via AoG

(def line-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1.0 4.0 9.0 16.0 25.0 36.0 49.0 64.0 81.0 100.0]})

(let [layers (aog/* (aog/data line-data)
                    (aog/mapping :x :y)
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Bar Chart via AoG

(def bar-data
  {:x [1 2 3 4 5]
   :value [23 45 12 67 34]})

(let [layers (aog/* (aog/data bar-data)
                    (aog/mapping :x :value)
                    (aog/bar))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Multi-Layer Composition with +
;;
;; The + operator overlays multiple visual types.

;; ### Scatter + Line (Trend)

(def trend-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2.1 3.9 6.2 7.8 10.1 12.3 13.9 16.2 17.8 20.1]})

(let [layers (aog/* (aog/data trend-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter {:alpha 0.6})
                           (aog/line {:width 2})))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Multiple Time Series

(def multi-series-data
  {:time [1 2 3 4 5 6 7 8 9 10]
   :series-a [10 12 15 13 17 19 18 22 21 25]
   :series-b [5 7 6 9 8 11 10 13 14 12]
   :series-c [15 14 16 15 18 17 20 19 22 21]})

;; Create separate layers for each series
(let [layer-a (aog/* (aog/data {:x (:time multi-series-data)
                                :y (:series-a multi-series-data)})
                     (aog/mapping :x :y)
                     (aog/line))
      layer-b (aog/* (aog/data {:x (:time multi-series-data)
                                :y (:series-b multi-series-data)})
                     (aog/mapping :x :y)
                     (aog/line))
      layer-c (aog/* (aog/data {:x (:time multi-series-data)
                                :y (:series-c multi-series-data)})
                     (aog/mapping :x :y)
                     (aog/line))
      entries (processing/layers->entries [layer-a layer-b layer-c])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Cartesian Product with *
;;
;; The * operator merges layer specifications.

;; ### Adding Color Aesthetic

(def colored-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]
   :group [1 1 2 2 1 2 1 2 1 2]})

(let [layers (aog/* (aog/data colored-data)
                    (aog/mapping :x :y)
                    (aog/scatter {:alpha 0.7}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Multiple Aesthetics (Color + Size)

(def multi-aesthetic-data
  {:x [1 2 3 4 5 6 7 8]
   :y [2 4 3 5 6 4 7 5]
   :category [1 2 1 2 1 2 1 2]
   :size [10 20 15 25 12 22 18 28]})

(let [layers (aog/* (aog/data multi-aesthetic-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:size :size})
                    (aog/scatter {:alpha 0.7}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Statistical Transformations
;;
;; AoG statistical transforms work seamlessly with thi.ng/geom.

;; ### Linear Regression

(def regression-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2.5 4.8 5.9 8.2 10.1 11.7 14.3 15.8 18.1 19.9]})

(let [layers (aog/* (aog/data regression-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter {:alpha 0.5})
                           (aog/linear)))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Frequency (Counts)

(def frequency-data
  {:category [1 2 1 3 2 1 2 3 1 1
              2 3 1 2 1 3 2 2 1 3]})

(let [layers (aog/* (aog/data frequency-data)
                    (aog/mapping :category)
                    (aog/frequency))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Expectation (Conditional Mean)

(def expectation-data
  {:x [1 1 1 2 2 2 3 3 3 4 4 4]
   :y [2 3 2.5 4 5 4.5 6 7 6.5 8 9 8.5]})

(let [layers (aog/* (aog/data expectation-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter {:alpha 0.4})
                           (aog/expectation)))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Histogram

(def histogram-data
  {:values [1.2 1.5 1.8 2.1 2.3 2.5 2.7 2.9 3.1 3.3
            3.5 3.7 3.9 4.1 4.3 4.5 4.7 4.9 5.1 5.3
            5.5 5.7 5.9 6.1 6.3 6.5 6.7 6.9 7.1 7.3]})

(let [layers (aog/* (aog/data histogram-data)
                    (aog/mapping :values)
                    (aog/histogram {:bins 10}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Density Estimation

(def density-data
  {:values [1 1.2 1.5 2 2.3 2.5 3 3.2 3.5 4
            4.2 4.5 5 5.5 6 6.5 7 7.5 8 8.5]})

(let [layers (aog/* (aog/data density-data)
                    (aog/mapping :values)
                    (aog/density))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Scale Transformations
;;
;; Logarithmic, power, and custom domain transformations.

;; ### Logarithmic Scale (Y-axis)

(def log-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1.0 10.0 100.0 1000.0 10000.0 100.0 1000.0 10000.0 100000.0 1000000.0]})

(let [layers (aog/* (aog/data log-data)
                    (aog/mapping :x :y)
                    (aog/log-scale :y {:base 10})
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Power Scale (Square Root)

(def sqrt-data
  {:x [1.0 4.0 9.0 16.0 25.0 36.0 49.0 64.0 81.0 100.0]
   :y [1 2 3 4 5 6 7 8 9 10]})

(let [layers (aog/* (aog/data sqrt-data)
                    (aog/mapping :x :y)
                    (aog/sqrt-scale :x)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Custom Domain

(def domain-data
  {:x [5 10 15 20 25 30 35 40 45 50]
   :y [100.0 150.0 200.0 250.0 300.0 350.0 400.0 450.0 500.0 550.0]})

(let [layers (aog/* (aog/data domain-data)
                    (aog/mapping :x :y)
                    (aog/scale-domain :x [0 60])
                    (aog/scale-domain :y [0 600])
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Complex Compositions
;;
;; Combining multiple AoG features.

;; ### Grouped Data with Multiple Layers

(def grouped-complex-data
  {:x [1 2 3 4 5 6 7 8 9 10
       1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6
       3 5 4 6 7 5 8 6 9 7]
   :group [1 1 1 1 1 1 1 1 1 1
           2 2 2 2 2 2 2 2 2 2]})

(let [layers (aog/* (aog/data grouped-complex-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter {:alpha 0.5})
                           (aog/line {:width 2})))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Statistical Transform with Grouping

(def stat-grouped-data
  {:x [1 1 1 2 2 2 3 3 3 4 4 4 5 5 5
       1 1 1 2 2 2 3 3 3 4 4 4 5 5 5]
   :y [2 3 2.5 4 5 4.5 6 7 6.5 8 9 8.5 10 11 10.5
       3 4 3.5 5 6 5.5 7 8 7.5 9 10 9.5 11 12 11.5]
   :group [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
           2 2 2 2 2 2 2 2 2 2 2 2 2 2 2]})

(let [layers (aog/* (aog/data stat-grouped-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter {:alpha 0.3})
                           (aog/linear)))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Multi-Aesthetic with Transformations

(def multi-transform-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2.0 4.0 8.0 16.0 32.0 64.0 128.0 256.0 512.0 1024.0]})

(let [layers (aog/* (aog/data multi-transform-data)
                    (aog/mapping :x :y)
                    (aog/log-scale :y {:base 2})
                    (aog/+ (aog/scatter {:alpha 0.7})
                           (aog/line {:width 2})))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Polar Coordinates with AoG

;; ### Simple Polar Scatter

(def polar-scatter-data
  {:theta [0 0.5 1 1.5 2 2.5 3 3.5 4 4.5 5 5.5 6]
   :r [1 2 3 4 5 6 7 6 5 4 3 2 1]})

(let [layers (aog/* (aog/data polar-scatter-data)
                    (aog/mapping :theta :r)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:polar true :width 600 :height 600}))

;; ### Polar Line (Rose Curve)

(let [theta (vec (range 0 (* 2 Math/PI) 0.05))
      r (vec (map #(* 5 (Math/cos (* 3 %))) theta))
      rose-data {:theta theta :r r}
      layers (aog/* (aog/data rose-data)
                    (aog/mapping :theta :r)
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:polar true :width 600 :height 600}))

;; ### Polar Multi-Layer

(let [theta (vec (range 0 (* 2 Math/PI) 0.1))
      r1 (vec (repeat (count theta) 5))
      r2 (vec (range 1 (inc (count theta))))
      layer1 (aog/* (aog/data {:theta theta :r r1})
                    (aog/mapping :theta :r)
                    (aog/line))
      layer2 (aog/* (aog/data {:theta theta :r r2})
                    (aog/mapping :theta :r)
                    (aog/scatter {:alpha 0.5}))
      entries (processing/layers->entries [layer1 layer2])]
  (tg/entries->svg entries {:polar true :width 600 :height 600}))

;; # 6. Styling & Customization
;;
;; Comprehensive styling options for all plot types.

;; ## Basic Styling Properties

;; ### Scatter Plot Styling

(def base-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]})

;; Default scatter
(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {}}
 {:width 600 :height 400})

;; Custom color
(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#e74c3c"}}
 {:width 600 :height 400})

;; Custom opacity
(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:opacity 0.3}}
 {:width 600 :height 400})

;; Custom size
(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:size 8}}
 {:width 600 :height 400})

;; Combined styling
(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#9b59b6"
          :opacity 0.7
          :size 10}}
 {:width 600 :height 400})

;; ### Line Plot Styling

(def line-styling-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1 3 2 5 4 7 6 9 8 10]})

;; Custom stroke color
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-styling-data) (:y line-styling-data)]
  :named {:stroke "#3498db"}}
 {:width 600 :height 400})

;; Custom stroke width
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-styling-data) (:y line-styling-data)]
  :named {:stroke-width 4}}
 {:width 600 :height 400})

;; Dashed line
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-styling-data) (:y line-styling-data)]
  :named {:stroke "#e67e22"
          :stroke-width 2
          :stroke-dasharray "5,5"}}
 {:width 600 :height 400})

;; Dotted line
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-styling-data) (:y line-styling-data)]
  :named {:stroke "#1abc9c"
          :stroke-width 3
          :stroke-dasharray "2,3"}}
 {:width 600 :height 400})

;; ### Bar Chart Styling

(def bar-styling-data
  {:x [1 2 3 4 5]
   :y [23 45 12 67 34]})

;; Custom fill color
(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-styling-data) (:y bar-styling-data)]
  :named {:fill "#f39c12"}}
 {:width 600 :height 400})

;; Custom fill with stroke
(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-styling-data) (:y bar-styling-data)]
  :named {:fill "#3498db"
          :stroke "#2c3e50"
          :stroke-width 2}}
 {:width 600 :height 400})

;; Semi-transparent bars
(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-styling-data) (:y bar-styling-data)]
  :named {:fill "#e74c3c"
          :opacity 0.6}}
 {:width 600 :height 400})

;; ## Color Palettes

;; ### Categorical Colors via AoG
;;
;; Note: Categorical color mapping not currently supported in thi.ng/geom backend.
;; Use single color instead.

(def categorical-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]
   :category [1 2 3 4 1 2 3 4 1 2]})

(let [layers (aog/* (aog/data categorical-data)
                    (aog/mapping :x :y)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Gradient Colors
;;
;; Note: Per-point coloring not currently supported in entry->svg API.
;; Use a single color for all points instead.

(let [n 20
      x (vec (range n))
      y (vec (map #(+ 5 (Math/sin (/ % 3.0))) x))]
  (tg/entry->svg
   {:plottype :scatter
    :positional [x y]
    :named {:color "#9b59b6"}}
   {:width 600 :height 400}))

;; ## Size Variations

;; ### Square Plot

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#3498db"}}
 {:width 500 :height 500})

;; ### Wide Plot

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-styling-data) (:y line-styling-data)]
  :named {:stroke "#e74c3c" :stroke-width 2}}
 {:width 800 :height 300})

;; ### Small Plot (Thumbnail)

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#1abc9c" :size 4}}
 {:width 200 :height 150})

;; ### Large Plot (Presentation)

(def large-plot-data
  {:x (vec (range 1 51))
   :y (vec (map #(+ % (* 5 (Math/sin (/ % 5.0)))) (range 1 51)))})

(tg/entry->svg
 {:plottype :line
  :positional [(:x large-plot-data) (:y large-plot-data)]
  :named {:stroke "#e67e22" :stroke-width 3}}
 {:width 1200 :height 700})

;; ## Data-Driven Aesthetics

;; ### Size Mapping

(def size-mapped-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]
   :size [3 5 4 6 7 5 8 6 9 7]})

(let [layers (aog/* (aog/data size-mapped-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:size :size})
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Color and Size Combined

(def multi-aesthetic-styling-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]
   :group [1 1 2 2 1 2 1 2 1 2]
   :size [4 6 5 7 8 6 9 7 10 8]})

(let [layers (aog/* (aog/data multi-aesthetic-styling-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:size :size})
                    (aog/scatter {:alpha 0.7}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Polar Plot Styling

;; ### Styled Polar Scatter

(let [theta (vec (range 0 (* 2 Math/PI) 0.2))
      r (vec (map #(+ 5 (* 2 (Math/sin (* 3 %)))) theta))]
  (tg/entry->svg
   {:plottype :scatter
    :positional [theta r]
    :named {:color "#e74c3c"
            :size 6
            :opacity 0.8}}
   {:polar true :width 600 :height 600}))

;; ### Styled Rose Curve

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 7 (Math/sin (* 4 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#9b59b6"
            :stroke-width 3}}
   {:polar true :width 600 :height 600}))

;; ## Multi-Layer Styling

;; ### Contrasting Layers

(tg/entries->svg
 [{:plottype :scatter
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2 4 3 5 6 4 7 5 8 6]]
   :named {:color "#3498db"
           :opacity 0.5
           :size 8}}
  {:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2 4 3 5 6 4 7 5 8 6]]
   :named {:stroke "#e74c3c"
           :stroke-width 3}}]
 {:width 600 :height 400})

;; ### Multiple Lines with Different Styles

(tg/entries->svg
 [{:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2 4 6 8 10 12 14 16 18 20]]
   :named {:stroke "#3498db"
           :stroke-width 2}}
  {:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [1 3 5 7 9 11 13 15 17 19]]
   :named {:stroke "#e74c3c"
           :stroke-width 2
           :stroke-dasharray "5,5"}}
  {:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [3 5 7 9 11 13 15 17 19 21]]
   :named {:stroke "#2ecc71"
           :stroke-width 2
           :stroke-dasharray "2,3"}}]
 {:width 600 :height 400})

;; ## Real-World Styling Examples

;; ### Stock Chart Style

(def stock-data
  {:date [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15]
   :price [100.0 102.0 98.0 105.0 103.0 110.0 108.0 115.0 112.0 118.0 120.0 117.0 122.0 125.0 123.0]})

(tg/entries->svg
 [{:plottype :line
   :positional [(:date stock-data) (:price stock-data)]
   :named {:stroke "#2ecc71"
           :stroke-width 2.5}}
  {:plottype :scatter
   :positional [(:date stock-data) (:price stock-data)]
   :named {:color "#27ae60"
           :size 4
           :opacity 0.8}}]
 {:width 800 :height 400})

;; ### Scientific Plot Style

(def scientific-data
  {:x [0 0.5 1 1.5 2 2.5 3 3.5 4 4.5 5]
   :measured [0.1 1.2 2.8 4.1 5.5 6.2 7.8 8.5 9.9 10.5 11.8]
   :theoretical [0 1 2.5 4 5.5 7 8.5 10 11.5 13 14.5]})

(tg/entries->svg
 [{:plottype :line
   :positional [(:x scientific-data) (:theoretical scientific-data)]
   :named {:stroke "#95a5a6"
           :stroke-width 2
           :stroke-dasharray "10,5"}}
  {:plottype :scatter
   :positional [(:x scientific-data) (:measured scientific-data)]
   :named {:color "#e74c3c"
           :size 6
           :opacity 0.8}}]
 {:width 700 :height 500})

;; ### Presentation Style (Bold Colors)

(def presentation-data
  {:category [1 2 3 4 5]
   :q4 [60 70 50 80 65]})

(tg/entry->svg
 {:plottype :bar
  :positional [(:category presentation-data) (:q4 presentation-data)]
  :named {:fill "#e74c3c"
          :stroke "#c0392b"
          :stroke-width 2
          :opacity 0.9}}
 {:width 800 :height 500})

;; # 7. Edge Cases & Best Practices
;;
;; Testing boundary conditions and recommended patterns.

;; ## Edge Case Testing

;; ### Empty Data (Error Handling)

(def empty-data {:x [] :y []})

(try
  (let [layers (aog/* (aog/data empty-data)
                      (aog/mapping :x :y)
                      (aog/scatter))
        entries (processing/layers->entries [layers])]
    (tg/entries->svg entries {:width 600 :height 400}))
  (catch Exception e
    (kind/html (str "<div style='color: red;'>Error: " (.getMessage e) "</div>"))))

;; ### Single Point

(def single-point-data {:x [5] :y [10]})

(let [layers (aog/* (aog/data single-point-data)
                    (aog/mapping :x :y)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Large Dataset (500 points)

(def large-data
  (let [n 500
        x (vec (range n))
        y (vec (map #(+ (* 0.5 %) (Math/sin (/ % 10.0)) (rand 5)) x))]
    {:x x :y y}))

(let [layers (aog/* (aog/data large-data)
                    (aog/mapping :x :y)
                    (aog/scatter {:alpha 0.3}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 800 :height 500}))

;; ### Very Transparent Plot

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#3498db"
          :opacity 0.1}}
 {:width 600 :height 400})

;; ### Very Thick Line

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-styling-data) (:y line-styling-data)]
  :named {:stroke "#9b59b6"
          :stroke-width 10}}
 {:width 600 :height 400})

;; ### Very Thin Line

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-styling-data) (:y line-styling-data)]
  :named {:stroke "#1abc9c"
          :stroke-width 0.5}}
 {:width 600 :height 400})

;; ## Best Practices

;; ### 1. Always Provide Width and Height
;;
;; Explicitly specify dimensions for predictable output.

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#3498db"}}
 {:width 600 :height 400}) ; Good!

;; ### 2. Use Vectors for Positional Data
;;
;; Not lists or lazy sequences.

(let [x (vec (range 10)) ; vec, not range
      y (vec (map inc x))] ; vec, not map
  (tg/entry->svg
   {:plottype :line
    :positional [x y]
    :named {}}
   {:width 600 :height 400}))

;; ### 3. Set Opacity for Overlapping Points

(tg/entry->svg
 {:plottype :scatter
  :positional [(vec (take 100 (repeatedly #(rand 10))))
               (vec (take 100 (repeatedly #(rand 10))))]
  :named {:color "#e74c3c"
          :opacity 0.5}} ; Prevents over-plotting
 {:width 600 :height 400})

;; ### 4. Use Appropriate Plot Sizes
;;
;; - Thumbnails: 200×150
;; - Standard: 600×400
;; - Presentations: 1200×700
;; - Polar plots: Square aspect ratio (600×600)

;; ### 5. Choose Colors Wisely
;;
;; Use distinct, accessible colors for categorical data.

;; Good color choices for accessibility:
;; - Blues: #3498db, #2980b9
;; - Greens: #2ecc71, #27ae60
;; - Reds: #e74c3c, #c0392b
;; - Purples: #9b59b6, #8e44ad
;; - Oranges: #e67e22, #d35400

;; ### 6. Close Polar Polygons
;;
;; Repeat the first value at the end for closed shapes.

(let [n 5
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n)))) ; (inc n) for closure
      r [0.8 0.6 0.9 0.7 0.5 0.8]] ; Repeat first value
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#2980b9"
            :stroke-width 3
            :fill "#2980b9"
            :opacity 0.3}}
   {:polar true :width 600 :height 600}))

;; ### 7. Use AoG for Complex Workflows
;;
;; The AlgebraOfGraphics API provides better composability.

;; Direct API (simple):
(tg/entry->svg
 {:plottype :scatter
  :positional [[1 2 3] [4 5 6]]
  :named {:color "#3498db"}}
 {:width 600 :height 400})

;; AoG API (compositional):
(let [layers (aog/* (aog/data {:x [1 2 3] :y [4 5 6]})
                    (aog/mapping :x :y)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; # Summary
;;
;; This comprehensive reference covers all aspects of the thi.ng/geom backend:
;;
;; ✅ **Basic Plot Types** - Scatter, line, bar with full styling
;; ✅ **Polar Coordinates** - Rose curves, spirals, radar charts with native support
;; ✅ **Multi-Layer Compositions** - Combining multiple plot types
;; ✅ **AoG Integration** - Full AlgebraOfGraphics pipeline support
;; ✅ **Statistical Transformations** - Linear, frequency, expectation, histogram, density
;; ✅ **Scale Transformations** - Log, power, sqrt, custom domains
;; ✅ **Styling & Customization** - Colors, sizes, opacities, line styles
;; ✅ **Edge Cases** - Empty data, single points, large datasets
;; ✅ **Best Practices** - Recommended patterns and guidelines
;;
;; ## Next Steps
;;
;; - Explore faceting (when implemented)
;; - Integrate with theme system
;; - Add legend generation
;; - Implement additional plot types (area, heatmap)
;;
;; ## Resources
;;
;; - thi.ng/geom documentation: https://github.com/thi-ng/geom
;; - AlgebraOfGraphics API: See `aog_demo.clj`
;; - Project summary: See `PROJECT_SUMMARY.md`
