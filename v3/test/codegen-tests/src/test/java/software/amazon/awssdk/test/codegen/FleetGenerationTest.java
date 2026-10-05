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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Fleet generation-only triage harness (task 13.2, Requirements 2.1, 2.10, 16.4): runs {@link AwsCodegen} over the
 * canonical model of every V2 service module of the Launch Parity Baseline, without compiling the output, and writes
 * a machine-readable report that groups failures by root cause. The authoritative fleet gate is the Maven fleet build
 * itself ({@code -Dv3.aggregate=services}: one generated module per baseline module under {@code v3/services}, pinned
 * by {@code v3/services/service-fleet.json}) together with {@code tools/service-fleet}'s
 * {@code ServiceFleetRepositoryTest}; this harness answers "what breaks, and why" across the whole fleet in about a
 * minute, for example before moving the pinned canonical model revision.
 *
 * <p>Skipped unless {@code -Dv3.fleetGeneration=true}. Run it with, for example:
 * <pre>
 * mvn -B -o -pl :codegen-tests verify -Dtest=FleetGenerationTest -Dv3.fleetGeneration=true \
 *     -DargLine=-Xmx12g [-Dv3.fleetGeneration.threads=6] [-Dv3.fleetGeneration.services=s3,ec2] \
 *     [-Dv3.fleetGeneration.keep=s3,ec2,sts] [-Dv3.fleetGeneration.timeoutMinutes=40]
 * </pre>
 *
 * <p><b>Service-to-model mapping.</b> The baseline is the {@code <module>} list of the V2 {@code services/pom.xml}
 * (the same list the parity manifest's {@code v2-services} scanner reports). Each V2 service of a module (one per
 * {@code codegen-resources[/<sub>]/service-2.json}; dynamodb and waf bundle two) is mapped to the canonical
 * model whose service shape's {@code aws.api#service} {@code sdkId} equals the {@code metadata.serviceId} of that
 * {@code service-2.json}. V2 derives its service name from that serviceId, so the mapping also yields the same
 * generated type names; the package is V2's, {@code software.amazon.awssdk.services.<module>[.<packageName>]}.
 * Unmapped services, unmapped canonical models and ambiguous keys are reported and fail the gate.
 *
 * <p><b>Provenance.</b> The canonical revision is the Git HEAD of the models checkout; a dirty checkout is reported.
 * Every generated {@code model-provenance.json} must record exactly that revision, the model digest returned by the
 * generator and the SHA-256 of every model file as read by this harness, or the service is a
 * {@code PROVENANCE_MISMATCH}.
 *
 * <p>The report is written to {@code target/fleet-generation/fleet-generation-report.json}; generated trees are
 * deleted after they are measured unless the service is listed in {@code v3.fleetGeneration.keep}.
 */
class FleetGenerationTest {

