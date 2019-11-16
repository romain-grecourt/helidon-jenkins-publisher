package io.helidon.build.publisher.storage;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.common.reactive.RetrySchema;
import io.helidon.media.common.ReadableByteChannelPublisher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
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
 * Storage facility on a shared file-system with distributed locks.
 */
public final class Storage {

    private static final Logger LOGGER = Logger.getLogger(Storage.class.getName());
    private static final int APPENDER_QUEUE_SIZE = 1024; // max number of append action in the queue

    private final Path storagePath;
    private final ExecutorService appenderExecutors;
    private final BlockingQueue<AppendAction>[] appendActionQueues;

    /**
     * Create the singleton instance.
     * @param path location of the local storage storage
     * @param appenderThreads number of threads used for the append operations
     * @throws IllegalArgumentException if the path does not exist
     * @throws IllegalArgumentException if writerThreads {@code <= 0}
     * @throws IllegalArgumentException if appenderThreads {@code <= 0}
     * @throws IllegalStateException if an IO error occurs while creating the object storage client
     */
    public Storage(String path, int appenderThreads) {
        Path dirPath = FileSystems.getDefault().getPath(path);
        if (!Files.exists(dirPath)) {
            throw new IllegalArgumentException("local storage does not exist");
        }
        this.storagePath = dirPath;
        this.appenderExecutors = Executors.newFixedThreadPool(appenderThreads);
        this.appendActionQueues = new BlockingQueue[appenderThreads];
    }

    /**
     * Test if the given path exists.
     * @param path path to test
     * @return {@code true} if exists, {@code false} otherwise
     */
    public boolean exists(String path) {
        Path filePath = storagePath.resolve(path);
        return Files.exists(filePath);
    }

