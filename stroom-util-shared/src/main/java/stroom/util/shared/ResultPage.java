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

package stroom.util.shared;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * List that knows how big the whole set is.
 */
public class ResultPage<T> {
    @JsonProperty
    private List<T> values;
    @JsonProperty
    private PageResponse pageResponse;

    public ResultPage() {
        // Default constructor necessary for GWT serialisation.
    }

    public ResultPage(final List<T> values) {
        this.values = values;
        this.pageResponse = new PageResponse(0L, values.size(), (long) values.size(), true);
    }

    @JsonCreator
    public ResultPage(@JsonProperty("values") final List<T> values,
                      @JsonProperty("pageResponse") final PageResponse pageResponse) {
        this.values = values;
        this.pageResponse = pageResponse;
    }

    /**
     * @param exact is the complete size exactly correct
     */
    public ResultPage(final List<T> values, final Long offset, final Long completeSize, final boolean exact) {
        this.values = values;
        pageResponse = new PageResponse(offset, values.size(), completeSize, exact);
    }

    /**
     * Creates a list limited to a result page from a full list of results.
     *
     * @param fullList    The full list of results to create the result page from.
     * @param pageRequest The page request to limit the result list by.
     * @param <T>         The type of list item.
     * @return A list limited to a result page from a full list of results.
     */
    public static <T> ResultPage<T> createPageLimitedList(final List<T> fullList, final PageRequest pageRequest) {
        if (pageRequest != null) {
            int offset = 0;
            if (pageRequest.getOffset() != null) {
                offset = pageRequest.getOffset().intValue();
            }

            int length = fullList.size() - offset;
            if (pageRequest.getLength() != null) {
                length = Math.min(length, pageRequest.getLength());
            }

            // If the page request will lead to a limited number of results then apply that limit here.
            if (offset != 0 || length < fullList.size()) {
                // Ideally we'd use List.subList here but can't as GWT can't serialise the returned list type.
//                final List<T> limited = fullList.subList(offset, offset + length);
                final List<T> limited = new ArrayList<>(length);
                for (int i = offset; i < offset + length; i++) {
                    limited.add(fullList.get(i));
                }

                return new ResultPage<>(limited, (long) offset, (long) fullList.size(), true);
            }
        }

        return new ResultPage<>(fullList, 0L, (long) fullList.size(), true);
    }

    /**
     * Used for full queries (not bounded).
     */
    public static <T> ResultPage<T> createUnboundedList(final List<T> realList) {
        if (realList != null) {
            return new ResultPage<>(realList, 0L, (long) realList.size(), true);
        } else {
            return new ResultPage<>(new ArrayList<>(), 0L, 0L, true);
        }
    }

    /**
     * Used for filter queries (maybe bounded).
     */
    public static <T> ResultPage<T> createCriterialBasedList(final List<T> realList,
                                                             final BaseCriteria baseCriteria) {
        return createPageResultList(realList, baseCriteria.getPageRequest(), null);
    }

    /**
     * Used for filter queries (maybe bounded).
     */
    public static <T> ResultPage<T> createCriterialBasedList(final List<T> realList,
                                                             final BaseCriteria baseCriteria, final Long totalSize) {
        return createPageResultList(realList, baseCriteria.getPageRequest(), totalSize);
    }

    /**
     * Used for filter queries (maybe bounded).
     */
    public static <T> ResultPage<T> createPageResultList(final List<T> realList,
                                                         final PageRequest pageRequest,
                                                         final Long totalSize) {
        final boolean limited = pageRequest != null
                && pageRequest.getLength() != null;
        boolean moreToFollow = false;
        Long calulatedTotalSize = totalSize;
        long offset = 0;
        if (pageRequest != null && pageRequest.getOffset() != null) {
            offset = pageRequest.getOffset();
        }
        if (limited) {
            if (realList.size() > (pageRequest.getLength() + 1)) {
                // Here we check that if the query was supposed to be limited
                // make sure we have
                // get to process more that 1 + that limit. If this fails it
                // will be a coding error
                // or not applying the limit.
                throw new IllegalStateException(
                        "For some reason we returned more rows that we were limited to. Did you apply the restriction criteria?");
            }
        }

        // If we have not been given the total size see if we can work it out
        // based on hitting the end
        if (totalSize == null && limited) {
            // All our queries are + 1 on the limit so that we know there is
            // more to come
            moreToFollow = realList.size() > pageRequest.getLength();
            if (!moreToFollow) {
                calulatedTotalSize = pageRequest.getOffset() + realList.size();
            }
        }

        final ResultPage<T> results = new ResultPage<>(realList, offset, calulatedTotalSize, !moreToFollow);
        if (moreToFollow) {
            // All our queries are + 1 to we need to remove the last element
            results.values.remove(results.values.size() - 1);
            results.pageResponse = new PageResponse(results.pageResponse.getOffset(),
                    results.pageResponse.getLength() - 1, results.pageResponse.getTotal(),
                    results.pageResponse.isExact());
        }
        return results;
    }

    public PageResponse getPageResponse() {
        return pageResponse;
    }

    public void setPageResponse(final PageResponse pageResponse) {
        this.pageResponse = pageResponse;
    }

    public List<T> getValues() {
        return values;
    }

    public void setValues(final List<T> values) {
        this.values = values;
    }

    public int size() {
        return values.size();
    }

    /**
     * @return the first item or null if the list is empty
     */
    public T getFirst() {
        if (values.size() > 0) {
            return values.get(0);
        } else {
            return null;
        }
    }

    public int getPageStart() {
        if (pageResponse.getOffset() == null) {
            return 0;
        }
        return pageResponse.getOffset().intValue();
    }

    public int getPageSize() {
        if (pageResponse.getTotal() == null) {
            return getPageStart() + values.size();
        }
        return pageResponse.getTotal().intValue();
    }

    public boolean isExact() {
        return pageResponse.isExact();
    }
}
