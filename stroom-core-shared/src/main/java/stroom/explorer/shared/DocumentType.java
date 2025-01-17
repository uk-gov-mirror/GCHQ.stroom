/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.explorer.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class DocumentType {

    public static final String DOC_IMAGE_URL = "document/";

    @JsonProperty
    private final int priority;
    @JsonProperty
    private final String type;
    @JsonProperty
    private final String displayType;
    @JsonProperty
    private final String iconUrl;

    public DocumentType(final int priority, final String type, final String displayType) {
        this.priority = priority;
        this.type = type;
        this.displayType = displayType;
        this.iconUrl = getIconUrl(type);
    }

    @JsonCreator
    public DocumentType(@JsonProperty("priority") final int priority,
                        @JsonProperty("type") final String type,
                        @JsonProperty("displayType") final String displayType,
                        @JsonProperty("iconUrl") final String iconUrl) {
        this.priority = priority;
        this.type = type;
        this.displayType = displayType;
        this.iconUrl = iconUrl;
    }

    public int getPriority() {
        return priority;
    }

    public String getDisplayType() {
        return displayType;
    }

    public String getType() {
        return type;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    private String getIconUrl(final String type) {
        return DocumentType.DOC_IMAGE_URL + type + ".svg";
    }

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DocumentType that = (DocumentType) o;

        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type;
    }
}
