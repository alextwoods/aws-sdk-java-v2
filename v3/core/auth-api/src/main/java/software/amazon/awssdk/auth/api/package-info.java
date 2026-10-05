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
 * Root package of the {@code software.amazon.awssdk:auth-api} V3 artifact.
 *
 * <p>Owns: Auth scheme, signer, and auth option contracts.
 *
 * <p>Modern Auth (Requirements 13.1, 13.2): {@link software.amazon.awssdk.auth.api.AuthScheme},
 * {@link software.amazon.awssdk.auth.api.HttpSigner}, {@link software.amazon.awssdk.auth.api.AuthSchemeOption}, the
 * modeled effective-scheme computation {@link software.amazon.awssdk.auth.api.ModeledAuth}, per-call selection
 * {@link software.amazon.awssdk.auth.api.AuthSchemeSelector} and the anonymous
 * {@link software.amazon.awssdk.auth.api.NoAuthAuthScheme}. There is no legacy {@code Signer} contract.
 *
 * <p>Allowed V3 module dependencies: {@code identity-spi}, {@code http-spi} (signers sign
 * {@code SdkHttpRequest}s). Forbidden dependencies: generated service artifacts.
 */
package software.amazon.awssdk.auth.api;
