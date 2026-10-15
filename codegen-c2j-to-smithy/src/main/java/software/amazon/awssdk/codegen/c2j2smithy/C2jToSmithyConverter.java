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

package software.amazon.awssdk.codegen.c2j2smithy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.aws.traits.HttpChecksumTrait;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientDiscoveredEndpointTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryIdTrait;
import software.amazon.smithy.aws.traits.clientendpointdiscovery.ClientEndpointDiscoveryTrait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryCompatibleTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
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
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.ContextParamTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;
import software.amazon.smithy.rulesengine.traits.OperationContextParamsTrait;
import software.amazon.smithy.rulesengine.traits.StaticContextParamsTrait;

/**
 * Converts the C2J model files of a service into a Smithy {@link Model}.
 *
 * <p>C2J is treated as an input format, and the {@link Model} produced here is the source of truth for code
 * generation. Every piece of C2J information that has a standard Smithy or AWS trait is emitted as that trait:
 * protocols, service identity, auth, HTTP bindings and wire names, errors, endpoints and endpoint rules, endpoint
 * discovery, checksums, compression, event streams, IDL 2 enums, pagination and waiters. The service model, the
 * paginators, the waiters and the endpoint rule set and tests are all converted.
 *
 * <p>C2J information with no standard Smithy or AWS trait is carried on {@code com.amazonaws.c2j} marker traits
 * ({@link C2jMarkerTrait}), and each such property, together with everything that could not be converted, is recorded
 * in the {@link ConversionGapReport} of the result.
 *
 * <p>The converter depends only on smithy-model, smithy-aws-traits, smithy-protocol-traits, smithy-rules-engine,
 * smithy-waiters and Jackson, never on the SDK codegen module. It is a Java 8 port of {@code C2jToSmithyConverter} from
 * the {@code smithy-java-bridge} branch, extended with standard traits; deviations are recorded in
 * {@code docs/design/schema-serde/porting-deviations.md}.
 */
public final class C2jToSmithyConverter {
    /**
     * The suffix appended to an operation shape name that collides, ignoring case, with a shape name. C2J allows an
     * operation and a shape to share a name (for example pinpoint {@code CreateRecommenderConfiguration}); Smithy shape
     * IDs must be unique ignoring case. The real operation name is kept on {@link C2jMarkerTrait#OPERATION_NAME}.
     */
    public static final String OPERATION_ID_SUFFIX = "C2jOperation";

    /**
     * The suffix appended to the service shape name when it collides, ignoring case, with a shape or operation name
     * (for example the Budgets service and its {@code Budgets} list shape).
     */
    public static final String SERVICE_ID_SUFFIX = "C2jService";

    /**
     * The suffix appended to a C2J shape name that collides, ignoring case, with another C2J shape name (for example
     * SNS {@code String} and {@code string}). The original name is kept on {@link C2jMarkerTrait#SHAPE_NAME}.
     */
    public static final String SHAPE_ID_SUFFIX = "C2jShape";

    /**
     * The suffix of the copy of a shared string or blob target that {@code jsonvalue} members target, so that only
     * those members get {@code @mediaType("application/json")}.
     */
    public static final String JSON_VALUE_SUFFIX = "JsonValue";

    /**
     * The suffix of the copy of a blob that members with a member-level C2J {@code streaming} flag target, when other
     * members target the blob without the flag; only the copy gets {@code @streaming}.
     */
    public static final String STREAMING_SUFFIX = "Streaming";

    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");
    private static final String JSON_MEDIA_TYPE = "application/json";
    private static final Pattern IDENTIFIER = Pattern.compile("^(_+[A-Za-z0-9]|[A-Za-z])[A-Za-z0-9_]*$");
    private static final Pattern HOST_LABEL = Pattern.compile("\\{([^}]*)}");
    private static final Pattern CLOUD_FORMATION_NAME = Pattern.compile("^[A-Z][A-Za-z0-9]+$");
    private static final Pattern ARN_NAMESPACE = Pattern.compile("^[a-z0-9.\\-]{1,63}$");
    private static final Set<String> CLASS_SHAPE_TYPES = setOf("structure");
    private static final String STRING_ARRAY_PARAMETER_TYPE = "stringArray";

    // AWS endpoint rule set built-ins that smithy-aws-endpoints flags for review (RuleSetAwsBuiltInValidator).
    private static final Set<String> REVIEWED_BUILT_INS = setOf(
        "AWS::Auth::AccountId", "AWS::Auth::AccountIdEndpointMode", "AWS::Auth::CredentialScope");

    // Headers Smithy forbids binding with @httpHeader (HttpHeaderTraitValidator), lowercased.
    private static final Set<String> RESERVED_HEADERS = setOf(
        "authorization", "connection", "content-length", "expect", "host", "max-forwards", "proxy-authenticate",
        "server", "te", "trailer", "transfer-encoding", "upgrade", "user-agent", "www-authenticate", "x-forwarded-for");

    // Metadata properties that are always fully represented by standard traits once read.
    private static final Set<String> CONVERTED_METADATA = setOf(
        "apiVersion", "endpointPrefix", "signingName", "serviceFullName", "serviceId", "uid", "xmlNamespace",
        "protocol", "protocols", "jsonVersion");

    private static final Set<String> HTTP_CHECKSUM_PROPERTIES = setOf(
        "requestAlgorithmMember", "requestChecksumRequired", "requestValidationModeMember", "responseAlgorithms");

    private final String serviceName;
    private final String namespace;
    private final C2jInput input;
    private final JsonNode service;
    private final JsonNode shapes;
    private final JsonNode metadata;
    private final ConversionGapReport.Builder gaps;

    // C2J shape name -> Smithy shape name; differs only for case-insensitive collisions.
    private final Map<String, String> shapeNames = new HashMap<>();
    // Every Smithy shape name in use, lowercased (shapes and synthesized copies).
    private final Set<String> takenNames = new HashSet<>();
    // C2J target name -> Smithy name that jsonvalue references target (the shape itself, or its JSON-value copy).
    private final Map<String, String> jsonValueTargets = new HashMap<>();
    // Smithy name of a JSON-value copy -> C2J name it copies.
    private final Map<String, String> jsonValueCopies = new LinkedHashMap<>();
    // C2J blob name -> Smithy name that members with a member-level "streaming" flag target (the blob itself, or its
    // streaming copy).
    private final Map<String, String> streamingTargets = new HashMap<>();
    // Smithy name of a streaming copy -> C2J blob name it copies.
    private final Map<String, String> streamingCopies = new LinkedHashMap<>();
    // C2J blob names whose streaming target also gets @requiresLength.
    private final Set<String> streamingRequiresLength = new HashSet<>();
    // "<C2J structure>$<member>" of members bound by a host prefix label.
    private final Set<String> hostLabelMembers = new HashSet<>();
    // Operations (C2J keys) whose host prefix labels are all bindable.
    private final Set<String> hostPrefixOperations = new HashSet<>();
    // C2J input structure name -> the locationName and xmlNamespace of its operations' input reference, when they are
    // emitted on the structure as @xmlName and @xmlNamespace.
    private final Map<String, JsonNode> convertedInputReferences = new HashMap<>();
    // Metadata properties converted to standard traits.
    private final Set<String> convertedMetadata = new HashSet<>(CONVERTED_METADATA);

    private final List<Trait> protocolTraits;
    private final AuthConversion auth;
    private final String endpointDiscoveryOperation;
    private String serviceShapeName;

    private C2jToSmithyConverter(String namespace, C2jInput input) {
        this.serviceName = input.serviceName();
        this.namespace = namespace;
        this.input = input;
        this.service = input.serviceModel();
        this.metadata = service.path("metadata");
        this.shapes = service.path("shapes");
        this.gaps = ConversionGapReport.builder(serviceName);
        assignShapeNames();
        assignJsonValueTargets();
        assignStreamingTargets();
        findHostLabels();
        this.protocolTraits = protocolTraits();
        this.auth = AuthConversion.of(metadata, service.path("operations"), gaps);
        this.endpointDiscoveryOperation = endpointDiscoveryOperation();
    }

    /**
     * Convert the C2J models of one service into a Smithy model and its gap report.
     *
     * <p>The model namespace is {@code com.amazonaws.<serviceId>}, with the serviceId lowercased and stripped of
     * characters that are not letters or digits. The service shape is named after {@code metadata.targetPrefix} when it
     * is a valid identifier (the awsJson and rpcv2Cbor protocols put the service shape name on the wire), and after the
     * serviceId otherwise.
     *
     * @throws C2jConversionException if a C2J shape, operation or service property cannot be converted
     */
    public static ConversionResult convert(C2jInput models) {
        JsonNode c2j = models.serviceModel();
        String serviceId = c2j.path("metadata").path("serviceId").asText("Service");
        String namespace = "com.amazonaws." + smithyNamespaceSegment(serviceId);
        C2jToSmithyConverter converter = new C2jToSmithyConverter(namespace, models);
        Model model = converter.build();
        return new ConversionResult(model, converter.gaps.build());
    }

