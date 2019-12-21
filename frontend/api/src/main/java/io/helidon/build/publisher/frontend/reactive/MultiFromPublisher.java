package io.helidon.build.publisher.frontend.reactive;

import java.util.Objects;

import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;

/**
 * Implementation of {@link Multi} that is backed by a {@link Publisher}.
 *
 * @param <T> items type
 */
final class MultiFromPublisher<T> implements Multi<T> {

    private final Publisher<? extends T> source;

    MultiFromPublisher(Publisher<? extends T> source) {
        Objects.requireNonNull(source, "source cannot be null!");
        this.source = source;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        source.subscribe(subscriber);
    }
}
