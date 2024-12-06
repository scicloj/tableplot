;; # Plotly API reference

;; This chapter is a detailed refernce of Tableplot's Plotly API.
;; For diverse examples, see the
;; [Plotly API Walkthrough](./tableplot_book.plotly_walkthrough.html).

(ns tableplot-book.plotly-reference
  (:require [scicloj.tableplot.v1.plotly :as plotly]
            [scicloj.tableplot.v1.dag :as dag]
            [scicloj.kindly.v4.kind :as kind]
            [aerial.hanami.common :as hc]
            [clojure.string :as str]
            [tablecloth.api :as tc]
            [tableplot-book.datasets :as datasets]
            [tablecloth.column.api :as tcc]
            [scicloj.kindly.v4.api :as kindly]
            [aerial.hanami.templates :as ht]))

^:kindly/hide-code
(defn include-form [form]
  (format "`%s`" (pr-str form)))

^:kindly/hide-code
(defn include-key [k]
  (format "[`%s`](#%s)"
          (pr-str k)
          (-> k name (str/replace #"^=" ""))))

(defn turn-keys-into-links [text]
  (or (some->> text
               (re-seq #"`\:\=[a-z|\-]+`")
               (reduce (fn [s k]
                         (str/replace
                          s
                          k
                          (-> k
                              (str/replace #"[`|:]" "")
                              keyword
                              include-key)))
                       text))
      text))

^:kindly/hide-code
(defn include-fn [f]
  (format "%s\n\n **depends on**: %s"
          (-> f
              meta
              :doc
              turn-keys-into-links)
          (->> f
               meta
               ::dag/dep-ks
               (map include-key)
               (str/join " "))))

(defn include-fnvar [fnvar]
  (-> (let [{:keys [name arglists doc]} (meta fnvar)]
        (str (format "### `%s`\n" name)
             (->> arglists
                  (map (fn [l]
                         (->> l
                              pr-str
                              (format "`%s`\n\n"))))
                  (str/join ""))
             (turn-keys-into-links doc)))
      kind/md
      kindly/hide-code))

^:kindly/hide-code
(defn include-all-keys []
  (->> plotly/standard-defaults
       (map (fn [[k v doc]]
              (kind/md
               [(format "### %s" (include-key k))
                (some->> doc
                         (format "**role:** %s\n"))
                (format "**default:** %s\n"
                        (cond (fn? v) (include-fn v)
                              (= v hc/RMV) "`NONE`"
                              (keyword? v) (include-key v)
                              :else (include-form v)))])))
       kind/fragment))

^:kindly/hide-code
(defn md [& strings]
  (->> strings
       (str/join " ")
       turn-keys-into-links
       kind/md
       kindly/hide-code))


;; ## Overview

;; The Tableplot Plotly API allows the user to write functional pipelines
;; to create and process *templates* of plots, that can eventually be realized
;; as [Plotly.js](https://plotly.com/javascript/) specifications.

;; The data is assumed to be held in datasets defined by [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset)
;; (which can conveniently be used using [Tablecloth](https://scicloj.github.io/tablecloth)).

;; The templates are an adapted version of
;; [Hanami Templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations).
;; Hanami transforms templates by recursively applying a simple set of rules.
;; The rules are based on looking up *substitution keys* according to standard defaults
;; as well as user substitutions overriding those defaults.
;; Tableplot uses a slighly adapted version of Hanami's template transformations,
;; which make sure not to recurse into datasets.

;; For example, the `layer-point` function generates a template with some specified
;; substitutions. Let us apply this function to a dataset with some user substitutions.
;; As you can see be low, all the substitution keys are keywords beginning with `=`.
;; This is just a convention that helps distinguish their role from other k

;; By default, this template is displayed by realizing it as  an actual Plotly.js
;; specification.     

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10}))

;; We will use [Kindly](https://scicloj.github.io/kindly-noted/) to specify
;; that this template should rather be pretty-printed as a data structure.
(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10})
    kind/pprint)

;; For now, you are not supposed to make sense of this data representation.
;; As a user, you usually do not need to think about it.

;; If you wish to see the actual Plotly.js specification, you can use
;; the `plot` function:

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10})
    plotly/plot
    kind/pprint)

;; This is useful for debugging, and also when one wishes to edit the Plotly.js
;; spec directly.

;; In the remainder of this chapter, we will offer a detailed reference to the API
;; functions, the way layers are defined, the substitution keys, and the relationships
;; among them.

;; ## Debugging

;; Throughout this notebook, we will sometimes use the `debug` function that
;; allows one to look into the value of a given substitution key in a given
;; context.

(md
 "For example, here we learn about the"
 (include-key :=background)
 "key for background color, which is a grey colour by default.")

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10})
    (plotly/debug :=background))

