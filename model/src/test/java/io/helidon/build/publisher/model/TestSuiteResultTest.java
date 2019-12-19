package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import io.helidon.build.publisher.model.TestSuiteResult.TestResult;

import org.junit.jupiter.api.Test;

import static io.helidon.build.publisher.model.TestSuiteResult.TestStatus.FAILED;
import static io.helidon.build.publisher.model.TestSuiteResult.TestStatus.PASSED;
import static io.helidon.build.publisher.model.TestSuiteResult.TestStatus.SKIPPED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test JSON with {@link TestSuiteResult}.
 */
public class TestSuiteResultTest {

    @Test
    public void testJson() throws IOException {
        LinkedList<TestResult> tests = new LinkedList<>();
        tests.add(new TestSuiteResult.TestResult("test1", PASSED, null));
        tests.add(new TestSuiteResult.TestResult("test2", FAILED, null));
        tests.add(new TestSuiteResult.TestResult("test3", SKIPPED, null));
        TestSuiteResult testResults = new TestSuiteResult("suite", 3, 1, 1, 1, tests);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JacksonSupport.write(baos, testResults);

        // pretty print
        System.out.println(new String(baos.toByteArray()));

        TestSuiteResult fromJson = JacksonSupport.read(new ByteArrayInputStream(baos.toByteArray()), TestSuiteResult.class);
        assertThat(fromJson, is(equalTo((testResults))));
    }
}
