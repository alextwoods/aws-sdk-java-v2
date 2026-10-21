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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.schema.SchemaFieldOrder.SchemaField;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnitTypeTrait;

/**
 * Generates {@code <Svc>TypeRegistry}, the package's Primary_Type_Registry (design A.18, B.5, OD-18; Requirements 22.6
 * and 22.7):
 *
 * <pre>{@code
 * @SdkPublicApi
 * public final class SqsTypeRegistry {
 *     public static final TypeRegistry INSTANCE = TypeRegistry.builder()
 *         .putLazyType(ShapeId.fromParts("com.amazonaws.sqs", "Message"), () -> SqsSchemas.MESSAGE)
 *         .build();
 * }
 * }</pre>
 *
 * <p><b>Entries.</b> Every shape of type {@code structure} that the service shape's {@code shapes} property lists, in
 * ShapeId order. Unions, other shape types and {@code smithy.api#Unit} are not registered. A listed structure must be
 * in the service closure, so that it has a schema field; otherwise generation fails. With no entries, the field is
 * {@code TypeRegistry.empty()}.
 *
 * <p><b>The {@code shapes} property.</b> Smithy's service shape has no {@code shapes} property yet (smithy-model
 * through 1.74 has no such member on {@code ServiceShape}, and its JSON AST loader accepts only {@code version},
 * {@code operations}, {@code resources}, {@code rename}, {@code errors}, {@code traits} and {@code mixins}), and C2J
 * models have no equivalent. As the Document SEP directs, the set is empty until the property exists:
 * {@link SchemaCodegen#create} passes no shapes, and {@link SchemaCodegen.Builder#serviceShapes(Collection)} supplies
 * them.
 *
 * <p>Each schema supplier is a non-capturing lambda that reads a {@code <Svc>Schemas*} field, so initializing the
 * registry, and calling {@code contains}, initializes no schema partition and no model class (Requirement 22.7). The
 * class lives in the model package because the partition fields are package-private.
 */
@SdkInternalApi
final class TypeRegistrySpec {
    static final String FIELD = "INSTANCE";

    private final ServiceShape service;
    private final SchemasGenerator schemas;
    private final ClassName className;
    private final Map<ShapeId, SchemaField> registered;

    /**
     * @param model the model
     * @param service the service
     * @param schemas the partition classes, whose fields the suppliers read
     * @param modelPackage the model package
     * @param serviceName the class name prefix
     * @param listedShapes the shapes the service's {@code shapes} property lists
     * @throws SchemaCodegenException if a listed shape is not in the model, or a listed structure has no schema
     */
    TypeRegistrySpec(Model model, ServiceShape service, SchemasGenerator schemas, String modelPackage,
                     String serviceName, Collection<ShapeId> listedShapes) {
        this.service = service;
        this.schemas = schemas;
        this.className = ClassName.get(modelPackage, serviceName + "TypeRegistry");
        Map<ShapeId, SchemaField> entries = new LinkedHashMap<>();
        for (ShapeId id : new TreeSet<>(listedShapes)) {
            Shape shape = model.getShape(id).orElseThrow(
                () -> new SchemaCodegenException("The shapes property of " + service.getId() + " lists " + id
                                                 + ", which is not in the model"));
            if (!shape.isStructureShape() || shape.hasTrait(UnitTypeTrait.class)) {
                continue;
            }
            SchemaField field = schemas.order().field(id);
            if (field == null) {
                throw new SchemaCodegenException("The shapes property of " + service.getId() + " lists " + id
                                                 + ", which is not in the service closure and has no schema");
            }
            entries.put(id, field);
        }
        this.registered = Collections.unmodifiableMap(entries);
    }

    /**
     * @return the generated class name
     */
    ClassName className() {
        return className;
    }

    /**
     * @return the model ShapeIds of the registered structures, in ShapeId order
     */
    Set<ShapeId> registeredShapes() {
        return registered.keySet();
    }

    /**
     * @return the class
     */
    TypeSpec spec() {
        return TypeSpec.classBuilder(className)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addAnnotation(SdkPublicApi.class)
                       .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                       .addJavadoc("The type registry of the $L service's model package: the structures the service "
                                   + "lists in its\n{@code shapes} property, by shape ID. Use it to turn a document "
                                   + "with a discriminator into a model\nobject with {@code TypeRegistry.deserialize}, "
                                   + "or to compose it with other registries.\n", service.getId())
                       .addField(registryField())
                       .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                       .build();
    }

    private FieldSpec registryField() {
        CodeBlock init;
        if (registered.isEmpty()) {
            init = CodeBlock.of("$T.empty()", SchemaRuntimeType.TYPE_REGISTRY);
        } else {
            CodeBlock.Builder builder = CodeBlock.builder().add("$T.builder()$>", SchemaRuntimeType.TYPE_REGISTRY);
            for (SchemaField field : registered.values()) {
                ShapeId id = field.schemaId();
                builder.add("\n.putLazyType($T.fromParts($S, $S), () -> $L)", SchemaRuntimeType.SHAPE_ID,
                            id.getNamespace(), id.getName(), schemas.fieldReference(field));
            }
            init = builder.add("\n.build()$<").build();
        }
        return FieldSpec.builder(SchemaRuntimeType.TYPE_REGISTRY, FIELD, Modifier.PUBLIC, Modifier.STATIC,
                                 Modifier.FINAL)
                        .addJavadoc("The registry. Every access returns the same instance; creating it loads no "
                                    + "model class.\n")
                        .initializer(init)
                        .build();
    }
}
