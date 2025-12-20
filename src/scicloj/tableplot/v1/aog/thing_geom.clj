(ns scicloj.tableplot.v1.aog.thing-geom
  "thi.ng/geom-viz backend for AlgebraOfGraphics.

  Converts Entry IR to thi.ng/geom-viz specifications for native SVG rendering.
  
  Key features:
  - Native Clojure SVG generation (no JavaScript)
  - Excellent polar coordinate support (svg-plot2d-polar)
  - Full control over rendering
  - Static SVG output (no interactivity)
  - ggplot2-compatible themes
  
  This backend is ideal for:
  - Scientific papers (static SVG/PDF)
  - Notebooks (when interactivity not needed)
  - Print reports
  - Polar coordinate visualizations (radar charts, rose diagrams)"
  (:require [scicloj.tableplot.v1.aog.ir :as ir]
            [scicloj.tableplot.v1.aog.thing-geom-themes :as themes]
            [thi.ng.geom.viz.core :as viz]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.geom.vector :as v]
            [thi.ng.color.core :as col]
            [thi.ng.math.core :as m]
            [scicloj.kindly.v4.kind :as kind]))

;;; =============================================================================
;;; Plottype mapping

(def plottype->layout
  "Map AoG plot types to thi.ng/geom-viz layout functions."
  {:scatter viz/svg-scatter-plot
   :line viz/svg-line-plot
   :bar viz/svg-bar-plot
   :area viz/svg-area-plot
   :heatmap viz/svg-heatmap
   :contour viz/svg-contour-plot
   ;; Histogram maps to bar with binning (handled in data transform)
   :histogram viz/svg-bar-plot
   ;; Density maps to area/line with KDE (handled in data transform)
   :density viz/svg-line-plot
   ;; Radar plot uses special handling
   :radar viz/svg-radar-plot})

;;; =============================================================================
;;; Data format conversion

