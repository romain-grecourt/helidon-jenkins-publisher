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
import io.helidon.build.publisher.model.Step;
import io.helidon.build.publisher.model.Steps;

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
    private static final Map<Run, WeakReference<JobPublisher>> PUBLISHERS = new WeakHashMap<>();
    private static final ArtifactsProcessor.Factory ARTIFACTS_PROCESSOR_FACTORY = new ArtifactsProcessorFactory();
    private static final TestResultSuiteMatcher TEST_RESULT_SUITE_MATCHER = new TestResultSuiteMatcher();
    private static final JobPublisher EMPTY_PUBLISHER = new JobPublisher(null);

    private final boolean enabled;
    private final BackendClient client;
    private final Pipeline pipeline;
    private final String pipelineId;
    private final Steps steps;
    private final Step step;

    private JobPublisher(Run<?,?> run) {
        PipelineRunInfo runInfo;
        if (run == null || run instanceof WorkflowRun) {
            runInfo = null;
        } else {
            runInfo = new PipelineRunInfo(run);
        }
        if (runInfo != null && runInfo.id != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Pipeline enabled, runInfo={0}", runInfo);
            }
            enabled = true;
            pipelineId = runInfo.id;
            client = BackendClient.getOrCreate(runInfo.publisherServerUrl, runInfo.publisherClientThreads);
            GlobalStatus status = new GlobalStatus(run);
            GlobalTimings timings = new GlobalTimings(run);
            pipeline = new Pipeline(runInfo.toPipelineInfo(status, timings));
            pipeline.addEventListener(client);
            pipeline.addEventListener(new TestResulProcessor(pipeline, client, run, TEST_RESULT_SUITE_MATCHER));
            steps = new Steps(pipeline, status, timings);
            step = new Step(steps, "exec", "", false, true, status, timings);
            steps.addStep(step);
            pipeline.addStage(steps);
            ArtifactsProcessor.register(ARTIFACTS_PROCESSOR_FACTORY);
        } else {
            if (LOGGER.isLoggable(Level.FINE) && runInfo != null) {
                LOGGER.log(Level.FINE, "Pipeline NOT enabled, run={0}", run);
            }
            enabled = false;
            pipelineId = null;
            client = null;
            pipeline = null;
            steps = null;
            step = null;
        }
    }

    /**
     * Start the job.
     */
    void start() {
        if (enabled) {
            pipeline.fireCreated();
            steps.fireCreated();
            step.fireCreated();
        }
    }

    /**
     * Complete the job.
     */
    void complete() {
        if (enabled) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Completing pipeline, pipelineId={0}", pipelineId);
            }
            step.fireCompleted();
            steps.fireCompleted();
            pipeline.fireCompleted();
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
     * @return JobPublisher or {@code null} if not found
     */
    static JobPublisher get(Run run) {
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

        private final transient JobPublisher jobPublisher;
        private static final long serialVersionUID = 1L;

        public ConsoleLogFilterImpl() {
            super();
            jobPublisher = EMPTY_PUBLISHER;
        }

        public ConsoleLogFilterImpl(Run run) {
            jobPublisher = get(run);
        }

        @Override
        public OutputStream decorateLogger(Run build, OutputStream outputStream) throws IOException, InterruptedException {
            if (jobPublisher != null && jobPublisher.enabled) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Decorating output, pipelineId={0}, step={1}", new Object[]{
                        jobPublisher.pipelineId,
                        jobPublisher.step
                    });
                }
                return new PipelineOutputStream(outputStream, jobPublisher.pipelineId, jobPublisher.step, jobPublisher.client);
            }
            return outputStream;
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
            if (jobPublisher != null && jobPublisher.enabled) {
                ArchivedArtifactsStepsProvider stepsProvider = new ArchivedArtifactsStepsProvider(jobPublisher);
                return new ArtifactsProcessor(run, jobPublisher.client, jobPublisher.pipelineId, stepsProvider);
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
        public Steps getSteps() {
            return jobPublisher.steps;
        }
    }

    /**
     * Test result suite matcher.
     */
    private static final class TestResultSuiteMatcher implements TestResulProcessor.SuiteResultMatcher {

        @Override
        public boolean match(SuiteResult suite, Steps steps) {
            return true;
        }
    }
}