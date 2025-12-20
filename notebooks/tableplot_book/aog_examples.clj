;; # RDatasets Examples with Vega-Lite Backend
;; Testing the AlgebraOfGraphics API with real-world datasets
;;
;; This notebook explores larger, real-world datasets from the RDatasets collection
;; using the Vega-Lite backend for visualization.

(ns tableplot-book.aog-examples
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.metamorph.ml.rdatasets :as rdatasets]
            [tablecloth.api :as tc]))

;; ## Dataset Overview
;;
;; The `scicloj.metamorph.ml.rdatasets` namespace provides access to hundreds of
;; datasets from the R ecosystem. All datasets are returned as tablecloth datasets.

;; ## Example 1: Motor Trend Car Road Tests (mtcars)
;;
;; The mtcars dataset contains fuel consumption and 10 aspects of automobile design
;; for 32 automobiles (1973–74 models).

(def mtcars (rdatasets/datasets-mtcars))

;; Let's examine the structure:
mtcars

;; ### Basic Scatter Plot
;;
;; Relationship between weight and fuel efficiency:

(aog/draw
 (aog/* (aog/data mtcars)
        (aog/mapping :wt :mpg)
        (aog/scatter)))

;; ### Colored by Number of Cylinders
;;
;; Add color aesthetic to show cylinder groups:

(aog/draw
 (aog/* (aog/data mtcars)
        (aog/mapping :wt :mpg {:color :cyl})
        (aog/scatter)))

;; ### Size and Color Aesthetics
;;
;; Use both color (cylinders) and size (horsepower) to show multiple dimensions:

(aog/draw
 (aog/* (aog/data mtcars)
        (aog/mapping :wt :mpg {:color :cyl :size :hp})
        (aog/scatter)))

;; ### With Linear Regression
;;
;; Add a linear trend line:

(aog/draw
 (aog/* (aog/data mtcars)
        (aog/mapping :wt :mpg)
        (aog/+ (aog/scatter)
               (aog/* (aog/linear)
                      (aog/line)))))

;; ## Example 2: Iris Dataset
;;
;; Fisher's iris dataset - measurements of iris flowers across 3 species.

(def iris (rdatasets/datasets-iris))

iris

;; ### Sepal Dimensions
;;
;; Compare sepal length vs width across species:

(aog/draw
 (aog/* (aog/data iris)
        (aog/mapping :sepal-length :sepal-width {:color :species})
        (aog/scatter)))

;; ### Petal Dimensions
;;
;; Petal measurements show clearer species separation:

(aog/draw
 (aog/* (aog/data iris)
        (aog/mapping :petal-length :petal-width {:color :species})
        (aog/scatter)))

;; ## Example 3: Palmer Penguins
;;
;; Size measurements for three penguin species on islands in Palmer Archipelago, Antarctica.

(def penguins (rdatasets/palmerpenguins-penguins))

penguins

;; ### Bill Dimensions by Species
;;
;; Explore penguin bill measurements:

(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :bill-length-mm :bill-depth-mm {:color :species})
        (aog/scatter)))

;; ### Body Mass vs Flipper Length
;;
;; Relationship between body size and flipper length:

(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :body-mass-g :flipper-length-mm {:color :species})
        (aog/scatter)))

;; ## Example 4: Diamonds Dataset
;;
;; A large dataset with prices and attributes of ~54,000 diamonds.

(def diamonds (rdatasets/ggplot2-diamonds))

;; Show first few rows and size:
(println "Diamonds dataset:" (tc/row-count diamonds) "rows")
(tc/head diamonds 10)

;; ### Carat vs Price
;;
;; Basic relationship between diamond weight and price:

(aog/draw
 (aog/* (aog/data diamonds)
        (aog/mapping :carat :price)
        (aog/scatter)))

;; ### By Diamond Cut Quality
;;
;; Color by cut quality:

(aog/draw
 (aog/* (aog/data diamonds)
        (aog/mapping :carat :price {:color :cut})
        (aog/scatter)))

;; ### Smaller Sample for Clarity
;;
;; With 54k points, let's take a sample for better visualization:

(def diamonds-sample (tc/random diamonds 1000))

(aog/draw
 (aog/* (aog/data diamonds-sample)
        (aog/mapping :carat :price {:color :cut})
        (aog/scatter)))

;; ## Example 5: NYC Flights 2013
;;
;; Flight data for all flights departing NYC in 2013.

(def flights (rdatasets/nycflights13-flights))

(println "Flights dataset:" (tc/row-count flights) "rows")
(tc/head flights)

;; ### Sample of Flights
;;
;; Take a sample to visualize departure delays vs arrival delays.
;; Drop missing values and filter extreme outliers for better visualization:

(def flights-clean
  (-> flights
      (tc/drop-missing [:dep-delay :arr-delay])
      (tc/select-rows (fn [row]
                        (and (< (:dep-delay row) 200)
                             (> (:dep-delay row) -50)
                             (< (:arr-delay row) 200)
                             (> (:arr-delay row) -50))))))

(println "Clean flights:" (tc/row-count flights-clean) "rows")

(def flights-sample (tc/random flights-clean 2000))

(aog/draw
 (aog/* (aog/data flights-sample)
        (aog/mapping :dep-delay :arr-delay)
        (aog/scatter)))

;; ### By Carrier
;;
;; Color by airline carrier:

(aog/draw
 (aog/* (aog/data flights-sample)
        (aog/mapping :dep-delay :arr-delay {:color :carrier})
        (aog/scatter)))

;; ## Example 6: Economics Time Series
;;
;; US economic time series data.

(def economics (rdatasets/ggplot2-economics))

economics

;; ### Unemployment Over Time
;;
;; Line plot of unemployment numbers:

(aog/draw
 (aog/* (aog/data economics)
        (aog/mapping :date :unemploy)
        (aog/line)))

;; ### Personal Savings Rate
;;
;; Another time series:

(aog/draw
 (aog/* (aog/data economics)
        (aog/mapping :date :psavert)
        (aog/line)))

;; ## Example 7: Gapminder
;;
;; Global health and economic data over time.

(def gapminder (rdatasets/gapminder-gapminder))

(tc/head gapminder)

;; ### Life Expectancy vs GDP (2007)
;;
;; Filter to most recent year:

(def gapminder-2007 (tc/select-rows gapminder #(= (:year %) 2007)))

(aog/draw
 (aog/* (aog/data gapminder-2007)
        (aog/mapping :gdp-percap :life-exp {:color :continent :size :pop})
        (aog/scatter)))

;; ## Example 8: Faceting

;; ### Penguins Faceted by Island
;;
;; Faceting creates small multiples - separate panels for each category.
;; Colors remain consistent across facets automatically.

(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :bill-length-mm :bill-depth-mm {:color :species :col :island})
        (aog/scatter)))

;; ### Iris Faceted by Species - Column Layout
;;
;; Use `:col` for horizontal facets:

(aog/draw
 (aog/* (aog/data iris)
        (aog/mapping :sepal-length :sepal-width {:col :species})
        (aog/scatter)))

;; ### Row Faceting
;;
;; Use `:row` for vertical facets:

(aog/draw
 (aog/* (aog/data iris)
        (aog/mapping :sepal-length :sepal-width {:row :species})
        (aog/scatter)))

;; ### Wrapped Faceting
;;
;; Use `:facet` for wrapped layout (automatically arranges facets in a grid):

(aog/draw
 (aog/* (aog/data iris)
        (aog/mapping :sepal-length :sepal-width {:facet :species})
        (aog/scatter)))

;; ## Summary
;;
;; These examples demonstrate the Vega-Lite backend working with:
;; - Small datasets (32 rows - mtcars)
;; - Medium datasets (150 rows - iris, 344 rows - penguins)
;; - Large datasets (54k rows - diamonds, 336k rows - flights)
;; - Time series data (economics)
;; - Multiple aesthetics (color, size)
;; - Statistical transformations (linear regression)
;; - Faceting/small multiples with consistent color scales
;;
;; Next steps to explore:
;; - More statistical transformations (smooth, density, histogram)
;; - Additional plot types (bar, area, box, violin)
;; - Custom scales and themes