(defn- positional->thing-values
  "Convert AoG positional data to thi.ng format.
  
  AoG format: [[x1 x2 x3] [y1 y2 y3]]
  thi.ng format: [[x1 y1] [x2 y2] [x3 y3]]
  
  Returns vector of tuples."
  [positional]
  (if (empty? positional)
    []
    (let [arrays (vec positional)
          n (count (first arrays))]
      (vec (for [i (range n)]
             (vec (map #(nth % i) arrays)))))))

(defn- infer-domain
  "Infer domain [min max] from data values."
  [values]
  (if (empty? values)
    [0 1]
    (let [nums (filter number? values)]
      (if (empty? nums)
        [0 (count values)]
        [(apply min nums) (apply max nums)]))))

(defn- infer-range-from-domain
  "Infer reasonable pixel range from data domain.
  Uses a standard 600x400 plot area with margins."
  [domain axis]
  (case axis
    :x [50 550] ; 50px left margin, 50px right margin
    :y [350 50] ; Inverted for SVG coords (350px bottom, 50px top)
    [0 500]))

(defn- build-axis
  "Build a thi.ng/geom-viz axis specification from data.
  
  Args:
  - data-array: Vector of data values
  - axis-type: :x or :y
  - scale-type: :linear or :log (default :linear)
  
  Returns:
  - Axis spec map for viz/linear-axis or viz/log-axis, or nil if data-array is empty"
  [data-array axis-type scale-type]
  (when (and data-array (seq data-array))
    (let [domain (infer-domain data-array)
          range (infer-range-from-domain domain axis-type)
          axis-fn (case scale-type
                    :log viz/log-axis
                    viz/linear-axis)
          ;; Position: where the axis line is drawn
          ;; For X axis: Y position (bottom of plot area = 350)
          ;; For Y axis: X position (left of plot area = 50)
          pos (case axis-type
                :x 350 ; X axis at bottom
                :y 50) ; Y axis at left

          ;; Calculate major tick spacing (~4-5 major ticks)
          domain-range (- (second domain) (first domain))
          major-spacing (/ domain-range 4)

          ;; Minor ticks: place 1 minor tick between each major tick
          ;; So minor spacing is major/2
          minor-spacing (/ major-spacing 2)]

      (axis-fn
       {:domain domain
        :range range
        :pos pos
        :major major-spacing
        :minor minor-spacing}))))

;;; =============================================================================
;;; Entry conversion

(defn- entry->thing-spec
  "Convert a single Entry to a thi.ng/geom-viz specification.
  
  Args:
  - entry: Entry map {:plottype, :positional, :named}
  - theme: Theme keyword or map (default :grey)
  
  Returns:
  - thi.ng/geom-viz spec map ready for svg-plot2d-cartesian or svg-plot2d-polar"
  [entry & [theme]]
  (let [{:keys [plottype positional named]} entry
        layout-fn (plottype->layout plottype)

        ;; Get theme config
        theme-config (themes/get-theme (or theme :grey))

        ;; Convert data format
        values (positional->thing-values positional)

        ;; Extract scale info from named (if provided)
        x-scale-type (get-in named [:x-scale :type] :linear)
        y-scale-type (get-in named [:y-scale :type] :linear)

        ;; Build axes from data
        x-axis-from-data (when (seq positional)
                           (build-axis (first positional) :x x-scale-type))
        y-axis-from-data (when (>= (count positional) 2)
                           (build-axis (second positional) :y y-scale-type))

        ;; Provide default axes if data axes are nil (thi.ng requires non-nil axes)
        ;; IMPORTANT: major, minor, and pos must all be provided
        x-axis (or x-axis-from-data
                   (viz/linear-axis {:domain [0 1]
                                     :range [50 550]
                                     :pos 350 ; Y position for X axis
                                     :major 0.2
                                     :minor 0.1}))
        y-axis (or y-axis-from-data
                   (viz/linear-axis {:domain [0 1]
                                     :range [350 50]
                                     :pos 50 ; X position for Y axis
                                     :major 0.2
                                     :minor 0.1}))

        ;; Extract visual attributes (user-specified take precedence over theme defaults)
        default-color (themes/get-default-data-color theme-config)
        default-stroke-width (themes/get-default-stroke-width theme-config)
        default-opacity (themes/get-default-opacity theme-config)

        stroke (or (:stroke named)
                   (:color named)
                   default-color)
        fill (or (:fill named)
                 (when (#{:area :bar} plottype) stroke)
                 (when (= :scatter plottype) stroke)
                 "none")
        opacity (or (:alpha named) (:opacity named) default-opacity)
        stroke-width (or (:stroke-width named)
                         (:linewidth named)
                         default-stroke-width)

        ;; Build attribute map for thi.ng
        attribs {:stroke stroke
                 :fill fill
                 :opacity opacity
                 :stroke-width stroke-width}

        ;; Build data spec
        data-spec {:values values
                   :layout layout-fn
                   :attribs attribs}

        ;; Add bar-width for bar charts
        data-spec (if (= :bar plottype)
                    (assoc data-spec :bar-width (or (:bar-width named) 20))
                    data-spec)

        ;; Apply theme to grid
        grid-spec (themes/apply-theme-to-grid theme-config)

        ;; Build complete spec with themed grid
        spec {:x-axis x-axis
              :y-axis y-axis
              :grid grid-spec
              :data [data-spec]}]

    spec))

(defn entry->svg
  "Convert a single Entry to SVG (wrapped with kind/html for Clay rendering).
  
  Args:
  - entry: Entry map {:plottype, :positional, :named}
  - opts: Optional map with:
    - :width (default from theme or 600)
    - :height (default from theme or 400)
    - :polar (default false) - use polar coordinates
    - :theme (default :grey) - theme keyword or map
  
  Returns:
  - Kindly-wrapped SVG for Clay rendering"
  [entry & [opts]]
  (let [theme (or (:theme opts) :grey)
        theme-config (themes/get-theme theme)

        ;; Merge theme defaults with opts
        width (or (:width opts) (get-in theme-config [:plot :width]) 600)
        height (or (:height opts) (get-in theme-config [:plot :height]) 400)
        polar? (or (:polar opts) false)

        ;; Generate spec with theme
        spec (entry->thing-spec entry theme)

        ;; Choose plot function
        plot-fn (if polar?
                  viz/svg-plot2d-polar
                  viz/svg-plot2d-cartesian)

        ;; Add origin for polar plots
        spec (if polar?
               (assoc spec :origin (v/vec2 (/ width 2) (/ height 2))
                      :circle true)
               spec)

        ;; Get panel background from theme
        panel-bg (get-in theme-config [:panel :background] "white")
        plot-bg (get-in theme-config [:plot :background] "white")

        ;; Extract axis ranges from spec to calculate panel dimensions
        x-range (get-in spec [:x-axis :range])
        y-range (get-in spec [:y-axis :range])

        ;; Calculate panel background position and dimensions from axis ranges
        ;; x-range is [x-min x-max], y-range is [y-max y-min] (SVG y is inverted)
        panel-x (first x-range)
        panel-y (second y-range)
        panel-width (- (second x-range) (first x-range))
        panel-height (- (first y-range) (second y-range))

        ;; Generate SVG plot
        plot (plot-fn spec)

        ;; Add background rectangles (plot background, then panel background, then plot)
        svg-doc (svg/svg {:width width :height height}
                  ;; Plot background (full area)
                         (svg/rect [0 0] width height {:fill plot-bg})
                  ;; Panel background (plot area defined by axis ranges)
                         (when (not= panel-bg plot-bg)
                           (svg/rect [panel-x panel-y] panel-width panel-height {:fill panel-bg}))
                  ;; The actual plot
                         plot)

        svg-string (svg/serialize svg-doc)]

    ;; Wrap with kind/html for proper Clay rendering
    (kind/html svg-string)))

(defn entries->svg
  "Convert multiple entries to layered SVG (wrapped with kind/html for Clay rendering).
  
  For multiple entries, combines data specs into a single plot.
  
  Args:
  - entries: Vector of Entry maps
  - opts: Optional map (see entry->svg)
  
  Returns:
  - Kindly-wrapped SVG for Clay rendering"
  [entries & [opts]]
  (if (= 1 (count entries))
    ;; Single entry - simple case
    (entry->svg (first entries) opts)

    ;; Multiple entries - combine data specs
    (let [theme (or (:theme opts) :grey)
          theme-config (themes/get-theme theme)

          ;; Generate specs with theme
          specs (map #(entry->thing-spec % theme) entries)

          ;; Use axes from first entry (assumes same domain)
          ;; In a more sophisticated version, we'd merge domains
          x-axis (:x-axis (first specs))
          y-axis (:y-axis (first specs))

          ;; Combine all data specs
          all-data (vec (mapcat :data specs))

          ;; Apply theme to grid
          grid-spec (themes/apply-theme-to-grid theme-config)

          ;; Build combined spec with themed grid
          combined-spec {:x-axis x-axis
                         :y-axis y-axis
                         :grid grid-spec
                         :data all-data}

          ;; Merge theme defaults with opts
          width (or (:width opts) (get-in theme-config [:plot :width]) 600)
          height (or (:height opts) (get-in theme-config [:plot :height]) 400)
          polar? (or (:polar opts) false)

          ;; Choose plot function
          plot-fn (if polar?
                    viz/svg-plot2d-polar
                    viz/svg-plot2d-cartesian)

          ;; Add origin for polar plots
          combined-spec (if polar?
                          (assoc combined-spec :origin (v/vec2 (/ width 2) (/ height 2))
                                 :circle true)
                          combined-spec)

          ;; Get panel background from theme
          panel-bg (get-in theme-config [:panel :background] "white")
          plot-bg (get-in theme-config [:plot :background] "white")

          ;; Extract axis ranges from spec to calculate panel dimensions
          x-range (get-in combined-spec [:x-axis :range])
          y-range (get-in combined-spec [:y-axis :range])

          ;; Calculate panel background position and dimensions from axis ranges
          ;; x-range is [x-min x-max], y-range is [y-max y-min] (SVG y is inverted)
          panel-x (first x-range)
          panel-y (second y-range)
          panel-width (- (second x-range) (first x-range))
          panel-height (- (first y-range) (second y-range))

          ;; Generate SVG plot
          plot (plot-fn combined-spec)

          ;; Add background rectangles (plot background, then panel background, then plot)
          svg-doc (svg/svg {:width width :height height}
                    ;; Plot background (full area)
                           (svg/rect [0 0] width height {:fill plot-bg})
                    ;; Panel background (plot area defined by axis ranges)
                           (when (not= panel-bg plot-bg)
                             (svg/rect [panel-x panel-y] panel-width panel-height {:fill panel-bg}))
                    ;; The actual plot
                           plot)

          svg-string (svg/serialize svg-doc)]

      ;; Wrap with kind/html for proper Clay rendering
      (kind/html svg-string))))

(defn entries->thing-spec
  "Convert multiple entries to a thi.ng/geom-viz spec (not SVG).
  
  Useful for debugging or further manipulation.
  
  Args:
  - entries: Vector of Entry maps
  
  Returns:
  - thi.ng/geom-viz spec map"
  [entries]
  (if (= 1 (count entries))
    (entry->thing-spec (first entries))

    ;; Multiple entries - combine
    (let [specs (map entry->thing-spec entries)
          x-axis (:x-axis (first specs))
          y-axis (:y-axis (first specs))
          all-data (vec (mapcat :data specs))]

      {:x-axis x-axis
       :y-axis y-axis
       :grid {:attribs {:stroke "#ccc" :stroke-width 0.5}
              :minor-x true
              :minor-y true}
       :data all-data})))

;;; =============================================================================
;;; Main rendering function (matches vegalite.clj API)

(defn thing-geom
  "Render entries using thi.ng/geom-viz backend.
  
  This is the main entry point that matches the vegalite.clj API.
  
  Args:
  - entries: Vector of Entry IR maps or AxisEntries map
  - opts: Optional map with:
    - :width (default 600)
    - :height (default 400)
    - :polar (default false)
    - :format (default :svg) - :svg or :spec
  
  Returns:
  - Kindly-wrapped SVG or spec map"
  [entries & [opts]]
  (let [;; Handle both Entry vector and AxisEntries map
        entry-vec (if (map? entries)
                    (:entries entries)
                    entries)

        format (or (:format opts) :svg)]

    (case format
      :svg (entries->svg entry-vec opts)
      :spec (entries->thing-spec entry-vec))))

;;; =============================================================================
;;; Validation helpers

(defn validate-entry
  "Validate that an entry can be rendered with thi.ng/geom backend.
  
  Returns:
  - {:valid? true} or {:valid? false :error \"...\"}"
  [entry]
  (try
    (let [plottype (:plottype entry)]
      (if-not (contains? plottype->layout plottype)
        {:valid? false
         :error (str "Unsupported plottype for thi.ng/geom: " plottype)}
        {:valid? true}))
    (catch Exception e
      {:valid? false
       :error (.getMessage e)})))
