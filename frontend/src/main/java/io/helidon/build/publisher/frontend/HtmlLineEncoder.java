package io.helidon.build.publisher.frontend;

import io.helidon.build.publisher.reactive.BaseProcessor;
import io.helidon.build.publisher.reactive.VirtualBuffer;
import io.helidon.build.publisher.reactive.VirtualChunk;
import io.helidon.common.http.DataChunk;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTML reactive line "encoder".
 */
final class HtmlLineEncoder extends BaseProcessor<DataChunk, DataChunk> {

    private static final Logger LOGGER = Logger.getLogger(HtmlLineEncoder.class.getName());

    static final String DIV_TEXT = "<div class=\"line\">";
    static final String SLASH_DIV_TEXT = "</div>";

    private final ByteBuffer DIV_DATA = ByteBuffer.wrap(DIV_TEXT.getBytes());
    private final ByteBuffer SLASH_DIV_DATA = ByteBuffer.wrap(SLASH_DIV_TEXT.getBytes());

    private final DataChunk DIV = DataChunk.create(false, DIV_DATA, true);
    private final DataChunk SLASH_DIV = DataChunk.create(false, SLASH_DIV_DATA, true);

    private final VirtualBuffer vbuf = new VirtualBuffer();
    private int position;
    private VirtualChunk.Parent parent;
    private final long requestId;

    HtmlLineEncoder(long requestId) {
        this.requestId = requestId;
    }

    @Override
    protected void hookOnNext(DataChunk item) {
        parent = new VirtualChunk.Parent(item);
        vbuf.offer(item.data(), position);
        position = 0;
        int len = vbuf.length();
        int linepos = position;
        int lines = 0;
        for(int i = position ; i < len ; i++) {
            int readByte = vbuf.getByte(i);
            if (readByte == 0xA) {
                submitLine(linepos, i);
                linepos = i + 1;
                lines++;
            }
        }
        position = linepos;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "onNext, requestId={0}, lines={1}, newposition={2}", new Object[] {
                requestId,
                lines,
                position
            });
        }
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
        int buflen = vbuf.length();
        submitLine(position, buflen);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "onComplete, requestId={0}, lastLine={1}", new Object[] {
                requestId,
                buflen > position
            });
        }
    }
}
