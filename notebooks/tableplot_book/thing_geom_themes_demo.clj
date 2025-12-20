;; # thi.ng/geom Theme Showcase
;;
;; This notebook demonstrates all ggplot2-compatible themes available for the thi.ng/geom backend.
;;
;; Each theme controls:
;; - Panel background color
;; - Plot background color
;; - Grid line colors and styles
;; - Default data colors and styling

(ns tableplot-book.thing-geom-themes-demo
  (:require [scicloj.tableplot.v1.aog.thing-geom :as tg]
            [scicloj.kindly.v4.kind :as kind]))

^:kindly/hide-code
(def md (comp kind/md))

(md "# thi.ng/geom Theme System

The thi.ng/geom backend now supports **ggplot2-compatible themes** that control:

- **Panel background** - The background color of the plotting area
- **Plot background** - The background color of the entire figure
- **Grid styling** - Major/minor grid lines, colors, and widths
- **Default data styling** - Colors, stroke widths, opacity

All themes are designed to match ggplot2's visual style.")

;; ## Sample Data

(def scatter-data
  {:plottype :scatter
   :positional [[1 2 3 4 5 6 7 8]
                [2 4 3 5 6 4 7 5]]
   :named {}})

(def line-data
  {:plottype :line
   :positional [(vec (range 0 10 0.2))
                (vec (map #(Math/sin %) (range 0 10 0.2)))]
   :named {}})

(def bar-data
  {:plottype :bar
   :positional [[1 2 3 4 5]
                [23 45 34 56 42]]
   :named {}})

;; ## Theme Gallery

(md "## :grey (Default)

The default ggplot2 theme with grey panel background and white grid lines.")

(tg/entry->svg scatter-data {:theme :grey :width 500 :height 350})

(md "**Line plot with :grey theme:**")

(tg/entry->svg line-data {:theme :grey :width 500 :height 350})

(md "## :bw (Black & White)

Clean black and white theme with grey grid lines.")

(tg/entry->svg scatter-data {:theme :bw :width 500 :height 350})

(md "## :minimal

Minimalist theme with subtle grey grid lines on white background.")

(tg/entry->svg scatter-data {:theme :minimal :width 500 :height 350})

(md "## :classic

Classic look with no grid lines and simple axes.")

(tg/entry->svg scatter-data {:theme :classic :width 500 :height 350})

(md "## :dark

Dark theme optimized for presentations and dark mode displays.")

(tg/entry->svg scatter-data {:theme :dark :width 500 :height 350})

(md "**Line plot with :dark theme:**")

(tg/entry->svg line-data {:theme :dark :width 500 :height 350})

(md "## :light

Light theme with very subtle grid lines.")

(tg/entry->svg scatter-data {:theme :light :width 500 :height 350})

(md "## :void

Completely minimal - no grid, no panel background.")

(tg/entry->svg scatter-data {:theme :void :width 500 :height 350})

(md "## :linedraw

Crisp theme with strong black grid lines.")

(tg/entry->svg scatter-data {:theme :linedraw :width 500 :height 350})

(md "## :tableplot

The tableplot default theme (similar to :grey).")

(tg/entry->svg scatter-data {:theme :tableplot :width 500 :height 350})

;; ## Multi-layer Plots with Themes

(md "## Multi-Layer Example

Themes work seamlessly with multi-layer plots:")

(def multi-scatter
  {:plottype :scatter
   :positional [[1 2 3 4 5 6 7 8]
                [2 4 3 5 6 4 7 5]]
   :named {:color "#0af"}})

(def multi-line
  {:plottype :line
   :positional [[1 2 3 4 5 6 7 8]
                [2 4 3 5 6 4 7 5]]
   :named {:stroke "#f80" :stroke-width 2}})

(md "**Multi-layer with :grey theme:**")

(tg/entries->svg [multi-scatter multi-line] {:theme :grey :width 500 :height 350})

(md "**Multi-layer with :dark theme:**")

(tg/entries->svg [multi-scatter multi-line] {:theme :dark :width 500 :height 350})

(md "**Multi-layer with :minimal theme:**")

(tg/entries->svg [multi-scatter multi-line] {:theme :minimal :width 500 :height 350})

;; ## Bar Charts with Themes

(md "## Bar Charts

Bar charts work with all themes:")

(md "**Bar chart with :grey theme:**")

(tg/entry->svg bar-data {:theme :grey :width 500 :height 350})

(md "**Bar chart with :dark theme:**")

(tg/entry->svg bar-data {:theme :dark :width 500 :height 350})

(md "**Bar chart with :linedraw theme:**")

(tg/entry->svg bar-data {:theme :linedraw :width 500 :height 350})

;; ## Polar Plots with Themes

(md "## Polar Plots

Themes also work with polar coordinate plots:")

(def rose-data
  (let [theta (range 0 (* 2 Math/PI) 0.01)
        r (map (fn [t] (* 8 (Math/cos (* 5 t)))) theta)]
    {:plottype :line
     :positional [(vec theta) (vec r)]
     :named {}}))

(md "**5-petaled rose with :grey theme:**")

(tg/entry->svg rose-data {:theme :grey :polar true :width 500 :height 500})

(md "**5-petaled rose with :dark theme:**")

(tg/entry->svg rose-data {:theme :dark :polar true :width 500 :height 500})

(md "**5-petaled rose with :minimal theme:**")

(tg/entry->svg rose-data {:theme :minimal :polar true :width 500 :height 500})

;; ## Theme Comparison Table

(md "## Theme Comparison

| Theme | Panel BG | Plot BG | Grid Color | Best For |
|-------|----------|---------|------------|----------|
| **:grey** | Grey (#ebebeb) | White | White (#ffffff) | General use (ggplot2 default) |
| **:bw** | White | White | Light grey (#d9d9d9) | Publications, black & white printing |
| **:minimal** | White | White | Very light grey (#f0f0f0) | Clean, modern look |
| **:classic** | White | White | None | Simple, traditional plots |
| **:dark** | Dark grey (#333333) | Very dark (#222222) | Medium grey (#555555) | Presentations, dark mode |
| **:light** | Very light (#fafafa) | White | Light grey (#e0e0e0) | Subtle, airy aesthetic |
| **:void** | White | White | None | Absolutely minimal |
| **:linedraw** | White | White | Black (#000000) | High contrast, crisp lines |
| **:tableplot** | Grey (#ebebeb) | White | White (#ffffff) | tableplot default |")

;; ## Using Themes

(md "## How to Use Themes

Simply pass the `:theme` option to `entry->svg` or `entries->svg`:

```clojure
;; Single entry
(tg/entry->svg my-entry {:theme :dark :width 600 :height 400})

;; Multiple entries
(tg/entries->svg [entry1 entry2] {:theme :minimal :width 600 :height 400})

;; Polar plot with theme
(tg/entry->svg polar-entry {:theme :grey :polar true :width 500 :height 500})
```

Default theme is `:grey` (matching ggplot2 defaults).")

(md "## Custom Styling

You can still override theme defaults with entry-specific styling:

```clojure
{:plottype :scatter
 :positional [[1 2 3] [4 5 6]]
 :named {:color \"#ff0000\"        ; Override default color
         :stroke-width 3           ; Override default stroke width
         :alpha 0.5}}              ; Override default opacity
```

Entry-specific styling always takes precedence over theme defaults.")

(md "## Summary

✅ **9 ggplot2-compatible themes** available  
✅ **Panel & plot backgrounds** fully themed  
✅ **Grid styling** (colors, widths, major/minor)  
✅ **Works with all plot types** (scatter, line, bar, etc.)  
✅ **Polar coordinate support** included  
✅ **Multi-layer plots** fully supported  
✅ **Entry-level overrides** preserved

The theme system brings ggplot2's visual consistency to pure Clojure SVG rendering!")
