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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.metadata.ModelProvenance;
import software.amazon.awssdk.awscore.metadata.ModelProvenance.ModelFile;
import software.amazon.awssdk.awscore.metadata.ModelProvenance.RevisionKind;
import software.amazon.awssdk.test.codegen.support.GeneratedTree;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 4: Provenance is exact (Requirements 2.12 and 22.2).
 *
 * <p>Each try generates a service from a synthetic canonical model whose service shape ID has a generated namespace
 * (one to five segments, mixed case, underscores, digits, long identifiers) and name, generated into a generated Java
 * package from model files at generated relative paths, pinned to a generated full-length Git commit (40 or 64
 * lower-case hex digits, biased to all-digit and leading-zero values that a numeric or trimming codec would alter) or
 * unpinned. The oracle is the generated input itself, never the generator: the published provenance, read back as
 * bytes, must carry exactly the requested revision and service shape ID, as seen by an independent JSON parser
 * (Smithy {@link Node}), by the runtime reader {@link ModelProvenance#parse}, by the generation manifest, and by the
 * generated {@code GeneratedServiceInfo} type, which is compiled against {@code aws-core} and loaded together with the
 * resource tree so that {@code provenance()} reads the published resource through the class path.
 *
 * <p>Nothing is conflated or abbreviated: every truncated (7, 12, 39 or 63 digits), extended (41 or 65), upper-case,
 * non-hex, whitespace-padded, digest-shaped or empty variant of the revision is rejected by the generator before
 * anything is published and by the runtime reader; service IDs differing from the modeled one only in the case of one
 * character, or by an added or dropped namespace segment, are rejected by the generator with nothing published and by
 * the runtime identity check; and non-absolute forms of the service ID are rejected by both.
 *
 * <p><b>Validates: Requirements 2.12, 22.2</b>
 */
class ExactProvenancePropertyTest {

    private static final String HEX = "0123456789abcdef";
    private static final String DIGITS = "0123456789";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String IDENTIFIER_CHARS = UPPER + LOWER + DIGITS + "_";

    /** Shapes the synthetic model defines next to the service; the service name must not collide with them. */
    private static final String OPERATION = "GetStatus";
    private static final Set<String> MODEL_SHAPE_NAMES = Set.of(OPERATION, OPERATION + "Input", OPERATION + "Output");

    private static final String RESOURCE_DIRECTORY = "META-INF/aws-sdk-java-v3/services/";
    private static final String SERVICE_INFO = "GeneratedServiceInfo";

    private static final JavaCompiler JAVAC = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(),
                                                                     "the tests must run on a JDK");
    private static final String RUNTIME_CLASS_PATH = runtimeClassPath();

    // Feature: aws-sdk-java-v3, Property 4: Provenance is exact
    @Property(tries = 200)
    @Label("Property 4: Provenance is exact")
    void provenanceIsExact(@ForAll("cases") ProvenanceCase c) throws Exception {
        Path work = Files.createTempDirectory("exact-provenance-");
        try {
            verify(c, work);
        } finally {
            deleteRecursively(work);
        }
        Statistics.label("revision").collect(c.kind());
        Statistics.label("revision").coverage(k -> {
            k.check(Kind.SHA1).percentage(p -> p >= 20);
            k.check(Kind.SHA256).percentage(p -> p >= 20);
            k.check(Kind.UNPINNED).percentage(p -> p >= 5);
        });
        Statistics.label("namespace segments").collect(c.namespace().split("\\.").length > 1 ? "multi" : "single");
        Statistics.label("namespace segments").coverage(k -> k.check("multi").percentage(p -> p >= 30));
        Statistics.label("revision digits only").collect(c.revisionBase().chars().allMatch(Character::isDigit));
        Statistics.label("revision digits only").coverage(k -> k.check(true).percentage(p -> p >= 5));
        Statistics.label("underscore-led identifier").collect(
            Stream.concat(Stream.of(c.name()), Stream.of(c.namespace().split("\\."))).anyMatch(s -> s.startsWith("_")));
    }

    private static void verify(ProvenanceCase c, Path work) throws Exception {
        String serviceId = c.serviceId();
        Path models = work.resolve("model");
        Map<String, byte[]> modelFiles = c.modelFiles();
        for (Map.Entry<String, byte[]> file : modelFiles.entrySet()) {
            Path target = models.resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.write(target, file.getValue());
        }
        Path out = work.resolve("out");
        Path sourceRoot = out.resolve("src");
        Path resourceRoot = out.resolve("resources");
        AwsCodegenRequest request = new AwsCodegenRequest(serviceId, c.packageName(), models, sourceRoot,
                                                          resourceRoot, c.pinnedRevision());

        // Nothing but the exact untruncated revision and the exact modeled service ID is ever accepted as input.
        for (String invalid : invalidRevisions(c.revisionBase())) {
            assertThrows(AwsCodegenException.class, () -> request.withModelRevision(invalid),
                         () -> "revision " + quoted(invalid) + " must be rejected");
        }
        for (String nonAbsolute : nonAbsoluteForms(c)) {
            assertThrows(AwsCodegenException.class,
                         () -> new AwsCodegenRequest(nonAbsolute, c.packageName(), models, sourceRoot, resourceRoot,
                                                     c.pinnedRevision()),
                         () -> "service ID " + quoted(nonAbsolute) + " must be rejected");
        }
        for (String other : otherServiceIds(c)) {
            AwsCodegenRequest otherRequest = new AwsCodegenRequest(other, c.packageName(), models, sourceRoot,
                                                                   resourceRoot, c.pinnedRevision());
            assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(otherRequest, List.of()),
                         () -> "service " + other + " is not the modeled service " + serviceId);
            assertEquals(Set.of(), GeneratedTree.read(out).paths(),
                         () -> "nothing may be published when generating " + other);
        }

        AwsCodegenResult result = AwsCodegen.generate(request, List.of());
        String digest = result.modelDigest();
        String revision = c.kind() == Kind.UNPINNED ? digest : c.revisionBase();
        assertEquals(revision, result.modelRevision());

        // The published provenance, as bytes, at the resource name the runtime reader looks up.
        GeneratedTree resources = GeneratedTree.read(resourceRoot);
        String provenancePath = RESOURCE_DIRECTORY + c.packageName() + "/model-provenance.json";
        assertEquals(provenancePath, ModelProvenance.resourceName(c.packageName()));
        byte[] provenanceBytes = resources.content(provenancePath);
        String provenanceText = new String(provenanceBytes, StandardCharsets.UTF_8);
        assertEquals(1, occurrences(provenanceText, quoted(serviceId)), provenanceText);
        assertTrue(provenanceText.contains(quoted(revision)), provenanceText);

        // Independent JSON parser.
        ObjectNode provenanceNode = Node.parse(provenanceText).expectObjectNode();
        assertEquals(serviceId, provenanceNode.expectStringMember("service").getValue());
        ObjectNode modelNode = provenanceNode.expectObjectMember("model");
        assertEquals(revision, modelNode.expectStringMember("revision").getValue());
        assertEquals(c.kind() == Kind.UNPINNED ? "model-digest" : "git-commit",
                     modelNode.expectStringMember("revisionKind").getValue());
        assertEquals(digest, modelNode.expectStringMember("digest").getValue());
        List<ModelFile> expectedFiles = modelFiles.entrySet().stream()
                                                  .map(f -> new ModelFile(f.getKey(), sha256(f.getValue())))
                                                  .sorted(Comparator.comparing(ModelFile::path))
                                                  .toList();
        assertEquals(expectedFiles, modelNode.expectArrayMember("files").getElements().stream()
            .map(Node::expectObjectNode)
            .map(f -> new ModelFile(f.expectStringMember("path").getValue(), f.expectStringMember("digest").getValue()))
            .toList());

        // Runtime reader.
        ModelProvenance provenance = ModelProvenance.parse(provenanceBytes);
        assertEquals(serviceId, provenance.serviceShapeId());
        assertEquals(ShapeId.from(serviceId), provenance.serviceId());
        assertEquals(serviceId, provenance.serviceId().toString());
        assertEquals(c.namespace(), provenance.serviceId().getNamespace());
        assertEquals(c.name(), provenance.serviceId().getName());
        assertEquals(revision, provenance.modelRevision());
        assertEquals(c.kind() == Kind.UNPINNED ? RevisionKind.MODEL_DIGEST : RevisionKind.GIT_COMMIT,
                     provenance.revisionKind());
        assertEquals(digest, provenance.modelDigest());
        assertEquals(expectedFiles, provenance.modelFiles());

        // The runtime reader rejects every abbreviated or altered revision and every non-absolute service ID.
        if (c.kind() != Kind.UNPINNED) {
            for (String invalid : invalidRevisions(revision)) {
                assertParseRejects(provenanceText.replace(quoted(revision), jsonString(invalid)),
                                   "revision " + quoted(invalid));
            }
        }
        for (String nonAbsolute : nonAbsoluteForms(c)) {
            assertParseRejects(provenanceText.replace(quoted(serviceId), jsonString(nonAbsolute)),
                               "service " + quoted(nonAbsolute));
        }

        // The generation manifest names the same service and the published provenance, with its exact digest.
        ObjectNode manifest = Node.parse(resources.text(RESOURCE_DIRECTORY + c.packageName()
                                                        + "/generation-manifest.json")).expectObjectNode();
        assertEquals(serviceId, manifest.expectStringMember("service").getValue());
        assertEquals(provenancePath, manifest.expectStringMember("provenance").getValue());
        ObjectNode settings = manifest.expectObjectMember("configuration").expectObjectMember("settings");
        assertEquals(serviceId, settings.expectStringMember("service").getValue());
        assertEquals(c.packageName(), settings.expectStringMember("packageName").getValue());
        List<String> provenanceDigests = manifest.expectArrayMember("files").getElements().stream()
            .map(Node::expectObjectNode)
            .filter(f -> f.expectStringMember("root").getValue().equals("resources")
                         && f.expectStringMember("path").getValue().equals(provenancePath))
            .map(f -> f.expectStringMember("digest").getValue())
            .toList();
        assertEquals(List.of(sha256(provenanceBytes)), provenanceDigests);

        // The generated identity type records the same values and reads the published provenance at run time.
        String serviceInfoPath = c.packageName().replace('.', '/') + "/" + SERVICE_INFO + ".java";
        String serviceInfoSource = GeneratedTree.read(sourceRoot).text(serviceInfoPath);
        assertTrue(serviceInfoSource.contains("SERVICE_SHAPE_ID = " + quoted(serviceId) + ";"), serviceInfoSource);
        assertTrue(serviceInfoSource.contains("MODEL_REVISION = " + quoted(revision) + ";"), serviceInfoSource);
        verifyCompiledServiceInfo(c, work, sourceRoot.resolve(serviceInfoPath), resourceRoot, revision, digest,
                                  provenance);
    }

    private static void verifyCompiledServiceInfo(ProvenanceCase c, Path work, Path source, Path resourceRoot,
                                                  String revision, String digest, ModelProvenance expected)
        throws Exception {
        Path classes = Files.createDirectories(work.resolve("classes"));
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int status = JAVAC.run(null, diagnostics, diagnostics, "-proc:none", "-implicit:none", "--release", "21",
                               "-classpath", RUNTIME_CLASS_PATH, "-d", classes.toString(), source.toString());
        assertEquals(0, status, () -> "javac failed:\n" + diagnostics.toString(StandardCharsets.UTF_8));

        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL(),
                                                                   resourceRoot.toUri().toURL()},
                                                        ExactProvenancePropertyTest.class.getClassLoader())) {
            Class<?> type = loader.loadClass(c.packageName() + "." + SERVICE_INFO);
            assertEquals(loader, type.getClassLoader());
            assertEquals(c.serviceId(), staticString(type, "SERVICE_SHAPE_ID"));
            assertEquals(revision, staticString(type, "MODEL_REVISION"));
            assertEquals(digest, staticString(type, "MODEL_DIGEST"));
            Method provenance = type.getDeclaredMethod("provenance");
            provenance.setAccessible(true);
            Object loaded;
            try {
                loaded = provenance.invoke(null);
            } catch (InvocationTargetException e) {
                throw new AssertionError("GeneratedServiceInfo.provenance() failed", e.getCause());
            }
            assertEquals(expected, loaded);
            assertEquals(c.serviceId(), ((ModelProvenance) loaded).serviceShapeId());
            assertEquals(revision, ((ModelProvenance) loaded).modelRevision());

            // The identity check neither conflates service IDs nor accepts a prefix or variant of the revision.
            for (String other : otherServiceIds(c)) {
                assertThrows(IllegalStateException.class, () -> ModelProvenance.load(type, other, revision),
                             () -> "provenance of " + c.serviceId() + " must not be accepted as " + other);
            }
            List<String> otherRevisions = new ArrayList<>(invalidRevisions(revision));
            if (c.kind() == Kind.SHA256) {
                // A well-formed 40-digit prefix of a 64-digit commit is still a different revision.
                otherRevisions.add(revision.substring(0, 40));
            }
            for (String other : otherRevisions) {
                assertThrows(IllegalStateException.class, () -> ModelProvenance.load(type, c.serviceId(), other),
                             () -> "provenance at " + revision + " must not be accepted as " + quoted(other));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Variants derived from the generated inputs.

    /** Every abbreviated, extended, case-changed, non-hex, padded or otherwise altered form of {@code revision}. */
    static List<String> invalidRevisions(String revision) {
        Set<String> variants = new LinkedHashSet<>();
        for (int length : new int[] {0, 7, 12, 39, 63}) {
            if (length < revision.length()) {
                variants.add(revision.substring(0, length));
            }
        }
        if (revision.length() == 64) {
            variants.add(revision.substring(0, 41));
        }
        variants.add(revision + "0");
        variants.add(revision + revision.charAt(revision.length() - 1));
        String upper = revision.toUpperCase(Locale.ROOT);
        variants.add(upper.equals(revision) ? "A" + revision.substring(1) : upper);
        int middle = revision.length() / 2;
        for (char c : new char[] {'g', 'G', '-', 'z'}) {
            variants.add(revision.substring(0, middle) + c + revision.substring(middle + 1));
        }
        variants.add(" " + revision);
        variants.add(revision + " ");
        variants.add(revision + "\n");
        variants.add("\t" + revision);
        variants.add("sha256:" + revision);
        variants.remove(revision);
        return List.copyOf(variants);
    }

    /**
     * Valid absolute shape IDs that differ from the modeled service ID in the case of one character of the namespace
     * or of the name, or by an added or dropped namespace segment.
     */
    static List<String> otherServiceIds(ProvenanceCase c) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(flipFirstLetter(c.namespace()) + "#" + c.name());
        ids.add(c.namespace() + "#" + flipFirstLetter(c.name()));
        ids.add(c.namespace() + ".x#" + c.name());
        int dot = c.namespace().indexOf('.');
        if (dot > 0) {
            ids.add(c.namespace().substring(dot + 1) + "#" + c.name());
        }
        ids.remove(c.serviceId());
        return List.copyOf(ids);
    }

    /** Forms of the service ID that are not an absolute Smithy shape ID. */
    static List<String> nonAbsoluteForms(ProvenanceCase c) {
        String id = c.serviceId();
        return List.of(c.name(), "#" + c.name(), c.namespace() + "#", id + "$member", " " + id, id + " ", id + "\n",
                       c.namespace() + ".#" + c.name(), c.namespace() + "#" + c.name() + "#" + c.name(), "");
    }

    private static String flipFirstLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isLetter(ch)) {
                char flipped = Character.isUpperCase(ch) ? Character.toLowerCase(ch) : Character.toUpperCase(ch);
                return value.substring(0, i) + flipped + value.substring(i + 1);
            }
        }
        throw new IllegalArgumentException("no letter in " + value);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators.

    enum Kind {
        SHA1(40), SHA256(64), UNPINNED(64);

        private final int length;

        Kind(int length) {
            this.length = length;
        }
    }

    /**
     * One generation input.
     *
     * @param namespace the service's namespace
     * @param name the service's name
     * @param packageName the Java package of the generated service
     * @param kind whether the generation is pinned to a SHA-1 or SHA-256 commit, or unpinned
     * @param revisionBase the pinned revision, or (unpinned) a commit ID from which only invalid variants are derived
     * @param serviceFile relative path of the model file declaring the service
     * @param operationFile relative path of the model file declaring its operation
     */
    record ProvenanceCase(String namespace, String name, String packageName, Kind kind, String revisionBase,
                          String serviceFile, String operationFile) {

        String serviceId() {
            return namespace + "#" + name;
        }

        /** The revision of the request, or {@code null} when unpinned. */
        String pinnedRevision() {
            return kind == Kind.UNPINNED ? null : revisionBase;
        }

        Map<String, byte[]> modelFiles() {
            String header = "$version: \"2\"\nnamespace " + namespace + "\n\n";
            Map<String, byte[]> files = new TreeMap<>();
            files.put(serviceFile, (header
                                    + "@aws.protocols#awsJson1_0\n"
                                    + "@aws.auth#sigv4(name: \"provenance\")\n"
                                    + "service " + name + " {\n"
                                    + "    version: \"2024-01-01\"\n"
                                    + "    operations: [" + OPERATION + "]\n"
                                    + "}\n").getBytes(StandardCharsets.UTF_8));
            files.put(operationFile, (header
                                      + "operation " + OPERATION + " {\n"
                                      + "    input := { id: String }\n"
                                      + "    output := { status: String }\n"
                                      + "}\n").getBytes(StandardCharsets.UTF_8));
            return files;
        }

        @Override
        public String toString() {
            return "ProvenanceCase[service=" + serviceId() + ", package=" + packageName + ", " + kind + " "
                   + revisionBase + ", files=" + serviceFile + ", " + operationFile + "]";
        }
    }

    @Provide
    Arbitrary<ProvenanceCase> cases() {
        Arbitrary<Kind> kinds = Arbitraries.frequency(Tuple.of(4, Kind.SHA1),
                                                      Tuple.of(4, Kind.SHA256),
                                                      Tuple.of(1, Kind.UNPINNED));
        Arbitrary<String> serviceFiles = modelPaths();
        Arbitrary<String> operationFiles = modelPaths();
        return Combinators.combine(namespaces(), serviceNames(), packages(),
                                   kinds.flatMap(k -> hex(k.length).map(r -> Map.entry(k, r))),
                                   serviceFiles, operationFiles)
                          .filter((ns, name, pkg, revision, serviceFile, operationFile) ->
                                      !serviceFile.equals(operationFile))
                          .as((ns, name, pkg, revision, serviceFile, operationFile) ->
                                  new ProvenanceCase(ns, name, pkg, revision.getKey(), revision.getValue(),
                                                     serviceFile, operationFile));
    }

    /** Full-length lower-case hex, biased to values a numeric, trimming or case-folding codec would alter. */
    private static Arbitrary<String> hex(int length) {
        return Arbitraries.frequencyOf(
            Tuple.of(6, Arbitraries.strings().withChars(HEX).ofLength(length)),
            Tuple.of(1, Arbitraries.strings().withChars(DIGITS).ofLength(length)),
            Tuple.of(1, Arbitraries.integers().between(1, length - 1).flatMap(
                zeros -> Arbitraries.strings().withChars(HEX).ofLength(length - zeros).map(t -> "0".repeat(zeros) + t))),
            Tuple.of(1, Arbitraries.of('0', 'f', 'a', '9').map(ch -> String.valueOf(ch).repeat(length))));
    }

    /** Smithy identifiers: a letter, optionally after leading underscores, then letters, digits and underscores. */
    private static Arbitrary<String> identifiers(int maxTail) {
        Arbitrary<String> underscores = Arbitraries.frequency(Tuple.of(6, ""),
                                                              Tuple.of(1, "_"),
                                                              Tuple.of(1, "__"));
        Arbitrary<Character> first = Arbitraries.chars().with((UPPER + LOWER).toCharArray());
        Arbitrary<String> tail = Arbitraries.strings().withChars(IDENTIFIER_CHARS).ofMaxLength(maxTail);
        return Combinators.combine(underscores, first, tail).as((u, f, t) -> u + f + t);
    }

    private static Arbitrary<String> namespaces() {
        return identifiers(20).list().ofMinSize(1).ofMaxSize(5)
                              .map(segments -> String.join(".", segments))
                              // The prelude and the AWS/Smithy trait models occupy these namespace roots.
                              .filter(ns -> {
                                  String root = ns.split("\\.")[0].toLowerCase(Locale.ROOT);
                                  return !root.equals("smithy") && !root.equals("aws");
                              });
    }

    private static Arbitrary<String> serviceNames() {
        return identifiers(40).filter(name -> MODEL_SHAPE_NAMES.stream().noneMatch(name::equalsIgnoreCase));
    }

    private static Arbitrary<String> packages() {
        Arbitrary<String> segment = Combinators.combine(Arbitraries.chars().with(LOWER.toCharArray()),
                                                        Arbitraries.strings().withChars(LOWER + DIGITS + "_")
                                                                   .ofMaxLength(12))
                                               .as((f, t) -> f + t);
        return segment.list().ofMinSize(1).ofMaxSize(3)
                      .map(segments -> "software.amazon.awssdk.services." + String.join(".", segments))
                      .filter(pkg -> SourceVersion.isName(pkg, SourceVersion.RELEASE_21));
    }

    private static Arbitrary<String> modelPaths() {
        Arbitrary<String> segment = Combinators.combine(Arbitraries.chars().with(LOWER.toCharArray()),
                                                        Arbitraries.strings().withChars(LOWER + DIGITS + "-")
                                                                   .ofMaxLength(10))
                                               .as((f, t) -> f + t);
        return Combinators.combine(segment.list().ofMaxSize(2), segment)
                          .as((directories, file) -> Stream.concat(directories.stream(), Stream.of(file + ".smithy"))
                                                           .collect(Collectors.joining("/")));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers.

    private static void assertParseRejects(String json, String what) {
        assertThrows(IllegalArgumentException.class,
                     () -> ModelProvenance.parse(json.getBytes(StandardCharsets.UTF_8)),
                     () -> "the runtime reader must reject " + what + ":\n" + json);
    }

    private static String staticString(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private static String quoted(String value) {
        return "\"" + value + "\"";
    }

    /** {@code value} as a JSON string literal. */
    private static String jsonString(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\t' -> result.append("\\t");
                default -> result.append(ch);
            }
        }
        return result.append('"').toString();
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + 1)) {
            count++;
        }
        return count;
    }

    private static String sha256(byte[] content) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The class path of {@code aws-core}'s provenance reader and the types its API refers to. */
    private static String runtimeClassPath() {
        Set<String> entries = new LinkedHashSet<>();
        for (Class<?> type : List.of(ModelProvenance.class, ShapeId.class, Document.class, JsonCodec.class)) {
            try {
                entries.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return String.join(File.pathSeparator, entries);
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to delete " + root, e);
        }
    }
}
