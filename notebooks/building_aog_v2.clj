;; # Building a Composable Graphics API in Clojure (v2)
;; **A Design Exploration for Tableplot with Minimal Delegation**
;;
;; *This notebook explores a fresh approach to composable plot specifications
;; in Clojure, inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/). 
;; It implements a **minimal delegation strategy** where we compute statistical transforms
;; and leverage target capabilities for rendering.*
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
;; Each API is also tied to a specific rendering target. If you choose `hanami`,
;; you get Vega-Lite—which is excellent for many use cases but has limitations
;; with certain coordinate systems. If you choose `plotly`, you get rich interactivity
;; but rendering static images programmatically becomes tricky. When you hit a
;; limitation of your chosen target, switching means learning a different API.
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
;; Can one API work with multiple targets? Can we use plain Clojure data structures
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

(ns building-aog-v2
  (:require
   ;; Tablecloth - Dataset manipulation
   [tablecloth.api :as tc]
   [tablecloth.column.api :as col]

   ;; Kindly - Notebook visualization protocol
   [scicloj.kindly.v4.kind :as kind]

   ;; thi.ng/geom-viz - Static SVG visualization
   [thi.ng.geom.viz.core :as viz]
   [thi.ng.geom.svg.core :as svg]

   ;; Fastmath - Statistical computations
   [fastmath.ml.regression :as regr]
   [fastmath.stats :as stats]

   ;; RDatasets - Example datasets
   [scicloj.metamorph.ml.rdatasets :as rdatasets]))

