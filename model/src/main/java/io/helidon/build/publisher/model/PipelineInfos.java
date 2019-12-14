package io.helidon.build.publisher.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List of {@link PipelineInfo}.
 */
public final class PipelineInfos {

    final List<PipelineInfo> pipelineInfos;

    @JsonCreator
    public PipelineInfos(@JsonProperty("pipelines") List<PipelineInfo> pipelineInfos) {
        this.pipelineInfos = pipelineInfos;
    }

    @JsonProperty
    public List<PipelineInfo> pipelines() {
        return pipelineInfos;
    }
}
