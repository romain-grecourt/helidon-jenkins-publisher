package io.helidon.build.publisher.frontend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Artifact directory item model.
 */
public final class ArtifactDirItem extends ArtifactItem {

    final List<ArtifactItem> children;

    private ArtifactDirItem(String name, String path, List<ArtifactItem> children) {
        super(name, path);
        this.children = children;
    }

    @JsonProperty
    public List<ArtifactItem> children() {
        return children;
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
    public static final class Builder extends ArtifactItem.Builder {

        protected List<ArtifactItem.Builder> childrenBuilder = new ArrayList<>();

        /**
         * Add a child.
         * @param childBuilder the child
         * @return this builder instance
         */
        public Builder child(ArtifactItem.Builder childBuilder) {
            if (path == null) {
                path = "/" + name;
            }
            childBuilder.path = path + "/" + childBuilder.name;
            this.childrenBuilder.add(childBuilder);
            return this;
        }

        @Override
        public ArtifactDirItem build() {
            List<ArtifactItem> children = new ArrayList<>();
            for(ArtifactItem.Builder childBuilder : childrenBuilder) {
                children.add(childBuilder.build());
            }
            return new ArtifactDirItem(name, path == null ? ("/" + name) : path, children);
        }
    }
}
