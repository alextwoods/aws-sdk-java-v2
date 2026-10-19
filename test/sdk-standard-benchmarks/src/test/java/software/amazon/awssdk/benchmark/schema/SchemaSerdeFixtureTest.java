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

package software.amazon.awssdk.benchmark.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.core.SdkPojo;

/**
 * Checks the schema-serde benchmark fixtures: each protocol marshalls and unmarshalls the payload, and the serialized
 * bodies have the sizes Requirement 34.1 asks for (1 KB or less, 100 KB or more).
 */
class SchemaSerdeFixtureTest {
    private static final int ONE_KB = 1024;
    private static final int HUNDRED_KB = 100 * 1024;

    @ParameterizedTest
    @EnumSource(SchemaSerdeProtocol.class)
    void fixture_whenSmallPayload_bodiesAreAtMostOneKb(SchemaSerdeProtocol protocol) {
        SchemaSerdeFixture fixture = new SchemaSerdeFixture(protocol, SchemaSerdePayload.SMALL);

        report(protocol, SchemaSerdePayload.SMALL, fixture);
        assertThat(fixture.requestBodySize()).isBetween(1, ONE_KB);
        assertThat(fixture.responseBodySize()).isBetween(1, ONE_KB);
    }

    @ParameterizedTest
    @EnumSource(SchemaSerdeProtocol.class)
    void fixture_whenLargePayload_bodiesAreAtLeastHundredKb(SchemaSerdeProtocol protocol) {
        SchemaSerdeFixture fixture = new SchemaSerdeFixture(protocol, SchemaSerdePayload.LARGE);

        report(protocol, SchemaSerdePayload.LARGE, fixture);
        assertThat(fixture.requestBodySize()).isGreaterThanOrEqualTo(HUNDRED_KB);
        assertThat(fixture.responseBodySize()).isGreaterThanOrEqualTo(HUNDRED_KB);
    }

    @ParameterizedTest
    @EnumSource(SchemaSerdeProtocol.class)
    void unmarshall_whenGeneratedResponse_returnsThePayload(SchemaSerdeProtocol protocol) {
        for (SchemaSerdePayload payload : SchemaSerdePayload.values()) {
            SchemaSerdeFixture fixture = new SchemaSerdeFixture(protocol, payload);

            SdkPojo expected = (SdkPojo) fixture.output();

            // Compared by fields: equals() also compares the response metadata, which only the read response has.
            assertThat(expected.equalsBySdkFields(fixture.unmarshall())).as(payload.name()).isTrue();
            // A second call reads a new stream over the same bytes.
            assertThat(expected.equalsBySdkFields(fixture.unmarshall())).as(payload.name()).isTrue();
        }
    }

    private static void report(SchemaSerdeProtocol protocol, SchemaSerdePayload payload, SchemaSerdeFixture fixture) {
        System.out.printf("payload-size %s %s request=%d response=%d%n", protocol, payload,
                          fixture.requestBodySize(), fixture.responseBodySize());
    }
}
