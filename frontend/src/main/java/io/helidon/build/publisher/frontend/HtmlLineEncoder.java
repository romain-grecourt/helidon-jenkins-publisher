package io.helidon.build.publisher.frontend;

import io.helidon.build.publisher.reactive.BaseProcessor;
import io.helidon.build.publisher.reactive.VirtualBuffer;
import io.helidon.build.publisher.reactive.VirtualChunk;
import io.helidon.common.http.DataChunk;
import java.nio.ByteBuffer;

/**
 * HTML reactive line "encoder".
 */
final class HtmlLineEncoder extends BaseProcessor<DataChunk, DataChunk> {

    private static final DataChunk DIV = DataChunk.create("<div class=\"line\">".getBytes());
    private static final DataChunk SLASH_DIV = DataChunk.create("</div>".getBytes());

    private final VirtualBuffer vbuf = new VirtualBuffer();
    private int position;
    private VirtualChunk.Parent parent;

    @Override
    protected void hookOnNext(DataChunk item) {
        parent = new VirtualChunk.Parent(item);
        vbuf.offer(item.data(), position);
        int len = vbuf.length();
        int linepos = position;
        for(int i = position ; i < len ; i++) {
            int readByte = vbuf.getByte(i);
            if (readByte == 0xA) {
                submitLine(linepos, i);
                linepos = i + 1;
            }
        }
        position = linepos;
    }

    private void submitLine(int begin, int end) {
        if (end > begin) {
            submit(DIV);
            for (ByteBuffer slice : vbuf.slice(begin, end)) {
                submit(new VirtualChunk(parent, slice));
            }
            submit(SLASH_DIV);
        }
    }

    @Override
    protected void hookOnComplete() {
        submitLine(position, vbuf.length());
    }
}
