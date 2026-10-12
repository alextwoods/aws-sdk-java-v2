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

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

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
