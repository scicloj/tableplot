(ns
 tableplot-book.plotly-reference-generated-test
 (:require
  [scicloj.tableplot.v1.plotly :as plotly]
  [tablecloth.api :as tc]
  [tablecloth.column.api :as tcc]
  [tech.v3.tensor :as tensor]
  [scicloj.kindly.v4.kind :as kind]
  [scicloj.kindly.v4.api :as kindly]
  [scicloj.tableplot.v1.dag :as dag]
  [scicloj.metamorph.ml.rdatasets :as rdatasets]
  [clojure.string :as str]
  [aerial.hanami.common :as hc]
  [aerial.hanami.templates :as ht]
  [clojure.math :as math]
  [clojure.test :refer [deftest is]]))


(def v2_l34 (require '[scicloj.tableplot.v1.book-utils :as book-utils]))


(def
 v3_l37
 (book-utils/md
  "## Overview 🐦\nThe Tableplot Plotly API allows the user to write functional pipelines\nthat create and process *templates* of plots, that can eventually be realized\nas [Plotly.js](https://plotly.com/javascript/) specifications.\n\nThe data is assumed to be held in datasets defined by [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset)\n(which can conveniently be used using [Tablecloth](https://scicloj.github.io/tablecloth)).\n\nThe templates are an adapted version of\n[Hanami Templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations).\nHanami transforms templates by recursively applying a simple set of rules.\nThe rules are based on looking up *substitution keys* according to standard defaults\nas well as user substitutions overriding those defaults.\nTableplot uses a slighly adapted version of Hanami's template transformations,\nwhich makes sure not to recurse into datasets.\n\nFor example, the `layer-point` function generates a template with some specified\nsubstitutions. Let us apply this function to a dataset with some user substitutions.\nAs you can see below, all the substitution keys are keywords beginning with `=`. E.g., `:=color-type`.\nThis is just a convention that helps distinguish their role from other keys.\n\nBy default, this template is annotated by the [Kindly](https://scicloj.github.io/kindly/)\nstandared so that tools supporting Kindly (such as [Clay](https://scicloj.github.io/clay/))\nwill display by realizing it and using it as a Plotly.js specification.     \n"))


(def
 v4_l64
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 10})))


(deftest
 t6_l71
 (is
  ((fn*
    [p1__103413#]
    (contains?
     (:aerial.hanami.templates/defaults p1__103413#)
     :=dataset))
   v4_l64)))


(deftest
 t8_l74
 (is
  ((fn*
    [p1__103414#]
    (let
     [layer-defaults
      (->
       p1__103414#
       :aerial.hanami.templates/defaults
       :=layers
       first
       :aerial.hanami.templates/defaults)]
     (and
      (= (:=x layer-defaults) :sepal-width)
      (= (:=y layer-defaults) :sepal-length))))
   v4_l64)))


(deftest
 t10_l79
 (is
  ((fn*
    [p1__103415#]
    (= (-> p1__103415# plotly/plot :data first :type) "scatter"))
   v4_l64)))


(def
 v11_l81
 (book-utils/md
  "To inspect it, let us use [Kindly](https://scicloj.github.io/kindly-noted/) to request\nthat this template would rather be pretty-printed as a data structure."))


(def
 v12_l83
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 10})
  kind/pprint))


(def
 v13_l90
 (book-utils/md
  "\nFor now, you are not supposed to make sense of this data representation.\nAs a user, you usually do not need to think about it.\n\nIf you wish to see the actual Plotly.js specification, you can use\nthe `plot` function:\n"))


(def
 v14_l97
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 10})
  plotly/plot
  kind/pprint))


(def
 v15_l105
 (book-utils/md
  "We may also inspect it with [Portal](https://github.com/djblue/portal):"))


(def
 v16_l107
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 10})
  plotly/plot
  kind/portal))


(def
 v17_l115
 (book-utils/md
  "\nThis is useful for debugging, and also when one wishes to edit the Plotly.js\nspec directly.\n\nIn the remainder of this chapter, we will offer a detailed reference to the API\nfunctions, the way layers are defined, the substitution keys, and the relationships\namong them.\n\n## Debugging 🐛\n\nThroughout this notebook, we will sometimes use the `debug` function that\nallows one to look into the value of a given substitution key in a given\ncontext.\n\nFor example, here we learn about the `:=background`\nkey for background color. In this example,\nwe kept it grey, which is its default."))


(def
 v18_l133
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 10})
  (plotly/debug :=background)))


(def
 v19_l140
 (book-utils/md
  "\n## Raw Plotly specifications ✏\n\nBefore beginning the exploration of Tableplot's Plotly API, let us remember we may\nalso use the raw format supported by Plotly.js.\nWe simply use plain Clojuree data structures to represent the JSON format expected by Plotly.js\nand annotate it as `kind/plotly`, so that our Clojure tooling knows to treat it as such\nwhen displaying it.\n\nFor example, let us reproduce one of its [Bubble Chart Examples](https://plotly.com/javascript/bubble-charts/).\n\nThe original Javascript code looks like this:\n```js\nvar trace1 = {\n              x: [1, 2, 3, 4],\n              y: [10, 11, 12, 13],\n              text: ['A<br>size: 40', 'B<br>size: 60', 'C<br>size: 80', 'D<br>size: 100'],\n              mode: 'markers',\n              marker: {\n                       color: ['rgb(93, 164, 214)', 'rgb(255, 144, 14)',  'rgb(44, 160, 101)', 'rgb(255, 65, 54)'],\n                       size: [40, 60, 80, 100]\n                       }\n              };\n\nvar data = [trace1];\n\nvar layout = {\n              title: {\n                      text: 'Bubble Chart Hover Text'\n                      },\n              showlegend: false,\n              height: 600,\n              width: 600\n              };\n\nPlotly.newPlot('myDiv', data, layout);\n\n```\n\nHere is how we represent that in Clojure:\n"))


(def
 v20_l181
 (plotly/plot
  {:data
   [{:x [1 2 3 4],
     :y [10 11 12 13],
     :text
     ["A<br>size: 40"
      "B<br>size: 60"
      "C<br>size: 80"
      "D<br>size: 100"],
     :mode :markers,
     :marker
     {:color
      ["rgb(93, 164, 214)"
       "rgb(255, 144, 14)"
       "rgb(44, 160, 101)"
       "rgb(255, 65, 54)"],
      :size [40 60 80 100]}}],
   :layout
   {:title {:text "Bubble Chart Hover Text"},
    :showlegend false,
    :height 600,
    :width 600}}))


