(ns
 tableplot-book.plotly-walkthrough-generated-test
 (:require
  [scicloj.tableplot.v1.plotly :as plotly]
  [tablecloth.api :as tc]
  [tablecloth.column.api :as tcc]
  [tech.v3.datatype.datetime :as datetime]
  [tech.v3.dataset.print :as print]
  [scicloj.kindly.v4.kind :as kind]
  [scicloj.kindly.v4.api :as kindly]
  [scicloj.metamorph.ml.rdatasets :as rdatasets]
  [aerial.hanami.templates :as ht]
  [clojure.test :refer [deftest is]]))


(def
 v2_l42
 (comment [aerial.hanami.templates :as ht] [clojure.string :as str]))


(def
 v4_l59
 (->
  (rdatasets/datasets-iris)
  (tc/random 10 {:seed 1})
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 20,
    :=mark-opacity 0.6})))


(def
 v6_l101
 (def
  example1
  (->
   (rdatasets/datasets-iris)
   (tc/random 10 {:seed 1})
   (plotly/layer-point
    {:=x :sepal-width,
     :=y :sepal-length,
     :=color :species,
     :=mark-size 20,
     :=mark-opacity 0.6}))))


(def v7_l111 (kind/pprint example1))


(deftest
 t8_l113
 (is
  ((fn*
    [p1__86401#]
    (->
     p1__86401#
     :aerial.hanami.templates/defaults
     :=dataset
     tc/row-count
     (= 10)))
   v7_l111)))


(deftest
 t9_l119
 (is
  ((fn*
    [p1__86402#]
    (-> p1__86402# meta :kindly/kind (= :kind/pprint)))
   v7_l111)))


(def v11_l132 (meta example1))


(def v12_l134 (:kindly/f example1))


(def v14_l145 (-> example1 plotly/plot kind/pprint))


(def v16_l152 (-> example1 plotly/plot meta))


(def
 v18_l166
 (-> example1 plotly/plot (assoc-in [:layout :plot_bgcolor] "#eeeedd")))


(def
 v20_l176
 (->
  example1
  plotly/plot
  (assoc-in [:layout :yaxis :scaleanchor] :x)
  (assoc-in [:layout :yaxis :scaleratio] 0.25)))


(def
 v22_l191
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 20})))


(deftest
 t24_l199
 (is
  ((fn*
    [p1__86403#]
    (= (-> p1__86403# plotly/plot :data first :type) "scatter"))
   v22_l191)))


(def
 v26_l203
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=color-type :nominal,
    :=mark-size 20})))


(def
 v28_l215
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot {:=x :cyl, :=y :disp})))


(deftest
 t30_l221
 (is
  ((fn*
    [p1__86404#]
    (= (-> p1__86404# plotly/plot :data first :type) "box"))
   v28_l215)))


(def
 v32_l225
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp})))


(def
 v34_l232
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp, :=box-visible true})))


