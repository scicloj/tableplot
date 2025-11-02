;; # Plotly API reference 📖

;; This chapter is a detailed refernce of Tableplot's Plotly API.
;; For diverse examples, see the
;; [Plotly API Walkthrough](./tableplot_book.plotly_walkthrough.html).

;; ## Setup 🔨

;; In this tutorial, we use:

;; * The Tableplot plotly API namepace `scicloj.tableplot.v1.plotly`
;; * [Tablecloth](https://scicloj.github.io/tablecloth/) for dataset processing and column processing
;; * [dtype-next](https://github.com/cnuernber/dtype-next)'s tensor namespace for tensor examples
;; * dtype-next's buffered image namespace for image representation
;; * [Kindly](https://scicloj.github.io/kindly-noted/) (to specify how certain values should be visualized)
;; * the datasets defined in the [Datasets chapter](./tableplot_book.datasets.html)

;; We require a few aditional namespaces which are used internally to generate this reference.

(ns tableplot-book.plotly-reference
  (:require [scicloj.tableplot.v1.plotly :as plotly]
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
            [clojure.math :as math]))

^:kindly/hide-code
(require '[scicloj.tableplot.v1.book-utils :as book-utils])

(book-utils/md
 "## Overview 🐦
The Tableplot Plotly API allows the user to write functional pipelines
that create and process *templates* of plots, that can eventually be realized
as [Plotly.js](https://plotly.com/javascript/) specifications.

The data is assumed to be held in datasets defined by [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset)
(which can conveniently be used using [Tablecloth](https://scicloj.github.io/tablecloth)).

The templates are an adapted version of
[Hanami Templates](https://github.com/jsa-aerial/hanami?tab=readme-ov-file#templates-substitution-keys-and-transformations).
Hanami transforms templates by recursively applying a simple set of rules.
The rules are based on looking up *substitution keys* according to standard defaults
as well as user substitutions overriding those defaults.
Tableplot uses a slighly adapted version of Hanami's template transformations,
which makes sure not to recurse into datasets.

For example, the `layer-point` function generates a template with some specified
substitutions. Let us apply this function to a dataset with some user substitutions.
As you can see below, all the substitution keys are keywords beginning with `=`. E.g., `:=color-type`.
This is just a convention that helps distinguish their role from other keys.

By default, this template is annotated by the [Kindly](https://scicloj.github.io/kindly/)
standared so that tools supporting Kindly (such as [Clay](https://scicloj.github.io/clay/))
will display by realizing it and using it as a Plotly.js specification.     
")

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10}))

;; Test: template has dataset
(kind/test-last [#(contains? (::ht/defaults %) :=dataset)])

;; Test: layer has correct x and y mappings
(kind/test-last [#(let [layer-defaults (-> % ::ht/defaults :=layers first ::ht/defaults)]
                    (and (= (:=x layer-defaults) :sepal-width)
                         (= (:=y layer-defaults) :sepal-length)))])

;; Test: realized spec produces scatter type traces
(kind/test-last [#(= (-> % plotly/plot :data first :type) "scatter")])

(book-utils/md "To inspect it, let us use [Kindly](https://scicloj.github.io/kindly-noted/) to request
that this template would rather be pretty-printed as a data structure.")
(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10})
    kind/pprint)

(book-utils/md "
For now, you are not supposed to make sense of this data representation.
As a user, you usually do not need to think about it.

If you wish to see the actual Plotly.js specification, you can use
the `plot` function:
")
(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10})
    plotly/plot
    kind/pprint)

(book-utils/md "We may also inspect it with [Portal](https://github.com/djblue/portal):")

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10})
    plotly/plot
    kind/portal)

(book-utils/md "
This is useful for debugging, and also when one wishes to edit the Plotly.js
spec directly.

In the remainder of this chapter, we will offer a detailed reference to the API
functions, the way layers are defined, the substitution keys, and the relationships
among them.

## Debugging 🐛

Throughout this notebook, we will sometimes use the `debug` function that
allows one to look into the value of a given substitution key in a given
context.

For example, here we learn about the `:=background`
key for background color. In this example,
we kept it grey, which is its default.")

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10})
    (plotly/debug :=background))

(book-utils/md "
## Raw Plotly specifications ✏

Before beginning the exploration of Tableplot's Plotly API, let us remember we may
also use the raw format supported by Plotly.js.
We simply use plain Clojuree data structures to represent the JSON format expected by Plotly.js
and annotate it as `kind/plotly`, so that our Clojure tooling knows to treat it as such
when displaying it.

For example, let us reproduce one of its [Bubble Chart Examples](https://plotly.com/javascript/bubble-charts/).

The original Javascript code looks like this:
```js
var trace1 = {
              x: [1, 2, 3, 4],
              y: [10, 11, 12, 13],
              text: ['A<br>size: 40', 'B<br>size: 60', 'C<br>size: 80', 'D<br>size: 100'],
              mode: 'markers',
              marker: {
                       color: ['rgb(93, 164, 214)', 'rgb(255, 144, 14)',  'rgb(44, 160, 101)', 'rgb(255, 65, 54)'],
                       size: [40, 60, 80, 100]
                       }
              };

var data = [trace1];

var layout = {
              title: {
                      text: 'Bubble Chart Hover Text'
                      },
              showlegend: false,
              height: 600,
              width: 600
              };

Plotly.newPlot('myDiv', data, layout);

```

Here is how we represent that in Clojure:
")
(plotly/plot
 {:data [{:x [1 2 3 4]
          :y [10 11 12 13]
          :text ["A<br>size: 40" "B<br>size: 60" "C<br>size: 80" "D<br>size: 100"]
          :mode :markers
          :marker {:color ["rgb(93, 164, 214)", "rgb(255, 144, 14)", "rgb(44, 160, 101)", "rgb(255, 65, 54)"]
                   :size [40 60 80 100]}}]
  :layout {:title {:text "Bubble Chart Hover Text"}
           :showlegend false
           :height 600
           :width 600}})

(book-utils/md "
Sometimes, this raw way is all we need; but in common situations, Tableplot makes things easier.

## Concepts 💡

### Plotly.js traces

Traces are a core concept in Plotly.js.
They specify separate parts of the plots which can be drawn on the same canvas
but may vary in their visual nature.

For example, here is a raw Plotly.js spec with two traces.
")
(plotly/plot
 {:data [{:x [1 2 3 4]
          :y [10 15 12 13]
          :color "blue"
          :mode :markers
          :marker {:size [40 60 80 100]
                   :color ["blue" "brown" "red" "green"]}}
         {:x [1 2 3 4]
          :y [15 21 17 18]
          :mode :markers
          :color "grey"
          :marker {:size 50
                   :color "grey"}}]})

(book-utils/md "
In Tableplot, we often do not need to think about traces, as they are drawn for us.
But it is helpful to know about them if we wish to understand the specs generated by Tableplot.

### Layers

Layers are a higher-level concept. We introduce them in Tableplot following
[ggplot2](https://ggplot2.tidyverse.org/)'s
[layered grammar of graphics](https://vita.had.co.nz/papers/layered-grammar.html).
Plotly bindings in other languages have similar concepts.

Like traces, layers are also parts of the plot that can be drawn on the same canvas,
but they are a slightly higher-level concept, that makes it easier to bind our data to parts of the plot.

Layers are themselves templates, so they can have their own substitutions.

For example:")
(-> (rdatasets/datasets-iris)
    (tc/random 10 {:seed 1})
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 20})
    (plotly/layer-text {:=text :species}))

(book-utils/md "
This plot has **two layers**: one for points, and one for text (which is visible on hover).

Let us see that using `debug`:
")
(-> (rdatasets/datasets-iris)
    (tc/random 10 {:seed 1})
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 20})
    (plotly/layer-text {:=text :species})
    (plotly/debug :=layers)
    kind/pprint)

(book-utils/md "
You see, a layer is an intermediate data representation of Tableplot
that takes care of the details necessary to generate traces.

In our example, the two layers are realized as **four traces**: since the point layer is colored
by species, it is realized as three traces.

Let us see that using `debug`:
")
(-> (rdatasets/datasets-iris)
    (tc/random 10 {:seed 1})
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 20})
    (plotly/layer-text {:=text :species})
    (plotly/debug :=traces)
    kind/pprint)

(book-utils/md "
### Mark

Mark is a Tableplot notion that is used to distinguish different types of layers,
e.g. `point-layer` vs `line-layer`.
It is similar to the ggplot notion of 'geom'.

Its possible values are:")
^:kindly/hide-code
[:point :text :line :box :violin :bar :segment]

(book-utils/md "
Here, `:box` means [boxplot](https://en.wikipedia.org/wiki/Box_plot),
and `:violin` means [Violin plot](https://en.wikipedia.org/wiki/Violin_plot).

### Coordinates

Coordinates are a Tableplot notion that defines
how marks are eventually mapped over to the cranvas,
similar to ggplot's notion.

We currently support the following:")
^:kindly/hide-code
[:2d :3d :polar :geo]

(book-utils/md "Here, 2d and 3d mean Eucledian coordinates of the corresponding dimensions,
and `:geo` means latitude and longitude.

For example:

")

;; #### geo

;; Inspired by Plotly's tutorial for [Scatter Plots on Maps in JavaScript](https://plotly.com/javascript/scatter-plots-on-maps/):

(-> {:lat [45.5, 43.4, 49.13, 51.1, 53.34, 45.24,
           44.64, 48.25, 49.89, 50.45]
     :lon [-73.57, -79.24, -123.06, -114.1, -113.28,
           -75.43, -63.57, -123.21, -97.13, -104.6]
     :text ["Montreal", "Toronto", "Vancouver", "Calgary", "Edmonton",
            "Ottawa", "Halifax", "Victoria", "Winnepeg", "Regina"]}
    tc/dataset
    (plotly/base {:=coordinates :geo
                  :=lat :lat
                  :=lon :lon})
    (plotly/layer-point {:=mark-opacity 0.8
                         :=mark-color ["#bebada", "#fdb462", "#fb8072", "#d9d9d9", "#bc80bd",
                                       "#b3de69", "#8dd3c7", "#80b1d3", "#fccde5", "#ffffb3"]
                         :=mark-size 20
                         :=name "Canadian cities"})
    (plotly/layer-text {:=text :text
                        :=textfont {:size 7
                                    :color :purple}})
    plotly/plot
    (assoc-in [:layout :geo]
              {:scope "north america"
               :resolution 10
               :lonaxis {:range [-130 -55]}
               :lataxis {:range [40 60]}
               :countrywidth 1.5
               :showland true
               :showlakes true
               :showrivers true}))

;; #### 3d

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=z :petal-length
                         :=color :petal-width
                         :=coordinates :3d}))

;; Test: 3d coordinates are configured correctly
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=coordinates) :3d)])

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=z :petal-length
                         :=color :species
                         :=coordinates :3d}))

;; #### polar

;; Monthly rain amounts - polar bar-chart

(def rain-data
  (tc/dataset
   {:month [:Jan :Feb :Mar :Apr
            :May :Jun :Jul :Aug
            :Sep :Oct :Nov :Dec]
    :rain (repeatedly #(rand-int 200))}))

(-> rain-data
    (plotly/layer-bar
     {:=r :rain
      :=theta :month
      :=coordinates :polar
      :=mark-size 20
      :=mark-opacity 0.6}))

;; Test: polar coordinates are configured correctly
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=coordinates) :polar)])

;; Controlling the polar layout
;; (by manipulating the raw Plotly.js spec):

(-> rain-data
    (plotly/base
     {})
    (plotly/layer-bar
     {:=r :rain
      :=theta :month
      :=coordinates :polar
      :=mark-size 20
      :=mark-opacity 0.6})
    plotly/plot
    (assoc-in [:layout :polar]
              {:angularaxis {:tickfont {:size 16}
                             :rotation 90
                             :direction "counterclockwise"}
               :sector [0 180]}))

;; A polar random walk - polar line-chart

(let [n 50]
  (-> {:r (->> (repeatedly n #(- (rand) 0.5))
               (reductions +))
       :theta (->> (repeatedly n #(* 10 (rand)))
                   (reductions +)
                   (map #(rem % 360)))
       :color (range n)}
      tc/dataset
      (plotly/layer-point
       {:=r :r
        :=theta :theta
        :=coordinates :polar
        :=mark-size 10
        :=mark-opacity 0.6})
      (plotly/layer-line
       {:=r :r
        :=theta :theta
        :=coordinates :polar
        :=mark-size 3
        :=mark-opacity 0.6})))

(book-utils/md "

### Plotly.js mode and type  

Mode and type are Plotly.js notions that are used to distinguish
diffetent types of traces.

Combinations of Tableplot's mark and coordinates
are mapped onto combinations of Plotly.js mode and type,
but currently we do not use all the meaningful combinations
supported by Plotly.js.

Mode is derived from mark as follows:
")
^:kindly/hide-code
(-> {:mark [:point :text :line :box :bar :segment :heatmap :surface]}
    tc/dataset
    (tc/map-columns :mode [:mark] plotly/mark->mode))

(book-utils/md "
Type is defined as the concatenation of a mark-based string:
(`\"box\"`,`\"violin\"`,`\"bar\"`,`\"heatmap\"`,`\"surface\"` if that is the mark,
and `\"scatter\"` otherwise)
with a coordinates-based string
(`\"3d\"`, `\"polar\"`, or `\"geo\"` if we have such coordinates, `nil` otherwise).

Thus, for example, if the mark is `:point` and the coordinates are `:polar`,
then the type is `\"scatterpolar\"`.

### Field types

Looking into the data in the columns, we may classify them into the following types:

- `:quantitative` - numerical columns
- `:temporal` - date-time columns
- `:nominal` - all other column types (e.g., Strings, keywords)

In certain situations, the types of data in relevant columns determines the way things
should be plotted.

For example, when a column is used for coloring a plot, we should use gradient colors
when it is quantitative but more distinct colors when it is nominal.

Colorin gby a nominal column:
")
(-> (rdatasets/datasets-iris)
    (plotly/layer-point
     {:=x :sepal-width
      :=y :sepal-length
      :=color :species
      :=mark-size 20}))

(book-utils/md "
Coloring by a Quantitative column:
")
(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=mark-size 20}))

(book-utils/md "
Overriding a quantitative column to be considered nominal by the `:=color-type` key:
")
(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=color-type :nominal
      :=mark-size 20}))

;; Test: color-type can be explicitly set to nominal
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=color-type) :nominal)])

(book-utils/md "
### Stat

A stat is a statistical transformation that takes the substitution context
and returns a new dataset.
Stats such as `smooth-stat`, `histogram-stat`, and `density-stat` are used in a few of the layer functions, such as
`layer-smooth`, `layer-histogram`, and `layer-density`.

The user will typically not need to think about them, but they
are a useful concept in extending Tableplot.

## API functions ⚙
")

(book-utils/include-fnvar-as-section #'plotly/base)

(book-utils/md "#### For example

Setting layout options:
")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=background "floralwhite"
                  :=height 300
                  :=width 400})
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species
                         :=mark-size 10
                         :=mark-opacity 0.6}))

