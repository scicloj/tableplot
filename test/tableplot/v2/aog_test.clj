(ns tableplot.v2.aog-test
  "Tests for normalized AoG API (v2).
  
  These tests demonstrate:
  1. All constructors return vectors
  2. Simplified operators with no type checking
  3. Both algebraic and data-oriented APIs"
  (:require [clojure.test :refer [deftest is testing]]
            [tableplot.v2.aog :as aog]
            [scicloj.tableplot.v1.aog.ir :as ir]))

;;; =============================================================================
;;; Constructor Tests

(deftest test-constructors-return-vectors
  (testing "All constructors return vectors"
    (is (vector? (aog/data {:x [1 2 3]})))
    (is (vector? (aog/mapping :x :y)))
    (is (vector? (aog/mapping {:color :species})))
    (is (vector? (aog/visual :scatter {})))
    (is (vector? (aog/scatter)))
    (is (vector? (aog/line)))
    (is (vector? (aog/linear)))
    (is (vector? (aog/histogram))))

  (testing "All constructors return single-element vectors"
    (is (= 1 (count (aog/data {:x [1 2 3]}))))
    (is (= 1 (count (aog/mapping :x :y))))
    (is (= 1 (count (aog/scatter))))))

(deftest test-data-constructor
  (testing "data creates layer with data"
    (let [df {:x [1 2 3] :y [4 5 6]}
          [layer] (aog/data df)]
      (is (= df (:data layer)))
      (is (= [] (:positional layer)))
      (is (= {} (:named layer))))))

(deftest test-mapping-constructor
  (testing "mapping with positional args"
    (let [[layer] (aog/mapping :x :y)]
      (is (= [:x :y] (:positional layer)))
      (is (= {} (:named layer)))))

  (testing "mapping with named args"
    (let [[layer] (aog/mapping {:color :species :size :body-mass})]
      (is (= [] (:positional layer)))
      (is (= {:color :species :size :body-mass} (:named layer)))))

  (testing "mapping with mixed args"
    (let [[layer] (aog/mapping :x :y {:color :species})]
      (is (= [:x :y] (:positional layer)))
      (is (= {:color :species} (:named layer))))))

(deftest test-visual-constructor
  (testing "visual with plottype and attrs"
    (let [[layer] (aog/visual :scatter {:alpha 0.5})]
      (is (= :scatter (:plottype layer)))
      (is (= {:alpha 0.5} (:attributes layer)))))

  (testing "visual with attrs only"
    (let [[layer] (aog/visual {:alpha 0.5})]
      (is (= {:alpha 0.5} (:attributes layer))))))

;;; =============================================================================
;;; Operator Tests - The Key Simplification!

