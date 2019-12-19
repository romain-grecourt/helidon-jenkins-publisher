package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.helidon.build.publisher.model.Status.State;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test JSON with {@link Pipeline}.
 */
public class PipelineTest {

    @Test
    public void testJson() throws IOException {
        PipelineInfo info = PipelineInfo.builder()
                .id("abcdefgh")
                .title("testJob")
                .repositoryUrl("https://github.com/john_doe/repo.git")
                .headRef("master")
                .commit("123456789")
                .status(new Status(State.RUNNING))
                .timings(new Timings(now()))
                .build();
        Pipeline pipeline  = new Pipeline(info);

        Steps steps = new Steps(pipeline, new Status(State.RUNNING), new Timings(now()));
        pipeline.addStage(steps);
        steps.addStep(new Step(steps, "sh", "echo foo", false, true, new Status(State.RUNNING), new Timings(now())));

        Parallel parallel = new Parallel(pipeline, "tests", new Status(State.RUNNING), new Timings(now()));
        pipeline.addStage(parallel);

        Sequence test1 = new Sequence(parallel, "test1", new Status(State.RUNNING), new Timings(now()));
        parallel.addStage(test1);
        Steps test1Steps = new Steps(test1, new Status(State.RUNNING), new Timings(now()));
        test1.addStage(test1Steps);
        test1Steps.addStep(new Step(test1Steps, "sh", "echo test1a", false, true, new Status(State.RUNNING), new Timings(now())));
        test1Steps.addStep(new Step(test1Steps, "sh", "echo test1b", false, true, new Status(State.RUNNING), new Timings(now())));
        test1Steps.tests = new TestsInfo(1, 1, 0, 0);

        Sequence test2 = new Sequence(parallel, "test2", new Status(State.RUNNING), new Timings(now()));
        parallel.addStage(test2);
        Steps test2Steps = new Steps(test2, new Status(State.RUNNING), new Timings(now()));
        test2.addStage(test2Steps);
        test2Steps.addStep(new Step(test2Steps, "sh", "echo test2a", false, true, new Status(State.RUNNING), new Timings(now())));
        test2Steps.addStep(new Step(test2Steps, "sh", "echo test2b", false, true, new Status(State.RUNNING), new Timings(now())));
        test2Steps.tests = new TestsInfo(1, 0, 1, 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JacksonSupport.write(baos, pipeline);

        // pretty print
        System.out.println(new String(baos.toByteArray()));

        Pipeline fromJson = JacksonSupport.read(new ByteArrayInputStream(baos.toByteArray()), Pipeline.class);
        assertThat(fromJson, is(equalTo(pipeline)));
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
