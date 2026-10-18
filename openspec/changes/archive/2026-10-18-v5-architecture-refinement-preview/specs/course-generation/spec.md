## ADDED Requirements

### Requirement: Experimental V5-derived architecture preserves retained generations
The experimental course-architecture refinement SHALL use a separate deterministic generator version while retained
V5 and older versions continue generating their historical layouts. It SHALL not become the default generator before
manual acceptance.

#### Scenario: Existing version-five world is restored
- **WHEN** a world pinned to generator version 5 requests a course
- **THEN** the retained V5 implementation SHALL generate it without the experimental route or terrain changes.

#### Scenario: Experimental Parkland or Links course is generated
- **WHEN** the same seed and environment request the experimental version
- **THEN** fairway, rough, boundary, green, recovery and hazards SHALL be backend-authored deterministic polygons
- **AND THEN** legal pins, surface lookup and human/AI rules SHALL use those same polygons.
