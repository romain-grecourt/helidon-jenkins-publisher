package io.helidon.jenkins.publisher;

import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

/**
 * Flow step.
 */
final class FlowStep {

    private final String id;
    private final String name;
    private final String args;
    private final boolean declared;
    private final String signature;
    private final boolean meta;
    private final StepAtomNode node;

    /**
     * Create a new flow step instance.
     * @param node step node
     * @param signatures declared step signatures
     */
    FlowStep(StepAtomNode node, FlowStepSignatures signatures) {
        this.node = node;
        this.id = node.getId();
        this.name = node.getDisplayFunctionName();
        this.args = ArgumentsAction.getStepArgumentsAsString(node);
        this.signature = FlowStepSignatures.createSignature(name, args, node.getEnclosingBlocks());
        this.declared = signatures.contains(signature);
        this.meta = node.getDescriptor().isMetaStep();
    }

    /**
     * Get the underlying node.
     *
     * @return StepAtomNode
     */
    StepAtomNode getNode() {
        return node;
    }

    /**
     * Get the step unique id.
     * @return String
     */
    String getId() {
        return id;
    }

    /**
     * Get the step name.
     * @return String
     */
    String getName() {
        return name;
    }

    /**
     * Get the step arguments.
     * @return String
     */
    String getArgs() {
        return args;
    }

    /**
     * Indicate if this step is declared.
     * @return {@code true} if declared, {@code false} otherwise
     */
    boolean isDeclared() {
        return declared;
    }

    /**
     * Indicate if this step is a meta step.
     * @return {@code true} if meta, {@code false} otherwise
     */
    boolean isMeta() {
        return meta;
    }

    @Override
    public String toString() {
        return FlowStep.class.getSimpleName() + "{ "
                + "id=" + id
                + ", stepName=" + name
                + ", args=" + args
                + ", declared=" + declared
                + ", meta=" + meta
                + ", signature=" + signature
                + "}";
    }
}
