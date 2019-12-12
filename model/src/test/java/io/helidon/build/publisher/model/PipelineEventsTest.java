package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.publisher.model.Pipeline.Sequence;
import io.helidon.build.publisher.model.Pipeline.Stage;
import io.helidon.build.publisher.model.Pipeline.Stage.StageType;
import io.helidon.build.publisher.model.Pipeline.Step;
import io.helidon.build.publisher.model.Pipeline.Steps;
import io.helidon.build.publisher.model.PipelineEvents.Output;
import io.helidon.build.publisher.model.PipelineEvents.PipelineCompleted;
import io.helidon.build.publisher.model.PipelineEvents.PipelineCreated;
import io.helidon.build.publisher.model.PipelineEvents.StageCompleted;
import io.helidon.build.publisher.model.PipelineEvents.StageCreated;
import io.helidon.build.publisher.model.PipelineEvents.StepCompleted;
import io.helidon.build.publisher.model.PipelineEvents.StepCreated;
import io.helidon.build.publisher.model.PipelineEvents.Tests;
import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Status.State;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    public void testJSON() throws IOException {
        List<PipelineEvents.Event> events = new LinkedList<>();
        String runId = "abcdefgh";
        events.add(new PipelineCreated(runId, "testJob", REPO_URL, "master", "123456789", System.currentTimeMillis()));
        events.add(new StageCreated(runId, 1, 0, 0, "build", System.currentTimeMillis(), StageType.STEPS));
        events.add(new StepCreated(runId, 2, 1, 0, "sh", System.currentTimeMillis(), "echo foo", false, true));
        events.add(new Output(runId, 2));
        events.add(new StepCompleted(runId, 2, Result.SUCCESS, System.currentTimeMillis()));
        events.add(new StageCompleted(runId, 1, Result.SUCCESS, System.currentTimeMillis()));
        events.add(new StageCompleted(runId, 0, Result.SUCCESS, System.currentTimeMillis()));
        events.add(new PipelineCompleted(runId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, new PipelineEvents(events));

        // pretty print
        Object json = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Object.class);
        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println(indented);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PipelineEvents fromJson = mapper.readValue(bais, PipelineEvents.class);
        assertThat(fromJson.events, is(equalTo((events))));
    }

    @Test
    public void testApplyEvents() {
        String runId = "abcdefgh";
        PipelineCreated runCreated = new PipelineCreated(runId, "testJob", REPO_URL, "master", "123456789", System.currentTimeMillis());
        PipelineRun run = new PipelineRun(runCreated);
        List<PipelineEvents.Event> events = new LinkedList<>();
        events.add(new StageCreated(runId, 1, 0, 0, "build", System.currentTimeMillis(), StageType.SEQUENCE));
        events.add(new StageCreated(runId, 2, 1, 0, null, System.currentTimeMillis(), StageType.STEPS));
        events.add(new StepCreated(runId, 3, 2, 0, "sh", System.currentTimeMillis(), "echo foo", false, true));
        events.add(new StepCompleted(runId, 3, Result.SUCCESS, System.currentTimeMillis()));
        events.add(new Tests(runId, 2, new TestsInfo(1, 1, 0, 0)));
        events.add(new PipelineEvents.ArtifactsInfo(runId, 2, 1));
        events.add(new StageCompleted(runId, 2, Result.SUCCESS, System.currentTimeMillis()));
        events.add(new StageCompleted(runId, 1, Result.SUCCESS, System.currentTimeMillis()));
        events.add(new StageCompleted(runId, 0, Result.SUCCESS, System.currentTimeMillis()));
        events.add(new PipelineCompleted(runId));
        run.pipeline.applyEvents(events);

        // pretty print
        System.out.println(run.pipeline.prettyPrint("", true, true));

        assertThat(run.pipeline.sequence.stages.size(), is(1));
        assertThat(run.pipeline.sequence.status.state, is(State.FINISHED));
        assertThat(run.pipeline.sequence.status.result, is(Result.SUCCESS));
        Stage stage = run.pipeline.sequence.stages.get(0);
        assertThat(stage.id, is(1));
        assertThat(stage.name, is("build"));
        assertThat(stage.type, is(Stage.StageType.SEQUENCE));
        assertThat(stage.status.state, is(State.FINISHED));
        assertThat(stage.status.result, is(Result.SUCCESS));

        Sequence sequence = (Sequence)stage;
        assertThat(sequence.stages.size(), is(1));
        assertThat(sequence.stages.get(0).id, is(2));
        assertThat(sequence.stages.get(0).type, is(Stage.StageType.STEPS));

        Steps steps = (Steps) sequence.stages.get(0);

        assertThat(steps.artifacts, is(1));

        assertThat(steps.tests, is(notNullValue()));
        assertThat(steps.tests.total, is(1));
        assertThat(steps.tests.passed, is(1));
        assertThat(steps.tests.failed, is(0));
        assertThat(steps.tests.skipped, is(0));

        assertThat(steps.steps.size(), is(1));
        Step step = steps.steps.get(0);
        assertThat(step.id, is(3));
        assertThat(step.name, is("sh"));
        assertThat(step.status.state, is(State.FINISHED));
        assertThat(step.status.result, is(Result.SUCCESS));
    }
}
