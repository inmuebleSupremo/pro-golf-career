## ADDED Requirements

### Requirement: Staff Hire and Release

For the player's golfer, staff hiring SHALL be a player decision rather than automatic. The World SHALL present a market of generated candidates for the player's open roles as pending staff offers; the player MAY hire a candidate (spending its hiring cost, if affordable) or release a current member. Mandatory salaries and under-debt release SHALL still apply automatically. When no candidate is hired, no staff SHALL be added.

#### Scenario: Candidates await the player's decision

- **WHEN** the world runs the player's season transition
- **THEN** a market of staff candidates for the player's open roles SHALL be held as pending offers and none SHALL be hired automatically

#### Scenario: Hiring a candidate signs and pays for them

- **WHEN** the player hires a pending candidate they can afford
- **THEN** that member SHALL join the support team, the hiring cost SHALL be charged to the player's account, and the offer SHALL be removed

#### Scenario: An unaffordable hire is declined

- **WHEN** the player attempts to hire a candidate they cannot afford
- **THEN** no member SHALL be hired and no charge SHALL be made

#### Scenario: The player releases a member

- **WHEN** the player releases one of their staff
- **THEN** that role SHALL become vacant and the departure SHALL be recorded in the team history

### Requirement: Equipment Purchase and Loadout

For the player's golfer, equipment purchases SHALL be a player decision rather than automatic. The World SHALL present generated upgrade offers as pending; the player MAY buy an upgrade (spending its cost, if affordable), which is added to their inventory, and MAY set their tournament loadout by selecting an owned item for a category. When nothing is bought, no equipment SHALL be acquired.

#### Scenario: Upgrades await the player's decision

- **WHEN** the world runs the player's season transition
- **THEN** equipment upgrade offers SHALL be held as pending and none SHALL be purchased automatically

#### Scenario: Buying an upgrade acquires and pays for it

- **WHEN** the player buys a pending upgrade they can afford
- **THEN** the item SHALL be added to their inventory, its cost SHALL be charged, and it SHALL be selectable in their loadout

#### Scenario: The player sets their loadout from owned gear

- **WHEN** the player selects an owned item for a category
- **THEN** their tournament loadout SHALL use that item for that category
