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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/**
 * One cold-start sample (Requirement 34.7, design D.3): the client builder's {@code build()} time and the first
 * GetItem against the {@link MockDdbServer} stub, which returns a fixed response. Run it in a fresh JVM per sample;
 * {@code scripts/benchmark.sh --main ClientColdStartBenchmark --repeat N} starts one server and N client JVMs.
 *
 * <p>Nothing runs before the measured section except argument parsing: the builder is configured, then only
 * {@code build()} is timed, then only the first call. {@link PipelineCheck}, which builds its own probe client, runs
 * after both measurements so it cannot warm the classes being measured. The JVM's uptime when {@code main} starts is
 * printed too, so JVM start cost is visible and not mixed into either number.
 *
 * <p>Covers DynamoDB only: the harness's stub is a DynamoDB server. The other Prototype_Service_Set services are
 * recorded as a gap in {@code porting-deviations.md} (task 9.18).
 */
public final class ClientColdStartBenchmark {

    private static final String USAGE = """
        Usage: ClientColdStartBenchmark --client <v2-sync|v2-async> --endpoint URL
                                        [--append-to-results-file PATH]
        One sample per JVM. Prints a COLDSTART line; with --append-to-results-file, also one CSV row.
        """;

    static final String CSV_HEADER = "client,build_ms,first_call_ms,jvm_uptime_at_main_ms,pipeline,commit,sdk_commit";

    private ClientColdStartBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        long uptimeAtMainMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String client = null;
        URI endpoint = URI.create("http://127.0.0.1:" + MockDdbServer.DEFAULT_PORT);
        Path resultsFile = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--client":
                    client = args[++i];
                    break;
                case "--endpoint":
                    endpoint = URI.create(args[++i]);
                    break;
                case "--append-to-results-file":
                    resultsFile = Path.of(args[++i]);
                    break;
                case "--help":
                    System.out.print(USAGE);
                    return;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.print(USAGE);
                    System.exit(2);
            }
        }
        if (!"v2-sync".equals(client) && !"v2-async".equals(client)) {
            System.err.println("--client must be v2-sync or v2-async");
            System.err.print(USAGE);
            System.exit(2);
        }

        long buildNanos;
        long firstCallNanos;
        if ("v2-sync".equals(client)) {
            DynamoDbClientBuilder builder =
                DynamoDbClient.builder()
                              .endpointOverride(endpoint).region(Region.US_EAST_1)
                              .credentialsProvider(Workloads.v2Creds())
                              .httpClientBuilder(software.amazon.awssdk.http.apache5.Apache5HttpClient.builder())
                              .overrideConfiguration(Workloads.v2Override(false, null, null));
            long t0 = System.nanoTime();
            DynamoDbClient ddb = builder.build();
            long t1 = System.nanoTime();
            ddb.getItem(Workloads.v2GetReq());
            long t2 = System.nanoTime();
            buildNanos = t1 - t0;
            firstCallNanos = t2 - t1;
            ddb.close();
        } else {
            DynamoDbAsyncClientBuilder builder =
                DynamoDbAsyncClient.builder()
                                   .endpointOverride(endpoint).region(Region.US_EAST_1)
                                   .credentialsProvider(Workloads.v2Creds())
                                   .httpClientBuilder(AwsCrtAsyncHttpClient.builder())
                                   .overrideConfiguration(Workloads.v2Override(false, null, null));
            long t0 = System.nanoTime();
            DynamoDbAsyncClient ddb = builder.build();
            long t1 = System.nanoTime();
            ddb.getItem(Workloads.v2GetReq()).join();
            long t2 = System.nanoTime();
            buildNanos = t1 - t0;
            firstCallNanos = t2 - t1;
            ddb.close();
        }

        // After the measurements: the probe builds its own client and would otherwise warm what is measured.
        PipelineCheck.report(endpoint, "v2-sync".equals(client) ? PipelineCheck.ClientKind.SYNC
                                                                : PipelineCheck.ClientKind.ASYNC);
        BuildProvenance provenance = BuildProvenance.get();
        String pipeline = PipelineCheck.detectedName();
        System.out.printf(Locale.US, "COLDSTART client=%s build_ms=%.2f first_call_ms=%.2f jvm_uptime_at_main_ms=%d"
                                     + " pipeline=%s build=%s%n",
                          client, buildNanos / 1e6, firstCallNanos / 1e6, uptimeAtMainMs, pipeline,
                          provenance.summary());
        if (resultsFile != null) {
            String row = String.format(Locale.US, "%s,%.3f,%.3f,%d,%s,%s,%s", client, buildNanos / 1e6,
                                       firstCallNanos / 1e6, uptimeAtMainMs, pipeline, provenance.shortCommit(),
                                       provenance.shortSdkCommit());
            appendRow(resultsFile, row);
        }
    }

    private static void appendRow(Path file, String row) throws IOException {
        boolean needsHeader = !Files.exists(file) || Files.size(file) == 0;
        String content = needsHeader ? CSV_HEADER + "\n" + row + "\n" : row + "\n";
        Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