(def
 v21_l193
 (book-utils/md
  "\nSometimes, this raw way is all we need; but in common situations, Tableplot makes things easier.\n\n## Concepts 💡\n\n### Plotly.js traces\n\nTraces are a core concept in Plotly.js.\nThey specify separate parts of the plots which can be drawn on the same canvas\nbut may vary in their visual nature.\n\nFor example, here is a raw Plotly.js spec with two traces.\n"))


(def
 v22_l206
 (plotly/plot
  {:data
   [{:x [1 2 3 4],
     :y [10 15 12 13],
     :color "blue",
     :mode :markers,
     :marker
     {:size [40 60 80 100], :color ["blue" "brown" "red" "green"]}}
    {:x [1 2 3 4],
     :y [15 21 17 18],
     :mode :markers,
     :color "grey",
     :marker {:size 50, :color "grey"}}]}))


(def
 v23_l220
 (book-utils/md
  "\nIn Tableplot, we often do not need to think about traces, as they are drawn for us.\nBut it is helpful to know about them if we wish to understand the specs generated by Tableplot.\n\n### Layers\n\nLayers are a higher-level concept. We introduce them in Tableplot following\n[ggplot2](https://ggplot2.tidyverse.org/)'s\n[layered grammar of graphics](https://vita.had.co.nz/papers/layered-grammar.html).\nPlotly bindings in other languages have similar concepts.\n\nLike traces, layers are also parts of the plot that can be drawn on the same canvas,\nbut they are a slightly higher-level concept, that makes it easier to bind our data to parts of the plot.\n\nLayers are themselves templates, so they can have their own substitutions.\n\nFor example:"))


(def
 v24_l237
 (->
  (rdatasets/datasets-iris)
  (tc/random 10 {:seed 1})
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 20})
  (plotly/layer-text {:=text :species})))


(def
 v25_l245
 (book-utils/md
  "\nThis plot has **two layers**: one for points, and one for text (which is visible on hover).\n\nLet us see that using `debug`:\n"))


(def
 v26_l250
 (->
  (rdatasets/datasets-iris)
  (tc/random 10 {:seed 1})
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 20})
  (plotly/layer-text {:=text :species})
  (plotly/debug :=layers)
  kind/pprint))


(def
 v27_l260
 (book-utils/md
  "\nYou see, a layer is an intermediate data representation of Tableplot\nthat takes care of the details necessary to generate traces.\n\nIn our example, the two layers are realized as **four traces**: since the point layer is colored\nby species, it is realized as three traces.\n\nLet us see that using `debug`:\n"))


(def
 v28_l269
 (->
  (rdatasets/datasets-iris)
  (tc/random 10 {:seed 1})
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 20})
  (plotly/layer-text {:=text :species})
  (plotly/debug :=traces)
  kind/pprint))


(def
 v29_l279
 (book-utils/md
  "\n### Mark\n\nMark is a Tableplot notion that is used to distinguish different types of layers,\ne.g. `point-layer` vs `line-layer`.\nIt is similar to the ggplot notion of 'geom'.\n\nIts possible values are:"))


(def v30_l288 [:point :text :line :box :violin :bar :segment])


(def
 v31_l290
 (book-utils/md
  "\nHere, `:box` means [boxplot](https://en.wikipedia.org/wiki/Box_plot),\nand `:violin` means [Violin plot](https://en.wikipedia.org/wiki/Violin_plot).\n\n### Coordinates\n\nCoordinates are a Tableplot notion that defines\nhow marks are eventually mapped over to the cranvas,\nsimilar to ggplot's notion.\n\nWe currently support the following:"))


(def v32_l302 [:2d :3d :polar :geo])


(def
 v33_l304
 (book-utils/md
  "Here, 2d and 3d mean Eucledian coordinates of the corresponding dimensions,\nand `:geo` means latitude and longitude.\n\nFor example:\n\n"))


(def
 v35_l315
 (->
  {:lat [45.5 43.4 49.13 51.1 53.34 45.24 44.64 48.25 49.89 50.45],
   :lon
   [-73.57
    -79.24
    -123.06
    -114.1
    -113.28
    -75.43
    -63.57
    -123.21
    -97.13
    -104.6],
   :text
   ["Montreal"
    "Toronto"
    "Vancouver"
    "Calgary"
    "Edmonton"
    "Ottawa"
    "Halifax"
    "Victoria"
    "Winnepeg"
    "Regina"]}
  tc/dataset
  (plotly/base {:=coordinates :geo, :=lat :lat, :=lon :lon})
  (plotly/layer-point
   {:=mark-opacity 0.8,
    :=mark-color
    ["#bebada"
     "#fdb462"
     "#fb8072"
     "#d9d9d9"
     "#bc80bd"
     "#b3de69"
     "#8dd3c7"
     "#80b1d3"
     "#fccde5"
     "#ffffb3"],
    :=mark-size 20,
    :=name "Canadian cities"})
  (plotly/layer-text
   {:=text :text, :=textfont {:size 7, :color :purple}})
  plotly/plot
  (assoc-in
   [:layout :geo]
   {:scope "north america",
    :resolution 10,
    :lonaxis {:range [-130 -55]},
    :lataxis {:range [40 60]},
    :countrywidth 1.5,
    :showland true,
    :showlakes true,
    :showrivers true})))


(def
 v37_l346
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :petal-width,
    :=coordinates :3d})))


(deftest
 t39_l354
 (is
  ((fn*
    [p1__103416#]
    (=
     (->
      p1__103416#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=coordinates)
     :3d))
   v37_l346)))


(def
 v40_l356
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :species,
    :=coordinates :3d})))


(def
 v42_l367
 (def
  rain-data
  (tc/dataset
   {:month
    [:Jan :Feb :Mar :Apr :May :Jun :Jul :Aug :Sep :Oct :Nov :Dec],
    :rain (repeatedly (fn* [] (rand-int 200)))})))


