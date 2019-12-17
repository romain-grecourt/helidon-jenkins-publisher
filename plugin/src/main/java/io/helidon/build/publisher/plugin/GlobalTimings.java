package io.helidon.build.publisher.plugin;

import hudson.model.Run;
import io.helidon.build.publisher.model.Timings;

/**
 * {@link Timings} implementation that can get the end time from the run.
 */
final class GlobalTimings extends Timings {

    private final Run run;

    /**
     * Create a new timing with start time derived from the given {@link FlowNode}.
     *
     * @param source
     */
    GlobalTimings(Run run) {
        super(run.getStartTimeInMillis());
        this.run = run;
    }

    @Override
    protected void refresh() {
        if (super.endTime == 0 && !run.isBuilding()) {
            super.endTime = run.getDuration();
        }
    }
}
