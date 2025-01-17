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

package stroom.editor.client.view;

import stroom.editor.client.event.FormatEvent;
import stroom.editor.client.event.FormatEvent.FormatHandler;
import stroom.editor.client.model.XmlFormatter;
import stroom.editor.client.presenter.Action;
import stroom.editor.client.presenter.EditorUiHandlers;
import stroom.editor.client.presenter.EditorView;
import stroom.editor.client.presenter.Option;
import stroom.util.shared.DefaultLocation;
import stroom.util.shared.Location;
import stroom.util.shared.Severity;
import stroom.util.shared.StoredError;
import stroom.util.shared.TextRange;
import stroom.widget.contextmenu.client.event.ContextMenuEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;
import edu.ycp.cs.dh.acegwt.client.ace.AceAnnotationType;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorCursorPosition;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;
import edu.ycp.cs.dh.acegwt.client.ace.AceMarkerType;
import edu.ycp.cs.dh.acegwt.client.ace.AceRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * This is a widget that can be used to edit text. It provides useful
 * functionality such as formatting, styling, line numbers and warning/error
 * markers.
 */
public class EditorViewImpl extends ViewWithUiHandlers<EditorUiHandlers> implements EditorView {

    private static final IndicatorPopup indicatorPopup = new IndicatorPopup();
    private static final boolean SHOW_INDICATORS_DEFAULT = false;
    private static volatile Binder binder;
    private static volatile Resources resources;
    private final Action formatAction;
    private final Option stylesOption;
    private final Option lineNumbersOption;
    private final Option indicatorsOption;
    private final Option lineWrapOption;
    private final Option showInvisiblesOption;
    private final Option useVimBindingsOption;
    private final Option basicAutoCompletionOption;
    private final Option snippetsOption;
    private final Option liveAutoCompletionOption;
    private final Option highlightActiveLineOption;

    @UiField(provided = true)
    DockLayoutPanel layout;
    @UiField
    Editor editor;
    @UiField
    RightBar rightBar;
    @UiField
    FlowPanel filterButtons;
    @UiField
    Image filterInactive;
    @UiField
    Image filterActive;

    private IndicatorLines indicators;
    private AceEditorMode mode = AceEditorMode.XML;
    private int firstLineNumber = 1;
    private Function<String, List<TextRange>> formattedHighlightsFunc;

    @Inject
    public EditorViewImpl() {
        if (binder == null) {
            binder = GWT.create(Binder.class);
            resources = GWT.create(Resources.class);
            resources.style().ensureInjected();
        }

        layout = new DockLayoutPanel(Unit.PX) {
            @Override
            public void onResize() {
                super.onResize();
                doLayout(SHOW_INDICATORS_DEFAULT);
            }
        };

        layout = binder.createAndBindUi(this);

        filterButtons.addDomHandler(event -> {
            if ((event.getNativeButton() & NativeEvent.BUTTON_LEFT) != 0) {
                if (getUiHandlers() != null) {
                    getUiHandlers().changeFilterSettings();
                }
            }
        }, ClickEvent.getType());

        filterButtons.setWidth(ScrollbarMetrics.getVerticalScrollBarWidth() + "px");
        filterButtons.setHeight(ScrollbarMetrics.getHorizontalScrollBarWidth() + "px");

        final int left = ((ScrollbarMetrics.getVerticalScrollBarWidth() - 10) / 2);
        final int top = ((ScrollbarMetrics.getHorizontalScrollBarWidth() - 10) / 2);
        filterInactive.getElement().getStyle().setLeft(left, Unit.PX);
        filterActive.getElement().getStyle().setLeft(left, Unit.PX);
        filterInactive.getElement().getStyle().setTop(top, Unit.PX);
        filterActive.getElement().getStyle().setTop(top, Unit.PX);

        formatAction = new Action("Format", false, this::format);

        // Don't forget to add any new options into EditorPresenter
        stylesOption = new Option(
                "Styles", true, true, (on) -> setMode(mode, on));
        lineNumbersOption = new Option(
                "Line Numbers", true, true, (on) -> editor.setShowGutter(on));
        indicatorsOption = new Option(
                "Indicators", SHOW_INDICATORS_DEFAULT, false, this::doLayout);
        lineWrapOption = new Option(
                "Wrap Lines", false, true, (on) -> editor.setUseWrapMode(on));
        showInvisiblesOption = new Option(
                "Show Hidden Characters", false, true, (on) -> editor.setShowInvisibles(on));
        useVimBindingsOption = new Option(
                "Vim Key Bindings", false, true, (on) -> editor.setUseVimBindings(on));
        basicAutoCompletionOption = new Option(
                "Auto Completion", true, true, (on) -> editor.setUseBasicAutoCompletion(on));
        liveAutoCompletionOption = new Option(
                "Live Auto Completion", false, true, (on) -> editor.setUseLiveAutoCompletion(on));
        snippetsOption = new Option(
                "Snippets", true, true, (on) -> editor.setUseSnippets(on));
        highlightActiveLineOption = new Option(
                "Highlight Active Line", true, true, (on) -> editor.setHighlightActiveLine(on));

        editor.getElement().setClassName("editor");
        editor.addDomHandler(event -> handleMouseDown(event), MouseDownEvent.getType());
        rightBar.setEditor(editor);
    }

