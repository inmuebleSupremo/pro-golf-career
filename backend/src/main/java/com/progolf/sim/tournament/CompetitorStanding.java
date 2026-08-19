package com.progolf.sim.tournament;

import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.shot.HoleStats;
import com.progolf.sim.shot.ShotStatLine;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable per-competitor state during a Tournament: the entry, its round scores (relative to par), and
 * cut/withdrawal flags. Package-private — only the {@link Tournament} engine mutates it.
 */
final class CompetitorStanding {

    private final ProfessionalGolfer golfer;
    private final int fieldIndex;
    private final List<Integer> roundScores = new ArrayList<>();
    private ShotStatLine shotStats = ShotStatLine.empty();
    private boolean madeCut;
    private boolean withdrawn;

    CompetitorStanding(ProfessionalGolfer golfer, int fieldIndex) {
        this.golfer = golfer;
        this.fieldIndex = fieldIndex;
    }

    ProfessionalGolfer golfer() {
        return golfer;
    }

    int fieldIndex() {
        return fieldIndex;
    }

    void addRoundScore(int relativeToPar) {
        roundScores.add(relativeToPar);
    }

    /** Folds one hole's shot stats into this competitor's running line (auto-resolution path). */
    void addHole(HoleStats hole) {
        shotStats = shotStats.plus(hole);
    }

    /** Folds a whole round's shot stats (submitted by the interactive player) into the running line. */
    void addRoundStats(ShotStatLine round) {
        shotStats = shotStats.plus(round);
    }

    ShotStatLine shotStats() {
        return shotStats;
    }

    int cumulative() {
        int sum = 0;
        for (int s : roundScores) {
            sum += s;
        }
        return sum;
    }

    int roundsPlayed() {
        return roundScores.size();
    }

    /** This competitor's per-round scores relative to par, in order (an immutable copy). */
    List<Integer> roundScores() {
        return List.copyOf(roundScores);
    }

    boolean hasMadeCut() {
        return madeCut;
    }

    void setMadeCut(boolean madeCut) {
        this.madeCut = madeCut;
    }

    boolean isWithdrawn() {
        return withdrawn;
    }

    void withdraw() {
        this.withdrawn = true;
    }
}
