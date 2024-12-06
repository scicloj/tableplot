(ns scicloj.tableplot.v1.plotly
  (:require [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [tech.v3.dataset :as ds]
            [tech.v3.dataset.modelling :as dsmod]
            [fastmath.stats]
            [fastmath.kernel]
            [scicloj.metamorph.ml :as ml]
            [scicloj.metamorph.ml.regression]
            [scicloj.metamorph.ml.design-matrix :as design-matrix]
            [scicloj.tableplot.v1.dag :as dag]
            [clojure.string :as str]
            [scicloj.tableplot.v1.util :as util]
            [scicloj.tableplot.v1.cache :as cache]
            [scicloj.tableplot.v1.xform :as xform]))

(def submap->dataset-after-stat
  (dag/fn-with-deps-keys
   "If the statistical transformation `:=stat` is specified,
apply it to the whole context.
Otherwise, keep the original `:=dataset`."
   [:=dataset :=stat]
   (fn [{:as submap
         :keys [=dataset =stat]}]
     (when-not (tc/dataset? =dataset)
       (throw (ex-info "missing :=dataset"
                       submap)))
     (if =stat
       (@=stat submap)
       =dataset))))

(defn submap->field-type [colname-key]
  (let [dataset-key :=dataset]
    (dag/fn-with-deps-keys ""
                           [colname-key dataset-key]
                           (fn [submap]
                             (if-let [colname (submap colname-key)]
                               (let [column (-> submap
                                                (get dataset-key)
                                                (get colname))]
                                 (cond (tcc/typeof? column :numerical) :quantitative
                                       (tcc/typeof? column :datetime) :temporal
                                       :else :nominal))
                               hc/RMV)))))

(defn submap->field-type-after-stat [colname-key]
  (let [dataset-key :=dataset-after-stat
        colname-key-before-stat (-> colname-key
                                    name
                                    (str/replace #"-after-stat" "")
                                    keyword)
        colname-key-type-before-stat (-> colname-key-before-stat
                                         name
                                         (str "-type")
                                         keyword)]
    (dag/fn-with-deps-keys ""
                           [colname-key
                            colname-key-before-stat
                            colname-key-type-before-stat
                            dataset-key]
                           (fn [submap]
                             (if-let [colname (submap colname-key)]
                               (let [column (-> submap
                                                (get dataset-key)
                                                (get colname))
                                     colname-before-stat (submap
                                                          colname-key-before-stat)]
                                 (or (when (= colname colname-before-stat)
                                       (submap colname-key-type-before-stat))
                                     (cond (tcc/typeof? column :numerical) :quantitative
                                           (tcc/typeof? column :datetime) :temporal
                                           :else :nominal)))
                               hc/RMV)))))


(defn select-column [dataset column-selector]
  (-> dataset
      (tc/select-columns column-selector)
      vals
      first))


(defn submap->data [column-selector-key]
  (dag/fn-with-deps-keys ""
                         [column-selector-key :=dataset]
                         (fn [submap]
                           (if-let [column-selector (submap
                                                     column-selector-key)]
                             (do (-> submap
                                     (get :=dataset))
                                 (or (-> submap
                                         (get :=dataset)
                                         (select-column column-selector)
                                         vec)
                                     hc/RMV))))))


(dag/defn-with-deps submap->group ""
  [=color =color-type =size =size-type]
  (concat (when (= =color-type :nominal)
            [=color])
          (when (= =size-type :nominal)
            [=size])))

(defn mark->mode [mark]
  (case mark
    :point :markers
    :text :text
    :line :lines
    :box nil
    :bar nil
    :segment :lines))

(dag/defn-with-deps submap->mode ""
  [=mark]
  (mark->mode =mark))

(dag/defn-with-deps submap->type ""
  [=mark =coordinates]
  (str (case =mark
         :box "box"
         :bar "bar"
         ;; else
         "scatter")
       (case =coordinates
         :2d nil
         :3d "3d"
         :polar "polar"
         :geo "geo")))


(def colors-palette
  ;; In R:
  ;; library(RColorBrewer)
  ;; brewer.pal(n = 8, name = "Dark2")
  ["#1B9E77" "#D95F02" "#7570B3" "#E7298A" "#66A61E" "#E6AB02" "#A6761D"
   "#666666"])

(def sizes-palette
  (->> 5
       (iterate (partial * 1.4))
       (take 8)
       (mapv int)))

(def view-base
  {:data :=traces
   :layout :=layout})

(dag/defn-with-deps submap->marker-size-key ""
  [=mode =type]
  (if (or (= =mode :lines)
          (= =type :line)) :width
      :size))

(def layer-base
  {:dataset :=dataset-after-stat
   :mark :=mark
   :x :=x-after-stat
   :y :=y-after-stat
   :z :=z-after-stat
   :x0 :=x0-after-stat
   :y0 :=y0-after-stat
   :x1 :=x1-after-stat
   :y1 :=y1-after-stat
   :bar-width :=bar-width
   :r :=r
   :theta :=theta
   :lat :=lat
   :lon :=lon
   :coordinates :=coordinates
   :x-title :=x-title
   :y-title :=y-title
   :color :=color
   :color-type :=color-type
   :size :=size
   :size-type :=size-type
   :text :=text
   :inferred-group :=inferred-group
   :group :=group :marker-override {:color :=mark-color
                                    :=marker-size-key :=mark-size}
   :fill :=mark-fill
   :trace-base {:mode :=mode
                :type :=type
                :opacity :=mark-opacity
                :textfont :=textfont}
   :name :=name})


(dag/defn-with-deps submap->traces ""
  [=layers]
  (->>
   =layers
   (mapcat
    (fn [{:as layer
          :keys [dataset
                 mark
                 x y z
                 x0 y0 x1 y1
                 bar-width
                 r theta
                 lat lon
                 coordinates
                 color color-type
                 size size-type
                 text
                 marker-override
                 fill
                 inferred-group
                 trace-base]}]
      (let [group-kvs (if inferred-group
                        (-> dataset
                            (tc/group-by inferred-group {:result-type :as-map}))
                        {nil dataset})]
        (-> group-kvs
            (->> (map
                  (fn [[group-key group-dataset]]
                    (let [marker (merge
                                  (when color
                                    (case color-type
                                      :nominal {:color (cache/cached-assignment (color group-key)
                                                                                colors-palette
                                                                                ::color)}
                                      :quantitative {:color (-> group-dataset color vec)}))
                                  (when size
                                    (case size-type
                                      :nominal {:size (cache/cached-assignment (size group-key)
                                                                               sizes-palette
                                                                               ::size)}
                                      :quantitative {:size (-> group-dataset size vec)}))
                                  marker-override)]
                      (merge trace-base
                             {:name (->> [(:name layer)
                                          (some->> group-key
                                                   vals
                                                   (str/join " "))]
                                         (remove nil?)
                                         (str/join " "))}
                             {:fill fill}
                             {:r (some-> r group-dataset vec)
                              :theta (some-> theta group-dataset vec)}
                             {:lat (some-> lat group-dataset vec)
                              :lon (some-> lon group-dataset vec)}
                             {:text (some-> text group-dataset vec)}
                             {:z (some-> z group-dataset vec)}
                             {:width (some-> bar-width group-dataset vec)}
                             ;; else
                             (if (= mark :segment)
                               {:x (vec
                                    (interleave (group-dataset x0)
                                                (group-dataset x1)
                                                (repeat nil)))
                                :y (vec
                                    (interleave (group-dataset y0)
                                                (group-dataset y1)
                                                (repeat nil)))}
                               ;; else
                               {:x (-> x group-dataset vec)
                                :y (-> y group-dataset vec)})
                             (when marker
                               (let [marker-key (if (or (-> trace-base :mode (= :lines))
                                                        (-> trace-base :type (= :line)))
                                                  :line
                                                  :marker)]
                                 {marker-key marker})))))))))))
   vec))


(dag/defn-with-deps submap->layout ""
  [=width =height =margin =automargin =background =title
   =xaxis-gridcolor =yaxis-gridcolor
   =x-after-stat =y-after-stat
   =x-title =y-title
   =layers]
  (let [final-x-title (or (->> =layers
                               (map :x-title)
                               (cons =x-title)
                               (remove nil?)
                               last)
                          (->> =layers
                               (map :x)
                               (cons =x-after-stat)
                               (remove nil?)
                               last))
        final-y-title (or (->> =layers
                               (map :y-title)
                               (cons =y-title)
                               (remove nil?)
                               last)
                          (->> =layers
                               (map :y)
                               (cons =y-after-stat)
                               (remove nil?)
                               last))]
    {:width =width
     :height =height
     :margin =margin
     :automargin =automargin
     :plot_bgcolor =background
     :xaxis {:gridcolor =xaxis-gridcolor
             :title final-x-title}
     :yaxis {:gridcolor =yaxis-gridcolor
             :title final-y-title}
     :title =title}))

(dag/defn-with-deps submap->design-matrix ""
  [=predictors]
  (->> =predictors
       (mapv (fn [k]
               [k (list
                   'identity
                   (-> k name symbol))]))))

(def standard-defaults
  [[:=stat hc/RMV
    "A user-defined or layer-specific statistical transformation.
Received the whole context and returns a new dataset."]
   [:=dataset hc/RMV
    "The data to be plotted."]
   [:=dataset-after-stat submap->dataset-after-stat
    "The data after a possible statistical transformation."]
   [:=x :x
    "The column for the x axis."]
   [:=x-after-stat :=x
    "The column for the x axis to be used after `:=stat`."]
   [:=y :y
    "The column for the y axis."]
   [:=y-after-stat :=y
    "The column for the y axis to be used after `:=stat`."]
   [:=z hc/RMV
    "The column for the z axis."]
   [:=z-after-stat :=z
    "The column for the z axis to be used after `:=stat`."]
   [:=x0 hc/RMV
    "The column for the first x axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=x0-after-stat :=x0
    "The column for the first x axis value after stat, in cases where pairs are needed, e.g. segment layers."]
   [:=y0 hc/RMV
    "The column for the first y axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=y0-after-stat :=y0
    "The column for the first y axis value after stat, in cases where pairs are needed, e.g. segment layers."]
   [:=x1 hc/RMV
    "The column for the second x axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=x1-after-stat :=x1
    "The column for the second x axis value after stat, in cases where pairs are needed, e.g. segment layers."]
   [:=y1 hc/RMV
    "The column for the second y axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=y1-after-stat :=y1
    "The column for the second y axis value after stat, in cases where pairs are needed, e.g. segment layers."]
   [:=bar-width hc/RMV
    "The column to determine the bar width in bar layers."]
   [:=color hc/RMV
    "The column to determine the color of marks."]
   [:=size hc/RMV
    "The column to determine the size of marks."]
   [:=x-type (submap->field-type :=x)]
   [:=x-type-after-stat (submap->field-type-after-stat :=x-after-stat)]
   [:=y-type (submap->field-type :=y)]
   [:=y-type-after-stat (submap->field-type-after-stat :=y-after-stat)]
   [:=z-type (submap->field-type :=z)]
   [:=z-type-after-stat (submap->field-type-after-stat :=z-after-stat)]
   [:=r hc/RMV]
   [:=theta hc/RMV]
   [:=lat hc/RMV]
   [:=lon hc/RMV]
   [:=color-type (submap->field-type :=color)
    "The data type of the column used to determine mark color."]
   [:=size-type (submap->field-type :=size)
    "The data type of the column used to determine mark size"]
   [:=mark-color hc/RMV
    "A fixed color specification for marks."]
   [:=mark-size hc/RMV
    "A fixed size specification for marks."]
   [:=marker-size-key submap->marker-size-key
    "What key does Plotly.js use to hold the marker size?"]
   [:=mark-fill hc/RMV
    "A fixed fill specification for marks."]
   [:=mark-opacity hc/RMV
    "A fixed opacity specification for marks."]
   [:=text hc/RMV
    "The column to determine the text of marks (relevant for text layer)."]
   [:=textfont hc/RMV
    "Text font specification as defined in Plotly.js. See [Text and oFnt Styling](https://plotly.com/javascript/font/)."]
   [:=mark :point
    "The mark used for a layer (a Tablepot concept)."]
   [:=mode submap->mode
    "The Plotly.js mode used in a trace."]
   [:=type submap->type
    "The Plotly.js type used in a trace."]
   [:=name hc/RMV
    "The layer name (which affects the Plotly.js traces names)."]
   [:=layers []
    "A vector of all lyaers in the plot (an inermediate Tableplot representation before converting to Plotly.js traces)."]
   [:=traces submap->traces
    "A vector of all Plotly.js traces in the plot."]
   [:=layout submap->layout
    "The layout part of the resulting Plotly.js specification."]
   [:=inferred-group submap->group
    "The grouping of columns, that can be inferred from other keys and data (e.g., `:=color`) and can influence statistical layers such as `layer-smooth`."]
   [:=group :=inferred-group
    "A possible user override of `:=inerred-group`."]
   [:=predictors [:=x]
    "The list of predictors to be used in regression (`layer-smooth`)."]
   [:=design-matrix submap->design-matrix
    "The design matrix definition to be used in regression (`layer-smooth`)."]
   [:=model-options {:model-type :fastmath/ols}
    "The optional specification of a model for regression (`layer-smooth`)."]
   [:=histogram-nbins 10
    "The number of bins for `layer-histogram`."]
   [:=density-bandwidth hc/RMV
    "The bandwidth of density estimation for `layer-density`."]
   [:=coordinates :2d
    "The coordinates to use: `:2d`/`:3d`/`:polar`/`:geo`."]
   [:=height 400
    "The plot's height."]
   [:=width 500
    "The plot's width."]
   [:=margin {:t 25}
    "Plotly.js margin specification. See [Setting Graph Size in Javaspcrit](https://plotly.com/javascript/setting-graph-size/)."]
   [:=automargin false
    "Should Plotly.js margins be automatically adjusted? See [Setting Graph Size in Javaspcrit](https://plotly.com/javascript/setting-graph-size/)."]
   [:=x-title hc/RMV
    "The title for x axis."]
   [:=y-title hc/RMV
    "The title for y axis."]
   [:=title hc/RMV
    "The plot title."]
   [:=background "rgb(235,235,235)"
    "The plot background color."]
   [:=xaxis-gridcolor "rgb(255,255,255)"
    "The color for the x axis grid lines."]
   [:=yaxis-gridcolor "rgb(255,255,255)"
    "The color for the y axis grid lines."]])

(def standard-defaults-map
  (->> standard-defaults
       (map (comp vec (partial take 2)))
       (into {})))

(defn plotly-xform [template]
  (cache/with-clean-cache
    (-> template
        xform/xform
        (kind/plotly {:style {:height :auto}})
        (dissoc :kindly/f))))

(defn base
  "  The `base` function can be used to create the basis template too which we can add layers.
  It can be used to set up some substitution keys to be shared by the various layers.

  The return value is always a template which is set up to be visualized as plotly.
  
  In the full case of three arguments `(dataset template submap)`,
  `dataset` is added to `template` as the value substituted for the 
  `:=dataset`key, and the substitution map `submap` is added as well.

  In the other cases, if the `template` is not passed missing, it is replaced by a minimal base
  template to be carried along the pipeline. If the `dataset` or `submap` parts are not passed,
  they are simply not substituted into the template.

  If the first argument is a dataset, it is converted to a very basic template

  We typically use `base` with other layers added to it.
  The base substitutions are shared between layers,
  and the layers can override them and add substitutions of their own.
  "  
  ;;
  ([dataset-or-template]
   (base dataset-or-template {}))
  ;;
  ([dataset-or-template submap]
   (if (tc/dataset? dataset-or-template)
     ;; a dataest
     (base dataset-or-template
           view-base
           submap)
     ;; a template
     (-> dataset-or-template
         (update ::ht/defaults merge submap)
         (assoc :kindly/f #'plotly-xform)
         kind/fn)))
  ;;
  ([dataset template submap]
   (-> template
       (update ::ht/defaults merge
               standard-defaults-map
               {:=dataset dataset})
       (base submap))))


(defn plot [template]
  "The `plot` function realizes a template as a Plotly.js specification."
  (plotly-xform template))

(defn layer
  "The `layer` function is typically not used on the user side.
  It is a generic way to create more specific functions to add layers.
  such as `layer-point`.

  If `dataset-or-template` is a dataset, it is converted to
  a basic template where it is substituted at the
  `:=dataset` key.

  Otherwise, it is already template and can be processed further.
  The `layer-template` template is added as an additional layer
  to our template.
  The `submap` substitution map is added as additional substitutions
  to that layer.

  The var `layer-base` is typicall used as the `layer-template`.
  "
  ([dataset-or-template layer-template submap]
   (if (tc/dataset? dataset-or-template)
     (layer (base dataset-or-template {})
            layer-template
            submap)
     ;; else - the dataset-or-template is already a template
     (-> dataset-or-template
         (update ::ht/defaults
                 (fn [defaults]
                   (-> defaults
                       (update :=layers
                               util/conjv
                               (assoc layer-template
                                      ::ht/defaults (merge
                                                     standard-defaults-map
                                                     defaults
                                                     submap))))))))))

(defmacro def-mark-based-layer
  "  This macro is typically not used on the user side.
  It is used to generate more specific functions to add specific types of layers.

  It creates a function definition of two possible arities:

  `[dataset-or-template]`

  `[dataset-or-template submap]`

  the returned function can be used to process a dataset or a template in a pipeline
  by adding a layer of a specificed kind and possibly some substutution maps.
  "
  [fsymbol mark description]
  (list 'defn fsymbol
        (format
         "Add a %s layer to the given `dataset-or-template`,
         with possible additional substitutions if `submap` is provided."
         (or description (name mark)))
        (list '[dataset-or-template]
              (list fsymbol 'dataset-or-template {}))
        (list '[dataset-or-template submap]
              (list `layer 'dataset-or-template
                    `layer-base
                    (list `merge {:=mark mark}
                          'submap)))))

(def-mark-based-layer layer-point
  :point nil)

(def-mark-based-layer layer-line
  :line nil)

(def-mark-based-layer layer-bar
  :bar nil)

(def-mark-based-layer layer-boxplot
  :box
  "[boxplot](https://en.wikipedia.org/wiki/Box_plot)")

(def-mark-based-layer layer-segment
  :segment nil)

(def-mark-based-layer layer-text
  :text nil)

(dag/defn-with-deps smooth-stat ""
  [=dataset =x =y =predictors =group =design-matrix =model-options]
  (when-not (=dataset =y)
    (throw (ex-info "missing =y column"
                    {:missing-column-name =y})))
  (->> =predictors
       (run! (fn [p]
               (when-not (=dataset p)
                 (throw (ex-info "missing predictor column"
                                 {:predictors =predictors
                                  :missing-column-name p}))))))
  (->> =group
       (run! (fn [g]
               (when-not (=dataset g)
                 (throw (ex-info "missing =group column"
                                 {:group =group
                                  :missing-column-name g}))))))
  (let [predictions-fn (fn [ds]
                         (let [model (-> ds
                                         (tc/drop-missing [=y])
                                         (design-matrix/create-design-matrix [=y]
                                                                             =design-matrix)
                                         (tc/select-columns (->> =design-matrix
                                                                 (map first)
                                                                 (cons =y)))
                                         (ml/train =model-options))]
                           (-> ds
                               (design-matrix/create-design-matrix [=y]
                                                                   =design-matrix)
                               (ml/predict model)
                               =y)))]
    (if =group
      (-> =dataset
          (tc/group-by =group)
          (tc/add-column =y predictions-fn)
          (tc/order-by [=x])
          tc/ungroup)
      (-> =dataset
          (tc/add-column =y predictions-fn)
          (tc/order-by [=x])))))


(defn layer-smooth
  "
`layer-smooth` is a applies statistical regression methods
to the dataset to model it as a smooth shape.
It is inspired by ggplot's [geom_smooth](https://ggplot2.tidyverse.org/reference/geom_smooth.html).

By default, the regression is computed with only one predictor variable,
which is `:=x`.
  This can be overriden using the `:=predictors` key, which allows
  computing a regression with more than one predictor.

  One can also specify the predictor columns as expressions
  through the `:=design-matrix` key.
  Here, we use the design matrix functionality of
  [Metamorph.ml](https://github.com/scicloj/metamorph.ml).

  One can also provide the regression model details through `:=model-options`
  and use any regression model and parameters registered by Metamorph.ml.
  "
  ([context]
   (layer-smooth context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat (delay smooth-stat)
                  :=mark :line}
                 submap))))

(defn update-data [template dataset-fn & submap]
  (-> template
      (update-in [::ht/defaults :=dataset]
                 (fn [data]
                   (apply dataset-fn
                          data
                          submap)))))

(dag/defn-with-deps histogram-stat ""
  [=dataset =group =x =histogram-nbins]
  (when-not (=dataset =x)
    (throw (ex-info "missing =x column"
                    {:missing-column-name =x})))
  (->> =group
       (run! (fn [g]
               (when-not (=dataset g)
                 (throw (ex-info "missing =group column"
                                 {:group =group
                                  :missing-column-name g}))))))
  (let [summary-fn (fn [dataset]
                     (let [{:keys [bins max step]} (-> dataset
                                                       (get =x)
                                                       (fastmath.stats/histogram
                                                        =histogram-nbins))
                           left (map first bins)]
                       (-> {:left left
                            :right (concat (rest left)
                                           [max])
                            :count (map second bins)}
                           tc/dataset
                           (tc/add-columns {:middle #(tcc/*
                                                      0.5
                                                      (tcc/+ (:left %)
                                                             (:right %)))
                                            :width #(tcc/- (:right %)
                                                           (:left %))}))))]
    (if =group
      (-> =dataset
          (tc/group-by =group {:result-type :as-map})
          (->> (map (fn [[group ds]]
                      (-> ds
                          summary-fn
                          (tc/add-columns group))))
               (apply tc/concat)))
      (-> =dataset
          summary-fn))))





(defn layer-histogram
  "  Add a histogram layer to the given `dataset-or-template`,
  with possible additional substitutions if `submap` is provided.
  
  The histogram's binning and counting are computed
  using [Fastmath](https://github.com/generateme/fastmath).
  
  The `:=histogram-nbins` key controls the number of bins.

  If the plot is colored by a nominal type,
  then the data is grouped by this column,
  and overlapping histograms are generated.
  "
  ([context]
   (layer-histogram context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat (delay histogram-stat)
                  :=mark :bar
                  :=x-after-stat :middle
                  :=y-after-stat :count
                  :=bar-width :width
                  :=x-title :=x
                  :=y-title "count"
                  :=x-bin {:binned true}}
                 submap))))



(dag/defn-with-deps density-stat ""
  [=dataset =group =x =density-bandwidth]
  (when-not (=dataset =x)
    (throw (ex-info "missing =x column"
                    {:missing-column-name =x})))
  (->> =group
       (run! (fn [g]
               (when-not (=dataset g)
                 (throw (ex-info "missing =group column"
                                 {:group =group
                                  :missing-column-name g}))))))
  (let [summary-fn (fn [dataset]
                     (let [xs (=x dataset)
                           k (if =density-bandwidth
                               (fastmath.kernel/kernel-density :gaussian xs =density-bandwidth)
                               (fastmath.kernel/kernel-density :gaussian xs))
                           min-x (tcc/reduce-min xs)
                           max-x (tcc/reduce-max xs)
                           range-width (- max-x min-x)]
                       (when-not (< min-x max-x)
                         (throw (ex-info "invalid range"
                                         [min-x max-x])))
                       ;; using an int range to avoid the following bug:
                       ;; https://clojurians.zulipchat.com/#narrow/channel/236259-tech.2Eml.2Edataset.2Edev/topic/a.20strange.20bug.20with.20a.20range.20column
                       (-> {:x (-> (->> [(- min-x (* 0.5 range-width))
                                         (+ max-x (* 0.5 range-width))]
                                        (map #(int (* 100 %)))
                                        (apply range))
                                   (tcc/* 0.01))}
                           tc/dataset
                           (tc/map-columns :y [:x] k))))]
    (if =group
      (-> =dataset
          (tc/group-by =group {:result-type :as-map})
          (->> (map (fn [[group ds]]
                      (-> ds
                          summary-fn
                          (tc/add-columns group))))
               (apply tc/concat)))
      (-> =dataset
          summary-fn))))



(defn layer-density
  "Add an estimated density layer to the given `dataset-or-template`,
  with possible additional substitutions if `submap` is provided.
  
  The density is estimated by Gaussian kernel density estimation
  using [Fastmath](https://github.com/generateme/fastmath).

  The `:=density-bandwidth` can controls the bandwidth.
  Otherwise, it is determined by a rule of thumb.

  If the plot is colored by a nominal type,
  then the data is grouped by this column,
  and overlapping histograms are generated.
  "
  ([context]
   (layer-histogram context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat (delay density-stat)
                  :=mark :line
                  :=mark-fill :tozeroy
                  :=x-after-stat :x
                  :=y-after-stat :y
                  :=x-title :=x
                  :=y-title "density"}
                 submap))))

(defn dag [template]
  (let [edges (->> template
                   ::ht/defaults
                   (mapcat (fn [[k v]]
                             (if (fn? v)
                               (->> v
                                    meta
                                    :scicloj.tableplot.v1.dag/dep-ks
                                    (map #(vector % k)))))))
        nodes (flatten edges)]
    (kind/cytoscape
     {:elements {:nodes (->> nodes
                             (map (fn [k]
                                    {:data {:id k}})))
                 :edges (->> edges
                             (map (fn [[k0 k1]]
                                    {:data {:id (str k0 k1)
                                            :source k0
                                            :target k1}})))}
      :style [{:selector "node"
               :css {:content "data(id)"
                     :text-valign "center"
                     :text-halign "center"}}
              {:selector "parent"
               :css {:text-valign "top"
                     :text-halign "center"}}
              {:selector "edge"
               :css {:curve-style "bezier"
                     :target-arrow-shape "triangle"}}]
      :layout {:name "breadthfirst"
               :padding 5}})))

(defn debug
  "(experimental)

  Given a `template` and a substitution key `k`,
  find out what value `k` would receive when realizing the template.

  Given a `template`, a `layer-idx` integer, and a substitution key `k`,
  find out what value `k` would receive when realizing the `layer-idx`th layer in the template.
  "
  ([template k]
   (-> template
       (assoc ::debug k)
       plot
       ::debug))
  ([template layer-idx k]
   (-> template
       (assoc ::debug :=layers)
       (assoc-in [::ht/defaults :=layers layer-idx ::debug1]
                 k)
       plot
       ::debug
       (nth layer-idx)
       ::debug1)))
