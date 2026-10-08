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

package software.amazon.awssdk.dynamic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * Requirements 14.16, 19.6: a dynamic call applies {@code @default} values as a generated builder does. Unset input
 * members with a default are serialized with it, in nested structures and list elements too, and output members with a
 * default that the response omits read as it. Members without a default stay absent, and set members keep their value.
 */
class DynamicDefaultsTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.defaults

        @aws.protocols#awsJson1_0
        @aws.api#service(sdkId: "Defaults")
        service Defaults {
            version: "2020-01-01"
            operations: [Put]
        }

        operation Put {
            input := {
                note: String
                limit: Integer = 10
                mode: Mode = "RED"
                item: Item
                items: Items
            }
            output := {
                note: String
                total: Long = 0
                enabled: Boolean = false
                tags: Tags = []
                items: Items
            }
        }

        structure Item {
            label: String
            size: Integer = 1
        }

        list Items {
            member: Item
        }

        list Tags {
            member: String
        }

        enum Mode {
            RED
            BLUE
        }
        """;

    @Test
    void unsetInputDefaultsAreSerializedAndAbsentOutputDefaultsRead() {
        TestTransport transport = TestTransport.sync(
            r -> TestSupport.json(200, "{\"note\":\"n\",\"items\":[{\"label\":\"o\"},{\"size\":3}]}"));
        Document output;
        try (DynamicClient client = TestSupport.syncClient(transport).model(TestSupport.load(MODEL))
                                               .endpointOverride(URI.create("https://defaults.example.com"))
                                               .build()) {
            output = client.call("Put", Document.of(Map.of(
                "limit", Document.of(7),
                "item", Document.of(Map.of("label", Document.of("a"))),
                "items", Document.of(List.of(Document.of(Map.of("size", Document.of(2))))))));
        }

        assertEquals("{\"limit\":7,\"mode\":\"RED\",\"item\":{\"label\":\"a\",\"size\":1},\"items\":[{\"size\":2}]}",
                     transport.requests().get(0).body());

        assertEquals("n", output.getMember("note").asString());
        assertEquals(0L, output.getMember("total").asLong());
        assertEquals(false, output.getMember("enabled").asBoolean());
        assertEquals(List.of(), output.getMember("tags").asList());
        List<Document> items = output.getMember("items").asList();
        assertEquals("o", items.get(0).getMember("label").asString());
        assertEquals(1, items.get(0).getMember("size").asInteger());
        assertNull(items.get(1).getMember("label"));
        assertEquals(3, items.get(1).getMember("size").asInteger());
    }
}
