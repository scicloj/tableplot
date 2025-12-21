(ns tableplot.v2.aog
  "Normalized AlgebraOfGraphics API - v2 with always-vector representation.
  
  Key design decisions:
  1. ALL constructors return vectors of layers (never single layer)
  2. Operators * and + work on vectors only (no type checking needed)
  3. Provides both algebraic (* +) and data-oriented (merge-layers, concat-layers) APIs
  
  Benefits:
  - Simpler implementation (no conditionals for single vs vector)
  - More predictable behavior
  - Easier to reason about composition
  - Mirrors internal structure (:positional is vec, :named is map)
  
  Trade-offs:
  - Slightly more verbose: [(data x)] vs (data x)
  - But clearer and more consistent"
  (:require [scicloj.tableplot.v1.aog.ir :as ir]))

;;; =============================================================================
;;; Layer Constructors (all return vectors)

(defn layer
  "Create a single layer wrapped in a vector.
  
  This is the base constructor. All other constructors use this.
  
  Returns: Vector containing one layer"
  ([]
   [(ir/layer nil nil [] {})])
  ([data positional named]
   [(ir/layer nil data positional named)]))

(defn data
  "Create a layer with data attached.
  
  Returns: Vector containing one layer"
  [dataset]
  [(ir/layer nil dataset [] {})])

(defn mapping
  "Create a layer with aesthetic mappings.
  
  Accepts:
  - Positional: (mapping :x :y)
  - Named: (mapping {:color :species})
  - Mixed: (mapping :x :y {:color :species})
  
  Returns: Vector containing one layer"
  ([positional-or-named]
   (if (map? positional-or-named)
     [(ir/layer nil nil [] positional-or-named)]
     [(ir/layer nil nil [positional-or-named] {})]))
  ([pos1 pos2 & more]
   (let [last-arg (if (seq more) (last more) pos2)
         has-named? (map? last-arg)
         positional (cond
                      (and (empty? more) has-named?) [pos1]
                      has-named? (into [pos1 pos2] (butlast more))
                      :else (into [pos1 pos2] more))
         named (if has-named? last-arg {})]
     [(ir/layer nil nil positional named)])))

(defn visual
  "Create a layer with visual attributes.
  
  Returns: Vector containing one layer"
  ([plottype attrs]
   [(assoc (ir/layer)
           :plottype plottype
           :attributes attrs)])
  ([attrs]
   [(assoc (ir/layer)
           :attributes attrs)]))

;;; =============================================================================
;;; Transformation Constructors

(defn linear
  "Linear regression transformation layer.
  
  Returns: Vector containing one layer"
  []
  [(assoc (ir/layer)
          :transformation :linear
          :plottype :line)])

(defn frequency
  "Frequency (count) transformation layer.
  
  Returns: Vector containing one layer"
  []
  [(assoc (ir/layer)
          :transformation :frequency
          :plottype :bar)])

(defn expectation
  "Expectation (mean) transformation layer.
  
  Returns: Vector containing one layer"
  []
  [(assoc (ir/layer)
          :transformation :expectation
          :plottype :scatter)])

(defn smooth
  "Smoothing transformation layer.
  
  Returns: Vector containing one layer"
  []
  [(assoc (ir/layer)
          :transformation :smooth
          :plottype :line)])

(defn density
  "Density estimation transformation layer.
  
  Returns: Vector containing one layer"
  []
  [(assoc (ir/layer)
          :transformation :density
          :plottype :line)])

(defn histogram
  "Histogram transformation layer.
  
  Returns: Vector containing one layer"
  ([]
   (histogram {}))
  ([opts]
   [(assoc (ir/layer)
           :transformation [:histogram opts]
           :plottype :bar)]))

;;; =============================================================================
;;; Plot Type Shortcuts

(defn scatter
  "Scatter plot layer.
  
  Returns: Vector containing one layer"
  ([]
   (scatter {}))
  ([attrs]
   (visual :scatter attrs)))

(defn line
  "Line plot layer.
  
  Returns: Vector containing one layer"
  ([]
   (line {}))
  ([attrs]
   (visual :line attrs)))

(defn bar
  "Bar plot layer.
  
  Returns: Vector containing one layer"
  ([]
   (bar {}))
  ([attrs]
   (visual :bar attrs)))

(defn boxplot
  "Box plot layer.
  
  Returns: Vector containing one layer"
  ([]
   (boxplot {}))
  ([attrs]
   (visual :box attrs)))

