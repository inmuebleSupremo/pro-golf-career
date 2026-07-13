package com.progolf.app.persistence;

import com.progolf.sim.world.WorldConfig;
import com.progolf.sim.world.WorldSnapshot;

/**
 * The complete, serializable contents of a save (spec: save-persistence): the restore inputs (master seed
 * and world configuration) plus the immutable {@link WorldSnapshot} and display {@link SaveMetadata}.
 * Loading rebuilds the world via {@code World.restore(seed, config, snapshot)}.
 */
public record SaveGame(long seed, WorldConfig config, WorldSnapshot snapshot, SaveMetadata metadata) {
}
