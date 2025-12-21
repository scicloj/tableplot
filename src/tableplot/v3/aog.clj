(ns tableplot.v3.aog
  "v3 AlgebraOfGraphics API - Flat layer structure with standard merge.
  
  Core design principle: ALL aesthetics are top-level keys in the layer map,
  using the :aog namespace for clarity and collision prevention.
  This allows standard Clojure `merge` to work without custom logic.
  
  Key innovations:
  1. Flat structure - no :positional/:named distinction
  2. Namespaced keys - :aog/* prevents collisions with user data columns
  3. Value type determines usage:
     - keyword -> mapping to data column
     - number/string -> constant attribute
  4. Standard merge works - no custom merge-layer-pair needed
  5. Users can use merge/assoc/update directly on layers
  
  Layer structure:
  #:aog{:data {...}              ;; Dataset
        :x :column-name          ;; Aesthetic mapping (keyword)
        :y :column-name          ;; Aesthetic mapping (keyword)
        :color :species          ;; Aesthetic mapping (keyword)
        :alpha 0.5               ;; Constant attribute (number)
        :plottype :scatter}      ;; Plot type
  
  Expands to:
  {:aog/data {...}
   :aog/x :column-name
   :aog/y :column-name
   :aog/color :species
   :aog/alpha 0.5
   :aog/plottype :scatter}")

;; =============================================================================
;; Layer Constructors
;; =============================================================================

(defn data
  "Create a layer with data attached.
  
  Args:
  - dataset: Map of column-name -> vector, or tablecloth dataset
  
  Returns:
  - Map with :aog/data key
  
  Example:
  (data {:x [1 2 3] :y [4 5 6]})"
  [dataset]
  {:aog/data dataset})

(defn mapping
  "Create a layer with aesthetic mappings.
  
  Args:
  - x: Column name for x aesthetic (keyword)
  - y: Column name for y aesthetic (keyword)
  - named: Optional map of additional aesthetics (keyword -> keyword)
  
  Returns:
  - Map with :aog/* aesthetic keys
  
  Examples:
  (mapping :bill-length :bill-depth)
  (mapping :bill-length :bill-depth {:color :species :size :body-mass})"
  ([x y]
   {:aog/x x :aog/y y})
  ([x y named]
   (merge {:aog/x x :aog/y y}
          (update-keys named #(keyword "aog" (name %))))))

(defn visual
  "Create a layer with a plottype and optional attributes.
  
  Args:
  - plottype: Keyword like :scatter, :line, :bar
  - attrs: Optional map of attributes (keyword -> value)
  
  Returns:
  - Map with :aog/plottype and :aog/* attribute keys
  
  Examples:
  (visual :scatter)
  (visual :scatter {:alpha 0.5 :size 100})"
  ([plottype]
   {:aog/plottype plottype})
  ([plottype attrs]
   (merge {:aog/plottype plottype}
          (update-keys attrs #(keyword "aog" (name %))))))

;; =============================================================================
;; Statistical Transformations
;; =============================================================================

(defn linear
  "Create a layer for linear regression.
  
  Returns:
  - Map with :aog/transformation :linear and :aog/plottype :line"
  []
  {:aog/transformation :linear
   :aog/plottype :line})

(defn smooth
  "Create a layer for smoothed line (loess).
  
  Args:
  - opts: Optional map with transformation parameters
  
  Returns:
  - Map with :aog/transformation :smooth and :aog/plottype :line"
  ([]
   {:aog/transformation :smooth
    :aog/plottype :line})
  ([opts]
   (merge {:aog/transformation :smooth
           :aog/plottype :line}
          (update-keys opts #(keyword "aog" (name %))))))

(defn frequency
  "Create a layer for frequency/histogram transformation.
  
  Returns:
  - Map with :aog/transformation :frequency"
  []
  {:aog/transformation :frequency})

(defn density
  "Create a layer for density estimation.
  
  Returns:
  - Map with :aog/transformation :density"
  []
  {:aog/transformation :density})

(defn expectation
  "Create a layer for expectation (mean) calculation.
  
  Returns:
  - Map with :aog/transformation :expectation"
  []
  {:aog/transformation :expectation})

;; =============================================================================
;; Plot Types
;; =============================================================================

(defn scatter
  "Create a scatter plot layer.
  
  Args:
  - attrs: Optional map of attributes
  
  Examples:
  (scatter)
  (scatter {:alpha 0.5 :size 100})"
  ([]
   {:aog/plottype :scatter})
  ([attrs]
   (merge {:aog/plottype :scatter}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn line
  "Create a line plot layer.
  
  Args:
  - attrs: Optional map of attributes
  
  Examples:
  (line)
  (line {:alpha 0.8})"
  ([]
   {:aog/plottype :line})
  ([attrs]
   (merge {:aog/plottype :line}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn bar
  "Create a bar plot layer.
  
  Args:
  - attrs: Optional map of attributes
  
  Examples:
  (bar)
  (bar {:alpha 0.9})"
  ([]
   {:aog/plottype :bar})
  ([attrs]
   (merge {:aog/plottype :bar}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn area
  "Create an area plot layer.
  
  Args:
  - attrs: Optional map of attributes"
  ([]
   {:aog/plottype :area})
  ([attrs]
   (merge {:aog/plottype :area}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn boxplot
  "Create a boxplot layer.
  
  Args:
  - attrs: Optional map of attributes"
  ([]
   {:aog/plottype :boxplot})
  ([attrs]
   (merge {:aog/plottype :boxplot}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn violin
  "Create a violin plot layer.
  
  Args:
  - attrs: Optional map of attributes"
  ([]
   {:aog/plottype :violin})
  ([attrs]
   (merge {:aog/plottype :violin}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn heatmap
  "Create a heatmap layer.
  
  Args:
  - attrs: Optional map of attributes"
  ([]
   {:aog/plottype :heatmap})
  ([attrs]
   (merge {:aog/plottype :heatmap}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn histogram
  "Create a histogram layer.
  
  Args:
  - opts: Optional map with :bins, :binwidth, etc."
  ([]
   {:aog/plottype :bar
    :aog/transformation :frequency})
  ([opts]
   (merge {:aog/plottype :bar
           :aog/transformation :frequency}
          (update-keys opts #(keyword "aog" (name %))))))

;; =============================================================================
;; Algebraic Operators - Using Standard Merge!
;; =============================================================================

(defn *
  "Multiply layers (merge operation with distribution).
  
  This is the CORE operator that combines layer specifications.
  In v3, it's dramatically simpler because it uses standard merge!
  
  Behavior:
  - Two maps → merge them and return as single-element vector
  - Map + vector → distribute map over vector (merge with each element)
  - Two vectors → cartesian product with merge
  
  Args:
  - x, y, more: Maps or vectors of maps
  
  Returns:
  - Vector of merged layer maps
  
  Examples:
  (* (data df) (mapping :x :y) (scatter))
  ;; => [{:data df :x :x :y :y :plottype :scatter}]
  
  (* (data df) (mapping :x :y) (+ (scatter) (line)))
  ;; => [{:data df :x :x :y :y :plottype :scatter}
  ;;     {:data df :x :x :y :y :plottype :line}]"
  ([x]
   (if (map? x) [x] x))
  ([x y]
   (cond
     ;; Two maps -> merge and wrap in vector
     (and (map? x) (map? y))
     [(merge x y)]

     ;; Map and vector -> distribute map over vector
     (and (map? x) (vector? y))
     (mapv #(merge x %) y)

     (and (vector? x) (map? y))
     (mapv #(merge % y) x)

     ;; Two vectors -> cartesian product with merge
     (and (vector? x) (vector? y))
     (vec (for [a x, b y] (merge a b)))))
  ([x y & more]
   (reduce * (* x y) more)))

(defn +
  "Add layers (overlay operation).
  
  Creates a vector of layer specifications that will be overlaid.
  When combined with *, the base layer is distributed over each.
  
  Args:
  - layer-specs: Maps representing layer specifications
  
  Returns:
  - Vector of layer maps
  
  Example:
  (+ (scatter {:alpha 0.5})
     (linear))
  ;; => [{:plottype :scatter :alpha 0.5}
  ;;     {:transformation :linear :plottype :line}]"
  [& layer-specs]
  (vec layer-specs))

;; =============================================================================
;; Data-Oriented API (explicit function names)
;; =============================================================================

(defn merge-layers
  "Merge multiple layer specifications using standard merge.
  
  This is just an alias for * with a more descriptive name.
  Useful for users who prefer explicit function names over operators.
  
  Args:
  - layer-specs: Maps or vectors of maps
  
  Returns:
  - Vector of merged layer maps
  
  Example:
  (merge-layers (data df) (mapping :x :y) (scatter))"
  [& layer-specs]
  (apply * layer-specs))

(defn concat-layers
  "Concatenate multiple layer vectors.
  
  Args:
  - layer-vecs: Vectors of layer maps
  
  Returns:
  - Vector containing all layers
  
  Example:
  (concat-layers scatter-layers line-layers)"
  [& layer-vecs]
  (vec (apply concat layer-vecs)))

(defn layers
  "Create a vector of layers from raw layer maps.
  
  Provides direct access to the layer representation.
  Useful for programmatic layer generation.
  
  Args:
  - layer-maps: Individual layer maps
  
  Returns:
  - Vector of layer maps
  
  Example:
  (layers {:data df :x :x :y :y :plottype :scatter}
          {:data df :x :x :y :y :plottype :line})"
  [& layer-maps]
  (vec layer-maps))

;; =============================================================================
;; Scales (TODO: implement scale system)
;; =============================================================================

(defn scale
  "Apply a scale transformation to an aesthetic.
  
  Note: This is a placeholder. Full scale system TBD.
  
  Args:
  - aesthetic: Keyword like :x, :y, :color
  - scale-type: Keyword like :log, :sqrt, :pow
  - opts: Optional scale parameters"
  [aesthetic scale-type & [opts]]
  {aesthetic {:scale (merge {:type scale-type} opts)}})

;; =============================================================================
;; Faceting
;; =============================================================================

(defn facet
  "Add faceting to layers.
  
  Faceting aesthetics work just like other aesthetics in v3!
  
  Args:
  - layers: Vector of layer maps
  - facet-spec: Map with :row, :col, or :facet keys
  
  Returns:
  - Vector of layers with faceting aesthetics added
  
  Examples:
  (facet layers {:col :species})
  (facet layers {:row :island :col :species})"
  [layers facet-spec]
  (mapv #(merge % facet-spec) layers))

;; =============================================================================
;; Drawing / Rendering
;; =============================================================================

(defn draw
  "Draw layers to produce a visualization.
  
  Converts flat v3 layer structure (with :aog/* keys) to the format expected 
  by the processing layer, then dispatches to the appropriate backend.
  
  Args:
  - layers: Vector of layer maps (v3 flat structure with :aog/* keys)
  - opts: Optional map with:
    - :backend - :vegalite (default), :plotly, or :thing-geom
    - :theme - Theme keyword
    - :width, :height - Dimensions
  
  Examples:
  (draw (* (data df) (mapping :x :y) (scatter)))
  (draw (* (data df) (mapping :x :y) (+ (scatter) (linear)))
        {:backend :vegalite})"
  ([layers]
   (draw layers {}))
  ([layers opts]
   ;; Convert v3 flat layers (with :aog/* keys) to v2 format for processing layer
   (let [v2-layers (mapv (fn [layer]
                           ;; Extract aesthetics from flat layer with :aog/* namespace
                           (let [positional (filterv some? [(:aog/x layer) (:aog/y layer)])
                                 ;; Aesthetic keys we need to check (with :aog namespace)
                                 aesthetic-keys [:aog/color :aog/size :aog/shape :aog/alpha :aog/opacity
                                                 :aog/stroke :aog/fill :aog/row :aog/col :aog/facet]
                                 ;; Extract only the aesthetic keys that exist
                                 named (select-keys layer aesthetic-keys)
                                 ;; Separate attributes (non-keyword values) from mappings (keyword values)
                                 {mappings true attrs false}
                                 (group-by (fn [[k v]] (keyword? v)) named)

                                 ;; Remove :aog/ namespace for v2 format
                                 named-map (into {} (map (fn [[k v]] [(keyword (name k)) v]) mappings))
                                 attr-map (into {} (map (fn [[k v]] [(keyword (name k)) v]) attrs))]
                             {:transformation (:aog/transformation layer)
                              :data (:aog/data layer)
                              :positional positional
                              :named named-map
                              :plottype (:aog/plottype layer)
                              :attributes attr-map}))
                         layers)
         entries ((requiring-resolve 'scicloj.tableplot.v1.aog.processing/layers->entries)
                  v2-layers)
         backend (or (:backend opts) :vegalite)]
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
