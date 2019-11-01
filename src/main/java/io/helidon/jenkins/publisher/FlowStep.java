package io.helidon.jenkins.publisher;

import java.util.Base64;
import java.util.Objects;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;

/**
 * Flow step.
 */
final class FlowStep {

    private final String id;
    private final String name;
    private final String args;
    private final String path;
    private final boolean meta;
    private final int parentIndex;
    private final FlowStage.Steps stage;
    private final StepAtomNode node;
    private final boolean declared;

    /**
     * Create a new flow step instance.
     * @param node step node
     * @param signatures declared step signatures
     */
    FlowStep(StepAtomNode node, FlowStage.Steps stage, FlowStepSignatures signatures) {
        Objects.requireNonNull(stage, "stage is null");
        this.node = node;
        this.stage = stage;
        this.id = node.getId();
        this.name = node.getDisplayFunctionName();
        String stepArgs = ArgumentsAction.getStepArgumentsAsString(node);
        this.args = stepArgs != null ? stepArgs : "";
        this.meta = node.getDescriptor().isMetaStep();
        String p = stage.path() + "step(" + name + ")";
        if (!args.isEmpty()) {
            p += "=" + new String(Base64.getEncoder().encode(args.getBytes()));
        }
        this.path = p;
        this.parentIndex = stage.index();
        this.declared = signatures.contains(this.path);
    }

    /**
     * Indicate if this step is explicitly declared.
     * @return {@code true} if declared, {@code false} otherwise
     */
    boolean declared() {
        return declared;
    }

    /**
     * Get the corresponding node.
     * @return StepAtomNode
     */
    StepAtomNode node() {
        return node;
    }

    /**
     * Get the index in the stage sequence.
     * @return int
     */
    int parentIndex() {
        return parentIndex;
    }

    /**
     * Get the parent stage.
     * @return FlowStage.StepsSequence
     */
    FlowStage.Steps stage() {
        return stage;
    }

    /**
     * Get the step unique id.
     * @return String
     */
    String id() {
        return id;
    }

    /**
     * Get the step name.
     * @return String
     */
    String name() {
        return name;
    }

    /**
     * Get the step arguments.
     * @return String
     */
    String args() {
        return args;
    }

    /**
     * Indicate if this step is a meta step.
     * @return {@code true} if meta, {@code false} otherwise
     */
    boolean meta() {
        return meta;
    }

    /**
     * Get the step path.
     * @return String
     */
    String path() {
        return path;
    }

    @Override
    public String toString() {
        return FlowStep.class.getSimpleName() + "{ "
                + "id=" + id
                + ", stepName=" + name
                + ", args=" + args
                + ", meta=" + meta
                + ", path=" + path
                + "}";
    }

    /**
     * Pretty print this step.
     * @param indent indentation
     * @return String
     */
    String prettyPrint(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(name);
        if (!args.isEmpty()) {
            String[] argsLines = args.split("\\r?\\n");
            sb.append(" ");
            for (String line : argsLines) {
                String argPreview = line.trim();
                if (!argPreview.isEmpty()) {
                    sb.append(argPreview);
                    break;
                }
            }
            if (argsLines.length > 1) {
                sb.append(" [...]");
            }
        }
        return sb.toString();
    }
}
