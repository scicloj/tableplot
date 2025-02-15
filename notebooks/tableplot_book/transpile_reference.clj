;; # Transpile API reference 📖 - experimental 🧪

;; Sometimes, data visualization in the browser requires not only
;; plain JSON structures but also some Javascript code.

;; One way to generate such code from Clojure is through
;; [std.lang](https://clojureverse.org/t/std-lang-a-universal-template-transpiler/),
;; a universal transpiler from Clojure to many languages.

;; The `transpile` API of Tableplot provides functions
;; for using this practice in combination with various JS libraries.
;; *It is considered experimental at this stage.*

;; ## Setup 🔨

;; In this tutorial, we use:

;; * The Tableplot `transpile` API namepace
;; * [Tablecloth](https://scicloj.github.io/tablecloth/) for dataset processing and column processing
;; * the datasets defined in the [Datasets chapter](./tableplot_book.datasets.html)
;; * [Kindly](https://scicloj.github.io/kindly-noted/kindly) to annotate the kind of way certain values should be displayed

(ns tableplot-book.transpile-reference
  (:require [scicloj.tableplot.v1.transpile :as transpile]
            [tablecloth.api :as tc]
            [scicloj.metamorph.ml.rdatasets :as rdatasets]
            [clojure.string :as str]
            [scicloj.kindly.v4.kind :as kind]))

^:kindly/hide-code
(require '[scicloj.tableplot.v1.book-utils :as book-utils])


;; ## Functions ⚙

(book-utils/include-fnvar-as-section #'transpile/js)

;; #### Examples

(kind/code
 (transpile/js '(var x 9)
               '(return (+ x 11))))


;; An adaptation of a Plotly.js example - 
;; [Animations in Javascript](https://plotly.com/javascript/animations/):

(kind/hiccup
 [:div
  [:button {:onclick (transpile/js '(randomize))}
   "Randomize!"]
  [:div {:style {:height "auto"}}
   [:script
    (transpile/js
     '(var plotly-animaton-element document.currentScript.parentElement)
     '(Plotly.newPlot plotly-animaton-element
                      [{:x [1 2 3]
                        :y [0 0.5 1]
                        :line {:simplify false}}])
     '(defn randomize []
        (Plotly.animate plotly-animaton-element
                        {:data [{:y [(Math.random)
                                     (Math.random)
                                     (Math.random)]}]
                         :traces [0]
                         :layout {}}
                        {:transition {:duration 500
                                      :easing "cut-in-out"}
                         :frame {:duration 500}})))]]]
 {:html/deps [:plotly]})

(book-utils/include-fnvar-as-section #'transpile/div-with-script)

;; #### Examples

;; Let us create an Apache Echarts plot.

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

;; #### A closer look

;; Let us create a Plotly.js plot with some custom event handling.
;; We will explore the resulting structure a little bit.

(def clickable-example
  (transpile/div-with-script
   ;; data (with symbol bindings)
   {'x [1 2 3 4]
    'y [3 4 9 16]}
   ;; script
   ['(var plotly-animaton-element document.currentScript.parentElement)
    '(Plotly.newPlot plotly-animaton-element
                     {:data
                      [{:x x
                        :y y
                        :mode "markers"
                        :type "scatter"
                        :marker {:size 20}}]
                      :layout {:title
                               "Would you please click the points?"}})
    '(. plotly-animaton-element
        (on "plotly_click"
            (fn []
              (alert "Thanks for clicking."))))]
   ;; Kindly options
   {:html/deps [:plotly]}))

;; First, let us see it visualized:

clickable-example

;; Now, let us check the metadata:

(meta clickable-example)

;; Now, let us pretty-print it to see the Hiccup structure.

(kind/pprint
 clickable-example)

;; Let us focus on the code of the script:

(-> clickable-example
    second
    second
    kind/code)

(book-utils/include-fnvar-as-section #'transpile/echarts)

;; #### Examples

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

(-> (rdatasets/datasets-mtcars)
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


(book-utils/include-fnvar-as-section #'transpile/plotly)

;; #### Examples

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

(book-utils/include-fnvar-as-section #'transpile/vega-embed)

;; #### Example

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

(book-utils/include-fnvar-as-section #'transpile/highcharts)

;; #### Example

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

(book-utils/include-fnvar-as-section #'transpile/leaflet)

;; #### Example

(transpile/leaflet
 ;; data with symbol bindings
 {'center [-37.84 144.95]
  'zoom 11
  'provider "OpenStreetMap.Mapnik"
  'marker [-37.9 144.8]
  'popup "<i style='color:purple'>Have you been here?</i>"}
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

;; ## Danymic vars 🎧

(book-utils/include-fnvar-as-section #'transpile/*base-kindly-options*)

transpile/*base-kindly-options*




