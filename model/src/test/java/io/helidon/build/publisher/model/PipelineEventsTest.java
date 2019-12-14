package io.helidon.build.publisher.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.publisher.model.Status.Result;
import io.helidon.build.publisher.model.Stage.StageType;
import io.helidon.build.publisher.model.events.PipelineCompletedEvent;
import io.helidon.build.publisher.model.events.PipelineCreatedEvent;
import io.helidon.build.publisher.model.events.PipelineEvent;
import io.helidon.build.publisher.model.events.PipelineEvents;
import io.helidon.build.publisher.model.events.StageCompletedEvent;
import io.helidon.build.publisher.model.events.StageCreatedEvent;
import io.helidon.build.publisher.model.events.StepCompletedEvent;
import io.helidon.build.publisher.model.events.StepCreatedEvent;
import io.helidon.build.publisher.model.events.StepOutputEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test JSON with {@link PipelineEvents}.
 */
public class PipelineEventsTest {

    private static final String REPO_URL = "https://github.com/john_doe/repo.git";
    private static final long TIMESTAMP = System.currentTimeMillis();

    @Test
    public void testJSON() throws IOException {
        List<PipelineEvent> events = new LinkedList<>();
        PipelineInfo info = new PipelineInfo("abcdefgh", "testJob", REPO_URL, "master", "123456789");
        events.add(new PipelineCreatedEvent(info, TIMESTAMP));
        events.add(new StageCreatedEvent(info.id, "1", "0", 0, "build", TIMESTAMP, StageType.STEPS));
        events.add(new StepCreatedEvent(info.id, "2", "1", 0, "sh", TIMESTAMP, "echo foo"));
        events.add(new StepOutputEvent(info.id, "2"));
        events.add(new StepCompletedEvent(info.id, "2", Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(info.id, "1", Result.SUCCESS, TIMESTAMP));
        events.add(new StageCompletedEvent(info.id, "0", Result.SUCCESS, TIMESTAMP));
        events.add(new PipelineCompletedEvent(info.id, Result.SUCCESS, TIMESTAMP));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(baos, new PipelineEvents(events));

        // pretty print
        Object json = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Object.class);
        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println(indented);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PipelineEvents fromJson = mapper.readValue(bais, PipelineEvents.class);
        for (PipelineEvent evt : fromJson.events()) {
            assertThat(events, hasItem(evt));
        }
    }
}
