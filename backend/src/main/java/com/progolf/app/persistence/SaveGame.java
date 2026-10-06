package com.progolf.app.persistence;

import com.progolf.sim.world.WorldConfig;
import com.progolf.sim.world.WorldSnapshot;

/**
 * The complete, serializable contents of a save (spec: save-persistence): the restore inputs (master seed
 * and world configuration) plus the immutable {@link WorldSnapshot}, display {@link SaveMetadata}, and the
 * application-owned seasonal Schedule- and Staff-review acknowledgements. Loading rebuilds the world via
 * {@code World.restore(seed, config, snapshot)}; acknowledgements never enter the simulation snapshot.
 */
public record SaveGame(long seed, WorldConfig config, WorldSnapshot snapshot, SaveMetadata metadata,
                       Integer scheduleReviewAcknowledgedSeason, Integer staffReviewAcknowledgedSeason) {
}
