## ADDED Requirements

### Requirement: Premium Desert full-stage presentation

Normal play SHALL select Premium Desert for supported Generator 8 Desert holes with valid canonical
geometry. Vector material and scenery SHALL follow the approved V2 Desert SVG. Backend geometry SHALL
remain authoritative for all playing surfaces, hazards, aiming, shots, and saves.

#### Scenario: Supported generated Desert hole

- **WHEN** a supported Desert hole appears in normal play
- **THEN** its five tee-to-green visual bands SHALL remain continuous and rounded around the green
- **AND** canonical tee, green, bunker, water, and tree hazards SHALL keep their actual positions
- **AND** canonical bunker footprints SHALL cut through underlying maintained bands
- **AND** decorative terrain and vegetation SHALL clear playing surfaces and access tracks.

#### Scenario: Neighbouring golf property

- **WHEN** a neighbouring Desert hole is visible
- **THEN** it SHALL use real generated geometry at a consistent scale with a complete rounded green
- **AND** a hazard-clear walking track SHALL connect it to the active access network
- **AND** it SHALL ignore pointer input.

#### Scenario: Historical or unsupported geometry

- **WHEN** a Desert hole lacks supported Generator 8 provenance or valid canonical geometry
- **THEN** the existing presentation SHALL remain selected.
