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

package stroom.search.elastic.search;

import stroom.dashboard.expression.v1.FieldIndex;
import stroom.query.common.v2.Receiver;
import stroom.search.elastic.shared.ElasticIndexDoc;

import org.elasticsearch.index.query.QueryBuilder;

class ElasticSearchTask {
    private final ElasticAsyncSearchTask asyncSearchTask;
    private final ElasticIndexDoc elasticIndex;
    private final QueryBuilder query;
    private final FieldIndex fieldIndex;
    private final Receiver receiver;
    private final Tracker tracker;

    ElasticSearchTask(final ElasticAsyncSearchTask asyncSearchTask,
                      final ElasticIndexDoc elasticIndex,
                      final QueryBuilder query,
                      final FieldIndex fieldIndex,
                      final Receiver receiver,
                      final Tracker tracker) {
        this.asyncSearchTask = asyncSearchTask;
        this.elasticIndex = elasticIndex;
        this.query = query;
        this.fieldIndex = fieldIndex;
        this.receiver = receiver;
        this.tracker = tracker;
    }

    ElasticAsyncSearchTask getAsyncSearchTask() {
        return asyncSearchTask;
    }

    ElasticIndexDoc getElasticIndex() {
        return elasticIndex;
    }

    QueryBuilder getQuery() {
        return query;
    }

    FieldIndex getFieldIndex() {
        return fieldIndex;
    }

    Receiver getReceiver() {
        return receiver;
    }

    Tracker getTracker() {
        return tracker;
    }
}
