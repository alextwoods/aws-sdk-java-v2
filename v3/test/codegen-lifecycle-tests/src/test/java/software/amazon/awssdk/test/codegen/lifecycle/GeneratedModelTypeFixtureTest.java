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

package software.amazon.awssdk.test.codegen.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.MissingRequiredMembersException;
import software.amazon.awssdk.test.codegen.lifecycle.model.CodegenLifecycleFixtureException;
import software.amazon.awssdk.test.codegen.lifecycle.model.Color;
import software.amazon.awssdk.test.codegen.lifecycle.model.GetWidgetResponse;
import software.amazon.awssdk.test.codegen.lifecycle.model.Limits;
import software.amazon.awssdk.test.codegen.lifecycle.model.ServiceUnavailableException;
import software.amazon.awssdk.test.codegen.lifecycle.model.Size;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetAnnotation;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetDetails;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetNotFoundException;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetShape;
import software.amazon.awssdk.test.codegen.lifecycle.model.WidgetTree;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Runtime behavior of the generated model types of the fixture service (task 3.2): direct SERDE through Smithy Java's
 * {@link JsonCodec}, V2-compatible equality and string rendering, deep immutability, required-member aggregation,
 * recursion, unions, unknown enum values and modeled errors.
 */
class GeneratedModelTypeFixtureTest {

    private static final String NAMESPACE = "software.amazon.awssdk.test.codegen.lifecycle";
    private static final JsonCodec JSON = JsonCodec.builder().build();

    private static ShapeId id(String relative) {
        return ShapeId.from(NAMESPACE + "#" + relative);
    }

    private static WidgetDetails details() {
        Map<String, List<String>> tags = new LinkedHashMap<>();
        tags.put("b", List.of("x", "y"));
        tags.put("a", List.of());
        return WidgetDetails.builder()
                            .owner("owner")
                            .serial(9_007_199_254_740_993L)
                            .revision(3)
                            .secret("hunter2")
                            .secretsByName(Map.of("pin", "1234"))
                            .colors(Color.GREEN, Color.RED)
                            .colorsByName(Map.of("sky", Color.GREEN))
                            .sizes(Size.LARGE, Size.SMALL)
                            .matrix(List.of("a", "b"), List.of(), List.of("c"))
                            .tagsByGroup(tags)
                            .thumbnail(SdkBytes.fromUtf8String("png"))
                            .createdAt(Instant.ofEpochSecond(1_700_000_000L))
                            .build();
    }

    private static WidgetTree tree() {
        WidgetTree root = WidgetTree.builder().label("root").build();
        return WidgetTree.builder()
                         .label("mid")
                         .parent(root)
                         .children(c -> c.label("leaf").parent(root), c -> c.label("other"))
                         .build();
    }

    private static GetWidgetResponse response() {
        return GetWidgetResponse.builder()
                                .widgetId("w-1")
                                .name("widget")
                                .tree(tree())
                                .outline(WidgetShape.fromNested(n -> n.tree(tree())))
                                .color(Color.RED)
                                .size(Size.SMALL)
                                .limits(l -> l.code("abc").flags(true, false))
                                .details(details())
                                .build();
    }

