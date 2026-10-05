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
 * Root package of the {@code software.amazon.awssdk:apache-client} V3 artifact.
 *
 * <p>Owns: The native sync Apache HttpClient 4 implementation:
 * {@link software.amazon.awssdk.http.apache.ApacheHttpClient} (V2 builder names), its {@link software.amazon.awssdk.http.apache.ProxyConfiguration} and TLS provider types, and
 * the {@link software.amazon.awssdk.http.apache.ApacheSdkHttpService} provider ({@code apache-client}, sync priority
 * 200: below {@code apache5-client} 300, above {@code url-connection-client} 100 and {@code aws-crt-client} 20).
 *
 * <p>Allowed V3 module dependencies: {@code http-spi}. Forbidden dependencies: async core.
 */
package software.amazon.awssdk.http.apache;
