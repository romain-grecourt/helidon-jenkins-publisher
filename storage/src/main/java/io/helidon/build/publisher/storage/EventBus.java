package io.helidon.build.publisher.storage;

import io.helidon.build.publisher.model.PipelineEvents.Event;
import java.util.List;

/**
 * Event bus.
 */
public final class EventBus {

    /**
     * Create the event bus instance.
     */
    public EventBus() {
    }

    /**
     * Publish the given event.
     * @param event event to publish
     */
    public void publish(Event event) {
        // TODO
    }

    /**
     * Publish the given events.
     * @param events events to publish
     */
    public void publish(List<Event> events) {
        for(Event event : events) {
            publish(event);
        }
    }

    /**
     * Subscribe to events.
     * @param listener message listener
     */
    public void subscribe(Listener listener) {
        // TODO
    }

    /**
     * Listener interface.
     */
    public interface Listener {

        /**
         * Process an event.
         * @param event event to be processed
         */
        void onEvent(Event event);
    }
}
