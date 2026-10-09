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

package software.amazon.awssdk.test.modulepath.smoke;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ResolvedModule;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;

/**
 * Builds a generated DynamoDB client over each requested HTTP Implementation mode and completes one modeled call
 * (ListTables, awsJson1_0) against a loopback fake that answers only a correctly targeted, signed JSON request.
 *
 * <p>Arguments are the modes to run (all of {@link #MODES} when none are given). Output, one record per line:
 * {@code MODE <mode> OK <transport module> <table names>} per completed call, {@code RESOLVED <module> <location>} per
 * module of the boot layer, {@code APP <module>} for this application's module, and {@code DONE} last. Any failure
 * exits non-zero.
 */
public final class SmokeApp {
    /** The HTTP Implementation modes of the Parity Manifest's {@code http-mode:} entries, in run order. */
    public static final List<String> MODES = List.of("apache-client:sync", "apache5-client:sync",
                                                     "url-connection-client:sync", "netty-nio-client:async",
                                                     "aws-crt-client:sync", "aws-crt-client:async");

    private static final String TABLE = "module-path-smoke";
    private static final String TARGET = "DynamoDB_20120810.ListTables";

    private SmokeApp() {
    }

    public static void main(String[] args) throws Exception {
        List<String> modes = args.length == 0 ? MODES : Arrays.asList(args);
        HttpServer server = fakeDynamoDb();
        server.start();
        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            for (String mode : modes) {
                run(mode, endpoint);
            }
        } finally {
            server.stop(0);
        }
        System.out.println("APP " + SmokeApp.class.getModule().getName());
        ModuleLayer layer = SmokeApp.class.getModule().getLayer();
        if (layer != null) {
            for (ResolvedModule module : layer.configuration().modules()) {
                System.out.println("RESOLVED " + module.name() + " "
                                   + module.reference().location().map(URI::toString).orElse("-"));
            }
        }
        System.out.println("DONE");
        System.out.flush();
        // Transport worker threads (Netty event loops, CRT) must not keep a finished smoke run alive.
        System.exit(0);
    }

    private static void run(String mode, URI endpoint) throws Exception {
        Map<String, Supplier<SdkSyncHttpClient>> sync = new LinkedHashMap<>();
        sync.put("apache-client:sync", ApacheHttpClient::create);
        sync.put("apache5-client:sync", Apache5HttpClient::create);
        sync.put("url-connection-client:sync", UrlConnectionHttpClient::create);
        sync.put("aws-crt-client:sync", AwsCrtHttpClient::create);
        Map<String, Supplier<SdkAsyncHttpClient>> async = new LinkedHashMap<>();
        async.put("netty-nio-client:async", NettyNioAsyncHttpClient::create);
        async.put("aws-crt-client:async", AwsCrtAsyncHttpClient::create);

        StaticCredentialsProvider credentials =
            StaticCredentialsProvider.create(AwsCredentialsIdentity.create("AKIDMODULEPATH", "module-path-secret"));
        List<String> tables;
        String transportModule;
        if (sync.containsKey(mode)) {
            try (SdkSyncHttpClient http = sync.get(mode).get();
                 DynamoDbClient client = DynamoDbClient.builder().region(Region.US_EAST_1)
                                                       .credentialsProvider(credentials)
                                                       .endpointOverride(endpoint).httpClient(http).build()) {
                transportModule = http.getClass().getModule().getName();
                tables = client.listTables(ListTablesRequest.builder().limit(5).build()).tableNames();
            }
        } else if (async.containsKey(mode)) {
            try (SdkAsyncHttpClient http = async.get(mode).get();
                 DynamoDbAsyncClient client = DynamoDbAsyncClient.builder().region(Region.US_EAST_1)
                                                                 .credentialsProvider(credentials)
                                                                 .endpointOverride(endpoint).httpClient(http).build()) {
                transportModule = http.getClass().getModule().getName();
                tables = client.listTables(ListTablesRequest.builder().limit(5).build())
                               .get(60, TimeUnit.SECONDS).tableNames();
            }
        } else {
            throw new IllegalArgumentException("Unknown mode " + mode + "; supported: " + MODES);
        }
        if (!List.of(TABLE).equals(tables)) {
            throw new IllegalStateException(mode + ": unexpected ListTables result " + tables);
        }
        System.out.println("MODE " + mode + " OK " + transportModule + " " + tables);
    }

    /** A loopback DynamoDB that answers a signed ListTables request with one table and rejects anything else. */
    private static HttpServer fakeDynamoDb() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                List<String> problems = new ArrayList<>();
                if (!"POST".equals(exchange.getRequestMethod())) {
                    problems.add("method " + exchange.getRequestMethod());
                }
                if (!TARGET.equals(exchange.getRequestHeaders().getFirst("X-Amz-Target"))) {
                    problems.add("X-Amz-Target " + exchange.getRequestHeaders().getFirst("X-Amz-Target"));
                }
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth == null || !auth.startsWith("AWS4-HMAC-SHA256 Credential=AKIDMODULEPATH/")) {
                    problems.add("Authorization " + auth);
                }
                if (!body.replace(" ", "").contains("\"Limit\":5")) {
                    problems.add("body " + body);
                }
                byte[] response = (problems.isEmpty()
                                   ? "{\"TableNames\":[\"" + TABLE + "\"]}"
                                   : "{\"__type\":\"com.amazon.coral.validate#ValidationException\",\"message\":\""
                                     + String.join("; ", problems).replace("\"", "'") + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.0");
                exchange.sendResponseHeaders(problems.isEmpty() ? 200 : 400, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        return server;
    }
}
