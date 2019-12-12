package io.helidon.build.publisher.backend;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.publisher.model.PipelineEvents;
import io.helidon.build.publisher.model.PipelineRun;
import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This service implements the endpoints used by the Jenkins plugin.
 */
final class BackendService implements Service {

    private static final Logger LOGGER = Logger.getLogger(BackendService.class.getName());
    private static final String PIPELINE_DESC = "/pipeline.json";

    private final Path storagePath;
    private final ObjectMapper mapper;
    private final FileAppender appender;

    /**
     * Create a new instance.
     * @param path storage path
     * @param appenderThreads number of threads used for appending data
     */
    BackendService(String path, int appenderThreads) {
        Path dirPath = FileSystems.getDefault().getPath(path);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectory(dirPath);
            } catch (IOException ex) {
                throw new IllegalStateException("Error initializing storage directory", ex);
            }
        }
        this.storagePath = dirPath;
        this.appender = new FileAppender(storagePath, appenderThreads);
        this.mapper = new ObjectMapper();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.put("/events", this::processEvents)
             .put("/output/{pipelineId}/{stepId}", this::appendOutput)
             .post("/files/{pipelineId}/{filepath:.+}", this::uploadFile);
    }

    private void processEvents(ServerRequest req, ServerResponse res) {
        req.content().as(PipelineEvents.class).thenAccept(pevents -> {
            try {
                PipelineRun pipelineRun = null;
                List<PipelineEvents.Event> events = new LinkedList<>();
                for (PipelineEvents.Event event : pevents.events()) {
                    if (pipelineRun == null || !pipelineRun.id().equals(event.runId())) {
                        if (pipelineRun != null) {
                            Path filePath = storagePath.resolve(pipelineRun.id() + PIPELINE_DESC);
                            pipelineRun.pipeline().applyEvents(events);
                            mapper.writeValue(Files.newOutputStream(filePath), pipelineRun);
                            events = new LinkedList<>();
                        }
                        String path = event.runId() + PIPELINE_DESC;
                        Path filePath = storagePath.resolve(path);
                        if (!Files.exists(filePath)) {
                            if (event.eventType() == PipelineEvents.EventType.PIPELINE_CREATED) {
                                pipelineRun = new PipelineRun((PipelineEvents.PipelineCreated) event);
                                Files.createDirectories(filePath.getParent());
                                continue;
                            } else {
                                res.status(400).send();
                                LOGGER.log(Level.WARNING, "Pipeline descriptor not found: {0}", path);
                                return;
                            }
                        } else {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.log(Level.FINEST, "Reading pipeline descriptor: {0}", path);
                            }
                            pipelineRun = mapper.readValue(Files.newInputStream(filePath), PipelineRun.class);
                        }
                    }
                    events.add(event);
                }
                if (pipelineRun != null) {
                    pipelineRun.pipeline().applyEvents(events);
                    String path = pipelineRun.id() + PIPELINE_DESC;
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Writing pipeline descriptor: {0}", path);
                    }
                    Path filePath = storagePath.resolve(pipelineRun.id() + PIPELINE_DESC);
                    mapper.writeValue(Files.newOutputStream(filePath), pipelineRun);
                }
                res.status(200).send();
            } catch (IllegalStateException ex) {
                res.status(400).send();
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            } catch (IOException ex) {
                req.next(ex);
            }
        }).exceptionally((ex) -> {
            req.next(ex);
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return null;
        });
    }

    private void appendOutput(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        String stepIdParam = req.path().param("stepId");
        if (pipelineId == null || pipelineId.isEmpty() || stepIdParam == null || stepIdParam.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }
        int stepId;
        try {
            stepId = Integer.valueOf(stepIdParam);
        } catch( NumberFormatException ex) {
            res.status(400).send();
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return;
        }
        boolean compressed = req.headers().value(Http.Header.CONTENT_ENCODING).map(hdr -> "gzip".equals(hdr)).orElse(false);
        appender.append(req.content(), pipelineId + "/step-" + stepId + ".log", compressed).thenAccept((v) -> {
            res.status(Http.Status.OK_200).send();
        }).exceptionally((ex) -> {
            req.next(ex);
            return null;
        });
    }

    private void uploadFile(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        String filepath = req.path().param("filepath");
        if (pipelineId == null || pipelineId.isEmpty() || filepath == null || filepath.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }
        boolean compressed = req.headers().value(Http.Header.CONTENT_ENCODING).map(hdr -> "gzip".equals(hdr)).orElse(false);
        appender.append(req.content(), pipelineId + "/" + filepath, compressed).thenAccept((v) -> {
            res.status(Http.Status.CREATED_201).send();
        }).exceptionally((ex) -> {
            req.next(ex);
            return null;
        });
    }
}
