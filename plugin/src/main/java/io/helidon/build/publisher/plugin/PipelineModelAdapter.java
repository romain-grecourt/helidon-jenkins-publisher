package io.helidon.build.publisher.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.Parallel;
import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.Sequence;
import io.helidon.build.publisher.model.Stage;
import io.helidon.build.publisher.model.Stages;
import io.helidon.build.publisher.model.Status;
import io.helidon.build.publisher.model.Step;
import io.helidon.build.publisher.model.Steps;
import io.helidon.build.publisher.model.Timings;

import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.QueueItemAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.cps.steps.ParallelStep;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

/**
 * Pipeline event adapter.
 */
final class PipelineModelAdapter {

    private static final Logger LOGGER = Logger.getLogger(PipelineModelAdapter.class.getName());
    private static final String STAGE_DESC_ID = StageStep.class.getName();
    private static final String PARALLEL_DESC_ID = ParallelStep.class.getName();

    private final PipelineSignatures signatures;
    private final LinkedList<StepAtomNode> headNodes = new LinkedList<>();
    private final Map<String, Step> steps;
    private final Map<String, Stage> stages;
    private final Pipeline pipeline;
    private final String pipelineId;
    private final boolean excludeSyntheticSteps;
    private final boolean excludeMetaSteps;

    /**
     * Create a new flow graph.
     * @param signatures the signatures used to identify the step node that are declared
     * @param pipeline the pipeline
     * @param excludeSyntheticSteps {@code true} to exclude synthetic steps
     * @param excludeMetaSteps {@code true} to exclude meta steps
     * @throws NullPointerException if signatures or pipeline is {@code null}
     */
    PipelineModelAdapter(PipelineSignatures signatures, Pipeline pipeline, boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
        Objects.requireNonNull(signatures, "signatures is null");
        Objects.requireNonNull(pipeline, "pipeline is null");
        this.signatures = signatures;
        this.pipeline = pipeline;
        this.pipelineId = pipeline.pipelineId();
        this.excludeSyntheticSteps = excludeSyntheticSteps;
        this.excludeMetaSteps = excludeMetaSteps;
        this.steps = new HashMap<>();
        this.stages = new HashMap<>();
    }

    /**
     * Get the underlying pipeline.
     * @return Pipeline
     */
    Pipeline pipeline() {
        return pipeline;
    }

    /**
     * Get the step for the given step id.
     * @param id step id
     * @return {@link Step} if found, or {@code null} if not found
     */
    Step step(String id) {
        return steps.get(id);
    }

    /**
     * Get the next unprocessed and included step at the head.
     * @return Step or {@code null} if there is no "included" and "unprocessed" step at the head
     */
    Step poll() {
        if (!headNodes.isEmpty()) {
            StepAtomNode node = headNodes.pollLast();
            Step step = steps.get(node.getId());
            if (step != null && step.isIncluded(excludeSyntheticSteps, excludeMetaSteps)) {
                return step;
            }
        }
        return null;
    }

    /**
     * Store the new current head and fires the status events if any.
     * @param node new head
     * @throws NullPointerException if node or listener is {@code null}
     */
    void offer(FlowNode node) {
        Objects.requireNonNull(node, "node is null");
        if ((node instanceof StepAtomNode)) {
            headNodes.addFirst((StepAtomNode) node);
            createStep((StepAtomNode)node);
        } else if (node instanceof StepStartNode) {
            createStage((StepStartNode) node);
        }
    }

