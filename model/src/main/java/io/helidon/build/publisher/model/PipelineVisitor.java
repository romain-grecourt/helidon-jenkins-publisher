package io.helidon.build.publisher.model;

/**
 * Pipeline visitor.
 */
public interface PipelineVisitor {

    /**
     * The start of the traversal.
     * @param pipeline the pipeline
     */
    void visitStart(Pipeline pipeline);

    /**
     * The start of the traversal of a stages node.
     *
     * @param stages the stages node
     * @param depth the depth
     */
    void visitStagesStart(Stages stages, int depth);

    /**
     * The start of the traversal of a steps node.
     *
     * @param steps the steps node
     * @param depth the depth
     */
    void visitStepsStart(Steps steps, int depth);

    /**
     * Visit a step node (leaf).
     * @param step the step node
     * @param depth the depth
     */
    void visitStep(Step step, int depth);

    /**
     * The end of the traversal of a steps node.
     * @param steps the steps node
     * @param depth the depth
     */
    void visitStepsEnd(Steps steps, int depth);

    /**
     * The end of the traversal of a stages node.
     *
     * @param stages the stages node
     * @param depth the depth
     */
    void visitStagesEnd(Stages stages, int depth);

    /**
     * The end of the traversal.
     * @param pipeline the pipeline
     */
    void visitEnd(Pipeline pipeline);
}