;; Test: layout settings (background, height, width) are applied
(kind/test-last [#(let [layout (-> % plotly/plot :layout)]
                    (and (= (:plot_bgcolor layout) "floralwhite")
                         (= (:height layout) 300)
                         (= (:width layout) 400)))])

(book-utils/md "
Setting properties which are shared between layers.")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length
                  :=mark-size 10})
    (plotly/layer-point {:=mark-color "grey"
                         :=mark-size 20
                         :=mark-opacity 0.3})
    (plotly/layer-point {:=color :species}))

;; Test: base with multiple layers creates 2 layers
(kind/test-last [#(= (-> % ::ht/defaults :=layers count) 2)])

;; Test: base sets shared x and y defaults
(kind/test-last [#(and (= (-> % ::ht/defaults :=x) :sepal-width)
                       (= (-> % ::ht/defaults :=y) :sepal-length))])

(book-utils/include-fnvar-as-section #'plotly/layer)

(book-utils/md "#### For example
We could write someting like:")

(-> (rdatasets/datasets-iris)
    (plotly/layer plotly/layer-base
                  {:=mark :point
                   :=x :sepal-width
                   :=y :sepal-length}))

(book-utils/md "
Of course, this can also be expressed more succinctly using `layer-point`.
")
(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length}))

(book-utils/include-fnvar-as-section #'plotly/layer-point)

(book-utils/md "#### For example")

;; Customizing mark size:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=mark-size 20}))

