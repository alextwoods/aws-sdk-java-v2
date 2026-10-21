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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.schema.SchemaFieldOrder.SchemaField;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.TraitInclusionConfig;
import software.amazon.awssdk.codegen.schema.TraitInclusionPolicy;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaIndex;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.schema.traits.RestJson1Trait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;
import software.amazon.awssdk.schema.traits.SigV4Trait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;

/**
 * Generates the schema classes of a small model (recursive components, prelude targets, enums, unions, errors, a shape
 * from another namespace), compiles them with {@code --release 8} against the runtime schema module, loads them and
 * walks the schemas.
 */
class SchemaCodegenTest {
    private static final String MODEL_PACKAGE = "test.demo.model";
    private static final String INTERNAL_PACKAGE = "test.demo.internal";
    private static final ShapeId SERVICE = ShapeId.from("demo.schemas#DemoService");
    private static final String NS = "demo.schemas";

    @TempDir
    Path dir;

    @Test
    void closure_coversOperationsErrorsAndReachableShapesOnly() {
        SchemaCodegen codegen = generate(model(false), SchemaCodegen.DEFAULT_PARTITION_BUDGET);
        Set<String> ids = codegen.order().fields().stream().map(f -> f.schemaId().toString())
                                 .collect(Collectors.toSet());

        assertThat(ids).contains(NS + "#GetThingRequest", NS + "#GetThingResponse", NS + "#PutThingRequest",
                                 NS + "#ThingBody", NS + "#TreeNode", NS + "#TreeNodeList", NS + "#SelfRef",
                                 NS + "#MutualA", NS + "#MutualB", NS + "#Color", NS + "#Level", NS + "#Choice",
                                 NS + "#TagMap", NS + "#EpochTime", NS + "#SecretString", NS + "#NotFound",
                                 NS + "#ServiceFault", "other.ns#External");
        assertThat(ids).doesNotContain(NS + "#NotInTheClosure", "smithy.api#Unit", "smithy.api#String");
        assertThat(ids).hasSize(18);
    }

    @Test
    void order_initializesEveryFieldAfterTheFieldsItReferences() {
        SchemaFieldOrder order = generate(model(false), SchemaCodegen.DEFAULT_PARTITION_BUDGET).order();
        List<String> names = order.fields().stream().map(f -> f.shape().getId().getName()).collect(Collectors.toList());

        assertThat(names.indexOf("ThingBody")).isLessThan(names.indexOf("PutThingRequest"));
        assertThat(names.indexOf("TreeNode")).isLessThan(names.indexOf("GetThingResponse"));
        assertThat(names.indexOf("TagMap")).isLessThan(names.indexOf("GetThingResponse"));
        assertThat(names.indexOf("External")).isLessThan(names.indexOf("GetThingResponse"));
        // Inside the tree component, the list is built first and defers its edge back to TreeNode.
        assertThat(names.indexOf("TreeNodeList")).isLessThan(names.indexOf("TreeNode"));
    }

    @Test
    void recursiveComponents_deferOnlyCycleClosingEdges() {
        SchemaFieldOrder order = generate(model(false), SchemaCodegen.DEFAULT_PARTITION_BUDGET).order();

        List<String> deferred = new ArrayList<>();
        List<String> builders = new ArrayList<>();
        for (SchemaField field : order.fields()) {
            field.shape().members().forEach(m -> {
                if (field.isDeferred(m)) {
                    deferred.add(m.getId().toString());
                }
            });
            if (field.hasBuilder()) {
                builders.add(field.builderName());
            }
            assertThat(field.isRecursive())
                .as(field.toString())
                .isEqualTo(Arrays.asList("TreeNode", "TreeNodeList", "SelfRef", "MutualA", "MutualB")
                                 .contains(field.shape().getId().getName()));
        }
        assertThat(deferred).containsExactlyInAnyOrder(NS + "#TreeNodeList$member", NS + "#SelfRef$Next",
                                                       NS + "#MutualB$A");
        assertThat(builders).containsExactlyInAnyOrder("TREE_NODE_BUILDER", "SELF_REF_BUILDER", "MUTUAL_A_BUILDER");
    }

