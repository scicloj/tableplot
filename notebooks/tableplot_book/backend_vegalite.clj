;; # Vega-Lite Backend: Complete Guide
;;
;; Comprehensive guide to using Vega-Lite as a backend for AlgebraOfGraphics.
;;
;; **Vega-Lite Strengths:**
;; - 📊 **Faceting** - Small multiples (column, row, facet, repeat)
;; - 🎨 **Themes** - Publication-ready styling (tableplot, ggplot2, vega)
;; - 📄 **Static SVG** - Embeddable, print-ready graphics
;; - 🖥️ **Server-side** - No browser required (via darkstar)
;; - 📐 **Declarative** - Vega-Lite specs are data
;;
;; **Best for:**
;; - Publications and reports
;; - Faceted plots (trellis plots, small multiples)
;; - Themed, publication-ready graphics
;; - Server-side rendering
;; - When you want Vega-Lite specs as output

(ns tableplot-book.backend-vegalite
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.processing :as proc]
            [scicloj.tableplot.v1.aog.vegalite :as vegalite]
            [tablecloth.api :as tc]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

(md "# Vega-Lite Backend Guide

This guide covers everything you need to know about using Vega-Lite as a backend for AlgebraOfGraphics.

**Three ways to use Vega-Lite:**
1. Via AoG's `draw` function (automatic backend selection)
2. Directly via `vegalite/vegalite` function (explicit backend)
3. Entry IR → Vega-Lite spec → Custom rendering

We'll cover all three approaches.")

;; # Part 1: Basic Usage

(md "## Part 1: Basic Vega-Lite Usage

Vega-Lite can be used either through AoG's high-level API or directly with Entry IR.")

;; ## Using AoG with Vega-Lite Backend

;; Simple scatter plot via AoG
(def simple-data
  {:x [1 2 3 4 5 6 7 8]
   :y [2 4 3 5 6 4 7 5]})

;; Via AoG (uses Plotly by default)
(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/scatter)))

