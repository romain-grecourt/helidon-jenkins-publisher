package io.helidon.build.publisher.model;

/**
 * Pipeline file manager to load and save a pipeline descriptor.
 */
public interface PipelineDescriptorManager {

    /**
     * Load a pipeline.
     * @param id pipeline id
     * @return Pipeline
     */
    Pipeline load(String id);

    /**
     * Save a pipeline.
     * @param pipeline pipeline to save
     */
    void save(Pipeline pipeline);
}
