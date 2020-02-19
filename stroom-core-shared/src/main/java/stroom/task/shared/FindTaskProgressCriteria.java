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

package stroom.task.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import stroom.util.shared.BaseCriteria;
import stroom.util.shared.PageRequest;
import stroom.util.shared.Sort;
import stroom.util.shared.Sort.Direction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonInclude(Include.NON_DEFAULT)
public class FindTaskProgressCriteria extends BaseCriteria {
    public static final String FIELD_NODE = "Node";
    public static final String FIELD_NAME = "Name";
    public static final String FIELD_USER = "User";
    public static final String FIELD_SUBMIT_TIME = "Submit Time";
    public static final String FIELD_AGE = "Age";
    public static final String FIELD_INFO = "Info";

    @JsonProperty
    private Set<TaskProgress> expandedTasks;
    @JsonProperty
    private String nameFilter;
    @JsonProperty
    private String sessionId;

    public FindTaskProgressCriteria() {
    }

    @JsonCreator
    public FindTaskProgressCriteria(@JsonProperty("pageRequest") final PageRequest pageRequest,
                                    @JsonProperty("sortList") final List<Sort> sortList,
                                    @JsonProperty("expandedTasks") final Set<TaskProgress> expandedTasks,
                                    @JsonProperty("nameFilter") final String nameFilter,
                                    @JsonProperty("sessionId") final String sessionId) {
        super(pageRequest, sortList);
        this.expandedTasks = expandedTasks;
        this.nameFilter = nameFilter;
        this.sessionId = sessionId;
    }

    public Set<TaskProgress> getExpandedTasks() {
        return expandedTasks;
    }

    public void setExpandedTasks(final Set<TaskProgress> expandedTasks) {
        this.expandedTasks = expandedTasks;
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public void setNameFilter(final String nameFilter) {
        this.nameFilter = nameFilter;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    @JsonIgnore
    public void setExpanded(final TaskProgress taskProgress, final boolean expanded) {
        if (expanded) {
            if (expandedTasks == null) {
                expandedTasks = new HashSet<>();
            }
            expandedTasks.add(taskProgress);
        } else {
            if (expandedTasks != null) {
                expandedTasks.remove(taskProgress);
                if (expandedTasks.size() == 0) {
                    expandedTasks = null;
                }
            }
        }
    }

    @JsonIgnore
    public boolean isExpanded(final TaskProgress taskProgress) {
        if (expandedTasks != null) {
            return expandedTasks.contains(taskProgress);
        }
        return false;
    }

    @JsonIgnore
    public void validateSortField() {
        if (this.getSortList().isEmpty()) {
            Sort defaultSort = new Sort(FindTaskProgressCriteria.FIELD_SUBMIT_TIME, Direction.ASCENDING, true);
            this.getSortList().add(defaultSort);
        } else {
            for (Sort sort : this.getSortList()) {
                if (!Arrays.asList(
                        FindTaskProgressCriteria.FIELD_AGE,
                        FindTaskProgressCriteria.FIELD_INFO,
                        FindTaskProgressCriteria.FIELD_NAME,
                        FindTaskProgressCriteria.FIELD_NODE,
                        FindTaskProgressCriteria.FIELD_SUBMIT_TIME,
                        FindTaskProgressCriteria.FIELD_USER).contains(sort.getField())) {
                    throw new IllegalArgumentException(
                            "A sort field of " + sort.getField() + " is not valid! It must be one of FindTaskProgressCriteria.FIELD_xxx");
                }
            }
        }
    }

    @JsonIgnore
    public boolean isMatch(final String sessionId) {
        return true;
    }
}
