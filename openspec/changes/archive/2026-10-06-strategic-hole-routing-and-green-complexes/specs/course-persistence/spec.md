## MODIFIED Requirements

### Requirement: Courses Are Permanent Historical Assets

A Course generated during a career SHALL become a permanent historical asset. Historical tournament results SHALL always reference the exact Course used, and that Course SHALL remain reproducible from its seed, environment classification, and recorded generator version. For a V3 Course, deterministic regeneration SHALL reproduce its associated route, landing-zone, green-complex, and canonical geometry data under that recorded version. A generator version SHALL be retained or deliberately migrated under an explicitly approved compatibility policy for as long as supported saves/history can reference it.

#### Scenario: Historical V3 course regenerates its semantic identity

- **WHEN** a historical V3 Course is reconstructed from a saved career
- **THEN** it SHALL use its recorded V3 generator and reproduce the same spatial semantics and canonical geometry rather than the latest generator's interpretation

#### Scenario: Historical versions remain isolated

- **WHEN** a newer generator implementation changes
- **THEN** saved V1 and V2 course pools SHALL remain regenerated through their recorded historical implementations without fabricated V3 semantics
