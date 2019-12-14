package io.helidon.build.publisher.model;

/**
 * Pipeline pretty printer.
 */
public class PipelinePrettyPrinter implements PipelineVisitor {

    private static final String INDENT = "  ";

    private final boolean excludeSyntheticSteps;
    private final boolean excludeMetaSteps;
    private final StringBuilder sb;

    /**
     * Create a new pipeline printer.
     * @param excludeSyntheticSteps {@code true} to exclude synthetic steps
     * @param excludeMetaSteps {@code true} to exclude meta steps
     */
    public PipelinePrettyPrinter(boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
        this.excludeSyntheticSteps = excludeSyntheticSteps;
        this.excludeMetaSteps = excludeMetaSteps;
        this.sb = new StringBuilder();
    }

    /**
     * Get the pipeline pretty string.
     * @return String
     */
    public String getString() {
        return sb.toString();
    }

    @Override
    public void visitStart(Pipeline pipeline) {
        sb.append(pipeline.name).append(" {\n");
    }

    @Override
    public void visitStagesStart(Stages stages, int depth) {
        sb.append(indent(depth));
        if (stages instanceof Parallel) {
            sb.append("parallel {\n");
        } else {
            sb.append("stage");
            if (stages.name != null && !stages.name.isEmpty()) {
                sb.append("('").append(stages.name).append("')");
            }
            sb.append(" {\n");
        }
    }

    @Override
    public void visitStepsStart(Steps steps, int depth) {
        sb.append(indent(depth)).append("steps {\n");
    }

    @Override
    public void visitStep(Step step, int depth) {
        sb.append(indent(depth)).append(step.name).append(" ").append(step.args);
        if (!step.isIncluded(excludeSyntheticSteps, excludeMetaSteps)) {
            sb.append(" // filtered");
        }
        sb.append("\n");
    }

    @Override
    public void visitStepsEnd(Steps steps, int depth) {
        sb.append(indent(depth)).append("}\n");
    }

    @Override
    public void visitStagesEnd(Stages stages, int depth) {
        sb.append(indent(depth)).append("}\n");
    }

    @Override
    public void visitEnd(Pipeline pipeline) {
        sb.append("}\n");
    }

    private static String indent(int depth) {
        String indent = "";
        for (int i=0 ; i < depth ; i++) {
            indent += INDENT;
        }
        return indent;
    }
}
