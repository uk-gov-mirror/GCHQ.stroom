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

package stroom.dashboard.client.table;

import stroom.alert.client.event.AlertEvent;
import stroom.alert.client.event.ConfirmEvent;
import stroom.annotation.shared.EventId;
import stroom.cell.expander.client.ExpanderCell;
import stroom.core.client.LocationManager;
import stroom.dashboard.client.main.AbstractComponentPresenter;
import stroom.dashboard.client.main.Component;
import stroom.dashboard.client.main.ComponentRegistry.ComponentType;
import stroom.dashboard.client.main.DataSourceFieldsMap;
import stroom.dashboard.client.main.ResultComponent;
import stroom.dashboard.client.main.SearchModel;
import stroom.dashboard.client.query.QueryPresenter;
import stroom.dashboard.client.table.TablePresenter.TableView;
import stroom.dashboard.shared.ComponentConfig;
import stroom.dashboard.shared.ComponentResultRequest;
import stroom.dashboard.shared.ComponentSettings;
import stroom.dashboard.shared.DashboardQueryKey;
import stroom.dashboard.shared.DashboardResource;
import stroom.dashboard.shared.DashboardSearchRequest;
import stroom.dashboard.shared.DownloadSearchResultsRequest;
import stroom.dashboard.shared.IndexConstants;
import stroom.dashboard.shared.Search;
import stroom.dashboard.shared.TableComponentSettings;
import stroom.dashboard.shared.TableResultRequest;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.FieldTypes;
import stroom.datasource.api.v2.LongField;
import stroom.datasource.api.v2.TextField;
import stroom.dispatch.client.ApplicationInstanceIdProvider;
import stroom.dispatch.client.ExportFileCompleteUtil;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.document.client.event.DirtyEvent;
import stroom.document.client.event.DirtyEvent.DirtyHandler;
import stroom.document.client.event.HasDirtyHandlers;
import stroom.processor.shared.ProcessorExpressionUtil;
import stroom.query.api.v2.ConditionalFormattingRule;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionTerm;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Field.Builder;
import stroom.query.api.v2.Format;
import stroom.query.api.v2.Format.Type;
import stroom.query.api.v2.OffsetRange;
import stroom.query.api.v2.ParamUtil;
import stroom.query.api.v2.Result;
import stroom.query.api.v2.ResultRequest.Fetch;
import stroom.query.api.v2.Row;
import stroom.query.api.v2.TableResult;
import stroom.query.api.v2.TableSettings;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.svg.client.SvgPresets;
import stroom.ui.config.client.UiConfigCache;
import stroom.util.shared.Expander;
import stroom.util.shared.RandomId;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.Version;
import stroom.widget.button.client.ButtonView;
import stroom.widget.menu.client.presenter.MenuListPresenter;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TablePresenter extends AbstractComponentPresenter<TableView>
        implements HasDirtyHandlers, ResultComponent {

    private static final DashboardResource DASHBOARD_RESOURCE = GWT.create(DashboardResource.class);
    public static final ComponentType TYPE = new ComponentType(1, "table", "Table");
    private static final int MIN_EXPANDER_COL_WIDTH = 0;
    private static final Version CURRENT_MODEL_VERSION = new Version(6, 1, 26);

    private final LocationManager locationManager;
    private TableResultRequest tableResultRequest = TableResultRequest.builder()
            .requestedRange(new OffsetRange(0, 100))
            .build();
    private final List<Column<TableRow, ?>> existingColumns = new ArrayList<>();
    private final List<HandlerRegistration> searchModelHandlerRegistrations = new ArrayList<>();
    private final ButtonView addFieldButton;
    private final ButtonView downloadButton;
    private final ButtonView annotateButton;
    private final Provider<FieldAddPresenter> fieldAddPresenterProvider;
    private final DownloadPresenter downloadPresenter;
    private final AnnotationManager annotationManager;
    private final RestFactory restFactory;
    private final ApplicationInstanceIdProvider applicationInstanceIdProvider;
    private final TimeZones timeZones;
    private final FieldsManager fieldsManager;
    private final DataGridView<TableRow> dataGrid;
    private final Column<TableRow, Expander> expanderColumn;

    private int expanderColumnWidth;
    private SearchModel currentSearchModel;
    private FieldAddPresenter fieldAddPresenter;
    private boolean ignoreRangeChange;
    private int[] maxResults = TableComponentSettings.DEFAULT_MAX_RESULTS;
    private final Set<String> usedFieldIds = new HashSet<>();

    @Inject
    public TablePresenter(final EventBus eventBus,
                          final TableView view,
                          final ClientSecurityContext securityContext,
                          final LocationManager locationManager,
                          final MenuListPresenter menuListPresenter,
                          final Provider<RenameFieldPresenter> renameFieldPresenterProvider,
                          final Provider<ExpressionPresenter> expressionPresenterProvider,
                          final FormatPresenter formatPresenter,
                          final FilterPresenter filterPresenter,
                          final Provider<FieldAddPresenter> fieldAddPresenterProvider,
                          final Provider<TableSettingsPresenter> settingsPresenterProvider,
                          final DownloadPresenter downloadPresenter,
                          final AnnotationManager annotationManager,
                          final RestFactory restFactory,
                          final ApplicationInstanceIdProvider applicationInstanceIdProvider,
                          final UiConfigCache clientPropertyCache,
                          final TimeZones timeZones) {
        super(eventBus, view, settingsPresenterProvider);
        this.locationManager = locationManager;
        this.fieldAddPresenterProvider = fieldAddPresenterProvider;
        this.downloadPresenter = downloadPresenter;
        this.annotationManager = annotationManager;
        this.restFactory = restFactory;
        this.applicationInstanceIdProvider = applicationInstanceIdProvider;
        this.timeZones = timeZones;
        this.dataGrid = new DataGridViewImpl<>(true, true);

        view.setTableView(dataGrid);

        // Add the 'add field' button.
        addFieldButton = dataGrid.addButton(SvgPresets.ADD);
        addFieldButton.setTitle("Add Field");

        // Download
        downloadButton = dataGrid.addButton(SvgPresets.DOWNLOAD);
        downloadButton.setVisible(securityContext.hasAppPermission(PermissionNames.DOWNLOAD_SEARCH_RESULTS_PERMISSION));

        // Annotate
        annotateButton = dataGrid.addButton(SvgPresets.ANNOTATE);
        annotateButton.setVisible(securityContext.hasAppPermission(PermissionNames.ANNOTATIONS));
        annotateButton.setEnabled(false);

        fieldsManager = new FieldsManager(
                this,
                menuListPresenter,
                renameFieldPresenterProvider,
                expressionPresenterProvider,
                formatPresenter,
                filterPresenter);
        dataGrid.setHeadingListener(fieldsManager);

        clientPropertyCache.get()
                .onSuccess(result -> {
                    final String value = result.getDefaultMaxResults();
                    if (value != null) {
                        final String[] parts = value.split(",");
                        final int[] arr = new int[parts.length];
                        for (int i = 0; i < arr.length; i++) {
                            arr[i] = Integer.parseInt(parts[i].trim());
                        }
                        maxResults = arr;
                    }
                })
                .onFailure(caught -> AlertEvent.fireError(TablePresenter.this, caught.getMessage(), null));


        // Expander column.
        expanderColumn = new Column<TableRow, Expander>(new ExpanderCell()) {
            @Override
            public Expander getValue(final TableRow row) {
                if (row == null) {
                    return null;
                }
                return row.getExpander();
            }
        };
        expanderColumn.setFieldUpdater((index, result, value) -> {
            tableResultRequest = tableResultRequest
                    .copy()
                    .openGroup(result.getGroupKey(), !value.isExpanded())
                    .build();
            refresh();
        });
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(dataGrid.getSelectionModel().addSelectionHandler(event -> {
            enableAnnotate();
            getComponents().fireComponentChangeEvent(this);
        }));
        registerHandler(dataGrid.addRangeChangeHandler(event -> {
            final com.google.gwt.view.client.Range range = event.getNewRange();
            tableResultRequest = tableResultRequest
                    .copy()
                    .requestedRange(new OffsetRange(range.getStart(), range.getLength()))
                    .build();
            if (!ignoreRangeChange) {
                refresh();
            }
        }));
        registerHandler(dataGrid.addHyperlinkHandler(event -> getEventBus().fireEvent(event)));
        registerHandler(addFieldButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                onAddField(event);
            }
        }));

        registerHandler(downloadButton.addClickHandler(event -> {
            if (currentSearchModel != null) {
                if (currentSearchModel.isSearching()) {
                    ConfirmEvent.fire(TablePresenter.this,
                            "Search still in progress. Do you want to download the current results? " +
                                    "Note that these may be incomplete.",
                            ok -> {
                                if (ok) {
                                    download();
                                }
                            });
                } else {
                    download();
                }
            }
        }));

        registerHandler(annotateButton.addClickHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                annotationManager.showAnnotationMenu(event.getNativeEvent(),
                        getTableSettings(),
                        dataGrid.getSelectionModel().getSelectedItems());
            }
        }));
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        cleanupSearchModelAssociation();
    }

    private void onAddField(final ClickEvent event) {
        if (currentSearchModel != null && fieldAddPresenter == null) {
            fieldAddPresenter = fieldAddPresenterProvider.get();
            final AddSelectionHandler selectionHandler = new AddSelectionHandler(fieldAddPresenter);
            final HandlerRegistration handlerRegistration = fieldAddPresenter
                    .addSelectionChangeHandler(selectionHandler);

            final List<Field> addFields = new ArrayList<>();
            if (currentSearchModel.getIndexLoader().getIndexFieldNames() != null) {
                for (final String indexFieldName : currentSearchModel.getIndexLoader().getIndexFieldNames()) {
                    final Builder fieldBuilder = Field.builder();
                    fieldBuilder.name(indexFieldName);
                    final String fieldParam = ParamUtil.makeParam(indexFieldName);

                    if (indexFieldName.startsWith("annotation:")) {
                        final AbstractField dataSourceField = currentSearchModel.getIndexLoader()
                                .getDataSourceFieldsMap().get(indexFieldName);
                        if (dataSourceField != null && FieldTypes.DATE.equals(dataSourceField.getType())) {
                            fieldBuilder.expression("annotation(formatDate(" + fieldParam + "), ${annotation:Id})");
                        } else {
                            fieldBuilder.expression("annotation(" + fieldParam + ", ${annotation:Id})");
                        }
                    } else {
                        fieldBuilder.expression(fieldParam);
                    }

                    final DataSourceFieldsMap indexFieldsMap = getIndexFieldsMap();
                    if (indexFieldsMap != null) {
                        final AbstractField indexField = indexFieldsMap.get(indexFieldName);
                        if (indexField != null) {
                            switch (indexField.getType()) {
                                case FieldTypes.DATE:
                                    fieldBuilder.format(Format.DATE_TIME);
                                    break;
                                case FieldTypes.INTEGER:
                                case FieldTypes.LONG:
                                case FieldTypes.FLOAT:
                                case FieldTypes.DOUBLE:
                                case FieldTypes.ID:
                                    fieldBuilder.format(Format.NUMBER);
                                    break;
                                default:
                                    fieldBuilder.format(Format.GENERAL);
                                    break;
                            }
                        }
                    }

                    addFields.add(fieldBuilder.build());
                }
            }

            final Field count = Field.builder()
                    .name("Count")
                    .format(Format.NUMBER)
                    .expression("count()")
                    .build();
            addFields.add(count);

            final Field countGroups = Field.builder()
                    .name("Count Groups")
                    .format(Format.NUMBER)
                    .expression("countGroups()")
                    .build();
            addFields.add(countGroups);

            final Field custom = Field.builder()
                    .name("Custom")
                    .build();
            addFields.add(custom);

            fieldAddPresenter.setFields(addFields);
            fieldAddPresenter.clearSelection();

            final PopupUiHandlers popupUiHandlers = new PopupUiHandlers() {
                @Override
                public void onHideRequest(final boolean autoClose, final boolean ok) {
                    HidePopupEvent.fire(TablePresenter.this, fieldAddPresenter);
                }

                @Override
                public void onHide(final boolean autoClose, final boolean ok) {
                    handlerRegistration.removeHandler();
                    fieldAddPresenter = null;
                }
            };

            final com.google.gwt.dom.client.Element target = event.getNativeEvent().getEventTarget().cast();

            final PopupPosition popupPosition = new PopupPosition(target.getAbsoluteLeft() - 3,
                    target.getAbsoluteTop() + target.getClientHeight() + 1);
            ShowPopupEvent.fire(
                    this,
                    fieldAddPresenter,
                    PopupType.POPUP,
                    popupPosition,
                    popupUiHandlers,
                    target);
        }
    }

    private String createRandomFieldId() {
        String id = getComponentConfig().getId() + "|" + RandomId.createId(5);
        // Make sure we don't duplicate ids.
        while (usedFieldIds.contains(id)) {
            id = getComponentConfig().getId() + "|" + RandomId.createId(5);
        }
        usedFieldIds.add(id);
        return id;
    }

    private void download() {
        if (currentSearchModel != null) {
            final Search activeSearch = currentSearchModel.getActiveSearch();
            final DashboardQueryKey queryKey = currentSearchModel.getCurrentQueryKey();
            if (activeSearch != null && queryKey != null) {
                final PopupUiHandlers popupUiHandlers = new PopupUiHandlers() {
                    @Override
                    public void onHideRequest(final boolean autoClose, final boolean ok) {
                        if (ok) {
                            final TableResultRequest tableResultRequest = TableResultRequest
                                    .builder()
                                    .componentId(getComponentConfig().getId())
                                    .requestedRange(new OffsetRange(0, Integer.MAX_VALUE))
                                    .tableSettings(TablePresenter.this.tableResultRequest.getTableSettings())
                                    .fetch(Fetch.ALL)
                                    .build();

                            final List<ComponentResultRequest> requests = new ArrayList<>();
                            requests.add(tableResultRequest);

                            final Search search = Search
                                    .builder()
                                    .dataSourceRef(activeSearch.getDataSourceRef())
                                    .expression(activeSearch.getExpression())
                                    .componentSettingsMap(activeSearch.getComponentSettingsMap())
                                    .params(activeSearch.getParams())
                                    .incremental(true)
                                    .storeHistory(false)
                                    .queryInfo(activeSearch.getQueryInfo())
                                    .build();

                            final DashboardSearchRequest searchRequest = new DashboardSearchRequest(
                                    queryKey,
                                    search,
                                    requests,
                                    timeZones.getTimeZone());

                            final DownloadSearchResultsRequest downloadSearchResultsRequest =
                                    new DownloadSearchResultsRequest(
                                            applicationInstanceIdProvider.get(),
                                            searchRequest,
                                            getComponentConfig().getId(),
                                            downloadPresenter.getFileType(),
                                            downloadPresenter.isSample(),
                                            downloadPresenter.getPercent(),
                                            timeZones.getTimeZone());
                            final Rest<ResourceGeneration> rest = restFactory.create();
                            rest
                                    .onSuccess(result -> ExportFileCompleteUtil.onSuccess(locationManager,
                                            null,
                                            result))
                                    .call(DASHBOARD_RESOURCE)
                                    .downloadSearchResults(downloadSearchResultsRequest);
                        }

                        HidePopupEvent.fire(TablePresenter.this, downloadPresenter);
                    }

                    @Override
                    public void onHide(final boolean autoClose, final boolean ok) {
                    }
                };

                final PopupSize popupSize = new PopupSize(316, 124, false);
                ShowPopupEvent.fire(this, downloadPresenter, PopupType.OK_CANCEL_DIALOG, popupSize, "Download Options",
                        popupUiHandlers);
            }
        }
    }

    private void enableAnnotate() {
        final List<EventId> idList = annotationManager.getEventIdList(getTableSettings(),
                dataGrid.getSelectionModel().getSelectedItems());
        final boolean enabled = idList.size() > 0;
        annotateButton.setEnabled(enabled);
    }


    @Override
    public void startSearch() {
        final TableSettings tableSettings = getTableSettings()
                .copy()
                .buildTableSettings();
        tableResultRequest = tableResultRequest
                .copy()
                .tableSettings(tableSettings)
                .build();
    }

    @Override
    public void endSearch() {
    }

    @Override
    public void setWantsData(final boolean wantsData) {
        getView().setRefreshing(wantsData);
        if (wantsData) {
            tableResultRequest = tableResultRequest
                    .copy()
                    .fetch(Fetch.CHANGES)
                    .build();
        } else {
            tableResultRequest = tableResultRequest
                    .copy()
                    .fetch(Fetch.NONE)
                    .build();
        }
    }

    @Override
    public void setData(final Result componentResult) {
        ignoreRangeChange = true;

        try {
            if (componentResult != null) {
                // Don't refresh the table unless the results have changed.
                final TableResult tableResult = (TableResult) componentResult;

                final List<TableRow> values = processData(tableResult.getFields(), tableResult.getRows());
                final OffsetRange valuesRange = tableResult.getResultRange();

                // Only set data in the table if we have got some results and
                // they have changed.
                if (valuesRange.getOffset() == 0 || values.size() > 0) {
                    dataGrid.setRowData(valuesRange.getOffset().intValue(), values);
                    dataGrid.setRowCount(tableResult.getTotalResults(), true);
                }

                // Enable download of current results.
                downloadButton.setEnabled(true);
            } else {
                // Disable download of current results.
                downloadButton.setEnabled(false);

                dataGrid.setRowData(0, new ArrayList<>());
                dataGrid.setRowCount(0, true);

                dataGrid.getSelectionModel().clear();
            }
        } catch (final RuntimeException e) {
            GWT.log(e.getMessage());
        }

        ignoreRangeChange = false;
    }

    public static AbstractField buildDsField(final Field field) {
        Type colType = Optional.ofNullable(field.getFormat())
                .map(Format::getType)
                .orElse(Type.GENERAL);

        try {
            switch (colType) {
                case NUMBER:
                    return new LongField(field.getName(), true);

                case DATE_TIME:
                    return new DateField(field.getName(), true);

                default:
                    // CONTAINS only supported for legacy content, not for use in UI
                    final List<Condition> conditionList = new ArrayList<>();
                    conditionList.add(Condition.IN);
                    conditionList.add(Condition.EQUALS);
                    return new TextField(field.getName(), true, conditionList);

            }
        } catch (Exception e) {
            GWT.log(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private List<TableRow> processData(final List<Field> fields, final List<Row> values) {
        // See if any fields have more than 1 level. If they do then we will add
        // an expander column.
        int maxGroup = -1;
        final boolean showDetail = getTableSettings().showDetail();
        for (final Field field : fields) {
            if (field.getGroup() != null) {
                final int group = field.getGroup();
                if (group > maxGroup) {
                    maxGroup = group;
                }
            }
        }
        int maxDepth = maxGroup;
        if (showDetail) {
            maxDepth++;
        }

        final List<TableRow> processed = new ArrayList<>(values.size());
        for (final Row row : values) {
            SafeStylesBuilder rowStyle = new SafeStylesBuilder();

            // Row styles.
            if (row.getBackgroundColor() != null
                    && !row.getBackgroundColor().isEmpty()) {
                rowStyle.trustedBackgroundColor(row.getBackgroundColor());
            }
            if (row.getTextColor() != null
                    && !row.getTextColor().isEmpty()) {
                rowStyle.trustedColor(row.getTextColor());
            }

            final Map<String, TableRow.Cell> cellsMap = new HashMap<>();
            for (int i = 0; i < fields.size() && i < row.getValues().size(); i++) {
                final Field field = fields.get(i);
                final String value = row.getValues().get(i) != null
                        ? row.getValues().get(i)
                        : "";

                SafeStylesBuilder stylesBuilder = new SafeStylesBuilder();
                stylesBuilder.append(rowStyle.toSafeStyles());

                // Wrap
                if (field.getFormat() != null && field.getFormat().getWrap() != null && field.getFormat().getWrap()) {
                    stylesBuilder.whiteSpace(Style.WhiteSpace.NORMAL);
                }
                // Grouped
                if (field.getGroup() != null && field.getGroup() >= row.getDepth()) {
                    stylesBuilder.fontWeight(Style.FontWeight.BOLD);
                }

                final String style = stylesBuilder.toSafeStyles().asString();

                final TableRow.Cell cell = new TableRow.Cell(value, style);
                cellsMap.put(field.getId(), cell);
            }

            // Create an expander for the row.
            Expander expander = null;
            if (row.getDepth() < maxDepth) {
                final boolean open = tableResultRequest.isGroupOpen(row.getGroupKey());
                expander = new Expander(row.getDepth(), open, false);
            } else if (row.getDepth() > 0) {
                expander = new Expander(row.getDepth(), false, true);
            }

            processed.add(new TableRow(expander, row.getGroupKey(), cellsMap));
        }

        // Set the expander column width.
        if (maxDepth > 0) {
            expanderColumnWidth = 16 + (maxDepth * 10);
        } else {
            expanderColumnWidth = MIN_EXPANDER_COL_WIDTH;
        }
        dataGrid.setColumnWidth(expanderColumn, expanderColumnWidth, Unit.PX);

        return processed;
    }

    private void addExpanderColumn() {
        dataGrid.addColumn(expanderColumn, "<br/>", expanderColumnWidth);
        existingColumns.add(expanderColumn);
    }

    private void addColumn(final Field field) {
        final Column<TableRow, SafeHtml> column = new Column<TableRow, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final TableRow row) {
                if (row == null) {
                    return null;
                }

                return row.getValue(field.getId());
            }
        };

        final FieldHeader fieldHeader = new FieldHeader(fieldsManager, field);
        fieldHeader.setUpdater(value -> dataGrid.redrawHeaders());

        dataGrid.addResizableColumn(column, fieldHeader, field.getWidth());
        existingColumns.add(column);
    }

    void handleFieldRename(final String oldName,
                           final String newName) {
        if (!Objects.equals(oldName, newName)) {
            if (getTableSettings() != null && getTableSettings().getConditionalFormattingRules() != null) {
                final AtomicBoolean wasModified = new AtomicBoolean(false);
                getTableSettings().getConditionalFormattingRules().stream()
                        .map(ConditionalFormattingRule::getExpression)
                        .forEach(expressionOperator -> {
                            boolean wasRuleModified = renameField(expressionOperator, oldName, newName);
                            if (wasRuleModified) {
                                wasModified.compareAndSet(false, true);
                            }
                        });
                if (wasModified.get()) {
                    setDirty(true);
                }
            }
        }
    }

    private boolean renameField(final ExpressionItem expressionItem,
                                final String oldTermName,
                                final String newTermName) {
        final AtomicBoolean wasModified = new AtomicBoolean(false);
        ProcessorExpressionUtil.walkExpressionTree(
                expressionItem,
                null,
                (parent, childOffset, oldTerm) -> {
                    if (Objects.equals(oldTerm.getField(), oldTermName)) {
                        if (parent == null) {
                            throw new RuntimeException("Should not have a term without a parent operator");
                        }

                        final ExpressionTerm newTerm = oldTerm.copy().field(newTermName).build();

                        // Replace the old term with the new one
                        parent.getChildren().set(childOffset, newTerm);
                        wasModified.compareAndSet(false, true);
                    }
                });
        return wasModified.get();
    }

    void redrawHeaders() {
        dataGrid.redrawHeaders();
    }

    private void setQueryId(final String queryId) {
        cleanupSearchModelAssociation();

        if (queryId != null) {
            final Component component = getComponents().get(queryId);
            if (component instanceof QueryPresenter) {
                final QueryPresenter queryPresenter = (QueryPresenter) component;
                currentSearchModel = queryPresenter.getSearchModel();
                if (currentSearchModel != null) {
                    currentSearchModel.addComponent(getComponentConfig().getId(), this);
                }
            }
        }

        if (currentSearchModel != null) {
            searchModelHandlerRegistrations
                    .add(currentSearchModel.getIndexLoader().addChangeDataHandler(event -> updateFields()));
        }

        updateFields();
        getComponents().fireComponentChangeEvent(this);
    }

    private void cleanupSearchModelAssociation() {
        if (currentSearchModel != null) {
            // Remove this component from the list of components the search
            // model expects to update.
            currentSearchModel.removeComponent(getComponentConfig().getId());

            // Clear any existing handler registrations on the search model.
            for (final HandlerRegistration handlerRegistration : searchModelHandlerRegistrations) {
                handlerRegistration.removeHandler();
            }
            searchModelHandlerRegistrations.clear();

            currentSearchModel = null;
        }
    }

    private void updateFields() {
        if (getTableSettings().getFields() == null) {
            setSettings(getTableSettings().copy().fields(new ArrayList<>()).build());
        }

        // Update columns.
        updateColumns();
    }

    private void ensureSpecialFields(final String... indexFieldNames) {
        // Get special fields from the current data source.
        final List<AbstractField> requiredSpecialDsFields = new ArrayList<>();
        final List<Field> requiredSpecialFields = new ArrayList<>();
        // Get all index fields provided by the datasource
        final DataSourceFieldsMap dataSourceFieldsMap = getIndexFieldsMap();
        if (dataSourceFieldsMap != null) {
            for (final String indexFieldName : indexFieldNames) {
                final AbstractField indexField = dataSourceFieldsMap.get(indexFieldName);
                if (indexField != null) {
                    requiredSpecialDsFields.add(indexField);
                    final Field specialField = buildSpecialField(indexFieldName);
                    requiredSpecialFields.add(specialField);
                }
            }

            // If the fields we want to make special do exist in the current data source then
            // add them.
            if (requiredSpecialFields.size() > 0) {
                // Remove all special fields as we will re-add them with the right names if there are any.
                getTableSettings().getFields().removeIf(Field::isSpecial);

                // Prior to the introduction of the special field concept, special fields were
                // treated as invisible fields. For this reason we need to remove old invisible
                // fields if we haven't yet turned them into special fields.
                final Version version = Version.parse(getTableSettings().getModelVersion());
                final boolean old = version.lt(CURRENT_MODEL_VERSION);
                if (old) {
                    requiredSpecialDsFields.forEach(requiredSpecialDsField ->
                            getTableSettings().getFields().removeIf(field ->
                                    !field.isVisible() && field.getName().equals(requiredSpecialDsField.getName())));
                    setSettings(getTableSettings()
                            .copy()
                            .modelVersion(CURRENT_MODEL_VERSION.toString())
                            .build());
                }

                // Add special fields.
                requiredSpecialFields.forEach(field ->
                        getTableSettings().getFields().add(field));
            }

//        GWT.log(tableSettings.getFields().stream()
//                .map(field ->
//                        String.join(
//                                ", ",
//                                field.getId(),
//                                field.getName(),
//                                Boolean.toString(field.isVisible()),
//                                Boolean.toString(field.isSpecial())))
//                .collect(Collectors.joining("\n")));
        }
    }

    public static Field buildSpecialField(final String indexFieldName) {
        final String obfuscatedColumnName = IndexConstants.generateObfuscatedColumnName(indexFieldName);
        return Field.builder()
                .id(obfuscatedColumnName)
                .name(obfuscatedColumnName)
                .expression(ParamUtil.makeParam(indexFieldName))
                .visible(false)
                .special(true)
                .build();
    }

    DataSourceFieldsMap getIndexFieldsMap() {
        if (currentSearchModel != null
                && currentSearchModel.getIndexLoader() != null
                && currentSearchModel.getIndexLoader().getDataSourceFieldsMap() != null) {
            return currentSearchModel.getIndexLoader().getDataSourceFieldsMap();
        }

        return null;
    }

    void updateColumns() {
        // Now make sure special fields exist for stream id and event id.
        ensureSpecialFields(IndexConstants.STREAM_ID, IndexConstants.EVENT_ID, "Id");

        // Remove existing columns.
        for (final Column<TableRow, ?> column : existingColumns) {
            dataGrid.removeColumn(column);
        }
        existingColumns.clear();

        final List<Field> fields = getTableSettings().getFields();
        addExpanderColumn();
        fieldsManager.setFieldsStartIndex(1);

        // Add fields as columns.
        for (final Field field : fields) {
            // Only include the field if it is supposed to be visible.
            if (field.isVisible()) {
                addColumn(field);
            }
        }

        dataGrid.resizeTableToFitColumns();
    }

    @Override
    public HandlerRegistration addDirtyHandler(final DirtyHandler handler) {
        return addHandlerToSource(DirtyEvent.getType(), handler);
    }

    @Override
    public ComponentType getType() {
        return TYPE;
    }

    @Override
    public void read(final ComponentConfig componentConfig) {
        super.read(componentConfig);

        tableResultRequest = tableResultRequest
                .copy()
                .componentId(componentConfig.getId())
                .build();

        ComponentSettings settings = componentConfig.getSettings();
        if (!(settings instanceof TableComponentSettings)) {
            setSettings(createSettings());
        }

        // Ensure all fields have ids.
        if (getTableSettings().getFields() != null) {
            final String obfuscatedStreamId = IndexConstants.generateObfuscatedColumnName(IndexConstants.STREAM_ID);
            final String obfuscatedEventId = IndexConstants.generateObfuscatedColumnName(IndexConstants.EVENT_ID);

            final List<Field> fields = new ArrayList<>();
            getTableSettings().getFields().forEach(field -> {
                Field f = field;
                if (obfuscatedStreamId.equals(f.getName())) {
                    f = buildSpecialField(IndexConstants.STREAM_ID);
                } else if (obfuscatedEventId.equals(f.getName())) {
                    f = buildSpecialField(IndexConstants.EVENT_ID);
                } else if (field.getId() == null) {
                    f = field.copy().id(createRandomFieldId()).build();
                }
                usedFieldIds.add(field.getId());
                fields.add(f);
            });
            setSettings(getTableSettings().copy().fields(fields).build());
        }
    }

    public TableComponentSettings getTableSettings() {
        return (TableComponentSettings) getSettings();
    }

    @Override
    public void link() {
        String queryId = getTableSettings().getQueryId();
        queryId = getComponents().validateOrGetFirstComponentId(queryId, QueryPresenter.TYPE.getId());
        setSettings(getTableSettings().copy().queryId(queryId).build());
        setQueryId(queryId);
    }

    @Override
    protected void changeSettings() {
        super.changeSettings();
        setQueryId(getTableSettings().getQueryId());
    }

    @Override
    public ComponentResultRequest getResultRequest() {
        return tableResultRequest;
    }

    @Override
    public ComponentResultRequest createDownloadQueryRequest() {
        final TableSettings tableSettings = getTableSettings()
                .copy()
                .buildTableSettings();
        return tableResultRequest
                .copy()
                .requestedRange(new OffsetRange(0, Integer.MAX_VALUE))
                .tableSettings(tableSettings)
                .fetch(Fetch.ALL)
                .build();
    }

    @Override
    public void reset() {
        final long length = Math.max(1, tableResultRequest.getRequestedRange().getLength());
        dataGrid.setRowData(0, new ArrayList<>());
        dataGrid.setRowCount(0, true);
        dataGrid.setVisibleRange(0, (int) length);
        tableResultRequest = tableResultRequest
                .copy()
                .requestedRange(new OffsetRange(0L, length))
                .build();
    }

    void clearAndRefresh() {
        clear();
    }

    private void refresh() {
        currentSearchModel.refresh(getComponentConfig().getId());
    }

    private void clear() {
        setData(null);
    }

    public List<TableRow> getSelectedRows() {
        return dataGrid.getSelectionModel().getSelectedItems();
    }

    private TableComponentSettings createSettings() {
        List<Integer> arr = null;
        if (maxResults != null && maxResults.length > 0) {
            arr = new ArrayList<>();
            arr.add(maxResults[0]);
        }

        return TableComponentSettings.builder().maxResults(arr).build();
    }

    public Set<String> getHighlights() {
        if (currentSearchModel != null
                && currentSearchModel.getCurrentResult() != null
                && currentSearchModel.getCurrentResult().getHighlights() != null) {
            return currentSearchModel.getCurrentResult().getHighlights();
        }

        return null;
    }

    public interface TableView extends View {

        void setTableView(View view);

        void setRefreshing(boolean refreshing);
    }

    private class AddSelectionHandler implements SelectionChangeEvent.Handler {

        private final FieldAddPresenter presenter;

        AddSelectionHandler(final FieldAddPresenter presenter) {
            this.presenter = presenter;
        }

        @Override
        public void onSelectionChange(final SelectionChangeEvent event) {
            Field field = presenter.getSelectedObject();
            if (field != null) {
                HidePopupEvent.fire(TablePresenter.this, presenter);

                final String fieldName = field.getName();
                String suffix = "";
                int count = 1;
                final Set<String> currentFields = getTableSettings().getFields().stream().map(Field::getName).collect(
                        Collectors.toSet());
                while (currentFields.contains(fieldName + suffix)) {
                    count++;
                    suffix = " " + count;
                }

                field = field.copy()
                        .name(fieldName + suffix)
                        .id(createRandomFieldId())
                        .build();
                fieldsManager.addField(field);
            }
        }
    }
}
