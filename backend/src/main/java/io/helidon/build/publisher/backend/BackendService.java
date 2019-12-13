package io.helidon.build.publisher.backend;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.http.Http;
import io.helidon.webserver.BadRequestException;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import io.helidon.build.publisher.model.PipelineDescriptorFileManager;
import io.helidon.build.publisher.model.PipelineEventProcessor;
import io.helidon.build.publisher.model.events.PipelineEvents;

/**
 * This service implements the endpoints used by the Jenkins plugin.
 */
final class BackendService implements Service {

    private static final Logger LOGGER = Logger.getLogger(BackendService.class.getName());

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
        }).exceptionally(new ErrorHandler(req));
    }

    private void appendOutput(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        int stepId = Integer.valueOf(req.path().param("stepId"));
        appender.append(req.content(), pipelineId + "/step-" + stepId + ".log", isCompressed(req))
                .thenAccept(new AsyncResponseHandler(res))
                .exceptionally(new ErrorHandler(req));
    }

    private void uploadFile(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        String filepath = req.path().param("filepath");
        appender.append(req.content(), pipelineId + "/" + filepath, isCompressed(req))
                .thenAccept(new AsyncResponseHandler(res))
                .exceptionally(new ErrorHandler(req));
    }

    private static boolean isCompressed(ServerRequest request) {
        return request.headers().value(Http.Header.CONTENT_ENCODING).map(hdr -> "gzip".equals(hdr)).orElse(false);
    }

    private static final class AsyncResponseHandler implements Consumer<Void> {

        private final ServerResponse response;

        AsyncResponseHandler(ServerResponse response) {
            this.response = response;
        }

        @Override
        public void accept(Void v) {
            response.status(Http.Status.OK_200).send();
        }
    }

    private static final class ErrorHandler implements Function<Throwable, Void> {

        private final ServerRequest request;

        ErrorHandler(ServerRequest request) {
            this.request = request;
        }

        @Override
        public Void apply(Throwable ex) {
            if (ex instanceof IllegalStateException
                    || ex instanceof IllegalArgumentException) {
                request.next(new BadRequestException(ex.getMessage(), ex));
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            } else {
                request.next(ex);
            }
            return null;
        }
    }
}
