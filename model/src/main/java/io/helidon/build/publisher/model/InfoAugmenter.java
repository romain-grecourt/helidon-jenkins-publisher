package io.helidon.build.publisher.model;

/**
 * Pipeline info augmenter.
 */
public interface InfoAugmenter {

    /**
     * Process a pipeline info.
     * @param info pipeline info
     * @return {@code true} if this augmented matched
     */
    boolean process(PipelineInfo info);
}
