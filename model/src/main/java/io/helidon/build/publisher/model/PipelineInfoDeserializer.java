package io.helidon.build.publisher.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/**
 * Jackson deserializer for {@link PipelineInfo}.
 */
public final class PipelineInfoDeserializer extends StdDeserializer<PipelineInfo> {

    /**
     * Create a new deserializer instance.
     */
    public PipelineInfoDeserializer() {
        this(PipelineInfo.class);
    }

    /**
     * Create a new deserializer instance.
     * @param vc value class
     */
    public PipelineInfoDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PipelineInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        return readPipelineInfo(node);
    }

    static PipelineInfo readPipelineInfo(JsonNode node) {
        return new PipelineInfo(node.get("id").asText(), node.get("repositoryUrl").asText(null),
                node.get("name").asText(null), node.get("scmHead").asText(null), node.get("scmHash").asText(null));
    }
}
