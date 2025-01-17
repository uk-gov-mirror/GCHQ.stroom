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

package stroom.query.common.v2;

import stroom.bytebuffer.ByteBufferPool;
import stroom.dashboard.expression.v1.Any.AnySelector;
import stroom.dashboard.expression.v1.Bottom.BottomSelector;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.Last.LastSelector;
import stroom.dashboard.expression.v1.Nth.NthSelector;
import stroom.dashboard.expression.v1.Selection;
import stroom.dashboard.expression.v1.Selector;
import stroom.dashboard.expression.v1.Top.TopSelector;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValSerialiser;
import stroom.query.api.v2.TableSettings;
import stroom.util.io.ByteSizeUnit;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import org.lmdbjava.CursorIterable;
import org.lmdbjava.CursorIterable.KeyVal;
import org.lmdbjava.Dbi;
import org.lmdbjava.KeyRange;
import org.lmdbjava.PutFlags;
import org.lmdbjava.Txn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;

public class LmdbDataStore implements DataStore {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(LmdbDataStore.class);
    private static final int MIN_VALUE_SIZE = (int) ByteSizeUnit.KIBIBYTE.longBytes(1);
    private static final int MAX_VALUE_SIZE = (int) ByteSizeUnit.MEBIBYTE.longBytes(1);
    private static final int MIN_PAYLOAD_SIZE = (int) ByteSizeUnit.MEBIBYTE.longBytes(1);
    private static final int MAX_PAYLOAD_SIZE = (int) ByteSizeUnit.GIBIBYTE.longBytes(1);

    private static final long COMMIT_FREQUENCY_MS = 1000;
    private final LmdbEnvironment lmdbEnvironment;
    private final LmdbConfig lmdbConfig;
    private final Dbi<ByteBuffer> lmdbDbi;
//    private final ByteBufferPool byteBufferPool;

    private final CompiledField[] compiledFields;
    private final CompiledSorter<HasGenerators>[] compiledSorters;
    private final CompiledDepths compiledDepths;
    private final Sizes maxResults;
    private final AtomicLong totalResultCount = new AtomicLong();
    private final AtomicLong resultCount = new AtomicLong();
    private final boolean hasSort;

    private final AtomicBoolean hasEnoughData = new AtomicBoolean();
    private final AtomicBoolean dropped = new AtomicBoolean();

    private final AtomicBoolean createPayload = new AtomicBoolean();
    private final AtomicReference<byte[]> currentPayload = new AtomicReference<>();

    private final LinkedBlockingQueue<QueueItem> queue = new LinkedBlockingQueue<>(1000000);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final CountDownLatch complete = new CountDownLatch(1);
    private final CompletionState completionState = new CompletionStateImpl(queue, complete);
    private final AtomicLong uniqueKey = new AtomicLong();

    private final LmdbKey rootParentRowKey;

    LmdbDataStore(final LmdbEnvironment lmdbEnvironment,
                  final LmdbConfig lmdbConfig,
                  final ByteBufferPool byteBufferPool,
                  final String queryKey,
                  final String componentId,
                  final TableSettings tableSettings,
                  final FieldIndex fieldIndex,
                  final Map<String, String> paramMap,
                  final Sizes maxResults,
                  final Sizes storeSize) {
        this.lmdbEnvironment = lmdbEnvironment;
        this.lmdbConfig = lmdbConfig;
        this.maxResults = maxResults;

        compiledFields = CompiledFields.create(tableSettings.getFields(), fieldIndex, paramMap);
        compiledDepths = new CompiledDepths(compiledFields, tableSettings.showDetail());
        compiledSorters = CompiledSorter.create(compiledDepths.getMaxDepth(), compiledFields);

        rootParentRowKey = new LmdbKey.Builder()
                .keyBytes(Key.root().getBytes())
                .build();

        this.lmdbDbi = lmdbEnvironment.openDbi(queryKey, UUID.randomUUID().toString());
//        this.byteBufferPool = byteBufferPool;

        // Find out if we have any sorting.
        boolean hasSort = false;
        for (final CompiledSorter<HasGenerators> sorter : compiledSorters) {
            if (sorter != null) {
                hasSort = true;
                break;
            }
        }
        this.hasSort = hasSort;

        // Start transfer loop.
        // TODO : Use provided executor but don't allow it to be terminated by search termination.
        final Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(this::transfer);
    }

