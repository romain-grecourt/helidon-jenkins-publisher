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
        gen.writeFieldName("name");
        gen.writeString(pipeline.info.name);
        gen.writeFieldName("gitRepositoryUrl");
        gen.writeString(pipeline.info.gitRepositoryUrl);
        gen.writeFieldName("gitHead");
        gen.writeString(pipeline.info.gitHead);
        gen.writeFieldName("gitCommit");
        gen.writeString(pipeline.info.gitCommit);
        gen.writeFieldName("state");
        gen.writeString(pipeline.status.state.toString());
        gen.writeFieldName("result");
        gen.writeString(pipeline.status.result.toString());
        gen.writeFieldName("date");
        gen.writeString(pipeline.info.timings.date);
        gen.writeFieldName("duration");
        gen.writeNumber(pipeline.info.duration());
        gen.writeFieldName("items");
        gen.writeStartArray();
        for (Stage child : pipeline.children) {
            gen.writeObject(child);
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }
}
