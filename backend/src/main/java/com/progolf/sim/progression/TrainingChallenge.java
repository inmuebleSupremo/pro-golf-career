package com.progolf.sim.progression;

import com.progolf.sim.course.Position2d;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.ShotFamily;

/** A fixed, backend-generated objective in the existing range or putting green. Distances are yards. */
public record TrainingChallenge(int number, Position2d start, Position2d target, int cupNumber,
                                ClubId suggestedClub, ShotFamily suggestedFamily, double tolerance, double slope) {
    public TrainingChallenge {
        if (number < 1 || start == null || target == null || !Double.isFinite(tolerance) || tolerance <= 0
                || !Double.isFinite(slope) || slope < 0)
            throw new IllegalArgumentException("invalid training challenge");
        if (cupNumber < 0 || (cupNumber > 0) != (suggestedClub == null))
            throw new IllegalArgumentException("challenge must use one practice environment");
        if ((cupNumber == 0) != (suggestedFamily != null))
            throw new IllegalArgumentException("range challenge requires a shot family; putts cannot have one");
    }

    public boolean putting() { return cupNumber > 0; }
    public double distance() { return start.distanceTo(target); }
}
