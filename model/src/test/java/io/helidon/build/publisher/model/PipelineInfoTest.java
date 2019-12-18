package io.helidon.build.publisher.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.jupiter.api.Test;

/**
 * Test JSON with {@link PipelineInfo}.
 */
public class PipelineInfoTest {

    static final String REPO_URL = "https://github.com/john_doe/repo.git";
    static final long TIMESTAMP = System.currentTimeMillis();

    @Test
    public void testJson() throws IOException {
        PipelineInfo info = PipelineInfo.builder()
                .id("abcdefgh")
                .title("testJob")
                .repositoryUrl(REPO_URL)
                .headRef("master")
                .commit("123456789")
                .status(new Status(Status.State.RUNNING))
                .timings(new Timings(TIMESTAMP))
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, info);

        // pretty print
        Object json = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Object.class);
        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println(indented);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PipelineInfo fromJson = mapper.readValue(bais, PipelineInfo.class);
        assertThat(fromJson, is(equalTo(info)));
    }
}
