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

package software.amazon.awssdk.test.v2migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport.StepResult;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationResult;

/**
 * Compiles a transformed corpus project the way its migrated POM builds it (Requirement 17.7): the SDK artifacts and
 * versions the POM selects must be candidate release artifacts at the candidate version (directly or through the
 * imported V3 BOM), the Java release must be at least 21, and the sources must compile against exactly the run-time
 * closure of those artifacts.
 *
 * <p>A failure is attributed to the candidate ({@link software.amazon.awssdk.tools.releaseevidence.CompatibilityReport
 * .Status#BLOCKED_BY_CANDIDATE}) only when every cause is a Parity Manifest entry the candidate does not ship: a
 * selected artifact the V3 BOM does not manage, or a compiler error inside a migration item whose entry (for a
 * transformed item, its replacement) no candidate artifact ships. Any other failure is the migration's
 * ({@code FAILED}).
 */
record CandidateCompile(StepResult result, Path classes, List<Path> classpath) {
    static final int V3_MINIMUM_JAVA_RELEASE = 21;

    static CandidateCompile compile(Path root, PomFacts pom, MigrationResult migration, Corpus corpus, Path out)
        throws IOException {
        List<String> failures = new ArrayList<>();
        Set<String> blockedBy = new TreeSet<>();
        int release = pom.javaRelease();
        if (release < V3_MINIMUM_JAVA_RELEASE) {
            failures.add("The migrated POM compiles for Java " + release + ", below the V3 minimum Java "
                         + V3_MINIMUM_JAVA_RELEASE);
        }
        Optional<String> bom = pom.bomVersion();
        if (bom.isPresent() && !bom.get().equals(Corpus.CANDIDATE_VERSION)) {
            failures.add("The migrated POM imports software.amazon.awssdk:bom:" + bom.get() + ", not the candidate "
                         + Corpus.CANDIDATE_VERSION);
        }
        Set<String> roots = new LinkedHashSet<>();
        for (PomFacts.Dependency d : pom.dependencies()) {
            if ("test".equals(d.scope())) {
                continue;
            }
            String version = d.version() != null ? d.version() : bom.orElse(null);
            if (version == null) {
                failures.add("The migrated POM selects " + d.key() + " without a version or an SDK BOM import");
            } else if (!version.equals(Corpus.CANDIDATE_VERSION)) {
                failures.add("The migrated POM selects " + d.key() + ":" + version + ", not the candidate version "
                             + Corpus.CANDIDATE_VERSION);
            }
            if (!corpus.bomArtifacts().contains(d.key())) {
                blockedBy.add("artifact:" + d.key());
                failures.add("The migrated POM selects " + d.key() + ", which is not a release artifact of the "
                             + "candidate (the V3 BOM does not manage it)");
                continue;
            }
            if (!corpus.candidate().has(d.key())) {
                throw new AssertionError("The migrated POM selects the candidate artifact " + d.key() + "; declare it "
                                         + "as a test dependency of v2-migration-tests so this build resolves it");
            }
            roots.add(d.key());
        }
        List<Path> classpath = corpus.candidate().classpath(roots);
        boolean blockedOnly = failures.size() == blockedBy.size();
        if (!blockedOnly) {
            return new CandidateCompile(StepResult.failed("The migrated POM does not select the candidate", failures),
                                        null, classpath);
        }
        JavaCompilation.Result compiled = JavaCompilation.compile(root, release, classpath, out);
        List<String> unattributed = new ArrayList<>();
        for (JavaCompilation.Error error : compiled.errors()) {
            Optional<String> missing = unshipped(migration, error, corpus);
            if (missing.isPresent()) {
                blockedBy.add(missing.get());
                failures.add(error + " [candidate does not ship " + missing.get() + "]");
            } else {
                unattributed.add(error.toString());
            }
        }
        if (!unattributed.isEmpty()) {
            List<String> all = new ArrayList<>(unattributed);
            all.addAll(failures);
            return new CandidateCompile(StepResult.failed(
                "The transformed project does not compile for Java " + release + " against the candidate ("
                + unattributed.size() + " errors not caused by an entry the candidate does not ship)", all),
                                        null, classpath);
        }
        if (!blockedBy.isEmpty()) {
            return new CandidateCompile(StepResult.blocked(
                "The transformed project needs " + blockedBy.size() + " Parity Manifest entries the candidate does not "
                + "ship", List.copyOf(blockedBy), failures), null, classpath);
        }
        return new CandidateCompile(StepResult.passed(
            "Compiled for Java " + release + " against the candidate (" + String.join(", ", roots) + " and "
            + (classpath.size() - roots.size()) + " transitive artifacts)"), compiled.classes(), classpath);
    }

    /** The entry the candidate does not ship that the innermost item around {@code error} uses, if any. */
    private static Optional<String> unshipped(MigrationResult migration, JavaCompilation.Error error, Corpus corpus)
        throws IOException {
        List<ItemResult> items = new ArrayList<>();
        String text = migration.changed().getOrDefault(error.path(), migration.originals().get(error.path()));
        int position = (int) error.position();
        if (text != null && position >= 0 && position + 1 < text.length() && text.charAt(position) == '.') {
            // javac reports an unknown member of a call chain at the dot that selects it; the member's own item
            // starts after the dot.
            items.addAll(CorpusRun.itemsAt(migration, error.path(), position + 1));
        }
        items.addAll(CorpusRun.itemsAt(migration, error.path(), position));
        for (ItemResult r : items) {
            String id = r.disposition() == Disposition.TRANSFORMED && r.entry() != null
                        && r.entry().replacement() != null ? r.entry().replacement() : r.item().id();
            if (id != null && !corpus.ships(id)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
