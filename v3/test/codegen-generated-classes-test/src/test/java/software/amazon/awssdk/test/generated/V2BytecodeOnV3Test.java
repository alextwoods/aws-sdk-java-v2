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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.test.generated.support.ClassFileRefs;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures.Fixture;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures.Target;
import software.amazon.awssdk.test.generated.support.FakeHttp;
import software.amazon.awssdk.test.generated.support.Javac;

/**
 * Executes V2 caller bytecode against V3 (Requirements 17.1, 17.2, 19.12). Every caller is compiled against the
 * released V2 API jars ({@code target/v2-api}, never on the test class path) and then loaded in a class loader whose
 * parent holds the generated V3 classes, so every V2 reference links against V3.
 *
 * <ul>
 *   <li>The generated linkage fixtures ({@code compatibility-fixtures.json}): the V2 caller of a binary-preserved entry
 *       references the entry's exact owner, name and JVM descriptor and links; the V2 caller of an {@code ADAPTED} or
 *       {@code EXCLUDED} entry without a binary claim fails with a {@link LinkageError}.</li>
 *   <li>A hand-written V2 {@code getItem} caller that also runs: the evidence behind the manifest's binary and
 *       behavior claims for the DynamoDB {@code getItem} surface.</li>
 * </ul>
 */
class V2BytecodeOnV3Test {

    private static final String V2_CALLER = """
        import java.util.Map;
        import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
        import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
        import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
        import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

        public final class V2Caller {
            public static String call(DynamoDbClient client) {
                Map<String, AttributeValue> key = Map.of("id", AttributeValue.builder().s("1").build());
                GetItemResponse typed = client.getItem(GetItemRequest.builder().tableName("t").key(key).build());
                GetItemResponse consumer = client.getItem(b -> b.tableName("t").key(key).consistentRead(true));
                return typed.item().get("id").s() + "," + consumer.item().get("n").n() + ","
                       + typed.equals(consumer);
            }
        }
        """;

    /** Platform types javac inlines as compile-time constants when a static final field holds one. */
    private static final Set<String> CONSTANT_DESCRIPTORS = Set.of("B", "C", "D", "F", "I", "J", "S", "Z",
                                                                   "Ljava/lang/String;");

