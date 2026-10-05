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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.BEHAVIOR;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.BINARY;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.SOURCE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.tools.paritymanifest.BaselineIdentity;
import software.amazon.awssdk.tools.paritymanifest.Classification;
import software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension;
import software.amazon.awssdk.tools.paritymanifest.EntryKind;
import software.amazon.awssdk.tools.paritymanifest.ManifestEntry;
import software.amazon.awssdk.tools.paritymanifest.ManifestJson;
import software.amazon.awssdk.tools.paritymanifest.MigrationKind;
import software.amazon.awssdk.tools.paritymanifest.ParityManifest;
import software.amazon.awssdk.tools.paritymanifest.SourceLocation;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.Kind;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Fixture generation over a small synthetic V2 baseline compiled in-test: which fixtures each classification and
 * dimension yields, the generated callers (which must be valid V2 code: they compile, and link callers link, against
 * the baseline they were generated from), required supertypes, problems, and the deterministic JSON form.
 */
class CompatibilityFixtureGeneratorTest {
    private static final JavaCompiler JAVAC = ToolProvider.getSystemJavaCompiler();
    private static final BaselineIdentity BASELINE =
        new BaselineIdentity("2.54.0", "abc123", null, null, "parity-manifest/test");

    private static final Map<String, String> V2_SOURCES = Map.of(
        "p/Base.java", "package p; public class Base extends RuntimeException { public Base(String m) { super(m); } }",
        "p/Err.java", "package p; public final class Err extends Base implements Marker {"
                      + " public Err() { super(\"e\"); } }",
        "p/Marker.java", "package p; public interface Marker {}",
        "p/Api.java", "package p; public interface Api extends AutoCloseable {"
                      + " String get(int key, java.util.function.Consumer<String> c);"
                      + " static Api create() { return null; }"
                      + " interface Nested { long size(); } }",
        "p/Ext.java", "package p; public abstract class Ext { protected Ext(String s) {}"
                      + " protected void hook() {} public static final String NAME = \"n\";"
                      + " public static Ext INSTANCE; }",
        "p/Legacy.java", "package p; public final class Legacy { public Legacy() {} }",
        "p/Gen.java", "package p; public interface Gen<V> { void put(java.util.List<?> l, String s);"
                      + " <T> void put(java.util.List<T> l, T t); void get(String key); void get(V item); }",
        "p/Hidden.java", "package p; public class Hidden { public Hidden(Impl i) {} public Hidden() {}"
                         + " private static final class Impl {} }");

    @TempDir
    static Path temp;
    private static Path v2;
    private static BaselineClasses baseline;

    @BeforeAll
    static void compileBaseline() throws IOException {
        Path sources = temp.resolve("v2-src");
        List<Path> files = new ArrayList<>();
        for (Map.Entry<String, String> s : V2_SOURCES.entrySet()) {
            Path file = sources.resolve(s.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, s.getValue(), StandardCharsets.UTF_8);
            files.add(file);
        }
        v2 = temp.resolve("v2-classes");
        assertEquals(List.of(), compile(files, "", v2));
        baseline = BaselineClasses.read(List.of(v2));
    }

