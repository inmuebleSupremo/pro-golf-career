## 1. Visual plan

- [x] 1.1 Add a pure deterministic decoration helper that produces bounded, non-overlapping placements within
  supplied canonical tree polygons.
- [x] 1.2 Add focused unit coverage for repeatability, polygon containment, and no-tree geometry.

## 2. Canonical renderer

- [x] 2.1 Add reusable SVG definitions and canonical-surface fills for the parkland material treatment.
- [x] 2.2 Render parkland deciduous canopies only through authoritative `TREES` polygons; preserve all existing
  terrain, target, ball, cup, aim, and trace coordinates and interactions.
- [x] 2.3 Refine marker/trace contrast for the richer terrain without changing their semantic states.

## 3. Verify

- [x] 3.1 Run focused renderer tests, frontend typecheck, lint, and production build.
- [x] 3.2 Capture reproducible development comparison evidence using the same canonical geometry and a second
  geometry with a different shape.
- [x] 3.3 Manual E2E review accepted the integrated Generator 7 Parkland/Links result as interim progress after
  authenticated gameplay aiming and backend-authored shot playback inspection. This acceptance does not claim final
  golf-course realism, does not retrofit absent V4 architecture, and does not make Generator 7 the default for new
  careers.
