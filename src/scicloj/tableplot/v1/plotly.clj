(ns scicloj.tableplot.v1.plotly
  (:require [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [tech.v3.dataset :as ds]
            [tech.v3.dataset.modelling :as dsmod]
            [fastmath.stats :as stats]
            [fastmath.grid :as grid]
            [fastmath.kernel :as kernel]
            [scicloj.metamorph.ml :as ml]
            [scicloj.metamorph.ml.design-matrix :as design-matrix]
            [scicloj.tableplot.v1.dag :as dag]
            [clojure.string :as str]
            [scicloj.tableplot.v1.util :as util]
            [scicloj.tableplot.v1.cache :as cache]
            [scicloj.tableplot.v1.xform :as xform]
            [tech.v3.libs.buffered-image :as bufimg]
            [tech.v3.tensor :as tensor]
            [tech.v3.datatype :as dtype]
            [clojure.math :as math])
  (:import java.awt.image.BufferedImage))

(defn submap->field-type [colname-key]
  (let [dataset-key :=dataset]
    (dag/fn-with-deps-keys
     (str (format "Check the field type of the column specified by `%s` after `:=stat`."
                  colname-key)
          "

- `:quantitative` - numerical columns
- `:temporal` - date-time columns
- `:nominal` - all other column types (e.g., Strings, keywords)
")
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
  (str (format "Check the field type of the column specified by `%s` after `:=stat`."
               colname-key)
       "

- `:quantitative` - numerical columns
- `:temporal` - date-time columns
- `:nominal` - all other column types (e.g., Strings, keywords)
")
  (let [dataset-key :=stat
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



(dag/defn-with-deps submap->group
  "Infer the relevant grouping for statistical layers such as `layer-smooth`.
The `:=color` column affects the grouing if and only if `:=color-type` is `:nominal`.
The `:=size` column affects the grouing if and only if `:=size-type` is `:nominal`.
The `:=symbol` column affects the grouping.
"
  [=color =color-type =size =size-type =symbol]
  (concat (when (= =color-type :nominal)
            [=color])
          (when (= =size-type :nominal)
            [=size])
          (when =symbol
            [=symbol])))

(defn mark->mode [mark]
  (case mark
    :point :markers
    :text :text
    :line :lines
    :box nil
    :violin nil
    :bar nil
    :segment :lines
    :heatmap nil
    :surface nil))

(dag/defn-with-deps submap->mode
  "Determine the Plotly.js mode for a trace."
  [=mark]
  (mark->mode =mark))

(dag/defn-with-deps submap->type
  "Determine the Plotly.js type for a trace."
  [=mark =coordinates]
  (str (case =mark
         :box "box"
         :violin "violin"
         :bar "bar"
         :heatmap "heatmap"
         :surface "surface"
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

(def symbols-palette
  [:circle :diamond :x :square
   :circle-open :diamond-open :x-open :square-open])

(def view-base
  {:data :=traces
   :layout :=layout})

(dag/defn-with-deps submap->marker-size-key
  "Determine which Plotly.js key should be used to specify the mark size.
For lines, it is `:width`. Otherwise, it is `:size`."
  [=mode =type]
  (if (or (= =mode :lines)
          (= =type :line)) :width
      :size))

(def layer-base
  {:dataset :=stat
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
   :size-range :=size-range
   :symbol :=symbol
   :text :=text
   :inferred-group :=inferred-group
   :group :=group
   :marker-override {:color :=mark-color
                     :=marker-size-key :=mark-size
                     :symbol :=mark-symbol
                     :colorscale :=colorscale}
   :fill :=mark-fill
   :trace-base {:mode :=mode
                :type :=type
                :opacity :=mark-opacity
                :textfont :=textfont}
   :box-visible :=box-visible
   :meanline-visible :=meanline-visible
   :boxmode :=boxmode
   :violinmode :=violinmode
   :name :=name
   :zmin :=zmin
   :zmax :=zmax
   :colorscale :=colorscale
   :annotations :=annotations})

(defn- rerange [values [left right]]
  (let [minv (tcc/reduce-min values)
        maxv (tcc/reduce-max values)
        ratio (/ (- right left)
                 (- maxv minv))]
    (-> values
        (tcc/- minv)
        (tcc/* ratio)
        (tcc/+ left))))

(dag/defn-with-deps submap->traces
  "Create the Plotly.js traces from the Tableplot layers."
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
                 size size-type size-range
                 symbol
                 text
                 marker-override
                 fill
                 inferred-group
                 trace-base
                 box-visible meanline-visible
                 zmin zmax colorscale]}]
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
                                      :nominal {:color (cache/cached-assignment (get group-key color)
                                                                                colors-palette
                                                                                ::color)}
                                      :quantitative {:color (-> color group-dataset vec)}))
                                  (when size
                                    (case size-type
                                      :nominal {:size (cache/cached-assignment (get group-key size)
                                                                               sizes-palette
                                                                               ::size)}
                                      :quantitative {:size (if size-range
                                                             (-> size
                                                                 group-dataset
                                                                 (cond-> size-range
                                                                   (rerange size-range))
                                                                 vec))}))
                                  (when symbol
                                    {:symbol (cache/cached-assignment (get group-key symbol)
                                                                      symbols-palette
                                                                      ::symbol)})
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
                             (when box-visible
                               {:box {:visible true}})
                             (when meanline-visible
                               {:meanline {:visible true}})
                             {:z (some-> z group-dataset vec)}
                             (when zmin {:zmin zmin})
                             (when zmax {:zmax zmax})
                             (when colorscale {:colorscale colorscale})
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


(dag/defn-with-deps submap->layout
  "Create the layout part of the Plotly.js specification."
  [=width =height =margin =automargin =background =title
   =xaxis-gridcolor =yaxis-gridcolor
   =x-after-stat =y-after-stat
   =x-title =y-title
   =x-showgrid =y-showgrid
   =boxmode =violinmode
   =annotations
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
                               last))
        final-boxmode (or =boxmode
                          (->> =layers
                               (map :boxmode)
                               first))
        final-violinmode (or =violinmode
                             (->> =layers
                                  (map :violinmode)
                                  first))
        final-annotations (->> =layers
                               (map :annotations)
                               (apply concat =annotations)
                               seq)]
    (merge {:width =width
            :height =height
            :margin =margin
            :automargin =automargin
            :plot_bgcolor =background
            :xaxis {:gridcolor =xaxis-gridcolor
                    :title final-x-title
                    :showgrid =x-showgrid}
            :yaxis {:gridcolor =yaxis-gridcolor
                    :title final-y-title
                    :showgrid =y-showgrid}
            :title =title}
           (when final-annotations
             {:annotations final-annotations})
           (when final-boxmode
             {:boxmode final-boxmode})
           (when final-violinmode
             {:violinmode final-violinmode}))))

(dag/defn-with-deps submap->design-matrix
  "Determine a trivial design matrix specifiation from a set of `:=predictors` columns.
The design matrix simply uses these columns without any additional transformation."
  [=predictors]
  (->> =predictors
       (mapv (fn [k]
               [k (list
                   'identity
                   k)]))))


(defn- dataset->splom-dimensions
  [dataset colnames]
  (->> colnames
       (map (fn [colname]
              {:label colname
               :values (-> colname
                           dataset
                           vec)}))))

(dag/defn-with-deps submap->splom-traces
  "Create the trace for a SPLOM plot."
  [=dataset =colnames =color =color-type =symbol =splom-colnames]
  (let []
    (cond
      ;; varying color
      (and =color
           (= =color-type :nominal))
      (let [class->color (-> =color
                             =dataset
                             distinct
                             (interleave colors-palette)
                             (->> (apply hash-map)))]
        (-> =dataset
            (tc/group-by =color {:result-type :as-map})
            (->> (map (fn [[cls group-dataset]]
                        {:type :splom
                         :dimensions (dataset->splom-dimensions
                                      group-dataset
                                      =colnames)
                         :marker {:color (class->color cls)}
                         :name cls})))))
      ;; varying symbol
      =symbol
      (let [class->symbol (-> =symbol
                              =dataset
                              distinct
                              (interleave symbols-palette)
                              (->> (apply hash-map)))]
        (-> =dataset
            (tc/group-by =symbol {:result-type :as-map})
            (->> (map (fn [[cls group-dataset]]
                        {:type :splom
                         :dimensions (dataset->splom-dimensions
                                      group-dataset
                                      =colnames)
                         :marker {:symbol (class->symbol cls)}
                         :name cls})))))
      ;; else
      :else
      [{:type :splom
        :dimensions (dataset->splom-dimensions
                     =dataset
                     =colnames)}])))


(dag/defn-with-deps submap->splom-layout
  "Create the layout for a SPLOM plot."
  [=layout =colnames]
  (let [axis {:showline false
              :zeroline false
              :gridcolor "#ffff"
              :ticklen 4}]
    (->> =colnames
         count
         range
         (mapcat (fn [i]
                   (let [suffix (when (pos? i)
                                  (str (inc i)))]
                     [[(keyword (str "xaxis" suffix)) axis]
                      [(keyword (str "yaxis" suffix)) axis]])))
         (into (merge =layout
                      {:hovermode :closest
                       :dragmode :select})))))

(dag/defn-with-deps submap->grid-nrows
  nil
  [=inner-plots]
  (-> =inner-plots count math/sqrt math/ceil))

(dag/defn-with-deps submap->grid-layout
  ;; define
  nil
  [=inner-plots =grid-nrows] ;; grid-nrows; be default derived from grid-nplots
  (let [axis {:showline false
              :zeroline false
              :gridcolor "#ffff"
              :ticklen 4}
        n (count =inner-plots)
        nrow (min =grid-nrows n)
        ncol (-> (/ n nrow) math/ceil)
        step-row (/ 1.0 nrow)
        step-col (/ 1.0 ncol)]
    (into {}
          (concat
           (map #(vector (keyword (str "xaxis" %)) (conj axis {:domain (vector (* step-row (dec %)) (* step-row %))}))
                (range 1 (inc nrow)))
           (map #(vector (keyword (str "yaxis" %)) (conj axis {:domain (vector (* step-col (dec %)) (* step-col %))}))
                (range 1 (inc ncol))))))) ;; no gaps between plots

(dag/defn-with-deps grid-traces-no-xform
  nil
  [=inner-plots =grid-nrows]
  (let [n (count =inner-plots)
        nrow (min =grid-nrows n)
        ncol (-> (/ n nrow) math/ceil)]
    (mapcat (fn [plot-layers xaxis yaxis]
              (map (fn [layer] (conj layer {:xaxis (str "x" xaxis)
                                            :yaxis (str "y" yaxis)}))
                   plot-layers))
            (->> =inner-plots (map :data))
            (mapcat #(repeat ncol %) (range 1 (inc nrow)))
            (cycle (range 1 (inc ncol))))))

(dag/defn-with-deps submap->colnames
  "Extract all column names of the dataset."
  [=dataset]
  (when =dataset
    (tc/column-names =dataset)))

(def standard-defaults
  [[:=stat :=dataset
    "The data resulting from a possible statistical transformation."]
   [:=dataset hc/RMV
    "The data to be plotted."]
   [:=x :x
    "The column for the x axis."]
   [:=x-after-stat :=x
    "The column for the x axis to be used after `:=stat`."]
   [:=y :y
    "The column for the y axis."]
   [:=y-after-stat :=y
    "The column for the y axis to be used after `:=stat`."]
   [:=z :z
    "The column for the z axis."]
   [:=z-after-stat :=z
    "The column for the z axis to be used after `:=stat`."]
   [:=x0 hc/RMV
    "The column for the first x axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=x0-after-stat :=x0
    "The column for the first x axis value after `:=stat`, in cases where pairs are needed, e.g. segment layers."]
   [:=y0 hc/RMV
    "The column for the first y axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=y0-after-stat :=y0
    "The column for the first y axis value after `:=stat`, in cases where pairs are needed, e.g. segment layers."]
   [:=x1 hc/RMV
    "The column for the second x axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=x1-after-stat :=x1
    "The column for the second x axis value after `:=stat`, in cases where pairs are needed, e.g. segment layers."]
   [:=y1 hc/RMV
    "The column for the second y axis value, in cases where pairs are needed, e.g. segment layers."]
   [:=y1-after-stat :=y1
    "The column for the second y axis value after `:=stat`, in cases where pairs are needed, e.g. segment layers."]
   [:=bar-width hc/RMV
    "The column to determine the bar width in bar layers."]
   [:=color hc/RMV
    "The column to determine the color of marks."]
   [:=size hc/RMV
    "The column to determine the size of marks."]
   [:=size-range [10 30]
    "The desired range of values for the marker sizes when sizing by a quantitative variable."]
   [:=symbol hc/RMV
    "The column to determine the [symbol](https://plotly.com/javascript/reference/#box-marker-symbol) of marks."]
   [:=x-type (submap->field-type :=x)
    "The field type of the column used to determine the x axis."]
   [:=x-type-after-stat (submap->field-type-after-stat :=x-after-stat)
    "The field type of the column used to determine the x axis after `:=stat`."]
   [:=y-type (submap->field-type :=y)
    "The field type of the column used to determine the y axis."]
   [:=y-type-after-stat (submap->field-type-after-stat :=y-after-stat)
    "The field type of the column used to determine the y axis after `:=stat`."]
   [:=z-type (submap->field-type :=z)
    "The field type of the column used to determine the z axis."]
   [:=z-type-after-stat (submap->field-type-after-stat :=z-after-stat)
    "The field type of the column used to determine the z axis after `:=stat`."]
   [:=r hc/RMV
    "The column for the radius in polar coordinates."]
   [:=theta hc/RMV
    "The column for the angle in polar coordinates."]
   [:=lat hc/RMV
    "The column for the latitude in geo coordinates."]
   [:=lon hc/RMV
    "The column for the longitude in geo coordinates."]
   [:=color-type (submap->field-type :=color)
    "The field type of the column used to determine mark color."]
   [:=size-type (submap->field-type :=size)
    "The field type of the column used to determine mark size"]
   [:=mark-color hc/RMV
    "A fixed color specification for marks."]
   [:=mark-size hc/RMV
    "A fixed size specification for marks."]
   [:=marker-size-key submap->marker-size-key
    "What key does Plotly.js use to hold the marker size?"]
   [:=mark-symbol hc/RMV
    "A fixed [symbol](https://plotly.com/javascript/reference/#box-marker-symbol) specification for marks"]
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
    "The layer name (which affects the Plotly.js traces names and appears in the legend)."]
   [:=layers []
    "A vector of all lyaers in the plot (an inermediate Tableplot representation before converting to Plotly.js traces)."]
   [:=traces submap->traces
    "A vector of all Plotly.js traces in the plot."]
   [:=layout submap->layout
    "The layout part of the resulting Plotly.js specification."]
   [:=inferred-group submap->group
    "A list of columns to be used for grouping of statistical computations, inferred from other keys and data (e.g., `:=color`)."]
   [:=group :=inferred-group
    "A list of columns to be used for grouping of statisticsl computations, a possible user override of `:=inerred-group`."]
   [:=predictors [:=x]
    "The list of predictors to be used in regression (`layer-smooth`)."]
   [:=design-matrix submap->design-matrix
    "The design matrix definition to be used in regression (`layer-smooth`)."]
   [:=model-options {:model-type :metamorph.ml/ols}
    "The optional specification of a model for regression (`layer-smooth`)."]
   [:=histogram-nbins 10
    "The number of bins for `layer-histogram`."]
   [:=density-bandwidth hc/RMV
    "The bandwidth of density estimation for `layer-density`."]
   [:=box-visible hc/RMV
    "Should the boxplot be visible in Violin plots? (boolean)"]
   [:=meanline-visible hc/RMV
    "Should the mean line be visible in Violin plots? (boolean)"]
   [:=boxmode hc/RMV
    "How to show a group of box plots? The default is `nil`, which means overlay. The alternative is `:group`."]
   [:=violinmode hc/RMV
    "How to show a group of violin plots? The default is `nil`, which means overlay. The alternative is `:group`."]
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
   [:=x-showgrid true
    "Should we show the grid for the x axis?"]
   [:=y-showgrid true
    "Should we show the grid for the y axis?"]
   [:=background "rgb(235,235,235)"
    "The plot background color."]
   [:=xaxis-gridcolor "rgb(255,255,255)"
    "The color for the x axis grid lines."]
   [:=yaxis-gridcolor "rgb(255,255,255)"
    "The color for the y axis grid lines."]
   [:=colnames hc/RMV
    "Column names for a SPLOM plot."]
   [:=inner-plots hc/RMV
    "List of plots (used in a grid)"]
   [:=grid-nrows submap->grid-nrows
    "The number of rows in a plot grid."]
   [:=grid-layout submap->grid-layout
    "The layout of a plot grid."]
   [:=grid-traces grid-traces-no-xform
    "The trace of a plot grid."]
   [:=colnames submap->colnames
    "Column names for a SPLOM plot. The default is all columns of the dataset."]
   [:=splom-layout submap->splom-layout
    "The layout for a SPLOM plot."]
   [:=splom-traces submap->splom-traces
    "The trace for a SPLOM plot."]
   [:=zmin hc/RMV
    "Minimal z range value for heatmap."]
   [:=zmax hc/RMV
    "Maximal z range value for heatmap."]
   [:=colorscale hc/RMV
    "[Color scale](https://plotly.com/javascript/colorscales/) for heatmap and scatterplots."]
   [:=annotations hc/RMV
    "Plot [annotations](https://plotly.com/javascript/text-and-annotations/)."]])

(def standard-defaults-map
  (->> standard-defaults
       (map (comp vec (partial take 2)))
       (into {})))

(defn- plotly [spec]
  (kind/plotly spec {:style {:height :auto}}))

(defn- plotly-xform [template]
  (cache/with-clean-cache
    (-> template
        xform/xform
        plotly
        (dissoc :kindly/f))))

(defn base
  "  The `base` function can be used to create the basis
  template to which we can add layers.
  It can be used to set up some substitution keys to be shared
  by the various layers.
  It can also be used to set up some general substitution keys,
  which affect the layout rather than any specific layer.

  The return value is always a template which is set up
  to be visualized as Plotly.js.
  
  In the full case of three arguments `(dataset template submap)`,
  `dataset` is added to `template` as the value substituted for the 
  `:=dataset` key, and the substitution map `submap` is added as well.

  In the other cases, if the `template` is not passed missing,
  it is replaced by a minimal base template to be carried along
  the pipeline. If the `dataset` or `submap` parts are not passed,
  they are simply not substituted into the template.

  If the first argument is a dataset, it is converted to
  a very basic template where it is substituted at the `:=dataset` key.

  We typically use `base` with other layers added to it.
  The base substitutions are shared between layers,
  and the layers can override them and add substitutions of their own.

  🔑 **Main useful keys:**

  - the keys which are useful for `layer`
  - the keys that affect `:=layout`
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


(defn plot
  "The `plot` function realizes a template as a Plotly.js specification."
  [template]
  (plotly-xform template))

(defn layer
  "The `layer` function is typically not used on the user side.
  It is a generic way to create more specific functions to add layers
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

  🔑 **Main useful keys:**

  - `:=mark`
  - The keys that are useful for the `layer-*` functions."
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
  [fsymbol mark description suffix]
  (list 'defn fsymbol
        (format
         "Add a %s layer to the given `dataset-or-template`,
with possible additional substitutions if `submap` is provided.

%s"
         (or description (name mark))
         suffix)
        (list '[dataset-or-template]
              (list fsymbol 'dataset-or-template {}))
        (list '[dataset-or-template submap]
              (list `layer 'dataset-or-template
                    `layer-base
                    (list `merge {:=mark mark}
                          'submap)))))

(def-mark-based-layer layer-point
  :point
  nil
  "🔑 **Main useful keys:**
  `:=dataset` `:=mark` `:=x` `:=y`
  `:=color` `:=size` `:=size-range` `:=symbol` `:=color-type` `:=size-type`
  `:=mark-color` `:=mark-size` `:=mark-symbol` `:=mark-opacity`")

(def-mark-based-layer layer-line
  :line
  nil
  "🔑 **Main useful keys:**
  `:=dataset` `:=x` `:=y`
  `:=color` `:=size` `:=siz-range` `:=color-type` `:=size-type`
  `:=mark-color` `:=mark-size` `:=mark-opacity`")

(def-mark-based-layer layer-bar
  :bar
  nil
  "🔑 **Main useful keys:**
  `:=bar-width`
  `:=dataset` `:=x` `:=y`
  `:=color` `:=size` `:=color-type` `:=size-type`
  `:=mark-color` `:=mark-size` `:=mark-opacity`")

(def-mark-based-layer layer-boxplot
  :box
  "[boxplot](https://en.wikipedia.org/wiki/Box_plot)"
  "🔑 **Main useful keys:**
  `:=boxmode`
  `:=dataset` `:=x` `:=y`
  `:=color` `:=size` `:=color-type` `:=size-type`
  `:=mark-color` `:=mark-size` `:=mark-opacity`")

(def-mark-based-layer layer-violin
  :violin
  "[Violin plot](https://en.wikipedia.org/wiki/Violin_plot)"
  "🔑 **Main useful keys:**
  `:=violinmode` `:=box-visible` `:=meanline-visible`
  `:=dataset` `:=x` `:=y`
  `:=color` `:=size` `:=color-type` `:=size-type`
  `:=mark-color` `:=mark-size` `:=mark-opacity`")

(def-mark-based-layer layer-segment
  :segment
  nil
  "🔑 **Main useful keys:**
  `:=dataset` `:=x0` `:=y0` `:=x1` `:=y1` 
  `:=color` `:=size` `:=color-type` `:=size-type`
  `:=mark-color` `:=mark-size` `:=mark-opacity`")

(def-mark-based-layer layer-text
  :text
  nil
  "🔑 **Main useful keys:**
  `:=text` `:=textfont`
  `:=dataset` `:=x` `:=y`
  `:=color` `:=size` `:=color-type` `:=size-type`
  `:=mark-color` `:=mark-size` `:=mark-opacity`")

(def-mark-based-layer layer-heatmap
  :heatmap
  nil
  "🔑 **Main useful keys:**
  `:=dataset` `:=x` `:=y` `:=z`
  `:=zmin` `:=zmax` `:=colorscale`")

(def-mark-based-layer layer-surface
  :surface
  nil
  "🔑 **Main useful keys:**
  `:=dataset` `:=z`")

(dag/defn-with-deps smooth-stat
  "Compute a dataset
with the `:=y` column in `:=dataset` replaced with
its value predicted by regression,
and with the results ordered by the `:=x` column.

The predictor columns are specified by `:=design-matrix`

and the regression model is specified by `:=model-options`.

If the grouping list of columns `:=group` is specified,
then the regression is computed in groups.
" 
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
                         (when =model-options
                                        ; in the future, we will support
                                        ; other ways to specify a model
                           (require '[scicloj.metamorph.ml.regression]))
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
  Add a smoothed layer layer to the given `dataset-or-template`,
  with possible additional substitutions if `submap` is provided.

  Statistical [regression](https://en.wikipedia.org/wiki/Regression_analysis)
  methods are applied to the dataset to model it as a smooth shape.
  It is inspired by ggplot's [geom_smooth](https://ggplot2.tidyverse.org/reference/geom_smooth.html).
  
  `smooth-stat` is used internally as `:=stat`.

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

  The regressions computed are done on a group level, where the grouping
  can be inferred as `:=inferred-group`
  but can also be user-overridden through `:=group`.
  "
  ([context]
   (layer-smooth context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat smooth-stat
                  :=mark :line}
                 submap))))

(defn update-data [template dataset-fn & submap]
  (-> template
      (update-in [::ht/defaults :=dataset]
                 (fn [data]
                   (apply dataset-fn
                          data
                          submap)))))

(dag/defn-with-deps correlation-stat
  "Compute a dataset representing the [correlations](https://en.wikipedia.org/wiki/Histogram)
of the columns in `:=dataset`."
  [=dataset]
  (let [names (tc/column-names =dataset)]
    (tc/dataset {:row names
                 :col names
                 :corr (-> =dataset
                           tc/columns
                           fastmath.stats/correlation-matrix)})))


(dag/defn-with-deps submap->correlation-annotations
  "Crate the annotations for a correlation heatmap layer."
  [=stat]
  (let [{:keys [row col corr]} =stat
        nrow (count row)
        ncol (count col)
        font-size (int (/ 100 (max nrow ncol)))]
    (for [i (range nrow) 
          j (range ncol)]
      {:x (col j)
       :y (row i)
       :text (format "%.02f" ((corr i) j))
       :showarrow false
       :font {:size font-size
              :color "white"}})))

(defn layer-correlation
  "Add a correlation heatmap
  layer to the given `dataset-or-template`,
  with possible additional substitutions if `submap` is provided.

  See also: `layer-heatmap`.

  🔑 **Main useful keys:**
  `:=dataset` `:=zmin` `:=zmax` `:=colorscale`"
  ([context]
   (layer-correlation context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat correlation-stat
                  :=mark :heatmap
                  :=x-after-stat :col
                  :=y-after-stat :row
                  :=z-after-stat :corr
                  :=x-title ""
                  :=y-title ""
                  :=zmin -1
                  :=zmax 1
                  :=annotations submap->correlation-annotations}
                 submap))))


(dag/defn-with-deps histogram-stat
  "Compute a dataset representing the [histogram](https://en.wikipedia.org/wiki/Histogram)
of the `:=x` column in `:=dataset`.

The histogram's binning and counting are computed
using [Fastmath](https://github.com/generateme/fastmath).

The number of bins is specified by `:histogram-nbins`.

If the grouping list of columns `:=group` is specified,
then the histogram is computed in groups."
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
  "Add a [histogram](https://en.wikipedia.org/wiki/Histogram)
  layer to the given `dataset-or-template`,
  with possible additional substitutions if `submap` is provided.
  
  `histogram-stat` is used internally as `:=stat`.
  
  The histogram's binning and counting are computed
  using [Fastmath](https://github.com/generateme/fastmath).
  
  The `:=histogram-nbins` key controls the number of bins.

  If a list of grouping columns `:=group` is specified,
  e.g., when the plot is colored by a nominal type,
  then the data is grouped by this column,
  and overlapping histograms are generated.

  🔑 **Main useful keys:**
  `:=histogram-nbins`
  `:=dataset` `:=x`
  `:=color` `:=color-type`
  `:=mark-color` `:=mark-size` `:=mark-opacity`"
  ([context]
   (layer-histogram context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat histogram-stat
                  :=mark :bar
                  :=x-after-stat :middle
                  :=y-after-stat :count
                  :=bar-width :width
                  :=x-title :=x
                  :=y-title "count"
                  :=x-bin {:binned true}}
                 submap))))


(defn compute-histogram-2d [dataset x-colname y-colname nbins]
  (let [xs (dataset x-colname)
        ys (dataset y-colname)
        x-min (tcc/reduce-min xs)
        x-max (tcc/reduce-max xs)
        x-gap (double (- x-max x-min))
        y-min (tcc/reduce-min ys)
        y-max (tcc/reduce-max ys)
        y-gap (double (- y-max y-min))
        normalized-xs (-> xs (tcc/- x-min) (tcc// x-gap))
        normalized-ys (-> ys (tcc/- y-min) (tcc// y-gap))
        ;; nbins (max (stats/estimate-bins normalized-xs)
        ;;            (stats/estimate-bins normalized-ys))
        size (/ (math/sqrt nbins))
        grid (grid/grid :square size)]
    (-> {:normalized-x normalized-xs
         :normalized-y normalized-ys}
        tc/dataset
        tc/rows
        (->> (map (partial grid/coords->mid grid)))
        frequencies
        (->> (map (fn [[[mid-normalized-x mid-normalized-y] freq]]
                    {:mid-normalized-x mid-normalized-x
                     :mid-normalized-y mid-normalized-y
                     :count freq})))
        tc/dataset
        (tc/add-column x-colname
                       #(tcc/* x-gap
                               (tcc/+ x-min
                                      (:mid-normalized-x %))))
        (tc/add-column y-colname
                       #(tcc/* y-gap
                               (tcc/+ y-min
                                      (:mid-normalized-y %))))
        (tc/select-columns [x-colname y-colname :count]))))


(dag/defn-with-deps histogram2d-stat
  "Compute a dataset representing a 2d histogram
of columns `:=x` `:=y` in `:=dataset`."
  [=dataset =x =y =histogram-nbins]
  (compute-histogram-2d =dataset =x =y =histogram-nbins))


(defn layer-histogram2d
  "Given columns `=x`,`=y`,
  add a corresponding 2d histogram heatmap 
  layer to the given `dataset-or-template`,
  with possible additional substitutions if `submap` is provided.

  See also: `layer-heatmap`.

  🔑 **Main useful keys:**
  `:=dataset` `:=x` `:=y` `:=histogram-nbins` `:=colorscale`"
  ([context]
   (layer-histogram2d context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat histogram2d-stat
                  :=mark :heatmap
                  :=z-after-stat :count}
                 submap))))



(dag/defn-with-deps density-stat
  "Compute a dataset representing the approximated [density](https://en.wikipedia.org/wiki/Histogram)
of the `:=x` column in `:=dataset`.

The density is estimated by Gaussian [kernel density estimation](https://en.wikipedia.org/wiki/Kernel_density_estimation)
using [Fastmath](https://github.com/generateme/fastmath).

The `:=density-bandwidth` can controls the bandwidth.
Otherwise, it is determined by a rule of thumb.

If the grouping list of columns `:=group` is specified,
then the density is estimated in groups.

🔑 **Main useful keys:**
`:=density-bandwidth`
`:=dataset` `:=x`
`:=color` `:=color-type`
`:=mark-color` `:=mark-size` `:=mark-opacity`" 
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
                     (let [xs (dataset =x)
                           k (if =density-bandwidth
                               (kernel/kernel-density :gaussian xs =density-bandwidth)
                               (kernel/kernel-density :gaussian xs))
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
  "
  (experimental)
  
  Add an estimated density layer to the given `dataset-or-template`,
  with possible additional substitutions if `submap` is provided.

  `density-stat` is used internally as `:=stat`.
  
  The density is estimated by Gaussian [kernel density estimation](https://en.wikipedia.org/wiki/Kernel_density_estimation)
  using [Fastmath](https://github.com/generateme/fastmath).

  The `:=density-bandwidth` can controls the bandwidth.
  Otherwise, it is determined by a rule of thumb.

  If a list of grouping columns `:=group` is specified,
  e.g., when the plot is colored by a nominal type,
  then the data is grouped by this column,
  and overlapping densities are generated.

  🔑 **Main useful keys:**
  `:=dataset` `:=x` `:=y`
  `:=predictors` `:=design-matrix` `:=model-options`
  `:=group`
  `:=mark-color` `:=mark-size` `:=mark-opacity`
  "
  ([context]
   (layer-density context {}))
  ([context submap]
   (layer context
          layer-base
          (merge {:=stat density-stat
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

  Given a `template` and a `result` structure involving substitution keys,
  find out what value `result` would receive when realizing the template.

  Given a `template`, a `layer-idx` integer, and a `result` structure involving substitution keys,
  find out what value `result` would receive when realizing the `layer-idx`th layer in the template.
  "
  ([template result]
   (-> template
       (assoc ::debug result)
       plot
       ::debug))
  ([template layer-idx result]
   (-> template
       (assoc ::debug :=layers)
       (assoc-in [::ht/defaults :=layers layer-idx ::debug1]
                 result)
       plot
       ::debug
       (nth layer-idx)
       ::debug1)))

(defn img->tensor [^BufferedImage image]
  (let [t (bufimg/as-ubyte-tensor image)
        [width height channel] (dtype/shape t)
        {:keys [gray a r g b]} (bufimg/image-channel-map image)]
    (cond gray (tensor/compute-tensor [width height 3]
                                      (fn [i j k]
                                        (t i j 0))
                                      :uint8)
          a (tensor/compute-tensor [width height 4]
                                   (fn [i j k]
                                     (t i
                                        j
                                        (case k
                                          0 r
                                          1 g
                                          2 b
                                          3 a)))
                                   :uint8)
          :else (tensor/compute-tensor [width height 3]
                                       (fn [i j k]
                                         (t i
                                            j
                                            (case k
                                              0 r
                                              1 g
                                              2 b)))
                                       :uint8))))

(defn imshow
  "Show a given `image` -
  either a `java.awt.image.BufferedImage` object
  or a two dimensional matrix of RGB triples."
  [image]
  (plotly-xform
   {:data [{:z (if (instance? java.awt.image.BufferedImage image)
                 (img->tensor image)
                 image)
            :type :image}]
    :layout :=layout
    ::ht/defaults (assoc standard-defaults-map
                         :=x-showgrid false
                         :=y-showgrid false
                         :=x-title ""
                         :=y-title "")}))

(defn surface
  "Show a given surface, represented as a matrix of `z` values, in 3d."
  [surface]
  (plotly-xform
   {:data [{:z surface
            :type :surface}]
    :layout :=layout
    ::ht/defaults standard-defaults-map}))

(defn submap->field-type [colname-key]
  (let [dataset-key :=dataset]
    (dag/fn-with-deps-keys
     (str (format "Check the field type of the column specified by `%s` after `:=stat`."
                  colname-key)
          "

- `:quantitative` - numerical columns
- `:temporal` - date-time columns
- `:nominal` - all other column types (e.g., Strings, keywords)
")
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

(defn splom
  "Show a SPLOM (ScatterPLOt Matrix) of given dimensions of a dataset.
  🔑 **Main useful keys:**
  `:=dataset` `:=colnames`
  `:=color` `:=color-type` `:=symbol`
  and the other keys that affect `:=layout`, especially `:=height` and `:=width`.
  "
  [dataset submap]
  (-> dataset
      (base submap)
      (assoc :data :=splom-traces
             :layout :=splom-layout)
      plotly-xform))

(defn grid
  "Arrange a list of plots into a grid."
  [plots]
  (plotly-xform
   {:data :=grid-traces-no-xform
    :layout :=grid-layout
    ::ht/defaults (assoc standard-defaults-map
                         :inner-plots plots)}))
