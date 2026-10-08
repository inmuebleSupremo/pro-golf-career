# course-persistence Specification

## Purpose
TBD - created by archiving change add-course-domain. Update Purpose after archive.
## Requirements
### Requirement: Courses Are Permanent Historical Assets

A Course generated during a career SHALL become a permanent historical asset. Historical tournament results SHALL always reference the exact Course used, and that Course SHALL remain reproducible from its seed, environment classification, and recorded generator version. For a V3 Course, deterministic regeneration SHALL reproduce its associated route, landing-zone, green-complex, and canonical geometry data under that recorded version. A generator version SHALL be retained or deliberately migrated under an explicitly approved compatibility policy for as long as supported saves/history can reference it.

#### Scenario: Historical V3 course regenerates its semantic identity

- **WHEN** a historical V3 Course is reconstructed from a saved career
- **THEN** it SHALL use its recorded V3 generator and reproduce the same spatial semantics and canonical geometry rather than the latest generator's interpretation

#### Scenario: Historical versions remain isolated

- **WHEN** a newer generator implementation changes
- **THEN** saved V1 and V2 course pools SHALL remain regenerated through their recorded historical implementations without fabricated V3 semantics

### Requirement: Course Revisions Do Not Invalidate History

Introducing a new generator version or new course-design profile behaviour SHALL NOT alter or invalidate Courses already recorded in history. New worlds MAY use the current version; existing worlds and legacy saves SHALL remain pinned to their historical version.

#### Scenario: New design generator does not rewrite an existing career

- **WHEN** a save created with an earlier course generator is loaded after a newer design-aware generator exists
- **THEN** the restored course pool and its historical venue geometry SHALL be regenerated through the earlier version

### Requirement: Persistence Carries No Tournament State

The persisted Course SHALL describe only the environment. It SHALL NOT carry tournament-specific data.

#### Scenario: Persisted course excludes competitive data

- **WHEN** a Course is persisted and later restored
- **THEN** it SHALL contain no leaderboards, prize money, rankings, competitors, or round scores

### Requirement: V4 course identity regenerates role-based hazard intent

A persisted V4 course/world SHALL regenerate its semantic hazard features and canonical hazard terrain deterministically from its established seed hierarchy, design inputs, and explicit V4 generator pin. Existing V1/V2/V3 persisted courses SHALL continue to regenerate through their historical pins without fabricated V4 feature metadata.

#### Scenario: Restored V4 course retains intentional hazards

- **WHEN** a V4 world is restored from its save provenance
- **THEN** its hazard features and canonical hazard polygons SHALL match the original generated world

### Requirement: Pin-Placement Policy Preserves Tournament History

Pin-placement policy provenance SHALL be retained independently of course-generator provenance wherever scheduled or archived tournament identity is persisted. A historical tournament's policy, course-generator version, and seed-derived course identity SHALL remain sufficient to reproduce its pin behaviour. Introducing V5 SHALL NOT reinterpret legacy V1–V4 policy events or alter their recorded results.

#### Scenario: Historical legacy event remains legacy

- **WHEN** an archived or completed tournament recorded with legacy pin policy is inspected or reproduced after V5 exists
- **THEN** it SHALL retain legacy pin behaviour and its course-generator provenance