;; ## Raw Plotly specifications

;; Before beginning the exploration of Tableplot's Plotly API, let us remember we may
;; also use the raw format of specifying plots to Plotly.js.
;; We simply use JSON data structures to represent the JSON format expected by Plotly.js
;; and annotate it as `kind/plotly`, so that our Clojure tooling knows to treat it as such
;; when displaying it.

;; For example, let us reproduce one of its [Bubble Chart Examples](https://plotly.com/javascript/bubble-charts/).

;; The original Javascript code looks like this:
;; ```js
;; var trace1 = {
;;               x: [1, 2, 3, 4],
;;               y: [10, 11, 12, 13],
;;               text: ['A<br>size: 40', 'B<br>size: 60', 'C<br>size: 80', 'D<br>size: 100'],
;;               mode: 'markers',
;;               marker: {
;;                        color: ['rgb(93, 164, 214)', 'rgb(255, 144, 14)',  'rgb(44, 160, 101)', 'rgb(255, 65, 54)'],
;;                        size: [40, 60, 80, 100]
;;                        }
;;               };

;; var data = [trace1];

;; var layout = {
;;               title: {
;;                       text: 'Bubble Chart Hover Text'
;;                       },
;;               showlegend: false,
;;               height: 600,
;;               width: 600
;;               };

;; Plotly.newPlot('myDiv', data, layout);

;; ```

;; Here is how we represent that in Clojure:

(kind/plotly
 {:data [{:x [1 2 3 4]
          :y [10 11 12 13]
          :text ["A<br>size: 40" "B<br>size: 60" "C<br>size: 80" "D<br>size: 100"]
          :mode :markers
          :marker {:color ["rgb(93, 164, 214)", "rgb(255, 144, 14)",  "rgb(44, 160, 101)", "rgb(255, 65, 54)"]
                   :size [40 60 80 100]}}]
  :layout {:title {:text "Bubble Chart Hover Text"}
           :showlegend false
           :height 600
           :width 600}})

;; Sometimes, this raw way is all we need; but in common situations, Tableplot make things easier.

;; ## Concepts

;; ### Templates 💡
;; For typical user needs, it is ok to skip this section.
;; It is here for the curious ones who wish to have a slightly clearer picture of the internals.

;; (coming soon)

;; ### Plotly.js traces

;; Traces are a core concept in Plotly.js.
;; They specify separate parts of the plots which can be drawn on the same canvas
;; but may vary in their visual nature.

;; For example, here is a raw Plotly.js spec with two traces.

(kind/plotly
 {:data [{:x [1 2 3 4]
          :y [10 15 12 13]
          :color "blue"
          :mode :markers
          :marker {:size [40 60 80 100]
                   :color ["blue" "brown" "red" "green"]}}
         {:x [1 2 3 4]
          :y [15 21 17 18]
          :mode :markers
          :color "grey"
          :marker {:size 50
                   :color "grey"}}]})

;; In Tableplot, we often do not need to think about traces, as they are drawn for us.
;; But it is helpful to know about them if we wish to understand the Plotly specs
;; generated by Tableplot.

;; ### Layers

;; Layers are a more high-level concept. We introduce them in Tableplot following
;; [ggplot2](https://ggplot2.tidyverse.org/)'s
;; [layered grammar of graphics](https://vita.had.co.nz/papers/layered-grammar.html).
;; Plotly bindings in other languages have similar concepts.

;; Like traces, layers are also parts of the plot that can be drawn on the same canvas,
;; but they are a slightly higher-level concept, that makes it easier to bind our data to
;; what we receive as the plot.

;; Layers are themselves templates, so they can have their own substitutions.

;; For example:
(-> datasets/iris
    (tc/random 10 {:seed 1})
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 20})
    (plotly/layer-text {:=text :species}))

;; This plot has **two layers**: one for points, and one for text (which is visible on hover).

;; Let us see that using `debug`:

(-> datasets/iris
    (tc/random 10 {:seed 1})
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 20})
    (plotly/layer-text {:=text :species})
    (plotly/debug :=layers)
    kind/pprint)

