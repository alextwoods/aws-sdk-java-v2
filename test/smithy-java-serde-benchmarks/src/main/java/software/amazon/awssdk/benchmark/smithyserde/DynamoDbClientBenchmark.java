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

package software.amazon.awssdk.benchmark.smithyserde;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

/**
 * Whole-client cost of the four standard DynamoDB calls on the stock v2 pipeline, with v2's serde
 * ({@code serde=v2}) or smithy-java's ({@code serde=smithy}), against an in-process canned HTTP client.
 *
 * <p>Everything except serde is the same in both arms: one jar, the same client configuration, the same
 * pipeline (endpoint rules, SigV4 signing, retries, interceptors, CRC32 validation, metrics). JMH runs each
 * {@code serde} value in its own forks, and the client reads {@code aws.smithyJavaSerde} once at
 * construction, so each fork's JIT only ever sees one serde.
 *
 * <p>There is no network, so the numbers are client CPU per call, not latency; they are what the
 * end-to-end harness in {@code test/standalone-e2e-benchmarks} measures minus the socket and the mock
 * server, which add the same cost to both arms.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(3)
public class DynamoDbClientBenchmark {

    @Param({"v2", "smithy", "smithy-native"})
    public String serde;

    private DynamoDbClient client;
    private GetItemRequest smallGet;
    private PutItemRequest smallPut;
    private BatchGetItemRequest batchGet;
    private BatchWriteItemRequest batchPut;

    @Setup
    public void setup() {
        SerdeSelection.select(serde);
        client = DynamoDbClient.builder()
                               .endpointOverride(URI.create("http://localhost:19080"))
                               .region(Region.US_EAST_1)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                               .overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                               .httpClient(new CannedDynamoDbHttpClient())
                               .build();
        smallGet = DynamoDbRequests.smallGet();
        smallPut = DynamoDbRequests.smallPut();
        batchGet = DynamoDbRequests.batchGet();
        batchPut = DynamoDbRequests.batchPut();
        SerdeSelection.verify(serde, client, client.getItem(smallGet));
    }

    @TearDown
    public void tearDown() {
        client.close();
    }

    @Benchmark
    public GetItemResponse smallGet() {
        return client.getItem(smallGet);
    }

    @Benchmark
    public PutItemResponse smallPut() {
        return client.putItem(smallPut);
    }

    @Benchmark
    public BatchGetItemResponse batchGet() {
        return client.batchGetItem(batchGet);
    }

    @Benchmark
    public BatchWriteItemResponse batchPut() {
        return client.batchWriteItem(batchPut);
    }
}
