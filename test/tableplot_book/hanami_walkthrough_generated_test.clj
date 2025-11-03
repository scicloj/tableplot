(ns
 tableplot-book.hanami-walkthrough-generated-test
 (:require
  [scicloj.tableplot.v1.hanami :as hanami]
  [aerial.hanami.templates :as ht]
  [tablecloth.api :as tc]
  [tech.v3.datatype.datetime :as datetime]
  [tech.v3.dataset.print :as print]
  [scicloj.kindly.v4.kind :as kind]
  [clojure.string :as str]
  [scicloj.kindly.v4.api :as kindly]
  [scicloj.metamorph.ml.rdatasets :as rdatasets]
  [clojure.test :refer [deftest is]]))


(def
 v3_l48
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   hanami/point-chart
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 200})))


(deftest
 t4_l55
 (is
  ((fn* [p1__86647#] (= (-> p1__86647# :mark :type) "circle")) v3_l48)))


(def
 v6_l73
 (->
  (rdatasets/datasets-iris)
  (hanami/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 200})))


(deftest
 t7_l80
 (is
  ((fn*
    [p1__86648#]
    (let
     [layer-defaults
      (->
       p1__86648#
       :aerial.hanami.templates/defaults
       :=layer
       first
       :aerial.hanami.templates/defaults)]
     (and
      (= (:=x layer-defaults) :sepal-width)
      (= (:=y layer-defaults) :sepal-length)
      (= (:=color layer-defaults) :species))))
   v6_l73)))


(def
 v9_l88
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   hanami/point-chart
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 200})
  kind/pprint))


(deftest
 t10_l96
 (is
  ((fn*
    [p1__86649#]
    (and
     (contains? p1__86649# :data)
     (contains? p1__86649# :encoding)
     (= (-> p1__86649# :mark :type) "circle")))
   v9_l88)))


(def
 v12_l107
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   hanami/point-chart
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 200})
  meta))


(deftest
 t13_l115
 (is
  ((fn* [p1__86650#] (= (:kindly/kind p1__86650#) :kind/vega-lite))
   v12_l107)))


(def
 v15_l122
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   ht/point-chart
   {:X :sepal-width, :Y :sepal-length, :MSIZE 200, :COLOR "species"})))


(deftest
 t16_l129
 (is
  ((fn* [p1__86651#] (= (-> p1__86651# :mark :type) "circle"))
   v15_l122)))


(def
 v17_l131
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   ht/point-chart
   {:X :sepal-width, :Y :sepal-length, :MSIZE 200, :COLOR "species"})
  kind/pprint))


(def
 v19_l145
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   hanami/point-chart
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 200})))


(deftest
 t20_l152
 (is
  ((fn*
    [p1__86652#]
    (and
     (= (-> p1__86652# :encoding :x :type) :quantitative)
     (= (-> p1__86652# :encoding :y :type) :quantitative)
     (= (-> p1__86652# :encoding :color :type) :nominal)))
   v19_l145)))


(def
 v21_l156
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   hanami/point-chart
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 200})
  kind/pprint))


(def
 v23_l167
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/point-chart
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 200})))


(deftest
 t24_l174
 (is
  ((fn*
    [p1__86653#]
    (= (-> p1__86653# :encoding :color :type) :quantitative))
   v23_l167)))


(def
 v25_l176
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/point-chart
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 200})
  kind/pprint))


(def
 v27_l186
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/point-chart
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=color-type :nominal,
    :=mark-size 200})))


(deftest
 t28_l194
 (is
  ((fn*
    [p1__86654#]
    (= (-> p1__86654# :encoding :color :type) :nominal))
   v27_l186)))


(def
 v29_l196
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/point-chart
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=color-type :nominal,
    :=mark-size 200})
  kind/pprint))


(def
 v31_l209
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/boxplot-chart
   {:=x :cyl, :=x-type :nominal, :=y :disp})))


(deftest
 t32_l215
 (is
  ((fn*
    [p1__86655#]
    (and
     (= (-> p1__86655# :mark :type) "boxplot")
     (= (-> p1__86655# :encoding :x :type) :nominal)))
   v31_l209)))


(def
 v34_l220
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot ht/boxplot-chart {:X :cyl, :XTYPE :nominal, :Y :disp})))


(deftest
 t35_l226
 (is
  ((fn* [p1__86656#] (= (-> p1__86656# :mark :type) "boxplot"))
   v34_l220)))


(def
 v37_l230
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   hanami/rule-chart
   {:=x :sepal-width,
    :=y :sepal-length,
    :=x2 :petal-width,
    :=y2 :petal-length,
    :=mark-opacity 0.5,
    :=mark-size 3,
    :=color :species})))


(deftest
 t38_l240
 (is
  ((fn*
    [p1__86657#]
    (and
     (= (-> p1__86657# :mark :type) "rule")
     (contains? (-> p1__86657# :encoding) :x2)
     (contains? (-> p1__86657# :encoding) :y2)))
   v37_l230)))


(def
 v40_l246
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   ht/rule-chart
   {:X :sepal-width,
    :Y :sepal-length,
    :X2 :petal-width,
    :Y2 :petal-length,
    :OPACITY 0.5,
    :SIZE 3,
    :COLOR "species"})))


(def
 v42_l260
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86658#] (-> p1__86658# :variable (= "unemploy"))))
  (hanami/plot
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})))


(deftest
 t43_l267
 (is
  ((fn*
    [p1__86659#]
    (and
     (= (-> p1__86659# :mark :type) "line")
     (= (-> p1__86659# :encoding :x :type) :temporal)))
   v42_l260)))


(def
 v45_l273
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86660#] (-> p1__86660# :variable (= "unemploy"))))
  (hanami/plot
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})
  kind/pprint))


(def
 v47_l286
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86661#] (-> p1__86661# :variable (= "unemploy"))))
  (hanami/base
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})))


(deftest
 t48_l293
 (is
  ((fn*
    [p1__86662#]
    (contains?
     (:aerial.hanami.templates/defaults p1__86662#)
     :=dataset))
   v47_l286)))


(def
 v50_l300
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86663#] (-> p1__86663# :variable (= "unemploy"))))
  (hanami/plot
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})
  kind/pprint))


(def
 v51_l308
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86664#] (-> p1__86664# :variable (= "unemploy"))))
  (hanami/base
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})
  kind/pprint))


(def
 v53_l329
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86665#] (-> p1__86665# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value, :=mark-color "purple"})
  hanami/layer-line))


(deftest
 t54_l336
 (is
  ((fn*
    [p1__86666#]
    (=
     (-> p1__86666# :aerial.hanami.templates/defaults :=layer count)
     1))
   v53_l329)))


(def
 v56_l340
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86667#] (-> p1__86667# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})))


(def
 v58_l348
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86668#] (-> p1__86668# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-point
   {:=mark-color "green", :=mark-size 200, :=mark-opacity 0.1})
  (hanami/layer-line {:=mark-color "purple"})))


(deftest
 t59_l357
 (is
  ((fn*
    [p1__86669#]
    (=
     (-> p1__86669# :aerial.hanami.templates/defaults :=layer count)
     2))
   v58_l348)))


(def
 v61_l361
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86670#] (-> p1__86670# :variable (= "unemploy"))))
  (hanami/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(def
 v63_l375
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86671#] (-> p1__86671# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})))


(deftest
 t64_l384
 (is
  ((fn*
    [p1__86672#]
    (let
     [layers (-> p1__86672# :aerial.hanami.templates/defaults :=layer)]
     (and
      (= (count layers) 2)
      (=
       (->
        layers
        second
        :aerial.hanami.templates/defaults
        :=layer-dataset
        tc/row-count)
       5))))
   v63_l375)))


(def
 v66_l397
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86673#] (-> p1__86673# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})
  hanami/plot
  kind/pprint))


(def
 v68_l412
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86674#] (-> p1__86674# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})
  hanami/plot
  (assoc :background "lightgrey")))


(deftest
 t69_l423
 (is
  ((fn* [p1__86675#] (= (:background p1__86675#) "lightgrey"))
   v68_l412)))


(def
 v71_l429
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86676#] (-> p1__86676# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})
  hanami/plot
  (assoc-in [:encoding :y :scale :type] "log")))


(deftest
 t72_l440
 (is
  ((fn*
    [p1__86677#]
    (= (-> p1__86677# :encoding :y :scale :type) "log"))
   v71_l429)))


(def
 v74_l451
 (->
  (rdatasets/datasets-iris)
  (hanami/base
   {:=title "dummy",
    :=mark-color "green",
    :=x :sepal-width,
    :=y :sepal-length})
  hanami/layer-point
  (hanami/layer-smooth {:=mark-color "orange"})))


(deftest
 t75_l459
 (is
  ((fn*
    [p1__86678#]
    (=
     (-> p1__86678# :aerial.hanami.templates/defaults :=layer count)
     2))
   v74_l451)))


(def
 v77_l466
 (->
  (rdatasets/datasets-iris)
  (hanami/base {:=x :sepal-width, :=y :sepal-length})
  hanami/layer-point
  (hanami/layer-smooth {:=predictors [:petal-width :petal-length]})))


(deftest
 t78_l473
 (is
  ((fn*
    [p1__86679#]
    (let
     [smooth-layer
      (->
       p1__86679#
       :aerial.hanami.templates/defaults
       :=layer
       second
       :aerial.hanami.templates/defaults)]
     (= (:=predictors smooth-layer) [:petal-width :petal-length])))
   v77_l466)))


(def
 v80_l484
 (->
  (rdatasets/datasets-iris)
  (hanami/base
   {:=title "dummy",
    :=color :species,
    :=x :sepal-width,
    :=y :sepal-length})
  hanami/layer-point
  hanami/layer-smooth))


(deftest
 t81_l492
 (is
  ((fn*
    [p1__86680#]
    (let
     [base-defaults (-> p1__86680# :aerial.hanami.templates/defaults)]
     (= (:=color base-defaults) :species)))
   v80_l484)))


(def
 v83_l501
 (->
  (rdatasets/datasets-iris)
  (hanami/base
   {:=title "dummy",
    :=mark-color "green",
    :=color :species,
    :=group [],
    :=x :sepal-width,
    :=y :sepal-length})
  hanami/layer-point
  hanami/layer-smooth))


(deftest
 t84_l511
 (is
  ((fn*
    [p1__86681#]
    (let
     [base-defaults (-> p1__86681# :aerial.hanami.templates/defaults)]
     (= (:=group base-defaults) [])))
   v83_l501)))


(def
 v86_l524
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86682#] (-> p1__86682# :variable (= "unemploy"))))
  (tc/add-column :relative-time "Past")
  (tc/concat
   (tc/dataset
    {:date
     (->
      (rdatasets/ggplot2-economics_long)
      :date
      last
      (datetime/plus-temporal-amount (range 96) :days)),
     :relative-time "Future"}))
  (print/print-range 6)))


(def
 v88_l536
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86683#] (-> p1__86683# :variable (= "unemploy"))))
  (tc/add-column :relative-time "Past")
  (tc/concat
   (tc/dataset
    {:date
     (->
      (rdatasets/ggplot2-economics_long)
      :date
      last
      (datetime/plus-temporal-amount (range 96) :months)),
     :relative-time "Future"}))
  (tc/add-column
   :year
   (fn*
    [p1__86684#]
    (datetime/long-temporal-field :years (:date p1__86684#))))
  (tc/add-column
   :month
   (fn*
    [p1__86685#]
    (datetime/long-temporal-field :months (:date p1__86685#))))
  (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
  (print/print-range 6)))


(def
 v90_l555
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86686#] (-> p1__86686# :variable (= "unemploy"))))
  (tc/add-column :relative-time "Past")
  (tc/concat
   (tc/dataset
    {:date
     (->
      (rdatasets/ggplot2-economics_long)
      :date
      last
      (datetime/plus-temporal-amount (range 96) :months)),
     :relative-time "Future"}))
  (tc/add-column
   :year
   (fn*
    [p1__86687#]
    (datetime/long-temporal-field :years (:date p1__86687#))))
  (tc/add-column
   :month
   (fn*
    [p1__86688#]
    (datetime/long-temporal-field :months (:date p1__86688#))))
  (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-smooth
   {:=color :relative-time,
    :=mark-size 10,
    :=group [],
    :=predictors [:yearmonth]})
  (hanami/update-data
   (fn
    [dataset]
    (->
     dataset
     (tc/select-rows (fn [row] (-> row :relative-time (= "Past")))))))
  (hanami/layer-line {:=mark-color "purple", :=mark-size 3})))


(def
 v92_l585
 (->
  (rdatasets/datasets-iris)
  (hanami/layer-histogram {:=x :sepal-width})))


(deftest
 t93_l588
 (is
  ((fn*
    [p1__86689#]
    (let
     [layer-defaults
      (->
       p1__86689#
       :aerial.hanami.templates/defaults
       :=layer
       first
       :aerial.hanami.templates/defaults)]
     (= (:=x layer-defaults) :sepal-width)))
   v92_l585)))


(def
 v94_l591
 (->
  (rdatasets/datasets-iris)
  (hanami/layer-histogram {:=x :sepal-width, :=histogram-nbins 30})))


(deftest
 t95_l595
 (is
  ((fn*
    [p1__86690#]
    (let
     [layer-defaults
      (->
       p1__86690#
       :aerial.hanami.templates/defaults
       :=layer
       first
       :aerial.hanami.templates/defaults)]
     (= (:=histogram-nbins layer-defaults) 30)))
   v94_l591)))
