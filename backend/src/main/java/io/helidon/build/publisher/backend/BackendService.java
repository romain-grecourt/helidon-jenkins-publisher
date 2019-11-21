package io.helidon.build.publisher.backend;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import io.helidon.build.publisher.model.PipelineEvents;
import io.helidon.build.publisher.model.PipelineRun;
import io.helidon.common.http.Http;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This service implements the endpoints used by the Jenkins plugin.
 */
final class BackendService implements Service {

    private static final Logger LOGGER = Logger.getLogger(BackendService.class.getName());
    private static final int APPENDER_QUEUE_SIZE = 1024; // max number of append action in the queue
    private static final String PIPELINE_DESC = "/pipeline.json";

    private final Path storagePath;
    private final ExecutorService appenderExecutors;
    private final BlockingQueue<AppendAction>[] appendActionQueues;
    private final ObjectMapper mapper;

    /**
     * Create a new instance.
     * @param path storage path
     * @param appenderThreads number of threads used for appending data
     */
    BackendService(String path, int appenderThreads) {
        Path dirPath = FileSystems.getDefault().getPath(path);
        if (!Files.exists(dirPath)) {
            throw new IllegalArgumentException("local storage does not exist: " + dirPath);
        }
        this.storagePath = dirPath;
        this.appenderExecutors = Executors.newFixedThreadPool(appenderThreads);
        this.appendActionQueues = new BlockingQueue[appenderThreads];
        this.mapper = new ObjectMapper();
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.put("/events", this::processEvents)
             .put("/output/{pipelineId}/{stepId}", this::appendOutput);
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
                        Path filePath = storagePath.resolve(event.runId() + PIPELINE_DESC);
                        if (!Files.exists(filePath)) {
                            if (event.eventType() == PipelineEvents.EventType.PIPELINE_CREATED) {
                                pipelineRun = new PipelineRun((PipelineEvents.PipelineCreated) event);
                                Files.createDirectories(filePath.getParent());
                                continue;
                            } else {
                                res.status(400).send();
                                return;
                            }
                        } else {
                            pipelineRun = mapper.readValue(Files.newInputStream(filePath), PipelineRun.class);
                        }
                    }
                    events.add(event);
                }
                if (pipelineRun != null) {
                    pipelineRun.pipeline().applyEvents(events);
                    Path filePath = storagePath.resolve(pipelineRun.id() + PIPELINE_DESC);
                    mapper.writeValue(Files.newOutputStream(filePath), pipelineRun);
                }
                res.status(200).send();
            } catch (Throwable ex) {
                req.next(ex);
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
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
        append(req.content(), pipelineId, stepId);
        res.status(Http.Status.OK_200).send();
    }

    private boolean append(Publisher<DataChunk> payload, String pipelineId, int stepId) {
        AppendAction action = new AppendAction(payload, pipelineId, stepId);
        int queueId = action.hashCode() % appendActionQueues.length;
        BlockingQueue<AppendAction> queue = appendActionQueues[queueId];
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(APPENDER_QUEUE_SIZE);
            appendActionQueues[queueId] = queue;
            LOGGER.log(Level.FINE, () -> "creating append thread for queueId: " + queueId);
            appenderExecutors.submit(new AppendThread(queue));
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Adding append action ({0}) to queue: #{1}", new Object[] {action.path, queueId});
        }
        if (!queue.offer(action)) {
            LOGGER.log(Level.WARNING, "Queue #{0} is full, dropping all append actions ({1})",
                    new Object[]{ queueId, action.path });
            Iterator<AppendAction> it = queue.iterator();
            while(it.hasNext()) {
                AppendAction a = it.next();
                if (a.path.equals(action.path)) {
                    it.remove();
                }
            }
            return false;
        }
        return true;
    }

    private final class AppendThread implements Runnable {

        private final BlockingQueue<AppendAction> queue;

        AppendThread(BlockingQueue<AppendAction> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    queue.take().execute();
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

    private final class AppendAction {

        private final String path;
        private final Publisher<DataChunk> payload;
        private final String pipelineId;
        private final int stepId;

        AppendAction(Publisher<DataChunk> payload, String pipelineId, int stepId) {
            this.payload = payload;
            this.pipelineId = pipelineId;
            this.stepId = stepId;
            this.path = pipelineId + "/step-" + stepId + ".log";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.path);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AppendAction other = (AppendAction) obj;
            if (!Objects.equals(this.path, other.path)) {
                return false;
            }
            return Objects.equals(this.payload, other.payload);
        }

        void execute() {
            Path filePath = storagePath.resolve(path);
            try {
                OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.APPEND);
                payload.subscribe(new Appender(os, this::onComplete));
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "AppendAction.execute() ; IO error", ex);
                throw new IllegalStateException(ex);
            }
        }

        void onComplete() {
            // TODO publish event
        }
    }

    private static final class Appender implements Subscriber<DataChunk> {

        private Subscription subscription;
        private final OutputStream os;
        private final Runnable runnable;

        Appender(OutputStream os, Runnable runnable) {
            this.os = os;
            this.runnable = runnable;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(1);
            this.subscription = subscription;
        }

        @Override
        public void onNext(DataChunk item) {
            try {
                os.write(item.bytes());
                item.release();
                subscription.request(1);
            } catch (IOException ex) {
                subscription.cancel();
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        @Override
        public void onComplete() {
            try {
                os.flush();
                os.close();
                runnable.run();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
