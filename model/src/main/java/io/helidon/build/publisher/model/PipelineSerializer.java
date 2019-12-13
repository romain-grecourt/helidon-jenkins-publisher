package io.helidon.build.publisher.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson serializer for {@link Pipeline}.
 */
public final class PipelineSerializer extends StdSerializer<Pipeline> {

    /**
     * Create a new serializer instance.
     */
    public PipelineSerializer() {
        super(Pipeline.class);
    }

    @Override
    public void serialize(Pipeline pipeline, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("id");
        gen.writeString(pipeline.info.id);
        gen.writeFieldName("title");
        gen.writeString(pipeline.info.title);
        gen.writeFieldName("repositoryUrl");
        gen.writeString(pipeline.info.repositoryUrl);
        gen.writeFieldName("scmHead");
        gen.writeString(pipeline.info.scmHead);
        gen.writeFieldName("scmHash");
        gen.writeString(pipeline.info.scmHash);
        gen.writeFieldName("state");
        gen.writeString(pipeline.status.state.toString());
        gen.writeFieldName("result");
        gen.writeString(pipeline.status.result.toString());
        gen.writeFieldName("startTime");
        gen.writeNumber(pipeline.timings.startTime);
        gen.writeFieldName("endTime");
        gen.writeNumber(pipeline.timings.endTime);
        gen.writeFieldName("children");
        gen.writeStartArray();
        for (Stage child : pipeline.children) {
            gen.writeObject(child);
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }
}
