;; # ggplot2-Style API for Tableplot V2
;;
;; This walkthrough demonstrates the ggplot2-style API built on top of
;; Tableplot's V2 dataflow model.

(ns ggplot2-walkthrough
  "ggplot2-style API Walkthrough for Tableplot V2
  
  This notebook demonstrates the ggplot2-style API built on top of
  Tableplot's V2 dataflow model. The API provides familiar R-style
  syntax while leveraging the power of the V2 architecture."
  (:require [tableplot.v2.ggplot :as gg]
            [tableplot.v2.ggplot.scales :as scales]
            [tableplot.v2.ggplot.themes :as themes]
            [tableplot.v2.plotly :as plotly]
            [tech.v3.dataset :as ds]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Introduction
;;
;; The ggplot2 API provides a familiar R-style interface for creating
;; visualizations in Clojure. It features:
;;
;; - **Hierarchical aesthetics** - Global aesthetics with layer-specific overrides
;; - **Threading API** - Use `->` to build plots step by step
;; - **Layering** - Combine multiple geoms on the same plot
;; - **Scale customization** - Override default scales
;; - **Faceting** - Create small multiples
;; - **Built on V2 dataflow** - Specs are just data transformations

;; ## How Rendering Works
;;
;; The ggplot2 API separates plot construction from rendering:
;;
;; 1. **Build the spec** - Use `ggplot()`, `geom-*()`, etc. to create a plot specification
;; 2. **Run inference** - Call `render()` to resolve all aesthetics, scales, and guides
;; 3. **Generate visualization** - The inferred spec is converted to Plotly (or other backends)
;;
;; Example:

;; Build a simple spec (no rendering yet)
(def my-spec
  (-> (gg/ggplot {:x [1 2 3] :y [2 4 6]} (gg/aes :x :x :y :y))
      (gg/geom-point)))

;; Inspect the spec structure
^kind/pprint
(:sub/map my-spec)
;; => {:=data {:x [1 2 3] :y [2 4 6]}
;;     :=global-aes {:x :x :y :y}
;;     :=layers [{:geom :point :local-aes {} :attributes {}}]}

;; Render to visualization (returns Plotly spec with Kindly metadata)
(gg/render my-spec)

;; Or use :infer-only to see the inferred spec without rendering
(gg/render my-spec {:infer-only true})

;; ## Setup: Load Dataset

(def iris
  (ds/->dataset {:Sepal.Length [5.1 4.9 4.7 4.6 5.0 5.4 4.6 5.0 4.4 4.9]
                 :Sepal.Width [3.5 3.0 3.2 3.1 3.6 3.9 3.4 3.4 2.9 3.1]
                 :Petal.Length [1.4 1.4 1.3 1.5 1.4 1.7 1.4 1.5 1.4 1.5]
                 :Petal.Width [0.2 0.2 0.2 0.2 0.2 0.4 0.3 0.2 0.2 0.1]
                 :Species [:setosa :setosa :setosa :setosa :setosa
                           :setosa :setosa :setosa :setosa :setosa]}))

iris

;; ## Basic Scatter Plot
;;
;; The simplest ggplot: data + aesthetics + geom

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    gg/render)

;; Compare to R:
;; ```r
;; ggplot(iris, aes(x = Sepal.Length, y = Sepal.Width)) +
;;   geom_point()
;; ```

;; ## Adding Color
;;
;; Map a categorical variable to color

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
    (gg/geom-point)
    gg/render)

;; ## Global vs Local Aesthetics
;;
;; **Global aesthetics** are inherited by all layers

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
    (gg/geom-point {:alpha 0.6})
    gg/render)

;; **Local aesthetics** override global

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
    ;; This layer overrides color to use Petal.Length instead
    (gg/geom-point {:aes (gg/aes :color :Petal.Length)})
    gg/render)

;; ## Layering: Multiple Geoms
;;
;; Combine scatter plot with smooth line

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
    (gg/geom-point {:alpha 0.5})
    (gg/geom-smooth {:method :lm})
    gg/render)

;; ## Different Geom Types

;; ### Line Plot

(def time-series
  (ds/->dataset {:time [1 2 3 4 5 6 7 8 9 10]
                 :value [2.3 2.8 3.1 2.9 3.5 4.0 4.2 3.8 4.5 5.0]
                 :group [:A :A :A :A :A :B :B :B :B :B]}))

(-> (gg/ggplot time-series (gg/aes :x :time :y :value :color :group))
    (gg/geom-line)
    gg/render)

;; ### Bar Plot

(def category-data
  (ds/->dataset {:category [:A :B :C :D]
                 :count [12 19 15 8]}))

(-> (gg/ggplot category-data (gg/aes :x :category :y :count))
    (gg/geom-bar {:stat :identity})
    gg/render)

;; ### Histogram

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length))
    (gg/geom-histogram {:bins 10})
    gg/render)

;; ## Labels and Titles

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
    (gg/geom-point)
    (gg/labs :title "Iris Dataset Analysis"
             :subtitle "Sepal Dimensions"
             :x "Sepal Length (cm)"
             :y "Sepal Width (cm)"
             :color "Species")
    gg/render)

