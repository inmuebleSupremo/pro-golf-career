package com.progolf.sim.support;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.List;

/** Shared, deterministic test fixtures for the shot engine. */
public final class Fixtures {

    public static final long MASTER_SEED = 0xC0FFEE123456789AL;

    private Fixtures() {
    }

    /** A single-band reachable line [0,400) with fairway centre flanked by progressively worse rough. */
    public static ShotZoneProfile standardProfile() {
        ZoneBand band = new ZoneBand(0.0, 400.0, List.of(
                new LateralRegion(20.0, Surface.FAIRWAY),
                new LateralRegion(30.0, Surface.FIRST_CUT),
                new LateralRegion(45.0, Surface.PRIMARY_ROUGH),
                new LateralRegion(60.0, Surface.DEEP_ROUGH)));
        return new ShotZoneProfile(List.of(band));
    }

    /** Root coordinate for one golfer, ready to address individual shots via withShot(n). */
    public static SeedCoordinate coordinate() {
        return new SeedCoordinate(MASTER_SEED, 1, 1, 1, 42, 1, 0);
    }

    /** A neutral driver shot at a 250-yard pin with the given attributes/strategy at shot n. */
    public static ShotContext driverShot(Attributes attributes, Strategy strategy, int shotNo) {
        return new ShotContext(
                attributes,
                GolferState.fresh(),
                Environment.calm(),
                250.0,
                standardProfile(),
                ShotDecision.straight(Club.DRIVER, 250.0, strategy),
                coordinate().withShot(shotNo));
    }
}
