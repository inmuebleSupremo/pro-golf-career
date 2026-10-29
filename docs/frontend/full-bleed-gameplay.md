# Full-bleed gameplay stage

Supported Generator 8 premium holes use one viewport-sized SVG scene inside `PlayMode`. The HUD, hole chip, command dock, and drawer are sibling interface layers above that scene. The HUD and dock use dark translucent token surfaces; they do not terminate or duplicate the underlying renderer. `StageHole` continues to select either `Hole2d` or `PhysicalPuttingGreen`. Historical and unsupported presentations retain the stacked layout and original renderer.

## Scenic viewport and safe area

`PlayMode` measures its viewport, header, and dock with `ResizeObserver`. The **scenic viewport** covers the complete playing screen. The **gameplay-safe area** begins below the header and ends above the dock, with a small margin. Changes to dock height, viewport size, or shot mode update those measurements without a fixed breakpoint offset.

For normal play, `scenicFrame` expresses the full viewport in the canonical renderer's existing units. `canonicalRenderModel` fits the authoritative hole geometry inside the safe area at the prior effective scale, while the accepted family renderer receives the full scenic height and width for landscape, neighbouring holes, and lighting. The SVG viewBox matches the actual viewport aspect; its outer artwork may extend beyond that viewBox on narrow screens. Aim selection still uses the SVG screen CTM inverse followed by the same world unprojection.

The accepted terrain polygons terminate at the tee. In full-bleed mode a short mask blends that termination into the same renderer's continuous backdrop when it falls above the dock. It does not move the tee, ball, target, hazards, or interaction coordinates.

For physical putting, the existing yard-space projector remains the single authority for the ball, cup, contour, guide, animation, and inverse click mapping. Its scene viewBox and terrain extend around the safe putting frame into the full viewport. Narrow diagonal putts add only the padding needed to keep the ball and cup inside the visible width. The putting renderer and normal renderer share the same measured layout and interface layers.

The SVG receives aiming events directly. Hole information and pressure chips have no pointer capture. Header, dock, and drawer have their own pointer surfaces above the SVG, so control clicks cannot aim through them. Closed drawer contents are inert. The drawer backdrop and panel remain above the header and dock.

## Evidence and review

[Full-bleed evidence](../evidence/full-bleed-gameplay/README.md) contains production component captures at 1600 × 900 and 390 × 844. The development-only fixture route uses backend-generated Generator 8 holes and physical putting states; it is unavailable in production. The `layout=before` query keeps the prior stacked composition for same-state comparison without changing the production renderers.
