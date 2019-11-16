package io.helidon.build.publisher.frontend;

import io.helidon.build.publisher.storage.EventBus;
import io.helidon.build.publisher.storage.Storage;
import io.helidon.build.publisher.storage.StoragePaths;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * Service that implements the endpoint used by the UI.
 */
final class FrontendService implements Service {

    private final Storage storage;
    private final EventBus eventBus;

    FrontendService(EventBus eventBus, Storage storage) {
        this.storage = storage;
        this.eventBus = eventBus;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{pipelineId}", this::getPipeline)
             .get("/{pipelineId}/output/{stepId}", this::getOutput);
    }

    /**
     * Get job.
     * @param req server request
     * @param res server response
     */
    private void getPipeline(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        String path = StoragePaths.pipelineDescriptor(pipelineId);
        if (!storage.exists(path)) {
            res.status(404).send();
            return;
        }
        Storage.StoragePublisher pub = storage.get(path);
        res.headers().contentType(MediaType.APPLICATION_JSON);
        res.headers().contentLength(pub.length());
        res.send(pub);
    }

    /**
     * Get output.
     * @param req server request
     * @param res server response
     */
    private void getOutput(ServerRequest req, ServerResponse res) {
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
        int nlines = req.queryParams().first("nlines").map(Integer::parseInt).orElse(0);
        int position = req.queryParams().first("position").map(Integer::parseInt).orElse(0);
        res.headers().contentType(MediaType.TEXT_PLAIN);
        Storage.StoragePublisher pub = storage.get(StoragePaths.stepOutput(pipelineId, stepId), position, nlines);
        res.headers().put(Http.Header.CONTENT_LENGTH, String.valueOf(pub.length()));
        res.send(pub);
    }
}
