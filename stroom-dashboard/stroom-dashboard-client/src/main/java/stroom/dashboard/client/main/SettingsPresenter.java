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

package stroom.dashboard.client.main;

import stroom.dashboard.shared.ComponentConfig;
import stroom.widget.tab.client.presenter.LinkTabsLayoutView;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Layer;
import com.gwtplatform.mvp.client.MyPresenterWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SettingsPresenter extends MyPresenterWidget<LinkTabsLayoutView> {

    private final Map<TabData, Layer> tabViewMap = new HashMap<>();
    private final Map<TabData, ComponentDataModifier> modifiers = new HashMap<>();

    private TabData firstTab;
    private TabData selectedTab;
    private boolean firstShowing = true;

    @Inject
    public SettingsPresenter(final EventBus eventBus, final LinkTabsLayoutView view) {
        super(eventBus, view);
    }

    public TabData addTab(final String text, final Layer layer) {
        final TabData tab = new TabDataImpl(text, false);
        tabViewMap.put(tab, layer);
        getView().getTabBar().addTab(tab);

        if (layer instanceof ComponentDataModifier) {
            final ComponentDataModifier componentDataModifier = (ComponentDataModifier) layer;
            modifiers.put(tab, componentDataModifier);
        }

        if (firstTab == null) {
            firstTab = tab;
        }

        return tab;
    }

    public void removeTab(final TabData tab) {
        getView().getTabBar().removeTab(tab);
        changeSelectedTab(firstTab);
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(getView().getTabBar().addSelectionHandler(event -> {
            final TabData tab = event.getSelectedItem();
            if (tab != null && tab != selectedTab) {
                changeSelectedTab(tab);
            }
        }));
    }

    public void setComponents(final Components components) {
        for (final ComponentDataModifier modifier : modifiers.values()) {
            modifier.setComponents(components);
        }

        if (firstShowing) {
            firstShowing = false;
            if (firstTab != null) {
                changeSelectedTab(firstTab);
            }
        }
    }

    public void read(final ComponentConfig componentData) {
        for (final ComponentDataModifier modifier : modifiers.values()) {
            modifier.read(componentData);
        }
    }

    public ComponentConfig write(ComponentConfig componentConfig) {
        ComponentConfig result = componentConfig;
        for (final Entry<TabData, ComponentDataModifier> entry : modifiers.entrySet()) {
            if (!getView().getTabBar().isTabHidden(entry.getKey())) {
                result = entry.getValue().write(result);
            }
        }
        return result;
    }

    public boolean validate() {
        return !modifiers.values().stream().anyMatch(v -> !v.validate());
    }

    public boolean isDirty(final ComponentConfig componentData) {
        for (final Entry<TabData, ComponentDataModifier> entry : modifiers.entrySet()) {
            if (!getView().getTabBar().isTabHidden(entry.getKey())) {
                if (entry.getValue().isDirty(componentData)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void changeSelectedTab(final TabData tab) {
        if (selectedTab != tab) {
            selectedTab = tab;
            if (selectedTab != null) {
                final Layer layer = tabViewMap.get(selectedTab);
                if (layer != null) {
                    getView().getTabBar().selectTab(tab);
                    getView().getLayerContainer().show(layer);
                }
            }
        }
    }
}
