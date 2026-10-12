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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.model.node.Node;

/**
 * The legacy suites and the protocol and C2J model each one runs against. The mapping comes from the legacy runner
 * classes in {@code test/protocol-tests/src/test/java/.../protocol/tests/*ProtocolTest.java}: each loads one suite and
 * one {@code /models/<uid>-intermediate.json}, and the uid identifies the {@code codegen-resources} directory.
 */
final class LegacySuites {

    /** One legacy suite. */
    static final class Suite {
        final String file;
        final String protocol;
        /** Directory under {@code codegen-resources} holding {@code service-2.json}, or null. */
        final String modelDir;
        /** False when no legacy runner class loads this suite. */
        final boolean run;

        Suite(String file, String protocol, String modelDir, boolean run) {
            this.file = file;
            this.protocol = protocol;
            this.modelDir = modelDir;
            this.run = run;
        }

        boolean httpBindings() {
            return Protocols.hasHttpBindings(protocol);
        }
    }

    static final Map<String, Suite> SUITES;

    static {
        Map<String, Suite> m = new LinkedHashMap<>();
        // apigateway-suite.json has had no runner class since 2.0.0-preview-1, and no API Gateway C2J model exists.
        put(m, new Suite("apigateway-suite.json", Protocols.REST_JSON, null, false));
        put(m, new Suite("ec2-suite.json", Protocols.EC2_QUERY, "ec2", true));
        put(m, new Suite("json-querycompatible-suite.json", Protocols.AWS_JSON_1_0, "json-querycompatible", true));
        put(m, new Suite("json10-suite.json", Protocols.AWS_JSON_1_0, "json10", true));
        put(m, new Suite("jsonrpc-suite.json", Protocols.AWS_JSON_1_1, "awsjson", true));
        // QueryProtocolTest loads awsquery-2020-01-08, which is the smithy-query model, not the older query model.
        put(m, new Suite("query-suite.json", Protocols.AWS_QUERY, "smithy-query", true));
        put(m, new Suite("restjson-contenttype-suite.json", Protocols.REST_JSON, "restjson/contenttype", true));
        put(m, new Suite("restjson-suite.json", Protocols.REST_JSON, "restjson", true));
        put(m, new Suite("restxml-suite.json", Protocols.REST_XML, "restxml", true));
        put(m, new Suite("smithy-rpcv2-nonquerycompatible-suite.json", Protocols.RPCV2_CBOR, "rpcv2-nonquerycompatible",
                         true));
        put(m, new Suite("smithy-rpcv2-querycompatible-suite.json", Protocols.RPCV2_CBOR, "rpcv2-querycompatible",
                         true));
        put(m, new Suite("smithy-rpcv2-suite.json", Protocols.RPCV2_CBOR, "rpcv2", true));
        SUITES = Collections.unmodifiableMap(m);
    }

    private LegacySuites() {
    }

    private static void put(Map<String, Suite> m, Suite s) {
        m.put(s.file, s);
    }

    /** Reads every {@code *-suite.json} and maps each case file to the suites that list it. */
    static Map<String, List<Suite>> suitesByCaseFile(Path suitesDir) throws IOException {
        Map<String, List<Suite>> out = new TreeMap<>();
        List<Path> suiteFiles = new ArrayList<>();
        try (java.util.stream.Stream<Path> s = Files.list(suitesDir)) {
            s.filter(p -> p.getFileName().toString().endsWith("-suite.json")).sorted().forEach(suiteFiles::add);
        }
        for (Path path : suiteFiles) {
            String name = path.getFileName().toString();
            Suite suite = SUITES.get(name);
            if (suite == null) {
                throw new IllegalStateException("Unknown legacy suite " + name + "; add it to LegacySuites.SUITES");
            }
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            for (Node n : Node.parseJsonWithComments(text, path.toString()).expectObjectNode()
                              .expectArrayMember("testCases").getElements()) {
                String caseFile = n.expectStringNode().getValue().replace("cases/", "");
                out.computeIfAbsent(caseFile, k -> new ArrayList<>()).add(suite);
            }
        }
        return out;
    }
}
