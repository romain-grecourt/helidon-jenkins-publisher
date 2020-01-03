package io.helidon.build.publisher.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.logging.Level;

import io.helidon.common.http.Http;
import io.helidon.webserver.BadRequestException;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import io.helidon.build.publisher.model.DescriptorFileManager;
import io.helidon.build.publisher.model.EventProcessor;
import io.helidon.build.publisher.model.events.PipelineEvents;

import static io.helidon.common.CollectionsHelper.listOf;
import static io.helidon.common.http.Http.Status.CREATED_201;
import static io.helidon.common.http.Http.Status.OK_200;

/**
 * This service implements the endpoints used by the Jenkins plugin.
 */
final class BackendService implements Service {

    private static final Logger LOGGER = Logger.getLogger(BackendService.class.getName());
    private final Path storagePath;
    private final EventProcessor eventProcessor;
    private final FileAppender appender;

    /**
     * Create a new instance.
     * @param path storage path
     * @param appenderThreads number of threads used for appending data
     */
    BackendService(Path storagePath, int appenderThreads) {
        this.storagePath = storagePath;
        if (!Files.exists(storagePath)) {
            try {
                Files.createDirectories(storagePath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        this.appender = new FileAppender(appenderThreads);
        this.eventProcessor = new EventProcessor(new DescriptorFileManager(storagePath), listOf(new GitHubInfoAugmenter()));
        LOGGER.log(Level.INFO, "Creating backend service, storagePath={0}, appender nThreads={1}", new Object[]{
            storagePath,
            appenderThreads
        });
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/ping", this::ping)
             .put("/events", this::processEvents)
             .put("/output/{pipelineId}/{stepId}", this::appendOutput)
             .post("/files/{pipelineId}/{filepath:.+}", this::uploadFile);
    }

    private void ping(ServerRequest req, ServerResponse res) {
        res.status(OK_200).send();
    }

    private void processEvents(ServerRequest req, ServerResponse res) {
        req.content().as(PipelineEvents.class).thenAccept(pipelineEvents -> {
            eventProcessor.process(pipelineEvents.events());
            res.status(OK_200).send();
        }).exceptionally(AsyncHandlers.error(req));
    }

    private void appendOutput(ServerRequest req, ServerResponse res) {
        Path pipelinePath = storagePath.resolve(req.path().param("pipelineId"));
        Path path = pipelinePath.resolve("step-" + req.path().param("stepId") + ".log");
        if (!path.getParent().equals(pipelinePath)) {
            throw new BadRequestException("Invalid stepId");
        }
        appender.append(req.content(), path, isCompressed(req))
                .thenAccept(AsyncHandlers.status(res, OK_200))
                .exceptionally(AsyncHandlers.error(req));
    }

    private void uploadFile(ServerRequest req, ServerResponse res) {
        Path pipelinePath = storagePath.resolve(req.path().param("pipelineId"));
        Path path = pipelinePath.resolve(req.path().param("filepath"));
        if (!path.startsWith(pipelinePath)) {
            throw new BadRequestException("Invalid path");
        }
        appender.append(req.content(), path, isCompressed(req))
                .thenAccept(AsyncHandlers.status(res, CREATED_201))
                .exceptionally(AsyncHandlers.error(req));
    }

    private static boolean isCompressed(ServerRequest request) {
        return request.headers().value(Http.Header.CONTENT_ENCODING).map(hdr -> "gzip".equals(hdr)).orElse(false);
    }
}
