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
 *
 */

package stroom.search.elastic.client.presenter;

import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.HasDocumentRead;
import stroom.search.elastic.shared.ElasticIndexDoc;
import stroom.search.elastic.shared.ElasticIndexField;
import stroom.search.elastic.shared.ElasticIndexResource;
import stroom.search.solr.shared.SolrIndexResource;
import stroom.widget.customdatebox.client.ClientDateUtil;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ElasticIndexFieldListPresenter
        extends MyPresenterWidget<ElasticIndexFieldListPresenter.ElasticIndexFieldListView>
        implements HasDocumentRead<ElasticIndexDoc> {

    private static final SolrIndexResource ELASTIC_INDEX_RESOURCE = GWT.create(ElasticIndexResource.class);

    private final DataGridView<ElasticIndexField> dataGridView;
    private List<ElasticIndexField> fields;
    private ElasticIndexFieldDataProvider<ElasticIndexField> dataProvider;

    @Inject
    public ElasticIndexFieldListPresenter(final EventBus eventBus,
                                          final ElasticIndexFieldListView view
    ) {
        super(eventBus, view);

        dataGridView = new DataGridViewImpl<>(true, true);
        view.setDataGridView(dataGridView);

        addColumns();
    }

    @Override
    protected void onBind() {
        super.onBind();
    }

    private void addColumns() {
        addStringColumn("Name", 150, ElasticIndexField::getFieldName);
        addStringColumn("Use", row -> row.getFieldUse().getDisplayValue());
        addStringColumn("Type", ElasticIndexField::getFieldType);
        addBooleanColumn("Stored", ElasticIndexField::isStored);
        addBooleanColumn("Indexed", ElasticIndexField::isIndexed);
        dataGridView.addEndColumn(new EndColumn<>());
    }

    private void addStringColumn(final String name, final Function<ElasticIndexField, String> function) {
        addStringColumn(name, 100, function);
    }

    private void addStringColumn(final String name,
                                 final int width,
                                 final Function<ElasticIndexField, String> function
    ) {
        dataGridView.addResizableColumn(new Column<ElasticIndexField, String>(new TextCell()) {
            @Override
            public String getValue(final ElasticIndexField row) {
                return function.apply(row);
            }
        }, name, width);
    }

    private void addBooleanColumn(final String name, final Function<ElasticIndexField, Boolean> function) {
        dataGridView.addResizableColumn(new Column<ElasticIndexField, String>(new TextCell()) {
            @Override
            public String getValue(final ElasticIndexField row) {
                return getYesNoString(function.apply(row));
            }
        }, name, 100);
    }

    private String getYesNoString(final boolean bool) {
        if (bool) {
            return "Yes";
        }
        return "No";
    }

    private void refresh() {
        if (fields == null) {
            fields = new ArrayList<>();
        }

        if (dataProvider == null) {
            this.dataProvider = new ElasticIndexFieldDataProvider<>();
            dataProvider.addDataDisplay(dataGridView.getDataDisplay());
        }

        dataProvider.setList(fields);
        dataProvider.refresh();
    }

    @Override
    public void read(final DocRef docRef, final ElasticIndexDoc index) {

        if (index != null) {
            fields = index.getFields();

            final StringBuilder sb = new StringBuilder();
            String now = ClientDateUtil.toISOString(System.currentTimeMillis());
            sb
                .append("Field list updated at: ")
                .append(now)
                .append("<br />Field count: " + fields.size());

            getView().setStatusMessage(sb.toString());
        }

        refresh();
    }

    public interface ElasticIndexFieldListView extends View {
        void setDataGridView(final View view);

        void setStatusMessage(final String syncState);
    }
}
