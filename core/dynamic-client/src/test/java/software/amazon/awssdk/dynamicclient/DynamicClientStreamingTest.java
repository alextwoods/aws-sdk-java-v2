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

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Recorded;
import software.amazon.awssdk.dynamicclient.RecordingHttpClients.Stub;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Streaming members on dynamic calls through the shared pipeline (Requirement 31): streaming blob inputs from request
 * bodies, streaming blob outputs through response transformers, and the checks that fail a call before anything is
 * sent, including every event stream call (event streams are not supported by the schema-based pipeline yet).
 */
class DynamicClientStreamingTest {
    private static final byte[] CONTENT = "archive bytes".getBytes(StandardCharsets.UTF_8);

    private final RecordingHttpClients.Sync sync = new RecordingHttpClients.Sync();
    private final RecordingHttpClients.Async async = new RecordingHttpClients.Async();
    private final HookRecorder hooks = new HookRecorder();
    private DynamicClient client;

    @BeforeEach
    void setUp() {
        client = CallTestClients.client(DynamicTestModels.streaming(), sync, async, hooks);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void call_streamingBlobInput_sendsTheRequestBodyWithItsLength() {
        sync.stub(Stub.of(200, "", "X-Version", "3"));

        Document output = client.call("UploadArchive", map("name", "a1", "tag", "t",
                                                           "body", DynamicStreams.dataStream(RequestBody.fromBytes(CONTENT))));

        Recorded sent = sync.lastRequest();
        assertThat(sent.request.method().name()).isEqualTo("PUT");
        assertThat(sent.request.encodedPath()).isEqualTo("/archives/a1");
        assertThat(sent.bytes).isEqualTo(CONTENT);
        assertThat(sent.request.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(CONTENT.length));
        assertThat(sent.request.firstMatchingHeader("Content-Type")).hasValue("application/octet-stream");
        assertThat(sent.request.firstMatchingHeader("Transfer-Encoding")).isEmpty();
        assertThat(sent.request.firstMatchingHeader("X-Tag")).hasValue("t");
        assertThat(sent.request.firstMatchingHeader("Authorization")).hasValueSatisfying(
            a -> assertThat(a).startsWith("AWS4-HMAC-SHA256 Credential=").contains("/archives/aws4_request"));
        assertThat(output.getMember("version").asInteger()).isEqualTo(3);
        assertThat(hooks.bodyTypes).containsExactly(ShapeType.BLOB);
    }

    @Test
    void callAsync_streamingBlobInput_sendsTheAsyncRequestBodyWithItsLength() {
        async.stub(Stub.of(200, "", "X-Version", "4"));

        Document output = client.callAsync("UploadArchive",
                                           map("name", "a2", "body",
                                               DynamicStreams.dataStream(AsyncRequestBody.fromBytes(CONTENT))))
                                .join();

        Recorded sent = async.lastRequest();
        assertThat(sent.request.encodedPath()).isEqualTo("/archives/a2");
        assertThat(sent.bytes).isEqualTo(CONTENT);
        assertThat(sent.request.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(CONTENT.length));
        assertThat(sent.request.firstMatchingHeader("Content-Type")).hasValue("application/octet-stream");
        assertThat(output.getMember("version").asInteger()).isEqualTo(4);
    }

    @Test
    void call_streamingBlobInputWithoutLengthOnAnUnsignedPayloadOperation_isSentChunked() {
        RequestBody body = RequestBody.fromContentProvider(() -> new ByteArrayInputStream(CONTENT), "text/plain");

        client.call("UploadUnsigned", map("body", DynamicStreams.dataStream(body)));
        client.callAsync("UploadUnsigned",
                         map("body", DynamicStreams.dataStream(AsyncRequestBody.fromPublisher(
                             AsyncRequestBody.fromBytes(CONTENT)))))
              .join();

        for (Recorded sent : new Recorded[] {sync.lastRequest(), async.lastRequest()}) {
            assertThat(sent.bytes).isEqualTo(CONTENT);
            assertThat(sent.request.firstMatchingHeader("Transfer-Encoding")).hasValue("chunked");
            assertThat(sent.request.firstMatchingHeader("Content-Length")).isEmpty();
        }
        assertThat(sync.lastRequest().request.firstMatchingHeader("Content-Type")).hasValue("text/plain");
    }

    @Test
    void call_streamingBlobInputWithAFlexibleChecksum_sendsTheChecksumOfTheBody() throws Exception {
        client.call("UploadChecked", map("algorithm", "SHA256",
                                         "body", DynamicStreams.dataStream(RequestBody.fromBytes(CONTENT))));
        client.callAsync("UploadChecked", map("algorithm", "SHA256",
                                              "body", DynamicStreams.dataStream(AsyncRequestBody.fromBytes(CONTENT))))
              .join();

        byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(CONTENT);
        for (Recorded sent : new Recorded[] {sync.lastRequest(), async.lastRequest()}) {
            // The checksum and the signed payload hash are both computed over the streamed body, which is sent as is.
            assertThat(sent.bytes).isEqualTo(CONTENT);
            assertThat(sent.request.firstMatchingHeader("x-amz-checksum-sha256"))
                .hasValue(Base64.getEncoder().encodeToString(sha256));
            assertThat(sent.request.firstMatchingHeader("x-amz-content-sha256")).hasValue(BinaryUtils.toHex(sha256));
            assertThat(sent.request.firstMatchingHeader("Content-Length")).hasValue(String.valueOf(CONTENT.length));
            assertThat(sent.request.firstMatchingHeader("Authorization")).hasValueSatisfying(
                a -> assertThat(a).contains("x-amz-checksum-sha256;x-amz-content-sha256"));
        }
    }

    @Test
    void call_optionalStreamingBlobInputAbsent_sendsNoBody() {
        client.call("UploadOptional", Document.EMPTY_MAP);
        client.callAsync("UploadOptional").join();

        assertThat(sync.lastRequest().bytes).isEmpty();
        assertThat(async.lastRequest().bytes).isEmpty();
    }

    @Test
    void call_streamingBlobOutput_givesTheTransformerTheOutputDocumentAndTheBody() {
        sync.stub(Stub.of(200, "downloaded bytes", "X-Version", "7"));

        ResponseBytes<Document> result = client.call("DownloadArchive", map("name", "a1"), ResponseTransformer.toBytes());

        assertThat(result.asUtf8String()).isEqualTo("downloaded bytes");
        Document output = result.response();
        assertThat(output.getDiscriminator().toString()).isEqualTo("test.streaming#DownloadArchiveOutput");
        assertThat(output.getMember("version").asInteger()).isEqualTo(7);
        assertThat(output.getMemberNames()).containsExactly("version");
        assertThat(sync.lastRequest().request.encodedPath()).isEqualTo("/archives/a1");
    }

    @Test
    void callAsync_streamingBlobOutput_streamsTheBodyToTheTransformer() {
        async.stub(Stub.of(200, "async bytes", "X-Version", "8"));

        ResponseBytes<Document> result = client.callAsync("DownloadArchive", map("name", "a1"),
                                                          AsyncResponseTransformer.toBytes())
                                               .join();

        assertThat(result.asUtf8String()).isEqualTo("async bytes");
        assertThat(result.response().getMember("version").asInteger()).isEqualTo(8);
        assertThat(result.response().getMemberNames()).containsExactly("version");
    }

    @Test
    void call_streamingBlobOutputErrorResponse_isThrownWithoutCallingTheTransformer() {
        sync.stub(Stub.of(404, "{\"message\":\"missing\"}"));
        async.stub(Stub.of(404, "{\"message\":\"missing\"}"));
        List<String> calls = new CopyOnWriteArrayList<>();
        ResponseTransformer<Document, String> transformer = (response, in) -> {
            calls.add("transform");
            return IoUtils.toUtf8String(in);
        };
        RecordingAsyncTransformer asyncTransformer = new RecordingAsyncTransformer();

        assertThatThrownBy(() -> client.call("DownloadArchive", map("name", "a1"), transformer))
            .isInstanceOf(AwsServiceException.class)
            .satisfies(e -> assertThat(((AwsServiceException) e).statusCode()).isEqualTo(404));
        assertThat(client.callAsync("DownloadArchive", map("name", "a1"), asyncTransformer))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(AwsServiceException.class);

        assertThat(calls).isEmpty();
        assertThat(asyncTransformer.calls).doesNotContain("onResponse", "onStream").contains("exceptionOccurred");
    }

    @Test
    void call_requiredStreamingBlobInputAbsent_failsBeforeSending() {
        assertThatThrownBy(() -> client.call("UploadArchive", map("name", "a1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation test.streaming#UploadArchive needs a value for its required streaming member "
                        + "test.streaming#UploadArchiveInput$body: a data stream document from "
                        + "DynamicStreams.dataStream(RequestBody). No request was sent.");
        assertThat(client.callAsync("UploadArchive", map("name", "a1")))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessageContaining("a data stream document from DynamicStreams.dataStream(AsyncRequestBody)");

        assertNothingSent();
    }

    @Test
    void call_blobDocumentForAStreamingBlobInput_failsBeforeSending() {
        assertThatThrownBy(() -> client.call("UploadArchive", map("name", "a1", "body", Document.of(CONTENT))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation test.streaming#UploadArchive: the streaming member "
                        + "test.streaming#UploadArchiveInput$body of call needs a data stream document from "
                        + "DynamicStreams.dataStream(RequestBody), but found a blob document. No request was sent.");
        assertThat(client.callAsync("UploadArchive", map("name", "a1", "body", "text")))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessageEndingWith("but found a string document. No request was sent.");

        assertNothingSent();
    }

    @Test
    void call_dataStreamOfTheOtherCallForm_failsBeforeSending() {
        Document asyncBody = DynamicStreams.dataStream(AsyncRequestBody.fromBytes(CONTENT));
        Document syncBody = DynamicStreams.dataStream(RequestBody.fromBytes(CONTENT));

        assertThatThrownBy(() -> client.call("UploadArchive", map("name", "a1", "body", asyncBody)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageEndingWith("of call needs a data stream document from DynamicStreams.dataStream(RequestBody), "
                                  + "but found a data stream that was not created from a RequestBody. No request was "
                                  + "sent.");
        assertThat(client.callAsync("UploadArchive", map("name", "a1", "body", syncBody)))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessageEndingWith("but found a data stream that was not created from an AsyncRequestBody. No request "
                                   + "was sent.");

        assertNothingSent();
    }

    @Test
    void call_streamingBlobOutputWithoutATransformer_failsBeforeSending() {
        assertThatThrownBy(() -> client.call("DownloadArchive", map("name", "a1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation test.streaming#DownloadArchive has the streaming blob output member "
                        + "test.streaming#DownloadArchiveOutput$body, so it can be called only with a "
                        + "ResponseTransformer. No request was sent.");
        assertThat(client.callAsync("DownloadArchive", map("name", "a1")))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessageContaining("only with a AsyncResponseTransformer");
        assertThatThrownBy(() -> client.call("Describe", Document.EMPTY_MAP, ResponseTransformer.toBytes()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation test.streaming#Describe has no streaming blob output member, so it cannot be "
                        + "called with a response transformer. No request was sent.");
        RecordingAsyncTransformer asyncTransformer = new RecordingAsyncTransformer();
        assertThat(client.callAsync("Describe", Document.EMPTY_MAP, asyncTransformer))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(IllegalArgumentException.class);
        assertThat(asyncTransformer.calls).containsExactly("exceptionOccurred");

        assertNothingSent();
    }

    @Test
    void call_eventStreamOperation_failsBeforeSending() {
        Document events = map("events", DynamicStreams.eventStream(new NoEvents()));

        assertThatThrownBy(() -> client.call("PublishEvents", events))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation test.streaming#PublishEvents has the event stream member "
                        + "test.streaming#PublishEventsInput$events, so it can be called only with callAsync. No "
                        + "request was sent.");
        assertThatThrownBy(() -> client.call("WatchEvents"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has the event stream member test.streaming#WatchEventsOutput$events, so it can be "
                                  + "called only with callAsync");
        assertThat(client.callAsync("PublishEvents", events))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(UnsupportedOperationException.class)
            .withMessage("The dynamic client does not support event streams yet: operation "
                         + "test.streaming#PublishEvents has the event stream member "
                         + "test.streaming#PublishEventsInput$events. No request was sent.");
        assertThat(client.callAsync("WatchEvents"))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(UnsupportedOperationException.class);

        assertNothingSent();
    }

    @Test
    void callAsync_eventStreamInputMissingOrMistyped_failsBeforeSending() {
        assertThat(client.callAsync("PublishEvents", map("channel", "c")))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessage("Operation test.streaming#PublishEvents needs a value for its required streaming member "
                         + "test.streaming#PublishEventsInput$events: an event stream document from "
                         + "DynamicStreams.eventStream(Publisher). No request was sent.");
        assertThat(client.callAsync("PublishEvents", map("events", map("message", map("text", "hi")))))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessage("Operation test.streaming#PublishEvents: the streaming member "
                         + "test.streaming#PublishEventsInput$events of callAsync needs an event stream document from "
                         + "DynamicStreams.eventStream(Publisher), but found a map document. No request was sent.");

        assertNothingSent();
    }

    @Test
    void createStruct_streamingMembers_keepTheStreamDocuments() {
        Document body = DynamicStreams.dataStream(RequestBody.fromBytes(CONTENT));
        NoEvents publisher = new NoEvents();

        Document upload = client.createStruct(
            software.amazon.smithy.model.shapes.ShapeId.from("test.streaming#UploadArchiveInput"),
            map("name", "a1", "body", body));
        Document publish = client.createStruct(
            software.amazon.smithy.model.shapes.ShapeId.from("test.streaming#PublishEventsInput"),
            map("events", DynamicStreams.eventStream(publisher)));

        assertThat(upload.getMember("body").asDataStream()).isSameAs(body.asDataStream());
        assertThat(upload.getMember("body")).isEqualTo(body);
        assertThat(publish.getMember("events").asEventStream()).isSameAs(publisher);
        assertThatThrownBy(() -> client.createStruct(
            software.amazon.smithy.model.shapes.ShapeId.from("test.streaming#PublishEventsInput"),
            map("events", map("message", map("text", "hi")))))
            .hasMessage("Cannot coerce member test.streaming#PublishEventsInput$events: an event stream member needs "
                        + "an event stream document, but found a map document. Create one with "
                        + "DynamicStreams.eventStream(Publisher).");
    }

    @Test
    void call_streamingBlobInputOnAnRpcProtocol_failsBeforeSending() {
        try (DynamicClient json = CallTestClients.client(DynamicTestModels.streamingJson10(), sync, async)) {
            assertThatThrownBy(() -> json.call("PutBlob",
                                               map("body", DynamicStreams.dataStream(RequestBody.fromBytes(CONTENT)))))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("Streaming blob input is not supported by aws.protocols#awsJson1_0");
        }

        assertThat(sync.requests()).isEmpty();
    }

    private void assertNothingSent() {
        assertThat(sync.requests()).isEmpty();
        assertThat(async.requests()).isEmpty();
        assertThat(hooks.bodyTypes).isEmpty();
        assertThat(hooks.beforeExecution).isZero();
    }

    /**
     * Records the calls that reach the pipeline, and the type of the streaming input member interceptors see.
     */
    private static final class HookRecorder implements ExecutionInterceptor {
        private final List<ShapeType> bodyTypes = new CopyOnWriteArrayList<>();
        private volatile int beforeExecution;

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            beforeExecution++;
            Document body = ((DynamicRequest) context.request()).document().getMember("body");
            if (body != null) {
                bodyTypes.add(body.type());
            }
        }
    }

    private static final class RecordingAsyncTransformer implements AsyncResponseTransformer<Document, String> {
        private final List<String> calls = new CopyOnWriteArrayList<>();
        private final CompletableFuture<String> result = new CompletableFuture<>();

        @Override
        public CompletableFuture<String> prepare() {
            return result;
        }

        @Override
        public void onResponse(Document response) {
            calls.add("onResponse");
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            calls.add("onStream");
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            calls.add("exceptionOccurred");
            result.completeExceptionally(error);
        }
    }

    /**
     * A publisher of events that is never subscribed to: event stream calls fail before sending.
     */
    private static final class NoEvents implements Publisher<Document> {
        @Override
        public void subscribe(org.reactivestreams.Subscriber<? super Document> subscriber) {
            throw new UnsupportedOperationException("not subscribed in these tests");
        }
    }
}
