;; # thi.ng/geom - Polar Coordinates
;;
;; Comprehensive testing of polar coordinate visualizations.

(ns tableplot-book.thing-geom-polar
  (:require [scicloj.tableplot.v1.aog.thing-geom :as tg]))

;; ## Simple Polar Plots

;; ### Circle

(let [theta (vec (range 0 (* 2 Math/PI) 0.1))
      r (vec (repeat (count theta) 5))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#3498db"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### Spiral

(let [theta (vec (range 0 (* 4 Math/PI) 0.1))
      r (vec theta)]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#e74c3c"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Rose Curves

;; ### 3-petaled rose (r = cos(3θ))

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (Math/cos (* 3 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#9b59b6"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### 4-petaled rose (r = sin(2θ))

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 6 (Math/sin (* 2 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#2ecc71"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### 5-petaled rose (r = cos(5θ))

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 7 (Math/cos (* 5 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#e67e22"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### 8-petaled rose (r = sin(4θ))

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 8 (Math/sin (* 4 %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#16a085"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Lissajous-style Polar

;; ### r = 5 + 3sin(3θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.05))
      r (vec (map #(+ 5 (* 3 (Math/sin (* 3 %)))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#c0392b"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### r = 8 + 4cos(2θ)

(let [theta (vec (range 0 (* 2 Math/PI) 0.05))
      r (vec (map #(+ 8 (* 4 (Math/cos (* 2 %)))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#8e44ad"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Cardioid and Limacon

;; ### Cardioid (r = 1 + cos(θ))

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (+ 1 (Math/cos %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#d35400"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ### Limacon (r = 2 + 3cos(θ))

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(+ 2 (* 3 (Math/cos %))) theta))]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#27ae60"
            :stroke-width 2}}
   {:polar true :width 600 :height 600}))

;; ## Radar/Spider Charts

;; ### 5-category radar (pentagon)

(let [n 5
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n))))
      r [0.8 0.6 0.9 0.7 0.5 0.8]] ; Repeat first value to close
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#2980b9"
            :stroke-width 3
            :fill "#2980b9"
            :opacity 0.3}}
   {:polar true :width 600 :height 600}))

;; ### 6-category radar (hexagon)

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

;; ### 8-category radar (octagon)

(let [n 8
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n))))
      r [0.7 0.8 0.6 0.9 0.75 0.85 0.65 0.8 0.7]]
  (tg/entry->svg
   {:plottype :line
    :positional [theta r]
    :named {:stroke "#c0392b"
            :stroke-width 3
            :fill "#c0392b"
            :opacity 0.2}}
   {:polar true :width 600 :height 600}))

;; ## Summary
;;
;; Polar coordinate visualizations tested:
;; - ✅ Simple shapes (circle, spiral)
;; - ✅ Rose curves (3, 4, 5, 8 petals)
;; - ✅ Complex polar functions
;; - ✅ Cardioid and limacon
;; - ✅ Radar charts (5, 6, 8 categories)
