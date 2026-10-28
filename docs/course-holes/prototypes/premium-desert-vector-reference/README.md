# Premium Desert — vector prototype V2 (route and bunker integration)

This is the **actual SVG and Python generator**, not a raster concept. It is a design-review prototype with fixed illustrative golf geometry, NOT a playable or backend-authoritative course renderer.

## Files

- `premium_desert_vector.svg` — editable native SVG, final product of the deterministic generator.
- `create_desert_svg.py` — the Python source producing the SVG; generation requires NumPy, SciPy, and Shapely, **not** browser runtime.
- `premium_desert_preview.png` — raster preview rendered from this exact SVG.
- `before-after.png` — V1 versus V2 comparison at equal scale.

## Exactly two targeted design changes

1. **Integrate access routes with the actual desert terrain.** The established connected path network is preserved. Its geometry is planned before landscaping, and it reserves a safe shoulder: raised rock benches are placed/reduced in traversable positions rather than crossed; individual rock clusters, shrubs, cacti, and small surface details are kept clear of the route. The route strokes use compacted-sand shoulders and wear rather than an artificial dark raised outline. Clearance is checked against the complete active and decorative hole bands and bunker hazards as well.
2. **Make bunkers appear excavated, not pasted on.** The sand footprints are cut out of the nested golf-surface SVG bands via a single shared mask, and the bunkers use irregular excavation outlines, contextual turf/desert shoulders, subtle recessed bank shadows, and a shaded sand floor rather than detached outer drop shadows. All original illustrative bunker locations and hazard dimensions are preserved.

## Deliberately preserved

- All three golf-corridor geometries, continuous green ends, green positions and putting surfaces.
- Bunker locations, existing hole lengths/turns, targeting illustration and tee marker.
- The connection topology and all four route centreline geometries, with no new shortcuts or spurs.
- Overall desert palette, ground textures, rock material treatment, active focus and full-stage side lighting.

The placement logic moves/scales only raised outcrops obstructing routes and screens nearby decorative rocks/shrubs, so the precise rock arrangement is necessarily different near those paths.

## Verification / limits

- The Python generator checks the access path buffers against all complete golf corridors, bunkers and hazards; shared physical junctions; rounded green containment; and its deterministic desert rock-placement guard.
- Native SVG source includes **one shared bunker cutout mask**; no raster artwork or external image references.
- The preview was rendered from the SVG and the generated file can be reproduced from the Python source.
- These checks apply **only to this illustrative composition**. A future generated-course renderer must calculate route/vegetation clearance and bunker mask geometry from the backend's canonical hole and hazard data.

Keep this V2 separate from the earlier V1 for review. Do not hand off to Codex until the reviewer confirms the two changes meet the intended realism.
