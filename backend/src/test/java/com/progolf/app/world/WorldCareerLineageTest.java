package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveEnvelope;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldCareerLineageTest {
    private static final String OWNER = "lineage-owner";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path dir;

    @Test
    void newCareersHaveDistinctLineagesEvenWithIdenticalGolferDetailsAndSeed() {
        WorldService service = service();
        WorldSession first = newCareer(service, 301L);
        WorldSession second = newCareer(service, 301L);

        assertThat(first.world().snapshot()).isEqualTo(second.world().snapshot());
        assertThat(first.careerLineageId()).isNotEqualTo(second.careerLineageId());
        assertThat(first.careerLineageId()).isNotEqualTo(first.id());
    }

    @Test
    void namedSaveLoadAndSaveAsKeepTheOriginalLineageWithoutChangingTheWorld() {
        WorldService service = service();
        WorldSession original = newCareer(service, 302L);
        var before = original.world().snapshot();
        String lineage = original.careerLineageId();

        service.save(OWNER, original.id(), "career-original");
        WorldSession firstLoad = service.load(OWNER, "career-original");
        WorldSession secondLoad = service.load(OWNER, "career-original");
        assertThat(firstLoad.id()).isNotEqualTo(secondLoad.id());
        assertThat(firstLoad.careerLineageId()).isEqualTo(lineage);
        assertThat(secondLoad.careerLineageId()).isEqualTo(lineage);

        service.save(OWNER, original.id(), "career-copy");
        assertThat(service.load(OWNER, "career-copy").careerLineageId()).isEqualTo(lineage);
        assertThat(service.get(OWNER, original.id()).careerLineageId()).isEqualTo(lineage);
        assertThat(service.get(OWNER, original.id()).originSaveId()).isEqualTo("career-original");
        service.saveCareer(OWNER, original.id());
        assertThat(service.load(OWNER, "career-original").careerLineageId()).isEqualTo(lineage);
        assertThat(original.world().snapshot()).isEqualTo(before);
        assertThat(firstLoad.world().snapshot()).isEqualTo(before);
    }

    @Test
    void autosaveRecoveryIntoNewNamedSlotPreservesLineage() {
        WorldService service = service();
        WorldSession original = newCareer(service, 303L);
        service.save(OWNER, original.id(), "career-original");
        service.save(OWNER, original.id(), WorldService.AUTOSAVE_ID);

        WorldService restarted = service();
        WorldSession recovered = restarted.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(recovered.careerLineageId()).isEqualTo(original.careerLineageId());
        String newSlot = restarted.saveCareer(OWNER, recovered.id()).saveId();
        assertThat(newSlot).startsWith("career-");
        assertThat(restarted.load(OWNER, newSlot).careerLineageId()).isEqualTo(original.careerLineageId());
        assertThat(recovered.world().snapshot()).isEqualTo(original.world().snapshot());
    }

    @Test
    void legacyNamedLoadsShareARevisionBasedIdentityAndPersistItOnNextSave() throws Exception {
        WorldService service = service();
        WorldSession original = newCareer(service, 304L);
        service.save(OWNER, original.id(), "legacy-named");
        Path file = asLegacyV3("legacy-named");
        byte[] before = Files.readAllBytes(file);

        WorldSession first = service().load(OWNER, "legacy-named");
        WorldSession second = service().load(OWNER, "legacy-named");
        assertThat(first.careerLineageId()).startsWith("legacy-named-");
        assertThat(second.careerLineageId()).isEqualTo(first.careerLineageId());
        assertThat(first.careerLineageId()).isNotEqualTo(original.careerLineageId());
        assertThat(Files.readAllBytes(file)).isEqualTo(before); // load does not migrate the file
        assertThat(first.world().snapshot()).isEqualTo(original.world().snapshot());

        WorldService resumed = service();
        WorldSession migrated = resumed.load(OWNER, "legacy-named");
        resumed.saveCareer(OWNER, migrated.id());
        assertThat(readEnvelope(file).formatVersion()).isEqualTo(4);
        assertThat(service().load(OWNER, "legacy-named").careerLineageId())
                .isEqualTo(first.careerLineageId());
    }

    @Test
    void legacyAutosaveRestoresWithoutClaimingLegacyNamedAncestry() throws Exception {
        WorldService service = service();
        WorldSession original = newCareer(service, 305L);
        service.save(OWNER, original.id(), "legacy-named");
        service.save(OWNER, original.id(), WorldService.AUTOSAVE_ID);
        asLegacyV3("legacy-named");
        asLegacyV3(WorldService.AUTOSAVE_ID);

        WorldService restarted = service();
        WorldSession named = restarted.load(OWNER, "legacy-named");
        WorldSession recovery = restarted.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(recovery.careerLineageId()).startsWith("legacy-autosave-");
        assertThat(recovery.careerLineageId()).isNotEqualTo(named.careerLineageId());
        assertThat(recovery.world().snapshot()).isEqualTo(named.world().snapshot());
        String recoveredNamed = restarted.saveCareer(OWNER, recovery.id()).saveId();
        assertThat(restarted.load(OWNER, recoveredNamed).careerLineageId())
                .isEqualTo(recovery.careerLineageId());
    }

    private WorldService service() {
        return new WorldService(new FilesystemSaveGameStore(dir.toString()));
    }

    private WorldSession newCareer(WorldService service, long seed) {
        WorldSession session = service.create(OWNER, seed, SMALL);
        service.createPlayer(OWNER, session.id(), "Same", "Golfer", Nationality.USA, 18,
                Archetype.ALL_ROUNDER);
        return session;
    }

    private Path asLegacyV3(String saveId) throws Exception {
        Path file = dir.resolve(OWNER).resolve(saveId + ".json");
        SaveEnvelope current = readEnvelope(file);
        ObjectNode payload = (ObjectNode) JSON.readTree(current.payload());
        payload.remove("careerLineageId");
        String oldPayload = JSON.writeValueAsString(payload);
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(oldPayload.getBytes(StandardCharsets.UTF_8)));
        Files.writeString(file, JSON.writeValueAsString(new SaveEnvelope(3, checksum, oldPayload)));
        return file;
    }

    private static SaveEnvelope readEnvelope(Path file) throws Exception {
        return JSON.readValue(Files.readString(file), SaveEnvelope.class);
    }
}
