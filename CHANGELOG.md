# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [1-beta11] - 2025-02-19
- added `:=size-range` support
  - (with a slight breaking change with respect to default mark sizes)
- added `:=colorscale` support for scatterplots

## [1-beta10.2] - 2025-02-11
- updated metamorph.ml version
- moved book-utils into the library code base

## [1-beta10.1] - 2025-02-05
- fixed broken require

## [1-beta10] - 2025-02-04
- added dependency: std.lang
- added the [`transpile`](https://scicloj.github.io/tableplot/tableplot_book.transpile_reference) API - experimental

## [1-beta9.1] - 2025-01-22
- redeployed the previous version that seems to be a broken JAR

## [1-beta9] - 2025-01-16
- updated deps (metamorph.ml, tempfiles)
- adapted to changes in metamorph.ml design-matrix api

## [1-beta8] - 2025-01-12
- added `layer-histogram2d` (experimental)

## [1-beta7] - 2025-01-09
- added support for `:=symbol` and `:=mark-symbol` (controlling mark symbols)

## [1-beta6] - 2024-12-31
- added `layer-heatmap`
- added `layer-correlation`
- added `layer-surface`
- [annotations](https://plotly.com/javascript/text-and-annotations/) support
- updated docstrings
- defined `:=z` to be `:z` by default, for convenience and compatibility with `:=x`, `:=y`
- bugfix: avoiding the assumption that column names are keywords (#10) - thanks, @harold

## [1-beta5] - 2024-12-16
- added initial [Violin plot](https://plotly.com/javascript/violin/) support
- improved [boxplot](https://plotly.com/javascript/box-plots/) support
- added [SPLOM](https://plotly.com/javascript/splom/) support
- added many details to the docstrings to reflect the main useful substitution keys for the various functions

## [1-beta4] - 2024-12-14
- improved styling of `imshow`
- added initial 3d `surface` support

## [1-beta3] - 2024-12-14
- refactored and made some functions private
- added `imshow` function to show images
- added some layout substitution keys
- support String columns (PR #11, fixes #10) - thanks, @harold

## [1-beta2] - 2024-12-06
- updated docstrings
- refactored the plotly implementation and added detailed for reference generation
- renamed some fn arguments for clarity
- encoded the default coordinates as 2d rather than nil
- made sure the `plotly/plot` function would accept only one arity
- extended the `plotly/debug` function to handle layers
- simpilified the internal workflow of applying statistical transformations
- styling: set the wrapping div height to be "auto"

## [1-beta1.1] - 2024-12-01 
- improved handling of edges in density layers

## [1-beta1] - 2024-12-01
- replaced the ad-hoc `WrappedValue` daratype with the Clojure `delay` idiom 
(used to avoid the recursive transformations of certain functions)
- plotly: corrected bar width for histograms
- plotly: added support for overlapping histograms
- plotly: added support for density layers
- plotly: added mark-fill support (in use by default for density layers)

## [1-alpha14.1] - 2024-11-15
- updated deps (Hanami, metamorph.ml, Fastmath, Kindly)

## [1-alpha14] - 2024-11-15
- using our own port of Hanami template `xform` function
- avoiding recursion into datasets in `xform`
- avoiding the wrapping of datasets which is unnecessary with the new `xform`
- removing the api dataset wrapper function which is not necessary anymore

## [1-alpha13] - 2024-11-08
- plotly: added support for 3d coordinates (3d scatterplots)

## [1-alpha12] - 2024-11-03
- plotly: added support for configuring the margin with sensible defaults (breaking the previous default behavior)

## [1-alpha11] - 2024-11-02
- initial support for geo coordinates

## [1-alpha10] - 2024-10-20
- renaming to Tableplot
- changing the main API namespaces

## [1-alpha9] - 2024-10-04
- deps update
- plotly - added support to override specific layer data (experimental)
- plotly - extended layer-smooth to use metamorph.ml models and design matrices

## [1-alpha8] - 2024-09-21
- deps update

## [1-alpha7-SNAPSHOT] - 2024-09-13
- plotly - added text & font support
- plotly - bugfix: broken x axis in histograms

## [1-alpha6-SNAPSHOT] - 2024-08-09
- plotly - coordinates support - WIP
- plotlylcoth - styling changes
- plotly - simplified the inference of mode and type a bit
- debugging support - WIP

## [1-alpha5-SNAPSHOT] - 2024-08-06
- added the `plotly` API (experimental) generating [Plotly.js plots](https://plotly.com/javascript/)

## [1-alpha4-SNAPSHOT] - 2024-07-12
- breaking change: substitution keys are `:=abcd`-style rather than `:haclo/abcd`-style
- simplified CSV writing
- more careful handling of datasets - avoiding layer data when they can reuse the toplevel data
- related refactoring
- facets support - WIP

## [1-alpha3-SNAPSHOT] - 2024-06-28
- catching common problems
- bugfix (#1) of type inference after stat

## [1-alpha2-SNAPSHOT] - 2024-06-22
- renamed the `hanami` keyword namespace to `haclo` to avoid confusion

## [1-alpha1-SNAPSHOT] - 2024-06-22
- initial version
