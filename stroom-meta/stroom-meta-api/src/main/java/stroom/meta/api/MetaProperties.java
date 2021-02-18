package stroom.meta.api;

import stroom.meta.shared.Meta;

public class MetaProperties {

    private final Long parentId;
    private final String typeName;
    private final String feedName;
    private final String processorUuid;
    private final String pipelineUuid;
    private final Long createMs;
    private final Long effectiveMs;
    private final Long statusMs;

    public MetaProperties(final Long parentId,
                          final String typeName,
                          final String feedName,
                          final String processorUuid,
                          final String pipelineUuid,
                          final Long createMs,
                          final Long effectiveMs,
                          final Long statusMs) {
        this.parentId = parentId;
        this.typeName = typeName;
        this.feedName = feedName;
        this.processorUuid = processorUuid;
        this.pipelineUuid = pipelineUuid;
        this.createMs = createMs;
        this.effectiveMs = effectiveMs;
        this.statusMs = statusMs;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getFeedName() {
        return feedName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getProcessorUuid() {
        return processorUuid;
    }

    public String getPipelineUuid() {
        return pipelineUuid;
    }

    public Long getCreateMs() {
        return createMs;
    }

    public Long getEffectiveMs() {
        return effectiveMs;
    }

    public Long getStatusMs() {
        return statusMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static final class Builder {

        private Long parentId;
        private String typeName;
        private String feedName;
        private String processorUuid;
        private String pipelineUuid;
        private Long createMs;
        private Long effectiveMs;
        private Long statusMs;

        private Builder() {
        }

        private Builder(final MetaProperties metaProperties) {
            parentId = metaProperties.parentId;
            typeName = metaProperties.typeName;
            feedName = metaProperties.feedName;
            processorUuid = metaProperties.processorUuid;
            pipelineUuid = metaProperties.pipelineUuid;
            createMs = metaProperties.createMs;
            effectiveMs = metaProperties.effectiveMs;
            statusMs = metaProperties.statusMs;
        }

        /**
         * This is a utility method to perform common parent association behaviour, e.g.
         * setting the effective time from the parent.
         *
         * @param parent The parent to set.
         * @return The builder.
         */
        public Builder parent(final Meta parent) {
            // Set effective time from the parent data.
            if (parent != null) {
                parentId = parent.getId();
                if (effectiveMs == null) {
                    if (parent.getEffectiveMs() != null) {
                        effectiveMs = parent.getEffectiveMs();
                    } else {
                        effectiveMs = parent.getCreateMs();
                    }
                }
            } else {
                parentId = null;
            }

            return this;
        }

        public Builder parentId(final Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder feedName(final String feedName) {
            this.feedName = feedName;
            return this;
        }

        public Builder typeName(final String typeName) {
            this.typeName = typeName;
            return this;
        }

        public Builder processorUuid(final String processorUuid) {
            this.processorUuid = processorUuid;
            return this;
        }

        public Builder pipelineUuid(final String pipelineUuid) {
            this.pipelineUuid = pipelineUuid;
            return this;
        }

        public Builder createMs(final Long createMs) {
            this.createMs = createMs;
            return this;
        }

        public Builder effectiveMs(final Long effectiveMs) {
            this.effectiveMs = effectiveMs;
            return this;
        }

        public Builder statusMs(final Long statusMs) {
            this.statusMs = statusMs;
            return this;
        }

        public MetaProperties build() {
            // When were we created
            long timeMs = createMs != null
                    ? createMs
                    : System.currentTimeMillis();

            return new MetaProperties(
                    parentId,
                    typeName,
                    feedName,
                    processorUuid,
                    pipelineUuid,
                    timeMs,
                    effectiveMs != null
                            ? effectiveMs
                            : timeMs, // Ensure an effective time
                    statusMs);
        }
    }
}
