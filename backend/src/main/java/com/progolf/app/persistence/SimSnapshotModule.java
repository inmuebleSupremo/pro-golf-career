package com.progolf.app.persistence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Jackson support for the engine's hand-written value types that are not records (spec: save-persistence).
 * The engine stays serialization-free — this module lives in the app layer and teaches the mapper how to
 * read/write those types by reflection-free custom handlers. Registered into the dedicated persistence
 * ObjectMapper (see {@code FilesystemSaveGameStore#persistenceMapper}).
 *
 * <p>Currently only {@link Attributes} needs it (a final class over a private {@code int[]} with no bean
 * accessors); it is written as an attribute-name → value object and rebuilt via {@link Attributes#of}.
 */
public class SimSnapshotModule extends SimpleModule {

    public SimSnapshotModule() {
        super("sim-snapshot");
        addSerializer(Attributes.class, new AttributesSerializer());
        addDeserializer(Attributes.class, new AttributesDeserializer());
        // Preserve JSON array order for sets: the snapshot's ordered sets (e.g. activeGolfers) drive
        // order-sensitive advancement, and Jackson would otherwise deserialize Set to an unordered HashSet.
        addAbstractTypeMapping(java.util.Set.class, java.util.LinkedHashSet.class);
    }

    private static final class AttributesSerializer extends JsonSerializer<Attributes> {
        @Override
        public void serialize(Attributes value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            for (Attribute a : Attribute.values()) {
                gen.writeNumberField(a.name(), value.get(a));
            }
            gen.writeEndObject();
        }
    }

    private static final class AttributesDeserializer extends JsonDeserializer<Attributes> {
        @Override
        public Attributes deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.readValueAsTree();
            Map<Attribute, Integer> values = new EnumMap<>(Attribute.class);
            for (Attribute a : Attribute.values()) {
                JsonNode field = node.get(a.name());
                if (field == null) {
                    throw new IOException("Missing attribute in save: " + a.name());
                }
                values.put(a, field.asInt());
            }
            return Attributes.of(values);
        }
    }
}
