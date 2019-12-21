package io.helidon.build.publisher.frontend.reactive;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.common.http.DataChunk;

/**
 * Data chunk with data referencing slices of a parent chunk.
 */
public final class VirtualChunk implements DataChunk {

    private final Parent parent;
    private final ByteBuffer data;
    private final AtomicBoolean released;

    public VirtualChunk(Parent parent, ByteBuffer data) {
        this.parent = Objects.requireNonNull(parent, "parent cannot be null!");
        parent.refCount.incrementAndGet();
        this.data = data;
        this.released = new AtomicBoolean(false);
    }

    @Override
    public ByteBuffer data() {
        return data;
    }

    @Override
    public void release() {
        if (released.compareAndSet(false, true)) {
            if (parent.refCount.decrementAndGet() <= 0) {
                parent.delegate.release();
            }
        }
    }

    /**
     * Parent chunk holder with a reference count so that it can be released
     * when all the sub-chunks are released since they share the same underlying
     * buffer.
     */
    public static final class Parent {

        private final DataChunk delegate;
        private final AtomicInteger refCount;

        public Parent(DataChunk delegate) {
            this.delegate = delegate;
            refCount = new AtomicInteger(0);
        }
    }
}