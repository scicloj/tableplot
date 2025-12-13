# Dataflow Design Analysis: Comprehensive Reference

This document provides a detailed analysis of Tableplot's dataflow system for future work on grammar-of-graphics implementations in Clojure, including conversion of systems like Julia's Algebra of Graphics.

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [The Six-Rule Foundation](#the-six-rule-foundation)
3. [Implementation Deep Dive](#implementation-deep-dive)
4. [Tableplot Patterns in Practice](#tableplot-patterns-in-practice)
5. [Hanami Research Findings](#hanami-research-findings)
6. [Grammar-of-Graphics Challenges](#grammar-of-graphics-challenges)
7. [Comparison Framework](#comparison-framework)
8. [Conversion Patterns](#conversion-patterns)
9. [Alternative Design Considerations](#alternative-design-considerations)
10. [Open Questions and Limitations](#open-questions-and-limitations)
11. [Future Directions](#future-directions)

---

## Executive Summary

Tableplot implements a **term rewriting system** with explicit dependency tracking for grammar-of-graphics visualizations. The system has two layers:

1. **Six core substitution rules** (from Hanami) handle template transformation through fixpoint computation
2. **Declared dependencies** (Tableplot addition) make parameter requirements explicit and enable automatic caching

This approach solves fundamental GG challenges:
- Parameter threading (`:dataset`, `:x-column`, `:y-column` flow automatically)
- Statistical pipelines (transformations declare dependencies, compute once, cache results)
- Layered composition (shared and unique parameters merge naturally)
- Hierarchical parameter scoping (nested defaults create local contexts)

The system is more explicit than Hanami, simpler than Vega-Lite, and more declarative than imperative ggplot2 implementations.

---

## The Six-Rule Foundation

### Formal Specification

Given a term `T` and an environment `E` (a map from keys to values), the substitution function `xform(T, E)` applies these rules:

**Rule 1: Lookup** — If `T` is a key in `E`, return `xform(E[T], E)` (recursive)

**Rule 2: Identity** — If `T` is a key not in `E`, return `T` (fixpoint)

**Rule 3: Function Application** — If `T` is a function, return `xform(T(E), E)` (apply function to entire environment, then recurse)

**Rule 4: Collection Recursion** — If `T` is a map/vector, return the same structure with `xform` applied to each element

**Rule 5: Template Defaults** — If `T` is a map with `::ht/defaults`, merge those into `E` for substitutions within `T`

**Rule 6: Value Pass-through** — Otherwise, return `T` (fixpoint for primitives, datasets, etc.)

### Fixpoint Semantics

The system computes **fixpoints** through repeated application:

```clojure
;; Example: :a → :B → :C → 10 → 10 (fixpoint)
(xform/xform
 {:a :B}
 {:B :C, :C 10})
;; => {:a 10}

;; The reduction sequence:
;; {:a :B}
;; {:a :C}    (after one step: :B → :C)
;; {:a 10}    (after two steps: :C → 10)
;; {:a 10}    (after three steps: 10 → 10, fixpoint!)
```

**Fixpoint detection for keys**:
```clojure
;; Self-reference is a fixpoint
(xform/xform
 {:x :X}
 {:X :X})  ;; :X maps to itself
;; => {:x :X}  ;; Can't reduce further

;; Missing key is a fixpoint
(xform/xform
 {:y :Y}
 {:X 10})  ;; :Y not in environment
;; => {:y :Y}  ;; Can't reduce further
```

### Function Application Examples

**Simple function**:
```clojure
(xform/xform
 {:greeting :Greeting}
 {:Name "Alice"
  :Greeting (fn [{:keys [Name]}]
              (str "Hello, " Name "!"))})
;; => {:greeting "Hello, Alice!"}

;; Reduction sequence:
;; 1. {:greeting :Greeting}
;; 2. {:greeting (fn [...] ...)}  (lookup :Greeting)
;; 3. {:greeting "Hello, Alice!"}  (apply function to environment)
;; 4. {:greeting "Hello, Alice!"}  (string is fixpoint)
```

**Computed dependencies**:
```clojure
(xform/xform
 {:result :C}
 {:A 10
  :B (fn [{:keys [A]}] (* A 2))
  :C (fn [{:keys [B]}] (+ B 5))})
;; => {:result 25}

;; But note: :B isn't computed until :C requests it!
;; The function receives the environment, but functions
;; aren't called eagerly - only when their key is looked up.
```

### Nested Defaults

**Local environment extension**:
```clojure
(xform/xform
 {:title :Title
  :section {:heading :Heading
            ::ht/defaults {:Heading "Default Heading"}}
  ::ht/defaults {:Title "Default Title"}})
;; => {:title "Default Title"
;;     :section {:heading "Default Heading"}}

;; The environment for :section includes both
;; the outer defaults {:Title "Default Title"}
;; and the inner defaults {:Heading "Default Heading"}
```

**User overrides cascade down**:
```clojure
(xform/xform
 {:section {:heading :Heading
            ::ht/defaults {:Heading "Default"}}}
 :Heading "User Heading")
;; => {:section {:heading "User Heading"}}

;; User substitutions take precedence over nested defaults
```

### Recursive Removal with `RMV`

**Optional parameters**:
```clojure
(xform/xform
 {:title :Title
  :subtitle :Subtitle
  :footer :Footer}
 {:Title "My Chart"
  :Subtitle hc/RMV
  :Footer hc/RMV})
;; => {:title "My Chart"}
;; Subtitle and Footer removed
```

**Recursive cleanup**:
```clojure
(xform/xform
 {:plot {:layout {:title {:text :Title
                          :font {:size :Size
                                 :color :Color}}}}
  ::ht/defaults {:Title "Chart"
                 :Size hc/RMV
                 :Color hc/RMV}})
;; => {:plot {:layout {:title {:text "Chart"}}}}
;; Empty :font map removed, empty maps cleaned up recursively
```

**Conditional inclusion**:
```clojure
(xform/xform
 {:subtitle :Subtitle
  ::ht/defaults
  {:ShowSubtitle true
   :Subtitle (fn [{:keys [ShowSubtitle]}]
               (if ShowSubtitle "A subtitle" hc/RMV))}}
 :ShowSubtitle false)
;; => {}
;; Function returns RMV when condition false
```

---

## Implementation Deep Dive

### xform.clj: The Core Rewriter

```clojure
(defn xform
  "An adapted port of Hanami's xform function"
  ([spec xkv]
   (let [;; Setup defaults
         xkv (if (not (xkv ::hc/spec)) (assoc xkv ::hc/spec spec) xkv)
         defaults @hc/_defaults
         use-defaults? (get xkv ::hc/use-defaults? (defaults ::hc/use-defaults?))
         xkv (if use-defaults? (merge defaults xkv) xkv)
         
         ;; Handle ::ht/defaults in spec
         template-defaults (if (map? spec) (spec ::ht/defaults) false)
         spec (if template-defaults (dissoc spec ::ht/defaults) spec)
         xkv (if template-defaults
               (merge xkv template-defaults (xkv ::hc/user-kvs))
               xkv)]
     
     ;; Transform with Specter
     (sp/transform
      sp/ALL
      (fn [v]
        (if (coll? v)
          ;; Collection: recurse and clean empty
          (let [xv (xform v xkv)
                rmv? (xkv ::hc/rmv-empty?)]
            (if (seq xv) xv (if rmv? hc/RMV xv)))
          
          ;; Non-collection: apply substitution rules
          (let [subval (get xkv v v)  ;; Rule 1 & 2: Lookup or identity
                subval (if (fn? subval) (subval xkv) subval)  ;; Rule 3: Apply function
                subkeyfn (@hc/subkeyfns v)  ;; Hanami type dispatch (not used in Tableplot)
                subval (if subkeyfn (subkeyfn xkv v subval) subval)]
            (cond
              ;; Rule 2: Fixpoint detection
              (= v subval) v

              ;; Rule 6: Dataset pass-through (Tableplot addition)
              (or (= v hc/data-key)
                  (-> subval type (= tech.v3.dataset.impl.dataset.Dataset)))
              subval

              ;; Continue reduction for strings and primitives
              (or (string? subval) (not (coll? subval)))
              (recur subval)

              ;; Collection: recurse with cleanup
              :else
              (let [xv (xform subval xkv)
                    rmv? (xkv ::hc/rmv-empty?)]
                (if (seq xv) xv (if rmv? hc/RMV xv)))))))
      spec)))

  ;; Multi-arity for convenience
  ([spec k v & kvs]
   (let [user-kv-map (into {k v}
                           (->> kvs (partition-all 2)
                                (mapv (fn [[k v]] [k v]))))
         start-kv-map (assoc user-kv-map ::hc/user-kvs user-kv-map)]
     (xform spec start-kv-map)))

  ([spec] (xform spec {})))
```

**Key implementation details**:

1. **Specter's `sp/ALL`** transforms every element in nested collections
2. **Recursive termination** via `(= v subval)` - if value doesn't change, stop
3. **Dataset special case** - datasets pass through unchanged (prevent deep recursion into data)
4. **Empty collection cleanup** - controlled by `:rmv-empty?` flag
5. **User kvs tracking** - `::hc/user-kvs` preserves user substitutions to override nested defaults

### dag.clj: Dependency Resolution (Rule 7)

**The core function**: `fn-with-deps-keys`

```clojure
(defn fn-with-deps-keys
  "Given a set of dependency keys and a submap function,
  create a submap function that first makes sure
  that the xform results for these keys are available."
  [doc dep-ks f]
  (vary-meta
   (fn [submap]
     (->> dep-ks
          (map (fn [k]
                 [k (cached-xform-k k submap)]))  ;; Resolve each dependency
          (into submap)  ;; Merge resolved values into environment
          f))  ;; Call function with augmented environment
   assoc
   ::dep-ks dep-ks  ;; Store dependency metadata
   :doc doc))
```

**How it works**:

1. When the function is called, it receives the substitution map (environment)
2. For each declared dependency `k` in `dep-ks`:
   - Call `cached-xform-k k submap` to resolve that key
   - Add the resolved value to the environment
3. Pass the augmented environment to the actual function `f`
4. Store dependency information in function metadata for introspection

**The macro interface**:

```clojure
(defmacro fn-with-deps
  [doc dep-symbols & forms]
  `(fn-with-deps-keys
    ~doc
    ~(mapv #(keyword (name %)) dep-symbols)  ;; Convert symbols to keywords
    (fn [{:keys ~dep-symbols}]  ;; Destructure in function
      ~@forms)))

(defmacro defn-with-deps
  [fsymbol doc dep-symbols & forms]
  `(def ~fsymbol
     (fn-with-deps ~doc ~dep-symbols ~@forms)))
```

**Example usage**:

```clojure
(dag/defn-with-deps area->radius
  "Compute radius from area"
  [Area]  ;; Depends on :Area
  (Math/sqrt (/ Area Math/PI)))

;; Expands to:
(def area->radius
  (fn-with-deps-keys
   "Compute radius from area"
   [:Area]
   (fn [{:keys [Area]}]
     (Math/sqrt (/ Area Math/PI)))))

;; Metadata inspection:
(:scicloj.tableplot.v1.dag/dep-ks (meta area->radius))
;; => [:Area]
```

**Dependency chains**:

```clojure
(dag/defn-with-deps radius->circumference
  "Compute circumference from radius"
  [Radius]
  (* 2 Math/PI Radius))

(xform/xform
 {:circumference :Circumference
  ::ht/defaults
  {:Area 100
   :Radius area->radius  ;; Depends on :Area
   :Circumference radius->circumference}}  ;; Depends on :Radius
 )
;; Execution order:
;; 1. :Area → 100 (lookup)
;; 2. :Radius → (area->radius {:Area 100, ...}) → 5.64...
;; 3. :Circumference → (radius->circumference {:Radius 5.64..., ...}) → 35.45...
```

### cache.clj: Memoization

```clojure
(ns scicloj.tableplot.v1.cache
  (:require [scicloj.tableplot.v1.palette :as palette]))

(def ^:dynamic *cache (atom {}))

(defmacro with-clean-cache
  "Execute body with a fresh cache"
  [& body]
  `(binding [*cache (atom {})]
     ~@body))
```

**How caching works**:

```clojure
(defn cached-xform-k
  [k submap]
  (let [id [k submap]]  ;; Cache key is [key, entire-substitution-map]
    (if-let [result (@*cache id)]
      result  ;; Cache hit
      (let [computed-result (xform-k k submap)]
        (swap! *cache assoc id computed-result)  ;; Store in cache
        computed-result))))
```

**Why cache on `[k submap]`**:

The cache key includes both the substitution key and the entire environment. This means:

1. Same key with different environments → separate cache entries
2. Multiple references to same key in same environment → computed once

**Example**:

```clojure
(cache/with-clean-cache
  (xform/xform
   {:result :C
    ::ht/defaults
    {:A (fn [_] (prn :computing-A) 10)
     :B (dag/fn-with-deps nil [A] (prn :computing-B) (* A 2))
     :C (dag/fn-with-deps nil [A B] (prn :computing-C) (+ A B))}}))

;; Prints:
;; :computing-A
;; :computing-B
;; :computing-C

;; Note: :A computed only once, even though both :B and :C need it!
```

**Palette assignment caching**:

```clojure
(defn cached-assignment
  "Given a value and a palette, assign a color/size/symbol
  consistently across calls"
  [v palette cache-key]
  (let [id [cache-key v]]
    (if-let [result (@*cache id)]
      result
      (let [assignment (palette/assign v palette)]
        (swap! *cache assoc id assignment)
        assignment))))
```

This ensures categorical variables get consistent visual encodings throughout the visualization.

---

## Tableplot Patterns in Practice

### The Substitution Key Hierarchy

Tableplot's `plotly.clj` defines ~90 substitution keys organized hierarchically:

```clojure
(def standard-defaults
  [;; Data pipeline
   [:=stat :=dataset
    "The data resulting from a possible statistical transformation."]
   [:=dataset hc/RMV
    "The data to be plotted."]
   
   ;; Aesthetic mappings
   [:=x :x "The column for the x axis."]
   [:=x-after-stat :=x "The column for the x axis to be used after :=stat."]
   [:=y :y "The column for the y axis."]
   [:=y-after-stat :=y "The column for the y axis to be used after :=stat."]
   
   ;; Optional aesthetics
   [:=color hc/RMV "The column to determine the color of marks."]
   [:=size hc/RMV "The column to determine the size of marks."]
   [:=symbol hc/RMV "The column to determine the symbol of marks."]
   
   ;; Type inference
   [:=x-type (submap->field-type :=x) "The field type of the column..."]
   [:=color-type (submap->field-type :=color) "The field type of the column..."]
   
   ;; Grouping
   [:=inferred-group submap->group "A list of columns to be used for grouping..."]
   [:=group :=inferred-group "A list of columns..., user override..."]
   
   ;; Plotly.js mappings
   [:=mode submap->mode "The Plotly.js mode used in a trace."]
   [:=type submap->type "The Plotly.js type used in a trace."]
   
   ;; Output
   [:=layers [] "A vector of all layers in the plot..."]
   [:=traces submap->traces "A vector of all Plotly.js traces..."]
   [:=layout submap->layout "The layout part of the resulting Plotly.js specification."]
   
   ;; ... ~70 more keys
   ])
```

### Data Pipeline Pattern

**The flow**: `=dataset → =stat → =x-after-stat → =x-for-trace`

```clojure
;; Layer definition
(def layer-base
  {:dataset :=stat  ;; Use transformed data
   :x :=x-after-stat  ;; Use column after stat transformation
   :y :=y-after-stat
   ;; ...
   })

;; Statistical transformation
(dag/defn-with-deps histogram-stat
  "Compute histogram bins"
  [=dataset =x =histogram-nbins]  ;; Declares what it needs
  (-> =dataset
      (compute-histogram =x =histogram-nbins)))

;; User code
(-> dataset
    (base)
    (layer-histogram {:=x :temperature
                      :=histogram-nbins 20}))

;; What happens:
;; 1. :=dataset → dataset (user provided)
;; 2. :=x → :temperature (user provided)
;; 3. :=histogram-nbins → 20 (user provided)
;; 4. :=stat → histogram-stat → (computed histogram) [cached]
;; 5. :=x-after-stat → :=x → :temperature
;; 6. layer :dataset → :=stat → (computed histogram)
;; 7. layer :x → :=x-after-stat → :temperature
```

### Type Inference Pattern

```clojure
(defn submap->field-type
  "Infer field type from column"
  [colname-key]
  (dag/fn-with-deps
   nil
   [=dataset]  ;; Needs the dataset to inspect column
   (fn [{:keys [=dataset], :as submap}]
     (when-let [colname (get submap colname-key)]
       (let [column (get =dataset colname)]
         (cond
           (-> column meta :categorical-data) :nominal
           (tcc/typeof? column :numerical) :quantitative
           :else :nominal))))))

;; Usage:
[:=x-type (submap->field-type :=x)
 "The field type of the column used to determine the x axis."]

;; This creates a function that:
;; 1. Looks up :=x in the environment → :temperature
;; 2. Looks up :=dataset in the environment → <dataset>
;; 3. Gets the :temperature column from dataset
;; 4. Inspects metadata/type → :quantitative
```

### Grouping and Color Assignment

```clojure
(dag/defn-with-deps submap->group
  "Infer grouping columns from aesthetics"
  [=color =size =symbol]
  ;; Only nominal (categorical) aesthetics create groups
  (let [grouping-aesthetics
        (cond-> []
          (and =color (= (submap->field-type-after-stat :=color) :nominal))
          (conj =color)
          
          (and =size (= (submap->field-type-after-stat :=size) :nominal))
          (conj =size)
          
          (and =symbol)
          (conj =symbol))]
    (when (seq grouping-aesthetics)
      grouping-aesthetics)))

;; Then in trace generation:
(dag/defn-with-deps submap->traces
  [=layers]
  (->> =layers
       (mapcat
        (fn [{:keys [inferred-group dataset color color-type] :as layer}]
          (let [group-kvs (if inferred-group
                            (tc/group-by dataset inferred-group {:result-type :as-map})
                            {nil dataset})]
            (->> group-kvs
                 (map (fn [[group-key group-dataset]]
                        ;; Assign colors from palette
                        (let [marker (when color
                                       (case color-type
                                         :nominal {:color (cache/cached-assignment
                                                          (get group-key color)
                                                          colors-palette
                                                          ::color)}
                                         :quantitative {:color (-> color group-dataset vec)}))]
                          ;; ... build trace
                          )))))))))
```

**What this achieves**:

1. If `:=color` maps to a nominal column (e.g., `:species`), create groups
2. For each group, assign a color from the palette (cached for consistency)
3. For quantitative color, pass the column values directly to Plotly

### Layer Composition Pattern

```clojure
(defn layer
  "Add a layer to a template"
  [dataset-or-template layer-spec submap]
  (let [template (if (tc/dataset? dataset-or-template)
                   (base dataset-or-template)
                   dataset-or-template)
        
        ;; Each layer gets its own nested environment
        layer-with-submap (-> layer-spec
                              (update ::ht/defaults merge submap))]
    
    ;; Add to layers vector
    (-> template
        (update-in [::ht/defaults :=layers]
                   (fnil conj [])
                   layer-with-submap))))

;; Example:
(-> dataset
    (base {:=x :year})  ;; Shared parameter
    (layer-point {:=y :temp})  ;; Layer-specific parameter
    (layer-line {:=y :temp-smooth}))  ;; Different y for this layer

;; Each layer has environment:
;; {:=dataset dataset
;;  :=x :year          ;; shared from base
;;  :=y :temp          ;; layer-specific (first layer)
;;  ;; or
;;  :=y :temp-smooth   ;; layer-specific (second layer)
;;  ...}
```

### The `base` Function

```clojure
(defn base
  ([dataset-or-template]
   (base dataset-or-template {}))
  
  ([dataset-or-template submap]
   (if (tc/dataset? dataset-or-template)
     (base dataset-or-template view-base submap)
     ;; Template case
     (-> dataset-or-template
         (update ::ht/defaults merge submap)
         (assoc :kindly/f #'plotly-xform)
         kind/fn)))
  
  ([dataset template submap]
   (-> template
       (update ::ht/defaults merge
               standard-defaults-map  ;; All 90 default substitutions
               {:=dataset dataset})
       (base submap))))

;; What view-base looks like:
(def view-base
  {:data :=traces
   :layout :=layout})

;; So (base dataset) creates:
{:data :=traces
 :layout :=layout
 :kindly/f #'plotly-xform  ;; Tells Clay how to render
 ::ht/defaults {... all 90 standard defaults ...
                :=dataset dataset}}
```

### Statistical Layer Pattern

```clojure
(defn layer-smooth
  [dataset-or-template submap]
  (layer dataset-or-template
         layer-base
         (merge {:=stat smooth-stat  ;; Override the stat
                 :=mark :line}
                submap)))

(dag/defn-with-deps smooth-stat
  "Fit a regression model and generate predictions"
  [=dataset =x =predictors =design-matrix =model-options =group]
  (if =group
    ;; Grouped: fit separate model for each group
    (-> =dataset
        (tc/group-by =group)
        (tc/aggregate
         (fn [group-ds]
           (fit-and-predict group-ds =x =predictors =design-matrix =model-options)))
        (tc/ungroup))
    ;; Ungrouped: single model
    (fit-and-predict =dataset =x =predictors =design-matrix =model-options)))
```

**Key insight**: Statistical transformations are just functions with declared dependencies. The dataflow system handles:
- Computing the stat only when needed
- Caching the result
- Respecting grouping variables
- Passing the transformed data to the layer

---

## Hanami Research Findings

### Template Usage Patterns

Hanami's codebase reveals sophisticated patterns:

**1. Recursive Cleanup with `hc/RMV`**

```clojure
;; From Hanami's common.clj
(def _mark-color "green")
(def _mark-size 30)

;; In a template:
{:mark {:type :point
        :size :SIZE
        :color :COLOR}
 ::ht/defaults {:SIZE _mark-size
                :COLOR RMV}}  ;; Optional color

;; After xform with no :COLOR provided:
{:mark {:type :point
        :size 30}}  ;; :color removed, empty :mark not removed
```

**2. Nested Defaults**

```clojure
;; From Hanami templates
(def point-chart
  {:mark :MARK
   :encoding {:x {:field :X, :type :XTYPE}
              :y {:field :Y, :type :YTYPE}}
   ::ht/defaults {:MARK {:type :point
                         :size :SIZE
                         ::ht/defaults {:SIZE 30}}}})

;; User provides:
{:X "x", :Y "y", :XTYPE "quantitative", :YTYPE "quantitative"}

;; Result:
{:mark {:type :point, :size 30}
 :encoding {:x {:field "x", :type "quantitative"}
            :y {:field "y", :type "quantitative"}}}
```

**3. Diamond Dependencies**

```clojure
;; Complex reference graph in Hanami
{:data :DATA
 :transform :TRANSFORM
 :mark :MARK
 ::ht/defaults
 {:DATA :USERDATA  ;; DATA references USERDATA
  :USERDATA {:url "data.json"}  ;; Could be overridden
  :TRANSFORM [{:filter :FILTER}  ;; TRANSFORM references FILTER
              {:calculate :CALCULATE}]  ;; and CALCULATE
  :MARK {:type :point
         :size :SIZE  ;; MARK references SIZE
         :color :COLOR}}}  ;; and COLOR

;; Multiple paths to same values - all resolved via fixpoint
```

**4. Type-Based Polymorphism (not used in Tableplot)**

```clojure
;; Hanami's subkeyfns allow type-based dispatch
(swap! hc/subkeyfns assoc
       :DATA
       (fn [xkv k v]
         (cond
           (string? v) {:url v}  ;; String → URL reference
           (vector? v) {:values v}  ;; Vector → inline data
           :else v)))

;; User can write:
{:data :DATA
 ::ht/defaults {:DATA "data.json"}}

;; Or:
{:data :DATA
 ::ht/defaults {:DATA [{:x 1 :y 2} {:x 2 :y 4}]}}

;; Tableplot deliberately avoids this for explicitness
```

### Key Differences: Hanami vs Tableplot

| Feature | Hanami | Tableplot |
|---------|--------|-----------|
| **Dependency Tracking** | Implicit (through substitution) | Explicit (`dag/fn-with-deps`) |
| **Type Dispatch** | Yes (`subkeyfns`) | No (explicit functions) |
| **Caching** | None (recompute each time) | Built-in (`cache/with-clean-cache`) |
| **Introspection** | Must run xform to know deps | Deps in metadata |
| **Target Domain** | Vega-Lite (declarative JSON) | Plotly.js (imperative JSON) |
| **Data Handling** | Passes through | Special handling for datasets |

**Why Tableplot diverges**:

1. **Grammar-of-graphics needs statistical transformations** - These are expensive and need caching
2. **Plotly.js is less declarative than Vega-Lite** - More imperative trace construction
3. **Explicit dependencies enable tooling** - Can generate dependency graphs, validate requirements
4. **Type dispatch adds magic** - Harder to debug "what substitution happened?"

---

## Grammar-of-Graphics Challenges

### The Core Problems

Every GG system must solve these:

#### 1. Parameter Threading

**Problem**: How do you specify `:x` → `:year` once and have it flow to:
- Scale domain computation
- Axis labels
- Statistical transformations
- Multiple layers
- Facet titles

**Imperative solution (ggplot2 style)**:
```python
# Each component must explicitly extract parameters
plot = Plot()
plot.data = dataset
plot.x_column = "year"
plot.add_layer(PointLayer(plot.data, plot.x_column, ...))
plot.add_layer(LineLayer(plot.data, plot.x_column, ...))
plot.x_axis = Axis(title=plot.x_column, domain=compute_domain(plot.data[plot.x_column]))
```

**Declarative solution (Vega-Lite style)**:
```json
{
  "data": {"url": "data.json"},
  "mark": "point",
  "encoding": {
    "x": {"field": "year", "type": "quantitative"}
  }
}
// Vega-Lite infers scales, axes, domains automatically
```

**Tableplot solution (dataflow)**:
```clojure
(-> dataset
    (base {:=x :year})  ;; Specify once
    (layer-point {})    ;; :=x flows automatically
    (layer-line {}))    ;; :=x flows here too

;; Under the hood:
;; - :=x → :year (user substitution)
;; - :=x-type → (infer-type dataset :year) → :quantitative
;; - :=x-domain → (compute-domain dataset :year)
;; - :=x-axis-title → :year (default) or user override
;; All through term rewriting!
```

#### 2. Statistical Transformations

**Problem**: How do you:
- Apply expensive computation (smoothing, binning, aggregation)
- Use the result in visualization
- Ensure it only runs once
- Respect grouping variables

**ggplot2 solution**:
```r
# stats are special layer types
ggplot(data, aes(x=x, y=y)) +
  geom_smooth()  # stat_smooth is built into geom

# The stat layer knows to:
# 1. Group by color/facet variables
# 2. Fit model for each group
# 3. Generate prediction data
# 4. Pass to geom for rendering
```

**Tableplot solution**:
```clojure
(dag/defn-with-deps smooth-stat
  [=dataset =x =y =group]  ;; Explicit dependencies
  (if =group
    (-> =dataset
        (tc/group-by =group)
        (tc/aggregate fit-model))
    (fit-model =dataset)))

;; In layer-smooth:
{:=stat smooth-stat  ;; Override default :=stat
 :=mark :line}

;; The system:
;; 1. Sees layer needs :=stat
;; 2. Finds smooth-stat function
;; 3. Checks smooth-stat dependencies: [=dataset =x =y =group]
;; 4. Resolves each dependency (with caching)
;; 5. Calls smooth-stat
;; 6. Caches result
;; 7. Layer uses cached result
```

#### 3. Layered Composition

**Problem**: Multiple layers share some parameters but have unique parameters.

**Vega-Lite solution**: Layering via array
```json
{
  "data": {"url": "data.json"},
  "layer": [
    {"mark": "point", "encoding": {"x": {...}, "y": {...}}},
    {"mark": "line", "encoding": {"x": {...}, "y": {...}, "color": {...}}}
  ]
}
// Each layer repeats shared parameters
```

**Tableplot solution**: Pipeline with environment inheritance
```clojure
(-> dataset
    (base {:=x :year          ;; Shared
           :=y :temperature})  ;; Shared
    (layer-point {:=color :station})  ;; Unique to this layer
    (layer-line {:=color :station
                 :=group [:station]}))  ;; Unique + explicit grouping

;; Each layer inherits base environment, adds its own
```

#### 4. Faceting and Grouping

**Problem**: Split visualization into small multiples while maintaining:
- Consistent scales across facets
- Correct statistical computations per group
- Shared parameters with facet-specific overrides

**Tableau solution**: Drag dimensions to Rows/Columns shelf - automatic faceting

**Tableplot potential solution** (not yet implemented):
```clojure
(-> dataset
    (base {:=x :year, :=y :temperature})
    (layer-point {})
    (facet-grid {:=facet-row :region
                 :=facet-col :season}))

;; Would need:
;; 1. Split dataset by facet variables
;; 2. Compute shared scale domains across all facets
;; 3. Render each facet with local data but shared scales
;; 4. Nested defaults per facet for overrides

;; Implementation sketch:
(dag/defn-with-deps faceted-plots
  [=dataset =facet-row =facet-col =layers]
  (let [facet-groups (tc/group-by =dataset [=facet-row =facet-col])
        shared-domains (compute-shared-domains facet-groups)]
    (for [[facet-key facet-data] facet-groups]
      {:data facet-data
       :layers =layers
       :title (format-facet-title facet-key)
       ::ht/defaults {:=x-domain shared-domains
                      :=y-domain shared-domains}})))
```

### Why Dataflow Solves These

**Parameter threading**: Keys flow through substitution automatically

**Statistical transformations**: Functions with declared dependencies + caching

**Layered composition**: Nested defaults create layered environments

**Faceting**: Each facet gets its own environment with shared base

The key insight: **These aren't special features, they're natural consequences of term rewriting with environments.**

---

## Comparison Framework

For evaluating GG systems and planning conversions:

### Dimensions of Comparison

#### 1. **Parameterization Model**

How are parameters specified and propagated?

- **Global state** (Matplotlib): `plt.xlabel()`, `plt.ylabel()` modify global current axes
- **Object methods** (ggplot2): `plot + labs(x="Year")` chains method calls
- **Declarative spec** (Vega-Lite): JSON object with nested parameter structure
- **Environment substitution** (Tableplot): Term rewriting with key-value environment

#### 2. **Composition Model**

How are multiple visual elements combined?

- **Imperative layering** (Matplotlib): `ax.plot()`, `ax.scatter()` called sequentially
- **Algebraic operators** (ggplot2, Algebra of Graphics): `+` operator for additive composition
- **Array of specs** (Vega-Lite): `"layer": [...]` with repeated parameters
- **Pipeline with inheritance** (Tableplot): `->` threading with environment inheritance

#### 3. **Statistical Integration**

How are data transformations integrated?

- **Manual** (Matplotlib): User computes, passes result
- **Stat layers** (ggplot2): Special geom/stat pairs
- **Transform array** (Vega-Lite): Declarative transform specifications
- **Dependency functions** (Tableplot): Functions with explicit deps, cached results

#### 4. **Type System**

How are data types handled?

- **Duck typing** (Matplotlib): Accepts anything, user ensures correctness
- **Factor/numeric** (ggplot2): R's type system for nominal/continuous
- **Field types** (Vega-Lite): "quantitative", "nominal", "temporal", "ordinal"
- **Inferred types** (Tableplot): Inspect column metadata/dtypes

#### 5. **Extensibility**

How do users add custom elements?

- **Functions** (Matplotlib): Write functions that call plot methods
- **Stat/Geom classes** (ggplot2): Inherit from base classes
- **Custom transforms** (Vega-Lite): JavaScript expressions
- **Substitution functions** (Tableplot): Any function can be a substitution value

### System Comparison Matrix

| System | Param Model | Composition | Stats | Types | Target Rendering |
|--------|-------------|-------------|-------|-------|------------------|
| **Matplotlib** | Imperative | Sequential calls | Manual | Duck | Canvas (Agg/SVG) |
| **ggplot2** | Object-oriented | Algebraic `+` | Stat layers | R types | grid graphics |
| **Plotnine** | OO (ggplot2 port) | Algebraic `+` | Stat layers | Pandas types | Matplotlib |
| **Vega-Lite** | Declarative JSON | Layer array | Transform array | Field types | Vega (Canvas/SVG) |
| **Altair** | Declarative (Python) | Method chains | Vega transforms | Inferred | Vega-Lite → Vega |
| **AlgebraOfGraphics.jl** | Algebraic expressions | `*` and `+` | Stat layers | Julia types | Makie.jl |
| **Hanami** | Template substitution | Template composition | Manual | Pass-through | Vega-Lite |
| **Tableplot** | Template + deps | Pipeline + inheritance | Dependency functions | Inferred | Plotly.js |

### Julia's Algebra of Graphics

**Core concepts**:

```julia
using AlgebraOfGraphics

# Specification algebra
spec = data(df) * mapping(:x, :y) * visual(Scatter)

# Composition operators:
# * (product) - combine orthogonal aspects
# + (sum) - overlay multiple layers

# Example:
spec = data(df) * (
    mapping(:x, :y, color=:group) * visual(Scatter) +
    mapping(:x, :y) * linear() * visual(Lines)  # linear() is a stat
)

# Draw
draw(spec)
```

**Key features**:

1. **Algebraic composition**: `*` for combining attributes, `+` for layering
2. **Lazy evaluation**: Specs are symbolic until `draw()`
3. **Stat transformations**: `linear()`, `histogram()`, `density()` as algebra elements
4. **Type-driven**: Leverages Julia's type system for method dispatch
5. **Makie backend**: Generates Makie.jl plotting commands

**Conversion considerations**:

| AoG Concept | Tableplot Equivalent | Notes |
|-------------|----------------------|-------|
| `data(df)` | `(base dataset)` | Similar - specify dataset |
| `mapping(:x, :y)` | `{:=x :x, :=y :y}` | AoG more implicit, Tableplot explicit |
| `visual(Scatter)` | `(layer-point {})` | Similar - specify mark type |
| `* operator` | `merge` on substitution maps | AoG: algebraic, Tableplot: map merge |
| `+ operator` | `(layer ...)` calls | Both create layers |
| `linear()` | `smooth-stat` | Both are statistical transformations |
| Lazy evaluation | All evaluation is lazy | Both defer until render |
| Type dispatch | Explicit type inference | AoG uses Julia types, Tableplot inspects data |

---

## Conversion Patterns

### Pattern 1: Aesthetic Mappings

**From Algebra of Graphics**:
```julia
mapping(:x => "Year", :y => "Temperature", color = :Station)
```

**To Tableplot**:
```clojure
{:=x :year
 :=y :temperature
 :=color :station}
```

**Conversion logic**:
- Column names: symbol → keyword
- Aesthetic names: predefined set (x, y, color, size, etc.)
- No need for `=>` renaming in Clojure (column names are already keywords)

### Pattern 2: Algebraic Composition with `*`

**From AoG**:
```julia
data(df) * mapping(:x, :y) * visual(Scatter)
```

**To Tableplot**:
```clojure
(-> df
    (base {:=x :x, :=y :y})
    (layer-point {}))
```

**Conversion logic**:
- `data(df) * mapping(...)` → `(base df {...})`
- `* visual(Scatter)` → `(layer-point {})`
- Product operator represents "combine these attributes" → merge substitution maps

### Pattern 3: Layering with `+`

**From AoG**:
```julia
mapping(:x, :y) * visual(Scatter) +
mapping(:x, :y) * smooth() * visual(Lines)
```

**To Tableplot**:
```clojure
(-> df
    (base {:=x :x, :=y :y})  ;; Shared mappings
    (layer-point {})
    (layer-smooth {}))
```

**Conversion logic**:
- Each `+` term → separate `layer` call
- Shared mappings factor to `base`
- Sum operator represents "overlay" → multiple layer calls

### Pattern 4: Statistical Transformations

**From AoG**:
```julia
mapping(:x) * histogram() * visual(BarPlot)
```

**To Tableplot**:
```clojure
(-> df
    (layer-histogram {:=x :x
                      :=histogram-nbins 30}))
```

**Conversion logic**:
- `histogram()` → `layer-histogram` (stat layer combines stat + visual)
- Parameters to stat → substitution keys
- Result: transformed data is used automatically

### Pattern 5: Faceting

**From AoG**:
```julia
data(df) * mapping(:x, :y) * visual(Scatter) |> facet(cols = :region)
```

**To Tableplot** (hypothetical - not yet implemented):
```clojure
(-> df
    (base {:=x :x, :=y :y})
    (layer-point {})
    (facet-wrap {:=facet-col :region}))
```

**Implementation needs**:
- `facet-wrap` and `facet-grid` functions
- Shared scale computation across facets
- Facet-level environment nesting

### Pattern 6: Grouped Operations

**From AoG**:
```julia
data(df) * mapping(:x, :y, color = :group) * smooth()
```

**To Tableplot**:
```clojure
(-> df
    (base {:=x :x
           :=y :y
           :=color :group})
    (layer-smooth {}))

;; The :=color → :group causes:
;; 1. :=inferred-group → [:group] (from submap->group)
;; 2. smooth-stat receives :=group → [:group]
;; 3. smooth-stat fits separate models per group
;; 4. Results are colored by group automatically
```

**Conversion logic**:
- Grouping in AoG often implicit from color/facet
- Tableplot makes it explicit via `:=inferred-group`
- Both systems compute stats per group

---

## Alternative Design Considerations

### If Designing a New System

Learning from Tableplot, what would we do differently?

#### 1. **Separate Substitution from Dependency Resolution**

**Current**: `xform` handles both substitution and `dag/fn-with-deps` interleaves with it

**Alternative**:
```clojure
;; Step 1: Build dependency graph
(def dep-graph (analyze-dependencies template))

;; Step 2: Topologically sort
(def sorted-keys (topological-sort dep-graph))

;; Step 3: Single-pass resolution
(def resolved-env
  (reduce (fn [env key]
            (assoc env key (compute-value key env)))
          base-env
          sorted-keys))

;; Step 4: Simple substitution (no fixpoint needed)
(def result (substitute template resolved-env))
```

**Tradeoffs**:
- ✅ Faster (single pass vs fixpoint iteration)
- ✅ Easier to debug (clear execution order)
- ✅ Detects cycles before computation
- ❌ Less flexible (no arbitrary nesting)
- ❌ Requires upfront dependency declaration

#### 2. **Typed Environments with Schemas**

**Current**: No validation, typos cause runtime errors

**Alternative**:
```clojure
(def layer-schema
  {:=x {:required true, :type :keyword}
   :=y {:required true, :type :keyword}
   :=color {:required false, :type :keyword}
   :=dataset {:required true, :type :dataset}})

(defn validate-layer [submap]
  (doseq [[k {:keys [required type]}] layer-schema]
    (when required
      (assert (contains? submap k) (str "Missing required key: " k)))
    (when-let [v (get submap k)]
      (assert (check-type v type) (str "Wrong type for " k)))))

;; Usage:
(-> dataset
    (base {:=x :temperatur})  ;; Typo!
    (layer-point {}))

;; Error: "Did you mean :=x → :temperature? (found :temperatur)"
```

**Tradeoffs**:
- ✅ Catch errors early
- ✅ Better IDE support (autocomplete)
- ✅ Self-documenting schemas
- ❌ Less flexible (must update schema for extensions)
- ❌ More boilerplate

#### 3. **Explicit Parameter Passing**

**Current**: Implicit lookup via environment

**Alternative**:
```clojure
;; Explicit parameter objects
(def my-layer
  (layer-point dataset
               {:x-column :year
                :y-column :temperature
                :color-column :station
                :x-scale (continuous-scale)
                :y-scale (continuous-scale)
                :color-scale (categorical-scale ["red" "blue"])}))

;; No environment, no substitution - just function calls
```

**Tradeoffs**:
- ✅ No magic, completely explicit
- ✅ Type checking possible
- ✅ IDE navigation works
- ❌ Extremely verbose
- ❌ Hard to share parameters across layers
- ❌ No template reuse

#### 4. **Bidirectional Substitution**

**Current**: One-way (template + environment → result)

**Alternative**: Allow extracting environment from result
```clojure
;; Forward: template + env → result
(def result (xform template env))

;; Backward: result → env
(def extracted-env (extract-parameters result))

;; Useful for:
;; - Understanding what parameters were used
;; - Saving/loading visualization specs
;; - Round-tripping through serialization
```

**Implementation**:
- Track substitutions in metadata
- Store "parameter provenance" for each value
- Provide `xform-inverse` function

### Alternatives to Key Design Patterns

#### Instead of `RMV` Sentinel

**Option A: Explicit Optional Schema**
```clojure
(def layer-schema
  {:x {:required true}
   :y {:required true}
   :color {:required false}  ;; Optional - omit if not provided
   :size {:required false}})

;; No need for RMV, just don't include optional keys
```

**Option B: Nil Means Absent**
```clojure
;; Treat nil differently from missing
(xform/xform
 {:color :Color}
 {:Color nil})  ;; nil → remove :color key
;; => {}
```

**Option C: Explicit Filter**
```clojure
;; Separate cleanup phase
(def result
  (-> template
      (xform env)
      (remove-keys [:omit])))  ;; Remove all :omit values
```

#### Instead of Nested `::ht/defaults`

**Option A: Explicit Context Stack**
```clojure
(def context-stack
  [(base-env)
   (layer-env)
   (facet-env)])

(defn lookup [key]
  (some #(get % key) context-stack))  ;; Search from top to bottom
```

**Option B: Environment Transformers**
```clojure
(-> template
    (with-env base-env)
    (modify-env #(assoc % :x :year))  ;; Explicit modification
    (render))
```

**Option C: Explicit Merge**
```clojure
;; No nested defaults - user merges explicitly
(layer dataset
       layer-base
       (merge global-params local-params))
```

---

## Open Questions and Limitations

### Current Limitations

#### 1. **No Cycle Detection**

```clojure
(xform/xform
 {:a :B, :b :A}
 {:A :B, :B :A})
;; StackOverflowError!

;; Should ideally detect and report:
;; "Circular dependency: :A → :B → :A"
```

**Possible solution**: Track substitution path, detect revisits

#### 2. **Cryptic Error Messages**

```clojure
(-> dataset
    (base {:=x :nonexistent-column})
    (layer-point {}))

;; Error somewhere deep in Plotly trace generation:
;; "NullPointerException" or "Key :nonexistent-column not found"

;; Should say:
;; "Column :nonexistent-column not found in dataset. Available: [:x, :y, :z]"
```

**Possible solution**: Validation layer before xform

#### 3. **Difficult to Debug Substitution Chains**

```clojure
;; Where did this value come from?
(def result (xform template env))
(get-in result [:layout :xaxis :title])
;; => "Year"

;; How do I know which substitution key provided this?
;; Was it:=x-title? A default? A function?
```

**Possible solution**: Metadata tracking substitution provenance

#### 4. **No Faceting Yet**

Tableplot doesn't yet implement faceting (small multiples).

**Needed for faceting**:
- Shared scale computation across facets
- Facet labeling
- Layout management (grid arrangement)
- Per-facet environment with shared base

**Implementation sketch**:
```clojure
(dag/defn-with-deps faceted-layout
  [=dataset =facet-row =facet-col =layers =shared-scales]
  (let [facets (partition-facets =dataset =facet-row =facet-col)]
    {:type :grid
     :subplots (for [facet facets]
                 {:data (:data facet)
                  :layers =layers
                  :scales =shared-scales
                  :title (facet-title facet)})}))
```

#### 5. **Cache Scope Management**

```clojure
;; Current: Cache is scoped to with-clean-cache block
(cache/with-clean-cache
  (render viz1)
  (render viz2))  ;; viz2 might benefit from viz1's cached computations

;; But if viz1 and viz2 are independent, stale cache could cause bugs
```

**Questions**:
- Should cache be automatically cleared between top-level renders?
- Should users control cache scope more explicitly?
- Should there be cache invalidation based on data identity?

#### 6. **Limited Introspection**

```clojure
;; Can check deps:
(:scicloj.tableplot.v1.dag/dep-ks (meta smooth-stat))
;; => [:=dataset :=x :=y :=group]

;; But can't easily ask:
;; - "What would this template need to render?"
;; - "Why did this key fail to resolve?"
;; - "What's the full dependency graph?"
```

**Possible solution**: Build visualization tools for dependency graphs

### Open Design Questions

#### 1. **How to Handle Scale Coordination?**

Currently, each layer computes its own scales. For consistency:
- Should scales be computed once at the base level?
- Should layers share scale objects?
- How to handle layers with different data ranges?

**Example issue**:
```clojure
(-> dataset1  ;; Range: [0, 100]
    (base)
    (layer-point {:=dataset dataset2  ;; Range: [0, 1000]
                  :=x :x}))

;; Should x-axis show [0, 100] or [0, 1000]?
;; Currently: Plotly.js handles it
;; Ideally: We compute union of domains
```

#### 2. **How to Handle Heterogeneous Layers?**

```clojure
(-> dataset
    (base)
    (layer-point {:=x :x, :=y :y})
    (layer-line {:=x :x, :=y :smooth-y})  ;; Different y column
    (layer-area {:=x :x, :=y0 :lower, :=y1 :upper}))  ;; Different aesthetic mapping

;; These have incompatible parameter sets
;; Current approach: Use RMV for unused parameters
;; Is there a better way?
```

#### 3. **Should Statistics Be Separate from Marks?**

**Current**: `layer-smooth` combines stat and mark
```clojure
(layer-smooth dataset {...})
;; Combines: smooth-stat + layer-line
```

**Alternative**: Separate specification
```clojure
(-> dataset
    (add-stat smooth-stat {:=x :x, :=y :y})  ;; Creates :=smooth-y column
    (layer-line {:=y :=smooth-y}))  ;; Uses computed column
```

**Tradeoffs**:
- Current: Convenient, fewer steps
- Alternative: More flexible, can reuse stat for multiple marks

#### 4. **How to Handle Animation/Interaction?**

Grammar of graphics typically focuses on static visualizations. For interactive/animated:

**Option A**: Extend substitution keys
```clojure
{:=animation-frame :year
 :=hover-tooltip :temperature}
```

**Option B**: Separate interaction spec
```clojure
(-> plot
    (add-animation {:frame-var :year, :transition 500})
    (add-interaction {:on :hover, :show :tooltip}))
```

**Option C**: Generate multiple static frames
```clojure
(for [year (range 2000 2020)]
  (-> dataset
      (tc/select-rows #(= (:year %) year))
      (base)
      (layer-point {})))
```

---

## Future Directions

### For Grammar-of-Graphics Research

#### 1. **Formal Specification Language**

Develop a notation for precisely specifying GG semantics:

```
Visualization := Data × Mappings × Layers × Scales × Coords

Mappings := {aesthetic: column}

Layer := Mark × Stat × Position

Stat := Dataset → Dataset  (with grouping awareness)

Scale := Domain × Range × Transform
```

Then prove properties:
- **Composability**: `viz1 + viz2` is valid visualization
- **Substitutability**: Changing data doesn't break spec
- **Determinism**: Same spec + data → same viz

#### 2. **Dataflow System Comparison Study**

Systematically compare dataflow approaches:

| System | Model | Pros | Cons |
|--------|-------|------|------|
| Hanami | Pure substitution | Simple, elegant | No introspection |
| Tableplot | Substitution + deps | Introspectable, cacheable | More verbose |
| Vega | Declarative transforms | Standardized | Less flexible |
| Polars | SQL-like | Familiar | Not GG-specific |
| TidyPipe | Function pipeline | Composable | Imperative |

Metrics:
- Expressiveness (can you implement X?)
- Performance (how expensive is Y?)
- Understandability (can users debug Z?)

#### 3. **Conversion Tool: AlgebraOfGraphics.jl → Tableplot**

Build a translator:

```clojure
(defn aog->tableplot
  "Convert Algebra of Graphics spec to Tableplot"
  [aog-expr]
  (match aog-expr
    [:data df] `(base ~df)
    [:mapping bindings] (parse-mappings bindings)
    [:visual mark] (visual->layer mark)
    [:* & terms] (merge-terms terms)
    [:+ & terms] (layer-terms terms)))

;; Usage:
(aog->tableplot
  '(* (data df)
      (mapping :x :y)
      (visual Scatter)))
;; => (-> df (base {:=x :x, :=y :y}) (layer-point {}))
```

This would:
- Validate the approach
- Provide migration path for Julia users
- Reveal design incompatibilities

#### 4. **Dependency Graph Visualizer**

Tool to visualize template dependencies:

```clojure
(viz-dependencies
  (-> dataset
      (base {:=x :year})
      (layer-smooth {})))

;; Shows:
;; =dataset ───┐
;; =x ─────────┤
;; =y ─────────┼──→ smooth-stat ──→ =stat
;; =group ─────┘                       │
;;                                     ↓
;;                              layer :dataset
```

This helps:
- Understanding complex templates
- Debugging missing dependencies
- Optimizing computation order

#### 5. **Grammar of Graphics Benchmark Suite**

Create standard test cases:

```clojure
(def gg-benchmark-suite
  [;; Basic cases
   {:name "scatter-plot"
    :data iris-dataset
    :spec {:x :sepal-length, :y :sepal-width, :color :species}}
   
   ;; Statistical transformations
   {:name "histogram"
    :data diamonds-dataset
    :spec {:x :price, :stat :bin, :bins 30}}
   
   ;; Layering
   {:name "scatter-with-smooth"
    :data cars-dataset
    :spec {:layers [{:mark :point, :x :speed, :y :dist}
                    {:mark :smooth, :x :speed, :y :dist}]}}
   
   ;; Faceting
   {:name "faceted-scatter"
    :data mpg-dataset
    :spec {:x :displ, :y :hwy, :facet-wrap :class}}
   
   ;; ... 50 more cases covering GG space
   ])

;; Run against multiple implementations
(for [impl [tableplot vega-lite plotnine algebra-of-graphics]]
  (benchmark-suite impl gg-benchmark-suite))
```

Measure:
- Time to implement
- Lines of code
- Rendering performance
- Memory usage
- Feature coverage

### For Tableplot Specifically

#### 1. **Implement Faceting**

Full small multiples support with:
- `facet-wrap` (1D wrapping)
- `facet-grid` (2D grid)
- Shared scale computation
- Free scales option
- Facet labeling

#### 2. **Add Validation Layer**

Schema-based validation before rendering:
- Check required keys present
- Validate column names exist
- Type check aesthetic mappings
- Suggest corrections for typos

#### 3. **Improve Error Messages**

Wrap xform in error handling:
- Track substitution path
- Detect cycles early
- Provide context in errors
- Suggest fixes

#### 4. **Build Developer Tools**

- Dependency graph visualizer
- Substitution debugger (step through xform)
- Template inspector
- Performance profiler

#### 5. **Optimize Performance**

- Memoize type inference
- Share scale computations
- Parallelize independent stats
- Lazy evaluation of unused layers

---

## Conclusion

Tableplot's dataflow system demonstrates that grammar-of-graphics challenges can be elegantly addressed through **term rewriting with explicit dependencies**. The six core substitution rules provide a minimal, mathematically grounded foundation. The addition of declared dependencies makes the system practical for real-world visualization.

For future work on converting systems like Algebra of Graphics:

**Key learnings**:
1. Algebraic composition (`*`, `+`) maps naturally to function composition and environment merging
2. Statistical transformations benefit enormously from explicit dependency tracking and caching
3. Parameter threading is not a feature to implement - it's what happens naturally when you apply rewriting rules
4. The boundary between "configuration" and "computation" should be explicit

**Conversion strategy**:
1. Map high-level operators (`*`, `+`, `|>`) to function composition patterns
2. Translate aesthetic mappings to substitution keys
3. Convert stat specifications to `dag/defn-with-deps` functions
4. Let the dataflow system handle the rest

**Open questions**:
1. How to handle truly dynamic specifications (e.g., user-driven exploration)?
2. What's the right level of type checking vs flexibility?
3. Should we separate concerns more (substitution / dependencies / caching)?
4. How to make debugging easier while preserving elegance?

This analysis should provide a solid foundation for future explorations of grammar-of-graphics dataflow models in Clojure.
