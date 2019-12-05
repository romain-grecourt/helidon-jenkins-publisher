package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Artifact item model.
 */
public abstract class ArtifactItem {

    final String name;

    protected ArtifactItem(String name) {
        this.name = name;
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
