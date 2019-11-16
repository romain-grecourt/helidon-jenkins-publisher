package io.helidon.build.publisher.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pipeline run.
 */
public final class PipelineRun {

    final String jobName;
    final String scmHead;
    final String scmHash;
    final String id;
    final Pipeline pipeline;

    /**
     * Create a new pipeline run from a {@link FlowEvents.EventType#RUN_CREATED} event.
     * @param runCreated event
     */
    public PipelineRun(PipelineEvents.PipelineCreated runCreated) {
        this(runCreated.runId, runCreated.jobName, runCreated.scmHead, runCreated.scmHash,
                new Pipeline(new Status(runCreated.state), new Timings(runCreated.startTime)));
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
     * @throws IllegalArgumentException if buildNumber or not a positive value
     * @throws NullPointerException if pipeline {@code null}
     */
    @JsonCreator
    public PipelineRun(@JsonProperty("id") String id, @JsonProperty("jobName") String jobName,
            @JsonProperty("scmHead") String scmHead, @JsonProperty("scmHash") String scmHash,
            @JsonProperty("pipeline") Pipeline pipeline) {

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
        this.pipeline = pipeline;
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
     * @return Flow
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
     * Fire an event.
     *
     * @param listener consumer of the event
     * @param nodeEventType the node event type
     * @param runId the run id
     */
    public void fireEvent(PipelineEvents.EventListener listener, PipelineEvents.NodeEventType nodeEventType, String runId) {
        pipeline.fireEvent(listener, nodeEventType, runId, this);
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
}