    private static <T extends software.amazon.smithy.java.core.schema.SerializableShape> T roundTrip(
        T value, software.amazon.smithy.java.core.schema.ShapeBuilder<T> builder) {
        return JSON.deserializeShape(JSON.serializeToString(value), builder);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // SERDE.

    @Test
    void everyModeledMemberRoundTripsThroughTheJsonCodec() {
        GetWidgetResponse response = response();
        GetWidgetResponse copy = roundTrip(response, GetWidgetResponse.builder());
        assertEquals(response, copy);
        assertEquals(response.hashCode(), copy.hashCode());
        assertEquals(JSON.serializeToString(response), JSON.serializeToString(copy));
        assertEquals(9_007_199_254_740_993L, copy.details().serial());
        assertEquals("png", copy.details().thumbnail().asUtf8String());
        assertEquals(List.of("b", "a"), List.copyOf(copy.details().tagsByGroup().keySet()), "map order is kept");
    }

    @Test
    void defaultsInitializeTheBuilderAndAreSerialized() {
        Limits limits = Limits.builder().code("abc").build();
        assertEquals(50, limits.percent());
        assertTrue(limits.enabled());
        assertFalse(limits.hasFlags());
        assertEquals(1.5, details().ratio());
        String json = JSON.serializeToString(limits);
        assertTrue(json.contains("\"percent\":50"), json);
        assertEquals(limits, roundTrip(limits, Limits.builder()));
    }

    @Test
    void getMemberValueExposesEveryModeledMemberWithoutReflection() {
        WidgetDetails details = details();
        assertEquals("owner", details.getMemberValue(WidgetDetails.SCHEMA.member("owner")));
        assertEquals(3, (Integer) details.getMemberValue(WidgetDetails.SCHEMA.member("revision")));
        assertEquals(List.of("green", "red"), details.getMemberValue(WidgetDetails.SCHEMA.member("colors")));
        assertEquals(List.of(10, 1), details.getMemberValue(WidgetDetails.SCHEMA.member("sizes")));
        assertNull(WidgetDetails.builder().owner("o").serial(1L).revision(1).build()
                                .getMemberValue(WidgetDetails.SCHEMA.member("secret")));
        assertThrows(IllegalArgumentException.class, () -> details.getMemberValue(Limits.SCHEMA.member("code")));
    }

    @Test
    void setMemberValueAssignsMembersByIndex() {
        WidgetDetails.Builder builder = WidgetDetails.builder();
        builder.setMemberValue(WidgetDetails.SCHEMA.member("owner"), "o");
        builder.setMemberValue(WidgetDetails.SCHEMA.member("serial"), 7L);
        builder.setMemberValue(WidgetDetails.SCHEMA.member("revision"), 2);
        builder.setMemberValue(WidgetDetails.SCHEMA.member("colors"), List.of("red"));
        WidgetDetails details = builder.build();
        assertEquals(List.of(Color.RED), details.colors());
        assertEquals(7L, details.serial());
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Equality, hash and string rendering.

    @Test
    void equalityAndHashCoverEveryModeledMember() {
        assertEquals(details(), details());
        assertEquals(details().hashCode(), details().hashCode());
        assertEquals(details(), details().toBuilder().build());
        assertNotEquals(details(), details().toBuilder().revision(4).build());
        assertNotEquals(details(), details().toBuilder().colors(Color.RED, Color.GREEN).build(), "order matters");
        WidgetDetails base = WidgetDetails.builder().owner("o").serial(1L).revision(1).build();
        WidgetDetails empty = base.toBuilder().colors(List.of()).build();
        assertNotEquals(base, empty, "an absent collection differs from an empty one, as hasXxx() does");
        assertFalse(base.hasColors());
        assertTrue(empty.hasColors());
        assertEquals(List.of(), base.colors());
    }

    @Test
    void toStringRendersMemberNamesAndRedactsSensitiveValues() {
        String rendered = details().toString();
        assertTrue(rendered.startsWith("WidgetDetails("), rendered);
        assertTrue(rendered.contains("owner=owner"), rendered);
        assertTrue(rendered.contains("secret=*** Sensitive Data Redacted ***"), rendered);
        assertTrue(rendered.contains("secretsByName=*** Sensitive Data Redacted ***"), rendered);
        assertFalse(rendered.contains("hunter2"), rendered);
        assertFalse(rendered.contains("1234"), rendered);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Immutability.

    @Test
    void builtValuesAreDeeplyImmutableSnapshots() {
        List<String> row = new ArrayList<>(List.of("a"));
        List<List<String>> matrix = new ArrayList<>(List.of(row));
        Map<String, List<String>> tags = new LinkedHashMap<>(Map.of("g", row));
        WidgetDetails.Builder builder = WidgetDetails.builder().owner("o").serial(1L).revision(1)
                                                     .matrix(matrix).tagsByGroup(tags);
        WidgetDetails details = builder.build();
        row.add("mutated");
        matrix.add(List.of());
        tags.put("h", List.of());
        assertEquals(List.of(List.of("a")), details.matrix());
        assertEquals(Map.of("g", List.of("a")), details.tagsByGroup());
        assertThrows(UnsupportedOperationException.class, () -> details.matrix().add(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> details.matrix().get(0).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> details.tagsByGroup().put("x", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> details.tagsByGroup().get("g").add("x"));
        assertThrows(UnsupportedOperationException.class, () -> details.colors().add(Color.RED));

        // The builder kept the caller's collections; a later build snapshots their current state, the first is intact.
        WidgetDetails later = builder.build();
        assertEquals(List.of(List.of("a", "mutated"), List.of()), later.matrix());
        assertEquals(List.of(List.of("a")), details.matrix());

        WidgetDetails deserialized = roundTrip(details(), WidgetDetails.builder());
        assertThrows(UnsupportedOperationException.class, () -> deserialized.matrix().get(0).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> deserialized.tagsByGroup().get("b").add("x"));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Requiredness.

    @Test
    void missingRequiredMembersAreAggregatedIntoOneException() {
        MissingRequiredMembersException e = assertThrows(MissingRequiredMembersException.class,
                                                         () -> WidgetDetails.builder().build());
        assertEquals(WidgetDetails.ID, e.shape());
        assertEquals(List.of(id("WidgetDetails$owner"), id("WidgetDetails$serial"), id("WidgetDetails$revision")),
                     e.missingMembers(), "a required member with a default is not missing");

        e = assertThrows(MissingRequiredMembersException.class,
                         () -> WidgetDetails.builder().serial(1L).ratio(null).build());
        assertEquals(List.of(id("WidgetDetails$owner"), id("WidgetDetails$revision"), id("WidgetDetails$ratio")),
                     e.missingMembers(), "explicitly clearing a defaulted member makes it missing");

        byte[] partial = "{\"owner\":\"o\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        e = assertThrows(MissingRequiredMembersException.class,
                         () -> WidgetDetails.builder().deserialize(JSON.createDeserializer(partial)).build());
        assertEquals(List.of(id("WidgetDetails$serial"), id("WidgetDetails$revision")), e.missingMembers());

        // Codec.deserializeShape applies Smithy client error correction before build(), as Smithy Java clients do.
        WidgetDetails corrected = JSON.deserializeShape(partial, WidgetDetails.builder());
        assertEquals(0L, corrected.serial());
    }

    @Test
    void errorCorrectionFillsMissingRequiredMembersWithZeroValues() {
        WidgetDetails corrected = WidgetDetails.builder().owner("o").errorCorrection().build();
        assertEquals("o", corrected.owner());
        assertEquals(0L, corrected.serial());
        assertEquals(0, corrected.revision());
    }

    @Test
    void errorCorrectionLeavesAMissingRequiredDocumentNullAndPresent() {
        // Without error correction a missing (or explicitly cleared) required document is missing.
        MissingRequiredMembersException e = assertThrows(MissingRequiredMembersException.class,
                                                         () -> WidgetAnnotation.builder().note("n").build());
        assertEquals(List.of(id("WidgetAnnotation$body")), e.missingMembers());
        assertThrows(MissingRequiredMembersException.class, () -> WidgetAnnotation.builder().body(null).build());

        // Smithy client error correction: a document has no zero value, so it stays null and counts as present.
        WidgetAnnotation corrected = WidgetAnnotation.builder().note("n").errorCorrection().build();
        assertNull(corrected.body());
        assertEquals("n", corrected.note());
        assertNull(corrected.toBuilder().build().body(), "a copy of the error-corrected value builds");

        // A complete value copied and then cleared is missing again.
        WidgetAnnotation complete = WidgetAnnotation.builder().body(Document.of("x")).build();
        e = assertThrows(MissingRequiredMembersException.class, () -> complete.toBuilder().body(null).build());
        assertEquals(List.of(id("WidgetAnnotation$body")), e.missingMembers());

        // Codec.deserializeShape error-corrects, so a response without the document deserializes.
        WidgetAnnotation read = JSON.deserializeShape("{\"note\":\"n\"}", WidgetAnnotation.builder());
        assertNull(read.body());
        assertEquals("{\"note\":\"n\"}", JSON.serializeToString(read));
        assertEquals("x", roundTrip(complete, WidgetAnnotation.builder()).body().asString());
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Recursion and unions.

    @Test
    void recursiveStructuresAndListsRoundTrip() {
        WidgetTree tree = tree();
        assertSame(WidgetTree.SCHEMA, WidgetTree.SCHEMA.member("parent").memberTarget());
        assertEquals("root", tree.children().get(0).parent().label());
        assertEquals(tree, roundTrip(tree, WidgetTree.builder()));
    }

    @Test
    void unionsExposeTheirSetMember() {
        assertEquals(WidgetShape.Type.CIRCLE, WidgetShape.fromCircle(2.0).type());
        assertEquals(WidgetShape.Type.NONE, WidgetShape.fromNone(Unit.getInstance()).type());
        assertEquals(WidgetShape.Type.UNKNOWN_TO_SDK_VERSION, WidgetShape.builder().build().type());
        assertNull(WidgetShape.builder().circle(1.0).tree(tree()).build().type(), "more than one member is set");

        WidgetShape nested = WidgetShape.fromNested(WidgetShape.fromNested(WidgetShape.fromCircle(3.0)));
        WidgetShape copy = roundTrip(nested, WidgetShape.builder());
        assertEquals(nested, copy);
        assertEquals(WidgetShape.Type.NESTED, copy.type());
        assertEquals(3.0, copy.nested().nested().circle());
        assertEquals(WidgetShape.Type.NONE, roundTrip(WidgetShape.fromNone(Unit.getInstance()),
                                                      WidgetShape.builder()).type());
    }

    @Test
    void unknownUnionMembersReadAsUnknownToSdkVersion() {
        WidgetShape shape = JSON.deserializeShape("{\"hexagon\":6}", WidgetShape.builder());
        assertEquals(WidgetShape.Type.UNKNOWN_TO_SDK_VERSION, shape.type());
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Enums.

    @Test
    void enumAccessorsKeepTheModeledValue() {
        assertEquals(Color.GREEN, Color.fromValue("green"));
        assertEquals(Color.UNKNOWN_TO_SDK_VERSION, Color.fromValue("purple"));
        assertNull(Color.fromValue(null));
        assertEquals("red", Color.RED.toString());
        assertEquals(java.util.Set.of(Color.RED, Color.GREEN), Color.knownValues());
        assertEquals(10, Size.LARGE.value());
        assertEquals(Size.UNKNOWN_TO_SDK_VERSION, Size.fromValue(7));

        WidgetDetails details = details();
        assertEquals(List.of("green", "red"), details.colorsAsStrings());
        assertEquals(Map.of("sky", "green"), details.colorsByNameAsStrings());
        assertEquals(List.of(10, 1), details.sizesAsIntegers());
        WidgetDetails unknown = details.toBuilder().colorsWithStrings("red", "purple").sizesWithIntegers(7).build();
        assertEquals(List.of(Color.RED, Color.UNKNOWN_TO_SDK_VERSION), unknown.colors());
        assertEquals(List.of("red", "purple"), unknown.colorsAsStrings());
        assertEquals(List.of(Size.UNKNOWN_TO_SDK_VERSION), unknown.sizes());
    }

    @Test
    void unknownEnumValuesSurviveDeserialization() {
        GetWidgetResponse response = JSON.deserializeShape(
            "{\"widgetId\":\"w\",\"color\":\"purple\",\"size\":7}", GetWidgetResponse.builder());
        assertEquals(Color.UNKNOWN_TO_SDK_VERSION, response.color());
        assertEquals("purple", response.colorAsString());
        assertEquals(Size.UNKNOWN_TO_SDK_VERSION, response.size());
        assertEquals(7, response.sizeAsInteger());
        String json = JSON.serializeToString(response);
        assertTrue(json.contains("\"color\":\"purple\""), json);
        assertTrue(json.contains("\"size\":7"), json);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Errors.

    @Test
    void modeledErrorsAreServiceExceptionsAndSerializableStructs() {
        WidgetNotFoundException error = WidgetNotFoundException.builder().message("gone").widgetId("w-1")
                                                               .statusCode(404).build();
        assertInstanceOf(CodegenLifecycleFixtureException.class, error);
        assertInstanceOf(AwsServiceException.class, error);
        assertEquals("w-1", error.widgetId());
        assertEquals(404, error.statusCode());
        assertTrue(error.getMessage().startsWith("gone"), error.getMessage());
        assertEquals("gone", error.getMemberValue(WidgetNotFoundException.SCHEMA.member("message")));
        assertEquals("w-1", error.getMemberValue(WidgetNotFoundException.SCHEMA.member("widgetId")));
        assertFalse(error.isRetryableException());

        WidgetNotFoundException copy = error.toBuilder().build();
        assertEquals("w-1", copy.widgetId());
        assertEquals(404, copy.statusCode());

        String json = JSON.serializeToString(error);
        assertEquals("{\"message\":\"gone\",\"widgetId\":\"w-1\"}", json);
        WidgetNotFoundException read = JSON.deserializeShape(json, WidgetNotFoundException.builder());
        assertEquals("w-1", read.widgetId());
        assertTrue(read.getMessage().startsWith("gone"), read.getMessage());
    }

    @Test
    void retryableErrorsAreRetryable() {
        ServiceUnavailableException error = ServiceUnavailableException.builder().message("busy").build();
        assertTrue(error.isRetryableException());
        assertInstanceOf(CodegenLifecycleFixtureException.class, error);
    }
}
