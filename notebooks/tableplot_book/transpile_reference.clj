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
            [tableplot-book.datasets :as datasets]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

(-> [[1 2]
     [2 4]
     [3 9]
     [4 16]]
    ;; Pass the data to be handled by a custom script:
    (transpile/div-with-script ['(var myChart
                                      (echarts.init
                                       document.currentScript.parentElement))
                                (list 'myChart.setOption
                                      {:xAxis {}
                                       :yAxis {}
                                       :series [{:type "scatter"
                                                 :data 'data}]})])
    ;; Use metadata to specify Kindly options:
    (vary-meta 
     assoc-in [:kindly/options :html/deps] [:echarts]))


(-> [[1 2]
     [2 4]
     [3 9]
     [4 16]]
    (transpile/echarts {:xAxis {}
                        :yAxis {}
                        :series [{:type "scatter"
                                  :data 'data}]}))




(-> datasets/mtcars
    (tc/select-columns [:wt :mpg :cyl :gear])
    tc/rows
    (transpile/echarts {:xAxis {:name "weight"}
                        :yAxis {:name "miles per gallon"}
                        :visualMap [{:dimension 2
                                     :categories ["4" "6" "8"]
                                     :inRange {:color ["#51689b", "#ce5c5c", "#fbc357"]}
                                     :name "cylinders"}]
                        :tooltip {:formatter '(fn [obj]
                                                (return
                                                 (+ 
                                                  "<p>weight:" (. obj value [0]) "</p>"
                                                  "<p>miles per fallon:" (. obj value [1]) "</p>"
                                                  "<p>cylinders:" (. obj value [2]) "</p>"
                                                  "<p>gears:" (. obj value [3]) "</p>")))}
                        :series [{:type "scatter"
                                  :symbolSize '(fn [value]
                                                 (-> value
                                                     (. [3])
                                                     (* 5)
                                                     return))
                                  :data 'data}]}))



(-> {:x [1 2 3 4]
     :y [3 4 9 16]}
    (transpile/plotly {:data ['{:x data.x
                                :y data.y
                                :mode "markers"
                                :type "scatter"}]}))

(-> {'x [1 2 3 4]
     'y [3 4 9 16]}
    (transpile/plotly {:data ['{:x x
                                :y y
                                :mode "markers"
                                :type "scatter"}]}))


(-> [{:x 1 :y 1}
     {:x 2 :y 4}
     {:x 3 :y 9}
     {:x 4 :y 16}]
    (transpile/vega-embed {:data {:values 'data}
                           :mark "point"
                           :encoding {:x {:field "x" :type "quantitative"}
                                      :y {:field "y" :type "quantitative"}}}))

(-> [[1 2]
     [2 4]
     [3 9]
     [4 16]]
    (transpile/highcharts {:chart {:type "scatter"}
                           :series [{:data 'data}]}))


(-> {'center [-37.84 144.95]
     'zoom 11
     'provider "OpenStreetMap.Mapnik"
     'marker [-37.8 144.8]
     'popup "Here we are.<br> Exactly here."}
    (transpile/leaflet '(fn [m]
                          (m.setView center zoom)
                          (-> (L.tileLayer.provider provider)
                              (. (addTo m)))
                          (-> marker
                              L.marker
                              (. (addTo m))
                              (. (bindPopup popup))
                              (. (openPopup))))))







