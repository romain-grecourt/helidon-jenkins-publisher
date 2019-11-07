package io.helidon.jenkins.publisher.plugin;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;

/**
 * Flow step.
 */
final class FlowStep {

    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(new HashMap<>());

    private final FlowRun flowRun;
    private final String id;
    private final String name;
    private final String args;
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
     * @param flowRun the flow run that this step belongs to
     * @param node step node
     * @param stage the parent stage that this step is part of
     * @param signatures declared step signatures
     */
    FlowStep(FlowRun flowRun, StepAtomNode node, FlowStage.Steps stage, FlowStepSignatures signatures) {
        Objects.requireNonNull(flowRun, "flowRun is null");
        Objects.requireNonNull(stage, "stage is null");
        this.node = node;
        this.flowRun = flowRun;
        this.stage = stage;
        this.id = node.getId();
        this.name = node.getDisplayFunctionName();
        String stepArgs = ArgumentsAction.getStepArgumentsAsString(node);
        this.args = stepArgs != null ? stepArgs : "";
        this.meta = node.getDescriptor().isMetaStep();
        String stageUri = stage.uri();
        String signature = stageUri + "step[" + name + "]";
        if (!args.isEmpty()) {
            signature += "/" + new String(Base64.getEncoder().encode(args.getBytes()));
        }
        this.declared = signatures.contains(signature);
        this.parentIndex = stage.index();
        this.startTime = TimingAction.getStartTime(node);
        this.endTime = -1;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.flowRun);
        hash = 89 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FlowStep other = (FlowStep) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return Objects.equals(this.flowRun, other.flowRun);
    }

    /**
     * Get the identifier for that this step belongs to.
     * @return FlowRun
     */
    FlowRun flowRun() {
        return flowRun;
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

    /**
     * Test if this step should be included for the given flags.
     * @param declaredOnly if {@code true} this step is included only if declared
     * @param skipMeta if {@code true} this step is not included if meta
     * @return {@code true} if included, {@code false} if excluded
     */
    boolean isIncluded(boolean declaredOnly, boolean skipMeta) {
        return ((declaredOnly && declared) || !declaredOnly)
                && ((skipMeta && !meta) || !skipMeta);
    }

    @Override
    public String toString() {
        return FlowStep.class.getSimpleName() + "{"
                + " id=" + id
                + ", stage=" + stage.id
                + ", flowRun=" + flowRun.desc()
                + " }";
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

    /**
     * Build a JSON object representation of this event.
     * @return JsonObject
     */
    JsonObject toJson() {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("id", id)
                .add("stageId", stage.id)
                .add("name", name)
                .add("args", args)
                .add("status", status().toJson())
                .add("startTime", startTime)
                .add("endTime", endTime())
                .build();
    }
}
