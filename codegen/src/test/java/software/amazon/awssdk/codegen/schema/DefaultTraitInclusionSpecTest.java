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

import static org.assertj.core.api.Assertions.assertThat;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.TraitFactories;

class DefaultTraitInclusionSpecTest {
    private static final ClassName NAME = ClassName.get("software.amazon.awssdk.dynamicclient.schema.internal",
                                                        "DefaultTraitInclusion");

    @Test
    void generatedConstantsMatchTheCodegenLists(@TempDir Path dir) throws Exception {
        Class<?> generated = RuntimeCompiler.compileAndLoad(PoetUtils.buildJavaFile(new DefaultTraitInclusionSpec(NAME)), dir);

        assertThat(constant(generated, "DEFAULT_TRAITS")).isEqualTo(strings(DefaultTraitInclusion.DEFAULT_TRAITS));
        assertThat(constant(generated, "EXCLUDED_CONSTRAINT_TRAITS"))
            .isEqualTo(strings(DefaultTraitInclusion.EXCLUDED_CONSTRAINT_TRAITS));
        assertThat(constant(generated, "HARD_EXCLUDED_TRAITS")).isEqualTo(strings(DefaultTraitInclusion.HARD_EXCLUDED_TRAITS));
        assertThat(constant(generated, "HARD_EXCLUDED_NAMESPACES"))
            .isEqualTo(strings(DefaultTraitInclusion.HARD_EXCLUDED_NAMESPACES));
    }

    @Test
    void generationIsDeterministic() {
        JavaFile first = PoetUtils.buildJavaFile(new DefaultTraitInclusionSpec(NAME));
        JavaFile second = PoetUtils.buildJavaFile(new DefaultTraitInclusionSpec(NAME));
        assertThat(first.toString()).isEqualTo(second.toString());
    }

    @Test
    void everyDefaultTraitHasATypedRuntimeTrait() {
        for (software.amazon.smithy.model.shapes.ShapeId id : DefaultTraitInclusion.DEFAULT_TRAITS) {
            assertThat(TraitFactories.factoryFor(ShapeId.from(id.toString()))).as(id.toString()).isNotNull();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> constant(Class<?> type, String name) throws ReflectiveOperationException {
        return (List<String>) type.getField(name).get(null);
    }

    private static List<String> strings(Collection<?> values) {
        List<String> result = new ArrayList<>();
        values.forEach(v -> result.add(v.toString()));
        return result;
    }
}
