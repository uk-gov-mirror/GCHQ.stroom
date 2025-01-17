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

package stroom.pipeline.refdata;

import stroom.data.shared.StreamTypeNames;
import stroom.data.store.api.InputStreamProvider;
import stroom.data.store.api.SizeAwareInputStream;
import stroom.docref.DocRef;
import stroom.meta.shared.Meta;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.cache.DocumentPermissionCache;
import stroom.pipeline.errorhandler.StoredErrorReceiver;
import stroom.pipeline.refdata.store.MapDefinition;
import stroom.pipeline.refdata.store.MultiRefDataValueProxy;
import stroom.pipeline.refdata.store.RefDataStore;
import stroom.pipeline.refdata.store.RefDataValue;
import stroom.pipeline.refdata.store.RefDataValueProxy;
import stroom.pipeline.refdata.store.RefStreamDefinition;
import stroom.pipeline.refdata.store.SingleRefDataValueProxy;
import stroom.pipeline.refdata.store.StringValue;
import stroom.pipeline.shared.data.PipelineReference;
import stroom.pipeline.state.FeedHolder;
import stroom.pipeline.state.MetaHolder;
import stroom.security.api.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.Severity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class ReferenceData {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceData.class);
    private static final LambdaLogger LAMBDA_LOGGER = LambdaLoggerFactory.getLogger(ReferenceData.class);

    // Actually 11.5 days but this is fine for the purposes of reference data.
    private static final long APPROX_TEN_DAYS = 1000000000;

    // Maps can be nested during the look up process e.g. "MAP1/MAP2"
    private static final int MINIMUM_BYTE_COUNT = 10;

    private EffectiveStreamCache effectiveStreamCache;
    private final FeedHolder feedHolder;
    private final MetaHolder metaHolder;
    private final ContextDataLoader contextDataLoader;
    private final DocumentPermissionCache documentPermissionCache;
    private final Map<PipelineReference, Boolean> localDocumentPermissionCache = new HashMap<>();
    private final ReferenceDataLoader referenceDataLoader;
    private final RefDataStoreHolder refDataStoreHolder;
    private final RefDataLoaderHolder refDataLoaderHolder;
    private final PipelineStore pipelineStore;
    private final SecurityContext securityContext;


    @Inject
    ReferenceData(final EffectiveStreamCache effectiveStreamCache,
                  final FeedHolder feedHolder,
                  final MetaHolder metaHolder,
                  final ContextDataLoader contextDataLoader,
                  final DocumentPermissionCache documentPermissionCache,
                  final ReferenceDataLoader referenceDataLoader,
                  final RefDataStoreHolder refDataStoreHolder,
                  final RefDataLoaderHolder refDataLoaderHolder,
                  final PipelineStore pipelineStore,
                  final SecurityContext securityContext) {
        this.effectiveStreamCache = effectiveStreamCache;
        this.feedHolder = feedHolder;
        this.metaHolder = metaHolder;
        this.contextDataLoader = contextDataLoader;
        this.documentPermissionCache = documentPermissionCache;
        this.referenceDataLoader = referenceDataLoader;
        this.refDataStoreHolder = refDataStoreHolder;
        this.refDataLoaderHolder = refDataLoaderHolder;
        this.pipelineStore = pipelineStore;
        this.securityContext = securityContext;
    }

    /**
     * <p>
     * Given a {@link LookupIdentifier} and a list of ref data pipelines, ensure that
     * the data required to perform the lookup is in the ref store. This method will not
     * perform the lookup, instead it will populate the {@link ReferenceDataResult} with
     * a proxy object that can later be used to do the lookup.
     * </p>
     *
     * @param pipelineReferences The references to look for reference data in.
     * @param lookupIdentifier   The identifier to lookup in the reference data
     * @param result             The reference result object containing the proxy object for performing the lookup
     */
    public void ensureReferenceDataAvailability(final List<PipelineReference> pipelineReferences,
                                                final LookupIdentifier lookupIdentifier,
                                                final ReferenceDataResult result) {

        LOGGER.trace("ensureReferenceDataAvailability({}, {}", pipelineReferences, lookupIdentifier);

        // TODO @AT It would be better if the nested map logic was pushed down into the store so that the store
        //   can accept nested map names. This would allow the store to do the lookup chain in a more efficient
        //   way with the bytebuffer of the lookup value being used as the key for the next lookup.
        //   You would still need to have code here to ensure that the data for all maps in the chain was loaded.

        // Do we have a nested map e.g a map of 'USER_ID_TO_MANAGER/USER_ID_TO_LOCATION' to get the location of a
        // user's manager using a chained lookup
        if (lookupIdentifier.isMapNested()) {
            LOGGER.trace("lookupIdentifier is nested {}", lookupIdentifier);

            // Look up the KV then use that to recurse
            doGetValue(pipelineReferences, lookupIdentifier, result);

            final Optional<RefDataValue> optValue = result.getRefDataValueProxy()
                    .flatMap(RefDataValueProxy::supplyValue);
            // This is a nested map so we are expecting the value of the first map to be a simple
            // string so we can use it as the key for the next map. The next map could also be nested.

            if (optValue.isEmpty()) {
                LOGGER.trace("sub-map not found for {}", lookupIdentifier);
                // map broken ... no link found
                result.log(Severity.WARNING, () -> "No map found for '" + lookupIdentifier + "'");
            } else {
                final RefDataValue refDataValue = optValue.get();
                try {
                    final String nextKey = ((StringValue) refDataValue).getValue();
                    LOGGER.trace("Found value to use as next key {}", nextKey);
                    // use the value from this lookup as the key for the nested map
                    LookupIdentifier nestedIdentifier = lookupIdentifier.getNestedLookupIdentifier(nextKey);

                    ensureReferenceDataAvailability(pipelineReferences, nestedIdentifier, result);
                } catch (ClassCastException e) {
                    result.log(Severity.ERROR,
                            () -> LogUtil.message("Value is the wrong type, expected: {}, found: {}",
                                    StringValue.class.getName(), refDataValue.getClass().getName()));
                }
            }
        } else {
            LOGGER.trace("lookupIdentifier is not nested {}", lookupIdentifier);
            // non-nested map so just do a lookup
            doGetValue(pipelineReferences, lookupIdentifier, result);
        }
    }

    private void doGetValue(final List<PipelineReference> pipelineReferences,
                            final LookupIdentifier lookupIdentifier,
                            final ReferenceDataResult referenceDataResult) {

        final List<SingleRefDataValueProxy> refDataValueProxies = new ArrayList<>();

        // A data feed can have multiple ref pipelines associated with it and we don't know which
        // contains the map/key we are after. None-all could. At the moment we ensure the data is loaded
        // for the effective stream of all associated ref pipelines. Given that the user probably included
        // multiple ref pipelines for a reason it is probably reasonable to load them all, then do the lookups.
        for (final PipelineReference pipelineReference : pipelineReferences) {
            LOGGER.trace("doGetValue - processing pipelineReference {} for {}",
                    pipelineReference, lookupIdentifier);

            // Handle context data differently loading it from the current stream context.
            if (pipelineReference.getStreamType() != null
                    && StreamTypeNames.CONTEXT.equals(pipelineReference.getStreamType())) {

                getNestedStreamEventList(
                        pipelineReference,
                        lookupIdentifier.getPrimaryMapName(),
                        lookupIdentifier.getKey(),
                        referenceDataResult);
            } else {
                getExternalEventList(
                        pipelineReference,
                        lookupIdentifier.getEventTime(),
                        lookupIdentifier.getPrimaryMapName(),
                        lookupIdentifier.getKey(),
                        referenceDataResult);
            }

            // We are dealing with multiple ref pipelines so collect the value proxy for
            // each one
            if (pipelineReferences.size() > 1) {
                // If a proxy has been set on the result then it means this pipeline reference
                // contains the requested map, i.e. these is no point in doing a lookup on the
                // map def if the map is know to not exist for that ref stream
                referenceDataResult.getRefDataValueProxy().ifPresent(refDataValueProxy -> {
                    if (refDataValueProxy instanceof SingleRefDataValueProxy) {
                        // Add it to a list and remove it from the result so we
                        // can add it back once we have checked all pipe refs
                        refDataValueProxies.add((SingleRefDataValueProxy) refDataValueProxy);
                        referenceDataResult.setRefDataValueProxy(null);
                    } else {
                        throw new RuntimeException("Unexpected type " + refDataValueProxy.getClass().getName());
                    }
                });
            }
        }

        // We are dealing with multiple ref pipelines so replace the current value proxy with a
        // multi one that will perform a lookup on each one in turn until it finds a hit
        if (!refDataValueProxies.isEmpty()) {
            if (refDataValueProxies.size() > 1) {
                LAMBDA_LOGGER.trace(() -> LogUtil.message(
                        "Replacing value proxy with multi proxy ({})", refDataValueProxies.size()));
                referenceDataResult.setRefDataValueProxy(new MultiRefDataValueProxy(refDataValueProxies));
            } else {
                referenceDataResult.setRefDataValueProxy(refDataValueProxies.get(0));
            }
        }
    }

    /**
     * Get an event list from a stream that is a nested child of the current
     * stream context and is therefore not effective time sensitive.
     * i.e. a context stream attached to this stream and contains data applicable
     * to this stream only.
     */
    private RefStreamDefinition getNestedStreamEventList(final PipelineReference pipelineReference,
                                                         final String mapName,
                                                         final String keyName,
                                                         final ReferenceDataResult result) {

        LAMBDA_LOGGER.trace(() -> LogUtil.message(
                "getNestedStreamEventList called, pipe: {}, map {}, key {}",
                pipelineReference.getName(),
                mapName,
                keyName));

        // Get nested stream.
        final long streamNo = metaHolder.getStreamNo();

        LAMBDA_LOGGER.trace(() -> LogUtil.message("StreamId {}, parentStreamId {}",
                metaHolder.getMeta().getId(),
                metaHolder.getMeta().getParentMetaId()));

        // the parent stream appears to be null at this point so just use the stream id
        final RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
                pipelineReference.getPipeline(),
                getPipelineVersion(pipelineReference),
                metaHolder.getMeta().getId(),
                streamNo);

        result.addEffectiveStream(refStreamDefinition);

        // Establish if we have the data for the context stream in the store
        final RefDataStore onHeapRefDataStore = refDataStoreHolder.getOnHeapRefDataStore();
        final boolean isEffectiveStreamDataLoaded = onHeapRefDataStore.isDataLoaded(refStreamDefinition);

        if (!isEffectiveStreamDataLoaded) {
            // data is not in the store so load it
            final InputStreamProvider provider = metaHolder.getInputStreamProvider();
            // There may not be a provider for this stream type if we do not
            // have any context data stream.
            if (provider != null) {
                final SizeAwareInputStream inputStream = provider.get(pipelineReference.getStreamType());
                loadContextData(
                        metaHolder.getMeta(),
                        inputStream,
                        pipelineReference.getPipeline(),
                        refStreamDefinition,
                        onHeapRefDataStore);
            }
        }

        setValueProxyOnResult(onHeapRefDataStore, mapName, keyName, result, refStreamDefinition);
        return refStreamDefinition;
    }

    private void setValueProxyOnResult(final RefDataStore refDataStore,
                                       final String mapName,
                                       final String keyName,
                                       final ReferenceDataResult result,
                                       final RefStreamDefinition refStreamDefinition) {

        final MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);

        // This stream def may/may not have this map name. To save us hitting the DB each time to
        // find out we hold the result in pipeline scope. This assumes we have already determined
        // that the refStreamDef is fully loaded
        Boolean doesMapDefExist = refDataLoaderHolder.isMapDefinitionAvailable(mapDefinition);

        if (doesMapDefExist == null) {
            // existence unknown so do a lookup in the DB
            doesMapDefExist = refDataStore.exists(mapDefinition);
            refDataLoaderHolder.markMapDefinitionAvailability(mapDefinition, doesMapDefExist);
        }
        if (doesMapDefExist) {
            // Define a proxy object to allow callers to get the required value from the store
            // now that we know that the stream that may contain it is in there.
            final RefDataValueProxy refDataValueProxy = refDataStore.getValueProxy(mapDefinition, keyName);
            result.setRefDataValueProxy(refDataValueProxy);
        } else {
            // this stream doesn't have this map so return a null proxy to save a pointless lookup
            result.setRefDataValueProxy(null);
        }
    }

    private String getPipelineVersion(final PipelineReference pipelineReference) {
        return refDataLoaderHolder.getPipelineVersion(pipelineReference, pipelineStore);
    }

    private void loadContextData(
            final Meta meta,
            final SizeAwareInputStream contextStream,
            final DocRef contextPipeline,
            final RefStreamDefinition refStreamDefinition,
            final RefDataStore refDataStore) {

        if (contextStream != null) {
            // Check the size of the input stream.
            final long byteCount = contextStream.size();
            // Only use context data if we actually have some.
            if (byteCount > MINIMUM_BYTE_COUNT) {
                // load the context data into the RefDataStore so it is available for lookups
                contextDataLoader.load(
                        contextStream,
                        meta,
                        feedHolder.getFeedName(),
                        contextPipeline,
                        refStreamDefinition,
                        refDataStore);
            }
        }
    }

    /**
     * Get an event list from a store level/non nested stream that is sensitive
     * to effective time.
     */
    private Optional<RefStreamDefinition> getExternalEventList(
            final PipelineReference pipelineReference,
            final long time,
            final String mapName,
            final String keyName,
            final ReferenceDataResult result) {

        Optional<RefStreamDefinition> optEffectiveRefStreamDef = Optional.empty();

        // Create a window of approx 10 days to cache effective streams.
        // First round down the time to the nearest 10 days approx (actually more like 11.5, one billion milliseconds).
        final long fromMs = (time / APPROX_TEN_DAYS) * APPROX_TEN_DAYS;
        final long toMs = fromMs + APPROX_TEN_DAYS;

        // Make sure the reference feed is persistent otherwise lookups will
        // fail as the equals method will only test for feeds that are the
        // same object instance rather than id.
        if (pipelineReference.getFeed() == null ||
                pipelineReference.getFeed().getUuid() == null ||
                pipelineReference.getFeed().getUuid().isEmpty() ||
                pipelineReference.getStreamType() == null ||
                pipelineReference.getStreamType().isEmpty()) {

            result.log(Severity.ERROR, () ->
                    LogUtil.message("pipelineReference is not fully formed, {}", pipelineReference));
        }

        // Check that the current user has permission to read the ref stream.
        final boolean hasPermission = localDocumentPermissionCache.computeIfAbsent(pipelineReference, k ->
                documentPermissionCache == null ||
                        documentPermissionCache.hasDocumentPermission(
                                pipelineReference.getFeed().getUuid(),
                                DocumentPermissionNames.USE));

        if (hasPermission) {
            // Create a key to find a set of effective times in the pool.
            final EffectiveStreamKey effectiveStreamKey = new EffectiveStreamKey(
                    pipelineReference.getFeed().getName(),
                    pipelineReference.getStreamType(),
                    fromMs,
                    toMs);

            // Try and fetch a tree set of effective streams for this key.
            final NavigableSet<EffectiveStream> streamSet = effectiveStreamCache.get(effectiveStreamKey);

            if (streamSet != null && streamSet.size() > 0) {
                result.log(Severity.INFO, () -> {
                    final String maxEffectiveDate = Instant.ofEpochMilli(streamSet.last().getEffectiveMs()).toString();
                    final String minEffectiveDate = Instant.ofEpochMilli(streamSet.first().getEffectiveMs()).toString();

                    return "Got " + streamSet.size() +
                            " effective streams (from " + minEffectiveDate + " to " + maxEffectiveDate +
                            ") for effective stream key: " + effectiveStreamKey;
                });

                if (LOGGER.isDebugEnabled()) {
                    String streams = streamSet.stream()
                            .map(effectiveStream -> effectiveStream.getStreamId() + " - "
                                    + Instant.ofEpochMilli(effectiveStream.getEffectiveMs()))
                            .collect(Collectors.joining("\n"));
                    LOGGER.debug("Comparing {} to Effective streams:\n{}", Instant.ofEpochMilli(time), streams);
                }

                // Try and find the stream before the requested time that is less
                // than or equal to it.
                final EffectiveStream effectiveStream = streamSet.floor(new EffectiveStream(0, time));
                // If we have an effective stream then use it.
                if (effectiveStream != null) {

                    result.log(Severity.INFO, () ->
                            "Using stream: " +
                                    effectiveStream.getStreamId() +
                                    " (effective date: " +
                                    Instant.ofEpochMilli(effectiveStream.getEffectiveMs()).toString() +
                                    ") for event time: " +
                                    Instant.ofEpochMilli(time).toString() +
                                    ", feed: " +
                                    effectiveStreamKey.getFeed());

                    final RefStreamDefinition refStreamDefinition = new RefStreamDefinition(
                            pipelineReference.getPipeline(),
                            getPipelineVersion(pipelineReference),
                            effectiveStream.getStreamId());
                    result.addEffectiveStream(refStreamDefinition);
                    final RefDataStore offHeapRefDataStore = refDataStoreHolder.getOffHeapRefDataStore();

                    // First check the pipeline scoped object to save us hitting the store for every lookup in a
                    // pipeline process run.
                    if (refDataLoaderHolder.isRefStreamAvailable(refStreamDefinition)) {
                        // we have already loaded or confirmed the load state for this refStream in this
                        // pipeline process so no need to try again
                        LOGGER.debug("refStreamDefinition {} is available for use", refStreamDefinition);
                    } else {
                        // we don't know what the load state is for this refStreamDefinition so need to find out
                        // by querying the store. This will also update the last accessed time so will prevent
                        // (unless the purge age is very small) a purge from removing the data we are about to use
                        final boolean isEffectiveStreamDataLoaded = offHeapRefDataStore.isDataLoaded(
                                refStreamDefinition);

                        if (!isEffectiveStreamDataLoaded) {
                            // we don't have the complete data so kick off a process to load it all
                            LOGGER.debug("Creating task to load reference data {}", refStreamDefinition);

                            // Initiate a load of the ref data for this stream
                            // It is possible for another thread, i.e. another pipeline process to be trying to
                            // load the same stream at the same time. There is concurrency protection further
                            // down to protect against this.
                            final StoredErrorReceiver storedErrorReceiver = securityContext.asProcessingUserResult(() ->
                                    referenceDataLoader.load(refStreamDefinition));

                            // Replay any errors/warning from the load onto our result
                            if (storedErrorReceiver != null) {
                                storedErrorReceiver.replay(result);
                            }

                            LAMBDA_LOGGER.debug(() -> LogUtil.message(
                                    "Loaded {} refStreamDefinition", refStreamDefinition));

                            // mark this ref stream defs as available for future lookups within this
                            // pipeline process
                            refDataLoaderHolder.markRefStreamAsAvailable(refStreamDefinition);
                        }
                    }

                    // now we should have the required data in the store (unless the max age is set far too small)
                    // Note however that the effective stream may not contain the map we are interested in. A data
                    // may have two ref loaders on it.  When a lookup is done it must try the lookup against the two
                    // effective streams as it cannot know which ref streams contain (if at all) the map name of
                    // interest.
                    setValueProxyOnResult(offHeapRefDataStore, mapName, keyName, result, refStreamDefinition);
                    optEffectiveRefStreamDef = Optional.of(refStreamDefinition);
                } else {

                    result.log(
                            Severity.WARNING,
                            () -> LogUtil.message(
                                    "No effective streams can be found in the returned set (" +
                                            "feed: {}, event time {})",
                                    pipelineReference.getFeed().getName(),
                                    Instant.ofEpochMilli(time).toString()));
                }
            } else {
                result.log(
                        Severity.WARNING,
                        () -> LogUtil.message(
                                "No effective stream can be found (" +
                                        "feed: {}, event time {})",
                                pipelineReference.getFeed().getName(),
                                Instant.ofEpochMilli(time).toString()));
            }
        } else {
            result.log(
                    Severity.ERROR,
                    () -> "User does not have permission to use data from feed '"
                            + pipelineReference.getFeed().getName() + "'");
        }

        return optEffectiveRefStreamDef;
    }

    void setEffectiveStreamCache(final EffectiveStreamCache effectiveStreamcache) {
        this.effectiveStreamCache = effectiveStreamcache;
    }
}
