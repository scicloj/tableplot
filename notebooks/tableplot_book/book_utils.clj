(ns tableplot-book.book-utils
  (:require [clojure.string :as str]
            [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly.v4.kind :as kind]))

(defn include-form [form]
  (format "`%s`" (pr-str form)))

(defn include-key-or-symbol-name [s]
  (format "[`%s`](#%s)"
          s
          (-> s (str/replace #"^\:\=" ""))))

(def symbol-or-key-pattern
  #"`[a-z|\-|\:|\=]+`")

(def subkey-pattern
  #"`\:\=[a-z|\-]+`")

(defn known-symbol? [s]
  (case (str *ns*)
    "tableplot-book.plotly-reference"
    (-> s
        (str/replace #"`" "")
        symbol
        ('#{base layer
            layer-point layer-line layer-bar layer-boxplot layer-violin layer-segment layer-text layer-heatmap layer-surface
            layer-histogram layer-histogram2d layer-density layer-smooth layer-correlation
            plot
            debug
            smooth-stat histogram-stat density-stat correlatoion-stat
            imshow surface splom}))
    "tableplot-book.transpile-reference"
    ((ns-publics 'scicloj.tableplot.v1.transpile) s)
    ;; else
    nil))

(defn enrich-text-with-links [text]
  (or (some->> text
               (re-seq symbol-or-key-pattern)
               distinct
               (reduce (fn [current-text s]
                         (if (or (re-matches subkey-pattern s)
                                 (known-symbol? s))
                           (str/replace current-text
                                        s
                                        (-> s
                                            (str/replace #"[`]" "")
                                            include-key-or-symbol-name))
                           current-text))
                       text))
      text))

(defn f->deps [f]
  (->> f
       meta
       :scicloj.tableplot.v1.dag/dep-ks
       (map include-key-or-symbol-name)
       (str/join " ")))

(defn include-dag-fn [f]
  (format "%s\n\n **by default depends on**: %s"
          (-> f
              meta
              :doc
              enrich-text-with-links)
          (f->deps f)))

(defn include-dag-fn-as-section [fnsymbol f]
  (-> (format "### `%s`\n%s"
              (pr-str fnsymbol)
              (include-dag-fn f))
      kind/md
      kindly/hide-code))

(defn include-fnvar-as-section [fnvar]
  (-> (let [{:keys [name arglists doc]} (meta fnvar)]
        (str (format "### `%s`\n" name)
             (->> arglists
                  (map (fn [l]
                         (->> l
                              pr-str
                              (format "`%s`\n\n"))))
                  (str/join ""))
             (enrich-text-with-links doc)))
      kind/md
      kindly/hide-code))

^:kindly/hide-code
(defn md [& strings]
  (->> strings
       (str/join " ")
       enrich-text-with-links
       kind/md
       kindly/hide-code))
