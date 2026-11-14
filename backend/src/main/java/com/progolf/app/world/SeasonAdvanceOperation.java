package com.progolf.app.world;

/** The latest season simulation attempt for one live session. A failed attempt is not safe to replay. */
public record SeasonAdvanceOperation(String requestId, int startingSeason, State state, Integer completedSeason) {

    public enum State { RUNNING, COMPLETED, COMPLETED_UNSAVED, FAILED }

    public static SeasonAdvanceOperation running(String requestId, int startingSeason) {
        return new SeasonAdvanceOperation(requestId, startingSeason, State.RUNNING, null);
    }

    public SeasonAdvanceOperation completed(int season) {
        return new SeasonAdvanceOperation(requestId, startingSeason, State.COMPLETED, season);
    }

    public SeasonAdvanceOperation completedUnsaved(int season) {
        return new SeasonAdvanceOperation(requestId, startingSeason, State.COMPLETED_UNSAVED, season);
    }

    public SeasonAdvanceOperation failed() {
        return new SeasonAdvanceOperation(requestId, startingSeason, State.FAILED, null);
    }
}
