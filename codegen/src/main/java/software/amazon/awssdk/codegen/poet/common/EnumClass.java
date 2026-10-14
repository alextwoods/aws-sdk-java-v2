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

package software.amazon.awssdk.codegen.poet.common;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;

public final class EnumClass extends AbstractEnumClass {
    private final String enumPackageName;
    private final CodeBlock schemaReference;

    public EnumClass(String enumPackage, ShapeModel shape) {
        this(enumPackage, shape, null);
    }

    /**
     * @param schemaReference the expression of the enum's schema; when not null, the enum implements
     *     {@code SmithyEnum} (task 3.7)
     */
    public EnumClass(String enumPackage, ShapeModel shape, CodeBlock schemaReference) {
        super(shape);
        this.enumPackageName = enumPackage;
        this.schemaReference = schemaReference;
    }

    @Override
    protected void addDeprecated(Builder enumBuilder) {
        PoetUtils.addDeprecated(enumBuilder::addAnnotation, getShape());
    }

    @Override
    protected void addJavadoc(Builder enumBuilder) {
        PoetUtils.addJavadoc(enumBuilder::addJavadoc, getShape());
    }

    @Override
    protected void addEnumConstants(Builder enumBuilder) {
        getShape().getEnums().forEach(
            e -> enumBuilder.addEnumConstant(e.getName(), TypeSpec.anonymousClassBuilder("$S", e.getValue()).build())
        );
    }

    @Override
    protected void addSchemaSupport(Builder enumBuilder, String valueField) {
        if (schemaReference == null) {
            return;
        }
        enumBuilder.addSuperinterface(SchemaRuntimeType.SMITHY_ENUM)
                   .addField(FieldSpec.builder(SchemaRuntimeType.SCHEMA, "SCHEMA", Modifier.PUBLIC, Modifier.STATIC,
                                               Modifier.FINAL)
                                      .addJavadoc("The schema of this enum.\n")
                                      .initializer(schemaReference)
                                      .build())
                   .addMethod(MethodSpec.methodBuilder("getValue")
                                        .addAnnotation(Override.class)
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(String.class)
                                        .addJavadoc("@return the wire value, or null for {@code UNKNOWN_TO_SDK_VERSION}\n")
                                        .addStatement("return $N", valueField)
                                        .build())
                   .addMethod(MethodSpec.methodBuilder("sdkSchema")
                                        .addAnnotation(Override.class)
                                        .addModifiers(Modifier.PUBLIC)
                                        .returns(SchemaRuntimeType.SCHEMA)
                                        .addStatement("return SCHEMA")
                                        .build());
    }

    @Override
    public ClassName className() {
        return ClassName.get(enumPackageName, getShape().getShapeName());
    }
}
