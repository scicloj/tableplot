# AlgebraOfGraphics (AoG) Notebooks Guide

This directory contains notebooks demonstrating the AlgebraOfGraphics API and its backends.

## 📚 Recommended Learning Path

### Start Here (New Users)

1. **`aog_tutorial.clj`** ⭐ **START HERE**
   - Comprehensive step-by-step tutorial following AlgebraOfGraphics.jl
   - Covers fundamentals, aesthetics, transformations, and compositions
   - ~300 lines, progressive complexity
   - Backend: Plotly (interactive)

2. **`aog_plot_types.clj`** - Plot Type Reference
   - Complete reference for all available plot types
   - Organized by category (basic, statistical, transformations)
   - Examples of scatter, line, bar, box, violin, histogram, density, etc.
   - ~500 lines
   - Backend: Plotly (interactive)

3. **`aog_backends_demo.clj`** - Backend Comparison
   - Compare Plotly vs thi.ng/geom backends
   - Pipeline inspection (Layer → ProcessedLayer → Entry → Backend)
   - Polar coordinate showcase (rose diagrams, radar charts, spirals)
   - When to use which backend
   - ~430 lines
   - Backend: Both Plotly and thi.ng/geom

4. **`rdatasets_examples.clj`** - Real-World Examples
   - Real datasets (mtcars, iris, penguins, diamonds, flights, etc.)
   - Practical visualization patterns
   - ~300 lines
   - Backend: Plotly (interactive)

### Advanced/Technical

5. **`aog_thing_geom_architecture.clj`** - Architecture Deep-Dive
   - Four-layer pipeline architecture
   - IR schemas (Layer, ProcessedLayer, Entry, AxisEntries)
   - Processing pipeline internals
   - Backend implementation details
   - For contributors and advanced users
   - ~800 lines
   - Backend: thi.ng/geom (static SVG)

6. **`thing_geom_complete_reference.clj`** - Complete thi.ng/geom Reference
   - Comprehensive guide to thi.ng/geom backend
   - All features, styling, polar coordinates
   - 33 major sections including styling & customization
   - ~1500+ lines
   - Backend: thi.ng/geom (static SVG)

## 🎯 Quick Reference by Topic

### Learning AoG API
- Tutorial: `aog_tutorial.clj`
- Plot types: `aog_plot_types.clj`
- Architecture: `aog_thing_geom_architecture.clj`

### Polar Coordinates
- Demo: `aog_backends_demo.clj` (Part 7: Polar Coordinates)
- Architecture: `aog_thing_geom_architecture.clj` (Part 5: Polar Coordinates)
- Complete reference: `thing_geom_complete_reference.clj` (Polar sections)

### Statistical Transformations
- Tutorial: `aog_tutorial.clj` (Part 4: Statistical Transformations)
- Plot types: `aog_plot_types.clj` (Statistical Plots section)
- Backend demo: `aog_backends_demo.clj` (Part 3)

### Styling & Customization
- Complete reference: `thing_geom_complete_reference.clj` (Section 6)
- Backend demo: `aog_backends_demo.clj` (styling examples throughout)

### Real Datasets
- Examples: `rdatasets_examples.clj`

## 🔧 Backend Comparison

| Feature | Plotly.js | thi.ng/geom |
|---------|-----------|-------------|
| **Interactivity** | ✅ Excellent (zoom, pan, hover) | ❌ Static SVG only |
| **Polar coordinates** | ⚠️ Limited | ✅ **Excellent** ✨ |
| **3D plots** | ✅ Full support | ❌ 2D only |
| **Dependencies** | cljplotly (JS) | Pure Clojure |
| **Output format** | HTML + JS | SVG |
| **Server-side** | ⚠️ Requires browser | ✅ **Pure Clojure** ✨ |
| **Best for** | Dashboards, exploration | Papers, reports, polar plots |

### When to Use Which Backend

**Use Plotly for:**
- Interactive dashboards
- Data exploration
- 3D visualizations
- Web applications
- Real-time updates

**Use thi.ng/geom for:**
- Scientific papers (static SVG)
- **Polar coordinate plots** (rose diagrams, radar charts)
- Server-side rendering (no JS)
- Embedded graphics
- PDF generation
- Mathematical visualizations

## 📦 Final Structure (6 notebooks)

After consolidation from 9 notebooks:

**User-Facing (4):**
1. `aog_tutorial.clj` - Tutorial for learning AoG
2. `aog_plot_types.clj` - Plot type reference
3. `aog_backends_demo.clj` - Backend comparison (NEW - merged from 3 notebooks)
4. `rdatasets_examples.clj` - Real-world examples

**Technical/Advanced (2):**
5. `aog_thing_geom_architecture.clj` - Architecture guide for contributors
6. `thing_geom_complete_reference.clj` - Complete thi.ng/geom reference

## 🗂️ Archived Notebooks

The following notebooks have been consolidated and moved to `archive/`:
- `aog_demo.clj` → Merged into `aog_backends_demo.clj`
- `thing_geom_demo.clj` → Merged into `aog_backends_demo.clj`
- `thing_geom_aog_integration.clj` → Merged into `aog_backends_demo.clj`
- `thing_geom_styling.clj` → Content already in `thing_geom_complete_reference.clj`
- `thing_geom_test.clj` → Test file, moved to archive

## 💡 Tips

1. **New to AoG?** Start with `aog_tutorial.clj`
2. **Looking for specific plot type?** Check `aog_plot_types.clj`
3. **Want to use polar coordinates?** See `aog_backends_demo.clj` Part 7
4. **Building for production?** Choose your backend using `aog_backends_demo.clj`
5. **Contributing to tableplot?** Read `aog_thing_geom_architecture.clj`
6. **Need styling reference?** See `thing_geom_complete_reference.clj` Section 6

## 🔗 Related Resources

- [AlgebraOfGraphics.jl](https://aog.makie.org/) - The Julia inspiration
- [thi.ng/geom](https://github.com/thi-ng/geom) - Pure Clojure geometry library
- [Plotly.js](https://plotly.com/javascript/) - Interactive charting library
- [PROJECT_SUMMARY.md](../../PROJECT_SUMMARY.md) - Tableplot project overview
