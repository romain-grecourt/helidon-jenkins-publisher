package io.helidon.build.publisher.plugin;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.events.PipelineEvents;
import io.helidon.build.publisher.model.PipelineInfo;
import io.helidon.build.publisher.model.TestSuiteResult;
import io.helidon.build.publisher.model.TestsInfo;

import hudson.model.Actionable;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;

/**
 * Pipeline event listener that matches test results with steps.
 */
final class TestResulProcessor implements PipelineEvents.EventListener {

    private final Supplier<PipelineInfo> pipelineRunSupplier;
    private final BackendClient client;
    private final Actionable actionnable;
    private final Set<SuiteResult> processedSuites;
    private final SuiteResultMatcher matcher;
    private PipelineInfo pipelineRun;
    private Pipeline pipeline;
    private TestResultAction tra;

    TestResulProcessor(Supplier<PipelineInfo> pipelineSupplier, BackendClient client, Actionable actionnable,
            SuiteResultMatcher matcher) {

        this.pipelineRunSupplier = Objects.requireNonNull(pipelineSupplier, "pipelineSupplier is null");
        this.client =  Objects.requireNonNull(client, "client is null");
        this.actionnable = Objects.requireNonNull(actionnable, "actionnable is null");
        this.matcher = Objects.requireNonNull(matcher, "matcher is null");
        this.processedSuites = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void onEvent(PipelineEvents.Event event) {
        if (event instanceof PipelineEvents.StageCompleted) {
            int stageId = ((PipelineEvents.StageCompleted) event).id();
            if (pipelineRun == null) {
                pipelineRun = pipelineRunSupplier.get();
                if (pipelineRun != null) {
                    pipeline = pipelineRun.pipeline();
                }
            }
            if (pipeline != null) {
                String runId = pipelineRun.id();
                Pipeline.Node node = pipeline.node(stageId);
                if (node instanceof Pipeline.Steps) {
                    Pipeline.Steps steps = (Pipeline.Steps) node;
                    if (tra == null) {
                        tra = actionnable.getAction(TestResultAction.class);
                    }
                    if (tra != null) {
                        int totalPassed = 0;
                        int totalFailed = 0;
                        int totalSkipped = 0;
                        for (SuiteResult suite : tra.getResult().getSuites()) {
                            if (processedSuites.contains(suite)) {
                                continue;
                            }
                            if (matcher.match(suite, steps)) {
                                int passed = 0;
                                int failed = 0;
                                int skipped = 0;
                                LinkedList<TestSuiteResult.TestResult> tests = new LinkedList<>();
                                for (CaseResult caseResult : suite.getCases()) {
                                    TestSuiteResult.TestStatus status;
                                    if (caseResult.isPassed()) {
                                        status = TestSuiteResult.TestStatus.PASSED;
                                        passed++;
                                    } else if (caseResult.isSkipped()) {
                                        status = TestSuiteResult.TestStatus.SKIPPED;
                                        skipped++;
                                    } else {
                                        status = TestSuiteResult.TestStatus.FAILED;
                                        failed++;
                                    }
                                    String output = caseResult.getErrorStackTrace();
                                    if (output == null) {
                                        output = caseResult.getErrorDetails();
                                    }
                                    tests.add(new TestSuiteResult.TestResult(caseResult.getName(), status, output));
                                }
                                int total = passed + failed + skipped;
                                if (total > 0) {
                                    TestSuiteResult testSuiteResult =
                                            new TestSuiteResult(suite.getName(), total, passed, failed, skipped, tests);
                                    client.onEvent(new PipelineEvents.TestSuite(runId, stageId, testSuiteResult));
                                    totalPassed += passed;
                                    totalFailed += failed;
                                    totalSkipped += skipped;
                                }
                                processedSuites.add(suite);
                            }
                        }
                        int total = totalPassed + totalFailed + totalSkipped;
                        if (total > 0) {
                            TestsInfo testsInfo = new TestsInfo(total, totalPassed, totalFailed, totalSkipped);
                            client.onEvent(new PipelineEvents.Tests(runId, stageId, testsInfo));
                        }
                    }
                }
            }
        }
    }

    /**
     * {@link SuiteResult} matcher.
     */
    interface SuiteResultMatcher {

        /**
         * Match a suite with steps.
         * @param suite the suite to match
         * @param steps the current steps to associate with the suite
         * @return {@code true} if the suite matches the steps, {@code false} otherwise
         */
        boolean match(SuiteResult suite, Pipeline.Steps steps);
    }
}
