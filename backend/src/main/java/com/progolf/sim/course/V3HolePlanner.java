package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Pure deterministic translator from a V2 brief to the constrained V3 semantic spatial plan. */
final class V3HolePlanner {
    private static final long PLAN_SALT = 0x5633524f555445L;

    private V3HolePlanner() {
    }

    static HoleSpatialPlan plan(HoleBrief brief, double length, double fairwayHalf, double greenHalf,
                                double greenDepth, long holeSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, PLAN_SALT));
        boolean dogleg = usesDogleg(brief.archetype(), brief.par(), rng);
        double approachY = length - Math.max(30.0, greenDepth * 0.75 + 12.0);
        double approachX = (rng.nextDouble() * 2.0 - 1.0) * 8.0;
        List<Position2d> anchors = new ArrayList<>();
        if (dogleg) {
            double direction = rng.nextDouble() < 0.5 ? -1.0 : 1.0;
            // Put the single V3 turn far enough down the hole that today's long-club decision can progress to
            // it rather than cutting an early interior corner on an otherwise ordinary par 4.
            double anchorY = Math.clamp(length * (0.58 + rng.nextDouble() * 0.08), 150.0, approachY - 95.0);
            double displacement = Math.min(24.0, Math.max(12.0, length * (0.025 + rng.nextDouble() * 0.015)));
            anchors.add(new Position2d(direction * displacement, anchorY));
            approachX = direction * displacement * (0.16 + rng.nextDouble() * 0.16);
        }
        HoleRoute route = new HoleRoute(new Position2d(0.0, 0.0), anchors, new Position2d(approachX, approachY));
        Position2d finalUnit = route.finalUnit();
        Position2d right = new Position2d(finalUnit.y(), -finalUnit.x());
        PreferredApproachSide preferred = preferredSide(brief.archetype(), rng);
        double side = preferred == PreferredApproachSide.RIGHT ? 1.0
                : preferred == PreferredApproachSide.LEFT ? -1.0 : 0.0;
        Position2d greenOffset = new Position2d(finalUnit.x() * (greenDepth * 0.48 + 9.0) + right.x() * side * 4.0,
                finalUnit.y() * (greenDepth * 0.48 + 9.0) + right.y() * side * 4.0);
        double approachAngle = StrictMath.atan2(finalUnit.y(), finalUnit.x());
        double rotation = switch (brief.archetype()) {
            case POSITIONAL -> approachAngle + side * 0.42;
            case BALANCED -> approachAngle + (rng.nextDouble() * 2.0 - 1.0) * 0.18;
            case RISK_REWARD -> approachAngle + side * 0.64;
        };
        GreenComplexPlan green = new GreenComplexPlan(greenOffset, Math.max(greenDepth / 2.0, greenHalf),
                Math.min(greenDepth / 2.0, greenHalf), rotation, finalUnit, GreenSide.FRONT,
                side >= 0.0 ? GreenSide.RIGHT : GreenSide.LEFT, side >= 0.0 ? GreenSide.LEFT : GreenSide.RIGHT,
                brief.archetype() != StrategicArchetype.POSITIONAL,
                EnumSet.of(GreenSurroundRole.OPEN_ENTRY, GreenSurroundRole.PROTECTED_SIDE,
                        GreenSurroundRole.BAILOUT_SIDE, GreenSurroundRole.SHORT_MISS, GreenSurroundRole.LONG_MISS,
                        GreenSurroundRole.RECOVERY_SIDE));
        return new HoleSpatialPlan(route, zones(brief, route, fairwayHalf, preferred), green);
    }

    private static boolean usesDogleg(StrategicArchetype archetype, int par, Rng rng) {
        if (par == 3) return false;
        return switch (archetype) {
            // One bend is a strategic accent, not a mandatory penalty on every archetyped hole. Preferred-side
            // zones and green orientation still express the archetype when a route stays straight.
            case POSITIONAL -> rng.nextDouble() < 0.02;
            case BALANCED -> rng.nextDouble() < 0.005;
            case RISK_REWARD -> rng.nextDouble() < 0.015;
        };
    }

    private static PreferredApproachSide preferredSide(StrategicArchetype archetype, Rng rng) {
        if (archetype == StrategicArchetype.BALANCED) return PreferredApproachSide.NEUTRAL;
        return rng.nextDouble() < 0.5 ? PreferredApproachSide.LEFT : PreferredApproachSide.RIGHT;
    }

    private static List<LandingZone> zones(HoleBrief brief, HoleRoute route, double fairwayHalf,
                                           PreferredApproachSide preferred) {
        double total = route.length();
        // The compatibility target must still let the current club-only policy reach a normal tee-shot landing
        // distance. It is semantic routing, not a compulsory short lay-up.
        double minimumZoneDistance = Math.max(25.0, Math.min(70.0, total - 70.0));
        double maximumZoneDistance = Math.max(minimumZoneDistance, total - 64.0);
        double primaryDistance = Math.clamp(270.0, minimumZoneDistance, maximumZoneDistance);
        double preferredOffset = preferred == PreferredApproachSide.RIGHT ? fairwayHalf * 0.28
                : preferred == PreferredApproachSide.LEFT ? -fairwayHalf * 0.28 : 0.0;
        List<LandingZone> zones = new ArrayList<>();
        zones.add(new LandingZone(LandingZoneRole.PRIMARY, primaryDistance, preferredOffset, 42.0,
                fairwayHalf * (brief.archetype() == StrategicArchetype.POSITIONAL ? 0.78 : 1.05), preferred,
                ReferenceCarryBand.STANDARD));
        if (brief.archetype() == StrategicArchetype.POSITIONAL) {
            zones.add(new LandingZone(LandingZoneRole.SAFE, Math.max(25.0, primaryDistance - 46.0), -preferredOffset * 0.45, 48.0,
                    fairwayHalf * 1.16, PreferredApproachSide.NEUTRAL, ReferenceCarryBand.SHORT));
        } else if (brief.archetype() == StrategicArchetype.RISK_REWARD && brief.par() >= 4) {
            zones.add(new LandingZone(LandingZoneRole.SAFE, Math.max(26.0, primaryDistance - 52.0), -preferredOffset * 0.25, 50.0,
                    fairwayHalf * 1.16, PreferredApproachSide.NEUTRAL, ReferenceCarryBand.SHORT));
            zones.add(new LandingZone(LandingZoneRole.AGGRESSIVE, Math.min(total - 42.0, primaryDistance + 48.0), preferredOffset * 1.15, 35.0,
                    fairwayHalf * 0.78, preferred, ReferenceCarryBand.LONG_REACHABLE));
        }
        return zones;
    }
}
