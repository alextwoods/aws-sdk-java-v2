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

package software.amazon.awssdk.test.fleet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The checked-in record of intentional V3-versus-V2 wire deltas ({@code wire-differential/intentional-deltas.json},
 * format version 1, documented in {@code wire-differential/README.md}; task 18.3 / Property 62 build on it).
 *
 * <p>Each record names the delta it covers ({@code case} and {@code aspect} globs over
 * {@code <service>/<operation>/<category>} and the aspect names of {@link Wire.Request#aspects()} and the response
 * observation) and the Parity Manifest entry that makes it intentional: {@code manifestEntry} (a Parity Manifest entry
 * ID such as {@code behavior:telemetry.user-agent}) and {@code rule} (the name of the classification rule of
 * {@code tools/parity-manifest/.../classification-rules.json} that classifies that entry). A record is valid only when
 * the rule exists, classifies {@code ADAPTED} or {@code EXCLUDED}, its {@code match.id} glob matches the entry, and
 * no more specific rule (more literal characters) matching the entry classifies it otherwise. A Parity Manifest
 * <em>known gap</em> is not a classification: a delta that only a known gap explains stays unclassified and fails.
 */
final class IntentionalDeltas {
    static final Path RULES = Fleet.V3_ROOT.resolve(
        "tools/parity-manifest/src/main/resources/software/amazon/awssdk/tools/paritymanifest/classification-rules.json");
    private static final Set<String> INTENTIONAL = Set.of("ADAPTED", "EXCLUDED");

    /** One record. */
    record Delta(String id, Pattern cases, Pattern aspect, String manifestEntry, String rule, String rationale) {
        boolean covers(String caseId, String aspectName) {
            return cases.matcher(caseId).matches() && aspect.matcher(aspectName).matches();
        }
    }

    private IntentionalDeltas() {
    }

    /** The records of the checked-in file, in file order. */
    static List<Delta> load() {
        ObjectNode root;
        try (InputStream in = IntentionalDeltas.class.getResourceAsStream("/wire-differential/intentional-deltas.json")) {
            if (in == null) {
                throw new AssertionError("wire-differential/intentional-deltas.json is not on the test class path");
            }
            root = Node.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)).expectObjectNode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (root.expectNumberMember("formatVersion").getValue().intValue() != 1) {
            throw new AssertionError("Unsupported intentional-deltas.json formatVersion");
        }
        List<Delta> result = new ArrayList<>();
        for (Node node : root.expectArrayMember("deltas").getElements()) {
            ObjectNode d = node.expectObjectNode();
            d.expectNoAdditionalProperties(Set.of("id", "case", "aspect", "manifestEntry", "rule", "rationale"));
            result.add(new Delta(d.expectStringMember("id").getValue(), glob(d.expectStringMember("case").getValue()),
                                 glob(d.expectStringMember("aspect").getValue()),
                                 d.expectStringMember("manifestEntry").getValue(),
                                 d.expectStringMember("rule").getValue(),
                                 d.expectStringMember("rationale").getValue()));
        }
        return result;
    }

    /** Every problem of {@code delta} against the checked-in classification rules (empty when it is valid). */
    static List<String> validate(Delta delta) {
        List<String> problems = new ArrayList<>();
        if (delta.rationale().isBlank()) {
            problems.add(delta.id() + ": no rationale");
        }
        ObjectNode rules = rules();
        Optional<ObjectNode> named = Optional.empty();
        List<ObjectNode> all = new ArrayList<>();
        rules.expectArrayMember("rules").getElements().forEach(n -> all.add(n.expectObjectNode()));
        for (ObjectNode rule : all) {
            if (rule.expectStringMember("name").getValue().equals(delta.rule())) {
                named = Optional.of(rule);
            }
        }
        if (named.isEmpty()) {
            problems.add(delta.id() + ": no classification rule named " + delta.rule());
            return problems;
        }
        String classification = named.get().expectStringMember("classification").getValue();
        if (!INTENTIONAL.contains(classification)) {
            problems.add(delta.id() + ": rule " + delta.rule() + " classifies " + classification
                         + ", not ADAPTED or EXCLUDED");
        }
        Optional<String> idGlob = named.get().expectObjectMember("match").getStringMember("id").map(s -> s.getValue());
        if (idGlob.isEmpty() || !glob(idGlob.get()).matcher(delta.manifestEntry()).matches()) {
            problems.add(delta.id() + ": rule " + delta.rule() + " does not match " + delta.manifestEntry());
            return problems;
        }
        int namedSpecificity = literal(idGlob.get());
        for (ObjectNode rule : all) {
            Optional<String> other = rule.expectObjectMember("match").getStringMember("id").map(s -> s.getValue());
            if (other.isPresent() && glob(other.get()).matcher(delta.manifestEntry()).matches()
                && literal(other.get()) > namedSpecificity
                && !INTENTIONAL.contains(rule.expectStringMember("classification").getValue())) {
                problems.add(delta.id() + ": the more specific rule " + rule.expectStringMember("name").getValue()
                             + " classifies " + delta.manifestEntry() + " "
                             + rule.expectStringMember("classification").getValue());
            }
        }
        return problems;
    }

    private static ObjectNode rules() {
        try {
            return Node.parse(Files.readString(RULES, StandardCharsets.UTF_8)).expectObjectNode();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the Parity Manifest classification rules " + RULES, e);
        }
    }

    private static int literal(String glob) {
        return glob.replace("*", "").length();
    }

    /** A glob ({@code *} matches any run of characters) as an anchored pattern. */
    static Pattern glob(String glob) {
        return Pattern.compile(java.util.Arrays.stream(glob.split("\\*", -1)).map(Pattern::quote)
                                               .collect(java.util.stream.Collectors.joining(".*")));
    }
}
