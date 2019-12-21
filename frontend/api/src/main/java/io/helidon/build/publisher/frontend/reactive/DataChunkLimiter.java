package io.helidon.build.publisher.frontend.reactive;

import io.helidon.common.http.DataChunk;
import java.nio.ByteBuffer;

/**
 * Limiter implementation for {@link DataChunk}.
 */
public final class DataChunkLimiter implements Limiter<DataChunk> {

    private final long limit;
    private long current;

    /**
     * Create a new limiter instance.
     * @param limit byte limit
     */
    public DataChunkLimiter(long limit) {
        this.limit = limit;
    }

    @Override
    public void limit(DataChunk item, Action<DataChunk> action) {
        ByteBuffer data = item.data();
        int len = data.remaining();
        if (current < limit) {
            long curLimit = limit - current;
            if (curLimit < len) {
                data.limit((int) curLimit);
                current = limit;
                action.complete(DataChunk.create(/* flush */ true, data, /* readonly */ true));
            } else {
                current += len;
                action.publish(item);
            }
        }
    }
}
