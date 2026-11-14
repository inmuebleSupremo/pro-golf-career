package com.progolf.app.persistence;

import com.progolf.sim.world.WorldConfig;
import com.progolf.sim.world.WorldSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The complete, serializable contents of a save (spec: save-persistence): the restore inputs (master seed
 * and world configuration) plus the immutable {@link WorldSnapshot}, display {@link SaveMetadata}, and the
 * application-owned seasonal Schedule- and Staff-review acknowledgements and career lineage. Loading rebuilds
 * the world via {@code World.restore(seed, config, snapshot)}; application provenance never enters the
 * simulation snapshot.
 */
public record SaveGame(long seed, WorldConfig config, WorldSnapshot snapshot, SaveMetadata metadata,
                       Integer scheduleReviewAcknowledgedSeason, Integer staffReviewAcknowledgedSeason,
                       String careerLineageId, long autosaveGeneration, Map<String, Long> autosaveHeads) {

    public SaveGame {
        autosaveHeads = autosaveHeads == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(autosaveHeads));
    }

    public SaveGame(long seed, WorldConfig config, WorldSnapshot snapshot, SaveMetadata metadata,
                    Integer scheduleReviewAcknowledgedSeason, Integer staffReviewAcknowledgedSeason,
                    String careerLineageId) {
        this(seed, config, snapshot, metadata, scheduleReviewAcknowledgedSeason,
                staffReviewAcknowledgedSeason, careerLineageId, 0, null);
    }

    /** Older save payloads have no lineage; WorldService assigns a transitional identity on load. */
    public SaveGame(long seed, WorldConfig config, WorldSnapshot snapshot, SaveMetadata metadata,
                    Integer scheduleReviewAcknowledgedSeason, Integer staffReviewAcknowledgedSeason) {
        this(seed, config, snapshot, metadata, scheduleReviewAcknowledgedSeason,
                staffReviewAcknowledgedSeason, null);
    }
}
