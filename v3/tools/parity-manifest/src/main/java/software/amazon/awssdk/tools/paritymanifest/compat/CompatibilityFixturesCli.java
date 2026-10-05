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

package software.amazon.awssdk.tools.paritymanifest.compat;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.tools.paritymanifest.ManifestJson;
import software.amazon.awssdk.tools.paritymanifest.ManifestValidationException;
import software.amazon.awssdk.tools.paritymanifest.ParityManifest;

/**
 * Generates the compatibility fixtures of a test scope from a Parity Manifest JSON document.
 *
 * <pre>
 * java -cp parity-manifest.jar:smithy-model.jar \
 *     software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixturesCli \
 *     --manifest &lt;manifest.json&gt; --baseline-classpath &lt;V2 jars and class dirs&gt; \
 *     --scope &lt;scope.txt&gt; --output &lt;fixtures.json&gt;
 * </pre>
 *
 * <p>{@code --baseline-classpath} uses the platform path separator and must hold the released V2 jars the manifest was
 * scanned from (every in-scope type and its supertypes). The scope file holds one entry ID or {@code *} glob per line
 * ({@link #parseScope}). Exit status is 0 on success, 1 when the manifest or fixture generation has problems (all
 * printed to standard error), and 2 on usage errors.
 */
public final class CompatibilityFixturesCli {
    private static final Set<String> OPTIONS = Set.of("--manifest", "--baseline-classpath", "--scope", "--output");

    private CompatibilityFixturesCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs the generator; returns the exit status. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!OPTIONS.contains(args[i]) || i + 1 >= args.length || options.put(args[i], args[++i]) != null) {
                err.println("Unknown, incomplete or repeated argument: " + args[i]);
                return 2;
            }
        }
        if (!options.keySet().equals(OPTIONS)) {
            err.println("--manifest, --baseline-classpath, --scope and --output are required");
            return 2;
        }
        try {
            ParityManifest manifest = ManifestJson.read(Path.of(options.get("--manifest")));
            List<Path> classPath = Arrays.stream(options.get("--baseline-classpath").split(File.pathSeparator))
                                         .filter(s -> !s.isBlank()).map(Path::of).toList();
            List<String> scope = parseScope(Files.readString(Path.of(options.get("--scope")), StandardCharsets.UTF_8));
            CompatibilityFixtures fixtures = new CompatibilityFixtureGenerator(manifest, BaselineClasses.read(classPath))
                .generate(scope);
            Path output = Path.of(options.get("--output"));
            fixtures.write(output);
            out.println("Wrote " + fixtures.fixtures().size() + " fixtures " + fixtures.summary() + " to " + output);
            return 0;
        } catch (ManifestValidationException e) {
            err.println(e.getMessage());
            return 1;
        } catch (CompatibilityFixtureException e) {
            err.println(e.getMessage());
            return 1;
        } catch (IOException | RuntimeException e) {
            err.println(e.getMessage());
            return 2;
        }
    }

    /**
     * Parses a scope document: one entry ID or {@code *} glob per line, or {@code !<glob>} to exclude the entries it
     * matches ({@link CompatibilityFixtureGenerator#generate}), blank lines ignored. Member IDs contain {@code #}, so
     * a comment starts only at a {@code #} that begins the line or follows whitespace.
     */
    public static List<String> parseScope(String text) {
        return text.lines().map(l -> l.replaceAll("(^|\\s)#.*", "").strip()).filter(l -> !l.isEmpty()).toList();
    }
}
