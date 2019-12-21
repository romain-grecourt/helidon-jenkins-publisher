package io.helidon.build.publisher.frontend;

import io.helidon.build.publisher.frontend.reactive.BaseProcessor;
import io.helidon.build.publisher.frontend.reactive.VirtualBuffer;
import io.helidon.build.publisher.frontend.reactive.VirtualChunk;
import io.helidon.common.http.DataChunk;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTML reactive line "encoder".
 */
final class HtmlLineEncoder extends BaseProcessor<DataChunk, DataChunk> {

    private static final Logger LOGGER = Logger.getLogger(HtmlLineEncoder.class.getName());

    static final String PAGE_BEGIN_TEXT = getPageBeginTemplate();
    static final String PAGE_END_TEXT = " <body>\n</html>\n";
    static final String DIV_TEXT = "  <div class=\"line\">";
    static final String SLASH_DIV_TEXT = "</div>\n";

    private final ByteBuffer PAGE_BEGIN_DATA = ByteBuffer.wrap(PAGE_BEGIN_TEXT.getBytes());
    private final ByteBuffer PAGE_END_DATA = ByteBuffer.wrap(PAGE_END_TEXT.getBytes());

    private final ByteBuffer DIV_DATA = ByteBuffer.wrap(DIV_TEXT.getBytes());
    private final ByteBuffer SLASH_DIV_DATA = ByteBuffer.wrap(SLASH_DIV_TEXT.getBytes());

    private final DataChunk PAGE_BEGIN = DataChunk.create(false, PAGE_BEGIN_DATA, true);
    private final DataChunk PAGE_END = DataChunk.create(false, PAGE_END_DATA, true);
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
        if (parent == null) {
            PAGE_BEGIN_DATA.position(0);
            submit(PAGE_BEGIN);
        }
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
        PAGE_END_DATA.position(0);
        submit(PAGE_END);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "onComplete, requestId={0}, lastLine={1}", new Object[] {
                requestId,
                buflen > position
            });
        }
    }

    private static String getPageBeginTemplate() {
        return "<html>\n"
                + " <head>\n"
                + "  <style type=\"text/css\">\n"
                + "    body {\n"
                + "      counter-reset: log;\n"
                + "      padding: 0;\n"
                + "      text-decoration: none;\n"
                + "      font: 13px \"Source Code Pro\", Menlo, Monaco, Consolas, \"Courier New\", monospace;\n"
                + "      display: block;\n"
                + "      position: relative;\n"
                + "      color: #E0E0E0;\n"
                + "      background-color: #424242;\n"
                + "    }\n"
                + "    body > div.line {\n"
                + "      color: #eee;\n"
                + "      position: relative;\n"
                + "      display: block;\n"
                + "      padding: 0px 0 0 30px;\n"
                + "      line-height: 20px;\n"
                + "      word-break: break-all;\n"
                + "    }\n"
                + "    body > div.line:before {\n"
                + "      counter-increment: log;\n"
                + "      content: counter(log);\n"
                + "      min-width: 25px;\n"
                + "      position: absolute;\n"
                + "      display: inline-block;\n"
                + "      text-align: right;\n"
                + "      margin-left: -35px;\n"
                + "      color: #777777;\n"
                + "    }\n"
                + "  </style>\n"
                + " <body>";
    }
}
