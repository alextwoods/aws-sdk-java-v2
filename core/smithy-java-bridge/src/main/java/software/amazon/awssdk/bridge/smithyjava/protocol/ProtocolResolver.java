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

package software.amazon.awssdk.bridge.smithyjava.protocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.core.SdkProtocolMetadata;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson11Protocol;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;

/**
 * Resolves the appropriate smithy-java {@link ClientProtocol} implementation based on the
 * protocol trait present on the service schema. Caches protocol instances per service shape ID
 * for reuse across operations.
 *
 * <p>Supported protocols:
 * <ul>
 *   <li>{@code aws.protocols#awsJson1_0} &rarr; {@link AwsJson1Protocol}</li>
 *   <li>{@code aws.protocols#awsJson1_1} &rarr; {@link AwsJson11Protocol}</li>
 *   <li>{@code aws.protocols#restJson1} &rarr; {@link RestJsonClientProtocol}</li>
 *   <li>{@code aws.protocols#restXml} &rarr; {@link RestXmlClientProtocol}</li>
 *   <li>{@code aws.protocols#awsQuery} &rarr; not yet supported (throws UnsupportedOperationException)</li>
 *   <li>{@code smithy.protocols#rpcv2Cbor} &rarr; {@link RpcV2CborProtocol}</li>
 * </ul>
 */
@SdkInternalApi
public final class ProtocolResolver {

    /**
     * ShapeId for the aws.protocols#awsJson1_0 protocol trait.
     */
    static final ShapeId AWS_JSON_1_0 = AwsJson1_0Trait.ID;

    /**
     * ShapeId for the aws.protocols#awsJson1_1 protocol trait.
     */
    static final ShapeId AWS_JSON_1_1 = AwsJson1_1Trait.ID;

    /**
     * ShapeId for the aws.protocols#restJson1 protocol trait.
     */
    static final ShapeId REST_JSON_1 = RestJson1Trait.ID;

    /**
     * ShapeId for the aws.protocols#restXml protocol trait.
     */
    static final ShapeId REST_XML = RestXmlTrait.ID;

    /**
     * ShapeId for the aws.protocols#awsQuery protocol trait.
     */
    static final ShapeId AWS_QUERY = AwsQueryTrait.ID;

    /**
     * ShapeId for the smithy.protocols#rpcv2Cbor protocol trait.
     */
    static final ShapeId RPC_V2_CBOR = Rpcv2CborTrait.ID;

    private static final TraitKey<AwsJson1_0Trait> AWS_JSON_1_0_KEY = TraitKey.get(AwsJson1_0Trait.class);
    private static final TraitKey<AwsJson1_1Trait> AWS_JSON_1_1_KEY = TraitKey.get(AwsJson1_1Trait.class);
    private static final TraitKey<RestJson1Trait> REST_JSON_1_KEY = TraitKey.get(RestJson1Trait.class);
    private static final TraitKey<RestXmlTrait> REST_XML_KEY = TraitKey.get(RestXmlTrait.class);
    private static final TraitKey<AwsQueryTrait> AWS_QUERY_KEY = TraitKey.get(AwsQueryTrait.class);
    private static final TraitKey<Rpcv2CborTrait> RPC_V2_CBOR_KEY = TraitKey.get(Rpcv2CborTrait.class);

    private final ConcurrentMap<ShapeId, ClientProtocol<HttpRequest, HttpResponse>> cache = new ConcurrentHashMap<>();

    public ProtocolResolver() {
    }

    /**
     * Resolves the appropriate {@link ClientProtocol} for the given operation based on the
     * protocol trait present on the service schema.
     *
     * <p>Protocol instances are cached per service shape ID so that subsequent calls for
     * operations on the same service reuse the same protocol instance.
     *
     * @param operation the API operation whose service schema determines the protocol
     * @return the resolved protocol instance
     * @throws SdkClientException if no recognized protocol trait is found on the service schema
     */
    @SuppressWarnings("unchecked")
    public ClientProtocol<HttpRequest, HttpResponse> resolve(ApiOperation<?, ?> operation) {
        ShapeId serviceShapeId = operation.service().schema().id();
        return cache.computeIfAbsent(serviceShapeId, id -> {
            ClientProtocol<HttpRequest, HttpResponse> fromSchema = tryCreateFromSchema(operation.service().schema());
            if (fromSchema != null) {
                return fromSchema;
            }
            throw SdkClientException.create(
                "No recognized protocol trait found on service schema for " + serviceShapeId
                + ". Supported protocols: awsJson1_0, awsJson1_1, restJson1, restXml, awsQuery, rpcv2Cbor.");
        });
    }

