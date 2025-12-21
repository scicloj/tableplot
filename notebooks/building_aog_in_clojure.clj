;; # Building an Algebra of Graphics in Clojure
;; **A Design Exploration for Tableplot**
;;
;; *This notebook explores a fresh approach to composable plot specifications
;; in Clojure, inspired by Julia's AlgebraOfGraphics.jl. It's a working prototype
;; demonstrating an alternative compositional API that addresses limitations in
;; Tableplot's current APIs.*

(ns building-aog-in-clojure
  (:require [tablecloth.api :as tc]
            [scicloj.kindly.v4.kind :as kind]
            [thi.ng.geom.viz.core :as viz]
            [thi.ng.geom.svg.core :as svg]
            [fastmath.ml.regression :as regr]
            [fastmath.stats :as stats]
            [scicloj.metamorph.ml.rdatasets :as rdatasets]))

;; # 1. Context & Motivation
;;
;; ## Why Explore a New Approach?
;;
;; Tableplot currently provides two visualization APIs:
;; - `scicloj.tableplot.v1.hanami` - Vega-Lite via Hanami templates
;; - `scicloj.tableplot.v1.plotly` - Plotly.js visualizations
;;
;; While these APIs are pragmatic and functional, they have accumulated
;; several limitations that are difficult to address incrementally:
;;
;; ### Current Limitations
;;
;; **1. Backwards Compatibility Burden**
;; - The `hanami` API extends the original Hanami library API, including
;;   its substitution key conventions (uppercase keywords like `:X`, `:Y`)
;; - The `plotly` API shares infrastructure with `hanami`, inheriting
;;   these backwards compatibility constraints
;; - Changes to the core abstraction affect both APIs
;;
;; **2. Rendering Target Lock-in**
;; - Each API supports exactly one rendering target (Vega-Lite or Plotly.js)
;; - The target's concepts and jargon leak into the API
;; - Users must learn target-specific vocabulary
;; - Switching backends requires learning a different API
;;
;; **3. Rendering Target Limitations**
;; - Vega-Lite: Limited support for coordinate systems (polar, geographic)
;; - Plotly.js: Difficult to programmatically render as static images
;; - No single backend handles all use cases
;; - Users must switch APIs when hitting backend limitations
;;
;; **4. Pragmatic Compromises**
;; - APIs optimized for common cases, not theoretical elegance
;; - Design decisions leak between layers:
;;   - Template substitution (IR layer) visible to users
;;   - Backend specifics (rendering layer) affect user-facing API
;;   - Data layer (tech.ml.dataset) tightly coupled to API
;;
;; **5. Confusing Intermediate Representation**
;; - Hanami templates serve as the IR between API and renderers
;; - Template substitution is powerful but often confusing in debugging
;; - Unclear which substitution parameters can be used when
;; - Error messages reference template internals, not user code
;;
;; **6. Inflexible Data Handling**
;; - Only accepts tech.ml.dataset datasets
;; - Cannot directly visualize plain Clojure maps/vectors
;; - Requires conversion step for simple data
;;
;; ### The Opportunity
;;
;; A fresh design exploration allows us to:
;; - **Separate concerns**: Clean separation between algebra, IR, and rendering
;; - **Support multiple backends**: One API, many rendering targets
;; - **Use standard Clojure**: Layers as plain maps, standard lib operations
;; - **Make debugging clear**: Transparent IR that's easy to inspect
;; - **Accept any data**: Plain maps, datasets, or custom structures
;;
;; This notebook explores whether an Algebra of Graphics approach can
;; deliver these benefits while maintaining a coherent, learnable API.
;;
;; ## Scope of This Exploration
;;
;; - **Goal**: Design and prototype an alternative compositional API
;; - **Non-goal**: Replace existing Tableplot APIs immediately
;; - **Outcome**: This may become an additional namespace in Tableplot v1,
;;   coexisting with `hanami` and `plotly` APIs
;; - **Audience**: Tableplot maintainers and contributors evaluating design options

;; # 2. Inspiration: AlgebraOfGraphics.jl
;;
;; This design is inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/),
;; a visualization library that builds on decades of experience from systems
;; like ggplot2 while introducing clearer algebraic principles.
;;
;; ## Core Insight: Layers + Algebra
;;
;; AlgebraOfGraphics.jl treats visualization as an algebra with:
;;
;; **1. Uniform abstraction**: Everything is a "layer"
;; - Data sources
;; - Aesthetic mappings (x → column, color → column)
;; - Visual marks (scatter, line, bar)
;; - Statistical transformations (regression, smoothing)
;;
;; **2. Two operations**: `*` (product) and `+` (sum)
;;
;; - `*` **merges** layers together (composition)
;;   ```julia
;;   data * mapping(x, y) * scatter
;;   # → Single layer with all properties combined
;;   ```
;;
;; - `+` **overlays** layers (overlay)
;;   ```julia
;;   scatter + line
;;   # → Two separate visual marks on same plot
;;   ```
;;
;; **3. Distributive law**: `a * (b + c) = (a * b) + (a * c)`
;;   ```julia
;;   data * mapping(x, y) * (scatter + line)
;;   # ↓ expands to
;;   (data * mapping * scatter) + (data * mapping * line)
;;   ```
;;
;; This allows factoring out common properties and applying them to
;; multiple plot types without repetition.
;;
;; ## Comparison to ggplot2
;;
;; ggplot2 uses `+` for everything:
;; ```r
;; ggplot(data, aes(x, y)) + 
;;   geom_point() +
;;   geom_smooth()
;; ```
;;
;; AlgebraOfGraphics separates concerns:
;; ```julia
;; data * mapping(x, y) * (scatter + smooth)
;; ```
;;
;; **Why two operators?**
;; - **Clarity**: `*` = "combine properties", `+` = "overlay visuals"  
;; - **Composability**: `data * mapping` can be reused across plot types
;; - **Mathematical elegance**: Follows distributive law naturally
;;
;; ## Translating to Clojure
;;
;; Julia's algebraic approach relies on:
;; - Custom `*` and `+` operators defined on Layer types
;; - Multiple dispatch to handle type combinations
;; - Object-oriented layer representations
;;
;; **Key challenge**: How do we bring this algebraic elegance to Clojure while:
;; - Using plain data structures (maps, not objects)
;; - Enabling standard library operations (merge, assoc, filter)
;; - Maintaining the compositional benefits
;; - Making the IR transparent and inspectable
;;
;; The answer: **Layers as flat maps with namespaced keys**, combined with
;; operators that work on map collections.

