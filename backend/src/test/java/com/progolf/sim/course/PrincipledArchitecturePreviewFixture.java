package com.progolf.sim.course;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.ShotGuidance;
import com.progolf.sim.shot.StrategicTargetPlanner;
import com.progolf.sim.spatial.Surface;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Writes the complete fixed preview corpus from the same retained Generator 8 canonical geometry careers use. */
public final class PrincipledArchitecturePreviewFixture {
    private static final long WORLD = 0x5052494e4349504cL;
    private static final long[] CORPUS = {103L, 271L, 509L, 887L, 1597L, 2861L};
    private static final Attributes PLAYER = Attributes.uniform(90);

    private PrincipledArchitecturePreviewFixture() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected output JSON path");
        List<String> courses = new ArrayList<>();
        for (EnvironmentClassification environment : new EnvironmentClassification[] {
                EnvironmentClassification.PARKLAND, EnvironmentClassification.LINKS}) {
            for (int seedIndex = 0; seedIndex < CORPUS.length; seedIndex++) {
                long seed = CORPUS[seedIndex];
                SeedCoordinate coordinate = coordinate(seed, environment);
                Course retained = CourseGenerator.generate(coordinate, environment, CourseGenConstants.V5_REFINED_GENERATOR_VERSION);
                Course experiment = CourseGenerator.generatePrincipledArchitecturePreview(coordinate, environment);
                courses.add("{\"seed\":\"" + seed + "\",\"unseen\":" + (seedIndex >= 3)
                        + ",\"biome\":\"" + environment + "\",\"retained\":"
                        + course(retained, environment, false) + ",\"experiment\":"
                        + course(experiment, environment, true) + "}");
            }
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, "[\n" + String.join(",\n", courses) + "\n]\n");
    }

    private static String course(Course course, EnvironmentClassification environment, boolean experiment) {
        List<String> holes = new ArrayList<>();
        for (int holeIndex = 0; holeIndex < course.holes().size(); holeIndex++) {
            holes.add(hole(course.holes().get(holeIndex), course.coursePlan().briefs().get(holeIndex), environment, experiment));
        }
        return "{\"label\":\"" + (experiment ? "Generator 8 principles-driven architecture" : "Retained Generator 7")
                + "\",\"generatorVersion\":" + course.generatorVersion()
                + ",\"provenance\":\"" + (experiment
                ? "Registered Generator 8 output from the same coordinate; playable only through the explicit new-world opt-in."
                : "Registered retained Generator 7 output from the same coordinate.") + "\",\"metrics\":"
                + metrics(PreviewCourseMetrics.inspect(course)) + ",\"holes\":[" + String.join(",", holes) + "]}";
    }

    private static String hole(GeneratedHole hole, HoleBrief brief, EnvironmentClassification environment,
                               boolean experiment) {
        PinPlacement pin = hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
        List<ShotGuidance.StrategicOption> options = List.of();
        if (experiment) {
            var model = hole.forRound(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
            options = StrategicTargetPlanner.options(model, new BallState(model.geometry().tee(), Surface.TEE_BOX),
                    model.startDistance(), Surface.TEE_BOX, PLAYER, Environment.calm());
        }
        StringBuilder json = new StringBuilder("{\"routing\":\"");
        json.append(hole.architecturePlan().routingForm()).append("\",\"bunkers\":").append(count(hole, Surface.BUNKER))
                .append(",\"trees\":").append(count(hole, Surface.TREES)).append(",\"water\":")
                .append(count(hole, Surface.WATER)).append(",\"meaningfulTurns\":")
                .append(PreviewCourseMetrics.meaningfulTurns(hole.architecturePlan().route(), hole.par()))
                .append(",\"strategy\":");
        strategy(json, options);
        json.append(",\"hole\":");
        geometry(json, hole, environment, pin);
        return json.append('}').toString();
    }

    private static String metrics(PreviewCourseMetrics metrics) {
        return "{\"parFourFiveHoles\":" + metrics.parFourFiveHoles()
                + ",\"straightforwardParFourFive\":" + metrics.straightforwardParFourFive()
                + ",\"meaningfulTurnHoles\":" + metrics.meaningfulTurnHoles()
                + ",\"doubleMeaningfulTurnHoles\":" + metrics.doubleMeaningfulTurnHoles()
                + ",\"bunkerRegions\":" + metrics.bunkerRegions()
                + ",\"groupedBunkerHoles\":" + metrics.groupedBunkerHoles()
                + ",\"waterRegions\":" + metrics.waterRegions()
                + ",\"treeRegions\":" + metrics.treeRegions()
                + ",\"repeatedStructureHoles\":" + metrics.repeatedStructureHoles()
                + ",\"minimumFairwayWidth\":" + metrics.minimumFairwayWidth()
                + ",\"meanFairwayWidth\":" + metrics.meanFairwayWidth() + "}";
    }

    private static void strategy(StringBuilder json, List<ShotGuidance.StrategicOption> options) {
        json.append('[');
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) json.append(',');
            ShotGuidance.StrategicOption option = options.get(i);
            json.append("{\"role\":\"").append(option.role()).append("\",\"aimPoint\":");
            point(json, new Position2d(option.aimPoint().x(), option.aimPoint().y()));
            json.append(",\"club\":\"").append(option.suggestedClub()).append("\",\"route\":\"")
                    .append(escape(option.routeSummary())).append("\",\"exposure\":\"")
                    .append(escape(option.exposureSummary())).append("\"}");
        }
        json.append(']');
    }

    private static void geometry(StringBuilder json, GeneratedHole hole, EnvironmentClassification environment,
                                 PinPlacement pin) {
        CourseGeometry geometry = hole.geometry();
        json.append('{').append("\"holeNumber\":").append(hole.number()).append(",\"par\":").append(hole.par())
                .append(",\"length\":").append(hole.length()).append(",\"fairwayHalfWidth\":")
                .append(hole.fairwayHalfWidth()).append(",\"greenHalfWidth\":").append(hole.greenHalfWidth())
                .append(",\"greenDepth\":").append(hole.greenDepth()).append(",\"elevationDelta\":")
                .append(hole.elevationDelta()).append(",\"hasGreensideBunker\":").append(hole.hasGreensideBunker())
                .append(",\"hasWater\":").append(hole.hasWater()).append(",\"hasTrees\":").append(hole.hasTrees())
                .append(",\"pinLateral\":").append(pin.pin().lateralOffset()).append(",\"pinDepth\":")
                .append(pin.pin().depthOffset()).append(",\"courseType\":\"").append(environment)
                .append("\",\"layoutSeed\":\"").append(hole.holeSeed())
                .append("\",\"effectiveWind\":{\"x\":0,\"y\":0,\"magnitude\":0,\"unit\":\"EFFECTIVE_YARDS\"}")
                .append(",\"ball\":{\"position\":");
        point(json, geometry.tee());
        json.append(",\"lie\":\"TEE_BOX\"},\"geometry\":{\"tee\":");
        point(json, geometry.tee());
        json.append(",\"cup\":");
        point(json, pin.cup());
        json.append(",\"playableBoundary\":");
        polygon(json, geometry.playableBoundary());
        json.append(",\"regions\":[");
        for (int i = 0; i < geometry.regions().size(); i++) {
            if (i > 0) json.append(',');
            TerrainRegion region = geometry.regions().get(i);
            json.append("{\"surface\":\"").append(region.surface()).append("\",\"boundary\":");
            polygon(json, region.boundary());
            json.append('}');
        }
        json.append("]}}");
    }

    private static int count(GeneratedHole hole, Surface surface) {
        return (int) hole.geometry().regions().stream().filter(region -> region.surface() == surface).count();
    }

    private static SeedCoordinate coordinate(long seed, EnvironmentClassification environment) {
        return new SeedCoordinate(WORLD, 7, seed, environment.ordinal(), 0, 0, 0);
    }

    private static void polygon(StringBuilder json, List<Position2d> polygon) {
        json.append('[');
        for (int i = 0; i < polygon.size(); i++) {
            if (i > 0) json.append(',');
            point(json, polygon.get(i));
        }
        json.append(']');
    }

    private static void point(StringBuilder json, Position2d point) {
        json.append("{\"x\":").append(point.x()).append(",\"y\":").append(point.y()).append('}');
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
