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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
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
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.XmlNameTrait;

/**
 * Converts an AWS C2J service model ({@code service-2.json}) into a canonical Smithy {@link Model}.
 *
 * <p>This is the build-time on-ramp for the Smithy-first SDK: C2J is treated purely as an input
 * format, and the {@link Model} produced here is the source of truth for downstream code
 * generation (which emits native Smithy schema literals). The conversion translates C2J's
 * protocol-coupled {@code location}/{@code locationName} into Smithy's protocol-specific binding
 * traits ({@code @httpLabel}/{@code @httpHeader}/{@code @httpQuery}/{@code @httpPayload}/
 * {@code @jsonName}/{@code @xmlName}), which is the decoupling the SDK needs ("less coupling to
 * C2J, not more").
 *
 * <p>Self-contained: depends only on smithy-model + jackson, never on the v2 codegen, so it can be
 * reused independently.
 */
public final class C2jToSmithyConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Custom trait id carrying the verbatim C2J {@code metadata} block on the service shape. */
    static final ShapeId C2J_METADATA_TRAIT = ShapeId.from("com.amazonaws.c2j#metadata");
    /** Custom trait carrying the verbatim C2J operation {@code httpChecksum} block. */
    static final ShapeId C2J_HTTP_CHECKSUM_TRAIT = ShapeId.from("com.amazonaws.c2j#httpChecksum");
    /** Custom trait carrying the C2J operation {@code output.resultWrapper} (awsQuery). */
    static final ShapeId C2J_RESULT_WRAPPER_TRAIT = ShapeId.from("com.amazonaws.c2j#resultWrapper");
    /** Custom trait carrying the C2J operation {@code input} {locationName, xmlNamespace} (rest-xml). */
    static final ShapeId C2J_INPUT_META_TRAIT = ShapeId.from("com.amazonaws.c2j#inputMeta");

    private final String namespace;
    private final JsonNode service;
    private final JsonNode shapes;
    private final JsonNode metadata;

    private C2jToSmithyConverter(String namespace, JsonNode service) {
        this.namespace = namespace;
        this.service = service;
        this.metadata = service.path("metadata");
        this.shapes = service.path("shapes");
    }

    /** Convert a C2J {@code service-2.json} file into a Smithy {@link Model}. */
    public static Model convert(Path serviceJson) {
        try (InputStream is = Files.newInputStream(serviceJson)) {
            return convert(MAPPER.readTree(is));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read C2J model: " + serviceJson, e);
        }
    }

    /** Convert a parsed C2J model tree into a Smithy {@link Model}. */
    public static Model convert(JsonNode c2j) {
        String serviceId = c2j.path("metadata").path("serviceId").asText("Service");
        String namespace = "com.amazonaws." + serviceId.toLowerCase(java.util.Locale.US);
        return new C2jToSmithyConverter(namespace, c2j).build();
    }

    private Model build() {
        Model.Builder model = Model.builder();

        // Convert every C2J shape to a Smithy shape (structures, unions, lists, maps, scalars).
        Iterator<Map.Entry<String, JsonNode>> it = shapes.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            for (Shape shape : convertShape(e.getKey(), e.getValue())) {
                model.addShape(shape);
            }
        }

        // Build each operation shape (with @http + input/output references).
        JsonNode operations = service.path("operations");
        Iterator<Map.Entry<String, JsonNode>> ops = operations.fields();
        while (ops.hasNext()) {
            Map.Entry<String, JsonNode> e = ops.next();
            model.addShape(buildOperation(e.getKey(), e.getValue()));
        }

        // Build the service referencing the operations.
        model.addShape(buildService());

        // Operations with no C2J input/output default to smithy.api#Unit. A model built via
        // Model.builder() (unlike the assembler) doesn't include the prelude, so add the Unit
        // shape explicitly so those operations resolve.
        model.addShape(StructureShape.builder()
                .id(ShapeId.from("smithy.api#Unit"))
                .addTrait(new software.amazon.smithy.model.traits.UnitTypeTrait())
                .build());
        return model.build();
    }

    private OperationShape buildOperation(String name, JsonNode c2j) {
        ShapeId unit = ShapeId.from("smithy.api#Unit");
        OperationShape.Builder op = OperationShape.builder().id(id(name));
        op.input(c2j.path("input").has("shape") ? id(c2j.path("input").path("shape").asText()) : unit);
        op.output(c2j.path("output").has("shape") ? id(c2j.path("output").path("shape").asText()) : unit);
        for (JsonNode err : c2j.path("errors")) {
            op.addError(id(err.path("shape").asText()));
        }
        // @http trait — ONLY for HTTP-binding protocols (restJson/restXml). RPC-style protocols
        // (awsJson, awsQuery, rpcv2Cbor) have no per-operation URI: C2J gives them all requestUri
        // "/", which would be a URI conflict and is semantically meaningless for those protocols.
        JsonNode http = c2j.path("http");
        if (isHttpBindingProtocol() && http.has("requestUri")) {
            HttpTrait.Builder ht = HttpTrait.builder()
                    .method(http.path("method").asText("POST"))
                    .uri(software.amazon.smithy.model.pattern.UriPattern.parse(
                            http.path("requestUri").asText("/")));
            if (http.has("responseCode")) {
                ht.code(http.path("responseCode").asInt(200));
            }
            op.addTrait(ht.build());
        }
        // Preserve operation-level C2J fields the protocol traits don't carry, verbatim, so the
        // inverse restores them: httpChecksum block and the awsQuery output.resultWrapper.
        if (c2j.has("httpChecksum")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_HTTP_CHECKSUM_TRAIT, Node.parse(c2j.get("httpChecksum").toString())));
        }
        JsonNode output = c2j.path("output");
        if (output.has("resultWrapper")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_RESULT_WRAPPER_TRAIT, Node.from(output.path("resultWrapper").asText())));
        }
        // Operation input wire metadata (rest-xml request wrapper name + xmlNamespace) that lives on
        // the C2J input ref, not the input shape itself.
        JsonNode input = c2j.path("input");
        if (input.has("locationName") || input.has("xmlNamespace")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_INPUT_META_TRAIT, Node.parse(input.toString())));
        }
        return op.build();
    }

    private boolean isHttpBindingProtocol() {
        String p = metadata.path("protocol").asText("");
        return p.equals("rest-json") || p.equals("rest-xml");
    }

    private ShapeId id(String name) {
        return ShapeId.fromParts(namespace, name);
    }

    // ----- shape conversion -----

    private List<Shape> convertShape(String name, JsonNode c2j) {
        String type = c2j.path("type").asText();
        ShapeId id = id(name);
        switch (type) {
            case "structure":
                return c2j.has("exception") && c2j.path("exception").asBoolean()
                        ? structure(id, c2j, true)
                        : structure(id, c2j, false);
            case "list":
                return list(id, c2j);
            case "map":
                return map(id, c2j);
            case "string":
                return string(id, c2j);
            case "boolean":
                return one(BooleanShape.builder().id(id).build());
            case "blob":
                return one(BlobShape.builder().id(id).build());
            case "byte":
                return one(numeric(ByteShape.builder().id(id), c2j));
            case "short":
                return one(numeric(ShortShape.builder().id(id), c2j));
            case "integer":
                return one(numeric(IntegerShape.builder().id(id), c2j));
            case "long":
                return one(numeric(LongShape.builder().id(id), c2j));
            case "float":
                return one(numeric(FloatShape.builder().id(id), c2j));
            case "double":
                return one(numeric(DoubleShape.builder().id(id), c2j));
            case "bigInteger":
                return one(numeric(BigIntegerShape.builder().id(id), c2j));
            case "bigDecimal":
                return one(numeric(BigDecimalShape.builder().id(id), c2j));
            case "timestamp":
                return one(TimestampShape.builder().id(id).build());
            default:
                throw new IllegalArgumentException("Unsupported C2J shape type '" + type + "' for " + name);
        }
    }

    private List<Shape> string(ShapeId id, JsonNode c2j) {
        StringShape.Builder b = StringShape.builder().id(id);
        // C2J enum -> Smithy @enum trait (kept as the older enum trait for broad compatibility).
        if (c2j.has("enum")) {
            EnumTrait.Builder en = EnumTrait.builder();
            for (JsonNode v : c2j.get("enum")) {
                en.addEnum(software.amazon.smithy.model.traits.EnumDefinition.builder()
                        .value(v.asText()).build());
            }
            b.addTrait(en.build());
        }
        lengthTrait(c2j).ifPresent(b::addTrait);   // C2J min/max on a string -> @length
        return one(b.build());
    }

    private List<Shape> list(ShapeId id, JsonNode c2j) {
        MemberShape member = MemberShape.builder()
                .id(id.withMember("member"))
                .target(targetId(c2j.path("member")))
                .build();
        ListShape.Builder b = ListShape.builder().id(id).member(member);
        lengthTrait(c2j).ifPresent(b::addTrait);   // C2J min/max on a list -> @length
        return one(b.build());
    }

    private List<Shape> map(ShapeId id, JsonNode c2j) {
        MemberShape key = MemberShape.builder().id(id.withMember("key"))
                .target(targetId(c2j.path("key"))).build();
        MemberShape value = MemberShape.builder().id(id.withMember("value"))
                .target(targetId(c2j.path("value"))).build();
        MapShape.Builder b = MapShape.builder().id(id).key(key).value(value);
        lengthTrait(c2j).ifPresent(b::addTrait);   // C2J min/max on a map -> @length
        return one(b.build());
    }

    // C2J min/max on string/list/map -> Smithy @length (count/size bounds).
    private static java.util.Optional<Trait> lengthTrait(JsonNode c2j) {
        if (!c2j.has("min") && !c2j.has("max")) {
            return java.util.Optional.empty();
        }
        software.amazon.smithy.model.traits.LengthTrait.Builder lt =
                software.amazon.smithy.model.traits.LengthTrait.builder();
        if (c2j.has("min")) {
            lt.min(c2j.get("min").asLong());
        }
        if (c2j.has("max")) {
            lt.max(c2j.get("max").asLong());
        }
        return java.util.Optional.of(lt.build());
    }

    // Apply C2J numeric metadata to a numeric shape builder: min/max -> @range, box -> @box.
    private static Shape numeric(AbstractShapeBuilder<?, ?> b, JsonNode c2j) {
        if (c2j.has("min") || c2j.has("max")) {
            software.amazon.smithy.model.traits.RangeTrait.Builder rt =
                    software.amazon.smithy.model.traits.RangeTrait.builder();
            if (c2j.has("min")) {
                rt.min(new java.math.BigDecimal(c2j.get("min").asText()));
            }
            if (c2j.has("max")) {
                rt.max(new java.math.BigDecimal(c2j.get("max").asText()));
            }
            b.addTrait(rt.build());
        }
        if (c2j.path("box").asBoolean(false)) {
            b.addTrait(new software.amazon.smithy.model.traits.BoxTrait());
        }
        return (Shape) b.build();
    }

    private List<Shape> structure(ShapeId id, JsonNode c2j, boolean isException) {
        // C2J represents both structures and unions as "structure"; a union has a top-level
        // "union": true. (Benchmark models don't, but handle it for generality.)
        boolean isUnion = c2j.path("union").asBoolean(false);
        AbstractShapeBuilder<?, ?> b = isUnion ? UnionShape.builder().id(id) : StructureShape.builder().id(id);

        String payloadMember = c2j.path("payload").asText(null);
        List<String> required = new ArrayList<>();
        if (c2j.has("required")) {
            for (JsonNode r : c2j.get("required")) {
                required.add(r.asText());
            }
        }

        JsonNode members = c2j.path("members");
        Iterator<Map.Entry<String, JsonNode>> it = members.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String memberName = e.getKey();
            JsonNode m = e.getValue();
            MemberShape.Builder mb = MemberShape.builder()
                    .id(id.withMember(memberName))
                    .target(targetId(m));
            for (Trait t : memberTraits(memberName, m, payloadMember, required.contains(memberName))) {
                mb.addTrait(t);
            }
            if (isUnion) {
                ((UnionShape.Builder) b).addMember(mb.build());
            } else {
                ((StructureShape.Builder) b).addMember(mb.build());
            }
        }
        Shape shape = b.build();
        if (isException) {
            // Re-add with @error trait (structure only).
            shape = ((StructureShape) shape).toBuilder()
                    .addTrait(new software.amazon.smithy.model.traits.ErrorTrait("client"))
                    .build();
        }
        return one(shape);
    }

    // Translate C2J member metadata (location/locationName/timestampFormat/required) into Smithy
    // binding + wire-name traits. This is the C2J->Smithy decoupling: protocol-specific C2J
    // "location" becomes protocol-specific Smithy traits.
    private List<Trait> memberTraits(String memberName, JsonNode m, String payloadMember, boolean required) {
        List<Trait> traits = new ArrayList<>();
        String location = m.path("location").asText(null);
        String locationName = m.path("locationName").asText(null);
        String wire = locationName != null ? locationName : memberName;

        if (location != null) {
            switch (location) {
                case "uri":
                    traits.add(new HttpLabelTrait());
                    break;
                case "header":
                    traits.add(new HttpHeaderTrait(wire));
                    break;
                case "querystring":
                    traits.add(new HttpQueryTrait(wire));
                    break;
                case "statusCode":
                    traits.add(new software.amazon.smithy.model.traits.HttpResponseCodeTrait());
                    break;
                case "headers":
                    traits.add(new software.amazon.smithy.model.traits.HttpPrefixHeadersTrait(wire));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported C2J location '" + location
                            + "' on member " + memberName);
            }
        }

        // The whole-body payload member (C2J "payload": "<member>").
        if (memberName.equals(payloadMember)) {
            traits.add(new HttpPayloadTrait());
        }

        // Body member wire name divergence -> jsonName + xmlName (each protocol reads only its own).
        boolean bodyMember = location == null;
        if (bodyMember && locationName != null && !locationName.equals(memberName)) {
            traits.add(new JsonNameTrait(locationName));
            traits.add(new XmlNameTrait(locationName));
        }

        if (m.has("timestampFormat")) {
            traits.add(new TimestampFormatTrait(smithyTimestampFormat(m.get("timestampFormat").asText())));
        }
        if (required) {
            traits.add(new RequiredTrait());
        }
        return traits;
    }

    private ShapeId targetId(JsonNode memberOrRef) {
        return id(memberOrRef.path("shape").asText());
    }

    // C2J timestamp format names -> Smithy @timestampFormat values.
    private static String smithyTimestampFormat(String c2j) {
        switch (c2j) {
            case "iso8601":
                return "date-time";
            case "rfc822":
                return "http-date";
            case "unixTimestamp":
                return "epoch-seconds";
            default:
                throw new IllegalArgumentException("Unknown C2J timestampFormat: " + c2j);
        }
    }

    // ----- service + operations -----

    private ServiceShape buildService() {
        String serviceName = metadata.path("serviceId").asText("Service");
        ServiceShape.Builder svc = ServiceShape.builder()
                .id(id(serviceName))
                .version(metadata.path("apiVersion").asText("1.0"));

        for (Trait t : protocolTraits()) {
            svc.addTrait(t);
        }

        // Preserve the FULL C2J metadata block verbatim as a custom node trait, so the inverse
        // (SmithyToServiceModel) can restore service identity fields the protocol traits don't carry
        // (endpointPrefix, signingName, targetPrefix, signatureVersion, uid, auth, protocols, ...).
        // Smithy treats unknown traits as opaque nodes, so this round-trips losslessly.
        svc.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(C2J_METADATA_TRAIT, metadataNode()));

        // The @awsQuery protocol's selector requires the service to carry @xmlNamespace.
        if (metadata.path("protocol").asText("").equals("query")) {
            String xmlns = metadata.path("xmlNamespace").asText(
                    "https://" + metadata.path("endpointPrefix").asText("service") + ".amazonaws.com/doc/"
                    + metadata.path("apiVersion").asText("2020-01-01") + "/");
            svc.addTrait(software.amazon.smithy.model.traits.XmlNamespaceTrait.builder().uri(xmlns).build());
        }

        JsonNode operations = service.path("operations");
        Iterator<Map.Entry<String, JsonNode>> it = operations.fields();
        while (it.hasNext()) {
            // The operations are added to the model separately; here we only wire the service to
            // reference them. We accumulate and add them via a side list.
            Map.Entry<String, JsonNode> e = it.next();
            svc.addOperation(id(e.getKey()));
        }
        // Operations themselves are added in buildOperations(); but ServiceShape only needs IDs.
        return svc.build();
    }

    // The verbatim C2J metadata object as a Smithy Node (Jackson JSON text -> Smithy Node).
    private Node metadataNode() {
        return Node.parse(metadata.toString());
    }

    private List<Shape> one(Shape shape) {
        List<Shape> l = new ArrayList<>(1);
        l.add(shape);
        return l;
    }

    private List<Trait> protocolTraits() {
        String protocol = metadata.path("protocol").asText("");
        List<Trait> traits = new ArrayList<>(1);
        switch (protocol) {
            case "rest-json":
                traits.add(restJson1());
                break;
            case "json":
                traits.add(awsJson(metadata.path("jsonVersion").asText("1.0")));
                break;
            case "rest-xml":
                traits.add(new software.amazon.smithy.aws.traits.protocols.RestXmlTrait.Provider()
                        .createTrait(software.amazon.smithy.aws.traits.protocols.RestXmlTrait.ID,
                                Node.objectNode()));
                break;
            case "query":
                traits.add(new software.amazon.smithy.aws.traits.protocols.AwsQueryTrait.Provider()
                        .createTrait(software.amazon.smithy.aws.traits.protocols.AwsQueryTrait.ID,
                                Node.objectNode()));
                break;
            default:
                // Leave protocol unset; downstream may set it explicitly. (rpcv2 cbor trait lives
                // in a separate artifact; handled by the caller when needed.)
                break;
        }
        return traits;
    }

    private Trait restJson1() {
        return software.amazon.smithy.aws.traits.protocols.RestJson1Trait.builder().build();
    }

    private Trait awsJson(String version) {
        if ("1.1".equals(version)) {
            return software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait.builder().build();
        }
        return software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait.builder().build();
    }
}
