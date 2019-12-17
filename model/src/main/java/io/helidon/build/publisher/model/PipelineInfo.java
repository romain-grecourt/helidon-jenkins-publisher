package io.helidon.build.publisher.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Pipeline info.
 */
@JsonDeserialize(using = PipelineInfoDeserializer.class)
@JsonPropertyOrder({"id", "name", "gitRepositoryUrl", "gitHead", "gitCommit"})
public final class PipelineInfo {

    final String id;
    final String name;
    final String gitRepositoryUrl;
    final String gitHead;
    final String gitCommit;
    final Status status;
    final Timings timings;

    /**
     * Create a new pipeline info.
     *
     * @param id the pipeline id
     * @param name the pipeline name, must be a valid {@code String}
     * @param gitRepositoryUrl the repository URL, may be {@code null}
     * @param gitHead the ref name that this pipeline was triggered against, may be {@code null}
     * @param gitCommit the GIT commit id that this pipeline was triggered against, may be {@code null}
     * @param status the pipeline status
     * @param timings the pipeline timings
     * @throws IllegalArgumentException if name is not a valid {@code String}
     * @throws NullPointerException if pipeline {@code null}
     */
    public PipelineInfo(String id, String name, String gitRepositoryUrl, String gitHead, String gitCommit, Status status,
            Timings timings) {

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id is null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        this.id = id;
        this.name = name;
        this.gitRepositoryUrl = gitRepositoryUrl;
        this.gitHead = gitHead;
        this.gitCommit = gitCommit;
        this.status = Objects.requireNonNull(status, "status is null!");
        this.timings = Objects.requireNonNull(timings, "timings is null!");
    }

    /**
     * Get the pipeline id.
     * @return String
     */
    @JsonProperty
    public String id() {
        return id;
    }

    /**
     * Get the pipeline name.
     * @return String
     */
    @JsonProperty
    public String name() {
        return name;
    }

    /**
     * Get the GIT repository URL.
     * @return String
     */
    @JsonProperty
    public String gitRepositoryUrl() {
        return gitRepositoryUrl;
    }

    /**
     * Get the GIT head for this pipeline.
     * @return String
     */
    @JsonProperty
    public String gitHead() {
        return gitHead;
    }

    /**
     * Get the GIT commit id for this pipeline.
     * @return String
     */
    @JsonProperty
    public String gitCommit() {
        return gitCommit;
    }

    @Override
    public String toString() {
        return PipelineInfo.class.getSimpleName() + "{"
                + " id=" + id
                + ", name=" + name
                + ", gitRepositoryUrl=" + gitRepositoryUrl
                + ", gitHead=" + gitHead
                + ", gitCommit=" + gitCommit
                + " }";
    }

    /**
     * Get the start timestamp.
     *
     * @return String
     */
    @JsonProperty
    public final String date() {
        return timings.date;
    }

    /**
     * Get the duration in seconds.
     *
     * @return long
     */
    @JsonProperty
    public final long duration() {
        return timings.endTime > timings.startTime? (timings.endTime - timings.startTime) / 1000 : 0;
    }

    /**
     * Get the state.
     *
     * @return State
     */
    @JsonProperty
    public final Status.State state() {
        return status.state;
    }

    /**
     * Get the result.
     *
     * @return Result
     */
    @JsonProperty
    public final Status.Result result() {
        return status.result;
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
        final PipelineInfo other = (PipelineInfo) obj;
        return Objects.equals(this.id, other.id);
    }
}
