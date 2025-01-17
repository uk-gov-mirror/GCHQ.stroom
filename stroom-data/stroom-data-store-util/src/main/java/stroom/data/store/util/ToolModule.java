/*
 * Copyright 2018 Crown Copyright
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

package stroom.data.store.util;

import stroom.cluster.lock.mock.MockClusterLockModule;
import stroom.collection.mock.MockCollectionModule;
import stroom.dictionary.mock.MockWordListProviderModule;
import stroom.docrefinfo.mock.MockDocRefInfoModule;
import stroom.security.mock.MockSecurityContextModule;
import stroom.statistics.mock.MockInternalStatisticsModule;
import stroom.task.mock.MockTaskModule;
import stroom.util.db.ForceLegacyMigration;
import stroom.util.entityevent.EntityEventBus;
import stroom.util.io.HomeDirProvider;
import stroom.util.io.HomeDirProviderImpl;
import stroom.util.io.TempDirProvider;
import stroom.util.io.TempDirProviderImpl;
import stroom.util.servlet.MockServletModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ToolModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new stroom.activity.mock.MockActivityModule());
        install(new stroom.cache.impl.CacheModule());
        install(new MockClusterLockModule());
        install(new stroom.data.store.impl.fs.FsDataStoreModule());
        install(new stroom.data.store.impl.fs.db.FsDataStoreDbModule());
        install(new stroom.event.logging.impl.EventLoggingModule());
        install(new stroom.meta.impl.db.MetaDbModule());
        install(new stroom.meta.impl.MetaModule());
        install(new MockSecurityContextModule());
        install(new MockInternalStatisticsModule());
        install(new MockServletModule());
        install(new MockCollectionModule());
        install(new MockDocRefInfoModule());
        install(new MockWordListProviderModule());
        install(new MockTaskModule());

        // Not using all the DB modules so just bind to an empty anonymous class
        bind(ForceLegacyMigration.class).toInstance(new ForceLegacyMigration() {
        });

        bind(HomeDirProvider.class).to(HomeDirProviderImpl.class);
        bind(TempDirProvider.class).to(TempDirProviderImpl.class);
    }

    @Provides
    EntityEventBus entityEventBus() {
        return event -> {
        };
    }
}
