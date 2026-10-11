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
import com.squareup.javapoet.TypeSpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.schema.SchemaFieldOrder.SchemaField;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generates {@code <Svc>SchemaIndex}, the service's {@code SchemaIndex}: a map from each closure shape's schema
 * ShapeId to its schema, filled by one {@code _initN()} method per partition so no method grows with the service
 * (design A.17, Requirements 10.3, 10.10). Port of smithy-java {@code SchemaIndexGenerator}.
 *
 * <p>The class is public with a public no-argument constructor, because {@code ServiceLoader} instantiates it from
 * {@code META-INF/services/software.amazon.awssdk.schema.SchemaIndex} ({@link #serviceFileName()}). It sits in the
 * model package because the partition classes are package-private. Unknown IDs return null.
 */
@SdkInternalApi
public final class SchemaIndexGenerator {
    private static final String MAP_FIELD = "SCHEMAS";

    private final SchemasGenerator schemas;
    private final ClassName className;
    private final ShapeId serviceId;

    SchemaIndexGenerator(SchemasGenerator schemas, ShapeId serviceId, String modelPackage, String serviceName) {
        this.schemas = schemas;
        this.serviceId = serviceId;
        this.className = ClassName.get(modelPackage, serviceName + "SchemaIndex");
    }

    /**
     * @return the path of the service registration file, relative to the resources directory
     */
    public static String serviceFileName() {
        return "META-INF/services/" + SchemaRuntimeType.SCHEMA_INDEX.reflectionName();
    }

    /**
     * @return the generated class name
     */
    public ClassName className() {
        return className;
    }

    /**
     * @return the class
     */
    public TypeSpec spec() {
        List<List<SchemaField>> partitions = schemas.order().partitions();
        int count = schemas.order().fields().size();
        ParameterizedTypeName mapType = ParameterizedTypeName.get(ClassName.get(Map.class), SchemaRuntimeType.SHAPE_ID,
                                                                  SchemaRuntimeType.SCHEMA);
        TypeSpec.Builder type = TypeSpec.classBuilder(className)
                                        .addAnnotation(PoetUtils.generatedAnnotation())
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                        .superclass(SchemaRuntimeType.SCHEMA_INDEX)
                                        .addJavadoc("The schema of every shape of the $L service, by shape ID.\n",
                                                    serviceId);
        type.addField(FieldSpec.builder(mapType, MAP_FIELD, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                               .initializer("new $T<>($L)", LinkedHashMap.class, (int) (count / 0.75f) + 1)
                               .build());
        CodeBlock.Builder init = CodeBlock.builder();
        for (int i = 0; i < partitions.size(); i++) {
            init.addStatement("_init$L()", i);
        }
        type.addStaticBlock(init.build());
        for (int i = 0; i < partitions.size(); i++) {
            MethodSpec.Builder method = MethodSpec.methodBuilder("_init" + i)
                                                  .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
            for (SchemaField field : partitions.get(i)) {
                method.addStatement("put($L)", schemas.fieldReference(field));
            }
            type.addMethod(method.build());
        }
        type.addMethod(MethodSpec.methodBuilder("put")
                                 .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                 .addParameter(SchemaRuntimeType.SCHEMA, "schema")
                                 .addStatement("$L.put(schema.id(), schema)", MAP_FIELD)
                                 .build());
        type.addMethod(MethodSpec.methodBuilder("getSchema")
                                 .addAnnotation(Override.class)
                                 .addModifiers(Modifier.PUBLIC)
                                 .returns(SchemaRuntimeType.SCHEMA)
                                 .addParameter(SchemaRuntimeType.SHAPE_ID, "id")
                                 .addStatement("return $L.get(id)", MAP_FIELD)
                                 .build());
        type.addMethod(MethodSpec.methodBuilder("visit")
                                 .addAnnotation(Override.class)
                                 .addModifiers(Modifier.PUBLIC)
                                 .addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                         SchemaRuntimeType.SCHEMA), "visitor")
                                 .beginControlFlow("for ($T schema : $L.values())", SchemaRuntimeType.SCHEMA, MAP_FIELD)
                                 .addStatement("visitor.accept(schema)")
                                 .endControlFlow()
                                 .build());
        return type.build();
    }
}
