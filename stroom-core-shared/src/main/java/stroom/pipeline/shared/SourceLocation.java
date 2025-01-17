/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline.shared;

import stroom.util.shared.DataRange;
import stroom.util.shared.TextRange;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * Defines the location of some (typically character) data
 */
@JsonInclude(Include.NON_NULL)
public class SourceLocation {

    public static final int MAX_ERRORS_PER_PAGE = 100;

    @JsonProperty
    private final long id; // The meta ID
    @JsonProperty
    private final String childType; // null for actual data, else non null (e.g. context/meta)
    @JsonProperty
    private final long partNo; // For multipart data only, aka streamNo, 0 for non multi-part data, zero based
    // TODO @AT Change to an OffsetRange to support error segments
    // TODO @AT This was a bad name choice. In segmented data the header and footer occupy the first and
    //  last segments technically the first rec is at seg 1 (zero based). Probably should have called it
    //  recordNo like in the stepper to avoid the confusion. Here we treat it like the record no, ignoring the
    //  header segment.
    @JsonProperty
    private final long segmentNo; // optional for segmented data only (segment aka record), zero based
    //    private final OffsetRange segmentNoRange;
    @JsonProperty
    private final DataRange dataRange; // The optional specified range of the character data which may be a subset
    @JsonProperty
    private final TextRange highlight; // The optional highlighted range of the character data which may be a subset
    @JsonProperty
    private final boolean truncateToWholeLines;

    @JsonCreator
    public SourceLocation(@JsonProperty("id") final long id,
                          @JsonProperty("childType") final String childType,
                          @JsonProperty("partNo") final long partNo,
                          @JsonProperty("segmentNo") final long segmentNo,
                          @JsonProperty("dataRange") final DataRange dataRange,
                          @JsonProperty("highlight") final TextRange highlight,
                          @JsonProperty("truncateToWholeLines") final boolean truncateToWholeLines) {
        this.id = id;
        this.childType = childType;
        this.partNo = partNo;
        this.segmentNo = segmentNo;
        this.dataRange = dataRange;
        this.highlight = highlight;
        this.truncateToWholeLines = truncateToWholeLines;
    }

    private SourceLocation(final Builder builder) {
        id = builder.id;
        partNo = builder.partNo;
        childType = builder.childType;
        segmentNo = builder.segmentNo;
        dataRange = builder.dataRange;
        highlight = builder.highlight;
        truncateToWholeLines = builder.truncateToWholeLines;
    }

    public static Builder builder(final long id) {
        return new Builder(id);
    }

    public long getId() {
        return id;
    }

    /**
     * @return The type of the child stream that is being requested.
     */
    public String getChildType() {
        return childType;
    }

    @JsonIgnore
    public Optional<String> getOptChildType() {
        return Optional.ofNullable(childType);
    }

    /**
     * @return Part number in the stream (aka streamNo), zero based. Non multi-part streams would have
     * a single part with number zero.
     */
    public long getPartNo() {
        return partNo;
    }

    /**
     * @return The segment number (AKA record number), zero based
     */
    public long getSegmentNo() {
        return segmentNo;
    }

    @JsonIgnore
    public OptionalLong getOptSegmentNo() {
        return OptionalLong.of(segmentNo);
    }

    /**
     * @return The range of data specified, may be null
     */
    public DataRange getDataRange() {
        return dataRange;
    }

    @JsonIgnore
    public Optional<DataRange> getOptDataRange() {
        return Optional.ofNullable(dataRange);
    }

    /**
     * @return The range of data that is highlighted, may be null.
     */
    public TextRange getHighlight() {
        return highlight;
    }

    @JsonIgnore
    public Optional<TextRange> getOptHighlight() {
        return Optional.ofNullable(highlight);
    }

    public boolean isTruncateToWholeLines() {
        return truncateToWholeLines;
    }

    public boolean isSameSource(final SourceLocation other) {
        if (other == null) {
            return false;
        } else {
            return this.id == other.id
                    && this.partNo == other.partNo
                    && Objects.equals(this.childType, other.childType);
        }
    }

    public boolean isSameLocation(final SourceLocation other) {
        if (other == null) {
            return false;
        } else {
            return this.isSameSource(other)
                    && this.segmentNo == other.segmentNo
                    && Objects.equals(this.dataRange, other.dataRange);
        }
    }

    /**
     * @return The identifier i.e. strm:part:segment (one based for human use)
     */
    @JsonIgnore
    public String getIdentifierString() {
        // Convert to one-based
        return id + ":" + (partNo + 1) + ":" + (segmentNo + 1);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SourceLocation that = (SourceLocation) o;
        return id == that.id &&
                partNo == that.partNo &&
                segmentNo == that.segmentNo &&
                truncateToWholeLines == that.truncateToWholeLines &&
                Objects.equals(childType, that.childType) &&
                Objects.equals(dataRange, that.dataRange) &&
                Objects.equals(highlight, that.highlight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, childType, partNo, segmentNo, dataRange, highlight, truncateToWholeLines);
    }

    @Override
    public String toString() {
        return "SourceLocation{" +
                "id=" + id +
                ", childType='" + childType + '\'' +
                ", partNo=" + partNo +
                ", segmentNo=" + segmentNo +
                ", dataRange=" + dataRange +
                ", highlight=" + highlight +
                ", truncateToWholeLines=" + truncateToWholeLines +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static final class Builder {

        private long id;
        private long partNo = 0; // Non multipart data has segment no of zero by default, zero based
        private String childType;
        private long segmentNo = 0; // Non-segmented data has no segment no., zero based
        private DataRange dataRange;
        private TextRange highlight;
        private boolean truncateToWholeLines = false;

        private Builder(final long id) {
            this.id = id;
        }

        private Builder() {
        }

        private Builder(final SourceLocation currentSourceLocation) {
            this.id = currentSourceLocation.id;
            this.partNo = currentSourceLocation.partNo;
            this.childType = currentSourceLocation.childType;
            this.segmentNo = currentSourceLocation.segmentNo;
            this.dataRange = currentSourceLocation.dataRange;
            this.highlight = currentSourceLocation.highlight;
            this.truncateToWholeLines = currentSourceLocation.truncateToWholeLines;
        }

        /**
         * Zero based
         */
        public Builder withPartNo(final Long partNo) {
            if (partNo != null) {
                this.partNo = partNo;
            }
            return this;
        }

        public Builder withChildStreamType(final String childStreamType) {
            this.childType = childStreamType;
            return this;
        }

        /**
         * Zero based
         */
        public Builder withSegmentNumber(final Long segmentNo) {
            if (segmentNo != null) {
                this.segmentNo = segmentNo;
            }
            return this;
        }

        public Builder withDataRange(final DataRange dataRange) {
            this.dataRange = dataRange;
            return this;
        }

        public Builder withDataRangeBuilder(final Consumer<DataRange.Builder> dataRangeBuilder) {
            final DataRange.Builder builder = DataRange.builder();
            dataRangeBuilder.accept(builder);
            this.dataRange = builder.build();
            return this;
        }

        public Builder withHighlight(final TextRange highlight) {
            this.highlight = highlight;
            return this;
        }

        public Builder truncateToWholeLines() {
            this.truncateToWholeLines = true;
            return this;
        }

        public SourceLocation build() {
            return new SourceLocation(this);
        }
    }
}
