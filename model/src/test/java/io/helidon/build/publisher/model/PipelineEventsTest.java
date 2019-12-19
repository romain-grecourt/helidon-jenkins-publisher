package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Status.State;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineEvents;
import io.helidon.build.publisher.model.events.StageCompletedEvent;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCompletedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.StepOutputEvent;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test JSON with {@link PipelineEvents}.
 */
public class PipelineEventsTest {

    @Test
    public void testJSON() throws IOException {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = PipelineInfo.builder()
                .id("abcdefgh")
                .title("testJob")
                .repositoryUrl("https://github.com/john_doe/repo.git")
                .headRef("master")
                .commit("123456789")
                .status(new Status(State.RUNNING))
                .timings(new Timings(now()))
                .build();
        events.add(new PipelineCreatedEvent(info));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", now(), "STEPS"));
        events.add(new StepCreatedEvent(info.id, "2", "1", 0, "sh", now(), "echo foo"));
        events.add(new StepOutputEvent(info.id, "2"));
        events.add(new StepCompletedEvent(info.id, "2", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "1", Result.SUCCESS, now()));
        events.add(new StageCompletedEvent(info.id, "0", Result.SUCCESS, now()));
        events.add(new PipelineCompletedEvent(info.id, Result.SUCCESS, now()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JacksonSupport.write(baos, new PipelineEvents(events));
        System.out.println(new String(baos.toByteArray()));

        PipelineEvents fromJson = JacksonSupport.read(new ByteArrayInputStream(baos.toByteArray()), PipelineEvents.class);
        for (PipelineEvent evt : fromJson.events()) {
            assertThat(events, hasItem(evt));
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
