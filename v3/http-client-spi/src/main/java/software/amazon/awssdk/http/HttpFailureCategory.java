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

import java.util.Objects;

/**
 * The stable, provider-independent category of a transport failure (Requirement 12.7; design, "HTTP SPI"). Every
 * transport maps its engine-specific failure to one category through {@link #of(HttpPhase, Kind)}, so a retry
 * classifier, metric or test sees the same category for the same phase and kind whichever transport is used.
 *
 * <table>
 *   <caption>Category by phase and kind</caption>
 *   <tr><th>Kind</th><th>Phase</th><th>Category</th></tr>
 *   <tr><td>{@link Kind#CANCELLED}</td><td>any</td><td>{@link #CANCELLED}</td></tr>
 *   <tr><td>{@link Kind#TIMEOUT}</td><td>any (kept by {@link HttpTransportException#phase()})</td>
 *       <td>{@link #TIMEOUT}</td></tr>
 *   <tr><td>{@link Kind#PROTOCOL_VIOLATION}</td><td>any</td><td>{@link #PROTOCOL}</td></tr>
 *   <tr><td>{@link Kind#IO}</td><td>{@link HttpPhase#CONNECTION_ACQUISITION}</td>
 *       <td>{@link #CONNECTION_ACQUISITION}</td></tr>
 *   <tr><td>{@link Kind#IO}</td><td>{@link HttpPhase#CONNECT}</td><td>{@link #CONNECT}</td></tr>
 *   <tr><td>{@link Kind#IO}</td><td>{@link HttpPhase#PROXY}</td><td>{@link #PROXY}</td></tr>
 *   <tr><td>{@link Kind#IO}</td><td>{@link HttpPhase#TLS_HANDSHAKE}</td><td>{@link #TLS}</td></tr>
 *   <tr><td>{@link Kind#IO}</td><td>{@link HttpPhase#REQUEST_TRANSMISSION}</td>
 *       <td>{@link #REQUEST_TRANSMISSION}</td></tr>
 *   <tr><td>{@link Kind#IO}</td><td>{@link HttpPhase#RESPONSE_CONSUMPTION}</td>
 *       <td>{@link #RESPONSE_CONSUMPTION}</td></tr>
 * </table>
 *
 * <p>{@link #transientByDefault()} is the transport's hint to the retry classifier (task 8.x owns the decision): a
 * failure that another attempt, typically on another connection, can plausibly avoid.
 */
public enum HttpFailureCategory {
    /** No pooled connection or stream slot could be acquired (pool exhausted or closed). */
    CONNECTION_ACQUISITION(true),
    /** The connection could not be established: DNS, refused, unreachable, reset during connect. */
    CONNECT(true),
    /** The proxy refused, failed or could not be authenticated to. */
    PROXY(false),
    /** The TLS handshake or certificate validation failed. */
    TLS(false),
    /** A configured transport timeout elapsed; {@link HttpTransportException#phase()} names which. */
    TIMEOUT(true),
    /** The exchange was cancelled or aborted by its caller or by closing the client. */
    CANCELLED(false),
    /** Writing the request failed, for example the connection was reset or the body source failed. */
    REQUEST_TRANSMISSION(true),
    /** Reading the response failed, for example the connection closed before the declared length. */
    RESPONSE_CONSUMPTION(true),
    /** The peer violated HTTP framing: a malformed status line or header, an HTTP/2 protocol error. */
    PROTOCOL(false);

    private final boolean transientByDefault;

    HttpFailureCategory(boolean transientByDefault) {
        this.transientByDefault = transientByDefault;
    }

    /** What went wrong, independent of the engine's exception types. */
    public enum Kind {
        /** An I/O or engine error. */
        IO,
        /** A configured timeout elapsed. */
        TIMEOUT,
        /** The caller cancelled or aborted, or the client closed. */
        CANCELLED,
        /** The peer's bytes are not valid HTTP. */
        PROTOCOL_VIOLATION
    }

    /** The category of a failure of {@code kind} in {@code phase}; see the table above. */
    public static HttpFailureCategory of(HttpPhase phase, Kind kind) {
        Objects.requireNonNull(phase, "phase");
        return switch (Objects.requireNonNull(kind, "kind")) {
            case CANCELLED -> CANCELLED;
            case TIMEOUT -> TIMEOUT;
            case PROTOCOL_VIOLATION -> PROTOCOL;
            case IO -> switch (phase) {
                case CONNECTION_ACQUISITION -> CONNECTION_ACQUISITION;
                case CONNECT -> CONNECT;
                case PROXY -> PROXY;
                case TLS_HANDSHAKE -> TLS;
                case REQUEST_TRANSMISSION -> REQUEST_TRANSMISSION;
                case RESPONSE_CONSUMPTION -> RESPONSE_CONSUMPTION;
            };
        };
    }

    /** True if another attempt can plausibly succeed; a hint for the retry classifier. */
    public boolean transientByDefault() {
        return transientByDefault;
    }
}
