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

package software.amazon.awssdk.codegen.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Resolves the protocol from the service model {@code protocol} and {@code protocols} fields.
 */
public final class ProtocolUtils {

    /**
     * The protocol definition traits a schema-based client can use, in default-selection priority order
     * (Requirement 12.3).
     */
    public static final List<ShapeId> CLIENT_PROTOCOL_PRIORITY = Collections.unmodifiableList(Arrays.asList(
        ShapeId.from("smithy.protocols#rpcv2Cbor"),
        ShapeId.from("aws.protocols#awsJson1_0"),
        ShapeId.from("aws.protocols#awsJson1_1"),
        ShapeId.from("aws.protocols#restJson1"),
        ShapeId.from("aws.protocols#restXml"),
        ShapeId.from("aws.protocols#awsQuery"),
        ShapeId.from("aws.protocols#ec2Query")));

    /**
     * Priority-ordered list of protocols supported by the SDK.
     */
    private static final List<String> SUPPORTED_PROTOCOLS = Arrays.asList(
        "smithy-rpc-v2-cbor", "json", "rest-json", "rest-xml", "query", "ec2");

    /**
     * Maps a Smithy protocol-trait shape id to the SDK protocol string. {@code awsJson1_0} and
     * {@code awsJson1_1} both map to {@code json}; the JSON version is resolved separately at the
     * metadata layer.
     */
    private static final Map<String, String> SMITHY_PROTOCOL_TRAITS = new HashMap<>();

    static {
        SMITHY_PROTOCOL_TRAITS.put("smithy.protocols#rpcv2Cbor", "smithy-rpc-v2-cbor");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#awsJson1_0", "json");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#awsJson1_1", "json");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#restJson1", "rest-json");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#restXml", "rest-xml");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#awsQuery", "query");
        SMITHY_PROTOCOL_TRAITS.put("aws.protocols#ec2Query", "ec2");
    }

    private ProtocolUtils() {
    }

    /**
     * {@code protocols} supersedes {@code protocol}. The highest priority protocol supported by the SDK that is present in the
     * service model {@code protocols} list will be selected. If none of the values in {@code protocols} is supported by the
     * SDK, an error will be thrown. If {@code protocols} is empty or null, the value from {@code protocol} will be returned.
     */
    public static String resolveProtocol(ServiceMetadata serviceMetadata) {

        List<String> protocols = serviceMetadata.getProtocols();
        String protocol = serviceMetadata.getProtocol();

        if (protocols == null || protocols.isEmpty()) {
            return protocol;
        }

        // Kinesis uses customization.config customServiceMetadata to set cbor
        if ("cbor".equals(protocols.get(0))) {
            return "cbor";
        }

        for (String supportedProtocol : SUPPORTED_PROTOCOLS) {
            if (protocols.contains(supportedProtocol)) {
                return supportedProtocol;
            }
        }

        throw new IllegalArgumentException("The SDK does not support any of provided protocols: " + protocols);
    }

    /**
     * Smithy equivalent of {@link #resolveProtocol(ServiceMetadata)}: maps the service's applied
     * protocol traits to SDK strings and picks the highest priority from {@link #SUPPORTED_PROTOCOLS}.
     * A service may apply several (e.g. SQS has both {@code awsQuery} and {@code awsJson1_0}); the
     * priority order matches the C2J {@code protocols}-list resolution.
     *
     * @throws IllegalArgumentException if no applied protocol trait is supported.
     */
    public static String resolveProtocol(ServiceIndex serviceIndex, ServiceShape service) {
        // CLIENT_PROTOCOL_PRIORITY maps, in order, to the SUPPORTED_PROTOCOLS order, so the client protocol trait
        // selects the same SDK protocol string as a priority scan over the mapped strings.
        return SMITHY_PROTOCOL_TRAITS.get(resolveDefaultClientProtocol(serviceIndex, service).toString());
    }

    /**
     * Selects the default client protocol of a Smithy service (Requirement 12.3): the first protocol definition trait
     * applied to the service in {@link #CLIENT_PROTOCOL_PRIORITY} order.
     *
     * @return the shape ID of the selected protocol definition trait
     * @throws IllegalArgumentException {@code "Service <id> declares no supported protocol; found: [<trait ids>]"} if
     *     the service applies none of the supported protocol traits (Requirement 12.4)
     */
    public static ShapeId resolveDefaultClientProtocol(ServiceIndex serviceIndex, ServiceShape service) {
        Set<ShapeId> applied = serviceIndex.getProtocols(service).keySet();
        for (ShapeId protocol : CLIENT_PROTOCOL_PRIORITY) {
            if (applied.contains(protocol)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Service " + service.getId() + " declares no supported protocol; found: "
                                           + new TreeSet<>(applied));
    }
}
