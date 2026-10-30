package com.progolf.sim.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.progolf.app.api.ApiMapper;
import com.progolf.app.api.dto.PlayingHoleDto;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.WindVector;
import com.progolf.sim.spatial.Surface;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Regenerates the full development-only visual sample from actual version 9 Links geometry. */
public final class CoastalLinksPreviewFixture {
    private CoastalLinksPreviewFixture() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected output JSON path");
        SeedCoordinate coordinate = new SeedCoordinate(103L, 0, 0, 0, 0, 0, 0);
        Course course = CourseGenerator.generate(coordinate, EnvironmentClassification.LINKS,
                CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION);
        List<PreviewHole> holes = course.holes().stream().map(hole -> {
            CourseGeometry geometry = hole.geometry();
            PinPlacement pin = hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
            PlayingHoleDto dto = ApiMapper.playingHole(hole, pin.pin(), "LINKS", "LINKS",
                    course.generatorVersion(), new BallState(geometry.tee(), Surface.TEE_BOX),
                    geometry, pin.cup(), new WindVector(0.0, 0.0), PuttingPolicy.LEGACY, null, "LINKS");
            boolean coastal = geometry.regions().stream().anyMatch(region -> region.surface() == Surface.WATER
                    && region.boundary().stream().mapToDouble(Position2d::x).max().orElseThrow()
                    - region.boundary().stream().mapToDouble(Position2d::x).min().orElseThrow() > 600);
            return new PreviewHole(dto, coastal, hole.architecturePlan().routingForm().name());
        }).toList();
        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        new ObjectMapper().writeValue(output.toFile(), new PreviewCourse(103, holes));
    }

    private record PreviewCourse(int seed, List<PreviewHole> holes) { }
    private record PreviewHole(PlayingHoleDto hole, boolean coastal, String routing) { }
}