;; ## Scale Customization

;; ### Custom Color Palette

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
    (gg/geom-point)
    (scales/scale-color-manual {:setosa "#FF6B6B"
                                :versicolor "#4ECDC4"
                                :virginica "#45B7D1"})
    (gg/labs :title "Custom Colors")
    gg/render)

;; ### Logarithmic Scales

(def log-data
  (ds/->dataset {:x [1 10 100 1000 10000]
                 :y [1 100 10000 1000000 100000000]}))

(-> (gg/ggplot log-data (gg/aes :x :x :y :y))
    (gg/geom-point {:size 5})
    (scales/scale-x-log10)
    (scales/scale-y-log10)
    (gg/labs :title "Log-Log Plot")
    gg/render)

;; ### Custom Continuous Scales

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (scales/scale-x-continuous {:domain [4 8] :nice true})
    (scales/scale-y-continuous {:domain [2 5] :nice true})
    (gg/labs :title "Custom Axis Domains")
    gg/render)

;; ### Square Root Scale

(def sqrt-data
  (ds/->dataset {:x [0 1 4 9 16 25 36 49 64 81 100]
                 :y [0 10 20 30 40 50 60 70 80 90 100]}))

(-> (gg/ggplot sqrt-data (gg/aes :x :x :y :y))
    (gg/geom-point)
    (scales/scale-x-sqrt)
    (gg/labs :title "Square Root X-Axis")
    gg/render)

;; ## Faceting (Small Multiples)

;; ### Facet Wrap

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/facet-wrap :Species)
    (gg/labs :title "Faceted by Species")
    gg/render)

;; ### Facet Grid

(def multi-facet-data
  (ds/->dataset {:x [1 2 3 4 5 1 2 3 4 5 1 2 3 4 5 1 2 3 4 5]
                 :y [2 4 6 8 10 3 6 9 12 15 1 2 3 4 5 4 8 12 16 20]
                 :row [:A :A :A :A :A :A :A :A :A :A :B :B :B :B :B :B :B :B :B :B]
                 :col [:X :X :X :X :X :Y :Y :Y :Y :Y :X :X :X :X :X :Y :Y :Y :Y :Y]}))

(-> (gg/ggplot multi-facet-data (gg/aes :x :x :y :y))
    (gg/geom-point)
    (gg/facet-grid {:row :row :col :col})
    (gg/labs :title "Facet Grid: Rows and Columns")
    gg/render)

;; ## Themes
;;
;; Themes control the non-data visual elements of your plot like backgrounds,
;; grids, fonts, and more. All plots use `theme-grey` by default (matching R's ggplot2).

;; ### Default Theme (theme-grey)
;;
;; By default, all plots use the classic ggplot2 grey theme with:
;; - Grey panel background (#ebebeb)
;; - White grid lines
;; - White plot background

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/labs :title "Default Theme (theme-grey)")
    gg/render)

;; ### Complete Themes
;;
;; Apply different complete themes to change the overall look

;; theme-minimal: Clean white background with subtle grids
(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/theme gg/theme-minimal)
    (gg/labs :title "Minimal Theme")
    gg/render)

;; theme-bw: Black and white theme
(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/theme gg/theme-bw)
    (gg/labs :title "Black and White Theme")
    gg/render)

;; theme-classic: Classic look with axis lines
(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/theme gg/theme-classic)
    (gg/labs :title "Classic Theme")
    gg/render)

;; theme-dark: Dark background for contrast
(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/theme gg/theme-dark)
    (gg/labs :title "Dark Theme")
    gg/render)

;; ### Customizing Themes
;;
;; Customize individual theme elements using dot-notation

;; Change plot and panel backgrounds
(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/theme :plot.background "#f5f5f5"
              :panel.background "#ffffff")
    (gg/labs :title "Custom Backgrounds")
    gg/render)

;; Customize grid colors
(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/theme :panel.grid.major.color "#e0e0e0"
              :panel.grid.minor.color "#f0f0f0")
    (gg/labs :title "Custom Grid Colors")
    gg/render)

;; Customize fonts
(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
    (gg/geom-point)
    (gg/theme :plot.title.font.size 18
              :axis.text.font.size 12)
    (gg/labs :title "Custom Font Sizes")
    gg/render)

;; ### Combining Themes
;;
;; Start with a complete theme and customize it

(-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
    (gg/geom-point)
    ;; Start with minimal theme
    (gg/theme gg/theme-minimal)
    ;; Then customize specific elements
    (gg/theme :plot.background "#fafafa"
              :legend.position "bottom")
    (gg/labs :title "Minimal Theme + Customizations")
    gg/render)

;; ### Available Themes
;;
;; - `theme-grey` (default) - Classic ggplot2 grey background
;; - `theme-bw` - Black and white
;; - `theme-minimal` - Minimal with subtle grids
;; - `theme-classic` - Classic with axis lines
;; - `theme-dark` - Dark background
;; - `theme-light` - Light background
;; - `theme-void` - Completely empty (no axes, grids, etc.)
;; - `theme-linedraw` - Only black lines

