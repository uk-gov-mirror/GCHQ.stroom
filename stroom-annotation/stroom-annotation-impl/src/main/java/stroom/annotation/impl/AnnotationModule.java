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

package stroom.annotation.impl;

import com.google.inject.AbstractModule;
import stroom.annotation.api.AnnotationCreator;
import stroom.annotation.shared.AnnotationDetail;
import stroom.event.logging.api.ObjectInfoProviderBinder;
import stroom.search.extraction.AnnotationsDecoratorFactory;
import stroom.util.guice.GuiceUtil;
import stroom.util.shared.RestResource;

public class AnnotationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind (AnnotationCreator.class).to(AnnotationService.class);

        bind(AnnotationsDecoratorFactory.class).to(AnnotationReceiverDecoratorFactory.class);

        GuiceUtil.buildMultiBinder(binder(), RestResource.class)
                .addBinding(AnnotationResourceImpl.class);

        // Provide object info to the logging service.
        ObjectInfoProviderBinder.create(binder())
                .bind(AnnotationDetail.class, AnnotationEventInfoProvider.class);
    }
}