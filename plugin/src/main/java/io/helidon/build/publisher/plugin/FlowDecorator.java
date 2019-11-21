package io.helidon.build.publisher.plugin;

import hudson.Extension;
import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.PipelineEvents;
import io.helidon.build.publisher.model.PipelineEvents.NodeEventType;
import io.helidon.build.publisher.model.Status;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * events flow related events.
 */
@SuppressRestrictedWarnings({TaskListenerDecorator.class})
final class FlowDecorator extends TaskListenerDecorator implements GraphListener.Synchronous {

    private static final Logger LOGGER = Logger.getLogger(FlowDecorator.class.getName());
    private static final EmptyDecorator EMPTY_DECORATOR = new EmptyDecorator();
    private static final Map<FlowExecution, WeakReference<TaskListenerDecorator>> DECORATORS = new WeakHashMap<>();

    private final PipelineEventsEmitter emitter;
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
            emitter = new PipelineEventsEmitter(signatures, runInfo);
            client = BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline NOT enabled, runInfo={0}", runInfo);
            }
            emitter = null;
            client = null;
        }
    }

    @Override
    public OutputStream decorate(OutputStream out) throws IOException, InterruptedException {
        if (runInfo.isEnabled()) {
            Pipeline.Step step = emitter.poll();
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
            emitter.offer(node, client);
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
                                dec.emitter.run().prettyPrint(dec.runInfo.excludeSyntheticSteps,
                                        dec.runInfo.excludeSyntheticSteps)
                            });
                        }
                        dec.emitter.run().fireEvent(dec.client, NodeEventType.COMPLETED, dec.runInfo.id);
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
            BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads)
                    .onEvent(new PipelineEvents.PipelineCompleted(runInfo.id, Status.State.FINISHED, result,
                            System.currentTimeMillis()));
        }

        @Override
        public void onResumed(FlowExecution execution) {
        }

        @Override
        public void onRunning(FlowExecution execution) {
        }
    }
}
