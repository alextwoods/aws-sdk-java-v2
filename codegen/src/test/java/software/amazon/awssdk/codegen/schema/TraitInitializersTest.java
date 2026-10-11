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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.TraitFactories;
import software.amazon.awssdk.schema.traits.TraitFactory;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Renders every built-in trait initializer and the generic initializer, compiles the expressions with
 * {@code --release 8} against the runtime schema module, and checks each value against the trait the runtime factory
 * builds from the same model value.
 */
class TraitInitializersTest {
    private static final ShapeId TARGET = ShapeId.from("test.traits#Target");
    private static final software.amazon.smithy.model.traits.TraitFactory MODEL_TRAITS =
        software.amazon.smithy.model.traits.TraitFactory.createServiceFactory();

    @TempDir
    static Path dir;

    private static List<Trait> traits;
    private static Object[][] rendered;

    @BeforeAll
    static void renderAndCompile() throws Exception {
        traits = cases();
        TraitInitializers registry = TraitInitializers.of(Collections.emptyList());
        TraitRenderContext context = TraitRenderContext.defaultContext();

        List<CodeBlock> rows = new ArrayList<>();
        for (Trait trait : traits) {
            rows.add(CodeBlock.of("{$L, $L, $L}", registry.render(trait, context), context.shapeId(trait.toShapeId()),
                                  context.document(trait.toNode())));
        }
        ClassName name = ClassName.get("test.rendered", "RenderedTraits");
        TypeSpec type = TypeSpec.classBuilder(name)
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                .addMethod(MethodSpec.methodBuilder("values")
                                                     .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                     .returns(ArrayTypeName.of(ArrayTypeName.of(Object.class)))
                                                     .addStatement("return new $T[][] {\n$L\n}", Object.class,
                                                                   CodeBlock.join(rows, ",\n"))
                                                     .build())
                                .build();
        Class<?> compiled = RuntimeCompiler.compileAndLoad(JavaFile.builder(name.packageName(), type).build(), dir);
        rendered = (Object[][]) compiled.getMethod("values").invoke(null);
    }

    @Test
    void renderedTraitsEqualRuntimeFactoryTraits() {
        TraitInitializers registry = TraitInitializers.of(Collections.emptyList());
        for (int i = 0; i < traits.size(); i++) {
            Trait modelTrait = traits.get(i);
            Object value = rendered[i][0];
            software.amazon.awssdk.schema.ShapeId id = (software.amazon.awssdk.schema.ShapeId) rendered[i][1];
            Document document = (Document) rendered[i][2];
            String description = modelTrait.toShapeId() + " " + Node.printJson(modelTrait.toNode());

            assertThat(id.toString()).as(description).isEqualTo(modelTrait.toShapeId().toString());
            assertThat(Document.equals(document, expectedDocument(modelTrait.toNode()))).as(description).isTrue();

            software.amazon.awssdk.schema.traits.Trait runtimeTrait = (software.amazon.awssdk.schema.traits.Trait) value;
            software.amazon.awssdk.schema.traits.Trait expected = TraitFactories.create(id, document);
            assertThat(runtimeTrait.traitId()).as(description).isEqualTo(id);
            assertThat(runtimeTrait).as(description).isEqualTo(expected);
            assertThat(runtimeTrait.getClass()).as(description).isEqualTo(expected.getClass());
            if (registry.initializerFor(modelTrait.toShapeId()) == null) {
                assertThat(runtimeTrait).as(description).isInstanceOf(GenericTrait.class);
                assertThat(Document.equals(((GenericTrait) runtimeTrait).value(), document)).as(description).isTrue();
            } else {
                assertThat(runtimeTrait).as(description).isNotInstanceOf(GenericTrait.class);
            }
        }
    }

    @Test
    void annotationTraitsRenderSharedInstances() {
        for (int i = 0; i < traits.size(); i++) {
            if (traits.get(i).toShapeId().equals(ShapeId.from("smithy.api#required"))) {
                assertThat(rendered[i][0]).isSameAs(software.amazon.awssdk.schema.traits.RequiredTrait.INSTANCE);
                return;
            }
        }
        throw new AssertionError("no required trait case");
    }

    @Test
    void multiMemberDocumentsKeepMemberOrder() {
        for (int i = 0; i < traits.size(); i++) {
            if (traits.get(i).toShapeId().equals(ShapeId.from("test.traits#custom"))) {
                Document document = (Document) rendered[i][2];
                assertThat(new ArrayList<>(document.asMap().keySet())).containsExactly("z", "a", "m", "big");
                return;
            }
        }
        throw new AssertionError("no custom trait case");
    }

