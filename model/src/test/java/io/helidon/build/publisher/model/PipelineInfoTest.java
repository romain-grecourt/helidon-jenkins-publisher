package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test JSON with {@link PipelineInfo}.
 */
public class PipelineInfoTest {

    @Test
    public void testJson() throws IOException {
        PipelineInfo info = PipelineInfo.builder()
                .id("abcdefgh")
                .title("testJob")
                .repositoryUrl("https://github.com/john_doe/repo.git")
                .headRef("master")
                .commit("123456789")
                .status(new Status(Status.State.RUNNING))
                .timings(new Timings(System.currentTimeMillis()))
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JacksonSupport.write(baos, info);

        // pretty print
        System.out.println(new String(baos.toByteArray()));

        PipelineInfo fromJson = JacksonSupport.read(new ByteArrayInputStream(baos.toByteArray()), PipelineInfo.class);
        assertThat(fromJson, is(equalTo(info)));
    }
}
