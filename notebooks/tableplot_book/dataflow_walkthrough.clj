;; # Dataflow Model Walkthrough 🌊

;; Tableplot includes a general-purpose dataflow system for creating templates with automatic dependency resolution and caching. While this system powers the visualization layer functions in Tableplot, it can be used for any kind of composable, data-driven transformations. It is inspired by [Hanami](http://github.com/jsa-aerial/hanami)'s templating but is different in a few ways.

;; This walkthrough explores the `xform`, `dag`, and `cache` namespaces that implement this dataflow model.

;; ## Overview

;; The dataflow system consists of three core components:

;; 1. **Template Transformation** (`xform.clj`) - A template substitution system adapted from Hanami that recursively replaces keys with their values
;; 2. **Dependency Management** (`dag.clj`) - Functions that declare their dependencies and are computed in the correct order
;; 3. **Caching** (`cache.clj`) - Memoization of computed values to avoid redundant computation

;; Together, these create a system for building composable, lazy, dependency-aware computations.

;; ## Setup

(ns tableplot-book.dataflow-walkthrough
  (:require [scicloj.tableplot.v1.xform :as xform]
            [scicloj.tableplot.v1.dag :as dag]
            [scicloj.tableplot.v1.cache :as cache]
            [aerial.hanami.templates :as ht]
            [aerial.hanami.common :as hc]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]))

;; ## Part 1: Template Transformation with `xform`

;; ### The Problem: Repetitive Configuration

;; Imagine building visualization specifications. You find yourself writing the same structure over and over:

;; ```clojure
;; {:plot {:data my-data
;;         :layout {:title {:text "My Chart"}}}
;;  :plot {:data other-data
;;         :layout {:title {:text "Other Chart"}}}
;; ```

;; You want to parameterize these structures - define a template once, then fill in the values. But you also want:
;; - **Computed values** that depend on other parameters
;; - **Sensible defaults** that can be overridden
;; - **Deep nesting** without manually threading values through every level
;; - **Optional parameters** that don't clutter output when unused

;; Template transformation solves this by treating data structures as templates with placeholder keys that get recursively replaced with their values.

;; ### Basic Substitution

;; The `xform` function performs template substitution. It takes a template (any nested data structure) and a substitution map, then recursively replaces keys with their values until no more substitutions are possible:

(xform/xform
 {:a :B
  :c :D}
 {:B :C
  :C 10
  :D 20})

