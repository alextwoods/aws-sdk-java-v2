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
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.model.ModelPackageCompiler;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The Primary_Type_Registry {@code <Svc>TypeRegistry} (task 11.16; Requirements 22.6, 22.7): empty for a model whose
 * service lists no shapes, which is every Smithy model today, and, with shapes supplied through
 * {@link SchemaCodegen.Builder#serviceShapes}, exactly the listed structures. Golden files for both, and the generated
 * model package compiled with {@code --release 8} to check laziness and document deserialization.
 */
class TypeRegistrySpecTest {
    private static final String NS = "registry";
    private static final String PACKAGE = "software.amazon.awssdk.services.registry.model";
    private static final String MODEL =
        "$version: \"2.0\"\nnamespace registry\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "@service(sdkId: \"Registry\")\n"
        + "@sigv4(name: \"registry\")\n"
        + "@restJson1\n"
        + "service RegistryService { version: \"2024-01-01\", operations: [GetThing], errors: [ThingNotFound] }\n"
        + "@http(method: \"POST\", uri: \"/things\")\n"
        + "operation GetThing { input: GetThingRequest, output: GetThingResponse }\n"
        + "structure GetThingRequest { Thing: Thing, Choice: Choice, Color: Color, Label: Label }\n"
        + "structure GetThingResponse { Other: Other }\n"
        + "structure Thing { Name: String, Count: Integer }\n"
        + "structure Other { Name: String }\n"
        + "union Choice { Text: String }\n"
        + "enum Color { RED }\n"
        + "string Label\n"
        + "@error(\"client\") @httpError(404) structure ThingNotFound { message: String }\n"
        + "structure Orphan { Name: String }\n";

    /**
     * Structures, a union, an enum, a string and {@code smithy.api#Unit}; only the two structures are registered.
     */
    private static final List<ShapeId> LISTED = Arrays.asList(
        id("ThingNotFound"), id("Choice"), id("Color"), id("Label"), id("Thing"), ShapeId.from("smithy.api#Unit"));

    private static IntermediateModel model;
    private static TraitInitializers initializers;

    @TempDir
    Path dir;

    @BeforeAll
    static void buildModel() {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("registry.smithy", MODEL)
                            .assemble()
                            .unwrap();
        model = new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                               .model(smithy)
                                                               .customizationConfig(CustomizationConfig.create())
                                                               .build())
            .build();
        initializers = TraitInitializers.load(TypeRegistrySpecTest.class.getClassLoader());
    }

    @Test
    void create_serviceListsNoShapes_generatesTheEmptyRegistry() {
        SchemaCodegen codegen = SchemaCodegen.create(model, initializers);

        assertThat(codegen.typeRegistryClass().toString()).isEqualTo(PACKAGE + ".RegistryTypeRegistry");
        assertThat(codegen.typeRegistryShapes()).isEmpty();
        MatcherAssert.assertThat(registrySpec(codegen), generatesTo("EmptyTypeRegistry.java"));
    }

    @Test
    void serviceShapes_registersOnlyTheListedStructures() {
        SchemaCodegen codegen = withListedShapes(LISTED);

        assertThat(codegen.typeRegistryShapes()).containsExactly(id("Thing"), id("ThingNotFound"));
        MatcherAssert.assertThat(registrySpec(codegen), generatesTo("RegistryTypeRegistry.java"));
    }

    @Test
    void generatedRegistry_loadsNoModelClassUntilResolvedAndDeserializesDocuments() throws Exception {
        SchemaCodegen codegen = withListedShapes(LISTED);
        Path classes = ModelPackageCompiler.compile(model, codegen, dir);
        String partition = codegen.schemas().partitionClass(0).reflectionName();
        software.amazon.awssdk.schema.ShapeId thingId = runtimeId("Thing");

        try (TrackingClassLoader loader = new TrackingClassLoader(classes)) {
            Class<?> registryClass = Class.forName(codegen.typeRegistryClass().reflectionName(), true, loader);
            TypeRegistry registry = (TypeRegistry) registryClass.getField("INSTANCE").get(null);

            assertThat(registryClass.getField("INSTANCE").get(null)).isSameAs(registry);
            assertThat(registry.contains(thingId)).isTrue();
            assertThat(registry.contains(runtimeId("ThingNotFound"))).isTrue();
            for (String unregistered : new String[] {"Choice", "Color", "Label", "Other", "Orphan", "GetThingRequest"}) {
                assertThat(registry.contains(runtimeId(unregistered))).as(unregistered).isFalse();
                assertThat(registry.getShapeClass(runtimeId(unregistered))).as(unregistered).isNull();
            }
            assertThat(loader.loaded(partition)).as("partition loaded").isFalse();
            assertThat(loader.loaded(PACKAGE + ".Thing")).as("Thing loaded").isFalse();
            assertThat(loader.loaded(PACKAGE + ".ThingNotFoundException")).as("exception loaded").isFalse();

            Class<? extends SerializableStruct> thingClass = registry.getShapeClass(thingId);
            assertThat(thingClass.getName()).isEqualTo(PACKAGE + ".Thing");
            // Resolving Thing initializes its partition, whose shapeClass(X.class) literals load (but do not
            // initialize) the other classes of the partition, as for the Error_Registry.
            Schema thingSchema = (Schema) thingClass.getField("SCHEMA").get(null);
            ShapeBuilder<?> builder = registry.createBuilder(thingId);
            builder.setMemberValue(thingSchema.member("Name"), "n");
            builder.setMemberValue(thingSchema.member("Count"), 3);
            SerializableStruct thing = (SerializableStruct) builder.build();

            SerializableStruct deserialized = registry.deserialize(Document.of(thing));

            assertThat(deserialized).isInstanceOf(thingClass).isNotSameAs(thing).isEqualTo(thing);
            assertThat(registry.getShapeClass(runtimeId("ThingNotFound")).getName())
                .isEqualTo(PACKAGE + ".ThingNotFoundException");
        }
    }

    @Test
    void serviceShapes_shapeNotInTheModel_fails() {
        assertThatThrownBy(() -> withListedShapes(Collections.singletonList(id("Missing"))))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("registry#Missing")
            .hasMessageContaining("not in the model");
    }

    @Test
    void serviceShapes_structureOutsideTheServiceClosure_fails() {
        assertThatThrownBy(() -> withListedShapes(Collections.singletonList(id("Orphan"))))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("registry#Orphan")
            .hasMessageContaining("not in the service closure");
    }

    private static SchemaCodegen withListedShapes(List<ShapeId> shapes) {
        return SchemaCodegen.builderFor(model, initializers).serviceShapes(shapes).build();
    }

    private static ClassSpec registrySpec(SchemaCodegen codegen) {
        for (ClassSpec spec : codegen.classSpecs()) {
            if (spec.className().equals(codegen.typeRegistryClass())) {
                return spec;
            }
        }
        throw new AssertionError("No type registry class");
    }

    private static ShapeId id(String name) {
        return ShapeId.fromParts(NS, name);
    }

    private static software.amazon.awssdk.schema.ShapeId runtimeId(String name) {
        return software.amazon.awssdk.schema.ShapeId.fromParts(NS, name);
    }

    /**
     * Reports whether it has loaded a class, without loading it.
     */
    private static final class TrackingClassLoader extends URLClassLoader {
        private TrackingClassLoader(Path classes) throws IOException {
            super(new URL[] {classes.toUri().toURL()}, RuntimeCompiler.class.getClassLoader());
        }

        boolean loaded(String name) {
            return findLoadedClass(name) != null;
        }
    }
}
