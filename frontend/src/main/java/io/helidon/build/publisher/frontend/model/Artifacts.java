package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Artifacts model.
 */
public final class Artifacts {

    final int count;
    final List<ArtifactItem> items;

    private Artifacts(int count, List<ArtifactItem> items) {
        this.count = count;
        this.items = items;
    }

    @JsonProperty
    public int count() {
        return count;
    }

    @JsonProperty
    public List<ArtifactItem> items() {
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
    public static final class Builder implements io.helidon.common.Builder<Artifacts> {

        private int count;
        private final List<ArtifactItem.Builder> itemBuilders = new ArrayList<>();

        /**
         * Set the count.
         * @param count the count
         * @return this builder instance
         */
        public Builder count(int count) {
            this.count = count;
            return this;
        }

        /**
         * Add an item.
         * @param itemBuilder the item
         * @return this builder instance
         */
        public Builder item(ArtifactItem.Builder itemBuilder) {
            this.itemBuilders.add(itemBuilder);
            return this;
        }

        @Override
        public Artifacts build() {
            List<ArtifactItem> items = new ArrayList<>();
            for (ArtifactItem.Builder itemBuilder : itemBuilders) {
                items.add(itemBuilder.build());
            }
            return new Artifacts(count, items);
        }
    }
}
