package io.helidon.build.publisher.frontend;

import io.helidon.build.publisher.reactive.DataChunkLimiter;
import io.helidon.build.publisher.reactive.Multi;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.RetrySchema;
import io.helidon.media.common.ReadableByteChannelPublisher;
import java.io.IOException;

/**
 * {@link FileSegment} publisher.
 */
final class FileSegmentPublisher implements Publisher<DataChunk> {

    private static final RetrySchema RETRY_SCHEMA = RetrySchema.linear(0, 10, 250);
    private final Publisher<DataChunk> delegate;

    FileSegmentPublisher(FileSegment segment) throws IOException {
        segment.raf.seek(segment.begin);
        delegate = Multi.from(new ReadableByteChannelPublisher(segment.raf.getChannel(), RETRY_SCHEMA))
                .limit(new DataChunkLimiter(segment.end - segment.begin));
    }

    @Override
    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        delegate.subscribe(subscriber);
    }
}
