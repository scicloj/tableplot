;; # thi.ng/geom Backend - AoG Integration Tests
;;
;; This notebook tests the full AlgebraOfGraphics pipeline with the thi.ng/geom backend.
;; It validates that all AoG operators (* and +), transformations, and scale modifications
;; work correctly end-to-end.

(ns tableplot-book.thing-geom-aog-integration
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.thing-geom :as tg]
            [scicloj.tableplot.v1.aog.processing :as processing]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Basic AoG Pipeline
;;
;; Test the fundamental AoG workflow: data -> mapping -> visual -> draw

;; ### Simple Scatter Plot

(def simple-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]})

(-> (aog/data simple-data)
    (aog/* (aog/mapping :x :y))
    (aog/* (aog/scatter {:alpha 0.7}))
    (aog/draw {:backend :vegalite}))

;; Now with thi.ng/geom backend
;; (Currently using direct API since backend routing needs implementation)

(let [layers (aog/* (aog/data simple-data)
                    (aog/mapping :x :y)
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Line Plot

(def line-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1 4 9 16 25 36 49 64 81 100]})

(let [layers (aog/* (aog/data line-data)
                    (aog/mapping :x :y)
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Bar Chart

(def bar-data
  {:category ["A" "B" "C" "D" "E"]
   :value [23 45 12 67 34]})

(let [layers (aog/* (aog/data bar-data)
                    (aog/mapping :category :value)
                    (aog/bar))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Multi-Layer Composition with +
;;
;; Test layering multiple visual types using the + operator

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

;; ### Multiple Lines (Time Series)

(def multi-series-data
  {:time [1 2 3 4 5 6 7 8 9 10]
   :series-a [10 12 15 13 17 19 18 22 21 25]
   :series-b [5 7 6 9 8 11 10 13 14 12]
   :series-c [15 14 16 15 18 17 20 19 22 21]})

;; We need to create separate layers for each series
;; This requires reshaping the data or creating multiple layer specs

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
;; Test merging layers using the * operator

;; ### Adding Color Aesthetic

(def colored-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]
   :group ["A" "A" "B" "B" "A" "B" "A" "B" "A" "B"]})

(let [layers (aog/* (aog/data colored-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:color :group})
                    (aog/scatter {:alpha 0.7}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Multiple Aesthetics

(def multi-aesthetic-data
  {:x [1 2 3 4 5 6 7 8]
   :y [2 4 3 5 6 4 7 5]
   :category ["A" "B" "A" "B" "A" "B" "A" "B"]
   :size [10 20 15 25 12 22 18 28]})

(let [layers (aog/* (aog/data multi-aesthetic-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:color :category :size :size})
                    (aog/scatter {:alpha 0.7}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Statistical Transformations
;;
;; Test AoG's statistical transformation layers

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

;; ### Frequency (Histogram-like)

(def frequency-data
  {:category ["A" "B" "A" "C" "B" "A" "B" "C" "A" "A"
              "B" "C" "A" "B" "A" "C" "B" "B" "A" "C"]})

(let [layers (aog/* (aog/data frequency-data)
                    (aog/mapping :category)
                    (aog/frequency))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Expectation (Mean)

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
;; Test logarithmic and power scale transformations

;; ### Logarithmic Scale (Y-axis)

(def log-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1 10 100 1000 10000 100 1000 10000 100000 1000000]})

(let [layers (aog/* (aog/data log-data)
                    (aog/mapping :x :y)
                    (aog/log-scale :y {:base 10})
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Power Scale (Square Root)

(def sqrt-data
  {:x [1 4 9 16 25 36 49 64 81 100]
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
   :y [100 150 200 250 300 350 400 450 500 550]})

(let [layers (aog/* (aog/data domain-data)
                    (aog/mapping :x :y)
                    (aog/scale-domain :x [0 60])
                    (aog/scale-domain :y [0 600])
                    (aog/line))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Complex Compositions
;;
;; Test complex layer combinations that exercise multiple features

;; ### Grouped Data with Multiple Layers

(def grouped-complex-data
  {:x [1 2 3 4 5 6 7 8 9 10
       1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6
       3 5 4 6 7 5 8 6 9 7]
   :group ["A" "A" "A" "A" "A" "A" "A" "A" "A" "A"
           "B" "B" "B" "B" "B" "B" "B" "B" "B" "B"]})

(let [layers (aog/* (aog/data grouped-complex-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:color :group})
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
   :group ["A" "A" "A" "A" "A" "A" "A" "A" "A" "A" "A" "A" "A" "A" "A"
           "B" "B" "B" "B" "B" "B" "B" "B" "B" "B" "B" "B" "B" "B" "B"]})

(let [layers (aog/* (aog/data stat-grouped-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:color :group})
                    (aog/+ (aog/scatter {:alpha 0.3})
                           (aog/linear)))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Multi-Aesthetic with Transformations

(def multi-transform-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 8 16 32 64 128 256 512 1024]})

(let [layers (aog/* (aog/data multi-transform-data)
                    (aog/mapping :x :y)
                    (aog/log-scale :y {:base 2})
                    (aog/+ (aog/scatter {:alpha 0.7})
                           (aog/line {:width 2})))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Polar Coordinates with AoG
;;
;; Test polar coordinate visualizations using the full AoG pipeline

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

;; ## Edge Cases and Error Handling
;;
;; Test boundary conditions and potential error scenarios

;; ### Empty Data

(def empty-data {:x [] :y []})

;; This should either render nothing or show empty axes
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

;; ### Large Dataset

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

;; ### Negative Values

(def negative-data
  {:x [-10 -8 -6 -4 -2 0 2 4 6 8 10]
   :y [-5 -3 -1 2 4 5 4 2 -1 -3 -5]})

(let [layers (aog/* (aog/data negative-data)
                    (aog/mapping :x :y)
                    (aog/+ (aog/scatter)
                           (aog/line)))
      entries (processing/layers->entries layers)]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Summary
;;
;; This notebook provides comprehensive integration testing of the thi.ng/geom backend
;; with the full AlgebraOfGraphics API. It covers:
;;
;; - Basic plot types (scatter, line, bar)
;; - Multi-layer composition with + operator
;; - Cartesian product merging with * operator
;; - Statistical transformations (linear, frequency, expectation, histogram, density)
;; - Scale transformations (log, pow, sqrt, custom domains)
;; - Complex compositions with multiple features
;; - Polar coordinate support
;; - Edge cases (empty data, single points, large datasets, negative values)
;;
;; All examples should render correctly as SVG visualizations when the REPL is available.
