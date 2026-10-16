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


package software.amazon.awssdk.codecs.json.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.JsonNameTrait;

final class SmithyJsonSchemaExtensionsTest {
    @Test
    void precomputesEscapedFieldTokens() {
        Schema schema = Schema.structureBuilder(ShapeId.from("example#EscapedNames"))
                .putMember(
                        "value",
                        PreludeSchemas.STRING,
                        new JsonNameTrait("quoted\"slash\\line\n"))
                .build();

        SmithyJsonSchemaExtensions.NativeJsonExtension memberExtension = schema.member("value").getExtension(SmithyJsonSchemaExtensions.KEY);
        SmithyJsonSchemaExtensions.NativeJsonExtension structureExtension = schema.getExtension(SmithyJsonSchemaExtensions.KEY);

        assertThat(new String(memberExtension.jsonNameBytes(), StandardCharsets.UTF_8))
                .isEqualTo("\"quoted\\\"slash\\\\line\\n\":");
        assertThat(structureExtension.jsonFieldNameTable()[0])
                .containsExactly(memberExtension.jsonNameBytes());
    }
}
