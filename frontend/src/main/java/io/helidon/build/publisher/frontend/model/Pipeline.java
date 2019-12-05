package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline model.
 */
public final class Pipeline {

    final String id;
    final String status;
    final String title;
    final String repository;
    final String repositoryUrl;
    final String branch;
    final String branchUrl;
    final String commit;
    final String commitUrl;
    final String author;
    final String authorUrl;
    final String startTime;
    final String duration;
    final List<PipelineStageItem> items;

    private Pipeline(String id, String status, String title, String repository, String repositoryUrl, String branch,
            String branchUrl, String commit, String commitUrl, String author, String authorUrl, String startTime,
            String duration, List<PipelineStageItem> items) {

        this.id = id;
        this.status = status;
        this.title = title;
        this.repository = repository;
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.branchUrl = branchUrl;
        this.commit = commit;
        this.commitUrl = commitUrl;
        this.author = author;
        this.authorUrl = authorUrl;
        this.startTime = startTime;
        this.duration = duration;
        this.items = items;
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
    public String commit() {
        return commit;
    }

    @JsonProperty
    public String commitUrl() {
        return commitUrl;
    }

    @JsonProperty
    public String author() {
        return author;
    }

    @JsonProperty
    public String authorUrl() {
        return authorUrl;
    }

    @JsonProperty
    public String startTime() {
        return startTime;
    }

    @JsonProperty
    public String duration() {
        return duration;
    }

    @JsonProperty
    public List<PipelineStageItem> items() {
        return items;
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
    public static final class Builder implements io.helidon.common.Builder<Pipeline> {

        private String id;
        private String status;
        private String title;
        private String repository;
        private String repositoryUrl;
        private String branch;
        private String branchUrl;
        private String commit;
        private String commitUrl;
        private String author;
        private String authorUrl;
        private String startTime;
        private String duration;
        private final List<PipelineStageItem.Builder> itemBuilders = new ArrayList<>();

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
         * Set the commit.
         *
         * @param commit the commit
         * @return this builder instance
         */
        public Builder commit(String commit) {
            this.commit = commit;
            return this;
        }

        /**
         * Set the commitUrl.
         * @param commitUrl the commitUrl
         * @return this builder instance
         */
        public Builder commitUrl(String commitUrl) {
            this.commitUrl = commitUrl;
            return this;
        }

        /**
         * Set the author.
         *
         * @param author the author
         * @return this builder instance
         */
        public Builder author(String author) {
            this.author = author;
            return this;
        }

        /**
         * Set the authorUrl.
         *
         * @param authorUrl the authorUrl
         * @return this builder instance
         */
        public Builder authorUrl(String authorUrl) {
            this.authorUrl = authorUrl;
            return this;
        }

        /**
         * Set the startTime.
         *
         * @param startTime the startTime
         * @return this builder instance
         */
        public Builder startTime(String startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Set the duration.
         * @param duration the duration
         * @return this builder instance
         */
        public Builder duration(String duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Add an item.
         *
         * @param itemBuilder the item
         * @return this builder instance
         */
        public Builder item(PipelineStageItem.Builder itemBuilder) {
            this.itemBuilders.add(itemBuilder);
            return this;
        }

        @Override
        public Pipeline build() {
            List<PipelineStageItem> items = new ArrayList<>();
            for (PipelineStageItem.Builder itemBuilder : itemBuilders) {
                items.add(itemBuilder.build());
            }
            return new Pipeline(id, status, title, repository, repositoryUrl, branch, branchUrl, commit, commitUrl, author,
                    authorUrl, startTime, duration, items);
        }
    }
}