(kind/test-last [#(= % {:a 10, :c 20})])

;; ### Recursive Lookup and Termination

;; The substitution process is truly recursive: **any non-collection value** (strings, numbers, keywords, etc.) that appears as a substitution result triggers another lookup. This continues until either:

;; 1. A value is found that isn't in the substitution map (terminates)
;; 2. A value maps to itself (terminates)

;; Strings and numbers can act as template keys:

(xform/xform
 {:title :Title}
 {:Title "MyTitle"
  "MyTitle" "Resolved!"})

(kind/test-last [#(= % {:title "Resolved!"})])

;; This enables indirect lookups through intermediate values:

(xform/xform
 {:result :Key}
 {:Key "lookup-this"
  "lookup-this" 42})

(kind/test-last [#(= % {:result 42})])

;; Termination occurs when a key maps to itself or to a value not in the map:

(xform/xform
 {:self-ref :X
  :not-found :Y}
 {:X :X ; maps to itself
  :Y :Missing}) ; :Missing not in map

(kind/test-last [#(= % {:self-ref :X, :not-found :Missing})])

;; ### Dataset Pass-Through

;; **Important for Tableplot users:** Dataset objects are **never transformed**. They pass through unchanged, preventing xform from recursively processing their internal structure:

(xform/xform
 {:data :Data
  :nested {:deep {:data :Data}}}
 {:Data (tc/dataset {:x [1 2 3] :y [4 5 6]})})

(kind/test-last [#(and (= (-> % :data tc/column-names) [:x :y])
                       (= (-> % :nested :deep :data tc/column-names) [:x :y]))])

;; This is crucial because datasets contain complex internal structures that shouldn't be treated as templates.

;; ### Function Values

;; Substitution values can be functions that receive the entire substitution map, enabling dynamic computation based on other values:

(xform/xform
 {:greeting :Greeting}
 {:Name "Alice"
  :Greeting (fn [{:keys [Name]}]
              (str "Hello, " Name "!"))})

(kind/test-last [#(= % {:greeting "Hello, Alice!"})])

;; ### Template Defaults

;; Templates can include their own defaults via the `:aerial.hanami.templates/defaults` key. These defaults are merged with the provided substitution map.

;; **Note:** When you have no user substitutions, you can omit the empty map `{}`:

(xform/xform
 {:message :Message
  :aerial.hanami.templates/defaults {:Name "World"
                                     :Message (fn [{:keys [Name]}]
                                                (str "Hello, " Name "!"))}})

(kind/test-last [#(= % {:message "Hello, World!"})])

;; **Important:** Template defaults (from `:aerial.hanami.templates/defaults` in the template) take precedence over values in the substitution map when using the map form. To override template defaults, use the multi-arity form with key-value pairs:

(xform/xform
 {:message :Message
  :aerial.hanami.templates/defaults {:Name "World"
                                     :Message (fn [{:keys [Name]}]
                                                (str "Hello, " Name "!"))}}
 :Name "Clojure")

(kind/test-last [#(= % {:message "Hello, Clojure!"})])

;; Alternatively, you can use `:aerial.hanami.common/user-kvs` in the substitution map to explicitly override template defaults:

(xform/xform
 {:message :Message
  :aerial.hanami.templates/defaults {:Name "World"
                                     :Message (fn [{:keys [Name]}]
                                                (str "Hello, " Name "!"))}}
 {:Name "Clojure"
  :aerial.hanami.common/user-kvs {:Name "Clojure"}})

(kind/test-last [#(= % {:message "Hello, Clojure!"})])

;; ### Nested Template Defaults

;; Template defaults can appear **at any level** of the structure, not just at the top. Each nested map can have its own `:aerial.hanami.templates/defaults`:

(xform/xform
 {:title :Title
  :section {:heading :Heading
            :aerial.hanami.templates/defaults {:Heading "Default Heading"}}
  :aerial.hanami.templates/defaults {:Title "Default Title"}})

(kind/test-last [#(= % {:title "Default Title"
                        :section {:heading "Default Heading"}})])

;; Nested defaults can reference values from parent-level defaults:

(xform/xform
 {:outer {:inner :InnerValue
          :aerial.hanami.templates/defaults {:InnerValue (fn [{:keys [OuterValue]}]
                                                           (str "Inner uses: " OuterValue))}}
  :aerial.hanami.templates/defaults {:OuterValue "Parent Value"}})

(kind/test-last [#(= % {:outer {:inner "Inner uses: Parent Value"}})])

;; User substitutions override nested defaults:

(xform/xform
 {:section {:heading :Heading
            :aerial.hanami.templates/defaults {:Heading "Default Heading"}}}
 :Heading "User Heading")

(kind/test-last [#(= % {:section {:heading "User Heading"}})])

;; This enables modular, composable template design where different parts of a template can have their own defaults.

;; ### Nested Defaults with Dependencies

;; Nested defaults work seamlessly with `dag/fn-with-deps` (covered in Part 2). Functions in nested defaults can depend on values from parent defaults:

(xform/xform
 {:config {:database {:url :DbUrl
                      :pool-size :PoolSize}
           :aerial.hanami.templates/defaults {:DbHost "localhost"
                                              :DbPort 5432
                                              :DbName "mydb"
                                              :DbUrl (dag/fn-with-deps nil [DbHost DbPort DbName]
                                                                       (str "postgresql://" DbHost ":" DbPort "/" DbName))
                                              :PoolSize (dag/fn-with-deps nil [Environment]
                                                                          (if (= Environment "prod") 50 10))}}
  :aerial.hanami.templates/defaults {:Environment "prod"}})

(kind/test-last [#(= % {:config {:database {:url "postgresql://localhost:5432/mydb"
                                            :pool-size 50}}})])

;; Notice how:
;; - `DbUrl` depends on `DbHost`, `DbPort`, `DbName` (all local to the nested defaults)
;; - `PoolSize` depends on `Environment` (from the parent defaults)
;; - Dependencies are resolved correctly regardless of nesting level

;; ### Nested Structures and Collections

;; Template transformation works recursively on nested structures and collections:

(xform/xform
 {:user {:name :UserName
         :age :UserAge}
  :items [{:x :X} {:y :Y}]}
 {:UserName "Bob"
  :UserAge 30
  :X 10
  :Y 20})

(kind/test-last [#(= % {:user {:name "Bob", :age 30}
                        :items [{:x 10} {:y 20}]})])

;; ### Recursive Empty Collection Removal

;; One of Hanami's most powerful features is the **recursive** removal of empty collections. This allows you to create elaborate templates with many optional parameters, yet produce lean results when most parameters use their default `hc/RMV` value.

;; By default, empty collections are removed from the output. The removal is recursive - when a collection becomes empty, its parent may also become empty, cascading upward:

(xform/xform
 {:outer {:middle {:inner []}}})

(kind/test-last [#(= % {})])

;; The mechanism uses the special `hc/RMV` value. Most template parameters default to `hc/RMV`, meaning they're only included if you provide a value:

(xform/xform
 {:title :Title
  :subtitle :Subtitle
  :footer :Footer}
 {:Title "My Chart"
  :Subtitle hc/RMV
  :Footer hc/RMV})

(kind/test-last [#(= % {:title "My Chart"})])

;; #### Elaborate Templates → Lean Results

;; This recursive removal enables a powerful pattern: create comprehensive templates with many optional parameters, but get lean output when using defaults. Here's a realistic visualization template:

(xform/xform
 {:plot {:data :Data
         :layout {:title {:text :Title
                          :font {:size :TitleSize
                                 :family :TitleFamily
                                 :color :TitleColor}
                          :x :TitleX
                          :y :TitleY}
                  :xaxis {:title :XAxisTitle
                          :showgrid :ShowXGrid
                          :gridcolor :XGridColor
                          :gridwidth :XGridWidth}
                  :yaxis {:title :YAxisTitle
                          :showgrid :ShowYGrid
                          :gridcolor :YGridColor
                          :gridwidth :YGridWidth}
                  :annotations :Annotations
                  :shapes :Shapes
                  :images :Images}}
  :aerial.hanami.templates/defaults {:TitleSize hc/RMV
                                      :TitleFamily hc/RMV
                                      :TitleColor hc/RMV
                                      :TitleX hc/RMV
                                      :TitleY hc/RMV
                                      :XAxisTitle hc/RMV
                                      :ShowXGrid hc/RMV
                                      :XGridColor hc/RMV
                                      :XGridWidth hc/RMV
                                      :YAxisTitle hc/RMV
                                      :ShowYGrid hc/RMV
                                      :YGridColor hc/RMV
                                      :YGridWidth hc/RMV
                                      :Annotations hc/RMV
                                      :Shapes hc/RMV
                                      :Images hc/RMV}}
 :Data [{:x [1 2 3] :y [4 5 6] :type "scatter"}]
 :Title "Simple Chart")

(kind/test-last [#(= % {:plot {:data [{:x [1 2 3] :y [4 5 6] :type "scatter"}]
                               :layout {:title {:text "Simple Chart"}}}})])

;; Notice how the elaborate template with 17 parameters produces a lean result with just the data and title! The recursive removal cascaded through empty maps: `:font`, `:xaxis`, `:yaxis` all removed, leaving only the specified values.

;; **Why This Matters for Tableplot:**

;; Tableplot's layer functions define comprehensive templates with dozens of optional parameters for colors, sizes, opacity, tooltips, hover behavior, etc. Most users only need a few of these. Thanks to recursive `hc/RMV` removal:

;; 1. **Template authors** can provide complete, well-documented APIs
;; 2. **Users** get clean results without specifying irrelevant parameters  
;; 3. **Default values** are `hc/RMV`, so unspecified options don't clutter output
;; 4. **Progressive disclosure** - start simple, add complexity only when needed

;; This pattern enables templates to be both powerful and approachable.

;; #### Conditional Defaults

;; You can create **conditional defaults** by using functions that return `hc/RMV` based on logic. This enables parameters that are only included when certain conditions are met:

(xform/xform
 {:title :Title
  :subtitle :Subtitle
  :aerial.hanami.templates/defaults {:ShowSubtitle true
                                      :Title "My Chart"
                                      :Subtitle (fn [{:keys [ShowSubtitle]}]
                                                  (if ShowSubtitle "A subtitle" hc/RMV))}})

(kind/test-last [#(= % {:title "My Chart", :subtitle "A subtitle"})])

;; When the condition is false, the value becomes `hc/RMV` and gets removed:

(xform/xform
 {:title :Title
  :subtitle :Subtitle
  :aerial.hanami.templates/defaults {:ShowSubtitle true
                                      :Title "My Chart"
                                      :Subtitle (fn [{:keys [ShowSubtitle]}]
                                                  (if ShowSubtitle "A subtitle" hc/RMV))}}
 :ShowSubtitle false)

(kind/test-last [#(= % {:title "My Chart"})])

;; This pattern is powerful for:
;; - Feature flags (show/hide UI elements)
;; - Environment-specific configuration (only include debug info in dev mode)
;; - Conditional validation (only validate when validation is enabled)
;; - Progressive disclosure (show advanced options only when requested)

;; ### Moving Forward: Dependencies

;; So far we've seen how to create flexible templates with computed values using functions. But there's a limitation: when one parameter depends on another, we have to ensure values are computed in the right order. In the next part, we'll see how the `dag` namespace solves this by letting functions **declare their dependencies** explicitly.

;; ## Part 2: Dependency-Aware Functions with `dag`

;; ### The Problem: Manual Dependency Ordering

;; Template transformation lets us parameterize structures, but what if one parameter depends on computing another first?

;; ```clojure
;; ;; We want circumference, which needs radius, which needs area
;; {:Area 100
;;  :Radius ...needs Area first...
;;  :Circumference ...needs Radius first...}
;; ```

;; Without dependency management, you have to:

;; 1. Manually compute values in the right order
;; 2. Pass intermediate results around
;; 3. Remember which values depend on which
;; 4. Update all call sites when dependencies change

;; The `dag` namespace solves this by letting functions **declare their dependencies**. The system automatically:
;; - Computes values in the correct order
;; - Resolves transitive dependencies (A needs B, B needs C → compute C, then B, then A)
;; - Handles complex graphs like diamonds (A and B both need C → compute C once)
;; - Caches intermediate results to avoid redundant work

;; ### `fn-with-deps`: Declaring Dependencies

;; The `fn-with-deps` macro creates a function that declares its dependencies. Before the function is called, its dependencies are automatically resolved via `xform`:

(xform/xform
 {:b :B
  :c :C
  :aerial.hanami.templates/defaults
  {:A 10
   :B (dag/fn-with-deps "B depends on A"
                        [A]
                        (* A 2))
   :C (dag/fn-with-deps "C depends on B"
                        [B]
                        (+ B 5))}})

(kind/test-last [#(= % {:b 20, :c 25})])

;; ### Diamond Dependencies

;; The system handles complex dependency graphs, including diamond patterns where multiple paths lead to the same node:

(xform/xform
 {:result :E
  :aerial.hanami.templates/defaults
  {:A 5
   :B (dag/fn-with-deps nil [A] (* A 2))
   :C (dag/fn-with-deps nil [A] (+ A 3))
   :D (dag/fn-with-deps nil [B C] (+ B C))
   :E (dag/fn-with-deps nil [D] (* D 10))}})

(kind/test-last [#(= % {:result 180})])

;; ### Circular Dependencies

;; **Important**: The system does not detect circular dependencies. If you create a cycle (A depends on B, B depends on A), you'll get a `StackOverflowError`:

;; ```clojure
;; (xform/xform
;;  {:result :A
;;   :aerial.hanami.templates/defaults
;;   {:A (dag/fn-with-deps nil [B] (inc B))
;;    :B (dag/fn-with-deps nil [A] (inc A))}})
;; ;; => StackOverflowError
;; ```

;; When you encounter a `StackOverflowError`, check your dependency graph for cycles. Each function should only depend on values that don't transitively depend on it.

;; ### `defn-with-deps`: Named Functions

;; For reusable functions with dependencies, use `defn-with-deps`:

(dag/defn-with-deps area->radius
  "Compute radius from area"
  [Area]
  (Math/sqrt (/ Area Math/PI)))

(dag/defn-with-deps radius->circumference
  "Compute circumference from radius"
  [Radius]
  (* 2 Math/PI Radius))

(xform/xform
 {:circumference :Circumference
  :aerial.hanami.templates/defaults
  {:Area 100
   :Radius area->radius
   :Circumference radius->circumference}})

(kind/test-last [#(let [expected (* 2 Math/PI (Math/sqrt (/ 100 Math/PI)))]
                    (< (Math/abs (- (:circumference %) expected)) 0.001))])

;; ### Inspecting Dependencies

;; Functions created with `fn-with-deps` carry metadata about their dependencies, accessible via `(meta fn)` with the key `:scicloj.tableplot.v1.dag/dep-ks`. This is useful for debugging and understanding dependency graphs:

(:scicloj.tableplot.v1.dag/dep-ks (meta area->radius))

(kind/test-last [#(= % [:Area])])

(:scicloj.tableplot.v1.dag/dep-ks (meta radius->circumference))

(kind/test-last [#(= % [:Radius])])

;; You can also inspect anonymous functions:

(def complex-fn
  (dag/fn-with-deps "Complex computation"
                    [A B C]
                    (+ A B C)))

(:scicloj.tableplot.v1.dag/dep-ks (meta complex-fn))

(kind/test-last [#(= % [:A :B :C])])

;; ### Moving Forward: Caching

;; We now have dependency-aware functions that compute values in the correct order. But what happens when the same dependency is needed by multiple functions? Without caching, we'd compute the same value multiple times. In the next part, we'll see how the `cache` namespace provides automatic memoization to avoid redundant work.

;; ## Part 3: Caching with `cache`

;; ### The Problem: Redundant Computation

;; With dependencies, the system computes values in the right order. But what if the same value is needed multiple times?

;; ```clojure
;; ;; Both B and C need A
;; {:A (expensive-computation)
;;  :B (fn [A] ...)  ; computes A
;;  :C (fn [A] ...)  ; computes A again?!
;; ```

;; Without caching, expensive computations run repeatedly. With large datasets or complex calculations, this becomes a performance bottleneck.

;; The `cache` namespace provides memoization for dependency resolution. When a value is computed once, subsequent requests return the cached result.

;; ### `cached-xform-k`: Memoized Key Resolution

;; The `dag/cached-xform-k` function resolves a key via `xform` and caches the result:

(let [call-count (atom 0)
      expensive-fn (fn [{:keys [X]}]
                     (swap! call-count inc)
                     (println "Computing...")
                     (* X X))]
  (cache/with-clean-cache
    (do
      (dotimes [_ 3]
        (dag/cached-xform-k :Y {:X 5, :Y expensive-fn}))
      @call-count)))

(kind/test-last [#(= % 1)])

;; The `with-clean-cache` macro ensures we start with an empty cache.

;; ### Automatic Caching in Dependencies

;; Functions created with `fn-with-deps` automatically use caching for their dependencies:

(def computation-log (atom []))

(reset! computation-log [])

(cache/with-clean-cache
  (do
    (xform/xform
     {:result :C
      :aerial.hanami.templates/defaults
      {:A (fn [_]
            (swap! computation-log conj :computing-A)
            10)
       :B (dag/fn-with-deps nil [A]
                            (swap! computation-log conj :computing-B)
                            (* A 2))
       :C (dag/fn-with-deps nil [A B]
                            (swap! computation-log conj :computing-C)
                            (+ A B))}})
    @computation-log))

(kind/test-last [#(= % [:computing-A :computing-B :computing-C])])

;; Even though `:A` is needed by both `:B` and `:C`, it's only computed once. The log shows the computation order: `:A` first, then `:B`, then `:C`.

;; ### How Dependencies Are Cached

;; When you use `fn-with-deps`, the system automatically uses `cached-xform-k` for each dependency. Let's trace this more explicitly:

(def detailed-log (atom []))

(reset! detailed-log [])

(cache/with-clean-cache
  (do
    (xform/xform
     {:final :D
      :aerial.hanami.templates/defaults
      {:A (fn [_]
            (swap! detailed-log conj [:computing :A])
            100)
       :B (dag/fn-with-deps nil [A]
                            (swap! detailed-log conj [:computing :B :with-A A])
                            (* A 2))
       :C (dag/fn-with-deps nil [A]
                            (swap! detailed-log conj [:computing :C :with-A A])
                            (+ A 50))
       :D (dag/fn-with-deps nil [B C]
                            (swap! detailed-log conj [:computing :D :with-B B :with-C C])
                            (+ B C))}})
    @detailed-log))

(kind/test-last [#(= % [[:computing :A]
                        [:computing :B :with-A 100]
                        [:computing :C :with-A 100]
                        [:computing :D :with-B 200 :with-C 150]])])

;; The log shows that:

;; 1. `:A` is computed once (even though both `:B` and `:C` need it)
;; 2. `:B` and `:C` receive the cached value `100`
;; 3. `:D` receives the cached values `200` and `150`

;; This automatic caching of dependencies is why `fn-with-deps` is so powerful - you declare what you need, and the system ensures it's computed exactly once.

;; ### Cache Keys

;; The cache key includes both the key name and the substitution map, so different contexts maintain separate caches:

(cache/with-clean-cache
  [(dag/cached-xform-k :Result {:X 5, :Result (fn [{:keys [X]}] (* X X))})
   (dag/cached-xform-k :Result {:X 7, :Result (fn [{:keys [X]}] (* X X))})])

(kind/test-last [#(= % [25 49])])

;; ### All Three Systems Together

;; We've now seen the three core components:

;; 1. **Templates** (`xform`) - Parameterize structures with computed values
;; 2. **Dependencies** (`dag`) - Declare what each value needs, computed in the right order
;; 3. **Caching** (`cache`) - Avoid redundant computation

;; These work seamlessly together: templates can use dependency-aware functions, and dependencies are automatically cached. In the next part, we'll see realistic examples of all three systems working in harmony.

;; ## Part 4: Putting It Together

;; Now let's see how all three systems work together in a realistic scenario.

;; ### Comprehensive Example: Data Visualization Pipeline

;; Let's build a data processing and visualization pipeline that demonstrates templates, dependencies, and caching working together:

(def viz-pipeline
  {:visualization
   {:data :ProcessedData
    :layout {:title {:text :ChartTitle
                     :font {:size :TitleFontSize
                            :color :TitleColor}}
             :xaxis {:title :XAxisLabel
                     :gridcolor :GridColor}
             :yaxis {:title :YAxisLabel
                     :gridcolor :GridColor}}
    :aerial.hanami.templates/defaults
    {;; Data pipeline dependencies
     :RawData (tc/dataset {:x [1 2 3 4 5]
                           :y [10 15 13 17 20]})
     :FilteredData (dag/fn-with-deps
                    "Filter data based on threshold"
                    [RawData MinValue]
                    (tc/select-rows RawData #(> (:y %) MinValue)))
     :ProcessedData (dag/fn-with-deps
                     "Transform filtered data"
                     [FilteredData ScaleFactor]
                     (tc/map-columns FilteredData :y [:y] #(* % ScaleFactor)))

     ;; Computed labels based on processing
     :XAxisLabel (dag/fn-with-deps nil [RawData]
                                   (str "X Values (n=" (tc/row-count RawData) ")"))
     :YAxisLabel (dag/fn-with-deps nil [ScaleFactor]
                                   (str "Y Values (scaled ×" ScaleFactor ")"))

     ;; Conditional styling
     :ChartTitle (dag/fn-with-deps nil [MinValue ScaleFactor]
                                   (str "Filtered & Scaled Data (threshold=" MinValue ", scale=" ScaleFactor ")"))
     :TitleFontSize (dag/fn-with-deps nil [Environment]
                                      (if (= Environment "presentation") 24 16))
     :TitleColor (dag/fn-with-deps nil [Environment]
                                   (if (= Environment "dark-mode") "#ffffff" "#000000"))
     :GridColor (dag/fn-with-deps nil [Environment]
                                  (if (= Environment "dark-mode") "#444444" hc/RMV))

     ;; Parameters with defaults
     :MinValue 12
     :ScaleFactor 2
     :Environment "standard"}}
   :aerial.hanami.templates/defaults {:TitleFontSize hc/RMV
                                       :TitleColor hc/RMV
                                       :GridColor hc/RMV}})

;; Use it with defaults:

^:kind/pprint
(cache/with-clean-cache
  (xform/xform viz-pipeline))

(kind/test-last [#(and (= (get-in % [:visualization :layout :title :text])
                          "Filtered & Scaled Data (threshold=12, scale=2)")
                       (= (get-in % [:visualization :layout :xaxis :title])
                          "X Values (n=5)")
                       (= (get-in % [:visualization :layout :yaxis :title])
                          "Y Values (scaled ×2)")
                       (= (tc/row-count (get-in % [:visualization :data]))
                          4))])

;; Override for presentation mode:

^:kind/pprint
(cache/with-clean-cache
  (xform/xform viz-pipeline
               :Environment "presentation"
               :MinValue 14))

(kind/test-last [#(and (= (get-in % [:visualization :layout :title :text])
                          "Filtered & Scaled Data (threshold=14, scale=2)")
                       (= (get-in % [:visualization :layout :title :font :size])
                          24)
                       (= (tc/row-count (get-in % [:visualization :data]))
                          3))])

;; This example demonstrates:
;; - **Templates**: Nested structure with `:aerial.hanami.templates/defaults` at multiple levels
;; - **Dependencies**: Data processing pipeline where each step depends on previous ones
;; - **Caching**: Each dependency computed once, even when needed by multiple downstream computations
;; - **Conditional logic**: Styling changes based on environment
;; - **Recursive removal**: Unused styling options removed from output
;; - **Easy overrides**: Change environment and threshold with simple parameters

;; ### Configuration System

;; A configuration system demonstrates how template defaults, functions, and dependencies combine:

(def app-config
  {:Environment "production"
   :DatabaseHost "db.example.com"
   :DatabasePort 5432
   :DatabaseName "myapp"
   :DatabaseURL (dag/fn-with-deps
                 "Build database connection string"
                 [DatabaseHost DatabasePort DatabaseName]
                 (format "postgresql://%s:%d/%s"
                         DatabaseHost DatabasePort DatabaseName))
   :LogLevel (dag/fn-with-deps
              "Set log level based on environment"
              [Environment]
              (if (= Environment "production") "WARN" "DEBUG"))
   :MaxConnections (dag/fn-with-deps
                    "Set connection pool size based on environment"
                    [Environment]
                    (if (= Environment "production") 50 10))})

(xform/xform
 {:db-url :DatabaseURL
  :log-level :LogLevel
  :max-conn :MaxConnections}
 app-config)

(kind/test-last [#(= % {:db-url "postgresql://db.example.com:5432/myapp"
                        :log-level "WARN"
                        :max-conn 50})])

;; Override for development:

(xform/xform
 {:db-url :DatabaseURL
  :log-level :LogLevel
  :max-conn :MaxConnections}
 (assoc app-config :Environment "development"))

(kind/test-last [#(= % {:db-url "postgresql://db.example.com:5432/myapp"
                        :log-level "DEBUG"
                        :max-conn 10})])

;; ### Lazy Evaluation

;; The system is lazy - values are only computed when needed:

(def lazy-counter (atom 0))

(reset! lazy-counter 0)

(xform/xform
 {:needed :A
  :aerial.hanami.templates/defaults
  {:A (fn [_]
        (swap! lazy-counter inc)
        "value-a")
   :B (fn [_]
        (swap! lazy-counter inc)
        "value-b")}})

(deref lazy-counter)

(kind/test-last [#(= % 1)])

;; ### How Tableplot Uses This System

;; Tableplot's layer functions use this dataflow system extensively. Here's a simplified example:

(defn simplified-layer-point [dataset options]
  (let [layer-defaults
        {:=dataset dataset
         :=mark :point
         :=x (get options :=x :x)
         :=y (get options :=y :y)
         :=x-data (dag/fn-with-deps nil [=dataset =x]
                                    (get =dataset =x))
         :=y-data (dag/fn-with-deps nil [=dataset =y]
                                    (get =dataset =y))
         :=trace (dag/fn-with-deps nil [=x-data =y-data =mark]
                                   {:type (name =mark)
                                    :x =x-data
                                    :y =y-data})}]
    {:aerial.hanami.templates/defaults (merge layer-defaults options)
     :kindly/f #(xform/xform %)}))

;; This pattern allows:
;; - Lazy computation of trace data
;; - Automatic dependency resolution (`=trace` depends on `=x-data`, `=y-data`, and `=mark`)
;; - Easy override of any parameter
;; - Caching of expensive operations

;; ## Summary

;; The dataflow system provides:

;; 1. **Template Transformation**: Recursive substitution of keys with values, including function-computed values
;; 2. **Dependency Management**: Explicit declaration of dependencies with automatic resolution
;; 3. **Caching**: Memoization to avoid redundant computation
;; 4. **Composability**: Templates can be merged, parameterized, and reused
;; 5. **Laziness**: Values only computed when needed

;; These features combine to create a system for building complex, composable, data-driven transformations - useful far beyond just data visualization!

;; ## Key Takeaways

;; - Use `xform/xform` for template substitution with recursive key replacement
;; - Use `dag/fn-with-deps` to create functions that declare their dependencies
;; - Use `dag/defn-with-deps` for reusable named functions with dependencies
;; - Use `cache/with-clean-cache` to control caching behavior in tests
;; - Dependencies are automatically cached to avoid redundant computation
;; - The system handles complex dependency graphs including diamonds and chains
;; - Templates can include defaults via `:aerial.hanami.templates/defaults`
;; - The system is lazy - values are only computed when requested
;; - Template defaults take precedence over map values; use multi-arity form or `:aerial.hanami.common/user-kvs` to override
;; - `hc/RMV` combined with `:aerial.hanami.common/rmv-empty?` enables conditional template inclusion

;; For more information:
;; - [Hanami documentation](https://github.com/jsa-aerial/hanami) - The template system that inspired `xform`
;; - [Plotly walkthrough](tableplot_book.plotly_walkthrough.html) - See the dataflow system in action
;; - [Plotly reference](tableplot_book.plotly_reference.html) - Comprehensive examples of the dataflow system
