package io.helidon.build.publisher.frontend;

import io.helidon.build.publisher.reactive.DataChunkLimiter;
import io.helidon.build.publisher.reactive.Multi;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.RetrySchema;
import io.helidon.media.common.ReadableByteChannelPublisher;
import io.helidon.webserver.BadRequestException;
import io.helidon.webserver.ResponseHeaders;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Service that implements the endpoint used by the UI.
 */
final class FrontendService implements Service {

    private static final RetrySchema RETRY_SCHEMA = RetrySchema.linear(0, 10, 250);
    private static final String LINES_HEADERS = "vnd.io.helidon.publisher.lines";
    private static final String REMAINING_HEADER = "vnd.io.helidon.publisher.remaining";
    private static final String POSITION_HEADER = "vnd.io.helidon.publisher.position";
    private final Path storagePath;

    /**
     * Create a new front-end service.
     * @param path storage path
     */
    FrontendService(String path) {
        Path dirPath = FileSystems.getDefault().getPath(path);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectory(dirPath);
            } catch (IOException ex) {
                throw new IllegalStateException("Error initializing storage directory", ex);
            }
        }
        this.storagePath = dirPath;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::listPipelines)
             .get("/{pipelineId}", this::getPipeline)
             .get("/{pipelineId}/output/{stepId}", this::getOutput);
    }

    private void listPipelines(ServerRequest req, ServerResponse res) {
        int page = toInt(req.queryParams().first("page"), 1);
        int numitems = toInt(req.queryParams().first("numitems"), 20);
        ResponseHeaders headers = res.headers();
        headers.contentType(MediaType.APPLICATION_JSON);
        headers.put("Access-Control-Allow-Origin", "*");
        res.send(MockHelper.mockPipelines(page, numitems));
    }

    private void getPipeline(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        ResponseHeaders headers = res.headers();
        headers.contentType(MediaType.APPLICATION_JSON);
        headers.put("Access-Control-Allow-Origin", "*");
        if ("non-existent".equals(pipelineId)) {
            res.status(404).send();
        } else {
            res.send(MockHelper.mockPipeline(pipelineId));
        }
    }

    private void getDescriptor(ServerRequest req, ServerResponse res) {
        // TODO adapt between backend model and UI model
        // OR, maybe not..
        String pipelineId = req.path().param("pipelineId");
        Path filePath = storagePath.resolve(pipelineId + "/pipeline.json");
        if (Files.exists(filePath)) {
            res.status(404).send();
            return;
        }
        try {
            long size = Files.size(filePath);
            FileChannel fc = FileChannel.open(filePath, StandardOpenOption.READ);
            Multi<DataChunk> publisher = Multi
                    .from(new ReadableByteChannelPublisher(fc, RETRY_SCHEMA))
                    .limit(new DataChunkLimiter(size));
            res.headers().contentType(MediaType.APPLICATION_JSON);
            res.headers().contentLength(size);
            res.send(publisher);
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private void getOutput(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        int stepId = toInt(Optional.ofNullable(req.path().param("stepId")));

        // number of lines (default is infinite)
        int lines = toInt(req.queryParams().first("lines"), Integer.MAX_VALUE);
        // start position (default is 0)
        long position = toLong(req.queryParams().first("position"), 0L);
        // count lines from the end? (default is false)
        boolean backward = req.queryParams().first("backward").map(Boolean::valueOf).orElse(false);
        // return only complete lines? (default is false)
        boolean linesOnly = req.queryParams().first("lines_only").map(Boolean::valueOf).orElse(false);
        // wrap each lines with div markups ? (default is false)
        boolean wrapHtml = req.queryParams().first("html").map(Boolean::valueOf).orElse(false);

        Path filePath = storagePath.resolve(pipelineId + "/step-" + stepId + ".log");
        if (!Files.exists(filePath)) {
            res.status(404).send();
            return;
        }

        try {
            FileSegment fseg;
            if (backward) {
                fseg = new FileSegment(0, position == 0 ? Files.size(filePath): position, filePath.toFile());
            } else {
                fseg = new FileSegment(0, Files.size(filePath), filePath.toFile());
            }
            FileSegment lseg = fseg.findLines(lines, linesOnly, backward);
            ResponseHeaders headers = res.headers();
            headers.contentType(MediaType.TEXT_PLAIN);
            headers.put(LINES_HEADERS, String.valueOf(lseg.lines));
            headers.put(REMAINING_HEADER, String.valueOf(backward ? lseg.begin : fseg.end - lseg.end));
            headers.put(POSITION_HEADER, String.valueOf(lseg.end));

            // TODO remove me
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Expose-Headers", LINES_HEADERS, REMAINING_HEADER, POSITION_HEADER);

            Publisher<DataChunk> publisher = new FileSegmentPublisher(lseg);
            if (!wrapHtml) {
                res.send(publisher);
            } else {
                HtmlLineEncoder htmlEncoder = new HtmlLineEncoder(req.requestId());
                publisher.subscribe(htmlEncoder);
                res.send(htmlEncoder);
            }
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private static int toInt(Optional<String> optional) {
        return optional.map(Integer::valueOf)
                .orElseThrow(() -> new BadRequestException(""));
    }

    private static int toInt(Optional<String> optional, int defaultValue) {
        try {
            return optional.map(Integer::valueOf).orElse(defaultValue);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("");
        }
    }

    private static long toLong(Optional<String> optional, long defaultValue) {
        try {
            return optional.map(Long::valueOf).orElse(defaultValue);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("");
        }
    }
}
