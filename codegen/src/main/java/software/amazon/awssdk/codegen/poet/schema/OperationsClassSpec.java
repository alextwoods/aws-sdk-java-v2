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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.schema.SchemaFieldOrder.SchemaField;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generates {@code <Svc>Operations}, the one class per service that holds every operation's {@code ApiOperation} and
 * the service's Error_Registry, shared by the sync and async clients (design A.18, K4; Requirements 23.1, 23.6, 36.4):
 *
 * <pre>{@code
 * public static final ApiService SERVICE = () -> SqsServiceSchemas.SERVICE;
 * public static final ErrorRegistry ERROR_REGISTRY = ...;            // ErrorRegistrySpec
 * public static final ApiOperation<SerializableStruct, SerializableStruct> GET_QUEUE_URL =
 *     SimpleApiOperation.<SerializableStruct, SerializableStruct>builder()
 *         .schema(SqsServiceSchemas.GET_QUEUE_URL)
 *         .inputSchema(() -> SqsSchemas.GET_QUEUE_URL_REQUEST)
 *         .outputSchema(() -> SqsSchemas.GET_QUEUE_URL_RESULT)
 *         .errorIds(QUEUE_DOES_NOT_EXIST, ...)
 *         .errorRegistry(ERROR_REGISTRY)
 *         .effectiveAuthSchemes(SIGV4)
 *         .service(SERVICE)
 *         .build();
 * }</pre>
 *
 * <ul>
 *     <li>Operation fields have the names of the {@code <Svc>ServiceSchemas} operation fields and are in the same
 *     (ShapeId) order.</li>
 *     <li>Input and output schemas are non-capturing lambdas over the {@code <Svc>Schemas*} fields (OD-8), so
 *     initializing the class initializes no schema partition and no model class. A {@code smithy.api#Unit} input or
 *     output reads {@code PreludeSchemas.UNIT}.</li>
 *     <li>Error IDs are the operation's errors, then the service's errors, resolved through the Error_Registry's
 *     class owners and without duplicates.</li>
 *     <li>Effective auth schemes come from {@code ServiceIndex.getEffectiveAuthSchemes(service, operation,
 *     NO_AUTH_AWARE)}: the operation's {@code @auth}, else the service's, in priority order, with
 *     {@code smithy.api#noAuth} appended for {@code @optionalAuth} and used alone when no scheme applies.</li>
 * </ul>
 *
 * <p>The class is public and {@code @SdkInternalApi}, and lives in the model package because the partition fields it
 * reads are package-private. Until generated model classes implement {@code SerializableStruct} (task 3.7), the
 * operations are typed {@code ApiOperation<SerializableStruct, SerializableStruct>}.
 */
@SdkInternalApi
public final class OperationsClassSpec {
    private static final ClassName SERIALIZABLE_STRUCT = SchemaRuntimeType.SERIALIZABLE_STRUCT;

    private final Model model;
    private final ServiceShape service;
    private final SchemasGenerator schemas;
    private final ServiceSchemasGenerator serviceSchemas;
    private final ClassName className;
    private final String namespaceConstant;
    private final ErrorRegistrySpec errorRegistry;
    private final Map<ShapeId, List<ShapeId>> authSchemes;
    private final Map<ShapeId, String> authConstants;

    OperationsClassSpec(Model model, ServiceShape service, SchemasGenerator schemas,
                        ServiceSchemasGenerator serviceSchemas, String modelPackage, String serviceName,
                        Function<ShapeId, ShapeId> errorShapeOwner) {
        this.model = model;
        this.service = service;
        this.schemas = schemas;
        this.serviceSchemas = serviceSchemas;
        this.className = ClassName.get(modelPackage, serviceName + "Operations");

        Names names = new Names();
        names.reserve(ServiceSchemasGenerator.SERVICE_FIELD);
        names.reserve(ErrorRegistrySpec.FIELD);
        for (Map.Entry<ShapeId, String> operation : serviceSchemas.operationFields().entrySet()) {
            if (!names.reserve(operation.getValue())) {
                throw new SchemaCodegenException("The operations field of " + operation.getKey() + ", "
                                                 + operation.getValue() + ", conflicts with another field of "
                                                 + className.simpleName());
            }
        }
        this.namespaceConstant = names.allocate(SchemaFieldOrder.NAMESPACE_CONSTANT);
        this.errorRegistry = new ErrorRegistrySpec(service, schemas, errorShapeOwner, names);

        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Map<ShapeId, List<ShapeId>> auth = new LinkedHashMap<>();
        Map<ShapeId, String> authNames = new LinkedHashMap<>();
        for (ShapeId operation : serviceSchemas.operationFields().keySet()) {
            List<ShapeId> schemes = new ArrayList<>(
                serviceIndex.getEffectiveAuthSchemes(service, operation, ServiceIndex.AuthSchemeMode.NO_AUTH_AWARE)
                            .keySet());
            auth.put(operation, Collections.unmodifiableList(schemes));
        }
        for (List<ShapeId> schemes : auth.values()) {
            for (ShapeId scheme : schemes) {
                if (!authNames.containsKey(scheme)) {
                    authNames.put(scheme, names.allocate(SchemaFieldOrder.toSchemaName(scheme.getName())));
                }
            }
        }
        this.authSchemes = Collections.unmodifiableMap(auth);
        this.authConstants = Collections.unmodifiableMap(authNames);
    }

    /**
     * @return the generated class name
     */
    public ClassName className() {
        return className;
    }

    /**
     * @return the model ShapeIds of the error shapes in the Error_Registry, in ShapeId order
     */
    public Set<ShapeId> registeredErrors() {
        return errorRegistry.registeredShapes();
    }

    /**
     * @return the {@code awsQueryError} codes of the Error_Registry's code index and the model ShapeId each resolves
     *     to, in emission order
     */
    public Map<String, ShapeId> errorCodes() {
        return errorRegistry.codes();
    }

    /**
     * @return the operation field names by operation ShapeId, in ShapeId order (the {@code <Svc>ServiceSchemas} names)
     */
    public Map<ShapeId, String> operationFields() {
        return serviceSchemas.operationFields();
    }

    /**
     * @return the effective auth scheme IDs of each operation, in priority order
     */
    public Map<ShapeId, List<ShapeId>> effectiveAuthSchemes() {
        return authSchemes;
    }

    /**
     * @return the class
     */
    public TypeSpec spec() {
        TypeSpec.Builder type = TypeSpec.classBuilder(className)
                                        .addAnnotation(PoetUtils.generatedAnnotation())
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                        .addJavadoc("The operations of the $L service and its error registry, shared by "
                                                    + "the sync and async clients.\n", service.getId());
        if (usesServiceNamespace()) {
            type.addField(FieldSpec.builder(String.class, namespaceConstant, Modifier.PRIVATE, Modifier.STATIC,
                                            Modifier.FINAL)
                                   .initializer("$S", service.getId().getNamespace())
                                   .build());
        }
        for (FieldSpec id : errorRegistry.idFields(namespaceConstant)) {
            type.addField(id);
        }
        for (Map.Entry<ShapeId, String> scheme : authConstants.entrySet()) {
            type.addField(FieldSpec.builder(SchemaRuntimeType.SHAPE_ID, scheme.getValue(), Modifier.PRIVATE,
                                            Modifier.STATIC, Modifier.FINAL)
                                   .initializer(shapeId(scheme.getKey(), service, namespaceConstant))
                                   .build());
        }
        type.addField(FieldSpec.builder(SchemaRuntimeType.API_SERVICE, ServiceSchemasGenerator.SERVICE_FIELD,
                                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                               .addJavadoc("The $L service.\n", service.getId().getName())
                               .initializer("() -> $T.$L", serviceSchemas.className(),
                                            ServiceSchemasGenerator.SERVICE_FIELD)
                               .build());
        type.addField(errorRegistry.registryField());
        for (Map.Entry<ShapeId, String> operation : serviceSchemas.operationFields().entrySet()) {
            type.addField(operationField(model.expectShape(operation.getKey(), OperationShape.class),
                                         operation.getValue()));
        }
        type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        return type.build();
    }

    private boolean usesServiceNamespace() {
        if (errorRegistry.usesServiceNamespace()) {
            return true;
        }
        for (ShapeId scheme : authConstants.keySet()) {
            if (scheme.getNamespace().equals(service.getId().getNamespace())) {
                return true;
            }
        }
        return false;
    }

    private FieldSpec operationField(OperationShape operation, String fieldName) {
        TypeName input = SERIALIZABLE_STRUCT;
        TypeName output = SERIALIZABLE_STRUCT;
        CodeBlock.Builder init = CodeBlock.builder()
                                          .add("$T.<$T, $T>builder()$>", SchemaRuntimeType.SIMPLE_API_OPERATION, input,
                                               output)
                                          .add("\n.schema($T.$L)", serviceSchemas.className(), fieldName)
                                          .add("\n.inputSchema(() -> $L)", structureSchema(operation.getInputShape()))
                                          .add("\n.outputSchema(() -> $L)", structureSchema(operation.getOutputShape()));
        List<ShapeId> errors = new ArrayList<>(operation.getErrors());
        errors.addAll(service.getErrors());
        List<String> errorIds = errorRegistry.errorConstants(errors);
        if (!errorIds.isEmpty()) {
            init.add("\n.errorIds($L)", String.join(", ", errorIds));
        }
        init.add("\n.errorRegistry($L)", ErrorRegistrySpec.FIELD);
        List<String> auth = new ArrayList<>();
        for (ShapeId scheme : authSchemes.get(operation.getId())) {
            auth.add(authConstants.get(scheme));
        }
        if (!auth.isEmpty()) {
            init.add("\n.effectiveAuthSchemes($L)", String.join(", ", auth));
        }
        init.add("\n.service($L)", ServiceSchemasGenerator.SERVICE_FIELD)
            .add("\n.build()$<");
        return FieldSpec.builder(ParameterizedTypeName.get(SchemaRuntimeType.API_OPERATION, input, output), fieldName,
                                 Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .addJavadoc("The $L operation.\n", operation.getId().getName())
                        .initializer(init.build())
                        .build();
    }

    /**
     * An expression that reads the schema of an operation's input or output structure.
     */
    private CodeBlock structureSchema(ShapeId id) {
        if (Prelude.isPreludeShape(id)) {
            if (!"Unit".equals(id.getName())) {
                throw new SchemaCodegenException("Operation input or output " + id + " is not a structure");
            }
            return CodeBlock.of("$T.UNIT", SchemaRuntimeType.PRELUDE_SCHEMAS);
        }
        SchemaField field = schemas.order().field(id);
        if (field == null) {
            throw new SchemaCodegenException("Operation input or output " + id + " has no schema");
        }
        return schemas.fieldReference(field);
    }

    /**
     * Renders a {@code ShapeId} with the namespace constant for the service namespace, as the partitions do.
     */
    static CodeBlock shapeId(ShapeId id, ServiceShape service, String namespaceConstant) {
        if (id.getNamespace().equals(service.getId().getNamespace())) {
            return CodeBlock.of("$T.fromParts($L, $S)", SchemaRuntimeType.SHAPE_ID, namespaceConstant, id.getName());
        }
        return CodeBlock.of("$T.fromParts($S, $S)", SchemaRuntimeType.SHAPE_ID, id.getNamespace(), id.getName());
    }

    /**
     * Unique field names of the generated class.
     */
    static final class Names {
        private final Set<String> used = new HashSet<>();

        /**
         * @return whether the name was free
         */
        boolean reserve(String name) {
            return used.add(name);
        }

        /**
         * @return the name, or the name with the first free {@code _2}, {@code _3}, … suffix
         */
        String allocate(String name) {
            String candidate = name;
            for (int i = 2; used.contains(candidate); i++) {
                candidate = name + "_" + i;
            }
            used.add(candidate);
            return candidate;
        }
    }
}
