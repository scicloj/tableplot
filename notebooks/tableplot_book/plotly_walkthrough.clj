;; # Plotly walkthrough 👣

;; Tableplot offers a Clojure API for creating [Plotly.js](https://plotly.com/javascript/) plots through layered pipelines.

;; The API uses [Hanami templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations) but is completely separate from the classical Hanami templates and parameters.

;; Here, we provide a walkthrough of that API.

;; See also the more detailed [Tableplot Plotly reference](tableplot_book.plotly_reference.html) 📖.
;; You might find the official [Plotly.js reference](https://plotly.com/javascript/) helpful. (Tip: rotate narrow devices.)
;; There are additional examples in [Intro to data visualization with Tableplot](https://scicloj.github.io/noj/noj_book.tableplot_datavis_intro.html) in the [Noj book](https://scicloj.github.io/noj).

;; ## Setup
;; For this tutorial, we require:

;; * The Tableplot plotly API namespace

;; * [Tablecloth](https://scicloj.github.io/tablecloth/) for dataset processing

;; * the [datetime namespace](https://cnuernber.github.io/dtype-next/tech.v3.datatype.datetime.html) of [dtype-next](https://github.com/cnuernber/dtype-next)

;; * the [print namespace](https://techascent.github.io/tech.ml.dataset/tech.v3.dataset.print.html) of [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) for customized dataset printing

;; * [Kindly](https://scicloj.github.io/kindly-noted/) (to specify how certain values should be visualized)

;; * the datasets defined in the [Datasets chapter](./tableplot_book.datasets.html)

;; * a few other namespaces used in particular examples.

(ns tableplot-book.plotly-walkthrough
  (:require [scicloj.tableplot.v1.plotly :as plotly]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.dataset.print :as print]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.metamorph.ml.rdatasets :as rdatasets]))
^:kindly/hide-code
(comment
  ;; These were in the `require` but aren't used below:
  [aerial.hanami.templates :as ht]
  [clojure.string :as str]
)

;; ## Basic usage

;; Plotly plots are created by passing datasets to a pipeline
;; of layer functions.

;; Additional parameters to the functions are passed as maps.
;; Map keys begin with `=` (e.g., `:=color`).

;; For example, let us plot a scatterplot (a layer of points)
;; of 10 random items from the Iris dataset.

(-> (rdatasets/datasets-iris)
    (tc/random 10 {:seed 1})
    (plotly/layer-point
     {:=x :sepal-width
      :=y :sepal-length
      :=color :species
      :=mark-size 20
      :=mark-opacity 0.6}))


;; ## Processing overview

;; For basic use of Tableplot with a tool such as Clay, it's not necessary to
;; understand the process leading to display of a plot. Knowing more
;; might be helpful for debugging and advanced customizations, though.
;; This section and the following ones provide more information about the
;; process:

;; 1. The parameter map passed to a function such as `plotly/layer-point` 
;; will typically contain Plotly-specific [Hanami substitution
;; keys](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations).
;; 2. The values of those keys are automatically be combined with default values
;; calculated for other Plotly-specific keys.
;; 3. The preceding step results in an EDN map that specifies a Plotly.js plot.
;; 4. The EDN-format plot specification is automatically transformed into a [Plotly
;; JSON](https://plotly.com/chart-studio-help/json-chart-schema)
;; specification.
;; 5. The JSON specification is automatically used to display the plot.

;; The reason Clay knows what to do with the maps at each step is
;; because previous steps add appropriate [Kindly](https://scicloj.github.io/kindly-noted/) 
;; meta annotations to the maps.


;; ## Templates and parameters

;; Technically, the parameter maps contain [Hanami substitution keys](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations),
;; which means they are processed by a [simple set of rules](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#basic-transformation-rules),
;; but you do not need to understand what this means yet.

;; The layer functions return a Hanami template. Let us print the resulting
;; structure of the previous plot.

(def example1
  (-> (rdatasets/datasets-iris)
      (tc/random 10 {:seed 1})
      (plotly/layer-point
       {:=x :sepal-width
        :=y :sepal-length
        :=color :species
        :=mark-size 20
        :=mark-opacity 0.6})))

(kind/pprint example1)

;; This template has all the necessary knowledge, including the substitution
;; keys, to turn into a plot. This happens when your visual tool (e.g., Clay)
;; displays the plot. The tool knows what to do thanks to the Kindly metadata
;; and a special function attached to the plot.  For example, the metadata
;; lets Clay know that the template should be transformed into a
;; specification with template keys and values replaced with what Plotly.js
;; needs.

(meta example1)

(:kindly/f example1)

;; ## Realizing the plot and further customization

;; If you wish to see the resulting EDN plot specification before displaying it
;; as a plot, you can use the `plot` function.  You can also use this
;; specification for customizations that might not be supported by the
;; Plotly Hanami keys mentioned above.

;; In this case, it generates a Plotly.js plot:

(-> example1
    plotly/plot
    kind/pprint)

;; It is annotated as `kind/plotly`, so that visual tools know how to
;; render it.

(-> example1
    plotly/plot
    meta)

;; You can manipulate the resulting the Plotly EDN specification with
;; arbitrary Clojure functions. In Clay, by default this will then cause
;; the modified EDN to be used to generate a plot.

;; As a simple illustration, let us change the background colour this way.  
;; We can use `assoc-in` to modify the value of `:plot_bgcolor` nested near
;; the end of the `example1` map displayed above.
;; (One could also do this using the `:=background` Hanami key.)

(-> example1
    plotly/plot
    (assoc-in [:layout :plot_bgcolor] "#eeeedd"))

;; Manipulating the Plotly EDN allows customizations that might not yet be supported by the
;; Tableplot Plotly API. The next example compresses distances in the `y` direction,
;; using a [capability of
;; Plotly.js](https://plotly.com/javascript/reference/layout/xaxis/#layout-xaxis-scaleanchor)
;; that isn't directly supported using Tableplot's Hanami keys.

(-> example1
    plotly/plot
    (assoc-in [:layout :yaxis :scaleanchor] :x)
    (assoc-in [:layout :yaxis :scaleratio] 0.25))


;; ## Field type inference

;; Tableplot infers the type of relevant fields from the data.

;; The example above was colored as it were since `:species`
;; column was nominal, so it was assigned distinct colours.

;; In the following example, the coloring is by a quantitative
;; column, so a color gradient is used:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=mark-size 20}))

;; We can override the inferred types and thus affect the generated plot:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=color-type :nominal
      :=mark-size 20}))

;; ## More examples

;; ### Boxplot

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-boxplot
     {:=x :cyl
      :=y :disp}))

;; ### Area chart

(-> (rdatasets/datasets-mtcars)
    (tc/group-by [:cyl])
    (tc/aggregate {:total-disp #(-> % :disp tcc/sum)})
    (tc/order-by [:cyl])
    (plotly/layer-line
     {:=x :cyl
      :=mark-fill :tozeroy
      :=y :total-disp}))

;; ### Bar chart

(-> (rdatasets/datasets-mtcars)
    (tc/group-by [:cyl])
    (tc/aggregate {:total-disp #(-> % :disp tcc/sum)})
    (plotly/layer-bar
     {:=x :cyl
      :=y :total-disp}))

(-> (rdatasets/datasets-mtcars)
    (tc/group-by [:cyl])
    (tc/aggregate {:total-disp #(-> % :disp tcc/sum)})
    (tc/add-column :bar-width 0.5)
    (plotly/layer-bar
     {:=x :cyl
      :=bar-width :bar-width
      :=y :total-disp}))

;; ### Text

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-text
     {:=x :mpg
      :=y :disp
      :=text :cyl
      :=mark-size 20}))

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-text
     {:=x :mpg
      :=y :disp
      :=text :cyl
      :=textfont {:family "Courier New, monospace"
                  :size 16
                  :color :purple}
      :=mark-size 20}))

;; ### Segment plot

(-> (rdatasets/datasets-iris)
    (plotly/layer-segment
     {:=x0 :sepal-width
      :=y0 :sepal-length
      :=x1 :petal-width
      :=y1 :petal-length
      :=mark-opacity 0.4
      :=mark-size 3
      :=color :species}))

;; ## Varying color and size

(-> {:x (range 10)}
    tc/dataset
    (plotly/layer-point {:=x :x
                         :=y :x
                         :=mark-size (range 15 65 5)
                         :=mark-color ["#bebada", "#fdb462", "#fb8072", "#d9d9d9", "#bc80bd",
                                       "#b3de69", "#8dd3c7", "#80b1d3", "#fccde5", "#ffffb3"]}))

(-> {:ABCD (range 1 11)
     :EFGH [5 2.5 5 7.5 5 2.5 7.5 4.5 5.5 5]
     :IJKL [:A :A :A :A :A :B :B :B :B :B]
     :MNOP [:C :D :C :D :C :D :C :D :C :D]}
    tc/dataset
    (plotly/base {:=title "IJKLMNOP"})
    (plotly/layer-point {:=x :ABCD
                         :=y :EFGH
                         :=color :IJKL
                         :=size :MNOP
                         :=name "QRST1"})
    (plotly/layer-line
     {:=title "IJKL MNOP"
      :=x :ABCD
      :=y :ABCD
      :=name "QRST2"
      :=mark-color "magenta"
      :=mark-size 20
      :=mark-opacity 0.2}))

;; ## Time series

;; Date and time fields are handle appropriately.
;; Let us, for example, draw the time series of unemployment counts.

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/layer-line
     {:=x :date
      :=y :value
      :=mark-color "purple"}))

;; ## Multiple layers

;; We can draw more than one layer:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/layer-point {:=x :date
                         :=y :value
                         :=mark-color "green"
                         :=mark-size 20
                         :=mark-opacity 0.5})
    (plotly/layer-line {:=x :date
                        :=y :value
                        :=mark-color "purple"}))

