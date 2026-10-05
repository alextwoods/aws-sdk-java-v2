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

package software.amazon.awssdk.http.tck;

/**
 * A documented transport-specific exception to a shared TCK check. A {@link TransportUnderTest} lists the deviations
 * its implementation documents (in its own javadoc and status notes); the shared checks never weaken their
 * assertions: a check that a deviation affects is either skipped with the deviation's reason, or (where the
 * transport documents an alternative behavior) asserts that documented behavior instead.
 */
enum Deviation {

    /**
     * URLConnection: an early-closed response whose unreceived remainder is at most {@code http.KeepAlive.remainingData}
     * (512 KB) is handed to the JDK keep-alive cleaner, which may drain it and reuse the connection (JDK internals, no
     * public hook). The "early close discards the connection" check is skipped.
     */
    JDK_KEEP_ALIVE_CLEANER_MAY_REUSE_EARLY_CLOSED_CONNECTION(
        "the JDK keep-alive cleaner may drain an early-closed remainder <= 512 KB and reuse the connection"),

    /**
     * URLConnection: {@code abort()} cannot interrupt a response-body read blocked inside the JDK (the read is
     * released when data arrives or the socket timeout elapses). The "abort releases a blocked read" check is
     * skipped; aborting between reads is still checked.
     */
    ABORT_CANNOT_INTERRUPT_BLOCKED_BODY_READ(
        "abort() cannot interrupt a response-body read blocked inside the JDK (released by data or socketTimeout)"),

    /**
     * CRT: request bodies of unknown length (no Content-Length) are not supported and fail fast with category
     * REQUEST_TRANSMISSION; the unknown-length check asserts that fast failure instead of a streamed upload.
     */
    UNKNOWN_LENGTH_REQUEST_BODY_FAILS_FAST("request bodies of unknown length fail fast with REQUEST_TRANSMISSION"),

    /**
     * CRT: the test certificate is trusted through the CRT-specific {@code tlsTrustedCertificatesPem} builder option,
     * not the (undeclared) {@code TLS_TRUST_MANAGERS} option, so the TLS round trip runs through that option.
     */
    TEST_TRUST_BY_PEM("the test certificate is trusted via tlsTrustedCertificatesPem, not TLS_TRUST_MANAGERS"),

    /**
     * Apache 5 (TCK finding, open): when a host resolves to several addresses, the engine's connect loop tries the
     * next address after a TLS handshake failure and reports only the last failure, so an untrusted certificate on
     * 127.0.0.1 followed by a refused ::1 surfaces as CONNECT (retryable) instead of TLS. The single-address TLS
     * check still runs; the multi-address variant is skipped.
     */
    TLS_FAILURE_MASKED_BY_A_LATER_ADDRESS(
        "(TCK finding, open) a TLS handshake failure on one address is replaced by the last address's connect failure"),

    /**
     * CRT (TCK finding, not yet in the transport's own documentation): the connection is returned to the pool as soon
     * as the native stream has received the complete response (which fits in the stream window), before the caller
     * has read it, so a later {@code abort()} / early close no longer discards it. Nothing beyond the declared bound is
     * ever drained, but this differs from the SPI rule that abort discards the connection. The "abort after the
     * response discards the connection" check is skipped until the transport is changed or the rule is relaxed.
     */
    RELEASES_FULLY_RECEIVED_RESPONSE_FOR_REUSE(
        "(TCK finding, open) a completely received response returns its connection to the pool before the caller"
        + " reads it, so abort()/early close after that does not discard it");

    private final String reason;

    Deviation(String reason) {
        this.reason = reason;
    }

    String reason() {
        return reason;
    }
}
