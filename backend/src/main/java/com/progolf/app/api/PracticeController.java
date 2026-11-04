package com.progolf.app.api;

import com.progolf.app.api.dto.BallStrikeIntentInput;
import com.progolf.app.api.dto.ShotOutcomeDto;
import com.progolf.app.api.dto.ClubReachDto;
import com.progolf.app.auth.AuthenticatedUser;
import com.progolf.app.world.WorldService;
import com.progolf.sim.practice.PracticeRange;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PracticeController {
    public record Point(double x, double y) { }
    public record Target(String label, Point position) { }
    public record ExpectedReach(String club, String family, double yards) { }
    public record State(int ballsHit, String shotRevision, double aimHalfWidth, double fieldHalfWidth,
                        double targetGreenRadius,
                        List<Target> targets, List<ClubReachDto> clubs, List<ExpectedReach> expectedReaches) { }
    public record Shot(ShotOutcomeDto outcome, double totalDistance, double proximity, int ballNumber) { }

    private final WorldService worlds;

    public PracticeController(WorldService worlds) { this.worlds = worlds; }

    @QueryMapping
    public State practiceRange(@Argument String id) {
        PracticeRange range = worlds.practiceRange(AuthenticatedUser.requireId(), id);
        int ballsHit = range.ballsHit();
        return new State(ballsHit, Integer.toString(ballsHit), PracticeRange.aimHalfWidth(), PracticeRange.fieldHalfWidth(),
                PracticeRange.targetGreenRadius(),
                PracticeRange.targets().stream()
                .map(target -> new Target(target.label(), new Point(target.position().x(), target.position().y())))
                .toList(), ApiMapper.clubReaches(range.clubs()), range.expectedReaches().stream()
                .map(reach -> new ExpectedReach(reach.club().name(), reach.family().name(), reach.yards())).toList());
    }

    @MutationMapping
    public Shot practiceShot(@Argument String id, @Argument Integer bayNumber, @Argument BallStrikeIntentInput intent) {
        PracticeRange.Result result = worlds.practiceShot(AuthenticatedUser.requireId(), id,
                ApiMapper.ballStrikeIntent(intent), bayNumber == null ? 4 : bayNumber, intent.expectedShotRevision());
        return new Shot(ApiMapper.shotOutcome(result.outcome()), result.totalDistance(), result.proximity(),
                result.ballNumber());
    }
}
