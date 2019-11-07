package io.helidon.build.publisher.webapp;

import com.hazelcast.core.HazelcastInstance;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetBucketRequest;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.RetrySchema;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.ReadableByteChannelPublisher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage.
 */
final class Storage {

    private static final Logger LOGGER = Logger.getLogger(Storage.class.getName());
    private final HazelcastInstance hz;
    private final Path storagePath;
    private final ExecutorService executor;
    private final ObjectStorage objectStorageClient;
    private final String bucketName;

    /**
     * Create the singleton instance.
     * @param hz {@code HazelcastInstance}
     * @param path location of the local storage cache
     * @param nThread number of thread for the underlying executor service
     * @param ociConfigFilePath OCI config file path
     * @param ociConfigProfile OCI config profile
     * @param ociRegion OCI region id
     * @throws IllegalArgumentException if the path does not exist
     * @throws IllegalStateException if an IO error occurs while creating the object storage client
     */
    Storage(HazelcastInstance hz, String path, int nThread, String ociConfigFilePath, String ociConfigProfile, String ociRegion,
            String bucketName) {

        this.hz = hz;
        Path dirPath = FileSystems.getDefault().getPath(path);
        if (!Files.exists(dirPath)) {
            throw new IllegalArgumentException("local storage does not exist");
        }
        this.storagePath = dirPath;
        this.executor = Executors.newFixedThreadPool(nThread);
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalArgumentException("bucketName is null or empty");
        }
        this.bucketName = bucketName;
        try {
            this.objectStorageClient = createObjectStorageClient(ociConfigFilePath, ociConfigProfile, ociRegion);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        try {
            objectStorageClient.getBucket(GetBucketRequest.builder()
                    .bucketName(bucketName)
                    .build());
        } catch (BmcException ex) {
            throw new IllegalStateException("An error occured while getting bucket", ex);
        }
    }

