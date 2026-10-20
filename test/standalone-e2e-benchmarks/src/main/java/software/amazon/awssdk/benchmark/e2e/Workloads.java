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

package software.amazon.awssdk.benchmark.e2e;

import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * One workload per SDK variant: a single client pointed at the mock server, prebuilt request
 * objects, and one method per scenario.
 *
 * <p>Retry parity: every client is explicitly configured for standard, token-bucket retries with a
 * maximum of 3 attempts ({@code RetryMode.STANDARD}), so each arm pays the same per-call retry
 * accounting while none actually retries (the server always returns 200).
 *
 * <p>Ported from racecar ({@code feature/poc/racecar}) as benchmark tooling only. The V1, smithy-java
 * and bridged arms were removed: this branch compares V2 builds (Baseline_Build, racecar, feature,
 * candidate) built from git refs, so every arm runs the same {@code v2-sync}/{@code v2-async} code.
 */
interface Workloads {

    String ACCESS_KEY = "benchmarkAccessKey";
    String SECRET_KEY = "benchmarkSecretKey";

    interface Workload extends AutoCloseable {
        void smallGet() throws Exception;

        void smallPut() throws Exception;

        void batchGet() throws Exception;

        void batchPut() throws Exception;

        /**
         * Structure-heavy read: a DescribeTable response is 46 nested structures and no unions, where the item-based
         * scenarios are the reverse. Both V2 arms implement it.
         */
        default void describeTable() throws Exception {
            throw new UnsupportedOperationException("describe-table is not implemented for this client");
        }

        /**
         * Human-readable transport identity for the run header and the {@code transport} results
         * column. Reported because it has been a silent variable: with several {@code SdkHttpService}
         * implementations on the classpath, V2's default resolution picks by an internal priority
         * table, and this benchmark's classpath resolved to Apache5 while its README claimed Apache
         * 4.x. Every client pins its transport explicitly and records which one it used, so a results
         * file can never leave the question open.
         */
        String transport();

        /**
         * Whether {@code *Async} methods are usable. Async clients can hold many operations in
         * flight from a single thread, which is the workload shape they exist for; a blocking
         * {@code join()} per call measures something else.
         */
        default boolean supportsAsync() {
            return false;
        }

        default CompletableFuture<?> smallGetAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> smallPutAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> batchGetAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> batchPutAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> describeTableAsync() {
            throw new UnsupportedOperationException("describe-table is not implemented for this client");
        }

        default void resetMetrics() {
        }

        default void printMetrics(PrintStream out) {
        }
    }

    /**
     * @param concurrency operations the driver will keep in flight. Every client's connection pool is
     *                    sized to exactly this, so no client is measured waiting on its own pool and
     *                    none is given a larger pool than another.
     */
    static Workload create(String client, URI endpoint, boolean metrics, int concurrency) {
        // Recorded in the transport column so a results file states which stack AND whether it was
        // encrypted, rather than leaving it to be inferred from the run script.
        String tlsSuffix = "https".equalsIgnoreCase(endpoint.getScheme()) ? "+tls" : "";
        switch (client) {
            case "v2-sync":
                return v2Sync(endpoint, metrics,
                              software.amazon.awssdk.http.apache5.Apache5HttpClient.builder()
                                                                                   .maxConnections(concurrency),
                              "apache5" + tlsSuffix);
            case "v2-async":
                return v2Async(endpoint, metrics,
                               AwsCrtAsyncHttpClient.builder()
                                                    .maxConcurrency(concurrency)
                                                    .buildWithDefaults(crtTlsDefaults(endpoint)),
                               "crt" + tlsSuffix);
            default:
                throw new IllegalArgumentException("unknown client: " + client);
        }
    }

    /**
     * Trust configuration for the CRT client under {@code --tls}.
     *
     * <p>The sync client does TLS through the JDK, so it picks the benchmark's throwaway certificate up from
     * the {@code javax.net.ssl.trustStore} the launcher sets, and verifies a real chain against a real hostname. CRT
     * does TLS natively (aws-lc) and ignores that truststore, and the SDK's CRT client exposes no hook for a custom
     * CA — only {@link SdkHttpConfigurationOption#TRUST_ALL_CERTIFICATES}, which maps to CRT's
     * {@code withVerifyPeer(false)}. So CRT verifies nothing while the others verify fully.
     *
     * <p>That asymmetry cannot bias what is being measured: certificate verification happens once per handshake, and
     * these runs reuse pooled connections across 200,000 operations, so any per-handshake cost is divided by five
     * orders of magnitude. It would matter for a handshake-per-request workload, which this is not.
     */
    private static AttributeMap crtTlsDefaults(URI endpoint) {
        if (!"https".equalsIgnoreCase(endpoint.getScheme())) {
            return AttributeMap.empty();
        }
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                           .build();
    }

    // ==================== V2 shared ====================

