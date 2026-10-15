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

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * C2J forms that codegen's C2J route accepts and the C2J test models of {@code test/codegen-generated-classes-test}
 * and {@code test/protocol-tests} use, which the converter used to reject or translate into an invalid Smithy model
 * (checkpoint 6).
 */
class LegacyC2jFormsConversionTest {
    private static final String NS = "com.amazonaws.x";

    // ----- bigdecimal -----

    @Test
    void lowercaseBigNumberTypes_becomeBigDecimalAndBigIntegerShapes() {
        ConversionResult result = TestModels.convertJson("x", "{'metadata': {'serviceId': 'X', 'protocol': 'json'}, "
                                                              + "'shapes': {'D': {'type': 'bigdecimal'}, "
                                                              + "'I': {'type': 'biginteger'}}}");

        assertThat(result.model().expectShape(ShapeId.from(NS + "#D")).getType()).isEqualTo(ShapeType.BIG_DECIMAL);
        assertThat(result.model().expectShape(ShapeId.from(NS + "#I")).getType()).isEqualTo(ShapeType.BIG_INTEGER);
    }
}
