package com.progolf.sim.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.progolf.app.api.ApiMapper;
import com.progolf.app.api.dto.PlayingHoleDto;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.WindVector;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.putt.PuttingPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reproducible actual Generator 8 Parkland/Woodland holes for the focused visual comparison. */
public final class CourseFamilyPreviewFixture {
    private CourseFamilyPreviewFixture() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected output JSON path");
        SeedCoordinate coordinate = new SeedCoordinate(0x5041524b574f4f44L, 7, 103, 0, 0, 0, 0);
        List<PreviewCase> cases = List.of(
                preview(coordinate, EnvironmentClassification.PARKLAND),
                preview(coordinate, EnvironmentClassification.WOODLAND));
        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.toFile(), cases);
    }

    private static PreviewCase preview(SeedCoordinate coordinate, EnvironmentClassification classification) {
        Course course = CourseGenerator.generate(coordinate, classification,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        GeneratedHole hole = course.holes().stream().filter(candidate -> candidate.par() == 4).findFirst().orElseThrow();
        CourseGeometry geometry = hole.geometry();
        PinPlacement pin = hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
        PlayingHoleDto readModel = ApiMapper.playingHole(hole, pin.pin(), classification.name(),
                classification.name(), course.generatorVersion(),
                new BallState(geometry.tee(), Surface.TEE_BOX), geometry, pin.cup(),
                new WindVector(0.0, 0.0), PuttingPolicy.LEGACY, null, "PARKLAND");
        return new PreviewCase(classification.name(), readModel);
    }

    private record PreviewCase(String identity, PlayingHoleDto hole) { }
}