    private static final Pattern MODULE = Pattern.compile("<module>([^<]+)</module>");
    private static final Pattern SERVICE_ID = Pattern.compile("\"serviceId\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern PACKAGE_NAME = Pattern.compile("\"packageName\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern FRAME = Pattern.compile("software\\.amazon\\.awssdk\\.awscodegen\\.");

    @Test
    void everyBaselineServiceGeneratesFromItsCanonicalModel() throws Exception {
        assumeTrue(Boolean.getBoolean("v3.fleetGeneration"), "Fleet generation runs only with -Dv3.fleetGeneration=true");
        Path models = Path.of(System.getProperty("v3.canonicalModels")).toRealPath();
        Path repository = Path.of(System.getProperty("v3.repositoryRoot")).toRealPath();
        Path target = Path.of(System.getProperty("v3.fleetGeneration.output", "target/fleet-generation"))
                          .toAbsolutePath().normalize();
        int threads = Integer.getInteger("v3.fleetGeneration.threads",
                                         Math.max(1, Math.min(6, Runtime.getRuntime().availableProcessors() / 2)));
        long timeoutMinutes = Long.getLong("v3.fleetGeneration.timeoutMinutes", 40);
        Set<String> only = csv(System.getProperty("v3.fleetGeneration.services", ""));
        Set<String> keep = csv(System.getProperty("v3.fleetGeneration.keep", ""));
        String revision = git(models, "rev-parse", "HEAD").trim();
        boolean clean = git(models, "status", "--porcelain").isBlank();

        long start = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<V2Module> baseline = v2Modules(repository);
            List<CanonicalModel> canonical = canonicalModels(models, executor);
            Mapping mapping = Mapping.of(baseline, canonical);

            List<Mapped> work = mapping.mapped.stream()
                                              .filter(m -> only.isEmpty() || only.contains(m.module.name))
                                              .sorted(Comparator.comparingLong((Mapped m) -> -m.model.size))
                                              .toList();
            deleteRecursively(target.resolve("services"));
            Map<String, Future<Map<String, Object>>> futures = new LinkedHashMap<>();
            for (Mapped m : work) {
                futures.put(m.module.name, executor.submit(
                    () -> generate(m, revision, target.resolve("services").resolve(m.module.name),
                                   keep.contains(m.module.name))));
            }
            long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(timeoutMinutes);
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map.Entry<String, Future<Map<String, Object>>> e : futures.entrySet()) {
                try {
                    results.add(e.getValue().get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS));
                } catch (TimeoutException timeout) {
                    e.getValue().cancel(true);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("module", e.getKey());
                    r.put("outcome", "TIMEOUT");
                    r.put("category", "TIMEOUT");
                    results.add(r);
                }
            }
            results.sort(Comparator.comparing(r -> (String) r.get("module")));
            long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("formatVersion", 1);
            report.put("canonicalModels", Map.of("revision", revision, "clean", clean,
                                                 "directories", canonical.size()));
            report.put("threads", threads);
            report.put("totalMillis", millis);
            report.put("mapping", mapping.report());
            report.put("summary", summary(results));
            report.put("services", results);
            Files.createDirectories(target);
            Path file = target.resolve("fleet-generation-report.json");
            Files.writeString(file, Node.prettyPrintJson(new NodeMapper().serialize(report)) + "\n", StandardCharsets.UTF_8);
            System.out.println("Fleet generation report: " + file + " (" + millis + " ms)");
            System.out.println(Node.prettyPrintJson(new NodeMapper().serialize(report.get("summary"))));

