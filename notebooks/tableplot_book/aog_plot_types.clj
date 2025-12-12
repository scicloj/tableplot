;; # AlgebraOfGraphics Plot Types
;;
;; Comprehensive examples of all supported plot types in tableplot's AoG implementation.

(ns tableplot-book.aog-plot-types
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Basic Plot Types

;; ### Scatter Plot
;;
;; Points showing individual data values.

(def scatter-data
  {:x (vec (repeatedly 50 rand))
   :y (vec (repeatedly 50 rand))})

^kind/md
["**Scatter Plot** - Individual points"]

(aog/draw
 (aog/* (aog/data scatter-data)
        (aog/mapping :x :y)
        (aog/scatter)))

;; ### Line Plot
;;
;; Connected points showing trends or time series.

(def line-data
  (let [x (vec (range 0 10 0.2))]
    {:x x
     :y (vec (map #(Math/sin %) x))}))

^kind/md
["**Line Plot** - Connected points (sine wave)"]

(aog/draw
 (aog/* (aog/data line-data)
        (aog/mapping :x :y)
        (aog/line)))

;; ### Bar Chart
;;
;; Categorical comparisons using bar heights.

(def bar-data
  {:category ["A" "B" "C" "D" "E"]
   :value [15 28 12 35 22]})

^kind/md
["**Bar Chart** - Categorical values"]

(aog/draw
 (aog/* (aog/data bar-data)
        (aog/mapping :category :value)
        (aog/bar)))

;; ### Grouped Bar Chart
;;
;; Multiple bars per category, grouped by color.

(def grouped-bar-data
  {:category (vec (mapcat #(repeat 3 %) ["Q1" "Q2" "Q3" "Q4"]))
   :product (vec (take 12 (cycle ["Product A" "Product B" "Product C"])))
   :sales (vec (repeatedly 12 #(+ 50 (rand-int 100))))})

^kind/md
["**Grouped Bar Chart** - Multiple series per category"]

(aog/draw
 (aog/* (aog/data grouped-bar-data)
        (aog/mapping :category :sales {:color :product})
        (aog/bar)))

;; ## Statistical Plots

;; ### Histogram
;;
;; Distribution of continuous data using binned bars.

(def hist-data
  {:values (vec (repeatedly 1000 #(* (- (rand) 0.5) 6)))})

^kind/md
["**Histogram** - Distribution of values (20 bins)"]

(aog/draw
 (aog/* (aog/data hist-data)
        (aog/mapping :values)
        (aog/histogram {:bins 20})))

;; ### Density Plot
;;
;; Smooth distribution curve using kernel density estimation.

(def density-data
  {:values (vec (repeatedly 500 #(+ 5 (* 2 (- (rand) 0.5)))))})

^kind/md
["**Density Plot** - Kernel density estimation (Gaussian kernel)"]

(aog/draw
 (aog/* (aog/data density-data)
        (aog/mapping :values)
        (aog/density)))

;; ### Box Plot
;;
;; Distribution summary showing quartiles, median, and outliers.

(def boxplot-data
  {:group (vec (mapcat #(repeat 30 %) ["Group A" "Group B" "Group C"]))
   :value (vec (concat
                (repeatedly 30 #(+ 50 (* 10 (- (rand) 0.5))))
                (repeatedly 30 #(+ 70 (* 15 (- (rand) 0.5))))
                (repeatedly 30 #(+ 60 (* 8 (- (rand) 0.5))))))})

^kind/md
["**Box Plot** - Distribution quartiles and outliers"]

(aog/draw
 (aog/* (aog/data boxplot-data)
        (aog/mapping :group :value)
        (aog/boxplot)))

;; ### Violin Plot
;;
;; Distribution shape using kernel density, mirrored vertically.

^kind/md
["**Violin Plot** - Distribution density visualization"]

(aog/draw
 (aog/* (aog/data boxplot-data)
        (aog/mapping :group :value)
        (aog/violin)))

;; ## Regression and Smoothing

;; ### Linear Regression
;;
;; Scatter plot with fitted linear regression line.

(def regression-data
  (let [x (vec (repeatedly 80 rand))
        y (vec (map #(+ (* 3 %) 2 (* 0.5 (- (rand) 0.5))) x))]
    {:x x :y y}))

^kind/md
["**Linear Regression** - Scatter with fitted line"]

(aog/draw
 (aog/* (aog/data regression-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))

;; ### LOESS Smoothing
;;
;; Scatter plot with smooth (locally weighted) curve.

(def smooth-data
  (let [x (vec (repeatedly 100 rand))
        y (vec (map #(+ (rand) (* 5 % %)) x))]
    {:x x :y y}))

^kind/md
["**LOESS Smoothing** - Scatter with smooth curve (quadratic data)"]

(aog/draw
 (aog/* (aog/data smooth-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.4})
               (aog/smooth))))

;; ### Heatmap
;;
;; 2D grid visualization using color to encode values.

(def heatmap-data
  (let [x (vec (range 8))
        y (vec (range 6))]
    {:x (vec (for [xi x yi y] xi))
     :y (vec (for [xi x yi y] yi))
     :value (vec (for [xi x yi y] (+ (* xi yi) (* 0.5 (rand)))))}))

^kind/md
["**Heatmap** - 2D grid with color-encoded values"]

(aog/draw
 (aog/* (aog/data heatmap-data)
        (aog/mapping :x :y {:color :value})
        (aog/heatmap)))

;; ## Multi-layer Compositions

;; ### Triple Layer: Scatter + Line + Smooth
;;
;; Demonstrating AoG's `+` operator for layering.

(def multi-layer-data
  (let [x (vec (range 0 10 0.1))
        y (vec (map #(+ (Math/sin %) (* 0.3 (- (rand) 0.5))) x))]
    {:x x :y y}))

^kind/md
["**Multi-layer Plot** - Three plot types combined: scatter (α=0.3) + line (α=0.5) + smooth (α=0.8)"]

(aog/draw
 (aog/* (aog/data multi-layer-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.3})
               (aog/line {:alpha 0.5})
               (aog/smooth))))

;; ### Grouped Regression
;;
;; Multiple regression lines by group.

(def grouped-regression-data
  (let [n 40
        x-a (vec (repeatedly n rand))
        y-a (vec (map #(+ (* 2 %) 10 (* 0.5 (- (rand) 0.5))) x-a))
        x-b (vec (repeatedly n rand))
        y-b (vec (map #(+ (* -1.5 %) 80 (* 0.5 (- (rand) 0.5))) x-b))]
    {:x (vec (concat x-a x-b))
     :y (vec (concat y-a y-b))
     :group (vec (concat (repeat n "Positive Trend") (repeat n "Negative Trend")))}))

^kind/md
["**Grouped Regression** - Separate regression lines per group"]

(aog/draw
 (aog/* (aog/data grouped-regression-data)
        (aog/mapping :x :y {:color :group})
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))

;; ### Grouped Density
;;
;; Multiple density curves overlaid.

(def grouped-density-data
  (let [n 500
        vals-a (vec (repeatedly n #(+ 3 (* 1.5 (- (rand) 0.5)))))
        vals-b (vec (repeatedly n #(+ 7 (* 2 (- (rand) 0.5)))))]
    {:values (vec (concat vals-a vals-b))
     :distribution (vec (concat (repeat n "Distribution A") (repeat n "Distribution B")))}))

^kind/md
["**Grouped Density** - Multiple distributions compared"]

(aog/draw
 (aog/* (aog/data grouped-density-data)
        (aog/mapping :values {:color :distribution})
        (aog/density)))

;; ## Statistical Aggregations

;; ### Frequency (Count) Table
;;
;; Count occurrences of each unique value or combination.

(def frequency-data
  {:category (vec (repeatedly 100 #(rand-nth ["Product A" "Product B" "Product C" "Product D"])))
   :region (vec (repeatedly 100 #(rand-nth ["North" "South" "East" "West"])))})

^kind/md
["**Frequency** - Count occurrences per category"]

(aog/draw
 (aog/* (aog/data frequency-data)
        (aog/mapping :category)
        (aog/frequency)))

;; ### Expectation (Mean) Values
;;
;; Compute mean of y for each unique x value.

(def expectation-data
  (let [n 60
        x (vec (repeatedly n #(rand-nth [1 2 3 4 5])))
        y (vec (map #(+ (* 10 %) (* 5 (- (rand) 0.5))) x))]
    {:x x :y y}))

^kind/md
["**Expectation** - Mean y value for each x (with scatter overlay)"]

(aog/draw
 (aog/* (aog/data expectation-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.3})
               (aog/expectation))))

;; ## Summary
;;
;; All supported plot types:
;;
;; **Basic:**
;; - `scatter` - Individual points
;; - `line` - Connected points
;; - `bar` - Categorical bars
;; - `heatmap` - 2D grid with color encoding
;;
;; **Statistical:**
;; - `histogram` - Binned distribution
;; - `density` - Kernel density estimation
;; - `boxplot` - Quartile summary
;; - `violin` - Density shape
;;
;; **Transformations:**
;; - `linear` - Linear regression
;; - `smooth` - LOESS smoothing
;; - `frequency` - Count aggregation
;; - `expectation` - Mean/conditional expectation
;;
;; **Composition:**
;; - `+` - Layer overlay
;; - `*` - Specification merge
;; - `{:color ...}` - Grouping by aesthetics

^kind/md
["
## Next Steps

See also:
- `theme_showcase.clj` - Theme variants and styling
- `aog_tutorial.clj` - Comprehensive AoG tutorial
- `THEMES_GUIDE.md` - Theme documentation
"]