    @Test
    void builtInsCoverEveryTypedRuntimeTrait() {
        TraitInitializers registry = TraitInitializers.of(Collections.emptyList());
        assertThat(registry.initializers()).hasSize(48);
        for (Map.Entry<ShapeId, TraitInitializer<?>> entry : registry.initializers().entrySet()) {
            TraitFactory<?> factory = TraitFactories.factoryFor(
                software.amazon.awssdk.schema.ShapeId.from(entry.getKey().toString()));
            assertThat(factory).as(entry.getKey().toString()).isNotNull();
            ClassName runtimeType = ((CoreSchemaCodegenExtension.NodeTraitInitializer) entry.getValue()).runtimeType();
            assertThat(runtimeType.reflectionName()).as(entry.getKey().toString())
                                                    .isEqualTo(factory.traitClass().getName());
        }

        Set<ShapeId> covered = new TreeSet<>();
        for (Trait trait : traits) {
            covered.add(trait.toShapeId());
        }
        assertThat(covered).containsAll(registry.initializers().keySet());
    }

    @Test
    void everyDefaultTraitHasTypedInitializer() {
        TraitInitializers registry = TraitInitializers.of(Collections.emptyList());
        assertThat(DefaultTraitInclusion.DEFAULT_TRAITS).allSatisfy(id -> assertThat(registry.initializerFor(id)).isNotNull());
    }

    @Test
    void extensionInitializerReplacesGenericRendering() {
        ShapeId deprecated = ShapeId.from("smithy.api#deprecated");
        SchemaCodegenExtension extension = extension(initializer(deprecated, Trait.class, "custom"));
        TraitInitializers registry = TraitInitializers.of(Collections.singletonList(extension));

        Trait trait = new DynamicTrait(deprecated, Node.objectNode());
        assertThat(registry.render(trait, TraitRenderContext.defaultContext()).toString()).isEqualTo("custom");
        assertThat(TraitInitializers.of(Collections.emptyList()).render(trait, TraitRenderContext.defaultContext())
                                    .toString())
            .startsWith("new software.amazon.awssdk.schema.traits.GenericTrait(");
    }

    @Test
    void secondInitializerForBuiltInTraitFails() {
        ShapeId jsonName = ShapeId.from("smithy.api#jsonName");
        SchemaCodegenExtension extension = extension(initializer(jsonName, Trait.class, "x"));

        assertThatThrownBy(() -> TraitInitializers.of(Collections.singletonList(extension)))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#jsonName")
            .hasMessageContaining("more than one initializer");
    }

    @Test
    void twoExtensionsForOneTraitFail() {
        ShapeId custom = ShapeId.from("test.traits#custom");
        SchemaCodegenExtension first = extension(initializer(custom, Trait.class, "a"));
        SchemaCodegenExtension second = extension(initializer(custom, Trait.class, "b"));

        assertThatThrownBy(() -> TraitInitializers.of(Arrays.asList(first, second)))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("test.traits#custom");
    }

    @Test
    void loadWithoutServiceProvidersHasOnlyBuiltIns() {
        assertThat(TraitInitializers.load(getClass().getClassLoader()).initializers())
            .isEqualTo(TraitInitializers.of(Collections.emptyList()).initializers());
    }

