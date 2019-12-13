package io.helidon.build.publisher.model;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.helidon.build.publisher.model.events.StageCreatedEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A stage node.
 */
public abstract class Stage extends Node {

    final StageType type;

    /**
     * Create a new non parented stage.
     *
     * @param info pipeline info
     * @param type the stage type, must not be {@code null}
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if info, status or timings is {@code null}
     */
    protected Stage(PipelineInfo info, StageType type, Status status, Timings timings) {
        super(info, status, timings);
        this.type = Objects.requireNonNull(type, "type is null");
    }

    /**
     * Create a new stage.
     *
     * @param type the stage type, must not be {@code null}
     * @param parent the parent stage that this step is part of
     * @param name the step name, must be non {@code null} and non empty
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    protected Stage(StageType type, Node parent, String name, Status status, Timings timings) {
        super(parent, name, createPath(parent, type, name), status, timings);
        this.type = Objects.requireNonNull(type, "type is null");
    }

    /**
     * Create a new stage.
     *
     * @param type the stage type, must not be {@code null}
     * @param id node id, must not be used in the parent graph
     * @param parent parent node, must not be {@code null}
     * @param name node name, may be {@code null}
     * @param path path, if {@code null}, defaults to the path of the parent
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    protected Stage(StageType type, int id, Node parent, String name, String path, Status status, Timings timings) {
        super(id, parent, name, path, status, timings);
        this.type = Objects.requireNonNull(type, "type is null");
    }

    /**
     * Create a new stage.
     *
     * @param type stage type, must not be {@code null}
     * @param id node id, must not be used in the parent graph
     * @param parent parent node, must not be {@code null}
     * @param name node name, may be {@code null}
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    protected Stage(StageType type, int id, Node parent, String name, Status status, Timings timings) {
        this(type, id, parent, name, createPath(parent, type, name), status, timings);
    }

    /**
     * Create a stage path.
     *
     * @param prefix path prefix
     * @param parallel {@code true} if the parent is a parallel stage
     * @param name the stage name
     * @return String
     */
    public static String createPath(String prefix, boolean parallel, String name) {
        if (parallel) {
            return prefix + "parallel/" + name + "/";
        }
        return prefix + "stage[" + name + "]/";
    }

    private static String createPath(Node parent, Stage.StageType stageType, String name) {
        Objects.requireNonNull(parent, "parent is null");
        if (stageType == StageType.SEQUENCE) {
            return createPath(parent.path, parent instanceof Parallel, name == null ? "" : name);
        }
        return parent.path;
    }

    /**
     * The type of stages.
     */
    public enum StageType {
        STEPS,
        SEQUENCE,
        PARALLEL
    }

    /**
     * Get the stage type.
     *
     * @return Type
     */
    @JsonProperty
    public final StageType type() {
        return type;
    }

    @Override
    public Stage previous() {
        if (parent != null) {
            List<Stage> sequence = ((Stages) parent).children;
            int previousIndex = index() - 1;
            if (previousIndex >= 0) {
                return sequence.get(previousIndex);
            }
        }
        return null;
    }

    @Override
    public Stage next() {
        if (parent != null) {
            List<Stage> sequence = ((Stages) parent).children;
            int nextIndex = index() + 1;
            if (nextIndex > 0 && nextIndex < sequence.size()) {
                return sequence.get(nextIndex);
            }
        }
        return null;
    }

    @Override
    public int index() {
        if (parent != null) {
            Iterator<Stage> it = ((Stages) parent).children.iterator();
            for (int i = 0; it.hasNext(); i++) {
                if (it.next().id == id) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Test if this stage is the head of its parent.
     *
     * @return {@code true} if this stage is the head, {@code false} otherwise
     */
    @Override
    public boolean head() {
        if (parent != null) {
            List<Stage> sequence = ((Stages) parent).children;
            return index() == sequence.size() - 1;
        }
        return true;
    }

    @Override
    public void fireCreated() {
        Stage previous = previous();
        if (previous != null) {
            previous.fireCompleted();
        }
        int parentId = parent == null ? -1 : parent.id;
        fireEvent(new StageCreatedEvent(info.id, id, parentId, index(), name, timings.startTime, type));
    }
}
