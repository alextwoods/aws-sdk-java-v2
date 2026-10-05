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

package software.amazon.awssdk.test.protocols.support;

import java.util.function.Supplier;
import software.amazon.awssdk.protocols.awsjson.AwsJson10ProtocolProvider;
import software.amazon.awssdk.protocols.awsjson.AwsJson11ProtocolProvider;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.ec2query.Ec2QueryProtocolProvider;
import software.amazon.awssdk.protocols.query.AwsQueryProtocolProvider;
import software.amazon.awssdk.protocols.restjson.RestJsonProtocolProvider;
import software.amazon.awssdk.protocols.restxml.RestXmlProtocolProvider;
import software.amazon.awssdk.protocols.rpcv2cbor.Rpcv2CborProtocolProvider;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The seven launch protocols with the model features each supports, which constrain the random models: documents
 * only in the JSON protocols, sparse collections only in JSON and CBOR, XML binding traits in the XML protocols,
 * HTTP binding traits in the REST protocols, and event streams everywhere but the query protocols.
 */
public enum WireProtocol {
    AWS_JSON_1_0(ProtocolIds.AWS_JSON_1_0, AwsJson10ProtocolProvider::new),
    AWS_JSON_1_1(ProtocolIds.AWS_JSON_1_1, AwsJson11ProtocolProvider::new),
    REST_JSON_1(ProtocolIds.REST_JSON_1, RestJsonProtocolProvider::new),
    REST_XML(ProtocolIds.REST_XML, RestXmlProtocolProvider::new),
    AWS_QUERY(ProtocolIds.AWS_QUERY, AwsQueryProtocolProvider::new),
    EC2_QUERY(ProtocolIds.EC2_QUERY, Ec2QueryProtocolProvider::new),
    RPC_V2_CBOR(ProtocolIds.RPC_V2_CBOR, Rpcv2CborProtocolProvider::new);

    private final ShapeId id;
    private final Supplier<ProtocolProvider> provider;

    WireProtocol(ShapeId id, Supplier<ProtocolProvider> provider) {
        this.id = id;
        this.provider = provider;
    }

    public ShapeId id() {
        return id;
    }

    public ProtocolProvider provider() {
        return provider.get();
    }

    /** True for restJson1 and restXml, which bind members to HTTP message locations. */
    public boolean rest() {
        return this == REST_JSON_1 || this == REST_XML;
    }

    /** True for the JSON protocols, the only ones with documents. */
    public boolean json() {
        return this == AWS_JSON_1_0 || this == AWS_JSON_1_1 || this == REST_JSON_1;
    }

    /** True for the protocols whose bodies are XML (restXml and the query protocols' responses). */
    public boolean xml() {
        return this == REST_XML || query();
    }

    public boolean query() {
        return this == AWS_QUERY || this == EC2_QUERY;
    }

    public boolean cbor() {
        return this == RPC_V2_CBOR;
    }

    /** True if lists and maps may be {@code @sparse} (XML has no null collection element). */
    public boolean sparse() {
        return json() || cbor();
    }

    public boolean eventStreams() {
        return !query();
    }

    /** True if the protocol honors {@code timestampFormat} (rpcv2Cbor always writes CBOR epoch timestamps). */
    public boolean timestampFormats() {
        return !cbor();
    }
}
