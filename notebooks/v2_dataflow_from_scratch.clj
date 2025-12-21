;; # Understanding Tableplot V2 Dataflow: A Minimal Implementation
;;
;; **Goal**: Understand why and how the V2 dataflow model works by building
;; a minimal version from scratch and using it to implement a toy grammar of graphics.
;;
;; **What you'll learn**:
;; - Why direct construction is limiting
;; - How substitution keys and inference enable composability
;; - How to build a grammar of graphics on top of dataflow
;; - Why this architecture matters for extensibility

(ns v2-dataflow-from-scratch
  "A minimal implementation of the V2 dataflow model.
  
  This is a teaching tool - the real implementation in tableplot.v2.dataflow
  is more sophisticated, but the core ideas are the same."
  (:require [tech.v3.dataset :as ds]
            [clojure.pprint :refer [pprint]]))

;; =============================================================================
;; ## Part 1: The Problem - Direct Construction
;; =============================================================================

;; Let's start by seeing what happens when we build plots directly.

(defn plot-points-direct
  "Direct approach: immediately construct the visualization spec."
  [data x-field y-field]
  {:type :scatter
   :mode :markers
   :x (vec (get data x-field))
   :y (vec (get data y-field))})

;; Try it:
(def tiny-data {:x [1 2 3 4] :y [2 4 6 8]})

^:kind/pprint
(plot-points-direct tiny-data :x :y)

;; ### Problems with Direct Construction:
;;
;; 1. **Not Inspectable**: The spec is built all at once. Can't see intermediate stages.
;; 2. **Not Composable**: Hard to add layers, override defaults, or transform
;; 3. **Tight Coupling**: Data extraction, type inference, and rendering are mixed
;; 4. **No Override**: Can't customize what gets inferred without changing the function
;; 5. **No Backends**: Locked into one output format
;;
;; Let's see a concrete example of composition problems:

(comment
  ;; How do we add a color aesthetic?
  ;; Need a new function!
  (defn plot-points-with-color [data x-field y-field color-field]
    ...)

  ;; How do we add a log scale?
  ;; Need yet another function!
  (defn plot-points-with-log-scale [data x-field y-field log-x?]
    ...)

  ;; Combinatorial explosion of options!
  ;; What if we want color AND log scale AND custom palette AND ...?
  )

;; =============================================================================
;; ## Part 2: The Dataflow Solution
;; =============================================================================

;; **Key Insight**: Separate the "what" (plot structure) from the "how" (inference).
;;
;; Instead of computing values immediately, we use **substitution keys** (subkeys)
;; as placeholders. Later, an **inference engine** fills them in.
;;
;; ### Substitution Key Convention:
;;
;; Keywords starting with `=` are substitution keys (subkeys).
;; Examples: `:=x-field`, `:=x-scale`, `:=data`
;;
;; This is just a **naming convention** - subkeys are regular keywords!

;; ### The Three-Stage Process:
;;
;; 1. **Construction**: Build a spec with placeholders (`:=x-field`, `:=x-scale`)
;; 2. **Inference**: Resolve placeholders using registered rules
;; 3. **Rendering**: Convert the finalized spec to output format
;;
;; This separation enables:
;; - Inspection at any stage
;; - Override any inferred value
;; - Compose transformations
;; - Multiple backends

;; =============================================================================
;; ## Part 3: Minimal Dataflow Engine
;; =============================================================================

;; Let's implement the core concepts in ~100 lines.

;; ### 3.1 Spec Structure
;;
;; A spec is a map with:
;; - **:sub/keys** - Set of subkeys that can be inferred (for documentation)
;; - **:sub/map** - Map of subkeys to their values
;; - **Everything else** - The plot structure (can reference subkeys)

(defn make-spec
  "Create a spec from a template with optional initial substitutions."
  [template & {:as substitutions}]
  (cond-> template
    (seq substitutions)
    (assoc :sub/map substitutions)))

(defn get-substitution
  "Get the value for a subkey."
  [spec k]
  (get-in spec [:sub/map k]))

(defn add-substitution
  "Add a substitution to the spec."
  [spec k v]
  (assoc-in spec [:sub/map k] v))

;; ### 3.2 Inference Rules
;;
;; Rules are functions that compute values for subkeys.
;; They receive the spec and return a value.

(def ^:dynamic *inference-rules*
  "Global registry of inference rules."
  {})

