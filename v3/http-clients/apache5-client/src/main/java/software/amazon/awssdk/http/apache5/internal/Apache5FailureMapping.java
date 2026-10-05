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

package software.amazon.awssdk.http.apache5.internal;

import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.impl.TunnelRefusedException;
import org.apache.hc.client5.http.impl.classic.RequestFailedException;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http2.H2ConnectionException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpFailureCategory.Kind;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;

/**
 * The one mapping from Apache HttpClient 5 failures to {@link HttpTransportException} (Requirement 12.7), shared by
 * the sync and async Apache 5 clients. Internal to {@code apache5-client}; public only so the transport failure
 * property test (task 7.10) can compare it with the provider-independent table of {@link HttpFailureCategory#of}.
 *
 * <p>A failure is classified by the first {@link #TABLE} row whose type matches the failure (after unwrapping
 * {@link ExecutionException}, {@link CompletionException} and {@link UncheckedIOException}). A row fixes the
 * {@link Kind} and, where the engine type implies it, the {@link HttpPhase}; otherwise the phase is the one the
 * exchange tracked when the failure happened. A failure of an exchange that was aborted or cancelled is always
 * {@link Kind#CANCELLED}, in the tracked phase.
 *
 * <table>
 *   <caption>Apache 5 failure rows, in match order</caption>
 *   <tr><th>Engine type</th><th>Phase</th><th>Kind</th></tr>
 *   <tr><td>any failure caused by an {@link SSLHandshakeException} ({@link #TLS_HANDSHAKE_CAUSE})</td>
 *       <td>TLS_HANDSHAKE</td><td>IO</td></tr>
 *   <tr><td>{@link CancellationException}, {@link RequestFailedException}</td><td>tracked</td><td>CANCELLED</td></tr>
 *   <tr><td>{@link ConnectionRequestTimeoutException}</td><td>CONNECTION_ACQUISITION</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link ConnectTimeoutException}</td><td>CONNECT</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link SocketTimeoutException}, {@link TimeoutException}</td><td>tracked</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link TunnelRefusedException}</td><td>PROXY</td><td>IO</td></tr>
 *   <tr><td>{@link SSLHandshakeException}</td><td>TLS_HANDSHAKE</td><td>IO</td></tr>
 *   <tr><td>{@link SSLException}</td><td>TLS_HANDSHAKE before transmission, else tracked</td><td>IO</td></tr>
 *   <tr><td>{@link HttpHostConnectException}, {@link ConnectException}, {@link UnknownHostException},
 *       {@link NoRouteToHostException}</td><td>CONNECT</td><td>IO</td></tr>
 *   <tr><td>{@link ProtocolException}, {@link H2ConnectionException}</td><td>tracked</td>
 *       <td>PROTOCOL_VIOLATION</td></tr>
 *   <tr><td>{@link InterruptedIOException} (other)</td><td>tracked</td><td>TIMEOUT</td></tr>
 *   <tr><td>any other {@link Throwable} ({@code IOException}, {@link HttpException}, engine errors)</td>
 *       <td>tracked</td><td>IO</td></tr>
 * </table>
 */
public final class Apache5FailureMapping {
    /** One classification row: failures of {@code type} have {@code kind}, in {@code phase} if not null. */
    public record Row(Class<? extends Throwable> type, HttpPhase phase, Kind kind) {
        public Row {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(kind, "kind");
        }
    }

