;; # Building a Composable Graphics API in Clojure, Version 1
;; **A Design Exploration for Tableplot**
;;
;; This is the first post in a series documenting the design and implementation of a new
;; compositional plotting API for Clojure. We're exploring fresh approaches to declarative
;; data visualization, drawing inspiration from Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/)
;; while staying true to Clojure's values of simplicity and composability.
;;
;; Each post in this series combines narrative explanation with executable code—this is a
;; working Clojure notebook where every example runs and produces real visualizations.
;; You'll see the API evolve from basic scatter plots through faceting, statistical transforms,
;; and support for multiple rendering backends. By the end, we'll have a complete prototype
;; that handles real-world plotting tasks while maintaining an inspectable design.
;;
;; ## 📖 A Bit of Context: Tableplot's Journey
;;
;; Before we dive into the technical details, let's talk about where we're coming from.
;;
;; [Tableplot](https://scicloj.github.io/tableplot/) was created in mid-2024 as a pragmatic plotting solution for the
;; [Noj](https://scicloj.github.io/noj/) toolkit—Clojure's growing data science
;; and scientific computing ecosystem. We needed *something* that worked, and we
;; needed it soon. The goal was to add to Noj's offering:
;; a way to visualize data without leaving Clojure or reaching for external tools.
;;
;; Since then, Tableplot's current APIs 
;; ([`scicloj.tableplot.v1.hanami`](https://scicloj.github.io/tableplot/tableplot_book.hanami_walkthrough.html) and
;; [`scicloj.tableplot.v1.plotly`](https://scicloj.github.io/tableplot/tableplot_book.plotly_walkthrough.html)) 
;; have been used in quite a few serious projects.
;;
;; However, we never intended these APIs to be the final word on
;; plotting in Clojure. They were a decent compromise—pragmatic, functional,
;; good enough to be useful. Better designs have been waiting to be explored.
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
;; recently reinitiated by Timothy Pratley with new spirit and a stronger focus on
;; open-source collaboration.
;;
;; With that context in mind, let's explore what we're building.

;; # Context & Motivation
;;
;; ## 📖 Why Explore a New Approach?
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
;; you get Vega-Lite—which works for many use cases but has limitations
;; with certain coordinate systems. If you choose `plotly`, you get interactivity
;; but rendering static images programmatically becomes tricky. When you hit a
;; limitation of your chosen rendering target, switching means learning a different API.
;;
;; The intermediate representation between the API and the renderers uses Hanami
;; templates. Template substitution is flexible, but it can be
;; open-ended and difficult to understand when debugging. Error messages sometimes
;; reference template internals rather than your code, and it's not always clear
;; which substitution parameters are valid in which contexts.
;;
;; The APIs also currently expect [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) 
;; datasets. If you have a simple Clojure map or vector, you need to convert it 
;; first—even for simple visualizations.
;;
;; ## 📖 What We're Exploring
;;
;; Some of these limitations will be addressed within the current APIs themselves—
;; we're actively working on improvements. But as we always intended, it's valuable
;; to explore fresh solutions in parallel. A fresh design lets us ask questions
;; that are harder to answer incrementally: Can we design an API where the intermediate
;; representation is plain maps that are easy to inspect and manipulate with standard
;; operations, while working harmoniously with both a functional interface and multiple
;; rendering targets?
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

(ns building-aog-v3
  (:refer-clojure :exclude [* +])
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
;; which eliminates the need for complex type inference.
;;
;; [**Kindly**](https://scicloj.github.io/kindly-noted/) is the visualization protocol that lets this notebook render
;; plots in different environments ([Clay](https://scicloj.github.io/clay/), [Portal](https://github.com/djblue/portal), etc.). Each rendering target returns
;; a Kindly-wrapped spec.
;;
;; [**thi.ng/geom**](https://github.com/thi-ng/geom) gives us the static SVG target for visualizations that can be saved as images.
;; We specifically use
;; [geom.viz](https://github.com/thi-ng/geom/blob/feature/no-org/org/examples/viz/demos.org) for data visualization.
;;
;; [**Fastmath**](https://github.com/generateme/fastmath) handles our statistical computations, particularly linear
;; regression. It's a comprehensive math library for Clojure.
;;
;; [**RDatasets**](https://vincentarelbundock.github.io/Rdatasets/articles/data.html) provides classic datasets (penguins, mtcars, iris) for examples.
;; It is made available in Clojure through [metamorph.ml](https://github.com/scicloj/metamorph.ml).

;; # Inspiration: AlgebraOfGraphics.jl
;;
;; This design is inspired by Julia's [AlgebraOfGraphics.jl](https://aog.makie.org/stable/),
;; a visualization library that builds on decades of experience from systems
;; like [ggplot2](https://ggplot2.tidyverse.org/) while introducing clear compositional principles.
;;
;; As the [AoG philosophy](https://aog.makie.org/stable/philosophy) states:

;; > "In a declarative framework, the user needs to express the _question_, and the 
;; > library will take care of creating the visualization."
;;
;; This approach of 
;; ["describing a higher-level 'intent' how your tabular data should be transformed"](https://aog.makie.org/dev/tutorials/intro-i)
;; aligns naturally with Clojure's functional and declarative tendencies—something
;; we've seen in libraries like Hanami, Oz, and others in the ecosystem.
;;
;; We chose AoG because it seemed small enough to grasp and reproduce, while still being
;; reasonably complete in its scope.

;; ## 📖 Glossary: Visualization Terminology
;;
;; Before diving deeper, let's clarify some terms we'll use throughout:
;;
;; **Domain** - The extent of input values from your data. For a column of bill lengths 
;; containing values 32.1, 45.3, and 59.6, the domain extends from 32.1 to 59.6. This is *data space*.
;;
;; **Range** - The extent of output values in the visualization. For an x-axis that spans 
;; from pixel 50 to pixel 550, the range extends from 50 to 550. This is *visual space*.
;;
;; **Scale** - The mapping function from domain to range. A [linear scale](https://en.wikipedia.org/wiki/Scale_(ratio))
;; maps data value 32.1 → pixel 50, and 59.6 → pixel 550, with proportional mapping in between.
;; Other scale types include logarithmic, time, and categorical.
;;
;; **Aesthetic** - A visual property that can encode data. Common aesthetics: `x` position, 
;; `y` position, `color`, `size`, `shape`, `alpha` (opacity). Each aesthetic needs 
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
;; **Rendering Target** - The library that produces final output: thi.ng/geom, 
;; Vega-Lite, or Plotly.

;; ## 📖 Core Insight: Layers + Operations
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
;; *(Note: This is a different notion from "layers" in
;; the [layered grammar of graphics](https://vita.had.co.nz/papers/layered-grammar.html) used by ggplot2 and Vega-Lite,
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

;; ## 📖 Comparison to ggplot2
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
;; while `+` means "overlay visuals"—and enables composability.

;; ## 📖 Translating to Clojure
;;
;; Julia's approach relies on custom `*` and `+` operators defined on Layer types,
;; using multiple dispatch to handle different type combinations with object-oriented
;; layer representations. This works in Julia's type system.
;;
;; One of our goals here would be bringing this compositional approach while staying
;; true to Clojure idioms: using plain data structures (maps, not objects), enabling
;; standard library operations like `merge`, `assoc`, and `filter`, maintaining the
;; compositional benefits, and making the intermediate representation transparent and
;; inspectable. How do we get the compositional approach of AoG while keeping everything
;; as simple Clojure data?

;; # Design Exploration
;;
;; The core design question: how should we structure layer specifications
;; so they compose naturally with Clojure's standard library?

;; ## 📖 The Problem: Nested Doesn't Merge

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
;; Lost `:x` and `:color`.

;; Nested structure requires a custom `merge-layer` function. Not ideal.

;; ## 📖 The Solution: Flat Structure with Namespaced Keys

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
;; - **`:geom`** ([thi.ng/geom](https://github.com/thi-ng/geom)) - Static SVG, easy to save to files
;; - **`:vl`** ([Vega-Lite](https://vega.github.io/vega-lite/)) - Interactive web visualizations, some coordinate system limitations
;; - **`:plotly`** ([Plotly.js](https://plotly.com/javascript/)) - Interactive with 3D support, static export is tricky
;;
;; The idea: you write your plot specification once using our API, and it can be
;; rendered by different targets. This separates **what** you want to visualize from
;; **how** it gets rendered.

;; # The Delegation Strategy
;;
;; A key architectural decision: **What do we compute vs. what do rendering targets handle?**
;;
;; ## 📖 The Core Principle
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
;; ### Statistical Transforms Need Domains First
;;
;; Consider [histogram](https://en.wikipedia.org/wiki/Histogram) computation:
;;
;; ```clojure
;; (* (data penguins) (mapping :bill-length) (histogram))
;; 
;; ```

;; Computation sequence:

;; 1. Must compute domain first (extent from 32.1 to 59.6)
;; 2. Use domain to decide bin edges  
;; 3. Compute bin counts
;; 4. Create bar chart from computed bins
;;
;; **The dependency**: We can't delegate domain to the rendering target because we need it
;; BEFORE we can compute bins. Statistical transforms are an **important part of a
;; plotting library**, so we accept this dependency.
;;
;; **Why compute in our library rather than delegate to rendering targets?** This project is
;; part of a holistic toolkit for data and science. We need visualizations to be consistent
;; with statistical algorithms - when you compute a regression or histogram using Clojure's
;; statistical libraries, the visualization should show exactly the same calculation.
;; Additionally, when working with large datasets, we want to compute statistical summaries
;; in our library (where we have access to the full data) and send only the summarized
;; results to rendering targets. For example, with a million points, we compute the histogram
;; bins and send ~20 bars to Vega-Lite or Plotly, not a million points.
;;
;; ## 📖 What We Compute (Minimal Set)
;; 
;; **1. Statistical Transforms**
;; - Histogram, density, smoothing, regression
;; - Why: Core value, consistency across rendering targets, inspectability
;; 
;; **2. Domain Computation**
;; - Always for :geom target (min/max, then :geom handles "nice numbers")
;; - Only custom domains for :vl and :plotly targets
;; - Why: Required by statistical transforms, :geom needs explicit domains
;; 
;; **3. Type Information**
;; - Use Tablecloth's column types (`col/typeof`)
;; - Fallback inference for plain maps
;; - Why: Free from Tablecloth, needed for transform selection
;; 
;; **4. Theme Colors**
;; - ggplot2 color palette, background, and grid colors
;; - Why: Consistent theming across all rendering targets
;; 
;; ## 📖 What We Delegate (Maximize)
;; 
;; **1. Axis Rendering**
;; - Tick placement, "nice numbers", label formatting
;; - Why: Rendering targets are polished, edge cases are many
;; 
;; **2. Range Computation**
;; - Pixel/visual coordinates
;; - Why: Tightly coupled with layout
;; 
;; **3. Domains for :vl and :plotly (when not customized)**
;; - Rendering targets compute from data we send
;; - Why: Rendering targets already do this well
;; 
;; **4. Scale Merging**
;; - Multi-layer plots: rendering targets handle shared domains
;; - Why: Avoid complex conflict resolution

;; ## 📖 Tablecloth Provides Types
;;
;; We don't need complex type inference. Tablecloth already knows:
;;
;; ```clojure
;; (col/typeof (penguins :bill-length-mm))  ;; => :float64
;; (col/typeof (penguins :species))         ;; => :string
;; ```

;; # Proposed Design
;;
;; ## 📖 API Overview
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
;; - ✅ Threading-macro friendly API (`->` works naturally)
;; - ✅ Minimal delegation (compute transforms, delegate rendering)
;; - ✅ Type information from Tablecloth
;; - ✅ Type-aware grouping (categorical color groups, continuous doesn't)
;; - ✅ Explicit `:group` aesthetic for override control
;; - ✅ Three rendering targets (:geom, :vl, :plotly - all with full feature parity)
;; - ✅ Statistical transforms: linear regression, histograms (with grouping support)
;; - ✅ Faceting (row, column, and grid faceting across all targets)
;; - ✅ Custom scale domains
;; - ✅ ggplot2-compatible theming
;; - ✅ Plain Clojure data structures (maps, vectors - no dataset required)
;;
;; **What's Missing (compared to tableplot.v1.plotly)**:
;;
;; - ⚠️ Plot types: line, bar, box, violin, density, smooth, heatmap, text, segment
;; - ⚠️ Additional aesthetics: size, symbol/shape, opacity, fill, line-width
;; - ⚠️ Statistical transforms: density estimation, smooth (loess/spline), correlation
;; - ⚠️ Coordinate systems: 3D, polar, geo
;; - ⚠️ Advanced layouts: subplots, secondary axes, insets
;; - ⚠️ Interactivity: hover templates, click events, selections
;;
;; **Design Philosophy Differences**:
;;
;; This API prioritizes composability and algebraic clarity over feature completeness.
;; The focus is on a minimal, well-understood core that can be extended incrementally.
;; Missing features are deferred, not abandoned - they can be added as needed while
;; maintaining the compositional design.

;; ## 📖 How Plots are Displayed
;;
;; Layer specifications returned by `*` and `+` are **automatically displayed as plots**
;; in the notebook. This means you typically don't need to call `plot` explicitly.
;;
;; ```clojure
;; ;; Auto-displays as plot:
;; (-> penguins
;;     (mapping :bill-length-mm :bill-depth-mm)
;;     (scatter))
;;
;; ;; To inspect the raw layer data, use kind/pprint:
;; (kind/pprint
;;   (-> penguins
;;       (mapping :bill-length-mm :bill-depth-mm)
;;       (scatter)))
;;
;; ;; To get the target spec (for debugging or customization):
;; (plot
;;   (-> penguins
;;       (mapping :bill-length-mm :bill-depth-mm)
;;       (scatter)))
;; ```
;;
;; **When to use `plot` explicitly**:
;; - Debugging: Inspect the Plotly.js/Vega-Lite/SVG spec
;; - Customization: Post-process the spec with target-specific features
;; - Extension: Add features not yet supported by the layer API
;;
;; **When to use `kind/pprint`**:
;; - Inspect the raw layer specification (`:aog/*` keys)
;; - Understand how composition merges layers
;; - Debug layer construction before rendering

;; # Part I: Infrastructure Setup
;;
;; This section establishes the core infrastructure that all features build upon.
;; We define constants, helper functions, multimethod extension points, composition 
;; operators, and all three rendering targets. Features defined later will add their 
;; own `defmethod` implementations to extend the system.

;; ## 📖 Core Concepts
;;
;; ### Layer Representation
;;
;; Layers are flat maps with namespaced keys (`:aog/*`):

^{:kindly/hide-code true}
(kind/code
 "{:aog/data penguins
  :aog/x :bill-length-mm
  :aog/y :bill-depth-mm
  :aog/color :species
  :aog/plottype :scatter
  :aog/alpha 0.7}")

;; **Why flat + namespaced?**
;; - Standard `merge` works correctly (no custom layer-merge needed)
;; - No collision with data columns (`:aog/plottype` ≠ `:plottype`)
;; - All standard library operations work: `assoc`, `update`, `mapv`, `filter`, `into`

;; ### Two Operators
;;
;; **`*` (merge)** - Combines layer properties:

^{:kindly/hide-code true}
(kind/code
 "(* (data penguins) (mapping :x :y) (scatter))
  ;; → [{:aog/data penguins :aog/x :x :aog/y :y :aog/plottype :scatter}]")

;; **`+` (concatenate)** - Overlays multiple layers:

^{:kindly/hide-code true}
(kind/code
 "(+ (scatter) (linear))
  ;; → [{:aog/plottype :scatter} {:aog/transformation :linear}]")

;; **Distributive property**: `a * (b + c) = (a * b) + (a * c)`

^{:kindly/hide-code true}
(kind/code
 "(* (data penguins) (mapping :x :y) (+ (scatter) (linear)))
  ;; → Two layers, both with same data and mapping")

;; ### Dataflow
;;
;; 1. **Construction**: Build layers with `*` and `+`
;; 2. **Transform**: Apply statistical transforms (regression, histogram)
;; 3. **Render**: Convert to visualization (target-specific)

;; ## 📖 Infrastructure Setup
;;
;; This section establishes the core infrastructure that all features build upon.
;; We define the multimethod extension points, composition operators, and rendering
;; scaffolding. Features defined later will add their own `defmethod` implementations.

;; ### Constants

(def ^:private ggplot2-colors
  ["#F8766D" "#00BA38" "#619CFF" "#F564E3"])

(def ^:private ggplot2-background "#EBEBEB")
(def ^:private ggplot2-grid "#FFFFFF")
(def ^:private ggplot2-default-mark "#333333")

;; ### Helper Functions

(defn- ensure-dataset
  "Convert data to tablecloth dataset if it isn't already.

  Accepts:
  - tech.ml.dataset datasets (passed through)
  - Maps of vectors: {:x [1 2 3] :y [4 5 6]}
  - Vector of maps: [{:x 1 :y 4} {:x 2 :y 5}]"
  [data]
  (cond
    (tc/dataset? data) data
    (map? data)
    (let [values (vals data)]
      (when-not (every? sequential? values)
        (throw (ex-info "Map data must have sequential values (vectors or lists)"
                        {:data data
                         :invalid-keys (filter #(not (sequential? (get data %))) (keys data))})))
      (tc/dataset data))
    (and (sequential? data) (every? map? data))
    (tc/dataset data)
    :else
    (throw (ex-info "Data must be a dataset, map of vectors, or vector of maps"
                    {:data data
                     :type (type data)}))))

(defn- infer-from-values
  "Simple fallback type inference for plain Clojure data."
  [values]
  (cond
    (every? number? values) :continuous
    (some #(instance? java.time.temporal.Temporal %) values) :temporal
    :else :categorical))

(defn- categorical-type?
  "Check if a column type should be treated as categorical."
  [col-type]
  (contains? #{:string :keyword :boolean :symbol :text} col-type))

(defn- get-grouping-column
  "Determine which column should be used for grouping statistical transforms.

  Logic:
  1. Explicit :aog/group always wins
  2. If :aog/color is categorical type, use it for grouping
  3. Otherwise, no grouping (continuous/temporal color is visual-only)"
  [layer dataset]
  (let [group-col (:aog/group layer)
        color-col (:aog/color layer)]
    (cond
      group-col group-col
      (and color-col dataset)
      (let [col-type (try
                       (col/typeof (get dataset color-col))
                       (catch Exception _
                         (infer-from-values (get dataset color-col))))]
        (when (categorical-type? col-type)
          color-col))
      :else nil)))

(defn- layer->points
  "Convert layer to point data for processing."
  [layer]
  (let [data (:aog/data layer)
        dataset (ensure-dataset data)
        x-vals (vec (get dataset (:aog/x layer)))
        y-col (:aog/y layer)
        y-vals (when y-col (vec (get dataset y-col)))
        color-col (:aog/color layer)
        color-vals (when color-col (vec (get dataset color-col)))
        group-col (get-grouping-column layer dataset)
        group-vals (when group-col (vec (get dataset group-col)))]
    (map-indexed (fn [i _]
                   (cond-> {:x (nth x-vals i)}
                     y-vals (assoc :y (nth y-vals i))
                     color-vals (assoc :color (nth color-vals i))
                     group-vals (assoc :group (nth group-vals i))))
                 x-vals)))

(defn- color-scale
  "ggplot2-like color scale for categorical data."
  [categories]
  (zipmap categories (cycle ggplot2-colors)))

(defn- infer-domain
  "Infer domain from data values."
  [values]
  (cond
    (empty? values) [0 1]
    (every? number? values) [(apply min values) (apply max values)]
    :else (vec (distinct values))))

;; ### Multimethod Declarations
;;
;; These establish the extension points. Features will add their own `defmethod`
;; implementations later.

(defmulti apply-transform
  "Apply statistical transform to layer points.

  Dispatches on :aog/transformation key. Features add defmethod implementations."
  (fn [layer points] (:aog/transformation layer)))

(defmulti transform->domain-points
  "Convert transform result to points for domain computation.

  Dispatches on :type key in transform result."
  (fn [transform-result] (:type transform-result)))

(defmulti render-layer
  "Render a layer for a specific target.

  Dispatches on [target plottype-or-transform]."
  (fn [target layer transform-result alpha]
    [target (or (:aog/transformation layer) (:aog/plottype layer))]))

;; ### Rendering Infrastructure

(defn- split-by-facets
  "Split a layer's data by facet variables."
  [layer]
  (let [col-var (:aog/col layer)
        row-var (:aog/row layer)
        data (:aog/data layer)]
    (if-not (or col-var row-var)
      [{:row-label nil :col-label nil :layer layer}]
      (let [dataset (ensure-dataset data)
            col-categories (when col-var (sort (distinct (get dataset col-var))))
            row-categories (when row-var (sort (distinct (get dataset row-var))))
            combinations (cond
                           (and row-var col-var)
                           (for [r row-categories c col-categories]
                             {:row-label r :col-label c})
                           col-var
                           (for [c col-categories]
                             {:row-label nil :col-label c})
                           row-var
                           (for [r row-categories]
                             {:row-label r :col-label nil}))]
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
  "Organize multiple layers by their facet groups."
  [layers-vec]
  (if-not (has-faceting? layers-vec)
    [{:row-label nil :col-label nil :layers layers-vec}]
    (let [all-split (mapcat split-by-facets layers-vec)
          by-labels (group-by (juxt :row-label :col-label) all-split)
          row-labels (sort (distinct (map :row-label all-split)))
          col-labels (sort (distinct (map :col-label all-split)))
          combinations (for [r row-labels c col-labels] [r c])]
      (mapv (fn [[r c]]
              {:row-label r
               :col-label c
               :layers (mapv :layer (get by-labels [r c]))})
            combinations))))

(defn- get-scale-domain
  "Extract custom domain for an aesthetic from layers."
  [layers-vec aesthetic]
  (let [scale-key (keyword "aog" (str "scale-" (name aesthetic)))]
    (some #(get-in % [scale-key :domain]) layers-vec)))

(defn- render-single-panel
  "Render a single plot panel."
  [layers x-domain y-domain width height x-offset y-offset]
  (let [x-range (clojure.core/- (second x-domain) (first x-domain))
        y-range (clojure.core/- (second y-domain) (first y-domain))
        x-major (max 1 (clojure.core/* x-range 0.2))
        y-major (max 1 (clojure.core/* y-range 0.2))
        panel-left (clojure.core/+ 50 x-offset)
        panel-right (clojure.core/+ panel-left (clojure.core/- width 100))
        panel-top (clojure.core/+ 50 y-offset)
        panel-bottom (clojure.core/+ panel-top (clojure.core/- height 100))
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
        layer-data (mapcat (fn [layer]
                             (let [points (layer->points layer)
                                   alpha (or (:aog/alpha layer) 1.0)
                                   transform-result (apply-transform layer points)]
                               (render-layer :geom layer transform-result alpha)))
                           layers)
        {viz-data true rect-data false} (group-by #(not= (:type %) :rect) layer-data)
        plot-spec {:x-axis x-axis
                   :y-axis y-axis
                   :grid {:attribs {:stroke ggplot2-grid :stroke-width 1}}
                   :data (vec viz-data)}
        bg-rect (svg/rect [panel-left panel-top]
                          (clojure.core/- width 100)
                          (clojure.core/- height 100)
                          {:fill ggplot2-background
                           :stroke ggplot2-grid
                           :stroke-width 1})
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

;; ### Composition Operators

(defn- layers?
  "Check if x is a vector of layer maps."
  [x]
  (and (vector? x)
       (seq x)
       (map? (first x))
       (some #(= "aog" (namespace %))
             (keys (first x)))))

(defmulti plot-impl
  "Internal multimethod for plot dispatch."
  (fn [layers opts]
    (let [layers-vec (if (vector? layers) layers [layers])
          spec-target (some :aog/target layers-vec)]
      (or (:target opts) spec-target :geom))))

(defn plot
  "Unified rendering function supporting multiple targets."
  ([layers]
   (plot-impl layers {}))
  ([layers opts]
   (plot-impl layers opts)))

(defn displays-as-plot
  "Annotate layers to auto-display as a plot in notebooks."
  [layers]
  (kind/fn layers
    {:kindly/f #'plot}))

(defn *
  "Merge layer specifications (composition)."
  ([x]
   (displays-as-plot
    (if (map? x) [x] x)))
  ([x y]
   (let [;; Helper: check if a map is a layer (has :aog/* keys) or data
         is-layer? (fn [m]
                     (and (map? m)
                          (some #(= "aog" (namespace %)) (keys m))))
         ;; Convert to layers: wrap data, pass through layers/vectors
         to-layers (fn [v]
                     (cond
                       (layers? v) v
                       (is-layer? v) [v]
                       (map? v) [{:aog/data v}] ; Dataset or map of vectors
                       :else [{:aog/data v}])) ; Other data
         x' (to-layers x)
         y' (to-layers y)]
     (displays-as-plot
      (cond
        ;; Both are single-element vectors
        (and (= 1 (count x')) (= 1 (count y')))
        [(merge (first x') (first y'))]

        ;; x is single element, y is multiple
        (= 1 (count x'))
        (mapv #(merge (first x') %) y')

        ;; y is single element, x is multiple
        (= 1 (count y'))
        (mapv #(merge % (first y')) x')

        ;; Both are multiple elements
        :else
        (vec (for [a x', b y'] (merge a b)))))))
  ([x y & more]
   (displays-as-plot
    (reduce * (* x y) more))))

(defn +
  "Combine multiple layer specifications for overlay."
  [& layer-specs]
  (displays-as-plot
   (if (and (>= (count layer-specs) 3)
            (layers? (first layer-specs)))
     (* (first layer-specs) (apply + (rest layer-specs)))
     (vec (mapcat #(if (vector? %) % [%]) layer-specs)))))

;; ### Basic Constructors

(defn data
  "Attach data to a layer."
  ([dataset]
   [{:aog/data dataset}])
  ([layers dataset]
   (* layers (data dataset))))

(defn mapping
  "Define aesthetic mappings from data columns to visual properties."
  ([x y]
   [{:aog/x x :aog/y y}])
  ([x y named]
   (if (map? named)
     [(merge {:aog/x x :aog/y y}
             (update-keys named #(keyword "aog" (name %))))]
     (let [layers-or-data x
           x-field y
           y-field named
           layers (if (layers? layers-or-data)
                    layers-or-data
                    (data layers-or-data))]
       (* layers (mapping x-field y-field)))))
  ([first-arg x y named]
   (let [layers (if (layers? first-arg)
                  first-arg
                  (data first-arg))]
     (* layers (mapping x y named)))))

(defn facet
  "Add faceting to a layer specification."
  [layer-spec facet-spec]
  (let [facet-keys (update-keys facet-spec #(keyword "aog" (name %)))]
    (if (vector? layer-spec)
      (mapv #(merge % facet-keys) layer-spec)
      [(merge layer-spec facet-keys)])))

(defn scale
  "Specify scale properties for an aesthetic."
  ([aesthetic opts]
   (let [scale-key (keyword "aog" (str "scale-" (name aesthetic)))]
     [{scale-key opts}]))
  ([layers aesthetic opts]
   (* layers (scale aesthetic opts))))

(defn target
  "Specify the rendering target for layers."
  ([target-kw]
   [{:aog/target target-kw}])
  ([layers target-kw]
   (* layers (target target-kw))))

;; ### Load Datasets

(def penguins (tc/drop-missing (rdatasets/palmerpenguins-penguins)))
(def mtcars (rdatasets/datasets-mtcars))
(def iris (rdatasets/datasets-iris))

;; Dataset previews:

penguins
mtcars
iris

;; ### :geom Target Implementation

(defmethod plot-impl :geom
  [layers opts]
  (let [layers-vec (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)
        facet-groups (organize-by-facets layers-vec)
        row-labels (distinct (map :row-label facet-groups))
        col-labels (distinct (map :col-label facet-groups))
        num-rows (count row-labels)
        num-cols (count col-labels)
        is-faceted? (or (> num-rows 1) (> num-cols 1))
        panel-width (/ width num-cols)
        panel-height (/ height num-rows)
        custom-x-domain (get-scale-domain layers-vec :x)
        custom-y-domain (get-scale-domain layers-vec :y)
        all-transformed-points
        (mapcat (fn [{:keys [layers]}]
                  (mapcat (fn [layer]
                            (let [points (layer->points layer)
                                  transform-result (apply-transform layer points)]
                              (transform->domain-points transform-result)))
                          layers))
                facet-groups)
        x-vals (keep :x all-transformed-points)
        y-vals (keep :y all-transformed-points)
        x-domain (or custom-x-domain (infer-domain x-vals))
        y-domain (or custom-y-domain (infer-domain y-vals))
        valid? (and (vector? x-domain) (vector? y-domain)
                    (every? number? x-domain) (every? number? y-domain))]
    (if-not valid?
      (kind/hiccup
       [:div {:style {:padding "20px"}}
        [:p "Cannot render: non-numeric data or empty dataset"]])
      (let [row-positions (zipmap row-labels (range num-rows))
            col-positions (zipmap col-labels (range num-cols))
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
                               :x-offset x-offset
                               :y-offset y-offset)))
                    facet-groups)
            all-backgrounds (mapv :background panels)
            all-plots (mapv :plot panels)
            all-hist-rects (mapcat :hist-rects panels)
            facet-labels (when is-faceted?
                           (concat
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
            svg-elem (apply svg/svg
                            {:width width :height height}
                            (concat all-backgrounds
                                    all-plots
                                    all-hist-rects
                                    (or facet-labels [])))]
        (kind/html (svg/serialize svg-elem))))))

;; ### :vl Target Implementation

(defmethod plot-impl :vl
  [layers opts]
  (let [layers-vec (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)
        facet-groups (organize-by-facets layers-vec)
        is-faceted? (has-faceting? layers-vec)
        row-var (when is-faceted? (some :aog/row layers-vec))
        col-var (when is-faceted? (some :aog/col layers-vec))
        custom-x-domain (get-scale-domain layers-vec :x)
        custom-y-domain (get-scale-domain layers-vec :y)
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
        layer->vl-mark (fn [layer]
                         (let [plottype (:aog/plottype layer)
                               transform (:aog/transformation layer)]
                           (cond
                             (= transform :linear) "line"
                             (= transform :histogram) "bar"
                             (= plottype :scatter) "circle"
                             (= plottype :line) "line"
                             :else "circle")))
        layer->vl-encoding (fn [layer vl-data]
                             (let [x-col (:aog/x layer)
                                   y-col (:aog/y layer)
                                   color-col (:aog/color layer)
                                   alpha (:aog/alpha layer)
                                   tooltip-fields (cond-> []
                                                    x-col (conj {:field (name x-col) :type "quantitative"})
                                                    y-col (conj {:field (name y-col) :type "quantitative"})
                                                    color-col (conj {:field (name color-col) :type "nominal"}))]
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
                                 alpha (assoc :opacity {:value alpha})
                                 (seq tooltip-fields) (assoc :tooltip tooltip-fields))))
        vl-layers (mapcat (fn [layer]
                            (let [points (layer->points layer)
                                  transform-result (apply-transform layer points)]
                              (case (:type transform-result)
                                :raw
                                [{:mark (layer->vl-mark layer)
                                  :data {:values (layer->vl-data layer)}
                                  :encoding (layer->vl-encoding layer (layer->vl-data layer))}]
                                :regression
                                (let [fitted (:fitted transform-result)
                                      fitted-data (mapv (fn [p]
                                                          {(keyword (name (:aog/x layer))) (:x p)
                                                           (keyword (name (:aog/y layer))) (:y p)})
                                                        fitted)]
                                  [{:mark "line"
                                    :data {:values fitted-data}
                                    :encoding (layer->vl-encoding layer fitted-data)}])
                                :grouped-regression
                                (let [groups (:groups transform-result)
                                      group-col (get-grouping-column layer (ensure-dataset (:aog/data layer)))]
                                  (mapv (fn [[group-val {:keys [fitted]}]]
                                          (when fitted
                                            (let [group-fitted-data (mapv (fn [p]
                                                                            {(keyword (name (:aog/x layer))) (:x p)
                                                                             (keyword (name (:aog/y layer))) (:y p)
                                                                             (keyword (name group-col)) group-val})
                                                                          fitted)]
                                              {:mark "line"
                                               :data {:values group-fitted-data}
                                               :encoding (layer->vl-encoding layer group-fitted-data)})))
                                        groups))
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
                                               :y {:field "count" :type "quantitative"}
                                               :tooltip [{:field "bin-start" :type "quantitative" :title "Min"}
                                                         {:field "bin-end" :type "quantitative" :title "Max"}
                                                         {:field "count" :type "quantitative" :title "Count"}]}}])
                                :grouped-histogram
                                (let [groups (:groups transform-result)
                                      group-col (get-grouping-column layer (ensure-dataset (:aog/data layer)))]
                                  (mapcat (fn [[group-val {:keys [bars]}]]
                                            (when bars
                                              (let [bar-data (mapv (fn [bar]
                                                                     {:bin-start (:x-min bar)
                                                                      :bin-end (:x-max bar)
                                                                      :count (:height bar)
                                                                      (keyword (name group-col)) group-val})
                                                                   bars)]
                                                [{:mark "bar"
                                                  :data {:values bar-data}
                                                  :encoding (merge
                                                             {:x {:field "bin-start"
                                                                  :type "quantitative"
                                                                  :bin {:binned true :step (- (:x-max (first bars)) (:x-min (first bars)))}
                                                                  :axis {:title (name (:aog/x layer))}}
                                                              :x2 {:field "bin-end"}
                                                              :y {:field "count" :type "quantitative"}
                                                              :tooltip [{:field "bin-start" :type "quantitative" :title "Min"}
                                                                        {:field "bin-end" :type "quantitative" :title "Max"}
                                                                        {:field "count" :type "quantitative" :title "Count"}
                                                                        {:field (name group-col) :type "nominal"}]}
                                                             (when group-col
                                                               {:color {:field (name group-col) :type "nominal"}}))}])))
                                          groups)))))
                          layers-vec)
        vl-layers (remove nil? (flatten vl-layers))
        ggplot2-config {:view {:stroke "transparent"}
                        :background ggplot2-background
                        :axis {:gridColor ggplot2-grid
                               :domainColor ggplot2-grid
                               :tickColor ggplot2-grid}
                        :mark {:color ggplot2-default-mark}}
        spec (cond
               is-faceted?
               (let [layers-without-data (mapv #(dissoc % :data) vl-layers)
                     all-data (mapcat layer->vl-data layers-vec)]
                 (cond-> {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                          :data {:values all-data}
                          :width (int (/ width (if col-var 3 1)))
                          :height (int (/ height (if row-var 3 1)))
                          :config ggplot2-config
                          :spec {:layer layers-without-data}}
                   col-var (assoc :facet {:column {:field (name col-var) :type "nominal"}})
                   row-var (assoc-in [:facet :row] {:field (name row-var) :type "nominal"})))
               (> (count vl-layers) 1)
               {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                :width width
                :height height
                :config ggplot2-config
                :layer vl-layers}
               :else
               (merge {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                       :width width
                       :height height
                       :config ggplot2-config}
                      (first vl-layers)))]
    (kind/vega-lite spec)))

;; ### :plotly Target Implementation

(defmethod plot-impl :plotly
  [layers opts]
  (let [layers-vec (if (vector? layers) layers [layers])
        width (or (:width opts) 600)
        height (or (:height opts) 400)
        is-faceted? (has-faceting? layers-vec)
        row-var (when is-faceted? (some :aog/row layers-vec))
        col-var (when is-faceted? (some :aog/col layers-vec))
        custom-x-domain (get-scale-domain layers-vec :x)
        custom-y-domain (get-scale-domain layers-vec :y)
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
        plotly-traces (mapcat (fn [layer]
                                (let [plotly-data (layer->plotly-data layer)
                                      points (layer->points layer)
                                      transform-result (apply-transform layer points)
                                      color-col (:aog/color layer)]
                                  (case (:type transform-result)
                                    :raw
                                    (if color-col
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
                                      [{:type "scatter"
                                        :mode "markers"
                                        :x (:x-vals plotly-data)
                                        :y (:y-vals plotly-data)
                                        :marker {:color ggplot2-default-mark :size 8}
                                        :showlegend false}])
                                    :regression
                                    (let [fitted (:fitted transform-result)]
                                      [{:type "scatter"
                                        :mode "lines"
                                        :x (mapv :x fitted)
                                        :y (mapv :y fitted)
                                        :line {:color ggplot2-default-mark :width 2}
                                        :showlegend false}])
                                    :grouped-regression
                                    (let [groups (:groups transform-result)]
                                      (map-indexed
                                       (fn [idx [group-val {:keys [fitted]}]]
                                         (when fitted
                                           {:type "scatter"
                                            :mode "lines"
                                            :x (mapv :x fitted)
                                            :y (mapv :y fitted)
                                            :name (str group-val " (fit)")
                                            :line {:color (get ggplot2-colors idx ggplot2-default-mark)
                                                   :width 2}
                                            :showlegend false}))
                                       groups))
                                    :histogram
                                    (let [bars (:bars transform-result)]
                                      [{:type "bar"
                                        :x (mapv (fn [b] (clojure.core// (clojure.core/+ (:x-min b) (:x-max b)) 2)) bars)
                                        :y (mapv :height bars)
                                        :width (mapv (fn [b] (clojure.core/- (:x-max b) (:x-min b))) bars)
                                        :marker {:color ggplot2-default-mark
                                                 :line {:color ggplot2-grid :width 1}}
                                        :showlegend false}])
                                    :grouped-histogram
                                    (let [groups (:groups transform-result)]
                                      (map-indexed
                                       (fn [idx [group-val {:keys [bars]}]]
                                         (when bars
                                           {:type "bar"
                                            :x (mapv (fn [b] (clojure.core// (clojure.core/+ (:x-min b) (:x-max b)) 2)) bars)
                                            :y (mapv :height bars)
                                            :width (mapv (fn [b] (clojure.core/- (:x-max b) (:x-min b))) bars)
                                            :name (str group-val)
                                            :marker {:color (get ggplot2-colors idx ggplot2-default-mark)
                                                     :line {:color ggplot2-grid :width 1}}}))
                                       groups)))))
                              layers-vec)
        plotly-traces (remove nil? plotly-traces)
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
        spec (if is-faceted?
               (let [first-layer (first layers-vec)
                     data (:aog/data first-layer)
                     dataset (ensure-dataset data)
                     row-vals (when row-var (sort (distinct (tc/column dataset row-var))))
                     col-vals (when col-var (sort (distinct (tc/column dataset col-var))))
                     num-rows (if row-var (count row-vals) 1)
                     num-cols (if col-var (count col-vals) 1)
                     faceted-traces
                     (for [[row-idx row-val] (map-indexed vector (or row-vals [nil]))
                           [col-idx col-val] (map-indexed vector (or col-vals [nil]))]
                       (let [filtered-data (cond-> dataset
                                             row-var (tc/select-rows #(= (get % row-var) row-val))
                                             col-var (tc/select-rows #(= (get % col-var) col-val)))
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
               {:data plotly-traces
                :layout layout})]
    (kind/plotly spec)))

;; ---
;; # Part II: Features
;;
;; Each feature section contains:
;; 1. Constructor function
;; 2. Compute helper (if needed)
;; 3. Multimethod implementations (defmethod)
;; 4. Examples showing the feature in action

;; ## Feature 4: Scatter Plots
;;
;; The simplest feature: scatter plots render raw data points with no transformation.

;; ### 4.1 Implementation

(defn scatter
  "Create a scatter plot layer."
  ([]
   [{:aog/plottype :scatter}])
  ([attrs-or-layers]
   (if (layers? attrs-or-layers)
     (* attrs-or-layers (scatter))
     [(merge {:aog/plottype :scatter}
             (update-keys attrs-or-layers #(keyword "aog" (name %))))]))
  ([layers attrs]
   (* layers (scatter attrs))))

;; Scatter has no transformation - it's a pass-through:

(defmethod apply-transform nil
  [layer points]
  {:type :raw
   :points points})

(defmethod transform->domain-points :raw
  [transform-result]
  (:points transform-result))

;; Render scatter for :geom target:

(defmethod render-layer [:geom :scatter]
  [target layer transform-result alpha]
  (let [points (:points transform-result)
        color-groups (group-by :color points)]
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

;; ### 4.2 Examples

;; #### Basic Scatter Plot

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter))

;; #### Grouped Scatter (Color Aesthetic)

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (scatter))

;; #### Scatter with Opacity

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (scatter {:alpha 0.5}))

;; #### Plain Clojure Data (No Dataset Required)

(-> {:x [1 2 3 4 5]
     :y [2 4 1 5 3]}
    (mapping :x :y)
    (scatter))

;; ---
;; ## Feature 5: Linear Regression
;;
;; Linear regression fits a line through points. When color aesthetic is present
;; and categorical, it computes separate regressions per group.

;; ### 5.1 Implementation

(defn linear
  "Add linear regression transformation."
  ([]
   [{:aog/transformation :linear
     :aog/plottype :line}])
  ([layers-or-data]
   (let [layers (if (layers? layers-or-data)
                  layers-or-data
                  (data layers-or-data))]
     (* layers (linear)))))

(defn- compute-linear-regression
  "Compute linear regression using fastmath."
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
          [{:x x-min :y (clojure.core/+ intercept (clojure.core/* slope x-min))}
           {:x x-max :y (clojure.core/+ intercept (clojure.core/* slope x-max))}])
        (catch Exception e nil)))))

(defmethod apply-transform :linear
  [layer points]
  (let [has-groups? (some :group points)]
    (if has-groups?
      (let [grouped (group-by :group points)
            group-results (into {}
                                (map (fn [[group-val group-points]]
                                       [group-val
                                        {:fitted (compute-linear-regression group-points)
                                         :points group-points}])
                                     grouped))]
        {:type :grouped-regression
         :points points
         :groups group-results})
      (let [fitted (compute-linear-regression points)]
        {:type :regression
         :points points
         :fitted (or fitted points)}))))

(defmethod transform->domain-points :regression
  [transform-result]
  (:fitted transform-result))

(defmethod transform->domain-points :grouped-regression
  [transform-result]
  (mapcat (fn [{:keys [fitted]}] fitted)
          (vals (:groups transform-result))))

(defmethod render-layer [:geom :linear]
  [target layer transform-result alpha]
  (if (= :grouped-regression (:type transform-result))
    (let [groups (:groups transform-result)
          color-groups (group-by :color (:points transform-result))
          colors (color-scale (keys color-groups))]
      (keep (fn [[group-val {:keys [fitted]}]]
              (when fitted
                {:values (mapv (fn [p] [(:x p) (:y p)]) fitted)
                 :layout viz/svg-line-plot
                 :attribs {:stroke (get colors group-val ggplot2-default-mark)
                           :stroke-width 2
                           :fill "none"
                           :opacity alpha}}))
            groups))
    (when-let [fitted (:fitted transform-result)]
      [{:values (mapv (fn [p] [(:x p) (:y p)]) fitted)
        :layout viz/svg-line-plot
        :attribs {:stroke ggplot2-default-mark
                  :stroke-width 2
                  :fill "none"
                  :opacity alpha}}])))

;; ### 5.2 Examples

;; #### Single Regression Line

(-> mtcars
    (mapping :wt :mpg)
    (linear))

;; #### Scatter + Regression

(-> mtcars
    (mapping :wt :mpg)
    (+ (scatter) (linear)))

;; #### Grouped Regression (One Line Per Group)

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (linear))

;; #### Scatter + Grouped Regression

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (+ (scatter {:alpha 0.5}) (linear)))

;; ---
;; ## Feature 6: Histograms
;;
;; Histograms bin continuous data and count occurrences. Requires domain computation
;; to determine bin edges.

;; ### 6.1 Implementation

(defn histogram
  "Add histogram transformation."
  ([]
   [{:aog/transformation :histogram
     :aog/plottype :bar
     :aog/bins :sturges}])
  ([opts-or-layers]
   (if (layers? opts-or-layers)
     (* opts-or-layers (histogram))
     [(merge {:aog/transformation :histogram
              :aog/plottype :bar
              :aog/bins :sturges}
             (update-keys opts-or-layers #(keyword "aog" (name %))))]))
  ([layers opts]
   (* layers (histogram opts))))

(defn- compute-histogram
  "Compute histogram bins using fastmath.stats/histogram."
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

(defmethod apply-transform :histogram
  [layer points]
  (let [has-groups? (some :group points)]
    (if has-groups?
      (let [grouped (group-by :group points)
            group-results (into {}
                                (map (fn [[group-val group-points]]
                                       [group-val
                                        {:bars (compute-histogram group-points (:aog/bins layer))
                                         :points group-points}])
                                     grouped))]
        {:type :grouped-histogram
         :points points
         :groups group-results})
      (let [bins-method (:aog/bins layer)
            bars (compute-histogram points bins-method)]
        {:type :histogram
         :points points
         :bars bars}))))

(defmethod transform->domain-points :histogram
  [transform-result]
  (mapcat (fn [bar]
            [{:x (:x-min bar) :y 0}
             {:x (:x-max bar) :y (:height bar)}])
          (:bars transform-result)))

(defmethod transform->domain-points :grouped-histogram
  [transform-result]
  (mapcat (fn [{:keys [bars]}]
            (mapcat (fn [bar]
                      [{:x (:x-min bar) :y 0}
                       {:x (:x-max bar) :y (:height bar)}])
                    bars))
          (vals (:groups transform-result))))

(defmethod render-layer [:geom :histogram]
  [target layer transform-result alpha]
  (if (= :grouped-histogram (:type transform-result))
    (let [groups (:groups transform-result)
          color-groups (group-by :color (:points transform-result))
          colors (color-scale (keys color-groups))]
      (mapcat (fn [[group-val {:keys [bars]}]]
                (mapv (fn [bar]
                        {:type :rect
                         :x-min (:x-min bar)
                         :x-max (:x-max bar)
                         :height (:height bar)
                         :attribs {:fill (get colors group-val ggplot2-default-mark)
                                   :stroke ggplot2-grid
                                   :stroke-width 1
                                   :opacity alpha}})
                      bars))
              groups))
    (when-let [bars (:bars transform-result)]
      (mapv (fn [bar]
              {:type :rect
               :x-min (:x-min bar)
               :x-max (:x-max bar)
               :height (:height bar)
               :attribs {:fill ggplot2-default-mark
                         :stroke ggplot2-grid
                         :stroke-width 1
                         :opacity alpha}})
            bars))))

;; ### 6.2 Examples

;; #### Basic Histogram

(-> penguins
    (mapping :bill-length-mm nil)
    (histogram))

;; #### Grouped Histogram (Side-by-Side Bars)

(-> penguins
    (mapping :bill-length-mm nil {:color :species})
    (histogram))

;; #### Custom Bin Count

(-> penguins
    (mapping :bill-length-mm nil)
    (histogram {:bins 20}))

;; ---
;; # Part III: Advanced Topics

;; ## Multi-Layer Composition
;;
;; The `+` operator overlays multiple layers. Combined with `*`, you can
;; factor out common properties.

;; ### Scatter + Regression

(-> mtcars
    (mapping :wt :mpg)
    (+ (scatter {:alpha 0.6})
       (linear)))

;; ### Grouped Multi-Layer

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (+ (scatter {:alpha 0.4})
       (linear)))

;; ### Distributive Property in Action

;; These are equivalent:

;; Compact form (using distributive property):
(-> penguins
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (* (+ (scatter {:alpha 0.5}) (linear))))

;; Explicit form:
(+ (* penguins
      (mapping :bill-length-mm :bill-depth-mm {:color :species})
      (scatter {:alpha 0.5}))
   (* penguins
      (mapping :bill-length-mm :bill-depth-mm {:color :species})
      (linear)))

;; ## Faceting
;;
;; Create small multiples by splitting data across categorical variables.

;; ### Column Faceting

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)
    (facet {:col :species}))

;; ### Row Faceting

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)
    (facet {:row :species}))

;; ### Grid Faceting (Row × Column)

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)
    (facet {:row :sex :col :species}))

;; ### Faceted with Multiple Layers

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm)
    (+ (scatter {:alpha 0.5}) (linear))
    (facet {:col :species}))

;; ## Multiple Rendering Targets
;;
;; The same layer specification can be rendered by different targets.

;; ### :geom (Default - Static SVG)

(-> mtcars
    (mapping :wt :mpg)
    (+ (scatter) (linear)))

;; ### :vl (Vega-Lite - Interactive)

(-> mtcars
    (mapping :wt :mpg)
    (+ (scatter) (linear))
    (target :vl))

;; ### :plotly (Plotly.js - Rich Interactivity)

(-> mtcars
    (mapping :wt :mpg)
    (+ (scatter) (linear))
    (target :plotly))

;; ### Target Comparison: Same Spec, Different Outputs

(def my-spec
  (-> penguins
      (mapping :bill-length-mm :bill-depth-mm {:color :species})
      (+ (scatter {:alpha 0.5}) (linear))))

;; View as :geom:
my-spec

;; View as :vl:
(* my-spec (target :vl))

;; View as :plotly:
(* my-spec (target :plotly))

;; ## Custom Scale Domains

;; ### Fixed X Domain

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm)
    (scatter)
    (scale :x {:domain [30 65]}))

;; ### Fixed X and Y Domains

(-> penguins
    (mapping :bill-length-mm :bill-depth-mm {:color :species})
    (scatter)
    (scale :x {:domain [30 65]})
    (scale :y {:domain [12 23]}))

;; ---
;; # Part IV: Reflection

;; ## Design Trade-offs
;;
;; **What We Gained**:
;; - Flat structure → standard `merge` works
;; - Namespaced keys → no collision with data columns
;; - Multimethod extensibility → add features incrementally
;; - Three rendering targets → same spec, different outputs
;; - Plain data support → no dataset conversion required
;; - Vertical presentation → learn one feature at a time
;;
;; **What We Paid**:
;; - Namespace verbosity (`:aog/color` vs `:color`)
;; - Novel operators (`*`, `+` shadow arithmetic)
;; - Incomplete feature set (many plot types not implemented)
;;
;; **Key Insight**: Multimethod architecture enables pedagogical reorganization.
;; Each feature can be presented completely (impl → examples) before moving to
;; the next, making the design easier to learn and understand incrementally.

;; ## Implementation Status
;;
;; **Complete**:
;; - ✅ Scatter plots (basic and grouped)
;; - ✅ Linear regression (basic and grouped)
;; - ✅ Histograms (basic and grouped)
;; - ✅ Faceting (row, column, grid)
;; - ✅ Three rendering targets (:geom, :vl, :plotly)
;; - ✅ Custom scale domains
;; - ✅ Plain Clojure data support
;; - ✅ Type-aware grouping
;;
;; **Not Implemented** (compared to tableplot.v1):
;; - ⚠️ Other plot types: line, bar, box, violin, density, smooth
;; - ⚠️ Additional aesthetics: size, shape, opacity
;; - ⚠️ Statistical transforms: density, smooth (loess), correlation
;; - ⚠️ Coordinate systems: polar, 3D, geographic
;; - ⚠️ Advanced layouts: subplots, secondary axes
;;
;; **Design Philosophy**: Focus on compositional clarity over feature completeness.
;; Missing features can be added incrementally while maintaining the algebraic design.

;; ## Integration Path
;;
;; This design exploration could:
;; 1. Coexist as `scicloj.tableplot.v2.aog` (additional namespace)
;; 2. Inform future Tableplot design decisions
;; 3. Provide a third compositional option alongside ggplot2-style and current AoG
;;
;; Not intended to replace existing APIs, but to explore alternative approaches
;; and gather feedback from the community.

;; ## Summary
;;
;; We've built a complete working prototype of a compositional graphics API using:
;; - Flat maps with namespaced keys for transparent data structures
;; - Two operators (`*`, `+`) for algebraic composition
;; - Multimethods for extensibility and pedagogical presentation
;; - Three rendering targets sharing the same layer specification
;;
;; The reorganization (enabled by multimethods) presents each feature vertically
;; (implementation directly followed by examples), making the design easier to
;; learn one feature at a time rather than requiring readers to jump between
;; distant sections.
;;
;; **Key Takeaway**: Multimethod architecture enables both extensibility AND
;; pedagogical clarity—features can be added anywhere AND presented in any order.
