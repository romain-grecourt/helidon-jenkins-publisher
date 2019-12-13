package io.helidon.build.publisher.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.events.PipelineEvents.EventListener;
import io.helidon.build.publisher.model.PipelineInfo;
import io.helidon.build.publisher.model.Status;
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
import org.jenkinsci.plugins.workflow.support.steps.StageStep;

/**
 * Pipeline event adapter.
 */
final class PipelineModelAdapter {

    private static final Logger LOGGER = Logger.getLogger(PipelinePublisher.class.getName());
    private static final String STAGE_DESC_ID = StageStep.class.getName();
    private static final String PARALLEL_DESC_ID = ParallelStep.class.getName();

    private final PipelineSignatures signatures;
    private final LinkedList<StepAtomNode> headNodes = new LinkedList<>();
    private final Map<String, Pipeline.Step> steps;
    private final Map<String, Pipeline.Stage> stages;
    private final PipelineRunInfo runInfo;
    private final List<EventListener> listeners;
    private PipelineInfo run;

    /**
     * Create a new flow graph.
     * @param signatures the signatures used to identify the step node that are declared
     * @param runInfo run info
     * @param listener event listener
     */
    PipelineModelAdapter(PipelineSignatures signatures, PipelineRunInfo runInfo) {
        Objects.requireNonNull(signatures, "signatures is null");
        Objects.requireNonNull(runInfo, "runInfo is null");
        this.signatures = signatures;
        this.listeners = new LinkedList<>();
        this.steps = new HashMap<>();
        this.stages = new HashMap<>();
        this.runInfo = runInfo;
        this.run = null;
    }

    /**
     * Add a pipeline event listener.
     * @param listener listener to add
     * @throws NullPointerException if listener is {@code null}
     */
    void addEventListener(EventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener is null"));
    }

    /**
     * Get the underlying pipeline.
     * @return {@code Pipeline} or {@code null} if the run is not yet started
     */
    Pipeline pipeline() {
        if (run != null) {
            return run.pipeline();
        }
        return null;
    }

    /**
     * Get the pipeline run.
     * @return StageSequence
     * @throw IllegalStateException if the root is not set yet
     */
    PipelineInfo run() {
        if (run == null) {
            throw new IllegalStateException("Run is not started");
        }
        return run;
    }

    /**
     * Get the step for the given step id.
     * @param id step id
     * @return {@link Step} if found, or {@code null} if not found
     */
    Pipeline.Step step(String id) {
        return steps.get(id);
    }

