## ADDED Requirements

### Requirement: Personal Career Attention

The system SHALL provide a Career Inbox for the human-controlled golfer. The Inbox SHALL be a current-state, action-oriented projection containing at most one item for each qualifying MVP source area: Schedule, Development, Finances/Sponsorships, Equipment, and Staff. Each item SHALL identify its source area and direct the player to its existing authoritative career-management section. Inbox content SHALL derive from actual career state and SHALL NOT create fictional opportunities or outcomes.

#### Scenario: A qualifying source is surfaced once

- **WHEN** one MVP source area has qualifying current attention
- **THEN** the Career Inbox SHALL present one item for that area rather than one item per underlying offer, candidate, or event

#### Scenario: No current attention exists

- **WHEN** no MVP source area has qualifying current attention
- **THEN** the Career Inbox SHALL present an intentional empty state rather than unrelated world updates

### Requirement: MVP Source Qualification And Relevance

The MVP Career Inbox SHALL derive items only from the following current states:

- Schedule: the current week is week 1, the player has at least one unplayed eligible event, and the player has not acknowledged this season's Schedule review;
- Development: the player has banked Development Points sufficient to fund at least one permitted attribute raise, respecting the golfer's potential;
- Finances/Sponsorships: at least one sponsorship offer is pending and the sponsorship book has capacity;
- Equipment: at least one pending equipment upgrade is affordable or an equipment brand-deal offer is pending; and
- Staff: at least one pending staff candidate is affordable and the player has not acknowledged this season's Staff review.

An item SHALL cease to appear when its qualifying condition is no longer true. Existing domain expiry and acceptance rules SHALL remain authoritative.

#### Scenario: New-season Schedule planning is timely

- **WHEN** the player is in week 1 with at least one unplayed eligible event
- **THEN** the Career Inbox SHALL include one Schedule item

#### Scenario: Schedule planning does not become a perpetual notification

- **WHEN** the player advances beyond week 1
- **THEN** the Career Inbox SHALL no longer include the MVP Schedule item solely because future events remain

#### Scenario: Applied Development no longer needs allocation

- **WHEN** the player applies Development changes and the resulting banked points cannot fund any further permitted attribute raise
- **THEN** the Development Inbox item SHALL no longer be returned
- **AND THEN** the Inbox count SHALL no longer include Development

#### Scenario: A reviewed Schedule is complete for its season

- **WHEN** the player enters the Schedule section in week 1 and subsequently leaves it
- **THEN** the Career Inbox SHALL no longer include the Schedule item for that season
- **AND** the acknowledgement SHALL NOT change schedule entries, rest, or simulation behaviour

#### Scenario: A new season needs a fresh Schedule review

- **WHEN** a new season begins with one or more unplayed eligible events
- **THEN** a Schedule acknowledgement from a prior season SHALL NOT prevent the Career Inbox from including the new season's Schedule item

#### Scenario: A reviewed Staff opportunity is complete for its season

- **WHEN** the player follows the Staff Inbox item and the Staff landing successfully loads
- **THEN** the Career Inbox SHALL no longer include the Staff item for that season even when affordable candidates remain
- **AND** the acknowledgement SHALL NOT hire, release, change, or remove any staff candidate

#### Scenario: A new season needs a fresh Staff review

- **WHEN** a new season generates one or more affordable staff candidates
- **THEN** a Staff acknowledgement from a prior season SHALL NOT prevent the Career Inbox from including the new season's Staff item

#### Scenario: An unavailable offer is not presented as actionable

- **WHEN** sponsorship capacity is full, all pending equipment upgrades are unaffordable and no brand deal is pending, or all staff candidates are unaffordable
- **THEN** the Career Inbox SHALL not present the respective source as current actionable attention

#### Scenario: Resolved attention no longer misleads

- **WHEN** the player resolves an opportunity through its authoritative section or its existing source rule expires or invalidates it
- **THEN** the Career Inbox SHALL no longer present it as an available action

### Requirement: Authoritative Decision Handoff

The Career Inbox SHALL direct the player to the existing authoritative section for an item’s decision or review. The Inbox SHALL NOT provide a separate mutation path, apply a decision automatically, or duplicate an owning section’s decision rules.

#### Scenario: An Inbox item leads to its owner

- **WHEN** a player follows an Inbox item
- **THEN** they SHALL be taken to the applicable existing Schedule, Development, Finances, Equipment, or Staff section where that concern is authoritatively handled

#### Scenario: Inbox does not become a second decision system

- **WHEN** an Inbox item represents a decision governed by player-control behaviour
- **THEN** accepting, purchasing, hiring, allocating, resting, and scheduling SHALL remain governed by the existing authoritative capability

### Requirement: Inbox Scope Is Distinct From World News

The Career Inbox SHALL be distinct from the world/news feed. News SHALL continue to report significant simulation events; Inbox SHALL report only player-specific current career attention defined by its source policy.

#### Scenario: A world event is news but not Inbox attention

- **WHEN** a significant world event has no qualifying current career attention for the player
- **THEN** it MAY appear in the news feed but SHALL NOT be added to the Career Inbox solely because it is newsworthy

#### Scenario: A personal decision is attention but not news

- **WHEN** a qualifying personal opportunity has no independently newsworthy simulation event
- **THEN** it MAY appear in the Career Inbox without being added to the world/news feed

### Requirement: No Generic Inbox Lifecycle State In MVP

The MVP Career Inbox SHALL be derived from current state and SHALL NOT persist message history, read/unread state, dismissal, archive state, urgency categories, or Inbox-specific deadlines. The Inbox SHALL NOT block a normal week or season advance. The sole exceptions are the narrowly scoped current-season Schedule- and Staff-review acknowledgements required to represent completion of those review tasks; they SHALL NOT be a generic item acknowledgement mechanism.

#### Scenario: Inbox count reflects current state

- **WHEN** a player resolves a qualifying source state and the Career Inbox is read again
- **THEN** its items and any displayed count SHALL reflect the newly current qualifying state

#### Scenario: Inbox does not create a new pause

- **WHEN** qualifying Inbox items exist
- **THEN** the player SHALL retain the existing ability to advance normally without resolving those items

#### Scenario: Review acknowledgements survive loading without becoming a mailbox

- **WHEN** the player has completed a Schedule or Staff review, saves, and loads during the same season
- **THEN** the corresponding reviewed item SHALL remain absent
- **AND** no message history, item-level read state, archive, or acknowledgement for another source SHALL be created

### Requirement: Seasonal Review Handoff

The completed-season review SHALL remain a retrospective surface and SHALL NOT embed Development or Staff decision controls. On leaving the review, the experience SHALL direct the player to the Career Inbox when qualifying attention exists, or to the career hub when it does not. The Inbox SHALL remain non-blocking.

#### Scenario: A season review hands off to current attention

- **WHEN** the player leaves a completed-season review and one or more Career Inbox items qualify
- **THEN** the player SHALL be taken to the Career Inbox without being required to resolve an item

#### Scenario: A quiet season review returns to the hub

- **WHEN** the player leaves a completed-season review and no Career Inbox item qualifies
- **THEN** the player SHALL be taken to the career hub
