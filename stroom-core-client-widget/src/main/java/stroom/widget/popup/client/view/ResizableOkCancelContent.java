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

package stroom.widget.popup.client.view;

import stroom.widget.popup.client.presenter.PopupUiHandlers;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class ResizableOkCancelContent extends ResizeComposite implements HasEnabled {

    private static final Binder binder = GWT.create(Binder.class);
    private final PopupUiHandlers popupUiHandlers;
    @UiField
    Button ok;
    @UiField
    Button cancel;
    @UiField
    ResizeLayoutPanel content;

    public ResizableOkCancelContent(final PopupUiHandlers popupUiHandlers) {
        initWidget(binder.createAndBindUi(this));
        this.popupUiHandlers = popupUiHandlers;
    }

    public void setContent(final Widget widget) {
        content.setWidget(widget);
        ok.setEnabled(true);
        cancel.setEnabled(true);
    }

    @UiHandler("ok")
    public void onOkClick(final ClickEvent event) {
        popupUiHandlers.onHideRequest(false, true);
    }

    @UiHandler("cancel")
    public void onCancelClick(final ClickEvent event) {
        popupUiHandlers.onHideRequest(false, false);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        ok.setEnabled(enabled);
        cancel.setEnabled(enabled);
    }


    public interface Binder extends UiBinder<Widget, ResizableOkCancelContent> {

    }
}
