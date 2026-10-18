package com.progolf.sim.course;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.Surface;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Rebuilds the frontend review fixture from ordinary V5 Links holes; never writes career data. */
public final class LinksPreviewFixture {
    private static final long WORLD = 0x5635_4c49_4e4b_53L;

    private LinksPreviewFixture() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected output JSON path");
        long[] seeds = {4242L, 99127L, 812347L, 137997L};
        int[] pars = {4, 3, 5, 4};
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < seeds.length; i++) {
            long seed = seeds[i];
            Course course = CourseGenerator.generate(new SeedCoordinate(WORLD, 5, seed,
                    EnvironmentClassification.LINKS.ordinal(), 0, 0, 0),
                    EnvironmentClassification.LINKS, CourseGenConstants.V5_GENERATOR_VERSION);
            int par = pars[i];
            boolean shoreline = i == 3;
            GeneratedHole hole = course.holes().stream().filter(candidate -> candidate.par() == par)
                    .max(Comparator.comparingInt(candidate -> hazardScore(candidate, shoreline))).orElseThrow();
            entries.add(entry(seed, i >= 2, hole));
            System.out.println("Links seed " + seed + " hole " + hole.number() + " par " + par
                    + " routing " + hole.architecturePlan().routingForm() + " bunkers " + count(hole, Surface.BUNKER)
                    + " trees " + count(hole, Surface.TREES) + " water " + count(hole, Surface.WATER));
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, "[\n" + String.join(",\n", entries) + "\n]\n");
    }

    private static int hazardScore(GeneratedHole hole, boolean shoreline) {
        return (shoreline ? 20 : -20) * count(hole, Surface.WATER)
                + 3 * count(hole, Surface.BUNKER)
                + (hole.architecturePlan().routingForm() == RoutingForm.DOGLEG ? 5 : 0)
                + (hole.architecturePlan().routingForm() == RoutingForm.DOUBLE_DOGLEG ? 8 : 0);
    }

    private static int count(GeneratedHole hole, Surface surface) {
        return (int) hole.geometry().regions().stream().filter(region -> region.surface() == surface).count();
    }

    private static String entry(long seed, boolean unseen, GeneratedHole hole) {
        PinPlacement pin = hole.pinPlacementFor(1, CourseSetup.standard(), PinPlacementVersion.V5_EFFECTIVE_GREEN);
        CourseGeometry geometry = hole.geometry();
        StringBuilder json = new StringBuilder();
        json.append("{\"seed\":\"").append(seed).append("\",\"unseen\":").append(unseen)
                .append(",\"routing\":\"").append(hole.architecturePlan().routingForm())
                .append("\",\"bunkers\":").append(count(hole, Surface.BUNKER))
                .append(",\"trees\":").append(count(hole, Surface.TREES))
                .append(",\"water\":").append(count(hole, Surface.WATER)).append(",\"hole\":{")
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
                .append(",\"courseType\":\"LINKS\",\"layoutSeed\":\"").append(hole.holeSeed())
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
