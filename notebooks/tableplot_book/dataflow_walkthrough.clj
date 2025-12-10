;; # Dataflow Model Walkthrough 🌊

;; Tableplot's dataflow system creates templates with automatic dependency resolution and caching. This system is inspired by the work of Jon Anthony (jsa-aerial), author of [Hanami](http://github.com/jsa-aerial/hanami), building on Hanami's template substitution while adding explicit dependency management.

;; Hanami drew from [lambda calculus](https://en.wikipedia.org/wiki/Lambda_calculus) ideas. This dataflow model applies similar minimalistic principles to parameterized data transformations.

;; This walkthrough explores three namespaces: `xform` for template transformation, `dag` for dependency management, and `cache` for memoization.

;; ## Setup

(ns tableplot-book.dataflow-walkthrough
  (:require [scicloj.tableplot.v1.xform :as xform]
            [scicloj.tableplot.v1.dag :as dag]
            [scicloj.tableplot.v1.cache :as cache]
            [aerial.hanami.templates :as ht]
            [aerial.hanami.common :as hc]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]))

;; ## Template Transformation with `xform`

;; Template transformation is [term rewriting](https://en.wikipedia.org/wiki/Rewriting) - we substitute terms until reaching a [fixpoint](https://en.wikipedia.org/wiki/Fixed_point_(mathematics)) (a value that cannot be reduced further).

;; ### Six Rules

;; Given a term `T` and an environment `E` (a map from keys to values):

;; **Rule 1: Lookup** - If `T` is a key in `E`, replace with `E[T]` and recurse.

;; **Rule 2: Identity** - If `T` is a key not in `E`, it's a fixpoint.

;; **Rule 3: Function Application** - If `T` is a function, apply it to `E` and recurse.

;; **Rule 4: Collection Recursion** - If `T` is a map or vector, recurse on elements.

;; **Rule 5: Template Defaults** - Templates extend the environment via `:aerial.hanami.templates/defaults`.

;; **Rule 6: Value** - Non-collection, non-key values (numbers, strings, datasets) pass through.

;; ### Examples

;; **Lookup and Identity**

(xform/xform
 {:a :B
  :c :D}
 {:B :C
  :C 10
  :D 20})

(kind/test-last [#(= % {:a 10, :c 20})])

;; Here `:a` → `:B` → `:C` → `10` (stops when 10 isn't in the map).

;; **Multi-step Substitution**

;; Even strings and numbers can act as keys:

(xform/xform
 {:title :Title}
 {:Title "MyTitle"
  "MyTitle" "Resolved!"})

(kind/test-last [#(= % {:title "Resolved!"})])

;; **Fixpoint Detection**

(xform/xform
 {:self-ref :X
  :not-found :Y}
 {:X :X ;; maps to itself → fixpoint
  :Y :Missing}) ;; :Missing not in map → fixpoint

(kind/test-last [#(= % {:self-ref :X, :not-found :Missing})])

;; **Dataset Pass-Through**

;; Datasets pass through unchanged:

(xform/xform
 {:data :Data
  :nested {:deep {:data :Data}}}
 {:Data (tc/dataset {:x [1 2 3] :y [4 5 6]})})

(kind/test-last [#(and (= (-> % :data tc/column-names) [:x :y])
                       (= (-> % :nested :deep :data tc/column-names) [:x :y]))])

;; **Function Application**

;; Functions receive the entire environment:

(xform/xform
 {:greeting :Greeting}
 {:Name "Alice"
  :Greeting (fn [{:keys [Name]}]
              (str "Hello, " Name "!"))})

(kind/test-last [#(= % {:greeting "Hello, Alice!"})])

;; **Template Defaults**

;; Templates extend the environment:

(xform/xform
 {:message :Message
  :aerial.hanami.templates/defaults {:Name "World"
                 :Message (fn [{:keys [Name]}]
                            (str "Hello, " Name "!"))}})

(kind/test-last [#(= % {:message "Hello, World!"})])

;; To override defaults, use key-value pairs:

(xform/xform
 {:message :Message
  :aerial.hanami.templates/defaults {:Name "World"
                 :Message (fn [{:keys [Name]}]
                            (str "Hello, " Name "!"))}}
 :Name "Clojure")

(kind/test-last [#(= % {:message "Hello, Clojure!"})])

;; ### Nested Defaults

;; Defaults can appear at any level:

(xform/xform
 {:title :Title
  :section {:heading :Heading
            :aerial.hanami.templates/defaults {:Heading "Default Heading"}}
  :aerial.hanami.templates/defaults {:Title "Default Title"}})

(kind/test-last [#(= % {:title "Default Title"
                        :section {:heading "Default Heading"}})])

;; Inner environments access outer bindings:

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

;; Nested defaults work with `dag/fn-with-deps`:

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

;; **Recursive Transformation**

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

;; ### Removing Empty Structure

;; Empty collections are removed recursively. When a collection becomes empty, its parent may also become empty:

(xform/xform
 {:outer {:middle {:inner []}}})

(kind/test-last [#(= % {})])

;; The `hc/RMV` value marks parameters as optional:

(xform/xform
 {:title :Title
  :subtitle :Subtitle
  :footer :Footer}
 {:Title "My Chart"
  :Subtitle hc/RMV
  :Footer hc/RMV})

(kind/test-last [#(= % {:title "My Chart"})])

;; Here's a realistic visualization template with many optional parameters:

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

;; The 17-parameter template produces lean output with just data and title. Empty maps (`:font`, `:xaxis`, `:yaxis`) are removed.

;; Tableplot layer functions offer dozens of optional parameters. Default everything to `hc/RMV`, and users get clean output without specifying irrelevant details.

;; Functions can return `hc/RMV` to conditionally include values:

(xform/xform
 {:title :Title
  :subtitle :Subtitle
  :aerial.hanami.templates/defaults {:ShowSubtitle true
                 :Title "My Chart"
                 :Subtitle (fn [{:keys [ShowSubtitle]}]
                             (if ShowSubtitle "A subtitle" hc/RMV))}})

(kind/test-last [#(= % {:title "My Chart", :subtitle "A subtitle"})])

(xform/xform
 {:title :Title
  :subtitle :Subtitle
  :aerial.hanami.templates/defaults {:ShowSubtitle true
                 :Title "My Chart"
                 :Subtitle (fn [{:keys [ShowSubtitle]}]
                             (if ShowSubtitle "A subtitle" hc/RMV))}}
 :ShowSubtitle false)

(kind/test-last [#(= % {:title "My Chart"})])

;; ## Dependencies with `dag`

;; Functions can receive the entire environment, but that makes dependencies invisible. The `dag` namespace lets functions declare what they need.

;; **Rule 7: Declared Dependencies** - resolve dependencies before applying function body.

;; The `fn-with-deps` macro declares dependencies:

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

;; The system computes in order: `:C` needs `:B`, which needs `:A`. So A=10, then B=20, then C=25.

;; When multiple values need the same dependency:

(xform/xform
 {:result :E
  :aerial.hanami.templates/defaults
  {:A 5
   :B (dag/fn-with-deps nil [A] (* A 2))
   :C (dag/fn-with-deps nil [A] (+ A 3))
   :D (dag/fn-with-deps nil [B C] (+ B C))
   :E (dag/fn-with-deps nil [D] (* D 10))}})

(kind/test-last [#(= % {:result 180})])

;; Both B and C need A. The system computes A once, then B and C, then D, then E. The system doesn't detect cycles - if A needs B and B needs A, you'll get `StackOverflowError`.

;; For reusable functions:

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

;; Functions carry dependencies in metadata:

(:scicloj.tableplot.v1.dag/dep-ks (meta area->radius))

(kind/test-last [#(= % [:Area])])

(:scicloj.tableplot.v1.dag/dep-ks (meta radius->circumference))

(kind/test-last [#(= % [:Radius])])

(def complex-fn
  (dag/fn-with-deps "Complex computation"
                    [A B C]
                    (+ A B C)))

(:scicloj.tableplot.v1.dag/dep-ks (meta complex-fn))

(kind/test-last [#(= % [:A :B :C])])

;; ## Caching with `cache`

;; The `cache` namespace [memoizes](https://en.wikipedia.org/wiki/Memoization) dependency resolution. If both B and C need A, A is computed once.

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

;; The expensive function ran once despite three lookups. `with-clean-cache` clears the cache before and after. Dependencies are cached automatically:

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

;; `:A` is needed by both `:B` and `:C`, but computed only once. More explicitly:

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

;; A computed once, B and C both receive 100, D receives 200 and 150.

;; The cache key includes the key name and substitution map, so different contexts get separate caches:

(cache/with-clean-cache
  [(dag/cached-xform-k :Result {:X 5, :Result (fn [{:keys [X]}] (* X X))})
   (dag/cached-xform-k :Result {:X 7, :Result (fn [{:keys [X]}] (* X X))})])

(kind/test-last [#(= % [25 49])])

;; ## Examples

;; ### Data Visualization Pipeline

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
    {:RawData (tc/dataset {:x [1 2 3 4 5]
                           :y [10 15 13 17 20]})
     :FilteredData (dag/fn-with-deps
                    "Filter data based on threshold"
                    [RawData MinValue]
                    (tc/select-rows RawData #(> (:y %) MinValue)))
     :ProcessedData (dag/fn-with-deps
                     "Transform filtered data"
                     [FilteredData ScaleFactor]
                     (tc/map-columns FilteredData :y [:y] #(* % ScaleFactor)))
     :XAxisLabel (dag/fn-with-deps nil [RawData]
                                   (str "X Values (n=" (tc/row-count RawData) ")"))
     :YAxisLabel (dag/fn-with-deps nil [ScaleFactor]
                                   (str "Y Values (scaled ×" ScaleFactor ")"))
     :ChartTitle (dag/fn-with-deps nil [MinValue ScaleFactor]
                                   (str "Filtered & Scaled Data (threshold=" MinValue ", scale=" ScaleFactor ")"))
     :TitleFontSize (dag/fn-with-deps nil [Environment]
                                      (if (= Environment "presentation") 24 16))
     :TitleColor (dag/fn-with-deps nil [Environment]
                                   (if (= Environment "dark-mode") "#ffffff" "#000000"))
     :GridColor (dag/fn-with-deps nil [Environment]
                                  (if (= Environment "dark-mode") "#444444" hc/RMV))
     :MinValue 12
     :ScaleFactor 2
     :Environment "standard"}}
   :aerial.hanami.templates/defaults {:TitleFontSize hc/RMV
                  :TitleColor hc/RMV
                  :GridColor hc/RMV}})

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

;; ### Configuration System

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

(xform/xform
 {:db-url :DatabaseURL
  :log-level :LogLevel
  :max-conn :MaxConnections}
 (assoc app-config :Environment "development"))

(kind/test-last [#(= % {:db-url "postgresql://db.example.com:5432/myapp"
                        :log-level "DEBUG"
                        :max-conn 10})])

;; ### Lazy Evaluation

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

;; Only `:A` computed despite `:B` being in the environment.

;; ### Tableplot Layers

(defn simplified-layer-point [dataset options]
  {:aerial.hanami.templates/defaults (merge {:=dataset dataset
                         :=mark :point
                         :=x-data (dag/fn-with-deps nil [=dataset =x]
                                                    (get =dataset =x))
                         :=y-data (dag/fn-with-deps nil [=dataset =y]
                                                    (get =dataset =y))
                         :=trace (dag/fn-with-deps nil [=x-data =y-data =mark]
                                                   {:type (name =mark)
                                                    :x =x-data
                                                    :y =y-data})}
                        options)
   :kindly/f #(xform/xform %)})

;; ## Summary

;; Six rewriting rules plus declared dependencies create a system for parameterized data transformations. This work builds on Jon Anthony's Hanami, which showed how term rewriting could parameterize data structures. Tableplot adds explicit dependency declaration and caching.

;; For more information:
;; - [Hanami documentation](https://github.com/jsa-aerial/hanami) - The template system that inspired `xform`
;; - [Plotly walkthrough](tableplot_book.plotly_walkthrough.html) - See the dataflow system in action
;; - [Plotly reference](tableplot_book.plotly_reference.html) - Comprehensive examples
