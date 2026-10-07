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

/** Permanently deletes a stored save. */
export const DeleteSaveDocument = graphql(`
  mutation DeleteSave($saveId: ID!) {
    deleteSave(saveId: $saveId)
  }
`);

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
      pendingEventFinished
    }
    playerSchedule(id: $id) {
      tournamentId
      week
      tier
      prestige
      entered
      name
      location
      courseType
    }
    newsFeed(id: $id, limit: 12) {
      season
      type
      headline
      prominence
      subjectGolferId
    }
    playerSeasonStats(id: $id) {
      season
      events
      wins
      topTens
      cuts
      bestFinish
      earnings
    }
  }
`);

/** Current, aggregated career attention. Individual management spokes remain the decision owners. */
export const CareerInboxDocument = graphql(`
  query CareerInbox($id: ID!) {
    careerInbox(id: $id) {
      items {
        kind
        count
      }
    }
  }
`);

/**
 * A completed season's off-season review — the end-of-season moment. `season` omitted reviews the most
 * recently completed season; null when none has completed yet. The headlines include the player's own
 * milestones (split from the wider tour's by `playerGolferId`).
 */
export const SeasonReviewDocument = graphql(`
  query SeasonReview($id: ID!, $season: Int) {
    seasonReview(id: $id, season: $season) {
      season
      rankStart
      rankEnd
      playerGolferId
      stats {
        season
        events
        wins
        topTens
        cuts
        bestFinish
        earnings
      }
      development {
        attribute
        delta
        season
      }
      headlines {
        season
        type
        headline
        prominence
        subjectGolferId
      }
    }
  }
`);

/** The world's Hall-of-Fame inductions (name-enriched), for the legacy view. */
export const HallOfFameDocument = graphql(`
  query HallOfFame($id: ID!) {
    hallOfFame(id: $id) {
      golferId
      name
      season
      score
      careerWins
    }
  }
`);

/** The current World Ranking, name-enriched, for the rankings + rivals spokes. */
export const WorldRankingsDocument = graphql(`
  query WorldRankings($id: ID!, $limit: Int) {
    worldRankings(id: $id, limit: $limit) {
      position
      golferId
      name
      rankingValue
    }
  }
`);

/** The world Record Book — the current holder of each record (name-enriched). */
export const RecordsDocument = graphql(`
  query Records($id: ID!) {
    records(id: $id) {
      type
      holderGolferId
      holderName
      value
      season
    }
  }
`);

/** The player's fitness — condition, fatigue, availability, and any injury. */
export const PlayerFitnessDocument = graphql(`
  query PlayerFitness($id: ID!) {
    playerFitness(id: $id) {
      availability
      fitness
      fatigue
      canCompete
      canPlayThroughInjury
      injury {
        type
        severity
        rehabWeeksRemaining
      }
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
      retired
      attributes {
        attribute
        value
        potential
      }
    }
  }
`);

/** The player's current development focus (attribute enum names being prioritised). */
export const PlayerDevelopmentFocusDocument = graphql(`
  query PlayerDevelopmentFocus($id: ID!) {
    playerDevelopmentFocus(id: $id)
  }
`);

/** The player's attribute gains from their most recently developed season (end-of-season report). */
export const DevelopmentReportDocument = graphql(`
  query DevelopmentReport($id: ID!) {
    developmentReport(id: $id) {
      attribute
      delta
      season
    }
  }
`);

/** The player's banked Development Points and the cost-curve constants (for previewing spend costs). */
export const PlayerDevelopmentDocument = graphql(`
  query PlayerDevelopment($id: ID!) {
    playerDevelopment(id: $id) {
      points
      pointsPerRating
      costGrowth
      costReference
    }
  }
`);

/** Spends banked Development Points to raise attributes; returns the new balance. */
export const SpendDevelopmentPointsDocument = graphql(`
  mutation SpendDevelopmentPoints($id: ID!, $raises: [AttributeRaiseInput!]!) {
    spendDevelopmentPoints(id: $id, raises: $raises)
  }
