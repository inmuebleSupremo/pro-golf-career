package com.progolf.app.api;

import com.progolf.app.api.dto.AimPointInput;
import com.progolf.app.api.dto.PhysicalGreenDto;
import com.progolf.app.api.dto.PhysicalPuttIntentInput;
import com.progolf.app.api.dto.PhysicalPuttOutcomeDto;
import com.progolf.app.auth.AuthenticatedUser;
import com.progolf.app.world.WorldService;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.practice.PracticePuttingGreen;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PracticePuttingController {
    public record Point(double x, double y) { }
    public record Cup(int number, Point position) { }
    public record Ball(int number, Point position) { }
    public record State(String shotRevision, int puttsHit, Point startingPosition, Point readyPosition,
                        List<Ball> restingBalls, List<Point> footprint, List<Cup> cups,
                        PhysicalGreenDto physicalGreen) { }
    public record Putt(PhysicalPuttOutcomeDto outcome, Integer holedCup, int ballNumber, State state) { }

    private final WorldService worlds;

    public PracticePuttingController(WorldService worlds) { this.worlds = worlds; }

    @QueryMapping
    public State practicePuttingGreen(@Argument String id) {
        return state(worlds.practicePuttingGreen(AuthenticatedUser.requireId(), id).state());
    }

    @MutationMapping
    public State placePracticeBall(@Argument String id, @Argument AimPointInput position,
                                   @Argument String expectedShotRevision) {
        return state(worlds.placePracticeBall(AuthenticatedUser.requireId(), id,
                new Position2d(position.x(), position.y()), expectedShotRevision));
    }

    @MutationMapping
    public State collectPracticeBalls(@Argument String id, @Argument String expectedShotRevision) {
        return state(worlds.collectPracticeBalls(AuthenticatedUser.requireId(), id, expectedShotRevision));
    }

    @MutationMapping
    public Putt practicePhysicalPutt(@Argument String id, @Argument int targetCup,
                                     @Argument PhysicalPuttIntentInput intent) {
        var result = worlds.practicePhysicalPutt(AuthenticatedUser.requireId(), id, targetCup,
                ApiMapper.physicalPuttIntent(intent), intent.expectedShotRevision());
        return new Putt(ApiMapper.physicalPutt(result.outcome()), result.holedCup(), result.ballNumber(),
                state(result.state()));
    }

    private static State state(PracticePuttingGreen.State state) {
        return new State(state.shotRevision(), state.puttsHit(), point(state.startingPosition()),
                point(state.readyPosition()), state.restingBalls().stream()
                .map(ball -> new Ball(ball.number(), point(ball.position()))).toList(),
                PracticePuttingGreen.footprintView().stream().map(PracticePuttingController::point).toList(),
                PracticePuttingGreen.cups().stream()
                .map(cup -> new Cup(cup.number(), point(cup.position()))).toList(),
                ApiMapper.physicalGreen(PracticePuttingGreen.surfaceView()));
    }

    private static Point point(Position2d position) { return new Point(position.x(), position.y()); }
}
