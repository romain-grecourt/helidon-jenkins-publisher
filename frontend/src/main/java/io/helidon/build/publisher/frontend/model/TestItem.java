package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Test item model.
 */
public final class TestItem {

    final String name;
    final String status;
    final String output;

    private TestItem(String name, String status, String output) {
        this.name = name;
        this.status = status;
        this.output = output;
    }

    @JsonProperty
    public String name() {
        return name;
    }

    @JsonProperty
    public String status() {
        return status;
    }

    @JsonProperty
    public String output() {
        return output;
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
    public static final class Builder implements io.helidon.common.Builder<TestItem> {

        private String name;
        private String status;
        private String output;

        /**
         * Set the name.
         * @param name the name
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
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
         * Set the output.
         * @param output the output
         * @return this builder instance
         */
        public Builder output(String output) {
            this.output = output;
            return this;
        }

        @Override
        public TestItem build() {
            return new TestItem(name, status, output);
        }
    }
}
