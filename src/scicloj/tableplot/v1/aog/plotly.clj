(ns scicloj.tableplot.v1.aog.plotly
  "Plotly.js backend for AlgebraOfGraphics.

  Converts Entry IR to Plotly.js trace specifications."
  (:require [scicloj.tableplot.v1.aog.ir :as ir]
            [scicloj.kindly.v4.kind :as kind]))

;;; =============================================================================
;;; Plottype mapping

(def plottype->trace-type
  "Map AoG plot types to Plotly trace types."
  {:scatter "scatter"
   :line "scatter" ; mode will be "lines"
   :bar "bar"
   :histogram "histogram"
   :heatmap "heatmap"
   :surface "surface"
   :contour "contour"
   :density "scatter" ; Will use mode "lines" with filled area
   :box "box"
   :violin "violin"})

(def plottype->mode
  "Map AoG plot types to Plotly mode (for scatter traces)."
  {:scatter "markers"
   :line "lines"
   :density "lines"})

;;; =============================================================================
;;; Color mapping

(def plotly-categorical-colors
  "Default Plotly categorical color palette."
  ["#1f77b4" "#ff7f0e" "#2ca02c" "#d62728" "#9467bd"
   "#8c564b" "#e377c2" "#7f7f7f" "#bcbd22" "#17becf"])

