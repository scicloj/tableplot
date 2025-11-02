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
    [p1__82045#]
    (contains?
     (:aerial.hanami.templates/defaults p1__82045#)
     :=dataset))
   v4_l64)))


(deftest
 t8_l74
 (is
  ((fn*
    [p1__82046#]
    (let
     [layer-defaults
      (->
       p1__82046#
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
    [p1__82047#]
    (= (-> p1__82047# plotly/plot :data first :type) "scatter"))
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
    [p1__82048#]
    (=
     (->
      p1__82048#
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
    [p1__82049#]
    (=
     (->
      p1__82049#
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
     (map (fn* [p1__82050#] (rem p1__82050# 360)))),
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
    [p1__82051#]
    (=
     (->
      p1__82051#
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
    [p1__82052#]
    (let
     [layout (-> p1__82052# plotly/plot :layout)]
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
    [p1__82053#]
    (=
     (-> p1__82053# :aerial.hanami.templates/defaults :=layers count)
     2))
   v67_l543)))


(deftest
 t71_l556
 (is
  ((fn*
    [p1__82054#]
    (and
     (=
      (-> p1__82054# :aerial.hanami.templates/defaults :=x)
      :sepal-width)
     (=
      (-> p1__82054# :aerial.hanami.templates/defaults :=y)
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


(def
 v82_l591
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=mark-symbol :diamond})))


(def
 v84_l599
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=mark-color "darkred"})))


(def
 v86_l607
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=mark-opacity 0.5})))


(def
 v88_l615
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=color :cyl, :=mark-size 20})))


(def
 v89_l622
 (book-utils/md
  "\nColoring by `:cyl` and overriding `:=colorscale`:\n"))


(def
 v90_l626
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=colorscale :Greens,
    :=mark-size 20})))


(def
 v92_l636
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=color :cyl,
    :=color-type :nominal,
    :=mark-size 20})))


(def
 v94_l646
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point {:=x :mpg, :=y :disp, :=size :cyl})))


(def
 v95_l652
 (book-utils/md
  "Determining mark size by `:cyl` and specifying the `:=size-range`:"))


(def
 v96_l653
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=size :cyl, :=size-range [5 15]})))


(def
 v97_l660
 (book-utils/md
  "Determining mark size by `:cyl`, and marking `:=size-type` as `:nominal`: "))


(def
 v98_l663
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg, :=y :disp, :=size :cyl, :=size-type :nominal})))


(def
 v100_l672
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-point
   {:=x :mpg,
    :=y :disp,
    :=symbol :cyl,
    :=mark-size 20,
    :=mark-color "darkred"})))


(def
 v102_l682
 (->
  {:x (range 29), :y (reductions + (repeatedly 29 rand))}
  tc/dataset
  plotly/layer-point))


(def
 v104_l688
 (->
  {"x" [1 2 3 4], "y" [1 4 9 16], "z" [:A :B :A :B], "w" [:C :C :D :D]}
  tc/dataset
  (plotly/layer-point {:=x "x", :=y "y", :=color "z", :=size "w"})))


(def
 v106_l699
 (->
  {"x" [1 2 3 4], "y" [1 4 9 16], "z" [:A :B :A :B], "w" [:C :C :D :D]}
  tc/dataset
  (plotly/layer-point
   {:=x "x", :=y "y", :=color "z", :=symbol "w", :=mark-size 20})))


(def
 v108_l712
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :petal-width,
    :=coordinates :3d})))


(def
 v109_l719
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width,
    :=y :sepal-length,
    :=z :petal-length,
    :=color :species,
    :=coordinates :3d})))


