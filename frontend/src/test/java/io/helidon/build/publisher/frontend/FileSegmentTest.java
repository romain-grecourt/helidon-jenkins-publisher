package io.helidon.build.publisher.frontend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link FileSegment} test.
 */
public final class FileSegmentTest {

    static final String TEXT1 = "line1\nline2\nline3\nline4\n";
    static final FileSegment SEG1 = new FileSegment(0, createFile(TEXT1));
    static final FileSegment SEG1_POS6 = SEG1.slice(6L);

    static final String TEXT2 = "line1\nline2\nline3\nline4";
    static final FileSegment SEG2 = new FileSegment(0, createFile(TEXT2));

    static final String TEXT3 = "incompleteline";
    static final FileSegment SEG3 = new FileSegment(0, createFile(TEXT3));

    static final String TEXT4 = "\nline\n";
    static final FileSegment SEG4 = new FileSegment(0, createFile(TEXT4));

    static final String TEXT5 = "\nline";
    static final FileSegment SEG5 = new FileSegment(0, createFile(TEXT5));

    static final String TEXT6 = "\n\nline\n\n\n";
    static final FileSegment SEG6 = new FileSegment(0, createFile(TEXT6));

    static final String TEXT7 = "\n";
    static final FileSegment SEG7 = new FileSegment(0, createFile(TEXT7));

    static final String TEXT8 = "";
    static final FileSegment SEG8 = new FileSegment(0, createFile(TEXT8));

