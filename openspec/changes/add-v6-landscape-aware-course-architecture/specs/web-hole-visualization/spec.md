# web-hole-visualization Specification

## ADDED Requirements

### Requirement: V6 renders the same geography in normal play and review

The normal-hole renderer SHALL place local V6 context behind literal effective canonical geometry. Context without a
matching in-bound canonical surface SHALL stay outside or be masked at playable boundaries. Gallery output SHALL
render padded local views and a complete course-scale map from the same landscape/placement identities and
transformed canonical geometry.

#### Scenario: Local and course-map context agree

- **WHEN** a V6 hole appears in normal play and on its complete course map
- **THEN** both views SHALL reference the same landscape feature/placement context at their respective scales