(defn violin
  "Violin plot layer.
  
  Returns: Vector containing one layer"
  ([]
   (violin {}))
  ([attrs]
   (visual :violin attrs)))

(defn heatmap
  "Heatmap layer.
  
  Returns: Vector containing one layer"
  ([]
   (heatmap {}))
  ([attrs]
   (visual :heatmap attrs)))

;;; =============================================================================
;;; Algebraic Operations - SIMPLIFIED!

(defn- merge-layer-pair
  "Merge two individual layer maps.
  
  This is the core merge logic used by both * and merge-layers.
  
  Important: Uses rightmost non-nil value for each field."
  [layer1 layer2]
  (let [pick-non-nil (fn [k] (if (nil? (get layer2 k))
                               (get layer1 k)
                               (get layer2 k)))]
    {:transformation (pick-non-nil :transformation)
     :data (pick-non-nil :data)
     :positional (into (vec (:positional layer1)) (:positional layer2))
     :named (merge (:named layer1) (:named layer2))
     :plottype (pick-non-nil :plottype)
     :attributes (merge (:attributes layer1) (:attributes layer2))}))

(defn *
  "Multiply layers (Cartesian product / merge).
  
  SIMPLIFIED VERSION - no type checking needed!
  All inputs are vectors, output is vector.
  
  Takes two or more vectors of layers, returns vector of merged layers.
  
  Examples:
  (* [(data df)] [(mapping :x :y)])
  ; => [merged-layer]
  
  (* [(data df)] [(mapping :x :y)] [(scatter)])
  ; => [merged-layer]
  
  (* [(data df)] [(mapping :x :y)] (+ [(scatter)] [(line)]))
  ; => [scatter-layer line-layer]  (distributes)"
  ([layers]
   layers)
  ([layers1 layers2]
   (vec (for [l1 layers1
              l2 layers2]
          (merge-layer-pair l1 l2))))
  ([layers1 layers2 & more]
   (reduce * (* layers1 layers2) more)))

(defn +
  "Add layers (overlay).
  
  SIMPLIFIED VERSION - no type checking needed!
  All inputs are vectors, output is vector.
  
  Concatenates vectors of layers.
  
  Examples:
  (+ [(scatter)] [(line)])
  ; => [scatter-layer line-layer]
  
  (+ [(scatter {:alpha 0.3})] [(linear)])
  ; => [scatter-layer linear-layer]"
  [& layer-vecs]
  (vec (apply concat layer-vecs)))

;;; =============================================================================
;;; Data-Oriented API Alternatives

(defn merge-layers
  "Merge multiple layer vectors using Cartesian product.
  
  This is the data-oriented alternative to *.
  Semantically identical to *, but with a more explicit name.
  
  Examples:
  (merge-layers [(data df)] [(mapping :x :y)] [(scatter)])
  ; Same as: (* [(data df)] [(mapping :x :y)] [(scatter)])"
  [& layer-vecs]
  (apply * layer-vecs))

(defn concat-layers
  "Concatenate multiple layer vectors.
  
  This is the data-oriented alternative to +.
  Semantically identical to +, but with a more explicit name.
  
  Examples:
  (concat-layers [(scatter)] [(line)])
  ; Same as: (+ [(scatter)] [(line)])"
  [& layer-vecs]
  (apply + layer-vecs))

(defn layers
  "Create a vector of layers from raw layer maps.
  
  This is for advanced users who want to work with raw data structures.
  
  Example:
  (layers {:data df :positional [:x :y] :plottype :scatter}
          {:data df :positional [:x :y] :plottype :line})"
  [& layer-maps]
  (vec layer-maps))

;;; =============================================================================
;;; Scale Transformations

(defn log-scale
  "Create a logarithmic scale specification.
  
  Returns: Vector containing one layer"
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
     [(ir/layer nil nil [] {scale-key scale-config})])))

(defn pow-scale
  "Create a power scale specification.
  
  Returns: Vector containing one layer"
  ([axis exponent]
   (pow-scale axis exponent {}))
  ([axis exponent opts]
   (let [domain (get opts :domain)
         nice (get opts :nice true)
         scale-config (cond-> {:type "pow" :exponent exponent}
                        domain (assoc :domain domain)
                        nice (assoc :nice nice))
         scale-key (keyword (str (name axis) "-scale"))]
     [(ir/layer nil nil [] {scale-key scale-config})])))

