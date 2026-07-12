## MODIFIED Requirements

### Requirement: News Events

The Media System SHALL generate News Events when significant gameplay events occur — for example tournament victories, major upsets, ranking milestones, promotions, retirements, historic performances, and career milestones. Every News Event SHALL originate from an actual gameplay outcome. An **upset** (a low-ranked winner) SHALL be reported only once the World Ranking is established — that is, at least one full season of ranking has completed — so that the opening season, when the ranking is still empty and every winner reads as unranked, does not generate false upsets.

#### Scenario: A significant outcome produces a News Event

- **WHEN** a significant gameplay event occurs (such as a tournament victory)
- **THEN** a News Event SHALL be generated that originates from that outcome

#### Scenario: Diverse categories of news are produced

- **WHEN** different kinds of significant events occur over time
- **THEN** the generated News Events SHALL span diverse categories, not only tournament wins

#### Scenario: No false upsets in the opening season

- **WHEN** winners are decided in the opening season, before any full season of ranking has completed
- **THEN** no upset News Events SHALL be generated, even though every winner is still unranked

#### Scenario: Genuine upsets are reported once the ranking is established

- **WHEN** a low-ranked winner wins after at least one full season of ranking has completed
- **THEN** an upset News Event SHALL be generated
