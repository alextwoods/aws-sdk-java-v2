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

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.core.SelectedProtocol;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Test support: a service of a Smithy model served by any launch protocol runtime through Smithy Java dynamic
 * schemas, in place of generated types.
 *
 * <p>Condensed from the protocol modules' conformance test support (their {@code DynamicService}s), declaring
 * whichever launch protocol the service carries, with the service data generated {@code ServiceMetadata} records
 * (XML namespace, service version, {@code ec2QueryName}s). The random models declare no errors, so the type registry
 * is empty; a later error-path property can register errors as the protocol modules' {@code DynamicError} does.
 */
public final class DynamicService {
    private final Model model;
    private final ServiceShape service;
    private final SchemaConverter converter;
    private final ServiceProtocols protocols;
    private final SchemaTypeRegistry registry;

    public DynamicService(Model model, ShapeId serviceId) {
        this.model = model;
        this.service = model.expectShape(serviceId, ServiceShape.class);
        this.converter = new SchemaConverter(model);
        Schema serviceSchema = service.getTrait(XmlNamespaceTrait.class)
                                      .map(ns -> Schema.createService(serviceId, ns))
                                      .orElseGet(() -> Schema.createService(serviceId));
        ServiceProtocols.Builder builder = ServiceProtocols.builder(serviceSchema);
        for (ShapeId trait : service.getAllTraits().keySet()) {
            if (trait.equals(ProtocolIds.REST_XML)) {
                RestXmlTrait restXml = service.expectTrait(RestXmlTrait.class);
                builder.protocol(new ModeledProtocol(ProtocolIds.REST_XML, restXml.getHttp(),
                                                     restXml.getEventStreamHttp(), restXml.isNoErrorWrapping()));
            } else if (ProtocolIds.LAUNCH_PRECEDENCE.contains(trait)) {
                builder.protocol(ModeledProtocol.of(trait));
            }
        }
        builder.serviceVersion(service.getVersion());
        new Walker(model).walkShapes(service).forEach(shape -> shape.asMemberShape().ifPresent(
            member -> member.getTrait(Ec2QueryNameTrait.class).ifPresent(
                trait -> builder.ec2QueryName(member.getId(), trait.getValue()))));
        this.protocols = builder.build();
        this.registry = SchemaTypeRegistry.builder().build();
    }

    public Model model() {
        return model;
    }

    public ServiceProtocols protocols() {
        return protocols;
    }

    /** The client runtime of {@code provider} for this service, with host-prefix injection enabled. */
    public ClientProtocol client(ProtocolProvider provider) {
        ModeledProtocol declaration = protocols.protocol(provider.protocolId()).orElseThrow();
        SelectedProtocol selected = new SelectedProtocol(protocols, declaration, provider,
                                                         SelectedProtocol.Source.EXPLICIT_PROVIDER);
        return provider.createClientProtocol(ProtocolContext.of(selected, registry));
    }

    public OperationShape operation(ShapeId id) {
        return model.expectShape(id, OperationShape.class);
    }

    /** The schemas of {@code operation}, as generated code would declare them. */
    public OperationSchemas schemas(OperationShape operation) {
        OperationSchemas.Builder builder = OperationSchemas.builder(converter.getSchema(operation));
        StructureShape input = model.expectShape(operation.getInputShape(), StructureShape.class);
        StructureShape output = model.expectShape(operation.getOutputShape(), StructureShape.class);
        builder.input(converter.getSchema(input)).output(converter.getSchema(output));
        streamMember(input).ifPresent(builder::inputStreamMember);
        streamMember(output).ifPresent(builder::outputStreamMember);
        List<Schema> errors = operation.getErrors(service).stream()
                                       .map(error -> converter.getSchema(model.expectShape(error)))
                                       .toList();
        builder.errors(errors.toArray(new Schema[0]));
        return builder.build();
    }

    private Optional<String> streamMember(StructureShape structure) {
        for (MemberShape member : structure.members()) {
            if (model.expectShape(member.getTarget()).hasTrait(StreamingTrait.class)) {
                return Optional.of(member.getMemberName());
            }
        }
        return Optional.empty();
    }

    public Schema schema(Shape shape) {
        return converter.getSchema(shape);
    }

    public Schema schema(ShapeId shape) {
        return converter.getSchema(model.expectShape(shape));
    }

    /** A fresh builder of {@code structure}. */
    public ShapeBuilder<StructDocument> builder(ShapeId structure) {
        return SchemaConverter.createDocumentBuilder(schema(structure));
    }
}
