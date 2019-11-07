package io.helidon.jenkins.publisher.plugin;

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
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;
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
    private final boolean declaredOnly;
    private final boolean skipMeta;
    private final FlowRun run;
    private FlowStage.Sequence root;

    /**
     * Create a new flow graph.
     * @param signatures the signatures used to identify the step node that are declared
     * @param declaredOnly if {@code true} fire events only for declared steps
     * @param skipMeta if {@code true} fire events only for non meta steps
     */
    FlowGraph(FlowStepSignatures signatures, boolean declaredOnly, boolean skipMeta, FlowRun run) {
        Objects.requireNonNull(signatures, "signatures is null");
        this.signatures = signatures;
        this.steps = new HashMap<>();
        this.stages = new HashMap<>();
        this.declaredOnly = declaredOnly;
        this.skipMeta = skipMeta;
        this.run = run;
    }

    /**
     * Get the top level stage sequence.
     * @return StageSequence
     * @throw IllegalStateException if the root is not set yet
     */
    FlowStage.Sequence root() {
        if (root == null) {
            throw new IllegalStateException("Graph is empty");
        }
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
    void offer(FlowNode node, FlowEvent.Listener listener) {
        Objects.requireNonNull(node, "node is null");
        Objects.requireNonNull(listener, "listener is null");
        if (root == null) {
            root = new FlowStage.Sequence(run, findRootNode(node));
            stages.put(root.id, root);
            listener.onEvent(new FlowEvent.GlobalEvent(root, /* completed */ false));
        } else if ((node instanceof StepAtomNode)) {
            headNodes.addFirst((StepAtomNode) node);
            FlowStep step = createFlowStep((StepAtomNode)node);
            if (step.isIncluded(declaredOnly, skipMeta)) {
                listener.onEvent(new FlowEvent.StepEvent(step, /* completed */ true));
            }
            FlowStep previous = step.previous();
            if (previous != null) {
                if (previous.isIncluded(declaredOnly, skipMeta)) {
                    listener.onEvent(new FlowEvent.StepEvent(previous, /* completed */ true));
                }
            }
        } else if (node instanceof StepStartNode) {
            FlowStage.Stages stage = createFlowStages((StepStartNode) node);
            if (stage != null) {
                listener.onEvent(new FlowEvent.StageEvent(stage, /* completed */ false));
                FlowStage previous = stage.previous();
                if (previous != null) {
                    listener.onEvent(new FlowEvent.StageEvent(previous, /* completed */ true));
                }
            }
        } else if (node instanceof FlowEndNode) {
            listener.onEvent(new FlowEvent.GlobalEvent(root, /* completed */ true));
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
        String parentId = parentStage.id() + ".";
        for (int i = 1 ; stepsStage == null ; i++) {
            String stepsStageId = parentId +i;
            if (stages.containsKey(stepsStageId)) {
                stepsStage = stages.get(stepsStageId).asSteps();
                if (!stepsStage.head()) {
                    stepsStage = null;
                }
            } else {
                stepsStage = new FlowStage.Steps(run, stepsStageId, parentStage.node(), parentStage);
                parentStage.addStage(stepsStage);
                stages.put(stepsStageId, stepsStage);
            }
        }
        FlowStep step = new FlowStep(run, (StepAtomNode)node, stepsStage, signatures);
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
                FlowStage.Stages stage = new FlowStage.Sequence(run, node, parentStage);
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
                        parallelStage = new FlowStage.Parallel(run, nodeParent, parentStage);
                        parentStage.addStage(parallelStage);
                        stages.put(parentId, parallelStage);
                    }
                    FlowStage.Stages stage = new FlowStage.Sequence(run, node, parallelStage);
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
            throw new IllegalStateException("Invalid parent node");
        }
        throw new IllegalStateException("Node has no parent");
    }

    /**
     * Find the root node.
     * @param node the node to walk
     * @return FlowStartNode
     * @throws IllegalStateException if the root node is not found
     */
    private FlowStartNode findRootNode(FlowNode node) {
        List<FlowNode> parents = node.getParents();
        while (!parents.isEmpty()) {
            FlowNode parent = parents.get(0);
            if (parent instanceof FlowStartNode) {
                return (FlowStartNode) parent;
            } else if (parent != null) {
                parents = parent.getParents();
            }
        }
        throw new IllegalStateException("Root node not found");
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
