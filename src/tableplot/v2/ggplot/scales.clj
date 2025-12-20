(ns tableplot.v2.ggplot.scales
  "Scale customization functions for ggplot2-style API.
  
  Provides functions to override default scale inference:
  - scale-color-manual - Custom color palette
  - scale-x-log10, scale-y-log10 - Logarithmic scales
  - scale-x-continuous, scale-y-continuous - Custom scale configuration
  - scale-x-sqrt, scale-y-sqrt - Square root scales
  
  These functions add scale specifications to :sub/map which override
  the default scale inference."
  (:require [tableplot.v2.dataflow :as df]))

;;; ============================================================================
;;; Color Scales
;;; ============================================================================

(defn scale-color-manual
  "Set a custom color palette.
  
  Args:
  - spec: Plot spec
  - values: Map of data values to colors, or vector of colors
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-color-manual spec {:setosa \"red\" 
                            :versicolor \"blue\" 
                            :virginica \"green\"})
  (scale-color-manual spec [\"#FF0000\" \"#00FF00\" \"#0000FF\"])"
  [spec values]
  (let [scale-spec (if (map? values)
                     {:type :ordinal
                      :domain (vec (keys values))
                      :range (vec (vals values))}
                     {:type :ordinal
                      :range (vec values)})]
    (df/add-substitution spec :=color-scale scale-spec)))

(defn scale-fill-manual
  "Set a custom fill palette (for bars, areas, etc.).
  
  Args:
  - spec: Plot spec
  - values: Map of data values to colors, or vector of colors
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-fill-manual spec {:A \"red\" :B \"blue\"})"
  [spec values]
  (let [scale-spec (if (map? values)
                     {:type :ordinal
                      :domain (vec (keys values))
                      :range (vec (vals values))}
                     {:type :ordinal
                      :range (vec values)})]
    (df/add-substitution spec :=fill-scale scale-spec)))

;;; ============================================================================
;;; Position Scales - Logarithmic
;;; ============================================================================

(defn scale-x-log10
  "Set logarithmic scale for x-axis (base 10).
  
  Args:
  - spec: Plot spec
  - opts: Optional map with:
    - :base - Log base (default 10)
    - :domain - Custom domain [min max]
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-x-log10 spec)
  (scale-x-log10 spec {:base 2})
  (scale-x-log10 spec {:domain [1 1000]})"
  ([spec]
   (scale-x-log10 spec {}))
  ([spec opts]
   (let [base (get opts :base 10)
         domain (get opts :domain)
         scale-spec (cond-> {:type :log :base base}
                      domain (assoc :domain domain))]
     (df/add-substitution spec :=x-scale scale-spec))))

(defn scale-y-log10
  "Set logarithmic scale for y-axis (base 10).
  
  Args:
  - spec: Plot spec
  - opts: Optional map with:
    - :base - Log base (default 10)
    - :domain - Custom domain [min max]
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-y-log10 spec)
  (scale-y-log10 spec {:base 2})"
  ([spec]
   (scale-y-log10 spec {}))
  ([spec opts]
   (let [base (get opts :base 10)
         domain (get opts :domain)
         scale-spec (cond-> {:type :log :base base}
                      domain (assoc :domain domain))]
     (df/add-substitution spec :=y-scale scale-spec))))

;;; ============================================================================
;;; Position Scales - Square Root
;;; ============================================================================

(defn scale-x-sqrt
  "Set square root scale for x-axis.
  
  Args:
  - spec: Plot spec
  - opts: Optional map with:
    - :domain - Custom domain [min max]
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-x-sqrt spec)
  (scale-x-sqrt spec {:domain [0 100]})"
  ([spec]
   (scale-x-sqrt spec {}))
  ([spec opts]
   (let [domain (get opts :domain)
         scale-spec (cond-> {:type :sqrt}
                      domain (assoc :domain domain))]
     (df/add-substitution spec :=x-scale scale-spec))))

(defn scale-y-sqrt
  "Set square root scale for y-axis.
  
  Args:
  - spec: Plot spec
  - opts: Optional map with:
    - :domain - Custom domain [min max]
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-y-sqrt spec)"
  ([spec]
   (scale-y-sqrt spec {}))
  ([spec opts]
   (let [domain (get opts :domain)
         scale-spec (cond-> {:type :sqrt}
                      domain (assoc :domain domain))]
     (df/add-substitution spec :=y-scale scale-spec))))

