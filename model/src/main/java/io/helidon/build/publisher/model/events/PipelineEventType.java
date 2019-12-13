package io.helidon.build.publisher.model.events;

/**
 * Event type.
 */
public enum PipelineEventType {

    /**
     * A new step has been created and is active.
     */
    STEP_CREATED,

    /**
     * A step has been completed.
     */
    STEP_COMPLETED,

    /**
     * A new stage has been created.
     */
    STAGE_CREATED,

    /**
     * A stage has been completed.
     */
    STAGE_COMPLETED,

    /**
     * Output is available.
     */
    STEP_OUTPUT,

    /**
     * Output data has been produced.
     */
    STEP_OUTPUT_DATA,

    /**
     * Artifact file was archived.
     */
    ARTIFACT_DATA,

    /**
     * Artifacts info for a steps stage.
     */
    ARTIFACTS_INFO,

    /**
     * Test suite data.
     */
    TESTSUITE_RESULT,

    /**
     * Tests info for a steps stage.
     */
    TESTS_INFO,

    /**
     * A pipeline has been created.
     */
    PIPELINE_CREATED,

    /**
     * A pipeline has been completed.
     */
    PIPELINE_COMPLETED,

    /**
     * An error occurred.
     */
    PIPELINE_ERROR
}
