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

package software.amazon.awssdk.awscore.client.builder;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.utils.Validate;

/**
 * Runtime protocol selection for the builders of clients generated with schema-based serialization (design A.11).
 *
 * <p>Generated builders set {@link SdkClientOption#CLIENT_PROTOCOL_FACTORY} to the factory of the service's default
 * protocol, resolve {@link SdkClientOption#CLIENT_PROTOCOL} to the configured override or, when none is configured, to
 * the protocol {@link #createProtocol(ClientProtocolFactory, ProtocolSettings)} creates, and call
 * {@link #validateMessageExchange(SdkClientConfiguration)} when the client is built.
 */
@SdkProtectedApi
public final class ClientProtocolSupport {
    private ClientProtocolSupport() {
    }

    /**
     * Creates a protocol with a factory. The factory receives the protocol definition trait whose ID is the factory's
     * {@link ClientProtocolFactory#id()}, read from the service schema of the settings, or null when the service schema
     * does not carry that trait or the settings have no service schema.
     *
     * @param factory the protocol factory
     * @param settings the settings of the service
     * @return the protocol the factory created
     * @throws NullPointerException if {@code factory} or {@code settings} is null
     * @throws IllegalStateException if the factory returns null
     */
    public static ClientProtocol<?, ?> createProtocol(ClientProtocolFactory<?> factory, ProtocolSettings settings) {
        Validate.paramNotNull(factory, "factory");
        Validate.paramNotNull(settings, "settings");
        @SuppressWarnings("unchecked")
        ClientProtocolFactory<Trait> typedFactory = (ClientProtocolFactory<Trait>) factory;
        ClientProtocol<?, ?> protocol = typedFactory.createProtocol(settings, protocolTrait(settings.serviceSchema(),
                                                                                            factory.id()));
        if (protocol == null) {
            throw new IllegalStateException(factory + " created a null protocol for " + settings.service());
        }
        return protocol;
    }

    /**
     * Checks that the client's protocol and transport exchange the same message types. The transport is
     * {@link SdkClientOption#CLIENT_TRANSPORT} for a synchronous client and {@link SdkClientOption#ASYNC_CLIENT_TRANSPORT}
     * for an asynchronous one ({@link SdkClientOption#CLIENT_TYPE}); when it is not set, the client sends requests through
     * the SDK HTTP client transport, whose exchange is {@link HttpMessageExchange#INSTANCE}. Nothing is checked when the
     * configuration has no {@link SdkClientOption#CLIENT_PROTOCOL}.
     *
     * @param config the finalized client configuration
     * @throws IllegalStateException if the message exchanges differ; the message names the protocol's and the
     *     transport's request and response types
     */
    public static void validateMessageExchange(SdkClientConfiguration config) {
        ClientProtocol<?, ?> protocol = config.option(SdkClientOption.CLIENT_PROTOCOL);
        if (protocol == null) {
            return;
        }
        Object transport;
        MessageExchange<?, ?> transportExchange;
        if (config.option(SdkClientOption.CLIENT_TYPE) == ClientType.ASYNC) {
            AsyncClientTransport<?, ?> asyncTransport = config.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT);
            transport = asyncTransport;
            transportExchange = asyncTransport == null ? HttpMessageExchange.INSTANCE : asyncTransport.messageExchange();
        } else {
            ClientTransport<?, ?> syncTransport = config.option(SdkClientOption.CLIENT_TRANSPORT);
            transport = syncTransport;
            transportExchange = syncTransport == null ? HttpMessageExchange.INSTANCE : syncTransport.messageExchange();
        }
        MessageExchange<?, ?> protocolExchange = protocol.messageExchange();
        if (protocolExchange == null || !protocolExchange.equals(transportExchange)) {
            throw new IllegalStateException(String.format(
                "The client protocol and the client transport exchange different message types. Protocol %s exchanges "
                + "%s; transport %s exchanges %s.",
                protocol, protocolExchange, transport == null ? "(the SDK HTTP client transport)" : transport,
                transportExchange));
        }
    }

    private static Trait protocolTrait(Schema serviceSchema, ShapeId protocolId) {
        if (serviceSchema == null || protocolId == null) {
            return null;
        }
        for (Trait trait : serviceSchema.traits()) {
            if (protocolId.equals(trait.traitId())) {
                return trait;
            }
        }
        return null;
    }
}