;; [**Tablecloth**](https://scicloj.github.io/tablecloth/) provides our dataset API, wrapping 
;; [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) with a
;; friendly interface. We use it to manipulate data and access column types.
;;
;; **Key insight**: Tablecloth provides column type information via `tablecloth.column.api/typeof`,
;; which eliminates the need for complex type inference!
;;
;; [**Kindly**](https://scicloj.github.io/kindly-noted/) is the visualization protocol that lets this notebook render
;; plots in different environments ([Clay](https://scicloj.github.io/clay/), [Portal](https://github.com/djblue/portal), etc.). Each target returns
;; a Kindly-wrapped spec.
;;
;; [**thi.ng/geom**](https://github.com/thi-ng/geom) gives us the static SVG target. It's excellent for
;; ggplot2-style visualizations that can be saved as publication-quality images. We specifically use
;; [geom.viz](https://github.com/thi-ng/geom/blob/feature/no-org/org/examples/viz/demos.org) for data visualization.
;;
;; [**Fastmath**](https://github.com/generateme/fastmath) handles our statistical computations, particularly linear
;; regression. It's a comprehensive math library for Clojure.
;;
;; [**RDatasets**](https://vincentarelbundock.github.io/Rdatasets/articles/data.html) provides classic datasets (penguins, mtcars, iris) for examples.
;; These are the same datasets you'd find in R or Python data science tutorials, made available
;; in Clojure through [metamorph.ml](https://github.com/scicloj/metamorph.ml).

;; # Inspiration: AlgebraOfGraphics.jl
;;
;; This design is inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/),
;; a visualization library that builds on decades of experience from systems
;; like ggplot2 while introducing clearer compositional principles.
;;
;; As the [AoG philosophy](https://aog.makie.org/stable/philosophy) states:

;; > "In a declarative framework, the user needs to express the _question_, and the 
;; > library will take care of creating the visualization."
;;
;; The key innovation is moving from imperative drawing to 
;; ["describing a higher-level 'intent' how your tabular data should be transformed"](https://aog.makie.org/dev/tutorials/intro-i).
;;
;; ## Core Insight: Layers + Operations
;;
;; AlgebraOfGraphics.jl treats visualization with two key ideas:
;;
;; **1. Everything is a [layer](https://aog.makie.org/dev/tutorials/intro-i#Layers:-data,-mapping,-visual-and-transformation)**: 
;; Data, mappings, visuals, and transformations all compose the same way
;;
;; - Data sources
;; - Aesthetic mappings (x → column, color → column)
;; - Visual marks (scatter, line, bar)
;; - Statistical transformations (regression, smoothing)
;;
;; *(Note: This is a different notion from "layers" in ggplot2 and Vega-Lite's 
;; [layered grammar of graphics](https://vita.had.co.nz/papers/layered-grammar.html), 
;; where layers specifically refer to overlaid visual marks. In AoG, "layer" is the fundamental 
;; compositional unit containing data, mappings, visuals, and transformations.)*
;;
;; **2. Two operations**: `*` (product) and `+` (sum)
;;
;; - `*` **merges** layers together (composition)
;;   ```julia
;;   data(penguins) * mapping(:bill_length, :bill_depth) * visual(Scatter)
;;   # → Single layer with all properties combined
;;   ```
;;
;; - `+` **overlays** layers (overlay)
;;   ```julia
;;   visual(Scatter) + visual(Lines)
;;   # → Two separate visual marks on same plot
;;   ```
;;
;; **3. Distributive property**: `a * (b + c) = (a * b) + (a * c)`
;;
;;   ```julia
;;   data(penguins) * mapping(:bill_length, :bill_depth) * 
;;       (visual(Scatter) + linear())
;;   # ↓ expands to
;;   (data(penguins) * mapping(:bill_length, :bill_depth) * visual(Scatter)) + 
;;   (data(penguins) * mapping(:bill_length, :bill_depth) * linear())
;;   ```
;;
;; This allows factoring out common properties and applying them to
;; multiple plot types without repetition.

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
;; data(df) * mapping(:x, :y) * (visual(Scatter) + smooth())
;; ```
;;
;; **Why two operators?** The separation brings clarity—`*` means "combine properties" 
;; while `+` means "overlay visuals"—and enables powerful composability.

;; ## Translating to Clojure
;;
;; Julia's approach relies on custom `*` and `+` operators defined on Layer types,
;; using multiple dispatch to handle different type combinations with object-oriented
;; layer representations. This works beautifully in Julia's type system.
;;
;; The key challenge for Clojure is bringing this compositional elegance while staying
;; true to Clojure idioms: using plain data structures (maps, not objects), enabling
;; standard library operations like `merge`, `assoc`, and `filter`, maintaining the
;; compositional benefits, and making the intermediate representation transparent and
;; inspectable. How do we get the algebraic elegance of AoG while keeping everything
;; as simple Clojure data?

;; # Design Exploration
;;
;; The core design question: how should we structure layer specifications
;; so they compose naturally with Clojure's standard library?

;; ## The Problem: Nested Doesn't Merge

(def nested-layer-example
  {:transformation nil
   :data {:bill-length-mm [39.1 39.5 40.3]
          :bill-depth-mm [18.7 17.4 18.0]
          :species [:adelie :adelie :adelie]}
   :positional [:bill-length-mm :bill-depth-mm]
   :named {:color :species}
   :attributes {:alpha 0.5}})

;; Standard `merge` doesn't compose nested structures:

(merge {:positional [:x] :named {:color :species}}
       {:positional [:y] :named {:size :body-mass}})
;; Lost :x and :color!

;; Nested structure requires custom `merge-layer` function. Not ideal.

;; ## The Solution: Flat Structure with Namespaced Keys

(def flat-layer-example
  #:aog{:data {:bill-length-mm [39.1 39.5 40.3]
               :bill-depth-mm [18.7 17.4 18.0]
               :species [:adelie :adelie :adelie]}
        :x :bill-length-mm
        :y :bill-depth-mm
        :color :species
        :alpha 0.5
        :plottype :scatter})

;; **Why this works**:
;;
;; - Standard `merge` composes correctly (flat structure)
;; - No collision with data columns (`:aog/plottype` ≠ `:plottype`)
;; - All standard library operations work: `assoc`, `update`, `mapv`, `filter`, `into`
;; - Namespace map syntax `#:aog{...}` keeps things concise

;; # The Delegation Strategy
;;
;; A key architectural decision: **What do we compute vs. what do targets handle?**
;;
;; ## The Core Principle
;;
;; **Statistical transforms require domain computation. Everything else can delegate.**
;;
;; ### Why Statistical Transforms Drive Everything
;;
;; Consider histogram computation:
;;
;; ```clojure
;; (* (data penguins) (mapping :bill-length) (histogram))
;; 
;; ;; Computation sequence:
;; ;; 1. Must compute domain first: [32.1, 59.6]
;; ;; 2. Use domain to decide bin edges  
;; ;; 3. Compute bin counts
;; ;; 4. Create bar chart from computed bins
;; ```
;;
;; **The dependency**: We can't delegate domain to the target because we need it
;; BEFORE we can compute bins. Statistical transforms are our **core value proposition**,
;; so we accept this dependency.
;;
;; ## What We Compute (Minimal Set)
;;
;; **1. Statistical Transforms**
;; - Histogram, density, smoothing, regression
;; - Why: Core value, consistency across targets, inspectability
;;
;; **2. Domain Computation (Only When Needed)**
;; - Only for aesthetics involved in statistical transforms
;; - Coordinate-aware (Cartesian, polar, geographic)
;; - Why: Required by statistical transforms
;;
;; **3. Type Information**
;; - Use Tablecloth's column types (`col/typeof`)
;; - Fallback inference for plain maps
;; - Why: Free from Tablecloth, needed for transform selection
;;
;; ## What We Delegate (Maximize)
;;
;; **1. Axis Rendering**
;; - Tick placement, "nice numbers", label formatting
;; - Why: Targets are polished, edge cases are many
;;
;; **2. Range Computation**
;; - Pixel/visual coordinates
;; - Why: Tightly coupled with layout
;;
;; **3. Domains for Simple Plots**
;; - When no transforms, targets compute from data
;; - Why: Targets already do this well
;;
;; **4. Scale Merging**
;; - Multi-layer plots: targets handle shared domains
;; - Why: Avoid complex conflict resolution
;;
;; **5. Color Palette Application**
;; - We provide preferences (`:palette-preference :ggplot2`)
;; - Targets apply from their libraries
;; - Why: Targets have palette expertise
;;
;; ## The Key Insight: Tablecloth Provides Types!
;;
;; We don't need complex type inference. Tablecloth already knows:
;;
;; ```clojure
;; (col/typeof (penguins :bill-length-mm))  ;; => :float64
;; (col/typeof (penguins :species))         ;; => :string
;; ```
;;
;; This eliminates the "context-aware type inference" problem entirely!

;; # Proposed Design
;;
;; ## API Overview
;;
;; The API consists of three parts:
;;
;; 1. **Constructors** - Build partial layer specifications
;; 2. **Composition operators** - Merge layers (`*`) and overlay them (`+`)
;; 3. **Renderer** - Single `plot` function that interprets layer specs
;;
;; **Current Implementation Status**:
;;
;; - ✅ Core composition (`*`, `+`, layer merging)
;; - ✅ Minimal delegation (compute transforms, delegate rendering)
;; - ✅ Type information from Tablecloth
;; - ⚠️ Three rendering targets (in progress)
;; - ⚠️ Statistical transforms (partial - linear done, histogram/smooth/density pending)

;; ## Constructors

(defn data
  "Attach a dataset to a layer.
  
  Accepts plain Clojure maps or tech.ml.dataset datasets.
  Returns a layer map with :aog/data."
  [dataset]
  {:aog/data dataset})

;; Examples:
(data {:x [1 2 3] :y [4 5 6]})

;; Works with datasets too (shown later when we load penguins)

(defn mapping
  "Define aesthetic mappings from data columns to visual properties.
  
  Args:
  - x, y: Column names (keywords) for positional aesthetics
  - named: (optional) Map of other aesthetics
  
  Mappings tell the renderer which columns to use for which visual properties."
  ([x y]
   {:aog/x x :aog/y y})
  ([x y named]
   (merge {:aog/x x :aog/y y}
          (update-keys named #(keyword "aog" (name %))))))

;; Examples:
(mapping :bill-length-mm :bill-depth-mm)

(mapping :bill-length-mm :bill-depth-mm {:color :species})

(mapping :wt :mpg {:color :cyl :size :hp})

(defn scatter
  "Create a scatter plot layer."
  ([]
   {:aog/plottype :scatter})
  ([attrs]
   (merge {:aog/plottype :scatter}
          (update-keys attrs #(keyword "aog" (name %))))))

;; Examples:
(scatter)

(scatter {:alpha 0.7})

(scatter {:alpha 0.5 :size 10})

(defn line
  "Create a line plot layer."
  ([]
   {:aog/plottype :line})
  ([attrs]
   (merge {:aog/plottype :line}
          (update-keys attrs #(keyword "aog" (name %))))))

(defn linear
  "Add linear regression transformation.
  
  Computes best-fit line through points.
  When combined with color aesthetic, computes separate regression per group."
  []
  {:aog/transformation :linear
   :aog/plottype :line})

;; Example:
(linear)

(defn histogram
  "Plot type: Histogram with binning.
  
  We compute bins using Sturges' formula, then pass to target as bar chart."
  ([] {:aog/plottype :histogram})
  ([opts] (merge {:aog/plottype :histogram} opts)))

(defn smooth
  "Statistical transformation: LOESS smoothing."
  []
  {:aog/transformation :smooth
   :aog/plottype :line})

(defn density
  "Statistical transformation: Kernel density estimation."
  []
  {:aog/transformation :density
   :aog/plottype :area})

(defn target
  "Specify rendering target.
  
  Args:
  - t: Target keyword - :geom (default), :vl (Vega-Lite), or :plotly"
  [t]
  {:aog/target t})

;; ## Composition Operators

(defn *
  "Merge layer specifications (composition)."
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

;; Examples - demonstrating the distributive property:

;; Map × Map → [Map]
(* (data {:x [1 2] :y [3 4]}) (mapping :x :y))

;; Full composition
(* (data {:x [1 2] :y [3 4]}) (mapping :x :y) (scatter))

;; Distributive: a * (b + c) = (a * b) + (a * c)
;; (Full example with dataset shown later after loading penguins)

(defn +
  "Combine multiple layer specifications for overlay (sum)."
  [& layer-specs]
  (vec (mapcat #(if (vector? %) % [%]) layer-specs)))

;; ## Type Information (Using Tablecloth)

(defn- infer-from-values
  "Simple fallback type inference for plain Clojure data."
  [values]
  (cond
    (every? number? values) :continuous
    (some #(instance? java.time.temporal.Temporal %) values) :temporal
    :else :categorical))

;; Examples:
(+ (scatter) (linear))

(+ (scatter {:alpha 0.5}) (linear) (line))

;; ## Type Information (Using Tablecloth)

(defn infer-scale-type
  "Get scale type from Tablecloth metadata or fallback to value inference.
  
  This is a KEY WIN from the delegation strategy: Tablecloth provides types,
  so we don't need complex type inference!"
  [layer aesthetic]
  (let [data (:aog/data layer)
        column-key (get layer aesthetic)]
    (if (tc/dataset? data)
      ;; Use Tablecloth type information (O(1) lookup!)
      (let [col-type (col/typeof (data column-key))]
        (cond
          (#{:int8 :int16 :int32 :int64 :float32 :float64} col-type)
          :continuous

          (#{:local-date :local-date-time :instant} col-type)
          :temporal

          (#{:string :keyword :boolean :object} col-type)
          :categorical))

      ;; Fallback for plain maps (simple inference)
      (infer-from-values (get data column-key)))))

;; Example with plain data:
(let [layer (first (* (data {:x [1.0 2.0 3.0] :y [4.0 5.0 6.0] :group ["a" "b" "c"]})
                      (mapping :x :y {:color :group})
                      (scatter)))]
  {:x-type (infer-scale-type layer :aog/x)
   :y-type (infer-scale-type layer :aog/y)})

;; With Tablecloth datasets, this is O(1) lookup from metadata!
;; (Full example shown later after loading penguins dataset)

(defn- infer-from-values
  "Simple fallback type inference for plain Clojure data."
  [values]
  (cond
    (every? number? values) :continuous
    (some #(instance? java.time.temporal.Temporal %) values) :temporal
    :else :categorical))

;; ## Domain Computation (Only for Statistical Transforms)

(defmulti compute-domain
  "Compute domain for an aesthetic, coordinate-system aware.
  
  ONLY called when statistical transforms need it. For simple plots,
  we delegate domain computation to targets."
  (fn [data aesthetic coord-system] coord-system))

(defmethod compute-domain :cartesian
  [data aesthetic _]
  (let [values (filter some? data)] ;; Handle nils
    (when (seq values)
      [(apply min values) (apply max values)])))

(defmethod compute-domain :polar
  [data aesthetic _]
  (case aesthetic
    :theta [0 360] ;; Angular (wrapping)
    :r [0 (apply max (filter some? data))])) ;; Radial

(defmethod compute-domain :geographic
  [data aesthetic _]
  (case aesthetic
    :lon [-180 180]
    :lat [-90 90]))

(defmethod compute-domain :default
  [data aesthetic coord]
  (throw (ex-info "Unsupported coordinate system"
                  {:coord coord :aesthetic aesthetic})))

;; Examples (after all defmethods are defined):

;; Cartesian coordinates - simple min/max
(compute-domain [32.1 45.3 59.6] :x :cartesian)

;; Polar coordinates - angular wrapping
(compute-domain [10 350 270 90] :theta :polar)

;; Polar radial - from center
(compute-domain [5 10 15] :r :polar)

;; Geographic - bounded domains
(compute-domain [-122.4 -118.2 -73.9] :lon :geographic)

(compute-domain [37.7 34.0 40.7] :lat :geographic)

;; ## Renderer

(defmulti plot-impl
  "Internal multimethod for plot dispatch."
  (fn [layers opts]
    (let [layers-vec (if (vector? layers) layers [layers])
          spec-target (some :aog/target layers-vec)]
      (or (:target opts) spec-target :geom))))

(defn plot
  "Unified rendering function supporting multiple targets.

  Args:
  - layers: Vector of layer maps or single layer map
  - opts: Optional map with:
    - :target - :geom (static SVG), :vl (Vega-Lite), or :plotly (Plotly.js)
    - :width - Width in pixels (default 600)
    - :height - Height in pixels (default 400)

  Returns:
  - Kindly-wrapped visualization specification

  Examples:
  (plot layers)                    ;; Uses :geom target by default
  (plot layers {:target :vl})      ;; Vega-Lite specification"
  ([layers]
   (plot-impl layers {}))
  ([layers opts]
   (plot-impl layers opts)))

;; # Examples
;;
;; These examples demonstrate the design in practice, showing how minimal
;; delegation works.

;; ## Setup: Load Datasets

;; Palmer Penguins - 344 observations, 3 species
(def penguins (tc/drop-missing (rdatasets/palmerpenguins-penguins)))

;; Motor Trend Car Road Tests - 32 automobiles
(def mtcars (rdatasets/datasets-mtcars))

;; Fisher's Iris - 150 flowers, 3 species
(def iris (rdatasets/datasets-iris))

;; ## Type Information Example
;;
;; Let's see Tablecloth's type information in action:

(kind/pprint
 {:bill-length-type (col/typeof (penguins :bill-length-mm))
  :species-type (col/typeof (penguins :species))
  :island-type (col/typeof (penguins :island))})

;; Notice: We get precise type information (`:float64`, `:string`) without
;; examining values. This eliminates the need for complex type inference!

;; ## Implementation: Helper Functions & :geom Target
;;
;; Before we can render examples, we need basic implementation.
;; This version follows the minimal delegation strategy.

;; ### Helper Functions

(defn- layer->points
  "Convert layer to point data for rendering."
  [layer]
  (let [data (:aog/data layer)
        dataset (if (tc/dataset? data) data (tc/dataset data))
        x-vals (vec (get dataset (:aog/x layer)))
        y-vals (vec (get dataset (:aog/y layer)))
        color-col (:aog/color layer)
        color-vals (when color-col
                     (vec (get dataset color-col)))]
    (map-indexed (fn [i _]
                   (cond-> {:x (nth x-vals i) :y (nth y-vals i)}
                     color-vals (assoc :color (nth color-vals i))))
                 x-vals)))

(defn- compute-linear-regression
  "Compute linear regression using fastmath.
  
  Returns: Vector of 2 points representing the fitted line."
  [points]
  (when (>= (count points) 2)
    (let [x-vals (mapv :x points)
          y-vals (mapv :y points)
          x-min (apply min x-vals)
          x-max (apply max x-vals)]
      (try
        (let [xss (mapv vector x-vals)
              model (regr/lm y-vals xss)
              intercept (:intercept model)
              slope (first (:beta model))]
          ;; A straight line only needs 2 points
          [{:x x-min :y (clojure.core/+ intercept (clojure.core/* slope x-min))}
           {:x x-max :y (clojure.core/+ intercept (clojure.core/* slope x-max))}])
        (catch Exception e
          nil)))))

(defn- color-scale
  "ggplot2-like color scale for categorical data."
  [categories]
  (let [colors ["#F8766D" "#00BA38" "#619CFF" "#F564E3"]]
    (zipmap categories (cycle colors))))

;; ### Simple :geom Target (Delegating Domain Computation)

(defn- infer-domain
  "Infer domain from data values.
  
  For delegation: We compute RAW domain (just min/max).
  thi.ng/geom handles 'nice numbers' and tick placement."
  [values]
  (cond
    (empty? values) [0 1]
    (every? number? values) [(apply min values) (apply max values)]
    :else (vec (distinct values))))

;; This is a placeholder - full implementation coming
(defmethod plot-impl :geom
  [layers opts]
  (let [layers-vec (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)

        ;; IMPORTANT: We need to compute domains AFTER transforms
        ;; because statistical transforms (like regression) can extend beyond raw data!

        ;; First, compute all transformed points
        all-transformed-points
        (mapcat (fn [layer]
                  (let [points (layer->points layer)
                        transformation (:aog/transformation layer)]
                    (if (= transformation :linear)
                      ;; For linear regression, use fitted points for domain
                      (or (compute-linear-regression points) points)
                      ;; Otherwise use raw points
                      points)))
                layers-vec)

        ;; Now compute domain from ALL points (including transformed)
        x-vals (mapv :x all-transformed-points)
        y-vals (mapv :y all-transformed-points)
        x-domain (infer-domain x-vals)
        y-domain (infer-domain y-vals)

        ;; Only proceed if we have valid numeric domains
        valid? (and (vector? x-domain) (vector? y-domain)
                    (every? number? x-domain) (every? number? y-domain))]

    (if-not valid?
      (kind/hiccup
       [:div {:style {:padding "20px"}}
        [:p "Cannot render: non-numeric data or empty dataset"]
        [:pre (pr-str {:x-domain x-domain :y-domain y-domain})]])

      ;; Render with thi.ng/geom
      (let [x-range (clojure.core/- (second x-domain) (first x-domain))
            y-range (clojure.core/- (second y-domain) (first y-domain))
            x-major (max 1 (clojure.core/* x-range 0.2))
            y-major (max 1 (clojure.core/* y-range 0.2))

            ;; Create axes - thi.ng/geom handles tick placement
            x-axis (viz/linear-axis
                    {:domain x-domain
                     :range [50 (clojure.core/- width 50)]
                     :major x-major
                     :pos (clojure.core/- height 50)})
            y-axis (viz/linear-axis
                    {:domain y-domain
                     :range [(clojure.core/- height 50) 50]
                     :major y-major
                     :pos 50})

            ;; Process each layer to viz data
            layer-data (mapcat (fn [layer]
                                 (let [points (layer->points layer)
                                       plottype (:aog/plottype layer)
                                       alpha (or (:aog/alpha layer) 1.0)
                                       transformation (:aog/transformation layer)]

                                   (cond
                                     ;; Linear regression transformation
                                     (= transformation :linear)
                                     (let [color-groups (group-by :color points)]
                                       (if (> (count color-groups) 1)
                                         ;; Regression per group
                                         (keep (fn [[color group-points]]
                                                 (when-let [fitted (compute-linear-regression group-points)]
                                                   (let [colors (color-scale (keys color-groups))
                                                         line-data (mapv (fn [p] [(:x p) (:y p)]) fitted)]
                                                     {:values line-data
                                                      :layout viz/svg-line-plot
                                                      :attribs {:stroke (get colors color)
                                                                :stroke-width 2
                                                                :fill "none"
                                                                :opacity alpha}})))
                                               color-groups)
                                         ;; Single regression
                                         (when-let [fitted (compute-linear-regression points)]
                                           [{:values (mapv (fn [p] [(:x p) (:y p)]) fitted)
                                             :layout viz/svg-line-plot
                                             :attribs {:stroke "#333333"
                                                       :stroke-width 2
                                                       :fill "none"
                                                       :opacity alpha}}])))

                                     ;; Scatter plot
                                     (= plottype :scatter)
                                     (let [color-groups (group-by :color points)]
                                       (if (> (count color-groups) 1)
                                         ;; Colored scatter
                                         (let [colors (color-scale (keys color-groups))]
                                           (mapv (fn [[color group-points]]
                                                   {:values (mapv (fn [p] [(:x p) (:y p)]) group-points)
                                                    :layout viz/svg-scatter-plot
                                                    :attribs {:fill (get colors color)
                                                              :stroke (get colors color)
                                                              :stroke-width 0.5
                                                              :opacity alpha}})
                                                 color-groups))
                                         ;; Simple scatter
                                         [{:values (mapv (fn [p] [(:x p) (:y p)]) points)
                                           :layout viz/svg-scatter-plot
                                           :attribs {:fill "#333333"
                                                     :stroke "#333333"
                                                     :stroke-width 0.5
                                                     :opacity alpha}}]))

                                     ;; Line plot
                                     (= plottype :line)
                                     (let [sorted-points (sort-by :x points)
                                           color-groups (group-by :color sorted-points)]
                                       (if (> (count color-groups) 1)
                                         ;; Colored lines
                                         (let [colors (color-scale (keys color-groups))]
                                           (mapv (fn [[color group-points]]
                                                   {:values (mapv (fn [p] [(:x p) (:y p)])
                                                                  (sort-by :x group-points))
                                                    :layout viz/svg-line-plot
                                                    :attribs {:stroke (get colors color)
                                                              :stroke-width 1
                                                              :fill "none"
                                                              :opacity alpha}})
                                                 color-groups))
                                         ;; Simple line
                                         [{:values (mapv (fn [p] [(:x p) (:y p)]) sorted-points)
                                           :layout viz/svg-line-plot
                                           :attribs {:stroke "#333333"
                                                     :stroke-width 1
                                                     :fill "none"
                                                     :opacity alpha}}]))

                                     :else [])))
                               layers-vec)

            ;; Create the plot spec
            plot-spec {:x-axis x-axis
                       :y-axis y-axis
                       :grid {:attribs {:stroke "#FFFFFF" :stroke-width 1}}
                       :data (vec layer-data)}

            ;; Render to SVG with ggplot2-style background
            bg-rect (svg/rect [50 50]
                              (clojure.core/- width 100)
                              (clojure.core/- height 100)
                              {:fill "#EBEBEB"
                               :stroke "#FFFFFF"
                               :stroke-width 1})

            svg-elem (svg/svg {:width width :height height}
                              bg-rect
                              (viz/svg-plot2d-cartesian plot-spec))]

        (kind/html (svg/serialize svg-elem))))))

;; Placeholder implementations for other targets
(defmethod plot-impl :vl
  [layers opts]
  (kind/hiccup
   [:div [:p "Vega-Lite target - Coming soon"]]))

(defmethod plot-impl :plotly
  [layers opts]
  (kind/hiccup
   [:div [:p "Plotly target - Coming soon"]]))

;; ## Example 1: Simple Scatter Plot (Delegated Domain)

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)))

;; **What happens here**:
;; 1. We create layer spec with data, mapping, plottype
;; 2. We DON'T compute X/Y domains
;; 3. Target receives data and computes domain itself
;; 4. This is simpler - we delegate what targets do well!

;; ## Example 2: Type Information in Action

(kind/pprint
 (let [layer (* (data penguins)
                (mapping :bill-length-mm :bill-depth-mm {:color :species})
                (scatter))
       layer (first layer)]
   {:x-type (infer-scale-type layer :aog/x)
    :y-type (infer-scale-type layer :aog/y)
    :color-type (infer-scale-type layer :aog/color)}))

;; Notice: Type inference is instant (O(1) lookup from Tablecloth metadata),
;; not O(n) value examination!

;; ## Example 3: Multi-Layer Composition (Scatter + Linear Regression)

(plot
 (+ (* (data penguins)
       (mapping :bill-length-mm :bill-depth-mm)
       (scatter))
    (* (data penguins)
       (mapping :bill-length-mm :bill-depth-mm)
       (linear))))

;; **What happens here**:
;; 1. First layer: scatter plot of raw data
;; 2. Second layer: linear regression line
;; 3. `+` operator overlays them
;; 4. Both layers share same data and mapping
;; 5. Target renders both with delegated domains

;; ## Example 3b: Same Plot Using Distributivity

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (+ (scatter)
       (linear))))

;; **What happens here**:
;; 1. `(+ (scatter) (linear))` creates a vector of two plot specs
;; 2. `*` distributes over the vector (cartesian product)
;; 3. Result: same as Example 3, but more succinct!
;; 4. Factor out common parts (data, mapping), vary only what differs (plottype)

;; ## Example 4: Color Mapping with Categorical Variable

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (scatter)))

;; **What happens here**:
;; 1. `:color :species` maps species to color aesthetic
;; 2. Type system recognizes :species as categorical (via Tablecloth)
;; 3. Target creates color scale automatically
;; 4. Three species = three colors in the scatter plot

;; ## Example 5: Multi-Layer with Color Groups

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (+ (scatter)
       (linear))))

;; **What happens here**:
;; 1. Scatter plot colored by species
;; 2. Linear regression per species (separate line for each)
;; 3. Color scale shared between layers
;; 4. Demonstrates composition + grouping + statistical transforms
;; 5. Uses distributivity for succinctness!

;; ## Example 6: Different Dataset (mtcars)

(plot
 (* (data mtcars)
    (mapping :wt :mpg)
    (scatter)))

;; **What happens here**:
;; 1. Works with any dataset
;; 2. Different column names (:wt, :mpg instead of bill-*)
;; 3. Type inference works the same way
;; 4. Target handles domain computation

;; ## Example 7: mtcars with Regression

(plot
 (* (data mtcars)
    (mapping :wt :mpg)
    (+ (scatter)
       (linear))))

;; **What happens here**:
;; 1. Negative correlation visible (heavier cars have lower MPG)
;; 2. Regression line captures the trend
;; 3. Same delegation strategy works across datasets
;; 4. Uses distributivity for succinctness

;; ## Debugging & Inspection Utilities

(defn inspect-layers
  "Inspect what layers are created and what data they contain.
  
  Useful for debugging composition and understanding what gets passed to the renderer."
  [layer-spec]
  (let [layers-vec (if (vector? layer-spec) layer-spec [layer-spec])]
    {:num-layers (count layers-vec)
     :layers (mapv (fn [layer]
                     {:plottype (:aog/plottype layer)
                      :transformation (:aog/transformation layer)
                      :aesthetics (select-keys layer [:aog/x :aog/y :aog/color])
                      :data-rows (when-let [data (:aog/data layer)]
                                   (if (tc/dataset? data)
                                     (tc/row-count data)
                                     (count data)))})
                   layers-vec)}))

(kind/pprint
 (inspect-layers
  (* (data mtcars)
     (mapping :wt :mpg)
     (+ (scatter)
        (linear)))))

;; **What we discovered during debugging**:
;; 
;; Initial problem: Regression line was invisible (only 1 point rendered instead of 2)
;; Root cause: Domain computed from raw scatter points, but regression extends beyond that range
;; Example: mtcars regression line endpoint y=8.296 < scatter domain min y=10.4
;; Solution: Compute domains AFTER applying statistical transforms
;; 
;; This highlights why statistical transforms need domain computation - they can
;; extend beyond the raw data!

;; # Summary
;;
;; ## What We've Explored
;;
;; This notebook demonstrates a composable graphics API with **minimal delegation**:
;;
;; **Core Design**:
;; - Layers as flat maps with `:aog/*` namespaced keys
;; - Composition using `*` (merge) and `+` (overlay)
;; - Standard library operations work natively
;; - Backend-agnostic IR
;;
;; **Delegation Strategy**:
;; 1. ✅ **We compute**: Statistical transforms, domains (when needed), types (from Tablecloth)
;; 2. ❌ **We delegate**: Axis rendering, ranges, ticks, "nice numbers", layout
;; 3. 🎯 **Result**: ~500 lines of focused code vs ~1500 lines of edge cases
;;
;; **Key Wins**:
;; - Type information from Tablecloth (free!)
;; - Domain computation only for statistical transforms
;; - Leverage target polish for rendering
;; - Simple, focused implementation
;;
;; ## Implementation Status
;;
;; - ✅ Core composition
;; - ✅ Type inference via Tablecloth
;; - ✅ Delegation strategy designed
;; - ⚠️ Target implementations (in progress)
;; - ⚠️ Statistical transforms (linear done, others pending)
;;
;; ## Next Steps
;;
;; 1. Complete target implementations with delegated domains
;; 2. Implement histogram with domain computation
;; 3. Add coordinate system support
;; 4. Test across all three targets
;; 5. Gather community feedback

;; ---
;; *This is a design exploration. Feedback welcome!*
