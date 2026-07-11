package com.progolf.sim.tournament;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import java.util.Objects;

/**
 * The permanent, immutable result of a completed Tournament (REQ-096/099). Append-only history: the
 * finishing order, the winner, the cut result, and the prize distribution.
 */
public record TournamentResult(
        String tournamentName,
        List<Finish> finishingOrder,
        ProfessionalGolfer winner,
        CutResult cutResult) {

    public TournamentResult {
        Objects.requireNonNull(tournamentName, "tournamentName");
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(cutResult, "cutResult");
        finishingOrder = List.copyOf(finishingOrder);
    }

    /** A single competitor's finishing record, including their shot-level statistics for the event. */
    public record Finish(
            ProfessionalGolfer golfer,
            int position,
            int score,
            boolean madeCut,
            boolean withdrawn,
            double prize,
            com.progolf.sim.shot.ShotStatLine shotStats) {

        /** Convenience for a finish with no recorded shot statistics (empty line). */
        public Finish(ProfessionalGolfer golfer, int position, int score, boolean madeCut, boolean withdrawn,
                      double prize) {
            this(golfer, position, score, madeCut, withdrawn, prize, com.progolf.sim.shot.ShotStatLine.empty());
        }
    }
}
