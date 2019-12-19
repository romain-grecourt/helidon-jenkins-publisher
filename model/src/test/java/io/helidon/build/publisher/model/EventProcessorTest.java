package io.helidon.build.publisher.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Status.State;
import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
import io.helidon.build.publisher.model.events.PipelineErrorEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.StageCompletedEvent;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCompletedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.TestsInfoEvent;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

/**
 * Tests {@link EventProcessor}.
 */
public class EventProcessorTest {

    @Test
    public void testInfoAugmenter() {
        List<PipelineEvent> events = new LinkedList<>();
        events.add(new PipelineCreatedEvent(createInfo()));

        TestManager manager = new TestManager();
        TestAugmenter augmenter = new TestAugmenter(
                "https://github.com/john_doe/repo/commits/123456789",
                "https://github.com/john_doe/repo/tree/master",
                "john_doe",
                "https://github.com/john_doe");
        ArrayList<InfoAugmenter> augmenters = new ArrayList<>();
        augmenters.add(augmenter);
        new EventProcessor(manager, augmenters).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        assertThat(pipeline.info.commitUrl, is(augmenter.commitUrl));
        assertThat(pipeline.info.headRefUrl, is(augmenter.headRefUrl));
        assertThat(pipeline.info.user, is(augmenter.user));
        assertThat(pipeline.info.userUrl, is(augmenter.userUrl));
    }

