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

package software.amazon.awssdk.http.apache5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.apache5.internal.Apache5FailureMapping;

/** The Apache 5 failure table against the provider-independent {@link HttpFailureCategory#of} table. */
class Apache5FailureMappingTest {
    private static void expect(Throwable failure, HttpPhase tracked, boolean cancelled, HttpPhase phase,
                               HttpFailureCategory category) {
        HttpTransportException e = Apache5FailureMapping.toTransportException(failure, tracked, cancelled, "Apache5");
        assertEquals(phase, e.phase(), failure.toString());
        assertEquals(category, e.category(), failure.toString());
        assertEquals(HttpFailureCategory.of(e.phase(), Apache5FailureMapping.kind(failure, cancelled)), e.category());
        assertEquals("Apache5", e.clientName().orElseThrow());
        assertFalse(e.getMessage().contains("secret"));
    }

    @Test
    void everyRowMapsToTheSharedTable() {
        HttpPhase response = HttpPhase.RESPONSE_CONSUMPTION;
        expect(new CancellationException(), response, false, response, HttpFailureCategory.CANCELLED);
        expect(new IOException("secret"), response, true, response, HttpFailureCategory.CANCELLED);
        expect(new ConnectionRequestTimeoutException("secret"), HttpPhase.CONNECTION_ACQUISITION, false,
               HttpPhase.CONNECTION_ACQUISITION, HttpFailureCategory.TIMEOUT);
        expect(new ConnectTimeoutException("secret"), HttpPhase.CONNECTION_ACQUISITION, false, HttpPhase.CONNECT,
               HttpFailureCategory.TIMEOUT);
        expect(new SocketTimeoutException(), response, false, response, HttpFailureCategory.TIMEOUT);
        expect(new SSLHandshakeException("secret"), HttpPhase.CONNECTION_ACQUISITION, false,
               HttpPhase.TLS_HANDSHAKE, HttpFailureCategory.TLS);
        expect(new IOException(new SSLHandshakeException("secret")), HttpPhase.CONNECTION_ACQUISITION, false,
               HttpPhase.TLS_HANDSHAKE, HttpFailureCategory.TLS);
        expect(new SSLException("secret"), HttpPhase.CONNECTION_ACQUISITION, false, HttpPhase.TLS_HANDSHAKE,
               HttpFailureCategory.TLS);
        expect(new SSLException("secret"), response, false, response, HttpFailureCategory.RESPONSE_CONSUMPTION);
        expect(new ConnectException(), HttpPhase.CONNECTION_ACQUISITION, false, HttpPhase.CONNECT,
               HttpFailureCategory.CONNECT);
        expect(new UnknownHostException(), HttpPhase.CONNECTION_ACQUISITION, false, HttpPhase.CONNECT,
               HttpFailureCategory.CONNECT);
        expect(new ProtocolException("secret"), response, false, response, HttpFailureCategory.PROTOCOL);
        expect(new IOException("secret"), HttpPhase.REQUEST_TRANSMISSION, false, HttpPhase.REQUEST_TRANSMISSION,
               HttpFailureCategory.REQUEST_TRANSMISSION);
        expect(new ExecutionException(new SocketTimeoutException()), response, false, response,
               HttpFailureCategory.TIMEOUT);
    }

    @Test
    void transportExceptionsPassThrough() {
        HttpTransportException original = HttpTransportException.create(HttpPhase.PROXY,
                                                                        HttpFailureCategory.Kind.IO, "m", null);
        assertSame(original, Apache5FailureMapping.toTransportException(original, HttpPhase.CONNECT, true, "x"));
    }
}
