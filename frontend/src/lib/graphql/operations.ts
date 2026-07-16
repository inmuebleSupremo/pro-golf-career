import { graphql } from "./generated";

/*
 * Typed GraphQL operations. Authored against the generated `graphql()` function so
 * every field is checked against the backend schema at build time (schema drift
 * fails type-check rather than surfacing at runtime).
 */

/** The player's stored saved games, newest first. Proves the data-access layer. */
export const ListSavesDocument = graphql(`
  query ListSaves {
    listSaves {
      saveId
      savedAt
      season
      week
      playerGolferId
    }
  }
`);

/* --- Onboarding: create-your-golfer orchestration (createWorld → createPlayer → save). --- */

/** Creates a new world session from a seed; returns its id. */
export const CreateWorldDocument = graphql(`
  mutation CreateWorld($seed: Long!) {
    createWorld(seed: $seed) {
      id
      season
      week
    }
  }
`);

/** Creates the player's custom golfer in a world; returns the new golfer id. */
export const CreatePlayerDocument = graphql(`
  mutation CreatePlayer(
    $id: ID!
    $firstName: String!
    $lastName: String!
    $nationality: String!
    $startAge: Int!
    $archetype: String!
  ) {
    createPlayer(
      id: $id
      firstName: $firstName
      lastName: $lastName
      nationality: $nationality
      startAge: $startAge
      archetype: $archetype
    )
  }
`);

/** Persists a world session under a save id. */
export const SaveGameDocument = graphql(`
  mutation SaveGame($id: ID!, $saveId: ID!) {
    save(id: $id, saveId: $saveId)
  }
`);

/* --- Career hub: resume a save, then read the loaded session's overview. --- */

/** Loads a save into a new session; returns the restored session's status (incl. its new id). */
export const LoadCareerDocument = graphql(`
  mutation LoadCareer($saveId: ID!) {
    load(saveId: $saveId) {
      id
      season
      week
    }
  }
`);

/** The read-only career overview for a session: world status, goals, and schedule in one request. */
export const CareerOverviewDocument = graphql(`
  query CareerOverview($id: ID!) {
    world(id: $id) {
      id
      season
      week
      activePopulation
      hasPendingEvent
    }
    careerGoals(id: $id) {
      type
      target
      current
      achieved
    }
    playerSchedule(id: $id) {
      tournamentId
      week
      tier
      prestige
      entered
    }
    newsFeed(id: $id, limit: 12) {
      season
      type
      headline
      prominence
      subjectGolferId
    }
  }
`);

/** The player's golfer profile — identity, attributes, ranking, earnings; null when no player. */
export const PlayerProfileDocument = graphql(`
  query PlayerProfile($id: ID!) {
    playerProfile(id: $id) {
      golferId
      firstName
      lastName
      nationality
      age
      archetype
      worldRanking
      careerEarnings
      availableFunds
      tour
      events
      wins
      topTens
      attributes {
        attribute
        value
      }
    }
  }
`);

/** Sets the player's self-chosen career goals (replaces the current set). */
export const SetCareerGoalsDocument = graphql(`
  mutation SetCareerGoals($id: ID!, $goals: [CareerGoalInput!]!) {
    setCareerGoals(id: $id, goals: $goals)
  }
`);

/* --- Manage: schedule & availability (skip/enter events, rest the golfer). --- */

/** The player's full reviewable schedule — the manage view's source of truth for entered state. */
export const PlayerScheduleDocument = graphql(`
  query PlayerSchedule($id: ID!) {
    playerSchedule(id: $id) {
      tournamentId
      week
      tier
      prestige
      entered
    }
  }
`);

/** Skips a specific upcoming event for the player. */
export const SkipEventDocument = graphql(`
  mutation SkipEvent($id: ID!, $tournamentId: Long!) {
    skipEvent(id: $id, tournamentId: $tournamentId)
  }
`);

/** Re-enters a previously skipped event for the player. */
export const EnterEventDocument = graphql(`
  mutation EnterEvent($id: ID!, $tournamentId: Long!) {
    enterEvent(id: $id, tournamentId: $tournamentId)
  }
`);

/** Sets whether the player's golfer is resting (a blanket sit-out). */
export const SetRestingDocument = graphql(`
  mutation SetResting($id: ID!, $resting: Boolean!) {
    setResting(id: $id, resting: $resting)
  }
`);

/** Sets the player's development focus — an ordered list of Attribute enum names. */
export const SetDevelopmentFocusDocument = graphql(`
  mutation SetDevelopmentFocus($id: ID!, $focus: [String!]!) {
    setDevelopmentFocus(id: $id, focus: $focus)
  }
`);

/* --- Manage: equipment (current loadout, owned bag, and upgrade offers). --- */

