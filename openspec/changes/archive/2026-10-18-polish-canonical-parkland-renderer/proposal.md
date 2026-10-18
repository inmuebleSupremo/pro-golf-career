## Why

The playable canonical SVG has trustworthy geometry and shot playback, but its current flat surface fills do
not yet reflect the quality of the authored course references. The first visible improvement should be a
polished parkland presentation that makes real holes easier and more satisfying to read without changing any
gameplay fact.

## What Changes

- Add a deterministic, parkland-led visual treatment to the existing canonical-hole renderer: layered grass
  tones, restrained mowing/grass texture, richer green/fringe separation, and improved bunker and water shading.
- Render deciduous canopies only inside authoritative `TREES` terrain polygons, using the existing seeded hole
  identity and the reusable SVG symbols already sourced from `docs/course-holes/parkland001.svg`.
- Improve the visual hierarchy of the tee, cup, target, ball, and authoritative trace while retaining their
  literal positions and existing interactions.
- Add focused coverage for deterministic, polygon-clipped cosmetic placement. No GraphQL, backend, course
  generation, shot-resolution, or targeting contract changes are included.

## Impact

- **Frontend:** canonical SVG renderer and a small pure visual-planning helper; the parkland kit becomes the
  first polished reference treatment while remaining reusable for other geometry.
- **Authority:** all playable shapes and all trace/aim endpoints remain backend supplied. Cosmetic details are
  deterministic and clipped within their matching canonical surface.
- **References:** `docs/course-holes/parkland001.svg` and its prototype guide visual direction; neither its
  layout nor any authored hole geometry is copied into generation or play.
