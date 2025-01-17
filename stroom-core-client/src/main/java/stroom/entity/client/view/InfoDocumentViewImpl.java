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

package stroom.entity.client.view;

import stroom.entity.client.presenter.InfoDocumentPresenter;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

public class InfoDocumentViewImpl extends ViewImpl implements InfoDocumentPresenter.InfoDocumentView {

    private final TextArea widget;

    @Inject
    public InfoDocumentViewImpl(final Resources resources) {
        resources.style().ensureInjected();
        widget = new TextArea();
        widget.setReadOnly(true);
        widget.setStyleName(resources.style().layout());
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void setInfo(final String string) {
        widget.setText(string);
    }

    public interface Style extends CssResource {

        String DEFAULT_STYLE = "Info.css";

        String layout();
    }

    public interface Resources extends ClientBundle {

        @Source(Style.DEFAULT_STYLE)
        Style style();
    }
}
