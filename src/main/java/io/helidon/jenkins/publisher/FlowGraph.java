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
 * Wrapper model for the flow graph.
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
            FlowStep step = createFlowStep((StepAtomNode)node);
            listener.onStepStatus(step, step.createStatus());
            FlowStep previous = step.previous();
            if (previous != null) {
                listener.onStepStatus(previous, previous.createStatus());
            }
        } else if (node instanceof StepStartNode) {
            FlowStage.Stages stage = createFlowStages((StepStartNode) node);
            if (stage != null) {
                listener.onStageStatus(stage, stage.createStatus());
                FlowStage previous = stage.previous();
                if (previous != null) {
                    listener.onStageStatus(previous, previous.createStatus());
                }
            }
        }
    }

    /**
     * Create a new flow step.
     * @param node the original graph node
     * @return FlowStep, never {@code null}
     */
    private FlowStep createFlowStep(StepAtomNode node) {
        FlowStage.Sequence parentStage = findParentStage(node).asSequence();
        FlowStage.Steps stepsStage = null;
        String parentId = parentStage.id() + "__steps-wrapper__";
        for (int i = 1 ; stepsStage == null ; i++) {
            String stepsStageId = parentId + "#" +i;
            if (stages.containsKey(stepsStageId)) {
                stepsStage = stages.get(stepsStageId).asSteps();
                if (!stepsStage.head()) {
                    stepsStage = null;
                }
            } else {
                stepsStage = new FlowStage.Steps(stepsStageId, parentStage.node(), parentStage);
                parentStage.addStage(stepsStage);
                stages.put(stepsStageId, stepsStage);
            }
        }
        FlowStep step = new FlowStep((StepAtomNode)node, stepsStage, signatures);
        stepsStage.addStep(step);
        steps.put(step.id(), step);
        return step;
    }

    /**
     * Create a new flow multi stage.
     * @param node the original graph node
     * @return FlowStage.Stages or null if this node doesn't map to a stage
     */
    private FlowStage.Stages createFlowStages(StepStartNode node) {
        if (node.getAction(LabelAction.class) != null) {
            FlowStage.Stages parentStage = findParentStage(node).asStages();
            String descId = node.getDescriptor().getId();
            if (STAGE_DESC_ID.equals(descId)) {
                FlowStage.Stages stage = new FlowStage.Sequence(node, parentStage);
                parentStage.addStage(stage);
                stages.put(stage.id(), stage);
                return stage;
            } else if (PARALLEL_DESC_ID.equals(descId) && node.isBody()) {
                StepStartNode nodeParent = getParentStepStartNode(node);
                if (PARALLEL_DESC_ID.equals(nodeParent.getDescriptor().getId()) && !nodeParent.isBody()) {
                    FlowStage.Parallel parallelStage;
                    String parentId = nodeParent.getId();
                    if (stages.containsKey(parentId)) {
                        parallelStage = stages.get(parentId).asParallel();
                    } else {
                        parallelStage = new FlowStage.Parallel(nodeParent, parentStage);
                        parentStage.addStage(parallelStage);
                        stages.put(parentId, parallelStage);
                    }
                    FlowStage.Stages stage = new FlowStage.Sequence(node, parallelStage);
                    parallelStage.addStage(stage);
                    stages.put(stage.id(), stage);
                    return stage;
                }
            }
        }
        return null;
    }

    /**
     * Get the given node parent as a {@link StepStartNode}.
     * @param node the node to process
     * @return StepStartNode
     * @throws IllegalStateException if the node is not a {@link StepStartNode} or has no parent
     */
    private StepStartNode getParentStepStartNode(FlowNode node) {
        List<FlowNode> parents = node.getParents();
        if (!parents.isEmpty()) {
            FlowNode parent = parents.get(0);
            if (parent instanceof StepStartNode) {
                return (StepStartNode) parent;
            }
            throw new IllegalStateException("not a step start node");
        }
        throw new IllegalStateException("node has no parents");
    }

    /**
     * Find the first stage in the enclosing blocks of the given node.
     * @param node the node to visit
     * @return FlowStage, never {@code null}
     */
    private FlowStage findParentStage(FlowNode node) {
        for (BlockStartNode parent : node.getEnclosingBlocks()) {
            FlowStage stage = stages.get(parent.getId());
            if (stage != null) {
                return stage;
            }
        }
        return root;
    }
}