;; We can also use the `base` function for the common parameters
;; across layers:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/base {:=x :date
                  :=y :value})
    (plotly/layer-point {:=mark-color "green"
                         :=mark-size 20
                         :=mark-opacity 0.5})
    (plotly/layer-line {:=mark-color "purple"}))


;; Layers can be named:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/base {:=x :date
                  :=y :value})
    (plotly/layer-point {:=mark-color "green"
                         :=mark-size 20
                         :=mark-opacity 0.5
                         :=name "points"})
    (plotly/layer-line {:=mark-color "purple"
                        :=name "line"}))

;; ## Updating data

;; We can use the `update-data` function to vary the
;; dataset along a plotting pipeline, affecting
;; the layers that follow.

;; This functionality is inspired by [ggbuilder](https://github.com/mjskay/ggbuilder)
;; and [metamorph](https://github.com/scicloj/metamorph).

;; Here, for example, we draw a line,
;; then sample 5 data rows,
;; and draw them as points:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/base {:=x :date
                  :=y :value})
    (plotly/layer-line {:=mark-color "purple"})
    (plotly/update-data tc/random 5)
    (plotly/layer-point {:=mark-color "green"
                         :=mark-size 15
                         :=mark-opacity 0.5}))

;; ## Overriding layer data

(-> (tc/dataset {:x (range 4)
                 :y [1 2 5 9]})
    tc/dataset
    (tc/sq :y :x)
    (plotly/layer-point {:=mark-size 20})
    (plotly/layer-line {:=dataset (tc/dataset {:x [0 3]
                                               :y [1 10]})
                        :=mark-size 5}))

