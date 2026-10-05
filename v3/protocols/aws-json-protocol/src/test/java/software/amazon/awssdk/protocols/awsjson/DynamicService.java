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

package software.amazon.awssdk.protocols.awsjson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.core.SelectedProtocol;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.smithy.runtime.registry.RegisteredType;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.aws.traits.protocols.AwsQueryCompatibleTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Test support: a service of a Smithy model served by a protocol runtime through Smithy Java dynamic schemas, in
 * place of generated types. Modeled errors are registered as {@link DynamicError}s.
 */
final class DynamicService {
    /** Canonical rendering of values for comparison (JSON, epoch-seconds timestamps, base64 blobs). */
    private static final JsonCodec CANONICAL = JsonCodec.builder().useTimestampFormat(false).build();

    private final Model model;
    private final ServiceShape service;
    private final SchemaConverter converter;
    private final ServiceProtocols protocols;
    private final SchemaTypeRegistry registry;

    DynamicService(Model model, ShapeId serviceId) {
        this.model = model;
        this.service = model.expectShape(serviceId, ServiceShape.class);
        this.converter = new SchemaConverter(model);
        ServiceProtocols.Builder builder = ServiceProtocols.builder(Schema.createService(serviceId));
        for (ShapeId trait : service.getAllTraits().keySet()) {
            if (trait.getNamespace().equals("aws.protocols") && trait.getName().startsWith("awsJson")) {
                builder.protocol(ModeledProtocol.of(trait));
            }
        }
        builder.awsQueryCompatible(service.hasTrait(AwsQueryCompatibleTrait.class));
        Set<ShapeId> errors = new TreeSet<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            errors.addAll(operation.getErrors(service));
        }
        errors.addAll(service.getErrors());
        List<RegisteredType<?>> entries = new ArrayList<>();
        for (ShapeId error : errors) {
            StructureShape shape = model.expectShape(error, StructureShape.class);
            shape.getTrait(AwsQueryErrorTrait.class).ifPresent(
                trait -> builder.awsQueryError(error, trait.getCode(), trait.getHttpResponseCode()));
            Schema schema = converter.getSchema(shape);
            entries.add(RegisteredType.of(schema, DynamicError.class, () -> DynamicError.builder(schema)));
        }
        this.protocols = builder.build();
        List<ShapeId> ids = List.copyOf(errors);
        this.registry = SchemaTypeRegistry.builder().partition(ids, () -> entries).build();
    }

    ServiceProtocols protocols() {
        return protocols;
    }

    /** The client runtime of {@code provider} for this service. */
    ClientProtocol client(ProtocolProvider provider, boolean hostPrefixInjection) {
        ModeledProtocol declaration = protocols.protocol(provider.protocolId()).orElseThrow();
        SelectedProtocol selected = new SelectedProtocol(protocols, declaration, provider,
                                                         SelectedProtocol.Source.EXPLICIT_PROVIDER);
        return provider.createClientProtocol(new ProtocolContext(selected, registry, hostPrefixInjection));
    }

    OperationShape operation(ShapeId id) {
        return model.expectShape(id, OperationShape.class);
    }

    Model model() {
        return model;
    }

    ServiceShape service() {
        return service;
    }

    /** The schemas of {@code operation}, as generated code would declare them. */
    OperationSchemas schemas(OperationShape operation) {
        OperationSchemas.Builder builder = OperationSchemas.builder(converter.getSchema(operation));
        StructureShape input = model.expectShape(operation.getInputShape(), StructureShape.class);
        StructureShape output = model.expectShape(operation.getOutputShape(), StructureShape.class);
        builder.input(converter.getSchema(input)).output(converter.getSchema(output));
        streamMember(input).ifPresent(builder::inputStreamMember);
        streamMember(output).ifPresent(builder::outputStreamMember);
        List<Schema> errors = new ArrayList<>();
        for (ShapeId error : operation.getErrors(service)) {
            errors.add(converter.getSchema(model.expectShape(error)));
        }
        builder.errors(errors.toArray(new Schema[0]));
        return builder.build();
    }

    private java.util.Optional<String> streamMember(StructureShape structure) {
        for (MemberShape member : structure.members()) {
            if (model.expectShape(member.getTarget()).hasTrait(StreamingTrait.class)) {
                return java.util.Optional.of(member.getMemberName());
            }
        }
        return java.util.Optional.empty();
    }

    Schema schema(Shape shape) {
        return converter.getSchema(shape);
    }

    /** A value of {@code structure} built from protocol-test params. */
    StructDocument value(Shape structure, Node params) {
        ShapeBuilder<StructDocument> builder = SchemaConverter.createDocumentBuilder(converter.getSchema(structure));
        builder.deserialize(new NodeDeserializer(params));
        return builder.build();
    }

    /** A fresh builder of {@code structure}. */
    ShapeBuilder<StructDocument> builder(Shape structure) {
        return SchemaConverter.createDocumentBuilder(converter.getSchema(structure));
    }

    static boolean isError(Shape shape) {
        return shape.hasTrait(ErrorTrait.class);
    }

    /** The canonical rendering of a value, as a parsed node (object member order is insignificant). */
    static Node canonical(SerializableStruct value) {
        return Node.parse(new String(bytes(CANONICAL.serialize(value)), StandardCharsets.UTF_8));
    }

    private static byte[] bytes(java.nio.ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }
}
