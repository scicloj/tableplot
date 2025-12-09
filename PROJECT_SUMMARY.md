# Tableplot Project Summary

## Overview

Tableplot is a Clojure data visualization library inspired by ggplot2's layered grammar of graphics. It provides easy layered graphics by composing Hanami templates with Tablecloth datasets. The library enables creation of interactive visualizations that work with any tool supporting the Kindly data visualization standard, such as Clay and Clojupyter.

**Current Version:** 1-beta14  
**Maven Coordinates:** `org.scicloj/tableplot`  
**License:** Eclipse Public License v2.0

## Key Features

- Layered grammar of graphics similar to ggplot2
- Integration with Tablecloth for data processing
- Support for Plotly.js and Vega-Lite backends
- Kindly-compatible visualizations
- Composable plotting functions with pipeline-friendly API
- Statistical transformations (smoothing, histograms, density plots)
- Multiple plot types (scatter, line, bar, heatmap, 3D surfaces, etc.)

## Project Structure

### Source Code Organization

```
src/scicloj/tableplot/v1/
├── plotly.clj          # Main Plotly backend implementation
├── hanami.clj          # Vega-Lite backend via Hanami
├── transpile.clj       # Cross-backend transpilation
├── dag.clj             # Dependency-aware function system
├── xform.clj           # Data transformations
├── util.clj            # Utility functions
└── cache.clj           # Caching mechanisms
```

### Documentation and Examples

```
docs/                   # Quarto-generated documentation
notebooks/tableplot_book/  # Example notebooks and tutorials
├── plotly_walkthrough.clj    # Plotly API examples
├── hanami_walkthrough.clj    # Hanami/Vega-Lite examples
├── plotly_reference.clj      # Complete Plotly reference
└── transpile_reference.clj   # Cross-backend examples
```

## Core Dependencies

### Required Dependencies
- **tablecloth** (7.029.2) - Dataset manipulation and processing
- **aerial.hanami** (0.20.1) - Vega-Lite template system
- **metamorph.ml** (1.2) - Machine learning pipeline integration
- **fastmath** (3.0.0-alpha3) - Mathematical operations and statistics
- **kindly** (4-beta16) - Visualization standard compliance
- **tempfiles** (1-beta1) - Temporary file management
- **std.lang** (4.0.10) - Language utilities

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

### Dependency-Aware Function System (DAG)
The library uses a sophisticated dependency system defined in `dag.clj`:

- `defn-with-deps` macro creates functions that declare their data dependencies
- Automatic caching of intermediate computations
- Lazy evaluation of expensive operations
- Dependency resolution for complex visualization pipelines

### Data Flow Architecture
1. **Input Dataset** (Tablecloth/tech.ml.dataset)
2. **Layer Functions** apply aesthetic mappings and transformations
3. **Statistical Computations** (optional) - smoothing, binning, etc.
4. **Backend-Specific Rendering** (Plotly/Vega-Lite/ECharts)
5. **Kindly Metadata** added for tool integration
6. **Visualization Output** (JSON specifications)

### Cross-Backend Compatibility
The `transpile.clj` namespace enables cross-backend functionality:
- Convert between Plotly and Vega-Lite specifications
- Maintain feature parity across backends
- Backend-specific optimizations

## Implementation Patterns

### Aesthetic Mappings
- Keys prefixed with `=` (e.g., `:=x`, `:=y`, `:=color`)
- Automatic type inference from dataset columns
- Support for continuous and categorical mappings

### Template System
- Built on Hanami's substitution key system
- Composable plot specifications
- Default value propagation and merging

### Caching Strategy
- Intermediate results cached by content hash
- Statistical computations cached separately
- Cache invalidation on data changes

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
- Total: 110 comprehensive API tests

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
3. Add statistical transformation if needed
4. Update documentation and examples

### Custom Statistical Transformations
1. Implement stat function with `defn-with-deps`
2. Define data dependencies and caching strategy
3. Integrate with existing layer functions

### New Backend Support
1. Create new namespace following `plotly.clj` pattern
2. Implement core functions: `base`, `plot`, layer functions
3. Add transpilation support in `transpile.clj`
4. Update cross-backend tests

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
(defn-with-deps custom-stat [dataset x-col y-col]
  [dataset :=x :=y]  ; dependencies
  ;; transformation logic
  (tc/add-column dataset :computed-col computed-values))
```

### Cross-Backend Testing
```clojure
;; Test same visualization across backends
(def viz-spec {:=x :col1 :=y :col2})
(plotly/layer-point viz-spec)
(hanami/layer-point viz-spec)
```

This summary provides the essential information needed for an LLM to understand and work effectively with the Tableplot codebase, including its architecture, APIs, patterns, and extension points.
