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
import software.amazon.smithy.model.traits.StreamingTrait;
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
    // Headers Smithy reserves: they cannot carry @httpHeader (hard validation error) — computed by
    // the runtime, never modeled as bindings. The member is still emitted, just unbound.
    private static final java.util.Set<String> RESERVED_HEADERS =
            java.util.Set.of("connection", "content-length", "transfer-encoding");

    /** Custom trait id carrying the verbatim C2J {@code metadata} block on the service shape. */
    static final ShapeId C2J_METADATA_TRAIT = ShapeId.from("com.amazonaws.c2j#metadata");
    /** Custom trait carrying the verbatim top-level C2J {@code clientContextParams} block (service). */
    static final ShapeId C2J_CLIENT_CONTEXT_PARAMS_TRAIT = ShapeId.from("com.amazonaws.c2j#clientContextParams");
    /** Custom trait carrying the verbatim C2J operation {@code httpChecksum} block. */
    static final ShapeId C2J_HTTP_CHECKSUM_TRAIT = ShapeId.from("com.amazonaws.c2j#httpChecksum");
    /** Custom trait carrying the verbatim C2J operation {@code requestcompression} block. */
    static final ShapeId C2J_REQUEST_COMPRESSION_TRAIT = ShapeId.from("com.amazonaws.c2j#requestcompression");
    /** Carries the C2J operation {@code endpointdiscovery} block + {@code endpointoperation} flag. */
    static final ShapeId C2J_ENDPOINT_DISCOVERY_TRAIT = ShapeId.from("com.amazonaws.c2j#endpointdiscovery");
    static final ShapeId C2J_ENDPOINT_OPERATION_TRAIT = ShapeId.from("com.amazonaws.c2j#endpointoperation");
    /** Carries verbatim C2J operation {@code staticContextParams} / {@code operationContextParams}
     *  (arbitrary JSON values used by endpoint rules). */
    static final ShapeId C2J_STATIC_CONTEXT_PARAMS_TRAIT = ShapeId.from("com.amazonaws.c2j#staticContextParams");
    static final ShapeId C2J_OPERATION_CONTEXT_PARAMS_TRAIT = ShapeId.from("com.amazonaws.c2j#operationContextParams");
    /** Carries a member's C2J {@code contextParam} {name} (endpoint-rules binding). */
    static final ShapeId C2J_CONTEXT_PARAM_TRAIT = ShapeId.from("com.amazonaws.c2j#contextParam");
    /** Carries a member's C2J {@code queryName} (the ec2 protocol's wire name, distinct from locationName). */
    static final ShapeId C2J_QUERY_NAME_TRAIT = ShapeId.from("com.amazonaws.c2j#queryName");
    /** Marks a member with the C2J {@code jsonvalue} flag (base64 JSON value). */
    static final ShapeId C2J_JSON_VALUE_TRAIT = ShapeId.from("com.amazonaws.c2j#jsonvalue");
    /** Carries a structure's C2J {@code required} array verbatim, to preserve its declaration order. */
    static final ShapeId C2J_REQUIRED_TRAIT = ShapeId.from("com.amazonaws.c2j#required");
    /** Carries a C2J operation's {@code name} field when it differs from the operations-map key (e.g.
     *  cloudfront "AssociateAlias" key but name "AssociateAlias2020_05_31" — the wire action). */
    static final ShapeId C2J_OPERATION_NAME_TRAIT = ShapeId.from("com.amazonaws.c2j#operationName");
    /** Carries an operation's C2J {@code errors} array verbatim. The Smithy @http error set dedupes and
     *  reorders, but v2's IR keeps the exact declared sequence (some C2J models even list a shape twice,
     *  e.g. appsync CreateApiKey lists LimitExceededException both before and after the common errors). */
    static final ShapeId C2J_ERRORS_TRAIT = ShapeId.from("com.amazonaws.c2j#errors");
    /** Carries the verbatim C2J operation {@code http} block (method/requestUri/responseCode). The
     *  Smithy @http trait can't represent every C2J URI (e.g. hyphenated labels like {resource-arn}),
     *  so the IR's requestUri is restored from this marker, not from @http. */
    static final ShapeId C2J_HTTP_TRAIT = ShapeId.from("com.amazonaws.c2j#http");
    /** Carries C2J operation {@code endpoint.hostPrefix}, {@code authtype}, {@code unsignedPayload}. */
    static final ShapeId C2J_HOST_PREFIX_TRAIT = ShapeId.from("com.amazonaws.c2j#hostPrefix");
    static final ShapeId C2J_AUTHTYPE_TRAIT = ShapeId.from("com.amazonaws.c2j#authtype");
    static final ShapeId C2J_UNSIGNED_PAYLOAD_TRAIT = ShapeId.from("com.amazonaws.c2j#unsignedPayload");
    /** Custom trait carrying the C2J operation {@code output.resultWrapper} (awsQuery). */
    static final ShapeId C2J_RESULT_WRAPPER_TRAIT = ShapeId.from("com.amazonaws.c2j#resultWrapper");
    /** Carries the ref-level {@code documentation} on a C2J operation {@code output} (drives the IR's
     *  returnType javadoc; distinct from the output shape's own documentation). */
    static final ShapeId C2J_OUTPUT_DOC_TRAIT = ShapeId.from("com.amazonaws.c2j#outputDoc");
    /** Marks a C2J structure with the shape-level {@code wrapper:true} flag (query/rest-xml result
     *  wrapping). No native Smithy equivalent; v2's IR carries it through to the (un)marshaller. */
    static final ShapeId C2J_WRAPPER_TRAIT = ShapeId.from("com.amazonaws.c2j#wrapper");
    /** Marks a C2J structure with {@code synthetic:true} (a shape SDK-synthesized rather than modeled,
     *  e.g. some event-stream exception members). No Smithy analog; carried through to the IR. */
    static final ShapeId C2J_SYNTHETIC_TRAIT = ShapeId.from("com.amazonaws.c2j#synthetic");
    /** Custom trait carrying the C2J operation {@code input} {locationName, xmlNamespace} (rest-xml). */
    static final ShapeId C2J_INPUT_META_TRAIT = ShapeId.from("com.amazonaws.c2j#inputMeta");
    /** Marker traits carrying the C2J {@code eventstream}/{@code event} shape flags (the IR reads these
     *  booleans directly; no Smithy @streaming-union semantics are needed for a lossless round-trip). */
    static final ShapeId C2J_EVENTSTREAM_TRAIT = ShapeId.from("com.amazonaws.c2j#eventstream");
    static final ShapeId C2J_EVENT_TRAIT = ShapeId.from("com.amazonaws.c2j#event");
    /** Carries a member's C2J {@code locationName} when it diverges from the member name AND the binding
     *  trait can't express it (e.g. @httpLabel uri labels are matched by member name). */
    static final ShapeId C2J_LOCATION_NAME_TRAIT = ShapeId.from("com.amazonaws.c2j#locationName");
    /** Carries the verbatim C2J exception {@code error} block (code/httpStatusCode/senderFault) so the
     *  inverse restores the full C2J ErrorTrait (the @error trait alone loses code + status). */
    static final ShapeId C2J_ERROR_TRAIT = ShapeId.from("com.amazonaws.c2j#error");

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
        String namespace = "com.amazonaws." + smithyNamespaceSegment(serviceId);
        return new C2jToSmithyConverter(namespace, c2j).build();
    }

    // A C2J serviceId may contain spaces/punctuation (e.g. "DynamoDB Streams", "API Gateway"); a Smithy
    // namespace segment must be a valid identifier (letters/digits/underscore, not starting with a
    // digit). Strip non-alphanumerics and lowercase, matching how the SDK derives package names.
    private static String smithyNamespaceSegment(String serviceId) {
        String cleaned = serviceId.replaceAll("[^A-Za-z0-9]", "").toLowerCase(java.util.Locale.US);
        if (cleaned.isEmpty()) {
            cleaned = "service";
        } else if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "s" + cleaned;
        }
        return cleaned;
    }

    // The service shape name: strip non-alphanumerics (spaces/punctuation are illegal in a Smithy
    // identifier). The real serviceId round-trips via the preserved metadata trait, so this is lossless.
    private static String sanitizeShapeName(String serviceId) {
        String cleaned = serviceId.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isEmpty()) {
            cleaned = "Service";
        } else if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "S" + cleaned;
        }
        return cleaned;
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
        OperationShape.Builder op = OperationShape.builder().id(operationId(name));
        // Carry the operations-map key (= the C2J name field, which the IR uses as the marshaller
        // action, e.g. cloudfront "AssociateAlias2020_05_31") whenever it can't be recovered verbatim
        // from the Smithy op-shape id: either the name field diverges from the key, OR the id was
        // suffixed to avoid an operation/shape name collision.
        String c2jName = c2j.path("name").asText(name);
        if (!c2jName.equals(name) || shapes.has(name)) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_OPERATION_NAME_TRAIT, Node.from(c2jName)));
        }
        op.input(c2j.path("input").has("shape") ? id(c2j.path("input").path("shape").asText()) : unit);
        op.output(c2j.path("output").has("shape") ? id(c2j.path("output").path("shape").asText()) : unit);
        for (JsonNode err : c2j.path("errors")) {
            op.addError(id(err.path("shape").asText()));
        }
        // Carry the verbatim C2J errors[] so the IR's exception list round-trips in exact declared order
        // (Smithy's error set dedupes/reorders; v2's IR preserves duplicates and sequence).
        if (c2j.path("errors").isArray() && c2j.get("errors").size() > 0) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_ERRORS_TRAIT, Node.parse(c2j.get("errors").toString())));
        }
        // Carry the verbatim C2J http block so the IR's requestUri/method/responseCode round-trip
        // independently of the Smithy @http trait (which can't represent every C2J URI).
        JsonNode http = c2j.path("http");
        if (http.isObject() && http.size() > 0) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_HTTP_TRAIT, Node.parse(http.toString())));
        }
        // @http trait — ONLY for HTTP-binding protocols (restJson/restXml). RPC-style protocols
        // (awsJson, awsQuery, rpcv2Cbor) have no per-operation URI: C2J gives them all requestUri
        // "/", which would be a URI conflict and is semantically meaningless for those protocols.
        // Skip if the URI has labels Smithy's UriPattern can't parse (e.g. hyphenated {resource-arn});
        // the IR doesn't need @http (it uses the marker above) and the native path tolerates the gap.
        if (isHttpBindingProtocol() && http.has("requestUri")) {
            try {
                HttpTrait.Builder ht = HttpTrait.builder()
                        .method(http.path("method").asText("POST"))
                        .uri(software.amazon.smithy.model.pattern.UriPattern.parse(
                                http.path("requestUri").asText("/")));
                if (http.has("responseCode")) {
                    ht.code(http.path("responseCode").asInt(200));
                }
                op.addTrait(ht.build());
            } catch (RuntimeException e) {
                // Unparseable URI pattern (hyphenated label, etc.) — @http omitted; marker carries it.
            }
        }
        // Operation-level C2J documentation -> @documentation.
        if (c2j.hasNonNull("documentation")) {
            op.addTrait(new software.amazon.smithy.model.traits.DocumentationTrait(
                    c2j.get("documentation").asText()));
        }
        // Operation-level C2J deprecated -> @deprecated(message).
        if (c2j.path("deprecated").asBoolean(false)) {
            op.addTrait(deprecatedTrait(c2j));
        }
        // Preserve operation-level C2J fields the protocol traits don't carry, verbatim, so the
        // inverse restores them: httpChecksum block and the awsQuery output.resultWrapper.
        if (c2j.has("httpChecksum")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_HTTP_CHECKSUM_TRAIT, Node.parse(c2j.get("httpChecksum").toString())));
        }
        // Legacy boolean httpChecksumRequired (distinct from the httpChecksum block) -> @httpChecksumRequired.
        if (c2j.path("httpChecksumRequired").asBoolean(false)) {
            op.addTrait(new software.amazon.smithy.model.traits.HttpChecksumRequiredTrait());
        }
        if (c2j.has("requestcompression")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_REQUEST_COMPRESSION_TRAIT, Node.parse(c2j.get("requestcompression").toString())));
        }
        if (c2j.has("endpointdiscovery")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_ENDPOINT_DISCOVERY_TRAIT, Node.parse(c2j.get("endpointdiscovery").toString())));
        }
        if (c2j.path("endpointoperation").asBoolean(false)) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_ENDPOINT_OPERATION_TRAIT, Node.objectNode()));
        }
        if (c2j.path("staticContextParams").isObject() && c2j.get("staticContextParams").size() > 0) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_STATIC_CONTEXT_PARAMS_TRAIT, Node.parse(c2j.get("staticContextParams").toString())));
        }
        if (c2j.path("operationContextParams").isObject() && c2j.get("operationContextParams").size() > 0) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_OPERATION_CONTEXT_PARAMS_TRAIT,
                    Node.parse(c2j.get("operationContextParams").toString())));
        }
        if (c2j.path("endpoint").path("hostPrefix").isTextual()) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_HOST_PREFIX_TRAIT, Node.from(c2j.path("endpoint").path("hostPrefix").asText())));
        }
        if (c2j.path("authtype").isTextual()) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_AUTHTYPE_TRAIT, Node.from(c2j.path("authtype").asText())));
        }
        if (c2j.path("unsignedPayload").asBoolean(false)) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_UNSIGNED_PAYLOAD_TRAIT, Node.objectNode()));
        }
        JsonNode output = c2j.path("output");
        if (output.has("resultWrapper")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_RESULT_WRAPPER_TRAIT, Node.from(output.path("resultWrapper").asText())));
        }
        // ref-level documentation on the C2J output ref (distinct from the output shape's own doc;
        // drives the generated operation's returnType javadoc). Carried verbatim.
        if (output.hasNonNull("documentation")) {
            op.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_OUTPUT_DOC_TRAIT, Node.from(output.path("documentation").asText())));
        }
        // Operation input wire metadata (rest-xml request wrapper name + xmlNamespace + ref doc) that
        // lives on the C2J input ref, not the input shape itself.
        JsonNode input = c2j.path("input");
        if (input.has("locationName") || input.has("xmlNamespace") || input.hasNonNull("documentation")) {
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

    // C2J allows an operation and a shape to share a name (e.g. pinpoint CreateRecommenderConfiguration);
    // Smithy shape ids must be unique, so give a colliding operation a distinct id. The real C2J
    // operation name round-trips via C2J_OPERATION_NAME_TRAIT, and the adapter strips this suffix to
    // recover the operations-map key.
    static final String OP_ID_SUFFIX = "C2jOperation";

    private ShapeId operationId(String name) {
        return shapes.has(name) ? id(name + OP_ID_SUFFIX) : id(name);
    }

    // ----- shape conversion -----

    private List<Shape> convertShape(String name, JsonNode c2j) {
        List<Shape> shapes = convertShape0(name, c2j);
        // Apply C2J shape-level documentation to the produced top-level shape (the first element; any
        // nested member shapes carry their own docs via memberTraits). Smithy shapes are immutable, so
        // re-add via toBuilder(). The doc trait must survive into codegen so generated javadoc matches.
        if (!shapes.isEmpty()) {
            List<Trait> shapeTraits = new ArrayList<>(2);
            if (c2j.hasNonNull("documentation")) {
                shapeTraits.add(new software.amazon.smithy.model.traits.DocumentationTrait(
                        c2j.get("documentation").asText()));
            }
            // C2J shape-level flattened (list/map) -> @xmlFlattened, so the marshaller emits the
            // flattened wire form (and v2 codegen sets isFlattened(true)).
            if (c2j.path("flattened").asBoolean(false)) {
                shapeTraits.add(new software.amazon.smithy.model.traits.XmlFlattenedTrait());
            }
            // C2J event-stream flags -> marker traits (the IR reads shape.isEventstream()/isEvent()).
            if (c2j.path("eventstream").asBoolean(false)) {
                shapeTraits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                        C2J_EVENTSTREAM_TRAIT, Node.objectNode()));
            }
            if (c2j.path("event").asBoolean(false)) {
                shapeTraits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                        C2J_EVENT_TRAIT, Node.objectNode()));
            }
            // C2J shape-level sensitive -> @sensitive (v2 redacts the value in toString()).
            if (c2j.path("sensitive").asBoolean(false)) {
                shapeTraits.add(new software.amazon.smithy.model.traits.SensitiveTrait());
            }
            // Carry the C2J required[] array verbatim to preserve its DECLARATION ORDER (v2's IR keeps
            // it; @required per-member would come back in member-iteration order instead). Emitted even
            // when empty so an explicit required:[] round-trips as [] rather than null (e.g.
            // pinpointsmsvoice CallInstructionsMessageType).
            if (c2j.path("required").isArray()) {
                shapeTraits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                        C2J_REQUIRED_TRAIT, Node.parse(c2j.get("required").toString())));
            }
            // C2J shape-level deprecated -> @deprecated(message).
            if (c2j.path("deprecated").asBoolean(false)) {
                shapeTraits.add(deprecatedTrait(c2j));
            }
            // C2J shape-level streaming -> @streaming (+ @requiresLength).
            if (c2j.path("streaming").asBoolean(false)) {
                shapeTraits.add(new StreamingTrait());
                if (c2j.path("requiresLength").asBoolean(false)) {
                    shapeTraits.add(new software.amazon.smithy.model.traits.RequiresLengthTrait());
                }
            }
            // C2J shape-level xmlNamespace (uri[, prefix]) -> @xmlNamespace. v2 derives XmlAttributesTrait
            // (xmlns:xsi / xsi:type attributes) from a referenced shape's xmlNamespace + prefix.
            JsonNode shapeXmlns = c2j.path("xmlNamespace");
            if (shapeXmlns.hasNonNull("uri")) {
                software.amazon.smithy.model.traits.XmlNamespaceTrait.Builder xb =
                        software.amazon.smithy.model.traits.XmlNamespaceTrait.builder().uri(shapeXmlns.get("uri").asText());
                if (shapeXmlns.hasNonNull("prefix")) {
                    xb.prefix(shapeXmlns.get("prefix").asText());
                }
                shapeTraits.add(xb.build());
            }
            // C2J shape-level retryable -> @retryable(throttling) (drives v2's isRetryableException()).
            if (c2j.path("retryable").isObject()) {
                software.amazon.smithy.model.traits.RetryableTrait.Builder rb =
                        software.amazon.smithy.model.traits.RetryableTrait.builder();
                rb.throttling(c2j.path("retryable").path("throttling").asBoolean(false));
                shapeTraits.add(rb.build());
            }
            // C2J shape-level wrapper:true -> marker (query/rest-xml result wrapping; no Smithy analog).
            if (c2j.path("wrapper").asBoolean(false)) {
                shapeTraits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                        C2J_WRAPPER_TRAIT, Node.from(true)));
            }
            // C2J shape-level synthetic:true -> marker (no Smithy analog).
            if (c2j.path("synthetic").asBoolean(false)) {
                shapeTraits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                        C2J_SYNTHETIC_TRAIT, Node.from(true)));
            }
            if (!shapeTraits.isEmpty()) {
                AbstractShapeBuilder<?, ?> b = Shape.shapeToBuilder(shapes.get(0));
                shapeTraits.forEach(b::addTrait);
                shapes.set(0, (Shape) b.build());
            }
        }
        return shapes;
    }

    private List<Shape> convertShape0(String name, JsonNode c2j) {
        String type = c2j.path("type").asText();
        ShapeId id = id(name);
        switch (type) {
            case "structure":
                // C2J models the open "document" type as a memberless structure with document:true.
                // Smithy has a native document shape; v2's IR maps it back to the Document SDK type
                // (variableType software.amazon.awssdk.core.document.Document, marshallingType DOCUMENT).
                if (c2j.path("document").asBoolean(false)) {
                    return one(software.amazon.smithy.model.shapes.DocumentShape.builder().id(id).build());
                }
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
            case "timestamp": {
                TimestampShape.Builder b = TimestampShape.builder().id(id);
                // Shape-level C2J timestampFormat -> @timestampFormat (the IR resolves a member's
                // format from its target shape when the member itself doesn't specify one).
                if (c2j.has("timestampFormat")) {
                    b.addTrait(new TimestampFormatTrait(
                            smithyTimestampFormat(c2j.get("timestampFormat").asText())));
                }
                return one(b.build());
            }
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
        MemberShape.Builder member = MemberShape.builder()
                .id(id.withMember("member"))
                .target(targetId(c2j.path("member")));
        collectionMemberWireName(member, c2j.path("member"));   // list member element wire name
        ListShape.Builder b = ListShape.builder().id(id).member(member.build());
        lengthTrait(c2j).ifPresent(b::addTrait);   // C2J min/max on a list -> @length
        return one(b.build());
    }

    // A C2J list/map element ref carries its own metadata: locationName (the XML element/entry name,
    // e.g. a non-flattened list of "Change" elements) and documentation. Smithy carries the name via
    // @xmlName (+ @jsonName for symmetry) and docs via @documentation; the inverse restores
    // Member.locationName / Member.documentation so generated metadata + javadoc match.
    private static void collectionMemberWireName(MemberShape.Builder member, JsonNode ref) {
        String locationName = ref.path("locationName").asText(null);
        if (locationName != null) {
            member.addTrait(new XmlNameTrait(locationName));
            member.addTrait(new JsonNameTrait(locationName));
        }
        if (ref.hasNonNull("documentation")) {
            member.addTrait(new software.amazon.smithy.model.traits.DocumentationTrait(
                    ref.get("documentation").asText()));
        }
        // C2J jsonvalue on a list/map element (e.g. pricing PriceListJsonItems' member) -> marker.
        if (ref.path("jsonvalue").asBoolean(false)) {
            member.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_JSON_VALUE_TRAIT, Node.objectNode()));
        }
    }

    private List<Shape> map(ShapeId id, JsonNode c2j) {
        MemberShape.Builder key = MemberShape.builder().id(id.withMember("key"))
                .target(targetId(c2j.path("key")));
        collectionMemberWireName(key, c2j.path("key"));
        MemberShape.Builder value = MemberShape.builder().id(id.withMember("value"))
                .target(targetId(c2j.path("value")));
        collectionMemberWireName(value, c2j.path("value"));
        MapShape.Builder b = MapShape.builder().id(id).key(key.build()).value(value.build());
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
            // @error("server") for a C2J server fault (top-level "fault":true, e.g. InternalServerError —
            // drives the generated 500 status), else "client". The verbatim C2J error block is carried on
            // a marker so the inverse restores the full ErrorTrait (code + httpStatusCode); the marker
            // also records "fault" so Shape.fault round-trips even when there's no error block.
            JsonNode err = c2j.path("error");
            boolean serverFault = c2j.path("fault").asBoolean(false)
                                  || (err.isObject() && !err.path("senderFault").asBoolean(true));
            String errorType = serverFault ? "server" : "client";
            StructureShape.Builder eb = ((StructureShape) shape).toBuilder()
                    .addTrait(new software.amazon.smithy.model.traits.ErrorTrait(errorType));
            // Carry the C2J error block + fault flag verbatim (build a node even if "error" is absent).
            software.amazon.smithy.model.node.ObjectNode.Builder errMarker =
                    software.amazon.smithy.model.node.Node.objectNodeBuilder();
            if (err.isObject()) {
                errMarker.merge(Node.parse(err.toString()).expectObjectNode());
            }
            if (c2j.path("fault").asBoolean(false)) {
                errMarker.withMember("fault", true);
            }
            // Always attach the marker for a converted C2J exception (even when empty): its presence
            // tells the inverse this came from C2J, so it uses the marker (and leaves errorCode unset
            // when there's no C2J error block -> the IR defaults the code to the shape name) rather
            // than the hand-authored-Smithy fallback that would wrongly use the @error type as code.
            eb.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(C2J_ERROR_TRAIT, errMarker.build()));
            shape = eb.build();
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
                    // @httpLabel matches the URI placeholder by MEMBER name, so a divergent C2J
                    // locationName (e.g. member "Name" bound to "{LexiconName}") can't be expressed by
                    // the trait — carry it on a marker so the inverse restores Member.locationName.
                    if (locationName != null && !locationName.equals(memberName)) {
                        traits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                                C2J_LOCATION_NAME_TRAIT,
                                software.amazon.smithy.model.node.Node.from(locationName)));
                    }
                    break;
                case "header":
                    // C2J header binding -> @httpHeader. (v2's IR build, the primary consumer, uses
                    // Model.builder() and does not run Smithy's HTTP-binding validators, so reserved
                    // headers like Content-Length are kept bound and round-trip exactly as legacy v2
                    // generates them. The validating native-types path handles reserved headers via
                    // customization excludes — see customization.config shapeModifiers.)
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
        // Member-level C2J documentation -> @documentation on the member.
        if (m.hasNonNull("documentation")) {
            traits.add(new software.amazon.smithy.model.traits.DocumentationTrait(
                    m.get("documentation").asText()));
        }
        // Member-level C2J flattened -> @xmlFlattened on the member.
        if (m.path("flattened").asBoolean(false)) {
            traits.add(new software.amazon.smithy.model.traits.XmlFlattenedTrait());
        }
        // C2J idempotencyToken -> @idempotencyToken (v2 emits DefaultValueTrait.idempotencyToken()).
        if (m.path("idempotencyToken").asBoolean(false)) {
            traits.add(new software.amazon.smithy.model.traits.IdempotencyTokenTrait());
        }
        // Member-level C2J sensitive -> @sensitive.
        if (m.path("sensitive").asBoolean(false)) {
            traits.add(new software.amazon.smithy.model.traits.SensitiveTrait());
        }
        // C2J xmlAttribute -> @xmlAttribute (member serialized as an XML attribute, not an element).
        if (m.path("xmlAttribute").asBoolean(false)) {
            traits.add(new software.amazon.smithy.model.traits.XmlAttributeTrait());
        }
        // C2J member deprecated -> @deprecated(message).
        if (m.path("deprecated").asBoolean(false)) {
            traits.add(deprecatedTrait(m));
        }
        // C2J member contextParam {name} (endpoint-rules binding) -> marker, restored verbatim.
        if (m.path("contextParam").isObject()) {
            traits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_CONTEXT_PARAM_TRAIT, Node.parse(m.get("contextParam").toString())));
        }
        // C2J member queryName (ec2 protocol wire name) -> marker, restored to Member.queryName.
        if (m.path("queryName").isTextual()) {
            traits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_QUERY_NAME_TRAIT, Node.from(m.get("queryName").asText())));
        }
        // C2J member jsonvalue flag -> marker, restored to Member.jsonvalue.
        if (m.path("jsonvalue").asBoolean(false)) {
            traits.add(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_JSON_VALUE_TRAIT, Node.objectNode()));
        }
        // C2J event member payload/header flags -> @eventPayload / @eventHeader.
        if (m.path("eventpayload").asBoolean(false)) {
            traits.add(new software.amazon.smithy.model.traits.EventPayloadTrait());
        }
        if (m.path("eventheader").asBoolean(false)) {
            traits.add(new software.amazon.smithy.model.traits.EventHeaderTrait());
        }
        // C2J member streaming -> @streaming (+ @requiresLength).
        if (m.path("streaming").asBoolean(false)) {
            traits.add(new StreamingTrait());
            if (m.path("requiresLength").asBoolean(false)) {
                traits.add(new software.amazon.smithy.model.traits.RequiresLengthTrait());
            }
        }
        // C2J member xmlNamespace -> @xmlNamespace(uri[, prefix]). The rest-xml marshaller derives the
        // operation's root XML namespace from its members' xmlNamespace, so this must round-trip.
        JsonNode xmlns = m.path("xmlNamespace");
        if (xmlns.hasNonNull("uri")) {
            software.amazon.smithy.model.traits.XmlNamespaceTrait.Builder xb =
                    software.amazon.smithy.model.traits.XmlNamespaceTrait.builder().uri(xmlns.get("uri").asText());
            if (xmlns.hasNonNull("prefix")) {
                xb.prefix(xmlns.get("prefix").asText());
            }
            traits.add(xb.build());
        }
        return traits;
    }

    // Build a Smithy @deprecated trait carrying the C2J deprecatedMessage (if any).
    private static Trait deprecatedTrait(JsonNode c2j) {
        software.amazon.smithy.model.traits.DeprecatedTrait.Builder b =
                software.amazon.smithy.model.traits.DeprecatedTrait.builder();
        if (c2j.hasNonNull("deprecatedMessage")) {
            b.message(c2j.get("deprecatedMessage").asText());
        }
        return b.build();
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
        // The service SHAPE name must be a valid Smithy identifier (no spaces/punctuation). The true
        // C2J serviceId is preserved verbatim in the @com.amazonaws.c2j#metadata trait and restored
        // from there by SmithyToServiceModel, so sanitizing the shape name here is lossless.
        String serviceName = sanitizeShapeName(metadata.path("serviceId").asText("Service"));
        // Avoid a service/shape id collision: some serviceIds match a modeled shape name (e.g. the
        // Budgets service has a "Budgets" list shape). Smithy ids must be unique, and a clobbered list
        // shape would make members targeting it unresolvable. The adapter finds the service via
        // model.getServiceShapes() (not by name) and restores the real serviceId from the metadata
        // trait, so suffixing the internal shape id here is lossless.
        if (shapes.has(serviceName)) {
            serviceName = serviceName + "C2jService";
        }
        ServiceShape.Builder svc = ServiceShape.builder()
                .id(id(serviceName))
                .version(metadata.path("apiVersion").asText("1.0"));

        for (Trait t : protocolTraits()) {
            svc.addTrait(t);
        }

        // Service-level C2J documentation (top-level service.documentation) -> @documentation.
        if (service.hasNonNull("documentation")) {
            svc.addTrait(new software.amazon.smithy.model.traits.DocumentationTrait(
                    service.get("documentation").asText()));
        }

        // Preserve the FULL C2J metadata block verbatim as a custom node trait, so the inverse
        // (SmithyToServiceModel) can restore service identity fields the protocol traits don't carry
        // (endpointPrefix, signingName, targetPrefix, signatureVersion, uid, auth, protocols, ...).
        // Smithy treats unknown traits as opaque nodes, so this round-trips losslessly.
        svc.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(C2J_METADATA_TRAIT, metadataNode()));

        // Preserve the top-level clientContextParams block (drives endpoint client-context params,
        // S3ClientContextParams, *ResolveEndpointInterceptor, client builders).
        JsonNode ccp = service.path("clientContextParams");
        if (ccp.isObject() && ccp.size() > 0) {
            svc.addTrait(new software.amazon.smithy.model.traits.DynamicTrait(
                    C2J_CLIENT_CONTEXT_PARAMS_TRAIT, Node.parse(ccp.toString())));
        }

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
            svc.addOperation(operationId(e.getKey()));
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
