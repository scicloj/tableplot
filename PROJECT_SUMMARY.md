# Tableplot Project Summary

## Overview

Tableplot is a Clojure data visualization library inspired by ggplot2's layered grammar of graphics. It provides easy layered graphics by composing Hanami templates with Tablecloth datasets. The library enables creation of interactive visualizations that work with any tool supporting the Kindly data visualization standard, such as Clay and Clojupyter.

**Current Version:** 1-beta14  
**Maven Coordinates:** `org.scicloj/tableplot`  
**License:** Eclipse Public License v2.0

## Key Features

- Layered grammar of graphics similar to ggplot2
- Integration with Tablecloth for data processing
- Support for Plotly.js, Vega-Lite, and thi.ng/geom backends
- Kindly-compatible visualizations
- Composable plotting functions with pipeline-friendly API
- Statistical transformations (smoothing, histograms, density plots)
- Multiple plot types (scatter, line, bar, heatmap, 3D surfaces, etc.)
- **Dataflow system with term rewriting and explicit dependency tracking**
- **Native polar coordinate support** via thi.ng/geom backend

## Project Structure

### Source Code Organization

```
src/scicloj/tableplot/v1/
├── plotly.clj          # Main Plotly backend implementation (~90 substitution keys)
├── hanami.clj          # Vega-Lite backend via Hanami
├── transpile.clj       # Cross-backend transpilation
├── dag.clj             # Dependency-aware function system (Rule 7: explicit dependencies)
├── xform.clj           # Term rewriting engine (Rules 1-6: substitution semantics)
├── cache.clj           # Memoization layer for expensive computations
├── util.clj            # Utility functions
├── palette.clj         # Color/size/symbol palette management
└── aog/                # AlgebraOfGraphics-style API (production-ready)
    ├── core.clj        # Algebraic layer composition (* and + operators)
    ├── ir.clj          # Intermediate representation with Malli schemas
    ├── processing.clj  # Layer → ProcessedLayer → Entry pipeline
    ├── transforms.clj  # Statistical transformations (linear, smooth, density, histogram)
    ├── scales.clj      # Automatic scale inference (categorical & continuous)
    ├── plotly.clj      # Plotly.js backend for AoG
    ├── vegalite.clj    # Vega-Lite backend with faceting support
    └── thing_geom.clj  # thi.ng/geom backend with native polar coordinates
```

### Documentation and Examples

```
docs/                   # Quarto-generated documentation
notebooks/tableplot_book/  # Example notebooks and tutorials
├── plotly_walkthrough.clj            # Plotly API examples
├── hanami_walkthrough.clj            # Hanami/Vega-Lite examples
├── plotly_reference.clj              # Complete Plotly reference
├── transpile_reference.clj           # Cross-backend examples
├── dataflow_walkthrough.clj          # Complete dataflow system documentation
├── dataflow_design_analysis.md       # Deep analysis for future GG systems
├── aog_demo.clj                      # AlgebraOfGraphics API examples (20+ plots)
├── rdatasets_examples.clj            # Real-world dataset examples with faceting
└── aog_thing_geom_architecture.clj   # thi.ng/geom backend deep-dive (10 interactive parts)
AOG_THING_GEOM_REFERENCE.md  # Comprehensive 4-layer architecture guide (28K)
```

## Core Dependencies

### Required Dependencies
- **tablecloth** (7.029.2) - Dataset manipulation and processing
- **aerial.hanami** (0.20.1) - Vega-Lite template system (inspiration for xform)
- **metamorph.ml** (1.2) - Machine learning pipeline integration
- **fastmath** (3.0.0-alpha3) - Mathematical operations and statistics
- **kindly** (4-beta16) - Visualization standard compliance
- **tempfiles** (1-beta1) - Temporary file management
- **std.lang** (4.0.10) - Language utilities
- **com.rpl/specter** - Recursive tree transformation in xform
- **thi.ng/geom** (1.0.1) - Pure Clojure SVG generation with native polar coordinates

### Development Dependencies
- **noj** (2-beta18) - Data science stack (dev/test)
- **test.check** (1.1.1) - Property-based testing
- **test-runner** - Test execution
- **nrepl** (1.3.1) - REPL server

## API Overview

### Main Namespaces

#### `scicloj.tableplot.v1.plotly`
The primary Plotly backend providing the richest feature set.

