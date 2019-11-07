package io.helidon.build.publisher.webapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Line filtering processor.
 */
final class LineFilteringProcessor extends BaseProcessor<ByteBuffer, String> {

    private static final Logger LOGGER = Logger.getLogger(LineFilteringProcessor.class.getName());

    private final int nlines;
    private volatile int lineno;

    /**
     * Create a new line filtering processor.s
     * @param nlines the number of lines relative to the beginning to not publish.
     */
    LineFilteringProcessor(int nlines) {
        this.nlines = nlines;
    }

    @Override
    protected void hookOnNext(ByteBuffer buffer) {
        for(String line : readLines(buffer)) {
            if (lineno < nlines) {
                lineno++;
            } else {
                submit(line);
            }
        }
    }

    /**
     * Read all lines from a byte buffer.
     * @param buffer
     * @return {@code List<String>}, never {@code null}
     */
    private static List<String> readLines(ByteBuffer buffer) {
        LinkedList<String> lines = new LinkedList<>();
        InputStream is = new ByteBufferBackedInputStream(buffer);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return lines;
    }

    /**
     * Simple {@link InputStream} implementation to read the content of a {@link ByteBuffer}.
     */
    private static final class ByteBufferBackedInputStream extends InputStream {

        private final ByteBuffer buf;

        ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        @Override
        public int read(byte[] bytes, int off, int len)
                throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }
}
