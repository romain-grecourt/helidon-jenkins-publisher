package io.helidon.jenkins.publisher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;

/**
 * Publisher client.
 */
final class HelidonPublisherClient implements FlowEvent.Listener {

    private static final Logger LOGGER = Logger.getLogger(HelidonPublisherClient.class.getName());
    private static final Map<String, HelidonPublisherClient> CLIENTS = new HashMap<>();
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(new HashMap<>());
    private static final int OUTPUT_THRESHOLD = 100 * 1024; // 100KIB
    private static final int EVENT_QUEUE_SIZE = 4096; // max number of events in the queue
    private static final int AGGREGATE_SIZE = 100; // max number of aggregated events

    private final BlockingQueue<FlowEvent>[] queues;
    private final ExecutorService executor;
    private final String serverUrl;
    private final int nThreads;

    /**
     * Get or create the client for the given server URL.
     * @param serverUrl the publisher server URL
     * @return HelidonPublisherClient
     */
    static HelidonPublisherClient getOrCreate(String serverUrl, int nThreads) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("server url is null or empty");
        }
        if (nThreads <= 0) {
            throw new IllegalArgumentException("invalid thread size: " + nThreads);
        }
        synchronized(CLIENTS) {
            HelidonPublisherClient client = CLIENTS.get(serverUrl);
            if (client != null) {
                return client;
            }
        }
        HelidonPublisherClient client = new HelidonPublisherClient(serverUrl, nThreads);
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
    private HelidonPublisherClient(String serverUrl, int nThreads) {
        this.serverUrl = serverUrl;
        this.nThreads = nThreads;
        this.queues = new BlockingQueue[nThreads];
        this.executor = Executors.newFixedThreadPool(nThreads);
    }

    @Override
    public void onEvent(FlowEvent event) {
        int queueId = event.flowRun().hashCode() % nThreads;
        BlockingQueue<FlowEvent> queue = queues[queueId];
        if (queues[queueId] == null) {
            queue = new LinkedBlockingQueue<>(EVENT_QUEUE_SIZE);
            queues[queueId] = queue;
            LOGGER.log(Level.FINE, () -> "submitting event: " + event.toString());
            executor.submit(new ClientThread(serverUrl, queue));
        }
        if (!queue.offer(event)) {
            FlowRun flowRun = event.flowRun();
            LOGGER.log(Level.WARNING, "event queue #{0} is full, dropping all events for run: {1}",
                    new Object[]{
                        queueId,
                        flowRun.desc()
                    });
            Iterator<FlowEvent> it = queue.iterator();
            while(it.hasNext()) {
                FlowEvent e = it.next();
                if (e.flowRun().equals(flowRun)) {
                    it.remove();
                }
            }
            queue.add(new FlowEvent.ErrorEvent(flowRun, /* error code */ 1, "event queue is full"));
        }
    }

    /**
     * Publisher client thread is responsible for a set of jobs.
     * Work load for a job is processed by the same client thread in order to guarantee the ordering.
     */
    private static final class ClientThread implements Runnable {

        private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());
        private final BlockingQueue<FlowEvent> queue;
        private final String serverUrl;

        /**
         * Create a new client thread bound to the given queue.
         * @param serverUrl the serverUrl
         * @param queue the queue that this thread processes
         */
        ClientThread(String serverUrl, BlockingQueue<FlowEvent> queue) {
            Objects.requireNonNull(queue, "queue is null");
            this.queue = queue;
            this.serverUrl = serverUrl;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    FlowEvent event = queue.take();
                    try {
                        switch (event.type()) {
                            case CREATED:
                            case STEP_CREATED:
                            case STAGE_CREATED:
                            case COMPLETED:
                            case STEP_COMPLETED:
                            case STAGE_COMPLETED:
                            case ERROR:
                                processEvent(event);
                            case OUTPUT:
                                processOutputEvent(event.asOutput());
                                break;
                            default:
                                LOGGER.log(Level.WARNING, "Unknown event type: {0}", event.type());
                        }
                    } catch (IOException ex) {
                        
                    }
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    break;
                }
            }
        }

        /**
         * Process an output event.
         *
         * @param outputEvent event
         */
        private void processEvent(FlowEvent event) throws IOException {
            FlowEvent.Type eventType = event.type();
            boolean created = eventType == FlowEvent.Type.CREATED;
            FlowRun flowRun = event.flowRun();
            URL url = URI.create(serverUrl + "/jobs/" + flowRun.id()).toURL();
            URLConnection con = url.openConnection();
            if (!(con instanceof HttpURLConnection)) {
                throw new IllegalStateException("Not an HttpURLConnection");
            }
            HttpURLConnection hcon = (HttpURLConnection) con;
            hcon.setDoOutput(true);
            hcon.addRequestProperty("Content-Type", "application/json");
            if (created) {
                hcon.setRequestMethod("POST");
            } else {
                hcon.setRequestMethod("PUT");
            }

            JsonObjectBuilder payload = JSON_BUILDER_FACTORY.createObjectBuilder();
            if (created) {
                payload.add("name", flowRun.jobName());
                payload.add("scmHead", flowRun.scmHead());
                payload.add("scmHash", flowRun.scmHash());
                payload.add("timestamp", flowRun.timestamp());
            }

            // aggregate event for the same run in the next 100 events in the queue
            // or until an output for that run is found
            JsonArrayBuilder events = JSON_BUILDER_FACTORY.createArrayBuilder();
            Iterator<FlowEvent> it = queue.iterator();
            FlowEvent.ErrorEvent error = null;
            boolean outputFound = false;
            for (int i = 0; !outputFound && error == null && it.hasNext() && i < AGGREGATE_SIZE; i++) {
                FlowEvent e = it.next();
                switch (e.type()) {
                    case CREATED:
                    case STEP_CREATED:
                    case STAGE_CREATED:
                    case COMPLETED:
                    case STEP_COMPLETED:
                    case STAGE_COMPLETED:
                        events.add(e.toJson());
                        it.remove();
                        break;
                    case ERROR:
                        if (e.flowRun().equals(flowRun)) {
                            error = e.asError();
                        }
                        break;
                    case OUTPUT:
                        if (e.type() == FlowEvent.Type.OUTPUT && e.flowRun().equals(flowRun)) {
                            outputFound = true;
                        }
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "Unknown event type: {0}", event.type());
                }
            }
            if (error != null) {
                payload.add("events",
                        JSON_BUILDER_FACTORY.createArrayBuilder()
                                .add(error.toJson()));
            } else {
                payload.add("events", events);
            }
            Json.createWriter(hcon.getOutputStream()).writeObject(payload.build());

            hcon.connect();
            int code = hcon.getResponseCode();
            if ((created && 201 != code) || 200 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code for job events, url:{0}, code: {1}",
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
        private void processOutputEvent(FlowEvent.OutputEvent outputEvent) throws IOException {
            URL url = URI.create(serverUrl + "/jobs" + outputEvent.flowRun().id()+ "/steps/" + outputEvent.step().id()).toURL();
            URLConnection con = url.openConnection();
            if (!(con instanceof HttpURLConnection)) {
                throw new IllegalStateException("Not an HttpURLConnection");
            }
            HttpURLConnection hcon = (HttpURLConnection) con;
            hcon.setDoOutput(true);
            hcon.addRequestProperty("Content-Type", "text/plain");
            hcon.connect();
            try (DeflaterOutputStream out = new DeflaterOutputStream(hcon.getOutputStream())) {
                byte[] data = outputEvent.data();
                out.write(data, 0, data.length);
                int len = data.length;
                Iterator<FlowEvent> it = queue.iterator();
                // aggregate output for the same step in the next 100 events in the queue
                // or until
                for (int i = 0; it.hasNext() && i < AGGREGATE_SIZE && len < OUTPUT_THRESHOLD; i++) {
                    FlowEvent e = it.next();
                    if (e.type() == FlowEvent.Type.OUTPUT && e.asOutput().step().equals(outputEvent.step())) {
                        data = e.asOutput().data();
                        out.write(data, 0, data.length);
                        len += data.length;
                        it.remove();
                    }
                }
                out.flush();
            }
            int code = hcon.getResponseCode();
            if (200 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code for step output, url:{0}, code: {1}, step: {1}",
                        new Object[]{
                            url.toString(),
                            code,
                            outputEvent.step()
                        });
            }
        }
    }
}
