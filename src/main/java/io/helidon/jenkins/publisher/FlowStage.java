package io.helidon.jenkins.publisher;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;

/**
 * A stage or meta stage.
 */
abstract class FlowStage {

    private final StepStartNode node;
    private final FlowStage parent;
    private final String id;
    private final String name;
    private final int parentIndex;
    private final String path;
    protected int index = 0;

    /**
     * Create a new stage.
     *
     * @param id the node id to use
     * @param node original graph node, may be {@code null}
     * @param parent parent stage, may be {@code null}
     * @param synthetic if {@code true}, this stage path is equal to the parent path
     */
    protected FlowStage(String id, StepStartNode node, FlowStage parent, boolean synthetic) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("stage id is null or empty");
        }
        this.id = id;
        String p;
        if (parent != null && node != null) {
            this.node = node;
            this.parent = parent;
            LabelAction action = node.getAction(LabelAction.class);
            this.name = action != null ? action.getDisplayName() : null;
            this.parentIndex = parent.index + 1;
            p = parent.path;
            if (!synthetic) {
                p += node.getDescriptor().getFunctionName();
                if (node.isBody() && this.name != null && !this.name.isEmpty()) {
                    p += "[" + name + "]";
                }
                p += "/";
            }
        } else {
            this.node = null;
            this.name = null;
            this.parent = null;
            this.parentIndex = -1;
            p = "/";
        }
        this.path = p;
    }

    /**
     * Create a new stage.
     *
     * @param id the node id to use
     * @param node original graph node, may be {@code null}
     * @param parent parent stage, may be {@code null}
     */
    protected FlowStage(String id, StepStartNode node, FlowStage parent) {
        this(node != null ? node.getId() : "0", node, parent, /* meta */ false);
    }

    /**
     * Create a new stage.
     *
     * @param node original graph node, may be {@code null}
     * @param parent parent stage, may be {@code null}
     */
    protected FlowStage(StepStartNode node, FlowStage parent) {
        this(node != null ? node.getId() : "0", node, parent);
    }

    /**
     * Get the original graph node.
     *
     * @return v
     */
    StepStartNode node() {
        return node;
    }

    /**
     * Get the path of this stage.
     *
     * @return String
     */
    String path() {
        return path;
    }

    /**
     * Get the stage name.
     *
     * @return String
     */
    String getName() {
        return name;
    }

    /**
     * Get the index in the parent stage.
     *
     * @return int
     */
    int parentIndex() {
        return parentIndex;
    }

    /**
     * Get the stage unique id.
     *
     * @return String
     */
    String id() {
        return id;
    }

    /**
     * Get the stage parent.
     *
     * @return FlowStage or {@code null} if this is the root stage.
     */
    FlowStage parent() {
        return parent;
    }

    /**
     * Get the next nested sequence index.
     *
     * @return int
     */
    int index() {
        return index;
    }

    /**
     * An ordered sequence of steps.
     */
    static final class Steps extends FlowStage {

        private final List<FlowStep> steps = new LinkedList<>();

        /**
         * Create a new steps sequence.
         *
         * @param node original graph node
         * @param parent parent stage
         * @param id unique node id
         * @param name stage name, may be {@code null}
         * @param steps ordered list of steps
         * @param parentIndex index in the parent sequence
         */
        Steps(String id, StepStartNode node, FlowStage parent) {
            super(id, node, parent, /* true */ true);
        }

        /**
         * Get the steps in sequence order.
         *
         * @return immutable list of {@link Step}
         */
        List<FlowStep> steps() {
            return Collections.unmodifiableList(steps);
        }

        /**
         * Add a nested step.
         *
         * @param step to add
         */
        void addStep(FlowStep step) {
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
        String prettyPrint(String indent, boolean declaredOnly, boolean skipMeta) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("steps {\n");
            indent += "  ";
            for (FlowStep step : steps) {
                if ((declaredOnly && !step.declared()) || step.meta() && skipMeta) {
                    continue;
                }
                sb.append(step.prettyPrint(indent));
            }
            indent = indent.substring(2);
            sb.append(indent).append("}\n");
            return sb.toString();
        }
    }

    /**
     * A parallel stage or a stage sequence.
     */
    static abstract class Virtual extends FlowStage {

        private final List<FlowStage> stages = new LinkedList<>();

        /**
         * Create a new virtual stage.
         *
         * @param stages the nested stages
         * @param node original graph node
         * @param parent parent stage
         * @param id unique node id
         * @param name stage name, may be {@code null}
         * @param stages nested stages
         * @param index index in the parent sequence
         */
        protected Virtual(StepStartNode node, Virtual parent) {
            super(node, parent);
        }

        /**
         * Add a nested stage.
         *
         * @param stage stage to add
         */
        void addStage(FlowStage stage) {
            stages.add(stage);
            index++;
        }

        /**
         * Get the nested stages.
         *
         * @return
         */
        List<FlowStage> stages() {
            return Collections.unmodifiableList(stages);
        }

        /**
         * Get a pretty print-able description of the graph.
         *
         * @return String
         */
        String prettyPrint(String indent, boolean declaredOnly, boolean skipMeta) {
            StringBuilder sb = new StringBuilder();
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
                } else if (stage instanceof FlowStage.Virtual) {
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
                            String name = stage.getName();
                            if (name != null && !name.isEmpty()) {
                                sb.append("('").append(name).append("')");
                            }
                            sb.append(" {");
                        }
                        List<FlowStage> children = ((FlowStage.Virtual) stage).stages();
                        if (!children.isEmpty()) {
                            sb.append("\n");
                            // process children
                            indent += "  ";
                            for (int i = 0; i < children.size(); i++) {
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
    static final class Parallel extends Virtual {

        /**
         * Create a new parallel stages.
         *
         * @param node original graph node
         * @param parent parent stage
         * @param id unique node id
         * @param name stage name, may be {@code null}
         * @param index index in the parent sequence
         */
        Parallel(StepStartNode node, Virtual parent) {
            super(node, parent);
        }
    }

    /**
     * An ordered sequence of stages.
     */
    static final class Sequence extends Virtual {

        /**
         * Create a new top level sequence.
         */
        Sequence() {
            super(/* node */null, /* parent */ null);
        }

        /**
         * Create a new stage sequence
         *
         * @param stages nested stages in order
         */
        Sequence(StepStartNode node, Virtual parent) {
            super(node, parent);
        }
    }
}
