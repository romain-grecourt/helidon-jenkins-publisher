package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pipeline info model.
 */
public final class PipelineInfo {

    final String id;
    final String status;
    final String title;
    final String repository;
    final String repositoryUrl;
    final String branch;
    final String branchUrl;
    final String when;

    private PipelineInfo(String id, String status, String title, String repository, String repositoryUrl,
            String branch, String branchUrl, String when) {

        this.id = id;
        this.status = status;
        this.title = title;
        this.repository = repository;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.branchUrl = branchUrl;
        this.when = when;
    }

    @JsonProperty
    public String id() {
        return id;
    }

    @JsonProperty
    public String status() {
        return status;
    }

    @JsonProperty
    public String title() {
        return title;
    }

    @JsonProperty
    public String repository() {
        return repository;
    }

    @JsonProperty
    public String repositoryUrl() {
        return repositoryUrl;
    }

    @JsonProperty
    public String branch() {
        return branch;
    }

    @JsonProperty
    public String branchUrl() {
        return branchUrl;
    }

    @JsonProperty
    public String when() {
        return when;
    }

    /**
     * Create a new builder.
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder implements io.helidon.common.Builder<PipelineInfo> {

        private String id;
        private String status;
        private String title;
        private String repository;
        private String repositoryUrl;
        private String branch;
        private String branchUrl;
        private String when;

        /**
         * Set the id.
         * @param id the id
         * @return this builder instance
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the status.
         * @param status the status
         * @return this builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Set the title.
         * @param title the title
         * @return this builder instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the repository.
         * @param repository the repository
         * @return this builder instance
         */
        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        /**
         * Set the repositoryUrl.
         * @param repositoryUrl the repositoryUrl
         * @return this builder instance
         */
        public Builder repositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        /**
         * Set the branch.
         * @param branch the branch
         * @return this builder instance
         */
        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        /**
         * Set the branchUrl.
         * @param branchUrl the branchUrl
         * @return this builder instance
         */
        public Builder branchUrl(String branchUrl) {
            this.branchUrl = branchUrl;
            return this;
        }

        /**
         * Set the when.
         * @param when the when
         * @return this builder instance
         */
        public Builder when(String when) {
            this.when = when;
            return this;
        }

        @Override
        public PipelineInfo build() {
            return new PipelineInfo(id, status, title, repository, repositoryUrl, branch, branchUrl, when);
        }
    }
}
