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

package software.amazon.awssdk.protocol.smithytests.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;

class LegacyDispositionToolTest {

    @Test
    void csvLine_quotesFieldsWithCommasAndQuotes() {
        assertThat(LegacyDispositionTool.csvLine("a", "b,c", "say \"hi\"", null))
            .isEqualTo("a,\"b,c\",\"say \"\"hi\"\"\",");
    }

    @Test
    void csvLine_replacesLineBreaksWithSpaces() {
        assertThat(LegacyDispositionTool.csvLine("one\ntwo\r\nthree")).isEqualTo("one two  three");
    }

    @Test
    void words_splitsCamelCaseAndDropsFillerWords() {
        assertThat(Matcher.words("RestJsonSerializesEmptyMap"))
            .containsExactlyInAnyOrder("empty", "map");
        assertThat(Matcher.words("Empty maps are marshalled correctly"))
            .containsExactlyInAnyOrder("empty", "maps");
    }

    @Test
    void request_emptyObjectBody_isDistinctFromEmptyBody() {
        ShapeView.OperationView op = new NoMemberOperation();
        FeatureExtractor.RequestFacts json = new FeatureExtractor.RequestFacts();
        json.body = " { } ";
        FeatureExtractor.RequestFacts empty = new FeatureExtractor.RequestFacts();
        empty.body = "";

        assertThat(FeatureExtractor.request(op, Node.objectNode(), false, json)).contains("reqbody:emptyObject");
        assertThat(FeatureExtractor.request(op, Node.objectNode(), false, empty)).contains("reqbody:empty");
    }

    @Test
    void request_methodIsAFeatureOnlyWhenAssertedForRestProtocols() {
        ShapeView.OperationView op = new NoMemberOperation();
        FeatureExtractor.RequestFacts unasserted = new FeatureExtractor.RequestFacts();
        FeatureExtractor.RequestFacts asserted = new FeatureExtractor.RequestFacts();
        asserted.method = "get";

        assertThat(FeatureExtractor.request(op, Node.objectNode(), true, unasserted))
            .noneMatch(f -> f.startsWith("method:"));
        assertThat(FeatureExtractor.request(op, Node.objectNode(), true, asserted)).contains("method:GET");
        assertThat(FeatureExtractor.request(op, Node.objectNode(), false, asserted))
            .noneMatch(f -> f.startsWith("method:"));
    }

    @Test
    void contentCheck_identicalRequestCopy_hasNoDifferences() {
        assertThat(ContentCheck.differences(legacyRequest("{\"name\": \"foo\"}", "{\"name\":\"foo\"}"),
                                            smithyRequest("{\"name\": \"foo\"}")))
            .isEmpty();
    }

    @Test
    void contentCheck_editedRequestBody_isADifference() {
        assertThat(ContentCheck.differences(legacyRequest("{\"name\": \"foo\"}", "{\"Name\": \"foo\"}"),
                                            smithyRequest("{\"name\": \"foo\"}")))
            .containsExactly("body (jsonEquals) differs");
    }

    @Test
    void contentCheck_legacyAssertsFewerHeaders_isNotADifference() {
        ObjectNode legacy = legacyRequest("{\"name\": \"foo\"}", "{\"name\": \"foo\"}");
        HttpRequestTestCase smithy = smithyRequest("{\"name\": \"foo\"}").toBuilder()
                                                                         .putHeader("X-Amz-Target", "Svc.Op")
                                                                         .build();

        assertThat(ContentCheck.differences(legacy, smithy)).isEmpty();
    }

    @Test
    void contentCheck_editedResponseBodyKey_isADifference() {
        ObjectNode legacy = Node.parse("{\"id\": \"Err\", \"given\": {\"response\": {\"status_code\": 400, "
                                       + "\"body\": \"{\\\"Message\\\": \\\"Hi\\\"}\"}}, "
                                       + "\"when\": {\"action\": \"errorUnmarshall\", \"operation\": \"Op\"}, "
                                       + "\"then\": {\"deserializedAs\": {\"message\": \"Hi\"}}}").expectObjectNode();
        HttpResponseTestCase smithy = HttpResponseTestCase.builder()
                                                          .id("Err").protocol(ShapeId.from(AWS_JSON_1_1)).code(400)
                                                          .body("{\"message\": \"Hi\"}")
                                                          .bodyMediaType("application/json")
                                                          .params(Node.objectNode().withMember("message", "Hi"))
                                                          .build();

        assertThat(ContentCheck.differences(legacy, smithy)).containsExactly("body differs");
    }

