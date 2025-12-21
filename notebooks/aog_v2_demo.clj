;; # AoG v2 Demo: Normalized Representation
;;
;; Demonstration of v2 AoG API with normalized representation.
;;
;; This notebook shows:
;; 1. Side-by-side comparison of v1 vs v2
;; 2. Both algebraic and data-oriented styles in v2
;; 3. Real examples using the normalized API

(ns aog-v2-demo
  (:require [tableplot.v2.aog :as aog]
            [scicloj.tableplot.v1.aog.core :as aog-v1]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Sample Data

(def penguins
  {:bill-length [39.1 39.5 40.3 36.7 39.3 38.9 39.2 41.1]
   :bill-depth [18.7 17.4 18.0 19.3 20.6 17.8 19.6 17.6]
   :species [:adelie :adelie :adelie :adelie :chinstrap :chinstrap :gentoo :gentoo]
   :body-mass [3750 3800 3250 3450 3650 3625 4675 3200]})

(def simple-data
  {:x [1 2 3 4 5]
   :y [2 4 6 8 10]})

;; ## Example 1: Simple Scatter Plot
;;
;; Comparing v1 and v2 syntax for a basic scatter plot.

;; ### v1 (current)
;;
;; ```clojure
;; (aog-v1/draw
;;   (aog-v1/* (aog-v1/data simple-data)
;;             (aog-v1/mapping :x :y)
;;             (aog-v1/scatter {:alpha 0.7})))
;; ```

;; ### v2 algebraic style

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/scatter {:alpha 0.7})))

;; ### v2 data-oriented style

(aog/draw
 (aog/merge-layers (aog/data simple-data)
                   (aog/mapping :x :y)
                   (aog/scatter {:alpha 0.7})))

;; **Observation**: The v2 API is very similar to v1 - constructors return vectors internally.

;; ## Example 2: Multi-layer with Overlay
;;
;; Scatter plot with linear regression overlay.

;; ### v1 (current)
;;
;; ```clojure
;; (aog-v1/draw
;;   (aog-v1/* (aog-v1/data simple-data)
;;             (aog-v1/mapping :x :y)
;;             (aog-v1/+ (aog-v1/scatter {:alpha 0.5})
;;                       (aog-v1/linear))))
;; ```

;; ### v2 algebraic style

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))

;; ### v2 data-oriented style

(let [base (aog/merge-layers (aog/data simple-data)
                             (aog/mapping :x :y))
      plots (aog/concat-layers (aog/scatter {:alpha 0.5})
                               (aog/linear))]
  (aog/draw (aog/merge-layers base plots)))

;; **Note**: The `+` operator in both versions creates a collection of layers.
;; In v2, it's explicit: `(aog/+ [...] [...])` returns a vector.
;; In v1, it's implicit: `(aog-v1/+ ... ...)` also returns a vector, but input types vary.

;; ## Example 3: Color by Species
;;
;; Using the penguins dataset with color mapping.
;;
;; Note: Color aesthetics currently work with vegalite backend.
;; The thing-geom backend has limited color support.

;; ### v2 algebraic style

(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :bill-length :bill-depth)
        (aog/mapping {:color :species})
        (aog/scatter {:alpha 0.7}))
 {:backend :vegalite})

;; ### v2 algebraic style (combined mapping)

(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :bill-length :bill-depth {:color :species})
        (aog/scatter {:alpha 0.7})))

;; ### v2 data-oriented style

(aog/draw
 (aog/merge-layers (aog/data penguins)
                   (aog/mapping :bill-length :bill-depth {:color :species})
                   (aog/scatter {:alpha 0.7})))

;; **Advantage**: The combined mapping `(aog/mapping :x :y {:color :species})` 
;; is equally clear in v2, and we save one `merge-layers` call.

;; ## Example 4: Scatter + Regression, Colored by Species
;;
;; The most complex example: multiple aesthetics and multiple layers.

;; ### v2 algebraic style

(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :bill-length :bill-depth {:color :species})
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))

;; ### v2 data-oriented style (step-by-step)

