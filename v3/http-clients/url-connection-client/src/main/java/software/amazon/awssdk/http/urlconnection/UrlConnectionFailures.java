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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;

/**
 * The one place where JDK {@code HttpURLConnection} failures become stable V3 categories (Requirement 12.7); the HTTP
 * SPI's {@link HttpFailureCategory#of} then turns the (phase, kind) pair into the category. The exchange reports the
 * step it was in: {@link HttpPhase#CONNECT} ({@code connect()}: TCP, proxy tunnel and TLS handshake),
 * {@link HttpPhase#REQUEST_TRANSMISSION} (writing the body) or {@link HttpPhase#RESPONSE_CONSUMPTION}
 * ({@code getResponseCode()}, headers and body). A request without a body has its request line and headers written
 * inside {@code getResponseCode()}, so its write failures are reported in {@code RESPONSE_CONSUMPTION}.
 *
 * <table>
 *   <caption>Mapping, first matching row wins</caption>
 *   <tr><th>Condition</th><th>Phase</th><th>Kind</th><th>Category</th></tr>
 *   <tr><td>exchange aborted or client closed</td><td>reported step</td><td>CANCELLED</td><td>CANCELLED</td></tr>
 *   <tr><td>{@link SocketTimeoutException} in CONNECT whose message names connect</td><td>CONNECT</td>
 *       <td>TIMEOUT</td><td>TIMEOUT</td></tr>
 *   <tr><td>other {@link SocketTimeoutException} in CONNECT, proxied</td><td>PROXY</td><td>TIMEOUT</td>
 *       <td>TIMEOUT</td></tr>
 *   <tr><td>other {@link SocketTimeoutException} in CONNECT, https</td><td>TLS_HANDSHAKE</td><td>TIMEOUT</td>
 *       <td>TIMEOUT</td></tr>
 *   <tr><td>any other {@link SocketTimeoutException}</td><td>reported step</td><td>TIMEOUT</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link HttpRetryException} 407 (proxy authentication in streaming mode) or an IOException
 *       "Unable to tunnel through proxy"</td><td>PROXY</td><td>IO</td><td>PROXY</td></tr>
 *   <tr><td>{@link HttpRetryException} 401 (the JDK cannot return a 401 to a streamed request)</td>
 *       <td>RESPONSE_CONSUMPTION</td><td>PROTOCOL_VIOLATION</td><td>PROTOCOL</td></tr>
 *   <tr><td>{@link SSLHandshakeException}, or any {@link SSLException} in CONNECT</td><td>TLS_HANDSHAKE</td>
 *       <td>IO</td><td>TLS</td></tr>
 *   <tr><td>{@link ProtocolException}, or an IOException "Invalid Http response"</td><td>reported step</td>
 *       <td>PROTOCOL_VIOLATION</td><td>PROTOCOL</td></tr>
 *   <tr><td>{@link ConnectException}, {@link UnknownHostException}, {@link NoRouteToHostException} or any other
 *       IOException in CONNECT, proxied</td><td>PROXY</td><td>IO</td><td>PROXY</td></tr>
 *   <tr><td>the same, not proxied</td><td>CONNECT</td><td>IO</td><td>CONNECT</td></tr>
 *   <tr><td>any other failure</td><td>reported step</td><td>IO</td>
 *       <td>REQUEST_TRANSMISSION or RESPONSE_CONSUMPTION</td></tr>
 * </table>
 *
 * <p>{@code HttpURLConnection} has no connection pool to wait for, so {@link HttpPhase#CONNECTION_ACQUISITION}
 * never occurs. Public so provider-independent property tests (task 7.10) can drive the table.
 */
public final class UrlConnectionFailures {
    private UrlConnectionFailures() {
    }

    /** A classified failure: where it happened and what went wrong. */
    public record Classification(HttpPhase phase, HttpFailureCategory.Kind kind) {
        public Classification {
            Objects.requireNonNull(phase, "phase");
            Objects.requireNonNull(kind, "kind");
        }

