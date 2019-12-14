package io.helidon.build.publisher.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Pipeline info.
 */
@JsonDeserialize(using = PipelineInfoDeserializer.class)
@JsonPropertyOrder({"id", "name", "repositoryUrl", "scmHead", "scmHash"})
public final class PipelineInfo {

    final String id;
    final String name;
    final String repositoryUrl;
    final String scmHead;
    final String scmHash;

    /**
     * Create a new pipeline info.
     *
     * @param id the pipeline id
     * @param name the pipeline name, must be a valid {@code String}
     * @param repositoryUrl the repository URL, may be {@code null}
     * @param scmHead the ref name that this pipeline was triggered against, may be {@code null}
     * @param scmHash the GIT commit id that this pipeline was triggered against, may be {@code null}
     * @throws IllegalArgumentException if name is not a valid {@code String}
     * @throws NullPointerException if pipeline {@code null}
     */
    public PipelineInfo(String id, String name, String repositoryUrl, String scmHead, String scmHash) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id is null or empty");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        this.id = id;
        this.name = name;
        this.repositoryUrl = repositoryUrl;
        this.scmHead = scmHead;
        this.scmHash = scmHash;
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
     * Get the repository URL.
     * @return String
     */
    @JsonProperty
    public String repositoryUrl() {
        return repositoryUrl;
    }

    /**
     * Get the branch name for this pipeline.
     * @return String
     */
    @JsonProperty
    public String scmHead() {
        return scmHead;
    }

    /**
     * Get the GIT commit id for this pipeline.
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
                + ", name=" + name
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