**Key Functions:**
```clojure
;; Base plot creation
(plotly/base dataset options)
(plotly/plot template)

;; Layer functions
(plotly/layer-point {:=x :col1 :=y :col2 :=color :col3})
(plotly/layer-line {:=x :time :=y :value})
(plotly/layer-bar {:=x :category :=y :count})
(plotly/layer-histogram {:=x :values :=bins 20})
(plotly/layer-smooth {:=x :x :=y :y :=method :loess})

;; Specialized plots
(plotly/splom dataset {:=columns [:col1 :col2 :col3]})
(plotly/surface matrix)
(plotly/imshow image-data)
```

#### `scicloj.tableplot.v1.hanami`
Vega-Lite backend for web-standard visualizations.

**Key Functions:**
```clojure
(hanami/base dataset options)
(hanami/layer-point {:=x :col1 :=y :col2})
(hanami/layer-line {:=x :time :=y :value})
(hanami/facet context facet-config)
```

#### `scicloj.tableplot.v1.xform`
Core term rewriting engine (direct port of Hanami's xform with dataset pass-through).

**Key Function:**
```clojure
;; Apply substitution rules to template with environment
(xform/xform template env)
(xform/xform template :key1 val1 :key2 val2)

;; Example:
(xform/xform
  {:x :X, :y :Y}
  {:X :year, :Y :temperature})
;; => {:x :year, :y :temperature}
```

#### `scicloj.tableplot.v1.dag`
Explicit dependency tracking (Rule 7 addition to Hanami's model).

**Key Macros:**
```clojure
;; Define function with declared dependencies
(dag/defn-with-deps area->radius
  "Compute radius from area"
  [Area]  ; dependency list
  (Math/sqrt (/ Area Math/PI)))

;; Anonymous function with dependencies
(dag/fn-with-deps nil [X Y] (+ X Y))

;; Metadata inspection
(:scicloj.tableplot.v1.dag/dep-ks (meta area->radius))
;; => [:Area]
```

#### `scicloj.tableplot.v1.cache`
Memoization for expensive operations.

**Key Macro:**
```clojure
;; Execute with fresh cache
(cache/with-clean-cache
  (xform/xform template env))
```

### Common Usage Patterns

#### Basic Plotting Pipeline
```clojure
(-> dataset
    (tc/select-columns [:x :y :category])
    (plotly/layer-point {:=x :x :=y :y :=color :category}))
```

#### Layered Plots
```clojure
(-> dataset
    (plotly/layer-point {:=x :x :=y :y})
    (plotly/layer-smooth {:=x :x :=y :y :=method :loess}))
```

#### Statistical Transformations
```clojure
(-> dataset
    (plotly/layer-histogram {:=x :values :=bins 30})
    (plotly/layer-density {:=x :values :=alpha 0.7}))
```

## Architecture

### The Dataflow System: Term Rewriting with Explicit Dependencies

Tableplot's core innovation is a **two-layer dataflow system**:

#### Layer 1: Six Core Substitution Rules (from Hanami)

Given a term `T` and an environment `E`:

1. **Lookup** — If `T` is a key in `E`, replace with `E[T]` and recurse
2. **Identity** — If `T` is a key not in `E`, it's a fixpoint (stop)
3. **Function Application** — If `T` is a function, apply to `E` and recurse
4. **Collection Recursion** — If `T` is a map/vector, recurse on elements
5. **Template Defaults** — `::ht/defaults` merges into environment for nested scope
6. **Value Pass-through** — Primitives, datasets, etc. pass through unchanged

**Implementation**: `xform.clj` using Specter for recursive transformation

**Example**:
```clojure
(xform/xform
  {:a :B, :c :D}
  {:B :C, :C 10, :D 20})
;; Reduction: :a → :B → :C → 10 (fixpoint)
;; Result: {:a 10, :c 20}
```

#### Layer 2: Explicit Dependencies (Tableplot Addition)

**Rule 7: Declared Dependencies** — Functions declare parameter requirements via metadata

**Implementation**: `dag.clj` provides `defn-with-deps` macro

**Example**:
```clojure
(dag/defn-with-deps smooth-stat
  "Fit regression model"
  [=dataset =x =y =group]  ; Explicit dependencies
  (if =group
    (group-and-fit =dataset =group =x =y)
    (fit-model =dataset =x =y)))

;; The system:
;; 1. Sees :=stat → smooth-stat
;; 2. Reads metadata: needs [=dataset =x =y =group]
;; 3. Resolves each dependency (with caching)
;; 4. Calls function with resolved values
;; 5. Caches result
```

#### Why This Solves Grammar-of-Graphics Problems

**Parameter Threading**: Keys like `:=x`, `:=y`, `:=dataset` flow automatically through substitution—no manual passing needed.

**Statistical Pipelines**: Stats declare dependencies, compute once, cache results. Grouping variables respected automatically.

**Layered Composition**: Each layer inherits base environment via nested `::ht/defaults`, adds local parameters.

**Hierarchical Scoping**: Nested defaults create local contexts (e.g., facet-specific overrides).

### Dependency-Aware Function System (DAG)

The library uses a sophisticated dependency system defined in `dag.clj`:

- `defn-with-deps` macro creates functions that declare their data dependencies
- Automatic caching of intermediate computations via `cache/cached-xform-k`
- Lazy evaluation of expensive operations
- Dependency resolution for complex visualization pipelines
- **Metadata-based introspection**: Query function dependencies before execution

### Data Flow Architecture

1. **Input Dataset** (Tablecloth/tech.ml.dataset)
2. **Layer Functions** create templates with `::ht/defaults` (environment)
3. **Base Function** merges ~90 standard substitution keys
4. **Template Composition** via pipeline (`->`) or direct merge
5. **Statistical Computations** (optional) - declare dependencies, cache results
6. **Term Rewriting** (`xform/xform`) resolves all substitution keys to fixpoint
7. **Trace Generation** (`submap->traces`) converts layers to Plotly.js/Vega-Lite format
8. **Kindly Metadata** added for tool integration
9. **Visualization Output** (JSON specifications)

**Data Pipeline Pattern** (in Plotly backend):
```
=dataset → =stat → =x-after-stat → layer :x → trace :x
          ↓
    (statistical transformation with caching)
```

### Cross-Backend Compatibility

The `transpile.clj` namespace enables cross-backend functionality:
- Convert between Plotly and Vega-Lite specifications
- Maintain feature parity across backends
- Backend-specific optimizations

### Key Implementation Details

**Fixpoint Computation**:
- `xform` applies rules recursively until no changes occur
- Self-reference (`:X → :X`) detected as fixpoint
- Missing keys also fixpoints (`:Y` not in environment → `:Y`)

**Recursive Removal (`hc/RMV`)**:
- Sentinel value for optional parameters
- Collections containing only `RMV` removed recursively
- Enables clean output without unused keys

**Caching Strategy**:
- Cache key: `[substitution-key entire-environment]`
- Same key + different environment → separate cache entries
- Multiple references to same key in same environment → computed once
- Scoped to `with-clean-cache` blocks

**Type Inference**:
- Inspect dataset column metadata (`:categorical-data`)
- Check column types (`:numerical` vs others)
- Infer `:nominal` or `:quantitative` for aesthetic mappings

**Grouping**:
- Categorical aesthetics (`:=color`, `:=size`, `:=symbol`) create groups
- Stats compute separately per group
- Palette assignment cached for consistency

## Implementation Patterns

### Aesthetic Mappings
- Keys prefixed with `=` (e.g., `:=x`, `:=y`, `:=color`)
- Automatic type inference from dataset columns
- Support for continuous (`:quantitative`) and categorical (`:nominal`) mappings

### Template System (Substitution Keys)

**Standard defaults** (~90 keys in `plotly.clj`):
```clojure
[:=dataset hc/RMV "The data to be plotted"]
[:=x :x "The column for the x axis"]
[:=y :y "The column for the y axis"]
[:=color hc/RMV "The column to determine color"]
[:=stat :=dataset "Data after statistical transformation"]
[:=x-after-stat :=x "X column after stat"]
[:=inferred-group submap->group "Inferred from aesthetics"]
[:=traces submap->traces "Final Plotly.js traces"]
;; ... ~80 more
```

**Key categories**:
- **Data pipeline**: `=dataset`, `=stat`, `=x-after-stat`
- **Aesthetics**: `=x`, `=y`, `=color`, `=size`, `=symbol`
- **Types**: `=x-type`, `=color-type` (inferred)
- **Grouping**: `=inferred-group`, `=group`
- **Output**: `=traces`, `=layout`, `=layers`

### Template Composition Patterns

**Nested Defaults** (hierarchical environments):
```clojure
{:title :Title
 :section {:heading :Heading
           ::ht/defaults {:Heading "Default Heading"}}
 ::ht/defaults {:Title "Default Title"}}

;; Inner scope has access to both defaults
;; User substitutions override nested defaults
```

**Layer Composition**:
```clojure
(defn layer [dataset-or-template layer-spec submap]
  (-> template
      (update-in [::ht/defaults :=layers]
                 (fnil conj [])
                 (update layer-spec ::ht/defaults merge submap))))

;; Each layer: inherits base environment + adds local parameters
```

**Conditional Inclusion**:
```clojure
;; Function can return RMV to remove parameter
{:subtitle :Subtitle
 ::ht/defaults
 {:ShowSubtitle false
  :Subtitle (fn [{:keys [ShowSubtitle]}]
              (if ShowSubtitle "A subtitle" hc/RMV))}}
;; Result: {:subtitle} removed
```

### Caching Strategy

- Intermediate results cached by `[key environment]` pair
- Statistical computations cached separately via `dag/cached-xform-k`
- Cache invalidation via `cache/with-clean-cache` macro
- Palette assignments cached for visual consistency

## Development Workflow

### Setup
```bash
# Clone and enter directory
cd tableplot

# Start REPL with dev dependencies
clj -M:dev

# Start REPL with dev dependencies and Clojure-MCP support
clj -M:dev:nrepl

# Run tests
clj -X:test

# Build documentation
# (requires Quarto installation)
```

### Testing

#### Unit Testing with Clay Test Generation
The project uses Clay's `kind/test-last` mechanism for generating unit tests from notebooks:

```clojure
;; In notebook files (e.g., plotly_walkthrough.clj, plotly_reference.clj)
(-> dataset
    (plotly/layer-point {:=x :x :=y :y}))

;; Add test annotation - must contain exactly ONE predicate
(kind/test-last [#(= (-> % plotly/plot :data first :type) "scatter")])
```

**Test Pattern Best Practices:**
1. **Single predicate per test**: Each `kind/test-last` takes ONE predicate function
2. **REPL verification**: Always test predicates in REPL before adding to file
3. **Template-level tests**: Check `::ht/defaults` for mappings and settings
4. **Spec-level tests**: Use `plotly/plot` to realize and test final Plotly.js spec
5. **Layer-specific tests**: Navigate to `(-> template ::ht/defaults :=layers first ::ht/defaults)`

**Test Coverage (as of current session):**
- `plotly_reference.clj`: 67 tests (36 original + 31 new spec-level tests)
- `plotly_walkthrough.clj`: 24 tests covering key examples and patterns
- `hanami_walkthrough.clj`: 19 tests covering Hanami/Vega-Lite API
- **Total: 110 comprehensive API tests**

**Common Test Patterns:**

*Template-level tests* (check configuration before realization):
```clojure
;; Template has correct dataset
(kind/test-last [#(contains? (:aerial.hanami.templates/defaults %) :=dataset)])

;; Layer has correct mappings
(kind/test-last [#(let [layer-defaults (-> % :aerial.hanami.templates/defaults :=layers first :aerial.hanami.templates/defaults)]
                    (and (= (:=x layer-defaults) :sepal-width)
                         (= (:=y layer-defaults) :sepal-length)))])
```

*Spec-level tests* (check realized Plotly.js/Vega-Lite output):
```clojure
;; Plotly: Check trace type in realized spec
(kind/test-last [#(= (-> % plotly/plot :data first :type) "scatter")])

;; Plotly: Check marker properties appear in spec
(kind/test-last [#(= (-> % plotly/plot :data first :marker :size) 20)])

;; Plotly: Check aesthetic mappings create expected structures
(kind/test-last [#(vector? (-> % plotly/plot :data first :marker :color))])

;; Vega-Lite: Check encoding types
(kind/test-last [#(= (-> % hanami/plot :encoding :x :type) :quantitative)])

;; Check multiple traces from grouping
(kind/test-last [#(= (-> % plotly/plot :data count) 3)])
```

**Important Testing Notes:**
- Use `:aerial.hanami.templates/defaults` not `::ht/defaults` (namespace-qualified keywords don't work in test generation context)
- **Prefer spec-level tests** over template-level tests - they verify actual output
- For Plotly: opacity is at trace level (`:opacity`), not marker level
- For Plotly: z-data in heatmaps/surfaces can be lazy seqs, use `seq?` not `vector?`
- For Plotly: SPLOM dimensions are seqs, not vectors
- For Vega-Lite: encoding types are keywords (`:quantitative`), not strings

**Running Tests:**
```bash
# Run all tests
clj -T:build test

# Run specific test namespaces
clj -M:test -n test.namespace

# Generate tests from notebooks using Clay
# (Tests are auto-generated when rendering notebooks with Clay)
```

### Building and Deployment
```bash
# CI pipeline (test + build JAR)
clj -T:build ci

# Deploy to Clojars
clj -T:build deploy
```

### Documentation Generation
- Documentation built with Quarto from `.qmd` files
- Notebooks in `notebooks/tableplot_book/` provide examples
- Generated docs deployed to GitHub Pages

## Extension Points

### Adding New Plot Types
1. Define layer function in appropriate backend namespace
2. Implement aesthetic mapping logic
3. Add statistical transformation if needed (using `dag/defn-with-deps`)
4. Update documentation and examples

**Pattern**:
```clojure
(defn layer-new-type
  [dataset-or-template submap]
  (layer dataset-or-template
         layer-base
         (merge {:=mark :new-type
                 :=stat default-stat}
                submap)))
```

### Custom Statistical Transformations

**Pattern**:
```clojure
(dag/defn-with-deps my-stat
  "Documentation"
  [=dataset =x =y =group]  ; Declare dependencies
  (if =group
    (-> =dataset
        (tc/group-by =group)
        (tc/aggregate transform-fn))
    (transform-fn =dataset)))

;; Use in layer:
{:=stat my-stat}
```

### New Backend Support
1. Create new namespace following `plotly.clj` pattern
2. Implement core functions: `base`, `plot`, layer functions
3. Define substitution keys (inspired by `standard-defaults`)
4. Implement trace/spec generation function (like `submap->traces`)
5. Add transpilation support in `transpile.clj`
6. Update cross-backend tests

### Integration Points
- **Kindly**: Add new visualization kinds
- **Tablecloth**: Extend dataset processing pipeline
- **Hanami**: Create new template types
- **Clay/Clojupyter**: Enhance notebook integration

## Common Development Tasks

### Adding a New Layer Type
```clojure
(defn layer-new-type [& {:as options}]
  (plotly/layer {:=mark :new-type} options))
```

### Creating Custom Statistical Functions
```clojure
(dag/defn-with-deps custom-stat
  "Custom statistical transformation"
  [=dataset =x =y]  ; dependencies
  ;; transformation logic
  (tc/add-column =dataset :computed-col computed-values))
```

### Cross-Backend Testing
```clojure
;; Test same visualization across backends
(def viz-spec {:=x :col1 :=y :col2})
(plotly/layer-point viz-spec)
(hanami/layer-point viz-spec)
```

### Debugging Substitution Chains

**Check template before realization**:
```clojure
(def template
  (-> dataset
      (plotly/base {:=x :year})
      (plotly/layer-point {})))

;; Inspect defaults
(::ht/defaults template)

;; Check layer configuration
(-> template ::ht/defaults :=layers first ::ht/defaults)
```

**Inspect function dependencies**:
```clojure
(:scicloj.tableplot.v1.dag/dep-ks (meta smooth-stat))
;; => [:=dataset :=x :=y :=group]
```

**Test xform directly**:
```clojure
(require '[scicloj.tableplot.v1.xform :as xform])
(require '[scicloj.tableplot.v1.cache :as cache])

(cache/with-clean-cache
  (xform/xform
    {:result :X}
    {:X :Y, :Y 10}))
;; => {:result 10}
```

## Design Principles and Patterns

### Comparison to Other Systems

| Feature | Hanami | Tableplot |
|---------|--------|-----------|
| **Substitution** | Pure (Rules 1-6) | Pure + explicit deps (Rule 7) |
| **Dependencies** | Implicit (via fixpoint) | Explicit (`dag/fn-with-deps`) |
| **Caching** | None | Built-in memoization |
| **Introspection** | Must run xform | Metadata inspection |
| **Type Dispatch** | Yes (`subkeyfns`) | No (explicit functions) |
| **Target** | Vega-Lite | Plotly.js (+ Vega-Lite) |

### Why Explicit Dependencies?

1. **Introspection**: Query "what does this need?" without running
2. **Caching**: Expensive stats computed once, cached automatically
3. **Debugging**: Clear dependency graph, easier to trace errors
4. **Tooling**: Can generate dependency visualizations

### Mathematical Foundation

The six rules form a **small-step operational semantics** for term rewriting:

```
T, E → T'  (term T in environment E reduces to term T')
```

The system computes the **transitive closure**: reduce until reaching a fixpoint where no rules apply.

Connections to formal systems:
- **Lambda calculus**: Function application and substitution
- **Rewriting systems**: Confluence and termination properties
- **Fixed-point theory**: Iterative approximation of solutions

### Limitations and Future Work

**Current Limitations**:
1. No cycle detection (`:A → :B → :A` causes stack overflow)
2. Cryptic error messages when substitution fails
3. Difficult to debug deep substitution chains
4. No faceting yet (small multiples)
5. Cache scope management could be more flexible

**Future Directions** (see `dataflow_design_analysis.md`):
1. Implement faceting (`facet-wrap`, `facet-grid`)
2. Add validation layer (catch typos, missing columns early)
3. Improve error messages (track substitution path)
4. Build dependency graph visualizer
5. Consider separating concerns (substitution / dependencies / caching)

## AlgebraOfGraphics API (Beta)

### Overview

A new experimental API inspired by Julia's AlgebraOfGraphics.jl, providing algebraic composition of visualizations through multiplication (`*`) and addition (`+`) operators.

**Status**: Beta implementation with core features working, visual validation pending

**Namespace**: `scicloj.tableplot.v1.aog.*`

**Supported Backends**:
- **Plotly.js** - Interactive web visualizations
- **Vega-Lite** - Declarative visualizations with faceting support
- **thi.ng/geom** - Pure Clojure SVG generation with native polar coordinate support

### Core Concepts

**Algebraic Operations**:
- `*` (multiplication) - Merge layer specifications (Cartesian product)
- `+` (addition) - Overlay multiple layers

**Basic Usage**:
```clojure
(require '[scicloj.tableplot.v1.aog.core :as aog])

;; Simple scatter plot
(aog/draw
  (aog/* (aog/data dataset)
         (aog/mapping :x :y)
         (aog/scatter {:alpha 0.7})))

;; Colored by categorical variable
(aog/draw
  (aog/* (aog/data dataset)
         (aog/mapping :x :y {:color :species})
         (aog/scatter {:alpha 0.6})))

;; Overlay scatter + linear regression
(aog/draw
  (aog/* (aog/data dataset)
         (aog/mapping :x :y {:color :species})
         (aog/+ (aog/scatter {:alpha 0.5})
                (aog/linear))))

;; Faceting (small multiples)
(aog/draw
  (aog/* (aog/data iris)
         (aog/mapping :sepal-length :sepal-width {:col :species})
         (aog/scatter)))
```

### Architecture

**Pipeline**: Layer → ProcessedLayer → Entry → Vega-Lite/Plotly.js/SVG

**Four-Layer Architecture**:
1. **Layer 1 (User API)**: Data + mappings + transformation + plottype
2. **Layer 2 (IR Schemas)**: 7 Malli schemas - Layer, ProcessedLayer, Entry, AxisConfig, CategoricalScale, ContinuousScale, AxisEntries
3. **Layer 3 (Processing)**: `layer->processed-layer`, `processed-layer->entry`, statistical transforms
4. **Layer 4 (Backend Rendering)**: Vega-Lite/Plotly.js/thi.ng/geom SVG generation

**Intermediate Representation (Entry)**:
The Entry IR is backend-agnostic, containing:
- `:x-data`, `:y-data` - Resolved data vectors
- `:x-scale`, `:y-scale` - Scale configurations (categorical/continuous)
- `:axes` - Axis specifications with domains/ranges
- `:plottype` - Visual mark type
- `:attributes` - Styling (colors, sizes, opacity, etc.)
- `:coord-system` - `:cartesian` or `:polar` (native polar support via thi.ng/geom)

**Key Features**:
- ✅ **8 core plot types** - scatter, line, bar, histogram, density, box, violin, heatmap
- ✅ **6 statistical transformations** - linear, smooth, density, histogram, frequency, expectation
- ✅ **4 scale transformations** - log, sqrt, power, custom domains
- ✅ Grouped transformations (linear, smooth respect categorical aesthetics)
- ✅ Automatic scale inference (categorical colors, continuous sizes)
- ✅ Multiple data formats (maps, vectors, tablecloth datasets)
- ✅ Malli schema validation throughout pipeline
- ✅ **Faceting (small multiples)** - column, row, and wrapped layouts
- ✅ **Vega-Lite backend** - production-ready for web-standard visualizations
- ✅ Color consistency across facets (automatic shared scales)
- ✅ **Theme system** - 3 polished variants (subtle, balanced, bold)

### Available Functions

**Layer Construction**:
```clojure
(aog/data dataset)           ; Attach data
(aog/mapping :x :y {:color :species})  ; Specify aesthetics
(aog/mapping :x :y {:col :island})     ; Faceting aesthetic
(aog/scatter {:alpha 0.5})   ; Scatter plot
(aog/line {:width 2})        ; Line plot
(aog/bar)                    ; Bar plot
(aog/boxplot)                ; Box plot
(aog/violin)                 ; Violin plot
(aog/heatmap)                ; Heatmap (2D grid with color encoding)
```

**Faceting Aesthetics**:
```clojure
{:col :category}    ; Horizontal facets (columns)
{:row :category}    ; Vertical facets (rows)
{:facet :category}  ; Wrapped facets (automatic grid)
```

**Statistical Transformations**:
```clojure
(aog/linear)                 ; Linear regression (grouped by aesthetics)
(aog/smooth)                 ; LOESS smoothing
(aog/density)                ; Kernel density estimation
(aog/histogram {:bins 20})   ; Histogram with configurable bins
(aog/frequency)              ; Count aggregation (frequency table)
(aog/expectation)            ; Conditional mean (E[Y|X])
```

**Scale Transformations**:
```clojure
(aog/log-scale :y)                        ; Logarithmic scale (default base 10)
(aog/log-scale :y {:base 2})              ; Log scale with custom base
(aog/sqrt-scale :y)                       ; Square root scale
(aog/pow-scale :y {:exponent 3})          ; Power scale with custom exponent
(aog/scale-domain :x [0 10])              ; Custom axis domain/range

;; Multiple scales on different axes
(aog/* (aog/data dataset)
       (aog/mapping :x :y)
       (aog/log-scale :x)
       (aog/sqrt-scale :y)
       (aog/line))
```

**Composition**:
```clojure
(aog/* layer1 layer2)        ; Merge specifications
(aog/+ layer1 layer2)        ; Overlay on same plot
(aog/draw layers)            ; Render to Plotly.js
```

### Implementation Details

**Grouping/Split-Apply-Combine**:
- Transformations identify categorical aesthetics (`:color`, `:shape`, `:linetype`)
- Data split by groups using `tc/group-by`
- Transformations applied per group
- Results combined with group columns preserved

**Scale Inference**:
- Categorical scales: domain extracted from unique values, mapped to color palette
- Continuous scales: domain from min/max, mapped to visual ranges
- Default Plotly/D3 color palette for categorical variables

**Vega-Lite Generation**:
- Automatic encoding channel mapping (`:x`, `:y`, `:color`, `:size`)
- Faceting support via `:column`, `:row`, `:facet` encoding channels
- Automatic sizing for faceted plots (`width: 200`, `height: 200`)
- Autosize configuration for proper rendering (`{type: "fit", contains: "padding"}`)
- Type inference for all channels (`:quantitative`, `:nominal`, `:ordinal`)
- Scatter plots: colors in `:color` encoding
- Line plots: colors in `:color` encoding
- Implicit y-columns for density (`:density`) and histogram (`:count`)
- Scale configuration with transformations:
  - Default: `{zero: false}` to prevent forcing origin
  - Log scale: `{type: "log", base: 10, nice: true}`
  - Sqrt scale: `{type: "sqrt", nice: true}`
  - Power scale: `{type: "pow", exponent: N, nice: true}`
  - Custom domain: `{domain: [min max]}`

**Malli Validation**:
- All IR structures validated with schemas
- Clear error messages on validation failure
- Schema inspection available via `ir/schemas`

### Known Limitations

1. **Clay rendering issue**: Faceted Vega-Lite specs generate correctly but only show one facet in Clay notebooks (appears to be Clay container sizing bug). Specs render correctly in standalone HTML with vega-embed.
2. **No legend generation testing**: Relying on Vega-Lite/Plotly.js defaults
3. **Plotly backend lacks faceting**: Faceting only implemented for Vega-Lite backend
4. **Color scale transformations**: Scale transformations only apply to positional axes (x/y), not color encoding

### Next Steps for AoG

**Completed**:
- ✅ All 8 core plot types (scatter, line, bar, histogram, density, box, violin, heatmap)
- ✅ All 6 statistical transformations (linear, smooth, density, histogram, frequency, expectation)
- ✅ All 4 scale transformations (log, sqrt, power, custom domains)
- ✅ Faceting implementation (column, row, wrapped layouts)
- ✅ Real-world dataset testing (iris, penguins, diamonds, flights, gapminder)
- ✅ Color consistency across facets (automatic shared scales)
- ✅ Vega-Lite backend with proper sizing and autosize
- ✅ Theme system with 3 polished variants
- ✅ Comprehensive examples notebook (`aog_plot_types.clj`, `rdatasets_examples.clj`)

**Immediate (next session)**:
- Fix Clay rendering issue for faceted plots (container sizing)
- Test faceting with Plotly backend
- Visual validation of all faceting examples in browser

**Future enhancements**:
- Custom color palettes
- Color scale transformations (log/sqrt scales for color encoding)
- Better data transformation helpers (renamer, sorter, etc.)
- Legend customization
- Axis customization (labels, limits, ticks)
- Integration with main Tableplot API

**Reference**: 
- `notebooks/tableplot_book/aog_plot_types.clj` - All 8 plot types + scale transformations + multi-layer examples
- `notebooks/tableplot_book/aog_demo.clj` - 20+ working examples
- `notebooks/tableplot_book/rdatasets_examples.clj` - Real-world datasets with faceting
- `FACETING_IMPLEMENTATION.md` - Complete faceting implementation details
- `STATISTICAL_TRANSFORMS_COMPLETE.md` - Statistical transformation documentation
- `SCALE_TRANSFORMATIONS_COMPLETE.md` - Scale transformation documentation
- `AOG_PROGRESS_SUMMARY.md` - Comprehensive AoG implementation status

## Reference Documentation

### Dataflow System Documentation

- **`notebooks/tableplot_book/dataflow_walkthrough.clj`**: Complete tutorial on the six rules, dependency tracking, caching, and practical examples
- **`notebooks/tableplot_book/dataflow_design_analysis.md`**: Comprehensive analysis for future grammar-of-graphics work, including:
  - Full implementation details (xform.clj, dag.clj, cache.clj)
  - Tableplot patterns in practice (~90 substitution keys explained)
  - Comparison framework for GG systems
  - Conversion patterns (e.g., Julia's Algebra of Graphics → Tableplot)
  - Alternative design considerations
  - Open questions and limitations
  - Future directions for research

### AlgebraOfGraphics Documentation

- **`notebooks/tableplot_book/aog_demo.clj`**: AlgebraOfGraphics API examples and comprehensive feature showcase
- **`AOG_THING_GEOM_REFERENCE.md`**: Comprehensive 4-layer architecture guide (28K) covering:
  - Complete architecture: Layer 1 (User API) → Layer 2 (IR) → Layer 3 (Processing) → Layer 4 (Rendering)
  - All 7 IR schemas with examples (Layer, ProcessedLayer, Entry, AxisConfig, CategoricalScale, ContinuousScale, AxisEntries)
  - Processing pipeline details (`layer->processed-layer`, `processed-layer->entry`)
  - Backend rendering internals (thi.ng/geom SVG generation)
  - 5 complete worked examples showing full pipeline
  - Best practices and patterns
- **`notebooks/tableplot_book/aog_thing_geom_architecture.clj`**: Interactive executable notebook with 10 parts:
  - Live schema validation examples
  - Statistical transforms (linear, smooth, histogram, density)
  - Native polar coordinate support
  - Multi-layer compositions
  - Complete pipeline demonstrations

This summary provides the essential information needed for an LLM to understand and work effectively with the Tableplot codebase, including its architecture, APIs, patterns, and extension points.
