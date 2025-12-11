;; # AlgebraOfGraphics Tutorial
;; A comprehensive tutorial following the AlgebraOfGraphics.jl approach
;;
;; This notebook closely follows the structure and examples from AoG.jl's tutorial series,
;; demonstrating how the same patterns work in Clojure.

(ns tableplot-book.aog-tutorial
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [tablecloth.api :as tc]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Part 1: Fundamentals
;;
;; Following intro-i.md from AlgebraOfGraphics.jl

;; ### Basic Scatter Plot
;;
;; The simplest visualization - just x and y:

(def simple-df
  {:x (vec (range 100))
   :y (vec (repeatedly 100 #(rand)))})

(aog/draw
 (aog/* (aog/data simple-df)
        (aog/mapping :x :y)
        (aog/scatter)))

;; ### Visual Attributes
;;
;; We can customize the appearance with visual attributes:

(aog/draw
 (aog/* (aog/data simple-df)
        (aog/mapping :x :y)
        (aog/scatter {:markersize 15 :alpha 0.3})))

;; Different marker shapes:

(aog/draw
 (aog/* (aog/data simple-df)
        (aog/mapping :x :y)
        (aog/scatter {:markersize 10 :alpha 0.5})))

;; ### Continuous Color Mapping
;;
;; Map a continuous variable to color:

(def color-continuous-df
  {:x (vec (range 100))
   :y (vec (repeatedly 100 #(rand)))
   :z (vec (repeatedly 100 #(* 100 (rand))))})

(aog/draw
 (aog/* (aog/data color-continuous-df)
        (aog/mapping :x :y {:color :z})
        (aog/scatter)))

;; ### Categorical Color Mapping
;;
;; Map a categorical variable to color:

(def color-categorical-df
  {:x (vec (range 60))
   :y (vec (repeatedly 60 #(rand)))
   :species (vec (take 60 (cycle ["A" "B" "C"])))})

(aog/draw
 (aog/* (aog/data color-categorical-df)
        (aog/mapping :x :y {:color :species})
        (aog/scatter)))

;; ### Density Plots
;;
;; The density transformation creates a smooth density curve:

(def dense-data
  {:values (vec (repeatedly 200 #(+ 5 (* 2 (- (rand) 0.5)))))})

(aog/draw
 (aog/* (aog/data dense-data)
        (aog/mapping :values)
        (aog/density)))

;; ## Part 2: Lines and Markers
;;
;; Following lines-and-markers.md from AoG.jl examples

;; ### Simple Scatter

(def random-scatter
  {:x (vec (repeatedly 100 #(rand)))
   :y (vec (repeatedly 100 #(rand)))})

(aog/draw
 (aog/* (aog/data random-scatter)
        (aog/mapping :x :y)
        (aog/scatter)))

;; ### Simple Line Plot

(def sine-wave
  (let [x (vec (map #(* % 0.1) (range -31 32)))]
    {:x x
     :y (vec (map #(Math/sin %) x))}))

(aog/draw
 (aog/* (aog/data sine-wave)
        (aog/mapping :x :y)
        (aog/line)))

;; ### Lines and Scatter Combined

(aog/draw
 (aog/* (aog/data sine-wave)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter)
               (aog/line))))

;; ### Different Data for Different Layers

(def smooth-line
  (let [x (vec (map #(* % 0.1) (range -31 32)))]
    {:x x
     :y (vec (map #(Math/sin %) x))}))

(def noisy-points
  {:x (vec (repeatedly 20 #(* (- (rand) 0.5) 6)))
   :y (vec (repeatedly 20 #(* (- (rand) 0.5) 2)))})

(aog/draw
 (aog/+ (aog/* (aog/data smooth-line)
               (aog/mapping :x :y)
               (aog/line {:width 3}))
        (aog/* (aog/data noisy-points)
               (aog/mapping :x :y)
               (aog/scatter {:markersize 10 :alpha 0.6}))))

;; ## Part 3: Histograms
;;
;; Following histograms.md from AoG.jl examples

;; ### Basic Histogram

(def uniform-data
  {:x (vec (repeatedly 1000 #(rand-int 100)))})

(aog/draw
 (aog/* (aog/data uniform-data)
        (aog/mapping :x)
        (aog/histogram {:bins 20})))

;; ### Grouped Histogram (Overlaid)

(def grouped-normal
  (let [n 1000]
    {:x (vec (concat (repeatedly (/ n 2) #(+ 50 (* 15 (- (rand) 0.5))))
                     (repeatedly (/ n 2) #(+ 60 (* 20 (- (rand) 0.5))))))
     :category (vec (concat (repeat (/ n 2) "a")
                            (repeat (/ n 2) "b")))}))

(aog/draw
 (aog/* (aog/data grouped-normal)
        (aog/mapping :x {:color :category})
        (aog/histogram {:bins 20})))

;; ### Multiple Aesthetics
;;
;; Combine color and size mappings:

(def multi-aesthetic-data
  {:x (vec (range 50))
   :y (vec (repeatedly 50 #(* 10 (rand))))
   :category (vec (take 50 (cycle ["A" "B" "C"])))
   :magnitude (vec (repeatedly 50 #(+ 5 (* 15 (rand)))))})

(aog/draw
 (aog/* (aog/data multi-aesthetic-data)
        (aog/mapping :x :y {:color :category :size :magnitude})
        (aog/scatter {:alpha 0.6})))

;; ### Time Series Example

(def time-series
  (let [t (vec (range 100))
        baseline (vec (map #(+ 50 (* 10 (Math/sin (/ % 5)))) t))
        noise (vec (repeatedly 100 #(* 5 (- (rand) 0.5))))]
    {:time t
     :value (vec (map + baseline noise))
     :series (vec (take 100 (cycle ["A"])))}))

(aog/draw
 (aog/* (aog/data time-series)
        (aog/mapping :time :value {:color :series})
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/line {:width 2}))))

;; ### Multiple Time Series

(def multi-time-series
  (let [t (vec (range 100))
        series-a (vec (map #(+ 50 (* 10 (Math/sin (/ % 5))) (rand-int 5)) t))
        series-b (vec (map #(+ 60 (* 8 (Math/cos (/ % 4))) (rand-int 5)) t))]
    {:time (vec (concat t t))
     :value (vec (concat series-a series-b))
     :series (vec (concat (repeat 100 "A") (repeat 100 "B")))}))

(aog/draw
 (aog/* (aog/data multi-time-series)
        (aog/mapping :time :value {:color :series})
        (aog/+ (aog/scatter {:alpha 0.4})
               (aog/line {:width 2}))))

;; ## Part 4: Statistical Transformations

;; ### Linear Regression

(def regression-data
  (let [x (vec (range 50))
        y (vec (map #(+ (* 2 %) 10 (* 10 (- (rand) 0.5))) x))]
    {:x x
     :y y}))

(aog/draw
 (aog/* (aog/data regression-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))

;; ### Grouped Linear Regression

(def grouped-regression
  (let [n 30
        x-a (vec (range n))
        y-a (vec (map #(+ (* 2 %) 10 (* 8 (- (rand) 0.5))) x-a))
        x-b (vec (range n))
        y-b (vec (map #(+ (* -1.5 %) 80 (* 8 (- (rand) 0.5))) x-b))]
    {:x (vec (concat x-a x-b))
     :y (vec (concat y-a y-b))
     :group (vec (concat (repeat n "A") (repeat n "B")))}))

(aog/draw
 (aog/* (aog/data grouped-regression)
        (aog/mapping :x :y {:color :group})
        (aog/+ (aog/scatter {:alpha 0.6})
               (aog/linear))))

;; ### Smooth Transformation

(aog/draw
 (aog/* (aog/data grouped-regression)
        (aog/mapping :x :y {:color :group})
        (aog/+ (aog/scatter {:alpha 0.6})
               (aog/smooth))))

;; ### Density Estimation

(def normal-sample
  {:values (vec (repeatedly 500 #(+ 50 (* 15 (- (rand) 0.5)))))})

(aog/draw
 (aog/* (aog/data normal-sample)
        (aog/mapping :values)
        (aog/density)))

;; ### Grouped Density

(def grouped-density-data
  (let [n 400]
    {:values (vec (concat (repeatedly (/ n 2) #(+ 40 (* 12 (- (rand) 0.5))))
                          (repeatedly (/ n 2) #(+ 60 (* 15 (- (rand) 0.5))))))
     :group (vec (concat (repeat (/ n 2) "Low")
                         (repeat (/ n 2) "High")))}))

(aog/draw
 (aog/* (aog/data grouped-density-data)
        (aog/mapping :values {:color :group})
        (aog/density)))

;; ## Part 5: Advanced Layering

;; ### Three-Layer Composition

(aog/draw
 (aog/* (aog/data grouped-regression)
        (aog/mapping :x :y {:color :group})
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear)
               (aog/smooth))))

;; ### Shared Base Specification

(def base-spec
  (aog/* (aog/data multi-time-series)
         (aog/mapping :time :value {:color :series})))

;; Just scatter:
(aog/draw
 (aog/* base-spec
        (aog/scatter {:alpha 0.6})))

;; Just lines:
(aog/draw
 (aog/* base-spec
        (aog/line {:width 2})))

;; Both:
(aog/draw
 (aog/* base-spec
        (aog/+ (aog/scatter {:alpha 0.4})
               (aog/line {:width 2}))))

;; ## Part 6: Real-World Example
;;
;; A comprehensive visualization with multiple features:

(def comprehensive-data
  (let [n 60]
    {:x (vec (concat (map #(+ 10 (* 0.5 %) (* 3 (- (rand) 0.5))) (range n))
                     (map #(+ 20 (* 0.3 %) (* 3 (- (rand) 0.5))) (range n))
                     (map #(+ 5 (* 0.7 %) (* 3 (- (rand) 0.5))) (range n))))
     :y (vec (concat (map #(+ 50 (* 1.2 %) (* 5 (- (rand) 0.5))) (range n))
                     (map #(+ 40 (* 0.8 %) (* 5 (- (rand) 0.5))) (range n))
                     (map #(+ 30 (* 1.5 %) (* 5 (- (rand) 0.5))) (range n))))
     :species (vec (concat (repeat n "Adelie")
                           (repeat n "Chinstrap")
                           (repeat n "Gentoo")))}))

(aog/draw
 (aog/* (aog/data comprehensive-data)
        (aog/mapping :x :y {:color :species})
        (aog/+ (aog/scatter {:alpha 0.6})
               (aog/linear)))
 {:layout {:title {:text "Comprehensive Example: Scatter + Linear Regression by Group"
                   :font {:size 16}}
           :xaxis {:title "X Variable"}
           :yaxis {:title "Y Variable"}
           :showlegend true}})

;; ## Summary
;;
;; This tutorial demonstrates:
;;
;; 1. **Basic plots**: scatter, line, histogram, density
;; 2. **Visual attributes**: markersize, alpha, width
;; 3. **Aesthetic mappings**: x, y, color, size
;; 4. **Layer composition**: `*` for merging, `+` for stacking
;; 5. **Statistical transformations**: linear, smooth, density, histogram
;; 6. **Grouped transformations**: automatic grouping by categorical aesthetics
;; 7. **Multiple data sources**: different data for different layers
;; 8. **Complex compositions**: building specs incrementally

(kind/md "**Tutorial complete!** ✅")