;; ## Smoothing

;; `layer-smooth` is a layer that applies statistical regression methods
;; to the dataset to model it as a smooth shape.
;; It is inspired by ggplot's [geom_smooth](https://ggplot2.tidyverse.org/reference/geom_smooth.html).

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=mark-color "orange"
                          :=name "Predicted"}))

;; By default, the regression is computed with only one predictor variable,
;; which is `:=x`.
;; But this can be overriden using the `:=predictors` key.
;; We may compute a regression with more than one predictor.

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=predictors [:petal-width
                                        :petal-length]
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

;; We can also specify the predictor columns as expressions
;; through the `:=design-matrix` key.
;; Here, we use the design matrix functionality of
;; [Metamorph.ml](https://github.com/scicloj/metamorph.ml).

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=design-matrix [[:sepal-width '(identity :sepal-width)]
                                           [:sepal-width-2 '(* :sepal-width
                                                               :sepal-width)]]
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

;; Inspired by Sami Kallinen's [Heart of Clojure talk](https://2024.heartofclojure.eu/talks/sailing-with-scicloj-a-bayesian-adventure/):

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=design-matrix [[:sepal-width '(identity :sepal-width)]
                                           [:sepal-width-2 '(* :sepal-width
                                                               :sepal-width)]
                                           [:sepal-width-3 '(* :sepal-width
                                                               :sepal-width
                                                               :sepal-width)]]
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

;; One can also provide the regression model details through `:=model-options`
;; and use any regression model and parameters registered by Metamorph.ml.

(require 'scicloj.ml.tribuo)

(def regression-tree-options
  {:model-type :scicloj.ml.tribuo/regression
   :tribuo-components [{:name "cart"
                        :type "org.tribuo.regression.rtree.CARTRegressionTrainer"
                        :properties {:maxDepth "8"
                                     :fractionFeaturesInSplit "1.0"
                                     :seed "12345"
                                     :impurity "mse"}}
                       {:name "mse"
                        :type "org.tribuo.regression.rtree.impurity.MeanSquaredError"}]
   :tribuo-trainer-name "cart"})

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=model-options regression-tree-options
                          :=mark-opacity 0.5
                          :=name "Predicted"}))


