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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Builds a {@link CandidateInventory} from V3 release-candidate artifacts: jars or class/resource directories (for
 * example {@code target/classes}). Per artifact it records:
 * <ul>
 *   <li>identity: {@code META-INF/aws-sdk-java-v3/modules/<artifactId>.properties} (every V3 jar module), else a
 *       single {@code META-INF/maven/<groupId>/<artifactId>/pom.properties}; the {@code artifact:} capability;</li>
 *   <li>{@code api:} capabilities: public and protected types and members of non-internal packages, with the same
 *       visibility rules as the V2 baseline scan ({@link PublicApiInventorySource});</li>
 *   <li>{@code service:<artifactId>} when the artifact holds a generated
 *       {@code META-INF/aws-sdk-java-v3/services/<package>/generation-manifest.json} (16.4);</li>
 *   <li>{@code http-mode:<artifactId>:sync|async} when it registers a V3 {@value #SYNC_HTTP_SERVICE} or
 *       {@value #ASYNC_HTTP_SERVICE} provider (16.9);</li>
 *   <li>the IDs listed in {@value #CAPABILITIES_RESOURCE} (behaviors and configuration settings the artifact
 *       implements, 16.10), one entry ID per line, {@code #} comments;</li>
 *   <li>its Java module name ({@code module-info.class} or {@code Automatic-Module-Name}) and whether it ships
 *       {@code META-INF/native-image/} configuration.</li>
 * </ul>
 * Capabilities without an artifact (for example POM-only artifacts such as the BOM) can be declared separately.
 */
public final class CandidateInventoryScanner {
    /** Behaviors and configuration settings an artifact declares it implements. */
    public static final String CAPABILITIES_RESOURCE = "META-INF/aws-sdk-java-v3/parity/capabilities.txt";
    /** The V3 sync HTTP SPI provider interface. */
    public static final String SYNC_HTTP_SERVICE = "software.amazon.awssdk.http.provider.SdkHttpService";
    /** The V3 async HTTP SPI provider interface. */
    public static final String ASYNC_HTTP_SERVICE = "software.amazon.awssdk.http.provider.SdkAsyncHttpService";

    static final String MODULE_PROPERTIES = "META-INF/aws-sdk-java-v3/modules/";
    static final Pattern GENERATION_MANIFEST =
        Pattern.compile("META-INF/aws-sdk-java-v3/services/[^/]+/generation-manifest\\.json");
    static final Pattern POM_PROPERTIES = Pattern.compile("META-INF/maven/([^/]+)/([^/]+)/pom\\.properties");
    static final Pattern MODULE_INFO = Pattern.compile("(?:META-INF/versions/[0-9]+/)?module-info\\.class");

    private CandidateInventoryScanner() {
    }

    /**
     * Scans {@code roots} (jars or directories) and adds {@code declaredIds} owned by
     * {@link CandidateInventory#DECLARED}.
     *
     * @throws IOException              if a root cannot be read
     * @throws IllegalArgumentException if a root's artifact cannot be identified, two roots are the same artifact, or
     *                                  a declared ID is malformed
     */
    public static CandidateInventory scan(List<Path> roots, List<String> declaredIds) throws IOException {
        CandidateInventory.Builder builder = CandidateInventory.builder();
        for (Path root : roots) {
            scanRoot(root, builder);
        }
        for (String id : declaredIds) {
            builder.capability(id, CandidateInventory.DECLARED);
        }
        return builder.build();
    }

    /**
     * The entry IDs of a capabilities or ID-list document: one ID per line; blank lines and comments (a {@code #}
     * that begins the line or follows whitespace, see {@link EvidenceHooks}) are ignored.
     *
     * @throws IllegalArgumentException naming the first malformed line
     */
    public static List<String> readIdList(String text, String origin) {
        List<String> ids = new ArrayList<>();
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = EvidenceHooks.stripComment(lines[i]);
            if (line.isEmpty()) {
                continue;
            }
            if (EntryKind.ofId(line).isEmpty()) {
                throw new IllegalArgumentException(origin + ":" + (i + 1) + ": '" + line
                                                   + "' is not a well-formed Parity Manifest entry ID");
            }
            ids.add(line);
        }
        return ids;
    }

    static void scanRoot(Path root, CandidateInventory.Builder builder) throws IOException {
        SortedMap<String, byte[]> files = Files.isDirectory(root) ? readDirectory(root) : readJar(root);
        String location = root.toString();
        String[] identity = identity(files, location);
        String coordinates = identity[0] + ":" + identity[1];
        String artifactId = identity[1];
        builder.artifact(new CandidateInventory.CandidateArtifact(
            coordinates, identity[2], location, moduleName(files, location),
            files.keySet().stream().anyMatch(n -> n.startsWith("META-INF/native-image/"))));

        Map<String, ApiClassFile> classes = new HashMap<>();
        Map<String, String> paths = new HashMap<>();
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            String name = file.getKey();
            if (name.endsWith(".class") && !name.startsWith("META-INF/") && !name.endsWith("module-info.class")
                && !name.endsWith("package-info.class")) {
                ApiClassFile c = ApiClassFile.parse(file.getValue());
                classes.put(c.name(), c);
                paths.put(c.name(), location + "!/" + name);
            }
        }
        for (InventoryItem item : PublicApiInventorySource.items(classes, paths, coordinates)) {
            builder.capability(item.id(), coordinates);
        }
        if (files.keySet().stream().anyMatch(n -> GENERATION_MANIFEST.matcher(n).matches())) {
            builder.capability("service:" + artifactId, coordinates);
        }
        if (hasProviders(files.get("META-INF/services/" + SYNC_HTTP_SERVICE))) {
            builder.capability(EntryKind.httpModeId(artifactId, false), coordinates);
        }
        if (hasProviders(files.get("META-INF/services/" + ASYNC_HTTP_SERVICE))) {
            builder.capability(EntryKind.httpModeId(artifactId, true), coordinates);
        }
        byte[] declared = files.get(CAPABILITIES_RESOURCE);
        if (declared != null) {
            for (String id : readIdList(new String(declared, StandardCharsets.UTF_8),
                                        location + "!/" + CAPABILITIES_RESOURCE)) {
                builder.capability(id, coordinates);
            }
        }
    }

    /** {groupId, artifactId, version (nullable)} of the artifact. */
    private static String[] identity(SortedMap<String, byte[]> files, String location) throws IOException {
        List<String> modules = files.keySet().stream()
                                    .filter(n -> n.startsWith(MODULE_PROPERTIES) && n.endsWith(".properties")
                                                 && n.indexOf('/', MODULE_PROPERTIES.length()) < 0)
                                    .toList();
        if (modules.size() == 1) {
            Properties p = properties(files.get(modules.get(0)));
            String group = p.getProperty("groupId");
            String artifact = p.getProperty("artifactId");
            if (group == null || artifact == null) {
                throw new IllegalArgumentException(location + "!/" + modules.get(0) + " lacks groupId or artifactId");
            }
            return new String[] {group, artifact, p.getProperty("version")};
        }
        if (modules.size() > 1) {
            throw new IllegalArgumentException(location + " holds several V3 module resources: " + modules);
        }
        List<Matcher> poms = files.keySet().stream().map(POM_PROPERTIES::matcher).filter(Matcher::matches).toList();
        if (poms.size() != 1) {
            throw new IllegalArgumentException("Cannot identify the candidate artifact at " + location + ": expected "
                                               + MODULE_PROPERTIES + "<artifactId>.properties or exactly one "
                                               + "META-INF/maven/<groupId>/<artifactId>/pom.properties");
        }
        Matcher pom = poms.get(0);
        return new String[] {pom.group(1), pom.group(2), properties(files.get(pom.group())).getProperty("version")};
    }

    private static String moduleName(SortedMap<String, byte[]> files, String location) throws IOException {
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            if (MODULE_INFO.matcher(file.getKey()).matches()) {
                try {
                    return ModuleDescriptor.read(new ByteArrayInputStream(file.getValue())).name();
                } catch (InvalidModuleDescriptorException e) {
                    throw new IOException("Invalid " + location + "!/" + file.getKey(), e);
                }
            }
        }
        byte[] manifest = files.get("META-INF/MANIFEST.MF");
        if (manifest != null) {
            String name = new Manifest(new ByteArrayInputStream(manifest)).getMainAttributes()
                                                                          .getValue("Automatic-Module-Name");
            if (name != null && !name.isBlank()) {
                return name.strip();
            }
        }
        return null;
    }

    private static boolean hasProviders(byte[] registration) {
        if (registration == null) {
            return false;
        }
        return new String(registration, StandardCharsets.UTF_8).lines()
                                                               .map(l -> l.replaceAll("#.*", "").strip())
                                                               .anyMatch(l -> !l.isEmpty());
    }

    private static Properties properties(byte[] bytes) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            p.load(in);
        }
        return p;
    }

    private static SortedMap<String, byte[]> readJar(Path jar) throws IOException {
        if (!Files.isRegularFile(jar)) {
            throw new IOException("Candidate artifact not found: " + jar);
        }
        SortedMap<String, byte[]> files = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : zip.stream().filter(e -> !e.isDirectory()).toList()) {
                try (InputStream in = zip.getInputStream(entry)) {
                    files.put(entry.getName(), in.readAllBytes());
                }
            }
        }
        return files;
    }

    private static SortedMap<String, byte[]> readDirectory(Path directory) throws IOException {
        SortedMap<String, byte[]> files = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(directory)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                String name = directory.relativize(file).toString().replace('\\', '/');
                files.put(name, Files.readAllBytes(file));
            }
        }
        return files;
    }
}
