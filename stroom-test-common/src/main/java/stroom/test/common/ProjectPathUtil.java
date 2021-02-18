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

package stroom.test.common;

import stroom.util.io.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ProjectPathUtil {

    public static Path resolveDir(final String projectDir) {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        Path dir = root.resolve(projectDir);
        if (!Files.isDirectory(dir)) {
            dir = root.getParent().resolve(projectDir);
            if (!Files.isDirectory(dir)) {
                throw new RuntimeException("Path not found: " + FileUtil.getCanonicalPath(dir));
            }
        }

        return dir;
    }
}
