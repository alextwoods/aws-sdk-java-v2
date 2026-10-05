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
 * Root package of the {@code software.amazon.awssdk:identity-providers} V3 artifact.
 *
 * <p>Owns: Credential and token providers, including profile, environment, container, web identity, SSO, role, and
 * instance-metadata providers (Requirements 13.3, 13.4, 13.16, 13.17, 13.19, 16.10), with the V2 class names:
 * {@link software.amazon.awssdk.identity.providers.DefaultCredentialsProvider} (sources in the Parity Manifest
 * precedence), {@link software.amazon.awssdk.identity.providers.AwsCredentialsProviderChain}, the system-property,
 * environment, profile, process, container, instance-profile, web-identity, STS assume-role and SSO credential
 * providers, the SSO and profile token providers, and the instance-metadata region provider.
 *
 * <p>Temporary identities are cached with the V2 stale/prefetch semantics and implement
 * {@link software.amazon.awssdk.identity.spi.RefreshableIdentityProvider}: a rejected identity is invalidated and
 * refreshed before the next attempt, and a failed required refresh fails with the refresh failure as its cause.
 * Blocking loads never run on the caller thread. Network providers use the HTTP SPI with a caller-supplied client or the
 * installed default implementation; STS and SSO calls are implemented natively because identity providers cannot
 * depend on generated service artifacts.
 *
 * <p>Allowed V3 module dependencies: {@code identity-spi}, {@code regions}, {@code http-spi}, {@code auth-api},
 * {@code auth-aws}. Forbidden dependencies: generated service artifacts, concrete HTTP engines.
 */
package software.amazon.awssdk.identity.providers;
