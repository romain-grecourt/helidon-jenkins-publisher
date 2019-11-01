package io.helidon.jenkins.publisher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.cps.steps.ParallelStep;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

/**
 * Representation of a flow graph.
 */
final class FlowGraph {

    private static final String STAGE_DESC_ID = StageStep.class.getName();
    private static final String PARALLEL_DESC_ID = ParallelStep.class.getName();

    private final FlowStepSignatures signatures;
    private final LinkedList<StepAtomNode> headNodes = new LinkedList<>();
    private final Map<String, FlowStep> steps;
    private final Map<String, FlowStage> stages;
    private final FlowStage.Sequence root;

    /**
     * Status listener.
     */
    interface StatusListener {

        /**
         * Status event for a step.
         * @param step the corresponding step
         * @param status the status
         */
        void onStepStatus(FlowStep step, FlowStatus status);

        /**
         * Status event for a stage.
         * @param stage the corresponding stage
         * @param status the status
         */
        void onStageStatus(FlowStage stage, FlowStatus status);
    }

    /**
     * Create a new flow graph.
     * @param signatures the signatures used to identify the step node that are declared
     * @param node the root node
     */
    FlowGraph(FlowStepSignatures signatures) {
        Objects.requireNonNull(signatures, "signatures is null");
        this.signatures = signatures;
        this.root = new FlowStage.Sequence();
        this.steps = new HashMap<>();
        this.stages = new HashMap<>();
        this.stages.put(root.id(), root);
    }

    /**
     * Get the top level stage sequence.
     * @return StageSequence
     */
    FlowStage.Sequence root() {
        return root;
    }

    /**
     * Get the step for the given step id.
     * @param id step id
     * @return {@link FlowStep} if found, or {@code null} if not found
     */
    FlowStep step(String id) {
        return steps.get(id);
    }

    /**
     * Get the next unprocessed step at the head.
     * @return FlowStep or {@code null} if there is no unprocessed step at the head
     */
    FlowStep poll() {
        if (!headNodes.isEmpty()) {
            StepAtomNode node = headNodes.pollLast();
            return steps.get(node.getId());
        }
        return null;
    }

    /**
     * Store the new current head and fires the status events if any.
     * @param node new head
     * @param listener listener to consume the status events
     */
    void offer(FlowNode node, StatusListener listener) {
        Objects.requireNonNull(node, "node is null");
        Objects.requireNonNull(listener, "listener is null");
        if ((node instanceof StepAtomNode)) {
            headNodes.addFirst((StepAtomNode) node);
            FlowStage.Sequence parent = findSequenceStage((StepAtomNode) node);
            String stepsStageId = parent.id() + "-steps";
            FlowStage s = stages.get(stepsStageId);
            FlowStage.Steps stepsStage;
            if (s != null){
                if(!(s instanceof FlowStage.Steps)) {
                    throw new IllegalStateException("invalid steps stage");
                }
                stepsStage = (FlowStage.Steps) s;
            } else {
                stepsStage = new FlowStage.Steps(stepsStageId, parent.node(), parent);
                parent.addStage(stepsStage);
                stages.put(stepsStageId, stepsStage);
            }
            FlowStep step = new FlowStep((StepAtomNode)node, stepsStage, signatures);
            stepsStage.addStep(step);
            steps.put(step.id(), step);
            listener.onStepStatus(step, new FlowStatus(step.node()));
            int index = step.parentIndex();
            List<FlowStep> sequence = step.stage().steps();
            if (index > 0 && sequence.size() > index + 1) {
                FlowStep previous = sequence.get(index - 1);
                listener.onStepStatus(previous, new FlowStatus(previous.node()));
            }
        } else if (node instanceof StepStartNode) {
            StepStartNode stepStartNode = (StepStartNode) node;
            if (node.getAction(LabelAction.class) != null) {
                FlowStage.Virtual stage = null;
                FlowStage.Virtual parent = findVirtualStage(stepStartNode);
                String descId = stepStartNode.getDescriptor().getId();
                if (STAGE_DESC_ID.equals(descId)) {
                    stage = new FlowStage.Sequence(stepStartNode, parent);
                } else if (PARALLEL_DESC_ID.equals(descId)) {
                    // TODO wrap against the parent node of this step so that the parallel branches
                    // are grouped in the same stage
                    // TODO check order of parallel steps to avoid mismatch
                    stage = new FlowStage.Parallel(stepStartNode, parent);
                }
                if (stage != null) {
                    parent.addStage(stage);
                    stages.put(stage.id(), stage);
                    listener.onStageStatus(stage, new FlowStatus(stage.node()));
                    int index = stage.parentIndex();
                    if (parent instanceof FlowStage.Sequence && index > 0) {
                        List<FlowStage> sequence = ((FlowStage.Sequence) parent).stages();
                        if (index > 0 && sequence.size() > index + 1) {
                            FlowStage previous = sequence.get(index - 1);
                            listener.onStageStatus(previous, new FlowStatus(previous.node()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Find the first sequence stage in the enclosing blocks of the given node.
     * @param node the node to visit
     * @return FlowStage.Sequence, never {@code null}
     */
    private FlowStage.Sequence findSequenceStage(StepAtomNode node) {
        FlowStage parent = findParent(node);
        if (!(parent instanceof FlowStage.Sequence)) {
            throw new IllegalStateException("steps parent stage is not a sequence");
        }
        return (FlowStage.Sequence) parent;
    }

    /**
     * Find the first virtual stage in the enclosing blocks of the given node.
     * @param node the node to visit
     * @return FlowStage.Virtual, never {@code null}
     */
    private FlowStage.Virtual findVirtualStage(StepStartNode node) {
        FlowStage parent = findParent(node);
        if (!(parent instanceof FlowStage.Virtual)) {
            throw new IllegalStateException("parent stage is not a virtual stage");
        }
        return (FlowStage.Virtual) parent;
    }

    /**
     * Find the first stage in the enclosing blocks of the given node.
     * @param node the node to visit
     * @return FlowStage, never {@code null}
     */
    private FlowStage findParent(FlowNode node) {
        for (BlockStartNode parent : node.getEnclosingBlocks()) {
            FlowStage stage = stages.get(parent.getId());
            if (stage != null) {
                return stage;
            }
        }
        return root;
    }
}
