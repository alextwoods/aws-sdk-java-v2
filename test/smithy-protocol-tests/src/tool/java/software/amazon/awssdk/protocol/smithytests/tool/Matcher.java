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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.node.Node;

/**
 * Finds the Smithy case equivalent to a legacy case. A Smithy equivalent (Requirement 32.10) is a case for the same
 * protocol with equivalent input or response that asserts the same properties. Matching tries, in order:
 * <ol>
 *     <li>{@link Strategy#ID}: the legacy case carries the Smithy case id (copied Smithy cases). The id alone decides
 *     the match; {@link ContentCheck} then compares the content and records any difference in
 *     {@link Result#contentDifferences}, so an edited copy is flagged. A legacy case with an id that a later strategy
 *     matches gets the same check.</li>
 *     <li>{@link Strategy#DOCUMENTATION}: same operation (or error shape) name and the legacy description equals the
 *     Smithy documentation.</li>
 *     <li>{@link Strategy#PARAMS}: same operation name and equal params.</li>
 *     <li>{@link Strategy#FEATURES}: the Smithy case whose behavior fingerprint ({@link FeatureExtractor}) covers the
 *     legacy fingerprint best. Accepted automatically only at full coverage with Jaccard similarity of at least
 *     {@link #MIN_JACCARD}; otherwise it is only a review candidate.</li>
 * </ol>
 */
final class Matcher {

    static final double MIN_JACCARD = 0.5;

    /** How a match was found. */
    enum Strategy {
        ID, DOCUMENTATION, PARAMS, FEATURES
    }

    /** A scored Smithy candidate. */
    static final class Candidate {
        final SmithyCase smithy;
        final double coverage;
        final double jaccard;
        final Set<String> missing;
        /** Words shared by the legacy description and the Smithy id and documentation; breaks score ties. */
        final int sharedWords;

        Candidate(SmithyCase smithy, Set<String> legacy, Set<String> legacyWords) {
            this.smithy = smithy;
            Set<String> words = words(smithy.id + " " + smithy.documentation);
            words.retainAll(legacyWords);
            this.sharedWords = words.size();
            Set<String> inter = new TreeSet<>(legacy);
            inter.retainAll(smithy.features);
            Set<String> union = new TreeSet<>(legacy);
            union.addAll(smithy.features);
            this.coverage = legacy.isEmpty() ? 0 : (double) inter.size() / legacy.size();
            this.jaccard = union.isEmpty() ? 0 : (double) inter.size() / union.size();
            this.missing = new TreeSet<>(legacy);
            this.missing.removeAll(smithy.features);
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s (coverage %.2f, jaccard %.2f, missing %s)", smithy.id, coverage,
                                 jaccard, missing);
        }
    }

    /** The result of matching one legacy case. */
    static final class Result {
        /** The accepted equivalent, or null. */
        final SmithyCase match;
        final Strategy strategy;
        /** Best feature candidates, for review output. */
        final List<Candidate> candidates;
        /** For a match of a legacy case that carries an id (a copy), how it differs from the match; otherwise empty. */
        final List<String> contentDifferences;

        Result(SmithyCase match, Strategy strategy, List<Candidate> candidates) {
            this(match, strategy, candidates, Collections.emptyList());
        }

        Result(SmithyCase match, Strategy strategy, List<Candidate> candidates, List<String> contentDifferences) {
            this.match = match;
            this.strategy = strategy;
            this.candidates = candidates;
            this.contentDifferences = contentDifferences;
        }
    }

    private final List<SmithyCase> smithyCases;

    Matcher(List<SmithyCase> smithyCases) {
        this.smithyCases = smithyCases;
    }

    /**
     * @param legacyFeatures the legacy fingerprint, or null when it cannot be computed (no C2J model)
     */
    Result match(LegacyCase legacy, Set<String> legacyFeatures) {
        String protocol = legacy.suite.protocol;
        String kind = legacy.kind();
        String shapeName = "error".equals(kind) && legacy.error != null ? legacy.error : legacy.operation;
        List<SmithyCase> sameKind = new ArrayList<>();
        for (SmithyCase c : smithyCases) {
            if (c.protocol.equals(protocol) && c.kind.equals(kind)) {
                sameKind.add(c);
            }
        }
        List<Candidate> candidates = rank(sameKind, legacyFeatures, words(legacy.description));

        if (legacy.id != null) {
            for (SmithyCase c : sameKind) {
                if (c.id.equals(legacy.id)) {
                    return new Result(c, Strategy.ID, candidates, ContentCheck.differences(legacy.raw, c.testCase));
                }
            }
        }
        List<SmithyCase> sameShape = new ArrayList<>();
        for (SmithyCase c : sameKind) {
            if (c.shapeName.equals(shapeName)) {
                sameShape.add(c);
            }
        }
        SmithyCase byDoc = null;
        for (SmithyCase c : sameShape) {
            if (!c.documentation.isEmpty() && c.documentation.equalsIgnoreCase(legacy.description)) {
                if (byDoc == null || paramsEqual(legacy, c)) {
                    byDoc = c;
                }
            }
        }
        if (byDoc != null) {
            // A legacy case that carries an id is a copy even when the id names no case of this protocol.
            List<String> differences = legacy.id == null ? Collections.emptyList()
                                                          : ContentCheck.differences(legacy.raw, byDoc.testCase);
            return new Result(byDoc, Strategy.DOCUMENTATION, candidates, differences);
        }
        List<SmithyCase> byParams = new ArrayList<>();
        for (SmithyCase c : sameShape) {
            if (paramsEqual(legacy, c)) {
                byParams.add(c);
            }
        }
        if (byParams.size() == 1) {
            return new Result(byParams.get(0), Strategy.PARAMS, candidates);
        }
        if (!candidates.isEmpty()) {
            Candidate best = candidates.get(0);
            if (best.coverage == 1.0 && best.jaccard >= MIN_JACCARD) {
                return new Result(best.smithy, Strategy.FEATURES, candidates);
            }
        }
        return new Result(null, null, candidates);
    }

    private static boolean paramsEqual(LegacyCase legacy, SmithyCase c) {
        Node params = legacy.params();
        return params.isObjectNode() && params.expectObjectNode().equals(c.params);
    }

    private static List<Candidate> rank(List<SmithyCase> cases, Set<String> legacyFeatures, Set<String> legacyWords) {
        List<Candidate> out = new ArrayList<>();
        if (legacyFeatures == null) {
            return out;
        }
        for (SmithyCase c : cases) {
            out.add(new Candidate(c, legacyFeatures, legacyWords));
        }
        out.sort(Comparator.comparingDouble((Candidate c) -> -c.coverage)
                           .thenComparingDouble(c -> -c.jaccard)
                           .thenComparingInt(c -> -c.sharedWords)
                           .thenComparing(c -> c.smithy.id));
        return out.size() > 3 ? new ArrayList<>(out.subList(0, 3)) : out;
    }

    /** Lower-case words of a description or a camel-case or snake-case id, without common filler words. */
    static Set<String> words(String text) {
        Set<String> out = new TreeSet<>();
        String split = text.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        for (String w : split.split("[^a-z0-9]+")) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) {
                out.add(w);
            }
        }
        return out;
    }

    private static final Set<String> STOP_WORDS = new TreeSet<>(java.util.Arrays.asList(
        "the", "and", "with", "are", "is", "that", "for", "correctly", "serializes", "serialized", "marshalled",
        "unmarshalled", "deserializes", "parses", "member", "members", "value", "values", "rest", "json", "xml",
        "aws", "ec2", "query", "tests"));
}