;; You see, a layer is an intermediate data representation of Tableplot
;; that takes care of the details necessary to generate traces.

;; In our example, the two layers are realied as **four traces**: since the point layer is colored
;; by species, it is realized as three traces.

;; Let us see that using `debug`:

(-> datasets/iris
    (tc/random 10 {:seed 1})
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 20})
    (plotly/layer-text {:=text :species})
    (plotly/debug :=traces)
    kind/pprint)

;; ### Mark

;; Mark is a Tableplot notion that is used to distinguish different types of layers,
;; e.g. a layer of points from a layer of lines.
;; It is similar to the ggplot notion of 'geom'.

;; Its possible values are:
[:point :text :line :box :bar :segment]

;; Here, `:box` means [boxplot](https://en.wikipedia.org/wiki/Box_plot).

;; ### Coordinates

;; Coordinates are a Tableplot notion that defines
;; how marks are eventually mapped over to the cranvas,
;; similar to ggplot's notion.

;; We currently support the following:
[:2d :3d :polar :geo]

;; Here, 2d and 3d mean Eucledian coordinates of the corresponding dimensions,
;; and `:geo` means latitude and longitude.

;; ### Plotly.js mode and type  

;; Mode and type are Plotl.js notions that are used to distinguish
;; diffetent types of layers.

;; Combinations of Tableplot's marks and coordinates
;; are mapped into combinations of Plotly.js mode and type,
;; but currently we do not use all the meaningful combinations
;; supported by Plotly.js.

;; Mode is defined as follows:

^:kindly/hide-code
(-> {:mark [:point :text :line :box :bar :segment]}
    tc/dataset
    (tc/map-columns :mode [:mark] plotly/mark->mode))

;; Type is defined as the concatenation of a mark-based string
;; (`"box"` or `"bar"` in the cases we have these marks, and `"scatter"` in all other marks)
;; with a coordinates-based string
;; (`"3d"`, `"polar"`, or `"geo"` in the cases whre we have such coordinates, nil otherwise).

;; Thus, for example, if the mark is `:point` and the coordinates are `:polar`,
;; then the type is `"scatterpolar"`.

;; ### Variable types

;; Looking into the data in the columns, we may classify them into the following types:
;; - `:quantitative` - numerical columns
;; - `:temporal` - date-time columns
;; - `:nominal` - all other column types (e.g., Strings, keywords)

;; In certain situations, the types of data in relevant columns determines the way things
;; should be plotted.

;; For example, when a column is used for coloring a plot, we should use gradient colors
;; if it is quantitative but more distinct colors if it is nominal.

;; Nominal color column:

(-> datasets/iris
    (plotly/layer-point
     {:=x :sepal-width
      :=y :sepal-length
      :=color :species
      :=mark-size 20}))

;; Quantitative color column:

(-> datasets/mtcars
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=mark-size 20}))

;; This can be overridden through the `:=color-type` key:

(-> datasets/mtcars
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=color-type :nominal
      :=mark-size 20}))

;; ## API functions

(include-fnvar #'plotly/base)

;; For example:

(-> datasets/iris
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length
                  :=mark-size 10})
    (plotly/layer-point {:=mark-color "grey"
                         :=mark-size 20
                         :=mark-opacity 0.3})
    (plotly/layer-point {:=color :species}))

(include-fnvar #'plotly/layer)

;; For example, we could write someting like:

(-> datasets/iris
    (plotly/layer plotly/layer-base
                  {:=mark :point
                   :=x :sepal-width
                   :=y :sepal-length}))

;; Of course, this can also be expressed more succinctly using `layer-point`.

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length}))

(include-fnvar #'plotly/layer-point)

;; Example:

(-> datasets/mtcars
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=color-type :nominal
      :=mark-size 20}))

(include-fnvar #'plotly/layer-line)

;; Example:

(-> datasets/economics-long
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/layer-line
     {:=x :date
      :=y :value
      :=mark-color "purple"}))

