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

package software.amazon.awssdk.protocols.client.rpcv2;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.codecs.cbor.CborSettings;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.core.client.protocol.ClientProtocolFactory;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;

/**
 * The client protocol for {@code smithy.protocols#rpcv2Cbor}: the
 * <a href="https://smithy.io/2.0/additional-specs/protocols/smithy-rpc-v2.html">Smithy RPC v2 protocol</a> with CBOR
 * payloads ({@code application/cbor}), serialized by {@link Rpcv2CborCodec} with {@link CborSettings#RPCV2}.
 *
 * <p>A request for operation {@code GetMetricData} of a service whose shape name is
 * {@code GraniteServiceVersion20100801} is sent as
 * <pre>
 * POST /service/GraniteServiceVersion20100801/operation/GetMetricData
 * smithy-protocol: rpc-v2-cbor
 * Accept: application/cbor
 * Content-Type: application/cbor
 * Content-Length: &lt;length&gt;
 *
 * &lt;the input as a CBOR map&gt;
 * </pre>
 * Error responses are resolved from the body's {@code __type} member.
 *
 * <p>Instances are created once per client and are safe to use from multiple threads. Create one with
 * {@link #RpcV2CborProtocol(ProtocolSettings)}, or through {@link Factory}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol}.
 */
@SdkPublicApi
@ThreadSafe
public final class RpcV2CborProtocol extends AbstractRpcV2ClientProtocol {
    private static final String PAYLOAD_MEDIA_TYPE = "application/cbor";
    private static final Rpcv2CborCodec CBOR_CODEC = Rpcv2CborCodec.builder().settings(CborSettings.RPCV2).build();

    /**
     * Creates the protocol for a service.
     *
     * @param settings the settings of the service. The service shape name forms the request path; the service schema,
     *     when set, tells whether the service is {@code aws.protocols#awsQueryCompatible}; and the base exception
     *     builder creates the exception for errors that do not resolve to a modeled error
     * @throws NullPointerException if {@code settings} is null
     */
    public RpcV2CborProtocol(ProtocolSettings settings) {
        super(Rpcv2CborTrait.ID, settings, CBOR_CODEC, PAYLOAD_MEDIA_TYPE);
    }

    @Override
    public String toString() {
        return "RpcV2CborProtocol(" + service() + ")";
    }

    /**
     * Creates {@link RpcV2CborProtocol}s. Generated clients use {@link #INSTANCE} directly; the factory is also
     * registered for {@link software.amazon.awssdk.core.client.protocol.ClientProtocolFactories#forId(ShapeId)}.
     */
    @SdkPublicApi
    @ThreadSafe
    public static final class Factory implements ClientProtocolFactory<Rpcv2CborTrait> {
        /**
         * The shared factory instance.
         */
        public static final Factory INSTANCE = new Factory();

        /**
         * Creates a factory. Used by {@link java.util.ServiceLoader}; prefer {@link #INSTANCE}.
         */
        public Factory() {
        }

        /**
         * Returns {@link Rpcv2CborTrait#ID}.
         *
         * @return {@code smithy.protocols#rpcv2Cbor}
         */
        @Override
        public ShapeId id() {
            return Rpcv2CborTrait.ID;
        }

        /**
         * Creates the protocol. The trait has no settings that change the protocol's behavior; its {@code http} and
         * {@code eventStreamHttp} lists are for the HTTP client.
         *
         * @param settings the settings of the service
         * @param protocolTrait the protocol trait; may be null
         * @return a new {@link RpcV2CborProtocol}
         * @throws NullPointerException if {@code settings} is null
         */
        @Override
        public RpcV2CborProtocol createProtocol(ProtocolSettings settings, Rpcv2CborTrait protocolTrait) {
            return new RpcV2CborProtocol(settings);
        }
    }
}
