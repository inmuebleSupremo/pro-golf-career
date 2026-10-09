package com.progolf.sim.shot;

import com.progolf.sim.course.LandingZoneRole;
import java.util.List;

/** Advisory planning data; its points are ordinary literal aim points, not resolver commands. */
public record ShotGuidance(AimPoint safe, AimPoint primary, AimPoint aggressive, List<ClubReach> clubs,
                           List<StrategicOption> strategicOptions) {
    public ShotGuidance {
        clubs = List.copyOf(clubs);
        strategicOptions = List.copyOf(strategicOptions);
    }
    public ShotGuidance(AimPoint safe, AimPoint primary, AimPoint aggressive, List<ClubReach> clubs) {
        this(safe, primary, aggressive, clubs, List.of());
    }
    public record ClubReach(ClubId club, String label, double nominalCarry, double normalReach,
                            List<FamilyAvailability> families) {
        public ClubReach { families = List.copyOf(families); }
        public ClubReach(ClubId club, String label, double nominalCarry, double normalReach) {
            this(club, label, nominalCarry, normalReach, List.of());
        }
    }
    public record FamilyAvailability(ShotFamily family, boolean available, String reason,
                                     List<ShapeAvailability> shapes) {
        public FamilyAvailability { shapes = shapes == null ? List.of() : List.copyOf(shapes); }
        public FamilyAvailability(ShotFamily family, boolean available, String reason) { this(family, available, reason, List.of()); }
    }
    public record ShapeAvailability(ShotShape shape, boolean available, String reason) { }

    /** Backend-owned, non-binding V4 landing guidance. It describes current facts, not a predicted result. */
    public record StrategicOption(LandingZoneRole role, AimPoint aimPoint, ClubId suggestedClub,
                                  ShotFamily suggestedFamily, String routeSummary, String exposureSummary) {
        public StrategicOption {
            if (role == null || aimPoint == null || suggestedClub == null || suggestedFamily == null
                    || routeSummary == null || routeSummary.isBlank() || exposureSummary == null || exposureSummary.isBlank()) {
                throw new IllegalArgumentException("strategic option fields are required");
            }
        }
    }
}
