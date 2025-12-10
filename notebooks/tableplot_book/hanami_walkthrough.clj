;; # Hanami walkthrough 👣

;; Tableplot offers a Clojure API for creating [Vega-Lite](https://vega.github.io/vega-lite/) plots through layered pipelines.

;; The API not only uses [Hanami templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations) but also partially composes with the original Hanami templates and parameters.

;; Here, we provide a walkthrough of the API.

;; In this walkthrough, we will demonstrate the main functionality of Tableplot.

;; Soon, we will provide more in-depth explanations in additional chapters.

;; ## Setup

;; For this tutorial, we require:

;; * Tableplot's Hanami API namespace

;; * [Hanami](https://github.com/jsa-aerial/hanami)

;; * [Tablecloth](https://scicloj.github.io/tablecloth/)

;; * the [datetime namespace](https://cnuernber.github.io/dtype-next/tech.v3.datatype.datetime.html) of [dtype-next](https://github.com/cnuernber/dtype-next)

;; * the [print namespace](https://techascent.github.io/tech.ml.dataset/tech.v3.dataset.print.html) of [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) for customized dataset printing

;; * [Kindly](https://scicloj.github.io/kindly-noted/) (to specify how certain values should be visualized)

;; * the datasets defined in the [Datasets chapter](./tableplot_book.datasets.html)

(ns tableplot-book.hanami-walkthrough
  (:require [scicloj.tableplot.v1.hanami :as hanami]
            [aerial.hanami.templates :as ht]
            [tablecloth.api :as tc]
            [tech.v3.datatype.datetime :as datetime]
            [tech.v3.dataset.print :as print]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.metamorph.ml.rdatasets :as rdatasets]))

;; ## Basic usage

;; Let us create a scatter plot from the Iris dataset.
;; We pass a Tablecloth dataset to a Tableplot function
;; with a Tableplot template.

(-> (rdatasets/datasets-iris)
    (hanami/plot hanami/point-chart
                 {:=x :sepal-width
                  :=y :sepal-length
                  :=color :species
                  :=mark-size 200}))

(kind/test-last [#(= (-> % :mark :type) "circle")])

;; Soon, the Tableplot docs will offer an introduction to the use of
;; Hanami templates and substitution keys.
;; For now, please see the [Hanami documentation](https://github.com/jsa-aerial/hanami).

;; While Tableplot allows using the original Hanami templates and substitution keys,
;; it also offers its own sets of templates, that we just used here.

;; Unlike the original Hanami keys of using
;; capital letter substitution keys (e.g. `:COLOR`)
;; Tableplot uses the convention of
;; substitution keys beginning with `=` (e.g. `:=color`)

;; The templates of Tableplot also support a layered grammar which is demonstrated later in this document.

;; (Here is how we can express the same plot with the layered grammar:)

(-> (rdatasets/datasets-iris)
    (hanami/layer-point
     {:=x :sepal-width
      :=y :sepal-length
      :=color :species
      :=mark-size 200}))

(kind/test-last [#(let [layer-defaults (-> %
                                           :aerial.hanami.templates/defaults
                                           :=layer
                                           first
                                           :aerial.hanami.templates/defaults)]
                    (and (= (:=x layer-defaults) :sepal-width)
                         (= (:=y layer-defaults) :sepal-length)
                         (= (:=color layer-defaults) :species)))])

;; The value returned by a `hanami/plot` function
;; is a [Vega-Lite](https://vega.github.io/vega-lite/) spec:

(-> (rdatasets/datasets-iris)
    (hanami/plot hanami/point-chart
                 {:=x :sepal-width
                  :=y :sepal-length
                  :=color :species
                  :=mark-size 200})
    kind/pprint)

(kind/test-last [#(and (contains? % :data)
                       (contains? % :encoding)
                       (= (-> % :mark :type) "circle"))])

;; By looking at the `:values` key above,
;; you can see that the dataset was implicitly represented as CSV,
;; and that it was defined to be rendered as `:svg` by default.

;; The resulting plot is displayed correctly,
;; as it is annotated by Kindly as a Vega-lite plot:

(-> (rdatasets/datasets-iris)
    (hanami/plot hanami/point-chart
                 {:=x :sepal-width
                  :=y :sepal-length
                  :=color :species
                  :=mark-size 200})
    meta)

(kind/test-last [#(= (:kindly/kind %) :kind/vega-lite)])

;; ## Using original Hanami templates and defaults

;; We can also use Hanami's original templates (`ht/chart`)
;; and substitution keys (`:X`, `:Y`, `:MSIZE`).

(-> (rdatasets/datasets-iris)
    (hanami/plot ht/point-chart
                 {:X :sepal-width
                  :Y :sepal-length
                  :MSIZE 200
                  :COLOR "species"}))

(kind/test-last [#(= (-> % :mark :type) "circle")])

(-> (rdatasets/datasets-iris)
    (hanami/plot ht/point-chart
                 {:X :sepal-width
                  :Y :sepal-length
                  :MSIZE 200
                  :COLOR "species"})
    kind/pprint)

;; ## Inferring and overriding field types

;; Field [types](https://vega.github.io/vega-lite/docs/type.html) are inferred from the dataset's column type.
;; Here, for example, `x` and `y` are `:quantitative`, and `color` is `:nominal`
;; (and is thus coloured with distinct colours rather than a gradient).

(-> (rdatasets/datasets-iris)
    (hanami/plot hanami/point-chart
                 {:=x :sepal-width
                  :=y :sepal-length
                  :=color :species
                  :=mark-size 200}))

(kind/test-last [#(and (= (-> % :encoding :x :type) :quantitative)
                       (= (-> % :encoding :y :type) :quantitative)
                       (= (-> % :encoding :color :type) :nominal))])

(-> (rdatasets/datasets-iris)
    (hanami/plot hanami/point-chart
                 {:=x :sepal-width
                  :=y :sepal-length
                  :=color :species
                  :=mark-size 200})
    kind/pprint)

;; On the other hand, in the following example,
;; `color` is `:quantitative`:

(-> (rdatasets/datasets-mtcars)
    (hanami/plot hanami/point-chart
                 {:=x :mpg
                  :=y :disp
                  :=color :cyl
                  :=mark-size 200}))

(kind/test-last [#(= (-> % :encoding :color :type) :quantitative)])

(-> (rdatasets/datasets-mtcars)
    (hanami/plot hanami/point-chart
                 {:=x :mpg
                  :=y :disp
                  :=color :cyl
                  :=mark-size 200})
    kind/pprint)

;; This can be overridden to define `color` as `:noninal`:

(-> (rdatasets/datasets-mtcars)
    (hanami/plot hanami/point-chart
                 {:=x :mpg
                  :=y :disp
                  :=color :cyl
                  :=color-type :nominal
                  :=mark-size 200}))

(kind/test-last [#(= (-> % :encoding :color :type) :nominal)])

(-> (rdatasets/datasets-mtcars)
    (hanami/plot hanami/point-chart
                 {:=x :mpg
                  :=y :disp
                  :=color :cyl
                  :=color-type :nominal
                  :=mark-size 200})
    kind/pprint)

;; ## More examples

;; A Tableplot boxplot:

(-> (rdatasets/datasets-mtcars)
    (hanami/plot hanami/boxplot-chart
                 {:=x :cyl
                  :=x-type :nominal
                  :=y :disp}))

(kind/test-last [#(and (= (-> % :mark :type) "boxplot")
                       (= (-> % :encoding :x :type) :nominal))])

;; An original Hanami boxplot:

(-> (rdatasets/datasets-mtcars)
    (hanami/plot ht/boxplot-chart
                 {:X :cyl
                  :XTYPE :nominal
                  :Y :disp}))

(kind/test-last [#(= (-> % :mark :type) "boxplot")])

;; Plotting segments with Tableplot:

(-> (rdatasets/datasets-iris)
    (hanami/plot hanami/rule-chart
                 {:=x :sepal-width
                  :=y :sepal-length
                  :=x2 :petal-width
                  :=y2 :petal-length
                  :=mark-opacity 0.5
                  :=mark-size 3
                  :=color :species}))

(kind/test-last [#(and (= (-> % :mark :type) "rule")
                       (contains? (-> % :encoding) :x2)
                       (contains? (-> % :encoding) :y2))])

;; Plotting segments with original Hanami:

(-> (rdatasets/datasets-iris)
    (hanami/plot ht/rule-chart
                 {:X :sepal-width
                  :Y :sepal-length
                  :X2 :petal-width
                  :Y2 :petal-length
                  :OPACITY 0.5
                  :SIZE 3
                  :COLOR "species"}))

;; ## Time series

;; Let us plot a time series:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/plot hanami/line-chart
                 {:=x :date
                  :=y :value
                  :=mark-color "purple"}))

(kind/test-last [#(and (= (-> % :mark :type) "line")
                       (= (-> % :encoding :x :type) :temporal))])

;; You see, the `:date` field was correctly inferred to be
;; of the `:temporal` kind.

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/plot hanami/line-chart
                 {:=x :date
                  :=y :value
                  :=mark-color "purple"})
    kind/pprint)

;; ## Delayed transformation

;; Instead of the `hanami/plot` function, it is possible to used
;; `hanami/base`:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base hanami/line-chart
                 {:=x :date
                  :=y :value
                  :=mark-color "purple"}))

(kind/test-last [#(contains? (:aerial.hanami.templates/defaults %) :=dataset)])

;; The result is displayed the same way, but the internal representation
;; delays the Hanami transformation of templates.

;; Let us compare the two:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/plot hanami/line-chart
                 {:=x :date
                  :=y :value
                  :=mark-color "purple"})
    kind/pprint)

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base hanami/line-chart
                 {:=x :date
                  :=y :value
                  :=mark-color "purple"})
    kind/pprint)

;; The structure returned by `hanami/base` is a Hanami template
;; (with [local defaults](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#template-local-defaults)).
;; When it is displayed, it goes through the Hanami transform
;; to recieve the Vega-Lite spec.

;; When we use base, we can keep processing the template in a pipeline
;; of transformations. We will use it soon with layers.

;; ## Adding layers

;; A base plot does not need to have a specified chart.
;; Instead, we may add layers:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value
                  :=mark-color "purple"})
    hanami/layer-line)

(kind/test-last [#(= (-> % :aerial.hanami.templates/defaults :=layer count) 1)])

;; The substitution keys can also be specified on the layer level:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-line {:=mark-color "purple"}))

;; This allows us to create, e.g., aesthetic differences between layers:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-point {:=mark-color "green"
                         :=mark-size 200
                         :=mark-opacity 0.1})
    (hanami/layer-line {:=mark-color "purple"}))

(kind/test-last [#(= (-> % :aerial.hanami.templates/defaults :=layer count) 2)])

;; We can also skip the base and have everything in the layer:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/layer-line {:=x :date
                        :=y :value
                        :=mark-color "purple"}))

;; ## Updating data

;; Using `hanami/update-data`, we may process the dataset
;; during the pipeline, affecting only the layers added further down the pipeline.

;; This functionality is inspired by [ggbuilder](https://github.com/mjskay/ggbuilder)
;; and [metamorph](https://github.com/scicloj/metamorph).

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-line {:=mark-color "purple"})
    (hanami/update-data tc/random 5)
    (hanami/layer-point {:=mark-color "green"
                         :=mark-size 200}))

(kind/test-last [#(let [layers (-> % :aerial.hanami.templates/defaults :=layer)]
                    (and (= (count layers) 2)
                         (= (-> layers second :aerial.hanami.templates/defaults :=layer-dataset tc/row-count) 5)))])