;;; ============================================================================
;;; Position Scales - Continuous (General)
;;; ============================================================================

(defn scale-x-continuous
  "Set continuous scale for x-axis with custom configuration.
  
  Args:
  - spec: Plot spec
  - opts: Map with scale options:
    - :domain - Custom domain [min max]
    - :range - Custom range [min max]
    - :nice - Round domain to nice values (default false)
    - :zero - Include zero in domain (default false)
    - :clamp - Clamp values outside domain (default false)
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-x-continuous spec {:domain [0 10]})
  (scale-x-continuous spec {:domain [0 10] :nice true})
  (scale-x-continuous spec {:zero true})"
  [spec opts]
  (let [scale-spec (merge {:type :linear} opts)]
    (df/add-substitution spec :=x-scale scale-spec)))

(defn scale-y-continuous
  "Set continuous scale for y-axis with custom configuration.
  
  Args:
  - spec: Plot spec
  - opts: Map with scale options:
    - :domain - Custom domain [min max]
    - :range - Custom range [min max]
    - :nice - Round domain to nice values (default false)
    - :zero - Include zero in domain (default false)
    - :clamp - Clamp values outside domain (default false)
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-y-continuous spec {:domain [0 100] :nice true})"
  [spec opts]
  (let [scale-spec (merge {:type :linear} opts)]
    (df/add-substitution spec :=y-scale scale-spec)))

;;; ============================================================================
;;; Position Scales - Discrete/Categorical
;;; ============================================================================

(defn scale-x-discrete
  "Set discrete/categorical scale for x-axis.
  
  Args:
  - spec: Plot spec
  - opts: Optional map with:
    - :limits - Vector of category values in desired order
    - :labels - Map of category values to display labels
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-x-discrete spec {:limits [:low :medium :high]})
  (scale-x-discrete spec {:labels {:A \"Group A\" :B \"Group B\"}})"
  ([spec]
   (scale-x-discrete spec {}))
  ([spec opts]
   (let [limits (get opts :limits)
         labels (get opts :labels)
         scale-spec (cond-> {:type :ordinal}
                      limits (assoc :domain limits)
                      labels (assoc :labels labels))]
     (df/add-substitution spec :=x-scale scale-spec))))

(defn scale-y-discrete
  "Set discrete/categorical scale for y-axis.
  
  Args:
  - spec: Plot spec
  - opts: Optional map with:
    - :limits - Vector of category values in desired order
    - :labels - Map of category values to display labels
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-y-discrete spec {:limits [:low :medium :high]})"
  ([spec]
   (scale-y-discrete spec {}))
  ([spec opts]
   (let [limits (get opts :limits)
         labels (get opts :labels)
         scale-spec (cond-> {:type :ordinal}
                      limits (assoc :domain limits)
                      labels (assoc :labels labels))]
     (df/add-substitution spec :=y-scale scale-spec))))

;;; ============================================================================
;;; Size Scales
;;; ============================================================================

(defn scale-size
  "Set custom size scale.
  
  Args:
  - spec: Plot spec
  - opts: Map with:
    - :range - Size range [min max] (e.g., [1 10])
    - :domain - Data domain [min max]
  
  Returns:
  - Updated plot spec
  
  Examples:
  (scale-size spec {:range [1 20]})
  (scale-size spec {:range [5 50] :domain [0 100]})"
  [spec opts]
  (let [scale-spec (merge {:type :linear} opts)]
    (df/add-substitution spec :=size-scale scale-spec)))

;;; ============================================================================
;;; Example Usage (commented out)
;;; ============================================================================

(comment
  (require '[tableplot.v2.ggplot :as gg])
  (require '[tech.v3.dataset :as ds])

  (def iris (ds/->dataset "path/to/iris.csv"))

  ;; Custom color palette
  (-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width :color :Species))
      (gg/geom-point)
      (scale-color-manual {:setosa "red"
                           :versicolor "blue"
                           :virginica "green"})
      gg/render)

  ;; Log scale
  (-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
      (gg/geom-point)
      (scale-x-log10)
      (scale-y-log10)
      gg/render)

  ;; Custom continuous scale
  (-> (gg/ggplot iris (gg/aes :x :Sepal.Length :y :Sepal.Width))
      (gg/geom-point)
      (scale-x-continuous {:domain [4 8] :nice true})
      (scale-y-continuous {:domain [2 5] :zero true})
      gg/render))
