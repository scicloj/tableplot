;; # Transpile API reference 📖

;; This chapter is a detailed refernce of Tableplot's Transpile API.

;; ## Setup 🔨

;; In this tutorial, we use:

;; * The Tableplot `transpile` API namepace
;; * [Tablecloth](https://scicloj.github.io/tablecloth/) for dataset processing and column processing
;; * the datasets defined in the [Datasets chapter](./tableplot_book.datasets.html)

;; We require a few aditional namespaces which are used internally to generate this reference.

(ns tableplot-book.transpile-reference
  (:require [scicloj.tableplot.v1.transpile :as transpile]
            [tablecloth.api :as tc]
            [tableplot-book.datasets :as datasets]))

(transpile/hiccup
 (-> datasets/iris
     (tc/select-columns [:sepal-width :sepal-length])
     tc/rows)
 {:body [:p "An echarts example with some nice background"]
  :script ['(var myChart
                 (echarts.init document.currentScript.parentElement))
           (list 'myChart.setOption
                 {:tooltip {}
                  :xAxis {}
                  :yAxis {}
                  :series [{:type "scatter"
                            :data 'data}]})]
  :deps [:echarts]
  :style {:background "floralwhite"}})

(transpile/echarts
 {:form {:tooltip {}
         :xAxis {}
         :yAxis {}
         :series [{:type "scatter"
                   :data (-> datasets/iris
                             (tc/select-columns [:sepal-width :sepal-length])
                             tc/rows)}]}
  :style {:background "floralwhite"}})

(-> datasets/iris
    (tc/select-columns [:sepal-width :sepal-length])
    tc/rows
    (transpile/echarts
     {:form {:tooltip {}
             :xAxis {}
             :yAxis {}
             :series [{:type "scatter"
                       :data 'data}]}
      :style {:background "floralwhite"}}))

(transpile/echarts
 {'iris (-> datasets/iris
            (tc/select-columns [:sepal-width :sepal-length])
            tc/rows)}
 {:form {:tooltip {}
         :xAxis {}
         :yAxis {}
         :series [{:type "scatter"
                   :data 'iris}]}
  :style {:background "floralwhite"}})

