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

package stroom.node.client.presenter;

import stroom.cell.info.client.InfoColumn;
import stroom.cell.tickbox.client.TickBoxCell;
import stroom.cell.tickbox.shared.TickBoxState;
import stroom.cell.valuespinner.client.ValueSpinnerCell;
import stroom.cell.valuespinner.shared.EditableInteger;
import stroom.content.client.presenter.ContentTabPresenter;
import stroom.data.client.presenter.RestDataProvider;
import stroom.data.grid.client.DataGridView;
import stroom.data.grid.client.DataGridViewImpl;
import stroom.data.grid.client.EndColumn;
import stroom.data.table.client.Refreshable;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.node.shared.ClusterNodeInfo;
import stroom.node.shared.FetchNodeStatusResponse;
import stroom.node.shared.Node;
import stroom.node.shared.NodeResource;
import stroom.node.shared.NodeStatusResult;
import stroom.svg.client.Icon;
import stroom.svg.client.SvgPresets;
import stroom.util.client.DataGridUtil;
import stroom.util.client.SafeHtmlUtil;
import stroom.util.shared.BuildInfo;
import stroom.util.shared.ModelStringUtil;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupPosition;
import stroom.widget.popup.client.presenter.PopupView.PopupType;
import stroom.widget.tooltip.client.presenter.TooltipPresenter;
import stroom.widget.tooltip.client.presenter.TooltipUtil;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class NodeMonitoringPresenter extends ContentTabPresenter<DataGridView<NodeStatusResult>>
        implements Refreshable {

    private static final NodeResource NODE_RESOURCE = GWT.create(NodeResource.class);
    private static final NumberFormat THOUSANDS_FORMATTER = NumberFormat.getFormat("#,###");

    private final RestFactory restFactory;
    private final TooltipPresenter tooltipPresenter;
    private final RestDataProvider<NodeStatusResult, FetchNodeStatusResponse> dataProvider;

    private final Map<String, PingResult> latestPing = new HashMap<>();

    @Inject
    public NodeMonitoringPresenter(final EventBus eventBus,
                                   final RestFactory restFactory,
                                   final TooltipPresenter tooltipPresenter) {
        super(eventBus, new DataGridViewImpl<>(true));
        this.restFactory = restFactory;
        this.tooltipPresenter = tooltipPresenter;
        initTableColumns();
        dataProvider = new RestDataProvider<NodeStatusResult, FetchNodeStatusResponse>(eventBus) {
            @Override
            protected void exec(final Consumer<FetchNodeStatusResponse> dataConsumer,
                                final Consumer<Throwable> throwableConsumer) {
                final Rest<FetchNodeStatusResponse> rest = restFactory.create();
                rest
                        .onSuccess(dataConsumer)
                        .onFailure(throwableConsumer)
                        .call(NODE_RESOURCE)
                        .find();
            }

            @Override
            protected void changeData(final FetchNodeStatusResponse data) {
                // Ping each node.
                data.getValues().forEach(row -> {
                    final String nodeName = row.getNode().getName();
                    final Rest<Long> rest = restFactory.create();
                    rest.onSuccess(ping -> {
                        latestPing.put(nodeName, new PingResult(ping, null));
                        super.changeData(data);
                    }).onFailure(throwable -> {
                        latestPing.put(nodeName, new PingResult(null, throwable.getMessage()));
                        super.changeData(data);
                    }).call(NODE_RESOURCE).ping(nodeName);
                });
                super.changeData(data);
            }
        };
        dataProvider.addDataDisplay(getView().getDataDisplay());
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns() {
        // Info column.
        final InfoColumn<NodeStatusResult> infoColumn = new InfoColumn<NodeStatusResult>() {
            @Override
            protected void showInfo(final NodeStatusResult row, final int x, final int y) {
                final Rest<ClusterNodeInfo> rest = restFactory.create();
                rest
                        .onSuccess(result -> showNodeInfoResult(row.getNode(), result, x, y))
                        .onFailure(caught -> showNodeInfoError(caught, x, y))
                        .call(NODE_RESOURCE)
                        .info(row.getNode().getName());
            }
        };
        getView().addColumn(infoColumn, "<br/>", 20);


        // Name.
        final Column<NodeStatusResult, String> nameColumn = new Column<NodeStatusResult, String>(new TextCell()) {
            @Override
            public String getValue(final NodeStatusResult row) {
                if (row == null) {
                    return null;
                }
                return row.getNode().getName();
            }
        };
        getView().addResizableColumn(nameColumn, "Name", 100);

        // Host Name.
        final Column<NodeStatusResult, String> hostNameColumn = new Column<NodeStatusResult, String>(new TextCell()) {
            @Override
            public String getValue(final NodeStatusResult row) {
                if (row == null) {
                    return null;
                }
                return row.getNode().getUrl();
            }
        };
        getView().addResizableColumn(hostNameColumn, "Cluster Base Endpoint URL", 400);

        // Ping (ms)
        final Column<NodeStatusResult, SafeHtml> safeHtmlColumn = DataGridUtil.safeHtmlColumn(row -> {
            if (row == null) {
                return null;
            }

            final PingResult pingResult = latestPing.get(row.getNode().getName());
            if (pingResult != null) {
                if ("No response".equals(pingResult.getError())) {
                    return SafeHtmlUtil.getSafeHtml(pingResult.getError());
                }
                if (pingResult.getError() != null) {
                    return SafeHtmlUtil.getSafeHtml("Error");
                }

                // Bar widths are all relative to the longest ping.
                final long unHealthyThresholdPing = 250;
                final long highestPing = latestPing.values().stream()
                        .filter(result -> result.getPing() != null)
                        .mapToLong(PingResult::getPing)
                        .max()
                        .orElse(unHealthyThresholdPing);
                final int maxBarWidthPct = 100;
                final double barWidthPct = pingResult.getPing() > highestPing
                        ? maxBarWidthPct
                        : ((double) pingResult.getPing() / highestPing * maxBarWidthPct);

                final SafeHtml textHtml = TooltipUtil.styledSpan(
                        THOUSANDS_FORMATTER.format(pingResult.getPing()),
                        safeStylesBuilder ->
                                safeStylesBuilder.paddingLeft(0.25, Unit.EM));

                final String healthyBarColour = "rgba(33, 150, 243, 0.6)"; // stroom blue with alpha
                final String unHealthyBarColour = "rgba(255, 111, 0, 0.6)"; // stroom dirty amber with aplha
                final String barColour = pingResult.getPing() < unHealthyThresholdPing
                        ? healthyBarColour
                        : unHealthyBarColour;

                final SafeHtml barDiv = TooltipUtil.styledDiv(textHtml, safeStylesBuilder ->
                        safeStylesBuilder
                                .trustedColor("#white")
                                .trustedBackgroundColor(barColour)
                                .width(barWidthPct, Unit.PCT)
                                .height(14, Unit.PX));

                final SafeHtml outerDiv = TooltipUtil.styledDiv(barDiv, safeStylesBuilder ->
                        safeStylesBuilder
                                .paddingLeft(0.25, Unit.EM)
                                .paddingRight(0.25, Unit.EM));

                return outerDiv;
            }
            return SafeHtmlUtil.getSafeHtml("-");
        });
        getView().addResizableColumn(safeHtmlColumn, "Ping (ms)", 300);

        // Master.
        final Column<NodeStatusResult, TickBoxState> masterColumn = new Column<NodeStatusResult, TickBoxState>(
                TickBoxCell.create(new TickBoxCell.NoBorderAppearance(), false, false, false)) {
            @Override
            public TickBoxState getValue(final NodeStatusResult row) {
                if (row == null) {
                    return null;
                }
                return TickBoxState.fromBoolean(row.isMaster());
            }
        };
        masterColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        getView().addColumn(masterColumn, "Master", 50);

        // Priority.
        final Column<NodeStatusResult, Number> priorityColumn = new Column<NodeStatusResult, Number>(
                new ValueSpinnerCell(1, 100)) {
            @Override
            public Number getValue(final NodeStatusResult row) {
                if (row == null) {
                    return null;
                }
                return new EditableInteger(row.getNode().getPriority());
            }
        };
        priorityColumn.setFieldUpdater((index, row, value) -> {
            final Rest<Node> rest = restFactory.create();
            rest
                    .onSuccess(result -> refresh())
                    .call(NODE_RESOURCE)
                    .setPriority(row.getNode().getName(), value.intValue());
        });
        getView().addColumn(priorityColumn, "Priority", 55);

        // Enabled
        final Column<NodeStatusResult, TickBoxState> enabledColumn = new Column<NodeStatusResult, TickBoxState>(
                TickBoxCell.create(false, false)) {
            @Override
            public TickBoxState getValue(final NodeStatusResult row) {
                if (row == null) {
                    return null;
                }
                return TickBoxState.fromBoolean(row.getNode().isEnabled());
            }
        };
        enabledColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        enabledColumn.setFieldUpdater((index, row, value) -> {
            final Rest<Node> rest = restFactory.create();
            rest
                    .onSuccess(result -> refresh())
                    .call(NODE_RESOURCE)
                    .setEnabled(row.getNode().getName(), value.toBoolean());
        });

        getView().addColumn(enabledColumn, "Enabled", 60);

        getView().addEndColumn(new EndColumn<>());
    }

    private void showNodeInfoResult(final Node node, final ClusterNodeInfo result, final int x, final int y) {
        final TooltipUtil.Builder builder = TooltipUtil.builder();

        if (result != null) {
            final BuildInfo buildInfo = result.getBuildInfo();
            builder
                    .addTwoColTable(tableBuilder -> {
                        tableBuilder.addHeaderRow("Node Details");
                        tableBuilder.addRow("Node Name", result.getNodeName(), true);
                        if (buildInfo != null) {
                            tableBuilder
                                    .addRow("Build Version", buildInfo.getBuildVersion(), true)
                                    .addRow("Build Date", buildInfo.getBuildDate(), true)
                                    .addRow("Up Date", buildInfo.getUpDate(), true);
                        }
                        return tableBuilder
                                .addRow("Discover Time", result.getDiscoverTime(), true)
                                .addRow("Node Endpoint URL", result.getEndpointUrl(), true)
                                .addRow("Ping", ModelStringUtil.formatDurationString(result.getPing()))
                                .addRow("Error", result.getError())
                                .build();
                    })
                    .addBreak()
                    .addHeading("Node List");

            if (result.getItemList() != null) {
                for (final ClusterNodeInfo.ClusterNodeInfoItem info : result.getItemList()) {
                    String nodeValue = info.getNodeName();

                    if (!info.isActive()) {
                        nodeValue += " (Unknown)";
                    }
                    if (info.isMaster()) {
                        nodeValue += " (Master)";
                    }
                    builder.addLine(nodeValue);
                }
            }
        } else {
            builder.addTwoColTable(tableBuilder -> tableBuilder
                    .addRow("Node Name", node.getName(), true)
                    .addRow("Cluster URL", node.getUrl(), true)
                    .build());
        }

        tooltipPresenter.setHTML(builder.build());
        final PopupPosition popupPosition = new PopupPosition(x, y);
        ShowPopupEvent.fire(NodeMonitoringPresenter.this, tooltipPresenter, PopupType.POPUP,
                popupPosition, null);
    }

    private void showNodeInfoError(final Throwable caught, final int x, final int y) {
        tooltipPresenter.setHTML(SafeHtmlUtils.fromString(caught.getMessage()));
        final PopupPosition popupPosition = new PopupPosition(x, y);
        ShowPopupEvent.fire(NodeMonitoringPresenter.this, tooltipPresenter, PopupType.POPUP,
                popupPosition, null);
    }

    @Override
    public void refresh() {
        dataProvider.refresh();
    }

    @Override
    public Icon getIcon() {
        return SvgPresets.NODES;
    }

    @Override
    public String getLabel() {
        return "Nodes";
    }

    private static final class PingResult {

        private final Long ping;
        private final String error;

        PingResult(final Long ping, final String error) {
            this.ping = ping;
            this.error = error;
        }

        Long getPing() {
            return ping;
        }

        String getError() {
            return error;
        }
    }
}
