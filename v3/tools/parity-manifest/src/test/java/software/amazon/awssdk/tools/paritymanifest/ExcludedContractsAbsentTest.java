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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Public API scans of the V3 release artifacts against the Parity Manifest's excluded legacy facilities (task 15.4;
 * Requirements 18.1-18.8).
 *
 * <p>The baseline is the Parity Manifest of the released V2 API jars of every module the full manifest inventories
 * ({@link PublicApiInventorySource#DEFAULT_MODULES}, copied to {@code target/v2-public-api}) under the checked-in
 * classification rules. The candidate is the compiled output of every release artifact the V3 BOM manages
 * ({@link CandidateTree#moduleOutputs}), scanned with the launch gate's {@link CandidateInventoryScanner}, so the
 * public API compared is exactly what the launch gate sees: public and protected types and members of non-internal
 * packages, with JVM descriptors. The V3 release artifacts must be built first (the {@code v3-all} reactor builds
 * them before {@code tools/}); a missing artifact fails the scan rather than shrinking it.
 */
class ExcludedContractsAbsentTest {
    private static final Path V3 = Path.of("../..").toAbsolutePath().normalize();
    private static final String V2 = "software.amazon.awssdk.";
    /** The V3 packages of Modern Retry and Modern Auth, the only homes of a legacy facility's replacement. */
    private static final List<String> MODERN_RETRY = List.of("api:" + V2 + "retries.");
    private static final List<String> MODERN_AUTH = List.of("api:" + V2 + "auth.api.", "api:" + V2 + "auth.aws.");
    /** Package segments of the V2 signing and retry facilities (signer, presigner, retry policy and strategy APIs). */
    private static final Set<String> FACILITY_SEGMENTS = Set.of("signer", "retry", "retries");
    private static final Pattern REFERENCE = Pattern.compile("L([^;]+);");
    private static final String PRESIGNING = "software/amazon/awssdk/auth/aws/AwsV4Presigning";

    private static ParityManifest manifest;
    private static SortedMap<String, Path> outputs;
    private static SortedSet<String> releaseArtifacts;
    private static CandidateInventory candidate;

    @BeforeAll
    static void scan() throws Exception {
        manifest = CompatibilityFixturesGoldenTest.baselineManifest(v2PublicApiJars());
        releaseArtifacts = CandidateTree.bomArtifacts(V3.resolve("bom/pom.xml"));
        outputs = CandidateTree.moduleOutputs(V3, releaseArtifacts);
        candidate = CandidateInventoryScanner.scan(List.copyOf(outputs.values()), List.of());
    }

    /** The scan compares the whole baseline with the whole release candidate, so its absence claims are not vacuous. */
    @Test
    void scan_comparesEveryDefaultBaselineModuleWithEveryBuiltReleaseArtifact() throws IOException {
        String version = System.getProperty("v3.fixture.v2Version");
        List<String> modules = v2PublicApiJars().stream()
                                                .map(p -> p.getFileName().toString())
                                                .map(f -> f.substring(0, f.length() - ("-" + version + ".jar").length()))
                                                .toList();
        assertEquals(new TreeSet<>(PublicApiInventorySource.DEFAULT_MODULES), new TreeSet<>(modules),
                     "target/v2-public-api must hold exactly PublicApiInventorySource.DEFAULT_MODULES; update the "
                     + "copy-v2-public-api execution of this module's POM");
        SortedSet<String> unbuilt = new TreeSet<>(releaseArtifacts);
        unbuilt.removeAll(outputs.keySet());
        assertEquals(Set.of(), unbuilt, "Release artifacts without compiled output under " + V3 + "; build the V3 "
                                        + "release artifacts before this module (v3-all builds them before tools/)");
        assertTrue(manifest.entries().stream().anyMatch(e -> e.classification() == Classification.EXCLUDED),
                   "the baseline has no EXCLUDED entry");
    }

    /**
     * No {@code EXCLUDED} contract ships (Requirements 18.1, 18.2, 18.4, 18.6): no excluded type or member, which
     * covers the pre-SRA signers and signer overrides, the legacy {@code Signer}/{@code Presigner} contracts, the
     * legacy retry policies and strategies, and every deprecated API classified {@code EXCLUDED}.
     */
    @Test
    void releaseArtifacts_exposeNoExcludedContract() {
        List<String> shipped = new ArrayList<>();
        for (ManifestEntry e : manifest.entries()) {
            if (e.classification() == Classification.EXCLUDED && candidate.ships(e.id())) {
                shipped.add(e.id() + (e.deprecated() ? " (deprecated)" : "") + " shipped by " + candidate.owners(e.id()));
            }
        }
        assertEquals(List.of(), shipped, "EXCLUDED Parity Manifest entries are part of the V3 public API; remove them "
                                         + "from V3 or reclassify them with evidence");
        // The facilities of Requirement 18 are part of this baseline, so the absence above covers them.
        Set<String> excluded = manifest.entries().stream().filter(e -> e.classification() == Classification.EXCLUDED)
                                       .map(ManifestEntry::id).collect(Collectors.toSet());
        for (String id : List.of("api:" + V2 + "core.signer.Signer", "api:" + V2 + "core.signer.Presigner",
                                 "api:" + V2 + "auth.signer.Aws4Signer", "api:" + V2 + "core.retry.RetryPolicy",
                                 "api:" + V2 + "retries.LegacyRetryStrategy")) {
            assertTrue(excluded.contains(id), id + " is not EXCLUDED in the baseline manifest");
        }
    }

    /**
     * Deprecated entries do not gate general availability, so nothing else checks that a deprecated
     * {@code PRESERVED} claim (the {@code deprecated-*-shipped} rules) is still true: each must ship exactly.
     */
    @Test
    void deprecatedPreservedEntries_shipWithTheirExactV2Descriptor() {
        List<String> missing = manifest.entries().stream()
                                       .filter(e -> e.deprecated() && e.classification() == Classification.PRESERVED)
                                       .filter(e -> e.id().startsWith("api:") && !candidate.ships(e.id()))
                                       .map(ManifestEntry::id).toList();
        assertEquals(List.of(), missing, "deprecated PRESERVED entries that V3 does not ship");
    }

    /**
     * Every excluded signing or retry facility names its Modern Auth or Modern Retry replacement, and the candidate
     * ships it (Requirements 18.7, 18.8; the migration diagnostics name these replacements). A facility is an entry of
     * a V2 signer, retry or retries package, or one whose descriptor references such a type.
     */
    @Test
    void excludedSignerAndRetryFacilities_nameShippedModernReplacements() {
        List<String> problems = new ArrayList<>();
        int facilities = 0;
        for (ManifestEntry e : manifest.entries()) {
            if (e.classification() != Classification.EXCLUDED || !legacyFacility(e.id())) {
                continue;
            }
            facilities++;
            String replacement = e.replacement();
            if (replacement == null) {
                problems.add(e.id() + ": no replacement");
            } else if (!modern(replacement)) {
                problems.add(e.id() + ": replacement " + replacement + " is not a Modern Auth or Modern Retry API");
            } else if (!candidate.ships(replacement)) {
                problems.add(e.id() + ": replacement " + replacement + " is not shipped");
            }
        }
        assertTrue(facilities > 0, "no excluded signer or retry facility in the baseline");
        assertEquals(List.of(), problems, "excluded signer and retry facilities without a shipped modern replacement");
    }

    /**
     * The retry extension contracts a V3 client accepts (the parameter types of the generated client builders and of
     * the Modern Retry strategy builders) are Modern Retry types that the Parity Manifest designates: a V2 entry is
     * replaced by, or preserved as, each of them (Requirement 18.5).
     */
    @Test
    void clientRetryExtensionContracts_areModernRetryTypesTheManifestDesignates() {
        Set<String> designated = new HashSet<>();
        for (ManifestEntry e : manifest.entries()) {
            if (e.replacement() != null) {
                designated.add(e.replacement());
            }
            if (e.classification() != Classification.EXCLUDED) {
                designated.add(e.id());
            }
        }
        SortedSet<String> contracts = new TreeSet<>();
        for (String id : candidate.ids()) {
            int hash = id.indexOf('#');
            int open = id.indexOf('(', hash);
            if (hash < 0 || open < 0) {
                continue;
            }
            String owner = id.substring("api:".length(), hash);
            boolean clientBuilder = owner.endsWith("ClientBuilder") && owner.indexOf('$') < 0;
            boolean strategyBuilder = owner.startsWith(V2 + "retries.") && owner.endsWith("$Builder");
            if (!clientBuilder && !strategyBuilder) {
                continue;
            }
            Matcher m = REFERENCE.matcher(id.substring(open + 1, id.indexOf(')', open)));
            while (m.find()) {
                String type = m.group(1).replace('/', '.');
                if (type.startsWith(V2 + "retries.")) {
                    contracts.add("api:" + type);
                }
            }
        }
        // Extension contracts are the types a caller implements or extends; final values (records such as
        // ModeledRetryTraits) only carry data.
        contracts.removeIf(c -> !extensible(c));
        assertTrue(contracts.contains("api:" + V2 + "retries.RetryStrategy"), contracts.toString());
        List<String> undesignated = contracts.stream()
                                             .filter(c -> c.contains(".internal.") || !designated.contains(c))
                                             .toList();
        assertEquals(List.of(), undesignated, "retry extension contracts of V3 clients " + contracts
                                              + " that the Parity Manifest does not designate");
    }

    /**
     * Every V2 service presigner ships in V3 and signs through the Modern Auth presigning that the manifest records as
     * the legacy {@code Presigner}'s replacement (Requirement 18.3).
     */
    @Test
    void servicePresigners_signThroughTheModernAuthReplacementOfTheLegacyPresigner() throws IOException {
        ManifestEntry legacy = manifest.entry("api:" + V2 + "core.signer.Presigner").orElseThrow();
        assertEquals("api:" + PRESIGNING.replace('/', '.'), legacy.replacement());
        assertTrue(candidate.ships(legacy.replacement()), legacy.replacement());
        List<ManifestEntry> presigners = manifest.entries().stream()
                                                 .filter(e -> e.kind() == EntryKind.PUBLIC_TYPE)
                                                 .filter(e -> e.id().startsWith("api:" + V2 + "services.")
                                                              && e.id().contains(".presigner.")
                                                              && e.id().endsWith("Presigner"))
                                                 .toList();
        assertFalse(presigners.isEmpty(), "no V2 service presigner in the baseline");
        for (ManifestEntry presigner : presigners) {
            assertEquals(Classification.PRESERVED, presigner.classification(), presigner.id());
            assertTrue(candidate.ships(presigner.id()), presigner.id() + " is not shipped");
            String type = presigner.id().substring("api:".length());
            String directory = type.substring(0, type.lastIndexOf('.')).replace('.', '/') + "/";
            boolean modernAuth = false;
            for (String owner : candidate.owners(presigner.id())) {
                modernAuth |= anyClassReferences(outputs.get(owner), directory, PRESIGNING);
            }
            assertTrue(modernAuth, presigner.id() + ": no class of its package presigns through " + PRESIGNING);
        }
    }

    /**
     * The Modern Auth and Modern Retry entries the migration recipes act on resolve in the candidate: their
     * replacements and owning artifacts ship, and their evidence IDs are registered hooks naming V3 test classes (the
     * launch gate's packaging checks, run here without the launch-gate profile).
     */
    @Test
    void modernAuthAndRetryEntries_resolveInTheCandidate() throws IOException {
        EvidenceHooks hooks = LaunchGateCli.builtinHooks();
        Set<String> testClasses = CandidateTree.testClasses(V3);
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (ManifestEntry e : manifest.entries()) {
            boolean modernReplacement = e.replacement() != null && modern(e.replacement());
            if (!modernReplacement && !(e.deprecated() && e.classification() == Classification.PRESERVED)) {
                continue;
            }
            checked++;
            if (e.replacement() != null && !candidate.ships(e.replacement())) {
                problems.add(e.id() + ": replacement " + e.replacement() + " is not shipped");
            }
            if (e.v3Artifact() != null && !candidate.hasArtifact(e.v3Artifact())) {
                problems.add(e.id() + ": owning artifact " + e.v3Artifact() + " is not part of the candidate");
            }
            for (String evidence : e.evidenceIds()) {
                if (!hooks.known(evidence) || !testClasses.contains(evidence)) {
                    problems.add(e.id() + ": evidence " + evidence + " is not a registered hook naming a V3 test");
                }
            }
        }
        assertTrue(checked > 0, "no Modern Auth or Modern Retry entry in the baseline");
        assertEquals(List.of(), problems);
    }

    /**
     * True if an entry is a V2 signing or retry facility: declared in a V2 signer, retry or retries package, or a
     * member named for signing or retrying (such as {@code RequestOverrideConfiguration.Builder#signer} or
     * {@code ClientOverrideConfiguration.Builder#retryPolicy}). A member that merely uses a legacy retry type for
     * something else (the waiter's polling backoff) is not one.
     */
    static boolean legacyFacility(String id) {
        String body = id.substring("api:".length());
        int hash = body.indexOf('#');
        String owner = hash < 0 ? body : body.substring(0, hash);
        if (facilityPackage(owner)) {
            return true;
        }
        if (hash < 0) {
            return false;
        }
        String member = body.substring(hash + 1).split("[(:]", 2)[0].toLowerCase(Locale.ROOT);
        return member.contains("signer") || member.contains("retry");
    }

    private static boolean facilityPackage(String binaryName) {
        if (!binaryName.startsWith(V2)) {
            return false;
        }
        String[] segments = binaryName.substring(V2.length()).split("\\.");
        for (int i = 0; i < segments.length - 1; i++) {
            if (FACILITY_SEGMENTS.contains(segments[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean modern(String id) {
        return !id.contains(".internal.")
               && Stream.concat(MODERN_RETRY.stream(), MODERN_AUTH.stream()).anyMatch(id::startsWith);
    }

    /** True if the shipped type {@code typeId} is an interface or a non-final class. */
    private static boolean extensible(String typeId) {
        String path = typeId.substring("api:".length()).replace('.', '/') + ".class";
        for (String owner : candidate.owners(typeId)) {
            byte[] bytes = classBytes(outputs.get(owner), path);
            if (bytes != null) {
                ApiClassFile type = ApiClassFile.parse(bytes);
                return (type.access() & ApiClassFile.ACC_INTERFACE) != 0 || !type.isFinal();
            }
        }
        throw new AssertionError(typeId + " is not a class of its owning artifacts " + candidate.owners(typeId));
    }

    private static byte[] classBytes(Path root, String path) {
        try {
            if (Files.isDirectory(root)) {
                Path file = root.resolve(path);
                return Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
            }
            try (ZipFile zip = new ZipFile(root.toFile())) {
                ZipEntry entry = zip.getEntry(path);
                if (entry == null) {
                    return null;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    return in.readAllBytes();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** True if a class file of {@code directory} (a package path) in {@code root} names {@code internalName}. */
    private static boolean anyClassReferences(Path root, String directory, String internalName) throws IOException {
        byte[] needle = internalName.getBytes(StandardCharsets.UTF_8);
        if (Files.isDirectory(root)) {
            Path packageDirectory = root.resolve(directory);
            if (!Files.isDirectory(packageDirectory)) {
                return false;
            }
            try (Stream<Path> files = Files.list(packageDirectory)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                    if (contains(Files.readAllBytes(f), needle)) {
                        return true;
                    }
                }
            }
            return false;
        }
        try (ZipFile zip = new ZipFile(root.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(directory) && name.endsWith(".class") && name.indexOf('/', directory.length()) < 0) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        if (contains(in.readAllBytes(), needle)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /** The V2 API jars of {@code target/v2-public-api}, sorted. */
    static List<Path> v2PublicApiJars() throws IOException {
        String version = System.getProperty("v3.fixture.v2Version");
        try (Stream<Path> files = Files.list(Path.of(System.getProperty("v3.fixture.v2PublicApi")))) {
            return files.filter(p -> p.toString().endsWith("-" + version + ".jar")).sorted().toList();
        }
    }
}
