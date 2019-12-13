package io.helidon.build.publisher.model.events;

/**
 * Pipeline event listener.
 */
public interface PipelineEventListener {

    /**
     * Process a pipeline event.
     *
     * @param event the event to process
     */
    void onEvent(PipelineEvent event);
}
