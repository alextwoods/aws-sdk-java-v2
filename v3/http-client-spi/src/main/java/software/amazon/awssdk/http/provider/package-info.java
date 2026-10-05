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
 * HTTP implementation metadata, deterministic selection, capability validation and construction rollback (task 7.2;
 * Requirements 12.11-12.14).
 *
 * <p>An HTTP client module provides an {@link software.amazon.awssdk.http.provider.SdkHttpService} (sync) or
 * {@link software.amazon.awssdk.http.provider.SdkAsyncHttpService} (async) declaring a stable name, a numeric
 * priority and its {@link software.amazon.awssdk.http.HttpClientCapabilities}.
 * {@link software.amazon.awssdk.http.provider.HttpClientResolver} applies the documented order (explicit client,
 * then explicit implementation, then the highest-priority installed implementation of the mode, a top-priority tie
 * failing), checks every configured option against the chosen implementation before it acquires anything, and rolls
 * back the SDK-owned resources registered with the
 * {@link software.amazon.awssdk.http.provider.HttpClientConstruction} if construction fails.
 * {@link software.amazon.awssdk.http.provider.HttpImplementationDiscovery#discover} is the SPI's only service
 * discovery.
 */
package software.amazon.awssdk.http.provider;
