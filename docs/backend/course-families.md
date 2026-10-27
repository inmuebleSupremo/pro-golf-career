# Course-family identity

New worlds have four core visual/course families and five persisted course classifications:

| Family | New-world classification | Presentation |
|---|---|---|
| Parkland | `PARKLAND`, `WOODLAND` | Open Parkland and dense Woodland variations of Premium Parkland V2 when Generator 8 and the Parkland scene are eligible |
| Links | `LINKS` | Existing Links presentation |
| Heathland | `HEATHLAND` | Existing Heathland presentation |
| Desert | `DESERT` | Existing Desert presentation |

The persisted classification remains the course identity and controls weather and procedural inputs. `HEATHLAND` generates with the previously established Woodland architecture calibration, then receives its own stable Heathland identity and region. This shares the generator implementation without altering historical Woodland holes. The Woodland variation reuses Premium Parkland V2 with its established dense canopy settings. Open Parkland V2 now has fewer decorative crowns and larger managed grassland openings; the V1 comparison retains its prior seeded decoration. The underlying tree artwork, authoritative surfaces, projection, lighting, targeting, and gameplay remain unchanged.

On desktop-width Premium V2 stages, deterministic cart-path planning follows the active hole's maintained corridor from tee-side access to green-side access. It tests fixed offsets and, where necessary, a gradual clearance change to avoid surfaces, hazards, and protected crowns. Par-3 corridors no longer require a fairway polygon. Neighbouring holes remain authentic scale, outside the playable projection, and appear only with a clear on-screen junction to the active cart route. Up to two can occupy separate parts of one side where space permits. Narrow mobile framing still omits the wider decorative property.

The world snapshot pins the course-pool policy separately from the generator version. `FOUR_FAMILIES` is used for new worlds. `CORE_FOUR` retains the previous four-identifier course cycle and interprets `WOODLAND` as Heathland. A snapshot without a policy restores `LEGACY_SIX`, with Mountain and Coastal in the original positions. Both legacy policies preserve their existing course indices, geometry, scenes, and tournament venues. Mountain/Coastal rendering stays available for older saves; `TROPICAL` remains an authored scene token, not a persisted course classification.

Pro events keep their curated names, locations, and scene tokens; their host course indices still come from the generated pool. The curated Pro scenes cover nine Parkland and two Links regular/signature stops, one Desert stop, three Parkland majors, one Links major, and a Parkland finale. Coastal Heritage Classic, Sunshine State Showdown, and Blue Ridge Mountain Open retain their Coastal, Tropical, and Mountain scenes. A Pro scene can differ from the host classification. The live PlayingHole API therefore sends both the raw `canonicalBiome` and its policy-resolved `courseFamily`; the renderer uses that provenance for the Parkland/Heathland distinction.

The [Premium Parkland preview](/dev/premium-parkland-preview#family-variations) shows actual Generator 8 Parkland and Woodland holes from the same seed at the same scale. Heathland, Links, and Desert premium artwork remain separate future milestones.

The [18-hole Parkland contact sheet](../course-family-renders/parkland-18-hole-contact.png) records the complete seed-103 Generator 8 acceptance course at a 2.16 desktop stage ratio. All 18 holes have an active cart path; the 18 views contain 7 with no neighbour, 9 with one, and 2 with two. All 13 visible neighbouring paths have explicit junctions. Holes 1, 3, 9, 10, 11, 14, and 15 omit neighbours because no placed candidate passes the on-screen secondary-route and junction clearance checks. Hole 1 also loses some candidates to stage cropping. The planner leaves those scenes open rather than drawing an unconnected path or shrinking a neighbouring hole.
