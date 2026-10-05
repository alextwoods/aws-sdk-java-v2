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

package software.amazon.awssdk.protocols.rpcv2cbor;

import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.cbor.Rpcv2CborCodec;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The per-client Smithy RPC v2 CBOR runtime. Wire rules ({@code smithy.protocols#rpcv2Cbor}):
 * <ul>
 *   <li>every request is {@code POST <base path>/service/<service shape name>/operation/<operation shape name>} with
 *       {@code smithy-protocol: rpc-v2-cbor} and {@code Accept: application/cbor}; HTTP binding traits are ignored and
 *       no {@code X-Amz-Target} header is sent;</li>
 *   <li>the body is the whole input as a CBOR map ({@code {}} when it has no members) with
 *       {@code Content-Type: application/cbor}, except that an operation whose input is {@code smithy.api#Unit} sends
 *       no body and no {@code Content-Type};</li>
 *   <li>values follow Smithy Java's {@link Rpcv2CborCodec}: member names are the modeled names, timestamps are tag-1
 *       epoch seconds with millisecond resolution ({@code timestampFormat} is ignored), and null members are
 *       omitted;</li>
 *   <li>a response is successful only with status code 200 and is malformed unless it carries
 *       {@code smithy-protocol: rpc-v2-cbor}: a malformed success is a protocol violation and a malformed error is
 *       reported from its status code alone, without reading its body;</li>
 *   <li>the error type is the body's {@code __type} (an absolute shape ID); {@code X-Amzn-Errortype} and the body's
 *       {@code code} are ignored. {@code awsQueryCompatible} services send {@code x-amzn-query-mode: true} and
 *       report the {@code x-amzn-query-error} code.</li>
 * </ul>
 */
final class Rpcv2CborClientProtocol implements ClientProtocol {
    private final ProtocolContext context;
    private final Rpcv2CborCodec codec;
    private final ModeledErrors errors;
    private final String pathPrefix;

    Rpcv2CborClientProtocol(ProtocolContext context) {
        this.context = context;
        this.codec = Rpcv2CborCodec.builder()
                                   .defaultNamespace(context.service().serviceId().getNamespace())
                                   .build();
        this.errors = ModeledErrors.of(context);
        this.pathPrefix = "/service/" + context.service().serviceId().getName() + "/operation/";
    }

    @Override
    public ShapeId protocolId() {
        return context.selected().protocolId();
    }

    @Override
    public OperationProtocol operation(OperationSchemas operation) {
        return new Rpcv2CborOperationProtocol(this, operation);
    }

    ProtocolContext context() {
        return context;
    }

    Rpcv2CborCodec codec() {
        return codec;
    }

    ModeledErrors errors() {
        return errors;
    }

    /** {@code /service/<service shape name>/operation/}, to which the operation's shape name is appended. */
    String pathPrefix() {
        return pathPrefix;
    }
}