;; To use Vega-Lite, convert to Entry and render
(let [layer (aog/* (aog/data simple-data)
                   (aog/mapping :x :y)
                   (aog/scatter))
      entries (proc/layers->entries [layer])]
  (vegalite/vegalite entries))

;; ## Direct Entry IR Usage

;; You can also work directly with Entry IR
(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5]
               [2 4 3 5 6]]
  :named {}})

;; With styling options
(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5]
               [2 4 3 5 6]]
  :named {:alpha 0.5
          :color "#e74c3c"}}
 {:width 500 :height 300})

;; # Part 2: Plot Types

(md "## Part 2: Supported Plot Types

Vega-Lite supports a wide range of 2D plot types:")

;; ## Scatter Plot

(vegalite/vegalite
 {:plottype :scatter
  :positional [(vec (repeatedly 50 rand))
               (vec (repeatedly 50 rand))]
  :named {}})

;; ## Line Plot

(let [x (range 0 10 0.1)
      y (map #(Math/sin %) x)]
  (vegalite/vegalite
   {:plottype :line
    :positional [(vec x) (vec y)]
    :named {}}))

;; ## Bar Chart

(vegalite/vegalite
 {:plottype :bar
  :positional [[1 2 3 4 5]
               [23 45 34 56 42]]
  :named {}}
 {:width 500 :height 350})

;; ## Histogram

(vegalite/vegalite
 {:plottype :histogram
  :positional [(vec (repeatedly 1000 #(* 100 (rand))))]
  :named {:bins 20}}
 {:width 500 :height 350})

;; ## Density Plot

(vegalite/vegalite
 {:plottype :density
  :positional [(vec (repeatedly 500 #(+ 50 (* 15 (- (rand) 0.5)))))]
  :named {}}
 {:width 500 :height 350})

;; ## Box Plot

(let [data (vec (repeatedly 30 #(+ 50 (* 20 (- (rand) 0.5)))))]
  (vegalite/vegalite
   {:plottype :box
    :positional [data]
    :named {}}
   {:width 400 :height 350}))

;; ## Violin Plot

(let [data (vec (repeatedly 50 #(+ 60 (* 15 (- (rand) 0.5)))))]
  (vegalite/vegalite
   {:plottype :violin
    :positional [data]
    :named {}}
   {:width 400 :height 350}))

;; ## Heatmap

(let [x-vals (vec (range 8))
      y-vals (vec (range 6))]
  (vegalite/vegalite
   {:plottype :heatmap
    :positional [(vec (for [xi x-vals yi y-vals] xi))
                 (vec (for [xi x-vals yi y-vals] yi))]
    :named {:color (vec (for [xi x-vals yi y-vals]
                          (+ (* xi yi) (* 0.5 (rand)))))}}
   {:width 500 :height 350}))

;; # Part 3: Faceting (Vega-Lite's Superpower!)

(md "## Part 3: Faceting - Small Multiples

**Faceting is Vega-Lite's unique strength!** It creates small multiples by splitting data across panels.

This feature is **only available in Vega-Lite**, not in Plotly or thi.ng/geom backends.

Three faceting types:
- **`:col`** - Split by column (horizontal)
- **`:row`** - Split by row (vertical)
- **`:facet`** - Split by wrapping")

;; ## Column Faceting

;; Create data with categories
(def facet-data
  (let [species ["Adelie" "Chinstrap" "Gentoo"]
        n-per 20]
    {:bill-length (vec (concat
                        (repeatedly n-per #(+ 38 (* 4 (rand))))
                        (repeatedly n-per #(+ 48 (* 4 (rand))))
                        (repeatedly n-per #(+ 45 (* 4 (rand))))))
     :bill-depth (vec (concat
                       (repeatedly n-per #(+ 18 (* 3 (rand))))
                       (repeatedly n-per #(+ 17 (* 3 (rand))))
                       (repeatedly n-per #(+ 14 (* 3 (rand))))))
     :species (vec (mapcat #(repeat n-per %) species))}))

;; Facet by column (horizontal panels)
(vegalite/vegalite
 {:plottype :scatter
  :positional [(:bill-length facet-data) (:bill-depth facet-data)]
  :named {:col (:species facet-data)}}
 {:width 600 :height 400})

;; ## Row Faceting

;; Facet by row (vertical panels)
(vegalite/vegalite
 {:plottype :scatter
  :positional [(:bill-length facet-data) (:bill-depth facet-data)]
  :named {:row (:species facet-data)}}
 {:width 500 :height 600})

;; ## Combined Column and Row Faceting

;; Create data with two categorical variables
(def two-factor-data
  (let [species ["A" "B"]
        habitat ["Forest" "Ocean"]
        n-per 15]
    {:x (vec (repeatedly (* n-per 4) #(* 10 (rand))))
     :y (vec (repeatedly (* n-per 4) #(* 10 (rand))))
     :species (vec (take (* n-per 4) (cycle (mapcat #(repeat n-per %) species))))
     :habitat (vec (take (* n-per 4) (cycle (mapcat #(repeat (* n-per 2) %) habitat))))}))

;; Facet by both row AND column
(vegalite/vegalite
 {:plottype :scatter
  :positional [(:x two-factor-data) (:y two-factor-data)]
  :named {:col (:species two-factor-data)
          :row (:habitat two-factor-data)}}
 {:width 600 :height 500})

;; ## Faceting with Color

;; Combine faceting with color aesthetic
(def rich-facet-data
  (let [species ["Adelie" "Chinstrap" "Gentoo"]
        sex ["Male" "Female"]
        n-per 10]
    {:bill-length (vec (repeatedly (* n-per 6) #(+ 40 (* 10 (rand)))))
     :bill-depth (vec (repeatedly (* n-per 6) #(+ 16 (* 4 (rand)))))
     :species (vec (take (* n-per 6) (cycle (mapcat #(repeat (* n-per 2) %) species))))
     :sex (vec (take (* n-per 6) (cycle (mapcat #(repeat n-per %) sex))))}))

;; Facet by species, color by sex
(vegalite/vegalite
 {:plottype :scatter
  :positional [(:bill-length rich-facet-data) (:bill-depth rich-facet-data)]
  :named {:col (:species rich-facet-data)
          :color (:sex rich-facet-data)}}
 {:width 700 :height 350})

;; ## Faceted Line Plots

(def time-series-facet
  (let [regions ["North" "South" "East"]
        n 30]
    {:time (vec (concat (range n) (range n) (range n)))
     :value (vec (concat
                  (map #(+ 50 (* 2 %) (* 5 (rand))) (range n))
                  (map #(+ 40 (* 1.5 %) (* 5 (rand))) (range n))
                  (map #(+ 60 (* 1 %) (* 5 (rand))) (range n))))
     :region (vec (mapcat #(repeat n %) regions))}))

(vegalite/vegalite
 {:plottype :line
  :positional [(:time time-series-facet) (:value time-series-facet)]
  :named {:col (:region time-series-facet)}}
 {:width 700 :height 300})

;; ## Faceted Histograms

(def histogram-facet-data
  (let [groups ["Group A" "Group B" "Group C"]
        n 100]
    {:values (vec (concat
                   (repeatedly n #(+ 40 (* 10 (rand))))
                   (repeatedly n #(+ 60 (* 12 (rand))))
                   (repeatedly n #(+ 50 (* 8 (rand))))))
     :group (vec (mapcat #(repeat n %) groups))}))

(vegalite/vegalite
 {:plottype :histogram
  :positional [(:values histogram-facet-data)]
  :named {:col (:group histogram-facet-data)
          :bins 15}}
 {:width 700 :height 300})

;; # Part 4: Themes

(md "## Part 4: Vega-Lite Themes

Vega-Lite supports multiple themes for publication-ready graphics:

- **`:tableplot-balanced`** (default) - Balanced, professional
- **`:tableplot-subtle`** - Subtle, minimal
- **`:tableplot-bold`** - Bold, high contrast
- **`:ggplot2`** - Matches ggplot2 aesthetics
- **`:vega`** - Vega's minimal theme
- Custom theme maps")

;; ## Default Theme: tableplot-balanced

(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5 6 7 8]
               [2 4 3 5 6 4 7 5]]
  :named {}}
 {:theme :tableplot-balanced
  :width 500 :height 350})

;; ## Theme: tableplot-subtle

(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5 6 7 8]
               [2 4 3 5 6 4 7 5]]
  :named {}}
 {:theme :tableplot-subtle
  :width 500 :height 350})

;; ## Theme: tableplot-bold

(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5 6 7 8]
               [2 4 3 5 6 4 7 5]]
  :named {}}
 {:theme :tableplot-bold
  :width 500 :height 350})

;; ## Theme: ggplot2

(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5 6 7 8]
               [2 4 3 5 6 4 7 5]]
  :named {}}
 {:theme :ggplot2
  :width 500 :height 350})

;; ## Theme: vega (minimal)

(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5 6 7 8]
               [2 4 3 5 6 4 7 5]]
  :named {}}
 {:theme :vega
  :width 500 :height 350})

;; ## Themes with Faceting

;; ggplot2 theme with faceted plot
(vegalite/vegalite
 {:plottype :scatter
  :positional [(:bill-length facet-data) (:bill-depth facet-data)]
  :named {:col (:species facet-data)}}
 {:theme :ggplot2
  :width 650 :height 350})

;; Bold theme with faceted plot
(vegalite/vegalite
 {:plottype :line
  :positional [(:time time-series-facet) (:value time-series-facet)]
  :named {:col (:region time-series-facet)}}
 {:theme :tableplot-bold
  :width 700 :height 300})

;; # Part 5: Multi-Layer Compositions

(md "## Part 5: Multi-Layer Compositions

Vega-Lite supports layering multiple plot types.")

;; ## Scatter + Line

(def scatter-line-data
  {:x [1 2 3 4 5 6 7 8]
   :y [2 4 3 5 6 4 7 5]})

(let [scatter-entry {:plottype :scatter
                     :positional [(:x scatter-line-data) (:y scatter-line-data)]
                     :named {:color "#3498db"}}
      line-entry {:plottype :line
                  :positional [(:x scatter-line-data) (:y scatter-line-data)]
                  :named {:color "#e74c3c" :width 2}}]
  (vegalite/vegalite [scatter-entry line-entry]
                     {:width 500 :height 350}))

;; ## Scatter + Smooth Trend

(def trend-data
  (let [x (vec (range 20))
        y (vec (map #(+ (* 2 %) 10 (* 5 (rand))) x))
        trend-y (vec (map #(+ (* 2 %) 10) x))]
    {:x x
     :y y
     :trend-y trend-y}))

(let [scatter {:plottype :scatter
               :positional [(:x trend-data) (:y trend-data)]
               :named {:alpha 0.6}}
      trend {:plottype :line
             :positional [(:x trend-data) (:trend-y trend-data)]
             :named {:color "#e74c3c" :width 3}}]
  (vegalite/vegalite [scatter trend]
                     {:width 500 :height 350}))

;; ## Multiple Lines

(def multi-line-data
  (let [x (vec (range 0 10 0.2))]
    {:x x
     :sin (vec (map #(Math/sin %) x))
     :cos (vec (map #(Math/cos %) x))}))

(let [sin-line {:plottype :line
                :positional [(:x multi-line-data) (:sin multi-line-data)]
                :named {:color "#3498db" :width 2}}
      cos-line {:plottype :line
                :positional [(:x multi-line-data) (:cos multi-line-data)]
                :named {:color "#e74c3c" :width 2}}]
  (vegalite/vegalite [sin-line cos-line]
                     {:width 500 :height 350}))

;; # Part 6: AoG Integration

(md "## Part 6: AoG Integration

Using Vega-Lite with the full AoG pipeline:")

;; ## Simple AoG to Vega-Lite

(def penguins
  (tc/dataset {:bill-length [39.1 39.5 40.3 36.7 39.3 38.9 39.2 34.1]
               :bill-depth [18.7 17.4 18.0 19.3 20.6 17.8 19.6 18.1]
               :species ["Adelie" "Adelie" "Adelie" "Adelie"
                         "Chinstrap" "Chinstrap" "Gentoo" "Gentoo"]}))

(let [layer (aog/* (aog/data penguins)
                   (aog/mapping :bill-length :bill-depth {:color :species})
                   (aog/scatter))
      entries (proc/layers->entries [layer])]
  (vegalite/vegalite entries
                     {:theme :ggplot2
                      :width 500 :height 400}))

;; ## Multi-Layer AoG to Vega-Lite

(let [layers (aog/* (aog/data penguins)
                    (aog/mapping :bill-length :bill-depth {:color :species})
                    (aog/+ (aog/scatter {:alpha 0.6})
                           (aog/line)))
      entries (proc/layers->entries [layers])]
  (vegalite/vegalite entries
                     {:theme :tableplot-balanced
                      :width 500 :height 400}))

;; # Part 7: Advanced Vega-Lite Features

(md "## Part 7: Advanced Features

### Accessing Vega-Lite Specs

One advantage of Vega-Lite is that specs are **data** - you can inspect and modify them.")

;; ## Getting the Vega-Lite Spec

(def my-spec
  (vegalite/entry->vegalite-spec
   {:plottype :scatter
    :positional [[1 2 3] [4 5 6]]
    :named {}}))

;; View the spec
my-spec

;; ## Custom Width/Height

(vegalite/vegalite
 {:plottype :scatter
  :positional [[1 2 3 4 5]
               [2 4 3 5 6]]
  :named {}}
 {:width 800 :height 200}) ; Wide and short

;; ## Combining Faceting and Themes

(vegalite/vegalite
 {:plottype :scatter
  :positional [(:bill-length rich-facet-data) (:bill-depth rich-facet-data)]
  :named {:col (:species rich-facet-data)
          :row (:sex rich-facet-data)
          :color (:species rich-facet-data)}}
 {:theme :ggplot2
  :width 650 :height 500})

;; # Part 8: When to Use Vega-Lite

(md "## Part 8: When to Use Vega-Lite

### ✅ Use Vega-Lite when you need:

1. **Faceted plots** (small multiples)
   - Column/row/facet layouts
   - Trellis plots
   - Comparative visualizations

2. **Static SVG output**
   - Publications and papers
   - Print-ready graphics
   - Embedded SVGs

3. **Themed graphics**
   - Consistent styling
   - Publication-ready aesthetics
   - ggplot2-compatible themes

4. **Server-side rendering**
   - No browser required
   - Batch processing
   - Automated report generation

5. **Declarative specs**
   - Vega-Lite specs as data
   - Programmatic spec manipulation
   - Integration with Vega ecosystem

### ❌ Don't use Vega-Lite when you need:

1. **Interactivity** → Use **Plotly**
2. **3D plots** → Use **Plotly**
3. **Polar coordinates** → Use **thi.ng/geom**
4. **Pure Clojure (no JS)** → Use **thi.ng/geom**")

;; # Part 9: Comparison with Other Backends

(md "## Part 9: Backend Comparison

How does Vega-Lite compare to the other backends?

| Feature | **Plotly** | **Vega-Lite** | **thi.ng/geom** |
|---------|-----------|--------------|----------------|
| **Output Format** | HTML + JS | SVG (via Vega-Lite spec) | SVG (native) |
| **Interactivity** | ✅ Full (pan, zoom, hover) | ⚠️ Limited (tooltips only) | ❌ None (static) |
| **3D Support** | ✅ Native (scatter3d, surface) | ❌ None | ❌ None |
| **Polar Coordinates** | ⚠️ Manual conversion | ❌ None | ✅ Native |
| **Faceting** | ⚠️ Manual subplots | ✅ Built-in (`col`, `row`) | ❌ None |
| **Themes** | ✅ 7+ built-in | ✅ 5 built-in | ✅ 9 ggplot2 themes |
| **File Size** | ~3MB (includes plotly.js) | ~100KB (spec only) | ~10KB (pure SVG) |
| **Performance** | ⚠️ Slower with 100k+ points | ✅ Fast (declarative) | ✅ Very fast |
| **Customization** | ✅ Extensive (Plotly.js API) | ✅ Good (Vega spec) | ✅ Full (thi.ng/geom) |
| **Dependencies** | Plotly.js (browser) | Vega-Lite (JVM) | Pure Clojure |
| **Use Cases** | Dashboards, 3D, EDA | Reports, facets, static | Polar, print, pure SVG |

### Summary

**Choose Vega-Lite when:**
- You need faceting/small multiples
- Static SVG output is preferred
- You want declarative specs
- You're generating reports

**Choose Plotly when:**
- You need rich interactivity (pan, zoom, hover)
- You're creating 3D visualizations
- You're building web-based dashboards
- Output format is HTML (not SVG)

**Choose thi.ng/geom when:**
- You need polar coordinates (radar charts, rose diagrams)
- Pure Clojure solution is preferred
- You want minimal file sizes
- You need ggplot2-style themes")

;; # Summary

(md "## Summary

This guide covered:

1. ✅ **Basic usage** - Entry IR and AoG integration
2. ✅ **Plot types** - All supported 2D plots
3. ✅ **Faceting** - Vega-Lite's superpower (column, row, facet)
4. ✅ **Themes** - Five built-in themes
5. ✅ **Multi-layer** - Composing multiple plot types
6. ✅ **AoG integration** - Full pipeline support
7. ✅ **Advanced features** - Specs, customization
8. ✅ **When to use** - Clear guidance on use cases

**Vega-Lite excels at:**
- 📊 Faceted plots (small multiples)
- 🎨 Themed, publication-ready graphics
- 📄 Static SVG for papers and reports
- 🖥️ Server-side rendering

For faceting, there's no better choice than Vega-Lite!")

(kind/md "**Vega-Lite Guide Complete!** ✅")
