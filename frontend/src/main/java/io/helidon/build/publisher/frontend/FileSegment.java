package io.helidon.build.publisher.frontend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Pair of being and end position.
 */
final class FileSegment {

    /**
     * Segment start, inclusive.
     */
    final long begin;

    /**
     * Segment end, exclusive.
     */
    final long end;

    /**
     * The underlying file.
     */
    final RandomAccessFile raf;

    // TODO add lines attributes
    // initialize it to -1

    final int lines;

    /**
     * Create a new segment.
     *
     * @param begin begin position
     * @param file the file
     */
    FileSegment(long begin, File file) {
        this(begin, file.length(), file);
    }

    /**
     * Create a new segment.
     *
     * @param begin begin position
     * @param end end position
     * @param file the file
     */
    FileSegment(long begin, long end, File file) {
        this.begin = begin;
        this.end = end;
        this.lines = -1;
        try {
            this.raf = new RandomAccessFile(file, "r");
        } catch(FileNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Create a new segment.
     *
     * @param begin begin position
     * @param end end position
     */
    private FileSegment(long begin, long end, int lines, RandomAccessFile raf) {
        this.begin = begin;
        this.end = end;
        this.lines = lines;
        this.raf  = raf;
    }

    /**
     * Return a new segment starting at position.
     * @param position the new begin position
     * @return FileSegment
     * @throws IllegalArgumentException if position is greater than the end of this segment
     */
    FileSegment slice(long position) {
        if (position > end) {
            throw new IllegalArgumentException("Invalid position");
        }
        return new FileSegment(position, end, -1, raf);
    }

    /**
     * Return a new segment starting at position.
     * @param position the new begin position
     * @return FileSegment
     * @throws IllegalArgumentException if position is greater than the end of this segment
     * @throws IllegalArgumentException if {@code position + limit} is greater than the end of this segment
     */
    FileSegment slice(long position, long limit) {
        if (position > end) {
            throw new IllegalArgumentException("Invalid position");
        }
        if (position + limit > end) {
            throw new IllegalArgumentException("Invalid limit");
        }
        return new FileSegment(position, position + limit, -1, raf);
    }

    /**
     * Read this segment into a {@link String}.
     * @return String
     * @throws IOException if an IO error occurs
     */
    String readString() throws IOException {
        raf.seek(begin);
        byte[] buf = new byte[lenght()];
        raf.read(buf);
        return new String(buf);
    }

    /**
     * Return the segment length.
     * @return int
     */
    int lenght() {
        return (int) (end - begin);
    }

    /**
     * Find the segment for a number of lines from the end of a file within this segment.
     *
     * @param filePath the path of the file to search again
     * @param lines the number of lines to search:, a non positive value results in an empty segment, and
     * {@code Integer.MAX_VALUE} results in a segment that begins at {@code 0}
     * @param linesOnly if {@code true} the end position of the resulting segment matches the end of a line
     * @param backward if {@code true} the lines are counted from the end of this segment, otherwise from the beginning
     * @return BufferSegment
     * @throws IOException 
     */
    FileSegment findLines(int lines, boolean linesOnly, boolean backward) throws IOException {
        if (lines == Integer.MAX_VALUE && !linesOnly) {
            return this;
        }
        if (backward) {
            return findLinesBackward(lines, linesOnly);
        } else {
            return findLinesForward(lines, linesOnly);
        }
    }

    private FileSegment findLinesForward(int lines, boolean linesOnly) throws IOException {
        int numlines = 0;
        long beginPos = begin;
        long endPos = begin;
        long linePos = begin;
        raf.seek(beginPos);
        while((lines == Integer.MAX_VALUE  || numlines < lines) && endPos < end) {
            int readByte = raf.readByte() & 0xFF;
            if (readByte == 0xA) {
                linePos = endPos + 1;
                numlines++;
            } else if (readByte < 0) {
                break;
            }
            endPos++;
        }
        if (linesOnly) {
            endPos = linePos;
        }
        return new FileSegment(beginPos, endPos, numlines, raf);
    }

    private FileSegment findLinesBackward(int lines, boolean linesOnly) throws IOException {
        int numlines = 0;
        long beginPos = end;
        long endPos = end;
        boolean skip = linesOnly;
        int readByte = 0;
        while((lines == Integer.MAX_VALUE || numlines < lines) && beginPos > begin) {
            raf.seek(--beginPos);
            readByte = raf.readByte() & 0xFF;
            if (readByte == 0xA) {
                if (skip) {
                    endPos = beginPos + 1;
                    skip = false;
                } else if (beginPos < end - 1) {
                    numlines++;
                }
            }
        }
        if (skip) {
            beginPos = endPos;
        } else if (lines != Integer.MAX_VALUE && readByte == 0xA && endPos - beginPos > 1) {
            beginPos++;
        }
        return new FileSegment(beginPos, endPos, numlines, raf);
    }
}
