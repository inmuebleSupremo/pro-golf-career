package com.progolf.sim.core;

import java.util.Locale;
import java.util.function.Supplier;

/** Request-thread diagnostics for one season advance; never part of the simulated or saved world. */
public final class SeasonProfile implements AutoCloseable {
    public enum Metric {
        WORLD, AUTOSAVE, EVENT_SETUP, EVENT_PLAY, EVENT_RESULTS, SEASON_END,
        SEASON_RANKING, DEVELOPMENT, FINANCE, SNAPSHOT, SERIALIZATION, CHECKSUM, FILE_WRITE,
        GREEN_GENERATION
    }

    private static final ThreadLocal<SeasonProfile> CURRENT = new ThreadLocal<>();

    private final long[] nanos = new long[Metric.values().length];
    private long events;
    private long physicalEvents;
    private long holeModels;
    private long greenSurfaces;

    private SeasonProfile() {
    }

    public static SeasonProfile start() {
        if (CURRENT.get() != null) throw new IllegalStateException("Season profiling is already active on this thread");
        SeasonProfile profile = new SeasonProfile();
        CURRENT.set(profile);
        return profile;
    }

    public static SeasonProfile current() {
        return CURRENT.get();
    }

    public static <T> T time(Metric metric, Supplier<T> work) {
        SeasonProfile profile = CURRENT.get();
        if (profile == null) return work.get();
        long start = System.nanoTime();
        try {
            return work.get();
        } finally {
            profile.record(metric, System.nanoTime() - start);
        }
    }

    public static void time(Metric metric, Runnable work) {
        SeasonProfile profile = CURRENT.get();
        if (profile == null) {
            work.run();
            return;
        }
        long start = System.nanoTime();
        try {
            work.run();
        } finally {
            profile.record(metric, System.nanoTime() - start);
        }
    }

    public void record(Metric metric, long elapsedNanos) {
        nanos[metric.ordinal()] += elapsedNanos;
    }

    public static void eventProcessed(boolean physicalPutting) {
        SeasonProfile profile = CURRENT.get();
        if (profile == null) return;
        profile.events++;
        if (physicalPutting) profile.physicalEvents++;
    }

    public static void holeModelConstructed() {
        SeasonProfile profile = CURRENT.get();
        if (profile != null) profile.holeModels++;
    }

    public static void greenSurfaceGenerated() {
        SeasonProfile profile = CURRENT.get();
        if (profile != null) profile.greenSurfaces++;
    }

    /** All child timings are inclusive subsets of their parent, not additional elapsed time. */
    public String summary(String outcome, long backendTotalNanos) {
        StringBuilder result = new StringBuilder("seasonProfile outcome=").append(outcome);
        appendMs(result, "backendTotalMs", backendTotalNanos);
        appendMs(result, "worldMs", Metric.WORLD);
        appendMs(result, "autosaveMs", Metric.AUTOSAVE);
        appendMs(result, "eventSetupMs", Metric.EVENT_SETUP);
        appendMs(result, "eventPlayMs", Metric.EVENT_PLAY);
        appendMs(result, "greenGenerationMs", Metric.GREEN_GENERATION);
        appendMs(result, "eventResultsMs", Metric.EVENT_RESULTS);
        appendMs(result, "seasonEndMs", Metric.SEASON_END);
        appendMs(result, "seasonRankingMs", Metric.SEASON_RANKING);
        appendMs(result, "developmentMs", Metric.DEVELOPMENT);
        appendMs(result, "financeMs", Metric.FINANCE);
        appendMs(result, "snapshotMs", Metric.SNAPSHOT);
        appendMs(result, "serializationMs", Metric.SERIALIZATION);
        appendMs(result, "checksumMs", Metric.CHECKSUM);
        appendMs(result, "fileWriteMs", Metric.FILE_WRITE);
        return result.append(" events=").append(events)
                .append(" physicalEvents=").append(physicalEvents)
                .append(" holeModels=").append(holeModels)
                .append(" greenSurfaces=").append(greenSurfaces)
                .append(" timing=childrenIncludedInParents")
                .append(" greenGenerationNestedIn=eventPlayWhereApplicable").toString();
    }

    private void appendMs(StringBuilder result, String name, Metric metric) {
        appendMs(result, name, nanos[metric.ordinal()]);
    }

    private static void appendMs(StringBuilder result, String name, long elapsedNanos) {
        result.append(' ').append(name).append('=')
                .append(String.format(Locale.ROOT, "%.3f", elapsedNanos / 1_000_000.0));
    }

    @Override
    public void close() {
        if (CURRENT.get() == this) CURRENT.remove();
    }
}
