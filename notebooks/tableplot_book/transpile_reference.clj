;; # Transpile API reference 📖

;; Sometimes, data visualization in the browser requires not only
;; plain JSON structures but also some Javascript code.

;; One way to generate such code from Clojure is through
;; [std.lang](https://clojureverse.org/t/std-lang-a-universal-template-transpiler/),
;; a universal transpiler from Clojure to many languages.

;; The `tanspile` API of Tableplot provides convenience functions
;; for achieving this in typical data visualization contexts.
;; It is considered experimental at this stage.

;; The `std.lang` transpiler itself is already stable.
;; We are working on creating relevant documentation and tutorials
;; to clarify its usage.

;; This chapter is a detailed refernce of Tableplot's Transpile API.

;; ## Setup 🔨

;; In this tutorial, we use:

;; * The Tableplot `transpile` API namepace
;; * [Tablecloth](https://scicloj.github.io/tablecloth/) for dataset processing and column processing
;; * the datasets defined in the [Datasets chapter](./tableplot_book.datasets.html)

(ns tableplot-book.transpile-reference
  (:require [scicloj.tableplot.v1.transpile :as transpile]
            [tablecloth.api :as tc]
            [tableplot-book.datasets :as datasets]))

(transpile/div-with-script
 ;; data (with symbol bindings)
 {'x [1 2 3 4]
  'y [3 4 9 16]}
 ;; script
 ['(var el document.currentScript.parentElement)
  '(Plotly.newPlot el
                   {:data
                    [{:x x
                      :y y
                      :mode "markers"
                      :type "scatter"
                      :marker {:size 20}}]
                    :layout {:title
                             "Would you please click the points?"}})
  '(. el
      (on "plotly_click"
          (fn []
            (alert "Thanks for clicking."))))]
 ;; Kindly options
 {:html/deps [:plotly]})


(transpile/div-with-script
 ;; data
 [[1 2]
  [2 4]
  [3 9]
  [4 16]]
 ;; script
 ['(var myChart
        (echarts.init
         document.currentScript.parentElement))
  (list 'myChart.setOption
        {:xAxis {}
         :yAxis {}
         :series [{:type "scatter"
                   :data 'data}]})]
 ;; Kindly options (merged with default)
 {:html/deps [:echarts]
  :style {:height "300px"
          :background "floralwhite"}})


(transpile/echarts
 ;; data
 [[1 2]
  [2 4]
  [3 9]
  [4 16]]
 ;; form
 {:xAxis {}
  :yAxis {}
  :series [{:type "scatter"
            :data 'data}]})



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


(transpile/plotly
 ;; data
 {:x [1 2 3 4]
  :y [3 4 9 16]}
 ;; form
 {:data ['{:x data.x
           :y data.y
           :mode "markers"
           :type "scatter"}]})

(transpile/plotly
 ;; data with symbol bindings
 {'x [1 2 3 4]
  'y [3 4 9 16]}
 ;; form
 {:data ['{:x x
           :y y
           :mode "markers"
           :type "scatter"}]})


(transpile/vega-embed
 ;; data
 [{:x 1 :y 1}
  {:x 2 :y 4}
  {:x 3 :y 9}
  {:x 4 :y 16}]
 ;; form
 {:data {:values 'data}
  :mark "point"
  :encoding {:x {:field "x" :type "quantitative"}
             :y {:field "y" :type "quantitative"}}})


(transpile/highcharts
 ;; data
 [[1 2]
  [2 4]
  [3 9]
  [4 16]]
 ;; form
 {:title {:text "a scatterplot"}
  :chart {:type "scatter"}
  :series [{:data 'data}]})


(transpile/leaflet
 ;; data with symbol bindings
 {'center [-37.84 144.95]
  'zoom 11
  'provider "OpenStreetMap.Mapnik"
  'marker [-37.8 144.8]
  'popup "Here we are.<br> Exactly here."}
 ;; form
 '(fn [m]
    (m.setView center zoom)
    (-> (L.tileLayer.provider provider)
        (. (addTo m)))
    (-> marker
        L.marker
        (. (addTo m))
        (. (bindPopup popup))
        (. (openPopup)))))







