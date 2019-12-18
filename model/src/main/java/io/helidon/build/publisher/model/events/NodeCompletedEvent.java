package io.helidon.build.publisher.model.events;

import java.util.Objects;

import io.helidon.build.publisher.model.Status;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Abstract node completed event.
 */
public abstract class NodeCompletedEvent extends PipelineEvent {

    final PipelineEventType type;
    final String id;
    final Status.Result result;
    final long duration;

    /**
     * Create a new completed node event.
     *
     * @param pipelineId pipeline id
     * @param id node id
     * @param state node state
     * @param result node result
     * @param duration node duration
     */
    NodeCompletedEvent(String pipelineId, PipelineEventType type, String id, Status.Result result, long duration) {
        super(pipelineId);
        this.type = type;
        this.id = id;
        this.result = result;
        this.duration = duration;
    }

    @JsonProperty
    @Override
    public final PipelineEventType eventType() {
        return type;
    }

    /**
     * Get the node id.
     *
     * @return String
     */
    @JsonProperty
    public final String id() {
        return id;
    }

    /**
     * Get the result.
     *
     * @return Status.Result
     */
    @JsonProperty
    public final Status.Result result() {
        return result;
    }

    /**
     * Get the duration in seconds.
     *
     * @return long
     */
    @JsonProperty
    public final long duration() {
        return duration;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", id=" + id
                + ", result=" + result
                + ", duration=" + duration
                + " }";
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.pipelineId);
        hash = 83 * hash + Objects.hashCode(this.id);
        hash = 83 * hash + Objects.hashCode(eventType());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeCompletedEvent other = (NodeCompletedEvent) obj;
        if (!Objects.equals(this.pipelineId, other.pipelineId)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }
}
