;; # thi.ng/geom Backend Demo
;;
;; Demonstrating the new thi.ng/geom-viz backend for tableplot AoG.
;;
;; This backend provides:
;; - Native Clojure SVG generation
;; - Excellent polar coordinate support
;; - Full control over rendering
;; - Static SVG output

(ns tableplot-book.thing-geom-demo
  (:require [scicloj.tableplot.v1.aog.core :as aog]
            [scicloj.tableplot.v1.aog.thing-geom :as thing-geom]
            [scicloj.tableplot.v1.aog.processing :as processing]
            [scicloj.kindly.v4.kind :as kind]
            [scicloj.kindly.v4.api :as kindly]))

^:kindly/hide-code
(def md (comp kindly/hide-code kind/md))

(md "## Basic Plots

Let's start with simple examples using the thing-geom backend directly.")

(md "### Scatter Plot

A simple scatter plot with 5 points:")

(thing-geom/entry->svg
 {:plottype :scatter
  :positional [[1 2 3 4 5]
               [2 4 3 5 6]]
  :named {}}
 {:width 600 :height 400})

(md "### Line Plot

A sine wave:")

(let [x (range 0 10 0.1)
      y (map #(Math/sin %) x)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec x) (vec y)]
    :named {:stroke "#0af"
            :stroke-width 2}}))

(md "### Bar Chart

A bar chart with custom fill color:")

(thing-geom/entry->svg
 {:plottype :bar
  :positional [[1 2 3 4 5]
               [23 45 34 56 42]]
  :named {:fill "#5E9B4D"
          :bar-width 30}})

(md "## Integration with AoG API

Now let's use the full AoG API with thing-geom as the rendering backend.")

(md "### Scatter Plot via AoG

Create a layer using `aog/data`, `aog/mapping`, and `aog/scatter`, then render with thing-geom:")

(let [layer (aog/* (aog/data {:x [1 2 3 4 5]
                              :y [2 4 3 5 6]})
                   (aog/mapping :x :y)
                   (aog/scatter))
      entries (processing/layers->entries [layer])]
  (thing-geom/entries->svg entries))

(md "### Multi-layer Composition

Combining scatter and line plots on the same axes:")

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
  (thing-geom/entries->svg entries))

(md "## Polar Coordinates

This is where thing-geom truly shines! Native polar coordinate support with automatic polar axes.")

(md "### Polar Line Plot (Rose Diagram)

A 3-petaled rose in polar coordinates:")

(let [theta (range 0 (* 2 Math/PI) 0.1)
      r (map (fn [t] (+ 5 (* 3 (Math/sin (* 3 t))))) theta)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) (vec r)]
    :named {:stroke "#0af"
            :stroke-width 2}}
   {:polar true
    :width 600
    :height 600}))

(md "### Radar Chart

A radar/spider chart showing performance across 5 categories:")

(let [categories [0 1 2 3 4]
      values [0.8 0.6 0.9 0.7 0.5]
      theta (map #(* % (/ (* 2 Math/PI) 5)) categories)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) values]
    :named {:stroke "#5E9B4D"
            :stroke-width 3
            :fill "#5E9B4D"
            :opacity 0.3}}
   {:polar true
    :width 600
    :height 600}))

(md "### Complex Polar Plot

A 5-petaled rose:")

(let [theta (range 0 (* 2 Math/PI) 0.01)
      r (map (fn [t] (* 8 (Math/cos (* 5 t)))) theta)]
  (thing-geom/entry->svg
   {:plottype :line
    :positional [(vec theta) (vec r)]
    :named {:stroke "#e74c3c"
            :stroke-width 2}}
   {:polar true
    :width 600
    :height 600}))

(md "## Debugging and Validation

Utilities for inspecting the backend.")

(md "### Spec Inspection

View the internal thi.ng/geom spec (before rendering):")

(thing-geom/entries->thing-spec
 [{:plottype :scatter
   :positional [[1 2 3] [4 5 6]]
   :named {}}])

(md "### Plottype Validation

Check if a plottype is supported:")

(md "Valid plottype (`:scatter`):")
(thing-geom/validate-entry {:plottype :scatter :positional [] :named {}})

(md "Invalid plottype (`:unknown`):")
(thing-geom/validate-entry {:plottype :unknown :positional [] :named {}})

(md "## Summary

The **thi.ng/geom backend** successfully provides:

- ✅ Native Clojure SVG generation (no JavaScript dependency)
- ✅ **Excellent polar coordinate support** (radar charts, rose diagrams)
- ✅ Full integration with the AoG API
- ✅ Multi-layer compositions
- ✅ All basic plot types (scatter, line, bar)

This makes it ideal for:
- Scientific papers and reports (static SVG)
- Clojure-only environments
- Polar coordinate visualizations
- Fine-grained rendering control

### Comparison with Other Backends

| Feature | Vega-Lite | Plotly | thi.ng/geom |
|---------|-----------|--------|-------------|
| Polar coordinates | Limited | Excellent | **Excellent** ✨ |
| Interactivity | Excellent | Excellent | None (static) |
| Clojure-native | No (JS) | No (JS) | **Yes** ✨ |
| Dependencies | darkstar | cljplotly | Pure Clojure |

### Next Steps

1. Integrate with `aog/draw` to support `:backend` option
2. Add `(aog/polar)` layer function for cleaner polar API
3. Apply tableplot themes to thi.ng output
4. Test with real-world datasets
5. Add more plot types (area, contour, heatmap)")
