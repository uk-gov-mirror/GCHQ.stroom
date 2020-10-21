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

package stroom.data.client.gin;

import stroom.core.client.gin.PluginModule;
import stroom.data.client.SourceTabPlugin;
import stroom.data.client.presenter.CharacterNavigatorPresenter;
import stroom.data.client.presenter.CharacterNavigatorPresenter.CharacterNavigatorView;
import stroom.data.client.presenter.CharacterRangeSelectionPresenter;
import stroom.data.client.presenter.CharacterRangeSelectionPresenter.CharacterRangeSelectionView;
import stroom.data.client.presenter.ClassificationWrapperPresenter;
import stroom.data.client.presenter.ClassificationWrapperPresenter.ClassificationWrapperView;
import stroom.data.client.presenter.DataPopupSupport;
import stroom.data.client.presenter.DataPresenter;
import stroom.data.client.presenter.DataPresenter.DataView;
import stroom.data.client.presenter.DataTypeUiManager;
import stroom.data.client.presenter.ExpressionPresenter;
import stroom.data.client.presenter.ExpressionPresenter.ExpressionView;
import stroom.data.client.presenter.ItemNavigatorPresenter;
import stroom.data.client.presenter.ItemNavigatorPresenter.ItemNavigatorView;
import stroom.data.client.presenter.ItemSelectionPresenter;
import stroom.data.client.presenter.ItemSelectionPresenter.ItemSelectionView;
import stroom.data.client.presenter.MetaListPresenter;
import stroom.data.client.presenter.MetaPresenter;
import stroom.data.client.presenter.MetaPresenter.StreamView;
import stroom.data.client.presenter.ProcessChoicePresenter;
import stroom.data.client.presenter.ProcessChoicePresenter.ProcessChoiceView;
import stroom.data.client.presenter.ProcessorTaskListPresenter;
import stroom.data.client.presenter.ProcessorTaskPresenter;
import stroom.data.client.presenter.ProcessorTaskPresenter.StreamTaskView;
import stroom.data.client.presenter.SourcePresenter;
import stroom.data.client.presenter.SourcePresenter.SourceView;
import stroom.data.client.presenter.SourceTabPresenter;
import stroom.data.client.presenter.SourceTabPresenter.SourceTabView;
import stroom.data.client.presenter.TextPresenter;
import stroom.data.client.presenter.TextPresenter.TextView;
import stroom.data.client.view.CharacterNavigatorViewImpl;
import stroom.data.client.view.CharacterRangeSelectionViewImpl;
import stroom.data.client.view.ClassificationWrapperViewImpl;
import stroom.data.client.view.DataViewImpl;
import stroom.data.client.view.ExpressionViewImpl;
import stroom.data.client.view.ItemNavigatorViewImpl;
import stroom.data.client.view.ItemSelectionViewImpl;
import stroom.data.client.view.ProcessChoiceViewImpl;
import stroom.data.client.view.SourceTabViewImpl;
import stroom.data.client.view.SourceViewImpl;
import stroom.data.client.view.StreamTaskViewImpl;
import stroom.data.client.view.StreamViewImpl;
import stroom.data.client.view.TextViewImpl;
import stroom.editor.client.presenter.DelegatingAceCompleter;
import stroom.editor.client.presenter.EditorPresenter;
import stroom.editor.client.presenter.EditorView;
import stroom.editor.client.view.EditorViewImpl;
import stroom.widget.dropdowntree.client.presenter.DropDownPresenter.DropDrownView;
import stroom.widget.dropdowntree.client.presenter.DropDownTreePresenter.DropDownTreeView;
import stroom.widget.dropdowntree.client.view.DropDownTreeViewImpl;
import stroom.widget.dropdowntree.client.view.DropDownViewImpl;

public class StreamStoreModule extends PluginModule {
    @Override
    protected void configure() {
        bind(DataTypeUiManager.class).asEagerSingleton();
        bind(DataPopupSupport.class).asEagerSingleton();

        bindPlugin(SourceTabPlugin.class);

        bind(DelegatingAceCompleter.class).asEagerSingleton();

        bindPresenterWidget(
                ClassificationWrapperPresenter.class,
                ClassificationWrapperView.class,
                ClassificationWrapperViewImpl.class);
        bindPresenterWidget(
                MetaPresenter.class,
                StreamView.class,
                StreamViewImpl.class);
        bindPresenterWidget(
                EditorPresenter.class,
                EditorView.class,
                EditorViewImpl.class);
        bindPresenterWidget(
                DataPresenter.class,
                DataView.class,
                DataViewImpl.class);
        bindPresenterWidget(
                TextPresenter.class,
                TextView.class,
                TextViewImpl.class);
        bindPresenterWidget(
                ProcessorTaskPresenter.class,
                StreamTaskView.class,
                StreamTaskViewImpl.class);
        bindPresenterWidget(
                ExpressionPresenter.class,
                ExpressionView.class,
                ExpressionViewImpl.class);
        bindPresenterWidget(
                ProcessChoicePresenter.class,
                ProcessChoiceView.class,
                ProcessChoiceViewImpl.class);
        bindPresenterWidget(
                SourceTabPresenter.class,
                SourceTabView.class,
                SourceTabViewImpl.class);
        bindPresenterWidget(
                SourcePresenter.class,
                SourceView.class,
                SourceViewImpl.class);
        bindPresenterWidget(
                CharacterRangeSelectionPresenter.class,
                CharacterRangeSelectionView.class,
                CharacterRangeSelectionViewImpl.class);
        bindPresenterWidget(
                CharacterNavigatorPresenter.class,
                CharacterNavigatorView.class,
                CharacterNavigatorViewImpl.class);
        bindPresenterWidget(
                ItemNavigatorPresenter.class,
                ItemNavigatorView.class,
                ItemNavigatorViewImpl.class);
        bindPresenterWidget(
                ItemSelectionPresenter.class,
                ItemSelectionView.class,
                ItemSelectionViewImpl.class);
        bind(MetaListPresenter.class);

        bind(ProcessorTaskListPresenter.class);

        bindSharedView(DropDrownView.class, DropDownViewImpl.class);
        bindSharedView(DropDownTreeView.class, DropDownTreeViewImpl.class);
    }
}
