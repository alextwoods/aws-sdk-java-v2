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

package software.amazon.awssdk.benchmark.harness;

import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.response;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import software.amazon.awssdk.benchmark.harness.StreamingWorkloads.Paths;
import software.amazon.awssdk.benchmark.harness.StreamingWorkloads.StreamFixture;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The restXml streaming fixture: S3 {@code PutObject} (streaming blob input) then {@code GetObject} (streaming blob
 * output) of the same object. The upload response carries the object's {@code ETag} and CRC32 checksum headers; the
 * download response carries the content with its CRC32, which the client validates (it sends
 * {@code x-amz-checksum-mode: ENABLED}).
 *
 * <p>The mock routes on the path the V3 S3 client actually sends to an IP-address endpoint override, on every client
 * path: {@code /bucket/bucket/key}. That is the known V3 S3 defect of the bucket being bound both by the endpoint
 * rules' path-style URL and by the modeled {@code {Bucket}} label; it is not corrected here. If the client's path
 * changes, every request is {@code unmatched} and the run is invalid, so the fixture cannot silently measure a
 * different request.
 */
final class S3Workloads extends StreamingWorkloads.Provider {
    static final String BUCKET = "benchmark-bucket";
    static final String KEY = "benchmark-object";
    static final String ETAG = "\"benchmark-etag\"";
    /** The path the V3 S3 client sends for {@link #BUCKET}/{@link #KEY} to an IP endpoint (see the class comment). */
    static final String OBJECT_PATH = "/" + BUCKET + "/" + BUCKET + "/" + KEY;
    static final String XML = "application/xml";

    S3Workloads() {
        super("s3-putobject-getobject", BenchProtocol.REST_XML, "s3", "com.amazonaws.s3#AmazonS3", "PutObject",
              "GetObject");
    }

    /** The base64 big-endian CRC32 of the content, as S3 reports it in {@code x-amz-checksum-crc32}. */
    static String crc32() {
        CRC32 crc = new CRC32();
        crc.update(StreamingWorkloads.sharedContent());
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    @Override
    public StreamFixture fixture(BenchmarkCell cell) {
        String crc = crc32();
        return new StreamFixture(
            OBJECT_PATH, OBJECT_PATH,
            new ServiceFixture.MockResponse(200, XML, new byte[0], Map.of("ETag", ETAG, "x-amz-checksum-crc32", crc)),
            Map.of("ETag", ETAG, "x-amz-checksum-crc32", crc),
            response(404, XML, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error><Code>NoSuchKey</Code>"
                               + "<Message>The specified key does not exist.</Message></Error>"),
            response(500, XML, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error><Code>InternalError</Code>"
                               + "<Message>injected retry</Message></Error>"));
    }

    @Override
    Paths generated(boolean async, URI endpoint, AutoCloseable http) {
        PutObjectRequest put = PutObjectRequest.builder().bucket(BUCKET).key(KEY).build();
        GetObjectRequest get = GetObjectRequest.builder().bucket(BUCKET).key(KEY).build();
        if (async) {
            S3AsyncClient client = S3AsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return StreamingWorkloads.asyncPaths(
                client, () -> client.putObject(put, StreamingWorkloads.asyncBody()).thenApply(r -> r),
                exact -> client.getObject(get, StreamingWorkloads.asyncConsumer(exact)));
        }
        S3Client client = S3Client.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return StreamingWorkloads.syncPaths(client, () -> client.putObject(put, StreamingWorkloads.syncBody()),
                                            exact -> client.getObject(get, StreamingWorkloads.syncConsumer(exact)));
    }

    @Override
    Document dynamicInput() {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("Bucket", Document.of(BUCKET));
        input.put("Key", Document.of(KEY));
        return Document.of(input);
    }

    @Override
    void checkUploadOutput(Object output) {
        String etag;
        String crc;
        if (output instanceof PutObjectResponse r) {
            etag = r.eTag();
            crc = r.checksumCRC32();
        } else {
            Document d = (Document) output;
            etag = d.getMember("ETag").asString();
            crc = d.getMember("ChecksumCRC32").asString();
        }
        if (!ETAG.equals(etag) || !crc32().equals(crc)) {
            throw new IllegalStateException("decoded PutObject output does not match the fixture: ETag=" + etag
                                            + " ChecksumCRC32=" + crc);
        }
    }
}