    @Test
    void eachClassificationAndDimensionYieldsItsFixtureKinds() throws Exception {
        CompatibilityFixtures fixtures = generate(
            preserved("api:p.Api", SOURCE, BINARY),
            preserved("api:p.Api#get(ILjava/util/function/Consumer;)Ljava/lang/String;", SOURCE),
            preserved("api:p.Base", SOURCE),
            preserved("api:p.Err", SOURCE, BINARY),
            preserved("api:p.Marker", SOURCE, BINARY),
            preserved("api:p.Ext#hook()V", SOURCE, BINARY),
            preserved("api:p.Ext#NAME:Ljava/lang/String;", BINARY),
            excluded("api:p.Legacy"),
            adapted("api:p.Api#create()Lp/Api;"),
            withEvidence(preserved("behavior:retry.mode", BEHAVIOR), "RetryTest"));

        assertEquals(Set.of(
            "api:p.Api COMPILE source", "api:p.Api HIERARCHY source", "api:p.Api LINK binary",
            "api:p.Api HIERARCHY binary",
            "api:p.Api#create()Lp/Api; COMPILE_REJECTED", "api:p.Api#create()Lp/Api; LINK_REJECTED",
            "api:p.Api#create()Lp/Api; BEHAVIOR",
            "api:p.Api#get(ILjava/util/function/Consumer;)Ljava/lang/String; COMPILE source",
            "api:p.Api#get(ILjava/util/function/Consumer;)Ljava/lang/String; SIGNATURE source",
            "api:p.Base COMPILE source", "api:p.Base HIERARCHY source",
            "api:p.Err COMPILE source", "api:p.Err HIERARCHY source", "api:p.Err LINK binary",
            "api:p.Err HIERARCHY binary",
            "api:p.Ext#NAME:Ljava/lang/String; LINK binary", "api:p.Ext#NAME:Ljava/lang/String; SIGNATURE binary",
            "api:p.Ext#hook()V COMPILE source", "api:p.Ext#hook()V SIGNATURE source",
            "api:p.Ext#hook()V SIGNATURE binary",
            "api:p.Legacy COMPILE_REJECTED", "api:p.Legacy LINK_REJECTED",
            "api:p.Marker COMPILE source", "api:p.Marker HIERARCHY source", "api:p.Marker LINK binary",
            "api:p.Marker HIERARCHY binary",
            "behavior:retry.mode BEHAVIOR behavior"),
                     fixtures.fixtures().stream().map(CompatibilityFixture::id).collect(Collectors.toSet()));
        assertEquals(List.of(new CompatibilityFixtures.Note("api:p.Ext#hook()V", "no link caller: javac reaches a "
            + "protected member through a synthetic lambda, so no method handle links it directly; the binary "
            + "signature fixture verifies it")), fixtures.notes());
        assertEquals(List.of("RetryTest"), fixture(fixtures, "behavior:retry.mode BEHAVIOR behavior").evidenceIds());
        assertNull(fixture(fixtures, "behavior:retry.mode BEHAVIOR behavior").target());
    }

    @Test
    void requiredSupertypesAreJdkTypesPreservedExceptionSuperclassesAndBinaryPreservedTypes() throws Exception {
        CompatibilityFixtures fixtures = generate(preserved("api:p.Err", SOURCE, BINARY),
                                                  preserved("api:p.Base", SOURCE),
                                                  preserved("api:p.Marker", BINARY));
        List<String> jdkAndExceptionChain = List.of("java.io.Serializable", "java.lang.Exception",
                                                    "java.lang.RuntimeException", "java.lang.Throwable", "p.Base");
        assertEquals(jdkAndExceptionChain, fixture(fixtures, "api:p.Err HIERARCHY source").supertypes());
        // The binary dimension adds the binary-preserved interface, which source preservation does not require.
        List<String> binary = new ArrayList<>(jdkAndExceptionChain);
        binary.add("p.Marker");
        assertEquals(binary, fixture(fixtures, "api:p.Err HIERARCHY binary").supertypes());
        assertTrue(fixture(fixtures, "api:p.Err COMPILE source").source()
                       .contains("p.Base supertype4 = (p.Err) null;"));
    }