(defn register-rule!
  "Register an inference rule for a subkey."
  [subkey rule-fn]
  (alter-var-root #'*inference-rules* assoc subkey rule-fn))

(defn infer-key
  "Infer the value for a single subkey using its registered rule."
  [spec subkey]
  (if-let [rule (get *inference-rules* subkey)]
    (rule spec)
    (throw (ex-info (str "No rule for " subkey) {:key subkey}))))

;; ### 3.3 Finding Subkeys
;;
;; Walk the spec structure and find all keywords starting with `=`.

(defn find-subkeys
  "Find all subkeys (keywords starting with =) in a spec.
  
  Excludes the special :sub/keys and :sub/map keys."
  [spec]
  (let [found (atom #{})]
    (letfn [(subkey? [x]
              (and (keyword? x)
                   (clojure.string/starts-with? (name x) "=")))
            (walk [x]
              (cond
                (and (subkey? x) (not (#{:sub/keys :sub/map} x)))
                (swap! found conj x)

                (map? x)
                (doseq [[k v] x]
                  (when-not (#{:sub/keys :sub/map} k)
                    (walk k)
                    (walk v)))

                (or (vector? x) (seq? x))
                (doseq [item x] (walk item))))]
      (walk spec))
    @found))

;; ### 3.4 Inference Engine

(defn infer-missing
  "Infer all subkeys that don't have values yet."
  [spec]
  (let [all-keys (find-subkeys spec)
        existing (set (keys (get spec :sub/map {})))
        missing (remove existing all-keys)]
    (reduce
     (fn [s k]
       (let [value (infer-key s k)]
         (add-substitution s k value)))
     spec
     missing)))

(defn apply-substitutions
  "Walk the spec and replace subkeys with their values."
  [spec]
  (let [subs (get spec :sub/map {})]
    (letfn [(subkey? [x]
              (and (keyword? x)
                   (clojure.string/starts-with? (name x) "=")))
            (substitute [x]
              (cond
                (subkey? x) (get subs x x)
                (map? x) (into {} (map (fn [[k v]] [(substitute k) (substitute v)]) x))
                (vector? x) (mapv substitute x)
                (seq? x) (map substitute x)
                :else x))]
      (-> (substitute spec)
          (dissoc :sub/map :sub/keys)))))

(defn infer
  "Main inference function: resolve all subkeys and apply substitutions."
  [spec]
  (-> spec
      infer-missing
      apply-substitutions))

;; ### 3.5 Example: See It In Action

(comment
  ;; Define a simple template
  (def simple-template
    {:sub/keys #{:=name :=greeting}
     :message :=greeting
     :name :=name})

  ;; Register rules
  (register-rule! :=name (fn [spec] "World"))
  (register-rule! :=greeting (fn [spec]
                               (str "Hello, " (get-substitution spec :=name) "!")))

  ;; Create spec
  (def spec (make-spec simple-template))

  ;; Before inference - see the template with placeholders
  (pprint spec)

  ;; After inference - placeholders are replaced with values
  (pprint (infer spec))

  ;; Override a value - change "World" to "Clojure"
  (pprint (infer (add-substitution spec :=name "Clojure"))))

;; =============================================================================
;; ## Part 4: Building a Toy Grammar of Graphics
;; =============================================================================

;; Now let's use our dataflow engine to build a simple ggplot-like API.

;; ### 4.1 Plot Template

(def plot-template
  "Base template for plots."
  {:sub/keys #{:=data :=x-field :=y-field :=color-field
               :=x-data :=y-data :=color-data
               :=x-scale :=y-scale
               :=geom}
   :data :=data
   :geom :=geom
   :x {:field :=x-field
       :data :=x-data
       :scale :=x-scale}
   :y {:field :=y-field
       :data :=y-data
       :scale :=y-scale}
   :color {:field :=color-field
           :data :=color-data}})

;; ### 4.2 API Functions

(defn plot
  "Initialize a plot with data."
  [data]
  (-> (make-spec plot-template)
      (add-substitution :=data data)))

(defn aes
  "Set aesthetic mappings."
  [spec & {:keys [x y color]}]
  (cond-> spec
    x (add-substitution :=x-field x)
    y (add-substitution :=y-field y)
    color (add-substitution :=color-field color)))

(defn geom
  "Set the geometry type."
  [spec geom-type]
  (add-substitution spec :=geom geom-type))

;; ### 4.3 Inference Rules for our Grammar

;; Rule: Default data (already set by plot function)
(register-rule! :=data (fn [spec] nil))

;; Rule: Default geom
(register-rule! :=geom (fn [spec] :point))

;; Rule: Default field (nil if not set)
(register-rule! :=x-field (fn [spec] nil))
(register-rule! :=y-field (fn [spec] nil))
(register-rule! :=color-field (fn [spec] nil))

;; Rule: Extract x data from dataset
(register-rule! :=x-data
                (fn [spec]
                  (let [data (get-substitution spec :=data)
                        field (get-substitution spec :=x-field)]
                    (when (and data field)
                      (vec (get data field))))))

;; Rule: Extract y data from dataset
(register-rule! :=y-data
                (fn [spec]
                  (let [data (get-substitution spec :=data)
                        field (get-substitution spec :=y-field)]
                    (when (and data field)
                      (vec (get data field))))))

;; Rule: Extract color data from dataset
(register-rule! :=color-data
                (fn [spec]
                  (let [data (get-substitution spec :=data)
                        field (get-substitution spec :=color-field)]
                    (when (and data field)
                      (vec (get data field))))))

;; Rule: Infer x scale from data
(register-rule! :=x-scale
                (fn [spec]
                  (let [x-data (get-substitution spec :=x-data)]
                    (when (and x-data (seq x-data))
                      (if (every? number? x-data)
                        {:type :linear
                         :domain [(apply min x-data) (apply max x-data)]}
                        {:type :categorical
                         :domain (vec (distinct x-data))})))))

;; Rule: Infer y scale from data
(register-rule! :=y-scale
                (fn [spec]
                  (let [y-data (get-substitution spec :=y-data)]
                    (when (and y-data (seq y-data))
                      (if (every? number? y-data)
                        {:type :linear
                         :domain [(apply min y-data) (apply max y-data)]}
                        {:type :categorical
                         :domain (vec (distinct y-data))})))))

;; ### 4.4 Example: Build a Plot Step by Step

(def my-data {:x [1 2 3 4 5]
              :y [2 4 6 8 10]
              :category [:a :a :b :b :c]})

;; Step 1: Create plot
(def step1 (plot my-data))

^:kind/pprint
(:sub/map step1)

;; Step 2: Add aesthetics
(def step2 (-> step1
               (aes :x :x :y :y :color :category)))

^:kind/pprint
(:sub/map step2)

;; Step 3: Set geometry
(def step3 (geom step2 :point))

^:kind/pprint
(:sub/map step3)

;; Step 4: Infer!
(def final (infer step3))

^:kind/pprint
final

;; Or do it all at once with threading:
^:kind/pprint
(-> (plot my-data)
    (aes :x :x :y :y :color :category)
    (geom :point)
    infer)

;; =============================================================================
;; ## Part 5: The Power of Dataflow
;; =============================================================================

;; ### 5.1 Inspection at Any Stage

;; See what's been set so far:
^:kind/pprint
(:sub/map (-> (plot my-data)
              (aes :x :x :y :y)))

;; See what will be inferred:
^:kind/pprint
(find-subkeys step3)

;; ### 5.2 Override Any Inference

;; Let's override the x-scale to be logarithmic:
^:kind/pprint
(-> (plot my-data)
    (aes :x :x :y :y)
    (geom :point)
    (add-substitution :=x-scale {:type :log :base 10})
    infer)

;; Notice: x-scale is now :log, not the inferred :linear!

;; ### 5.3 Multiple Backends

(defn render-plotly
  "Render to Plotly format."
  [spec]
  {:data [{:type (case (:geom spec)
                   :point "scatter"
                   :line "scatter")
           :mode (case (:geom spec)
                   :point "markers"
                   :line "lines")
           :x (get-in spec [:x :data])
           :y (get-in spec [:y :data])}]
   :layout {:xaxis {:title (str (get-in spec [:x :field]))}
            :yaxis {:title (str (get-in spec [:y :field]))}}})

(defn render-text
  "Render to simple text visualization."
  [spec]
  (let [x-data (get-in spec [:x :data])
        y-data (get-in spec [:y :data])
        x-field (get-in spec [:x :field])
        y-field (get-in spec [:y :field])]
    (str "Plot: " x-field " vs " y-field "\n"
         "Points: " (count x-data) "\n"
         "Data:\n"
         (clojure.string/join "\n"
                              (map (fn [x y] (format "  %s: %s, %s: %s" x-field x y-field y))
                                   x-data y-data)))))

;; Same spec, different renderers:
(def my-spec
  (-> (plot my-data)
      (aes :x :x :y :y)
      (geom :point)
      infer))

^:kind/pprint
(render-plotly my-spec)

^:kind/pprint
(render-text my-spec)

;; ### 5.4 Composition

;; Create a helper function that adds log scale:
(defn scale-x-log
  "Add logarithmic x-scale."
  [spec]
  (add-substitution spec :=x-scale {:type :log :base 10}))

;; Use it:
^:kind/pprint
(-> (plot {:x [1 10 100 1000] :y [2 4 8 16]})
    (aes :x :x :y :y)
    (geom :point)
    scale-x-log ;; <-- composable transformation
    infer)

;; =============================================================================
;; ## Part 6: Comparison - Direct vs Dataflow
;; =============================================================================

(def comparison-data {:x [1 2 3] :y [2 4 6]})

;; ### Direct Approach
(defn direct-plot [data x y]
  {:type "scatter"
   :mode "markers"
   :x (get data x)
   :y (get data y)})

^:kind/pprint
(direct-plot comparison-data :x :y)

;; Problems:
;; - Can't inspect intermediate steps
;; - Can't override scale inference
;; - Can't add transformations
;; - Locked into one output format

;; ### Dataflow Approach
^:kind/pprint
(-> (plot comparison-data)
    (aes :x :x :y :y)
    (geom :point)
    infer)

;; Benefits:
;; - Inspect at any stage: (-> ... (aes :x :x) :sub/map)
;; - Override anything: (-> ... (add-substitution :=x-scale {...}))
;; - Compose: (-> ... scale-x-log ...)
;; - Multiple backends: render-plotly vs render-text

;; =============================================================================
;; ## Part 7: From Toy to Real
;; =============================================================================

;; Our minimal implementation has the core concepts, but the real
;; `tableplot.v2.dataflow` adds:
;;
;; 1. **Dependency Tracking**: Rules can declare dependencies (::depends-on metadata)
;; 2. **Topological Sort**: Infers keys in dependency order
;; 3. **Dataset Handling**: Stops walking into tech.ml.dataset objects
;; 4. **Error Messages**: Better errors when rules are missing
;; 5. **Layers**: Support for multiple geoms on one plot
;; 6. **Scales per Aesthetic**: Separate x-scale, y-scale, color-scale, etc.
;; 7. **Guides**: Automatic axes and legends
;; 8. **Faceting**: Small multiples support
;;
;; But the **fundamental architecture is the same**:
;; - Templates with subkeys (`:=` prefix convention)
;; - :sub/map for values
;; - Inference rules
;; - apply-substitutions
;;
;; This separation of concerns enables the rich ggplot2-style API you saw
;; in the main walkthrough!

;; =============================================================================
;; ## Part 8: Design Alternatives Considered
;; =============================================================================

;; ### Alternative 1: Builder Pattern
(comment
  (-> (PlotBuilder.)
      (.setData data)
      (.setX :x)
      (.setY :y)
      (.setGeom :point)
      (.build))

  ;; Pros: Type-safe, familiar
  ;; Cons: Not inspectable, not data-driven, more code
  )

;; ### Alternative 2: Function Composition
(comment
  (-> data
      (with-x :x)
      (with-y :y)
      (as-points)
      render)

  ;; Pros: Simple, direct
  ;; Cons: Hard to override inferred values, no inspection
  )

;; ### Alternative 3: Multimethods
(comment
  (defmulti render-geom :geom)
  (defmethod render-geom :point [spec] ...)

  ;; Pros: Extensible
  ;; Cons: Global, harder to trace, no substitution model
  )

;; ### Why Dataflow Won:
;;
;; 1. **Inspectable**: Can see the spec at any stage
;; 2. **Override**: Can replace any inferred value
;; 3. **Composable**: Easy to write transformation functions
;; 4. **Declarative**: Specs are data, not opaque objects
;; 5. **Extensible**: Add new inference rules without changing core
;; 6. **Multiple Backends**: Same spec, different renderers

;; =============================================================================
;; ## Exercises: Try It Yourself!
;; =============================================================================

;; 1. Add a new aesthetic (size):
;;    - Add :=size-field, :=size-data to the template
;;    - Write an inference rule for :=size-data
;;    - Test with (aes :x :x :y :y :size :z)

;; 2. Add a new scale type (sqrt):
;;    - Modify the :=x-scale rule to detect large ranges
;;    - Use sqrt scale when max/min ratio > 100

;; 3. Add a new backend (Vega):
;;    - Write render-vega function
;;    - Map the inferred spec to Vega-Lite format

;; 4. Add layers:
;;    - Modify template to support :layers [:=layer1 :=layer2]
;;    - Allow multiple geoms on one plot

;; 5. Add dependency tracking:
;;    - Add ::depends-on metadata to rules
;;    - Implement proper topological sort

;; =============================================================================
;; ## Conclusion
;; =============================================================================

;; The dataflow model provides a **powerful foundation** for a composable
;; grammar of graphics. By separating construction, inference, and rendering:
;;
;; - Users get a clean, composable API
;; - Developers can add features independently
;; - The system remains inspectable and debuggable
;;
;; This is why Tableplot V2 uses this architecture - it's not just clever,
;; it's **essential** for building a flexible, extensible plotting library!

;; =============================================================================
;; ## Summary: The Core Ideas
;; =============================================================================

;; 1. **Substitution Keys** - Keywords with `=` prefix (`:=x-field`) as placeholders
;; 2. **Templates** - Structure with holes to fill
;; 3. **Inference Rules** - Functions that compute values for subkeys
;; 4. **Three Stages** - Construction → Inference → Rendering
;; 5. **Inspection** - Can see :sub/map at any time
;; 6. **Override** - Can replace any inferred value
;; 7. **Composition** - Easy to write transformation functions
;; 8. **Backends** - Multiple renderers for same spec

;; Now you understand the V2 dataflow model! 🎉