`);

/** The player's achievements: the full catalogue in display order, each with its unlock state. */
export const AchievementsDocument = graphql(`
  query Achievements($id: ID!) {
    achievements(id: $id) {
      id
      category
      categoryLabel
      title
      description
      secret
      unlocked
      seasonUnlocked
    }
  }
`);

/** The player's career records — headline totals, scoring bests, and per-event history. */
export const CareerRecordsDocument = graphql(`
  query CareerRecords($id: ID!) {
    careerRecords(id: $id) {
      summary {
        events
        wins
        majors
        runnerUps
        topTens
        cutsMade
        bestFinish
        lowestRound {
          scoreToPar
          eventName
          location
          season
          date
          round
        }
        lowestTournament {
          scoreToPar
          eventName
          location
          season
          date
          round
        }
        careerEarnings
      }
      events {
        eventName
        location
        prestige
        tier
        tourTier
        appearances
        wins
        bestPosition
        results {
          season
          date
          position
          scoreToPar
          won
          madeCut
          location
          prize
          fairwaysHit
          fairwaysPossible
          greensInRegulation
          holesPlayed
          putts
          roundScores
        }
      }
    }
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
      name
      location
      courseType
    }
  }
`);

/** The player's season calendar with per-event results — the calendar page's source of truth. */
export const PlayerCalendarDocument = graphql(`
  query PlayerCalendar($id: ID!) {
    playerCalendar(id: $id) {
      tournamentId
      week
      tier
      prestige
      entered
      name
      location
      played
      result {
        winner {
          position
          name
          score
          madeCut
          earnings
        }
        topThree {
          position
          name
          score
          madeCut
          earnings
        }
        playerFinish {
          position
          name
          score
          madeCut
          earnings
        }
      }
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

/** Records the current season's Schedule review without changing any event entry or rest decision. */
export const AcknowledgeScheduleReviewDocument = graphql(`
  mutation AcknowledgeScheduleReview($id: ID!) {
    acknowledgeScheduleReview(id: $id)
  }
`);

/** Records this season's optional Staff review without changing candidates or staffing decisions. */
export const AcknowledgeStaffReviewDocument = graphql(`
  mutation AcknowledgeStaffReview($id: ID!) {
    acknowledgeStaffReview(id: $id)
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
      brand
      quality
      cost
      forgiveness
      power
      workability
      feel
      fit
    }
    playerEquipment(id: $id) {
      name
      category
      brand
      quality
      cost
      forgiveness
      power
      workability
      feel
      fit
    }
    pendingEquipment(id: $id) {
      name
      category
      brand
      quality
      cost
      forgiveness
      power
      workability
      feel
      fit
    }
    activeEquipmentDeal(id: $id) {
      brand
      perSeasonRetainer
      signingBonus
      durationSeasons
      gearTier
      gearFit
      seasonsRemaining
    }
    pendingEquipmentDeals(id: $id) {
      brand
      perSeasonRetainer
      signingBonus
      durationSeasons
      gearTier
      gearFit
      seasonsRemaining
    }
  }
`);

/** Buys a pending equipment upgrade by index (if affordable); auto-equips it. */
export const BuyEquipmentDocument = graphql(`
  mutation BuyEquipment($id: ID!, $index: Int!) {
    buyEquipment(id: $id, index: $index)
  }
`);

/** Signs a pending equipment brand deal by index (pays signing, kits + equips the brand's bag, locks in). */
export const AcceptEquipmentDealDocument = graphql(`
  mutation AcceptEquipmentDeal($id: ID!, $index: Int!) {
    acceptEquipmentDeal(id: $id, index: $index)
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
      nationality
      personality
      quality
      hiringCost
      seasonalSalary
    }
  }
`);

/** The player's current support-team roster, for the Manage Staff page. */
export const PlayerStaffDocument = graphql(`
  query PlayerStaff($id: ID!) {
    playerStaff(id: $id) {
      role
      name
      nationality
      personality
      quality
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

/** Releases a current staff member by role (a StaffRole enum name). */
export const ReleaseStaffDocument = graphql(`
  mutation ReleaseStaff($id: ID!, $role: String!) {
    releaseStaff(id: $id, role: $role)
  }
`);

/* --- Manage: sponsorships (offers awaiting a decision). --- */

/** The player's pending sponsorship offers awaiting a decision. */
export const PendingSponsorshipsDocument = graphql(`
  query PendingSponsorships($id: ID!) {
    pendingSponsorships(id: $id) {
      sponsor
      industry
      perSeasonPayment
      signingBonus
      durationSeasons
      grossValue
    }
  }
`);

/** The player's sponsorship book: the concurrency cap and the currently-active signed agreements. */
export const SponsorshipStatusDocument = graphql(`
  query SponsorshipStatus($id: ID!) {
    sponsorshipStatus(id: $id) {
      maxConcurrent
      active {
        sponsor
        industry
        perSeasonPayment
        seasonsRemaining
      }
    }
  }
`);

/** Accepts a pending sponsorship offer by index; returns false when the concurrency cap blocks it. */
export const AcceptSponsorshipDocument = graphql(`
  mutation AcceptSponsorship($id: ID!, $index: Int!) {
    acceptSponsorship(id: $id, index: $index)
  }
`);

/* --- Play: advance the calendar, then play/sim a pending tournament event. --- */

/** Advances the session to the end of the season, simming any events the player is entered in. */
export const AdvanceSeasonDocument = graphql(`
  mutation AdvanceSeason($id: ID!) {
    advanceSeason(id: $id) {
      id
      season
      week
      hasPendingEvent
    }
  }
`);

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
      shotRevision
      aimEnvelope { minX maxX minY maxY }
      guidance {
        safe { x y }
        primary { x y }
        aggressive { x y }
        clubs { club label nominalCarry normalReach }
      }
      reachable {
        startDistance
        endDistance
        regions {
          surface
          halfWidth
        }
      }
    }
    currentEvent(id: $id) {
      name
      location
      courseType
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
    playerPressure(id: $id)
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

/** Geometry of a hole in the pending event, for the 2D render: current hole by default, or an explicit hole. */
export const PlayingHoleDocument = graphql(`
  query PlayingHole($id: ID!, $hole: Int) {
    playingHole(id: $id, hole: $hole) {
      holeNumber
      par
      length
      fairwayHalfWidth
      greenHalfWidth
      greenDepth
      elevationDelta
      hasGreensideBunker
      hasWater
      hasTrees
      pinLateral
      pinDepth
      courseType
      layoutSeed
      geometry {
        tee { x y }
        cup { x y }
        playableBoundary { x y }
        regions { surface boundary { x y } }
      }
      ball { position { x y } lie }
    }
  }
`);

/** Plays the current shot with an individual club and literal canonical aim point. */
export const PlayShotDocument = graphql(`
  mutation PlayShot($id: ID!, $intent: BallStrikeIntentInput!) {
    playShot(id: $id, intent: $intent) {
      stale
      outcome {
        finalSurface
        carry
        lateral
        distanceRemaining
        hazardEntered
        penaltyStrokes
        strokes
        settlement {
          contact { position { x y } surface }
          recoveryPosition { x y }
          recoveryKind
          ball { position { x y } lie }
        }
      }
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
      settlement {
        contact { position { x y } surface }
        recoveryPosition { x y }
        recoveryKind
        ball { position { x y } lie }
      }
    }
  }
`);

/** Sims the rest of the current hole. */
export const SimHoleDocument = graphql(`
  mutation SimHole($id: ID!) {
    simHole(id: $id)
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
