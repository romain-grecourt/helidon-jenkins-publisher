package io.helidon.build.publisher.model;

/**
 * Pipeline descriptor manager to load and save a pipeline descriptor.
 */
public interface PipelineDescriptorManager {

    /**
     * Load a pipeline.
     * @param id pipeline id
     * @return Pipeline
     */
    Pipeline loadPipeline(String id);

    /**
     * Save a pipeline.
     * @param pipeline pipeline to save
     */
    void savePipeline(Pipeline pipeline);
}
