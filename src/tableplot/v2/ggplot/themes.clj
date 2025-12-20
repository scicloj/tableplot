(ns tableplot.v2.ggplot.themes
  "ggplot2-style themes for Tableplot V2.
  
  Themes control non-data visual elements like backgrounds, grids, fonts, etc.
  Inspired by ggplot2's theme system.")

;;; =============================================================================
;;; Theme Structure
;;;
;;; Themes are maps with the following structure:
;;; {:plot {:background COLOR
;;;         :title {:font {:family STR :size NUM :color COLOR}}}
;;;  :panel {:background COLOR
;;;          :grid {:major {:show BOOL :color COLOR :width NUM}
;;;                 :minor {:show BOOL :color COLOR :width NUM}}
;;;          :border {:show BOOL :color COLOR :width NUM}}
;;;  :axis {:text {:font {:size NUM :color COLOR}}
;;;         :title {:font {:size NUM :color COLOR :weight STR}}
;;;         :line {:show BOOL :color COLOR :width NUM}
;;;         :ticks {:show BOOL :color COLOR}}
;;;  :legend {:position STR  ; "right", "top", "bottom", "left", "none"
;;;           :background COLOR
;;;           :border {:show BOOL :color COLOR :width NUM}
;;;           :text {:font {:size NUM :color COLOR}}}}

;;; =============================================================================
;;; Complete Themes (ggplot2-compatible)

(def theme-grey
  "Default ggplot2 theme: grey background with white grid lines.
  
  This is the default ggplot2 theme with a grey panel background
  and white grid lines."
  {:plot {:background "white"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#000000"}}}
   :panel {:background "#ebebeb"
           :grid {:major {:show true
                          :color "#ffffff"
                          :width 0.8}
                  :minor {:show true
                          :color "#ffffff"
                          :width 0.5}}
           :border {:show false}}
   :axis {:text {:font {:size 10
                        :color "#000000"}}
          :title {:font {:size 11
                         :color "#000000"
                         :weight "normal"}}
          :line {:show false}
          :ticks {:show true
                  :color "#000000"}}
   :legend {:position "right"
            :background "white"
            :border {:show false}
            :text {:font {:size 10
                          :color "#000000"}}}})

(def theme-bw
  "Black and white theme.
  
  White background with thin grey grid lines and black border.
  Clean and publication-ready."
  {:plot {:background "white"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#000000"}}}
   :panel {:background "white"
           :grid {:major {:show true
                          :color "#d9d9d9"
                          :width 0.5}
                  :minor {:show false}}
           :border {:show true
                    :color "#000000"
                    :width 1}}
   :axis {:text {:font {:size 10
                        :color "#000000"}}
          :title {:font {:size 11
                         :color "#000000"
                         :weight "normal"}}
          :line {:show false}
          :ticks {:show true
                  :color "#000000"}}
   :legend {:position "right"
            :background "white"
            :border {:show true
                     :color "#000000"
                     :width 1}
            :text {:font {:size 10
                          :color "#000000"}}}})

(def theme-minimal
  "Minimal theme.
  
  Minimal design with no background and very light grey grid lines.
  Great for clean, modern visualizations."
  {:plot {:background "white"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#000000"}}}
   :panel {:background "white"
           :grid {:major {:show true
                          :color "#f0f0f0"
                          :width 0.5}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:text {:font {:size 10
                        :color "#666666"}}
          :title {:font {:size 11
                         :color "#000000"
                         :weight "normal"}}
          :line {:show false}
          :ticks {:show false}}
   :legend {:position "right"
            :background "transparent"
            :border {:show false}
            :text {:font {:size 10
                          :color "#000000"}}}})

(def theme-classic
  "Classic theme.
  
  Traditional look with axis lines and no grid.
  Like base R graphics."
  {:plot {:background "white"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#000000"}}}
   :panel {:background "white"
           :grid {:major {:show false}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:text {:font {:size 10
                        :color "#000000"}}
          :title {:font {:size 11
                         :color "#000000"
                         :weight "normal"}}
          :line {:show true
                 :color "#000000"
                 :width 1}
          :ticks {:show true
                  :color "#000000"}}
   :legend {:position "right"
            :background "transparent"
            :border {:show false}
            :text {:font {:size 10
                          :color "#000000"}}}})

(def theme-dark
  "Dark theme.
  
  Dark background designed to make colored lines stand out.
  Good for presentations."
  {:plot {:background "#222222"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#ffffff"}}}
   :panel {:background "#333333"
           :grid {:major {:show true
                          :color "#555555"
                          :width 0.5}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:text {:font {:size 10
                        :color "#dddddd"}}
          :title {:font {:size 11
                         :color "#ffffff"
                         :weight "normal"}}
          :line {:show false}
          :ticks {:show true
                  :color "#dddddd"}}
   :legend {:position "right"
            :background "#222222"
            :border {:show false}
            :text {:font {:size 10
                          :color "#ffffff"}}}})

(def theme-light
  "Light theme.
  
  Very subtle grey lines and light grey background.
  Similar to theme-minimal but slightly more defined."
  {:plot {:background "white"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#000000"}}}
   :panel {:background "#fafafa"
           :grid {:major {:show true
                          :color "#e0e0e0"
                          :width 0.5}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:text {:font {:size 10
                        :color "#666666"}}
          :title {:font {:size 11
                         :color "#000000"
                         :weight "normal"}}
          :line {:show false}
          :ticks {:show true
                  :color "#999999"}}
   :legend {:position "right"
            :background "white"
            :border {:show false}
            :text {:font {:size 10
                          :color "#000000"}}}})

(def theme-void
  "Void theme.
  
  Completely empty theme with no annotations.
  Use for maps or when you want complete control."
  {:plot {:background "white"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#000000"}}}
   :panel {:background "white"
           :grid {:major {:show false}
                  :minor {:show false}}
           :border {:show false}}
   :axis {:text {:font {:size 0
                        :color "transparent"}}
          :title {:font {:size 0
                         :color "transparent"}}
          :line {:show false}
          :ticks {:show false}}
   :legend {:position "none"
            :background "transparent"
            :border {:show false}
            :text {:font {:size 10
                          :color "#000000"}}}})

(def theme-linedraw
  "Line draw theme.
  
  Black lines on white background.
  Emphasizes the drawn elements."
  {:plot {:background "white"
          :title {:font {:family "Arial, sans-serif"
                         :size 14
                         :color "#000000"}}}
   :panel {:background "white"
           :grid {:major {:show true
                          :color "#000000"
                          :width 0.3}
                  :minor {:show false}}
           :border {:show true
                    :color "#000000"
                    :width 1}}
   :axis {:text {:font {:size 10
                        :color "#000000"}}
          :title {:font {:size 11
                         :color "#000000"
                         :weight "normal"}}
          :line {:show false}
          :ticks {:show true
                  :color "#000000"}}
   :legend {:position "right"
            :background "white"
            :border {:show true
                     :color "#000000"
                     :width 1}
            :text {:font {:size 10
                          :color "#000000"}}}})

;;; =============================================================================
;;; Theme Customization

(defn theme
  "Customize or create a theme.
  
  Accepts keyword arguments matching theme structure.
  Can be used to:
  1. Override elements of a complete theme
  2. Create a custom theme from scratch
  
  Args:
  - base: Optional base theme to start from (default: empty map)
  - Keyword args for theme elements
  
  Examples:
  ;; Custom plot background
  (theme :plot.background \"#f0f0f0\")
  
  ;; Multiple customizations
  (theme :plot.background \"white\"
         :panel.grid.major.color \"#cccccc\"
         :axis.text.font.size 12)
  
  ;; Override a complete theme
  (merge theme-minimal
         (theme :plot.title.font.size 18
                :legend.position \"bottom\"))"
  [& {:as customizations}]
  (let [theme-map (atom {})]
    (doseq [[path value] customizations]
      (let [keys (clojure.string/split (name path) #"\.")]
        (swap! theme-map assoc-in (mapv keyword keys) value)))
    @theme-map))

(comment
  ;; REPL experiments

  ;; Use a complete theme
  theme-minimal

  ;; Customize a theme
  (merge theme-minimal
         (theme :plot.title.font.size 18
                :legend.position "bottom"))

  ;; Create custom theme from scratch
  (theme :plot.background "white"
         :panel.background "#f5f5f5"
         :panel.grid.major.color "#ddd"))
