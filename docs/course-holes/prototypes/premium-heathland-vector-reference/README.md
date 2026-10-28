# Premium Heathland — optimized SVG handoff

**Status:** Approved Heathland visual prototype, optimized for a smaller vector DOM and less micro-path geometry. This is a **static illustrative SVG prototype**, not yet a runtime renderer using Pro Golf Career's authoritative course geometry.

## Contents

- `premium_heathland_vector.svg`: optimized, editable, native vector SVG; no embedded raster artwork.
- `create_heathland_svg.py`: deterministic Python source of the SVG (generation-time NumPy, SciPy, Shapely required). Running it writes the SVG beside the script.
- `premium_heathland_preview.png`: raster *preview of the optimized SVG*, not a source of artwork.
- `before-after.png`: baseline on left, optimized on right, same 1600×900 SVGs rendered at 800×450 each.

## Optimization results (this static 1600 × 900 prototype)

| Metric | Approved realism baseline | Optimized | Change |
| --- | ---: | ---: | ---: |
| SVG elements | 11,222 | 6,390 | −43.1% |
| SVG `<path>` elements | 11,066 | 6,234 | −43.7% |
| SVG bytes | 4,392,397 | 3,354,704 | −23.6% |
| Compressed SVG bytes (gzip) | 1,320,892 | 1,079,123 | −18.3% |

Micro textures are grouped into compound vector paths; heather flowering sprigs, pine needle flecks and gorse flowers share batched fills/strokes where detail is tiny. Tiny *decorative-only* heather/gorse contours use fewer quadratic segments. The random seed and primary geometry, route layout, golf hazard construction, scene lighting, and all source gradients are unchanged. No raster texture/filter was introduced.

**Visual parity check:** Both images rendered with CairoSVG at 1600×900; average absolute RGB difference 0.211 on a 0–255 scale, 0.63% of pixels differed by >10 in any channel. This is a numerical approximation of fidelity, not a subjective visual guarantee; inspect preview yourself.

**Structural checks:** these groups are XML-byte-identical to the baseline: both decorative neighbour corridor groups, active surface bands, active fairway stripes, active green complex and bunkers, neighbour greens and bunkers, all paths and junctions, aim overlay, pond and sandy patches, lighting. The four route-clearance checks still pass.

**Offline performance indication:** CairoSVG 800×450 render times in a small local sample (3 runs per source) had median 3.10 s baseline vs 2.43 s optimized. **Not browser frame rate** and does not establish production shot-playback performance. The number of generated elements in an actual game scene can differ.

## Strict integration rules

1. Treat the optimized SVG and Python source as the approved procedural **art construction**. Do not reinterpret the PNG or invent a new style.
2. Replace illustrative static golf geometry with authoritative generated hole and hazard coordinates, preserving safe fallbacks for older saves.
3. Keep heather, gorse, pine trees, ground texture, and other decorations *off all active and decorative golf-hole surfaces*; protect greens, fairways, teeing grounds, bunkers, and water.
4. All active and neighbouring green ends use the same **continuous nested, rounded surface construction**, not a separate blob or lollipop cap.
5. Walking paths must **physically connect** access areas between holes and must route outside all playing corridors and hazards. No crossed playing areas, artificial switchbacks, or routes concealed beneath surfaces.
6. Maintain a true overhead view, the approved colours, scenic composition and side lighting; no global Parkland or Links restyling.
7. Check performance and correctness in **actual browser gameplay** after integration: long/dogleg/par-3 holes, decorative neighbours, responsive framing, aim and shot controls. Optimize generated scene based on measured performance rather than imposing this exact static prototype's element count as a runtime promise.

This optimization deliberately keeps the approved 11k-element baseline outside this bundle as a rollback reference. Only the optimized files in this bundle should be considered for the next Codex handoff.
