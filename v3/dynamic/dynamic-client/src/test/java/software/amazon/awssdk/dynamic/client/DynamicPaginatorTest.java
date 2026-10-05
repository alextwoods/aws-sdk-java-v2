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

package software.amazon.awssdk.dynamic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.dynamic.client.ControlledHttp.EVENT_LOOP;
import static software.amazon.awssdk.dynamic.client.ControlledHttp.await;
import static software.amazon.awssdk.dynamic.client.TestSupport.doc;
import static software.amazon.awssdk.dynamic.client.TestSupport.restJson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.dynamic.client.ControlledHttp.Exchange;
import software.amazon.awssdk.dynamic.client.ControlledHttp.PageSubscriber;
import software.amazon.awssdk.dynamic.client.ControlledHttp.Reply;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.awssdk.dynamic.model.DynamicModelException;
import software.amazon.awssdk.dynamic.model.DynamicPagination;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * Dynamic paginators (Requirements 14.13, 14.15): modeled-token sync iteration and demand-driven async publishers on
 * the sdk-core paginator runtime generated paginators use, with lexical diagnostics for operations that are not
 * paginated and cancellation of the in-flight page call through the shared pipeline.
 */
class DynamicPaginatorTest {

    /** Three pages: t1, t2, then an empty token, which ends pagination as a generated paginator does. */
    private static String page(String query) {
        String q = query == null ? "" : query;
        if (q.contains("next=t1")) {
            return "{\"nextToken\":\"t2\",\"items\":[\"c\"]}";
        }
        if (q.contains("next=t2")) {
            return "{\"nextToken\":\"\",\"items\":[\"d\"]}";
        }
        return "{\"nextToken\":\"t1\",\"items\":[\"a\",\"b\"]}";
    }

    private static Document limit(int limit) {
        return doc(Map.of("limit", Document.of(limit)));
    }