    // A C2J serviceId may contain spaces or punctuation (e.g. "DynamoDB Streams"). A Smithy namespace segment must be a
    // valid identifier, so strip non-alphanumerics and lowercase, matching how the SDK derives package names.
    private static String smithyNamespaceSegment(String serviceId) {
        String cleaned = serviceId.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.US);
        if (cleaned.isEmpty()) {
            cleaned = "service";
        } else if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "s" + cleaned;
        }
        return cleaned;
    }

    // Strip non-alphanumerics, which are illegal in a Smithy identifier.
    private static String sanitizeShapeName(String serviceId) {
        String cleaned = serviceId.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isEmpty()) {
            cleaned = "Service";
        } else if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "S" + cleaned;
        }
        return cleaned;
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private Model build() {
        Model.Builder model = Model.builder();
        collectInputReferences();

        // Convert every C2J shape (structures, unions, lists, maps, scalars), then the JSON-value copies.
        for (Map.Entry<String, JsonNode> e : shapes.properties()) {
            convertGuarded(e.getKey(), () -> model.addShapes(convertShape(e.getKey(), shapeId(e.getKey()), e.getValue())));
        }
        for (Map.Entry<String, String> e : jsonValueCopies.entrySet()) {
            convertGuarded(e.getValue(), () -> model.addShapes(convertCopy(e.getKey(), e.getValue())));
        }
        for (Map.Entry<String, String> e : streamingCopies.entrySet()) {
            convertGuarded(e.getValue(), () -> model.addShapes(convertCopy(e.getKey(), e.getValue())));
        }

        // Build each operation shape (with @http and input/output references).
        for (Map.Entry<String, JsonNode> e : service.path("operations").properties()) {
            convertGuarded(e.getKey(), () -> model.addShape(buildOperation(e.getKey(), e.getValue())));
        }

        Model converted;
        try {
            model.addShape(buildService());

            // Operations with no C2J input or output target smithy.api#Unit, and enum members target it too.
            // Model.builder() (unlike the assembler) does not include the prelude, so add Unit explicitly.
            model.addShape(StructureShape.builder().id(UNIT).addTrait(new UnitTypeTrait()).build());
            converted = model.build();
        } catch (C2jConversionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new C2jConversionException(serviceName, "metadata", String.valueOf(ex.getMessage()), ex);
        }
        try {
            return addOperationTraits(converted);
        } catch (C2jConversionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new C2jConversionException(serviceName, "paginators/waiters", String.valueOf(ex.getMessage()), ex);
        }
    }

    /**
     * Finds the operation input references whose rest-xml request root element name ({@code locationName}) or
     * {@code xmlNamespace} can go on the input structure as {@code @xmlName} and {@code @xmlNamespace}: all operations
     * that use the structure as input agree on them, and no other structure member targets the structure (where the
     * traits would change the member's serialization). The others keep them on the {@code inputMeta} marker.
     */
    private void collectInputReferences() {
        Map<String, JsonNode> candidates = new LinkedHashMap<>();
        Set<String> conflicting = new HashSet<>();
        for (Map.Entry<String, JsonNode> e : service.path("operations").properties()) {
            JsonNode inputRef = e.getValue().path("input");
            String shape = inputRef.path("shape").asText(null);
            if (shape == null) {
                continue;
            }
            com.fasterxml.jackson.databind.node.ObjectNode xml = JsonNodeFactory.instance.objectNode();
            if (inputRef.hasNonNull("locationName")) {
                xml.set("locationName", inputRef.get("locationName"));
            }
            if (inputRef.hasNonNull("xmlNamespace")) {
                xml.set("xmlNamespace", inputRef.get("xmlNamespace"));
            }
            JsonNode previous = candidates.putIfAbsent(shape, xml);
            if (previous != null && !previous.equals(xml)) {
                conflicting.add(shape);
            }
        }
        for (JsonNode shape : shapes) {
            for (JsonNode member : shape.path("members")) {
                conflicting.add(member.path("shape").asText());
            }
            for (String container : Arrays.asList("member", "key", "value")) {
                conflicting.add(shape.path(container).path("shape").asText());
            }
        }
        candidates.forEach((shape, xml) -> {
            if (xml.size() > 0 && !conflicting.contains(shape)) {
                convertedInputReferences.put(shape, xml);
            }
        });
    }

    private void convertGuarded(String c2jName, Runnable conversion) {
        try {
            conversion.run();
        } catch (C2jConversionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new C2jConversionException(serviceName, c2jName, String.valueOf(ex.getMessage()), ex);
        }
    }

    // @paginated and smithy.waiters#waitable need the converted input and output shapes, so they are added last.
    private Model addOperationTraits(Model model) {
        Map<ShapeId, List<Trait>> traits = new LinkedHashMap<>();
        PaginatorConversion.convert(input.paginatorsModel(), model, this::operationId, gaps)
                           .forEach((id, t) -> traits.computeIfAbsent(id, k -> new ArrayList<>()).addAll(t));
        WaiterConversion.convert(input.waitersModel(), this::operationId, gaps)
                        .forEach((id, t) -> traits.computeIfAbsent(id, k -> new ArrayList<>()).add(t));
        if (traits.isEmpty()) {
            return model;
        }
        Model.Builder builder = model.toBuilder();
        for (Map.Entry<ShapeId, List<Trait>> e : traits.entrySet()) {
            Optional<OperationShape> operation = model.getShape(e.getKey()).flatMap(Shape::asOperationShape);
            if (!operation.isPresent()) {
                gaps.record("waiter.operation(missing)", "A waiter names an operation the service model does not have. "
                                                         + "The waiter is not converted.");
                continue;
            }
            OperationShape.Builder op = operation.get().toBuilder();
            e.getValue().forEach(op::addTrait);
            builder.addShape(op.build());
        }
        return builder.build();
    }

    private DynamicTrait marker(C2jMarkerTrait marker, Node value) {
        gaps.record(marker);
        return new DynamicTrait(marker.traitId(), value);
    }

    private static Node toNode(JsonNode json) {
        return Node.parse(json.toString());
    }

    private static Trait provide(TraitService provider, ShapeId target, JsonNode value) {
        return provider.createTrait(target, toNode(value));
    }

    // ----- names -----

    // Smithy shape IDs must be unique ignoring case. In each group of C2J names that differ only in case, a structure
    // (which becomes a generated class) keeps its name; otherwise the name that sorts first keeps it. The others get
    // SHAPE_ID_SUFFIX (plus a number if that collides too).
    private void assignShapeNames() {
        Map<String, List<String>> groups = new TreeMap<>();
        for (Map.Entry<String, JsonNode> e : shapes.properties()) {
            groups.computeIfAbsent(e.getKey().toLowerCase(Locale.US), k -> new ArrayList<>()).add(e.getKey());
        }
        for (List<String> group : groups.values()) {
            if (group.size() == 1) {
                shapeNames.put(group.get(0), group.get(0));
                takenNames.add(group.get(0).toLowerCase(Locale.US));
            }
        }
        for (List<String> group : groups.values()) {
            if (group.size() == 1) {
                continue;
            }
            Collections.sort(group);
            String kept = group.get(0);
            for (String name : group) {
                if (CLASS_SHAPE_TYPES.contains(shapes.path(name).path("type").asText())) {
                    kept = name;
                    break;
                }
            }
            shapeNames.put(kept, kept);
            takenNames.add(kept.toLowerCase(Locale.US));
            for (String name : group) {
                if (!name.equals(kept)) {
                    shapeNames.put(name, uniqueName(name + SHAPE_ID_SUFFIX));
                }
            }
        }
    }

    private String uniqueName(String base) {
        String name = base;
        for (int i = 2; takenNames.contains(name.toLowerCase(Locale.US)); i++) {
            name = base + i;
        }
        takenNames.add(name.toLowerCase(Locale.US));
        return name;
    }

    private String smithyName(String c2jName) {
        String name = shapeNames.get(c2jName);
        return name != null ? name : c2jName;
    }

    private ShapeId shapeId(String c2jName) {
        return ShapeId.fromParts(namespace, smithyName(c2jName));
    }

    private ShapeId operationId(String name) {
        return ShapeId.fromParts(namespace, takenNames.contains(name.toLowerCase(Locale.US))
                                            ? name + OPERATION_ID_SUFFIX
                                            : name);
    }

    // @mediaType applies to the target shape, not the member. A string or blob targeted only by jsonvalue references
    // gets it directly; one also targeted by other references is copied as <Name>JsonValue, and the jsonvalue
    // references target the copy.
    private void assignJsonValueTargets() {
        Map<String, int[]> references = new TreeMap<>();
        for (Map.Entry<String, JsonNode> e : shapes.properties()) {
            JsonNode shape = e.getValue();
            for (Map.Entry<String, JsonNode> m : shape.path("members").properties()) {
                countReference(references, m.getValue());
            }
            for (String ref : Arrays.asList("member", "key", "value")) {
                if (shape.has(ref)) {
                    countReference(references, shape.get(ref));
                }
            }
        }
        for (Map.Entry<String, int[]> e : references.entrySet()) {
            int jsonValue = e.getValue()[1];
            if (jsonValue == 0) {
                continue;
            }
            String type = shapes.path(e.getKey()).path("type").asText();
            if (!"string".equals(type) && !"blob".equals(type)) {
                gaps.record("member.jsonvalue(non-string)", "jsonvalue on a member that targets neither a string nor "
                                                            + "a blob has no @mediaType equivalent and is dropped.");
            } else if (jsonValue == e.getValue()[0]) {
                jsonValueTargets.put(e.getKey(), smithyName(e.getKey()));
            } else {
                String copy = uniqueName(smithyName(e.getKey()) + JSON_VALUE_SUFFIX);
                jsonValueTargets.put(e.getKey(), copy);
                jsonValueCopies.put(copy, e.getKey());
            }
        }
    }

    /**
     * C2J can mark a structure member {@code streaming} instead of its blob target (S3's {@code Body}); codegen's C2J
     * route then treats the member as a streaming payload. Smithy allows {@code @streaming} only on the blob, so the
     * members target a streaming blob: the C2J blob itself when only such members reference it, otherwise a copy named
     * {@code <Name>Streaming}. A member-level {@code requiresLength} goes on that target too.
     */
    private void assignStreamingTargets() {
        Map<String, int[]> references = new TreeMap<>();
        for (JsonNode shape : shapes) {
            for (JsonNode member : shape.path("members")) {
                countStreamingReference(references, member);
            }
            for (String ref : Arrays.asList("member", "key", "value")) {
                countStreamingReference(references, shape.path(ref));
            }
        }
        for (Map.Entry<String, int[]> e : references.entrySet()) {
            int streaming = e.getValue()[1];
            if (streaming == 0) {
                continue;
            }
            JsonNode target = shapes.path(e.getKey());
            if (!"blob".equals(target.path("type").asText())) {
                gaps.record("member.streaming(non-blob)", "A member-level streaming flag on a member that does not "
                                                          + "target a blob has no Smithy equivalent and is dropped.");
            } else if (target.path("streaming").asBoolean(false)) {
                // Already a streaming blob; the member flag adds nothing.
                continue;
            } else if (streaming == e.getValue()[0]) {
                streamingTargets.put(e.getKey(), smithyName(e.getKey()));
            } else {
                String copy = uniqueName(smithyName(e.getKey()) + STREAMING_SUFFIX);
                streamingTargets.put(e.getKey(), copy);
                streamingCopies.put(copy, e.getKey());
            }
            if (e.getValue()[2] > 0) {
                streamingRequiresLength.add(e.getKey());
            }
        }
    }

    private static void countStreamingReference(Map<String, int[]> references, JsonNode ref) {
        if (!ref.path("shape").isTextual()) {
            return;
        }
        int[] counts = references.computeIfAbsent(ref.get("shape").asText(), k -> new int[3]);
        counts[0]++;
        if (ref.path("streaming").asBoolean(false)) {
            counts[1]++;
            if (ref.path("requiresLength").asBoolean(false)) {
                counts[2]++;
            }
        }
    }

    // Whether a structure member targets a streaming blob: a C2J streaming blob, or one made streaming for
    // member-level streaming flags (assignStreamingTargets).
    private boolean targetsStreamingBlob(JsonNode member) {
        String target = member.path("shape").asText();
        JsonNode shape = shapes.path(target);
        if (!"blob".equals(shape.path("type").asText())) {
            return false;
        }
        return shape.path("streaming").asBoolean(false)
               || member.path("streaming").asBoolean(false) && streamingTargets.containsKey(target);
    }

    private static void countReference(Map<String, int[]> references, JsonNode ref) {
        if (!ref.path("shape").isTextual()) {
            return;
        }
        int[] counts = references.computeIfAbsent(ref.get("shape").asText(), k -> new int[2]);
        counts[0]++;
        if (ref.path("jsonvalue").asBoolean(false)) {
            counts[1]++;
        }
    }

    // A host prefix label binds the input member of the same name, which Smithy requires to be required and to target
    // a string (@hostLabel). Operations whose labels all qualify get @endpoint; the others keep a marker.
    private void findHostLabels() {
        for (Map.Entry<String, JsonNode> e : service.path("operations").properties()) {
            JsonNode prefix = e.getValue().path("endpoint").path("hostPrefix");
            if (!prefix.isTextual()) {
                continue;
            }
            String inputName = e.getValue().path("input").path("shape").asText(null);
            JsonNode inputShape = inputName == null ? null : shapes.path(inputName);
            List<String> members = new ArrayList<>();
            boolean bindable = true;
            Matcher m = HOST_LABEL.matcher(prefix.asText());
            while (m.find()) {
                String label = m.group(1);
                boolean required = false;
                if (inputShape != null) {
                    for (JsonNode r : inputShape.path("required")) {
                        required |= label.equals(r.asText());
                    }
                }
                JsonNode member = inputShape == null ? null : inputShape.path("members").get(label);
                String target = member == null ? "" : shapes.path(member.path("shape").asText()).path("type").asText();
                if (!required || !"string".equals(target)) {
                    bindable = false;
                }
                members.add(inputName + "$" + label);
            }
            if (bindable) {
                hostPrefixOperations.add(e.getKey());
                hostLabelMembers.addAll(members);
            }
        }
    }

    // ----- operations -----

    private OperationShape buildOperation(String name, JsonNode c2j) {
        ShapeId operationId = operationId(name);
        OperationShape.Builder op = OperationShape.builder().id(operationId);
        // Carry the C2J name field (the wire action, e.g. cloudfront "AssociateAlias2020_05_31") whenever it can't be
        // recovered from the operation shape ID: the name diverges from the key, or the ID was suffixed to avoid an
        // operation/shape name collision.
        String c2jName = c2j.path("name").asText(name);
        if (!c2jName.equals(name) || !operationId.getName().equals(name)) {
            op.addTrait(marker(C2jMarkerTrait.OPERATION_NAME, Node.from(c2jName)));
        }
        JsonNode inputRef = c2j.path("input");
        op.input(inputRef.has("shape") ? shapeId(inputRef.path("shape").asText()) : UNIT);
        op.output(c2j.path("output").has("shape") ? shapeId(c2j.path("output").path("shape").asText()) : UNIT);
        for (JsonNode err : c2j.path("errors")) {
            op.addError(shapeId(err.path("shape").asText()));
        }
        // The verbatim errors[] keeps the declared order and duplicates; Smithy's error set dedupes and reorders.
        if (c2j.path("errors").isArray() && c2j.get("errors").size() > 0) {
            op.addTrait(marker(C2jMarkerTrait.ERRORS, toNode(c2j.get("errors"))));
        }
        addHttp(op, c2j);
        if (c2j.hasNonNull("documentation")) {
            op.addTrait(new DocumentationTrait(c2j.get("documentation").asText()));
        }
        if (c2j.path("deprecated").asBoolean(false)) {
            op.addTrait(deprecatedTrait(c2j));
        }
        if (c2j.path("httpChecksum").isObject()) {
            op.addTrait(httpChecksumTrait(c2j.get("httpChecksum")));
        }
        // The legacy boolean httpChecksumRequired, distinct from the httpChecksum block.
        if (c2j.path("httpChecksumRequired").asBoolean(false)) {
            op.addTrait(new HttpChecksumRequiredTrait());
        }
        if (c2j.path("requestcompression").isObject()) {
            JsonNode compression = c2j.get("requestcompression");
            recordUnconvertedProperties("operation.requestcompression", compression, setOf("encodings"));
            RequestCompressionTrait.Builder rc = RequestCompressionTrait.builder();
            compression.path("encodings").forEach(encoding -> rc.addEncoding(encoding.asText()));
            op.addTrait(rc.build());
        }
        addEndpointDiscovery(op, c2j);
        if (c2j.path("staticContextParams").isObject() && c2j.get("staticContextParams").size() > 0) {
            op.addTrait(provide(new StaticContextParamsTrait.Provider(), operationId, c2j.get("staticContextParams")));
        }
        if (c2j.path("operationContextParams").isObject() && c2j.get("operationContextParams").size() > 0) {
            op.addTrait(provide(new OperationContextParamsTrait.Provider(), operationId,
                                c2j.get("operationContextParams")));
        }
        JsonNode hostPrefix = c2j.path("endpoint").path("hostPrefix");
        if (hostPrefix.isTextual()) {
            if (hostPrefixOperations.contains(name)) {
                op.addTrait(EndpointTrait.builder().hostPrefix(hostPrefix.asText()).build());
            } else {
                op.addTrait(marker(C2jMarkerTrait.HOST_PREFIX, Node.from(hostPrefix.asText())));
            }
        }
        auth.operationTraits(name).forEach(op::addTrait);
        String authtype = auth.unmappedAuthtype(name);
        if (authtype != null) {
            op.addTrait(marker(C2jMarkerTrait.AUTHTYPE, Node.from(authtype)));
        }
        JsonNode output = c2j.path("output");
        // The awsQuery result wrapper is <OperationName>Result unless C2J says otherwise.
        if (output.has("resultWrapper") && !output.path("resultWrapper").asText().equals(c2jName + "Result")) {
            op.addTrait(marker(C2jMarkerTrait.RESULT_WRAPPER, Node.from(output.path("resultWrapper").asText())));
        }
        // Documentation on the output reference, distinct from the output shape's own documentation.
        if (output.hasNonNull("documentation")) {
            op.addTrait(marker(C2jMarkerTrait.OUTPUT_DOC, Node.from(output.path("documentation").asText())));
        }
        // The input reference's documentation, and its rest-xml request root element name and xmlNamespace when they
        // could not go on the input structure (see collectInputReferences).
        JsonNode convertedInput = convertedInputReferences.get(inputRef.path("shape").asText(""));
        com.fasterxml.jackson.databind.node.ObjectNode inputMeta = JsonNodeFactory.instance.objectNode();
        inputRef.properties().forEach(p -> {
            boolean onStructure = convertedInput != null && convertedInput.has(p.getKey());
            if (!p.getKey().equals("shape") && !onStructure) {
                inputMeta.set(p.getKey(), p.getValue());
            }
        });
        if (inputMeta.has("locationName") || inputMeta.has("xmlNamespace") || inputMeta.hasNonNull("documentation")) {
            op.addTrait(marker(C2jMarkerTrait.INPUT_META, toNode(inputMeta)));
        }
        if (c2j.path("authorizer").isTextual()) {
            op.addTrait(marker(C2jMarkerTrait.AUTHORIZER, Node.from(c2j.get("authorizer").asText())));
        }
        return op.build();
    }

    // @http only for HTTP-binding protocols. RPC protocols have no per-operation URI; C2J gives them all "/", which
    // would be a URI conflict, so their verbatim http block goes on a marker. URI labels are renamed to the bound
    // member names; a label that is bound to no member, or a URI Smithy cannot parse, fails the conversion.
    private void addHttp(OperationShape.Builder op, JsonNode c2j) {
        JsonNode http = c2j.path("http");
        if (isHttpBindingProtocol() && http.has("requestUri")) {
            String inputName = c2j.path("input").path("shape").asText(null);
            JsonNode inputShape = inputName == null ? shapes.path("") : shapes.path(inputName);
            String uri = UriLabels.normalize(http.path("requestUri").asText("/"), inputShape);
            HttpTrait.Builder ht = HttpTrait.builder()
                                            .method(http.path("method").asText("POST"))
                                            .uri(UriPattern.parse(uri));
            if (http.has("responseCode")) {
                ht.code(http.path("responseCode").asInt(200));
            }
            op.addTrait(ht.build());
        } else if (http.isObject() && http.size() > 0) {
            op.addTrait(marker(C2jMarkerTrait.HTTP, toNode(http)));
        }
    }

    private Trait httpChecksumTrait(JsonNode c2j) {
        recordUnconvertedProperties("operation.httpChecksum", c2j, HTTP_CHECKSUM_PROPERTIES);
        HttpChecksumTrait.Builder b = HttpChecksumTrait.builder();
        if (c2j.path("requestAlgorithmMember").isTextual()) {
            b.requestAlgorithmMember(c2j.get("requestAlgorithmMember").asText());
        }
        b.requestChecksumRequired(c2j.path("requestChecksumRequired").asBoolean(false));
        if (c2j.path("requestValidationModeMember").isTextual()) {
            b.requestValidationModeMember(c2j.get("requestValidationModeMember").asText());
        }
        c2j.path("responseAlgorithms").forEach(a -> b.addResponseAlgorithm(a.asText()));
        return b.build();
    }

    private void addEndpointDiscovery(OperationShape.Builder op, JsonNode c2j) {
        JsonNode discovery = c2j.path("endpointdiscovery");
        if (!discovery.isObject()) {
            return;
        }
        recordUnconvertedProperties("operation.endpointdiscovery", discovery, setOf("required"));
        if (endpointDiscoveryOperation == null) {
            gaps.record("operation.endpointdiscovery(no endpoint operation)",
                        "The service has no endpointoperation, so aws.api#clientDiscoveredEndpoint cannot be applied; "
                        + "the operation does not use endpoint discovery.");
            return;
        }
        op.addTrait(ClientDiscoveredEndpointTrait.builder().required(discovery.path("required").asBoolean(false))
                                                 .build());
    }

    private void recordUnconvertedProperties(String property, JsonNode c2j, Set<String> converted) {
        for (Map.Entry<String, JsonNode> e : c2j.properties()) {
            if (!converted.contains(e.getKey())) {
                gaps.record(property + "." + e.getKey(), "This C2J property has no standard trait and is dropped.");
            }
        }
    }

    private boolean isHttpBindingProtocol() {
        for (Trait t : protocolTraits) {
            if (t instanceof RestJson1Trait || t instanceof RestXmlTrait) {
                return true;
            }
        }
        return false;
    }

    // ----- shape conversion -----

    private List<Shape> convertShape(String c2jName, ShapeId id, JsonNode c2j) {
        List<Shape> converted = convertShape0(c2jName, id, c2j);
        // Apply shape-level C2J flags to the produced top-level shape. Smithy shapes are immutable, so re-add the
        // traits via a builder.
        List<Trait> shapeTraits = new ArrayList<>(2);
        if (c2j.hasNonNull("documentation")) {
            shapeTraits.add(new DocumentationTrait(c2j.get("documentation").asText()));
        }
        // Shape-level flattened is not emitted on the list or map shape, where @xmlFlattened is invalid. Every
        // structure member that targets the shape gets @xmlFlattened instead (see memberTraits), which is how the SDK
        // reads it: a member is flattened if it or its target is.
        if (c2j.path("sensitive").asBoolean(false)) {
            shapeTraits.add(new SensitiveTrait());
        }
        // The verbatim required[] keeps declaration order; emitted even when empty so an explicit [] is kept.
        if (c2j.path("required").isArray()) {
            shapeTraits.add(marker(C2jMarkerTrait.REQUIRED, toNode(c2j.get("required"))));
        }
        if (c2j.path("deprecated").asBoolean(false)) {
            shapeTraits.add(deprecatedTrait(c2j));
        }
        if (c2j.path("streaming").asBoolean(false) || c2j.path("eventstream").asBoolean(false)) {
            shapeTraits.add(new StreamingTrait());
            if (c2j.path("requiresLength").asBoolean(false)) {
                shapeTraits.add(new RequiresLengthTrait());
            }
        }
        Optional<Trait> shapeXmlNamespace = xmlNamespaceTrait(c2j.path("xmlNamespace"));
        shapeXmlNamespace.ifPresent(shapeTraits::add);
        // The rest-xml request root element name and namespace, from the operation input reference, go on the input
        // structure (see convertedInputReferences).
        JsonNode inputReference = convertedInputReferences.get(c2jName);
        if (inputReference != null) {
            if (!shapeXmlNamespace.isPresent()) {
                xmlNamespaceTrait(inputReference.path("xmlNamespace")).ifPresent(shapeTraits::add);
            }
            String rootName = inputReference.path("locationName").asText(null);
            if (rootName != null && !rootName.equals(id.getName())) {
                shapeTraits.add(new XmlNameTrait(rootName));
            }
        }
        // Shape-level retryable -> @retryable(throttling).
        if (c2j.path("retryable").isObject()) {
            shapeTraits.add(RetryableTrait.builder()
                                          .throttling(c2j.path("retryable").path("throttling").asBoolean(false))
                                          .build());
        }
        if (c2j.path("wrapper").asBoolean(false)) {
            shapeTraits.add(marker(C2jMarkerTrait.WRAPPER, Node.from(true)));
        }
        if (c2j.path("synthetic").asBoolean(false)) {
            shapeTraits.add(marker(C2jMarkerTrait.SYNTHETIC, Node.from(true)));
        }
        if (id.getName().equals(smithyName(c2jName)) && !id.getName().equals(c2jName)) {
            shapeTraits.add(marker(C2jMarkerTrait.SHAPE_NAME, Node.from(c2jName)));
        }
        if (id.getName().equals(jsonValueTargets.get(c2jName))) {
            shapeTraits.add(new MediaTypeTrait(JSON_MEDIA_TYPE));
        }
        if (id.getName().equals(streamingTargets.get(c2jName))) {
            shapeTraits.add(new StreamingTrait());
            if (streamingRequiresLength.contains(c2jName)) {
                shapeTraits.add(new RequiresLengthTrait());
            }
        }
        if (!shapeTraits.isEmpty()) {
            AbstractShapeBuilder<?, ?> b = Shape.shapeToBuilder(converted.get(0));
            shapeTraits.forEach(b::addTrait);
            converted.set(0, (Shape) b.build());
        }
        return converted;
    }

    // A JSON-value or streaming copy of a C2J shape, with the C2J name on a shapeName marker.
    private List<Shape> convertCopy(String copyName, String c2jName) {
        List<Shape> copy = convertShape(c2jName, ShapeId.fromParts(namespace, copyName), shapes.get(c2jName));
        AbstractShapeBuilder<?, ?> b = Shape.shapeToBuilder(copy.get(0));
        b.addTrait(marker(C2jMarkerTrait.SHAPE_NAME, Node.from(c2jName)));
        copy.set(0, (Shape) b.build());
        return copy;
    }

    private List<Shape> convertShape0(String c2jName, ShapeId id, JsonNode c2j) {
        String type = c2j.path("type").asText();
        switch (type) {
            case "structure":
                // C2J models the open document type as a memberless structure with document:true.
                if (c2j.path("document").asBoolean(false)) {
                    return one(DocumentShape.builder().id(id).build());
                }
                return structure(c2jName, id, c2j);
            case "list":
                return list(id, c2j);
            case "map":
                return map(id, c2j);
            case "string":
                return string(c2jName, id, c2j);
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
            // codegen's C2J loader knows only the lowercase spellings (TypeUtils); the camel-case ones are the fork's.
            case "bigInteger":
            case "biginteger":
                return one(numeric(BigIntegerShape.builder().id(id), c2j));
            case "bigDecimal":
            case "bigdecimal":
                return one(numeric(BigDecimalShape.builder().id(id), c2j));
            case "timestamp": {
                TimestampShape.Builder b = TimestampShape.builder().id(id);
                // Shape-level timestampFormat; a member without its own format resolves it from its target.
                if (c2j.has("timestampFormat")) {
                    b.addTrait(new TimestampFormatTrait(smithyTimestampFormat(c2jName,
                                                                              c2j.get("timestampFormat").asText())));
                }
                return one(b.build());
            }
            default:
                throw new C2jConversionException(serviceName, c2jName, "Unsupported C2J shape type '" + type + "'");
        }
    }

    // C2J enum -> an IDL 2 enum shape. Member names are synthesized from the values; the SDK names enum constants from
    // the values, so only validity matters. Values Smithy cannot hold (an empty string) keep the legacy @enum trait.
    private List<Shape> string(String c2jName, ShapeId id, JsonNode c2j) {
        AbstractShapeBuilder<?, ?> b;
        Set<String> values = new LinkedHashSet<>();
        c2j.path("enum").forEach(v -> values.add(v.asText()));
        if (!values.isEmpty()) {
            if (values.contains("")) {
                gaps.record("shape.enum(empty value)", "A C2J enum with an empty value cannot be an IDL 2 enum shape; "
                                                       + "it is a string with the legacy @enum trait.");
                EnumTrait.Builder en = EnumTrait.builder();
                values.forEach(v -> en.addEnum(EnumDefinition.builder().value(v).build()));
                b = StringShape.builder().id(id).addTrait(en.build());
            } else {
                EnumShape.Builder en = EnumShape.builder().id(id);
                Set<String> memberNames = new HashSet<>();
                int index = 0;
                for (String value : values) {
                    en.addMember(MemberShape.builder()
                                            .id(id.withMember(enumMemberName(value, index++, memberNames)))
                                            .target(UNIT)
                                            .addTrait(EnumValueTrait.builder().stringValue(value).build())
                                            .build());
                }
                b = en;
            }
        } else {
            b = StringShape.builder().id(id);
        }
        lengthTrait(c2j).ifPresent(b::addTrait);
        if (c2j.path("pattern").isTextual()) {
            try {
                b.addTrait(new PatternTrait(c2j.get("pattern").asText()));
            } catch (RuntimeException e) {
                gaps.record("shape.pattern(invalid)", "The C2J pattern is not a valid regular expression and is "
                                                      + "dropped.");
            }
        }
        return one((Shape) b.build());
    }

    // An enum member name from its value: characters outside [A-Za-z0-9_] become '_', a leading digit gets a '_'
    // prefix, a value with no letters or digits becomes VALUE_<index>, and names unique ignoring case get a numeric
    // suffix.
    private static String enumMemberName(String value, int index, Set<String> taken) {
        String name = value.replaceAll("[^A-Za-z0-9_]", "_");
        if (!IDENTIFIER.matcher(name).matches()) {
            name = "_" + name;
        }
        if (!IDENTIFIER.matcher(name).matches()) {
            name = "VALUE_" + index;
        }
        String unique = name;
        for (int i = 2; !taken.add(unique.toLowerCase(Locale.US)); i++) {
            unique = name + "_" + i;
        }
        return unique;
    }

    private List<Shape> list(ShapeId id, JsonNode c2j) {
        MemberShape.Builder member = MemberShape.builder()
                                                .id(id.withMember("member"))
                                                .target(targetId(c2j.path("member")));
        collectionMemberWireName(member, c2j.path("member"));
        ListShape.Builder b = ListShape.builder().id(id).member(member.build());
        lengthTrait(c2j).ifPresent(b::addTrait);
        return one(b.build());
    }

    // A list or map element reference carries its own locationName (the XML element or entry name) and documentation.
    // Only @xmlName is emitted: @jsonName is invalid on list and map members, and JSON protocols never name elements.
    private static void collectionMemberWireName(MemberShape.Builder member, JsonNode ref) {
        String locationName = ref.path("locationName").asText(null);
        if (locationName != null) {
            member.addTrait(new XmlNameTrait(locationName));
        }
        if (ref.hasNonNull("documentation")) {
            member.addTrait(new DocumentationTrait(ref.get("documentation").asText()));
        }
    }

    private List<Shape> map(ShapeId id, JsonNode c2j) {
        MemberShape.Builder key = MemberShape.builder().id(id.withMember("key")).target(targetId(c2j.path("key")));
        collectionMemberWireName(key, c2j.path("key"));
        MemberShape.Builder value = MemberShape.builder().id(id.withMember("value")).target(targetId(c2j.path("value")));
        collectionMemberWireName(value, c2j.path("value"));
        MapShape.Builder b = MapShape.builder().id(id).key(key.build()).value(value.build());
        lengthTrait(c2j).ifPresent(b::addTrait);
        return one(b.build());
    }

    // C2J min/max on a string, list or map -> @length.
    private static Optional<Trait> lengthTrait(JsonNode c2j) {
        if (!c2j.has("min") && !c2j.has("max")) {
            return Optional.empty();
        }
        LengthTrait.Builder lt = LengthTrait.builder();
        if (c2j.has("min")) {
            lt.min(c2j.get("min").asLong());
        }
        if (c2j.has("max")) {
            lt.max(c2j.get("max").asLong());
        }
        return Optional.of(lt.build());
    }

    // C2J numeric min/max -> @range. C2J box is dropped: in IDL 2 a member is optional unless it has @required or
    // @default, and the converter emits no @default.
    private static Shape numeric(AbstractShapeBuilder<?, ?> b, JsonNode c2j) {
        if (c2j.has("min") || c2j.has("max")) {
            RangeTrait.Builder rt = RangeTrait.builder();
            if (c2j.has("min")) {
                rt.min(new BigDecimal(c2j.get("min").asText()));
            }
            if (c2j.has("max")) {
                rt.max(new BigDecimal(c2j.get("max").asText()));
            }
            b.addTrait(rt.build());
        }
        return (Shape) b.build();
    }

    private List<Shape> structure(String c2jName, ShapeId id, JsonNode c2j) {
        // C2J represents structures and unions as "structure"; a union has "union": true, and an event stream
        // container ("eventstream": true) is a @streaming union.
        boolean isUnion = c2j.path("union").asBoolean(false) || c2j.path("eventstream").asBoolean(false);
        AbstractShapeBuilder<?, ?> b = isUnion ? UnionShape.builder().id(id) : StructureShape.builder().id(id);

        String payloadMember = c2j.path("payload").asText(null);
        Set<String> required = new HashSet<>();
        for (JsonNode r : c2j.path("required")) {
            required.add(r.asText());
        }

        for (Map.Entry<String, JsonNode> e : c2j.path("members").properties()) {
            String memberName = e.getKey();
            JsonNode m = e.getValue();
            MemberShape.Builder mb = MemberShape.builder()
                                                .id(id.withMember(memberName))
                                                .target(targetId(m));
            String c2jMemberName = c2jName + "$" + memberName;
            // Union members cannot be required (or client-optional).
            boolean isRequired = !isUnion && required.contains(memberName);
            for (Trait t : memberTraits(c2jMemberName, memberName, m, payloadMember, isRequired)) {
                mb.addTrait(t);
            }
            // Smithy requires a structure member that targets a streaming blob to be @required or to have a @default.
            // C2J streaming members are optional; the published AWS models give them an empty default (S3's
            // `Body: StreamingBlob = ""`).
            if (!isUnion && !isRequired && targetsStreamingBlob(m)) {
                mb.addTrait(new DefaultTrait(Node.from("")));
            }
            if (isUnion) {
                ((UnionShape.Builder) b).addMember(mb.build());
            } else {
                ((StructureShape.Builder) b).addMember(mb.build());
            }
        }
        if (c2j.path("exception").asBoolean(false)) {
            errorTraits(c2jName, id, c2j).forEach(b::addTrait);
        }
        return one((Shape) b.build());
    }

    // A server fault: top-level "fault": true; else the error block's senderFault (false means server); else an HTTP
    // status code of 500 or more. Anything else is a client fault.
    private static boolean isServerFault(JsonNode c2j) {
        if (c2j.path("fault").asBoolean(false)) {
            return true;
        }
        JsonNode err = c2j.path("error");
        if (err.path("senderFault").isBoolean()) {
            return !err.get("senderFault").asBoolean();
        }
        return err.path("httpStatusCode").asInt(400) >= 500;
    }

    // @error("server"|"client") (see isServerFault); @httpError for the HTTP status code; aws.protocols#awsQueryError
    // when the wire error code is not the shape name.
    private static List<Trait> errorTraits(String c2jName, ShapeId id, JsonNode c2j) {
        JsonNode err = c2j.path("error");
        boolean serverFault = isServerFault(c2j);
        List<Trait> traits = new ArrayList<>(3);
        traits.add(new ErrorTrait(serverFault ? "server" : "client"));
        boolean hasStatus = err.path("httpStatusCode").isIntegralNumber();
        if (hasStatus) {
            traits.add(new HttpErrorTrait(err.get("httpStatusCode").asInt()));
        }
        String code = err.path("code").asText(c2jName);
        if (!code.equals(id.getName())) {
            traits.add(AwsQueryErrorTrait.builder()
                                         .code(code)
                                         .httpResponseCode(hasStatus ? err.get("httpStatusCode").asInt()
                                                                     : serverFault ? 500 : 400)
                                         .build());
        }
        return traits;
    }

    // Translate C2J member metadata (location, locationName, timestampFormat, required, flags) into Smithy binding and
    // wire-name traits.
    private List<Trait> memberTraits(String c2jMemberName, String memberName, JsonNode m, String payloadMember,
                                     boolean required) {
        List<Trait> traits = new ArrayList<>();
        String location = m.path("location").asText(null);
        String locationName = m.path("locationName").asText(null);
        String wire = locationName != null ? locationName : memberName;

        if (location != null) {
            switch (location) {
                case "uri":
                    traits.add(new HttpLabelTrait());
                    // The URI label is renamed to the member name (see UriLabels); the original label name goes on a
                    // marker.
                    if (locationName != null && !locationName.equals(memberName)) {
                        traits.add(marker(C2jMarkerTrait.LOCATION_NAME, Node.from(locationName)));
                    }
                    break;
                case "header":
                    // Smithy forbids binding reserved headers such as Content-Length. The member is unbound and the
                    // header name goes on a marker.
                    if (RESERVED_HEADERS.contains(wire.toLowerCase(Locale.US))) {
                        traits.add(marker(C2jMarkerTrait.RESERVED_HEADER, Node.from(wire)));
                    } else {
                        traits.add(new HttpHeaderTrait(wire));
                    }
                    break;
                case "querystring":
                    // A map in the query string is one query parameter per entry, named by the key (codegen's
                    // QueryParamMarshaller), which is @httpQueryParams; @httpQuery is invalid on a map. The C2J
                    // locationName of such a member names no parameter.
                    if ("map".equals(shapes.path(m.path("shape").asText()).path("type").asText())) {
                        traits.add(new HttpQueryParamsTrait());
                        if (locationName != null && !locationName.equals(memberName)) {
                            gaps.record("member.locationName(querystring map)",
                                        "The locationName of a query string map names no query parameter, and "
                                        + "@httpQueryParams has none; it is dropped, so the member's SdkField "
                                        + "location name is the member name.");
                        }
                    } else {
                        traits.add(new HttpQueryTrait(wire));
                    }
                    break;
                case "statusCode":
                    traits.add(new HttpResponseCodeTrait());
                    break;
                case "headers":
                    traits.add(new HttpPrefixHeadersTrait(wire));
                    break;
                default:
                    throw new C2jConversionException(serviceName, c2jMemberName,
                                                     "Unsupported C2J location '" + location + "'");
            }
        }

        // The whole-body payload member (C2J "payload": "<member>").
        if (memberName.equals(payloadMember)) {
            traits.add(new HttpPayloadTrait());
        }

        // A body member wire name that differs from the member name -> @jsonName and @xmlName; each protocol reads only
        // its own.
        if (location == null && locationName != null && !locationName.equals(memberName)) {
            traits.add(new JsonNameTrait(locationName));
            traits.add(new XmlNameTrait(locationName));
        }

        if (m.has("timestampFormat")) {
            traits.add(new TimestampFormatTrait(smithyTimestampFormat(c2jMemberName, m.get("timestampFormat").asText())));
        }
        // C2J required is not a client-side guarantee: the SDK keeps required members nullable and tolerates their
        // absence, which is what @clientOptional means.
        if (required) {
            traits.add(new RequiredTrait());
            traits.add(new ClientOptionalTrait());
        }
        if (m.hasNonNull("documentation")) {
            traits.add(new DocumentationTrait(m.get("documentation").asText()));
        }
        if (m.path("flattened").asBoolean(false) || isFlattenedCollection(m.path("shape").asText())) {
            traits.add(new XmlFlattenedTrait());
        }
        if (m.path("idempotencyToken").asBoolean(false)) {
            traits.add(new IdempotencyTokenTrait());
        }
        if (m.path("sensitive").asBoolean(false)) {
            traits.add(new SensitiveTrait());
        }
        if (m.path("xmlAttribute").asBoolean(false)) {
            traits.add(new XmlAttributeTrait());
        }
        if (m.path("deprecated").asBoolean(false)) {
            traits.add(deprecatedTrait(m));
        }
        if (m.path("contextParam").isObject()) {
            traits.add(provide(new ContextParamTrait.Provider(), ShapeId.fromParts(namespace, "member"),
                               m.get("contextParam")));
        }
        if (m.path("queryName").isTextual()) {
            traits.add(new Ec2QueryNameTrait(m.get("queryName").asText()));
        }
        if (m.path("endpointdiscoveryid").asBoolean(false)) {
            traits.add(new ClientEndpointDiscoveryIdTrait());
        }
        if (hostLabelMembers.contains(c2jMemberName)) {
            traits.add(new HostLabelTrait());
        }
        if (m.path("eventpayload").asBoolean(false)) {
            traits.add(new EventPayloadTrait());
        }
        if (m.path("eventheader").asBoolean(false)) {
            traits.add(new EventHeaderTrait());
        }
        // A member-level streaming flag makes the member's blob target streaming (see assignStreamingTargets);
        // @streaming and @requiresLength are invalid on members.
        if (m.path("deprecatedName").isTextual()) {
            traits.add(marker(C2jMarkerTrait.DEPRECATED_NAME, Node.from(m.get("deprecatedName").asText())));
        }
        if (m.path("alternateBeanPropertyName").isTextual()) {
            traits.add(marker(C2jMarkerTrait.ALTERNATE_BEAN_PROPERTY_NAME,
                              Node.from(m.get("alternateBeanPropertyName").asText())));
        }
        // The rest-xml marshaller derives the operation's root XML namespace from its members' xmlNamespace.
        xmlNamespaceTrait(m.path("xmlNamespace")).ifPresent(traits::add);
        return traits;
    }

    // Whether a C2J shape is a list or map with the shape-level flattened flag.
    private boolean isFlattenedCollection(String shapeName) {
        JsonNode target = shapes.path(shapeName);
        String type = target.path("type").asText("");
        return target.path("flattened").asBoolean(false) && ("list".equals(type) || "map".equals(type));
    }

    // C2J xmlNamespace {uri[, prefix]} -> @xmlNamespace.
    private static Optional<Trait> xmlNamespaceTrait(JsonNode xmlns) {
        if (!xmlns.hasNonNull("uri")) {
            return Optional.empty();
        }
        XmlNamespaceTrait.Builder xb = XmlNamespaceTrait.builder().uri(xmlns.get("uri").asText());
        if (xmlns.hasNonNull("prefix")) {
            xb.prefix(xmlns.get("prefix").asText());
        }
        return Optional.of(xb.build());
    }

    // @deprecated with the C2J deprecatedMessage, if any.
    private static Trait deprecatedTrait(JsonNode c2j) {
        DeprecatedTrait.Builder b = DeprecatedTrait.builder();
        if (c2j.hasNonNull("deprecatedMessage")) {
            b.message(c2j.get("deprecatedMessage").asText());
        }
        return b.build();
    }

    // The target of a member or collection element; jsonvalue references target the shape that carries @mediaType.
    private ShapeId targetId(JsonNode memberOrRef) {
        String target = memberOrRef.path("shape").asText();
        if (memberOrRef.path("jsonvalue").asBoolean(false) && jsonValueTargets.containsKey(target)) {
            return ShapeId.fromParts(namespace, jsonValueTargets.get(target));
        }
        if (memberOrRef.path("streaming").asBoolean(false) && streamingTargets.containsKey(target)) {
            return ShapeId.fromParts(namespace, streamingTargets.get(target));
        }
        return shapeId(target);
    }

    // C2J timestamp format names -> @timestampFormat values.
    private String smithyTimestampFormat(String c2jName, String c2jFormat) {
        switch (c2jFormat) {
            case "iso8601":
                return "date-time";
            case "rfc822":
                return "http-date";
            case "unixTimestamp":
                return "epoch-seconds";
            default:
                throw new C2jConversionException(serviceName, c2jName, "Unknown C2J timestampFormat '" + c2jFormat + "'");
        }
    }

    // ----- service -----

    private ServiceShape buildService() {
        ShapeId serviceId = ShapeId.fromParts(namespace, serviceShapeName());
        ServiceShape.Builder svc = ServiceShape.builder()
                                               .id(serviceId)
                                               .version(metadata.path("apiVersion").asText("1.0"));
        protocolTraits.forEach(svc::addTrait);
        serviceTrait(serviceId).ifPresent(svc::addTrait);
        if (metadata.path("serviceFullName").isTextual()) {
            svc.addTrait(new TitleTrait(metadata.get("serviceFullName").asText()));
        }
        if (service.hasNonNull("documentation")) {
            svc.addTrait(new DocumentationTrait(service.get("documentation").asText()));
        }
        auth.serviceTraits().forEach(svc::addTrait);
        if (auth.serviceAuthConverted()) {
            convertedMetadata.add("auth");
            convertedMetadata.add("signatureVersion");
        }

        boolean queryCompatible = false;
        if (metadata.has("awsQueryCompatible")) {
            if (hasProtocol(AwsJson1_0Trait.ID) || hasProtocol(Rpcv2CborTrait.ID)) {
                queryCompatible = true;
                svc.addTrait(new AwsQueryCompatibleTrait());
                convertedMetadata.add("awsQueryCompatible");
            } else {
                gaps.record("metadata.awsQueryCompatible(protocol)", "aws.protocols#awsQueryCompatible applies only to "
                                                                     + "awsJson1_0 and rpcv2Cbor services; it is "
                                                                     + "dropped.");
            }
        }

        // The awsQuery and ec2Query protocols require the service to carry @xmlNamespace.
        boolean query = hasProtocol(AwsQueryTrait.ID) || hasProtocol(Ec2QueryTrait.ID);
        if (metadata.path("xmlNamespace").isTextual() || query) {
            String xmlns = metadata.path("xmlNamespace").asText(
                "https://" + metadata.path("endpointPrefix").asText("service") + ".amazonaws.com/doc/"
                + metadata.path("apiVersion").asText("2020-01-01") + "/");
            svc.addTrait(XmlNamespaceTrait.builder().uri(xmlns).build());
        }

        Set<String> suppressions = new TreeSet<>();
        JsonNode ccp = service.path("clientContextParams");
        if (ccp.isObject() && ccp.size() > 0) {
            svc.addTrait(provide(new ClientContextParamsTrait.Provider(), serviceId, ccp));
        }
        JsonNode ruleSet = withNormalizedParameterTypes(input.endpointRuleSetModel());
        if (ruleSet != null) {
            svc.addTrait(provide(new EndpointRuleSetTrait.Provider(), serviceId, ruleSet));
            // Smithy flags these AWS built-ins as DANGER because rules that use them need review; the SDK already
            // supports them, so the advisory is suppressed (as the published AWS Smithy models do).
            for (JsonNode parameter : ruleSet.path("parameters")) {
                String builtIn = parameter.path("builtIn").asText("");
                if (REVIEWED_BUILT_INS.contains(builtIn)) {
                    suppressions.add("RuleSetAwsBuiltIn." + builtIn);
                }
            }
        }
        // An empty test suite ("testCases": []) carries nothing, and as a trait it fails validation: Smithy reports every
        // rule-set parameter as unused by the tests (DANGER) and every required one as missing (ERROR). codegen
        // generates no endpoint tests for an empty suite, which is also what it does without the trait.
        JsonNode endpointTests = input.endpointTestsModel();
        boolean emptySuite = endpointTests != null && endpointTests.path("testCases").isArray()
                             && endpointTests.get("testCases").size() == 0;
        if (endpointTests != null && !emptySuite) {
            svc.addTrait(provide(new EndpointTestsTrait.Provider(), serviceId, endpointTests));
        }
        addClientEndpointDiscovery(svc, suppressions);
        if ((query || queryCompatible) && hasDuplicateErrorCodes()) {
            gaps.record("error.code(duplicate)", "Several bound error shapes share one wire error code, so a query "
                                                 + "or awsQueryCompatible client resolves that code to only one of "
                                                 + "them. The QueryErrorCode validation event is suppressed.");
            suppressions.add("QueryErrorCode");
        }
        if (!suppressions.isEmpty()) {
            svc.addTrait(SuppressTrait.builder().values(new ArrayList<>(suppressions)).build());
        }
        if (service.path("authorizers").isObject() && service.get("authorizers").size() > 0) {
            svc.addTrait(marker(C2jMarkerTrait.AUTHORIZERS, toNode(service.get("authorizers"))));
        }

        // The metadata properties no standard trait carries.
        ObjectNode.Builder unconverted = Node.objectNodeBuilder();
        for (Map.Entry<String, JsonNode> e : metadata.properties()) {
            if (!convertedMetadata.contains(e.getKey())) {
                unconverted.withMember(e.getKey(), toNode(e.getValue()));
            }
        }
        ObjectNode leftover = unconverted.build();
        if (!leftover.isEmpty()) {
            svc.addTrait(marker(C2jMarkerTrait.METADATA, leftover));
        }

        for (Map.Entry<String, JsonNode> e : service.path("operations").properties()) {
            svc.addOperation(operationId(e.getKey()));
        }
        return svc.build();
    }

    /**
     * The rule set with every parameter type spelled as the rules engine expects. codegen's endpoint generators read
     * parameter types ignoring case, and so does Smithy for {@code string} and {@code boolean} (the published rule sets
     * use {@code String} and {@code Boolean}), but Smithy accepts only the exact spelling {@code stringArray}.
     */
    private static JsonNode withNormalizedParameterTypes(JsonNode ruleSet) {
        if (ruleSet == null || !ruleSet.path("parameters").isObject()) {
            return ruleSet;
        }
        JsonNode normalized = null;
        for (Map.Entry<String, JsonNode> parameter : ruleSet.get("parameters").properties()) {
            String type = parameter.getValue().path("type").asText(null);
            if (STRING_ARRAY_PARAMETER_TYPE.equalsIgnoreCase(type) && !STRING_ARRAY_PARAMETER_TYPE.equals(type)) {
                if (normalized == null) {
                    normalized = ruleSet.deepCopy();
                }
                ((com.fasterxml.jackson.databind.node.ObjectNode) normalized.get("parameters").get(parameter.getKey()))
                    .put("type", STRING_ARRAY_PARAMETER_TYPE);
            }
        }
        return normalized == null ? ruleSet : normalized;
    }

    // The awsJson and rpcv2Cbor protocols name the service shape on the wire (X-Amz-Target, the request path), so the
    // service shape is named after the C2J targetPrefix when it is a valid identifier that does not collide. Otherwise
    // it is the sanitized serviceId, suffixed if it collides with a shape or operation name.
    private String serviceShapeName() {
        if (serviceShapeName != null) {
            return serviceShapeName;
        }
        Set<String> taken = new HashSet<>(takenNames);
        for (Map.Entry<String, JsonNode> e : service.path("operations").properties()) {
            taken.add(operationId(e.getKey()).getName().toLowerCase(Locale.US));
        }
        String targetPrefix = metadata.path("targetPrefix").asText(null);
        if (targetPrefix != null && IDENTIFIER.matcher(targetPrefix).matches()
            && !taken.contains(targetPrefix.toLowerCase(Locale.US))) {
            convertedMetadata.add("targetPrefix");
            serviceShapeName = targetPrefix;
        } else {
            String name = sanitizeShapeName(metadata.path("serviceId").asText("Service"));
            serviceShapeName = taken.contains(name.toLowerCase(Locale.US)) ? name + SERVICE_ID_SUFFIX : name;
        }
        return serviceShapeName;
    }

    // aws.api#service from serviceId (sdkId), endpointPrefix, signingName (arnNamespace) and uid (docId).
    private Optional<Trait> serviceTrait(ShapeId serviceId) {
        if (!metadata.path("serviceId").isTextual()) {
            return Optional.empty();
        }
        String sdkId = metadata.get("serviceId").asText();
        String endpointPrefix = metadata.path("endpointPrefix").asText(sdkId.toLowerCase(Locale.US));
        String arnNamespace = metadata.path("signingName").asText(endpointPrefix).toLowerCase(Locale.US);
        if (!ARN_NAMESPACE.matcher(arnNamespace).matches()) {
            arnNamespace = endpointPrefix.toLowerCase(Locale.US).replaceAll("[^a-z0-9.\\-]", "");
        }
        ServiceTrait.Builder b = ServiceTrait.builder()
                                             .sdkId(sdkId)
                                             .endpointPrefix(endpointPrefix)
                                             .arnNamespace(arnNamespace)
                                             .cloudFormationName(cloudFormationName(sdkId))
                                             .cloudTrailEventSource(arnNamespace + ".amazonaws.com");
        if (metadata.path("uid").isTextual()) {
            b.docId(metadata.get("uid").asText());
        }
        return Optional.of(b.build(serviceId));
    }

    private static String cloudFormationName(String sdkId) {
        String name = sdkId.replaceAll("[^A-Za-z0-9]", "");
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return CLOUD_FORMATION_NAME.matcher(name).matches() ? name : "Service" + name;
    }

    // aws.api#clientEndpointDiscovery names the C2J endpointoperation. Its error is the InvalidEndpoint* error that every
    // discovered operation declares; when there is none, the DANGER for the missing error is suppressed.
    // Whether two error shapes bound to operations share an effective wire error code (the C2J error code, else the C2J
    // shape name; errorTraits emits awsQueryError whenever that differs from the Smithy shape name).
    private boolean hasDuplicateErrorCodes() {
        Set<String> bound = new TreeSet<>();
        for (JsonNode op : service.path("operations")) {
            op.path("errors").forEach(err -> bound.add(err.path("shape").asText()));
        }
        Set<String> codes = new HashSet<>();
        for (String name : bound) {
            if (!codes.add(shapes.path(name).path("error").path("code").asText(name))) {
                return true;
            }
        }
        return false;
    }

    private void addClientEndpointDiscovery(ServiceShape.Builder svc, Set<String> suppressions) {
        if (endpointDiscoveryOperation == null) {
            return;
        }
        ClientEndpointDiscoveryTrait.Builder discovery = ClientEndpointDiscoveryTrait.builder()
                                                                                     .operation(operationId(
                                                                                         endpointDiscoveryOperation));
        Set<String> common = null;
        for (JsonNode op : service.path("operations")) {
            if (op.path("endpointdiscovery").isObject()) {
                Set<String> errors = new HashSet<>();
                op.path("errors").forEach(err -> errors.add(err.path("shape").asText()));
                if (common == null) {
                    common = errors;
                } else {
                    common.retainAll(errors);
                }
            }
        }
        String error = null;
        if (common != null) {
            for (String name : new TreeSet<>(common)) {
                if (name.startsWith("InvalidEndpoint")) {
                    error = name;
                    break;
                }
            }
        }
        if (error != null) {
            discovery.error(shapeId(error));
        } else {
            gaps.record("endpointdiscovery.error", "C2J does not name the invalid-endpoint error, and no InvalidEndpoint* "
                                                   + "error is declared by every discovered operation, so "
                                                   + "aws.api#clientEndpointDiscovery has no error.");
            suppressions.add("ClientEndpointDiscovery.MissingErrorDefinition");
        }
        svc.addTrait(discovery.build());
    }

    private String endpointDiscoveryOperation() {
        for (Map.Entry<String, JsonNode> e : service.path("operations").properties()) {
            if (e.getValue().path("endpointoperation").asBoolean(false)) {
                return e.getKey();
            }
        }
        return null;
    }

    private boolean hasProtocol(ShapeId protocol) {
        for (Trait t : protocolTraits) {
            if (t.toShapeId().equals(protocol)) {
                return true;
            }
        }
        return false;
    }

    private static List<Shape> one(Shape shape) {
        List<Shape> l = new ArrayList<>(1);
        l.add(shape);
        return l;
    }

    // One protocol trait per metadata.protocols entry (falling back to metadata.protocol), in C2J order. A protocol
    // with no mapping gets no trait and a gap entry.
    private List<Trait> protocolTraits() {
        List<String> names = new ArrayList<>();
        JsonNode list = metadata.path("protocols");
        if (list.isArray() && list.size() > 0) {
            list.forEach(p -> names.add(p.asText()));
        } else if (metadata.path("protocol").isTextual()) {
            names.add(metadata.get("protocol").asText());
        }
        ObjectNode httpConfiguration = httpConfiguration();
        List<Trait> traits = new ArrayList<>();
        Set<ShapeId> seen = new HashSet<>();
        for (String name : names) {
            Trait trait = protocolTrait(name, httpConfiguration);
            if (trait == null) {
                gaps.record("metadata.protocols=" + name,
                            "No protocol trait is emitted for C2J protocol '" + name + "'; the service cannot use it.");
            } else if (seen.add(trait.toShapeId())) {
                traits.add(trait);
            }
        }
        return traits;
    }

    private Trait protocolTrait(String protocol, ObjectNode httpConfiguration) {
        ShapeId target = ShapeId.fromParts(namespace, "service");
        switch (protocol) {
            case "rest-json":
                return new RestJson1Trait.Provider().createTrait(target, httpConfiguration);
            case "json":
                return "1.1".equals(metadata.path("jsonVersion").asText("1.0"))
                       ? new AwsJson1_1Trait.Provider().createTrait(target, httpConfiguration)
                       : new AwsJson1_0Trait.Provider().createTrait(target, httpConfiguration);
            case "rest-xml":
                return new RestXmlTrait.Provider().createTrait(target, httpConfiguration);
            case "smithy-rpc-v2-cbor":
                return Rpcv2CborTrait.fromNode(httpConfiguration);
            case "query":
                return new AwsQueryTrait.Provider().createTrait(target, Node.objectNode());
            case "ec2":
                return new Ec2QueryTrait();
            default:
                return null;
        }
    }

    // metadata.protocolSettings.h2 -> the http and eventStreamHttp lists of the HTTP-based protocol traits.
    private ObjectNode httpConfiguration() {
        JsonNode settings = metadata.path("protocolSettings");
        if (!settings.isObject()) {
            return Node.objectNode();
        }
        String h2 = settings.path("h2").asText("");
        ObjectNode.Builder config = Node.objectNodeBuilder();
        switch (h2) {
            case "eventstream":
                config.withMember("http", Node.fromStrings("http/1.1", "h2"))
                      .withMember("eventStreamHttp", Node.fromStrings("h2"));
                break;
            case "optional":
                config.withMember("http", Node.fromStrings("http/1.1", "h2"));
                break;
            case "required":
                config.withMember("http", Node.fromStrings("h2")).withMember("eventStreamHttp", Node.fromStrings("h2"));
                break;
            default:
                return Node.objectNode();
        }
        if (settings.size() == 1) {
            convertedMetadata.add("protocolSettings");
        }
        return config.build();
    }
}
