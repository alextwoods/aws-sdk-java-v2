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

package software.amazon.awssdk.benchmark.reference.v2;

import java.util.Set;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

/**
 * The V2 transports of the cells' transport ids, each built explicitly with a pool sized to the cell's concurrency,
 * exactly as the harness's V3 {@code Transport} builds its counterpart. V2's transport classes carry the same names
 * as V3's, so the validity rules' transport check compares the same class names for both arms. V2 has no Apache 5
 * async client, so {@code apache5-async} cells are outside this reference.
 *
 * <p>Shared: test/sdk-benchmarks-racecar-reference compiles this source against the Racecar jars (Racecar keeps
 * V2's client API), hence public.
 */
public final class V2Transports {

    public static final Set<String> IDS = Set.of("apache5-sync", "apache4-sync", "urlconnection-sync", "crt-sync",
                                          "netty-async", "crt-async");

    private V2Transports() {
    }

    public static boolean isAsync(String id) {
        return id.endsWith("-async");
    }

    public static SdkHttpClient sync(String id, int concurrency) {
        return switch (id) {
            case "apache5-sync" -> Apache5HttpClient.builder().maxConnections(concurrency).build();
            case "apache4-sync" -> ApacheHttpClient.builder().maxConnections(concurrency).build();
            case "urlconnection-sync" -> {
                // The JDK keep-alive cache is sized by http.maxConnections, as for the V3 arm.
                System.setProperty("http.maxConnections", Integer.toString(concurrency));
                yield UrlConnectionHttpClient.builder().build();
            }
            case "crt-sync" -> AwsCrtHttpClient.builder().maxConcurrency(concurrency).build();
            default -> throw new IllegalArgumentException("no V2 sync transport " + id);
        };
    }

    public static SdkAsyncHttpClient async(String id, int concurrency) {
        return switch (id) {
            case "netty-async" -> NettyNioAsyncHttpClient.builder().maxConcurrency(concurrency).build();
            case "crt-async" -> AwsCrtAsyncHttpClient.builder().maxConcurrency(concurrency).build();
            default -> throw new IllegalArgumentException("no V2 async transport " + id);
        };
    }
}
