package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Pipeline stage item model.
 */
public final class PipelineStageItem extends PipelineItem {

    final Tests tests;
    final Artifacts artifacts;
    final List<PipelineItem> children;

    protected PipelineStageItem(int id, boolean parallel, String name, List<PipelineItem> children, Tests tests, Artifacts artifacts) {
        super(id, name, parallel ? Type.PARALLEL : Type.SEQUENCE);
        this.children = children;
        this.tests = tests;
        this.artifacts = artifacts;
    }

    @JsonProperty
    public Tests tests() {
        return tests;
    }

    @JsonProperty
    public Artifacts artifacts() {
        return artifacts;
    }

    @JsonProperty
    public List<PipelineItem> children() {
        return children;
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

        private boolean parallel;
        private Tests tests;
        private Artifacts artifacts;
        private final List<PipelineItem.Builder> childrenBuilder = new ArrayList<>();

        /**
         * Set the parallel.
         *
         * @param parallel the parallel
         * @return this builder instance
         */
        public Builder parallel(boolean parallel) {
            this.parallel = parallel;
            return this;
        }

        /**
         * Set the tests.
         *
         * @param tests the tests
         * @return this builder instance
         */
        public Builder tests(Tests tests) {
            this.tests = tests;
            return this;
        }

        /**
         * Set the artifacts.
         *
         * @param artifacts the artifacts
         * @return this builder instance
         */
        public Builder artifacts(Artifacts artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        /**
         * Add a child.
         *
         * @param childBuilder the child
         * @return this builder instance
         */
        public Builder child(PipelineItem.Builder childBuilder) {
            this.childrenBuilder.add(childBuilder);
            return this;
        }

        @Override
        public PipelineStageItem build() {
            List<PipelineItem> children = new ArrayList<>();
            for (PipelineItem.Builder childBuilder : childrenBuilder) {
                children.add(childBuilder.build());
            }
            return new PipelineStageItem(id, parallel, name, children, tests, artifacts);
        }
    }
}
