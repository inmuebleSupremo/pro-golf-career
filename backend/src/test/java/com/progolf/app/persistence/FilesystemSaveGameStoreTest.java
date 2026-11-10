package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.PinPlacementVersion;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.PreparationFocus;
import com.progolf.sim.world.CoursePoolPolicy;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * save-persistence / resource-ownership: the filesystem store round-trips a world through disk, detects
 * corruption, isolates saves, and scopes every operation to its owner so users never see each other's saves.
 */
class FilesystemSaveGameStoreTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final String OWNER = "owner-1";
    private final ObjectMapper mapper = FilesystemSaveGameStore.persistenceMapper();

    @TempDir
    Path dir;
    private FilesystemSaveGameStore store;

    @BeforeEach
    void setUp() {
        store = new FilesystemSaveGameStore(dir.toString());
    }

    private SaveGame gameFrom(World world, long seed, String id) {
        SaveMetadata meta = new SaveMetadata(id, Instant.now(), world.currentSeason(), world.currentWeek(),
                world.playerGolferId().orElse(null));
        return new SaveGame(seed, world.config(), world.snapshot(), meta, null, null);
    }

    @Test
    void savingThenLoadingPreservesTheSnapshot() {
        World world = World.create(123L, SMALL);
        world.advanceSeason();
        store.save(OWNER, "s1", gameFrom(world, 123L, "s1"));

        SaveGame loaded = store.load(OWNER, "s1");
        assertThat(loaded.seed()).isEqualTo(123L);
        assertThat(loaded.config()).isEqualTo(SMALL);
        assertThat(loaded.snapshot()).isEqualTo(world.snapshot()); // full structural equality through JSON
    }

    @Test
    void aPayloadBeyondTheOldJacksonLimitIsListedAndLoadedWithCorrectMetadata() throws Exception {
        World world = World.create(123L, SMALL);
        world.advanceSeason();
        store.save(OWNER, "large", gameFrom(world, 123L, "large"));
        store.save(OWNER, "small", gameFrom(World.create(456L, SMALL), 456L, "small"));

        Path file = dir.resolve(OWNER).resolve("large.json");
        SaveEnvelope original = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        String payload = original.payload().substring(0, original.payload().length() - 1)
                + ",\"futureExtension\":\"" + "x".repeat(20_000_001) + "\"}";
        assertThat(payload.length()).isGreaterThan(20_000_000);
        Files.writeString(file, mapper.writeValueAsString(
                new SaveEnvelope(original.formatVersion(), sha256(payload), payload)));

        assertThat(store.list(OWNER)).extracting(SaveMetadata::saveId).containsExactlyInAnyOrder("large", "small");
        SaveMetadata listed = store.list(OWNER).stream().filter(s -> s.saveId().equals("large")).findFirst().orElseThrow();
        assertThat(listed.season()).isEqualTo(world.currentSeason());
        assertThat(listed.week()).isEqualTo(world.currentWeek());
        SaveGame loaded = store.load(OWNER, "large");
        assertThat(loaded.snapshot()).isEqualTo(world.snapshot());
        assertThat(store.load(OWNER, "small").seed()).isEqualTo(456L);
    }

    @Test
    void theSaveParserLimitIsExplicitAndBounded() {
        var constraints = mapper.getFactory().streamReadConstraints();
        assertThat(constraints.getMaxStringLength()).isEqualTo(FilesystemSaveGameStore.MAX_SAVE_STRING_LENGTH)
                .isLessThan(Integer.MAX_VALUE);
        assertThatThrownBy(() -> constraints.validateStringLength(FilesystemSaveGameStore.MAX_SAVE_STRING_LENGTH + 1))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void unreadableSavesAreSkippedWithAConciseWarning(CapturedOutput output) throws Exception {
        Path owner = Files.createDirectories(dir.resolve(OWNER));
        Files.writeString(owner.resolve("broken.json"), "{not-json");
        Files.writeString(owner.resolve("missing-metadata.json"), mapper.writeValueAsString(
                new SaveEnvelope(FilesystemSaveGameStore.FORMAT_VERSION, sha256("{}"), "{}")));

        assertThat(store.list(OWNER)).isEmpty();
        assertThat(output.getOut()).contains("Skipping unreadable save broken.json")
                .contains("Skipping save missing-metadata.json: missing metadata")
                .doesNotContain("{not-json");
        assertThatThrownBy(() -> store.load(OWNER, "broken")).isInstanceOf(SaveCorruptedException.class);
    }

    @Test
    void playableSessionAndAttemptSurviveDiskSaveAndLoad() {
        World world = World.create(124L, SMALL);
        world.createPlayer("Save", "Training", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        var training = world.startPlayerTraining(PreparationFocus.WEDGES);
        var challenge = training.challenges().get(0);
        world.playerTrainingRangeShot(training.id(), 1, new com.progolf.sim.shot.BallStrikeIntent(
                challenge.suggestedClub(), new com.progolf.sim.shot.AimPoint(
                challenge.target().x(), challenge.target().y()), challenge.suggestedFamily()));
        store.save(OWNER, "training", gameFrom(world, 124L, "training"));

        SaveGame loaded = store.load(OWNER, "training");
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        assertThat(restored.playerTrainingSession()).isEqualTo(world.playerTrainingSession());
        var second = restored.playerTrainingSession().challenges().get(1);
        assertThat(restored.playerTrainingSession().challenges())
                .hasSize(8).allMatch(com.progolf.sim.progression.TrainingChallenge::greenside);
        assertThat(com.progolf.sim.progression.TrainingChallengeGenerator.landingZone(second)).isFalse();
        assertThat(com.progolf.sim.progression.TrainingChallengeGenerator.targetLabel(second))
                .isEqualTo("Hole " + second.holeNumber() + " cup");
        assertThat(restored.playerTrainingSession().nextChallenge()).isEqualTo(2);
        assertThat(restored.startPlayerTraining(PreparationFocus.WEDGES))
                .isEqualTo(restored.playerTrainingSession());
        assertThat(restored.playerPreparation().progressFor(PreparationFocus.WEDGES)).isEqualTo(0.95);
    }

    @Test
    void olderRangeOnlyWedgeSessionWithoutHoleLocatorStillLoadsAndPlays() throws Exception {
        World world = World.create(127L, SMALL);
        world.createPlayer("Old", "Wedges", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        var generated = world.startPlayerTraining(PreparationFocus.WEDGES);
        var golfer = world.careerOf(world.playerGolferId().orElseThrow()).player();
        var oldRange = com.progolf.sim.progression.TrainingChallengeGenerator.generate(PreparationFocus.WEDGES,
                golfer.attributes(), golfer.toGolferState(0), golfer.handedness(),
                Long.parseUnsignedLong(generated.id(), 16));
        store.save(OWNER, "range-only", gameFrom(world, 127L, "range-only"));
        Path file = dir.resolve(OWNER).resolve("range-only.json");
        SaveEnvelope current = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        ObjectNode payload = (ObjectNode) mapper.readTree(current.payload());
        var preparation = (ObjectNode) payload.get("snapshot").get("careerPreparation");
        preparation.properties().forEach(entry -> {
            if (!(entry.getValue().get("playableSession") instanceof ObjectNode session)) return;
            var challenges = (com.fasterxml.jackson.databind.node.ArrayNode) mapper.valueToTree(oldRange);
            challenges.forEach(value -> ((ObjectNode) value).remove(java.util.List.of("courseIndex", "holeNumber")));
            session.set("challenges", challenges);
        });
        String oldPayload = mapper.writeValueAsString(payload);
        Files.writeString(file, mapper.writeValueAsString(new SaveEnvelope(
                current.formatVersion(), sha256(oldPayload), oldPayload)));
        SaveGame loaded = store.load(OWNER, "range-only");
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        var session = restored.playerTrainingSession();
        assertThat(session.challenges()).noneMatch(com.progolf.sim.progression.TrainingChallenge::greenside);
        for (int number = 1; number <= 4; number++) {
            var challenge = session.challenges().get(number - 1);
            session = restored.playerTrainingRangeShot(session.id(), number, new com.progolf.sim.shot.BallStrikeIntent(
                    challenge.suggestedClub(), new com.progolf.sim.shot.AimPoint(
                    challenge.target().x(), challenge.target().y()), challenge.suggestedFamily())).session();
        }
        assertThat(session.nextChallenge()).isEqualTo(5);
        assertThat(restored.playerTrainingGreensideScene()).isNull();
    }

    @Test
    void previouslyMixedWedgeSessionKeepsItsPersistedRangeAndGreensideRoutes() throws Exception {
        World world = World.create(128L, SMALL);
        world.createPlayer("Mixed", "Wedges", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        var generated = world.startPlayerTraining(PreparationFocus.WEDGES);
        var golfer = world.careerOf(world.playerGolferId().orElseThrow()).player();
        var oldRange = com.progolf.sim.progression.TrainingChallengeGenerator.generate(PreparationFocus.WEDGES,
                golfer.attributes(), golfer.toGolferState(0), golfer.handedness(),
                Long.parseUnsignedLong(generated.id(), 16));
        store.save(OWNER, "mixed", gameFrom(world, 128L, "mixed"));
        Path file = dir.resolve(OWNER).resolve("mixed.json");
        SaveEnvelope current = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        ObjectNode payload = (ObjectNode) mapper.readTree(current.payload());
        var preparation = (ObjectNode) payload.get("snapshot").get("careerPreparation");
        preparation.properties().forEach(entry -> {
            if (!(entry.getValue().get("playableSession") instanceof ObjectNode session)) return;
            var challenges = (com.fasterxml.jackson.databind.node.ArrayNode) session.get("challenges");
            challenges.set(0, mapper.valueToTree(oldRange.get(0)));
            challenges.set(2, mapper.valueToTree(oldRange.get(2)));
        });
        String oldPayload = mapper.writeValueAsString(payload);
        Files.writeString(file, mapper.writeValueAsString(new SaveEnvelope(
                current.formatVersion(), sha256(oldPayload), oldPayload)));
        SaveGame loaded = store.load(OWNER, "mixed");
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        var session = restored.playerTrainingSession();
        assertThat(restored.startPlayerTraining(PreparationFocus.WEDGES)).isEqualTo(session);
        assertThat(session.challenges()).hasSize(8);
        assertThat(session.challenges().get(0).greenside()).isFalse();
        assertThat(session.challenges().get(1).greenside()).isTrue();
        assertThat(session.challenges().get(2).greenside()).isFalse();
        assertThat(restored.playerTrainingGreensideScene()).isNull();
        for (int number = 1; number <= 3; number++) {
            var challenge = session.challenges().get(number - 1);
            assertThat(restored.playerTrainingGreensideScene() != null).isEqualTo(challenge.greenside());
            session = restored.playerTrainingRangeShot(session.id(), number, new com.progolf.sim.shot.BallStrikeIntent(
                    challenge.suggestedClub(), new com.progolf.sim.shot.AimPoint(
                    challenge.target().x(), challenge.target().y()), challenge.suggestedFamily())).session();
        }
        assertThat(session.nextChallenge()).isEqualTo(4);
    }

    @Test
    void priorCareerPreparationRecordWithoutPlayableFieldStillLoads() throws Exception {
        World world = World.create(125L, SMALL);
        world.createPlayer("Old", "Career", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        store.save(OWNER, "old-training", gameFrom(world, 125L, "old-training"));
        Path file = dir.resolve(OWNER).resolve("old-training.json");
        SaveEnvelope current = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        ObjectNode payload = (ObjectNode) mapper.readTree(current.payload());
        var preparation = (ObjectNode) payload.get("snapshot").get("careerPreparation");
        preparation.properties().forEach(entry -> ((ObjectNode) entry.getValue()).remove("playableSession"));
        String oldPayload = mapper.writeValueAsString(payload);
        Files.writeString(file, mapper.writeValueAsString(new SaveEnvelope(
                current.formatVersion(), sha256(oldPayload), oldPayload)));

        SaveGame loaded = store.load(OWNER, "old-training");
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        assertThat(restored.playerTrainingSession()).isNull();
        assertThat(restored.playerPreparationAvailable()).isTrue();
    }

    @Test
    void playableSessionWithoutReceiptFieldsStillLoads() throws Exception {
        World world = World.create(126L, SMALL);
        world.createPlayer("Old", "Session", Nationality.USA, 19, Archetype.ALL_ROUNDER);
        world.startPlayerTraining(PreparationFocus.WEDGES);
        store.save(OWNER, "old-session", gameFrom(world, 126L, "old-session"));
        Path file = dir.resolve(OWNER).resolve("old-session.json");
        SaveEnvelope current = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        ObjectNode payload = (ObjectNode) mapper.readTree(current.payload());
        var preparation = (ObjectNode) payload.get("snapshot").get("careerPreparation");
        preparation.properties().forEach(entry -> {
            if (entry.getValue().get("playableSession") instanceof ObjectNode session)
                session.remove(java.util.List.of("fatigueBefore", "fatigueAfter", "ratingChanges"));
        });
        String oldPayload = mapper.writeValueAsString(payload);
        Files.writeString(file, mapper.writeValueAsString(new SaveEnvelope(
                current.formatVersion(), sha256(oldPayload), oldPayload)));

        SaveGame loaded = store.load(OWNER, "old-session");
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        assertThat(restored.playerTrainingSession().fatigueBefore()).isNull();
        assertThat(restored.playerTrainingSession().ratingChanges()).isEmpty();
        assertThat(restored.playerTrainingSession().nextChallenge()).isEqualTo(1);
    }

    @Test
    void aWorldLoadedFromDiskContinuesIdentically() {
        World original = World.create(0xABCDEL, SMALL);
        original.advanceSeason();
        store.save(OWNER, "s1", gameFrom(original, 0xABCDEL, "s1"));

        SaveGame loaded = store.load(OWNER, "s1");
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        for (int i = 0; i < 3; i++) {
            original.advanceSeason();
            restored.advanceSeason();
        }
        assertThat(restored.snapshot()).as("save -> load -> advance == advance").isEqualTo(original.snapshot());
    }

    @Test
    void aCorruptPayloadIsRejected() throws IOException {
        store.save(OWNER, "s1", gameFrom(World.create(1L, SMALL), 1L, "s1"));
        Path file = dir.resolve(OWNER).resolve("s1.json");
        SaveEnvelope env = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        // Keep the version valid but corrupt the checksum so the payload no longer matches.
        Files.writeString(file, mapper.writeValueAsString(
                new SaveEnvelope(env.formatVersion(), "deadbeef", env.payload())));
        assertThatThrownBy(() -> store.load(OWNER, "s1")).isInstanceOf(SaveCorruptedException.class);
    }

    @Test
    void anUnsupportedFormatVersionIsRejected() throws IOException {
        store.save(OWNER, "s1", gameFrom(World.create(1L, SMALL), 1L, "s1"));
        Path file = dir.resolve(OWNER).resolve("s1.json");
        SaveEnvelope env = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        Files.writeString(file, mapper.writeValueAsString(
                new SaveEnvelope(FilesystemSaveGameStore.FORMAT_VERSION + 1, env.checksum(), env.payload())));
        assertThatThrownBy(() -> store.load(OWNER, "s1")).isInstanceOf(SaveVersionMismatchException.class);
    }

    @Test
    void aLegacyV2EnvelopeWithoutGeneratorProvenanceRestoresThroughV1() throws Exception {
        store.save(OWNER, "legacy", gameFrom(World.create(77L, SMALL), 77L, "legacy"));
        Path file = dir.resolve(OWNER).resolve("legacy.json");
        SaveEnvelope current = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        ObjectNode payload = (ObjectNode) mapper.readTree(current.payload());
        ((ObjectNode) payload.get("snapshot")).remove("courseGeneratorVersion");
        ((ObjectNode) payload.get("snapshot")).remove("coursePoolPolicy");
        ((ObjectNode) payload.get("snapshot")).remove("defaultPinPlacementVersion");
        for (var event : ((ObjectNode) payload.get("snapshot")).withArray("schedule")) {
            ((ObjectNode) event).remove("pinPlacementVersion");
        }
        String legacyPayload = mapper.writeValueAsString(payload);
        Files.writeString(file, mapper.writeValueAsString(new SaveEnvelope(2, sha256(legacyPayload), legacyPayload)));

        SaveGame loaded = store.load(OWNER, "legacy");
        assertThat(loaded.snapshot().courseGeneratorVersion()).isNull();
        assertThat(loaded.snapshot().coursePoolPolicy()).isNull();
        assertThat(World.restore(loaded.seed(), loaded.config(), loaded.snapshot()).courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V1_GENERATOR_VERSION);
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        assertThat(restored.snapshot().coursePoolPolicy()).isEqualTo(CoursePoolPolicy.LEGACY_SIX);
        assertThat(restored.defaultPinPlacementVersion()).isEqualTo(PinPlacementVersion.LEGACY_V1);
        assertThat(restored.snapshot().schedule()).allSatisfy(event ->
                assertThat(event.pinPlacementVersion()).isEqualTo(PinPlacementVersion.LEGACY_V1));
    }

    @Test
    void loadingAMissingSaveReportsNotFound() {
        assertThatThrownBy(() -> store.load(OWNER, "nope")).isInstanceOf(SaveNotFoundException.class);
    }

    @Test
    void multipleSavesAreIsolatedListedAndDeletable() {
        store.save(OWNER, "alpha", gameFrom(World.create(1L, SMALL), 1L, "alpha"));
        World b = World.create(2L, SMALL);
        b.advanceSeason();
        store.save(OWNER, "beta", gameFrom(b, 2L, "beta"));

        assertThat(store.list(OWNER)).extracting(SaveMetadata::saveId).containsExactlyInAnyOrder("alpha", "beta");
        assertThat(store.load(OWNER, "alpha").seed()).isEqualTo(1L);
        assertThat(store.load(OWNER, "beta").seed()).isEqualTo(2L);

        store.delete(OWNER, "alpha");
        assertThat(store.exists(OWNER, "alpha")).isFalse();
        assertThat(store.list(OWNER)).extracting(SaveMetadata::saveId).containsExactly("beta");
        assertThatThrownBy(() -> store.load(OWNER, "alpha")).isInstanceOf(SaveNotFoundException.class);
    }

    @Test
    void savesAreIsolatedPerOwner() {
        // Two owners save under the SAME id; each keeps their own world and cannot see the other's.
        store.save("alice", "game", gameFrom(World.create(1L, SMALL), 1L, "game"));
        store.save("bob", "game", gameFrom(World.create(2L, SMALL), 2L, "game"));

        assertThat(store.load("alice", "game").seed()).isEqualTo(1L);
        assertThat(store.load("bob", "game").seed()).isEqualTo(2L);
        assertThat(store.list("alice")).extracting(SaveMetadata::saveId).containsExactly("game");

        // Bob deleting his own save leaves Alice's intact.
        store.delete("bob", "game");
        assertThat(store.exists("bob", "game")).isFalse();
        assertThat(store.exists("alice", "game")).isTrue();

        // An owner with nothing saved sees an empty list, not another owner's saves.
        assertThat(store.list("carol")).isEmpty();
    }

    private static String sha256(String value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
