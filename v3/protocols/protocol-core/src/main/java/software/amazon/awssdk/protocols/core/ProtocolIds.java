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

package software.amazon.awssdk.protocols.core;

import java.util.List;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Trait shape IDs of the seven launch protocols (Requirement 5.5) and the fixed precedence used when a service
 * declares more than one installed protocol and the caller names none.
 */
public final class ProtocolIds {
    /** {@code aws.protocols#awsJson1_0}. */
    public static final ShapeId AWS_JSON_1_0 = ShapeId.from("aws.protocols#awsJson1_0");
    /** {@code aws.protocols#awsJson1_1}. */
    public static final ShapeId AWS_JSON_1_1 = ShapeId.from("aws.protocols#awsJson1_1");
    /** {@code aws.protocols#restJson1}. */
    public static final ShapeId REST_JSON_1 = ShapeId.from("aws.protocols#restJson1");
    /** {@code aws.protocols#restXml}. */
    public static final ShapeId REST_XML = ShapeId.from("aws.protocols#restXml");
    /** {@code aws.protocols#awsQuery}. */
    public static final ShapeId AWS_QUERY = ShapeId.from("aws.protocols#awsQuery");
    /** {@code aws.protocols#ec2Query}. */
    public static final ShapeId EC2_QUERY = ShapeId.from("aws.protocols#ec2Query");
    /** {@code smithy.protocols#rpcv2Cbor}. */
    public static final ShapeId RPC_V2_CBOR = ShapeId.from("smithy.protocols#rpcv2Cbor");

    /**
     * The launch protocols, highest precedence first. Default selection picks the first entry that the service
     * declares and that is installed; a declared, installed protocol outside this list ranks after all of them, by
     * absolute shape ID. The order is fixed (never classpath-dependent) so that selection is deterministic; it is
     * intended to match the precedence other AWS SDKs apply to multi-protocol services.
     */
    public static final List<ShapeId> LAUNCH_PRECEDENCE = List.of(
        RPC_V2_CBOR, AWS_JSON_1_0, AWS_JSON_1_1, REST_JSON_1, REST_XML, AWS_QUERY, EC2_QUERY);

    private ProtocolIds() {
    }

    /** True if {@code protocolId} is one of the seven launch protocols. */
    public static boolean isLaunchProtocol(ShapeId protocolId) {
        return LAUNCH_PRECEDENCE.contains(protocolId);
    }
}
