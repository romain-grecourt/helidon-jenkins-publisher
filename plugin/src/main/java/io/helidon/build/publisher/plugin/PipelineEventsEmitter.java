package io.helidon.build.publisher.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.PipelineEvents;
import io.helidon.build.publisher.model.PipelineRun;
import io.helidon.build.publisher.model.Status;
import io.helidon.build.publisher.model.Timings;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

/**
 * Pipeline event emitter.
 */
final class PipelineEventsEmitter {

    private static final Logger LOGGER = Logger.getLogger(FlowDecorator.class.getName());
    private static final String STAGE_DESC_ID = StageStep.class.getName();
    private static final String PARALLEL_DESC_ID = ParallelStep.class.getName();

    private final PipelineSignatures signatures;
    private final LinkedList<StepAtomNode> headNodes = new LinkedList<>();
    private final Map<String, Pipeline.Step> steps;
    private final Map<String, Pipeline.Stage> stages;
    private final PipelineRunInfo runInfo;
    private PipelineRun run;

    /**
     * Create a new flow graph.
     * @param signatures the signatures used to identify the step node that are declared
     * @param runInfo run info
     */
    PipelineEventsEmitter(PipelineSignatures signatures, PipelineRunInfo runInfo) {
        Objects.requireNonNull(signatures, "signatures is null");
        Objects.requireNonNull(runInfo, "runInfo is null");
        this.signatures = signatures;
        this.steps = new HashMap<>();
        this.stages = new HashMap<>();
        this.runInfo = runInfo;
        this.run = null;
    }

    /**
     * Get the top level stage sequence.
     * @return StageSequence
     * @throw IllegalStateException if the root is not set yet
     */
    PipelineRun run() {
        if (run == null) {
            throw new IllegalStateException("Run is not started");
        }
        return run;
    }

    /**
     * Get the step for the given step id.
     * @param id step id
     * @return {@link FlowStep} if found, or {@code null} if not found
     */
    Pipeline.Step step(String id) {
        return steps.get(id);
    }

