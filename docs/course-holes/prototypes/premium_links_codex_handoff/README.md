# Premium Links — approved editable vector SVG prototype

This bundle contains the **actual vector implementation**, not a screenshot to reinterpret.
The standalone hole layout is illustrative; its coordinates are not authoritative
Pro Golf Career geometry.

## Deliverables

- `premium_links_true_vector.svg`: 1600 × 900 editable native SVG; no raster images.
- `create_links_svg.py`: deterministic Python generator for exactly this SVG.
- `premium_links_svg_preview.png`: visual inspection only.

To regenerate, install `numpy` and `scipy`, then run `python create_links_svg.py`.
The generator writes the SVG alongside itself and does not belong in the game's runtime.

SVG SHA-256: `19dcceb2a1ed615a4985322e2a53533ae9baf2f38755aae80854009be2cb1161`

## Corrections following visual review (2026-10-27)

**1. Realistic access routing.** The routes follow the *outside* of the active
and neighbouring course corridors, avoid the drawn bunker locations, and do
not cross their greens or fairways. A left-side neighbour access path joins the
active access path through a clear dune saddle; a second neighbour's route
continues safely beyond the top of the visible stage instead of making an
impossible cross-fairway shortcut. The lower-left decorative golf glimpse is
also kept clear. The route bends smoothly around the active mid-hole bunker.
The route samples were checked against all depicted course corridors, green
ellipses, bunker shapes and lower-left partial hole using geometry intersections.
These are static-prototype checks, not a substitute for collision/route planning
in dynamically generated real courses.

**2. Proper green-end geometry.** All five nested playing-surface bands now
share one continuous, rounded, cubic-curve rear closure, with ordered widths
and inset cap endpoints. The separate overlaid green-end blob has been removed.
The fairway-width taper near the tee is also smoothed to avoid a sharp step.
The green sits inside the continuous course, with a real apron beyond it.

No palette, dune artwork, typography, lighting, vegetation or bunker rendering
was redesigned for this corrective revision.

## Engineering integration guidance

1. Display this *exact static SVG* at its original viewBox in the frontend first.
2. Preserve the native SVG styling (gradients, contours, shrubs, lighting).
3. Replace static fairway/green/bunker/tee coordinates with backend-authoritative
   procedural Links geometry. Keep smooth, multi-band rounded green ends.
4. Generate the outside access routes by checking the actual projected surfaces,
   hazards and neighbouring holes. Never use the fixed prototype routes in a
   different geometry without route validation.
5. Preserve accepted gameplay targeting and pointer-exclusion, and do not
   change Parkland/Woodland artwork.
