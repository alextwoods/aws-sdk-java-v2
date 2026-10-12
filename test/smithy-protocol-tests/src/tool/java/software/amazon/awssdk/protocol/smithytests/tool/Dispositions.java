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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Applies the design's D.1 disposition rules to a matched legacy case, with the hand-review decisions taking
 * precedence.
 *
 * <p>Rule order:
 * <ol>
 *     <li>A review entry for the row (protocol-specific, then file-wide) decides it.</li>
 *     <li>{@code api-gateway-*} cases are retired: API Gateway behavior is customization-driven, not a Supported_Protocol
 *     behavior, and no legacy runner loads {@code apigateway-suite.json}.</li>
 *     <li>An automatic match ({@link Matcher}) makes the row {@code equivalent}.</li>
 *     <li>Anything else is {@code unresolved}: the reviewer must port it to an SDK-owned Smithy case or retire it with
 *     a reason. The tool fails while unresolved rows remain.</li>
 * </ol>
 */
final class Dispositions {

    static final String EQUIVALENT = "equivalent";
    static final String PORTED = "ported";
    static final String RETIRED = "retired";
    static final String UNRESOLVED = "unresolved";

    /** Reason categories, used for the summary counts. */
    static final Set<String> CATEGORIES = new HashSet<>(java.util.Arrays.asList(
        "core", "copied-smithy", "apigateway", "contenttype", "querycompatible", "customization", "runtime-feature",
        "sdk-only-serialization", "unsupported-protocol-feature", "unresolved"));

    /** One CSV row. */
    static final class Row {
        final LegacyCase legacy;
        final String smithyId;
        final String disposition;
        final String category;
        final String reason;
        final boolean needsHumanReview;

        Row(LegacyCase legacy, String smithyId, String disposition, String category, String reason,
            boolean needsHumanReview) {
            this.legacy = legacy;
            this.smithyId = smithyId;
            this.disposition = disposition;
            this.category = category;
            this.reason = reason;
            this.needsHumanReview = needsHumanReview;
        }

        String dispositionColumn() {
            return PORTED.equals(disposition) ? PORTED + ":" + smithyId : disposition;
        }

        String smithyColumn() {
            return EQUIVALENT.equals(disposition) ? smithyId : "none";
        }

        String reasonColumn() {
            return "[" + category + "] " + reason + (needsHumanReview ? " (needs human review)" : "");
        }
    }

    /** A hand-review decision from the review file. */
    static final class Review {
        final String key;
        /** Null when the decision applies to the case under every suite protocol. */
        final String protocol;
        final String disposition;
        final String smithyId;
        final String category;
        final String reason;
        final boolean needsHumanReview;
        boolean used;

        Review(String key, String protocol, String disposition, String smithyId, String category, String reason,
               boolean needsHumanReview) {
            this.key = key;
            this.protocol = protocol;
            this.disposition = disposition;
            this.smithyId = smithyId;
            this.category = category;
            this.reason = reason;
            this.needsHumanReview = needsHumanReview;
        }
    }

    private final Map<String, Review> reviews = new HashMap<>();
    private final Map<String, SmithyCase> casesById = new HashMap<>();

    Dispositions(List<Review> reviews, List<SmithyCase> smithyCases) {
        for (Review r : reviews) {
            String k = reviewKey(r.key, r.protocol);
            if (this.reviews.put(k, r) != null) {
                throw new IllegalArgumentException("Duplicate review entry for " + k);
            }
            if (!CATEGORIES.contains(r.category)) {
                throw new IllegalArgumentException("Review " + k + " has unknown category " + r.category);
            }
            if (r.reason == null || r.reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Review " + k + " has an empty reason");
            }
        }
        for (SmithyCase c : smithyCases) {
            casesById.put(c.protocol + "|" + c.id, c);
        }
    }

    private static String reviewKey(String key, String protocol) {
        return protocol == null ? key : key + "@" + protocol;
    }

