package io.helidon.build.publisher.frontend;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.RetrySchema;
import io.helidon.media.common.ReadableByteChannelPublisher;
import io.helidon.webserver.ResponseHeaders;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Service that implements the endpoint used by the UI.
 */
final class FrontendService implements Service {

    private final Path storagePath;

    /**
     * Create a new front-end service.
     * @param path storage path
     */
    FrontendService(String path) {
        Path dirPath = FileSystems.getDefault().getPath(path);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectory(dirPath);
            } catch (IOException ex) {
                throw new IllegalStateException("Error initializing storage directory", ex);
            }
        }
        this.storagePath = dirPath;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::listPipelines)
             .get("/{pipelineId}", this::getPipeline)
             .get("/{pipelineId}/output/{stepId}", this::getOutput);
    }

    private void listPipelines(ServerRequest req, ServerResponse res) {
        int page = req.queryParams().first("page").map(Integer::parseInt).orElse(1);
        int numitems = req.queryParams().first("numitems").map(Integer::parseInt).orElse(20);
        ResponseHeaders headers = res.headers();
        headers.contentType(MediaType.APPLICATION_JSON);
        headers.put("Access-Control-Allow-Origin", "*");
        res.send(MockHelper.mockPipelines(page, numitems));
    }

    private void getPipeline(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        ResponseHeaders headers = res.headers();
        headers.contentType(MediaType.APPLICATION_JSON);
        headers.put("Access-Control-Allow-Origin", "*");
        if ("non-existent".equals(pipelineId)) {
            res.status(404).send();
        } else {
            res.send(MockHelper.mockPipeline(pipelineId));
        }
    }

    private void getDescriptor(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        Path filePath = storagePath.resolve(pipelineId + "/pipeline.json");
        if (Files.exists(filePath)) {
            res.status(404).send();
            return;
        }
        try {
            long size = Files.size(filePath);
            FileChannel fc = FileChannel.open(filePath, StandardOpenOption.READ);
            RetrySchema retrySchema = RetrySchema.linear(0, 10, 250);
            Publisher<DataChunk> publisher = new LimitPublisher(new ReadableByteChannelPublisher(fc, retrySchema), size);
            res.headers().contentType(MediaType.APPLICATION_JSON);
            res.headers().contentLength(size);
            res.send(publisher);
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private void getOutput(ServerRequest req, ServerResponse res) {
        String pipelineId = req.path().param("pipelineId");
        String stepIdParam = req.path().param("stepId");
        if (pipelineId == null || pipelineId.isEmpty() || stepIdParam == null || stepIdParam.isEmpty()) {
            res.status(Http.Status.BAD_REQUEST_400).send();
            return;
        }
        int stepId, nlines;
        long position;
        try {
            stepId = Integer.valueOf(stepIdParam);
            nlines = req.queryParams().first("nlines").map(Integer::valueOf).orElse(0);
            position = req.queryParams().first("position").map(Long::parseLong).orElse(0L);
        } catch( NumberFormatException ex) {
            res.status(400).send();
            return;
        }
        Path filePath = storagePath.resolve(pipelineId + "/step-" + stepId + ".log");
        if (Files.exists(filePath)) {
            res.status(404).send();
            return;
        }
        try {
            long size = Files.size(filePath);
            position = findLinesPosition(filePath, position, size - 1, nlines);
            FileChannel fc = FileChannel.open(filePath, StandardOpenOption.READ);
            fc.position(position);
            RetrySchema retrySchema = RetrySchema.linear(0, 10, 250);
            Publisher<DataChunk> publisher = new LimitPublisher(new ReadableByteChannelPublisher(fc, retrySchema), size);
            res.headers().contentType(MediaType.TEXT_PLAIN);
            res.headers().contentLength(size - position);
            res.send(publisher);
        } catch (IOException ex) {
            req.next(ex);
        }
    }

    private long findLinesPosition(Path filePath, long min, long pos, int nlines) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
        while(pos >= min && nlines > 0) {
            raf.seek(min);
            int readByte = raf.readByte();
            if (readByte == 0xA) {
                nlines--;
            }
            pos--;
        }
        return pos;
    }

    private static final class LimitPublisher implements Publisher<DataChunk> {

        private final Publisher<DataChunk> delegate;
        private final long limit;

        LimitPublisher(Publisher<DataChunk> delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public void subscribe(Subscriber<? super DataChunk> subscriber) {
            delegate.subscribe(new LimitSubscriber(subscriber, limit));
        }
    }

    private static final class LimitSubscriber implements Subscriber<DataChunk> {

        private final Subscriber<? super DataChunk> delegate;
        private final long limit;
        private long current;
        private boolean completed;

        LimitSubscriber(Subscriber<? super DataChunk> delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(DataChunk item) {
            ByteBuffer data = item.data();
            int itemLength = data.remaining();
            if (current < limit) {
                long currentLimit = limit - current;
                if (itemLength > currentLimit) {
                    data.position((int) (itemLength - currentLimit));
                    current = limit;
                    delegate.onNext(item);
                    delegate.onComplete();
                    completed = true;
                } else {
                    current += itemLength;
                    delegate.onNext(item);
                }
            }
        }

        @Override
        public void onError(Throwable ex) {
            delegate.onError(ex);
        }

        @Override
        public void onComplete() {
            if (!completed) {
                delegate.onComplete();
            }
        }
    }
}