    /**
     * Get an {@link InputStream} to read from the given storage entry.
     * @param path entry path
     * @return {@link InputStream}
     * @throws IllegalStateException if an IO error occurs or if the storage entry is not found
     */
    public InputStream inputStream(String path) {
        Path filePath = storagePath.resolve(path);
        if (Files.exists(filePath)) {
            try {
                return Files.newInputStream(filePath);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ".inputStream() ; IO error", ex);
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("Cache entry not found: " + path);
    }

    /**
     * Get an {@link OutputStream} to overwrite the given storage entry.
     * @param path entry path
     * @return {@link OutputStream}
     * @throws IllegalStateException if an IO error occurs
     */
    public OutputStream outputStream(String path) {
        Path filePath = storagePath.resolve(path);
        try {
            Files.createDirectories(filePath.getParent());
            return Files.newOutputStream(filePath);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ".outputStream() ; IO error", ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Queue an append action for appending data to the given entry.
     * @param path the entry path
     * @param payload the payload
     * @param runnable action to be executed after the action
     * @return {@code true} if the append action is queued, {@code false} if the queue is full
     */
    public boolean append(String path, Publisher<DataChunk> payload, Runnable runnable) {
        AppendAction action = new AppendAction(path, payload, runnable);
        int queueId = action.hashCode() % appendActionQueues.length;
        BlockingQueue<AppendAction> queue = appendActionQueues[queueId];
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(APPENDER_QUEUE_SIZE);
            appendActionQueues[queueId] = queue;
            LOGGER.log(Level.FINE, () -> "creating append thread for queueId: " + queueId);
            appenderExecutors.submit(new AppendThread(queue));
        }
        LOGGER.log(Level.FINE, () -> "Adding append action for path: " + path + " to queue: #" + queueId);
        if (!queue.offer(action)) {
            LOGGER.log(Level.WARNING, "appender queue #{0} is full, dropping all append actions path: {1}",
                    new Object[]{ queueId, path });
            Iterator<AppendAction> it = queue.iterator();
            while(it.hasNext()) {
                AppendAction a = it.next();
                if (a.path.equals(path)) {
                    it.remove();
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Get the complete payload for the given entry.
     * @param path the entry path
     * @return {@code Publisher<DataChunk>}
     */
    public StoragePublisher get(String path) {
        return get(path, 0, 0);
    }

    /**
     * Get the payload for the given entry.
     * @param path the entry path
     * @param beginPosition the begin position to read from
     * @param nlines the max number of lines to display from the end of the file ; if {@code == 0} there is no maximum number
     * of lines ; if if {@code > 0} the begin position is adjusted.
     * @return {@code Publisher<DataChunk>}
     * @throws IllegalArgumentException if nlines if {@code < 0}
     */
    public StoragePublisher get(String path, long beginPosition, long nlines) {
        if (nlines < 0) {
            throw new IllegalArgumentException("nlines is negative");
        }
        Path filePath = storagePath.resolve(path);
        if (Files.exists(filePath)) {
            if (nlines > 0) {
                try {
                    long fileLength = Files.size(filePath);
                    beginPosition = findLinesPosition(filePath, beginPosition, fileLength - 1, nlines);
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            return doGet(filePath, path, beginPosition);
        }
        throw new IllegalStateException("Entry not found: " + path);
    }

    /**
     * Find the position of {@code nlines} lines before {@code position}.
     * @param file the file to process
     * @param pos the position that matches the end of the last line
     * @param min the position at which to stop reading for lines
     * @param nlines number of lines to search for
     * @return long
     * @throws IOException if an IO error occurs
     */
    private long findLinesPosition(Path filePath, long min, long pos, long nlines) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
        while(pos >= min && nlines > 0) {
            raf.seek(min);
            int readByte = raf.readByte();
            if (readByte == 0xA) {
                nlines--;
            }
            pos--;
        }
        return pos;
    }

    /**
     * Do get the payload for the given entry.
     * @param filePath the entry path
     * @param relativePath the relative path
     * @param position position from where to start reading from the file
     * @return {@code Publisher<DataChunk>}
     */
    private StoragePublisher doGet(Path filePath, String relativePath, long position) {
        try {
            if (Files.exists(filePath)) {
                FileChannel fc = FileChannel.open(filePath, StandardOpenOption.READ);
                fc.position(position);
                RetrySchema retrySchema = RetrySchema.linear(0, 10, 250);
                Publisher<DataChunk> publisher = new ReadableByteChannelPublisher(fc, retrySchema);
                return new StoragePublisher(publisher, Files.size(filePath) - position);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ".get() ; IO error", ex);
            throw new IllegalStateException(ex);
        }
        throw new IllegalStateException("Entry not found: " + relativePath);
    }

    /**
     * Storage publisher.
     */
    public final class StoragePublisher implements Publisher<DataChunk> {

        private final Publisher<DataChunk> delegate;
        private final long length;

        StoragePublisher(Publisher<DataChunk> delegate, long length) {
            this.delegate = delegate;
            this.length = length;
        }

        /**
         * Get the payload length.
         * @return long
         */
        public long length() {
            return length;
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            delegate.subscribe(subscriber);
        }
    }

    /**
     * Append thread to execute append action.
     */
    private final class AppendThread implements Runnable {

        private final BlockingQueue<AppendAction> queue;

        AppendThread(BlockingQueue<AppendAction> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    AppendAction action = queue.take();
                    action.execute();
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "AppendThread.run ; interupted", ex);
                }
            }
        }
    }

    /**
     * Append action.
     */
    private final class AppendAction {

        private final String path;
        private final Publisher<DataChunk> payload;
        private final Runnable runnable;

        AppendAction(String path, Publisher<DataChunk> payload, Runnable runnable) {
            this.path = path;
            this.payload = payload;
            this.runnable = runnable;
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

        /**
         * Execute the append action.
         */
        void execute() {
            Path filePath = storagePath.resolve(path);
            try {
                OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.APPEND);
                payload.subscribe(new OutputStreamAppender(os, runnable));
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "AppendAction.execute() ; IO error", ex);
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * Reactive subscriber that appends the received data to an {@link OutputStream} and invokes an onComplete callback.
     */
    private static final class OutputStreamAppender implements Subscriber<DataChunk> {

        private Flow.Subscription subscription;
        private final OutputStream os;
        private final Runnable runnable;

        OutputStreamAppender(OutputStream os, Runnable runnable) {
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
                LOGGER.log(Level.SEVERE, ".doAppend() ; IO error", ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            LOGGER.log(Level.SEVERE, "OutputStreamAppender.onError() ; IO error", ex);
        }

        @Override
        public void onComplete() {
            try {
                os.flush();
                os.close();
                runnable.run();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "OutputStreamAppender.onError() ; IO error", ex);
            }
        }
    }
}
