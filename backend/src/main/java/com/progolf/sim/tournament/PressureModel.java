package com.progolf.sim.tournament;

/**
 * The situational-pressure model (spec: shot-resolution pressure): how much competitive pressure a golfer
 * feels for a given round, given the event's prestige and how near the lead they are. Pressure is a value
 * in [0,1] that the shot/putt models consume (where {@code COMPOSURE} resists it and a sports psychologist
 * relieves it) — so it is a pure, deterministic function of the competitive situation.
 *
 * <p>Pressure builds on the closing rounds (rounds 1-2 carry none; the third round is "moving day"; the
 * final round is "Sunday"), scales with event prestige (a major's Sunday is the most), and weighs only on
 * those still in contention — it decays linearly to zero as a competitor falls out of the hunt. The same
 * function is used for the auto-resolved field and the human player's interactive round, so a simmed event
 * stays byte-identical to automatic resolution.
 */
public final class PressureModel {

    private PressureModel() {
    }

    /**
     * Situational pressure in [0,1] for a competitor about to play {@code roundNo} of an event of the given
     * {@code prestige}, currently {@code strokesBehind} the leader (0 = leading or tied for the lead).
     */
    public static double forRound(int roundNo, EventPrestige prestige, int strokesBehind) {
        double roundWeight = switch (roundNo) {
            case 3 -> TournamentConstants.PRESSURE_ROUND_3_WEIGHT;
            case 4 -> TournamentConstants.PRESSURE_ROUND_4_WEIGHT;
            default -> 0.0; // opening rounds (and any beyond four) carry no closing pressure
        };
        if (roundWeight == 0.0) {
            return 0.0;
        }
        double prestigeWeight = switch (prestige) {
            case MAJOR -> TournamentConstants.PRESSURE_PRESTIGE_MAJOR;
            case TOUR_CHAMPIONSHIP -> TournamentConstants.PRESSURE_PRESTIGE_TOUR_CHAMPIONSHIP;
            case SIGNATURE -> TournamentConstants.PRESSURE_PRESTIGE_SIGNATURE;
            case REGULAR -> TournamentConstants.PRESSURE_PRESTIGE_REGULAR;
        };
        // Contention: 1 at the lead, decaying linearly to 0 by CONTENTION_STROKES back (never negative).
        double behind = Math.max(0, strokesBehind);
        double contention = Math.max(0.0, 1.0 - behind / (double) TournamentConstants.PRESSURE_CONTENTION_STROKES);
        double pressure = TournamentConstants.PRESSURE_BASE * roundWeight * prestigeWeight * contention;
        return Math.max(0.0, Math.min(1.0, pressure));
    }
}
