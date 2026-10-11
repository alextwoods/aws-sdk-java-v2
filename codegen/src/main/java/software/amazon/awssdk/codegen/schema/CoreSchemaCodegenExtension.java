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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * The SDK's built-in trait initializers, one per typed runtime trait in {@code software.amazon.awssdk.schema.traits}
 * (port of smithy-java {@code CoreIntegration#traitInitializers}). Traits outside this set are rendered by
 * {@link GenericTraitInitializer}.
 *
 * <p>Every initializer reads the trait's {@link Trait#toNode() node value} rather than smithy-model getters, so it
 * renders the same expression whether or not the trait's smithy-model class is on the class path. The arguments follow
 * the runtime constructors, with null for an absent optional property.
 */
@SdkInternalApi
public final class CoreSchemaCodegenExtension implements SchemaCodegenExtension {
    private static final List<TraitInitializer<?>> INITIALIZERS = Collections.unmodifiableList(createInitializers());

    @Override
    public List<TraitInitializer<?>> traitInitializers() {
        return INITIALIZERS;
    }

    private static List<TraitInitializer<?>> createInitializers() {
        List<TraitInitializer<?>> result = new ArrayList<>();

        // Annotation traits: shared singletons.
        result.add(annotation("smithy.api#required", "RequiredTrait"));
        result.add(annotation("smithy.api#sensitive", "SensitiveTrait"));
        result.add(annotation("smithy.api#sparse", "SparseTrait"));
        result.add(annotation("smithy.api#idempotencyToken", "IdempotencyTokenTrait"));
        result.add(annotation("smithy.api#streaming", "StreamingTrait"));
        result.add(annotation("smithy.api#requiresLength", "RequiresLengthTrait"));
        result.add(annotation("smithy.api#eventHeader", "EventHeaderTrait"));
        result.add(annotation("smithy.api#eventPayload", "EventPayloadTrait"));
        result.add(annotation("smithy.api#hostLabel", "HostLabelTrait"));
        result.add(annotation("smithy.api#httpChecksumRequired", "HttpChecksumRequiredTrait"));
        result.add(annotation("smithy.api#xmlAttribute", "XmlAttributeTrait"));
        result.add(annotation("smithy.api#xmlFlattened", "XmlFlattenedTrait"));
        result.add(annotation("smithy.api#httpLabel", "HttpLabelTrait"));
        result.add(annotation("smithy.api#httpQueryParams", "HttpQueryParamsTrait"));
        result.add(annotation("smithy.api#httpPayload", "HttpPayloadTrait"));
        result.add(annotation("smithy.api#httpResponseCode", "HttpResponseCodeTrait"));
        result.add(annotation("smithy.api#unitType", "UnitTypeTrait"));
        result.add(annotation("smithy.api#optionalAuth", "OptionalAuthTrait"));
        result.add(annotation("aws.protocols#awsQueryCompatible", "AwsQueryCompatibleTrait"));
        result.add(annotation("aws.protocols#awsQuery", "AwsQueryTrait"));
        result.add(annotation("aws.protocols#ec2Query", "Ec2QueryTrait"));
        result.add(annotation("aws.auth#unsignedPayload", "UnsignedPayloadTrait"));

        // String traits: new X("value").
        result.add(string("smithy.api#error", "ErrorTrait"));
        result.add(string("smithy.api#mediaType", "MediaTypeTrait"));
        result.add(string("smithy.api#xmlName", "XmlNameTrait"));
        result.add(string("smithy.api#jsonName", "JsonNameTrait"));
        result.add(string("smithy.api#timestampFormat", "TimestampFormatTrait"));
        result.add(string("smithy.api#httpHeader", "HttpHeaderTrait"));
        result.add(string("smithy.api#httpQuery", "HttpQueryTrait"));
        result.add(string("smithy.api#httpPrefixHeaders", "HttpPrefixHeadersTrait"));
        result.add(string("aws.protocols#ec2QueryName", "Ec2QueryNameTrait"));

        // Traits with structured values.
        result.add(of("smithy.api#default", "DefaultTrait",
            (type, node, ctx) -> CodeBlock.of("new $T($L)", type, ctx.document(node))));
        result.add(of("smithy.api#httpError", "HttpErrorTrait",
            (type, node, ctx) -> CodeBlock.of("new $T($L)", type, intValue(node, "value"))));
        result.add(of("smithy.api#retryable", "RetryableTrait",
            (type, node, ctx) -> CodeBlock.of("new $T($L)", type,
                                              node.expectObjectNode().getBooleanMemberOrDefault("throttling", false))));
        result.add(of("smithy.api#endpoint", "EndpointTrait",
            (type, node, ctx) -> CodeBlock.of("new $T($S)", type,
                                              node.expectObjectNode().expectStringMember("hostPrefix").getValue())));
        result.add(of("smithy.api#http", "HttpTrait", CoreSchemaCodegenExtension::http));
        result.add(of("smithy.api#requestCompression", "RequestCompressionTrait",
            (type, node, ctx) -> CodeBlock.of("new $T($L)", type,
                                              ctx.stringList(stringList(node.expectObjectNode(), "encodings", true)))));
        result.add(of("smithy.api#paginated", "PaginatedTrait", CoreSchemaCodegenExtension::paginated));
        result.add(of("smithy.api#xmlNamespace", "XmlNamespaceTrait", CoreSchemaCodegenExtension::xmlNamespace));
        result.add(of("smithy.api#auth", "AuthTrait", CoreSchemaCodegenExtension::auth));
        result.add(of("aws.protocols#httpChecksum", "HttpChecksumTrait", CoreSchemaCodegenExtension::httpChecksum));
        result.add(of("aws.protocols#awsQueryError", "AwsQueryErrorTrait",
            (type, node, ctx) -> {
                ObjectNode object = node.expectObjectNode();
                return CodeBlock.of("new $T($S, $L)", type, object.expectStringMember("code").getValue(),
                                    intValue(object.expectMember("httpResponseCode"), "httpResponseCode"));
            }));
        result.add(of("aws.auth#sigv4", "SigV4Trait",
            (type, node, ctx) -> CodeBlock.of("new $T($S)", type,
                                              node.expectObjectNode().expectStringMember("name").getValue())));

        // Protocol definition traits.
        result.add(of("aws.protocols#awsJson1_0", "AwsJson1_0Trait", CoreSchemaCodegenExtension::protocol));
        result.add(of("aws.protocols#awsJson1_1", "AwsJson1_1Trait", CoreSchemaCodegenExtension::protocol));
        result.add(of("aws.protocols#restJson1", "RestJson1Trait", CoreSchemaCodegenExtension::protocol));
        result.add(of("smithy.protocols#rpcv2Cbor", "Rpcv2CborTrait", CoreSchemaCodegenExtension::protocol));
        result.add(of("aws.protocols#restXml", "RestXmlTrait", CoreSchemaCodegenExtension::restXml));
        return result;
    }

    private static CodeBlock http(ClassName type, Node node, TraitRenderContext ctx) {
        ObjectNode object = node.expectObjectNode();
        String method = object.expectStringMember("method").getValue();
        String uri = object.expectStringMember("uri").getValue();
        Optional<Node> code = object.getMember("code");
        if (code.isPresent()) {
            int value = intValue(code.get(), "code");
            if (value != 200) {
                return CodeBlock.of("new $T($S, $S, $L)", type, method, uri, value);
            }
        }
        return CodeBlock.of("new $T($S, $S)", type, method, uri);
    }

    private static CodeBlock paginated(ClassName type, Node node, TraitRenderContext ctx) {
        ObjectNode object = node.expectObjectNode();
        return CodeBlock.of("new $T($S, $S, $S, $S)", type,
                            optionalString(object, "inputToken"), optionalString(object, "outputToken"),
                            optionalString(object, "items"), optionalString(object, "pageSize"));
    }

    private static CodeBlock xmlNamespace(ClassName type, Node node, TraitRenderContext ctx) {
        ObjectNode object = node.expectObjectNode();
        String uri = object.expectStringMember("uri").getValue();
        String prefix = optionalString(object, "prefix");
        return prefix == null ? CodeBlock.of("new $T($S)", type, uri) : CodeBlock.of("new $T($S, $S)", type, uri, prefix);
    }

    private static CodeBlock auth(ClassName type, Node node, TraitRenderContext ctx) {
        List<ShapeId> ids = new ArrayList<>();
        for (StringNode element : node.expectArrayNode().getElementsAs(StringNode.class)) {
            ids.add(element.expectShapeId());
        }
        return CodeBlock.of("new $T($L)", type, ctx.shapeIdList(ids));
    }

    private static CodeBlock httpChecksum(ClassName type, Node node, TraitRenderContext ctx) {
        ObjectNode object = node.expectObjectNode();
        return CodeBlock.of("new $T($L, $S, $S, $L)", type,
                            object.getBooleanMemberOrDefault("requestChecksumRequired", false),
                            optionalString(object, "requestAlgorithmMember"),
                            optionalString(object, "requestValidationModeMember"),
                            ctx.stringList(stringList(object, "responseAlgorithms", false)));
    }

    private static CodeBlock protocol(ClassName type, Node node, TraitRenderContext ctx) {
        ObjectNode object = node.expectObjectNode();
        List<String> http = stringList(object, "http", false);
        List<String> eventStreamHttp = stringList(object, "eventStreamHttp", false);
        if (http.isEmpty() && eventStreamHttp.isEmpty()) {
            return CodeBlock.of("new $T()", type);
        }
        return CodeBlock.of("new $T($L, $L)", type, ctx.stringList(http), ctx.stringList(eventStreamHttp));
    }

    private static CodeBlock restXml(ClassName type, Node node, TraitRenderContext ctx) {
        ObjectNode object = node.expectObjectNode();
        List<String> http = stringList(object, "http", false);
        List<String> eventStreamHttp = stringList(object, "eventStreamHttp", false);
        boolean noErrorWrapping = object.getBooleanMemberOrDefault("noErrorWrapping", false);
        if (http.isEmpty() && eventStreamHttp.isEmpty() && !noErrorWrapping) {
            return CodeBlock.of("new $T()", type);
        }
        return CodeBlock.of("new $T($L, $L, $L)", type, ctx.stringList(http), ctx.stringList(eventStreamHttp),
                            noErrorWrapping);
    }

    private static String optionalString(ObjectNode object, String member) {
        Optional<StringNode> value = object.getStringMember(member);
        return value.isPresent() ? value.get().getValue() : null;
    }

    private static List<String> stringList(ObjectNode object, String member, boolean required) {
        Optional<Node> value = object.getMember(member);
        if (!value.isPresent()) {
            if (required) {
                object.expectMember(member);
            }
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (StringNode element : value.get().expectArrayNode().getElementsAs(StringNode.class)) {
            result.add(element.getValue());
        }
        return result;
    }

    private static int intValue(Node node, String property) {
        NumberNode number = node.expectNumberNode();
        Number value = number.getValue();
        if (number.isNaturalNumber() && value.longValue() >= Integer.MIN_VALUE && value.longValue() <= Integer.MAX_VALUE) {
            return value.intValue();
        }
        throw new SchemaCodegenException("Expected " + property + " to be an integer in int range, but found " + value);
    }

    private static TraitInitializer<Trait> annotation(String traitId, String runtimeClass) {
        return of(traitId, runtimeClass, (type, node, ctx) -> {
            node.expectObjectNode();
            return CodeBlock.of("$T.INSTANCE", type);
        });
    }

    private static TraitInitializer<Trait> string(String traitId, String runtimeClass) {
        return of(traitId, runtimeClass,
            (type, node, ctx) -> CodeBlock.of("new $T($S)", type, node.expectStringNode().getValue()));
    }

    private static TraitInitializer<Trait> of(String traitId, String runtimeClass, NodeRenderer renderer) {
        return new NodeTraitInitializer(ShapeId.from(traitId), SchemaRuntimeType.trait(runtimeClass), renderer);
    }

    @FunctionalInterface
    private interface NodeRenderer {
        CodeBlock render(ClassName runtimeType, Node value, TraitRenderContext context);
    }

    /**
     * A built-in initializer: renders the runtime trait {@code runtimeType} from the trait's node value.
     */
    static final class NodeTraitInitializer implements TraitInitializer<Trait> {
        private final ShapeId traitId;
        private final ClassName runtimeType;
        private final NodeRenderer renderer;

        private NodeTraitInitializer(ShapeId traitId, ClassName runtimeType, NodeRenderer renderer) {
            this.traitId = traitId;
            this.runtimeType = runtimeType;
            this.renderer = renderer;
        }

        @Override
        public ShapeId traitId() {
            return traitId;
        }

        @Override
        public Class<Trait> traitClass() {
            return Trait.class;
        }

        /**
         * @return the runtime trait class this initializer instantiates
         */
        ClassName runtimeType() {
            return runtimeType;
        }

        @Override
        public CodeBlock render(Trait trait, TraitRenderContext context) {
            return renderer.render(runtimeType, trait.toNode(), context);
        }

        @Override
        public String toString() {
            return "NodeTraitInitializer(" + traitId + " -> " + runtimeType + ")";
        }
    }
}
