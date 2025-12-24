# Tableplot Project Summary

## Overview
Tableplot is a Clojure library for declarative data visualization, implementing a grammar of graphics approach similar to R's ggplot2. The project is actively developing V2, which introduces a sophisticated dataflow-based architecture that separates specification from rendering.

## Project Structure

```
tableplot/
├── src/scicloj/tableplot/
│   ├── v1/
│   │   └── aog/                     # AlgebraOfGraphics implementation
│   │       ├── core.clj             # AoG API (*, +, data, mapping, etc.)
│   │       ├── processing.clj       # Layer → Entry pipeline
│   │       ├── ir.clj               # Intermediate representation specs
│   │       ├── plotly.clj           # Plotly backend
│   │       ├── vegalite.clj         # Vega-Lite backend
│   │       ├── thing-geom.clj       # thi.ng/geom backend
│   │       └── thing-geom-themes.clj # ggplot2 themes for thi.ng/geom
│   └── v2/
│       ├── api.clj                  # Public API functions
│       ├── dataflow.clj             # Core dataflow engine
│       ├── inference.clj            # Inference rules for deriving values
│       ├── plotly.clj               # Plotly backend renderer
│       ├── hanami.clj               # Hanami backend renderer  
│       ├── ggplot.clj               # ggplot2-compatible API
│       └── ggplot/
│           └── themes.clj           # Complete ggplot2 theme system (8 themes)
├── notebooks/
│   ├── building_aog_in_clojure.clj  # Design exploration: AoG v2 with normalized representation
│   │
│   └── tableplot_book/
│       ├── # V2 (ggplot2-style) notebooks
│       ├── ggplot2_walkthrough.clj      # Complete ggplot2 API tutorial
│       ├── v2_dataflow_from_scratch.clj # Educational: building dataflow from scratch
│       ├── v2_dataflow_walkthrough.clj  # V2 dataflow tutorial
│       ├── plotly_backend_dev.clj       # Plotly backend development
│       │
│       ├── # V1 (AlgebraOfGraphics) notebooks
│       ├── # Learning Path (4 notebooks):
│       ├── aog_tutorial.clj             # AoG introduction
│       ├── aog_plot_types.clj           # Comprehensive plot type reference
│       ├── aog_backends_demo.clj        # Backend comparison (Plotly/Vega/thi.ng)
│       ├── aog_examples.clj             # Real-world dataset examples
│       │
│       ├── # Backend Guides (3 notebooks - Equal Depth):
│       ├── backend_plotly.clj           # Plotly backend complete guide
│       ├── backend_vegalite.clj         # Vega-Lite backend complete guide
│       ├── backend_thing_geom.clj       # thi.ng/geom backend complete guide
│       │
│       ├── # Technical (2 notebooks):
│       ├── aog_architecture.clj         # Architecture and pipeline
│       └── theme_showcase.clj           # Theme examples
│       │
│       └── archive/                     # Archived/old notebooks
├── test/
│   └── tableplot/v2/                # V2 test suite
└── docs/
    ├── GGPLOT2_IMPLEMENTATION.md    # ggplot2 implementation guide
    ├── v2_dataflow_design.md        # V2 dataflow design decisions
    └── v2_refactoring_summary.md    # V2 refactoring history
```

## Core Architecture: V2 Dataflow Model

### Key Concept: Three-Stage Process

V2 uses a dataflow architecture that separates **specification** → **inference** → **rendering**:

1. **Construction**: User builds a spec using high-level API functions
2. **Inference**: System automatically derives missing values using rules
3. **Rendering**: Backend converts spec to visualization (Plotly, Hanami, etc.)

### Substitution Keys Convention

The dataflow model uses **substitution keys** (keywords starting with `=`) as placeholders:

```clojure
;; Template with placeholders
{:x :=x-data
 :y :=y-data
 :type :=geom-type}

;; Spec with substitutions  
{:x :=x-data
 :y :=y-data
 :type :=geom-type
 :sub/map {:=x-data [1 2 3]
           :=y-data [4 5 6]
           :=geom-type "scatter"}}

;; After inference and substitution
{:x [1 2 3]
 :y [4 5 6]
 :type "scatter"}
```

**Important**: Substitution keys (`:=x-data`, `:=y-field`, etc.) are a **naming convention**, not an enforced concept. They're just keywords starting with `=` that the dataflow engine knows to substitute.

### Core Dataflow Functions

From `src/tableplot/v2/dataflow.clj`:

