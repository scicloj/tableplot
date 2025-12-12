(ns scicloj.tableplot.v1.aog.core
  "AlgebraOfGraphics-style API for Clojure.

  Provides algebraic composition of visualizations using:
  - data(dataset) - Create a layer with data
  - mapping(...) - Specify aesthetic mappings
  - visual(...) - Specify visual attributes
  - * (multiply) - Merge layers (Cartesian product)
  - + (add) - Overlay layers

  Example:
  (-> (data penguins)
      (* (mapping :bill-length :bill-depth))
      (* (mapping {:color :species}))
      (* (+ (scatter {:alpha 0.3})
            (linear)))
      (draw))"
  (:require [scicloj.tableplot.v1.aog.ir :as ir]))

;;; =============================================================================
;;; Layer construction

(defn data
  "Create a layer with data attached.

  Args:
  - dataset: A dataset (tech.ml.dataset, tablecloth, vector of maps, or map of vectors)

  Returns:
  - Layer map with :data field set

  Example:
  (data penguins-dataset)"
  [dataset]
  (ir/layer nil dataset [] {}))

(defn mapping
  "Create a layer with aesthetic mappings.

  Accepts either:
  1. Positional args (column keys): (mapping :x :y)
  2. Named args (aesthetics): (mapping {:color :species :size :body-mass})
  3. Mixed: (mapping :x :y {:color :species})

  Args:
  - positional...: Column keys for positional mappings
  - named: Map of aesthetic -> column key

  Returns:
  - Layer map with :positional and/or :named fields set

  Examples:
  (mapping :bill-length :bill-depth)
  (mapping {:color :species})
  (mapping :x :y {:color :species :size :body-mass})"
  ([positional-or-named]
   (if (map? positional-or-named)
     ;; Named only
     (ir/layer nil nil [] positional-or-named)
     ;; Single positional
     (ir/layer nil nil [positional-or-named] {})))
  ([pos1 pos2 & more]
   (let [;; Check if last arg is a map (named mappings)
         ;; When more is empty, check pos2; otherwise check last of more
         last-arg (if (seq more) (last more) pos2)
         has-named? (map? last-arg)
         positional (cond
                      ;; Two args where second is named
                      (and (empty? more) has-named?)
                      [pos1]

                      ;; Multiple args with named at end
                      has-named?
                      (into [pos1 pos2] (butlast more))

                      ;; All positional
                      :else
                      (into [pos1 pos2] more))
         named (if has-named? last-arg {})]
     (ir/layer nil nil positional named))))

(defn visual
  "Create a layer with visual attributes.

  Visual attributes are passed directly to the plot function
  (e.g., alpha, size, color values, not mapped from data).

  Args:
  - plottype: Plot type keyword (:scatter, :line, :bar, etc.) - optional
  - attrs: Map of attribute -> value

  Returns:
  - Layer map with plottype and attributes

  Examples:
  (visual :scatter {:alpha 0.3})
  (visual {:alpha 0.5 :size 10})"
  ([plottype attrs]
   (assoc (ir/layer)
          :plottype plottype
          :attributes attrs))
  ([attrs]
   (assoc (ir/layer)
          :attributes attrs)))

;;; =============================================================================
;;; Transformation constructors

(defn linear
  "Create a linear regression transformation layer.

  Returns a layer with linear regression transformation attached.

  Example:
  (linear)"
  []
  (assoc (ir/layer)
         :transformation :linear
         :plottype :line))

(defn frequency
  "Create a frequency (count) transformation layer.

  Computes counts for each unique combination of input variables.
  The result will have a :count column.

  Example:
  (frequency)"
  []
  (assoc (ir/layer)
         :transformation :frequency
         :plottype :bar))

(defn expectation
  "Create an expectation (mean) transformation layer.

  Computes the mean of y conditioned on x (and optionally grouping variables).

  Example:
  (expectation)"
  []
  (assoc (ir/layer)
         :transformation :expectation
         :plottype :scatter))

