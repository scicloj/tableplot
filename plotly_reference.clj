(ns tableplot-book.plotly-reference
  (:require [scicloj.tableplot.v1.plotly :as plotly]
            [scicloj.tableplot.v1.dag :as dag]
            [scicloj.kindly.v4.kind :as kind]
            [aerial.hanami.common :as hc]
            [clojure.string :as str]))

;; ## Key reference

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