;; ## Complex Example: Combining Everything

(-> (gg/ggplot iris (gg/aes :x :Petal.Length :y :Petal.Width :color :Species))
    ;; Layer 1: Semi-transparent points
    (gg/geom-point {:alpha 0.6 :size 3})
    ;; Layer 2: Smooth trend lines
    (gg/geom-smooth {:method :loess :alpha 0.2})
    ;; Custom colors
    (scales/scale-color-manual {:setosa "#E74C3C"
                                :versicolor "#3498DB"
                                :virginica "#2ECC71"})
    ;; Labels
    (gg/labs :title "Iris Petal Dimensions"
             :subtitle "Scatter plot with LOESS smoothing"
             :x "Petal Length (cm)"
             :y "Petal Width (cm)"
             :color "Species")
    ;; Render
    gg/render)

;; ## Understanding the Dataflow
;;
;; Let's peek under the hood to see how ggplot specs compile to V2 dataflow

;; ### Step 1: Create the spec

(def my-plot
  (-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
      (gg/geom-point {:alpha 0.5})))

;; ### Step 2: Inspect the :sub/map (before inference)

(:sub/map my-plot)

;; You'll see:
;; - `:=data` - The iris dataset
;; - `:=global-aes` - The global aesthetic mappings
;; - `:=layers` - Vector of layer specifications

;; ### Step 3: Inspect a layer

(first (:=layers (:sub/map my-plot)))

;; Shows:
;; - `:geom` - :point
;; - `:local-aes` - Empty (using global)
;; - `:attributes` - {:alpha 0.5}

;; ### Step 4: Render (run inference)

(def rendered (gg/render my-plot))

;; ### Step 5: See what inference computed

(keys rendered)

;; All the :=subkeys have been resolved to actual values!

;; ## Comparison with R ggplot2

;; R:
;; ```r
;; ggplot(iris, aes(x = Sepal.Length, y = Sepal.Width, color = Species)) +
;;   geom_point(alpha = 0.6) +
;;   geom_smooth(method = "lm") +
;;   scale_color_manual(values = c("red", "blue", "green")) +
;;   labs(title = "Iris", x = "Length", y = "Width") +
;;   facet_wrap(~Species)
;; ```

;; Clojure:
;; ```clojure
;; (-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
;;     (gg/geom-point {:alpha 0.6})
;;     (gg/geom-smooth {:method :lm})
;;     (scales/scale-color-manual {:setosa "red" :versicolor "blue" :virginica "green"})
;;     (gg/labs :title "Iris" :x "Length" :y "Width")
;;     (gg/facet-wrap :Species)
;;     gg/render)
;; ```

;; ## Summary
;;
;; The ggplot2 API provides:
;;
;; 1. **Familiar syntax** - Similar to R's ggplot2
;; 2. **Hierarchical aesthetics** - Global + local with proper override semantics
;; 3. **Composable** - Use threading (`->`) to build plots incrementally
;; 4. **Powerful** - Layering, faceting, scale customization, themes
;; 5. **Flexible** - Built on V2 dataflow, so specs are just data
;; 6. **Beautiful defaults** - Uses ggplot2's classic grey theme by default
;;
;; ## Available Functions
;;
;; **Core:**
;; - `ggplot` - Initialize plot
;; - `aes` - Create aesthetic mapping
;; - `render` - Generate visualization
;;
;; **Geoms:**
;; - `geom-point` - Scatter plot
;; - `geom-line` - Line plot
;; - `geom-bar` - Bar chart
;; - `geom-smooth` - Smoothed line with trend
;; - `geom-histogram` - Histogram
;;
;; **Scales:**
;; - `scale-color-manual` - Custom color palette
;; - `scale-x-log10`, `scale-y-log10` - Log scales
;; - `scale-x-sqrt`, `scale-y-sqrt` - Square root scales
;; - `scale-x-continuous`, `scale-y-continuous` - Custom continuous scales
;; - `scale-x-discrete`, `scale-y-discrete` - Custom categorical scales
;; - `scale-size` - Custom size scale
;;
;; **Labels:**
;; - `labs` - Set title, subtitle, axis labels, legend labels
;;
;; **Faceting:**
;; - `facet-wrap` - Single variable faceting
;; - `facet-grid` - Two-variable grid faceting
;;
;; **Themes:**
;; - `theme` - Apply or customize a theme
;; - `theme-grey` - Default ggplot2 grey theme (default)
;; - `theme-bw` - Black and white theme
;; - `theme-minimal` - Minimal theme
;; - `theme-classic` - Classic theme with axis lines
;; - `theme-dark` - Dark background theme
;; - `theme-light` - Light background theme
;; - `theme-void` - Completely empty theme
;; - `theme-linedraw` - Only black lines
;;
;; ## Next Steps
;;
;; - More geoms (boxplot, violin, heatmap, etc.)
;; - Coordinate systems (coord-flip, coord-polar)
;; - Statistical transformations (beyond smooth)
;; - Position adjustments (dodge, stack, jitter)