(include-fnvar #'plotly/layer-bar)

;; Example:

(-> datasets/mtcars
    (tc/group-by [:cyl])
    (tc/aggregate {:total-disp #(-> % :disp tcc/sum)})
    (tc/add-column :bar-width 0.5)
    (plotly/layer-bar
     {:=x :cyl
      :=bar-width :bar-width
      :=y :total-disp}))

(include-fnvar #'plotly/layer-boxplot)

;; Example:

(-> datasets/mtcars
    (plotly/layer-boxplot
     {:=x :cyl
      :=y :disp}))

(include-fnvar #'plotly/layer-segment)

;; Example:

(-> datasets/iris
    (plotly/layer-segment
     {:=x0 :sepal-width
      :=y0 :sepal-length
      :=x1 :petal-width
      :=y1 :petal-length
      :=mark-opacity 0.4
      :=mark-size 3
      :=color :species}))

(include-fnvar #'plotly/layer-text)

;; Example:

(-> datasets/mtcars
    (plotly/layer-text
     {:=x :mpg
      :=y :disp
      :=text :cyl
      :=textfont {:family "Courier New, monospace"
                  :size 16
                  :color :purple}
      :=mark-size 20}))

(include-fnvar #'plotly/layer-histogram)

;; Examples:

(-> datasets/iris
    (plotly/layer-histogram {:=x :sepal-width}))

(-> datasets/iris
    (plotly/layer-histogram {:=x :sepal-width
                             :=histogram-nbins 30}))

(-> datasets/iris
    (plotly/layer-histogram {:=x :sepal-width
                             :=color :species
                             :=mark-opacity 0.5}))

(include-fnvar #'plotly/layer-density)

;; Examples:

(-> datasets/iris
    (plotly/layer-density {:=x :sepal-width}))

(-> datasets/iris
    (plotly/layer-density {:=x :sepal-width
                           :=density-bandwidth 0.05}))

(-> datasets/iris
    (plotly/layer-density {:=x :sepal-width
                           :=density-bandwidth 1}))

(-> datasets/iris
    (plotly/layer-density {:=x :sepal-width
                           :=color :species}))


(include-fnvar #'plotly/layer-smooth)

;; Examples:

(md "Simple linear regression of `:=y` by `:=x`:")

(-> datasets/iris
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=mark-color "orange"
                          :=name "Predicted"}))

(md "Multiple linear regression of `:=y` by `:=predictors`:")

(-> datasets/iris
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=predictors [:petal-width
                                        :petal-length]
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

(md "Polynomial regression of `:=y` by `:=design-matrix`:")

(-> datasets/iris
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=design-matrix [[:sepal-width '(identity sepal-width)]
                                           [:sepal-width-2 '(* sepal-width
                                                               sepal-width)]]
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

(md "Custom regression defined by `:=model-options`:")

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

(-> datasets/iris
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=model-options regression-tree-options
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

(md "Grouped regression where `:=inferred-group` is influenced by `:color`
since `:=color-type` is `:nominal`:")

(-> datasets/iris
    (plotly/base {:=title "dummy"
                  :=color :species
                  :=x :sepal-width
                  :=y :sepal-length})
    plotly/layer-point
    plotly/layer-smooth)

(md "Regression where grouping is avoiding using through `:=group`:")

(-> datasets/iris
    (plotly/base {:=title "dummy"
                  :=color :species
                  :=group []
                  :=x :sepal-width
                  :=y :sepal-length})
    plotly/layer-point
    plotly/layer-smooth)

(include-fnvar #'plotly/plot)

;; For example:
(-> datasets/iris
    tc/head
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length})
    plotly/plot
    kind/pprint)

;; This can be useful for editing the plot as a raw Plotly.js specification.
;; For example:

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length})
    plotly/plot
    (assoc-in [:layout :plot_bgcolor] "floralwhite"))

(include-fnvar #'plotly/debug)

;; For example:
(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species}))

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species})
    (plotly/debug :=background))

(md "Here, we see that `:=background` is deterimined to be grey.")

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species})
    (plotly/debug 0 :=color-type))

(md "Here, we see that `:=color-type` for the 0th layer is deterimined to be `:nominal`.")

;; ## Substitution Keys 

^:kindly/hide-code
(include-all-keys)
