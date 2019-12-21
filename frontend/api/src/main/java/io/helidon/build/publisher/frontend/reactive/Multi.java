package io.helidon.build.publisher.frontend.reactive;

import io.helidon.common.reactive.Flow.Publisher;
import java.util.Objects;

/**
 * Multiple items publisher facility.
 *
 * @param <T> item type
 */
public interface Multi<T> extends io.helidon.common.reactive.Multi<T> {

    /**
     * Create a {@link Multi} instance wrapped around the given publisher.
     *
     * @param <T> item type
     * @param source source publisher
     * @return Multi
     * @throws NullPointerException if source is {@code null}
     */
    @SuppressWarnings("unchecked")
    static <T> Multi<T> from(Publisher<T> source) {
        if (source instanceof Multi) {
            return (Multi<T>) source;
        }
        return new MultiFromPublisher<>(source);
    }

    /**
     * Limit this {@link Multi} instance using the given {@link Limiter}.
     *
     * @param limiter limiter
     * @return Multi
     * @throws NullPointerException if limiter is {@code null}
     */
    default Multi<T> limit(Limiter<T> limiter) {
        return new LimitPublisher<>(this, Objects.requireNonNull(limiter));
    }
}
