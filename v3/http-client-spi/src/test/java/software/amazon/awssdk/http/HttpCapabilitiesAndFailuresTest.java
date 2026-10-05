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

package software.amazon.awssdk.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;

/** Capability descriptors, execution options, the failure-category table and the transport exception. */
class HttpCapabilitiesAndFailuresTest {
    private static HttpClientCapabilities.Builder capabilities() {
        return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                     .maxBufferedBytesPerExchange(64 * 1024).cancellationTimeout(Duration.ofSeconds(1));
    }

    @Test
    void capabilitiesDescribeSupportedOptionsAndBounds() {
        HttpClientCapabilities caps = capabilities().addOption(HttpClientOption.READ_TIMEOUT, HttpClientOption.PROXY,
                                                               HttpClientOption.CONNECTION_TIMEOUT).build();
        assertEquals(List.of(HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.READ_TIMEOUT,
                             HttpClientOption.PROXY), List.copyOf(caps.options()));
        assertTrue(caps.supports(HttpClientOption.PROXY));
        assertFalse(caps.supports(Protocol.HTTP2));
        assertEquals(List.of(HttpClientOption.TLS_KEY_MANAGERS, HttpClientOption.MAX_CONNECTIONS,
                             HttpClientOption.PROTOCOL_VERSION),
                     caps.unsupported(Set.of(HttpClientOption.PROTOCOL_VERSION, HttpClientOption.PROXY,
                                             HttpClientOption.MAX_CONNECTIONS, HttpClientOption.TLS_KEY_MANAGERS)));
        assertEquals(caps, caps.toBuilder().build());
        assertEquals(caps.hashCode(), caps.copy(b -> { }).hashCode());
        HttpClientCapabilities http2 = caps.copy(b -> b.addProtocol(Protocol.HTTP2));
        assertTrue(http2.supports(Protocol.HTTP2));
        assertFalse(caps.supports(Protocol.HTTP2));
        assertThrows(UnsupportedOperationException.class, () -> caps.options().add(HttpClientOption.TCP_KEEP_ALIVE));
    }

    @Test
    void capabilitiesRequireModeProtocolAndPositiveBounds() {
        assertThrows(NullPointerException.class, () -> capabilities().mode(null).build());
        assertThrows(IllegalArgumentException.class,
                     () -> HttpClientCapabilities.builder().mode(HttpTransportMode.ASYNC)
                                                 .maxBufferedBytesPerExchange(1)
                                                 .cancellationTimeout(Duration.ofSeconds(1)).build());
        assertThrows(IllegalArgumentException.class, () -> capabilities().maxBufferedBytesPerExchange(0).build());
        assertThrows(IllegalArgumentException.class,
                     () -> capabilities().cancellationTimeout(Duration.ZERO).build());
        assertThrows(NullPointerException.class, () -> capabilities().cancellationTimeout(null).build());
    }

    @Test
    void everyOptionHasACategoryAndConfigurationName() {
        Set<String> names = new java.util.HashSet<>();
        for (HttpClientOption option : HttpClientOption.values()) {
            assertTrue(names.add(option.configurationName()), option.toString());
        }
        assertEquals(HttpClientOption.Category.TLS, HttpClientOption.TRUST_ALL_CERTIFICATES.category());
    }

    @Test
    void executionOptionsAreImmutableAndPositive() {
        HttpExecutionOptions options = HttpExecutionOptions.builder().readTimeout(Duration.ofSeconds(2)).build();
        assertEquals(Duration.ofSeconds(2), options.readTimeout().orElseThrow());
        assertTrue(options.writeTimeout().isEmpty());
        assertEquals(options, options.toBuilder().build());
        assertTrue(HttpExecutionOptions.defaults().readTimeout().isEmpty());
        assertThrows(IllegalArgumentException.class,
                     () -> HttpExecutionOptions.builder().writeTimeout(Duration.ofMillis(-1)));
    }

    @Test
    void failureCategoryTableIsStable() {
        Map<HttpPhase, HttpFailureCategory> io = Map.of(
            HttpPhase.CONNECTION_ACQUISITION, HttpFailureCategory.CONNECTION_ACQUISITION,
            HttpPhase.CONNECT, HttpFailureCategory.CONNECT,
            HttpPhase.PROXY, HttpFailureCategory.PROXY,
            HttpPhase.TLS_HANDSHAKE, HttpFailureCategory.TLS,
            HttpPhase.REQUEST_TRANSMISSION, HttpFailureCategory.REQUEST_TRANSMISSION,
            HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.RESPONSE_CONSUMPTION);
        for (HttpPhase phase : HttpPhase.values()) {
            assertEquals(io.get(phase), HttpFailureCategory.of(phase, HttpFailureCategory.Kind.IO));
            assertEquals(HttpFailureCategory.TIMEOUT, HttpFailureCategory.of(phase, HttpFailureCategory.Kind.TIMEOUT));
            assertEquals(HttpFailureCategory.CANCELLED,
                         HttpFailureCategory.of(phase, HttpFailureCategory.Kind.CANCELLED));
            assertEquals(HttpFailureCategory.PROTOCOL,
                         HttpFailureCategory.of(phase, HttpFailureCategory.Kind.PROTOCOL_VIOLATION));
        }
        assertEquals(EnumSet.of(HttpFailureCategory.CONNECTION_ACQUISITION, HttpFailureCategory.CONNECT,
                                HttpFailureCategory.TIMEOUT, HttpFailureCategory.REQUEST_TRANSMISSION,
                                HttpFailureCategory.RESPONSE_CONSUMPTION),
                     EnumSet.copyOf(java.util.Arrays.stream(HttpFailureCategory.values())
                                                    .filter(HttpFailureCategory::transientByDefault).toList()));
    }

    @Test
    void transportExceptionCarriesCategoryPhaseAndCause() {
        IOException cause = new IOException("Connection reset");
        HttpTransportException e = HttpTransportException.create(HttpPhase.RESPONSE_CONSUMPTION,
                                                                 HttpFailureCategory.Kind.IO, null, cause);
        assertTrue(e instanceof SdkClientException);
        assertEquals(HttpFailureCategory.RESPONSE_CONSUMPTION, e.category());
        assertEquals(HttpPhase.RESPONSE_CONSUMPTION, e.phase());
        assertSame(cause, e.getCause());
        assertTrue(e.retryable());
        assertTrue(e.getMessage().contains("RESPONSE_CONSUMPTION"));
        HttpTransportException copy = e.toBuilder().clientName("Test").numAttempts(2).build();
        assertEquals("Test", copy.clientName().orElseThrow());
        assertEquals(HttpFailureCategory.RESPONSE_CONSUMPTION, copy.category());
        assertEquals(2, copy.numAttempts());
        HttpTransportException timeout = HttpTransportException.create(HttpPhase.CONNECT,
                                                                       HttpFailureCategory.Kind.TIMEOUT, "slow", null);
        assertEquals(HttpFailureCategory.TIMEOUT, timeout.category());
        assertEquals(HttpPhase.CONNECT, timeout.phase());
        assertFalse(HttpTransportException.builder().phase(HttpPhase.CONNECT).category(HttpFailureCategory.CANCELLED)
                                          .build().retryable());
        assertThrows(NullPointerException.class,
                     () -> HttpTransportException.builder().phase(HttpPhase.CONNECT).build());
    }
}
