package io.helidon.build.publisher.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.build.publisher.model.Pipeline.Parallel;
import io.helidon.build.publisher.model.Pipeline.Sequence;
import io.helidon.build.publisher.model.Pipeline.Step;
import io.helidon.build.publisher.model.Pipeline.Steps;
import io.helidon.build.publisher.model.Status.State;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test JSON with {@link PipelineRun}.
 */
public class PipelineRunTest {

    @Test
    public void testJSON() throws IOException {
        PipelineRun run = new PipelineRun("abcdefgh", "testJob", "master", "123456789", new Pipeline(new Status(State.RUNNING), new Timings(System.currentTimeMillis())));

        Steps steps = new Steps(run.pipeline.stages, new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        run.pipeline.stages.addStage(steps);
        steps.addStep(new Step(steps, "sh", "echo foo", false, true, new Status(State.RUNNING), new Timings(System.currentTimeMillis())));

        Parallel parallel = new Parallel(run.pipeline.stages, "tests", new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        run.pipeline.stages.addStage(parallel);

        Sequence test1 = new Sequence(parallel, "test1", new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        parallel.addStage(test1);
        Steps test1Steps = new Steps(test1, new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        test1.addStage(test1Steps);
        test1Steps.addStep(new Step(test1Steps, "sh", "echo test1a", false, true, new Status(State.RUNNING), new Timings(System.currentTimeMillis())));
        test1Steps.addStep(new Step(test1Steps, "sh", "echo test1b", false, true, new Status(State.RUNNING), new Timings(System.currentTimeMillis())));

        Sequence test2 = new Sequence(parallel, "test2", new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        parallel.addStage(test2);
        Steps test2Steps = new Steps(test2, new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        test2.addStage(test2Steps);
        test2Steps.addStep(new Step(test2Steps, "sh", "echo test2a", false, true, new Status(State.RUNNING), new Timings(System.currentTimeMillis())));
        test2Steps.addStep(new Step(test2Steps, "sh", "echo test2b", false, true, new Status(State.RUNNING), new Timings(System.currentTimeMillis())));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, run);

        // pretty print
        Object json = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Object.class);
        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println(indented);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PipelineRun fromJson = mapper.readValue(bais, PipelineRun.class);
        assertThat(fromJson, is(equalTo(run)));
    }
}
