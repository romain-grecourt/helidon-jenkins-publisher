package io.helidon.build.publisher.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.Status;
import io.helidon.build.publisher.model.Step;
import io.helidon.build.publisher.model.Steps;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;

import hudson.Extension;
import hudson.model.Run;
import hudson.tasks.junit.SuiteResult;
import io.helidon.build.publisher.plugin.config.HelidonPublisherServer;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Pipeline publisher.
 * Uses {@link TaskListenerDecorator}, {@link GraphListener.Synchronous} and {@link FlowExecutionListener}.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.class})
final class PipelinePublisher extends TaskListenerDecorator implements GraphListener.Synchronous {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PipelinePublisher.class.getName());
    private static final EmptyPublisher EMPTY_PUBLISHER = new EmptyPublisher();
    private static final Map<FlowExecution, WeakReference<TaskListenerDecorator>> PUBLISHERS = new WeakHashMap<>();
    private static final ArtifactsProcessor.Factory ARTIFACTS_PROCESSOR_FACTORY = new ArtifactsProcessorFactory();

    private transient PipelineModelAdapter modelAdapter;
    private transient BackendClient client;
    private transient Pipeline pipeline;
    private final String pipelineId;
    private final boolean excludeSyntheticSteps;
    private final boolean excludeMetaSteps;
    private final boolean enabled;

    PipelinePublisher(FlowExecution execution) {
        PipelineRunInfo runInfo = new PipelineRunInfo(execution);
        WorkflowRun run = Helper.getRun(execution.getOwner());
        if (run.isBuilding() && runInfo.id != null) {
            enabled = true;
            String pkey = HelidonPublisherServer.lookupCredentials(runInfo.credentialsId, runInfo.publisherServerUrl);
            client = BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads, pkey);
            excludeSyntheticSteps = runInfo.excludeSyntheticSteps;
            excludeMetaSteps = runInfo.excludeMetaSteps;
            pipelineId = runInfo.id;
            pipeline = new Pipeline(runInfo.toPipelineInfo(new GlobalStatus(run), new GlobalTimings(run)));
            modelAdapter = new PipelineModelAdapter(PipelineSignatures.getOrCreate(execution), pipeline, excludeSyntheticSteps,
                    excludeMetaSteps);
            pipeline.addEventListener(client);
            pipeline.addEventListener(new TestResulProcessor(pipeline, client, run, new TestResultSuiteMatcher(modelAdapter)));
            pipeline.fireCreated();
            ArtifactsProcessor.register(ARTIFACTS_PROCESSOR_FACTORY);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline enabled, execution={0}, pipelineId={1}", new Object[]{
                    execution,
                    pipelineId
                });
            }
        } else {
            enabled = false;
            excludeMetaSteps = false;
            excludeSyntheticSteps = false;
            pipelineId = null;
            pipeline = null;
            modelAdapter = null;
            client = null;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline NOT enabled, execution={0}", execution);
            }
        }
    }

    @Override
    public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
        if (enabled) {
            Step step = modelAdapter.poll();
            if (step != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Decorating output, pipelineId={0}, step={1}", new Object[]{
                        pipelineId,
                        step
                    });
                }
                return new PipelineOutputStream(out, pipelineId, step, client);
            }
        }
        return out;
    }

    @Override
    public void onNewHead(FlowNode node) {
        if (enabled) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "New head, pipelineId={0}, node={1}", new Object[]{
                    pipelineId,
                    node
                });
            }
            modelAdapter.offer(node);
        }
    }

    /**
     * Get the pipeline id for this publisher instance.
     * @return String, {@code null} if not enabled
     */
    String pipelineId() {
        return pipelineId;
    }

    /**
     * Get a pipeline publisher for the given run.
     * @param run workflow run
     * @return PipelinePublisher or {@code null} if not found
     */
    static PipelinePublisher get(WorkflowRun run) {
        WeakReference<TaskListenerDecorator> ref = PUBLISHERS.get(run.getExecution());
        if (ref != null && ref.get() instanceof PipelinePublisher) {
            return (PipelinePublisher) ref.get();
        }
        return null;
    }

    /**
     * TaskListenerDecorator factory.
     */
    @SuppressRestrictedWarnings({TaskListenerDecorator.Factory.class, TaskListenerDecorator.class})
    @Extension
    public static final class Factory implements TaskListenerDecorator.Factory {

        @Override
        public TaskListenerDecorator of(FlowExecutionOwner owner) {
            FlowExecution execution = owner.getOrNull();
            if (execution == null) {
                return EMPTY_PUBLISHER;
            }
            synchronized (PUBLISHERS) {
                WeakReference<TaskListenerDecorator> decoratorRef = PUBLISHERS.get(execution);
                if (decoratorRef != null && decoratorRef.get() != null) {
                    return decoratorRef.get();
                }
            }
            PipelinePublisher pipelinePublisher = new PipelinePublisher(execution);
            synchronized (PUBLISHERS) {
                WeakReference<TaskListenerDecorator> decoratorRef = PUBLISHERS.get(execution);
                if (decoratorRef != null && decoratorRef.get() != null) {
                    return decoratorRef.get();
                }
                TaskListenerDecorator dec;
                if (pipelinePublisher.pipeline != null) {
                    dec = pipelinePublisher;
                    execution.addListener(pipelinePublisher);
                } else {
                    dec = EMPTY_PUBLISHER;
                }
                PUBLISHERS.put(execution, new WeakReference<>(dec));
                return dec;
            }
        }
    }

    /**
     * No-op decorator.
     */
    @SuppressRestrictedWarnings(TaskListenerDecorator.class)
    private static final class EmptyPublisher extends TaskListenerDecorator {

        @Override
        public OutputStream decorate(OutputStream logger) throws IOException, InterruptedException {
            return logger;
        }
    }

    /**
     * {@link FlowExecutionListener} implementation used to complete the pipeline status and clear the cache.
     */
    @Extension
    public static final class ExecutionListener extends FlowExecutionListener {

        @Override
        public void onCompleted(FlowExecution execution) {
            synchronized (PUBLISHERS) {
                WeakReference<TaskListenerDecorator> ref = PUBLISHERS.remove(execution);
                if (ref != null) {
                    TaskListenerDecorator decorator = ref.get();
                    if (decorator instanceof PipelinePublisher) {
                        PipelinePublisher pipelinePublisher = (PipelinePublisher) decorator;
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Pipeline completed, pipelineId={0}, pipeline={1}", new Object[]{
                                pipelinePublisher.pipelineId,
                                "\n" + pipelinePublisher.pipeline.toPrettyString(pipelinePublisher.excludeSyntheticSteps,
                                    pipelinePublisher.excludeSyntheticSteps)
                            });
                        }
                        pipelinePublisher.pipeline.fireCompleted();
                        return;
                    }
                }
            }
            // force the result, just in case the publisher got gced.
            WorkflowRun run = Helper.getRun(execution.getOwner());
            Status.Result result = Helper.convertResult(run.getResult());
            PipelineRunInfo runInfo = new PipelineRunInfo(execution);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Forcing pipeline result: pipelineId={0}, result={1}", new Object[]{
                    runInfo.id,
                    result
                });
            }
            String pkey = HelidonPublisherServer.lookupCredentials(runInfo.credentialsId, runInfo.publisherServerUrl);
            BackendClient client = BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads, pkey);
            client.onEvent(new PipelineCompletedEvent(runInfo.id, result, run.getDuration()));
        }

        @Override
        public void onResumed(FlowExecution execution) {
        }

        @Override
        public void onRunning(FlowExecution execution) {
        }
    }

    /**
     * Artifact processor factory.
     */
    private static final class ArtifactsProcessorFactory implements ArtifactsProcessor.Factory {

        @Override
        public ArtifactsProcessor create(Run<?, ?> run) {
            if (run instanceof WorkflowRun) {
                FlowExecution exec = ((WorkflowRun) run).getExecution();
                WeakReference<TaskListenerDecorator> ref = PipelinePublisher.PUBLISHERS.get(exec);
                TaskListenerDecorator decorator = ref != null ? ref.get() : null;
                if (decorator instanceof PipelinePublisher && ((PipelinePublisher) decorator).enabled) {
                    PipelinePublisher publisher = (PipelinePublisher) decorator;
                    ArchivedArtifactsStepsProvider stepsProvider = new ArchivedArtifactsStepsProvider(publisher, exec);
                    return new ArtifactsProcessor(run, publisher.client, publisher.pipelineId, stepsProvider);
                }
            }
            return null;
        }
    }

    /**
     * Archived artifacts steps provider.
     */
    private static final class ArchivedArtifactsStepsProvider implements ArtifactsProcessor.StepsProvider {

        private final FlowExecution exec;
        private final PipelinePublisher pipelinePublisher;

        ArchivedArtifactsStepsProvider(PipelinePublisher pipelinePublisher, FlowExecution exec) {
            this.pipelinePublisher = Objects.requireNonNull(pipelinePublisher, "pipelinePublisher is null!");
            this.exec = Objects.requireNonNull(exec, "exec is null!");
        }

        @Override
        public Steps getSteps() {
            for (FlowNode node : exec.getCurrentHeads()) {
                if (("archiveArtifacts").equals(node.getDisplayFunctionName())) {
                    Step step = pipelinePublisher.modelAdapter.step(node.getId());
                    if (step != null) {
                        return (Steps) step.parent();
                    }
                }
            }
            return null;
        }
    }

    /**
     * Test result suite matcher.
     */
    private static final class TestResultSuiteMatcher implements TestResulProcessor.SuiteResultMatcher {

        private final PipelineModelAdapter modelAdapter;

        TestResultSuiteMatcher(PipelineModelAdapter adapter) {
            this.modelAdapter = adapter;
        }

        @Override
        public boolean match(SuiteResult suite, Steps steps) {
            String nodeId = suite.getNodeId();
            if (nodeId != null) {
                Step step = modelAdapter.step(nodeId);
                if (step != null && step.parent().equals(steps)) {
                    return true;
                }
            }
            return false;
        }
    }
}
