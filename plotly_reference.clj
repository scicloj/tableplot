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
            [tableplot-book.datasets :as datasets]))

;; ## Overview

;; The Tableplot Plotly API allows the user to write functional pipelines
;; to create and process *templates* of plots, that can eventually be realized
;; as [Plotly.js](https://plotly.com/javascript/) specifications.

;; The data is assumed to be held in datasets defined by [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset)
;; (which can conveniently be used using [Tablecloth](https://scicloj.github.io/tablecloth)).

;; The templates are an adapted version of
;; [Hanami Templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations).
;; Hanami transforms templates by recursively applying a simple set of rules.
;; The rules are based on looking up keys according to standard defaults
;; as well as user substitutions overriding those defaults.
;; Tableplot uses a slighly adapted version of Hanami's template transformations,
;; which make sure not to recurse into datasets.

;; For example, the `layer-point` function generates a template with some specified
;; substitutions. Let us apply this function to a dataset with some uesr substitutions.

;; By default, this template is realized as an actual Plotly.js classification

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-height}))

;; We will use [Kindly](https://scicloj.github.io/kindly-noted/) to specify
;; that this template should be pretty-printed:
(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-height})
    kind/pprint)

;; For now, you are not supposed to make sense of this data representation.
;; As a user, you usually do not need to think about it.

;; If you wish to see the actual Plotly.js specification, you can use
;; the `plot` function:

(-> datasets/iris
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-height})
    plotly/plot
    kind/pprint)

;; This is useful for debugging, and also when one wishes to edit the Plotly.js
;; spec directly.


;; ## Layers

;; ## Keys 

^:kindly/hide-code
(defn include-form [form]
  (format "`%s`" (pr-str form)))

^:kindly/hide-code
(defn include-key [k]
  (format "[`%s`](#%s)"
          (pr-str k)
          (-> k name (str/replace #"^=" ""))))

^:kindly/hide-code
(defn include-fn [f]
  (format "%s\n\n **depends on**: %s"
          (-> f
              meta
              :doc)
          (->> f
               meta
               ::dag/dep-ks
               (map include-key)
               (str/join " "))))

^:kindly/hide-code
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
     kind/fragment)
