package io.helidon.build.publisher.plugin;

import hudson.model.Run;
import io.helidon.build.publisher.model.Status;

/**
 * {@link Status} implementation that can compute the state and result from the run.
 */
final class GlobalStatus extends Status {

    private final Run run;

    GlobalStatus(Run run) {
        super(run.isBuilding() ? Status.State.RUNNING : Status.State.FINISHED);
        this.run = run;
    }

    @Override
    protected void refresh() {
        result = Helper.convertResult(run.getResult());
    }
}
