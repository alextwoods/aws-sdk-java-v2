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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.poet.schema.SchemaFieldOrder.SchemaField;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;

/**
 * Computes and renders the service's Error_Registry, the {@code ERROR_REGISTRY} field of {@code <Svc>Operations}
 * (design A.18 and B.5, Requirement 23.1):
 *
 * <pre>{@code
 * public static final ErrorRegistry ERROR_REGISTRY = ErrorRegistry.builder()
 *     .putLazyType(QUEUE_DOES_NOT_EXIST, () -> SqsSchemas.QUEUE_DOES_NOT_EXIST)
 *     .errorCode("AWS.SimpleQueueService.NonExistentQueue", QUEUE_DOES_NOT_EXIST)
 *     .build();
 * }</pre>
 *
 * <p><b>Entries.</b> One entry per shape in the schema closure that has the {@code error} trait and is the owner of its
 * generated exception class. Several error shapes can get the same exception class (CloudWatch
 * {@code LimitExceededException} and {@code LimitExceededFault}); the intermediate model generates the class for one of
 * them, and only that shape is registered, so an error resolves to exactly the classes, and with exactly the codes,
 * the legacy clients register. The other shapes are aliases of the owner: an operation that lists an alias in its
 * errors gets the owner's ShapeId. An error shape with no generated class and no owner is not registered.
 *
 * <p><b>Code index.</b> One {@code errorCode} per registered shape with an {@code aws.protocols#awsQueryError} trait.
 * Entries are emitted in ShapeId order; when two registered shapes have the same code, only the first is emitted, which
 * is what {@code ErrorRegistry.Builder.errorCode} would keep.
 *
 * <p>Each schema supplier is a non-capturing lambda that reads a {@code <Svc>Schemas*} field, and each ShapeId is a
 * {@code private static final} constant, so initializing the registry initializes no schema partition and no
 * exception class (Requirement 22.7, OD-8).
 */
@SdkInternalApi
final class ErrorRegistrySpec {
    static final String FIELD = "ERROR_REGISTRY";

    private final ServiceShape service;
    private final SchemasGenerator schemas;
    private final Map<ShapeId, SchemaField> registered;
    private final Map<ShapeId, ShapeId> owners;
    private final Map<ShapeId, String> idConstants;
    private final Map<String, ShapeId> codes;

    /**
     * @param service the service
     * @param schemas the partition classes, whose fields the suppliers read
     * @param errorShapeOwner maps an error shape to the error shape that owns its generated exception class: itself,
     *     another error shape, or null if no class is generated for it
     * @param names allocates the constant names of the registered ShapeIds
     */
    ErrorRegistrySpec(ServiceShape service, SchemasGenerator schemas, Function<ShapeId, ShapeId> errorShapeOwner,
                      OperationsClassSpec.Names names) {
        this.service = service;
        this.schemas = schemas;
        SchemaFieldOrder order = schemas.order();

        TreeMap<ShapeId, SchemaField> errors = new TreeMap<>();
        for (SchemaField field : order.fields()) {
            if (field.shape().hasTrait(ErrorTrait.class)) {
                errors.put(field.shape().getId(), field);
            }
        }

        Map<ShapeId, SchemaField> registeredFields = new LinkedHashMap<>();
        Map<ShapeId, ShapeId> ownerByError = new LinkedHashMap<>();
        for (Map.Entry<ShapeId, SchemaField> error : errors.entrySet()) {
            ShapeId owner = errorShapeOwner.apply(error.getKey());
            if (owner == null) {
                continue;
            }
            if (!errors.containsKey(owner) || !owner.equals(errorShapeOwner.apply(owner))) {
                throw new SchemaCodegenException("Error shape " + error.getKey() + " shares the exception class of "
                                                 + owner + ", which is not an error shape in the closure of "
                                                 + service.getId() + " that owns its class");
            }
            ownerByError.put(error.getKey(), owner);
            if (owner.equals(error.getKey())) {
                registeredFields.put(owner, error.getValue());
            }
        }
        this.registered = Collections.unmodifiableMap(registeredFields);
        this.owners = Collections.unmodifiableMap(ownerByError);

        Map<ShapeId, String> constants = new LinkedHashMap<>();
        Map<String, ShapeId> codeIndex = new LinkedHashMap<>();
        for (Map.Entry<ShapeId, SchemaField> entry : registered.entrySet()) {
            constants.put(entry.getKey(), names.allocate(SchemaFieldOrder.toSchemaName(entry.getKey().getName())));
            entry.getValue().shape().getTrait(AwsQueryErrorTrait.class)
                 .ifPresent(trait -> codeIndex.putIfAbsent(trait.getCode(), entry.getKey()));
        }
        this.idConstants = Collections.unmodifiableMap(constants);
        this.codes = Collections.unmodifiableMap(codeIndex);
    }

