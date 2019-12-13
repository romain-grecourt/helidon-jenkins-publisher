package io.helidon.build.publisher.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.PipelineInfo;
import io.helidon.build.publisher.model.Status;
import io.helidon.build.publisher.model.Timings;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.junit.SuiteResult;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

/**
 * Job publisher.
 * Uses {@link RunListener} and {@link ConsoleLogFilter}.
 */
public final class JobPublisher {

    private static final Logger LOGGER = Logger.getLogger(PipelinePublisher.class.getName());
    private static final PipelineRunInfo EMPTY_PIPELINE_RUNINFO = new PipelineRunInfo();
    private static final JobPublisher EMPTY_PUBLISHER = new JobPublisher(null);
    private static final Map<Run, WeakReference<JobPublisher>> PUBLISHERS = new WeakHashMap<>();
    private static final ArtifactsProcessor.Factory ARTIFACTS_PROCESSOR_FACTORY = new ArtifactsProcessorFactory();
    private static final TestResultSuiteMatcher TEST_RESULT_SUITE_MATCHER = new TestResultSuiteMatcher();

    private final Run<?, ?> run;
    private final PipelineRunInfo runInfo;
    private final BackendClient client;
    private final Status status;
    private final Timings timings;
    private PipelineInfo pipelineRun;
    private Pipeline.Steps steps;
    private Pipeline.Step step;

    private JobPublisher(Run<?,?> run) {
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
            this.run = run;
            ArtifactsProcessor.register(ARTIFACTS_PROCESSOR_FACTORY);
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
            this.run = null;
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
            final Pipeline pipeline = new Pipeline(runInfo.id, status, timings);
            pipeline.addEventListener(client);
            pipeline.addEventListener(new TestResulProcessor(() -> pipelineRun, client, run, TEST_RESULT_SUITE_MATCHER));
            pipelineRun = new PipelineInfo(runInfo.id, runInfo.jobName, runInfo.repositoryUrl, runInfo.scmHead, runInfo.scmHash,
                    pipeline);
            pipelineRun.fireCreated();
            Pipeline.Sequence root = pipeline.stages();
            steps = new Pipeline.Steps(root, status, timings);
            root.addStage(steps);
            steps.fireCreated();
            step = new Pipeline.Step(steps, "exec", "", false, true, status, timings);
            steps.addStep(step);
            step.fireCreated();
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
            step.fireCompleted();
            steps.fireCompleted();
            pipelineRun.fireCompleted();
        }
    }

    private static JobPublisher get(Run run) {
        if (run == null) {
            return EMPTY_PUBLISHER;
        }
        synchronized (PUBLISHERS) {
            WeakReference<JobPublisher> ref = PUBLISHERS.get(run);
            if (ref != null && ref.get() != null) {
                return ref.get();
            }
        }
        JobPublisher jobPublisher = new JobPublisher(run);
        synchronized (PUBLISHERS) {
            WeakReference<JobPublisher> ref = PUBLISHERS.get(run);
            if (ref != null && ref.get() != null) {
                return ref.get();
            }
            PUBLISHERS.put(run, new WeakReference<>(jobPublisher));
            return jobPublisher;
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

        private final JobPublisher jobPublisher;

        public ConsoleLogFilterImpl() {
            super();
            jobPublisher = EMPTY_PUBLISHER;
        }

        public ConsoleLogFilterImpl(Run run) {
            jobPublisher = get(run);
        }

        @Override
        public OutputStream decorateLogger(Run build, OutputStream outputStream) throws IOException, InterruptedException {
            if (jobPublisher.runInfo.isEnabled()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Decorating output, runId={0}, step={1}", new Object[]{
                        jobPublisher.runInfo.id,
                        jobPublisher.step
                    });
                }
                return new PipelineOutputStream(outputStream, jobPublisher.runInfo.id, jobPublisher.step, jobPublisher.client);
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
        protected void refresh() {
            if (super.endTime == 0 && !run.isBuilding()) {
                 super.endTime = run.getDuration();
            }
        }
    }

    /**
     * {@link Status} implementation that can compute the state and result from the run.
     */
    private static final class StatusImpl extends Status {

        private final Run run;

        StatusImpl(Run run) {
            super(run.isBuilding() ? State.RUNNING : State.FINISHED);
            this.run = run;
        }

        @Override
        protected void refresh() {
            result = Helper.convertResult(run.getResult());
        }
    }

    /**
     * Artifact processor factory.
     */
    private static final class ArtifactsProcessorFactory implements ArtifactsProcessor.Factory {

        @Override
        public ArtifactsProcessor create(Run<?, ?> run) {
            WeakReference<JobPublisher> ref = PUBLISHERS.get(run);
            JobPublisher jobPublisher = ref != null ? ref.get() : null;
            if (jobPublisher != null && jobPublisher.runInfo.isEnabled()) {
                ArchivedArtifactsStepsProvider stepsProvider = new ArchivedArtifactsStepsProvider(jobPublisher);
                return new ArtifactsProcessor(run, jobPublisher.client, jobPublisher.runInfo.id, stepsProvider);
            }
            return null;
        }
    }

    /**
     * Archived artifacts steps provider.
     */
    private static final class ArchivedArtifactsStepsProvider implements ArtifactsProcessor.StepsProvider {

        private final JobPublisher jobPublisher;

        ArchivedArtifactsStepsProvider(JobPublisher publisher) {
            this.jobPublisher = Objects.requireNonNull(publisher, "publisher is null!");
        }

        @Override
        public Pipeline.Steps getSteps() {
            return jobPublisher.steps;
        }
    }

    /**
     * Test result suite matcher.
     */
    private static final class TestResultSuiteMatcher implements TestResulProcessor.SuiteResultMatcher {

        @Override
        public boolean match(SuiteResult suite, Pipeline.Steps steps) {
            return true;
        }
    }
}