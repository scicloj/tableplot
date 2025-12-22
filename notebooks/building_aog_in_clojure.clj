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

(ns building-aog-in-clojure
  (:require
   ;; Tablecloth - Dataset manipulation
   [tablecloth.api :as tc]

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
;; multiple plot types without repetition. As the [philosophy doc](https://aog.makie.org/stable/philosophy) 
;; explains, the package achieves its goal of "distinguish[ing] settings that are private to a layer from 
;; those that are shared across layers" using "the distributive properties of addition and multiplication."
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
;; data(df) * mapping(:x, :y) * (visual(Scatter) + smooth())
;; ```
;;
;; **Why two operators?** The separation brings clarity—`*` means "combine properties" 
;; while `+` means "overlay visuals"—and enables powerful composability, since expressions 
;; like `data(df) * mapping(:x, :y)` can be reused across different plot types. Most 
;; importantly, it follows the distributive law naturally, allowing algebraic manipulation 
;; of visualization specifications.
;;
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
;;
;; The next section explores different approaches to solving this challenge.

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
;; => {:positional [:y], :named {:size :body-mass}}
;; Lost :x and :color!

;; Nested structure requires custom `merge-layer` function. Not ideal.

;; ## The Solution: Flat Structure with Namespaced Keys
;;
;; Use flat maps with namespaced keywords to enable standard `merge` while
;; preventing collisions with data column names:

(def flat-layer-example-v2
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
;;
;; **Trade-off**: Slightly more verbose than plain keys (`:aog/color` vs `:color`).
;;
;; *Note: An alternative approach using `:=*` prefix convention (like `:=color`) was
;; considered for brevity, similar to Tableplot's current APIs. We may revisit based
;; on usage patterns and community feedback.*

;; ## Why This Matters: Julia's Compositional Approach → Clojure Data Structures
;;
;; Julia's AlgebraOfGraphics.jl uses custom Layer types with specialized `*` and `+`
;; operators, leveraging multiple dispatch to handle type combinations through
;; object-oriented composition. Our Clojure translation takes a different path: layers
;; are **just maps** with `:aog/*` namespaced keys, `*` and `+` are **functions** that
;; work on map collections, and composition uses **standard `merge`**, not custom logic.
;;
;; The result is the same compositional power, but layers are transparent data that work
;; with Clojure's entire standard library. You can use `merge` to combine layers, `assoc`
;; to add properties, `update` to modify values, `mapv` to transform all layers, `filterv`
;; to select specific layers, and `into` to accumulate layers. This is a fundamental design
;; advantage: **compositional operations on plain data**.
;;
;; As Alan Perlis wrote in his [Epigrams in Programming](https://www.cs.yale.edu/homes/perlis-alan/quotes.html):

;; > "It is better to have 100 functions operate on one data structure than 10 functions 
;; > on 10 data structures."
;;
;; That's exactly what we get here—the entire Clojure standard library working seamlessly
;; with our visualization specifications.

;; # Proposed Design
;;
;; ## API Overview
;;
;; The API consists of three parts:
;;
;; 1. **Constructors** - Build partial layer specifications
;;    - Data: `data`
;;    - Mappings: `mapping`
;;    - Plot types: `scatter`, `line`, `bar`, `area`, etc.
;;    - Transformations: `linear`, `smooth`, etc.
;;    - Target: `target` (specify rendering target compositionally)
;; 2. **Composition operators** - Merge layers (`*`) and overlay them (`+`)
;; 3. **Renderer** - Single `plot` function that interprets layer specs
;;
;; Implementation details are in the Implementation section. Here we show signatures and usage.
;;
;; **Current Implementation Status**:
;;
;; - ✅ Core composition (`*`, `+`, layer merging with standard library)
;; - ✅ Three rendering targets (:geom, :vl, :plotly)
;; - ✅ Plot types: scatter, line, area, bar
;; - ✅ Statistical transform: linear regression
;; - ✅ Aesthetics: position (x, y), color, alpha
;; - ✅ Faceting (Vega-Lite target only)
;; - ⚠️ Statistical transforms: smooth, density, histogram (API defined, not yet implemented)
;; - ⚠️ Additional coordinate systems (polar, geographic - planned)

;; ## Constructors

(defn data
  "Attach a dataset to a layer.
  
  Accepts plain Clojure maps or tech.ml.dataset datasets.
  Returns a layer map with :aog/data."
  [dataset]
  {:aog/data dataset})

;; Example:
(data {:x [1 2 3] :y [4 5 6]})

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

(mapping :x :y {:color :group :size :value})

(defn scatter
  "Create a scatter plot layer.
  
  Args:
  - attrs: (optional) Map of visual attributes
  
  Attributes are constant values applied to all points.
  Contrast with mappings, which vary by data."
  ([]
   {:aog/plottype :scatter})
  ([attrs]
   (merge {:aog/plottype :scatter}
          (update-keys attrs #(keyword "aog" (name %))))))

;; Examples:
(scatter)

(scatter {:alpha 0.7})

(defn line
  "Create a line plot layer.
  
  Connects points in x-order with lines."
  ([]
   {:aog/plottype :line})
  ([attrs]
   (merge {:aog/plottype :line}
          (update-keys attrs #(keyword "aog" (name %))))))

;; Example:
(line)

(defn linear
  "Add linear regression transformation.
  
  Computes best-fit line through points.
  When combined with color aesthetic, computes separate regression per group."
  []
  {:aog/transformation :linear
   :aog/plottype :line})

;; Example:
(linear)

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
  ([] {:aog/plottype :area})
  ([opts] (merge {:aog/plottype :area} opts)))

(defn bar
  "Plot type: Bar chart."
  ([] {:aog/plottype :bar})
  ([opts] (merge {:aog/plottype :bar} opts)))

(defn target
  "Specify rendering target.
  
  Args:
  - t: Target keyword - :geom (default), :vl (Vega-Lite), or :plotly
  
  Examples:
  (* (data penguins) (mapping :x :y) (scatter) (target :vl))"
  [t]
  {:aog/target t})

;; ## Composition Operators

(defn *
  "Merge layer specifications (composition).
  
  The `*` operator combines layer properties through merge.
  It handles multiple input types to enable flexible composition:
  
  - Map × Map → [Map] - Merge and wrap in vector
  - Map × Vec → Vec - Merge map into each vector element  
  - Vec × Vec → Vec - Cartesian product with merge
  
  The distributive property holds: (* a (+ b c)) = (+ (* a b) (* a c))"
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

;; Examples:
(* (data {:x [1 2] :y [3 4]}) (mapping :x :y) (scatter))

(defn +
  "Combine multiple layer specifications for overlay (sum).
  
  The `+` operator creates multiple layers that will be rendered together.
  Simply concatenates layers into a vector."
  [& layer-specs]
  (vec (mapcat #(if (vector? %) % [%]) layer-specs)))

;; Examples:
(+ (scatter) (linear))

(* (data {:x [1 2] :y [3 4]}) (mapping :x :y) (+ (scatter) (linear)))

;; **Design Note**: Why `*` and `+`?
;;
;; These operators shadow Clojure's arithmetic operators, which requires using
;; `clojure.core/*` for multiplication in implementation code. The trade-off:
;;
;; - ✅ Mathematical elegance and familiarity (for Julia/AoG users)
;; - ✅ Conciseness: `*` vs `compose`, `+` vs `overlay`
;; - ✅ Algebraic properties (distributive law) are self-evident
;; - ⚠️ Novel semantics for Clojure users
;;
;; Alternatives considered: `compose`/`overlay`, `merge-layers`/`concat-layers`.
;; Feedback welcome on whether the conciseness is worth the non-standard interpretation.

;; ## Renderer
;;
;; The `plot` function is the single user-facing renderer. It dispatches to
;; target-specific implementations based on the `:aog/target` in layer specs
;; or the `:target` option (options take precedence).

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

  The target can be specified in three ways (in order of precedence):
  1. In opts: (plot layers {:target :vl})
  2. In layer spec: (* ... (target :vl))
  3. Default: :geom

  Returns:
  - Kindly-wrapped visualization specification

  Examples:
  (plot layers)                              ;; Uses :geom target by default
  (plot layers {:target :vl})                ;; Vega-Lite specification
  (plot (* (data ...) (scatter) (target :vl))) ;; Target in layer spec
  (plot layers {:target :plotly :width 800 :height 600})"
  ([layers]
   (plot-impl layers {}))
  ([layers opts]
   (plot-impl layers opts)))

;; Target implementations are defined progressively throughout the notebook:
;; - :geom target (thi.ng/geom) - defined before Example 1
;; - :vl target (Vega-Lite) - defined before faceting examples
;; - :plotly target (Plotly.js) - defined before Plotly-specific examples

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

;; ## Implementation: Helper Functions & :geom Target
;;
;; **Note**: This implementation section can be skipped on first reading.
;; It's placed here to make the notebook fully self-contained and reproducible
;; from top to bottom. Feel free to jump ahead to "Example 1" and return here
;; if you're interested in the rendering details.
;;
;; Before we can use `plot` in examples, we need to implement at least one target.
;; We'll start with the :geom target (thi.ng/geom-viz) for static SVG visualizations.

;; ### Helper Functions

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
          y-vals (mapv :y points)
          x-min (apply min x-vals)
          x-max (apply max x-vals)]
      (try
        (let [xss (mapv vector x-vals)
              model (regr/lm y-vals xss)
              intercept (:intercept model)
              slope (first (:beta model))
              ;; Generate 20 points for the line (simpler than 100)
              step (clojure.core// (clojure.core/- x-max x-min) 20.0)
              x-range (vec (concat (range x-min x-max step) [x-max]))]
          (mapv (fn [x]
                  {:x x
                   :y (clojure.core/+ intercept (clojure.core/* slope x))})
                x-range))
        (catch Exception e
          nil)))))

(defn- unique-values
  "Get unique values from a sequence, preserving order."
  [coll]
  (vec (distinct coll)))

(defn- color-scale
  "Create a ggplot2-like color scale for categorical data."
  [categories]
  (let [colors ["#F8766D" ;; red-orange  
                "#00BA38" ;; green
                "#619CFF" ;; blue
                "#F564E3"]] ;; magenta
    (zipmap categories (cycle colors))))

(defn- make-axes
  "Create x and y axes for thi.ng/geom-viz plot."
  [x-domain y-domain width height]
  (let [x-range (clojure.core/- (second x-domain) (first x-domain))
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
     :grid {:attribs {:stroke "#FFFFFF" :stroke-width 1}}}))

;; ### thi.ng/geom-viz Implementation

(defn- layer->scatter-spec
  "Convert a scatter layer to thi.ng/geom-viz spec."
  [layer width height]
  (let [points (layer->points layer)
        x-vals (mapv :x points)
        y-vals (mapv :y points)
        x-domain (infer-domain x-vals)
        y-domain (infer-domain y-vals)]

    (when (and (vector? x-domain) (vector? y-domain)
               (every? number? x-domain) (every? number? y-domain))
      (let [alpha (or (:aog/alpha layer) 1.0)
            color-groups (when-let [colors (seq (keep :color points))]
                           (group-by :color points))
            axes (make-axes x-domain y-domain width height)

            data (if color-groups
                   (let [categories (vec (distinct (keep :color points)))
                         colors (color-scale categories)]
                     (mapv (fn [category]
                             (let [cat-points (get color-groups category)
                                   point-data (mapv (fn [p] [(:x p) (:y p)]) cat-points)]
                               {:values point-data
                                :layout viz/svg-scatter-plot
                                :attribs {:fill (get colors category)
                                          :stroke (get colors category)
                                          :stroke-width 0.5
                                          :opacity alpha}}))
                           categories))
                   [{:values (mapv (fn [p] [(:x p) (:y p)]) points)
                     :layout viz/svg-scatter-plot
                     :attribs {:fill "#333333"
                               :stroke "#333333"
                               :stroke-width 0.5
                               :opacity alpha}}])]
        (assoc axes :data data)))))

(defn- layer->line-spec
  "Convert a line layer to thi.ng/geom-viz spec.
  
  Handles :aog/transformation :linear by computing regression lines."
  [layer width height]
  (let [transformation (:aog/transformation layer)
        points (layer->points layer)

        ;; Apply linear regression if needed
        processed-points (if (= transformation :linear)
                           (let [color-vals (keep :color points)]
                             (if (seq color-vals)
                               ;; Group by color and compute separate regressions
                               (mapcat (fn [[color group-points]]
                                         (when-let [fitted (compute-linear-regression group-points)]
                                           (mapv #(assoc % :color color) fitted)))
                                       (group-by :color points))
                               (compute-linear-regression points)))
                           points)

        sorted-points (sort-by :x processed-points)
        x-vals (mapv :x sorted-points)
        y-vals (mapv :y sorted-points)
        x-domain (infer-domain x-vals)
        y-domain (infer-domain y-vals)]

    (when (and (vector? x-domain) (vector? y-domain)
               (every? number? x-domain) (every? number? y-domain))
      (let [alpha (or (:aog/alpha layer) 1.0)
            color-groups (when-let [colors (seq (keep :color processed-points))]
                           (group-by :color processed-points))
            axes (make-axes x-domain y-domain width height)

            data (if color-groups
                   (let [categories (vec (distinct (keep :color processed-points)))
                         colors (color-scale categories)]
                     (mapv (fn [category]
                             (let [cat-points (sort-by :x (get color-groups category))
                                   line-data (mapv (fn [p] [(:x p) (:y p)]) cat-points)]
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
                               :opacity alpha}}])]
        (assoc axes :data data)))))

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

;; ### :geom Target Method

(defmethod plot-impl :geom
  [layers opts]
  (let [layers (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)
        svg-string (layers->svg layers width height)]
    (kind/html svg-string)))

;; ## Example 1: Simple Scatter Plot

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)))

;; Breaking this down:
;;
;; 1. `(data penguins)` → `{:aog/data penguins}`
;; 2. `(mapping :bill-length-mm :bill-depth-mm)` → `{:aog/x :bill-length-mm :aog/y :bill-depth-mm}`
;; 3. `(scatter)` → `{:aog/plottype :scatter}`
;; 4. `(* ...)` merges all three → `[{:aog/data penguins :aog/x ... :aog/y ... :aog/plottype :scatter}]`
;; 5. `(plot ...)` renders the layer vector to SVG

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
;;
;; 1. `(+ (scatter ...) (linear))` creates two layer specs
;; 2. `(* (data ...) (mapping ...) (+ ...))` distributes data+mapping to both
;; 3. Result: Two complete layers with same data/mapping, different plot types
;; 4. Both rendered on same plot
;;
;; This uses the distributive property: `*` distributes over `+`, so common
;; properties (data, mapping) are factored out and applied to multiple visuals.
;;
;; Notice that the linear regression is computed separately for each species, 
;; since they are grouped by the `:color` aesthetic.

;; ## Example 3: Standard Clojure Operations Work
;;
;; Because layers are plain maps, all standard library operations work.

;; Build with standard merge  
(def layer-with-merge
  (-> (data penguins)
      (merge (mapping :bill-length-mm :bill-depth-mm {:color :species}))
      (merge (scatter {:alpha 0.7}))))

(kind/pprint
 layer-with-merge)

(plot [layer-with-merge])

;; Add properties with standard assoc
(def base-layer
  (-> (data penguins)
      (merge (mapping :bill-length-mm :bill-depth-mm))
      (merge (scatter))))

(def with-color
  (-> base-layer
      (assoc :aog/color :species)))

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

;; ## Implementation: :vl Target (Vega-Lite)
;;
;; **Note**: Implementation interlude - can be skipped on first reading.
;;
;; Now we add support for the Vega-Lite target, which provides interactive
;; visualizations and built-in faceting support.

(defn- make-vega-encoding
  "Build Vega-Lite encoding from layer aesthetics."
  [layer]
  (let [x (:aog/x layer)
        y (:aog/y layer)
        color (:aog/color layer)
        col (:aog/col layer)
        row (:aog/row layer)
        facet (:aog/facet layer)]
    (cond-> {:x {:field (name x)
                 :type "quantitative"
                 :scale {:zero false}}
             :y {:field (name y)
                 :type "quantitative"
                 :scale {:zero false}}}
      color (assoc :color {:field (name color) :type "nominal"})
      col (assoc :column {:field (name col) :type "nominal"})
      row (assoc :row {:field (name row) :type "nominal"})
      facet (assoc :facet {:field (name facet) :type "nominal"}))))

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
  {:mark {:type "point" :opacity (or (:aog/alpha layer) 1.0)}
   :encoding (make-vega-encoding layer)})

(defn- layer->vega-line
  "Convert a line layer to Vega-Lite mark spec.

  Handles :aog/transformation :linear with Vega-Lite's regression transform."
  [layer]
  (let [transformation (:aog/transformation layer)
        transform (when (= transformation :linear)
                    [{:regression (name (:aog/y layer))
                      :on (name (:aog/x layer))
                      :groupby (when-let [color (:aog/color layer)]
                                 [(name color)])}])]
    (cond-> {:mark {:type "line"}
             :encoding (make-vega-encoding layer)}
      transform (assoc :transform transform))))

(defn- layer->vega-area
  "Convert an area layer to Vega-Lite mark spec."
  [layer]
  (let [spec (layer->vega-line layer)]
    (assoc spec :mark {:type "area"})))

(defn- layer->vega-bar
  "Convert a bar layer to Vega-Lite mark spec."
  [layer]
  (let [encoding (cond-> {:x {:field (name (:aog/x layer)) :type "nominal"}
                          :y {:field (name (:aog/y layer))
                              :type "quantitative"
                              :scale {:zero false}}}
                   (:aog/color layer) (assoc :color {:field (name (:aog/color layer))
                                                     :type "nominal"}))]
    {:mark {:type "bar"}
     :encoding encoding}))

(defn- layers->vega-spec
  "Convert layers to Vega-Lite specification."
  [layers width height]
  (let [layers (if (vector? layers) layers [layers])
        first-layer (first layers)
        data-values (layer->vega-data first-layer)

        ;; Check for faceting in any layer
        col-field (:aog/col first-layer)
        row-field (:aog/row first-layer)
        facet-field (:aog/facet first-layer)
        has-faceting? (or col-field row-field facet-field)

        layer-specs (mapv (fn [layer]
                            (let [plottype (:aog/plottype layer)]
                              (case plottype
                                :scatter (layer->vega-scatter layer)
                                :line (layer->vega-line layer)
                                :area (layer->vega-area layer)
                                :bar (layer->vega-bar layer)
                                (layer->vega-scatter layer))))
                          layers)

        ;; For multi-layer with faceting, remove facet encodings from individual layers
        layer-specs (if (and (> (count layer-specs) 1) has-faceting?)
                      (mapv (fn [spec]
                              (update spec :encoding dissoc :column :row :facet))
                            layer-specs)
                      layer-specs)

        base-spec (if (= 1 (count layer-specs))
                    (merge (first layer-specs)
                           {:width width
                            :height height
                            :data {:values data-values}})
                    {:width width
                     :height height
                     :data {:values data-values}
                     :layer layer-specs})]

    ;; Wrap in facet spec if we have multiple layers with faceting
    (if (and (> (count layers) 1) has-faceting?)
      (let [facet-encoding (cond-> {}
                             col-field (assoc :column {:field (name col-field) :type "nominal"})
                             row-field (assoc :row {:field (name row-field) :type "nominal"})
                             facet-field (assoc :facet {:field (name facet-field) :type "nominal"}))]
        {:data {:values data-values}
         :facet facet-encoding
         :spec (-> base-spec
                   (dissoc :data)
                   (assoc :width (/ width (if col-field 3 1)))
                   (assoc :height (/ height (if row-field 3 1))))})
      base-spec)))

;; ### :vl Target Method

(defmethod plot-impl :vl
  [layers opts]
  (let [width (or (:width opts) 400)
        height (or (:height opts) 300)
        spec (layers->vega-spec layers width height)
        ;; Add ggplot2-like theme
        themed-spec (assoc spec :config
                           {:view {:stroke nil}
                            :axis {:grid true
                                   :gridColor "#FFFFFF"
                                   :gridOpacity 1
                                   :domain false
                                   :tickColor "#FFFFFF"}
                            :background "#EBEBEB"
                            ;; ggplot2 color palette
                            :range {:category ["#F8766D" "#00BA38" "#619CFF"
                                               "#F564E3" "#00BFC4" "#B79F00"
                                               "#FF61CC" "#00B4F0" "#C77CFF"
                                               "#00C19A" "#FF6A98" "#00A9FF"]}})]
    (kind/vega-lite themed-spec)))

;; ## Example 5: Faceting (Small Multiples)
;;
;; Faceting creates separate panels for each category, demonstrating how
;; compositional power scales to more complex visualizations.
;;
;; Note that faceting is currently implemented only in the Vega-Lite target.

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species :col :island})
    (scatter {:alpha 0.7})
    (target :vl)))

;; Each island gets its own panel, with species shown by color.
;; Notice that the species distribution varies by island!

;; Faceting with multiple layers works too:
(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species :col :island})
    (+ (scatter {:alpha 0.5})
       (linear))
    (target :vl)))

;; Each facet shows both scatter points and regression lines.

;; ## Example 6: Backend Independence
;;
;; The same layer specification can be rendered with different targets.
;; This demonstrates clean separation between layer IR and rendering.

;; Define visualization once
(def viz-layers
  (* (data penguins)
     (mapping :bill-length-mm :bill-depth-mm {:color :species})
     (scatter {:alpha 0.7})))

;; Render with default target (geom - static SVG, ggplot2 aesthetics)
(plot viz-layers)

;; Specify target via options
(plot viz-layers {:target :vl})

;; Or specify target compositionally in the layer spec
(plot (* viz-layers (target :vl)))

;; Options take precedence over spec target
(plot (* viz-layers (target :vl)) {:target :geom})

;; **Key insight**: Same layer specification, different renderers.
;; The IR is truly target-agnostic. Target can be specified compositionally
;; or via options.

;; ## Implementation: :plotly Target (Plotly.js)
;;
;; **Note**: Implementation interlude - can be skipped on first reading.
;;
;; Finally, we add support for the Plotly.js target, which provides rich
;; interactive visualizations with advanced features.

(defn- group-by-color-plotly
  "Helper to create Plotly traces grouped by color column."
  [x-data y-data color-data trace-fn]
  (let [groups (group-by identity color-data)
        colors ["#F8766D" "#00BA38" "#619CFF" "#F564E3"]]
    (mapv (fn [[idx group-val]]
            (let [indices (keep-indexed (fn [i v] (when (= v group-val) i)) color-data)]
              (trace-fn group-val
                        (mapv #(nth x-data %) indices)
                        (mapv #(nth y-data %) indices)
                        (nth colors idx (first colors)))))
          (map-indexed vector (keys groups)))))

(defn- layer->plotly-trace
  "Convert a layer to a Plotly.js trace (data series).
  
  Returns a vector of Plotly trace maps."
  [layer]
  (let [data (:aog/data layer)
        dataset (if (tc/dataset? data) data (tc/dataset data))
        x-data (vec (get-column-data dataset (:aog/x layer)))
        y-data (vec (get-column-data dataset (:aog/y layer)))
        color-col (:aog/color layer)
        plottype (:aog/plottype layer)
        alpha (or (:aog/alpha layer) 1.0)
        transformation (:aog/transformation layer)]

    (cond
      ;; Scatter plot
      (= plottype :scatter)
      (if (and color-col (keyword? color-col))
        (group-by-color-plotly
         x-data y-data (vec (get-column-data dataset color-col))
         (fn [group-val x y color]
           {:x x :y y :type "scatter" :mode "markers"
            :name (str group-val)
            :marker {:size 8 :opacity alpha :color color}}))
        [{:x x-data :y y-data :type "scatter" :mode "markers"
          :marker {:size 8 :opacity alpha :color "#333333"}}])

      ;; Linear regression
      (= transformation :linear)
      (if (and color-col (keyword? color-col))
        (let [color-data (vec (get-column-data dataset color-col))]
          (group-by-color-plotly
           x-data y-data color-data
           (fn [group-val x y color]
             (let [points (mapv (fn [xi yi] {:x xi :y yi}) x y)
                   fitted (compute-linear-regression points)]
               {:x (mapv :x fitted) :y (mapv :y fitted)
                :type "scatter" :mode "lines"
                :name (str group-val " (regression)")
                :line {:color color :width 2}}))))
        (let [points (mapv (fn [x y] {:x x :y y}) x-data y-data)
              fitted (compute-linear-regression points)]
          [{:x (mapv :x fitted) :y (mapv :y fitted)
            :type "scatter" :mode "lines"
            :line {:color "#333333" :width 2}}]))

      ;; Line plot
      (= plottype :line)
      (if (and color-col (keyword? color-col))
        (group-by-color-plotly
         x-data y-data (vec (get-column-data dataset color-col))
         (fn [group-val x y color]
           {:x x :y y :type "scatter" :mode "lines"
            :name (str group-val)
            :line {:color color :width 2}}))
        [{:x x-data :y y-data :type "scatter" :mode "lines"
          :line {:color "#333333" :width 2}}])

      ;; Bar chart
      (= plottype :bar)
      [{:x x-data :y y-data :type "bar" :marker {:opacity alpha}}]

      ;; Default: treat as scatter
      :else
      [{:x x-data :y y-data :type "scatter" :mode "markers"
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
              ;; ggplot2-like theme
              :plot_bgcolor "#EBEBEB"
              :paper_bgcolor "#EBEBEB"
              :xaxis {:title ""
                      :zeroline false
                      :showgrid true
                      :gridcolor "#FFFFFF"
                      :gridwidth 1}
              :yaxis {:title ""
                      :zeroline false
                      :showgrid true
                      :gridcolor "#FFFFFF"
                      :gridwidth 1}}}))

;; ### :plotly Target Method

(defmethod plot-impl :plotly
  [layers opts]
  (let [width (or (:width opts) 600)
        height (or (:height opts) 400)
        spec (layers->plotly-spec layers width height)]
    (kind/plotly spec)))

;; # Backend Independence
;;
;; One of the key design goals is target independence: the same layer
;; specification should work with multiple rendering targets. Let's demonstrate
;; this with all three targets.

;; ## Same Layers, Three Targets

;; Define a single layer specification:
(def example-layers
  (* (data penguins)
     (mapping :bill-length-mm :bill-depth-mm {:color :species})
     (+ (scatter {:alpha 0.6})
        (linear))))

;; ### Target 1: thi.ng/geom (Static SVG)
;;
;; Great for: Static images, PDFs, publications

(plot example-layers {:target :geom})

;; ### Target 2: Vega-Lite (Interactive, Declarative)
;;
;; Great for: Web dashboards, exploratory analysis
;; Features: Hover tooltips, zooming, panning, responsive sizing

(plot example-layers {:target :vl})

;; ### Target 3: Plotly.js (Interactive, Imperative)
;;
;; Great for: Complex interactions, custom behaviors
;; Features: Hover tooltips, zooming, panning, 3D plots, animations

(plot example-layers {:target :plotly})

;; **Key observation**: Same layer specification, three different renderers.
;; No target-specific code in the layers themselves.

;; # Advanced: Direct Spec Manipulation
;;
;; The high-level API (`plot` with layers) handles most use cases. But sometimes
;; you need target-specific features that aren't exposed by the API. Because specs
;; are just Clojure data structures, you can manipulate them directly using standard
;; library functions and your knowledge of the target library.
;;
;; This is the "escape hatch" for power users—except there's nothing to escape from,
;; because it's data all the way down.

;; ### Understanding Specs as Data
;;
;; Under the hood, `plot` converts layers to target-specific specs (Vega-Lite JSON,
;; Plotly.js JSON, or SVG). These specs are just Clojure data structures - plain maps
;; and vectors that follow each target's schema.
;;
;; This transparency means you can manipulate specs using standard Clojure functions
;; when you need features beyond the high-level API.

;; ### Example: Programmatic Enhancement with Plotly Knowledge
;;
;; Because specs are data, you can use Plotly-specific features by
;; directly manipulating the spec. This is useful when you need features
;; not exposed by the high-level API.

;; First, create a basic scatter plot
(def basic-scatter
  (* (data penguins)
     (mapping :bill-length-mm :bill-depth-mm {:color :species})
     (scatter {:alpha 0.7})))

;; Normal usage - just use plot:
(plot basic-scatter {:target :plotly})

;; But if you need to customize with Plotly-specific features,
;; generate the raw spec for manipulation:
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

;; Normal usage - just use plot:
(plot example-layers {:target :vl})

;; But if you need Vega-Lite-specific features like interactive selections,
;; customize the spec directly:

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
;;
;; - Added a title with custom styling
;; - Enhanced axes with gridlines and labels
;; - Added Vega-Lite's selection mechanism for interactive legend filtering
;; All using standard Clojure functions (`assoc`, `assoc-in`) and knowledge
;; of the Vega-Lite spec structure.

;; ### Example: Combining High-Level API with Low-Level Tweaks

;; The typical workflow:
;;
;; 1. Use `plot` to get 90% of the way there
;; 2. For the last 10%, generate the raw spec and enhance it
;; 3. Render with target-specific Kindly wrapper

;; Create the layers
(def mtcars-scatter
  (* (data mtcars)
     (mapping :wt :mpg {:color :cyl})
     (scatter {:alpha 0.7})))

;; Normal usage - just use plot:
(plot mtcars-scatter {:target :plotly :width 700 :height 500})

;; For advanced Plotly features, customize the spec:
(def scatter-with-plotly-enhancements
  (-> (layers->plotly-spec mtcars-scatter 700 500)
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
;;
;; - The high-level API handles common cases
;; - Specs are transparent data structures
;; - You can drop down to lower levels when needed
;; - No "escape hatches" required - it's data all the way down
;;
;; ## Why Direct Spec Manipulation Matters
;;
;; Traditional plotting libraries often have opaque objects you can't inspect,
;; limited extension points, and "escape hatches" that break the abstraction.
;;
;; With specs as data, you get:
;;
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
;;
;; - Layers compose with `merge`, not custom `merge-layer` function
;; - All standard library operations work: `assoc`, `update`, `mapv`, `filter`, `into`
;; - No need to learn custom combinators
;;
;; **2. Transparent Intermediate Representation**
;;
;; - Layers are plain maps - inspect them with `(first layers)`
;; - No hidden template substitution to debug
;; - Error messages reference actual data structures
;;
;; **3. Backend Independence**
;;
;; - Same layer spec works with multiple renderers
;; - Easy to add new targets (just write conversion functions)
;; - No target-specific jargon in user-facing API
;;
;; **4. Flexible Data Handling**
;;
;; - Accepts plain Clojure maps: `{:x [1 2 3] :y [4 5 6]}`
;; - Accepts tech.ml.dataset datasets
;; - No forced conversion step
;;
;; **5. Clear Separation of Concerns**
;;
;; - Composition operators separate from IR (layer maps)
;; - IR separate from rendering (targets)
;; - Each layer can be understood independently
;;
;; ## What We Pay
;;
;; **1. Namespace Verbosity**
;;
;; - Plain key: `:color` (6 chars)
;; - Namespaced: `:aog/color` (11 chars)
;; - Mitigated by namespace map syntax: `#:aog{:color ...}`
;;
;; **2. Novel Operators**
;;
;; - `*` and `+` shadow arithmetic operators
;; - Need `clojure.core/*` for multiplication in implementation
;; - Users must learn non-standard interpretation of familiar operators
;;
;; **3. Incomplete Feature Set (Currently)**
;;
;; - Some statistical transforms defined but not implemented (smooth, density)
;; - Polar coordinates not yet supported
;; - Fewer plot types than mature libraries
;;
;; ## Why These Choices?
;;
;; **Flat + Namespaced Structure**
;;
;; - Enables standard `merge` (the core composability win)
;; - Prevents collisions with data columns
;; - Aligns with modern Clojure practices (Ring 2.0, clojure.spec)
;;
;; **Composition Operators (`*` and `+`)**
;;
;; - Directly translates AlgebraOfGraphics.jl concepts
;; - Distributive property enables factoring common properties
;; - Clear distinction: `*` = merge, `+` = overlay
;;
;; **Multiple Targets from Start**
;;
;; - Validates that IR is truly target-agnostic
;; - geom for static/print, Vega-Lite for interactive, Plotly for rich interactions
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
;; ;; - aog: Compositional API, target flexibility
;; ```
;;
;; ## Migration & Compatibility
;;
;; **No migration required**:
;;
;; - Existing code continues to work unchanged
;; - Users can adopt AoG API incrementally for new code
;; - APIs can interoperate through shared rendering targets
;;
;; **Potential bridges**:
;;
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

;; # Open Questions
;;
;; Feedback welcome on these design decisions:
;;
;; **1. Namespace vs Prefix Convention**
;;
;; Current: `:aog/color` (standard namespace, discoverable)
;; Alternative: `:=color` (more concise, like Tableplot v1)
;; See Design Exploration section for discussion.
;;
;; **2. Symbolic Operators**
;;
;; Current: `*` and `+` (concise, mathematical)
;; Alternatives: `compose`/`overlay`, `merge-layers`/`concat-layers`
;; See Composition Operators section for trade-offs.
;;
;; **3. clojure.spec Integration**
;;
;; Should we provide official specs for layer maps?
;; Benefits: Validation, generative testing, documentation
;; Costs: Spec dependency, maintenance burden
;;
;; **4. Extensible Target Registry**
;;
;; Should users be able to register custom rendering targets,
;; or should targets remain core-only?

;; # Summary
;;
;; ## What We've Explored
;;
;; This notebook demonstrates a complete composable graphics API for Clojure.
;;
;; **Core Design**:
;;
;; - Layers as **flat maps** with `:aog/*` namespaced keys
;; - Composition using `*` (merge) and `+` (overlay)
;; - Standard library operations work natively (`merge`, `assoc`, `mapv`, `filter`)
;; - Backend-agnostic IR enables multiple rendering targets
;;
;; **Key Insights**:
;;
;; 1. **Flat structure enables standard merge** - No custom layer-merge needed
;; 2. **Namespacing prevents collisions** - Can coexist with any data columns
;; 3. **Transparent IR** - Layers are inspectable plain maps, not templates
;; 4. **Target as data** - Rendering target specified compositionally via `(target :vl)` or options
;; 5. **Julia's compositional approach translates to Clojure data** - Same concepts, different substrate
;;
;; **Implementation Status**:
;;
;; - ✅ Core composition (`*`, `+`, layer composition)
;; - ✅ Three rendering targets (geom, Vega-Lite, Plotly)
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
;;
;; - Backend independence (one API, multiple renderers)
;; - Transparent IR (plain maps, not template substitution)
;; - Flexible data handling (maps or datasets)
;; - Composability through standard library
;;
;; ## This is a Design Document
;;
;; The purpose is to:
;;
;; - Demonstrate feasibility of the approach
;; - Explore design trade-offs
;; - Gather community feedback
;; - Inform decision-making about Tableplot's future
;;
;; ## Try It Yourself
;;
;; This notebook is fully functional and self-contained. You can:
;;
;; - Modify the examples to experiment with the API
;; - Add new plot types (extend the target conversion functions)
;; - Test with your own datasets
;; - Provide feedback on the design decisions
;;
;; **Feedback welcome**: What works? What doesn't? What would you change?
