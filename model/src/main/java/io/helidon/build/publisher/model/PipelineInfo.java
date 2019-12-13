package io.helidon.build.publisher.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Pipeline info.
 */
public final class PipelineInfo {

    final String jobName;
    final String repositoryUrl;
    final String scmHead;
    final String scmHash;
    final String id;

    // TODO date with zone info
    // TODO author
    // TODO status
    // TODO result

    /**
     * Create a new pipeline run.
     *
     * @param id the run id
     * @param repositoryUrl the repository URL, may be {@code null}
     * @param jobName the job name, must be a valid {@code String}
     * @param scmHead the branch name that this run was triggered against, may be {@code null}
     * @param scmHash the GIT commit id that this run was triggered against, may be {@code null}
     * @throws IllegalArgumentException if jobName is not a valid {@code String}
     * @throws NullPointerException if pipeline {@code null}
     */
    @JsonCreator
    public PipelineInfo(@JsonProperty("id") String id, @JsonProperty("repositoryUrl") String repositoryUrl,
            @JsonProperty("jobName") String jobName, @JsonProperty("scmHead") String scmHead,
            @JsonProperty("scmHash") String scmHash) {

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id is null or empty");
        }
        if (jobName == null || jobName.isEmpty()) {
            throw new IllegalArgumentException("job name is null or empty");
        }
        this.id = id;
        this.jobName = jobName;
        this.repositoryUrl = repositoryUrl;
        this.scmHead = scmHead;
        this.scmHash = scmHash;
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
     * Get the repository URL.
     * @return String
     */
    @JsonProperty
    public String repositoryUrl() {
        return repositoryUrl;
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
     * Get the GIT commit id for this run.
     * @return String
     */
    @JsonProperty
    public String scmHash() {
        return scmHash;
    }

    @Override
    public String toString() {
        return PipelineInfo.class.getSimpleName() + "{"
                + " id=" + id
                + ", jobName=" + jobName
                + ", repositoryUrl=" + repositoryUrl
                + ", scmHead=" + scmHead
                + ", scmHash=" + scmHash
                + ", scmHead=" + scmHead
                + " }";
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
