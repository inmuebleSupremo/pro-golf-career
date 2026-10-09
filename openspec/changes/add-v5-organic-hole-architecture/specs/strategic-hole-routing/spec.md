# strategic-hole-routing Specification

## ADDED Requirements

### Requirement: V5 route and landing plans remain connected, bounded and actionable

A V5 hole SHALL own a deterministic architecture plan with one connected route corridor, ordered landing areas,
asymmetric width stations and a final approach relationship. The route MAY contain zero, one or two bounded
directional changes but SHALL not self-intersect, branch or claim an unmodelled mandatory obstacle route. Each
published/default target SHALL be reachable under current legal-club rules, lie on viable canonical terrain and
have a real current route or next-shot consequence.

Its routing form SHALL be explicit for composition evidence: straight, gentle-moving, dogleg or double-dogleg.
The latter SHALL remain bounded and rare at whole-course level.

#### Scenario: V5 dogleg remains compatible with current targeting

- **WHEN** a V5 hole has a bounded directional change and the ball is not yet in an approved green-attack range
- **THEN** current human guidance and AI policy SHALL use the same deterministic viable next route/landing target
- **AND THEN** neither path SHALL read a future resolver random result or target a canonical hazard core

### Requirement: V5 fairway geometry makes landing-space variation explicit

V5 SHALL compile ordered route-relative left and right width stations into its canonical fairway corridor. Stations
MAY create bounded necks, widenings, asymmetric preferred landing space and diagonal approach relationships, but
the resulting fairway/rough/boundary polygons SHALL remain simple, connected and authoritative.
The compiler SHALL use bounded smooth interpolation between stations and route changes so that canonical landforms
retain the station's tactical narrowing/asymmetry without visibly faceted transitions.

#### Scenario: Asymmetric landing space is canonical

- **WHEN** a V5 plan declares unequal left and right width at a landing station
- **THEN** the compiled canonical geometry SHALL expose that unequal usable space
- **AND THEN** rendering and settlement SHALL use the same polygons rather than a client-side approximation
