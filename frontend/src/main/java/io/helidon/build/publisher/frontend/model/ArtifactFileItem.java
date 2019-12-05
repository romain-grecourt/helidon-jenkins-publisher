package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Artifact file item model.
 */
public final class ArtifactFileItem extends ArtifactItem {

    final String file;

    private ArtifactFileItem(String name, String file) {
        super(name);
        this.file = file;
    }

    @JsonProperty
    public String file() {
        return file;
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
    public static final class Builder extends ArtifactItem.Builder {

        protected String file;

        /**
         * Set the file.
         * @param file the file
         * @return this builder instance
         */
        public Builder file(String file) {
            this.file= file;
            return this;
        }

        @Override
        public ArtifactFileItem build() {
            return new ArtifactFileItem(name, file);
        }
    }
}
