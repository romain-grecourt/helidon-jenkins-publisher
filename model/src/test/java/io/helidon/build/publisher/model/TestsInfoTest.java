package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
        JacksonSupport.write(baos, testsInfo);

        // pretty print
        System.out.println(new String(baos.toByteArray()));

        TestsInfo fromJson = JacksonSupport.read(new ByteArrayInputStream(baos.toByteArray()), TestsInfo.class);
        assertThat(fromJson, is(equalTo((testsInfo))));
    }
}
