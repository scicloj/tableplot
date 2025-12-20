(ns scicloj.tableplot.v1.aog.thing-geom-themes
  "ggplot2-style themes for thi.ng/geom backend.
  
  Provides complete themes matching ggplot2 aesthetics, adapted for SVG rendering.
  Themes control non-data visual elements like backgrounds, grids, fonts, axes, etc.")

;;; =============================================================================
;;; Theme Structure for thi.ng/geom
;;;
;;; Themes are maps with the following structure:
;;; {:plot {:background COLOR           ; Overall plot background
;;;         :width NUM                   ; Default plot width
;;;         :height NUM}                 ; Default plot height
;;;  :panel {:background COLOR           ; Panel (data area) background
;;;          :grid {:major {:show BOOL   ; Show major grid lines
;;;                         :color COLOR
;;;                         :width NUM}
;;;                 :minor {:show BOOL   ; Show minor grid lines
;;;                         :color COLOR
;;;                         :width NUM}}
;;;          :border {:show BOOL         ; Panel border
;;;                   :color COLOR
;;;                   :width NUM}}
;;;  :axis {:line {:color COLOR          ; Axis line color
;;;                :width NUM}
;;;         :ticks {:color COLOR         ; Tick mark color
;;;                 :length NUM}
;;;         :text {:color COLOR          ; Axis tick labels
;;;                :size NUM
;;;                :font STR}
;;;         :title {:color COLOR         ; Axis title
;;;                 :size NUM
;;;                 :font STR
;;;                 :weight STR}}
;;;  :legend {:background COLOR          ; Legend background
;;;           :text {:color COLOR
;;;                  :size NUM
;;;                  :font STR}}
;;;  :data {:default-color COLOR         ; Default mark color
;;;         :stroke-width NUM             ; Default stroke width
;;;         :opacity NUM}}                ; Default opacity

;;; =============================================================================
;;; Complete ggplot2-compatible Themes

(def theme-grey
  "Default ggplot2 theme: grey background with white grid lines.
  
  This is the default ggplot2 theme with a grey panel background
  and white grid lines. Clean and professional."
  {:plot {:background "white"
          :width 600
          :height 400}
   :panel {:background "#ebebeb" ; ggplot2 grey
           :grid {:major {:show true
                          :color "#ffffff" ; White grid lines
                          :width 0.8}
                  :minor {:show true
                          :color "#ffffff"
                          :width 0.5}}
           :border {:show false}}
   :axis {:line {:color "#000000"
                 :width 1}
          :ticks {:color "#000000"
                  :length 5}
          :text {:color "#000000"
                 :size 10
                 :font "Arial, sans-serif"}
          :title {:color "#000000"
                  :size 11
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "white"
            :text {:color "#000000"
                   :size 10
                   :font "Arial, sans-serif"}}
   :data {:default-color "#0af" ; Default blue
          :stroke-width 2
          :opacity 1.0}})

(def theme-bw
  "Black and white theme.
  
  White background with thin grey grid lines and black border.
  Clean and publication-ready."
  {:plot {:background "white"
          :width 600
          :height 400}
   :panel {:background "white"
           :grid {:major {:show true
                          :color "#d9d9d9" ; Light grey grid
                          :width 0.5}
                  :minor {:show false}}
           :border {:show true
                    :color "#000000"
                    :width 1}}
   :axis {:line {:color "#000000"
                 :width 1}
          :ticks {:color "#000000"
                  :length 5}
          :text {:color "#000000"
                 :size 10
                 :font "Arial, sans-serif"}
          :title {:color "#000000"
                  :size 11
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "white"
            :text {:color "#000000"
                   :size 10
                   :font "Arial, sans-serif"}}
   :data {:default-color "#0af"
          :stroke-width 2
          :opacity 1.0}})

(def theme-minimal
  "Minimal theme.
  
  Minimal design with no background and very light grey grid lines.
  Great for clean, modern visualizations."
  {:plot {:background "white"
          :width 600
          :height 400}
   :panel {:background "white"
           :grid {:major {:show true
                          :color "#f0f0f0" ; Very light grey
                          :width 0.5}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:line {:color "#000000"
                 :width 0} ; No axis lines
          :ticks {:color "#666666"
                  :length 0} ; No tick marks
          :text {:color "#666666"
                 :size 10
                 :font "Arial, sans-serif"}
          :title {:color "#000000"
                  :size 11
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "transparent"
            :text {:color "#000000"
                   :size 10
                   :font "Arial, sans-serif"}}
   :data {:default-color "#0af"
          :stroke-width 2
          :opacity 1.0}})

(def theme-classic
  "Classic theme.
  
  Traditional look with axis lines and no grid.
  Like base R graphics."
  {:plot {:background "white"
          :width 600
          :height 400}
   :panel {:background "white"
           :grid {:major {:show false}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:line {:color "#000000"
                 :width 1}
          :ticks {:color "#000000"
                  :length 5}
          :text {:color "#000000"
                 :size 10
                 :font "Arial, sans-serif"}
          :title {:color "#000000"
                  :size 11
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "transparent"
            :text {:color "#000000"
                   :size 10
                   :font "Arial, sans-serif"}}
   :data {:default-color "#0af"
          :stroke-width 2
          :opacity 1.0}})

(def theme-dark
  "Dark theme.
  
  Dark background designed to make colored lines stand out.
  Good for presentations."
  {:plot {:background "#222222"
          :width 600
          :height 400}
   :panel {:background "#333333" ; Dark grey panel
           :grid {:major {:show true
                          :color "#555555" ; Lighter grey grid
                          :width 0.5}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:line {:color "#dddddd"
                 :width 1}
          :ticks {:color "#dddddd"
                  :length 5}
          :text {:color "#dddddd"
                 :size 10
                 :font "Arial, sans-serif"}
          :title {:color "#ffffff"
                  :size 11
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "#222222"
            :text {:color "#ffffff"
                   :size 10
                   :font "Arial, sans-serif"}}
   :data {:default-color "#5fc" ; Cyan for dark bg
          :stroke-width 2
          :opacity 1.0}})

(def theme-light
  "Light theme.
  
  Very subtle grey lines and light grey background.
  Similar to theme-minimal but slightly more defined."
  {:plot {:background "white"
          :width 600
          :height 400}
   :panel {:background "#fafafa" ; Very light grey
           :grid {:major {:show true
                          :color "#e0e0e0"
                          :width 0.5}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:line {:color "#999999"
                 :width 0}
          :ticks {:color "#999999"
                  :length 5}
          :text {:color "#666666"
                 :size 10
                 :font "Arial, sans-serif"}
          :title {:color "#000000"
                  :size 11
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "white"
            :text {:color "#000000"
                   :size 10
                   :font "Arial, sans-serif"}}
   :data {:default-color "#0af"
          :stroke-width 2
          :opacity 1.0}})

(def theme-void
  "Void theme.
  
  Completely empty theme with no annotations.
  Use for maps or when you want complete control."
  {:plot {:background "white"
          :width 600
          :height 400}
   :panel {:background "white"
           :grid {:major {:show false}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:line {:color "transparent"
                 :width 0}
          :ticks {:color "transparent"
                  :length 0}
          :text {:color "transparent"
                 :size 0
                 :font "Arial, sans-serif"}
          :title {:color "transparent"
                  :size 0
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "transparent"
            :text {:color "transparent"
                   :size 0
                   :font "Arial, sans-serif"}}
   :data {:default-color "#0af"
          :stroke-width 2
          :opacity 1.0}})

(def theme-linedraw
  "Line draw theme.
  
  Black lines on white background.
  Emphasizes the drawn elements."
  {:plot {:background "white"
          :width 600
          :height 400}
   :panel {:background "white"
           :grid {:major {:show true
                          :color "#000000"
                          :width 0.3}
                  :minor {:show false}}
           :border {:show true
                    :color "#000000"
                    :width 1}}
   :axis {:line {:color "#000000"
                 :width 1}
          :ticks {:color "#000000"
                  :length 5}
          :text {:color "#000000"
                 :size 10
                 :font "Arial, sans-serif"}
          :title {:color "#000000"
                  :size 11
                  :font "Arial, sans-serif"
                  :weight "normal"}}
   :legend {:background "white"
            :text {:color "#000000"
                   :size 10
                   :font "Arial, sans-serif"}}
   :data {:default-color "#0af"
          :stroke-width 2
          :opacity 1.0}})

;; Tableplot-specific themes (matching v1.aog.themes aesthetic)
(def theme-tableplot
  "Tableplot theme - balanced variant.
  
  Clojure green (#5E9B4D) as primary, grey background like ggplot2.
  Modern web-optimized sizing."
  (-> theme-grey
      (assoc-in [:data :default-color] "#5E9B4D") ; Clojure green
      (assoc-in [:data :stroke-width] 2.5)))

;;; =============================================================================
;;; Theme Registry

(def themes
  "Available themes for thi.ng/geom backend"
  {:grey theme-grey
   :bw theme-bw
   :minimal theme-minimal
   :classic theme-classic
   :dark theme-dark
   :light theme-light
   :void theme-void
   :linedraw theme-linedraw
   :tableplot theme-tableplot})

(defn get-theme
  "Get theme config by keyword.
  
  Args:
  - theme-key: One of :grey, :bw, :minimal, :classic, :dark, :light, :void, 
               :linedraw, :tableplot, or a custom theme map
  
  Returns:
  - Theme config map"
  [theme-key]
  (if (keyword? theme-key)
    (get themes theme-key theme-grey) ; Default to theme-grey
    theme-key)) ; Custom theme map

(defn apply-theme-to-grid
  "Extract grid styling from theme for thi.ng/geom grid spec.
  
  Args:
  - theme: Theme map
  
  Returns:
  - Grid spec {:attribs {...} :minor-x BOOL :minor-y BOOL}"
  [theme]
  (let [panel (get theme :panel {})
        grid (get panel :grid {})
        major (get grid :major {})
        minor (get grid :minor {})]
    (if (:show major)
      {:attribs {:stroke (:color major "#ccc")
                 :stroke-width (:width major 0.5)
                 :stroke-dasharray (when (:show minor) "1 1")}
       :minor-x (:show minor false)
       :minor-y (:show minor false)}
      ;; No grid
      {:attribs {:stroke "transparent"
                 :stroke-width 0}
       :minor-x false
       :minor-y false})))

(defn apply-theme-to-axis-attribs
  "Extract axis styling from theme for thi.ng/geom axis.
  
  Args:
  - theme: Theme map
  
  Returns:
  - Axis attribs map"
  [theme]
  (let [axis (get theme :axis {})]
    {:stroke (get-in axis [:line :color] "#000000")
     :stroke-width (get-in axis [:line :width] 1)}))

(defn get-default-data-color
  "Get default data color from theme.
  
  Args:
  - theme: Theme map
  
  Returns:
  - Color string"
  [theme]
  (get-in theme [:data :default-color] "#0af"))

(defn get-default-stroke-width
  "Get default stroke width from theme.
  
  Args:
  - theme: Theme map
  
  Returns:
  - Number"
  [theme]
  (get-in theme [:data :stroke-width] 2))

(defn get-default-opacity
  "Get default opacity from theme.
  
  Args:
  - theme: Theme map
  
  Returns:
  - Number between 0 and 1"
  [theme]
  (get-in theme [:data :opacity] 1.0))

(defn merge-theme-opts
  "Merge theme defaults into opts map.
  
  Opts from user take precedence over theme defaults.
  
  Args:
  - opts: User-provided opts map
  - theme: Theme keyword or map
  
  Returns:
  - Merged opts with theme defaults"
  [opts theme]
  (let [theme-config (get-theme theme)
        width (or (:width opts) (get-in theme-config [:plot :width]))
        height (or (:height opts) (get-in theme-config [:plot :height]))]
    (assoc opts
           :width width
           :height height
           :theme theme-config)))
