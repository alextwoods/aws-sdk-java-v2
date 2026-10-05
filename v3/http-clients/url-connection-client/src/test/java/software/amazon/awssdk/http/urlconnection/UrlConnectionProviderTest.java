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

package software.amazon.awssdk.http.urlconnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.HttpImplementationDiscovery;
import software.amazon.awssdk.http.provider.HttpImplementationSelectionException;
import software.amazon.awssdk.http.provider.InstalledHttpImplementations;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;
import software.amazon.awssdk.http.provider.SdkHttpService;

/** Capabilities, fail-fast options, provider discovery, priority and construction rollback. */
class UrlConnectionProviderTest {
    private static final UrlConnectionSdkHttpService SERVICE = new UrlConnectionSdkHttpService();

    static Stream<HttpClientOption> unsupportedOptions() {
        return EnumSet.complementOf(EnumSet.copyOf(UrlConnectionHttpClient.SUPPORTED_OPTIONS)).stream();
    }

    @Test
    void capabilitiesAreSyncHttp11WithTheDocumentedOptions() {
        HttpClientCapabilities c = UrlConnectionHttpClient.create().capabilities();
        assertEquals(HttpTransportMode.SYNC, c.mode());
        assertEquals(Set.of(Protocol.HTTP1_1), c.protocols());
        assertFalse(c.supports(Protocol.HTTP2));
        assertEquals(EnumSet.of(HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.READ_TIMEOUT,
                                HttpClientOption.TLS_KEY_MANAGERS, HttpClientOption.TLS_TRUST_MANAGERS,
                                HttpClientOption.TRUST_ALL_CERTIFICATES, HttpClientOption.PROXY,
                                HttpClientOption.PROXY_CREDENTIALS, HttpClientOption.NON_PROXY_HOSTS),
                     c.options());
        assertEquals(64 * 1024, c.maxBufferedBytesPerExchange());
        assertEquals(Duration.ofSeconds(2), c.cancellationTimeout());
        assertEquals(c, SERVICE.capabilities());
        assertEquals("UrlConnection", UrlConnectionHttpClient.create().clientName());
    }

    @Test
    void theFailFastListCoversPoolHttp2AndOtherUnsupportedOptions() {
        Set<HttpClientOption> rejected = EnumSet.copyOf(unsupportedOptions().toList());
        assertEquals(EnumSet.of(HttpClientOption.CONNECTION_ACQUISITION_TIMEOUT, HttpClientOption.WRITE_TIMEOUT,
                                HttpClientOption.TLS_NEGOTIATION_TIMEOUT, HttpClientOption.PROXY_FROM_ENVIRONMENT,
                                HttpClientOption.MAX_CONNECTIONS, HttpClientOption.MAX_PENDING_CONNECTION_ACQUIRES,
                                HttpClientOption.CONNECTION_TIME_TO_LIVE, HttpClientOption.CONNECTION_MAX_IDLE_TIME,
                                HttpClientOption.REAP_IDLE_CONNECTIONS, HttpClientOption.TCP_KEEP_ALIVE,
                                HttpClientOption.EXPECT_CONTINUE, HttpClientOption.PROTOCOL_VERSION,
                                HttpClientOption.HTTP2_MAX_STREAMS, HttpClientOption.HTTP2_INITIAL_WINDOW_SIZE),
                     rejected);
    }

    @ParameterizedTest
    @MethodSource("unsupportedOptions")
    void resolverRejectsEachUnsupportedOptionWithItsV2Name(HttpClientOption option) {
        HttpClientConfiguration configuration = HttpClientConfiguration.builder().put(option, "any").build();
        HttpImplementationSelectionException e = assertThrows(
            HttpImplementationSelectionException.class,
            () -> HttpClientResolver.sync().implementation(SERVICE).configuration(configuration).resolve());
        assertEquals(HttpImplementationSelectionException.Reason.UNSUPPORTED_OPTION, e.reason());
        assertEquals(List.of("url-connection-client"), e.implementations());
        assertEquals(List.of(option), e.unsupportedOptions());
        assertTrue(e.getMessage().contains(option.configurationName()), e.getMessage());
        assertTrue(e.getMessage().contains("url-connection-client"), e.getMessage());
    }

    @Test
    void supportedOptionsResolveAndConfigureTheClient() {
        HttpClientConfiguration configuration = HttpClientConfiguration
            .builder()
            .put(HttpClientOption.CONNECTION_TIMEOUT, Duration.ofSeconds(1))
            .put(HttpClientOption.READ_TIMEOUT, Duration.ofSeconds(3))
            .put(HttpClientOption.TRUST_ALL_CERTIFICATES, Boolean.TRUE)
            .put(HttpClientOption.PROXY, ProxyConfiguration.builder().endpoint(URI.create("http://proxy:8080"))
                                                           .username("u").password("p")
                                                           .addNonProxyHost("localhost").build())
            .put(HttpClientOption.PROXY_CREDENTIALS, Boolean.TRUE)
            .put(HttpClientOption.NON_PROXY_HOSTS, Boolean.TRUE)
            .build();
        try (ResolvedHttpClient<SdkSyncHttpClient> resolved =
                 HttpClientResolver.sync().implementation(SERVICE).configuration(configuration).resolve()) {
            assertInstanceOf(UrlConnectionHttpClient.class, resolved.client());
            assertEquals("url-connection-client", resolved.implementationName());
        }
    }

