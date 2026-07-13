package com.progolf.app.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.app.persistence.SaveMetadata;
import com.progolf.app.world.WorldService;
import com.progolf.app.world.WorldSession;
import com.progolf.app.world.WorldSessionNotFoundException;
import com.progolf.sim.world.WorldConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * resource-ownership: sessions and saves belong to the user who created them. A different user is denied
 * (as not-found) at both the service boundary and over GraphQL, and each user's save list is their own.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class WorldOwnershipTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorldService worldService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private String freshUser() {
        return "user-" + UUID.randomUUID();
    }

    private void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of()));
    }

    // --- Service boundary ---

    @Test
    void aNonOwnerCannotReachAnothersSession() {
        String alice = freshUser();
        String bob = freshUser();
        WorldSession session = worldService.create(alice, 700L, SMALL);

        assertThat(worldService.status(alice, session.id()).season()).isEqualTo(1);
        assertThatThrownBy(() -> worldService.status(bob, session.id()))
                .isInstanceOf(WorldSessionNotFoundException.class);
        assertThatThrownBy(() -> worldService.advanceSeason(bob, session.id()))
                .isInstanceOf(WorldSessionNotFoundException.class);
    }

    @Test
    void savesAndAutosavesAreScopedToTheOwner() {
        String alice = freshUser();
        String bob = freshUser();
        WorldSession aliceSession = worldService.create(alice, 701L, SMALL);
        worldService.save(alice, aliceSession.id(), "shared-id");
        WorldSession bobSession = worldService.create(bob, 702L, SMALL);
        worldService.save(bob, bobSession.id(), "shared-id");

        // Each lists only their own, even though the save id is identical.
        assertThat(worldService.listSaves(alice)).extracting(SaveMetadata::saveId).containsExactly("shared-id");
        assertThat(worldService.load(alice, "shared-id").world().snapshot())
                .isEqualTo(aliceSession.world().snapshot());
        assertThat(worldService.load(bob, "shared-id").world().snapshot())
                .isEqualTo(bobSession.world().snapshot());

        // Autosaves are independent: advancing Alice's season does not touch Bob's reserved slot.
        worldService.advanceSeason(alice, aliceSession.id());
        worldService.advanceSeason(bob, bobSession.id());
        assertThat(worldService.load(alice, WorldService.AUTOSAVE_ID).world().currentSeason()).isEqualTo(2);
        assertThat(worldService.load(bob, WorldService.AUTOSAVE_ID).world().currentSeason()).isEqualTo(2);
        assertThat(worldService.load(alice, WorldService.AUTOSAVE_ID).world().snapshot())
                .isNotEqualTo(worldService.load(bob, WorldService.AUTOSAVE_ID).world().snapshot());
    }

    // --- Over GraphQL ---

    @Test
    void oneUsersSessionIsNotFoundForAnother() {
        String alice = freshUser();
        String bob = freshUser();

        authenticateAs(alice);
        String worldId = graphQlTester.document("""
                        mutation Create($cfg: WorldConfigInput){ createWorld(seed: 703, config: $cfg){ id } }
                        """)
                .variable("cfg", java.util.Map.of("populationSize", 40, "weeksPerSeason", 6,
                        "eventsPerTierPerSeason", 3, "fieldSize", 20, "coursePoolSize", 4))
                .execute()
                .path("createWorld.id").entity(String.class).get();

        // Bob cannot see Alice's world.
        authenticateAs(bob);
        graphQlTester.document("query($id: ID!){ world(id: $id){ season } }")
                .variable("id", worldId).execute()
                .errors().expect(error -> error.getErrorType() == ErrorType.NOT_FOUND);

        // Alice still can.
        authenticateAs(alice);
        graphQlTester.document("query($id: ID!){ world(id: $id){ season } }")
                .variable("id", worldId).execute()
                .path("world.season").entity(Integer.class).isEqualTo(1);
    }
}