```clojure
;; Spec construction
(defn make-spec [template & {:as substitutions}]
  "Create a spec from a template with optional substitutions")

(defn add-substitution [spec k v]
  "Add a substitution to a spec")

(defn get-substitution [spec k]
  "Get a substitution value from a spec")

;; Inference
(defn infer [spec]
  "Run inference to derive all missing substitution values")

;; Finding substitution keys
(defn find-subkeys [spec]
  "Find all substitution keys (keywords starting with =) in spec structure")
```

### Inference Rules

Inference rules are registered functions that derive values for substitution keys:

```clojure
;; From src/tableplot/v2/inference.clj
(register-rule :=x-data
  (fn [spec context]
    (let [data (df/get-substitution spec :=data)
          x-field (df/get-substitution spec :=x-field)]
      (when (and data x-field)
        (mapv x-field data)))))

(register-rule :=title
  (fn [spec context]
    (let [user-labels (df/get-substitution spec :=labels)
          user-title (:title user-labels)]
      (or user-title
          ;; Auto-generate if no user title
          (let [x-field (df/get-substitution spec :=x-field)
                y-field (df/get-substitution spec :=y-field)]
            (when (and x-field y-field)
              (str (name y-field) " vs " (name x-field))))))))
```

**Rule Priority**: User-provided values always take precedence over inferred values.

## ggplot2 API Implementation

### Complete Implementation Status

The library implements a comprehensive ggplot2-compatible API in 4 phases:

#### Phase 1: Core Grammar ✅ Complete
- `ggplot` - Initialize plot with data
- `aes` - Define aesthetic mappings  
- `geom-point`, `geom-line`, `geom-bar` - Geometric objects
- `+` - Layer composition operator

#### Phase 2: Customization ✅ Complete  
- `xlim`, `ylim` - Set axis limits
- `labs` - Set labels (title, x, y)
- `scale-color-discrete`, `scale-x-continuous` - Scale transformations

#### Phase 3: Faceting ✅ Complete
- `facet-wrap` - Create small multiples by wrapping
- `facet-grid` - Create small multiples in grid layout

#### Phase 4: Themes ✅ Complete
Complete theme system with 8 themes matching R's ggplot2:

**Available Themes** (`src/tableplot/v2/ggplot/themes.clj`):
- `theme-grey` - Default ggplot2 theme (grey background, white grid)
- `theme-bw` - Black and white theme
- `theme-minimal` - Minimal theme with minimal non-data ink
- `theme-classic` - Classic theme with axis lines
- `theme-dark` - Dark background theme
- `theme-light` - Light theme with light grey lines
- `theme-void` - Completely empty theme
- `theme-linedraw` - Theme with only black lines

**Theme Structure** (hierarchical):
```clojure
{:plot {:background "color"
        :title {:font {...}}}
 :panel {:background "color"
         :grid {:major {...} :minor {...}}
         :border {...}}
 :axis {:text {:font {...}}
        :title {:font {...}}
        :line {...}
        :ticks {...}}
 :legend {:position "right|left|top|bottom"
          :background "color"
          :text {:font {...}}}}
```

**Theme Customization**:
```clojure
;; Apply a predefined theme
(-> (ggplot mtcars (aes :x :wt :y :mpg))
    (geom-point)
    (theme spec theme-minimal))

;; Customize using dot-notation
(-> (ggplot mtcars (aes :x :wt :y :mpg))
    (geom-point)
    (theme spec
           :plot.background "#f0f0f0"
           :panel.grid.major.color "#cccccc"
           :axis.title.font.size 14))

;; Combine theme with customizations
(-> (ggplot mtcars (aes :x :wt :y :mpg))
    (geom-point)
    (theme spec theme-dark)
    (theme spec :plot.title.font.size 16))
```

