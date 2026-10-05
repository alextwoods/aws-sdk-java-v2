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

package software.amazon.awssdk.http.apache.internal;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Objects;
import javax.net.ssl.SSLException;
import org.apache.http.HttpException;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.ParseException;
import org.apache.http.TruncatedChunkException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;

/**
 * The single mapping of Apache HttpClient 4 failures to {@link HttpTransportException} (Requirement 12.7). The
 * exchange reports the {@link HttpPhase} it was in; this class refines the phase from the failure and picks the
 * {@link HttpFailureCategory.Kind}, and {@link HttpFailureCategory#of} picks the category.
 *
 * <table>
 *   <caption>Apache failure to phase and kind</caption>
 *   <tr><th>Failure</th><th>Phase</th><th>Kind</th></tr>
 *   <tr><td>any, after {@code abort()} or client close</td><td>the exchange's</td><td>CANCELLED</td></tr>
 *   <tr><td>{@link InterruptedException}, plain {@link InterruptedIOException}</td><td>the exchange's</td>
 *       <td>CANCELLED</td></tr>
 *   <tr><td>{@link ConnectionPoolTimeoutException}</td><td>CONNECTION_ACQUISITION</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@link TlsHandshakeException} (TLS factory)</td>
 *       <td>TLS_HANDSHAKE</td><td>TIMEOUT if caused by a socket timeout, else IO</td></tr>
 *   <tr><td>{@link SSLException} while connecting (whatever its cause)</td><td>TLS_HANDSHAKE</td><td>IO</td></tr>
 *   <tr><td>{@link ConnectTimeoutException}, {@link SocketTimeoutException}</td><td>the exchange's</td>
 *       <td>TIMEOUT</td></tr>
 *   <tr><td>{@link TruncatedChunkException}</td><td>the exchange's</td><td>IO</td></tr>
 *   <tr><td>{@link MalformedChunkCodingException}, {@link HttpException} (incl. {@code ProtocolException}),
 *       {@link ParseException}, {@link ClientProtocolException}</td><td>the exchange's</td>
 *       <td>PROTOCOL_VIOLATION</td></tr>
 *   <tr><td>any other exception</td><td>the exchange's</td><td>IO</td></tr>
 * </table>
 *
 * <p>The exchange's phase is CONNECTION_ACQUISITION while leasing, CONNECT while opening a direct connection, PROXY
 * while connecting to a proxy or tunnelling through it, TLS_HANDSHAKE while upgrading a tunnel, REQUEST_TRANSMISSION
 * while writing the request (including a request body source failure), and RESPONSE_CONSUMPTION while reading the
 * status, headers and body. Messages name the phase, kind and exception type only, never header values.
 */
public final class ApacheFailureMapper {
    private ApacheFailureMapper() {
    }

    /** The transport exception of {@code failure}, which happened in {@code phase}. */
    public static HttpTransportException map(HttpPhase phase, Throwable failure, boolean cancelled, String clientName) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(failure, "failure");
        // First table row: an aborted exchange is CANCELLED in the exchange's own phase, never a refined one.
        HttpPhase effective = cancelled ? phase : phaseOf(phase, failure);
        HttpFailureCategory.Kind kind = kindOf(failure, cancelled);
        return HttpTransportException.builder()
                                     .phase(effective)
                                     .category(HttpFailureCategory.of(effective, kind))
                                     .clientName(clientName)
                                     .message(clientName + ": " + kind + " failure during " + effective + " ("
                                              + failure.getClass().getName() + ")")
                                     .cause(failure)
                                     .build();
    }

    /** The phase a failure is reported in: the exchange's, refined for pool timeouts and TLS failures. */
    public static HttpPhase phaseOf(HttpPhase phase, Throwable failure) {
        if (failure instanceof ConnectionPoolTimeoutException) {
            return HttpPhase.CONNECTION_ACQUISITION;
        }
        if (failure instanceof TlsHandshakeException) {
            return HttpPhase.TLS_HANDSHAKE;
        }
        if (failure instanceof SSLException && (phase == HttpPhase.CONNECT || phase == HttpPhase.PROXY)) {
            return HttpPhase.TLS_HANDSHAKE;
        }
        return phase;
    }

    /** The kind of a failure; {@code cancelled} if the exchange was aborted or its client closed. */
    public static HttpFailureCategory.Kind kindOf(Throwable failure, boolean cancelled) {
        if (cancelled) {
            return HttpFailureCategory.Kind.CANCELLED;
        }
        Throwable t = failure instanceof TlsHandshakeException && failure.getCause() != null ? failure.getCause()
                                                                                          : failure;
        if (t instanceof ConnectionPoolTimeoutException || t instanceof ConnectTimeoutException
            || t instanceof SocketTimeoutException) {
            return HttpFailureCategory.Kind.TIMEOUT;
        }
        if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
            return HttpFailureCategory.Kind.CANCELLED;
        }
        if (t instanceof TruncatedChunkException) {
            return HttpFailureCategory.Kind.IO;
        }
        if (t instanceof MalformedChunkCodingException || t instanceof HttpException || t instanceof ParseException
            || t instanceof ClientProtocolException) {
            return HttpFailureCategory.Kind.PROTOCOL_VIOLATION;
        }
        return HttpFailureCategory.Kind.IO;
    }
}
