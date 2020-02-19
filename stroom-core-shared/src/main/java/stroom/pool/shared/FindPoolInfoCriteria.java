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

package stroom.pool.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.util.shared.CompareUtil;
import stroom.util.shared.FindNamedEntityCriteria;
import stroom.util.shared.PageRequest;
import stroom.util.shared.Sort;
import stroom.util.shared.Sort.Direction;
import stroom.util.shared.StringCriteria;

import java.util.Comparator;
import java.util.List;

@JsonInclude(Include.NON_DEFAULT)
public class FindPoolInfoCriteria extends FindNamedEntityCriteria implements Comparator<PoolInfo> {
    public static final String FIELD_LAST_ACCESS = "Last Access";
    public static final String FIELD_IN_USE = "In Use";
    public static final String FIELD_IN_POOL = "In Pool";
    public static final String FIELD_IDLE_TIME = "Idle Time (s)";
    public static final String FIELD_LIVE_TIME = "Live Time (s)";

    @JsonCreator
    public FindPoolInfoCriteria(@JsonProperty("pageRequest") final PageRequest pageRequest,
                                @JsonProperty("sortList") final List<Sort> sortList,
                                @JsonProperty("name") final StringCriteria name) {
        super(pageRequest, sortList, name);
    }

    @Override
    public int compare(final PoolInfo o1, final PoolInfo o2) {
        if (getSortList() != null) {
            for (final Sort sort : getSortList()) {
                final String field = sort.getField();

                int compare = 0;
                if (FIELD_NAME.equals(field)) {
                    compare = CompareUtil.compareString(o1.getName(), o2.getName());
                } else if (FIELD_LAST_ACCESS.equals(field)) {
                    compare = CompareUtil.compareLong(o1.getLastAccessTime(), o2.getLastAccessTime());
                } else if (FIELD_IN_USE.equals(field)) {
                    compare = CompareUtil.compareInteger(o1.getInUse(), o2.getInUse());
                } else if (FIELD_IN_POOL.equals(field)) {
                    compare = CompareUtil.compareInteger(o1.getInPool(), o2.getInPool());
                } else if (FIELD_IDLE_TIME.equals(field)) {
                    compare = CompareUtil.compareLong(o1.getTimeToIdleMs(), o2.getTimeToIdleMs());
                } else if (FIELD_LIVE_TIME.equals(field)) {
                    compare = CompareUtil.compareLong(o1.getTimeToLiveMs(), o2.getTimeToLiveMs());
                }
                if (Direction.DESCENDING.equals(sort.getDirection())) {
                    compare = compare * -1;
                }

                if (compare != 0) {
                    return compare;
                }
            }
        }

        return 0;
    }
}
