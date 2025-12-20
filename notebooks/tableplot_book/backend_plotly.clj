;; # Plotly Backend - Complete Guide
;;
;; Comprehensive guide to using the Plotly backend in tableplot's AoG implementation.
;;
;; The Plotly backend is tableplot's default backend, offering:
;; - **Interactive visualizations** - Pan, zoom, hover tooltips, legend toggles
;; - **3D support** - Native 3D scatter, surface, and mesh plots
;; - **Rich interactivity** - Click events, selections, and animations
;; - **HTML output** - Self-contained HTML files with embedded JavaScript
;; - **Production-ready** - Widely used, well-tested, feature-complete
;;
;; This guide covers:
;; 1. Basic usage (AoG integration and direct Entry IR)
;; 2. All supported plot types
;; 3. 3D visualizations (Plotly's unique strength)
;; 4. Interactivity features
;; 5. Themes and styling
;; 6. Multi-layer compositions
;; 7. Advanced features
;; 8. When to use Plotly
;; 9. Backend comparison

(ns tableplot-book.backend-plotly
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.plotly :as plotly]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Part 1: Basic Usage
;;
;; The Plotly backend can be used in two ways:
;; 1. Through the AoG API (recommended)
;; 2. Directly via Entry IR (for low-level control)

;; ### Via AoG API (Default)

;; Plotly is the default backend, so you don't need to specify it:

(def simple-data
  {:x [1 2 3 4 5]
   :y [2 4 3 5 6]})

;; **Simple scatter plot** - Using AoG API (Plotly by default)

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/scatter)))

;; ### Direct Entry IR Usage

;; For more control, you can create entries directly and call `plotly/plotly`:

(def scatter-entry
  {:plottype :scatter
   :positional [[1 2 3 4 5]
                [2 4 3 5 6]]
   :named {:marker-color "#0af"
           :marker-size 10}})

;; **Direct Entry IR** - Lower-level access

(plotly/plotly scatter-entry)

;; You can also pass options:

(plotly/plotly scatter-entry {:width 800 :height 500 :title "My Scatter Plot"})

;; ## Part 2: All Plot Types
;;
;; Comprehensive examples of all plot types supported by the Plotly backend.

;; ### Scatter Plot

(def scatter-data
  {:x (vec (repeatedly 50 rand))
   :y (vec (repeatedly 50 rand))})

;; **Scatter Plot** - Individual points with hover tooltips

