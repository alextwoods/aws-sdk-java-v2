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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.test.generated.support.FakeHttp;
import software.amazon.awssdk.test.generated.support.FakeHttp.Recorded;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedObjectTransfer;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The {@code ADAPTED} members of the V2 S3 Transfer Manager API (task 14.6; Requirements 16.7, 16.12, 19.12), run
 * through the V3 {@code s3-transfer-manager} artifact over this module's generated {@link S3AsyncClient}: the
 * evidence that classification-rules.json names for them, executed by the generated behavior fixtures.
 *
 * <ul>
 *   <li>{@code UploadRequest.Builder#requestBody}: a caller body is retried after its transmission began only when it
 *       declares itself replayable ({@link AsyncRequestBody#isReplayable()}); a custom body that does not is sent
 *       once, where V2 subscribed to it again.</li>
 *   <li>{@code CompletedObjectTransfer#response()} and {@code TransferProgressSnapshot#sdkResponse()}: the V3 response
 *       supertype is {@link SerializableStruct}; the value is the typed S3 response.</li>
 * </ul>
 *
 * <p>It also checks the premise that lets this module put the high-level libraries on its class path: they were built
 * against the fleet's generated service artifacts, which are excluded here, so this module's generated DynamoDB and S3
 * classes must come from the same canonical model revision, and be the only copies on the class path.
 */
class S3TransferManagerCompatibilityTest {
    private static final String PAYLOAD = "transfer manager payload";

    @Test
    void upload_customBodyNotDeclaringReplayable_isSentOnceAndNotRetriedAfterTransmission() {
        AtomicInteger attempts = new AtomicInteger();
        FakeHttp http = FakeHttp.async(recorded -> {
            if (attempts.incrementAndGet() == 1) {
                throw connectionReset();
            }
            return ok();
        });
        CustomBody body = new CustomBody();

        try (S3AsyncClient client = client(http); S3TransferManager tm = transferManager(client)) {
            Upload upload = tm.upload(u -> u.requestBody(body).putObjectRequest(p -> p.bucket("bucket").key("k")));

            RuntimeException failure = assertThrows(RuntimeException.class, () -> join(upload.completionFuture()));
            assertTrue(hasCause(failure, HttpTransportException.class), String.valueOf(failure));
        }
        assertFalse(body.isReplayable());
        assertEquals(1, http.requests().size(), "a body that is not replayable is not retransmitted");
        assertEquals(1, body.subscriptions.get(), "transmission is the only read");
        assertEquals(PAYLOAD, http.requests().get(0).decodedBody());
    }

    @Test
    void upload_customBodyDeclaringReplayable_isRetriedWithIdenticalBytes() {
        AtomicInteger attempts = new AtomicInteger();
        FakeHttp http = FakeHttp.async(recorded -> {
            if (attempts.incrementAndGet() == 1) {
                throw connectionReset();
            }
            return ok();
        });
        CustomBody body = new ReplayableCustomBody();

        try (S3AsyncClient client = client(http); S3TransferManager tm = transferManager(client)) {
            CompletedUpload done = join(tm.upload(u -> u.requestBody(body)
                                                        .putObjectRequest(p -> p.bucket("bucket").key("k")))
                                          .completionFuture());
            assertEquals("\"etag\"", done.response().eTag());
        }
        assertEquals(2, http.requests().size(), "the failed transmission is retried");
        assertEquals(2, body.subscriptions.get());
        for (Recorded sent : http.requests()) {
            assertEquals(PAYLOAD, sent.decodedBody(), "the identical bytes");
        }
    }

    @Test
    void upload_factoryBody_isRetriedAsInV2() {
        AtomicInteger attempts = new AtomicInteger();
        FakeHttp http = FakeHttp.async(recorded -> {
            if (attempts.incrementAndGet() == 1) {
                throw connectionReset();
            }
            return ok();
        });

        try (S3AsyncClient client = client(http); S3TransferManager tm = transferManager(client)) {
            join(tm.upload(u -> u.requestBody(AsyncRequestBody.fromString(PAYLOAD))
                                 .putObjectRequest(p -> p.bucket("bucket").key("k")))
                   .completionFuture());
        }
        assertEquals(2, http.requests().size());
        assertEquals(PAYLOAD, http.requests().get(1).decodedBody());
    }

    @Test
    void completedTransferAndProgressSnapshot_exposeTheTypedS3ResponseAsSerializableStruct() {
        FakeHttp http = FakeHttp.async(recorded -> ok());

        try (S3AsyncClient client = client(http); S3TransferManager tm = transferManager(client)) {
            Upload upload = tm.upload(u -> u.requestBody(AsyncRequestBody.fromString(PAYLOAD))
                                            .putObjectRequest(p -> p.bucket("bucket").key("k")));
            CompletedUpload done = join(upload.completionFuture());

            CompletedObjectTransfer completed = done;
            SerializableStruct response = completed.response();
            assertSame(done.response(), response, "the supertype view is the typed PutObjectResponse");
            assertInstanceOf(PutObjectResponse.class, response);

            Optional<SerializableStruct> snapshot = upload.progress().snapshot().sdkResponse();
            assertTrue(snapshot.isPresent(), "the completed transfer's snapshot carries the response");
            PutObjectResponse typed = assertInstanceOf(PutObjectResponse.class, snapshot.get());
            assertEquals("\"etag\"", typed.eTag());
        }
    }

    @Test
    void theHighLevelLibrariesLinkAgainstThisModulesServicesGeneratedFromTheFleetModelRevision() throws Exception {
        String fleetPom = Files.readString(Path.of(System.getProperty("v3.fixture.fleetPom")), StandardCharsets.UTF_8);
        Matcher revision = Pattern.compile("<aws\\.models\\.revision>([0-9a-f]{40})</aws\\.models\\.revision>")
                                  .matcher(fleetPom);
        assertTrue(revision.find(), "no aws.models.revision in the fleet POM");
        assertEquals(revision.group(1), System.getProperty("v3.fixture.canonicalModelRevision"),
                     "this module generates DynamoDB and S3 from another model revision than the fleet artifacts the "
                     + "high-level libraries were built against");

        // Exactly one copy of each generated service on the class path: this module's.
        URI generated = S3AsyncClient.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        assertEquals(generated, DynamoDbClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Path.of(generated).endsWith(Path.of("target", "classes")), generated.toString());
        for (String resource : new String[] {"software/amazon/awssdk/services/s3/S3AsyncClient.class",
                                             "software/amazon/awssdk/services/dynamodb/DynamoDbClient.class"}) {
            assertEquals(1, java.util.Collections.list(getClass().getClassLoader().getResources(resource)).size(),
                         resource + " is on the class path more than once");
        }
    }

    /**
     * A custom caller body written the V2 way: it replays the same bytes on every subscription but does not override
     * {@link AsyncRequestBody#isReplayable()}. Counts its subscriptions.
     */
    private static class CustomBody implements AsyncRequestBody {
        final AtomicInteger subscriptions = new AtomicInteger();

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) PAYLOAD.length());
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriptions.incrementAndGet();
            AsyncRequestBody.fromString(PAYLOAD).subscribe(subscriber);
        }
    }

    /** The same body, declaring itself replayable: the V3 migration of a V2 custom body. */
    private static final class ReplayableCustomBody extends CustomBody {
        @Override
        public boolean isReplayable() {
            return true;
        }
    }

    private static S3AsyncClient client(FakeHttp http) {
        return S3AsyncClient.builder().region(Region.of("us-west-2")).credentialsProvider(FakeHttp.CREDENTIALS)
                            .endpointOverride(URI.create("https://s3.test")).httpClient(http).build();
    }

    private static S3TransferManager transferManager(S3AsyncClient client) {
        return S3TransferManager.builder().s3Client(client).build();
    }

    private static SdkHttpResponse ok() {
        return SdkHttpResponse.builder().statusCode(200).addHeader("ETag", "\"etag\"").body(HttpBody.empty()).build();
    }

    private static HttpTransportException connectionReset() {
        return HttpTransportException.create(HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.IO,
                                             "connection reset", null);
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException runtime ? runtime : new CompletionException(e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
