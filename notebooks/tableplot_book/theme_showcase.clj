;; # Tableplot Themes Showcase
;; 
;; Visual comparison of tableplot's ggplot2-inspired themes with Clojure-specific refinements.
;;
;; This notebook demonstrates the three main theme variants:
;; - **Subtle**: Classic ggplot2 with tasteful Clojure green accent
;; - **Balanced**: Modern, web-optimized (DEFAULT)
;; - **Bold**: High-impact, presentation-ready

(ns tableplot-book.theme-showcase
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Sample Data
;;
;; We'll use three datasets to showcase different plot types:

(def sample-data
  "Simple scatter data with linear trend"
  {:x (vec (range 50))
   :y (vec (map #(+ (* 2 %) 10 (* 5 (- (rand) 0.5))) (range 50)))})

(def categorical-data
  "Multi-group categorical data for color comparison"
  (let [n 30
        groups ["Group A" "Group B" "Group C"]]
    {:x (vec (repeatedly (* 3 n) #(rand)))
     :y (vec (repeatedly (* 3 n) #(* (- (rand) 0.5) 10)))
     :group (vec (mapcat #(repeat n %) groups))}))

(def regression-data
  "Data for regression line demonstration"
  (let [x (vec (repeatedly 80 rand))
        y (vec (map #(+ (* 3 %) 2 (* 0.5 (- (rand) 0.5))) x))]
    {:x x :y y}))

;; ## Theme Comparison: Simple Scatter
;;
;; Let's start with a basic scatter plot across all three themes.

;; ### Subtle Theme

^kind/md
["**Subtle Theme** - Classic ggplot2 with Clojure green as primary accent. Professional and familiar."]

(aog/draw
 (aog/* (aog/data sample-data)
        (aog/mapping :x :y)
        (aog/scatter))
 {:theme :tableplot-subtle})

;; ### Balanced Theme (DEFAULT)

^kind/md
["**Balanced Theme** - Modern, web-optimized with slightly larger marks and vibrant colors. This is the default."]

(aog/draw
 (aog/* (aog/data sample-data)
        (aog/mapping :x :y)
        (aog/scatter))
 {:theme :tableplot-balanced})

;; Or equivalently (balanced is the default):

(aog/draw
 (aog/* (aog/data sample-data)
        (aog/mapping :x :y)
        (aog/scatter)))

;; ### Bold Theme

^kind/md
["**Bold Theme** - High-impact with saturated Clojure green, largest marks, and thickest lines. Perfect for presentations."]

(aog/draw
 (aog/* (aog/data sample-data)
        (aog/mapping :x :y)
        (aog/scatter))
 {:theme :tableplot-bold})

;; ## Color Palette Comparison: Categorical Data
;;
;; The real differences shine when using multiple colors. Here we plot three groups
;; to see how each theme's color palette performs.

;; ### Subtle Palette

^kind/md
["**Subtle Palette**: `[#5E9B4D, #4A90E2, #E87722, ...]` - Clojure green + neutral complements"]

(aog/draw
 (aog/* (aog/data categorical-data)
        (aog/mapping :x :y {:color :group})
        (aog/scatter))
 {:theme :tableplot-subtle})

;; ### Balanced Palette (DEFAULT)

^kind/md
["**Balanced Palette**: `[#5E9B4D, #63B8D1, #F39C12, ...]` - Vibrant, saturated companions to Clojure green"]

(aog/draw
 (aog/* (aog/data categorical-data)
        (aog/mapping :x :y {:color :group})
        (aog/scatter))
 {:theme :tableplot-balanced})

;; ### Bold Palette

^kind/md
["**Bold Palette**: `[#63C356, #3498DB, #E67E22, ...]` - Saturated Clojure green (#63C356) with high-contrast companions"]

(aog/draw
 (aog/* (aog/data categorical-data)
        (aog/mapping :x :y {:color :group})
        (aog/scatter))
 {:theme :tableplot-bold})

;; ## Line Rendering: Regression Plots
;;
;; Multi-layer plots show how line widths and mark sizes interact.

;; ### Subtle: Lines + Points

^kind/md
["**Subtle**: 2px lines, 80pt points, standard opacity"]

(aog/draw
 (aog/* (aog/data regression-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter)
               (aog/linear)))
 {:theme :tableplot-subtle})

;; ### Balanced: Lines + Points (DEFAULT)

^kind/md
["**Balanced**: 2.5px lines, 90pt points, refined grid"]

(aog/draw
 (aog/* (aog/data regression-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter)
               (aog/linear)))
 {:theme :tableplot-balanced})

;; ### Bold: Lines + Points

^kind/md
["**Bold**: 3px lines, 100pt points, larger text"]

(aog/draw
 (aog/* (aog/data regression-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter)
               (aog/linear)))
 {:theme :tableplot-bold})

;; ## Reference Themes
;;
;; For comparison, here are the standard ggplot2 and Vega-Lite themes.

;; ### Classic ggplot2 Theme

^kind/md
["**ggplot2 Theme** - Pure ggplot2 aesthetics without Clojure modifications"]

(aog/draw
 (aog/* (aog/data categorical-data)
        (aog/mapping :x :y {:color :group})
        (aog/scatter))
 {:theme :ggplot2})

;; ### Vega-Lite Default

^kind/md
["**Vega Default** - Vega-Lite's native defaults (white background, light grid)"]

(aog/draw
 (aog/* (aog/data categorical-data)
        (aog/mapping :x :y {:color :group})
        (aog/scatter))
 {:theme :vega})

;; ## Theme Characteristics Summary

^kind/md
["
| Aspect | Subtle | Balanced | Bold |
|--------|--------|----------|------|
| **Point size** | 80 | 90 | 100 |
| **Line width** | 2px | 2.5px | 3px |
| **Grid width** | 0.5px | 0.6px | 0.7px |
| **Label size** | 11pt | 11pt | 12pt |
| **Title size** | 12pt | 12pt | 13pt |
| **Primary color** | #5E9B4D | #5E9B4D | #63C356 |
| **Best for** | Publications | Notebooks | Presentations |
"]

;; ## Usage Guide

^kind/md
["
### Basic Usage

```clojure
;; Default (balanced)
(aog/draw layers)

;; Explicit theme
(aog/draw layers {:theme :tableplot-bold})

;; Custom theme
(aog/draw layers {:theme {:background \"#FFFFFF\"
                          :point {:size 120}
                          :range {:category [\"#FF6B6B\" \"#4ECDC4\"]}}})
```

### Available Themes

- `:tableplot-subtle` - Professional, close to classic ggplot2
- `:tableplot-balanced` - Modern, web-optimized (DEFAULT)
- `:tableplot-bold` - High-impact, presentation-ready
- `:ggplot2` - Pure ggplot2 without modifications
- `:vega` - Vega-Lite defaults

### Design Philosophy

The tableplot themes are **ggplot2-inspired** with **Clojure-specific refinements**:

1. **Familiar** - Gray background + white grid (ggplot2 signature)
2. **Distinctive** - Clojure green as primary accent color
3. **Pragmatic** - Web-optimized sizing, interactive tooltips
4. **Charming** - Modern refinements (thinner grids, rounded strokes)

### Key Improvements Over Classic ggplot2

- Lighter grid lines (0.5-0.7px vs 1px) - less visual clutter
- Larger marks (80-100 vs 60) - better for screens
- Rounded line joins - smoother appearance
- Interactive tooltips enabled by default
- Clojure green branding

---

**Documentation:** See `THEMES_GUIDE.md` for detailed specifications.
"]
