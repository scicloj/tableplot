(ns scicloj.tableplot.v1.transpile
  (:require [scicloj.kindly.v4.kind :as kind]
            [std.lang :as l]
            [charred.api :as charred]
            [clojure.string :as str]))

(defn- js-transpile
  "Transpile the given Clojure `forms` to Javascript code."
  [& forms]
  ((l/ptr :js)
   (cons 'do forms)))

(defn- js-assignment [symbol data]
  (format "let %s = %s;"
          symbol
          (charred/write-json-str data)))

(defn- js-closure [js-statements]
  (->> js-statements
       (str/join "\n")
       (format "(function () {\n%s\n})();")))

(defn echarts
  "Given a data structure `data` and a Clojure `form`,
  transpile both of them to Javascript and return
  a Hiccup block of a data visualization.

  The transpiled `form` is used as the Echarts specification, that
  is a data structure which may contain functions if necessary.

  A Javscript variable `data` is bound to the `data` converted to JSON,
  and thus can be referred to from the Echarts specification.

  If `data` is a map that has some keys of symbol type, then
  corresponding Javascript variables named by these symbols
  are bound to the corresponding values converted to JSON.

  If only one argument is provided, then it is considered the
  `form`, with no data binding."
  ([form]
   (echarts nil form))
  ([data form]
   (kind/hiccup
    [:div
     {:style {:height "400px"
              :width "100%"}}
     [:script
      (js-closure
       (concat
        (when data
          [(js-assignment 'data data)])
        (when (map? data)
          (->> data
               (map (fn [[k v]]
                      (when (symbol? k)
                        (js-assignment k v))))
               (remove nil?)))
        [(js-transpile '(var myChart
                             (echarts.init document.currentScript.parentElement))
                       (list 'myChart.setOption form))]))]]
    {:html/deps [:echarts]})))



