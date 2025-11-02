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
    [p1__94322#]
    (->
     p1__94322#
     :aerial.hanami.templates/defaults
     :=dataset
     tc/row-count
     (= 10)))
   v7_l111)))


(deftest
 t9_l119
 (is
  ((fn*
    [p1__94323#]
    (-> p1__94323# meta :kindly/kind (= :kind/pprint)))
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


(def
 v24_l200
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=color-type :nominal,
    :=mark-size 20})))


(def
 v26_l212
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot {:=x :cyl, :=y :disp})))


(def
 v28_l219
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp})))


(def
 v30_l226
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp, :=box-visible true})))


(def
 v32_l234
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__94324#] (-> p1__94324# :disp tcc/sum))})
  (tc/order-by [:cyl])
  (plotly/layer-line
   {:=x :cyl, :=mark-fill :tozeroy, :=y :total-disp})))


(def
 v34_l245
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__94325#] (-> p1__94325# :disp tcc/sum))})
  (plotly/layer-bar {:=x :cyl, :=y :total-disp})))


(def
 v35_l252
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__94326#] (-> p1__94326# :disp tcc/sum))})
  (tc/add-column :bar-width 0.5)
  (plotly/layer-bar
   {:=x :cyl, :=bar-width :bar-width, :=y :total-disp})))


(def
 v37_l263
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-text
   {:=x :mpg, :=y :disp, :=text :cyl, :=mark-size 20})))


(def
 v38_l270
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-text
   {:=x :mpg,
    :=y :disp,
    :=text :cyl,
    :=textfont
    {:family "Courier New, monospace", :size 16, :color :purple},
    :=mark-size 20})))


(def
 v40_l284
 (->
  {:x (range 5),
   :y (range 5),
   :z (for [i (range 5)] (for [j (range 5)] (+ i j)))}
  tc/dataset
  (plotly/layer-heatmap {:=colorscale :Viridis})))


(def
 v42_l294
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


(def
 v44_l306
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
 v45_l314
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
 v47_l339
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94327#] (-> p1__94327# :variable (= "unemploy"))))
  (plotly/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(def
 v49_l351
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=color :species,
    :=height 600,
    :=width 600})))


(def
 v51_l361
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94328#] (-> p1__94328# :variable (= "unemploy"))))
  (plotly/layer-point
   {:=x :date,
    :=y :value,
    :=mark-color "green",
    :=mark-size 20,
    :=mark-opacity 0.5})
  (plotly/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(def
 v53_l375
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94329#] (-> p1__94329# :variable (= "unemploy"))))
  (plotly/base {:=x :date, :=y :value})
  (plotly/layer-point
   {:=mark-color "green", :=mark-size 20, :=mark-opacity 0.5})
  (plotly/layer-line {:=mark-color "purple"})))


(def
 v55_l386
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94330#] (-> p1__94330# :variable (= "unemploy"))))
  (plotly/base {:=x :date, :=y :value})
  (plotly/layer-point
   {:=mark-color "green",
    :=mark-size 20,
    :=mark-opacity 0.5,
    :=name "points"})
  (plotly/layer-line {:=mark-color "purple", :=name "line"})))


(def
 v57_l410
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94331#] (-> p1__94331# :variable (= "unemploy"))))
  (plotly/base {:=x :date, :=y :value})
  (plotly/layer-line {:=mark-color "purple"})
  (plotly/update-data tc/random 5)
  (plotly/layer-point
   {:=mark-color "green", :=mark-size 15, :=mark-opacity 0.5})))


(def
 v59_l422
 (->
  (tc/dataset {:x (range 4), :y [1 2 5 9]})
  tc/dataset
  (tc/sq :y :x)
  (plotly/layer-point {:=mark-size 20})
  (plotly/layer-line
   {:=dataset (tc/dataset {:x [0 3], :y [1 10]}), :=mark-size 5})))


(def
 v61_l437
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth {:=mark-color "orange", :=name "Predicted"})))