        /** {@link HttpFailureCategory#of} of this classification. */
        public HttpFailureCategory category() {
            return HttpFailureCategory.of(phase, kind);
        }
    }

    /**
     * Classifies {@code failure}, which happened in {@code step}.
     *
     * @param step one of {@code CONNECT}, {@code REQUEST_TRANSMISSION}, {@code RESPONSE_CONSUMPTION}
     * @param cancelled whether the exchange was aborted or the client closed
     * @param proxied whether the exchange goes through a proxy
     * @param https whether the target scheme is https
     */
    public static Classification classify(HttpPhase step, Throwable failure, boolean cancelled, boolean proxied,
                                          boolean https) {
        Objects.requireNonNull(step, "step");
        Throwable cause = failure instanceof UncheckedIOException u && u.getCause() != null ? u.getCause() : failure;
        if (cancelled) {
            return new Classification(step, HttpFailureCategory.Kind.CANCELLED);
        }
        if (cause instanceof SocketTimeoutException) {
            if (step != HttpPhase.CONNECT) {
                return new Classification(step, HttpFailureCategory.Kind.TIMEOUT);
            }
            String message = lower(cause);
            HttpPhase phase = message.contains("connect") ? HttpPhase.CONNECT
                                                          : proxied ? HttpPhase.PROXY
                                                                    : https ? HttpPhase.TLS_HANDSHAKE
                                                                            : HttpPhase.CONNECT;
            return new Classification(phase, HttpFailureCategory.Kind.TIMEOUT);
        }
        if (cause instanceof HttpRetryException retry) {
            return retry.responseCode() == 407
                   ? new Classification(HttpPhase.PROXY, HttpFailureCategory.Kind.IO)
                   : new Classification(HttpPhase.RESPONSE_CONSUMPTION, HttpFailureCategory.Kind.PROTOCOL_VIOLATION);
        }
        if (cause instanceof IOException && lower(cause).startsWith("unable to tunnel through proxy")) {
            return new Classification(HttpPhase.PROXY, HttpFailureCategory.Kind.IO);
        }
        if (cause instanceof SSLHandshakeException || cause instanceof SSLException && step == HttpPhase.CONNECT) {
            return new Classification(HttpPhase.TLS_HANDSHAKE, HttpFailureCategory.Kind.IO);
        }
        if (cause instanceof ProtocolException || lower(cause).startsWith("invalid http response")) {
            return new Classification(step, HttpFailureCategory.Kind.PROTOCOL_VIOLATION);
        }
        if (step == HttpPhase.CONNECT) {
            return new Classification(proxied ? HttpPhase.PROXY : HttpPhase.CONNECT, HttpFailureCategory.Kind.IO);
        }
        return new Classification(step, HttpFailureCategory.Kind.IO);
    }

    /** The {@link HttpTransportException} of {@link #classify}, with {@code failure} as its cause. */
    static HttpTransportException toException(String clientName, HttpPhase step, Throwable failure, boolean cancelled,
                                              boolean proxied, boolean https) {
        Classification c = classify(step, failure, cancelled, proxied, https);
        String detail = failure == null || failure.getMessage() == null ? "" : ": " + failure.getMessage();
        String message = c.kind() == HttpFailureCategory.Kind.CANCELLED
                         ? "The HTTP exchange was aborted during " + c.phase() + "."
                         : "HTTP transport failure (" + c.category() + " during " + c.phase() + ") in " + clientName
                           + detail;
        return HttpTransportException.builder().phase(c.phase()).category(c.category()).clientName(clientName)
                                     .message(message).cause(failure).build();
    }

    /** A cancellation in {@code step} without an engine failure. */
    static HttpTransportException cancelled(String clientName, HttpPhase step) {
        return toException(clientName, step, null, true, false, false);
    }

    private static String lower(Throwable t) {
        return t == null || t.getMessage() == null ? "" : t.getMessage().toLowerCase(Locale.ROOT);
    }
}
