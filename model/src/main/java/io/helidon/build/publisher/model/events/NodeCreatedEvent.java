package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Abstract node created event.
 */
public abstract class NodeCreatedEvent extends PipelineEvent {

    final int id;
    final int parentId;
    final int index;
    final String name;
    final long startTime;

    /**
     * Create a new node created event.
     *
     * @param runId run id
     * @param id node id
     * @param parentId node parent id
     * @param index index in the parent node
     * @param name node name, may be {@code null}
     * @param startTime start timestamp
     */
    NodeCreatedEvent(String runId, int id, int parentId, int index, String name, long startTime) {
        super(runId);
        this.id = id;
        this.parentId = parentId;
        this.index = index;
        this.name = name;
        this.startTime = startTime;
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
     * Get the parent node id.
     *
     * @return int
     */
    @JsonProperty
    public final int parentId() {
        return parentId;
    }

    /**
     * Get the parent index.
     *
     * @return int
     */
    @JsonProperty
    public final int index() {
        return index;
    }

    /**
     * Get the name of the created node.
     *
     * @return String
     */
    @JsonProperty
    public final String name() {
        return name;
    }

    /**
     * Get the start timestamp.
     *
     * @return long
     */
    @JsonProperty
    public final long startTime() {
        return startTime;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + this.id;
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
        final NodeCreatedEvent other = (NodeCreatedEvent) obj;
        if (!Objects.equals(this.runId, other.runId)) {
            return false;
        }
        if (this.id != other.id) {
            return false;
        }
        if (this.parentId != other.parentId) {
            return false;
        }
        if (this.index != other.index) {
            return false;
        }
        if (this.startTime != other.startTime) {
            return false;
        }
        return Objects.equals(this.name, other.name);
    }
}
