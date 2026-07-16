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
        long tournamentId,
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

    /**
     * A by-id capture of a result (spec: world-snapshot). This is the only captured type that holds live
     * golfer references (the winner and each finisher), so it is snapshotted by golfer id and re-linked to a
     * rebuilt registry on restore. It lives here (not in the world package) so the world never imports the
     * shot engine even though a finish carries a shot-stat line.
     */
    public record Snapshot(String tournamentName, long tournamentId, List<FinishSnapshot> finishes, String winnerId,
                           CutResult cutResult) {

        public record FinishSnapshot(String golferId, int position, int score, boolean madeCut, boolean withdrawn,
                                     double prize, com.progolf.sim.shot.ShotStatLine shotStats) {
        }

        public static Snapshot capture(TournamentResult result) {
            List<FinishSnapshot> finishes = result.finishingOrder().stream()
                    .map(f -> new FinishSnapshot(f.golfer().player().id(), f.position(), f.score(), f.madeCut(),
                            f.withdrawn(), f.prize(), f.shotStats()))
                    .toList();
            return new Snapshot(result.tournamentName(), result.tournamentId(), finishes,
                    result.winner().player().id(), result.cutResult());
        }

        public TournamentResult restore(java.util.Map<String, ProfessionalGolfer> registry) {
            List<Finish> rebuilt = finishes.stream()
                    .map(f -> new Finish(require(registry, f.golferId()), f.position(), f.score(), f.madeCut(),
                            f.withdrawn(), f.prize(), f.shotStats()))
                    .toList();
            return new TournamentResult(tournamentName, tournamentId, rebuilt, require(registry, winnerId), cutResult);
        }

        private static ProfessionalGolfer require(java.util.Map<String, ProfessionalGolfer> registry, String id) {
            ProfessionalGolfer g = registry.get(id);
            if (g == null) {
                throw new IllegalStateException("Snapshot references unknown golfer id: " + id);
            }
            return g;
        }
    }
}
