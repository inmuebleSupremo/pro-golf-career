package com.progolf.sim.progression;

import com.progolf.sim.course.Position2d;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.ShotFamily;

/** A fixed, backend-generated objective in the existing range or putting green. Distances are yards. */
public record TrainingChallenge(int number, Position2d start, Position2d target, int cupNumber,
                                ClubId suggestedClub, ShotFamily suggestedFamily, double tolerance, double slope,
                                Integer courseIndex, Integer holeNumber) {
    public TrainingChallenge {
        if (number < 1 || start == null || target == null || !Double.isFinite(tolerance) || tolerance <= 0
                || !Double.isFinite(slope) || slope < 0)
            throw new IllegalArgumentException("invalid training challenge");
        if (cupNumber < 0 || (cupNumber > 0) != (suggestedClub == null))
            throw new IllegalArgumentException("challenge must use one practice environment");
        if ((cupNumber == 0) != (suggestedFamily != null))
            throw new IllegalArgumentException("range challenge requires a shot family; putts cannot have one");
        if ((courseIndex == null) != (holeNumber == null)
                || courseIndex != null && (courseIndex < 0 || holeNumber < 1 || holeNumber > 18 || cupNumber != 0))
            throw new IllegalArgumentException("greenside challenge requires a valid course and hole");
    }

    /** Historical challenges have no course locator and retain their existing range/putting route. */
    public TrainingChallenge(int number, Position2d start, Position2d target, int cupNumber,
                             ClubId suggestedClub, ShotFamily suggestedFamily, double tolerance, double slope) {
        this(number, start, target, cupNumber, suggestedClub, suggestedFamily, tolerance, slope, null, null);
    }

    public boolean putting() { return cupNumber > 0; }
    public boolean greenside() { return courseIndex != null; }
    public double distance() { return start.distanceTo(target); }
}
