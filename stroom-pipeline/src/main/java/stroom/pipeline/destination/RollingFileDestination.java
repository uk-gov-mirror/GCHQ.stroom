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

package stroom.pipeline.destination;

import stroom.util.io.ByteCountOutputStream;
import stroom.util.io.FileUtil;
import stroom.util.io.GZipByteCountOutputStream;
import stroom.util.io.GZipOutputStream;
import stroom.util.io.PathCreator;
import stroom.util.scheduler.SimpleCron;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public class RollingFileDestination extends RollingDestination {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingFileDestination.class);

    private static final int MAX_FAILED_RENAME_ATTEMPTS = 100;

    private final PathCreator pathCreator;
    private final String fileName;
    private final String rolledFileName;

    private final Path dir;
    private final Path file;

    /**
     * Whether to apply GZIP compression to output files
     */
    private final boolean useCompression;

    public RollingFileDestination(final PathCreator pathCreator,
                                  final String key,
                                  final Long frequency,
                                  final SimpleCron schedule,
                                  final long rollSize,
                                  final long creationTime,
                                  final String fileName,
                                  final String rolledFileName,
                                  final Path dir,
                                  final Path file,
                                  final boolean useCompression)
            throws IOException {
        super(key, frequency, schedule, rollSize, creationTime);

        this.pathCreator = pathCreator;
        this.fileName = fileName;
        this.rolledFileName = rolledFileName;

        this.dir = dir;
        this.file = file;

        this.useCompression = useCompression;

        // Make sure we can create this path.
        try {
            if (Files.isRegularFile(file)) {
                LOGGER.debug("File exists for key={}", key);

                // I have a feeling that the OS might sometimes report that a
                // file exists that has actually just been rolled.
                LOGGER.warn("File exists for key={} so rolling immediately", key);
                setOutputStream(createOutputStream(file));

                // Roll the file.
                roll();

            } else {
                setOutputStream(createOutputStream(file));
            }
        } catch (final IOException e) {
            try {
                close();
            } catch (final IOException t) {
                LOGGER.error("Unable to close the output stream.");
            }
            throw e;
        }
    }

    private ByteCountOutputStream createOutputStream(final Path file) throws IOException {
        OutputStream fileOutputStream = Files.newOutputStream(
                file,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);

        if (useCompression) {
            return new GZipByteCountOutputStream(new GZipOutputStream(fileOutputStream));
        } else {
            return new ByteCountOutputStream(new BufferedOutputStream(fileOutputStream));
        }
    }

    @Override
    protected void afterRoll(Consumer<Throwable> exceptionConsumer) {
        boolean success = false;

        String destFileName = rolledFileName;
        destFileName = pathCreator.replaceTimeVars(destFileName);
        destFileName = pathCreator.replaceUUIDVars(destFileName);
        destFileName = pathCreator.replaceFileName(destFileName, fileName);

        // Create the destination file.
        final Path destFile = dir.resolve(destFileName);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Rolling file '{}' to '{}'", getFullPath(file), getFullPath(destFile));
        }

        // Create source path.
        Path source = file;

        // Create destination path.
        Path dest = destFile;

        // If we have got valid paths for source and dest then attempt move.
        if (source != null) {
            if (Files.isRegularFile(dest)) {
                LOGGER.error("Failed to roll file '{}' to '{}' as target exists", getFullPath(file),
                        getFullPath(destFile));
            } else {
                try {
                    Files.move(source, dest);
                    success = true;
                } catch (final IOException | RuntimeException e) {
                    exceptionConsumer.accept(wrapRollException(file, destFile, e));
                }
            }
        }

        if (source != null && !success) {
            try {
                int attempt = 1;
                while (!success && attempt <= MAX_FAILED_RENAME_ATTEMPTS) {
                    // Try to rename the file to something else.
                    final String suffix = Strings.padStart(String.valueOf(attempt), 3, '0');
                    final Path failedFile = dir.resolve(file.getFileName().toString() + "." + suffix);
                    dest = failedFile;

                    if (!Files.isRegularFile(dest)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Renaming file '{}' to '{}'", getFullPath(file), getFullPath(failedFile));
                        }
                        try {
                            Files.move(source, dest);
                            success = true;
                        } catch (final IOException | RuntimeException e) {
                            LOGGER.debug(e.getMessage(), e);
                        }
                    }
                    attempt++;
                }

                // If we didn't succeed in renaming the file to a failed file
                // then try and delete the file.
                if (!success) {
                    // Try to delete the file so we can continue to create a
                    // destination.
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Deleting file '{}'", getFullPath(file));
                    }
                    if (!Files.deleteIfExists(source)) {
                        LOGGER.error("Failed to delete file '{}'", getFullPath(file));
                    }
                }
            } catch (final IOException | RuntimeException e) {
                LOGGER.debug(e.getMessage(), e);
            }
        }
    }

    private String getFullPath(final Path file) {
        return FileUtil.getCanonicalPath(file);
    }

    private Throwable wrapRollException(final Path sourceFile,
                                        final Path destFile,
                                        final Throwable e) {
        final String msg = String.format("Failed to roll file '%s' to '%s' - %s",
                getFullPath(sourceFile),
                getFullPath(destFile),
                e.getMessage());

        return new IOException(msg, e);
    }
}
