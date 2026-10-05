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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import java.io.IOException;
import java.net.ConnectException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;

class NettyFailuresTest {
    private static HttpTransportException map(Throwable t, HttpPhase phase) {
        return NettyFailures.map(t, phase, "test");
    }

    @Test
    void engineFailuresMapThroughThePhaseTable() {
        assertEquals(HttpFailureCategory.TIMEOUT, map(new ConnectTimeoutException("x"), HttpPhase.CONNECT).category());
        assertEquals(HttpPhase.TLS_HANDSHAKE,
                     map(new SslHandshakeTimeoutException("x"), HttpPhase.CONNECT).phase());
        assertEquals(HttpFailureCategory.CONNECT, map(new ConnectException("refused"), HttpPhase.CONNECT).category());
        assertEquals(HttpFailureCategory.TLS,
                     map(new DecoderException(new SSLHandshakeException("bad cert")), HttpPhase.RESPONSE_CONSUMPTION)
                         .category());
        assertEquals(HttpFailureCategory.PROTOCOL,
                     map(new DecoderException("bad status line"), HttpPhase.RESPONSE_CONSUMPTION).category());
        assertEquals(HttpFailureCategory.PROXY, map(new ProxyTunnelException("407"), HttpPhase.PROXY).category());
        assertEquals(HttpFailureCategory.REQUEST_TRANSMISSION,
                     map(new IOException("reset"), HttpPhase.REQUEST_TRANSMISSION).category());
        assertEquals(HttpFailureCategory.RESPONSE_CONSUMPTION,
                     map(new IllegalStateException("?"), HttpPhase.RESPONSE_CONSUMPTION).category());
        HttpTransportException own = NettyFailures.timeout(HttpPhase.CONNECTION_ACQUISITION, "t", "test");
        assertSame(own, map(own, HttpPhase.RESPONSE_CONSUMPTION));
    }

    @Test
    void everyRowAgreesWithTheProviderIndependentTable() {
        for (NettyFailures.Row row : NettyFailures.TABLE) {
            for (HttpPhase phase : HttpPhase.values()) {
                HttpPhase expectedPhase = row.phase().orElse(phase);
                assertEquals(HttpFailureCategory.of(expectedPhase, row.kind()),
                             map(instance(row.type()), phase).category(), row.toString());
            }
        }
    }

    private static Throwable instance(Class<? extends Throwable> type) {
        if (type == io.netty.handler.codec.http2.Http2Exception.class) {
            return io.netty.handler.codec.http2.Http2Exception.connectionError(
                io.netty.handler.codec.http2.Http2Error.PROTOCOL_ERROR, "x");
        }
        try {
            return type.getConstructor(String.class).newInstance("x");
        } catch (ReflectiveOperationException e) {
            try {
                return type.getConstructor().newInstance();
            } catch (ReflectiveOperationException e2) {
                throw new AssertionError(type.getName(), e2);
            }
        }
    }
}
