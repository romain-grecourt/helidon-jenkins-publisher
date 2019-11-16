package io.helidon.build.publisher.storage;

/**
 * Utility to create storage paths.
 */
public final class StoragePaths {

    private StoragePaths() {
    }

    /**
     * Get the storage path entry for an output file.
     * @param pipelineId pipeline id
     * @param stepId step id
     * @return String
     * @throws IllegalArgumentException if pipelineId {@code null} or empty
     * @throws IllegalArgumentException if stepId {@code <=0}
     */
    public static String stepOutput(String pipelineId, int stepId) {
        if (pipelineId == null || pipelineId.isEmpty()) {
            throw new IllegalArgumentException("pipelineId required");
        }
        if (stepId <= 0) {
            throw new IllegalArgumentException("Invalid stepId: " + stepId);
        }
        return pipelineId + "/step-" + stepId + ".log";
    }

    /**
     * Get the storage path entry for a pipelineId descriptor JSON file.
     * @param pipelineId pipeline id
     * @return String
     * @throws IllegalArgumentException if pipelineId is {@code null} or empty
     */
    public static String pipelineDescriptor(String pipelineId) {
        if (pipelineId == null || pipelineId.isEmpty()) {
            throw new IllegalArgumentException("pipelineId required");
        }
        return pipelineId + "/pipeline.json";
    }
}