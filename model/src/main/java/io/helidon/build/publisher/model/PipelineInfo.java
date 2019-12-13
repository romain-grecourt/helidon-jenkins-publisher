package io.helidon.build.publisher.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Pipeline info.
 */
@JsonPropertyOrder({"id", "title", "repositoryUrl", "scmHead", "scmHash"})
public final class PipelineInfo {

    final String id;
    final String title;
    final String repositoryUrl;
    final String scmHead;
    final String scmHash;

    /**
     * Create a new pipeline run.
     *
     * @param id the run id
     * @param title the job name, must be a valid {@code String}
     * @param repositoryUrl the repository URL, may be {@code null}
     * @param scmHead the branch name that this run was triggered against, may be {@code null}
     * @param scmHash the GIT commit id that this run was triggered against, may be {@code null}
     * @throws IllegalArgumentException if title is not a valid {@code String}
     * @throws NullPointerException if pipeline {@code null}
     */
    @JsonCreator
    public PipelineInfo(@JsonProperty("id") String id, @JsonProperty("title") String title,
            @JsonProperty("repositoryUrl") String repositoryUrl, @JsonProperty("scmHead") String scmHead,
            @JsonProperty("scmHash") String scmHash) {

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id is null or empty");
        }
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("job name is null or empty");
        }
        this.id = id;
        this.title = title;
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
    public String title() {
        return title;
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
                + ", title=" + title
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