;; You see, we have lots of data for the lines,
;; but only five random points.

;; ## Processing raw vega-lite

;; During a pipeline, we may call `hanami/plot`
;; to apply the Hanami transform and realize the
;; `Vega-Lite` spec.

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-line {:=mark-color "purple"})
    (hanami/update-data tc/random 5)
    (hanami/layer-point {:=mark-color "green"
                         :=mark-size 200})
    hanami/plot
    kind/pprint)

;; While this in itself does not affect the display of the plot,
;; it allows us to keep editing it as a Vega-Lite spec.
;; For example, let us change the backgound colour this way:

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-line {:=mark-color "purple"})
    (hanami/update-data tc/random 5)
    (hanami/layer-point {:=mark-color "green"
                         :=mark-size 200})
    hanami/plot
    (assoc :background "lightgrey"))

(kind/test-last [#(= (:background %) "lightgrey")])

;; For another example, let us change the y scale to logarithmic.
;; See [Scale](https://vega.github.io/vega-lite/docs/scale.html)
;; in the Vega-Lite documentation.

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-line {:=mark-color "purple"})
    (hanami/update-data tc/random 5)
    (hanami/layer-point {:=mark-color "green"
                         :=mark-size 200})
    hanami/plot
    (assoc-in [:encoding :y :scale :type] "log"))

(kind/test-last [#(= (-> % :encoding :y :scale :type) "log")])

;; ## Smoothing

;; `hanami/layer-smooth` is a layer that applies some statistical
;; processing to the dataset to model it as a smooth shape.
;; It is inspired by ggplot's [geom_smooth](https://ggplot2.tidyverse.org/reference/geom_smooth.html).

;; At the moment, it can only be used to model `:=y` by linear regression.
;; Soon we will add more ways of modelling the data.

(-> (rdatasets/datasets-iris)
    (hanami/base {:=title "dummy"
                  :=mark-color "green"
                  :=x :sepal-width
                  :=y :sepal-length})
    hanami/layer-point
    (hanami/layer-smooth {:=mark-color "orange"}))

(kind/test-last [#(= (-> % :aerial.hanami.templates/defaults :=layer count) 2)])

;; By default, the regression is computed with only one predictor variable,
;; which is `:=x`.
;; But this can be overriden using the `:predictors` key.
;; We may compute a regression with more than one predictor.

(-> (rdatasets/datasets-iris)
    (hanami/base {:=x :sepal-width
                  :=y :sepal-length})
    hanami/layer-point
    (hanami/layer-smooth {:=predictors [:petal-width
                                        :petal-length]}))

(kind/test-last [#(let [smooth-layer (-> % :aerial.hanami.templates/defaults :=layer second :aerial.hanami.templates/defaults)]
                    (= (:=predictors smooth-layer) [:petal-width :petal-length]))])

;; ## Grouping

;; The regression computed by `hanami/layer-smooth`
;; is affected by the inferred grouping of the data.

;; For example, here we recieve three regression lines,
;; each for every species.

(-> (rdatasets/datasets-iris)
    (hanami/base {:=title "dummy"
                  :=color :species
                  :=x :sepal-width
                  :=y :sepal-length})
    hanami/layer-point
    hanami/layer-smooth)

(kind/test-last [#(let [base-defaults (-> % :aerial.hanami.templates/defaults)]
                    (= (:=color base-defaults) :species))])

;; This happened because the `:color` field was `:species`,
;; which is of `:nominal` type.

;; But we may override this using the `:group` key.
;; For example, let us avoid grouping:

(-> (rdatasets/datasets-iris)
    (hanami/base {:=title "dummy"
                  :=mark-color "green"
                  :=color :species
                  :=group []
                  :=x :sepal-width
                  :=y :sepal-length})
    hanami/layer-point
    hanami/layer-smooth)

(kind/test-last [#(let [base-defaults (-> % :aerial.hanami.templates/defaults)]
                    (= (:=group base-defaults) []))])

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
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-smooth {:=color :relative-time
                          :=mark-size 10
                          :=group []
                          :=predictors [:yearmonth]})
    ;; Keep only the past for the following layer:
    (hanami/update-data (fn [dataset]
                          (-> dataset
                              (tc/select-rows (fn [row]
                                                (-> row :relative-time (= "Past")))))))
    (hanami/layer-line {:=mark-color "purple"
                        :=mark-size 3}))

;; ## Histograms

;; Histograms can also be represented as layers
;; with statistical processing:

(-> (rdatasets/datasets-iris)
    (hanami/layer-histogram {:=x :sepal-width}))

(kind/test-last [#(let [layer-defaults (-> % :aerial.hanami.templates/defaults :=layer first :aerial.hanami.templates/defaults)]
                    (= (:=x layer-defaults) :sepal-width))])

(-> (rdatasets/datasets-iris)
    (hanami/layer-histogram {:=x :sepal-width
                             :=histogram-nbins 30}))

(kind/test-last [#(let [layer-defaults (-> % :aerial.hanami.templates/defaults :=layer first :aerial.hanami.templates/defaults)]
                    (= (:=histogram-nbins layer-defaults) 30))])



