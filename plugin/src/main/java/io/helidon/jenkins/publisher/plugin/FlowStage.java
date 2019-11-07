package io.helidon.jenkins.publisher.plugin;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;

/**
 * A stage or meta stage.
 */
abstract class FlowStage {

    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(new HashMap<>());

    protected final FlowRun flowRun;
    protected final BlockStartNode node;
    protected final FlowStage parent;
    protected final String id;
    protected final String name;
    protected final int parentIndex;
    protected final String uri;
    protected int index = 0;
    protected FlowStatus status;
    protected final long startTime;
    protected long endTime;

    /**
     * Create a new stage.
     *
     * @param flowRun the flow run that this stage belongs to
     * @param id the node id to use
     * @param node original graph node, may be {@code null}
     * @param parent parent stage, may be {@code null}
     * @param synthetic if {@code true}, this stage path is equal to the parent path
     */
    protected FlowStage(FlowRun flowRun, String id, BlockStartNode node, FlowStage parent, boolean synthetic) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Stage id is null or empty");
        }
        Objects.requireNonNull(node, "node is null");
        Objects.requireNonNull(flowRun, "run is null");
        this.flowRun = flowRun;
        this.id = id;
        String u;
        this.node = node;
        this.parent = parent;
        if (parent != null) {
            LabelAction action = node.getAction(LabelAction.class);
            if (action != null) {
                if (action instanceof ThreadNameAction) {
                    this.name = ((ThreadNameAction)action).getThreadName();
                } else {
                    this.name = action.getDisplayName();
                }
            } else {
                this.name = "";
            }
            this.parentIndex = parent.index + 1;
            u = parent.uri;
            if (!synthetic) {
                if (node instanceof StepStartNode) {
                    u += ((StepStartNode)node).getDescriptor().getFunctionName();
                }
                if (((StepStartNode)node).isBody() && this.name != null && !this.name.isEmpty()) {
                    if (parent instanceof Parallel) {
                        u += "/";
                    }
                    try {
                        u += URLEncoder.encode(name, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
                u += "/";
            }
        } else {
            this.name = "";
            this.parentIndex = -1;
            u = "/";
        }
        this.uri = u;
        this.startTime = TimingAction.getStartTime(node);
        this.endTime = -1;
    }

    /**
     * Create a new stage.
     *
     * @param flowRun the flow run that this stage belongs to
     * @param id the node id to use
     * @param node original graph node, may be {@code null}
     * @param parent parent stage, may be {@code null}
     */
    protected FlowStage(FlowRun flowRun, BlockStartNode node, FlowStage parent, boolean synthetic) {
        this(flowRun, node != null ? node.getId() : "0", node, parent, synthetic);
    }

    /**
     * Create a new stage.
     *
     * @param flowRun the flow run that this stage belongs to
     * @param node original graph node, may be {@code null}
     * @param parent parent stage, may be {@code null}
     */
    protected FlowStage(FlowRun flowRun, BlockStartNode node, FlowStage parent) {
        this(flowRun, node, parent, /* synthetic */ false);
    }

    /**
     * If the parent stage is a {@link Sequence}, return the previous stage in the parent sequence.
     *
     * @return FlowStage or {@code null} if the parent is not a {@link Sequence} or there is no previous stage in the sequence
     */
    final FlowStage previous() {
        if (parent instanceof FlowStage.Sequence && parentIndex > 0) {
            List<FlowStage> sequence = ((FlowStage.Sequence) parent).stages;
            if (parentIndex > 0 && sequence.size() > parentIndex + 1) {
                return sequence.get(parentIndex - 1);
            }
        }
        return null;
    }

    /**
     * Indicate that this stage is the head of the parent sequence. 
     * @return {@code true} if this stage is the current head of the parent sequence
     */
    final boolean head() {
        return parentIndex == parent.index;
    }

    /**
     * Get the start time in milliseconds for this step.
     * @return long
     */
    final long startTime() {
        return startTime;
    }

    /**
     * Get the end time in milliseconds for this step.
     * @return long
     */
    final long endTime() {
        if (endTime <= 0 && status != null && status.state == FlowStatus.FlowState.FINISHED) {
            BlockEndNode endNode = node.getEndNode();
            if (endNode != null) {
                endTime = TimingAction.getStartTime(endNode);
            }
        }
        return endTime;
    }

    /**
     * Get this stage as a {@link Steps}.
     * @return Steps
     * @throws IllegalStateException if this instance is not a steps stage
     */
    final Steps asSteps() {
        if (this instanceof Steps) {
            return (Steps) this;
        }
        throw new IllegalStateException("Not a steps stage");
    }

    /**
     * Get this stage as a {@link Stages}.
     * @return Stages
     * @throws IllegalStateException if this instance is not a multi stage
     */
    final Stages asStages() {
        if (this instanceof Stages) {
            return (Stages) this;
        }
        throw new IllegalStateException("Not a multi stage");
    }

    /**
     * Get this stage as a {@link Parallel}.
     * @return Parallel
     * @throws IllegalStateException if this instance is not a parallel stage
     */
    final Parallel asParallel() {
        if (this instanceof Parallel) {
            return (Parallel) this;
        }
        throw new IllegalStateException("Not a parallel stage");
    }

    /**
     * Get this stage as a {@link Sequence}.
     * @return Sequence
     * @throws IllegalStateException if this instance is not a sequence
     */
    final Sequence asSequence() {
        if (this instanceof Sequence) {
            return (Sequence) this;
        }
        throw new IllegalStateException("Not a stage sequence");
    }

    /**
     * Indicate if this stage is a stage sequence.
     * @return {@code true} if this stage is a stage sequence.
     */
    final boolean isSequence() {
        return this instanceof Sequence;
    }

    /**
     * Indicate if this stage is a parallel stage.
     * @return {@code true} if this stage is a parallel stage.
     */
    final boolean isParallel() {
        return this instanceof Parallel;
    }

    /**
     * Indicate if this stage is a steps sequence.
     * @return {@code true} if this stage is a steps sequence.
     */
    final boolean isSteps() {
        return this instanceof Steps;
    }

    /**
     * Get the identifier for that this stage belongs to.
     *
     * @return FlowRun
     */
    final FlowRun flowRun() {
        return flowRun;
    }

    /**
     * Get the status for this stage.
     * @return FlowStatus
     */
    final FlowStatus status() {
        if (status == null || status.state != FlowStatus.FlowState.FINISHED) {
            status = new FlowStatus(node);
        }
        return status;
    }

    /**
     * Get the original graph node.
     *
     * @return v
     */
    final BlockStartNode node() {
        return node;
    }

    /**
     * Get the URI of this stage.
     *
     * @return String
     */
    final String uri() {
        return uri;
    }

    /**
     * Get the stage name.
     *
     * @return String
     */
    final String getName() {
        return name;
    }

    /**
     * Get the index in the parent stage.
     *
     * @return int
     */
    final int parentIndex() {
        return parentIndex;
    }

    /**
     * Get the stage unique id.
     *
     * @return String
     */
    final String id() {
        return id;
    }

    /**
     * Get the stage parent.
     *
     * @return FlowStage or {@code null} if this is the root stage.
     */
    final FlowStage parent() {
        return parent;
    }

    /**
     * Get the next nested sequence index.
     *
     * @return int
     */
    final int index() {
        return index;
    }

    @Override
    public String toString() {
        String type;
        if (this instanceof Steps) {
            type = Steps.class.getSimpleName();
        } else if (this instanceof Sequence) {
            type = Sequence.class.getSimpleName();
        } else if (this instanceof Parallel) {
            type = Parallel.class.getSimpleName();
        } else {
            throw new IllegalStateException("Unknown stage implementation");
        }
        return type + "{"
                + " id=" + id
                + ", parentId=" + (parent != null ? parent.id : "-1")
                + ", flowRun=" + flowRun.desc()
                + "}";
    }

    /**
     * Build a JSON object representation of this event.
     * @return JsonObject
     */
    final JsonObject toJson() {
        JsonObjectBuilder stage = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("id", id)
                .add("parentId", parent != null ? parent.id() : "-1");
        if (this instanceof Steps) {
            stage.add("type", "steps");
        } else if (this instanceof Sequence) {
            stage.add("type", "sequence");
        } else if (this instanceof Parallel) {
            stage.add("type", "parallel");
        } else {
            throw new IllegalStateException("Unknown stage implementation");
        }
        return stage
                .add("name", name)
                .add("status", status().toJson())
                .add("startTime", startTime)
                .add("endTime", endTime())
                .build();
    }

    /**
     * An ordered sequence of steps.
     */
    static final class Steps extends FlowStage {

        private final List<FlowStep> steps = new LinkedList<>();

        /**
         * Create a new steps sequence.
         *
         * @param flowRun the flow run that this stage belongs to
         * @param node original graph node
         * @param parent parent stage
         */
        Steps(FlowRun flowRun, String id, BlockStartNode node, FlowStage parent) {
            super(flowRun, id, node, parent, /* true */ true);
        }

        /**
         * Get the steps in sequence order.
         *
         * @return immutable list of {@link Step}
         */
        final List<FlowStep> steps() {
            return Collections.unmodifiableList(steps);
        }

        /**
         * Add a nested step.
         *
         * @param step to add
         */
        final void addStep(FlowStep step) {
            steps.add(step);
            index++;
        }

        /**
         * Pretty print this stage.
         * @param indent indentation
         * @param declaredOnly include declared steps only if {@code true}
         * @param skipMeta skip meta steps if {@code true}
         * @return String
         */
        final String prettyPrint(String indent, boolean declaredOnly, boolean skipMeta) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("steps {\n");
            indent += "  ";
            for (FlowStep step : steps) {
                sb.append(step.prettyPrint(indent));
                if ((declaredOnly && !step.declared()) || step.meta() && skipMeta) {
                    sb.append(" // filtered");
                }
                sb.append("\n");
            }
            indent = indent.substring(2);
            sb.append(indent).append("}\n");
            return sb.toString();
        }
    }

    /**
     * A parallel stage or a stage sequence.
     */
    static abstract class Stages extends FlowStage {

        protected final List<FlowStage> stages = new LinkedList<>();

        /**
         * Create a new multi stage.
         *
         * @param flowRun the flow run that this stage belongs to
         * @param node original graph node
         * @param parent parent stage
         */
        protected Stages(FlowRun flowRun, BlockStartNode node, Stages parent) {
            super(flowRun, node, parent);
        }

        /**
         * Create a new multi stage.
         *
         * @param flowRun the flow run that this stage belongs to
         * @param node the original graph nodes
         * @param parent parent stage
         * @param synthetic if {@code true}, this stage path is equal to the parent path
         */
        protected Stages(FlowRun flowRun, StepStartNode node, Stages parent, boolean synthetic) {
            super(flowRun, node, parent, synthetic);
        }

        /**
         * Add a nested stage.
         *
         * @param stage stage to add
         */
        final void addStage(FlowStage stage) {
            stages.add(stage);
            index++;
        }

        /**
         * Get the nested stages.
         *
         * @return
         */
        final List<FlowStage> stages() {
            return Collections.unmodifiableList(stages);
        }

        /**
         * Get a pretty print-able description of the graph.
         * @param indent indentation, must not be {@code null}
         * @param declaredOnly include the declared steps only
         * @param skipMeta skip the meta steps
         * @return String
         */
        final String prettyPrint(String indent, boolean declaredOnly, boolean skipMeta) {
            Objects.requireNonNull(indent, "indent is null");
            StringBuilder sb = new StringBuilder(indent);
            if (parent() != null) {
                sb.append("stage ");
            }
            sb.append("{\n");
            indent += "  ";
            LinkedList<FlowStage> stack = new LinkedList<>(stages);
            String parentId = id();
            while (!stack.isEmpty()) {
                FlowStage stage = stack.peek();
                if (stage instanceof FlowStage.Steps) {
                    // leaf
                    sb.append(((FlowStage.Steps) stage).prettyPrint(indent, declaredOnly, skipMeta));
                    parentId = stage.parent().id();
                    stack.pop();
                } else if (stage instanceof FlowStage.Stages) {
                    // node
                    if (parentId.equals(stage.id())) {
                        // leaving (2nd pass)
                        indent = indent.substring(2);
                        sb.append(indent).append("}\n");
                        parentId = stage.parent().id();
                        stack.pop();
                    } else {
                        // entering
                        sb.append(indent);
                        if (stage instanceof FlowStage.Parallel) {
                            sb.append("parallel {");
                        } else {
                            sb.append("stage");
                            String stageName = stage.getName();
                            if (stageName != null && !stageName.isEmpty()) {
                                sb.append("('").append(stageName).append("')");
                            }
                            sb.append(" {");
                        }
                        List<FlowStage> children = ((FlowStage.Stages) stage).stages();
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
                            parentId = stage.parent().id();
                            stack.pop();
                        }
                    }
                }
            }
            sb.append("}\n");
            return sb.toString();
        }
    }

    /**
     * A set of stages running in parallel.
     */
    static final class Parallel extends Stages {

        /**
         * Create a new parallel stages.
         *
         * @param flowRun the flow run that this stage belongs to
         * @param node original graph node
         * @param parent parent stage
         */
        Parallel(FlowRun flowRun, StepStartNode node, Stages parent) {
            super(flowRun, node, parent, /* synthetic */ true);
        }
    }

    /**
     * An ordered sequence of stages.
     */
    static final class Sequence extends Stages {

        /**
         * Create a new top level sequence.
         * @param flowRun the flow run that this stage belongs to
         * @param node the original graph node
         */
        Sequence(FlowRun flowRun, FlowStartNode node) {
            super(flowRun, node, /* parent */ null);
        }

        /**
         * Create a new stage sequence
         *
         * @param flowRun the flow run that this stage belongs to
         * @param node original graph node
         * @param parent parent stage
         */
        Sequence(FlowRun flowRun, StepStartNode node, Stages parent) {
            super(flowRun, node, parent);
        }
    }
}
