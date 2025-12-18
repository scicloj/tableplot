;; # Unified Dataflow Model - Complete Walkthrough
;;
;; This notebook explains the V2 dataflow design from first principles,
;; showing both the user-facing API and the internal implementation.

;; ## Motivation
;;
;; ### The Problem
;;
;; Plotting libraries face a tension:
;; - **Ease of use**: Users want `(scatter data :x :y)` to "just work"
;; - **Customization**: Power users need fine-grained control
;; - **Clarity**: Understanding what the library is doing shouldn't be magic
;;
;; ### Previous Approaches
;;
;; **Tableplot (Hanami-based)**:
;; - Template substitution with implicit keys
;; - Magic but sometimes opaque
;; - Hard to customize deeply
;;
;; **AoG (Function composition)**:
;; - Explicit function calls for everything
;; - Clear but verbose
;; - Great for customization
;;
;; ### Our Solution
;;
;; A **unified dataflow model** that:
;; 1. Everything is data transformation on a simple spec map
;; 2. Functional API compiles to these transformations
;; 3. Smart defaults via explicit inference rules
;; 4. Single generic mechanism for all inference

;; ## Core Concepts

(ns v2-dataflow-walkthrough
  (:require [tableplot.v2.dataflow :as df]
            [tableplot.v2.inference :as infer]
            [tableplot.v2.api :as api]
            [tech.v3.dataset :as ds]
            [scicloj.kindly.v4.kind :as kind]))

;; ### Concept 1: Subkeys (Substitution Keys)
;;
;; We use **subkeys** as placeholders for values to be inferred.
;; By convention (not requirement), we prefix them with `=`:

;; Regular keyword (literal value):
:x           ; The column named :x

;; Subkey (placeholder to be filled):
:=x          ; A value to be inferred/substituted

;; The `:=` prefix is just a convention - templates declare their subkeys via metadata.
;; This makes the dataflow **explicit** and **flexible**:

(df/subkey-by-convention? :x)

^kind/test-last
{:test false}

(df/subkey-by-convention? :=x)

^kind/test-last
{:test true}

;; But the real check consults the template's declared subkeys:
(df/subkey? df/base-plot-template :=data)

^kind/test-last
{:test true}

;; ### Concept 2: The Spec Structure
;;
;; A plot spec is just a map with two parts:

;; **Part 1: The Template** (structure with placeholders)
{:data :=data                    ; Data to be filled in
 :layers :=layers                ; Layers to be filled in
 :scales {:x :=x-scale           ; Scale definitions
          :y :=y-scale}
 :guides {:x :=x-guide           ; Axes, legends
          :y :=y-guide}}

;; **Part 2: The Substitutions** (concrete values)
{:=substitutions {:=data nil        ; placeholder for dataset
                  :=layers []}}     ; placeholder for layers

;; They live together in one map:
{:data :=data
 :layers :=layers
 :scales {:x :=x-scale :y :=y-scale}
 :=substitutions {:=data nil
                  :=layers []}}

;; ### Concept 3: Inference Rules
;;
;; Instead of hardcoded logic, we have a **registry of inference rules**.
;; Each rule knows how to compute one subkey from the spec.

