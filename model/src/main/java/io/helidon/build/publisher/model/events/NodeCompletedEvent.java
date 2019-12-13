package io.helidon.build.publisher.model.events;

import java.util.Objects;

import io.helidon.build.publisher.model.Status;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Abstract node completed event.
 */
public abstract class NodeCompletedEvent extends PipelineEvent {

    final PipelineEventType type;
    final int id;
    final Status.Result result;
    final long endTime;

    /**
     * Create a new completed node event.
     *
     * @param runId run id
     * @param id node id
     * @param state node state
     * @param result node result
     * @param endTime node end timestamp
     */
    NodeCompletedEvent(String runId, PipelineEventType type, int id, Status.Result result, long endTime) {
        super(runId);
        this.type = type;
        this.id = id;
        this.result = result;
        this.endTime = endTime;
    }

    @JsonProperty
    @Override
    public final PipelineEventType eventType() {
        return type;
    }

    /**
     * Get the node id.
     *
     * @return int
     */
    @JsonProperty
    public final int id() {
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
     * Get the end timestamp.
     *
     * @return long
     */
    @JsonProperty
    public final long endTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + " runId=" + runId
                + ", id=" + id
                + ", result=" + result
                + ", endTime=" + endTime
                + " }";
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.type);
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
        if (this.id != other.id) {
            return false;
        }
        if (this.endTime != other.endTime) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return this.result == other.result;
    }
}
