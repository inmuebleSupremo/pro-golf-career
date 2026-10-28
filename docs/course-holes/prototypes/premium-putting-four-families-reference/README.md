# Premium Putting Green — Shared SVG + Four Environment Treatments (V1)

## Purpose

This is **a source-driven, editable SVG visual-design prototype** for reviewing the putting experience across Parkland, Links, Heathland and Desert. Nothing is connected to the Pro Golf Career repository or a live save. The artwork is not a gameplay model and no game rules have been changed.

The **approved Parkland V1** is preserved unchanged. The three new environments are generated using its exact green geometry, fringe/collar layers, fine turf detail, ball, cup, flag, aim line, and optional Read Green graphics. Environmental substitutions change only peripheral scenery, bunkers' material colours, background, and lateral-shade hue (keeping the same falloff positions and opacity). The green artwork and guidance paths themselves are byte-identical in the generated source.

## The files

- `create_premium_putting_svg.py`: unmodified approved Parkland V1 generator.
- `create_putting_environment_variants.py`: new standard-library Python generator importing the approved module; produces 4 families × 2 viewing modes.
- `premium_putting_<family>_<mode>.svg`: eight editable native SVG outputs, with `family` = `parkland`, `links`, `heathland`, `desert`; `mode` = `natural` or `read_green`.
- Matching `.png` previews of each SVG (1200 × 675), produced by CairoSVG for review; preview renderers are **not** production dependencies.
- `comparison_natural.jpg`, `comparison_read_green.jpg`: four-up review sheets.

## Visual materials

| Family | Surrounding land, hazards and plants | Putting surface |
| --- | --- | --- |
| Parkland | Original layered woodland, shaded deciduous/pine canopies and pale bunkers | Original approved V1, unchanged |
| Links | Exposed sand / dry grass, low fescue, restrained dune contours and links-toned sand | Same approved V1 |
| Heathland | Dark olive/sandy inland heath, *small clustered flowering heather*, yellow gorse, scattered pines | Same approved V1 |
| Desert | Warm sandstone, gravel, rocky terraces, dry-wash features, sparse desert shrubs and cacti | Same approved V1 |

The two sample bunkers retain exactly the same fixed outlines and positions in all four environments; only the rim and sand materials vary. The same outside access track is retained and given context-appropriate surface colours. All environment-specific large objects are excluded from the fixed green/collar, bunkers and walking track with silhouette-aware distance checks. The composition remains strictly top-down.

## Regenerate

From this folder:

```sh
python create_putting_environment_variants.py
```

Python standard library only. No NumPy, SciPy, Shapely or app runtime are required. The generator emits SVG, not PNG.

## Important production boundaries

1. This is **fixed illustrative geometry**. The green boundary, collar, ball/cup and bunker positions are *not* derived from Generator 8 or an actual putting session. Production must use the backend's authoritative green geometry and present ball/cup positions.
2. Contour lines and slope arrows are **illustrative**. Their directions/locations must be computed from existing gameplay grade/break data and the live `Read Green` behaviour. Do not use static strokes as gameplay feedback.
3. Framing/zoom of a 2-foot vs 92-foot putt, animated roll, ball speed and aiming interaction all belong to the application; this prototype does not define them.
4. Tree/rock/plant placement is decorative. Respect real hazard and green boundaries. Surroundings must agree with the current course family without inventing gameplay obstacles.
5. Do not bake static sample bunkers, routes, trees or sand features into live greens. Reuse established premium course-family art utilities where practical and check actual route/decoration clearances.
6. **No Codex handoff yet**: this bundle is for human visual review and possible refinement. Do not interpret the previews as a production implementation.

## Preservation checks

- `premium_putting_parkland_natural.svg` and `premium_putting_parkland_read_green.svg` match approved V1 files byte-for-byte.
- For each mode, the green-rendering fragment from the original V1 builder is embedded verbatim in Links, Heathland and Desert; it covers the continuous nested green bands, turf shapes and shading, contours, arrows (when enabled), aiming line, cup, flag and ball.
- Every SVG is self-contained vector markup without raster `<image>` elements.
- The two view modes share all scenery and differ only in read guidance and original V1's subtle contour visibility.
