/*
 * Copyright 2017 Crown Copyright
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

package stroom.search.elastic.shared;

import stroom.datasource.api.v2.AbstractField;
import stroom.docref.DocRef;
import stroom.docstore.shared.Doc;
import stroom.query.api.v2.ExpressionOperator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
        "type",
        "uuid",
        "name",
        "version",
        "createTime",
        "updateTime",
        "createUser",
        "updateUser",
        "description",
        "clusterRef",
        "indexName",
        "fields",
        "dataSourceFields",
        "retentionExpression"
})
@JsonInclude(Include.NON_NULL)
public class ElasticIndexDoc extends Doc {

    public static final String DOCUMENT_TYPE = "ElasticIndex";

    /**
     * Reference to the `ElasticCluster` containing common Elasticsearch cluster connection properties
     */
    @JsonProperty
    private DocRef clusterRef;

    @JsonProperty
    private String description;

    @JsonProperty
    private String indexName;

    @JsonProperty
    private List<ElasticIndexField> fields;

    @JsonProperty
    private List<AbstractField> dataSourceFields;

    @JsonProperty
    private ExpressionOperator retentionExpression;

    public ElasticIndexDoc() {
        this.fields = new ArrayList<>();
        this.dataSourceFields = new ArrayList<>();
    }

    @JsonCreator
    public ElasticIndexDoc(
            @JsonProperty("type") final String type,
            @JsonProperty("uuid") final String uuid,
            @JsonProperty("name") final String name,
            @JsonProperty("version") final String version,
            @JsonProperty("createTime") final Long createTime,
            @JsonProperty("updateTime") final Long updateTime,
            @JsonProperty("createUser") final String createUser,
            @JsonProperty("updateUser") final String updateUser,
            @JsonProperty("description") final String description,
            @JsonProperty("clusterRef") final DocRef clusterRef,
            @JsonProperty("indexName") final String indexName,
            @JsonProperty("fields") final List<ElasticIndexField> fields,
            @JsonProperty("dataSourceFields") final List<AbstractField> dataSourceFields,
            @JsonProperty("retentionExpression") final ExpressionOperator retentionExpression
    ) {
        super(type, uuid, name, version, createTime, updateTime, createUser, updateUser);
        this.description = description;
        this.clusterRef = clusterRef;
        this.indexName = indexName;
        this.fields = fields;
        this.dataSourceFields = dataSourceFields;
        this.retentionExpression = retentionExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public DocRef getClusterRef() {
        return clusterRef;
    }

    public void setClusterRef(final DocRef clusterRef) {
        this.clusterRef = clusterRef;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(final String indexName) {
        if (indexName == null || indexName.trim().isEmpty()) {
            this.indexName = null;
        } else {
            this.indexName = indexName;
        }
    }

    public List<ElasticIndexField> getFields() {
        return fields;
    }

    public void setFields(final List<ElasticIndexField> fields) {
        this.fields = fields;
    }

    public List<AbstractField> getDataSourceFields() {
        return dataSourceFields;
    }

    public void setDataSourceFields(final List<AbstractField> dataSourceFields) {
        this.dataSourceFields = dataSourceFields;
    }

    public ExpressionOperator getRetentionExpression() {
        return retentionExpression;
    }

    public void setRetentionExpression(final ExpressionOperator retentionExpression) {
        this.retentionExpression = retentionExpression;
    }

    @JsonIgnore
    @Override
    public final String getType() {
        return DOCUMENT_TYPE;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ElasticIndexDoc)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final ElasticIndexDoc elasticIndex = (ElasticIndexDoc) o;
        return Objects.equals(description, elasticIndex.description) &&
                Objects.equals(clusterRef, elasticIndex.clusterRef) &&
                Objects.equals(indexName, elasticIndex.indexName) &&
                Objects.equals(fields, elasticIndex.fields) &&
                Objects.equals(dataSourceFields, elasticIndex.dataSourceFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), description, indexName, clusterRef, fields, dataSourceFields);
    }

    @Override
    public String toString() {
        return "ElasticIndex{" +
                "description='" + description + '\'' +
                ", clusterRef='" + clusterRef + '\'' +
                ", indexName='" + indexName + '\'' +
                ", fields=" + fields +
                ", dataSourceFields=" + dataSourceFields +
                '}';
    }
}
