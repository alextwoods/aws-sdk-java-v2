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
import static software.amazon.awssdk.http.HttpPhase.CONNECT;
import static software.amazon.awssdk.http.HttpPhase.PROXY;
import static software.amazon.awssdk.http.HttpPhase.REQUEST_TRANSMISSION;
import static software.amazon.awssdk.http.HttpPhase.RESPONSE_CONSUMPTION;
import static software.amazon.awssdk.http.HttpPhase.TLS_HANDSHAKE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;

/** Every row of the {@link UrlConnectionFailures} table. */
class UrlConnectionFailuresTest {
    private static void check(HttpPhase step, Throwable failure, boolean cancelled, boolean proxied, boolean https,
                              HttpPhase phase, HttpFailureCategory category) {
        UrlConnectionFailures.Classification c =
            UrlConnectionFailures.classify(step, failure, cancelled, proxied, https);
        assertEquals(phase, c.phase(), () -> step + " " + failure);
        assertEquals(category, c.category(), () -> step + " " + failure);
    }

    @Test
    void cancellationWinsInEveryStep() {
        for (HttpPhase step : new HttpPhase[] {CONNECT, REQUEST_TRANSMISSION, RESPONSE_CONSUMPTION}) {
            check(step, new SocketException("Socket closed"), true, true, true, step, HttpFailureCategory.CANCELLED);
            check(step, null, true, false, false, step, HttpFailureCategory.CANCELLED);
        }
    }

    @Test
    void timeouts() {
        check(CONNECT, new SocketTimeoutException("Connect timed out"), false, true, true, CONNECT,
              HttpFailureCategory.TIMEOUT);
        check(CONNECT, new SocketTimeoutException("Read timed out"), false, true, true, PROXY,
              HttpFailureCategory.TIMEOUT);
        check(CONNECT, new SocketTimeoutException("Read timed out"), false, false, true, TLS_HANDSHAKE,
              HttpFailureCategory.TIMEOUT);
        check(CONNECT, new SocketTimeoutException("Read timed out"), false, false, false, CONNECT,
              HttpFailureCategory.TIMEOUT);
        check(REQUEST_TRANSMISSION, new SocketTimeoutException(), false, false, false, REQUEST_TRANSMISSION,
              HttpFailureCategory.TIMEOUT);
        check(RESPONSE_CONSUMPTION, new SocketTimeoutException("Read timed out"), false, false, false,
              RESPONSE_CONSUMPTION, HttpFailureCategory.TIMEOUT);
        check(RESPONSE_CONSUMPTION, new UncheckedIOException(new SocketTimeoutException()), false, false, false,
              RESPONSE_CONSUMPTION, HttpFailureCategory.TIMEOUT);
    }

    @Test
    void proxyFailures() {
        check(RESPONSE_CONSUMPTION, new HttpRetryException("proxy auth", 407), false, true, false, PROXY,
              HttpFailureCategory.PROXY);
        check(CONNECT, new IOException("Unable to tunnel through proxy. Proxy returns \"HTTP/1.1 403\""), false,
              true, true, PROXY, HttpFailureCategory.PROXY);
        check(CONNECT, new ConnectException("Connection refused"), false, true, false, PROXY,
              HttpFailureCategory.PROXY);
        check(CONNECT, new UnknownHostException("proxy"), false, true, false, PROXY, HttpFailureCategory.PROXY);
    }

    @Test
    void tlsFailures() {
        check(CONNECT, new SSLHandshakeException("PKIX"), false, false, true, TLS_HANDSHAKE, HttpFailureCategory.TLS);
        check(CONNECT, new SSLException("closed"), false, false, true, TLS_HANDSHAKE, HttpFailureCategory.TLS);
        check(RESPONSE_CONSUMPTION, new SSLHandshakeException("renegotiation"), false, false, true, TLS_HANDSHAKE,
              HttpFailureCategory.TLS);
        check(RESPONSE_CONSUMPTION, new SSLException("bad record"), false, false, true, RESPONSE_CONSUMPTION,
              HttpFailureCategory.RESPONSE_CONSUMPTION);
    }

    @Test
    void protocolViolations() {
        check(RESPONSE_CONSUMPTION, new ProtocolException("bad"), false, false, false, RESPONSE_CONSUMPTION,
              HttpFailureCategory.PROTOCOL);
        check(RESPONSE_CONSUMPTION, new IOException("Invalid Http response"), false, false, false,
              RESPONSE_CONSUMPTION, HttpFailureCategory.PROTOCOL);
        check(RESPONSE_CONSUMPTION, new HttpRetryException("server auth", 401), false, false, false,
              RESPONSE_CONSUMPTION, HttpFailureCategory.PROTOCOL);
    }

    @Test
    void connectAndIoFailures() {
        check(CONNECT, new ConnectException("Connection refused"), false, false, false, CONNECT,
              HttpFailureCategory.CONNECT);
        check(CONNECT, new UnknownHostException("h"), false, false, false, CONNECT, HttpFailureCategory.CONNECT);
        check(CONNECT, new NoRouteToHostException(), false, false, false, CONNECT, HttpFailureCategory.CONNECT);
        check(REQUEST_TRANSMISSION, new SocketException("Broken pipe"), false, true, true, REQUEST_TRANSMISSION,
              HttpFailureCategory.REQUEST_TRANSMISSION);
        check(RESPONSE_CONSUMPTION, new IOException("Premature EOF"), false, false, false, RESPONSE_CONSUMPTION,
              HttpFailureCategory.RESPONSE_CONSUMPTION);
        check(RESPONSE_CONSUMPTION, new IllegalStateException("x"), false, false, false, RESPONSE_CONSUMPTION,
              HttpFailureCategory.RESPONSE_CONSUMPTION);
    }

    @Test
    void neverReportsConnectionAcquisition() {
        for (HttpPhase step : new HttpPhase[] {CONNECT, REQUEST_TRANSMISSION, RESPONSE_CONSUMPTION}) {
            for (Throwable t : new Throwable[] {new IOException(), new SocketTimeoutException(), new SSLException("")}) {
                for (boolean flag : new boolean[] {false, true}) {
                    UrlConnectionFailures.Classification c = UrlConnectionFailures.classify(step, t, false, flag, flag);
                    assertEquals(false, c.category() == HttpFailureCategory.CONNECTION_ACQUISITION);
                }
            }
        }
    }
}
