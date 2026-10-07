package com.progolf.sim.shot;

import java.util.List;

/** Advisory planning data; its points are ordinary literal aim points, not resolver commands. */
public record ShotGuidance(AimPoint safe, AimPoint primary, AimPoint aggressive, List<ClubReach> clubs) {
    public ShotGuidance { clubs = List.copyOf(clubs); }
    public record ClubReach(ClubId club, String label, double nominalCarry, double normalReach,
                            List<FamilyAvailability> families) {
        public ClubReach { families = List.copyOf(families); }
        public ClubReach(ClubId club, String label, double nominalCarry, double normalReach) {
            this(club, label, nominalCarry, normalReach, List.of());
        }
    }
    public record FamilyAvailability(ShotFamily family, boolean available, String reason) { }
}