    @Test
    public void testCompleted() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", now(), "echo foo"));
        events.add(new PipelineCompletedEvent(info.id, Result.SUCCESS, now()));

        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        pipeline.visit(new PipelineStatusVerifier(State.FINISHED, Result.SUCCESS));
    }

    @Test
    public void testUnknownFailure() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", now(), "echo foo"));
        events.add(new StepCompletedEvent(info.id, "3", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "2", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "1", Result.SUCCESS, now()));
        events.add(new PipelineCompletedEvent(info.id, Result.FAILURE, now()));

        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        assertThat(pipeline.status.state, is(State.FINISHED));
        assertThat(pipeline.status.result, is(Result.FAILURE));
        assertThat(pipeline.error, is(not(nullValue())));
        assertThat(pipeline.error, is(EventProcessor.UNKNOWN_ERROR));
    }

    @Test
    public void testknownFailure() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", now(), "echo foo"));
        events.add(new StepCompletedEvent(info.id, "3", Result.FAILURE, now()));
        events.add(new StageCompletedEvent(info.id, "2", Result.FAILURE, now()));
        events.add(new StageCompletedEvent(info.id, "1", Result.FAILURE, now()));
        events.add(new PipelineCompletedEvent(info.id, Result.FAILURE, now()));

        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        assertThat(pipeline.status.state, is(State.FINISHED));
        assertThat(pipeline.status.result, is(Result.FAILURE));
        assertThat(pipeline.error, is(nullValue()));
    }

    @Test
    public void testUnknownTestFailures() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", now(), "echo foo"));
        events.add(new StepCompletedEvent(info.id, "3", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "2", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "1", Result.SUCCESS, now()));
        events.add(new PipelineCompletedEvent(info.id, Result.UNSTABLE, now()));

        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        assertThat(pipeline.status.state, is(State.FINISHED));
        assertThat(pipeline.status.result, is(Result.UNSTABLE));
        assertThat(pipeline.error, is(not(nullValue())));
        assertThat(pipeline.error, is(EventProcessor.UNKNOWN_TEST_FAILURES));
    }

    @Test
    public void testKnownTestFailures() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", now(), "echo foo"));
        events.add(new StepCompletedEvent(info.id, "3", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "2", Result.SUCCESS, now()));
        events.add(new TestsInfoEvent(info.id, "2", new TestsInfo(2, 1, 1, 0)));
        events.add(new StageCompletedEvent(info.id, "1", Result.SUCCESS, now()));
        events.add(new PipelineCompletedEvent(info.id, Result.UNSTABLE, now()));

        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        assertThat(pipeline.status.state, is(State.FINISHED));
        assertThat(pipeline.status.result, is(Result.UNSTABLE));
        assertThat(pipeline.error, is(nullValue()));
    }

    @Test
    public void testPipelineErrorEvent() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", now(), "echo foo"));
        events.add(new PipelineErrorEvent(info.id, 100, "bad mistake"));

        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        pipeline.visit(new PipelineStatusVerifier(State.FINISHED, Result.ABORTED));
        assertThat(pipeline.error, is(not(nullValue())));
        assertThat(pipeline.error, is("bad mistake (100)"));
    }

    @Test
    public void testPipelineAbortedNoEmptyStages() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new PipelineCompletedEvent(info.id, Result.ABORTED, now()));

        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));
        assertThat(pipeline.error, is(nullValue()));
        assertThat(pipeline.children, is(empty()));
    }

    @Test
    public void testEvents() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = createInfo();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", now(), "echo foo"));
        events.add(new StepCompletedEvent(info.id, "3", Result.SUCCESS, now()));
        events.add(new TestsInfoEvent(info.id, "2", new TestsInfo(1, 1, 0, 0)));
        events.add(new ArtifactsInfoEvent(info.id, "2", 1));
        events.add(new StageCompletedEvent(info.id, "2", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "1", Result.SUCCESS, now()));
        events.add(new PipelineCompletedEvent(info.id, Result.SUCCESS, now()));
        TestManager manager = new TestManager();
        new EventProcessor(manager, null).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));

        // pretty print
        System.out.println(pipeline.toPrettyString(true, true));

        assertThat(pipeline.status.state, is(State.FINISHED));
        assertThat(pipeline.status.result, is(Result.SUCCESS));
        assertThat(pipeline.children.size(), is(1));
        assertThat(pipeline.children.get(0), is(instanceOf(Stages.class)));

        Stages buildStage = (Stages) pipeline.children.get(0);
        assertThat(buildStage.id, is("1"));
        assertThat(buildStage.name, is("build"));
        assertThat(buildStage.type(), is("SEQUENCE"));
        assertThat(buildStage.status.state, is(State.FINISHED));
        assertThat(buildStage.status.result, is(Result.SUCCESS));
        assertThat(buildStage.children.size(), is(1));
        assertThat(buildStage.children.get(0), is(instanceOf(Steps.class)));

        Steps buildSteps = (Steps) buildStage.children.get(0);
        assertThat(buildSteps.id, is("2"));
        assertThat(buildSteps.artifacts, is(1));
        assertThat(buildSteps.tests, is(notNullValue()));
        assertThat(buildSteps.tests.total, is(1));
        assertThat(buildSteps.tests.passed, is(1));
        assertThat(buildSteps.tests.failed, is(0));
        assertThat(buildSteps.tests.skipped, is(0));
        assertThat(buildSteps.children.size(), is(1));

        Step buildStep = buildSteps.children.get(0);
        assertThat(buildStep.id, is("3"));
        assertThat(buildStep.name, is("sh"));
        assertThat(buildStep.status.state, is(State.FINISHED));
        assertThat(buildStep.status.result, is(Result.SUCCESS));
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static PipelineInfo createInfo() {
        return PipelineInfo.builder()
                .id("abcdefgh")
                .title("testJob")
                .repositoryUrl("https://github.com/john_doe/repo.git")
                .headRef("master")
                .commit("123456789")
                .status(new Status(State.RUNNING))
                .timings(new Timings(now()))
                .build();
    }

    private static final class TestManager implements DescriptorManager {

        Pipeline pipeline;

        @Override
        public Pipeline loadPipeline(String id) {
            return null;
        }

        @Override
        public void savePipeline(Pipeline pipeline) {
            this.pipeline = pipeline;
        }
    }

    private static final class TestAugmenter implements InfoAugmenter {

        final String commitUrl;
        final String headRefUrl;
        final String user;
        final String userUrl;

        TestAugmenter(String commitUrl, String headRefUrl, String user, String userUrl) {
            this.commitUrl = commitUrl;
            this.headRefUrl = headRefUrl;
            this.user = user;
            this.userUrl = userUrl;
        }

        @Override
        public boolean process(PipelineInfo info) {
            info.commitUrl(commitUrl);
            info.headRefUrl(headRefUrl);
            info.user(user);
            info.userUrl(userUrl);
            return true;
        }
    }

    private static final class PipelineStatusVerifier implements PipelineVisitor {

        final State expectedState;
        final Result expectedResult;

        PipelineStatusVerifier(State expectedState, Result expectedResult) {
            this.expectedState = expectedState;
            this.expectedResult = expectedResult;
        }

        @Override
        public void visitStart(Pipeline pipeline) {
        }

        @Override
        public void visitStagesStart(Stages stages, int depth) {
        }

        @Override
        public void visitStepsStart(Steps steps, int depth) {
        }

        @Override
        public void visitStep(Step step, int depth) {
            assertThat(step.status.state, is(expectedState));
            assertThat(step.status.result, is(expectedResult));
        }

        @Override
        public void visitStepsEnd(Steps steps, int depth) {
            assertThat(steps.status.state, is(expectedState));
            assertThat(steps.status.result, is(expectedResult));
        }

        @Override
        public void visitStagesEnd(Stages stages, int depth) {
            assertThat(stages.status.state, is(expectedState));
            assertThat(stages.status.result, is(expectedResult));
        }

        @Override
        public void visitEnd(Pipeline pipeline) {
            assertThat(pipeline.status.state, is(expectedState));
            assertThat(pipeline.status.result, is(expectedResult));
        }
    }
}
