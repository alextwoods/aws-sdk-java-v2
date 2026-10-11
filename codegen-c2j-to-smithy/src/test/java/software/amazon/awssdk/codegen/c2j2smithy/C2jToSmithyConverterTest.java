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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/**
 * Shape, member and operation conversion carried over from the source converter (task 1.20), adjusted for the
 * standard traits added in task 1.21. The standard-trait rows themselves are covered by
 * {@link StandardTraitConversionTest} and {@link PaginatorAndWaiterConversionTest}.
 */
class C2jToSmithyConverterTest {
    static final String REST_JSON_NS = "com.amazonaws.fixturerestjson";

    @Test
    void convert_restJsonFixture_createsServiceShapeFromMetadata() {
        ConversionResult result = TestModels.convertFixture("restjson");

        ServiceShape service = expectOnlyService(result.model());
        assertThat(service.getId()).isEqualTo(ShapeId.from(REST_JSON_NS + "#FixtureRestJson"));
        assertThat(service.getVersion()).isEqualTo("2024-01-01");
        assertThat(service.hasTrait(RestJson1Trait.class)).isTrue();
        assertThat(service.expectTrait(DocumentationTrait.class).getValue()).isEqualTo("<p>A rest-json fixture.</p>");
        assertThat(service.getOperations()).containsExactlyInAnyOrder(
            ShapeId.from(REST_JSON_NS + "#GetThing"), ShapeId.from(REST_JSON_NS + "#PutThing"),
            ShapeId.from(REST_JSON_NS + "#SendMetrics"), ShapeId.from(REST_JSON_NS + "#ListThings"),
            ShapeId.from(REST_JSON_NS + "#WatchThings"), ShapeId.from(REST_JSON_NS + "#ThingC2jOperation"));
    }

    @Test
    void convert_serviceIdCollidesWithShapeName_suffixesServiceShapeId() {
        ConversionResult result = TestModels.convertJson("budgets", "{'metadata': {'serviceId': 'Budgets', 'protocol': "
                                                                    + "'json'}, 'shapes': {'Budgets': {'type': 'list', "
                                                                    + "'member': {'shape': 'S'}}, 'S': {'type': "
                                                                    + "'string'}}}");

        assertThat(expectOnlyService(result.model()).getId())
            .isEqualTo(ShapeId.from("com.amazonaws.budgets#BudgetsC2jService"));
        assertThat(result.model().getShape(ShapeId.from("com.amazonaws.budgets#Budgets")).get().isListShape()).isTrue();
    }

    @Test
    void convert_serviceIdStartsWithDigit_prefixesNamespaceAndShapeName() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': '1 Thing!', 'protocol': 'json'}}");

