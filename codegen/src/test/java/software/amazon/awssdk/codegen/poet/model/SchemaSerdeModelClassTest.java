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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.SmithyEnum;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.smithy.model.Model;

/**
 * Model classes of a schema-enabled service (task 3.7): golden files for each kind of class, and the generated model
 * package compiled with {@code --release 8} and exercised: {@code SerializableStruct} members, builders, enums,
 * schema-derived {@code SdkField}s and {@code toString}.
 */
class SchemaSerdeModelClassTest {
    private static final String PACKAGE = "software.amazon.awssdk.services.demo.model";
    private static final String MODEL =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "@service(sdkId: \"Demo\")\n"
        + "@sigv4(name: \"demo\")\n"
        + "@restJson1\n"
        + "service DemoService { version: \"2024-01-01\", operations: [GetThing, DeleteThing], errors: [ThingNotFound] }\n"
        + "@http(method: \"POST\", uri: \"/things/{Id}\")\n"
        + "operation GetThing { input: GetThingRequest, output: GetThingResponse }\n"
        + "@http(method: \"POST\", uri: \"/things/delete\")\n"
        + "operation DeleteThing { input: DeleteThingRequest }\n"
        + "structure GetThingRequest {\n"
        + "  @required @httpLabel Id: String\n"
        + "  @httpHeader(\"x-token\") Token: String\n"
        + "  @httpQuery(\"v\") Version: Integer\n"
        + "  Tags: TagMap\n"
        + "  Names: NameList\n"
        + "  Color: Color\n"
        + "  @jsonName(\"when\") When: Timestamp\n"
        + "  Data: Blob\n"
        + "  Nested: Thing\n"
        + "}\n"
        + "structure GetThingResponse { Thing: Thing, Things: ThingList, Choice: Choice, SecretNames: SecretList }\n"
        + "structure DeleteThingRequest { Name: String }\n"
        + "structure Thing { Name: String, Secret: SecretString, Count: Long, Enabled: Boolean, Children: ThingList }\n"
        + "@sensitive string SecretString\n"
        + "list ThingList { member: Thing }\n"
        + "list SecretList { member: SecretString }\n"
        + "list NameList { member: String }\n"
        + "map TagMap { key: String, value: NameList }\n"
        + "enum Color { RED, GREEN }\n"
        + "union Choice { Text: String, Number: Integer, Nothing: Unit }\n"
        + "@error(\"client\") @httpError(404) structure ThingNotFound { message: String, Reason: String }\n";

    private static IntermediateModel model;
    private static SchemaCodegen codegen;

    @TempDir
    Path dir;