(aog/draw
 (aog/* (aog/data scatter-data)
        (aog/mapping :x :y)
        (aog/scatter)))

;; ### Line Plot

(def line-data
  (let [x (vec (range 0 10 0.2))]
    {:x x
     :y (vec (map #(Math/sin %) x))}))

;; **Line Plot** - Connected points (sine wave)

(aog/draw
 (aog/* (aog/data line-data)
        (aog/mapping :x :y)
        (aog/line)))

;; ### Bar Chart

(def bar-data
  {:category ["Product A" "Product B" "Product C" "Product D" "Product E"]
   :value [23 45 34 56 42]})

;; **Bar Chart** - Categorical values with hover showing exact values

(aog/draw
 (aog/* (aog/data bar-data)
        (aog/mapping :category :value)
        (aog/bar)))

;; ### Grouped Bar Chart

(def grouped-bar-data
  {:quarter (vec (mapcat #(repeat 3 %) ["Q1" "Q2" "Q3" "Q4"]))
   :product (vec (take 12 (cycle ["Product A" "Product B" "Product C"])))
   :sales (vec (repeatedly 12 #(+ 50 (rand-int 100))))})

;; **Grouped Bar Chart** - Multiple series per category (interactive legend)

(aog/draw
 (aog/* (aog/data grouped-bar-data)
        (aog/mapping :quarter :sales {:color :product})
        (aog/bar)))

;; ### Histogram

(def hist-data
  {:values (vec (repeatedly 1000 #(* (- (rand) 0.5) 6)))})

;; **Histogram** - Distribution with 20 bins (hover shows count per bin)

(aog/draw
 (aog/* (aog/data hist-data)
        (aog/mapping :values)
        (aog/histogram {:bins 20})))

;; ### Density Plot

(def density-data
  {:values (vec (repeatedly 500 #(+ 5 (* 2 (- (rand) 0.5)))))})

;; **Density Plot** - Kernel density estimation (Gaussian kernel)

(aog/draw
 (aog/* (aog/data density-data)
        (aog/mapping :values)
        (aog/density)))

;; ### Box Plot

(def boxplot-data
  {:group (vec (mapcat #(repeat 30 %) ["Group A" "Group B" "Group C"]))
   :value (vec (concat
                (repeatedly 30 #(+ 50 (* 10 (- (rand) 0.5))))
                (repeatedly 30 #(+ 70 (* 15 (- (rand) 0.5))))
                (repeatedly 30 #(+ 60 (* 8 (- (rand) 0.5))))))})

;; **Box Plot** - Distribution quartiles and outliers (hover shows statistics)

(aog/draw
 (aog/* (aog/data boxplot-data)
        (aog/mapping :group :value)
        (aog/boxplot)))

;; ### Violin Plot

;; **Violin Plot** - Distribution density visualization (interactive)

(aog/draw
 (aog/* (aog/data boxplot-data)
        (aog/mapping :group :value)
        (aog/violin)))

;; ### Heatmap

(def heatmap-data
  (let [x (vec (range 8))
        y (vec (range 6))]
    {:x (vec (for [xi x yi y] xi))
     :y (vec (for [xi x yi y] yi))
     :value (vec (for [xi x yi y] (+ (* xi yi) (* 0.5 (rand)))))}))

;; **Heatmap** - 2D grid with color-encoded values (hover shows exact value)

(aog/draw
 (aog/* (aog/data heatmap-data)
        (aog/mapping :x :y {:color :value})
        (aog/heatmap)))

;; ## Part 3: 3D Visualizations (Plotly's Superpower!)
;;
;; Plotly has native support for 3D plots, which neither Vega-Lite nor thi.ng/geom support.

;; ### 3D Scatter Plot

(def scatter-3d-data
  {:x (vec (repeatedly 100 #(* 2 (- (rand) 0.5))))
   :y (vec (repeatedly 100 #(* 2 (- (rand) 0.5))))
   :z (vec (repeatedly 100 #(* 2 (- (rand) 0.5))))})

;; **3D Scatter Plot** - Fully rotatable 3D visualization

(plotly/plotly
 {:plottype :scatter3d
  :positional [(:x scatter-3d-data) (:y scatter-3d-data) (:z scatter-3d-data)]
  :named {:marker-color "#0af"
          :marker-size 4}}
 {:width 700 :height 600 :title "Interactive 3D Scatter Plot"})

;; ### 3D Surface Plot

(def surface-data
  (let [x (vec (range -5 5 0.5))
        y (vec (range -5 5 0.5))]
    {:x x
     :y y
     :z (vec (for [xi x]
               (vec (for [yi y]
                      (let [r (Math/sqrt (+ (* xi xi) (* yi yi)))]
                        (if (zero? r)
                          1.0
                          (/ (Math/sin r) r)))))))}))

;; **3D Surface Plot** - sinc function surface (rotatable, pan, zoom)

(plotly/plotly
 {:plottype :surface
  :positional [(:x surface-data) (:y surface-data) (:z surface-data)]
  :named {:colorscale "Viridis"}}
 {:width 700 :height 600 :title "Sinc Function Surface"})

;; ### 3D Line Plot

(def line-3d-data
  (let [t (vec (range 0 (* 4 Math/PI) 0.1))]
    {:x (vec (map #(Math/cos %) t))
     :y (vec (map #(Math/sin %) t))
     :z t}))

;; **3D Line Plot** - Helix in 3D space

(plotly/plotly
 {:plottype :scatter3d
  :positional [(:x line-3d-data) (:y line-3d-data) (:z line-3d-data)]
  :named {:mode "lines"
          :line-color "#f80"
          :line-width 4}}
 {:width 700 :height 600 :title "3D Helix"})

;; ### 3D Mesh Plot

(def mesh-data
  {:x [0 1 2 0]
   :y [0 0 1 2]
   :z [0 2 0 1]
   :i [0 0 0 1] ; Triangle indices
   :j [1 2 3 2]
   :k [2 3 1 3]})

;; **3D Mesh Plot** - Tetrahedral mesh

(plotly/plotly
 {:plottype :mesh3d
  :positional [(:x mesh-data) (:y mesh-data) (:z mesh-data)]
  :named {:i (:i mesh-data)
          :j (:j mesh-data)
          :k (:k mesh-data)
          :color "#0af"
          :opacity 0.7}}
 {:width 700 :height 600 :title "3D Mesh"})

;; ## Part 4: Interactivity Features
;;
;; Plotly excels at interactive visualizations.

;; ### Hover Tooltips

;; Plotly automatically adds hover tooltips showing data values.
;; You can customize them:

(def hover-data
  {:city ["New York" "Los Angeles" "Chicago" "Houston" "Phoenix"]
   :population [8336817 3979576 2693976 2320268 1680992]
   :area [302.6 468.7 227.3 637.5 517.9]})

;; **Custom Hover Text** - City statistics with formatted tooltips

(plotly/plotly
 {:plottype :scatter
  :positional [(:area hover-data) (:population hover-data)]
  :named {:mode "markers+text"
          :marker-size 15
          :marker-color "#5E9B4D"
          :text (:city hover-data)
          :textposition "top center"
          :hovertemplate "<b>%{text}</b><br>Area: %{x:.1f} sq mi<br>Population: %{y:,}<extra></extra>"}}
 {:width 700 :height 500
  :title "US Cities: Population vs Area"
  :xaxis {:title "Area (sq mi)"}
  :yaxis {:title "Population"}})

;; ### Interactive Legend

;; Click legend items to show/hide traces:

(def legend-data
  (let [x (vec (range 0 10 0.2))]
    {:x x
     :sin (vec (map #(Math/sin %) x))
     :cos (vec (map #(Math/cos %) x))
     :tan (vec (map #(Math/tan %) x))}))

;; **Interactive Legend** - Click to toggle traces on/off

(plotly/plotly
 {:plottype :multi-trace
  :traces [{:x (:x legend-data) :y (:sin legend-data) :name "sin(x)" :line-color "#0af"}
           {:x (:x legend-data) :y (:cos legend-data) :name "cos(x)" :line-color "#f80"}
           {:x (:x legend-data) :y (:tan legend-data) :name "tan(x)" :line-color "#5E9B4D"}]}
 {:width 700 :height 500 :title "Trigonometric Functions (Click legend to toggle)"})

;; ### Zoom and Pan

;; All Plotly plots support:
;; - **Box zoom**: Click and drag to zoom into a region
;; - **Pan**: Drag to pan around
;; - **Autoscale**: Double-click to reset view
;; - **Zoom in/out**: Use toolbar buttons

;; **Large Dataset** - Try zooming and panning!

(def large-data
  (let [x (vec (range 0 100 0.1))]
    {:x x
     :y (vec (map #(+ (Math/sin %) (* 0.1 (Math/cos (* 10 %)))) x))}))

(aog/draw
 (aog/* (aog/data large-data)
        (aog/mapping :x :y)
        (aog/line)))

;; ## Part 5: Themes and Styling
;;
;; Plotly supports multiple built-in themes.

;; ### Available Themes

;; Plotly has several built-in themes:
;; - `plotly` (default) - White background, colorful
;; - `plotly_white` - Clean white background
;; - `plotly_dark` - Dark background
;; - `ggplot2` - Mimics ggplot2 styling
;; - `seaborn` - Seaborn-inspired
;; - `simple_white` - Minimal white theme
;; - `none` - No theme (full control)

(def theme-data
  {:x [1 2 3 4 5]
   :y [2 4 3 5 6]})

;; **Default Theme** - Plotly default (colorful)

(plotly/plotly
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {:mode "markers+lines"
          :marker-size 12}}
 {:width 600 :height 400
  :title "Default Theme"
  :template "plotly"})

;; **White Theme** - Clean and minimal

(plotly/plotly
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {:mode "markers+lines"
          :marker-size 12}}
 {:width 600 :height 400
  :title "White Theme"
  :template "plotly_white"})

;; **Dark Theme** - Dark background (great for presentations)

(plotly/plotly
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {:mode "markers+lines"
          :marker-size 12
          :marker-color "#0af"
          :line-color "#0af"}}
 {:width 600 :height 400
  :title "Dark Theme"
  :template "plotly_dark"})

;; **ggplot2 Theme** - Familiar to R users

(plotly/plotly
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {:mode "markers+lines"
          :marker-size 12}}
 {:width 600 :height 400
  :title "ggplot2 Theme"
  :template "ggplot2"})

;; **Seaborn Theme** - Seaborn-inspired styling

(plotly/plotly
 {:plottype :scatter
  :positional [(:x theme-data) (:y theme-data)]
  :named {:mode "markers+lines"
          :marker-size 12}}
 {:width 600 :height 400
  :title "Seaborn Theme"
  :template "seaborn"})

;; ### Custom Styling

;; You can customize colors, sizes, and more:

(def custom-style-data
  {:category ["Q1" "Q2" "Q3" "Q4"]
   :revenue [45 52 61 58]})

;; **Custom Colors and Styling**

(plotly/plotly
 {:plottype :bar
  :positional [(:category custom-style-data) (:revenue custom-style-data)]
  :named {:marker-color ["#FF6B6B" "#4ECDC4" "#45B7D1" "#FFA07A"]
          :marker-line-color "#000"
          :marker-line-width 2}}
 {:width 600 :height 400
  :title "Quarterly Revenue"
  :template "plotly_white"
  :xaxis {:title "Quarter"}
  :yaxis {:title "Revenue ($M)"}})

;; ## Part 6: Multi-layer Compositions
;;
;; Combine multiple plot types in a single visualization.

;; ### Scatter + Linear Regression

(def regression-data
  (let [x (vec (repeatedly 80 rand))
        y (vec (map #(+ (* 3 %) 2 (* 0.5 (- (rand) 0.5))) x))]
    {:x x :y y}))

;; **Scatter + Linear Fit** - AoG layering with `+`

(aog/draw
 (aog/* (aog/data regression-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))

;; ### Scatter + Smooth Curve

(def smooth-data
  (let [x (vec (repeatedly 100 rand))
        y (vec (map #(+ (rand) (* 5 % %)) x))]
    {:x x :y y}))

;; **Scatter + LOESS Smooth** - Non-linear trend

(aog/draw
 (aog/* (aog/data smooth-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.4})
               (aog/smooth))))

;; ### Triple Layer: Scatter + Line + Smooth

(def multi-layer-data
  (let [x (vec (range 0 10 0.1))
        y (vec (map #(+ (Math/sin %) (* 0.3 (- (rand) 0.5))) x))]
    {:x x :y y}))

;; **Three Layers** - Scatter (points) + Line (direct) + Smooth (trend)

(aog/draw
 (aog/* (aog/data multi-layer-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.3})
               (aog/line {:alpha 0.5})
               (aog/smooth))))

;; ## Part 7: Advanced Features

;; ### Accessing Plotly Specs

;; You can access the underlying Plotly spec:

(def spec-example
  (plotly/entries->plotly
   [{:plottype :scatter
     :positional [[1 2 3] [4 5 6]]
     :named {}}]))

;; View the spec:
;; spec-example
;; => {:data [{:x [1 2 3] :y [4 5 6] :type "scatter" :mode "markers"}]
;;     :layout {:width 600 :height 400 :template "plotly"}}

;; ### Custom Layout Options

;; Full control over layout:

(plotly/plotly
 {:plottype :scatter
  :positional [[1 2 3 4 5] [2 4 3 5 6]]
  :named {:marker-size 15 :marker-color "#5E9B4D"}}
 {:width 800
  :height 500
  :title "Custom Layout"
  :xaxis {:title "X Axis"
          :showgrid true
          :gridcolor "#ddd"
          :zeroline false}
  :yaxis {:title "Y Axis"
          :showgrid true
          :gridcolor "#ddd"
          :zeroline false}
  :template "plotly_white"
  :showlegend false})

;; ### Subplots

;; Create multi-panel plots using Plotly's subplot support:

(def subplot-data
  (let [x (vec (range 0 10 0.2))]
    {:x x
     :sin (vec (map #(Math/sin %) x))
     :cos (vec (map #(Math/cos %) x))}))

;; **Subplots** - Multiple plots in one figure

(plotly/plotly
 {:plottype :subplots
  :traces [{:x (:x subplot-data) :y (:sin subplot-data) :name "sin(x)" :row 1 :col 1}
           {:x (:x subplot-data) :y (:cos subplot-data) :name "cos(x)" :row 2 :col 1}]
  :layout {:rows 2 :cols 1 :subplot-titles ["sin(x)" "cos(x)"]}}
 {:width 700 :height 600 :title "Trigonometric Subplots"})

;; ### Annotations

;; Add text annotations and arrows:

(plotly/plotly
 {:plottype :scatter
  :positional [[1 2 3 4 5] [2 4 3 5 6]]
  :named {:marker-size 12}}
 {:width 700 :height 500
  :title "Annotated Plot"
  :annotations [{:x 3 :y 3 :text "Local Minimum" :showarrow true :arrowhead 2}
                {:x 5 :y 6 :text "Maximum" :showarrow true :arrowhead 2}]})

;; ## Part 8: When to Use Plotly
;;
;; Choose Plotly when you need:
;;
;; ### ✅ Best Use Cases:
;;
;; 1. **Interactivity is essential**
;;    - Pan, zoom, hover tooltips out of the box
;;    - Click events and selections
;;    - Dynamic legend toggling
;;
;; 2. **3D visualizations**
;;    - Only backend with native 3D support
;;    - Scatter3D, Surface, Mesh3D plots
;;    - Fully rotatable 3D spaces
;;
;; 3. **Web-based dashboards**
;;    - HTML output embeds nicely in web pages
;;    - Works great with Clerk, Portal, or custom HTML
;;    - Self-contained files with embedded JavaScript
;;
;; 4. **Exploratory data analysis**
;;    - Quick interactive exploration
;;    - Zoom into interesting regions
;;    - Hover to see exact values
;;
;; 5. **Presentations and reports**
;;    - Professional-looking output
;;    - Interactive elements engage audience
;;    - Multiple theme options
;;
;; ### ❌ Not Ideal For:
;;
;; 1. **Static SVG output required**
;;    - Use Vega-Lite or thi.ng/geom instead
;;    - Plotly generates HTML with embedded JS
;;
;; 2. **Polar coordinates or radar charts**
;;    - Use thi.ng/geom for native polar support
;;    - Plotly can do it, but requires manual conversion
;;
;; 3. **Faceted/small multiples layouts**
;;    - Use Vega-Lite for built-in faceting
;;    - Plotly requires manual subplot configuration
;;
;; 4. **Extremely large datasets (100k+ points)**
;;    - Plotly can slow down in browser
;;    - Consider downsampling or WebGL mode

;; ## Part 9: Backend Comparison
;;
;; How does Plotly compare to the other backends?

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

;; **Choose thi.ng/geom when:**
;; - You need polar coordinates (radar charts, rose diagrams)
;; - Pure Clojure solution is preferred
;; - You want minimal file sizes
;; - You need ggplot2-style themes

;; ## Next Steps
;;
;; - See `backend_vegalite.clj` for Vega-Lite backend guide
;; - See `backend_thing_geom.clj` for thi.ng/geom backend guide
;; - See `aog_backends.clj` for side-by-side comparison
;; - See `aog_tutorial.clj` for general AoG introduction
;; - See `aog_plot_types.clj` for comprehensive plot type reference
;; - See `aog_examples.clj` for real-world dataset examples
