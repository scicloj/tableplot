
;; Easy layered graphics with Hanami & Tablecloth

^:kindly/hide-code
(ns index
  (:require [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]
            [clojure.string :as string]
            [scicloj.clay.v2.api :as clay]
            [scicloj.tableplot.v1.hanami :as hanami]
            [tableplot-book.datasets :as datasets]
            [tablecloth.api :as tc]))

^:kindly/hide-code
(def md
  (comp kindly/hide-code kind/md))

(-> datasets/economics-long
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (hanami/base {:=x :date
                  :=y :value})
    (hanami/layer-line {:=mark-color "purple"})
    (hanami/update-data tc/random 5)
    (hanami/layer-point {:=mark-color "green"
                         :=mark-size 200}))

(md "See more examples:

* [Hanami API Walkthrough](./tableplot_book.hanami_walkthrough.html) 👣

* [Plotly API Walkthrough](./tableplot_book.plotly_walkthrough.html) 👣 - experimental 🛠")

;; # Preface

(md
 "
Tableplot is a composition of
[Hanami](https://github.com/jsa-aerial/hanami) data visualization [templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations)
and [Tablecloth](https://scicloj.github.io/tablecloth/) datasets.

It adds a simplified set of Hanami templates and defaults alongside those of Hanami,
as well as a set of template-processing functions
inspired by [ggplot2](https://ggplot2.tidyverse.org/)'s
[layered grammar of graphics](https://vita.had.co.nz/papers/layered-grammar.html).

The current draft was written by Daniel Slutsky,
mentored by jsa-aerial (Hanami author) and Kira McLean.

**Source:** [![(GitHub repo)](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)](https://github.com/scicloj/tableplot)

**Artifact:** [![(Clojars coordinates)](https://img.shields.io/clojars/v/org.scicloj/tableplot.svg)](https://clojars.org/org.scicloj/tableplot)

**Status:** The API is almost stable and will soon move to beta stage.

-------------------

An early version of this library was demonstrated in Kira Mclean's
April 2024 talk at London Clojurians:
")

^{:kind/video true
  :kindly/hide-code true}
{:youtube-id "eUFf3-og_-Y"}

(md "
## Two APIs

Tableplot currently supports two APIs:

- `scicloj.tableplot.v1.hanami` generates [Vega-Lite](https://vega.github.io/vega-lite/) plots and parially composes with the classic Hanami templates.

- `scicloj.tableplot.v1.plotly` generates the [Plotly.js](https://plotly.com/javascript/) plots.

Each of these APIs builds upon the strengths of its target platform and partially uses its naming and concepts. Thus, the two APIs are not completely compatible. The Plotly-based API is expected to grow a little further in terms of its flexibility and the kinds of idioms it can express. ")

(md "

## Near term plan
- Stabilize both the Vega-Lite-based API and the Plotly.js-based API as Beta stage.
- Keep developing main ly the Plotly.js-based API (as it will be more flexible to extend).

## Goals

- Have a functional grammar for common plotting tasks (but not all tasks).
- In particular, provide an easy way to work with layers.
- Be able to pass Tablecloth/tech.ml.dataset datasets directly to the plotting functions.
- Work out-of-the box in Kindly-supporting tools.
- By default, infer relevant information from the data (e.g., field types).
- Catch common errors using the data (e.g., missing fields).
- Be able to use backend Clojure for relevant statistical tasks (e.g., smoothing by regression, histograms, density estimation).
- Be able to rely on Vega-Lite/Plotly.js for other some components of the pipeline (e.g., scales and coordinates).
- Provide simpler Hanami templates, compared to the original ones.
- Still have the option of using the original Hanami templates.
- Still be able to use all of Vega-Lite/Plotly.js in its raw format for the highest flexibility.

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
     (string/join "\n")
     md)
