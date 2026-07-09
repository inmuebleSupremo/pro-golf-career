## ADDED Requirements

### Requirement: Support Team

Every Professional Golfer MAY employ a Support Team consisting of zero or more professional staff members. The composition of the team MAY change throughout a Career, and Support Team relationships SHALL persist until they are concluded.

#### Scenario: A golfer may employ zero or more staff

- **WHEN** a Professional Golfer exists in the world
- **THEN** they SHALL have a Support Team that may hold any number of staff members from zero upward, persisting until relationships are concluded

### Requirement: Staff Roles

The simulation SHALL support the following V1 staff roles: Coach, Caddie, Fitness Coach, Physiotherapist, and Sports Psychologist. Future versions MAY introduce additional roles without changing this requirement.

#### Scenario: The five V1 roles are available

- **WHEN** a staff member is employed
- **THEN** their role SHALL be one of Coach, Caddie, Fitness Coach, Physiotherapist, or Sports Psychologist

### Requirement: Staff Responsibilities

Each staff role SHALL possess clearly defined responsibilities consistent with the role (for example: Coach — long-term development; Caddie — strategic support; Fitness Coach — physical preparation; Physiotherapist — recovery support; Sports Psychologist — mental preparation).

#### Scenario: A role carries its defined responsibility

- **WHEN** a role's responsibility is inspected
- **THEN** it SHALL be the defined responsibility for that role

### Requirement: Independent Staff Entities

Support Team members SHALL be independent entities. The same staff member SHALL NOT simultaneously belong to multiple Professional Golfers unless explicitly permitted by future gameplay systems; staff are not required to be globally unique.

#### Scenario: A staff member belongs to one golfer

- **WHEN** a staff member is employed by a golfer
- **THEN** that staff member SHALL belong to that golfer's Support Team only
