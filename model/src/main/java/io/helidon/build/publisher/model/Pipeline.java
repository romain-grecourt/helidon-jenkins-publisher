package io.helidon.build.publisher.model;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.PipelineEvents.Event;
import io.helidon.build.publisher.model.PipelineEvents.EventListener;
import io.helidon.build.publisher.model.PipelineEvents.EventType;
import io.helidon.build.publisher.model.PipelineEvents.NodeCompletedEvent;
import io.helidon.build.publisher.model.PipelineEvents.StageCompleted;
import io.helidon.build.publisher.model.PipelineEvents.StageCreated;
import io.helidon.build.publisher.model.PipelineEvents.StepCompleted;
import io.helidon.build.publisher.model.PipelineEvents.StepCreated;
import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Status.State;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import static io.helidon.build.publisher.model.PipelineEvents.EventType.PIPELINE_COMPLETED;
import static io.helidon.build.publisher.model.PipelineEvents.EventType.STAGE_COMPLETED;
import static io.helidon.build.publisher.model.PipelineEvents.EventType.STAGE_CREATED;
import static io.helidon.build.publisher.model.PipelineEvents.EventType.STEP_COMPLETED;
import static io.helidon.build.publisher.model.PipelineEvents.EventType.STEP_CREATED;
import static io.helidon.build.publisher.model.Pipeline.Stage.StageType.PARALLEL;
import static io.helidon.build.publisher.model.Pipeline.Stage.StageType.SEQUENCE;
import static io.helidon.build.publisher.model.Pipeline.Stage.StageType.STEPS;

/**
 * Pipeline model.
 */
public final class Pipeline {

    private static final Logger LOGGER = Logger.getLogger(Pipeline.class.getName());

    final Sequence sequence;
    final EventListener listener;
    final String runId;

    /**
     * Create a new pipeline instance.
     *
     * @param runId pipeline run id
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if status or timings is {@code null}
     */
    public Pipeline(String runId, Status status, Timings timings) {
        this(runId, status, timings, PipelineEvents.NOOP_LISTENER);
    }

    /**
     * Create a new pipeline instance.
     *
     * @param status the status object
     * @param timings the timings object
     * @param listener event listener
     * @param runId pipeline run id
     * @throws NullPointerException if status or timings is {@code null}
     */
    public Pipeline(String runId, Status status, Timings timings, EventListener listener) {
        this.runId = runId;
        this.listener = listener;
        this.sequence = new Sequence(status, timings, listener, runId);
    }

    /**
     * Get the pipeline top-level sequence stage.
     * @return Sequence
     */
    @JsonProperty
    public Sequence stages() {
        return sequence;
    }

