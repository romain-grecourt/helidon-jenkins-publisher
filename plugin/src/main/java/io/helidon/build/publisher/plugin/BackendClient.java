package io.helidon.build.publisher.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.build.publisher.model.PipelineEvents;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Publisher client.
 */
final class BackendClient implements PipelineEvents.EventListener {

    private static final Logger LOGGER = Logger.getLogger(BackendClient.class.getName());
    private static final Map<String, BackendClient> CLIENTS = new HashMap<>();
    private static final int OUTPUT_THRESHOLD = 100 * 1024; // 100KIB
    private static final int EVENT_QUEUE_SIZE = 4096; // max number of events in the queue
    private static final int AGGREGATE_SIZE = 100; // max number of aggregated events

    private final BlockingQueue<PipelineEvents.Event>[] queues;
    private final ExecutorService executor;
    private final String serverUrl;
    private final int nThreads;
    private final ObjectMapper mapper;

    /**
     * Get or create the client for the given server URL.
     * @param serverUrl the publisher server URL
     * @return HelidonPublisherClient
     */
    static BackendClient getOrCreate(String serverUrl, int nThreads) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("server url is null or empty");
        }
        if (nThreads <= 0) {
            throw new IllegalArgumentException("invalid thread size: " + nThreads);
        }
        synchronized(CLIENTS) {
            BackendClient client = CLIENTS.get(serverUrl);
            if (client != null) {
                return client;
            }
        }
        BackendClient client = new BackendClient(serverUrl, nThreads);
        synchronized(CLIENTS) {
            CLIENTS.put(serverUrl, client);
            return client;
        }
    }

    /**
     * Create a new publisher client.
     * @param serverUrl publisher server URL
     * @param scmHead
     * @param scmHash 
     */
    private BackendClient(String serverUrl, int nThreads) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Creating client, serverUrl={0}, nThreads={1}", new Object[]{
                serverUrl,
                nThreads
            });
        }
        this.serverUrl = serverUrl;
        this.nThreads = nThreads;
        this.queues = new BlockingQueue[nThreads];
        this.executor = Executors.newFixedThreadPool(nThreads);
        this.mapper = new ObjectMapper();
    }

    @Override
    public void onEvent(PipelineEvents.Event event) {
        String runId = event.runId();
        int queueId = Math.floorMod(runId.hashCode(), nThreads);
        BlockingQueue<PipelineEvents.Event> queue = queues[queueId];
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(EVENT_QUEUE_SIZE);
            queues[queueId] = queue;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Creating client thread, serverUrl={0}, queueId={1}", new Object[]{
                    serverUrl,
                    queueId
                });
            }
            executor.submit(new ClientThread(serverUrl, queueId, queue, mapper));
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Adding event to queue, serverUrl={0}, queueId={1}", new Object[]{
                serverUrl,
                queueId
            });
        }
        if (!queue.offer(event)) {
            LOGGER.log(Level.WARNING, "Queue is full, dropping pipeline events, serverUrl={0}, queueId={1}, runId={2}",
                    new Object[]{
                        serverUrl,
                        queueId,
                        runId
                    });
            Iterator<PipelineEvents.Event> it = queue.iterator();
            while(it.hasNext()) {
                PipelineEvents.Event e = it.next();
                if (e.runId().equals(event.runId())) {
                    it.remove();
                }
            }
            queue.add(new PipelineEvents.Error(event.runId(), /* error code */ 1, "event queue is full"));
        }
    }

    /**
     * Publisher client thread is responsible for a set of jobs.
     * Work load for a job is processed by the same client thread in order to guarantee the ordering.
     */
    private static final class ClientThread implements Runnable {

        private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());
        private final BlockingQueue<PipelineEvents.Event> queue;
        private final String serverUrl;
        private final int queueId;
        private final ObjectMapper mapper;

        /**
         * Create a new client thread bound to the given queue.
         * @param serverUrl the serverUrl
         * @param queue the queue that this thread processes
         */
        ClientThread(String serverUrl, int queueId, BlockingQueue<PipelineEvents.Event> queue, ObjectMapper mapper) {
            Objects.requireNonNull(queue, "queue is null");
            this.queue = queue;
            this.queueId = queueId;
            this.serverUrl = serverUrl;
            this.mapper = mapper;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    PipelineEvents.Event event = queue.take();
                    switch (event.eventType()) {
                        case PIPELINE_CREATED:
                        case STEP_CREATED:
                        case STAGE_CREATED:
                        case PIPELINE_COMPLETED:
                        case STEP_COMPLETED:
                        case STAGE_COMPLETED:
                        case ERROR:
                            processEvent(event);
                            break;
                        case OUTPUT_DATA:
                            processOutputEvent((PipelineEvents.OutputData) event);
                            break;
                        default:
                            LOGGER.log(Level.WARNING, "Unknown event type: {0}", event.eventType());
                    }
                } catch (Throwable ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
            }
        }

        /**
         * Process an output event.
         *
         * @param outputEvent event
         */
        private void processEvent(PipelineEvents.Event event) throws IOException {
            // aggregate event for the same run in the next 100 events in the queue
            // or until an output for that run is found
            List<PipelineEvents.Event> events = new LinkedList<>();
            events.add(event);
            Iterator<PipelineEvents.Event> it = queue.iterator();
            PipelineEvents.Error error = null;
            boolean outputFound = false;
            for (int i = 0; !outputFound && error == null && it.hasNext() && i < AGGREGATE_SIZE; i++) {
                PipelineEvents.Event e = it.next();
                switch (e.eventType()) {
                    case PIPELINE_CREATED:
                    case STEP_CREATED:
                    case STAGE_CREATED:
                    case PIPELINE_COMPLETED:
                    case STEP_COMPLETED:
                    case STAGE_COMPLETED:
                        events.add(e);
                        it.remove();
                        break;
                    case ERROR:
                        if (e.runId().equals(event.runId())) {
                            error = (PipelineEvents.Error) e;
                        }
                        break;
                    case OUTPUT_DATA:
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "Unknown event type: {0}", event.eventType());
                }
            }
            if (error != null) {
                events = new LinkedList<>();
                events.add(error);
            }

            URL url = URI.create(serverUrl + "/events/").toURL();

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Sending events, queueId={0}, url={1}, events={2}", new Object[]{
                    queueId,
                    url,
                    events
                });
            }

            URLConnection con = url.openConnection();
            if (!(con instanceof HttpURLConnection)) {
                throw new IllegalStateException("Not an HttpURLConnection");
            }
            HttpURLConnection hcon = (HttpURLConnection) con;
            hcon.addRequestProperty("Content-Type", "application/json");
            hcon.setRequestMethod("PUT");
            hcon.setDoOutput(true);
            mapper.writeValue(hcon.getOutputStream(), new PipelineEvents(events));
            int code = hcon.getResponseCode();
            if (200 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code for event, url: {0}, code: {1}",
                        new Object[]{
                            url.toString(),
                            code
                        });
            }
        }

        /**
         * Process an output event.
         * @param outputEvent event
         */
        private void processOutputEvent(PipelineEvents.OutputData outputEvent) throws IOException {
            URL url = URI.create(serverUrl + "/output/" + outputEvent.runId()+ "/" + outputEvent.stepId()).toURL();

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Sending output data event, queueId={0}, url={1}, event={2}", new Object[]{
                    queueId,
                    url,
                    outputEvent
                });
            }

            URLConnection con = url.openConnection();
            if (!(con instanceof HttpURLConnection)) {
                throw new IllegalStateException("Not an HttpURLConnection");
            }
            HttpURLConnection hcon = (HttpURLConnection) con;
            hcon.setDoOutput(true);
            hcon.addRequestProperty("Content-Type", "text/plain");
            hcon.addRequestProperty("Content-Encoding", "gzip");
            try (GZIPOutputStream out = new  GZIPOutputStream(hcon.getOutputStream())) {
                byte[] data = outputEvent.data();
                out.write(data, 0, data.length);
                int len = data.length;
                Iterator<PipelineEvents.Event> it = queue.iterator();
                // aggregate output for the same step in the next 100 events in the queue
                // or until
                for (int i = 0; it.hasNext() && i < AGGREGATE_SIZE && len < OUTPUT_THRESHOLD; i++) {
                    PipelineEvents.Event e = it.next();
                    if (e.eventType() == PipelineEvents.EventType.OUTPUT
                            && ((PipelineEvents.OutputData)e).stepId() == outputEvent.stepId()) {
                        data = ((PipelineEvents.OutputData)e).data();
                        out.write(data, 0, data.length);
                        len += data.length;
                        it.remove();
                    }
                }
                out.flush();
            }
            int code = hcon.getResponseCode();
            if (200 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code for output data event, url={0}, code={1}, step: {1}",
                        new Object[]{
                            url.toString(),
                            code,
                            outputEvent
                        });
            }
        }
    }
}
