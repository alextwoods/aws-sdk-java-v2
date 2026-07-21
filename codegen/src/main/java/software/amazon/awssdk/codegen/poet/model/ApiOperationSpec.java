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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Generates a static singleton {@code ApiOperation} implementation per service operation.
 * These provide the smithy-java protocol layer with operation/input/output schemas,
 * error registries, and builder factories — without needing DynamicClient or a Smithy model at runtime.
 */
public class ApiOperationSpec implements ClassSpec {

    private static final ClassName SCHEMA = ClassName.get("software.amazon.smithy.java.core.schema", "Schema");
    private static final ClassName SHAPE_ID = ClassName.get("software.amazon.smithy.model.shapes", "ShapeId");
    private static final ClassName API_OPERATION = ClassName.get("software.amazon.smithy.java.core.schema", "ApiOperation");
    private static final ClassName API_SERVICE = ClassName.get("software.amazon.smithy.java.core.schema", "ApiService");
    private static final ClassName SHAPE_BUILDER = ClassName.get("software.amazon.smithy.java.core.schema", "ShapeBuilder");
    private static final ClassName SERIALIZABLE_STRUCT =
        ClassName.get("software.amazon.smithy.java.core.schema", "SerializableStruct");
    private static final ClassName TYPE_REGISTRY =
        ClassName.get("software.amazon.smithy.java.core.serde", "TypeRegistry");
    private static final ClassName LIST = ClassName.get("java.util", "List");

    private final IntermediateModel model;
    private final OperationModel operationModel;
    private final PoetExtension poetExtensions;
    private final ClassName inputType;
    private final ClassName outputType;

    public ApiOperationSpec(IntermediateModel model, OperationModel operationModel) {
        this.model = model;
        this.operationModel = operationModel;
        this.poetExtensions = new PoetExtension(model);
        this.inputType = operationModel.getInputShape() != null
            ? poetExtensions.getModelClass(operationModel.getInputShape().getShapeName())
            : ClassName.get("software.amazon.smithy.java.core.schema", "SerializableStruct");
        this.outputType = operationModel.getOutputShape() != null
            ? poetExtensions.getModelClass(operationModel.getOutputShape().getShapeName())
            : poetExtensions.getModelClass(operationModel.getOperationName() + "Response");
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName self = className();
        ParameterizedTypeName apiOpType = ParameterizedTypeName.get(API_OPERATION, inputType, outputType);

        return TypeSpec.classBuilder(self)
                       .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addSuperinterface(apiOpType)
                       .addField(instanceField(self))
                       .addField(schemaField())
                       .addField(typeRegistryField())
                       .addField(schemesField())
                       .addMethod(instanceMethod(self))
                       .addMethod(privateConstructor())
                       .addMethod(inputBuilderMethod())
                       .addMethod(outputBuilderMethod())
                       .addMethod(schemaMethod())
                       .addMethod(inputSchemaMethod())
                       .addMethod(outputSchemaMethod())
                       .addMethod(errorRegistryMethod())
                       .addMethod(errorSchemasMethod())
                       .addMethod(effectiveAuthSchemesMethod())
                       .addMethod(serviceMethod())
                       .build();
    }

    @Override
    public ClassName className() {
        String basePackage = model.getMetadata().getFullModelPackageName();
        // Place operations in a sibling "operations" package
        String operationsPackage = basePackage.replace(".model", ".operations");
        return ClassName.get(operationsPackage, operationModel.getOperationName() + "Operation");
    }

    private FieldSpec instanceField(ClassName self) {
        return FieldSpec.builder(self, "INSTANCE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", self)
                        .build();
    }

    private FieldSpec schemaField() {
        return FieldSpec.builder(SCHEMA, "$SCHEMA", Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.createOperation($T.from($S))",
                                     SCHEMA, SHAPE_ID, smithyOperationId())
                        .build();
    }

