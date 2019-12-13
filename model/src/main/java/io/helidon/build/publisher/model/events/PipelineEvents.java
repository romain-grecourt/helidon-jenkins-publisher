package io.helidon.build.publisher.model.events;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pipeline events.
 */
public final class PipelineEvents {

    final List<PipelineEvent> events;

    /**
     * Create a new pipeline events.
     * @param events events
     */
    public PipelineEvents(@JsonProperty("events") List<PipelineEvent> events) {
        this.events = new LinkedList<>(events);
    }

    /**
     * Get the events.
     * @return {@code List<PipelineEvent>}
     */
    @JsonProperty
    public List<PipelineEvent> events() {
        return events;
    }
}
