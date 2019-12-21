package io.helidon.build.publisher.frontend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.build.publisher.model.Artifacts;
import io.helidon.build.publisher.model.Pipeline;
import io.helidon.build.publisher.model.PipelineInfo;
import io.helidon.build.publisher.model.PipelineInfos;
import io.helidon.build.publisher.model.DescriptorFileManager;
import io.helidon.build.publisher.model.TestSuiteResult;
import io.helidon.build.publisher.model.TestSuiteResults;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.webserver.BadRequestException;
import io.helidon.webserver.ResponseHeaders;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import static io.helidon.common.http.Http.Status.NOT_FOUND_404;

/**
 * Service that implements the endpoint used by the UI.
 */
final class FrontendService implements Service {

    private static final Logger LOGGER = Logger.getLogger(FrontendService.class.getName());
    private static final String LINES_HEADERS = "vnd.io.helidon.publisher.lines";
    private static final String REMAINING_HEADER = "vnd.io.helidon.publisher.remaining";
    private static final String POSITION_HEADER = "vnd.io.helidon.publisher.position";

    private final Path storagePath;
    private final DescriptorFileManager descriptorManager;
    private final ContentTypeSelector contentTypeSelector;

    /**
     * Create a new front-end service.
     * @param storagePath storage path
     */
    FrontendService(Path storagePath) {
        this.storagePath = storagePath;
        if (!Files.exists(storagePath)) {
            try {
                Files.createDirectories(storagePath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        descriptorManager = new DescriptorFileManager(storagePath);
        contentTypeSelector = new ContentTypeSelector(null);
        LOGGER.log(Level.INFO, "Creating frontend service, storagePath={0}", storagePath);
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::listPipelines)
             .get("/{pipelineId}", this::getPipeline)
             .get("/{pipelineId}/output/{stepId}", this::getOutput)
             .get("/{pipelineId}/artifacts/{stageId}", this::getArtifacts)
             .get("/{pipelineId}/artifacts/{stageId}/{filepath:.+}", this::getArtifact)
             .get("/{pipelineId}/tests/{stageId}", this::getTests);
    }

    private void getTests(ServerRequest req, ServerResponse res) {
        ResponseHeaders headers = res.headers();
        headers.contentType(MediaType.APPLICATION_JSON);
        // TODO remove me
        headers.put("Access-Control-Allow-Origin", "*");

        Path pipelinePath = storagePath.resolve(req.path().param("pipelineId"));
        Path stagePath = pipelinePath.resolve(req.path().param("stageId"));
        if (!stagePath.getParent().equals(pipelinePath)) {
            throw new BadRequestException("Invalid stageId");
        }
        if (!(Files.exists(stagePath) && Files.isDirectory(stagePath))) {
            res.send(NOT_FOUND_404);
            return;
        }
        Path testsPath = stagePath.resolve("tests");
        try {
            List<TestSuiteResult> results = Files.list(testsPath)
                    .filter((path) -> path.toString().endsWith(".json"))
                    .map(descriptorManager::loadTestSuiteResult)
                    .collect(Collectors.toList());
            res.send(new TestSuiteResults(results));
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private void getArtifacts(ServerRequest req, ServerResponse res) {
        ResponseHeaders headers = res.headers();
        // TODO remove me
        headers.put("Access-Control-Allow-Origin", "*");

        Path pipelinePath = storagePath.resolve(req.path().param("pipelineId"));
        Path stagePath = pipelinePath.resolve(req.path().param("stageId"));
        if (!stagePath.getParent().equals(pipelinePath)) {
            throw new BadRequestException("Invalid stageId");
        }
        if (!(Files.exists(stagePath) && Files.isDirectory(stagePath))) {
            res.send(NOT_FOUND_404);
            return;
        }
        try {
            headers.contentType(MediaType.APPLICATION_JSON);
            res.send(Artifacts.find(stagePath.resolve("artifacts")));
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private void getArtifact(ServerRequest req, ServerResponse res) {
        // produce raw text ? (default is false)
        boolean download = toBoolean(req.queryParams().first("download"), false);

        ResponseHeaders headers = res.headers();
        // TODO remove me
        headers.put("Access-Control-Allow-Origin", "*");

        Path pipelinePath = storagePath.resolve(req.path().param("pipelineId"));
        Path stagePath = pipelinePath.resolve(req.path().param("stageId"));
        if (!stagePath.getParent().equals(pipelinePath)) {
            throw new BadRequestException("Invalid stageId");
        }
        Path artifactsPath = stagePath.resolve("artifacts");
        Path filePath = artifactsPath.resolve(req.path().param("filepath"));
        if (!filePath.startsWith(artifactsPath)) {
            throw new BadRequestException("Invalid filepath");
        }
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            res.status(NOT_FOUND_404).send();
        } else {
            try {
                if (download) {
                    headers.contentType(MediaType.APPLICATION_OCTET_STREAM);
                    headers.put("Content-Disposition", "attachment; filename=\"" + filePath.getFileName().toString() + "\"");
                } else {
                    headers.contentType(contentTypeSelector.determine(filePath.getFileName().toString(), req.headers()));
                }
                res.send(new FileSegmentPublisher(new FileSegment(0, Files.size(filePath), filePath.toFile())));
            } catch (IOException ex) {
                req.next(ex);
            }
        }
    }

    private void listPipelines(ServerRequest req, ServerResponse res) {
        int pagenum = toInt(req.queryParams().first("pagenum"), 1);
        int numitems = toInt(req.queryParams().first("numitems"), 20);
        ResponseHeaders headers = res.headers();
        // TODO remove me
        headers.put("Access-Control-Allow-Origin", "*");
        try {
            List<Path> allDescriptors = Files.list(storagePath)
                    .sorted(Comparator.<Path>comparingLong((p) -> p.toFile().lastModified()).reversed())
                    .collect(Collectors.toList());
            List<Path> pageDescriptors = allDescriptors.stream()
                    .skip((pagenum - 1) * numitems) // skip to get to the current page
                    .limit(numitems) // max limit calculate the remaining pages
                    .collect(Collectors.toList());
            List<PipelineInfo> pipelines = pageDescriptors.stream()
                    .limit(numitems)
                    .map(descriptorManager::loadInfoFromDir)
                    .collect(Collectors.toList());
            int totalsize = allDescriptors.size();
            int totalpages = (int) totalsize / numitems;
            if (totalsize % numitems > 0) {
                totalpages++;
            }
            headers.contentType(MediaType.APPLICATION_JSON);
            res.send(new PipelineInfos(pipelines, pagenum, totalpages));
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private void getPipeline(ServerRequest req, ServerResponse res) {
        ResponseHeaders headers = res.headers();
        // TODO remove me
        headers.put("Access-Control-Allow-Origin", "*");
        Pipeline pipeline = descriptorManager.loadPipeline(req.path().param("pipelineId"));
        if (pipeline != null) {
            headers.contentType(MediaType.APPLICATION_JSON);
            res.send(pipeline);
        } else {
            res.status(NOT_FOUND_404).send();
        }
    }

    private void getOutput(ServerRequest req, ServerResponse res) {
        // number of lines (default is infinite)
        int lines = toInt(req.queryParams().first("lines"), Integer.MAX_VALUE);
        // start position (default is 0)
        long position = toLong(req.queryParams().first("position"), 0L);
        // count lines from the end? (default is false)
        boolean backward = toBoolean(req.queryParams().first("backward"), false);
        // return only complete lines? (default is false)
        boolean linesOnly = toBoolean(req.queryParams().first("lines_only"), false);
        // produce html ? (default is false)
        boolean html = toBoolean(req.queryParams().first("html"), false);
        // produce raw text ? (default is false)
        boolean raw = toBoolean(req.queryParams().first("html"), false);

        String pipelineId = req.path().param("pipelineId");
        Path pipelinePath = storagePath.resolve(pipelineId);
        String stepId = req.path().param("stepId");
        String fname = "step-" + stepId + ".log";
        Path filePath = pipelinePath.resolve(fname);
        if (!filePath.getParent().equals(pipelinePath)) {
            throw new BadRequestException("Invalid stepId");
        }

        ResponseHeaders headers = res.headers();
        // TODO remove me
        headers.put("Access-Control-Allow-Origin", "*");
        if (!Files.exists(filePath)) {
            res.status(404).send();
            return;
        }

        try {
            FileSegment fseg;
            if (backward) {
                fseg = new FileSegment(0, position == 0 ? Files.size(filePath): position, filePath.toFile());
            } else {
                fseg = new FileSegment(position, Files.size(filePath), filePath.toFile());
            }
            FileSegment lseg = fseg.findLines(lines, linesOnly, backward);
            // TODO remove me
            headers.put("Access-Control-Expose-Headers", LINES_HEADERS, REMAINING_HEADER, POSITION_HEADER);

            headers.put(LINES_HEADERS, String.valueOf(lseg.lines));
            headers.put(REMAINING_HEADER, String.valueOf(backward ? lseg.begin : fseg.end - lseg.end));
            headers.put(POSITION_HEADER, String.valueOf(lseg.end));

            Publisher<DataChunk> publisher = new FileSegmentPublisher(lseg);
            if (!html) {
                if (raw) {
                    headers.contentType(MediaType.TEXT_PLAIN);
                } else {
                    headers.contentType(MediaType.APPLICATION_OCTET_STREAM);
                    headers.put("Content-Disposition", "attachment; filename=\"" + pipelineId + "-" + stepId + ".log\"");
                }
                res.send(publisher);
            } else {
                headers.contentType(MediaType.TEXT_HTML);
                HtmlLineEncoder htmlEncoder = new HtmlLineEncoder(req.requestId());
                publisher.subscribe(htmlEncoder);
                res.send(htmlEncoder);
            }
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private static boolean toBoolean(Optional<String> optional, boolean defaultValue) {
        return optional.map((s) -> s.isEmpty() || Boolean.valueOf(s)).orElse(false);
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
