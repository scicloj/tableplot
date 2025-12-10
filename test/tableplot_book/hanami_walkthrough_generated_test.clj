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
  ((fn* [p1__70723#] (= (-> p1__70723# :mark :type) "circle")) v3_l48)))


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
    [p1__70724#]
    (let
     [layer-defaults
      (->
       p1__70724#
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
 v9_l92
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
 t10_l100
 (is
  ((fn*
    [p1__70725#]
    (and
     (contains? p1__70725# :data)
     (contains? p1__70725# :encoding)
     (= (-> p1__70725# :mark :type) "circle")))
   v9_l92)))


(def
 v12_l111
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
 t13_l119
 (is
  ((fn* [p1__70726#] (= (:kindly/kind p1__70726#) :kind/vega-lite))
   v12_l111)))


(def
 v15_l126
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   ht/point-chart
   {:X :sepal-width, :Y :sepal-length, :MSIZE 200, :COLOR "species"})))


(deftest
 t16_l133
 (is
  ((fn* [p1__70727#] (= (-> p1__70727# :mark :type) "circle"))
   v15_l126)))


(def
 v17_l135
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   ht/point-chart
   {:X :sepal-width, :Y :sepal-length, :MSIZE 200, :COLOR "species"})
  kind/pprint))


(def
 v19_l149
 (->
  (rdatasets/datasets-iris)
  (hanami/plot
   hanami/point-chart
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 200})))


(deftest
 t20_l156
 (is
  ((fn*
    [p1__70728#]
    (and
     (= (-> p1__70728# :encoding :x :type) :quantitative)
     (= (-> p1__70728# :encoding :y :type) :quantitative)
     (= (-> p1__70728# :encoding :color :type) :nominal)))
   v19_l149)))


(def
 v21_l160
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
 v23_l171
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/point-chart
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 200})))


(deftest
 t24_l178
 (is
  ((fn*
    [p1__70729#]
    (= (-> p1__70729# :encoding :color :type) :quantitative))
   v23_l171)))


(def
 v25_l180
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/point-chart
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 200})
  kind/pprint))


(def
 v27_l190
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
 t28_l198
 (is
  ((fn*
    [p1__70730#]
    (= (-> p1__70730# :encoding :color :type) :nominal))
   v27_l190)))


(def
 v29_l200
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
 v31_l213
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot
   hanami/boxplot-chart
   {:=x :cyl, :=x-type :nominal, :=y :disp})))


(deftest
 t32_l219
 (is
  ((fn*
    [p1__70731#]
    (and
     (= (-> p1__70731# :mark :type) "boxplot")
     (= (-> p1__70731# :encoding :x :type) :nominal)))
   v31_l213)))


(def
 v34_l224
 (->
  (rdatasets/datasets-mtcars)
  (hanami/plot ht/boxplot-chart {:X :cyl, :XTYPE :nominal, :Y :disp})))


(deftest
 t35_l230
 (is
  ((fn* [p1__70732#] (= (-> p1__70732# :mark :type) "boxplot"))
   v34_l224)))


(def
 v37_l234
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
 t38_l244
 (is
  ((fn*
    [p1__70733#]
    (and
     (= (-> p1__70733# :mark :type) "rule")
     (contains? (-> p1__70733# :encoding) :x2)
     (contains? (-> p1__70733# :encoding) :y2)))
   v37_l234)))


(def
 v40_l250
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
 v42_l264
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70734#] (-> p1__70734# :variable (= "unemploy"))))
  (hanami/plot
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})))


(deftest
 t43_l271
 (is
  ((fn*
    [p1__70735#]
    (and
     (= (-> p1__70735# :mark :type) "line")
     (= (-> p1__70735# :encoding :x :type) :temporal)))
   v42_l264)))


(def
 v45_l277
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70736#] (-> p1__70736# :variable (= "unemploy"))))
  (hanami/plot
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})
  kind/pprint))


(def
 v47_l290
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70737#] (-> p1__70737# :variable (= "unemploy"))))
  (hanami/base
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})))


(deftest
 t48_l297
 (is
  ((fn*
    [p1__70738#]
    (contains?
     (:aerial.hanami.templates/defaults p1__70738#)
     :=dataset))
   v47_l290)))


(def
 v50_l304
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70739#] (-> p1__70739# :variable (= "unemploy"))))
  (hanami/plot
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})
  kind/pprint))


(def
 v51_l312
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70740#] (-> p1__70740# :variable (= "unemploy"))))
  (hanami/base
   hanami/line-chart
   {:=x :date, :=y :value, :=mark-color "purple"})
  kind/pprint))


(def
 v53_l333
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70741#] (-> p1__70741# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value, :=mark-color "purple"})
  hanami/layer-line))


(deftest
 t54_l340
 (is
  ((fn*
    [p1__70742#]
    (=
     (-> p1__70742# :aerial.hanami.templates/defaults :=layer count)
     1))
   v53_l333)))


(def
 v56_l344
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70743#] (-> p1__70743# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})))


(def
 v58_l352
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70744#] (-> p1__70744# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-point
   {:=mark-color "green", :=mark-size 200, :=mark-opacity 0.1})
  (hanami/layer-line {:=mark-color "purple"})))


(deftest
 t59_l361
 (is
  ((fn*
    [p1__70745#]
    (=
     (-> p1__70745# :aerial.hanami.templates/defaults :=layer count)
     2))
   v58_l352)))


(def
 v61_l365
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70746#] (-> p1__70746# :variable (= "unemploy"))))
  (hanami/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(def
 v63_l379
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70747#] (-> p1__70747# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})))


(deftest
 t64_l388
 (is
  ((fn*
    [p1__70748#]
    (let
     [layers (-> p1__70748# :aerial.hanami.templates/defaults :=layer)]
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
   v63_l379)))


(def
 v66_l401
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70749#] (-> p1__70749# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})
  hanami/plot
  kind/pprint))


(def
 v68_l416
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70750#] (-> p1__70750# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})
  hanami/plot
  (assoc :background "lightgrey")))


(deftest
 t69_l427
 (is
  ((fn* [p1__70751#] (= (:background p1__70751#) "lightgrey"))
   v68_l416)))


(def
 v71_l433
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70752#] (-> p1__70752# :variable (= "unemploy"))))
  (hanami/base {:=x :date, :=y :value})
  (hanami/layer-line {:=mark-color "purple"})
  (hanami/update-data tc/random 5)
  (hanami/layer-point {:=mark-color "green", :=mark-size 200})
  hanami/plot
  (assoc-in [:encoding :y :scale :type] "log")))


(deftest
 t72_l444
 (is
  ((fn*
    [p1__70753#]
    (= (-> p1__70753# :encoding :y :scale :type) "log"))
   v71_l433)))


(def
 v74_l455
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
 t75_l463
 (is
  ((fn*
    [p1__70754#]
    (=
     (-> p1__70754# :aerial.hanami.templates/defaults :=layer count)
     2))
   v74_l455)))


(def
 v77_l470
 (->
  (rdatasets/datasets-iris)
  (hanami/base {:=x :sepal-width, :=y :sepal-length})
  hanami/layer-point
  (hanami/layer-smooth {:=predictors [:petal-width :petal-length]})))


(deftest
 t78_l477
 (is
  ((fn*
    [p1__70755#]
    (let
     [smooth-layer
      (->
       p1__70755#
       :aerial.hanami.templates/defaults
       :=layer
       second
       :aerial.hanami.templates/defaults)]
     (= (:=predictors smooth-layer) [:petal-width :petal-length])))
   v77_l470)))


(def
 v80_l488
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
 t81_l496
 (is
  ((fn*
    [p1__70756#]
    (let
     [base-defaults (-> p1__70756# :aerial.hanami.templates/defaults)]
     (= (:=color base-defaults) :species)))
   v80_l488)))


(def
 v83_l505
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
 t84_l515
 (is
  ((fn*
    [p1__70757#]
    (let
     [base-defaults (-> p1__70757# :aerial.hanami.templates/defaults)]
     (= (:=group base-defaults) [])))
   v83_l505)))


(def
 v86_l528
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70758#] (-> p1__70758# :variable (= "unemploy"))))
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
 v88_l540
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70759#] (-> p1__70759# :variable (= "unemploy"))))
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
    [p1__70760#]
    (datetime/long-temporal-field :years (:date p1__70760#))))
  (tc/add-column
   :month
   (fn*
    [p1__70761#]
    (datetime/long-temporal-field :months (:date p1__70761#))))
  (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
  (print/print-range 6)))


(def
 v90_l559
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__70762#] (-> p1__70762# :variable (= "unemploy"))))
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
    [p1__70763#]
    (datetime/long-temporal-field :years (:date p1__70763#))))
  (tc/add-column
   :month
   (fn*
    [p1__70764#]
    (datetime/long-temporal-field :months (:date p1__70764#))))
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
 v92_l589
 (->
  (rdatasets/datasets-iris)
  (hanami/layer-histogram {:=x :sepal-width})))


(deftest
 t93_l592
 (is
  ((fn*
    [p1__70765#]
    (let
     [layer-defaults
      (->
       p1__70765#
       :aerial.hanami.templates/defaults
       :=layer
       first
       :aerial.hanami.templates/defaults)]
     (= (:=x layer-defaults) :sepal-width)))
   v92_l589)))


(def
 v94_l595
 (->
  (rdatasets/datasets-iris)
  (hanami/layer-histogram {:=x :sepal-width, :=histogram-nbins 30})))


(deftest
 t95_l599
 (is
  ((fn*
    [p1__70766#]
    (let
     [layer-defaults
      (->
       p1__70766#
       :aerial.hanami.templates/defaults
       :=layer
       first
       :aerial.hanami.templates/defaults)]
     (= (:=histogram-nbins layer-defaults) 30)))
   v94_l595)))