    private void commit(final Txn<ByteBuffer> writeTxn) {
        Metrics.measure("Commit", () -> {
            writeTxn.commit();
            writeTxn.close();
        });
    }

    @Override
    public CompletionState getCompletionState() {
        return completionState;
    }

    @Override
    public void add(final Val[] values) {
        final int[] groupSizeByDepth = compiledDepths.getGroupSizeByDepth();
        final boolean[][] groupIndicesByDepth = compiledDepths.getGroupIndicesByDepth();
        final boolean[][] valueIndicesByDepth = compiledDepths.getValueIndicesByDepth();

        Key key = Key.root();
        LmdbKey parentRowKey = rootParentRowKey;

        for (int depth = 0; depth < groupIndicesByDepth.length; depth++) {
            final LmdbKey.Builder rowKeyBuilder = new LmdbKey.Builder();
            final Generator[] generators = new Generator[compiledFields.length];

            final int groupSize = groupSizeByDepth[depth];
            final boolean[] groupIndices = groupIndicesByDepth[depth];
            final boolean[] valueIndices = valueIndicesByDepth[depth];

            Val[] groupValues = ValSerialiser.EMPTY_VALUES;
            if (groupSize > 0) {
                groupValues = new Val[groupSize];
            }

            int groupIndex = 0;
            for (int fieldIndex = 0; fieldIndex < compiledFields.length; fieldIndex++) {
                final CompiledField compiledField = compiledFields[fieldIndex];

                final Expression expression = compiledField.getExpression();
                if (expression != null) {
                    Generator generator = null;
                    Val value = null;

                    // If this is the first level then check if we should filter out this data.
                    if (depth == 0) {
                        final CompiledFilter compiledFilter = compiledField.getCompiledFilter();
                        if (compiledFilter != null) {
                            generator = expression.createGenerator();
                            generator.set(values);

                            // If we are filtering then we need to evaluate this field
                            // now so that we can filter the resultant value.
                            value = generator.eval();

                            if (!compiledFilter.match(value.toString())) {
                                // We want to exclude this item so get out of this method ASAP.
                                return;
                            }
                        }
                    }

                    // If we are grouping at this level then evaluate the expression and add to the group values.
                    if (groupIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (value == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                            value = generator.eval();
                        }
                        groupValues[groupIndex++] = value;
                    }

                    // If we need a value at this level then evaluate the expression and add the value.
                    if (valueIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (generator == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                        }
                        generators[fieldIndex] = generator;
                    }
                }
            }

            final boolean grouped = depth <= compiledDepths.getMaxGroupDepth();
            final byte[] keyBytes;
            if (grouped) {
                // This is a grouped item.
                key = key.resolve(groupValues);
                keyBytes = key.getBytes();

                final LmdbKey rowKey = rowKeyBuilder
                        .depth(depth)
                        .parentRowKey(parentRowKey)
                        .keyBytes(keyBytes)
                        .group(true)
                        .build();
                final LmdbValue rowValue = new LmdbValue(
                        keyBytes,
                        new Generators(compiledFields, generators));
                parentRowKey = rowKey;
                put(new QueueItemImpl(rowKey, rowValue));

            } else {
                // This item will not be grouped.
                final long uniqueId = getUniqueId();
                key = key.resolve(uniqueId);
                keyBytes = key.getBytes();

                final LmdbKey rowKey = rowKeyBuilder
                        .depth(depth)
                        .parentRowKey(parentRowKey)
                        .uniqueId(uniqueId)
                        .group(false)
                        .build();
                final LmdbValue rowValue = new LmdbValue(
                        keyBytes,
                        new Generators(compiledFields, generators));
                put(new QueueItemImpl(rowKey, rowValue));
            }
        }
    }

    private long getUniqueId() {
        return uniqueKey.incrementAndGet();
    }

