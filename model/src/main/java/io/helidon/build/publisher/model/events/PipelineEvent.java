package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract pipeline event.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PipelineCreatedEvent.class, name = "PIPELINE_CREATED"),
    @JsonSubTypes.Type(value = PipelineCompletedEvent.class, name = "PIPELINE_COMPLETED"),
    @JsonSubTypes.Type(value = StepCreatedEvent.class, name = "STEP_CREATED"),
    @JsonSubTypes.Type(value = StepCompletedEvent.class, name = "STEP_COMPLETED"),
    @JsonSubTypes.Type(value = StageCreatedEvent.class, name = "STAGE_CREATED"),
    @JsonSubTypes.Type(value = StageCompletedEvent.class, name = "STAGE_COMPLETED"),
    @JsonSubTypes.Type(value = StepOutputEvent.class, name = "STEP_OUTPUT"),
    @JsonSubTypes.Type(value = StepOutputDataEvent.class, name = "STEP_OUTPUT_DATA"),
    @JsonSubTypes.Type(value = ArtifactDataEvent.class, name = "ARTIFACT_DATA"),
    @JsonSubTypes.Type(value = ArtifactsInfoEvent.class, name = "ARTIFACTS_INFO"),
    @JsonSubTypes.Type(value = TestSuiteResultEvent.class, name = "TESTSUITE_RESULT"),
    @JsonSubTypes.Type(value = TestsInfoEvent.class, name = "TESTS_INFO"),
    @JsonSubTypes.Type(value = PipelineErrorEvent.class, name = "PIPELINE_ERROR")
})
public abstract class PipelineEvent {

    final String pipelineId;

    /**
     * Create a new event.
     *
     * @param pipelineId pipeline id
     */
    PipelineEvent(String pipelineId) {
        this.pipelineId = pipelineId;
    }

    /**
     * Get the pipeline id.
     *
     * @return String
     */
    @JsonProperty
    public final String pipelineId() {
        return pipelineId;
    }

    /**
     * Get the event type.
     *
     * @return PipelineEventType, never {@code null}
     */
    @JsonProperty
    public abstract PipelineEventType eventType();
}