    @Test
    void initializerForAnotherTraitClassFails() {
        ShapeId custom = ShapeId.from("test.traits#custom");
        SchemaCodegenExtension extension = extension(initializer(custom, JsonNameTrait.class, "x"));
        TraitInitializers registry = TraitInitializers.of(Collections.singletonList(extension));

        assertThatThrownBy(() -> registry.render(new DynamicTrait(custom, Node.objectNode()),
                                                 TraitRenderContext.defaultContext()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("test.traits#custom")
            .hasMessageContaining(JsonNameTrait.class.getName());
    }

    @Test
    void malformedTraitValueFailsNamingTheTrait() {
        TraitInitializers registry = TraitInitializers.of(Collections.emptyList());
        Trait http = new DynamicTrait(ShapeId.from("smithy.api#http"), Node.from("GET /"));
        Trait httpError = new DynamicTrait(ShapeId.from("smithy.api#httpError"), Node.from(4.5));

        assertThatThrownBy(() -> registry.render(http, TraitRenderContext.defaultContext()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#http");
        assertThatThrownBy(() -> registry.render(httpError, TraitRenderContext.defaultContext()))
            .isInstanceOf(SchemaCodegenException.class)
            .hasMessageContaining("smithy.api#httpError");
    }

    private static SchemaCodegenExtension extension(TraitInitializer<?> initializer) {
        return new SchemaCodegenExtension() {
            @Override
            public List<TraitInitializer<?>> traitInitializers() {
                return Collections.singletonList(initializer);
            }
        };
    }

    private static <T extends Trait> TraitInitializer<T> initializer(ShapeId id, Class<T> traitClass, String code) {
        return new TraitInitializer<T>() {
            @Override
            public ShapeId traitId() {
                return id;
            }

            @Override
            public Class<T> traitClass() {
                return traitClass;
            }

            @Override
            public CodeBlock render(T trait, TraitRenderContext context) {
                return CodeBlock.of(code);
            }
        };
    }

    /**
     * The node-to-document mapping documented on {@link DocumentLiterals}, computed in Java.
     */
    private static Document expectedDocument(Node node) {
        if (node.isNullNode()) {
            return null;
        }
        if (node.isBooleanNode()) {
            return Document.of(node.expectBooleanNode().getValue());
        }
        if (node.isStringNode()) {
            return Document.of(node.expectStringNode().getValue());
        }
        if (node.isNumberNode()) {
            Number value = node.expectNumberNode().getValue();
            if (value instanceof BigDecimal) {
                return Document.of((BigDecimal) value);
            }
            if (value instanceof Double || value instanceof Float) {
                return Document.of(value.doubleValue());
            }
            BigInteger big = value instanceof BigInteger ? (BigInteger) value : BigInteger.valueOf(value.longValue());
            if (big.bitLength() < Integer.SIZE) {
                return Document.of(big.intValue());
            }
            return big.bitLength() < Long.SIZE ? Document.of(big.longValue()) : Document.of(big);
        }
        if (node.isArrayNode()) {
            List<Document> elements = new ArrayList<>();
            for (Node element : node.expectArrayNode().getElements()) {
                elements.add(expectedDocument(element));
            }
            return Document.of(elements);
        }
        Map<String, Document> members = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : node.expectObjectNode().getStringMap().entrySet()) {
            members.put(entry.getKey(), expectedDocument(entry.getValue()));
        }
        return Document.of(members);
    }

    private static List<Trait> cases() {
        List<Trait> result = new ArrayList<>();
        for (String annotation : Arrays.asList(
            "smithy.api#required", "smithy.api#sensitive", "smithy.api#sparse", "smithy.api#idempotencyToken",
            "smithy.api#streaming", "smithy.api#requiresLength", "smithy.api#eventHeader", "smithy.api#eventPayload",
            "smithy.api#hostLabel", "smithy.api#httpChecksumRequired", "smithy.api#xmlAttribute", "smithy.api#xmlFlattened",
            "smithy.api#httpLabel", "smithy.api#httpQueryParams", "smithy.api#httpPayload", "smithy.api#httpResponseCode",
            "smithy.api#unitType", "smithy.api#optionalAuth", "aws.protocols#awsQueryCompatible", "aws.protocols#awsQuery",
            "aws.protocols#ec2Query", "aws.auth#unsignedPayload")) {
            result.add(trait(annotation, "{}"));
        }
        result.add(trait("smithy.api#error", "\"client\""));
        result.add(trait("smithy.api#error", "\"server\""));
        result.add(trait("smithy.api#mediaType", "\"application/json\""));
        result.add(trait("smithy.api#xmlName", "\"Item\""));
        result.add(trait("smithy.api#jsonName", "\"item_name\""));
        result.add(trait("smithy.api#timestampFormat", "\"epoch-seconds\""));
        result.add(trait("smithy.api#httpHeader", "\"X-Amz-Meta\""));
        result.add(trait("smithy.api#httpQuery", "\"max-items\""));
        result.add(trait("smithy.api#httpPrefixHeaders", "\"X-Meta-\""));
        result.add(trait("aws.protocols#ec2QueryName", "\"Item\""));

        for (String value : Arrays.asList("0", "-7", "\"text\"", "true", "[]", "{}", "1.5", "9999999999", "null",
                                          "[\"a\", \"b\"]", "{\"k\": \"v\"}")) {
            result.add(trait("smithy.api#default", value));
        }
        result.add(trait("smithy.api#httpError", "404"));
        result.add(trait("smithy.api#retryable", "{}"));
        result.add(trait("smithy.api#retryable", "{\"throttling\": true}"));
        result.add(trait("smithy.api#endpoint", "{\"hostPrefix\": \"{Bucket}.data-\"}"));
        result.add(trait("smithy.api#http", "{\"method\": \"GET\", \"uri\": \"/items/{id}\"}"));
        result.add(trait("smithy.api#http", "{\"method\": \"PUT\", \"uri\": \"/\", \"code\": 204}"));
        result.add(trait("smithy.api#requestCompression", "{\"encodings\": [\"gzip\"]}"));
        result.add(trait("smithy.api#requestCompression", "{\"encodings\": [\"gzip\", \"br\"]}"));
        result.add(trait("smithy.api#paginated", "{}"));
        result.add(trait("smithy.api#paginated", "{\"inputToken\": \"NextToken\", \"outputToken\": \"NextToken\", "
                                                 + "\"items\": \"Items\", \"pageSize\": \"MaxResults\"}"));
        result.add(trait("smithy.api#paginated", "{\"outputToken\": \"Marker\"}"));
        result.add(trait("smithy.api#xmlNamespace", "{\"uri\": \"https://example.com/doc/\"}"));
        result.add(trait("smithy.api#xmlNamespace", "{\"uri\": \"https://example.com/doc/\", \"prefix\": \"ex\"}"));
        result.add(trait("smithy.api#auth", "[]"));
        result.add(trait("smithy.api#auth", "[\"aws.auth#sigv4\"]"));
        result.add(trait("smithy.api#auth", "[\"aws.auth#sigv4\", \"smithy.api#httpBearerAuth\"]"));
        result.add(trait("aws.protocols#httpChecksum", "{\"requestChecksumRequired\": true}"));
        result.add(trait("aws.protocols#httpChecksum", "{\"requestAlgorithmMember\": \"Algorithm\", "
                                                       + "\"requestValidationModeMember\": \"Mode\", "
                                                       + "\"responseAlgorithms\": [\"CRC32\", \"SHA256\"]}"));
        result.add(trait("aws.protocols#awsQueryError", "{\"code\": \"AWS.Queue.Missing\", \"httpResponseCode\": 400}"));
        result.add(trait("aws.auth#sigv4", "{\"name\": \"monitoring\"}"));

        result.add(trait("aws.protocols#awsJson1_0", "{}"));
        result.add(trait("aws.protocols#awsJson1_0", "{\"http\": [\"h2\"], \"eventStreamHttp\": [\"h2\"]}"));
        result.add(trait("aws.protocols#awsJson1_1", "{}"));
        result.add(trait("aws.protocols#restJson1", "{\"http\": [\"h2\", \"http/1.1\"]}"));
        result.add(trait("smithy.protocols#rpcv2Cbor", "{}"));
        result.add(trait("aws.protocols#restXml", "{}"));
        result.add(trait("aws.protocols#restXml", "{\"noErrorWrapping\": true}"));
        result.add(trait("aws.protocols#restXml", "{\"http\": [\"http/1.1\"]}"));

        // Rendered as GenericTrait.
        result.add(trait("aws.auth#sigv4a", "{\"name\": \"s3\"}"));
        result.add(trait("smithy.api#httpBearerAuth", "{}"));
        result.add(trait("smithy.api#deprecated", "{\"message\": \"Use Other\", \"since\": \"2024-01-01\"}"));
        result.add(trait("test.traits#custom", "{\"z\": [1, 2.5, null, \"s\", false, -3000000000], \"a\": null, "
                                               + "\"m\": {\"n\": {\"o\": []}, \"p\": 12345678901}, "
                                               + "\"big\": 123456789012345678901234567890}"));
        result.add(trait("test.traits#scalar", "\"only a string\""));
        result.add(trait("test.traits#list", "[[], [{}], {\"x\": [null]}]"));
        result.add(trait("test.traits#nothing", "null"));
        return result;
    }

    private static Trait trait(String id, String json) {
        ShapeId traitId = ShapeId.from(id);
        Node value = Node.parse(json);
        return MODEL_TRAITS.createTrait(traitId, TARGET, value).orElseGet(() -> new DynamicTrait(traitId, value));
    }
}
