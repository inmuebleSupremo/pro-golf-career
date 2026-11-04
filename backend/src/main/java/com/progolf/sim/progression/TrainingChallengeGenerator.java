package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Handedness;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.practice.PracticePuttingGreen;
import com.progolf.sim.practice.PracticeRange;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.Club;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.shot.ClubSpec;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.ShotExecutionProfile;
import com.progolf.sim.shot.ShotFamily;
import com.progolf.sim.shot.ShotGuidance;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Deterministic eight-shot objectives on the existing range and putting green. */
public final class TrainingChallengeGenerator {
    private static final double[] REACH_FRACTIONS = {0.70, 0.76, 0.82, 0.88, 0.94, 0.98};
    private static final int PROBE_SAMPLES = 25;
    private static final double MAX_RESTING_BIAS_FRACTION = 0.2;
    private static final double MIN_MEAN_SCORE = 0.5;
    private TrainingChallengeGenerator() { }

    public static List<TrainingChallenge> generate(PreparationFocus focus, Attributes skills, long seed) {
        return generate(focus, skills, GolferState.fresh(), Handedness.RIGHT, seed);
    }

    public static List<TrainingChallenge> generate(PreparationFocus focus, Attributes skills,
                                                    GolferState state, Handedness handedness, long seed) {
        if (!focus.technical()) throw new IllegalArgumentException("playable training requires a technical focus");
        SplitMix64Rng rng = new SplitMix64Rng(Seeds.deriveSeed(seed, focus.ordinal()));
        return focus == PreparationFocus.PUTTING ? putts(skills, rng) : range(focus, skills, state, handedness, seed, rng);
    }

    /** Derived from persisted world coordinates; older fixed-flag challenges need no save migration. */
    public static boolean landingZone(TrainingChallenge challenge) {
        return !challenge.putting() && nearestFlag(challenge.target()) < 0;
    }

    public static String targetLabel(TrainingChallenge challenge) {
        if (challenge.putting()) return "Cup " + challenge.cupNumber();
        int flag = nearestFlag(challenge.target());
        if (flag >= 0) return PracticeRange.targets().get(flag).label() + " flag";
        String lane = challenge.target().x() < -3 ? "L" : challenge.target().x() > 3 ? "R" : "C";
        return "Zone " + lane + (lane.equals("C") ? "" : Math.round(Math.abs(challenge.target().x())))
                + "-" + Math.round(challenge.distance());
    }

    private static int nearestFlag(Position2d target) {
        for (int index = 0; index < PracticeRange.targets().size(); index++) {
            if (PracticeRange.targets().get(index).position().distanceTo(target) <= 2.5 + 1e-8) return index;
        }
        return -1;
    }

    /** Also checks stored challenges made by the earlier fixed-flag generator without rewriting them. */
    public static boolean compatible(PreparationFocus focus, TrainingChallenge challenge, Attributes skills,
                                      GolferState state) {
        if (focus == PreparationFocus.PUTTING) {
            if (!challenge.putting() || challenge.cupNumber() > PracticePuttingGreen.cups().size()) return false;
            Position2d cup = PracticePuttingGreen.cups().get(challenge.cupNumber() - 1).position();
            return challenge.target().equals(cup) && PracticePuttingGreen.surfaceView().contains(challenge.start())
                    && challenge.distance() >= 1.8 && challenge.distance() <= 6.5
                    && PracticePuttingGreen.surfaceView().gradientAt(challenge.start()).magnitude() <= 0.15;
        }
        if (challenge.putting() || !challenge.start().equals(PracticeRange.bay())) return false;
        int flag = nearestFlag(challenge.target());
        if (flag >= 0) {
            int first = focus == PreparationFocus.DRIVING ? 5 : focus == PreparationFocus.IRONS ? 3 : 0;
            int last = focus == PreparationFocus.DRIVING ? 6 : focus == PreparationFocus.IRONS ? 5 : 2;
            if (flag < first || flag > last
                    || PracticeRange.geometryView().surfaceAt(challenge.target()) != Surface.GREEN) return false;
        } else if (!validLandingZone(challenge.target(), challenge.distance())) return false;
        Club required = focus == PreparationFocus.DRIVING ? Club.DRIVER
                : focus == PreparationFocus.IRONS ? Club.IRON : Club.WEDGE;
        if (ClubSpec.of(challenge.suggestedClub()).family() != required) return false;
        return ShotGuidance.clubReaches(skills, Surface.TEE_BOX).stream()
                .filter(club -> club.club() == challenge.suggestedClub())
                .anyMatch(club -> reachable(club, challenge.suggestedFamily(), skills, state, challenge.distance()));
    }