;; Customizing mark symbol:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=mark-symbol :diamond}))

;; Customizing mark color:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=mark-color "darkred"}))

;; Customizing mark opacity:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=mark-opacity 0.5}))

;; Coloring by `:cyl` (considered `:quantitative` as it is a numerical column).

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=mark-size 20}))

(book-utils/md "
Coloring by `:cyl` and overriding `:=colorscale`:
")

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=colorscale :Greens
      :=mark-size 20}))

;; Coloring by `:cyl`, and marking it as `:nominal`:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=color :cyl
      :=color-type :nominal
      :=mark-size 20}))

;; Determining mark size by `:cyl`:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=size :cyl}))

(book-utils/md "Determining mark size by `:cyl` and specifying the `:=size-range`:")
(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=size :cyl
      :=size-range [5 15]}))

(book-utils/md
 "Determining mark size by `:cyl`, and marking `:=size-type` as `:nominal`: ")

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=size :cyl
      :=size-type :nominal}))

;; Determining mark symbol by `:cyl`:

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-point
     {:=x :mpg
      :=y :disp
      :=symbol :cyl
      :=mark-size 20
      :=mark-color "darkred"}))

;; Using the fact that `:=x` and `:=y` default to `:x` and `:y`:

(-> {:x (range 29)
     :y (reductions + (repeatedly 29 rand))}
    tc/dataset
    plotly/layer-point)

