package io.helidon.build.publisher.plugin;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.helidon.build.publisher.model.Node;
import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.Steps;
import io.helidon.build.publisher.model.TestSuiteResult;
import io.helidon.build.publisher.model.TestsInfo;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineEventListener;
import io.helidon.build.publisher.model.events.StageCompletedEvent;
import io.helidon.build.publisher.model.events.TestSuiteResultEvent;
import io.helidon.build.publisher.model.events.TestsInfoEvent;

import hudson.model.Actionable;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;

/**
 * Pipeline event listener that matches test results with steps.
 */
final class TestResulProcessor implements PipelineEventListener {

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
        boolean match(SuiteResult suite, Steps steps);
    }

    private final BackendClient client;
    private final Actionable actionnable;
    private final Set<SuiteResult> processedSuites;
    private final SuiteResultMatcher matcher;
    private final Pipeline pipeline;
    private TestResultAction tra;

    TestResulProcessor(Pipeline pipeline, BackendClient client, Actionable actionnable, SuiteResultMatcher matcher) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline is null");
        this.client =  Objects.requireNonNull(client, "client is null");
        this.actionnable = Objects.requireNonNull(actionnable, "actionnable is null");
        this.matcher = Objects.requireNonNull(matcher, "matcher is null");
        this.processedSuites = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void onEvent(PipelineEvent event) {
        if (event instanceof StageCompletedEvent) {
            String stageId = ((StageCompletedEvent) event).id();
            Node node = pipeline.node(stageId);
            if (node instanceof Steps) {
                Steps steps = (Steps) node;
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
                            processedSuites.add(suite);
                            TestSuiteResult testSuiteResult = processSuite(suite);
                            if (testSuiteResult != null) {
                                client.onEvent(new TestSuiteResultEvent(pipeline.pipelineId(), steps.id(), testSuiteResult));
                                totalPassed += testSuiteResult.passed();
                                totalFailed += testSuiteResult.failed();
                                totalSkipped += testSuiteResult.skipped();
                            }
                        }
                    }
                    int total = totalPassed + totalFailed + totalSkipped;
                    if (total > 0) {
                        TestsInfo testsInfo = new TestsInfo(total, totalPassed, totalFailed, totalSkipped);
                        client.onEvent(new TestsInfoEvent(pipeline.pipelineId(), stageId, testsInfo));
                    }
                }
            }
        }
    }

    private TestSuiteResult processSuite(SuiteResult suite) {
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
            return new TestSuiteResult(suite.getName(), total, passed, failed, skipped, tests);
        }
        return null;
    }
}
