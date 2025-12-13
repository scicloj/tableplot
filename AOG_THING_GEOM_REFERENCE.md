# AlgebraOfGraphics + thi.ng/geom: Complete Reference

**A comprehensive guide to Tableplot's AoG API and thi.ng/geom backend**

## Table of Contents

1. [Overview](#overview)
2. [Architecture: The Four-Layer Pipeline](#architecture-the-four-layer-pipeline)
3. [Layer 1: User API (AlgebraOfGraphics)](#layer-1-user-api-algebraofgraphics)
4. [Layer 2: Intermediate Representations](#layer-2-intermediate-representations)
5. [Layer 3: Processing Pipeline](#layer-3-processing-pipeline)
6. [Layer 4: Backend Rendering (thi.ng/geom)](#layer-4-backend-rendering-thinggeom)
7. [Complete Examples](#complete-examples)
8. [Best Practices](#best-practices)

---

## Overview

### What is This?

Tableplot's AlgebraOfGraphics (AoG) API provides a **grammar of graphics** for Clojure, inspired by Julia's AlgebraOfGraphics.jl. The system separates **what to plot** (the grammar) from **how to render it** (the backend).

Key innovations:

1. **Algebraic composition** - Use `*` (merge) and `+` (overlay) to build complex visualizations
2. **Backend-agnostic IR** - Entry format works with Vega-Lite, Plotly.js, or thi.ng/geom
3. **Pure Clojure rendering** - thi.ng/geom backend needs no JavaScript
4. **Malli validation** - Every layer is schema-validated for correctness

### Why thi.ng/geom?

The thi.ng/geom backend offers unique advantages:

- ✅ **Pure Clojure** - No JavaScript runtime required
- ✅ **Native polar coordinates** - True polar rendering (not simulated)
- ✅ **SVG output** - Static, embeddable graphics for papers/reports
- ✅ **Mathematical precision** - Excellent for parametric curves and geometric plots
- ✅ **Server-side rendering** - Generate visualizations without a browser

**Best for**: Scientific papers, polar plots (rose curves, radar charts), embedded SVGs, server-side rendering

**Not ideal for**: Interactive dashboards, 3D visualizations, real-time updates

---

## Architecture: The Four-Layer Pipeline

The system uses a **four-layer architecture** that cleanly separates concerns:

```
┌──────────────────────────────────────────────────────────────┐
│ Layer 1: USER API (AlgebraOfGraphics)                       │
│ ────────────────────────────────────────────────────────     │
│  • data(dataset)         - Attach data                       │
│  • mapping(:x :y)        - Specify aesthetics                │
│  • scatter({:alpha 0.5}) - Choose visual type                │
│  • * (multiply)          - Merge specifications              │
│  • + (add)               - Overlay layers                    │
│                                                              │
│  Example: (* (data penguins)                                │
│               (mapping :bill-length :bill-depth)             │
│               (scatter {:alpha 0.7}))                        │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ Layer 2: INTERMEDIATE REPRESENTATION (IR)                   │
│ ────────────────────────────────────────────────────────     │
│  • Layer             - User-facing spec (what + how)         │
│  • ProcessedLayer    - After transformations + grouping      │
│  • Entry             - Backend-agnostic plot spec (THE IR!)  │
│  • AxisEntries       - Entry + scales + axis config          │
│                                                              │
│  Schema validation: Malli ensures correctness at each step  │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ Layer 3: PROCESSING PIPELINE                                │
│ ────────────────────────────────────────────────────────     │
│  • normalize-data()           - Tablecloth/maps → dataset    │
│  • apply-transform()          - Stats (linear, smooth, etc.) │
│  • extract-column()           - Pull data by key             │
│  • identify-categorical()     - Find grouping variables      │
│  • generate-labels()          - Create axis labels           │
│  • layer->processed-layer()   - Apply pipeline               │
│  • processed-layer->entry()   - Final IR generation          │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│ Layer 4: BACKEND RENDERING (thi.ng/geom)                    │
│ ────────────────────────────────────────────────────────     │
│  • entry->thing-spec()   - Entry → thi.ng spec               │
│  • build-axis()          - Infer axes from data              │
│  • positional->thing()   - [[x...] [y...]] → [[x y] ...]    │
│  • svg-plot2d-*()        - Cartesian or polar rendering      │
│  • entry->svg()          - Generate final SVG + Kindly wrap  │
└──────────────────────────────────────────────────────────────┘
```

**Data Flow Example**:

```clojure
;; Input: User code
(* (data {:x [1 2 3] :y [4 5 6]})
   (mapping :x :y)
   (scatter {:alpha 0.7}))

;; After Layer 1: Layer map
{:transformation nil
 :data {:x [1 2 3] :y [4 5 6]}
 :positional [:x :y]
 :named {}
 :plottype :scatter
 :attributes {:alpha 0.7}}

;; After Layer 3: Entry IR
{:plottype :scatter
 :positional [[1 2 3] [4 5 6]]
 :named {:alpha 0.7}}

;; After Layer 4: SVG string (wrapped with kind/html)
"<svg width='600' height='400'>...</svg>"
```

---

## Layer 1: User API (AlgebraOfGraphics)

**Namespace**: `scicloj.tableplot.v1.aog.core`

The user-facing API provides composable functions for building visualizations.

### Core Constructors

#### `data(dataset)`

Attach data to a layer.

**Accepts**:
- tech.ml.dataset / tablecloth dataset
- Map of vectors: `{:x [1 2 3] :y [4 5 6]}`
- Vector of maps: `[{:x 1 :y 4} {:x 2 :y 5}]`

```clojure
(data penguins-dataset)
(data {:x [1 2 3] :y [4 5 6]})
(data [{:x 1 :y 4} {:x 2 :y 5}])
```

#### `mapping(positional... named)`

Specify aesthetic mappings (columns → visual properties).

**Positional** (order matters): `:x`, `:y`, `:z`
**Named** (map): `:color`, `:size`, `:shape`, `:col` (faceting), `:row` (faceting)

```clojure
;; Positional only
(mapping :bill-length :bill-depth)

;; Named only
(mapping {:color :species})

;; Mixed
(mapping :bill-length :bill-depth {:color :species :size :body-mass})

;; Faceting
(mapping :x :y {:col :island})
```

#### Plot Type Functions

**Basic plots**:
```clojure
(scatter {:alpha 0.5 :size 10})
(line {:width 2 :color "red"})
(bar {:opacity 0.8})
(boxplot)
(violin)
(heatmap)
```

**Statistical transformations**:
```clojure
(linear)              ; Linear regression
(smooth)              ; LOESS smoothing
(histogram {:bins 20}) ; Histogram with bins
(density)             ; Kernel density estimation
(frequency)           ; Count unique values
(expectation)         ; Conditional mean E[Y|X]
```

**Scale transformations**:
```clojure
(log-scale :y {:base 10})
(sqrt-scale :x)
(pow-scale :y 3)              ; Cubic scale
(scale-domain :x [0 100])     ; Custom domain
```

### Algebraic Operators

#### `*` - Merge (Cartesian Product)

Combines layer specifications by merging their fields.

**Merging rules**:
- `:data` - Rightmost overrides
- `:positional` - Concatenated
- `:named` - Merged (rightmost overrides)
- `:transformation` - Rightmost overrides
- `:plottype` - Rightmost overrides
- `:attributes` - Merged

```clojure
;; Build up a complete specification
(* (data penguins)
   (mapping :bill-length :bill-depth)
   (mapping {:color :species})
   (scatter {:alpha 0.7}))

;; Distributes across layers from +
(* (data penguins)
   (mapping :x :y)
   (+ (scatter) (linear)))  ; Both get data and mapping
```

#### `+` - Overlay

Creates a vector of layers to be drawn on the same plot.

```clojure
;; Points + trend line
(+ (scatter {:alpha 0.5})
   (linear))

;; Multiple series
(+ (scatter {:color "red"})
   (line {:color "blue"})
   (bar {:color "green"}))
```

### Drawing

#### `draw(layers opts)`

The final step: render layers to a visualization.

**Options**:
- `:backend` - `:vegalite`, `:plotly`, or `:thing-geom` (default: `:vegalite`)
- `:theme` - `:tableplot-subtle`, `:tableplot-balanced`, `:tableplot-bold`, `:ggplot2`, `:vega`
- `:width` - Width in pixels (default: 600)
- `:height` - Height in pixels (default: 400)
- `:polar` - Enable polar coordinates (thi.ng/geom only)

```clojure
;; Default (Vega-Lite)
(draw layers)

;; Custom theme and size
(draw layers {:theme :tableplot-bold
              :width 800
              :height 600})

;; thi.ng/geom with polar
(draw layers {:backend :thing-geom
              :polar true
              :width 600
              :height 600})
```

---

## Layer 2: Intermediate Representations

**Namespace**: `scicloj.tableplot.v1.aog.ir`

All IR structures are **plain maps** validated with **Malli schemas**.

### Schema 1: Layer

**User-facing layer specification** (before processing).

```clojure
{:transformation nil-or-fn    ; Statistical transform (or nil)
 :data any                     ; Dataset/maps/vectors (or nil)
 :positional [:col1 :col2]     ; Positional mappings
 :named {:color :species}      ; Named aesthetic mappings
 :plottype :scatter            ; Plot type (optional)
 :attributes {:alpha 0.5}}     ; Static visual attributes
```

**Example**:
```clojure
{:transformation :linear
 :data {:x [1 2 3] :y [4 5 6]}
 :positional [:x :y]
 :named {:color :group}
 :plottype :line
 :attributes {:alpha 0.8}}
```

### Schema 2: ProcessedLayer

**Layer after transformations and grouping**.

```clojure
{:primary {:color [:A :B :C]}      ; Categorical aesthetics (grouping)
 :positional [[1 2 3] [4 5 6]]     ; Actual data arrays
 :named {:marker-size [5 6 7]}     ; Named aesthetics with data
 :labels {:x "X Label" :y "Y"}     ; Axis/legend labels
 :scale-mapping {:color :color-1}  ; Aesthetic → scale ID
 :plottype :scatter                ; Resolved plot type
 :attributes {:alpha 0.7}}         ; Static attributes
```

**Purpose**: Contains everything needed for scale inference and entry generation.

### Schema 3: Entry ⭐

**THE INTERMEDIATE REPRESENTATION** - Backend-agnostic plot specification.

```clojure
{:plottype :scatter               ; One of: :scatter, :line, :bar, etc.
 :positional [[1 2 3] [4 5 6]]    ; Actual data in positional order
 :named {:marker-color [...]      ; Attribute → value
         :marker-size 10
         :opacity 0.3}}
```

**Supported plottypes**:
`:scatter`, `:line`, `:bar`, `:histogram`, `:heatmap`, `:surface`, `:contour`, `:density`, `:box`, `:violin`

**Key insight**: Entry contains **only data and rendering attributes**, no column references. This makes it **backend-independent**.

### Schema 4: AxisConfig

**Axis configuration** (for subplots/faceting).

```clojure
{:type :cartesian             ; :cartesian, :3d, :polar
 :position [0 0]              ; Grid position [row col]
 :attributes {:xlabel "X"     ; Axis-specific attributes
              :ylabel "Y"
              :xlim [0 10]}}
```

### Schema 5: CategoricalScale

**Categorical scale** (discrete values → visual attributes).

```clojure
{:id :color-scale-1
 :aesthetic :color
 :domain [:Adelie :Chinstrap :Gentoo]
 :range ["#1f77b4" "#ff7f0e" "#2ca02c"]}
```

### Schema 6: ContinuousScale

**Continuous scale** (continuous values → visual attributes).

```clojure
{:id :size-scale-1
 :aesthetic :size
 :domain [0 100]
 :range [5 20]}
```

### Schema 7: AxisEntries

**Complete specification for a single axis/subplot**.

```clojure
{:axis-config {...}              ; AxisConfig
 :entries [{...} {...}]          ; Vector of Entry maps
 :categorical-scales {...}       ; Scale ID → CategoricalScale
 :continuous-scales {...}        ; Scale ID → ContinuousScale
 :processed-layers [...]}        ; Original ProcessedLayers
```

**Purpose**: Everything needed to render one subplot, including scales for legend generation.

---

## Layer 3: Processing Pipeline

**Namespace**: `scicloj.tableplot.v1.aog.processing`

Converts `Layer` → `ProcessedLayer` → `Entry`.

### Pipeline Functions

#### `normalize-data(data)`

Convert various formats to tablecloth dataset.

**Accepts**:
- `nil` → `nil`
- tech.ml.dataset → pass-through
- `{:x [...] :y [...]}` → dataset
- `[{:x 1 :y 2} ...]` → dataset

#### `extract-column(data col-key)`

Pull a column as a vector.

```clojure
(extract-column dataset :bill-length)
;; => [39.1 39.5 40.3 ...]
```

#### `identify-categorical-aesthetics(layer data)`

Find which named aesthetics are categorical (for grouping).

**Categorical aesthetics**: `:color`, `:colour`, `:shape`, `:linetype`, `:group`

```clojure
(identify-categorical-aesthetics
  {:named {:color :species :size :body-mass}}
  dataset)
;; => [:species]  (not :body-mass, it's continuous)
```

#### `apply-transform(transform data x-col y-col group-cols)`

Apply statistical transformation.

**Transformations** (from `scicloj.tableplot.v1.aog.transforms`):
- `:linear` - Linear regression (grouped if group-cols provided)
- `:smooth` - LOESS smoothing
- `:density` - Kernel density estimation (adds `:density` column)
- `:histogram` - Binning (adds `:count` column)
- `:frequency` - Count unique values (adds `:count` column)
- `:expectation` - Conditional mean E[Y|X]

**Grouping**: If `group-cols` is non-empty, transformation is applied per group.

#### `layer->processed-layer(layer)`

**The main processing function**.

**Steps**:
1. Normalize data
2. Identify grouping columns
3. Apply transformation (if present)
4. Extract primary aesthetics (categorical)
5. Extract positional data (handling implicit columns like `:density`)
6. Extract named data
7. Generate labels
8. Generate scale mapping
9. Infer plottype

**Special handling for transformations**:
- `:density` creates `:density` column → used as implicit y
- `:histogram` creates `:count` column → used as implicit y
- `:frequency` creates `:count` column → used as implicit y

```clojure
(layer->processed-layer
  {:transformation :density
   :data {:values [1 2 3 4 5]}
   :positional [:values]
   :named {}
   :plottype :line})

;; Result: positional becomes [x-grid density-values]
;; (x-grid from KDE, density-values computed)
```

#### `processed-layer->entry(processed-layer)`

Convert ProcessedLayer to Entry (simple extraction).

```clojure
(processed-layer->entry processed-layer)
;; => {:plottype ... :positional ... :named ...}
```

#### `layers->entries(layers)`

**Convenience function**: Direct Layer → Entry pipeline.

```clojure
(layers->entries [layer1 layer2 layer3])
;; => [entry1 entry2 entry3]
```

---

## Layer 4: Backend Rendering (thi.ng/geom)

**Namespace**: `scicloj.tableplot.v1.aog.thing-geom`

Converts Entry IR to thi.ng/geom-viz specifications and renders to SVG.

### Architecture

```
Entry {:plottype :scatter          thi.ng/geom spec
      :positional [[x...] [y...]]}  {:x-axis ...
      :named {...}}                  :y-axis ...
                                     :data [{:values [[x y] ...]
                                             :layout viz/svg-scatter-plot
                                             :attribs {...}}]}
         ↓
    svg-plot2d-cartesian or
    svg-plot2d-polar
         ↓
    SVG string (wrapped with kind/html)
```

### Key Functions

#### `entry->thing-spec(entry)`

Convert Entry to thi.ng/geom-viz spec.

**Steps**:
1. Extract plottype and look up layout function (`viz/svg-scatter-plot`, etc.)
2. Convert data format: `[[x...] [y...]]` → `[[x1 y1] [x2 y2] ...]`
3. Build axes from data (infer domains and ranges)
4. Map AoG attributes → thi.ng SVG attributes
5. Build complete spec with `:x-axis`, `:y-axis`, `:grid`, `:data`

**Plottype mapping**:
```clojure
{:scatter viz/svg-scatter-plot
 :line viz/svg-line-plot
 :bar viz/svg-bar-plot
 :area viz/svg-area-plot
 :histogram viz/svg-bar-plot    ; Histogram → bar (data already binned)
 :density viz/svg-line-plot}    ; Density → line (KDE already computed)
```

**Attribute mapping**:
```clojure
;; AoG → thi.ng SVG
:color    → :stroke (lines) or :fill (scatter/bar)
:alpha    → :opacity
:linewidth → :stroke-width
:stroke   → :stroke (direct)
:fill     → :fill (direct)
```

#### `build-axis(data-array axis-type scale-type)`

Infer axis specification from data.

**Inputs**:
- `data-array`: Vector of numbers `[1 2 3 4 5]`
- `axis-type`: `:x` or `:y`
- `scale-type`: `:linear` or `:log`

**Output**: Axis spec with `:domain`, `:range`, `:pos`, `:major`, `:minor`

```clojure
(build-axis [1 2 3 4 5] :x :linear)
;; => {:domain [1 5]
;;     :range [50 550]      ; 50px left margin, 50px right margin
;;     :pos 350             ; Y position for X axis (bottom)
;;     :major 0.8           ; (5-1)/5 = 0.8
;;     :minor 0.2}          ; (5-1)/20 = 0.2
```

**Default plot area**: 600×400 with margins (50px all sides)
- X axis range: `[50 550]` (horizontal pixels)
- Y axis range: `[350 50]` (inverted for SVG coords, vertical pixels)

#### `positional->thing-values(positional)`

Convert columnar data to row-wise tuples.

```clojure
(positional->thing-values [[1 2 3] [4 5 6]])
;; => [[1 4] [2 5] [3 6]]
```

**Why?** thi.ng/geom-viz expects row-wise data points.

#### `entry->svg(entry opts)`

**Main rendering function** - Entry → SVG.

**Options**:
- `:width` - Width in pixels (default: 600)
- `:height` - Height in pixels (default: 400)
- `:polar` - Use polar coordinates (default: false)

**Steps**:
1. Convert entry to thing-spec
2. Choose plot function (`svg-plot2d-cartesian` or `svg-plot2d-polar`)
3. If polar, add `:origin` and `:circle` to spec
4. Call plot function to generate SVG structure
5. Serialize to SVG string
6. Wrap with `kind/html` for Clay rendering

```clojure
(entry->svg
  {:plottype :scatter
   :positional [[1 2 3] [4 5 6]]
   :named {:color "#e74c3c" :opacity 0.7}}
  {:width 600 :height 400})
;; => (kind/html "<svg width='600' height='400'>...</svg>")
```

#### `entries->svg(entries opts)`

**Multi-layer rendering** - Combine multiple entries on one plot.

**Strategy**: Merge data specs from all entries, use axes from first entry.

```clojure
(entries->svg
  [{:plottype :scatter :positional [[1 2 3] [4 5 6]] :named {...}}
   {:plottype :line :positional [[1 2 3] [4 5 6]] :named {...}}]
  {:width 600 :height 400})
```

**Note**: For more sophisticated multi-layer plots, domains should be merged. Current implementation uses first entry's axes.

### Polar Coordinates

**Enable polar mode** with `:polar true`.

**Changes**:
1. Uses `svg-plot2d-polar` instead of `svg-plot2d-cartesian`
2. Adds `:origin (v/vec2 (/ width 2) (/ height 2))` - Center point
3. Adds `:circle true` - Draw polar grid circles
4. Positional data interpreted as `[theta r]` instead of `[x y]`

**Example - Rose Curve**:
```clojure
(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (Math/cos (* 3 %))) theta))]
  (entry->svg
    {:plottype :line
     :positional [theta r]
     :named {:stroke "#e74c3c" :stroke-width 2}}
    {:polar true :width 600 :height 600}))
```

### Validation

#### `validate-entry(entry)`

Check if entry can be rendered with thi.ng/geom.

```clojure
(validate-entry entry)
;; => {:valid? true}
;; or {:valid? false :error "Unsupported plottype: :surface"}
```

**Currently unsupported**: `:surface`, `:contour`, `:box`, `:violin` (2D only)

---

## Complete Examples

### Example 1: Simple Scatter Plot

**Full pipeline from user code to SVG**:

```clojure
(require '[scicloj.tableplot.v1.aog.core :as aog])
(require '[scicloj.tableplot.v1.aog.processing :as processing])
(require '[scicloj.tableplot.v1.aog.thing-geom :as tg])

;; Step 1: Create layer with AoG API
(def layer
  (aog/* (aog/data {:x [1 2 3 4 5]
                    :y [2 4 3 5 6]})
         (aog/mapping :x :y)
         (aog/scatter {:alpha 0.7})))

;; Inspect layer
layer
;; => {:transformation nil
;;     :data {:x [1 2 3 4 5] :y [2 4 3 5 6]}
;;     :positional [:x :y]
;;     :named {}
;;     :plottype :scatter
;;     :attributes {:alpha 0.7}}

;; Step 2: Convert to entry
(def entry (processing/layer->entry layer))

;; Inspect entry
entry
;; => {:plottype :scatter
;;     :positional [[1 2 3 4 5] [2 4 3 5 6]]
;;     :named {:alpha 0.7}}

;; Step 3: Render to SVG
(tg/entry->svg entry {:width 600 :height 400})
;; => (kind/html "<svg width='600' height='400'>...</svg>")
```

### Example 2: Multi-Layer with Statistical Transform

**Scatter + Linear Regression**:

```clojure
(def layers
  (aog/* (aog/data {:x [1 2 3 4 5 6 7 8 9 10]
                    :y [2.1 3.9 6.2 7.8 10.1 12.3 13.9 16.2 17.8 20.1]})
         (aog/mapping :x :y)
         (aog/+ (aog/scatter {:alpha 0.6})
                (aog/linear))))

;; layers is now a vector of 2 layers
(count layers)
;; => 2

(first layers)
;; => {:transformation nil, :plottype :scatter, :attributes {:alpha 0.6}, ...}

(second layers)
;; => {:transformation :linear, :plottype :line, ...}

;; Convert to entries
(def entries (processing/layers->entries layers))

;; Render
(tg/entries->svg entries {:width 600 :height 400})
```

### Example 3: Polar Plot (Rose Curve)

**3-petaled rose: r = 5 × cos(3θ)**:

```clojure
(let [theta (vec (range 0 (* 2 Math/PI) 0.01))
      r (vec (map #(* 5 (Math/cos (* 3 %))) theta))
      layer (aog/* (aog/data {:theta theta :r r})
                   (aog/mapping :theta :r)
                   (aog/line {:stroke "#9b59b6" :stroke-width 2}))
      entry (processing/layer->entry layer)]
  (tg/entry->svg entry {:polar true :width 600 :height 600}))
```

### Example 4: Histogram

**Distribution of values**:

```clojure
(def layer
  (aog/* (aog/data {:values [1.2 1.5 1.8 2.1 2.3 2.5 2.7 2.9 3.1 3.3
                              3.5 3.7 3.9 4.1 4.3 4.5 4.7 4.9 5.1 5.3]})
         (aog/mapping :values)
         (aog/histogram {:bins 10})))

;; Histogram transform creates :count column
(def entry (processing/layer->entry layer))

entry
;; => {:plottype :bar
;;     :positional [[bin-centers] [counts]]
;;     :named {...}}

(tg/entry->svg entry {:width 600 :height 400})
```

### Example 5: Grouped Data (Color by Category)

**Automatic grouping by categorical aesthetic**:

```clojure
(def layer
  (aog/* (aog/data {:x [1 2 3 4 5 6]
                    :y [2 4 3 5 6 4]
                    :group ["A" "A" "B" "B" "A" "B"]})
         (aog/mapping :x :y {:color :group})
         (aog/scatter {:alpha 0.7})))

;; Processing identifies :group as categorical
(def processed (processing/layer->processed-layer layer))

(:primary processed)
;; => {:color ["A" "B"]}

;; Scale inference creates color scale
(require '[scicloj.tableplot.v1.aog.scales :as scales])
(def scale-info (scales/infer-scales [(processing/processed-layer->entry processed)]))

(:categorical-scales scale-info)
;; => {:color-scale {:id :color-scale
;;                   :aesthetic :color
;;                   :domain ["A" "B"]
;;                   :range ["#1f77b4" "#ff7f0e"]}}
```

---

## Best Practices

### 1. Data Format

**Always use vectors** for positional data (not lazy seqs or lists):

```clojure
;; Good
(let [x (vec (range 10))
      y (vec (map inc x))]
  ...)

;; Bad (lazy seq causes issues)
(let [x (range 10)
      y (map inc x)]
  ...)
```

### 2. Plot Sizes

**Recommended dimensions**:
- Thumbnails: 200×150
- Standard: 600×400
- Presentations: 1200×700
- Polar plots: Square aspect ratio (600×600)

### 3. Overlapping Points

**Use opacity** to prevent over-plotting:

```clojure
(aog/scatter {:alpha 0.5})  ; 50% transparent
```

### 4. Polar Polygons

**Close polar shapes** by repeating first value:

```clojure
(let [n 5
      theta (vec (map #(* % (/ (* 2 Math/PI) n)) (range (inc n)))) ; (inc n)!
      r [0.8 0.6 0.9 0.7 0.5 0.8]] ; First value repeated at end
  ...)
```

### 5. Color Accessibility

**Use distinct, accessible colors**:
- Blues: `#3498db`, `#2980b9`
- Greens: `#2ecc71`, `#27ae60`
- Reds: `#e74c3c`, `#c0392b`
- Purples: `#9b59b6`, `#8e44ad`
- Oranges: `#e67e22`, `#d35400`

### 6. Error Handling

**Validate before rendering**:

```clojure
(require '[scicloj.tableplot.v1.aog.ir :as ir])

;; Check if entry is valid
(ir/valid? ir/Entry entry)
;; => true or false

;; Get validation errors
(ir/explain ir/Entry entry)
;; => {:plottype ["missing required key"], ...}
```

### 7. Debugging

**Inspect intermediate stages**:

```clojure
;; Check layer structure
(clojure.pprint/pprint layer)

;; Check processed layer
(def processed (processing/layer->processed-layer layer))
(clojure.pprint/pprint processed)

;; Check entry
(def entry (processing/processed-layer->entry processed))
(clojure.pprint/pprint entry)

;; Check thing-spec (before SVG)
(def spec (tg/entry->thing-spec entry))
(clojure.pprint/pprint spec)
```

### 8. Coordinate Systems

**Choose the right system**:
- Cartesian (default): Standard x-y plots
- Polar (`:polar true`): Angles and radii (radar charts, rose curves)

**Polar data format**: `[theta r]` where theta is in radians, r is distance from origin.

### 9. Statistical Transforms

**Understand implicit columns**:
- `:density` → Creates `:density` column (y-axis)
- `:histogram` → Creates `:count` column (y-axis)
- `:frequency` → Creates `:count` column (y-axis)

**Grouping**: Categorical aesthetics (`:color`, `:shape`) automatically group data for transformations.

### 10. Performance

**thi.ng/geom is best for**:
- Small to medium datasets (< 10,000 points)
- Static output (no interaction needed)
- Polar/parametric plots
- Server-side rendering

**For large datasets or interactivity**, use Vega-Lite or Plotly.js backends.

---

## Summary

The AoG + thi.ng/geom system provides a **powerful, pure-Clojure** approach to data visualization:

1. **Layer 1 (AoG API)** - Algebraic composition with `*` and `+`
2. **Layer 2 (IR)** - Backend-agnostic Entry format with Malli validation
3. **Layer 3 (Processing)** - Statistical transforms and scale inference
4. **Layer 4 (thi.ng/geom)** - Native SVG rendering with excellent polar support

**Key strengths**:
- ✅ Compositional and declarative
- ✅ Backend independence (swap Vega-Lite ↔ Plotly ↔ thi.ng/geom)
- ✅ Type safety (Malli schemas)
- ✅ Pure Clojure (no JavaScript for thi.ng/geom)
- ✅ Polar coordinate excellence

**Next steps**:
- Explore the 60+ test examples in `notebooks/tableplot_book/thing_geom_*.clj`
- Try the existing draft reference: `thing_geom_complete_reference.clj`
- Experiment with polar plots and mathematical curves
- Integrate with your own datasets

**Resources**:
- thi.ng/geom docs: https://github.com/thi-ng/geom
- AoG examples: `notebooks/tableplot_book/aog_demo.clj`
- Project summary: `PROJECT_SUMMARY.md`
