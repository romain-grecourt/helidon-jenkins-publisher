package io.helidon.build.publisher.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineEventListener;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Graph node.
 */
public abstract class Node {

    final PipelineInfo info;
    final Map<String, Node> nodesByIds;
    final AtomicInteger nextId;
    final Node parent;
    final String id;
    final String name;
    final String path;
    final Status status;
    final Timings timings;
    final LinkedList<PipelineEventListener> listeners;

    /**
     * Create a new non parented node.
     * @param info pipeline info
     * @throws NullPointerException if info is {@code null}
     */
    protected Node(PipelineInfo info) {
        this.info = Objects.requireNonNull(info, "info is null");
        this.status = info.status;
        this.timings = info.timings;
        this.listeners = new LinkedList<>();
        this.parent = null;
        this.name = "pipeline";
        this.path = "/";
        this.id = "0";
        this.nextId = new AtomicInteger(1);
        this.nodesByIds = new HashMap<>();
        if (nodesByIds.containsKey(id)) {
            throw new IllegalArgumentException("Id already used, id=" + id);
        }
        this.nodesByIds.put(id, this);
    }

    /**
     * Create a new parented node.
     * @param parent parent node, must not be {@code null}
     * @param id node id, must not be used in the parent graph
     * @param name node name, may be {@code null}
     * @param path path, if {@code null}, defaults to the path of the parent
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    protected Node(Node parent, String id, String name, String path, Status status, Timings timings) {
        this.parent = Objects.requireNonNull(parent, "parent is null");
        this.status = Objects.requireNonNull(status, "status is null");
        this.timings = Objects.requireNonNull(timings, "timings is null");
        this.info = parent.info;
        this.listeners = parent.listeners;
        this.nodesByIds = parent.nodesByIds;
        this.nextId = parent.nextId;
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid id");
        }
        if (nodesByIds.containsKey(id)) {
            throw new IllegalArgumentException("Id already used, id=" + id);
        }
        this.id = id;
        this.name = name;
        this.path = path != null ? path : parent.path;
        this.nodesByIds.put(id, this);
    }

    /**
     * Create a new graph node.
     *
     * @param parent enclosing node, must be non {@code null}
     * @param name stage name, may be {@code null}
     * @param path path, if {@code null}, defaults to the path of the parent
     * @param status the status object
     * @param timings the timings object
     * @throws IllegalArgumentException if path does not start with a {@code /}
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    protected Node(Node parent, String name, String path, Status status, Timings timings) {
        this(parent, createId(parent), name, path, status, timings);
    }

    private static String createId(Node parent) {
        Objects.requireNonNull(parent, "parent is null");
        return String.valueOf(parent.nextId.getAndIncrement());
    }

    /**
     * Get the pipeline id.
     * @return String
     */
    public final String pipelineId() {
        return info.id;
    }

    /**
     * Get the pipeline info.
     * @return PipelineInfo
     */
    public final PipelineInfo pipelineInfo() {
        return info;
    }

    /**
     * Get the parent node;
     *
     * @return Node or {@code null} if this is the root node
     */
    public Node parent() {
        return parent;
    }

    /**
     * Get the unique id.
     *
     * @return String
     */
    @JsonProperty
    public final String id() {
        return id;
    }

    /**
     * Get the step name.
     *
     * @return String
     */
    @JsonProperty
    public String name() {
        return name;
    }

    /**
     * Get the step name.
     *
     * @return String
     */
    public String path() {
        return path;
    }

    /**
     * Get the start timestamp.
     *
     * @return long
     */
    @JsonProperty
    public final long startTime() {
        return timings.startTime;
    }

    /**
     * Get the end timestamp.
     *
     * @return long
     */
    @JsonProperty
    public final long endTime() {
        return timings.endTime;
    }

    /**
     * Get the state.
     *
     * @return State
     */
    @JsonProperty
    public final Status.State state() {
        return status.state;
    }

    /**
     * Get the result.
     *
     * @return Result
     */
    @JsonProperty
    public final Status.Result result() {
        return status.result;
    }

    /**
     * Fire a created event.
     */
    public abstract void fireCreated();

    /**
     * Fire a completed event.
     */
    public abstract void fireCompleted();

    /**
     * Get the previous Node in the parent node.
     *
     * @return Node or {@code null} if there is no previous Node.
     */
    public abstract Node previous();

    /**
     * Get the next Node in the parent node.
     *
     * @return Node or {@code null} if there is no previous Node.
     */
    public abstract Node next();

    /**
     * Get the index in the parent node.
     *
     * @return index or -1 if the node has no parent or the node is not found in the parent
     */
    public abstract int index();

    /**
     * Test if this node is the head of its parent.
     *
     * @return {@code true} if this node is the head, {@code false} otherwise
     */
    public abstract boolean head();

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Node other = (Node) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public final String toString() {
        return this.getClass().getSimpleName() + " {"
                + " pipelineId=" + info.id
                + ", id=" + id
                + parent == null ? "" : (", parentId=" +  parent.id)
                + " }";
    }

    protected final void fireEvent(PipelineEvent event) {
        for (PipelineEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
