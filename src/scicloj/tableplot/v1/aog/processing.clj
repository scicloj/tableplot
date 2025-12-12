(ns scicloj.tableplot.v1.aog.processing
  "Processing pipeline: Layer → ProcessedLayer → Entry.

  Converts user-facing Layer specifications into backend-agnostic Entry IR.
  Applies statistical transformations and scale inference."
  (:require [scicloj.tableplot.v1.aog.ir :as ir]
            [scicloj.tableplot.v1.aog.transforms :as transforms]
            [scicloj.tableplot.v1.aog.scales :as scales]
            [tablecloth.api :as tc]))

;;; =============================================================================
;;; Data utilities

(defn- dataset?
  "Check if x is a tech.ml.dataset."
  [x]
  (instance? tech.v3.dataset.impl.dataset.Dataset x))

(defn- normalize-data
  "Convert various data formats to a common map-of-vectors format.

  Accepts:
  - tech.ml.dataset
  - tablecloth dataset
  - map of vectors {:x [1 2 3] :y [4 5 6]}
  - vector of maps [{:x 1 :y 4} {:x 2 :y 5}]"
  [data]
  (cond
    ;; nil data
    (nil? data)
    nil

    ;; Already a dataset
    (dataset? data)
    data

    ;; Map of vectors - convert to dataset
    (and (map? data)
         (every? vector? (vals data)))
    (tc/dataset data)

    ;; Vector of maps - convert to dataset
    (and (vector? data)
         (every? map? data))
    (tc/dataset data)

    ;; Unknown format
    :else
    (throw (ex-info "Unsupported data format"
                    {:data data
                     :type (type data)}))))

(defn- extract-column
  "Extract a column from data by key.

  Args:
  - data: Dataset or map-of-vectors
  - col-key: Column key (keyword or string)

  Returns:
  - Vector of values"
  [data col-key]
  (cond
    (nil? data)
    nil

    (dataset? data)
    (vec (get data col-key))

    (map? data)
    (get data col-key)

    :else
    (throw (ex-info "Cannot extract column from data"
                    {:data data
                     :col-key col-key}))))

;;; =============================================================================
;;; Simple processing (no transformations)

