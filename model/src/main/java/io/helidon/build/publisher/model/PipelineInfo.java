package io.helidon.build.publisher.model;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Pipeline info.
 */
@JsonDeserialize(using = JacksonSupport.PipelineInfoDeserializer.class)
@JsonSerialize(using = JacksonSupport.PipelineInfoSerializer.class)
public final class PipelineInfo {

    final String id;
    String title;
    String repositoryUrl;
    String headRef;
    String headRefUrl;
    final String commit;
    String commitUrl;
    final String mergeCommit;
    String mergeCommitUrl;
    String user;
    String userUrl;
    final Status status;
    final Timings timings;

    private PipelineInfo(String id, String title, String repositoryUrl, String headRef, String headRefUrl, String commit,
            String commitUrl, String mergeCommit, String mergeCommitUrl, String user, String userUrl, Status status,
            Timings timings) {

        this.id = nonNullNonEmpty(id, "is is null or empty!");
        this.title = nonNullNonEmpty(title, "title is null or empty!");;
        this.repositoryUrl = repositoryUrl;
        this.headRef = headRef;
        this.headRefUrl = headRefUrl;
        this.commit = commit;
        this.commitUrl = commitUrl;
        this.mergeCommit = mergeCommit;
        this.mergeCommitUrl = mergeCommitUrl;
        this.user = user;
        this.userUrl = userUrl;
        this.status = Objects.requireNonNull(status, "status is null!");
        this.timings = Objects.requireNonNull(timings, "timings is null!");
    }

    /**
     * Get the pipeline id.
     * @return String
     */
    public String id() {
        return id;
    }

    /**
     * Get the title.
     * @return String
     */
    public String title() {
        return title;
    }

    /**
     * Set the title.
     * @param title the title
     * @throws IllegalArgumentException if title is {@code null} or empty
     */
    public void title(String title) {
        this.title = nonNullNonEmpty(title, "title is null! or empty");
    }

    /**
     * Get the repository URL.
     * @return String
     */
    public String repositoryUrl() {
        return repositoryUrl;
    }

    /**
     * Set the repository URL.
     * @param repositoryUrl repository URL
     * @throws IllegalArgumentException if repositoryUrl is {@code null} or empty
     */
    public void repositoryUrl(String repositoryUrl) {
        this.repositoryUrl = nonNullNonEmpty(repositoryUrl, "repositoryUrl is null or empty!");
    }

    /**
     * Get the head ref.
     * @return String
     */
    public String headRef() {
        return headRef;
    }

    /**
     * Set the head ref.
     * @param headRef head ref string
     * @throws IllegalArgumentException if head is {@code null} or empty
     */
    public void headRef(String headRef){
        this.headRef = nonNullNonEmpty(headRef, "headRef is null or empty!");
    }

    /**
     * Get the head ref URL.
     *
     * @return String, may be {@code null}
     */
    public String headRefUrl() {
        return headRefUrl;
    }

    /**
     * Set the head ref URL.
     * @param headRefUrl URL string
     * @throws IllegalArgumentException if headRefUrl is {@code null} or empty
     */
    public void headRefUrl(String headRefUrl){
        this.headRefUrl = nonNullNonEmpty(headRefUrl, "headRefUrl is null or empty!");
    }

    /**
     * Get the commit id.
     * @return String
     */
    public String commit() {
        return commit;
    }

    /**
     * Get the commit URL.
     *
     * @return String, may be {@code null}
     */
    public String commitUrl() {
        return commitUrl;
    }

    /**
     * Set the commit URL.
     *
     * @param commitUrl URL string
     * @throws IllegalArgumentException if commitUrl is {@code null} or empty
     */
    public void commitUrl(String commitUrl) {
        this.commitUrl = nonNullNonEmpty(commitUrl, "commitUrl is null or empty!");
    }

    /**
     * Get the merge commit id.
     * @return String, may be {@code null}
     */
    public String mergeCommit() {
        return mergeCommit;
    }

    /**
     * Get the merge commit URL.
     *
     * @return String, may be {@code null}
     */
    public String mergeCommitUrl() {
        return mergeCommitUrl;
    }

    /**
     * Set the merge commit URL.
     *
     * @param mergeCommitUrl URL string
     * @throws IllegalArgumentException if mergeCommitUrl is {@code null} or empty
     */
    public void mergeCommitUrl(String mergeCommitUrl) {
        this.mergeCommitUrl = nonNullNonEmpty(mergeCommitUrl, "mergeCommitUrl is null or empty!");
    }

    /**
     * Get the user.
     *
     * @return String, may be {@code null}
     */
    public String user() {
        return user;
    }

