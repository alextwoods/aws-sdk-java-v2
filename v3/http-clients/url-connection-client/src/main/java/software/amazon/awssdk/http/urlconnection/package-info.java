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
 * Root package of the {@code software.amazon.awssdk:url-connection-client} V3 artifact.
 *
 * <p>Owns: The native sync JDK URLConnection implementation.
 *
 * <p>Allowed V3 module dependencies: {@code http-spi}. Forbidden dependencies: async core, any HTTP engine.
 *
 * <p>Entry points: {@link software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient} (V2-named builder) and
 * the {@link software.amazon.awssdk.http.urlconnection.UrlConnectionSdkHttpService} provider (priority 100). Limits:
 * options on {@code UrlConnectionHttpClient}, header/target/method fidelity on {@code UrlConnectionRequestPlan},
 * stream-close and abort on {@code UrlConnectionExchange}, failure categories on
 * {@link software.amazon.awssdk.http.urlconnection.UrlConnectionFailures}.
 */
package software.amazon.awssdk.http.urlconnection;
