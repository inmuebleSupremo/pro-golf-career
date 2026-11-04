package com.progolf.app.api;

import com.progolf.app.api.dto.BallStrikeIntentInput;
import com.progolf.app.api.dto.DevelopmentDeltaDto;
import com.progolf.app.api.dto.PhysicalPuttIntentInput;
import com.progolf.app.api.dto.PhysicalPuttOutcomeDto;
import com.progolf.app.api.dto.ShotOutcomeDto;
import com.progolf.app.auth.AuthenticatedUser;
import com.progolf.app.world.WorldService;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.progression.PlayableTrainingSession;
import com.progolf.sim.progression.PreparationFocus;
import com.progolf.sim.progression.TrainingAttempt;
import com.progolf.sim.progression.TrainingChallenge;
import com.progolf.sim.world.World;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Authenticated playable-training API. Shot outcomes are resolved inside the simulation, never supplied by a client. */
@Controller
public class CareerTrainingController {
    public record Point(double x, double y) { }
    public record Challenge(int number, boolean putting, Point start, Point target, int cupNumber,
                            String suggestedClub, String suggestedFamily, double distance,
                            double tolerance, double slope, String targetLabel, boolean landingZone) { }
    public record Attempt(int number, double score, double proximity, double totalDistance,
                          boolean holedTarget, Point finalPosition) { }
    public record RatingChange(String attribute, int before, int after) { }
    public record Session(String id, String focus, int season, int week, String status, int nextChallenge,
                          List<Challenge> challenges, List<Attempt> attempts, double score, double baseProgress,
                          double bonusProgress, boolean bonusSettled, List<DevelopmentDeltaDto> attributeImprovements,
                          Double fatigueBefore, Double fatigueAfter, List<RatingChange> ratingChanges) { }
    public record Shot(Session session, Attempt attempt, ShotOutcomeDto rangeOutcome,
                       PhysicalPuttOutcomeDto puttOutcome) { }

    private final WorldService worlds;

    public CareerTrainingController(WorldService worlds) { this.worlds = worlds; }

    @QueryMapping
    public Session playerTrainingSession(@Argument String id) {
        return session(worlds.playerTrainingSession(AuthenticatedUser.requireId(), id));
    }

    @QueryMapping
    public boolean playerTrainingChallengePlayable(@Argument String id) {
        return worlds.playerTrainingChallengePlayable(AuthenticatedUser.requireId(), id);
    }

    @MutationMapping
    public Session startPlayerTraining(@Argument String id, @Argument String focus) {
        return session(worlds.startPlayerTraining(AuthenticatedUser.requireId(), id, PreparationFocus.valueOf(focus)));
    }

    @MutationMapping
    public Session abandonPlayerTraining(@Argument String id, @Argument String trainingId) {
        return session(worlds.abandonPlayerTraining(AuthenticatedUser.requireId(), id, trainingId));
    }

    @MutationMapping
    public Shot playerTrainingRangeShot(@Argument String id, @Argument String trainingId,
                                        @Argument int challengeNumber, @Argument BallStrikeIntentInput intent) {
        checkRevision(challengeNumber, intent.expectedShotRevision());
        return shot(worlds.playerTrainingRangeShot(AuthenticatedUser.requireId(), id, trainingId,
                challengeNumber, ApiMapper.ballStrikeIntent(intent)));
    }

    @MutationMapping
    public Shot playerTrainingPutt(@Argument String id, @Argument String trainingId,
                                   @Argument int challengeNumber, @Argument PhysicalPuttIntentInput intent) {
        checkRevision(challengeNumber, intent.expectedShotRevision());
        return shot(worlds.playerTrainingPutt(AuthenticatedUser.requireId(), id, trainingId,
                challengeNumber, ApiMapper.physicalPuttIntent(intent)));
    }

    private static void checkRevision(int challenge, String revision) {
        if (!Integer.toString(challenge - 1).equals(revision))
            throw new IllegalArgumentException("stale training shot revision");
    }

    private static Session session(PlayableTrainingSession value) {
        if (value == null) return null;
        return new Session(value.id(), value.focus().name(), value.season(), value.week(), value.status().name(),
                value.nextChallenge(), value.challenges().stream().map(CareerTrainingController::challenge).toList(),
                value.attempts().stream().map(CareerTrainingController::attempt).toList(), value.score(),
                value.baseProgress(), value.bonusProgress(), value.bonusSettled(),
                value.attributeImprovements().stream().map(change -> new DevelopmentDeltaDto(
                        change.attribute().name(), change.delta(), change.season())).toList(),
                value.fatigueBefore(), value.fatigueAfter(), value.ratingChanges().stream()
                        .map(change -> new RatingChange(change.attribute().name(), change.before(), change.after()))
                        .toList());
    }

    private static Challenge challenge(TrainingChallenge value) {
        return new Challenge(value.number(), value.putting(), point(value.start()), point(value.target()),
                value.cupNumber(), value.suggestedClub() == null ? null : value.suggestedClub().name(),
                value.suggestedFamily() == null ? null : value.suggestedFamily().name(),
                value.distance(), value.tolerance(), value.slope(),
                com.progolf.sim.progression.TrainingChallengeGenerator.targetLabel(value),
                com.progolf.sim.progression.TrainingChallengeGenerator.landingZone(value));
    }

    private static Attempt attempt(TrainingAttempt value) {
        return new Attempt(value.number(), value.score(), value.proximity(), value.totalDistance(),
                value.holedTarget(), point(value.finalPosition()));
    }

    private static Shot shot(World.TrainingShotResult value) {
        return new Shot(session(value.session()), attempt(value.attempt()),
                value.rangeShot() == null ? null : ApiMapper.shotOutcome(value.rangeShot().outcome()),
                value.putt() == null ? null : ApiMapper.physicalPutt(value.putt().outcome()));
    }

    private static Point point(Position2d value) { return new Point(value.x(), value.y()); }
}
