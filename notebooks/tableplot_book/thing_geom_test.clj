;; # thi.ng/geom Backend Test
;;
;; Testing the new thi.ng/geom-viz backend for tableplot AoG.
;;
;; This backend provides:
;; - Native Clojure SVG generation
;; - Excellent polar coordinate support
;; - Full control over rendering
;; - Static SVG output

(ns tableplot-book.thing-geom-test
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.thing-geom :as thing-geom]
            [scicloj.tableplot.v1.aog.ir :as ir]
            [scicloj.tableplot.v1.aog.processing :as processing]))

;; ## Basic Tests
;;
;; Let's start with simple examples to test the backend.

;; ### Test 1: Simple Scatter Plot

;; Create a simple scatter plot using the thing-geom backend directly
(def test-1-scatter
  (let [entry {:plottype :scatter
               :positional [[1 2 3 4 5]
                            [2 4 3 5 6]]
               :named {}}]
    ;; Test entry->svg conversion
    (thing-geom/entry->svg entry {:width 600 :height 400})))

;; ### Test 2: Line Plot

(def test-2-line
  (let [x (range 0 10 0.1)
        y (map #(Math/sin %) x)
        entry {:plottype :line
               :positional [(vec x) (vec y)]
               :named {:stroke "#0af"
                       :stroke-width 2}}]
    (thing-geom/entry->svg entry)))

;; ### Test 3: Bar Chart

(def test-3-bar
  (let [entry {:plottype :bar
               :positional [[1 2 3 4 5]
                            [23 45 34 56 42]]
               :named {:fill "#5E9B4D"
                       :bar-width 30}}]
    (thing-geom/entry->svg entry)))

;; ## Integration with AoG API
;;
;; Now let's test using the full AoG API with the thing-geom backend.

;; First, we need to update the core.clj to support backend selection.
;; For now, let's test the conversion pipeline manually.

;; ### Test 4: AoG Layer to thing-geom

;; Create an AoG layer
(def test-4-aog-scatter
  (let [layer (aog/* (aog/data {:x [1 2 3 4 5]
                                :y [2 4 3 5 6]})
                     (aog/mapping :x :y)
                     (aog/scatter))

        ;; Process the layer to get entries
        entries (processing/layers->entries [layer])]

    ;; Render with thing-geom
    (thing-geom/entries->svg entries)))

;; ### Test 5: Multi-layer Plot

;; Scatter + Line
(def test-5-multi-layer
  (let [data {:x [1 2 3 4 5]
              :y [2 4 3 5 6]}

        layers [(aog/* (aog/data data)
                       (aog/mapping :x :y)
                       (aog/scatter)
                       (aog/visual {:color "#0af"}))

                (aog/* (aog/data data)
                       (aog/mapping :x :y)
                       (aog/line)
                       (aog/visual {:stroke "#f80"
                                    :stroke-width 2}))]

        entries (processing/layers->entries layers)]

    (thing-geom/entries->svg entries)))

;; ## Polar Coordinate Tests
;;
;; This is where thing-geom shines!

;; ### Test 6: Polar Line Plot

(def test-6-polar-line
  (let [theta (range 0 (* 2 Math/PI) 0.1)
        r (map (fn [t] (+ 5 (* 3 (Math/sin (* 3 t))))) theta)

        entry {:plottype :line
               :positional [(vec theta) (vec r)]
               :named {:stroke "#0af"
                       :stroke-width 2}}]

    ;; Render with polar coordinates!
    (thing-geom/entry->svg entry {:polar true
                                  :width 600
                                  :height 600})))

;; ### Test 7: Radar Chart Data

;; Simple radar chart with 5 categories
(def test-7-radar
  (let [categories [0 1 2 3 4]
        values [0.8 0.6 0.9 0.7 0.5]

        ;; Convert to angular coordinates
        theta (map #(* % (/ (* 2 Math/PI) 5)) categories)

        entry {:plottype :line
               :positional [(vec theta) values]
               :named {:stroke "#5E9B4D"
                       :stroke-width 3
                       :fill "#5E9B4D"
                       :opacity 0.3}}]

    (thing-geom/entry->svg entry {:polar true
                                  :width 600
                                  :height 600})))

;; ## Debugging Utilities

;; View the thing-geom spec (not SVG)
(def test-debug-spec
  (let [entry {:plottype :scatter
               :positional [[1 2 3] [4 5 6]]
               :named {}}]

    (thing-geom/entries->thing-spec [entry])))

;; ## Validation

;; Check if plottypes are supported
(def test-validate-scatter
  (thing-geom/validate-entry {:plottype :scatter :positional [] :named {}}))
;; => {:valid? true}

(def test-validate-unknown
  (thing-geom/validate-entry {:plottype :unknown :positional [] :named {}}))
;; => {:valid? false :error "..."}

;; ## Next Steps
;;
;; 1. Update aog/core.clj to support :backend option in draw
;; 2. Add polar coordinate layer function (aog/polar)
;; 3. Test with real-world datasets
;; 4. Compare output with Vega-Lite backend
;; 5. Add theme support
