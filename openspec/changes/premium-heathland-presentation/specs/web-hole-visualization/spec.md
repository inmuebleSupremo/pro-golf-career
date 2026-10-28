## ADDED Requirements

### Requirement: Premium Heathland full-stage presentation

The normal play stage SHALL select Premium Heathland for supported Generator 8 Heathland holes with
canonical Heathland geometry. The presentation SHALL derive its vector materials and scenery from the
approved optimized Heathland SVG, while the backend remains authoritative for playing surfaces,
hazards, aiming, shots and saves.

#### Scenario: Supported generated Heathland hole

- **WHEN** a supported Heathland hole appears in normal play
- **THEN** its tee-to-green bands SHALL remain continuous and smoothly rounded around the green
- **AND** canonical tee, green, bunker, water and tree hazards SHALL retain their true positions
- **AND** decorative texture and vegetation SHALL clear playing surfaces and access routes.

#### Scenario: Visible neighbouring golf property

- **WHEN** a neighbouring Heathland hole is visible
- **THEN** it SHALL use generated geometry at consistent scale and complete rounded playing bands
- **AND** it SHALL have a clear walking route connected to the active-hole access network
- **AND** it SHALL not accept gameplay pointer input.

#### Scenario: Historical or unsupported geometry

- **WHEN** a saved hole lacks supported Generator 8 Heathland provenance or keeps historical Woodland identity
- **THEN** the existing appropriate presentation SHALL remain selected.
