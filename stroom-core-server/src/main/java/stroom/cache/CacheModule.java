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

package stroom.cache;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import stroom.entity.shared.Clearable;
import stroom.task.TaskHandler;
import stroom.task.cluster.ClusterDispatchAsyncHelper;
import stroom.util.cache.CacheManager;
import stroom.util.spring.StroomScope;

public class CacheModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StroomCacheManager.class).to(StroomCacheManagerImpl.class);

        final Multibinder<Clearable> clearableBinder = Multibinder.newSetBinder(binder(), Clearable.class);
        clearableBinder.addBinding().to(StroomCacheManagerImpl.class);

        final Multibinder<TaskHandler> taskHandlerBinder = Multibinder.newSetBinder(binder(), TaskHandler.class);
        taskHandlerBinder.addBinding().to(stroom.cache.CacheClearHandler.class);
        taskHandlerBinder.addBinding().to(stroom.cache.FetchCacheNodeRowHandler.class);
        taskHandlerBinder.addBinding().to(stroom.cache.FetchCacheRowHandler.class);
    }


    //    @Bean
//    @Scope(StroomScope.TASK)
//    public CacheClearHandler cacheClearHandler(final ClusterDispatchAsyncHelper dispatchHelper) {
//        return new CacheClearHandler(dispatchHelper);
//    }
//
//    @Bean
//    @Scope(StroomScope.TASK)
//    public FetchCacheNodeRowHandler fetchCacheNodeRowHandler(final ClusterDispatchAsyncHelper dispatchHelper) {
//        return new FetchCacheNodeRowHandler(dispatchHelper);
//    }
//
//    @Bean
//    @Scope(StroomScope.TASK)
//    public FetchCacheRowHandler fetchCacheRowHandler(final CacheManager cacheManager) {
//        return new FetchCacheRowHandler(cacheManager);
//    }
//
//    @Bean
//    public StroomCacheManager stroomCacheManager(final CacheManager cacheManager) {
//        return new StroomCacheManagerImpl(cacheManager);
//    }
}