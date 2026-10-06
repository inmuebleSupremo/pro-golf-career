## MODIFIED Requirements

### Requirement: Courses Are Permanent Historical Assets

A Course generated during a career SHALL become a permanent historical asset. Historical tournament results SHALL always reference the exact Course used, and that Course SHALL remain reproducible from its seed, environment classification, and recorded generator version. A generator version SHALL be retained or deliberately migrated under an explicitly approved compatibility policy for as long as supported saves/history can reference it.

#### Scenario: Historical result uses its recorded generator version

- **WHEN** a historical Course is reconstructed from a saved career
- **THEN** it SHALL use the generator version recorded for that career/course pool rather than the latest generator version

#### Scenario: World pin is unambiguous for the fixed course pool

- **WHEN** a current-world snapshot is restored
- **THEN** its single world-level generator-version pin SHALL govern the complete fixed course pool, whose Course generator-version stamps SHALL agree with that pin

#### Scenario: Version retirement is explicit

- **WHEN** maintainers propose removing a historical generator version
- **THEN** they SHALL provide a separately specified equivalent migration, continued compatibility adapter, or explicit support-retirement policy before removal

### Requirement: Course Revisions Do Not Invalidate History

Introducing a new generator version or new course-design profile behaviour SHALL NOT alter or invalidate Courses already recorded in history. New worlds MAY use the current version; existing worlds and legacy saves SHALL remain pinned to their historical version.

#### Scenario: New design generator does not rewrite an existing career

- **WHEN** a save created with an earlier course generator is loaded after a newer design-aware generator exists
- **THEN** the restored course pool and its historical venue geometry SHALL be regenerated through the earlier version
