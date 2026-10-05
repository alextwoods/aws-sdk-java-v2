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

package software.amazon.awssdk.tools.paritymanifest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The registered verification evidence hooks of a release candidate: every known evidence ID (a test suite, generated
 * compatibility fixture or other verification that can produce candidate evidence) and the manifest entries each one
 * verifies in addition to the entries whose {@code evidenceIds} name it. Generated per-entry verifications (for
 * example the compile, linkage and behavior fixtures generated from the manifest) bind themselves here, so the manifest
 * does not have to list them.
 *
 * <p>Text form ({@link #parse}): one hook per line, {@code <evidenceId> [<entryId> ...]}, whitespace-separated;
 * blank lines are ignored, and so is a comment: a {@code #} that begins the line or follows whitespace, and the rest
 * of the line (member entry IDs contain {@code #}, so a {@code #} inside a token is not a comment). A hook may appear
 * on several lines. {@code CompatibilityFixtures.evidenceHooks} emits this form.
 */
public final class EvidenceHooks {
    /** No registered hooks; evidence IDs are then only checked for presence, not registration. */
    public static final EvidenceHooks NONE = new EvidenceHooks(null);

    private final SortedMap<String, SortedSet<String>> hooks;
    private final Map<String, SortedSet<String>> byEntry = new HashMap<>();

    /**
     * @param hooks entry IDs bound to each known evidence ID (possibly none), or {@code null} for no registry
     */
    public EvidenceHooks(Map<String, ? extends Iterable<String>> hooks) {
        if (hooks == null) {
            this.hooks = null;
            return;
        }
        SortedMap<String, SortedSet<String>> copy = new TreeMap<>();
        hooks.forEach((evidence, entries) -> {
            SortedSet<String> bound = new TreeSet<>();
            entries.forEach(bound::add);
            copy.put(evidence, Collections.unmodifiableSortedSet(bound));
            bound.forEach(entry -> byEntry.computeIfAbsent(entry, k -> new TreeSet<>()).add(evidence));
        });
        this.hooks = Collections.unmodifiableSortedMap(copy);
    }

    /** The registry, or {@code null} when none was supplied. */
    public SortedMap<String, SortedSet<String>> hooks() {
        return hooks;
    }

    /** True if a hook registry was supplied, so evidence IDs must resolve in it. */
    public boolean registered() {
        return hooks != null;
    }

    /** True if {@code evidenceId} is a registered hook. */
    public boolean known(String evidenceId) {
        return hooks != null && hooks.containsKey(evidenceId);
    }

    /** The evidence hooks of {@code entry}: its manifest {@code evidenceIds} plus the hooks bound to its ID. */
    public SortedSet<String> of(ManifestEntry entry) {
        SortedSet<String> result = new TreeSet<>(entry.evidenceIds());
        result.addAll(byEntry.getOrDefault(entry.id(), Collections.emptySortedSet()));
        return result;
    }

    /** Parses the text form; {@code origin} names the document in errors. */
    public static EvidenceHooks parse(String text, String origin) {
        Map<String, SortedSet<String>> hooks = new TreeMap<>();
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = stripComment(lines[i]);
            if (line.isEmpty()) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            SortedSet<String> entries = hooks.computeIfAbsent(tokens[0], k -> new TreeSet<>());
            for (int t = 1; t < tokens.length; t++) {
                if (EntryKind.ofId(tokens[t]).isEmpty()) {
                    throw new IllegalArgumentException(origin + ":" + (i + 1) + ": '" + tokens[t]
                                                       + "' is not a well-formed Parity Manifest entry ID");
                }
                entries.add(tokens[t]);
            }
        }
        return new EvidenceHooks(hooks);
    }

    /** These hooks and {@code other}'s; registered if either is. */
    public EvidenceHooks merge(EvidenceHooks other) {
        if (!registered() && !other.registered()) {
            return NONE;
        }
        Map<String, SortedSet<String>> merged = new TreeMap<>();
        for (EvidenceHooks h : List.of(this, other)) {
            if (h.registered()) {
                h.hooks.forEach((evidence, entries) -> merged.computeIfAbsent(evidence, k -> new TreeSet<>())
                                                             .addAll(entries));
            }
        }
        return new EvidenceHooks(merged);
    }

    /**
     * {@code line} without its comment and surrounding whitespace: a comment starts at a {@code #} that begins the
     * line or follows whitespace, because entry IDs of members contain {@code #}.
     */
    static String stripComment(String line) {
        return line.replaceAll("(^|\\s)#.*", "").strip();
    }
}
