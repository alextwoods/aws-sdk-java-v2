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

import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.TraitInclusionConfig;
import software.amazon.awssdk.codegen.schema.TraitInclusionPolicy;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.SchemaIndex;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generates {@code <Svc>Operations} for a small awsQuery model (several auth schemes, {@code @optionalAuth},
 * {@code @auth([])}, {@code Unit} input and output, service errors, a shared {@code awsQueryError} code, an error shape
 * that shares another's exception class, an error reachable only through a member), compares it with a golden file,
 * then compiles and loads it with {@code --release 8} against the runtime schema module.
 */
class OperationsClassSpecTest {
    private static final String NS = "ops.test";
    private static final ShapeId SERVICE = ShapeId.from(NS + "#OpsService");
    private static final ShapeId LEGACY_MISSING = ShapeId.from(NS + "#LegacyMissing");
    private static final ShapeId ITEM_MISSING = ShapeId.from(NS + "#ItemMissing");

    @TempDir
    Path dir;

    @Test
    void operationsClass_matchesGoldenFile() {
        MatcherAssert.assertThat(operationsSpec(generate()), generatesTo("OpsOperations.java"));
    }

    @Test
    void errorRegistry_holdsExactlyTheClosureErrorsThatOwnTheirClass() {
        OperationsClassSpec operations = generate().operations();

        assertThat(names(operations.registeredErrors()))
            .containsExactly("ItemMissing", "MemberError", "OtherMissing", "ServerFault");
        // First registered shape in ShapeId order wins a shared code; an alias's code is not indexed.
        assertThat(operations.errorCodes()).hasSize(1).containsEntry("Missing", ITEM_MISSING);
    }

    @Test
    void effectiveAuthSchemes_followAuthAndOptionalAuth() {
        OperationsClassSpec operations = generate().operations();

        assertThat(operations.effectiveAuthSchemes().get(ShapeId.from(NS + "#GetItem")))
            .containsExactly(ShapeId.from("aws.auth#sigv4"), ShapeId.from("smithy.api#httpBearerAuth"));
        assertThat(operations.effectiveAuthSchemes().get(ShapeId.from(NS + "#PutItem")))
            .containsExactly(ShapeId.from("aws.auth#sigv4"), ShapeId.from("smithy.api#httpBearerAuth"),
                             ShapeId.from("smithy.api#noAuth"));
        assertThat(operations.effectiveAuthSchemes().get(ShapeId.from(NS + "#Ping")))
            .containsExactly(ShapeId.from("smithy.api#noAuth"));
        assertThat(operations.effectiveAuthSchemes().get(ShapeId.from(NS + "#SignedOnly")))
            .containsExactly(ShapeId.from("aws.auth#sigv4"));
    }

    @Test
    void generatedOperations_initializeNoSchemaPartitionUntilASchemaIsRead() throws Exception {
        SchemaCodegen codegen = generate();
        Path classes = compile(codegen);
        String partition = codegen.schemas().partitionClass(0).reflectionName();
        try (TrackingClassLoader loader = new TrackingClassLoader(classes)) {
            Class<?> type = Class.forName(codegen.operations().className().reflectionName(), true, loader);
            ErrorRegistry registry = (ErrorRegistry) type.getField("ERROR_REGISTRY").get(null);
            ApiOperation<?, ?> getItem = (ApiOperation<?, ?>) type.getField("GET_ITEM").get(null);
            assertThat(registry.contains(runtimeId(ITEM_MISSING))).isTrue();
            assertThat(registry.resolveCode("Missing")).isEqualTo(runtimeId(ITEM_MISSING));
            assertThat(getItem.schema().id()).isEqualTo(runtimeId(ShapeId.from(NS + "#GetItem")));
            assertThat(loader.loaded(partition)).as("partition loaded by <clinit>").isFalse();

            assertThat(getItem.inputSchema().id()).isEqualTo(runtimeId(ShapeId.from(NS + "#GetItemInput")));
            assertThat(loader.loaded(partition)).isTrue();
        }
    }

    @Test
    void generatedOperations_describeTheModel() throws Exception {
        SchemaCodegen codegen = generate();
        Path classes = compile(codegen);
        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
            Class<?> type = Class.forName(codegen.operations().className().reflectionName(), true, loader);
            Class<?> serviceSchemas = Class.forName(codegen.serviceSchemasClass().reflectionName(), true, loader);
            SchemaIndex index = (SchemaIndex) Class.forName(codegen.schemaIndexClass().reflectionName(), true, loader)
                                                   .getConstructor().newInstance();
            ApiService service = (ApiService) type.getField("SERVICE").get(null);
            ErrorRegistry registry = (ErrorRegistry) type.getField("ERROR_REGISTRY").get(null);
            assertThat(service.schema()).isSameAs(serviceSchemas.getField("SERVICE").get(null));

            ApiOperation<?, ?> getItem = operation(type, "GET_ITEM");
            assertThat(getItem.schema()).isSameAs(serviceSchemas.getField("GET_ITEM").get(null));
            assertThat(getItem.inputSchema()).isSameAs(index.getSchema(runtimeId(ShapeId.from(NS + "#GetItemInput"))));
            assertThat(getItem.outputSchema())
                .isSameAs(index.getSchema(runtimeId(ShapeId.from(NS + "#GetItemOutput"))));
            // Error schemas are resolved through the registry, which needs the shape classes generated model classes get
            // in task 3.7; until then the first declared error fails resolution. The golden file covers the error IDs:
            // model order, LegacyMissing resolved to its class owner and deduplicated, then service errors.
            assertThatThrownBy(getItem::errorSchemas).isInstanceOf(IllegalStateException.class)
                                                     .hasMessageContaining(NS + "#OtherMissing has no shape class");
            assertThat(getItem.errorRegistry()).isSameAs(registry);
            assertThat(getItem.service()).isSameAs(service);
            assertThat(getItem.effectiveAuthSchemes().stream().map(Object::toString).collect(Collectors.toList()))
                .containsExactly("aws.auth#sigv4", "smithy.api#httpBearerAuth");

            ApiOperation<?, ?> ping = operation(type, "PING");
            assertThat(ping.inputSchema()).isSameAs(PreludeSchemas.UNIT);
            assertThat(ping.outputSchema()).isSameAs(PreludeSchemas.UNIT);
            assertThat(ping.effectiveAuthSchemes().stream().map(Object::toString).collect(Collectors.toList()))
                .containsExactly("smithy.api#noAuth");
            assertThat(operation(type, "PUT_ITEM").outputSchema()).isSameAs(PreludeSchemas.UNIT);

            for (String error : new String[] {"ItemMissing", "OtherMissing", "ServerFault", "MemberError"}) {
                software.amazon.awssdk.schema.ShapeId id = software.amazon.awssdk.schema.ShapeId.fromParts(NS, error);
                assertThat(registry.contains(id)).as(error).isTrue();
                // The registry checks the supplied schema's ID before its shape class, so this shows the supplier
                // returns the error's own schema; the schema has no shape class until task 3.7.
                assertThatThrownBy(() -> registry.getSchema(id)).isInstanceOf(IllegalStateException.class)
                                                                .hasMessageContaining(NS + "#" + error + " has no");
            }
            assertThat(registry.contains(software.amazon.awssdk.schema.ShapeId.fromParts(NS, "LegacyMissing")))
                .isFalse();
            assertThat(registry.contains(software.amazon.awssdk.schema.ShapeId.fromParts(NS, "NotInTheClosure")))
                .isFalse();
            assertThat(registry.contains(software.amazon.awssdk.schema.ShapeId.fromParts(NS, "Item"))).isFalse();
            assertThat(registry.resolveCode("Missing:Sender")).isEqualTo(runtimeId(ITEM_MISSING));
            assertThat(registry.resolveCode("Legacy")).isNull();
        }
    }

    @Test
    void generation_isDeterministic() {
        assertThat(source(operationsSpec(generate(model(true), aliases()))))
            .isEqualTo(source(operationsSpec(generate())));
    }

    @Test
    void aliasOfAShapeThatDoesNotOwnItsClass_fails() {
        Function<ShapeId, ShapeId> owners = id -> {
            if (id.equals(LEGACY_MISSING)) {
                return ITEM_MISSING;
            }
            return id.equals(ITEM_MISSING) ? null : id;
        };
        assertThatThrownBy(() -> generate(model(false), owners))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("LegacyMissing")
            .hasMessageContaining("ItemMissing");
    }

    private static ApiOperation<?, ?> operation(Class<?> type, String field) throws ReflectiveOperationException {
        return (ApiOperation<?, ?>) type.getField(field).get(null);
    }

    private static List<String> names(Iterable<ShapeId> ids) {
        List<String> names = new ArrayList<>();
        ids.forEach(id -> names.add(id.getName()));
        return names;
    }

    private static software.amazon.awssdk.schema.ShapeId runtimeId(ShapeId id) {
        return software.amazon.awssdk.schema.ShapeId.from(id.toString());
    }

    private static ClassSpec operationsSpec(SchemaCodegen codegen) {
        for (ClassSpec spec : codegen.classSpecs()) {
            if (spec.className().equals(codegen.operations().className())) {
                return spec;
            }
        }
        throw new AssertionError("No operations class");
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

    /**
     * LegacyMissing shares ItemMissing's exception class, as when two error shapes get the same class name.
     */
    private static Function<ShapeId, ShapeId> aliases() {
        return id -> id.equals(LEGACY_MISSING) ? ITEM_MISSING : id;
    }

    private static SchemaCodegen generate() {
        return generate(model(false), aliases());
    }

    private static SchemaCodegen generate(Model model, Function<ShapeId, ShapeId> owners) {
        ServiceShape service = model.expectShape(SERVICE, ServiceShape.class);
        return SchemaCodegen.builder()
                            .model(model)
                            .service(service)
                            .policy(TraitInclusionPolicy.forService(model, service, TraitInclusionConfig.empty()))
                            .initializers(TraitInitializers.of(Collections.emptyList()))
                            .modelPackage("test.ops.model")
                            .internalPackage("test.ops.internal")
                            .serviceName("Ops")
                            .errorShapeOwner(owners)
                            .build();
    }

    private static Model model(boolean withUnusedFileFirst) {
        software.amazon.smithy.model.loader.ModelAssembler assembler =
            Model.assembler().discoverModels(Model.class.getClassLoader());
        if (withUnusedFileFirst) {
            assembler.addUnparsedModel("other.smithy", resource("other.smithy"));
        }
        return assembler.addUnparsedModel("operations.smithy", resource("operations.smithy")).assemble().unwrap();
    }

    private static String resource(String name) {
        URL url = OperationsClassSpecTest.class.getResource(name);
        try (InputStream in = url.openStream()) {
            return new String(IoUtils.toByteArray(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
