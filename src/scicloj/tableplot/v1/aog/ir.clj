(ns scicloj.tableplot.v1.aog.ir
  "Intermediate representation (IR) for AlgebraOfGraphics-style plotting.

  Defines the core data structures following AoG's architecture:
  - Layer: User-facing algebraic object (data + mappings + transformation)
  - ProcessedLayer: After statistical transformations applied
  - Entry: Backend-agnostic plot specification (the key IR)
  - AxisEntries: Collection of entries with scales and axis config

  All structures are plain maps validated with Malli schemas."
  (:require [malli.core :as m]
            [malli.error :as me]))

;;; =============================================================================
;;; Malli Schemas (following AlgebraOfGraphics.jl structure)

(def Layer
  "User-facing layer specification.

  Fields:
  - :transformation - Statistical transform function (or nil for identity)
  - :data - Dataset (tech.ml.dataset, tablecloth, vector of maps, or nil)
  - :positional - Vector of positional mappings (column keys or selectors)
  - :named - Map of aesthetic name -> column key/selector

  Example:
  {:transformation nil
   :data penguins-dataset
   :positional [:bill-length :bill-depth]
   :named {:color :species}}"
  [:map {:closed true}
   [:transformation [:maybe fn?]]
   [:data :any] ; Can be dataset, nil, or vector of maps
   [:positional [:vector :any]]
   [:named [:map-of :keyword :any]]])

