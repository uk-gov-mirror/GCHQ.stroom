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

package stroom.index.shared;

import stroom.index.shared.IndexShard.IndexShardStatus;
import stroom.util.shared.BaseCriteria;
import stroom.util.shared.CriteriaFieldSort;
import stroom.util.shared.PageRequest;
import stroom.util.shared.Range;
import stroom.util.shared.Selection;
import stroom.util.shared.StringCriteria;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class FindIndexShardCriteria extends BaseCriteria {

    public static final String FIELD_ID = "Id";
    public static final String FIELD_PARTITION = "Partition";

    @JsonProperty
    private Range<Integer> documentCountRange;
    @JsonProperty
    private final Selection<String> nodeNameSet;
    @JsonProperty
    private final Selection<Integer> volumeIdSet;
    @JsonProperty
    private final Selection<String> indexUuidSet;
    @JsonProperty
    private final Selection<Long> indexShardIdSet;
    @JsonProperty
    private final Selection<IndexShardStatus> indexShardStatusSet;
    @JsonProperty
    private final StringCriteria partition;

    public static FindIndexShardCriteria matchAll() {
        return new FindIndexShardCriteria(
                null,
                null,
                new Range<>(),
                Selection.selectAll(),
                Selection.selectAll(),
                Selection.selectAll(),
                Selection.selectAll(),
                Selection.selectAll(),
                new StringCriteria());
    }

    public static FindIndexShardCriteria matchNone() {
        return new FindIndexShardCriteria(
                null,
                null,
                new Range<>(),
                Selection.selectNone(),
                Selection.selectNone(),
                Selection.selectNone(),
                Selection.selectNone(),
                Selection.selectNone(),
                new StringCriteria());
    }

    @JsonCreator
    public FindIndexShardCriteria(
            @JsonProperty("pageRequest") final PageRequest pageRequest,
            @JsonProperty("sortList") final List<CriteriaFieldSort> sortList,
            @JsonProperty("documentCountRange") final Range<Integer> documentCountRange,
            @JsonProperty("nodeNameSet") final Selection<String> nodeNameSet,
            @JsonProperty("volumeIdSet") final Selection<Integer> volumeIdSet,
            @JsonProperty("indexUuidSet") final Selection<String> indexUuidSet,
            @JsonProperty("indexShardIdSet") final Selection<Long> indexShardIdSet,
            @JsonProperty("indexShardStatusSet") final Selection<IndexShardStatus> indexShardStatusSet,
            @JsonProperty("partition") final StringCriteria partition) {

        super(pageRequest, sortList);
        this.documentCountRange = documentCountRange;
        this.nodeNameSet = nodeNameSet;
        this.volumeIdSet = volumeIdSet;
        this.indexUuidSet = indexUuidSet;
        this.indexShardIdSet = indexShardIdSet;
        this.indexShardStatusSet = indexShardStatusSet;
        this.partition = partition;
    }

    public Selection<IndexShardStatus> getIndexShardStatusSet() {
        return indexShardStatusSet;
    }

    public Range<Integer> getDocumentCountRange() {
        return documentCountRange;
    }

    public void setDocumentCountRange(Range<Integer> documentCountRange) {
        this.documentCountRange = documentCountRange;
    }

    public Selection<String> getIndexUuidSet() {
        return indexUuidSet;
    }

    public Selection<Long> getIndexShardIdSet() {
        return indexShardIdSet;
    }

    public Selection<String> getNodeNameSet() {
        return nodeNameSet;
    }

    public Selection<Integer> getVolumeIdSet() {
        return volumeIdSet;
    }

    public StringCriteria getPartition() {
        return partition;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("indexUuidSet=");
        sb.append(indexUuidSet);
        return sb.toString();
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindIndexShardCriteria)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final FindIndexShardCriteria that = (FindIndexShardCriteria) o;
        return Objects.equals(documentCountRange, that.documentCountRange) &&
                Objects.equals(nodeNameSet, that.nodeNameSet) &&
                Objects.equals(volumeIdSet, that.volumeIdSet) &&
                Objects.equals(indexUuidSet, that.indexUuidSet) &&
                Objects.equals(indexShardIdSet, that.indexShardIdSet) &&
                Objects.equals(indexShardStatusSet, that.indexShardStatusSet) &&
                Objects.equals(partition, that.partition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                documentCountRange,
                nodeNameSet,
                volumeIdSet,
                indexUuidSet,
                indexShardIdSet,
                indexShardStatusSet,
                partition);
    }
}
