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

package software.amazon.awssdk.http.crt.internal;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.ContentLengthMismatchException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpFailureCategory.Kind;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;

/**
 * The single place where CRT failures become {@link HttpTransportException}s (Requirement 12.7), shared by the sync
 * and async clients. A failure is classified to a {@link HttpPhase} and {@link Kind}; the category is then always
 * {@link HttpFailureCategory#of(HttpPhase, Kind)}, so it agrees with the provider-independent phase table.
 *
 * <p>CRT error codes (aws-c-io / aws-c-http names) and their classification; "current" is the phase the exchange was
 * in when the failure was reported:
 * <table>
 *   <caption>CRT error code to phase and kind</caption>
 *   <tr><th>CRT errors</th><th>Phase</th><th>Kind</th></tr>
 *   <tr><td>{@code AWS_IO_TLS_*} handshake and certificate errors (1029-1037, 1068, 1071, 1072, 1173, 1178,
 *       1182-1194)</td><td>TLS_HANDSHAKE</td><td>IO</td></tr>
 *   <tr><td>{@code AWS_IO_TLS_NEGOTIATION_TIMEOUT} (1067)</td><td>TLS_HANDSHAKE</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@code AWS_IO_SOCKET_TIMEOUT} (1048, the connect timeout)</td><td>CONNECT</td><td>TIMEOUT</td></tr>
 *   <tr><td>socket refused/unreachable/network down/invalid address/connect aborted (1045, 1047, 1049, 1050, 1055,
 *       1057), DNS (1058-1061, 1195)</td><td>CONNECT</td><td>IO</td></tr>
 *   <tr><td>{@code AWS_ERROR_HTTP_PROXY_*} (2071, 2081-2083)</td><td>PROXY</td><td>IO</td></tr>
 *   <tr><td>{@code AWS_ERROR_HTTP_CONNECTION_MANAGER_ACQUISITION_TIMEOUT} (2093)</td>
 *       <td>CONNECTION_ACQUISITION</td><td>TIMEOUT</td></tr>
 *   <tr><td>connection manager invalid state / shutting down / max pending acquisitions (2068, 2072, 2094),
 *       stream manager acquire failure (2087)</td><td>CONNECTION_ACQUISITION</td><td>IO</td></tr>
 *   <tr><td>{@code AWS_ERROR_HTTP_RESPONSE_FIRST_BYTE_TIMEOUT} (2092)</td><td>RESPONSE_CONSUMPTION</td>
 *       <td>TIMEOUT</td></tr>
 *   <tr><td>{@code AWS_ERROR_HTTP_CHANNEL_THROUGHPUT_FAILURE} (2073)</td><td>current</td><td>TIMEOUT</td></tr>
 *   <tr><td>{@code AWS_ERROR_IO_OPERATION_CANCELLED} (1042), {@code AWS_ERROR_HTTP_STREAM_CANCELLED} (2095)</td>
 *       <td>current</td><td>CANCELLED</td></tr>
 *   <tr><td>invalid header field/name/value, method, path, status (2050-2055), switched protocols (2059),
 *       protocol error (2074), RST_STREAM received (2077), protocol switch failure (2084), unexpected HTTP version
 *       (2088)</td><td>current</td><td>PROTOCOL_VIOLATION</td></tr>
 *   <tr><td>request body stream errors: missing/invalid body stream (2056, 2057), outgoing length incorrect (2063),
 *       stream read failed (1063)</td><td>REQUEST_TRANSMISSION</td><td>IO</td></tr>
 *   <tr><td>any other code, including connection/socket/server closed (2058, 1051, 2070)</td><td>current</td>
 *       <td>IO</td></tr>
 * </table>
 *
 * <p>Non-CRT failures: an {@link HttpTransportException} is kept; a {@link ContentLengthMismatchException} (the
 * request body source ran short or long) is REQUEST_TRANSMISSION/IO; a {@link TimeoutException} (a per-exchange
 * acquisition deadline) is current/TIMEOUT; anything else is current/IO.
 */
public final class CrtFailureMapping {
    /** A classification: the phase to report and the kind of failure. */
    public record Classification(HttpPhase phase, Kind kind) {
        public HttpFailureCategory category() {
            return HttpFailureCategory.of(phase, kind);
        }
    }

    /** Sentinel phase meaning "the exchange's current phase" in {@link #TABLE}. */
    private static final HttpPhase CURRENT = null;

    private static final Map<Integer, Classification> TABLE = table();

    private CrtFailureMapping() {
    }