(def
 v43_l374
 (->
  rain-data
  (plotly/layer-bar
   {:=r :rain,
    :=theta :month,
    :=coordinates :polar,
    :=mark-size 20,
    :=mark-opacity 0.6})))


(deftest
 t45_l383
 (is
  ((fn*
    [p1__103417#]
    (=
     (->
      p1__103417#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=coordinates)
     :polar))
   v43_l374)))


(def
 v47_l388
 (->
  rain-data
  (plotly/base {})
  (plotly/layer-bar
   {:=r :rain,
    :=theta :month,
    :=coordinates :polar,
    :=mark-size 20,
    :=mark-opacity 0.6})
  plotly/plot
  (assoc-in
   [:layout :polar]
   {:angularaxis
    {:tickfont {:size 16},
     :rotation 90,
     :direction "counterclockwise"},
    :sector [0 180]})))


(def
 v49_l406
 (let
  [n 50]
  (->
   {:r (->> (repeatedly n (fn* [] (- (rand) 0.5))) (reductions +)),
    :theta
    (->>
     (repeatedly n (fn* [] (* 10 (rand))))
     (reductions +)
     (map (fn* [p1__103418#] (rem p1__103418# 360)))),
    :color (range n)}
   tc/dataset
   (plotly/layer-point
    {:=r :r,
     :=theta :theta,
     :=coordinates :polar,
     :=mark-size 10,
     :=mark-opacity 0.6})
   (plotly/layer-line
    {:=r :r,
     :=theta :theta,
     :=coordinates :polar,
     :=mark-size 3,
     :=mark-opacity 0.6}))))


(def
 v50_l427
 (book-utils/md
  "\n\n### Plotly.js mode and type  \n\nMode and type are Plotly.js notions that are used to distinguish\ndiffetent types of traces.\n\nCombinations of Tableplot's mark and coordinates\nare mapped onto combinations of Plotly.js mode and type,\nbut currently we do not use all the meaningful combinations\nsupported by Plotly.js.\n\nMode is derived from mark as follows:\n"))


(def
 v51_l441
 (->
  {:mark [:point :text :line :box :bar :segment :heatmap :surface]}
  tc/dataset
  (tc/map-columns :mode [:mark] plotly/mark->mode)))


(def
 v52_l446
 (book-utils/md
  "\nType is defined as the concatenation of a mark-based string:\n(`\"box\"`,`\"violin\"`,`\"bar\"`,`\"heatmap\"`,`\"surface\"` if that is the mark,\nand `\"scatter\"` otherwise)\nwith a coordinates-based string\n(`\"3d\"`, `\"polar\"`, or `\"geo\"` if we have such coordinates, `nil` otherwise).\n\nThus, for example, if the mark is `:point` and the coordinates are `:polar`,\nthen the type is `\"scatterpolar\"`.\n\n### Field types\n\nLooking into the data in the columns, we may classify them into the following types:\n\n- `:quantitative` - numerical columns\n- `:temporal` - date-time columns\n- `:nominal` - all other column types (e.g., Strings, keywords)\n\nIn certain situations, the types of data in relevant columns determines the way things\nshould be plotted.\n\nFor example, when a column is used for coloring a plot, we should use gradient colors\nwhen it is quantitative but more distinct colors when it is nominal.\n\nColorin gby a nominal column:\n"))


(def
 v53_l472
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 20})))


(def v54_l479 (book-utils/md "\nColoring by a Quantitative column:\n"))


(def
 v55_l482
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 20})))


(def
 v56_l489
 (book-utils/md
  "\nOverriding a quantitative column to be considered nominal by the `:=color-type` key:\n"))


(def
 v57_l492
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=color-type :nominal,
    :=mark-size 20})))


(deftest
 t59_l501
 (is
  ((fn*
    [p1__103419#]
    (=
     (->
      p1__103419#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=color-type)
     :nominal))
   v57_l492)))


(def
 v60_l503
 (book-utils/md
  "\n### Stat\n\nA stat is a statistical transformation that takes the substitution context\nand returns a new dataset.\nStats such as `smooth-stat`, `histogram-stat`, and `density-stat` are used in a few of the layer functions, such as\n`layer-smooth`, `layer-histogram`, and `layer-density`.\n\nThe user will typically not need to think about them, but they\nare a useful concept in extending Tableplot.\n\n## API functions ⚙\n"))


(def v61_l517 (book-utils/include-fnvar-as-section #'plotly/base))


(def
 v62_l519
 (book-utils/md "#### For example\n\nSetting layout options:\n"))


(def
 v63_l524
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=background "floralwhite", :=height 300, :=width 400})
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=color :species,
    :=mark-size 10,
    :=mark-opacity 0.6})))


(deftest
 t65_l535
 (is
  ((fn*
    [p1__103420#]
    (let
     [layout (-> p1__103420# plotly/plot :layout)]
     (and
      (= (:plot_bgcolor layout) "floralwhite")
      (= (:height layout) 300)
      (= (:width layout) 400))))
   v63_l524)))


(def
 v66_l540
 (book-utils/md
  "\nSetting properties which are shared between layers."))


(def
 v67_l543
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length, :=mark-size 10})
  (plotly/layer-point
   {:=mark-color "grey", :=mark-size 20, :=mark-opacity 0.3})
  (plotly/layer-point {:=color :species})))


