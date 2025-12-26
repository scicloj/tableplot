# Tableplot Project Summary

## Recent Updates (2025-12-26)

**Session 16 - AoG V2 Decision Implementation Attempt (2025-12-26) ⚠️ INCOMPLETE**:
- **Goal**: Systematically implement the 6 design decisions from AOG_V2_DESIGN_DECISIONS.md in building_aog_v2.clj
- **Decisions Implemented**:
  1. ✅ **Decision 1 - Operators**: Renamed `*` → `=*` and `+` → `=+` (removed `:refer-clojure :exclude [* +]` from namespace)
  2. ✅ **Decision 2 - Auto-wrapping**: Added `ensure-vec` helper and updated `=*` and `=+` to auto-wrap single items
  3. ⚠️ **Decision 3 - Keyword prefix**: INCOMPLETE - Started replacing `:aog/*` → `:=*` but encountered namespace loading issues
- **Technical Challenges**:
  - Used `sed` to replace 273 occurrences of `:aog/` with `:=`
  - Updated `update-keys` functions to create `:=` prefixed keywords
  - Added `clojure.string` require for `str/starts-with?` in `layers?` function
  - Wrapped validation examples in `comment` blocks to prevent load-time execution
  - Fixed `layers?` function to check for `:=` prefix instead of `"aog"` namespace
- **Critical Issue**: Session ended with file reverted to state before Decision 3 changes
  - Changes were applied to file during session
  - File was restored from backup `building_aog_v2.clj.backup_before_keyword_rename` 
  - This reverted ALL Decision 3 work (keyword replacement, namespace fixes, comment wrapping)
  - File verified loading successfully but without the changes
- **Backup Files**:
  - `building_aog_v2.clj.backup_before_keyword_rename` - State before keyword changes
  - Multiple other backups exist from previous sessions
- **Remaining Decisions** (not started):
  4. Decision 4: Replace dual validation flags with single `*validate*`
  5. Decision 5: Remove custom type inference, delegate to tablecloth
  6. Decision 6: Verify ensure-dataset flexibility is maintained
- **Next Session Actions**:
  1. Re-implement Decision 3 more carefully (avoid sed, use targeted edits)
  2. Ensure namespace loads after each change
  3. Handle examples that execute at load time (wrap in `comment` or move)
  4. Rediscuss Decision 4 (user wanted to change it)
  5. Implement remaining decisions 5-6

