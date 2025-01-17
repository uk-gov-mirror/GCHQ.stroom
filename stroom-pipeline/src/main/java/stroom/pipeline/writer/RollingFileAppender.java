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

package stroom.pipeline.writer;

import stroom.pipeline.destination.RollingDestination;
import stroom.pipeline.destination.RollingDestinations;
import stroom.pipeline.destination.RollingFileDestination;
import stroom.pipeline.errorhandler.ProcessException;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.util.io.FileUtil;
import stroom.util.io.PathCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;

/**
 * Joins text instances into a single text instance.
 */
@ConfigurableElement(
        type = "RollingFileAppender",
        category = Category.DESTINATION,
        roles = {
                PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_DESTINATION,
                PipelineElementType.VISABILITY_STEPPING},
        icon = ElementIcons.FILES)
public class RollingFileAppender extends AbstractRollingAppender {

    private final PathCreator pathCreator;

    private String[] outputPaths;
    private String fileNamePattern;
    private String rolledFileNamePattern;
    private boolean useCompression;

    private String dir;
    private String fileName;
    private String rolledFileName;
    private String key;

    @Inject
    RollingFileAppender(final RollingDestinations destinations,
                        final PathCreator pathCreator) {
        super(destinations);
        this.pathCreator = pathCreator;
    }

    @Override
    public RollingDestination createDestination() throws IOException {
        String dir = this.dir;
        String fileName = this.fileName;

        dir = pathCreator.replaceTimeVars(dir);
        dir = pathCreator.replaceUUIDVars(dir);

        fileName = pathCreator.replaceTimeVars(fileName);
        fileName = pathCreator.replaceUUIDVars(fileName);

        // Create a new destination.
        final Path file = Paths.get(dir).resolve(fileName);

        // Try and create the path.
        final Path parentDir = file.getParent();
        if (!Files.isDirectory(parentDir)) {
            try {
                Files.createDirectories(parentDir);
            } catch (final IOException e) {
                throw new ProcessException("Unable to create output dirs: " + FileUtil.getCanonicalPath(parentDir));
            }
        }

        return new RollingFileDestination(pathCreator,
                key,
                getFrequency(),
                getSchedule(),
                getRollSize(),
                System.currentTimeMillis(),
                fileName,
                rolledFileName,
                parentDir,
                file,
                useCompression);
    }

    @Override
    protected Object getKey() throws IOException {
        try {
            // Create the current file name if one isn't set.
            if (key == null) {
                dir = getRandomOutputPath();
                dir = pathCreator.replaceContextVars(dir);
                dir = pathCreator.replaceSystemProperties(dir);
                dir = pathCreator.makeAbsolute(dir);

                fileName = fileNamePattern;
                fileName = pathCreator.replaceContextVars(fileName);
                fileName = pathCreator.replaceSystemProperties(fileName);

                rolledFileName = rolledFileNamePattern;
                rolledFileName = pathCreator.replaceContextVars(rolledFileName);
                rolledFileName = pathCreator.replaceSystemProperties(rolledFileName);

                key = dir + '/' + fileName;
            }

            return key;
        } catch (final RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private String getRandomOutputPath() throws IOException {
        if (outputPaths == null || outputPaths.length == 0) {
            throw new IOException("No output paths have been set");
        }

        // Get a path to use.
        String path;
        if (outputPaths.length == 1) {
            path = outputPaths[0];
        } else {
            // Choose one of the output paths at random.
            path = outputPaths[(int) Math.round(Math.random() * (outputPaths.length - 1))];
        }

        return path;
    }

    @Override
    protected void validateSpecificSettings() {
        if (outputPaths == null || outputPaths.length == 0) {
            throw new ProcessException("No output paths have been specified");
        }

        if (fileNamePattern == null || fileNamePattern.length() == 0) {
            throw new ProcessException("No file name has been specified");
        }

        if (rolledFileNamePattern == null || rolledFileNamePattern.length() == 0) {
            throw new ProcessException("No rolled file name has been specified");
        }

        if (fileNamePattern.equals(rolledFileNamePattern)) {
            throw new ProcessException("File name and rolled file name cannot be the same");
        }
    }

    @PipelineProperty(
            description = "One or more destination paths for output files separated with commas. " +
                    "Replacement variables can be used in path strings such as ${feed}.",
            displayPriority = 1)
    public void setOutputPaths(final String outputPaths) {
        this.outputPaths = outputPaths.split(",");
    }

    @PipelineProperty(description = "Choose the name of the file to write.",
            displayPriority = 2)
    public void setFileName(final String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    @PipelineProperty(description = "Choose the name that files will be renamed to when they are rolled.",
            displayPriority = 3)
    public void setRolledFileName(final String rolledFileNamePattern) {
        this.rolledFileNamePattern = rolledFileNamePattern;
    }

    @PipelineProperty(description = "Choose how frequently files are rolled.",
            defaultValue = "1h",
            displayPriority = 4)
    public void setFrequency(final String frequency) {
        super.setFrequency(frequency);
    }

    @PipelineProperty(description = "Provide a cron expression to determine when files are rolled.",
            displayPriority = 5)
    public void setSchedule(final String expression) {
        super.setSchedule(expression);
    }

    @PipelineProperty(
            description = "When the current output file exceeds this size it will be closed and a new one " +
                    "created, e.g. 10M, 1G.",
            defaultValue = "100M",
            displayPriority = 6)
    public void setRollSize(final String rollSize) {
        super.setRollSize(rollSize);
    }

    @PipelineProperty(
            description = "Apply GZIP compression to output files",
            defaultValue = "false",
            displayPriority = 7)
    public void setUseCompression(final boolean useCompression) {
        this.useCompression = useCompression;
    }
}