;; An example inspired by Plotly's
;; [ML Regressoin in Python](https://plotly.com/python/ml-regression/)
;; example.

(defonce tips
  (-> "https://raw.githubusercontent.com/plotly/datasets/master/tips.csv"
      (tc/dataset {:key-fn keyword})))

(-> tips
    (tc/split :holdout {:seed 1})
    (plotly/base {:=x :total_bill
                  :=y :tip})
    (plotly/layer-point {:=color :$split-name})
    (plotly/update-data (fn [ds]
                          (-> ds
                              (tc/select-rows #(-> % :$split-name (= :train))))))
    (plotly/layer-smooth {:=model-options regression-tree-options
                          :=name "prediction"
                          :=mark-color "purple"}))


;; ## Grouping

;; The regression computed by `layer-smooth`
;; is affected by the inferred grouping of the data.

;; For example, here we recieve three regression lines,
;; each for every species.

(-> (rdatasets/datasets-iris)
    (plotly/base {:=title "dummy"
                  :=color :species
                  :=x :sepal-width
                  :=y :sepal-length})
    plotly/layer-point
    plotly/layer-smooth)

;; This happened because the `:color` field was `:species`,
;; which is of `:nominal` type.

;; But we may override this using the `:group` key.
;; For example, let us avoid grouping:

(-> (rdatasets/datasets-iris)
    (plotly/base {:=title "dummy"
                  :=color :species
                  :=group []
                  :=x :sepal-width
                  :=y :sepal-length})
    plotly/layer-point
    plotly/layer-smooth)

;; Alternatively, we may assign the `:=color` only to the points layer
;; without affecting the smoothing layer.

(-> (rdatasets/datasets-iris)
    (plotly/base {:=title "dummy"
                  :=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=color :species})
    (plotly/layer-smooth {:=name "Predicted"
                          :=mark-color "blue"}))

;; ## Example: out-of-sample predictions

;; Here is a slighly more elaborate example
;; inpired by the London Clojurians [talk](https://www.youtube.com/watch?v=eUFf3-og_-Y)
;; mentioned in the preface.

;; Assume we wish to predict the unemployment rate for 96 months.
;; Let us add those months to our dataset,
;; and mark them as `Future` (considering the original data as `Past`):

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (tc/add-column :relative-time "Past")
    (tc/concat (tc/dataset {:date (-> (rdatasets/ggplot2-economics_long)
                                      :date
                                      last
                                      (datetime/plus-temporal-amount (range 96) :days))
                            :relative-time "Future"}))
    (print/print-range 6))

;; Let us represent our dates as numbers, so that we can use them in linear regression:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (tc/add-column :relative-time "Past")
    (tc/concat (tc/dataset {:date (-> (rdatasets/ggplot2-economics_long)
                                      :date
                                      last
                                      (datetime/plus-temporal-amount (range 96) :months))
                            :relative-time "Future"}))
    (tc/add-column :year #(datetime/long-temporal-field :years (:date %)))
    (tc/add-column :month #(datetime/long-temporal-field :months (:date %)))
    (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
    (print/print-range 6))

;; Let us use the same regression line for the `Past` and `Future` groups.
;; To do this, we avoid grouping by assigning  `[]` to `:=group`.
;; The line is affected only by the past, since in the Future, `:=y` is missing.
;; We use the numerical field `:yearmonth` as the regression predictor,
;; but for plotting, we still use the `:temporal` field `:date`.

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (tc/add-column :relative-time "Past")
    (tc/concat (tc/dataset {:date (-> (rdatasets/ggplot2-economics_long)
                                      :date
                                      last
                                      (datetime/plus-temporal-amount (range 96) :months))
                            :relative-time "Future"}))
    (tc/add-column :year #(datetime/long-temporal-field :years (:date %)))
    (tc/add-column :month #(datetime/long-temporal-field :months (:date %)))
    (tc/map-columns :yearmonth [:year :month] (fn [y m] (+ m (* 12 y))))
    (plotly/base {:=x :date
                  :=y :value})
    (plotly/layer-smooth {:=color :relative-time
                          :=mark-size 15
                          :=group []
                          :=predictors [:yearmonth]})
    ;; Keep only the past for the following layer:
    (plotly/update-data (fn [dataset]
                          (-> dataset
                              (tc/select-rows (fn [row]
                                                (-> row :relative-time (= "Past")))))))
    (plotly/layer-line {:=mark-color "purple"
                        :=mark-size 3
                        :=name "Actual"}))

;; ## Histograms

;; Histograms can also be represented as layers
;; with statistical processing:

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram {:=x :sepal-width}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram {:=x :sepal-width
                             :=histogram-nbins 30}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram {:=x :sepal-width
                             :=color :species
                             :=mark-opacity 0.5}))

;; ## Density

;; (experimental)

;; Density estimates are handled similarly to Histograms:

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width
                           :=density-bandwidth 0.05}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width
                           :=density-bandwidth 1}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width
                           :=color :species}))

;; ## Coordinates
;; (WIP)

;; ### geo

;; Inspired by Plotly's tutorial for [Scatter Plots on Maps in JavaScript](https://plotly.com/javascript/scatter-plots-on-maps/):

(-> {:lat [45.5, 43.4, 49.13, 51.1, 53.34, 45.24,
           44.64, 48.25, 49.89, 50.45]
     :lon [-73.57, -79.24, -123.06, -114.1, -113.28,
           -75.43, -63.57, -123.21, -97.13, -104.6]
     :text ["Montreal", "Toronto", "Vancouver", "Calgary", "Edmonton",
            "Ottawa", "Halifax", "Victoria", "Winnepeg", "Regina"],}
    tc/dataset
    (plotly/base {:=coordinates :geo
                  :=lat :lat
                  :=lon :lon})
    (plotly/layer-point {:=mark-opacity 0.8
                         :=mark-color ["#bebada", "#fdb462", "#fb8072", "#d9d9d9", "#bc80bd",
                                       "#b3de69", "#8dd3c7", "#80b1d3", "#fccde5", "#ffffb3"]
                         :=mark-size 20
                         :=name "Canadian cities"})
    (plotly/layer-text {:=text :text
                        :=textfont {:size 7
                                    :color :purple}})
    plotly/plot
    (assoc-in [:layout :geo]
              {:scope "north america"
               :resolution 10
               :lonaxis {:range [-130 -55]}
               :lataxis {:range [40 60]}
               :countrywidth 1.5
               :showland true
               :showlakes true
               :showrivers true}))

;; ### 3d

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=z :petal-length
                         :=color :petal-width
                         :=coordinates :3d}))