;; String columns, varying size and color:
(-> {"x" [1 2 3 4]
     "y" [1 4 9 16]
     "z" [:A :B :A :B]
     "w" [:C :C :D :D]}
    tc/dataset
    (plotly/layer-point {:=x "x"
                         :=y "y"
                         :=color "z"
                         :=size "w"}))

;; String columns, varying symbol and color:
(-> {"x" [1 2 3 4]
     "y" [1 4 9 16]
     "z" [:A :B :A :B]
     "w" [:C :C :D :D]}
    tc/dataset
    (plotly/layer-point {:=x "x"
                         :=y "y"
                         :=color "z"
                         :=symbol "w"
                         :=mark-size 20}))

;; Using 3d coordinates:

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=z :petal-length
                         :=color :petal-width
                         :=coordinates :3d}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=z :petal-length
                         :=color :species
                         :=coordinates :3d}))

(book-utils/include-fnvar-as-section #'plotly/layer-line)

(book-utils/md "#### For example")

(-> (rdatasets/ggplot2-economics_long)
    (tc/select-rows #(-> % :variable (= "unemploy")))
    (plotly/layer-line
     {:=x :date
      :=y :value
      :=mark-color "purple"}))

;; Test: layer-line produces lines mode traces
(kind/test-last [#(= (-> % plotly/plot :data first :mode) :lines)])

(book-utils/include-fnvar-as-section #'plotly/layer-bar)

(book-utils/md "#### For example")

(-> (rdatasets/datasets-mtcars)
    (tc/group-by [:cyl])
    (tc/aggregate {:total-disp #(-> % :disp tcc/sum)})
    (tc/add-column :bar-width 0.5)
    (plotly/layer-bar
     {:=x :cyl
      :=bar-width :bar-width
      :=y :total-disp}))

;; Test: bar chart produces correct trace type
(kind/test-last [#(-> % plotly/plot :data first :type (= "bar"))])

(book-utils/include-fnvar-as-section #'plotly/layer-boxplot)

(book-utils/md "#### For example")

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-boxplot
     {:=x :cyl
      :=y :disp}))

;; Test: boxplot produces box trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) "box")])

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-boxplot
     {:=x :cyl
      :=y :disp
      :=color :am
      :=color-type :nominal}))

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-boxplot
     {:=x :cyl
      :=y :disp
      :=color :am
      :=color-type :nominal
      :=boxmode :group}))