    private static boolean validLandingZone(Position2d target, double distance) {
        if (target.x() < -PracticeRange.aimHalfWidth() || target.x() > PracticeRange.aimHalfWidth()
                || target.y() < 15 || target.y() > 320 || distance > 320
                || PracticeRange.geometryView().surfaceAt(target) != Surface.FAIRWAY
                || Math.abs(target.x()) > Math.max(8, distance * 0.15)) return false;
        return PracticeRange.targets().stream().allMatch(flag ->
                flag.position().distanceTo(target) > PracticeRange.targetGreenRadius() + 4);
    }

    private static boolean reachable(ShotGuidance.ClubReach club, ShotFamily family, Attributes skills,
                                     GolferState state, double distance) {
        ShotExecutionProfile profile = ShotExecutionProfile.derive(Surface.TEE_BOX,
                ClubSpec.of(club.club()), family, skills, Environment.calm());
        if (!profile.eligibility().allowed()) return false;
        double effectiveReach = club.normalReach() * (1.0 + state.equipmentPower())
                * profile.effectiveCarryCapMultiplier();
        return distance >= effectiveReach * 0.68 && distance <= effectiveReach * 0.99;
    }

    private record Candidate(TrainingChallenge challenge, int lane, double tieBreak) { }

    private static List<TrainingChallenge> range(PreparationFocus focus, Attributes skills, GolferState state,
                                                 Handedness handedness, long seed, SplitMix64Rng rng) {
        List<Candidate> options = new ArrayList<>();
        List<ShotGuidance.ClubReach> clubs = ShotGuidance.clubReaches(skills, Surface.TEE_BOX).stream()
                .sorted(Comparator.comparingInt(club -> club.club().ordinal())).toList();
        Club required = focus == PreparationFocus.DRIVING ? Club.DRIVER
                : focus == PreparationFocus.IRONS ? Club.IRON : Club.WEDGE;
        double tolerance = focus == PreparationFocus.DRIVING ? 42 : focus == PreparationFocus.IRONS ? 21 : 9;
        for (var club : clubs) {
            if (ClubSpec.of(club.club()).family() != required) continue;
            List<ShotFamily> families = focus == PreparationFocus.DRIVING ? List.of(ShotFamily.FULL)
                    : focus == PreparationFocus.IRONS ? List.of(ShotFamily.FULL, ShotFamily.CONTROLLED)
                    : List.of(ShotFamily.FULL, ShotFamily.CONTROLLED, ShotFamily.PITCH);
            for (ShotFamily family : families) {
                ShotExecutionProfile profile = ShotExecutionProfile.derive(Surface.TEE_BOX,
                        ClubSpec.of(club.club()), family, skills, Environment.calm());
                if (!profile.eligibility().allowed()) continue;
                double reach = club.normalReach() * (1.0 + state.equipmentPower())
                        * profile.effectiveCarryCapMultiplier();
                for (int step = 0; step < REACH_FRACTIONS.length; step++) {
                    double distance = Math.round(reach * REACH_FRACTIONS[step]);
                    int laneCount = focus == PreparationFocus.DRIVING ? 2 : 1;
                    for (int lane = -laneCount; lane <= laneCount; lane++) {
                        double x = lane * Math.max(7, Math.min(24, distance * 0.12)) / laneCount;
                        Position2d target = new Position2d(x, StrictMath.sqrt(distance * distance - x * x));
                        TrainingChallenge challenge = new TrainingChallenge(1, PracticeRange.bay(), target, 0,
                                club.club(), family, tolerance, 0);
                        if (compatible(focus, challenge, skills, state))
                            options.add(new Candidate(challenge, lane(target), rng.nextDouble()));
                    }
                }
            }
        }
        if (options.isEmpty()) throw new IllegalStateException("no achievable " + focus.name().toLowerCase() + " training challenge");
        List<TrainingChallenge> chosen = new ArrayList<>(8);
        Set<Candidate> rejected = new HashSet<>();
        for (int number = 1; number <= 8; number++) {
            Candidate best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            // Choose a full sequence with new clubs, techniques, distances and directions wherever possible.
            while (true) {
                best = null;
                bestScore = Double.NEGATIVE_INFINITY;
                for (Candidate candidate : options) {
                    if (rejected.contains(candidate) || !distinct(focus, chosen, candidate.challenge())) continue;
                    double score = varietyScore(chosen, candidate) + candidate.tieBreak();
                    if (score > bestScore) { bestScore = score; best = candidate; }
                }
                if (best == null) break;
                if (assessRangeAim(best.challenge(), skills, state, handedness, seed).isPresent()) break;
                rejected.add(best);
            }
            if (best == null) break;
            TrainingChallenge c = best.challenge();
            chosen.add(new TrainingChallenge(number, c.start(), c.target(), 0,
                    c.suggestedClub(), c.suggestedFamily(), c.tolerance(), 0));
            rejected.add(best);
        }
        if (!varied(focus, chosen, options))
            throw new IllegalStateException("no achievable varied " + focus.name().toLowerCase() + " training session");
        return List.copyOf(chosen);
    }

