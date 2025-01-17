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
 */

package stroom.receive.common;

import stroom.data.zip.StroomZipEntry;

import java.io.IOException;

public interface StroomStreamHandler {

    void handleEntryStart(StroomZipEntry stroomZipEntry) throws IOException;

    void handleEntryData(byte[] data, int off, int len) throws IOException;

    void handleEntryEnd() throws IOException;
}
