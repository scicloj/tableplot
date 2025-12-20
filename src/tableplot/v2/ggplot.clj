(ns tableplot.v2.ggplot
  "ggplot2-style API for Tableplot V2.
  
  Provides a familiar ggplot2 interface on top of the V2 dataflow model:
  - ggplot(data, aes) - Initialize plot with data and global aesthetics
  - geom-*() - Add layers
  - scale-*() - Customize scales
  - facet-*() - Create small multiples
  - labs() - Set labels
  - theme-*() - Set visual theme
  - render() - Generate visualization
  
  Example:
  (-> (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width :color :Species))
      (geom-point)
      (geom-smooth {:method :lm})
      render)"
  (:require [tableplot.v2.dataflow :as df]
            [tableplot.v2.plotly :as plotly]
            [tableplot.v2.ggplot.themes :as themes]))

(defn- deep-merge
  "Recursively merges maps."
  [& maps]
  (apply merge-with
         (fn [x y]
           (if (and (map? x) (map? y))
             (deep-merge x y)
             y))
         maps))

;;; ============================================================================
;;; Base Template
;;; ============================================================================

(def ggplot-template
  "Base template for ggplot2-style plots.
  
  Adds :=global-aes to the standard V2 template for hierarchical aesthetics.
  Sets default theme to theme-grey (ggplot2 default)."
  (-> df/base-plot-template
      (assoc :sub/keys (conj (:sub/keys df/base-plot-template) :=global-aes))
      (assoc :global-aes :=global-aes)
      ;; Set default theme to ggplot2 grey theme
      (assoc-in [:sub/map :=theme] themes/theme-grey)))

;;; ============================================================================
;;; Aesthetic Mapping
;;; ============================================================================

(defn aes
  "Create an aesthetic mapping.
  
  Aesthetics map data variables to visual properties like position,
  color, size, and shape. Can be used both globally (in ggplot) and
  locally (in geom-* functions).
  
  Args:
  - keyword pairs: aesthetic -> column name
  
  Returns:
  - Map of aesthetic mappings
  
  Examples:
  (aes :x :Sepal.Length :y :Sepal.Width)
  ;; => {:x :Sepal.Length :y :Sepal.Width}
  
  (aes :x :time :y :value :color :species :size :weight)
  ;; => {:x :time :y :value :color :species :size :weight}"
  [& {:as mappings}]
  mappings)

;;; ============================================================================
;;; Plot Initialization
;;; ============================================================================

(defn ggplot
  "Initialize a ggplot.
  
  Creates a plot specification with data and optional global aesthetics.
  Global aesthetics are inherited by all layers unless overridden.
  
  Args:
  - data: Dataset (tech.ml.dataset, tablecloth, vector of maps, or map of vectors)
  - global-aes: Optional aesthetic mapping created with (aes ...)
  
  Returns:
  - Plot spec (map with :sub/keys and :sub/map)
  
  Examples:
  ;; With global aesthetics
  (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width))
  
  ;; With color aesthetic
  (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width :color :Species))
  
  ;; Just data (add aesthetics in layers)
  (ggplot iris)"
  ([data]
   (ggplot data {}))
  ([data global-aes]
   (-> (df/make-spec ggplot-template)
       (df/add-substitution :=data data)
       (df/add-substitution :=global-aes global-aes)
       (df/add-substitution :=layers []))))

;;; ============================================================================
;;; Geom Functions (Layer Builders)
;;; ============================================================================

(defn- add-layer
  "Helper function to add a layer to the plot spec.
  
  Internal use only."
  [spec layer-map]
  (let [layers (or (df/get-substitution spec :=layers) [])]
    (df/add-substitution spec :=layers (conj layers layer-map))))

(defn geom-point
  "Add a point (scatter) layer.
  
  Args:
  - spec: Plot spec from ggplot
  - opts: Optional map with:
    - :aes - Local aesthetic mappings (override global)
    - :alpha - Opacity (0-1)
    - :size - Point size
    - :shape - Point shape
  
  Returns:
  - Updated plot spec
  
  Examples:
  (geom-point spec)
  (geom-point spec {:alpha 0.5})
  (geom-point spec {:aes (aes :color :species) :size 3})"
  ([spec]
   (geom-point spec {}))
  ([spec opts]
   (let [local-aes (:aes opts)
         attributes (dissoc opts :aes)
         layer {:geom :point
                :local-aes (or local-aes {})
                :attributes attributes}]
     (add-layer spec layer))))

