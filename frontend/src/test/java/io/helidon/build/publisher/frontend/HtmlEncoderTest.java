package io.helidon.build.publisher.frontend;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.List;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Multi;

import org.junit.jupiter.api.Test;

import static io.helidon.build.publisher.frontend.HtmlLineEncoder.DIV_TEXT;
import static io.helidon.build.publisher.frontend.HtmlLineEncoder.SLASH_DIV_TEXT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link FileSegment} test.
 */
public final class HtmlEncoderTest {

    private static String bbtos(ByteBuffer byteBuffer) {
        byte[] buff = new byte[byteBuffer.remaining()];
        byteBuffer.get(buff);
        return new String(buff);
    }

    private static List<String> lines(Publisher<DataChunk> publisher) throws InterruptedException, ExecutionException {
        return Multi.from(publisher).map(DataChunk::data).map(HtmlEncoderTest::bbtos).collectList().get();
    }

    @Test
    public void testSimple() throws Exception {
        HtmlLineEncoder encoder = new HtmlLineEncoder(1L);
        new FileSegmentPublisher(FileSegmentTest.SEG1).subscribe(encoder);
        List<String> lines = lines(encoder);
        assertThat(lines, is(equalTo(Arrays.asList(
                DIV_TEXT, "line1", SLASH_DIV_TEXT,
                DIV_TEXT, "line2", SLASH_DIV_TEXT,
                DIV_TEXT, "line3", SLASH_DIV_TEXT,
                DIV_TEXT, "line4", SLASH_DIV_TEXT))));
    }
}
