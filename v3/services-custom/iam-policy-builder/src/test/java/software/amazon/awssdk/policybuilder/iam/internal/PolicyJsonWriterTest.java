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

package software.amazon.awssdk.policybuilder.iam.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * The output format of the V2 policy writer (Jackson's UTF-8 generator and {@code DefaultPrettyPrinter}) that
 * {@link PolicyJsonWriter} reproduces.
 */
class PolicyJsonWriterTest {
    private static final String NL = System.lineSeparator();

    @Test
    void writeValue_withSpecialCharacters_escapesLikeJackson() {
        String value = "\" \\ / \b \t \n \f \r \u0000 \u001f \u007f \u00e9 \u4e2d \uD83D\uDE00 \uDC00";

        String json = new PolicyJsonWriter(false).writeValue(value).getString();

        assertThat(json).isEqualTo("\"\\\" \\\\ / \\b \\t \\n \\f \\r \\u0000 \\u001F \u007f \u00e9 \u4e2d "
                                   + "\\uD83D\\uDE00 \\uDC00\"");
    }

    @Test
    void getBytes_isTheUtf8EncodingOfTheText() {
        PolicyJsonWriter writer = new PolicyJsonWriter(false).writeValue("\u00e9\uD83D\uDE00");

        assertThat(writer.getBytes()).isEqualTo(writer.getString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void compact_withNestedContainers_writesNoWhitespace() {
        assertThat(nested(false)).isEqualTo("{\"a\":\"1\",\"b\":[\"x\",\"y\",{\"c\":\"2\"},{}],\"d\":{},\"e\":[]}");
    }

    @Test
    void pretty_withNestedContainers_matchesJacksonsDefaultPrettyPrinter() {
        assertThat(nested(true)).isEqualTo("{" + NL
                                           + "  \"a\" : \"1\"," + NL
                                           + "  \"b\" : [ \"x\", \"y\", {" + NL
                                           + "    \"c\" : \"2\"" + NL
                                           + "  }, { } ]," + NL
                                           + "  \"d\" : { }," + NL
                                           + "  \"e\" : [ ]" + NL
                                           + "}");
    }

    private static String nested(boolean pretty) {
        PolicyJsonWriter w = new PolicyJsonWriter(pretty);
        w.writeStartObject();
        w.writeFieldName("a").writeValue("1");
        w.writeFieldName("b").writeStartArray().writeValue("x").writeValue("y");
        w.writeStartObject().writeFieldName("c").writeValue("2").writeEndObject();
        w.writeStartObject().writeEndObject();
        w.writeEndArray();
        w.writeFieldName("d").writeStartObject().writeEndObject();
        w.writeFieldName("e").writeStartArray().writeEndArray();
        w.writeEndObject();
        return w.getString();
    }
}