/** The player's equipment in one request: current loadout, everything owned, and pending upgrade offers. */
export const EquipmentDocument = graphql(`
  query Equipment($id: ID!) {
    playerLoadout(id: $id) {
      name
      category
      quality
      cost
      forgiveness
      power
      workability
      feel
    }
    playerEquipment(id: $id) {
      name
      category
      quality
      cost
      forgiveness
      power
      workability
      feel
    }
    pendingEquipment(id: $id) {
      name
      category
      quality
      cost
      forgiveness
      power
      workability
      feel
    }
  }
`);

/** Buys a pending equipment upgrade by index (if affordable); auto-equips it. */
export const BuyEquipmentDocument = graphql(`
  mutation BuyEquipment($id: ID!, $index: Int!) {
    buyEquipment(id: $id, index: $index)
  }
`);

/** Switches the loadout to an already-owned item, by category (enum name) and item name. */
export const SelectLoadoutItemDocument = graphql(`
  mutation SelectLoadoutItem($id: ID!, $category: String!, $name: String!) {
    selectLoadoutItem(id: $id, category: $category, name: $name)
  }
`);

/* --- Manage: staff hiring (candidates awaiting a decision). --- */

/** The player's pending staff candidates awaiting a hire decision. */
export const PendingStaffDocument = graphql(`
  query PendingStaff($id: ID!) {
    pendingStaff(id: $id) {
      role
      name
      quality
      hiringCost
      seasonalSalary
    }
  }
`);

/** Hires a pending staff candidate by index (if affordable). */
export const HireStaffDocument = graphql(`
  mutation HireStaff($id: ID!, $index: Int!) {
    hireStaff(id: $id, index: $index)
  }
`);

/* --- Manage: sponsorships (offers awaiting a decision). --- */

/** The player's pending sponsorship offers awaiting a decision. */
export const PendingSponsorshipsDocument = graphql(`
  query PendingSponsorships($id: ID!) {
    pendingSponsorships(id: $id) {
      sponsor
      perSeasonPayment
      signingBonus
      durationSeasons
      grossValue
    }
  }
`);

/** Accepts a pending sponsorship offer by index. */
export const AcceptSponsorshipDocument = graphql(`
  mutation AcceptSponsorship($id: ID!, $index: Int!) {
    acceptSponsorship(id: $id, index: $index)
  }
`);

/* --- Play: advance the calendar, then play/sim a pending tournament event. --- */

/** Advances the session one week; pauses at the player's event (hasPendingEvent). */
export const AdvanceWeekDocument = graphql(`
  mutation AdvanceWeek($id: ID!) {
    advanceWeek(id: $id) {
      id
      season
      week
      hasPendingEvent
    }
  }
`);

/** The play state for a pending event: world status, current shot, and leaderboard, in one request. */
export const PlayStateDocument = graphql(`
  query PlayState($id: ID!) {
    world(id: $id) {
      id
      season
      week
      hasPendingEvent
    }
    currentSituation(id: $id) {
      holeNumber
      par
      shotNumber
      strokesThisHole
      distanceToPin
      lie
      pinLateral
      minReach
      maxReach
    }
    eventLeaderboard(id: $id) {
      position
      golfer {
        id
        name
      }
      score
      roundsPlayed
    }
    playerMadeCut(id: $id)
    playerScorecard(id: $id) {
      roundNumber
      currentHole
      toPar
      totalStrokes
      holes {
        holeNumber
        par
        strokes
      }
    }
  }
`);

/** Plays the current shot with a club/target/strategy decision; returns the outcome. */
export const PlayShotDocument = graphql(`
  mutation PlayShot($id: ID!, $decision: ShotDecisionInput!) {
    playShot(id: $id, decision: $decision) {
      finalSurface
      carry
      lateral
      distanceRemaining
      hazardEntered
      penaltyStrokes
      strokes
    }
  }
`);

/** Sims the current shot; returns the outcome. */
export const SimShotDocument = graphql(`
  mutation SimShot($id: ID!) {
    simShot(id: $id) {
      finalSurface
      carry
      lateral
      distanceRemaining
      hazardEntered
      penaltyStrokes
      strokes
    }
  }
`);

/** Sims the rest of the current round. */
export const SimRoundDocument = graphql(`
  mutation SimRound($id: ID!) {
    simRound(id: $id)
  }
`);

/** Sims the remainder of the event (all remaining rounds and any playoff). */
export const SimEventDocument = graphql(`
  mutation SimEvent($id: ID!) {
    simEvent(id: $id)
  }
`);

/** Completes the finished event so its result counts and the paused week resumes. */
export const CompleteEventDocument = graphql(`
  mutation CompleteEvent($id: ID!) {
    completeEvent(id: $id) {
      id
      season
      week
      hasPendingEvent
    }
  }
`);
