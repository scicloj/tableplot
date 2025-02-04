(ns scicloj.tableplot.v1.transpile
  (:require [scicloj.kindly.v4.kind :as kind]
            [std.lang :as l]
            [charred.api :as charred]
            [clojure.string :as str]
            [tablecloth.api :as tc]
            [tableplot-book.datasets :as datasets]
            [scicloj.kindly.v4.api :as kindly]))

(defn- js-transpile
  "Transpile the given Clojure `forms` to Javascript code."
  [forms]
  ((l/ptr :js)
   (cons 'do forms)))

(defn- js-assignment [symbol data]
  (format "let %s = %s;"
          symbol
          (charred/write-json-str data)))

(defn- js-entry-assignment [symbol0 symbol1 symbol2]
  (format "let %s = %s['%s'];"
          symbol0
          symbol1
          symbol2))

(defn- js-closure [js-statements]
  (->> js-statements
       (str/join "\n")
       (format "(function () {\n%s\n})();")))

(def base-style {:height :auto})

(defn div-with-script
  "Create a general transpiled data visualization.

  Given a data structure `data` and a form `script`,
  create a correspondinh Hiccup structure for a data visualization.

  The structure will be a `:div` with a `:script` part.

  The `:script` is created from `script` with some possible
  variable bindings preceeding it:
  - A Javscript variable `data` is bound to the `data` value
  converted to JSON.
  - If `data` is a map that has some keys of symbol type, then
  corresponding Javascript variables named by these symbols
  are bound to the corresponding values converted to JSON."
  ([data script kindly-options]
   (kind/hiccup
    [:div
     [:script
      (js-closure
       (concat
        (when data
          [(js-assignment 'data data)])
        (when (map? data)
          (->> data
               (map (fn [[k v]]
                      (when (symbol? k)
                        (js-entry-assignment k 'data k))))
               (remove nil?)))
        [(js-transpile script)]))]]
    (kindly/deep-merge {:style base-style}
                       kindly-options))))


(defn echarts
  ([form]
   (echarts nil form))
  ([data form]
   (div-with-script
    data
    ['(var chart
           (echarts.init document.currentScript.parentElement))
     (list 'chart.setOption form)]
    {:style {:height "400px"}
     :html/deps [:echarts]})))


(defn plotly
  ([form]
   (plotly nil form))
  ([data form]
   (div-with-script
    data
    [(list 'Plotly.newPlot
           'document.currentScript.parentElement
           (:data form)
           (:layout form)
           (:config form))]
    {:html/deps [:plotly]})))

(defn vega-embed
  ([form]
   (vega-embed nil form))
  ([data form]
   (div-with-script
    data
    [(list 'vegaEmbed
           'document.currentScript.parentElement
           form)]
    {:html/deps [:vega]})))

(defn highcharts
  ([form]
   (highcharts nil form))
  ([data form]
   (div-with-script
    data
    [(list 'Highcharts.chart
           'document.currentScript.parentElement
           form)]
    {:html/deps [:highcharts]})))


(defn leaflet
  ([form]
   (leaflet nil form))
  ([data form]
   (div-with-script
    data
    [(list 'var 'f form)
     '(var m (L.map document.currentScript.parentElement))
     '(f m)]
    {:html/deps [:leaflet]})))