    private static StaticCredentialsProvider v2Creds() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
    }

    private static ClientOverrideConfiguration v2Override(boolean metrics, MetricsSupport.V2Publisher publisher) {
        ClientOverrideConfiguration.Builder b = ClientOverrideConfiguration.builder()
                                                                           .retryStrategy(RetryMode.STANDARD);
        if (metrics) {
            b.addMetricPublisher(publisher);
        }
        return b.build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.GetItemRequest v2GetReq() {
        return software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .key(Map.of("pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue
                .fromS(BenchmarkItems.SMALL_KEY)))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.PutItemRequest v2PutReq() {
        return software.amazon.awssdk.services.dynamodb.model.PutItemRequest.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .item(BenchmarkItems.toV2(BenchmarkItems.smallItem()))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest v2BatchGetReq() {
        List<Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> keys = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue
                .fromS(BenchmarkItems.batchKey(i))));
        }
        return software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                 software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes.builder()
                                                                                                 .keys(keys).build()))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest v2BatchPutReq() {
        List<software.amazon.awssdk.services.dynamodb.model.WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            writes.add(software.amazon.awssdk.services.dynamodb.model.WriteRequest.builder()
                .putRequest(software.amazon.awssdk.services.dynamodb.model.PutRequest.builder()
                                .item(BenchmarkItems.toV2(BenchmarkItems.mediumItem(i))).build())
                .build());
        }
        return software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME, writes))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest v2DescribeReq() {
        return software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .build();
    }

    // ==================== V2 sync (Apache5) ====================
    //
    // Apache5 is the transport V2 sync is standardizing on, so it is the one worth measuring. It is
    // pinned rather than resolved from the classpath: `dynamodb` pulls in apache5-client and
    // netty-nio-client transitively, so several SdkHttpService implementations can be present at
    // once and V2 picks by an internal priority table (ClasspathSdkHttpServiceProvider) instead of
    // failing. That made the transport an invisible variable — this module's README claimed Apache
    // 4.x for `v2-sync` while Apache5 was in fact what ran. Naming it here means the priority table,
    // and anything new that lands on the classpath, cannot change what is under test.

    private static Workload v2Sync(URI endpoint,
                                   boolean metrics,
                                   software.amazon.awssdk.http.SdkHttpClient.Builder<?> httpClientBuilder,
                                   String transportName) {
        // Before anything is measured: record which request pipeline this SDK build runs (legacy or the
        // schema path), so a results row can never be attributed to the wrong pipeline.
        PipelineCheck.report(endpoint, PipelineCheck.ClientKind.SYNC);
        MetricsSupport.V2Publisher publisher = new MetricsSupport.V2Publisher();
        var ddb = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder()
            .endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(v2Creds())
            .httpClientBuilder(httpClientBuilder)
            .overrideConfiguration(v2Override(metrics, publisher))
            .build();

        var getReq = v2GetReq();
        var putReq = v2PutReq();
        var batchGetReq = v2BatchGetReq();
        var batchPutReq = v2BatchPutReq();
        var describeReq = v2DescribeReq();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq);
            }

            public void smallPut() {
                ddb.putItem(putReq);
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq);
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq);
            }

            public void describeTable() {
                ddb.describeTable(describeReq);
            }

            public String transport() {
                return transportName;
            }

            public void resetMetrics() {
                publisher.reset();
            }

            public void printMetrics(PrintStream out) {
                publisher.print(out);
            }

            public void close() {
                ddb.close();
            }
        };
    }

    // ==================== V2 async (CRT) ====================
    //
    // CRT is the transport V2 async is standardizing on, so it is the one worth measuring, and it is
    // pinned for the same reason as the sync side. Note that V2's *current* default resolution would
    // pick Netty (priority 1 in ClasspathSdkHttpServiceProvider's async table), so this benchmark is
    // deliberately measuring the intended long-term default rather than today's fallback.

    private static Workload v2Async(URI endpoint,
                                    boolean metrics,
                                    software.amazon.awssdk.http.async.SdkAsyncHttpClient httpClient,
                                    String transportName) {
        PipelineCheck.report(endpoint, PipelineCheck.ClientKind.ASYNC);
        MetricsSupport.V2Publisher publisher = new MetricsSupport.V2Publisher();
        var ddb = software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.builder()
            .endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(v2Creds())
            .httpClient(httpClient)
            .overrideConfiguration(v2Override(metrics, publisher))
            .build();

        var getReq = v2GetReq();
        var putReq = v2PutReq();
        var batchGetReq = v2BatchGetReq();
        var batchPutReq = v2BatchPutReq();
        var describeReq = v2DescribeReq();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq).join();
            }

            public void smallPut() {
                ddb.putItem(putReq).join();
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq).join();
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq).join();
            }

            public void describeTable() {
                ddb.describeTable(describeReq).join();
            }

            public boolean supportsAsync() {
                return true;
            }

            public CompletableFuture<?> smallGetAsync() {
                return ddb.getItem(getReq);
            }

            public CompletableFuture<?> smallPutAsync() {
                return ddb.putItem(putReq);
            }

            public CompletableFuture<?> batchGetAsync() {
                return ddb.batchGetItem(batchGetReq);
            }

            public CompletableFuture<?> batchPutAsync() {
                return ddb.batchWriteItem(batchPutReq);
            }

            public CompletableFuture<?> describeTableAsync() {
                return ddb.describeTable(describeReq);
            }

            public String transport() {
                return transportName;
            }

            public void resetMetrics() {
                publisher.reset();
            }

            public void printMetrics(PrintStream out) {
                publisher.print(out);
            }

            public void close() {
                ddb.close();
            }
        };
    }

}
