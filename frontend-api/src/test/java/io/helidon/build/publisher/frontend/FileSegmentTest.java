package io.helidon.build.publisher.frontend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeAll;

/**
 * {@link FileSegment} test.
 */
public final class FileSegmentTest {

    private static final String TEXT1 = "line1\nline2\nline3\nline4\n";
    private static FileSegment seg1;
    private static FileSegment seg1_pos6;

    private static final String TEXT2 = "line1\nline2\nline3\nline4";
    private static FileSegment seg2;

    private static final String TEXT3 = "incompleteline";
    private static FileSegment seg3;

    private static final String TEXT4 = "\nline\n";
    private static FileSegment seg4;

    private static final String TEXT5 = "\nline";
    private static FileSegment seg5;

    private static final String TEXT6 = "\n\nline\n\n\n";
    private static FileSegment seg6;

    private static final String TEXT7 = "\n";
    private static FileSegment seg7;

    private static final String TEXT8 = "";
    private static FileSegment seg8;

    static File createFile(String content) {
        try {
            File file = File.createTempFile(FileSegmentTest.class.getSimpleName(), null);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                writer.flush();
            }
            return new File(file.getAbsolutePath());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create test file", ex);
        }
    }

    @BeforeAll
    public static void setup() {
        seg1 = new FileSegment(0, createFile(TEXT1));
        seg1_pos6 = seg1.slice(6L);
        seg2 = new FileSegment(0, createFile(TEXT2));
        seg3 = new FileSegment(0, createFile(TEXT3));
        seg4 = new FileSegment(0, createFile(TEXT4));
        seg5 = new FileSegment(0, createFile(TEXT5));
        seg6 = new FileSegment(0, createFile(TEXT6));
        seg7 = new FileSegment(0, createFile(TEXT7));
        seg8 = new FileSegment(0, createFile(TEXT8));
    }

    @Test
    public void testZeroLines() throws IOException {
        // seg 1
        FileSegment segment = seg1.findLines(0, false, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg 1 - backward
        segment = seg1.findLines(0, false, true);
        assertThat(segment.begin, is(seg1.end));
        assertThat(segment.end, is(seg1.end));

        // seg 1 - lines_only
        segment = seg1.findLines(0, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg 1 - lines_only + backward
        segment = seg1.findLines(0, true, true);
        assertThat(segment.begin, is(seg1.end));
        assertThat(segment.end, is(seg1.end));

        // seg 1 - pos=6L
        segment = seg1_pos6.findLines(0, false, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(6L));

        // seg 1 - pos=6L + backward
        segment = seg1_pos6.findLines(0, false, true);
        assertThat(segment.begin, is(seg1.end));
        assertThat(segment.end, is(seg1.end));

        // seg 1 - pos=6L + lines_only
        segment = seg1_pos6.findLines(0, true, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(6L));

        // seg 1 - pos=6L + lines_only + backward
        segment = seg1_pos6.findLines(0, true, true);
        assertThat(segment.begin, is(seg1.end));
        assertThat(segment.end, is(seg1.end));
    }

    @Test
    public void testInfiniteLines() throws IOException {
        // seg 1
        FileSegment segment = seg1.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(seg1.end));

        // seg1 - backwards
        segment = seg1.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(seg1.end));

        // seg1 - lines_only
        segment = seg1.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(seg1.end));

        // seg1 - lines_only + backward
        segment = seg1.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(seg1.end));

        // seg 2
        segment = seg2.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - backwards
        segment = seg2.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - lines_only
        segment = seg2.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg 2 - lines_only + backward
        segment = seg2.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg1 - pos=6L
        segment = seg1_pos6.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(seg1.end));

        // seg1 - pos=6L + backward
        segment = seg1_pos6.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(seg1.end));

        // seg1 - pos=6L + lines_only
        segment = seg1_pos6.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(seg1.end));

        // seg1 - pos=6L + line_only + backward
        segment = seg1_pos6.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(6L));
        assertThat(segment.end, is(seg1.end));

        // seg3
        segment = seg3.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 + backward
        segment = seg3.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 + lines_only
        segment = seg3.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg3 + lines_only + backward
        segment = seg3.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(seg3.end));
        assertThat(segment.end, is(seg3.end));

        // seg4
        segment = seg4.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT4));

        // seg4 + backward
        segment = seg4.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT4));

        // seg4 + lines_only
        segment = seg4.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is(TEXT4));

        // seg4 + lines_only + backward
        segment = seg4.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is(TEXT4));

        // seg5
        segment = seg5.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT5));

        // seg5 + backward
        segment = seg5.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT5));

        // seg5 + lines_only
        segment = seg5.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is("\n"));

        // seg5 + lines_only + backward
        segment = seg5.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is("\n"));

        // seg7
        segment = seg7.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 + backward
        segment = seg7.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 + lines_only
        segment = seg7.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 + lines_only + backward
        segment = seg7.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg8
        segment = seg8.findLines(Integer.MAX_VALUE, false, false);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));

        // seg8 + backward
        segment = seg8.findLines(Integer.MAX_VALUE, false, true);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));

        // seg8 + lines_only
        segment = seg8.findLines(Integer.MAX_VALUE, true, false);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));

        // seg8 + lines_only + backward
        segment = seg8.findLines(Integer.MAX_VALUE, true, true);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));
    }

    @Test
    public void testNLines() throws IOException {
        // seg1 - 1 line
        FileSegment segment = seg1.findLines(1, false, false);
        assertThat(segment.readString(), is("line1\n"));

        // seg1 - 1 line + backwards
        segment = seg1.findLines(1, false, true);
        assertThat(segment.readString(), is("line4\n"));

        // seg1 - 1 line + lines_only
        segment = seg1.findLines(1, true, false);
        assertThat(segment.readString(), is("line1\n"));

        // seg1 - 1 line + lines_only + backward
        segment = seg1.findLines(1, true, true);
        assertThat(segment.readString(), is("line4\n"));

        // seg 2 - 1 line + backwards
        segment = seg2.findLines(1, false, true);
        assertThat(segment.readString(), is("line4"));

        // seg 2 - 1 line + lines_only + backward
        segment = seg2.findLines(1, true, true);
        assertThat(segment.readString(), is("line3\n"));

        // seg1 - 2 lines
        segment = seg1.findLines(2, false, false);
        assertThat(segment.readString(), is("line1\nline2\n"));

        // seg1 - 2 lines + backwards
        segment = seg1.findLines(2, false, true);
        assertThat(segment.readString(), is("line3\nline4\n"));

        // seg1 - 2 lines + lines_only
        segment = seg1.findLines(2, true, false);
        assertThat(segment.readString(), is("line1\nline2\n"));

        // seg1 - 2 lines + lines_only + backward
        segment = seg1.findLines(2, true, true);
        assertThat(segment.readString(), is("line3\nline4\n"));

        // seg 2 - 2 lines + backwards
        segment = seg2.findLines(2, false, true);
        assertThat(segment.readString(), is("line3\nline4"));

        // seg 2 - 2 lines + lines_only + backward
        segment = seg2.findLines(2, true, true);
        assertThat(segment.readString(), is("line2\nline3\n"));

        // seg1 - 4 lines
        segment = seg1.findLines(4, false, false);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 4 lines + backwards
        segment = seg1.findLines(4, false, true);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 4 lines + lines_only
        segment = seg1.findLines(4, true, false);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 4 lines + lines_only + backward
        segment = seg1.findLines(4, true, true);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 5 lines
        segment = seg1.findLines(4, false, false);
        assertThat(segment.readString(), is(TEXT1));

        // seg1 - 5 lines + backward
        segment = seg1.findLines(4, false, true);
        assertThat(segment.readString(), is(TEXT1));

        // seg 2 - 4 lines
        segment = seg2.findLines(4, false, false);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - 4 lines + backwards
        segment = seg2.findLines(4, false, true);
        assertThat(segment.readString(), is(TEXT2));

        // seg 2 - 4 lines + lines_only
        segment = seg2.findLines(4, true, false);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg 2 - 4 lines + lines_only + backward
        segment = seg2.findLines(4, true, true);
        assertThat(segment.readString(), is("line1\nline2\nline3\n"));

        // seg1 - pos=6 + 1 line + lines_only
        segment = seg1_pos6.findLines(1, true, false);
        assertThat(segment.readString(), is("line2\n"));

        // seg1 - pos=6 + 2 lines
        segment = seg1_pos6.findLines(2, false, false);
        assertThat(segment.readString(), is("line2\nline3\n"));

        // seg1 - pos=6 + 2 lines + lines_only
        segment = seg1_pos6.findLines(2, true, false);
        assertThat(segment.readString(), is("line2\nline3\n"));

        // seg1 - pos=6 + 4 lines + lines_only + backward
        segment = seg1_pos6.findLines(4, true, true);
        assertThat(segment.readString(), is("line2\nline3\nline4\n"));

        // seg3 - 1 line
        segment = seg3.findLines(1, false, false);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 - 1 line + backward
        segment = seg3.findLines(1, false, true);
        assertThat(segment.readString(), is(TEXT3));

        // seg3 - 1 line + lines_only
        segment = seg3.findLines(1, true, false);
        assertThat(segment.begin, is(0L));
        assertThat(segment.end, is(0L));

        // seg3 - 1 line + lines_only + backward
        segment = seg3.findLines(1, true, true);
        assertThat(segment.begin, is(seg3.end));
        assertThat(segment.end, is(seg3.end));

        // seg4 - 1 line
        segment = seg4.findLines(1, false, false);
        assertThat(segment.readString(), is("\n"));

        // seg4 - 1 line + backward
        segment = seg4.findLines(1, false, true);
        assertThat(segment.readString(), is("line\n"));

        // seg4 - 1 line + lines_only
        segment = seg4.findLines(1, true, false);
        assertThat(segment.readString(), is("\n"));

        // seg4 - 1 line + lines_only + backward
        segment = seg4.findLines(1, true, true);
        assertThat(segment.readString(), is("line\n"));

        // seg5 - 1 line
        segment = seg5.findLines(1, false, false);
        assertThat(segment.readString(), is("\n"));

        // seg5 - 1 line + backward
        segment = seg5.findLines(1, false, true);
        assertThat(segment.readString(), is("line"));

        // seg5 - 1 line + lines_only
        segment = seg5.findLines(1, true, false);
        assertThat(segment.readString(), is("\n"));

        // seg5 - 1 line + lines_only + backward
        segment = seg5.findLines(1, true, true);
        assertThat(segment.readString(), is("\n"));

        // seg6 - 4 lines
        segment = seg6.findLines(4, false, false);
        assertThat(segment.readString(), is("\n\nline\n\n"));

        // seg6 - 4 lines + backward
        segment = seg6.findLines(4, false, true);
        assertThat(segment.readString(), is("\nline\n\n\n"));

        // seg6 - 4 lines + lines_only
        segment = seg6.findLines(4, true, false);
        assertThat(segment.readString(), is("\n\nline\n\n"));

        // seg6 - 4 lines + lines_only + backward
        segment = seg6.findLines(4, true, true);
        assertThat(segment.readString(), is("\nline\n\n\n"));

        // seg7 - 1 line
        segment = seg7.findLines(1, false, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 - 1 line + backward
        segment = seg7.findLines(1, false, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 - 1 line + lines_only
        segment = seg7.findLines(1, true, false);
        assertThat(segment.readString(), is(TEXT7));

        // seg7 - 1 line + lines_only + backward
        segment = seg7.findLines(1, true, true);
        assertThat(segment.readString(), is(TEXT7));

        // seg8 - 1 line
        segment = seg8.findLines(1, false, false);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));

        // seg8 - 1 line + backward
        segment = seg8.findLines(1, false, true);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));

        // seg8 - 1 line + lines_only
        segment = seg8.findLines(1, true, false);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));

        // seg8 - 1 line + lines_only + backward
        segment = seg8.findLines(1, true, true);
        assertThat(segment.begin, is(seg8.begin));
        assertThat(segment.end, is(seg8.end));
    }

    @Test
    public void testIncremental() throws IOException {
        FileSegment source = seg1.slice(0L, 14L);
        FileSegment lines = source.findLines(10, true, true); // backward
        assertThat(lines.readString(), is("line1\nline2\n"));
        source = seg1.slice(lines.end);
        lines = source.findLines(10, true, false); // no backward
        assertThat(lines.readString(), is("line3\nline4\n"));
    }
}