(def
 v63_l450
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=predictors [:petal-width :petal-length],
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def
 v65_l465
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
 v67_l478
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


(def v69_l495 (require 'scicloj.ml.tribuo))


(def
 v70_l497
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
 v71_l509
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=model-options regression-tree-options,
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def
 v73_l522
 (defonce
  tips
  (->
   "https://raw.githubusercontent.com/plotly/datasets/master/tips.csv"
   (tc/dataset {:key-fn keyword}))))


(def
 v74_l526
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
      (fn* [p1__94332#] (-> p1__94332# :$split-name (= :train)))))))
  (plotly/layer-smooth
   {:=model-options regression-tree-options,
    :=name "prediction",
    :=mark-color "purple"})))


(def
 v76_l546
 (->
  (rdatasets/datasets-iris)
  (plotly/base
   {:=title "dummy",
    :=color :species,
    :=x :sepal-width,
    :=y :sepal-length})
  plotly/layer-point
  plotly/layer-smooth))


(def
 v78_l560
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
 v80_l572
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=title "dummy", :=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=color :species})
  (plotly/layer-smooth {:=name "Predicted", :=mark-color "blue"})))


(def
 v82_l590
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94333#] (-> p1__94333# :variable (= "unemploy"))))
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
 v84_l602
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94334#] (-> p1__94334# :variable (= "unemploy"))))
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
    [p1__94335#]
    (datetime/long-temporal-field :years (:date p1__94335#))))
  (tc/add-column
   :month
   (fn*
    [p1__94336#]
    (datetime/long-temporal-field :months (:date p1__94336#))))
  (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
  (print/print-range 6)))


(def
 v86_l621
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__94337#] (-> p1__94337# :variable (= "unemploy"))))
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
    [p1__94338#]
    (datetime/long-temporal-field :years (:date p1__94338#))))
  (tc/add-column
   :month
   (fn*
    [p1__94339#]
    (datetime/long-temporal-field :months (:date p1__94339#))))
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
 v88_l652
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width})))


(def
 v89_l655
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width, :=histogram-nbins 30})))


(def
 v90_l659
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram
   {:=x :sepal-width, :=color :species, :=mark-opacity 0.5})))


(def
 v92_l668
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d
   {:=x :sepal-width, :=y :sepal-length, :=histogram-nbins 15})))


(def
 v94_l679
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width})))


(def
 v95_l682
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 0.05})))


(def
 v96_l686
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 1})))


(def
 v97_l690
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=color :species})))


(def
 v99_l701
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
 v101_l732
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :petal-width,
    :=coordinates :3d})))


(def
 v102_l739
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :species,
    :=coordinates :3d})))


(def
 v104_l750
 (->
  {:z
   (for [i (range 20)] (for [j (range 20)] (Math/sin (/ (* i j) 10))))}
  tc/dataset
  (plotly/layer-surface {:=colorscale :Viridis})))


(def
 v106_l760
 (def
  rain-data
  (tc/dataset
   {:month
    [:Jan :Feb :Mar :Apr :May :Jun :Jul :Aug :Sep :Oct :Nov :Dec],
    :rain (repeatedly (fn* [] (rand-int 200)))})))


(def
 v107_l767
 (->
  rain-data
  (plotly/layer-bar
   {:=r :rain,
    :=theta :month,
    :=coordinates :polar,
    :=mark-size 20,
    :=mark-opacity 0.6})))


(def
 v109_l778
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
 v111_l796
 (let
  [n 50]
  (->
   {:r (->> (repeatedly n (fn* [] (- (rand) 0.5))) (reductions +)),
    :theta
    (->>
     (repeatedly n (fn* [] (* 10 (rand))))
     (reductions +)
     (map (fn* [p1__94340#] (rem p1__94340# 360)))),
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
 v113_l824
 (def
  example-to-debug
  (->
   (rdatasets/datasets-iris)
   (tc/random 10 {:seed 1})
   (plotly/layer-point
    {:=x :sepal-width, :=y :sepal-length, :=color :species}))))


(def v114_l831 (-> example-to-debug plotly/dag))


(def
 v116_l838
 (-> example-to-debug (plotly/debug :=layers) kind/pprint))


(def
 v118_l844
 (-> example-to-debug (plotly/debug :=traces) kind/pprint))


(def
 v120_l850
 (->
  example-to-debug
  (plotly/debug {:layers :=layers, :traces :=traces})
  kind/pprint))


(def v122_l859 (-> example-to-debug (plotly/debug :=background)))


(def v124_l867 (-> example-to-debug plotly/plot kind/pprint))