    static Stream<Fixture> linkageFixtures() {
        return CompatibilityFixtures.ofKinds("LINK", "LINK_REJECTED");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("linkageFixtures")
    void eachV2CallerLinksOrIsRejectedAsItsManifestEntryDeclares(Fixture fixture, @TempDir Path temp)
        throws Exception {
        Path classes = temp.resolve("classes");
        assertEquals(List.of(), Javac.compile(CompatibilityFixtures.CALLER_CLASS, fixture.source(), v2ApiClassPath(),
                                              classes),
                     fixture.entryId() + ": the V2 caller must compile against the V2 API jars");
        boolean inlined = !referencesTarget(fixture.target(), ClassFileRefs.read(
            classes.resolve(CompatibilityFixtures.CALLER_CLASS + ".class")), fixture.entryId());
        boolean rejected = fixture.kind().equals("LINK_REJECTED");
        if (inlined) {
            // javac copied the constant into the caller, so no linkage happens; compare the V3 field instead.
            Field field = v3Field(fixture.target());
            if (rejected) {
                assertTrue(field == null, fixture.diagnostic() + ": V3 keeps the inlined constant field");
            } else {
                assertTrue(field != null && Modifier.isStatic(field.getModifiers()),
                           fixture.entryId() + ": V3 lacks the static constant field");
            }
            return;
        }

        Throwable failure = run(classes);
        boolean linked = failure == null || failure instanceof NullPointerException
                                            && fixture.target().kind().equals("FIELD") && !fixture.target().isStatic();
        if (rejected) {
            if (!(failure instanceof LinkageError)) {
                fail(fixture.diagnostic() + ": compiled V2 bytecode " + (linked ? "links" : "fails without a linkage "
                                                                                     + "error")
                     + " against V3; claim the binary dimension or remove the surface", failure);
            }
        } else if (!linked) {
            throw new AssertionError(fixture.entryId() + " is binary-preserved, but its V2 bytecode fails against V3",
                                     failure);
        }
    }

    @Test
    void theV2ApiJarsAreTheFixtureBaseline() throws IOException {
        String version = CompatibilityFixtures.v2Version();
        for (String jar : jars("v3.fixture.v2Api").split(File.pathSeparator)) {
            assertTrue(jar.endsWith("-" + version + ".jar"), jar + " is not from the fixture baseline V2 " + version);
        }
    }

    @Test
    void v2CompiledGetItemCallerRunsAgainstTheGeneratedV3Client(@TempDir Path temp) throws Exception {
        Path classes = temp.resolve("classes");
        List<String> errors = Javac.compile("V2Caller", V2_CALLER, v2ApiClassPath(), classes);
        assertEquals(List.of(), errors, "the caller must compile against the V2 API jars");

        FakeHttp http = FakeHttp.sync(r -> FakeHttp.ok("{\"Item\":{\"id\":{\"S\":\"1\"},\"n\":{\"N\":\"7\"}}}"));
        try (DynamoDbClient client = DynamoDbClient.builder().region(Region.of("us-east-1"))
                                                   .credentialsProvider(FakeHttp.CREDENTIALS)
                                                   .endpointOverride(URI.create("https://dynamodb.test"))
                                                   .httpClient(http).build();
             URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()},
                                                        V2BytecodeOnV3Test.class.getClassLoader())) {
            Object result;
            try {
                result = loader.loadClass("V2Caller").getMethod("call", DynamoDbClient.class).invoke(null, client);
            } catch (InvocationTargetException e) {
                throw new AssertionError("V2 bytecode failed to link or run against V3", e.getCause());
            }
            assertEquals("1,7,true", result);
            assertEquals(2, http.requests().size());
            assertTrue(http.requests().get(1).body().contains("\"ConsistentRead\":true"));
        }
    }

    /**
     * Asserts the compiled caller references the target exactly (methods and constructors through a method handle);
     * returns false only for a field javac inlined as a compile-time constant.
     */
    private static boolean referencesTarget(Target target, ClassFileRefs refs, String entryId) {
        ClassFileRefs.Ref ref = new ClassFileRefs.Ref(target.owner(), target.name(), target.descriptor());
        switch (target.kind()) {
            case "TYPE" -> assertTrue(refs.classes().contains(target.owner()), entryId + ": no class reference");
            case "METHOD", "CONSTRUCTOR" -> assertTrue(refs.handleTargets().contains(ref),
                                                       entryId + ": the caller does not reference " + ref
                                                       + " through a method handle; it references "
                                                       + refs.handleTargets());
            case "FIELD" -> {
                if (!refs.fields().contains(ref)) {
                    assertTrue(target.isStatic() && CONSTANT_DESCRIPTORS.contains(target.descriptor()),
                               entryId + ": the caller does not reference " + ref + "; it references "
                               + refs.fields());
                    return false;
                }
            }
            default -> fail("Unknown target kind " + target.kind());
        }
        return true;
    }

    /** Loads the caller against the V3 class path and runs its {@code link()}; returns what it threw, if anything. */
    private static Throwable run(Path classes) throws IOException {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()},
                                                        V2BytecodeOnV3Test.class.getClassLoader())) {
            Class.forName(CompatibilityFixtures.CALLER_CLASS, true, loader).getMethod("link").invoke(null);
            return null;
        } catch (InvocationTargetException e) {
            return e.getCause();
        } catch (LinkageError | ReflectiveOperationException e) {
            return e;
        }
    }

    private static Field v3Field(Target target) {
        try {
            return Class.forName(target.owner(), false, V2BytecodeOnV3Test.class.getClassLoader())
                        .getField(target.name());
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            return null;
        }
    }

    /** The V2 API jars and the third-party jars their types extend, which V2 callers are compiled against. */
    private static String v2ApiClassPath() throws IOException {
        String path = jars("v3.fixture.v2Api");
        assertTrue(path.contains("dynamodb-"), "no V2 DynamoDB jar in " + System.getProperty("v3.fixture.v2Api"));
        return path + File.pathSeparator + jars("v3.fixture.v2ApiDependencies");
    }

    private static String jars(String directoryProperty) throws IOException {
        try (Stream<Path> jars = Files.list(Path.of(System.getProperty(directoryProperty)))) {
            return jars.filter(p -> p.toString().endsWith(".jar")).sorted().map(Path::toString)
                       .collect(Collectors.joining(File.pathSeparator));
        }
    }
}