(defn geom-line
  "Add a line layer.
  
  Args:
  - spec: Plot spec from ggplot
  - opts: Optional map with:
    - :aes - Local aesthetic mappings
    - :alpha - Opacity
    - :width - Line width
  
  Returns:
  - Updated plot spec
  
  Examples:
  (geom-line spec)
  (geom-line spec {:width 2 :alpha 0.8})"
  ([spec]
   (geom-line spec {}))
  ([spec opts]
   (let [local-aes (:aes opts)
         attributes (dissoc opts :aes)
         layer {:geom :line
                :local-aes (or local-aes {})
                :attributes attributes}]
     (add-layer spec layer))))

(defn geom-bar
  "Add a bar layer.
  
  Args:
  - spec: Plot spec from ggplot
  - opts: Optional map with:
    - :aes - Local aesthetic mappings
    - :stat - Statistical transformation (:identity, :count, :bin)
    - :alpha - Opacity
  
  Returns:
  - Updated plot spec
  
  Examples:
  (geom-bar spec)
  (geom-bar spec {:stat :count})"
  ([spec]
   (geom-bar spec {}))
  ([spec opts]
   (let [local-aes (:aes opts)
         stat (:stat opts :identity)
         attributes (dissoc opts :aes :stat)
         layer {:geom :bar
                :stat stat
                :local-aes (or local-aes {})
                :attributes attributes}]
     (add-layer spec layer))))

(defn geom-smooth
  "Add a smoothing layer (with line geom).
  
  Args:
  - spec: Plot spec from ggplot
  - opts: Optional map with:
    - :aes - Local aesthetic mappings
    - :method - Smoothing method (:lm, :loess, :gam)
    - :alpha - Opacity
    - :se - Show confidence interval (default true)
  
  Returns:
  - Updated plot spec
  
  Examples:
  (geom-smooth spec)
  (geom-smooth spec {:method :lm})
  (geom-smooth spec {:method :loess :se false})"
  ([spec]
   (geom-smooth spec {}))
  ([spec opts]
   (let [local-aes (:aes opts)
         method (:method opts :loess)
         se (:se opts true)
         attributes (dissoc opts :aes :method :se)
         layer {:geom :line
                :stat :smooth
                :method method
                :se se
                :local-aes (or local-aes {})
                :attributes attributes}]
     (add-layer spec layer))))

(defn geom-histogram
  "Add a histogram layer.
  
  Args:
  - spec: Plot spec from ggplot
  - opts: Optional map with:
    - :aes - Local aesthetic mappings
    - :bins - Number of bins (default 30)
    - :binwidth - Width of bins (overrides :bins)
    - :alpha - Opacity
  
  Returns:
  - Updated plot spec
  
  Examples:
  (geom-histogram spec)
  (geom-histogram spec {:bins 20})
  (geom-histogram spec {:binwidth 0.5})"
  ([spec]
   (geom-histogram spec {}))
  ([spec opts]
   (let [local-aes (:aes opts)
         bins (:bins opts 30)
         binwidth (:binwidth opts)
         attributes (dissoc opts :aes :bins :binwidth)
         layer {:geom :bar
                :stat :histogram
                :bins bins
                :binwidth binwidth
                :local-aes (or local-aes {})
                :attributes attributes}]
     (add-layer spec layer))))

;;; ============================================================================
;;; Labels and Titles
;;; ============================================================================