(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=z :petal-length
                         :=color :species
                         :=coordinates :3d}))


;; ### polar

;; Monthly rain amounts - polar bar-chart

(def rain-data
(tc/dataset
 {:month [:Jan :Feb :Mar :Apr
          :May :Jun :Jul :Aug
          :Sep :Oct :Nov :Dec]
  :rain (repeatedly #(rand-int 200))}))

(-> rain-data
(plotly/layer-bar
     {:=r :rain
      :=theta :month
      :=coordinates :polar
      :=mark-size 20
      :=mark-opacity 0.6}))

;; Controlling the polar layout
;; (by manipulating the raw Plotly.js spec):

(-> rain-data
    (plotly/base
     {})
    (plotly/layer-bar
     {:=r :rain
      :=theta :month
      :=coordinates :polar
      :=mark-size 20
      :=mark-opacity 0.6})
    plotly/plot
    (assoc-in [:layout :polar]
              {:angularaxis {:tickfont {:size 16}
                             :rotation 90
                             :direction "counterclockwise"}
               :sector [0 180]}))

;; A polar random walk - polar line-chart

(let [n 50]
  (-> {:r (->> (repeatedly n #(- (rand) 0.5))
               (reductions +))
       :theta (->> (repeatedly n #(* 10 (rand)))
                   (reductions +)
                   (map #(rem % 360)))
       :color (range n)}
      tc/dataset
      (plotly/layer-point
       {:=r :r
        :=theta :theta
        :=coordinates :polar
        :=mark-size 10
        :=mark-opacity 0.6})
      (plotly/layer-line
       {:=r :r
        :=theta :theta
        :=coordinates :polar
        :=mark-size 3
        :=mark-opacity 0.6})))

;; ## Debugging (WIP)

;; ### Viewing the computational dag of substitution keys:

(def example-to-debug
  (-> (rdatasets/datasets-iris)
      (tc/random 10 {:seed 1})
      (plotly/layer-point {:=x :sepal-width
                           :=y :sepal-length
                           :=color :species})))

(-> example-to-debug
    plotly/dag)

;; ### Viewing intermediate values in the computational dag:

;; Layers (tableplot's intermediate data representation)

(-> example-to-debug
    (plotly/debug :=layers)
    kind/pprint)

;; Traces (part of the Plotly spec)

(-> example-to-debug
    (plotly/debug :=traces)
    kind/pprint)

;; Both

(-> example-to-debug
    (plotly/debug {:layers :=layers
                   :traces :=traces})
    kind/pprint)

;; ## Coming soon

;; ### Facets
;; (coming soon)

;; ### Scales
;; (coming soon)