(book-utils/include-fnvar-as-section #'plotly/layer-violin)

(book-utils/md "#### For example")

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-violin
     {:=x :cyl
      :=y :disp}))

;; Test: violin produces violin trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) "violin")])

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-violin
     {:=x :cyl
      :=y :disp
      :=box-visible true}))

;; Test: violin with box-visible option is configured correctly
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=box-visible) true)])

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-violin
     {:=x :cyl
      :=y :disp
      :=meanline-visible true}))

;; Test: meanline-visible option is configured
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=meanline-visible) true)])

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-violin
     {:=x :cyl
      :=y :disp
      :=color :am
      :=color-type :nominal}))

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-violin
     {:=x :cyl
      :=y :disp
      :=color :am
      :=color-type :nominal
      :=violinmode :group}))

;; Test: violinmode option is configured
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=violinmode) :group)])

(book-utils/include-fnvar-as-section #'plotly/layer-segment)

(book-utils/md "#### For example")

(-> (rdatasets/datasets-iris)
    (plotly/layer-segment
     {:=x0 :sepal-width
      :=y0 :sepal-length
      :=x1 :petal-width
      :=y1 :petal-length
      :=mark-opacity 0.4
      :=mark-size 3
      :=color :species}))

;; Test: segment produces scatter traces with lines mode
(kind/test-last [#(and (= (-> % plotly/plot :data first :type) "scatter")
                       (= (-> % plotly/plot :data first :mode) :lines))])

(book-utils/include-fnvar-as-section #'plotly/layer-text)

(book-utils/md "#### For example")

(-> (rdatasets/datasets-mtcars)
    (plotly/layer-text
     {:=x :mpg
      :=y :disp
      :=text :cyl
      :=textfont {:family "Courier New, monospace"
                  :size 16
                  :color :purple}
      :=mark-size 20}))

;; Test: text layer produces text mode traces
(kind/test-last [#(= (-> % plotly/plot :data first :mode) :text)])

;; Test: textfont configuration is applied
(kind/test-last [#(= (-> % plotly/plot :data first :textfont :color) :purple)])

(book-utils/include-fnvar-as-section #'plotly/layer-histogram)

(book-utils/md "#### Examples:")

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram {:=x :sepal-width}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram {:=x :sepal-width
                             :=histogram-nbins 30}))

;; Test: histogram layer has correct nbins configuration
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=histogram-nbins) 30)])

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram {:=x :sepal-width
                             :=color :species
                             :=mark-opacity 0.5}))

;; Test: histogram with grouping creates 3 traces (one per species)
(kind/test-last [#(= (-> % plotly/plot :data count) 3)])

(book-utils/include-fnvar-as-section #'plotly/layer-histogram2d)

(book-utils/md "(experimental)")

(book-utils/md "#### Examples:
Currently, the number of bins is determined by `:histogram-nbins`.
We are exploring various rules of thumbs to determine it automatically.
")

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram2d {:=x :sepal-width
                               :=y :sepal-length}))

;; Test: histogram2d produces heatmap trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) "heatmap")])

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram2d {:=x :sepal-width
                               :=y :sepal-length
                               :=colorscale :Portland}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-histogram2d {:=x :sepal-width
                               :=y :sepal-length
                               :=histogram-nbins 100}))

