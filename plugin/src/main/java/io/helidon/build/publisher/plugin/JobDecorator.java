package io.helidon.build.publisher.plugin;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.PipelineEvents;
import io.helidon.build.publisher.model.PipelineRun;
import io.helidon.build.publisher.model.Status;
import io.helidon.build.publisher.model.Timings;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Job decorator.
 */
public final class JobDecorator {

    private static final Logger LOGGER = Logger.getLogger(FlowDecorator.class.getName());
    private static final PipelineRunInfo EMPTY_PIPELINE_RUNINFO = new PipelineRunInfo();
    private static final JobDecorator EMPTY_DECORATOR = new JobDecorator(null);
    private static final Map<Run, WeakReference<JobDecorator>> DECORATORS = new WeakHashMap<>();

    private final PipelineRunInfo runInfo;
    private final BackendClient client;
    private final Status status;
    private final Timings timings;
    private PipelineRun pipelineRun;
    private Pipeline.Steps steps;
    private Pipeline.Step step;

    private JobDecorator(Run<?,?> run) {
        if (run == null || run instanceof WorkflowRun) {
            runInfo = EMPTY_PIPELINE_RUNINFO;
        } else {
            runInfo = new PipelineRunInfo(run);
        }
        if (runInfo.isEnabled()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline enabled, runInfo={0}", runInfo);
            }
            client = BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads);
            status = new StatusImpl(run);
            timings = new TimingsImpl(run);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline NOT enabled, runInfo={0}, isWorkflowRun={1}", new Object[]{
                    runInfo,
                    run instanceof WorkflowRun
                });
            }
            client = null;
            status = null;
            timings = null;
        }
    }

    /**
     * Start the job.
     */
    void start() {
        if (runInfo.isEnabled()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Creating pipeline, runId={0}", runInfo.id);
            }
            Pipeline pipeline = new Pipeline(status, timings);
            pipelineRun = new PipelineRun(runInfo.id, runInfo.jobName, runInfo.scmHead, runInfo.scmHash, pipeline);
            pipelineRun.fireEvent(client, PipelineEvents.NodeEventType.CREATED, runInfo.id);
            Pipeline.Sequence root = pipeline.stages();
            steps = new Pipeline.Steps(root, status, timings);
            root.addStage(steps);
            steps.fireEvent(client, PipelineEvents.NodeEventType.CREATED, runInfo.id);
            step = new Pipeline.Step(steps, "exec", "", false, true, status, timings);
            steps.addStep(step);
            step.fireEvent(client, PipelineEvents.NodeEventType.CREATED, runInfo.id);
        }
    }

    /**
     * Complete the job.
     */
    void complete() {
        if (runInfo.isEnabled()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Completing pipeline, runId={0}", runInfo.id);
            }
            step.fireEvent(client, PipelineEvents.NodeEventType.COMPLETED, runInfo.id);
            steps.fireEvent(client, PipelineEvents.NodeEventType.COMPLETED, runInfo.id);
            pipelineRun.fireEvent(client, PipelineEvents.NodeEventType.COMPLETED, runInfo.id);
        }
    }

    private static JobDecorator get(Run run) {
        if (run == null) {
            return EMPTY_DECORATOR;
        }
        synchronized (DECORATORS) {
            WeakReference<JobDecorator> decoratorRef = DECORATORS.get(run);
            if (decoratorRef != null && decoratorRef.get() != null) {
                return decoratorRef.get();
            }
        }
        JobDecorator decorator = new JobDecorator(run);
        synchronized (DECORATORS) {
            WeakReference<JobDecorator> decoratorRef = DECORATORS.get(run);
            if (decoratorRef != null && decoratorRef.get() != null) {
                return decoratorRef.get();
            }
            DECORATORS.put(run, new WeakReference<>(decorator));
            return decorator;
        }
    }

    @Extension
    public static final class RunListenerImpl extends RunListener<Run<?, ?>> {

        @Override
        public void onStarted(Run run, TaskListener listener) {
            get(run).start();
        }

        @Override
        public void onCompleted(Run run, @Nonnull TaskListener listener) {
            get(run).complete();
        }
    }

    @Extension
    public static final class ConsoleLogFilterImpl extends ConsoleLogFilter implements Serializable {

        private final JobDecorator decorator;

        public ConsoleLogFilterImpl() {
            super();
            decorator = EMPTY_DECORATOR;
        }

        public ConsoleLogFilterImpl(Run run) {
            decorator = get(run);
        }

        @Override
        public OutputStream decorateLogger(Run build, OutputStream outputStream) throws IOException, InterruptedException {
            if (decorator.runInfo.isEnabled()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Decorating output, runId={0}, step={1}", new Object[]{
                        decorator.runInfo.id,
                        decorator.step
                    });
                }
                return new PipelineOutputStream(outputStream, decorator.runInfo.id, decorator.step, decorator.client);
            }
            return outputStream;
        }
    }

    /**
     * {@link Timings} implementation that can get the end time from the run.
     */
    private static final class TimingsImpl extends Timings {

        private final Run run;

        /**
         * Create a new timing with start time derived from the given {@link FlowNode}.
         * @param source 
         */
        TimingsImpl(Run run) {
            super(run.getStartTimeInMillis());
            this.run = run;
        }

        @Override
        protected long computeEndTime() {
            if (!run.isBuilding()) {
                return run.getDuration();
            }
            return 0;
        }
    }

    /**
     * {@link Status} implementation that can compute the state and result from the run.
     */
    private static final class StatusImpl extends Status {

        private final Run run;

        StatusImpl(Run run) {
            super(stateOf(run));
            this.run = run;
        }

        @Override
        protected void refresh(Pipeline.Node node) {
            state = stateOf(run);
            result = Helper.convertResult(run.getResult());
        }

        private static State stateOf(Run run) {
            if (run.isBuilding()) {
                if (run.hasntStartedYet()) {
                    return State.QUEUED;
                }
                return State.RUNNING;
            }
            return State.FINISHED;
        }
    }
}