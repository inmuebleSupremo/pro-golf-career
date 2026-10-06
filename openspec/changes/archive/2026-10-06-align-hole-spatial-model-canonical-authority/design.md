## Context

The first `hole-spatial-model` requirement says resolution is one-dimensional and 2D layout is presentation-only. Later in that same specification, the zone-band requirement calls canonical geometry the first-class production contract and its final scenario states that two-dimensional layout is gameplay authority. `canonical-course-geometry` and the implemented `ShotResolver.spatialize` likewise use `CourseGeometry.surfaceAt(position)` for production settlement.

The old wording predates the completed canonical-course-geometry migration. It is no longer a valid compatibility contract: only the zone-band adapter remains transitional.

## Goals / Non-Goals

**Goals:**

- State one unambiguous spatial-authority rule for production gameplay.
- Preserve the legitimate 1D compatibility seam for legacy fixtures and controlled comparison/read paths.
- State that frontend presentation derives gameplay terrain from canonical geometry.

**Non-Goals:**

- Change resolver sampling, zone-band retirement timing, course generation, frontend rendering, or any code.
- Remove `ShotZoneProfile`/`HoleZones` compatibility requirements.
- Introduce new terrain, flight, putting, or shot-intent mechanics.

## Decisions

### Replace, rather than append to, the obsolete opening requirement

Modify `Hybrid Spatial Representation` in place so it requires canonical two-dimensional terrain for generated production holes and canonical lookup/settlement, while allowing one-dimensional carry/lateral sampling and the separately specified legacy adapter. Removing the section entirely was rejected because the opening position remains useful once stated correctly.

### Keep compatibility scope in its existing requirement

`Zone-Band Abstraction` already states that zone bands are a bounded compatibility representation and that new terrain must be canonical. It remains unchanged, avoiding accidental removal of fixture and transition support.

## Risks / Trade-offs

- **Readers mistake retained 1D sampling for presentation-only authority** → explicitly distinguish sampling coordinates from terrain authority.
- **Cleanup broadens into implementation work** → tasks are specification review/validation only; no source changes belong to this change.

## Migration Plan

Update the active base specification through this small delta, validate it strictly, and archive only after the wording is accepted. No runtime migration or rollback is needed because behavior already follows canonical authority.