    /**
     * Resolves the protocol using the v2 protocol metadata as a fallback when the service schema
     * doesn't have protocol traits (which is the current state of generated ApiService classes).
     *
     * <p>First tries to resolve from schema traits. If none are present, falls back to the
     * {@link SdkProtocolMetadata} from {@code ClientExecutionParams.getProtocolMetadata()}.
     *
     * @param operation the API operation whose service schema determines the protocol
     * @param protocolMetadata the v2 protocol metadata providing the fallback protocol identifier
     * @return the resolved protocol instance
     * @throws SdkClientException if no recognized protocol can be determined from either source
     */
    @SuppressWarnings("unchecked")
    public ClientProtocol<HttpRequest, HttpResponse> resolve(ApiOperation<?, ?> operation,
                                                             SdkProtocolMetadata protocolMetadata) {
        ShapeId serviceShapeId = operation.service().schema().id();
        return cache.computeIfAbsent(serviceShapeId, id -> {
            // First try schema traits
            ClientProtocol<HttpRequest, HttpResponse> fromSchema = tryCreateFromSchema(operation.service().schema());
            if (fromSchema != null) {
                return fromSchema;
            }
            // Fall back to v2 protocol metadata
            if (protocolMetadata instanceof AwsProtocolMetadata) {
                AwsServiceProtocol serviceProtocol =
                    AwsServiceProtocol.fromValue(protocolMetadata.serviceProtocol());
                return createFromServiceProtocol(serviceProtocol, id);
            }
            throw SdkClientException.create(
                "No recognized protocol trait found on service schema for " + id
                + " and no protocol metadata available.");
        });
    }

    private ClientProtocol<HttpRequest, HttpResponse> tryCreateFromSchema(Schema serviceSchema) {
        if (serviceSchema.hasTrait(AWS_JSON_1_0_KEY)) {
            return new AwsJson1Protocol(serviceSchema.id());
        }
        if (serviceSchema.hasTrait(AWS_JSON_1_1_KEY)) {
            return new AwsJson11Protocol(serviceSchema.id());
        }
        if (serviceSchema.hasTrait(REST_JSON_1_KEY)) {
            return new RestJsonClientProtocol(serviceSchema.id());
        }
        if (serviceSchema.hasTrait(REST_XML_KEY)) {
            return new RestXmlClientProtocol(serviceSchema.id());
        }
        if (serviceSchema.hasTrait(AWS_QUERY_KEY)) {
            // TODO: AwsQueryClientProtocol requires a service version string that is not available
            // from the schema traits alone. Add aws-client-awsquery dependency and resolve version
            // from service metadata when AwsQuery support is needed.
            throw new UnsupportedOperationException(
                "aws.protocols#awsQuery is not yet supported by the smithy-java bridge. "
                + "Service: " + serviceSchema.id());
        }
        if (serviceSchema.hasTrait(RPC_V2_CBOR_KEY)) {
            return new RpcV2CborProtocol(serviceSchema.id());
        }
        return null; // No recognized trait found
    }

    private ClientProtocol<HttpRequest, HttpResponse> createFromServiceProtocol(AwsServiceProtocol serviceProtocol,
                                                                                ShapeId serviceShapeId) {
        switch (serviceProtocol) {
            case AWS_JSON:
                return new AwsJson1Protocol(serviceShapeId);
            case REST_JSON:
                return new RestJsonClientProtocol(serviceShapeId);
            case REST_XML:
                return new RestXmlClientProtocol(serviceShapeId);
            case CBOR:
            case SMITHY_RPC_V2_CBOR:
                return new RpcV2CborProtocol(serviceShapeId);
            default:
                throw SdkClientException.create(
                    "Unsupported protocol: " + serviceProtocol + " for service " + serviceShapeId);
        }
    }
}