(let [;; Step 1: Add data
      with-data (aog/data penguins)

      ;; Step 2: Add mappings
      with-mapping (aog/merge-layers
                    with-data
                    (aog/mapping :bill-length :bill-depth {:color :species}))

      ;; Step 3: Create multiple plot types
      scatter-layer (aog/merge-layers with-mapping (aog/scatter {:alpha 0.5}))
      linear-layer (aog/merge-layers with-mapping (aog/linear))

      ;; Step 4: Combine them
      all-layers (aog/concat-layers scatter-layer linear-layer)]

  (aog/draw all-layers))

;; **Data-oriented style advantage**: Each step is explicit and can be inspected.
;; You can see exactly what `with-data` contains, what `with-mapping` contains, etc.
;;
;; This is valuable for debugging and understanding the pipeline.

;; ## Example 5: Raw Data Style
;;
;; For advanced users who want full control, v2 provides `layers` which accepts raw layer maps.

(aog/draw
 (aog/layers
  {:transformation nil
   :data penguins
   :positional [:bill-length :bill-depth]
   :named {:color :species}
   :plottype :scatter
   :attributes {:alpha 0.5}}
  {:transformation :linear
   :data penguins
   :positional [:bill-length :bill-depth]
   :named {:color :species}
   :plottype :line
   :attributes {}}))

;; **Use case**: When you need to:
;; - Programmatically generate layers
;; - Store layer specs in a database
;; - Transform layers with standard Clojure functions (map, filter, etc.)

;; ## Example 6: Vector Operations
;;
;; Because v2 normalizes to vectors, you can use standard vector operations.

;; Create base layers
(def scatter-layers
  (aog/* (aog/data penguins)
         (aog/mapping :bill-length :bill-depth {:color :species})
         (aog/scatter {:alpha 0.5})))

;; Inspect the layers
scatter-layers

;; Use standard vector operations
(count scatter-layers) ;; => 1

(first scatter-layers) ;; => the layer map

;; Map over layers to change alpha
(def with-bigger-alpha
  (mapv #(assoc-in % [:attributes :alpha] 0.8)
        scatter-layers))

(aog/draw with-bigger-alpha)

;; Filter layers (trivial with 1 layer, but shows the pattern)
(def only-scatter
  (filterv #(= :scatter (:plottype %))
           scatter-layers))

;; Combine with other layers using `into`
(def with-linear
  (into scatter-layers
        (aog/* (aog/data penguins)
               (aog/mapping :bill-length :bill-depth {:color :species})
               (aog/linear))))

(aog/draw with-linear)

;; **Key insight**: Normalization enables standard Clojure idioms.
;; No special functions needed - just `map`, `filter`, `into`, etc.

;; ## Example 7: Conditional Layer Construction
;;
;; Building plots conditionally is cleaner with normalized vectors.

(defn make-plot [data show-regression? show-smooth?]
  (let [base (aog/* (aog/data data)
                    (aog/mapping :x :y {:color :species})
                    (aog/scatter {:alpha 0.5}))

        ;; Conditionally add layers
        with-regression (if show-regression?
                          (into base (aog/* (aog/data data)
                                            (aog/mapping :x :y {:color :species})
                                            (aog/linear)))
                          base)

        with-smooth (if show-smooth?
                      (into with-regression (aog/* (aog/data data)
                                                   (aog/mapping :x :y {:color :species})
                                                   (aog/smooth)))
                      with-regression)]

    (aog/draw with-smooth)))

;; Just scatter
(make-plot penguins false false)

;; Scatter + regression
(make-plot penguins true false)

;; Scatter + regression + smooth
(make-plot penguins true true)

;; **Advantage**: Using `into` to add conditional layers is natural and efficient.
;; No type checking needed - everything is a vector.

;; ## Summary
;;
;; For detailed comparison, see:
;; - `AOG_V2_SUMMARY.md` - Quick reference
;; - `docs/aog_normalization_analysis.md` - Full analysis
;; - `EXPLORATION_REPORT.md` - Executive summary

;; ### Key Takeaways
;;
;; 1. **Normalization simplifies implementation**: `*` operator reduced from 30 lines to 8 lines (-73%)
;; 2. **Cost is minimal**: Only 6-14 extra characters per plot
;; 3. **Three API styles**: Algebraic (concise), data-oriented (explicit), raw data (programmatic)
;; 4. **Standard Clojure operations**: Can use `map`, `filter`, `into` directly on layers
;; 5. **Predictable behavior**: Always vectors, no type checking needed
