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

package software.amazon.awssdk.test.protocols;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.test.protocols.support.Canonical.Struct;
import software.amazon.awssdk.test.protocols.support.RandomModel;
import software.amazon.awssdk.test.protocols.support.RoundTrips;
import software.amazon.awssdk.test.protocols.support.WireProtocol;

/**
 * Property 12 (Requirements 5.1, 5.2, 5.8): for any valid value of a random schema and any launch protocol,
 * serialization followed by deserialization through the protocol runtime preserves member presence and values,
 * list and map order, blob bytes, timestamp values, union selection and event content.
 *
 * <p>Each try draws a protocol, a seed and a size; {@link RandomModel} derives from them a valid Smithy model (one
 * service, one operation) and a schema-valid input value as a canonical tree built without any codec. The value is
 * built as a Smithy Java dynamic-schema value (the stand-in for a generated type), round-tripped as
 * {@link RoundTrips} defines per protocol, and read back into a tree; the tree must equal the generated one exactly
 * (the documented lossy cases are excluded from generation, not normalized away: see {@link RandomModel}).
 * Failures report the model IDL, the value tree and the wire request/response.
 */
class SchemaSerdeRoundTripPropertyTest {
    // Feature: aws-sdk-java-v3, Property 12: Schema SERDE round trip
    // Each try assembles and validates a fresh model, so the tries are split evenly over the seven protocols
    // (about 140 each) by the statistics check below rather than increased further.
    @Property(tries = 1_000, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 12: Schema SERDE round trip")
    void schemaSerdeRoundTrip(@ForAll("validGeneratedValues") RandomModel model) {
        Statistics.label("protocol").collect(model.protocol());
        Statistics.label("event stream").collect(model.protocol(), model.hasEventStream());
        Statistics.label("protocol").coverage(coverage -> {
            for (WireProtocol protocol : WireProtocol.values()) {
                coverage.check(protocol).count(n -> n >= 50);
            }
        });
        RoundTrips.Result result;
        try {
            result = RoundTrips.run(model);
        } catch (RuntimeException | AssertionError e) {
            throw new AssertionError("Round trip failed under " + model.protocol() + "\n" + describe(model), e);
        }
        Map<String, Object> expectedOutput = new TreeMap<>(model.input().members());
        Map<String, Object> expectedUri = new TreeMap<>();
        for (RandomModel.UriMember member : model.uriMembers()) {
            Object value = expectedOutput.remove(member.name());
            if (value != null) {
                expectedUri.put(member.name(), value);
            }
        }
        String context = "under " + model.protocol() + "\n" + describe(model) + "\nwire:\n" + result.wire();
        assertEquals(new Struct(expectedOutput), result.output(), () -> "Round-tripped value differs " + context);
        assertEquals(new Struct(expectedUri), result.uriValues(), () -> "URI-bound values differ " + context);
    }

    private static String describe(RandomModel model) {
        return "model:\n" + model.idl() + "\ninput: " + model.input();
    }

    @Provide
    Arbitrary<RandomModel> validGeneratedValues() {
        return Combinators.combine(Arbitraries.of(WireProtocol.class), Arbitraries.longs(),
                                   Arbitraries.integers().between(0, 6))
                          .as(RandomModel::generate);
    }
}
