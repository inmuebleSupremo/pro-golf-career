## ADDED Requirements

### Requirement: Links canonical-hole visual treatment
The playable canonical-hole renderer SHALL give Links holes an open fescue-and-dune visual identity distinct from Parkland, with exposed-turf fairways, muted greens and pot-bunker styling. Every rendered playing surface, bunker, water feature and tree region SHALL derive from backend canonical geometry; non-playable landscape texture SHALL NOT be represented as a gameplay hazard.

#### Scenario: Ordinary generated Links hole
- **WHEN** a V5 Links hole is rendered from its canonical terrain regions
- **THEN** the renderer SHALL distinguish fairway, rough, deep rough, green, bunker and any present water or tree regions with Links-specific materials
- **AND THEN** its aiming and shot feedback SHALL continue using the canonical projection and backend-authored positions.

#### Scenario: Links background contains decorative dune texture
- **WHEN** dune contours or distant fescue are shown beyond the canonical playable boundary
- **THEN** they SHALL remain presentation-only landscape context
- **AND THEN** they SHALL not add a playable surface, obstacle or hazard to the simulation.
