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

package stroom.feed.client.view;

import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.feed.client.presenter.FeedSettingsPresenter.FeedSettingsView;
import stroom.feed.client.presenter.SupportedRetentionAge;
import stroom.feed.shared.FeedDoc;
import stroom.feed.shared.FeedDoc.FeedStatus;
import stroom.item.client.ItemListBox;
import stroom.item.client.StringListBox;
import stroom.widget.tickbox.client.view.TickBox;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

public class FeedSettingsViewImpl extends ViewImpl implements FeedSettingsView, ReadOnlyChangeHandler {

    private final Widget widget;

    @UiField
    TextArea description;
    @UiField
    TextBox classification;
    @UiField
    StringListBox dataEncoding;
    @UiField
    StringListBox contextEncoding;
    @UiField
    ItemListBox<FeedDoc.FeedStatus> feedStatus;
    @UiField
    StringListBox receivedType;
    @UiField
    ItemListBox<SupportedRetentionAge> retentionAge;
    @UiField
    TickBox reference;

    @Inject
    public FeedSettingsViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public TextArea getDescription() {
        return description;
    }

    @Override
    public TextBox getClassification() {
        return classification;
    }

    @Override
    public TickBox getReference() {
        return reference;
    }

    @Override
    public StringListBox getDataEncoding() {
        return dataEncoding;
    }

    @Override
    public StringListBox getContextEncoding() {
        return contextEncoding;
    }

    @Override
    public StringListBox getReceivedType() {
        return receivedType;
    }

    @Override
    public ItemListBox<SupportedRetentionAge> getRetentionAge() {
        return retentionAge;
    }

    @Override
    public ItemListBox<FeedStatus> getFeedStatus() {
        return feedStatus;
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        description.setEnabled(!readOnly);
        classification.setEnabled(!readOnly);
        dataEncoding.setEnabled(!readOnly);
        contextEncoding.setEnabled(!readOnly);
        receivedType.setEnabled(!readOnly);
        feedStatus.setEnabled(!readOnly);
        retentionAge.setEnabled(!readOnly);
        reference.setEnabled(!readOnly);
    }

    public interface Binder extends UiBinder<Widget, FeedSettingsViewImpl> {

    }
}
