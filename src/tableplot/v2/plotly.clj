(ns tableplot.v2.plotly
  "Plotly.js backend for Tableplot V2.
  
  Converts finalized V2 specs (after inference) to Plotly.js traces for rendering."
  (:require [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]))

;;; =============================================================================
;;; Geom to Plotly type/mode mapping

(def geom->plotly-type
  "Map V2 geom types to Plotly trace types."
  {:point "scatter"
   :scatter "scatter"
   :line "scatter"
   :bar "bar"
   :histogram "histogram"
   :heatmap "heatmap"
   :box "box"
   :violin "violin"
   :contour "contour"
   :surface "surface"})

(def geom->plotly-mode
  "Map V2 geom types to Plotly mode (for scatter traces)."
  {:point "markers"
   :scatter "markers"
   :line "lines"
   :smooth "lines"})

;;; =============================================================================
;;; Color utilities

(def plotly-categorical-colors
  "Default Plotly categorical color palette."
  ["#1f77b4" "#ff7f0e" "#2ca02c" "#d62728" "#9467bd"
   "#8c564b" "#e377c2" "#7f7f7f" "#bcbd22" "#17becf"])

(defn- categorical-value?
  "Check if a value is categorical (keyword, string, symbol)."
  [v]
  (or (keyword? v)
      (symbol? v)
      (and (string? v)
           ;; Not a hex color code
           (not (re-matches #"^#[0-9a-fA-F]{6}$" v))
           ;; Not a CSS color
           (not (re-matches #"^(rgb|rgba|hsl|hsla)\(.*\)$" v)))))

(defn- map-categorical-colors
  "Map categorical values to Plotly colors.
  
  Args:
  - values: Vector of categorical values
  
  Returns:
  - Vector of hex color strings"
  [values]
  (let [unique-vals (distinct values)
        val->color (zipmap unique-vals (cycle plotly-categorical-colors))]
    (mapv val->color values)))

;;; =============================================================================
;;; Data extraction

(defn- extract-column
  "Extract a column from a dataset by field name.
  
  Args:
  - data: Dataset (map or tablecloth dataset)
  - field: Field name (keyword)
  
  Returns:
  - Vector of values, or nil if field is nil"
  [data field]
  (when field
    (cond
      ;; Plain map of vectors
      (map? data)
      (vec (get data field))

      ;; tech.ml.dataset / tablecloth dataset (has :tech.v3.dataset/dataset metadata)
      (and data (get (meta data) :tech.v3.dataset/dataset))
      (vec (get data field))

      ;; Try generic get (works for many dataset types)
      :else
      (vec (get data field)))))

;;; =============================================================================
;;; Layer to trace conversion

(defn- layer->trace
  "Convert a V2 layer to a Plotly trace.
  
  Args:
  - layer: Layer map {:geom, :local-aes, :attributes}
  - data: Dataset
  - spec: Full spec (for accessing global-aes and scales)
  
  Returns:
  - Plotly trace map"
  [layer data spec]
  (let [geom (or (:geom layer) (:mark layer))

        ;; Merge global and local aesthetics (local overrides global)
        global-aes (:global-aes spec)
        local-aes (:local-aes layer)
        merged-aes (merge global-aes local-aes)

        ;; Get field names from merged aesthetics
        x-field (:x merged-aes)
        y-field (:y merged-aes)
        color-field (:color merged-aes)
        size-field (:size merged-aes)

        ;; Extract positional data
        x-data (extract-column data x-field)
        y-data (extract-column data y-field)

        ;; Extract aesthetic data
        color-data (extract-column data color-field)
        size-data (extract-column data size-field)

        ;; Get static attributes from layer
        attributes (:attributes layer)

        ;; Determine if colors are categorical and need mapping
        color-values (when (and color-data
                                (seq color-data)
                                (categorical-value? (first color-data)))
                       (map-categorical-colors color-data))

        ;; Build base trace
        trace-type (geom->plotly-type geom "scatter")
        mode (geom->plotly-mode geom "markers")

        trace {:type trace-type
               :mode mode}

        ;; Add positional data
        trace (cond-> trace
                x-data (assoc :x x-data)
                y-data (assoc :y y-data))

        ;; Add aesthetics based on geom type
        is-line? (contains? #{:line :smooth} geom)

        trace (cond-> trace
                ;; Color
                (and color-values is-line?)
                (assoc-in [:line :color] (first color-values))

                (and color-values (not is-line?))
                (assoc-in [:marker :color] color-values)

                (and color-data (not color-values) is-line?)
                (assoc-in [:line :color] color-data)

                (and color-data (not color-values) (not is-line?))
                (assoc-in [:marker :color] color-data)

                ;; Size
                size-data
                (assoc-in [:marker :size] size-data)

                ;; Static attributes from layer
                (:alpha attributes)
                (assoc :opacity (:alpha attributes))

                (:width attributes)
                (assoc-in [:line :width] (:width attributes))

                (:marker-size attributes)
                (assoc-in [:marker :size] (:marker-size attributes))

                (and (:marker-color attributes) (not is-line?))
                (assoc-in [:marker :color] (:marker-color attributes))

                (and (:line-color attributes) is-line?)
                (assoc-in [:line :color] (:line-color attributes)))]

    trace))

;;; =============================================================================
;;; Layout construction

(defn- apply-theme-to-layout
  "Apply theme settings to Plotly layout.
  
  Maps ggplot2-style theme elements to Plotly layout properties.
  
  Args:
  - layout: Base Plotly layout map
  - theme: Theme map from spec
  
  Returns:
  - Layout map with theme applied"
  [layout theme]
  (if-not theme
    layout
    (cond-> layout
      ;; Plot background (paper)
      (get-in theme [:plot :background])
      (assoc :paper_bgcolor (get-in theme [:plot :background]))

      ;; Panel background (plot area)
      (get-in theme [:panel :background])
      (assoc :plot_bgcolor (get-in theme [:panel :background]))

      ;; Plot title font
      (get-in theme [:plot :title :font])
      (assoc-in [:title :font] (get-in theme [:plot :title :font]))

      ;; X-axis grid
      (get-in theme [:panel :grid :major :show])
      (assoc-in [:xaxis :showgrid] (get-in theme [:panel :grid :major :show]))

      (get-in theme [:panel :grid :major :color])
      (assoc-in [:xaxis :gridcolor] (get-in theme [:panel :grid :major :color]))

      (get-in theme [:panel :grid :major :width])
      (assoc-in [:xaxis :gridwidth] (get-in theme [:panel :grid :major :width]))

      ;; Y-axis grid
      (get-in theme [:panel :grid :major :show])
      (assoc-in [:yaxis :showgrid] (get-in theme [:panel :grid :major :show]))

      (get-in theme [:panel :grid :major :color])
      (assoc-in [:yaxis :gridcolor] (get-in theme [:panel :grid :major :color]))

      (get-in theme [:panel :grid :major :width])
      (assoc-in [:yaxis :gridwidth] (get-in theme [:panel :grid :major :width]))

      ;; Axis text (tick labels)
      (get-in theme [:axis :text :font])
      (assoc-in [:xaxis :tickfont] (get-in theme [:axis :text :font]))

      (get-in theme [:axis :text :font])
      (assoc-in [:yaxis :tickfont] (get-in theme [:axis :text :font]))

      ;; Axis title font
      (get-in theme [:axis :title :font])
      (assoc-in [:xaxis :title :font] (get-in theme [:axis :title :font]))

      (get-in theme [:axis :title :font])
      (assoc-in [:yaxis :title :font] (get-in theme [:axis :title :font]))

      ;; Axis lines
      (get-in theme [:axis :line :show])
      (assoc-in [:xaxis :showline] (get-in theme [:axis :line :show]))

      (get-in theme [:axis :line :show])
      (assoc-in [:yaxis :showline] (get-in theme [:axis :line :show]))

      (get-in theme [:axis :line :color])
      (assoc-in [:xaxis :linecolor] (get-in theme [:axis :line :color]))

      (get-in theme [:axis :line :color])
      (assoc-in [:yaxis :linecolor] (get-in theme [:axis :line :color]))

      (get-in theme [:axis :line :width])
      (assoc-in [:xaxis :linewidth] (get-in theme [:axis :line :width]))

      (get-in theme [:axis :line :width])
      (assoc-in [:yaxis :linewidth] (get-in theme [:axis :line :width]))

      ;; Axis ticks
      (get-in theme [:axis :ticks :show])
      (assoc-in [:xaxis :ticks] (if (get-in theme [:axis :ticks :show]) "outside" ""))

      (get-in theme [:axis :ticks :show])
      (assoc-in [:yaxis :ticks] (if (get-in theme [:axis :ticks :show]) "outside" ""))

      (get-in theme [:axis :ticks :color])
      (assoc-in [:xaxis :tickcolor] (get-in theme [:axis :ticks :color]))

      (get-in theme [:axis :ticks :color])
      (assoc-in [:yaxis :tickcolor] (get-in theme [:axis :ticks :color]))

      ;; Panel border
      (and (get-in theme [:panel :border :show])
           (get-in theme [:panel :border :color]))
      (assoc-in [:xaxis :linecolor] (get-in theme [:panel :border :color]))

      (and (get-in theme [:panel :border :show])
           (get-in theme [:panel :border :color]))
      (assoc-in [:yaxis :linecolor] (get-in theme [:panel :border :color]))

      (and (get-in theme [:panel :border :show])
           (get-in theme [:panel :border :width]))
      (assoc-in [:xaxis :linewidth] (get-in theme [:panel :border :width]))

      (and (get-in theme [:panel :border :show])
           (get-in theme [:panel :border :width]))
      (assoc-in [:yaxis :linewidth] (get-in theme [:panel :border :width]))

      ;; Legend
      (get-in theme [:legend :position])
      (assoc-in [:legend :x] (case (get-in theme [:legend :position])
                               "right" 1.02
                               "left" -0.02
                               "top" 0.5
                               "bottom" 0.5
                               "none" nil
                               1.02))

      (get-in theme [:legend :position])
      (assoc-in [:legend :y] (case (get-in theme [:legend :position])
                               "right" 1
                               "left" 1
                               "top" 1.02
                               "bottom" -0.15
                               "none" nil
                               1))

      (and (get-in theme [:legend :position])
           (= "none" (get-in theme [:legend :position])))
      (assoc :showlegend false)

      (get-in theme [:legend :background])
      (assoc-in [:legend :bgcolor] (get-in theme [:legend :background]))

      (get-in theme [:legend :text :font])
      (assoc-in [:legend :font] (get-in theme [:legend :text :font]))

      (and (get-in theme [:legend :border :show])
           (get-in theme [:legend :border :color]))
      (assoc-in [:legend :bordercolor] (get-in theme [:legend :border :color]))

      (and (get-in theme [:legend :border :show])
           (get-in theme [:legend :border :width]))
      (assoc-in [:legend :borderwidth] (get-in theme [:legend :border :width])))))

(defn- spec->layout
  "Build Plotly layout from V2 spec.
  
  Args:
  - spec: Finalized V2 spec
  
  Returns:
  - Plotly layout map"
  [spec]
  (let [labels (:labels spec)
        title (:title spec)
        scales (:scales spec)
        x-scale (:x scales)
        y-scale (:y scales)
        theme (:theme spec)

        ;; Build base layout
        base-layout (cond-> {}
                      ;; Title - convert string to map if needed for theme compatibility
                      title
                      (assoc :title (if (string? title)
                                      {:text title}
                                      title))

                      ;; X-axis label - convert string to map for theme compatibility
                      (:x labels)
                      (assoc-in [:xaxis :title] (let [label (:x labels)]
                                                  (if (string? label)
                                                    {:text label}
                                                    label)))

                      ;; Y-axis label - convert string to map for theme compatibility
                      (:y labels)
                      (assoc-in [:yaxis :title] (let [label (:y labels)]
                                                  (if (string? label)
                                                    {:text label}
                                                    label)))

                      ;; X-axis scale type
                      (= :log (:type x-scale))
                      (assoc-in [:xaxis :type] "log")

                      ;; Y-axis scale type
                      (= :log (:type y-scale))
                      (assoc-in [:yaxis :type] "log"))]

    ;; Apply theme on top of base layout
    (apply-theme-to-layout base-layout theme)))

;;; =============================================================================
;;; Main rendering API

(defn spec->traces
  "Convert all layers in a V2 spec to Plotly traces.
  
  Args:
  - spec: Finalized V2 spec (after inference)
  
  Returns:
  - Vector of Plotly trace maps"
  [spec]
  (let [data (:data spec)
        layers (:layers spec)]
    (mapv #(layer->trace % data spec) layers)))

(defn spec->plotly
  "Convert a finalized V2 spec to a Plotly figure spec.
  
  Args:
  - spec: Finalized V2 spec (after inference)
  
  Returns:
  - Plotly figure {:data [...traces...] :layout {...}}"
  [spec]
  (let [traces (spec->traces spec)
        layout (spec->layout spec)]
    {:data traces
     :layout layout}))

(defn render
  "Render a V2 spec to Plotly with Kindly metadata.
  
  This is the main entry point for visualization.
  
  Args:
  - spec: Finalized V2 spec (after inference)
  
  Returns:
  - Plotly spec with Kindly :plotly kind metadata
  
  Example:
  (require '[tableplot.v2.ggplot :as gg])
  (-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
      (gg/geom-point)
      gg/render
      plotly/render)"
  [spec]
  (let [plotly-spec (spec->plotly spec)]
    (kind/plotly plotly-spec {:style {:height :auto}})))

(comment
  ;; REPL experiments

  (require '[tableplot.v2.dataflow :as df])
  (require '[tableplot.v2.ggplot :as gg])

  ;; Simple test with map data
  (def simple-data {:x [1 2 3 4 5]
                    :y [2 4 6 8 10]})

  (def simple-spec
    (-> (gg/ggplot simple-data (gg/aes :x :x :y :y))
        (gg/geom-point)
        gg/render))

  ;; Convert to Plotly
  (spec->plotly simple-spec)
  ;; => {:data [{:type "scatter"
  ;;             :mode "markers"
  ;;             :x [1 2 3 4 5]
  ;;             :y [2 4 6 8 10]}]
  ;;     :layout {}}

  ;; Render with Kindly
  (render simple-spec))