;; # 3. Design Exploration
;;
;; Let's explore three different approaches to structuring layer specifications,
;; showing the evolution toward a design that works naturally with Clojure's
;; standard library.

;; ## Approach 1: Nested Structure
;;
;; This mirrors how traditional plotting libraries often work internally.

(def nested-layer-example
  {:transformation nil
   :data {:bill-length-mm [39.1 39.5 40.3]
          :bill-depth-mm [18.7 17.4 18.0]
          :species [:adelie :adelie :adelie]}
   :positional [:bill-length-mm :bill-depth-mm]
   :named {:color :species}
   :attributes {:alpha 0.5}})

;; **Problem**: Standard `merge` doesn't compose correctly

(merge {:positional [:x] :named {:color :species}}
       {:positional [:y] :named {:size :body-mass}})
;; => {:positional [:y]           ;; Lost :x!
;;     :named {:size :body-mass}} ;; Lost :color!

;; Standard merge overwrites rather than combines:
;; - `:positional` vectors should concatenate, but instead [:y] replaces [:x]
;; - `:named` maps should merge, but instead {:size ...} replaces {:color ...}
;;
;; **Consequence**: Need custom `merge-layer` function with special logic.
;; This means standard Clojure operations don't work on layer specifications.

;; ## Approach 2: Flat Structure with Plain Keys
;;
;; What if we flatten everything to the same level?

(def flat-layer-example-v1
  {:data {:bill-length-mm [39.1 39.5 40.3]
          :bill-depth-mm [18.7 17.4 18.0]
          :species [:adelie :adelie :adelie]}
   :x :bill-length-mm
   :y :bill-depth-mm
   :color :species
   :alpha 0.5
   :plottype :scatter})

;; **Advantage**: Standard `merge` works perfectly!

(merge {:data {:bill-length-mm [39.1] :bill-depth-mm [18.7]} :x :bill-length-mm}
       {:y :bill-depth-mm :color :species}
       {:plottype :scatter :alpha 0.5})
;; => All properties combine correctly!

;; **Problem**: Collision with data columns

(def tricky-data
  {:x [1 2 3]
   :y [4 5 6]
   :plottype [:a :b :c]}) ;; Dataset has column named :plottype!

;; This becomes ambiguous:
;; {:data tricky-data :plottype :scatter :y :plottype}
;;                     ^^^^^^^^           ^^^^^^^^^
;;                     layer metadata?    or mapping to column?
;;
;; With plain keys, we can't distinguish layer metadata from data columns.

;; ## Approach 3: Flat Structure with Namespaced Keys ✅
;;
;; Use Clojure's namespaced keywords to prevent collisions!

(def flat-layer-example-v2
  #:aog{:data {:bill-length-mm [39.1 39.5 40.3]
               :bill-depth-mm [18.7 17.4 18.0]
               :species [:adelie :adelie :adelie]}
        :x :bill-length-mm
        :y :bill-depth-mm
        :color :species
        :alpha 0.5
        :plottype :scatter})

;; The `#:aog{...}` syntax expands to:
;; {:aog/data {...}
;;  :aog/x :bill-length-mm
;;  :aog/y :bill-depth-mm
;;  :aog/color :species
;;  :aog/alpha 0.5
;;  :aog/plottype :scatter}

;; **Advantages**:
;; 1. ✅ Standard `merge` works (flat structure)
;; 2. ✅ No collision with data columns (namespaced keys)
;; 3. ✅ Clear distinction: `:aog/plottype` vs `:plottype` column
;; 4. ✅ Concise with namespace map syntax `#:aog{...}`
;; 5. ✅ All standard library operations work (assoc, update, mapv, filter)
;;
;; **Trade-off**: Slightly more verbose than plain keys
;; - Plain: `:color` (6 chars)
;; - Namespaced: `:aog/color` (11 chars), or `#:aog{:color ...}` in map context
;;
;; **This is our chosen approach.**
;;
;; ## Why This Matters: Julia's Algebraic Culture → Clojure Data Structures
;;
;; Julia's AlgebraOfGraphics.jl uses:
;; - Custom Layer types with specialized `*` and `+` operators
;; - Multiple dispatch to handle type combinations
;; - Object-oriented composition
;;
;; Our Clojure translation:
;; - Layers are **just maps** with `:aog/*` namespaced keys
;; - `*` and `+` are **functions** that work on map collections
;; - Composition uses **standard merge**, not custom logic
;;
;; **Result**: The same algebraic elegance, but layers are transparent data
;; that work with Clojure's entire standard library:
;; - `merge` - Combine layers
;; - `assoc` - Add properties
;; - `update` - Modify values
;; - `mapv` - Transform all layers
;; - `filterv` - Select specific layers
;; - `into` - Accumulate layers
;;
;; This is a fundamental design advantage: **algebraic operations on plain data**.

;; # 4. Proposed Design
;;
;; ## API Overview
;;
;; The API consists of three parts:
;; 1. **Constructors** - Build partial layer specifications
;; 2. **Operators** - Compose layers (`*`) and overlay them (`+`)
;; 3. **Renderers** - Convert layers to visualizations
;;
;; Implementation details are in Section 6. Here we show signatures and usage.

;; ## Constructors

(defn data
  "Attach a dataset to a layer.
  
  Accepts:
  - Plain Clojure map: {:x [1 2 3] :y [4 5 6]}
  - tech.ml.dataset: (tc/dataset ...)
  
  Returns: Layer map with :aog/data"
  [dataset]
  {:aog/data dataset})

