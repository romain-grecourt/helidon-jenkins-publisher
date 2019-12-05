package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests model.
 */
public final class Tests {

    final int passed;
    final int failed;
    final int skipped;
    final List<TestItem> items;

    private Tests(int passed, int failed, int skipped, List<TestItem> items) {
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
        this.items = items;
    }

    @JsonProperty
    public int passed() {
        return passed;
    }

    @JsonProperty
    public int failed() {
        return failed;
    }

    @JsonProperty
    public int skipped() {
        return skipped;
    }

    @JsonProperty
    public List<TestItem> items() {
        return items;
    }

    /**
     * Create a new builder.
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder implements io.helidon.common.Builder<Tests> {

        private int passed;
        private int failed;
        private int skipped;
        private final List<TestItem> items = new ArrayList<>();

        /**
         * Set the passed.
         * @param passed the passed
         * @return this builder instance
         */
        public Builder passed(int passed) {
            this.passed = passed;
            return this;
        }

        /**
         * Set the failed.
         * @param failed the failed
         * @return this builder instance
         */
        public Builder failed(int failed) {
            this.failed = failed;
            return this;
        }

        /**
         * Set the skipped.
         * @param skipped the skipped
         * @return this builder instance
         */
        public Builder skipped(int skipped) {
            this.skipped = skipped;
            return this;
        }

        /**
         * Add an item.
         * @param item the item
         * @return this builder instance
         */
        public Builder item(TestItem item) {
            this.items.add(item);
            return this;
        }

        @Override
        public Tests build() {
            return new Tests(passed, failed, skipped, items);
        }
    }
}
