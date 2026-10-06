package com.progolf.sim.course;

import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** One intentional V4 gameplay hazard with non-polygon strategic provenance. */
public record HazardFeature(Surface surface, HazardRole role, HazardAnchor anchor, HazardSide side,
                            HazardSeverity severity, HazardEnvelope envelope) {
    public HazardFeature {
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(envelope, "envelope");
        if (surface != Surface.BUNKER && surface != Surface.WATER && surface != Surface.TREES
                && surface != Surface.RECOVERY_AREA) {
            throw new IllegalArgumentException("V4 hazard feature needs an existing hazard/recovery surface");
        }
        validateRole(role, surface, anchor.type());
        if (anchor.type() == HazardAnchorType.GREEN_SIDE && (side == HazardSide.LEFT || side == HazardSide.RIGHT)
                && anchor.greenSide() != sideToGreenSide(side)) {
            throw new IllegalArgumentException("green-side anchor and relative side must agree");
        }
    }

    private static GreenSide sideToGreenSide(HazardSide side) {
        return side == HazardSide.LEFT ? GreenSide.LEFT : GreenSide.RIGHT;
    }

    private static void validateRole(HazardRole role, Surface surface, HazardAnchorType anchorType) {
        boolean valid = switch (role) {
            case LANDING_GUARD -> anchorType == HazardAnchorType.LANDING_ZONE
                    && (surface == Surface.BUNKER || surface == Surface.WATER);
            case TURN_GUARD -> anchorType == HazardAnchorType.DOGLEG_CORNER
                    && (surface == Surface.BUNKER || surface == Surface.TREES || surface == Surface.RECOVERY_AREA);
            case GREEN_GUARD -> anchorType == HazardAnchorType.GREEN_SIDE
                    && (surface == Surface.BUNKER || surface == Surface.WATER);
            case BAILOUT_BOUNDARY -> anchorType == HazardAnchorType.ROUTE_DISTANCE
                    && (surface == Surface.BUNKER || surface == Surface.TREES || surface == Surface.RECOVERY_AREA);
            case RECOVERY_BOUNDARY -> anchorType == HazardAnchorType.ROUTE_DISTANCE
                    && (surface == Surface.TREES || surface == Surface.RECOVERY_AREA);
        };
        if (!valid) throw new IllegalArgumentException("invalid V4 hazard role, surface, or anchor combination");
    }
}