    Row decide(LegacyCase legacy, Matcher.Result result) {
        Review review = reviews.get(reviewKey(legacy.key(), legacy.suite.protocol));
        if (review == null) {
            review = reviews.get(legacy.key());
        }
        if (review != null) {
            review.used = true;
            return fromReview(legacy, review);
        }
        if (legacy.file.startsWith("api-gateway-")) {
            String closest = result.match == null ? "" : " Closest upstream case: " + result.match.id + ".";
            return new Row(legacy, null, RETIRED, "apigateway",
                           "SDK-specific API Gateway suite: customization-driven, not a Supported_Protocol behavior, "
                           + "and apigateway-suite.json has had no runner class since 2.0.0-preview-1." + closest,
                           false);
        }
        if (result.match != null) {
            String category = legacy.id != null ? "copied-smithy" : "core";
            String how = result.strategy == Matcher.Strategy.FEATURES
                         ? String.format(Locale.ROOT, "behavior fingerprint, coverage 1.00, jaccard %.2f",
                                         result.candidates.get(0).jaccard)
                         : result.strategy.name().toLowerCase(Locale.ROOT) + " match";
            // A fingerprint match is coarse (it sees member kinds and traits, not values), so it stays flagged until a
            // review entry confirms or corrects it.
            boolean flagged = result.strategy == Matcher.Strategy.FEATURES;
            return new Row(legacy, result.match.id, EQUIVALENT, category, "Automatic " + how + ".", flagged);
        }
        return new Row(legacy, null, UNRESOLVED, "unresolved", unresolvedHint(legacy), true);
    }

    private Row fromReview(LegacyCase legacy, Review review) {
        String where = "review of " + legacy.key() + " (" + legacy.suite.protocol + ")";
        switch (review.disposition) {
            case EQUIVALENT:
                SmithyCase eq = expectCase(legacy.suite.protocol, review.smithyId, where);
                if (eq.sdkOwned) {
                    throw new IllegalArgumentException(where + ": equivalent must name an upstream case; use ported");
                }
                break;
            case PORTED:
                SmithyCase ported = expectCase(legacy.suite.protocol, review.smithyId, where);
                if (!ported.sdkOwned) {
                    throw new IllegalArgumentException(where + ": ported must name an SDK-owned case");
                }
                break;
            case RETIRED:
                break;
            default:
                throw new IllegalArgumentException(where + ": unknown disposition " + review.disposition);
        }
        return new Row(legacy, review.smithyId, review.disposition, review.category, review.reason,
                       review.needsHumanReview);
    }

    private SmithyCase expectCase(String protocol, String id, String where) {
        SmithyCase c = id == null ? null : casesById.get(protocol + "|" + id);
        if (c == null) {
            throw new IllegalArgumentException(where + ": no " + protocol + " Smithy case with id " + id);
        }
        return c;
    }

    private static String unresolvedHint(LegacyCase legacy) {
        if (legacy.file.startsWith("rest-json-contenttype")) {
            return "No upstream equivalent; port to software.amazon.awssdk.protocoltests#RestJsonContentType.";
        }
        if (legacy.file.contains("querycompatible")) {
            return "No upstream awsQueryCompatible equivalent; port as an SDK-owned case.";
        }
        return "No automatic match; review and port or retire.";
    }

    /** Review entries that matched no row; stale entries are an error. */
    List<String> unusedReviews() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Review> e : reviews.entrySet()) {
            if (!e.getValue().used) {
                out.add(e.getKey());
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    static List<Review> loadReviews(Path file) throws IOException {
        List<Review> out = new ArrayList<>();
        if (file == null || !Files.exists(file)) {
            return out;
        }
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        for (Node n : Node.parseJsonWithComments(text, file.toString()).expectObjectNode()
                          .expectArrayMember("reviews").getElements()) {
            ObjectNode o = n.expectObjectNode();
            out.add(new Review(o.expectStringMember("key").getValue(),
                               o.getStringMember("protocol").map(StringNode::getValue).orElse(null),
                               o.expectStringMember("disposition").getValue(),
                               o.getStringMember("smithyId").map(StringNode::getValue).orElse(null),
                               o.expectStringMember("category").getValue(),
                               o.expectStringMember("reason").getValue(),
                               o.getBooleanMemberOrDefault("needsHumanReview")));
        }
        return out;
    }
}
