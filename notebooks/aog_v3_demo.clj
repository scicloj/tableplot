;; # AoG v3 Demo: Flat Layer Structure with Namespaced Keys
;;
;; This notebook demonstrates the v3 AlgebraOfGraphics API, which achieves
;; the goal of building plot specs with standard Clojure operations.
;;
;; ## Key Innovation
;;
;; v3 uses a **flat layer structure** with **:aog namespaced keys**.
;; All aesthetics are top-level keys in the :aog namespace, which allows
;; **standard `merge`** to work without any custom logic while preventing
;; collisions with user data columns!

(ns aog-v3-demo
  (:require [tableplot.v3.aog :as aog]
            [scicloj.kindly.v4.kind :as kind]))

;; ## Sample Data

(def simple-data
  {:x [1 2 3 4 5]
   :y [2 4 6 8 10]})

(def penguins
  {:bill-length [39.1 39.5 40.3 36.7 39.3 38.9 39.2 41.1]
   :bill-depth [18.7 17.4 18.0 19.3 20.6 17.8 19.6 17.6]
   :species [:adelie :adelie :adelie :adelie :chinstrap :chinstrap :gentoo :gentoo]
   :body-mass [3750 3800 3250 3450 3650 3625 4675 3200]})

;; ## Example 1: Standard Merge Works!
;;
;; The breakthrough: you can use standard `merge` to build plot specs.
;; Constructor functions return maps with :aog/* keys.

;; Layer components as simple maps
(def data-part (aog/data simple-data))
(def mapping-part (aog/mapping :x :y))
(def visual-part (aog/scatter {:alpha 0.7}))

;; Inspect them - notice the #:aog namespace map syntax
data-part
mapping-part
visual-part

;; Standard merge combines them into a complete layer!
(def layer (merge data-part mapping-part visual-part))
layer

;; Draw it (wrap in vector for draw function)
(aog/draw [layer])

;; ## Example 2: Using the * Operator
;;
;; The * operator is just a wrapper around merge with distribution logic.

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/scatter {:alpha 0.7})))

;; ## Example 3: Multi-Layer with +
;;
;; The + operator creates multiple layers, and * distributes over them.

(aog/draw
 (aog/* (aog/data simple-data)
        (aog/mapping :x :y)
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))

;; ## Example 4: Standard `assoc` Works
;;
;; Add aesthetics with standard `assoc`.

(def base-layer (merge (aog/data penguins)
                       (aog/mapping :bill-length :bill-depth)
                       (aog/scatter)))

;; Add color aesthetic with standard assoc using :aog/color key
(def layer-with-color (assoc base-layer :aog/color :species))

layer-with-color

(aog/draw [layer-with-color])

;; ## Example 5: Standard `update` Works
;;
;; Modify values with standard `update`.

(def with-alpha (merge (aog/data simple-data)
                       (aog/mapping :x :y)
                       (aog/scatter {:alpha 0.3})))

;; Double the alpha using standard update on :aog/alpha key
(def more-alpha (update with-alpha :aog/alpha * 2))

more-alpha

(aog/draw [more-alpha])

;; ## Example 6: Standard `mapv` for Bulk Transformations
;;
;; Transform all layers with standard `mapv`.

(def multi-layer
  (aog/* (aog/data penguins)
         (aog/mapping :bill-length :bill-depth {:color :species})
         (aog/+ (aog/scatter)
                (aog/linear))))

;; Add alpha to all layers using :aog/alpha key
(def all-with-alpha
  (mapv #(assoc % :aog/alpha 0.6) multi-layer))

(aog/draw all-with-alpha)

;; ## Example 7: Standard `into` for Conditional Layers
;;
;; Build plots conditionally with standard `into`.

(defn make-plot [data show-regression? show-smooth?]
  (let [base (aog/* (aog/data data)
                    (aog/mapping :bill-length :bill-depth {:color :species})
                    (aog/scatter {:alpha 0.5}))

        with-regression (if show-regression?
                          (into base
                                (aog/* (aog/data data)
                                       (aog/mapping :bill-length :bill-depth {:color :species})
                                       (aog/linear)))
                          base)

        with-smooth (if show-smooth?
                      (into with-regression
                            (aog/* (aog/data data)
                                   (aog/mapping :bill-length :bill-depth {:color :species})
                                   (aog/smooth)))
                      with-regression)]

    (aog/draw with-smooth)))

;; Just scatter
(make-plot penguins false false)

;; Scatter + regression
(make-plot penguins true false)

;; Scatter + regression + smooth
(make-plot penguins true true)

;; ## Example 8: Standard `filter` for Layer Selection
;;
;; Filter layers with standard `filterv`.

(def many-layers
  (aog/* (aog/data simple-data)
         (aog/mapping :x :y)
         (aog/+ (aog/scatter)
                (aog/line)
                (aog/bar))))

;; Keep only scatter and line (check :aog/plottype key)
(def filtered (filterv #(#{:scatter :line} (:aog/plottype %)) many-layers))

(aog/draw filtered)

;; ## Example 9: Working Directly with Maps
;;
;; You can create layers without any helper functions!
;; Use the #:aog namespace map syntax for conciseness.

;; Manual layer - just a map with :aog/* keys
(def manual-layer
  #:aog{:data simple-data
        :x :x
        :y :y
        :plottype :scatter
        :alpha 0.8})

(aog/draw [manual-layer])

;; Merge multiple maps manually
(def manual-merged
  (merge #:aog{:data penguins}
         #:aog{:x :bill-length :y :bill-depth :color :species}
         #:aog{:plottype :scatter :alpha 0.6}))

(aog/draw [manual-merged])

;; ## Example 10: Value Type Determines Usage
;;
;; Keywords = mappings to columns
;; Numbers/Strings = constant attributes

(def layer-with-both
  #:aog{:data penguins
        :x :bill-length ;; Mapping (keyword -> column name)
        :y :bill-depth ;; Mapping (keyword -> column name)
        :color :species ;; Mapping (keyword -> column name)
        :size :body-mass ;; Mapping (keyword -> column name)
        :alpha 0.5 ;; Attribute (number -> constant)
        :plottype :scatter})

(aog/draw [layer-with-both])

;; You can override a mapping with a constant
(def with-constant-color
  (assoc layer-with-both :aog/color "red")) ;; String = constant color

(aog/draw [with-constant-color])

;; ## Example 11: Faceting as Standard Aesthetics
;;
;; Faceting works just like any other aesthetic!

(def with-facets
  (merge #:aog{:data penguins
               :x :bill-length
               :y :bill-depth
               :plottype :scatter}
         #:aog{:col :species})) ;; Facet aesthetic - just another key!

(aog/draw [with-facets])

;; Or using the helper function
(def base-plot
  (aog/* (aog/data penguins)
         (aog/mapping :bill-length :bill-depth)
         (aog/scatter)))

(def faceted-plot
  (aog/facet base-plot #:aog{:row :species}))

(aog/draw faceted-plot)

;; ## Example 12: Programmatic Layer Generation
;;
;; Generate layers programmatically using standard Clojure.

;; Create multiple plots with different alpha values
(def alpha-variants
  (mapv (fn [alpha]
          #:aog{:data simple-data
                :x :x
                :y :y
                :plottype :scatter
                :alpha alpha})
        [0.3 0.5 0.7 0.9]))

;; Display them all
(doseq [layer alpha-variants]
  (aog/draw [layer]))

;; ## Example 13: Layer Composition with Threading
;;
;; Use threading macros for readable layer building.

(def threaded-layer
  (-> {}
      (merge (aog/data penguins))
      (merge (aog/mapping :bill-length :bill-depth))
      (assoc :aog/color :species)
      (assoc :aog/size :body-mass)
      (merge (aog/scatter))
      (assoc :aog/alpha 0.7)))

(aog/draw [threaded-layer])

;; ## Example 14: Namespace Map Syntax
;;
;; The #:aog{...} syntax is a reader feature that expands to :aog/* keys.
;; It's equivalent to writing out each key with the :aog/ prefix.

;; These are equivalent:
(def explicit
  {:aog/data penguins
   :aog/x :bill-length
   :aog/y :bill-depth
   :aog/plottype :scatter})

(def with-syntax
  #:aog{:data penguins
        :x :bill-length
        :y :bill-depth
        :plottype :scatter})

;; They're equal!
(= explicit with-syntax) ;; => true

;; ## Example 15: Why Namespaced Keys?
;;
;; Namespacing prevents collisions with user data columns.

;; Imagine your data has a column named :plottype or :data
(def tricky-data
  {:x [1 2 3]
   :y [4 5 6]
   :plottype [:a :b :c] ;; Column named :plottype!
   :data [10 20 30]}) ;; Column named :data!

;; Without namespacing, this would be ambiguous:
;; {:data tricky-data :plottype :scatter :y :plottype}
;; Is :plottype the plot type or a column mapping?

;; With :aog namespace, it's clear:
(def no-confusion
  #:aog{:data tricky-data ;; Layer metadata
        :x :x
        :y :plottype ;; Mapping to the :plottype column!
        :plottype :scatter}) ;; Plot type

(aog/draw [no-confusion])

;; ## Example 16: Comparing v3 with v2
;;
;; v2 required custom merge-layer-pair with special logic.
;; v3 uses standard merge - that's it!

;; v2 layer structure (nested):
(def v2-example
  {:transformation nil
   :data {:x [1 2] :y [3 4]}
   :positional [:x :y] ;; Vector - needs concatenation
   :named {:color :species} ;; Map - needs merge
   :attributes {:alpha 0.5}}) ;; Map - needs merge

;; v3 layer structure (flat with namespace):
(def v3-example
  #:aog{:data penguins
        :x :bill-length ;; All at top level
        :y :bill-depth
        :color :species
        :alpha 0.5
        :plottype :scatter})

;; Standard merge works in v3!
(merge #:aog{:data penguins :x :bill-length}
       #:aog{:y :bill-depth :color :species}
       #:aog{:plottype :scatter :alpha 0.5})

;; ## Summary
;;
;; ### v3 Achievements
;;
;; 1. **Standard `merge` works** - No custom merge logic needed
;; 2. **Standard `assoc`/`update` work** - Direct manipulation of layers
;; 3. **Standard `mapv`/`filter`/`into` work** - Bulk transformations
;; 4. **Simpler implementation** - 17 lines vs 30 lines for * operator
;; 5. **Simpler mental model** - Everything at same level
;; 6. **No collision risk** - :aog/* namespace prevents conflicts with data columns
;; 7. **Three API styles**:
;;    - Algebraic: `(* (data df) (mapping :x :y) (scatter))`
;;    - Standard Clojure: `(merge (data df) (mapping :x :y) (scatter))`
;;    - Raw maps: `#:aog{:data df :x :x :y :y :plottype :scatter}`
;;
;; ### Key Design Principles
;;
;; 1. **Flat structure** - All aesthetics are top-level keys
;; 2. **Namespaced keys** - :aog/* prevents collisions, enables specs
;; 3. **Value type determines usage**:
;;    - Keyword -> mapping to data column
;;    - Number/String -> constant attribute
;; 4. **Everything is just data** - No special types, just maps and vectors
;; 5. **Standard operations work** - Use regular Clojure, no learning curve
;;
;; ### Namespace Benefits
;;
;; - **Collision prevention**: Your data can have :x, :y, :color columns without conflicts
;; - **Clear intent**: :aog/plottype is clearly metadata, not a data column
;; - **Spec-friendly**: Can define specs like `(s/def :aog/plottype #{:scatter :line ...})`
;; - **Concise syntax**: `#:aog{...}` is readable and not much longer than plain maps
;; - **Standard Clojure**: Follows Ring 2.0, Datomic, and modern Clojure conventions
;;
;; ### Performance Notes
;;
;; - No performance penalty - standard merge is fast
;; - Actually simpler implementation = potentially faster
;; - Fewer function calls in the merge path
;; - Namespaced keyword access is just as fast as plain keywords
;;
;; For more details, see:
;; - `docs/v3_flat_layer_design.md` - Design rationale
;; - `docs/v3_namespaced_keys_analysis.md` - Namespace decision analysis
;; - `docs/v3_namespace_comparison.md` - :aog vs :tableplot.v3.aog comparison
