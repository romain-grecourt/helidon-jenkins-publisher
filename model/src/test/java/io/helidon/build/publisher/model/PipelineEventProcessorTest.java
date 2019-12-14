package io.helidon.build.publisher.model;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.StageCompletedEvent;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCompletedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.TestsInfoEvent;

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
        String pid = "abcdefgh";
        events.add(new PipelineCreatedEvent(new PipelineInfo("abcdefgh", "testJob", REPO_URL, "master", "123456789"), TIMESTAMP));
        events.add(new StageCreatedEvent(pid, "1", "0", 0, "build", TIMESTAMP, Stage.StageType.SEQUENCE));
        events.add(new StageCreatedEvent(pid, "2", "1", 0, null, TIMESTAMP, Stage.StageType.STEPS));
        events.add(new StepCreatedEvent(pid, "3", "2", 0, "sh", TIMESTAMP, "echo foo"));
        events.add(new StepCompletedEvent(pid, "3", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new TestsInfoEvent(pid, "2", new TestsInfo(1, 1, 0, 0)));
        events.add(new ArtifactsInfoEvent(pid, "2", 1));
        events.add(new StageCompletedEvent(pid, "2", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(pid, "1", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(pid, "0", Status.Result.SUCCESS, TIMESTAMP));
        events.add(new PipelineCompletedEvent(pid, Status.Result.SUCCESS, TIMESTAMP));
        AtomicReference<Pipeline> pipelineRef = new AtomicReference<>();
        new PipelineEventProcessor(new PipelineDescriptorManager(){
            @Override
            public Pipeline load(String id) {
                return null;
            }

            @Override
            public void save(Pipeline pipeline) {
                pipelineRef.set(pipeline);
            }
        }).process(events);

        Pipeline pipeline = pipelineRef.get();
        assertThat(pipeline, is(not(nullValue())));

        // pretty print
        System.out.println(pipeline.toPrettyString(true, true));

        assertThat(pipeline.children.size(), is(1));
        assertThat(pipeline.status.state, is(Status.State.FINISHED));
        assertThat(pipeline.status.result, is(Status.Result.SUCCESS));
        Stage stage = pipeline.children.get(0);
        assertThat(stage.id, is("1"));
        assertThat(stage.name, is("build"));
        assertThat(stage.type(), is(Stage.StageType.SEQUENCE));
        assertThat(stage.status.state, is(Status.State.FINISHED));
        assertThat(stage.status.result, is(Status.Result.SUCCESS));

        Sequence sequence = (Sequence)stage;
        assertThat(sequence.children.size(), is(1));
        assertThat(sequence.children.get(0).id, is("2"));
        assertThat(sequence.children.get(0).type(), is(Stage.StageType.STEPS));

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
}
