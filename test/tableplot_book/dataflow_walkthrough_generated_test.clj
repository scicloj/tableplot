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


(def v3_l46 (xform/xform {:a :B, :c :D} {:B :C, :C 10, :D 20}))


(deftest
 t4_l53
 (is ((fn* [p1__91032#] (= p1__91032# {:a 10, :c 20})) v3_l46)))


(def
 v6_l61
 (xform/xform
  {:title :Title}
  {:Title "MyTitle", "MyTitle" "Resolved!"}))


(deftest
 t7_l66
 (is ((fn* [p1__91033#] (= p1__91033# {:title "Resolved!"})) v6_l61)))


(def
 v9_l70
 (xform/xform {:self-ref :X, :not-found :Y} {:X :X, :Y :Missing}))


(deftest
 t11_l76
 (is
  ((fn*
    [p1__91034#]
    (= p1__91034# {:self-ref :X, :not-found :Missing}))
   v9_l70)))


(def
 v13_l82
 (xform/xform
  {:data :Data, :nested {:deep {:data :Data}}}
  {:Data (tc/dataset {:x [1 2 3], :y [4 5 6]})}))


(deftest
 t14_l87
 (is
  ((fn*
    [p1__91035#]
    (and
     (= (-> p1__91035# :data tc/column-names) [:x :y])
     (= (-> p1__91035# :nested :deep :data tc/column-names) [:x :y])))
   v13_l82)))


(def
 v16_l94
 (xform/xform
  {:greeting :Greeting}
  {:Name "Alice",
   :Greeting (fn [{:keys [Name]}] (str "Hello, " Name "!"))}))


(deftest
 t17_l100
 (is
  ((fn* [p1__91036#] (= p1__91036# {:greeting "Hello, Alice!"}))
   v16_l94)))


(def
 v19_l106
 (xform/xform
  {:message :Message,
   :aerial.hanami.templates/defaults
   {:Name "World",
    :Message (fn [{:keys [Name]}] (str "Hello, " Name "!"))}}))


(deftest
 t20_l112
 (is
  ((fn* [p1__91037#] (= p1__91037# {:message "Hello, World!"}))
   v19_l106)))


(def
 v22_l116
 (xform/xform
  {:message :Message,
   :aerial.hanami.templates/defaults
   {:Name "World",
    :Message (fn [{:keys [Name]}] (str "Hello, " Name "!"))}}
  :Name
  "Clojure"))


(deftest
 t23_l123
 (is
  ((fn* [p1__91038#] (= p1__91038# {:message "Hello, Clojure!"}))
   v22_l116)))


(def
 v25_l129
 (xform/xform
  {:title :Title,
   :section
   {:heading :Heading,
    :aerial.hanami.templates/defaults {:Heading "Default Heading"}},
   :aerial.hanami.templates/defaults {:Title "Default Title"}}))


(deftest
 t26_l135
 (is
  ((fn*
    [p1__91039#]
    (=
     p1__91039#
     {:title "Default Title", :section {:heading "Default Heading"}}))
   v25_l129)))


(def
 v28_l140
 (xform/xform
  {:outer
   {:inner :InnerValue,
    :aerial.hanami.templates/defaults
    {:InnerValue
     (fn [{:keys [OuterValue]}] (str "Inner uses: " OuterValue))}},
   :aerial.hanami.templates/defaults {:OuterValue "Parent Value"}}))


(deftest
 t29_l146
 (is
  ((fn*
    [p1__91040#]
    (= p1__91040# {:outer {:inner "Inner uses: Parent Value"}}))
   v28_l140)))


(def
 v31_l150
 (xform/xform
  {:section
   {:heading :Heading,
    :aerial.hanami.templates/defaults {:Heading "Default Heading"}}}
  :Heading
  "User Heading"))


(deftest
 t32_l155
 (is
  ((fn*
    [p1__91041#]
    (= p1__91041# {:section {:heading "User Heading"}}))
   v31_l150)))


(def
 v34_l159
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
 t35_l171
 (is
  ((fn*
    [p1__91042#]
    (=
     p1__91042#
     {:config
      {:database
       {:url "postgresql://localhost:5432/mydb", :pool-size 50}}}))
   v34_l159)))


(def
 v37_l176
 (xform/xform
  {:user {:name :UserName, :age :UserAge}, :items [{:x :X} {:y :Y}]}
  {:UserName "Bob", :UserAge 30, :X 10, :Y 20}))


(deftest
 t38_l185
 (is
  ((fn*
    [p1__91043#]
    (=
     p1__91043#
     {:user {:name "Bob", :age 30}, :items [{:x 10} {:y 20}]}))
   v37_l176)))


(def v40_l192 (xform/xform {:outer {:middle {:inner []}}}))


(deftest t41_l195 (is ((fn* [p1__91044#] (= p1__91044# {})) v40_l192)))


(def
 v43_l199
 (xform/xform
  {:title :Title, :subtitle :Subtitle, :footer :Footer}
  {:Title "My Chart", :Subtitle hc/RMV, :Footer hc/RMV}))


(deftest
 t44_l207
 (is ((fn* [p1__91045#] (= p1__91045# {:title "My Chart"})) v43_l199)))


(def
 v46_l211
 (xform/xform
  {:title :Title, :subtitle :Subtitle, :footer :Footer}
  {:Title "My Chart", :Subtitle nil, :Footer nil}))


(deftest
 t47_l219
 (is ((fn* [p1__91046#] (= p1__91046# {:title "My Chart"})) v46_l211)))


(def
 v49_l223
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
 t50_l261
 (is
  ((fn*
    [p1__91047#]
    (=
     p1__91047#
     {:plot
      {:data [{:x [1 2 3], :y [4 5 6], :type "scatter"}],
       :layout {:title {:text "Simple Chart"}}}}))
   v49_l223)))


(def
 v52_l270
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
 t53_l278
 (is
  ((fn*
    [p1__91048#]
    (= p1__91048# {:title "My Chart", :subtitle "A subtitle"}))
   v52_l270)))


(def
 v54_l280
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
 t55_l289
 (is ((fn* [p1__91049#] (= p1__91049# {:title "My Chart"})) v54_l280)))


(def
 v57_l299
 (xform/xform
  {:b :B,
   :c :C,
   :aerial.hanami.templates/defaults
   {:A 10,
    :B (dag/fn-with-deps "B depends on A" [A] (* A 2)),
    :C (dag/fn-with-deps "C depends on B" [B] (+ B 5))}}))


(deftest
 t58_l311
 (is ((fn* [p1__91050#] (= p1__91050# {:b 20, :c 25})) v57_l299)))


(def
 v60_l317
 (xform/xform
  {:result :E,
   :aerial.hanami.templates/defaults
   {:A 5,
    :B (dag/fn-with-deps nil [A] (* A 2)),
    :C (dag/fn-with-deps nil [A] (+ A 3)),
    :D (dag/fn-with-deps nil [B C] (+ B C)),
    :E (dag/fn-with-deps nil [D] (* D 10))}}))


(deftest
 t61_l326
 (is ((fn* [p1__91051#] (= p1__91051# {:result 180})) v60_l317)))


(def
 v63_l332
 (dag/defn-with-deps
  area->radius
  "Compute radius from area"
  [Area]
  (Math/sqrt (/ Area Math/PI))))


(def
 v64_l337
 (dag/defn-with-deps
  radius->circumference
  "Compute circumference from radius"
  [Radius]
  (* 2 Math/PI Radius)))


(def
 v65_l342
 (xform/xform
  {:circumference :Circumference,
   :aerial.hanami.templates/defaults
   {:Area 100,
    :Radius area->radius,
    :Circumference radius->circumference}}))


(deftest
 t66_l349
 (is
  ((fn*
    [p1__91052#]
    (let
     [expected (* 2 Math/PI (Math/sqrt (/ 100 Math/PI)))]
     (< (Math/abs (- (:circumference p1__91052#) expected)) 0.001)))
   v65_l342)))


(def v68_l354 (:scicloj.tableplot.v1.dag/dep-ks (meta area->radius)))


(deftest
 t69_l356
 (is ((fn* [p1__91053#] (= p1__91053# [:Area])) v68_l354)))


(def
 v70_l358
 (:scicloj.tableplot.v1.dag/dep-ks (meta radius->circumference)))


(deftest
 t71_l360
 (is ((fn* [p1__91054#] (= p1__91054# [:Radius])) v70_l358)))


(def
 v72_l362
 (def
  complex-fn
  (dag/fn-with-deps "Complex computation" [A B C] (+ A B C))))


(def v73_l367 (:scicloj.tableplot.v1.dag/dep-ks (meta complex-fn)))


(deftest
 t74_l369
 (is ((fn* [p1__91055#] (= p1__91055# [:A :B :C])) v73_l367)))


(def
 v76_l375
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


(deftest t77_l386 (is ((fn* [p1__91056#] (= p1__91056# 1)) v76_l375)))


(def v79_l390 (def computation-log (atom [])))


(def v80_l392 (reset! computation-log []))


(def
 v81_l394
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
 t82_l410
 (is
  ((fn*
    [p1__91057#]
    (= p1__91057# [:computing-A :computing-B :computing-C]))
   v81_l394)))


(def v84_l414 (def detailed-log (atom [])))


(def v85_l416 (reset! detailed-log []))


(def
 v86_l418
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
 t87_l437
 (is
  ((fn*
    [p1__91058#]
    (=
     p1__91058#
     [[:computing :A]
      [:computing :B :with-A 100]
      [:computing :C :with-A 100]
      [:computing :D :with-B 200 :with-C 150]]))
   v86_l418)))


(def
 v89_l446
 (cache/with-clean-cache
  [(dag/cached-xform-k
    :Result
    {:X 5, :Result (fn [{:keys [X]}] (* X X))})
   (dag/cached-xform-k
    :Result
    {:X 7, :Result (fn [{:keys [X]}] (* X X))})]))


(deftest
 t90_l450
 (is ((fn* [p1__91059#] (= p1__91059# [25 49])) v89_l446)))


(def
 v92_l456
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
       (fn* [p1__91060#] (> (:y p1__91060#) MinValue)))),
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
       (fn* [p1__91061#] (* p1__91061# ScaleFactor)))),
     :ScaleFactor 2,
     :GridColor
     (dag/fn-with-deps
      nil
      [Environment]
      (if (= Environment "dark-mode") "#444444" hc/RMV))}},
   :aerial.hanami.templates/defaults
   {:TitleFontSize hc/RMV, :TitleColor hc/RMV, :GridColor hc/RMV}}))


(def v93_l496 (cache/with-clean-cache (xform/xform viz-pipeline)))


(deftest
 t94_l500
 (is
  ((fn*
    [p1__91062#]
    (and
     (=
      (get-in p1__91062# [:visualization :layout :title :text])
      "Filtered & Scaled Data (threshold=12, scale=2)")
     (=
      (get-in p1__91062# [:visualization :layout :xaxis :title])
      "X Values (n=5)")
     (=
      (get-in p1__91062# [:visualization :layout :yaxis :title])
      "Y Values (scaled ×2)")
     (= (tc/row-count (get-in p1__91062# [:visualization :data])) 4)))
   v93_l496)))


(def
 v95_l509
 (cache/with-clean-cache
  (xform/xform viz-pipeline :Environment "presentation" :MinValue 14)))


(deftest
 t96_l515
 (is
  ((fn*
    [p1__91063#]
    (and
     (=
      (get-in p1__91063# [:visualization :layout :title :text])
      "Filtered & Scaled Data (threshold=14, scale=2)")
     (=
      (get-in p1__91063# [:visualization :layout :title :font :size])
      24)
     (= (tc/row-count (get-in p1__91063# [:visualization :data])) 3)))
   v95_l509)))


(def
 v98_l524
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
 v99_l543
 (xform/xform
  {:db-url :DatabaseURL,
   :log-level :LogLevel,
   :max-conn :MaxConnections}
  app-config))


(deftest
 t100_l549
 (is
  ((fn*
    [p1__91064#]
    (=
     p1__91064#
     {:db-url "postgresql://db.example.com:5432/myapp",
      :log-level "WARN",
      :max-conn 50}))
   v99_l543)))


(def
 v101_l553
 (xform/xform
  {:db-url :DatabaseURL,
   :log-level :LogLevel,
   :max-conn :MaxConnections}
  (assoc app-config :Environment "development")))


(deftest
 t102_l559
 (is
  ((fn*
    [p1__91065#]
    (=
     p1__91065#
     {:db-url "postgresql://db.example.com:5432/myapp",
      :log-level "DEBUG",
      :max-conn 10}))
   v101_l553)))


(def v104_l565 (def lazy-counter (atom 0)))


(def v105_l567 (reset! lazy-counter 0))


(def
 v106_l569
 (xform/xform
  {:needed :A,
   :aerial.hanami.templates/defaults
   {:A (fn [_] (swap! lazy-counter inc) "value-a"),
    :B (fn [_] (swap! lazy-counter inc) "value-b")}}))


(def v107_l579 (deref lazy-counter))


(deftest t108_l581 (is ((fn* [p1__91066#] (= p1__91066# 1)) v107_l579)))


(def
 v110_l587
 (defn
  simplified-layer-point
  [dataset options]
  {:aerial.hanami.templates/defaults
   (merge
    {:=dataset dataset,
     :=mark :point,
     :=x-data (dag/fn-with-deps nil [=dataset =x] (get =dataset =x)),
     :=y-data (dag/fn-with-deps nil [=dataset =y] (get =dataset =y)),
     :=trace
     (dag/fn-with-deps
      nil
      [=x-data =y-data =mark]
      {:type (name =mark), :x =x-data, :y =y-data})}
    options),
   :kindly/f (fn* [p1__91067#] (xform/xform p1__91067#))}))
