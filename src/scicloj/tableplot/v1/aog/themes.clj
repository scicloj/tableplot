(ns scicloj.tableplot.v1.aog.themes
  "Theme definitions for tableplot visualizations.
  
  Provides ggplot2-inspired themes with Clojure-specific refinements.
  Three variants available: subtle, balanced, and bold.")

;; Color palettes
(def clojure-green "#5E9B4D")
(def clojure-blue "#4A90E2")

(def categorical-subtle
  "Subtle palette - Clojure green as accent, otherwise neutral"
  ["#5E9B4D" ;; Clojure green (primary)
   "#4A90E2" ;; Blue
   "#E87722" ;; Orange
   "#9B59B6" ;; Purple
   "#1ABC9C" ;; Teal
   "#E74C3C" ;; Red
   "#95A5A6" ;; Gray
   "#34495E"]) ;; Dark gray

(def categorical-balanced
  "Balanced palette - Clojure green prominent, vibrant companions"
  ["#5E9B4D" ;; Clojure green (primary)
   "#63B8D1" ;; Cyan blue
   "#F39C12" ;; Bright orange
   "#9B59B6" ;; Purple
   "#E74C3C" ;; Red
   "#1ABC9C" ;; Teal
   "#F1C40F" ;; Yellow
   "#7F8C8D"]) ;; Medium gray

(def categorical-bold
  "Bold palette - Saturated Clojure green, high contrast"
  ["#63C356" ;; Bright Clojure green (more saturated)
   "#3498DB" ;; Bright blue
   "#E67E22" ;; Bold orange
   "#9B59B6" ;; Purple
   "#E74C3C" ;; Red
   "#1ABC9C" ;; Teal
   "#F39C12" ;; Gold
   "#2C3E50"]) ;; Dark slate

;; Base ggplot2-inspired config
(def ggplot2-base
  "Base ggplot2 aesthetic - gray background, white grid"
  {:view {:stroke "transparent"
          :strokeWidth 0
          :fill "#EBEBEB"} ;; Gray plot area background
   :axis {:grid true
          :gridColor "#FFFFFF"
          :gridOpacity 1.0
          :gridWidth 0.5
          :domainColor "#333333"
          :domainWidth 1
          :tickColor "#333333"
          :tickWidth 0.5
          :tickSize 5
          :labelFont "Helvetica Neue, Arial, sans-serif"
          :labelFontSize 11
          :labelColor "#333333"
          :labelPadding 4
          :titleFont "Helvetica Neue, Arial, sans-serif"
          :titleFontSize 12
          :titleFontWeight 600
          :titleColor "#1a1a1a"
          :titlePadding 8}
   :legend {:labelFont "Helvetica Neue, Arial, sans-serif"
            :labelFontSize 11
            :labelColor "#333333"
            :titleFont "Helvetica Neue, Arial, sans-serif"
            :titleFontSize 12
            :titleFontWeight 600
            :titleColor "#1a1a1a"
            :titlePadding 8
            :strokeColor "#CCCCCC"
            :padding 8}
   :header {:labelFont "Helvetica Neue, Arial, sans-serif"
            :labelFontSize 12
            :labelColor "#333333"
            :titleFont "Helvetica Neue, Arial, sans-serif"
            :titleFontSize 13
            :titleFontWeight 600
            :titleColor "#1a1a1a"}
   :mark {:tooltip true} ;; Interactive by default
   :point {:size 80
           :opacity 0.8
           :filled true}
   :line {:strokeWidth 2
          :strokeCap "round"
          :strokeJoin "round"}
   :bar {:opacity 0.9}
   :rect {:opacity 0.9}
   :area {:opacity 0.7
          :line true
          :strokeWidth 2}})

(def tableplot-subtle
  "Tableplot theme - subtle variant.

  Clojure green as primary accent, otherwise close to classic ggplot2.
  Gray background slightly lighter than standard ggplot2 (#EBEBEB vs #E5E5E5)."
  (assoc ggplot2-base
         :range {:category categorical-subtle}))

(def tableplot-balanced
  "Tableplot theme - balanced variant.

  Clojure green prominent, vibrant companion colors.
  Modern web-optimized sizing, cleaner grid."
  (-> ggplot2-base
      (assoc :range {:category categorical-balanced})
      (assoc-in [:point :size] 90) ;; Slightly larger points
      (assoc-in [:line :strokeWidth] 2.5) ;; Slightly thicker lines
      (assoc-in [:axis :gridWidth] 0.6)))

(def tableplot-bold
  "Tableplot theme - bold variant.

  Saturated Clojure green, high contrast, optimized for presentations.
  Larger text, thicker lines, more prominent marks."
  (-> ggplot2-base
      (assoc :range {:category categorical-bold})
      (assoc-in [:point :size] 100) ;; Larger points
      (assoc-in [:point :opacity] 0.85)
      (assoc-in [:line :strokeWidth] 3) ;; Thicker lines
      (assoc-in [:bar :opacity] 0.95)
      (assoc-in [:axis :gridWidth] 0.7)
      (assoc-in [:axis :labelFontSize] 12)
      (assoc-in [:axis :titleFontSize] 13)
      (assoc-in [:legend :labelFontSize] 12)
      (assoc-in [:legend :titleFontSize] 13)))

;; Theme registry
(def themes
  "Available themes"
  {:tableplot-subtle tableplot-subtle
   :tableplot-balanced tableplot-balanced
   :tableplot-bold tableplot-bold
   :ggplot2 ggplot2-base
   :vega nil}) ;; nil = use Vega-Lite defaults

(defn get-theme
  "Get theme config by keyword.
  
  Args:
  - theme-key: One of :tableplot-subtle, :tableplot-balanced, :tableplot-bold, 
               :ggplot2, :vega, or a custom theme map
  
  Returns:
  - Theme config map, or nil for Vega defaults"
  [theme-key]
  (if (keyword? theme-key)
    (get themes theme-key tableplot-balanced) ;; Default to balanced
    theme-key)) ;; Custom theme map

(defn apply-theme
  "Apply theme config to a Vega-Lite spec.
  
  Args:
  - spec: Vega-Lite specification map
  - theme: Theme keyword or custom config map
  
  Returns:
  - Spec with theme applied"
  [spec theme]
  (let [theme-config (get-theme theme)]
    (if theme-config
      (assoc spec :config theme-config)
      spec))) ;; No config = use Vega defaults
