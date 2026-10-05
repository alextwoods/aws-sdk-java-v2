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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Public and protected V2 types and members ({@code api:} entries), read from released V2 bytecode.
 *
 * <p><b>Source of truth.</b> The released jar {@code software/amazon/awssdk/<a>/<v>/<a>-<v>.jar} of each scanned
 * module in a local Maven repository ({@link #MAVEN_REPOSITORY_OPTION}, default {@code maven.repo.local} or
 * {@code ~/.m2/repository}). The version is {@link #VERSION_OPTION}, or by default the highest released
 * ({@code 2.x.y}) version not above the baseline's V2 version for which every scanned module's jar is present. The
 * scan never touches the network; a missing jar fails the scan.
 *
 * <p><b>Modules.</b> {@link #MODULES_OPTION} is a comma-separated artifact-ID list, or {@code all} for every
 * published {@code jar} module of the V2 aggregator tree except the aggregate re-packagings in
 * {@link #AGGREGATE_JARS}. The default is the bounded {@link #DEFAULT_MODULES}: the core runtime, the HTTP
 * Implementations, the high-level libraries, and the DynamoDB and S3 services the V3 fixtures generate.
 *
 * <p><b>What is API.</b> A type is API if it is public or protected (by its {@code InnerClasses} flags when nested),
 * not synthetic, local or anonymous, every enclosing type is API, it is not in a package with an {@code internal}
 * segment, and neither it nor an enclosing type is annotated {@code @SdkInternalApi}. A member of an API type is API
 * if it is public or protected, not synthetic or a bridge, not {@code <clinit>}, not annotated
 * {@code @SdkInternalApi}, and not a protected member of a final type. A type is deprecated if it or an enclosing type
 * is {@code @Deprecated}; a member is deprecated if it or its type is.
 */
public final class PublicApiInventorySource implements InventorySource {

    public static final String MODULES_OPTION = "api-modules";
    public static final String VERSION_OPTION = "api-version";
    public static final String MAVEN_REPOSITORY_OPTION = "maven-repository";

    public static final List<String> DEFAULT_MODULES = List.of(
        "annotations", "utils", "sdk-core", "aws-core", "regions", "auth", "profiles", "http-client-spi",
        "identity-spi", "http-auth-spi", "http-auth", "http-auth-aws", "checksums-spi", "checksums", "retries-spi",
        "retries", "metrics-spi", "endpoints-spi", "json-utils", "arns", "crt-core", "apache-client",
        "apache5-client", "url-connection-client", "netty-nio-client", "aws-crt-client",
        "cloudwatch-metric-publisher", "dynamodb", "s3", "dynamodb-enhanced", "s3-transfer-manager",
        "iam-policy-builder", "s3-event-notifications", "sns-message-manager");

    /** Jars that re-package other modules' classes; scanning them would duplicate every entry. */
    public static final Set<String> AGGREGATE_JARS = Set.of("bundle", "bundle-sdk", "bundle-logging-bridge",
                                                            "aws-sdk-java");

    private static final String GROUP_PATH = "software/amazon/awssdk";
    private static final String SDK_INTERNAL_API = "Lsoftware/amazon/awssdk/annotations/SdkInternalApi;";

    @Override
    public String name() {
        return "v2-public-api";
    }

    @Override
    public List<InventoryItem> scan(Context context) throws IOException {
        List<String> modules = modules(context);
        Path repository = mavenRepository(context);
        String version = context.option(VERSION_OPTION) != null
                         ? context.option(VERSION_OPTION)
                         : defaultVersion(repository, modules, context.baseline().v2Version());
        Map<String, InventoryItem> items = new TreeMap<>();
        for (String module : modules) {
            Path jar = jar(repository, module, version);
            if (!Files.isRegularFile(jar)) {
                throw new IOException("Released V2 jar not found (offline scan): " + jar);
            }
            String location = GROUP_PATH + "/" + module + "/" + version + "/" + jar.getFileName();
            for (InventoryItem item : scanJar(jar, module, location)) {
                items.putIfAbsent(item.id(), item);
            }
        }
        return List.copyOf(items.values());
    }

    /** The selected module artifact IDs. */
    static List<String> modules(Context context) throws IOException {
        String option = context.option(MODULES_OPTION);
        if (option == null || option.isBlank()) {
            return DEFAULT_MODULES;
        }
        if (option.strip().equals("all")) {
            return V2PomTree.modules(context.repositoryRoot()).stream()
                            .filter(m -> m.published() && m.packaging().equals("jar"))
                            .map(V2PomTree.Module::artifactId)
                            .filter(a -> !AGGREGATE_JARS.contains(a))
                            .distinct().sorted().toList();
        }
        return Arrays.stream(option.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    static Path mavenRepository(Context context) {
        String option = context.option(MAVEN_REPOSITORY_OPTION);
        if (option != null) {
            return Path.of(option);
        }
        String local = System.getProperty("maven.repo.local");
        return local != null ? Path.of(local) : Path.of(System.getProperty("user.home"), ".m2", "repository");
    }

    static Path jar(Path repository, String module, String version) {
        return repository.resolve(GROUP_PATH).resolve(module).resolve(version).resolve(module + "-" + version + ".jar");
    }

    /** The highest {@code 2.x.y} version not above {@code ceiling} whose jar exists for every module. */
    static String defaultVersion(Path repository, List<String> modules, String ceiling) throws IOException {
        int[] max = numeric(ceiling.replaceAll("-.*", ""));
        Path first = repository.resolve(GROUP_PATH).resolve(modules.get(0));
        List<String> candidates;
        try (Stream<Path> s = Files.isDirectory(first) ? Files.list(first) : Stream.empty()) {
            candidates = s.map(p -> p.getFileName().toString()).filter(v -> v.matches("2\\.\\d+\\.\\d+"))
                          .filter(v -> compare(numeric(v), max) <= 0)
                          .sorted(Comparator.comparing(PublicApiInventorySource::numeric,
                                                       PublicApiInventorySource::compare).reversed())
                          .toList();
        }
        for (String v : candidates) {
            if (modules.stream().allMatch(m -> Files.isRegularFile(jar(repository, m, v)))) {
                return v;
            }
        }
        throw new IOException("No released V2 version <= " + ceiling + " has jars for all of " + modules + " in "
                              + repository + "; pass --api-version or populate the local repository");
    }

    private static int[] numeric(String version) {
        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    private static int compare(int[] a, int[] b) {
        return Arrays.compare(a, b);
    }

    /** The API items of one jar, sorted by ID. */
    static List<InventoryItem> scanJar(Path jar, String artifact, String location) throws IOException {
        Map<String, ApiClassFile> classes = new HashMap<>();
        Map<String, String> paths = new HashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry entry : zip.stream().sorted(Comparator.comparing(ZipEntry::getName)).toList()) {
                String name = entry.getName();
                if (!name.endsWith(".class") || name.startsWith("META-INF/") || name.endsWith("module-info.class")
                    || name.endsWith("package-info.class")) {
                    continue;
                }
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }
                ApiClassFile c = ApiClassFile.parse(bytes);
                classes.put(c.name(), c);
                paths.put(c.name(), location + "!/" + name);
            }
        }
        return items(classes, paths, artifact);
    }

    /** The API items of {@code classes}, sorted by ID. */
    static List<InventoryItem> items(Map<String, ApiClassFile> classes, Map<String, String> paths, String artifact) {
        List<InventoryItem> items = new ArrayList<>();
        for (ApiClassFile c : classes.values()) {
            if (!isApiType(c, classes)) {
                continue;
            }
            boolean typeDeprecated = isDeprecated(c, classes);
            SourceLocation source = new SourceLocation(artifact, paths.get(c.name()));
            items.add(new InventoryItem(EntryKind.typeId(c.name()), EntryKind.PUBLIC_TYPE,
                                        c.typeKind() + " " + c.name(), source, typeDeprecated));
            for (ApiClassFile.Member f : c.fields()) {
                if (isApiMember(c, f)) {
                    items.add(new InventoryItem(EntryKind.fieldId(c.name(), f.name(), f.descriptor()),
                                                EntryKind.PUBLIC_MEMBER, "field " + c.name() + "." + f.name(),
                                                source, typeDeprecated || f.deprecated()));
                }
            }
            for (ApiClassFile.Member m : c.methods()) {
                if (isApiMember(c, m) && !m.name().equals("<clinit>")) {
                    String what = m.name().equals("<init>") ? "constructor " : "method ";
                    items.add(new InventoryItem(EntryKind.methodId(c.name(), m.name(), m.descriptor()),
                                                EntryKind.PUBLIC_MEMBER, what + c.name() + "#" + m.name()
                                                                         + m.descriptor(),
                                                source, typeDeprecated || m.deprecated()));
                }
            }
        }
        items.sort(Comparator.comparing(InventoryItem::id));
        return items;
    }

    private static boolean isApiMember(ApiClassFile owner, ApiClassFile.Member member) {
        return member.isApi() && !member.annotations().contains(SDK_INTERNAL_API)
               && !(member.isProtected() && owner.isFinal());
    }

    private static boolean isApiType(ApiClassFile c, Map<String, ApiClassFile> classes) {
        if (Arrays.asList(c.packageName().split("\\.")).contains("internal")) {
            return false;
        }
        for (ApiClassFile t = c; t != null; t = t.outerName() == null ? null : classes.get(t.outerName())) {
            if (!t.isApi() || t.annotations().contains(SDK_INTERNAL_API)) {
                return false;
            }
            if (t.outerName() != null && !classes.containsKey(t.outerName())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDeprecated(ApiClassFile c, Map<String, ApiClassFile> classes) {
        for (ApiClassFile t = c; t != null; t = t.outerName() == null ? null : classes.get(t.outerName())) {
            if (t.deprecated()) {
                return true;
            }
        }
        return false;
    }
}