(deftest
 t36_l239
 (is
  ((fn*
    [p1__86405#]
    (=
     (->
      p1__86405#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=box-visible)
     true))
   v34_l232)))


(def
 v38_l243
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__86406#] (-> p1__86406# :disp tcc/sum))})
  (tc/order-by [:cyl])
  (plotly/layer-line
   {:=x :cyl, :=mark-fill :tozeroy, :=y :total-disp})))


(deftest
 t40_l253
 (is
  ((fn*
    [p1__86407#]
    (=
     (->
      p1__86407#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=mark-fill)
     :tozeroy))
   v38_l243)))


(def
 v42_l257
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__86408#] (-> p1__86408# :disp tcc/sum))})
  (plotly/layer-bar {:=x :cyl, :=y :total-disp})))


(deftest
 t44_l265
 (is
  ((fn*
    [p1__86409#]
    (= (-> p1__86409# plotly/plot :data first :type) "bar"))
   v42_l257)))


(def
 v45_l267
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__86410#] (-> p1__86410# :disp tcc/sum))})
  (tc/add-column :bar-width 0.5)
  (plotly/layer-bar
   {:=x :cyl, :=bar-width :bar-width, :=y :total-disp})))


(deftest
 t47_l277
 (is
  ((fn*
    [p1__86411#]
    (=
     (->
      p1__86411#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=bar-width)
     :bar-width))
   v45_l267)))


(def
 v49_l281
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-text
   {:=x :mpg, :=y :disp, :=text :cyl, :=mark-size 20})))


(deftest
 t51_l289
 (is
  ((fn*
    [p1__86412#]
    (and
     (= (-> p1__86412# plotly/plot :data first :type) "scatter")
     (= (-> p1__86412# plotly/plot :data first :mode) :text)))
   v49_l281)))


(def
 v52_l292
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-text
   {:=x :mpg,
    :=y :disp,
    :=text :cyl,
    :=textfont
    {:family "Courier New, monospace", :size 16, :color :purple},
    :=mark-size 20})))


(deftest
 t54_l303
 (is
  ((fn*
    [p1__86413#]
    (=
     (->
      p1__86413#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=textfont)
     {:family "Courier New, monospace", :size 16, :color :purple}))
   v52_l292)))


(def
 v56_l312
 (->
  {:x (range 5),
   :y (range 5),
   :z (for [i (range 5)] (for [j (range 5)] (+ i j)))}
  tc/dataset
  (plotly/layer-heatmap {:=colorscale :Viridis})))


(deftest
 t58_l321
 (is
  ((fn*
    [p1__86414#]
    (= (-> p1__86414# plotly/plot :data first :type) "heatmap"))
   v56_l312)))


(def
 v60_l325
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-segment
   {:=x0 :sepal-width,
    :=y0 :sepal-length,
    :=x1 :petal-width,
    :=y1 :petal-length,
    :=mark-opacity 0.4,
    :=mark-size 3,
    :=color :species})))


(deftest
 t62_l336
 (is
  ((fn*
    [p1__86415#]
    (and
     (= (-> p1__86415# plotly/plot :data first :type) "scatter")
     (= (-> p1__86415# plotly/plot :data first :mode) :lines)))
   v60_l325)))


(def
 v64_l341
 (->
  {:x (range 10)}
  tc/dataset
  (plotly/layer-point
   {:=x :x,
    :=y :x,
    :=mark-size (range 15 65 5),
    :=mark-color
    ["#bebada"
     "#fdb462"
     "#fb8072"
     "#d9d9d9"
     "#bc80bd"
     "#b3de69"
     "#8dd3c7"
     "#80b1d3"
     "#fccde5"
     "#ffffb3"]})))


(def
 v65_l349
 (->
  {:ABCD (range 1 11),
   :EFGH [5 2.5 5 7.5 5 2.5 7.5 4.5 5.5 5],
   :IJKL [:A :A :A :A :A :B :B :B :B :B],
   :MNOP [:C :D :C :D :C :D :C :D :C :D]}
  tc/dataset
  (plotly/base {:=title "IJKLMNOP"})
  (plotly/layer-point
   {:=x :ABCD, :=y :EFGH, :=color :IJKL, :=size :MNOP, :=name "QRST1"})
  (plotly/layer-line
   {:=title "IJKL MNOP",
    :=x :ABCD,
    :=y :ABCD,
    :=name "QRST2",
    :=mark-color "magenta",
    :=mark-size 20,
    :=mark-opacity 0.2})))


(def
 v67_l374
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86416#] (-> p1__86416# :variable (= "unemploy"))))
  (plotly/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(def
 v69_l386
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=color :species,
    :=height 600,
    :=width 600})))


(deftest
 t71_l393
 (is
  ((fn*
    [p1__86417#]
    (= (-> p1__86417# plotly/plot :data first :type) :splom))
   v69_l386)))


(def
 v73_l399
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86418#] (-> p1__86418# :variable (= "unemploy"))))
  (plotly/layer-point
   {:=x :date,
    :=y :value,
    :=mark-color "green",
    :=mark-size 20,
    :=mark-opacity 0.5})
  (plotly/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(def
 v75_l413
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86419#] (-> p1__86419# :variable (= "unemploy"))))
  (plotly/base {:=x :date, :=y :value})
  (plotly/layer-point
   {:=mark-color "green", :=mark-size 20, :=mark-opacity 0.5})
  (plotly/layer-line {:=mark-color "purple"})))


(deftest
 t77_l423
 (is
  ((fn*
    [p1__86420#]
    (=
     (-> p1__86420# :aerial.hanami.templates/defaults :=layers count)
     2))
   v75_l413)))


(def
 v79_l427
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86421#] (-> p1__86421# :variable (= "unemploy"))))
  (plotly/base {:=x :date, :=y :value})
  (plotly/layer-point
   {:=mark-color "green",
    :=mark-size 20,
    :=mark-opacity 0.5,
    :=name "points"})
  (plotly/layer-line {:=mark-color "purple", :=name "line"})))


(def
 v81_l451
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86422#] (-> p1__86422# :variable (= "unemploy"))))
  (plotly/base {:=x :date, :=y :value})
  (plotly/layer-line {:=mark-color "purple"})
  (plotly/update-data tc/random 5)
  (plotly/layer-point
   {:=mark-color "green", :=mark-size 15, :=mark-opacity 0.5})))


(deftest
 t83_l462
 (is
  ((fn*
    [p1__86423#]
    (=
     (-> p1__86423# :aerial.hanami.templates/defaults :=layers count)
     2))
   v81_l451)))


(def
 v85_l466
 (->
  (tc/dataset {:x (range 4), :y [1 2 5 9]})
  tc/dataset
  (tc/sq :y :x)
  (plotly/layer-point {:=mark-size 20})
  (plotly/layer-line
   {:=dataset (tc/dataset {:x [0 3], :y [1 10]}), :=mark-size 5})))


(def
 v87_l481
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth {:=mark-color "orange", :=name "Predicted"})))


(deftest
 t89_l490
 (is
  ((fn*
    [p1__86424#]
    (and
     (=
      (-> p1__86424# :aerial.hanami.templates/defaults :=layers count)
      2)
     (= (-> p1__86424# plotly/plot :data count) 2)))
   v87_l481)))


(def
 v91_l498
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=predictors [:petal-width :petal-length],
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def
 v93_l513
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=design-matrix
    [[:sepal-width '(identity :sepal-width)]
     [:sepal-width-2 '(* :sepal-width :sepal-width)]],
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def
 v95_l526
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=design-matrix
    [[:sepal-width '(identity :sepal-width)]
     [:sepal-width-2 '(* :sepal-width :sepal-width)]
     [:sepal-width-3 '(* :sepal-width :sepal-width :sepal-width)]],
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def v97_l543 (require 'scicloj.ml.tribuo))


(def
 v98_l545
 (def
  regression-tree-options
  {:model-type :scicloj.ml.tribuo/regression,
   :tribuo-components
   [{:name "cart",
     :type "org.tribuo.regression.rtree.CARTRegressionTrainer",
     :properties
     {:maxDepth "8",
      :fractionFeaturesInSplit "1.0",
      :seed "12345",
      :impurity "mse"}}
    {:name "mse",
     :type "org.tribuo.regression.rtree.impurity.MeanSquaredError"}],
   :tribuo-trainer-name "cart"}))


(def
 v99_l557
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=model-options regression-tree-options,
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def
 v101_l570
 (defonce
  tips
  (->
   "https://raw.githubusercontent.com/plotly/datasets/master/tips.csv"
   (tc/dataset {:key-fn keyword}))))


(def
 v102_l574
 (->
  tips
  (tc/split :holdout {:seed 1})
  (plotly/base {:=x :total_bill, :=y :tip})
  (plotly/layer-point {:=color :$split-name})
  (plotly/update-data
   (fn
    [ds]
    (->
     ds
     (tc/select-rows
      (fn* [p1__86425#] (-> p1__86425# :$split-name (= :train)))))))
  (plotly/layer-smooth
   {:=model-options regression-tree-options,
    :=name "prediction",
    :=mark-color "purple"})))


(def
 v104_l594
 (->
  (rdatasets/datasets-iris)
  (plotly/base
   {:=title "dummy",
    :=color :species,
    :=x :sepal-width,
    :=y :sepal-length})
  plotly/layer-point
  plotly/layer-smooth))


(deftest
 t106_l603
 (is
  ((fn* [p1__86426#] (= (-> p1__86426# plotly/plot :data count) 6))
   v104_l594)))


(def
 v108_l611
 (->
  (rdatasets/datasets-iris)
  (plotly/base
   {:=title "dummy",
    :=color :species,
    :=group [],
    :=x :sepal-width,
    :=y :sepal-length})
  plotly/layer-point
  plotly/layer-smooth))


(def
 v110_l623
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=title "dummy", :=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=color :species})
  (plotly/layer-smooth {:=name "Predicted", :=mark-color "blue"})))


(deftest
 t112_l632
 (is
  ((fn* [p1__86427#] (= (-> p1__86427# plotly/plot :data count) 4))
   v110_l623)))


(def
 v114_l644
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86428#] (-> p1__86428# :variable (= "unemploy"))))
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
 v116_l656
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86429#] (-> p1__86429# :variable (= "unemploy"))))
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
    [p1__86430#]
    (datetime/long-temporal-field :years (:date p1__86430#))))
  (tc/add-column
   :month
   (fn*
    [p1__86431#]
    (datetime/long-temporal-field :months (:date p1__86431#))))
  (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
  (print/print-range 6)))


(def
 v118_l675
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__86432#] (-> p1__86432# :variable (= "unemploy"))))
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
    [p1__86433#]
    (datetime/long-temporal-field :years (:date p1__86433#))))
  (tc/add-column
   :month
   (fn*
    [p1__86434#]
    (datetime/long-temporal-field :months (:date p1__86434#))))
  (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
  (plotly/base {:=x :date, :=y :value})
  (plotly/layer-smooth
   {:=color :relative-time,
    :=mark-size 15,
    :=group [],
    :=predictors [:yearmonth]})
  (plotly/update-data
   (fn
    [dataset]
    (->
     dataset
     (tc/select-rows (fn [row] (-> row :relative-time (= "Past")))))))
  (plotly/layer-line
   {:=mark-color "purple", :=mark-size 3, :=name "Actual"})))


(def
 v120_l706
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width})))


(deftest
 t122_l710
 (is
  ((fn*
    [p1__86435#]
    (= (-> p1__86435# plotly/plot :data first :type) "bar"))
   v120_l706)))


(def
 v123_l712
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width, :=histogram-nbins 30})))


(def
 v124_l716
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram
   {:=x :sepal-width, :=color :species, :=mark-opacity 0.5})))


(def
 v126_l725
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d
   {:=x :sepal-width, :=y :sepal-length, :=histogram-nbins 15})))


(deftest
 t128_l731
 (is
  ((fn*
    [p1__86436#]
    (= (-> p1__86436# plotly/plot :data first :type) "heatmap"))
   v126_l725)))


(def
 v130_l739
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width})))


(deftest
 t132_l743
 (is
  ((fn*
    [p1__86437#]
    (= (-> p1__86437# plotly/plot :data first :type) "scatter"))
   v130_l739)))


(def
 v133_l745
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 0.05})))


(deftest
 t135_l750
 (is
  ((fn*
    [p1__86438#]
    (=
     (->
      p1__86438#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=density-bandwidth)
     0.05))
   v133_l745)))


(def
 v136_l752
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 1})))


(def
 v137_l756
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=color :species})))


(deftest
 t139_l761
 (is
  ((fn* [p1__86439#] (= (-> p1__86439# plotly/plot :data count) 3))
   v137_l756)))


(def
 v141_l770
 (->
  {:lat [45.5 43.4 49.13 51.1 53.34 45.24 44.64 48.25 49.89 50.45],
   :lon
   [-73.57
    -79.24
    -123.06
    -114.1
    -113.28
    -75.43
    -63.57
    -123.21
    -97.13
    -104.6],
   :text
   ["Montreal"
    "Toronto"
    "Vancouver"
    "Calgary"
    "Edmonton"
    "Ottawa"
    "Halifax"
    "Victoria"
    "Winnepeg"
    "Regina"]}
  tc/dataset
  (plotly/base {:=coordinates :geo, :=lat :lat, :=lon :lon})
  (plotly/layer-point
   {:=mark-opacity 0.8,
    :=mark-color
    ["#bebada"
     "#fdb462"
     "#fb8072"
     "#d9d9d9"
     "#bc80bd"
     "#b3de69"
     "#8dd3c7"
     "#80b1d3"
     "#fccde5"
     "#ffffb3"],
    :=mark-size 20,
    :=name "Canadian cities"})
  (plotly/layer-text
   {:=text :text, :=textfont {:size 7, :color :purple}})
  plotly/plot
  (assoc-in
   [:layout :geo]
   {:scope "north america",
    :resolution 10,
    :lonaxis {:range [-130 -55]},
    :lataxis {:range [40 60]},
    :countrywidth 1.5,
    :showland true,
    :showlakes true,
    :showrivers true})))


(def
 v143_l801
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :petal-width,
    :=coordinates :3d})))


(deftest
 t145_l809
 (is
  ((fn*
    [p1__86440#]
    (=
     (->
      p1__86440#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=coordinates)
     :3d))
   v143_l801)))


(def
 v146_l811
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :species,
    :=coordinates :3d})))


(def
 v148_l822
 (->
  {:z
   (for [i (range 20)] (for [j (range 20)] (Math/sin (/ (* i j) 10))))}
  tc/dataset
  (plotly/layer-surface {:=colorscale :Viridis})))


(deftest
 t150_l829
 (is
  ((fn*
    [p1__86441#]
    (= (-> p1__86441# plotly/plot :data first :type) "surface"))
   v148_l822)))


(def
 v152_l835
 (def
  rain-data
  (tc/dataset
   {:month
    [:Jan :Feb :Mar :Apr :May :Jun :Jul :Aug :Sep :Oct :Nov :Dec],
    :rain (repeatedly (fn* [] (rand-int 200)))})))


(def
 v153_l842
 (->
  rain-data
  (plotly/layer-bar
   {:=r :rain,
    :=theta :month,
    :=coordinates :polar,
    :=mark-size 20,
    :=mark-opacity 0.6})))


(deftest
 t155_l851
 (is
  ((fn*
    [p1__86442#]
    (=
     (->
      p1__86442#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=coordinates)
     :polar))
   v153_l842)))


(def
 v157_l856
 (->
  rain-data
  (plotly/base {})
  (plotly/layer-bar
   {:=r :rain,
    :=theta :month,
    :=coordinates :polar,
    :=mark-size 20,
    :=mark-opacity 0.6})
  plotly/plot
  (assoc-in
   [:layout :polar]
   {:angularaxis
    {:tickfont {:size 16},
     :rotation 90,
     :direction "counterclockwise"},
    :sector [0 180]})))


(def
 v159_l874
 (let
  [n 50]
  (->
   {:r (->> (repeatedly n (fn* [] (- (rand) 0.5))) (reductions +)),
    :theta
    (->>
     (repeatedly n (fn* [] (* 10 (rand))))
     (reductions +)
     (map (fn* [p1__86443#] (rem p1__86443# 360)))),
    :color (range n)}
   tc/dataset
   (plotly/layer-point
    {:=r :r,
     :=theta :theta,
     :=coordinates :polar,
     :=mark-size 10,
     :=mark-opacity 0.6})
   (plotly/layer-line
    {:=r :r,
     :=theta :theta,
     :=coordinates :polar,
     :=mark-size 3,
     :=mark-opacity 0.6}))))


(def
 v161_l902
 (def
  example-to-debug
  (->
   (rdatasets/datasets-iris)
   (tc/random 10 {:seed 1})
   (plotly/layer-point
    {:=x :sepal-width, :=y :sepal-length, :=color :species}))))


(def v162_l909 (-> example-to-debug plotly/dag))


(def
 v164_l916
 (-> example-to-debug (plotly/debug :=layers) kind/pprint))


(def
 v166_l922
 (-> example-to-debug (plotly/debug :=traces) kind/pprint))


(def
 v168_l928
 (->
  example-to-debug
  (plotly/debug {:layers :=layers, :traces :=traces})
  kind/pprint))


(def v170_l937 (-> example-to-debug (plotly/debug :=background)))


(def v172_l945 (-> example-to-debug plotly/plot kind/pprint))
