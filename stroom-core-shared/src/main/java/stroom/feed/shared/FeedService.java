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

package stroom.feed.shared;

import stroom.entity.shared.DocumentEntityService;
import stroom.entity.shared.FindService;

public interface FeedService extends DocumentEntityService<Feed>, FindService<Feed, FindFeedCriteria> {
    String getDisplayClassification(final Feed feed);

    Feed loadByName(String name);
}