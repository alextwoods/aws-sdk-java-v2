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

import java.util.function.IntFunction;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache5.Apache5AsyncHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

/**
 * An explicitly constructed V3 transport. The harness never resolves a transport from the classpath (the Racecar
 * auto-selection ambiguity): every cell names one, the client process builds exactly that class with a pool sized to
 * the cell's concurrency, checks the built instance's class and self-reported name, and records both.
 */
public enum Transport {
    APACHE5_SYNC("apache5-sync", HttpTransportMode.SYNC, Apache5HttpClient.class, Apache5HttpClient.CLIENT_NAME,
                 n -> Apache5HttpClient.builder().maxConnections(n).build()),
    APACHE4_SYNC("apache4-sync", HttpTransportMode.SYNC, ApacheHttpClient.class, ApacheHttpClient.CLIENT_NAME,
                 n -> ApacheHttpClient.builder().maxConnections(n).build()),
    /** The JDK keep-alive cache is sized by {@code http.maxConnections}, which {@link #create} sets. */
    URLCONNECTION_SYNC("urlconnection-sync", HttpTransportMode.SYNC, UrlConnectionHttpClient.class,
                       UrlConnectionHttpClient.CLIENT_NAME, n -> UrlConnectionHttpClient.builder().build()),
    CRT_SYNC("crt-sync", HttpTransportMode.SYNC, AwsCrtHttpClient.class, AwsCrtHttpClient.CLIENT_NAME,
             n -> AwsCrtHttpClient.builder().maxConcurrency(n).build()),
    NETTY_ASYNC("netty-async", HttpTransportMode.ASYNC, NettyNioAsyncHttpClient.class,
                NettyNioAsyncHttpClient.CLIENT_NAME, n -> NettyNioAsyncHttpClient.builder().maxConcurrency(n).build()),
    CRT_ASYNC("crt-async", HttpTransportMode.ASYNC, AwsCrtAsyncHttpClient.class, AwsCrtAsyncHttpClient.CLIENT_NAME,
              n -> AwsCrtAsyncHttpClient.builder().maxConcurrency(n).build()),
    APACHE5_ASYNC("apache5-async", HttpTransportMode.ASYNC, Apache5AsyncHttpClient.class,
                  Apache5AsyncHttpClient.CLIENT_NAME, n -> Apache5AsyncHttpClient.builder().maxConnections(n).build());

    private final String id;
    private final HttpTransportMode mode;
    private final Class<?> implementation;
    private final String clientName;
    private final IntFunction<AutoCloseable> factory;

    Transport(String id, HttpTransportMode mode, Class<?> implementation, String clientName,
              IntFunction<AutoCloseable> factory) {
        this.id = id;
        this.mode = mode;
        this.implementation = implementation;
        this.clientName = clientName;
        this.factory = factory;
    }

    public String id() {
        return id;
    }

    public HttpTransportMode mode() {
        return mode;
    }

    public Class<?> implementation() {
        return implementation;
    }

    public String expectedClientName() {
        return clientName;
    }

    /**
     * Builds this transport with a pool of {@code concurrency} connections and verifies that the built instance is
     * the named implementation; a mismatch throws rather than measuring the wrong transport.
     */
    public AutoCloseable create(int concurrency) {
        if (this == URLCONNECTION_SYNC) {
            System.setProperty("http.maxConnections", Integer.toString(concurrency));
        }
        AutoCloseable built = factory.apply(concurrency);
        String name = built instanceof SdkSyncHttpClient s ? s.clientName()
                      : built instanceof SdkAsyncHttpClient a ? a.clientName() : null;
        if (built.getClass() != implementation || !clientName.equals(name)) {
            throw new IllegalStateException("transport " + id + " built " + built.getClass().getName() + " named "
                                            + name + "; expected " + implementation.getName() + " named " + clientName);
        }
        return built;
    }

    public static Transport fromId(String id) {
        for (Transport t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown transport: " + id);
    }
}
