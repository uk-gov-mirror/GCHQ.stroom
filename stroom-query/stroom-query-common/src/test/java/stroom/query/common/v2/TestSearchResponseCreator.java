package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.StaticValueFunction;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValString;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.OffsetRange;
import stroom.query.api.v2.ResultRequest;
import stroom.query.api.v2.SearchRequest;
import stroom.query.api.v2.SearchResponse;
import stroom.query.api.v2.TableResult;
import stroom.query.api.v2.TableSettings;
import stroom.query.common.v2.MapDataStore.ItemsImpl;
import stroom.query.test.util.MockitoExtension;
import stroom.query.test.util.TimingUtils;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TestSearchResponseCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSearchResponseCreator.class);
    private static final Duration TOLLERANCE = Duration.ofMillis(100);

    @Mock
    private Store mockStore;
    @Mock
    private SizesProvider sizesProvider;


    @BeforeEach
    void setup() {
        // Default mock behaviour
        Mockito.when(mockStore.getErrors()).thenReturn(Collections.emptyList());
        Mockito.when(mockStore.getHighlights()).thenReturn(Collections.emptyList());
        Mockito.when(mockStore.getData(Mockito.any())).thenReturn(createSingleItemDataObject());
        Mockito.when(sizesProvider.getDefaultMaxResultsSizes()).thenReturn(Sizes.create(Integer.MAX_VALUE));
        Mockito.when(sizesProvider.getStoreSizes()).thenReturn(Sizes.create(Integer.MAX_VALUE));
    }

    @Test
    void create_nonIncremental_timesOut() {
        final Duration serverTimeout = Duration.ofMillis(500);
        SearchResponseCreator searchResponseCreator = createSearchResponseCreator();

        //store is never complete
        Mockito.when(mockStore.isComplete()).thenReturn(false);
        makeSearchStateAfter(500, false);

        SearchRequest searchRequest = getSearchRequest(false, null);

        TimingUtils.TimedResult<SearchResponse> timedResult = TimingUtils.timeIt(() ->
                searchResponseCreator.create(searchRequest));

        SearchResponse searchResponse = timedResult.getResult();
        Duration actualDuration = timedResult.getDuration();

        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.getResults()).isNullOrEmpty();

        assertThat(TimingUtils.isWithinTollerance(
                serverTimeout,
                actualDuration,
                TOLLERANCE)).isTrue();

        assertThat(searchResponse.getErrors()).hasSize(1);
        assertThat(searchResponse.getErrors().get(0)).containsIgnoringCase("timed out");
    }

    private SearchResponseCreator createSearchResponseCreator() {
        return new SearchResponseCreator(
                sizesProvider,
                mockStore);
    }

    @Test
    void create_nonIncremental_completesImmediately() {
        SearchResponseCreator searchResponseCreator = createSearchResponseCreator();

        //store is immediately complete to replicate a synchronous store
        Mockito.when(mockStore.isComplete()).thenReturn(true);
        makeSearchStateAfter(0, true);

        SearchRequest searchRequest = getSearchRequest(false, null);

        TimingUtils.TimedResult<SearchResponse> timedResult = TimingUtils.timeIt(() ->
                searchResponseCreator.create(searchRequest));

        SearchResponse searchResponse = timedResult.getResult();
        Duration actualDuration = timedResult.getDuration();

        assertResponseWithData(searchResponse);

        //allow 100ms for java to run the code, it will never be 0
        assertThat(TimingUtils.isWithinTollerance(
                Duration.ZERO,
                actualDuration,
                TOLLERANCE)).isTrue();
    }

    @Test
    void create_nonIncremental_completesBeforeTimeout() {
        Duration clientTimeout = Duration.ofMillis(5_000);
        SearchResponseCreator searchResponseCreator = createSearchResponseCreator();

        //store initially not complete
        Mockito.when(mockStore.isComplete()).thenReturn(false);
        long sleepTime = 200L;
        makeSearchStateAfter(sleepTime, true);

        SearchRequest searchRequest = getSearchRequest(false, clientTimeout.toMillis());

        TimingUtils.TimedResult<SearchResponse> timedResult = TimingUtils.timeIt(() ->
                searchResponseCreator.create(searchRequest));

        SearchResponse searchResponse = timedResult.getResult();
        Duration actualDuration = timedResult.getDuration();

        assertResponseWithData(searchResponse);

        assertThat(TimingUtils.isWithinTollerance(
                Duration.ofMillis(sleepTime),
                actualDuration,
                TOLLERANCE)).isTrue();
    }

    @Test
    void create_incremental_noTimeout() {
        final Duration clientTimeout = Duration.ofMillis(0);
        final SearchResponseCreator searchResponseCreator = createSearchResponseCreator();

        //store is not complete during test
        Mockito.when(mockStore.isComplete()).thenReturn(false);
        makeSearchStateAfter(0, true);

        //When getData is called it should return null as it won't have had a chance to get any data yet
        Mockito.when(mockStore.getData(Mockito.any())).thenReturn(null);

        //zero timeout
        SearchRequest searchRequest = getSearchRequest(true, clientTimeout.toMillis());

        TimingUtils.TimedResult<SearchResponse> timedResult = TimingUtils.timeIt(() ->
                searchResponseCreator.create(searchRequest));

        SearchResponse searchResponse = timedResult.getResult();
        Duration actualDuration = timedResult.getDuration();

        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.getResults()).isNullOrEmpty();

        assertThat(TimingUtils.isWithinTollerance(
                clientTimeout,
                actualDuration,
                TOLLERANCE)).isTrue();

        assertThat(searchResponse.getErrors()).isNullOrEmpty();
    }

    @Test
    void create_incremental_timesOutWithDataThenCompletes() {
        Duration clientTimeout = Duration.ofMillis(500);
        SearchResponseCreator searchResponseCreator = createSearchResponseCreator();

        //store is immediately complete to replicate a synchronous store
        Mockito.when(mockStore.isComplete()).thenReturn(false);
        makeSearchStateAfter(500, false);

        SearchRequest searchRequest = getSearchRequest(true, clientTimeout.toMillis());

        TimingUtils.TimedResult<SearchResponse> timedResult = TimingUtils.timeIt(() ->
                searchResponseCreator.create(searchRequest));

        SearchResponse searchResponse = timedResult.getResult();
        Duration actualDuration = timedResult.getDuration();

        assertResponseWithData(searchResponse);

        //allow 100ms for java to run the code, it will never be 0
        assertThat(TimingUtils.isWithinTollerance(
                clientTimeout,
                actualDuration,
                TOLLERANCE)).isTrue();


        //Now the search request is sent again but this time the data will be available and the search complete
        //so should return immediately
        long sleepTime = 200L;
        makeSearchStateAfter(sleepTime, true);

        SearchRequest searchRequest2 = getSearchRequest(true, clientTimeout.toMillis());

        timedResult = TimingUtils.timeIt(() ->
                searchResponseCreator.create(searchRequest2));

        SearchResponse searchResponse2 = timedResult.getResult();
        actualDuration = timedResult.getDuration();

        assertResponseWithData(searchResponse2);

        //allow 100ms for java to run the code, it will never be 0
        assertThat(TimingUtils.isWithinTollerance(
                Duration.ofMillis(sleepTime),
                actualDuration,
                TOLLERANCE)).isTrue();
    }

    private void makeSearchStateAfter(final long sleepTime, final boolean state) {
        try {
            final Answer<Boolean> answer = invocation -> {
                TimingUtils.sleep(sleepTime);
                //change the store to be complete
                Mockito.when(mockStore.isComplete()).thenReturn(state);
                return state;
            };

            //200ms after the request is made another thread should complete the search
//            Mockito.doAnswer(answer).when(mockStore).awaitCompletion();
            Mockito.doAnswer(answer).when(mockStore).awaitCompletion(Mockito.anyLong(), Mockito.any(TimeUnit.class));
        } catch (final InterruptedException e) {
            // Ignore.
        }
    }

    private SearchRequest getSearchRequest(final boolean isIncremental, final Long timeout) {
        String key = UUID.randomUUID().toString();
        return SearchRequest.builder()
                .key(key)
                .addResultRequests(ResultRequest.builder()
                        .componentId(UUID.randomUUID().toString())
                        .resultStyle(ResultRequest.ResultStyle.TABLE)
                        .requestedRange(OffsetRange.builder()
                                .offset(0L)
                                .length(100L)
                                .build())
                        .addMappings(TableSettings.builder()
                                .queryId("someQueryId")
                                .addFields(
                                        Field.builder()
                                                .id("field1")
                                                .name("field1")
                                                .expression("expression1")
                                                .build(),
                                        Field.builder()
                                                .id("field2")
                                                .name("field2")
                                                .expression("expression1")
                                                .build(),
                                        Field.builder()
                                                .id("field3")
                                                .name("field3")
                                                .expression("expression2")
                                                .build())
                                .extractValues(false)
                                .showDetail(false)
                                .build())
                        .build())
                .incremental(isIncremental)
                .timeout(timeout)
                .dateTimeLocale("en-gb")
                .build();
    }

    private DataStore createSingleItemDataObject() {
        final ItemsImpl items = new ItemsImpl(
                100,
                null,
                null,
                null,
                remove ->
                        LOGGER.info(remove.toString()));
        final Generator[] generators = new Generator[3];
        generators[0] = new StaticValueFunction(ValString.create("A")).createGenerator();
        generators[1] = new StaticValueFunction(ValString.create("B")).createGenerator();
        generators[2] = new StaticValueFunction(ValString.create("C")).createGenerator();
        items.add(Key.root(), generators);

        final CompletionState completionState = new CompletionStateImpl();

        return new DataStore() {
            @Override
            public Items get() {
                return items;
            }

            @Override
            public Items get(final Key key) {
                return null;
            }

            @Override
            public long getSize() {
                return items.size();
            }

            @Override
            public long getTotalSize() {
                return items.size();
            }

            @Override
            public void add(final Val[] values) {
            }

            @Override
            public boolean readPayload(final Input input) {
                return false;
            }

            @Override
            public void writePayload(final Output output) {
            }

            @Override
            public void clear() {
            }

            @Override
            public CompletionState getCompletionState() {
                return completionState;
            }
        };
    }

    private void assertResponseWithData(final SearchResponse searchResponse) {
        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.getResults()).hasSize(1);
        assertThat(searchResponse.getResults().get(0)).isInstanceOf(TableResult.class);
        TableResult tableResult = (TableResult) searchResponse.getResults().get(0);
        assertThat(tableResult.getTotalResults()).isEqualTo(1);
    }
}
