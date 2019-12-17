package io.helidon.build.publisher.model;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import io.helidon.build.publisher.model.Stage.StageType;
import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Status.State;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import static io.helidon.build.publisher.model.Stage.StageType.PARALLEL;
import static io.helidon.build.publisher.model.Stage.StageType.SEQUENCE;

/**
 * Jackson deserializer for {@link Pipeline}.
 */
public final class PipelineDeserializer extends StdDeserializer<Pipeline> {

    /**
     * Create a new deserializer instance.
     */
    public PipelineDeserializer() {
        this(Pipeline.class);
    }

    /**
     * Create a new deserializer instance.
     * @param vc value class
     */
    public PipelineDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Pipeline deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        PipelineInfo info = PipelineInfoDeserializer.readPipelineInfo(node);
        Pipeline pipeline = new Pipeline(info);

        // depth first traversal
        LinkedList<JsonNode> stack = new LinkedList<>();
        for (JsonNode stage : node.get("items")) {
            stack.add(stage);
        }
        Node parent = pipeline;
        while (!stack.isEmpty()) {
            JsonNode stageNode = stack.peek();
            String stageId = stageNode.get("id").asText();
            StageType stageType = StageType.valueOf(stageNode.get("type").asText());
            if (stageType == StageType.STEPS) {
                // tree leaf
                Steps steps = readSteps(stageNode, stageId, (Stages) parent);
                for (JsonNode stepNode : stageNode.get("children")) {
                    steps.addStep(readStep(stepNode, steps));
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
                        case SEQUENCE:
                            stage = readSequence(stageNode, stageId, (Stages) parent);
                            break;
                        case PARALLEL:
                            stage = readParallel(stageNode, stageId, (Stages) parent);
                            break;
                        default:
                            throw new IllegalStateException("Unknown type: " + stageType);
                    }

                    // add nested stages on the stack
                    JsonNode childrenNode = stageNode.get("children");
                    if (childrenNode.size() > 0) {
                        Iterator<JsonNode> nestedStagesIt = childrenNode.elements();
                        while (nestedStagesIt.hasNext()) {
                            stack.push(nestedStagesIt.next());
                        }
                        parent = stage;
                    } else {
                        // one pass only
                        stack.pop();
                    }
                }
            }
        }
        return pipeline;
    }

    private static Step readStep(JsonNode node, Steps parent) {
        return new Step(node.get("id").asText(), parent, node.get("name").asText(), node.get("args").asText(), /* meta */ false,
                /* declared */ true, readStatus(node), readTimings(node));
    }

    private static Steps readSteps(JsonNode node, String id, Stages parent) {
        Steps steps = new Steps(parent, id, readStatus(node), readTimings(node));
        steps.artifacts = node.get("artifacts").asInt(0);
        if (node.hasNonNull("tests")) {
            steps.tests = readTestsInfo(node.get("tests"));
        }
        parent.addStage(steps);
        return steps;
    }

    private static TestsInfo readTestsInfo(JsonNode node) {
        return new TestsInfo(node.get("total").asInt(0), node.get("passed").asInt(0), node.get("failed").asInt(0),
                node.get("skipped").asInt(0));
    }

    private static Sequence readSequence(JsonNode node, String id, Stages parent) {
        Sequence sequence = new Sequence(parent, id, node.get("name").asText(), readStatus(node), readTimings(node));
        parent.addStage(sequence);
        return sequence;
    }

    private static Parallel readParallel(JsonNode node, String id, Stages parent) {
        Parallel parallel = new Parallel(parent, id, node.get("name").asText(), readStatus(node), readTimings(node));
        parent.addStage(parallel);
        return parallel;
    }

    static Status readStatus(JsonNode node) {
        String state = node.get("state").asText(null);
        String result = node.get("result").asText(null);
        return new Status(State.valueOf(state), result != null ? Result.valueOf(result) : null);
    }

    static Timings readTimings(JsonNode node) {
        if (node.hasNonNull("date")) {
            return new Timings(node.get("date").asText(), node.get("duration").asLong(0));
        } else {
            return new Timings(node.get("startTime").asLong(-1), node.get("endTime").asLong(-1));
        }
    }
}
