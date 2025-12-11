(ns scicloj.tableplot.v1.aog.scales
  "Scale inference and management for AlgebraOfGraphics.
  
  Handles automatic generation of scales from data:
  - Categorical scales (color, shape, linetype)
  - Continuous scales (size, alpha, etc.)"
  (:require [scicloj.tableplot.v1.aog.ir :as ir]))

;;; =============================================================================
;;; Default color palettes

(def default-categorical-colors
  "Default categorical color palette (Plotly/D3 colors)."
  ["#1f77b4" "#ff7f0e" "#2ca02c" "#d62728" "#9467bd"
   "#8c564b" "#e377c2" "#7f7f7f" "#bcbd22" "#17becf"])

(def default-continuous-color-scale
  "Default continuous color scale."
  {:low "#440154" ; viridis
   :mid "#21918c"
   :high "#fde724"})

;;; =============================================================================
;;; Scale inference

(defn- infer-categorical-domain
  "Infer domain from data values (unique values, sorted)."
  [values]
  (vec (sort (distinct values))))

(defn- infer-continuous-domain
  "Infer domain from numeric data (min/max)."
  [values]
  (when (seq values)
    (let [nums (filter number? values)]
      (when (seq nums)
        [(apply min nums) (apply max nums)]))))

(defn infer-categorical-scale
  "Infer a categorical scale from data.
  
  Args:
  - id: Scale identifier keyword
  - aesthetic: Aesthetic name (:color, :shape, etc.)
  - values: Data values (vector)
  
  Returns:
  - CategoricalScale map"
  [id aesthetic values]
  (let [domain (infer-categorical-domain values)
        ;; Generate range based on aesthetic
        range (case aesthetic
                (:color :colour)
                (take (count domain) (cycle default-categorical-colors))

                :shape
                (take (count domain) (cycle [:circle :square :diamond :cross :triangle-up]))

                :linetype
                (take (count domain) (cycle [:solid :dash :dot :dashdot]))

                ;; Default: just use domain values
                domain)]
    {:id id
     :aesthetic aesthetic
     :domain domain
     :range (vec range)}))

(defn infer-continuous-scale
  "Infer a continuous scale from data.
  
  Args:
  - id: Scale identifier keyword
  - aesthetic: Aesthetic name (:size, :alpha, etc.)
  - values: Data values (vector)
  
  Returns:
  - ContinuousScale map"
  [id aesthetic values]
  (let [domain (infer-continuous-domain values)
        ;; Generate range based on aesthetic
        range (case aesthetic
                :size [5 20] ; marker size range
                :alpha [0.3 1.0] ; alpha/opacity range
                :width [1 5] ; line width range
                domain)] ; default: use domain
    {:id id
     :aesthetic aesthetic
     :domain (or domain [0 1])
     :range range}))

(defn infer-scales
  "Infer all scales from a collection of entries.
  
  Analyzes the named attributes across all entries to determine
  which scales are needed.
  
  Args:
  - entries: Vector of Entry maps
  
  Returns:
  - Map with :categorical-scales and :continuous-scales"
  [entries]
  (let [;; Collect all named attributes across entries
        all-named (mapcat (fn [entry]
                            (for [[k v] (:named entry)]
                              [k v]))
                          entries)

        ;; Group by aesthetic
        by-aesthetic (group-by first all-named)

        ;; Separate categorical and continuous
        categorical-aesthetics #{:color :colour :shape :linetype :group}

        categorical-scales
        (into {}
              (for [[aesthetic pairs] by-aesthetic
                    :when (categorical-aesthetics aesthetic)]
                (let [all-values (mapcat (fn [[_ v]]
                                           (if (vector? v) v [v]))
                                         pairs)
                      ;; Only create scale if values are not all numbers or keywords
                      ;; (i.e., they're categorical data)
                      non-numeric? (some #(not (number? %)) all-values)]
                  (when (and (seq all-values) non-numeric?)
                    (let [scale-id (keyword (str (name aesthetic) "-scale"))]
                      [scale-id (infer-categorical-scale scale-id aesthetic all-values)])))))

        continuous-scales
        (into {}
              (for [[aesthetic pairs] by-aesthetic
                    :when (not (categorical-aesthetics aesthetic))]
                (let [all-values (mapcat (fn [[_ v]]
                                           (if (vector? v) v [v]))
                                         pairs)
                      numeric-values (filter number? all-values)]
                  (when (seq numeric-values)
                    (let [scale-id (keyword (str (name aesthetic) "-scale"))]
                      [scale-id (infer-continuous-scale scale-id aesthetic numeric-values)])))))]

    {:categorical-scales (into {} (remove nil? categorical-scales))
     :continuous-scales (into {} (remove nil? continuous-scales))}))

(defn apply-categorical-scale
  "Apply a categorical scale to map domain values to range values.
  
  Args:
  - scale: CategoricalScale map
  - values: Vector of domain values
  
  Returns:
  - Vector of range values"
  [scale values]
  (let [domain->range (zipmap (:domain scale) (:range scale))]
    (mapv #(get domain->range % (first (:range scale))) values)))

(defn apply-continuous-scale
  "Apply a continuous scale to map domain values to range values.
  
  Args:
  - scale: ContinuousScale map  
  - values: Vector of domain values
  
  Returns:
  - Vector of range values"
  [scale values]
  (let [[domain-min domain-max] (:domain scale)
        [range-min range-max] (:range scale)
        domain-span (- domain-max domain-min)
        range-span (- range-max range-min)]
    (if (zero? domain-span)
      (vec (repeat (count values) range-min))
      (mapv (fn [v]
              (let [normalized (/ (- v domain-min) domain-span)]
                (+ range-min (* normalized range-span))))
            values))))

(comment
  ;; REPL experiments

  ;; Test categorical scale inference
  (def cat-scale
    (infer-categorical-scale :color-1 :color [:a :b :c :a :b]))
  ;; => {:id :color-1
  ;;     :aesthetic :color
  ;;     :domain [:a :b :c]
  ;;     :range ["#1f77b4" "#ff7f0e" "#2ca02c"]}

  ;; Apply categorical scale
  (apply-categorical-scale cat-scale [:a :b :c :a])
  ;; => ["#1f77b4" "#ff7f0e" "#2ca02c" "#1f77b4"]

  ;; Test continuous scale inference
  (def cont-scale
    (infer-continuous-scale :size-1 :size [10 20 30 40 50]))
  ;; => {:id :size-1
  ;;     :aesthetic :size
  ;;     :domain [10 50]
  ;;     :range [5 20]}

  ;; Apply continuous scale
  (apply-continuous-scale cont-scale [10 30 50])
  ;; => [5.0 12.5 20.0]
  )
