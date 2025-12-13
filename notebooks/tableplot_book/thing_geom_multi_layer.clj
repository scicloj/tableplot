;; # thi.ng/geom - Multi-layer Compositions
;;
;; Testing layered visualizations with multiple plot types combined.

(ns tableplot-book.thing-geom-multi-layer
  (:require [scicloj.tableplot.v1.aog.thing-geom :as tg]))

;; ## Scatter + Line

;; ### Data points with trend line

(tg/entries->svg
 [{:plottype :scatter
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2.1 3.9 6.2 7.8 10.1 12.3 13.9 16.2 17.8 20.1]]
   :named {:color "#3498db"
           :opacity 0.7}}
  {:plottype :line
   :positional [[1 2 3 4 5 6 7 8 9 10]
                [2 4 6 8 10 12 14 16 18 20]]
   :named {:stroke "#e74c3c"
           :stroke-width 2}}]
 {:width 600 :height 400})

;; ### Scatter with smoothed curve

(let [x [1 2 3 4 5 6 7 8 9 10]
      y-scatter [2 4 3 5 6 4 7 5 8 6]
      x-smooth (vec (range 1 10.1 0.5))
      y-smooth (vec (map #(+ 4 (* 0.4 %) (Math/sin %)) x-smooth))]
  (tg/entries->svg
   [{:plottype :scatter
     :positional [x y-scatter]
     :named {:color "#9b59b6"
             :opacity 0.8}}
    {:plottype :line
     :positional [x-smooth y-smooth]
     :named {:stroke "#2ecc71"
             :stroke-width 2}}]
   {:width 600 :height 400}))

;; ## Multiple Lines

;; ### Three sine waves (different frequencies)

(let [x (vec (range 0 10 0.1))
      y1 (vec (map #(Math/sin %) x))
      y2 (vec (map #(Math/sin (* 2 %)) x))
      y3 (vec (map #(Math/sin (* 3 %)) x))]
  (tg/entries->svg
   [{:plottype :line
     :positional [x y1]
     :named {:stroke "#e74c3c"
             :stroke-width 2}}
    {:plottype :line
     :positional [x y2]
     :named {:stroke "#3498db"
             :stroke-width 2}}
    {:plottype :line
     :positional [x y3]
     :named {:stroke "#2ecc71"
             :stroke-width 2}}]
   {:width 600 :height 400}))

;; ### Two trends (growing vs declining)

(let [x [1 2 3 4 5 6 7 8 9 10]]
  (tg/entries->svg
   [{:plottype :line
     :positional [x [10 12 15 18 22 26 30 35 40 45]]
     :named {:stroke "#16a085"
             :stroke-width 3}}
    {:plottype :line
     :positional [x [45 40 36 32 29 26 24 22 21 20]]
     :named {:stroke "#c0392b"
             :stroke-width 3}}]
   {:width 600 :height 400}))

;; ## Scatter + Multiple Lines

;; ### Data with upper and lower bounds

(let [x [1 2 3 4 5 6 7 8 9 10]
      y-data [5.2 5.8 6.1 6.7 7.3 7.9 8.2 8.8 9.1 9.7]
      y-upper (vec (map #(+ % 1) y-data))
      y-lower (vec (map #(- % 1) y-data))
      y-center (vec (map #(+ 5 (* 0.5 %)) x))]
  (tg/entries->svg
   [{:plottype :line
     :positional [x y-upper]
     :named {:stroke "#95a5a6"
             :stroke-width 1
             :opacity 0.5}}
    {:plottype :line
     :positional [x y-lower]
     :named {:stroke "#95a5a6"
             :stroke-width 1
             :opacity 0.5}}
    {:plottype :line
     :positional [x y-center]
     :named {:stroke "#2980b9"
             :stroke-width 2}}
    {:plottype :scatter
     :positional [x y-data]
     :named {:color "#e67e22"
             :opacity 0.8}}]
   {:width 600 :height 400}))

;; ## Bar + Line

;; ### Bars with average line

(let [x [1 2 3 4 5 6 7]
      bars [23 45 34 56 42 38 51]
      avg (/ (reduce + bars) (count bars))
      avg-line (vec (repeat (count x) avg))]
  (tg/entries->svg
   [{:plottype :bar
     :positional [x bars]
     :named {:fill "#3498db"
             :bar-width 30
             :opacity 0.7}}
    {:plottype :line
     :positional [x avg-line]
     :named {:stroke "#e74c3c"
             :stroke-width 3}}]
   {:width 600 :height 400}))

;; ## Complex Combinations

;; ### Four different series

(let [x (vec (range 0 10 0.5))
      y1 (vec (map #(Math/sin %) x))
      y2 (vec (map #(Math/cos %) x))
      scatter-x [1 3 5 7 9]
      scatter-y1 [0.8 0.1 -0.9 0.6 0.4]
      scatter-y2 [0.5 -1.0 0.3 0.7 -0.4]]
  (tg/entries->svg
   [{:plottype :line
     :positional [x y1]
     :named {:stroke "#e74c3c"
             :stroke-width 2}}
    {:plottype :line
     :positional [x y2]
     :named {:stroke "#3498db"
             :stroke-width 2}}
    {:plottype :scatter
     :positional [scatter-x scatter-y1]
     :named {:color "#2ecc71"
             :opacity 0.9}}
    {:plottype :scatter
     :positional [scatter-x scatter-y2]
     :named {:color "#9b59b6"
             :opacity 0.9}}]
   {:width 600 :height 400}))

;; ## Polar Multi-layer

;; ### Two rose curves

(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r1 (vec (map #(* 5 (Math/cos (* 3 %))) theta))
      r2 (vec (map #(* 4 (Math/sin (* 5 %))) theta))]
  (tg/entries->svg
   [{:plottype :line
     :positional [theta r1]
     :named {:stroke "#e74c3c"
             :stroke-width 2
             :opacity 0.7}}
    {:plottype :line
     :positional [theta r2]
     :named {:stroke "#3498db"
             :stroke-width 2
             :opacity 0.7}}]
   {:polar true :width 600 :height 600}))

;; ### Overlapping radar charts

(let [n 6
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n))))
      r1 [0.9 0.7 0.8 0.6 0.85 0.75 0.9]
      r2 [0.6 0.8 0.7 0.9 0.65 0.85 0.6]]
  (tg/entries->svg
   [{:plottype :line
     :positional [theta r1]
     :named {:stroke "#2ecc71"
             :stroke-width 2
             :fill "#2ecc71"
             :opacity 0.2}}
    {:plottype :line
     :positional [theta r2]
     :named {:stroke "#9b59b6"
             :stroke-width 2
             :fill "#9b59b6"
             :opacity 0.2}}]
   {:polar true :width 600 :height 600}))

;; ## Summary
;;
;; Multi-layer combinations tested:
;; - ✅ Scatter + Line (trend, smoothed)
;; - ✅ Multiple lines (3 series, trends)
;; - ✅ Scatter + Multiple lines (bounds)
;; - ✅ Bar + Line (average)
;; - ✅ Complex (4 series)
;; - ✅ Polar multi-layer (roses, radar)
