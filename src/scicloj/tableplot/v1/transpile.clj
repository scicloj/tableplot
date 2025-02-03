(ns scicloj.tableplot.v1.transpile
  (:require [scicloj.kindly.v4.kind :as kind]
            [std.lang :as l]
            [charred.api :as charred]
            [clojure.string :as str]))

(defn- js-transpile
  "Transpile the given Clojure `forms` to Javascript code."
  [forms]
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

(def base-style {:height "400px"
                 :width "100%"})

(defn hiccup
  "Create a general transpiled data visualization.

  Given a data structure `data` and a `spec` map with keys
  `body`, `script`, `style`, and `deps`,
  crate a correspondinh Hiccup structure for a data visualization.

  The structure will be a `:div` with the following inside:
  - the `style` (merged over `base-style`)
  - the body
  - a Javascript `:script` part

  Also, add the relevant metadata of the `deps` as Kindly options
  metadata, to make sure these deps are available when rendering the hiccup.
  
  The `:script` is created from `script` with some possible
  variable bindings preceeding it.
  The preceeding variable bindings result from `data`:
  - A Javscript variable `data` is bound to the `data` converted to JSON,
  and thus can be referred to from the Echarts specification.
  - If `data` is a map that has some keys of symbol type, then
  corresponding Javascript variables named by these symbols
  are bound to the corresponding values converted to JSON.

  If only one argument is provided, then it is considered the
  `form`, with no data binding."
  ([spec]
   (hiccup nil spec))
  ([data {:as spec
          :keys [style body script deps]}]
   (kind/hiccup
    [:div
     {:style (merge base-style style)}
     body
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
        [(js-transpile script)]))]]
    {:html/deps deps})))

(defn echarts
  "Create an Echarts data visualization.

  Given a data structure `data` and a `spec` map with keys
  `form`, `body`, `script`, `style`, and `deps`, create
  a hiccup structure using `hiccup` with the 
  the given `data` and the spec resulting by
  merging the given `spec` over the following defaults:
  - `:script` - a script for applying an Echarts
  visualization specified in `form`.
  - `:deps` - `[:echarts]`."
  ([spec]
   (echarts nil spec))
  ([data {:as spec
          :keys [form]}]
   (hiccup data
           (merge {:script ['(var myChart
                                  (echarts.init document.currentScript.parentElement))
                            (list 'myChart.setOption form)]
                   :deps [:echarts]}
                  spec))))



