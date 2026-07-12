package com.progolf.sim.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The World's Media System (spec: news-generation / world-narrative, REQ-240): an append-only feed of
 * {@link NewsEvent}s that the World publishes to as real events occur. It is a pure observer — it stores
 * and answers queries and mutates nothing outside itself (REQ-247/250). It also tracks each golfer's most
 * recent win from published victories, so career narratives can reflect drought and resurgence.
 */
public final class MediaSystem {

    private final List<NewsEvent> feed = new ArrayList<>();
    private final Map<String, Integer> lastWinSeason = new HashMap<>();

    /** Publishes a News Event to the feed, updating win tracking for victory news. */
    public void publish(NewsEvent event) {
        Objects.requireNonNull(event, "event");
        feed.add(event);
        if (event.type() == NewsType.TOURNAMENT_VICTORY) {
            event.subjectGolferId().ifPresent(id -> lastWinSeason.put(id, event.season()));
        }
    }

    /** The full news feed in publication order — the persistent World Narrative (REQ-245). */
    public List<NewsEvent> feed() {
        return Collections.unmodifiableList(new ArrayList<>(feed));
    }

    /**
     * An immutable capture of the media feed (spec: world-snapshot). Only the published feed is stored; the
     * last-win-season index is rebuilt by replaying it (it is a pure function of the victory news).
     */
    public record Snapshot(List<NewsEvent> feed) {
        public Snapshot {
            feed = List.copyOf(feed);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(new ArrayList<>(feed));
    }

    public static MediaSystem restore(Snapshot s) {
        MediaSystem m = new MediaSystem();
        s.feed().forEach(m::publish); // rebuilds lastWinSeason from the victory news, in order
        return m;
    }

    /** Historically significant news, which remains discoverable (REQ-246). */
    public List<NewsEvent> significantNews() {
        List<NewsEvent> significant = new ArrayList<>();
        for (NewsEvent e : feed) {
            if (e.isSignificant()) {
                significant.add(e);
            }
        }
        return significant;
    }

    /** All news about a particular golfer. */
    public List<NewsEvent> newsForGolfer(String golferId) {
        List<NewsEvent> subset = new ArrayList<>();
        for (NewsEvent e : feed) {
            if (e.subjectGolferId().filter(id -> id.equals(golferId)).isPresent()) {
                subset.add(e);
            }
        }
        return subset;
    }

    /** The most recent {@code n} news items. */
    public List<NewsEvent> recent(int n) {
        int from = Math.max(0, feed.size() - Math.max(0, n));
        return Collections.unmodifiableList(new ArrayList<>(feed.subList(from, feed.size())));
    }

    public int size() {
        return feed.size();
    }

    /**
     * The evolving, descriptive career narrative for a golfer (REQ-244), combining the supplied career
     * facts with the feed's own record of the golfer's last win.
     */
    public CareerNarrative careerNarrative(String golferId, int age, int seasonsPlayed, int careerWins,
                                           int rankingPosition, int currentSeason) {
        int seasonsSinceLastWin = lastWinSeason.containsKey(golferId)
                ? currentSeason - lastWinSeason.get(golferId)
                : Integer.MAX_VALUE;
        return NarrativeClassifier.classify(
                new CareerSummary(age, seasonsPlayed, careerWins, rankingPosition, seasonsSinceLastWin));
    }
}
