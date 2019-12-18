package io.helidon.build.publisher.model;

/**
 * Pipeline info processor.
 */
public interface PipelineInfoAugmenter {

    /**
     * Process a pipeline info.
     * @param info pipeline info
     * @return {@code true} if this augmented matched
     */
    boolean process(PipelineInfo info);
}