    static File createFile(String content) {
        try {
            File file = File.createTempFile(FileSegmentTest.class.getSimpleName(), null);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                writer.flush();
            }
            return file;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create test file", ex);
        }
    }

    @Test
    public void testZeroLines() throws IOException {
        // seg 1
        FileSegment segment = SEG1.findLines(0, false, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg 1 - backward
        segment = SEG1.findLines(0, false, true);
        assertThat(segment.begin, is(SEG1.end));
        assertThat(segment.end, is(SEG1.end));

        // seg 1 - lines_only
        segment = SEG1.findLines(0, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg 1 - lines_only + backward
        segment = SEG1.findLines(0, true, true);
        assertThat(segment.begin, is(SEG1.end));
        assertThat(segment.end, is(SEG1.end));

        // seg 1 - pos=6L
        segment = SEG1_POS6.findLines(0, false, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(6L));

        // seg 1 - pos=6L + backward
        segment = SEG1_POS6.findLines(0, false, true);
        assertThat(segment.begin, is(SEG1.end));
        assertThat(segment.end, is(SEG1.end));

        // seg 1 - pos=6L + lines_only
        segment = SEG1_POS6.findLines(0, true, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(6L));

        // seg 1 - pos=6L + lines_only + backward
        segment = SEG1_POS6.findLines(0, true, true);
        assertThat(segment.begin, is(SEG1.end));
        assertThat(segment.end, is(SEG1.end));
    }

    @Test
    public void testInfiniteLines() throws IOException {
        // seg 1
        FileSegment segment = SEG1.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(SEG1.end));

        // seg1 - backwards
        segment = SEG1.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(SEG1.end));

        // seg1 - lines_only
        segment = SEG1.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(SEG1.end));

        // seg1 - lines_only + backward
        segment = SEG1.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(SEG1.end));

        // seg 2
        segment = SEG2.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - backwards
        segment = SEG2.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - lines_only
        segment = SEG2.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg 2 - lines_only + backward
        segment = SEG2.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg1 - pos=6L
        segment = SEG1_POS6.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(SEG1.end));

        // seg1 - pos=6L + backward
        segment = SEG1_POS6.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(SEG1.end));

        // seg1 - pos=6L + lines_only
        segment = SEG1_POS6.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(SEG1.end));

        // seg1 - pos=6L + line_only + backward
        segment = SEG1_POS6.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(SEG1.end));

        // seg3
        segment = SEG3.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 + backward
        segment = SEG3.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 + lines_only
        segment = SEG3.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg3 + lines_only + backward
        segment = SEG3.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(SEG3.end));
        assertThat(segment.end, is(SEG3.end));

        // seg4
        segment = SEG4.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT4));

        // seg4 + backward
        segment = SEG4.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT4));

        // seg4 + lines_only
        segment = SEG4.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is(TEXT4));

        // seg4 + lines_only + backward
        segment = SEG4.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is(TEXT4));

        // seg5
        segment = SEG5.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT5));

        // seg5 + backward
        segment = SEG5.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT5));

        // seg5 + lines_only
        segment = SEG5.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is("\n"));

        // seg5 + lines_only + backward
        segment = SEG5.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is("\n"));

        // seg7
        segment = SEG7.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 + backward
        segment = SEG7.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 + lines_only
        segment = SEG7.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 + lines_only + backward
        segment = SEG7.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg8
        segment = SEG8.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));

        // seg8 + backward
        segment = SEG8.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));

        // seg8 + lines_only
        segment = SEG8.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));

        // seg8 + lines_only + backward
        segment = SEG8.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));
    }

    @Test
    public void testNLines() throws IOException {
        // seg1 - 1 line
        FileSegment segment = SEG1.findLines(1, false, false);
        assertThat(segment.readString(), is("line1\n"));

        // seg1 - 1 line + backwards
        segment = SEG1.findLines(1, false, true);
        assertThat(segment.readString(), is("line4\n"));

        // seg1 - 1 line + lines_only
        segment = SEG1.findLines(1, true, false);
        assertThat(segment.readString(), is("line1\n"));

        // seg1 - 1 line + lines_only + backward
        segment = SEG1.findLines(1, true, true);
        assertThat(segment.readString(), is("line4\n"));

        // seg 2 - 1 line + backwards
        segment = SEG2.findLines(1, false, true);
        assertThat(segment.readString(), is("line4"));

        // seg 2 - 1 line + lines_only + backward
        segment = SEG2.findLines(1, true, true);
        assertThat(segment.readString(), is("line3\n"));

        // seg1 - 2 lines
        segment = SEG1.findLines(2, false, false);
        assertThat(segment.readString(), is("line1\nline2\n"));

        // seg1 - 2 lines + backwards
        segment = SEG1.findLines(2, false, true);
        assertThat(segment.readString(), is("line3\nline4\n"));

        // seg1 - 2 lines + lines_only
        segment = SEG1.findLines(2, true, false);
        assertThat(segment.readString(), is("line1\nline2\n"));

        // seg1 - 2 lines + lines_only + backward
        segment = SEG1.findLines(2, true, true);
        assertThat(segment.readString(), is("line3\nline4\n"));

        // seg 2 - 2 lines + backwards
        segment = SEG2.findLines(2, false, true);
        assertThat(segment.readString(), is("line3\nline4"));

        // seg 2 - 2 lines + lines_only + backward
        segment = SEG2.findLines(2, true, true);
        assertThat(segment.readString(), is("line2\nline3\n"));

        // seg1 - 4 lines
        segment = SEG1.findLines(4, false, false);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 4 lines + backwards
        segment = SEG1.findLines(4, false, true);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 4 lines + lines_only
        segment = SEG1.findLines(4, true, false);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 4 lines + lines_only + backward
        segment = SEG1.findLines(4, true, true);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 5 lines
        segment = SEG1.findLines(4, false, false);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 5 lines + backward
        segment = SEG1.findLines(4, false, true);
        assertThat(segment.readString(), is(TEXT1));

        // seg 2 - 4 lines
        segment = SEG2.findLines(4, false, false);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - 4 lines + backwards
        segment = SEG2.findLines(4, false, true);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - 4 lines + lines_only
        segment = SEG2.findLines(4, true, false);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg 2 - 4 lines + lines_only + backward
        segment = SEG2.findLines(4, true, true);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg1 - pos=6 + 1 line + lines_only
        segment = SEG1_POS6.findLines(1, true, false);
        assertThat(segment.readString(), is("line2\n"));

        // seg1 - pos=6 + 2 lines
        segment = SEG1_POS6.findLines(2, false, false);
        assertThat(segment.readString(), is("line2\nline3\n"));

        // seg1 - pos=6 + 2 lines + lines_only
        segment = SEG1_POS6.findLines(2, true, false);
        assertThat(segment.readString(), is("line2\nline3\n"));

        // seg1 - pos=6 + 4 lines + lines_only + backward
        segment = SEG1_POS6.findLines(4, true, true);
        assertThat(segment.readString(), is("line2\nline3\nline4\n"));

        // seg3 - 1 line
        segment = SEG3.findLines(1, false, false);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 - 1 line + backward
        segment = SEG3.findLines(1, false, true);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 - 1 line + lines_only
        segment = SEG3.findLines(1, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg3 - 1 line + lines_only + backward
        segment = SEG3.findLines(1, true, true);
        assertThat(segment.begin, is(SEG3.end));
        assertThat(segment.end, is(SEG3.end));

        // seg4 - 1 line
        segment = SEG4.findLines(1, false, false);
        assertThat(segment.readString(), is("\n"));

        // seg4 - 1 line + backward
        segment = SEG4.findLines(1, false, true);
        assertThat(segment.readString(), is("line\n"));

        // seg4 - 1 line + lines_only
        segment = SEG4.findLines(1, true, false);
        assertThat(segment.readString(), is("\n"));

        // seg4 - 1 line + lines_only + backward
        segment = SEG4.findLines(1, true, true);
        assertThat(segment.readString(), is("line\n"));

        // seg5 - 1 line
        segment = SEG5.findLines(1, false, false);
        assertThat(segment.readString(), is("\n"));

        // seg5 - 1 line + backward
        segment = SEG5.findLines(1, false, true);
        assertThat(segment.readString(), is("line"));

        // seg5 - 1 line + lines_only
        segment = SEG5.findLines(1, true, false);
        assertThat(segment.readString(), is("\n"));

        // seg5 - 1 line + lines_only + backward
        segment = SEG5.findLines(1, true, true);
        assertThat(segment.readString(), is("\n"));

        // seg6 - 4 lines
        segment = SEG6.findLines(4, false, false);
        assertThat(segment.readString(), is("\n\nline\n\n"));

        // seg6 - 4 lines + backward
        segment = SEG6.findLines(4, false, true);
        assertThat(segment.readString(), is("\nline\n\n\n"));

        // seg6 - 4 lines + lines_only
        segment = SEG6.findLines(4, true, false);
        assertThat(segment.readString(), is("\n\nline\n\n"));

        // seg6 - 4 lines + lines_only + backward
        segment = SEG6.findLines(4, true, true);
        assertThat(segment.readString(), is("\nline\n\n\n"));

        // seg7 - 1 line
        segment = SEG7.findLines(1, false, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 - 1 line + backward
        segment = SEG7.findLines(1, false, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 - 1 line + lines_only
        segment = SEG7.findLines(1, true, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 - 1 line + lines_only + backward
        segment = SEG7.findLines(1, true, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg8 - 1 line
        segment = SEG8.findLines(1, false, false);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));

        // seg8 - 1 line + backward
        segment = SEG8.findLines(1, false, true);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));

        // seg8 - 1 line + lines_only
        segment = SEG8.findLines(1, true, false);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));

        // seg8 - 1 line + lines_only + backward
        segment = SEG8.findLines(1, true, true);
        assertThat(segment.begin, is(SEG8.begin));
        assertThat(segment.end, is(SEG8.end));
    }

    @Test
    public void testIncremental() throws IOException {
        FileSegment source = SEG1.slice(0L, 14L);
        FileSegment lines = source.findLines(10, true, true); // backward
        assertThat(lines.readString(), is("line1\nline2\n"));
        source = SEG1.slice(lines.end);
        lines = source.findLines(10, true, false); // no backward
        assertThat(lines.readString(), is("line3\nline4\n"));
    }
}