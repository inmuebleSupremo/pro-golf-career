## ADDED Requirements

### Requirement: Invertible canonical target mapping
The canonical SVG renderer SHALL provide tested project and unproject operations between canonical yard-space and SVG viewBox coordinates. For points in the interactive planning area, a project/unproject round trip SHALL remain within documented numerical tolerance.

#### Scenario: Pointer maps to selected aim point
- **WHEN** a user clicks or touches the canonical SVG
- **THEN** the viewBox coordinate is unprojected to a canonical aim point
- **AND THEN** rendering the target projects the same point back to the displayed location within tolerance.
