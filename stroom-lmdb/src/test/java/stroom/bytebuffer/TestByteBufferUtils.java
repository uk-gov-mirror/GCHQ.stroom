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
 *
 */

package stroom.bytebuffer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TestByteBufferUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestByteBufferUtils.class);

    @Test
    void testIntCompare() {

        ByteBuffer buf1 = ByteBuffer.allocate(Integer.BYTES);
        ByteBuffer buf2 = ByteBuffer.allocate(Integer.BYTES);

        doIntCompareTest(0, 0, buf1, buf2);
        doIntCompareTest(0, 1, buf1, buf2);
        doIntCompareTest(-1, 0, buf1, buf2);
        doIntCompareTest(-1, 1, buf1, buf2);
        doIntCompareTest(-1000, 1000, buf1, buf2);
        doIntCompareTest(Integer.MAX_VALUE, Integer.MAX_VALUE, buf1, buf2);
        doIntCompareTest(Integer.MIN_VALUE, Integer.MIN_VALUE, buf1, buf2);
        doIntCompareTest(Integer.MIN_VALUE, Integer.MAX_VALUE, buf1, buf2);
        doIntCompareTest(Integer.MAX_VALUE, Integer.MIN_VALUE, buf1, buf2);

        // now just run the test with a load of random values
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            int val1 = random.nextInt();
            int val2 = random.nextInt();
            doIntCompareTest(val1, val2, buf1, buf2);
        }
    }

    @Test
    void testLongCompare() {

        ByteBuffer buf1 = ByteBuffer.allocate(Long.BYTES);
        ByteBuffer buf2 = ByteBuffer.allocate(Long.BYTES);
        doLongCompareTest(0L, 0L, buf1, buf2);
        doLongCompareTest(0L, 1L, buf1, buf2);
        doLongCompareTest(-1L, 0L, buf1, buf2);
        doLongCompareTest(-1L, 1L, buf1, buf2);
        doLongCompareTest(-1000L, 1000L, buf1, buf2);
        doLongCompareTest(Long.MAX_VALUE, Long.MAX_VALUE, buf1, buf2);
        doLongCompareTest(Long.MIN_VALUE, Long.MIN_VALUE, buf1, buf2);
        doLongCompareTest(Long.MIN_VALUE, Long.MAX_VALUE, buf1, buf2);
        doLongCompareTest(Long.MAX_VALUE, Long.MIN_VALUE, buf1, buf2);

        // now just run the test with a load of random values
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            long val1 = random.nextLong();
            long val2 = random.nextLong();
            doLongCompareTest(val1, val2, buf1, buf2);
        }
    }

    @Test
    void testContainsPrefix_match() {

        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5});
        ByteBuffer prefixByteBuffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4});

        boolean result = ByteBufferUtils.containsPrefix(byteBuffer, prefixByteBuffer);

        assertThat(result).isTrue();
    }

    @Test
    void testContainsPrefix_match2() {

        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5});
        ByteBuffer prefixByteBuffer = ByteBuffer.wrap(new byte[]{0});

        boolean result = ByteBufferUtils.containsPrefix(byteBuffer, prefixByteBuffer);

        assertThat(result).isTrue();
    }

    @Test
    void testContainsPrefix_bufferTooShort() {

        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{0, 1, 2});
        ByteBuffer prefixByteBuffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4});

        boolean result = ByteBufferUtils.containsPrefix(byteBuffer, prefixByteBuffer);

        assertThat(result).isFalse();
    }

    @Test
    void testContainsPrefix_noMatch() {

        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5});
        ByteBuffer prefixByteBuffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});

        boolean result = ByteBufferUtils.containsPrefix(byteBuffer, prefixByteBuffer);

        assertThat(result).isFalse();
    }

    private void doLongCompareTest(long val1, long val2, ByteBuffer buf1, ByteBuffer buf2) {
        buf1.clear();
        buf1.putLong(val1);
        buf1.flip();
        buf2.clear();
        buf2.putLong(val2);
        buf2.flip();

        int cmpLong = Long.compare(val1, val2);
        int cmpBuf = ByteBufferUtils.compareAsLong(buf1, buf2);
        LOGGER.trace("Comparing {} [{}] to {} [{}], {} {}",
                val1, ByteBufferUtils.byteBufferToHex(buf1),
                val2, ByteBufferUtils.byteBufferToHex(buf2),
                cmpLong, cmpBuf);

        // ensure comparison of the long value is the same (pos, neg or zero) as our func
        if (cmpLong == cmpBuf ||
                cmpLong < 0 && cmpBuf < 0 ||
                cmpLong > 0 && cmpBuf > 0) {
            // comparison is the same
        } else {
            fail("Mismatch on %s [%s] to %s [%s]",
                    val1, ByteBufferUtils.byteBufferToHex(buf1), val2, ByteBufferUtils.byteBufferToHex(buf2));
        }
    }

    private void doIntCompareTest(int val1, int val2, ByteBuffer buf1, ByteBuffer buf2) {
        buf1.clear();
        buf1.putInt(val1);
        buf1.flip();
        buf2.clear();
        buf2.putInt(val2);
        buf2.flip();

        int cmpLong = Integer.compare(val1, val2);
        int cmpBuf = ByteBufferUtils.compareAsInt(buf1, buf2);
        LOGGER.trace("Comparing {} [{}] to {} [{}], {} {}",
                val1, ByteBufferUtils.byteBufferToHex(buf1),
                val2, ByteBufferUtils.byteBufferToHex(buf2),
                cmpLong, cmpBuf);

        // ensure comparison of the int value is the same (pos, neg or zero) as our func
        if (cmpLong == cmpBuf ||
                cmpLong < 0 && cmpBuf < 0 ||
                cmpLong > 0 && cmpBuf > 0) {
            // comparison is the same
        } else {
            fail("Mismatch on %s [%s] to %s [%s]",
                    val1, ByteBufferUtils.byteBufferToHex(buf1), val2, ByteBufferUtils.byteBufferToHex(buf2));
        }
    }

    @Test
    void testBasicHashCode() {

        ByteBuffer byteBuffer1 = ByteBuffer.wrap(new byte[]{0, 0, 1, 2, 3, 4, 5, 0, 0});
        byteBuffer1.position(2);
        byteBuffer1.limit(7);
        LOGGER.info(ByteBufferUtils.byteBufferInfo(byteBuffer1));

        ByteBuffer byteBuffer2 = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5, 0, 0});
        byteBuffer2.position(1);
        byteBuffer2.limit(6);
        LOGGER.info(ByteBufferUtils.byteBufferInfo(byteBuffer2));

        int hash1 = ByteBufferUtils.basicHashCode(byteBuffer1);
        int hash2 = ByteBufferUtils.basicHashCode(byteBuffer2);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void testXxHash() {

        ByteBuffer byteBuffer1 = ByteBuffer.wrap(new byte[]{0, 0, 1, 2, 3, 4, 5, 0, 0});
        byteBuffer1.position(2);
        byteBuffer1.limit(7);
        LOGGER.info(ByteBufferUtils.byteBufferInfo(byteBuffer1));

        ByteBuffer byteBuffer2 = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5, 0, 0});
        byteBuffer2.position(1);
        byteBuffer2.limit(6);
        LOGGER.info(ByteBufferUtils.byteBufferInfo(byteBuffer2));

        long hash1 = ByteBufferUtils.xxHash(byteBuffer1);
        long hash2 = ByteBufferUtils.xxHash(byteBuffer2);

        assertThat(hash1).isEqualTo(hash2);
    }


    @Disabled // manual perf testing only
    @Test
    void testHashPerformance() throws IOException {

        // Warm up the jvm and get all files in the page cache
        for (int i = 0; i < 5; i++) {
            doHashTest("basic", ByteBufferUtils::basicHashCode);

            doHashTest("xxHash", ByteBufferUtils::xxHash);

            LOGGER.info("==========================================");
        }
    }

    @Disabled // manual perf testing only
    @Test
    void testCopyPerformance() {

        final int rounds = 2;
        final int iterations = 10_000;
        final int bufferSize = 1_000;

        final ByteBuffer src = ByteBuffer.allocateDirect(bufferSize);
        final ByteBuffer dest = ByteBuffer.allocateDirect(bufferSize);

        final Random random = new Random();

        final byte[] randBytes = new byte[bufferSize];
        random.nextBytes(randBytes);
        src.put(randBytes);
        src.flip();

        final Map<String, BiConsumer<ByteBuffer, ByteBuffer>> funcMap = Map.of(
                "Simple", this::doSimpleCopyTest,
                "Hadoop", this::doHadoopCopyTest);

        for (int j = 0; j < rounds; j++) {
            final int round = j;
            funcMap.forEach((name, func) -> {
                Instant startTime = Instant.now();

                for (int i = 0; i < iterations; i++) {

                    func.accept(src, dest);

                    Assertions.assertThat(dest)
                            .isEqualByComparingTo(src);

                    src.clear();
                    random.nextBytes(randBytes);
                    src.put(randBytes);
                    src.flip();

                    dest.clear();
                }
                LOGGER.info("Round {}, {} duration: {}",
                        round, name, Duration.between(startTime, Instant.now()));
            });
        }
    }

    private void doHadoopCopyTest(final ByteBuffer src, final ByteBuffer dest) {
        org.apache.hadoop.hbase.util.ByteBufferUtils.copyFromBufferToBuffer(src, dest);
    }

    private void doSimpleCopyTest(final ByteBuffer src, final ByteBuffer dest) {
        dest.put(src);
    }

    private void doHashTest(final String name,
                            final Function<ByteBuffer, Number> hashFunc) throws IOException {
        int iterations = 1_000;

        Path start = Paths.get(".")
                .resolve("src")
                .toAbsolutePath()
                .normalize();
        System.out.println(start);

        AtomicInteger fileCount = new AtomicInteger(0);
        List<Path> paths = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(start, Integer.MAX_VALUE)) {
            stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(paths::add);
        }

        Instant startTime = Instant.now();

        for (int i = 0; i < iterations; i++) {
            paths.forEach(path -> {
                try {
                    try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {

                        //Get file channel in read-only mode
                        final FileChannel fileChannel = file.getChannel();

                        //Get direct byte buffer access using channel.map() operation
                        final MappedByteBuffer buffer = fileChannel.map(
                                FileChannel.MapMode.READ_ONLY,
                                0,
                                fileChannel.size());

                        final Number hash = hashFunc.apply(buffer);
//                                LOGGER.info("  {} {}", path.toString(), hash);
                        fileCount.incrementAndGet();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        LOGGER.info("{} File contents test - count: {}, in {}",
                name, fileCount.get(), Duration.between(startTime, Instant.now()));


        ByteBuffer byteBuffer = ByteBuffer.allocate(300);

        startTime = Instant.now();

        for (int i = 0; i < iterations; i++) {
            paths.forEach(path -> {
                byteBuffer.clear();
                StandardCharsets.UTF_8.newEncoder()
                        .encode(CharBuffer.wrap(path.toString()), byteBuffer, true);

                final Number hash = hashFunc.apply(byteBuffer);
            });
        }

        LOGGER.info("{} File name test - count: {}, in {}",
                name, fileCount.get(), Duration.between(startTime, Instant.now()));
    }
}
