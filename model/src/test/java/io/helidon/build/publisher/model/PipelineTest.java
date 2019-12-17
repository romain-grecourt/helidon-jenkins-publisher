package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.helidon.build.publisher.model.Status.State;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test JSON with {@link PipelineInfo}.
 */
public class PipelineTest {

    static final String REPO_URL = "https://github.com/john_doe/repo.git";
    static final long TIMESTAMP = System.currentTimeMillis();

    @Test
    public void testJson() throws IOException {
        String pipelineId = "abcdefgh";
        PipelineInfo info = new PipelineInfo(pipelineId, "testJob", REPO_URL, "master", "123456789", new Status(State.RUNNING),
                new Timings(TIMESTAMP));
        Pipeline pipeline  = new Pipeline(info);

        Steps steps = new Steps(pipeline, new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        pipeline.addStage(steps);
        steps.addStep(new Step(steps, "sh", "echo foo", false, true, new Status(State.RUNNING), new Timings(TIMESTAMP)));

        Parallel parallel = new Parallel(pipeline, "tests", new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        pipeline.addStage(parallel);

        Sequence test1 = new Sequence(parallel, "test1", new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        parallel.addStage(test1);
        Steps test1Steps = new Steps(test1, new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        test1.addStage(test1Steps);
        test1Steps.addStep(new Step(test1Steps, "sh", "echo test1a", false, true, new Status(State.RUNNING), new Timings(TIMESTAMP)));
        test1Steps.addStep(new Step(test1Steps, "sh", "echo test1b", false, true, new Status(State.RUNNING), new Timings(TIMESTAMP)));
        test1Steps.tests = new TestsInfo(1, 1, 0, 0);

        Sequence test2 = new Sequence(parallel, "test2", new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        parallel.addStage(test2);
        Steps test2Steps = new Steps(test2, new Status(State.RUNNING), new Timings(System.currentTimeMillis()));
        test2.addStage(test2Steps);
        test2Steps.addStep(new Step(test2Steps, "sh", "echo test2a", false, true, new Status(State.RUNNING), new Timings(TIMESTAMP)));
        test2Steps.addStep(new Step(test2Steps, "sh", "echo test2b", false, true, new Status(State.RUNNING), new Timings(TIMESTAMP)));
        test2Steps.tests = new TestsInfo(1, 0, 1, 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, pipeline);

        // pretty print
        Object json = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Object.class);
        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println(indented);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Pipeline fromJson = mapper.readValue(bais, Pipeline.class);
        assertThat(fromJson, is(equalTo(pipeline)));
    }

    //@Test
    public void testJsonFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        PipelineInfo fromJson = mapper.readValue(PipelineTest.class.getResourceAsStream("pipeline.json"), PipelineInfo.class);
    }
}