    @Test
    void callersUseTheV2SurfaceWithExactlyTheRecordedDescriptor() throws Exception {
        CompatibilityFixtures fixtures = generate(
            preserved("api:p.Api#get(ILjava/util/function/Consumer;)Ljava/lang/String;", SOURCE, BINARY),
            preserved("api:p.Api$Nested#size()J", SOURCE),
            preserved("api:p.Ext#<init>(Ljava/lang/String;)V", SOURCE),
            preserved("api:p.Ext#INSTANCE:Lp/Ext;", SOURCE, BINARY),
            preserved("api:p.Legacy#<init>()V", SOURCE, BINARY));
        String get = "api:p.Api#get(ILjava/util/function/Consumer;)Ljava/lang/String;";
        assertTrue(fixture(fixtures, get + " COMPILE source").source().contains(
            "java.lang.String result = ((p.Api) null).get((int) 0, (java.util.function.Consumer) null);"));
        assertTrue(fixture(fixtures, get + " LINK binary").source().contains(
            "Target target = p.Api::get;"));
        assertTrue(fixture(fixtures, get + " LINK binary").source().contains(
            "java.lang.String apply(p.Api p0, int p1, java.util.function.Consumer p2);"));
        assertTrue(fixture(fixtures, "api:p.Api$Nested#size()J COMPILE source").source()
                       .contains("long result = ((p.Api.Nested) null).size();"));
        // A public constructor of an abstract class, like a protected member, is used from a subclass.
        assertTrue(fixture(fixtures, "api:p.Ext#<init>(Ljava/lang/String;)V COMPILE source").source()
                       .contains("public abstract class CompatCaller extends p.Ext {"));
        assertTrue(fixture(fixtures, "api:p.Legacy#<init>()V LINK binary").source()
                       .contains("Target target = p.Legacy::new;"));
        assertTrue(fixture(fixtures, "api:p.Ext#INSTANCE:Lp/Ext; LINK binary").source()
                       .contains("java.lang.Object value = p.Ext.INSTANCE;"));
    }

