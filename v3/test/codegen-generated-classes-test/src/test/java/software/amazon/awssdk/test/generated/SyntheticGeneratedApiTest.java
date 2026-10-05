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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.test.generated.support.Collect;
import software.amazon.awssdk.test.generated.support.FakeHttp;
import software.amazon.awssdk.test.generated.support.FakeHttp.Recorded;
import software.amazon.awssdk.test.generated.synthetic.SyntheticServiceAsyncClient;
import software.amazon.awssdk.test.generated.synthetic.SyntheticServiceClient;
import software.amazon.awssdk.test.generated.synthetic.model.GetThingRequest;
import software.amazon.awssdk.test.generated.synthetic.model.GetThingResponse;
import software.amazon.awssdk.test.generated.synthetic.model.ListThingsResponse;
import software.amazon.awssdk.test.generated.synthetic.paginators.ListThingsIterable;
import software.amazon.awssdk.test.generated.synthetic.waiters.SyntheticServiceWaiter;

/**
 * Compile and runtime fixtures of the API generated from the synthetic model in {@code src/main/smithy}: the
 * no-argument overload, a token paginator and a waiter whose retry acceptor matches a modeled error.
 */
class SyntheticGeneratedApiTest {

    private static final URI ENDPOINT = URI.create("https://synthetic.test");
    private static final SdkHttpResponse THING_NOT_FOUND =
        SdkHttpResponse.builder().statusCode(400).addHeader("Content-Type", "application/x-amz-json-1.0")
                       .body(HttpBody.fromUtf8("{\"__type\":\"software.amazon.awssdk.test.generated.synthetic"
                                               + "#ThingNotFound\",\"message\":\"not yet\"}"))
                       .build();

    private static SdkHttpResponse synthetic(Recorded recorded) {
        String target = recorded.request().headers().firstValue("X-Amz-Target").orElse("");
        return switch (target) {
            case "SyntheticService.DescribeService" -> FakeHttp.ok("{\"version\":\"1\"}");
            case "SyntheticService.GetThing" -> FakeHttp.ok("{\"thingId\":\"t\",\"sizeInBytes\":3}");
            case "SyntheticService.ListThings" -> recorded.body().contains("nextToken")
                                                  ? FakeHttp.ok("{\"things\":[\"c\"]}")
                                                  : FakeHttp.ok("{\"things\":[\"a\",\"b\"],\"nextToken\":\"n\"}");
            default -> throw new AssertionError("Unexpected target " + target);
        };
    }

    @Test
    void syncNoArgumentPaginatorAndWaiterSurfaces() {
        FakeHttp http = FakeHttp.sync(SyntheticGeneratedApiTest::synthetic);
        try (SyntheticServiceClient client = SyntheticServiceClient.builder().region(Region.of("us-east-1"))
                                                                   .credentialsProvider(FakeHttp.CREDENTIALS)
                                                                   .endpointOverride(ENDPOINT).httpClient(http)
                                                                   .build();
             SyntheticServiceWaiter waiter = client.waiter()) {
            assertEquals("1", client.describeService().version());
            assertEquals(3L, client.getThing(GetThingRequest.builder().thingId("t").build()).sizeInBytes());
            ListThingsIterable pages = client.listThingsPaginator(r -> r.maxResults(2));
            List<String> things = pages.stream().flatMap(page -> page.things().stream()).toList();
            assertEquals(List.of("a", "b", "c"), things);
            assertTrue(waiter.waitUntilThingExists(r -> r.thingId("t")).matched().response().isPresent());
        }
    }

    @Test
    void waiterRetriesOnTheModeledErrorUntilItSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        FakeHttp http = FakeHttp.sync(r -> calls.getAndIncrement() == 0 ? THING_NOT_FOUND : synthetic(r));
        try (SyntheticServiceClient client = SyntheticServiceClient.builder().region(Region.of("us-east-1"))
                                                                   .credentialsProvider(FakeHttp.CREDENTIALS)
                                                                   .endpointOverride(ENDPOINT).httpClient(http)
                                                                   .build();
             SyntheticServiceWaiter waiter = client.waiter()) {
            WaiterResponse<GetThingResponse> exists = waiter.waitUntilThingExists(r -> r.thingId("t"));
            assertEquals("t", exists.matched().response().orElseThrow().thingId());
            assertEquals(2, exists.attemptsExecuted());
            assertEquals(2, http.requests().size());
        }
    }

    @Test
    void asyncPaginatorPublishesEveryPage() {
        FakeHttp http = FakeHttp.async(SyntheticGeneratedApiTest::synthetic);
        try (SyntheticServiceAsyncClient client = SyntheticServiceAsyncClient.builder()
                                                                             .region(Region.of("us-east-1"))
                                                                             .credentialsProvider(FakeHttp.CREDENTIALS)
                                                                             .endpointOverride(ENDPOINT)
                                                                             .httpClient(http).build()) {
            assertEquals("1", client.describeService().join().version());
            List<ListThingsResponse> pages = Collect.all(client.listThingsPaginator(r -> r.maxResults(2))).join();
            assertEquals(2, pages.size());
        }
    }
}
