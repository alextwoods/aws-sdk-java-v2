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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.poet.schema.SchemaSerdeModel;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Request and response classes that the IntermediateModel generates from one Smithy structure (task 3.34): a structure
 * that is both an operation's input and its output, and a structure that is the input of two operations. Each class
 * gets schema serde members and holds its own copy of the structure's schema, with its own builder, so the operations
 * can run on the schema-based path.
 */
class SchemaSerdeSharedShapeTest {
    private static final String PACKAGE = "software.amazon.awssdk.services.shared.model";
    private static final String MODEL =
        "$version: \"2.0\"\nnamespace shared\n\n"
        + "use aws.api#service\n"
        + "use smithy.protocols#rpcv2Cbor\n"
        + "@service(sdkId: \"Shared\")\n"
        + "@rpcv2Cbor\n"
        + "service SharedService { version: \"2024-01-01\", operations: [Echo, Enable, Disable] }\n"
        + "operation Echo { input: EchoInputOutput, output: EchoInputOutput }\n"
        + "operation Enable { input: ToggleInput }\n"
        + "operation Disable { input: ToggleInput }\n"
        + "structure EchoInputOutput { @required Name: String, Thing: Thing, Things: ThingList }\n"
        + "structure ToggleInput { Id: String }\n"
        + "structure Thing { Value: Integer }\n"
        + "list ThingList { member: Thing }\n";

    /**
     * A structure that is an operation's input and output and also a member target, of itself and of an error, as
     * awsJson1_1 {@code KitchenSink} (task 7.21): the request and response classes and the model class all represent it.
     */
    private static final String SINK_PACKAGE = "software.amazon.awssdk.services.sink.model";
    private static final String SINK_MODEL =
        "$version: \"2.0\"\nnamespace sink\n\n"
        + "use aws.api#service\n"
        + "use smithy.protocols#rpcv2Cbor\n"
        + "@service(sdkId: \"Sink\")\n"
        + "@rpcv2Cbor\n"
        + "service SinkService { version: \"2024-01-01\", operations: [Store, Fail] }\n"
        + "operation Store { input: KitchenSink, output: KitchenSink }\n"
        + "operation Fail { errors: [Failure] }\n"
        + "structure KitchenSink { Name: String, Recursive: KitchenSink, Sinks: KitchenSinkList }\n"
        + "list KitchenSinkList { member: KitchenSink }\n"
        + "@error(\"client\")\n"
        + "structure Failure { Data: KitchenSink }\n";

    private static IntermediateModel model;
    private static SchemaCodegen codegen;
    private static IntermediateModel sinkModel;
    private static SchemaCodegen sinkCodegen;

    @TempDir
    Path dir;

    @BeforeAll
    static void generate() {
        model = intermediateModel(MODEL);
        codegen = SchemaCodegen.create(model, TraitInitializers.load(SchemaSerdeSharedShapeTest.class.getClassLoader()));
        sinkModel = intermediateModel(SINK_MODEL);
        sinkCodegen = SchemaCodegen.create(sinkModel,
                                           TraitInitializers.load(SchemaSerdeSharedShapeTest.class.getClassLoader()));
    }

