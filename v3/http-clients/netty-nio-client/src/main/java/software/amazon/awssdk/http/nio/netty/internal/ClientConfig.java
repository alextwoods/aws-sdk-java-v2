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

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import java.time.Duration;
import software.amazon.awssdk.http.Protocol;

/**
 * The resolved configuration of one client. {@code connectionTimeToLive} and {@code connectionMaxIdleTime} are
 * {@code null} when unbounded; {@code sslContext} is shared by every TLS connection.
 */
public record ClientConfig(String clientName,
                           EventLoopGroup group,
                           int maxConcurrency,
                           int maxPendingConnectionAcquires,
                           Duration connectionAcquisitionTimeout,
                           Duration connectionTimeout,
                           Duration readTimeout,
                           Duration writeTimeout,
                           Duration tlsNegotiationTimeout,
                           Duration connectionTimeToLive,
                           Duration connectionMaxIdleTime,
                           boolean tcpKeepAlive,
                           Protocol protocol,
                           int http2MaxStreams,
                           int http2InitialWindowSize,
                           SslContext sslContext) {
}
