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

package stroom.dashboard.expression.v1;

import java.util.Objects;

public final class ValInteger implements ValNumber {

    public static final Type TYPE = Type.INTEGER;

    private final int value;

    private ValInteger(final int value) {
        this.value = value;
    }

    public static ValInteger create(final int value) {
        final int offset = 128;
        if (value >= -128 && value <= 127) { // will cache
            return ValIntegerCache.cache[value + offset];
        }
        return new ValInteger(value);
    }

    @Override
    public Integer toInteger() {
        return value;
    }

    @Override
    public Long toLong() {
        return (long) value;
    }

    @Override
    public Double toDouble() {
        return (double) value;
    }

    @Override
    public Boolean toBoolean() {
        return value != 0;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public void appendString(final StringBuilder sb) {
        sb.append(toString());
    }

    @Override
    public Type type() {
        return TYPE;
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
        final ValInteger that = (ValInteger) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    private static class ValIntegerCache {

        static final ValInteger[] cache = new ValInteger[-(-128) + 127 + 1];

        static {
            for (int i = 0; i < cache.length; i++) {
                cache[i] = new ValInteger(i - 128);
            }
        }

        private ValIntegerCache() {
        }
    }
}
