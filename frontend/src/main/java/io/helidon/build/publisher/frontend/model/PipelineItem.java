package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * Pipeline item model.
 */
public abstract class PipelineItem {

    public static enum Type {
        SEQUENCE,
        PARALLEL,
        STEP
    }

    final int id;
    final String name;
    final Type type;

    protected PipelineItem(int id, String name, Type type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @JsonProperty
    public String name() {
        return name;
    }

    @JsonProperty
    public int id() {
        return id;
    }

    @JsonProperty
    public Type type() {
        return type;
    }

    /**
     * Builder class.
     */
    public static abstract class Builder implements io.helidon.common.Builder<PipelineItem> {

        protected int id;
        protected String name;
        protected Type type;

        /**
         * Set the id.
         * @param id the id
         * @return this builder instance
         */
        public Builder id(int id) {
            this.id = id;
            return this;
        }

        /**
         * Set the name.
         *
         * @param name the name
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
    }
}
