package io.helidon.build.publisher.backend;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import io.helidon.build.publisher.model.PipelineDescriptorFileManager;
import io.helidon.build.publisher.model.PipelineEventProcessor;
import io.helidon.build.publisher.model.events.PipelineEvents;

import static io.helidon.common.http.Http.Status.CREATED_201;
import static io.helidon.common.http.Http.Status.OK_200;

/**
 * This service implements the endpoints used by the Jenkins plugin.
 */
final class BackendService implements Service {

    private final Path storagePath;
    private final PipelineEventProcessor eventProcessor;
    private final FileAppender appender;

    /**
     * Create a new instance.
     * @param path storage path
     * @param appenderThreads number of threads used for appending data
     */
    BackendService(String path, int appenderThreads) {
        this.storagePath = FileSystems.getDefault().getPath(path);
        this.appender = new FileAppender(storagePath, appenderThreads);
        this.eventProcessor = new PipelineEventProcessor(new PipelineDescriptorFileManager(storagePath));
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.put("/events", this::processEvents)
             .put("/output/{pipelineId}/{stepId}", this::appendOutput)
             .post("/files/{pipelineId}/{filepath:.+}", this::uploadFile);
    }

    private void processEvents(ServerRequest req, ServerResponse res) {
        req.content().as(PipelineEvents.class).thenAccept(pipelineEvents -> {
            eventProcessor.process(pipelineEvents.events());
            res.status(OK_200).send();
        }).exceptionally(AsyncHandlers.error(req));
    }

    private void appendOutput(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        int stepId = Integer.valueOf(req.path().param("stepId"));
        // TODO double check that filepath is a file under storage/pipelineId
        appender.append(req.content(), pipelineId + "/step-" + stepId + ".log", isCompressed(req))
                .thenAccept(AsyncHandlers.status(res, OK_200))
                .exceptionally(AsyncHandlers.error(req));
    }

    private void uploadFile(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        String filepath = req.path().param("filepath");
        // TODO double check that filepath is nested under storage/pipelineId
        // TODO double check taht filepath is nested under a subdirectory under storage/pipelineId
        appender.append(req.content(), pipelineId + "/" + filepath, isCompressed(req))
                .thenAccept(AsyncHandlers.status(res, CREATED_201))
                .exceptionally(AsyncHandlers.error(req));
    }

    private static boolean isCompressed(ServerRequest request) {
        return request.headers().value(Http.Header.CONTENT_ENCODING).map(hdr -> "gzip".equals(hdr)).orElse(false);
    }
}
