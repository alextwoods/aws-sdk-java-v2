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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.Invoker;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The awsJson1.1 fixture: Kinesis {@code PutRecord}. The record's {@code Data} blob (base64 on the wire) is the pad;
 * the success response echoes it as the {@code SequenceNumber}. Error cells get the modeled
 * {@code ResourceNotFoundException}; retry cells the modeled, retryable {@code InternalFailureException} (HTTP 500).
 */
final class KinesisWorkloads extends FiniteWorkloads.Provider {
    static final String STREAM = KinesisPutRecordFixture.STREAM;
    static final String KEY = KinesisPutRecordFixture.KEY;
    static final String CONTENT_TYPE = KinesisPutRecordFixture.CONTENT_TYPE;

    KinesisWorkloads() {
        super("kinesis-putrecord", BenchProtocol.AWS_JSON_1_1, "kinesis", "com.amazonaws.kinesis#Kinesis_20131202",
              "PutRecord", "com.amazonaws.kinesis#ResourceNotFoundException");
    }

    @Override
    public PaddedFixture fixture(BenchmarkCell cell) {
        return KinesisPutRecordFixture.create(cell.payload());
    }

    @Override
    Invoker generated(boolean async, PaddedFixture fixture, URI endpoint, AutoCloseable http) {
        PutRecordRequest request = PutRecordRequest.builder().streamName(STREAM)
                                                   .data(SdkBytes.fromUtf8String(fixture.pad())).partitionKey(KEY)
                                                   .build();
        Predicate<Throwable> error = t -> t instanceof ResourceNotFoundException;
        Function<Object, String> echo = r -> ((PutRecordResponse) r).sequenceNumber();
        if (async) {
            KinesisAsyncClient client = KinesisAsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return Invoker.async(client, () -> client.putRecord(request), error, echo);
        }
        KinesisClient client = KinesisClient.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return Invoker.sync(client, () -> client.putRecord(request), error, echo);
    }

    @Override
    Document dynamicInput(PaddedFixture fixture) {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("StreamName", Document.of(STREAM));
        input.put("Data", Document.of(ByteBuffer.wrap(fixture.pad().getBytes(StandardCharsets.UTF_8))));
        input.put("PartitionKey", Document.of(KEY));
        return Document.of(input);
    }

    @Override
    String dynamicEcho(Document output) {
        return output.getMember("SequenceNumber").asString();
    }
}
