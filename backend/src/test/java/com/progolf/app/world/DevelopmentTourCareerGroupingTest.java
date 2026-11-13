package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.career.CareerEventRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class DevelopmentTourCareerGroupingTest {
    @Test
    void catalogueEditionsGroupByIdentityWithoutAbsorbingLegacyNames() {
        var first = record("Surrey Heath Invitational", 1, "surrey-heath");
        var second = record("Surrey Heath Invitational", 2, "surrey-heath");
        var other = record("Surrey Heath Invitational", 3, "another-event");
        var legacy = record("Surrey Heath Invitational", 4, null);
        var groups = WorldService.groupCareerRecords(List.of(first, second, other, legacy));
        assertThat(groups).hasSize(3);
        assertThat(groups.get("catalogue:surrey-heath")).containsExactly(first, second);
        assertThat(groups.get("catalogue:another-event")).containsExactly(other);
        assertThat(groups.get("legacy:Surrey Heath Invitational")).containsExactly(legacy);
    }

    private static CareerEventRecord record(String name, int season, String catalogueId) {
        return new CareerEventRecord(name, "Surrey, England", "SIGNATURE", "DEVELOPMENT", "DEVELOPMENT",
                season, 17, 1, -12, true, true, 1000, 0, 0, 0, 72, 0, List.of(), catalogueId, null, null);
    }
}