(defn smooth
  "Create a smoothing transformation layer (loess-like).

  Returns a layer with smoothing transformation attached.

  Example:
  (smooth)"
  []
  (assoc (ir/layer)
         :transformation :smooth
         :plottype :line))

(defn density
  "Create a density estimation transformation layer.

  Returns a layer with density transformation attached.

  Example:
  (density)"
  []
  (assoc (ir/layer)
         :transformation :density
         :plottype :line))

(defn histogram
  "Create a histogram transformation layer.

  Args:
  - opts: Options map (e.g., {:bins 30})

  Returns a layer with histogram transformation attached.

  Example:
  (histogram {:bins 20})"
  ([]
   (histogram {}))
  ([opts]
   (assoc (ir/layer)
          :transformation [:histogram opts]
          :plottype :bar)))

;;; =============================================================================
;;; Plot type shortcuts

(defn scatter
  "Create a scatter plot layer.

  Args:
  - attrs: Optional map of visual attributes

  Example:
  (scatter {:alpha 0.5 :size 10})"
  ([]
   (scatter {}))
  ([attrs]
   (visual :scatter attrs)))

(defn line
  "Create a line plot layer.

  Args:
  - attrs: Optional map of visual attributes

  Example:
  (line {:color \"red\" :width 2})"
  ([]
   (line {}))
  ([attrs]
   (visual :line attrs)))

(defn bar
  "Create a bar plot layer.

  Args:
  - attrs: Optional map of visual attributes

  Example:
  (bar {:opacity 0.8})"
  ([]
   (bar {}))
  ([attrs]
   (visual :bar attrs)))

(defn boxplot
  "Create a box plot layer.

  Box plots show the distribution of quantitative data using quartiles.
  Requires one categorical and one quantitative variable.

  Args:
  - attrs: Optional map of visual attributes

  Example:
  (boxplot)
  (boxplot {:extent 1.5})"
  ([]
   (boxplot {}))
  ([attrs]
   (visual :box attrs)))

(defn violin
  "Create a violin plot layer.

  Violin plots show the distribution using kernel density estimation.
  Requires one categorical and one quantitative variable.

  Args:
  - attrs: Optional map of visual attributes

  Example:
  (violin)
  (violin {:bandwidth 0.5})"
  ([]
   (violin {}))
  ([attrs]
   (visual :violin attrs)))

(defn heatmap
  "Create a heatmap layer.

  Heatmaps show values using color on a 2D grid.
  Requires two categorical/ordinal variables and one quantitative variable for color.

  Args:
  - attrs: Optional map of visual attributes

  Example:
  (heatmap)
  (heatmap {:scheme \"viridis\"})"
  ([]
   (heatmap {}))
  ([attrs]
   (visual :heatmap attrs)))

;;; =============================================================================
;;; Algebraic operations

;;; Helper utilities (defined first for use in algebraic operations)

(defn layers?
  "Check if x is a collection of layers (result of +)."
  [x]
  (and (vector? x)
       (every? #(contains? % :transformation) x)))

(defn layer?
  "Check if x is a single layer."
  [x]
  (and (map? x)
       (contains? x :transformation)))

(defn- merge-layers
  "Merge two individual layer maps."
  [layer1 layer2]
  {:transformation (or (:transformation layer2) (:transformation layer1))
   :data (or (:data layer2) (:data layer1))
   :positional (into (vec (:positional layer1)) (:positional layer2))
   :named (merge (:named layer1) (:named layer2))
   :plottype (or (:plottype layer2) (:plottype layer1))
   :attributes (merge (:attributes layer1) (:attributes layer2))})

(defn *
  "Multiply layers (Cartesian product / merge).

  Multiplication combines layer specifications by merging their fields:
  - :data - Rightmost layer's data overrides (if present)
  - :positional - Concatenated
  - :named - Merged (rightmost overrides)
  - :transformation - Rightmost overrides (if present)
  - :plottype - Rightmost overrides (if present)
  - :attributes - Merged

  When multiplying with a vector of layers (result of +), distributes
  the multiplication across each layer.

  This corresponds to AoG's * operator.

  Args:
  - layer1: First layer (or vector of layers)
  - layer2...: Additional layers (or vectors) to merge

  Returns:
  - Merged layer or vector of merged layers

  Examples:
  (* (data penguins)
     (mapping :bill-length :bill-depth)
     (mapping {:color :species}))

  (* (data penguins)
     (mapping :x :y)
     (+ (scatter) (linear)))  ; Distributes across both layers"
  ([x] x)
  ([layer1 layer2]
   (cond
     ;; Both are vectors - distribute across both
     (and (layers? layer1) (layers? layer2))
     (vec (for [l1 layer1, l2 layer2]
            (merge-layers l1 l2)))

     ;; layer1 is vector, layer2 is single - distribute layer2 to each in layer1
     (layers? layer1)
     (mapv #(merge-layers % layer2) layer1)

     ;; layer2 is vector, layer1 is single - distribute layer1 to each in layer2
     (layers? layer2)
     (mapv #(merge-layers layer1 %) layer2)

     ;; Both are single layers - simple merge
     :else
     (merge-layers layer1 layer2)))
  ([layer1 layer2 & more]
   (reduce * (* layer1 layer2) more)))

(defn +
  "Add layers (overlay).

  Addition creates a collection of layers that will be drawn on
  the same plot. This corresponds to AoG's + operator.

  Args:
  - layer1: First layer
  - layer2...: Additional layers to overlay

  Returns:
  - Vector of layers (or Layers record in future)

  Example:
  (+ (scatter {:alpha 0.3})
     (linear))"
  [layer1 & more-layers]
  (into [layer1] more-layers))

;;; =============================================================================
;;; Scale transformations

(defn log-scale
  "Create a logarithmic scale specification.

  Args:
  - axis: :x or :y
  - opts: Optional map with:
    - :base - Log base (default 10)
    - :domain - Custom domain [min max]
    - :nice - Round domain to nice values (default true)

  Example:
  (log-scale :y {:base 10 :domain [1 1000]})"
  ([axis]
   (log-scale axis {}))
  ([axis opts]
   (let [base (get opts :base 10)
         domain (get opts :domain)
         nice (get opts :nice true)
         scale-config (cond-> {:type "log" :base base}
                        domain (assoc :domain (vec domain))
                        nice (assoc :nice nice))
         scale-key (keyword (str (name axis) "-scale"))]
     (ir/layer nil nil [] {scale-key scale-config}))))

(defn pow-scale
  "Create a power scale specification.

  Args:
  - axis: :x or :y
  - exponent: Power exponent (e.g., 2 for square, 0.5 for sqrt)
  - opts: Optional map with :domain, :nice

  Example:
  (pow-scale :y 0.5)  ; square root scale"
  ([axis exponent]
   (pow-scale axis exponent {}))
  ([axis exponent opts]
   (let [domain (get opts :domain)
         nice (get opts :nice true)
         scale-config (cond-> {:type "pow" :exponent exponent}
                        domain (assoc :domain domain)
                        nice (assoc :nice nice))
         scale-key (keyword (str (name axis) "-scale"))]
     (ir/layer nil nil [] {scale-key scale-config}))))

(defn sqrt-scale
  "Create a square root scale specification.

  Args:
  - axis: :x or :y
  - opts: Optional map with :domain, :nice

  Example:
  (sqrt-scale :y)"
  ([axis]
   (sqrt-scale axis {}))
  ([axis opts]
   (let [domain (get opts :domain)
         nice (get opts :nice true)
         scale-config (cond-> {:type "sqrt"}
                        domain (assoc :domain domain)
                        nice (assoc :nice nice))
         scale-key (keyword (str (name axis) "-scale"))]
     (ir/layer nil nil [] {scale-key scale-config}))))

(defn scale-domain
  "Set a custom domain for a scale.

  Args:
  - axis: :x or :y
  - domain: Domain vector [min max] or list of categorical values
  - opts: Optional map with :zero (include zero), :nice (round to nice values)

  Example:
  (scale-domain :y [0 100] {:zero true :nice true})"
  ([axis domain]
   (scale-domain axis domain {}))
  ([axis domain opts]
   (let [zero (get opts :zero)
         nice (get opts :nice)
         scale-config (cond-> {:domain domain}
                        (some? zero) (assoc :zero zero)
                        (some? nice) (assoc :nice nice))
         scale-key (keyword (str (name axis) "-scale"))]
     (ir/layer nil nil [] {scale-key scale-config}))))

;;; =============================================================================
;;; Faceting

(defn facet
  "Add faceting to a layer or layers.

  Args:
  - layers: Layer or vector of layers
  - facet-spec: Map with :row and/or :col keys specifying facet variables

  Returns:
  - Layer(s) with faceting information

  Example:
  (facet my-layers {:col :sex})
  (facet my-layers {:row :island :col :sex})"
  [layers facet-spec]
  (let [add-facet (fn [layer]
                    (update layer :named merge facet-spec))]
    (if (vector? layers)
      (mapv add-facet layers)
      (add-facet layers))))

;;; =============================================================================
;;; Drawing (end-to-end pipeline)

(defn draw
  "Draw a layer or layers to produce a Vega-Lite visualization.

  This is the main entry point that ties together the full pipeline:
  Layer(s) → Entry(s) → Vega-Lite spec

  Args:
  - layers: Single layer or vector of layers (result of aog/*)
  - opts: Optional map with keys:
    - :backend - Backend to use (:vegalite or :plotly, default :vegalite)
    - :theme - Theme keyword (:tableplot-subtle, :tableplot-balanced, :tableplot-bold,
               :ggplot2, :vega) or custom theme map. Defaults to :tableplot-balanced
    - :width - Width in pixels (default: 600)
    - :height - Height in pixels (default: 400)
    - :layout - Layout configuration (Plotly only)

  Returns:
  - Vega-Lite or Plotly spec with Kindly metadata (ready for display)

  Examples:
  (draw (aog/* (aog/data {:x [1 2 3] :y [4 5 6]})
               (aog/mapping :x :y)
               (aog/scatter)))

  (draw (aog/* (aog/data penguins)
               (aog/mapping :bill-length :bill-depth {:color :species})
               (aog/+ (aog/scatter {:alpha 0.5})
                      (aog/linear)))
        {:theme :tableplot-bold
         :width 800
         :height 600})"
  ([layers]
   (draw layers {}))
  ([layers opts]
   (let [;; Ensure we have a vector of layers
         layers-vec (if (vector? layers) layers [layers])
         ;; Convert to entries
         entries ((requiring-resolve 'scicloj.tableplot.v1.aog.processing/layers->entries)
                  layers-vec)
         ;; Choose backend (default to Vega-Lite)
         backend (or (:backend opts) :vegalite)]

     (case backend
       :vegalite
       ;; Use Vega-Lite backend - pass opts through for theme
       ((requiring-resolve 'scicloj.tableplot.v1.aog.vegalite/vegalite)
        entries opts)

       :plotly
       ;; Use Plotly backend
       (let [layout (or (:layout opts) {})]
         ((requiring-resolve 'scicloj.tableplot.v1.aog.plotly/plotly)
          entries layout))

       ;; Default case
       (throw (ex-info "Unknown backend" {:backend backend}))))))

(comment
  ;; REPL experiments

  ;; Create a basic layer with data
  (def my-data {:x [1 2 3] :y [4 5 6] :species [:a :a :b]})
  (def layer1 (data my-data))

  ;; Add mappings using multiplication
  (def layer2 (* layer1 (mapping :x :y)))

  ;; Add color aesthetic
  (def layer3 (* layer2 (mapping {:color :species})))

  ;; Create overlaid layers
  (def overlaid
    (+ (scatter {:alpha 0.5})
       (linear)))

  ;; Full composition
  (def full-spec
    (* (data my-data)
       (mapping :x :y)
       (mapping {:color :species})
       (+ (scatter {:alpha 0.3})
          (linear))))

  ;; Check types
  (layer? layer1)
  ;; => true
  (layers? overlaid)
  ;; => true
  )
