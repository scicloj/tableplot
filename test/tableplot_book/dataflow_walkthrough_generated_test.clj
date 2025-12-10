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


(def v3_l44 (xform/xform {:a :B, :c :D} {:B :C, :C 10, :D 20}))


(deftest
 t4_l51
 (is ((fn* [p1__49470#] (= p1__49470# {:a 10, :c 20})) v3_l44)))


(def
 v6_l59
 (xform/xform
  {:title :Title}
  {:Title "MyTitle", "MyTitle" "Resolved!"}))


(deftest
 t7_l64
 (is ((fn* [p1__49471#] (= p1__49471# {:title "Resolved!"})) v6_l59)))


(def
 v9_l68
 (xform/xform {:self-ref :X, :not-found :Y} {:X :X, :Y :Missing}))


(deftest
 t11_l74
 (is
  ((fn*
    [p1__49472#]
    (= p1__49472# {:self-ref :X, :not-found :Missing}))
   v9_l68)))


(def
 v13_l80
 (xform/xform
  {:data :Data, :nested {:deep {:data :Data}}}
  {:Data (tc/dataset {:x [1 2 3], :y [4 5 6]})}))


(deftest
 t14_l85
 (is
  ((fn*
    [p1__49473#]
    (and
     (= (-> p1__49473# :data tc/column-names) [:x :y])
     (= (-> p1__49473# :nested :deep :data tc/column-names) [:x :y])))
   v13_l80)))


(def
 v16_l92
 (xform/xform
  {:greeting :Greeting}
  {:Name "Alice",
   :Greeting (fn [{:keys [Name]}] (str "Hello, " Name "!"))}))


(deftest
 t17_l98
 (is
  ((fn* [p1__49474#] (= p1__49474# {:greeting "Hello, Alice!"}))
   v16_l92)))


(def
 v19_l104
 (xform/xform
  {:message :Message,
   :aerial.hanami.templates/defaults
   {:Name "World",
    :Message (fn [{:keys [Name]}] (str "Hello, " Name "!"))}}))


(deftest
 t20_l110
 (is
  ((fn* [p1__49475#] (= p1__49475# {:message "Hello, World!"}))
   v19_l104)))


(def
 v22_l114
 (xform/xform
  {:message :Message,
   :aerial.hanami.templates/defaults
   {:Name "World",
    :Message (fn [{:keys [Name]}] (str "Hello, " Name "!"))}}
  :Name
  "Clojure"))


(deftest
 t23_l121
 (is
  ((fn* [p1__49476#] (= p1__49476# {:message "Hello, Clojure!"}))
   v22_l114)))


(def
 v25_l127
 (xform/xform
  {:title :Title,
   :section
   {:heading :Heading,
    :aerial.hanami.templates/defaults {:Heading "Default Heading"}},
   :aerial.hanami.templates/defaults {:Title "Default Title"}}))


(deftest
 t26_l133
 (is
  ((fn*
    [p1__49477#]
    (=
     p1__49477#
     {:title "Default Title", :section {:heading "Default Heading"}}))
   v25_l127)))


(def
 v28_l138
 (xform/xform
  {:outer
   {:inner :InnerValue,
    :aerial.hanami.templates/defaults
    {:InnerValue
     (fn [{:keys [OuterValue]}] (str "Inner uses: " OuterValue))}},
   :aerial.hanami.templates/defaults {:OuterValue "Parent Value"}}))


(deftest
 t29_l144
 (is
  ((fn*
    [p1__49478#]
    (= p1__49478# {:outer {:inner "Inner uses: Parent Value"}}))
   v28_l138)))


(def
 v31_l148
 (xform/xform
  {:section
   {:heading :Heading,
    :aerial.hanami.templates/defaults {:Heading "Default Heading"}}}
  :Heading
  "User Heading"))


(deftest
 t32_l153
 (is
  ((fn*
    [p1__49479#]
    (= p1__49479# {:section {:heading "User Heading"}}))
   v31_l148)))


(def
 v34_l157
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
 t35_l169
 (is
  ((fn*
    [p1__49480#]
    (=
     p1__49480#
     {:config
      {:database
       {:url "postgresql://localhost:5432/mydb", :pool-size 50}}}))
   v34_l157)))


(def
 v37_l174
 (xform/xform
  {:user {:name :UserName, :age :UserAge}, :items [{:x :X} {:y :Y}]}
  {:UserName "Bob", :UserAge 30, :X 10, :Y 20}))


(deftest
 t38_l183
 (is
  ((fn*
    [p1__49481#]
    (=
     p1__49481#
     {:user {:name "Bob", :age 30}, :items [{:x 10} {:y 20}]}))
   v37_l174)))


(def v40_l190 (xform/xform {:outer {:middle {:inner []}}}))


(deftest t41_l193 (is ((fn* [p1__49482#] (= p1__49482# {})) v40_l190)))


(def
 v43_l197
 (xform/xform
  {:title :Title, :subtitle :Subtitle, :footer :Footer}
  {:Title "My Chart", :Subtitle hc/RMV, :Footer hc/RMV}))


(deftest
 t44_l205
 (is ((fn* [p1__49483#] (= p1__49483# {:title "My Chart"})) v43_l197)))


(def
 v46_l209
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
 t47_l247
 (is
  ((fn*
    [p1__49484#]
    (=
     p1__49484#
     {:plot
      {:data [{:x [1 2 3], :y [4 5 6], :type "scatter"}],
       :layout {:title {:text "Simple Chart"}}}}))
   v46_l209)))


(def
 v49_l256
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
 t50_l264
 (is
  ((fn*
    [p1__49485#]
    (= p1__49485# {:title "My Chart", :subtitle "A subtitle"}))
   v49_l256)))


(def
 v51_l266
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
 t52_l275
 (is ((fn* [p1__49486#] (= p1__49486# {:title "My Chart"})) v51_l266)))


(def
 v54_l285
 (xform/xform
  {:b :B,
   :c :C,
   :aerial.hanami.templates/defaults
   {:A 10,
    :B (dag/fn-with-deps "B depends on A" [A] (* A 2)),
    :C (dag/fn-with-deps "C depends on B" [B] (+ B 5))}}))


(deftest
 t55_l297
 (is ((fn* [p1__49487#] (= p1__49487# {:b 20, :c 25})) v54_l285)))


(def
 v57_l303
 (xform/xform
  {:result :E,
   :aerial.hanami.templates/defaults
   {:A 5,
    :B (dag/fn-with-deps nil [A] (* A 2)),
    :C (dag/fn-with-deps nil [A] (+ A 3)),
    :D (dag/fn-with-deps nil [B C] (+ B C)),
    :E (dag/fn-with-deps nil [D] (* D 10))}}))


(deftest
 t58_l312
 (is ((fn* [p1__49488#] (= p1__49488# {:result 180})) v57_l303)))


(def
 v60_l318
 (dag/defn-with-deps
  area->radius
  "Compute radius from area"
  [Area]
  (Math/sqrt (/ Area Math/PI))))


(def
 v61_l323
 (dag/defn-with-deps
  radius->circumference
  "Compute circumference from radius"
  [Radius]
  (* 2 Math/PI Radius)))


(def
 v62_l328
 (xform/xform
  {:circumference :Circumference,
   :aerial.hanami.templates/defaults
   {:Area 100,
    :Radius area->radius,
    :Circumference radius->circumference}}))


(deftest
 t63_l335
 (is
  ((fn*
    [p1__49489#]
    (let
     [expected (* 2 Math/PI (Math/sqrt (/ 100 Math/PI)))]
     (< (Math/abs (- (:circumference p1__49489#) expected)) 0.001)))
   v62_l328)))


(def v65_l340 (:scicloj.tableplot.v1.dag/dep-ks (meta area->radius)))


(deftest
 t66_l342
 (is ((fn* [p1__49490#] (= p1__49490# [:Area])) v65_l340)))


(def
 v67_l344
 (:scicloj.tableplot.v1.dag/dep-ks (meta radius->circumference)))


(deftest
 t68_l346
 (is ((fn* [p1__49491#] (= p1__49491# [:Radius])) v67_l344)))


(def
 v69_l348
 (def
  complex-fn
  (dag/fn-with-deps "Complex computation" [A B C] (+ A B C))))


(def v70_l353 (:scicloj.tableplot.v1.dag/dep-ks (meta complex-fn)))


(deftest
 t71_l355
 (is ((fn* [p1__49492#] (= p1__49492# [:A :B :C])) v70_l353)))


(def
 v73_l361
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


(deftest t74_l372 (is ((fn* [p1__49493#] (= p1__49493# 1)) v73_l361)))


(def v76_l376 (def computation-log (atom [])))


(def v77_l378 (reset! computation-log []))


(def
 v78_l380
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
 t79_l396
 (is
  ((fn*
    [p1__49494#]
    (= p1__49494# [:computing-A :computing-B :computing-C]))
   v78_l380)))


(def v81_l400 (def detailed-log (atom [])))


(def v82_l402 (reset! detailed-log []))


(def
 v83_l404
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
 t84_l423
 (is
  ((fn*
    [p1__49495#]
    (=
     p1__49495#
     [[:computing :A]
      [:computing :B :with-A 100]
      [:computing :C :with-A 100]
      [:computing :D :with-B 200 :with-C 150]]))
   v83_l404)))


(def
 v86_l432
 (cache/with-clean-cache
  [(dag/cached-xform-k
    :Result
    {:X 5, :Result (fn [{:keys [X]}] (* X X))})
   (dag/cached-xform-k
    :Result
    {:X 7, :Result (fn [{:keys [X]}] (* X X))})]))


(deftest
 t87_l436
 (is ((fn* [p1__49496#] (= p1__49496# [25 49])) v86_l432)))


(def
 v89_l442
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
       (fn* [p1__49497#] (> (:y p1__49497#) MinValue)))),
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
       (fn* [p1__49498#] (* p1__49498# ScaleFactor)))),
     :ScaleFactor 2,
     :GridColor
     (dag/fn-with-deps
      nil
      [Environment]
      (if (= Environment "dark-mode") "#444444" hc/RMV))}},
   :aerial.hanami.templates/defaults
   {:TitleFontSize hc/RMV, :TitleColor hc/RMV, :GridColor hc/RMV}}))


(def v90_l482 (cache/with-clean-cache (xform/xform viz-pipeline)))


(deftest
 t91_l486
 (is
  ((fn*
    [p1__49499#]
    (and
     (=
      (get-in p1__49499# [:visualization :layout :title :text])
      "Filtered & Scaled Data (threshold=12, scale=2)")
     (=
      (get-in p1__49499# [:visualization :layout :xaxis :title])
      "X Values (n=5)")
     (=
      (get-in p1__49499# [:visualization :layout :yaxis :title])
      "Y Values (scaled ×2)")
     (= (tc/row-count (get-in p1__49499# [:visualization :data])) 4)))
   v90_l482)))


(def
 v92_l495
 (cache/with-clean-cache
  (xform/xform viz-pipeline :Environment "presentation" :MinValue 14)))


(deftest
 t93_l501
 (is
  ((fn*
    [p1__49500#]
    (and
     (=
      (get-in p1__49500# [:visualization :layout :title :text])
      "Filtered & Scaled Data (threshold=14, scale=2)")
     (=
      (get-in p1__49500# [:visualization :layout :title :font :size])
      24)
     (= (tc/row-count (get-in p1__49500# [:visualization :data])) 3)))
   v92_l495)))


(def
 v95_l510
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
 v96_l529
 (xform/xform
  {:db-url :DatabaseURL,
   :log-level :LogLevel,
   :max-conn :MaxConnections}
  app-config))


(deftest
 t97_l535
 (is
  ((fn*
    [p1__49501#]
    (=
     p1__49501#
     {:db-url "postgresql://db.example.com:5432/myapp",
      :log-level "WARN",
      :max-conn 50}))
   v96_l529)))


(def
 v98_l539
 (xform/xform
  {:db-url :DatabaseURL,
   :log-level :LogLevel,
   :max-conn :MaxConnections}
  (assoc app-config :Environment "development")))


(deftest
 t99_l545
 (is
  ((fn*
    [p1__49502#]
    (=
     p1__49502#
     {:db-url "postgresql://db.example.com:5432/myapp",
      :log-level "DEBUG",
      :max-conn 10}))
   v98_l539)))


(def v101_l551 (def lazy-counter (atom 0)))


(def v102_l553 (reset! lazy-counter 0))


(def
 v103_l555
 (xform/xform
  {:needed :A,
   :aerial.hanami.templates/defaults
   {:A (fn [_] (swap! lazy-counter inc) "value-a"),
    :B (fn [_] (swap! lazy-counter inc) "value-b")}}))


(def v104_l565 (deref lazy-counter))


(deftest t105_l567 (is ((fn* [p1__49503#] (= p1__49503# 1)) v104_l565)))


(def
 v107_l573
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
   :kindly/f (fn* [p1__49504#] (xform/xform p1__49504#))}))
