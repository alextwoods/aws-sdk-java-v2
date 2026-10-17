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

package software.amazon.awssdk.codegen.poet.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The runtime protocol selection of a client generated with schema-based serialization (design A.11, Requirements
 * 12.1–12.4): the default protocol, picked from the protocol traits the service declares in
 * {@link ProtocolUtils#CLIENT_PROTOCOL_PRIORITY} order, and the {@code ClientProtocolFactory} the generated base
 * builder references for it.
 *
 * <p>A client gets runtime protocol selection only when the factory of its default protocol is available to it:
 * <ul>
 *     <li>{@link #FACTORIES}, available to every service: {@code smithy.protocols#rpcv2Cbor};</li>
 *     <li>{@link #STAGED_FACTORIES}, available only to services whose {@code customization.config} sets
 *     {@code stagedClientProtocols} (the Protocol_Test_Services): {@code aws.protocols#awsJson1_0},
 *     {@code aws.protocols#awsJson1_1} and {@code aws.protocols#restJson1}. Phase 4 adds each new protocol here so its
 *     protocol tests run on the new path, while the prototype services that default to it keep the legacy path until
 *     phase 5 (tasks 9.2–9.4) moves them.</li>
 * </ul>
 * A service whose default protocol has no available factory keeps the legacy builder, because its calls still run on
 * the legacy marshallers and would ignore a configured protocol. A runtime override ({@code protocol(...)}) of a client
 * that has runtime protocol selection may use any registered factory, staged or not.
 */
@SdkInternalApi
public final class ClientProtocolSelection {
    private static final Map<ShapeId, ClassName> FACTORIES;
    private static final Map<ShapeId, ClassName> STAGED_FACTORIES;

    static {
        Map<ShapeId, ClassName> factories = new HashMap<>();
        factories.put(ShapeId.from("smithy.protocols#rpcv2Cbor"),
                      ClassName.get("software.amazon.awssdk.protocols.client.rpcv2", "RpcV2CborProtocol", "Factory"));
        FACTORIES = Collections.unmodifiableMap(factories);

        Map<ShapeId, ClassName> staged = new HashMap<>();
        staged.put(ShapeId.from("aws.protocols#awsJson1_0"),
                   ClassName.get("software.amazon.awssdk.protocols.client.awsjson", "AwsJson1Protocol", "Factory"));
        staged.put(ShapeId.from("aws.protocols#awsJson1_1"),
                   ClassName.get("software.amazon.awssdk.protocols.client.awsjson", "AwsJson11Protocol", "Factory"));
        staged.put(ShapeId.from("aws.protocols#restJson1"),
                   ClassName.get("software.amazon.awssdk.protocols.client.restjson", "RestJsonClientProtocol", "Factory"));
        STAGED_FACTORIES = Collections.unmodifiableMap(staged);
    }

    private final ShapeId defaultProtocol;
    private final ClassName factoryClass;
    private final ClassName serviceSchemasClass;
    private final ClassName baseExceptionClass;
    private final String serviceVersion;

    private ClientProtocolSelection(ShapeId defaultProtocol, ClassName factoryClass, Metadata metadata) {
        this.defaultProtocol = defaultProtocol;
        this.factoryClass = factoryClass;
        this.serviceSchemasClass = ServiceSchemasGenerator.className(metadata.getFullInternalPackageName(),
                                                                     metadata.getServiceName());
        this.baseExceptionClass = ClassName.get(metadata.getFullModelPackageName(), metadata.getBaseExceptionName());
        this.serviceVersion = metadata.getApiVersion();
    }

    /**
     * Returns the runtime protocol selection of a client.
     *
     * @param model the intermediate model
     * @return the selection, or empty if the client is not generated with schema-based serialization
     *     ({@link SchemaCodegen#isApplicable(IntermediateModel)}) or its default protocol has no factory available to
     *     it: none yet, or a staged one without {@code stagedClientProtocols}
     * @throws IllegalArgumentException if the service is generated with schema-based serialization and declares none of
     *     the supported protocols ({@link ProtocolUtils#resolveDefaultClientProtocol})
     */
    public static Optional<ClientProtocolSelection> forModel(IntermediateModel model) {
        if (!SchemaCodegen.isApplicable(model)) {
            return Optional.empty();
        }
        Model smithy = model.smithyModel();
        ServiceShape service = smithy.expectShape(model.serviceId(), ServiceShape.class);
        ShapeId selected = ProtocolUtils.resolveDefaultClientProtocol(ServiceIndex.of(smithy), service);
        ClassName factory = FACTORIES.get(selected);
        if (factory == null && model.getCustomizationConfig().isStagedClientProtocols()) {
            factory = STAGED_FACTORIES.get(selected);
        }
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(new ClientProtocolSelection(selected, factory, model.getMetadata()));
    }

    /**
     * @return the shape ID of the default protocol's definition trait
     */
    public ShapeId defaultProtocol() {
        return defaultProtocol;
    }

    /**
     * @return the factory class of the default protocol, whose {@code INSTANCE} the base builder sets as
     *     {@code SdkClientOption.CLIENT_PROTOCOL_FACTORY}
     */
    public ClassName factoryClass() {
        return factoryClass;
    }

    /**
     * @return the expression of the service schema, {@code <Svc>ServiceSchemas.SERVICE}
     */
    public CodeBlock serviceSchema() {
        return CodeBlock.of("$T.$L", serviceSchemasClass, ServiceSchemasGenerator.SERVICE_FIELD);
    }

    /**
     * @return the service's base exception class, such as {@code CloudWatchException}
     */
    public ClassName baseExceptionClass() {
        return baseExceptionClass;
    }

    /**
     * @return the service API version, such as {@code 2010-08-01}
     */
    public String serviceVersion() {
        return serviceVersion;
    }
}
