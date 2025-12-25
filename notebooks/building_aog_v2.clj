;; # Building a Composable Graphics API in Clojure (v2)
;; **A Design Exploration for Tableplot with Minimal Delegation**
;;
;; *This notebook explores a fresh approach to composable plot specifications
;; in Clojure, inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/). 
;; It implements a **minimal delegation strategy** where we compute statistical transforms
;; (like regression lines, histograms, smoothing) and leverage rendering target capabilities
;; for rendering.*
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
;; limitation of your chosen rendering target, switching means learning a different API.
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
;; Can one API work with multiple rendering targets? Can we use plain Clojure data structures
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
;; plots in different environments ([Clay](https://scicloj.github.io/clay/), [Portal](https://github.com/djblue/portal), etc.). Each rendering target returns
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
;; This approach of 
;; ["describing a higher-level 'intent' how your tabular data should be transformed"](https://aog.makie.org/dev/tutorials/intro-i)
;; aligns naturally with Clojure's functional and declarative tendencies—something
;; we've seen in libraries like Hanami, Oz, and others in the ecosystem.

;; ## Glossary: Visualization Terminology
;;
;; Before diving deeper, let's clarify some key terms we'll use throughout:
;;
;; **Domain** - The range of input values from your data. For a column of bill lengths 
;; `[32.1, 45.3, 59.6]`, the domain is `[32.1, 59.6]`. This is *data space*.
;;
;; **Range** - The range of output values in the visualization. For an x-axis that spans 
;; from pixel 50 to pixel 550, the range is `[50, 550]`. This is *visual space*.
;;
;; **Scale** - The mapping function from domain to range. A [linear scale](https://en.wikipedia.org/wiki/Scale_(ratio))
;; maps data value 32.1 → pixel 50, and 59.6 → pixel 550, with proportional mapping in between.
;; Other scale types include logarithmic, time, and categorical.
;;
;; **Aesthetic** - A visual property that can encode data. Common aesthetics: `x` position, 
;; `y` position, `color`, `size`, `shape`, `alpha` (transparency). Each aesthetic needs 
;; a scale to map data to visual values.
;;
;; **Mapping** - The connection between a data column and an aesthetic. "Map `:bill-length` 
;; to `x` aesthetic" means "use the bill-length values to determine x positions."
;;
;; **Coordinate System** - The spatial framework for positioning marks. [Cartesian](https://en.wikipedia.org/wiki/Cartesian_coordinate_system) 
;; (standard x/y grid), [polar](https://en.wikipedia.org/wiki/Polar_coordinate_system) (angle/radius), 
;; and [geographic](https://en.wikipedia.org/wiki/Geographic_coordinate_system) (latitude/longitude) 
;; interpret positions differently and affect domain computation.
;;
;; **Statistical Transform** - A computation that derives new data from raw data. Examples: 
;; [linear regression](https://en.wikipedia.org/wiki/Linear_regression) (fit line), 
;; [histogram](https://en.wikipedia.org/wiki/Histogram) (bin and count), 
;; [kernel density](https://en.wikipedia.org/wiki/Kernel_density_estimation) (smooth distribution).
;;
;; **Layer** (in AoG sense) - A composable unit containing data, mappings, visual marks, 
;; and optional transforms. Different from ggplot2's "layer" (which means overlaid visuals).
;;
;; **Rendering Target** - The library that produces final output: thi.ng/geom (SVG), 
;; Vega-Lite (JSON spec), or Plotly (JavaScript).

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
;; Lost `:x` and `:color`!

;; Nested structure requires a custom `merge-layer` function. Not ideal.

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

;; # Rendering Targets
;;
;; This API is designed to work with multiple **rendering targets**—the actual
;; visualization libraries that produce the final output. Each target has different
;; strengths:
;;
;; - **`:geom`** ([thi.ng/geom](https://github.com/thi-ng/geom)) - Static SVG, ggplot2-style, publication-quality
;; - **`:vl`** ([Vega-Lite](https://vega.github.io/vega-lite/)) - Declarative, interactive web visualizations
;; - **`:plotly`** ([Plotly.js](https://plotly.com/javascript/)) - Rich interactivity, 3D support
;;
;; Currently, `:geom` is implemented. The others are planned.
;;
;; The key idea: you write your plot specification once using our API, and it can be
;; rendered by different targets. This separates **what** you want to visualize from
;; **how** it gets rendered.

;; # The Delegation Strategy
;;
;; A key architectural decision: **What do we compute vs. what do rendering targets handle?**
;;
;; ## The Core Principle
;;
;; **Statistical transforms require domain computation. Everything else can delegate.**
;;
;; **What are statistical transforms?** These are operations that compute derived data
;; from your raw data points:
;; - **Regression lines** - computing best-fit lines through points ([linear regression](https://en.wikipedia.org/wiki/Linear_regression))
;; - **Smoothing** - [LOESS](https://en.wikipedia.org/wiki/Local_regression) or other smoothing curves
;; - **Histograms** - binning continuous data and counting occurrences ([histogram](https://en.wikipedia.org/wiki/Histogram))
;; - **Density estimation** - [kernel density estimation](https://en.wikipedia.org/wiki/Kernel_density_estimation) curves
;;
;; These differ from simple visual mappings (scatter, line) which just render raw data.
;;
;; ### Why Statistical Transforms Drive Everything
;;
;; Consider [histogram](https://en.wikipedia.org/wiki/Histogram) computation:
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
;; **The dependency**: We can't delegate domain to the rendering target because we need it
;; BEFORE we can compute bins. Statistical transforms are an **important part of a good
;; plotting library**, so we accept this dependency.
;;
;; **Why compute on the JVM?** When working with large datasets, we want to compute
;; statistical summaries on the JVM (where we have access to the full data) and send
;; only the summarized results to the browser. For example, with a million points, we
;; compute the histogram bins on the JVM and send ~20 bars to Vega-Lite or Plotly,
;; not a million points. This keeps visualizations fast and responsive.
;;
;; ## What We Compute (Minimal Set)
;;
;; **1. Statistical Transforms**
;; - Histogram, density, smoothing, regression
;; - Why: Core value, consistency across rendering targets, inspectability
;;
;; **2. Domain Computation (Only When Needed)**
;; - Only for aesthetics involved in statistical transforms
;; - Coordinate-aware ([Cartesian](https://en.wikipedia.org/wiki/Cartesian_coordinate_system), [polar](https://en.wikipedia.org/wiki/Polar_coordinate_system), [geographic](https://en.wikipedia.org/wiki/Geographic_coordinate_system))
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
;; - Why: Rendering targets are polished, edge cases are many
;;
;; **2. Range Computation**
;; - Pixel/visual coordinates
;; - Why: Tightly coupled with layout
;;
;; **3. Domains for Simple Plots**
;; - When no transforms, rendering targets compute from data
;; - Why: Rendering targets already do this well
;;
;; **4. Scale Merging**
;; - Multi-layer plots: rendering targets handle shared domains
;; - Why: Avoid complex conflict resolution
;;
;; **5. Color Palette Application**
;; - We provide preferences (`:palette-preference :ggplot2`)
;; - Rendering targets apply from their libraries
;; - Why: Rendering targets have palette expertise
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
;; - ✅ Three rendering targets (:geom, :vl, :plotly - all with full feature parity)
;; - ✅ Statistical transforms (linear regression, histograms complete)
;; - ✅ Faceting (row, column, and grid faceting across all targets)
;; - ✅ Custom scale domains
;; - ✅ ggplot2-compatible theming
;; - ⚠️ Additional transforms (smooth/density/contour pending)

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

;; (Removed line, histogram, smooth, density, target - not yet implemented in examples)

;; (linear moved to just before first regression example)

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

;; Examples moved to after function definitions

;; (Type Information section moved to just before example that demonstrates it)

;; (Removed Domain Computation section - compute-domain was unused, we use infer-domain instead)

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

;; # Constants

;; ggplot2-compatible color palette for categorical variables
(def ^:private ggplot2-colors
  ["#F8766D" "#00BA38" "#619CFF" "#F564E3"])

;; ggplot2 theme colors
(def ^:private ggplot2-background "#EBEBEB")
(def ^:private ggplot2-grid "#FFFFFF")
(def ^:private ggplot2-default-mark "#333333")

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

(defn- ensure-dataset
  "Convert data to a tablecloth dataset if it isn't already."
  [data]
  (if (tc/dataset? data) data (tc/dataset data)))

(defn- split-by-facets
  "Split a layer's data by facet variables.
  
  Returns: Vector of {:row-label r, :col-label c, :layer layer-with-subset} maps
  If no faceting, returns vector with single element containing original layer.
  Labels are nil for dimensions without faceting."
  [layer]
  (let [col-var (:aog/col layer)
        row-var (:aog/row layer)
        data (:aog/data layer)]

    (if-not (or col-var row-var)
      ;; No faceting - return original layer
      [{:row-label nil :col-label nil :layer layer}]

      ;; Has faceting
      (let [dataset (ensure-dataset data)

            ;; Get unique values for each facet dimension
            col-categories (when col-var (sort (distinct (get dataset col-var))))
            row-categories (when row-var (sort (distinct (get dataset row-var))))

            ;; Create all combinations
            combinations (cond
                           ;; Both row and col
                           (and row-var col-var)
                           (for [r row-categories
                                 c col-categories]
                             {:row-label r :col-label c})

                           ;; Only col
                           col-var
                           (for [c col-categories]
                             {:row-label nil :col-label c})

                           ;; Only row
                           row-var
                           (for [r row-categories]
                             {:row-label r :col-label nil}))]

        ;; For each combination, filter the data
        (mapv (fn [{:keys [row-label col-label]}]
                (let [filtered (cond-> dataset
                                 row-var (tc/select-rows
                                          (fn [row]
                                            (= (get row row-var) row-label)))
                                 col-var (tc/select-rows
                                          (fn [row]
                                            (= (get row col-var) col-label))))
                      new-layer (assoc layer :aog/data filtered)]
                  {:row-label row-label
                   :col-label col-label
                   :layer new-layer}))
              combinations)))))

(defn- has-faceting?
  "Check if any layer has faceting."
  [layers-vec]
  (some #(or (:aog/col %) (:aog/row %)) layers-vec))

(defn- organize-by-facets
  "Organize multiple layers by their facet groups.
  
  Returns: Vector of {:row-label r, :col-label c, :layers [layers-for-this-facet]}
  All layers must have the same facet specification (or no faceting)."
  [layers-vec]
  (if-not (has-faceting? layers-vec)
    ;; No faceting - return all layers in single group
    [{:row-label nil :col-label nil :layers layers-vec}]

    ;; Has faceting - split each layer and group by facet
    (let [;; Split each layer by its facets
          all-split (mapcat split-by-facets layers-vec)

          ;; Group by row and col labels
          by-labels (group-by (juxt :row-label :col-label) all-split)

          ;; Get all unique combinations (sorted)
          row-labels (sort (distinct (map :row-label all-split)))
          col-labels (sort (distinct (map :col-label all-split)))

          ;; Create combinations in row-major order
          combinations (for [r row-labels
                             c col-labels]
                         [r c])]

      ;; For each combination, collect all layers
      (mapv (fn [[r c]]
              {:row-label r
               :col-label c
               :layers (mapv :layer (get by-labels [r c]))})
            combinations))))

(defn- layer->points
  "Convert layer to point data for rendering."
  [layer]
  (let [data (:aog/data layer)
        dataset (ensure-dataset data)
        x-vals (vec (get dataset (:aog/x layer)))
        y-col (:aog/y layer)
        y-vals (when y-col (vec (get dataset y-col)))
        color-col (:aog/color layer)
        color-vals (when color-col
                     (vec (get dataset color-col)))]
    (map-indexed (fn [i _]
                   (cond-> {:x (nth x-vals i)}
                     y-vals (assoc :y (nth y-vals i))
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

(defn- compute-histogram
  "Compute histogram bins using fastmath.stats/histogram.
  
  Returns: Vector of bar specifications with x-min, x-max, and height."
  [points bins-method]
  (when (seq points)
    (let [x-vals (mapv :x points)]
      (when (every? number? x-vals)
        (let [hist-result (stats/histogram x-vals (or bins-method :sturges))]
          (mapv (fn [bin]
                  (let [x-min (:min bin)
                        x-max (:max bin)]
                    {:x-min x-min
                     :x-max x-max
                     :x-center (clojure.core// (clojure.core/+ x-min x-max) 2.0)
                     :height (:count bin)}))
                (:bins-maps hist-result)))))))

(defn- apply-transform
  "Apply statistical transform to layer points.
  
  Returns structured result based on transformation type:
  - nil (no transform): {:type :raw :points points}
  - :linear: {:type :regression :points points :fitted fitted-points}
  - :histogram: {:type :histogram :points points :bars bar-specs}"
  [layer points]
  (case (:aog/transformation layer)
    :linear
    (let [fitted (compute-linear-regression points)]
      {:type :regression
       :points points
       :fitted (or fitted points)})

    :histogram
    (let [bins-method (:aog/bins layer)
          bars (compute-histogram points bins-method)]
      {:type :histogram
       :points points
       :bars bars})

    ;; Default: no transformation
    {:type :raw
     :points points}))

(defn- transform->domain-points
  "Convert transform result to points for domain computation."
  [transform-result]
  (case (:type transform-result)
    :regression (:fitted transform-result)
    :histogram (mapcat (fn [bar]
                         [{:x (:x-min bar) :y 0}
                          {:x (:x-max bar) :y (:height bar)}])
                       (:bars transform-result))
    :raw (:points transform-result)))

(defn- color-scale
  "ggplot2-like color scale for categorical data."
  [categories]
  (zipmap categories (cycle ggplot2-colors)))

(defn- render-scatter-viz
  "Render scatter plot visualization data."
  [points alpha]
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
        :attribs {:fill ggplot2-default-mark
                  :stroke ggplot2-default-mark
                  :stroke-width 0.5
                  :opacity alpha}}])))

(defn- render-line-viz
  "Render line plot visualization data."
  [points alpha]
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
        :attribs {:stroke ggplot2-default-mark
                  :stroke-width 1
                  :fill "none"
                  :opacity alpha}}])))

(defn- render-regression-viz
  "Render linear regression visualization data."
  [points fitted alpha]
  (let [color-groups (group-by :color points)]
    (if (> (count color-groups) 1)
      ;; Regression per group
      (keep (fn [[color group-points]]
              (when-let [group-fitted (compute-linear-regression group-points)]
                (let [colors (color-scale (keys color-groups))
                      line-data (mapv (fn [p] [(:x p) (:y p)]) group-fitted)]
                  {:values line-data
                   :layout viz/svg-line-plot
                   :attribs {:stroke (get colors color)
                             :stroke-width 2
                             :fill "none"
                             :opacity alpha}})))
            color-groups)
      ;; Single regression
      (when fitted
        [{:values (mapv (fn [p] [(:x p) (:y p)]) fitted)
          :layout viz/svg-line-plot
          :attribs {:stroke ggplot2-default-mark
                    :stroke-width 2
                    :fill "none"
                    :opacity alpha}}]))))

(defn- render-histogram-viz
  "Render histogram visualization data."
  [bars alpha]
  (when bars
    (mapv (fn [bar]
            {:type :rect
             :x-min (:x-min bar)
             :x-max (:x-max bar)
             :height (:height bar)
             :attribs {:fill ggplot2-default-mark
                       :stroke ggplot2-grid
                       :stroke-width 1
                       :opacity alpha}})
          bars)))

(defn- transform->viz-data
  "Convert transform result to visualization data.
  
  Returns vector of viz specs ready for rendering."
  [layer transform-result alpha]
  (case (:type transform-result)
    :regression
    (render-regression-viz (:points transform-result)
                           (:fitted transform-result)
                           alpha)

    :histogram
    (render-histogram-viz (:bars transform-result) alpha)

    :raw
    (case (:aog/plottype layer)
      :scatter (render-scatter-viz (:points transform-result) alpha)
      :line (render-line-viz (:points transform-result) alpha)
      [])))

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

(defn- render-single-panel
  "Render a single plot panel (for use in both faceted and non-faceted plots).
  
  Args:
  - layers: Vector of layers to render in this panel
  - x-domain, y-domain: Domain for x and y axes
  - width, height: Panel dimensions
  - x-offset, y-offset: Horizontal and vertical offsets for this panel
  
  Returns: Map with :background, :plot, :hist-rects"
  [layers x-domain y-domain width height x-offset y-offset]
  (let [x-range (clojure.core/- (second x-domain) (first x-domain))
        y-range (clojure.core/- (second y-domain) (first y-domain))
        x-major (max 1 (clojure.core/* x-range 0.2))
        y-major (max 1 (clojure.core/* y-range 0.2))

        ;; Adjust panel boundaries for offsets
        panel-left (clojure.core/+ 50 x-offset)
        panel-right (clojure.core/+ panel-left (clojure.core/- width 100))
        panel-top (clojure.core/+ 50 y-offset)
        panel-bottom (clojure.core/+ panel-top (clojure.core/- height 100))

        ;; Create axes
        x-axis (viz/linear-axis
                {:domain x-domain
                 :range [panel-left panel-right]
                 :major x-major
                 :pos panel-bottom})
        y-axis (viz/linear-axis
                {:domain y-domain
                 :range [panel-bottom panel-top]
                 :major y-major
                 :pos panel-left})

        ;; Process each layer to viz data
        layer-data (mapcat (fn [layer]
                             (let [points (layer->points layer)
                                   alpha (or (:aog/alpha layer) 1.0)
                                   transform-result (apply-transform layer points)]
                               (transform->viz-data layer transform-result alpha)))
                           layers)

        ;; Separate regular viz data from histogram rectangles
        {viz-data true rect-data false} (group-by #(not= (:type %) :rect) layer-data)

;; Create the plot spec for regular data
        plot-spec {:x-axis x-axis
                   :y-axis y-axis
                   :grid {:attribs {:stroke ggplot2-grid :stroke-width 1}}
                   :data (vec viz-data)}

        ;; Background rectangle for this panel
        bg-rect (svg/rect [panel-left panel-top]
                          (clojure.core/- width 100)
                          (clojure.core/- height 100)
                          {:fill ggplot2-background
                           :stroke ggplot2-grid
                           :stroke-width 1})

        ;; Convert histogram rectangles to SVG
        x-scale (fn [x] (clojure.core/+ panel-left
                                        (clojure.core/* (/ (clojure.core/- x (first x-domain))
                                                           (clojure.core/- (second x-domain) (first x-domain)))
                                                        (clojure.core/- width 100))))
        y-scale (fn [y] (clojure.core/- panel-bottom
                                        (clojure.core/* (/ (clojure.core/- y (first y-domain))
                                                           (clojure.core/- (second y-domain) (first y-domain)))
                                                        (clojure.core/- height 100))))

        hist-rects (mapv (fn [r]
                           (svg/rect [(x-scale (:x-min r)) (y-scale (:height r))]
                                     (clojure.core/- (x-scale (:x-max r)) (x-scale (:x-min r)))
                                     (clojure.core/- (y-scale 0) (y-scale (:height r)))
                                     (:attribs r)))
                         (or rect-data []))]

    {:background bg-rect
     :plot (viz/svg-plot2d-cartesian plot-spec)
     :hist-rects hist-rects}))

(defn- get-scale-domain
  "Extract custom domain for an aesthetic from layers, or return nil if not specified."
  [layers-vec aesthetic]
  (let [scale-key (keyword "aog" (str "scale-" (name aesthetic)))]
    (some #(get-in % [scale-key :domain]) layers-vec)))

;; This is a placeholder - full implementation coming
(defmethod plot-impl :geom
  [layers opts]
  (let [layers-vec (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)

        ;; Organize layers by facets
        facet-groups (organize-by-facets layers-vec)

        ;; Determine grid dimensions
        row-labels (distinct (map :row-label facet-groups))
        col-labels (distinct (map :col-label facet-groups))
        num-rows (count row-labels)
        num-cols (count col-labels)
        is-faceted? (or (> num-rows 1) (> num-cols 1))

;; Calculate panel dimensions
        panel-width (/ width num-cols)
        panel-height (/ height num-rows)

        ;; Check for custom scale domains
        custom-x-domain (get-scale-domain layers-vec :x)
        custom-y-domain (get-scale-domain layers-vec :y)

        ;; Compute transformed points for ALL facets (for shared domain)
        all-transformed-points
        (mapcat (fn [{:keys [layers]}]
                  (mapcat (fn [layer]
                            (let [points (layer->points layer)
                                  transform-result (apply-transform layer points)]
                              (transform->domain-points transform-result)))
                          layers))
                facet-groups)

        ;; Compute domains (use custom if provided, otherwise infer)
        x-vals (keep :x all-transformed-points)
        y-vals (keep :y all-transformed-points)
        x-domain (or custom-x-domain (infer-domain x-vals))
        y-domain (or custom-y-domain (infer-domain y-vals))

        ;; Validate domains
        valid? (and (vector? x-domain) (vector? y-domain)
                    (every? number? x-domain) (every? number? y-domain))]

    (if-not valid?
      (kind/hiccup
       [:div {:style {:padding "20px"}}
        [:p "Cannot render: non-numeric data or empty dataset"]
        [:pre (pr-str {:x-domain x-domain :y-domain y-domain})]])

      ;; Render panels
      (let [;; Create row/col position lookup
            row-positions (zipmap row-labels (range num-rows))
            col-positions (zipmap col-labels (range num-cols))

            ;; Render each facet panel with grid position
            panels (mapv
                    (fn [{:keys [row-label col-label layers]}]
                      (let [row-idx (get row-positions row-label 0)
                            col-idx (get col-positions col-label 0)
                            x-offset (clojure.core/* col-idx panel-width)
                            y-offset (clojure.core/* row-idx panel-height)
                            panel (render-single-panel layers x-domain y-domain
                                                       panel-width panel-height
                                                       x-offset y-offset)]
                        (assoc panel
                               :row-label row-label
                               :col-label col-label
                               :row-idx row-idx
                               :col-idx col-idx
                               :x-offset x-offset
                               :y-offset y-offset)))
                    facet-groups)

            ;; Collect all elements
            all-backgrounds (mapv :background panels)
            all-plots (mapv :plot panels)
            all-hist-rects (mapcat :hist-rects panels)

            ;; Add facet labels if faceted
            facet-labels (when is-faceted?
                           (concat
                            ;; Column labels (top)
                            (when (> num-cols 1)
                              (map (fn [col-label]
                                     (let [col-idx (get col-positions col-label)
                                           label-x (clojure.core/+ (clojure.core/* col-idx panel-width)
                                                                   (/ panel-width 2))]
                                       (svg/text [label-x 30] (str col-label)
                                                 {:text-anchor "middle"
                                                  :font-family "Arial, sans-serif"
                                                  :font-size 12
                                                  :font-weight "bold"})))
                                   col-labels))

                            ;; Row labels (left side)
                            (when (> num-rows 1)
                              (map (fn [row-label]
                                     (let [row-idx (get row-positions row-label)
                                           label-y (clojure.core/+ (clojure.core/* row-idx panel-height)
                                                                   (/ panel-height 2))]
                                       (svg/text [20 label-y] (str row-label)
                                                 {:text-anchor "middle"
                                                  :font-family "Arial, sans-serif"
                                                  :font-size 12
                                                  :font-weight "bold"
                                                  :transform (str "rotate(-90 20 " label-y ")")})))
                                   row-labels))))

            ;; Combine into single SVG
            svg-elem (apply svg/svg
                            {:width width :height height}
                            (concat all-backgrounds
                                    all-plots
                                    all-hist-rects
                                    (or facet-labels [])))]

        (kind/html (svg/serialize svg-elem))))))

;; Placeholder implementations for other targets
(defmethod plot-impl :vl
  [layers opts]
  (let [layers-vec (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)

        ;; Check for faceting
        facet-groups (organize-by-facets layers-vec)
        is-faceted? (has-faceting? layers-vec)

        ;; Get facet dimensions if faceted
        row-var (when is-faceted? (some :aog/row layers-vec))
        col-var (when is-faceted? (some :aog/col layers-vec))

        ;; Check for custom scale domains
        custom-x-domain (get-scale-domain layers-vec :x)
        custom-y-domain (get-scale-domain layers-vec :y)

        ;; Helper to convert layer to VL data format
        layer->vl-data (fn [layer]
                         (let [data (:aog/data layer)
                               dataset (ensure-dataset data)
                               x-col (:aog/x layer)
                               y-col (:aog/y layer)
                               color-col (:aog/color layer)
                               row-col (:aog/row layer)
                               col-col (:aog/col layer)
                               rows (tc/rows dataset :as-maps)]
                           (mapv (fn [row]
                                   (cond-> {}
                                     x-col (assoc (keyword (name x-col)) (get row x-col))
                                     y-col (assoc (keyword (name y-col)) (get row y-col))
                                     color-col (assoc (keyword (name color-col)) (get row color-col))
                                     row-col (assoc (keyword (name row-col)) (get row row-col))
                                     col-col (assoc (keyword (name col-col)) (get row col-col))))
                                 rows)))

        ;; Helper to create VL mark for a layer
        layer->vl-mark (fn [layer]
                         (let [plottype (:aog/plottype layer)
                               transform (:aog/transformation layer)]
                           (cond
                             (= transform :linear) "line"
                             (= transform :histogram) "bar"
                             (= plottype :scatter) "circle"
                             (= plottype :line) "line"
                             :else "circle")))

        ;; Helper to create encoding for a layer
        layer->vl-encoding (fn [layer vl-data]
                             (let [x-col (:aog/x layer)
                                   y-col (:aog/y layer)
                                   color-col (:aog/color layer)
                                   alpha (:aog/alpha layer)
                                   transform (:aog/transformation layer)]
                               (cond-> {}
                                 x-col (assoc :x (cond-> {:field (name x-col) :type "quantitative"}
                                                   true (assoc :scale (merge {:zero false}
                                                                             (when custom-x-domain {:domain custom-x-domain})))))
                                 y-col (assoc :y (cond-> {:field (name y-col) :type "quantitative"}
                                                   true (assoc :scale (merge {:zero false}
                                                                             (when custom-y-domain {:domain custom-y-domain})))))
                                 color-col (assoc :color {:field (name color-col)
                                                          :type "nominal"
                                                          :scale {:range ggplot2-colors}})
                                 alpha (assoc :opacity {:value alpha}))))

        ;; Process each layer
        vl-layers (mapcat (fn [layer]
                            (let [points (layer->points layer)
                                  transform-result (apply-transform layer points)]
                              (case (:type transform-result)
                                ;; Scatter/line - use raw data
                                :raw
                                [{:mark (layer->vl-mark layer)
                                  :data {:values (layer->vl-data layer)}
                                  :encoding (layer->vl-encoding layer (layer->vl-data layer))}]

                                ;; Linear regression - send computed line
                                :regression
                                (let [fitted (:fitted transform-result)
                                      fitted-data (mapv (fn [p]
                                                          {(keyword (name (:aog/x layer))) (:x p)
                                                           (keyword (name (:aog/y layer))) (:y p)})
                                                        fitted)
                                      ;; Handle color grouping
                                      color-col (:aog/color layer)]
                                  (if color-col
                                    ;; Compute regression per color group
                                    (let [color-groups (group-by :color points)]
                                      (mapv (fn [[color-val group-points]]
                                              (when-let [group-fitted (compute-linear-regression group-points)]
                                                (let [group-fitted-data (mapv (fn [p]
                                                                                {(keyword (name (:aog/x layer))) (:x p)
                                                                                 (keyword (name (:aog/y layer))) (:y p)
                                                                                 (keyword (name color-col)) color-val})
                                                                              group-fitted)]
                                                  {:mark "line"
                                                   :data {:values group-fitted-data}
                                                   :encoding (layer->vl-encoding layer group-fitted-data)})))
                                            color-groups))
                                    ;; Single regression line
                                    [{:mark "line"
                                      :data {:values fitted-data}
                                      :encoding (layer->vl-encoding layer fitted-data)}]))

                                ;; Histogram - send computed bars
                                :histogram
                                (let [bars (:bars transform-result)
                                      bar-data (mapv (fn [bar]
                                                       {:bin-start (:x-min bar)
                                                        :bin-end (:x-max bar)
                                                        :count (:height bar)})
                                                     bars)]
                                  [{:mark "bar"
                                    :data {:values bar-data}
                                    :encoding {:x {:field "bin-start"
                                                   :type "quantitative"
                                                   :bin {:binned true :step (- (:x-max (first bars)) (:x-min (first bars)))}
                                                   :axis {:title (name (:aog/x layer))}}
                                               :x2 {:field "bin-end"}
                                               :y {:field "count" :type "quantitative"}}}]))))
                          layers-vec)

        ;; Remove nils from nested regression per-group
        vl-layers (remove nil? (flatten vl-layers))

;; ggplot2-compatible theme config
        ggplot2-config {:view {:stroke "transparent"}
                        :background ggplot2-background
                        :axis {:gridColor ggplot2-grid
                               :domainColor ggplot2-grid
                               :tickColor ggplot2-grid}
                        :mark {:color ggplot2-default-mark}}

        ;; Build final spec
        spec (cond
               ;; Faceted plot
               is-faceted?
               (let [;; For faceted plots, remove :data from individual layers
                     ;; Data goes at TOP level for VL faceting, not inside spec
                     layers-without-data (mapv #(dissoc % :data) vl-layers)
                     all-data (mapcat layer->vl-data layers-vec)]
                 (cond-> {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                          :data {:values all-data}
                          :width (int (/ width (if col-var 3 1)))
                          :height (int (/ height (if row-var 3 1)))
                          :config ggplot2-config
                          :spec {:layer layers-without-data}}
                   col-var (assoc :facet {:column {:field (name col-var) :type "nominal"}})
                   row-var (assoc-in [:facet :row] {:field (name row-var) :type "nominal"})))

               ;; Multi-layer plot
               (> (count vl-layers) 1)
               {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                :width width
                :height height
                :config ggplot2-config
                :layer vl-layers}

               ;; Single layer plot
               :else
               (merge {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                       :width width
                       :height height
                       :config ggplot2-config}
                      (first vl-layers)))]

    (kind/vega-lite spec)))

(defmethod plot-impl :plotly
  [layers opts]
  (let [layers-vec (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)

;; Check for faceting
        is-faceted? (has-faceting? layers-vec)
        row-var (when is-faceted? (some :aog/row layers-vec))
        col-var (when is-faceted? (some :aog/col layers-vec))

        ;; Check for custom scale domains
        custom-x-domain (get-scale-domain layers-vec :x)
        custom-y-domain (get-scale-domain layers-vec :y)

        ;; Helper to convert layer to Plotly data
        layer->plotly-data (fn [layer]
                             (let [data (:aog/data layer)
                                   dataset (ensure-dataset data)
                                   x-col (:aog/x layer)
                                   y-col (:aog/y layer)
                                   color-col (:aog/color layer)
                                   row-col (:aog/row layer)
                                   col-col (:aog/col layer)]
                               {:x-vals (when x-col (vec (tc/column dataset x-col)))
                                :y-vals (when y-col (vec (tc/column dataset y-col)))
                                :color-vals (when color-col (vec (tc/column dataset color-col)))
                                :row-vals (when row-col (vec (tc/column dataset row-col)))
                                :col-vals (when col-col (vec (tc/column dataset col-col)))
                                :x-name (when x-col (name x-col))
                                :y-name (when y-col (name y-col))}))

        ;; Process each layer into Plotly traces
        plotly-traces (mapcat (fn [layer]
                                (let [plotly-data (layer->plotly-data layer)
                                      points (layer->points layer)
                                      transform-result (apply-transform layer points)
                                      plottype (:aog/plottype layer)
                                      transform (:aog/transformation layer)
                                      color-col (:aog/color layer)]
                                  (case (:type transform-result)
                                    ;; Scatter plot
                                    :raw
                                    (if color-col
                                      ;; Grouped scatter - one trace per group
                                      (let [color-groups (group-by :color points)]
                                        (map-indexed
                                         (fn [idx [color-val group-points]]
                                           {:type "scatter"
                                            :mode "markers"
                                            :x (mapv :x group-points)
                                            :y (mapv :y group-points)
                                            :name (str color-val)
                                            :marker {:color (get ggplot2-colors idx ggplot2-default-mark)
                                                     :size 8}})
                                         color-groups))
                                      ;; Single scatter trace
                                      [{:type "scatter"
                                        :mode "markers"
                                        :x (:x-vals plotly-data)
                                        :y (:y-vals plotly-data)
                                        :marker {:color ggplot2-default-mark :size 8}
                                        :showlegend false}])

                                    ;; Linear regression
                                    :regression
                                    (if color-col
                                      ;; Per-group regression
                                      (let [color-groups (group-by :color points)]
                                        (map-indexed
                                         (fn [idx [color-val group-points]]
                                           (when-let [fitted (compute-linear-regression group-points)]
                                             {:type "scatter"
                                              :mode "lines"
                                              :x (mapv :x fitted)
                                              :y (mapv :y fitted)
                                              :name (str color-val " (fit)")
                                              :line {:color (get ggplot2-colors idx ggplot2-default-mark)
                                                     :width 2}
                                              :showlegend false}))
                                         color-groups))
                                      ;; Single regression line
                                      (let [fitted (:fitted transform-result)]
                                        [{:type "scatter"
                                          :mode "lines"
                                          :x (mapv :x fitted)
                                          :y (mapv :y fitted)
                                          :line {:color ggplot2-default-mark :width 2}
                                          :showlegend false}]))

                                    ;; Histogram
                                    :histogram
                                    (let [bars (:bars transform-result)]
                                      [{:type "bar"
                                        :x (mapv (fn [b] (clojure.core// (clojure.core/+ (:x-min b) (:x-max b)) 2)) bars)
                                        :y (mapv :height bars)
                                        :width (mapv (fn [b] (clojure.core/- (:x-max b) (:x-min b))) bars)
                                        :marker {:color ggplot2-default-mark
                                                 :line {:color ggplot2-grid :width 1}}
                                        :showlegend false}]))))
                              layers-vec)

        ;; Remove nils (mapcat already flattened one level)
        plotly-traces (remove nil? plotly-traces)

        ;; ggplot2-compatible layout
        layout {:width width
                :height height
                :plot_bgcolor ggplot2-background
                :paper_bgcolor ggplot2-background
                :xaxis {:gridcolor ggplot2-grid
                        :title (or (:x-name (layer->plotly-data (first layers-vec))) "x")
                        :range custom-x-domain}
                :yaxis {:gridcolor ggplot2-grid
                        :title (or (:y-name (layer->plotly-data (first layers-vec))) "y")
                        :range custom-y-domain}
                :margin {:l 60 :r 30 :t 30 :b 60}}

        ;; Handle faceting
        spec (if is-faceted?
               (let [;; Get unique facet values
                     first-layer (first layers-vec)
                     data (:aog/data first-layer)
                     dataset (ensure-dataset data)
                     row-vals (when row-var (sort (distinct (tc/column dataset row-var))))
                     col-vals (when col-var (sort (distinct (tc/column dataset col-var))))
                     num-rows (if row-var (count row-vals) 1)
                     num-cols (if col-var (count col-vals) 1)

                     ;; Create subplot traces
                     faceted-traces
                     (for [[row-idx row-val] (map-indexed vector (or row-vals [nil]))
                           [col-idx col-val] (map-indexed vector (or col-vals [nil]))]
                       (let [;; Filter data for this facet
                             filtered-data (cond-> dataset
                                             row-var (tc/select-rows #(= (get % row-var) row-val))
                                             col-var (tc/select-rows #(= (get % col-var) col-val)))
                             ;; Create layer with filtered data
                             facet-layer (assoc (first layers-vec) :aog/data filtered-data)
                             facet-points (layer->points facet-layer)
                             transform-result (apply-transform facet-layer facet-points)
                             subplot-idx (clojure.core/+ (clojure.core/* row-idx num-cols) col-idx 1)
                             xaxis-key (if (= subplot-idx 1) :xaxis (keyword (str "xaxis" subplot-idx)))
                             yaxis-key (if (= subplot-idx 1) :yaxis (keyword (str "yaxis" subplot-idx)))]
                         (case (:type transform-result)
                           :raw
                           {:type "scatter"
                            :mode "markers"
                            :x (mapv :x facet-points)
                            :y (mapv :y facet-points)
                            :xaxis (name xaxis-key)
                            :yaxis (name yaxis-key)
                            :marker {:color ggplot2-default-mark :size 6}
                            :showlegend false}

                           :histogram
                           (let [bars (:bars transform-result)]
                             {:type "bar"
                              :x (mapv (fn [b] (clojure.core// (clojure.core/+ (:x-min b) (:x-max b)) 2)) bars)
                              :y (mapv :height bars)
                              :width (mapv (fn [b] (clojure.core/- (:x-max b) (:x-min b))) bars)
                              :xaxis (name xaxis-key)
                              :yaxis (name yaxis-key)
                              :marker {:color ggplot2-default-mark
                                       :line {:color ggplot2-grid :width 1}}
                              :showlegend false})

                           nil)))

                     ;; Create subplot layout
                     subplot-layout (merge layout
                                           {:grid {:rows num-rows :columns num-cols :pattern "independent"}
                                            :annotations
                                            (concat
                                             (when col-var
                                               (for [[idx val] (map-indexed vector col-vals)]
                                                 {:text (str val)
                                                  :xref "paper"
                                                  :yref "paper"
                                                  :x (clojure.core// (clojure.core/+ idx 0.5) num-cols)
                                                  :y 1.0
                                                  :xanchor "center"
                                                  :yanchor "bottom"
                                                  :showarrow false}))
                                             (when row-var
                                               (for [[idx val] (map-indexed vector row-vals)]
                                                 {:text (str val)
                                                  :xref "paper"
                                                  :yref "paper"
                                                  :x -0.05
                                                  :y (clojure.core/- 1.0 (clojure.core// (clojure.core/+ idx 0.5) num-rows))
                                                  :xanchor "right"
                                                  :yanchor "middle"
                                                  :showarrow false})))})]
                 {:data (remove nil? faceted-traces)
                  :layout subplot-layout})
               ;; Non-faceted
               {:data plotly-traces
                :layout layout})]

    (kind/plotly spec)))

;; ## Example 1: Simple Scatter Plot (Delegated Domain)

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)))

;; **What happens here**:
;; 1. We create layer spec with data, mapping, plottype
;; 2. We DON'T compute X/Y domains
;; 3. Rendering target receives data and computes domain itself
;; 4. This is simpler - we delegate what rendering targets do well!

;; ## Example 2: Plain Clojure Maps

;; The API works with plain Clojure data structures, not just datasets!

(plot
 (* (data {:x [1 2 3 4 5]
           :y [2 4 6 8 10]})
    (mapping :x :y)
    (scatter)))

;; **What happens here**:
;; 1. Simple Clojure map with vectors of data
;; 2. No need to convert to tech.ml.dataset
;; 3. Type inference falls back to examining values
;; 4. Everything just works!

;; This is particularly useful for quick exploration or when working with
;; simple data that doesn't need the full power of tech.ml.dataset.

;; ## Example 3: Type Information in Action

;; Now let's see how we can use Tablecloth's type information.
;; This is a KEY WIN: we get types for free, no complex inference needed!

(defn- infer-from-values
  "Simple fallback type inference for plain Clojure data."
  [values]
  (cond
    (every? number? values) :continuous
    (some #(instance? java.time.temporal.Temporal %) values) :temporal
    :else :categorical))

(defn infer-scale-type
  "Infer scale type from values in a layer."
  [layer aesthetic]
  (let [data (:aog/data layer)
        dataset (ensure-dataset data)
        col-key (get layer (keyword "aog" (name aesthetic)))
        values (when col-key (tc/column dataset col-key))]
    (cond
      (nil? values) nil
      (every? number? values) :continuous
      (some #(instance? java.time.temporal.Temporal %) values) :temporal
      :else :categorical)))

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

;; ## Example 4: Multi-Layer Composition (Scatter + Linear Regression)

;; First, we need a way to add statistical transforms. Let's add [linear regression](https://en.wikipedia.org/wiki/Linear_regression):

(defn linear
  "Add linear regression transformation.
  
  Computes best-fit line through points.
  When combined with color aesthetic, computes separate regression per group."
  []
  {:aog/transformation :linear
   :aog/plottype :line})

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
                      :faceting (select-keys layer [:aog/row :aog/col])
                      :data-rows (when-let [data (:aog/data layer)]
                                   (if (tc/dataset? data)
                                     (tc/row-count data)
                                     (count data)))})
                   layers-vec)}))

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
;; 5. Rendering target renders both with delegated domains

;; ## Example 4b: Same Plot Using Distributivity

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

;; ## Example 5: Color Mapping with Categorical Variable

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (scatter)))

;; **What happens here**:
;; 1. `:color :species` maps species to color aesthetic
;; 2. Type system recognizes :species as categorical (via Tablecloth)
;; 3. Rendering target creates color scale automatically
;; 4. Three species = three colors in the scatter plot

;; ## Example 6: Multi-Layer with Color Groups

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

;; ## Example 7: Different Dataset (mtcars)

(plot
 (* (data mtcars)
    (mapping :wt :mpg)
    (scatter)))

;; **What happens here**:
;; 1. Works with any dataset
;; 2. Different column names (:wt, :mpg instead of bill-*)
;; 3. Type inference works the same way
;; 4. Rendering target handles domain computation

;; ## Example 8: mtcars with Regression

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

;; ## Debugging & Inspection

;; Before moving on to more complex features, let's look at a useful debugging tool
;; that helps us understand what's happening inside our layer specifications.

(kind/pprint
 (inspect-layers
  (* (data mtcars)
     (mapping :wt :mpg)
     (+ (scatter)
        (linear)))))

;; **What we see**:
;; - Two layers created (scatter + linear regression)
;; - Both share the same data and mappings
;; - Different plottypes and transformations
;; - Data row count shown for verification

;; **An important insight from building this**:
;;
;; During development, we discovered that regression lines need careful domain handling.
;; If we compute domains from raw scatter points, regression lines can extend beyond
;; that range (e.g., mtcars regression endpoint y=8.296 falls below the scatter domain
;; min y=10.4). This is why we compute domains AFTER applying statistical transforms—
;; transforms can legitimately extend beyond the raw data range!

;; ## Example 9: Histogram

;; Histograms are a key example of why we compute statistical transforms ourselves.
;; We need the domain to compute bin edges!

(defn histogram
  "Add histogram transformation.
  
  Bins continuous data and counts occurrences. Requires domain computation
  to determine bin edges.
  
  Options:
  - :bins - Binning method: :sturges (default), :sqrt, :rice, :freedman-diaconis, or explicit number"
  ([]
   {:aog/transformation :histogram
    :aog/plottype :bar
    :aog/bins :sturges})
  ([opts]
   (merge {:aog/transformation :histogram
           :aog/plottype :bar
           :aog/bins :sturges}
          (update-keys opts #(keyword "aog" (name %))))))

(defn facet
  "Add faceting to a layer specification.
  
  Args:
  - layer-spec: Layer or vector of layers
  - facet-spec: Map with :row and/or :col keys specifying faceting variables
  
  Examples:
  (facet layer {:col :species})
  (facet layer {:row :sex :col :island})"
  [layer-spec facet-spec]
  (let [facet-keys (update-keys facet-spec #(keyword "aog" (name %)))]
    (if (vector? layer-spec)
      (mapv #(merge % facet-keys) layer-spec)
      (merge layer-spec facet-keys))))

(defn scale
  "Specify scale properties for an aesthetic.
  
  Args:
  - aesthetic: Keyword like :x, :y, :color
  - opts: Map with scale options:
    - :domain - [min max] for continuous, or vector of categories
    - :transform - :log, :sqrt, :identity (default)
  
  Examples:
  (scale :x {:domain [0 100]})
  (scale :y {:transform :log})
  (scale :color {:domain [:setosa :versicolor :virginica]})"
  [aesthetic opts]
  (let [scale-key (keyword "aog" (str "scale-" (name aesthetic)))]
    {scale-key opts}))

;; Test histogram computation in isolation

(plot
 (* (data penguins)
    (mapping :bill-length-mm nil)
    (histogram)))

;; **What happens here**:
;; 1. We map only `:bill-length-mm` to x (no y mapping for histograms)
;; 2. Histogram transform bins the data using Sturges' rule
;; 3. We compute: domain → bin edges → bin counts
;; 4. Rendering target receives bars (bin ranges + heights), not raw points
;; 5. This shows why we can't fully delegate - we need domain BEFORE binning!

;; Try different binning methods:

(plot
 (* (data penguins)
    (mapping :bill-length-mm nil)
    (histogram {:bins 15})))

;; ## Debugging & Inspection Utilities

;; inspect-layers moved earlier to before its first use

;; # Faceting: Architectural Questions Revealed
;;
;; Implementing faceting has exposed several important design questions:
;;
;; ## 1. Statistical Transforms Must Be Per-Facet
;;
;; **Example**: Histogram of bill-length faceted by species
;; - Semantically: User expects 3 separate histograms (one per species)
;; - Implementation: Must split data by `:species` BEFORE computing histograms
;; - Implication: Can't compute transforms before knowing about facets
;;
;; **Current architecture**: We apply transforms in `plot-impl` after extracting points
;; **Needed**: Apply transforms to each facet group separately
;;
;; ## 2. Domain Computation: Shared vs Free Scales
;;
;; **Shared scales** (ggplot2 default):
;; - All facets use same x/y domain
;; - Pro: Easy to compare across facets
;; - Con: Facets with fewer observations may have compressed ranges
;;
;; **Free scales**:
;; - Each facet optimizes its own domain
;; - Pro: Each facet uses full visual range
;; - Con: Harder to compare magnitudes across facets
;;
;; **Decision**: Start with shared scales (simpler), add `:scales` option later
;;
;; ## 3. Delegation Strategy: Control vs Leverage
;;
;; **Our principle**: We control semantics, targets handle presentation
;;
;; **What we control**:
;; - Data splitting by facet variable
;; - Per-facet transform computation
;; - Domain computation (shared or free)
;; - Color scale consistency
;;
;; **What targets can handle**:
;; - `:geom` - we compute layout positions manually
;; - `:vl`/`:plotly` - could use their grid layout features
;;
;; ## 4. Rendering Architecture for :geom
;;
;; **Challenge**: thi.ng/geom-viz doesn't support multi-panel layouts
;;
;; **Needed**:
;; 1. Calculate panel dimensions (width / num-facets)
;; 2. Calculate panel positions (x-offsets)
;; 3. Create mini-plot for each facet at its position
;; 4. Add facet labels  
;; 5. Combine into single SVG
;;
;; **Complexity**: Significant refactoring of plot-impl :geom
;;
;; ## 5. The Core Insight
;;
;; **Faceting reveals the same pattern as statistical transforms**:
;; - We MUST control the semantics (data splitting, per-group computation)
;; - But we CAN delegate presentation (layout, when targets support it)
;; - This validates our minimal delegation strategy
;;
;; **For statistical transforms + faceting**:
;; ```clojure
;; (plot (facet (* (data penguins)
;;                 (mapping :bill-length-mm nil)
;;                 (histogram))
;;              {:col :species}))
;; ```
;;
;; This requires:
;; 1. Split data by species (3 groups)
;; 2. Compute histogram for EACH group (per-facet transforms)
;; 3. Collect domains across all groups (shared scales)
;; 4. Render 3 mini-histograms side-by-side
;;
;; **This is our value proposition**: Compute on JVM, send summaries to browser.
;; With faceting, even more important!"

;; # Faceting Exploration
;;
;; Let's explore faceting to see what architectural questions emerge.

;; ## Faceting Design Decisions
;;
;; After prototyping, we've decided:
;;
;; 1. **We control semantics** - Split data, compute transforms per-facet, manage domains
;; 2. **Targets handle presentation** - Layout and rendering (when they can)
;; 3. **Shared scales by default** - All facets use same domain (easier comparison)
;; 4. **Statistical transforms per-facet** - Histogram by species = 3 separate histograms
;;
;; ## Implementation Strategy
;;
;; 1. `split-by-facets` - Groups data by facet variable(s)
;; 2. Apply transforms to each facet group separately
;; 3. Compute domains across all facets (for shared scales)
;; 4. Render each facet as mini-plot
;;
;; For :geom target - compute layout positions manually
;; For :vl/:plotly targets - could use their grid layout features

;; ## Example 10: Simple Column Faceting
;;
;; Facet a scatter plot by species - this creates 3 side-by-side plots.

;; Test faceted scatter plot - 3 side-by-side plots
(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm :bill-depth-mm)
           (scatter))
        {:col :species}))

;; Test faceted histogram - per-species histograms with shared scales
(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm nil)
           (histogram))
        {:col :species}))

;; ## Example 11: Row Faceting
;;
;; Facet by rows creates vertically stacked panels

(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm :bill-depth-mm)
           (scatter))
        {:row :species})
 {:height 600})

;; ## Example 12: Row × Column Grid Faceting
;;
;; Create a 2D grid of facets - the full power of faceting!
;; This creates a 3×2 grid (3 islands × 2 sexes = 6 panels)

(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm :bill-depth-mm)
           (scatter))
        {:row :island :col :sex})
 {:width 800 :height 900})

;; **What happens here**:
;; 1. Data split by both `:island` (3 values) and `:sex` (2 values)
;; 2. Creates 3×2 = 6 panels in a grid
;; 3. Column labels at top, row labels on left (rotated)
;; 4. Shared scales across all panels for easy comparison
;; 5. Per-panel rendering with proper x and y offsets

;; ## Example 13: Custom Scale Domains
;;
;; Override auto-computed domains to control axis ranges

;; Force y-axis to start at 0
(plot
 (* (data mtcars)
    (mapping :wt :mpg)
    (scatter)
    (scale :y {:domain [0 40]})))

;; **What happens here**:
;; 1. Y-axis forced to [0, 40] instead of auto-computed [10.4, 33.9]
;; 2. Useful for starting axes at meaningful values (like 0)
;; 3. Custom domains compose via `*` operator

;; Custom domains on both axes
(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)
    (scale :x {:domain [30 65]})
    (scale :y {:domain [10 25]})))

;; **What happens here**:
;; 1. Both axes use custom ranges
;; 2. Zooms into a specific region of the data
;; 3. Useful for focusing on areas of interest or ensuring consistent scales across multiple plots

;; # Multiple Rendering Targets
;;
;; One of the key benefits of our API design is **backend agnosticism**. The same
;; plot specification can be rendered by different visualization libraries.
;;
;; So far, all examples have used the `:geom` target (thi.ng/geom for static SVG).
;; Now let's demonstrate the `:vl` target (Vega-Lite for interactive web visualizations).

;; ## Example 14: Simple Scatter with Vega-Lite

;; The exact same specification, just with `:target :vl`:

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter))
 {:target :vl})

;; **What's different**:
;; 1. Interactive tooltips on hover
;; 2. Pan and zoom capabilities
;; 3. Vega-Lite's polished default styling
;; 4. Same data, same API, different rendering

;; ## Example 15: Multi-Layer with Vega-Lite

;; Scatter + regression works too:

(plot
 (* (data mtcars)
    (mapping :wt :mpg)
    (+ (scatter)
       (linear)))
 {:target :vl})

;; **What happens here**:
;; 1. Scatter plot rendered as VL `point` mark
;; 2. Regression computed on JVM (our delegation strategy!)
;; 3. Fitted line sent to VL as `line` mark
;; 4. VL layers them together

;; ## Example 16: Color Mapping with Vega-Lite

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (+ (scatter)
       (linear)))
 {:target :vl})

;; **What happens here**:
;; 1. Color mapping becomes VL's color encoding
;; 2. VL provides interactive legend
;; 3. Regression computed per species (3 separate lines)
;; 4. Click legend to filter interactively!

;; ## Example 17: Histogram with Vega-Lite

(plot
 (* (data penguins)
    (mapping :bill-length-mm nil)
    (histogram))
 {:target :vl})

;; **What happens here**:
;; 1. Histogram bins computed on JVM using fastmath
;; 2. Bar data sent to VL
;; 3. VL renders as bar chart
;; 4. Interactive tooltips show bin ranges and counts

;; ## Example 18: Faceting with Vega-Lite

(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm :bill-depth-mm)
           (scatter))
        {:col :species})
 {:target :vl})

;; **What happens here**:
;; 1. Our API detects faceting specification
;; 2. Delegates to VL's native column faceting
;; 3. VL handles layout and labels
;; 4. Each panel is independently interactive

;; ## Example 19: Grid Faceting with Vega-Lite

(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm :bill-depth-mm)
           (scatter))
        {:row :island :col :sex})
 {:target :vl :width 800 :height 600})

;; **What happens here**:
;; 1. 2D grid faceting using VL's row × column faceting
;; 2. VL handles all layout computation
;; 3. Shared scales across all panels
;; 4. Interactive exploration across the grid

;; ## Example 20: Custom Domains with Vega-Lite

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)
    (scale :x {:domain [30 65]})
    (scale :y {:domain [10 25]}))
 {:target :vl})

;; **What happens here**:
;; 1. Custom domains passed to VL's scale specification
;; 2. VL respects our domain constraints
;; 3. Same composition semantics across targets

;; ## The Power of Backend Agnosticism
;;
;; **Key insight**: Our flat map representation with `:aog/*` keys creates a clean
;; separation between plot semantics and rendering implementation.
;;
;; **What we control** (across all targets):
;; - Statistical transforms (regression, histogram)
;; - Data transformations
;; - Composition semantics
;;
;; **What targets handle** (differently):
;; - `:geom` - Static SVG, manual layout, ggplot2-like styling
;; - `:vl` - Interactive web viz, native faceting, Vega-Lite styling
;; - `:plotly` - Rich interactivity, 3D support (coming soon)
;;
;; This is the payoff of our design choices:
;; - Flat maps compose with standard library
;; - Minimal delegation keeps implementation focused
;; - Backend-agnostic IR enables multiple rendering strategies

;; # Plotly.js Target Examples
;;
;; Now let's explore the `:plotly` target, which provides rich interactivity
;; and is particularly strong for dashboards and web applications.

;; ## Example 21: Simple Scatter with Plotly

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter))
 {:target :plotly})

;; **What's different from :geom and :vl**:
;; 1. Hover tooltips showing exact x/y values
;; 2. Toolbar with zoom, pan, and download options
;; 3. Smooth animations and transitions
;; 4. Same ggplot2 theming (grey background, white grid)

;; ## Example 22: Multi-Layer with Plotly

(plot
 (* (data mtcars)
    (mapping :wt :mpg)
    (+ (scatter)
       (linear)))
 {:target :plotly})

;; **What happens here**:
;; 1. Scatter plot rendered as Plotly scatter trace
;; 2. Regression computed on JVM (our minimal delegation!)
;; 3. Both traces combined in single Plotly spec
;; 4. Interactive hover works for both layers

;; ## Example 23: Color-Grouped Regression with Plotly

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (+ (scatter)
       (linear)))
 {:target :plotly})

;; **What happens here**:
;; 1. Three scatter traces (one per species) with ggplot2 colors
;; 2. Three regression lines (computed per-group on JVM)
;; 3. Matching colors for scatter points and regression lines
;; 4. Interactive legend - click to show/hide species
;; 5. Demonstrates full composability with color aesthetics

;; ## Example 24: Histogram with Plotly

(plot
 (* (data penguins)
    (mapping :bill-length-mm nil)
    (histogram))
 {:target :plotly :width 500})

;; **What happens here**:
;; 1. Histogram computed on JVM using Sturges' rule
;; 2. Pre-computed bins sent to Plotly as bar trace
;; 3. White bar borders (ggplot2 theme)
;; 4. Hover shows bin range and count

;; ## Example 25: Faceted Scatter with Plotly

(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm :bill-depth-mm)
           (scatter))
        {:col :species})
 {:target :plotly :width 800 :height 400})

;; **What happens here**:
;; 1. Data split by species (3 facets)
;; 2. Each facet rendered in separate subplot
;; 3. Shared axes for easy comparison
;; 4. Species names as column headers
;; 5. Independent zoom/pan for each subplot

;; ## Example 26: Custom Domains with Plotly

(plot
 (* (data penguins)
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)
    (scale :x {:domain [30 65]})
    (scale :y {:domain [10 25]}))
 {:target :plotly})

;; **What happens here**:
;; 1. Custom domain constraints respected
;; 2. Zoom/pan constrained to specified ranges
;; 3. Same composition semantics across all targets

;; ## Example 27: Full Feature Demo with Plotly

;; All features together - histogram faceted by species:

(plot
 (facet (* (data penguins)
           (mapping :bill-length-mm nil)
           (histogram {:bins 12}))
        {:col :species})
 {:target :plotly :width 900 :height 350})

;; **What happens here**:
;; 1. Per-species histograms (computed on JVM)
;; 2. Faceted layout (3 columns)
;; 3. Shared y-axis for easy comparison
;; 4. Custom bin count (12 bins)
;; 5. Full interactivity with hover tooltips

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
;; - Leverage rendering target polish for rendering
;; - Simple, focused implementation
;;
;; ## Implementation Status
;;
;; - ✅ Core composition (`*`, `+`, layer merging)
;; - ✅ Type inference via Tablecloth
;; - ✅ Statistical transforms (linear regression, histogram)
;; - ✅ Faceting (column, row, and 2D grid across all targets)
;; - ✅ Custom scale domains
;; - ✅ :geom rendering target (thi.ng/geom with SVG output)
;; - ✅ :vl rendering target (Vega-Lite with interactive web viz)
;; - ✅ :plotly rendering target (Plotly.js with rich interactivity)
;; - ✅ ggplot2-compatible theming across all targets
;; - ✅ Full feature parity (scatter, regression, histogram, faceting) across all three targets
;; - ⚠️ Additional transforms (smooth, density, contour - planned)
;;
;; ## Next Steps
;;
;; 1. Add smooth (LOESS) and density (kernel density estimation) transforms
;; 2. Add free scales option for faceting
;; 3. Add contour plots and heatmaps
;; 4. Performance optimization for large datasets
;; 5. Gather community feedback

;; ---
;; *This is a design exploration. Feedback welcome!*
