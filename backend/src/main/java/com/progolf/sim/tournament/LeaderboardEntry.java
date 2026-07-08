package com.progolf.sim.tournament;

import com.progolf.sim.player.ProfessionalGolfer;

/**
 * One row of the live leaderboard (REQ-094): a competitor, their shared position (ties share a
 * position), cumulative relative-to-par score, and how many rounds they have completed.
 */
public record LeaderboardEntry(int position, ProfessionalGolfer golfer, int score, int roundsPlayed) {
}
