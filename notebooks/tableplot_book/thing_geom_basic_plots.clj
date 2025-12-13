;; # thi.ng/geom - Basic Plot Types
;;
;; Comprehensive testing of basic plot types with the thing-geom backend.

(ns tableplot-book.thing-geom-basic-plots
  (:require [scicloj.tableplot.v1.aog.thing-geom :as tg]))

;; ## Scatter Plots

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

(let [n 100
      x (vec (range n))
      y (vec (map #(+ (* 0.5 %) (- (rand 10) 5)) x))]
  (tg/entry->svg
   {:plottype :scatter
    :positional [x y]
    :named {:color "#3498db"
            :opacity 0.6}}
   {:width 600 :height 400}))

;; ## Line Plots

;; ### Simple line

(tg/entry->svg
 {:plottype :line
  :positional [[1 2 3 4 5]
               [1 4 2 5 3]]
  :named {:stroke "#2ecc71"
          :stroke-width 3}}
 {:width 600 :height 400})

;; ### Sine wave

(let [x (vec (range 0 10 0.1))
      y (vec (map #(Math/sin %) x))]
  (tg/entry->svg
   {:plottype :line
    :positional [x y]
    :named {:stroke "#9b59b6"
            :stroke-width 2}}
   {:width 600 :height 400}))

;; ### Multiple oscillations

(let [x (vec (range 0 (* 2 Math/PI) 0.05))
      y (vec (map #(* (Math/sin (* 3 %)) (Math/cos %)) x))]
  (tg/entry->svg
   {:plottype :line
    :positional [x y]
    :named {:stroke "#e67e22"
            :stroke-width 2}}
   {:width 600 :height 400}))

;; ## Bar Charts

;; ### Simple bar chart

(tg/entry->svg
 {:plottype :bar
  :positional [[1 2 3 4 5]
               [10 25 15 30 20]]
  :named {:fill "#16a085"
          :bar-width 40}}
 {:width 600 :height 400})

;; ### Comparison bar chart

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

;; ## Edge Cases

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

;; ## Summary
;;
;; All basic plot types tested:
;; - ✅ Scatter plots (simple, colored, large)
;; - ✅ Line plots (simple, sine wave, complex)
;; - ✅ Bar charts (simple, comparison, wide)
;; - ✅ Edge cases (single point, two points)
;; - ✅ Negative values (scatter, line, bar)
