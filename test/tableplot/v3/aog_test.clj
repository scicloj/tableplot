(ns tableplot.v3.aog-test
  (:require [clojure.test :refer [deftest is testing]]
            [tableplot.v3.aog :as aog]))

;; =============================================================================
;; Test Data
;; =============================================================================

(def simple-data
  {:x [1 2 3 4 5]
   :y [2 4 6 8 10]})

(def penguins
  {:bill-length [39.1 39.5 40.3 36.7 39.3 38.9 39.2 41.1]
   :bill-depth [18.7 17.4 18.0 19.3 20.6 17.8 19.6 17.6]
   :species [:adelie :adelie :adelie :adelie :chinstrap :chinstrap :gentoo :gentoo]
   :body-mass [3750 3800 3250 3450 3650 3625 4675 3200]})

;; =============================================================================
;; Constructor Tests
;; =============================================================================

(deftest test-data-constructor
  (testing "data constructor creates map with :aog/data key"
    (is (= {:aog/data simple-data}
           (aog/data simple-data)))))

(deftest test-mapping-constructor
  (testing "mapping with x and y"
    (is (= {:aog/x :bill-length :aog/y :bill-depth}
           (aog/mapping :bill-length :bill-depth))))

  (testing "mapping with named aesthetics"
    (is (= {:aog/x :bill-length :aog/y :bill-depth :aog/color :species}
           (aog/mapping :bill-length :bill-depth {:color :species})))))

(deftest test-scatter-constructor
  (testing "scatter without attributes"
    (is (= {:aog/plottype :scatter}
           (aog/scatter))))

  (testing "scatter with attributes"
    (is (= {:aog/plottype :scatter :aog/alpha 0.5 :aog/size 100}
           (aog/scatter {:alpha 0.5 :size 100})))))

;; =============================================================================
;; Standard Merge Tests - THE KEY INNOVATION!
;; =============================================================================

(deftest test-standard-merge-works
  (testing "Standard merge combines layer components"
    (let [result (merge (aog/data simple-data)
                        (aog/mapping :x :y)
                        (aog/scatter {:alpha 0.7}))]
      (is (= #:aog{:data simple-data
                   :x :x
                   :y :y
                   :plottype :scatter
                   :alpha 0.7}
             result))))

  (testing "Standard merge with color aesthetic"
    (let [result (merge (aog/data penguins)
                        (aog/mapping :bill-length :bill-depth {:color :species})
                        (aog/scatter))]
      (is (= #:aog{:data penguins
                   :x :bill-length
                   :y :bill-depth
                   :color :species
                   :plottype :scatter}
             result)))))

(deftest test-standard-assoc-works
  (testing "Standard assoc adds aesthetics"
    (let [base #:aog{:data simple-data :x :x :y :y :plottype :scatter}
          with-color (assoc base :aog/color :species)]
      (is (= #:aog{:data simple-data
                   :x :x
                   :y :y
                   :plottype :scatter
                   :color :species}
             with-color)))))

(deftest test-standard-update-works
  (testing "Standard update modifies attributes"
    (let [base #:aog{:data simple-data :x :x :y :y :plottype :scatter :alpha 0.5}
          with-more-alpha (update base :aog/alpha * 2)]
      (is (= 1.0 (:aog/alpha with-more-alpha))))))