    private static IntermediateModel intermediateModel(String source) {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("model.smithy", source)
                            .assemble()
                            .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(smithy)
                                                              .customizationConfig(CustomizationConfig.create())
                                                              .build())
            .build();
    }

    @Test
    void create_withAStructureThatIsAlsoAMemberTarget_givesTheModelClassThePartitionSchema() {
        SchemaSerdeModel serde = sinkCodegen.serde();
        for (String name : new String[] {"StoreRequest", "StoreResponse"}) {
            assertThat(serde.entry(name)).as(name).isNotNull();
            assertThat(serde.entry(name).isSharedShape()).as(name).isTrue();
        }
        assertThat(serde.entry("KitchenSink")).isNotNull();
        assertThat(serde.entry("KitchenSink").isSharedShape()).isFalse();
        assertThat(serde.entry("FailureException")).isNotNull();
        assertThat(serde.builderClass(ShapeId.from("sink#KitchenSink")).simpleName()).isEqualTo("KitchenSink");
        assertThat(serde.operation(ShapeId.from("sink#Store"))).isNotNull();
        assertThat(serde.operation(ShapeId.from("sink#Fail"))).isNotNull();
    }

    @Test
    void generatedModelPackage_withAStructureThatIsAlsoAMemberTarget_buildsEachClassFromItsSchema() throws Exception {
        try (URLClassLoader loader = RuntimeCompiler.classLoader(ModelPackageCompiler.compile(sinkModel, sinkCodegen,
                                                                                              dir))) {
            Class<?> requestClass = loader.loadClass(SINK_PACKAGE + ".StoreRequest");
            Class<?> modelClass = loader.loadClass(SINK_PACKAGE + ".KitchenSink");
            Schema requestSchema = (Schema) requestClass.getField("SCHEMA").get(null);
            Schema modelSchema = (Schema) modelClass.getField("SCHEMA").get(null);

            assertThat(requestSchema).isNotSameAs(modelSchema);
            assertThat(requestSchema.id()).isEqualTo(modelSchema.id());
            assertThat(requestSchema.shapeClass()).isSameAs(requestClass);
            assertThat(modelSchema.shapeClass()).isSameAs(modelClass);
            // Members that target the structure, including the copy's own recursive member, reference the model
            // class's schema.
            assertThat(requestSchema.member("Recursive").memberTarget()).isSameAs(modelSchema);
            assertThat(modelSchema.member("Recursive").memberTarget()).isSameAs(modelSchema);
            assertThat(requestSchema.member("Sinks").memberTarget().member("member").memberTarget())
                .isSameAs(modelSchema);
            assertThat(requestSchema.member("Recursive").memberTarget().shapeBuilder().build())
                .isInstanceOf(modelClass);

            Class<?> errorClass = loader.loadClass(SINK_PACKAGE + ".FailureException");
            Schema errorSchema = (Schema) errorClass.getField("SCHEMA").get(null);
            assertThat(errorSchema.member("Data").memberTarget()).isSameAs(modelSchema);
        }
    }

    @Test
    void create_withSharedStructures_givesEveryRequestAndResponseClassItsOwnSchema() {
        SchemaSerdeModel serde = codegen.serde();
        for (String name : new String[] {"EchoRequest", "EchoResponse", "EnableRequest", "DisableRequest"}) {
            assertThat(serde.entry(name)).as(name).isNotNull();
            assertThat(serde.entry(name).isSharedShape()).as(name).isTrue();
            assertThat(serde.entry(name).hasOwnSchema()).as(name).isTrue();
        }
        assertThat(serde.entry("Thing").isSharedShape()).isFalse();
        // The partition fields of the shared structures have no builder: they name no single class.
        assertThat(serde.builderClass(ShapeId.from("shared#EchoInputOutput"))).isNull();
        assertThat(serde.builderClass(ShapeId.from("shared#ToggleInput"))).isNull();
        assertThat(serde.operation(ShapeId.from("shared#Echo"))).isNotNull();
        assertThat(serde.operation(ShapeId.from("shared#Enable"))).isNotNull();
        assertThat(serde.operation(ShapeId.from("shared#Disable"))).isNotNull();
    }

    @Test
    void sharedShapeRequest_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("EchoRequest"), generatesTo("schemaserde/echorequest.java"));
    }

    @Test
    void sharedShapeOperations_useEachClassSchema() {
        MatcherAssert.assertThat(schemaClass("SharedOperations"), generatesTo("schemaserde/sharedoperations.java"));
    }

    @Test
    void generatedModelPackage_withSharedStructures_buildsEachClassFromItsSchema() throws Exception {
        try (URLClassLoader loader = RuntimeCompiler.classLoader(ModelPackageCompiler.compile(model, codegen, dir))) {
            Class<?> requestClass = loader.loadClass(PACKAGE + ".EchoRequest");
            Class<?> responseClass = loader.loadClass(PACKAGE + ".EchoResponse");
            Schema requestSchema = (Schema) requestClass.getField("SCHEMA").get(null);
            Schema responseSchema = (Schema) responseClass.getField("SCHEMA").get(null);

            assertThat(requestSchema).isNotSameAs(responseSchema);
            assertThat(requestSchema.id().toString()).isEqualTo("shared#EchoInputOutput");
            assertThat(responseSchema.id()).isEqualTo(requestSchema.id());
            assertThat(requestSchema.shapeClass()).isSameAs(requestClass);
            assertThat(responseSchema.shapeClass()).isSameAs(responseClass);
            assertThat(memberNames(requestSchema)).containsExactly("Name", "Thing", "Things");
            assertThat(memberNames(responseSchema)).containsExactly("Name", "Thing", "Things");
            // Member targets are the partition fields, shared by both copies.
            assertThat(requestSchema.member("Thing").memberTarget())
                .isSameAs(responseSchema.member("Thing").memberTarget());

            ShapeBuilder<?> responseBuilder = responseSchema.shapeBuilder();
            responseBuilder.setMemberValue(responseSchema.member("Name"), "n");
            Object response = responseBuilder.build();
            assertThat(response).isInstanceOf(responseClass);
            assertThat((String) ((SerializableStruct) response).getMemberValue(responseSchema.member("Name")))
                .isEqualTo("n");

            Method builderMethod = requestClass.getMethod("builder");
            Object requestBuilder = builderMethod.invoke(null);
            builderMethod.getReturnType().getMethod("name", String.class).invoke(requestBuilder, "hi");
            SerializableStruct request = (SerializableStruct) ((SdkBuilder<?, ?>) requestBuilder).build();
            assertThat(request.sdkSchema()).isSameAs(requestSchema);
            List<String> written = new ArrayList<>();
            request.serializeMembers(new SpecificShapeSerializer() {
                @Override
                public void writeString(Schema schema, String value) {
                    written.add(schema.memberName() + "=" + value);
                }
            });
            assertThat(written).containsExactly("Name=hi");

            Class<?> operations = loader.loadClass(PACKAGE + ".SharedOperations");
            ApiOperation<?, ?> echo = (ApiOperation<?, ?>) operations.getField("ECHO").get(null);
            assertThat(echo.inputSchema()).isSameAs(requestSchema);
            assertThat(echo.outputSchema()).isSameAs(responseSchema);
            assertThat(echo.outputBuilder().build()).isInstanceOf(responseClass);
            ApiOperation<?, ?> disable = (ApiOperation<?, ?>) operations.getField("DISABLE").get(null);
            assertThat(disable.inputBuilder().build()).isInstanceOf(loader.loadClass(PACKAGE + ".DisableRequest"));
        }
    }

    private static List<String> memberNames(Schema schema) {
        List<String> names = new ArrayList<>();
        for (Schema member : schema.members()) {
            names.add(member.memberName());
        }
        return names;
    }

    private static ClassSpec modelClass(String shapeName) {
        return ModelPackageCompiler.modelClass(model, codegen, shapeName);
    }

    private static ClassSpec schemaClass(String simpleName) {
        return codegen.classSpecs().stream().filter(s -> s.className().simpleName().equals(simpleName)).findFirst()
                      .orElseThrow(() -> new AssertionError(simpleName));
    }
}