(deftest test-multiply-operator-simplified
  (testing "* with two vectors"
    (let [result (aog/* [(aog/data {:x [1]})]
                        [(aog/mapping :x)])]
      (is (vector? result))
      (is (= 1 (count result)))
      (let [[merged] result]
        (is (= {:x [1]} (:data merged)))
        (is (= [:x] (:positional merged))))))

  (testing "* with three vectors - full composition"
    (let [result (aog/* [(aog/data {:x [1 2 3] :y [4 5 6]})]
                        [(aog/mapping :x :y)]
                        [(aog/scatter {:alpha 0.5})])]
      (is (vector? result))
      (is (= 1 (count result)))
      (let [[merged] result]
        (is (= {:x [1 2 3] :y [4 5 6]} (:data merged)))
        (is (= [:x :y] (:positional merged)))
        (is (= :scatter (:plottype merged)))
        (is (= {:alpha 0.5} (:attributes merged))))))

  (testing "* distributes over + (Cartesian product)"
    (let [result (aog/* [(aog/data {:x [1 2 3] :y [4 5 6]})]
                        [(aog/mapping :x :y)]
                        (aog/+ [(aog/scatter)]
                               [(aog/line)]))]
      (is (vector? result))
      (is (= 2 (count result)) "Should have 2 layers (scatter and line)")
      (let [[scatter-layer line-layer] result]
        (is (= :scatter (:plottype scatter-layer)))
        (is (= :line (:plottype line-layer)))
        ;; Both should have the same data and mapping
        (is (= {:x [1 2 3] :y [4 5 6]} (:data scatter-layer)))
        (is (= {:x [1 2 3] :y [4 5 6]} (:data line-layer)))
        (is (= [:x :y] (:positional scatter-layer)))
        (is (= [:x :y] (:positional line-layer)))))))

(deftest test-add-operator-simplified
  (testing "+ concatenates layer vectors"
    (let [result (aog/+ [(aog/scatter)]
                        [(aog/line)])]
      (is (vector? result))
      (is (= 2 (count result)))
      (let [[scatter-layer line-layer] result]
        (is (= :scatter (:plottype scatter-layer)))
        (is (= :line (:plottype line-layer))))))

  (testing "+ with multiple vectors"
    (let [result (aog/+ [(aog/scatter)]
                        [(aog/line)]
                        [(aog/bar)])]
      (is (vector? result))
      (is (= 3 (count result))))))

(deftest test-no-type-checking-needed
  (testing "No layers? or layer? predicates needed"
    ;; Everything is a vector, so we can just work with vectors
    (let [layers (aog/* [(aog/data {:x [1]})]
                        [(aog/mapping :x)])]
      (is (vector? layers))
      ;; Can directly use vector functions
      (is (= 1 (count layers)))
      (is (map? (first layers))))))

;;; =============================================================================
;;; Data-Oriented API Tests

(deftest test-merge-layers
  (testing "merge-layers is equivalent to *"
    (let [using-star (aog/* [(aog/data {:x [1 2 3]})]
                            [(aog/mapping :x :y)]
                            [(aog/scatter)])
          using-merge (aog/merge-layers [(aog/data {:x [1 2 3]})]
                                        [(aog/mapping :x :y)]
                                        [(aog/scatter)])]
      (is (= using-star using-merge)))))

(deftest test-concat-layers
  (testing "concat-layers is equivalent to +"
    (let [using-plus (aog/+ [(aog/scatter)]
                            [(aog/line)])
          using-concat (aog/concat-layers [(aog/scatter)]
                                          [(aog/line)])]
      (is (= using-plus using-concat)))))

(deftest test-layers-constructor
  (testing "layers creates vector from raw maps"
    (let [result (aog/layers {:data {:x [1]}
                              :positional [:x]
                              :plottype :scatter
                              :transformation nil
                              :named {}
                              :attributes {}}
                             {:data {:x [2]}
                              :positional [:x]
                              :plottype :line
                              :transformation nil
                              :named {}
                              :attributes {}})]
      (is (vector? result))
      (is (= 2 (count result)))
      (is (= :scatter (:plottype (first result))))
      (is (= :line (:plottype (second result)))))))

;;; =============================================================================
;;; Faceting Tests

(deftest test-facet-simplified
  (testing "facet works on vector without conditionals"
    (let [layers [(aog/* [(aog/data {:x [1 2] :species [:a :b]})]
                         [(aog/mapping :x)]
                         [(aog/scatter)])]
          faceted (aog/facet layers {:col :species})]
      (is (vector? faceted))
      (is (= 1 (count faceted)))
      (is (= :species (get-in faceted [0 :named :col]))))))

;;; =============================================================================
;;; Integration Tests

(deftest test-complete-workflow-algebraic
  (testing "Complete workflow using algebraic operators"
    (let [df {:x [1 2 3 4 5]
              :y [2 4 6 8 10]
              :species [:a :a :b :b :c]}

          ;; Build plot using algebraic style
          layers (aog/* [(aog/data df)]
                        [(aog/mapping :x :y {:color :species})]
                        (aog/+ [(aog/scatter {:alpha 0.7})]
                               [(aog/linear)]))]

      ;; Verify structure
      (is (vector? layers))
      (is (= 2 (count layers)) "Should have 2 layers: scatter and linear")

      ;; Check first layer (scatter)
      (let [[scatter-layer linear-layer] layers]
        (is (= :scatter (:plottype scatter-layer)))
        (is (= df (:data scatter-layer)))
        (is (= [:x :y] (:positional scatter-layer)))
        (is (= :species (get-in scatter-layer [:named :color])))
        (is (= {:alpha 0.7} (:attributes scatter-layer)))

        ;; Check second layer (linear)
        (is (= :linear (:transformation linear-layer)))
        (is (= :line (:plottype linear-layer)))))))

(deftest test-complete-workflow-data-oriented
  (testing "Complete workflow using data-oriented API"
    (let [df {:x [1 2 3 4 5]
              :y [2 4 6 8 10]
              :species [:a :a :b :b :c]}

          ;; Build plot using data-oriented style
          base (aog/merge-layers [(aog/data df)]
                                 [(aog/mapping :x :y {:color :species})])

          overlays (aog/concat-layers [(aog/scatter {:alpha 0.7})]
                                      [(aog/linear)])

          layers (aog/merge-layers base overlays)]

      ;; Should have same structure as algebraic version
      (is (vector? layers))
      (is (= 2 (count layers))))))

(deftest test-raw-data-construction
  (testing "Advanced users can work with raw data"
    (let [layers (aog/layers
                  {:transformation nil
                   :data {:x [1 2 3] :y [4 5 6]}
                   :positional [:x :y]
                   :named {:color :species}
                   :plottype :scatter
                   :attributes {:alpha 0.5}})]

      (is (vector? layers))
      (is (= 1 (count layers)))
      (let [[layer] layers]
        (is (= :scatter (:plottype layer)))
        (is (= {:x [1 2 3] :y [4 5 6]} (:data layer)))
        (is (= [:x :y] (:positional layer)))))))

;;; =============================================================================
;;; Comparison with v1 Tests

(deftest test-simplification-benefits
  (testing "v2 eliminates type checking branches"
    ;; In v1, * had 4 branches checking (layers? x)
    ;; In v2, * just does Cartesian product on vectors
    (let [result (aog/* [(aog/data {:x [1]})]
                        [(aog/mapping :x)])]
      ;; No need to check if result is single or vector
      ;; It's ALWAYS a vector
      (is (vector? result))
      (is (= 1 (count result)))))

  (testing "v2 eliminates helper predicates"
    ;; In v1, we needed layers? and layer? predicates
    ;; In v2, we just check vector? (which is built-in)
    (let [layers (aog/* [(aog/data {:x [1]})]
                        [(aog/mapping :x)])]
      (is (vector? layers))
      (is (every? map? layers)))))

;;; =============================================================================
;;; Property-Based Tests (Demonstrating Consistency)

(deftest test-vector-consistency-property
  (testing "Every constructor always returns a vector"
    (let [constructors [#(aog/data {:x [1]})
                        #(aog/mapping :x :y)
                        #(aog/mapping {:color :species})
                        #(aog/scatter)
                        #(aog/line)
                        #(aog/linear)
                        #(aog/histogram)]]
      (doseq [constructor constructors]
        (is (vector? (constructor))))))

  (testing "Every operator always returns a vector"
    (is (vector? (aog/* [(aog/scatter)])))
    (is (vector? (aog/* [(aog/scatter)] [(aog/line)])))
    (is (vector? (aog/+ [(aog/scatter)] [(aog/line)])))))

(deftest test-composition-associativity
  (testing "* is associative"
    (let [a [(aog/data {:x [1]})]
          b [(aog/mapping :x)]
          c [(aog/scatter)]]
      (is (= (aog/* (aog/* a b) c)
             (aog/* a (aog/* b c))
             (aog/* a b c))))))

(comment
  ;; Run tests
  (clojure.test/run-tests 'tableplot.v2.aog-test))
