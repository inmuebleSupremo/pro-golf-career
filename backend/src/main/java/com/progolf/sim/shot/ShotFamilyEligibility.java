package com.progolf.sim.shot;

import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** Pure authoritative validation of a non-putting technique at its starting lie. */
public final class ShotFamilyEligibility {
    private ShotFamilyEligibility() { }

    public record Result(boolean allowed, String reason) {
        public Result {
            if (allowed && reason != null) throw new IllegalArgumentException("allowed result has no reason");
            if (!allowed && (reason == null || reason.isBlank())) throw new IllegalArgumentException("rejection reason required");
        }
        static Result permitted() { return new Result(true, null); }
        static Result rejected(String reason) { return new Result(false, reason); }
    }

    public static Result evaluate(Surface lie, ClubSpec club, ShotFamily family) {
        Objects.requireNonNull(lie, "lie");
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(family, "family");
        if (!lie.isPlayable()) return Result.rejected("Ball is not on a playable lie");
        if (lie == Surface.GREEN) return Result.rejected("Use the putting path on the green");
        if (club.family() == Club.PUTTER) return Result.rejected("Putter uses the dedicated putting path");
        if (lie == Surface.BUNKER) {
            if (family != ShotFamily.BUNKER) return Result.rejected("Bunker shots require BUNKER technique");
            return wedge(club) ? Result.permitted() : Result.rejected("BUNKER requires a wedge");
        }
        if (family == ShotFamily.BUNKER) return Result.rejected("BUNKER technique requires a bunker lie");
        if ((lie == Surface.TREES || lie == Surface.RECOVERY_AREA) && longClub(club)) {
            return Result.rejected("Long clubs are unsuitable from this recovery lie");
        }
        return switch (family) {
            case FULL, CONTROLLED -> Result.permitted();
            case PITCH -> wedge(club) ? Result.permitted() : Result.rejected("PITCH requires a pitching, gap, or sand wedge");
            case CHIP -> (club.family() == Club.IRON || wedge(club)) ? Result.permitted()
                    : Result.rejected("CHIP requires an iron or wedge");
            case BUNKER -> throw new IllegalStateException("bunker family handled above");
        };
    }

    private static boolean wedge(ClubSpec club) { return club.family() == Club.WEDGE; }
    private static boolean longClub(ClubSpec club) {
        return club.family() == Club.DRIVER || club.family() == Club.FAIRWAY_WOOD || club.family() == Club.HYBRID;
    }
}
