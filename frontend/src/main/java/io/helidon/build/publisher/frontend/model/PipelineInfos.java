package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipelines info model.
 */
public final class PipelineInfos {

    final int page;
    final int numpages;
    final List<PipelineInfo> pipelines;

    private PipelineInfos(int page, int numpages, List<PipelineInfo> pipelineInfos) {
        this.page = page;
        this.numpages = numpages;
        this.pipelines = pipelineInfos;
    }

    @JsonProperty
    public int page() {
        return page;
    }

    @JsonProperty
    public int numpages() {
        return numpages;
    }

    @JsonProperty
    public List<PipelineInfo> pipelines() {
        return pipelines;
    }

    /**
     * Create a new builder.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder implements io.helidon.common.Builder<PipelineInfos> {

        private int page;
        private int numpages;
        private final List<PipelineInfo> pipelines = new ArrayList<>();

        /**
         * Set the page.
         * @param page the page
         * @return this builder instance
         */
        public Builder page(int page) {
            this.page = page;
            return this;
        }

        /**
         * Set the numpages.
         * @param numpages the numpages
         * @return this builder instance
         */
        public Builder numpages(int numpages) {
            this.numpages = numpages;
            return this;
        }

        /**
         * Add the pipelines.
         *
         * @param pipeline the pipeline
         * @return this builder instance
         */
        public Builder pipeline(PipelineInfo pipeline) {
            this.pipelines.add(pipeline);
            return this;
        }

        @Override
        public PipelineInfos build() {
            return new PipelineInfos(page, numpages, pipelines);
        }
    }
}