    private static Map<Integer, Classification> table() {
        Map<Integer, Classification> t = new java.util.HashMap<>();
        for (int code : new int[] {1029, 1030, 1031, 1032, 1033, 1034, 1035, 1036, 1037, 1068, 1071, 1072, 1173, 1178,
                                   1182, 1183, 1184, 1185, 1186, 1187, 1188, 1189, 1190, 1191, 1192, 1193, 1194}) {
            t.put(code, new Classification(HttpPhase.TLS_HANDSHAKE, Kind.IO));
        }
        t.put(1067, new Classification(HttpPhase.TLS_HANDSHAKE, Kind.TIMEOUT));
        t.put(1048, new Classification(HttpPhase.CONNECT, Kind.TIMEOUT));
        for (int code : new int[] {1045, 1047, 1049, 1050, 1055, 1057, 1058, 1059, 1060, 1061, 1195}) {
            t.put(code, new Classification(HttpPhase.CONNECT, Kind.IO));
        }
        for (int code : new int[] {2071, 2081, 2082, 2083}) {
            t.put(code, new Classification(HttpPhase.PROXY, Kind.IO));
        }
        t.put(2093, new Classification(HttpPhase.CONNECTION_ACQUISITION, Kind.TIMEOUT));
        for (int code : new int[] {2068, 2072, 2087, 2094}) {
            t.put(code, new Classification(HttpPhase.CONNECTION_ACQUISITION, Kind.IO));
        }
        t.put(2092, new Classification(HttpPhase.RESPONSE_CONSUMPTION, Kind.TIMEOUT));
        t.put(2073, new Classification(CURRENT, Kind.TIMEOUT));
        t.put(1042, new Classification(CURRENT, Kind.CANCELLED));
        t.put(2095, new Classification(CURRENT, Kind.CANCELLED));
        for (int code : new int[] {2050, 2051, 2052, 2053, 2054, 2055, 2059, 2074, 2077, 2084, 2088}) {
            t.put(code, new Classification(CURRENT, Kind.PROTOCOL_VIOLATION));
        }
        for (int code : new int[] {2056, 2057, 2063, 1063}) {
            t.put(code, new Classification(HttpPhase.REQUEST_TRANSMISSION, Kind.IO));
        }
        return java.util.Collections.unmodifiableMap(t);
    }

    /** The classification of CRT error {@code errorCode} reported during {@code current}. */
    public static Classification classify(int errorCode, HttpPhase current) {
        Objects.requireNonNull(current, "current");
        Classification c = TABLE.get(errorCode);
        if (c == null) {
            return new Classification(current, Kind.IO);
        }
        return c.phase() == CURRENT ? new Classification(current, c.kind()) : c;
    }

    /** The classification of {@code failure} reported during {@code current}. */
    public static Classification classify(Throwable failure, HttpPhase current) {
        Throwable f = unwrap(failure);
        if (f instanceof HttpTransportException t) {
            return new Classification(t.phase(), kindOf(t.category()));
        }
        if (f instanceof HttpException h) {
            return classify(h.getErrorCode(), current);
        }
        if (f instanceof CrtRuntimeException c) {
            return classify(c.errorCode, current);
        }
        if (f instanceof ContentLengthMismatchException) {
            return new Classification(HttpPhase.REQUEST_TRANSMISSION, Kind.IO);
        }
        if (f instanceof TimeoutException) {
            return new Classification(current, Kind.TIMEOUT);
        }
        return new Classification(current, Kind.IO);
    }

    /** {@code failure} as an {@link HttpTransportException} of {@code clientName}; an existing one is kept. */
    public static HttpTransportException toTransportException(Throwable failure, HttpPhase current,
                                                              String clientName) {
        Throwable f = unwrap(failure);
        if (f instanceof HttpTransportException t) {
            return t;
        }
        Classification c = classify(f, current);
        return HttpTransportException.builder().phase(c.phase()).category(c.category()).clientName(clientName)
                                     .message("HTTP transport failure (" + c.category() + " during " + c.phase()
                                              + ") in " + clientName + ": " + describe(f))
                                     .cause(f).build();
    }

    /** A failure of CRT error {@code errorCode}. */
    public static HttpTransportException fromErrorCode(int errorCode, HttpPhase current, String clientName) {
        return toTransportException(new HttpException(errorCode), current, clientName);
    }

    private static String describe(Throwable f) {
        if (f instanceof HttpException h) {
            return "CRT error " + h.getErrorCode() + " (" + h.getMessage() + ")";
        }
        return f.getClass().getSimpleName() + (f.getMessage() == null ? "" : ": " + f.getMessage());
    }

    private static Kind kindOf(HttpFailureCategory category) {
        return switch (category) {
            case CANCELLED -> Kind.CANCELLED;
            case TIMEOUT -> Kind.TIMEOUT;
            case PROTOCOL -> Kind.PROTOCOL_VIOLATION;
            default -> Kind.IO;
        };
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable f = Objects.requireNonNull(failure, "failure");
        while ((f instanceof CompletionException || f instanceof ExecutionException) && f.getCause() != null) {
            f = f.getCause();
        }
        return f;
    }
}
