(ns tableplot.v2.api
  "High-level API for creating plots.
  
  Functions in this namespace are syntactic sugar that compile down to
  spec transformations. Each function adds substitutions to the spec map."
  (:require [tableplot.v2.dataflow :as df]
            [tableplot.v2.inference :as infer]))

;;; ============================================================================
;;; Layer Builders
;;; ============================================================================

(defn point
  "Create a point (scatter) layer.
  
  Usage:
    (point :x :sepal-width :y :sepal-length)
    (point :x :sepal-width :y :sepal-length :color :species)
    (point :x :sepal-width :y :sepal-length :color :species :size :petal-width)"
  [& {:keys [x y color size] :as aesthetics}]
  (merge {:mark :point} aesthetics))

(defn line
  "Create a line layer"
  [& {:keys [x y color size] :as aesthetics}]
  (merge {:mark :line} aesthetics))

(defn bar
  "Create a bar layer"
  [& {:keys [x y color] :as aesthetics}]
  (merge {:mark :bar} aesthetics))

(defn area
  "Create an area layer"
  [& {:keys [x y color] :as aesthetics}]
  (merge {:mark :area} aesthetics))

;;; ============================================================================
;;; Plot Construction
;;; ============================================================================

(defn plot
  "Create a plot from data and layers.
  
  Usage:
    (plot iris (point :x :sepal-width :y :sepal-length))
    (plot iris 
      (point :x :sepal-width :y :sepal-length :color :species))
    (plot iris
      (point :x :sepal-width :y :sepal-length)
      (line :x :sepal-width :y :sepal-length))"
  [data & layers]
  (-> (df/make-spec df/base-plot-template)
      (df/with-data data)
      (df/with-layers layers)))

(defn base-plot
  "Create an empty plot spec for incremental building"
  []
  (df/make-spec df/base-plot-template))

;;; ============================================================================
;;; Incremental Spec Building (threading-friendly)
;;; ============================================================================

(defn add-layer
  "Add a layer to an existing spec"
  [spec layer]
  (df/with-layer spec layer))

(defn add-data
  "Add data to an existing spec"
  [spec data]
  (df/with-data spec data))

(defn add-title
  "Add a title to an existing spec"
  [spec title]
  (df/with-title spec title))

(defn add-scale
  "Add a scale definition to an existing spec"
  [spec aesthetic scale-def]
  (df/with-scale spec aesthetic scale-def))

;;; ============================================================================
;;; Convenience Functions (Smart Defaults)
;;; ============================================================================

(defn scatter
  "Quick scatter plot with smart defaults.
  
  Usage:
    (scatter iris :sepal-width :sepal-length)
    (scatter iris :sepal-width :sepal-length :color :species)"
  [data x y & {:keys [color size]}]
  (plot data
        (point :x x :y y :color color :size size)))

(defn line-plot
  "Quick line plot with smart defaults"
  [data x y & {:keys [color]}]
  (plot data
        (line :x x :y y :color color)))

(defn histogram
  "Create a histogram.
  
  Usage:
    (histogram iris :sepal-width)
    (histogram iris :sepal-width :bins 20)"
  [data x & {:keys [bins]}]
  (plot data
        (bar :x x :y :count :bins (or bins 10))))

(defn auto-plot
  "Automatically choose plot type based on data types.
  
  - Two quantitative variables: scatter plot
  - One quantitative: histogram
  - One quantitative + one categorical: grouped bar chart"
  [data x & {:keys [y color]}]
  (cond
    ;; Both x and y: scatter
    y (scatter data x y :color color)

    ;; Just x: histogram
    :else (histogram data x)))

;;; ============================================================================
;;; Finalization
;;; ============================================================================

(defn finalize
  "Finalize a spec by running inference.
  
  This resolves all :=substitution-keys and returns a complete spec."
  [spec]
  (df/infer spec))

;;; ============================================================================
;;; Complete Flow (Convenience)
;;; ============================================================================

(defn render
  "Create and finalize a plot in one step.
  
  Usage:
    (render (scatter iris :sepal-width :sepal-length))
    
  Or with threading:
    (-> (base-plot)
        (add-data iris)
        (add-layer (point :x :sepal-width :y :sepal-length))
        render)"
  [spec]
  (finalize spec))

;;; ============================================================================
;;; Examples (in comment block)
;;; ============================================================================

(comment
  ;; Load some data
  (require '[tech.v3.dataset :as ds])
  (def iris (ds/->dataset "https://raw.githubusercontent.com/mwakom/seaborn-data/master/iris.csv"))

  ;; Simple scatter plot
  (render (scatter iris :sepal_width :sepal_length))

  ;; With color
  (render (scatter iris :sepal_width :sepal_length :color :species))

  ;; Incremental building
  (-> (base-plot)
      (add-data iris)
      (add-layer (point :x :sepal_width :y :sepal_length :color :species))
      (add-title "Iris Dataset")
      render)

  ;; Multiple layers
  (-> (plot iris
            (point :x :sepal_width :y :sepal_length :color :species)
            (line :x :sepal_width :y :sepal_length :color :species))
      render)

  ;; Inspect intermediate steps
  (def spec (scatter iris :sepal_width :sepal_length :color :species))

  ;; See the spec before inference
  spec

  ;; See just the substitutions
  (:sub/map spec)

  ;; See after inference
  (finalize spec))
