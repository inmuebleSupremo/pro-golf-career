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
