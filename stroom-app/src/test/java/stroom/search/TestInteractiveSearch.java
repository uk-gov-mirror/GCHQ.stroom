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

package stroom.search;

import stroom.annotation.api.AnnotationFields;
import stroom.dictionary.impl.DictionaryStore;
import stroom.dictionary.shared.DictionaryDoc;
import stroom.docref.DocRef;
import stroom.index.impl.IndexStore;
import stroom.index.shared.IndexConstants;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.query.api.v2.Field;
import stroom.query.api.v2.Format;
import stroom.query.api.v2.ParamUtil;
import stroom.query.api.v2.Query;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.Row;
import stroom.query.api.v2.TableSettings;
import stroom.query.common.v2.EventRef;
import stroom.query.common.v2.EventRefs;
import stroom.search.impl.EventSearchTask;
import stroom.search.impl.EventSearchTaskHandler;
import stroom.task.api.TaskContextFactory;
import stroom.task.impl.ExecutorProviderImpl;

import org.apache.hadoop.util.ThreadUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Provider;

import static org.assertj.core.api.Assertions.assertThat;

class TestInteractiveSearch extends AbstractSearchTest {

    private static boolean doneSetup;
    @Inject
    private CommonIndexingTestHelper commonIndexingTestHelper;
    @Inject
    private IndexStore indexStore;
    @Inject
    private DictionaryStore dictionaryStore;
    @Inject
    private Executor executor;
    @Inject
    private TaskContextFactory taskContextFactory;
    @Inject
    private Provider<EventSearchTaskHandler> eventSearchTaskHandlerProvider;
    @Inject
    private ExecutorProviderImpl executorProvider;

    @BeforeEach
    void setup() {
        if (!doneSetup) {
            commonIndexingTestHelper.setup();
            doneSetup = true;
        }
    }

    @Override
    protected boolean setupBetweenTests() {
        return false;
    }

