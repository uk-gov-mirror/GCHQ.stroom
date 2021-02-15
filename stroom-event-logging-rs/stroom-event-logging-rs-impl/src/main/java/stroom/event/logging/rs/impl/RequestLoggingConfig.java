/*
 * Copyright 2020 Crown Copyright
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

package stroom.event.logging.rs.impl;

import stroom.util.shared.AbstractConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.inject.Singleton;


@Singleton
public class RequestLoggingConfig extends AbstractConfig {

    private boolean globalLoggingEnabled = false;

    @JsonProperty("globalLoggingEnabled")
    @JsonPropertyDescription("Log additional RESTful service calls. N.B. This will result in some events being " +
            "recorded twice.")
    public boolean isGlobalLoggingEnabled() {
        return globalLoggingEnabled;
    }

    public void setGlobalLoggingEnabled(final boolean globalLoggingEnabled) {
        this.globalLoggingEnabled = globalLoggingEnabled;
    }

    @Override
    public String toString() {
        return "RequestLoggingConfig{" +
                "globalLoggingEnabled=" + globalLoggingEnabled +
                '}';
    }

}