    private static boolean distinct(PreparationFocus focus, List<TrainingChallenge> chosen, TrainingChallenge next) {
        double spacing = focus == PreparationFocus.DRIVING ? 8 : focus == PreparationFocus.IRONS ? 8 : 5;
        return chosen.stream().allMatch(previous -> previous.target().distanceTo(next.target()) >= spacing);
    }

    private static double varietyScore(List<TrainingChallenge> chosen, Candidate candidate) {
        TrainingChallenge next = candidate.challenge();
        double score = chosen.stream().noneMatch(c -> c.suggestedClub() == next.suggestedClub()) ? 80 : 0;
        score += chosen.stream().noneMatch(c -> c.suggestedFamily() == next.suggestedFamily()) ? 40 : 0;
        score += chosen.stream().noneMatch(c -> Math.abs(c.distance() - next.distance()) < 12) ? 45 : 0;
        score += chosen.stream().noneMatch(c -> lane(c.target()) == candidate.lane()) ? 35 : 0;
        score += chosen.isEmpty() ? 0 : Math.min(40, chosen.stream()
                .mapToDouble(c -> c.target().distanceTo(next.target())).min().orElse(0) / 2);
        return score;
    }

    private static int lane(Position2d target) { return target.x() < -3 ? -1 : target.x() > 3 ? 1 : 0; }

    private static boolean varied(PreparationFocus focus, List<TrainingChallenge> chosen, List<Candidate> options) {
        if (chosen.size() != 8) return false;
        double band = focus == PreparationFocus.DRIVING ? 14 : focus == PreparationFocus.IRONS ? 10 : 6;
        long distances = chosen.stream().map(c -> Math.round(c.distance() / band)).distinct().count();
        long lanes = chosen.stream().map(c -> lane(c.target())).distinct().count();
        long clubs = chosen.stream().map(TrainingChallenge::suggestedClub).distinct().count();
        long possibleClubs = options.stream().map(c -> c.challenge().suggestedClub()).distinct().count();
        return distances >= 3 && lanes >= 2 && (possibleClubs < 2 || clubs >= 2)
                && chosen.stream().allMatch(c -> distinct(focus, chosen.stream().filter(other -> other.number() < c.number()).toList(), c));
    }

    /** Resolver-tested click distance; it is validation data, never a commanded or locked player aim. */
    public record FeasibleAim(double intendedCarry, double medianActualCarry, double medianRestingDistance,
                              double medianProximity, double meanScore) { }

    public static Optional<FeasibleAim> assessRangeAim(TrainingChallenge challenge, Attributes skills,
                                                        GolferState state, Handedness handedness, long seed) {
        if (challenge.putting()) return Optional.empty();
        long coordinate = Double.doubleToLongBits(challenge.target().x())
                ^ Long.rotateLeft(Double.doubleToLongBits(challenge.target().y()), 17)
                ^ challenge.suggestedClub().ordinal() * 31L ^ challenge.suggestedFamily().ordinal();
        FeasibleAim direct = sampleAim(challenge, skills, state, handedness, seed, coordinate,
                challenge.distance(), 0);
        double normalReach = ShotGuidance.clubReaches(skills, Surface.TEE_BOX).stream()
                .filter(club -> club.club() == challenge.suggestedClub()).findFirst().orElseThrow().normalReach();
        double carryCap = normalReach * (1.0 + state.equipmentPower())
                * ShotExecutionProfile.derive(Surface.TEE_BOX, ClubSpec.of(challenge.suggestedClub()),
                        challenge.suggestedFamily(), skills, Environment.calm()).effectiveCarryCapMultiplier();
        double intended = Math.round((2 * challenge.distance() - direct.medianRestingDistance()) * 2) / 2.0;
        for (int iteration = 0; iteration < 2; iteration++) {
            double factor = intended / challenge.distance();
            Position2d aim = new Position2d(challenge.target().x() * factor, challenge.target().y() * factor);
            if (intended < 15 || intended > Math.min(320, carryCap) || aim.y() < 15 || aim.y() > 320
                    || Math.abs(aim.x()) > PracticeRange.aimHalfWidth()) return Optional.empty();
            // A disjoint seed set validates the calibration; a second correction gets another independent set.
            FeasibleAim assessment = sampleAim(challenge, skills, state, handedness, seed, coordinate,
                    intended, (iteration + 1) * 100);
            if (Math.abs(assessment.medianRestingDistance() - challenge.distance())
                    <= Math.max(2.0, challenge.tolerance() * MAX_RESTING_BIAS_FRACTION)
                    && assessment.medianProximity() <= challenge.tolerance()
                    && assessment.meanScore() >= MIN_MEAN_SCORE) return Optional.of(assessment);
            intended = Math.round((intended + challenge.distance() - assessment.medianRestingDistance()) * 2) / 2.0;
        }
        return Optional.empty();
    }

