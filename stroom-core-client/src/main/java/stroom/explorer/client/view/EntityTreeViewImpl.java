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

package stroom.explorer.client.view;

import stroom.explorer.client.presenter.EntityTreePresenter;
import stroom.explorer.client.presenter.EntityTreeUiHandlers;
import stroom.explorer.shared.ExplorerTreeFilter;
import stroom.widget.dropdowntree.client.view.QuickFilter;
import stroom.widget.dropdowntree.client.view.QuickFilterTooltipUtil;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.MaxScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class EntityTreeViewImpl extends ViewWithUiHandlers<EntityTreeUiHandlers>
        implements EntityTreePresenter.EntityTreeView {

    private final Widget widget;
    @UiField
    QuickFilter nameFilter;
    @UiField
    MaxScrollPanel scrollPanel;

    @Inject
    public EntityTreeViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);

        // Same field defs as the Explorer Tree
        nameFilter.registerPopupTextProvider(() -> QuickFilterTooltipUtil.createTooltip(
                "Choose Item Quick Filter",
                ExplorerTreeFilter.FIELD_DEFINITIONS));
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @UiHandler("nameFilter")
    void onFilterChange(final ValueChangeEvent<String> event) {
        getUiHandlers().changeFilter(nameFilter.getText());
    }

    @Override
    public void setCellTree(Widget cellTree) {
        scrollPanel.setWidget(cellTree);
    }

    public interface Binder extends UiBinder<Widget, EntityTreeViewImpl> {

    }
}
