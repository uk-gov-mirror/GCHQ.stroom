/*
 * Copyright 2019 Crown Copyright
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

package stroom.search.extraction;

import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Val;
import stroom.pipeline.filter.AbstractXMLFilter;

import java.util.function.Consumer;

public abstract class AbstractSearchResultOutputFilter extends AbstractXMLFilter {

    FieldIndex fieldIndexes;
    Consumer<Val[]> consumer;
    int count;

    public void setup(final FieldIndex fieldIndexes, final Consumer<Val[]> consumer) {
        this.fieldIndexes = fieldIndexes;
        this.consumer = consumer;
    }

    public int getCount() {
        return count;
    }
}
