## Why

The opening `hole-spatial-model` requirement still calls two-dimensional layout presentation-only, although canonical geometry now determines production surface lookup and settlement. That stale wording contradicts both the implemented architecture and later requirements in the same specification.

## What Changes

- Replace the obsolete hybrid/presentation-only requirement with the canonical-authority rule already established by the completed migration.
- Retain the valid, bounded legacy zone-band compatibility seam for fixtures and transitional read data.
- Clarify that presentation renders canonical terrain and may not author gameplay landforms.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `hole-spatial-model`: align the opening spatial-authority requirement with canonical production resolution while retaining the legacy adapter boundary.

## Impact

- **Specifications only:** no application code, GraphQL schema, persistence, generator, calibration, or frontend implementation change.
- **Future design:** prevents obsolete presentation-only wording from constraining `course-design-foundations`.
