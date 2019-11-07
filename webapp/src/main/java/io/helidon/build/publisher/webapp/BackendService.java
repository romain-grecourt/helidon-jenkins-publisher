package io.helidon.build.publisher.webapp;

import static io.helidon.common.CollectionsHelper.mapOf;
import io.helidon.common.http.Http;
import io.helidon.webserver.BadRequestException;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

/**
 * This service implements the endpoints used by the Jenkins plugin.
 */
final class BackendService implements Service {

    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(mapOf());
    private static final JsonWriterFactory JSON_WRITER_FACTORY = Json.createWriterFactory(mapOf());
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(mapOf());

    private final MessageBus messageBus;
    private final Storage storage;

    /**
     * Create a new instance.
     * @param messageBus message bus
     * @parma storage storage
     */
    BackendService(MessageBus messageBus, Storage storage) {
        this.messageBus = messageBus;
        this.storage = storage;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.post("/{jobId}", this::createJob)
             .put("/{jobId}", this::updateJob)
             .put("/{jobId}/step/{stepId}", this::appendOutput);
    }

    /**
     * Create a new job.
     * @param req server request
     * @param res server response
     */
    private void createJob(ServerRequest req, ServerResponse res) {
        String jobId = req.path().param("jobId");
        if (jobId == null || jobId.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }
        req.content().as(JsonObject.class).thenAccept(json -> {
            publishJobEvents(jobId, json);
            // TODO create the JSON file in storage
            res.status(Http.Status.CREATED_201).send();
        });
    }

    /**
     * Update an existing job.
     * @param req server request
     * @param res server response
     */
    private void updateJob(ServerRequest req, ServerResponse res) {
        String jobId = req.path().param("jobId");
        if (jobId == null || jobId.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }
        req.content().as(JsonObject.class).thenAccept(json -> {
            publishJobEvents(jobId, json);
            updateJobCache(jobId, json);
            res.status(Http.Status.OK_200).send();
        });
    }

    /**
     * Update a job cache with the given JSON payload.
     * @param payload JSON payload
     * @param jobId job id
     */
    private void updateJobCache(String jobId, JsonObject payload) {
        String storagePath = "jobs/" + jobId + "/job.json";
        // read the job in memory
        JsonReader reader = JSON_READER_FACTORY.createReader(storage.inputStream(storagePath));
        JsonObjectBuilder job = JSON_BUILDER_FACTORY.createObjectBuilder(reader.readObject());

        // TODO update the job
        // go through events and build the graph

        // write the job
        try (JsonWriter writer = JSON_WRITER_FACTORY.createWriter(storage.outputStream(storagePath))) {
            writer.writeObject(job.build());
        }
    }

    /**
     * Append output to a step.
     * @param req server request
     * @param res server response
     */
    private void appendOutput(ServerRequest req, ServerResponse res) {
        String jobId = req.path().param("jobId");
        String stepId = req.path().param("stepId");
        if (jobId == null || jobId.isEmpty() || stepId == null || stepId.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }

        String storagePath = "jobs/" + jobId + "/step-" + stepId + ".txt";
        storage.append(storagePath, req.content(), () -> {
            messageBus.publishJobEvent(new JobEvent.OutputEvent(jobId, stepId, /* data */ null));
        });
        res.status(Http.Status.OK_200).send();
    }

    /**
     * Publish the job event for the given JSON payload.
     * @param jobId the jobId from the path parameter
     * @param payload the JSON payload
     */
    private void publishJobEvents(String jobId, JsonObject payload) {
        try {
            JsonArray events = payload.getJsonArray("events");
            if (events != null) {
                for (JsonValue eventJson : events) {
                    JobEvent event = JobEvent.fromJson(jobId, eventJson.asJsonObject());
                    messageBus.publishJobEvent(event);
                }
            }
        } catch (ClassCastException ex) {
            throw new BadRequestException("event is not an object");
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}
