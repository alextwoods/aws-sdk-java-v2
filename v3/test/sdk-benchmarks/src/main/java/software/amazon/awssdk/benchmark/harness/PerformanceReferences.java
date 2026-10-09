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

package software.amazon.awssdk.benchmark.harness;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The Performance Reference arms (Req 20.14, 20.15; glossary "Performance Reference"): the Launch Parity Baseline
 * (released V2 {@value #V2_BASELINE_VERSION}, the Parity Manifest's frozen baseline), the inspected
 * {@code feature/poc/racecar} implementation pinned to commit {@value #RACECAR_COMMIT}, and the pinned Smithy Java
 * reference implementation ({@value #SMITHY_JAVA_VERSION}, V3's {@code v3.smithy-java.version}).
 *
 * <p>Each reference records its pinned provenance (coordinates or commit, and the SHA-256 and size of every resolved
 * artifact), the cells it applies to, and its status: {@link Status#READY} only when every pinned artifact resolves and
 * a reference client main honouring {@link ClientMain}'s command line and result record exists for it. No reference
 * result is ever synthesized: a reference that is not ready has no arm, its comparisons are absent from the report,
 * and {@link #requireReady} fails a release-gate collection closed.
 */
public final class PerformanceReferences {

    public static final String V2_BASELINE_VERSION = "2.54.0";
    public static final String RACECAR_BRANCH = "feature/poc/racecar";
    public static final String RACECAR_COMMIT = "ef9112895aca3057dd3a01b1731fc97ae0cdbe6e";
    public static final String SMITHY_JAVA_VERSION = "1.6.1";

    /**
     * V2 baseline runtime closure of the reference's cells (as fleet-differential-tests resolves it): the clients of
     * every protocol's finite fixture and their protocol modules.
     */
    static final List<String> V2_ARTIFACTS = List.of(
        "dynamodb", "kinesis", "lambda", "sns", "ec2", "cloudwatch", "aws-cbor-protocol", "aws-query-protocol",
        "smithy-rpcv2-protocol", "third-party-jackson-dataformat-cbor", "sdk-core", "aws-core", "aws-json-protocol", "protocol-core", "regions", "auth", "http-auth",
        "http-auth-aws", "http-auth-spi", "identity-spi", "http-client-spi", "endpoints-spi", "metrics-spi",
        "profiles", "retries", "retries-spi", "json-utils", "third-party-jackson-core", "utils", "utils-lite",
        "annotations", "checksums", "checksums-spi", "apache-client", "apache5-client", "url-connection-client",
        "netty-nio-client", "aws-crt-client");

    /** Smithy Java runtime modules of a Smithy Java DynamoDB (awsJson1.0) client, dynamic and generated. */
    static final List<String> SMITHY_JAVA_ARTIFACTS = List.of(
        "core", "io", "json-codec", "client-core", "client-http", "http-api", "aws-client-core",
        "aws-client-awsjson", "aws-sigv4", "dynamic-client");

    public enum Role {
        LAUNCH_PARITY_BASELINE("launch-parity-baseline"),
        RACECAR("racecar"),
        SMITHY_JAVA("smithy-java");

        private final String id;

        Role(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum Status {
        /** Every pinned artifact resolved and a reference client main exists: the arm can run. */
        READY("ready"),
        /** A pinned artifact is not available on this host. */
        ARTIFACT_MISSING("artifact-missing"),
        /** The artifacts resolve, but no reference client main drives the reference's client against the fixtures. */
        CLIENT_MISSING("client-missing");

        private final String id;

        Status(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** One resolved (or missing) pinned artifact. */
    public record Artifact(String coordinates, Path path, boolean present, long bytes, String sha256) {
        ObjectNode toNode() {
            ObjectNode.Builder b = ObjectNode.builder().withMember("coordinates", coordinates)
                                             .withMember("path", path.toString()).withMember("present", present);
            return present ? b.withMember("bytes", bytes).withMember("sha256", sha256).build() : b.build();
        }
    }

    /**
     * The cells a reference has a comparable cell for: the product of its protocols, client paths, workload kinds and
     * transports. For a ready reference this is exactly what its client main serves (it refuses any other cell); for
     * one that is not ready it is the scope its pinned build is expected to cover.
     */
    public record Applicability(Set<BenchProtocol> protocols, Set<ClientPath> paths, Set<WorkloadKind> kinds,
                                Set<Transport> transports) {
        public Applicability {
            protocols = Set.copyOf(protocols);
            paths = Set.copyOf(paths);
            kinds = Set.copyOf(kinds);
            transports = Set.copyOf(transports);
        }

        static Applicability of(Set<ClientPath> paths) {
            return new Applicability(Set.of(BenchProtocol.values()), paths, Set.of(WorkloadKind.values()),
                                     Set.of(Transport.values()));
        }

        public boolean contains(BenchmarkCell cell) {
            return protocols.contains(cell.protocol()) && paths.contains(cell.path()) && kinds.contains(cell.kind())
                   && transports.contains(cell.transport());
        }

        ObjectNode toNode() {
            return ObjectNode.builder()
                .withMember("protocols", ArrayNode.fromStrings(protocols.stream().map(BenchProtocol::id).sorted()
                                                                        .toList()))
                .withMember("paths", ArrayNode.fromStrings(paths.stream().map(ClientPath::id).sorted().toList()))
                .withMember("kinds", ArrayNode.fromStrings(kinds.stream().map(WorkloadKind::id).sorted().toList()))
                .withMember("transports", ArrayNode.fromStrings(transports.stream().map(Transport::id).sorted()
                                                                          .toList()))
                .build();
        }
    }

    /**
     * The Launch Parity Baseline's client ({@code test/sdk-benchmarks-v2-reference}): the finite cells of awsJson1.0
     * (DynamoDB), awsJson1.1 (Kinesis), restJson1 (Lambda), awsQuery (SNS), ec2Query (EC2) and rpcv2Cbor (CloudWatch,
     * which V2 2.54.0 speaks rpcv2Cbor for) on the generated sync and async clients over every V2 transport the cells
     * name. Not covered, so release mode fails closed on them ({@link #uncovered}): restXml (the fixture's Route 53
     * client is not available at {@value #V2_BASELINE_VERSION} offline), {@code apache5-async} (V2 has no Apache 5
     * async client), and the streaming and Event Stream kinds (no V2 reference fixture). Must equal that module's
     * {@code V2ReferenceMain} applicability ({@code V2ReferenceApplicabilityTest} compares them).
     */
    public static final Applicability V2_APPLICABILITY = new Applicability(
        Set.of(BenchProtocol.AWS_JSON_1_0, BenchProtocol.AWS_JSON_1_1, BenchProtocol.REST_JSON_1,
               BenchProtocol.AWS_QUERY, BenchProtocol.EC2_QUERY, BenchProtocol.RPC_V2_CBOR), Set.of(ClientPath.GENERATED_SYNC, ClientPath.GENERATED_ASYNC),
        Set.of(WorkloadKind.FINITE), Set.of(Transport.APACHE5_SYNC, Transport.APACHE4_SYNC,
                                            Transport.URLCONNECTION_SYNC, Transport.CRT_SYNC, Transport.NETTY_ASYNC,
                                            Transport.CRT_ASYNC));

    /** The V2 reference client module, relative to the reference root (the {@code v3/test} directory). */
    public static final String V2_REFERENCE_MODULE = "sdk-benchmarks-v2-reference";
    public static final String V2_REFERENCE_MAIN = "software.amazon.awssdk.benchmark.reference.v2.V2ReferenceMain";

    /**
     * One reference.
     *
     * @param id the arm id the reference runs as
     * @param pin the pinned version or commit
     * @param applicability the cells the reference has comparable cells for (V2 and Racecar have no Dynamic Client)
     * @param arm the runnable arm, present only when {@link Status#READY}
     */
    public record Reference(String id, Role role, String pin, String provenance, Applicability applicability,
                            List<Artifact> artifacts, Status status, String reason, Optional<BenchmarkArm> arm) {
        public Reference {
            artifacts = List.copyOf(artifacts);
        }

        /** Whether the reference has a cell comparable to {@code cell}. */
        public boolean appliesTo(BenchmarkCell cell) {
            return applicability.contains(cell);
        }

        public ObjectNode toNode() {
            ObjectNode.Builder b = ObjectNode.builder()
                .withMember("id", id).withMember("role", role.id()).withMember("pin", pin)
                .withMember("provenance", provenance).withMember("status", status.id())
                .withMember("paths", ArrayNode.fromStrings(applicability.paths().stream().map(ClientPath::id).sorted()
                                                                        .toList()))
                .withMember("applicability", applicability.toNode())
                .withMember("artifacts", ArrayNode.fromNodes(artifacts.stream().map(Artifact::toNode).toList()));
            if (reason != null) {
                b.withMember("reason", reason);
            }
            arm.ifPresent(a -> b.withMember("arm", a.toNode()));
            return b.build();
        }
    }

    private PerformanceReferences() {
    }

    /** {@link #resolve(Path, Path)} without any reference client module: no reference can be ready. */
    public static List<Reference> resolve(Path m2Repository) {
        return resolve(m2Repository, null);
    }

    /**
     * The three references resolved against the local Maven repository {@code m2Repository} and the built reference
     * client modules under {@code referenceRoot} (the {@code v3/test} directory; {@code null} for none).
     */
    public static List<Reference> resolve(Path m2Repository, Path referenceRoot) {
        List<Reference> refs = new ArrayList<>();
        Set<ClientPath> generated = Set.of(ClientPath.GENERATED_SYNC, ClientPath.GENERATED_ASYNC);
        List<Artifact> v2 = new ArrayList<>();
        for (String a : V2_ARTIFACTS) {
            v2.add(artifact(m2Repository, "software.amazon.awssdk", a, V2_BASELINE_VERSION));
        }
        refs.add(reference("v2-baseline", Role.LAUNCH_PARITY_BASELINE, V2_BASELINE_VERSION,
                           "software.amazon.awssdk:*:" + V2_BASELINE_VERSION
                           + " (Parity Manifest v3.v2Baseline.version)", V2_APPLICABILITY, v2,
                           v2Client(referenceRoot)));
        refs.add(reference("racecar", Role.RACECAR, RACECAR_COMMIT,
                           "git " + RACECAR_BRANCH + "@" + RACECAR_COMMIT + "; no published artifact: built from that "
                           + "commit by scripts/build-racecar-reference.sh", Applicability.of(generated), List.of(),
                           ClientLookup.none("no Racecar reference client main exists")));
        List<Artifact> smithy = new ArrayList<>();
        for (String a : SMITHY_JAVA_ARTIFACTS) {
            smithy.add(artifact(m2Repository, "software.amazon.smithy.java", a, SMITHY_JAVA_VERSION));
        }
        refs.add(reference("smithy-java", Role.SMITHY_JAVA, SMITHY_JAVA_VERSION,
                           "software.amazon.smithy.java:*:" + SMITHY_JAVA_VERSION + " (v3.smithy-java.version)",
                           Applicability.of(Set.of(ClientPath.values())), smithy,
                           ClientLookup.none("no Smithy Java reference client main exists")));
        return List.copyOf(refs);
    }

    /**
     * {@link #resolve(Path, Path)} of {@code ~/.m2/repository} (or {@code -Dmaven.repo.local}) and the reference root
     * {@code -Dv3.benchmark.referenceRoot}, by default the parent of the working directory (the harness runs from
     * {@code v3/test/sdk-benchmarks}).
     */
    public static List<Reference> resolveLocal() {
        String local = System.getProperty("maven.repo.local");
        String root = System.getProperty("v3.benchmark.referenceRoot");
        return resolve(local != null ? Path.of(local) : Path.of(System.getProperty("user.home"), ".m2", "repository"),
                       root != null ? Path.of(root) : Path.of(System.getProperty("user.dir")).toAbsolutePath()
                                                          .getParent());
    }

    /** A reference client main found on disk (its class path and main class), or why there is none. */
    record ClientLookup(String classpath, String mainClass, String problem) {
        static ClientLookup none(String problem) {
            return new ClientLookup(null, null, problem);
        }
    }

    /**
     * The built V2 reference client: {@code <root>/sdk-benchmarks-v2-reference/target/classes} (the driver and
     * {@code V2ReferenceMain}) plus {@code target/classpath.txt} (its V2-only runtime class path). Rejected when any
     * entry is missing or any {@code software.amazon.awssdk} jar on it is not a {@value #V2_BASELINE_VERSION} jar (a
     * V3 jar there would bind V2's shared package names to V3 classes).
     */
    static ClientLookup v2Client(Path root) {
        if (root == null) {
            return ClientLookup.none("no reference root given");
        }
        Path target = root.resolve(V2_REFERENCE_MODULE).resolve("target");
        Path classes = target.resolve("classes");
        // The V2 client code compiles at the module's test scope (against the released V2 jars), the driver in main.
        Path clientClasses = target.resolve("test-classes");
        Path main = clientClasses.resolve(V2_REFERENCE_MAIN.replace('.', '/') + ".class");
        Path classpathFile = target.resolve("classpath.txt");
        if (!Files.isRegularFile(main) || !Files.isRegularFile(classpathFile)) {
            return ClientLookup.none("the V2 reference client is not built (" + main + ", " + classpathFile
                                     + "); build test/" + V2_REFERENCE_MODULE);
        }
        String deps;
        try {
            deps = Files.readString(classpathFile).trim();
        } catch (IOException e) {
            return ClientLookup.none("unreadable " + classpathFile + ": " + e);
        }
        for (String entry : deps.split(java.io.File.pathSeparator)) {
            Path p = Path.of(entry);
            if (!Files.exists(p)) {
                return ClientLookup.none("V2 reference class path entry missing: " + entry);
            }
            String normalized = p.toAbsolutePath().normalize().toString().replace('\\', '/');
            if (normalized.contains("/software/amazon/awssdk/") && !normalized.contains("/software/amazon/awssdk/crt/")
                && !normalized.contains("/" + V2_BASELINE_VERSION + "/")) {
                return ClientLookup.none("V2 reference class path carries a non-" + V2_BASELINE_VERSION
                                         + " SDK jar: " + entry);
            }
        }
        if (!Files.isDirectory(classes.resolve("software/amazon/awssdk/benchmark/harness"))) {
            return ClientLookup.none("the V2 reference client's shared driver is not built: " + classes);
        }
        return new ClientLookup(classes.toAbsolutePath() + java.io.File.pathSeparator + clientClasses.toAbsolutePath()
                                + java.io.File.pathSeparator + deps, V2_REFERENCE_MAIN, null);
    }

    private static Reference reference(String id, Role role, String pin, String provenance,
                                       Applicability applicability, List<Artifact> artifacts, ClientLookup client) {
        List<String> missing = artifacts.stream().filter(a -> !a.present()).map(Artifact::coordinates).toList();
        if (artifacts.isEmpty()) {
            return new Reference(id, role, pin, provenance, applicability, artifacts, Status.ARTIFACT_MISSING,
                                 "no pinned build of " + provenance + " is available offline", Optional.empty());
        }
        if (!missing.isEmpty()) {
            return new Reference(id, role, pin, provenance, applicability, artifacts, Status.ARTIFACT_MISSING,
                                 missing.size() + " pinned artifact(s) missing, e.g. " + missing.get(0),
                                 Optional.empty());
        }
        if (client.problem() != null) {
            // A reference without a client main never yields a result.
            return new Reference(id, role, pin, provenance, applicability, artifacts, Status.CLIENT_MISSING,
                                 client.problem(), Optional.empty());
        }
        return new Reference(id, role, pin, provenance, applicability, artifacts, Status.READY, null,
                             Optional.of(new BenchmarkArm(id, client.classpath(), client.mainClass(), null)));
    }

    static Artifact artifact(Path repo, String group, String artifactId, String version) {
        String coordinates = group + ":" + artifactId + ":" + version;
        Path jar = repo.resolve(group.replace('.', '/')).resolve(artifactId).resolve(version)
                       .resolve(artifactId + "-" + version + ".jar");
        if (!Files.isRegularFile(jar)) {
            return new Artifact(coordinates, jar, false, -1, null);
        }
        try (InputStream in = Files.newInputStream(jar)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) {
                md.update(buf, 0, n);
            }
            return new Artifact(coordinates, jar, true, Files.size(jar), HexFormat.of().formatHex(md.digest()));
        } catch (IOException | NoSuchAlgorithmException e) {
            return new Artifact(coordinates, jar, false, -1, null);
        }
    }

    /** The runnable reference arms. */
    public static List<BenchmarkArm> readyArms(List<Reference> refs) {
        return refs.stream().flatMap(r -> r.arm().stream()).toList();
    }

    /**
     * Fails a release-gate collection closed when any reference is not {@link Status#READY}: the Performance Reference
     * is the best of all three, so a missing one cannot be skipped (Req 20.14, 20.15, 20.29).
     */
    public static void requireReady(HarnessMode mode, List<Reference> refs) {
        if (mode != HarnessMode.RELEASE) {
            return;
        }
        List<String> problems = refs.stream().filter(r -> r.status() != Status.READY)
                                    .map(r -> r.id() + " " + r.status().id() + ": " + r.reason()).toList();
        if (!problems.isEmpty()) {
            throw new HarnessPolicyException("release-gate collection needs every Performance Reference; "
                                             + String.join("; ", problems));
        }
    }

    /**
     * The generated cells of {@code cells} some reference has no comparable cell for, as {@code "<reference>: <cell>"}.
     * Every generated cell is compared with the best of all three references (Req 20.14, 20.15) and the generated
     * non-streaming aggregates with the Launch Parity Baseline over every cell (Req 20.11-20.13), so each generated
     * cell needs every reference; Dynamic Client cells are gated against V3's own generated cells (Req 20.17, 20.18).
     */
    public static List<String> uncovered(List<Reference> refs, List<BenchmarkCell> cells) {
        List<String> out = new ArrayList<>();
        for (BenchmarkCell cell : cells) {
            if (cell.path().isDynamic()) {
                continue;
            }
            for (Reference r : refs) {
                if (!r.appliesTo(cell)) {
                    out.add(r.id() + ": " + cell.id());
                }
            }
        }
        return out;
    }

    /**
     * {@link #requireReady(HarnessMode, List)}, and fails a release-gate collection closed when a selected generated
     * cell has no comparable cell in some reference ({@link #uncovered}): an uncovered cell would silently drop out of
     * the reference comparison and the aggregates.
     */
    public static void requireReady(HarnessMode mode, List<Reference> refs, List<BenchmarkCell> cells) {
        requireReady(mode, refs);
        if (mode != HarnessMode.RELEASE) {
            return;
        }
        List<String> uncovered = uncovered(refs, cells);
        if (!uncovered.isEmpty()) {
            throw new HarnessPolicyException("release-gate collection selects " + uncovered.size()
                                             + " generated cell/reference pair(s) without a reference cell, e.g. "
                                             + uncovered.get(0));
        }
    }
}
