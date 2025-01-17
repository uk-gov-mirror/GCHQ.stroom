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

package stroom.dashboard.client.text;

import stroom.alert.client.event.AlertEvent;
import stroom.dashboard.client.main.AbstractComponentPresenter;
import stroom.dashboard.client.main.Component;
import stroom.dashboard.client.main.ComponentRegistry.ComponentType;
import stroom.dashboard.client.main.Components;
import stroom.dashboard.client.table.TablePresenter;
import stroom.dashboard.client.table.TableRow;
import stroom.dashboard.shared.ComponentConfig;
import stroom.dashboard.shared.ComponentSettings;
import stroom.dashboard.shared.IndexConstants;
import stroom.dashboard.shared.TableComponentSettings;
import stroom.dashboard.shared.TextComponentSettings;
import stroom.data.shared.DataResource;
import stroom.dispatch.client.Rest;
import stroom.dispatch.client.RestFactory;
import stroom.editor.client.presenter.EditorPresenter;
import stroom.editor.client.presenter.HtmlPresenter;
import stroom.hyperlink.client.Hyperlink;
import stroom.hyperlink.client.HyperlinkEvent;
import stroom.pipeline.shared.AbstractFetchDataResult;
import stroom.pipeline.shared.FetchDataRequest;
import stroom.pipeline.shared.FetchDataResult;
import stroom.pipeline.shared.SourceLocation;
import stroom.pipeline.shared.stepping.StepLocation;
import stroom.pipeline.stepping.client.event.BeginPipelineSteppingEvent;
import stroom.query.api.v2.Field;
import stroom.security.client.api.ClientSecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.util.shared.DataRange;
import stroom.util.shared.DefaultLocation;
import stroom.util.shared.EqualsUtil;
import stroom.util.shared.TextRange;
import stroom.util.shared.Version;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TextPresenter extends AbstractComponentPresenter<TextPresenter.TextView> implements TextUiHandlers {

    private static final DataResource DATA_RESOURCE = GWT.create(DataResource.class);

    public static final ComponentType TYPE = new ComponentType(2, "text", "Text");

    private static final Version CURRENT_MODEL_VERSION = new Version(6, 1, 26);

    private final Provider<EditorPresenter> rawPresenterProvider;
    private final Provider<HtmlPresenter> htmlPresenterProvider;
    private final RestFactory restFactory;
    private final ClientSecurityContext securityContext;
    private List<FetchDataRequest> fetchDataQueue;
    private Timer delayedFetchDataTimer;
    private Long currentStreamId;
    private Long currentPartNo;
    private Long currentRecordNo;
    private Set<String> currentHighlightStrings;
    private boolean playButtonVisible;

    private TablePresenter currentTablePresenter;

    private EditorPresenter rawPresenter;
    private HtmlPresenter htmlPresenter;

    private boolean isHtml;

    @Inject
    public TextPresenter(final EventBus eventBus,
                         final TextView view,
                         final Provider<TextSettingsPresenter> settingsPresenterProvider,
                         final Provider<EditorPresenter> rawPresenterProvider,
                         final Provider<HtmlPresenter> htmlPresenterProvider,
                         final RestFactory restFactory,
                         final ClientSecurityContext securityContext) {
        super(eventBus, view, settingsPresenterProvider);
        this.rawPresenterProvider = rawPresenterProvider;
        this.htmlPresenterProvider = htmlPresenterProvider;
        this.restFactory = restFactory;
        this.securityContext = securityContext;

        view.setUiHandlers(this);
    }

    private void showData(final String data,
                          final String classification,
                          final Set<String> highlightStrings,
                          final boolean isHtml) {
//        final List<TextRange> highlights = getHighlights(data, highlightStrings);

        // Defer showing data to be sure that the data display has been made
        // visible first.
        Scheduler.get().scheduleDeferred(() -> {
            // Determine if we should show tha play button.
            playButtonVisible = !isHtml
                    && getTextSettings().isShowStepping()
                    && securityContext.hasAppPermission(PermissionNames.STEPPING_PERMISSION);

            // Show the play button if we have fetched input data.
            getView().setPlayVisible(playButtonVisible);

            getView().setClassification(classification);
            if (isHtml) {
                if (htmlPresenter == null) {
                    htmlPresenter = htmlPresenterProvider.get();
                    htmlPresenter.getWidget().addDomHandler(event -> {
                        final Element target = event.getNativeEvent().getEventTarget().cast();
                        final String link = target.getAttribute("link");
                        if (link != null) {
                            final Hyperlink hyperlink = Hyperlink.create(link);
                            if (hyperlink != null) {
                                HyperlinkEvent.fire(TextPresenter.this, hyperlink);
                            }
                        }
                    }, ClickEvent.getType());
                }

                getView().setContent(htmlPresenter.getView());
                htmlPresenter.setHtml(data);
            } else {
                if (rawPresenter == null) {
                    rawPresenter = rawPresenterProvider.get();
                    rawPresenter.setReadOnly(true);
                    rawPresenter.getLineNumbersOption().setOn(false);
                    rawPresenter.getLineWrapOption().setOn(true);
                }

                getView().setContent(rawPresenter.getView());

                if (highlightStrings != null && !highlightStrings.isEmpty()) {
                    rawPresenter.setFormattedHighlights(formattedText ->
                            getHighlights(formattedText, highlightStrings));
                } else {
                    rawPresenter.setFormattedHighlights(null);
                }

                rawPresenter.setText(data, true);
//                rawPresenter.setHighlights(highlights);
                rawPresenter.setControlsVisible(playButtonVisible);
            }
        });
    }

    /**
     * The ranges returned are line/col positions in the input text. Thus the input text should
     * not be changed/formatted after the highlight ranges have been generated.
     */
    private List<TextRange> getHighlights(final String input, final Set<String> highlightStrings) {
        final List<TextRange> highlights = new ArrayList<>();

        // See if we are going to add highlights.
        if (highlightStrings != null && highlightStrings.size() > 0) {
            final char[] inputChars = input.toLowerCase().toCharArray();
            final int inputLength = inputChars.length;

            // Find out where the highlight elements need to be placed.
            for (final String highlight : highlightStrings) {
                final char[] highlightChars = highlight.toLowerCase().toCharArray();
                final int highlightLength = highlightChars.length;

                boolean inElementTag = false;
                boolean inEscapedElement = false;
                int lineNo = 1;
                int colNo = 1;
                char lastInputChar = 0;
                for (int i = 0; i < inputLength; i++) {
                    final char inputChar = inputChars[i];

                    if (lastInputChar == '\n') {
                        lineNo++;
                        colNo = 1;
                    }
                    lastInputChar = inputChar;

                    if (!inElementTag && !inEscapedElement) {
                        if (inputChar == '<') {
                            inElementTag = true;
                        } else if (inputChar == '&'
                                && i + 3 < inputLength
                                && inputChars[i + 1] == 'l'
                                && inputChars[i + 2] == 't'
                                && inputChars[i + 3] == ';') {
                            inEscapedElement = true;
                        } else {
                            // If we aren't in an element or escaped element
                            // then try to match.
                            boolean found = false;
                            for (int j = 0; j < highlightLength && i + j < inputLength; j++) {
                                final char highlightChar = highlightChars[j];
                                if (inputChars[i + j] != highlightChar) {
                                    break;
                                } else if (j == highlightLength - 1) {
                                    found = true;
                                }
                            }

                            if (found) {
                                // All one based and inclusive
                                final TextRange hl = new TextRange(
                                        new DefaultLocation(lineNo, colNo),
                                        new DefaultLocation(lineNo, colNo + highlightLength - 1)); // inclusive
                                highlights.add(hl);

                                i += highlightLength;
                                // carry on looking for more instances of this highlight string
                            }
                        }
                    } else if (inElementTag && inputChar == '>') {
                        inElementTag = false;

                    } else if (inEscapedElement
                            && inputChar == '&'
                            && i + 3 < inputLength
                            && inputChars[i + 1] == 'g'
                            && inputChars[i + 2] == 't'
                            && inputChars[i + 3] == ';') {
                        inEscapedElement = false;
                    }

                    colNo++;
                }
            }
        }

        Collections.sort(highlights);

        return highlights;
    }

    @Override
    public void setComponents(final Components components) {
        super.setComponents(components);
        registerHandler(components.addComponentChangeHandler(event -> {
            if (getTextSettings() != null) {
                final Component component = event.getComponent();
                if (getTextSettings().getTableId() == null) {
                    if (component instanceof TablePresenter) {
                        currentTablePresenter = (TablePresenter) component;
                        update(currentTablePresenter);
                    }
                } else if (EqualsUtil.isEquals(getTextSettings().getTableId(), event.getComponentId())) {
                    if (component instanceof TablePresenter) {
                        currentTablePresenter = (TablePresenter) component;
                        update(currentTablePresenter);
                    }
                }
            }
        }));
    }

    private void update(final TablePresenter tablePresenter) {
        boolean updating = false;
        String message = "";

        final String permissionCheck = checkPermissions();
        if (permissionCheck != null) {
            isHtml = false;
            showData(permissionCheck, null, null, isHtml);
            updating = true;

        } else {
            currentStreamId = null;
            currentPartNo = null;
            currentRecordNo = null;
            currentHighlightStrings = null;

            if (tablePresenter != null) {
                final List<TableRow> selection = tablePresenter.getSelectedRows();
                if (selection != null && selection.size() == 1) {
                    final Field streamIdField = chooseBestField(tablePresenter, getTextSettings().getStreamIdField());
                    final Field partNoField = chooseBestField(tablePresenter, getTextSettings().getPartNoField());
                    final Field recordNoField = chooseBestField(tablePresenter, getTextSettings().getRecordNoField());
                    final Field lineFromField = chooseBestField(tablePresenter, getTextSettings().getLineFromField());
                    final Field colFromField = chooseBestField(tablePresenter, getTextSettings().getColFromField());
                    final Field lineToField = chooseBestField(tablePresenter, getTextSettings().getLineToField());
                    final Field colToField = chooseBestField(tablePresenter, getTextSettings().getColToField());

                    // Just use the first row.
                    final TableRow selected = selection.get(0);
                    currentStreamId = getLong(streamIdField, selected);
                    currentPartNo = getLong(partNoField, selected);
                    currentRecordNo = getLong(recordNoField, selected);
                    final Long currentLineFrom = getLong(lineFromField, selected);
                    final Long currentColFrom = getLong(colFromField, selected);
                    final Long currentLineTo = getLong(lineToField, selected);
                    final Long currentColTo = getLong(colToField, selected);

                    // Validate settings.
                    if (getTextSettings().getStreamIdField() == null) {
                        message = "No stream id field is configured";

                    } else if (getTextSettings().getStreamIdField() != null && currentStreamId == null) {
                        message = "No stream id found in selection";

                    } else if (getTextSettings().getRecordNoField() == null
                            && !(
                            getTextSettings().getLineFromField() != null
                                    && getTextSettings().getLineToField() != null)) { // Allow just line positions to
                        //                                                               be used rather than record no.
                        message = "No record number field is configured";

                    } else if (getTextSettings().getRecordNoField() != null && currentRecordNo == null) {
                        message = "No record number field found in selection";

                    } else {
                        DataRange dataRange = null;
                        TextRange highlight = null;
                        if (currentLineFrom != null
                                && currentColFrom != null
                                && currentLineTo != null
                                && currentColTo != null) {
                            dataRange = DataRange.between(
                                    new DefaultLocation(currentLineFrom.intValue(), currentColFrom.intValue()),
                                    new DefaultLocation(currentLineTo.intValue(), currentColTo.intValue()));

                            highlight = new TextRange(
                                    new DefaultLocation(currentLineFrom.intValue(), currentColFrom.intValue()),
                                    new DefaultLocation(currentLineTo.intValue(), currentColTo.intValue()));
                        }

                        final SourceLocation sourceLocation = SourceLocation.builder(currentStreamId)
                                .withPartNo(currentPartNo != null
                                        ? currentPartNo - 1
                                        : 0) // make zero based
                                .withSegmentNumber(currentRecordNo != null
                                        ? currentRecordNo - 1
                                        : 0) // make zero based
                                .withDataRange(dataRange)
                                .withHighlight(highlight)
                                .build();

                        currentHighlightStrings = tablePresenter.getHighlights();

//                        OffsetRange currentStreamRange = new OffsetRange(sourceLocation.getPartNo() - 1, 1L);

//                        Builder dataRangeBuilder = DataRange.builder(currentStreamId)
//                                .withPartNumber(sourceLocation.getPartNo() - 1) // make zero based
//                                .withChildStreamType(null);

//                        request.setStreamId(currentStreamId);
//                        request.setStreamRange(currentStreamRange);
//                        request.setChildStreamType(null);

                        // If we have a source highlight then use it.
//                        if (highlight != null) {
//                            // lines 2=>3 means lines 2 & 3, lines 4=>4 means line 4
//                            // -1 to offset to make zero based
//                            // +1 to length to make inclusive
//                            currentPageRange = new OffsetRange(
//                                    highlight.getFrom().getLineNo() - 1L,
//                                    (long) highlight.getTo().getLineNo() - highlight.getFrom().getLineNo() + 1);
//                        } else {
//                            currentPageRange = new OffsetRange(sourceLocation.getRecordNo() - 1L, 1L);
//                        }

                        // TODO @AT Fix/implement
                        // If we have a source highlight then use it.
//                        if (highlight != null) {
////                            dataRangeBuilder
////                                    .fromLocation(highlight.getFrom())
////                                    .toLocation(highlight.getTo());
//                        } else {
////                            // TODO assume this is segmented data
//////                            currentPageRange = new OffsetRange(sourceLocation.getRecordNo() - 0L, 1L);
//////                            request.setPageRange(new OffsetRange(sourceLocation.getRecordNo() - 1L, 1L));
////
////                            // Convert it to zero based
////                            dataRangeBuilder.withSegmentNumber(sourceLocation.getSegmentNo() - 1L);
//                        }

                        final FetchDataRequest request = new FetchDataRequest(sourceLocation);
                        request.setPipeline(getTextSettings().getPipeline());
                        request.setShowAsHtml(getTextSettings().isShowAsHtml());

                        ensureFetchDataQueue();
                        fetchDataQueue.add(request);
                        delayedFetchDataTimer.cancel();
                        delayedFetchDataTimer.schedule(250);
                        updating = true;
                    }
                }
            }
        }

        // If we aren't updating the data display then clear it.
        if (!updating) {
            showData(message, null, null, isHtml);
        }
    }

    private Field chooseBestField(final TablePresenter tablePresenter, final Field field) {
        if (field != null) {
            final TableComponentSettings tableComponentSettings = tablePresenter.getTableSettings();
            final List<Field> fields = tableComponentSettings.getFields();
            // Try and choose by id.
            for (final Field f : fields) {
                if (f.getId() != null && f.getId().equals(field.getId())) {
                    return f;
                }
            }
            // Try and choose by name.
            for (final Field f : fields) {
                if (f.getName() != null && f.getName().equals(field.getName())) {
                    return f;
                }
            }
        }
        return null;
    }

    private long getStartLine(final TextRange highlight) {
        int lineNoFrom = highlight.getFrom().getLineNo();
        if (lineNoFrom == 1) {
            // Content starts on first line so convert to an offset as the server code
            // works in zero based line numbers
            return lineNoFrom - 1;
        } else {
            //
            return lineNoFrom;
        }
    }

    private long getLineCount(final TextRange highlight) {
        int lineNoFrom = highlight.getFrom().getLineNo();
        if (lineNoFrom == 1) {
            return highlight.getTo().getLineNo() - highlight.getFrom().getLineNo() + 1;
        } else {
            return highlight.getTo().getLineNo() - highlight.getFrom().getLineNo();
        }
    }

//    private Long getLong(final Field field, List<Field> fields, final Row row) {
//        if (field != null && fields != null && row != null) {
//            int index = -1;
//
//            if (index == -1 && field.getId() != null) {
//                // Try matching on id alone.
//                for (int i = 0; i < fields.size(); i++) {
//                    if (field.getId().equals(fields.get(i).getId())) {
//                        index = i;
//                        break;
//                    }
//                }
//            }
//
//            if (index == -1 && field.getName() != null) {
//                // Try matching on name alone.
//                for (int i = 0; i < fields.size(); i++) {
//                    if (field.getName().equals(fields.get(i).getName())) {
//                        index = i;
//                        break;
//                    }
//                }
//            }
//
//            if (index != -1) {
//                if (row.getValues().size() > index) {
//                    return getLong(row.getValues().get(index));
//                }
//            }
//        }
//        return null;
//    }

    private Long getLong(final Field field, final TableRow row) {
        if (field != null && row != null) {
            return getLong(row.getText(field.getId()));
        }
        return null;
    }

    private Long getLong(final String string) {
        if (string != null) {
            try {
                return Long.valueOf(string);
            } catch (final NumberFormatException e) {
                // Ignore.
            }
        }

        return null;
    }

    private String checkPermissions() {
        if (!securityContext.hasAppPermission(PermissionNames.VIEW_DATA_PERMISSION)) {
            if (!securityContext.hasAppPermission(PermissionNames.VIEW_DATA_WITH_PIPELINE_PERMISSION)) {
                return "You do not have permission to display this item";
            } else if (getTextSettings().getPipeline() == null) {
                return "You must choose a pipeline to display this item";
            }
        }

        return null;
    }

    private void ensureFetchDataQueue() {
        if (fetchDataQueue == null) {
            fetchDataQueue = new ArrayList<>();
            delayedFetchDataTimer = new Timer() {
                @Override
                public void run() {
                    final FetchDataRequest request = fetchDataQueue.get(fetchDataQueue.size() - 1);
                    fetchDataQueue.clear();

                    final Rest<AbstractFetchDataResult> rest = restFactory.create();
                    rest
                            .onSuccess(result -> {
                                // If we are queueing more actions then don't update
                                // the text.
                                if (fetchDataQueue.size() == 0) {
                                    String data = "The data has been deleted or reprocessed since this index was built";
                                    String classification = null;
                                    boolean isHtml = false;
                                    if (result != null) {
                                        if (result instanceof FetchDataResult) {
                                            final FetchDataResult fetchDataResult = (FetchDataResult) result;
                                            data = fetchDataResult.getData();
                                            classification = result.getClassification();
                                            isHtml = fetchDataResult.isHtml();
                                        } else {
                                            data = "";
                                            classification = null;
                                            isHtml = false;
                                        }
                                    }

                                    TextPresenter.this.isHtml = isHtml;
                                    showData(data, classification, currentHighlightStrings, isHtml);
                                }
                            })
                            .call(DATA_RESOURCE)
                            .fetch(request);
                }
            };
        }
    }

    @Override
    public void read(final ComponentConfig componentConfig) {
        super.read(componentConfig);

        final TextComponentSettings.Builder builder;
        final ComponentSettings settings = componentConfig.getSettings();

        if (settings instanceof TextComponentSettings) {
            final TextComponentSettings textComponentSettings = (TextComponentSettings) settings;
            builder = textComponentSettings.copy();

            final Version version = Version.parse(textComponentSettings.getModelVersion());
            final boolean old = version.lt(CURRENT_MODEL_VERSION);

            // special field names have changed from EventId to __event_id__ so we need to deal
            // with those and replace them, also rebuild existing special fields just in case
            if (textComponentSettings.getStreamIdField() == null
                    || (old && IndexConstants.STREAM_ID.equals(textComponentSettings.getStreamIdField().getName()))
                    || (old && textComponentSettings.getStreamIdField().isSpecial())) {
                builder.streamIdField(TablePresenter.buildSpecialField(IndexConstants.STREAM_ID));
            }
            if (textComponentSettings.getRecordNoField() == null
                    || (old && IndexConstants.EVENT_ID.equals(textComponentSettings.getRecordNoField().getName()))
                    || (old && textComponentSettings.getRecordNoField().isSpecial())) {
                builder.recordNoField(TablePresenter.buildSpecialField(IndexConstants.EVENT_ID));
            }

        } else {
            builder = TextComponentSettings.builder();
            builder.streamIdField(TablePresenter.buildSpecialField(IndexConstants.STREAM_ID));
            builder.recordNoField(TablePresenter.buildSpecialField(IndexConstants.EVENT_ID));
        }

        builder.modelVersion(CURRENT_MODEL_VERSION.toString());
        setSettings(builder.build());
    }

    private TextComponentSettings getTextSettings() {
        return (TextComponentSettings) getSettings();
    }

    @Override
    public void link() {
        final String tableId = getTextSettings().getTableId();
        String newTableId = getComponents().validateOrGetFirstComponentId(tableId, TablePresenter.TYPE.getId());

        // If we can't get the same table id then set to null so that changes to any table can be listened to.
        if (!EqualsUtil.isEquals(tableId, newTableId)) {
            newTableId = null;
        }

        setSettings(getTextSettings()
                .copy()
                .tableId(newTableId)
                .build());
        update(currentTablePresenter);
    }

    @Override
    public void changeSettings() {
        super.changeSettings();
        update(currentTablePresenter);
    }

    @Override
    public ComponentType getType() {
        return TYPE;
    }

    @Override
    public void beginStepping() {
        if (currentStreamId != null) {
            final StepLocation stepLocation = new StepLocation(
                    currentStreamId,
                    currentPartNo != null
                            ? currentPartNo
                            : 1,
                    currentRecordNo != null
                            ? currentRecordNo
                            : -1);

            BeginPipelineSteppingEvent.fire(
                    this,
                    currentStreamId,
                    null,
                    null,
                    stepLocation,
                    null);
        } else {
            AlertEvent.fireError(this, "No stream id", null);
        }
    }

    public interface TextView extends View, HasUiHandlers<TextUiHandlers> {

        void setContent(View view);

        void setClassification(String classification);

        void setPlayVisible(boolean visible);
    }
}