    @Test
    void syncPagesAreFetchedLazilyAndFollowTheModeledToken() {
        TestTransport transport = TestTransport.sync(r -> restJson(200, page(r.request().encodedQuery())));
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            DynamicPages pages = client.paginator("ListItems", limit(2));
            assertEquals(new DynamicPagination("ListItems", "nextToken", List.of("nextToken"), List.of("items"),
                                               "limit"), pages.pagination());
            Iterator<Document> iterator = pages.iterator();
            assertEquals(0, transport.transmissions(), "nothing is sent before the first pull");

            assertTrue(iterator.hasNext());
            assertEquals("t1", iterator.next().getMember("nextToken").asString());
            assertEquals(1, transport.transmissions());
            List<String> tokens = new ArrayList<>();
            iterator.forEachRemaining(p -> tokens.add(p.getMember("nextToken").asString()));
            assertEquals(List.of("t2", ""), tokens);
            assertEquals(3, transport.transmissions());
            assertFalse(iterator.hasNext());

            List<String> items = new ArrayList<>();
            pages.items().forEach(item -> items.add(item.asString()));
            assertEquals(List.of("a", "b", "c", "d"), items, "each iterator starts again from the first page");
        }
        List<String> queries = transport.requests().stream().map(r -> r.request().encodedQuery()).toList();
        assertFalse(queries.get(0).contains("next="), queries.get(0));
        assertTrue(queries.get(1).contains("next=t1") && queries.get(1).contains("limit=2"),
                   "the next page is the first input with the token: " + queries.get(1));
        assertTrue(queries.get(2).contains("next=t2"), queries.get(2));
    }

    @Test
    void operationsThatAreNotPaginatedAreRejectedWithLexicalDiagnostics() {
        TestTransport transport = TestTransport.sync(r -> restJson(200, "{}"));
        try (DynamicClient client = TestSupport.mediaClient(transport).build();
             DynamicAsyncClient async = TestSupport.mediaAsyncClient(new ControlledHttp()).build()) {
            DynamicModelException notPaginated = assertThrows(
                DynamicModelException.class, () -> client.paginator("GetItem", doc(Map.of("id", Document.of("a")))));
            assertEquals(DynamicModelException.Reason.NOT_PAGINATED, notPaginated.reason());
            assertEquals(Optional.of("GetItem"), notPaginated.operationName());
            assertEquals(List.of("ListItems"), notPaginated.available());
            assertEquals(DynamicModelException.Reason.NOT_PAGINATED,
                         assertThrows(DynamicModelException.class,
                                      () -> async.paginator("Watch", Document.of(Map.of()))).reason());
            assertEquals(DynamicModelException.Reason.UNKNOWN_OPERATION,
                         assertThrows(DynamicModelException.class,
                                      () -> client.paginator("listItems", Document.of(Map.of()))).reason());
            DynamicInvocationException invalid = assertThrows(
                DynamicInvocationException.class,
                () -> client.paginator("ListItems", doc(Map.of("limit", Document.of("two")))));
            assertEquals("input.limit", invalid.violations().get(0).path());
        }
        assertEquals(0, transport.transmissions());
    }

    @Test
    void asyncPagesAreFetchedOnePerUnitOfDemandAndComplete() throws Exception {
        ControlledHttp http = new ControlledHttp();
        http.auto = request -> Reply.json(200, page(request.encodedQuery()));
        try (DynamicAsyncClient client = TestSupport.mediaAsyncClient(http).build()) {
            PageSubscriber pages = new PageSubscriber();
            client.paginator("ListItems", limit(2)).subscribe(pages);
            Subscription subscription = pages.subscription();
            http.settle();
            assertEquals(0, http.calls.get(), "no call before demand");

            subscription.request(1);
            await(() -> pages.pages.size() == 1);
            http.settle();
            assertEquals(1, http.calls.get(), "no page is fetched after demand reaches zero");
            subscription.request(10);
            await(() -> pages.terminal.get() != null);
            assertEquals("complete", pages.terminal.get());
            assertEquals(3, pages.pages.size());
            assertEquals(3, http.calls.get());
            assertTrue(pages.threads.stream().noneMatch(t -> t.getName().equals(EVENT_LOOP)),
                       "pages on " + pages.threads);
        } finally {
            http.close();
        }
    }

    @Test
    void aFailedAsyncPageFailsTheSubscriptionWithItsSdkException() throws Exception {
        ControlledHttp http = new ControlledHttp();
        http.auto = request -> request.encodedQuery() != null && request.encodedQuery().contains("next=t1")
                               ? Reply.error(400, "Broken") : Reply.json(200, page(request.encodedQuery()));
        try (DynamicAsyncClient client = TestSupport.mediaAsyncClient(http).build()) {
            PageSubscriber pages = new PageSubscriber();
            client.paginator("ListItems", Document.of(Map.of())).subscribe(pages);
            pages.subscription().request(5);
            await(() -> pages.terminal.get() != null);
            assertEquals("error", pages.terminal.get());
            assertEquals(1, pages.pages.size());
            assertInstanceOf(AwsServiceException.class, pages.error.get());
        } finally {
            http.close();
        }
    }

    @Test
    void cancellingThePageSubscriptionCancelsTheInFlightCall() throws Exception {
        ControlledHttp http = new ControlledHttp();
        try (DynamicAsyncClient client = TestSupport.mediaAsyncClient(http).build()) {
            PageSubscriber pages = new PageSubscriber();
            client.paginator("ListItems", Document.of(Map.of())).subscribe(pages);
            pages.subscription().request(1);
            Exchange exchange = http.next();

            pages.subscription().cancel();
            await(() -> exchange.cancels.get() == 1);
            assertEquals(List.of(CancellationReason.CALLER_CANCELLED), exchange.reasons);
            exchange.respond(Reply.json(200, page(null)));
            pages.subscription().request(1);
            http.settle();
            assertTrue(pages.pages.isEmpty(), "no page after cancellation");
            assertEquals(null, pages.terminal.get(), "a cancelled subscription is not signalled");
            assertEquals(1, http.calls.get());
        } finally {
            http.close();
        }
    }
}
