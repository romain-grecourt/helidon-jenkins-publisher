package io.helidon.build.publisher.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Flow graph.
 */
@JsonDeserialize(using = Pipeline.Deserializer.class)
public final class Pipeline {

    final Sequence stages;

    /**
     * Create a new pipeline instance.
     *
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if status or timings is {@code null}
     */
    public Pipeline(Status status, Timings timings) {
        stages = new Sequence(status, timings);
    }

    /**
     * Get the pipeline top-level sequence stage.
     * @return Sequence
     */
    @JsonProperty
    public Sequence stages() {
        return stages;
    }

    /**
     * Fire an event.
     *
     * @param listener consumer of the event
     * @param nodeEventType the node event type
     * @param runId the run id
     * @param run the run
     */
    void fireEvent(PipelineEvents.EventListener listener, PipelineEvents.NodeEventType nodeEventType, String runId,
            PipelineRun run) {

        if (nodeEventType == PipelineEvents.NodeEventType.CREATED) {
            listener.onEvent(new PipelineEvents.PipelineCreated(runId, run.jobName, run.scmHead, run.scmHash,
                    stages.timings.startTime, stages.status.state));
        }
        listener.onEvent(new PipelineEvents.PipelineCompleted(runId, stages.state(), stages.result(), stages.endTime()));
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
        LinkedList<Stage> stack = new LinkedList<>(stages.stages);
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
     * @param flowEvents the events to apply
     */
    public void applyEvents(List<PipelineEvents.Event> flowEvents) {
        for (PipelineEvents.Event event : flowEvents) {
            switch (event.eventType()) {
                case PIPELINE_COMPLETED:
                case STEP_COMPLETED:
                case STAGE_COMPLETED:
                    PipelineEvents.NodeCompletedEvent nodeCompleted = (PipelineEvents.NodeCompletedEvent) event;
                    Node node = stages.nodesByIds.get(nodeCompleted.id);
                    if (node == null) {
                        throw new IllegalStateException("Unkown node, id=" + nodeCompleted.id);
                    }
                    node.status.state = nodeCompleted.state;
                    node.status.result = nodeCompleted.result;
                    node.timings.endTime = nodeCompleted.endTime;
                    break;
                case STEP_CREATED:
                    PipelineEvents.StepCreated stepCreated = (PipelineEvents.StepCreated) event;
                    Node stepParent = stages.nodesByIds.get(stepCreated.parentId);
                    if (stepParent == null) {
                        throw new IllegalStateException("Unkown node, id=" + stepCreated.parentId);
                    }
                    if (!(stepParent instanceof Steps)) {
                        throw new IllegalStateException("Invalid step parent node");
                    }
                    if (stepCreated.index != ((Steps) stepParent).steps.size()) {
                        throw new IllegalStateException("Invalid index");
                    }
                    ((Steps) stepParent).addStep(new Step(stepCreated.id, (Steps) stepParent, stepCreated.name,
                            stepCreated.path, stepCreated.args, stepCreated.meta, stepCreated.declared,
                            new Status(stepCreated.state), new Timings(stepCreated.startTime)));
                    break;
                case STAGE_CREATED:
                    PipelineEvents.StageCreated stageCreated = (PipelineEvents.StageCreated) event;
                    Node stageParent = stages.nodesByIds.get(stageCreated.parentId);
                    if (stageParent == null) {
                        throw new IllegalStateException("Unkown node, id=" + stageCreated.parentId);
                    }
                    if (!(stageParent instanceof Stages)) {
                        throw new IllegalStateException("Invalid stage parent node");
                    }
                    if (stageCreated.index != ((Stages) stageParent).stages.size()) {
                        throw new IllegalStateException("Invalid index");
                    }
                    switch (stageCreated.stageType) {
                        case PARALLEL:
                            ((Stages) stageParent).addStage(new Parallel(stageCreated.id, stageParent,
                                    stageCreated.name, stageCreated.path,
                                    new Status(stageCreated.state), new Timings(stageCreated.startTime)));
                            break;
                        case SEQUENCE:
                            ((Stages) stageParent).addStage(new Sequence(stageCreated.id, stageParent,
                                    stageCreated.name, stageCreated.path,
                                    new Status(stageCreated.state), new Timings(stageCreated.startTime)));
                            break;
                        case STEPS:
                            ((Stages) stageParent).addStage(new Steps(stageCreated.id, stageParent,
                                    new Status(stageCreated.state), new Timings(stageCreated.startTime)));
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

        /**
         * Create a new root graph node.
         *
         * @param status the status object
         * @param timings the timings object
         * @throws NullPointerException if status or timings is {@code null}
         */
        private Node(Status status, Timings timings) {
            this.status = Objects.requireNonNull(status, "status is null");
            this.timings = Objects.requireNonNull(timings, "timings is null");
            this.parent = null;
            this.name = null;
            this.path = "/";
            this.id = 0;
            this.nextId = new AtomicInteger(1);
            this.nodesByIds = new HashMap<>();
            this.nodesByIds.put(id, this);
        }

        /**
         * Create a new graph node.
         *
         * @param id the id
         * @param parent enclosing node, must be non {@code null}
         * @param name stage name, may be {@code null}
         * @param path stage path, must be a valid {@code String} starting with a {@code /}
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if id is {@code <= 0}
         * @throws IllegalArgumentException if id is already used
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if parent, status or timings is {@code null}
         */
        private Node(int id, Node parent, String name, String path, Status status, Timings timings) {
            this.status = Objects.requireNonNull(status, "status is null");
            this.timings = Objects.requireNonNull(timings, "timings is null");
            this.parent = Objects.requireNonNull(parent, "parent is null");
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
            return timings.endTime(this);
        }

        /**
         * Get the result.
         *
         * @return FlowStatus.State
         */
        @JsonProperty
        public final Status.State state() {
            return status.state(this);
        }

        /**
         * Get the result.
         *
         * @return FlowStatus.Result
         */
        @JsonProperty
        public final Status.Result result() {
            return status.result(this);
        }

        /**
         * Fire an event.
         * @param listener consumer of the event
         * @param nodeEventType the node event type
         * @param id the run id
         */
        public abstract void fireEvent(PipelineEvents.EventListener listener, PipelineEvents.NodeEventType nodeEventType, String id);

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

        /**
         * Create a new root stage.
         * @param type stage type
         * @param status the status object
         * @param timings the timings object
         */
        private Stage(StageType type, Status status, Timings timings) {
            super(status, timings);
            this.type = type;
        }

        /**
         * Create a new stage.
         *
         * @param id the node id
         * @param type the stage type
         * @param parent the parent stage that this step is part of
         * @param name the stage name, must be non {@code null} and non empty
         * @param path the stage path, must be non {@code null} and non empty
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if id is {@code <= 0}
         * @throws IllegalArgumentException if id is already used
         * @throws NullPointerException if type, status or timings is {@code null}
         */
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

        /**
         * Create a stage path.
         * @param parent the parent node, must be non {@code null}
         * @param stageType the stage type
         * @param name the stage name
         * @return String
         */
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
        public void fireEvent(PipelineEvents.EventListener listener, PipelineEvents.NodeEventType nodeEventType, String runId) {
            if (nodeEventType == PipelineEvents.NodeEventType.CREATED) {
                int parentId = parent == null ? -1 : parent.id;
                listener.onEvent(new PipelineEvents.StageCreated(runId, id, parentId, index(), name, path, timings.startTime,
                        status.state, type));
            }
            listener.onEvent(new PipelineEvents.StageCompleted(runId, id, state(), result(), endTime()));
        }

        /**
         * Infer status from the graph.
         * @return FlowStatus
         */
        public abstract Status status();
    }

    /**
     * A steps stage.
     */
    public static final class Steps extends Stage {

        final List<Step> steps = new LinkedList<>();

        /**
         * Create a new steps stage.
         *
         * @param id the node id
         * @param parent the parent stage that this step is part of
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if id is {@code <= 0}
         * @throws IllegalArgumentException if id is already used
         * @throws NullPointerException if type, status or timings is {@code null}
         */
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

        /**
         * Pretty print this step stage.
         *
         * @param indent indentation
         * @param excludeSyntheticSteps if {@code true} the steps that are not declared are filtered out
         * @param excludeMetaSteps if {@code true} the meta steps are filtered out
         * @return String
         */
        public final String prettyPrint(String indent, boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("steps {\n");
            indent += "  ";
            for (Step step : steps) {
                sb.append(step.prettyPrint(indent));
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
        public Status status() {
            Status.State state = head() ? Status.State.RUNNING : Status.State.FINISHED;
            Status.Result result = Status.Result.SUCCESS;
            if (!steps.isEmpty()) {
                result = steps.get(steps.size() - 1).result();
            }
            return new Status(state, result);
        }
    }

    /**
     * A step node.
     */
    public static final class Step extends Node {

        final String args;
        final boolean meta;
        final boolean declared;

        /**
         * Create a new step instance.
         *
         * @param id the node id
         * @param parent the parent stage that this step is part of
         * @param name the step name, must be non {@code null} and non empty
         * @param args the step arguments
         * @param meta {@code true} if this step is a meta step
         * @param declared {@code true} if this step is declared
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if id is {@code <= 0}
         * @throws IllegalArgumentException if id is already used
         * @throws NullPointerException if status or timings is {@code null}
         */
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

        /**
         * Test if this step should be included for the given flags.
         *
         * @param excludeSyntheticSteps if {@code true} this step is included only if declared
         * @param excludeMetaSteps if {@code true} this step is not included if meta
         * @return {@code true} if included, {@code false} if excluded
         */
        public boolean isIncluded(boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
            return ((excludeSyntheticSteps && declared) || (!excludeSyntheticSteps && declared))
                    && ((excludeMetaSteps && !meta) || (!excludeMetaSteps && meta));
        }

        /**
         * Pretty print this step.
         *
         * @param indent indentation
         * @return String
         */
        public String prettyPrint(String indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append(name);
            if (!args.isEmpty()) {
                String[] argsLines = args.split("\\r?\\n");
                sb.append(" ");
                for (String line : argsLines) {
                    String argPreview = line.trim();
                    if (!argPreview.isEmpty()) {
                        sb.append(argPreview);
                        break;
                    }
                }
                if (argsLines.length > 1) {
                    sb.append(" [...]");
                }
            }
            return sb.toString();
        }

        @Override
        public void fireEvent(PipelineEvents.EventListener listener, PipelineEvents.NodeEventType nodeEventType, String runId) {
            if (nodeEventType == PipelineEvents.NodeEventType.CREATED) {
                int parentId = parent == null ? -1 : parent.id;
                listener.onEvent(new PipelineEvents.StepCreated(runId, id, parentId, index(), name,timings.startTime,
                        status.state, args, meta, declared));
            }
            listener.onEvent(new PipelineEvents.StepCompleted(runId, id, state(), result(), endTime()));
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

        /**
         * Create a step path.
         * @param parent the parent node, must be non {@code null}
         * @param stageType the stage type
         * @param name the stage name
         * @return String
         */
        private static String createPath(Node parent, String name, String args) {
            Objects.requireNonNull(parent, "parent is null");
            return createPath(parent.path, name, args);
        }
    }

    /**
     * A stage node with nested stage nodes.
     */
    public static abstract class Stages extends Stage {

        final List<Stage> stages = new LinkedList<>();

        /**
         * Create a new root stages.
         * @param type stage type
         * @param status the status object
         * @param timings the timings object
         */
        private Stages(StageType type, Status status, Timings timings) {
            super(type, status, timings);
        }

        /**
         * Create a new multi stage.
         *
         * @param type stage type, must be non {@code null}
         * @param id node id
         * @param parent parent stage, may be {@code null}
         * @param name stage name, may be {@code null}
         * @param path run id, must be a valid {@code String} starting with a {@code /}
         * @param status the status object
         * @param timing the timings object
         * @throws IllegalArgumentException if id is {@code <= 0}
         * @throws IllegalArgumentException if id is already used
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if type, status or timings is {@code null}
         */
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

        /**
         * Create a new parallel stages.
         *
         * @param id the node id
         * @param parent parent stage, may be {@code null}
         * @param name stage name, may be {@code null}
         * @param path run id, must be a valid {@code String} starting with a {@code /}
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if id is {@code <= 0}
         * @throws IllegalArgumentException if id is already used
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if status or timings is {@code null}
         */
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
        public Status status() {
            Status.State state = head() ? Status.State.RUNNING : Status.State.FINISHED;
            Status.Result result = Status.Result.SUCCESS;
            for (Pipeline.Stage stage : stages) {
                Status.Result res = stage.result();
                if (res.ordinal() < result.ordinal()) {
                    result = res;
                }
            }
            return new Status(state, result);
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
         */
        private Sequence(Status status, Timings timings) {
            super(StageType.SEQUENCE, status, timings);
        }

        /**
         * Create a new sequence stages.
         *
         * @param id the node id
         * @param parent parent stage, may be {@code null}
         * @param name stage name, may be {@code null}
         * @param path run id, must be a valid {@code String} starting with a {@code /}
         * @param status the status object
         * @param timings the timings object
         * @throws IllegalArgumentException if id is {@code <= 0}
         * @throws IllegalArgumentException if id is already used
         * @throws IllegalArgumentException if path does not start with a {@code /}
         * @throws NullPointerException if status or timings is {@code null}
         */
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
        public Status status() {
            Status.State state = head() ? Status.State.RUNNING : Status.State.FINISHED;
            Status.Result result = Status.Result.SUCCESS;
            if (!stages.isEmpty()) {
                  result = stages.get(stages.size() - 1).result();
            }
            return new Status(state, result);
        }
    }

    /**
     * Custom {@link Deserializer} for {@link Pipeline}.
     */
    public static final class Deserializer extends StdDeserializer<Pipeline> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Pipeline deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);

            // create the run.
            JsonNode root = node.get("stages");
            Status status = createStatus(root);
            Timings timings = createTimings(root);
            Pipeline graph = new Pipeline(status, timings);

            // create the stages (depth first)
            LinkedList<JsonNode> stack = new LinkedList<>();
            Iterator<JsonNode> stages = root.get("stages").elements();
            while (stages.hasNext()) {
                stack.add(stages.next());
            }
            Node parent = graph.stages;
            while (!stack.isEmpty()) {
                JsonNode stageNode = stack.pop();
                int stageId = stageNode.get("id").asInt(-1);
                Stage.StageType stageType = Stage.StageType.valueOf(stageNode.get("type").asText());
                if (stageType == Stage.StageType.STEPS) {
                    // tree leaf
                    Steps steps = new Steps(stageId, parent, createStatus(stageNode), createTimings(stageNode));
                    Iterator<JsonNode> nestedSteps = stageNode.get("steps").elements();
                    while (nestedSteps.hasNext()) {
                        JsonNode nestedStep = nestedSteps.next();
                        steps.addStep(new Step(nestedStep.get("id").asInt(), steps, nestedStep.get("name").asText(),
                                nestedStep.get("path").asText(), nestedStep.get("args").asText(),
                                nestedStep.get("meta").asBoolean(), nestedStep.get("declared").asBoolean(),
                                createStatus(nestedStep), createTimings(nestedStep)));
                    }
                    ((Stages)parent).addStage(steps);
                    parent = parent.parent;
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
                                        stageNode.get("path").asText(null), createStatus(stageNode), createTimings(stageNode));
                                break;
                            case PARALLEL:
                                stage = new Parallel(stageId, parent, stageNode.get("name").asText(),
                                        stageNode.get("path").asText(null), createStatus(stageNode), createTimings(stageNode));
                                break;
                            default:
                                throw new IllegalStateException("Unknown type: " + stageType);
                        }
                        ((Stages)parent).addStage(stage);

                        // add nested stages on the stack
                        JsonNode nestedStages = stageNode.get("stages");
                        if (nestedStages.size() > 0) {
                            Iterator<JsonNode> nestedStagesIt = nestedStages.elements();
                            while (nestedStagesIt.hasNext()) {
                                stack.add(nestedStagesIt.next());
                            }
                            parent = stage;
                        } else {
                            // one pass only
                            parent = parent.parent;
                            stack.pop();
                        }
                    }
                }
            }
            return graph;
        }

        private static Status createStatus(JsonNode node) {
            String state = node.get("state").asText(null);
            String result = node.get("result").asText(null);
            return new Status(Status.State.valueOf(state), result != null ? Status.Result.valueOf(result) : null);
        }

        private static Timings createTimings(JsonNode node) {
            long startTime = node.get("startTime").asLong(-1);
            long endTime = node.get("endTime").asLong(-1);
            return new Timings(startTime, endTime);
        }
    }
}
