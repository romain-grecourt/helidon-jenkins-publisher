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
import io.helidon.build.publisher.model.events.PipelineEvents;
import io.helidon.build.publisher.model.Status;

import hudson.Extension;
import hudson.model.Run;
import hudson.tasks.junit.SuiteResult;
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

    private static final Logger LOGGER = Logger.getLogger(PipelinePublisher.class.getName());
    private static final EmptyPublisher EMPTY_PUBLISHER = new EmptyPublisher();
    private static final Map<FlowExecution, WeakReference<TaskListenerDecorator>> PUBLISHERS = new WeakHashMap<>();
    private static final ArtifactsProcessor.Factory ARTIFACTS_PROCESSOR_FACTORY = new ArtifactsProcessorFactory();

    private final PipelineModelAdapter modelAdapter;
    private final BackendClient client;
    private final PipelineRunInfo runInfo;

    PipelinePublisher(FlowExecution execution) {
        runInfo = new PipelineRunInfo(execution);
        if (runInfo.isEnabled()) {
            PipelineSignatures signatures = PipelineSignatures.getOrCreate(execution);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline enabled, runInfo={0}, signatures: {1}", new Object[]{
                    runInfo,
                    signatures.signatures
                });
            }
            client = BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads);
            modelAdapter = new PipelineModelAdapter(signatures, runInfo);
            modelAdapter.addEventListener(client);
            TestResulProcessor testResultProcessor = new TestResulProcessor(modelAdapter::run, client,
                    Helper.getRun(execution.getOwner()), new TestResultSuiteMatcher(modelAdapter));
            modelAdapter.addEventListener(testResultProcessor);
            ArtifactsProcessor.register(ARTIFACTS_PROCESSOR_FACTORY);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline NOT enabled, runInfo={0}", runInfo);
            }
            modelAdapter = null;
            client = null;
        }
    }

    @Override
    public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
        if (runInfo.isEnabled()) {
            Pipeline.Step step = modelAdapter.poll();
            if (step != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Decorating output, runId={0}, step={1}", new Object[]{
                        runInfo.id,
                        step
                    });
                }
                return new PipelineOutputStream(out, runInfo.id, step, client);
            }
        }
        return out;
    }

    @Override
    public void onNewHead(FlowNode node) {
        if (runInfo.isEnabled()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "New head, runId{0}, node={1}", new Object[]{
                    runInfo.id,
                    node
                });
            }
            modelAdapter.offer(node);
        }
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
            PipelinePublisher decorator = new PipelinePublisher(execution);
            synchronized (PUBLISHERS) {
                WeakReference<TaskListenerDecorator> decoratorRef = PUBLISHERS.get(execution);
                if (decoratorRef != null && decoratorRef.get() != null) {
                    return decoratorRef.get();
                }
                TaskListenerDecorator dec;
                if (decorator.runInfo.isEnabled()) {
                    dec = decorator;
                    execution.addListener(decorator);
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
                    if (decorator != null) {
                        PipelinePublisher pipelinePublisher = (PipelinePublisher) decorator;
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Pipeline completed, runId={0}, pipeline={1}", new Object[]{
                                pipelinePublisher.runInfo.id,
                                pipelinePublisher.modelAdapter.run().prettyPrint(pipelinePublisher.runInfo.excludeSyntheticSteps,
                                        pipelinePublisher.runInfo.excludeSyntheticSteps)
                            });
                        }
                        // TODO pass the result to fireComplete
                        pipelinePublisher.modelAdapter.run().fireCompleted();
                        return;
                    }
                }
            }
            // force the result, just in case the publisher got gced.
            WorkflowRun run = Helper.getRun(execution.getOwner());
            Status.Result result = Helper.convertResult(run.getResult());
            PipelineRunInfo runInfo = new PipelineRunInfo(execution);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Forcing pipeline result: runId={0}, result={1}", new Object[]{
                    runInfo.id,
                    result
                });
            }
            BackendClient client = BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads);
            client.onEvent(new PipelineEvents.StageCompleted(runInfo.id, 0, result, run.getDuration()));
            client.onEvent(new PipelineEvents.PipelineCompleted(runInfo.id));
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
                if (decorator instanceof PipelinePublisher && ((PipelinePublisher) decorator).runInfo.isEnabled()) {
                    PipelinePublisher publisher = (PipelinePublisher) decorator;
                    ArchivedArtifactsStepsProvider stepsProvider = new ArchivedArtifactsStepsProvider(publisher, exec);
                    return new ArtifactsProcessor(run, publisher.client, publisher.runInfo.id, stepsProvider);
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
        public Pipeline.Steps getSteps() {
            for (FlowNode node : exec.getCurrentHeads()) {
                if (("archiveArtifacts").equals(node.getDisplayFunctionName())) {
                    Pipeline.Step step = pipelinePublisher.modelAdapter.step(node.getId());
                    if (step != null) {
                        return (Pipeline.Steps) step.parent();
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
        public boolean match(SuiteResult suite, Pipeline.Steps steps) {
            String nodeId = suite.getNodeId();
            if (nodeId != null) {
                Pipeline.Step step = modelAdapter.step(nodeId);
                if (step != null && step.parent().equals(steps)) {
                    return true;
                }
            }
            return false;
        }
    }
}