    private void put(final QueueItem item) {
        LOGGER.trace(() -> "put");
        if (Thread.currentThread().isInterrupted() || hasEnoughData.get()) {
            return;
        }

        totalResultCount.incrementAndGet();

        // Some searches can be terminated early if the user is not sorting or grouping.
        if (!hasSort && !compiledDepths.hasGroup()) {
            // No sorting or grouping so we can stop the search as soon as we have the number of results requested by
            // the client
            if (maxResults != null && totalResultCount.get() >= maxResults.size(0)) {
                hasEnoughData.set(true);
            }
        }

        try {
            queue.put(item);
        } catch (final InterruptedException e) {
            LOGGER.debug(e.getMessage(), e);
            // Keep interrupting this thread.
            Thread.currentThread().interrupt();
        }
    }

    private void transfer() {
        Metrics.measure("Transfer", () -> {
            try {
                Txn<ByteBuffer> writeTxn = null;
                boolean needsCommit = false;
                long lastCommitMs = System.currentTimeMillis();

                while (running.get()) {
                    final QueueItem item = queue.poll(1, TimeUnit.SECONDS);
                    if (item != null) {
                        if (item.getRowKey() != null) {
                            if (writeTxn == null) {
                                writeTxn = lmdbEnvironment.txnWrite();
                            }

                            insert(writeTxn, item);

                        } else {
                            // Ensure commit.
                            lastCommitMs = 0;
                        }

                        // We have either added something or need a final commit.
                        needsCommit = true;
                    }

                    if (createPayload.get() && currentPayload.get() == null) {
                        // Commit
                        if (writeTxn != null) {
                            // Commit
                            lastCommitMs = System.currentTimeMillis();
                            needsCommit = false;
                            commit(writeTxn);
                            writeTxn = null;
                        }

                        // Create payload and clear the DB.
                        currentPayload.set(createPayload());

                    } else if (needsCommit && writeTxn != null) {
                        final long now = System.currentTimeMillis();
                        if (lastCommitMs < now - COMMIT_FREQUENCY_MS) {
                            // Commit
                            lastCommitMs = now;
                            needsCommit = false;
                            commit(writeTxn);
                            writeTxn = null;
                        }
                    }

                    // Let the item know we have finished adding it.
                    if (item != null) {
                        item.complete();
                    }
                }

            } catch (final InterruptedException e) {
                LOGGER.debug(e.getMessage(), e);
                // Continue to interrupt.
                Thread.currentThread().interrupt();
            } finally {
                drop();
            }
        });
    }

    private void insert(final Txn<ByteBuffer> txn, final QueueItem queueItem) {
        Metrics.measure("Insert", () -> {
            try {
                LOGGER.trace(() -> "insert");

                final LmdbKey rowKey = queueItem.getRowKey();
                final LmdbValue rowValue = queueItem.getRowValue();

                // Just try to put first.
                final boolean success = put(
                        txn,
                        rowKey.getByteBuffer(),
                        rowValue.getByteBuffer(),
                        PutFlags.MDB_NOOVERWRITE);
                if (success) {
                    resultCount.incrementAndGet();

                } else if (rowKey.isGroup()) {
                    // Get the existing entry for this key.
                    final ByteBuffer existingValueBuffer = lmdbDbi.get(txn, rowKey.getByteBuffer());

                    final int minValueSize = Math.max(MIN_VALUE_SIZE, existingValueBuffer.remaining());
                    try (final UnsafeByteBufferOutput output =
                            new UnsafeByteBufferOutput(minValueSize, MAX_VALUE_SIZE)) {
                        boolean merged = false;

                        try (final UnsafeByteBufferInput input = new UnsafeByteBufferInput(existingValueBuffer)) {
                            while (!input.end()) {
                                final LmdbValue existingRowValue = LmdbValue.read(compiledFields, input);

                                // If this is the same value the update it and reinsert.
                                if (existingRowValue.getKey().equals(rowValue.getKey())) {
                                    final Generator[] generators = existingRowValue.getGenerators().getGenerators();
                                    final Generator[] newValue = rowValue.getGenerators().getGenerators();
                                    final Generator[] combined = combine(generators, newValue);

                                    LOGGER.debug("Merging combined value to output");
                                    final LmdbValue combinedValue = new LmdbValue(
                                            existingRowValue.getKey().getBytes(),
                                            new Generators(compiledFields, combined));
                                    combinedValue.write(output);

                                    // Copy any remaining values.
                                    if (!input.end()) {
                                        final byte[] remainingBytes = input.readAllBytes();
                                        output.writeBytes(remainingBytes, 0, remainingBytes.length);
                                    }

                                    merged = true;

                                } else {
                                    LOGGER.debug("Copying value to output");
                                    existingRowValue.write(output);
                                }
                            }
                        }

                        // Append if we didn't merge.
                        if (!merged) {
                            LOGGER.debug("Appending value to output");
                            rowValue.write(output);
                            resultCount.incrementAndGet();
                        }

                        final ByteBuffer newValue = output.getByteBuffer().flip();
                        final boolean ok = put(txn, rowKey.getByteBuffer(), newValue);
                        if (!ok) {
                            throw new RuntimeException("Unable to update");
                        }
                    }

                } else {
                    // We do not expect a key collision here.
                    throw new RuntimeException("Unexpected collision");
                }

            } catch (final RuntimeException | IOException e) {
                LOGGER.error("Error putting " + queueItem + " (" + e.getMessage() + ")", e);
            }
        });
    }

