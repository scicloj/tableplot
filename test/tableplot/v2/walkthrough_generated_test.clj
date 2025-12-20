(ns
 tableplot.v2.walkthrough-generated-test
 (:require
  [tableplot.v2.dataflow :as df]
  [tableplot.v2.inference :as infer]
  [tableplot.v2.api :as api]
  [tech.v3.dataset :as ds]
  [scicloj.kindly.v4.kind :as kind]
  [clojure.test :refer [deftest is]]))


(def v3_l50 nil)


(def v5_l53 nil)


(def v7_l58 (df/subkey-by-convention? :x))


(deftest t8_l61 (is ([:test false] v7_l58)))


(def v9_l63 (df/subkey-by-convention? :=x))


(deftest t10_l66 (is ([:test true] v9_l63)))


(def v12_l69 (df/subkey? df/base-plot-template :=data))


(deftest t13_l72 (is ([:test true] v12_l69)))


(def
 v15_l79
 {:data :=data,
  :layers :=layers,
  :scales {:x :=x-scale, :y :=y-scale},
  :guides {:x :=x-guide, :y :=y-guide}})


(def v17_l87 {:=substitutions {:=data nil, :=layers []}})


(def
 v19_l91
 {:data :=data,
  :layers :=layers,
  :scales {:x :=x-scale, :y :=y-scale},
  :=substitutions {:=data nil, :=layers []}})


