package io.helidon.build.publisher.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.helidon.build.publisher.model.events.StepCompletedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A step node.
 */
@JsonPropertyOrder({"id", "type", "name", "args", "state", "result", "startTime", "endTime"})
public final class Step extends Node {

    final String args;
    final boolean meta;
    final boolean declared;

    /**
     * Create a new step instance.
     *
     * @param parent the parent stage that this step is part of
     * @param name the step name, must be non {@code null} and non empty
     * @param args the step arguments
     * @param meta {@code true} if this step is a meta step
     * @param declared {@code true} if this step is declared
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if type, status or timings is {@code null}
     */
    public Step(Steps parent, String name, String args, boolean meta, boolean declared, Status status, Timings timings) {
        super(parent, name, createPath(parent, name, args), status, timings);
        this.args = args != null ? args : "";
        this.meta = meta;
        this.declared = declared;
    }

    public Step(String id, Steps parent, String name, String args, boolean meta, boolean declared,
            Status status, Timings timings) {

        super(parent, id, name, createPath(parent, name, args), status, timings);
        this.args = args != null ? args : "";
        this.meta = meta;
        this.declared = declared;
    }

    /**
     * Get the step arguments.
     *
     * @return String
     */
    @JsonProperty
    public String args() {
        return args;
    }

    @JsonProperty
    @Override
    public String type() {
        return "STEP";
    }

    /**
     * Get the meta flag.
     *
     * @return {@code boolean}
     */
    public boolean meta() {
        return meta;
    }

    /**
     * Get the declared flag.
     *
     * @return {@code boolean}
     */
    public boolean declared() {
        return declared;
    }

    @Override
    public Step previous() {
        int previousIndex = index() - 1;
        if (previousIndex >= 0) {
            return ((Steps) parent).children.get(previousIndex);
        }
        return null;
    }

    @Override
    public Step next() {
        if (parent != null) {
            List<Step> sequence = ((Steps) parent).children;
            int nextIndex = index() + 1;
            if (nextIndex > 0 && nextIndex < sequence.size()) {
                return sequence.get(nextIndex);
            }
        }
        return null;
    }

    @Override
    public int index() {
        Iterator<Step> it = ((Steps) parent).children.iterator();
        for (int i = 0; it.hasNext(); i++) {
            if (it.next().id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean head() {
        if (parent != null) {
            List<Stage> sequence = ((Stages) parent).children;
            return index() == sequence.size() - 1;
        }
        return true;
    }

    public boolean isIncluded(boolean excludeSyntheticSteps, boolean excludeMetaSteps) {
        return ((excludeSyntheticSteps && declared) || (!excludeSyntheticSteps && declared))
                && ((excludeMetaSteps && !meta) || (!excludeMetaSteps && meta));
    }

    @Override
    public void fireCreated() {
        Step previous = previous();
        if (previous != null) {
            previous.fireCompleted();
        }
        fireEvent(new StepCreatedEvent(info.id, id, Objects.requireNonNull(parent, "parent is null!").id, index(), name,
                timings.startTime, args));
    }

    @Override
    public void fireCompleted() {
        status.refresh();
        timings.refresh();
        status.state = Status.State.FINISHED;
        if (timings.endTime == 0) {
            timings.endTime = System.currentTimeMillis();
        }
        fireEvent(new StepCompletedEvent(info.id, id, status.result, timings.duration()));
    }

    /**
     * Create a step path.
     *
     * @param prefix path prefix
     * @param name step name
     * @param args step arguments
     * @return String
     */
    public static String createPath(String prefix, String name, String args) {
        String s = prefix + "step(" + name + ")";
        if (!args.isEmpty()) {
            s += "=" + new String(Base64.getEncoder().encode(args.getBytes()));
        }
        return s;
    }

    private static String createPath(Node parent, String name, String args) {
        Objects.requireNonNull(parent, "parent is null");
        String s = parent.path + "step(" + name + ")";
        if (!args.isEmpty()) {
            try {
                s += "=" + URLEncoder.encode(args, "UTF-8").replace("\\+", "%20");
            } catch (UnsupportedEncodingException ex) {
            }
        }
        return s;
    }
}
