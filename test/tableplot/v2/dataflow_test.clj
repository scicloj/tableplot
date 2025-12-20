(ns tableplot.v2.dataflow-test
  (:require [clojure.test :refer [deftest is testing]]
            [tableplot.v2.dataflow :as df]
            [tableplot.v2.inference :as infer]
            [tableplot.v2.api :as api]
            [tech.v3.dataset :as ds]))

;;; ============================================================================
;;; Test Data
;;; ============================================================================

(def test-dataset
  (ds/->dataset {:x [1 2 3 4 5]
                 :y [2 4 6 8 10]
                 :category [:a :b :a :b :a]}))

;;; ============================================================================
;;; Core Dataflow Tests
;;; ============================================================================

(deftest test-subkey-by-convention
  (testing "Subkey detection by convention"
    (is (df/subkey-by-convention? :=foo))
    (is (df/subkey-by-convention? :=x-scale))
    (is (not (df/subkey-by-convention? :foo)))
    (is (not (df/subkey-by-convention? :x)))))

(deftest test-find-subkeys
  (testing "Finding subkeys in nested structures"
    (is (= #{:=data :=x-scale}
           (df/find-subkeys {:data :=data
                             :scales {:x :=x-scale}})))
    (is (= #{:=layer-1 :=layer-2}
           (df/find-subkeys {:layers [:=layer-1 :=layer-2]})))))

(deftest test-spec-manipulation
  (testing "Creating and manipulating specs"
    (let [spec (df/make-spec {:data :=data})]
      (is (map? spec))

      ;; Add a substitution
      (let [spec' (df/add-substitution spec :=data test-dataset)]
        (is (= test-dataset (df/get-substitution spec' :=data))))

      ;; Add multiple substitutions
      (let [spec' (df/add-substitutions spec {:=data test-dataset
                                              :=title "Test Plot"})]
        (is (= test-dataset (df/get-substitution spec' :=data)))
        (is (= "Test Plot" (df/get-substitution spec' :=title)))))))

(deftest test-apply-substitutions
  (testing "Applying substitutions replaces :=keys with values"
    (let [spec {:data :=data
                :title :=title
                :=substitutions {:=data test-dataset
                                 :=title "My Plot"}}
          result (df/apply-substitutions spec)]
      (is (= test-dataset (:data result)))
      (is (= "My Plot" (:title result)))
      (is (nil? (:=substitutions result))))))

;;; ============================================================================
;;; API Tests
;;; ============================================================================

(deftest test-layer-builders
  (testing "Layer builder functions create correct maps"
    (is (= {:mark :point :x :foo :y :bar}
           (api/point :x :foo :y :bar)))
    (is (= {:mark :line :x :foo :y :bar :color :baz}
           (api/line :x :foo :y :bar :color :baz)))
    (is (= {:mark :bar :x :foo :y :bar}
           (api/bar :x :foo :y :bar)))))

(deftest test-plot-construction
  (testing "Plot construction adds data and layers"
    (let [spec (api/plot test-dataset
                         (api/point :x :x :y :y))]
      (is (= test-dataset (df/get-substitution spec :=data)))
      (is (= [(api/point :x :x :y :y)]
             (df/get-substitution spec :=layers))))))

(deftest test-incremental-building
  (testing "Incremental spec building works with threading"
    (let [spec (-> (api/base-plot)
                   (api/add-data test-dataset)
                   (api/add-layer (api/point :x :x :y :y))
                   (api/add-title "Test Plot"))]
      (is (= test-dataset (df/get-substitution spec :=data)))
      (is (= "Test Plot" (df/get-substitution spec :=title)))
      (is (= 1 (count (df/get-substitution spec :=layers)))))))

;;; ============================================================================
;;; Inference Tests
;;; ============================================================================

(deftest test-field-inference
  (testing "Fields are inferred from layers"
    (let [spec (-> (api/base-plot)
                   (api/add-data test-dataset)
                   (api/add-layer (api/point :x :x :y :y)))
          inferred (df/infer-missing-keys spec)]
      (is (= :x (df/get-substitution inferred :=x-field)))
      (is (= :y (df/get-substitution inferred :=y-field))))))

(deftest test-scale-inference
  (testing "Scales are inferred from data types"
    (let [spec (-> (api/scatter test-dataset :x :y)
                   df/infer-missing-keys)
          x-scale (df/get-substitution spec :=x-scale)]
      (is (= :linear (:type x-scale)))
      (is (= :x (:field x-scale)))
      (is (vector? (:domain x-scale)))
      (is (= [1 5] (:domain x-scale))))))

(deftest test-complete-flow
  (testing "Complete flow from API to finalized spec"
    (let [spec (api/scatter test-dataset :x :y)
          finalized (api/finalize spec)]
      ;; Should have data
      (is (= test-dataset (:data finalized)))

      ;; Should have layers
      (is (vector? (:layers finalized)))
      (is (= 1 (count (:layers finalized))))

      ;; Should have inferred scales
      (is (map? (:scales finalized)))
      (is (some? (get-in finalized [:scales :x])))
      (is (some? (get-in finalized [:scales :y])))

      ;; Should have title
      (is (string? (:title finalized)))

      ;; Should not have :=substitutions anymore
      (is (nil? (:=substitutions finalized))))))

;;; ============================================================================
;;; Integration Tests
;;; ============================================================================

(deftest test-scatter-plot
  (testing "Creating a scatter plot with all defaults"
    (let [plot (api/render (api/scatter test-dataset :x :y))]
      (is (map? plot))
      (is (some? (:data plot)))
      (is (some? (:layers plot)))
      (is (some? (:scales plot))))))

(deftest test-colored-scatter
  (testing "Creating a scatter plot with color aesthetic"
    (let [plot (api/render (api/scatter test-dataset :x :y :color :category))]
      (is (map? plot))
      (is (some? (get-in plot [:scales :color])))
      ;; Color scale should be ordinal for categorical data
      (is (= :ordinal (get-in plot [:scales :color :type]))))))

(deftest test-multiple-layers
  (testing "Creating a plot with multiple layers"
    (let [plot (api/render
                (api/plot test-dataset
                          (api/point :x :x :y :y)
                          (api/line :x :x :y :y)))]
      (is (= 2 (count (:layers plot)))))))
