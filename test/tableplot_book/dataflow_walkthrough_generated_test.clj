(ns
 tableplot-book.dataflow-walkthrough-generated-test
 (:require
  [scicloj.tableplot.v1.xform :as xform]
  [scicloj.tableplot.v1.dag :as dag]
  [scicloj.tableplot.v1.cache :as cache]
  [aerial.hanami.templates :as ht]
  [aerial.hanami.common :as hc]
  [scicloj.kindly.v4.kind :as kind]
  [tablecloth.api :as tc]
  [clojure.test :refer [deftest is]]))


(def v3_l53 (xform/xform {:a :B, :c :D} {:B :C, :C 10, :D 20}))


(deftest
 t4_l60
 (is ((fn* [p1__85958#] (= p1__85958# {:a 10, :c 20})) v3_l53)))


(def
 v6_l71
 (xform/xform
  {:title :Title}
  {:Title "MyTitle", "MyTitle" "Resolved!"}))


(deftest
 t7_l76
 (is ((fn* [p1__85959#] (= p1__85959# {:title "Resolved!"})) v6_l71)))


(def
 v9_l80
 (xform/xform {:result :Key} {:Key "lookup-this", "lookup-this" 42}))


(deftest
 t10_l85
 (is ((fn* [p1__85960#] (= p1__85960# {:result 42})) v9_l80)))


(def
 v12_l89
 (xform/xform {:self-ref :X, :not-found :Y} {:X :X, :Y :Missing}))


(deftest
 t14_l95
 (is
  ((fn*
    [p1__85961#]
    (= p1__85961# {:self-ref :X, :not-found :Missing}))
   v12_l89)))


(def
 v16_l101
 (xform/xform
  {:data :Data, :nested {:deep {:data :Data}}}
  {:Data (tc/dataset {:x [1 2 3], :y [4 5 6]})}))


(deftest
 t17_l106
 (is
  ((fn*
    [p1__85962#]
    (and
     (= (-> p1__85962# :data tc/column-names) [:x :y])
     (= (-> p1__85962# :nested :deep :data tc/column-names) [:x :y])))
   v16_l101)))


(def
 v19_l115
 (xform/xform
  {:greeting :Greeting}
  {:Name "Alice",
   :Greeting (fn [{:keys [Name]}] (str "Hello, " Name "!"))}))


(deftest
 t20_l121
 (is
  ((fn* [p1__85963#] (= p1__85963# {:greeting "Hello, Alice!"}))
   v19_l115)))


(def
 v22_l129
 (xform/xform
  {:message :Message,
   :aerial.hanami.templates/defaults
   {:Name "World",
    :Message (fn [{:keys [Name]}] (str "Hello, " Name "!"))}}))


(deftest
 t23_l135
 (is
  ((fn* [p1__85964#] (= p1__85964# {:message "Hello, World!"}))
   v22_l129)))


(def
 v25_l139
 (xform/xform
  {:message :Message,
   :aerial.hanami.templates/defaults
   {:Name "World",
    :Message (fn [{:keys [Name]}] (str "Hello, " Name "!"))}}
  :Name
  "Clojure"))


(deftest
 t26_l146
 (is
  ((fn* [p1__85965#] (= p1__85965# {:message "Hello, Clojure!"}))
   v25_l139)))


(def
 v28_l150
 (xform/xform
  {:message :Message,
   :aerial.hanami.templates/defaults
   {:Name "World",
    :Message (fn [{:keys [Name]}] (str "Hello, " Name "!"))}}
  {:Name "Clojure", :aerial.hanami.common/user-kvs {:Name "Clojure"}}))


(deftest
 t29_l158
 (is
  ((fn* [p1__85966#] (= p1__85966# {:message "Hello, Clojure!"}))
   v28_l150)))


(def
 v31_l164
 (xform/xform
  {:title :Title,
   :section
   {:heading :Heading,
    :aerial.hanami.templates/defaults {:Heading "Default Heading"}},
   :aerial.hanami.templates/defaults {:Title "Default Title"}}))


(deftest
 t32_l170
 (is
  ((fn*
    [p1__85967#]
    (=
     p1__85967#
     {:title "Default Title", :section {:heading "Default Heading"}}))
   v31_l164)))


(def
 v34_l175
 (xform/xform
  {:outer
   {:inner :InnerValue,
    :aerial.hanami.templates/defaults
    {:InnerValue
     (fn [{:keys [OuterValue]}] (str "Inner uses: " OuterValue))}},
   :aerial.hanami.templates/defaults {:OuterValue "Parent Value"}}))


(deftest
 t35_l181
 (is
  ((fn*
    [p1__85968#]
    (= p1__85968# {:outer {:inner "Inner uses: Parent Value"}}))
   v34_l175)))


(def
 v37_l185
 (xform/xform
  {:section
   {:heading :Heading,
    :aerial.hanami.templates/defaults {:Heading "Default Heading"}}}
  :Heading
  "User Heading"))


(deftest
 t38_l190
 (is
  ((fn*
    [p1__85969#]
    (= p1__85969# {:section {:heading "User Heading"}}))
   v37_l185)))


(def
 v40_l198
 (xform/xform
  {:config
   {:database {:url :DbUrl, :pool-size :PoolSize},
    :aerial.hanami.templates/defaults
    {:DbHost "localhost",
     :DbPort 5432,
     :DbName "mydb",
     :DbUrl
     (dag/fn-with-deps
      nil
      [DbHost DbPort DbName]
      (str "postgresql://" DbHost ":" DbPort "/" DbName)),
     :PoolSize
     (dag/fn-with-deps
      nil
      [Environment]
      (if (= Environment "prod") 50 10))}},
   :aerial.hanami.templates/defaults {:Environment "prod"}}))


(deftest
 t41_l210
 (is
  ((fn*
    [p1__85970#]
    (=
     p1__85970#
     {:config
      {:database
       {:url "postgresql://localhost:5432/mydb", :pool-size 50}}}))
   v40_l198)))


(def
 v43_l222
 (xform/xform
  {:user {:name :UserName, :age :UserAge}, :items [{:x :X} {:y :Y}]}
  {:UserName "Bob", :UserAge 30, :X 10, :Y 20}))


(deftest
 t44_l231
 (is
  ((fn*
    [p1__85971#]
    (=
     p1__85971#
     {:user {:name "Bob", :age 30}, :items [{:x 10} {:y 20}]}))
   v43_l222)))


(def v46_l240 (xform/xform {:outer {:middle {:inner []}}}))


(deftest t47_l243 (is ((fn* [p1__85972#] (= p1__85972# {})) v46_l240)))


(def
 v49_l247
 (xform/xform
  {:title :Title, :subtitle :Subtitle, :footer :Footer}
  {:Title "My Chart", :Subtitle hc/RMV, :Footer hc/RMV}))


(deftest
 t50_l255
 (is ((fn* [p1__85973#] (= p1__85973# {:title "My Chart"})) v49_l247)))


(def
 v52_l261
 (xform/xform
  {:plot
   {:data :Data,
    :layout
    {:title
     {:text :Title,
      :font
      {:size :TitleSize, :family :TitleFamily, :color :TitleColor},
      :x :TitleX,
      :y :TitleY},
     :xaxis
     {:title :XAxisTitle,
      :showgrid :ShowXGrid,
      :gridcolor :XGridColor,
      :gridwidth :XGridWidth},
     :yaxis
     {:title :YAxisTitle,
      :showgrid :ShowYGrid,
      :gridcolor :YGridColor,
      :gridwidth :YGridWidth},
     :annotations :Annotations,
     :shapes :Shapes,
     :images :Images}},
   :aerial.hanami.templates/defaults
   {:ShowYGrid hc/RMV,
    :TitleY hc/RMV,
    :TitleFamily hc/RMV,
    :TitleColor hc/RMV,
    :Shapes hc/RMV,
    :Images hc/RMV,
    :ShowXGrid hc/RMV,
    :XAxisTitle hc/RMV,
    :YAxisTitle hc/RMV,
    :TitleX hc/RMV,
    :XGridColor hc/RMV,
    :YGridColor hc/RMV,
    :YGridWidth hc/RMV,
    :XGridWidth hc/RMV,
    :Annotations hc/RMV,
    :TitleSize hc/RMV}}
  :Data
  [{:x [1 2 3], :y [4 5 6], :type "scatter"}]
  :Title
  "Simple Chart"))


(deftest
 t53_l299
 (is
  ((fn*
    [p1__85974#]
    (=
     p1__85974#
     {:plot
      {:data [{:x [1 2 3], :y [4 5 6], :type "scatter"}],
       :layout {:title {:text "Simple Chart"}}}}))
   v52_l261)))


(def
 v55_l319
 (xform/xform
  {:title :Title,
   :subtitle :Subtitle,
   :aerial.hanami.templates/defaults
   {:ShowSubtitle true,
    :Title "My Chart",
    :Subtitle
    (fn
     [{:keys [ShowSubtitle]}]
     (if ShowSubtitle "A subtitle" hc/RMV))}}))


(deftest
 t56_l327
 (is
  ((fn*
    [p1__85975#]
    (= p1__85975# {:title "My Chart", :subtitle "A subtitle"}))
   v55_l319)))


(def
 v58_l331
 (xform/xform
  {:title :Title,
   :subtitle :Subtitle,
   :aerial.hanami.templates/defaults
   {:ShowSubtitle true,
    :Title "My Chart",
    :Subtitle
    (fn
     [{:keys [ShowSubtitle]}]
     (if ShowSubtitle "A subtitle" hc/RMV))}}
  :ShowSubtitle
  false))


(deftest
 t59_l340
 (is ((fn* [p1__85976#] (= p1__85976# {:title "My Chart"})) v58_l331)))


(def
 v61_l382
 (xform/xform
  {:b :B,
   :c :C,
   :aerial.hanami.templates/defaults
   {:A 10,
    :B (dag/fn-with-deps "B depends on A" [A] (* A 2)),
    :C (dag/fn-with-deps "C depends on B" [B] (+ B 5))}}))


(deftest
 t62_l394
 (is ((fn* [p1__85977#] (= p1__85977# {:b 20, :c 25})) v61_l382)))


(def
 v64_l400
 (xform/xform
  {:result :E,
   :aerial.hanami.templates/defaults
   {:A 5,
    :B (dag/fn-with-deps nil [A] (* A 2)),
    :C (dag/fn-with-deps nil [A] (+ A 3)),
    :D (dag/fn-with-deps nil [B C] (+ B C)),
    :E (dag/fn-with-deps nil [D] (* D 10))}}))


(deftest
 t65_l409
 (is ((fn* [p1__85978#] (= p1__85978# {:result 180})) v64_l400)))


(def
 v67_l430
 (dag/defn-with-deps
  area->radius
  "Compute radius from area"
  [Area]
  (Math/sqrt (/ Area Math/PI))))


(def
 v68_l435
 (dag/defn-with-deps
  radius->circumference
  "Compute circumference from radius"
  [Radius]
  (* 2 Math/PI Radius)))


(def
 v69_l440
 (xform/xform
  {:circumference :Circumference,
   :aerial.hanami.templates/defaults
   {:Area 100,
    :Radius area->radius,
    :Circumference radius->circumference}}))


(deftest
 t70_l447
 (is
  ((fn*
    [p1__85979#]
    (let
     [expected (* 2 Math/PI (Math/sqrt (/ 100 Math/PI)))]
     (< (Math/abs (- (:circumference p1__85979#) expected)) 0.001)))
   v69_l440)))


(def v72_l454 (:scicloj.tableplot.v1.dag/dep-ks (meta area->radius)))


(deftest
 t73_l456
 (is ((fn* [p1__85980#] (= p1__85980# [:Area])) v72_l454)))


(def
 v74_l458
 (:scicloj.tableplot.v1.dag/dep-ks (meta radius->circumference)))


(deftest
 t75_l460
 (is ((fn* [p1__85981#] (= p1__85981# [:Radius])) v74_l458)))


(def
 v77_l464
 (def
  complex-fn
  (dag/fn-with-deps "Complex computation" [A B C] (+ A B C))))


(def v78_l469 (:scicloj.tableplot.v1.dag/dep-ks (meta complex-fn)))


(deftest
 t79_l471
 (is ((fn* [p1__85982#] (= p1__85982# [:A :B :C])) v78_l469)))


(def
 v81_l498
 (let
  [call-count
   (atom 0)
   expensive-fn
   (fn
    [{:keys [X]}]
    (swap! call-count inc)
    (println "Computing...")
    (* X X))]
  (cache/with-clean-cache
   (do
    (dotimes [_ 3] (dag/cached-xform-k :Y {:X 5, :Y expensive-fn}))
    @call-count))))


(deftest t82_l509 (is ((fn* [p1__85983#] (= p1__85983# 1)) v81_l498)))


(def v84_l517 (def computation-log (atom [])))


(def v85_l519 (reset! computation-log []))


(def
 v86_l521
 (cache/with-clean-cache
  (do
   (xform/xform
    {:result :C,
     :aerial.hanami.templates/defaults
     {:A (fn [_] (swap! computation-log conj :computing-A) 10),
      :B
      (dag/fn-with-deps
       nil
       [A]
       (swap! computation-log conj :computing-B)
       (* A 2)),
      :C
      (dag/fn-with-deps
       nil
       [A B]
       (swap! computation-log conj :computing-C)
       (+ A B))}})
   @computation-log)))


(deftest
 t87_l537
 (is
  ((fn*
    [p1__85984#]
    (= p1__85984# [:computing-A :computing-B :computing-C]))
   v86_l521)))


(def v89_l545 (def detailed-log (atom [])))


(def v90_l547 (reset! detailed-log []))


(def
 v91_l549
 (cache/with-clean-cache
  (do
   (xform/xform
    {:final :D,
     :aerial.hanami.templates/defaults
     {:A (fn [_] (swap! detailed-log conj [:computing :A]) 100),
      :B
      (dag/fn-with-deps
       nil
       [A]
       (swap! detailed-log conj [:computing :B :with-A A])
       (* A 2)),
      :C
      (dag/fn-with-deps
       nil
       [A]
       (swap! detailed-log conj [:computing :C :with-A A])
       (+ A 50)),
      :D
      (dag/fn-with-deps
       nil
       [B C]
       (swap! detailed-log conj [:computing :D :with-B B :with-C C])
       (+ B C))}})
   @detailed-log)))


(deftest
 t92_l568
 (is
  ((fn*
    [p1__85985#]
    (=
     p1__85985#
     [[:computing :A]
      [:computing :B :with-A 100]
      [:computing :C :with-A 100]
      [:computing :D :with-B 200 :with-C 150]]))
   v91_l549)))


(def
 v94_l585
 (cache/with-clean-cache
  [(dag/cached-xform-k
    :Result
    {:X 5, :Result (fn [{:keys [X]}] (* X X))})
   (dag/cached-xform-k
    :Result
    {:X 7, :Result (fn [{:keys [X]}] (* X X))})]))


(deftest
 t95_l589
 (is ((fn* [p1__85986#] (= p1__85986# [25 49])) v94_l585)))


(def
 v97_l609
 (def
  viz-pipeline
  {:visualization
   {:data :ProcessedData,
    :layout
    {:title
     {:text :ChartTitle,
      :font {:size :TitleFontSize, :color :TitleColor}},
     :xaxis {:title :XAxisLabel, :gridcolor :GridColor},
     :yaxis {:title :YAxisLabel, :gridcolor :GridColor}},
    :aerial.hanami.templates/defaults
    {:MinValue 12,
     :Environment "standard",
     :TitleFontSize
     (dag/fn-with-deps
      nil
      [Environment]
      (if (= Environment "presentation") 24 16)),
     :XAxisLabel
     (dag/fn-with-deps
      nil
      [RawData]
      (str "X Values (n=" (tc/row-count RawData) ")")),
     :YAxisLabel
     (dag/fn-with-deps
      nil
      [ScaleFactor]
      (str "Y Values (scaled ×" ScaleFactor ")")),
     :FilteredData
     (dag/fn-with-deps
      "Filter data based on threshold"
      [RawData MinValue]
      (tc/select-rows
       RawData
       (fn* [p1__85987#] (> (:y p1__85987#) MinValue)))),
     :TitleColor
     (dag/fn-with-deps
      nil
      [Environment]
      (if (= Environment "dark-mode") "#ffffff" "#000000")),
     :ChartTitle
     (dag/fn-with-deps
      nil
      [MinValue ScaleFactor]
      (str
       "Filtered & Scaled Data (threshold="
       MinValue
       ", scale="
       ScaleFactor
       ")")),
     :RawData (tc/dataset {:x [1 2 3 4 5], :y [10 15 13 17 20]}),
     :ProcessedData
     (dag/fn-with-deps
      "Transform filtered data"
      [FilteredData ScaleFactor]
      (tc/map-columns
       FilteredData
       :y
       [:y]
       (fn* [p1__85988#] (* p1__85988# ScaleFactor)))),
     :ScaleFactor 2,
     :GridColor
     (dag/fn-with-deps
      nil
      [Environment]
      (if (= Environment "dark-mode") "#444444" hc/RMV))}},
   :aerial.hanami.templates/defaults
   {:TitleFontSize hc/RMV, :TitleColor hc/RMV, :GridColor hc/RMV}}))


(def v99_l658 (cache/with-clean-cache (xform/xform viz-pipeline)))


(deftest
 t100_l662
 (is
  ((fn*
    [p1__85989#]
    (and
     (=
      (get-in p1__85989# [:visualization :layout :title :text])
      "Filtered & Scaled Data (threshold=12, scale=2)")
     (=
      (get-in p1__85989# [:visualization :layout :xaxis :title])
      "X Values (n=5)")
     (=
      (get-in p1__85989# [:visualization :layout :yaxis :title])
      "Y Values (scaled ×2)")
     (= (tc/row-count (get-in p1__85989# [:visualization :data])) 4)))
   v99_l658)))


(def
 v102_l673
 (cache/with-clean-cache
  (xform/xform viz-pipeline :Environment "presentation" :MinValue 14)))


(deftest
 t103_l679
 (is
  ((fn*
    [p1__85990#]
    (and
     (=
      (get-in p1__85990# [:visualization :layout :title :text])
      "Filtered & Scaled Data (threshold=14, scale=2)")
     (=
      (get-in p1__85990# [:visualization :layout :title :font :size])
      24)
     (= (tc/row-count (get-in p1__85990# [:visualization :data])) 3)))
   v102_l673)))


(def
 v105_l698
 (def
  app-config
  {:Environment "production",
   :DatabaseHost "db.example.com",
   :DatabasePort 5432,
   :DatabaseName "myapp",
   :DatabaseURL
   (dag/fn-with-deps
    "Build database connection string"
    [DatabaseHost DatabasePort DatabaseName]
    (format
     "postgresql://%s:%d/%s"
     DatabaseHost
     DatabasePort
     DatabaseName)),
   :LogLevel
   (dag/fn-with-deps
    "Set log level based on environment"
    [Environment]
    (if (= Environment "production") "WARN" "DEBUG")),
   :MaxConnections
   (dag/fn-with-deps
    "Set connection pool size based on environment"
    [Environment]
    (if (= Environment "production") 50 10))}))


(def
 v106_l717
 (xform/xform
  {:db-url :DatabaseURL,
   :log-level :LogLevel,
   :max-conn :MaxConnections}
  app-config))


(deftest
 t107_l723
 (is
  ((fn*
    [p1__85991#]
    (=
     p1__85991#
     {:db-url "postgresql://db.example.com:5432/myapp",
      :log-level "WARN",
      :max-conn 50}))
   v106_l717)))


(def
 v109_l729
 (xform/xform
  {:db-url :DatabaseURL,
   :log-level :LogLevel,
   :max-conn :MaxConnections}
  (assoc app-config :Environment "development")))


(deftest
 t110_l735
 (is
  ((fn*
    [p1__85992#]
    (=
     p1__85992#
     {:db-url "postgresql://db.example.com:5432/myapp",
      :log-level "DEBUG",
      :max-conn 10}))
   v109_l729)))


(def v112_l743 (def lazy-counter (atom 0)))


(def v113_l745 (reset! lazy-counter 0))


(def
 v114_l747
 (xform/xform
  {:needed :A,
   :aerial.hanami.templates/defaults
   {:A (fn [_] (swap! lazy-counter inc) "value-a"),
    :B (fn [_] (swap! lazy-counter inc) "value-b")}}))


(def v115_l757 (deref lazy-counter))


(deftest t116_l759 (is ((fn* [p1__85993#] (= p1__85993# 1)) v115_l757)))


(def
 v118_l765
 (defn
  simplified-layer-point
  [dataset options]
  (let
   [layer-defaults
    {:=dataset dataset,
     :=mark :point,
     :=x (get options :=x :x),
     :=y (get options :=y :y),
     :=x-data (dag/fn-with-deps nil [=dataset =x] (get =dataset =x)),
     :=y-data (dag/fn-with-deps nil [=dataset =y] (get =dataset =y)),
     :=trace
     (dag/fn-with-deps
      nil
      [=x-data =y-data =mark]
      {:type (name =mark), :x =x-data, :y =y-data})}]
   {:aerial.hanami.templates/defaults (merge layer-defaults options),
    :kindly/f (fn* [p1__85994#] (xform/xform p1__85994#))})))
