;; # V3 Stress Test: Finding the Limits
;;
;; This notebook rigorously tests the v3 flat layer design with:
;; - Edge cases and corner cases
;; - Conflicting specifications
;; - Complex transformations
;; - Multiple datasets
;; - SVG verification with thing-geom

(ns v3-stress-test
  (:require [tableplot.v3.aog :as aog]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Test Data

(def simple-data
  {:x [1 2 3 4 5]
   :y [2 4 6 8 10]})

(def penguins
  {:bill-length [39.1 39.5 40.3 36.7 39.3 38.9 39.2 41.1]
   :bill-depth [18.7 17.4 18.0 19.3 20.6 17.8 19.6 17.6]
   :species [:adelie :adelie :adelie :adelie :chinstrap :chinstrap :gentoo :gentoo]
   :body-mass [3750 3800 3250 3450 3650 3625 4675 3200]})

(def sparse-data
  {:x [1 nil 3 nil 5]
   :y [2 4 nil 8 nil]
   :category [:a :b :c :a :b]})

;; ## Test 1: Conflicting Aesthetics
;;
;; What happens when we have both a mapping AND a constant for the same aesthetic?

;; Scenario 1a: Mapping first, then constant
(def conflict-1a
  (merge {:data penguins :x :bill-length :y :bill-depth}
         {:color :species} ;; Mapping to column
         {:color "red"})) ;; Constant - should override!

conflict-1a

;; Last value wins (standard merge behavior)
;; This is actually GOOD - predictable!
(aog/draw [conflict-1a] {:backend :thing-geom})

;; Scenario 1b: Constant first, then mapping
(def conflict-1b
  (merge {:data penguins :x :bill-length :y :bill-depth}
         {:color "blue"} ;; Constant
         {:color :species})) ;; Mapping - should override!

conflict-1b
(aog/draw [conflict-1b] {:backend :thing-geom})

;; ## Test 2: Multiple Data Sources in One Plot
;;
;; Can we overlay layers with DIFFERENT datasets?

(def dataset1 {:x [1 2 3] :y [1 2 3]})
(def dataset2 {:x [1 2 3] :y [3 2 1]})

(def multi-data-plot
  [(merge {:data dataset1 :x :x :y :y :plottype :scatter :color "blue"})
   (merge {:data dataset2 :x :x :y :y :plottype :line :color "red"})])

multi-data-plot

(aog/draw multi-data-plot {:backend :thing-geom})

;; Using operators - does it work?
(def multi-data-with-ops
  (into (aog/* (aog/data dataset1)
               (aog/mapping :x :y)
               (aog/scatter {:color "blue"}))
        (aog/* (aog/data dataset2)
               (aog/mapping :x :y)
               (aog/line {:color "red"}))))

multi-data-with-ops
(aog/draw multi-data-with-ops {:backend :thing-geom})

;; ## Test 3: Missing Required Fields
;;
;; What if we forget essential fields?

;; Missing :plottype
(def missing-plottype
  {:data simple-data
   :x :x
   :y :y
   ;; No :plottype!
   :alpha 0.5})

(try
  (aog/draw [missing-plottype] {:backend :thing-geom})
  (catch Exception e
    (println "Error with missing plottype:")
    (println (.getMessage e))))

;; Missing :data
(def missing-data
  {:x :x
   :y :y
   :plottype :scatter})

(try
  (aog/draw [missing-data] {:backend :thing-geom})
  (catch Exception e
    (println "Error with missing data:")
    (println (.getMessage e))))

;; Missing x or y
(def missing-x
  {:data simple-data
   ;; :x missing!
   :y :y
   :plottype :scatter})

(try
  (aog/draw [missing-x] {:backend :thing-geom})
  (catch Exception e
    (println "Error with missing x:")
    (println (.getMessage e))))

;; ## Test 4: Nil Values in Data
;;
;; How does v3 handle nil/missing values?

(aog/draw
 [(merge {:data sparse-data}
         {:x :x :y :y :color :category}
         {:plottype :scatter :alpha 0.7})]
 {:backend :thing-geom})

;; ## Test 5: Empty Data
;;
;; What about completely empty datasets?

(def empty-data {:x [] :y []})

(def plot-empty
  (merge {:data empty-data}
         {:x :x :y :y}
         {:plottype :scatter}))

(try
  (aog/draw [plot-empty] {:backend :thing-geom})
  (catch Exception e
    (println "Error with empty data:")
    (println (.getMessage e))))

;; ## Test 6: Overriding via Standard Merge
;;
;; Can we progressively override specifications?

(def base {:data penguins :x :bill-length :y :bill-depth :plottype :scatter})
(def with-alpha (merge base {:alpha 0.3}))
(def with-more-alpha (merge with-alpha {:alpha 0.8})) ;; Override
(def with-color (merge with-more-alpha {:color :species}))

with-color
(aog/draw [with-color] {:backend :thing-geom})

;; ## Test 7: Aesthetic Name Collisions
;;
;; What if aesthetic names collide with data column names?

(def tricky-data
  {:x [1 2 3]
   :y [4 5 6]
   :color [:red :blue :green] ;; Column named "color"!
   :alpha [0.3 0.5 0.7]}) ;; Column named "alpha"!

;; Map to the :color column (should work)
(def map-to-color-column
  (merge {:data tricky-data}
         {:x :x :y :y}
         {:color :color} ;; Map color aesthetic to :color column
         {:plottype :scatter}))

map-to-color-column
(aog/draw [map-to-color-column] {:backend :thing-geom})

;; Use constant color instead
(def constant-color-override
  (merge map-to-color-column
         {:color "purple"})) ;; Override with constant

constant-color-override
(aog/draw [constant-color-override] {:backend :thing-geom})

;; ## Test 8: Deeply Nested merge
;;
;; What happens with many merge operations?

(def deeply-merged
  (merge {:data penguins}
         {:x :bill-length}
         {:y :bill-depth}
         {:color :species}
         {:size :body-mass}
         {:alpha 0.5}
         {:plottype :scatter}))

deeply-merged
(aog/draw [deeply-merged] {:backend :thing-geom})

;; ## Test 9: Transformations
;;
;; Test statistical transformations

;; Linear regression
(def with-linear
  (aog/* (aog/data simple-data)
         (aog/mapping :x :y)
         (aog/+ (aog/scatter {:alpha 0.5})
                (aog/linear))))

with-linear
(aog/draw with-linear {:backend :thing-geom})

;; Smooth
(def with-smooth
  (aog/* (aog/data simple-data)
         (aog/mapping :x :y)
         (aog/+ (aog/scatter {:alpha 0.5})
                (aog/smooth))))

with-smooth
(aog/draw with-smooth {:backend :thing-geom})

;; ## Test 10: Faceting Edge Cases
;;
;; Test faceting with various configurations

;; Single facet dimension
(def single-facet
  (merge {:data penguins}
         {:x :bill-length :y :bill-depth}
         {:col :species}
         {:plottype :scatter}))

(aog/draw [single-facet] {:backend :thing-geom})

;; Two facet dimensions (grid)
(def grid-facet
  (merge {:data penguins}
         {:x :bill-length :y :bill-depth}
         {:row :species :col :species} ;; Same variable for row and col
         {:plottype :scatter}))

(aog/draw [grid-facet] {:backend :thing-geom})

;; ## Test 11: Programmatic Layer Manipulation
;;
;; Build layers programmatically and manipulate them

;; Generate 5 layers with varying alpha
(def generated-layers
  (mapv (fn [i]
          (merge {:data simple-data}
                 {:x :x :y :y}
                 {:plottype :scatter}
                 {:alpha (/ i 10.0)}))
        (range 1 6)))

generated-layers

;; Filter to keep only some
(def filtered-layers
  (filterv #(> (:alpha %) 0.3) generated-layers))

filtered-layers

;; Modify all with standard update
(def modified-layers
  (mapv #(update % :alpha * 0.5) filtered-layers))

modified-layers
(aog/draw (take 1 modified-layers) {:backend :thing-geom})

;; ## Test 12: The "Kitchen Sink" Layer
;;
;; A layer with EVERYTHING

(def kitchen-sink
  {:data penguins
   :x :bill-length
   :y :bill-depth
   :color :species
   :size :body-mass
   :shape :species
   :alpha 0.6
   :stroke "black"
   :plottype :scatter
   :transformation nil})

kitchen-sink
(aog/draw [kitchen-sink] {:backend :thing-geom})

;; ## Test 13: SVG Validation
;;
;; Actually inspect the SVG output to verify correctness

(defn analyze-svg [svg-str]
  {:length (count svg-str)
   :has-svg-tag (clojure.string/includes? svg-str "<svg")
   :has-circles (clojure.string/includes? svg-str "<circle")
   :has-paths (clojure.string/includes? svg-str "<path")
   :has-lines (clojure.string/includes? svg-str "<line")
   :circle-count (count (re-seq #"<circle" svg-str))
   :path-count (count (re-seq #"<path" svg-str))})

;; Test simple scatter
(def svg1 (aog/draw
           (aog/* (aog/data simple-data)
                  (aog/mapping :x :y)
                  (aog/scatter))
           {:backend :thing-geom}))

(analyze-svg (str svg1))

;; Test multi-layer
(def svg2 (aog/draw
           (aog/* (aog/data simple-data)
                  (aog/mapping :x :y)
                  (aog/+ (aog/scatter)
                         (aog/line)))
           {:backend :thing-geom}))

(analyze-svg (str svg2))

;; ## Test 14: Merge Associativity
;;
;; Does merge order matter? (It shouldn't for commutative properties)

(def order1
  (merge {:data penguins}
         {:x :bill-length :y :bill-depth}
         {:plottype :scatter :alpha 0.5}))

(def order2
  (merge {:plottype :scatter :alpha 0.5}
         {:data penguins}
         {:x :bill-length :y :bill-depth}))

(= order1 order2) ;; Should be true

;; But for overrides, order DOES matter (rightmost wins)
(def override-test1
  (merge {:alpha 0.3}
         {:alpha 0.8})) ;; => {:alpha 0.8}

(def override-test2
  (merge {:alpha 0.8}
         {:alpha 0.3})) ;; => {:alpha 0.3}

(= override-test1 override-test2) ;; Should be false!

;; ## Test 15: Interaction with Threading Macros
;;
;; v3 should work naturally with -> and ->>

(def threaded-plot
  (-> {}
      (merge {:data simple-data})
      (assoc :x :x)
      (assoc :y :y)
      (merge {:plottype :scatter})
      (assoc :alpha 0.7)))

threaded-plot
(aog/draw [threaded-plot] {:backend :thing-geom})

;; With ->> for layer collections
(def threaded-layers
  (->> (range 3)
       (mapv (fn [i]
               {:data simple-data
                :x :x :y :y
                :plottype :scatter
                :alpha (/ (inc i) 5.0)}))
       (filterv #(> (:alpha %) 0.3))))

threaded-layers

;; ## Test 16: Extreme Values
;;
;; What about extreme alpha, size, etc.?

(def extreme-attrs
  (merge {:data simple-data}
         {:x :x :y :y}
         {:plottype :scatter}
         {:alpha 1.0 ;; Maximum
          :size 1000})) ;; Very large

(aog/draw [extreme-attrs] {:backend :thing-geom})

(def extreme-attrs2
  (merge {:data simple-data}
         {:x :x :y :y}
         {:plottype :scatter}
         {:alpha 0.01 ;; Nearly transparent
          :size 1})) ;; Tiny

(aog/draw [extreme-attrs2] {:backend :thing-geom})

;; ## Test 17: Nonsensical Combinations
;;
;; What if we specify incompatible things?

;; Transformation on a scatter (transformations typically for lines)
(def weird-transform
  (merge {:data simple-data}
         {:x :x :y :y}
         {:transformation :linear} ;; Transform
         {:plottype :scatter})) ;; But scatter plot?

weird-transform
(try
  (aog/draw [weird-transform] {:backend :thing-geom})
  (catch Exception e
    (println "Error with weird transform:")
    (println (.getMessage e))))

;; ## Summary of Findings
;;
;; Document what we learned about v3's behavior
