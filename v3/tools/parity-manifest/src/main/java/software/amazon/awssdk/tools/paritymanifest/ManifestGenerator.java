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

import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.DUPLICATE_ID;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.INVALID_ID;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.INVENTORY;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Combines inventory sources, classification rules and the baseline identity into a validated
 * {@link ParityManifest}. Every problem (source failures, malformed and duplicate IDs, unmatched and conflicting
 * items, required-field violations, unknown replacements, incomplete frozen baseline) is reported at once.
 */
public final class ManifestGenerator {
    private final List<InventorySource> sources;
    private final ClassificationRules rules;

    public ManifestGenerator(List<InventorySource> sources, ClassificationRules rules) {
        this.sources = List.copyOf(sources);
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    /**
     * Scans every source against {@code repositoryRoot} and generates the manifest.
     *
     * @param candidate the candidate identity, or {@code null}
     */
    public ParityManifest generate(Path repositoryRoot, BaselineIdentity baseline, CandidateIdentity candidate,
                                   ManifestValidator.Options options) throws ManifestValidationException {
        return generate(repositoryRoot, baseline, candidate, options, Map.of());
    }

    /**
     * Scans every source against {@code repositoryRoot} with scanner {@code scanOptions} (see
     * {@link InventorySource.Context#options()}) and generates the manifest.
     *
     * @param candidate the candidate identity, or {@code null}
     */
    public ParityManifest generate(Path repositoryRoot, BaselineIdentity baseline, CandidateIdentity candidate,
                                   ManifestValidator.Options options, Map<String, String> scanOptions)
        throws ManifestValidationException {
        List<ManifestProblem> problems = new ArrayList<>();
        if (sources.isEmpty()) {
            problems.add(new ManifestProblem(INVENTORY, "$", "no inventory source supplied"));
        }
        InventorySource.Context context = new InventorySource.Context(repositoryRoot, baseline, scanOptions);
        Map<String, List<String>> sourcesById = new TreeMap<>();
        Map<String, InventoryItem> items = new LinkedHashMap<>();
        for (InventorySource source : sources.stream().sorted(Comparator.comparing(InventorySource::name)).toList()) {
            List<InventoryItem> scanned;
            try {
                scanned = source.scan(context);
            } catch (IOException | RuntimeException e) {
                problems.add(new ManifestProblem(INVENTORY, "source:" + source.name(), "scan failed: " + e));
                continue;
            }
            for (InventoryItem item : scanned) {
                if (!item.kind().accepts(item.id())) {
                    // Excluded from classification, so the problem is reported once.
                    problems.add(new ManifestProblem(INVALID_ID, item.id(), "source " + source.name()
                                                                            + " reported an ID that does not follow "
                                                                            + "the " + item.kind() + " ID scheme"));
                    continue;
                }
                sourcesById.computeIfAbsent(item.id(), k -> new ArrayList<>()).add(source.name());
                items.putIfAbsent(item.id(), item);
            }
        }
        sourcesById.forEach((id, names) -> {
            if (names.size() > 1) {
                problems.add(new ManifestProblem(DUPLICATE_ID, id, "reported " + names.size() + " times, by "
                                                                   + String.join(", ", new TreeSet<>(names))));
                items.remove(id);
            }
        });
        ClassificationRules.Classified classified = rules.classify(List.copyOf(items.values()));
        problems.addAll(classified.problems());
        ParityManifest manifest = new ParityManifest(baseline, candidate, classified.entries());
        for (ManifestProblem p : ManifestValidator.validate(manifest, options)) {
            String rule = classified.ruleByEntry().get(p.subject());
            problems.add(rule == null ? p : new ManifestProblem(p.code(), p.subject(),
                                                                p.message() + " (classified by rule '" + rule + "')"));
        }
        ManifestValidationException.throwIfAny(problems);
        return manifest;
    }
}
