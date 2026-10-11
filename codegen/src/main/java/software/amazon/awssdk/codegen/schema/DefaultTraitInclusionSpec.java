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

package software.amazon.awssdk.codegen.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

/**
 * Generates the shared constants class with the fixed Trait_Inclusion_Policy lists of {@link DefaultTraitInclusion}, for
 * the Dynamic_Client's runtime port of the policy (task 13.1, Requirement 25.3).
 *
 * <p>The generated class holds only {@code List<String>} constants of absolute shape IDs and namespaces, so it compiles
 * in any module without {@code smithy-model} or the runtime schema module. The Dynamic_Client cannot depend on
 * {@code codegen}, so the lists reach it as generated source: the module that needs them generates this class (for
 * example into {@code software.amazon.awssdk.dynamicclient.schema.internal}) from the same {@link DefaultTraitInclusion}
 * that codegen's policy reads, and the two cannot drift.
 */
@SdkInternalApi
public final class DefaultTraitInclusionSpec implements ClassSpec {
    private static final TypeName STRING_LIST = ParameterizedTypeName.get(List.class, String.class);

    private final ClassName className;

    /**
     * @param className the class to generate
     */
    public DefaultTraitInclusionSpec(ClassName className) {
        this.className = Objects.requireNonNull(className, "className");
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addJavadoc("The fixed trait lists of the SDK's Trait_Inclusion_Policy (design A.16), generated from "
                                    + "codegen's {@code DefaultTraitInclusion}.\n")
                        .addField(field("DEFAULT_TRAITS", "Trait shape IDs every schema includes (Requirement 9.2).",
                                        DefaultTraitInclusion.DEFAULT_TRAITS))
                        .addField(field("EXCLUDED_CONSTRAINT_TRAITS",
                                        "Constraint trait shape IDs left out of the defaults and the protocol and auth "
                                        + "trait lists (Requirement 9.3).",
                                        DefaultTraitInclusion.EXCLUDED_CONSTRAINT_TRAITS))
                        .addField(field("HARD_EXCLUDED_TRAITS", "Trait shape IDs never included (Requirement 9.4).",
                                        DefaultTraitInclusion.HARD_EXCLUDED_TRAITS))
                        .addField(field("HARD_EXCLUDED_NAMESPACES",
                                        "Namespaces whose traits are never included (Requirement 9.4).",
                                        DefaultTraitInclusion.HARD_EXCLUDED_NAMESPACES))
                        .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                        .build();
    }

    @Override
    public ClassName className() {
        return className;
    }

    private static FieldSpec field(String name, String javadoc, Collection<?> values) {
        List<CodeBlock> elements = new ArrayList<>(values.size());
        for (Object value : values) {
            elements.add(CodeBlock.of("$S", value.toString()));
        }
        return FieldSpec.builder(STRING_LIST, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .addJavadoc(javadoc + "\n")
                        .initializer("$T.unmodifiableList($L)", Collections.class,
                                     DocumentLiterals.list(String.class, elements))
                        .build();
    }
}
