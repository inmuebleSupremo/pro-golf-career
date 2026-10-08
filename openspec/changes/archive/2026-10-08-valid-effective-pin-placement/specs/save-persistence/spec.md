## ADDED Requirements

### Requirement: Backward-Compatible Pin-Policy Provenance

Save persistence SHALL serialize the world's future pin-placement default and each persisted scheduled/archived event's policy provenance without serializing duplicate cup coordinates or terrain polygons. A supported save made before this provenance existed SHALL deserialize all existing/default policy values as `LEGACY_V1`, not the latest policy. An explicitly migrated or newly created V5 world SHALL restore its policy choices exactly.

#### Scenario: Legacy save does not silently adopt V5

- **WHEN** a save lacking pin-policy fields is loaded
- **THEN** it SHALL retain legacy pin behaviour until the owner explicitly adopts V5 at a permitted boundary

#### Scenario: Migrated future schedule survives save/load

- **WHEN** a V5-adopted career is saved and loaded before one of its future scheduled events begins
- **THEN** that event and schedules generated later SHALL retain V5 while historical events retain their recorded legacy policy
