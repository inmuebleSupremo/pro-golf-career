## Decision

V5 is an explicit generator version. `World.create` selects it only for a new world; an existing snapshot keeps its pinned generator version and regenerates its original course pool. The live renderer continues to project canonical polygons only.

The V5 selector chooses bounded candidate routes and width stations from the seed, then compiles the selected plan into canonical terrain regions. Trees, water and bunkers are real surfaces in that geometry, not frontend decoration. The existing shot, lie and targeting seams therefore apply equally to human and AI play.

## Scope boundary

Restore only V5 architecture classes and their compatibility records. Do not copy `CourseLandscape*`, placement transforms, or `V6LandscapePlanner`; those remain solely on the rejected experimental branch.

## Review evidence

The checkpoint must render multiple known and unseen deterministic Parkland seeds with the normal canonical-hole component, and label the route, green, tree, bunker and water features taken directly from each generated geometry.

## Green-complex corrective decision

The V5 route formerly terminated at the putting-surface centre. That made the green a terminal fairway cap,
left no authoritative recovery ground behind it, and allowed a generated greenside bunker to extend past the
playable boundary. V5 now deliberately ends fairway before the complex, uses a separately authored widening
approach apron and asymmetric collar, and carries primary/deep rough around and beyond the shaped green.
The playable boundary contains this full recovery landscape. The SVG frame includes canonical terrain vertices
only for presentation bounds; it does not change the authoritative playable boundary or shot rules.
