package com.progolf.sim.career;

/**
 * Runtime execution states of a Career (REQ-034). These affect application execution only (save/load/
 * pause) and SHALL NEVER advance or alter gameplay progression.
 */
public enum CareerRuntimeState {
    ACTIVE,
    SAVED,
    LOADED,
    PAUSED
}