    /**
     * Get the next unprocessed step at the head.
     * @return FlowStep or {@code null} if there is no unprocessed step at the head
     */
    Pipeline.Step poll() {
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
     * @throws NullPointerException if node or listener is {@code null}
     */
    void offer(FlowNode node, PipelineEvents.EventListener listener) {
        Objects.requireNonNull(node, "node is null");
        Objects.requireNonNull(listener, "listener is null");
        if (run == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Creating pipeline, runId={0}", runInfo.id);
            }
            Pipeline pipeline = new Pipeline(new StatusImpl(node), new TimingsImpl(node));
            run = new PipelineRun(runInfo.id, runInfo.jobName, runInfo.scmHead, runInfo.scmHash, pipeline);
            run.fireEvent(listener, PipelineEvents.NodeEventType.CREATED, runInfo.id);
        } else if ((node instanceof StepAtomNode)) {
            headNodes.addFirst((StepAtomNode) node);
            Pipeline.Step step = createStep((StepAtomNode)node);
            if (step.isIncluded(runInfo.excludeSyntheticSteps, runInfo.excludeMetaSteps)) {
                step.fireEvent(listener, PipelineEvents.NodeEventType.CREATED, runInfo.id);
            }
            Pipeline.Step previous = step.previous();
            if (previous != null) {
                if (previous.isIncluded(runInfo.excludeSyntheticSteps, runInfo.excludeMetaSteps)) {
                    previous.fireEvent(listener, PipelineEvents.NodeEventType.COMPLETED, runInfo.id);
                }
            }
        } else if (node instanceof StepStartNode) {
            Pipeline.Stages stage = createFlowStage((StepStartNode) node);
            if (stage != null) {
                stage.fireEvent(listener, PipelineEvents.NodeEventType.CREATED, runInfo.id);
                Pipeline.Stage previous = stage.previous();
                if (previous != null) {
                    previous.fireEvent(listener, PipelineEvents.NodeEventType.CREATED, runInfo.id);
                }
            }
        } else if (node instanceof FlowEndNode) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Completing pipeline, runId={0}", runInfo.id);
            }
            run.fireEvent(listener, PipelineEvents.NodeEventType.COMPLETED, runInfo.id);
        }
    }

    private Pipeline.Step createStep(StepAtomNode node) {
        Pipeline.Stage pstage = findParentStage(node);
        if (!(pstage instanceof Pipeline.Sequence)) {
            throw new IllegalStateException("Not a sequence stage");
        }
        Pipeline.Sequence psequence = (Pipeline.Sequence) pstage;
        Pipeline.Steps psteps = null;
        String parentId = pstage.id() + ".";
        for (int i = 1 ; psteps == null ; i++) {
            String stepsStageId = parentId +i;
            if (stages.containsKey(stepsStageId)) {
                Pipeline.Stage stage = stages.get(stepsStageId);
                if (!(stage instanceof Pipeline.Steps)) {
                    throw new IllegalStateException("Not a steps stage");
                }
                psteps = (Pipeline.Steps) stage;
                if (!psteps.head()) {
                    psteps = null;
                }
            } else {
                psteps = new Pipeline.Steps(pstage, new StatusImpl(), new TimingsImpl());
                psequence.addStage(psteps);
                stages.put(stepsStageId, psteps);
            }
        }
        String name = node.getDisplayFunctionName();
        String stepArgs = ArgumentsAction.getStepArgumentsAsString(node);
        String args = stepArgs != null ? stepArgs : "";
        boolean meta = node.getDescriptor().isMetaStep();
        String sig = Pipeline.Step.createPath(psteps.path(), name, args);
        boolean declared = signatures.contains(sig);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Creating pipeline step, runId={0}, signature={1}, declared={2}", new Object[]{
                runInfo.id,
                sig,
                declared
            });
        }
        Pipeline.Step step = new Pipeline.Step(psteps, name, args, meta, declared, new StatusImpl(node),
                new TimingsImpl(node));
        psteps.addStep(step);
        steps.put(node.getId(), step);
        return step;
    }

    private Pipeline.Stages createFlowStage(StepStartNode node) {
        if (node.getAction(LabelAction.class) != null) {
            Pipeline.Stage pstage = findParentStage(node);
            if (!(pstage instanceof Pipeline.Stages)) {
                throw new IllegalStateException("Not a multi stage");
            }
            Pipeline.Stages pstages = (Pipeline.Stages) pstage;
            String descId = node.getDescriptor().getId();
            String name = null;
            LabelAction action = node.getAction(LabelAction.class);
            if (action != null) {
                if (action instanceof ThreadNameAction) {
                    name = ((ThreadNameAction)action).getThreadName();
                } else {
                    name = action.getDisplayName();
                }
            }
            if (STAGE_DESC_ID.equals(descId)) {
                Pipeline.Stages sequence = new Pipeline.Sequence(pstages, name, new StatusImpl(node), new TimingsImpl(node));
                pstages.addStage(sequence);
                stages.put(node.getId(), sequence);
                return sequence;
            } else if (PARALLEL_DESC_ID.equals(descId) && node.isBody()) {
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
                if (PARALLEL_DESC_ID.equals(pnode.getDescriptor().getId()) && !pnode.isBody()) {
                    Pipeline.Parallel parallel;
                    String parentId = pnode.getId();
                    if (stages.containsKey(parentId)) {
                        Pipeline.Stage ppstage = stages.get(parentId);
                        if (!(ppstage instanceof Pipeline.Parallel)) {
                            throw new IllegalStateException("Not a parallel stage");
                        }
                        parallel = (Pipeline.Parallel) ppstage;
                    } else {
                        parallel = new Pipeline.Parallel(pstages, name, new StatusImpl(pnode), new TimingsImpl(pnode));
                        pstages.addStage(parallel);
                        stages.put(parentId, parallel);
                    }
                    Pipeline.Sequence sequence = new Pipeline.Sequence(parallel, name, new StatusImpl(node), new TimingsImpl(node));
                    parallel.addStage(sequence);
                    stages.put(node.getId(), sequence);
                    return sequence;
                }
            }
        }
        return null;
    }

    private Pipeline.Stage findParentStage(FlowNode node) {
        for (BlockStartNode parent : node.getEnclosingBlocks()) {
            Pipeline.Stage stage = stages.get(parent.getId());
            if (stage != null) {
                return stage;
            }
        }
        return run.pipeline().stages();
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
            super(System.currentTimeMillis());
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
        protected long computeEndTime() {
            if (source != null && source instanceof BlockStartNode) {
                BlockEndNode endNode = ((BlockStartNode) source).getEndNode();
                if (endNode != null) {
                    return TimingAction.getStartTime(endNode);
                }
            }
            return 0;
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
        protected void refresh(Pipeline.Node node) {
            if (source == null) {
                super.refresh(node);
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
                state = source.isActive() ? State.RUNNING : State.FINISHED;
            } else if (warningAction != null) {
                result = Helper.convertResult(warningAction.getResult());
                state = source.isActive() ? State.RUNNING : State.FINISHED;
            } else if (QueueItemAction.getNodeState(source) == QueueItemAction.QueueState.QUEUED) {
                result = Result.UNKNOWN;
                state = State.QUEUED;
            } else if (QueueItemAction.getNodeState(source) == QueueItemAction.QueueState.CANCELLED) {
                result = Result.ABORTED;
                state = State.FINISHED;
            } else if (source.isActive()) {
                result = Result.UNKNOWN;
                state = State.RUNNING;
            } else if (NotExecutedNodeAction.isExecuted(source)) {
                result = Result.SUCCESS;
                state = State.FINISHED;
            } else {
                result = Result.NOT_BUILT;
                state = State.QUEUED;
            }
        }
    }
}