    @Test
    void generatedSchemas_compileAndDescribeTheModel() throws Exception {
        SchemaCodegen codegen = generate(model(false), SchemaCodegen.DEFAULT_PARTITION_BUDGET);
        assertThat(codegen.order().partitions()).hasSize(1);
        Path classes = compile(codegen);

        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
            SchemaIndex index = index(codegen, loader);
            for (SchemaField field : codegen.order().fields()) {
                Schema schema = index.getSchema(runtimeId(field.schemaId()));
                assertThat(schema).as(field.toString()).isNotNull();
                assertThat(schema.id().toString()).isEqualTo(field.schemaId().toString());
                assertThat(schema.isMember()).isFalse();
            }
            List<Schema> visited = new ArrayList<>();
            index.visit(visited::add);
            assertThat(visited).hasSize(codegen.order().fields().size());
            assertThat(index.getSchema(software.amazon.awssdk.schema.ShapeId.fromParts(NS, "NotInTheClosure"))).isNull();

            Schema request = schema(index, "GetThingRequest");
            assertThat(request.type()).isEqualTo(ShapeType.STRUCTURE);
            assertThat(request.members().stream().map(Schema::memberName).collect(Collectors.toList()))
                .containsExactly("Id", "Verbose", "Count");
            assertThat(request.member("Id").memberTarget()).isSameAs(PreludeSchemas.STRING);
            assertThat(request.member("Id").getDirectTrait(TraitKey.REQUIRED_TRAIT)).isSameAs(RequiredTrait.INSTANCE);
            assertThat(request.member("Count").memberTarget()).isSameAs(PreludeSchemas.PRIMITIVE_INTEGER);
            assertThat(request.member("Count").getDirectTrait(TraitKey.get(JsonNameTrait.class)).getValue())
                .isEqualTo("count");

            Schema response = schema(index, "GetThingResponse");
            // Member and target traits are emitted apart and merged at runtime (Requirement 10.4).
            Schema secret = response.member("Secret");
            assertThat(secret.getDirectTrait(TraitKey.get(SensitiveTrait.class))).isNull();
            assertThat(secret.getTrait(TraitKey.get(SensitiveTrait.class))).isNotNull();
            assertThat(schema(index, "EpochTime").getTrait(TraitKey.get(TimestampFormatTrait.class))).isNotNull();
            assertThat(response.member("Color").memberTarget().stringEnumValues()).containsExactly("red", "green");
            assertThat(response.member("Level").memberTarget().intEnumValues()).containsExactly(1, 10);
            assertThat(response.member("Choice").memberTarget().type()).isEqualTo(ShapeType.UNION);
            assertThat(response.member("Choice").memberTarget().member("Empty").memberTarget())
                .isSameAs(PreludeSchemas.UNIT);
            assertThat(response.member("Tags").memberTarget().mapValueMember().memberTarget())
                .isSameAs(PreludeSchemas.STRING);
            assertThat(response.member("External").memberTarget().id().getNamespace()).isEqualTo("other.ns");
            assertThat(response.member("Doc").memberTarget()).isSameAs(PreludeSchemas.DOCUMENT);

            // Recursive targets resolve to the indexed schema instances.
            Schema tree = schema(index, "TreeNode");
            Schema treeList = schema(index, "TreeNodeList");
            assertThat(response.member("Root").memberTarget()).isSameAs(tree);
            assertThat(tree.member("Children").memberTarget()).isSameAs(treeList);
            assertThat(treeList.listMember().memberTarget()).isSameAs(tree);
            Schema self = schema(index, "SelfRef");
            assertThat(self.member("Next").memberTarget()).isSameAs(self);
            Schema a = schema(index, "MutualA");
            Schema b = schema(index, "MutualB");
            assertThat(a.member("B").memberTarget()).isSameAs(b);
            assertThat(b.member("A").memberTarget()).isSameAs(a);

            assertThat(schema(index, "NotFound").members()).hasSize(1);
            assertThat(schema(index, "ThingBody").member("Huge").memberTarget()).isSameAs(PreludeSchemas.BIG_INTEGER);
        }
    }

    @Test
    void serviceSchemas_carryServiceAndOperationTraits() throws Exception {
        SchemaCodegen codegen = generate(model(false), SchemaCodegen.DEFAULT_PARTITION_BUDGET);
        Path classes = compile(codegen);
        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
            Class<?> type = Class.forName(codegen.serviceSchemasClass().reflectionName(), true, loader);
            Schema service = (Schema) type.getField("SERVICE").get(null);
            assertThat(service.type()).isEqualTo(ShapeType.SERVICE);
            assertThat(service.id().toString()).isEqualTo(SERVICE.toString());
            assertThat(service.getTrait(TraitKey.get(RestJson1Trait.class))).isNotNull();
            assertThat(service.getTrait(TraitKey.get(SigV4Trait.class))).isNotNull();
            assertThat(service.traits()).noneMatch(t -> t.traitId().toString().equals("smithy.api#documentation"));

            Schema getThing = (Schema) type.getField("GET_THING").get(null);
            assertThat(getThing.type()).isEqualTo(ShapeType.OPERATION);
            assertThat(getThing.getTrait(TraitKey.get(HttpTrait.class))).isNotNull();
            assertThat(fieldNames(type)).containsExactly("SERVICE", "GET_THING", "PING", "PUT_THING");
        }
    }

    @Test
    void lowBudget_splitsIntoPartitionsThatEachInitializeFirst() throws Exception {
        SchemaCodegen codegen = generate(model(false), 200);
        List<List<SchemaField>> partitions = codegen.order().partitions();
        assertThat(partitions.size()).isGreaterThan(3);

        // A recursive component is never split, and a partition references only itself and earlier partitions.
        Map<Integer, Set<Integer>> unitPartitions = new java.util.HashMap<>();
        for (SchemaField field : codegen.order().fields()) {
            unitPartitions.computeIfAbsent(field.unit(), u -> new LinkedHashSet<>()).add(field.partition());
        }
        assertThat(unitPartitions.values()).allMatch(p -> p.size() == 1);
        Pattern reference = Pattern.compile("DemoSchemas(\\d*)\\.");
        for (int i = 0; i < partitions.size(); i++) {
            Matcher matcher = reference.matcher(source(codegen.classSpecs().get(i)));
            while (matcher.find()) {
                int target = matcher.group(1).isEmpty() ? 0 : Integer.parseInt(matcher.group(1));
                assertThat(target).as("partition %s references partition %s", i, target).isLessThan(i);
            }
        }

        Path classes = compile(codegen);
        for (int i = 0; i < partitions.size(); i++) {
            try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
                Class<?> partition = Class.forName(codegen.schemas().partitionClass(i).reflectionName(), true, loader);
                for (Field field : partition.getDeclaredFields()) {
                    if (field.getType() == Schema.class) {
                        field.setAccessible(true);
                        walk((Schema) field.get(null), new IdentityHashMap<>());
                    }
                }
            }
        }
        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
            assertThat(countSchemas(index(codegen, loader))).isEqualTo(codegen.order().fields().size());
        }
    }

    @Test
    void sizeEstimate_isAtLeastTheCompiledStaticInitializer() {
        SchemaCodegen codegen = generate(model(false), 400);
        Path classes = compile(codegen);
        for (int i = 0; i < codegen.order().partitions().size(); i++) {
            int estimate = 0;
            for (SchemaField field : codegen.order().partitions().get(i)) {
                estimate += codegen.schemas().estimate(field);
            }
            String file = codegen.schemas().partitionClass(i).reflectionName().replace('.', '/') + ".class";
            int actual = ClassFiles.staticInitializerLength(classes.resolve(file));
            assertThat(actual).as("partition %s", i).isPositive();
            assertThat(estimate).as("partition %s: estimate %s, actual %s", i, estimate, actual)
                                .isGreaterThanOrEqualTo(actual)
                                .isLessThanOrEqualTo(actual * 3);
        }
    }

    @Test
    void generation_isDeterministic() {
        String first = sources(generate(model(false), 300));
        String second = sources(generate(model(true), 300));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void namesThatCollideWithGeneratedClasses_useQualifiedFieldNames() {
        Model model = model(false);
        SchemaCodegen codegen = SchemaCodegen.builder()
                                             .model(model)
                                             .service(model.expectShape(SERVICE, ServiceShape.class))
                                             .policy(policy(model))
                                             .initializers(TraitInitializers.of(Collections.emptyList()))
                                             .modelPackage(MODEL_PACKAGE)
                                             .internalPackage(INTERNAL_PACKAGE)
                                             .serviceName("Demo")
                                             .reservedNames(Collections.singleton("COLOR"))
                                             .build();
        assertThat(codegen.order().field(ShapeId.from(NS + "#Color")).fieldName()).isEqualTo("DEMO_SCHEMAS_COLOR");
    }

    @Test
    void schemaClassNamedLikeAModelClass_failsBeforeWriting() {
        Model model = model(false);
        assertThatThrownBy(() -> SchemaCodegen.builder()
                                              .model(model)
                                              .service(model.expectShape(SERVICE, ServiceShape.class))
                                              .policy(policy(model))
                                              .initializers(TraitInitializers.of(Collections.emptyList()))
                                              .modelPackage(MODEL_PACKAGE)
                                              .internalPackage(INTERNAL_PACKAGE)
                                              .serviceName("Demo")
                                              .reservedNames(Collections.singleton("DemoSchemaIndex"))
                                              .build())
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("DemoSchemaIndex");
    }

    @Test
    void typesOnly_withRootShape_generatesOnlySchemasAndIndexForClosureAndRoots() throws Exception {
        Model model = model(false);
        ShapeId notInTheClosure = ShapeId.from(NS + "#NotInTheClosure");
        SchemaCodegen codegen = typesOnly(model).rootShapes(Collections.singleton(notInTheClosure)).build();

        assertThat(codegen.isTypesOnly()).isTrue();
        assertThat(codegen.classSpecs().stream().map(s -> s.className().toString()))
            .containsExactly(MODEL_PACKAGE + ".DemoSchemas", MODEL_PACKAGE + ".DemoSchemaIndex");
        assertThat(codegen.order().fields()).hasSize(19);
        assertThatThrownBy(codegen::operations).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(codegen::serviceSchemasClass).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(codegen::typeRegistryClass).isInstanceOf(IllegalStateException.class);

        try (URLClassLoader loader = RuntimeCompiler.classLoader(compile(codegen))) {
            Schema root = index(codegen, loader).getSchema(software.amazon.awssdk.schema.ShapeId.fromParts(
                NS, "NotInTheClosure"));
            assertThat(root.type()).isEqualTo(ShapeType.STRUCTURE);
            assertThat(root.member("Value").memberTarget()).isSameAs(PreludeSchemas.STRING);
        }
    }

    @Test
    void typesOnly_withClientOnlyInputs_fails() {
        Model model = model(false);
        assertThatThrownBy(() -> typesOnly(model).internalPackage(INTERNAL_PACKAGE).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internalPackage");
        assertThatThrownBy(() -> typesOnly(model).rootShapes(Collections.singleton(ShapeId.from(NS + "#Missing")))
                                                 .build())
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining(NS + "#Missing");
    }

    private static SchemaCodegen.Builder typesOnly(Model model) {
        return SchemaCodegen.builder()
                            .typesOnly(true)
                            .model(model)
                            .service(model.expectShape(SERVICE, ServiceShape.class))
                            .policy(policy(model))
                            .initializers(TraitInitializers.of(Collections.emptyList()))
                            .modelPackage(MODEL_PACKAGE)
                            .serviceName("Demo");
    }

    @Test
    void syntheticUnitShape_keepsItsOwnIdAndGetsTheUnitTrait() throws Exception {
        String src = "$version: \"2.0\"\nnamespace unit.test\n"
                     + "service UnitService { version: \"1\", operations: [Op] }\n"
                     + "operation Op { input: OpInput, output: Renamed }\n"
                     + "structure OpInput {}\n"
                     + "structure Renamed {}\n";
        Model base = Model.assembler().addUnparsedModel("unit.smithy", src).assemble().unwrap();
        // What ModelTransformer#createDedicatedInputAndOutput records: synthetic traits cannot be applied in IDL.
        Model model = base.toBuilder()
                          .addShape(base.expectShape(ShapeId.from("unit.test#OpInput"), StructureShape.class)
                                        .toBuilder()
                                        .addTrait(new OriginalShapeIdTrait(ShapeId.from("smithy.api#Unit")))
                                        .build())
                          .addShape(base.expectShape(ShapeId.from("unit.test#Renamed"), StructureShape.class)
                                        .toBuilder()
                                        .addTrait(new OriginalShapeIdTrait(ShapeId.from("unit.test#OpOutput")))
                                        .build())
                          .build();
        SchemaCodegen codegen = SchemaCodegen.builder()
                                             .model(model)
                                             .service(model.expectShape(ShapeId.from("unit.test#UnitService"),
                                                                        ServiceShape.class))
                                             .policy(TraitInclusionPolicy.forService(
                                                 model, model.expectShape(ShapeId.from("unit.test#UnitService"),
                                                                          ServiceShape.class),
                                                 TraitInclusionConfig.empty()))
                                             .initializers(TraitInitializers.of(Collections.emptyList()))
                                             .modelPackage("test.unit.model")
                                             .internalPackage("test.unit.internal")
                                             .serviceName("Unit")
                                             .build();
        Path classes = compile(codegen);
        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
            Schema input = index(codegen, loader)
                .getSchema(software.amazon.awssdk.schema.ShapeId.fromParts("unit.test", "OpInput"));
            assertThat(input).isNotNull();
            assertThat(input.getTrait(TraitKey.get(UnitTypeTrait.class))).isSameAs(UnitTypeTrait.INSTANCE);
            // The original ID names the schema (Requirement 10.6).
            SchemaIndex index = index(codegen, loader);
            assertThat(index.getSchema(software.amazon.awssdk.schema.ShapeId.fromParts("unit.test", "OpOutput")))
                .isNotNull();
            assertThat(index.getSchema(software.amazon.awssdk.schema.ShapeId.fromParts("unit.test", "Renamed")))
                .isNull();
        }
    }

    private static Schema schema(SchemaIndex index, String name) {
        Schema schema = index.getSchema(software.amazon.awssdk.schema.ShapeId.fromParts(NS, name));
        assertThat(schema).as(name).isNotNull();
        return schema;
    }

    private static int countSchemas(SchemaIndex index) {
        int[] count = {0};
        index.visit(s -> count[0]++);
        return count[0];
    }

    private static void walk(Schema schema, Map<Schema, Boolean> seen) {
        assertThat(schema).isNotNull();
        if (seen.put(schema, Boolean.TRUE) != null) {
            return;
        }
        for (Schema member : schema.members()) {
            assertThat(member.memberTarget()).as(member.id().toString()).isNotNull();
            walk(member.memberTarget(), seen);
        }
    }

    private static List<String> fieldNames(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == Schema.class) {
                names.add(field.getName());
            }
        }
        return names;
    }

    private static SchemaIndex index(SchemaCodegen codegen, ClassLoader loader) throws Exception {
        Class<?> type = Class.forName(codegen.schemaIndexClass().reflectionName(), true, loader);
        return (SchemaIndex) type.getConstructor().newInstance();
    }

    private static software.amazon.awssdk.schema.ShapeId runtimeId(ShapeId id) {
        return software.amazon.awssdk.schema.ShapeId.from(id.toString());
    }

    private Path compile(SchemaCodegen codegen) {
        List<JavaFile> files = new ArrayList<>();
        for (ClassSpec spec : codegen.classSpecs()) {
            files.add(PoetUtils.buildJavaFile(spec));
        }
        return RuntimeCompiler.compileAll(files, dir.resolve("compile-" + System.nanoTime()));
    }

    private static String source(ClassSpec spec) {
        return PoetUtils.buildJavaFile(spec).toString();
    }

    private static String sources(SchemaCodegen codegen) {
        StringBuilder all = new StringBuilder();
        for (ClassSpec spec : codegen.classSpecs()) {
            all.append(source(spec));
        }
        return all.toString();
    }

    static SchemaCodegen generate(Model model, int budget) {
        return SchemaCodegen.builder()
                            .model(model)
                            .service(model.expectShape(SERVICE, ServiceShape.class))
                            .policy(policy(model))
                            .initializers(TraitInitializers.of(Collections.emptyList()))
                            .modelPackage(MODEL_PACKAGE)
                            .internalPackage(INTERNAL_PACKAGE)
                            .serviceName("Demo")
                            .partitionBudget(budget)
                            .build();
    }

    private static TraitInclusionPolicy policy(Model model) {
        return TraitInclusionPolicy.forService(model, model.expectShape(SERVICE, ServiceShape.class),
                                               TraitInclusionConfig.empty());
    }

    /**
     * @param reversed whether to add the model files in the opposite order
     */
    static Model model(boolean reversed) {
        List<String> files = new ArrayList<>(Arrays.asList("demo.smithy", "other.smithy"));
        if (reversed) {
            Collections.reverse(files);
        }
        software.amazon.smithy.model.loader.ModelAssembler assembler =
            Model.assembler().discoverModels(Model.class.getClassLoader());
        for (String file : files) {
            assembler.addUnparsedModel(file, resource(file));
        }
        return assembler.assemble().unwrap();
    }

    private static String resource(String name) {
        URL url = SchemaCodegenTest.class.getResource(name);
        try (InputStream in = url.openStream()) {
            return new String(IoUtils.toByteArray(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