(def v21_l103 (keys df/*inference-rules*))


(def
 v23_l127
 (df/find-subkeys
  {:data :=data,
   :layers :=layers,
   :scales {:x :=x-scale, :y :=y-scale}}))


(deftest
 t24_l132
 (is
  ([:test
    (fn*
     [p1__98808#]
     (= #{:=y-scale :=layers :=data :=x-scale} p1__98808#))]
   v23_l127)))


(def
 v26_l139
 (def spec-1 (df/make-spec {:data :=data, :title :=title})))


(def v27_l142 spec-1)


(def
 v29_l145
 (def spec-2 (df/add-substitution spec-1 :=data "my-dataset")))


(def v30_l148 (df/get-substitution spec-2 :=data))


(deftest t31_l151 (is ([:test "my-dataset"] v30_l148)))


(def
 v33_l154
 (def spec-3 (df/add-substitutions spec-2 {:=title "My Plot"})))


(def v34_l157 (df/get-substitution spec-3 :=title))


(deftest t35_l160 (is ([:test "My Plot"] v34_l157)))


(def
 v37_l166
 (df/apply-substitutions
  {:data :=data,
   :title :=title,
   :=substitutions {:=data "my-dataset", :=title "My Plot"}}))


(deftest
 t38_l172
 (is
  ([:test
    (fn*
     [p1__98809#]
     (and
      (= "my-dataset" (:data p1__98809#))
      (= "My Plot" (:title p1__98809#))
      (nil? (:=substitutions p1__98809#))))]
   v37_l166)))


(def
 v40_l183
 (:tableplot.v2.dataflow/depends-on
  (meta (get df/*inference-rules* :=x-guide))))


(deftest t41_l186 (is ([:test #{:=x-scale}] v40_l183)))


(def v43_l195 (df/infer {:value :=value, :=substitutions {:=value 42}}))


(deftest
 t44_l199
 (is ([:test (fn* [p1__98810#] (= 42 (:value p1__98810#)))] v43_l195)))


(def
 v46_l209
 (infer/infer-x-field
  {:=substitutions
   {:=layers [{:mark :point, :x :sepal-width, :y :sepal-length}]}}
  {}))


(deftest t47_l214 (is ([:test :sepal-width] v46_l209)))


(def
 v48_l216
 (infer/infer-y-field
  {:=substitutions
   {:=layers [{:mark :point, :x :sepal-width, :y :sepal-length}]}}
  {}))


(deftest t49_l221 (is ([:test :sepal-length] v48_l216)))


(def
 v51_l227
 (def test-data (ds/->dataset {:x [1 2 3 4 5], :y [10 20 30 40 50]})))


(def
 v53_l232
 (infer/infer-x-scale
  {:=substitutions {:=data test-data, :=x-field :x}}
  {}))


(deftest
 t54_l237
 (is
  ([:test
    (fn*
     [p1__98811#]
     (and
      (= :linear (:type p1__98811#))
      (= :x (:field p1__98811#))
      (= [1 5] (:domain p1__98811#))))]
   v53_l232)))


(def
 v56_l243
 (infer/infer-x-guide
  {:=substitutions
   {:=x-scale {:type :linear, :field :sepal-width, :domain [2.0 4.5]}}}
  {}))


(deftest
 t57_l249
 (is
  ([:test
    (fn*
     [p1__98812#]
     (and
      (= :axis (:type p1__98812#))
      (= :bottom (:orientation p1__98812#))
      (= "sepal-width" (:title p1__98812#))))]
   v56_l243)))


(def
 v59_l255
 (infer/infer-title
  {:=substitutions {:=x-field :sepal-width, :=y-field :sepal-length}}
  {}))


(deftest t60_l260 (is ([:test "sepal-length vs sepal-width"] v59_l255)))


(def
 v62_l270
 (api/point :x :sepal-width :y :sepal-length :color :species))


(deftest
 t63_l273
 (is
  ([:test
    {:mark :point,
     :x :sepal-width,
     :y :sepal-length,
     :color :species,
     :size nil}]
   v62_l270)))


(def v64_l279 (api/line :x :time :y :value))


(deftest
 t65_l282
 (is
  ([:test
    (fn*
     [p1__98813#]
     (and
      (= :line (:mark p1__98813#))
      (= :time (:x p1__98813#))
      (= :value (:y p1__98813#))))]
   v64_l279)))


(def v67_l290 (def base-spec (api/base-plot)))


(def v68_l292 (keys base-spec))


(def v70_l295 (def with-data-spec (api/add-data base-spec test-data)))


(def v71_l297 (df/get-substitution with-data-spec :=data))


(deftest t72_l300 (is ([:test test-data] v71_l297)))


(def
 v74_l303
 (def
  with-layer-spec
  (api/add-layer with-data-spec (api/point :x :x :y :y))))


(def v75_l306 (count (df/get-substitution with-layer-spec :=layers)))


(deftest t76_l309 (is ([:test 1] v75_l306)))


(def
 v78_l312
 (def
  with-title-spec
  (api/add-title with-layer-spec "My Scatter Plot")))


(def v79_l315 (df/get-substitution with-title-spec :=title))


(deftest t80_l318 (is ([:test "My Scatter Plot"] v79_l315)))


(def v82_l325 (def quick-scatter (api/scatter test-data :x :y)))


(def v83_l328 (keys quick-scatter))


(deftest
 t84_l331
 (is
  ([:test (fn* [p1__98814#] (some #{:=substitutions} p1__98814#))]
   v83_l328)))


(def v86_l334 (df/get-substitution quick-scatter :=layers))


(deftest
 t87_l337
 (is
  ([:test
    (fn*
     [p1__98815#]
     (and
      (vector? p1__98815#)
      (= 1 (count p1__98815#))
      (= :point (:mark (first p1__98815#)))))]
   v86_l334)))


(def
 v89_l345
 (def
  threaded-spec
  (->
   (api/base-plot)
   (api/add-data test-data)
   (api/add-layer (api/point :x :x :y :y))
   (api/add-title "Threaded Example"))))


(def v90_l351 (df/get-substitution threaded-spec :=title))


(deftest t91_l354 (is ([:test "Threaded Example"] v90_l351)))


(def
 v93_l362
 (def
  iris
  (ds/->dataset
   {:sepal-width [3.5 3.0 3.2 3.1 3.6],
    :sepal-length [5.1 4.9 4.7 4.6 5.0],
    :species [:setosa :setosa :setosa :setosa :setosa]})))


(def v94_l367 iris)


(def
 v96_l371
 (def
  my-spec
  (api/scatter iris :sepal-width :sepal-length :color :species)))


(def v98_l375 (keys my-spec))


(def v100_l378 (keys (dissoc my-spec :=substitutions)))


(def v102_l381 (keys (:=substitutions my-spec)))


(deftest
 t103_l384
 (is
  ([:test (fn* [p1__98816#] (some #{:=layers :=data} p1__98816#))]
   v102_l381)))


(def v105_l389 (def all-keys (df/find-subkeys my-spec)))


(def v106_l391 (count all-keys))


(def
 v108_l394
 (def existing-keys (set (keys (:=substitutions my-spec)))))


(def v109_l396 existing-keys)


(deftest
 t110_l399
 (is
  ([:test (fn* [p1__98817#] (contains? p1__98817# :=data))] v109_l396)))


(def v112_l402 (def missing-keys (remove existing-keys all-keys)))


(def v113_l404 (count missing-keys))


(def v115_l410 (def finalized-plot (api/finalize my-spec)))


(def v117_l415 (ds/row-count (:data finalized-plot)))


(deftest t118_l418 (is ([:test 5] v117_l415)))


(def v120_l421 (count (:layers finalized-plot)))


(deftest t121_l424 (is ([:test 1] v120_l421)))


(def v123_l427 (:scales finalized-plot))


(def v125_l430 (get-in finalized-plot [:scales :x :type]))


(deftest t126_l433 (is ([:test :linear] v125_l430)))


(def v127_l435 (get-in finalized-plot [:scales :x :field]))


(deftest t128_l438 (is ([:test :sepal-width] v127_l435)))


(def v130_l441 (get-in finalized-plot [:guides :x :type]))


(deftest t131_l444 (is ([:test :axis] v130_l441)))


(def v133_l447 (:=substitutions finalized-plot))


(deftest t134_l450 (is ([:test nil] v133_l447)))


(def
 v136_l457
 (def trace-spec (api/scatter iris :sepal-width :sepal-length)))


(def
 v138_l460
 (def
  trace-missing
  (let
   [all
    (df/find-subkeys trace-spec)
    existing
    (set (keys (:=substitutions trace-spec)))]
   (remove existing all))))


(def v139_l465 (count trace-missing))


(def v141_l468 (def trace-inferred (df/infer-missing-keys trace-spec)))


(def
 v143_l471
 (>
  (count (keys (:=substitutions trace-inferred)))
  (count (keys (:=substitutions trace-spec)))))


(deftest t144_l475 (is ([:test true] v143_l471)))


(def
 v146_l478
 (def trace-final (df/apply-substitutions trace-inferred)))


(def v148_l481 (empty? (df/find-subkeys trace-final)))


(deftest t149_l484 (is ([:test true] v148_l481)))


(def
 v151_l494
 (defn
  infer-nice-x-label
  [spec _context]
  "Generate a nice axis label from field name"
  (when-let
   [x-field (df/get-substitution spec :=x-field)]
   (->
    x-field
    name
    (clojure.string/replace "-" " ")
    clojure.string/capitalize))))


(def
 v153_l503
 (infer-nice-x-label {:=substitutions {:=x-field :sepal-width}} {}))


(deftest t154_l506 (is ([:test "Sepal width"] v153_l503)))


(def
 v156_l517
 (defn
  my-histogram
  [data x & {:keys [bins]}]
  "Create a histogram"
  (->
   (api/base-plot)
   (api/add-data data)
   (api/add-layer {:mark :bar, :x x, :y :count, :bins (or bins 10)}))))


(def v158_l527 (def hist-spec (my-histogram iris :sepal-width :bins 5)))


(def v159_l529 (df/get-substitution hist-spec :=data))


(deftest t160_l532 (is ([:test iris] v159_l529)))


(def
 v161_l534
 (-> (df/get-substitution hist-spec :=layers) first :bins))


(deftest t162_l539 (is ([:test 5] v161_l534)))


(def v164_l547 (map? my-spec))


(deftest t165_l550 (is ([:test true] v164_l547)))


(def v166_l552 (map? finalized-plot))


(deftest t167_l555 (is ([:test true] v166_l552)))


(def v169_l561 (map? df/*inference-rules*))


(deftest t170_l564 (is ([:test true] v169_l561)))


(def v171_l566 (> (count df/*inference-rules*) 10))


(deftest t172_l569 (is ([:test true] v171_l566)))


(def v174_l575 (fn? (get df/*inference-rules* :=x-scale)))


(deftest t175_l578 (is ([:test true] v174_l575)))


(def v177_l585 (keys (:=substitutions my-spec)))


(def v179_l588 (keys finalized-plot))


(def
 v181_l591
 (def
  inferred-keys
  (clojure.set/difference
   (set (keys finalized-plot))
   (set (keys my-spec)))))


(def v182_l596 (seq inferred-keys))