(def
 v110_l726
 (book-utils/include-fnvar-as-section #'plotly/layer-line))


(def v111_l728 (book-utils/md "#### For example"))


(def
 v112_l730
 (->
  (rdatasets/ggplot2-economics_long)
  (tc/select-rows
   (fn* [p1__82055#] (-> p1__82055# :variable (= "unemploy"))))
  (plotly/layer-line {:=x :date, :=y :value, :=mark-color "purple"})))


(deftest
 t114_l738
 (is
  ((fn*
    [p1__82056#]
    (= (-> p1__82056# plotly/plot :data first :mode) :lines))
   v112_l730)))


(def v115_l740 (book-utils/include-fnvar-as-section #'plotly/layer-bar))


(def v116_l742 (book-utils/md "#### For example"))


(def
 v117_l744
 (->
  (rdatasets/datasets-mtcars)
  (tc/group-by [:cyl])
  (tc/aggregate
   {:total-disp (fn* [p1__82057#] (-> p1__82057# :disp tcc/sum))})
  (tc/add-column :bar-width 0.5)
  (plotly/layer-bar
   {:=x :cyl, :=bar-width :bar-width, :=y :total-disp})))


(deftest
 t119_l754
 (is
  ((fn*
    [p1__82058#]
    (-> p1__82058# plotly/plot :data first :type (= "bar")))
   v117_l744)))


(def
 v120_l756
 (book-utils/include-fnvar-as-section #'plotly/layer-boxplot))


(def v121_l758 (book-utils/md "#### For example"))


(def
 v122_l760
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot {:=x :cyl, :=y :disp})))


(deftest
 t124_l766
 (is
  ((fn*
    [p1__82059#]
    (= (-> p1__82059# plotly/plot :data first :type) "box"))
   v122_l760)))


(def
 v125_l768
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot
   {:=x :cyl, :=y :disp, :=color :am, :=color-type :nominal})))


(def
 v126_l775
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-boxplot
   {:=x :cyl,
    :=y :disp,
    :=color :am,
    :=color-type :nominal,
    :=boxmode :group})))


(def
 v127_l783
 (book-utils/include-fnvar-as-section #'plotly/layer-violin))


(def v128_l785 (book-utils/md "#### For example"))


(def
 v129_l787
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp})))


(deftest
 t131_l793
 (is
  ((fn*
    [p1__82060#]
    (= (-> p1__82060# plotly/plot :data first :type) "violin"))
   v129_l787)))


(def
 v132_l795
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp, :=box-visible true})))


(deftest
 t134_l802
 (is
  ((fn*
    [p1__82061#]
    (=
     (->
      p1__82061#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=box-visible)
     true))
   v132_l795)))


(def
 v135_l804
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin {:=x :cyl, :=y :disp, :=meanline-visible true})))


(deftest
 t137_l811
 (is
  ((fn*
    [p1__82062#]
    (=
     (->
      p1__82062#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=meanline-visible)
     true))
   v135_l804)))


(def
 v138_l813
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin
   {:=x :cyl, :=y :disp, :=color :am, :=color-type :nominal})))


(def
 v139_l820
 (->
  (rdatasets/datasets-mtcars)
  (plotly/layer-violin
   {:=x :cyl,
    :=y :disp,
    :=color :am,
    :=color-type :nominal,
    :=violinmode :group})))


(deftest
 t141_l829
 (is
  ((fn*
    [p1__82063#]
    (=
     (->
      p1__82063#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=violinmode)
     :group))
   v139_l820)))


(def
 v142_l831
 (book-utils/include-fnvar-as-section #'plotly/layer-segment))


(def v143_l833 (book-utils/md "#### For example"))


(def
 v144_l835
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
 t146_l846
 (is
  ((fn*
    [p1__82064#]
    (and
     (= (-> p1__82064# plotly/plot :data first :type) "scatter")
     (= (-> p1__82064# plotly/plot :data first :mode) :lines)))
   v144_l835)))


(def
 v147_l849
 (book-utils/include-fnvar-as-section #'plotly/layer-text))


(def v148_l851 (book-utils/md "#### For example"))


(def
 v149_l853
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
 t151_l864
 (is
  ((fn*
    [p1__82065#]
    (= (-> p1__82065# plotly/plot :data first :mode) :text))
   v149_l853)))


(deftest
 t153_l867
 (is
  ((fn*
    [p1__82066#]
    (=
     (-> p1__82066# plotly/plot :data first :textfont :color)
     :purple))
   v149_l853)))


(def
 v154_l869
 (book-utils/include-fnvar-as-section #'plotly/layer-histogram))


(def v155_l871 (book-utils/md "#### Examples:"))


(def
 v156_l873
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width})))


(def
 v157_l876
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram {:=x :sepal-width, :=histogram-nbins 30})))


(deftest
 t159_l881
 (is
  ((fn*
    [p1__82067#]
    (=
     (->
      p1__82067#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=histogram-nbins)
     30))
   v157_l876)))


(def
 v160_l883
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram
   {:=x :sepal-width, :=color :species, :=mark-opacity 0.5})))


(deftest
 t162_l889
 (is
  ((fn* [p1__82068#] (= (-> p1__82068# plotly/plot :data count) 3))
   v160_l883)))


(def
 v163_l891
 (book-utils/include-fnvar-as-section #'plotly/layer-histogram2d))


(def v164_l893 (book-utils/md "(experimental)"))


(def
 v165_l895
 (book-utils/md
  "#### Examples:\nCurrently, the number of bins is determined by `:histogram-nbins`.\nWe are exploring various rules of thumbs to determine it automatically.\n"))


(def
 v166_l900
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d {:=x :sepal-width, :=y :sepal-length})))


(deftest
 t168_l905
 (is
  ((fn*
    [p1__82069#]
    (= (-> p1__82069# plotly/plot :data first :type) "heatmap"))
   v166_l900)))


(def
 v169_l907
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d
   {:=x :sepal-width, :=y :sepal-length, :=colorscale :Portland})))


(def
 v170_l912
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-histogram2d
   {:=x :sepal-width, :=y :sepal-length, :=histogram-nbins 100})))


(def
 v171_l917
 (let
  [n 10000]
  (->
   {:x (repeatedly n rand)}
   tc/dataset
   (tc/add-column
    :y
    (fn*
     [p1__82070#]
     (tcc/* (repeatedly n rand) (:x p1__82070#) (:x p1__82070#))))
   (plotly/layer-histogram2d {:=histogram-nbins 250}))))


(def
 v172_l925
 (book-utils/include-fnvar-as-section #'plotly/layer-density))


(def v173_l927 (book-utils/md "#### Examples:"))


(def
 v174_l929
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width})))


(deftest
 t176_l933
 (is
  ((fn*
    [p1__82071#]
    (let
     [trace (-> p1__82071# plotly/plot :data first)]
     (and (= (:mode trace) :lines) (= (:fill trace) :tozeroy))))
   v174_l929)))


(def
 v177_l937
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 0.05})))


(deftest
 t179_l942
 (is
  ((fn*
    [p1__82072#]
    (=
     (->
      p1__82072#
      :aerial.hanami.templates/defaults
      :=layers
      first
      :aerial.hanami.templates/defaults
      :=density-bandwidth)
     0.05))
   v177_l937)))


(def
 v180_l944
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=density-bandwidth 1})))


(def
 v181_l948
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-density {:=x :sepal-width, :=color :species})))


(deftest
 t183_l953
 (is
  ((fn* [p1__82073#] (= (-> p1__82073# plotly/plot :data count) 3))
   v181_l948)))


(def
 v185_l958
 (-> {:x (repeatedly 9999 rand)} tc/dataset plotly/layer-density))


(def
 v186_l962
 (book-utils/include-fnvar-as-section #'plotly/layer-smooth))


(def v187_l964 (book-utils/md "#### Examples:"))


(def
 v188_l966
 (book-utils/md "Simple linear regression of `:=y` by `:=x`:"))


(def
 v189_l968
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth {:=mark-color "orange", :=name "Predicted"})))


(deftest
 t191_l977
 (is
  ((fn*
    [p1__82074#]
    (=
     (-> p1__82074# :aerial.hanami.templates/defaults :=layers count)
     2))
   v189_l968)))


(deftest
 t193_l980
 (is
  ((fn* [p1__82075#] (= (-> p1__82075# plotly/plot :data count) 2))
   v189_l968)))


(deftest
 t195_l983
 (is
  ((fn*
    [p1__82076#]
    (let
     [traces (-> p1__82076# plotly/plot :data)]
     (and
      (= (:name (first traces)) "Actual")
      (= (:name (second traces)) "Predicted"))))
   v189_l968)))


(def
 v196_l987
 (book-utils/md
  "Multiple linear regression of `:=y` by `:=predictors`:"))


(def
 v197_l989
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=predictors [:petal-width :petal-length],
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(deftest
 t199_l1000
 (is
  ((fn*
    [p1__82077#]
    (=
     (->
      p1__82077#
      :aerial.hanami.templates/defaults
      :=layers
      second
      :aerial.hanami.templates/defaults
      :=predictors)
     [:petal-width :petal-length]))
   v197_l989)))


(def
 v200_l1003
 (book-utils/md "Polynomial regression of `:=y` by `:=design-matrix`:"))


(def
 v201_l1005
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
 t203_l1017
 (is
  ((fn*
    [p1__82078#]
    (not
     (nil?
      (->
       p1__82078#
       :aerial.hanami.templates/defaults
       :=layers
       second
       :aerial.hanami.templates/defaults
       :=design-matrix))))
   v201_l1005)))


(def
 v204_l1019
 (book-utils/md "Custom regression defined by `:=model-options`:"))


(def v205_l1021 (require 'scicloj.ml.tribuo))


(def
 v206_l1023
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
 v207_l1035
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=mark-color "green", :=name "Actual"})
  (plotly/layer-smooth
   {:=model-options regression-tree-options,
    :=mark-opacity 0.5,
    :=name "Predicted"})))


(def
 v208_l1044
 (book-utils/md
  "Grouped regression where `:=inferred-group` is influenced by `:color`\nsince `:=color-type` is `:nominal`:"))


(def
 v209_l1047
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
 v210_l1055
 (book-utils/md
  "Regression where grouping is avoiding using through `:=group`:"))


(def
 v211_l1057
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
 t213_l1067
 (is
  ((fn*
    [p1__82079#]
    (= (-> p1__82079# :aerial.hanami.templates/defaults :=group) []))
   v211_l1057)))


(def
 v214_l1069
 (book-utils/md
  "An simpler way to achieve this -- the color is only defined for the point layer:"))


(def
 v215_l1071
 (->
  (rdatasets/datasets-iris)
  (plotly/base {:=title "dummy", :=x :sepal-width, :=y :sepal-length})
  (plotly/layer-point {:=color :species})
  plotly/layer-smooth))


(def
 v216_l1078
 (book-utils/include-fnvar-as-section #'plotly/layer-heatmap))


(def v217_l1080 (book-utils/md "#### For example"))


(def
 v219_l1086
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
 t221_l1096
 (is
  ((fn*
    [p1__82080#]
    (= (-> p1__82080# plotly/plot :data first :type) "heatmap"))
   v219_l1086)))


(def
 v223_l1100
 (->
  {:x [:A :B],
   :y (range 3),
   :z (for [i (range 3)] (for [j (range 2)] (+ i j)))}
  tc/dataset
  plotly/layer-heatmap))


(def
 v225_l1110
 (->
  {:x [:A :B],
   :y (range 3),
   :z (for [i (range 3)] (for [j (range 2)] (+ i j)))}
  tc/dataset
  (plotly/layer-heatmap {:=zmin 0, :=zmax 5})))


(def
 v227_l1122
 (->
  {:site [:A :B],
   :time (range 3),
   :temperature (for [i (range 3)] (for [j (range 2)] (+ i j)))}
  tc/dataset
  (plotly/layer-heatmap {:=x :site, :=y :time, :=z :temperature})))


(def
 v229_l1134
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
 v230_l1143
 (book-utils/include-fnvar-as-section #'plotly/layer-correlation))


(def v231_l1145 (book-utils/md "#### For example"))


(def
 v233_l1149
 (let
  [n 99]
  (->
   {:u (repeatedly n rand),
    :v (repeatedly n rand),
    :w (repeatedly n rand)}
   tc/dataset
   (tc/add-columns
    {:x (fn* [p1__82081#] (tcc/+ (:u p1__82081#) (:v p1__82081#))),
     :y
     (fn*
      [p1__82082#]
      (tcc/-
       (:w p1__82082#)
       (tcc/+ (:u p1__82082#) (:v p1__82082#))))})
   plotly/layer-correlation)))


(deftest
 t235_l1159
 (is
  ((fn*
    [p1__82083#]
    (= (-> p1__82083# plotly/plot :data first :type) "heatmap"))
   v233_l1149)))


(def
 v237_l1165
 (let
  [n 99]
  (->
   {:u (repeatedly n rand),
    :v (repeatedly n rand),
    :w (repeatedly n rand)}
   tc/dataset
   (tc/add-columns
    {:x (fn* [p1__82084#] (tcc/+ (:u p1__82084#) (:v p1__82084#)))})
   (plotly/layer-correlation {:=zmin 0, :=zmax 1, :=colorscale :hot}))))


(def
 v239_l1181
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
 v240_l1196
 (book-utils/include-fnvar-as-section #'plotly/layer-surface))


(def v241_l1198 (book-utils/md "#### For example"))


(def
 v242_l1200
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
 v244_l1214
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


(def v245_l1240 (book-utils/include-fnvar-as-section #'plotly/surface))


(def v246_l1242 (book-utils/md "#### For example"))


(def
 v247_l1244
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
 t249_l1256
 (is
  ((fn*
    [p1__82085#]
    (= (-> p1__82085# plotly/plot :data first :type) :surface))
   v247_l1244)))


(def v250_l1258 (book-utils/include-fnvar-as-section #'plotly/imshow))


(def
 v251_l1260
 (book-utils/md
  "Imshow uses dtype-next's [BufferedImage support](https://cnuernber.github.io/dtype-next/buffered-image.html) to figure out the right order of color channels, etc.\n\nSo, it can handle plain vectors of vectors, dtype next tensors, and actual Java BufferedImage objects."))


(def v252_l1264 (book-utils/md "#### For example"))


(def
 v253_l1266
 (plotly/imshow
  (for
   [i (range 10)]
   (for [j (range 10)] [(* 10 i) (* 10 j) (* 10 (+ i j))]))))


(deftest
 t255_l1275
 (is
  ((fn*
    [p1__82086#]
    (= (-> p1__82086# plotly/plot :data first :type) :image))
   v253_l1266)))


(def
 v256_l1277
 (plotly/imshow
  (tensor/compute-tensor
   [100 100 3]
   (fn [i j k] (case k 0 i 1 j 2 (+ i j)))
   :uint8)))


(def
 v257_l1288
 (defonce
  Crab-Nebula-image
  (->
   "https://scicloj.github.io/sci-cloj-logo-transparent.png"
   (java.net.URL.)
   (javax.imageio.ImageIO/read))))


(def v258_l1293 (plotly/imshow Crab-Nebula-image))


(def v259_l1295 (book-utils/include-fnvar-as-section #'plotly/splom))


(def v260_l1297 (book-utils/md "#### For example"))


(def
 v261_l1299
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=height 600,
    :=width 600})))


(deftest
 t263_l1308
 (is
  ((fn*
    [p1__82087#]
    (= (-> p1__82087# plotly/plot :data first :type) :splom))
   v261_l1299)))


(def
 v264_l1310
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=color :species,
    :=height 600,
    :=width 600})))


(def
 v265_l1319
 (->
  (rdatasets/datasets-iris)
  (plotly/splom
   {:=colnames [:sepal-width :sepal-length :petal-width :petal-length],
    :=symbol :species,
    :=height 600,
    :=width 600})))


(def
 v267_l1330
 (->
  (rdatasets/datasets-iris)
  (tc/select-columns
   [:sepal-width :sepal-length :petal-width :petal-length])
  (plotly/splom {:=height 600, :=width 600})))


(def v268_l1338 (book-utils/include-fnvar-as-section #'plotly/plot))


(def v269_l1340 (book-utils/md "#### For example"))


(def
 v270_l1341
 (->
  (rdatasets/datasets-iris)
  tc/head
  (plotly/layer-point {:=x :sepal-width, :=y :sepal-length})
  plotly/plot
  kind/pprint))


(def
 v271_l1348
 (book-utils/md
  "\nThis can be useful for editing the plot as a raw Plotly.js specification.\nFor example:\n"))


(def
 v272_l1352
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point {:=x :sepal-width, :=y :sepal-length})
  plotly/plot
  (assoc-in [:layout :plot_bgcolor] "floralwhite")))


(def v273_l1358 (book-utils/include-fnvar-as-section #'plotly/debug))


(def v274_l1360 (book-utils/md "#### For example"))


(def
 v275_l1361
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})))


(def
 v276_l1366
 (book-utils/md
  "Let us verify that `:=background` is deterimined to be grey."))


(def
 v277_l1368
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})
  (plotly/debug :=background)))


(def
 v278_l1374
 (book-utils/md
  "Here, let us verify `:=color-type` for the 0th layer is deterimined to be `:nominal`."))


(def
 v279_l1376
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})
  (plotly/debug 0 :=color-type)))


(def
 v280_l1382
 (book-utils/md
  "Here, let us check both `:=color` and `:=color-type` for the 0th layer."))


(def
 v281_l1384
 (->
  (rdatasets/datasets-iris)
  (plotly/layer-point
   {:=x :sepal-width, :=y :sepal-length, :=color :species})
  (plotly/debug 0 {:color :=color, :color-type :=color-type})))


(def v282_l1391 (book-utils/md "\n## Stats 🖩\n"))


(def
 v283_l1395
 (book-utils/include-dag-fn-as-section
  'histogram-stat
  plotly/histogram-stat))


(def
 v284_l1396
 (book-utils/include-dag-fn-as-section
  'density-stat
  plotly/density-stat))


(def
 v285_l1397
 (book-utils/include-dag-fn-as-section 'smooth-stat plotly/smooth-stat))


(def
 v286_l1398
 (book-utils/include-dag-fn-as-section
  'correlation-stat
  plotly/correlation-stat))


(def v287_l1400 (book-utils/md "\n## Substitution Keys 🔑\n"))


(def
 v288_l1404
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
