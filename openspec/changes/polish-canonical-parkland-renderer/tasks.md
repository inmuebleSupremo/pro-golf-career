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
- [ ] 3.3 Manually inspect authenticated gameplay aim and backend-authored shot playback before seeking
  acceptance. **Rejected in manual E2E:** renderer-only work cannot remedy V4 Parkland's absent authoritative
  trees, water and richer architecture; it remains unapproved and unarchived.
