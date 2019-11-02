package io.helidon.jenkins.publisher;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
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
    private FlowStatus status;
    private final long startTime;
    private long endTime;

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
        this.startTime = TimingAction.getStartTime(node);
        this.endTime = -1;
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

    /**
     * Get the previous step in the parent stage.
     *
     * @return FlowStep or {@code null} if there is no previous step.
     */
    FlowStep previous() {
        List<FlowStep> sequence = stage.steps();
        if (parentIndex > 0 && sequence.size() >= parentIndex) {
            return sequence.get(parentIndex - 1);
        }
        return null;
    }

    /**
     * Get the start time in milliseconds for this step.
     * @return long
     */
    long startTime() {
        return startTime;
    }

    /**
     * Get the end time in milliseconds for this step.
     * @return long
     */
    long endTime() {
        if (endTime < 0 && status != null && status.state == FlowStatus.FlowState.FINISHED) {
            List<FlowStep> sequence = stage.steps();
            if (parentIndex + 1 < sequence.size()) {
                // end time is the start time of the next step
                endTime = sequence.get(parentIndex + 1).startTime;
            } else {
                // end time is the end time of the state
                endTime = stage.endTime();
            }
        }
        return endTime;
    }

    /**
     * Get the status for this step.
     * @return FlowStatus
     */
    FlowStatus status() {
        if (status == null || status.state != FlowStatus.FlowState.FINISHED) {
            status = new FlowStatus(node);
        }
        return status;
    }

    @Override
    public String toString() {
        return FlowStep.class.getSimpleName() + "{"
                + " id=" + id
                + ", stage=" + stage.id
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
