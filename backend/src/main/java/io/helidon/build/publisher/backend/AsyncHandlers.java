package io.helidon.build.publisher.backend;

import io.helidon.common.http.Http;
import java.util.function.Consumer;
import java.util.function.Function;

import io.helidon.webserver.BadRequestException;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

/**
 * Asynchronous handlers.
 */
final class AsyncHandlers {

    private AsyncHandlers() {
    }

    /**
     * Create a new handler to return a valid response.
     * @param response response object
     * @return handler
     */
    static Consumer<Object> status(ServerResponse response, Http.Status status) {
        return new StatusHandler(response, status);
    }

    /**
     * Create a new handler to process an error.
     * @param request request object
     * @return handler
     */
    static Function<Throwable, Void> error(ServerRequest request) {
        return new ErrorHandler(request);
    }

    private static final class StatusHandler implements Consumer<Object> {

        private final ServerResponse response;
        private final Http.Status status;

        StatusHandler(ServerResponse response, Http.Status status) {
            this.response = response;
            this.status = status;
        }

        @Override
        public void accept(Object v) {
            response.status(status).send();
        }
    }

    private static final class ErrorHandler implements Function<Throwable, Void> {

        private final ServerRequest request;

        ErrorHandler(ServerRequest request) {
            this.request = request;
        }

        @Override
        public Void apply(Throwable ex) {
            if (ex instanceof IllegalStateException
                    || ex instanceof IllegalArgumentException) {
                request.next(new BadRequestException(ex.getMessage(), ex));
            } else {
                request.next(ex);
            }
            return null;
        }
    }
}
