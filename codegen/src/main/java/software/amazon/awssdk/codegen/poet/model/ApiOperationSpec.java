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
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Generates a static singleton {@code ApiOperation} implementation per service operation, for the
 * smithy-java serde path: the operation schema (with its {@code @http} trait for the REST protocols), the
 * input and output schemas, and the builders smithy's codecs deserialize into. Errors are still
 * unmarshalled by v2, so the error registry is empty and no error schemas are listed.
 */
public class ApiOperationSpec implements ClassSpec {

    private static final ClassName SCHEMA = ClassName.get("software.amazon.smithy.java.core.schema", "Schema");
    private static final ClassName SHAPE_ID = ClassName.get("software.amazon.smithy.model.shapes", "ShapeId");
    private static final ClassName HTTP_TRAIT = ClassName.get("software.amazon.smithy.model.traits", "HttpTrait");
    private static final ClassName URI_PATTERN = ClassName.get("software.amazon.smithy.model.pattern", "UriPattern");
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

        TypeSpec.Builder type = TypeSpec.classBuilder(self);

        return type.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                   .addAnnotation(PoetUtils.generatedAnnotation())
                   .addSuperinterface(apiOpType)
                   .addField(instanceField(self))
                   .addField(schemaField())
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

    /**
     * The operation schema, carrying an {@code @http} trait whenever the model has one.
     *
     * <p>awsJson never needed it — every call is {@code POST /} and the operation is named by the
     * {@code X-Amz-Target} header — so the original version of this method emitted a bare
     * {@code createOperation(id)}. A REST protocol cannot build a request line without it:
     * {@code HttpBindingClientProtocol} takes the method, the URI pattern that
     * {@code @httpLabel} members are substituted into, and the success status code from this trait.
     * Without it every member bound to the URI has nowhere to go.
     *
     * <p>The values come from the input shape's marshaller, which is where C2J's {@code http} block
     * lands in the intermediate model. Only the REST protocols get the trait (see
     * {@link #httpTraitInfo()}), so awsJson output is byte-for-byte what it was.
     */
    private FieldSpec schemaField() {
        HttpTraitInfo http = httpTraitInfo();
        if (http == null) {
            return FieldSpec.builder(SCHEMA, "$SCHEMA", Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.createOperation($T.from($S))",
                                         SCHEMA, SHAPE_ID, smithyOperationId())
                            .build();
        }
        CodeBlock.Builder init = CodeBlock.builder()
            .add("$T.createOperation($T.from($S),\n", SCHEMA, SHAPE_ID, smithyOperationId())
            .add("    $T.builder().method($S).uri($T.parse($S))",
                 HTTP_TRAIT, http.method, URI_PATTERN, http.uri);
        if (http.code != null) {
            init.add(".code($L)", http.code);
        }
        init.add(".build())");
        return FieldSpec.builder(SCHEMA, "$SCHEMA", Modifier.STATIC, Modifier.FINAL)
                        .initializer(init.build())
                        .build();
    }

    private static final class HttpTraitInfo {
        private final String method;
        private final String uri;
        private final String code;

        private HttpTraitInfo(String method, String uri, String code) {
            this.method = method;
            this.uri = uri;
            this.code = code;
        }
    }

    /**
     * Returns null for the RPC protocols, which do not want an {@code @http} trait.
     *
     * <p>The gate is on protocol rather than on the marshaller having a verb, because C2J writes
     * {@code {"method": "POST", "requestUri": "/"}} for awsJson and query too — reading it
     * unconditionally would emit a trait on every existing service and change generated output that
     * is currently byte-stable and benchmarked.
     */
    private HttpTraitInfo httpTraitInfo() {
        Protocol protocol = model.getMetadata().getProtocol();
        if (protocol != Protocol.REST_XML && protocol != Protocol.REST_JSON) {
            return null;
        }
        if (operationModel.getInputShape() == null || operationModel.getInputShape().getMarshaller() == null) {
            return null;
        }
        ShapeMarshaller marshaller = operationModel.getInputShape().getMarshaller();
        String verb = marshaller.getVerb();
        String requestUri = marshaller.getRequestUri();
        if (verb == null || requestUri == null) {
            return null;
        }
        // smithy requires an absolute path pattern; C2J writes "/" for the RPC protocols and a real
        // path for the REST ones, but be defensive about a leading slash either way.
        String uri = requestUri.startsWith("/") ? requestUri : "/" + requestUri;
        return new HttpTraitInfo(verb, uri, successStatusCode(marshaller));
    }

    /**
     * The modeled success status code, or null to let smithy default it to 200.
     *
     * <p>Most operations do not model one; S3 is unusual in that several do (204 on DeleteObject, 206
     * on a ranged GetObject). An unparseable value defers to smithy rather than guessing, since a
     * wrong number here would misdescribe a successful response.
     */
    private String successStatusCode(ShapeMarshaller marshaller) {
        String code = marshaller.getResponseCode();
        if (code == null) {
            return null;
        }
        try {
            return String.valueOf(Integer.parseInt(code.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
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
                         .addStatement("return $T.empty()", TYPE_REGISTRY)
                         .build();
    }

    private MethodSpec errorSchemasMethod() {
        return MethodSpec.methodBuilder("errorSchemas")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(ParameterizedTypeName.get(LIST, SCHEMA))
                         .addStatement("return $T.of()", LIST)
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