            List<String> failed = results.stream().filter(r -> !"SUCCESS".equals(r.get("outcome")))
                                         .map(r -> r.get("module") + "=" + r.get("category")).toList();
            assertEquals(List.of(), mapping.problems(), "Services and canonical models must map one to one");
            assertEquals(List.of(), failed, failed.size() + " service(s) failed generation; see " + file);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Map<String, Object> generate(Mapped m, String revision, Path out, boolean keep) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("module", m.module.name);
        r.put("sdkId", m.model.sdkId);
        r.put("modelDirectory", m.model.directory);
        r.put("service", m.model.shapeId);
        String packageName = m.module.packageName;
        r.put("v2Module", m.module.module);
        r.put("packageName", packageName);
        long start = System.nanoTime();
        try {
            AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest(
                m.model.shapeId, packageName, m.model.smithyDirectory, out.resolve("sources"), out.resolve("resources"),
                revision));
            r.put("sourceFiles", result.generatedFiles().size());
            r.put("javaFiles", result.generatedFiles().stream().filter(p -> p.toString().endsWith(".java")).count());
            r.put("resourceFiles", result.generatedResources().size());
            r.put("sourceBytes", size(result.sourceRoot()));
            r.put("modelDigest", result.modelDigest());
            r.put("revision", result.modelRevision());
            List<String> mismatches = provenanceMismatches(result, m.model.smithyDirectory, revision);
            r.put("outcome", mismatches.isEmpty() ? "SUCCESS" : "PROVENANCE_MISMATCH");
            r.put("category", mismatches.isEmpty() ? "SUCCESS" : "PROVENANCE_MISMATCH");
            if (!mismatches.isEmpty()) {
                r.put("provenanceMismatches", mismatches);
            }
        } catch (AwsCodegenException e) {
            r.put("outcome", "FAILURE");
            List<Diagnostic> diagnostics = e.diagnostics();
            if (diagnostics.isEmpty()) {
                r.put("category", "GENERATOR_EXCEPTION:" + signature(e.getCause() != null ? e.getCause() : e));
                r.put("error", firstLine(e.getMessage()));
                r.put("stack", stack(e));
            } else {
                r.put("category", diagnostics.stream().map(d -> d.code() + ":" + d.element()).distinct().sorted()
                                             .collect(Collectors.joining(",")));
                r.put("diagnosticCount", diagnostics.size());
                r.put("diagnostics", diagnostics.stream().limit(25).map(FleetGenerationTest::diagnostic).toList());
            }
        } catch (Throwable t) {
            r.put("outcome", "FAILURE");
            r.put("category", "CRASH:" + signature(t));
            r.put("error", firstLine(String.valueOf(t)));
            r.put("stack", stack(t));
        } finally {
            r.put("millis", Duration.ofNanos(System.nanoTime() - start).toMillis());
            if (!keep) {
                deleteRecursively(out);
            }
        }
        return r;
    }

    private static List<String> provenanceMismatches(AwsCodegenResult result, Path modelDirectory, String revision)
        throws Exception {
        List<Path> provenance = result.generatedResources().stream()
                                      .filter(p -> p.getFileName().toString().equals("model-provenance.json")).toList();
        if (provenance.size() != 1) {
            return List.of("expected one model-provenance.json, found " + provenance.size());
        }
        ObjectNode model = Node.parse(Files.readString(result.resourceRoot().resolve(provenance.get(0))))
                               .expectObjectNode().expectObjectMember("model");
        List<String> mismatches = new ArrayList<>();
        String recorded = model.expectStringMember("revision").getValue();
        if (!revision.equals(recorded) || !revision.equals(result.modelRevision())) {
            mismatches.add("revision " + recorded + " != pinned " + revision);
        }
        if (!result.modelDigest().equals(model.expectStringMember("digest").getValue())) {
            mismatches.add("digest " + model.expectStringMember("digest").getValue() + " != " + result.modelDigest());
        }
        for (Node f : model.expectArrayMember("files").getElements()) {
            ObjectNode file = f.expectObjectNode();
            String path = file.expectStringMember("path").getValue();
            String digest = "sha256:" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(modelDirectory.resolve(path))));
            if (!digest.equals(file.expectStringMember("digest").getValue())) {
                mismatches.add("file " + path + " digest " + file.expectStringMember("digest").getValue() + " != "
                               + digest);
            }
        }
        return mismatches;
    }

    private static Map<String, Object> summary(List<Map<String, Object>> results) {
        Map<String, Long> outcomes = new TreeMap<>(results.stream().collect(
            Collectors.groupingBy(r -> (String) r.get("outcome"), Collectors.counting())));
        Map<String, List<String>> categories = new TreeMap<>();
        for (Map<String, Object> r : results) {
            if (!"SUCCESS".equals(r.get("outcome"))) {
                for (String c : ((String) r.get("category")).split(",(?=[A-Z_]+:)")) {
                    categories.computeIfAbsent(c, k -> new ArrayList<>()).add((String) r.get("module"));
                }
            }
        }
        Map<String, Object> byCategory = new LinkedHashMap<>();
        categories.entrySet().stream()
                  .sorted(Comparator.comparing((Map.Entry<String, List<String>> e) -> -e.getValue().size())
                                    .thenComparing(Map.Entry::getKey))
                  .forEach(e -> byCategory.put(e.getKey(), Map.of("count", e.getValue().size(),
                                                                  "services", e.getValue())));
        long javaFiles = results.stream().map(r -> r.get("javaFiles")).filter(Long.class::isInstance)
                                .mapToLong(Long.class::cast).sum();
        long bytes = results.stream().map(r -> r.get("sourceBytes")).filter(Long.class::isInstance)
                            .mapToLong(Long.class::cast).sum();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("services", results.size());
        summary.put("outcomes", outcomes);
        summary.put("generatedJavaFiles", javaFiles);
        summary.put("generatedSourceBytes", bytes);
        summary.put("failureCategories", byCategory);
        return summary;
    }

    private static Map<String, Object> diagnostic(Diagnostic d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", d.code().name());
        m.put("element", d.element());
        d.operation().ifPresent(o -> m.put("operation", o));
        d.shape().ifPresent(s -> m.put("shape", s));
        m.put("message", d.message());
        return m;
    }

    /** Exception type plus the innermost aws-codegen frame: groups crashes with one root cause. */
    private static String signature(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        Optional<StackTraceElement> frame = Stream.of(root.getStackTrace())
                                                  .filter(f -> FRAME.matcher(f.getClassName()).lookingAt())
                                                  .findFirst();
        return root.getClass().getSimpleName()
               + frame.map(f -> "@" + f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1) + "."
                                + f.getMethodName()).orElse("");
    }

    /** The innermost cause and its first aws-codegen frames, to triage a generator exception. */
    private static List<String> stack(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        List<String> frames = new ArrayList<>();
        frames.add(firstLine(String.valueOf(root)));
        Stream.of(root.getStackTrace()).limit(14).map(String::valueOf).forEach(frames::add);
        return frames;
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        String line = s.lines().findFirst().orElse("");
        return line.length() > 600 ? line.substring(0, 600) + "..." : line;
    }

    private record V2Module(String name, String module, String serviceId, String packageName) {
    }

    private record CanonicalModel(String directory, Path smithyDirectory, String shapeId, String sdkId, long size) {
    }

    private record Mapped(V2Module module, CanonicalModel model) {
    }

    private record Mapping(List<Mapped> mapped, List<String> unmappedV2, List<String> unmappedCanonical,
                           List<String> ambiguous) {

        static Mapping of(List<V2Module> modules, List<CanonicalModel> models) {
            Map<String, List<CanonicalModel>> bySdkId = models.stream().filter(m -> m.sdkId != null)
                                                              .collect(Collectors.groupingBy(CanonicalModel::sdkId,
                                                                                             TreeMap::new,
                                                                                             Collectors.toList()));
            Map<String, List<V2Module>> byServiceId = modules.stream().filter(m -> m.serviceId != null)
                                                             .collect(Collectors.groupingBy(V2Module::serviceId,
                                                                                            TreeMap::new,
                                                                                            Collectors.toList()));
            List<Mapped> mapped = new ArrayList<>();
            List<String> unmappedV2 = new ArrayList<>();
            List<String> ambiguous = new ArrayList<>();
            for (V2Module module : modules) {
                List<CanonicalModel> candidates = module.serviceId == null ? List.of()
                                                                           : bySdkId.getOrDefault(module.serviceId,
                                                                                                  List.of());
                if (candidates.isEmpty()) {
                    unmappedV2.add(module.name + " (serviceId " + module.serviceId + ")");
                } else if (candidates.size() > 1 || byServiceId.get(module.serviceId).size() > 1) {
                    ambiguous.add(module.name + " (serviceId " + module.serviceId + ") -> "
                                  + candidates.stream().map(CanonicalModel::directory).toList());
                } else {
                    mapped.add(new Mapped(module, candidates.get(0)));
                }
            }
            List<String> unmappedCanonical = models.stream()
                                                   .filter(m -> m.sdkId == null || !byServiceId.containsKey(m.sdkId))
                                                   .map(m -> m.directory + " (sdkId " + m.sdkId + ", " + m.shapeId
                                                             + ")")
                                                   .toList();
            return new Mapping(mapped, unmappedV2, unmappedCanonical, ambiguous);
        }

        List<String> problems() {
            List<String> problems = new ArrayList<>();
            unmappedV2.forEach(s -> problems.add("V2 service without canonical model: " + s));
            unmappedCanonical.forEach(s -> problems.add("canonical model without V2 service: " + s));
            ambiguous.forEach(s -> problems.add("ambiguous mapping: " + s));
            return problems;
        }

        Map<String, Object> report() {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("key", "V2 codegen-resources/service-2.json metadata.serviceId == canonical aws.api#service sdkId");
            r.put("mapped", mapped.size());
            r.put("unmappedV2Services", unmappedV2);
            r.put("unmappedCanonicalModels", unmappedCanonical);
            r.put("ambiguous", ambiguous);
            Map<String, String> pairs = new TreeMap<>();
            mapped.forEach(m -> pairs.put(m.module.name, m.model.directory + " " + m.model.shapeId));
            r.put("services", pairs);
            return r;
        }
    }

    private static List<V2Module> v2Modules(Path repository) throws IOException {
        Matcher matcher = MODULE.matcher(Files.readString(repository.resolve("services/pom.xml")));
        List<V2Module> modules = new ArrayList<>();
        while (matcher.find()) {
            String module = matcher.group(1).trim();
            Path resources = repository.resolve("services").resolve(module)
                                       .resolve("src/main/resources/codegen-resources");
            List<Path> c2j = new ArrayList<>();
            if (Files.isRegularFile(resources.resolve("service-2.json"))) {
                c2j.add(resources.resolve("service-2.json"));
            } else if (Files.isDirectory(resources)) {
                // Modules bundling several services (dynamodb + dynamodbstreams, waf + wafregional) hold one C2J
                // model per sub-directory; a sub-directory customization's packageName is a sub-package.
                try (Stream<Path> list = Files.list(resources)) {
                    list.map(d -> d.resolve("service-2.json")).filter(Files::isRegularFile).sorted().forEach(c2j::add);
                }
            }
            if (c2j.isEmpty()) {
                modules.add(new V2Module(module, module, null, "software.amazon.awssdk.services." + module));
            }
            for (Path file : c2j) {
                // The metadata block precedes the operations, so the first serviceId is the service's.
                Matcher id = SERVICE_ID.matcher(Files.readString(file));
                String serviceId = id.find() ? id.group(1) : null;
                Path customization = file.resolveSibling("customization.config");
                String subPackage = null;
                if (Files.isRegularFile(customization)) {
                    Matcher p = PACKAGE_NAME.matcher(Files.readString(customization));
                    subPackage = p.find() ? p.group(1) : null;
                }
                String name = subPackage == null ? module : module + "." + subPackage;
                modules.add(new V2Module(name, module, serviceId, "software.amazon.awssdk.services." + name));
            }
        }
        return modules;
    }

    private static List<CanonicalModel> canonicalModels(Path models, ExecutorService executor) throws Exception {
        List<Path> directories;
        try (Stream<Path> list = Files.list(models)) {
            directories = list.filter(d -> Files.isRegularFile(d.resolve("smithy/model.json"))).sorted().toList();
        }
        List<Future<CanonicalModel>> futures = new ArrayList<>();
        for (Path d : directories) {
            futures.add(executor.submit(() -> {
                Path file = d.resolve("smithy/model.json");
                ObjectNode shapes = Node.parse(Files.readString(file)).expectObjectNode().expectObjectMember("shapes");
                String shapeId = null;
                String sdkId = null;
                for (Map.Entry<String, Node> shape : shapes.getStringMap().entrySet()) {
                    ObjectNode s = shape.getValue().expectObjectNode();
                    if ("service".equals(s.expectStringMember("type").getValue())) {
                        shapeId = shape.getKey();
                        sdkId = s.getObjectMember("traits").flatMap(t -> t.getObjectMember("aws.api#service"))
                                 .flatMap(t -> t.getStringMember("sdkId")).map(n -> n.getValue()).orElse(null);
                    }
                }
                return new CanonicalModel(d.getFileName().toString(), d.resolve("smithy"), shapeId, sdkId,
                                          Files.size(file));
            }));
        }
        List<CanonicalModel> result = new ArrayList<>();
        for (Future<CanonicalModel> f : futures) {
            result.add(f.get());
        }
        return result;
    }

    private static Set<String> csv(String value) {
        return Stream.of(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                     .collect(Collectors.toCollection(TreeSet::new));
    }

    private static String git(Path directory, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git", "-C", directory.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IOException("git " + String.join(" ", args) + " failed in " + directory + ": " + output);
        }
        return output;
    }

    private static long size(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
        }
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> files = Files.walk(root)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
