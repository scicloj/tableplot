;; # thi.ng/geom Backend - Complete Guide
;;
;; Comprehensive guide to using the thi.ng/geom backend in tableplot's AoG implementation.
;;
;; The thi.ng/geom backend is a pure Clojure visualization backend offering:
;; - **Pure Clojure** - No JavaScript dependencies
;; - **Native SVG output** - Static, embeddable graphics
;; - **True polar coordinates** - Not simulated via Cartesian conversion
;; - **ggplot2 themes** - 9 themes matching ggplot2 aesthetics
;; - **Mathematical precision** - Excellent for parametric curves
;;
;; This guide covers:
;; 1. Basic usage (AoG integration and direct Entry IR)
;; 2. All supported plot types
;; 3. Polar coordinates (thi.ng/geom's unique strength)
;; 4. Themes and styling
;; 5. Multi-layer compositions
;; 6. Advanced features
;; 7. When to use thi.ng/geom
;; 8. Backend comparison

(ns tableplot-book.backend-thing-geom
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.thing-geom :as tg]
            [scicloj.tableplot.v1.aog.processing :as processing]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Part 1: Basic Usage
;;
;; The thi.ng/geom backend can be used in two ways:
;; 1. Through the AoG API (recommended)
;; 2. Directly via Entry IR (for low-level control)

;; ### Via AoG API

;; Process AoG layers into entries, then render with thi.ng/geom:

(def simple-data
  {:x [1 2 3 4 5]
   :y [2 4 3 5 6]})

;; **Simple scatter plot** - Using AoG API

(let [layers (aog/* (aog/data simple-data)
                    (aog/mapping :x :y)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Direct Entry IR Usage

;; For more control, create entries directly and call `tg/entry->svg`:

(def scatter-entry
  {:plottype :scatter
   :positional [[1 2 3 4 5]
                [2 4 3 5 6]]
   :named {:color "#0af"
           :opacity 0.7}})

;; **Direct Entry IR** - Lower-level access

(tg/entry->svg scatter-entry {:width 600 :height 400})

;; ## Part 2: All Plot Types
;;
;; Comprehensive examples of all plot types supported by the thi.ng/geom backend.

;; ### Scatter Plot

(def scatter-data
  {:x (vec (repeatedly 50 rand))
   :y (vec (repeatedly 50 rand))})

;; **Scatter Plot** - Individual points

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x scatter-data) (:y scatter-data)]
  :named {:color "#e74c3c" :opacity 0.7}}
 {:width 600 :height 400})

;; ### Line Plot

(def line-data
  (let [x (vec (range 0 10 0.2))]
    {:x x
     :y (vec (map #(Math/sin %) x))}))

;; **Line Plot** - Sine wave

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke "#9b59b6" :stroke-width 2}}
 {:width 600 :height 400})

;; ### Bar Chart

(def bar-data
  {:category [1 2 3 4 5]
   :value [23 45 34 56 42]})

;; **Bar Chart** - Categorical values

(tg/entry->svg
 {:plottype :bar
  :positional [(:category bar-data) (:value bar-data)]
  :named {:fill "#16a085" :bar-width 40}}
 {:width 600 :height 400})

;; ### Line Styling

;; **Dashed line** - Using stroke-dasharray

(tg/entry->svg
 {:plottype :line
  :positional [[1 2 3 4 5 6 7 8 9 10]
               [1 3 2 5 4 7 6 9 8 10]]
  :named {:stroke "#e67e22"
          :stroke-width 2
          :stroke-dasharray "5,5"}}
 {:width 600 :height 400})

;; **Dotted line** - Fine dots

(tg/entry->svg
 {:plottype :line
  :positional [[1 2 3 4 5 6 7 8 9 10]
               [2 4 3 5 6 4 7 5 8 6]]
  :named {:stroke "#1abc9c"
          :stroke-width 3
          :stroke-dasharray "2,3"}}
 {:width 600 :height 400})

;; ### Bar Styling

;; **Bars with stroke** - Custom fill and outline

(tg/entry->svg
 {:plottype :bar
  :positional [[1 2 3 4 5]
               [23 45 12 67 34]]
  :named {:fill "#3498db"
          :stroke "#2c3e50"
          :stroke-width 2}}
 {:width 600 :height 400})

;; ## Part 3: Polar Coordinates (thi.ng/geom's Superpower!)
;;
;; The thi.ng/geom backend excels at polar coordinate visualizations with true
;; native support (not simulated via Cartesian transformations).
;;
;; Enable polar mode by passing `:polar true` in the options map.

;; ### Simple Polar Plots

;; **Circle** - r = constant

(let [theta (vec (range 0 (* 2 Math/PI) 0.1))
      r (vec (repeat (count theta) 5))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#3498db" :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; **Spiral** - r = θ (radius increases with angle)

(let [theta (vec (range 0 (* 4 Math/PI) 0.1))
      r (vec theta)]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#e74c3c" :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### Rose Curves
;;
;; Rose curves are sinusoidal curves in polar coordinates:
;; r = a × cos(kθ) or r = a × sin(kθ)

;; **3-petaled rose** - r = 5 × cos(3θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (Math/cos (* 3 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#9b59b6" :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; **4-petaled rose** (8 petals total) - r = 6 × sin(2θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 6 (Math/sin (* 2 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#2ecc71" :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; **5-petaled rose** - r = 7 × cos(5θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 7 (Math/cos (* 5 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#e67e22" :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### Cardioid and Limacon

;; **Cardioid** - r = a × (1 + cos(θ)) - Heart-like shape

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (+ 1 (Math/cos %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#d35400" :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; **Limacon** - r = a + b × cos(θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(+ 2 (* 3 (Math/cos %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#27ae60" :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### Radar/Spider Charts

;; **5-category radar** (pentagon)

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

;; **6-category radar** (hexagon)

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

;; ### Overlapping Radar Charts

;; **Comparing two profiles**

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

;; ## Part 4: Themes and Styling
;;
;; thi.ng/geom supports 9 ggplot2-compatible themes.

;; ### Available Themes

;; thi.ng/geom has 9 built-in themes:
;; - `:grey` (default) - Grey background with white grid (ggplot2 default)
;; - `:bw` - Black & white with grey grid
;; - `:minimal` - Minimalist with subtle grey grid
;; - `:classic` - Classic look with no grid
;; - `:dark` - Dark background (presentations)
;; - `:light` - Light with very subtle grid
;; - `:void` - Completely minimal (no grid, no panel background)
;; - `:linedraw` - Crisp black grid lines
;; - `:tableplot` - tableplot default (similar to :grey)

(def theme-data
  {:x [1 2 3 4 5 6 7 8]
   :y [2 4 3 5 6 4 7 5]})

;; **:grey theme** (default) - Grey panel, white grid

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {}}
 {:theme :grey :width 600 :height 400})

;; **:bw theme** - Black & white

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {}}
 {:theme :bw :width 600 :height 400})

;; **:minimal theme** - Clean and minimal

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {}}
 {:theme :minimal :width 600 :height 400})

;; **:dark theme** - Dark background (great for presentations)

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {}}
 {:theme :dark :width 600 :height 400})

;; **:linedraw theme** - Crisp black grid

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {}}
 {:theme :linedraw :width 600 :height 400})

;; **:void theme** - Absolutely minimal

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {}}
 {:theme :void :width 600 :height 400})

;; ### Themes with Line Plots

(def line-theme-data
  (let [x (vec (range 0 10 0.2))]
    {:x x :y (vec (map #(Math/sin %) x))}))

;; **Line plot with :grey theme**

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-theme-data) (:y line-theme-data)]
  :named {}}
 {:theme :grey :width 600 :height 400})

;; **Line plot with :dark theme**

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-theme-data) (:y line-theme-data)]
  :named {}}
 {:theme :dark :width 600 :height 400})

;; ### Themes with Bar Charts

(def bar-theme-data
  {:x [1 2 3 4 5]
   :y [23 45 34 56 42]})

;; **Bar chart with :grey theme**

(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-theme-data) (:y bar-theme-data)]
  :named {}}
 {:theme :grey :width 600 :height 400})

;; **Bar chart with :linedraw theme**

(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-theme-data) (:y bar-theme-data)]
  :named {}}
 {:theme :linedraw :width 600 :height 400})

;; ### Themes with Polar Plots

(def rose-theme-data
  (let [theta (range 0 (* 2 Math/PI) 0.01)
        r (map (fn [t] (* 8 (Math/cos (* 5 t)))) theta)]
    {:plottype :line
     :positional [(vec theta) (vec r)]
     :named {}}))

;; **5-petaled rose with :grey theme**

(tg/entry->svg rose-theme-data {:theme :grey :polar true :width 600 :height 600})

;; **5-petaled rose with :dark theme**

(tg/entry->svg rose-theme-data {:theme :dark :polar true :width 600 :height 600})

;; **5-petaled rose with :minimal theme**

(tg/entry->svg rose-theme-data {:theme :minimal :polar true :width 600 :height 600})

;; ### Custom Styling

;; You can override theme defaults with entry-specific styling:

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {:color "#ff0000" ; Override default color
          :opacity 0.5}} ; Override default opacity
 {:theme :grey :width 600 :height 400})

;; ## Part 5: Multi-layer Compositions
;;
;; Combine multiple plot types in a single visualization.

;; ### Scatter + Line

(def trend-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y-scatter [2.1 3.9 6.2 7.8 10.1 12.3 13.9 16.2 17.8 20.1]
   :y-line [2 4 6 8 10 12 14 16 18 20]})

;; **Data points with trend line**

(tg/entries->svg
 [{:plottype :scatter
   :positional [(:x trend-data) (:y-scatter trend-data)]
   :named {:color "#3498db" :opacity 0.7}}
  {:plottype :line
   :positional [(:x trend-data) (:y-line trend-data)]
   :named {:stroke "#e74c3c" :stroke-width 2}}]
 {:width 600 :height 400})

;; ### Multiple Lines

;; **Three sine waves** (different frequencies)

(let [x (vec (range 0 10 0.1))
      y1 (vec (map #(Math/sin %) x))
      y2 (vec (map #(Math/sin (* 2 %)) x))
      y3 (vec (map #(Math/sin (* 3 %)) x))]
  (tg/entries->svg
   [{:plottype :line
     :positional [x y1]
     :named {:stroke "#e74c3c" :stroke-width 2}}
    {:plottype :line
     :positional [x y2]
     :named {:stroke "#3498db" :stroke-width 2}}
    {:plottype :line
     :positional [x y3]
     :named {:stroke "#2ecc71" :stroke-width 2}}]
   {:width 600 :height 400}))

;; ### Bar + Line

;; **Bars with average line**

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

;; ### Multi-layer with Themes

(def multi-theme-data
  {:scatter {:x [1 2 3 4 5 6 7 8]
             :y [2 4 3 5 6 4 7 5]}
   :line {:x [1 2 3 4 5 6 7 8]
          :y [2 4 3 5 6 4 7 5]}})

;; **Multi-layer with :grey theme**

(tg/entries->svg
 [{:plottype :scatter
   :positional [(:x (:scatter multi-theme-data)) (:y (:scatter multi-theme-data))]
   :named {:color "#0af"}}
  {:plottype :line
   :positional [(:x (:line multi-theme-data)) (:y (:line multi-theme-data))]
   :named {:stroke "#f80" :stroke-width 2}}]
 {:theme :grey :width 600 :height 400})

;; **Multi-layer with :dark theme**

(tg/entries->svg
 [{:plottype :scatter
   :positional [(:x (:scatter multi-theme-data)) (:y (:scatter multi-theme-data))]
   :named {:color "#0af"}}
  {:plottype :line
   :positional [(:x (:line multi-theme-data)) (:y (:line multi-theme-data))]
   :named {:stroke "#f80" :stroke-width 2}}]
 {:theme :dark :width 600 :height 400})

;; ### Polar Multi-layer

;; **Two rose curves overlapping**

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

;; ## Part 6: AoG Integration
;;
;; The thi.ng/geom backend integrates fully with AlgebraOfGraphics.

;; ### AoG Pipeline: Data → Mapping → Visual → Render

(def aog-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]})

;; **Scatter plot via AoG**

(let [layers (aog/* (aog/data aog-data)
                    (aog/mapping :x :y)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; **Line plot via AoG**

(def line-aog-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1.0 4.0 9.0 16.0 25.0 36.0 49.0 64.0 81.0 100.0]})

(let [layers (aog/* (aog/data line-aog-data)
                    (aog/mapping :x :y)
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Multi-Layer Composition with +

(def trend-aog-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2.1 3.9 6.2 7.8 10.1 12.3 13.9 16.2 17.8 20.1]})

;; **Scatter + Line via AoG**

(let [layers (aog/* (aog/data trend-aog-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter {:alpha 0.6})
                           (aog/line {:width 2})))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Statistical Transformations

(def regression-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2.5 4.8 5.9 8.2 10.1 11.7 14.3 15.8 18.1 19.9]})

;; **Linear regression via AoG**

(let [layers (aog/* (aog/data regression-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter {:alpha 0.5})
                           (aog/linear)))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Part 7: Advanced Features

;; ### Internal Rendering

;; The underlying thi.ng/geom spec generation is handled internally.
;; Use `entry->svg` or `entries->svg` for direct SVG rendering.

;; ### Custom Plot Sizes

;; **Square plot** (500×500)

(tg/entry->svg scatter-entry {:width 500 :height 500})

;; **Wide plot** (800×300)

(tg/entry->svg scatter-entry {:width 800 :height 300})

;; **Large plot for presentations** (1200×700)

(let [large-data
      {:x (vec (range 1 51))
       :y (vec (map #(+ % (* 5 (Math/sin (/ % 5.0)))) (range 1 51)))}]
  (tg/entry->svg
   {:plottype :line
    :positional [(:x large-data) (:y large-data)]
    :named {:stroke "#e67e22" :stroke-width 3}}
   {:width 1200 :height 700}))

;; ### Edge Cases

;; **Single point scatter**

(tg/entry->svg
 {:plottype :scatter
  :positional [[5] [5]]
  :named {:color "#e74c3c"}}
 {:width 600 :height 400})

;; **Negative values**

(tg/entry->svg
 {:plottype :scatter
  :positional [[-5 -3 -1 0 1 3 5]
               [-10 -5 -2 0 2 5 10]]
  :named {:color "#8e44ad"}}
 {:width 600 :height 400})

;; ## Part 8: When to Use thi.ng/geom
;;
;; Choose thi.ng/geom when you need:
;;
;; ### ✅ Best Use Cases:
;;
;; 1. **Polar coordinates are essential**
;;    - Rose curves, spirals, radar charts
;;    - Native support (not simulated)
;;    - True mathematical polar rendering
;;
;; 2. **Pure Clojure solution preferred**
;;    - No JavaScript dependencies
;;    - Server-side rendering
;;    - JVM-only environments
;;
;; 3. **Static SVG output required**
;;    - Publications and papers
;;    - Embedded graphics
;;    - Minimal file sizes (~10KB)
;;
;; 4. **ggplot2-style themes desired**
;;    - 9 ggplot2-compatible themes
;;    - Familiar aesthetics for R users
;;    - Consistent styling
;;
;; 5. **Mathematical plots and curves**
;;    - Parametric curves
;;    - Geometric shapes
;;    - Precise rendering
;;
;; ### ❌ Not Ideal For:
;;
;; 1. **Interactivity required**
;;    - Use Plotly for pan, zoom, hover
;;    - thi.ng/geom produces static SVG
;;
;; 2. **3D visualizations**
;;    - Use Plotly for native 3D support
;;    - thi.ng/geom is 2D only (Cartesian + polar)
;;
;; 3. **Faceted layouts**
;;    - Use Vega-Lite for built-in faceting
;;    - thi.ng/geom requires manual composition
;;
;; 4. **Web-based dashboards with JS**
;;    - Use Plotly for rich interactivity
;;    - thi.ng/geom better for static reports

;; ## Part 9: Backend Comparison
;;
;; How does thi.ng/geom compare to the other backends?

;; | Feature | **Plotly** | **Vega-Lite** | **thi.ng/geom** |
;; |---------|-----------|--------------|----------------|
;; | **Output Format** | HTML + JS | SVG (via Vega-Lite spec) | SVG (native) |
;; | **Interactivity** | ✅ Full (pan, zoom, hover) | ⚠️ Limited (tooltips only) | ❌ None (static) |
;; | **3D Support** | ✅ Native (scatter3d, surface) | ❌ None | ❌ None |
;; | **Polar Coordinates** | ⚠️ Manual conversion | ❌ None | ✅ Native |
;; | **Faceting** | ⚠️ Manual subplots | ✅ Built-in (`col`, `row`) | ❌ None |
;; | **Themes** | ✅ 7+ built-in | ✅ 5 built-in | ✅ 9 ggplot2 themes |
;; | **File Size** | ~3MB (includes plotly.js) | ~100KB (spec only) | ~10KB (pure SVG) |
;; | **Performance** | ⚠️ Slower with 100k+ points | ✅ Fast (declarative) | ✅ Very fast |
;; | **Customization** | ✅ Extensive (Plotly.js API) | ✅ Good (Vega spec) | ✅ Full (thi.ng/geom) |
;; | **Dependencies** | Plotly.js (browser) | Vega-Lite (JVM) | Pure Clojure |
;; | **Use Cases** | Dashboards, 3D, EDA | Reports, facets, static | Polar, print, pure SVG |

;; ### Summary

;; **Choose thi.ng/geom when:**
;; - You need polar coordinates (radar charts, rose diagrams)
;; - Pure Clojure solution is preferred
;; - You want minimal file sizes
;; - You need ggplot2-style themes
;; - Static SVG output is required

;; **Choose Plotly when:**
;; - You need rich interactivity (pan, zoom, hover)
;; - You're creating 3D visualizations
;; - You're building web-based dashboards
;; - Output format is HTML (not SVG)

;; **Choose Vega-Lite when:**
;; - You need faceting/small multiples
;; - Static SVG output is preferred
;; - You want declarative specs
;; - You're generating reports

;; ## Best Practices

;; ### 1. Always Provide Width and Height

;; Explicitly specify dimensions for predictable output:

(tg/entry->svg scatter-entry {:width 600 :height 400}) ; Good!

;; ### 2. Use Vectors for Positional Data

;; Not lists or lazy sequences:

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

;; - Thumbnails: 200×150
;; - Standard: 600×400
;; - Presentations: 1200×700
;; - Polar plots: Square aspect ratio (600×600)

;; ### 5. Close Polar Polygons

;; Repeat the first value at the end for closed shapes:

(let [n 5
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n)))) ; (inc n)
      r [0.8 0.6 0.9 0.7 0.5 0.8]] ; Repeat first value
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#2980b9"
            :stroke-width 3
            :fill "#2980b9"
            :opacity 0.3}}
   {:polar true :width 600 :height 600}))

;; ### 6. Use Themes for Consistency

;; Leverage ggplot2-compatible themes for professional output:

(tg/entry->svg scatter-entry {:theme :grey :width 600 :height 400})

;; ### 7. Override Theme Defaults When Needed

;; Entry-specific styling always takes precedence:

(tg/entry->svg
 {:plottype :scatter
  :positional [[1 2 3] [4 5 6]]
  :named {:color "#ff0000"}} ; Overrides theme default
 {:theme :grey :width 600 :height 400})

;; ## Theme Comparison Table

;; | Theme | Panel BG | Plot BG | Grid Color | Best For |
;; |-------|----------|---------|------------|----------|
;; | **:grey** | Grey (#ebebeb) | White | White (#ffffff) | General use (ggplot2 default) |
;; | **:bw** | White | White | Light grey (#d9d9d9) | Publications, B&W printing |
;; | **:minimal** | White | White | Very light grey (#f0f0f0) | Clean, modern look |
;; | **:classic** | White | White | None | Simple, traditional plots |
;; | **:dark** | Dark grey (#333333) | Very dark (#222222) | Medium grey (#555555) | Presentations, dark mode |
;; | **:light** | Very light (#fafafa) | White | Light grey (#e0e0e0) | Subtle, airy aesthetic |
;; | **:void** | White | White | None | Absolutely minimal |
;; | **:linedraw** | White | White | Black (#000000) | High contrast, crisp lines |
;; | **:tableplot** | Grey (#ebebeb) | White | White (#ffffff) | tableplot default |

;; ## Next Steps
;;
;; - See `backend_plotly.clj` for Plotly backend guide
;; - See `backend_vegalite.clj` for Vega-Lite backend guide
;; - See `aog_backends.clj` for side-by-side comparison
;; - See `aog_tutorial.clj` for general AoG introduction
;; - See `aog_plot_types.clj` for comprehensive plot type reference
;; - See `aog_examples.clj` for real-world dataset examples