(defn mapping
  "Define aesthetic mappings from data columns to visual properties.
  
  Args:
  - x, y: Column names (keywords) for positional aesthetics
  - named: (optional) Map of other aesthetics {:color :species, :size :body-mass}
  
  Mappings tell the renderer which columns to use for which visual properties.
  
  Examples:
  (mapping :bill-length-mm :bill-depth-mm)
  (mapping :bill-length-mm :bill-depth-mm {:color :species})
  (mapping :x :y {:color :group :size :value})"
  ([x y]
   {:aog/x x :aog/y y})
  ([x y named]
   (merge {:aog/x x :aog/y y}
          (update-keys named #(keyword "aog" (name %))))))

(defn scatter
  "Create a scatter plot layer.
  
  Args:
  - attrs: (optional) Map of visual attributes {:alpha 0.5, :size 100}
  
  Attributes are constant values applied to all points.
  Contrast with mappings, which vary by data.
  
  Examples:
  (scatter)
  (scatter {:alpha 0.7})"
  ([]
   {:aog/plottype :scatter})
  ([attrs]
   (merge {:aog/plottype :scatter}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn line
  "Create a line plot layer.
  
  Connects points in x-order with lines."
  ([]
   {:aog/plottype :line})
  ([attrs]
   (merge {:aog/plottype :line}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn linear
  "Add linear regression transformation.
  
  Computes best-fit line through points.
  When combined with color aesthetic, computes separate regression per group.
  
  Returns: Layer with :aog/transformation :linear"
  []
  {:aog/transformation :linear
   :aog/plottype :line})

(defn smooth
  "Statistical transformation: LOESS smoothing.
  
  (Note: API defined, implementation pending)"
  []
  {:aog/transformation :smooth
   :aog/plottype :line})

(defn density
  "Statistical transformation: Kernel density estimation.
  
  (Note: API defined, implementation pending)"
  []
  {:aog/transformation :density
   :aog/plottype :area})

(defn histogram
  "Plot type: Histogram with binning.
  
  (Note: API defined, implementation pending)"
  ([] {:aog/plottype :histogram})
  ([opts] (merge {:aog/plottype :histogram} opts)))

(defn area
  "Plot type: Area plot (filled line plot)."
  ([] [{:aog/plottype :area}])
  ([opts] [(merge {:aog/plottype :area} opts)]))

(defn bar
  "Plot type: Bar chart."
  ([] [{:aog/plottype :bar}])
  ([opts] [(merge {:aog/plottype :bar} opts)]))

;; ## Algebraic Operators

(defn *
  "Merge layer specifications (product/composition).
  
  The `*` operator combines layer properties through merge.
  It handles multiple input types to enable flexible composition:
  
  - Map × Map → [Map] - Merge and wrap in vector
  - Map × Vec → Vec - Merge map into each vector element  
  - Vec × Vec → Vec - Cartesian product with merge
  
  The distributive law holds:
  (* a (+ b c)) = (+ (* a b) (* a c))
  
  Examples:
  (* (data df) (mapping :x :y) (scatter))
  ;; => [{:aog/data df :aog/x :x :aog/y :y :aog/plottype :scatter}]
  
  (* (data df) (mapping :x :y) (+ (scatter) (line)))
  ;; => [{:aog/data df ... :plottype :scatter}
  ;;     {:aog/data df ... :plottype :line}]"
  ([x] (if (map? x) [x] x))
  ([x y]
   (cond
     (and (map? x) (map? y))
     [(merge x y)]

     (and (map? x) (vector? y))
     (mapv #(merge x %) y)

     (and (vector? x) (map? y))
     (mapv #(merge % y) x)

     (and (vector? x) (vector? y))
     (vec (for [a x, b y] (merge a b)))))
  ([x y & more]
   (reduce * (* x y) more)))

(defn +
  "Combine multiple layer specifications for overlay (sum).
  
  The `+` operator creates multiple layers that will be rendered together.
  Simply concatenates layers into a vector.
  
  Example:
  (+ (scatter) (linear))
  ;; => [{:aog/plottype :scatter} 
  ;;     {:aog/transformation :linear :aog/plottype :line}]
  
  Combined with *:
  (* (data df) (mapping :x :y) (+ (scatter) (linear)))
  ;; => Two layers, both with same data and mapping"
  [& layer-specs]
  (vec (mapcat #(if (vector? %) % [%]) layer-specs)))

;; ## Renderers
;;
;; Signatures only - implementations in Section 6.

(declare plot plot-vega draw)

;; plot : layers → SVG (via thi.ng/geom-viz)
;; plot-vega : layers → Vega-Lite spec
;; draw : layers + opts → dispatch to backend

;; # 5. Examples
;;
;; These examples demonstrate the design in practice, showing how the
;; algebraic approach and data-oriented representation work together.

;; ## Setup: Load Datasets

;; Palmer Penguins - 344 observations, 3 species
(def penguins (tc/drop-missing (rdatasets/palmerpenguins-penguins)))

;; Motor Trend Car Road Tests - 32 automobiles
(def mtcars (rdatasets/datasets-mtcars))

;; Fisher's Iris - 150 flowers, 3 species
(def iris (rdatasets/datasets-iris))

;; ## Example 1: Simple Scatter Plot

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)))

;; Breaking this down:
;; 1. (data penguins) → {:aog/data penguins}
;; 2. (mapping :bill-length-mm :bill-depth-mm) → {:aog/x :bill-length-mm :aog/y :bill-depth-mm}
;; 3. (scatter) → {:aog/plottype :scatter}
;; 4. (* ...) merges all three → [{:aog/data penguins :aog/x ... :aog/y ... :aog/plottype :scatter}]
;; 5. (plot ...) renders the layer vector to SVG

;; ## Example 2: Multi-Layer Plot (Scatter + Regression)
;;
;; This demonstrates the key compositional feature: overlaying multiple
;; visual layers that share data and aesthetic mappings.

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (+ (scatter {:alpha 0.5})
       (linear))))

;; How this works:
;; 1. (+ (scatter ...) (linear)) creates two layer specs
;; 2. (* (data ...) (mapping ...) (+ ...)) distributes data+mapping to both
;; 3. Result: Two complete layers with same data/mapping, different plot types
;; 4. Both rendered on same plot
;;
;; This is the distributive law in action:
;; (* a (+ b c)) = (+ (* a b) (* a c))
;;
;; Notice: Linear regression computed separately for each species (by :color grouping)

;; ## Example 3: Standard Clojure Operations Work
;;
;; Because layers are plain maps, all standard library operations work.
;; This example demonstrates merge, assoc, update, and mapv.

;; Build with standard merge
(def layer-with-merge
  (merge (data penguins)
         (mapping :bill-length-mm :bill-depth-mm {:color :species})
         (scatter {:alpha 0.7})))

layer-with-merge ;; Inspect - it's just a map!

(plot [layer-with-merge])

;; Add properties with standard assoc
(def base-layer
  (merge (data penguins)
         (mapping :bill-length-mm :bill-depth-mm)
         (scatter)))

(def with-color
  (assoc base-layer :aog/color :species))

(plot [with-color])

;; Modify with standard update
(def with-alpha
  (merge (data penguins)
         (mapping :bill-length-mm :bill-depth-mm)
         (scatter {:alpha 0.3})))

(def doubled-alpha
  (update with-alpha :aog/alpha * 2))

(:aog/alpha doubled-alpha) ;; => 0.6

(plot [doubled-alpha])

;; Transform all layers with mapv
(def multi-layer
  (* (data penguins)
     (mapping :bill-length-mm :bill-depth-mm)
     (+ (scatter) (line))))

;; Add alpha to all layers
(def all-with-alpha
  (mapv #(assoc % :aog/alpha 0.6) multi-layer))

(plot all-with-alpha)

;; **Key insight**: No custom functions needed. Standard Clojure operations
;; work because layers are just maps with namespaced keys.

;; ## Example 4: Conditional Layer Building
;;
;; Build plots conditionally using standard Clojure control flow and `into`.

(defn make-plot [dataset show-regression?]
  (let [base (* (data dataset)
                (mapping :bill-length-mm :bill-depth-mm {:color :species})
                (scatter {:alpha 0.5}))

        with-regression (if show-regression?
                          (into base
                                (* (data dataset)
                                   (mapping :bill-length-mm :bill-depth-mm {:color :species})
                                   (linear)))
                          base)]
    (plot with-regression)))

;; Just scatter
(make-plot penguins false)

;; Scatter + regression
(make-plot penguins true)

;; With 344 penguin observations (100+ per species), regression lines are
;; statistically meaningful. Standard `into` naturally accumulates layers.

;; ## Example 5: Faceting (Small Multiples)
;;
;; Faceting creates separate panels for each category, demonstrating how
;; compositional power scales to more complex visualizations.
;;
;; (Note: Currently implemented in Vega-Lite backend only)

(plot-vega
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species :col :island})
    (scatter {:alpha 0.7})))

;; Each island gets its own panel, with species shown by color.
;; Notice species distribution varies by island!

;; Faceting with multiple layers works too:
(plot-vega
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species :col :island})
    (+ (scatter {:alpha 0.5})
       (linear))))

;; Each facet shows both scatter points and regression lines.

;; ## Example 6: Backend Independence
;;
;; The same layer specification can be rendered with different backends.
;; This demonstrates clean separation between layer IR and rendering.

;; Define visualization once
(def viz-layers
  (* (data penguins)
     (mapping :bill-length-mm :bill-depth-mm {:color :species})
     (scatter {:alpha 0.7})))

;; Render with geom-viz (static SVG, ggplot2 aesthetics)
(plot viz-layers)

;; Render with Vega-Lite (interactive, tooltips, zoom/pan)
(plot-vega viz-layers)

;; Or use unified API with backend option
(draw viz-layers {:backend :geom-viz})
(draw viz-layers {:backend :vega-lite})

;; **Key insight**: Same layer specification, different renderers.
;; The IR is truly backend-agnostic.

;; # 6. Implementation
;;
;; This section reveals how the API works under the hood.
;; The implementation is surprisingly simple because the design leverages
;; standard Clojure operations rather than custom logic.

;; ## Helper Functions

(defn- get-column-data
  "Extract column data from dataset.
  
  Handles both plain maps and tech.ml.dataset datasets."
  [data col-key]
  (vec (get data col-key)))

(defn- infer-domain
  "Infer domain from data values.
  
  For numeric data: [min max]
  For categorical: vector of unique values"
  [values]
  (cond
    (empty? values)
    [0 1]

    (every? number? values)
    [(apply min values) (apply max values)]

    :else
    values))

(defn- layer->points
  "Convert layer to point data for rendering.
  
  Extracts x, y, and optional color values from the layer's dataset
  based on the aesthetic mappings."
  [layer]
  (let [data (:aog/data layer)
        x-vals (get-column-data data (:aog/x layer))
        y-vals (get-column-data data (:aog/y layer))
        color-col (:aog/color layer)
        color-vals (when (keyword? color-col)
                     (get-column-data data color-col))
        n (count x-vals)]
    (mapv (fn [i]
            {:x (nth x-vals i)
             :y (nth y-vals i)
             :color (when color-vals (nth color-vals i))})
          (range n))))

(defn- compute-linear-regression
  "Compute linear regression for a set of points using fastmath.
  
  Returns: Vector of {:x :y} points representing the fitted line."
  [points]
  (when (>= (count points) 2)
    (let [x-vals (mapv :x points)
          y-vals (mapv :y points)]

      (if (= (count points) 2)
        ;; With exactly 2 points, just draw a line between them
        (let [x-min (apply min x-vals)
              x-max (apply max x-vals)
              y1 (first y-vals)
              y2 (second y-vals)
              x1 (first x-vals)
              x2 (second x-vals)
              slope (if (= x1 x2) 0 (clojure.core// (clojure.core/- y2 y1) (clojure.core/- x2 x1)))
              intercept (clojure.core/- y1 (clojure.core/* slope x1))
              x-range (if (= x-min x-max)
                        [x-min]
                        (let [step (clojure.core// (clojure.core/- x-max x-min) 100.0)]
                          (vec (concat (range x-min x-max step) [x-max]))))]
          (mapv (fn [x]
                  {:x x
                   :y (clojure.core/+ intercept (clojure.core/* slope x))})
                x-range))

        ;; With 3+ points, use fastmath regression
        (try
          (let [xss (mapv vector x-vals)
                model (regr/lm y-vals xss)
                intercept (:intercept model)
                slope (first (:beta model))
                x-min (apply min x-vals)
                x-max (apply max x-vals)
                x-range (if (= x-min x-max)
                          [x-min]
                          (let [step (clojure.core// (clojure.core/- x-max x-min) 100.0)]
                            (vec (concat (range x-min x-max step) [x-max]))))]
            (mapv (fn [x]
                    {:x x
                     :y (clojure.core/+ intercept (clojure.core/* slope x))})
                  x-range))
          (catch Exception e
            nil))))))

(defn- unique-values
  "Get unique values from a sequence, preserving order."
  [coll]
  (vec (distinct coll)))

(defn- color-scale
  "Create a ggplot2-like color scale for categorical data.

  Uses the ggplot2 default hue scale (evenly spaced hues around the color wheel)."
  [categories]
  (let [colors ["#F8766D" ;; red-orange
                "#00BA38" ;; green
                "#619CFF" ;; blue
                "#F564E3" ;; magenta
                "#00BFC4" ;; cyan
                "#B79F00" ;; yellow-brown
                "#FF61CC" ;; pink
                "#00B4F0" ;; sky blue
                "#C77CFF" ;; purple
                "#00C19A" ;; teal
                "#FF6A98" ;; rose
                "#00A9FF"]] ;; azure
    (zipmap categories (cycle colors))))

;; ## Rendering Backend: thi.ng/geom-viz
;;
;; Primary backend for static SVG with ggplot2 aesthetics.

(defn- layer->scatter-spec
  "Convert a scatter layer to thi.ng/geom-viz spec."
  [layer width height]
  (let [points (layer->points layer)
        x-vals (mapv :x points)
        y-vals (mapv :y points)
        x-domain (infer-domain x-vals)
        y-domain (infer-domain y-vals)

        x-numeric? (and (vector? x-domain) (= 2 (count x-domain)) (every? number? x-domain))
        y-numeric? (and (vector? y-domain) (= 2 (count y-domain)) (every? number? y-domain))]

    (when (and x-numeric? y-numeric?)
      (let [alpha (or (:aog/alpha layer) 1.0)
            color-groups (when-let [colors (seq (keep :color points))]
                           (group-by :color points))
            has-color? (some? color-groups)

            x-range (- (second x-domain) (first x-domain))
            y-range (- (second y-domain) (first y-domain))
            x-major (clojure.core/* x-range 0.2)
            y-major (clojure.core/* y-range 0.2)]

        {:x-axis (viz/linear-axis
                  {:domain x-domain
                   :range [50 (- width 50)]
                   :major x-major
                   :pos (- height 50)})
         :y-axis (viz/linear-axis
                  {:domain y-domain
                   :range [(- height 50) 50]
                   :major y-major
                   :pos 50})
         :grid {:attribs {:stroke "#FFFFFF" :stroke-width 1}}
         :data (if has-color?
                 (let [categories (unique-values (keep :color points))
                       colors (color-scale categories)]
                   (mapv (fn [category]
                           (let [cat-points (get color-groups category)
                                 point-data (mapv (fn [p] [(:x p) (:y p)]) cat-points)
                                 fill-color (get colors category)]
                             {:values point-data
                              :layout viz/svg-scatter-plot
                              :attribs {:fill fill-color
                                        :stroke fill-color
                                        :stroke-width 0.5
                                        :opacity alpha}}))
                         categories))
                 [{:values (mapv (fn [p] [(:x p) (:y p)]) points)
                   :layout viz/svg-scatter-plot
                   :attribs {:fill "#333333"
                             :stroke "#333333"
                             :stroke-width 0.5
                             :opacity alpha}}])}))))

(defn- layer->line-spec
  "Convert a line layer to thi.ng/geom-viz spec.
  
  Handles :aog/transformation :linear by computing regression lines."
  [layer width height]
  (let [transformation (:aog/transformation layer)
        points (layer->points layer)

        processed-points (if (= transformation :linear)
                           (let [color-vals (keep :color points)]
                             (if (seq color-vals)
                               (let [color-groups (group-by :color points)]
                                 (mapcat (fn [[color group-points]]
                                           (when-let [fitted (compute-linear-regression group-points)]
                                             (mapv #(assoc % :color color) fitted)))
                                         color-groups))
                               (compute-linear-regression points)))
                           points)

        sorted-points (sort-by :x processed-points)
        x-vals (mapv :x sorted-points)
        y-vals (mapv :y sorted-points)
        x-domain (infer-domain x-vals)
        y-domain (infer-domain y-vals)

        x-numeric? (and (vector? x-domain) (= 2 (count x-domain)) (every? number? x-domain))
        y-numeric? (and (vector? y-domain) (= 2 (count y-domain)) (every? number? y-domain))]

    (when (and x-numeric? y-numeric?)
      (let [alpha (or (:aog/alpha layer) 1.0)
            color-groups (when-let [colors (seq (keep :color processed-points))]
                           (group-by :color processed-points))
            has-color? (some? color-groups)

            x-range (clojure.core/- (second x-domain) (first x-domain))
            y-range (clojure.core/- (second y-domain) (first y-domain))
            x-major (clojure.core/* x-range 0.2)
            y-major (clojure.core/* y-range 0.2)]

        {:x-axis (viz/linear-axis
                  {:domain x-domain
                   :range [50 (clojure.core/- width 50)]
                   :major x-major
                   :pos (clojure.core/- height 50)})
         :y-axis (viz/linear-axis
                  {:domain y-domain
                   :range [(clojure.core/- height 50) 50]
                   :major y-major
                   :pos 50})
         :grid {:attribs {:stroke "#FFFFFF" :stroke-width 1}}
         :data (if has-color?
                 (let [categories (unique-values (keep :color processed-points))
                       colors (color-scale categories)]
                   (mapv (fn [category]
                           (let [cat-points (get color-groups category)
                                 sorted-cat (sort-by :x cat-points)
                                 line-data (mapv (fn [p] [(:x p) (:y p)]) sorted-cat)]
                             {:values line-data
                              :layout viz/svg-line-plot
                              :attribs {:stroke (get colors category)
                                        :stroke-width 1
                                        :fill "none"
                                        :opacity alpha}}))
                         categories))
                 [{:values (mapv (fn [p] [(:x p) (:y p)]) sorted-points)
                   :layout viz/svg-line-plot
                   :attribs {:stroke "#333333"
                             :stroke-width 1
                             :fill "none"
                             :opacity alpha}}])}))))

(defn- layers->svg
  "Convert layers to SVG string using thi.ng/geom-viz.

  Handles multiple layers by overlaying them in a single plot.
  Adds ggplot2-like styling with gray panel background and white gridlines."
  [layers width height]
  (let [layers (if (vector? layers) layers [layers])

        specs (keep (fn [layer]
                      (let [plottype (:aog/plottype layer)]
                        (case plottype
                          :scatter (layer->scatter-spec layer width height)
                          :line (layer->line-spec layer width height)
                          (layer->scatter-spec layer width height))))
                    layers)]

    (when (seq specs)
      (let [panel-bg "#EBEBEB"
            panel-border "#FFFFFF"
            panel-left 50
            panel-right (- width 50)
            panel-top 50
            panel-bottom (- height 50)
            panel-width (- panel-right panel-left)
            panel-height (- panel-bottom panel-top)
            bg-rect (svg/rect [panel-left panel-top] panel-width panel-height
                              {:fill panel-bg
                               :stroke panel-border
                               :stroke-width 1})]

        (if (= 1 (count specs))
          (svg/serialize
           (svg/svg {:width width :height height}
                    bg-rect
                    (viz/svg-plot2d-cartesian (first specs))))

          (let [combined-spec {:x-axis (:x-axis (first specs))
                               :y-axis (:y-axis (first specs))
                               :grid (:grid (first specs))
                               :data (vec (mapcat :data specs))}]
            (svg/serialize
             (svg/svg {:width width :height height}
                      bg-rect
                      (viz/svg-plot2d-cartesian combined-spec)))))))))

(defn plot
  "Render layers as an SVG visualization using thi.ng/geom-viz.
  
  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with :width (default 600) and :height (default 400)
  
  Returns:
  - Kindly-wrapped HTML containing SVG string"
  ([layers]
   (plot layers {}))
  ([layers opts]
   (let [layers (if (vector? layers) layers [layers])
         width (or (:width opts) 600)
         height (or (:height opts) 400)
         svg-string (layers->svg layers width height)]
     (kind/html svg-string))))

;; ## Alternative Backend: Vega-Lite
;;
;; Interactive visualizations with additional features.

(defn- layer->vega-data
  "Convert layer dataset to Vega-Lite data format."
  [layer]
  (let [data (:aog/data layer)]
    (if (tc/dataset? data)
      (tc/rows data :as-maps)
      (tc/rows (tc/dataset data) :as-maps))))

(defn- layer->vega-scatter
  "Convert a scatter layer to Vega-Lite mark spec."
  [layer]
  (let [x (:aog/x layer)
        y (:aog/y layer)
        color (:aog/color layer)
        col (:aog/col layer)
        row (:aog/row layer)
        facet (:aog/facet layer)
        alpha (or (:aog/alpha layer) 1.0)

        encoding (cond-> {:x {:field (name x)
                              :type "quantitative"
                              :scale {:zero false}}
                          :y {:field (name y)
                              :type "quantitative"
                              :scale {:zero false}}}
                   color (assoc :color {:field (name color) :type "nominal"})
                   col (assoc :column {:field (name col) :type "nominal"})
                   row (assoc :row {:field (name row) :type "nominal"})
                   facet (assoc :facet {:field (name facet) :type "nominal"}))]

    {:mark {:type "point" :opacity alpha}
     :encoding encoding}))

(defn- layer->vega-line
  "Convert a line layer to Vega-Lite mark spec.

  Handles :aog/transformation :linear with Vega-Lite's regression transform."
  [layer]
  (let [x (:aog/x layer)
        y (:aog/y layer)
        color (:aog/color layer)
        col (:aog/col layer)
        row (:aog/row layer)
        facet (:aog/facet layer)
        transformation (:aog/transformation layer)

        encoding (cond-> {:x {:field (name x)
                              :type "quantitative"
                              :scale {:zero false}}
                          :y {:field (name y)
                              :type "quantitative"
                              :scale {:zero false}}}
                   color (assoc :color {:field (name color) :type "nominal"})
                   col (assoc :column {:field (name col) :type "nominal"})
                   row (assoc :row {:field (name row) :type "nominal"})
                   facet (assoc :facet {:field (name facet) :type "nominal"}))

        transform (when (= transformation :linear)
                    [{:regression (name y)
                      :on (name x)
                      :groupby (when color [(name color)])}])]

    (cond-> {:mark {:type "line"}
             :encoding encoding}
      transform (assoc :transform transform))))

(defn- layer->vega-area
  "Convert an area layer to Vega-Lite mark spec."
  [layer]
  (let [spec (layer->vega-line layer)]
    (assoc spec :mark {:type "area"})))

(defn- layer->vega-bar
  "Convert a bar layer to Vega-Lite mark spec."
  [layer]
  (let [x (:aog/x layer)
        y (:aog/y layer)
        color (:aog/color layer)

        encoding (cond-> {:x {:field (name x) :type "nominal"}
                          :y {:field (name y)
                              :type "quantitative"
                              :scale {:zero false}}}
                   color (assoc :color {:field (name color) :type "nominal"}))]

    {:mark {:type "bar"}
     :encoding encoding}))

(defn- layers->vega-spec
  "Convert layers to Vega-Lite specification."
  [layers width height]
  (let [layers (if (vector? layers) layers [layers])
        first-layer (first layers)
        data-values (layer->vega-data first-layer)

        layer-specs (mapv (fn [layer]
                            (let [plottype (:aog/plottype layer)]
                              (case plottype
                                :scatter (layer->vega-scatter layer)
                                :line (layer->vega-line layer)
                                :area (layer->vega-area layer)
                                :bar (layer->vega-bar layer)
                                (layer->vega-scatter layer))))
                          layers)]

    (if (= 1 (count layer-specs))
      (merge (first layer-specs)
             {:width width
              :height height
              :data {:values data-values}})
      {:width width
       :height height
       :data {:values data-values}
       :layer layer-specs})))

(defn plot-vega
  "Render layers as a Vega-Lite visualization.

  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with :width (default 400) and :height (default 300)

  Returns:
  - Kindly-wrapped Vega-Lite spec"
  ([layers]
   (plot-vega layers {}))
  ([layers opts]
   (let [width (or (:width opts) 400)
         height (or (:height opts) 300)
         spec (layers->vega-spec layers width height)]
     (kind/vega-lite spec))))

(defn draw
  "Unified rendering function supporting multiple backends.

  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with:
    - :backend - :geom-viz (default, static SVG) or :vega-lite (interactive)
    - :width - Width in pixels (default 600 for geom-viz, 400 for vega-lite)
    - :height - Height in pixels (default 400 for geom-viz, 300 for vega-lite)

  Returns:
  - Kindly-wrapped visualization (HTML with SVG, or Vega-Lite spec)

  Examples:
  (draw layers)                          ;; Uses geom-viz backend by default
  (draw layers {:backend :vega-lite})    ;; Interactive Vega-Lite visualization
  (draw layers {:backend :geom-viz :width 800 :height 600})"
  ([layers]
   (draw layers {}))
  ([layers opts]
   (let [backend (or (:backend opts) :geom-viz)]
     (case backend
       :geom-viz (plot layers opts)
       :vega-lite (plot-vega layers opts)
       :vegalite (plot-vega layers opts)
       (plot layers opts)))))

;; # 7. Trade-offs & Design Decisions
;;
;; ## What We Gain
;;
;; **1. Composability Through Standard Operations**
;; - Layers compose with `merge`, not custom `merge-layer` function
;; - All standard library operations work: `assoc`, `update`, `mapv`, `filter`, `into`
;; - No need to learn custom combinators
;;
;; **2. Transparent Intermediate Representation**
;; - Layers are plain maps - inspect them with `(first layers)`
;; - No hidden template substitution to debug
;; - Error messages reference actual data structures
;;
;; **3. Backend Independence**
;; - Same layer spec works with multiple renderers
;; - Easy to add new backends (just write conversion functions)
;; - No backend-specific jargon in user-facing API
;;
;; **4. Flexible Data Handling**
;; - Accepts plain Clojure maps: `{:x [1 2 3] :y [4 5 6]}`
;; - Accepts tech.ml.dataset datasets
;; - No forced conversion step
;;
;; **5. Clear Separation of Concerns**
;; - Algebra (operators) separate from IR (layer maps)
;; - IR separate from rendering (backends)
;; - Each layer can be understood independently
;;
;; ## What We Pay
;;
;; **1. Namespace Verbosity**
;; - Plain key: `:color` (6 chars)
;; - Namespaced: `:aog/color` (11 chars)
;; - Mitigated by namespace map syntax: `#:aog{:color ...}`
;;
;; **2. Novel Operators**
;; - `*` and `+` shadow arithmetic operators
;; - Need `clojure.core/*` for multiplication in implementation
;; - Users must learn algebraic interpretation
;;
;; **3. Incomplete Feature Set (Currently)**
;; - Some statistical transforms defined but not implemented (smooth, density)
;; - Polar coordinates not yet supported
;; - Fewer plot types than mature libraries
;;
;; ## Why These Choices?
;;
;; **Flat + Namespaced Structure**
;; - Enables standard `merge` (the core composability win)
;; - Prevents collisions with data columns
;; - Aligns with modern Clojure practices (Ring 2.0, clojure.spec)
;;
;; **Algebraic Operators (`*` and `+`)**
;; - Directly translates AlgebraOfGraphics.jl concepts
;; - Distributive law enables factoring common properties
;; - Clear distinction: `*` = merge, `+` = overlay
;;
;; **Multiple Backends from Start**
;; - Validates that IR is truly backend-agnostic
;; - geom-viz for static/print, Vega-Lite for interactive
;; - Proves future extensibility
;;
;; ## Comparison to Current Tableplot APIs
;;
;; | Aspect | Current (Hanami/Plotly) | This Design |
;; |--------|------------------------|-------------|
;; | IR | Hanami templates | Flat maps with `:aog/*` keys |
;; | Composition | Template substitution | Standard `merge` |
;; | Debugging | Template internals leak | Inspect plain maps |
;; | Backends | One per API | Multiple via same API |
;; | Data formats | tech.ml.dataset only | Maps or datasets |
;; | Operations | Custom functions | Standard library |
;; | Jargon | Backend-specific | Backend-agnostic |

;; # 8. Integration Path
;;
;; ## Coexistence with Tableplot v1
;;
;; This design is **not a replacement** for existing Tableplot APIs.
;; It's an exploration of an alternative approach that could coexist as
;; an additional namespace.
;;
;; **Possible integration:**
;;
;; ```clojure
;; ;; Existing APIs continue as-is
;; (require '[scicloj.tableplot.v1.hanami :as hanami])
;; (require '[scicloj.tableplot.v1.plotly :as plotly])
;;
;; ;; New AoG API as additional namespace
;; (require '[scicloj.tableplot.v1.aog :as aog])
;;
;; ;; Users choose based on their needs:
;; ;; - hanami: Rich Vega-Lite features, Hanami template power
;; ;; - plotly: Interactive Plotly.js visualizations
;; ;; - aog: Compositional algebra, backend flexibility
;; ```
;;
;; ## Migration & Compatibility
;;
;; **No migration required**:
;; - Existing code continues to work unchanged
;; - Users can adopt AoG API incrementally for new code
;; - APIs can interoperate through shared rendering targets
;;
;; **Potential bridges**:
;; - AoG layers → Vega-Lite spec → render with Hanami
;; - Share color scales, themes between APIs
;; - Common dataset handling utilities
;;
;; ## Next Steps
;;
;; **If pursuing this design:**
;;
;; 1. **Community feedback** - Gather input on design decisions
;; 2. **Complete implementation** - Finish statistical transforms
;; 3. **Performance testing** - Validate with large datasets
;; 4. **Documentation** - User guide and API reference
;; 5. **Integration planning** - Decide namespace structure
;; 6. **Testing** - Comprehensive test suite
;; 7. **Release** - Alpha release for early adopters

;; # 9. Decision Points
;;
;; These are open questions where community input would be valuable.
;;
;; ## 1. Namespace Convention
;;
;; **Current**: `:aog/*` (e.g., `:aog/color`, `:aog/x`)
;;
;; **Alternative**: Use `:=*` prefix similar to Tableplot's current style
;; ```clojure
;; {:=data penguins
;;  :=x :bill-length-mm
;;  :=y :bill-depth-mm
;;  :=color :species
;;  :=plottype :scatter}
;; ```
;;
;; **Comparison**:
;; - `:aog/color` (11 chars) - Standard namespace, discoverable
;; - `:=color` (7 chars) - More concise, less conventional
;;
;; **Question**: Which feels more natural for Clojure users?
;;
;; ## 2. Operator Names
;;
;; **Current**: `*` for merge/product, `+` for overlay/sum
;;
;; **Concerns**:
;; - Shadow arithmetic operators
;; - Require `clojure.core/*` for multiplication internally
;; - Novel interpretation for Clojure users (familiar to Julia users)
;;
;; **Alternatives**:
;; - `compose` and `overlay`
;; - `merge-layers` and `concat-layers`
;; - Keep `*` and `+` for algebraic elegance
;;
;; **Question**: Are symbolic operators worth the potential confusion?
;;
;; ## 3. Statistical Transformation Parameters
;;
;; **Current**: Transformations have no parameters
;; ```clojure
;; (linear)  ;; Returns {:aog/transformation :linear}
;; ```
;;
;; **Future need**: Parameters for transformations
;; ```clojure
;; (smooth {:bandwidth 0.5})
;; (bins {:n 20})
;; ```
;;
;; **Options**:
;; - Store params in layer map: `{:aog/transformation :smooth :aog/bandwidth 0.5}`
;; - Nested transformation spec: `{:aog/transformation {:type :smooth :bandwidth 0.5}}`
;; - Separate transform constructor: `(transform :smooth {:bandwidth 0.5})`
;;
;; **Question**: What's the cleanest approach for transformation configuration?
;;
;; ## 4. Spec Integration
;;
;; With namespaced keys, we could provide clojure.spec validation:
;; ```clojure
;; (s/def :aog/plottype #{:scatter :line :bar :area})
;; (s/def :aog/data (s/or :map map? :dataset tc/dataset?))
;; (s/def :aog/x keyword?)
;; (s/def :aog/alpha (s/and number? #(<= 0 % 1)))
;; (s/def ::layer (s/keys :opt [:aog/data :aog/x :aog/y :aog/color :aog/plottype ...]))
;; ```
;;
;; **Benefits**: Validation, generative testing, documentation
;; **Costs**: Spec dependency, maintenance burden
;;
;; **Question**: Should we provide official specs for layer maps?
;;
;; ## 5. Backend Registration
;;
;; **Current**: Hardcoded backend dispatch in `draw`
;; ```clojure
;; (case backend
;;   :geom-viz (plot layers opts)
;;   :vega-lite (plot-vega layers opts))
;; ```
;;
;; **Alternative**: Extensible backend registry
;; ```clojure
;; (register-backend! :plotly layers->plotly-spec)
;; (draw layers {:backend :plotly})
;; ```
;;
;; **Question**: Should backends be extensible by users, or core-only?
;;
;; ## 6. Faceting Model
;;
;; **Current**: Faceting via aesthetics (`:col`, `:row`, `:facet`)
;; ```clojure
;; (mapping :x :y {:color :species :col :island})
;; ```
;;
;; **Alternative**: Separate faceting layer
;; ```clojure
;; (* (data penguins)
;;    (mapping :x :y {:color :species})
;;    (facet :col :island)
;;    (scatter))
;; ```
;;
;; **Question**: Should faceting be an aesthetic or a separate operator?

;; # Summary
;;
;; ## What We've Explored
;;
;; This notebook demonstrates a complete Algebra of Graphics implementation
;; for Clojure in ~600 lines of code.
;;
;; **Core Design**:
;; - Layers as **flat maps** with `:aog/*` namespaced keys
;; - Algebraic composition using `*` (merge) and `+` (overlay)
;; - Standard library operations work natively (merge, assoc, mapv, filter)
;; - Backend-agnostic IR enables multiple rendering targets
;;
;; **Key Insights**:
;; 1. **Flat structure enables standard merge** - No custom layer-merge needed
;; 2. **Namespacing prevents collisions** - Can coexist with any data columns
;; 3. **Transparent IR** - Layers are inspectable plain maps, not templates
;; 4. **Julia's algebraic culture translates to Clojure data** - Same concepts, different substrate
;;
;; **Implementation Status**:
;; - ✅ Core algebra (`*`, `+`, layer composition)
;; - ✅ Two rendering backends (geom-viz, Vega-Lite)
;; - ✅ Plot types: scatter, line, area, bar
;; - ✅ Statistical transform: linear regression
;; - ✅ Aesthetics: position, color, alpha, faceting
;; - ⚠️ Statistical transforms: smooth, density, histogram (API defined, implementation pending)
;; - ⚠️ Additional coordinate systems (polar, geographic)
;;
;; ## Relationship to Tableplot
;;
;; This is **not a replacement** for current Tableplot APIs, but an exploration
;; of an alternative approach that could coexist as an additional namespace.
;;
;; It addresses several current limitations:
;; - Backend independence (one API, multiple renderers)
;; - Transparent IR (plain maps, not template substitution)
;; - Flexible data handling (maps or datasets)
;; - Composability through standard library
;;
;; ## This is a Design Document
;;
;; The purpose is to:
;; - Demonstrate feasibility of the approach
;; - Explore design trade-offs
;; - Gather community feedback
;; - Inform decision-making about Tableplot's future
;;
;; ## Try It Yourself
;;
;; This notebook is fully functional and self-contained. You can:
;; - Modify the examples to experiment with the API
;; - Add new plot types (extend the backend conversion functions)
;; - Test with your own datasets
;; - Provide feedback on the design decisions
;;
;; **Feedback welcome**: What works? What doesn't? What would you change?
