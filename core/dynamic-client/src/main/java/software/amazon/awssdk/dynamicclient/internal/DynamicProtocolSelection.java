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

package software.amazon.awssdk.dynamicclient.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.builder.ClientProtocolSupport;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson11Protocol;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.protocols.client.awsquery.Ec2QueryClientProtocol;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.protocols.client.restxml.RestXmlClientProtocol;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Selects the protocol of a dynamic client from the protocol traits of its service (Requirements 27.1 and 27.2).
 *
 * <p>The protocols are tried in the SDK's fixed priority order (Requirement 12.3, the same list as codegen's
 * {@code ProtocolUtils.CLIENT_PROTOCOL_PRIORITY}): {@code rpcv2Cbor}, {@code awsJson1_0}, {@code awsJson1_1},
 * {@code restJson1}, {@code restXml}, {@code awsQuery}, {@code ec2Query}. The factories are referenced directly, as in
 * generated builders, so neither the order of the traits in the model nor the order in which a {@code ServiceLoader}
 * discovers factories affects the result. Every protocol of the list is available to a dynamic client: the
 * {@code stagedClientProtocols} switch of codegen only keeps generated clients on their legacy marshallers, and a
 * dynamic client has none.
 */
@SdkInternalApi
public final class DynamicProtocolSelection {
    private static final Map<ShapeId, ClientProtocolFactory<?>> FACTORIES_BY_PRIORITY;

    static {
        Map<ShapeId, ClientProtocolFactory<?>> factories = new LinkedHashMap<>();
        factories.put(ShapeId.from("smithy.protocols#rpcv2Cbor"), RpcV2CborProtocol.Factory.INSTANCE);
        factories.put(ShapeId.from("aws.protocols#awsJson1_0"), AwsJson1Protocol.Factory.INSTANCE);
        factories.put(ShapeId.from("aws.protocols#awsJson1_1"), AwsJson11Protocol.Factory.INSTANCE);
        factories.put(ShapeId.from("aws.protocols#restJson1"), RestJsonClientProtocol.Factory.INSTANCE);
        factories.put(ShapeId.from("aws.protocols#restXml"), RestXmlClientProtocol.Factory.INSTANCE);
        factories.put(ShapeId.from("aws.protocols#awsQuery"), AwsQueryClientProtocol.Factory.INSTANCE);
        factories.put(ShapeId.from("aws.protocols#ec2Query"), Ec2QueryClientProtocol.Factory.INSTANCE);
        FACTORIES_BY_PRIORITY = Collections.unmodifiableMap(factories);
    }

    private final ServiceShape service;
    private final Set<ShapeId> protocolTraits;
    private final List<ClientProtocolFactory<?>> candidates;

    private DynamicProtocolSelection(ServiceShape service, Set<ShapeId> protocolTraits,
                                     List<ClientProtocolFactory<?>> candidates) {
        this.service = service;
        this.protocolTraits = protocolTraits;
        this.candidates = candidates;
    }

    /**
     * Finds the protocols a service can be called with.
     *
     * @param model the model
     * @param service the service shape of the model
     * @return the selection
     */
    public static DynamicProtocolSelection forService(Model model, ServiceShape service) {
        Validate.paramNotNull(model, "model");
        Validate.paramNotNull(service, "service");
        Set<ShapeId> applied = new TreeSet<>(ServiceIndex.of(model).getProtocols(service).keySet());
        List<ClientProtocolFactory<?>> candidates = new ArrayList<>();
        FACTORIES_BY_PRIORITY.forEach((id, factory) -> {
            if (applied.contains(id)) {
                candidates.add(factory);
            }
        });
        return new DynamicProtocolSelection(service, Collections.unmodifiableSet(applied),
                                            Collections.unmodifiableList(candidates));
    }

    /**
     * Returns the protocol definition trait IDs of the SDK's protocols, in priority order.
     *
     * @return the protocol IDs
     */
    public static List<ShapeId> priority() {
        return Collections.unmodifiableList(new ArrayList<>(FACTORIES_BY_PRIORITY.keySet()));
    }

    /**
     * Returns the factories of the protocols whose trait the service carries, in priority order.
     *
     * @return the candidate factories; empty when the service carries none of the SDK's protocol traits
     */
    public List<ClientProtocolFactory<?>> candidates() {
        return candidates;
    }

    /**
     * Creates the protocol of a client: the first candidate, in priority order, whose message exchange is the
     * transport's. When no candidate exchanges the transport's messages, the first candidate is returned, so that the
     * build fails on the protocol and transport mismatch and names both (Requirement 27.8).
     *
     * @param settings the protocol settings of the service
     * @param transportExchange the message exchange of the client's transport
     * @return the protocol, or null when the service carries none of the SDK's protocol traits
     */
    public ClientProtocol<?, ?> select(ProtocolSettings settings, MessageExchange<?, ?> transportExchange) {
        ClientProtocol<?, ?> first = null;
        for (ClientProtocolFactory<?> factory : candidates) {
            ClientProtocol<?, ?> protocol = ClientProtocolSupport.createProtocol(factory, settings);
            if (transportExchange.equals(protocol.messageExchange())) {
                return protocol;
            }
            if (first == null) {
                first = protocol;
            }
        }
        return first;
    }

    /**
     * Returns the build error of a client whose service carries none of the SDK's protocol traits and that has no
     * configured protocol (Requirement 27.2).
     *
     * @return the exception to throw
     */
    public IllegalStateException noSupportedProtocol() {
        String found = protocolTraits.isEmpty() ? "none found" : protocolTraits.toString();
        return new IllegalStateException("Service " + service.getId() + " declares no protocol that the dynamic client "
                                         + "supports. Protocol traits on the service: " + found
                                         + ". Supported protocols: " + FACTORIES_BY_PRIORITY.keySet()
                                         + ". Set a protocol with DynamicClient.Builder.protocol(...).");
    }
}