(let [n 10000]
  (-> {:x (repeatedly n rand)}
      tc/dataset
      (tc/add-column :y #(tcc/* (repeatedly n rand)
                                (:x %)
                                (:x %)))
      (plotly/layer-histogram2d {:=histogram-nbins 250})))

(book-utils/include-fnvar-as-section #'plotly/layer-density)

(book-utils/md "#### Examples:")

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width}))

;; Test: density produces scatter trace with line mode and tozeroy fill
(kind/test-last [#(let [trace (-> % plotly/plot :data first)]
                    (and (= (:mode trace) :lines)
                         (= (:fill trace) :tozeroy)))])

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width
                           :=density-bandwidth 0.05}))

;; Test: density bandwidth is configured correctly
(kind/test-last [#(= (-> % ::ht/defaults :=layers first ::ht/defaults :=density-bandwidth) 0.05)])

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width
                           :=density-bandwidth 1}))

(-> (rdatasets/datasets-iris)
    (plotly/layer-density {:=x :sepal-width
                           :=color :species}))

;; Test: density with color grouping creates 3 traces
(kind/test-last [#(= (-> % plotly/plot :data count) 3)])

;; Witn no substitution keys, we can rely on
;; the default for `:=x` being the `:x` column:

(-> {:x (repeatedly 9999 rand)}
    tc/dataset
    plotly/layer-density)

(book-utils/include-fnvar-as-section #'plotly/layer-smooth)

(book-utils/md "#### Examples:")

(book-utils/md "Simple linear regression of `:=y` by `:=x`:")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=mark-color "orange"
                          :=name "Predicted"}))

;; Test: multi-layer plot has 2 layers
(kind/test-last [#(= (-> % ::ht/defaults :=layers count) 2)])

;; Test: realized plot has 2 traces
(kind/test-last [#(= (-> % plotly/plot :data count) 2)])

;; Test: traces have correct names
(kind/test-last [#(let [traces (-> % plotly/plot :data)]
                    (and (= (:name (first traces)) "Actual")
                         (= (:name (second traces)) "Predicted")))])

(book-utils/md "Multiple linear regression of `:=y` by `:=predictors`:")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=predictors [:petal-width
                                        :petal-length]
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

;; Test: smooth layer with predictors is configured correctly
(kind/test-last [#(= (-> % ::ht/defaults :=layers second ::ht/defaults :=predictors)
                     [:petal-width :petal-length])])

(book-utils/md "Polynomial regression of `:=y` by `:=design-matrix`:")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=design-matrix [[:sepal-width '(identity :sepal-width)]
                                           [:sepal-width-2 '(* :sepal-width
                                                               :sepal-width)]]
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

;; Test: smooth layer with design-matrix is configured
(kind/test-last [#(not (nil? (-> % ::ht/defaults :=layers second ::ht/defaults :=design-matrix)))])

(book-utils/md "Custom regression defined by `:=model-options`:")

(require 'scicloj.ml.tribuo)

(def regression-tree-options
  {:model-type :scicloj.ml.tribuo/regression
   :tribuo-components [{:name "cart"
                        :type "org.tribuo.regression.rtree.CARTRegressionTrainer"
                        :properties {:maxDepth "8"
                                     :fractionFeaturesInSplit "1.0"
                                     :seed "12345"
                                     :impurity "mse"}}
                       {:name "mse"
                        :type "org.tribuo.regression.rtree.impurity.MeanSquaredError"}]
   :tribuo-trainer-name "cart"})

(-> (rdatasets/datasets-iris)
    (plotly/base {:=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=mark-color "green"
                         :=name "Actual"})
    (plotly/layer-smooth {:=model-options regression-tree-options
                          :=mark-opacity 0.5
                          :=name "Predicted"}))

(book-utils/md "Grouped regression where `:=inferred-group` is influenced by `:color`
since `:=color-type` is `:nominal`:")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=title "dummy"
                  :=color :species
                  :=x :sepal-width
                  :=y :sepal-length})
    plotly/layer-point
    plotly/layer-smooth)

(book-utils/md "Regression where grouping is avoiding using through `:=group`:")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=title "dummy"
                  :=color :species
                  :=group []
                  :=x :sepal-width
                  :=y :sepal-length})
    plotly/layer-point
    (plotly/layer-smooth {:=mark-color "red"}))

;; Test: group can be set to empty array to override grouping
(kind/test-last [#(= (-> % ::ht/defaults :=group) [])])

(book-utils/md "An simpler way to achieve this -- the color is only defined for the point layer:")

(-> (rdatasets/datasets-iris)
    (plotly/base {:=title "dummy"
                  :=x :sepal-width
                  :=y :sepal-length})
    (plotly/layer-point {:=color :species})
    plotly/layer-smooth)

(book-utils/include-fnvar-as-section #'plotly/layer-heatmap)

(book-utils/md "#### For example")

;; Numerical `x,y` axes:

;; (Note we are using the fact that `:=x`,`:=y`,`:=z` default to `:x`,`:y`,`:z`.)

(-> {:x (range 100)
     :y (range 200)
     :z (for [i (range 200)]
          (for [j (range 100)]
            (+ (* (math/sin (/ i 5)) i)
               (* (math/sin (/ j 5)) j))))}
    tc/dataset
    plotly/layer-heatmap)

;; Test: heatmap produces heatmap trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) "heatmap")])

