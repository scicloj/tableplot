(ns scicloj.tableplot.v1.aog.transforms
  "Statistical transformations for AlgebraOfGraphics.

  Transformations are functions that take data and return transformed data.
  For MVP, these are simple functions. Future: integrate with dag/defn-with-deps."
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [fastmath.stats :as stats]
            [tech.v3.dataset :as ds]))

;;; =============================================================================
;;; Helper functions

(defn- ensure-dataset
  "Convert data to dataset if needed."
  [data]
  (cond
    (nil? data) nil
    (ds/dataset? data) data
    (map? data) (tc/dataset data)
    (vector? data) (tc/dataset data)
    :else data))

(defn- column-values
  "Get values from a column, handling different data formats."
  [dataset col-key]
  (when (and dataset col-key)
    (vec (get dataset col-key))))

;;; =============================================================================
;;; Linear regression transformation

(defn linear-transform
  "Compute linear regression and return fitted values.

  Args:
  - dataset: The data to transform
  - x-col: X column key
  - y-col: Y column key
  - group-cols: Vector of grouping column keys (or nil if none)

  Returns:
  - Dataset with x values, fitted y values, and group columns"
  [dataset x-col y-col group-cols]
  (let [ds (ensure-dataset dataset)]
    (if (and ds x-col y-col)
      (if (and group-cols (seq group-cols))
        ;; Grouped linear regression
        (let [;; Group by all grouping columns
              grouped (tc/group-by ds group-cols)
              ;; Extract the groups
              group-data (get grouped :data)
              group-names (get grouped :name)

              fitted-data
              (mapcat
               (fn [group-name group-ds]
                 (let [x-vals (column-values group-ds x-col)
                       y-vals (column-values group-ds y-col)]
                   (when (and (seq x-vals) (seq y-vals) (> (count x-vals) 1))
                     (let [;; Simple linear regression: y = a + b*x
                           x-mean (stats/mean x-vals)
                           y-mean (stats/mean y-vals)
                           ;; Calculate slope
                           numerator (reduce + (map #(* (- %1 x-mean) (- %2 y-mean)) x-vals y-vals))
                           denominator (reduce + (map #(* (- % x-mean) (- % x-mean)) x-vals))
                           slope (if (zero? denominator) 0 (/ numerator denominator))
                           intercept (- y-mean (* slope x-mean))
                           ;; Generate fitted values over x range
                           x-sorted (sort x-vals)
                           x-min (first x-sorted)
                           x-max (last x-sorted)
                           x-range (if (= x-min x-max)
                                     [x-min]
                                     (let [step (/ (- x-max x-min) 100.0)]
                                       (vec (concat (range x-min x-max step) [x-max]))))
                           fitted-y (mapv #(+ intercept (* slope %)) x-range)]
                       (map (fn [x y]
                              (merge group-name {x-col x y-col y}))
                            x-range fitted-y)))))
               group-names group-data)]
          (tc/dataset fitted-data))

        ;; Ungrouped linear regression
        (let [x-vals (column-values ds x-col)
              y-vals (column-values ds y-col)]
          (if (and (seq x-vals) (seq y-vals) (> (count x-vals) 1))
            (let [;; Simple linear regression
                  x-mean (stats/mean x-vals)
                  y-mean (stats/mean y-vals)
                  numerator (reduce + (map #(* (- %1 x-mean) (- %2 y-mean)) x-vals y-vals))
                  denominator (reduce + (map #(* (- % x-mean) (- % x-mean)) x-vals))
                  slope (if (zero? denominator) 0 (/ numerator denominator))
                  intercept (- y-mean (* slope x-mean))
                  ;; Generate fitted values across the x range
                  x-sorted (sort x-vals)
                  x-min (first x-sorted)
                  x-max (last x-sorted)
                  x-range (if (= x-min x-max)
                            [x-min]
                            (let [step (/ (- x-max x-min) 100.0)]
                              (vec (concat (range x-min x-max step) [x-max]))))
                  fitted-y (mapv #(+ intercept (* slope %)) x-range)]
              (tc/dataset {x-col x-range
                           y-col fitted-y}))
            ds)))
      ds)))

;;; =============================================================================
;;; Smooth transformation (simplified loess-like)

(defn smooth-transform
  "Compute smoothed curve using local regression.

  Args:
  - dataset: The data to transform
  - x-col: X column key
  - y-col: Y column key
  - group-cols: Vector of grouping column keys (or nil if none)

  Returns:
  - Dataset with smoothed values"
  [dataset x-col y-col group-cols]
  (let [ds (ensure-dataset dataset)]
    (if (and ds x-col y-col)
      (if (and group-cols (seq group-cols))
        ;; Grouped smoothing
        (let [grouped (tc/group-by ds group-cols)
              group-data (get grouped :data)
              group-names (get grouped :name)

              smoothed-data
              (mapcat
               (fn [group-name group-ds]
                 (let [x-vals (column-values group-ds x-col)
                       y-vals (column-values group-ds y-col)]
                   (when (and (seq x-vals) (seq y-vals))
                     (let [sorted-pairs (sort-by first (map vector x-vals y-vals))
                           window-size 5
                           smoothed (mapv
                                     (fn [idx]
                                       (let [start (max 0 (- idx (quot window-size 2)))
                                             end (min (count sorted-pairs) (+ idx (quot window-size 2) 1))
                                             window (subvec (vec sorted-pairs) start end)
                                             avg-y (stats/mean (map second window))]
                                         [(first (nth sorted-pairs idx)) avg-y]))
                                     (range (count sorted-pairs)))]
                       (map (fn [[x y]]
                              (merge group-name {x-col x y-col y}))
                            smoothed)))))
               group-names group-data)]
          (tc/dataset smoothed-data))

        ;; Ungrouped smoothing
        (let [x-vals (column-values ds x-col)
              y-vals (column-values ds y-col)]
          (if (and (seq x-vals) (seq y-vals))
            ;; Simple smoothing: sort by x and use window average
            (let [sorted-pairs (sort-by first (map vector x-vals y-vals))
                  window-size 5
                  smoothed (mapv
                            (fn [idx]
                              (let [start (max 0 (- idx (quot window-size 2)))
                                    end (min (count sorted-pairs) (+ idx (quot window-size 2) 1))
                                    window (subvec (vec sorted-pairs) start end)
                                    avg-y (stats/mean (map second window))]
                                [(first (nth sorted-pairs idx)) avg-y]))
                            (range (count sorted-pairs)))]
              (tc/dataset {x-col (mapv first smoothed)
                           y-col (mapv second smoothed)}))
            ds)))
      ds)))

;;; =============================================================================
;;; Identity transformation (no-op)

(defn identity-transform
  "Identity transformation - returns data unchanged.

  This is used when no transformation is specified.

  Args:
  - dataset: The data
  - x-col: X column key (unused)
  - y-col: Y column key (unused)
  - group-cols: Grouping columns (unused)

  Returns:
  - Original dataset"
  [dataset x-col y-col group-cols]
  (ensure-dataset dataset))

(defn histogram-transform
  "Compute histogram bins and counts.

  Args:
  - dataset: The data to transform
  - x-col: X column key (for binning)
  - y-col: Y column key (ignored for histogram)
  - group-cols: Vector of grouping column keys (or nil if none)
  - opts: Options map with :bins (default 30)

  Returns:
  - Dataset with bin edges and counts"
  [dataset x-col y-col group-cols opts]
  (let [ds (ensure-dataset dataset)
        bins (get opts :bins 30)]
    (if (and ds x-col)
      (if (and group-cols (seq group-cols))
        ;; Grouped histogram
        (let [grouped (tc/group-by ds group-cols)
              group-data (get grouped :data)
              group-names (get grouped :name)

              hist-data
              (mapcat
               (fn [group-name group-ds]
                 (let [x-vals (column-values group-ds x-col)]
                   (when (seq x-vals)
                     (let [x-min (apply min x-vals)
                           x-max (apply max x-vals)
                           bin-width (/ (- x-max x-min) bins)
                           ;; Create bins
                           bin-edges (mapv #(+ x-min (* % bin-width)) (range (inc bins)))
                           ;; Count values in each bin
                           counts (mapv
                                   (fn [i]
                                     (let [left (nth bin-edges i)
                                           right (nth bin-edges (inc i))]
                                       (count (filter #(and (>= % left) (< % right)) x-vals))))
                                   (range bins))
                           ;; Use bin centers for x
                           bin-centers (mapv #(/ (+ (nth bin-edges %) (nth bin-edges (inc %))) 2.0)
                                             (range bins))]
                       (map (fn [x count]
                              (merge group-name {x-col x :count count}))
                            bin-centers counts)))))
               group-names group-data)]
          (tc/dataset hist-data))

        ;; Ungrouped histogram
        (let [x-vals (column-values ds x-col)]
          (if (seq x-vals)
            (let [x-min (apply min x-vals)
                  x-max (apply max x-vals)
                  bin-width (/ (- x-max x-min) bins)
                  bin-edges (mapv #(+ x-min (* % bin-width)) (range (inc bins)))
                  counts (mapv
                          (fn [i]
                            (let [left (nth bin-edges i)
                                  right (nth bin-edges (inc i))]
                              (count (filter #(and (>= % left) (< % right)) x-vals))))
                          (range bins))
                  bin-centers (mapv #(/ (+ (nth bin-edges %) (nth bin-edges (inc %))) 2.0)
                                    (range bins))]
              (tc/dataset {x-col bin-centers
                           :count counts}))
            ds)))
      ds)))

(defn density-transform
  "Compute kernel density estimation.

  Args:
  - dataset: The data to transform
  - x-col: X column key
  - y-col: Y column key (ignored)
  - group-cols: Vector of grouping column keys (or nil if none)

  Returns:
  - Dataset with x values and density estimates"
  [dataset x-col y-col group-cols]
  (let [ds (ensure-dataset dataset)]
    (if (and ds x-col)
      (if (and group-cols (seq group-cols))
        ;; Grouped density
        (let [grouped (tc/group-by ds group-cols)
              group-data (get grouped :data)
              group-names (get grouped :name)

              density-data
              (mapcat
               (fn [group-name group-ds]
                 (let [x-vals (column-values group-ds x-col)]
                   (when (seq x-vals)
                     (let [;; Simple KDE using Gaussian kernel
                           x-min (apply min x-vals)
                           x-max (apply max x-vals)
                           ;; Silverman's rule of thumb for bandwidth
                           n (count x-vals)
                           std-dev (stats/stddev x-vals)
                           bandwidth (* std-dev (Math/pow (* n 0.2) -0.2))
                           ;; Generate evaluation points
                           x-range (range x-min x-max (/ (- x-max x-min) 100.0))
                           x-range (concat x-range [x-max])
                           ;; Compute density at each point
                           densities (mapv
                                      (fn [x]
                                        (let [contributions
                                              (map (fn [xi]
                                                     (let [z (/ (- x xi) bandwidth)]
                                                       (/ (Math/exp (- (* 0.5 z z)))
                                                          (* bandwidth (Math/sqrt (* 2 Math/PI))))))
                                                   x-vals)]
                                          (/ (reduce + contributions) n)))
                                      x-range)]
                       (map (fn [x density]
                              (merge group-name {x-col x :density density}))
                            x-range densities)))))
               group-names group-data)]
          (tc/dataset density-data))

        ;; Ungrouped density
        (let [x-vals (column-values ds x-col)]
          (if (seq x-vals)
            (let [x-min (apply min x-vals)
                  x-max (apply max x-vals)
                  n (count x-vals)
                  std-dev (stats/stddev x-vals)
                  bandwidth (* std-dev (Math/pow (* n 0.2) -0.2))
                  x-range (range x-min x-max (/ (- x-max x-min) 100.0))
                  x-range (concat x-range [x-max])
                  densities (mapv
                             (fn [x]
                               (let [contributions
                                     (map (fn [xi]
                                            (let [z (/ (- x xi) bandwidth)]
                                              (/ (Math/exp (- (* 0.5 z z)))
                                                 (* bandwidth (Math/sqrt (* 2 Math/PI))))))
                                          x-vals)]
                                 (/ (reduce + contributions) n)))
                             x-range)]
              (tc/dataset {x-col (vec x-range)
                           :density densities}))
            ds)))
      ds)))

;;; =============================================================================
;;; Transformation dispatcher

(defn apply-transform
  "Apply a transformation to data.

  Args:
  - transform-spec: Transformation keyword, vector [keyword opts], or function
  - dataset: Data to transform
  - x-col: X column key
  - y-col: Y column key
  - group-cols: Vector of grouping column keys (or nil)

  Returns:
  - Transformed dataset"
  [transform-spec dataset x-col y-col group-cols]
  (cond
    ;; No transform
    (nil? transform-spec)
    (identity-transform dataset x-col y-col group-cols)

    ;; Vector transform with options [keyword opts]
    (vector? transform-spec)
    (let [[transform-kw opts] transform-spec]
      (case transform-kw
        :histogram (histogram-transform dataset x-col y-col group-cols opts)
        ;; Default: identity
        (identity-transform dataset x-col y-col group-cols)))

    ;; Keyword transform
    (keyword? transform-spec)
    (case transform-spec
      :linear (linear-transform dataset x-col y-col group-cols)
      :smooth (smooth-transform dataset x-col y-col group-cols)
      :density (density-transform dataset x-col y-col group-cols)
      :histogram (histogram-transform dataset x-col y-col group-cols {})
      :identity (identity-transform dataset x-col y-col group-cols)
      ;; Default: identity
      (identity-transform dataset x-col y-col group-cols))

    ;; Function transform (for custom transforms)
    (fn? transform-spec)
    (transform-spec dataset x-col y-col group-cols)

    ;; Default: identity
    :else
    (identity-transform dataset x-col y-col group-cols)))

(comment
  ;; REPL experiments
  (require '[tablecloth.api :as tc])

  ;; Test data
  (def test-data
    (tc/dataset {:x [1 2 3 4 5]
                 :y [2 4 3 5 6]}))

  ;; Test linear transform
  (linear-transform test-data :x :y nil)

  ;; Test with grouping
  (def grouped-data
    (tc/dataset {:x [1 2 3 4 5 6]
                 :y [2 4 3 5 6 4]
                 :group [:a :a :b :b :c :c]}))

  (linear-transform grouped-data :x :y :group))
