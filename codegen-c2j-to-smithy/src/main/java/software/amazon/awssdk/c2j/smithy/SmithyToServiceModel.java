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

package software.amazon.awssdk.c2j.smithy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.codegen.checksum.HttpChecksum;
import software.amazon.awssdk.codegen.model.service.ErrorMap;
import software.amazon.awssdk.codegen.model.service.ErrorTrait;
import software.amazon.awssdk.codegen.model.service.Http;
import software.amazon.awssdk.codegen.model.service.Input;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.Output;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.awssdk.codegen.model.service.XmlNamespace;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Converts a canonical Smithy {@link Model} back into an in-memory AWS C2J {@link ServiceModel} POJO.
 *
 * <p>This is the exact inverse of {@link C2jToSmithyConverter}: it inverts every mapping rule that
 * the forward converter applies. Where {@link C2jToSmithyConverter} treats C2J as an input format and
 * produces a Smithy {@link Model} as the source of truth, this class lets a Smithy model feed AWS SDK
 * for Java v2's existing {@code IntermediateModelBuilder(C2jModels)} pipeline without ever touching a
 * {@code service-2.json} file: the produced {@link ServiceModel} is the same POJO that v2's Jackson
 * deserializer would have produced from disk.
 *
 * <p>Smithy's protocol-specific binding traits ({@code @httpLabel}/{@code @httpHeader}/
 * {@code @httpQuery}/{@code @httpResponseCode}/{@code @httpPrefixHeaders}/{@code @httpPayload}/
 * {@code @jsonName}/{@code @xmlName}/{@code @timestampFormat}/{@code @required}) are folded back into
 * C2J's protocol-coupled {@code location}/{@code locationName}/{@code payload}/{@code required}
 * fields.
 *
 * <p>Shape and member references use Smithy shape SIMPLE names ({@link ShapeId#getName()}), matching
 * the keys {@link C2jToSmithyConverter} used (it built ids as {@code namespace#SimpleName} and the C2J
 * model keys/refs are simple names).
 *
 * <p>Self-contained except for the v2 {@code codegen} dependency that supplies the C2J POJOs; it never
 * touches v2 codegen logic.
 */
public final class SmithyToServiceModel {

    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    private final Model model;
    private final ServiceShape service;
    private final String namespace;

    private SmithyToServiceModel(Model model, ServiceShape service) {
        this.model = model;
        this.service = service;
        this.namespace = service.getId().getNamespace();
    }

    /**
     * Convert a Smithy {@link Model} into an in-memory C2J {@link ServiceModel}.
     *
     * <p>The model is expected to contain exactly one {@link ServiceShape} (the shape produced by
     * {@link C2jToSmithyConverter#buildService()}); the first service shape found is used.
     */
    public static ServiceModel convert(Model model) {
        ServiceShape service = model.getServiceShapes().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Smithy model has no service shape"));
        return new SmithyToServiceModel(model, service).build();
    }

    /**
     * Convenience round-trip helper: converts a C2J {@code service-2.json} to Smithy via
     * {@link C2jToSmithyConverter} and then straight back to a C2J {@link ServiceModel}. Useful for
     * tests that want to exercise both directions.
     */
    public static ServiceModel fromC2jViaSmithy(Path serviceJson) {
        return convert(C2jToSmithyConverter.convert(serviceJson));
    }

    private ServiceModel build() {
        ServiceModel serviceModel = new ServiceModel();
        serviceModel.setMetadata(buildMetadata());

        // Operations (skip anything outside the service's own namespace). Sorted by name to match the
        // alphabetical C2J file order (see the shapes block below for why order matters).
        Map<String, Operation> operations = new LinkedHashMap<>();
        List<OperationShape> sortedOps = new ArrayList<>(model.getOperationShapes());
        sortedOps.sort(java.util.Comparator.comparing(o -> o.getId().getName()));
        for (OperationShape op : sortedOps) {
            if (!ownNamespace(op.getId())) {
                continue;
            }
            operations.put(op.getId().getName(), buildOperation(op));
        }
        serviceModel.setOperations(operations);

        // Shapes: structures/unions, lists, maps, and simple shapes. Skip the synthetic Unit shape
        // and any prelude shape (only emit shapes in the model's own namespace).
        //
        // Emit in shape-name order. AWS C2J service-2.json files list shapes alphabetically, and v2's
        // direct path deserializes them into an insertion-ordered map; some IR steps are
        // order-dependent (e.g. AddExceptionShapes does "last class-name wins" into a HashMap, so when
        // two C2J shapes map to one Java class the file order decides the winner's errorCode). Sorting
        // by name reproduces the C2J file order, keeping the IR byte-identical. Smithy model.toSet() is
        // unordered, so we sort explicitly.
        Map<String, Shape> shapes = new LinkedHashMap<>();
        List<software.amazon.smithy.model.shapes.Shape> sorted = new ArrayList<>(model.toSet());
        sorted.sort(java.util.Comparator.comparing(sh -> sh.getId().getName()));
        for (software.amazon.smithy.model.shapes.Shape shape : sorted) {
            ShapeId id = shape.getId();
            if (id.equals(UNIT) || !ownNamespace(id) || shape.isMemberShape()) {
                continue;
            }
            Shape c2j = convertShape(shape);
            if (c2j != null) {
                doc(shape).ifPresent(c2j::setDocumentation);   // @documentation -> C2J shape doc
                if (shape.hasTrait(software.amazon.smithy.model.traits.XmlFlattenedTrait.class)) {
                    c2j.setFlattened(true);                    // @xmlFlattened -> C2J shape flattened
                }
                // Event-stream marker traits -> C2J eventstream/event booleans (the IR reads these).
                if (shape.findTrait(C2jToSmithyConverter.C2J_EVENTSTREAM_TRAIT).isPresent()) {
                    c2j.setEventstream(true);
                }
                if (shape.findTrait(C2jToSmithyConverter.C2J_EVENT_TRAIT).isPresent()) {
                    c2j.setEvent(true);
                }
                // Shape-level @timestampFormat -> C2J shape timestampFormat.
                shape.getTrait(TimestampFormatTrait.class)
                     .ifPresent(t -> c2j.setTimestampFormat(c2jTimestampFormat(t.getValue())));
                // Shape-level @sensitive -> C2J shape sensitive.
                if (shape.hasTrait(software.amazon.smithy.model.traits.SensitiveTrait.class)) {
                    c2j.setSensitive(true);
                }
                // Shape-level @deprecated -> C2J deprecated + deprecatedMessage.
                shape.getTrait(software.amazon.smithy.model.traits.DeprecatedTrait.class).ifPresent(dt -> {
                    c2j.setDeprecated(true);
                    dt.getMessage().ifPresent(c2j::setDeprecatedMessage);
                });
                // Shape-level @streaming (+ @requiresLength) -> C2J streaming/requiresLength.
                if (shape.hasTrait(software.amazon.smithy.model.traits.StreamingTrait.class)) {
                    c2j.setStreaming(true);
                }
                if (shape.hasTrait(software.amazon.smithy.model.traits.RequiresLengthTrait.class)) {
                    c2j.setRequiresLength(true);
                }
                // Shape-level @xmlNamespace -> C2J shape xmlNamespace (uri[, prefix]).
                shape.getTrait(XmlNamespaceTrait.class).ifPresent(xn -> {
                    XmlNamespace ns = new XmlNamespace();
                    ns.setUri(xn.getUri());
                    xn.getPrefix().ifPresent(ns::setPrefix);
                    c2j.setXmlNamespace(ns);
                });
                // Shape-level @retryable -> C2J retryable{throttling}.
                shape.getTrait(software.amazon.smithy.model.traits.RetryableTrait.class).ifPresent(rt -> {
                    software.amazon.awssdk.codegen.model.service.RetryableTrait c2jRetry =
                            new software.amazon.awssdk.codegen.model.service.RetryableTrait();
                    c2jRetry.setThrottling(rt.getThrottling());
                    c2j.setRetryable(c2jRetry);
                });
                shapes.put(id.getName(), c2j);
            }
        }
        serviceModel.setShapes(shapes);

        // Service-level documentation.
        doc(service).ifPresent(serviceModel::setDocumentation);

        // Top-level clientContextParams block.
        service.findTrait(C2jToSmithyConverter.C2J_CLIENT_CONTEXT_PARAMS_TRAIT).ifPresent(t -> {
            Map<String, software.amazon.awssdk.codegen.model.service.ClientContextParam> ccp =
                    new LinkedHashMap<>();
            t.toNode().expectObjectNode().getStringMap().forEach((name, node) -> {
                ObjectNode pn = node.expectObjectNode();
                software.amazon.awssdk.codegen.model.service.ClientContextParam p =
                        new software.amazon.awssdk.codegen.model.service.ClientContextParam();
                pn.getStringMember("type").ifPresent(v -> p.setType(v.getValue()));
                pn.getStringMember("documentation").ifPresent(v -> p.setDocumentation(v.getValue()));
                ccp.put(name, p);
            });
            serviceModel.setClientContextParams(ccp);
        });
        return serviceModel;
    }

    private boolean ownNamespace(ShapeId id) {
        return namespace.equals(id.getNamespace());
    }

    /** The value of a Smithy {@code @documentation} trait on a shape/member, if present. */
    private static Optional<String> doc(software.amazon.smithy.model.shapes.Shape shape) {
        return shape.getTrait(software.amazon.smithy.model.traits.DocumentationTrait.class)
                    .map(software.amazon.smithy.model.traits.DocumentationTrait::getValue);
    }

    // ----- metadata -----

    private ServiceMetadata buildMetadata() {
        // Preferred path: C2jToSmithyConverter stashes the verbatim C2J metadata block as a custom
        // node trait, so service identity (endpointPrefix/signingName/targetPrefix/signatureVersion/
        // uid/auth/protocols/...) round-trips losslessly. Restore directly from that node.
        Optional<Trait> preserved = service.findTrait(C2jToSmithyConverter.C2J_METADATA_TRAIT);
        if (preserved.isPresent()) {
            return metadataFromNode(preserved.get().toNode().expectObjectNode());
        }
        // Fallback: a hand-written Smithy model that never went through the forward converter.
        return metadataFromTraits();
    }

    // Restore ServiceMetadata from the preserved verbatim C2J metadata object node (lossless path).
    private ServiceMetadata metadataFromNode(ObjectNode node) {
        ServiceMetadata metadata = new ServiceMetadata();
        node.getStringMember("apiVersion").ifPresent(n -> metadata.setApiVersion(n.getValue()));
        node.getStringMember("endpointPrefix").ifPresent(n -> metadata.setEndpointPrefix(n.getValue()));
        node.getStringMember("signingName").ifPresent(n -> metadata.setSigningName(n.getValue()));
        node.getStringMember("serviceAbbreviation").ifPresent(n -> metadata.setServiceAbbreviation(n.getValue()));
        node.getStringMember("serviceFullName").ifPresent(n -> metadata.setServiceFullName(n.getValue()));
        node.getStringMember("serviceId").ifPresent(n -> metadata.setServiceId(n.getValue()));
        node.getStringMember("xmlNamespace").ifPresent(n -> metadata.setXmlNamespace(n.getValue()));
        node.getStringMember("protocol").ifPresent(n -> metadata.setProtocol(n.getValue()));
        node.getStringMember("jsonVersion").ifPresent(n -> metadata.setJsonVersion(n.getValue()));
        node.getStringMember("signatureVersion").ifPresent(n -> metadata.setSignatureVersion(n.getValue()));
        node.getStringMember("targetPrefix").ifPresent(n -> metadata.setTargetPrefix(n.getValue()));
        node.getStringMember("uid").ifPresent(n -> metadata.setUid(n.getValue()));
        node.getArrayMember("protocols").ifPresent(a -> metadata.setProtocols(stringList(a)));
        node.getArrayMember("auth").ifPresent(a -> metadata.setAuth(stringList(a)));
        if (node.getBooleanMemberOrDefault("resultWrapped", false)) {
            metadata.setResultWrapped(true);
        }
        // awsQueryCompatible / protocolSettings are string maps; presence of awsQueryCompatible drives
        // the IR's hasAwsQueryCompatible flag (and the generated client's .hasAwsQueryCompatible(true)).
        node.getObjectMember("awsQueryCompatible").ifPresent(o -> metadata.setAwsQueryCompatible(stringMap(o)));
        node.getObjectMember("protocolSettings").ifPresent(o -> metadata.setProtocolSettings(stringMap(o)));
        return metadata;
    }

    private static Map<String, String> stringMap(ObjectNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        node.getStringMap().forEach((k, v) -> map.put(k, v.asStringNode().map(StringNode::getValue).orElse("")));
        return map;
    }

    // C2J context-param values are deserialized by the model loader as jackson-jr JrsValue trees, and
    // the endpoint-rules codegen casts them to JrsString/JrsBoolean/JrsArray. So restore them as
    // jackson-jr TreeNodes (NOT databind nodes) by parsing the value's JSON through the jr tree codec.
    private static final com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec JRS_CODEC =
            new com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec();
    private static final com.fasterxml.jackson.core.JsonFactory JSON_FACTORY =
            new com.fasterxml.jackson.core.JsonFactory();

    private static com.fasterxml.jackson.core.TreeNode treeNode(software.amazon.smithy.model.node.Node node) {
        try (com.fasterxml.jackson.core.JsonParser p =
                     JSON_FACTORY.createParser(software.amazon.smithy.model.node.Node.printJson(node))) {
            p.nextToken();
            return JRS_CODEC.readTree(p);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static List<String> stringList(ArrayNode array) {
        List<String> values = new ArrayList<>();
        for (Node n : array.getElements()) {
            n.asStringNode().ifPresent(s -> values.add(s.getValue()));
        }
        return values;
    }

    // Fallback when no preserved metadata trait exists: derive from standard traits + defaults.
    private ServiceMetadata metadataFromTraits() {
        ServiceMetadata metadata = new ServiceMetadata();
        String serviceId = service.getId().getName();
        metadata.setServiceId(serviceId);
        metadata.setApiVersion(service.getVersion());

        if (service.hasTrait(RestJson1Trait.class)) {
            metadata.setProtocol("rest-json");
        } else if (service.hasTrait(AwsJson1_0Trait.class)) {
            metadata.setProtocol("json");
            metadata.setJsonVersion("1.0");
        } else if (service.hasTrait(AwsJson1_1Trait.class)) {
            metadata.setProtocol("json");
            metadata.setJsonVersion("1.1");
        } else if (service.hasTrait(RestXmlTrait.class)) {
            metadata.setProtocol("rest-xml");
        } else if (service.hasTrait(AwsQueryTrait.class)) {
            metadata.setProtocol("query");
        }

        String lower = serviceId.toLowerCase(Locale.US);
        metadata.setEndpointPrefix(lower);
        metadata.setSigningName(lower);
        if ("json".equals(metadata.getProtocol())) {
            metadata.setTargetPrefix(serviceId);
        }
        service.getTrait(XmlNamespaceTrait.class).ifPresent(t -> metadata.setXmlNamespace(t.getUri()));
        return metadata;
    }

    // ----- operations -----

    private Operation buildOperation(OperationShape op) {
        Operation operation = new Operation();
        operation.setName(op.getId().getName());
        doc(op).ifPresent(operation::setDocumentation);   // operation-level @documentation

        // @http trait -> Http. RPC protocols carry no @http, so default to POST "/".
        Optional<HttpTrait> http = op.getTrait(HttpTrait.class);
        Http c2jHttp = new Http();
        if (http.isPresent()) {
            HttpTrait ht = http.get();
            c2jHttp.setMethod(ht.getMethod());
            c2jHttp.setRequestUri(ht.getUri().toString());
            c2jHttp.setResponseCode(Integer.toString(ht.getCode()));
        } else {
            c2jHttp.setMethod("POST");
            c2jHttp.setRequestUri("/");
        }
        operation.setHttp(c2jHttp);

        // input/output: smithy.api#Unit means "no shape" in C2J (leave null).
        op.getInput().filter(id -> !id.equals(UNIT)).ifPresent(id -> {
            Input input = new Input();
            input.setShape(id.getName());
            // rest-xml request wrapper name + xmlNamespace on the C2J input ref, preserved verbatim.
            op.findTrait(C2jToSmithyConverter.C2J_INPUT_META_TRAIT)
              .map(t -> t.toNode().expectObjectNode())
              .ifPresent(node -> {
                  node.getStringMember("locationName").ifPresent(n -> input.setLocationName(n.getValue()));
                  node.getObjectMember("xmlNamespace").ifPresent(xn -> {
                      XmlNamespace ns = new XmlNamespace();
                      xn.getStringMember("uri").ifPresent(u -> ns.setUri(u.getValue()));
                      xn.getStringMember("prefix").ifPresent(p -> ns.setPrefix(p.getValue()));
                      input.setXmlNamespace(ns);
                  });
              });
            operation.setInput(input);
        });
        op.getOutput().filter(id -> !id.equals(UNIT)).ifPresent(id -> {
            Output output = new Output();
            output.setShape(id.getName());
            // awsQuery output.resultWrapper, preserved verbatim by the forward converter.
            op.findTrait(C2jToSmithyConverter.C2J_RESULT_WRAPPER_TRAIT)
              .flatMap(t -> t.toNode().asStringNode())
              .ifPresent(n -> output.setResultWrapper(n.getValue()));
            operation.setOutput(output);
        });

        // httpChecksum block, preserved verbatim by the forward converter.
        op.findTrait(C2jToSmithyConverter.C2J_HTTP_CHECKSUM_TRAIT)
          .ifPresent(t -> operation.setHttpChecksum(httpChecksum(t.toNode().expectObjectNode())));

        // requestcompression block, preserved verbatim.
        op.findTrait(C2jToSmithyConverter.C2J_REQUEST_COMPRESSION_TRAIT).ifPresent(t -> {
            software.amazon.awssdk.codegen.compression.RequestCompression rc =
                    new software.amazon.awssdk.codegen.compression.RequestCompression();
            t.toNode().expectObjectNode().getArrayMember("encodings")
             .ifPresent(a -> rc.setEncodings(stringList(a)));
            operation.setRequestcompression(rc);
        });

        // endpointdiscovery block (+ required flag) and endpointoperation flag.
        op.findTrait(C2jToSmithyConverter.C2J_ENDPOINT_DISCOVERY_TRAIT).ifPresent(t -> {
            software.amazon.awssdk.codegen.model.intermediate.EndpointDiscovery ed =
                    new software.amazon.awssdk.codegen.model.intermediate.EndpointDiscovery();
            if (t.toNode().expectObjectNode().getBooleanMemberOrDefault("required", false)) {
                ed.setRequired(true);
            }
            operation.setEndpointdiscovery(ed);
        });
        if (op.findTrait(C2jToSmithyConverter.C2J_ENDPOINT_OPERATION_TRAIT).isPresent()) {
            operation.setEndpointoperation(true);
        }

        // endpoint.hostPrefix, authtype, unsignedPayload.
        op.findTrait(C2jToSmithyConverter.C2J_HOST_PREFIX_TRAIT)
          .flatMap(t -> t.toNode().asStringNode()).ifPresent(n -> {
              software.amazon.awssdk.codegen.model.service.EndpointTrait et =
                      new software.amazon.awssdk.codegen.model.service.EndpointTrait();
              et.setHostPrefix(n.getValue());
              operation.setEndpoint(et);
          });
        op.findTrait(C2jToSmithyConverter.C2J_AUTHTYPE_TRAIT)
          .flatMap(t -> t.toNode().asStringNode()).ifPresent(n -> operation.setAuthtype(n.getValue()));
        if (op.findTrait(C2jToSmithyConverter.C2J_UNSIGNED_PAYLOAD_TRAIT).isPresent()) {
            operation.setUnsignedPayload(true);
        }

        // staticContextParams / operationContextParams (arbitrary JSON values for endpoint rules).
        op.findTrait(C2jToSmithyConverter.C2J_STATIC_CONTEXT_PARAMS_TRAIT).ifPresent(t -> {
            Map<String, software.amazon.awssdk.codegen.model.service.StaticContextParam> m = new LinkedHashMap<>();
            t.toNode().expectObjectNode().getStringMap().forEach((name, node) -> {
                software.amazon.awssdk.codegen.model.service.StaticContextParam p =
                        new software.amazon.awssdk.codegen.model.service.StaticContextParam();
                // C2J shape is {"<name>": {"value": <x>}}; restore the inner "value" verbatim.
                node.expectObjectNode().getMember("value").ifPresent(v -> p.setValue(treeNode(v)));
                m.put(name, p);
            });
            operation.setStaticContextParams(m);
        });
        op.findTrait(C2jToSmithyConverter.C2J_OPERATION_CONTEXT_PARAMS_TRAIT).ifPresent(t -> {
            Map<String, software.amazon.awssdk.codegen.model.service.OperationContextParam> m = new LinkedHashMap<>();
            t.toNode().expectObjectNode().getStringMap().forEach((name, node) -> {
                software.amazon.awssdk.codegen.model.service.OperationContextParam p =
                        new software.amazon.awssdk.codegen.model.service.OperationContextParam();
                // C2J shape is {"<name>": {"path": <expr>}}; restore the inner "path" value verbatim.
                node.expectObjectNode().getMember("path").ifPresent(pathNode -> p.setPath(treeNode(pathNode)));
                m.put(name, p);
            });
            operation.setOperationContextParams(m);
        });

        // errors[] -> ErrorMap{shape}.
        if (!op.getErrors().isEmpty()) {
            List<ErrorMap> errors = new ArrayList<>();
            for (ShapeId err : op.getErrors()) {
                ErrorMap map = new ErrorMap();
                map.setShape(err.getName());
                errors.add(map);
            }
            operation.setErrors(errors);
        }

        return operation;
    }

    // Restore the C2J httpChecksum POJO from the preserved verbatim node.
    private static HttpChecksum httpChecksum(ObjectNode node) {
        HttpChecksum checksum = new HttpChecksum();
        node.getBooleanMember("requestChecksumRequired")
            .ifPresent(n -> checksum.setRequestChecksumRequired(n.getValue()));
        node.getStringMember("requestAlgorithmMember")
            .ifPresent(n -> checksum.setRequestAlgorithmMember(n.getValue()));
        node.getStringMember("requestValidationModeMember")
            .ifPresent(n -> checksum.setRequestValidationModeMember(n.getValue()));
        node.getArrayMember("responseAlgorithms")
            .ifPresent(a -> checksum.setResponseAlgorithms(stringList(a)));
        return checksum;
    }

    // ----- shapes -----

    private Shape convertShape(software.amazon.smithy.model.shapes.Shape shape) {
        if (shape instanceof StructureShape) {
            return structure((StructureShape) shape);
        }
        if (shape instanceof UnionShape) {
            return union((UnionShape) shape);
        }
        if (shape instanceof ListShape) {
            return list((ListShape) shape);
        }
        if (shape instanceof MapShape) {
            return map((MapShape) shape);
        }
        if (shape instanceof StringShape) {
            return string((StringShape) shape);
        }
        if (shape instanceof BooleanShape) {
            return simple("boolean");
        }
        if (shape instanceof BlobShape) {
            return simple("blob");
        }
        if (shape instanceof ByteShape) {
            return numeric("byte", shape);
        }
        if (shape instanceof ShortShape) {
            return numeric("short", shape);
        }
        if (shape instanceof IntegerShape) {
            return numeric("integer", shape);
        }
        if (shape instanceof LongShape) {
            return numeric("long", shape);
        }
        if (shape instanceof FloatShape) {
            return numeric("float", shape);
        }
        if (shape instanceof DoubleShape) {
            return numeric("double", shape);
        }
        if (shape instanceof BigIntegerShape) {
            return numeric("bigInteger", shape);
        }
        if (shape instanceof BigDecimalShape) {
            return numeric("bigDecimal", shape);
        }
        if (shape instanceof TimestampShape) {
            return simple("timestamp");
        }
        // Service/operation/member/resource shapes are handled elsewhere or not represented as C2J
        // shapes; skip them.
        return null;
    }

    private Shape simple(String type) {
        Shape shape = new Shape();
        shape.setType(type);
        return shape;
    }

    // Numeric shape: restore C2J min/max from the Smithy @range trait (inverse of the forward
    // converter's numeric(...) helper). C2J Shape models min/max as long; box is not modeled in C2J.
    private Shape numeric(String type, software.amazon.smithy.model.shapes.Shape s) {
        Shape shape = new Shape();
        shape.setType(type);
        s.getTrait(RangeTrait.class).ifPresent(rt -> {
            rt.getMin().ifPresent(min -> shape.setMin(min.longValue()));
            rt.getMax().ifPresent(max -> shape.setMax(max.longValue()));
        });
        return shape;
    }

    private Shape string(StringShape s) {
        Shape shape = new Shape();
        shape.setType("string");
        s.getTrait(EnumTrait.class).ifPresent(en -> shape.setEnumValues(en.getEnumDefinitionValues()));
        applyLength(shape, s);   // C2J min/max on a string <- @length
        return shape;
    }

    private Shape list(ListShape s) {
        Shape shape = new Shape();
        shape.setType("list");
        shape.setListMember(memberRef(s.getMember()));
        applyLength(shape, s);   // C2J min/max on a list <- @length
        return shape;
    }

    private Shape map(MapShape s) {
        Shape shape = new Shape();
        shape.setType("map");
        shape.setMapKeyType(memberRef(s.getKey()));
        shape.setMapValueType(memberRef(s.getValue()));
        applyLength(shape, s);   // C2J min/max on a map <- @length
        return shape;
    }

    // Restore C2J min/max (count/size bounds) from the Smithy @length trait.
    private static void applyLength(Shape shape, software.amazon.smithy.model.shapes.Shape s) {
        s.getTrait(LengthTrait.class).ifPresent(lt -> {
            lt.getMin().ifPresent(shape::setMin);
            lt.getMax().ifPresent(shape::setMax);
        });
    }

    private Shape structure(StructureShape s) {
        Shape shape = new Shape();
        shape.setType("structure");
        populateMembers(shape, s.getAllMembers());

        // @error -> exception=true. Restore the full C2J error block (code + httpStatusCode) from the
        // verbatim marker the forward converter carried; the @error trait alone loses those.
        s.getTrait(software.amazon.smithy.model.traits.ErrorTrait.class).ifPresent(errorTrait -> {
            shape.setException(true);
            ErrorTrait c2jError = new ErrorTrait();
            Optional<ObjectNode> errNode = s.findTrait(C2jToSmithyConverter.C2J_ERROR_TRAIT)
                                            .map(t -> t.toNode().expectObjectNode());
            boolean hasErrorBlock = false;
            if (errNode.isPresent()) {
                ObjectNode n = errNode.get();
                // The C2J "fault" flag (server fault -> generated 500) is carried on the marker.
                if (n.getBooleanMemberOrDefault("fault", false)) {
                    shape.setFault(true);
                }
                hasErrorBlock = n.getStringMember("code").isPresent()
                                || n.getNumberMember("httpStatusCode").isPresent();
                n.getStringMember("code").ifPresent(c -> c2jError.setCode(c.getValue()));
                n.getNumberMember("httpStatusCode")
                 .ifPresent(h -> c2jError.setHttpStatusCode(h.getValue().intValue()));
            } else {
                // Fallback (hand-authored Smithy, no marker): derive from @error + @httpError.
                c2jError.setCode(errorTrait.getValue());
                s.getTrait(HttpErrorTrait.class).ifPresent(he -> c2jError.setHttpStatusCode(he.getCode()));
                hasErrorBlock = true;
            }
            // Only attach a C2J error block if there actually was one (a bare fault has none).
            if (hasErrorBlock) {
                shape.setError(c2jError);
            }
        });

        return shape;
    }

    private Shape union(UnionShape s) {
        Shape shape = new Shape();
        shape.setType("structure");
        shape.setUnion(true);
        populateMembers(shape, s.getAllMembers());
        return shape;
    }

    private void populateMembers(Shape shape, Map<String, MemberShape> members) {
        Map<String, Member> c2jMembers = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Map.Entry<String, MemberShape> e : members.entrySet()) {
            String memberName = e.getKey();
            MemberShape m = e.getValue();
            Member member = new Member();
            member.setShape(m.getTarget().getName());
            applyMemberTraits(shape, member, memberName, m, required);
            c2jMembers.put(memberName, member);
        }
        shape.setMembers(c2jMembers);
        if (!required.isEmpty()) {
            shape.setRequired(required);
        }
    }

    private Member memberRef(MemberShape m) {
        Member member = new Member();
        member.setShape(m.getTarget().getName());
        // Restore a list/map element's C2J locationName (the XML element/entry name) from @xmlName /
        // @jsonName carried by the forward converter.
        Optional<String> wire = m.getTrait(XmlNameTrait.class).map(XmlNameTrait::getValue);
        if (!wire.isPresent()) {
            wire = m.getTrait(JsonNameTrait.class).map(JsonNameTrait::getValue);
        }
        wire.ifPresent(member::setLocationName);
        return member;
    }

    // Inverse of C2jToSmithyConverter#memberTraits: Smithy binding/wire-name traits -> C2J
    // location/locationName/timestampFormat, plus payload (on the shape) and required (on the shape).
    private void applyMemberTraits(Shape shape, Member member, String memberName, MemberShape m,
                                   List<String> required) {
        if (m.hasTrait(HttpLabelTrait.class)) {
            member.setLocation("uri");
            // Restore a divergent uri-label locationName carried on the marker trait.
            m.findTrait(C2jToSmithyConverter.C2J_LOCATION_NAME_TRAIT)
             .flatMap(t -> t.toNode().asStringNode())
             .ifPresent(n -> member.setLocationName(n.getValue()));
        }
        m.getTrait(HttpHeaderTrait.class).ifPresent(t -> {
            member.setLocation("header");
            member.setLocationName(t.getValue());
        });
        m.getTrait(HttpQueryTrait.class).ifPresent(t -> {
            member.setLocation("querystring");
            member.setLocationName(t.getValue());
        });
        if (m.hasTrait(HttpResponseCodeTrait.class)) {
            member.setLocation("statusCode");
        }
        m.getTrait(HttpPrefixHeadersTrait.class).ifPresent(t -> {
            member.setLocation("headers");
            member.setLocationName(t.getValue());
        });

        // @httpPayload -> the enclosing shape's payload, NOT a member field.
        if (m.hasTrait(HttpPayloadTrait.class)) {
            shape.setPayload(memberName);
        }

        // @jsonName / @xmlName on a body member -> Member.locationName. (Forward converter writes both;
        // either resolves back to the same wire name.)
        if (member.getLocation() == null) {
            Optional<JsonNameTrait> jsonName = m.getTrait(JsonNameTrait.class);
            if (jsonName.isPresent()) {
                member.setLocationName(jsonName.get().getValue());
            } else {
                m.getTrait(XmlNameTrait.class).ifPresent(t -> member.setLocationName(t.getValue()));
            }
        }

        m.getTrait(TimestampFormatTrait.class)
         .ifPresent(t -> member.setTimestampFormat(c2jTimestampFormat(t.getValue())));

        if (m.hasTrait(RequiredTrait.class)) {
            required.add(memberName);
        }

        // Member-level @documentation -> C2J member documentation.
        doc(m).ifPresent(member::setDocumentation);

        // Member-level @xmlFlattened -> C2J member flattened.
        if (m.hasTrait(software.amazon.smithy.model.traits.XmlFlattenedTrait.class)) {
            member.setFlattened(true);
        }

        // @idempotencyToken -> C2J member idempotencyToken.
        if (m.hasTrait(software.amazon.smithy.model.traits.IdempotencyTokenTrait.class)) {
            member.setIdempotencyToken(true);
        }

        // Member-level @sensitive -> C2J member sensitive.
        if (m.hasTrait(software.amazon.smithy.model.traits.SensitiveTrait.class)) {
            member.setSensitive(true);
        }

        // @xmlAttribute -> C2J member xmlAttribute.
        if (m.hasTrait(software.amazon.smithy.model.traits.XmlAttributeTrait.class)) {
            member.setXmlAttribute(true);
        }

        // @deprecated -> C2J member deprecated + deprecatedMessage.
        m.getTrait(software.amazon.smithy.model.traits.DeprecatedTrait.class).ifPresent(dt -> {
            member.setDeprecated(true);
            dt.getMessage().ifPresent(member::setDeprecatedMessage);
        });

        // @eventPayload / @eventHeader -> C2J member eventpayload / eventheader.
        if (m.hasTrait(software.amazon.smithy.model.traits.EventPayloadTrait.class)) {
            member.setEventpayload(true);
        }
        if (m.hasTrait(software.amazon.smithy.model.traits.EventHeaderTrait.class)) {
            member.setEventheader(true);
        }

        // @streaming (+ @requiresLength) -> C2J member streaming/requiresLength.
        if (m.hasTrait(software.amazon.smithy.model.traits.StreamingTrait.class)) {
            member.setStreaming(true);
        }
        if (m.hasTrait(software.amazon.smithy.model.traits.RequiresLengthTrait.class)) {
            member.setRequiresLength(true);
        }

        // @xmlNamespace -> C2J member xmlNamespace (uri[, prefix]).
        m.getTrait(XmlNamespaceTrait.class).ifPresent(xn -> {
            XmlNamespace ns = new XmlNamespace();
            ns.setUri(xn.getUri());
            xn.getPrefix().ifPresent(ns::setPrefix);
            member.setXmlNamespace(ns);
        });

        // member contextParam {name} -> C2J ContextParam.
        m.findTrait(C2jToSmithyConverter.C2J_CONTEXT_PARAM_TRAIT).ifPresent(t -> {
            software.amazon.awssdk.codegen.model.service.ContextParam cp =
                    new software.amazon.awssdk.codegen.model.service.ContextParam();
            t.toNode().expectObjectNode().getStringMember("name").ifPresent(n -> cp.setName(n.getValue()));
            member.setContextParam(cp);
        });

        // member queryName (ec2 wire name) -> C2J Member.queryName.
        m.findTrait(C2jToSmithyConverter.C2J_QUERY_NAME_TRAIT)
         .flatMap(t -> t.toNode().asStringNode()).ifPresent(n -> member.setQueryName(n.getValue()));
    }

    // Smithy @timestampFormat values -> C2J timestampFormat names (inverse of
    // C2jToSmithyConverter#smithyTimestampFormat).
    private static String c2jTimestampFormat(String smithy) {
        switch (smithy) {
            case "date-time":
                return "iso8601";
            case "http-date":
                return "rfc822";
            case "epoch-seconds":
                return "unixTimestamp";
            default:
                throw new IllegalArgumentException("Unknown Smithy timestampFormat: " + smithy);
        }
    }
}
