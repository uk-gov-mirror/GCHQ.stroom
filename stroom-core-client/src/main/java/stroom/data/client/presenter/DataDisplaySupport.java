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

package stroom.data.client.presenter;

import stroom.data.client.DataPreviewTabPlugin;
import stroom.data.client.SourceTabPlugin;
import stroom.pipeline.shared.SourceLocation;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Provider;

public class DataDisplaySupport {

    private final Provider<ClassificationWrappedDataPresenter> dataPresenterProvider;
    private final Provider<DataPreviewTabPlugin> dataPreviewTabPluginProvider;

    private final Provider<ClassificationWrappedSourcePresenter> sourcePresenterProvider;
    private final Provider<SourceTabPlugin> sourceTabPluginProvider;

    @Inject
    public DataDisplaySupport(final EventBus eventBus,
                              final Provider<ClassificationWrappedDataPresenter> dataPresenterProvider,
                              final Provider<DataPreviewTabPlugin> dataPreviewTabPluginProvider,
                              final Provider<ClassificationWrappedSourcePresenter> sourcePresenterProvider,
                              final Provider<SourceTabPlugin> sourceTabPluginProvider) {

        this.dataPresenterProvider = dataPresenterProvider;
        this.dataPreviewTabPluginProvider = dataPreviewTabPluginProvider;
        this.sourcePresenterProvider = sourcePresenterProvider;
        this.sourceTabPluginProvider = sourceTabPluginProvider;

        eventBus.addHandler(ShowDataEvent.getType(), showDataEvent -> {

            switch (showDataEvent.getDisplayMode()) {
                case DIALOG:
                    openPopupDialog(showDataEvent);
                    break;
                case STROOM_TAB:
                    openStroomTab(showDataEvent);
                    break;
                default:
                    throw new RuntimeException("Unknown displayMode " + showDataEvent.getDisplayMode());
            }
        });
    }

    private void openStroomTab(final ShowDataEvent showDataEvent) {
        if (DataViewType.PREVIEW.equals(showDataEvent.getDataViewType())) {
            dataPreviewTabPluginProvider.get()
                    .open(showDataEvent.getSourceLocation(), true);
        } else {
            sourceTabPluginProvider.get()
                    .open(showDataEvent.getSourceLocation(), true);
        }
    }

    private void openPopupDialog(final ShowDataEvent showDataEvent) {
        final SourceLocation sourceLocation = showDataEvent.getSourceLocation();
        final ClassificationWrapperPresenter presenter;
        final String caption;

        if (DataViewType.PREVIEW.equals(showDataEvent.getDataViewType())) {
            final ClassificationWrappedDataPresenter dataPresenter = dataPresenterProvider.get();
            dataPresenter.setDisplayMode(showDataEvent.getDisplayMode());
            dataPresenter.fetchData(sourceLocation);
            presenter = dataPresenter;
            caption = "Stream "
                    + sourceLocation.getId();
        } else {
            final ClassificationWrappedSourcePresenter sourcePresenter = sourcePresenterProvider.get();
            sourcePresenter.setSourceLocationUsingHighlight(sourceLocation);
            presenter = sourcePresenter;
            // Convert to one based for UI;
            caption = "Stream "
                    + sourceLocation.getId() + ":"
                    + (sourceLocation.getPartNo() + 1) + ":"
                    + (sourceLocation.getSegmentNo() + 1);
        }

        final PopupSize popupSize = new PopupSize(
                1400,
                800,
                1000,
                600,
                true);

        ShowPopupEvent.fire(
                presenter,
                presenter,
                PopupType.OK_CANCEL_DIALOG,
                popupSize,
                caption,
                null);
    }
}
