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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/** One case from a legacy {@code cases/*.json} file, run under one legacy suite (one protocol). */
final class LegacyCase {

    final String file;
    /** 1-based position of the case in its file. */
    final int index;
    final String description;
    final String id;
    final String action;
    final String operation;
    final String error;
    final ObjectNode raw;
    final LegacySuites.Suite suite;

    private LegacyCase(String file, int index, ObjectNode raw, LegacySuites.Suite suite) {
        this.file = file;
        this.index = index;
        this.raw = raw;
        this.suite = suite;
        this.description = normalize(raw.getStringMemberOrDefault("description", ""));
        this.id = raw.getStringMember("id").map(StringNode::getValue).orElse(null);
        ObjectNode when = raw.expectObjectMember("when");
        this.action = when.expectStringMember("action").getValue();
        this.operation = when.expectStringMember("operation").getValue();
        this.error = when.getStringMember("error").map(StringNode::getValue).orElse(null);
    }

    /** Row key used by the CSV and the review file: {@code <file>#<index>}. */
    String key() {
        return file + "#" + index;
    }

    String kind() {
        switch (action) {
            case "marshall":
                return "request";
            case "unmarshall":
                return "response";
            case "errorUnmarshall":
                return "error";
            default:
                throw new IllegalStateException("Unknown legacy action " + action + " in " + key());
        }
    }

    ObjectNode given() {
        return raw.getObjectMember("given").orElse(Node.objectNode());
    }

    ObjectNode then() {
        return raw.expectObjectMember("then");
    }

    /** Input params (marshall) or expected output (unmarshall). */
    Node params() {
        if ("request".equals(kind())) {
            return given().getMember("input").orElse(Node.objectNode());
        }
        return then().getMember("deserializedAs").orElse(Node.objectNode());
    }

    /**
     * Request facts. The HTTP method is a fact only when the case asserts it: legacy models often use a different
     * method than the Smithy model for the same behavior, and a case that does not assert the method does not test it.
     */
    FeatureExtractor.RequestFacts requestFacts() {
        FeatureExtractor.RequestFacts facts = new FeatureExtractor.RequestFacts();
        ObjectNode serialized = then().getObjectMember("serializedAs").orElse(Node.objectNode());
        facts.method = serialized.getStringMember("method").map(StringNode::getValue).orElse(null);
        ObjectNode headers = serialized.getObjectMember("headers").orElse(Node.objectNode());
        headers.getObjectMember("contains").ifPresent(c -> facts.assertedHeaders.addAll(c.getStringMap().keySet()));
        facts.assertedHeaders.addAll(strings(headers.getArrayMember("mustContain").orElse(null)));
        facts.forbiddenHeaders.addAll(strings(headers.getArrayMember("doesNotContain").orElse(null)));
        facts.assertsHost = serialized.containsMember("resolvedHost") || given().containsMember("host");
        ObjectNode body = serialized.getObjectMember("body").orElse(null);
        if (body != null) {
            for (Map.Entry<String, Node> e : body.getStringMap().entrySet()) {
                facts.body = e.getValue().asStringNode().map(StringNode::getValue).orElse(null);
            }
        }
        return facts;
    }

    FeatureExtractor.ResponseFacts responseFacts() {
        FeatureExtractor.ResponseFacts facts = new FeatureExtractor.ResponseFacts();
        ObjectNode response = given().getObjectMember("response").orElse(Node.objectNode());
        facts.statusCode = response.getNumberMember("status_code").map(n -> n.getValue().intValue()).orElse(200);
        response.getObjectMember("headers").ifPresent(h -> {
            for (Map.Entry<String, Node> e : h.getStringMap().entrySet()) {
                Node v = e.getValue();
                facts.headers.put(e.getKey(), v.isArrayNode() ? String.join(",", strings(v.expectArrayNode()))
                                                              : v.expectStringNode().getValue());
            }
        });
        if (response.containsMember("binaryBody")) {
            facts.binary = !response.expectStringMember("binaryBody").getValue().isEmpty();
        } else {
            facts.body = response.getStringMember("body").map(StringNode::getValue).orElse(null);
        }
        return facts;
    }

    private static List<String> strings(ArrayNode array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (Node n : array.getElements()) {
            out.add(n.expectStringNode().getValue());
        }
        return out;
    }

    static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /** Loads every case of every case file under {@code suitesDir/cases}, once per suite that runs the file. */
    static List<LegacyCase> loadAll(Path suitesDir) throws IOException {
        Map<String, List<LegacySuites.Suite>> suitesByFile = LegacySuites.suitesByCaseFile(suitesDir);
        List<LegacyCase> out = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        try (java.util.stream.Stream<Path> s = Files.list(suitesDir.resolve("cases"))) {
            s.filter(p -> p.toString().endsWith(".json")).sorted().forEach(files::add);
        }
        for (Path path : files) {
            String name = path.getFileName().toString();
            List<LegacySuites.Suite> suites = suitesByFile.get(name);
            if (suites == null) {
                throw new IllegalStateException("Case file " + name + " is not listed by any suite");
            }
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            List<Node> cases = Node.parseJsonWithComments(text, path.toString()).expectArrayNode().getElements();
            for (LegacySuites.Suite suite : suites) {
                for (int i = 0; i < cases.size(); i++) {
                    out.add(new LegacyCase(name, i + 1, cases.get(i).expectObjectNode(), suite));
                }
            }
        }
        return out;
    }

    /** Number of distinct case files. */
    static int fileCount(List<LegacyCase> cases) {
        Set<String> files = new java.util.TreeSet<>();
        for (LegacyCase c : cases) {
            files.add(c.file);
        }
        return files.size();
    }
}