    private void createStep(StepAtomNode node) {
        Stage pstage = findParentStage(node);
        if (!(pstage instanceof Sequence)) {
            throw new IllegalStateException("Not a sequence stage");
        }
        String name = node.getDisplayFunctionName();
        String stepArgs = ArgumentsAction.getStepArgumentsAsString(node);
        String args = stepArgs != null ? stepArgs : "";
        StepDescriptor nodeDesc = node.getDescriptor();
        boolean meta = nodeDesc != null ? nodeDesc.isMetaStep() : true;
        String sig = Step.createPath(pstage.path(), name, args);
        boolean declared = signatures.contains(sig);
        Steps psteps = getOrCreateSteps((Sequence) pstage);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Creating step, pipelineId={0}, signature={1}, declared={2}", new Object[]{
                pipelineId,
                sig,
                declared
            });
        }
        Step step = new Step(psteps, name, truncateStepArgs(args), meta, declared, new StatusImpl(node), new TimingsImpl(node));
        steps.put(node.getId(), step);
        if (!step.isIncluded(excludeSyntheticSteps, excludeMetaSteps)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Excluding step, pipelineId={0}, signature={1}, stepId={2}, ", new Object[]{
                    node.getId(),
                    pipelineId,
                    sig
                });
            }
        } else {
            psteps.addStep(step);
            step.fireCreated();
        }
    }

    private String truncateStepArgs(String args) {
        String[] argsLines = args.trim().split("[\\r\\n]+");
        for (String argsLine : argsLines) {
            argsLine = argsLine.trim();
            if (argsLine.isEmpty()) {
                continue;
            }
            if (argsLine.length() > 50) {
                return argsLine.substring(0, 50) + " [...]";
            }
            return argsLine;
        }
        return "";
    }

    private Steps getOrCreateSteps(Sequence parent) {
        String parentId = parent.id() + ".";
        for (int i = 1 ;; i++) {
            String stepsStageId = parentId + i;
            if (stages.containsKey(stepsStageId)) {
                Stage stage = stages.get(stepsStageId);
                if (!(stage instanceof Steps)) {
                    throw new IllegalStateException("Not a steps stage");
                }
                Steps psteps = (Steps) stage;
                if (psteps.head()) {
                    return psteps;
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating steps stage, pipelineId={0}, parentId={1}", new Object[]{
                        pipelineId,
                        parentId
                    });
                }
                Steps psteps = new Steps(parent, new StatusImpl(), new TimingsImpl());
                parent.addStage(psteps);
                stages.put(stepsStageId, psteps);
                psteps.fireCreated();
                return psteps;
            }
        }
    }

    private void createStage(StepStartNode node) {
        if (node.getAction(LabelAction.class) == null) {
            return;
        }
        Stage pstage = findParentStage(node);
        if (!(pstage instanceof Stages)) {
            throw new IllegalStateException("Not a multi stage");
        }
        Stages pstages = (Stages) pstage;
        StepDescriptor nodeDesc = node.getDescriptor();
        String nodeDescId = nodeDesc != null ? nodeDesc.getId() : null;
        String name = null;
        LabelAction action = node.getAction(LabelAction.class);
        if (action != null) {
            if (action instanceof ThreadNameAction) {
                name = ((ThreadNameAction)action).getThreadName();
            } else {
                name = action.getDisplayName();
            }
        }
        if (STAGE_DESC_ID.equals(nodeDescId)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Creating sequence stage, pipelineId={0}, parentId={1}", new Object[]{
                    pipelineId,
                    pstages.id()
                });
            }
            Stages sequence = new Sequence(pstages, name, new StatusImpl(node), new TimingsImpl(node));
            pstages.addStage(sequence);
            stages.put(node.getId(), sequence);
            sequence.fireCreated();
        } else if (PARALLEL_DESC_ID.equals(nodeDescId) && node.isBody()) {
            StepStartNode pnode;
            List<FlowNode> parents = node.getParents();
            if (!parents.isEmpty()) {
                FlowNode parent = parents.get(0);
                if (parent instanceof StepStartNode) {
                    pnode = (StepStartNode) parent;
                } else {
                    throw new IllegalStateException("Invalid parent node");
                }
            } else {
                throw new IllegalStateException("Node has no parent");
            }
            StepDescriptor pnodeDesc = pnode.getDescriptor();
            String pnodeDescId = pnodeDesc != null ? pnodeDesc.getId() : null;
            if (PARALLEL_DESC_ID.equals(pnodeDescId) && !pnode.isBody()) {
                Parallel parallel;
                String parentId = pnode.getId();
                if (stages.containsKey(parentId)) {
                    Stage ppstage = stages.get(parentId);
                    if (!(ppstage instanceof Parallel)) {
                        throw new IllegalStateException("Not a parallel stage");
                    }
                    parallel = (Parallel) ppstage;
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Creating parallel stage, pipelineId={0}, parentId={1}, name={2}", new Object[]{
                            pipelineId,
                            pstages.id(),
                            name
                        });
                    }
                    parallel = new Parallel(pstages, name, new StatusImpl(pnode), new TimingsImpl(pnode));
                    pstages.addStage(parallel);
                    stages.put(parentId, parallel);
                    parallel.fireCreated();
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating sequence stage, pipelineId={0}, parentId={1}, name={2}", new Object[]{
                        pipelineId,
                        parallel.id(),
                        name
                    });
                }
                Sequence sequence = new Sequence(parallel, name, new StatusImpl(node), new TimingsImpl(node));
                parallel.addStage(sequence);
                stages.put(node.getId(), sequence);
                sequence.fireCreated();
            }
        }
    }

    private Stage findParentStage(FlowNode node) {
        for (BlockStartNode parent : node.getEnclosingBlocks()) {
            Stage stage = stages.get(parent.getId());
            if (stage != null) {
                return stage;
            }
        }
        return pipeline;
    }

    /**
     * {@link Timings} implementation that can compute the end time from the graph.
     */
    private static final class TimingsImpl extends Timings {

        private final FlowNode source;

        /**
         * Create a new timing with start time set to the current time.
         */
        TimingsImpl() {
            super();
            this.source = null;
        }

        /**
         * Create a new timing with start time derived from the given {@link FlowNode}.
         * @param source 
         */
        TimingsImpl(FlowNode source) {
            super(TimingAction.getStartTime(source));
            this.source = source;
        }

        @Override
        protected void refresh() {
            if (endTime == 0 && source != null && source instanceof BlockStartNode) {
                BlockEndNode endNode = ((BlockStartNode) source).getEndNode();
                if (endNode != null) {
                    long time = TimingAction.getStartTime(endNode);
                    if (time > startTime) {
                        endTime = time;
                    }
                }
            }
        }
    }

    /**
     * {@link Status} implementation that can compute the state and result from the original {@link FlowNode}.
     */
    private static final class StatusImpl extends Status {

        private final FlowNode source;

        StatusImpl() {
            super();
            this.source = null;
        }

        StatusImpl(FlowNode node) {
            super();
            this.source = node;
        }

        @Override
        protected void refresh() {
            if (source == null) {
                return;
            }
            hudson.model.Result res = null;
            ErrorAction errorAction = source.getError();
            WarningAction warningAction = source.getPersistentAction(WarningAction.class);
            if (errorAction != null) {
                if (errorAction.getError() instanceof FlowInterruptedException) {
                    res = ((FlowInterruptedException) errorAction.getError()).getResult();
                }
                if (res == null || res != hudson.model.Result.ABORTED) {
                    result = Result.FAILURE;
                } else {
                    result = Result.ABORTED;
                }
            } else if (warningAction != null) {
                result = Helper.convertResult(warningAction.getResult());
            } else if (QueueItemAction.getNodeState(source) == QueueItemAction.QueueState.QUEUED) {
                result = Result.UNKNOWN;
            } else if (QueueItemAction.getNodeState(source) == QueueItemAction.QueueState.CANCELLED) {
                result = Result.ABORTED;
            } else if (source.isActive()) {
                result = Result.UNKNOWN;
            } else if (NotExecutedNodeAction.isExecuted(source)) {
                result = Result.SUCCESS;
            } else {
                result = Result.NOT_BUILT;
            }
        }
    }
}
