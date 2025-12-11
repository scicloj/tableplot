(ns scicloj.tableplot.v1.aog.vegalite
  "Vega-Lite backend for AlgebraOfGraphics.

  Converts Entry IR to Vega-Lite specifications for SVG rendering."
  (:require [scicloj.tableplot.v1.aog.ir :as ir]
            [clojure.data.json :as json]
            [applied-science.darkstar :as darkstar]
            [scicloj.kindly.v4.kind :as kind]))

;;; =============================================================================
;;; Plottype mapping

(def plottype->mark
  "Map AoG plot types to Vega-Lite mark types."
  {:scatter "point"
   :line "line"
   :bar "bar"
   :histogram "bar" ; Will add histogram transform
   :heatmap "rect"
   :density "area"
   :box "boxplot"
   :violin "violin"})

;;; =============================================================================
;;; Color mapping

(def vegalite-categorical-colors
  "Default D3 categorical color palette (Vega-Lite default)."
  ["#1f77b4" "#ff7f0e" "#2ca02c" "#d62728" "#9467bd"
   "#8c564b" "#e377c2" "#7f7f7f" "#bcbd22" "#17becf"])

(defn- categorical-color?
  "Check if a value needs categorical color mapping."
  [v]
  (and (or (keyword? v)
           (symbol? v)
           (and (string? v)
                (not (re-matches #"^#[0-9a-fA-F]{6}$" v))
                (not (re-matches #"^(rgb|rgba|hsl|hsla)\(.*\)$" v))))
       (not (number? v))))

(defn- infer-type
  "Infer Vega-Lite type from data array."
  [data-array]
  (if (empty? data-array)
    :quantitative
    (let [first-val (first data-array)]
      (cond
        (string? first-val) :nominal
        (keyword? first-val) :nominal
        (number? first-val) :quantitative
        :else :nominal))))

;;; =============================================================================
;;; Data format conversion

(defn- positional->vegalite-data
  "Convert positional data arrays to Vega-Lite data format.

  Vega-Lite expects data as a vector of row objects.

  Args:
  - positional: Vector of data arrays (e.g., [[1 2 3] [4 5 6]])
  - named: Map of named attributes that contain data arrays

  Returns:
  - Map with :values key containing row-oriented data"
  [positional named]
  (let [;; Determine field names based on number of positional args
        pos-fields (case (count positional)
                     0 []
                     1 [:x]
                     2 [:x :y]
                     3 [:x :y :z]
                     (concat [:x :y :z]
                             (map #(keyword (str "pos" %)) (range (- (count positional) 3)))))

        ;; Extract data arrays from named attributes (for color, size, etc.)
        named-data-keys (filter #(let [v (get named %)]
                                   (and (sequential? v)
                                        (not (string? v))))
                                (keys named))

        ;; Create row maps
        n-rows (if (seq positional)
                 (count (first positional))
                 (if (seq named-data-keys)
                   (count (get named (first named-data-keys)))
                   0))

        ;; Use vec and mapv to ensure eager evaluation
        rows (vec
              (for [i (range n-rows)]
                (merge
                 ;; Positional fields
                 (into {} (map-indexed (fn [idx arr]
                                         [(nth pos-fields idx) (nth arr i)])
                                       positional))
                 ;; Named data fields - keep as keywords
                 (into {} (map (fn [k]
                                 [k (nth (get named k) i)])
                               named-data-keys)))))]

    {:values rows}))

;;; =============================================================================
;;; Encoding construction

(defn- build-encoding [positional named plottype]
  (let [;; positional is a vector of data arrays
        x-data (first positional)
        y-data (second positional)
        color-data (get named :color)
        size-data (get named :size)

        x-type (infer-type x-data)
        y-type (infer-type y-data)

        base-encoding {:x {:field :x
                           :type x-type
                           :scale {:zero false}}
                       :y {:field :y
                           :type y-type
                           :scale {:zero false}}}

        encoding-with-color (if color-data
                              (assoc base-encoding :color {:field :color
                                                           :type (infer-type color-data)})
                              base-encoding)

        encoding-with-size (if size-data
                             (assoc encoding-with-color :size {:field :size
                                                               :type :quantitative})
                             encoding-with-color)]
    encoding-with-size))

;;; =============================================================================
;;; Mark properties

(defn- build-mark-properties
  "Build Vega-Lite mark properties from named attributes.

  Extracts static visual properties (not data-driven aesthetics).

  Args:
  - named: Map of named attributes
  - mark-type: Mark type string

  Returns:
  - Map of mark properties with keyword keys"
  [named mark-type]
  (let [props {}

        ;; Opacity
        props (if-let [opacity (or (:alpha named) (:opacity named))]
                (if (number? opacity)
                  (assoc props :opacity opacity)
                  props)
                props)

        ;; Size (if static)
        props (if-let [size (:size named)]
                (if (number? size)
                  (assoc props :size size)
                  props)
                props)

        ;; Color (if static)
        props (if-let [color (:color named)]
                (if (and (not (sequential? color))
                         (or (string? color) (keyword? color)))
                  (assoc props :color (name color))
                  props)
                props)

        ;; Line width
        props (if-let [width (:width named)]
                (assoc props :strokeWidth width)
                props)]

    props))

;;; =============================================================================
;;; Entry to Vega-Lite spec conversion

(defn entry->vegalite-spec
  "Convert an Entry to a Vega-Lite specification.

  Args:
  - entry: Entry map {:plottype, :positional, :named}

  Returns:
  - Vega-Lite spec map (plain Clojure map with keyword keys)

  Example:
  (entry->vegalite-spec
    {:plottype :scatter
     :positional [[1 2 3] [4 5 6]]
     :named {:alpha 0.5}})
  => {:$schema \"https://vega.github.io/schema/vega-lite/v5.json\"
      :mark {:type \"point\" :opacity 0.5}
      :encoding {:x {:field :x :type :quantitative}
                 :y {:field :y :type :quantitative}}
      :data {:values [{:x 1 :y 4} {:x 2 :y 5} {:x 3 :y 6}]}}"
  [entry]
  (let [plottype (:plottype entry)
        positional (:positional entry)
        named (:named entry)
        mark-type (plottype->mark plottype)

        ;; Build components
        data (positional->vegalite-data positional named)
        encoding (build-encoding positional named plottype)
        mark-props (build-mark-properties named mark-type)

        ;; Mark can be string or map with properties
        mark (if (seq mark-props)
               (merge {:type mark-type} mark-props)
               mark-type)

        ;; Construct spec with keyword keys
        spec {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
              :mark mark
              :encoding encoding
              :data data}]

    spec))

;;; =============================================================================
;;; SVG rendering

(defn entry->svg
  "Convert an Entry to SVG via Vega-Lite.

  Args:
  - entry: Entry map

  Returns:
  - SVG string"
  [entry]
  (let [spec (entry->vegalite-spec entry)
        spec-json (json/write-str spec)
        svg (darkstar/vega-lite-spec->svg spec-json)]
    svg))

(defn entries->svg
  "Convert multiple entries to layered SVG via Vega-Lite.

  Args:
  - entries: Vector of Entry maps

  Returns:
  - SVG string"
  [entries]
  (if (= 1 (count entries))
    ;; Single entry - simple spec
    (entry->svg (first entries))
    ;; Multiple entries - use Vega-Lite layer composition
    (let [;; Convert each entry to a layer spec (mark + encoding only)
          layers (mapv (fn [entry]
                         (let [plottype (:plottype entry)
                               positional (:positional entry)
                               named (:named entry)
                               mark-type (plottype->mark plottype)
                               mark-props (build-mark-properties named mark-type)
                               encoding (build-encoding positional named plottype)

                               ;; Mark can be string or map with properties
                               mark (if (seq mark-props)
                                      (merge {"type" mark-type} mark-props)
                                      mark-type)]

                           {"mark" mark
                            "encoding" encoding}))
                       entries)

          ;; All entries should share the same data
          ;; Use data from first entry (they should all be the same in AoG)
          first-entry (first entries)
          data (positional->vegalite-data (:positional first-entry) (:named first-entry))

          ;; Construct layered spec
          spec {"$schema" "https://vega.github.io/schema/vega-lite/v5.json"
                "data" data
                "layer" layers}

          ;; Convert to JSON and render
          spec-json (json/write-str spec)
          svg (darkstar/vega-lite-spec->svg spec-json)]

      svg)))

(defn entries->vegalite-spec
  "Convert multiple entries to a Vega-Lite spec (not SVG).

  Args:
  - entries: Vector of Entry maps

  Returns:
  - Vega-Lite spec (map with keyword keys)"
  [entries]
  (if (= 1 (count entries))
    ;; Single entry - simple spec
    (entry->vegalite-spec (first entries))
    ;; Multiple entries - use Vega-Lite layer composition
    ;; Each layer gets its own data since transformations may produce different datasets
    (let [;; Convert each entry to a layer spec with its own data
          layers (mapv (fn [entry]
                         (let [plottype (:plottype entry)
                               positional (:positional entry)
                               named (:named entry)
                               mark-type (plottype->mark plottype)
                               mark-props (build-mark-properties named mark-type)
                               encoding (build-encoding positional named plottype)
                               data (positional->vegalite-data positional named)

                               ;; Mark can be string or map with properties
                               mark (if (seq mark-props)
                                      (merge {:type mark-type} mark-props)
                                      mark-type)]

                           {:mark mark
                            :encoding encoding
                            :data data}))
                       entries)

          ;; Construct layered spec with keyword keys
          spec {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                :layer layers}]

      spec)))

(defn vegalite
  "Convert entries to a Vega-Lite spec with Kindly metadata.

  This is the main entry point for rendering in notebooks.

  Args:
  - entries: Vector of Entry maps or single Entry map
  - opts: Optional map (reserved for future use, currently ignored)

  Returns:
  - Vega-Lite spec with Kindly :vega-lite kind metadata"
  ([entries]
   (vegalite entries {}))
  ([entries opts]
   (let [entries-vec (if (vector? entries) entries [entries])
         spec (entries->vegalite-spec entries-vec)]
     ;; Don't pass opts to kind/vega-lite - it expects Kindly options not Vega-Lite options
     (kind/vega-lite spec))))

;;; =============================================================================
;;; Testing and validation

(defn validate-entry
  "Validate an Entry and return Vega-Lite spec for inspection.

  Args:
  - entry: Entry map

  Returns:
  - Map with :entry, :spec, :valid? keys"
  [entry]
  (try
    (let [spec (entry->vegalite-spec entry)]
      {:entry entry
       :spec spec
       :valid? true})
    (catch Exception e
      {:entry entry
       :error (ex-message e)
       :valid? false})))

(comment
  ;; REPL experiments

  ;; Simple scatter plot entry
  (def scatter-entry
    {:plottype :scatter
     :positional [[1 2 3] [4 5 6]]
     :named {:alpha 0.5}})

  (entry->vegalite-spec scatter-entry)

  ;; Generate SVG
  (def svg (entry->svg scatter-entry))

  ;; Save for inspection
  (spit "/tmp/vegalite-scatter.svg" svg)

  ;; Line plot
  (def line-entry
    {:plottype :line
     :positional [[1 2 3] [4 5 6]]
     :named {:width 2}})

  (entry->vegalite-spec line-entry)

  ;; Colored scatter plot
  (def colored-scatter
    {:plottype :scatter
     :positional [[1 2 3 4] [5 6 7 8]]
     :named {:color [:a :b :a :b]
             :alpha 0.7}})

  (entry->vegalite-spec colored-scatter)
  (spit "/tmp/vegalite-colored.svg" (entry->svg colored-scatter)))
