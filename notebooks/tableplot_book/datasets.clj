;; # Some datasets 

;; In this documentation, we will use a few datasets from [RDatasets](https://vincentarelbundock.github.io/Rdatasets/articles/data.html) and from the [Plotly datasets](https://plotly.github.io/datasets/).

(ns tableplot-book.datasets
  (:require [tablecloth.api :as tc]
            [clojure.string :as str]
            [scicloj.kindly.v4.kind :as kind]))

;; A convenience function for fetching a dataset and taking care of its column names:

(defn fetch-dataset [dataset-name]
  (-> dataset-name
      (->> (format "https://vincentarelbundock.github.io/Rdatasets/csv/%s.csv"))
      (tc/dataset {:key-fn (fn [k]
                             (-> k
                                 str/lower-case
                                 (str/replace #"\." "-")
                                 keyword))})
      (tc/set-dataset-name dataset-name)))

;; ## Edgar Anderson's Iris Data

(defonce iris
  (fetch-dataset "datasets/iris"))

iris

;; ## Motor Trend Car Road Tests

(defonce mtcars
  (fetch-dataset "datasets/mtcars"))

mtcars

;; ## US economic time series

(defonce economics-long
  (fetch-dataset "ggplot2/economics_long"))

economics-long

;; ## Tips dataset

(defonce tips
  (-> "https://raw.githubusercontent.com/plotly/datasets/master/tips.csv"
      (tc/dataset {:key-fn keyword})))

tips
