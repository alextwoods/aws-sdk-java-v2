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
 * Helpers shared by the schema-driven protocol runtimes (task 4.2). A protocol runtime implements
 * {@link software.amazon.awssdk.protocols.core.ClientProtocol} and
 * {@link software.amazon.awssdk.protocols.core.OperationProtocol} and composes these:
 * <ul>
 *   <li>{@link software.amazon.awssdk.protocols.core.serde.LocatingDeserializer}: read any codec's
 *       {@code ShapeDeserializer} into a builder with member-path tracking, located
 *       {@link software.amazon.awssdk.protocols.core.SdkProtocolException}s without wire values, atomic build, and an
 *       optional timestamp-reading override;</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.serde.TimestampFormats}: per-location default formats, exact
 *       epoch-seconds reading ({@code EXACT_EPOCH_SECONDS}), text formatting and parsing;</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.serde.RequestTargets}: endpoint + operation path joining and
 *       {@code smithy.api#endpoint} host prefixes;</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.serde.HttpBindingWriter}: labels, query literals,
 *       {@code httpQuery}/{@code httpQueryParams}, {@code httpHeader}/{@code httpPrefixHeaders} serialization from an
 *       {@link software.amazon.awssdk.protocols.core.binding.HttpOperationBindings} plan, and body-members and
 *       payload-member views for the codec;</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.serde.HttpBindingReader}: the response side, reading
 *       {@code httpHeader}, {@code httpPrefixHeaders}, {@code httpResponseCode}, the {@code httpPayload} member and
 *       the body members (through a codec-supplied body deserializer factory) into an output or error builder;</li>
 *   <li>{@link software.amazon.awssdk.protocols.core.serde.WireContext}: the protocol/operation location of
 *       failures.</li>
 * </ul>
 *
 * <p>Extension points for the remaining launch protocols: {@code restJson1}/{@code restXml} build requests with
 * {@code HttpBindingWriter} plus their codec over {@code bodyMembers}/{@code payloadMember}, and read responses and
 * errors with {@code HttpBindingReader} plus their codec;
 * {@code awsQuery}/{@code ec2Query} need a form-encoding {@code ShapeSerializer} and XML response unwrapping;
 * {@code rpcv2Cbor} uses
 * {@code RequestTargets.start(endpoint, "POST", "/service/<Service>/operation/<Operation>")}, the
 * {@code smithy-protocol: rpc-v2-cbor} header and Smithy Java's CBOR codec. Streaming blob payloads go through
 * {@code StreamingPayloads} without buffering; event streams go through
 * {@code software.amazon.awssdk.protocols.core.eventstream} (every protocol except the query protocols, which
 * reject them with {@link UnsupportedOperationException}).
 */
package software.amazon.awssdk.protocols.core.serde;
