package io.helidon.build.publisher.model;

import java.util.LinkedList;
import java.util.List;

import io.helidon.build.publisher.model.Status.State;
import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.StageCompletedEvent;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCompletedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.TestsInfoEvent;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link PipelineEventProcessor}.
 */
public class PipelineEventProcessorTest {

    private static final String REPO_URL = "https://github.com/john_doe/repo.git";
    private static final long TIMESTAMP = System.currentTimeMillis();

    @Test
    public void testSimpleEvents() {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = PipelineInfo.builder()
                .id("abcdefgh")
                .title("testJob")
                .repositoryUrl(REPO_URL)
                .headRef("master")
                .commit("123456789")
                .status(new Status(State.RUNNING))
                .timings(new Timings(TIMESTAMP))
                .build();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", TIMESTAMP, "SEQUENCE"));
        events.add(new StageCreatedEvent(info.id, "2", "1", 0, null, TIMESTAMP, "STEPS"));
        events.add(new StepCreatedEvent(info.id, "3", "2", 0, "sh", TIMESTAMP, "echo foo"));
        events.add(new StepCompletedEvent(info.id, "3", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new TestsInfoEvent(info.id, "2", new TestsInfo(1, 1, 0, 0)));
        events.add(new ArtifactsInfoEvent(info.id, "2", 1));
        events.add(new StageCompletedEvent(info.id, "2", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(info.id, "1", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(info.id, "0", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new PipelineCompletedEvent(info.id, Status.Result.SUCCESS, TIMESTAMP));
        TestManager manager = new TestManager();
        TestAugmenter augmenter = new TestAugmenter(
                "https://github.com/john_doe/repo/commits/123456789",
                "https://github.com/john_doe/repo/tree/master",
                "john_doe",
                "https://github.com/john_doe");
        ArrayList<PipelineInfoAugmenter> augmenters = new ArrayList<>();
        augmenters.add(augmenter);
        new PipelineEventProcessor(manager, augmenters).process(events);

        Pipeline pipeline = manager.pipeline;
        assertThat(pipeline, is(not(nullValue())));

        assertThat(pipeline.info.commitUrl, is(augmenter.commitUrl));
        assertThat(pipeline.info.headRefUrl, is(augmenter.headRefUrl));
        assertThat(pipeline.info.user, is(augmenter.user));
        assertThat(pipeline.info.userUrl, is(augmenter.userUrl));

        // pretty print
        System.out.println(pipeline.toPrettyString(true, true));

        assertThat(pipeline.children.size(), is(1));
        assertThat(pipeline.status.state, is(Status.State.FINISHED));
        assertThat(pipeline.status.result, is(Status.Result.SUCCESS));
        Stage stage = pipeline.children.get(0);
        assertThat(stage.id, is("1"));
        assertThat(stage.name, is("build"));
        assertThat(stage.type(), is("SEQUENCE"));
        assertThat(stage.status.state, is(Status.State.FINISHED));
        assertThat(stage.status.result, is(Status.Result.SUCCESS));

        Sequence sequence = (Sequence)stage;
        assertThat(sequence.children.size(), is(1));
        assertThat(sequence.children.get(0).id, is("2"));
        assertThat(sequence.children.get(0).type(), is("STEPS"));

        Steps steps = (Steps) sequence.children.get(0);

        assertThat(steps.artifacts, is(1));

        assertThat(steps.tests, is(notNullValue()));
        assertThat(steps.tests.total, is(1));
        assertThat(steps.tests.passed, is(1));
        assertThat(steps.tests.failed, is(0));
        assertThat(steps.tests.skipped, is(0));

        assertThat(steps.children.size(), is(1));
        Step step = steps.children.get(0);
        assertThat(step.id, is("3"));
        assertThat(step.name, is("sh"));
        assertThat(step.status.state, is(Status.State.FINISHED));
        assertThat(step.status.result, is(Status.Result.SUCCESS));
    }

    private static final class TestManager implements PipelineDescriptorManager {

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

    private static final class TestAugmenter implements PipelineInfoAugmenter {

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
}
