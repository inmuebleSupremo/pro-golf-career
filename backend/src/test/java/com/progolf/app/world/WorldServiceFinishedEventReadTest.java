package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression (play-event flow): a player event that has been played to the end is still "pending" until
 * {@link WorldService#completeEvent} is called. In that finished-but-pending window the play surface reads
 * {@link WorldService#currentSituation} to decide whether to show the "finish event" step — it must return
 * {@code null}, not throw "the event is complete". Previously it threw (BAD_REQUEST), which failed the whole
 * play-state query so the finish step never rendered and the done event permanently jammed the career.
 */
class WorldServiceFinishedEventReadTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final String OWNER = "owner-finish-read";

    @TempDir
    Path dir;

    @Test
    void everyEventCanBeFinishedAndTheNextPlayed() {
        WorldService service = new WorldService(new FilesystemSaveGameStore(dir.toString()));

        // Onboarding (create + createPlayer + save), then resume via load — the real career path.
        WorldSession created = service.create(OWNER, 11L, SMALL);
        service.createPlayer(OWNER, created.id(), "Sam", "Okoye", Nationality.GBR, 19, Archetype.POWER_HITTER);
        service.save(OWNER, created.id(), "career-1");
        String id = service.load(OWNER, "career-1").id();

        int played = 0;
        int guard = 0;
        while (played < 3 && guard++ < 60) {
            if (!service.hasPendingEvent(OWNER, id)) {
                service.advanceWeek(OWNER, id);
                continue;
            }
            service.simEvent(OWNER, id); // "Sim to end" — event is now finished but still pending.

            // The play page refetches here: a finished pending event reports no situation (→ show Finish).
            assertThat(service.currentSituation(OWNER, id))
                    .as("event #%d situation after sim-to-end must be null, not throw", played + 1)
                    .isNull();
            assertThat(service.eventLeaderboard(OWNER, id))
                    .as("event #%d final leaderboard is still readable", played + 1)
                    .isNotEmpty();

            service.completeEvent(OWNER, id); // "Finish event" — resumes the week, autosaves.
            played++;
        }

        assertThat(played).as("should finish one event and go on to play the next").isGreaterThanOrEqualTo(2);
    }
}