    @Test
    void perExchangeOptionsTheTransportCannotHonourFailPrepare() {
        UrlConnectionHttpClient client = UrlConnectionHttpClient.create();
        IllegalArgumentException write = assertThrows(IllegalArgumentException.class, () -> client.prepare(
            TestRequests.get(1, "/").build(),
            HttpExecutionOptions.builder().writeTimeout(Duration.ofSeconds(1)).build()));
        assertTrue(write.getMessage().contains("writeTimeout"));
        IllegalArgumentException acquire = assertThrows(IllegalArgumentException.class, () -> client.prepare(
            TestRequests.get(1, "/").build(),
            HttpExecutionOptions.builder().connectionAcquisitionTimeout(Duration.ofSeconds(1)).build()));
        assertTrue(acquire.getMessage().contains("connectionAcquisitionTimeout"));
    }

    @Test
    void discoveryFindsTheRegisteredServiceWithItsPriority() {
        InstalledHttpImplementations installed =
            HttpImplementationDiscovery.discover(UrlConnectionProviderTest.class.getClassLoader());
        List<SdkHttpService> sync = installed.sync();
        assertEquals(1, sync.stream().filter(s -> s instanceof UrlConnectionSdkHttpService).count());
        assertEquals(100, UrlConnectionSdkHttpService.PRIORITY);
        assertEquals(100, sync.stream().filter(s -> s instanceof UrlConnectionSdkHttpService).findFirst()
                              .orElseThrow().priority());
        assertEquals("url-connection-client", installed.defaultSync().implementationName());
        assertTrue(installed.async().isEmpty());
        try (ResolvedHttpClient<SdkSyncHttpClient> resolved = HttpClientResolver.sync().resolve()) {
            assertInstanceOf(UrlConnectionHttpClient.class, resolved.client());
        }
    }

    @Test
    void apacheTransportsWinOverUrlConnectionAndATieFails() {
        // The documented sync priorities of the sibling transports (apache-client 200, apache5-client 300,
        // aws-crt-client-sync 20); this module cannot depend on them, so they are stood in by name and priority.
        SdkHttpService apache = fake("apache-client", 200);
        SdkHttpService apache5 = fake("apache5-client", 300);
        SdkHttpService crt = fake("aws-crt-client-sync", 20);
        assertSame(apache5, InstalledHttpImplementations.of(SERVICE, apache, apache5, crt).defaultSync());
        assertSame(apache, InstalledHttpImplementations.of(crt, apache, SERVICE).defaultSync());
        assertSame(SERVICE, InstalledHttpImplementations.of(crt, SERVICE).defaultSync());
        HttpImplementationSelectionException tie = assertThrows(
            HttpImplementationSelectionException.class,
            () -> InstalledHttpImplementations.of(SERVICE, fake("other-client", 100)).defaultSync());
        assertEquals(HttpImplementationSelectionException.Reason.TIED_PRIORITY, tie.reason());
        assertEquals(List.of("other-client", "url-connection-client"), tie.implementations());
    }

    @Test
    void invalidOptionValuesFailConstructionAndLeakNoClient() {
        HttpClientConfiguration wrongType = HttpClientConfiguration
            .builder().put(HttpClientOption.TLS_KEY_MANAGERS, "not key managers").build();
        SdkClientConfigurationException e = assertThrows(
            SdkClientConfigurationException.class,
            () -> HttpClientResolver.sync().implementation(SERVICE).configuration(wrongType).resolve());
        assertTrue(e.getMessage().contains("tlsKeyManagersProvider"), e.getMessage());

        HttpClientConfiguration credentialsWithoutProxy = HttpClientConfiguration
            .builder().put(HttpClientOption.PROXY_CREDENTIALS, Boolean.TRUE).build();
        assertThrows(SdkClientConfigurationException.class,
                     () -> HttpClientResolver.sync().implementation(SERVICE).configuration(credentialsWithoutProxy)
                                             .resolve());
    }

    @Test
    void resolvedClientIsClosedWithItsScope() {
        ResolvedHttpClient<SdkSyncHttpClient> resolved = HttpClientResolver.sync().implementation(SERVICE).resolve();
        SdkSyncHttpClient client = resolved.client();
        resolved.close();
        assertThrows(IllegalStateException.class,
                     () -> client.prepare(TestRequests.get(1, "/").build(), HttpExecutionOptions.defaults()));
    }

    @Test
    void builderValidatesItsValues() {
        assertThrows(IllegalArgumentException.class,
                     () -> UrlConnectionHttpClient.builder().socketTimeout(Duration.ZERO).build());
        assertThrows(IllegalArgumentException.class,
                     () -> UrlConnectionHttpClient.builder().trustAllCertificates(true)
                                                  .tlsTrustManagersProvider(() -> null).build());
        assertThrows(IllegalArgumentException.class,
                     () -> ProxyConfiguration.builder().endpoint(URI.create("https://proxy:443")).build());
        assertThrows(IllegalArgumentException.class,
                     () -> ProxyConfiguration.builder().endpoint(URI.create("http://proxy:1")).username("u").build());
        ProxyConfiguration proxy = ProxyConfiguration.builder().endpoint(URI.create("http://proxy:1"))
                                                     .username("user").password("secret").build();
        assertFalse(proxy.toString().contains("secret"));
    }

    private static SdkHttpService fake(String name, int priority) {
        return new SdkHttpService() {
            @Override
            public String implementationName() {
                return name;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public HttpClientCapabilities capabilities() {
                return UrlConnectionHttpClient.CAPABILITIES;
            }

            @Override
            public SdkSyncHttpClient create(HttpClientConstruction construction) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
