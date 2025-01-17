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

package stroom.test;

import stroom.data.shared.StreamTypeNames;
import stroom.data.store.api.OutputStreamProvider;
import stroom.data.store.api.SegmentOutputStream;
import stroom.data.store.api.Store;
import stroom.data.store.api.Target;
import stroom.data.zip.StroomZipEntry;
import stroom.data.zip.StroomZipFile;
import stroom.data.zip.StroomZipFileType;
import stroom.feed.api.FeedProperties;
import stroom.meta.api.AttributeMap;
import stroom.meta.api.AttributeMapUtil;
import stroom.meta.api.MetaProperties;
import stroom.receive.common.StreamTargetStroomStreamHandler;
import stroom.util.io.AbstractFileVisitor;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Optional;

public class DataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

    private static final String INPUT_EXTENSION = ".in";
    private static final String ZIP_EXTENSION = ".zip";

    private final FeedProperties feedProperties;
    private final Store streamStore;

    public static final DateTimeFormatter EFFECTIVE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public DataLoader(final FeedProperties feedProperties, final Store streamStore) {
        this.feedProperties = feedProperties;
        this.streamStore = streamStore;
    }

    public void read(final Path dir, final boolean mandateEffectiveDate, final Long effectiveMs) {
        readDir(dir, mandateEffectiveDate, effectiveMs);
    }

    private void readDir(final Path dir, final boolean mandateEffectiveDate, final Long effectiveMs) {
        try {
            Files.walkFileTree(dir,
                    EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                    Integer.MAX_VALUE,
                    new AbstractFileVisitor() {
                        @Override
                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                            try {
                                final String fileName = file.getFileName().toString().toLowerCase();
                                final long applicableEffectiveMs = getEffectiveTimeMsFromFileName(file)
                                        .orElse(effectiveMs);

                                if (fileName.endsWith(INPUT_EXTENSION)) {
                                    loadInputFile(file, mandateEffectiveDate, applicableEffectiveMs);

                                } else if (fileName.endsWith(ZIP_EXTENSION)) {
                                    loadZipFile(file, mandateEffectiveDate, applicableEffectiveMs);
                                }
                            } catch (final RuntimeException e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                            return super.visitFile(file, attrs);
                        }
                    });
        } catch (final IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void loadInputFile(final Path file, final boolean mandateEffectiveDate, final Long effectiveMs) {
        final String feedName = getFeedName(file);
        try (final InputStream inputStream = Files.newInputStream(file)) {
            loadInputStream(feedName, FileUtil.getCanonicalPath(file), inputStream, mandateEffectiveDate, effectiveMs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void loadInputStream(final String feedName, final String info, final InputStream inputStream,
                                final boolean mandateEffectiveDate, final Long effectiveMs) {
        final boolean isReference = feedProperties.isReference(feedName);
        if (isReference == mandateEffectiveDate) {
            final String effDateStr = effectiveMs != null
                    ? Instant.ofEpochMilli(effectiveMs).toString()
                    : "null";
            LOGGER.info("Loading data: " + info + " with eff. date " + effDateStr);

            final String streamTypeName = feedProperties.getStreamTypeName(feedName);

            final MetaProperties metaProperties = MetaProperties.builder()
                    .feedName(feedName)
                    .typeName(streamTypeName)
                    .effectiveMs(effectiveMs)
                    .build();

            try (final Target streamTarget = streamStore.openTarget(metaProperties)) {
                try (final OutputStreamProvider outputStreamProvider = streamTarget.next()) {
                    try (final SegmentOutputStream outputStream = outputStreamProvider.get()) {
                        StreamUtil.streamToStream(inputStream, outputStream);
                    }

                    try (final SegmentOutputStream outputStream = outputStreamProvider.get(StreamTypeNames.META)) {
                        final AttributeMap map = new AttributeMap();
                        map.put("TestData", "Loaded By SetupSampleData");
                        AttributeMapUtil.write(map, outputStream);
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void loadZipFile(final Path file, final boolean mandateEffectiveDate, final Long effectiveMs) {
        final String feedName = getFeedName(file);

        if (feedProperties.isReference(feedName) == mandateEffectiveDate) {
            LOGGER.info("Loading data: " + FileUtil.getCanonicalPath(file));

            try {
                final StroomZipFile stroomZipFile = new StroomZipFile(file);
                final byte[] buffer = new byte[1024];
                final StreamTargetStroomStreamHandler streamTargetStroomStreamHandler =
                        new StreamTargetStroomStreamHandler(
                                streamStore,
                                feedProperties,
                                null,
                                feedName,
                                feedProperties.getStreamTypeName(feedName),
                                false);

                final AttributeMap map = new AttributeMap();
                map.put("TestData", "Loaded By SetupSampleData");

                streamTargetStroomStreamHandler.handleHeader(map);
                for (final String baseName : stroomZipFile.getStroomZipNameSet().getBaseNameSet()) {
                    streamTargetStroomStreamHandler
                            .handleEntryStart(new StroomZipEntry(null, baseName, StroomZipFileType.Context));
                    InputStream inputStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Context);
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        streamTargetStroomStreamHandler.handleEntryData(buffer, 0, read);
                    }
                    streamTargetStroomStreamHandler.handleEntryEnd();

                    streamTargetStroomStreamHandler.handleEntryStart(new StroomZipEntry(null,
                            baseName,
                            StroomZipFileType.Data));
                    inputStream = stroomZipFile.getInputStream(baseName, StroomZipFileType.Data);
                    while ((read = inputStream.read(buffer)) != -1) {
                        streamTargetStroomStreamHandler.handleEntryData(buffer, 0, read);
                    }
                    streamTargetStroomStreamHandler.handleEntryEnd();

                }
                streamTargetStroomStreamHandler.close();

                stroomZipFile.close();

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

//    private FeedDoc getFeed(final Path file) {
//        // Get the stem of the file name.
//        String stem = file.getFileName().toString();
//        int index = stem.indexOf('.');
//        if (index != -1) {
//            stem = stem.substring(0, index);
//        }
//        index = stem.indexOf('~');
//        if (index != -1) {
//            stem = stem.substring(0, index);
//        }
//
//        return getFeed(stem);
//    }
//
//    public FeedDoc getFeed(final String name) {
//        // Find the associated feed.
//        final Optional<FeedDoc> optional = feedDocCache.get(name);
//
//        if (!optional.isPresent()) {
//            throw new RuntimeException("Feed not found \"" + name + "\"");
//        }
//
//        return optional.get();
//    }

    private String getFeedName(final Path file) {
        // Get the stem of the file name.
        String stem = getBaseName(file);

        int index = stem.indexOf('~');
        if (index != -1) {
            stem = stem.substring(0, index);
        }

        return stem;
    }

    private String getBaseName(final Path file) {
        String baseName = file.getFileName().toString();
        int index = baseName.indexOf('.');
        if (index != -1) {
            return baseName.substring(0, index);
        } else {
            return baseName;
        }
    }

    private Optional<Long> getEffectiveTimeMsFromFileName(final Path file) {
        try {
            final String baseName = getBaseName(file);
            final String[] parts = baseName.split("~");
            if (parts.length == 3) {
                final String effectiveDateStr = parts[2];

                return Optional.of(DataLoader.EFFECTIVE_DATE_FORMATTER
                        .parse(effectiveDateStr, LocalDateTime::from)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse effective date from " + file.toString(), e);
        }
    }
}
