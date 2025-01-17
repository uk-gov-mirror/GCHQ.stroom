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

package stroom.search.impl;

import stroom.datasource.api.v2.DataSource;
import stroom.docref.DocRef;
import stroom.event.logging.rs.api.AutoLogged;
import stroom.event.logging.rs.api.AutoLogged.OperationType;
import stroom.index.impl.StroomIndexQueryResource;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;

import com.codahale.metrics.annotation.Timed;

import javax.inject.Inject;
import javax.inject.Provider;

@AutoLogged
public class StroomIndexQueryResourceImpl implements StroomIndexQueryResource {
    private final Provider<StroomIndexQueryService> stroomIndexQueryServiceProvider;

    @Inject
    public StroomIndexQueryResourceImpl(final Provider<StroomIndexQueryService> stroomIndexQueryServiceProvider) {
        this.stroomIndexQueryServiceProvider = stroomIndexQueryServiceProvider;
    }

    @Override
    @Timed
    public DataSource getDataSource(final DocRef docRef) {
        return stroomIndexQueryServiceProvider.get().getDataSource(docRef);
    }

    @Override
    @Timed
    public SearchResponse search(final SearchRequest request) {
        return stroomIndexQueryServiceProvider.get().search(request);
    }

    @Override
    @Timed
    @AutoLogged(OperationType.UNLOGGED)
    public Boolean destroy(final QueryKey queryKey) {
        return stroomIndexQueryServiceProvider.get().destroy(queryKey);
    }
}
