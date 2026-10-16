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


package software.amazon.awssdk.codecs.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codecs.json.internal.SmithyJsonSerdeProvider;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Verifies that double/float serialization via the Smithy JSON serializer produces
 * output that roundtrips correctly through {@link Double#parseDouble}/{@link Float#parseFloat}.
 * The Smithy serializer delegates to zmij for finite values outside the integer fast path.
 */
class SchubfachTest {

    private static final SmithyJsonSerdeProvider SMITHY = new SmithyJsonSerdeProvider();

    private String serializeDouble(double v) throws Exception {
        try (JsonCodec codec = JsonCodec.builder().overrideSerdeProvider(SMITHY).build();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeDouble(PreludeSchemas.DOUBLE, v);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String serializeFloat(float v) throws Exception {
        try (JsonCodec codec = JsonCodec.builder().overrideSerdeProvider(SMITHY).build();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ShapeSerializer serializer = codec.createSerializer(output)) {
                serializer.writeFloat(PreludeSchemas.FLOAT, v);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0,
            1.0,
            -1.0,
            42.0,
            3.14,
            -3.14,
            0.1,
            1e100,
            1e-100,
            1.7976931348623157E308,
            4.9E-324})
    void doubleRoundtripsCorrectly(double v) throws Exception {
        assertThat(Double.parseDouble(serializeDouble(v))).isEqualTo(v);
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0f,
            1.0f,
            -1.0f,
            42.0f,
            3.14f,
            -3.14f,
            0.1f,
            1e10f,
            1e-10f,
            Float.MAX_VALUE,
            Float.MIN_VALUE})
    void floatRoundtripsCorrectly(float v) throws Exception {
        assertThat(Float.parseFloat(serializeFloat(v))).isEqualTo(v);
    }
}