**Default Theme**: All plots automatically use `theme-grey` (matching R's ggplot2 default) unless overridden.

### Theme Implementation Details

**Theme Application** (`src/tableplot/v2/plotly.clj`):
- Themes are applied during rendering to the Plotly layout
- ~20+ theme elements mapped to Plotly properties
- String titles/labels automatically converted to Plotly objects for font compatibility
- Deep merge preserves nested settings during customization

**Deep Merge Helper**:
```clojure
(defn- deep-merge
  "Recursively merges maps."
  [& maps]
  (apply merge-with
         (fn [x y]
           (if (and (map? x) (map? y))
             (deep-merge x y)
             y))
         maps))
```

### Example Usage

```clojure
(require '[tableplot.v2.ggplot :as gg])
(require '[tableplot.v2.ggplot.themes :as themes])

;; Basic scatter plot with default theme
(-> (gg/ggplot mtcars (gg/aes :x :wt :y :mpg))
    (gg/geom-point)
    (gg/labs :title "Fuel Efficiency vs Weight"))

;; With custom theme
(-> (gg/ggplot mtcars (gg/aes :x :wt :y :mpg :color :cyl))
    (gg/geom-point)
    (gg/theme spec themes/theme-minimal)
    (gg/labs :title "MPG by Weight and Cylinders"))

;; Faceted plot with theme customization
(-> (gg/ggplot mtcars (gg/aes :x :wt :y :mpg))
    (gg/geom-point)
    (gg/facet-wrap [:cyl])
    (gg/theme spec themes/theme-bw)
    (gg/theme spec
              :panel.grid.minor.show false
              :axis.title.font.size 12))
```

## Educational Resources

### v2_dataflow_from_scratch.clj

**Purpose**: Standalone educational notebook that builds a minimal dataflow engine from scratch and uses it to implement a toy grammar of graphics.

**Structure** (~633 lines):
1. **Part 1: The Problem** - Why direct construction is limiting
2. **Part 2: The Dataflow Solution** - Three-stage process overview
3. **Part 3: Minimal Dataflow Engine** (~100 lines of core code):
   - `make-spec`, `add-substitution`, `get-substitution` - Spec construction
   - `find-subkeys` - Convention-based substitution key discovery
   - `register-rule!`, `infer-key` - Inference rule system
   - `infer-missing` - Automatic value derivation
   - `apply-substitutions` - Template expansion
   - `infer` - Complete inference pipeline

4. **Part 4: Toy Grammar of Graphics** - Simple ggplot-like API:
   ```clojure
   (-> (ggplot {:x [1 2 3] :y [4 5 6]})
       (aes :x :x :y :y)
       (geom :point))
   ```

5. **Part 5: The Power of Dataflow**:
   - **Inspection**: See derived values before rendering
   - **Override**: Replace any inferred value
   - **Composition**: Build complex specs from simple parts
   - **Multiple Backends**: Same spec → different outputs (Plotly, text, etc.)

6. **Part 6: Comparison** - Direct vs Dataflow side-by-side
7. **Part 7: From Toy to Real** - Mapping to full V2 implementation
8. **Part 8: Design Alternatives** - Other approaches considered

**Key Teaching Points**:
- Substitution keys are just a naming convention (keywords starting with `=`)
- Inference rules allow automatic derivation of missing values
- Specs are data, enabling inspection and transformation
- Multiple backends can render the same spec differently
- Template-based approach enables code reuse

**Reproducibility**: Uses `^:kind/pprint` markers for Clay rendering. No hardcoded outputs - Clay writes actual outputs on rendering.

## Backend Implementations

### Plotly Backend (`src/tableplot/v2/plotly.clj`)

**Main Function**:
```clojure
(defn spec->plotly
  "Convert V2 spec to Plotly spec."
  [spec]
  (let [inferred (df/infer spec)
        traces (spec->traces inferred)
        layout (spec->layout inferred)]
    {:data traces
     :layout layout}))
```

**Key Features**:
- Converts V2 specs to Plotly data/layout format
- Handles multiple geometry types (point, line, bar)
- Applies faceting (facet-wrap, facet-grid)
- Applies theme settings to layout
- Converts string titles/labels to Plotly objects for theme compatibility

**Trace Generation**:
- Maps aesthetic mappings to Plotly trace properties
- Handles color, size, shape aesthetics
- Groups data for faceting

**Layout Generation**:
- Applies scales (limits, axis titles)
- Applies labels
- Applies theme settings (colors, fonts, grids, etc.)
- Creates subplot layouts for faceting

### Hanami Backend (`src/tableplot/v2/hanami.clj`)

Alternative backend using Hanami/Vega-Lite for web-based visualizations.

## Key Dependencies

```clojure
{:deps
 {org.clojure/clojure {:mvn/version "1.11.1"}
  scicloj/tablecloth {:mvn/version "7.0-beta2"}      ; Data manipulation
  scicloj/kindly {:mvn/version "4.0.0-beta4"}        ; Visualization protocol
  scicloj/clay {:mvn/version "2.0.0-beta15"}         ; Notebook rendering
  org.scicloj/hanamicloth {:mvn/version "1.0.0"}     ; Hanami integration
  generateme/fastmath {:mvn/version "2.2.1"}}}       ; Math utilities
```

## Architecture Insights from Analysis

### Strengths of V2 Dataflow Model

1. **Separation of Concerns** (9/10)
   - Clean separation: spec construction → inference → rendering
   - Backends are swappable (Plotly, Hanami, custom)
   - Inference rules are modular and composable

2. **Declarative API** (8/10)
   - Threading macro `->` enables natural composition
   - Functions like `aes`, `geom-point` are pure data transformations
   - No imperative mutation

3. **Extensibility** (8/10)
   - New geometry types: just add inference rules + backend support
   - New backends: implement `spec->backend` function
   - New transformations: add functions that modify spec

4. **Inspectability** (10/10)
   - Can inspect spec at any stage
   - Can see what was inferred vs user-provided
   - Can override any inferred value

5. **Code Reuse** (9/10)
   - Templates enable sharing common patterns
   - Inference rules eliminate repetitive code
   - Backend-agnostic specs

### Current Limitations

1. **Learning Curve** (Convenience: 7/10)
   - Understanding substitution keys requires mental model shift
   - Debugging inference can be non-obvious
   - Error messages could be more helpful

2. **Verbosity in Implementation**
   - Inference rules require boilerplate (register-rule, context threading)
   - Finding subkeys uses tree walking (performance concern for large specs)

3. **Inference Rule Dependencies**
   - Some rules depend on others (e.g., :=x-data needs :=x-field)
   - No explicit dependency graph
   - Order matters but isn't enforced

4. **Context Threading**
   - Context parameter threaded through but rarely used
   - Could be simplified or removed

5. **Error Handling**
   - Missing inference rule throws generic error
   - Stack traces don't clearly show which spec caused issue
   - No validation of substitution key values

### Recommended Improvements

**High Priority**:
1. Add spec validation (check required keys, type validation)
2. Improve error messages (show spec context, suggest fixes)
3. Document inference rule dependency graph
4. Add debugging utilities (`explain-inference`, `show-rules`)

**Medium Priority**:
5. Optimize `find-subkeys` for large specs (memoization, caching)
6. Simplify context parameter (make it optional or remove)
7. Add more inference rules for common patterns
8. Standardize inference rule naming conventions

**Low Priority**:
9. Add spec diffing utilities (compare specs, show changes)
10. Add spec transformation utilities (map over layers, filter geometries)
11. Add spec composition patterns (merge, overlay)

## Known Issues and Fixes

### Issue 1: Theme ClassCastException (FIXED)
**Problem**: `java.lang.String cannot be cast to clojure.lang.Associative` when applying theme to title

**Cause**: Plotly layout title was a string, but theme application used `assoc-in` to set font

**Fix**: Modified `spec->layout` to convert string titles/labels to Plotly objects:
```clojure
;; Convert string to map for theme compatibility
(assoc :title (if (string? title)
                {:text title}
                title))
```

### Issue 2: Title Inference Not Respecting User Values (FIXED)
**Problem**: User-provided titles were being overwritten by auto-generated titles

**Cause**: `infer-title` was always generating from x/y fields, not checking `:=labels` first

**Fix**: Modified inference to check user-provided values first:
```clojure
(defn infer-title [spec context]
  (let [user-labels (df/get-substitution spec :=labels)
        user-title (:title user-labels)]
    (or user-title
        ;; Auto-generate only if no user title
        (when (and x-field y-field)
          (str (name y-field) " vs " (name x-field))))))
```

### Issue 3: Theme Customization Losing Nested Settings (FIXED)
**Problem**: When customizing themes, entire nested structures were being replaced

**Cause**: Using shallow `merge` instead of recursive merge

**Fix**: Added `deep-merge` helper function for recursive map merging

### Issue 4: Empty Sequence to min/max (FIXED)
**Problem**: `ArityException: Wrong number of args (0) passed to: clojure.core/min`

**Cause**: Scale inference rules called `(apply min x-data)` on potentially empty data

**Fix**: Added nil and empty checks:
```clojure
(when (and x-data (seq x-data))
  (if (every? number? x-data)
    {:type :linear
     :domain [(apply min x-data) (apply max x-data)]}
    {:type :categorical
     :domain (vec (distinct x-data))}))
```

## Testing

Tests are located in `test/tableplot/v2/` and cover:
- Core dataflow functions
- Inference rules
- Backend rendering
- ggplot2 API functions

Run tests:
```bash
clojure -X:test
```

## Development Workflow

### Interactive Development
The project uses REPL-driven development with Clay notebooks:

1. Start REPL: `clojure -A:dev`
2. Load namespace: `(require '[tableplot.v2.ggplot :as gg] :reload)`
3. Evaluate in notebook: `notebooks/ggplot2_walkthrough.clj`
4. Render with Clay: `(clay/make! {:source-path "notebooks/ggplot2_walkthrough.clj"})`

### Notebooks
- **ggplot2_walkthrough.clj**: Complete tutorial of ggplot2 API with examples
- **v2_dataflow_from_scratch.clj**: Educational notebook building dataflow from scratch
- **v2_dataflow_walkthrough.clj**: Tutorial of V2 dataflow concepts
- **plotly_backend_dev.clj**: Development and testing of Plotly backend

### Adding New Features

**Example: Adding a new geometry type**

1. Add geometry function to `src/tableplot/v2/ggplot.clj`:
```clojure
(defn geom-histogram [spec]
  (df/add-substitution spec :=geom :histogram))
```

2. Add inference rules to `src/tableplot/v2/inference.clj`:
```clojure
(register-rule :=histogram-bins
  (fn [spec context]
    (let [x-data (df/get-substitution spec :=x-data)]
      (when x-data
        (int (Math/sqrt (count x-data)))))))  ; Sturges' rule
```

3. Add backend support to `src/tableplot/v2/plotly.clj`:
```clojure
(defmethod geom->trace :histogram [spec geom-type]
  {:type "histogram"
   :x (:x-data spec)
   :nbinsx (:histogram-bins spec)})
```

4. Add tests and examples

## Documentation

- **GGPLOT2_IMPLEMENTATION.md**: Complete guide to ggplot2 implementation (all 4 phases)
- **v2_dataflow_design.md**: Design decisions for V2 dataflow architecture
- **v2_refactoring_summary.md**: History of V2 refactoring process
- **PROJECT_SUMMARY.md**: This file - comprehensive project overview

## Design Philosophy

1. **Data-Driven**: Specs are data structures, not objects
2. **Composable**: Small functions that combine with `->` threading
3. **Declarative**: Describe what you want, not how to build it
4. **Inspectable**: Always able to see and modify the spec
5. **Backend-Agnostic**: Same spec works with multiple rendering backends
6. **Inference Over Configuration**: Smart defaults, explicit overrides
7. **Convention Over Enforcement**: Substitution keys are just a naming pattern

## Future Directions

### Short Term
- Add more geometry types (histogram, boxplot, violin, etc.)
- Add statistical transformations (smooth, bin, count)
- Add position adjustments (dodge, stack, jitter)
- Improve error messages and debugging tools
- Add spec validation

### Medium Term
- Add interactive features (tooltips, selection, brushing)
- Add animation support
- Add more backends (Vega-Lite, Observable Plot)
- Add more themes (economist, fivethirtyeight, etc.)
- Performance optimization for large datasets

### Long Term
- Grammar of tables (ggplot2-style table creation)
- Grammar of dashboards (compose multiple plots)
- Statistical modeling integration
- Publication-ready export (PDF, SVG)

## Contributing

The project is under active development. Key areas for contribution:
- New geometry types
- New statistical transformations  
- Backend implementations
- Theme designs
- Documentation improvements
- Performance optimizations

## AoG v2 Design Exploration: Normalized Representation

### Overview

**Location**: `notebooks/building_aog_in_clojure.clj` (~71,700 lines)

A comprehensive design exploration of an alternative AoG API that uses **normalized layer representation** (always vectors) instead of context-dependent types. This is a working prototype demonstrating compositional graphics with clean separation between API, intermediate representation, and rendering.

**Status**: Design exploration / prototype for feedback and decision-making about Tableplot's future

### Core Design: Everything is Vectors

**Key Innovation**: Normalize all layer operations to return vectors, enabling:
- Standard library operations (`map`, `filter`, `into`, `mapv`)
- Predictable composition (no type checking needed)
- Transparent data flow

**API Structure**:
```clojure
;; Constructors return vectors (single-element for most, multi-element for +)
(data penguins)          ;; → [{:aog/data penguins}]
(mapping :x :y)          ;; → [{:aog/x :x :aog/y :y}]
(scatter {:alpha 0.7})   ;; → [{:aog/plottype :scatter :aog/alpha 0.7}]

;; * merges layers
(* (data penguins) (mapping :x :y) (scatter))
;; → [{:aog/data penguins :aog/x :x :aog/y :y :aog/plottype :scatter}]

;; + concatenates layers (overlay)
(+ (scatter) (linear))
;; → [{:aog/plottype :scatter} {:aog/transformation :linear}]

;; Distributive property: a * (b + c) = (a * b) + (a * c)
(* (data penguins) (mapping :x :y) (+ (scatter) (linear)))
;; → Two layers with shared data/mapping

;; Single plot function dispatches to targets
(plot layers)                    ;; Default: :geom (static SVG)
(plot layers {:target :vl})      ;; Vega-Lite (interactive)
(plot layers {:target :plotly})  ;; Plotly (3D, rich interactions)

;; Target can be compositional
(plot (* (data penguins) (mapping :x :y) (scatter) (target :vl)))
```

### Three Rendering Targets

All three targets use the **same layer specification**:

1. **:geom** (thi.ng/geom) - Static SVG with ggplot2 aesthetics
2. **:vl** (Vega-Lite) - Declarative interactive visualizations
3. **:plotly** (Plotly.js) - Rich interactive visualizations

**Target Independence**: `plot` function checks for `:aog/target` in layers or `:target` in options (options take precedence)

**Consistent Theme**: ggplot2 color palette (#F8766D, #00BA38, #619CFF, etc.) and gray background (#EBEBEB) with white gridlines across all targets

### Layer Representation

**Flat maps with namespaced keys** (`:aog/*`) prevent collision with data columns:

```clojure
{:aog/data penguins
 :aog/x :bill-length-mm
 :aog/y :bill-depth-mm
 :aog/color :species
 :aog/plottype :scatter
 :aog/alpha 0.7
 :aog/target :vl}
```

**Advantage**: Standard `merge` works perfectly (no custom layer-merge needed)

### Implementation Highlights

**Simplified * operator** (73% smaller):
```clojure
(defn *
  ([x] (if (map? x) [x] x))
  ([x y]
   (cond
     (and (map? x) (map? y)) [(merge x y)]
     (and (map? x) (vector? y)) (mapv #(merge x %) y)
     (and (vector? x) (map? y)) (mapv #(merge % y) x)
     (and (vector? x) (vector? y)) (vec (for [a x, b y] (merge a b)))))
  ([x y & more] (reduce * (* x y) more)))
```

**Standard library operations**:
```clojure
;; Conditional building with into
(if show-regression?
  (into scatter-layers regression-layers)
  scatter-layers)

;; Transform layers with mapv
(mapv #(assoc-in % [:aog/alpha] 0.8) layers)

;; Filter layers
(filterv #(= :scatter (:aog/plottype %)) layers)
```

**Specs as data** - All target specs are plain maps/vectors:
```clojure
;; Normal usage
(plot layers {:target :vl})

;; Advanced: Manipulate raw specs
(def vega-spec (layers->vega-spec layers 600 400))
(def customized-spec
  (-> vega-spec
      (assoc :title {...})
      (assoc-in [:encoding :x :axis] {...})))
(kind/vega-lite customized-spec)
```

### Vega-Lite Faceting Fix

**Problem**: Multi-layer specs with faceting don't work in Vega-Lite when `:column` encoding is inside each layer

**Solution**: Different faceting approaches based on layer count:
- **Single layer**: Faceting in encoding (standard VL)
- **Multi-layer**: Top-level `:facet` with `:spec` containing layers (VL composition)

```clojure
;; Multi-layer faceting
{:data {:values [...]}
 :facet {:column {:field "island" :type "nominal"}}
 :spec {:layer [{...scatter...} {...regression...}]}}
```

### Key Insights from Notebook

1. **Flat structure enables standard merge** - No custom layer-merge function needed
2. **Namespacing prevents collisions** - `:aog/*` keys coexist with any data columns
3. **Transparent IR** - Layers are inspectable plain maps, not opaque objects
4. **Target as data** - Rendering target can be specified compositionally via `(target :vl)` or options
5. **Julia's compositional approach translates to Clojure data** - Same algebraic concepts, different substrate

### Trade-offs

**Gains**:
- Composability through standard operations (`merge`, `assoc`, `mapv`, `filter`, `into`)
- Transparent intermediate representation (plain maps)
- Target independence (one API, multiple renderers)
- Flexible data handling (plain maps or datasets)
- Clear separation of concerns

**Costs**:
- Namespace verbosity (`:aog/color` vs `:color` - 5 extra chars)
- Novel operators (`*`, `+` shadow arithmetic)
- Incomplete feature set (smooth, density, histogram defined but not implemented)

### Relationship to V1 and V2

This design exploration is **not a replacement** for current Tableplot APIs, but investigates an alternative approach that could:
- Coexist as additional namespace (e.g., `scicloj.tableplot.v1.aog.normalized`)
- Inform future design decisions for V2 or beyond
- Provide a third compositional option alongside ggplot2-style (V2) and current AoG (V1)

**Addresses different concerns than V2**:
- V2 focuses on dataflow/inference for smart defaults
- AoG v2 focuses on algebraic composition with explicit operations
- Both can coexist serving different use cases

### Documentation Structure

The notebook follows a clear narrative (~1,600 lines):
1. **Context & Motivation** - Why explore alternatives
2. **Inspiration** - AlgebraOfGraphics.jl concepts
3. **Design Exploration** - Evolution from nested → flat+plain → flat+namespaced
4. **Proposed Design** - API overview and constructors
5. **Examples** - Progressive complexity (scatter → multi-layer → faceting)
6. **Implementation** - Helper functions and all three targets
7. **Multiple Targets** - Demonstrating target independence
8. **Specs as Data** - Programmatic manipulation examples
9. **Trade-offs** - Honest assessment of gains and costs
10. **Integration Path** - Coexistence with V1/V2
11. **Decision Points** - Open questions for community feedback
12. **Summary** - Key insights and implementation status

### Recent Simplifications (2025-12-23)

The notebook underwent code simplification to focus on demonstrating design directions:

**Helper Functions Added** (~50 lines saved):
- `make-axes` - Extract repeated axis logic for thi.ng/geom
- `make-vega-encoding` - Build Vega-Lite encodings from layer aesthetics
- `group-by-color-plotly` - Group data by color for Plotly traces

**Code Reductions**:
- `compute-linear-regression`: 50 → 4 lines (uses only 2 points for straight line)
- `layer->scatter-spec`: 54 → 28 lines (using make-axes)
- `layer->line-spec`: 54 → 32 lines (using make-axes)
- `layer->vega-scatter`: 19 → 3 lines (using make-vega-encoding)
- `layer->vega-line`: 27 → 10 lines (using make-vega-encoding)
- `layer->plotly-trace`: 122 → 68 lines (using group-by-color-plotly)
- `layers->svg`: Simplified panel calculation, removed branching

**Restorations After Review**:
1. **Empty domain check** - `infer-domain` handles empty arrays safely (returns [0 1])
2. **2-point regression** - Linear regression uses exactly 2 points (start/end) since it's a straight line
3. **Faceting support** - Vega-Lite target supports `:facet`, `:col`, `:row` aesthetics with proper dimensions

**Total Reduction**: ~150-200 lines while maintaining full functionality

### Faceting Architecture Research

**Key Insight**: Proper faceting involves sophisticated architectural decisions beyond "render multiple times"

**AlgebraOfGraphics.jl Hybrid Scale Strategy** (see `docs/faceting_architecture_research.md`):

| Scale Type | Scope | Why |
|------------|-------|-----|
| Categorical (color groups) | Global | Consistency across facets |
| Continuous positional (X, Y) | Per-facet | Each panel shows optimal range |
| Continuous other (size, alpha) | Global | Maintains visual encoding |

**Separation of Concerns**:
- **Scale computation**: Determines domain/range
- **Axis linking**: Visual alignment (`:all`, `:colwise`, `:rowwise`, `:none`)

**Data Model**: Multi-dimensional arrays where dimensions = groupings, sliced via broadcast indexing

**Pipeline**:
1. Compute categorical scales globally
2. Create grid positions from faceting aesthetics
3. For each grid cell: slice data, compute per-facet X/Y scales, use global other scales
4. Render each panel
5. Apply axis linking, hide decorations, add labels

**Current Status**: Faceting supported in Vega-Lite target only. For `:geom` target, would require ~150-200 lines implementing:
- Data partitioning by faceting variable
- Global scale inference (with per-facet option for X/Y)
- Modified rendering to accept provided scales
- SVG grid layout with viewBox positioning

**Design Decision**: Keep Vega-Lite-only faceting for demo, acknowledge architectural complexity honestly

## V1: AlgebraOfGraphics Implementation

### Overview

V1 implements an AlgebraOfGraphics (AoG) API inspired by Julia's AlgebraOfGraphics.jl, providing a composable visualization grammar with three production-ready backends.

### Core Concepts

**Algebraic Operators**:
- `*` (multiplication) - Merge layer specifications
- `+` (addition) - Overlay multiple layers

**Pipeline**: `Layer → ProcessedLayer → Entry → Backend`

**Key Functions** (`src/scicloj/tableplot/v1/aog/core.clj`):
```clojure
(aog/data dataset)              ; Attach data to layer
(aog/mapping :x :y)             ; Map data columns to aesthetics
(aog/mapping :x :y {:color :species})  ; With additional aesthetics
(aog/scatter)                   ; Point geometry
(aog/line)                      ; Line geometry
(aog/bar)                       ; Bar geometry
(aog/linear)                    ; Linear regression transform
(aog/smooth)                    ; LOESS smoothing
(aog/histogram)                 ; Histogram
(aog/density)                   ; Kernel density estimation
(aog/draw spec opts)            ; Render visualization
```

**Example**:
```clojure
(require '[scicloj.tableplot.v1.aog.core :as aog])

;; Basic scatter plot
(aog/draw
 (aog/* (aog/data {:x [1 2 3] :y [4 5 6]})
        (aog/mapping :x :y)
        (aog/scatter)))

;; Multi-layer with grouping
(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :bill-length :bill-depth {:color :species})
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))
```

### Three Production-Ready Backends

All three backends have **equal status** with comprehensive documentation:

#### 1. Plotly Backend (Default)
- **Superpower**: 3D visualizations + Rich interactivity
- **Output**: HTML + JavaScript
- **Best for**: Dashboards, exploratory data analysis, web apps
- **Features**: Pan, zoom, hover tooltips, 3D scatter/surface/mesh
- **Guide**: `notebooks/tableplot_book/backend_plotly.clj` (656 lines)

#### 2. Vega-Lite Backend
- **Superpower**: Faceting (small multiples)
- **Output**: SVG (via Vega-Lite spec)
- **Best for**: Publications, reports, faceted plots
- **Features**: Column/row/facet layouts, 5 themes, declarative specs
- **Guide**: `notebooks/tableplot_book/backend_vegalite.clj` (577 lines)

#### 3. thi.ng/geom Backend
- **Superpower**: Native polar coordinates + Pure Clojure
- **Output**: SVG (native)
- **Best for**: Radar charts, rose diagrams, print-ready graphics
- **Features**: True polar rendering, 9 ggplot2 themes, no JavaScript
- **Guide**: `notebooks/tableplot_book/backend_thing_geom.clj` (835 lines)

### Backend Usage

```clojure
;; Plotly (default via aog/draw)
(aog/draw spec)

;; Vega-Lite (explicit)
(require '[scicloj.tableplot.v1.aog.vegalite :as vegalite])
(vegalite/vegalite entry {:width 600 :height 400})

;; thi.ng/geom (explicit)
(require '[scicloj.tableplot.v1.aog.thing-geom :as tg])
(tg/entry->svg entry {:width 600 :height 400 :theme :grey})
```

### Backend Comparison Table

| Feature | **Plotly** | **Vega-Lite** | **thi.ng/geom** |
|---------|-----------|--------------|----------------|
| **Output Format** | HTML + JS | SVG (via spec) | SVG (native) |
| **Interactivity** | ✅ Full (pan, zoom, hover) | ⚠️ Limited (tooltips) | ❌ None (static) |
| **3D Support** | ✅ Native | ❌ None | ❌ None |
| **Polar Coordinates** | ⚠️ Manual | ❌ None | ✅ Native |
| **Faceting** | ⚠️ Manual subplots | ✅ Built-in (`col`, `row`) | ❌ None |
| **Themes** | ✅ 7+ built-in | ✅ 5 built-in | ✅ 9 ggplot2 themes |
| **File Size** | ~3MB (plotly.js) | ~100KB (spec) | ~10KB (SVG) |
| **Performance** | ⚠️ Slower (100k+) | ✅ Fast | ✅ Very fast |
| **Customization** | ✅ Extensive | ✅ Good | ✅ Full |
| **Dependencies** | Plotly.js (browser) | Vega-Lite (JVM) | Pure Clojure |
| **Use Cases** | Dashboards, 3D, EDA | Reports, facets | Polar, print, pure SVG |

### Entry IR (Intermediate Representation)

The backend-agnostic format that all backends consume:

```clojure
{:plottype :scatter           ; :scatter, :line, :bar, etc.
 :positional [[1 2 3]         ; x values
              [4 5 6]]        ; y values
 :named {:alpha 0.5           ; Optional styling
         :color "#0af"}}
```

### thi.ng/geom Themes

9 ggplot2-compatible themes (`src/scicloj/tableplot/v1/aog/thing-geom-themes.clj`):

- `:grey` (default) - Grey panel, white grid (ggplot2 default)
- `:bw` - Black & white
- `:minimal` - Minimal non-data ink
- `:classic` - Classic with axis lines
- `:dark` - Dark background
- `:light` - Light grey lines
- `:void` - Completely empty
- `:linedraw` - Black lines only
- `:tableplot` - tableplot default

**Usage**:
```clojure
(tg/entry->svg entry {:theme :minimal :width 600 :height 400})
```

### Documentation Philosophy

The V1 AoG documentation follows a clear structure:

1. **Learning Path** (4 notebooks): Tutorial → Plot Types → Backends → Examples
2. **Backend Equality** (3 guides): Equal depth, equal structure, clear unique strengths
3. **Technical Deep Dive** (2 notebooks): Architecture + specialized topics

**Key Feature**: Identical comparison tables across all three backend guides ensure objective decision-making based on actual needs rather than implicit recommendations.

## License

[License information if available]