    private FieldSpec typeRegistryField() {
        CodeBlock.Builder builder = CodeBlock.builder()
            .add("$T.builder()\n", TYPE_REGISTRY);

        List<String> exceptions = operationModel.getExceptions().stream()
            .map(e -> e.getExceptionName())
            .collect(Collectors.toList());

        for (String exceptionName : exceptions) {
            ClassName exceptionClass = poetExtensions.getModelClass(exceptionName);
            builder.add("    .putType($T.$$SCHEMA.id(), $T.class, $T::builder)\n",
                        exceptionClass, exceptionClass, exceptionClass);
        }
        builder.add("    .build()");

        return FieldSpec.builder(TYPE_REGISTRY, "TYPE_REGISTRY", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(builder.build())
                        .build();
    }

    private FieldSpec schemesField() {
        return FieldSpec.builder(ParameterizedTypeName.get(LIST, SHAPE_ID), "SCHEMES",
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.of($T.from($S))", LIST, SHAPE_ID, "aws.auth#sigv4")
                        .build();
    }

    private MethodSpec instanceMethod(ClassName self) {
        return MethodSpec.methodBuilder("instance")
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .returns(self)
                         .addStatement("return INSTANCE")
                         .build();
    }

    private MethodSpec privateConstructor() {
        return MethodSpec.constructorBuilder()
                         .addModifiers(Modifier.PRIVATE)
                         .build();
    }

    private MethodSpec inputBuilderMethod() {
        ParameterizedTypeName returnType = ParameterizedTypeName.get(SHAPE_BUILDER, inputType);
        return MethodSpec.methodBuilder("inputBuilder")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(returnType)
                         .addStatement("return $T.builder()", inputType)
                         .build();
    }

    private MethodSpec outputBuilderMethod() {
        ParameterizedTypeName returnType = ParameterizedTypeName.get(SHAPE_BUILDER, outputType);
        return MethodSpec.methodBuilder("outputBuilder")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(returnType)
                         .addStatement("return $T.builder()", outputType)
                         .build();
    }

    private MethodSpec schemaMethod() {
        return MethodSpec.methodBuilder("schema")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(SCHEMA)
                         .addStatement("return $$SCHEMA")
                         .build();
    }

    private MethodSpec inputSchemaMethod() {
        return MethodSpec.methodBuilder("inputSchema")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(SCHEMA)
                         .addStatement("return $T.$$SCHEMA", inputType)
                         .build();
    }

    private MethodSpec outputSchemaMethod() {
        return MethodSpec.methodBuilder("outputSchema")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(SCHEMA)
                         .addStatement("return $T.$$SCHEMA", outputType)
                         .build();
    }

    private MethodSpec errorRegistryMethod() {
        return MethodSpec.methodBuilder("errorRegistry")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(TYPE_REGISTRY)
                         .addStatement("return TYPE_REGISTRY")
                         .build();
    }

    private MethodSpec errorSchemasMethod() {
        CodeBlock.Builder listBuilder = CodeBlock.builder().add("$T.of(", LIST);
        List<String> exceptions = operationModel.getExceptions().stream()
            .map(e -> e.getExceptionName())
            .collect(Collectors.toList());
        for (int i = 0; i < exceptions.size(); i++) {
            ClassName exceptionClass = poetExtensions.getModelClass(exceptions.get(i));
            listBuilder.add("$T.$$SCHEMA", exceptionClass);
            if (i < exceptions.size() - 1) {
                listBuilder.add(", ");
            }
        }
        listBuilder.add(")");

        return MethodSpec.methodBuilder("errorSchemas")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(ParameterizedTypeName.get(LIST, SCHEMA))
                         .addStatement("return $L", listBuilder.build())
                         .build();
    }

    private MethodSpec effectiveAuthSchemesMethod() {
        return MethodSpec.methodBuilder("effectiveAuthSchemes")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(ParameterizedTypeName.get(LIST, SHAPE_ID))
                         .addStatement("return SCHEMES")
                         .build();
    }

    private MethodSpec serviceMethod() {
        ClassName serviceClass = apiServiceClassName();
        return MethodSpec.methodBuilder("service")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(API_SERVICE)
                         .addStatement("return $T.instance()", serviceClass)
                         .build();
    }

    private String smithyOperationId() {
        String namespace = "com.amazonaws." + model.getMetadata().getEndpointPrefix();
        return namespace + "#" + operationModel.getOperationName();
    }

    private ClassName apiServiceClassName() {
        String basePackage = model.getMetadata().getFullModelPackageName();
        String operationsPackage = basePackage.replace(".model", ".operations");
        String serviceName = model.getMetadata().getServiceName();
        return ClassName.get(operationsPackage, serviceName + "ApiService");
    }
}