    /**
     * Positive case insensitive test.
     */
    @Test
    void positiveCaseInsensitiveTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description", "e0567");
        test(expression, 5);
    }

    @Test
    void positiveCaseInsensitiveTestMultiComponent() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description", "e0567");
        final List<String> componentIds = Arrays.asList("table-1", "table-2");
        test(expression, 5, componentIds, true);
    }

    /**
     * Positive case insensitive test.
     */
    @Test
    void positiveCaseInsensitiveTestWithoutExtraction() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description", "e0567");
        final List<String> componentIds = Collections.singletonList("table-1");
        test(expression, 5, componentIds, false);
    }

    /**
     * Positive case insensitive test with wildcard.
     */
    @Test
    void positiveCaseInsensitiveTest2() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user*", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description", "e0567");
        test(expression, 25);
    }

    /**
     * Negative test for case sensitive field.
     */
    @Test
    void negativeCaseSensitiveTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user*", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "e0567");
        test(expression, 0);
    }

    /**
     * Negative test for case sensitive field.
     */
    @Test
    void negativeCaseSensitiveTest2() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "e0567");
        test(expression, 0);
    }

    /**
     * Positive test case sensitive field.
     */
    @Test
    void positiveCaseSensitiveTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user*", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "E0567");
        test(expression, 25);
    }

    /**
     * Test case sensitive field plus other field.
     */
    @Test
    void positiveCaseSensitiveTest2() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "E0567");
        test(expression, 5);
    }

    /**
     * Test case sensitive field plus other field.
     */
    @Test
    void positiveCaseSensitiveTest3() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "E0567");
        test(expression, 5);
    }

    /**
     * Test analysed field search.
     */
    @Test
    void positiveAnalysedFieldTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command", "msg");
        test(expression, 4);
    }

    /**
     * Test analysed field search.
     */
    @Test
    void positiveAnalysedFieldTestWithLeadingWildcard() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command", "*msg");
        test(expression, 4);
    }

    /**
     * Test analysed field search.
     */
    @Test
    void negativeAnalysedFieldTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command", "msg foobar");
        test(expression, 0);
    }

    /**
     * Test analysed field search.
     */
    @Test
    void positiveAnalysedFieldTestWithIn() {
        final ExpressionOperator.Builder expression = buildInExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command", "msg foo bar");
        test(expression, 4);
    }

    /**
     * Negative test on keyword field.
     */
    @Test
    void negativeKeywordFieldTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command (Keyword)", "foo");
        test(expression, 0);
    }

    /**
     * Positive test on keyword field.
     */
    @Test
    void positiveKeywordFieldTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command (Keyword)", "msg=foo bar");
        test(expression, 4);
    }

    /**
     * Positive test on keyword field.
     */
    @Test
    void positiveKeywordFieldTestWithLeadingWildcard() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command (Keyword)", "*foo bar");
        test(expression, 4);
    }

    /**
     * Test not equals.
     */
    @Test
    void notEqualsTest() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user*", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "E0567");
        expression.addOperator(ExpressionOperator.builder().op(Op.NOT)
                .addTerm("EventTime", Condition.EQUALS, "2007-08-18T13:50:56.000Z")
                .build());
        test(expression, 24);
    }

    /**
     * Test exclusion of multiple items.
     */
    @Test
    void notEqualsTest2() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user*", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "E0567")
                .addOperator(ExpressionOperator.builder().op(Op.NOT)
                        .addOperator(ExpressionOperator.builder().op(Op.OR)
                                .addTerm("EventTime", Condition.EQUALS, "2007-08-18T13:50:56.000Z")
                                .addTerm("EventTime", Condition.EQUALS, "2007-01-18T13:56:42.000Z")
                                .build())
                        .build());
        test(expression, 23);
    }

    /**
     * Test exclusion of multiple items.
     */
    @Test
    void notEqualsTest3() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user*", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "E0567")
                .addOperator(ExpressionOperator.builder().op(Op.NOT)
                        .addOperator(ExpressionOperator.builder()
                                .addTerm("EventTime", Condition.EQUALS, "2007-08-18T13:50:56.000Z")
                                .addTerm("UserId", Condition.EQUALS, "user4")
                                .build())
                        .build());
        test(expression, 24);
    }

    /**
     * Test more complex exclusion of multiple items.
     */
    @Test
    void notEqualsTest4() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "user*", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Description (Case Sensitive)", "E0567")
                .addOperator(ExpressionOperator.builder().op(Op.NOT)
                        .addOperator(ExpressionOperator.builder().op(Op.OR)
                                .addOperator(ExpressionOperator.builder()
                                        .addTerm("EventTime", Condition.EQUALS, "2007-08-18T13:50:56.000Z")
                                        .addTerm("UserId", Condition.EQUALS, "user4")
                                        .build())
                                .addTerm("EventTime", Condition.EQUALS, "2007-01-18T13:56:42.000Z")
                                .build())
                        .build());
        test(expression, 23);
    }

    /**
     * Test the use of a dictionary.
     */
    @Test
    void dictionaryTest1() {
        final DocRef docRef = dictionaryStore.createDocument("users");
        final DictionaryDoc dic = dictionaryStore.readDocument(docRef);
        dic.setData("user1\nuser2\nuser5");
        dictionaryStore.writeDocument(dic);

        final ExpressionOperator.Builder and = ExpressionOperator.builder();
        and.addTerm("UserId", Condition.IN_DICTIONARY, stroom.docstore.shared.DocRefUtil.create(dic));

        test(and, 15);

        dictionaryStore.deleteDocument(dic.getUuid());
    }

    /**
     * Test the use of a dictionary.
     */
    @Test
    void dictionaryTest2() {
        final DocRef docRef1 = dictionaryStore.createDocument("users");
        DictionaryDoc dic1 = dictionaryStore.readDocument(docRef1);
        dic1.setData("user1\nuser2\nuser5");
        dictionaryStore.writeDocument(dic1);

        final DocRef docRef2 = dictionaryStore.createDocument("command");
        DictionaryDoc dic2 = dictionaryStore.readDocument(docRef2);
        dic2.setData("msg");
        dictionaryStore.writeDocument(dic2);

        final ExpressionOperator.Builder and = ExpressionOperator.builder();
        and.addTerm("UserId", Condition.IN_DICTIONARY, stroom.docstore.shared.DocRefUtil.create(dic1));
        and.addTerm("Command", Condition.IN_DICTIONARY, stroom.docstore.shared.DocRefUtil.create(dic2));

        test(and, 10);

        dictionaryStore.deleteDocument(dic1.getUuid());
        dictionaryStore.deleteDocument(dic2.getUuid());
    }

    /**
     * Test the use of a dictionary.
     */
    @Test
    void dictionaryTest3() {
        final DocRef docRef1 = dictionaryStore.createDocument("users");
        DictionaryDoc dic1 = dictionaryStore.readDocument(docRef1);
        dic1.setData("user1\nuser2\nuser5");
        dictionaryStore.writeDocument(dic1);

        final DocRef docRef2 = dictionaryStore.createDocument("command");
        DictionaryDoc dic2 = dictionaryStore.readDocument(docRef2);
        dic2.setData("msg foo bar");
        dictionaryStore.writeDocument(dic2);

        final ExpressionOperator.Builder and = ExpressionOperator.builder();
        and.addTerm("UserId", Condition.IN_DICTIONARY, stroom.docstore.shared.DocRefUtil.create(dic1));
        and.addTerm("Command", Condition.IN_DICTIONARY, stroom.docstore.shared.DocRefUtil.create(dic2));

        test(and, 10);

        dictionaryStore.deleteDocument(dic1.getUuid());
        dictionaryStore.deleteDocument(dic2.getUuid());
    }

    /**
     * Test analysed field search.
     */
    @Test
    void testBug173() {
        final ExpressionOperator.Builder expression = buildExpression("UserId", "use*5", "2000-01-01T00:00:00.000Z",
                "2016-01-02T00:00:00.000Z", "Command", "!");
        test(expression, 5);
    }

    private void test(final ExpressionOperator.Builder expressionIn, final int expectResultCount) {
        final List<String> componentIds = Collections.singletonList("table-1");
        test(expressionIn, expectResultCount, componentIds, true);
    }

    private void test(final ExpressionOperator.Builder expressionIn,
                      final int expectResultCount,
                      final List<String> componentIds,
                      final boolean extractValues) {
        testInteractive(expressionIn, expectResultCount, componentIds, extractValues);
        testEvents(expressionIn, expectResultCount);
    }

    private void testInteractive(final ExpressionOperator.Builder expressionIn,
                                 final int expectResultCount,
                                 final List<String> componentIds,
                                 final boolean extractValues) {

        // code to test the results when they come back
        Consumer<Map<String, List<Row>>> resultMapConsumer = resultMap -> {
            for (final List<Row> values : resultMap.values()) {
                if (expectResultCount == 0) {
                    assertThat(values.size()).isEqualTo(0);

                } else {
                    // Make sure we got what we expected.
                    Row firstResult = null;
                    if (values != null && values.size() > 0) {
                        firstResult = values.get(0);
                    }
                    assertThat(firstResult).as("No results found").isNotNull();

                    if (extractValues) {
                        final String time = firstResult.getValues().get(1);
                        assertThat(time).as("Incorrect heading").isNotNull();
                        assertThat(values.size()).as("Incorrect number of hits found").isEqualTo(expectResultCount);
                        boolean found = false;
                        for (final Row hit : values) {
                            final String str = hit.getValues().get(2);
                            if ("2007-03-18T14:34:41.000Z".equals(str)) {
                                found = true;
                            }
                        }
                        assertThat(found).as("Unable to find expected hit").isTrue();
                    }
                }
            }
        };

        testInteractive(expressionIn,
                expectResultCount,
                componentIds,
                this::createTableSettings,
                extractValues,
                resultMapConsumer,
                indexStore);

        while (executorProvider.getCurrentTaskCount() > 0) {
            ThreadUtil.sleepAtLeastIgnoreInterrupts(1000);
        }
        assertThat(executorProvider.getCurrentTaskCount()).isEqualTo(0);
    }

    private void testEvents(final ExpressionOperator.Builder expressionIn, final int expectResultCount) {
        // ADDED THIS SECTION TO TEST GUICE VALUE INJECTION.
//        StroomProperties.setOverrideProperty(
//        "stroom.search.impl.shard.concurrentTasks", "1", StroomProperties.Source.TEST);
//        StroomProperties.setOverrideProperty(
//        "stroom.search.impl.extraction.concurrentTasks", "1", StroomProperties.Source.TEST);

        final DocRef indexRef = indexStore.list().get(0);
        assertThat(indexRef).as("Index is null").isNotNull();

        final QueryKey key = new QueryKey(UUID.randomUUID().toString());
        final Query query = Query.builder().dataSource(indexRef).expression(expressionIn.build()).build();

        final CountDownLatch complete = new CountDownLatch(1);

        final EventSearchTask eventSearchTask = new EventSearchTask(
                key,
                query,
                new EventRef(1, 1),
                new EventRef(Long.MAX_VALUE, Long.MAX_VALUE),
                1000,
                1000,
                100);
        final AtomicReference<EventRefs> results = new AtomicReference<>();

        final Supplier<EventRefs> supplier = taskContextFactory.contextResult("Translate", taskContext -> {
            final EventSearchTaskHandler eventSearchTaskHandler = eventSearchTaskHandlerProvider.get();
            return eventSearchTaskHandler.exec(eventSearchTask);
        });
        CompletableFuture
                .supplyAsync(supplier, executor)
                .whenComplete((r, t) -> {
                    if (r != null) {
                        results.set(r);
                    }
                    complete.countDown();
                });

        try {
            complete.await();
        } catch (final InterruptedException e) {
            // Continue to interrupt this thread.
            Thread.currentThread().interrupt();
            throw new RuntimeException(e.getMessage(), e);
        }

        final EventRefs result = results.get();

        int count = 0;
        if (result != null) {
            count += result.size();
        }

        assertThat(count).isEqualTo(expectResultCount);

        while (executorProvider.getCurrentTaskCount() > 0) {
            ThreadUtil.sleepAtLeastIgnoreInterrupts(1000);
        }
        assertThat(executorProvider.getCurrentTaskCount()).isEqualTo(0);
    }

    private TableSettings createTableSettings(final boolean extractValues) {
        final Field streamIdField = Field.builder()
                .name("Stream Id")
                .expression(ParamUtil.makeParam(IndexConstants.STREAM_ID))
                .build();

        final Field eventIdField = Field.builder()
                .name("Event Id")
                .expression(ParamUtil.makeParam(IndexConstants.EVENT_ID))
                .build();

        final Field timeField = Field.builder()
                .name("Event Time")
                .expression(ParamUtil.makeParam("EventTime"))
                .format(Format.DATE_TIME)
                .build();

        final Field statusField = Field.builder()
                .name("Status")
                .expression(ParamUtil.makeParam(AnnotationFields.STATUS))
                .build();

        final DocRef resultPipeline = commonIndexingTestHelper.getSearchResultPipeline();
        return TableSettings.builder()
                .addFields(streamIdField)
                .addFields(eventIdField)
                .addFields(timeField)
                .addFields(statusField)
                .extractValues(extractValues)
                .extractionPipeline(resultPipeline)
                .build();
    }

    private ExpressionOperator.Builder buildExpression(final String userField,
                                                       final String userTerm,
                                                       final String from,
                                                       final String to,
                                                       final String wordsField,
                                                       final String wordsTerm) {
        final ExpressionOperator.Builder operator = ExpressionOperator.builder();
        operator.addTerm(userField, Condition.EQUALS, userTerm);
        operator.addTerm("EventTime", Condition.BETWEEN, from + "," + to);
        operator.addTerm(wordsField, Condition.EQUALS, wordsTerm);
        return operator;
    }

    private ExpressionOperator.Builder buildInExpression(final String userField,
                                                         final String userTerm,
                                                         final String from,
                                                         final String to,
                                                         final String wordsField,
                                                         final String wordsTerm) {
        final ExpressionOperator.Builder operator = ExpressionOperator.builder();
        operator.addTerm(userField, Condition.EQUALS, userTerm);
        operator.addTerm("EventTime", Condition.BETWEEN, from + "," + to);
        operator.addTerm(wordsField, Condition.IN, wordsTerm);

        return operator;
    }
}
