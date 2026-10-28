### Requirement: Shared premium physical putting terrain

For supported physical putting on current Parkland, Links, Heathland and Desert Generator 8 holes, the play stage SHALL display one shared premium green presentation over canonical green and fringe geometry. Family materials and decoration SHALL change only the surrounding scene. Canonical cup, ball, hazard geometry, projector scale, aim direction, and returned ball trajectory SHALL remain authoritative.

#### Scenario: Four supported families

- **WHEN** a supported physical green enters play in any of the four families
- **THEN** the shared renderer SHALL draw the real green and fringe with that family's surroundings
- **AND THEN** no illustrative prototype bunker, green outline, ball or cup SHALL be treated as canonical data.

#### Scenario: Read Green

- **WHEN** the existing Read Green control is enabled
- **THEN** contours and downhill arrows SHALL use the returned physical green contours and slope samples
- **AND THEN** the same green shape, aim controls, pace controls and shot rules SHALL remain in use.

#### Scenario: Unsupported or historical world

- **WHEN** required geometry or supported family provenance is absent, or an older policy applies
- **THEN** the original physical putting renderer SHALL be used without changing save data or full-hole premium eligibility.

#### Scenario: Different distances and widths

- **WHEN** a short, medium or long putt is displayed on desktop or a narrow stage
- **THEN** the uniform yard-space projection SHALL keep ball and cup positions and click-to-aim coordinates aligned
- **AND THEN** closer views MAY contain mostly close-cut turf while longer views reveal actual edges and context where visible.
