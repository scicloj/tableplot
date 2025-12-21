;; # Building a Composable Graphics API in Clojure
;; **A Design Exploration for Tableplot**
;;
;; *This notebook explores a fresh approach to composable plot specifications
;; in Clojure, inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/). 
;; It's a working prototype demonstrating an alternative compositional API that addresses 
;; limitations in [Tableplot](https://scicloj.github.io/tableplot)'s current APIs.*
;;
;; ## A Bit of Context: Tableplot's Journey
;;
;; Before we dive into the technical details, let's talk about where we're coming from.
;;
;; Tableplot was created in mid-2024 as a pragmatic plotting solution for the
;; [Noj](https://scicloj.github.io/noj/) toolkit—Clojure's growing data science
;; and scientific computing ecosystem. We needed *something* that worked, and we
;; needed it soon. The goal was to complete an important piece of Noj's offering:
;; a way to visualize data without leaving Clojure or reaching for external tools.
;;
;; And it worked! Tableplot's current APIs 
;; ([`scicloj.tableplot.v1.hanami`](https://scicloj.github.io/tableplot/tableplot_book.hanami_walkthrough.html) and
;; [`scicloj.tableplot.v1.plotly`](https://scicloj.github.io/tableplot/tableplot_book.plotly_walkthrough.html)) 
;; have been used in quite a few serious projects since then. People are actually using it 
;; to get real work done.
;;
;; But here's the thing: **We never intended these APIs to be the final word on
;; plotting in Clojure.** They were a decent compromise—pragmatic, functional,
;; good enough to be useful. We knew they had problems. We knew there were
;; better designs waiting to be explored.
;;
;; ### Learning from Our Users
;;
;; The feedback from Tableplot users has been invaluable. **Thank you** to everyone
;; who took the time to file issues, ask questions, share use cases, and push the
;; library in directions we hadn't anticipated. Your patience with the rough edges
;; and your insights about what works (and what doesn't) have shaped this next iteration.
;;
;; ### The Real-World Data Dev Group
;;
;; This work is also happening in the context of the
;; [Real-World Data dev group](https://scicloj.github.io/docs/community/groups/real-world-data/),
;; recently initiated by Timothy Pratley with new spirit and a stronger focus on
;; open-source collaboration.
;;
;; With that context in mind, let's explore what we're building.

;; # Context & Motivation
;;
;; ## Why Explore a New Approach?
;;
;; Tableplot currently provides two visualization APIs: `scicloj.tableplot.v1.hanami`
;; for [Vega-Lite](https://vega.github.io/vega-lite/) visualizations, and 
;; `scicloj.tableplot.v1.plotly` for [Plotly.js](https://plotly.com/javascript/).
;; Both have proven useful in real projects, but they've also revealed some constraints
;; that are worth thinking about fresh.
;;
;; The `hanami` API extends the original Hanami library, inheriting its template
;; substitution system with uppercase keywords like `:X` and `:Y`. This brings
;; backwards compatibility requirements that affect both APIs, since `plotly`
;; shares some infrastructure with `hanami`. When you want to change something
;; fundamental, you have to consider the impact across multiple layers.
;;
;; Each API is also tied to a specific rendering backend. If you choose `hanami`,
;; you get Vega-Lite—which is excellent for many use cases but has limitations
;; with certain coordinate systems. If you choose `plotly`, you get rich interactivity
;; but rendering static images programmatically becomes tricky. When you hit a
;; limitation of your chosen backend, switching means learning a different API.
;;
;; The intermediate representation between the API and the renderers uses Hanami
;; templates. Template substitution is powerful and flexible, but it can be
;; open-ended and difficult to understand when debugging. Error messages sometimes
;; reference template internals rather than your code, and it's not always clear
;; which substitution parameters are valid in which contexts.
;;
;; The APIs also currently expect [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) 
;; datasets. If you have a simple Clojure map or vector, you need to convert it 
;; first—even for trivial visualizations.
;;
;; ## What We're Exploring
;;
;; Some of these limitations will be addressed within the current APIs themselves—
;; we're actively working on improvements. But as we always intended, it's valuable
;; to explore fresh solutions in parallel. A clean-slate design lets us ask questions
;; that are harder to answer incrementally: Can we separate concerns more cleanly 
;; between the API layer, the intermediate representation, and the rendering?
;; Can one API work with multiple backends? Can we use plain Clojure data structures
;; and standard library operations throughout? Can we make the intermediate
;; representation easier to inspect and debug?
;;
;; This notebook prototypes an alternative compositional API to explore these ideas.
;; It's not meant to replace the existing Tableplot APIs—it might become an additional
;; namespace coexisting with `hanami` and `plotly`, or it might inform future design
;; decisions. This is for Tableplot maintainers, contributors, and curious users who
;; want to provide early feedback on the approach.

;; # Setup
;;
;; This notebook relies on several libraries from the Clojure data science ecosystem.
;; Here's what we use and why:

(ns building-aog-in-clojure
  (:require
   ;; Tablecloth - Dataset manipulation
   ;; https://scicloj.github.io/tablecloth/
   [tablecloth.api :as tc]

   ;; Kindly - Notebook visualization protocol
   ;; https://scicloj.github.io/kindly-noted/
   [scicloj.kindly.v4.kind :as kind]

   ;; thi.ng/geom-viz - Static SVG visualization
   ;; https://github.com/thi-ng/geom
   [thi.ng.geom.viz.core :as viz]
   [thi.ng.geom.svg.core :as svg]

   ;; Fastmath - Statistical computations
   ;; https://github.com/generateme/fastmath
   [fastmath.ml.regression :as regr]
   [fastmath.stats :as stats]

   ;; RDatasets - Example datasets
   ;; https://github.com/scicloj/metamorph.ml
   [scicloj.metamorph.ml.rdatasets :as rdatasets]))

;; **Tablecloth** provides our dataset API, wrapping 
;; [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) with a
;; friendly interface. We use it to manipulate data and access column types.
;;
;; **Kindly** is the visualization protocol that lets this notebook render
;; plots in different environments (Clay, Portal, etc.). Each backend returns
;; a Kindly-wrapped spec.
;;
;; **thi.ng/geom** gives us the static SVG backend. It's excellent for
;; ggplot2-style visualizations that can be saved as publication-quality images.
;;
;; **Fastmath** handles our statistical computations, particularly linear
;; regression. It's a comprehensive math library for Clojure.
;;
;; **RDatasets** provides classic datasets (penguins, mtcars, iris) for examples.
;; These are the same datasets you'd find in R or Python data science tutorials.

;; # Inspiration: AlgebraOfGraphics.jl
;;
;; This design is inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/),
;; a visualization library that builds on decades of experience from systems
;; like ggplot2 while introducing clearer compositional principles.
;;
;; ## Core Insight: Layers + Operations
;;
;; AlgebraOfGraphics.jl treats visualization with two key operations on layers:
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
;; **3. Distributive property**: `a * (b + c) = (a * b) + (a * c)`
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
;; Julia's approach relies on:
;; - Custom `*` and `+` operators defined on Layer types
;; - Multiple dispatch to handle type combinations
;; - Object-oriented layer representations
;;
;; **Key challenge**: How do we bring this compositional elegance to Clojure while:
;; - Using plain data structures (maps, not objects)
;; - Enabling standard library operations (merge, assoc, filter)
;; - Maintaining the compositional benefits
;; - Making the intermediate representation transparent and inspectable
;;
;; The next section explores different approaches to solving this challenge.

;; # Design Exploration
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
;; ## Why This Matters: Julia's Compositional Approach → Clojure Data Structures
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
;; **Result**: The same compositional power, but layers are transparent data
;; that work with Clojure's entire standard library:
;; - `merge` - Combine layers
;; - `assoc` - Add properties
;; - `update` - Modify values
;; - `mapv` - Transform all layers
;; - `filterv` - Select specific layers
;; - `into` - Accumulate layers
;;
;; This is a fundamental design advantage: **compositional operations on plain data**.

;; # Proposed Design
;;
;; ## API Overview
;;
;; The API consists of three parts:
;; 1. **Constructors** - Build partial layer specifications
;; 2. **Composition operators** - Merge layers (`*`) and overlay them (`+`)
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

;; ## Composition Operators

(defn *
  "Merge layer specifications (composition).
  
  The `*` operator combines layer properties through merge.
  It handles multiple input types to enable flexible composition:
  
  - Map × Map → [Map] - Merge and wrap in vector
  - Map × Vec → Vec - Merge map into each vector element  
  - Vec × Vec → Vec - Cartesian product with merge
  
  The distributive property holds:
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

;; # Examples
;;
;; These examples demonstrate the design in practice, showing how the
;; compositional approach and data-oriented representation work together.

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
;; This uses the distributive property (from Section 2):
;; (* a (+ b c)) = (+ (* a b) (* a c))
;;
;; Notice: Linear regression computed separately for each species (by :color grouping)

;; ## Example 3: Standard Clojure Operations Work
;;
;; Because layers are plain maps, all standard library operations work.

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

;; **Key insight**: No custom functions needed. Standard Clojure operations
;; work because layers are just maps with namespaced keys.
;;
;; Other standard operations work too: `update`, `mapv`, `filter`, `into`.
;; Example 4 shows conditional building with `into`.

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

;; # Implementation
;;
;; This section reveals how the API works under the hood.
;; The implementation is surprisingly simple because the design leverages
;; standard Clojure operations rather than custom logic.
;;
;; We'll cover:
;; - **Helper functions** for data extraction and transformation
;; - **Primary rendering backend** (thi.ng/geom-viz for static SVG)
;; - **Alternative backend** (Vega-Lite for interactive visualizations)

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
;; [Vega-Lite](https://vega.github.io/vega-lite/) provides interactive visualizations 
;; with additional features.

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

;; ## Plotly.js Backend
;;
;; [Plotly.js](https://plotly.com/javascript/) is a JavaScript library for interactive, 
;; publication-quality graphs. It provides hover tooltips, zooming, panning, and other 
;; interactive features.
;;
;; The Plotly.js spec is JSON-based, similar to [Vega-Lite](https://vega.github.io/vega-lite/) 
;; but with different structure.

(defn- layer->plotly-trace
  "Convert a layer to a Plotly.js trace (data series).
  
  Returns a map representing a Plotly trace with:
  - x, y: Arrays of values
  - type: 'scatter', 'bar', etc.
  - mode: 'markers', 'lines', 'lines+markers'
  - marker: Style properties
  - name: Legend name (for color grouping)"
  [layer]
  (let [data (:aog/data layer)
        dataset (if (tc/dataset? data)
                  data
                  (tc/dataset data))

        x-col (:aog/x layer)
        y-col (:aog/y layer)
        color-col (:aog/color layer)
        plottype (:aog/plottype layer)
        alpha (or (:aog/alpha layer) 1.0)
        transformation (:aog/transformation layer)

        x-data (vec (get-column-data dataset x-col))
        y-data (vec (get-column-data dataset y-col))]

    (cond
      ;; Scatter plot with optional color grouping
      (= plottype :scatter)
      (if (and color-col (keyword? color-col))
        ;; Group by color column
        (let [color-data (vec (get-column-data dataset color-col))
              groups (group-by identity color-data)
              colors ["#F8766D" "#00BA38" "#619CFF" "#F564E3" "#00BFC4"]]
          (mapv (fn [[idx group-val]]
                  (let [indices (keep-indexed (fn [i v] (when (= v group-val) i)) color-data)]
                    {:x (mapv #(nth x-data %) indices)
                     :y (mapv #(nth y-data %) indices)
                     :type "scatter"
                     :mode "markers"
                     :name (str group-val)
                     :marker {:size 8
                              :opacity alpha
                              :color (nth colors idx (first colors))}}))
                (map-indexed vector (keys groups))))
        ;; No grouping
        [{:x x-data
          :y y-data
          :type "scatter"
          :mode "markers"
          :marker {:size 8
                   :opacity alpha
                   :color "#333333"}}])

      ;; Line plot (or linear regression)
      (or (= plottype :line)
          (= transformation :linear))
      (if (= transformation :linear)
        ;; Linear regression
        (if (and color-col (keyword? color-col))
          ;; Grouped regression
          (let [color-data (vec (get-column-data dataset color-col))
                groups (group-by identity color-data)
                colors ["#F8766D" "#00BA38" "#619CFF" "#F564E3" "#00BFC4"]]
            (mapv (fn [[idx group-val]]
                    (let [indices (keep-indexed (fn [i v] (when (= v group-val) i)) color-data)
                          group-points (mapv (fn [i] {:x (nth x-data i) :y (nth y-data i)}) indices)
                          fitted (compute-linear-regression group-points)]
                      {:x (mapv :x fitted)
                       :y (mapv :y fitted)
                       :type "scatter"
                       :mode "lines"
                       :name (str group-val " (regression)")
                       :line {:color (nth colors idx (first colors))
                              :width 2}}))
                  (map-indexed vector (keys groups))))
          ;; Single regression line
          (let [points (mapv (fn [x y] {:x x :y y}) x-data y-data)
                fitted (compute-linear-regression points)]
            [{:x (mapv :x fitted)
              :y (mapv :y fitted)
              :type "scatter"
              :mode "lines"
              :line {:color "#333333" :width 2}}]))
        ;; Regular line plot
        (if (and color-col (keyword? color-col))
          ;; Grouped lines
          (let [color-data (vec (get-column-data dataset color-col))
                groups (group-by identity color-data)
                colors ["#F8766D" "#00BA38" "#619CFF" "#F564E3" "#00BFC4"]]
            (mapv (fn [[idx group-val]]
                    (let [indices (keep-indexed (fn [i v] (when (= v group-val) i)) color-data)]
                      {:x (mapv #(nth x-data %) indices)
                       :y (mapv #(nth y-data %) indices)
                       :type "scatter"
                       :mode "lines"
                       :name (str group-val)
                       :line {:color (nth colors idx (first colors))
                              :width 2}}))
                  (map-indexed vector (keys groups))))
          ;; Single line
          [{:x x-data
            :y y-data
            :type "scatter"
            :mode "lines"
            :line {:color "#333333" :width 2}}]))

      ;; Bar chart
      (= plottype :bar)
      [{:x x-data
        :y y-data
        :type "bar"
        :marker {:opacity alpha}}]

      ;; Default: treat as scatter
      :else
      [{:x x-data
        :y y-data
        :type "scatter"
        :mode "markers"
        :marker {:size 8 :opacity alpha}}])))

(defn- layers->plotly-spec
  "Convert layers to a Plotly.js specification.
  
  Returns a map with:
  - :data - Vector of traces
  - :layout - Layout configuration (title, axes, etc.)"
  [layers width height]
  (let [layers (if (vector? layers) layers [layers])
        traces (vec (mapcat layer->plotly-trace layers))]
    {:data traces
     :layout {:width width
              :height height
              :hovermode "closest"
              :xaxis {:title ""
                      :zeroline false}
              :yaxis {:title ""
                      :zeroline false}}}))

(defn plot-plotly
  "Render layers as an interactive Plotly.js visualization.
  
  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with :width (default 600) and :height (default 400)
  
  Returns:
  - Kindly-wrapped Plotly spec
  
  Examples:
  (plot-plotly layers)
  (plot-plotly layers {:width 800 :height 500})"
  ([layers]
   (plot-plotly layers {}))
  ([layers opts]
   (let [width (or (:width opts) 600)
         height (or (:height opts) 400)
         spec (layers->plotly-spec layers width height)]
     (kind/plotly spec))))

(defn draw
  "Unified rendering function supporting multiple backends.

  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with:
    - :backend - :geom-viz (static SVG), :vega-lite (interactive), or :plotly (interactive)
    - :width - Width in pixels (default 600)
    - :height - Height in pixels (default 400)

  Returns:
  - Kindly-wrapped visualization (HTML with SVG, Vega-Lite spec, or Plotly spec)

  Examples:
  (draw layers)                          ;; Uses geom-viz backend by default
  (draw layers {:backend :vega-lite})    ;; Interactive Vega-Lite visualization
  (draw layers {:backend :plotly})       ;; Interactive Plotly.js visualization
  (draw layers {:backend :geom-viz :width 800 :height 600})"
  ([layers]
   (draw layers {}))
  ([layers opts]
   (let [backend (or (:backend opts) :geom-viz)]
     (case backend
       :geom-viz (plot layers opts)
       :vega-lite (plot-vega layers opts)
       :vegalite (plot-vega layers opts)
       :plotly (plot-plotly layers opts)
       (plot layers opts)))))

;; # Multiple Backends & Specs as Data
;;
;; One of the key design goals is backend independence: the same layer
;; specification should work with multiple rendering targets. Let's demonstrate
;; this with all three backends, and then show that specs are just data that
;; can be manipulated programmatically.

;; ## Same Layers, Three Backends

;; Define a single layer specification:
(def example-layers
  (* (data penguins)
     (mapping :bill-length-mm :bill-depth-mm {:color :species})
     (+ (scatter {:alpha 0.6})
        (linear))))

;; ### Backend 1: thi.ng/geom-viz (Static SVG)
;;
;; Great for: Static images, PDFs, publications

(draw example-layers {:backend :geom-viz})

;; ### Backend 2: Vega-Lite (Interactive, Declarative)
;;
;; Great for: Web dashboards, exploratory analysis
;; Features: Hover tooltips, zooming, panning, responsive sizing

(draw example-layers {:backend :vega-lite})

;; ### Backend 3: Plotly.js (Interactive, Imperative)
;;
;; Great for: Complex interactions, custom behaviors
;; Features: Hover tooltips, zooming, panning, 3D plots, animations

(draw example-layers {:backend :plotly})

;; **Key observation**: Same layer specification, three different renderers.
;; No backend-specific code in the layers themselves.

;; ## Specs Are Just Data
;;
;; The real power comes from the fact that backend specs are just Clojure
;; data structures. You can inspect them, transform them, and enhance them
;; using standard Clojure functions and your knowledge of the target library.

;; ### Example: Inspecting a Vega-Lite Spec

;; Let's see what a Vega-Lite spec actually looks like:
(def vega-spec (layers->vega-spec example-layers 600 400))

;; It's just a map!
vega-spec

;; The structure follows Vega-Lite's JSON schema:
;; - :data - The data points
;; - :layer - Array of mark specifications
;; - :width, :height - Dimensions

;; ### Example: Inspecting a Plotly Spec

;; Similarly, Plotly specs are just data:
(def plotly-spec (layers->plotly-spec example-layers 600 400))

plotly-spec

;; The structure follows Plotly.js conventions:
;; - :data - Array of traces (one per series)
;; - :layout - Global layout configuration

;; ### Example: Programmatic Enhancement with Plotly Knowledge
;;
;; Because specs are data, you can use Plotly-specific features by
;; directly manipulating the spec. This is useful when you need features
;; not exposed by the high-level API.

;; Let's add custom hover text to a Plotly spec:

;; First, create a basic scatter plot
(def basic-scatter
  (* (data penguins)
     (mapping :bill-length-mm :bill-depth-mm {:color :species})
     (scatter {:alpha 0.7})))

;; Get the Plotly spec
(def basic-plotly-spec (layers->plotly-spec basic-scatter 600 400))

;; Now enhance it with custom hover templates using Plotly.js knowledge
(def enhanced-plotly-spec
  (update basic-plotly-spec :data
          (fn [traces]
            (mapv (fn [trace]
                    (assoc trace
                           :hovertemplate
                           "<b>%{fullData.name}</b><br>Bill Length: %{x:.1f}mm<br>Bill Depth: %{y:.1f}mm<extra></extra>"))
                  traces))))

;; Render the enhanced spec
(kind/plotly enhanced-plotly-spec)

;; **What we did**: Used `update` and `mapv` to add `:hovertemplate` to each trace.
;; This is a Plotly.js-specific feature that our high-level API doesn't expose,
;; but because specs are data, we can add it ourselves.

;; ### Example: Custom Layout with Vega-Lite Knowledge

;; Let's customize a Vega-Lite spec with features not in the high-level API:

(def custom-vega-spec
  (-> (layers->vega-spec example-layers 600 400)
      ;; Add a custom title
      (assoc :title {:text "Palmer Penguins: Bill Dimensions by Species"
                     :fontSize 18
                     :font "Arial"})
      ;; Customize axes with Vega-Lite knowledge
      (assoc-in [:encoding :x :axis] {:title "Bill Length (mm)"
                                      :grid true
                                      :gridColor "#e0e0e0"})
      (assoc-in [:encoding :y :axis] {:title "Bill Depth (mm)"
                                      :grid true
                                      :gridColor "#e0e0e0"})
      ;; Add selection for interactive filtering
      (assoc :selection {:species_highlight
                         {:type "multi"
                          :fields ["species"]
                          :bind "legend"}})
      ;; Make opacity conditional on selection
      (assoc-in [:layer 0 :encoding :opacity]
                {:condition {:selection "species_highlight"
                             :value 0.7}
                 :value 0.1})))

(kind/vega-lite custom-vega-spec)

;; **What we did**: 
;; - Added a title with custom styling
;; - Enhanced axes with gridlines and labels
;; - Added Vega-Lite's selection mechanism for interactive legend filtering
;; All using standard Clojure functions (`assoc`, `assoc-in`) and knowledge
;; of the Vega-Lite spec structure.

;; ### Example: Combining High-Level API with Low-Level Tweaks

;; The typical workflow:
;; 1. Use the high-level API to get 90% of the way there
;; 2. Inspect the generated spec
;; 3. Enhance it with target-specific features for the last 10%

;; Example: Add plotly-specific camera angle for 3D-like perspective effect

(def scatter-with-plotly-enhancements
  (-> (layers->plotly-spec
       (* (data mtcars)
          (mapping :wt :mpg {:color :cyl})
          (scatter {:alpha 0.7}))
       700 500)
      ;; Add custom layout features
      (assoc-in [:layout :title] {:text "Car Weight vs MPG"
                                  :font {:size 20 :family "Arial"}})
      (assoc-in [:layout :hovermode] "x unified")
      ;; Add range slider (Plotly-specific)
      (assoc-in [:layout :xaxis :rangeslider] {:visible true})
      ;; Custom legend
      (assoc-in [:layout :legend] {:x 1 :xanchor "right"
                                   :y 1 :yanchor "top"
                                   :bgcolor "rgba(255,255,255,0.8)"
                                   :bordercolor "black"
                                   :borderwidth 1})))

(kind/plotly scatter-with-plotly-enhancements)

;; **Key insight**: This is the "Clojure way" of building abstractions.
;; - The high-level API handles common cases
;; - Specs are transparent data structures
;; - You can drop down to lower levels when needed
;; - No "escape hatches" required - it's data all the way down

;; ## Why This Matters
;;
;; Traditional plotting libraries often have:
;; - Opaque objects you can't inspect
;; - Limited extension points
;; - "Escape hatches" that break the abstraction
;;
;; With specs as data:
;; - ✅ Full transparency - inspect any spec
;; - ✅ Composability - use standard library functions
;; - ✅ Extensibility - add target-specific features
;; - ✅ Debuggability - specs are readable data structures
;; - ✅ Portability - serialize specs to EDN/JSON
;;
;; This aligns perfectly with Clojure's philosophy: data > functions > macros.

;; # Trade-offs & Design Decisions
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
;; - Composition operators separate from IR (layer maps)
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
;; - Users must learn non-standard interpretation of familiar operators
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
;; **Composition Operators (`*` and `+`)**
;; - Directly translates AlgebraOfGraphics.jl concepts
;; - Distributive property enables factoring common properties
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

;; # Integration Path
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
;; ;; - aog: Compositional API, backend flexibility
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

;; # Decision Points
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
;; - Keep `*` and `+` for conciseness and mathematical elegance
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
;; This notebook demonstrates a complete composable graphics API
;; for Clojure in ~600 lines of code.
;;
;; **Core Design**:
;; - Layers as **flat maps** with `:aog/*` namespaced keys
;; - Composition using `*` (merge) and `+` (overlay)
;; - Standard library operations work natively (merge, assoc, mapv, filter)
;; - Backend-agnostic IR enables multiple rendering targets
;;
;; **Key Insights**:
;; 1. **Flat structure enables standard merge** - No custom layer-merge needed
;; 2. **Namespacing prevents collisions** - Can coexist with any data columns
;; 3. **Transparent IR** - Layers are inspectable plain maps, not templates
;; 4. **Julia's compositional approach translates to Clojure data** - Same concepts, different substrate
;;
;; **Implementation Status**:
;; - ✅ Core composition (`*`, `+`, layer composition)
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
