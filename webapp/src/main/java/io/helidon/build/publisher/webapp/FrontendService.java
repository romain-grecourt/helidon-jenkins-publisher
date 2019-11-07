package io.helidon.build.publisher.webapp;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * Service that implements the endpoint used by the UI.
 */
final class FrontendService implements Service {

    private final Storage storage;
    private final JobEventListener jobEventListener;
    private final OutputEventListener outputEventListener;

    FrontendService(MessageBus messageBus, Storage storage) {
        this.storage = storage;
        this.jobEventListener = new JobEventListener();
        this.outputEventListener = new OutputEventListener();
        messageBus.subscribeJobEvent(jobEventListener);
        messageBus.subscribeStepOutput(outputEventListener);
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/{jobId}", this::getJob)
                .get("/{jobId}/step/{stepId}", this::getOutput);
    }

    /**
     * Get job.
     * @param req server request
     * @param res server response
     */
    private void getJob(ServerRequest req, ServerResponse res) {
        String jobId = req.path().param("jobId");
        if (jobId == null || jobId.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }
        res.headers().contentType(MediaType.APPLICATION_JSON);
        res.send(storage.get("jobs/" + jobId + "/job.json"));
    }

    /**
     * Get output.
     * @param req server request
     * @param res server response
     */
    private void getOutput(ServerRequest req, ServerResponse res) {
        String jobId = req.path().param("jobId");
        String stepId = req.path().param("stepId");
        if (jobId == null || jobId.isEmpty() || stepId == null || stepId.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }
        // TODO set query param for maxlines
        // use storage.tail
        String storagePath = "jobs/" + jobId + "/step-" + stepId + ".txt";
        Publisher<DataChunk> pub = storage.get(storagePath);
        // TODO set last lineno as header
        res.headers().contentType(MediaType.TEXT_PLAIN);
        res.send(pub);
    }

    private final class OutputEventListener implements MessageListener<JobEvent.OutputEvent> {

        @Override
        public void onMessage(Message<JobEvent.OutputEvent> message) {
            JobEvent.OutputEvent event = message.getMessageObject();
        }
    }

    private final class JobEventListener implements MessageListener<JobEvent> {

        @Override
        public void onMessage(Message<JobEvent> message) {
            JobEvent event = message.getMessageObject();
        }
    }
}