(defn- process-positional
  "Extract positional data from layer.

  Args:
  - layer: Layer map
  - data: Normalized dataset
  - transformation: The transformation that was applied (for special handling)

  Returns:
  - Vector of data vectors for positional args"
  [layer data transformation]
  (let [base-positional (mapv #(extract-column data %) (:positional layer))]
    ;; Handle transformations that create implicit y-columns
    (cond
      ;; Density creates a :density column that should be y
      (= transformation :density)
      (if (= 1 (count base-positional))
        (conj base-positional (extract-column data :density))
        base-positional)

      ;; Histogram creates a :count column that should be y
      (or (= transformation :histogram)
          (and (vector? transformation) (= :histogram (first transformation))))
      (if (= 1 (count base-positional))
        (conj base-positional (extract-column data :count))
        base-positional)

      ;; Frequency creates a :count column that should be y
      (= transformation :frequency)
      (if (= 1 (count base-positional))
        (conj base-positional (extract-column data :count))
        base-positional)

      ;; Expectation keeps the same positional structure (x, y)
      (= transformation :expectation)
      base-positional

      ;; Default: use base positional
      :else
      base-positional)))

(defn- process-named
  "Extract named aesthetic data from layer.

  Args:
  - layer: Layer map
  - data: Normalized dataset

  Returns:
  - Map of aesthetic -> data vector"
  [layer data]
  (into {}
        (for [[aesthetic col-key] (:named layer)]
          ;; Scale configs (:x-scale, :y-scale) are already in the correct format (maps)
          ;; Pass them through unchanged instead of trying to extract as columns
          (if (#{:x-scale :y-scale} aesthetic)
            [aesthetic col-key]  ; col-key is already the scale config map
            [aesthetic (extract-column data col-key)]))))

(defn- infer-plottype
  "Infer plot type from layer if not explicitly set.

  Simple heuristic:
  - If :plottype is set, use it
  - Otherwise, default to :scatter"
  [layer]
  (or (:plottype layer)
      :scatter))

(defn- identify-categorical-aesthetics
  "Identify which named aesthetics map to categorical columns.
  
  Returns vector of column keys that should be used for grouping."
  [layer data]
  (when-let [named (:named layer)]
    (vec
     (for [[aesthetic col-key] named
           :when (and (keyword? col-key) ; Is a column mapping
                      ;; Categorical aesthetics
                      (#{:color :colour :shape :linetype :group} aesthetic))]
       col-key))))

(defn- extract-primary-aesthetics
  "Extract categorical aesthetics that define grouping.
  
  Returns map of aesthetic -> unique values."
  [layer data]
  (when-let [named (:named layer)]
    (into {}
          (for [[aesthetic col-key] named
                :when (and (keyword? col-key)
                           (#{:color :colour :shape :linetype :group} aesthetic))]
            (let [values (extract-column data col-key)]
              [aesthetic (vec (sort (distinct values)))])))))

(defn- generate-labels
  "Generate axis/legend labels from layer specification.
  
  Uses column keys as labels by default."
  [layer]
  (let [positional (:positional layer)
        named (:named layer)
        pos-labels (zipmap [:x :y :z] (map name positional))
        named-labels (into {}
                           (for [[aesthetic col-key] named
                                 :when (keyword? col-key)]
                             [aesthetic (name col-key)]))]
    (merge pos-labels named-labels)))

(defn- generate-scale-mapping
  "Generate scale mapping for aesthetics.
  
  Each aesthetic gets a unique scale ID."
  [primary-aesthetics]
  (into {}
        (for [[aesthetic _] primary-aesthetics]
          [aesthetic (keyword (str (name aesthetic) "-scale"))])))

(defn layer->processed-layer
  "Convert a Layer to a ProcessedLayer.
  
  This applies transformations and extracts all the information
  needed for scale inference and rendering.
  
  Args:
  - layer: Layer map
  
  Returns:
  - ProcessedLayer map"
  [layer]
  (let [data (normalize-data (:data layer))

        ;; Get transformation for later use
        transform (:transformation layer)

        ;; Identify grouping columns
        group-cols (identify-categorical-aesthetics layer data)

        ;; Apply transformation
        transformed-data
        (if transform
          (let [x-col (first (:positional layer))
                y-col (second (:positional layer))]
            (transforms/apply-transform transform data x-col y-col group-cols))
          data)

        ;; Extract primary (categorical) aesthetics for grouping
        primary (extract-primary-aesthetics layer transformed-data)

        ;; Extract positional data (pass transform for special handling)
        positional (process-positional layer transformed-data transform)

        ;; Extract named data
        named-data (process-named layer transformed-data)

        ;; Merge with static attributes
        attributes (or (:attributes layer) {})
        named (merge named-data attributes)

        ;; Generate labels
        labels (generate-labels layer)

        ;; Generate scale mapping
        scale-mapping (generate-scale-mapping primary)

        ;; Infer plottype
        plottype (infer-plottype layer)]

    {:primary primary
     :positional positional
     :named named
     :labels labels
     :scale-mapping scale-mapping
     :plottype plottype
     :attributes attributes}))

(defn processed-layer->entry
  "Convert a ProcessedLayer to an Entry.
  
  This is the final step before backend rendering.
  
  Args:
  - processed-layer: ProcessedLayer map
  
  Returns:
  - Entry map"
  [processed-layer]
  (ir/entry (:plottype processed-layer)
            (:positional processed-layer)
            (:named processed-layer)))

(defn layers->processed-layers
  "Convert layers to ProcessedLayers.
  
  Args:
  - layers: Vector of Layer maps
  
  Returns:
  - Vector of ProcessedLayer maps"
  [layers]
  (mapv layer->processed-layer layers))

(defn processed-layers->entries
  "Convert ProcessedLayers to Entries.
  
  Args:
  - processed-layers: Vector of ProcessedLayer maps
  
  Returns:
  - Vector of Entry maps"
  [processed-layers]
  (mapv processed-layer->entry processed-layers))

(defn layer->entry
  "Convert a Layer to an Entry, applying transformations if specified.

  Args:
  - layer: Layer map

  Returns:
  - Entry map

  Example:
  (layer->entry
    {:transformation :linear
     :data {:x [1 2 3] :y [4 5 6]}
     :positional [:x :y]
     :named {:color :species}
     :plottype :line
     :attributes {:alpha 0.5}})"
  [layer]
  (let [data (normalize-data (:data layer))

        ;; Get transformation for later use
        transform (:transformation layer)

        ;; Identify grouping columns from categorical aesthetics
        group-cols (identify-categorical-aesthetics layer data)

        ;; Apply transformation if present
        transformed-data
        (if transform
          (let [x-col (first (:positional layer))
                y-col (second (:positional layer))]
            (transforms/apply-transform transform data x-col y-col group-cols))
          data)

        ;; Extract positional and named data from transformed data
        positional (process-positional layer transformed-data transform)
        named-data (process-named layer transformed-data)
        attributes (or (:attributes layer) {})

        ;; Merge named data with static attributes
        named (merge named-data attributes)
        plottype (infer-plottype layer)]
    (ir/entry plottype positional named)))

;;; =============================================================================
;;; Processing multiple layers

(defn layers->entries
  "Convert a collection of layers to entries.
  
  This is a simplified pipeline that goes directly from Layer to Entry
  for backward compatibility.

  Args:
  - layers: Vector of Layer maps

  Returns:
  - Vector of Entry maps"
  [layers]
  (mapv layer->entry layers))

(defn layers->axis-entries
  "Convert layers to a complete AxisEntries specification.
  
  This is the full pipeline: Layer → ProcessedLayer → Entry + Scales.
  
  Args:
  - layers: Vector of Layer maps
  - axis-config: Optional axis configuration (defaults to cartesian at [0 0])
  
  Returns:
  - AxisEntries map with entries, scales, and processed layers"
  ([layers]
   (layers->axis-entries layers {:type :cartesian
                                 :position [0 0]
                                 :attributes {}}))
  ([layers axis-config]
   (let [;; Process layers
         processed-layers (layers->processed-layers layers)

         ;; Convert to entries
         entries (processed-layers->entries processed-layers)

         ;; Infer scales from entries
         scales (scales/infer-scales entries)

         ;; Create AxisEntries
         axis-entries (ir/axis-entries
                       axis-config
                       entries
                       (:categorical-scales scales)
                       (:continuous-scales scales)
                       processed-layers)]
     axis-entries)))

(comment
  ;; REPL experiments

  (require '[scicloj.tableplot.v1.aog.core :as aog])

  ;; Simple layer
  (def simple-layer
    (aog/* (aog/data {:x [1 2 3] :y [4 5 6]})
           (aog/mapping :x :y)
           (aog/scatter {:alpha 0.5})))

  (layer->entry simple-layer)
  ;; => {:plottype :scatter
  ;;     :positional [[1 2 3] [4 5 6]]
  ;;     :named {:alpha 0.5}}

  ;; Multiple layers
  (def multi-layers
    (aog/* (aog/data {:x [1 2 3] :y [4 5 6]})
           (aog/mapping :x :y)
           (aog/+ (aog/scatter {:alpha 0.5})
                  (aog/line))))

  (layers->entries multi-layers)
  ;; => [{:plottype :scatter ...}
  ;;     {:plottype :line ...}]
  )
