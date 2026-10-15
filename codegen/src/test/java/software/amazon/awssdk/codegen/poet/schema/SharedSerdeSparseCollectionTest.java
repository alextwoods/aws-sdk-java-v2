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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.model.ModelPackageCompiler;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.smithy.model.Model;

/**
 * {@code <Svc>SharedSerde} for sparse collections whose elements are lists or maps (task 3.34): the builder's copier
 * stores a null nested collection as the auto-construct instance, so the serde writes that instance as null and reads
 * null back as it. Dense collections and sparse collections of other element types keep plain null handling.
 */
class SharedSerdeSparseCollectionTest {
    private static final String MODEL =
        "$version: \"2.0\"\nnamespace sparse\n\n"
        + "use aws.api#service\n"
        + "use smithy.protocols#rpcv2Cbor\n"
        + "@service(sdkId: \"Sparse\")\n"
        + "@rpcv2Cbor\n"
        + "service SparseService { version: \"2024-01-01\", operations: [Put] }\n"
        + "operation Put { input: PutInput }\n"
        + "structure PutInput { SetMap: SparseSetMap, MapList: SparseMapList, DenseSetMap: DenseSetMap, "
        + "Strings: SparseStringList }\n"
        + "list StringSet { member: String }\n"
        + "map StringMap { key: String, value: String }\n"
        + "@sparse map SparseSetMap { key: String, value: StringSet }\n"
        + "@sparse list SparseMapList { member: StringMap }\n"
        + "map DenseSetMap { key: String, value: StringSet }\n"
        + "@sparse list SparseStringList { member: String }\n";

    private static IntermediateModel model;
    private static SchemaCodegen codegen;

    @TempDir
    Path dir;

    @BeforeAll
    static void generate() {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("sparse.smithy", MODEL)
                            .assemble()
                            .unwrap();
        model = new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                               .model(smithy)
                                                               .customizationConfig(CustomizationConfig.create())
                                                               .build())
            .build();
        codegen = SchemaCodegen.create(model, TraitInitializers.load(SharedSerdeSparseCollectionTest.class.getClassLoader()));
    }

    @Test
    void sharedSerde_withSparseNestedCollections_matchesGoldenFile() {
        MatcherAssert.assertThat(sharedSerde(), generatesTo("schemaserde/sparsesharedserde.java"));
    }

    @Test
    void generatedModelPackage_withSparseNestedCollections_compiles() {
        assertThat(ModelPackageCompiler.compile(model, codegen, dir)
                                       .resolve("software/amazon/awssdk/services/sparse/model/SparseSharedSerde.class"))
            .exists();
    }

    private static ClassSpec sharedSerde() {
        return codegen.classSpecs().stream().filter(s -> s.className().simpleName().equals("SparseSharedSerde"))
                      .findFirst().orElseThrow(() -> new AssertionError("SparseSharedSerde"));
    }
}