    private static FeasibleAim sampleAim(TrainingChallenge challenge, Attributes skills, GolferState state,
                                          Handedness handedness, long seed, long coordinate, double intended,
                                          int sampleOffset) {
        double factor = intended / challenge.distance();
        AimPoint aim = new AimPoint(challenge.target().x() * factor, challenge.target().y() * factor);
        List<Double> carries = new ArrayList<>(PROBE_SAMPLES);
        List<Double> rests = new ArrayList<>(PROBE_SAMPLES);
        List<Double> proximities = new ArrayList<>(PROBE_SAMPLES);
        double score = 0;
        for (int sample = 0; sample < PROBE_SAMPLES; sample++) {
            long probeSeed = Seeds.deriveSeed(seed ^ 0x50524f42455f5348L,
                    coordinate + sampleOffset + sample);
            PracticeRange range = new PracticeRange(skills, state, handedness, probeSeed);
            var shot = range.hit(new BallStrikeIntent(challenge.suggestedClub(), aim,
                    challenge.suggestedFamily()), 4, "0");
            carries.add(shot.outcome().carry());
            rests.add(shot.totalDistance());
            proximities.add(challenge.target().distanceTo(shot.outcome().trace().finalPoint()));
            score += TrainingScoring.range(challenge, shot);
        }
        return new FeasibleAim(intended, median(carries), median(rests), median(proximities),
                score / PROBE_SAMPLES);
    }

    private static double median(List<Double> values) {
        return values.stream().sorted().skip(values.size() / 2).findFirst().orElseThrow();
    }

    private static List<TrainingChallenge> putts(Attributes skills, SplitMix64Rng rng) {
        double maximum = skills.get(Attribute.PUTTING_PROXIMITY) >= 65 ? 6.5 : 5.0;
        int cupOffset = (int) Long.remainderUnsigned(rng.nextLong(), PracticePuttingGreen.cups().size());
        List<Integer> distances = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7));
        for (int index = distances.size() - 1; index > 0; index--) {
            int other = (int) Long.remainderUnsigned(rng.nextLong(), index + 1);
            int value = distances.get(index);
            distances.set(index, distances.get(other));
            distances.set(other, value);
        }
        List<TrainingChallenge> challenges = new ArrayList<>(8);
        for (int number = 1; number <= 8; number++) {
            int cupIndex = (cupOffset + number - 1) % PracticePuttingGreen.cups().size();
            Position2d cup = PracticePuttingGreen.cups().get(cupIndex).position();
            double radius = Math.max(1.8 + 1e-8, Math.min(maximum - 1e-8,
                    1.8 + distances.get(number - 1) * (maximum - 1.8) / 7));
            boolean placed = false;
            for (int attempt = 0; attempt < 128; attempt++) {
                double angle = rng.nextDouble() * 2 * StrictMath.PI;
                Position2d start = cup.plus(radius * StrictMath.cos(angle), radius * StrictMath.sin(angle));
                if (PracticePuttingGreen.surfaceView().contains(start)
                        && PracticePuttingGreen.surfaceView().gradientAt(start).magnitude() <= 0.15
                        && PracticePuttingGreen.cups().stream().allMatch(other -> other.position().distanceTo(start) >= 0.5)
                        && challenges.stream().allMatch(other -> other.start().distanceTo(start) >= 0.8)) {
                    challenges.add(new TrainingChallenge(number, start, cup, cupIndex + 1, null, null,
                            Math.max(0.65, radius * 0.28),
                            PracticePuttingGreen.surfaceView().gradientAt(start).magnitude()));
                    placed = true;
                    break;
                }
            }
            if (!placed) throw new IllegalStateException("could not place a varied practice putt");
        }
        if (challenges.stream().map(TrainingChallenge::cupNumber).distinct().count() < 4)
            throw new IllegalStateException("could not generate varied putting cups");
        return List.copyOf(challenges);
    }
}
