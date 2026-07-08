package com.progolf.sim.core;

/**
 * The address of a single random scope in the world seed hierarchy
 * (world -> season -> tournament -> round -> golfer -> hole -> shot) (spec: deterministic-rng).
 *
 * <p>A coordinate fully determines a derived seed; two identical coordinates always yield the same
 * seed, and any differing identifier yields an independent stream (sibling isolation).
 */
public record SeedCoordinate(
        long worldSeed,
        long seasonId,
        long tournamentId,
        long roundNo,
        long golferId,
        long holeNo,
        long shotNo) {

    /** A convenience root coordinate for a world with all sub-scopes at 0. */
    public static SeedCoordinate ofWorld(long worldSeed) {
        return new SeedCoordinate(worldSeed, 0, 0, 0, 0, 0, 0);
    }

    /** Returns a copy addressing hole {@code hole} (shot reset to 0). */
    public SeedCoordinate withHole(long hole) {
        return new SeedCoordinate(worldSeed, seasonId, tournamentId, roundNo, golferId, hole, 0);
    }

    /** Returns a copy addressing shot {@code shot} within the current hole. */
    public SeedCoordinate withShot(long shot) {
        return new SeedCoordinate(worldSeed, seasonId, tournamentId, roundNo, golferId, holeNo, shot);
    }
}