    /**
     * Get the next unprocessed and included step at the head.
     * @return Step or {@code null} if there is no "included" and "unprocessed" step at the head
     */
    Pipeline.Step poll() {
        if (!headNodes.isEmpty()) {
            StepAtomNode node = headNodes.pollLast();
            Pipeline.Step step = steps.get(node.getId());
            if (isStepIncluded(step.declared(), step.meta())) {
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
        if (run == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Creating pipeline, runId={0}", runInfo.id);
            }
            Pipeline pipeline = new Pipeline(runInfo.id, new StatusImpl(node), new TimingsImpl(node));
            for (EventListener listener : listeners) {
                pipeline.addEventListener(listener);
            }
            run = new PipelineInfo(runInfo.id, runInfo.jobName, runInfo.repositoryUrl, runInfo.scmHead, runInfo.scmHash, pipeline);
            run.fireCreated();
        } else if ((node instanceof StepAtomNode)) {
            headNodes.addFirst((StepAtomNode) node);
            createStep((StepAtomNode)node);
        } else if (node instanceof StepStartNode) {
            createFlowStage((StepStartNode) node);
        }
    }

    private void createStep(StepAtomNode node) {
        Pipeline.Stage pstage = findParentStage(node);
        if (!(pstage instanceof Pipeline.Sequence)) {
            throw new IllegalStateException("Not a sequence stage");
        }
        String name = node.getDisplayFunctionName();
        String stepArgs = ArgumentsAction.getStepArgumentsAsString(node);
        String args = stepArgs != null ? stepArgs : "";
        boolean meta = node.getDescriptor().isMetaStep();
        String sig = Pipeline.Step.createPath(pstage.path(), name, args);
        boolean declared = signatures.contains(sig);
        Pipeline.Steps psteps = getOrCreateSteps((Pipeline.Sequence) pstage);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Creating step, runId={0}, signature={1}", new Object[]{
                runInfo.id,
                sig,
            });
        }
        Pipeline.Step step = new Pipeline.Step(psteps, name, truncateStepArgs(args), meta, declared,
                new StatusImpl(node), new TimingsImpl(node));
        steps.put(node.getId(), step);
        if (!isStepIncluded(declared, meta)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Excluding step, runId={0}, signature={1}, stepId={2}, ", new Object[]{
                    node.getId(),
                    runInfo.id,
                    sig
                });
            }
        } else {
            psteps.addStep(step);
            step.fireCreated();
        }
    }

    private boolean isStepIncluded(boolean declared, boolean meta) {
        return ((runInfo.excludeSyntheticSteps && declared) || (!runInfo.excludeSyntheticSteps && declared))
                && ((runInfo.excludeMetaSteps && !meta) || (!runInfo.excludeMetaSteps && meta));
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

    private Pipeline.Steps getOrCreateSteps(Pipeline.Sequence parent) {
        String parentId = parent.id() + ".";
        for (int i = 1 ;; i++) {
            String stepsStageId = parentId + i;
            if (stages.containsKey(stepsStageId)) {
                Pipeline.Stage stage = stages.get(stepsStageId);
                if (!(stage instanceof Pipeline.Steps)) {
                    throw new IllegalStateException("Not a steps stage");
                }
                Pipeline.Steps psteps = (Pipeline.Steps) stage;
                if (psteps.head()) {
                    return psteps;
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating steps stage, runId={0}, parentId={1}", new Object[]{
                        runInfo.id,
                        parent.id()
                    });
                }
                Pipeline.Steps psteps = new Pipeline.Steps(parent, new StatusImpl(), new TimingsImpl());
                parent.addStage(psteps);
                stages.put(stepsStageId, psteps);
                psteps.fireCreated();
                return psteps;
            }
        }
    }

    private void createFlowStage(StepStartNode node) {
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
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating sequence stage, runId={0}, parentId={1}", new Object[]{
                        runInfo.id,
                        pstages.id()
                    });
                }
                Pipeline.Stages sequence = new Pipeline.Sequence(pstages, name, new StatusImpl(node), new TimingsImpl(node));
                pstages.addStage(sequence);
                stages.put(node.getId(), sequence);
                sequence.fireCreated();
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
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Creating parallel stage, runId={0}, parentId={1}, name={2}", new Object[]{
                                runInfo.id,
                                pstages.id(),
                                name
                            });
                        }
                        parallel = new Pipeline.Parallel(pstages, name, new StatusImpl(pnode), new TimingsImpl(pnode));
                        pstages.addStage(parallel);
                        stages.put(parentId, parallel);
                        parallel.fireCreated();
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Creating sequence stage, runId={0}, parentId={1}, name={2}", new Object[]{
                            runInfo.id,
                            parallel.id(),
                            name
                        });
                    }
                    Pipeline.Sequence sequence = new Pipeline.Sequence(parallel, name, new StatusImpl(node), new TimingsImpl(node));
                    parallel.addStage(sequence);
                    stages.put(node.getId(), sequence);
                    sequence.fireCreated();
                }
            }
        }
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
            if (super.endTime == 0 && source != null && source instanceof BlockStartNode) {
                BlockEndNode endNode = ((BlockStartNode) source).getEndNode();
                if (endNode != null) {
                    super.endTime = TimingAction.getStartTime(endNode);
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
