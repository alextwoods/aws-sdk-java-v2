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

package software.amazon.awssdk.http.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.ProtocolException;
import org.apache.http.TruncatedChunkException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.apache.internal.ApacheFailureMapper;
import software.amazon.awssdk.http.apache.internal.TlsHandshakeException;

/**
 * Every row of the documented {@link ApacheFailureMapper} table, against {@link HttpFailureCategory#of}.
 *
 * <p><b>Validates: Requirements 12.7</b>
 */
class ApacheFailureMapperTest {
    private static final HttpPhase RESPONSE = HttpPhase.RESPONSE_CONSUMPTION;

    private static void expect(Throwable failure, HttpPhase tracked, boolean cancelled, HttpPhase phase,
                               HttpFailureCategory category) {
        HttpTransportException e = ApacheFailureMapper.map(tracked, failure, cancelled, "Apache");
        assertEquals(phase, e.phase(), failure.toString());
        assertEquals(category, e.category(), failure.toString());
        assertEquals(HttpFailureCategory.of(e.phase(), ApacheFailureMapper.kindOf(failure, cancelled)),
                     e.category());
        assertEquals("Apache", e.clientName().orElseThrow());
        assertFalse(e.getMessage().contains("secret"), e.getMessage());
    }

    @Test
    void everyRowMapsToTheSharedTable() {
        expect(new IOException("secret"), RESPONSE, true, RESPONSE, HttpFailureCategory.CANCELLED);
        expect(new SocketTimeoutException("secret"), HttpPhase.CONNECT, true, HttpPhase.CONNECT,
               HttpFailureCategory.CANCELLED);
        expect(new InterruptedIOException("secret"), RESPONSE, false, RESPONSE, HttpFailureCategory.CANCELLED);
        expect(new ConnectionPoolTimeoutException("secret"), HttpPhase.CONNECTION_ACQUISITION, false,
               HttpPhase.CONNECTION_ACQUISITION, HttpFailureCategory.TIMEOUT);
        expect(new ConnectTimeoutException("secret"), HttpPhase.CONNECT, false, HttpPhase.CONNECT,
               HttpFailureCategory.TIMEOUT);
        expect(new SocketTimeoutException("secret"), RESPONSE, false, RESPONSE, HttpFailureCategory.TIMEOUT);
        expect(new TlsHandshakeException(new SSLHandshakeException("secret")), HttpPhase.CONNECT, false,
               HttpPhase.TLS_HANDSHAKE, HttpFailureCategory.TLS);
        expect(new TlsHandshakeException(new SocketTimeoutException("secret")), HttpPhase.CONNECT, false,
               HttpPhase.TLS_HANDSHAKE, HttpFailureCategory.TIMEOUT);
        expect(new SSLException("secret"), HttpPhase.PROXY, false, HttpPhase.TLS_HANDSHAKE, HttpFailureCategory.TLS);
        expect(new SSLException("secret"), RESPONSE, false, RESPONSE, HttpFailureCategory.RESPONSE_CONSUMPTION);
        expect(new ConnectException("secret"), HttpPhase.CONNECT, false, HttpPhase.CONNECT,
               HttpFailureCategory.CONNECT);
        expect(new ConnectException("secret"), HttpPhase.PROXY, false, HttpPhase.PROXY, HttpFailureCategory.PROXY);
        expect(new TruncatedChunkException("secret"), RESPONSE, false, RESPONSE,
               HttpFailureCategory.RESPONSE_CONSUMPTION);
        expect(new MalformedChunkCodingException("secret"), RESPONSE, false, RESPONSE, HttpFailureCategory.PROTOCOL);
        expect(new ProtocolException("secret"), RESPONSE, false, RESPONSE, HttpFailureCategory.PROTOCOL);
        expect(new IOException("secret"), HttpPhase.REQUEST_TRANSMISSION, false, HttpPhase.REQUEST_TRANSMISSION,
               HttpFailureCategory.REQUEST_TRANSMISSION);
        expect(new IOException("secret"), HttpPhase.CONNECTION_ACQUISITION, false,
               HttpPhase.CONNECTION_ACQUISITION, HttpFailureCategory.CONNECTION_ACQUISITION);
    }

    /** Regression (task 7.10): a cancelled exchange keeps its own phase even when the failure would refine it. */
    @Test
    void cancelledExchangeKeepsItsOwnPhase() {
        expect(new ConnectionPoolTimeoutException("secret"), HttpPhase.CONNECT, true, HttpPhase.CONNECT,
               HttpFailureCategory.CANCELLED);
        expect(new SSLException("secret"), HttpPhase.PROXY, true, HttpPhase.PROXY, HttpFailureCategory.CANCELLED);
        expect(new TlsHandshakeException(new SSLHandshakeException("secret")), RESPONSE, true, RESPONSE,
               HttpFailureCategory.CANCELLED);
    }

    /** Any failure in any phase: the category is the shared table's, and a cancelled exchange is CANCELLED. */
    @Property(tries = 200)
    void categoryAlwaysFollowsTheSharedTable(@ForAll HttpPhase phase, @ForAll("failures") Throwable failure,
                                             @ForAll boolean cancelled) {
        HttpTransportException e = ApacheFailureMapper.map(phase, failure, cancelled, "Apache");
        assertEquals(HttpFailureCategory.of(e.phase(), ApacheFailureMapper.kindOf(failure, cancelled)),
                     e.category());
        assertEquals(cancelled ? phase : ApacheFailureMapper.phaseOf(phase, failure), e.phase());
        if (cancelled) {
            assertEquals(HttpFailureCategory.CANCELLED, e.category());
        }
        assertEquals(failure, e.getCause());
    }

    @Provide
    Arbitrary<Throwable> failures() {
        return Arbitraries.of(new IOException("secret"), new SocketTimeoutException("secret"),
                              new ConnectException("secret"), new SSLException("secret"),
                              new ConnectionPoolTimeoutException("secret"), new ConnectTimeoutException("secret"),
                              new TlsHandshakeException(new SSLHandshakeException("secret")),
                              new ProtocolException("secret"), new TruncatedChunkException("secret"),
                              new InterruptedIOException("secret"), new IllegalStateException("secret"));
    }
}
