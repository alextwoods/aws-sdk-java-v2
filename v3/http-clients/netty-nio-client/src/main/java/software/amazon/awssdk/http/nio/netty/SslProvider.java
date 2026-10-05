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

package software.amazon.awssdk.http.nio.netty;

/**
 * The TLS implementation (V2 {@code software.amazon.awssdk.http.nio.netty.SslProvider} constants).
 *
 * <p>{@link #OPENSSL} needs {@code io.netty:netty-tcnative-*} on the application class path; building a client with
 * it fails when OpenSSL is not available. The default is {@link #JDK}.
 */
public enum SslProvider {
    /** The JDK's {@code SSLEngine}; supports ALPN (HTTP/2) on every supported JDK. */
    JDK,
    /** Netty's OpenSSL engine (netty-tcnative), when available. */
    OPENSSL
}
