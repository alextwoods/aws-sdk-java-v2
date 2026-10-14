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

package software.amazon.awssdk.protocols.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.protocols.client.http.TestShapes.ERRORS;
import static software.amazon.awssdk.protocols.client.http.TestShapes.NS;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.protocols.client.http.TestShapes.FooError;
import software.amazon.awssdk.protocols.client.http.TestShapes.QueueMissing;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.serde.document.Document;

class ErrorTypeUtilsTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(value = {
        // Relative names resolve in the service namespace.
        "FooError, smithy.example#FooError",
        // Everything from the first ':' is dropped.
        "FooError:http://internal.amazon.com/coral/com.amazon.coral.validate/, smithy.example#FooError",
        "smithy.example#FooError:http://abc.com, smithy.example#FooError",
        // Absolute IDs match exactly.
        "smithy.example#FooError, smithy.example#FooError",
        // An absolute ID from another namespace falls back to its name in the service namespace.
        "aws.protocoltests.restjson#FooError, smithy.example#FooError",
        // Not registered, invalid, member IDs, or empty after normalization.
        "BarError, NULL",
        "smithy.example#BarError, NULL",
        "Foo Error, NULL",
        "smithy.example#FooError$member, NULL",
        ":FooError, NULL",
        "#, NULL",
    }, nullValues = "NULL")
    void resolveNormalizesTheDiscriminator(String errorType, String expected) {
        ShapeId resolved = ErrorTypeUtils.resolve(errorType, NS, ERRORS, false);
        assertThat(resolved).isEqualTo(expected == null ? null : ShapeId.from(expected));
    }

    @Test
    void resolveUsesCustomCodesOnlyWhenAsked() {
        assertThat(ErrorTypeUtils.resolve(QueueMissing.CODE, NS, ERRORS, true)).isEqualTo(QueueMissing.ID);
        assertThat(ErrorTypeUtils.resolve(QueueMissing.CODE + ":suffix", NS, ERRORS, true)).isEqualTo(QueueMissing.ID);
        assertThat(ErrorTypeUtils.resolve(QueueMissing.CODE, NS, ERRORS, false)).isNull();
        // With the index, shape names still resolve.
        assertThat(ErrorTypeUtils.resolve("QueueMissing", NS, ERRORS, true)).isEqualTo(QueueMissing.ID);
        assertThat(ErrorTypeUtils.resolve("FooError", NS, ERRORS, true)).isEqualTo(FooError.ID);
    }

    @Test
    void resolveWithAPlainTypeRegistryIgnoresTheCodeIndex() {
        TypeRegistry plain = TypeRegistry.builder().putLazyType(QueueMissing.ID, () -> QueueMissing.SCHEMA).build();
        assertThat(ErrorTypeUtils.resolve(QueueMissing.CODE, NS, plain, true)).isNull();
        assertThat(ErrorTypeUtils.resolve("QueueMissing", NS, plain, true)).isEqualTo(QueueMissing.ID);
    }

    @Test
    void resolveHandlesMissingInputs() {
        assertThat(ErrorTypeUtils.resolve(null, NS, ERRORS, true)).isNull();
        assertThat(ErrorTypeUtils.resolve("", NS, ERRORS, true)).isNull();
        assertThat(ErrorTypeUtils.resolve("FooError", NS, null, true)).isNull();
        assertThat(ErrorTypeUtils.resolve("FooError", null, ERRORS, false)).isNull();
        assertThat(ErrorTypeUtils.resolve("smithy.example#FooError", null, ERRORS, false)).isEqualTo(FooError.ID);
    }

    @Test
    void readTypeAndCodeAndMessage() {
        Document both = map("__type", "TypeValue", "code", "CodeValue", "Message", "M", "errorMessage", "E");
        assertThat(ErrorTypeUtils.readType(both)).isEqualTo("TypeValue");
        assertThat(ErrorTypeUtils.readTypeAndCode(both)).isEqualTo("TypeValue");
        assertThat(ErrorTypeUtils.readMessage(both)).isEqualTo("M");

        Document codeOnly = map("code", "CodeValue", "errorMessage", "E");
        assertThat(ErrorTypeUtils.readType(codeOnly)).isNull();
        assertThat(ErrorTypeUtils.readTypeAndCode(codeOnly)).isEqualTo("CodeValue");
        assertThat(ErrorTypeUtils.readMessage(codeOnly)).isEqualTo("E");

        assertThat(ErrorTypeUtils.readMessage(map("message", "m", "Message", "M"))).isEqualTo("m");
    }

    @Test
    void readIgnoresNonStringMembersAndNonMapDocuments() {
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("__type", Document.of(1));
        members.put("message", Document.of(2));
        Document numbers = Document.of(members);
        assertThat(ErrorTypeUtils.readTypeAndCode(numbers)).isNull();
        assertThat(ErrorTypeUtils.readMessage(numbers)).isNull();
        assertThat(ErrorTypeUtils.readTypeAndCode(Document.of("FooError"))).isNull();
        assertThat(ErrorTypeUtils.readTypeAndCode(null)).isNull();
    }

    @Test
    void removeUriAndNamespace() {
        assertThat(ErrorTypeUtils.removeUri("ns#Foo:http://x")).isEqualTo("ns#Foo");
        assertThat(ErrorTypeUtils.removeUri("Foo")).isEqualTo("Foo");
        assertThat(ErrorTypeUtils.removeUri(null)).isNull();
        assertThat(ErrorTypeUtils.removeNamespaceAndUri("ns#Foo:http://x")).isEqualTo("Foo");
        assertThat(ErrorTypeUtils.removeNamespaceAndUri("Foo:http://x")).isEqualTo("Foo");
        assertThat(ErrorTypeUtils.removeNamespaceAndUri(null)).isNull();
    }

    @Test
    void queryErrorCode() {
        assertThat(ErrorTypeUtils.queryErrorCode("AWS.SimpleQueueService.NonExistentQueue;Sender"))
            .isEqualTo("AWS.SimpleQueueService.NonExistentQueue");
        assertThat(ErrorTypeUtils.queryErrorCode("NoDelimiter")).isNull();
        assertThat(ErrorTypeUtils.queryErrorCode(";Sender")).isNull();
        assertThat(ErrorTypeUtils.queryErrorCode(null)).isNull();
    }

    private static Document map(String... keysAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            members.put(keysAndValues[i], Document.of(keysAndValues[i + 1]));
        }
        return Document.of(members);
    }
}