    @Test
    void decide_idMatchWithDifferentContent_isFlagged() {
        SmithyCase smithy = smithyCases("{\"name\": \"foo\"}").get(0);
        Matcher matcher = new Matcher(Collections.singletonList(smithy));
        Dispositions dispositions = new Dispositions(Collections.emptyList(), Collections.singletonList(smithy));

        Dispositions.Row same = dispositions.decide(
            legacyCase(legacyRequest("{\"name\": \"foo\"}", "{\"name\": \"foo\"}")),
            matcher.match(legacyCase(legacyRequest("{\"name\": \"foo\"}", "{\"name\": \"foo\"}")), null));
        LegacyCase edited = legacyCase(legacyRequest("{\"name\": \"foo\"}", "{\"name\": \"bar\"}"));
        Dispositions.Row differs = dispositions.decide(edited, matcher.match(edited, null));

        assertThat(same.disposition).isEqualTo(Dispositions.EQUIVALENT);
        assertThat(same.needsHumanReview).isFalse();
        assertThat(differs.disposition).isEqualTo(Dispositions.EQUIVALENT);
        assertThat(differs.needsHumanReview).isTrue();
        assertThat(differs.reasonColumn()).contains("differs from the Smithy case: body (jsonEquals) differs")
                                          .endsWith("(needs human review)");
    }

    @Test
    void reviews_movedToHandwrittenMustBeRetired() {
        Dispositions.Review review = new Dispositions.Review("x.json#1", null, Dispositions.EQUIVALENT, "Foo",
                                                             Dispositions.MOVED_TO_HANDWRITTEN, "Reason.", false);

        assertThatThrownBy(() -> new Dispositions(Collections.singletonList(review), Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is a retired reason");
    }

    private static final String AWS_JSON_1_1 = "aws.protocols#awsJson1_1";

    /** A legacy awsJson1_1 request case with id {@code Copied}. */
    private static ObjectNode legacyRequest(String input, String expectedBody) {
        return Node.objectNode()
                   .withMember("id", "Copied")
                   .withMember("description", "A copied case")
                   .withMember("given", Node.objectNode().withMember("input", Node.parse(input)))
                   .withMember("when", Node.objectNode().withMember("action", "marshall")
                                           .withMember("operation", "Op"))
                   .withMember("then", Node.objectNode().withMember("serializedAs", Node.objectNode()
                       .withMember("method", "POST")
                       .withMember("uri", "/")
                       .withMember("headers", Node.objectNode().withMember("contains", Node.objectNode()
                           .withMember("Content-Type", "application/x-amz-json-1.1")))
                       .withMember("body", Node.objectNode().withMember("jsonEquals", expectedBody))));
    }

    private static HttpRequestTestCase smithyRequest(String body) {
        return HttpRequestTestCase.builder()
                                  .id("Copied").protocol(ShapeId.from(AWS_JSON_1_1)).method("POST").uri("/")
                                  .putHeader("Content-Type", "application/x-amz-json-1.1")
                                  .body(body).bodyMediaType("application/json")
                                  .params(Node.parse("{\"name\": \"foo\"}").expectObjectNode())
                                  .build();
    }

    private static LegacyCase legacyCase(ObjectNode raw) {
        return new LegacyCase("x.json", 1, raw, new LegacySuites.Suite("x-suite.json", AWS_JSON_1_1, null, true));
    }

    /** The cases of a one-operation awsJson1_1 model (its request case {@code Copied} expects {@code body}). */
    private static List<SmithyCase> smithyCases(String body) {
        String model = "$version: \"2.0\"\n"
                       + "namespace com.example\n"
                       + "use aws.protocols#awsJson1_1\n"
                       + "use smithy.test#httpRequestTests\n"
                       + "@awsJson1_1\n"
                       + "service Svc { operations: [Op] }\n"
                       + "@httpRequestTests([{ id: \"Copied\", protocol: awsJson1_1, method: \"POST\", uri: \"/\",\n"
                       + "  headers: { \"Content-Type\": \"application/x-amz-json-1.1\" },\n"
                       + "  body: " + Node.printJson(Node.from(body)) + ", bodyMediaType: \"application/json\",\n"
                       + "  params: { name: \"foo\" } }])\n"
                       + "operation Op { input := { name: String } }\n";
        // Discovery also loads the upstream protocol test models, whose validation events are not this test's concern.
        Model assembled = Model.assembler(LegacyDispositionToolTest.class.getClassLoader())
                               .discoverModels(LegacyDispositionToolTest.class.getClassLoader())
                               .addUnparsedModel("test.smithy", model)
                               .assemble()
                               .getResult()
                               .get();
        return SmithyCase.loadAll(assembled, "com.example").stream()
                         .filter(c -> c.sdkOwned)
                         .collect(Collectors.toList());
    }

    /** An operation with no input and no output. */
    private static final class NoMemberOperation implements ShapeView.OperationView {
        @Override
        public String name() {
            return "NoMemberOperation";
        }

        @Override
        public ShapeView input() {
            return null;
        }

        @Override
        public ShapeView output() {
            return null;
        }

        @Override
        public Set<String> traitTokens() {
            return Collections.emptySet();
        }
    }
}
