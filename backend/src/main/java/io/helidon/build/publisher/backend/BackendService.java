package io.helidon.build.publisher.backend;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.LinkedList;

import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.helidon.build.publisher.model.PipelineEvents;
import io.helidon.build.publisher.model.PipelineRun;
import io.helidon.build.publisher.storage.EventBus;
import io.helidon.build.publisher.storage.Storage;
import io.helidon.build.publisher.storage.StoragePaths;

/**
 * This service implements the endpoints used by the Jenkins plugin.
 */
final class BackendService implements Service {

    private final Storage storage;
    private final EventBus eventBus;
    private final ObjectMapper objectMapper;

    /**
     * Create a new instance.
     * @param EventBus event bus
     * @parma storage storage
     */
    BackendService(EventBus eventBus, Storage storage) {
        this.storage = storage;
        this.eventBus = eventBus;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.put("/events", this::processEvents)
             .put("/output/{pipelineId}/{stepId}", this::appendOutput);
    }

    /**
     * Create a new job.
     * @param req server request
     * @param res server response
     */
    private void processEvents(ServerRequest req, ServerResponse res) {
        req.content().as(PipelineEvents.class).thenAccept(pevents -> {
            PipelineRun pipelineRun = null;
            List<PipelineEvents.Event> events = new LinkedList<>();
            for (PipelineEvents.Event event : pevents.events()) {
                if (pipelineRun == null || !pipelineRun.id().equals(event.runId())) {
                    if (pipelineRun != null) {
                        pipelineRun.pipeline().applyEvents(events);
                        eventBus.publish(events);
                        events = new LinkedList<>();
                    }
                    String path = StoragePaths.pipelineDescriptor(event.runId());
                    if (!storage.exists(path)) {
                        if (event.eventType() == PipelineEvents.EventType.PIPELINE_CREATED) {
                            pipelineRun = new PipelineRun((PipelineEvents.PipelineCreated) event);
                        } else {
                            res.status(400).send();
                            return;
                        }
                    } else {
                        InputStream is = storage.inputStream(path);
                        try {
                            pipelineRun = objectMapper.readValue(is, PipelineRun.class);
                        } catch (IOException ex) {
                            req.next(ex);
                            return;
                        }
                    }
                    continue;
                }
                events.add(event);
            }
            if (pipelineRun != null) {
                pipelineRun.pipeline().applyEvents(events);
                eventBus.publish(events);
            }
        });
    }

    /**
     * Append output to a step.
     *
     * @param req server request
     * @param res server response
     */
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
            return;
        }
        boolean queued = storage.append(StoragePaths.stepOutput(pipelineId, stepId), req.content(), () -> {
            eventBus.publish(new PipelineEvents.Output(pipelineId, stepId));
        });
        if (!queued) {
            eventBus.publish(new PipelineEvents.Error(pipelineId, 2, "Error while appending output for step: " + stepId));
        }
        res.status(Http.Status.OK_200).send();
    }
}
