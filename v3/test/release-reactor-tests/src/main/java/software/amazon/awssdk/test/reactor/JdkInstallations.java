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

package software.amazon.awssdk.test.reactor;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * JDK installations available to fork Maven builds with, at most one per Java feature version.
 *
 * <p>Candidates, in priority order (the first JDK found for a feature version wins):
 * <ol>
 *   <li>{@value #HOMES_PROPERTY} system property, then {@value #HOMES_ENVIRONMENT_VARIABLE} environment variable: JDK
 *       homes separated by {@link File#pathSeparator}. Early-access builds are accepted only from these.</li>
 *   <li>The JDK running this code.</li>
 *   <li>{@code JAVA_HOME_<feature>_<arch>} environment variables (the convention of {@code actions/setup-java}) and
 *       {@code JAVA_HOME}.</li>
 *   <li>Unless {@value #SCAN_PROPERTY}{@code =false}: direct children of common JDK install roots (macOS
 *       {@code JavaVirtualMachines}, {@code /usr/lib/jvm}, SDKMAN, mise, asdf, IntelliJ {@code ~/.jdks}, and common
 *       Windows locations), newest update first.</li>
 * </ol>
 * A directory is a JDK if it has {@code bin/java}, {@code bin/javac}, and a {@code release} file with
 * {@code JAVA_VERSION}. Symbolic links are resolved so that one installation is never counted twice.
 */
public final class JdkInstallations {
    public static final String HOMES_PROPERTY = "v3.jdkHomes";
    public static final String HOMES_ENVIRONMENT_VARIABLE = "V3_JDK_HOMES";
    public static final String SCAN_PROPERTY = "v3.jdkDiscovery.scanWellKnownLocations";

    private static final Pattern JAVA_VERSION = Pattern.compile("^JAVA_VERSION=\"?(?<version>[^\"]+)\"?\\s*$");
    private static final Pattern SETUP_JAVA_VARIABLE = Pattern.compile("JAVA_HOME_\\d+(_[A-Za-z0-9]+)*");

    /** One JDK installation. */
    public record Jdk(Path home, int feature, String version, boolean earlyAccess) {
        public Path javaExecutable() {
            return home.resolve("bin").resolve(executable("java"));
        }

        @Override
        public String toString() {
            return "JDK " + version + " (" + home + ")";
        }
    }

    private final Map<Integer, Jdk> byFeature;
    private final List<String> searched;

    private JdkInstallations(Map<Integer, Jdk> byFeature, List<String> searched) {
        this.byFeature = Collections.unmodifiableMap(new TreeMap<>(byFeature));
        this.searched = List.copyOf(searched);
    }

    /** Discovers JDKs from this JVM's system properties and environment. */
    public static JdkInstallations discover() {
        String explicit = Optional.ofNullable(System.getProperty(HOMES_PROPERTY))
                                  .filter(v -> !v.isBlank() && !v.startsWith("${"))
                                  .orElse(System.getenv(HOMES_ENVIRONMENT_VARIABLE));
        boolean scan = !"false".equalsIgnoreCase(System.getProperty(SCAN_PROPERTY, "true").trim());
        return discover(split(explicit), Path.of(System.getProperty("java.home")), System.getenv(),
                        scan ? wellKnownRoots() : List.of());
    }

    static JdkInstallations discover(List<Path> explicitHomes, Path runningHome, Map<String, String> environment,
                                     List<Path> wellKnownRoots) {
        Map<Integer, Jdk> byFeature = new TreeMap<>();
        Set<Path> seen = new HashSet<>();
        List<String> searched = new ArrayList<>();
        searched.add(HOMES_PROPERTY + "/" + HOMES_ENVIRONMENT_VARIABLE + " " + explicitHomes);
        for (Path home : explicitHomes) {
            add(home, true, byFeature, seen);
        }
        searched.add("running JDK " + runningHome);
        add(runningHome, false, byFeature, seen);
        List<String> variables = new ArrayList<>(environment.keySet().stream()
                                                            .filter(k -> SETUP_JAVA_VARIABLE.matcher(k).matches())
                                                            .sorted()
                                                            .toList());
        variables.add("JAVA_HOME");
        searched.add("environment variables JAVA_HOME_<feature>_<arch>, JAVA_HOME");
        for (String variable : variables) {
            String value = environment.get(variable);
            if (value != null && !value.isBlank()) {
                add(Path.of(value), false, byFeature, seen);
            }
        }
        List<Jdk> scanned = new ArrayList<>();
        for (Path root : wellKnownRoots) {
            searched.add(root.toString());
            for (Path child : children(root)) {
                Stream.of(child, child.resolve("Contents/Home"))
                      .map(JdkInstallations::inspect)
                      .flatMap(Optional::stream)
                      .filter(jdk -> !jdk.earlyAccess())
                      .forEach(scanned::add);
            }
        }
        scanned.sort(Comparator.comparing(Jdk::version, JdkInstallations::compareVersions).reversed());
        for (Jdk jdk : scanned) {
            if (seen.add(jdk.home())) {
                byFeature.putIfAbsent(jdk.feature(), jdk);
            }
        }
        return new JdkInstallations(byFeature, searched);
    }

    /** Every discovered JDK, ascending by feature version. */
    public List<Jdk> all() {
        return List.copyOf(byFeature.values());
    }

    public Optional<Jdk> feature(int feature) {
        return Optional.ofNullable(byFeature.get(feature));
    }

    /** JDKs whose feature version is greater than {@code feature}. */
    public List<Jdk> newerThan(int feature) {
        return all().stream().filter(jdk -> jdk.feature() > feature).toList();
    }

    /** JDKs whose feature version is in {@code [minimumInclusive, feature)}. */
    public List<Jdk> olderThan(int feature, int minimumInclusive) {
        return all().stream().filter(jdk -> jdk.feature() < feature && jdk.feature() >= minimumInclusive).toList();
    }

    /** Where discovery looked and what it found, for skip and failure messages. */
    public String describe() {
        return "discovered " + all() + " after searching " + searched + ". Add JDK homes with -D" + HOMES_PROPERTY
               + "=<home>" + File.pathSeparator + "<home> or the " + HOMES_ENVIRONMENT_VARIABLE
               + " environment variable.";
    }

    /** Inspects {@code home}; empty if it is not a JDK installation. */
    static Optional<Jdk> inspect(Path home) {
        Path release = home.resolve("release");
        if (!Files.isRegularFile(release) || !Files.isRegularFile(home.resolve("bin").resolve(executable("java")))
            || !Files.isRegularFile(home.resolve("bin").resolve(executable("javac")))) {
            return Optional.empty();
        }
        try {
            Path real = home.toRealPath();
            for (String line : Files.readAllLines(release, StandardCharsets.UTF_8)) {
                Matcher m = JAVA_VERSION.matcher(line);
                if (m.matches()) {
                    String version = m.group("version");
                    return Optional.of(new Jdk(real, feature(version), version, version.contains("-ea")));
                }
            }
            return Optional.empty();
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Feature version of a {@code JAVA_VERSION}: {@code 1.8.0_392 -> 8}, {@code 21.0.2 -> 21}, {@code 27-ea -> 27}. */
    static int feature(String version) {
        String[] parts = version.split("[.\\-+_]");
        int first = Integer.parseInt(parts[0]);
        return first == 1 && parts.length > 1 ? Integer.parseInt(parts[1]) : first;
    }

    private static void add(Path home, boolean explicit, Map<Integer, Jdk> byFeature, Set<Path> seen) {
        Optional<Jdk> jdk = inspect(home);
        if (jdk.isEmpty()) {
            if (explicit) {
                throw new IllegalArgumentException(HOMES_PROPERTY + " entry " + home + " is not a JDK home (needs "
                                                   + "bin/java, bin/javac and a release file with JAVA_VERSION)");
            }
            return;
        }
        Jdk found = jdk.get();
        if ((explicit || !found.earlyAccess()) && seen.add(found.home())) {
            byFeature.putIfAbsent(found.feature(), found);
        }
    }

    private static int compareVersions(String a, String b) {
        String[] left = a.split("[.\\-+_]");
        String[] right = b.split("[.\\-+_]");
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            int l = numeric(left[i]);
            int r = numeric(right[i]);
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return Integer.compare(left.length, right.length);
    }

    private static int numeric(String part) {
        return part.chars().allMatch(Character::isDigit) && !part.isEmpty() ? Integer.parseInt(part) : -1;
    }

    private static List<Path> split(String homes) {
        if (homes == null || homes.isBlank()) {
            return List.of();
        }
        return Stream.of(homes.split(Pattern.quote(File.pathSeparator)))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .map(Path::of)
                     .toList();
    }

    private static List<Path> children(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> list = Files.list(root)) {
            return list.sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> wellKnownRoots() {
        Path home = Path.of(System.getProperty("user.home"));
        List<Path> roots = new ArrayList<>(List.of(home.resolve(".sdkman/candidates/java"),
                                                   home.resolve(".local/share/mise/installs/java"),
                                                   home.resolve(".asdf/installs/java"),
                                                   home.resolve(".jdks")));
        if (isWindows()) {
            String programFiles = Optional.ofNullable(System.getenv("ProgramFiles")).orElse("C:\\Program Files");
            roots.add(Path.of(programFiles, "Java"));
            roots.add(Path.of(programFiles, "Amazon Corretto"));
            roots.add(Path.of(programFiles, "Eclipse Adoptium"));
        } else {
            roots.addAll(0, List.of(Path.of("/Library/Java/JavaVirtualMachines"),
                                    home.resolve("Library/Java/JavaVirtualMachines"),
                                    Path.of("/usr/lib/jvm"),
                                    Path.of("/usr/java")));
        }
        return roots;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String executable(String name) {
        return isWindows() ? name + ".exe" : name;
    }
}
