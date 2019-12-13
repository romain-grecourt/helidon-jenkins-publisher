package io.helidon.build.publisher.model;

import io.helidon.build.publisher.model.events.PipelineEvents;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Status.State;

import com.fasterxml.jackson.databind.ObjectMapper;
import static io.helidon.build.publisher.model.PipelineRunTest.TIMESTAMP;
import io.helidon.build.publisher.model.Stage.StageType;
import io.helidon.build.publisher.model.events.ArtifactsInfoEvent;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.StageCompletedEvent;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCompletedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.StepOutputEvent;
import io.helidon.build.publisher.model.events.TestsInfoEvent;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test JSON with {@link PipelineEvents}.
 */
public class PipelineEventsTest {

    private static final String REPO_URL = "https://github.com/john_doe/repo.git";
    static final long TIMESTAMP = System.currentTimeMillis();

    @Test
    public void testJSON() throws IOException {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = new PipelineInfo("abcdefgh", "testJob", REPO_URL, "master", "123456789");
        events.add(new PipelineCreatedEvent(info, TIMESTAMP));
        events.add(new StageCreatedEvent(info.id, 1, 0, 0, "build", TIMESTAMP, StageType.STEPS));
        events.add(new StepCreatedEvent(info.id, 2, 1, 0, "sh", TIMESTAMP, "echo foo", false, true));
        events.add(new StepOutputEvent(info.id, 2));
        events.add(new StepCompletedEvent(info.id, 2, Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(info.id, 1, Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(info.id, 0, Result.SUCCESS, TIMESTAMP));
        events.add(new PipelineCompletedEvent(info.id, Result.SUCCESS, TIMESTAMP));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, new PipelineEvents(events));

        // pretty print
        Object json = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Object.class);
        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println(indented);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PipelineEvents fromJson = mapper.readValue(bais, PipelineEvents.class);
        assertThat(fromJson.events(), is(equalTo((events))));
    }

    @Test
    public void testApplyEvents() {
        PipelineCreatedEvent runCreated = new PipelineCreatedEvent(
                new PipelineInfo("abcdefgh", "testJob", REPO_URL, "master", "123456789"), TIMESTAMP);
        List<PipelineEvent> events = new LinkedList<>();
        events.add(new StageCreatedEvent(runCreated.info().id, 1, 0, 0, "build", TIMESTAMP, StageType.SEQUENCE));
        events.add(new StageCreatedEvent(runCreated.info().id, 2, 1, 0, null, TIMESTAMP, StageType.STEPS));
        events.add(new StepCreatedEvent(runCreated.info().id, 3, 2, 0, "sh", TIMESTAMP, "echo foo", false, true));
        events.add(new StepCompletedEvent(runCreated.info().id, 3, Result.SUCCESS, TIMESTAMP));
        events.add(new TestsInfoEvent(runCreated.info().id, 2, new TestsInfo(1, 1, 0, 0)));
        events.add(new ArtifactsInfoEvent(runCreated.info().id, 2, 1));
        events.add(new StageCompletedEvent(runCreated.info().id, 2, Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(runCreated.info().id, 1, Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(runCreated.info().id, 0, Result.SUCCESS, TIMESTAMP));
        events.add(new PipelineCompletedEvent(runCreated.info().id, Result.SUCCESS, TIMESTAMP));
        Pipeline pipeline  = new Pipeline(runCreated.info(), new Status(State.RUNNING), new Timings(runCreated.startTime()));
        new PipelineEventProcessor(pipeline).process(events);

        // pretty print
        System.out.println(pipeline.toPrettyString(true, true));

        assertThat(pipeline.children.size(), is(1));
        assertThat(pipeline.status.state, is(State.FINISHED));
        assertThat(pipeline.status.result, is(Result.SUCCESS));
        Stage stage = pipeline.children.get(0);
        assertThat(stage.id, is(1));
        assertThat(stage.name, is("build"));
        assertThat(stage.type, is(Stage.StageType.SEQUENCE));
        assertThat(stage.status.state, is(State.FINISHED));
        assertThat(stage.status.result, is(Result.SUCCESS));

        Sequence sequence = (Sequence)stage;
        assertThat(sequence.children.size(), is(1));
        assertThat(sequence.children.get(0).id, is(2));
        assertThat(sequence.children.get(0).type, is(Stage.StageType.STEPS));

        Steps steps = (Steps) sequence.children.get(0);

        assertThat(steps.artifacts, is(1));

        assertThat(steps.tests, is(notNullValue()));
        assertThat(steps.tests.total, is(1));
        assertThat(steps.tests.passed, is(1));
        assertThat(steps.tests.failed, is(0));
        assertThat(steps.tests.skipped, is(0));

        assertThat(steps.children.size(), is(1));
        Step step = steps.children.get(0);
        assertThat(step.id, is(3));
        assertThat(step.name, is("sh"));
        assertThat(step.status.state, is(State.FINISHED));
        assertThat(step.status.result, is(Result.SUCCESS));
    }
}
