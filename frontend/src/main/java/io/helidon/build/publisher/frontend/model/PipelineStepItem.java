package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Pipeline step item model.
 */
public final class PipelineStepItem extends PipelineItem {

    final String status;

    protected PipelineStepItem(int id, String name, String status) {
        super(id, name, Type.STEP);
        this.status = status;
    }

    @JsonProperty
    public String status() {
        return status;
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
    public static final class Builder extends PipelineItem.Builder {

        private String status;

        /**
         * Set the status.
         *
         * @param status the status
         * @return this builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        @Override
        public PipelineStepItem build() {
            return new PipelineStepItem(id, name, status);
        }
    }
}
