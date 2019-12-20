package io.helidon.build.publisher.backend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 * File appender.
 */
final class FileAppender {

    private static final Logger LOGGER = Logger.getLogger(FileAppender.class.getName());
    private static final int QUEUE_SIZE = 1024; // max number of append action in the queue

    private final ExecutorService executors;
    private final BlockingQueue<WorkItem>[] workQueues;

    /**
     * Create a new file appender.
     * @param nthreads the thread pool size
     */
    FileAppender(int nthreads) {
        this.executors = Executors.newFixedThreadPool(nthreads);
        this.workQueues = new BlockingQueue[nthreads];
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Creating file appender nThreads={0}", nthreads);
        }
    }

    /**
     * Append the data of a publisher to a file in the storage at the given path.
     * @param chunks the data
     * @param filePath the file path
     * @param compressed true if the payload is {@code gzip} compressed
     * @return a future that completes normally when the data is appended or exceptionally if an error occurred
     */
    CompletionStage<Void> append(Publisher<DataChunk> chunks, Path filePath, boolean compressed) {
        int queueId = Math.floorMod(filePath.hashCode(), workQueues.length);
        BlockingQueue<WorkItem> queue = workQueues[queueId];
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(QUEUE_SIZE);
            workQueues[queueId] = queue;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "creating appender thread for queueId: #{0}", queueId);
            }
            executors.submit(new AppenderThread(queue, queueId));
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        WorkItem workItem = new WorkItem(chunks, filePath, compressed, future);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Adding work item to queue, queueId={0}, queueSize={1}, workItem={2}",
                    new Object[]{
                        queueId,
                        queue.size(),
                        workItem
                    });
        }
        if (!queue.offer(workItem)) {
            LOGGER.log(Level.WARNING, "Queue full, draining work item, queueId={0}, workItem={1}", new Object[]{
                queueId,
                workItem
            });
            chunks.subscribe(new Subscriber<DataChunk>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(DataChunk item) {
                    item.release();
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            });
            future.completeExceptionally(new IllegalStateException("queue is full"));
        }
        return future;
    }

    private OutputStream outputStream(Path filePath) {
        try {
            if (!Files.exists(filePath.getParent())) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating directory: {0}", filePath.getParent());
                }
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Creating file: {0}", filePath);
                }
                Files.createFile(filePath);
            }
            return Files.newOutputStream(filePath, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private final class AppenderThread implements Runnable {

        private final BlockingQueue<WorkItem> queue;
        private final int queueId;

        AppenderThread(BlockingQueue<WorkItem> queue, int queueId) {
            this.queue = queue;
            this.queueId = queueId;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    WorkItem workItem = queue.take();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "New work item processing, queueId={0}, workItem={1}", new Object[]{
                            queueId,
                            workItem
                        });
                    }
                    Appender appender = new Appender(outputStream(workItem.filePath), workItem.compressed, workItem.future);
                    workItem.chunks.subscribe(appender);
                    workItem.future.get(2, TimeUnit.MINUTES);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "End of work item processing, queueId={0}, workItem={1}", new Object[]{
                            queueId,
                            workItem
                        });
                    }
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.WARNING, "Appender thread interupted, queueId={0}", queueId);
                } catch (TimeoutException ex) {
                    LOGGER.log(Level.WARNING, "Append timeout, queueId={0}", queueId);
                } catch (ExecutionException ex) {
                    LOGGER.log(Level.WARNING, "Append execution error, queueId=" + queueId, ex);
                } catch (Throwable ex) {
                    LOGGER.log(Level.WARNING, "Append unexpected error, queueId=" + queueId, ex);
                } 
            }
        }
    }

    private final class WorkItem {

        private final Path filePath;
        private final Publisher<DataChunk> chunks;
        private final boolean compressed;
        private final CompletableFuture<Void> future;

        WorkItem(Publisher<DataChunk> chunks, Path filePath, boolean compressed, CompletableFuture<Void> future) {
            this.chunks = chunks;
            this.filePath = filePath;
            this.compressed = compressed;
            this.future = future;
        }

        @Override
        public String toString() {
            return WorkItem.class.getSimpleName() + " {"
                    + " path=" + filePath
                    + ", compressed=" + compressed
                    + " }";
        }

    }

    private static final class Appender implements Subscriber<DataChunk> {

        private Subscription subscription;
        private final OutputStream os;
        private final CompletableFuture<Void> future;
        private final boolean compressed;

        Appender(OutputStream os, boolean compressed, CompletableFuture<Void> future) {
            this.os = os;
            this.compressed = compressed;
            this.future = future;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(DataChunk item) {
            try {
                byte[] data = item.bytes();
                if (!compressed) {
                    os.write(data);
                } else {
                    try (GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(data))) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) != -1) {
                            os.write(buf, 0, len);
                        }
                    }
                }
                item.release();
            } catch (IOException ex) {
                subscription.cancel();
                future.completeExceptionally(ex);
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            future.completeExceptionally(ex);
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

        @Override
        public void onComplete() {
            try {
                os.flush();
                os.close();
                future.complete(null);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
