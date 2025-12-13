;; # thi.ng/geom Backend - Styling and Customization Tests
;;
;; This notebook explores advanced styling, customization, and visual properties
;; available in the thi.ng/geom backend.

(ns tableplot-book.thing-geom-styling
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.thing-geom :as tg]
            [scicloj.tableplot.v1.aog.processing :as processing]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Basic Styling Properties
;;
;; Test various styling options for different mark types

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

(def line-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [1 3 2 5 4 7 6 9 8 10]})

;; Default line
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {}}
 {:width 600 :height 400})

;; Custom stroke color
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke "#3498db"}}
 {:width 600 :height 400})

;; Custom stroke width
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke-width 4}}
 {:width 600 :height 400})

;; Dashed line
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke "#e67e22"
          :stroke-width 2
          :stroke-dasharray "5,5"}}
 {:width 600 :height 400})

;; Dotted line
(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke "#1abc9c"
          :stroke-width 3
          :stroke-dasharray "2,3"}}
 {:width 600 :height 400})

;; ### Bar Chart Styling

(def bar-data
  {:x ["A" "B" "C" "D" "E"]
   :y [23 45 12 67 34]})

;; Default bars
(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-data) (:y bar-data)]
  :named {}}
 {:width 600 :height 400})

;; Custom fill color
(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-data) (:y bar-data)]
  :named {:fill "#f39c12"}}
 {:width 600 :height 400})

;; Custom fill with stroke
(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-data) (:y bar-data)]
  :named {:fill "#3498db"
          :stroke "#2c3e50"
          :stroke-width 2}}
 {:width 600 :height 400})

;; Semi-transparent bars
(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-data) (:y bar-data)]
  :named {:fill "#e74c3c"
          :opacity 0.6}}
 {:width 600 :height 400})

;; ## Color Palettes
;;
;; Test different color schemes and palettes

;; ### Categorical Colors

(def categorical-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]
   :category ["A" "B" "C" "D" "A" "B" "C" "D" "A" "B"]})

;; Default categorical colors
(let [layers (aog/* (aog/data categorical-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:color :category})
                    (aog/scatter))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ### Gradient Colors

;; Blue to red gradient
(let [n 20
      x (vec (range n))
      y (vec (map #(+ 5 (Math/sin (/ % 3.0))) x))
      colors (vec (for [i (range n)]
                    (let [ratio (/ i (dec n))
                          r (int (* 255 ratio))
                          b (int (* 255 (- 1 ratio)))]
                      (format "#%02x00%02x" r b))))]
  (tg/entry->svg
   {:plottype :scatter
    :positional [x y]
    :named {:color colors}}
   {:width 600 :height 400}))

;; Green gradient
(let [n 15
      x (vec (range n))
      y (vec (map #(Math/pow % 1.5) x))
      colors (vec (for [i (range n)]
                    (let [ratio (/ i (dec n))
                          g (int (+ 100 (* 155 ratio)))]
                      (format "#00%02x00" g))))]
  (tg/entry->svg
   {:plottype :scatter
    :positional [x y]
    :named {:color colors
            :size 8}}
   {:width 600 :height 400}))

;; ## Size Variations
;;
;; Test different plot sizes and aspect ratios

;; ### Square Plot

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#3498db"}}
 {:width 500 :height 500})

;; ### Wide Plot

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke "#e74c3c" :stroke-width 2}}
 {:width 800 :height 300})

;; ### Tall Plot

(tg/entry->svg
 {:plottype :bar
  :positional [(:x bar-data) (:y bar-data)]
  :named {:fill "#9b59b6"}}
 {:width 300 :height 600})

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
;;
;; Test aesthetics that map to data columns

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

(def multi-aesthetic-data
  {:x [1 2 3 4 5 6 7 8 9 10]
   :y [2 4 3 5 6 4 7 5 8 6]
   :group ["A" "A" "B" "B" "A" "B" "A" "B" "A" "B"]
   :size [4 6 5 7 8 6 9 7 10 8]})

(let [layers (aog/* (aog/data multi-aesthetic-data)
                    (aog/mapping :x :y)
                    (aog/mapping {:color :group :size :size})
                    (aog/scatter {:alpha 0.7}))
      entries (processing/layers->entries [layers])]
  (tg/entries->svg entries {:width 600 :height 400}))

;; ## Polar Plot Styling
;;
;; Test styling options for polar coordinate plots

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

;; ### Dashed Polar Line

(let [theta (vec (range 0 (* 2 Math/PI) 0.05))
      r (vec (repeat (count theta) 8))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#3498db"
            :stroke-width 2
            :stroke-dasharray "10,5"}}
   {:polar true :width 600 :height 600}))

;; ## Multi-Layer Styling
;;
;; Test styling when multiple layers are combined

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

;; ### Filled Area with Outline

(tg/entries->svg
 [{:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2 4 3 5 6 4 7 5 8 6]]
   :named {:fill "#3498db"
           :opacity 0.3}}
  {:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2 4 3 5 6 4 7 5 8 6]]
   :named {:stroke "#2980b9"
           :stroke-width 2}}]
 {:width 600 :height 400})

;; ## Complex Styling Examples
;;
;; Real-world styling scenarios

;; ### Stock Chart Style

(def stock-data
  {:date [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15]
   :price [100 102 98 105 103 110 108 115 112 118 120 117 122 125 123]})

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
  {:category ["Product A" "Product B" "Product C" "Product D" "Product E"]
   :q1 [45 55 35 65 50]
   :q2 [50 60 40 70 55]
   :q3 [55 65 45 75 60]
   :q4 [60 70 50 80 65]})

;; Show Q4 results with bold styling
(tg/entry->svg
 {:plottype :bar
  :positional [(:category presentation-data) (:q4 presentation-data)]
  :named {:fill "#e74c3c"
          :stroke "#c0392b"
          :stroke-width 2
          :opacity 0.9}}
 {:width 800 :height 500})

;; ## Edge Cases for Styling
;;
;; Test styling with unusual or edge case scenarios

;; ### Transparent Plot

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#3498db"
          :opacity 0.1}}
 {:width 600 :height 400})

;; ### Maximum Opacity

(tg/entry->svg
 {:plottype :scatter
  :positional [(:x base-data) (:y base-data)]
  :named {:color "#e74c3c"
          :opacity 1.0
          :size 12}}
 {:width 600 :height 400})

;; ### Very Thick Line

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke "#9b59b6"
          :stroke-width 10}}
 {:width 600 :height 400})

;; ### Very Thin Line

(tg/entry->svg
 {:plottype :line
  :positional [(:x line-data) (:y line-data)]
  :named {:stroke "#1abc9c"
          :stroke-width 0.5}}
 {:width 600 :height 400})

;; ## Summary
;;
;; This notebook provides comprehensive testing of styling and customization options
;; for the thi.ng/geom backend. It covers:
;;
;; - Basic styling properties (color, opacity, size)
;; - Mark-specific styling (scatter, line, bar)
;; - Color palettes and gradients
;; - Plot size and aspect ratio variations
;; - Data-driven aesthetics
;; - Polar plot styling
;; - Multi-layer styling combinations
;; - Real-world styling scenarios
;; - Edge cases
;;
;; All examples demonstrate the flexibility and control available when customizing
;; visualizations with the thi.ng/geom backend.