(deftest
 t69_l553
 (is
  ((fn*
    [p1__103421#]
    (=
     (-> p1__103421# :aerial.hanami.templates/defaults :=layers count)
     2))
   v67_l543)))


(deftest
 t71_l556
 (is
  ((fn*
    [p1__103422#]
    (and
     (=
      (-> p1__103422# :aerial.hanami.templates/defaults :=x)
      :sepal-width)
     (=
      (-> p1__103422# :aerial.hanami.templates/defaults :=y)
      :sepal-length)))
   v67_l543)))


(def v72_l559 (book-utils/include-fnvar-as-section #'plotly/layer))


(def
 v73_l561
 (book-utils/md "#### For example\nWe could write someting like:"))


(def
 v74_l564
 (->
  (rdatasets/datasets-iris)
  (plotly/layer
   plotly/layer-base
   {:=mark :point, :=x :sepal-width, :=y :sepal-length})))


(def
 v75_l570
 (book-utils/md
  "\nOf course, this can also be expressed more succinctly using `layer-point`.\n"))


(def
 v76_l573
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point {:=x :sepal-width, :=y :sepal-length})))


(def
 v77_l577
 (book-utils/include-fnvar-as-section #'plotly/layer-point))


(def v78_l579 (book-utils/md "#### For example"))


(def
 v80_l583
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=mark-size 20})))


(deftest
 t82_l590
 (is
  ((fn*
    [p1__103423#]
    (= (-> p1__103423# plotly/plot :data first :marker :size) 20))
   v80_l583)))


(def
 v84_l594
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=mark-symbol :diamond})))


(deftest
 t86_l601
 (is
  ((fn*
    [p1__103424#]
    (=
     (-> p1__103424# plotly/plot :data first :marker :symbol)
     :diamond))
   v84_l594)))


(def
 v88_l605
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=mark-color "darkred"})))


(deftest
 t90_l612
 (is
  ((fn*
    [p1__103425#]
    (=
     (-> p1__103425# plotly/plot :data first :marker :color)
     "darkred"))
   v88_l605)))


(def
 v92_l616
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=mark-opacity 0.5})))


(deftest
 t94_l623
 (is
  ((fn*
    [p1__103426#]
    (= (-> p1__103426# plotly/plot :data first :opacity) 0.5))
   v92_l616)))


(def
 v96_l627
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 20})))


(deftest
 t98_l635
 (is
  ((fn*
    [p1__103427#]
    (vector? (-> p1__103427# plotly/plot :data first :marker :color)))
   v96_l627)))


(def
 v99_l637
 (book-utils/md
  "\nColoring by `:cyl` and overriding `:=colorscale`:\n"))


(def
 v100_l641
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=colorscale :Greens,
    :=mark-size 20})))


(deftest
 t102_l650
 (is
  ((fn*
    [p1__103428#]
    (=
     (-> p1__103428# plotly/plot :data first :marker :colorscale)
     :Greens))
   v100_l641)))


(def
 v104_l654
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=color-type :nominal,
    :=mark-size 20})))


(deftest
 t106_l663
 (is
  ((fn* [p1__103429#] (> (-> p1__103429# plotly/plot :data count) 1))
   v104_l654)))


(def
 v108_l667
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=size :cyl})))


(deftest
 t110_l674
 (is
  ((fn*
    [p1__103430#]
    (vector? (-> p1__103430# plotly/plot :data first :marker :size)))
   v108_l667)))


(def
 v111_l676
 (book-utils/md
  "Determining mark size by `:cyl` and specifying the `:=size-range`:"))


(def
 v112_l677
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=size :cyl, :=size-range [5 15]})))


(deftest
 t114_l685
 (is
  ((fn*
    [p1__103431#]
    (let
     [sizes (-> p1__103431# plotly/plot :data first :marker :size)]
     (and (vector? sizes) (every? (fn [s] (<= 5 s 15)) sizes))))
   v112_l677)))


(def
 v115_l689
 (book-utils/md
  "Determining mark size by `:cyl`, and marking `:=size-type` as `:nominal`: "))


(def
 v116_l692
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=size :cyl, :=size-type :nominal})))


(def
 v118_l701
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=symbol :cyl,
    :=mark-size 20,
    :=mark-color "darkred"})))


(deftest
 t120_l710
 (is
  ((fn*
    [p1__103432#]
    (some? (-> p1__103432# plotly/plot :data first :marker :symbol)))
   v118_l701)))


(def
 v122_l714
 (->
  {:x (range 29), :y (reductions + (repeatedly 29 rand))}
  tc/dataset
  plotly/layer-point))


(def
 v124_l720
 (->
  {"x" [1 2 3 4], "y" [1 4 9 16], "z" [:A :B :A :B], "w" [:C :C :D :D]}
  tc/dataset
  (plotly/layer-point {:=x "x", :=y "y", :=color "z", :=size "w"})))


(def
 v126_l731
 (->
  {"x" [1 2 3 4], "y" [1 4 9 16], "z" [:A :B :A :B], "w" [:C :C :D :D]}
  tc/dataset
  (plotly/layer-point
   {:=x "x", :=y "y", :=color "z", :=symbol "w", :=mark-size 20})))


(def
 v128_l744
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :petal-width,
    :=coordinates :3d})))


(deftest
 t130_l752
 (is
  ((fn*
    [p1__103433#]
    (= (-> p1__103433# plotly/plot :data first :type) "scatter3d"))
   v128_l744)))


(def
 v131_l754
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :species,
    :=coordinates :3d})))


(def
 v132_l761
 (book-utils/include-fnvar-as-section #'plotly/layer-line))


(def v133_l763 (book-utils/md "#### For example"))


(def
 v134_l765
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__103434#] (-> p1__103434# :variable (= "unemploy"))))
  (plotly/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(deftest
 t136_l773
 (is
  ((fn*
    [p1__103435#]
    (= (-> p1__103435# plotly/plot :data first :mode) :lines))
   v134_l765)))


(deftest
 t138_l776
 (is
  ((fn*
    [p1__103436#]
    (= (-> p1__103436# plotly/plot :data first :line :color) "purple"))
   v134_l765)))


(def v139_l778 (book-utils/include-fnvar-as-section #'plotly/layer-bar))


(def v140_l780 (book-utils/md "#### For example"))


(def
 v141_l782
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__103437#] (-> p1__103437# :disp tcc/sum))})
  (tc/add-column :bar-width 0.5)
  (plotly/layer-bar
   {:=x :cyl, :=bar-width :bar-width, :=y :total-disp})))


(deftest
 t143_l792
 (is
  ((fn*
    [p1__103438#]
    (-> p1__103438# plotly/plot :data first :type (= "bar")))
   v141_l782)))


(deftest
 t145_l795
 (is
  ((fn*
    [p1__103439#]
    (let
     [trace (-> p1__103439# plotly/plot :data first)]
     (and (vector? (:x trace)) (vector? (:y trace)))))
   v141_l782)))


(def
 v146_l799
 (book-utils/include-fnvar-as-section #'plotly/layer-boxplot))


(def v147_l801 (book-utils/md "#### For example"))


(def
 v148_l803
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot {:=x :cyl, :=y :disp})))


(deftest
 t150_l809
 (is
  ((fn*
    [p1__103440#]
    (= (-> p1__103440# plotly/plot :data first :type) "box"))
   v148_l803)))


(deftest
 t152_l812
 (is
  ((fn*
    [p1__103441#]
    (let
     [trace (-> p1__103441# plotly/plot :data first)]
     (and (some? (:x trace)) (some? (:y trace)))))
   v148_l803)))


(def
 v153_l816
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot
   {:=x :cyl, :=y :disp, :=color :am, :=color-type :nominal})))


(deftest
 t155_l824
 (is
  ((fn* [p1__103442#] (> (-> p1__103442# plotly/plot :data count) 1))
   v153_l816)))


(def
 v156_l826
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot
   {:=x :cyl,
    :=y :disp,
    :=color :am,
    :=color-type :nominal,
    :=boxmode :group})))


(def
 v157_l834
 (book-utils/include-fnvar-as-section #'plotly/layer-violin))


(def v158_l836 (book-utils/md "#### For example"))


(def
 v159_l838
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp})))


(deftest
 t161_l844
 (is
  ((fn*
    [p1__103443#]
    (= (-> p1__103443# plotly/plot :data first :type) "violin"))
   v159_l838)))


(def
 v162_l846
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp, :=box-visible true})))


(deftest
 t164_l853
 (is
  ((fn*
    [p1__103444#]
    (=
     (->
      p1__103444#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=box-visible)
     true))
   v162_l846)))


(def
 v165_l855
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp, :=meanline-visible true})))


(deftest
 t167_l862
 (is
  ((fn*
    [p1__103445#]
    (=
     (->
      p1__103445#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=meanline-visible)
     true))
   v165_l855)))


(def
 v168_l864
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin
   {:=x :cyl, :=y :disp, :=color :am, :=color-type :nominal})))


(def
 v169_l871
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin
   {:=x :cyl,
    :=y :disp,
    :=color :am,
    :=color-type :nominal,
    :=violinmode :group})))


(deftest
 t171_l880
 (is
  ((fn*
    [p1__103446#]
    (=
     (->
      p1__103446#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=violinmode)
     :group))
   v169_l871)))


(def
 v172_l882
 (book-utils/include-fnvar-as-section #'plotly/layer-segment))


(def v173_l884 (book-utils/md "#### For example"))


(def
 v174_l886
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-segment
   {:=x0 :sepal-width,
    :=y0 :sepal-length,
    :=x1 :petal-width,
    :=y1 :petal-length,
    :=mark-opacity 0.4,
    :=mark-size 3,
    :=color :species})))


(deftest
 t176_l897
 (is
  ((fn*
    [p1__103447#]
    (and
     (= (-> p1__103447# plotly/plot :data first :type) "scatter")
     (= (-> p1__103447# plotly/plot :data first :mode) :lines)))
   v174_l886)))


(def
 v177_l900
 (book-utils/include-fnvar-as-section #'plotly/layer-text))


(def v178_l902 (book-utils/md "#### For example"))


(def
 v179_l904
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-text
   {:=x :mpg,
    :=y :disp,
    :=text :cyl,
    :=textfont
    {:family "Courier New, monospace", :size 16, :color :purple},
    :=mark-size 20})))


(deftest
 t181_l915
 (is
  ((fn*
    [p1__103448#]
    (= (-> p1__103448# plotly/plot :data first :mode) :text))
   v179_l904)))


(deftest
 t183_l918
 (is
  ((fn*
    [p1__103449#]
    (=
     (-> p1__103449# plotly/plot :data first :textfont :color)
     :purple))
   v179_l904)))


(def
 v184_l920
 (book-utils/include-fnvar-as-section #'plotly/layer-histogram))


(def v185_l922 (book-utils/md "#### Examples:"))


(def
 v186_l924
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width})))


(deftest
 t188_l928
 (is
  ((fn*
    [p1__103450#]
    (= (-> p1__103450# plotly/plot :data first :type) "bar"))
   v186_l924)))


(deftest
 t190_l931
 (is
  ((fn*
    [p1__103451#]
    (let
     [trace (-> p1__103451# plotly/plot :data first)]
     (and
      (vector? (:x trace))
      (vector? (:y trace))
      (pos? (count (:x trace))))))
   v186_l924)))


(def
 v191_l936
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width, :=histogram-nbins 30})))


(deftest
 t193_l941
 (is
  ((fn*
    [p1__103452#]
    (=
     (->
      p1__103452#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=histogram-nbins)
     30))
   v191_l936)))


(deftest
 t195_l944
 (is
  ((fn*
    [p1__103453#]
    (let
     [trace
      (-> p1__103453# plotly/plot :data first)
      nbins
      (count (:x trace))]
     (<= 25 nbins 35)))
   v191_l936)))


(def
 v196_l948
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram
   {:=x :sepal-width, :=color :species, :=mark-opacity 0.5})))


(deftest
 t198_l954
 (is
  ((fn* [p1__103454#] (= (-> p1__103454# plotly/plot :data count) 3))
   v196_l948)))


(def
 v199_l956
 (book-utils/include-fnvar-as-section #'plotly/layer-histogram2d))


(def v200_l958 (book-utils/md "(experimental)"))


(def
 v201_l960
 (book-utils/md
  "#### Examples:\nCurrently, the number of bins is determined by `:histogram-nbins`.\nWe are exploring various rules of thumbs to determine it automatically.\n"))


(def
 v202_l965
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d {:=x :sepal-width, :=y :sepal-length})))


(deftest
 t204_l970
 (is
  ((fn*
    [p1__103455#]
    (= (-> p1__103455# plotly/plot :data first :type) "heatmap"))
   v202_l965)))


(def
 v205_l972
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d
   {:=x :sepal-width, :=y :sepal-length, :=colorscale :Portland})))


(def
 v206_l977
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d
   {:=x :sepal-width, :=y :sepal-length, :=histogram-nbins 100})))


(def
 v207_l982
 (let
  [n 10000]
  (->
   {:x (repeatedly n rand)}
   tc/dataset
   (tc/add-column
    :y
    (fn*
     [p1__103456#]
     (tcc/* (repeatedly n rand) (:x p1__103456#) (:x p1__103456#))))
   (plotly/layer-histogram2d {:=histogram-nbins 250}))))


(def
 v208_l990
 (book-utils/include-fnvar-as-section #'plotly/layer-density))


(def v209_l992 (book-utils/md "#### Examples:"))


(def
 v210_l994
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width})))


(deftest
 t212_l998
 (is
  ((fn*
    [p1__103457#]
    (let
     [trace (-> p1__103457# plotly/plot :data first)]
     (and (= (:mode trace) :lines) (= (:fill trace) :tozeroy))))
   v210_l994)))


(deftest
 t214_l1003
 (is
  ((fn*
    [p1__103458#]
    (let
     [trace (-> p1__103458# plotly/plot :data first)]
     (and
      (vector? (:x trace))
      (vector? (:y trace))
      (> (count (:x trace)) 50))))
   v210_l994)))


(deftest
 t216_l1009
 (is
  ((fn*
    [p1__103459#]
    (let
     [trace (-> p1__103459# plotly/plot :data first)]
     (every? (fn [y] (>= y 0)) (:y trace))))
   v210_l994)))


(def
 v217_l1012
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 0.05})))


(deftest
 t219_l1017
 (is
  ((fn*
    [p1__103460#]
    (=
     (->
      p1__103460#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=density-bandwidth)
     0.05))
   v217_l1012)))


(def
 v220_l1019
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 1})))


(def
 v221_l1023
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=color :species})))


(deftest
 t223_l1028
 (is
  ((fn* [p1__103461#] (= (-> p1__103461# plotly/plot :data count) 3))
   v221_l1023)))


(def
 v225_l1033
 (-> {:x (repeatedly 9999 rand)} tc/dataset plotly/layer-density))


(def
 v226_l1037
 (book-utils/include-fnvar-as-section #'plotly/layer-smooth))


(def v227_l1039 (book-utils/md "#### Examples:"))


(def
 v228_l1041
 (book-utils/md "Simple linear regression of `:=y` by `:=x`:"))


(def
 v229_l1043
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth {:=mark-color "orange", :=name "Predicted"})))


(deftest
 t231_l1052
 (is
  ((fn*
    [p1__103462#]
    (=
     (-> p1__103462# :aerial.hanami.templates/defaults :=layers count)
     2))
   v229_l1043)))


(deftest
 t233_l1055
 (is
  ((fn* [p1__103463#] (= (-> p1__103463# plotly/plot :data count) 2))
   v229_l1043)))


(deftest
 t235_l1058
 (is
  ((fn*
    [p1__103464#]
    (let
     [traces (-> p1__103464# plotly/plot :data)]
     (and
      (= (:name (first traces)) "Actual")
      (= (:name (second traces)) "Predicted"))))
   v229_l1043)))


(deftest
 t237_l1063
 (is
  ((fn*
    [p1__103465#]
    (let
     [smooth-trace (-> p1__103465# plotly/plot :data second)]
     (and
      (vector? (:x smooth-trace))
      (vector? (:y smooth-trace))
      (pos? (count (:y smooth-trace))))))
   v229_l1043)))


(deftest
 t239_l1069
 (is
  ((fn*
    [p1__103466#]
    (=
     (-> p1__103466# plotly/plot :data second :line :color)
     "orange"))
   v229_l1043)))


(def
 v240_l1071
 (book-utils/md
  "Multiple linear regression of `:=y` by `:=predictors`:"))


(def
 v241_l1073
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=predictors [:petal-width :petal-length],
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(deftest
 t243_l1084
 (is
  ((fn*
    [p1__103467#]
    (=
     (->
      p1__103467#
      :aerial.hanami.templates/defaults
      :=layers
      second
      :aerial.hanami.templates/defaults
      :=predictors)
     [:petal-width :petal-length]))
   v241_l1073)))


(def
 v244_l1087
 (book-utils/md "Polynomial regression of `:=y` by `:=design-matrix`:"))


(def
 v245_l1089
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=design-matrix
    [[:sepal-width '(identity :sepal-width)]
     [:sepal-width-2 '(* :sepal-width :sepal-width)]],
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(deftest
 t247_l1101
 (is
  ((fn*
    [p1__103468#]
    (not
     (nil?
      (->
       p1__103468#
       :aerial.hanami.templates/defaults
       :=layers
       second
       :aerial.hanami.templates/defaults
       :=design-matrix))))
   v245_l1089)))


(def
 v248_l1103
 (book-utils/md "Custom regression defined by `:=model-options`:"))


(def v249_l1105 (require 'scicloj.ml.tribuo))


(def
 v250_l1107
 (def
  regression-tree-options
  {:model-type :scicloj.ml.tribuo/regression,
   :tribuo-components
   [{:name "cart",
     :type "org.tribuo.regression.rtree.CARTRegressionTrainer",
     :properties
     {:maxDepth "8",
      :fractionFeaturesInSplit "1.0",
      :seed "12345",
      :impurity "mse"}}
    {:name "mse",
     :type "org.tribuo.regression.rtree.impurity.MeanSquaredError"}],
   :tribuo-trainer-name "cart"}))


(def
 v251_l1119
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=model-options regression-tree-options,
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def
 v252_l1128
 (book-utils/md
  "Grouped regression where `:=inferred-group` is influenced by `:color`\nsince `:=color-type` is `:nominal`:"))


(def
 v253_l1131
 (->
  (rdatasets/datasets-iris)
  (plotly/base
   {:=title "dummy",
    :=color :species,
    :=x :sepal-width,
    :=y :sepal-length})
  plotly/layer-point
  plotly/layer-smooth))


(def
 v254_l1139
 (book-utils/md
  "Regression where grouping is avoiding using through `:=group`:"))


(def
 v255_l1141
 (->
  (rdatasets/datasets-iris)
  (plotly/base
   {:=title "dummy",
    :=color :species,
    :=group [],
    :=x :sepal-width,
    :=y :sepal-length})
  plotly/layer-point
  (plotly/layer-smooth {:=mark-color "red"})))


(deftest
 t257_l1151
 (is
  ((fn*
    [p1__103469#]
    (= (-> p1__103469# :aerial.hanami.templates/defaults :=group) []))
   v255_l1141)))


(def
 v258_l1153
 (book-utils/md
  "An simpler way to achieve this -- the color is only defined for the point layer:"))


(def
 v259_l1155
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=title "dummy", :=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=color :species})
  plotly/layer-smooth))


(def
 v260_l1162
 (book-utils/include-fnvar-as-section #'plotly/layer-heatmap))


(def v261_l1164 (book-utils/md "#### For example"))


(def
 v263_l1170
 (->
  {:x (range 100),
   :y (range 200),
   :z
   (for
    [i (range 200)]
    (for
     [j (range 100)]
     (+ (* (math/sin (/ i 5)) i) (* (math/sin (/ j 5)) j))))}
  tc/dataset
  plotly/layer-heatmap))


(deftest
 t265_l1180
 (is
  ((fn*
    [p1__103470#]
    (= (-> p1__103470# plotly/plot :data first :type) "heatmap"))
   v263_l1170)))


(deftest
 t267_l1183
 (is
  ((fn*
    [p1__103471#]
    (let
     [trace (-> p1__103471# plotly/plot :data first)]
     (and
      (vector? (:x trace))
      (vector? (:y trace))
      (vector? (:z trace)))))
   v263_l1170)))


(deftest
 t269_l1189
 (is
  ((fn*
    [p1__103472#]
    (let
     [z (-> p1__103472# plotly/plot :data first :z)]
     (and (vector? z) (seq? (first z)))))
   v263_l1170)))


(def
 v271_l1195
 (->
  {:x [:A :B],
   :y (range 3),
   :z (for [i (range 3)] (for [j (range 2)] (+ i j)))}
  tc/dataset
  plotly/layer-heatmap))


(def
 v273_l1205
 (->
  {:x [:A :B],
   :y (range 3),
   :z (for [i (range 3)] (for [j (range 2)] (+ i j)))}
  tc/dataset
  (plotly/layer-heatmap {:=zmin 0, :=zmax 5})))


(def
 v275_l1217
 (->
  {:site [:A :B],
   :time (range 3),
   :temperature (for [i (range 3)] (for [j (range 2)] (+ i j)))}
  tc/dataset
  (plotly/layer-heatmap {:=x :site, :=y :time, :=z :temperature})))


(def
 v277_l1229
 (->
  {:x (range 100),
   :y (range 200),
   :z
   (for
    [i (range 200)]
    (for
     [j (range 100)]
     (+ (* (math/sin (/ i 5)) i) (* (math/sin (/ j 5)) j))))}
  tc/dataset
  (plotly/layer-heatmap {:=colorscale :Greys})))


(def
 v278_l1238
 (book-utils/include-fnvar-as-section #'plotly/layer-correlation))


(def v279_l1240 (book-utils/md "#### For example"))


(def
 v281_l1244
 (let
  [n 99]
  (->
   {:u (repeatedly n rand),
    :v (repeatedly n rand),
    :w (repeatedly n rand)}
   tc/dataset
   (tc/add-columns
    {:x (fn* [p1__103473#] (tcc/+ (:u p1__103473#) (:v p1__103473#))),
     :y
     (fn*
      [p1__103474#]
      (tcc/-
       (:w p1__103474#)
       (tcc/+ (:u p1__103474#) (:v p1__103474#))))})
   plotly/layer-correlation)))


(deftest
 t283_l1254
 (is
  ((fn*
    [p1__103475#]
    (= (-> p1__103475# plotly/plot :data first :type) "heatmap"))
   v281_l1244)))


(deftest
 t285_l1257
 (is
  ((fn*
    [p1__103476#]
    (let
     [trace (-> p1__103476# plotly/plot :data first) z (:z trace)]
     (= (count z) (count (first z)))))
   v281_l1244)))


(deftest
 t287_l1262
 (is
  ((fn*
    [p1__103477#]
    (let
     [z
      (-> p1__103477# plotly/plot :data first :z)
      all-vals
      (flatten z)]
     (every? (fn [v] (<= -1 v 1)) all-vals)))
   v281_l1244)))


(def
 v289_l1270
 (let
  [n 99]
  (->
   {:u (repeatedly n rand),
    :v (repeatedly n rand),
    :w (repeatedly n rand)}
   tc/dataset
   (tc/add-columns
    {:x (fn* [p1__103478#] (tcc/+ (:u p1__103478#) (:v p1__103478#)))})
   (plotly/layer-correlation {:=zmin 0, :=zmax 1, :=colorscale :hot}))))


(def
 v291_l1286
 (let
  [autoregression
   (->>
    (repeatedly 1000 rand)
    (reductions (fn [x noise] (+ (* 0.8 x) (* 0.2 noise)))))
   shifts-dataset
   (->>
    (for
     [shift (range 50)]
     [(str "shift" shift) (drop shift autoregression)])
    (apply concat)
    (apply array-map)
    tc/dataset)]
  (->
   shifts-dataset
   (plotly/layer-correlation {:=zmin 0, :=zmax 1, :=colorscale :hot}))))


(def
 v292_l1301
 (book-utils/include-fnvar-as-section #'plotly/layer-surface))


(def v293_l1303 (book-utils/md "#### For example"))


(def
 v294_l1305
 (->
  {:z
   (for
    [i (range 100)]
    (for
     [j (range 100)]
     (->
      (tcc/- [i j] [30 60])
      (tcc// [20 50])
      tcc/sq
      tcc/sum
      -
      math/exp)))}
  tc/dataset
  plotly/layer-surface))


(def
 v296_l1319
 (let
  [xy->z (fn [x y] (-> [y x] tcc/sq tcc/sum - math/exp)) n 30]
  (->
   {:x (repeatedly n rand), :y (repeatedly n rand)}
   tc/dataset
   (tc/map-columns
    :z
    [:x :y]
    (fn [x y] (+ (xy->z x y) (* 0.1 (rand)))))
   (plotly/layer-point {:=coordinates :3d})
   (plotly/layer-surface
    {:=dataset
     (let
      [xs (range 0 1 0.1) ys (range 0 1 0.1)]
      (tc/dataset
       {:x xs, :y ys, :z (for [y ys] (for [x xs] (xy->z x y)))})),
     :=mark-opacity 0.5}))))


(def v297_l1345 (book-utils/include-fnvar-as-section #'plotly/surface))


(def v298_l1347 (book-utils/md "#### For example"))


(def
 v299_l1349
 (plotly/surface
  (for
   [i (range 100)]
   (for
    [j (range 100)]
    (->
     (tcc/- [i j] [30 60])
     (tcc// [20 50])
     tcc/sq
     tcc/sum
     -
     math/exp)))))


(deftest
 t301_l1361
 (is
  ((fn*
    [p1__103479#]
    (= (-> p1__103479# plotly/plot :data first :type) :surface))
   v299_l1349)))


(deftest
 t303_l1364
 (is
  ((fn*
    [p1__103480#]
    (let
     [z (-> p1__103480# plotly/plot :data first :z)]
     (and (seq? z) (seq? (first z)) (> (count z) 0))))
   v299_l1349)))


(def v304_l1369 (book-utils/include-fnvar-as-section #'plotly/imshow))


(def
 v305_l1371
 (book-utils/md
  "Imshow uses dtype-next's [BufferedImage support](https://cnuernber.github.io/dtype-next/buffered-image.html) to figure out the right order of color channels, etc.\n\nSo, it can handle plain vectors of vectors, dtype next tensors, and actual Java BufferedImage objects."))


(def v306_l1375 (book-utils/md "#### For example"))


(def
 v307_l1377
 (plotly/imshow
  (for
   [i (range 10)]
   (for [j (range 10)] [(* 10 i) (* 10 j) (* 10 (+ i j))]))))


(deftest
 t309_l1386
 (is
  ((fn*
    [p1__103481#]
    (= (-> p1__103481# plotly/plot :data first :type) :image))
   v307_l1377)))


(deftest
 t311_l1389
 (is
  ((fn*
    [p1__103482#]
    (some? (-> p1__103482# plotly/plot :data first :z)))
   v307_l1377)))


(def
 v312_l1391
 (plotly/imshow
  (tensor/compute-tensor
   [100 100 3]
   (fn [i j k] (case k 0 i 1 j 2 (+ i j)))
   :uint8)))


(def
 v313_l1402
 (defonce
  Crab-Nebula-image
  (->
   "https://scicloj.github.io/sci-cloj-logo-transparent.png"
   (java.net.URL.)
   (javax.imageio.ImageIO/read))))


(def v314_l1407 (plotly/imshow Crab-Nebula-image))


(def v315_l1409 (book-utils/include-fnvar-as-section #'plotly/splom))


(def v316_l1411 (book-utils/md "#### For example"))


(def
 v317_l1413
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=height 600,
    :=width 600})))


(deftest
 t319_l1422
 (is
  ((fn*
    [p1__103483#]
    (= (-> p1__103483# plotly/plot :data first :type) :splom))
   v317_l1413)))


(deftest
 t321_l1425
 (is
  ((fn*
    [p1__103484#]
    (let
     [trace
      (-> p1__103484# plotly/plot :data first)
      dims
      (:dimensions trace)]
     (and (seq? dims) (= (count dims) 4))))
   v317_l1413)))


(def
 v322_l1430
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=color :species,
    :=height 600,
    :=width 600})))


(deftest
 t324_l1440
 (is
  ((fn* [p1__103485#] (> (-> p1__103485# plotly/plot :data count) 1))
   v322_l1430)))


(def
 v325_l1442
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=symbol :species,
    :=height 600,
    :=width 600})))


(def
 v327_l1453
 (->
  (rdatasets/datasets-iris)
  (tc/select-columns
   [:sepal-width :sepal-length :petal-width :petal-length])
  (plotly/splom {:=height 600, :=width 600})))


(def v328_l1461 (book-utils/include-fnvar-as-section #'plotly/plot))


(def v329_l1463 (book-utils/md "#### For example"))


(def
 v330_l1464
 (->
  (rdatasets/datasets-iris)
  tc/head
  (plotly/layer-point {:=x :sepal-width, :=y :sepal-length})
  plotly/plot
  kind/pprint))


(def
 v331_l1471
 (book-utils/md
  "\nThis can be useful for editing the plot as a raw Plotly.js specification.\nFor example:\n"))


(def
 v332_l1475
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point {:=x :sepal-width, :=y :sepal-length})
  plotly/plot
  (assoc-in [:layout :plot_bgcolor] "floralwhite")))


(def v333_l1481 (book-utils/include-fnvar-as-section #'plotly/debug))


(def v334_l1483 (book-utils/md "#### For example"))


(def
 v335_l1484
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})))


(def
 v336_l1489
 (book-utils/md
  "Let us verify that `:=background` is deterimined to be grey."))


(def
 v337_l1491
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})
  (plotly/debug :=background)))


(def
 v338_l1497
 (book-utils/md
  "Here, let us verify `:=color-type` for the 0th layer is deterimined to be `:nominal`."))


(def
 v339_l1499
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})
  (plotly/debug 0 :=color-type)))


(def
 v340_l1505
 (book-utils/md
  "Here, let us check both `:=color` and `:=color-type` for the 0th layer."))


(def
 v341_l1507
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})
  (plotly/debug 0 {:color :=color, :color-type :=color-type})))


(def v342_l1514 (book-utils/md "\n## Stats 🖩\n"))


(def
 v343_l1518
 (book-utils/include-dag-fn-as-section
  'histogram-stat
  plotly/histogram-stat))


(def
 v344_l1519
 (book-utils/include-dag-fn-as-section
  'density-stat
  plotly/density-stat))


(def
 v345_l1520
 (book-utils/include-dag-fn-as-section 'smooth-stat plotly/smooth-stat))


(def
 v346_l1521
 (book-utils/include-dag-fn-as-section
  'correlation-stat
  plotly/correlation-stat))


(def v347_l1523 (book-utils/md "\n## Substitution Keys 🔑\n"))


(def
 v348_l1527
 (->>
  plotly/standard-defaults
  (map
   (fn
    [[k v doc]]
    (kind/md
     [(format "### %s" (book-utils/include-key-or-symbol-name k))
      (some->> doc (format "**role:** %s\n"))
      (format
       "**default:** %s\n"
       (cond
        (fn? v)
        (book-utils/include-dag-fn v)
        (= v hc/RMV)
        "`NONE`"
        (keyword? v)
        (book-utils/include-key-or-symbol-name v)
        :else
        (book-utils/include-form v)))])))
  kind/fragment))
