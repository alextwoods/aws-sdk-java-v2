/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.dynamicclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Recorded;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Stub;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.PaginatedTrait;

/**
 * {@link DynamicClient#paginate} and {@link DynamicClient#paginateAsync}, which page through an operation with its
 * {@code smithy.api#paginated} trait merged with the service's, as generated paginators do.
 */
class DynamicClientPaginationTest {
    private static final Stub PAGE_1 = Stub.of(200, "{\"nextToken\":\"t1\",\"things\":[\"a\"]}");
    private static final Stub PAGE_2 = Stub.of(200, "{\"nextToken\":\"t2\",\"things\":[\"b\"]}");
    private static final Stub PAGE_3 = Stub.of(200, "{\"things\":[\"c\"]}");

    private final RecordingHttpClients.Sync sync = new RecordingHttpClients.Sync();
    private final RecordingHttpClients.Async async = new RecordingHttpClients.Async();
    private DynamicClient client;

    @BeforeEach
    void setUp() {
        client = CallTestClients.client(DynamicTestModels.pipelineFeatures(), sync, async);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void paginated_mergesTheOperationTraitWithTheServiceTrait() {
        assertThat(client.getOperation("ListThings").paginated())
            .hasValue(new PaginatedTrait("nextToken", "nextToken", "things", "maxResults"));
        assertThat(client.getOperation("ListNested").paginated())
            .hasValue(new PaginatedTrait("nextToken", "meta.next", "things", "maxResults"));
        assertThat(client.getOperation("Ping").paginated()).isEmpty();
    }

    @Test
    void paginate_followsTheOutputTokenUntilItIsAbsent() {
        sync.stub(PAGE_1, PAGE_2, PAGE_3);

        SdkIterable<Document> pages = client.paginate("ListThings", map("prefix", "p", "maxResults", 2));
        assertThat(sync.requests()).isEmpty();
        List<String> things = new ArrayList<>();
        for (Document page : pages) {
            assertThat(page.getDiscriminator().toString()).isEqualTo("test.features#ListThingsOutput");
            page.getMember("things").asList().forEach(t -> things.add(t.asString()));
        }

        assertThat(things).containsExactly("a", "b", "c");
        List<Recorded> requests = sync.requests();
        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).request.rawQueryParameters()).doesNotContainKey("nextToken")
                                                                 .containsEntry("prefix", Collections.singletonList("p"))
                                                                 .containsEntry("maxResults", Collections.singletonList("2"));
        assertThat(requests.get(1).request.rawQueryParameters()).containsEntry("nextToken", Collections.singletonList("t1"))
                                                                 .containsEntry("prefix", Collections.singletonList("p"));
        assertThat(requests.get(2).request.rawQueryParameters()).containsEntry("nextToken", Collections.singletonList("t2"));
        assertThat(requests.get(0).request.firstMatchingHeader("User-Agent").get())
            .matches(BusinessMetricCollection.METRIC_SEARCH_PATTERN.apply("C"));
    }

    @Test
    void paginate_eachIteratorStartsFromTheFirstPage() {
        sync.stub(PAGE_1, PAGE_3, PAGE_1, PAGE_3);
        SdkIterable<Document> pages = client.paginate("ListThings", Document.EMPTY_MAP);

        assertThat(pages).hasSize(2);
        assertThat(pages).hasSize(2);
        assertThat(sync.requests()).hasSize(4);
        assertThat(sync.requests().get(2).request.rawQueryParameters()).doesNotContainKey("nextToken");
    }

    @Test
    void paginate_emptyOutputToken_endsPaging() {
        sync.stub(Stub.of(200, "{\"nextToken\":\"\",\"things\":[]}"));

        assertThat(client.paginate("ListThings", Document.EMPTY_MAP)).hasSize(1);
        assertThat(sync.requests()).hasSize(1);
    }

    @Test
    void paginate_nestedOutputTokenPath_readsTheTokenFromTheNestedMember() {
        sync.stub(Stub.of(200, "{\"meta\":{\"next\":\"n1\"},\"things\":[\"a\"]}"),
                  Stub.of(200, "{\"meta\":{},\"things\":[\"b\"]}"));

        assertThat(client.paginate("ListNested", map("maxResults", 5))).hasSize(2);
        assertThat(sync.requests().get(1).request.rawQueryParameters())
            .containsEntry("nextToken", Collections.singletonList("n1"))
            .containsEntry("maxResults", Collections.singletonList("5"));
    }

    @Test
    void paginate_failedCall_throwsWhenTheIterationReachesIt() {
        sync.stub(PAGE_1, Stub.of(400, "{\"__type\":\"BadRequest\"}"));
        List<Document> pages = new ArrayList<>();

        assertThatThrownBy(() -> client.paginate("ListThings", Document.EMPTY_MAP).forEach(pages::add))
            .isInstanceOf(AwsServiceException.class);
        assertThat(pages).hasSize(1);
    }

    @Test
    void paginateAsync_publishesEveryPage() throws Exception {
        async.stub(PAGE_1, PAGE_2, PAGE_3);
        List<String> things = Collections.synchronizedList(new ArrayList<>());

        SdkPublisher<Document> pages = client.paginateAsync("ListThings", map("prefix", "p"));
        assertThat(async.requests()).isEmpty();
        pages.subscribe(page -> page.getMember("things").asList().forEach(t -> things.add(t.asString())))
             .get(10, TimeUnit.SECONDS);

        assertThat(things).containsExactly("a", "b", "c");
        assertThat(async.requests()).hasSize(3);
        assertThat(async.requests().get(2).request.rawQueryParameters())
            .containsEntry("nextToken", Collections.singletonList("t2"))
            .containsEntry("prefix", Collections.singletonList("p"));
    }

    @Test
    void paginateAsync_failedCall_endsTheSubscriptionWithTheError() {
        async.stub(PAGE_1, Stub.of(400, "{\"__type\":\"BadRequest\"}"));

        assertThat(client.paginateAsync("ListThings", Document.EMPTY_MAP).subscribe(page -> { }))
            .failsWithin(Duration.ofSeconds(10))
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(AwsServiceException.class);
        assertThat(async.requests()).hasSize(2);
    }

    @Test
    void paginate_operationWithoutPaginatedTrait_throwsBeforeSending() {
        String message = "Operation test.features#Ping is not paginated: it needs a smithy.api#paginated trait with an "
                         + "inputToken and an outputToken, set on the operation or inherited from the service. Call it "
                         + "with call(...) instead.";
        assertThatThrownBy(() -> client.paginate("Ping", Document.EMPTY_MAP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(message);
        assertThatThrownBy(() -> client.paginateAsync("Ping", Document.EMPTY_MAP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(message);
        assertThatThrownBy(() -> client.paginate("ListMissing", Document.EMPTY_MAP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation ListMissing not found in service test.features#FeatureService");
        assertThatThrownBy(() -> client.paginate("ListThings", map("unknown", "x")))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("has no member named \"unknown\"");
        assertThat(sync.requests()).isEmpty();
        assertThat(async.requests()).isEmpty();
    }
}
