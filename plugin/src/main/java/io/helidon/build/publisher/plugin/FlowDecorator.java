package io.helidon.build.publisher.plugin;

import io.helidon.build.publisher.plugin.config.DelegateArtifactManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.PipelineEvents;
import io.helidon.build.publisher.model.Status;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;
import hudson.util.FileVisitor;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jenkins.model.ArtifactManager;
import jenkins.model.ArtifactManagerFactory;
import jenkins.model.StandardArtifactManager;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

/**
 * Implements both a {@link TaskListenerDecorator} and {@link GraphListener.Synchronous} to intercept all relevant
 * flow related events.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.class})
final class FlowDecorator extends TaskListenerDecorator implements GraphListener.Synchronous {

    private static final Logger LOGGER = Logger.getLogger(FlowDecorator.class.getName());
    private static final EmptyDecorator EMPTY_DECORATOR = new EmptyDecorator();
    private static final Map<FlowExecution, WeakReference<TaskListenerDecorator>> DECORATORS = new WeakHashMap<>();
    private static final InterceptingArtifactManagerFactory ARTIFACTS_INTERCEPTOR = new InterceptingArtifactManagerFactory();

    private final PipelineAdapter adapter;
    private final BackendClient client;
    private final PipelineRunInfo runInfo;

    FlowDecorator(FlowExecution execution) {
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
            adapter = new PipelineAdapter(signatures, runInfo, new EventListenerImpl(adapter, client, execution));
            DelegateArtifactManagerFactory.getInstance().register(ARTIFACTS_INTERCEPTOR);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline NOT enabled, runInfo={0}", runInfo);
            }
            adapter = null;
            client = null;
        }
    }

    @Override
    public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
        if (runInfo.isEnabled()) {
            Pipeline.Step step = adapter.poll();
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
            adapter.offer(node);
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
                return EMPTY_DECORATOR;
            }
            synchronized (DECORATORS) {
                WeakReference<TaskListenerDecorator> decoratorRef = DECORATORS.get(execution);
                if (decoratorRef != null && decoratorRef.get() != null) {
                    return decoratorRef.get();
                }
            }
            FlowDecorator decorator = new FlowDecorator(execution);
            synchronized (DECORATORS) {
                WeakReference<TaskListenerDecorator> decoratorRef = DECORATORS.get(execution);
                if (decoratorRef != null && decoratorRef.get() != null) {
                    return decoratorRef.get();
                }
                TaskListenerDecorator dec;
                if (decorator.runInfo.isEnabled()) {
                    dec = decorator;
                    execution.addListener(decorator);
                } else {
                    dec = EMPTY_DECORATOR;
                }
                DECORATORS.put(execution, new WeakReference<>(dec));
                return dec;
            }
        }
    }

    /**
     * No-op decorator.
     */
    @SuppressRestrictedWarnings(TaskListenerDecorator.class)
    private static final class EmptyDecorator extends TaskListenerDecorator {

        @Override
        public OutputStream decorate(OutputStream logger) throws IOException, InterruptedException {
            return logger;
        }
    }

    /**
     * {@link FlowExecutionListener} implementation used to clear the static cache of {@link TaskListenerDecorator}.
     */
    @Extension
    public static final class ExecutionListener extends FlowExecutionListener {

        @Override
        public void onCompleted(FlowExecution execution) {
            synchronized (DECORATORS) {
                WeakReference<TaskListenerDecorator> ref = DECORATORS.remove(execution);
                if (ref != null) {
                    TaskListenerDecorator decorator = ref.get();
                    if (decorator != null) {
                        FlowDecorator dec = (FlowDecorator) decorator;
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Pipeline completed, runId={0}, pipeline={1}", new Object[]{
                                dec.runInfo.id,
                                dec.adapter.run().prettyPrint(dec.runInfo.excludeSyntheticSteps,
                                        dec.runInfo.excludeSyntheticSteps)
                            });
                        }
                        dec.adapter.run().fireCompleted();
                        return;
                    }
                }
            }
            // force the result, just in case the decorator got gced.
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
     * {@link ArtifactManagerFactory} that produces {@link InterceptingArtifactManager}.
     */
    private static final class InterceptingArtifactManagerFactory extends ArtifactManagerFactory {

        @Override
        public ArtifactManager managerFor(Run<?, ?> run) {
            if (run instanceof WorkflowRun) {
                WeakReference<TaskListenerDecorator> ref = FlowDecorator.DECORATORS.get(((WorkflowRun) run).getExecution());
                TaskListenerDecorator decorator = ref != null ? ref.get() : null;
                if (decorator instanceof FlowDecorator && ((FlowDecorator) decorator).runInfo.isEnabled()) {
                    return new InterceptingArtifactManager(run, (FlowDecorator) decorator);
                }
            }
            return null;
        }
    }

    /**
     * Delegating artifact manager that submits events for the archived artifacts.
     */
    private static final class InterceptingArtifactManager extends StandardArtifactManager {

        private final transient FlowDecorator decorator;
        private final transient String runId;

        InterceptingArtifactManager(Run<?, ?> build, FlowDecorator decorator) {
            super(build);
            this.decorator = decorator;
            this.runId = decorator.runInfo.id;
        }

        @Override
        public void archive(FilePath workspace, Launcher launcher, BuildListener listener, final Map<String, String> artifacts)
                throws IOException, InterruptedException {

            Pipeline.Steps steps = getSteps();
            super.archive(workspace, launcher, listener, artifacts);
            if (steps != null) {
                final int stepsId = steps.id();
                final AtomicInteger artifactsCount = new AtomicInteger(0);
                new FilePath.ExplicitlySpecifiedDirScanner(artifacts).scan(getArtifactsDir(), new FileVisitor() {
                    @Override
                    public void visit(File file, String relativePath) throws IOException {
                        decorator.client.onEvent(new PipelineEvents.ArtifactData(runId, stepsId, file, relativePath));
                        artifactsCount.incrementAndGet();
                    }
                });
                int count = artifactsCount.get();
                if (count > 0) {
                    decorator.client.onEvent(new PipelineEvents.ArtifactsInfo(runId, stepsId, count));
                }
            }
        }

        private Pipeline.Steps getSteps() {
            FlowExecution exec = ((WorkflowRun) super.build).getExecution();
            if (exec != null) {
                for (FlowNode node : exec.getCurrentHeads()) {
                    if (("archiveArtifacts").equals(node.getDisplayFunctionName())) {
                        Pipeline.Step step = decorator.adapter.step(node.getId());
                        if (step != null) {
                            return (Pipeline.Steps) step.parent();
                        }
                    }
                }
            }
            return null;
        }

        @SuppressWarnings("deprecation")
        private File getArtifactsDir() {
            return build.getArtifactsDir();
        }
    }

    private static final class EventListenerImpl implements PipelineEvents.EventListener {

        private final PipelineAdapter adapter;
        private final BackendClient client;
        private final FlowExecution execution;
        private final Set<SuiteResult> processedSuites;
        private Pipeline pipeline;

        EventListenerImpl(PipelineAdapter adapter, BackendClient client, FlowExecution execution) {
            this.adapter = adapter;
            this.client = client;
            this.execution = execution;
            this.processedSuites = new ConcurrentHashMap<>().newKeySet();
        }

        @Override
        public void onEvent(PipelineEvents.Event event) {
            if (event instanceof PipelineEvents.StageCompleted) {
                int stageId = ((PipelineEvents.StageCompleted)event).id();
                if (pipeline == null) {
                    pipeline = adapter.run().pipeline();
                }
                if (pipeline != null) {
                    Pipeline.Node node = pipeline.node(stageId);
                    if (node instanceof Pipeline.Steps) {
                        processTestResults((Pipeline.Steps) node);
                    }
                }
            }
            // TODO support more than one listener on pipeline
            // and rename this class to something test related
            client.onEvent(event);
        }

        private void processTestResults(Pipeline.Steps steps) {
            TestResultAction tra = Helper.getRun(execution.getOwner()).getAction(TestResultAction.class);
            // TODO check if the tra hashCode differs in different invokations
            // otherwise cache it
            if (tra != null) {
                for(SuiteResult suite : tra.getResult().getSuites()) {
                    if (processedSuites.contains(suite)) {
                        continue;
                    }
                    String nodeId = suite.getNodeId();
                    if (nodeId != null) {
                        Pipeline.Step step = adapter.step(nodeId);
                        if (step != null && step.parent().equals(steps)) {
                            System.out.println("New suite to process !");
                            processedSuites.add(suite);
                        }
                    }
                }
            }
        }
    }
}