    /** The rows, in match order; a {@code null} phase means the tracked phase. */
    public static final List<Row> TABLE = List.of(
        new Row(CancellationException.class, null, Kind.CANCELLED),
        new Row(RequestFailedException.class, null, Kind.CANCELLED),
        new Row(ConnectionRequestTimeoutException.class, HttpPhase.CONNECTION_ACQUISITION, Kind.TIMEOUT),
        new Row(ConnectTimeoutException.class, HttpPhase.CONNECT, Kind.TIMEOUT),
        new Row(SocketTimeoutException.class, null, Kind.TIMEOUT),
        new Row(TimeoutException.class, null, Kind.TIMEOUT),
        new Row(TunnelRefusedException.class, HttpPhase.PROXY, Kind.IO),
        new Row(SSLHandshakeException.class, HttpPhase.TLS_HANDSHAKE, Kind.IO),
        new Row(SSLException.class, null, Kind.IO),
        new Row(HttpHostConnectException.class, HttpPhase.CONNECT, Kind.IO),
        new Row(ConnectException.class, HttpPhase.CONNECT, Kind.IO),
        new Row(UnknownHostException.class, HttpPhase.CONNECT, Kind.IO),
        new Row(NoRouteToHostException.class, HttpPhase.CONNECT, Kind.IO),
        new Row(ProtocolException.class, null, Kind.PROTOCOL_VIOLATION),
        new Row(H2ConnectionException.class, null, Kind.PROTOCOL_VIOLATION),
        new Row(InterruptedIOException.class, null, Kind.TIMEOUT));

    /** The row of a failure whose cause chain holds an {@link SSLHandshakeException}; it is checked first. */
    public static final Row TLS_HANDSHAKE_CAUSE = new Row(SSLHandshakeException.class, HttpPhase.TLS_HANDSHAKE,
                                                          Kind.IO);

    private Apache5FailureMapping() {
    }

    /** {@code failure} without {@link ExecutionException}, {@link CompletionException} or unchecked-I/O wrappers. */
    public static Throwable unwrap(Throwable failure) {
        Throwable t = Objects.requireNonNull(failure, "failure");
        while ((t instanceof ExecutionException || t instanceof CompletionException
                || t instanceof UncheckedIOException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /** The first row matching {@code failure} (unwrapped), or {@code null} for the IO default. */
    public static Row row(Throwable failure) {
        Throwable t = unwrap(failure);
        for (Throwable c = t.getCause(); c != null && c != c.getCause(); c = c.getCause()) {
            if (c instanceof SSLHandshakeException) {
                return TLS_HANDSHAKE_CAUSE;
            }
        }
        for (Row row : TABLE) {
            if (row.type().isInstance(t)) {
                return row;
            }
        }
        return null;
    }

    /** The kind of {@code failure}; {@link Kind#CANCELLED} if the exchange was {@code cancelled}. */
    public static Kind kind(Throwable failure, boolean cancelled) {
        if (cancelled) {
            return Kind.CANCELLED;
        }
        Row row = row(failure);
        return row == null ? Kind.IO : row.kind();
    }

    /** The phase of {@code failure} that happened while the exchange was in {@code tracked}. */
    public static HttpPhase phase(Throwable failure, HttpPhase tracked, boolean cancelled) {
        Objects.requireNonNull(tracked, "tracked");
        if (cancelled) {
            return tracked;
        }
        Row row = row(failure);
        if (row == null) {
            return tracked;
        }
        if (row.phase() != null) {
            return row.phase();
        }
        if (row.type() == SSLException.class && tracked.ordinal() < HttpPhase.REQUEST_TRANSMISSION.ordinal()) {
            return HttpPhase.TLS_HANDSHAKE;
        }
        return tracked;
    }

    /**
     * {@code failure} as an {@link HttpTransportException} of {@code clientName}: itself if it already is one, else
     * categorized by {@link HttpFailureCategory#of} from its {@link #phase} and {@link #kind}, with the unwrapped
     * engine failure as the cause. The message names only the category, phase and engine type, never message data.
     */
    public static HttpTransportException toTransportException(Throwable failure, HttpPhase tracked,
                                                              boolean cancelled, String clientName) {
        Throwable cause = unwrap(failure);
        if (cause instanceof HttpTransportException transport) {
            return transport;
        }
        HttpPhase phase = phase(cause, tracked, cancelled);
        Kind kind = kind(cause, cancelled);
        HttpFailureCategory category = HttpFailureCategory.of(phase, kind);
        return HttpTransportException.builder().phase(phase).category(category).clientName(clientName).cause(cause)
                                     .message("HTTP transport failure (" + category + " during " + phase + ") in "
                                              + clientName + ": " + cause.getClass().getName())
                                     .build();
    }
}
