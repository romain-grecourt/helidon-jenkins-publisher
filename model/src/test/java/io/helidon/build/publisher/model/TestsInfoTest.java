package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test JSON with {@link TestsInfo}.
 */
public class TestsInfoTest {

    @Test
    public void testJson() throws IOException {
        TestsInfo testsInfo = new TestsInfo(6, 1, 2, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, testsInfo);

        // pretty print
        Object json = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Object.class);
        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println(indented);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TestsInfo fromJson = mapper.readValue(bais, TestsInfo.class);
        assertThat(fromJson, is(equalTo((testsInfo))));
    }
}