(defn- categorical-color?
  "Check if a value needs categorical color mapping.
  
  Returns true if the value is categorical (keyword, string, symbol)
  and not already a hex color code."
  [v]
  (and (or (keyword? v)
           (symbol? v)
           (and (string? v)
                ;; Not a hex color code
                (not (re-matches #"^#[0-9a-fA-F]{6}$" v))
                ;; Not a CSS color name (rgb, rgba, etc.)
                (not (re-matches #"^(rgb|rgba|hsl|hsla)\(.*\)$" v))))
       (not (number? v)))) ; Not numeric

(defn- map-categorical-colors
  "Map categorical values to Plotly color strings.

  Args:
  - values: Vector of categorical values (keywords, strings, etc.)

  Returns:
  - Vector of hex color strings"
  [values]
  (let [unique-vals (distinct values)
        val->color (zipmap unique-vals (cycle plotly-categorical-colors))]
    (mapv val->color values)))

;;; =============================================================================
;;; Attribute mapping

(defn- map-attributes
  "Map Entry :named attributes to Plotly trace attributes.

  Handles both aesthetic mappings (data vectors) and static attributes.

  Args:
  - named: Map from Entry :named field
  - plottype: Plot type keyword

  Returns:
  - Map of Plotly trace attributes"
  [named plottype]
  (let [result {}
        ;; Add mode for scatter-type plots
        result (if-let [mode (plottype->mode plottype)]
                 (assoc result :mode mode)
                 result)
        ;; Determine if this is a line or marker plot
        is-line-plot? (contains? #{:line :density} plottype)]

    ;; Process each attribute
    (reduce-kv
     (fn [acc k v]
       (case k
          ;; Aesthetic mappings (data vectors)
         :color (let [color-vals (if (and (sequential? v)
                                          (seq v)
                                          (categorical-color? (first v)))
                                   (map-categorical-colors v)
                                   v)]
                  (if is-line-plot?
                    (assoc-in acc [:line :color] color-vals)
                    (assoc-in acc [:marker :color] color-vals)))
         :size (assoc-in acc [:marker :size] v)
         :symbol (assoc-in acc [:marker :symbol] v)

          ;; Static visual attributes
         :alpha (assoc acc :opacity v)
         :opacity (assoc acc :opacity v)
         :width (assoc-in acc [:line :width] v)

          ;; Marker attributes
         :marker-size (assoc-in acc [:marker :size] v)
         :marker-color (let [color-vals (if (and (sequential? v)
                                                 (seq v)
                                                 (categorical-color? (first v)))
                                          (map-categorical-colors v)
                                          v)]
                         (assoc-in acc [:marker :color] color-vals))
         :marker-symbol (assoc-in acc [:marker :symbol] v)

          ;; Line attributes
         :line-width (assoc-in acc [:line :width] v)
         :line-color (assoc-in acc [:line :color] v)
         :line-dash (assoc-in acc [:line :dash] v)

          ;; Default: pass through
         (assoc acc k v)))
     result
     named)))

;;; =============================================================================
;;; Entry to Plotly trace conversion

(defn entry->plotly-trace
  "Convert an Entry to a Plotly.js trace specification.

  Args:
  - entry: Entry map {:plottype, :positional, :named}

  Returns:
  - Plotly trace map (plain Clojure map ready for JSON serialization)

  Example:
  (entry->plotly-trace
    {:plottype :scatter
     :positional [[1 2 3] [4 5 6]]
     :named {:alpha 0.5}})
  => {:type \"scatter\"
      :mode \"markers\"
      :x [1 2 3]
      :y [4 5 6]
      :opacity 0.5}"
  [entry]
  (let [plottype (:plottype entry)
        positional (:positional entry)
        named (:named entry)
        trace-type (plottype->trace-type plottype)

        ;; Build base trace
        trace {:type trace-type}

        ;; Add positional data (x, y, z, etc.)
        trace (case (count positional)
                0 trace
                1 (assoc trace :x (first positional))
                2 (assoc trace :x (first positional)
                         :y (second positional))
                3 (assoc trace :x (first positional)
                         :y (second positional)
                         :z (nth positional 2))
                ;; More than 3 positional args - store extras
                (assoc trace :x (first positional)
                       :y (second positional)
                       :z (nth positional 2)
                       :extra (drop 3 positional)))

        ;; Map attributes
        attrs (map-attributes named plottype)

        ;; Merge attributes into trace
        trace (merge trace attrs)]

    trace))

;;; =============================================================================
;;; Multiple entries to Plotly figure

(defn entries->plotly
  "Convert multiple entries to a Plotly figure specification.

  Args:
  - entries: Vector of Entry maps

  Returns:
  - Plotly figure spec {:data [...traces...] :layout {...}}

  Example:
  (entries->plotly
    [{:plottype :scatter :positional [[1 2 3] [4 5 6]] :named {:alpha 0.5}}
     {:plottype :line :positional [[1 2 3] [5 6 7]] :named {}}])
  => {:data [{:type \"scatter\" :mode \"markers\" :x [...] :y [...] :opacity 0.5}
             {:type \"scatter\" :mode \"lines\" :x [...] :y [...]}]
      :layout {}}"
  ([entries]
   (entries->plotly entries {}))
  ([entries layout]
   {:data (mapv entry->plotly-trace entries)
    :layout (or layout {})}))

(defn plotly
  "Convert entries to a Plotly spec with Kindly metadata.

  This is the main entry point for rendering.

  Args:
  - entries: Vector of Entry maps or single Entry map
  - layout: Optional layout configuration map

  Returns:
  - Plotly spec with Kindly :plotly kind metadata"
  ([entries]
   (plotly entries {}))
  ([entries layout]
   (let [entries-vec (if (vector? entries) entries [entries])
         spec (entries->plotly entries-vec layout)]
     (kind/plotly spec))))

(comment
  ;; REPL experiments

  ;; Simple scatter plot entry
  (def scatter-entry
    {:plottype :scatter
     :positional [[1 2 3] [4 5 6]]
     :named {:alpha 0.5}})

  (entry->plotly-trace scatter-entry)
  ;; => {:type "scatter"
  ;;     :mode "markers"
  ;;     :x [1 2 3]
  ;;     :y [4 5 6]
  ;;     :opacity 0.5}

  ;; Line plot
  (def line-entry
    {:plottype :line
     :positional [[1 2 3] [4 5 6]]
     :named {:width 2}})

  (entry->plotly-trace line-entry)
  ;; => {:type "scatter"
  ;;     :mode "lines"
  ;;     :x [1 2 3]
  ;;     :y [4 5 6]
  ;;     :line {:width 2}}

  ;; Multiple entries
  (entries->plotly [scatter-entry line-entry])
  ;; => {:data [{...scatter...} {...line...}]
  ;;     :layout {}}
  )
