package io.helidon.build.publisher.model;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson support.
 */
public final class JacksonSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Serializer for {@link Pipeline}.
     */
    public static final class PipelineSerializer extends StdSerializer<Pipeline> {

        /**
         * Create a new serializer instance.
         */
        public PipelineSerializer() {
            super(Pipeline.class);
        }

        @Override
        public void serialize(Pipeline pipeline, JsonGenerator gen, SerializerProvider provider) throws IOException {
            writePipeline(pipeline, gen);
        }
    }

    /**
     * Deserializer for {@link Pipeline}.
     */
    public static final class PipelineDeserializer extends StdDeserializer<Pipeline> {

        /**
         * Create a new deserializer instance.
         */
        public PipelineDeserializer() {
            this(Pipeline.class);
        }

        /**
         * Create a new deserializer instance.
         *
         * @param vc value class
         */
        public PipelineDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Pipeline deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return readPipeline(jp.readValueAsTree());
        }
    }

    /**
     * Deserializer for {@link PipelineInfo}.
     */
    public static final class PipelineInfoDeserializer extends StdDeserializer<PipelineInfo> {

        /**
         * Create a new deserializer instance.
         */
        public PipelineInfoDeserializer() {
            this(PipelineInfo.class);
        }

        /**
         * Create a new deserializer instance.
         *
         * @param vc value class
         */
        public PipelineInfoDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public PipelineInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return readPipelineInfo(jp.readValueAsTree());
        }
    }

    /**
     * Serializer for {@link PipelineInfo}.
     */
    public static final class PipelineInfoSerializer extends StdSerializer<PipelineInfo> {

        /**
         * Create a new serializer instance.
         */
        public PipelineInfoSerializer() {
            super(PipelineInfo.class);
        }

        @Override
        public void serialize(PipelineInfo info, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            writePipelineInfo(info, gen);
            gen.writeEndObject();
        }
    }

    /**
     * Serializer for {@link TestSuiteResults}.
     */
    public static final class TestSuiteResultsSerializer extends StdSerializer<TestSuiteResults> {

        /**
         * Create a new instance.
         */
        public TestSuiteResultsSerializer() {
            super(TestSuiteResults.class);
        }

        @Override
        public void serialize(TestSuiteResults results, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartArray();
            for (TestSuiteResult result : results.items()) {
                gen.writeObject(result);
            }
            gen.writeEndArray();
        }
    }

    /**
     * Serializer for {@link Artifacts}.
     */
    public static final class ArtifactsSerializer extends StdSerializer<Artifacts> {

        /**
         * Create a new instance.
         */
        public ArtifactsSerializer() {
            super(Artifacts.class);
        }

        @Override
        public void serialize(Artifacts artifacts, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartArray();
            for (Artifacts.Item item : artifacts.items()) {
                gen.writeObject(item);
            }
            gen.writeEndArray();
        }
    }

    /**
     * Write a JSON object.
     * @param json object to write
     * @param os output stream
     * @throws IOException if an IO error occurs
     */
    public static void write(OutputStream os, Object json) throws IOException {
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(os, json);
    }

    /**
     * Read a JSON object.
     * @param <T> object type
     * @param is input stream
     * @param type type
     * @return T
     * @throws IOException if an IO error occurs
     */
    public static <T> T read(InputStream is, Class<T> type) throws IOException {
        return MAPPER.readValue(is, type);
    }

    /**
     * Missing field exception thrown when a field is missing while read a JSON tree.
     */
    private static final class MissingFieldException extends JsonProcessingException {

        /**
         * Create a new missing field exception.
         *
         * @param fieldName
         */
        MissingFieldException(String fieldName) {
            super("Missing required field: " + fieldName);
        }
    }

    private static String requiredTextField(JsonNode node, String fieldName) throws MissingFieldException {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asText();
        }
        throw new MissingFieldException(fieldName);
    }

    private static String optionalTextField(JsonNode node, String fieldName) throws MissingFieldException {
        if (node.has(fieldName)) {
            return node.get(fieldName).asText(null);
        }
        throw new MissingFieldException(fieldName);
    }

    private static int requiredIntField(JsonNode node, String fieldName) throws MissingFieldException {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asInt();
        }
        throw new MissingFieldException(fieldName);
    }

    private static long requiredLongField(JsonNode node, String fieldName) throws MissingFieldException {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asLong();
        }
        throw new MissingFieldException(fieldName);
    }

    private static PipelineInfo readPipelineInfo(JsonNode node) throws MissingFieldException {
        PipelineInfo.Builder builder = PipelineInfo.builder()
                .id(requiredTextField(node, "id"))
                .repositoryUrl(requiredTextField(node, "repositoryUrl"))
                .title(requiredTextField(node, "title"))
                .headRef(requiredTextField(node, "headRef"))
                .commit(requiredTextField(node, "commit"))
                .mergeCommit(optionalTextField(node, "mergeCommit"))
                .status(readStatus(node))
                .timings(readTimings(node));
        if (node.hasNonNull("headRefUrl")) {
            builder.headRefUrl(node.get("headRefUrl").asText());
        }
        if (node.hasNonNull("commitUrl")) {
            builder.commitUrl(node.get("commitUrl").asText());
        }
        if (node.hasNonNull("mergeCommitUrl")) {
            builder.mergeCommitUrl(node.get("mergeCommitUrl").asText());
        }
        if (node.hasNonNull("user")) {
            builder.user(node.get("user").asText());
        }
        if (node.hasNonNull("userUrl")) {
            builder.userUrl(node.get("userUrl").asText());
        }
        return builder.build();
    }

    private static void writePipelineInfo(PipelineInfo info, JsonGenerator generator) throws IOException {
        generator.writeFieldName("id");
        generator.writeString(info.id);
        generator.writeFieldName("title");
        generator.writeString(info.title);
        generator.writeFieldName("repositoryUrl");
        generator.writeString(info.repositoryUrl);
        generator.writeFieldName("headRef");
        generator.writeString(info.headRef);
        generator.writeFieldName("headRefUrl");
        generator.writeString(info.headRefUrl);
        generator.writeFieldName("commit");
        generator.writeString(info.commit);
        generator.writeFieldName("commitUrl");
        generator.writeString(info.commitUrl);
        generator.writeFieldName("mergeCommit");
        generator.writeString(info.mergeCommit);
        generator.writeFieldName("mergeCommitUrl");
        generator.writeString(info.mergeCommitUrl);
        generator.writeFieldName("user");
        generator.writeString(info.user);
        generator.writeFieldName("userUrl");
        generator.writeString(info.userUrl);
        generator.writeFieldName("status");
        generator.writeString(info.status());
        generator.writeFieldName("date");
        generator.writeString(info.timings.date);
        generator.writeFieldName("duration");
        generator.writeNumber(info.duration());
    }

    private static Pipeline readPipeline(JsonNode node) throws MissingFieldException {
        PipelineInfo info = readPipelineInfo(node);
        Pipeline pipeline = new Pipeline(info);
        pipeline.error = optionalTextField(node, "error");

        // depth first traversal
        LinkedList<JsonNode> stack = new LinkedList<>();
        if (node.has("items")) {
            for (JsonNode stage : node.get("items")) {
                stack.add(stage);
            }
        }
        Node parent = pipeline;
        while (!stack.isEmpty()) {
            JsonNode stageNode = stack.peek();
            String stageId = requiredTextField(stageNode, "id");
            String stageType = requiredTextField(stageNode, "type");
            if ("STEPS".equals(stageType)) {
                // tree leaf
                Steps steps = readSteps(stageNode, stageId, (Stages) parent);
                if (stageNode.hasNonNull("children")) {
                    for (JsonNode stepNode : stageNode.get("children")) {
                        steps.addStep(readStep(stepNode, steps));
                    }
                }
                stack.pop();
            } else {
                // tree node
                if (stageId.equals(parent.id)) {
                    // leaving a node (2nd pass)
                    parent = parent.parent;
                    stack.pop();
                } else {
                    // entering a node

                    // create the stage
                    Stages stage;
                    switch (stageType) {
                        case "SEQUENCE":
                            stage = readSequence(stageNode, stageId, (Stages) parent);
                            break;
                        case "PARALLEL":
                            stage = readParallel(stageNode, stageId, (Stages) parent);
                            break;
                        default:
                            throw new IllegalStateException("Unknown type: " + stageType);
                    }

                    // add nested stages on the stack
                    if (stageNode.hasNonNull("children")) {
                        JsonNode childrenNode = stageNode.get("children");
                        int size = childrenNode.size();
                        if (size > 0) {
                            for (int i=size-1 ; i >= 0 ; i--) {
                                stack.push(childrenNode.get(i));
                            }
                            parent = stage;
                            continue;
                        }
                    }
                    // one pass only
                    stack.pop();
                }
            }
        }
        return pipeline;
    }

    private static void writePipeline(Pipeline pipeline, JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        writePipelineInfo(pipeline.info, generator);
        generator.writeFieldName("error");
        generator.writeString(pipeline.error);
        generator.writeFieldName("items");
        generator.writeStartArray();
        for (Stage child : pipeline.children) {
            generator.writeObject(child);
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private static Step readStep(JsonNode node, Steps parent) throws MissingFieldException {
        return new Step(requiredTextField(node, "id"), parent, requiredTextField(node, "name"), requiredTextField(node, "args"),
                /* meta */ false, /* declared */ true, readStatus(node), readTimings(node));
    }

    private static Steps readSteps(JsonNode node, String id, Stages parent) throws MissingFieldException {
        Steps steps = new Steps(parent, id, readStatus(node), readTimings(node));
        steps.artifacts = requiredIntField(node, "artifacts");
        if (node.hasNonNull("tests")) {
            steps.tests = readTestsInfo(node.get("tests"));
        }
        parent.addStage(steps);
        return steps;
    }

    private static TestsInfo readTestsInfo(JsonNode node) throws MissingFieldException {
        return new TestsInfo(requiredIntField(node, "total"), requiredIntField(node, "passed"), requiredIntField(node, "failed"),
                requiredIntField(node, "skipped"));
    }

    private static Sequence readSequence(JsonNode node, String id, Stages parent) throws MissingFieldException {
        Sequence sequence = new Sequence(parent, id, requiredTextField(node, "name"), readStatus(node), readTimings(node));
        parent.addStage(sequence);
        return sequence;
    }

    private static Parallel readParallel(JsonNode node, String id, Stages parent) throws MissingFieldException {
        Parallel parallel = new Parallel(parent, id, requiredTextField(node, "name"), readStatus(node), readTimings(node));
        parent.addStage(parallel);
        return parallel;
    }

    private static Status readStatus(JsonNode node) throws MissingFieldException {
        return Status.valueOf(requiredTextField(node, "status"));
    }

    private static Timings readTimings(JsonNode node) throws MissingFieldException {
        return new Timings(requiredTextField(node, "date"), requiredLongField(node, "duration"));
    }
}
