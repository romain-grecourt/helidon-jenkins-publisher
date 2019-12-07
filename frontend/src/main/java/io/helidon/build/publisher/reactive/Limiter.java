package io.helidon.build.publisher.reactive;

/**
 * A limiter is a mapper that can stop the publishing.
 * @param <T> subscribed item type
 */
public interface Limiter<T> {

    /**
     * Limit processing.
     *
     * @param item object to process
     * @param action limiter action
     */
    void limit(T item, Action<T> action);

    /**
     * Complete action to stop the subscription.
     * @param <T> subscribed item type
     */
    interface Action<T> {

        /**
         * Publish the given item.
         * @param item to publish
         */
        void publish(T item);

        /**
         * Publish the last item.
         * @param lastItem last item to be published
         */
        void complete(T lastItem);
    }
}
