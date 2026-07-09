package com.progolf.sim.world;

import java.time.LocalDate;

/**
 * The shared world calendar (REQ-105): the current season and week, advancing in discrete weekly turns.
 * The single source of time — dates flow from here into ranking and career records.
 */
public final class WorldCalendar {

    private final int weeksPerSeason;
    private final LocalDate baseDate;
    private int season = 1;
    private int week = 1;

    public WorldCalendar(int weeksPerSeason, int baseYear) {
        this.weeksPerSeason = weeksPerSeason;
        this.baseDate = LocalDate.of(baseYear, 1, 1);
    }

    public int currentSeason() {
        return season;
    }

    public int currentWeek() {
        return week;
    }

    public int weeksPerSeason() {
        return weeksPerSeason;
    }

    /** True when the current week is the last of the season. */
    public boolean isSeasonEnd() {
        return week >= weeksPerSeason;
    }

    /** The calendar date for a given season and week. */
    public LocalDate dateFor(int season, int week) {
        long offsetWeeks = (long) (season - 1) * weeksPerSeason + (week - 1);
        return baseDate.plusWeeks(offsetWeeks);
    }

    /** The current calendar date. */
    public LocalDate currentDate() {
        return dateFor(season, week);
    }

    /** Advances one week, rolling into the next season after the final week. */
    public void advance() {
        if (week >= weeksPerSeason) {
            season++;
            week = 1;
        } else {
            week++;
        }
    }
}
