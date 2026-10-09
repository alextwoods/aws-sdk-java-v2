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
import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.utf8;

import java.net.URI;
import java.util.Map;
import software.amazon.awssdk.benchmark.harness.StreamingWorkloads.Paths;
import software.amazon.awssdk.benchmark.harness.StreamingWorkloads.StreamFixture;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mediastoredata.MediaStoreDataAsyncClient;
import software.amazon.awssdk.services.mediastoredata.MediaStoreDataClient;
import software.amazon.awssdk.services.mediastoredata.model.GetObjectRequest;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectRequest;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The restJson1 streaming fixture: AWS Elemental MediaStore Data {@code PutObject} (streaming blob input, the
 * {@code {Path+}} greedy label) then {@code GetObject} (streaming blob output) of the same object. The upload response
 * is the JSON {@code PutObjectResponse}, whose {@code ETag} the client decodes; the download response is the content.
 */
final class MediaStoreDataWorkloads extends StreamingWorkloads.Provider {
    static final String OBJECT = "benchmark/object";
    static final String ETAG = "benchmark-etag";
    static final String JSON = "application/json";

    MediaStoreDataWorkloads() {
        super("mediastoredata-putobject-getobject", BenchProtocol.REST_JSON_1, "mediastoredata",
              "com.amazonaws.mediastoredata#MediaStoreObject_20170901", "PutObject", "GetObject");
    }

    @Override
    public StreamFixture fixture(BenchmarkCell cell) {
        return new StreamFixture(
            "/" + OBJECT, "/" + OBJECT,
            response(200, JSON, "{\"ETag\":\"" + ETAG + "\",\"StorageClass\":\"TEMPORAL\"}"),
            Map.of("ETag", ETAG),
            response(404, JSON, utf8("{\"Message\":\"Object not found\"}"),
                     Map.of("X-Amzn-ErrorType", "ObjectNotFoundException")),
            response(500, JSON, utf8("{\"Message\":\"injected retry\"}"),
                     Map.of("X-Amzn-ErrorType", "InternalServerError")));
    }

    @Override
    Paths generated(boolean async, URI endpoint, AutoCloseable http) {
        PutObjectRequest put = PutObjectRequest.builder().path(OBJECT).build();
        GetObjectRequest get = GetObjectRequest.builder().path(OBJECT).build();
        if (async) {
            MediaStoreDataAsyncClient client = MediaStoreDataAsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return StreamingWorkloads.asyncPaths(
                client, () -> client.putObject(put, StreamingWorkloads.asyncBody()).thenApply(r -> r),
                exact -> client.getObject(get, StreamingWorkloads.asyncConsumer(exact)));
        }
        MediaStoreDataClient client = MediaStoreDataClient.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return StreamingWorkloads.syncPaths(client, () -> client.putObject(put, StreamingWorkloads.syncBody()),
                                            exact -> client.getObject(get, StreamingWorkloads.syncConsumer(exact)));
    }

    @Override
    Document dynamicInput() {
        return Document.of(Map.of("Path", Document.of(OBJECT)));
    }

    @Override
    void checkUploadOutput(Object output) {
        String etag = output instanceof PutObjectResponse r ? r.eTag()
                      : ((Document) output).getMember("ETag").asString();
        if (!ETAG.equals(etag)) {
            throw new IllegalStateException("decoded PutObject output does not match the fixture: ETag=" + etag);
        }
    }
}
