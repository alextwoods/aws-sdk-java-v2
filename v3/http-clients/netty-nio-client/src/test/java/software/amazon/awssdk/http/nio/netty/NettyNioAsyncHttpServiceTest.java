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

package software.amazon.awssdk.http.nio.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.HttpImplementationDiscovery;
import software.amazon.awssdk.http.provider.InstalledHttpImplementations;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;

class NettyNioAsyncHttpServiceTest {
    @Test
    void isDiscoveredAsTheAsyncDefault() {
        InstalledHttpImplementations installed = HttpImplementationDiscovery.discover(getClass().getClassLoader());
        assertTrue(installed.async().stream().anyMatch(NettyNioAsyncHttpService.class::isInstance));
        assertInstanceOf(NettyNioAsyncHttpService.class, installed.defaultAsync());
        NettyNioAsyncHttpService service = new NettyNioAsyncHttpService();
        assertEquals("netty-nio-client", service.implementationName());
        assertEquals(300, service.priority());
        assertEquals(NettyNioAsyncHttpClient.OPTIONS, service.capabilities().options());
    }

    @Test
    void resolvesWithConfiguredOptionsAndReleasesOnClose() {
        HttpClientConfiguration configuration = HttpClientConfiguration.builder()
            .put(HttpClientOption.READ_TIMEOUT, Duration.ofSeconds(3))
            .put(HttpClientOption.MAX_CONNECTIONS, 4)
            .build();
        ResolvedHttpClient<SdkAsyncHttpClient> resolved =
            HttpClientResolver.async().implementation(new NettyNioAsyncHttpService()).configuration(configuration)
                              .resolve();
        NettyNioAsyncHttpClient client = assertInstanceOf(NettyNioAsyncHttpClient.class, resolved.client());
        resolved.close();
        assertTrue(client.eventLoopGroup().eventLoopGroup().isShuttingDown());
    }

    @Test
    void unsupportedOptionFailsBeforeAnyResourceIsCreated() {
        HttpClientConfiguration proxy = HttpClientConfiguration.builder()
            .put(HttpClientOption.PROXY, "http://proxy:8080").build();
        SdkClientException e = assertThrows(SdkClientException.class,
                                            () -> HttpClientResolver.async()
                                                                    .implementation(new NettyNioAsyncHttpService())
                                                                    .configuration(proxy).resolve());
        assertTrue(e.getMessage().contains("proxyConfiguration"), e.getMessage());
    }

    @Test
    void failedCreateRollsBackTheEventLoopGroup() {
        HttpClientConfiguration bad = HttpClientConfiguration.builder()
            .put(HttpClientOption.TLS_TRUST_MANAGERS, (TlsTrustManagersProvider) () -> {
                throw new IllegalStateException("no trust store");
            }).build();
        assertThrows(RuntimeException.class, () -> HttpClientResolver.async()
                                                                     .implementation(new NettyNioAsyncHttpService())
                                                                     .configuration(bad).resolve());
    }
}
