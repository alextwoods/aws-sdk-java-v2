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

/**
 * Root package of the {@code software.amazon.awssdk:aws-crt-client} V3 artifact.
 *
 * <p>Owns: The native sync and async AWS CRT implementations.
 *
 * <p>Allowed V3 module dependencies: {@code http-spi}. Forbidden dependencies: generated service artifacts.
 *
 * <p>Two separate implementations, each with its own provider and service registration:
 * <ul>
 *   <li>{@link software.amazon.awssdk.http.crt.AwsCrtHttpClient} (sync), provider
 *       {@link software.amazon.awssdk.http.crt.AwsCrtSdkHttpService} ({@code aws-crt-client-sync});</li>
 *   <li>{@link software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient} (async), provider
 *       {@link software.amazon.awssdk.http.crt.AwsCrtSdkAsyncHttpService} ({@code aws-crt-client-async}).</li>
 * </ul>
 *
 * <p>Default-selection priority: 20 in both modes, below the other default transports of each mode (V2 did not pick
 * the CRT client by default when Apache or Netty was present). Explicitly configured clients or implementations
 * always win.
 *
 * <p>Buffer bound (Requirement 12.6): {@code readBufferSizeInBytes} (default 16 MiB), the CRT's manual per-stream
 * read window, reopened only by bytes the consumer has read (sync) or been delivered (async). Request bodies are
 * pulled one chunk at a time. Cancellation timeout: 2 seconds. HTTP/1.1 only; HTTP/2 is not declared.
 */
package software.amazon.awssdk.http.crt;
