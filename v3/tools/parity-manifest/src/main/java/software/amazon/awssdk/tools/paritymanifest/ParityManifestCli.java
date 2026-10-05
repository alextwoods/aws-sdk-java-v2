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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generates the Parity Manifest from the V2 repository and writes its canonical JSON.
 *
 * <pre>
 * java -cp parity-manifest.jar:smithy-model.jar:&lt;scanners&gt; \
 *     software.amazon.awssdk.tools.paritymanifest.ParityManifestCli \
 *     --repository &lt;V2 repo root&gt; --output &lt;manifest.json&gt;
 *     [--rules &lt;rules.json&gt;] [--source &lt;name&gt;]... [--git-revision &lt;sha&gt;]
 *     [--feature-freeze-cutoff &lt;yyyy-MM-dd&gt;] [--support-status &lt;text&gt;] [--require-frozen-baseline]
 *     [--candidate-version &lt;v&gt;] [--candidate-git-revision &lt;sha&gt;] [--candidate-ids &lt;file&gt;]
 *     [--api-modules &lt;a,b,...|all&gt;] [--api-version &lt;2.x.y&gt;] [--maven-repository &lt;dir&gt;]
 * </pre>
 *
 * <p>Inventory sources are discovered with {@link ServiceLoader}; {@code --source} restricts them by name. The V2
 * version is the root {@code pom.xml} version; the git revision defaults to the repository's {@code HEAD}. Rules
 * default to the checked-in {@link ClassificationRules#DEFAULT_RESOURCE}. {@code --candidate-ids} names a file of
 * candidate inventory IDs, one per line, against which replacements are resolved.
 * {@code --api-modules}, {@code --api-version} and {@code --maven-repository} configure
 * {@link PublicApiInventorySource} (default: its bounded {@link PublicApiInventorySource#DEFAULT_MODULES}; {@code all}
 * scans every published V2 jar module). Exit status is 0 on success, 1
 * when the manifest has problems (all printed to standard error), and 2 on usage errors.
 */
public final class ParityManifestCli {
    private static final String MODULE_PROPERTIES = "/META-INF/aws-sdk-java-v3/modules/parity-manifest.properties";
    private static final Set<String> FLAGS = Set.of("--require-frozen-baseline");
    private static final Set<String> OPTIONS = Set.of("--repository", "--output", "--rules", "--source",
                                                      "--git-revision", "--feature-freeze-cutoff",
                                                      "--support-status", "--candidate-version",
                                                      "--candidate-git-revision", "--candidate-ids",
                                                      "--api-modules", "--api-version", "--maven-repository");

    private ParityManifestCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, ServiceLoader.load(InventorySource.class).stream().map(ServiceLoader.Provider::get)
                                           .toList(), System.out, System.err));
    }

    /** Runs the generator with {@code available} sources; returns the exit status. */
    public static int run(String[] args, List<InventorySource> available, PrintStream out, PrintStream err) {
        Map<String, List<String>> options = new HashMap<>();
        Set<String> flags = new LinkedHashSet<>();
        for (int i = 0; i < args.length; i++) {
            if (FLAGS.contains(args[i])) {
                flags.add(args[i]);
            } else if (OPTIONS.contains(args[i]) && i + 1 < args.length) {
                options.computeIfAbsent(args[i], k -> new ArrayList<>()).add(args[++i]);
            } else {
                err.println("Unknown or incomplete argument: " + args[i]);
                return 2;
            }
        }
        if (!options.containsKey("--repository") || !options.containsKey("--output")) {
            err.println("--repository and --output are required");
            return 2;
        }
        try {
            Path repository = Path.of(single(options, "--repository"));
            List<InventorySource> sources = select(available, options.getOrDefault("--source", List.of()));
            ClassificationRules rules = options.containsKey("--rules")
                                        ? ClassificationRules.parse(Files.readString(Path.of(single(options, "--rules")),
                                                                                     StandardCharsets.UTF_8))
                                        : ClassificationRules.loadDefault();
            String cutoff = single(options, "--feature-freeze-cutoff");
            String revision = single(options, "--git-revision");
            BaselineIdentity baseline = new BaselineIdentity(v2Version(repository),
                                                             revision != null ? revision : gitHead(repository),
                                                             cutoff == null ? null : LocalDate.parse(cutoff),
                                                             single(options, "--support-status"), scannerVersion());
            String candidateVersion = single(options, "--candidate-version");
            CandidateIdentity candidate = candidateVersion == null
                                          ? null
                                          : new CandidateIdentity(candidateVersion,
                                                                  single(options, "--candidate-git-revision"));
            String idsFile = single(options, "--candidate-ids");
            Set<String> candidateIds = idsFile == null ? null
                                                       : Files.readAllLines(Path.of(idsFile), StandardCharsets.UTF_8)
                                                              .stream().map(String::strip)
                                                              .filter(s -> !s.isEmpty())
                                                              .collect(Collectors.toCollection(TreeSet::new));
            ParityManifest manifest = new ManifestGenerator(sources, rules).generate(
                repository, baseline, candidate,
                new ManifestValidator.Options(flags.contains("--require-frozen-baseline"), candidateIds),
                scanOptions(options));
            Path output = Path.of(single(options, "--output"));
            ManifestJson.write(manifest, output);
            out.println("Wrote " + manifest.entries().size() + " entries " + manifest.summary() + " to " + output);
            return 0;
        } catch (ManifestValidationException e) {
            err.println(e.getMessage());
            return 1;
        } catch (IllegalArgumentException | IOException | java.time.DateTimeException e) {
            err.println(e.getMessage());
            return 2;
        }
    }

    private static List<InventorySource> select(List<InventorySource> available, List<String> names) {
        if (names.isEmpty()) {
            return available;
        }
        List<InventorySource> selected = new ArrayList<>();
        for (String name : names) {
            selected.add(available.stream().filter(s -> s.name().equals(name)).findFirst()
                                  .orElseThrow(() -> new IllegalArgumentException("Unknown inventory source: " + name)));
        }
        return selected;
    }

    /** The {@code --api-*} and {@code --maven-repository} options as scanner options (without the leading dashes). */
    private static Map<String, String> scanOptions(Map<String, List<String>> options) {
        Map<String, String> result = new TreeMap<>();
        for (String name : List.of("--api-modules", "--api-version", "--maven-repository")) {
            String value = single(options, name);
            if (value != null) {
                result.put(name.substring(2), value);
            }
        }
        return result;
    }

    private static String single(Map<String, List<String>> options, String name) {
        List<String> values = options.get(name);
        if (values == null) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException(name + " may be given once");
        }
        return values.get(0);
    }

    /** The {@code project/version} of the V2 root {@code pom.xml} under {@code repository}. */
    static String v2Version(Path repository) throws IOException {
        Path pom = repository.resolve("pom.xml");
        try (InputStream in = Files.newInputStream(pom)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Element project = factory.newDocumentBuilder().parse(in).getDocumentElement();
            for (Node child = project.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof Element e && e.getTagName().equals("version")) {
                    return e.getTextContent().strip();
                }
            }
            throw new IllegalArgumentException(pom + " has no project version");
        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IOException("Cannot read " + pom, e);
        }
    }

    /** The commit {@code HEAD} resolves to in {@code repository}'s {@code .git} directory, or {@code null}. */
    static String gitHead(Path repository) throws IOException {
        Path git = repository.resolve(".git");
        Path headFile = git.resolve("HEAD");
        if (!Files.isRegularFile(headFile)) {
            return null;
        }
        String head = Files.readString(headFile, StandardCharsets.UTF_8).strip();
        if (!head.startsWith("ref: ")) {
            return head;
        }
        String ref = head.substring("ref: ".length());
        Path loose = git.resolve(ref);
        if (Files.isRegularFile(loose)) {
            return Files.readString(loose, StandardCharsets.UTF_8).strip();
        }
        Path packed = git.resolve("packed-refs");
        if (Files.isRegularFile(packed)) {
            for (String line : Files.readAllLines(packed, StandardCharsets.UTF_8)) {
                if (line.endsWith(" " + ref)) {
                    return line.substring(0, line.indexOf(' '));
                }
            }
        }
        return null;
    }

    /** {@code parity-manifest/<version>} from this module's build-generated properties. */
    static String scannerVersion() throws IOException {
        try (InputStream in = ParityManifestCli.class.getResourceAsStream(MODULE_PROPERTIES)) {
            if (in == null) {
                throw new IOException("Missing " + MODULE_PROPERTIES);
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("artifactId") + "/" + properties.getProperty("version");
        }
    }
}
