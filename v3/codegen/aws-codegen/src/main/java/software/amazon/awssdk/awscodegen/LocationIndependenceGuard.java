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

package software.amazon.awssdk.awscodegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;

/**
 * Rejects a staged generation, before publication, if any generated file contains the absolute path of a generation
 * input or output directory (Requirement 3.6): such a file would differ between checkouts and output roots.
 *
 * <p>Each location is matched in its normalized absolute form, its real-path form when it exists (for example
 * {@code /private/var/...} for {@code /var/...}), with {@code /} separators, and with Java-escaped backslashes.
 * Locations with fewer than two path elements are not matched, because their text is too short to identify a
 * checkout. Every offending file is reported with the contributors that produced it and its shape.
 */
final class LocationIndependenceGuard {

    private static final int MINIMUM_NAME_COUNT = 2;

    private LocationIndependenceGuard() {
    }

    /**
     * @throws AwsCodegenException with one {@link DiagnosticCode#ENVIRONMENT_DEPENDENT_OUTPUT} diagnostic per offending
     *         file
     */
    static void check(ContributionLedger ledger, List<StagedFileManifest> manifests, List<Path> locations) {
        Set<String> forbidden = forbiddenForms(locations);
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (StagedFileManifest manifest : manifests) {
            for (Path relative : manifest.relativeFiles()) {
                String content = new String(manifest.content(relative), StandardCharsets.UTF_8);
                List<String> found = forbidden.stream().filter(content::contains).toList();
                if (!found.isEmpty()) {
                    Path file = manifest.getBaseDir().resolve(relative);
                    String display = StagedFileManifest.portable(relative);
                    diagnostics.add(Diagnostic.forGeneration(
                        DiagnosticCode.ENVIRONMENT_DEPENDENT_OUTPUT, ledger.service(),
                        ledger.shapeOf(file, display).toString(), ledger.contributors(file), "output " + display,
                        "Generated file " + display + " contains the absolute generation path " + found.get(0)
                        + "; generated trees must be byte-identical for every checkout and output root."));
                }
            }
        }
        if (!diagnostics.isEmpty()) {
            diagnostics.sort(null);
            throw new AwsCodegenException("Generated output of service " + ledger.service() + " depends on its "
                                          + "checkout location (" + diagnostics.size() + " diagnostic(s)); no "
                                          + "Generated Service Package artifact was produced:", diagnostics);
        }
    }

    static Set<String> forbiddenForms(List<Path> locations) {
        Set<String> result = new TreeSet<>();
        for (Path location : locations) {
            Path absolute = location.toAbsolutePath().normalize();
            add(result, absolute);
            if (Files.exists(absolute)) {
                try {
                    add(result, absolute.toRealPath());
                } catch (IOException e) {
                    // The normalized absolute form is still checked.
                }
            }
        }
        return result;
    }

    private static void add(Set<String> forms, Path absolute) {
        if (absolute.getNameCount() < MINIMUM_NAME_COUNT) {
            return;
        }
        String text = absolute.toString();
        forms.add(text);
        forms.add(text.replace(absolute.getFileSystem().getSeparator(), "/"));
        if (text.contains("\\")) {
            forms.add(text.replace("\\", "\\\\"));
        }
    }
}