(defn sqrt-scale
  "Create a square root scale specification.
  
  Returns: Vector containing one layer"
  ([axis]
   (sqrt-scale axis {}))
  ([axis opts]
   (let [domain (get opts :domain)
         nice (get opts :nice true)
         scale-config (cond-> {:type "sqrt"}
                        domain (assoc :domain domain)
                        nice (assoc :nice nice))
         scale-key (keyword (str (name axis) "-scale"))]
     [(ir/layer nil nil [] {scale-key scale-config})])))

(defn scale-domain
  "Set a custom domain for a scale.
  
  Returns: Vector containing one layer"
  ([axis domain]
   (scale-domain axis domain {}))
  ([axis domain opts]
   (let [zero (get opts :zero)
         nice (get opts :nice)
         scale-config (cond-> {:domain domain}
                        (some? zero) (assoc :zero zero)
                        (some? nice) (assoc :nice nice))
         scale-key (keyword (str (name axis) "-scale"))]
     [(ir/layer nil nil [] {scale-key scale-config})])))

;;; =============================================================================
;;; Faceting

(defn facet
  "Add faceting to layers.
  
  SIMPLIFIED - no conditional needed!
  
  Returns: Vector of layers with faceting information"
  [layers facet-spec]
  (mapv #(update % :named merge facet-spec) layers))

;;; =============================================================================
;;; Drawing

(defn draw
  "Draw layers to produce a visualization.
  
  SIMPLIFIED - no conditional needed!
  Input is always a vector.
  
  Args:
  - layers: Vector of layers
  - opts: Optional map with:
    - :backend - :vegalite (default), :plotly, or :thing-geom
    - :theme - Theme keyword
    - :width, :height - Dimensions
  
  Examples:
  (draw (* (data df) (mapping :x :y) (scatter)))
  (draw (* (data df) (mapping :x :y) (+ (scatter) (linear)))
        {:backend :thing-geom})"
  ([layers]
   (draw layers {}))
  ([layers opts]
   ;; Convert to entries
   (let [entries ((requiring-resolve 'scicloj.tableplot.v1.aog.processing/layers->entries)
                  layers)
         backend (or (:backend opts) :vegalite)] ;; Default to vegalite
     (case backend
       :vegalite
       ((requiring-resolve 'scicloj.tableplot.v1.aog.vegalite/vegalite)
        entries opts)

       :plotly
       (let [layout (or (:layout opts) {})]
         ((requiring-resolve 'scicloj.tableplot.v1.aog.plotly/plotly)
          entries layout))

       :thing-geom
       ((requiring-resolve 'scicloj.tableplot.v1.aog.thing-geom/thing-geom)
        entries opts)

       (throw (ex-info "Unknown backend" {:backend backend}))))))

;;; =============================================================================
;;; Convenience Macros (Optional - for even terser syntax)

(comment
  ;; Future: Could add macros for implicit vector wrapping
  ;; This would let users write: (data* df) which expands to [(data df)]
  ;; But let's start simple and see if the explicit vectors are fine.
  )

(comment
  ;; REPL experiments showing the simplified API

  ;; Basic usage - note the explicit vectors
  (def my-layers
    (* [(data {:x [1 2 3] :y [4 5 6]})]
       [(mapping :x :y)]
       [(scatter)]))

  ;; => Vector of 1 merged layer

  ;; Multiple plot types
  (def multi
    (* [(data {:x [1 2 3] :y [4 5 6]})]
       [(mapping :x :y)]
       (+ [(scatter {:alpha 0.3})]
          [(linear)])))

  ;; => Vector of 2 layers (scatter and linear)

  ;; Data-oriented style (equivalent)
  (def using-merge
    (merge-layers [(data {:x [1 2 3] :y [4 5 6]})]
                  [(mapping :x :y)]
                  [(scatter)]))

  ;; Using concat for overlays
  (def using-concat
    (concat-layers [(scatter {:alpha 0.3})]
                   [(linear)]))

  ;; Raw data approach
  (def raw-layers
    (layers {:data {:x [1 2 3] :y [4 5 6]}
             :positional [:x :y]
             :plottype :scatter
             :transformation nil
             :named {}
             :attributes {:alpha 0.5}}))

  ;; Drawing is simple - always pass a vector
  (draw my-layers)
  (draw multi {:theme :tableplot-bold}))