        assertThat(expectOnlyService(result.model()).getId()).isEqualTo(ShapeId.from("com.amazonaws.s1thing#S1Thing"));
    }

    @Test
    void convert_operationNameCollidesWithShapeName_suffixesIdAndKeepsName() {
        Model model = TestModels.convertFixture("restjson").model();

        OperationShape op = model.expectShape(ShapeId.from(REST_JSON_NS + "#ThingC2jOperation"), OperationShape.class);
        assertThat(marker(op, C2jMarkerTrait.OPERATION_NAME)).isEqualTo(Node.from("ThingAction"));
        assertThat(op.getInputShape()).isEqualTo(ShapeId.from("smithy.api#Unit"));
        assertThat(op.getOutputShape()).isEqualTo(ShapeId.from("smithy.api#Unit"));
    }

    @Test
    void convert_operationNameCollidesWithShapeNameIgnoringCase_suffixesId() {
        ConversionResult result = TestModels.convertJson("q", "{'metadata': {'serviceId': 'Q', 'protocol': 'json'}, "
                                                              + "'operations': {'Thing': {'name': 'Thing'}}, "
                                                              + "'shapes': {'thing': {'type': 'string'}}}");

        assertThat(result.model().getShape(ShapeId.from("com.amazonaws.q#ThingC2jOperation"))).isPresent();
    }

    @Test
    void convert_restProtocol_emitsHttpTraitWithoutHttpMarker() {
        OperationShape op = restJsonOperation("GetThing");

        HttpTrait http = op.expectTrait(HttpTrait.class);
        assertThat(http.getMethod()).isEqualTo("GET");
        assertThat(http.getUri().toString()).isEqualTo("/things/{ThingId}");
        assertThat(http.getCode()).isEqualTo(200);
        assertThat(op.findTrait(C2jMarkerTrait.HTTP.traitId())).isEmpty();
    }

    @Test
    void convert_rpcProtocol_omitsHttpTraitButKeepsHttpMarker() {
        Model model = TestModels.convertFixture("json10").model();

        OperationShape op = model.expectShape(ShapeId.from("com.amazonaws.fixturejson10#SendThing"), OperationShape.class);
        assertThat(op.hasTrait(HttpTrait.class)).isFalse();
        assertThat(op.findTrait(C2jMarkerTrait.HTTP.traitId())).isPresent();
    }

    @Test
    void convert_requestMemberLocations_mapToHttpBindingTraits() {
        Model model = TestModels.convertFixture("restjson").model();

        MemberShape label = member(model, "GetThingRequest", "ThingId");
        assertThat(label.hasTrait(HttpLabelTrait.class)).isTrue();
        assertThat(label.hasTrait(RequiredTrait.class)).isTrue();
        assertThat(label.findTrait(C2jMarkerTrait.LOCATION_NAME.traitId())).isEmpty();
        assertThat(member(model, "GetThingRequest", "Version").expectTrait(HttpQueryTrait.class).getValue())
            .isEqualTo("version");
        assertThat(member(model, "GetThingRequest", "Trace").expectTrait(HttpHeaderTrait.class).getValue())
            .isEqualTo("X-Trace");
        MemberShape since = member(model, "GetThingRequest", "Since");
        assertThat(since.expectTrait(TimestampFormatTrait.class).getValue()).isEqualTo("http-date");
        assertThat(member(model, "GetThingRequest", "Token").hasTrait(IdempotencyTokenTrait.class)).isTrue();
    }

    @Test
    void convert_responseMemberLocations_mapToHttpBindingTraits() {
        Model model = TestModels.convertFixture("restjson").model();

        assertThat(member(model, "GetThingResponse", "Thing").hasTrait(HttpPayloadTrait.class)).isTrue();
        assertThat(member(model, "GetThingResponse", "Status").hasTrait(HttpResponseCodeTrait.class)).isTrue();
        assertThat(member(model, "GetThingResponse", "Meta").expectTrait(HttpPrefixHeadersTrait.class).getValue())
            .isEqualTo("x-meta-");
    }

    @Test
    void convert_bodyMemberLocationName_mapsToJsonNameAndXmlName() {
        MemberShape name = member(TestModels.convertFixture("restjson").model(), "Thing", "Name");

        assertThat(name.expectTrait(JsonNameTrait.class).getValue()).isEqualTo("name");
        assertThat(name.expectTrait(XmlNameTrait.class).getValue()).isEqualTo("name");
    }

    @Test
    void convert_collectionMemberLocationName_mapsToXmlNameOnly() {
        MemberShape element = member(TestModels.convertFixture("restjson").model(), "TagList", "member");

        assertThat(element.expectTrait(XmlNameTrait.class).getValue()).isEqualTo("Tag");
        assertThat(element.hasTrait(JsonNameTrait.class)).isFalse();
    }

    @Test
    void convert_flattenedCollectionShape_movesXmlFlattenedToTargetingMembers() {
        Model model = TestModels.convertFixture("restxml").model();

        assertThat(model.expectShape(ShapeId.from("com.amazonaws.fixturerestxml#FlatChanges"))
                        .hasTrait(XmlFlattenedTrait.class)).isFalse();
        assertThat(member(model, "com.amazonaws.fixturerestxml", "Change", "More").hasTrait(XmlFlattenedTrait.class))
            .isTrue();
        assertThat(member(model, "com.amazonaws.fixturerestxml", "ChangeRecordsRequest", "Changes")
                       .hasTrait(XmlFlattenedTrait.class)).isFalse();
    }

    @Test
    void convert_simpleShapes_mapConstraintsAndFlags() {
        Model model = TestModels.convertFixture("restjson").model();

        Shape name = restJsonShape(model, "ThingName");
        assertThat(name.expectTrait(LengthTrait.class).getMin()).contains(1L);
        assertThat(name.expectTrait(LengthTrait.class).getMax()).contains(64L);
        assertThat(name.hasTrait(SensitiveTrait.class)).isTrue();
        Shape count = restJsonShape(model, "Count");
        assertThat(count.getType()).isEqualTo(ShapeType.INTEGER);
        assertThat(count.expectTrait(RangeTrait.class).getMin()).contains(BigDecimal.ZERO);
        assertThat(count.expectTrait(RangeTrait.class).getMax()).contains(new BigDecimal("100"));
        assertThat(restJsonShape(model, "TagList").expectTrait(LengthTrait.class).getMax()).contains(10L);
        assertThat(restJsonShape(model, "Timestamp").expectTrait(TimestampFormatTrait.class).getValue())
            .isEqualTo("epoch-seconds");
        Shape blob = restJsonShape(model, "Blob");
        assertThat(blob.hasTrait(StreamingTrait.class)).isTrue();
        assertThat(blob.hasTrait(RequiresLengthTrait.class)).isTrue();
        assertThat(member(model, "Thing", "Created").expectTrait(TimestampFormatTrait.class).getValue())
            .isEqualTo("date-time");
    }

    @Test
    void convert_boxedNumeric_dropsBoxTrait() {
        Shape count = restJsonShape(TestModels.convertFixture("restjson").model(), "Count");

        assertThat(count.hasTrait(BoxTrait.class)).isFalse();
    }

    @Test
    void convert_documentAndUnionStructures_mapToDocumentAndUnionShapes() {
        Model model = TestModels.convertFixture("restjson").model();

        assertThat(restJsonShape(model, "Doc").getType()).isEqualTo(ShapeType.DOCUMENT);
        Shape union = restJsonShape(model, "ThingChoice");
        assertThat(union.getType()).isEqualTo(ShapeType.UNION);
        assertThat(union.getMemberNames()).containsExactly("Small", "Large");
    }

    @Test
    void convert_structureMembers_keepDeclarationOrderAndRequiredMarker() {
        Model model = TestModels.convertFixture("restjson").model();

        assertThat(restJsonShape(model, "Thing").getMemberNames())
            .containsExactly("Name", "Attributes", "Tags", "Created", "Choice", "Extra");
        assertThat(marker(restJsonShape(model, "PutThingRequest"), C2jMarkerTrait.REQUIRED))
            .isEqualTo(Node.fromStrings("ThingId", "Body"));
    }

    @Test
    void convert_operationProperties_mapToTraitsAndMarkers() {
        OperationShape get = restJsonOperation("GetThing");
        assertThat(get.expectTrait(DocumentationTrait.class).getValue()).isEqualTo("<p>Gets a thing.</p>");
        assertThat(marker(get, C2jMarkerTrait.OUTPUT_DOC)).isEqualTo(Node.from("The thing."));
        assertThat(get.getErrors()).containsExactly(ShapeId.from(REST_JSON_NS + "#ThingNotFound"),
                                                   ShapeId.from(REST_JSON_NS + "#InternalFailure"));

        OperationShape put = restJsonOperation("PutThing");
        assertThat(put.expectTrait(DeprecatedTrait.class).getMessage()).contains("Use GetThing.");
    }

    @Test
    void convert_json10Fixture_keepsDuplicateErrorsOnMarker() {
        Model model = TestModels.convertFixture("json10").model();

        OperationShape send = model.expectShape(ShapeId.from("com.amazonaws.fixturejson10#SendThing"),
                                                OperationShape.class);
        assertThat(send.getErrors()).containsExactly(ShapeId.from("com.amazonaws.fixturejson10#QueueMissing"),
                                                     ShapeId.from("com.amazonaws.fixturejson10#InvalidEndpointException"));
        assertThat(marker(send, C2jMarkerTrait.ERRORS).expectArrayNode().size()).isEqualTo(3);
        OperationShape ping = model.expectShape(ShapeId.from("com.amazonaws.fixturejson10#Ping"), OperationShape.class);
        assertThat(ping.getInputShape()).isEqualTo(ShapeId.from("smithy.api#Unit"));
    }

    @Test
    void convert_json11Fixture_emitsAwsJson11Trait() {
        assertThat(expectOnlyService(TestModels.convertFixture("json11").model()).hasTrait(AwsJson1_1Trait.class)).isTrue();
    }

    @Test
    void convert_queryFixture_emitsAwsQueryTraitAndXmlNamespace() {
        ServiceShape service = expectOnlyService(TestModels.convertFixture("query").model());

        assertThat(service.hasTrait(AwsQueryTrait.class)).isTrue();
        assertThat(service.expectTrait(XmlNamespaceTrait.class).getUri())
            .isEqualTo("http://fixturequery.amazonaws.com/doc/2010-03-31/");
    }

    @Test
    void convert_queryWithoutXmlNamespace_derivesServiceXmlNamespace() {
        ConversionResult result = TestModels.convertJson("q", "{'metadata': {'serviceId': 'Q', 'protocol': 'query', "
                                                              + "'endpointPrefix': 'q', 'apiVersion': '2011-01-01'}}");

        assertThat(expectOnlyService(result.model()).expectTrait(XmlNamespaceTrait.class).getUri())
            .isEqualTo("https://q.amazonaws.com/doc/2011-01-01/");
    }

    @Test
    void convert_restXmlFixture_mapsXmlTraitsAndInputMeta() {
        Model model = TestModels.convertFixture("restxml").model();
        String ns = "com.amazonaws.fixturerestxml";

        assertThat(expectOnlyService(model).hasTrait(RestXmlTrait.class)).isTrue();
        OperationShape op = model.expectShape(ShapeId.from(ns + "#ChangeRecords"), OperationShape.class);
        // The input reference's root element name equals the structure name, and its namespace goes on the structure,
        // so nothing is left for the inputMeta marker.
        assertThat(op.findTrait(C2jMarkerTrait.INPUT_META.traitId())).isEmpty();
        StructureShape input = model.expectShape(ShapeId.from(ns + "#ChangeRecordsRequest"), StructureShape.class);
        assertThat(input.expectTrait(XmlNamespaceTrait.class).getUri())
            .isEqualTo("https://fixturexml.amazonaws.com/doc/2013-04-01/");
        assertThat(input.hasTrait(XmlNameTrait.class)).isFalse();
        MemberShape owner = member(model, ns, "ChangeRecordsRequest", "Owner");
        assertThat(owner.hasTrait(XmlAttributeTrait.class)).isTrue();
        assertThat(owner.expectTrait(XmlNameTrait.class).getValue()).isEqualTo("owner");
        XmlNamespaceTrait typed = member(model, ns, "ChangeRecordsRequest", "Typed").expectTrait(XmlNamespaceTrait.class);
        assertThat(typed.getPrefix()).contains("xsi");
    }

    @Test
    void convert_anyFixture_recordsOneGapOccurrencePerEmittedMarker() {
        for (String fixture : ConvertAndAssembleTest.FIXTURES) {
            ConversionResult result = TestModels.convertFixture(fixture);
            Map<String, Integer> expected = new TreeMap<>();
            for (Shape shape : result.model().toSet()) {
                for (Trait trait : shape.getAllTraits().values()) {
                    if (trait.toShapeId().getNamespace().equals(C2jMarkerTrait.NAMESPACE)) {
                        expected.merge(markerFor(trait.toShapeId()).c2jProperty(), 1, Integer::sum);
                    }
                }
            }
            Map<String, Integer> markerGaps = new TreeMap<>(gapOccurrences(result));
            markerGaps.keySet().retainAll(markerProperties());

            assertThat(markerGaps).as(fixture).isEqualTo(expected);
        }
    }

    @Test
    void convert_sameInputTwice_producesIdenticalModelAndReport() {
        for (String fixture : ConvertAndAssembleTest.FIXTURES) {
            ConversionResult first = TestModels.convertFixture(fixture);
            ConversionResult second = TestModels.convertFixture(fixture);

            assertThat(serialize(second.model())).as(fixture).isEqualTo(serialize(first.model()));
            assertThat(second.gapReport().toJson()).as(fixture).isEqualTo(first.gapReport().toJson());
        }
    }

    static String serialize(Model model) {
        return Node.printJson(ModelSerializer.builder().build().serialize(model));
    }

    private static Set<String> markerProperties() {
        Set<String> properties = new HashSet<>();
        for (C2jMarkerTrait marker : C2jMarkerTrait.values()) {
            properties.add(marker.c2jProperty());
        }
        return properties;
    }

    private static C2jMarkerTrait markerFor(ShapeId traitId) {
        for (C2jMarkerTrait marker : C2jMarkerTrait.values()) {
            if (marker.traitId().equals(traitId)) {
                return marker;
            }
        }
        throw new AssertionError("Unknown marker " + traitId);
    }

    static Map<String, Integer> gapOccurrences(ConversionResult result) {
        Map<String, Integer> occurrences = new TreeMap<>();
        for (ConversionGapReport.Entry entry : result.gapReport().entries()) {
            occurrences.put(entry.property(), entry.occurrences());
        }
        return occurrences;
    }

    static ServiceShape expectOnlyService(Model model) {
        assertThat(model.getServiceShapes()).hasSize(1);
        return model.getServiceShapes().iterator().next();
    }

    static Node marker(Shape shape, C2jMarkerTrait marker) {
        return shape.findTrait(marker.traitId())
                    .orElseThrow(() -> new AssertionError(shape.getId() + " has no " + marker.traitId()))
                    .toNode();
    }

    static OperationShape restJsonOperation(String name) {
        return TestModels.convertFixture("restjson").model()
                         .expectShape(ShapeId.from(REST_JSON_NS + "#" + name), OperationShape.class);
    }

    static Shape restJsonShape(Model model, String name) {
        return model.expectShape(ShapeId.from(REST_JSON_NS + "#" + name));
    }

    static MemberShape member(Model model, String shape, String member) {
        return member(model, REST_JSON_NS, shape, member);
    }

    static MemberShape member(Model model, String namespace, String shape, String member) {
        return model.expectShape(ShapeId.fromParts(namespace, shape, member), MemberShape.class);
    }
}
