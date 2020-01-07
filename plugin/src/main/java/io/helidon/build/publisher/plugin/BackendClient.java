package io.helidon.build.publisher.plugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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

import io.helidon.build.publisher.model.JacksonSupport;
import io.helidon.build.publisher.model.events.ArtifactDataEvent;
import io.helidon.build.publisher.model.events.PipelineErrorEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineEvents;
import io.helidon.build.publisher.model.events.PipelineEventListener;
import io.helidon.build.publisher.model.events.PipelineEventType;
import io.helidon.build.publisher.model.events.StepOutputDataEvent;
import io.helidon.build.publisher.model.events.TestSuiteResultEvent;
import io.helidon.build.publisher.plugin.config.HttpSignatureHelper;

/**
 * Publisher client.
 */
final class BackendClient implements PipelineEventListener {

    private static final Logger LOGGER = Logger.getLogger(BackendClient.class.getName());
    private static final Map<String, BackendClient> CLIENTS = new HashMap<>();
    private static final int OUTPUT_THRESHOLD = 100 * 1024; // 100KIB
    private static final int EVENT_QUEUE_SIZE = 4096; // max number of events in the queue
    private static final int AGGREGATE_SIZE = 100; // max number of aggregated events
    private static final int CONNECT_TIMEOUT = 30 * 1000; // 30s
    private static final int READ_TIMEOUT = 60 * 2 * 1000; // 2min

    private final BlockingQueue<PipelineEvent>[] queues;
    private final ExecutorService executor;
    private final URI serverUri;
    private final int nThreads;
    private final String signatureHeader;

