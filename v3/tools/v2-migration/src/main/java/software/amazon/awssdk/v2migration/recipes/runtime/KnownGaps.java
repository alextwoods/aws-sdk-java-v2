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

package software.amazon.awssdk.v2migration.recipes.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * The known V3 launch gaps of the high-level-library parity matrix ({@code high-level-library-matrix.json} of the
 * {@code parity-manifest} release tooling, bundled into this artifact at build time so it cannot drift). A known gap
 * is a Parity Manifest entry that the manifest still classifies {@code PRESERVED} or {@code ADAPTED} but that the
 * candidate does not ship or has not verified, so the launch gate blocks it. Migration must therefore never treat a
 * gap's construct as preserved or rewrite it: every construct that joins a gap is left unchanged and reported.
 *
 * <p>Globs match entry IDs literally except that {@code *} matches any characters, as in the matrix.
 */
final class KnownGaps {

    /** Where the build bundles the matrix. */
    static final String RESOURCE = "/META-INF/aws-sdk-java-v3/v2-migration/high-level-library-matrix.json";

    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    /** One known gap of one library. */
    record Gap(String library, List<String> entries, String reason) {
        Gap {
            entries = List.copyOf(entries);
        }

        boolean matches(String id) {
            return entries.stream().anyMatch(g -> glob(g).matcher(id).matches());
        }
    }

    private final List<Gap> gaps;

    private KnownGaps(List<Gap> gaps) {
        this.gaps = List.copyOf(gaps);
    }

    /** The gaps of the matrix bundled with this artifact. */
    static KnownGaps bundled() {
        try (InputStream in = KnownGaps.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("The V2 migration tooling is packaged without " + RESOURCE
                                                + "; it cannot tell which V3 APIs are known launch gaps");
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The gaps of a matrix document; only the members this class reads are required. */
    static KnownGaps parse(String json) {
        List<Gap> gaps = new ArrayList<>();
        ObjectNode top = Node.parse(json).expectObjectNode();
        for (Node l : top.expectArrayMember("libraries").getElements()) {
            ObjectNode library = l.expectObjectNode();
            String name = library.expectStringMember("name").getValue();
            for (Node g : library.getArrayMember("knownGaps").map(ArrayNode::getElements).orElse(List.of())) {
                ObjectNode gap = g.expectObjectNode();
                List<String> entries = gap.expectArrayMember("entries").getElementsAs(StringNode.class).stream()
                                          .map(StringNode::getValue).toList();
                gaps.add(new Gap(name, entries, gap.expectStringMember("reason").getValue()));
            }
        }
        return new KnownGaps(gaps);
    }

    /** Every gap, in matrix order. */
    List<Gap> gaps() {
        return gaps;
    }

    /** The first gap matching the entry ID {@code id}, if any. */
    Optional<Gap> gapOf(String id) {
        return gaps.stream().filter(g -> g.matches(id)).findFirst();
    }

    private static Pattern glob(String glob) {
        return PATTERNS.computeIfAbsent(glob, g -> Pattern.compile(Pattern.quote(g).replace("*", "\\E.*\\Q")));
    }
}
