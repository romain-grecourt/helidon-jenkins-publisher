package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Artifact item model.
 */
public abstract class ArtifactItem {

    final String name;
    final String path;

    protected ArtifactItem(String name, String path) {
        this.name = name;
        this.path = path;
    }

    @JsonProperty
    public String path() {
        return path;
    }

    @JsonProperty
    public String name() {
        return name;
    }

    /**
     * Builder class.
     */
    public static abstract class Builder implements io.helidon.common.Builder<ArtifactItem> {

        protected String name;
        protected String path;

        /**
         * Set the name.
         * @param name the name
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
    }
}
