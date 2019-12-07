package io.helidon.build.publisher.reactive;

import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;

/**
 * A delegating publisher that subscribes a delegating subscriber in order to limit the published data.
 */
final class LimitPublisher<T> implements Multi<T> {

    private final Publisher<T> delegate;
    private final Limiter<T> limiter;

    LimitPublisher(Publisher<T> delegate, Limiter<T> limiter) {
        this.delegate = delegate;
        this.limiter = limiter;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        delegate.subscribe(new LimitSubscriber(subscriber, limiter));
    }

    /**
     * A delegating subscriber that limits the published data to a fixed amount.
     */
    private static final class LimitSubscriber<T> implements Subscriber<T> {

        private final Subscriber<? super T> delegate;
        private final Limiter<T> limiter;
        private final LimiterActionImpl<T> completeAction;

        LimitSubscriber(Subscriber<? super T> delegate, Limiter<T> limiter) {
            this.delegate = delegate;
            this.completeAction = new LimiterActionImpl(delegate);
            this.limiter = limiter;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(T item) {
            limiter.limit(item, completeAction);
        }

        @Override
        public void onError(Throwable ex) {
            delegate.onError(ex);
        }

        @Override
        public void onComplete() {
            if (!completeAction.isCompleted()) {
                delegate.onComplete();
            }
        }
    }

    private static final class LimiterActionImpl<T> implements Limiter.Action<T> {

        private final Subscriber<T> subscriber;
        private boolean completed;

        LimiterActionImpl(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        boolean isCompleted() {
            return completed;
        }

        @Override
        public void complete(T lastItem) {
            subscriber.onNext(lastItem);
            subscriber.onComplete();
            completed = true;
        }

        @Override
        public void publish(T item) {
            subscriber.onNext(item);
        }
    }
}