    @BeforeAll
    static void generate() {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("demo.smithy", MODEL)
                            .assemble()
                            .unwrap();
        model = new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                               .model(smithy)
                                                               .customizationConfig(CustomizationConfig.create())
                                                               .build())
            .build();
        codegen = SchemaCodegen.create(model, TraitInitializers.load(SchemaSerdeModelClassTest.class.getClassLoader()));
    }

    private static ClassSpec modelClass(String shapeName) {
        return ModelPackageCompiler.modelClass(model, codegen, shapeName);
    }

    private static ClassSpec schemaClass(String simpleName) {
        return codegen.classSpecs().stream().filter(s -> s.className().simpleName().equals(simpleName)).findFirst()
                      .orElseThrow(() -> new AssertionError(simpleName));
    }

    @Test
    void everyStructureEnumAndSyntheticClassHasSchemaSerdeMembers() {
        assertThat(codegen.serde().entries().keySet())
            .containsExactlyInAnyOrder("GetThingRequest", "GetThingResponse", "DeleteThingRequest", "DeleteThingResponse",
                                       "Thing", "Choice", "Unit", "ThingNotFoundException");
        assertThat(codegen.serde().entry("DeleteThingResponse").isSynthetic()).isTrue();
        assertThat(codegen.serde().entry("Unit").isUnit()).isTrue();
        assertThat(codegen.serde().entry("Unit").hasOwnSchema()).isTrue();
    }

    @Test
    void unit_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("Unit"), generatesTo("schemaserde/unit.java"));
    }

    @Test
    void request_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("GetThingRequest"), generatesTo("schemaserde/getthingrequest.java"));
    }

    @Test
    void structure_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("Thing"), generatesTo("schemaserde/thing.java"));
    }

    @Test
    void union_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("Choice"), generatesTo("schemaserde/choice.java"));
    }

    @Test
    void exception_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("ThingNotFoundException"), generatesTo("schemaserde/thingnotfoundexception.java"));
    }

    @Test
    void syntheticResponse_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("DeleteThingResponse"), generatesTo("schemaserde/deletethingresponse.java"));
    }

    @Test
    void enum_matchesGoldenFile() {
        MatcherAssert.assertThat(modelClass("Color"), generatesTo("schemaserde/color.java"));
    }

    @Test
    void sharedSerde_matchesGoldenFile() {
        MatcherAssert.assertThat(schemaClass("DemoSharedSerde"), generatesTo("schemaserde/demosharedserde.java"));
    }

    @Test
    void operations_useTheModelClasses() {
        MatcherAssert.assertThat(schemaClass("DemoOperations"), generatesTo("schemaserde/demooperations.java"));
    }

    @Test
    void generatedModelPackage_compilesAndImplementsSchemaSerde() throws Exception {
        try (URLClassLoader loader = RuntimeCompiler.classLoader(compileModelPackage())) {
            Class<?> thingClass = loader.loadClass(PACKAGE + ".Thing");
            Class<?> requestClass = loader.loadClass(PACKAGE + ".GetThingRequest");
            Class<?> colorClass = loader.loadClass(PACKAGE + ".Color");

            Schema thingSchema = (Schema) thingClass.getField("SCHEMA").get(null);
            assertThat(thingSchema.shapeClass()).isSameAs(thingClass);
            assertThat(SerializableStruct.class).isAssignableFrom(thingClass);

            // Build a Thing through the schema builder and setMemberValue.
            ShapeBuilder<?> thingBuilder = thingSchema.shapeBuilder();
            thingBuilder.setMemberValue(thingSchema.member("Name"), "n");
            thingBuilder.setMemberValue(thingSchema.member("Secret"), "s");
            thingBuilder.setMemberValue(thingSchema.member("Count"), 3L);
            SerializableStruct thing = (SerializableStruct) thingBuilder.build();
            assertThat((String) thing.getMemberValue(thingSchema.member("Name"))).isEqualTo("n");
            assertThat((Long) thing.getMemberValue(thingSchema.member("Count"))).isEqualTo(3L);
            assertThat((Object) thing.getMemberValue(thingSchema.member("Enabled"))).isNull();
            assertThat(thing.toString()).isEqualTo("Thing(Name=n, Secret=*** Sensitive Data Redacted ***, Count=3)");
            assertThatThrownBy(() -> thing.getMemberValue(Schema.createString(
                software.amazon.awssdk.schema.ShapeId.from("demo#Other"))))
                .isInstanceOf(IllegalArgumentException.class);

            // serializeMembers writes set members once, in member order; unset lists are not written.
            List<String> written = new ArrayList<>();
            thing.serializeMembers(new SpecificShapeSerializer() {
                @Override
                public void writeString(Schema schema, String value) {
                    written.add(schema.memberName() + "=" + value);
                }

                @Override
                public void writeLong(Schema schema, long value) {
                    written.add(schema.memberName() + "=" + value);
                }
            });
            assertThat(written).containsExactly("Name=n", "Secret=s", "Count=3");

            // A request built with its public builder; toString and sdkFields match the previous generator.
            Method builderMethod = requestClass.getMethod("builder");
            Class<?> builderType = builderMethod.getReturnType();
            Object requestBuilder = builderMethod.invoke(null);
            invoke(builderType, requestBuilder, "id", String.class, "i-1");
            invoke(builderType, requestBuilder, "token", String.class, "t");
            invoke(builderType, requestBuilder, "names", java.util.Collection.class, Arrays.asList("a", "b"));
            invoke(builderType, requestBuilder, "color", String.class, "RED");
            invoke(builderType, requestBuilder, "when", Instant.class, Instant.EPOCH);
            SdkBytes data = SdkBytes.fromUtf8String("hi");
            invoke(builderType, requestBuilder, "data", SdkBytes.class, data);
            Object request = ((SdkBuilder<?, ?>) requestBuilder).build();
            assertThat(request.toString())
                .isEqualTo("GetThingRequest(Id=i-1, Token=t, Names=[a, b], Color=RED, When=1970-01-01T00:00:00Z, "
                           + "Data=SdkBytes(bytes=0x6869))");

            // The blob member reaches writeBlob(Schema, byte[]) as the SdkBytes array itself, not a copy or a
            // ByteBuffer (SpecificShapeSerializer rejects the ByteBuffer write).
            List<byte[]> blobs = new ArrayList<>();
            ((SerializableStruct) request).serializeMembers(new SpecificShapeSerializer() {
                @Override
                public void writeString(Schema schema, String value) {
                }

                @Override
                public <T> void writeList(Schema schema, T listState, int size, BiConsumer<T, ShapeSerializer> consumer) {
                }

                @Override
                public void writeTimestamp(Schema schema, Instant value) {
                }

                @Override
                public void writeBlob(Schema schema, byte[] value) {
                    blobs.add(value);
                }
            });
            assertThat(blobs).hasSize(1);
            assertThat(blobs.get(0)).isSameAs(data.asByteArrayUnsafe());

            List<SdkField<?>> fields = ((SdkPojo) request).sdkFields();
            assertThat(fields.stream().map(SdkField::memberName).collect(Collectors.toList()))
                .containsExactly("Id", "Token", "Version", "Tags", "Names", "Color", "When", "Data", "Nested");
            assertThat(fields.get(0).getRequiredTrait(LocationTrait.class).location()).isEqualTo(MarshallLocation.PATH);
            assertThat(fields.get(1).getRequiredTrait(LocationTrait.class).locationName()).isEqualTo("x-token");
            assertThat(fields.get(6).getRequiredTrait(LocationTrait.class).locationName()).isEqualTo("when");
            assertThat(fields.get(4).getValueOrDefault(request)).isEqualTo(Arrays.asList("a", "b"));

            // A class with a sensitive collection keeps the previous toString, which redacts the unset collection.
            Class<?> responseClass = loader.loadClass(PACKAGE + ".GetThingResponse");
            Method responseBuilder = responseClass.getMethod("builder");
            Object emptyResponse = responseBuilder.getReturnType().getMethod("build")
                                                  .invoke(responseBuilder.invoke(null));
            assertThat(emptyResponse.toString())
                .isEqualTo("GetThingResponse(SecretNames=*** Sensitive Data Redacted ***)");

            // Enums implement SmithyEnum.
            SmithyEnum red = (SmithyEnum) colorClass.getMethod("fromValue", String.class).invoke(null, "RED");
            assertThat(red.getValue()).isEqualTo("RED");
            assertThat(red.sdkSchema()).isSameAs(colorClass.getField("SCHEMA").get(null));

            assertUnitUnionMember(loader);
        }
    }

    /**
     * A union member that targets {@code smithy.api#Unit} targets the generated {@code Unit} class's schema, which,
     * unlike {@code PreludeSchemas.UNIT}, builds a {@code Unit}: for schema deserializers and for the derived
     * {@code SdkField} constructor that the legacy unmarshallers call.
     */
    private static void assertUnitUnionMember(URLClassLoader loader) throws Exception {
        Class<?> unitClass = loader.loadClass(PACKAGE + ".Unit");
        Class<?> choiceClass = loader.loadClass(PACKAGE + ".Choice");
        Schema unitSchema = (Schema) unitClass.getField("SCHEMA").get(null);
        Schema choiceSchema = (Schema) choiceClass.getField("SCHEMA").get(null);
        assertThat(unitSchema.id().toString()).isEqualTo("smithy.api#Unit");
        assertThat(unitSchema.shapeClass()).isSameAs(unitClass);
        assertThat(choiceSchema.member("Nothing").memberTarget()).isSameAs(unitSchema);

        Object unit = unitSchema.shapeBuilder().build();
        assertThat(unit).isInstanceOf(unitClass);
        ShapeBuilder<?> choiceBuilder = choiceSchema.shapeBuilder();
        choiceBuilder.setMemberValue(choiceSchema.member("Nothing"), unit);
        SerializableStruct choice = (SerializableStruct) choiceBuilder.build();
        assertThat((Object) choice.getMemberValue(choiceSchema.member("Nothing"))).isSameAs(unit);
        assertThat(choice.toString()).isEqualTo("Choice(Nothing=Unit())");

        List<String> written = new ArrayList<>();
        choice.serializeMembers(new SpecificShapeSerializer() {
            @Override
            public void writeStruct(Schema schema, SerializableStruct struct) {
                written.add(schema.memberName() + "=" + struct.sdkSchema().id());
            }
        });
        assertThat(written).containsExactly("Nothing=smithy.api#Unit");

        SdkField<?> nothing = ((SdkPojo) choice).sdkFieldNameToField().get("Nothing");
        assertThat(((SdkBuilder<?, ?>) nothing.constructor().get()).build()).isInstanceOf(unitClass);
    }

    private static void invoke(Class<?> builderType, Object target, String name, Class<?> type, Object value)
        throws Exception {
        builderType.getMethod(name, type).invoke(target, value);
    }

    private Path compileModelPackage() {
        return ModelPackageCompiler.compile(model, codegen, dir);
    }
}
