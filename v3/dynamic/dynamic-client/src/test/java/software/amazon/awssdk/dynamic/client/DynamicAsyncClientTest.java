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
import static software.amazon.awssdk.dynamic.client.TestSupport.doc;
import static software.amazon.awssdk.dynamic.client.TestSupport.json;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.dynamic.client.TestSupport.RecordingInterceptor;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.awssdk.dynamic.model.DynamicModelException;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * Asynchronous dynamic calls on the shared pipeline (Requirements 14.9, 14.10, 14.11, 14.14, 14.19): the future is
 * returned before the call completes, completes exactly once with the modeled output or the causal SDK exception,
 * on the client's completion executor, and pre-transport rejections are already failed futures.
 */
class DynamicAsyncClientTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String FORECAST = "{\"city\":\"seattle\",\"high\":21}";

    private static Document forecastInput() {
        return doc(Map.of("city", Document.of("seattle")));
    }

    @Test
    void theFutureIsReturnedBeforeCompletionAndCompletesExactlyOnceWithTheOutput() throws Exception {
        TestTransport transport = TestTransport.async(r -> json(200, FORECAST)).holdResponses();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "dynamic-completion"));
        try (DynamicAsyncClient client = TestSupport.asyncClient(transport).asyncCompletionExecutor(executor)
                                                    .build()) {
            CompletableFuture<Document> future = client.callAsync("GetForecast", forecastInput());
            AtomicInteger completions = new AtomicInteger();
            AtomicReference<String> completionThread = new AtomicReference<>();
            future.whenComplete((value, failure) -> {
                completions.incrementAndGet();
                completionThread.set(Thread.currentThread().getName());
            });
            awaitTransmissions(transport, 1);
            assertFalse(future.isDone(), "the call has not terminated while the transport holds its response");

            transport.release();
            Document output = future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertEquals(21, output.getMember("high").asInteger());
            assertEquals("seattle.weather.us-west-2.example.com", transport.requests().get(0).request().host());
            assertEquals(1, completions.get());
            assertEquals("dynamic-completion", completionThread.get());
            // The one outcome is final.
            assertFalse(future.complete(Document.of(Map.of())));
            assertEquals(21, future.join().getMember("high").asInteger());
            assertEquals(1, transport.transmissions());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void modeledErrorsCompleteTheFutureWithTheDynamicServiceException() {
        TestTransport transport = TestTransport.async(r -> json(
            400, "{\"__type\":\"example.weather#NotFound\",\"message\":\"no forecast\",\"city\":\"atlantis\"}"));
        try (DynamicAsyncClient client = TestSupport.asyncClient(transport).build()) {
            CompletableFuture<Document> future = client.callAsync("GetForecast", forecastInput());
            ExecutionException e = assertThrows(ExecutionException.class,
                                                () -> future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
            DynamicServiceException error = assertInstanceOf(DynamicServiceException.class, e.getCause());
            assertEquals("atlantis", error.error().getMember("city").asString());
            assertEquals(400, error.statusCode());
            assertTrue(future.isCompletedExceptionally());
        }
    }

    @Test
    void rejectedCallsReturnFailedFuturesAndSendNothing() {
        TestTransport transport = TestTransport.async(r -> json(200, FORECAST));
        RecordingInterceptor interceptor = new RecordingInterceptor();
        try (DynamicAsyncClient client = TestSupport.asyncClient(transport).addExecutionInterceptor(interceptor)
                                                    .build()) {
            CompletableFuture<Document> unknown = client.callAsync("Forecast", forecastInput());
            DynamicModelException unknownCause = assertInstanceOf(DynamicModelException.class, cause(unknown));
            assertEquals(DynamicModelException.Reason.UNKNOWN_OPERATION, unknownCause.reason());
            assertEquals(List.of("GetForecast", "Ping", "Upload"), unknownCause.available());

            CompletableFuture<Document> invalid = client.callAsync(
                "GetForecast", doc(Map.of("city", Document.of(7))), AwsRequestOverrideConfiguration.builder().build());
            DynamicInvocationException invalidCause = assertInstanceOf(DynamicInvocationException.class,
                                                                       cause(invalid));
            assertEquals(DynamicInvocationException.Reason.INVALID_INPUT, invalidCause.reason());
            assertEquals("input.city", invalidCause.violations().get(0).path());

            CompletableFuture<Document> streaming = client.callAsync("Upload", Document.of(Map.of()));
            assertEquals(DynamicInvocationException.Reason.REQUIRES_STREAMING_CALL,
                         assertInstanceOf(DynamicInvocationException.class, cause(streaming)).reason());
        }
        assertEquals(0, transport.transmissions());
        assertEquals(List.of(), interceptor.hooks);
    }

    @Test
    void asyncCallsShareTheClientsPlansAndInterceptors() throws Exception {
        TestTransport transport = TestTransport.async(r -> json(200, FORECAST));
        RecordingInterceptor interceptor = new RecordingInterceptor();
        try (DynamicAsyncClient client = TestSupport.asyncClient(transport).addExecutionInterceptor(interceptor)
                                                    .build()) {
            client.callAsync("GetForecast", forecastInput()).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            client.callAsync("GetForecast", forecastInput()).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
        assertEquals(List.of("beforeExecution", "beforeTransmission", "afterExecution",
                             "beforeExecution", "beforeTransmission", "afterExecution"), interceptor.hooks);
        assertTrue(interceptor.contexts.get(0).operation() == interceptor.contexts.get(3).operation());
        assertTrue(transport.requests().get(0).header("Authorization").startsWith("AWS4-HMAC-SHA256"));
    }

    private static Throwable cause(CompletableFuture<?> future) {
        assertTrue(future.isCompletedExceptionally(), "a rejected call returns an already failed future");
        ExecutionException e = assertThrows(ExecutionException.class, future::get);
        return e.getCause();
    }

    private static void awaitTransmissions(TestTransport transport, int count) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (transport.transmissions() < count) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("the transport received " + transport.transmissions() + " requests");
            }
            Thread.sleep(5);
        }
    }
}
