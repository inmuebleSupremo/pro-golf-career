package com.progolf.sim.course;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reproducible old/new visual pairs from ordinary generated courses; writes no career data. */
public final class V5RefinementPreviewFixture {
    private static final long WORLD = 0x5635524546494e45L;

    private V5RefinementPreviewFixture() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected output JSON path");
        long[] seeds = {4242L, 99127L, 812347L, 4242L, 99127L, 137997L};
        int[] pars = {4, 3, 5, 4, 3, 5};
        RoutingForm[] forms = {RoutingForm.DOGLEG, RoutingForm.GENTLE, RoutingForm.DOUBLE_DOGLEG,
                RoutingForm.DOGLEG, RoutingForm.GENTLE, RoutingForm.DOUBLE_DOGLEG};
        EnvironmentClassification[] environments = {EnvironmentClassification.PARKLAND,
                EnvironmentClassification.PARKLAND, EnvironmentClassification.PARKLAND,
                EnvironmentClassification.LINKS, EnvironmentClassification.LINKS, EnvironmentClassification.LINKS};
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < seeds.length; i++) {
            long seed = seeds[i];
            EnvironmentClassification environment = environments[i];
            SeedCoordinate coordinate = new SeedCoordinate(WORLD, 5, seed, environment.ordinal(), 0, 0, 0);
            Course original = CourseGenerator.generate(coordinate, environment, CourseGenConstants.V5_GENERATOR_VERSION);
            Course refined = CourseGenerator.generate(coordinate, environment,
                    CourseGenConstants.V5_REFINED_GENERATOR_VERSION);
            int par = pars[i];
            RoutingForm form = forms[i];
            GeneratedHole after = refined.holes().stream().filter(hole -> hole.par() == par)
                    .filter(hole -> hole.architecturePlan().routingForm() == form)
                    .filter(hole -> count(hole, Surface.BUNKER) > 0).findFirst()
                    .orElseGet(() -> refined.holes().stream().filter(hole -> hole.par() == par)
                            .filter(hole -> count(hole, Surface.BUNKER) > 0).findFirst().orElseThrow());
            GeneratedHole before = original.holes().get(after.number() - 1);
            entries.add("{\"seed\":\"" + seed + "\",\"unseen\":" + (i == 2 || i == 5)
                    + ",\"biome\":\"" + environment + "\",\"holeNumber\":" + before.number()
                    + ",\"before\":" + entry(before, environment)
                    + ",\"after\":" + entry(after, environment) + "}");
            System.out.println(environment + " " + seed + " hole " + before.number() + " par " + par
                    + " original " + before.architecturePlan().routingForm() + "/" + count(before, Surface.BUNKER)
                    + " bunkers; refined " + after.architecturePlan().routingForm() + "/"
                    + count(after, Surface.BUNKER) + " bunkers, " + count(after, Surface.WATER) + " water");
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, "[\n" + String.join(",\n", entries) + "\n]\n");
    }

    private static int count(GeneratedHole hole, Surface surface) {
        return (int) hole.geometry().regions().stream().filter(region -> region.surface() == surface).count();
    }

    private static String entry(GeneratedHole hole, EnvironmentClassification environment) {
        PinPlacement pin = hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
        CourseGeometry geometry = hole.geometry();
        StringBuilder json = new StringBuilder();
        json.append("{\"routing\":\"").append(hole.architecturePlan().routingForm())
                .append("\",\"bunkers\":").append(count(hole, Surface.BUNKER))
                .append(",\"trees\":").append(count(hole, Surface.TREES))
                .append(",\"water\":").append(count(hole, Surface.WATER))
                .append(",\"hole\":{")
                .append("\"holeNumber\":").append(hole.number()).append(",\"par\":").append(hole.par())
                .append(",\"length\":").append(hole.length())
                .append(",\"fairwayHalfWidth\":").append(hole.fairwayHalfWidth())
                .append(",\"greenHalfWidth\":").append(hole.greenHalfWidth())
                .append(",\"greenDepth\":").append(hole.greenDepth())
                .append(",\"elevationDelta\":").append(hole.elevationDelta())
                .append(",\"hasGreensideBunker\":").append(hole.hasGreensideBunker())
                .append(",\"hasWater\":").append(hole.hasWater())
                .append(",\"hasTrees\":").append(hole.hasTrees())
                .append(",\"pinLateral\":").append(pin.pin().lateralOffset())
                .append(",\"pinDepth\":").append(pin.pin().depthOffset())
                .append(",\"courseType\":\"").append(environment)
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
        return json.append("]}}}").toString();
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
}
