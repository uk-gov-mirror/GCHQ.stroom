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

package stroom.security.client.presenter;

import stroom.entity.client.presenter.NameDocumentView;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;

public class ManageNewEntityPresenter extends MyPresenterWidget<NameDocumentView> {

    @Inject
    public ManageNewEntityPresenter(final EventBus eventBus, final NameDocumentView view) {
        super(eventBus, view);
    }

    public void show(final PopupUiHandlers popupUiHandlers) {
        getView().setUiHandlers(popupUiHandlers);
        getView().setName("");
        ShowPopupEvent.fire(this, this, PopupType.OK_CANCEL_DIALOG, "New", popupUiHandlers);
        getView().focus();
    }

    public void hide() {
        HidePopupEvent.fire(this, this);
    }

    public String getName() {
        return getView().getName();
    }
}