    private void handleMouseDown(final MouseDownEvent event) {
        if ((NativeEvent.BUTTON_RIGHT & event.getNativeButton()) != 0) {
            ContextMenuEvent.fire(this, event.getClientX(), event.getClientY());
        } else {
            indicatorPopup.hide();
            MouseDownEvent.fireNativeEvent(event.getNativeEvent(), this);
        }
    }

    @Override
    public Widget asWidget() {
        return layout;
    }

    private void updateGutter(final boolean showLineNumbers) {
        editor.setShowGutter(showLineNumbers);
    }

    private void doLayout() {
        doLayout(indicatorsOption.isOn());
    }

    private void doLayout(final boolean showIndicators) {
        rightBar.render(indicators, showIndicators);
        layout.setWidgetSize(rightBar, rightBar.getWidth());
        editor.onResize();
    }

    @Override
    public String getEditorId() {
        return editor.getId();
    }

    @Override
    public void focus() {
        editor.focus();
    }

    @Override
    public String getText() {
        return editor.getText();
    }

    @Override
    public void setText(final String text) {
        setText(text, false);
    }

    @Override
    public void setText(final String text, final boolean format) {
        if (text == null) {
            editor.setText("");
        } else {
            if (format) {
                final String formattedText = formatAsIfXml(text);
                editor.setText(formattedText);
                applyFormattedHighlights(formattedText);
            } else {
                editor.setText(text);
            }
        }
    }

    @Override
    public void insertTextAtCursor(final String text) {
        editor.insertTextAtCursor(text);
    }

    @Override
    public void replaceSelectedText(final String text) {
        editor.replaceSelectedText(text);
    }

    @Override
    public void insertSnippet(final String snippet) {
        editor.insertSnippet(snippet);
    }

    @Override
    public void setFirstLineNumber(final int firstLineNumber) {
        this.firstLineNumber = firstLineNumber;
        editor.setFirstLineNumber(firstLineNumber);
    }

    @Override
    public void setIndicators(final IndicatorLines indicators) {
        this.indicators = indicators;
        final List<Annotation> annotations = new ArrayList<>();

        if (indicators != null) {
            for (Integer lineNumber : indicators.getLineNumbers()) {
                final Indicator indicator = indicators.getIndicator(lineNumber);

                for (final Entry<Severity, Set<StoredError>> entry : indicator.getErrorMap().entrySet()) {
                    for (final StoredError error : entry.getValue()) {
                        int row = 0;
                        int col = 0;

                        final Location location = error.getLocation();
                        if (location != null) {
                            row = Math.max(location.getLineNo() - 1, 0);
                            col = location.getColNo();
                        }

                        final Severity severity = error.getSeverity();
                        AceAnnotationType annotationType = AceAnnotationType.INFO;
                        switch (severity) {
                            case INFO:
                                annotationType = AceAnnotationType.INFO;
                                break;
                            case WARNING:
                                annotationType = AceAnnotationType.WARNING;
                                break;
                            case ERROR:
                                annotationType = AceAnnotationType.ERROR;
                                break;
                            case FATAL_ERROR:
                                annotationType = AceAnnotationType.FATAL_ERROR;
                                break;
                            default:
                                throw new RuntimeException("Unexpected severity " + severity);
                        }

                        // Ace munges all the msgs together in one popup for annotaions on the same line/col
                        // so add the severity as if we have some info + warnings then we get a warning
                        // icon whose popup contains all the msgs.
                        annotations.add(new Annotation(
                                row,
                                col,
                                error.getSeverity().toString() + " " + error.getMessage(),
                                annotationType));
                    }
                }
            }
        }

        // Ensure line numbers are visible if there are annotations.
        if (annotations.size() > 0) {
            getLineNumbersOption().setOn(true);
        }

        editor.setAnnotations(annotations);
        doLayout(indicatorsOption.isOn());
    }

    @Override
    public void setFormattedHighlights(final Function<String, List<TextRange>> highlightsFunction) {
        this.formattedHighlightsFunc = highlightsFunction;
    }

    @Override
    public void setHighlights(final List<TextRange> highlights) {

        Scheduler.get().scheduleDeferred(() -> {
            if (highlights != null && highlights.size() > 0) {
                // Find our first from location
                final Location minLocation = highlights.stream()
                        .map(TextRange::getFrom)
                        .min(Location::compareTo)
                        .orElse(DefaultLocation.beginning());

                final List<Marker> markers = highlights.stream()
                        .map(highlightStr -> {
                            // Location is all one based and exclusive, AceRange is a mix
                            return AceRange.create(
                                    highlightStr.getFrom().getLineNo() - firstLineNumber,
                                    highlightStr.getFrom().getColNo() - 1, // zero-based & inclusive
                                    highlightStr.getTo().getLineNo() - firstLineNumber,
                                    highlightStr.getTo().getColNo()); // zero-based & exclusive so no need to change
                        })
                        .map(aceRange ->
                                new Marker(aceRange, "hl", AceMarkerType.TEXT, false))
                        .collect(Collectors.toList());

                editor.setMarkers(markers);
                // Move the cursor location so the highlight is in view.  Line and col as we
                // could be dealing with single line data with the highlight at col 500 for example.
                editor.gotoLocation(
                        minLocation.getLineNo() - firstLineNumber + 1,
                        minLocation.getColNo());
            } else {
                editor.setMarkers(null);
            }
        });
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        editor.setReadOnly(readOnly);
    }