    /**
     * @return the model ShapeIds of the registered error shapes, in ShapeId order
     */
    Set<ShapeId> registeredShapes() {
        return registered.keySet();
    }

    /**
     * @return the emitted {@code awsQueryError} codes and the model ShapeId each resolves to, in emission order
     */
    Map<String, ShapeId> codes() {
        return codes;
    }

    /**
     * @return whether a constant uses the service namespace constant
     */
    boolean usesServiceNamespace() {
        for (SchemaField field : registered.values()) {
            if (field.schemaId().getNamespace().equals(service.getId().getNamespace())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the errors an operation lists to the registered error shapes, keeping the first occurrence of each.
     *
     * @param errors the error shapes, in model order
     * @return the names of the ShapeId constants of the registered errors
     */
    List<String> errorConstants(List<ShapeId> errors) {
        Set<String> result = new LinkedHashSet<>();
        for (ShapeId error : errors) {
            ShapeId owner = owners.get(error);
            if (owner != null) {
                result.add(idConstants.get(owner));
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * @param namespaceConstant the name of the class's service namespace constant
     * @return the {@code private static final ShapeId} constants of the registered errors
     */
    List<FieldSpec> idFields(String namespaceConstant) {
        List<FieldSpec> fields = new ArrayList<>();
        for (Map.Entry<ShapeId, SchemaField> entry : registered.entrySet()) {
            fields.add(FieldSpec.builder(SchemaRuntimeType.SHAPE_ID, idConstants.get(entry.getKey()), Modifier.PRIVATE,
                                         Modifier.STATIC, Modifier.FINAL)
                                .initializer(OperationsClassSpec.shapeId(entry.getValue().schemaId(), service,
                                                                         namespaceConstant))
                                .build());
        }
        return fields;
    }

    /**
     * @return the {@code public static final ErrorRegistry ERROR_REGISTRY} field
     */
    FieldSpec registryField() {
        CodeBlock.Builder init = CodeBlock.builder().add("$T.builder()$>", SchemaRuntimeType.ERROR_REGISTRY);
        Map<ShapeId, List<String>> codesByShape = new LinkedHashMap<>();
        for (Map.Entry<String, ShapeId> code : codes.entrySet()) {
            codesByShape.computeIfAbsent(code.getValue(), k -> new ArrayList<>()).add(code.getKey());
        }
        for (Map.Entry<ShapeId, SchemaField> entry : registered.entrySet()) {
            String constant = idConstants.get(entry.getKey());
            init.add("\n.putLazyType($L, () -> $L)", constant, schemas.fieldReference(entry.getValue()));
            for (String code : codesByShape.getOrDefault(entry.getKey(), Collections.emptyList())) {
                init.add("\n.errorCode($S, $L)", code, constant);
            }
        }
        init.add("\n.build()$<");
        return FieldSpec.builder(SchemaRuntimeType.ERROR_REGISTRY, FIELD, Modifier.PUBLIC, Modifier.STATIC,
                                 Modifier.FINAL)
                        .addJavadoc("The service's error registry: every modeled error of the service, by shape ID, and "
                                    + "the custom codes of its\n{@code awsQueryError} errors.\n")
                        .initializer(init.build())
                        .build();
    }
}
