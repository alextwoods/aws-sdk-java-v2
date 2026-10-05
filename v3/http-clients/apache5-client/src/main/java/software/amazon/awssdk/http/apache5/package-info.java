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
 * Root package of the {@code software.amazon.awssdk:apache5-client} V3 artifact.
 *
 * <p>Owns: The native sync Apache HttpClient 5 implementation ({@link software.amazon.awssdk.http.apache5.Apache5HttpClient},
 * registered provider {@link software.amazon.awssdk.http.apache5.Apache5SdkHttpService}) and the isolated async
 * implementation ({@link software.amazon.awssdk.http.apache5.Apache5AsyncHttpClient}), whose provider
 * {@link software.amazon.awssdk.http.apache5.Apache5SdkAsyncHttpService} is not registered for discovery until the
 * Parity Manifest enables it.
 *
 * <p>Allowed V3 module dependencies: {@code http-spi}. Forbidden dependencies: async core.
 */
package software.amazon.awssdk.http.apache5;