    /**
     * Create an {@link ObjectStorageAsync} instance.
     * @param configFilePath OCI config file path
     * @param profile OCI config profile
     * @param region OCI region id
     * @return ObjectStorageAsync
     * @throws IOException if an IO error occurs
     */
    private ObjectStorage createObjectStorageClient(String configFilePath, String profile, String region) throws IOException {
        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFilePath, profile);
        ObjectStorage client = new ObjectStorageClient(provider);
        client.setRegion(region);
        return client;
    }

    /**
     * Get an {@link InputStream} to read from the given cache entry.
     * @param path entry path
     * @return InputStream
     * @throws IllegalStateException if an IO error occurs or if the cache entry is not found
     */
    InputStream inputStream(String path) {
        Path filePath = storagePath.resolve(path);
        if (Files.exists(filePath)) {
            final StorageLock lock = StorageLock.getOrCreate(hz, path);
            try {
                LOGGER.log(Level.FINE, () -> ".inputStream() ; locking read lock for entry: " + path);
                lock.readLock().lock();
                LOGGER.log(Level.FINE, () -> ".inputStream() ; creating input stream for entry: " + path);
                return new InputStreamDelegate(Files.newInputStream(filePath), () -> {
                    LOGGER.log(Level.FINE, () -> ".inputStream() ; unlocking read lock for entry: " + path);
                    lock.readLock().unlock();
                });
            } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ".inputStream() ; IO error", ex);
                lock.readLock().unlock();
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("Cache entry not found: " + path);
    }

    /**
     * Get an {@link OutputStream} to overwrite the given cache entry.
     * @param path entry path
     * @throws IllegalStateException if an IO error occurs
     */
    OutputStream outputStream(String path) {
        String newPath = path + ".new"; // create a tmp file
        Path newFilePath = storagePath.resolve(newPath);
        try {
            Files.createDirectories(newFilePath.getParent());
            LOGGER.log(Level.FINE, () -> ".outputStream() ; creating output stream for: " + path);
            return new OuputStreamDelegate(Files.newOutputStream(newFilePath), () -> {
                executor.submit(() -> rename(newPath, path));
            });
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ".outputStream() ; IO error", ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Append data to the given entry.
     * @param path the entry path
     * @param payload the payload
     * @param onComplete callback invoked when the data is successfully appended
     */
    void append(String path, Publisher<DataChunk> payload, Runnable onComplete) {
        Path filePath = storagePath.resolve(path);
        if (Files.exists(filePath)) {
            executor.submit(() -> doAppend(path, payload, onComplete));
        }
        throw new IllegalStateException("Cache entry not found: " + path);
    }

    /**
     * Perform the work of appending data to the given entry.
     * @param path the entry path
     * @param payload the payload
     * @param onComplete callback invoked when the data is successfully appended
     */
    void doAppend(String path, Publisher<DataChunk> payload, Runnable onComplete) {
        Path filePath = storagePath.resolve(path);
        final StorageLock lock = StorageLock.getOrCreate(hz, path);
        try {
            LOGGER.log(Level.FINE, () -> ".doAppend() ; locking write lock for entry: " + path);
            lock.writeLock().lock();
            OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.APPEND);
            // TODO use functional subscribers with subscribeable ?
            payload.subscribe(new OutputStreamAppender(os, () -> {
                lock.writeLock().unlock();
                onComplete.run();
            }));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ".doAppend() ; IO error", ex);
            lock.writeLock().unlock();
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Tail the payload for the given entry.
     * @param path the entry path
     * @param nlines the location from the tail in number of lines, if the value must be greater or equal to zero
     * @param follow if {@code true} and the entry is cached, the returned publisher will never complete. Otherwise
     * @return {@code Publisher<DataChunk>}
     * @throws IllegalArgumentException if {@code nlines} is negative
     */
    Publisher<DataChunk> tail(String path, int nlines, boolean follow) {
        if (nlines < 0) {
            throw new IllegalArgumentException("nlines is negative");
        }
        Path filePath = storagePath.resolve(path);
        if (Files.exists(filePath)) {
            return Multi.empty();
        }
        Publisher<ByteBuffer> pub = getFromObjectStorage(path);
        if (nlines == 0) {
            return Multi.from(pub).map(DataChunk::create);
        }
        // TODO cache to disk
        // then calculate nlines pos from head
        // then tail from cache
        LineFilteringProcessor processor = new LineFilteringProcessor(nlines);
        pub.subscribe(processor);
        return Multi.from(processor).map((line) -> DataChunk.create(line.getBytes()));
    }

    /**
     * Get the payload for the given entry.
     * @param path the entry path
     * @return {@code Publisher<DataChunk>}
     */
    Publisher<DataChunk> get(String path) {
        Path filePath = storagePath.resolve(path);
        if (Files.exists(filePath)) {
            final StorageLock lock = StorageLock.getOrCreate(hz, path);
            try {
                LOGGER.log(Level.FINE, () -> ".get() ; locking read lock for entry: " + path);
                lock.readLock().lock();
                if (Files.exists(filePath)) {
                    LOGGER.log(Level.FINE, () -> ".get() ; creating publisher from local disk for entry: " + path);
                    FileChannel fc = FileChannel.open(storagePath.resolve(path), StandardOpenOption.READ);
                    RetrySchema retrySchema = RetrySchema.linear(0, 10, 250);
                    return new PublisherDelegate<>(new ReadableByteChannelPublisher(fc, retrySchema), () -> {
                        LOGGER.log(Level.FINE, () -> ".get() ; unlocking read lock for entry: " + path);
                        lock.readLock().unlock();
                    });
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ".get() ; IO error", ex);
                lock.readLock().unlock();
                return Single.error(ex);
            }
        }
        return Multi.from(getFromObjectStorage(path))
                .map(DataChunk::create);
    }

    /**
     * Archive the given cache entry.
     * @param path the file path
     * @throws IllegalStateException if an IO error occurs or if the cache entry is not found
     */
    void archive(String path) {
        Path filePath = storagePath.resolve(path);
        if (Files.exists(filePath)) {
            try {
                LOGGER.log(Level.FINE, () -> ".archive() ; archiving cache entry: " + path);
                objectStorageClient.putObject(PutObjectRequest.builder()
                        .bucketName(bucketName)
                        .putObjectBody(inputStream(path))
                        .objectName(path)
                        .build());
                executor.submit(() -> delete(path));
            } catch (BmcException ex) {
                LOGGER.log(Level.SEVERE, ".archive() ; error", ex);
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("Cache entry not found: " + path);
    }

    /**
     * Get the given entry from the object storage.
     * @param path the entry path
     * @return {@code Publisher<ByteBuffer>}
     */
    private Publisher<ByteBuffer> getFromObjectStorage(String path) {
        LOGGER.log(Level.FINE, () -> ".getFromObjectStorage() ; creating publisher from remote object storage for entry: " + path);
        GetObjectResponse resp = objectStorageClient.getObject(GetObjectRequest.builder()
                .bucketName(bucketName)
                .objectName(path)
                .build());
        return new InputStreamPublisher(resp.getInputStream(), /* bufferSize */ 2048);
    }

    /**
     * Rename a entry 
     * This method acquires a write lock which waits for all readers.
     * @param sourcePath the target path to rename fromPath to
     * @param targetPath the input path to be renamed
     * @throw IllegalStateException if an IOException occurs while renaming the entry
     */
    private void rename(String sourcePath, String targetPath) {
        Path source = storagePath.resolve(targetPath);
        Path target = storagePath.resolve(sourcePath);
        StorageLock lock = StorageLock.getOrCreate(hz, sourcePath);
        try {
            LOGGER.log(Level.FINE, () -> ".rename() ; locking write lock for entry: " + targetPath);
            lock.writeLock().lock();
            LOGGER.log(Level.FINE, () -> ".rename() ; moving " + sourcePath + " to " + targetPath);
            Files.move(source, target);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            LOGGER.log(Level.FINE, () -> ".rename() ; unlocking write lock for entry: " + targetPath);
            lock.writeLock().unlock();
        }
    }

    /**
     * Delete an entry from the cache.
     * This method acquires a write lock which waits for all readers.
     * @param path entry path
     * @throw IllegalStateException if an IOException occurs while deleting the entry
     */
    private void delete(String path) {
        Path filePath = storagePath.resolve(path);
        final StorageLock lock = StorageLock.getOrCreate(hz, path);
        try {
            LOGGER.log(Level.FINE, () -> ".delete() ; locking write lock for entry: " + path);
            lock.writeLock().lock();
            LOGGER.log(Level.FINE, () -> ".delete() ; deleting entry: " + path);
            Files.delete(filePath);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ".delete() ; error while deleting entry: " + path, ex);
            throw new IllegalStateException(ex);
        } finally {
            LOGGER.log(Level.FINE, () -> ".delete() ; unlocking write lock for entry: " + path);
            lock.writeLock().unlock();
        }
    }

    /**
     * OutputStrema delegate with onClose callback.
     */
    private static final class OuputStreamDelegate extends OutputStream {

        private final OutputStream delegate;
        private final Runnable onClose;

        OuputStreamDelegate(OutputStream delegate, Runnable onClose) {
            this.delegate = delegate;
            this.onClose = onClose;
        }

        @Override
        public void close() throws IOException {
            onClose.run();
            delegate.close();
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }
    }

    /**
     * InputStream delegate with onClose callback.
     */
    private static final class InputStreamDelegate extends InputStream {

        private final InputStream delegate;
        private final Runnable onClose;

        InputStreamDelegate(InputStream delegate, Runnable onClose) {
            this.delegate = delegate;
            this.onClose = onClose;
        }

        @Override
        public void close() throws IOException {
            onClose.run();
            delegate.close();
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }
    }

    /**
     * Publisher delegate with onComplete callback.
     * @param <T> published items type
     */
    private static final class PublisherDelegate<T> implements Publisher<T> {

        private final Publisher<T> delegate;
        private final Runnable onComplete;

        PublisherDelegate(Publisher<T> delegate, Runnable onComplete) {
            this.delegate = delegate;
            this.onComplete = onComplete;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super T> subscriber) {
            delegate.subscribe(new SubscriberDelegate(subscriber, onComplete));
        }
    }

    /**
     * Subscriber delegate with onComplete callback.
     * @param <T> subscribed items type
     */
    private static final class SubscriberDelegate<T> implements Subscriber<T> {

        private final Subscriber<? super T> delegate;
        private final Runnable onComplete;

        SubscriberDelegate(Subscriber<? super T> delegate, Runnable onComplete) {
            this.delegate = delegate;
            this.onComplete = onComplete;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(T item) {
            delegate.onNext(item);
        }

        @Override
        public void onError(Throwable ex) {
            delegate.onError(ex);
            onComplete.run();
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
            onComplete.run();
        }
    }

    /**
     * Reactive subscriber that appends the received data to an {@link OutputStream} and invokes an onComplete callback.
     */
    private static final class OutputStreamAppender implements Subscriber<DataChunk> {

        private Flow.Subscription subscription;
        private final Runnable onComplete;
        private final OutputStream os;

        OutputStreamAppender(OutputStream os, Runnable onComplete) {
            this.os = os;
            this.onComplete = onComplete;
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
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "OutputStreamAppender.onError() ; IO error", ex);
            } finally {
                onComplete.run();
            }
        }
    }
}
