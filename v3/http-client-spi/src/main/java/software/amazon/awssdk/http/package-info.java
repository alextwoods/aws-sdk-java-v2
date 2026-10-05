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
 * Root package of the {@code software.amazon.awssdk:http-spi} V3 artifact.
 *
 * <p>Owns: Immutable HTTP messages, separate sync and async SPIs, streaming, cancellation, and configuration
 * capabilities.
 *
 * <p>Task 4.2 provides the minimal immutable message values the protocol runtimes use
 * ({@link software.amazon.awssdk.http.SdkHttpRequest}, {@link software.amazon.awssdk.http.SdkHttpResponse},
 * {@link software.amazon.awssdk.http.HttpHeaders}, {@link software.amazon.awssdk.http.HttpBody}). Task 4.3 adds the
 * streaming body contract ({@link software.amazon.awssdk.http.StreamingBody}: a blocking stream view and a
 * demand-driven {@link org.reactivestreams.Publisher} view with bounded chunks, one terminal signal, cancellation,
 * content-length enforcement and replay metadata) and its factories
 * ({@link software.amazon.awssdk.http.StreamingBodies}). Task 7.1 adds raw query entries and HTTP/2 pseudo-headers,
 * the separate sync ({@link software.amazon.awssdk.http.SdkSyncHttpClient},
 * {@link software.amazon.awssdk.http.SyncHttpExchange}) and async
 * ({@link software.amazon.awssdk.http.SdkAsyncHttpClient}, {@link software.amazon.awssdk.http.AsyncHttpExchange},
 * {@link software.amazon.awssdk.http.AsyncHttpResponseHandler}) SPIs with their ownership and terminal-uniqueness
 * contracts, {@link software.amazon.awssdk.http.HttpClientCapabilities capabilities} and
 * {@link software.amazon.awssdk.http.HttpClientOption options}, per-exchange
 * {@link software.amazon.awssdk.http.HttpExecutionOptions}, and stable
 * {@link software.amazon.awssdk.http.HttpFailureCategory failure categories} carried by
 * {@link software.amazon.awssdk.http.HttpTransportException}. Task 7.2 adds implementation metadata, deterministic
 * selection, option validation and construction rollback in {@code software.amazon.awssdk.http.provider}.
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}. Forbidden dependencies: every HTTP engine.
 */
package software.amazon.awssdk.http;
