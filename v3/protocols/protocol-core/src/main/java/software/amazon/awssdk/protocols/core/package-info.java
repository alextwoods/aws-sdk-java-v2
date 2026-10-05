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
 * Root package of the {@code software.amazon.awssdk:protocol-core} V3 artifact: protocol selection and the contracts
 * shared by the schema-driven protocol runtimes (Requirement 5).
 *
 * <ul>
 *   <li>{@link software.amazon.awssdk.protocols.core.ServiceProtocols} is the generated, build-time declaration of
 *       the protocols a service's canonical model applies ({@code <Service>ServiceMetadata.serviceProtocols()}),
 *       including the protocol options that live in AWS protocol traits, which are not in runtime schemas.</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.ProtocolProvider} is the SPI a protocol module implements;
 *       {@link software.amazon.awssdk.protocols.core.InstalledProtocols} holds the installed providers.</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.ProtocolSelector} selects exactly one declared, installed
 *       protocol at client construction or rejects the selection with a
 *       {@link software.amazon.awssdk.protocols.core.ProtocolSelectionException} (Requirements 5.6, 5.12).</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.SdkProtocolException} is the located wire-input error of every
 *       protocol runtime (Requirements 5.9, 5.10).</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.binding} derives immutable HTTP binding plans from schema
 *       traits.</li>
 *   <li>The Schema SERDE contract (task 4.2):
 *       {@link software.amazon.awssdk.protocols.core.ProtocolProvider#createClientProtocol} creates one
 *       {@link software.amazon.awssdk.protocols.core.ClientProtocol} per client from a
 *       {@link software.amazon.awssdk.protocols.core.ProtocolContext}; it yields one precomputed
 *       {@link software.amazon.awssdk.protocols.core.OperationProtocol} per operation, which serializes requests and
 *       deserializes responses and errors to and from the {@code http-spi} message values.</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.serde} and {@link software.amazon.awssdk.protocols.core.error}
 *       hold the helpers every protocol runtime shares.</li>
 * </ul>
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}, {@code smithy-runtime}, {@code http-spi}, {@code aws-core}.
 * Forbidden dependencies: generated service types.
 */
package software.amazon.awssdk.protocols.core;
