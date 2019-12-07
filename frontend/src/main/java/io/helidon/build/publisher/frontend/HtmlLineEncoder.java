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

    static final String DIV_TEXT = "<div class=\"line\">";
    static final String SLASH_DIV_TEXT = "</div>";

    private final ByteBuffer DIV_DATA = ByteBuffer.wrap(DIV_TEXT.getBytes());
    private final ByteBuffer SLASH_DIV_DATA = ByteBuffer.wrap(SLASH_DIV_TEXT.getBytes());

    private final DataChunk DIV = DataChunk.create(false, DIV_DATA, true);
    private final DataChunk SLASH_DIV = DataChunk.create(false, SLASH_DIV_DATA, true);

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
            DIV_DATA.position(0);
            submit(DIV);
            for (ByteBuffer slice : vbuf.slice(begin, end)) {
                submit(new VirtualChunk(parent, slice));
            }
            SLASH_DIV_DATA.position(0);
            submit(SLASH_DIV);
        }
    }

    @Override
    protected void hookOnComplete() {
        submitLine(position, vbuf.length());
    }
}