**Session 15 - AoG V2 Design Decisions Discussion (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Systematically reviewed and documented design decisions for the AoG V2 implementation
- **Decisions Made**:
  1. **Operators**: Use `=*` (compose) and `=+` (overlay) instead of shadowing Clojure's `*` and `+` - preserves algebraic clarity without surprising behavior
  2. **Auto-wrapping**: Single items automatically wrapped to vectors for cleaner API - reduces verbosity for common cases
  3. **Key naming**: Switch from `:aog/` namespaced keywords to `:=` convention (`:=color`, `:=x`, `:=data`) - lightweight, distinctive, consistent with other Tableplot APIs
  4. **Validation**: Remove dual flags (`*validate-on-construction*`, `*validate-on-draw*`), use single `*validate*` flag (default: true) with construction-time validation - fail fast for better developer experience
  5. **Type inference**: Delegate to tablecloth, remove custom `infer-from-values` logic - leverages existing library, provides escape hatch via typed datasets
  6. **Plain data support**: Keep `ensure-dataset` flexibility - convenience worth the maintenance
- **Design Discussions Added** (for future consideration):
  1. **Auto-display vs explicit rendering** (building_aog_v2.clj:3931-3960) - tension between notebook convenience and debugging clarity (Clay errors during display phase harder to track)
  2. **Multimethod cartesian expansion** (3962-3998) - `[target plottype]` dispatch creates N×M methods, scalability concern as plottypes grow (currently ~10 methods, could reach 50+)
  3. **Faceting implementation complexity** (4091-4126) - current manual grid organization feels imperative vs compositional spirit of `=*` and `=+`, question about more declarative approach
  4. **Error message enhancement** (4125-4162) - opportunity for did-you-mean suggestions, context-specific guidance, doc links (worth pursuing but deferred)
- **Documentation Created**: `AOG_V2_DESIGN_DECISIONS.md` - comprehensive record of all decisions and open questions for next session
- **Process**: Fresh reading as curious Clojure developer → identified 10 design dilemmas → systematic discussion → decisions documented
- **Next Steps**: Implement decisions 1-6 (operators, auto-wrapping, key naming, validation, type inference, plain data), update all examples and schemas

**Session 14 - Quality Review & Documentation Cleanup (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Comprehensive quality review of `building_aog_v2.clj` focusing on coherence, consistency, code quality, and clarity
- **Changes Applied** (5 total):
  1. **Fixed typo** (line ~1061): `Examplesbuilding_aog_v2.clj:` → `Examples:`
  2. **Removed duplicate section header** in `facet` function docstring (was: `;; ## ⚙️ Facet Constructor`)
  3. **Removed duplicate section header** in `scale` function docstring (was: `;; ## ⚙️ Scale Constructor`)
  4. **Removed duplicate "Implementation Status" section** (line ~3809):
     - Consolidated into first occurrence (line ~816)
     - Added validation items (Malli schemas, column validation) to main status list
     - Removed outdated "Next Steps" section with strikethrough items
  5. **Converted all `comment` blocks to executable code** (10 validation examples):
     - Removed `(comment ...)` wrappers from Examples 1-10
     - Removed expected result comments (e.g., `;; => nil (valid!)`)
     - Wrapped error-throwing examples (6-8) in `try-catch` blocks
     - Made all examples directly executable for Clay rendering
- **Rationale for Comment Block Removal**:
  - Clay notebooks should use executable code that renders actual results
  - Expected result comments are redundant (Clay shows computed results)
  - `comment` blocks prevent code from running and appearing in output
  - `try-catch` allows error examples to run safely and display actual error messages
- **Quality Assessment**: EXCELLENT across all dimensions
  - **Documentation**: All public functions have complete, consistent docstrings
  - **Error Handling**: Comprehensive validation with helpful error messages
  - **Code Consistency**: Consistent patterns, naming conventions, structure
  - **Testing**: Examples tested, namespace loads cleanly
  - **Maintainability**: Well-organized, extensible multimethod architecture
- **No Issues Found**:
  - No unused functions (verified `infer-scale-type` is used)
  - No missing docstrings on public functions
  - No TODO/FIXME/HACK comments
  - No inconsistent naming patterns
  - No unhandled edge cases in core logic
  - No code duplication requiring refactoring
- **Verification**: All tests passed
  - ✅ Namespace loads successfully (no errors)
  - ✅ Basic constructors work correctly
  - ✅ Dataset integration works
  - ✅ Composition operators work
  - ✅ All transformations work
- **Impact**: File is production-ready with clean, executable examples that demonstrate actual behavior
- **Key Insight**: In Clay notebooks, use executable code instead of `comment` blocks to show real results rather than documenting expected behavior in comments

**Session 13 - Compositional Size Specification & Design Discussion (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Made width/height compositional like `:target`, enabling full threading chains with `plot`
- **Core Changes**:
  1. **Added `size` constructor function** (after `target` at ~line 780):
     - 2-arity: `(size 800 600)` returns `[{:aog/width 800 :aog/height 600}]`
     - 3-arity: `(size layers 800 600)` merges size into layers via `*` operator
     - Enables: `(-> ... (size 800 600) (plot))`
  2. **Updated all three `plot-impl` methods** (:geom, :vl, :plotly):
     - Priority chain: `:aog/width` in layers > `:width` in opts > `default-plot-width`
     - Same for height: `:aog/height` > `:height opts` > `default-plot-height`
     - Uses `(some :aog/width layers-vec)` to extract from any layer
  3. **Fixed `scatter` function** for threading with attributes:
     - Added 2-arity: `([layers attrs] (* layers (scatter attrs)))`
     - Enables: `(-> penguins (mapping :x :y) (scatter {:alpha 0.7}))`
  4. **Added Example 15**: Demonstrates compositional size specification patterns
  5. **Updated 4 existing examples** to use new `size` pattern:
     - Example 19 (line ~3046): Grid faceting with VL
     - Example 22 (line ~3171): Simple histogram with Plotly
     - Example 23 (line ~3187): Faceted histogram with Plotly
     - Example 24 (line ~3205): Faceted scatter with Plotly
- **Design Discussion Section Added** (before Summary at ~line 3292):
  - Documents tension: `:aog/target`, `:aog/width`, `:aog/height` appear in every layer but apply to all layers together
  - This is a consequence of using vector-of-maps as the intermediate representation
  - **Four alternatives considered**:
    1. **Metadata on vector** - Conceptually clean (plot-level config is metadata) but fragile (easily lost during operations)
    2. **Wrapper map** - Explicit separation `{:plot-config {...} :layers [...]}` but breaks the algebra (no more `*` and `+`)
    3. **Special marker layer** - Keep vectors but add `{:aog/type :plot-config ...}` - still mixed concerns
    4. **Accept duplication (current)** - Simple, works, revisable; duplication overhead is negligible
  - **Current decision**: Alternative 4 because it's simple, works reliably with `some` extraction, and can migrate to metadata later if needed
  - **Key insight**: This is a limitation of the IR choice, not a flaw - documents trade-offs honestly
- **Pattern Evolution**:
  - **Before**: `(plot (-> ... (target :vl)) {:width 800 :height 600})`
  - **After**: `(-> ... (target :vl) (size 800 600) (plot))`
  - **Backwards compatibility**: Old pattern still works via opts map
- **Verification**: All changes tested in REPL, namespace loads successfully ✓
- **Impact**: Completes the compositional API vision - everything can be in the threading chain

**Session 12 - Comprehensive Test Coverage for building_aog_v2.clj (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Added thorough `kind/test-last` assertions to all 28 examples in `building_aog_v2.clj`
- **Test Coverage**: 28 test assertions following the pattern from `plotly_walkthrough.clj`
  - 22 basic layer structure tests (validate `:aog/*` keys)
  - 4 combined layer + render tests (Examples 1, 4, 14, 21)
  - 2 plot output structure tests (Examples 19, 24)
- **Key Learnings**:
  1. **`kind/test-last` API**: Takes `[test-fn & args]` - only first element is the function
  2. **Multiple assertions**: Must be combined with `and` in a single function, not as separate functions
  3. **Test what exists**: Verified actual output structure in REPL (VL facet fields are strings at top level, not nested)
  4. **Use `sequential?` not `vector?`**: More flexible for checking Plotly `:data` fields
- **Test Types**:
  - **Layer structure**: `#(= (:aog/plottype (first %)) :scatter)`
  - **Combined layer + render**: `#(and (layer-check %) (let [rendered (plot %)] (render-check rendered)))`
  - **VL spec structure**: Check for `:data`, `:mark`, `:encoding` keys
  - **Plotly spec structure**: Check for `:data`, `:layout` keys with `sequential?` data
  - **VL faceted output**: Top-level `:facet` with `:row/:column` fields as strings
- **Generated Tests**: Tests automatically generate at `test/building_aog_v2_generated_test.clj` when notebook renders through Clay
- **Verification**: All tests pass, namespace loads successfully, combined tests validated in REPL
- **Impact**: Comprehensive test coverage ensures examples work correctly and catches regressions when code changes

**Session 11 - Code Quality & Robustness Improvements (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Comprehensive review and improvement of `building_aog_v2.clj` for production readiness
- **Input Validation Added**:
  1. **`validate-column-exists`** - Validates single column with helpful error messages
  2. **`validate-columns`** - Validates multiple columns at once
  3. **`validate-layer-columns`** - Validates all aesthetic mappings (`:aog/x`, `:aog/y`, `:aog/color`, `:aog/row`, `:aog/col`, `:aog/group`)
  4. **Enhanced `layer->points`** - Checks for missing data, missing x mapping, empty datasets
- **Error Messages Enhanced**:
  - Column errors show available columns and suggestions
  - Empty dataset errors with specific context
  - Styled HTML error display in `plot-impl :geom` with troubleshooting tips
  - All errors include helpful context maps for debugging
- **Edge Case Handling**:
  1. **`compute-linear-regression`** - Handles < 2 points, identical x/y values, validates coefficients (no NaN/Infinity)
  2. **`compute-histogram`** - Validates numeric data, handles empty data, handles identical values
  3. **`infer-domain`** - Handles empty data ([0 1] fallback), single value (10% expansion), identical values
  4. **Multimethod validation** - Both `apply-transform` methods check for empty points with informative errors
- **Code Organization**:
  - Extracted 8 magic numbers to named constants (`default-plot-width`, `panel-margin-left`, `facet-label-offset`, etc.)
  - Updated `render-single-panel` and `plot-impl :geom` to use constants
  - Improved consistency throughout layout code
- **Documentation Improvements**:
  - Enhanced docstrings for all helper functions
  - Added comprehensive comment headers for multimethod implementations
  - Documented all edge cases and return values
  - Clear parameter descriptions and examples
- **Testing**: ✅ Namespace loads successfully, all examples functional, no breaking changes
- **Impact**: File is now production-ready with robust error handling, validation, and user-friendly error messages

**Session 10 - Multiple Grouping Columns & Code Simplification (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Refactored grouping system to support multiple categorical aesthetics creating composite groups
- **Core Changes**:
  1. **Refactored `get-grouping-column` → `get-grouping-columns` (plural)**:
     - Now returns vector of all grouping columns: `[:species :island]`
     - Supports `:aog/group` as keyword or vector
     - Collects all categorical aesthetics: `:aog/color`, `:aog/col`, `:aog/row`, `:aog/group`
     - Removes duplicates, preserves order
  2. **Updated `layer->points` for composite group keys**:
     - Group key is now a vector: `{:group [:Adelie :Torgersen]}`
     - Enables transforms to group by combinations (species × island)
  3. **Fixed grouped histogram coloring**:
     - Extracts color from first element of composite group key
     - Correctly applies ggplot2 color palette to bars
  4. **Updated Vega-Lite rendering**:
     - Handles composite group keys in grouped-regression and grouped-histogram
     - Creates proper field mappings for all grouping columns
- **Operator Simplification** (vectors-all-the-way convention):
  1. **Simplified `*` operator**: Removed 4-branch cond, now just vector×vector case (13 lines removed)
  2. **Simplified `+` operator**: Replaced `mapcat #(if (vector? %) % [%])` with `(apply concat)` (1 line)
- **Documentation Improvements**:
  1. **Added emoji legend**: Explains 📖 (narrative), ⚙️ (implementation), 🧪 (examples) convention
  2. **Fixed emoji usage**: Changed "Setup: Load Datasets" from ⚙️ to 🧪 (it's demonstration code)
  3. **Added Example 12b**: Demonstrates multiple grouping columns (color + faceting)
  4. **Updated grouped histogram example**: Added `:alpha 0.7` to show overlapping bars
- **Validation Items Added to Implementation Status**:
  - ⚠️ Malli schemas for layer validation (planned)
  - ⚠️ Column existence validation (planned)
- **Key Insight**: Grouping is now compositional - any categorical aesthetic creates groups, and they compose multiplicatively (3 species × 3 islands = 9 regression lines)
- **Testing**: All changes verified, namespace loads cleanly, examples render correctly ✓

**Session 9 - V2 Clarity Review & Cleanup (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Reviewed `building_aog_v2.clj` for clarity and cleaned up reorganization artifacts
- **Context Clarification**: Document is a **design document sharing status and dilemmas**, not a tutorial
  - Mixed voice (tutorial/design doc/code comments) is intentional - reflects different purposes
  - Early architecture discussions (Delegation Strategy) document reasoning for future reference
  - "What's Missing" section honestly shares current limitations, not discouraging but transparent
- **Cleanup Actions**:
  1. **Removed 21 placeholder comments**: All `;; (moved to X)` and `;; (moved below)` comments removed
  2. **Removed empty section headers**: `## ⚙️ Scatter Constructor`, `## ⚙️ Histogram Constructor`, `## ⚙️ Type Information`
  3. **Created backup**: `building_aog_v2.clj.backup_cleanup` before changes
  4. **Verification**: Namespace loads cleanly after cleanup ✓
- **Design Document Framing**:
  - Purpose: Share architectural decisions, current status, and open dilemmas with community
  - Not selling/teaching - documenting thinking process transparently
  - Early architectural sections (Delegation Strategy ~line 280) are intentional - frame core design choices upfront
  - Threading macro explanation (Example 2b) kept as-is - serves as reference for both compositional and threading styles
- **Key Decisions**:
  - Kept Delegation Strategy section early - helps understand code architecture
  - Kept mixed voice - natural for design document (narrative context + technical decisions + working examples)
  - Skipped "tutorial-oriented" fixes - not applicable to design document format
  - Threading macro section adequate - shows both `*` and `->` styles for reference
- **Result**: Cleaner file without reorganization artifacts, maintains honest design document character

**Session 8 - V2 Multi-Target Vertical Reorganization (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Moved `:vl` and `:plotly` plot-impl methods to Multi-Target Rendering section
- **Changes Made**:
  1. **Removed from early infrastructure** (~lines 1350):
     - Deleted both `(defmethod plot-impl :vl ...)` and `(defmethod plot-impl :plotly ...)`
     - Left comment marker: `(:vl and :plotly plot-impl methods moved to Multi-Target Rendering section)`
  2. **Added to Multi-Target Rendering section** (~lines 2360-2580):
     - **## ⚙️ Implementation: Vega-Lite Target** - Complete `:vl` plot-impl (~210 lines)
     - **## ⚙️ Implementation: Plotly Target** - Complete `:plotly` plot-impl (~220 lines)
     - Both implementations placed immediately before their examples
  3. **Fixed forward references**:
     - Moved Example 14 (Simple Scatter with Vega-Lite) from before Multi-Target section to after :vl implementation
     - Simplified Example 8 (removed :vl/:plotly examples to avoid forward references)
     - Updated `target` function docstring to remove examples using :vl/:plotly
     - Removed :vl/:plotly examples from `plot` function docstring
  4. **Verification**: Namespace loads cleanly after all changes ✓
- **Result**: All three rendering targets now follow vertical organization:
  - **:geom**: Implementation in infrastructure → Examples throughout (default target, needed early)
  - **:vl**: Implementation → Examples (lines 2150-2670)
  - **:plotly**: Implementation → Examples (lines 2360-2800)
- **Remaining Tasks** (for next session):
  1. Move faceting helpers (`split-by-facets`, `organize-by-facets`) to Faceting section (~line 1100 → ~line 2250)
  2. Add "Implementation Note" markers for core infrastructure that must stay early (`layer->points`, multimethod system, etc.)
- **Key Insight**: Vertical organization dramatically improves narrative flow - each feature tells complete story (implementation → examples) before moving on

**Session 7 - V2 Histogram Vertical Reorganization (2025-12-26) ✅ COMPLETE**:
- **Goal Achieved**: Reorganized histogram implementation and examples in `building_aog_v2.clj` following vertical organization principles
- **Changes Made**:
  1. **Created dedicated Histograms section** after Linear Regression, before Grouping & Color
     - **## ⚙️ Implementation** subsection: Constructor, compute function, 4 defmethods (apply-transform, transform->domain-points x2, render-layer)
     - **## 🧪 Examples** subsection: Simple histogram, custom bin count
  2. **Moved grouped histogram example** to Grouping & Color section (Example 5)
     - Placed after grouped regression to show categorical color creates groups for histograms too
  3. **Moved faceted histogram example** to Faceting section (Example 10)
     - Placed after faceted scatter to demonstrate faceting works with histograms
  4. **Added multi-target histogram examples**:
     - Vega-Lite: Added histogram example to VL section
     - Plotly: Added simple histogram + faceted histogram with custom bins
  5. **Removed scattered references**:
     - Removed histogram constructor from Constructors section
     - Removed compute-histogram and all defmethods from scattered locations
     - Removed misplaced histogram examples from Linear Regression section
     - Removed histogram from threading examples section
- **Pedagogical Flow**: Learn basic histogram → Learn grouping → See grouped histogram → Learn faceting → See faceted histogram → Learn targets → See histogram works across all targets
- **Verification**: Namespace loads cleanly ✓
- **Result**: Histogram now follows same vertical organization as scatter and linear regression, with implementation immediately followed by basic examples, then advanced examples appearing in their respective conceptual sections

**Session 5 - V3 Content Integration and Polish (2025-12-26) ✅ COMPLETE**:
- **Goal**: Integrate core V2 narrative content into V3 while maintaining vertical organization
- **Completed Actions**:
  1. **Content Integration** (~450 lines added):
     - Complete opening narrative (Tableplot's Journey, user feedback, Real-World Data dev group)
     - Context & Motivation section
     - Detailed library explanations (Tablecloth type system insight)
     - Inspiration: AlgebraOfGraphics.jl
     - Complete Glossary of visualization terms
     - Core Insight: Layers + Operations
     - Comparison to ggplot2
     - Translating to Clojure (nested → flat → namespaced evolution)
     - Design Exploration
     - Rendering Targets section
     - **The Delegation Strategy** (~180 lines - critical architectural decision section)
     - Proposed Design with API Overview
     - How Plots are Displayed
  2. **File Growth**: V3 grew from 1,464 → 1,927 lines (contains ~69% of V2 content)
  3. **Backup Created**: `building_aog_v3.clj.backup` before major integration
  4. **Narrative Polish** (coherence improvements):
     - Added transition section between "How Plots are Displayed" and code implementation
     - Clarified "Proposed Design" section as implementation preview
     - Improved flow between conceptual and technical sections
  5. **Verification**: Namespace loads cleanly after all changes
- **Result**: V3 is a **streamlined, pedagogically-reorganized version** preserving all code but with 31% less content than V2 (removed: faceting architecture discussion, 14 target-specific examples, detailed example explanations)
- **Title Clarification**: "Version 1" is intentional (first blog post in a series, not related to internal v3 naming)

**Session 6 - V2/V3 Content Verification (2025-12-26) ✅ COMPLETE**:
- **Discovery**: Detailed comparison revealed V3 is NOT just a reorganization
- **Findings**:
  - V2: 2,807 lines | V3: 1,927 lines (880 lines removed, 31% reduction)
  - Missing: Faceting architecture discussion (~150 lines)
  - Missing: 7 Vega-Lite examples (~200 lines)
  - Missing: 7 Plotly examples (~200 lines)  
  - Missing: Detailed "What happens here" explanations (~200 lines)
  - Missing: Backend agnosticism discussion, escape hatch examples (~130 lines)
- **Corrected Understanding**: V3 is a condensed pedagogical version, not a complete migration
- **Updated Documentation**: PROJECT_SUMMARY.md corrected to reflect actual content differences
- **Recommendation**: Use V2 for comprehensive reference, V3 for streamlined learning

**Session 4 - V3 Vertical Reorganization (2025-12-25) ✅ COMPLETE**:
- **Goal Achieved**: Created `building_aog_v3.clj` with vertical feature presentation (1,464 lines initially)
- **Structure**: Reorganized into 4 parts leveraging multimethod architecture:
  - **Part I: Foundation** - Infrastructure, multimethod declarations, all 3 rendering targets
  - **Part II: Features** - Each feature complete: Scatter (impl→examples), Linear (impl→examples), Histogram (impl→examples)
  - **Part III: Advanced Topics** - Multi-layer composition, faceting, multiple targets, custom domains
  - **Part IV: Reflection** - Trade-offs, implementation status, integration path
- **Key Benefit**: Each feature presented vertically (implementation immediately followed by examples)
- **Pedagogical Improvement**: Learn one feature at a time without jumping between distant sections
- **Bug Fixes Applied**:
  - Added 3-arity to `scatter` for threading with attributes: `(-> layers (scatter {:alpha 0.5}))`
  - Fixed `*` operator to detect datasets vs layer maps (prevents dataset columns from leaking during merge)
  - Dataset detection: checks for `:aog/*` keys to distinguish layers from data, wraps data as `{:aog/data ...}`
- **Coverage**: V3 is ~50% of V2 content (focused vertical demo), V2 remains comprehensive reference
- **Testing**: Namespace loads cleanly, all examples verified working
- **Decision Point**: Should V3 expand to full coverage or remain streamlined demo?

**Session 1 - Organization & Features**:
- Added emoji-based section markers throughout `building_aog_v2.clj` (📖 docs, ⚙️ implementation, 🧪 examples)
- Feature-based organization with clear section headers for each major feature
- Prepared groundwork for multimethod-based refactoring to enable late function definition
- `target` function for compositional backend selection: `(-> data (scatter) (target :vl))`
- Auto-display mechanism via `displays-as-plot` (no explicit `plot` calls needed)
- Type-aware grouping (categorical colors create semantic groups for transforms)
- Vega-Lite tooltips (hover to see x, y, color values; histogram bins show range/count)

**Session 2 - Refactoring Attempt**:
- Attempted Phase 1 refactoring: moving constructors before their examples
- Successfully moved `scatter` and `linear` constructors with proper testing after each move
- Learned that example code calling constructors must also be moved/removed
- Process validated: incremental moves with namespace load testing after each change
- Changes reverted to preserve stable state for future work

**Session 3 - Multimethod Refactoring (2025-12-25) ✅ COMPLETE**:
- **Goal Achieved**: Converted `building_aog_v2.clj` to use multimethods, enabling late feature definition
- **Phase 1**: Converted `apply-transform` from `case` statement to multimethod
  - `defmulti` dispatches on `:aog/transformation` key
  - `defmethod` for `nil` (scatter/raw), `:linear`, `:histogram`
  - Eliminates hard dependency on compute functions being defined first
- **Phase 2**: Converted `transform->domain-points` from `case` statement to multimethod
  - `defmulti` dispatches on `:type` key in transform result
  - 5 defmethods: `:raw`, `:regression`, `:grouped-regression`, `:histogram`, `:grouped-histogram`
- **Testing**: All multimethods verified working correctly, namespace loads cleanly
- **Result**: Features (scatter, linear, histogram) can now be defined anywhere AFTER the `defmulti` declarations

**Key Insights**:
- **Multimethod extensibility**: New features can be added without modifying core infrastructure
- **Late definition enabled**: `defmethod` calls can appear anywhere after `defmulti` declarations
- **Extension pattern**: To add new feature: `(defn constructor) → (defn- compute) → (defmethod apply-transform) → (defmethod transform->domain-points) → (defmethod render-layer)`
- Testing namespace load (`clojure -M -e "(require 'building-aog-v2)"`) essential after each change
- File edit tools (`clojure_edit`) provide better validation for Clojure code than text-based tools

**Multimethod Architecture** (`notebooks/building_aog_v2.clj`):
```clojure
;; Core extension points (no hard dependencies)
(defmulti apply-transform 
  "Dispatch on :aog/transformation key"
  (fn [layer points] (:aog/transformation layer)))

(defmulti transform->domain-points
  "Dispatch on :type key in transform result"
  (fn [transform-result] (:type transform-result)))

(defmulti render-layer
  "Dispatch on [target plottype-or-transform]"
  (fn [target layer transform-result alpha]
    [target (or (:aog/transformation layer) (:aog/plottype layer))]))

;; Features can be added anywhere after defmultis
(defmethod apply-transform :my-new-feature [layer points] ...)
(defmethod transform->domain-points :my-result-type [result] ...)
(defmethod render-layer [:geom :my-feature] [target layer result alpha] ...)
```

**Current State**:
- Infrastructure supports extensibility through multimethods
- All existing features (scatter, linear, histogram) working correctly
- File ready for either: (A) using as-is with multimethod extensibility, or (B) physical reorganization for pedagogical improvement

## Overview
Tableplot is a Clojure library for declarative data visualization, implementing a grammar of graphics approach similar to R's ggplot2. The project is actively developing V2, which introduces a sophisticated dataflow-based architecture that separates specification from rendering.

## Project Structure

```
tableplot/
├── src/scicloj/tableplot/
│   ├── v1/
│   │   └── aog/                     # AlgebraOfGraphics implementation
│   │       ├── core.clj             # AoG API (*, +, data, mapping, etc.)
│   │       ├── processing.clj       # Layer → Entry pipeline
│   │       ├── ir.clj               # Intermediate representation specs
│   │       ├── plotly.clj           # Plotly backend
│   │       ├── vegalite.clj         # Vega-Lite backend
│   │       ├── thing-geom.clj       # thi.ng/geom backend
│   │       └── thing-geom-themes.clj # ggplot2 themes for thi.ng/geom
│   └── v2/
│       ├── api.clj                  # Public API functions
│       ├── dataflow.clj             # Core dataflow engine
│       ├── inference.clj            # Inference rules for deriving values
│       ├── plotly.clj               # Plotly backend renderer
│       ├── hanami.clj               # Hanami backend renderer  
│       ├── ggplot.clj               # ggplot2-compatible API
│       └── ggplot/
│           └── themes.clj           # Complete ggplot2 theme system (8 themes)
├── notebooks/
│   ├── building_aog_v2.clj          # AoG v2 design (comprehensive, 2,807 lines)
│   ├── building_aog_v3.clj          # AoG v3 vertical presentation (streamlined, 1,464 lines)
│   ├── building_aog_in_clojure.clj  # Design exploration: AoG v2 with normalized representation
│   │
│   └── tableplot_book/
│       ├── # V2 (ggplot2-style) notebooks
│       ├── ggplot2_walkthrough.clj      # Complete ggplot2 API tutorial
│       ├── v2_dataflow_from_scratch.clj # Educational: building dataflow from scratch
│       ├── v2_dataflow_walkthrough.clj  # V2 dataflow tutorial
│       ├── plotly_backend_dev.clj       # Plotly backend development
│       │
│       ├── # V1 (AlgebraOfGraphics) notebooks
│       ├── # Learning Path (4 notebooks):
│       ├── aog_tutorial.clj             # AoG introduction
│       ├── aog_plot_types.clj           # Comprehensive plot type reference
│       ├── aog_backends_demo.clj        # Backend comparison (Plotly/Vega/thi.ng)
│       ├── aog_examples.clj             # Real-world dataset examples
│       │
│       ├── # Backend Guides (3 notebooks - Equal Depth):
│       ├── backend_plotly.clj           # Plotly backend complete guide
│       ├── backend_vegalite.clj         # Vega-Lite backend complete guide
│       ├── backend_thing_geom.clj       # thi.ng/geom backend complete guide
│       │
│       ├── # Technical (2 notebooks):
│       ├── aog_architecture.clj         # Architecture and pipeline
│       └── theme_showcase.clj           # Theme examples
│       │
│       └── archive/                     # Archived/old notebooks
├── test/
│   └── tableplot/v2/                # V2 test suite
└── docs/
    ├── AOG_V2_DESIGN_DECISIONS.md   # Design decisions from Session 15
    ├── GGPLOT2_IMPLEMENTATION.md    # ggplot2 implementation guide
    ├── v2_dataflow_design.md        # V2 dataflow design decisions
    └── v2_refactoring_summary.md    # V2 refactoring history
```

## Core Architecture: V2 Dataflow Model

### Key Concept: Three-Stage Process

V2 uses a dataflow architecture that separates **specification** → **inference** → **rendering**:

1. **Construction**: User builds a spec using high-level API functions
2. **Inference**: System automatically derives missing values using rules
3. **Rendering**: Backend converts spec to visualization (Plotly, Hanami, etc.)

### Substitution Keys Convention

The dataflow model uses **substitution keys** (keywords starting with `=`) as placeholders:

```clojure
;; Template with placeholders
{:x :=x-data
 :y :=y-data
 :type :=geom-type}

;; Spec with substitutions  
{:x :=x-data
 :y :=y-data
 :type :=geom-type
 :sub/map {:=x-data [1 2 3]
           :=y-data [4 5 6]
           :=geom-type "scatter"}}

;; After inference and substitution
{:x [1 2 3]
 :y [4 5 6]
 :type "scatter"}
```

**Important**: Substitution keys (`:=x-data`, `:=y-field`, etc.) are a **naming convention**, not an enforced concept. They're just keywords starting with `=` that the dataflow engine knows to substitute.

### Core Dataflow Functions

From `src/tableplot/v2/dataflow.clj`:

```clojure
;; Spec construction
(defn make-spec [template & {:as substitutions}]
  "Create a spec from a template with optional substitutions")

(defn add-substitution [spec k v]
  "Add a substitution to a spec")

(defn get-substitution [spec k]
  "Get a substitution value from a spec")

;; Inference
(defn infer [spec]
  "Run inference to derive all missing substitution values")

;; Finding substitution keys
(defn find-subkeys [spec]
  "Find all substitution keys (keywords starting with =) in spec structure")
```

### Inference Rules

Inference rules are registered functions that derive values for substitution keys:

```clojure
;; From src/tableplot/v2/inference.clj
(register-rule :=x-data
  (fn [spec context]
    (let [data (df/get-substitution spec :=data)
          x-field (df/get-substitution spec :=x-field)]
      (when (and data x-field)
        (mapv x-field data)))))

(register-rule :=title
  (fn [spec context]
    (let [user-labels (df/get-substitution spec :=labels)
          user-title (:title user-labels)]
      (or user-title
          ;; Auto-generate if no user title
          (let [x-field (df/get-substitution spec :=x-field)
                y-field (df/get-substitution spec :=y-field)]
            (when (and x-field y-field)
              (str (name y-field) " vs " (name x-field))))))))
```

**Rule Priority**: User-provided values always take precedence over inferred values.

## ggplot2 API Implementation

### Complete Implementation Status

The library implements a comprehensive ggplot2-compatible API in 4 phases:

#### Phase 1: Core Grammar ✅ Complete
- `ggplot` - Initialize plot with data
- `aes` - Define aesthetic mappings  
- `geom-point`, `geom-line`, `geom-bar` - Geometric objects
- `+` - Layer composition operator

#### Phase 2: Customization ✅ Complete  
- `xlim`, `ylim` - Set axis limits
- `labs` - Set labels (title, x, y)
- `scale-color-discrete`, `scale-x-continuous` - Scale transformations

#### Phase 3: Faceting ✅ Complete
- `facet-wrap` - Create small multiples by wrapping
- `facet-grid` - Create small multiples in grid layout

#### Phase 4: Themes ✅ Complete
Complete theme system with 8 themes matching R's ggplot2:

**Available Themes** (`src/tableplot/v2/ggplot/themes.clj`):
- `theme-grey` - Default ggplot2 theme (grey background, white grid)
- `theme-bw` - Black and white theme
- `theme-minimal` - Minimal theme with minimal non-data ink
- `theme-classic` - Classic theme with axis lines
- `theme-dark` - Dark background theme
- `theme-light` - Light theme with light grey lines
- `theme-void` - Completely empty theme
- `theme-linedraw` - Theme with only black lines

**Theme Structure** (hierarchical):
```clojure
{:plot {:background "color"
        :title {:font {...}}}
 :panel {:background "color"
         :grid {:major {...} :minor {...}}
         :border {...}}
 :axis {:text {:font {...}}
        :title {:font {...}}
        :line {...}
        :ticks {...}}
 :legend {:position "right|left|top|bottom"
          :background "color"
          :text {:font {...}}}}
```

**Theme Customization**:
```clojure
;; Apply a predefined theme
(-> (ggplot mtcars (aes :x :wt :y :mpg))
    (geom-point)
    (theme spec theme-minimal))

;; Customize using dot-notation
(-> (ggplot mtcars (aes :x :wt :y :mpg))
    (geom-point)
    (theme spec
           :plot.background "#f0f0f0"
           :panel.grid.major.color "#cccccc"
           :axis.title.font.size 14))

;; Combine theme with customizations
(-> (ggplot mtcars (aes :x :wt :y :mpg))
    (geom-point)
    (theme spec theme-dark)
    (theme spec :plot.title.font.size 16))
```

**Default Theme**: All plots automatically use `theme-grey` (matching R's ggplot2 default) unless overridden.

### Theme Implementation Details

**Theme Application** (`src/tableplot/v2/plotly.clj`):
- Themes are applied during rendering to the Plotly layout
- ~20+ theme elements mapped to Plotly properties
- String titles/labels automatically converted to Plotly objects for font compatibility
- Deep merge preserves nested settings during customization

**Deep Merge Helper**:
```clojure
(defn- deep-merge
  "Recursively merges maps."
  [& maps]
  (apply merge-with
         (fn [x y]
           (if (and (map? x) (map? y))
             (deep-merge x y)
             y))
         maps))
```

### Example Usage

```clojure
(require '[tableplot.v2.ggplot :as gg])
(require '[tableplot.v2.ggplot.themes :as themes])

;; Basic scatter plot with default theme
(-> (gg/ggplot mtcars (gg/aes :x :wt :y :mpg))
    (gg/geom-point)
    (gg/labs :title "Fuel Efficiency vs Weight"))

;; With custom theme
(-> (gg/ggplot mtcars (gg/aes :x :wt :y :mpg :color :cyl))
    (gg/geom-point)
    (gg/theme spec themes/theme-minimal)
    (gg/labs :title "MPG by Weight and Cylinders"))

;; Faceted plot with theme customization
(-> (gg/ggplot mtcars (gg/aes :x :wt :y :mpg))
    (gg/geom-point)
    (gg/facet-wrap [:cyl])
    (gg/theme spec themes/theme-bw)
    (gg/theme spec
              :panel.grid.minor.show false
              :axis.title.font.size 12))
```

## Educational Resources

### v2_dataflow_from_scratch.clj

**Purpose**: Standalone educational notebook that builds a minimal dataflow engine from scratch and uses it to implement a toy grammar of graphics.

**Structure** (~633 lines):
1. **Part 1: The Problem** - Why direct construction is limiting
2. **Part 2: The Dataflow Solution** - Three-stage process overview
3. **Part 3: Minimal Dataflow Engine** (~100 lines of core code):
   - `make-spec`, `add-substitution`, `get-substitution` - Spec construction
   - `find-subkeys` - Convention-based substitution key discovery
   - `register-rule!`, `infer-key` - Inference rule system
   - `infer-missing` - Automatic value derivation
   - `apply-substitutions` - Template expansion
   - `infer` - Complete inference pipeline

4. **Part 4: Toy Grammar of Graphics** - Simple ggplot-like API:
   ```clojure
   (-> (ggplot {:x [1 2 3] :y [4 5 6]})
       (aes :x :x :y :y)
       (geom :point))
   ```

5. **Part 5: The Power of Dataflow**:
   - **Inspection**: See derived values before rendering
   - **Override**: Replace any inferred value
   - **Composition**: Build complex specs from simple parts
   - **Multiple Backends**: Same spec → different outputs (Plotly, text, etc.)

6. **Part 6: Comparison** - Direct vs Dataflow side-by-side
7. **Part 7: From Toy to Real** - Mapping to full V2 implementation
8. **Part 8: Design Alternatives** - Other approaches considered

**Key Teaching Points**:
- Substitution keys are just a naming convention (keywords starting with `=`)
- Inference rules allow automatic derivation of missing values
- Specs are data, enabling inspection and transformation
- Multiple backends can render the same spec differently
- Template-based approach enables code reuse

**Reproducibility**: Uses `^:kind/pprint` markers for Clay rendering. No hardcoded outputs - Clay writes actual outputs on rendering.

## Backend Implementations

### Plotly Backend (`src/tableplot/v2/plotly.clj`)

**Main Function**:
```clojure
(defn spec->plotly
  "Convert V2 spec to Plotly spec."
  [spec]
  (let [inferred (df/infer spec)
        traces (spec->traces inferred)
        layout (spec->layout inferred)]
    {:data traces
     :layout layout}))
```

**Key Features**:
- Converts V2 specs to Plotly data/layout format
- Handles multiple geometry types (point, line, bar)
- Applies faceting (facet-wrap, facet-grid)
- Applies theme settings to layout
- Converts string titles/labels to Plotly objects for theme compatibility

**Trace Generation**:
- Maps aesthetic mappings to Plotly trace properties
- Handles color, size, shape aesthetics
- Groups data for faceting

**Layout Generation**:
- Applies scales (limits, axis titles)
- Applies labels
- Applies theme settings (colors, fonts, grids, etc.)
- Creates subplot layouts for faceting

### Hanami Backend (`src/tableplot/v2/hanami.clj`)

Alternative backend using Hanami/Vega-Lite for web-based visualizations.

## Key Dependencies

```clojure
{:deps
 {org.clojure/clojure {:mvn/version "1.11.1"}
  scicloj/tablecloth {:mvn/version "7.0-beta2"}      ; Data manipulation
  scicloj/kindly {:mvn/version "4.0.0-beta4"}        ; Visualization protocol
  scicloj/clay {:mvn/version "2.0.0-beta15"}         ; Notebook rendering
  org.scicloj/hanamicloth {:mvn/version "1.0.0"}     ; Hanami integration
  generateme/fastmath {:mvn/version "2.2.1"}}}       ; Math utilities
```

## Architecture Insights from Analysis

### Strengths of V2 Dataflow Model

1. **Separation of Concerns** (9/10)
   - Clean separation: spec construction → inference → rendering
   - Backends are swappable (Plotly, Hanami, custom)
   - Inference rules are modular and composable

2. **Declarative API** (8/10)
   - Threading macro `->` enables natural composition
   - Functions like `aes`, `geom-point` are pure data transformations
   - No imperative mutation

3. **Extensibility** (8/10)
   - New geometry types: just add inference rules + backend support
   - New backends: implement `spec->backend` function
   - New transformations: add functions that modify spec

4. **Inspectability** (10/10)
   - Can inspect spec at any stage
   - Can see what was inferred vs user-provided
   - Can override any inferred value

5. **Code Reuse** (9/10)
   - Templates enable sharing common patterns
   - Inference rules eliminate repetitive code
   - Backend-agnostic specs

### Current Limitations

1. **Learning Curve** (Convenience: 7/10)
   - Understanding substitution keys requires mental model shift
   - Debugging inference can be non-obvious
   - Error messages could be more helpful

2. **Verbosity in Implementation**
   - Inference rules require boilerplate (register-rule, context threading)
   - Finding subkeys uses tree walking (performance concern for large specs)

3. **Inference Rule Dependencies**
   - Some rules depend on others (e.g., :=x-data needs :=x-field)
   - No explicit dependency graph
   - Order matters but isn't enforced

4. **Context Threading**
   - Context parameter threaded through but rarely used
   - Could be simplified or removed

5. **Error Handling**
   - Missing inference rule throws generic error
   - Stack traces don't clearly show which spec caused issue
   - No validation of substitution key values

### Recommended Improvements

**High Priority**:
1. Add spec validation (check required keys, type validation)
2. Improve error messages (show spec context, suggest fixes)
3. Document inference rule dependency graph
4. Add debugging utilities (`explain-inference`, `show-rules`)

**Medium Priority**:
5. Optimize `find-subkeys` for large specs (memoization, caching)
6. Simplify context parameter (make it optional or remove)
7. Add more inference rules for common patterns
8. Standardize inference rule naming conventions

**Low Priority**:
9. Add spec diffing utilities (compare specs, show changes)
10. Add spec transformation utilities (map over layers, filter geometries)
11. Add spec composition patterns (merge, overlay)

## Known Issues and Fixes

### Issue 1: Theme ClassCastException (FIXED)
**Problem**: `java.lang.String cannot be cast to clojure.lang.Associative` when applying theme to title

**Cause**: Plotly layout title was a string, but theme application used `assoc-in` to set font

**Fix**: Modified `spec->layout` to convert string titles/labels to Plotly objects:
```clojure
;; Convert string to map for theme compatibility
(assoc :title (if (string? title)
                {:text title}
                title))
```

### Issue 2: Title Inference Not Respecting User Values (FIXED)
**Problem**: User-provided titles were being overwritten by auto-generated titles

**Cause**: `infer-title` was always generating from x/y fields, not checking `:=labels` first

**Fix**: Modified inference to check user-provided values first:
```clojure
(defn infer-title [spec context]
  (let [user-labels (df/get-substitution spec :=labels)
        user-title (:title user-labels)]
    (or user-title
        ;; Auto-generate only if no user title
        (when (and x-field y-field)
          (str (name y-field) " vs " (name x-field))))))
```

### Issue 3: Theme Customization Losing Nested Settings (FIXED)
**Problem**: When customizing themes, entire nested structures were being replaced

**Cause**: Using shallow `merge` instead of recursive merge

**Fix**: Added `deep-merge` helper function for recursive map merging

### Issue 4: Empty Sequence to min/max (FIXED)
**Problem**: `ArityException: Wrong number of args (0) passed to: clojure.core/min`

**Cause**: Scale inference rules called `(apply min x-data)` on potentially empty data

**Fix**: Added nil and empty checks:
```clojure
(when (and x-data (seq x-data))
  (if (every? number? x-data)
    {:type :linear
     :domain [(apply min x-data) (apply max x-data)]}
    {:type :categorical
     :domain (vec (distinct x-data))}))
```

## Testing

Tests are located in `test/tableplot/v2/` and cover:
- Core dataflow functions
- Inference rules
- Backend rendering
- ggplot2 API functions

Run tests:
```bash
clojure -X:test
```

## Development Workflow

### Interactive Development
The project uses REPL-driven development with Clay notebooks:

1. Start REPL: `clojure -A:dev`
2. Load namespace: `(require '[tableplot.v2.ggplot :as gg] :reload)`
3. Evaluate in notebook: `notebooks/ggplot2_walkthrough.clj`
4. Render with Clay: `(clay/make! {:source-path "notebooks/ggplot2_walkthrough.clj"})`

### Notebooks
- **ggplot2_walkthrough.clj**: Complete tutorial of ggplot2 API with examples
- **v2_dataflow_from_scratch.clj**: Educational notebook building dataflow from scratch
- **v2_dataflow_walkthrough.clj**: Tutorial of V2 dataflow concepts
- **plotly_backend_dev.clj**: Development and testing of Plotly backend

### Adding New Features

**Example: Adding a new geometry type**

1. Add geometry function to `src/tableplot/v2/ggplot.clj`:
```clojure
(defn geom-histogram [spec]
  (df/add-substitution spec :=geom :histogram))
```

2. Add inference rules to `src/tableplot/v2/inference.clj`:
```clojure
(register-rule :=histogram-bins
  (fn [spec context]
    (let [x-data (df/get-substitution spec :=x-data)]
      (when x-data
        (int (Math/sqrt (count x-data)))))))  ; Sturges' rule
```

3. Add backend support to `src/tableplot/v2/plotly.clj`:
```clojure
(defmethod geom->trace :histogram [spec geom-type]
  {:type "histogram"
   :x (:x-data spec)
   :nbinsx (:histogram-bins spec)})
```

4. Add tests and examples

## Documentation

- **AOG_V2_DESIGN_DECISIONS.md**: Design decisions from comprehensive review (Session 15)
- **GGPLOT2_IMPLEMENTATION.md**: Complete guide to ggplot2 implementation (all 4 phases)
- **v2_dataflow_design.md**: Design decisions for V2 dataflow architecture
- **v2_refactoring_summary.md**: History of V2 refactoring process
- **PROJECT_SUMMARY.md**: This file - comprehensive project overview

## Design Philosophy

1. **Data-Driven**: Specs are data structures, not objects
2. **Composable**: Small functions that combine with `->` threading
3. **Declarative**: Describe what you want, not how to build it
4. **Inspectable**: Always able to see and modify the spec
5. **Backend-Agnostic**: Same spec works with multiple rendering backends
6. **Inference Over Configuration**: Smart defaults, explicit overrides
7. **Convention Over Enforcement**: Substitution keys are just a naming pattern

## Future Directions

### Short Term
- Add more geometry types (histogram, boxplot, violin, etc.)
- Add statistical transformations (smooth, bin, count)
- Add position adjustments (dodge, stack, jitter)
- Improve error messages and debugging tools
- Add spec validation

### Medium Term
- Add interactive features (tooltips, selection, brushing)
- Add animation support
- Add more backends (Vega-Lite, Observable Plot)
- Add more themes (economist, fivethirtyeight, etc.)
- Performance optimization for large datasets

### Long Term
- Grammar of tables (ggplot2-style table creation)
- Grammar of dashboards (compose multiple plots)
- Statistical modeling integration
- Publication-ready export (PDF, SVG)

## Contributing

The project is under active development. Key areas for contribution:
- New geometry types
- New statistical transformations  
- Backend implementations
- Theme designs
- Documentation improvements
- Performance optimizations

## AoG v3: Vertical Feature Presentation (NEW!)

### Overview

**Location**: `notebooks/building_aog_v3.clj` (1,464 lines)

A **pedagogical reorganization** of building_aog_v2.clj that demonstrates the key benefit of multimethod architecture: **vertical feature presentation**. Each feature (scatter, linear, histogram) is now a complete "chapter" with implementation directly followed by examples, making it easier to learn one feature at a time.

**Status**: Complete and tested. Ready for use as streamlined introduction to the AoG v2 design.

### Structure

**Part I: Foundation** (~550 lines)
- Introduction & context (streamlined from v2)
- Core concepts (layer representation, operators, dataflow)
- Infrastructure setup:
  - Constants, helper functions
  - **Multimethod declarations** (extension points)
  - Rendering infrastructure (faceting, domain computation)
  - Composition operators (`*`, `+`)
  - Basic constructors (`data`, `mapping`, `facet`, `scale`, `target`)
  - Dataset loading (penguins, mtcars, iris)
  - **All three rendering targets** (:geom, :vl, :plotly)

**Part II: Features** (~350 lines) - Self-contained vertical chapters
- **Feature 4: Scatter Plots**
  - 4.1 Implementation: `scatter` constructor, `defmethod` for apply-transform/transform->domain-points/render-layer
  - 4.2 Examples: Basic scatter, grouped scatter, with opacity, plain Clojure data
- **Feature 5: Linear Regression**
  - 5.1 Implementation: `linear` constructor, `compute-linear-regression`, defmethods
  - 5.2 Examples: Single regression, scatter+regression, grouped regression, scatter+grouped regression
- **Feature 6: Histograms**
  - 6.1 Implementation: `histogram` constructor, `compute-histogram`, defmethods
  - 6.2 Examples: Basic histogram, grouped histogram, custom bin count

**Part III: Advanced Topics** (~150 lines)
- Multi-layer composition (scatter+regression examples)
- Faceting (column, row, grid)
- Multiple rendering targets (same spec → different outputs)
- Custom scale domains

**Part IV: Reflection** (~100 lines)
- Design trade-offs (what we gained vs what we paid)
- Implementation status (complete vs missing features)
- Integration path (coexistence with v1/v2)

### Key Improvements Over V2

1. **Vertical Learning**: Implementation → Examples for each feature (no 500-line gap)
2. **Incremental Complexity**: Scatter (simplest) → Linear (medium) → Histogram (complex)
3. **Self-Contained Chapters**: Can read "Feature 5" without reading "Feature 6"
4. **Clear Extension Pattern**: Each feature follows same structure
5. **Focused Content**: ~50% of v2 size, concentrates on demonstrating multimethod benefits

### Bug Fixes in V3

Two important fixes were applied during the reorganization:

**Fix 1: `scatter` function - Added 3-arity**
```clojure
(defn scatter
  ([]
   [{:aog/plottype :scatter}])
  ([attrs-or-layers]
   ...)
  ([layers attrs]              ;; NEW: enables (-> layers (scatter {:alpha 0.5}))
   (* layers (scatter attrs))))
```

**Fix 2: `*` operator - Dataset detection**
```clojure
(defn *
  ...
  ([x y]
   (let [;; Helper: check if a map is a layer (has :aog/* keys) or data
         is-layer? (fn [m]
                     (and (map? m)
                          (some #(= "aog" (namespace %)) (keys m))))
         to-layers (fn [v]
                     (cond
                       (layers? v) v
                       (is-layer? v) [v]
                       (map? v) [{:aog/data v}]  ;; Wrap datasets!
                       ...))
```

**Why this matters**: Datasets implement Clojure map interfaces. Without detection, `merge` would spread dataset columns into the layer map. The fix detects "no `:aog/*` keys = data" and wraps as `{:aog/data dataset}`.

### What's Missing from V3 (Compared to V2)

V3 is a **focused demo**, not a complete reference. Missing content (~50%):

- Extensive pedagogical content (Tableplot journey, motivation, detailed context)
- Design exploration narrative (nested problem → flat solution evolution)
- Delegation strategy section (detailed rationale for what we compute vs delegate)
- Glossary of visualization terminology
- Detailed API documentation sections
- ~12 examples (especially Vega-Lite and Plotly examples)
- Faceting architecture discussion
- Backend agnosticism section
- Comprehensive summary and next steps

### V2 vs V3 Comparison (Corrected 2025-12-26)

| Aspect | **V2** | **V3** |
|--------|--------|--------|
| **Size** | 2,807 lines | 1,927 lines |
| **Purpose** | Comprehensive reference with all examples | Pedagogically reorganized with focused examples |
| **Organization** | Horizontal (all impl, then all examples) | Vertical (impl→examples per feature) |
| **Narrative Content** | Complete (~1,400 lines) | Core narrative (~950 lines) |
| **Code Examples** | 27 numbered examples | 15 focused examples |
| **Coverage** | 100% (all content) | ~69% (31% reduction) |
| **Best For** | Complete reference, all variations | Learning resource, streamlined introduction |
| **Target Examples** | Dedicated sections for :vl and :plotly (14 examples) | Brief demonstration (2 examples) |
| **Status** | Comprehensive original | Condensed pedagogical version |

### Content Actually Missing from V3 (880 lines, 31%)

V3 is **NOT** just a reorganization—significant content has been intentionally removed:

**1. Faceting Architecture Discussion (~150 lines)**
- "Statistical Transforms Must Be Per-Facet"
- "Domain Computation: Shared vs Free Scales"  
- "Delegation Strategy: Control vs Leverage"
- "Rendering Architecture for :geom"
- "The Core Insight"

**2. Vega-Lite Examples Section (~200 lines)**
- V2 has 7 dedicated :vl examples (Examples 14-20)
- V3 has 1 brief :vl demonstration

**3. Plotly Examples Section (~200 lines)**
- V2 has 7 dedicated :plotly examples (Examples 21-27)
- V3 has 1 brief :plotly demonstration

**4. Detailed Example Explanations (~200 lines)**
- V2 includes "What happens here:" sections after most examples
- V3 uses more concise explanations

**5. Additional Narrative (~130 lines)**
- Extended backend agnosticism discussion
- "Escape hatch" examples (using `plot` for spec customization)
- More detailed sections throughout

**Key Point**: V3 is a **streamlined, pedagogically-reorganized version** with 31% less content. It preserves all CODE (functions, multimethods) but removes extensive narrative and most target-specific examples for clarity and focus.

### Recommended Usage (Updated)

- **Primary learning resource**: V3 - complete story with vertical organization
- **Blog post / publication**: V3 - polished narrative with focused examples
- **Teaching multimethods**: V3 - clearly demonstrates pedagogical benefits
- **Additional examples**: V2 - more variations of each feature
- **Historical reference**: V2 - original comprehensive exploration

## AoG v2 Design Exploration: Normalized Representation

### Overview

**Location**: `notebooks/building_aog_in_clojure.clj` (~71,700 lines)

A comprehensive design exploration of an alternative AoG API that uses **normalized layer representation** (always vectors) instead of context-dependent types. This is a working prototype demonstrating compositional graphics with clean separation between API, intermediate representation, and rendering.

**Status**: Design exploration / prototype for feedback and decision-making about Tableplot's future

### Core Design: Everything is Vectors

**Key Innovation**: Normalize all layer operations to return vectors, enabling:
- Standard library operations (`map`, `filter`, `into`, `mapv`)
- Predictable composition (no type checking needed)
- Transparent data flow

**API Structure**:
```clojure
;; Constructors return vectors (single-element for most, multi-element for +)
(data penguins)          ;; → [{:aog/data penguins}]
(mapping :x :y)          ;; → [{:aog/x :x :aog/y :y}]
(scatter {:alpha 0.7})   ;; → [{:aog/plottype :scatter :aog/alpha 0.7}]

;; * merges layers
(* (data penguins) (mapping :x :y) (scatter))
;; → [{:aog/data penguins :aog/x :x :aog/y :y :aog/plottype :scatter}]

;; + concatenates layers (overlay)
(+ (scatter) (linear))
;; → [{:aog/plottype :scatter} {:aog/transformation :linear}]

;; Distributive property: a * (b + c) = (a * b) + (a * c)
(* (data penguins) (mapping :x :y) (+ (scatter) (linear)))
;; → Two layers with shared data/mapping

;; Single plot function dispatches to targets
(plot layers)                    ;; Default: :geom (static SVG)
(plot layers {:target :vl})      ;; Vega-Lite (interactive)
(plot layers {:target :plotly})  ;; Plotly (3D, rich interactions)

;; Target can be compositional
(plot (* (data penguins) (mapping :x :y) (scatter) (target :vl)))
```

### Three Rendering Targets

All three targets use the **same layer specification**:

1. **:geom** (thi.ng/geom) - Static SVG with ggplot2 aesthetics
2. **:vl** (Vega-Lite) - Declarative interactive visualizations
3. **:plotly** (Plotly.js) - Rich interactive visualizations

**Target Independence**: `plot` function checks for `:aog/target` in layers or `:target` in options (options take precedence)

**Consistent Theme**: ggplot2 color palette (#F8766D, #00BA38, #619CFF, etc.) and gray background (#EBEBEB) with white gridlines across all targets

### Layer Representation

**Flat maps with namespaced keys** (`:aog/*`) prevent collision with data columns:

```clojure
{:aog/data penguins
 :aog/x :bill-length-mm
 :aog/y :bill-depth-mm
 :aog/color :species
 :aog/plottype :scatter
 :aog/alpha 0.7
 :aog/target :vl}
```

**Advantage**: Standard `merge` works perfectly (no custom layer-merge needed)

### Implementation Highlights

**Simplified * operator** (73% smaller):
```clojure
(defn *
  ([x] (if (map? x) [x] x))
  ([x y]
   (cond
     (and (map? x) (map? y)) [(merge x y)]
     (and (map? x) (vector? y)) (mapv #(merge x %) y)
     (and (vector? x) (map? y)) (mapv #(merge % y) x)
     (and (vector? x) (vector? y)) (vec (for [a x, b y] (merge a b)))))
  ([x y & more] (reduce * (* x y) more)))
```

**Standard library operations**:
```clojure
;; Conditional building with into
(if show-regression?
  (into scatter-layers regression-layers)
  scatter-layers)

;; Transform layers with mapv
(mapv #(assoc-in % [:aog/alpha] 0.8) layers)

;; Filter layers
(filterv #(= :scatter (:aog/plottype %)) layers)
```

**Specs as data** - All target specs are plain maps/vectors:
```clojure
;; Normal usage
(plot layers {:target :vl})

;; Advanced: Manipulate raw specs
(def vega-spec (layers->vega-spec layers 600 400))
(def customized-spec
  (-> vega-spec
      (assoc :title {...})
      (assoc-in [:encoding :x :axis] {...})))
(kind/vega-lite customized-spec)
```

### Vega-Lite Faceting Fix

**Problem**: Multi-layer specs with faceting don't work in Vega-Lite when `:column` encoding is inside each layer

**Solution**: Different faceting approaches based on layer count:
- **Single layer**: Faceting in encoding (standard VL)
- **Multi-layer**: Top-level `:facet` with `:spec` containing layers (VL composition)

```clojure
;; Multi-layer faceting
{:data {:values [...]}
 :facet {:column {:field "island" :type "nominal"}}
 :spec {:layer [{...scatter...} {...regression...}]}}
```

### Key Insights from Notebook

1. **Flat structure enables standard merge** - No custom layer-merge function needed
2. **Namespacing prevents collisions** - `:aog/*` keys coexist with any data columns
3. **Transparent IR** - Layers are inspectable plain maps, not opaque objects
4. **Target as data** - Rendering target can be specified compositionally via `(target :vl)` or options
5. **Julia's compositional approach translates to Clojure data** - Same algebraic concepts, different substrate

### Trade-offs

**Gains**:
- Composability through standard operations (`merge`, `assoc`, `mapv`, `filter`, `into`)
- Transparent intermediate representation (plain maps)
- Target independence (one API, multiple renderers)
- Flexible data handling (plain maps or datasets)
- Clear separation of concerns

**Costs**:
- Namespace verbosity (`:aog/color` vs `:color` - 5 extra chars)
- Novel operators (`*`, `+` shadow arithmetic)
- Incomplete feature set (smooth, density, histogram defined but not implemented)

### Relationship to V1 and V2

This design exploration is **not a replacement** for current Tableplot APIs, but investigates an alternative approach that could:
- Coexist as additional namespace (e.g., `scicloj.tableplot.v1.aog.normalized`)
- Inform future design decisions for V2 or beyond
- Provide a third compositional option alongside ggplot2-style (V2) and current AoG (V1)

**Addresses different concerns than V2**:
- V2 focuses on dataflow/inference for smart defaults
- AoG v2 focuses on algebraic composition with explicit operations
- Both can coexist serving different use cases

### Documentation Structure

The notebook follows a clear narrative (~1,600 lines):
1. **Context & Motivation** - Why explore alternatives
2. **Inspiration** - AlgebraOfGraphics.jl concepts
3. **Design Exploration** - Evolution from nested → flat+plain → flat+namespaced
4. **Proposed Design** - API overview and constructors
5. **Examples** - Progressive complexity (scatter → multi-layer → faceting)
6. **Implementation** - Helper functions and all three targets
7. **Multiple Targets** - Demonstrating target independence
8. **Specs as Data** - Programmatic manipulation examples
9. **Trade-offs** - Honest assessment of gains and costs
10. **Integration Path** - Coexistence with V1/V2
11. **Decision Points** - Open questions for community feedback
12. **Summary** - Key insights and implementation status

### Multimethod Rendering Architecture (2025-12-25)

**Major Refactoring**: Introduced multimethod-based rendering for extensibility and pedagogical clarity.

**Key Design Decision**: Transform computation is target-independent, rendering is target-specific.

**Implementation** (`notebooks/building_aog_v2.clj`):
```clojure
;; Multimethod dispatches on [target plottype-or-transform]
(defmulti render-layer
  (fn [target layer transform-result alpha]
    [target (or (:aog/transformation layer) (:aog/plottype layer))]))

;; Geom target methods
(defmethod render-layer [:geom :scatter] [target layer transform-result alpha] ...)
(defmethod render-layer [:geom :line] [target layer transform-result alpha] ...)
(defmethod render-layer [:geom :linear] [target layer transform-result alpha] ...)
(defmethod render-layer [:geom :histogram] [target layer transform-result alpha] ...)

;; Future: Add methods for other targets
(defmethod render-layer [:vl :scatter] [target layer transform-result alpha] ...)
(defmethod render-layer [:plotly :scatter] [target layer transform-result alpha] ...)
```

**Benefits**:
1. **Pedagogical**: Can introduce scatter plot with only `[:geom :scatter]` method, then add others incrementally
2. **Extensible**: Adding new targets is straightforward (define methods for each plottype)
3. **Separation**: Transform computation (`apply-transform`) shared across targets
4. **Cleaner**: Eliminates nested `case` statements

**Code Changes**:
- Removed 5 old functions: `render-scatter-viz`, `render-line-viz`, `render-regression-viz`, `render-histogram-viz`, `transform->viz-data`
- Added multimethod with 4 implementations for `:geom` target
- Updated `render-single-panel` to use multimethod

### Plain Data Structure Support (2025-12-25)

**Enhanced Validation and Documentation** for plain Clojure data structures:

**Supported Formats** (via `ensure-dataset`):
```clojure
;; tech.ml.dataset (passed through unchanged)
(data penguins-dataset)

;; Map of vectors (columnar data)
(data {:x [1 2 3] :y [4 5 6]})

;; Vector of maps (row-oriented data)
(data [{:x 1 :y 4} {:x 2 :y 5} {:x 3 :y 6}])
```

**Validation**: Helpful error messages for invalid data:
- "Map data must have sequential values (vectors or lists)" - with list of invalid keys
- "Data must be a dataset, map of vectors, or vector of maps" - with type info

**Implementation** (`notebooks/building_aog_v2.clj`):
```clojure
(defn- ensure-dataset
  "Convert data to tablecloth dataset with validation."
  [data]
  (cond
    (tc/dataset? data) data
    (map? data)
    (do (when-not (every? sequential? (vals data))
          (throw (ex-info "Map data must have sequential values..." {...})))
        (tc/dataset data))
    (and (sequential? data) (every? map? data))
    (tc/dataset data)
    :else
    (throw (ex-info "Data must be a dataset, map of vectors, or vector of maps" {...}))))
```

**Documentation**: Example 2 in notebook shows both formats with clear explanations.

### Recent Simplifications (2025-12-23)

The notebook underwent code simplification to focus on demonstrating design directions:

**Helper Functions Added** (~50 lines saved):
- `make-axes` - Extract repeated axis logic for thi.ng/geom
- `make-vega-encoding` - Build Vega-Lite encodings from layer aesthetics
- `group-by-color-plotly` - Group data by color for Plotly traces

**Code Reductions**:
- `compute-linear-regression`: 50 → 4 lines (uses only 2 points for straight line)
- `layer->scatter-spec`: 54 → 28 lines (using make-axes)
- `layer->line-spec`: 54 → 32 lines (using make-axes)
- `layer->vega-scatter`: 19 → 3 lines (using make-vega-encoding)
- `layer->vega-line`: 27 → 10 lines (using make-vega-encoding)
- `layer->plotly-trace`: 122 → 68 lines (using group-by-color-plotly)
- `layers->svg`: Simplified panel calculation, removed branching

**Restorations After Review**:
1. **Empty domain check** - `infer-domain` handles empty arrays safely (returns [0 1])
2. **2-point regression** - Linear regression uses exactly 2 points (start/end) since it's a straight line
3. **Faceting support** - Vega-Lite target supports `:facet`, `:col`, `:row` aesthetics with proper dimensions

**Total Reduction**: ~150-200 lines while maintaining full functionality

### Faceting Architecture Research

**Key Insight**: Proper faceting involves sophisticated architectural decisions beyond "render multiple times"

**AlgebraOfGraphics.jl Hybrid Scale Strategy** (see `docs/faceting_architecture_research.md`):

| Scale Type | Scope | Why |
|------------|-------|-----|
| Categorical (color groups) | Global | Consistency across facets |
| Continuous positional (X, Y) | Per-facet | Each panel shows optimal range |
| Continuous other (size, alpha) | Global | Maintains visual encoding |

**Separation of Concerns**:
- **Scale computation**: Determines domain/range
- **Axis linking**: Visual alignment (`:all`, `:colwise`, `:rowwise`, `:none`)

**Data Model**: Multi-dimensional arrays where dimensions = groupings, sliced via broadcast indexing

**Pipeline**:
1. Compute categorical scales globally
2. Create grid positions from faceting aesthetics
3. For each grid cell: slice data, compute per-facet X/Y scales, use global other scales
4. Render each panel
5. Apply axis linking, hide decorations, add labels

**Current Status**: Faceting supported in Vega-Lite target only. For `:geom` target, would require ~150-200 lines implementing:
- Data partitioning by faceting variable
- Global scale inference (with per-facet option for X/Y)
- Modified rendering to accept provided scales
- SVG grid layout with viewBox positioning

**Design Decision**: Keep Vega-Lite-only faceting for demo, acknowledge architectural complexity honestly

## V1: AlgebraOfGraphics Implementation

### Overview

V1 implements an AlgebraOfGraphics (AoG) API inspired by Julia's AlgebraOfGraphics.jl, providing a composable visualization grammar with three production-ready backends.

### Core Concepts

**Algebraic Operators**:
- `*` (multiplication) - Merge layer specifications
- `+` (addition) - Overlay multiple layers

**Pipeline**: `Layer → ProcessedLayer → Entry → Backend`

**Key Functions** (`src/scicloj/tableplot/v1/aog/core.clj`):
```clojure
(aog/data dataset)              ; Attach data to layer
(aog/mapping :x :y)             ; Map data columns to aesthetics
(aog/mapping :x :y {:color :species})  ; With additional aesthetics
(aog/scatter)                   ; Point geometry
(aog/line)                      ; Line geometry
(aog/bar)                       ; Bar geometry
(aog/linear)                    ; Linear regression transform
(aog/smooth)                    ; LOESS smoothing
(aog/histogram)                 ; Histogram
(aog/density)                   ; Kernel density estimation
(aog/draw spec opts)            ; Render visualization
```

**Example**:
```clojure
(require '[scicloj.tableplot.v1.aog.core :as aog])

;; Basic scatter plot
(aog/draw
 (aog/* (aog/data {:x [1 2 3] :y [4 5 6]})
        (aog/mapping :x :y)
        (aog/scatter)))

;; Multi-layer with grouping
(aog/draw
 (aog/* (aog/data penguins)
        (aog/mapping :bill-length :bill-depth {:color :species})
        (aog/+ (aog/scatter {:alpha 0.5})
               (aog/linear))))
```

### Three Production-Ready Backends

All three backends have **equal status** with comprehensive documentation:

#### 1. Plotly Backend (Default)
- **Superpower**: 3D visualizations + Rich interactivity
- **Output**: HTML + JavaScript
- **Best for**: Dashboards, exploratory data analysis, web apps
- **Features**: Pan, zoom, hover tooltips, 3D scatter/surface/mesh
- **Guide**: `notebooks/tableplot_book/backend_plotly.clj` (656 lines)

#### 2. Vega-Lite Backend
- **Superpower**: Faceting (small multiples)
- **Output**: SVG (via Vega-Lite spec)
- **Best for**: Publications, reports, faceted plots
- **Features**: Column/row/facet layouts, 5 themes, declarative specs
- **Guide**: `notebooks/tableplot_book/backend_vegalite.clj` (577 lines)

#### 3. thi.ng/geom Backend
- **Superpower**: Native polar coordinates + Pure Clojure
- **Output**: SVG (native)
- **Best for**: Radar charts, rose diagrams, print-ready graphics
- **Features**: True polar rendering, 9 ggplot2 themes, no JavaScript
- **Guide**: `notebooks/tableplot_book/backend_thing_geom.clj` (835 lines)

### Backend Usage

```clojure
;; Plotly (default via aog/draw)
(aog/draw spec)

;; Vega-Lite (explicit)
(require '[scicloj.tableplot.v1.aog.vegalite :as vegalite])
(vegalite/vegalite entry {:width 600 :height 400})

;; thi.ng/geom (explicit)
(require '[scicloj.tableplot.v1.aog.thing-geom :as tg])
(tg/entry->svg entry {:width 600 :height 400 :theme :grey})
```

### Backend Comparison Table

| Feature | **Plotly** | **Vega-Lite** | **thi.ng/geom** |
|---------|-----------|--------------|----------------|
| **Output Format** | HTML + JS | SVG (via spec) | SVG (native) |
| **Interactivity** | ✅ Full (pan, zoom, hover) | ⚠️ Limited (tooltips) | ❌ None (static) |
| **3D Support** | ✅ Native | ❌ None | ❌ None |
| **Polar Coordinates** | ⚠️ Manual | ❌ None | ✅ Native |
| **Faceting** | ⚠️ Manual subplots | ✅ Built-in (`col`, `row`) | ❌ None |
| **Themes** | ✅ 7+ built-in | ✅ 5 built-in | ✅ 9 ggplot2 themes |
| **File Size** | ~3MB (plotly.js) | ~100KB (spec) | ~10KB (SVG) |
| **Performance** | ⚠️ Slower (100k+) | ✅ Fast | ✅ Very fast |
| **Customization** | ✅ Extensive | ✅ Good | ✅ Full |
| **Dependencies** | Plotly.js (browser) | Vega-Lite (JVM) | Pure Clojure |
| **Use Cases** | Dashboards, 3D, EDA | Reports, facets | Polar, print, pure SVG |

### Entry IR (Intermediate Representation)

The backend-agnostic format that all backends consume:

```clojure
{:plottype :scatter           ; :scatter, :line, :bar, etc.
 :positional [[1 2 3]         ; x values
              [4 5 6]]        ; y values
 :named {:alpha 0.5           ; Optional styling
         :color "#0af"}}
```

### thi.ng/geom Themes

9 ggplot2-compatible themes (`src/scicloj/tableplot/v1/aog/thing-geom-themes.clj`):

- `:grey` (default) - Grey panel, white grid (ggplot2 default)
- `:bw` - Black & white
- `:minimal` - Minimal non-data ink
- `:classic` - Classic with axis lines
- `:dark` - Dark background
- `:light` - Light grey lines
- `:void` - Completely empty
- `:linedraw` - Black lines only
- `:tableplot` - tableplot default

**Usage**:
```clojure
(tg/entry->svg entry {:theme :minimal :width 600 :height 400})
```

### Documentation Philosophy

The V1 AoG documentation follows a clear structure:

1. **Learning Path** (4 notebooks): Tutorial → Plot Types → Backends → Examples
2. **Backend Equality** (3 guides): Equal depth, equal structure, clear unique strengths
3. **Technical Deep Dive** (2 notebooks): Architecture + specialized topics

**Key Feature**: Identical comparison tables across all three backend guides ensure objective decision-making based on actual needs rather than implicit recommendations.

## License

[License information if available]