(deftest test-standard-mapv-works
  (testing "Standard mapv transforms all layers"
    (let [layers [#:aog{:plottype :scatter :alpha 0.5}
                  #:aog{:plottype :line :alpha 0.5}]
          with-more-alpha (mapv #(assoc % :aog/alpha 0.9) layers)]
      (is (every? #(= 0.9 (:aog/alpha %)) with-more-alpha)))))

;; =============================================================================
;; Operator Tests
;; =============================================================================

(deftest test-star-operator-basic
  (testing "* with single map wraps in vector"
    (is (= [#:aog{:data simple-data}]
           (aog/* (aog/data simple-data)))))

  (testing "* merges two maps"
    (is (= [#:aog{:data simple-data :x :x}]
           (aog/* (aog/data simple-data)
                  #:aog{:x :x})))))

(deftest test-star-operator-complete-workflow
  (testing "Complete layer specification"
    (let [result (aog/* (aog/data simple-data)
                        (aog/mapping :x :y)
                        (aog/scatter {:alpha 0.7}))]
      (is (= 1 (count result)))
      (is (= #:aog{:data simple-data
                   :x :x
                   :y :y
                   :plottype :scatter
                   :alpha 0.7}
             (first result))))))

(deftest test-plus-operator
  (testing "+ creates vector of layer specs"
    (let [result (aog/+ (aog/scatter {:alpha 0.5})
                        (aog/linear))]
      (is (= 2 (count result)))
      (is (= #:aog{:plottype :scatter :alpha 0.5} (first result)))
      (is (= #:aog{:transformation :linear :plottype :line} (second result))))))

(deftest test-star-distributes-over-plus
  (testing "* distributes base over + layers"
    (let [result (aog/* (aog/data simple-data)
                        (aog/mapping :x :y)
                        (aog/+ (aog/scatter {:alpha 0.5})
                               (aog/linear)))]
      (is (= 2 (count result)))

      ;; First layer: scatter
      (is (= #:aog{:data simple-data
                   :x :x
                   :y :y
                   :plottype :scatter
                   :alpha 0.5}
             (first result)))

      ;; Second layer: linear
      (is (= #:aog{:data simple-data
                   :x :x
                   :y :y
                   :transformation :linear
                   :plottype :line}
             (second result))))))

;; =============================================================================
;; Value Type Tests (mapping vs attribute)
;; =============================================================================

(deftest test-keyword-vs-constant-values
  (testing "Keyword values are mappings to columns"
    (let [layer #:aog{:color :species :size :body-mass}]
      (is (keyword? (:aog/color layer)))
      (is (keyword? (:aog/size layer)))))

  (testing "Number values are constant attributes"
    (let [layer #:aog{:alpha 0.5 :size 100}]
      (is (number? (:aog/alpha layer)))
      (is (number? (:aog/size layer)))))

  (testing "String values are constant attributes"
    (let [layer #:aog{:color "red" :stroke "blue"}]
      (is (string? (:aog/color layer)))
      (is (string? (:aog/stroke layer))))))

;; =============================================================================
;; Faceting Tests
;; =============================================================================

(deftest test-faceting-as-aesthetics
  (testing "Facet aesthetics work like any other aesthetic"
    (let [base (aog/* (aog/data penguins)
                      (aog/mapping :bill-length :bill-depth)
                      (aog/scatter))
          with-facet (aog/facet base #:aog{:col :species})]
      (is (= 1 (count with-facet)))
      (is (= :species (:aog/col (first with-facet)))))))

;; =============================================================================
;; Data-Oriented API Tests
;; =============================================================================

(deftest test-merge-layers-function
  (testing "merge-layers is equivalent to *"
    (let [result1 (aog/* (aog/data simple-data)
                         (aog/mapping :x :y)
                         (aog/scatter))
          result2 (aog/merge-layers (aog/data simple-data)
                                    (aog/mapping :x :y)
                                    (aog/scatter))]
      (is (= result1 result2)))))

(deftest test-concat-layers-function
  (testing "concat-layers combines layer vectors"
    (let [scatter-layers (aog/* (aog/data simple-data)
                                (aog/mapping :x :y)
                                (aog/scatter))
          line-layers (aog/* (aog/data simple-data)
                             (aog/mapping :x :y)
                             (aog/line))
          combined (aog/concat-layers scatter-layers line-layers)]
      (is (= 2 (count combined)))
      (is (= :scatter (:aog/plottype (first combined))))
      (is (= :line (:aog/plottype (second combined)))))))

(deftest test-layers-function
  (testing "layers creates vector from raw maps"
    (let [result (aog/layers #:aog{:data simple-data :x :x :y :y :plottype :scatter}
                             #:aog{:data simple-data :x :x :y :y :plottype :line})]
      (is (= 2 (count result)))
      (is (vector? result)))))

;; =============================================================================
;; Real-World Workflow Tests
;; =============================================================================

(deftest test-conditional-layer-building
  (testing "Building plots conditionally with standard into"
    (let [base (aog/* (aog/data penguins)
                      (aog/mapping :bill-length :bill-depth {:color :species})
                      (aog/scatter {:alpha 0.5}))

          show-regression? true

          with-regression (if show-regression?
                            (into base
                                  (aog/* (aog/data penguins)
                                         (aog/mapping :bill-length :bill-depth {:color :species})
                                         (aog/linear)))
                            base)]

      (is (= 2 (count with-regression)))
      (is (= :scatter (:aog/plottype (first with-regression))))
      (is (= :line (:aog/plottype (second with-regression)))))))

(deftest test-layer-transformation-with-map
  (testing "Transforming layers with standard map"
    (let [base (aog/* (aog/data penguins)
                      (aog/mapping :bill-length :bill-depth)
                      (aog/+ (aog/scatter)
                             (aog/line)))

          ;; Add alpha to all layers
          with-alpha (mapv #(assoc % :aog/alpha 0.7) base)]

      (is (= 2 (count with-alpha)))
      (is (every? #(= 0.7 (:aog/alpha %)) with-alpha)))))

(deftest test-layer-filtering
  (testing "Filtering layers with standard filter"
    (let [mixed (aog/* (aog/data simple-data)
                       (aog/mapping :x :y)
                       (aog/+ (aog/scatter)
                              (aog/line)
                              (aog/bar)))

          ;; Keep only scatter and line
          filtered (filterv #(#{:scatter :line} (:aog/plottype %)) mixed)]

      (is (= 2 (count filtered)))
      (is (every? #(#{:scatter :line} (:aog/plottype %)) filtered)))))

;; =============================================================================
;; Direct Map Manipulation Tests
;; =============================================================================

(deftest test-users-can-work-directly-with-maps
  (testing "Users can create layers without helper functions"
    (let [manual-layer #:aog{:data simple-data
                             :x :x
                             :y :y
                             :plottype :scatter
                             :alpha 0.7}

          using-helpers (first (aog/* (aog/data simple-data)
                                      (aog/mapping :x :y)
                                      (aog/scatter {:alpha 0.7})))]

      (is (= manual-layer using-helpers))))

  (testing "Users can merge layers manually"
    (let [layer1 #:aog{:data simple-data :x :x}
          layer2 #:aog{:y :y :color :species}
          layer3 #:aog{:plottype :scatter :alpha 0.5}

          merged (merge layer1 layer2 layer3)]

      (is (= #:aog{:data simple-data
                   :x :x
                   :y :y
                   :color :species
                   :plottype :scatter
                   :alpha 0.5}
             merged)))))

;; =============================================================================
;; Comparison with v2 Tests
;; =============================================================================

(deftest test-v3-simpler-than-v2
  (testing "v3 uses standard merge instead of custom logic"
    ;; In v2, you need custom merge-layer-pair
    ;; In v3, standard merge works!

    (let [part1 #:aog{:data simple-data}
          part2 #:aog{:x :x :y :y}
          part3 #:aog{:plottype :scatter :alpha 0.5}

          ;; This is all you need in v3:
          v3-result (merge part1 part2 part3)]

      (is (= #:aog{:data simple-data
                   :x :x
                   :y :y
                   :plottype :scatter
                   :alpha 0.5}
             v3-result)))))

;; =============================================================================
;; Edge Cases
;; =============================================================================

(deftest test-nil-handling
  (testing "nil values don't override non-nil"
    (let [layer1 #:aog{:data simple-data :x :x}
          layer2 #:aog{:y :y :x nil} ;; Trying to override :x with nil

          merged (merge layer1 layer2)]

      ;; Standard merge behavior: last value wins, even if nil
      (is (nil? (:aog/x merged)))))

  (testing "explicit nil in layer spec"
    (let [layer #:aog{:data simple-data :x :x :y :y :color nil :plottype :scatter}]
      (is (nil? (:aog/color layer))))))

(deftest test-empty-merge
  (testing "Empty merge returns nil"
    (is (nil? (merge))))

  (testing "Merge with single map returns that map"
    (is (= #:aog{:data simple-data} (merge #:aog{:data simple-data})))))
