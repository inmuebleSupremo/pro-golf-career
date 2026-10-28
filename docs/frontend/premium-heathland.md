# Premium Heathland SVG presentation

Normal play selects Premium Heathland for canonical Generator 8 holes with Heathland family, Heathland
biome and resolved course type, `GENERATOR_8_PRINCIPLED_ARCHITECTURE` source, and a valid playable
boundary. Older and unsupported saves use the established polished renderer. Historical Woodland remains
Woodland even when its persisted family value is `HEATHLAND`.

The approved source files remain unchanged in
[`premium-heathland-vector-reference`](../course-holes/prototypes/premium-heathland-vector-reference/README.md).
The unchanged SVG has SHA-256 `e654a156ae52d25518f8a1a5cd4b75469eb9de60a713a1552a14ddf6078bb948`.
The frontend uses its quadratic soft contours, heather and gorse lobes with compound blossom paths,
layered Scots-pine canopies and shadows, sandy ground marks, turf and sand gradients, fine stroke batching,
and lateral atmospheric falloff. The Python generator remains a design artifact. The development-only
`/dev/premium-heathland-source` route serves the exact SVG for visual comparison.

The fixed illustrative corridor is replaced by four continuous, rounded bands fitted to the canonical
Generator 8 projection. Canonical green, tee, bunker, waste, water and tree regions remain authoritative.
Decorative neighbouring holes are selected from real generated Heathland geometry at the active hole's
scale. A bounded placement search accepts neighbours only with a clear outside walking route and a
physical junction to the active access track. Their bands wrap their own green complexes. Decorative
property has no pointer events, and clicks outside the active stage cannot set an aim point.

Vegetation and ground textures are deterministic from the hole seed. Full-shape clearance rejects
heather, gorse, freely placed pines, sandy clearings, and fine grass marks that would overlap the active
or neighbouring outer corridors, playable surfaces, bunkers, water, or access routes. Small pines inside
canonical `TREES` polygons visualize an existing hazard and are checked against playable surfaces.

## Visual review

In development, `/dev/premium-heathland-preview` shows the approved SVG, generated artwork, current
renderer comparison, full-size active scene, narrow stage, and an expandable contact sheet of all 18
holes of one backend-generated Heathland course (Generator 8, seed 103). The fixture is emitted from Java by
`HeathlandPreviewFixture`, while normal play receives live GraphQL geometry.

- [Approved SVG versus generated hole](../course-family-renders/premium-heathland-approved-vs-generated.jpg)
- [Current versus Premium Heathland](../course-family-renders/premium-heathland-current-vs-premium.jpg)
- [18-hole contact sheet](../course-family-renders/premium-heathland-18-hole-contact.jpg)
- [Par 3](../course-family-renders/premium-heathland-par3.jpg)
- [Straight par 4](../course-family-renders/premium-heathland-straight-par4.jpg)
- [Dogleg par 4](../course-family-renders/premium-heathland-dogleg-par4.jpg)
- [Long par 5 with connected property](../course-family-renders/premium-heathland-par5-connected.jpg)
- [Narrow stage](../course-family-renders/premium-heathland-mobile.jpg)

The 18-hole integration test checks the nested bands, deterministic scene plan, connected route
endpoints, path clearance and decorative clearance against active and neighbouring golf surfaces. Browser
review checks responsive framing and aim exclusion. A full-size generated par 5 with two neighbours
measured 6,013 SVG descendants (4,925 paths) in Chromium, compared with 6,390 elements in the approved
source. The all-18 live preview reached about 70,000 SVG descendants and felt slower to navigate; its
contact sheet now loads only when requested. Normal play renders a single hole. Scene complexity depends
on actual geometry and accepted neighbours. Compound paths batch fine ground marks and flowers without
rasterizing the artwork.

An isolated local account and career were used to verify an authenticated browser shot: aiming changed
the target, `Play shot` resolved through the backend, and the stage advanced to shot 2 with a new lie.
The first available event rendered Parkland, so that check exercised the shared shot controls rather
than a live Heathland event. The isolated account and save directories were removed after review.
