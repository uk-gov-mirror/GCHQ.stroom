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

package stroom.feed.client.gin;

import stroom.core.client.gin.PluginModule;
import stroom.data.client.presenter.DataUploadPresenter;
import stroom.data.client.presenter.DataUploadPresenter.DataUploadView;
import stroom.data.client.view.StreamUploadViewImpl;
import stroom.feed.client.FeedPlugin;
import stroom.feed.client.presenter.FeedPresenter;
import stroom.feed.client.presenter.FeedSettingsPresenter;
import stroom.feed.client.presenter.FeedSettingsPresenter.FeedSettingsView;
import stroom.feed.client.view.FeedSettingsViewImpl;

public class FeedModule extends PluginModule {

    @Override
    protected void configure() {
        bindPlugin(FeedPlugin.class);

        bind(FeedPresenter.class);

        bindSharedView(FeedSettingsView.class, FeedSettingsViewImpl.class);
        bind(FeedSettingsPresenter.class);

        bindPresenterWidget(DataUploadPresenter.class, DataUploadView.class, StreamUploadViewImpl.class);
    }
}
