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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Generates a static singleton {@code ApiService} implementation for the service.
 * This provides the service-level schema identity for the smithy-java protocol layer.
 */
public class ApiServiceSpec implements ClassSpec {

    private static final ClassName SCHEMA = ClassName.get("software.amazon.smithy.java.core.schema", "Schema");
    private static final ClassName SHAPE_ID = ClassName.get("software.amazon.smithy.model.shapes", "ShapeId");
    private static final ClassName API_SERVICE = ClassName.get("software.amazon.smithy.java.core.schema", "ApiService");

    private final IntermediateModel model;

    public ApiServiceSpec(IntermediateModel model) {
        this.model = model;
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName self = className();

        return TypeSpec.classBuilder(self)
                       .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                       .addAnnotation(PoetUtils.generatedAnnotation())
                       .addSuperinterface(API_SERVICE)
                       .addField(instanceField(self))
                       .addField(schemaField())
                       .addMethod(instanceMethod(self))
                       .addMethod(privateConstructor())
                       .addMethod(schemaMethod())
                       .build();
    }

    @Override
    public ClassName className() {
        String basePackage = model.getMetadata().getFullModelPackageName();
        String operationsPackage = basePackage.replace(".model", ".operations");
        String serviceName = model.getMetadata().getServiceName();
        return ClassName.get(operationsPackage, serviceName + "ApiService");
    }

    private FieldSpec instanceField(ClassName self) {
        return FieldSpec.builder(self, "INSTANCE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", self)
                        .build();
    }

    private FieldSpec schemaField() {
        return FieldSpec.builder(SCHEMA, "$SCHEMA", Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.createService($T.from($S))",
                                     SCHEMA, SHAPE_ID, smithyServiceId())
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

    private MethodSpec schemaMethod() {
        return MethodSpec.methodBuilder("schema")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(SCHEMA)
                         .addStatement("return $$SCHEMA")
                         .build();
    }

    /**
     * The service shape id, whose <em>name</em> is load-bearing rather than cosmetic: for awsJson
     * protocols smithy-java builds the {@code X-Amz-Target} header as
     * {@code <serviceShapeName>.<operationName>}, so the name has to be the model's target prefix
     * ({@code DynamoDB_20120810}) and not the SDK's Java-friendly service name ({@code DynamoDb}), which
     * would produce a target no service recognizes.
     *
     * <p>The namespace is not on the wire for awsJson, so the conventional {@code com.amazonaws.<prefix>}
     * is good enough. Services with no target prefix (non-awsJson protocols) fall back to the service
     * name; the target header is unused there.
     */
    private String smithyServiceId() {
        String namespace = "com.amazonaws." + model.getMetadata().getEndpointPrefix();
        String targetPrefix = model.getMetadata().getTargetPrefix();
        if (targetPrefix != null && !targetPrefix.isEmpty()) {
            return namespace + "#" + targetPrefix;
        }
        return namespace + "#" + model.getMetadata().getServiceName();
    }
}