    private boolean put(final Txn<ByteBuffer> txn,
                        final ByteBuffer key,
                        final ByteBuffer val,
                        final PutFlags... flags) {
        try {
            return lmdbDbi.put(txn, key, val, flags);
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return false;
    }

    private Generator[] combine(final Generator[] existing, final Generator[] value) {
        return Metrics.measure("Combine", () -> {
            // Combine the new item into the original item.
            for (int i = 0; i < existing.length; i++) {
                Generator existingGenerator = existing[i];
                Generator newGenerator = value[i];
                if (newGenerator != null) {
                    if (existingGenerator == null) {
                        existing[i] = newGenerator;
                    } else {
                        existingGenerator.merge(newGenerator);
                    }
                }
            }

            return existing;
        });
    }

    @Override
    public void clear() {
        // Stop the transfer loop running, this has the effect of dropping the DB when it stops.
        running.set(false);
        // Clear the queue for good measure.
        queue.clear();
        // Ensure we complete.
        complete.countDown();
        // If the transfer loop is waiting on new queue items ensure it loops once more.
        completionState.complete();
    }

    private synchronized void drop() {
        if (!dropped.get()) {
            try (final Txn<ByteBuffer> writeTxn = lmdbEnvironment.txnWrite()) {
                LOGGER.info("Dropping: " + new String(lmdbDbi.getName(), StandardCharsets.UTF_8));
                lmdbDbi.drop(writeTxn, true);
                writeTxn.commit();
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                lmdbEnvironment.list();
            } finally {
                resultCount.set(0);
                totalResultCount.set(0);
                dropped.set(true);
                lmdbEnvironment.list();
            }
        }
    }

    private byte[] createPayload() {
        final PayloadOutput payloadOutput = new PayloadOutput(MIN_PAYLOAD_SIZE, MAX_PAYLOAD_SIZE);

        Metrics.measure("createPayload", () -> {
            try (Txn<ByteBuffer> writeTxn = lmdbEnvironment.txnWrite()) {
                final long limit = lmdbConfig.getPayloadLimit().getBytes();
                if (limit > 0) {
                    final AtomicLong count = new AtomicLong();

                    try (final CursorIterable<ByteBuffer> cursorIterable = lmdbDbi.iterate(writeTxn)) {
                        final Iterator<KeyVal<ByteBuffer>> iterator = cursorIterable.iterator();
                        while (count.get() < limit && iterator.hasNext()) {
                            final KeyVal<ByteBuffer> kv = iterator.next();
                            final ByteBuffer keyBuffer = kv.key();
                            final ByteBuffer valBuffer = kv.val();

                            // Add to the size of the current payload.
                            count.addAndGet(keyBuffer.remaining());
                            count.addAndGet(valBuffer.remaining());

                            payloadOutput.writeInt(keyBuffer.remaining());
                            payloadOutput.writeByteBuffer(keyBuffer);
                            payloadOutput.writeInt(valBuffer.remaining());
                            payloadOutput.writeByteBuffer(valBuffer);

                            lmdbDbi.delete(writeTxn, keyBuffer.flip());
                        }
                    }

                    writeTxn.commit();

                } else {
                    lmdbDbi.iterate(writeTxn).forEach(kv -> {
                        final ByteBuffer keyBuffer = kv.key();
                        final ByteBuffer valBuffer = kv.val();

                        payloadOutput.writeInt(keyBuffer.remaining());
                        payloadOutput.writeByteBuffer(keyBuffer);
                        payloadOutput.writeInt(valBuffer.remaining());
                        payloadOutput.writeByteBuffer(valBuffer);
                    });

                    lmdbDbi.drop(writeTxn);
                    writeTxn.commit();
                }

            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException("Error clearing DB", e);
            }
        });

        return payloadOutput.toBytes();
    }

    @Override
    public void writePayload(final Output output) {
        Metrics.measure("writePayload", () -> {
            final boolean complete = getCompletionState().isComplete();
            createPayload.set(true);

            final List<byte[]> payloads = new ArrayList<>(2);

            final byte[] payload = currentPayload.getAndSet(null);
            if (payload != null) {
                payloads.add(payload);
            }

            if (complete && running.get()) {
                // If we are complete and running then we ought to be able to get the run loop to create a final
                // payload.
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                try {
                    queue.put(new QueueItem() {
                        @Override
                        public void complete() {
                            countDownLatch.countDown();
                        }
                    });
                    if (!countDownLatch.await(1, TimeUnit.MINUTES)) {
                        LOGGER.error("Timeout waiting for final payload creation");
                    }
                } catch (final InterruptedException e) {
                    LOGGER.debug(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }

                final byte[] finalPayload = currentPayload.getAndSet(null);
                if (finalPayload != null) {
                    payloads.add(finalPayload);
                }
            }

            output.writeInt(payloads.size());
            payloads.forEach(bytes -> {
                output.writeInt(bytes.length);
                output.writeBytes(bytes);
            });
        });
    }

    @Override
    public boolean readPayload(final Input input) {
        // Return false if we have been asked to terminate or are no longer running.
        if (!running.get() || Thread.currentThread().isInterrupted()) {
            return false;
        }

        return Metrics.measure("readPayload", () -> {
            final int count = input.readInt(); // There may be more than one payload if it was the final transfer.
            for (int i = 0; i < count; i++) {
                final int length = input.readInt();
                if (length > 0) {
                    final byte[] bytes = input.readBytes(length);
                    try (final Input in = new Input(new ByteArrayInputStream(bytes))) {
                        while (!in.end()) {
                            final int rowKeyLength = in.readInt();
                            final byte[] key = in.readBytes(rowKeyLength);
                            final ByteBuffer keyBuffer = ByteBuffer.allocateDirect(key.length);
                            keyBuffer.put(key, 0, key.length);
                            keyBuffer.flip();

                            final int valueLength = in.readInt();
                            final byte[] value = in.readBytes(valueLength);
                            final ByteBuffer valueBuffer = ByteBuffer.allocateDirect(value.length);
                            valueBuffer.put(value, 0, value.length);
                            valueBuffer.flip();

                            LmdbKey rowKey = new LmdbKey(keyBuffer);
                            if (!rowKey.isGroup()) {
                                // Create a new unique key if this isn't a group key.
                                rowKey.makeUnique(this::getUniqueId);
                            }

                            final QueueItem queueItem =
                                    new QueueItemImpl(rowKey, new LmdbValue(compiledFields, valueBuffer));
                            put(queueItem);
                        }
                    }
                }
            }

            // Return false if we have been asked to terminate or are no longer running.
            if (!running.get() || Thread.currentThread().isInterrupted()) {
                return false;
            }

            // Wait for the payload to be added before we go and ask for another.
            // If we don't wait here then remote search results may not be added to the data store before the associated
            // remote search is assumed to be complete.
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            put(new QueueItem() {
                @Override
                public void complete() {
                    countDownLatch.countDown();
                }
            });
            try {
                countDownLatch.await();
            } catch (final InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
            return !Thread.currentThread().isInterrupted() && !hasEnoughData.get();
        });
    }

    @Override
    public Items get() {
        return get(Key.root());
    }

    @Override
    public Items get(final Key parentKey) {
        return Metrics.measure("get", () -> {
            final int depth = parentKey.size();
            final int trimmedSize = maxResults.size(depth);

            final ItemArrayList list = getChildren(parentKey, depth, trimmedSize, true, false);

            return new Items() {
                @Override
                @Nonnull
                public Iterator<Item> iterator() {
                    return new Iterator<>() {
                        private int pos = 0;

                        @Override
                        public boolean hasNext() {
                            return list.size > pos;
                        }

                        @Override
                        public Item next() {
                            return list.array[pos++];
                        }
                    };
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        });
    }

    private ItemArrayList getChildren(final Key parentKey,
                                      final int depth,
                                      final int trimmedSize,
                                      final boolean allowSort,
                                      final boolean trimTop) {
        final ItemArrayList list = new ItemArrayList(10);

        final ByteBuffer start = LmdbKey.createKeyStem(depth, parentKey);
        final KeyRange<ByteBuffer> keyRange = KeyRange.atLeast(start);

        final int maxSize;
        if (trimmedSize < Integer.MAX_VALUE / 2) {
            maxSize = Math.max(1000, trimmedSize * 2);
        } else {
            maxSize = Integer.MAX_VALUE;
        }
        final CompiledSorter<HasGenerators> sorter = compiledSorters[depth];
        boolean trimmed = true;

        boolean inRange = true;
        try (final Txn<ByteBuffer> readTxn = lmdbEnvironment.txnRead()) {
            try (final CursorIterable<ByteBuffer> cursorIterable = lmdbDbi.iterate(readTxn, keyRange)) {
                final Iterator<KeyVal<ByteBuffer>> iterator = cursorIterable.iterator();

                while (iterator.hasNext() && inRange && !Thread.currentThread().isInterrupted()) {
                    final KeyVal<ByteBuffer> keyVal = iterator.next();

                    // Make sure the first part of the row key matches the start key we are looking for.
                    boolean match = true;
                    for (int i = 0; i < start.remaining() && match; i++) {
                        if (start.get(i) != keyVal.key().get(i)) {
                            match = false;
                        }
                    }

                    if (match) {
                        final ByteBuffer valueBuffer = keyVal.val();
                        try (final UnsafeByteBufferInput input = new UnsafeByteBufferInput(valueBuffer)) {
                            while (!input.end() && inRange) {
                                final LmdbValue rowValue = LmdbValue.read(compiledFields, input);
                                final Key key = rowValue.getKey();
                                if (key.getParent().equals(parentKey)) {
                                    final Generator[] generators = rowValue.getGenerators().getGenerators();
                                    list.add(new ItemImpl(this, key, generators));
                                    if (!allowSort && list.size >= trimmedSize) {
                                        // Stop without sorting etc.
                                        inRange = false;

                                    } else {
                                        trimmed = false;
                                        if (list.size() > maxSize) {
                                            list.sortAndTrim(sorter, trimmedSize, trimTop);
                                            trimmed = true;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        inRange = false;
                    }
                }
            }
        }

        if (!trimmed) {
            list.sortAndTrim(sorter, trimmedSize, trimTop);
        }

        return list;
    }


    @Override
    public long getSize() {
        return resultCount.get();
    }

    @Override
    public long getTotalSize() {
        return totalResultCount.get();
    }

    private static class ItemArrayList {

        private final int minArraySize;
        private ItemImpl[] array;
        private int size;

        public ItemArrayList(final int minArraySize) {
            this.minArraySize = minArraySize;
            array = new ItemImpl[minArraySize];
        }

        void sortAndTrim(final CompiledSorter<HasGenerators> sorter,
                         final int trimmedSize,
                         final boolean trimTop) {
            if (sorter != null && size > 0) {
                Arrays.sort(array, 0, size, sorter);
            }
            if (size > trimmedSize) {
                final int len = Math.max(minArraySize, trimmedSize);
                final ItemImpl[] newArray = new ItemImpl[len];
                if (trimTop) {
                    System.arraycopy(array, array.length - trimmedSize, newArray, 0, trimmedSize);
                } else {
                    System.arraycopy(array, 0, newArray, 0, trimmedSize);
                }
                array = newArray;
                size = trimmedSize;
            }
        }

        void add(final ItemImpl item) {
            if (array.length <= size) {
                final ItemImpl[] newArray = new ItemImpl[size * 2];
                System.arraycopy(array, 0, newArray, 0, array.length);
                array = newArray;
            }
            array[size++] = item;
        }

        ItemImpl get(final int index) {
            return array[index];
        }

        int size() {
            return size;
        }
    }

    public static class ItemImpl implements Item, HasGenerators {

        private final LmdbDataStore lmdbDataStore;
        private final Key key;
        private final Generator[] generators;

        public ItemImpl(final LmdbDataStore lmdbDataStore,
                        final Key key,
                        final Generator[] generators) {
            this.lmdbDataStore = lmdbDataStore;
            this.key = key;
            this.generators = generators;
        }

        @Override
        public Key getKey() {
            return key;
        }

        @Override
        public Val getValue(final int index) {
            Val val = null;

            final Generator generator = generators[index];
            if (generator instanceof Selector) {
                if (key.isGrouped()) {
                    int maxRows = 1;
                    boolean sort = true;
                    boolean trimTop = false;

                    if (generator instanceof AnySelector) {
                        sort = false;
//                    } else if (generator instanceof FirstSelector) {
                    } else if (generator instanceof LastSelector) {
                        trimTop = true;
                    } else if (generator instanceof TopSelector) {
                        maxRows = ((TopSelector) generator).getLimit();
                    } else if (generator instanceof BottomSelector) {
                        maxRows = ((BottomSelector) generator).getLimit();
                        trimTop = true;
                    } else if (generator instanceof NthSelector) {
                        maxRows = ((NthSelector) generator).getPos();
                    }

                    final ItemArrayList items = lmdbDataStore.getChildren(
                            key,
                            key.size(),
                            maxRows,
                            sort,
                            trimTop);

                    final Selector selector = (Selector) generator;
                    val = selector.select(new Selection<>() {
                        @Override
                        public int size() {
                            return items.size;
                        }

                        @Override
                        public Val get(final int pos) {
                            if (pos < items.size) {
                                items.get(pos).generators[index].eval();
                            }
                            return ValNull.INSTANCE;
                        }
                    });

                } else {
                    val = generator.eval();
                }
            } else if (generator != null) {
                val = generator.eval();
            }

            return val;
        }

        @Override
        public Generator[] getGenerators() {
            return generators;
        }
    }

    private abstract static class QueueItem {

        public LmdbKey getRowKey() {
            return null;
        }

        public LmdbValue getRowValue() {
            return null;
        }

        public void complete() {
        }
    }

    private static class QueueItemImpl extends QueueItem {

        private final LmdbKey rowKey;
        private final LmdbValue rowValue;

        public QueueItemImpl(final LmdbKey rowKey,
                             final LmdbValue rowValue) {
            this.rowKey = rowKey;
            this.rowValue = rowValue;
        }

        @Override
        public LmdbKey getRowKey() {
            return rowKey;
        }

        @Override
        public LmdbValue getRowValue() {
            return rowValue;
        }

        @Override
        public void complete() {
        }
    }

    private static class CompletionStateImpl implements CompletionState {

        private final LinkedBlockingQueue<QueueItem> queue;
        private final CountDownLatch complete;

        public CompletionStateImpl(final LinkedBlockingQueue<QueueItem> queue,
                                   final CountDownLatch complete) {
            this.queue = queue;
            this.complete = complete;
        }

        @Override
        public void complete() {
            try {
                queue.put(new QueueItem() {
                    @Override
                    public void complete() {
                        complete.countDown();
                    }
                });
            } catch (final InterruptedException e) {
                LOGGER.debug(e.getMessage(), e);
                Thread.currentThread().interrupt();
                complete.countDown();
            }
        }

        @Override
        public boolean isComplete() {
            boolean complete = true;

            try {
                complete = this.complete.await(0, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException e) {
                LOGGER.debug(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
            return complete;
        }

        @Override
        public void awaitCompletion() throws InterruptedException {
            complete.await();
        }

        @Override
        public boolean awaitCompletion(final long timeout, final TimeUnit unit) throws InterruptedException {
            return complete.await(timeout, unit);
        }

        @Override
        public void accept(final Long value) {
            complete();
        }
    }
}