    @Override
    public void setMode(final AceEditorMode mode) {
        this.mode = mode;
        if (stylesOption.isOn()) {
            editor.setMode(mode);
        } else {
            editor.setMode(AceEditorMode.TEXT);
        }
    }

    public void setMode(final AceEditorMode mode, final boolean areStylesEnabled) {
        this.mode = mode;
        if (areStylesEnabled) {
            editor.setMode(mode);
        } else {
            editor.setMode(AceEditorMode.TEXT);
        }
    }

    @Override
    public void setTheme(final AceEditorTheme theme) {
        editor.setTheme(theme);
    }

    private String formatAsIfXml(final String text) {
        return new XmlFormatter().format(text);
    }

    /**
     * Formats the currently displayed text.
     */
    private void format() {
        Scheduler.get().scheduleDeferred(() -> {
            final int scrollTop = editor.getScrollTop();
            final AceEditorCursorPosition cursorPosition = editor.getCursorPosition();

            final String formattedText;
            if (AceEditorMode.XML.equals(mode)) {
                formattedText = formatAsIfXml(getText());
                setText(formattedText);
            } else {
                editor.beautify();
                formattedText = editor.getText();
            }

            if (cursorPosition != null) {
                editor.moveTo(cursorPosition.getRow(), cursorPosition.getColumn());
            }
            if (scrollTop > 0) {
                editor.setScrollTop(scrollTop);
            }

            editor.focus();

            FormatEvent.fire(this);

            applyFormattedHighlights(formattedText);
        });
    }

    private void applyFormattedHighlights(final String formattedText) {
        if (formattedHighlightsFunc != null) {
            final List<TextRange> highlightRanges = formattedHighlightsFunc.apply(formattedText);
            setHighlights(highlightRanges);
        }
    }

    @Override
    public Action getFormatAction() {
        return formatAction;
    }

    @Override
    public Option getStylesOption() {
        return stylesOption;
    }

    @Override
    public Option getLineNumbersOption() {
        return lineNumbersOption;
    }

    @Override
    public Option getIndicatorsOption() {
        return indicatorsOption;
    }

    @Override
    public Option getLineWrapOption() {
        return lineWrapOption;
    }

    @Override
    public Option getShowInvisiblesOption() {
        return showInvisiblesOption;
    }

    @Override
    public Option getUseVimBindingsOption() {
        return useVimBindingsOption;
    }

    @Override
    public Option getBasicAutoCompletionOption() {
        return basicAutoCompletionOption;
    }

    @Override
    public Option getSnippetsOption() {
        return snippetsOption;
    }

    @Override
    public Option getLiveAutoCompletionOption() {
        return liveAutoCompletionOption;
    }

    @Override
    public Option getHighlightActiveLineOption() {
        return highlightActiveLineOption;
    }

    @Override
    public void showFilterButton(final boolean show) {
        filterActive.setVisible(false);
        filterButtons.setVisible(show);
    }

    @Override
    public void setFilterActive(final boolean active) {
        filterActive.setVisible(active);
    }

    @Override
    public void setControlsVisible(final boolean controlsVisible) {
        if (controlsVisible) {
            editor.setScrollMargin(0, 69, 0, 0);
        } else {
            editor.setScrollMargin(0, 0, 0, 0);
        }
    }

    @Override
    public HandlerRegistration addKeyDownHandler(final KeyDownHandler handler) {
        return layout.addDomHandler(handler, KeyDownEvent.getType());
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<String> handler) {
        return editor.addValueChangeHandler(handler);
    }

    @Override
    public HandlerRegistration addFormatHandler(final FormatHandler handler) {
        return layout.addHandler(handler, FormatEvent.TYPE);
    }

    @Override
    public HandlerRegistration addMouseDownHandler(final MouseDownHandler handler) {
        return layout.addHandler(handler, MouseDownEvent.getType());
    }

    @Override
    public HandlerRegistration addContextMenuHandler(final ContextMenuEvent.Handler handler) {
        return layout.addHandler(handler, ContextMenuEvent.getType());
    }

    @Override
    public void fireEvent(final GwtEvent<?> event) {
        layout.fireEvent(event);
    }

    @Override
    public void onResize() {
        doLayout();
    }

    /**
     * Declare styles.
     */
    public interface Style extends CssResource {

        String filterButtons();

        String filterButton();
    }

    /**
     * Bundle for the indicator icons and styles.
     */
    public interface Resources extends ClientBundle {

        ImageResource filterActive();

        ImageResource filterInactive();

        @Source("codeeditor.css")
        Style style();
    }

    public interface Binder extends UiBinder<DockLayoutPanel, EditorViewImpl> {

    }
}
