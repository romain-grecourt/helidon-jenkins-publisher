package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Abstract node created event.
 */
public abstract class NodeCreatedEvent extends PipelineEvent {

    final String id;
    final String parentId;
    final int index;
    final String name;
    final long startTime;

    /**
     * Create a new node created event.
     *
     * @param pipelineId pipeline id
     * @param id node id
     * @param parentId node parent id
     * @param index index in the parent node
     * @param name node name, may be {@code null}
     * @param startTime start timestamp
     */
    NodeCreatedEvent(String pipelineId, String id, String parentId, int index, String name, long startTime) {
        super(pipelineId);
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
    public final String id() {
        return id;
    }

    /**
     * Get the parent node id.
     *
     * @return int
     */
    @JsonProperty
    public final String parentId() {
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
        final NodeCreatedEvent other = (NodeCreatedEvent) obj;
        if (!Objects.equals(this.pipelineId, other.pipelineId)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

}