    /**
     * Pretty print the graph.
     *
     * @param indent indentation, must not be {@code null}
     * @param excludeSyntheticSteps if {@code true} the steps that are not declared are filtered out
     * @param excludeMetaSteps if {@code true} the meta steps are filtered out
     * @return String
     * @throws NullPointerException if indent is {@code null}
     */
    public final String prettyPrint(String indent, boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
        Objects.requireNonNull(indent, "indent is null");
        StringBuilder sb = new StringBuilder(indent);
        sb.append("pipeline {\n");
        indent += "  ";
        LinkedList<Stage> stack = new LinkedList<>(sequence.stages);
        int parentId = 0;
        while (!stack.isEmpty()) {
            Stage stage = stack.peek();
            if (stage instanceof Steps) {
                // leaf
                sb.append(((Steps) stage).prettyPrint(indent, excludeSyntheticSteps, excludeMetaSteps));
                parentId = stage.parent.id;
                stack.pop();
            } else if (stage instanceof Stages) {
                // node
                if (parentId == stage.id) {
                    // leaving (2nd pass)
                    indent = indent.substring(2);
                    sb.append(indent).append("}\n");
                    parentId = stage.parent.id;
                    stack.pop();
                } else {
                    // entering
                    sb.append(indent);
                    if (stage instanceof Parallel) {
                        sb.append("parallel {");
                    } else {
                        sb.append("stage");
                        if (stage.name != null && !stage.name.isEmpty()) {
                            sb.append("('").append(stage.name).append("')");
                        }
                        sb.append(" {");
                    }
                    List<Stage> children = ((Stages) stage).stages;
                    if (!children.isEmpty()) {
                        sb.append("\n");
                        // process children
                        indent += "  ";
                        for (int i = children.size() - 1; i >= 0; i--) {
                            stack.push(children.get(i));
                        }
                    } else {
                        // one pass only
                        sb.append(" }\n");
                        parentId = stage.parent.id;
                        stack.pop();
                    }
                }
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Apply the given events.
     * @param events the events to apply
     */
    public void applyEvents(List<Event> events) {
        for (Event event : events) {
            EventType eventType = event.eventType();
            if (sequence.status.state == State.FINISHED && eventType != EventType.PIPELINE_COMPLETED) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Skipping event, pipeline is in FINISHED state: {0}", event);
                }
                continue;
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Applying event: {0}", event);
            }
            switch (eventType) {
                case PIPELINE_COMPLETED:
                    if (sequence.status.state != State.FINISHED) {
                        sequence.status.state = State.FINISHED;
                        sequence.status.result = Result.UNKNOWN;
                    }
                    break;
                case STEP_COMPLETED:
                case STAGE_COMPLETED:
                    NodeCompletedEvent ncpt = (NodeCompletedEvent) event;
                    Node node = sequence.nodesByIds.get(ncpt.id);
                    if (node == null) {
                        throw new IllegalStateException("Unkown node, id=" + ncpt.id);
                    }
                    node.status.state = State.FINISHED;
                    node.status.result = ncpt.result;
                    node.timings.endTime = ncpt.endTime;
                    break;
                case STEP_CREATED:
                    StepCreated spcrt = (StepCreated) event;
                    Node stepParent = sequence.nodesByIds.get(spcrt.parentId);
                    if (stepParent == null) {
                        throw new IllegalStateException("Unkown node, id=" + spcrt.parentId);
                    }
                    if (!(stepParent instanceof Steps)) {
                        throw new IllegalStateException("Invalid step parent node");
                    }
                    if (spcrt.index != ((Steps) stepParent).steps.size()) {
                        throw new IllegalStateException("Invalid index");
                    }
                    ((Steps) stepParent).addStep(new Step(spcrt.id, (Steps) stepParent, spcrt.name, spcrt.path, spcrt.args,
                            spcrt.meta, spcrt.declared, new Status(), new Timings(spcrt.startTime)));
                    break;
                case STAGE_CREATED:
                    StageCreated sgcrt = (StageCreated) event;
                    Node stageParent = sequence.nodesByIds.get(sgcrt.parentId);
                    if (stageParent == null) {
                        throw new IllegalStateException("Unkown node, id=" + sgcrt.parentId);
                    }
                    if (!(stageParent instanceof Stages)) {
                        throw new IllegalStateException("Invalid stage parent node");
                    }
                    if (sgcrt.index != ((Stages) stageParent).stages.size()) {
                        throw new IllegalStateException("Invalid index");
                    }
                    switch (sgcrt.stageType) {
                        case PARALLEL:
                            ((Stages) stageParent).addStage(new Parallel(sgcrt.id, stageParent, sgcrt.name, sgcrt.path,
                                    new Status(), new Timings(sgcrt.startTime)));
                            break;
                        case SEQUENCE:
                            ((Stages) stageParent).addStage(new Sequence(sgcrt.id, stageParent, sgcrt.name, sgcrt.path,
                                    new Status(), new Timings(sgcrt.startTime)));
                            break;
                        case STEPS:
                            ((Stages) stageParent).addStage(new Steps(sgcrt.id, stageParent, new Status(),
                                    new Timings(sgcrt.startTime)));
                            break;
                        default:
                        // do nothing
                    }
                    break;
                default:
                // do nothing
            }
        }
    }

    /**
     * Graph node.
     */
    @JsonPropertyOrder({"id", "name", "path", "state", "result", "startTime", "endTime"})
    public static abstract class Node {

        final Map<Integer, Node> nodesByIds;
        final AtomicInteger nextId;
        final Node parent;
        final int id;
        final String name;
        final String path;
        final Status status;
        final Timings timings;
        final EventListener listener;
        final String runId;

        private Node(Status status, Timings timings, EventListener listener, String runId) {
            this.status = Objects.requireNonNull(status, "status is null");
            this.timings = Objects.requireNonNull(timings, "timings is null");
            this.listener = Objects.requireNonNull(listener, "listener is null");
            this.runId = Objects.requireNonNull(runId, "runId is null");
            this.parent = null;
            this.name = null;
            this.path = "/";
            this.id = 0;
            this.nextId = new AtomicInteger(1);
            this.nodesByIds = new HashMap<>();
            this.nodesByIds.put(id, this);
        }

        private Node(int id, Node parent, String name, String path, Status status, Timings timings) {
            this.status = Objects.requireNonNull(status, "status is null");
            this.timings = Objects.requireNonNull(timings, "timings is null");
            this.parent = Objects.requireNonNull(parent, "parent is null");
            this.listener = parent.listener;
            this.runId = parent.runId;
            this.nodesByIds = parent.nodesByIds;
            this.nextId = parent.nextId;
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid id: " + id);
            }
            if (nodesByIds.containsKey(id)) {
                throw new IllegalArgumentException("Id already used, id=" + id);
            }
            this.id = id;
            this.name = name;
            if (path != null && !path.startsWith("/")) {
                throw new IllegalArgumentException("Invalid node path: " + path);
            }
            this.path = path != null ? path : parent.path;
            this.nodesByIds.put(id, this);
        }

        /**
         * Create a new graph node.
         *
         * @param parent enclosing node, must be non {@code null}
         * @param name stage name, may be {@code null}
         * @param path stage path, must be a valid {@code String} starting with a {@code /}
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if parent, status or timings is {@code null}
         */
        Node(Node parent, String name, String path, Status status, Timings timings) {
            this(Objects.requireNonNull(parent, "parent is null").nextId.getAndIncrement(), parent, name, path, status, timings);
        }

        /**
         * Get the unique id.
         *
         * @return String
         */
        @JsonProperty
        public final int id() {
            return id;
        }

        /**
         * Get the parent node;
         * @return Node or {@code null} if this is the root node
         */
        public Node parent() {
            return parent;
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
        @JsonProperty
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
        public final State state() {
            return status.state;
        }

        /**
         * Get the result.
         *
         * @return Result
         */
        @JsonProperty
        public final Result result() {
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
         * @return index or -1 if the node has no parent or the node is not found in the parent
         */
        public abstract int index();

        /**
         * Test if this node is the head of its parent.
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
                    + " id=" + id
                    + ", parentId=" + (parent == null ? -1 : parent.id)
                    + " }";
        }
    }

    /**
     * A stage node.
     */
    public static abstract class Stage extends Node {

        final StageType type;

        private Stage(StageType type, Status status, Timings timings, EventListener listener, String runId) {
            super(status, timings, listener, runId);
            this.type = type;
        }

        private Stage(StageType type, int id, Node parent, String name, String path, Status status, Timings timings) {
            super(id, parent, name, path, status, timings);
            this.type = type;
        }

        /**
         * Create a new stage.
         *
         * @param type the stage type
         * @param parent the parent stage that this step is part of
         * @param name the step name, must be non {@code null} and non empty
         * @param status the status object
         * @param timings the timings object
         * @throws NullPointerException if type, status or timings is {@code null}
         */
        Stage(StageType type, Node parent, String name, Status status, Timings timings) {
            super(parent, name, createPath(parent, type, name), status, timings);
            this.type = Objects.requireNonNull(type, "type is null");
        }

        /**
         * Create a stage path.
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
                List<Stage> sequence = ((Stages)parent).stages;
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
                List<Stage> sequence = ((Stages)parent).stages;
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
                Iterator<Stage> it = ((Stages) parent).stages.iterator();
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
         * @return {@code true} if this stage is the head, {@code false} otherwise
         */
        @Override
        public boolean head() {
            if (parent != null) {
                List<Stage> sequence = ((Stages)parent).stages;
                return index() == sequence.size() - 1;
            }
            return true;
        }

        @Override
        public void fireCreated() {
            Pipeline.Stage previous = previous();
            if (previous != null) {
                previous.fireCompleted();
            }
            int parentId = parent == null ? -1 : parent.id;
            listener.onEvent(new StageCreated(runId, id, parentId, index(), name, path, timings.startTime, type));
        }
    }

    /**
     * A steps stage.
     */
    public static final class Steps extends Stage {

        final List<Step> steps = new LinkedList<>();

        private Steps(int id, Node parent, Status status, Timings timings) {
            super(StageType.STEPS, id, parent, null, Objects.requireNonNull(parent, "parent").path, status, timings);
        }

        /**
         * Create a new steps stage.
         * @param parent the parent stage that this step is part of
         * @param status the status object
         * @param timings the timings object
         * @throws NullPointerException if type, status or timings is {@code null}
         */
        public Steps(Node parent, Status status, Timings timings) {
            super(StageType.STEPS, parent, null, status, timings);
        }

        @JsonIgnore // steps is synthetic, it doesn't have a name
        @Override
        public String name() {
            return null;
        }

        @JsonIgnore // steps is synthetic, its path is the parent path
        @Override
        public String path() {
            return path;
        }

        /**
         * Get the steps in sequence order.
         *
         * @return immutable list of {@link Step}
         */
        @JsonProperty
        public List<Step> steps() {
            return Collections.unmodifiableList(steps);
        }

        /**
         * Add a nested step.
         *
         * @param step to add
         */
        public void addStep(Step step) {
            steps.add(step);
        }

        private String prettyPrint(String indent, boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("steps {\n");
            indent += "  ";
            for (Step step : steps) {
                sb.append(step.name).append(" ").append(step.args);
                if (!step.isIncluded(excludeSyntheticSteps, excludeMetaSteps)) {
                    sb.append(" // filtered");
                }
                sb.append("\n");
            }
            indent = indent.substring(2);
            sb.append(indent).append("}\n");
            return sb.toString();
        }

        @Override
        public void fireCompleted() {
            Step last = null;
            if (!steps.isEmpty()) {
                last = steps.get(steps.size() - 1);
                last.fireCompleted();
            }
            status.state = State.FINISHED;
            status.result = last != null ? last.result() : Result.SUCCESS;
            timings.endTime = last != null ? last.endTime() : System.currentTimeMillis();
            listener.onEvent(new StageCompleted(runId, id, status.result, timings.endTime));
        }
    }

    /**
     * A step node.
     */
    public static final class Step extends Node {

        final String args;
        final boolean meta;
        final boolean declared;

        private Step(int id, Steps parent, String name, String path, String args, boolean meta, boolean declared,
                Status status, Timings timings) {

            super(id, parent, name, path, status, timings);
            this.args = args != null ? args : "";
            this.meta = meta;
            this.declared = declared;
        }

        /**
         * Create a new step instance.
         *
         * @param parent the parent stage that this step is part of
         * @param name the step name, must be non {@code null} and non empty
         * @param args the step arguments
         * @param meta {@code true} if this step is a meta step
         * @param declared {@code true} if this step is declared
         * @param status the status object
         * @param timings the timings object
         * @throws NullPointerException if type, status or timings is {@code null}
         */
        public Step(Steps parent, String name, String args, boolean meta, boolean declared, Status status, Timings timings) {
            super(parent, name, createPath(parent, name, args), status, timings);
            this.args = args != null ? args : "";
            this.meta = meta;
            this.declared = declared;
        }

        /**
         * Get the step arguments.
         *
         * @return String
         */
        @JsonProperty
        public String args() {
            return args;
        }

        /**
         * Get the meta flag.
         *
         * @return {@code boolean}
         */
        @JsonProperty
        public boolean meta() {
            return meta;
        }

        /**
         * Get the declared flag.
         *
         * @return {@code boolean}
         */
        @JsonProperty
        public boolean declared() {
            return declared;
        }

        @Override
        public Step previous() {
            int previousIndex = index() - 1;
            if (previousIndex >= 0) {
                return ((Steps)parent).steps.get(previousIndex);
            }
            return null;
        }

        @Override
        public Step next() {
            if (parent != null) {
                List<Step> sequence = ((Steps)parent).steps;
                int nextIndex = index() + 1;
                if (nextIndex > 0 && nextIndex < sequence.size()) {
                    return sequence.get(nextIndex);
                }
            }
            return null;
        }

        @Override
        public int index() {
            Iterator<Step> it = ((Steps)parent).steps.iterator();
            for (int i=0 ; it.hasNext() ; i++) {
                if (it.next().id == id) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean head() {
            if (parent != null) {
                List<Stage> sequence = ((Stages)parent).stages;
                return index() == sequence.size() - 1;
            }
            return true;
        }

        private boolean isIncluded(boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
            return ((excludeSyntheticSteps && declared) || (!excludeSyntheticSteps && declared))
                    && ((excludeMetaSteps && !meta) || (!excludeMetaSteps && meta));
        }

        @Override
        public void fireCreated() {
            Pipeline.Step previous = previous();
            if (previous != null) {
                previous.fireCompleted();
            }
            int parentId = parent == null ? -1 : parent.id;
            listener.onEvent(new StepCreated(runId, id, parentId, index(), name, path, timings.startTime, args, meta, declared));
        }

        @Override
        public void fireCompleted() {
            status.refresh();
            timings.refresh();
            status.state = State.FINISHED;
            if (timings.endTime == 0) {
                timings.endTime = System.currentTimeMillis();
            }
            listener.onEvent(new StepCompleted(runId, id, status.result, timings.endTime));
        }

        /**
         * Create a step path.
         * @param prefix path prefix
         * @param name step name
         * @param args step arguments
         * @return String
         */
        public static String createPath(String prefix, String name, String args) {
            String s = prefix + "step(" + name + ")";
            if (!args.isEmpty()) {
                s += "=" + new String(Base64.getEncoder().encode(args.getBytes()));
            }
            return s;
        }

        private static String createPath(Node parent, String name, String args) {
            Objects.requireNonNull(parent, "parent is null");
            String s = parent.path + "step(" + name + ")";
            if (!args.isEmpty()) {
                try {
                    s += "=" + URLEncoder.encode(args, "UTF-8").replace("\\+", "%20");
                } catch (UnsupportedEncodingException ex) { }
            }
            return s;
        }
    }

    /**
     * A stage node with nested stage nodes.
     */
    public static abstract class Stages extends Stage {

        final List<Stage> stages = new LinkedList<>();

        private Stages(StageType type, Status status, Timings timings, EventListener listener, String runId) {
            super(type, status, timings, listener, runId);
        }

        private Stages(StageType type, int id, Node parent, String name, String path, Status status, Timings timings) {
            super(type, id, parent, name, path, status, timings);
        }

        /**
         * Create a new multi stage.
         *
         * @param type stage type, must be non {@code null}
         * @param id stage id, must be a valid {@code String}
         * @param parent parent stage, may be {@code null}
         * @param name stage name, may be {@code null}
         * @param path run id, must be a valid {@code String} starting with a {@code /}
         * @param status the status object
         * @param timing the timings object
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if type, status or timings is {@code null}
         */
        Stages(StageType type, Node parent, String name, Status status, Timings timings) {
            super(type, parent, name, status, timings);
        }

        /**
         * Add a nested stage.
         *
         * @param stage stage to add
         */
        public final void addStage(Stage stage) {
            stages.add(stage);
        }

        /**
         * Get the nested stages.
         *
         * @return immutable list of {@link Stage}
         */
        @JsonProperty
        public final List<Stage> stages() {
            return Collections.unmodifiableList(stages);
        }
    }

    /**
     * A set of stages running in parallel.
     */
    public static final class Parallel extends Stages {

        private Parallel(int id, Node parent, String name, String path, Status status, Timings timings) {
            super(StageType.PARALLEL, id, parent, name, path, status, timings);
        }

        /**
         * Create a new parallel stages.
         *
         * @param parent parent stage, may be {@code null}
         * @param name stage name, may be {@code null}
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if status or timings is {@code null}
         */
        public Parallel(Node parent, String name, Status status, Timings timings) {
            super(StageType.PARALLEL, parent, name, status, timings);
        }

        @Override
        public void fireCompleted() {
            Result result = Result.SUCCESS;
            long endTime = 0;
            // result is the worst nested result
            // endTime is the longest nested endTime
            for(Stage stage : stages) {
                stage.fireCompleted();
                Result res = stage.status.result;
                if (res.ordinal() < result.ordinal()) {
                    result  = res;
                }
                if (stage.timings.endTime > endTime) {
                    endTime = stage.timings.endTime;
                }
            }
            status.state = State.FINISHED;
            status.result = result;
            timings.endTime = endTime > 0 ? endTime : System.currentTimeMillis();
            listener.onEvent(new StageCompleted(runId, id, status.result, timings.endTime));
        }
    }

    /**
     * An ordered sequence of stages.
     */
    public static final class Sequence extends Stages {

        /**
         * Create a new root sequence stage.
         * @param status the status object
         * @param timings the timings object
         * @param listener event listener
         * @param runId pipeline run id
         */
        private Sequence(Status status, Timings timings, EventListener listener, String runId) {
            super(StageType.SEQUENCE, status, timings, listener, runId);
        }

        private Sequence(int id, Node parent, String name, String path, Status status, Timings timings) {
            super(StageType.SEQUENCE, id, parent, name, path, status, timings);
        }

        /**
         * Create a new sequence stages.
         *
         * @param parent parent stage, may be {@code null}
         * @param name stage name, may be {@code null}
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if status or timings is {@code null}
         */
        public Sequence(Node parent, String name, Status status, Timings timings) {
            super(StageType.SEQUENCE, parent, name, status, timings);
        }

        @Override
        public void fireCompleted() {
            Stage last = null;
            if (!stages.isEmpty()) {
                last = stages.get(stages.size() - 1);
                last.fireCompleted();
            }
            status.state = State.FINISHED;
            status.result = last != null ? last.result() : Result.SUCCESS;
            timings.endTime = last != null ? last.endTime() : System.currentTimeMillis();
            listener.onEvent(new StageCompleted(runId, id, status.result, timings.endTime));
        }
    }

    static Pipeline readPipeline(JsonNode node, String runId) throws IOException, JsonProcessingException {
        JsonNode rootNode = node.get("stages"); 
        Status status = readStatus(rootNode);
        Timings timings = readTimings(rootNode);
        Pipeline graph = new Pipeline(runId, status, timings);

        // create the stages (depth first)
        LinkedList<JsonNode> stack = new LinkedList<>();
        Iterator<JsonNode> stagesNodes = rootNode.get("stages").elements();
        while (stagesNodes.hasNext()) {
            stack.add(stagesNodes.next());
        }
        Node parent = graph.sequence;
        while (!stack.isEmpty()) {
            JsonNode stageNode = stack.peek();
            int stageId = stageNode.get("id").asInt(-1);
            Stage.StageType stageType = Stage.StageType.valueOf(stageNode.get("type").asText());
            if (stageType == Stage.StageType.STEPS) {
                // tree leaf
                Steps steps = new Steps(stageId, parent, readStatus(stageNode), readTimings(stageNode));
                Iterator<JsonNode> nestedSteps = stageNode.get("steps").elements();
                while (nestedSteps.hasNext()) {
                    JsonNode nestedStep = nestedSteps.next();
                    steps.addStep(new Step(nestedStep.get("id").asInt(), steps, nestedStep.get("name").asText(),
                            nestedStep.get("path").asText(), nestedStep.get("args").asText(),
                            nestedStep.get("meta").asBoolean(), nestedStep.get("declared").asBoolean(),
                            readStatus(nestedStep), readTimings(nestedStep)));
                }
                ((Stages) parent).addStage(steps);
                stack.pop();
            } else {
                // tree node
                if (parent.id == stageId) {
                    // leaving a node (2nd pass)
                    parent = parent.parent;
                    stack.pop();
                } else {
                    // entering a node

                    // create the stage
                    Stages stage;
                    switch (stageType) {
                        case SEQUENCE:
                            stage = new Sequence(stageId, parent, stageNode.get("name").asText(),
                                    stageNode.get("path").asText(null), readStatus(stageNode), readTimings(stageNode));
                            break;
                        case PARALLEL:
                            stage = new Parallel(stageId, parent, stageNode.get("name").asText(),
                                    stageNode.get("path").asText(null), readStatus(stageNode), readTimings(stageNode));
                            break;
                        default:
                            throw new IllegalStateException("Unknown type: " + stageType);
                    }
                    ((Stages) parent).addStage(stage);

                    // add nested stages on the stack
                    JsonNode nestedStages = stageNode.get("stages");
                    if (nestedStages.size() > 0) {
                        Iterator<JsonNode> nestedStagesIt = nestedStages.elements();
                        while (nestedStagesIt.hasNext()) {
                            stack.push(nestedStagesIt.next());
                        }
                        parent = stage;
                    } else {
                        // one pass only
                        stack.pop();
                    }
                }
            }
        }
        return graph;
    }

    private static Status readStatus(JsonNode node) {
        String state = node.get("state").asText(null);
        String result = node.get("result").asText(null);
        return new Status(State.valueOf(state), result != null ? Result.valueOf(result) : null);
    }

    private static Timings readTimings(JsonNode node) {
        long startTime = node.get("startTime").asLong(-1);
        long endTime = node.get("endTime").asLong(-1);
        return new Timings(startTime, endTime);
    }
}
