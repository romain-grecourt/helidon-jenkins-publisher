package io.helidon.build.publisher.model;

import java.util.Objects;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.helidon.build.publisher.model.PipelineEvents.PipelineCompleted;
import io.helidon.build.publisher.model.PipelineEvents.PipelineCreated;

/**
 * Pipeline run.
 */
@JsonDeserialize(using = PipelineRun.Deserializer.class)
public final class PipelineRun {

    final String jobName;
    final String scmHead;
    final String scmHash;
    final String id;
    final Pipeline pipeline;

    /**
     * Create a new pipeline run from a {@link PipelineEvents.EventType#RUN_CREATED} event.
     * @param runCreated event
     */
    public PipelineRun(PipelineEvents.PipelineCreated runCreated) {
        this(runCreated.runId, runCreated.jobName, runCreated.scmHead, runCreated.scmHash,
                new Pipeline(runCreated.runId, new Status(), new Timings(runCreated.startTime)));
    }

    /**
     * Create a new pipeline run.
     *
     * @param id the run id
     * @param jobName the job name, must be a valid {@code String}
     * @param scmHead the branch name that this run was triggered against, must be a valid {@code String}
     * @param scmHash the GIT commit id that this run was triggered against, must be a valid {@code String}
     * @param pipeline the pipeline, must be non {@code null}
     * @throws IllegalArgumentException if jobName, scmHead or scmHash are not valid {@code String}
     * @throws NullPointerException if pipeline {@code null}
     */
    public PipelineRun(String id, String jobName, String scmHead, String scmHash, Pipeline pipeline) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id is null or empty");
        }
        if (jobName == null || jobName.isEmpty()) {
            throw new IllegalArgumentException("job name is null or empty");
        }
        if (scmHead == null || scmHead.isEmpty()) {
            throw new IllegalArgumentException("scmHead is null or empty");
        }
        if (scmHash == null || scmHash.isEmpty()) {
            throw new IllegalArgumentException("scmHash is null or empty");
        }
        this.id = id;
        this.jobName = jobName;
        this.scmHead = scmHead;
        this.scmHash = scmHash;
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline is null!");
    }

    /**
     * Get the ID for this run.
     * @return String
     */
    @JsonProperty
    public String id() {
        return id;
    }

    /**
     * Get the job name.
     * @return String
     */
    @JsonProperty
    public String jobName() {
        return jobName;
    }

    /**
     * Get the branch name for this run.
     * @return String
     */
    @JsonProperty
    public String scmHead() {
        return scmHead;
    }

    /**
     * Get the pipeline.
     * @return Pipeline
     */
    @JsonProperty
    public Pipeline pipeline() {
        return pipeline;
    }

    /**
     * Get the GIT commit id for this run.
     * @return String
     */
    @JsonProperty
    public String scmHash() {
        return scmHash;
    }

    @Override
    public String toString() {
        return PipelineRun.class.getSimpleName() + "{"
                + " id=" + id
                + ", jobName=" + jobName
                + ", scmHead=" + scmHead
                + ", scmHash=" + scmHash
                + ", scmHead=" + scmHead
                + " }";
    }

    /**
     * Pretty print the pipeline.
     *
     * @param declaredOnly include the declared steps only
     * @param skipMeta skip the meta steps
     * @return String
     */
    public String prettyPrint(boolean declaredOnly, boolean skipMeta) {
        return pipeline.prettyPrint("", declaredOnly, declaredOnly);
    }

    /**
     * Fire a created event.
     */
    public void fireCreated() {
        if (pipeline != null) {
            pipeline.listener.onEvent(new PipelineCreated(id, jobName, scmHead, scmHash, pipeline.sequence.timings.startTime));
        }
    }

    /**
     * Fire a completed event.
     */
    public void fireCompleted() {
        if (pipeline != null) {
            pipeline.sequence.fireCompleted();
            pipeline.listener.onEvent(new PipelineCompleted(id));
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PipelineRun other = (PipelineRun) obj;
        return Objects.equals(this.id, other.id);
    }

    /**
     * Custom {@link Deserializer} for {@link PipelineRun}.
     */
    public static final class Deserializer extends StdDeserializer<PipelineRun> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public PipelineRun deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            String id = node.get("id").asText();
            String jobName = node.get("jobName").asText();
            String scmHead = node.get("scmHead").asText();
            String scmHash = node.get("scmHash").asText();
            Pipeline pipeline = Pipeline.readPipeline(node.get("pipeline"), id);
            return new PipelineRun(id, jobName, scmHead, scmHash, pipeline);
        }
    }
}