;; Mixed Categorical and numerical `x,y` axes:

(-> {:x [:A :B]
     :y (range 3)
     :z (for [i (range 3)]
          (for [j (range 2)]
            (+ i j)))}
    tc/dataset
    plotly/layer-heatmap)

;; Controling the `z` range:

(-> {:x [:A :B]
     :y (range 3)
     :z (for [i (range 3)]
          (for [j (range 2)]
            (+ i j)))}
    tc/dataset
    (plotly/layer-heatmap
     {:=zmin 0
      :=zmax 5}))

;; Referring to data elements by name:

(-> {:site [:A :B]
     :time (range 3)
     :temperature (for [i (range 3)]
                    (for [j (range 2)]
                      (+ i j)))}
    tc/dataset
    (plotly/layer-heatmap {:=x :site
                           :=y :time
                           :=z :temperature}))

;; Customizing [color scales](https://plotly.com/javascript/colorscales/):

(-> {:x (range 100)
     :y (range 200)
     :z (for [i (range 200)]
          (for [j (range 100)]
            (+ (* (math/sin (/ i 5)) i)
               (* (math/sin (/ j 5)) j))))}
    tc/dataset
    (plotly/layer-heatmap {:=colorscale :Greys}))

(book-utils/include-fnvar-as-section #'plotly/layer-correlation)

(book-utils/md "#### For example")

;; Correlations of a few columns:

(let [n 99]
  (-> {:u (repeatedly n rand)
       :v (repeatedly n rand)
       :w (repeatedly n rand)}
      tc/dataset
      (tc/add-columns {:x #(tcc/+ (:u %) (:v %))
                       :y #(tcc/- (:w %) (tcc/+ (:u %) (:v %)))})
      plotly/layer-correlation))

;; Test: correlation produces heatmap trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) "heatmap")])

