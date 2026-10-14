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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.awssdk.codegen.schema.TraitRenderContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generates {@code <Svc>ServiceSchemas} in the client's {@code internal} package: {@code SERVICE}, the service schema
 * with the service's included traits (protocol, auth and {@code awsQueryCompatible} traits among them), and one
 * operation schema per operation the service contains, with the operation's included traits (design A.17,
 * Requirement 10.2). Operation schemas hold no input, output or error references; {@code <Svc>Operations} (task 3.15)
 * pairs them with the input, output and error schemas.
 */
@SdkInternalApi
public final class ServiceSchemasGenerator {
    static final String SERVICE_FIELD = "SERVICE";

    private final Model model;
    private final ServiceShape service;
    private final SchemaTraits traits;
    private final ClassName className;
    private final Map<ShapeId, String> operationFields;

    ServiceSchemasGenerator(Model model, ServiceShape service, SchemaTraits traits, String internalPackage,
                            String serviceName) {
        this.model = model;
        this.service = service;
        this.traits = traits;
        this.className = className(internalPackage, serviceName);
        Set<String> used = new HashSet<>(Collections.singleton(SERVICE_FIELD));
        Map<ShapeId, String> fields = new LinkedHashMap<>();
        for (OperationShape operation : new TreeSet<>(TopDownIndex.of(model).getContainedOperations(service))) {
            String name = SchemaFieldOrder.toSchemaName(operation.getId().getName());
            if (used.contains(name)) {
                name = SchemaFieldOrder.toSchemaName(operation.getId().toString().replace('.', '_').replace('#', '_'));
            }
            String candidate = name;
            for (int i = 2; used.contains(candidate); i++) {
                candidate = name + "_" + i;
            }
            used.add(candidate);
            fields.put(operation.getId(), candidate);
        }
        this.operationFields = Collections.unmodifiableMap(fields);
        // Render every trait now, so a trait that cannot be rendered fails before any code is written.
        traits.forShape(service);
        for (ShapeId operation : operationFields.keySet()) {
            traits.forShape(model.expectShape(operation));
        }
    }

    /**
     * @return the generated class name
     */
    public ClassName className() {
        return className;
    }

    /**
     * @param internalPackage the client's internal package
     * @param serviceName the class name prefix, such as {@code Sqs}
     * @return the name of the {@code <Svc>ServiceSchemas} class
     */
    static ClassName className(String internalPackage, String serviceName) {
        return ClassName.get(internalPackage, serviceName + "ServiceSchemas");
    }

    /**
     * @return the operation schema field names by operation ShapeId, in ShapeId order
     */
    public Map<ShapeId, String> operationFields() {
        return operationFields;
    }

    /**
     * @return the class
     */
    public TypeSpec spec() {
        TraitRenderContext ids = TraitRenderContext.defaultContext();
        TypeSpec.Builder type = TypeSpec.classBuilder(className)
                                        .addAnnotation(PoetUtils.generatedAnnotation())
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                        .addJavadoc("The schemas of the $L service and its operations.\n",
                                                    service.getId());
        type.addField(FieldSpec.builder(SchemaRuntimeType.SCHEMA, SERVICE_FIELD, Modifier.PUBLIC, Modifier.STATIC,
                                        Modifier.FINAL)
                               .initializer("$T.createService($L$L)", SchemaRuntimeType.SCHEMA,
                                            ids.shapeId(service.getId()),
                                            SchemaTraits.arguments(traits.forShape(service)))
                               .build());
        for (Map.Entry<ShapeId, String> entry : operationFields.entrySet()) {
            CodeBlock traitArgs = SchemaTraits.arguments(traits.forShape(model.expectShape(entry.getKey())));
            type.addField(FieldSpec.builder(SchemaRuntimeType.SCHEMA, entry.getValue(), Modifier.PUBLIC,
                                            Modifier.STATIC, Modifier.FINAL)
                                   .initializer("$T.createOperation($L$L)", SchemaRuntimeType.SCHEMA,
                                                ids.shapeId(entry.getKey()), traitArgs)
                                   .build());
        }
        type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        return type.build();
    }
}