    /** Every generated caller is valid V2 code; link callers also link against the baseline they came from. */
    @Test
    void everyCallerCompilesAndLinksAgainstItsOwnBaseline() throws Exception {
        CompatibilityFixtures fixtures = generate(
            preserved("api:p.Api", SOURCE, BINARY),
            preserved("api:p.Api#get(ILjava/util/function/Consumer;)Ljava/lang/String;", SOURCE, BINARY),
            preserved("api:p.Api$Nested#size()J", SOURCE, BINARY),
            preserved("api:p.Err", SOURCE, BINARY),
            preserved("api:p.Ext#hook()V", SOURCE),
            preserved("api:p.Ext#<init>(Ljava/lang/String;)V", SOURCE),
            preserved("api:p.Ext#NAME:Ljava/lang/String;", SOURCE, BINARY),
            preserved("api:p.Ext#INSTANCE:Lp/Ext;", SOURCE, BINARY),
            preserved("api:p.Legacy#<init>()V", SOURCE, BINARY),
            preserved("api:p.Gen#get(Ljava/lang/String;)V", SOURCE, BINARY),
            excluded("api:p.Legacy"),
            adapted("api:p.Api#create()Lp/Api;"));
        int compiled = 0;
        for (CompatibilityFixture f : fixtures.fixtures()) {
            if (f.source() == null) {
                continue;
            }
            Path classes = temp.resolve("callers").resolve(Integer.toString(compiled++));
            Path file = classes.resolveSibling(classes.getFileName() + "-src").resolve(CompatibilityFixture.CALLER_CLASS
                                                                                     + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, f.source(), StandardCharsets.UTF_8);
            assertEquals(List.of(), compile(List.of(file), v2.toString(), classes), f.id() + "\n" + f.source());
            if (f.kind() == Kind.LINK || f.kind() == Kind.LINK_REJECTED) {
                try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL(), v2.toUri().toURL()},
                                                                ClassLoader.getPlatformClassLoader())) {
                    Class.forName(CompatibilityFixture.CALLER_CLASS, true, loader).getMethod("link").invoke(null);
                }
            }
        }
        assertEquals(fixtures.fixtures().stream().filter(f -> f.source() != null).count(), compiled);
        assertTrue(compiled >= 15, "expected compile and link callers, got " + compiled);
    }

    @Test
    void membersV2SourceCannotNameExactlyGetOnlySignatureFixtures() throws Exception {
        String put = "api:p.Gen#put(Ljava/util/List;Ljava/lang/String;)V";
        String hidden = "api:p.Hidden#<init>(Lp/Hidden$Impl;)V";
        // An overload taking a class type variable is not ambiguous: the raw receiver erases it to Object.
        String get = "api:p.Gen#get(Ljava/lang/String;)V";
        CompatibilityFixtures fixtures = generate(preserved(put, SOURCE, BINARY), preserved(hidden, SOURCE, BINARY),
                                                  preserved(get, SOURCE, BINARY));
        assertEquals(Set.of(put + " SIGNATURE source", put + " SIGNATURE binary", hidden + " SIGNATURE source",
                            hidden + " SIGNATURE binary", get + " COMPILE source", get + " SIGNATURE source",
                            get + " LINK binary", get + " SIGNATURE binary"),
                     fixtures.fixtures().stream().map(CompatibilityFixture::id).collect(Collectors.toSet()));
        String rival = "a call with typed null arguments is ambiguous with the generic method p.Gen#put("
                       + "Ljava/util/List;Ljava/lang/Object;)V";
        String inaccessible = "the descriptor type p.Hidden$Impl is not accessible to V2 callers";
        assertEquals(List.of(new CompatibilityFixtures.Note(put, "no compile caller: " + rival),
                             new CompatibilityFixtures.Note(put, "no link caller: " + rival
                                                                 + "; the binary signature fixture verifies it"),
                             new CompatibilityFixtures.Note(hidden, "no compile caller: " + inaccessible),
                             new CompatibilityFixtures.Note(hidden, "no link caller: " + inaccessible
                                                                    + "; the binary signature fixture verifies it")),
                     fixtures.notes());
    }

    @Test
    void rejectionFixturesCarryTheEntrySymbolsAndTheLinkedDiagnostic() throws Exception {
        CompatibilityFixtures fixtures = generate(excluded("api:p.Api$Nested#size()J"));
        CompatibilityFixture rejected = fixture(fixtures, "api:p.Api$Nested#size()J COMPILE_REJECTED");
        assertEquals(List.of("Api", "Nested", "p", "size"), rejected.symbols());
        assertEquals("[api:p.Api$Nested#size()J] EXCLUDED V2 api:p.Api$Nested#size()J -> no V3 replacement: "
                     + "remove it", rejected.diagnostic());
        assertEquals(rejected.diagnostic(), fixture(fixtures, "api:p.Api$Nested#size()J LINK_REJECTED").diagnostic());
    }

    @Test
    void everyProblemIsReportedAtOnce() {
        CompatibilityFixtureException e = assertThrows(CompatibilityFixtureException.class, () -> generate(
            List.of("api:p.*", "api:q.Missing*", "service:*", "behavior:*"),
            preserved("api:p.Gone", SOURCE),
            preserved("api:p.Api#absent()V", SOURCE),
            preserved("service:dynamodb", SOURCE),
            preserved("behavior:no.evidence", BEHAVIOR)));
        assertEquals(List.of(
            "[api:p.Api#absent()V] the baseline class p.Api declares no method absent()V",
            "[api:p.Gone] the baseline class path lacks p.Gone",
            "[behavior:no.evidence] the behavior claim names no evidence ID, so no behavior fixture can verify it",
            "[scope] 'api:q.Missing*' matches no manifest entry",
            "[service:dynamodb] no compatibility fixture is applicable to this PRESERVED SERVICE entry (the source "
            + "dimension of SERVICE entries is not verified by generated fixtures (launch-surface gate)); claim a "
            + "verifiable dimension, name evidence, or remove it from the test scope"), e.problems());
    }

    @Test
    void theJsonFormIsDeterministicAndCompleteAndEvidenceHooksBindSuitesToEntries() throws Exception {
        CompatibilityFixtures fixtures = generate(preserved("api:p.Marker", SOURCE), excluded("api:p.Legacy"));
        assertEquals(fixtures.toJson(), generate(excluded("api:p.Legacy"), preserved("api:p.Marker", SOURCE))
            .toJson());
        ObjectNode json = Node.parse(fixtures.toJson()).expectObjectNode();
        assertEquals(1, json.expectNumberMember("schemaVersion").getValue().intValue());
        assertEquals("2.54.0", json.expectObjectMember("baseline").expectStringMember("v2Version").getValue());
        assertEquals(List.of("api:p.Legacy COMPILE_REJECTED", "api:p.Legacy LINK_REJECTED",
                             "api:p.Marker COMPILE source", "api:p.Marker HIERARCHY source"),
                     json.expectArrayMember("fixtures").getElementsAs(ObjectNode.class).stream()
                         .map(f -> f.expectStringMember("id").getValue()).toList());
        assertEquals(1, json.expectObjectMember("summary").expectNumberMember("LINK_REJECTED").getValue().intValue());
        assertEquals("CompileSuite api:p.Legacy\nCompileSuite api:p.Marker\nLinkSuite api:p.Legacy\n",
                     fixtures.evidenceHooks(Map.of(Kind.COMPILE, "CompileSuite", Kind.COMPILE_REJECTED,
                                                   "CompileSuite", Kind.LINK_REJECTED, "LinkSuite")));
    }

    @Test
    void theCliGeneratesFromAManifestDocumentAndReportsProblemsAndUsageErrors() throws Exception {
        Path manifest = temp.resolve("cli/manifest.json");
        ManifestJson.write(new ParityManifest(BASELINE, null, List.of(preserved("api:p.Marker", SOURCE),
                                                                      excluded("api:p.Legacy"))), manifest);
        Path scope = Files.writeString(temp.resolve("cli/scope.txt"), "api:p.*\n", StandardCharsets.UTF_8);
        Path output = temp.resolve("cli/fixtures.json");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        String[] args = {"--manifest", manifest.toString(), "--baseline-classpath", v2.toString(), "--scope",
                         scope.toString(), "--output", output.toString()};
        assertEquals(0, CompatibilityFixturesCli.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                                                     new PrintStream(err, true, StandardCharsets.UTF_8)), err.toString());
        assertEquals(generate(List.of("api:p.*"), preserved("api:p.Marker", SOURCE), excluded("api:p.Legacy")).toJson(),
                     Files.readString(output, StandardCharsets.UTF_8));

        Files.writeString(scope, "api:q.*\n", StandardCharsets.UTF_8);
        assertEquals(1, CompatibilityFixturesCli.run(args, new PrintStream(out), new PrintStream(err)));
        assertEquals(2, CompatibilityFixturesCli.run(new String[] {"--manifest"}, new PrintStream(out),
                                                     new PrintStream(err)));
    }

    @Test
    void scopeFilesHoldOneGlobPerLineWithComments() {
        assertEquals(List.of("api:p.Api#get(*", "api:p.*", "!api:p.Legacy*"),
                     CompatibilityFixturesCli.parseScope("# header\n\napi:p.Api#get(*   # trailing\n  api:p.*\n"
                                                         + "!api:p.Legacy*  # known gap\n"));
    }

    @Test
    void exclusionsRemoveIncludedEntriesAndMustExcludeOne() throws Exception {
        List<String> scope = List.of("api:p.*", "!api:p.Legacy*");
        CompatibilityFixtures fixtures = generate(scope, preserved("api:p.Marker", SOURCE), excluded("api:p.Legacy"));
        assertEquals(List.of("api:p.Marker COMPILE source", "api:p.Marker HIERARCHY source"),
                     fixtures.fixtures().stream().map(CompatibilityFixture::id).toList());
        assertEquals(scope, fixtures.scope());
        assertTrue(CompatibilityFixtureGenerator.inScope("api:p.Marker", scope));
        assertFalse(CompatibilityFixtureGenerator.inScope("api:p.Legacy", scope));
        assertFalse(CompatibilityFixtureGenerator.inScope("api:q.Other", scope));

        CompatibilityFixtureException e = assertThrows(CompatibilityFixtureException.class, () -> generate(
            List.of("api:p.Marker", "!api:p.Legacy", "!api:q.*"), preserved("api:p.Marker", SOURCE),
            excluded("api:p.Legacy")));
        assertEquals(List.of("[scope] '!api:p.Legacy' excludes no entry of the scope",
                             "[scope] '!api:q.*' excludes no entry of the scope"), e.problems());
    }

    // ---- helpers ----

    private static CompatibilityFixtures generate(ManifestEntry... entries) throws CompatibilityFixtureException {
        return generate(List.of("*"), entries);
    }

    private static CompatibilityFixtures generate(List<String> scope, ManifestEntry... entries)
        throws CompatibilityFixtureException {
        return new CompatibilityFixtureGenerator(new ParityManifest(BASELINE, null, List.of(entries)), baseline)
            .generate(scope);
    }

    private static CompatibilityFixture fixture(CompatibilityFixtures fixtures, String id) {
        return fixtures.fixtures().stream().filter(f -> f.id().equals(id)).findFirst()
                       .orElseThrow(() -> new AssertionError("no fixture " + id + " in "
                                                             + fixtures.fixtures().stream()
                                                                       .map(CompatibilityFixture::id).toList()));
    }

    private static ManifestEntry preserved(String id, CompatibilityDimension... dimensions) {
        return entry(id, Classification.PRESERVED, Set.of(dimensions), null, null, null, List.of());
    }

    private static ManifestEntry adapted(String id) {
        return entry(id, Classification.ADAPTED, Set.of(), "api:p.Replacement", MigrationKind.RECIPE,
                     "apply recipe", List.of("AdaptedTest"));
    }

    private static ManifestEntry excluded(String id) {
        return entry(id, Classification.EXCLUDED, Set.of(), null, MigrationKind.DIAGNOSTIC, "remove it", List.of());
    }

    private static ManifestEntry withEvidence(ManifestEntry e, String... evidence) {
        return new ManifestEntry(e.id(), e.kind(), e.v2Surface(), e.source(), e.deprecated(), e.classification(),
                                 e.dimensions(), e.rationale(), e.differences(), e.v3Artifact(), e.replacement(),
                                 e.migrationKind(), e.migration(), e.supportEvidence(), new TreeSet<>(Set.of(evidence)));
    }

    private static ManifestEntry entry(String id, Classification classification, Set<CompatibilityDimension> dims,
                                       String replacement, MigrationKind migrationKind, String migration,
                                       List<String> evidence) {
        return new ManifestEntry(id, EntryKind.ofId(id).orElseThrow(), "V2 " + id,
                                 new SourceLocation("software.amazon.awssdk:test", null), false, classification,
                                 new TreeSet<>(dims), "because",
                                 classification == Classification.ADAPTED ? List.of("differs") : List.of(), null,
                                 replacement, migrationKind, migration,
                                 classification == Classification.EXCLUDED ? "unsupported" : null,
                                 new TreeSet<>(evidence));
    }

    private static List<String> compile(List<Path> files, String classPath, Path output) throws IOException {
        Files.createDirectories(output);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = JAVAC.getStandardFileManager(diagnostics, Locale.ROOT,
                                                                            StandardCharsets.UTF_8)) {
            List<String> options = List.of("-proc:none", "--release", "21", "-classpath", classPath, "-d",
                                           output.toString());
            JAVAC.getTask(null, manager, diagnostics, options, null, manager.getJavaFileObjectsFromPaths(files)).call();
        }
        return diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                          .map(d -> d.getMessage(Locale.ROOT)).toList();
    }
}
