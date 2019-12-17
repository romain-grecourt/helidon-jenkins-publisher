package io.helidon.build.publisher.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List of {@link PipelineInfo}.
 */
public final class PipelineInfos {

    final List<PipelineInfo> items;
    final int pagenum;
    final int totalpages;

    @JsonCreator
    public PipelineInfos(@JsonProperty("pipelines") List<PipelineInfo> items, int pagenum, int totalpages) {
        this.items = items;
        this.pagenum = pagenum;
        this.totalpages = totalpages;
    }

    @JsonProperty
    public List<PipelineInfo> items() {
        return items;
    }

    @JsonProperty
    public int pagenum() {
        return pagenum;
    }

    @JsonProperty
    public int totalpages() {
        return totalpages;
    }
}
