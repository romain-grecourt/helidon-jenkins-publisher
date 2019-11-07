package io.helidon.build.publisher.webapp;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ISemaphore;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A read write lock to handle caching on the local disk.
 */
final class StorageLock implements ReadWriteLock {

    private static final Map<String, WeakReference<StorageLock>> LOCKS_CACHE = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(StorageLock.class.getName());

    private final ISemaphore readSemaphore;
    private final ISemaphore writeSemaphore;
    private final Lock readLock;
    private final Lock writeLock;

    /**
     * Create a new storage lock.
     * @param hz hazelcast instance
     * @param id lock id
     * @throws IllegalArgumentException if the id is invalid
     */
    private StorageLock(HazelcastInstance hz, String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid id: null or empty");
        }
        if (id.startsWith("storagelock:")) {
            throw new IllegalArgumentException("Lock id cannot start with 'storagelock:'");
        }
        this.readSemaphore = hz.getCPSubsystem().getSemaphore("storagelock:" + id + ".read");
        this.readSemaphore.init(1);
        this.writeSemaphore = hz.getCPSubsystem().getSemaphore("storagelock:" + id + ".write");
        this.writeSemaphore.init(1);
        this.readLock = new ReadLock();
        this.writeLock = new WriteLock();
    }

    /**
     * Get or create a storage lock for the given id.
     * @param hz hazelcast instance
     * @param id lock id
     * @return StorageLock
     */
    static StorageLock getOrCreate(HazelcastInstance hz, String id) {
        synchronized(LOCKS_CACHE) {
            WeakReference<StorageLock> ref = LOCKS_CACHE.get(id);
            if (ref != null && ref.get() != null) {
                return ref.get();
            }
        }
        StorageLock lock = new StorageLock(hz, id);
        synchronized(LOCKS_CACHE) {
            WeakReference<StorageLock> ref = LOCKS_CACHE.get(id);
            if (ref  != null && ref .get() != null) {
                return ref .get();
            }
            LOCKS_CACHE.put(id, new WeakReference<>(lock));
            return lock;
        }
    }

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    /**
     * The write lock.
     */
    private final class WriteLock implements Lock {

        @Override
        public void lock() {
            try {
                lockInterruptibly();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            writeSemaphore.acquire();
            try {
                // wait for all reads
                readSemaphore.acquire();
            } finally {
                readSemaphore.release();
            }
        }

        @Override
        public boolean tryLock() {
            return writeSemaphore.tryAcquire();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            try {
                readSemaphore.acquire();
                writeSemaphore.increasePermits(1);
                if (!writeSemaphore.tryAcquire(time, unit)) {
                    writeSemaphore.reducePermits(1);
                    return false;
                }
                return true;
            } finally {
                readSemaphore.release();
            }
        }

        @Override
        public void unlock() {
            writeSemaphore.reducePermits(1);
            writeSemaphore.release();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("condition is not supported");
        }
    }

    /**
     * The read lock.
     */
    private final class ReadLock implements Lock {

        @Override
        public void lock() {
            try {
                lockInterruptibly();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            try {
                // acquire a permit in the write semaphore
                writeSemaphore.acquire();
                readSemaphore.acquire();
                readSemaphore.increasePermits(1);
            } finally {
                // release the permit from the write semaphore
                writeSemaphore.release();
            }
        }

        @Override
        public boolean tryLock() {
            readSemaphore.increasePermits(1);
            if (!readSemaphore.tryAcquire()) {
                readSemaphore.reducePermits(1);
                return false;
            }
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            try {
                // acquire a permit in the write semaphore
                writeSemaphore.acquire();
                readSemaphore.increasePermits(1);
                if (!readSemaphore.tryAcquire(time, unit)) {
                    readSemaphore.reducePermits(1);
                    return false;
                }
                return true;
            } finally {
                // release the permit from the write semaphore
                writeSemaphore.release();
            }
        }

        @Override
        public void unlock() {
            readSemaphore.reducePermits(1);
            readSemaphore.release();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("condition is not supported");
        }
    }
}
