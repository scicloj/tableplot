# Tableplot Project Summary

## Overview
Tableplot is a Clojure library for declarative data visualization, implementing a grammar of graphics approach similar to R's ggplot2. The project is actively developing V2, which introduces a sophisticated dataflow-based architecture that separates specification from rendering.

## Project Structure

```
tableplot/
├── src/tableplot/
│   ├── v1/                          # Legacy implementation
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
│   ├── ggplot2_walkthrough.clj      # Complete ggplot2 API tutorial
│   ├── v2_dataflow_from_scratch.clj # Educational notebook building dataflow from scratch
│   ├── v2_dataflow_walkthrough.clj  # V2 dataflow tutorial
│   └── plotly_backend_dev.clj       # Plotly backend development
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

## License

[License information if available]