    /**
     * Get or create the client for the given server URL.
     * @param serverUrl the publisher server URL
     * @param key the private key used to authenticate, may be {@code null}
     * @return HelidonPublisherClient
     */
    static BackendClient getOrCreate(String serverUrl, int nThreads, String key) {
        if (serverUrl == null || serverUrl.isEmpty()) {
            throw new IllegalArgumentException("server url is null or empty");
        }
        if (nThreads <= 0) {
            throw new IllegalArgumentException("invalid thread size: " + nThreads);
        }
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        URI uri = URI.create(serverUrl);
        synchronized(CLIENTS) {
            BackendClient client = CLIENTS.get(serverUrl);
            if (client != null) {
                return client;
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Creating client, serverUrl={0}, nThreads={1}", new Object[]{
                serverUrl,
                nThreads
            });
        }
        BackendClient client = new BackendClient(uri, nThreads, key);
        synchronized(CLIENTS) {
            CLIENTS.put(serverUrl, client);
            return client;
        }
    }

    /**
     * Create a new publisher client.
     * @param serverUri publisher server URI
     * @param nThreads number of threads
     * @param keyPath path to the private key path
     */
    private BackendClient(URI serverUri, int nThreads, String key) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Creating client, serverUri={0}, nThreads={1}", new Object[]{
                serverUri,
                nThreads
            });
        }
        this.serverUri = serverUri;
        String signature = HttpSignatureHelper.sign("Host: " + serverUri.getAuthority()+ "\n", key);
        this.signatureHeader = HttpSignatureHelper.signatureHeader(signature);
        this.nThreads = nThreads;
        this.queues = new BlockingQueue[nThreads];
        this.executor = Executors.newFixedThreadPool(nThreads);
    }

    @Override
    public void onEvent(PipelineEvent event) {
        String pipelineId = event.pipelineId();
        int queueId = Math.floorMod(pipelineId.hashCode(), nThreads);
        BlockingQueue<PipelineEvent> queue = queues[queueId];
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(EVENT_QUEUE_SIZE);
            queues[queueId] = queue;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Creating client thread, serverUri={0}, queueId={1}", new Object[]{
                    serverUri,
                    queueId
                });
            }
            executor.submit(new ClientThread(serverUri, signatureHeader, queueId, queue));
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Adding event to queue, serverUri={0}, queueId={1}, queueSize={2}, event={3}",
                    new Object[]{
                        serverUri,
                        queueId,
                        queue.size(),
                        event
                    });
        }
        if (!queue.offer(event)) {
            LOGGER.log(Level.WARNING, "Queue is full, dropping pipeline events, serverUri={0}, queueId={1}, pipelineId={2}",
                    new Object[]{
                        serverUri,
                        queueId,
                        pipelineId
                    });
            Iterator<PipelineEvent> it = queue.iterator();
            while(it.hasNext()) {
                PipelineEvent e = it.next();
                if (e.pipelineId().equals(event.pipelineId())) {
                    it.remove();
                }
            }
            queue.add(new PipelineErrorEvent(event.pipelineId(), /* error code */ 1, "event queue is full"));
        }
    }

    /**
     * Publisher client thread is responsible for a set of jobs.
     * Work load for a job is processed by the same client thread in order to guarantee the ordering.
     */
    private static final class ClientThread implements Runnable {

        private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());
        private final BlockingQueue<PipelineEvent> queue;
        private final URI serverUri;
        private final String signatureHeader;
        private final int queueId;

        /**
         * Create a new client thread bound to the given queue.
         * @param serverUri the server URI
         * @param queue the queue that this thread processes
         */
        ClientThread(URI serverUri, String signatureHeader, int queueId, BlockingQueue<PipelineEvent> queue) {
            Objects.requireNonNull(queue, "queue is null");
            this.queue = queue;
            this.queueId = queueId;
            this.serverUri = serverUri;
            this.signatureHeader = signatureHeader;
        }

        @Override
        public void run() {
            while (true) {
                PipelineEvent event = null;
                try {
                    event = queue.take();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "New event processing, queueId={0}, event={1}", new Object[]{
                            queueId,
                            event
                        });
                    }
                    switch (event.eventType()) {
                        case PIPELINE_CREATED:
                        case STEP_CREATED:
                        case STAGE_CREATED:
                        case PIPELINE_COMPLETED:
                        case STEP_COMPLETED:
                        case STAGE_COMPLETED:
                        case ARTIFACTS_INFO:
                        case TESTS_INFO:
                        case PIPELINE_ERROR:
                            processEvent(event);
                            break;
                        case STEP_OUTPUT_DATA:
                            processOutputEvent((StepOutputDataEvent) event);
                            break;
                        case ARTIFACT_DATA:
                            processArtifactEvent((ArtifactDataEvent) event);
                            break;
                        case TESTSUITE_RESULT:
                            processTestSuiteEvent((TestSuiteResultEvent) event);
                            break;
                        default:
                            LOGGER.log(Level.WARNING, "Unknown event type: {0}", event.eventType());
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "End of event processing, queueId={0}, event={1}", new Object[]{
                            queueId,
                            event
                        });
                    }
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.WARNING, "Client thread interupted, queueId={0}, event={1}", new Object[]{
                        queueId,
                        event
                    });
                } catch (SocketTimeoutException ex) {
                    LOGGER.log(Level.WARNING, "Client request timeout, queueId={0}, event={1}", new Object[]{
                        queueId,
                        event
                    });
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Client request IO error, queueId=" + queueId + ", event=" + event, ex);
                } catch (Throwable ex) {
                    LOGGER.log(Level.WARNING, "Client unexpected error, queueId=" + queueId + ", event=" + event, ex);
                }
            }
        }

        /**
         * Process an event.
         *
         * @param event event
         */
        private void processEvent(PipelineEvent event) throws IOException {
            // aggregate event for the same run in the next 100 events in the queue
            // or until an output for that run is found
            List<PipelineEvent> events = new LinkedList<>();
            events.add(event);
            Iterator<PipelineEvent> it = queue.iterator();
            PipelineErrorEvent error = null;
            boolean outputFound = false;
            for (int i = 0; !outputFound && error == null && it.hasNext() && i < AGGREGATE_SIZE; i++) {
                PipelineEvent e = it.next();
                switch (e.eventType()) {
                    case PIPELINE_CREATED:
                    case STEP_CREATED:
                    case STAGE_CREATED:
                    case PIPELINE_COMPLETED:
                    case STEP_COMPLETED:
                    case STAGE_COMPLETED:
                    case ARTIFACTS_INFO:
                    case TESTS_INFO:
                        events.add(e);
                        it.remove();
                        break;
                    case PIPELINE_ERROR:
                        if (e.pipelineId().equals(event.pipelineId())) {
                            error = (PipelineErrorEvent) e;
                        }
                        break;
                    case STEP_OUTPUT_DATA:
                    case ARTIFACT_DATA:
                    case TESTSUITE_RESULT:
                        break;
                    default:
                        LOGGER.log(Level.WARNING, "Unknown event type: {0}", event.eventType());
                }
            }
            if (error != null) {
                events = new LinkedList<>();
                events.add(error);
            }

            URL url = serverUri.resolve("events").toURL();

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Sending events, queueId={0}, url={1}, events={2}", new Object[]{
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
            if (signatureHeader != null) {
                hcon.addRequestProperty("Signature", signatureHeader);
            }
            hcon.setRequestMethod("PUT");
            hcon.setDoOutput(true);
            hcon.setConnectTimeout(CONNECT_TIMEOUT);
            hcon.setReadTimeout(READ_TIMEOUT);
            JacksonSupport.write(hcon.getOutputStream(), new PipelineEvents(events));
            int code = hcon.getResponseCode();
            if (200 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code, queueId={0}, url={1}, code={2}",
                        new Object[]{
                            queueId,
                            url,
                            code
                        });
            }
        }

        /**
         * Process a step output event.
         * @param event event to process
         */
        private void processOutputEvent(StepOutputDataEvent event) throws IOException {
            URL url = serverUri.resolve("output/"
                    + event.pipelineId()
                    + "/"
                    + event.stepId())
                    .toURL();

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Sending output data event, queueId={0}, url={1}, event={2}", new Object[]{
                    queueId,
                    url,
                    event
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
            if (signatureHeader != null) {
                hcon.addRequestProperty("Signature", signatureHeader);
            }
            hcon.setRequestMethod("PUT");
            hcon.setConnectTimeout(CONNECT_TIMEOUT);
            hcon.setReadTimeout(READ_TIMEOUT);
            try (GZIPOutputStream out = new  GZIPOutputStream(hcon.getOutputStream())) {
                byte[] data = event.data();
                out.write(data, 0, data.length);
                int len = data.length;
                Iterator<PipelineEvent> it = queue.iterator();
                // aggregate output for the same step in the next 100 events in the queue
                // or until
                for (int i = 0; it.hasNext() && i < AGGREGATE_SIZE && len < OUTPUT_THRESHOLD; i++) {
                    PipelineEvent e = it.next();
                    if (e.eventType() == PipelineEventType.STEP_OUTPUT_DATA
                            && ((StepOutputDataEvent)e).stepId().equals(event.stepId())) {
                        data = ((StepOutputDataEvent)e).data();
                        out.write(data, 0, data.length);
                        len += data.length;
                        it.remove();
                    }
                }
                out.flush();
            }
            int code = hcon.getResponseCode();
            if (200 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code, queueId={0}, url={1}, code={2}, event={3}",
                        new Object[]{
                            queueId,
                            url,
                            code,
                            event
                        });
            }
        }

        /**
         * Process a test suite event.
         * @param event event
         */
        private void processTestSuiteEvent(TestSuiteResultEvent event) throws IOException {
            URL url = serverUri.resolve("files/"
                    + event.pipelineId()
                    + "/"
                    + event.stepsId()
                    + "/tests/"
                    + event.suite().name() + ".json")
                    .toURL();

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Sending test suite event, queueId={0}, url={1}, event={2}", new Object[]{
                    queueId,
                    url,
                    event
                });
            }

            URLConnection con = url.openConnection();
            if (!(con instanceof HttpURLConnection)) {
                throw new IllegalStateException("Not an HttpURLConnection");
            }
            HttpURLConnection hcon = (HttpURLConnection) con;
            hcon.setDoOutput(true);
            hcon.addRequestProperty("Content-Type", "application/json");
            if (signatureHeader != null) {
                hcon.addRequestProperty("Signature", signatureHeader);
            }
            hcon.setRequestMethod("POST");
            hcon.setConnectTimeout(CONNECT_TIMEOUT);
            hcon.setReadTimeout(READ_TIMEOUT);
            JacksonSupport.write(hcon.getOutputStream(), event.suite());
            int code = hcon.getResponseCode();
            if (201 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code, queueId={0}, url={1}, code={2}, event={3}",
                        new Object[]{
                            queueId,
                            url,
                            code,
                            event
                        });
            }
        }

        /**
         * Process an artifact event.
         * @param event event to process
         */
        private void processArtifactEvent(ArtifactDataEvent event) throws IOException {
            URL url = serverUri.resolve("files/"
                    + event.pipelineId()
                    + "/"
                    + event.stepsId()
                    + "/artifacts/"
                    + URLEncoder.encode(event.filename(), "UTF-8"))
                    .toURL();

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Sending artifact data event, queueId={0}, url={1}, event={2}", new Object[]{
                    queueId,
                    url,
                    event
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
            if (signatureHeader != null) {
                hcon.addRequestProperty("Signature", signatureHeader);
            }
            hcon.setRequestMethod("POST");
            hcon.setConnectTimeout(CONNECT_TIMEOUT);
            hcon.setReadTimeout(READ_TIMEOUT);
            try (GZIPOutputStream out = new  GZIPOutputStream(hcon.getOutputStream());
                    FileInputStream fis = new FileInputStream(event.file())) {
                byte[] buf = new byte[1024];
                int nbytes = 0;
                while (nbytes >= 0) {
                    nbytes = fis.read(buf);
                    if (nbytes > 0) {
                        out.write(buf, 0, nbytes);
                    }
                }
                out.flush();
            }
            int code = hcon.getResponseCode();
            if (201 != code) {
                LOGGER.log(Level.WARNING, "Invalid response code, queueId={0}, url={1}, code={2}, event={3}",
                        new Object[]{
                            queueId,
                            url,
                            code,
                            event
                        });
            }
        }
    }
}
