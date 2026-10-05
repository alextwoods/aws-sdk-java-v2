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

/**
 * The phase of an exchange in which a transport failure happened, provider independent (Requirement 12.7). Together
 * with the kind of failure it determines the {@link HttpFailureCategory} ({@link HttpFailureCategory#of}).
 */
public enum HttpPhase {
    /** Waiting for a pooled connection or a stream slot. */
    CONNECTION_ACQUISITION,
    /** Opening the TCP connection (DNS resolution included). */
    CONNECT,
    /** Establishing a tunnel through, or authenticating to, a proxy. */
    PROXY,
    /** The TLS handshake, including certificate validation. */
    TLS_HANDSHAKE,
    /** Writing the request line or pseudo-headers, headers and body. */
    REQUEST_TRANSMISSION,
    /** Waiting for and reading the response status, headers and body. */
    RESPONSE_CONSUMPTION
}
