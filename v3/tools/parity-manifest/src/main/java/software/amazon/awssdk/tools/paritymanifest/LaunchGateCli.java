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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Runs the {@link LaunchGate} on a Parity Manifest and a V3 release candidate.
 *
 * <pre>
 * java -cp parity-manifest.jar:smithy-model.jar software.amazon.awssdk.tools.paritymanifest.LaunchGateCli \
 *     --manifest &lt;manifest.json&gt; [--stage packaging|general-availability]
 *     [--candidate-tree &lt;v3 dir&gt;] [--candidate &lt;jar or class directory&gt;]...
 *     [--declared &lt;id list&gt;]... [--builtin-hooks] [--hooks &lt;hooks.txt&gt;]...
 *     [--evidence &lt;candidate-evidence.json&gt;] [--report &lt;report.json&gt;] [--report-text &lt;report.txt&gt;]
 * </pre>
 *
 * <p>{@code --candidate-tree} names a built V3 source tree: the candidate is the built output (packaged jar, else
 * compiled classes) of every release artifact its {@code bom/pom.xml} manages plus the POM-only BOMs
 * ({@link CandidateTree}), every BOM artifact without output is {@link LaunchProblem.Code#CANDIDATE_INCOMPLETE}, and
 * evidence hooks must name test classes of the tree.
 * {@code --candidate} adds jars or class directories. {@code --declared} files list capability IDs shipped outside
 * any scanned artifact ({@link CandidateInventoryScanner#readIdList}). {@code --builtin-hooks} registers the
 * checked-in hook registry ({@value #BUILTIN_HOOKS}) and {@code --hooks} files register more evidence hooks
 * ({@link EvidenceHooks#parse}); without either, evidence IDs are only checked for presence.
 *
 * <p>Exit status: 0 when nothing blocks the stage, 1 when the gate blocks it (a summary and the first problems are
 * printed to standard error; the reports hold all of them), 2 on usage or input errors.
 */
public final class LaunchGateCli {
    /** Problems printed to standard error; the reports always hold every problem. */
    static final int PRINTED_PROBLEMS = 200;
    /** The classpath location of the checked-in evidence hook registry ({@code --builtin-hooks}). */
    public static final String BUILTIN_HOOKS = "/software/amazon/awssdk/tools/paritymanifest/launch-evidence-hooks.txt";

    private static final Set<String> SINGLE = Set.of("--manifest", "--stage", "--candidate-tree", "--evidence",
                                                     "--report", "--report-text");
    private static final Set<String> REPEATED = Set.of("--candidate", "--declared", "--hooks");

    private LaunchGateCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs the gate; returns the exit status. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Map<String, List<String>> options = new HashMap<>();
        boolean builtinHooks = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--builtin-hooks")) {
                builtinHooks = true;
                continue;
            }
            boolean single = SINGLE.contains(args[i]);
            if ((!single && !REPEATED.contains(args[i])) || i + 1 >= args.length
                || (single && options.containsKey(args[i]))) {
                err.println("Unknown, incomplete or repeated argument: " + args[i]);
                return 2;
            }
            options.computeIfAbsent(args[i], k -> new ArrayList<>()).add(args[++i]);
        }
        if (!options.containsKey("--manifest")) {
            err.println("--manifest is required");
            return 2;
        }
        try {
            LaunchGate.Stage stage = options.containsKey("--stage")
                                     ? LaunchGate.Stage.fromCliName(options.get("--stage").get(0))
                                     : LaunchGate.Stage.PACKAGING;
            ParityManifest manifest = ManifestJson.read(Path.of(options.get("--manifest").get(0)));
            List<Path> roots = new ArrayList<>();
            LaunchGate.Builder gate = LaunchGate.builder().manifest(manifest);
            if (options.containsKey("--candidate-tree")) {
                Path tree = Path.of(options.get("--candidate-tree").get(0));
                SortedSet<String> expected = CandidateTree.bomArtifacts(tree.resolve("bom/pom.xml"));
                roots.addAll(CandidateTree.moduleOutputs(tree, expected).values());
                gate.expectedArtifacts(expected).availableEvidence(CandidateTree.testClasses(tree));
            }
            options.getOrDefault("--candidate", List.of()).forEach(c -> roots.add(Path.of(c)));
            List<String> declared = new ArrayList<>();
            if (options.containsKey("--candidate-tree")) {
                CandidateTree.pomArtifacts(Path.of(options.get("--candidate-tree").get(0)))
                             .forEach(a -> declared.add(EntryKind.ARTIFACT.prefix() + a));
            }
            for (String file : options.getOrDefault("--declared", List.of())) {
                declared.addAll(CandidateInventoryScanner.readIdList(read(file), file));
            }
            EvidenceHooks hooks = builtinHooks ? builtinHooks() : EvidenceHooks.NONE;
            for (String file : options.getOrDefault("--hooks", List.of())) {
                hooks = hooks.merge(EvidenceHooks.parse(read(file), file));
            }
            if (options.containsKey("--evidence")) {
                gate.evidence(CandidateEvidence.parse(read(options.get("--evidence").get(0))));
            }
            LaunchGateReport report = gate.candidate(CandidateInventoryScanner.scan(roots, declared)).hooks(hooks)
                                          .build().evaluate(stage);
            if (options.containsKey("--report")) {
                write(Path.of(options.get("--report").get(0)), report.toJson());
            }
            if (options.containsKey("--report-text")) {
                write(Path.of(options.get("--report-text").get(0)), report.render());
            }
            (report.passed() ? out : err).print(report.render(PRINTED_PROBLEMS));
            return report.passed() ? 0 : 1;
        } catch (ManifestValidationException | IOException | RuntimeException e) {
            err.println(e.getMessage());
            return 2;
        }
    }

    /** The checked-in hook registry of the V3 release candidate ({@value #BUILTIN_HOOKS}). */
    public static EvidenceHooks builtinHooks() throws IOException {
        try (InputStream in = LaunchGateCli.class.getResourceAsStream(BUILTIN_HOOKS)) {
            if (in == null) {
                throw new IOException("Missing " + BUILTIN_HOOKS);
            }
            return EvidenceHooks.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), BUILTIN_HOOKS);
        }
    }

    private static String read(String file) throws IOException {
        return Files.readString(Path.of(file), StandardCharsets.UTF_8);
    }

    static void write(Path file, String text) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