(def ProcessedLayer
  "Layer after statistical transformations and grouping.

  Fields:
  - :primary - Categorical aesthetics for grouping (map of aesthetic -> values)
  - :positional - Processed positional arguments (vector of data arrays)
  - :named - Processed named arguments (map of aesthetic -> data)
  - :labels - Axis/legend labels (map of aesthetic -> label string)
  - :scale-mapping - Map of aesthetic -> scale ID
  - :plottype - Resolved plot type keyword (:scatter, :line, :bar, etc.)
  - :attributes - Visual attributes (map)

  Example:
  {:primary {:color [:Adelie :Chinstrap :Gentoo]}
   :positional [[39.1 39.5 ...] [18.7 17.4 ...]]
   :named {:marker-size [5 5 6 ...]}
   :labels {:x \"Bill Length (mm)\" :y \"Bill Depth (mm)\"}
   :scale-mapping {:color :color-1}
   :plottype :scatter
   :attributes {:opacity 0.7}}"
  [:map {:closed true}
   [:primary [:map-of :keyword :any]]
   [:positional [:vector :any]]
   [:named [:map-of :keyword :any]]
   [:labels [:map-of :keyword :string]]
   [:scale-mapping [:map-of :keyword :any]]
   [:plottype :keyword]
   [:attributes :map]])

(def Entry
  "Backend-agnostic plot specification (the IR!).

  This is the key intermediate representation that separates
  grammar processing from rendering. An Entry contains everything
  needed to draw a single plot without backend-specific details.

  Fields:
  - :plottype - Plot type (:scatter, :line, :bar, :heatmap, :surface, etc.)
  - :positional - Vector of actual data arrays in positional order
  - :named - Map of attribute keyword -> actual values

  Example:
  {:plottype :scatter
   :positional [[1 2 3] [4 5 6]]  ; x and y data
   :named {:marker-color [0 0 1 1 2 2]
           :marker-size 10
           :opacity 0.3}}"
  [:map {:closed true}
   [:plottype [:enum :scatter :line :bar :histogram :heatmap :surface
               :contour :density :box :violin]]
   [:positional [:vector :any]]
   [:named [:map-of :keyword :any]]])

(def AxisConfig
  "Axis configuration.

  Fields:
  - :type - Axis type (:cartesian, :3d, :polar, etc.)
  - :position - Grid position [row col]
  - :attributes - Axis-specific attributes (labels, limits, scales, etc.)

  Example:
  {:type :cartesian
   :position [0 0]
   :attributes {:xlabel \"Bill Length (mm)\"
                :ylabel \"Bill Depth (mm)\"
                :xlim [30 60]
                :ylim [10 25]}}"
  [:map {:closed true}
   [:type :keyword]
   [:position [:vector {:min 2 :max 2} :int]]
   [:attributes :map]])

(def CategoricalScale
  "Categorical scale mapping discrete values to visual attributes.

  Example:
  {:id :color-1
   :aesthetic :color
   :domain [:Adelie :Chinstrap :Gentoo]
   :range [\"#1f77b4\" \"#ff7f0e\" \"#2ca02c\"]}"
  [:map {:closed true}
   [:id :keyword]
   [:aesthetic :keyword]
   [:domain [:vector :any]]
   [:range [:vector :any]]])

(def ContinuousScale
  "Continuous scale mapping continuous values to visual attributes.

  Example:
  {:id :size-1
   :aesthetic :size
   :domain [0 100]
   :range [5 20]}"
  [:map {:closed true}
   [:id :keyword]
   [:aesthetic :keyword]
   [:domain [:vector {:min 2 :max 2} number?]]
   [:range [:vector {:min 2 :max 2} number?]]])

(def AxisEntries
  "Complete specification for a single axis/subplot.

  Contains all entries to be plotted on the same axis, along with
  scale information and the axis configuration.

  Fields:
  - :axis-config - Axis configuration (AxisConfig schema)
  - :entries - Vector of Entry records to plot
  - :categorical-scales - Map of scale-id -> CategoricalScale
  - :continuous-scales - Map of scale-id -> ContinuousScale
  - :processed-layers - Original ProcessedLayers (for legend generation)

  Example:
  {:axis-config {:type :cartesian, :position [0 0], :attributes {...}}
   :entries [{:plottype :scatter, :positional [...] :named {...}}
             {:plottype :line, :positional [...] :named {...}}]
   :categorical-scales {:color-1 {...}}
   :continuous-scales {:size-1 {...}}
   :processed-layers [...]}"
  [:map {:closed true}
   [:axis-config AxisConfig]
   [:entries [:vector Entry]]
   [:categorical-scales [:map-of :keyword CategoricalScale]]
   [:continuous-scales [:map-of :keyword ContinuousScale]]
   [:processed-layers [:vector ProcessedLayer]]])

(def FigureSpec
  "Complete figure specification with multiple axes.

  Fields:
  - :axes - Matrix (vector of vectors) of AxisEntries
  - :figure-attributes - Figure-level attributes (size, title, etc.)

  Example:
  {:axes [[{:axis-config {...} :entries [...]}
           {:axis-config {...} :entries [...]}]]
   :figure-attributes {:width 800 :height 600 :title \"Penguin Analysis\"}}"
  [:map {:closed true}
   [:axes [:vector [:vector AxisEntries]]]
   [:figure-attributes :map]])

;;; =============================================================================
;;; Validation helpers

(defn validate
  "Validate data against a schema. Returns data if valid, throws if invalid."
  [schema data]
  (if (m/validate schema data)
    data
    (throw (ex-info "Schema validation failed"
                    {:errors (me/humanize (m/explain schema data))
                     :schema (m/form schema)
                     :data data}))))

(defn valid?
  "Check if data matches schema. Returns boolean."
  [schema data]
  (m/validate schema data))

(defn explain
  "Get human-readable explanation of validation errors."
  [schema data]
  (when-let [explanation (m/explain schema data)]
    (me/humanize explanation)))

;;; =============================================================================
;;; Constructors with validation

(defn layer
  "Create a Layer map with validation.

  Args:
  - transformation: Statistical transform function (default: nil)
  - data: Dataset or data source (default: nil)
  - positional: Vector of positional mappings (default: [])
  - named: Map of aesthetic mappings (default: {})

  Example:
  (layer nil penguins [:bill-length :bill-depth] {:color :species})"
  ([transformation data positional named]
   (validate Layer
             {:transformation transformation
              :data data
              :positional positional
              :named named}))
  ([data positional named]
   (layer nil data positional named))
  ([data positional]
   (layer nil data positional {}))
  ([]
   (layer nil nil [] {})))

(defn entry
  "Create an Entry map with validation.

  Args:
  - plottype: Plot type keyword
  - positional: Vector of data arrays
  - named: Map of attribute keyword -> value

  Example:
  (entry :scatter [[1 2 3] [4 5 6]] {:marker-size 10})"
  [plottype positional named]
  (validate Entry
            {:plottype plottype
             :positional positional
             :named named}))

(defn axis-entries
  "Create an AxisEntries map with validation.

  Args:
  - axis-config: AxisConfig map
  - entries: Vector of Entry maps
  - categorical-scales: Map of scale-id -> CategoricalScale
  - continuous-scales: Map of scale-id -> ContinuousScale
  - processed-layers: Vector of ProcessedLayer maps

  Example:
  (axis-entries
    {:type :cartesian :position [0 0] :attributes {}}
    [{:plottype :scatter :positional [...] :named {...}}]
    {}
    {}
    [])"
  [axis-config entries categorical-scales continuous-scales processed-layers]
  (validate AxisEntries
            {:axis-config axis-config
             :entries entries
             :categorical-scales categorical-scales
             :continuous-scales continuous-scales
             :processed-layers processed-layers}))

;;; =============================================================================
;;; Schema registry (for instrumentation)

(def schemas
  "Registry of all IR schemas for instrumentation and testing."
  {:layer Layer
   :processed-layer ProcessedLayer
   :entry Entry
   :axis-config AxisConfig
   :categorical-scale CategoricalScale
   :continuous-scale ContinuousScale
   :axis-entries AxisEntries
   :figure-spec FigureSpec})

(comment
  ;; REPL experiments

  ;; Create a valid layer
  (def my-layer
    (layer nil
           {:x [1 2 3] :y [4 5 6]}
           [:x :y]
           {:color :species}))

  ;; Validate manually
  (valid? Layer my-layer)
  ;; => true

  ;; Invalid layer
  (explain Layer {:oops "wrong"})
  ;; => {:transformation ["missing required key"]
  ;;     :data ["missing required key"]
  ;;     ...}

  ;; Create a valid entry
  (def my-entry
    (entry :scatter
           [[1 2 3] [4 5 6]]
           {:marker-size 10
            :opacity 0.7}))

  (valid? Entry my-entry)
  ;; => true
  )