    /**
     * Set the user.
     *
     * @param user user string
     * @throws IllegalArgumentException if user is {@code null} or empty
     */
    public void user(String user) {
        this.user = nonNullNonEmpty(user, "user is null or empty!");
    }

    /**
     * Get the user URL.
     *
     * @return String, may be {@code null}
     */
    public String userUrl() {
        return userUrl;
    }

    /**
     * Set the user URL.
     * @param userUrl URL string
     * @throws IllegalArgumentException if userUrl is {@code null} or empty
     */
    public void userUrl(String userUrl){
        this.userUrl = nonNullNonEmpty(userUrl, "userUrl is null or empty!");
    }

    /**
     * Get the start timestamp.
     *
     * @return String
     */
    public String date() {
        return timings.date;
    }

    /**
     * Get the duration in seconds.
     *
     * @return long
     */
    public long duration() {
        return timings.duration();
    }

    /**
     * Get the state.
     *
     * @return State
     */
    public String status() {
        return status.toString();
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

    @Override
    public String toString() {
        return PipelineInfo.class.getSimpleName() + "{"
                + " id=" + id
                + ", title=" + title
                + ", repositoryUrl=" + repositoryUrl
                + ", headRef=" + headRef
                + ", headRefUrl=" + headRefUrl
                + ", commit=" + commit
                + ", commitUrl=" + commitUrl
                + ", user=" + user
                + ", userUrl=" + userUrl
                + " }";
    }

    /**
     * Create a new builder.
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for {@link PipelineInfo}.
     */
    public static final class Builder {

        private String id;
        private String title;
        private String repositoryUrl;
        private String headRef;
        private String headRefUrl;
        private String commit;
        private String commitUrl;
        private String mergeCommit;
        private String mergeCommitUrl;
        private String user;
        private String userUrl;
        private Status status;
        private Timings timings;

        /**
         * Set the pipeline id.
         * @param id pipeline id
         * @return this builder instance
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the title.
         * @param title title
         * @return this builder instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the repository URL.
         * @param repositoryUrl repository URL
         * @return this builder instance
         */
        public Builder repositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        /**
         * Set the head ref.
         * @param headRef head ref
         * @return this builder instance
         */
        public Builder headRef(String headRef) {
            this.headRef = headRef;
            return this;
        }

        /**
         * Set the head ref URL.
         * @param headRefUrl head ref URL
         * @return this builder instance
         */
        public Builder headRefUrl(String headRefUrl) {
            this.headRefUrl = headRefUrl;
            return this;
        }

        /**
         * Set the commit id.
         *
         * @param commit commit id
         * @return this builder instance
         */
        public Builder commit(String commit) {
            this.commit = commit;
            return this;
        }

        /**
         * Set the commit id URL.
         *
         * @param commitUrl commit id URL
         * @return this builder instance
         */
        public Builder commitUrl(String commitUrl) {
            this.commitUrl = commitUrl;
            return this;
        }

        /**
         * Set the merge commit id.
         *
         * @param mergeCommit merge commit id
         * @return this builder instance
         */
        public Builder mergeCommit(String mergeCommit) {
            this.mergeCommit = mergeCommit;
            return this;
        }

        /**
         * Set the merge commit URL.
         *
         * @param mergeCommitUrl merge commit URL
         * @return this builder instance
         */
        public Builder mergeCommitUrl(String mergeCommitUrl) {
            this.mergeCommitUrl = mergeCommitUrl;
            return this;
        }

        /**
         * Set the user.
         *
         * @param user user
         * @return this builder instance
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Set the user URL.
         *
         * @param userUrl user URL
         * @return this builder instance
         */
        public Builder userUrl(String userUrl) {
            this.userUrl = userUrl;
            return this;
        }

        /**
         * Set the status.
         *
         * @param status status
         * @return this builder instance
         */
        public Builder status(Status status) {
            Objects.requireNonNull(status, "status is null");
            this.status = status;
            return this;
        }

        /**
         * Set the timings.
         *
         * @param timings timings
         * @return this builder instance
         */
        public Builder timings(Timings timings) {
            Objects.requireNonNull(timings, "timings is null");
            this.timings = timings;
            return this;
        }

        /**
         * Build the {@link PipelineInfo} instance.
         * @return PipelineInfo
         * @throws NullPointerException if status or timings is {@code null}
         * @throws IllegalArgumentException if id or title are not valid strings
         */
        public PipelineInfo build() {
            return new PipelineInfo(id, title, repositoryUrl, headRef, headRefUrl, commit, commitUrl, mergeCommit, mergeCommitUrl,
                    user, userUrl, status, timings);
        }
    }

    /**
     * Validate a string.
     *
     * @param s string to validate
     * @param name name of the attribute that is validated
     * @return String
     */
    private static String nonNullNonEmpty(String s, String message) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return s;
    }
}
