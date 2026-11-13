package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.progolf.sim.course.CourseGeometry;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.course.TerrainRegion;
import com.progolf.sim.spatial.Surface;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseGeometryRepresentationTest {

    @Test
    void preparedRegionBoundsDoNotAppearInGeometryJson() throws Exception {
        List<Position2d> boundary = List.of(new Position2d(-10, -10), new Position2d(10, -10),
                new Position2d(10, 10), new Position2d(-10, 10));
        CourseGeometry geometry = new CourseGeometry(new Position2d(0, 0), new Position2d(0, 5), boundary,
                List.of(new TerrainRegion(Surface.GREEN, boundary)));
        ObjectMapper mapper = new ObjectMapper();

        var json = mapper.valueToTree(geometry);
        assertThat(json.size()).isEqualTo(4);
        assertThat(json.get("regions").isArray()).isTrue();
        assertThat(json.get("regions").get(0).size()).isEqualTo(2);
        assertThat(mapper.treeToValue(json, CourseGeometry.class)).isEqualTo(geometry);
    }
}
