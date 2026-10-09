package com.progolf.sim.course;

import com.progolf.sim.core.SeedCoordinate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes a reviewable V5/V6 complete-course gallery from shared context and authoritative canonical geometry. */
public final class V5CourseGallery {
    // Reuses the retained V4 role-hazard corpus world for the regression half of this gallery.
    private static final long WORLD = 0x563448415a415244L;

    private V5CourseGallery() { }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("target", "course-gallery", "index.html") : Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, html(), StandardCharsets.UTF_8);
        System.out.println(output.toAbsolutePath());
        System.out.println(summary());
    }

    private static String html() {
        StringBuilder page = new StringBuilder("""
                <!doctype html><meta charset="utf-8"><title>V5 versus V6 course architecture gallery</title>
                <style>
                body{margin:24px;background:#10251f;color:#eef4ee;font:14px system-ui,sans-serif} h1,h2{color:#f4d78e}
                .meta{color:#b8cbbb}.course{margin:28px 0;padding:18px;background:#17352c;border-radius:12px}
                .grid{display:grid;grid-template-columns:repeat(6,minmax(150px,1fr));gap:10px}.hole{background:#0e211c;padding:7px;border-radius:8px}
                svg{width:100%;aspect-ratio:1/1.35;background:#315747;border-radius:5px}.course-map{width:100%;aspect-ratio:1/0.48;margin:12px 0}.DEEP_ROUGH{fill:#315747}.PRIMARY_ROUGH{fill:#497452}
                .FIRST_CUT{fill:#6b9857}.FAIRWAY{fill:#9bcf6c}.FRINGE{fill:#b8dc7a}.GREEN{fill:#d3ee8a}.BUNKER{fill:#e4c986}
                .WATER{fill:#56a9cd}.TREES,.RECOVERY_AREA{fill:#214a30}.boundary{fill:none;stroke:#e9f3db;stroke-width:1.2}.tee{fill:#f4d78e}.cup{fill:#f9f9f0}
                .COAST_WATER,.BAY_WATER,.LAKE{fill:#56a9cd}.WOODLAND_MASS,.COPSE{fill:#1e5034}.CLEARING,.PARKLAND_FIELD{fill:#477b4b}.LINKS_HEATH{fill:#879064}.DUNE_BAND{fill:#cabe7d}
                @media(max-width:1100px){.grid{grid-template-columns:repeat(3,minmax(170px,1fr))}}
                </style><h1>V5 versus V6 course architecture gallery</h1>
                <p class="meta">Every reported course contains all 18 holes. V6 maps show shared generated context plus transformed authoritative
                geometry; local landforms remain direct projections of backend geometry, never frontend terrain generation.</p>
                """);
        List<EnvironmentClassification> environments = List.of(EnvironmentClassification.PARKLAND,
                EnvironmentClassification.LINKS, EnvironmentClassification.COASTAL, EnvironmentClassification.WOODLAND);
        for (int cohort = 0; cohort < 2; cohort++) for (int id = 0; id < environments.size(); id++) {
            EnvironmentClassification environment = environments.get(id);
            long courseId = cohort == 0 ? id : 100 + id;
            String corpus = cohort == 0 ? "known regression coordinate" : "locked unseen coordinate";
            SeedCoordinate coordinate = new SeedCoordinate(WORLD, 5, courseId, environment.ordinal(), 0, 0, 0);
            appendCourse(page, CourseGenerator.generate(coordinate, environment, CourseGenConstants.V5_GENERATOR_VERSION), environment,
                    "V5 " + corpus);
            appendCourse(page, CourseGenerator.generate(coordinate, environment, CourseGenConstants.V6_GENERATOR_VERSION), environment,
                    "V6 " + corpus);
        }
        page.append("<p class=\"meta\">Corpus: known regression and locked unseen coordinates across four environments, "
                + "paired V5/V6 courses, 288 displayed holes. "
                + "Use the V5 summaries and full sheets together; neither metrics nor selected highlights replace review.</p>");
        return page.toString();
    }

    private static List<CourseDesignProfile> profiles() {
        return List.of(new CourseDesignProfile(StrategicEmphasis.BALANCED, WidthTendency.GENEROUS, RecoverySeverity.FORGIVING),
                new CourseDesignProfile(StrategicEmphasis.POSITIONAL, WidthTendency.EXACTING, RecoverySeverity.BALANCED),
                new CourseDesignProfile(StrategicEmphasis.RISK_REWARD, WidthTendency.BALANCED, RecoverySeverity.PENAL),
                new CourseDesignProfile(StrategicEmphasis.POSITIONAL, WidthTendency.BALANCED, RecoverySeverity.PENAL));
    }

    private static List<CourseArchitectureProfile> architectures() {
        return List.of(new CourseArchitectureProfile(.25, .20, .35, .82, .22),
                new CourseArchitectureProfile(.74, .70, .78, .28, .76),
                new CourseArchitectureProfile(.88, .58, .66, .45, .88),
                new CourseArchitectureProfile(.52, .82, .55, .38, .64));
    }

    private static String summary() {
        List<EnvironmentClassification> environments = List.of(EnvironmentClassification.PARKLAND,
                EnvironmentClassification.LINKS, EnvironmentClassification.COASTAL, EnvironmentClassification.WOODLAND);
        double v5RouteChord = 0.0, v6RouteChord = 0.0;
        int v5TwoTurns = 0, v6TwoTurns = 0, v5Asymmetry = 0, v6Asymmetry = 0, v5Bunkers = 0, v6Bunkers = 0,
                v5Trees = 0, v6Trees = 0, pairs = 0;
        for (int cohort = 0; cohort < 2; cohort++) for (int id = 0; id < environments.size(); id++) {
            EnvironmentClassification environment = environments.get(id);
            long courseId = cohort == 0 ? id : 100 + id;
            SeedCoordinate coordinate = new SeedCoordinate(WORLD, 5, courseId, environment.ordinal(), 0, 0, 0);
            CourseQualityReport v5 = CourseQualityReport.inspect(CourseGenerator.generate(coordinate, environment, CourseGenConstants.V5_GENERATOR_VERSION));
            CourseQualityReport v6 = CourseQualityReport.inspect(CourseGenerator.generate(coordinate, environment, CourseGenConstants.V6_GENERATOR_VERSION));
            v5RouteChord += v5.meanRouteChordRatio(); v6RouteChord += v6.meanRouteChordRatio();
            v5TwoTurns += v5.twoTurnHoles(); v6TwoTurns += v6.twoTurnHoles();
            v5Asymmetry += v5.asymmetricWidthStations(); v6Asymmetry += v6.asymmetricWidthStations();
            v5Bunkers += v5.bunkerRegions(); v6Bunkers += v6.bunkerRegions();
            v5Trees += v5.treeRegions(); v6Trees += v6.treeRegions();
            pairs++;
        }
        return String.format(java.util.Locale.ROOT,
                "[v6-gallery] pairs=%d v5/v6 meanRouteChord=%.3f/%.3f twoTurnHoles=%d/%d asymmetricStations=%d/%d bunkerRegions=%d/%d treeRegions=%d/%d",
                pairs, v5RouteChord / pairs, v6RouteChord / pairs, v5TwoTurns, v6TwoTurns, v5Asymmetry, v6Asymmetry,
                v5Bunkers, v6Bunkers, v5Trees, v6Trees);
    }

    private static void appendCourse(StringBuilder page, Course course, EnvironmentClassification environment, String label) {
        CourseQualityReport report = CourseQualityReport.inspect(course);
        CourseEnvironmentalQualityReport environmental = CourseEnvironmentalQualityReport.inspect(course);
        CourseLandscapeQualityReport landscape = CourseLandscapeQualityReport.inspect(course);
        page.append("<section class=course><h2>").append(label).append(" — ").append(environment).append("</h2><p class=meta>")
                .append("version ").append(course.generatorVersion()).append(" · seed ").append(course.identity().name())
                .append(" · design ").append(course.designProfile())
                .append(course.architecturePlan() == null ? "" : " · architecture " + course.architecturePlan().profile())
                .append(" · straight/gentle/dogleg/double ").append(report.straightHoles()).append('/').append(report.gentleHoles())
                .append('/').append(report.doglegHoles()).append('/').append(report.twoTurnHoles())
                .append(" · bunker/tree regions ").append(report.bunkerRegions()).append('/').append(report.treeRegions())
                .append(" · asymmetric stations ").append(report.asymmetricWidthStations())
                .append(" · signature buckets ").append(report.distinctSignatureBuckets()).append(" · route/chord ")
                .append(String.format(java.util.Locale.ROOT, "%.3f", report.meanRouteChordRatio()))
                .append("<br>environment · trees in/out ").append(environmental.treeRegionsInBounds()).append('/')
                .append(environmental.treeRegionsOutsideBounds()).append(" · tree holes/adjacent/corridor ")
                .append(environmental.holesWithInBoundsTrees()).append('/').append(environmental.fairwayAdjacentTreeRegions())
                .append('/').append(environmental.woodedCorridorHoles()).append(" · bunkers fairway/green/large/grouped ")
                .append(environmental.fairwayBunkers()).append('/').append(environmental.greensideBunkers()).append('/')
                .append(environmental.largeBunkers()).append('/').append(environmental.groupedBunkerHoles())
                .append(" · water regions/shore holes/area ").append(environmental.waterRegions()).append('/')
                .append(environmental.shorelineAdjacentHoles()).append('/')
                .append(String.format(java.util.Locale.ROOT, "%.3f", environmental.waterAreaRatio()))
                .append("</p>");
        if (course.landscapePlan() != null) page.append("<p class=meta>V6 placement · ").append(landscape.placements())
                .append(" holes · max green-to-tee transition ").append(Math.round(landscape.maxTransitionToNextTee()))
                .append(" yd · fallback ").append(landscape.placementFallback())
                .append("<br>shared relationships · shore/bay ").append(landscape.shorelineRelationships()).append('/')
                .append(landscape.bayRelationships()).append(" · woodland/clearing ").append(landscape.woodlandCorridors())
                .append('/').append(landscape.clearingLandings()).append(" · parkland/copse ").append(landscape.parklandFields())
                .append('/').append(landscape.copseEdges()).append(" · links/dune ").append(landscape.linksExposed())
                .append('/').append(landscape.duneEdges()).append(" · context features ").append(landscape.featureCount())
                .append("</p>");
        if (course.landscapePlan() != null) appendCourseMap(page, course);
        page.append("<div class=grid>");
        for (GeneratedHole hole : course.holes()) appendHole(page, hole);
        page.append("</div></section>");
    }

    private static void appendHole(StringBuilder page, GeneratedHole hole) {
        CourseGeometry geometry = hole.geometry();
        Bounds bounds = Bounds.of(geometry, hole.landscapeContext());
        long bunkers = geometry.regions().stream().filter(region -> region.surface() == com.progolf.sim.spatial.Surface.BUNKER).count();
        long trees = geometry.regions().stream().filter(region -> region.surface() == com.progolf.sim.spatial.Surface.TREES).count();
        String form = hole.architecturePlan() == null ? "legacy" : hole.architecturePlan().routingForm().name().toLowerCase();
        page.append("<article class=hole><strong>").append(hole.number()).append(" · Par ").append(hole.par()).append("</strong><br><small>")
                .append(Math.round(hole.length())).append(" yd · ").append(form).append(" · ").append(bunkers)
                .append(" bunker / ").append(trees).append(" tree regions</small><svg viewBox=\"0 0 200 270\">");
        if (hole.landscapeContext() != null) for (LandscapeContextFeature feature : hole.landscapeContext().features()) {
            appendPolygon(page, feature.boundary(), bounds, feature.kind().name());
        }
        appendPolygon(page, geometry.playableBoundary(), bounds, "boundary");
        for (TerrainRegion region : geometry.regions()) appendPolygon(page, region.boundary(), bounds, region.surface().name());
        appendCircle(page, geometry.tee(), bounds, "tee", 3.2);
        appendCircle(page, geometry.greenCenter(), bounds, "cup", 2.7);
        page.append("</svg></article>");
    }

    private static void appendCourseMap(StringBuilder page, Course course) {
        CourseLandscapePlan landscape = course.landscapePlan();
        CourseBounds bounds = CourseBounds.of(course);
        page.append("<h3>Shared V6 course landscape</h3><svg class=course-map viewBox=\"0 0 620 300\">");
        for (LandscapeFeature feature : landscape.features()) appendCoursePolygon(page, feature.boundary(), bounds, feature.kind().name());
        for (int i = 0; i < course.holes().size(); i++) {
            GeneratedHole hole = course.holes().get(i);
            CourseCoordinateTransform transform = landscape.placement(hole.number()).transform();
            appendCoursePolygon(page, hole.geometry().playableBoundary().stream().map(transform::toCourse).toList(), bounds, "DEEP_ROUGH");
            hole.geometry().regions().stream().filter(region -> region.surface() == com.progolf.sim.spatial.Surface.FAIRWAY
                    || region.surface() == com.progolf.sim.spatial.Surface.GREEN).forEach(region -> appendCoursePolygon(page,
                    region.boundary().stream().map(transform::toCourse).toList(), bounds, region.surface().name()));
            Position2d tee = transform.toCourse(hole.geometry().tee());
            page.append("<text x=\"").append(bounds.x(tee.x())).append("\" y=\"").append(bounds.y(tee.y()))
                    .append("\" fill=\"#fff\" font-size=\"8\">").append(hole.number()).append("</text>");
        }
        page.append("</svg>");
    }

    private static void appendCoursePolygon(StringBuilder page, List<Position2d> points, CourseBounds bounds, String css) {
        page.append("<polygon class=\"").append(css).append("\" points=\"");
        for (Position2d point : points) page.append(bounds.x(point.x())).append(',').append(bounds.y(point.y())).append(' ');
        page.append("\"/>");
    }

    private static void appendPolygon(StringBuilder page, List<Position2d> points, Bounds bounds, String css) {
        page.append("<polygon class=\"").append(css).append("\" points=\"");
        for (Position2d point : points) page.append(bounds.x(point.x())).append(',').append(bounds.y(point.y())).append(' ');
        page.append("\"/>");
    }

    private static void appendCircle(StringBuilder page, Position2d point, Bounds bounds, String css, double radius) {
        page.append("<circle class=\"").append(css).append("\" cx=\"").append(bounds.x(point.x())).append("\" cy=\"")
                .append(bounds.y(point.y())).append("\" r=\"").append(radius).append("\"/>");
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {
        static Bounds of(CourseGeometry geometry, LandscapeHoleContext context) {
            List<Position2d> points = new java.util.ArrayList<>(geometry.playableBoundary());
            if (context != null) context.features().forEach(feature -> points.addAll(feature.boundary()));
            return new Bounds(points.stream().mapToDouble(Position2d::x).min().orElseThrow(),
                    points.stream().mapToDouble(Position2d::x).max().orElseThrow(),
                    points.stream().mapToDouble(Position2d::y).min().orElseThrow(),
                    points.stream().mapToDouble(Position2d::y).max().orElseThrow());
        }
        double x(double value) { return 14.0 + (value - minX) / (maxX - minX) * 172.0; }
        double y(double value) { return 256.0 - (value - minY) / (maxY - minY) * 242.0; }
    }

    private record CourseBounds(double minX, double maxX, double minY, double maxY) {
        static CourseBounds of(Course course) {
            List<Position2d> points = new java.util.ArrayList<>();
            CourseLandscapePlan plan = course.landscapePlan();
            for (LandscapeFeature feature : plan.features()) points.addAll(feature.boundary());
            for (GeneratedHole hole : course.holes()) points.addAll(hole.geometry().playableBoundary().stream()
                    .map(plan.placement(hole.number()).transform()::toCourse).toList());
            return new CourseBounds(points.stream().mapToDouble(Position2d::x).min().orElseThrow(),
                    points.stream().mapToDouble(Position2d::x).max().orElseThrow(),
                    points.stream().mapToDouble(Position2d::y).min().orElseThrow(),
                    points.stream().mapToDouble(Position2d::y).max().orElseThrow());
        }
        double x(double value) { return 14.0 + (value - minX) / (maxX - minX) * 592.0; }
        double y(double value) { return 286.0 - (value - minY) / (maxY - minY) * 272.0; }
    }
}
