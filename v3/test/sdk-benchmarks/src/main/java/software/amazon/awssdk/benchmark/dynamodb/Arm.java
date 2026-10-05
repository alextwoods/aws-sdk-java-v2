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

package software.amazon.awssdk.benchmark.dynamodb;

import java.util.function.IntFunction;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache5.Apache5AsyncHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

/**
 * A V3 benchmark arm: one explicitly constructed native transport. Nothing is resolved from the classpath, so the
 * transport is never an invisible variable (racecar's pinning lesson), and {@link TransportCheck} asserts before
 * timing that the constructed client really is the one named here.
 *
 * <p>Every arm has the same scope, {@link #SCOPE}: V3 has no client/call engine yet, so a "call" is awsJson1.0
 * {@code OperationProtocol.serializeRequest} → transport exchange → {@code deserializeResponse}. Every connection pool
 * is sized to exactly the concurrency, as in racecar.
 */
enum Arm {
    V3_SYNC_APACHE5("v3-sync-apache5", HttpTransportMode.SYNC, Apache5HttpClient.CLIENT_NAME,
                    Apache5HttpClient.class, n -> Apache5HttpClient.builder().maxConnections(n).build()),
    V3_SYNC_APACHE4("v3-sync-apache4", HttpTransportMode.SYNC, ApacheHttpClient.CLIENT_NAME,
                    ApacheHttpClient.class, n -> ApacheHttpClient.builder().maxConnections(n).build()),
    /** Pool size is the JDK's {@code http.maxConnections} keep-alive cache, set to the concurrency by the runner. */
    V3_SYNC_URLCONNECTION("v3-sync-urlconnection", HttpTransportMode.SYNC, UrlConnectionHttpClient.CLIENT_NAME,
                          UrlConnectionHttpClient.class, n -> UrlConnectionHttpClient.builder().build()),
    V3_SYNC_CRT("v3-sync-crt", HttpTransportMode.SYNC, AwsCrtHttpClient.CLIENT_NAME,
                AwsCrtHttpClient.class, n -> AwsCrtHttpClient.builder().maxConcurrency(n).build()),
    V3_ASYNC_NETTY("v3-async-netty", HttpTransportMode.ASYNC, NettyNioAsyncHttpClient.CLIENT_NAME,
                   NettyNioAsyncHttpClient.class, n -> NettyNioAsyncHttpClient.builder().maxConcurrency(n).build()),
    V3_ASYNC_CRT("v3-async-crt", HttpTransportMode.ASYNC, AwsCrtAsyncHttpClient.CLIENT_NAME,
                 AwsCrtAsyncHttpClient.class, n -> AwsCrtAsyncHttpClient.builder().maxConcurrency(n).build()),
    /** Apache 5 async is not registered as an {@code SdkAsyncHttpService}; it is constructed directly. */
    V3_ASYNC_APACHE5("v3-async-apache5", HttpTransportMode.ASYNC, Apache5AsyncHttpClient.CLIENT_NAME,
                     Apache5AsyncHttpClient.class, n -> Apache5AsyncHttpClient.builder().maxConnections(n).build());

    /** What a V3 "call" is in every arm today; printed in every header and results row. */
    static final String SCOPE = "serde+transport";
    static final String SCOPE_DETAIL = "awsJson1.0 OperationProtocol.serializeRequest -> transport exchange ->"
                                       + " deserializeResponse; no client engine, signer, endpoint resolution,"
                                       + " retries or interceptors (not yet in V3)";

    final String cliName;
    final HttpTransportMode mode;
    final String expectedClientName;
    final Class<?> expectedClass;
    private final IntFunction<AutoCloseable> factory;

    Arm(String cliName, HttpTransportMode mode, String expectedClientName, Class<?> expectedClass,
        IntFunction<AutoCloseable> factory) {
        this.cliName = cliName;
        this.mode = mode;
        this.expectedClientName = expectedClientName;
        this.expectedClass = expectedClass;
        this.factory = factory;
    }

    /** Short transport label for the {@code transport} column, e.g. {@code apache5}. */
    String transportLabel() {
        return cliName.substring(cliName.lastIndexOf('-') + 1);
    }

    /** The scope-honest arm label, e.g. {@code v3-serde+apache5} or {@code v3-serde+async-netty}. */
    String honestLabel() {
        return "v3-serde+" + (mode == HttpTransportMode.ASYNC ? "async-" : "") + transportLabel();
    }

    AutoCloseable newTransport(int concurrency) {
        if (this == V3_SYNC_URLCONNECTION) {
            // HttpURLConnection keeps idle connections in a JDK-wide cache sized by this property (default 5).
            System.setProperty("http.maxConnections", Integer.toString(concurrency));
        }
        return factory.apply(concurrency);
    }

    static Arm fromCli(String name) {
        for (Arm a : values()) {
            if (a.cliName.equals(name)) {
                return a;
            }
        }
        throw new IllegalArgumentException("unknown arm: " + name);
    }
}
