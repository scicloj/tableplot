(ns tableplot.v2.inference
  "Inference rules for automatic plot generation.
  
  Rules infer scales, guides, and other plot elements from data and aesthetics."
  (:require [tableplot.v2.dataflow :as df]
            [tech.v3.dataset :as ds]
            [tech.v3.datatype.functional :as dfn]))

;;; ============================================================================
;;; Helper Functions
;;; ============================================================================

(defn column-type
  "Determine the type of a column: :quantitative, :ordinal, or :nominal"
  [dataset col-name]
  (when dataset
    (let [col (ds/column dataset col-name)
          dtype (:datatype (meta col))]
      (cond
        (nil? col) nil
        (#{:int16 :int32 :int64 :float32 :float64} dtype) :quantitative
        (#{:string :keyword} dtype) :nominal
        :else :nominal))))

(defn infer-scale-type
  "Infer scale type from column type"
  [col-type]
  (case col-type
    :quantitative :linear
    :nominal :ordinal
    :ordinal :ordinal
    :linear))

(defn column-domain
  "Get the domain (min/max or unique values) of a column"
  [dataset col-name]
  (when dataset
    (let [col (ds/column dataset col-name)
          col-type (column-type dataset col-name)]
      (case col-type
        :quantitative [(dfn/reduce-min col) (dfn/reduce-max col)]
        :nominal (vec (distinct (seq col)))
        :ordinal (vec (distinct (seq col)))
        nil))))

;;; ============================================================================
;;; Data Inference
;;; ============================================================================

(defn infer-data
  "Infer data from context or use nil as default"
  [spec context]
  nil)

(df/register-inference-rule! :=data infer-data)

;;; ============================================================================
;;; Layer Inference
;;; ============================================================================

(defn infer-layers
  "Default to empty layers if none specified"
  [spec context]
  [])

(df/register-inference-rule! :=layers infer-layers)

;;; ============================================================================
;;; Scale Inference
;;; ============================================================================

(defn infer-scale
  "Infer a scale definition for an aesthetic channel.
  
  Looks at:
  - The field mapped to this aesthetic
  - The data type of that field
  - The data domain
  
  Returns a scale definition map."
  [spec context aesthetic]
  (let [field-key (keyword (str "=" (name aesthetic) "-field"))
        field (df/get-substitution spec field-key)
        data (df/get-substitution spec :=data)]
    (if (and field data)
      (let [col-type (column-type data field)
            scale-type (infer-scale-type col-type)
            domain (column-domain data field)]
        {:type scale-type
         :domain domain
         :field field})
      ;; No field or data, return minimal scale
      {:type :linear})))

(defn infer-x-scale [spec context]
  (infer-scale spec context :x))

(defn infer-y-scale [spec context]
  (infer-scale spec context :y))

(defn infer-color-scale [spec context]
  (infer-scale spec context :color))

(defn infer-size-scale [spec context]
  (infer-scale spec context :size))

(df/register-inference-rule! :=x-scale infer-x-scale)
(df/register-inference-rule! :=y-scale infer-y-scale)
(df/register-inference-rule! :=color-scale infer-color-scale)
(df/register-inference-rule! :=size-scale infer-size-scale)

;;; ============================================================================
;;; Guide Inference (axes, legends)
;;; ============================================================================

(defn infer-axis
  "Infer an axis guide from a scale"
  [spec context aesthetic]
  (let [scale-key (keyword (str "=" (name aesthetic) "-scale"))
        scale (df/get-substitution spec scale-key)
        field (:field scale)]
    (when scale
      {:type :axis
       :orientation (case aesthetic
                      :x :bottom
                      :y :left
                      :bottom)
       :scale scale
       :title (when field (name field))})))

(defn infer-legend
  "Infer a legend guide from a scale"
  [spec context aesthetic]
  (let [scale-key (keyword (str "=" (name aesthetic) "-scale"))
        scale (df/get-substitution spec scale-key)
        field (:field scale)]
    (when (and scale (#{:ordinal :nominal} (:type scale)))
      {:type :legend
       :scale scale
       :title (when field (name field))})))

(defn infer-x-guide [spec context]
  (infer-axis spec context :x))

(defn infer-y-guide [spec context]
  (infer-axis spec context :y))

(defn infer-color-guide [spec context]
  (infer-legend spec context :color))

(defn infer-size-guide [spec context]
  (infer-legend spec context :size))

;; Mark these rules as depending on scale rules
(alter-meta! #'infer-x-guide assoc ::df/depends-on #{:=x-scale})
(alter-meta! #'infer-y-guide assoc ::df/depends-on #{:=y-scale})
(alter-meta! #'infer-color-guide assoc ::df/depends-on #{:=color-scale})
(alter-meta! #'infer-size-guide assoc ::df/depends-on #{:=size-scale})

(df/register-inference-rule! :=x-guide infer-x-guide)
(df/register-inference-rule! :=y-guide infer-y-guide)
(df/register-inference-rule! :=color-guide infer-color-guide)
(df/register-inference-rule! :=size-guide infer-size-guide)

;;; ============================================================================
;;; Other Inference
;;; ============================================================================

(defn infer-facets
  "No facets by default"
  [spec context]
  nil)

(defn infer-theme
  "Default theme"
  [spec context]
  {:background :white
   :font-family "sans-serif"})

(defn infer-title
  "Get user-provided title from :=labels or auto-generate from aesthetics.
  
  Priority:
  1. User-provided title via labs(:title ...)
  2. Auto-generated from x/y fields"
  [spec context]
  (let [user-labels (df/get-substitution spec :=labels)
        user-title (:title user-labels)]
    (or user-title
        ;; Auto-generate if no user title
        (let [x-field (df/get-substitution spec :=x-field)
              y-field (df/get-substitution spec :=y-field)]
          (when (and x-field y-field)
            (str (name y-field) " vs " (name x-field)))))))

(defn infer-labels
  "Infer labels from user-provided :=labels or generate defaults.
  
  User can provide labels via labs() function which sets :=labels.
  Otherwise, generate default labels from field names."
  [spec context]
  (let [user-labels (df/get-substitution spec :=labels)
        x-field (df/get-substitution spec :=x-field)
        y-field (df/get-substitution spec :=y-field)]
    (merge
     ;; Default labels from field names
     (when x-field {:x (name x-field)})
     (when y-field {:y (name y-field)})
     ;; User-provided labels override defaults
     user-labels)))

(df/register-inference-rule! :=facets infer-facets)
(df/register-inference-rule! :=theme infer-theme)
(df/register-inference-rule! :=title infer-title)

(df/register-inference-rule! :=labels infer-labels)

;;; ============================================================================
;;; Field Inference (from layers)
;;; ============================================================================

(defn infer-x-field
  "Infer x field from global aesthetics or first layer.
  
  Supports hierarchical aesthetics (ggplot2-style):
  - Local aesthetics override global (layer :local-aes :x takes precedence)
  - Falls back to global aesthetics (:=global-aes :x)
  - Falls back to layer's direct :x (V2 API compatibility)"
  [spec context]
  (let [global-aes (df/get-substitution spec :=global-aes)
        layers (df/get-substitution spec :=layers)
        first-layer (first layers)]
    (or
     ;; Priority 1: Local aesthetics (override global)
     (when first-layer
       (get-in first-layer [:local-aes :x]))
     ;; Priority 2: Global aesthetics
     (:x global-aes)
     ;; Priority 3: Direct layer :x (V2 API style)
     (when first-layer
       (:x first-layer)))))

(defn infer-y-field
  "Infer y field from global aesthetics or first layer.
  
  Supports hierarchical aesthetics (ggplot2-style):
  - Local aesthetics override global (layer :local-aes :y takes precedence)
  - Falls back to global aesthetics (:=global-aes :y)
  - Falls back to layer's direct :y (V2 API compatibility)"
  [spec context]
  (let [global-aes (df/get-substitution spec :=global-aes)
        layers (df/get-substitution spec :=layers)
        first-layer (first layers)]
    (or
     ;; Priority 1: Local aesthetics (override global)
     (when first-layer
       (get-in first-layer [:local-aes :y]))
     ;; Priority 2: Global aesthetics
     (:y global-aes)
     ;; Priority 3: Direct layer :y (V2 API style)
     (when first-layer
       (:y first-layer)))))

(defn infer-color-field
  "Infer color field from global aesthetics or first layer.
  
  Supports hierarchical aesthetics (ggplot2-style):
  - Local aesthetics override global (layer :local-aes :color takes precedence)
  - Falls back to global aesthetics (:=global-aes :color)
  - Falls back to layer's direct :color (V2 API compatibility)"
  [spec context]
  (let [global-aes (df/get-substitution spec :=global-aes)
        layers (df/get-substitution spec :=layers)
        first-layer (first layers)]
    (or
     ;; Priority 1: Local aesthetics (override global)
     (when first-layer
       (get-in first-layer [:local-aes :color]))
     ;; Priority 2: Global aesthetics
     (:color global-aes)
     ;; Priority 3: Direct layer :color (V2 API style)
     (when first-layer
       (:color first-layer)))))

(defn infer-size-field
  "Infer size field from global aesthetics or first layer.
  
  Supports hierarchical aesthetics (ggplot2-style):
  - Local aesthetics override global (layer :local-aes :size takes precedence)
  - Falls back to global aesthetics (:=global-aes :size)
  - Falls back to layer's direct :size (V2 API compatibility)"
  [spec context]
  (let [global-aes (df/get-substitution spec :=global-aes)
        layers (df/get-substitution spec :=layers)
        first-layer (first layers)]
    (or
     ;; Priority 1: Local aesthetics (override global)
     (when first-layer
       (get-in first-layer [:local-aes :size]))
     ;; Priority 2: Global aesthetics
     (:size global-aes)
     ;; Priority 3: Direct layer :size (V2 API style)
     (when first-layer
       (:size first-layer)))))

(df/register-inference-rule! :=x-field infer-x-field)
(df/register-inference-rule! :=y-field infer-y-field)
(df/register-inference-rule! :=color-field infer-color-field)
(df/register-inference-rule! :=size-field infer-size-field)
