## ADDED Requirements

### Requirement: Pin-Placement Policy Preserves Tournament History

Pin-placement policy provenance SHALL be retained independently of course-generator provenance wherever scheduled or archived tournament identity is persisted. A historical tournament's policy, course-generator version, and seed-derived course identity SHALL remain sufficient to reproduce its pin behaviour. Introducing V5 SHALL NOT reinterpret legacy V1–V4 policy events or alter their recorded results.

#### Scenario: Historical legacy event remains legacy

- **WHEN** an archived or completed tournament recorded with legacy pin policy is inspected or reproduced after V5 exists
- **THEN** it SHALL retain legacy pin behaviour and its course-generator provenance
