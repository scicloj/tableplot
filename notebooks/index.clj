
;; Easy layered graphics with Hanami & Tablecloth

^:kindly/hide-code
(ns index
  (:require [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]
            [scicloj.tableplot.v1.plotly :as plotly]
            [tablecloth.api :as tc]
            [scicloj.metamorph.ml.rdatasets :as rdatasets]))

^:kindly/hide-code
(def md
  (comp kindly/hide-code kind/md))

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/base {:=x :date
                  :=y :value})
    (plotly/layer-line {:=mark-color "purple"})
    (plotly/update-data tc/random 5)
    (plotly/layer-point {:=mark-color "green"
                         :=mark-size 20}))

;; # Preface 👋

(md
 "
Tableplot is a Clojure library for data visualization 
inspired by [ggplot2](https://ggplot2.tidyverse.org/)'s
[layered grammar of graphics](https://vita.had.co.nz/papers/layered-grammar.html). 

It is built to be composable with [Tablecloth](https://scicloj.github.io/tablecloth/) table processing
and extend the plotting
[templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations)
of [Hanami](https://github.com/jsa-aerial/hanami).

Tableplot works with any tool that supports 
the [Kindly](https://scicloj.github.io/kindly-noted/) data visualization standard,
such as [Clay](https://scicloj.github.io/clay/).

**Source:** [![(GitHub repo)](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)](https://github.com/scicloj/tableplot)

**Artifact:** [![(Clojars coordinates)](https://img.shields.io/clojars/v/org.scicloj/tableplot.svg)](https://clojars.org/org.scicloj/tableplot)

")


(md "
## Three APIs

Tableplot currently supports three APIs:

- `scicloj.tableplot.v1.plotly` generates the [Plotly.js](https://plotly.com/javascript/) plots.

- `scicloj.tableplot.v1.hanami` generates [Vega-Lite](https://vega.github.io/vega-lite/) plots and parially composes with the original Hanami templates.

- `scicloj.tableplot.v1.transpile` (experimental) generates specs for various data visualization libraries that may include some Javascript code.

Each of the `plotly` and `hnami` APIs builds upon the strengths of its target platform and partially uses its naming and concepts. Thus, the two APIs are not completely compatible. The Plotly-based API is expected to grow a little further in terms of its flexibility and the kinds of idioms it can express. ")

(md "

## Goals

- Have a functional grammar for common plotting tasks (but not all tasks).
- In particular, provide an easy way to work with layers.
- Be able to pass Tablecloth/tech.ml.dataset datasets directly to the plotting functions.
- Work out-of-the box in Kindly-supporting tools.
- By default, infer relevant information from the data (e.g., field types).
- Catch common errors using the data (e.g., missing fields).
- Be able to use backend Clojure for relevant statistical tasks (e.g., smoothing by regression, histograms, density estimation).
- Be able to rely on Vega-Lite/Plotly.js for other some components of the pipeline (e.g., scales and coordinates).
- Provide simple Hanami templates in addition to the original ones.
- Still have the option of using the original Hanami templates.
- Still be able to use all of Vega-Lite/Plotly.js in their raw format for the highest flexibility.

In the longer term, this project is part of the Scicloj effort to create a grammar-of-graphics visualization library in Clojure.

## Discussion
- development - topic threads under [#tableplot-dev](https://clojurians.zulipchat.com/#narrow/stream/443101-tableplot-dev) at the [Clojurians Zulip chat](https://scicloj.github.io/docs/community/chat/) or [Github Issues](https://github.com/scicloj/tableplot/issues)
- usage - topic threads under [#data-science](https://clojurians.zulipchat.com/#narrow/stream/151924-data-science/)


## Chapters in this book:
")

^:kindly/hide-code
(defn chapter->title [chapter]
  (or (some->> chapter
               (format "notebooks/tableplot_book/%s.clj")
               slurp
               str/split-lines
               (filter #(re-matches #"^;; # .*" %))
               first
               (#(str/replace % #"^;; # " "")))
      chapter))

(->> "notebooks/chapters.edn"
     slurp
     clojure.edn/read-string
     (map (fn [chapter]
            (prn [chapter (chapter->title chapter)])
            (format "\n- [%s](tableplot_book.%s.html)\n"
                    (chapter->title chapter)
                    chapter)))
     (str/join "\n")
     md)