(defn labs
  "Set plot labels (title, axes).
  
  Args:
  - spec: Plot spec
  - opts: Keyword arguments for labels
    - :title - Plot title
    - :subtitle - Plot subtitle
    - :x - X axis label
    - :y - Y axis label
    - :color - Color legend label
    - :size - Size legend label
  
  Returns:
  - Updated plot spec
  
  Examples:
  (labs spec :title \"My Plot\")
  (labs spec :title \"Iris Data\" :x \"Sepal Length (cm)\" :y \"Sepal Width (cm)\")"
  [spec & {:as labels}]
  (df/add-substitution spec :=labels labels))

;;; ============================================================================
;;; Faceting
;;; ============================================================================

(defn facet-wrap
  "Create faceted plots arranged in a grid (wrapping).
  
  Args:
  - spec: Plot spec
  - facet-var: Variable to facet by (keyword)
  - opts: Optional map with:
    - :ncol - Number of columns
    - :nrow - Number of rows
    - :scales - \"fixed\", \"free\", \"free_x\", \"free_y\"
  
  Returns:
  - Updated plot spec
  
  Examples:
  (facet-wrap spec :Species)
  (facet-wrap spec :Species {:ncol 2})"
  ([spec facet-var]
   (facet-wrap spec facet-var {}))
  ([spec facet-var opts]
   (let [facet-spec (merge {:wrap facet-var} opts)]
     (df/add-substitution spec :=facets facet-spec))))

(defn facet-grid
  "Create faceted plots in a grid layout.
  
  Args:
  - spec: Plot spec
  - opts: Map with:
    - :row - Variable for rows
    - :col - Variable for columns
    - :scales - \"fixed\", \"free\", \"free_x\", \"free_y\"
  
  Returns:
  - Updated plot spec
  
  Examples:
  (facet-grid spec {:col :Species})
  (facet-grid spec {:row :Sex :col :Species})"
  [spec opts]
  (df/add-substitution spec :=facets (assoc opts :grid true)))

;;; ============================================================================
;;; Theme Functions
;;; ============================================================================

;; Re-export theme definitions from themes namespace
(def theme-grey themes/theme-grey)
(def theme-bw themes/theme-bw)
(def theme-minimal themes/theme-minimal)
(def theme-classic themes/theme-classic)
(def theme-dark themes/theme-dark)
(def theme-light themes/theme-light)
(def theme-void themes/theme-void)
(def theme-linedraw themes/theme-linedraw)

(defn theme
  "Apply or customize a theme.
  
  Can be used in two ways:
  1. Apply a complete theme: (theme spec theme-minimal)
  2. Customize theme elements: (theme spec :plot.background \"white\" ...)
  
  When customizing, merges with existing theme.
  
  Args:
  - spec: Plot spec
  - theme-map-or-kw: Either a complete theme map, or first keyword arg
  - & more: Additional keyword args for customization
  
  Returns:
  - Updated plot spec
  
  Examples:
  ;; Apply complete theme
  (theme spec theme-minimal)
  
  ;; Customize elements
  (theme spec :plot.background \"#f0f0f0\" 
              :panel.grid.major.color \"#ccc\")
  
  ;; Apply and customize
  (-> (theme spec theme-minimal)
      (theme :plot.title.font.size 18))"
  [spec theme-map-or-kw & more]
  (let [current-theme (df/get-substitution spec :=theme)
        new-theme (if (map? theme-map-or-kw)
                    ;; Complete theme provided
                    (deep-merge current-theme theme-map-or-kw)
                    ;; Keyword args for customization
                    (deep-merge current-theme
                                (apply themes/theme theme-map-or-kw more)))]
    (df/add-substitution spec :=theme new-theme)))

;;; ============================================================================
;;; Rendering
;;; ============================================================================

(defn render
  "Render the plot by running inference and generating visualization.
  
  This is the final step that converts the plot spec into a
  visualization by:
  1. Merging global and local aesthetics for each layer
  2. Inferring scales, guides, and other plot elements
  3. Generating backend-specific output (default: Plotly)
  
  Args:
  - spec: Plot spec
  - opts: Optional map with:
    - :backend - Backend to use (:plotly, default :plotly)
    - :infer-only - If true, return inferred spec without rendering
  
  Returns:
  - Visualization with Kindly metadata (or inferred spec if :infer-only true)
  
  Examples:
  (render spec)
  (render spec {:backend :plotly})
  (render spec {:infer-only true})"
  ([spec]
   (render spec {}))
  ([spec opts]
   (let [inferred (df/infer spec)]
     (if (:infer-only opts)
       inferred
       ;; Render to Plotly backend
       (plotly/render inferred)))))

;;; ============================================================================
;;; Example Usage (commented out)
;;; ============================================================================

(comment
  (require '[tech.v3.dataset :as ds])

  ;; Load iris dataset
  (def iris (ds/->dataset {:Sepal.Length [5.1 4.9 4.7 4.6 5.0]
                           :Sepal.Width [3.5 3.0 3.2 3.1 3.6]
                           :Species [:setosa :setosa :setosa :setosa :setosa]}))

  ;; Basic scatter plot
  (-> (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width))
      (geom-point)
      render)

  ;; With color
  (-> (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width :color :Species))
      (geom-point)
      render)

  ;; Layered (points + smooth)
  (-> (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width :color :Species))
      (geom-point {:alpha 0.5})
      (geom-smooth {:method :lm})
      render)

  ;; With labels
  (-> (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width :color :Species))
      (geom-point)
      (labs :title "Iris Dataset"
            :x "Sepal Length (cm)"
            :y "Sepal Width (cm)")
      render)

  ;; Faceted
  (-> (ggplot iris (aes :x :Sepal.Length :y :Sepal.Width))
      (geom-point)
      (facet-wrap :Species)
      render))