;; The registry is a map of functions:
(keys df/*inference-rules*)

;; Each rule is a function: (spec, context) -> value
;;
;; Rules can declare dependencies via metadata

;; ### Concept 4: Single Generic Inference
;;
;; One function resolves ALL subkeys through a simple algorithm:
;; 1. Find all subkeys in the template
;; 2. Check which ones don't have substitutions yet
;; 3. Sort them by dependency order
;; 4. Apply inference rules to compute values
;; 5. Replace all subkeys with their values

;; ## Implementation Deep Dive

;; Let's look at the actual implementation, piece by piece.

;; ### Finding Subkeys
;;
;; We need to walk the spec and find all subkeys.
;; But we must be careful NOT to descend into datasets!

(df/find-subkeys {:data :=data
                  :layers :=layers
                  :scales {:x :=x-scale :y :=y-scale}})

^kind/test-last
{:test #(= #{:=data :=layers :=x-scale :=y-scale} %)}

;; ### Managing Substitutions
;;
;; Simple functions to get/set substitutions:

;; Create a spec with substitutions:
(def spec-1
  (df/make-spec {:data :=data :title :=title}))

spec-1

;; Add a substitution:
(def spec-2
  (df/add-substitution spec-1 :=data "my-dataset"))

(df/get-substitution spec-2 :=data)

^kind/test-last
{:test "my-dataset"}

;; Add multiple substitutions:
(def spec-3
  (df/add-substitutions spec-2 {:=title "My Plot"}))

(df/get-substitution spec-3 :=title)

^kind/test-last
{:test "My Plot"}

;; ### Applying Substitutions
;;
;; Walk the spec and replace subkeys with values:

(df/apply-substitutions {:data :=data
                         :title :=title
                         :=substitutions {:=data "my-dataset"
                                          :=title "My Plot"}})

^kind/test-last
{:test #(and (= "my-dataset" (:data %))
             (= "My Plot" (:title %))
             (nil? (:=substitutions %)))}

;; ### Dependency Tracking
;;
;; Some rules depend on others. For example, guides depend on scales.
;; We use metadata to declare dependencies.
;;
;; Check dependencies of a rule:

(::df/depends-on (meta (get df/*inference-rules* :=x-guide)))

^kind/test-last
{:test #{:=x-scale}}

;; This tells the inference engine to compute :=x-scale before :=x-guide

;; ### The Inference Engine
;;
;; The main `infer` function orchestrates everything.
;; Let's see it in action with a simple example:

(df/infer {:value :=value
           :=substitutions {:=value 42}})

^kind/test-last
{:test #(= 42 (:value %))}

;; ## The Inference Rules

;; Let's look at some actual inference rules by seeing them work:

;; ### Rule 1: Infer Field from Layer
;;
;; Extract which fields are mapped to aesthetics by looking at the layer

(infer/infer-x-field
 {:=substitutions {:=layers [{:mark :point :x :sepal-width :y :sepal-length}]}}
 {})

^kind/test-last
{:test :sepal-width}

(infer/infer-y-field
 {:=substitutions {:=layers [{:mark :point :x :sepal-width :y :sepal-length}]}}
 {})

^kind/test-last
{:test :sepal-length}

;; ### Rule 2: Infer Scale from Data + Field
;;
;; Create a small test dataset:

(def test-data
  (ds/->dataset {:x [1 2 3 4 5]
                 :y [10 20 30 40 50]}))

;; Infer a scale for the x aesthetic:
(infer/infer-x-scale {:=substitutions {:=data test-data
                                       :=x-field :x}}
                     {})

^kind/test-last
{:test #(and (= :linear (:type %))
             (= :x (:field %))
             (= [1 5] (:domain %)))}

;; ### Rule 3: Infer Guide from Scale

(infer/infer-x-guide {:=substitutions {:=x-scale {:type :linear
                                                   :field :sepal-width
                                                   :domain [2.0 4.5]}}}
                     {})

^kind/test-last
{:test #(and (= :axis (:type %))
             (= :bottom (:orientation %))
             (= "sepal-width" (:title %)))}

;; ### Rule 4: Infer Title

(infer/infer-title {:=substitutions {:=x-field :sepal-width
                                     :=y-field :sepal-length}}
                   {})

^kind/test-last
{:test "sepal-length vs sepal-width"}

;; ## API Design

;; The functional API is just syntactic sugar that builds specs.

;; ### Layer Builders
;;
;; Pure functions that return layer maps:

(api/point :x :sepal-width :y :sepal-length :color :species)

^kind/test-last
{:test {:mark :point
        :x :sepal-width
        :y :sepal-length
        :color :species
        :size nil}}

(api/line :x :time :y :value)

^kind/test-last
{:test #(and (= :line (:mark %))
             (= :time (:x %))
             (= :value (:y %)))}

;; ### Spec Builders
;;
;; Functions that add substitutions to specs:

(def base-spec (api/base-plot))

(keys base-spec)

;; Add data:
(def with-data-spec (api/add-data base-spec test-data))

(df/get-substitution with-data-spec :=data)

^kind/test-last
{:test test-data}

;; Add a layer:
(def with-layer-spec
  (api/add-layer with-data-spec (api/point :x :x :y :y)))

(count (df/get-substitution with-layer-spec :=layers))

^kind/test-last
{:test 1}

;; Add a title:
(def with-title-spec
  (api/add-title with-layer-spec "My Scatter Plot"))

(df/get-substitution with-title-spec :=title)

^kind/test-last
{:test "My Scatter Plot"}

;; ### Convenience Functions
;;
;; Common patterns wrapped up nicely:

;; Quick scatter plot:
(def quick-scatter
  (api/scatter test-data :x :y))

(keys quick-scatter)

^kind/test-last
{:test #(some #{:=substitutions} %)}

;; Check the layers:
(df/get-substitution quick-scatter :=layers)

^kind/test-last
{:test #(and (vector? %)
             (= 1 (count %))
             (= :point (:mark (first %))))}

;; ### Incremental Building
;;
;; Thread-friendly API for building specs step by step:

(def threaded-spec
  (-> (api/base-plot)
      (api/add-data test-data)
      (api/add-layer (api/point :x :x :y :y))
      (api/add-title "Threaded Example")))

(df/get-substitution threaded-spec :=title)

^kind/test-last
{:test "Threaded Example"}

;; ## Complete Example

;; Let's walk through a complete example step by step.

;; ### Step 1: Load Data

(def iris
  (ds/->dataset {:sepal-width [3.5 3.0 3.2 3.1 3.6]
                 :sepal-length [5.1 4.9 4.7 4.6 5.0]
                 :species [:setosa :setosa :setosa :setosa :setosa]}))

iris

;; ### Step 2: Create Spec via API

(def my-spec
  (api/scatter iris :sepal-width :sepal-length :color :species))

;; Let's inspect the spec structure:
(keys my-spec)

;; The template part shows all the :=placeholder keys
(keys (dissoc my-spec :=substitutions))

;; The substitutions show what we've filled in so far:
(keys (:=substitutions my-spec))

^kind/test-last
{:test #(some #{:=data :=layers} %)}

;; ### Step 3: Inspect Before Inference

;; What subkeys still need values?
(def all-keys (df/find-subkeys my-spec))

(count all-keys)

;; Which ones do we already have?
(def existing-keys (set (keys (:=substitutions my-spec))))

existing-keys

^kind/test-last
{:test #(contains? % :=data)}

;; Which ones are missing?
(def missing-keys (remove existing-keys all-keys))

(count missing-keys)

;; These will be inferred!

;; ### Step 4: Run Inference

(def finalized-plot (api/finalize my-spec))

;; Now let's look at what was inferred:

;; The data is unchanged:
(ds/row-count (:data finalized-plot))

^kind/test-last
{:test 5}

;; The layers are unchanged:
(count (:layers finalized-plot))

^kind/test-last
{:test 1}

;; The scales were INFERRED:
(:scales finalized-plot)

;; Check the x scale specifically:
(get-in finalized-plot [:scales :x :type])

^kind/test-last
{:test :linear}

(get-in finalized-plot [:scales :x :field])

^kind/test-last
{:test :sepal-width}

;; The guides were INFERRED:
(get-in finalized-plot [:guides :x :type])

^kind/test-last
{:test :axis}

;; No more :=substitutions key in the final plot:
(:=substitutions finalized-plot)

^kind/test-last
{:test nil}

;; ### Step 5: Understanding the Flow

;; Let's trace through what happens step by step:

;; Start with the spec:
(def trace-spec (api/scatter iris :sepal-width :sepal-length))

;; Find what needs inference:
(def trace-missing
  (let [all (df/find-subkeys trace-spec)
        existing (set (keys (:=substitutions trace-spec)))]
    (remove existing all)))

(count trace-missing)

;; Apply inference rules (this is what `infer` does internally):
(def trace-inferred (df/infer-missing-keys trace-spec))

;; Check that substitutions were added:
(> (count (keys (:=substitutions trace-inferred)))
   (count (keys (:=substitutions trace-spec))))

^kind/test-last
{:test true}

;; Finally, apply substitutions to get the final spec:
(def trace-final (df/apply-substitutions trace-inferred))

;; No more subkeys in the final result:
(empty? (df/find-subkeys trace-final))

^kind/test-last
{:test true}

;; ## Extending the System

;; The beauty of this design is how easy it is to extend.

;; ### Example: Custom Inference Rule
;;
;; Let's add a rule to infer a nice axis label from the field name:

(defn infer-nice-x-label [spec _context]
  "Generate a nice axis label from field name"
  (when-let [x-field (df/get-substitution spec :=x-field)]
    (-> x-field
        name
        (clojure.string/replace "-" " ")
        clojure.string/capitalize)))

;; Test it:
(infer-nice-x-label {:=substitutions {:=x-field :sepal-width}} {})

^kind/test-last
{:test "Sepal width"}

;; To use it in the system, you would:
;; 1. Register it: (df/register-inference-rule! :=x-label infer-nice-x-label)
;; 2. Declare dependencies if needed
;; 3. Add :=x-label to the template's metadata and structure

;; ### Example: Custom API Function
;;
;; Let's create a histogram convenience function:

(defn my-histogram [data x & {:keys [bins]}]
  "Create a histogram"
  (-> (api/base-plot)
      (api/add-data data)
      (api/add-layer {:mark :bar
                      :x x
                      :y :count
                      :bins (or bins 10)})))

;; Test it:
(def hist-spec (my-histogram iris :sepal-width :bins 5))

(df/get-substitution hist-spec :=data)

^kind/test-last
{:test iris}

(-> (df/get-substitution hist-spec :=layers)
    first
    :bins)

^kind/test-last
{:test 5}

;; ## Key Insights

;; ### 1. Everything is Data
;;
;; The entire system operates on data structures - specs are just maps!

(map? my-spec)

^kind/test-last
{:test true}

(map? finalized-plot)

^kind/test-last
{:test true}

;; ### 2. Inference is Generic
;;
;; ONE generic mechanism handles all inference via the rules registry:

(map? df/*inference-rules*)

^kind/test-last
{:test true}

(> (count df/*inference-rules*) 10)

^kind/test-last
{:test true}

;; ### 3. Rules are Composable
;;
;; Each rule is independent and can be tested in isolation:

(fn? (get df/*inference-rules* :=x-scale))

^kind/test-last
{:test true}

;; ### 4. Debuggability
;;
;; You can inspect at every stage:

;; Before inference:
(keys (:=substitutions my-spec))

;; After inference:
(keys finalized-plot)

;; The difference:
(def inferred-keys
  (clojure.set/difference
    (set (keys finalized-plot))
    (set (keys my-spec))))

(seq inferred-keys)

;; ## Comparison Summary

;; **vs Hanami (Tableplot's current approach)**
;; - Explicit `:=keys` vs implicit substitution
;; - Same power, more clarity

;; **vs Pure Function Composition (AoG)**
;; - Functions compile to data
;; - Inference provides smart defaults
;; - Best of both worlds

;; **vs Vega-Lite (Declarative)**
;; - Specs are data but can be built functionally
;; - More flexible and composable

;; ## Conclusion

;; This unified dataflow model provides:
;;
;; 1. **Simplicity**: Everything is data transformation
;; 2. **Power**: Generic inference mechanism
;; 3. **Clarity**: Subkeys show what's happening, metadata declares them
;; 4. **Flexibility**: Multiple APIs can share the core, any convention works
;; 5. **Extensibility**: Add rules and functions easily
;; 6. **Debuggability**: Inspect at every stage
;; 7. **Idiomatic**: Convention-agnostic, metadata-driven

;; It combines the best aspects of template-based and compositional
;; approaches while maintaining clarity, simplicity, and Clojure idioms.

;; ## Next Steps

;; To make this production-ready:
;; 1. Add compilation to actual viz backends (Vega-Lite, Plotly)
;; 2. Implement proper topological sort for complex dependencies
;; 3. Add more sophisticated inference rules
;; 4. Performance optimization (caching, incremental inference)
;; 5. More API convenience functions
;; 6. Interactive capabilities (selections, brushing)
;; 7. Animation support
