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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;
import software.amazon.awssdk.smithy.runtime.schema.ServiceSchemaIndex;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Generated schema holders compile against the generated-client runtime, initialize without the Smithy parser or
 * assembler, and match the canonical model (Requirements 2.1, 4.5-4.7, 15.3): every runtime-relevant trait is created
 * exactly as modeled, in a class loader isolated from the test; and a service large enough to be partitioned compiles
 * with each exported schema reachable, as the same object, from the holder.
 */
class GeneratedSchemaCompilationTest {

    private static final String PACKAGE = "software.amazon.awssdk.services.everytrait";

    /** A restJson1 service that applies every runtime-relevant trait at least once. */
    private static final String EVERY_TRAIT = """
        $version: "2"
        namespace example.everytrait

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "everytrait")
        @auth([aws.auth#sigv4])
        @httpBasicAuth
        @httpBearerAuth
        @httpDigestAuth
        @httpApiKeyAuth(name: "X-Key", in: "header", scheme: "ApiKey")
        @paginated(inputToken: "next", outputToken: "next", pageSize: "size")
        service EveryTrait {
            version: "1"
            operations: [Put, Get, Watch]
        }

        @http(method: "PUT", uri: "/things/{id}", code: 201)
        @idempotent
        @httpChecksumRequired
        @optionalAuth
        operation Put {
            input := {
                @required
                @httpLabel
                @length(min: 1, max: 10)
                @pattern("^[a-z]+$")
                id: String

                @httpHeader("X-Count")
                @range(min: 0, max: 10)
                count: Integer = 1

                @httpQuery("tag")
                tag: Secret

                @httpQueryParams
                params: Params

                @httpPrefixHeaders("X-Meta-")
                meta: Params

                @required
                @httpPayload
                body: Body

                @idempotencyToken
                @httpHeader("X-Token")
                token: String
            }
            output := {
                @httpResponseCode
                status: Integer

                @httpHeader("X-When")
                @timestampFormat("http-date")
                when: Timestamp
            }
            errors: [Conflict]
        }

        @readonly
        @http(method: "GET", uri: "/things")
        @paginated
        operation Get {
            input := {
                @httpQuery("next")
                next: String

                @httpQuery("size")
                size: Integer
            }
            output := {
                next: String
                items: Items
                @jsonName("kind_of")
                kind: Kind
                level: Level
                doc: Document
                big: BigDecimal
                tags: Tags
            }
        }

        @http(method: "POST", uri: "/watch")
        @endpoint(hostPrefix: "{region}.")
        @requestCompression(encodings: ["gzip"])
        operation Watch {
            input := {
                @required
                @hostLabel
                region: String
            }
            output := {
                @httpPayload
                events: Events
            }
        }

        @streaming
        @requiresLength
        @mediaType("application/octet-stream")
        blob Body

        @sensitive
        string Secret

        map Params {
            key: String
            value: String
        }

        @sparse
        map Tags {
            key: String
            value: String
        }

        @uniqueItems
        list Items {
            @xmlName("item")
            member: Item
        }

        @xmlNamespace(uri: "https://example.com/ns", prefix: "ex")
        structure Item {
            @xmlAttribute
            name: String

            @xmlFlattened
            children: ItemList
        }

        list ItemList {
            member: Item
        }

        enum Kind {
            PLAIN = "plain"
            @enumValue("fancy")
            FANCY
        }

        intEnum Level {
            LOW = 1
            HIGH = 2
        }

        @streaming
        union Events {
            ping: PingEvent
        }

        structure PingEvent {
            @eventHeader
            id: String

            @eventPayload
            payload: Blob
        }

        @error("client")
        @httpError(409)
        @retryable(throttling: false)
        structure Conflict {
            message: String
        }
        """;

    @TempDir
    Path temp;

