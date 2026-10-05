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

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.ssl.SslHandshakeTimeoutException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpFailureCategory.Kind;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;

/**
 * The single mapping from Netty/JDK failures to {@link HttpTransportException} (Requirement 12.7). The category is
 * always {@link HttpFailureCategory#of(HttpPhase, Kind)} of the phase and kind below; nothing else in the module
 * chooses a category.
 *
 * <table>
 *   <caption>Engine failure to phase and kind (first matching row wins; causes are searched, outermost first)</caption>
 *   <tr><th>Failure (or cause)</th><th>Phase</th><th>Kind</th></tr>
 *   <tr><td>{@link HttpTransportException}</td><td>its own</td><td>kept as is</td></tr>
 *   <tr><td>{@link ConnectTimeoutException}</td><td>CONNECT</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link SslHandshakeTimeoutException}</td><td>TLS_HANDSHAKE</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link SocketTimeoutException}</td><td>current</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link ConnectException}, {@link NoRouteToHostException}, {@link UnknownHostException},
 *       {@link UnresolvedAddressException}</td><td>CONNECT</td><td>IO</td></tr>
 *   <tr><td>{@link ProxyTunnelException}</td><td>PROXY</td><td>IO</td></tr>
 *   <tr><td>{@link SSLException}</td><td>TLS_HANDSHAKE</td><td>IO</td></tr>
 *   <tr><td>{@link Http2Exception}, {@link TooLongFrameException}, other {@link DecoderException}</td>
 *       <td>current</td><td>PROTOCOL_VIOLATION</td></tr>
 *   <tr><td>{@link PrematureChannelClosureException}, other {@link IOException}, anything else</td>
 *       <td>current</td><td>IO</td></tr>
 * </table>
 *
 * <p>SDK-detected conditions are created here too: {@link #timeout} (acquisition, read and write timers),
 * {@link #poolExhausted}, {@link #closed} and {@link #prematureClose}.
 */
public final class NettyFailures {
    /** A row of the table, for tests (task 7.10). */
    public record Row(Class<? extends Throwable> type, Optional<HttpPhase> phase, Kind kind) {
    }

    /** The table rows in match order; an empty phase means the phase the exchange is in. */
    public static final List<Row> TABLE = List.of(
        new Row(ConnectTimeoutException.class, Optional.of(HttpPhase.CONNECT), Kind.TIMEOUT),
        new Row(SslHandshakeTimeoutException.class, Optional.of(HttpPhase.TLS_HANDSHAKE), Kind.TIMEOUT),
        new Row(SocketTimeoutException.class, Optional.empty(), Kind.TIMEOUT),
        new Row(ConnectException.class, Optional.of(HttpPhase.CONNECT), Kind.IO),
        new Row(NoRouteToHostException.class, Optional.of(HttpPhase.CONNECT), Kind.IO),
        new Row(UnknownHostException.class, Optional.of(HttpPhase.CONNECT), Kind.IO),
        new Row(UnresolvedAddressException.class, Optional.of(HttpPhase.CONNECT), Kind.IO),
        new Row(ProxyTunnelException.class, Optional.of(HttpPhase.PROXY), Kind.IO),
        new Row(SSLException.class, Optional.of(HttpPhase.TLS_HANDSHAKE), Kind.IO),
        new Row(Http2Exception.class, Optional.empty(), Kind.PROTOCOL_VIOLATION),
        new Row(TooLongFrameException.class, Optional.empty(), Kind.PROTOCOL_VIOLATION),
        new Row(DecoderException.class, Optional.empty(), Kind.PROTOCOL_VIOLATION));

    private NettyFailures() {
    }

    /** {@code failure}, which happened in {@code current}, as a categorized transport failure. */
    public static HttpTransportException map(Throwable failure, HttpPhase current, String clientName) {
        if (failure instanceof HttpTransportException transport) {
            return transport;
        }
        for (Throwable t = failure; t != null; t = t.getCause() == t ? null : t.getCause()) {
            for (Row row : TABLE) {
                if (row.type().isInstance(t)) {
                    // A decoder wrapping an SSLException is a TLS failure, not a protocol violation.
                    if (row.type() == DecoderException.class && t.getCause() instanceof SSLException) {
                        break;
                    }
                    return create(row.phase().orElse(current), row.kind(), failure, clientName);
                }
            }
        }
        return create(current, Kind.IO, failure, clientName);
    }

    /** A configured timer of {@code phase} elapsed. */
    public static HttpTransportException timeout(HttpPhase phase, String what, String clientName) {
        return HttpTransportException.builder().phase(phase).category(HttpFailureCategory.of(phase, Kind.TIMEOUT))
                                     .clientName(clientName).message(what + " elapsed during " + phase + ".").build();
    }

    /** No connection and no waiter slot (maxPendingConnectionAcquires reached). */
    public static HttpTransportException poolExhausted(int maxPending, String clientName) {
        return create(HttpPhase.CONNECTION_ACQUISITION, Kind.IO,
                      "No connection is available and " + maxPending + " acquisitions are already pending "
                      + "(maxPendingConnectionAcquires).", null, clientName);
    }

    /** The client is closed. */
    public static HttpTransportException closed(HttpPhase phase, String clientName) {
        return create(phase, Kind.IO, "The HTTP client is closed.", null, clientName);
    }

    /** The connection closed before the response completed. */
    public static HttpTransportException prematureClose(HttpPhase phase, String clientName) {
        return create(phase, Kind.IO, "The connection closed before the response was complete (" + phase + ").",
                      null, clientName);
    }

    private static HttpTransportException create(HttpPhase phase, Kind kind, Throwable cause, String clientName) {
        return create(phase, kind, null, cause, clientName);
    }

    private static HttpTransportException create(HttpPhase phase, Kind kind, String message, Throwable cause,
                                                 String clientName) {
        HttpFailureCategory category = HttpFailureCategory.of(phase, kind);
        String text = message != null ? message
                                      : "HTTP transport failure (" + category + " during " + phase + "): "
                                        + (cause == null ? "" : cause.getClass().getSimpleName());
        return HttpTransportException.builder().phase(phase).category(category).clientName(clientName).message(text)
                                     .cause(cause).build();
    }
}
