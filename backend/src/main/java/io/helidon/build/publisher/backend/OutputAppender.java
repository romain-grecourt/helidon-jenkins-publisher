package io.helidon.build.publisher.backend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * Output appender.
 */
final class OutputAppender {

    private static final Logger LOGGER = Logger.getLogger(OutputAppender.class.getName());
    private static final int QUEUE_SIZE = 1024; // max number of append action in the queue

    private final ExecutorService appenderExecutors;
    private final BlockingQueue<AppendAction>[] appendActionQueues;
    private final Path storagePath;

    OutputAppender(Path storagePath, int nthreads) {
        this.storagePath = storagePath;
        this.appenderExecutors = Executors.newFixedThreadPool(nthreads);
        this.appendActionQueues = new BlockingQueue[nthreads];
    }

    boolean append(Publisher<DataChunk> payload, String path, boolean compressed) {
        int queueId = Math.floorMod(path.hashCode(), appendActionQueues.length);
        BlockingQueue<AppendAction> queue = appendActionQueues[queueId];
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(QUEUE_SIZE);
            appendActionQueues[queueId] = queue;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "creating appender thread for queueId: #{0}", queueId);
            }
            appenderExecutors.submit(new AppenderThread(queue));
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Adding append action ({0}) to queue: #{1}", new Object[] {
                path,
                queueId
            });
        }
        AppendAction action = new AppendAction(payload, outputStream(path), compressed);
        if (!queue.offer(action)) {
            LOGGER.log(Level.WARNING, "Queue #{0} is full, dropping all append actions ({1})", new Object[]{
                queueId,
                path
            });
            action.drain();
            return false;
        }
        return true;
    }

    private OutputStream outputStream(String path) {
        Path filePath = storagePath.resolve(path);
        try {
            if (!Files.exists(filePath)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating file: {0}", path);
                }
                Files.createFile(filePath);
            }
            return Files.newOutputStream(filePath, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private final class AppenderThread implements Runnable {

        private final BlockingQueue<AppendAction> queue;

        AppenderThread(BlockingQueue<AppendAction> queue) {
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

        private final Appender appender;

        AppendAction(Publisher<DataChunk> payload, OutputStream os, boolean compressed) {
            appender = new Appender(os, compressed, this::onComplete);
            payload.subscribe(appender);
        }

        void drain() {
            appender.drain = true;
            appender.subscription.request(Long.MAX_VALUE);
        }

        void execute() {
            appender.subscription.request(1);
        }

        void onComplete() {
            // TODO publish event
        }
    }

    private static final class Appender implements Subscriber<DataChunk> {

        private Subscription subscription;
        private final OutputStream os;
        private final Runnable runnable;
        private final boolean compressed;
        private boolean drain;

        Appender(OutputStream os, boolean compressed, Runnable runnable) {
            this.os = os;
            this.compressed = compressed;
            this.runnable = runnable;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onNext(DataChunk item) {
            try {
                if (drain) {
                    item.release();
                } else {
                    byte[] data = item.bytes();
                    if (!compressed) {
                        os.write(data);
                    } else {
                        GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(data));
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) != -1) {
                            os.write(buf, 0, len);
                        }
                    }
                    item.release();
                    subscription.request(1);
                }
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