    /**
     * Every runtime-relevant trait of the model is on the generated schema of its shape or member (or on the enum
     * member schema) and equal to the modeled trait; unknown traits are on none. Initializing the holder, its index
     * and every descriptor in a class loader isolated from the test resolves no Smithy parser, assembler, validation,
     * knowledge-index, selector or code-generation class.
     */
    @Test
    void everyRuntimeTraitIsCreatedExactlyWithoutParserClasses() throws IOException {
        Path sources = generate("example.everytrait#EveryTrait", EVERY_TRAIT);
        CompiledSources compiled = CompiledSources.compile(sources, temp.resolve("classes"));
        String holder = PACKAGE + ".model.internal.EveryTraitSchemas";

        Model model = assemble(EVERY_TRAIT);
        Set<ShapeId> checkedTraits = new TreeSet<>();
        try (URLClassLoader loader = compiled.loader()) {
            ServiceSchemaIndex index = CompiledSources.index(loader, holder);
            for (Shape shape : new Walker(model).walkShapes(model.expectShape(ShapeId.from(
                "example.everytrait#EveryTrait")))) {
                if (shape.getId().getNamespace().equals("smithy.api") || shape.isResourceShape()) {
                    continue;
                }
                Schema schema = index.schema(shape.getId()).orElseThrow(() -> new AssertionError(shape.getId()));
                for (Trait trait : shape.getAllTraits().values()) {
                    if (trait instanceof DynamicTrait || !trait.getClass().getPackageName()
                                                              .equals("software.amazon.smithy.model.traits")) {
                        continue;
                    }
                    Trait generated = schema.getDirectTrait(TraitKey.get(trait.getClass()));
                    if (generated != null) {
                        assertEquals(trait, generated, trait.toShapeId() + " on " + shape.getId());
                        assertEquals(trait.toNode(), generated.toNode(), trait.toShapeId() + " on " + shape.getId());
                        checkedTraits.add(trait.toShapeId());
                    }
                }
                assertEquals(null, schema.getDirectTrait(TraitKey.get(DynamicTrait.class)), shape.getId().toString());
            }
        }
        Set<String> expected = new TreeSet<>(List.of(
            "auth", "default", "endpoint", "enumValue", "error", "eventHeader", "eventPayload", "hostLabel", "http",
            "httpApiKeyAuth", "httpBasicAuth", "httpBearerAuth", "httpChecksumRequired", "httpDigestAuth", "httpError",
            "httpHeader", "httpLabel", "httpPayload", "httpPrefixHeaders", "httpQuery", "httpQueryParams",
            "httpResponseCode", "idempotencyToken", "idempotent", "jsonName", "length", "mediaType", "optionalAuth",
            "paginated", "pattern", "range", "readonly", "requestCompression", "required", "requiresLength",
            "retryable", "sensitive", "sparse", "streaming", "timestampFormat", "uniqueItems", "xmlAttribute",
            "xmlFlattened", "xmlName", "xmlNamespace"));
        Set<String> checked = new TreeSet<>();
        checkedTraits.forEach(id -> checked.add(id.getName()));
        assertEquals(expected, checked, "runtime traits found on generated schemas");

        try (CompiledSources.RecordingClassLoader isolated = compiled.isolatedLoader()) {
            Object index = CompiledSources.invokeStatic(isolated, holder, "index");
            assertEquals(isolated, index.getClass().getClassLoader(), "the runtime is loaded in isolation");
            Object operations = CompiledSources.invokeStatic(isolated, PACKAGE + ".internal.operation"
                                                                       + ".EveryTraitOperations", "operations");
            assertEquals(3, ((List<?>) operations).size());
            List<String> forbidden = new ArrayList<>();
            isolated.requestedClasses().stream().filter(SmithyRuntimeBoundary::isForbiddenRuntimeClass)
                    .forEach(forbidden::add);
            assertEquals(List.of(), forbidden, "classes outside the runtime boundary were resolved");
            assertTrue(isolated.requestedClasses().contains("software.amazon.smithy.model.traits.HttpTrait"),
                       "the trait classes were loaded by the isolated loader");
        }
    }

    /** A partitioned holder compiles, re-exports each schema object and indexes every shape of every partition. */
    @Test
    void partitionedHoldersCompileAndExportTheSameObjects() throws IOException {
        int count = 120;
        StringBuilder model = new StringBuilder("""
            $version: "2"
            namespace example.wide

            @aws.protocols#awsJson1_0
            @aws.auth#sigv4(name: "wide")
            service Wide {
                version: "1"
                operations: [Get]
            }

            operation Get {
                output := {
                    tree: Tree
            """);
        for (int i = 0; i < count; i++) {
            model.append("        s").append(i).append(": S").append(i).append("\n");
        }
        model.append("""
                }
            }

            structure Tree {
                children: Trees
            }

            list Trees {
                member: Tree
            }

            """);
        for (int i = 0; i < count; i++) {
            model.append("structure S").append(i).append(" {\n");
            for (int m = 0; m < 6; m++) {
                model.append("    @length(min: ").append(m).append(")\n    m").append(m).append(": String\n");
            }
            model.append("    tree: Tree\n}\n\n");
        }
        Path sources = generate("example.wide#Wide", model.toString(), "software.amazon.awssdk.services.wide");
        CompiledSources compiled = CompiledSources.compile(sources, temp.resolve("classes"));
        String holder = "software.amazon.awssdk.services.wide.model.internal.WideSchemas";
        try (URLClassLoader loader = compiled.loader()) {
            ServiceSchemaIndex index = CompiledSources.index(loader, holder);
            assertEquals(count + 2, index.types().size(), "S0..S119, Tree and GetOutput");
            Schema tree = (Schema) CompiledSources.staticField(loader, holder, "TREE");
            assertSame(tree, index.schema(ShapeId.from("example.wide#Tree")).orElseThrow());
            assertSame(tree, tree.member("children").listMember().memberTarget());
            String last = "S" + (count - 1);
            Schema lastSchema = (Schema) CompiledSources.staticField(loader, holder, last);
            assertSame(tree, lastSchema.member("tree").memberTarget());
            assertEquals(count, ((Schema) CompiledSources.staticField(loader, holder, "GET_OUTPUT")).members().size()
                                - 1);
            assertTrue(Files.exists(sources.resolve("software/amazon/awssdk/services/wide/model/internal/"
                                                    + "WideSchemas2.java")), "the holder is partitioned");
        }
    }

    private Path generate(String service, String model) throws IOException {
        return generate(service, model, PACKAGE);
    }

    private Path generate(String service, String model, String packageName) throws IOException {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("model.smithy"), model, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest(service, packageName, models,
                                                                            temp.resolve("out")), List.of());
        return result.sourceRoot();
    }

    private static Model assemble(String model) {
        ClassLoader loader = AwsCodegen.class.getClassLoader();
        return Model.assembler(loader).discoverModels(loader).putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                    .addUnparsedModel("model.smithy", model).assemble().unwrap();
    }
}