;; Correlations of a few columns with a different
;; [color scale](https://plotly.com/javascript/colorscales/)
;; and `zmin`-`zmax` range that is mapped into colors:

(let [n 99]
  (-> {:u (repeatedly n rand)
       :v (repeatedly n rand)
       :w (repeatedly n rand)}
      tc/dataset
      (tc/add-columns {:x #(tcc/+ (:u %) (:v %))})
      (plotly/layer-correlation {:=zmin 0
                                 :=zmax 1
                                 :=colorscale :hot})))

;; Correlations of many columns:

;; Let us visualize the correlations of an
;; [autoregressive random process](https://en.wikipedia.org/wiki/Autoregressive_model)
;; at different time shifts.

(let [autoregression (->> (repeatedly 1000 rand)
                          (reductions (fn [x noise]
                                        (+ (* 0.8 x)
                                           (* 0.2 noise)))))
      shifts-dataset (->> (for [shift (range 50)]
                            [(str "shift" shift)
                             (drop shift autoregression)])
                          (apply concat)
                          (apply array-map)
                          tc/dataset)]
  (-> shifts-dataset
      (plotly/layer-correlation {:=zmin 0
                                 :=zmax 1
                                 :=colorscale :hot})))

(book-utils/include-fnvar-as-section #'plotly/layer-surface)

(book-utils/md "#### For example")

(-> {:z (for [i (range 100)]
          (for [j (range 100)]
            (-> (tcc/- [i j]
                       [30 60])
                (tcc// [20 50])
                tcc/sq
                tcc/sum
                -
                math/exp)))}
    tc/dataset
    plotly/layer-surface)

;; Combining a 3d scatterplot with a surface:

(let [xy->z (fn [x y]
              (-> [y x]
                  tcc/sq
                  tcc/sum
                  -
                  math/exp))
      n 30]
  (-> {:x (repeatedly n rand)
       :y (repeatedly n rand)}
      tc/dataset
      (tc/map-columns :z
                      [:x :y]
                      (fn [x y]
                        (+ (xy->z x y)
                           (* 0.1 (rand)))))
      (plotly/layer-point {:=coordinates :3d})
      (plotly/layer-surface {:=dataset (let [xs (range 0 1 0.1)
                                             ys (range 0 1 0.1)]
                                         (tc/dataset
                                          {:x xs
                                           :y ys
                                           :z (for [y ys]
                                                (for [x xs]
                                                  (xy->z x y)))}))
                             :=mark-opacity 0.5})))

(book-utils/include-fnvar-as-section #'plotly/surface)

(book-utils/md "#### For example")

(plotly/surface
 (for [i (range 100)]
   (for [j (range 100)]
     (-> (tcc/- [i j]
                [30 60])
         (tcc// [20 50])
         tcc/sq
         tcc/sum
         -
         math/exp))))

;; Test: surface function produces surface trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) :surface)])

(book-utils/include-fnvar-as-section #'plotly/imshow)

(book-utils/md
 "Imshow uses dtype-next's [BufferedImage support](https://cnuernber.github.io/dtype-next/buffered-image.html) to figure out the right order of color channels, etc.

So, it can handle plain vectors of vectors, dtype next tensors, and actual Java BufferedImage objects.")
(book-utils/md "#### For example")

(plotly/imshow
 (for [i (range 10)]
   (for [j (range 10)]
     [(* 10 i) ; Red
      (* 10 j) ; Green
      (* 10 (+ i j)) ; Blue
      ])))

;; Test: imshow produces image trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) :image)])

(plotly/imshow
 (tensor/compute-tensor
  [100 100 3]
  (fn [i j k]
    (case k
      0 i ; Red
      1 j ; Green
      2 (+ i j) ; Blue
      ))
  :uint8))

(defonce Crab-Nebula-image
  (-> "https://scicloj.github.io/sci-cloj-logo-transparent.png"
      (java.net.URL.)
      (javax.imageio.ImageIO/read)))

(plotly/imshow Crab-Nebula-image)

(book-utils/include-fnvar-as-section #'plotly/splom)

(book-utils/md "#### For example")

(-> (rdatasets/datasets-iris)
    (plotly/splom {:=colnames [:sepal-width
                               :sepal-length
                               :petal-width
                               :petal-length]
                   :=height 600
                   :=width 600}))

;; Test: splom produces splom trace type
(kind/test-last [#(= (-> % plotly/plot :data first :type) :splom)])

(-> (rdatasets/datasets-iris)
    (plotly/splom {:=colnames [:sepal-width
                               :sepal-length
                               :petal-width
                               :petal-length]
                   :=color :species
                   :=height 600
                   :=width 600}))

(-> (rdatasets/datasets-iris)
    (plotly/splom {:=colnames [:sepal-width
                               :sepal-length
                               :petal-width
                               :petal-length]
                   :=symbol :species
                   :=height 600
                   :=width 600}))

;; By default, all columns participate in a SPLOM:

(-> (rdatasets/datasets-iris)
    (tc/select-columns [:sepal-width
                        :sepal-length
                        :petal-width
                        :petal-length])
    (plotly/splom {:=height 600
                   :=width 600}))

(book-utils/include-fnvar-as-section #'plotly/plot)

(book-utils/md "#### For example")
(-> (rdatasets/datasets-iris)
    tc/head
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length})
    plotly/plot
    kind/pprint)

(book-utils/md "
This can be useful for editing the plot as a raw Plotly.js specification.
For example:
")
(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length})
    plotly/plot
    (assoc-in [:layout :plot_bgcolor] "floralwhite"))

(book-utils/include-fnvar-as-section #'plotly/debug)

(book-utils/md "#### For example")
(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species}))

(book-utils/md "Let us verify that `:=background` is deterimined to be grey.")

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species})
    (plotly/debug :=background))

(book-utils/md "Here, let us verify `:=color-type` for the 0th layer is deterimined to be `:nominal`.")

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species})
    (plotly/debug 0 :=color-type))

(book-utils/md "Here, let us check both `:=color` and `:=color-type` for the 0th layer.")

(-> (rdatasets/datasets-iris)
    (plotly/layer-point {:=x :sepal-width
                         :=y :sepal-length
                         :=color :species})
    (plotly/debug 0 {:color :=color
                     :color-type :=color-type}))

(book-utils/md "
## Stats 🖩
")

(book-utils/include-dag-fn-as-section 'histogram-stat plotly/histogram-stat)
(book-utils/include-dag-fn-as-section 'density-stat plotly/density-stat)
(book-utils/include-dag-fn-as-section 'smooth-stat plotly/smooth-stat)
(book-utils/include-dag-fn-as-section 'correlation-stat plotly/correlation-stat)

(book-utils/md "
## Substitution Keys 🔑
")

^:kindly/hide-code
(->> plotly/standard-defaults
     (map
      (fn [[k v doc]]
        (kind/md
         [(format "### %s" (book-utils/include-key-or-symbol-name k))
          (some->> doc
                   (format "**role:** %s\n"))
          (format "**default:** %s\n"
                  (cond (fn? v) (book-utils/include-dag-fn v)
                        (= v hc/RMV) "`NONE`"
                        (keyword? v) (book-utils/include-key-or-symbol-name v)
                        :else (book-utils/include-form v)))])))
     kind/fragment)

