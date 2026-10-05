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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.dynamic.client.TestSupport.doc;
import static software.amazon.awssdk.dynamic.client.TestSupport.messageFrame;
import static software.amazon.awssdk.dynamic.client.TestSupport.restJson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.dynamic.client.DynamicInvocationException.Reason;
import software.amazon.awssdk.dynamic.client.TestSupport.Recorded;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamReader;

/**
 * Synchronous dynamic streaming calls (Requirements 14.8, 14.12): streaming blobs through the engine's request-body
 * and response-transformer entry points, event streams as the Smithy Java {@code EventStream} documents generated
 * types hold, read one message per pull, and pre-transport rejection of streaming arguments that do not match the
 * operation's streaming members.
 */
class DynamicStreamingTest {

    private static Document name(String name) {
        return doc(Map.of("name", Document.of(name)));
    }

    @Test
    void aStreamingBlobInputIsSentAsTheRequestBody() {
        TestTransport transport = TestTransport.sync(r -> restJson(200, "{}", "X-Etag", "e1"));
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            Document output = client.call("PutBlob", name("n"), RequestBody.fromString("hello"));
            assertEquals("e1", output.getMember("etag").asString());
        }
        Recorded recorded = transport.requests().get(0);
        assertEquals("PUT", recorded.request().method());
        assertEquals("/blobs", recorded.request().encodedPath());
        assertEquals("n", recorded.header("X-Name"));
        assertEquals("hello", recorded.body());
    }

    @Test
    void aStreamingBlobOutputIsHandedLiveToTheResponseTransformer() {
        AtomicLong read = new AtomicLong();
        TestTransport transport = TestTransport.sync(r -> SdkHttpResponse.builder().statusCode(200)
            .addHeader("X-Kind", "k").addHeader("Content-Type", "application/octet-stream")
            .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                counting(new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8)), read), -1,
                "application/octet-stream")))
            .build());
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            String result = client.call("GetBlob", name("b"), (output, payload) -> {
                assertEquals(0, read.get(), "the SDK reads nothing of the payload before the transformer");
                return output.getMember("kind").asString() + ":"
                       + new String(payload.readAllBytes(), StandardCharsets.UTF_8);
            });
            assertEquals("k:payload", result);
        }
        assertEquals("/blobs/b", transport.requests().get(0).request().encodedPath());
    }

    @Test
    void anInputEventStreamIsTheWriterDocumentOfTheInput() throws Exception {
        TestTransport transport = TestTransport.sync(r -> restJson(200, "{}"));
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            EventStream<SerializableStruct> writer = EventStream.newWriter();
            CompletableFuture<Void> producer = CompletableFuture.runAsync(() -> {
                try {
                    writer.asWriter().write(client.inputEvent("Chat", message("hi")));
                    writer.asWriter().write(client.inputEvent("Chat", message("there")));
                    writer.close();
                } catch (RuntimeException e) {
                    writer.closeWithError(e);
                    throw e;
                }
            });
            CompletableFuture<Document> call = CompletableFuture.supplyAsync(() -> client.call(
                "Chat", doc(Map.of("room", Document.of("r1"), "messages", Document.ofObject(writer)))));
            producer.get(10, TimeUnit.SECONDS);
            call.get(10, TimeUnit.SECONDS);
        }
        Recorded recorded = transport.requests().get(0);
        assertEquals("r1", recorded.header("X-Room"));
        assertEquals("application/vnd.amazon.eventstream", recorded.header("Content-Type"));
        assertTrue(recorded.body().contains("{\"text\":\"hi\"}") && recorded.body().contains("{\"text\":\"there\"}"),
                   recorded.body());
        assertTrue(recorded.body().indexOf("hi") < recorded.body().indexOf("there"), "events in write order");
    }

    @Test
    void anOutputEventStreamIsReadOneMessagePerPull() throws Exception {
        byte[] first = messageFrame("a");
        byte[] second = messageFrame("b");
        ByteArrayOutputStream frames = new ByteArrayOutputStream();
        frames.write(first);
        frames.write(second);
        AtomicLong read = new AtomicLong();
        TestTransport transport = TestTransport.sync(r -> SdkHttpResponse.builder().statusCode(200)
            .addHeader("Content-Type", "application/vnd.amazon.eventstream")
            .body(HttpBody.streaming(StreamingBodies.fromInputStream(
                counting(new ByteArrayInputStream(frames.toByteArray()), read), -1,
                "application/vnd.amazon.eventstream")))
            .build());
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            Document output = client.call("Watch", Document.of(Map.of()));
            EventStreamReader<? extends SerializableStruct> reader = output.getMember("events").asEventStream()
                                                                           .asReader();
            assertTrue(read.get() <= first.length, "no message beyond the first is read before a pull");

            assertEquals("a", text(reader.read()));
            assertEquals(first.length, read.get(), "one pull reads exactly one message");
            assertEquals("b", text(reader.read()));
            assertEquals(first.length + second.length, read.get());
            assertNull(reader.read(), "the stream ends after the last message");
        }
    }

    @Test
    void streamingArgumentsThatDoNotMatchTheOperationAreRejectedBeforeTransport() {
        TestTransport transport = TestTransport.sync(r -> restJson(200, "{}"));
        ResponseTransformer<Document, String> ignore = (output, payload) -> "";
        try (DynamicClient client = TestSupport.mediaClient(transport).build()) {
            DynamicInvocationException noBody = assertThrows(DynamicInvocationException.class,
                                                             () -> client.call("PutBlob", name("n")));
            assertEquals(Reason.REQUIRES_STREAMING_CALL, noBody.reason());
            assertTrue(noBody.getMessage().contains("'body'") && noBody.getMessage().contains("request body"),
                       noBody.getMessage());

            DynamicInvocationException noTransformer = assertThrows(DynamicInvocationException.class,
                                                                    () -> client.call("GetBlob", name("b")));
            assertEquals(Reason.REQUIRES_STREAMING_CALL, noTransformer.reason());
            assertTrue(noTransformer.getMessage().contains("response transformer"), noTransformer.getMessage());

            assertEquals(Reason.NO_STREAMING_MEMBER, assertThrows(DynamicInvocationException.class, () -> client.call(
                "Watch", Document.of(Map.of()), RequestBody.fromString("x"))).reason());
            assertEquals(Reason.NO_STREAMING_MEMBER, assertThrows(DynamicInvocationException.class, () -> client.call(
                "ListItems", Document.of(Map.of()), ignore)).reason());

            DynamicInvocationException inDocument = assertThrows(DynamicInvocationException.class, () -> client.call(
                "PutBlob", doc(Map.of("body", Document.of("x"))), RequestBody.fromString("x")));
            assertEquals(Reason.INVALID_INPUT, inDocument.reason());
            assertEquals("input.body", inDocument.violations().get(0).path());

            DynamicInvocationException notAStream = assertThrows(DynamicInvocationException.class, () -> client.call(
                "Chat", doc(Map.of("messages", Document.of("x")))));
            assertEquals(Reason.INVALID_INPUT, notAStream.reason());
            assertEquals("input.messages", notAStream.violations().get(0).path());

            DynamicInvocationException badEvent = assertThrows(DynamicInvocationException.class,
                () -> client.inputEvent("Chat", doc(Map.of("nope", doc(Map.of())))));
            assertEquals(Reason.INVALID_INPUT, badEvent.reason());
            assertEquals(List.of("event.nope", "event"),
                         badEvent.violations().stream().map(DynamicInvocationException.Violation::path).toList());
            assertEquals(Reason.NO_STREAMING_MEMBER, assertThrows(DynamicInvocationException.class,
                () -> client.inputEvent("ListItems", message("x"))).reason());
        }
        assertEquals(0, transport.transmissions());
    }

    private static Document message(String text) {
        return doc(Map.of("message", doc(Map.of("text", Document.of(text)))));
    }

    private static String text(SerializableStruct event) {
        Document union = assertInstanceOf(Document.class, event);
        return union.getMember("message").getMember("text").asString();
    }

    private static InputStream counting(InputStream source, AtomicLong read) {
        return new FilterInputStream(source) {
            @Override
            public int read() throws IOException {
                int b = super.read();
                if (b >= 0) {
                    read.incrementAndGet();
                }
                return b;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                int n = super.read(buffer, offset, length);
                if (n > 0) {
                    read.addAndGet(n);
                }
                return n;
            }
        };
    }
}
