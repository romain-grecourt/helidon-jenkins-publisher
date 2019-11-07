package io.helidon.build.publisher.webapp;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

/**
 * Message bus.
 */
final class MessageBus {

    private final HazelcastInstance hz;
    private final ITopic<JobEvent> jobEvents;
    private final ITopic<JobEvent.OutputEvent> stepOutputs;

    /**
     * Create the singleton instance.
     */
    MessageBus(HazelcastInstance hz) {
        this.hz = hz;
        this.jobEvents = hz.getTopic("jobEvents");
        this.stepOutputs = hz.getTopic("stepOutputs");
    }

    /**
     * Publish the given job event.
     * @param event job event to publish
     */
    void publishJobEvent(JobEvent event) {
        jobEvents.publish(event);
    }

    /**
     * Subscribe to job events.
     * @param listener message listener
     */
    void subscribeJobEvent(MessageListener<JobEvent> listener) {
        jobEvents.addMessageListener(listener);
    }

    /**
     * Publish the given job event.
     * @param event job event to publish
     */
    void publishStepOutput(JobEvent.OutputEvent event) {
        stepOutputs.publish(event);
    }

    /**
     * Subscribe to step output events.
     * @param listener message listener
     */
    void subscribeStepOutput(MessageListener<JobEvent.OutputEvent> listener) {
        stepOutputs.addMessageListener(listener);
    }
}
