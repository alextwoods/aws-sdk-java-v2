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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyReader;
import software.amazon.awssdk.policybuilder.iam.IamPolicyWriter;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures;
import software.amazon.awssdk.test.generated.support.Javac;

/**
 * Launch-parity evidence for the V3 IAM policy builder (task 14.3; Requirements 16.8, 17.1, 17.2, 19.12): the
 * released V2 library ({@code target/v2-iam-runtime}: the V2 policy builder of the fixture baseline and its V2 runtime,
 * in an isolated class loader that never sees V3) and the V3 {@code iam-policy-builder} artifact (on the test class
 * path) side by side.
 *
 * <ul>
 *   <li>Source and binary: every public V2 type and member has the identical declaration in V3 (modifiers, generic
 *       signature, supertypes, default methods), V3 adds no public type, and one V2 caller compiled against the V2
 *       API jars compiles against V3 too and runs on both with identical results.</li>
 *   <li>Behavior: deterministic generated policy documents (valid, invalid, malformed, re-encoded) are read, rendered,
 *       hashed, written compact and pretty, and re-read by both; every outcome, including the exception type and
 *       message of every rejection, must be identical.</li>
 * </ul>
 *
 * <p>The Parity Manifest's {@code api:software.amazon.awssdk.policybuilder.iam.*} entries (classification rule
 * {@code iam-policy-builder-api}) name this suite as their source, binary and behavior evidence.
 */
class IamPolicyBuilderCompatibilityTest {
    private static final String PACKAGE = "software.amazon.awssdk.policybuilder.iam";
    private static final String PACKAGE_PATH = PACKAGE.replace('.', '/') + "/";
    private static final long SEED = 0x1A3_2014_03L;
    private static final int DOCUMENTS = 3000;

    private static URLClassLoader v2;
    private static Path v2Jar;

    @BeforeAll
    static void loadV2() throws IOException {
        Path directory = Path.of(System.getProperty("v3.fixture.v2IamRuntime"));
        List<Path> jars;
        try (Stream<Path> files = Files.list(directory)) {
            jars = files.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
        }
        String version = CompatibilityFixtures.v2Version();
        v2Jar = directory.resolve("iam-policy-builder-" + version + ".jar");
        assertTrue(jars.contains(v2Jar), "no V2 " + version + " policy builder in " + directory);
        URL[] urls = new URL[jars.size()];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = jars.get(i).toUri().toURL();
        }
        v2 = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
    }

    @AfterAll
    static void closeV2() throws IOException {
        v2.close();
    }

    @Test
    void v2Library_isIsolatedFromV3() throws Exception {
        Class<?> v2Policy = v2.loadClass(PACKAGE + ".IamPolicy");
        assertFalse(v2Policy == IamPolicy.class, "the V2 class loader must not see V3");
        assertTrue(v2Policy.getProtectionDomain().getCodeSource().getLocation().toURI().equals(v2Jar.toUri()));
    }

    @Test
    void everyPublicV2TypeAndMember_hasTheSameDeclarationInV3() throws Exception {
        List<String> v2Types = publicTypes(v2Jar);
        assertTrue(v2Types.size() >= 18, "V2 public types: " + v2Types);
        assertEquals(v2Types, publicTypes(location(IamPolicy.class)), "the public API types of the package");
        List<String> differences = new ArrayList<>();
        for (String type : v2Types) {
            List<String> expected = declaration(v2.loadClass(type));
            List<String> actual = declaration(Class.forName(type, false, getClass().getClassLoader()));
            if (!expected.equals(actual)) {
                List<String> missing = new ArrayList<>(expected);
                missing.removeAll(actual);
                List<String> added = new ArrayList<>(actual);
                added.removeAll(expected);
                differences.add(type + ": V2 only " + missing + "; V3 only " + added);
            }
        }
        assertEquals(List.of(), differences);
    }

    @Test
    void v2CompiledCaller_compilesAgainstV3AndRunsIdenticallyOnV2AndV3(@TempDir Path temp) throws Exception {
        Path classes = temp.resolve("v2-classes");
        assertEquals(List.of(), Javac.compile("IamV2Caller", CALLER, v2ApiClassPath(), classes),
                     "the caller must compile against the V2 API jars");
        assertEquals(List.of(), Javac.compile("IamV2Caller", CALLER, Javac.testClassPath(), temp.resolve("v3-classes")),
                     "V2 source must compile against V3 unchanged");
        String onV2 = runCaller(classes, v2);
        String onV3 = runCaller(classes, getClass().getClassLoader());
        assertTrue(onV2.contains("\"Statement\"") && onV2.lines().count() > 40, onV2);
        assertEquals(onV2, onV3, "V2 caller bytecode must behave identically on V3");
    }

    @Test
    void generatedDocuments_readRenderWriteAndFailIdenticallyOnV2AndV3() throws Exception {
        V2World v2World = new V2World(v2);
        Random random = new Random(SEED);
        List<String> mismatches = new ArrayList<>();
        int read = 0;
        int rejected = 0;
        for (int i = 0; i < DOCUMENTS; i++) {
            String document = Documents.document(random);
            Charset encoding = Documents.encoding(random);
            String expected = outcome(v2World, document, encoding);
            String actual = outcome(V3_WORLD, document, encoding);
            if (expected.startsWith("read ")) {
                read++;
            } else {
                rejected++;
            }
            if (!expected.equals(actual) && mismatches.size() < 10) {
                mismatches.add("document #" + i + " (" + encoding + "): " + escape(document) + "\n  V2: "
                               + expected + "\n  V3: " + actual);
            }
        }
        assertEquals(List.of(), mismatches, "documents V3 handles differently from V2");
        assertTrue(read > DOCUMENTS / 5 && rejected > DOCUMENTS / 5, "read " + read + ", rejected " + rejected
                                                                      + ": the generator must cover both");
    }

    /** The outcome of reading, rendering, hashing, writing and re-reading {@code document} in {@code world}. */
    private static String outcome(World world, String document, Charset encoding) {
        Object policy;
        try {
            policy = encoding == null ? world.read(document) : world.read(document.getBytes(encoding));
        } catch (Throwable t) {
            return "rejected " + failure(t);
        }
        StringBuilder result = new StringBuilder("read ").append(policy).append(" hash=").append(policy.hashCode());
        for (boolean pretty : new boolean[] {false, true}) {
            try {
                String json = world.write(policy, pretty);
                byte[] bytes = world.writeBytes(policy, pretty);
                Object again = world.read(json);
                result.append("\n  ").append(pretty ? "pretty " : "compact ").append(json)
                      .append(" bytes=").append(Arrays.equals(bytes, json.getBytes(StandardCharsets.UTF_8)))
                      .append(" reread=").append(policy.equals(again)).append('/').append(again.hashCode());
            } catch (Throwable t) {
                result.append("\n  write failed ").append(failure(t));
            }
        }
        return result.toString();
    }

    /**
     * A failure as compared between V2 and V3: the exception type and message, except that a malformed document is
     * compared by type only (the JSON parser's location text differs), and an empty document by its type.
     */
    private static String failure(Throwable t) {
        if (t instanceof UncheckedIOException) {
            return "UncheckedIOException(" + (t.getCause() instanceof IOException) + ")";
        }
        if (t instanceof NullPointerException && t.getMessage() != null
            && (t.getMessage().contains("JsonNode") || t.getMessage().contains("contains no JSON value"))) {
            return "NullPointerException(empty document)";
        }
        return t.getClass().getName() + ": " + t.getMessage();
    }

    /** One policy builder implementation: V3 directly or V2 through reflection. */
    private interface World {
        Object read(String document) throws Throwable;

        Object read(byte[] document) throws Throwable;

        String write(Object policy, boolean pretty) throws Throwable;

        byte[] writeBytes(Object policy, boolean pretty) throws Throwable;
    }

    private static final IamPolicyWriter V3_PRETTY = IamPolicyWriter.builder().prettyPrint(true).build();

    private static final World V3_WORLD = new World() {
        @Override
        public Object read(String document) {
            return IamPolicyReader.create().read(document);
        }

        @Override
        public Object read(byte[] document) {
            return IamPolicyReader.create().read(document);
        }

        @Override
        public String write(Object policy, boolean pretty) {
            return (pretty ? V3_PRETTY : IamPolicyWriter.create()).writeToString((IamPolicy) policy);
        }

        @Override
        public byte[] writeBytes(Object policy, boolean pretty) {
            return (pretty ? V3_PRETTY : IamPolicyWriter.create()).writeToBytes((IamPolicy) policy);
        }
    };

    /** The released V2 library, called through its public API reflectively. */
    private static final class V2World implements World {
        private final Object reader;
        private final Method readString;
        private final Method readBytes;
        private final Object compact;
        private final Object pretty;
        private final Method writeToString;
        private final Method writeToBytes;

        V2World(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> readerType = loader.loadClass(PACKAGE + ".IamPolicyReader");
            Class<?> writerType = loader.loadClass(PACKAGE + ".IamPolicyWriter");
            Class<?> writerBuilderType = loader.loadClass(PACKAGE + ".IamPolicyWriter$Builder");
            Class<?> policyType = loader.loadClass(PACKAGE + ".IamPolicy");
            reader = readerType.getMethod("create").invoke(null);
            readString = readerType.getMethod("read", String.class);
            readBytes = readerType.getMethod("read", byte[].class);
            compact = writerType.getMethod("create").invoke(null);
            Object builder = writerType.getMethod("builder").invoke(null);
            writerBuilderType.getMethod("prettyPrint", Boolean.class).invoke(builder, Boolean.TRUE);
            pretty = writerBuilderType.getMethod("build").invoke(builder);
            writeToString = writerType.getMethod("writeToString", policyType);
            writeToBytes = writerType.getMethod("writeToBytes", policyType);
        }

        @Override
        public Object read(String document) throws Throwable {
            return invoke(readString, reader, document);
        }

        @Override
        public Object read(byte[] document) throws Throwable {
            return invoke(readBytes, reader, (Object) document);
        }

        @Override
        public String write(Object policy, boolean prettyPrint) throws Throwable {
            return (String) invoke(writeToString, prettyPrint ? pretty : compact, policy);
        }

        @Override
        public byte[] writeBytes(Object policy, boolean prettyPrint) throws Throwable {
            return (byte[]) invoke(writeToBytes, prettyPrint ? pretty : compact, policy);
        }

        private static Object invoke(Method method, Object target, Object... arguments) throws Throwable {
            try {
                return method.invoke(target, arguments);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    /** The public API types of the policy builder package in a jar or class directory, sorted. */
    private static List<String> publicTypes(Path location) throws IOException, ClassNotFoundException {
        List<String> entries;
        if (Files.isDirectory(location)) {
            try (Stream<Path> files = Files.walk(location)) {
                entries = files.map(p -> location.relativize(p).toString().replace(File.separatorChar, '/')).toList();
            }
        } else {
            try (ZipFile zip = new ZipFile(location.toFile())) {
                entries = zip.stream().map(e -> e.getName()).toList();
            }
        }
        List<String> types = new ArrayList<>();
        for (String entry : entries) {
            if (entry.startsWith(PACKAGE_PATH) && entry.endsWith(".class") && !entry.contains("/internal/")
                && !entry.endsWith("package-info.class")) {
                String name = entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
                Class<?> type = Class.forName(name, false, location.equals(v2Jar)
                                                             ? v2 : IamPolicyBuilderCompatibilityTest.class
                                                                        .getClassLoader());
                if (isApi(type)) {
                    types.add(name);
                }
            }
        }
        return types.stream().sorted().toList();
    }

    private static boolean isApi(Class<?> type) {
        for (Class<?> t = type; t != null; t = t.getEnclosingClass()) {
            if (!Modifier.isPublic(t.getModifiers()) && !Modifier.isProtected(t.getModifiers())
                || t.isAnonymousClass() || t.isLocalClass() || t.isSynthetic()) {
                return false;
            }
        }
        return true;
    }

    /** Everything about {@code type} a V2 caller's source or bytecode depends on, one line per declaration. */
    private static List<String> declaration(Class<?> type) {
        TreeSet<String> lines = new TreeSet<>();
        lines.add("type " + Modifier.toString(type.getModifiers()) + " " + type.toGenericString());
        lines.add("superclass " + type.getGenericSuperclass());
        lines.add("interfaces " + Arrays.toString(type.getGenericInterfaces()));
        for (Method m : type.getDeclaredMethods()) {
            if (visible(m) && !m.isSynthetic() && !m.isBridge()) {
                lines.add("method " + m.toGenericString() + (m.isDefault() ? " [default]" : ""));
            }
        }
        for (Field f : type.getDeclaredFields()) {
            if (visible(f) && !f.isSynthetic()) {
                lines.add("field " + f.toGenericString());
            }
        }
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            if (visible(c) && !c.isSynthetic()) {
                lines.add("constructor " + c.toGenericString());
            }
        }
        return List.copyOf(lines);
    }

    private static boolean visible(Member member) {
        return Modifier.isPublic(member.getModifiers()) || Modifier.isProtected(member.getModifiers());
    }

    private static Path location(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static String v2ApiClassPath() throws IOException {
        try (Stream<Path> jars = Files.list(Path.of(System.getProperty("v3.fixture.v2Api")))) {
            String path = jars.filter(p -> p.toString().endsWith(".jar")).sorted().map(Path::toString)
                              .collect(Collectors.joining(File.pathSeparator));
            assertTrue(path.contains("iam-policy-builder-"), "no V2 policy builder API jar in " + path);
            return path;
        }
    }

    private static String runCaller(Path classes, ClassLoader parent) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, parent)) {
            return (String) loader.loadClass("IamV2Caller").getMethod("call").invoke(null);
        } catch (InvocationTargetException e) {
            throw new AssertionError("the V2 caller failed", e.getCause());
        }
    }

    private static String escape(String document) {
        StringBuilder result = new StringBuilder();
        document.chars().forEach(c -> result.append(c < 0x20 || c > 0x7E ? String.format("\\u%04x", c)
                                                                          : String.valueOf((char) c)));
        return result.toString();
    }

    /**
     * A V2 caller of the policy builder API: every factory, constant, builder overload, accessor, reader and writer
     * method family, plus equality, hashing, string rendering and the documented validation failures. It reports
     * everything it observes as text.
     */
    private static final String CALLER = """
        import java.io.ByteArrayInputStream;
        import java.nio.charset.StandardCharsets;
        import java.util.Arrays;
        import java.util.List;
        import java.util.function.Supplier;
        import software.amazon.awssdk.policybuilder.iam.*;

        public final class IamV2Caller {
            private static final StringBuilder OUT = new StringBuilder();

            public static String call() {
                OUT.setLength(0);
                IamConditionOperator[] operators = {
                    IamConditionOperator.STRING_EQUALS, IamConditionOperator.STRING_NOT_EQUALS,
                    IamConditionOperator.STRING_EQUALS_IGNORE_CASE, IamConditionOperator.STRING_NOT_EQUALS_IGNORE_CASE,
                    IamConditionOperator.STRING_LIKE, IamConditionOperator.STRING_NOT_LIKE,
                    IamConditionOperator.NUMERIC_EQUALS, IamConditionOperator.NUMERIC_NOT_EQUALS,
                    IamConditionOperator.NUMERIC_LESS_THAN, IamConditionOperator.NUMERIC_LESS_THAN_EQUALS,
                    IamConditionOperator.NUMERIC_GREATER_THAN, IamConditionOperator.NUMERIC_GREATER_THAN_EQUALS,
                    IamConditionOperator.DATE_EQUALS, IamConditionOperator.DATE_NOT_EQUALS,
                    IamConditionOperator.DATE_LESS_THAN, IamConditionOperator.DATE_LESS_THAN_EQUALS,
                    IamConditionOperator.DATE_GREATER_THAN, IamConditionOperator.DATE_GREATER_THAN_EQUALS,
                    IamConditionOperator.BOOL, IamConditionOperator.BINARY_EQUALS, IamConditionOperator.IP_ADDRESS,
                    IamConditionOperator.NOT_IP_ADDRESS, IamConditionOperator.ARN_EQUALS,
                    IamConditionOperator.ARN_NOT_EQUALS, IamConditionOperator.ARN_LIKE,
                    IamConditionOperator.ARN_NOT_LIKE, IamConditionOperator.NULL};
                for (IamConditionOperator operator : operators) {
                    IamValue value = operator;
                    out("operator " + value.value() + " " + operator + " " + operator.hashCode());
                }
                out("operator " + IamConditionOperator.STRING_LIKE.addPrefix("ForAnyValue:").addSuffix("IfExists")
                    + " " + IamConditionOperator.create("Custom").equals(IamConditionOperator.create("Custom")));
                out("principal types " + IamPrincipalType.AWS + IamPrincipalType.FEDERATED + IamPrincipalType.SERVICE
                    + IamPrincipalType.CANONICAL_USER + IamPrincipalType.create("Custom").value());
                out("constants " + IamPrincipal.ALL + IamAction.ALL + IamResource.ALL + IamEffect.ALLOW
                    + IamEffect.DENY + IamEffect.create("Audit") + IamAction.create("s3:GetObject")
                    + IamResource.create("arn:aws:s3:::b/*") + IamConditionKey.create("aws:SourceIp"));

                IamPrincipal p1 = IamPrincipal.create(IamPrincipalType.AWS, "123456789012");
                IamPrincipal p2 = IamPrincipal.create("Service", "ec2.amazonaws.com");
                IamPrincipal p3 = IamPrincipal.builder().type(IamPrincipalType.FEDERATED).id("cognito").build();
                IamPrincipal p4 = IamPrincipal.builder().type("CanonicalUser").id("abc").build();
                List<IamPrincipal> all1 = IamPrincipal.createAll(IamPrincipalType.AWS, Arrays.asList("1", "2"));
                List<IamPrincipal> all2 = IamPrincipal.createAll("Service", Arrays.asList("s1", "s2"));
                out("principals " + p1.type() + p1.id() + p2 + p3 + p4 + all1 + all2
                    + p1.toBuilder().id("x").build() + p1.copy(b -> b.id("y")) + p1.equals(p1.toBuilder().build()));

                IamConditionKey key = IamConditionKey.create("aws:PrincipalOrgID");
                IamCondition c1 = IamCondition.create(IamConditionOperator.STRING_EQUALS, key, "o-1");
                IamCondition c2 = IamCondition.create(IamConditionOperator.BOOL, "aws:SecureTransport", "true");
                IamCondition c3 = IamCondition.create("NumericLessThan", "s3:max-keys", "10");
                IamCondition c4 = IamCondition.builder().operator(IamConditionOperator.IP_ADDRESS)
                                              .key(IamConditionKey.create("aws:SourceIp")).value("10.0.0.0/8").build();
                IamCondition c5 = IamCondition.builder().operator("DateLessThan").key("aws:CurrentTime")
                                              .value("2030-01-01T00:00:00Z").build();
                List<IamCondition> cs1 = IamCondition.createAll(IamConditionOperator.STRING_LIKE, key,
                                                                Arrays.asList("a*", "b*"));
                List<IamCondition> cs2 = IamCondition.createAll(IamConditionOperator.ARN_LIKE, "aws:SourceArn",
                                                                Arrays.asList("arn:1", "arn:2"));
                List<IamCondition> cs3 = IamCondition.createAll("StringNotEquals", "aws:username",
                                                                Arrays.asList("u1"));
                out("conditions " + c1.operator() + c1.key() + c1.value() + c2 + c3 + c4 + c5 + cs1 + cs2 + cs3
                    + c1.copy(b -> b.value("o-2")) + c1.hashCode());

                IamStatement full = IamStatement.builder()
                    .sid("Full").effect(IamEffect.ALLOW).effect("Allow")
                    .principals(Arrays.asList(p1)).addPrincipal(p2).addPrincipal(b -> b.type("AWS").id("3"))
                    .addPrincipal(IamPrincipalType.SERVICE, "lambda.amazonaws.com").addPrincipal("Federated", "f")
                    .addPrincipals(IamPrincipalType.AWS, Arrays.asList("4", "5"))
                    .addPrincipals("CanonicalUser", Arrays.asList("cu"))
                    .notPrincipals(Arrays.asList(p3)).addNotPrincipal(p4).addNotPrincipal(b -> b.type("AWS").id("6"))
                    .addNotPrincipal(IamPrincipalType.AWS, "7").addNotPrincipal("Service", "s3.amazonaws.com")
                    .addNotPrincipals(IamPrincipalType.FEDERATED, Arrays.asList("n1"))
                    .addNotPrincipals("AWS", Arrays.asList("8", "9"))
                    .actions(Arrays.asList(IamAction.create("s3:GetObject"))).actionIds(Arrays.asList("s3:Put*"))
                    .addAction(IamAction.ALL).addAction("iam:PassRole")
                    .notActions(Arrays.asList(IamAction.create("s3:Delete*"))).notActionIds(Arrays.asList("x:Y"))
                    .addNotAction(IamAction.create("a:b")).addNotAction("c:d")
                    .resources(Arrays.asList(IamResource.ALL)).resourceIds(Arrays.asList("arn:aws:s3:::b"))
                    .addResource(IamResource.create("arn:aws:s3:::b/*")).addResource("arn:aws:sqs:*:*:q")
                    .notResources(Arrays.asList(IamResource.create("arn:nr"))).notResourceIds(Arrays.asList("arn:x"))
                    .addNotResource(IamResource.create("arn:y")).addNotResource("arn:z")
                    .conditions(Arrays.asList(c1)).addCondition(c2).addCondition(b -> b.operator("Bool").key("k")
                                                                                        .value("false"))
                    .addCondition(IamConditionOperator.STRING_EQUALS, key, "o-3")
                    .addCondition(IamConditionOperator.STRING_EQUALS, "aws:PrincipalTag/team", "t")
                    .addCondition("StringLike", "s3:prefix", "home/*")
                    .addConditions(IamConditionOperator.NUMERIC_EQUALS, IamConditionKey.create("n"),
                                   Arrays.asList("1", "2"))
                    .addConditions(IamConditionOperator.IP_ADDRESS, "aws:SourceIp", Arrays.asList("1.1.1.1/32"))
                    .addConditions("StringEquals", "aws:PrincipalOrgID", Arrays.asList("o-4", "o-5"))
                    .build();
                out("statement " + full.sid() + full.effect() + full.principals() + full.notPrincipals()
                    + full.actions() + full.notActions() + full.resources() + full.notResources() + full.conditions());
                IamStatement deny = IamStatement.builder().effect(IamEffect.DENY).addPrincipal(IamPrincipal.ALL)
                                                .addAction("*").addResource("*").build();
                IamStatement.Builder copied = full.toBuilder();
                out("statement copies " + copied.copy().build().equals(full) + full.copy(b -> b.sid("C")).sid()
                    + copied.applyMutation(b -> b.sid("M")).build().sid() + full.hashCode() + deny);

                IamPolicy policy = IamPolicy.builder().id("Id-1").version("2012-10-17")
                                            .statements(Arrays.asList(full)).addStatement(deny)
                                            .addStatement(b -> b.effect("Allow").addAction("sts:AssumeRole"))
                                            .build();
                IamPolicy created = IamPolicy.create(Arrays.asList(deny));
                out("policy " + policy.id() + policy.version() + policy.statements().size() + created
                    + created.version() + policy.hashCode() + policy.equals(policy.toBuilder().build()));

                IamPolicyWriter compact = IamPolicyWriter.create();
                IamPolicyWriter pretty = IamPolicyWriter.builder().prettyPrint(true).build();
                out("writers " + compact.equals(IamPolicyWriter.builder().build()) + pretty.hashCode()
                    + pretty.toBuilder().prettyPrint(false).build().equals(compact)
                    + pretty.equals(IamPolicyWriter.builder().prettyPrint(true).build()));
                out(policy.toJson());
                out(policy.toJson(pretty));
                out(compact.writeToString(created) + " " + new String(pretty.writeToBytes(created),
                                                                       StandardCharsets.UTF_8));

                IamPolicyReader reader = IamPolicyReader.create();
                String json = policy.toJson(pretty);
                IamPolicy fromString = reader.read(json);
                IamPolicy fromBytes = reader.read(json.getBytes(StandardCharsets.UTF_8));
                IamPolicy fromStream = reader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
                out("read " + fromString.equals(fromBytes) + fromBytes.equals(fromStream) + fromString
                    + IamPolicy.fromJson(policy.toJson()).toJson().equals(policy.toJson())
                    + reader.equals(IamPolicyReader.create()) + reader.hashCode());

                fail("statement without effect", () -> IamStatement.builder().build());
                fail("policy without statements", () -> IamPolicy.builder().build());
                fail("null action", () -> IamAction.create(null));
                fail("null principal type", () -> IamPrincipal.create((IamPrincipalType) null, "x"));
                fail("principal without id", () -> IamPrincipal.builder().type("AWS").build());
                fail("condition without key", () -> IamCondition.builder().operator("Bool").value("true").build());
                fail("null prefix", () -> IamConditionOperator.BOOL.addPrefix(null));
                fail("all and others", () -> IamPolicy.builder().addStatement(b -> b.effect("Allow")
                    .addPrincipal(IamPrincipal.ALL).addPrincipal("AWS", "1")).build().toJson());
                fail("not a policy", () -> reader.read("[]"));
                fail("bad statement", () -> reader.read("{\\"Statement\\":\\"x\\"}"));
                fail("bad principal", () -> reader.read("{\\"Statement\\":{\\"Effect\\":\\"Allow\\",\\"Principal\\":1}}"));
                return OUT.toString();
            }

            private static void out(Object line) {
                OUT.append(line).append('\\n');
            }

            private static void fail(String name, Supplier<?> action) {
                try {
                    out(name + " succeeded: " + action.get());
                } catch (RuntimeException e) {
                    out(name + " -> " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
        """;

    /** Deterministic policy documents: valid, structurally invalid, malformed and re-encoded. */
    private static final class Documents {
        private static final String[] OPERATORS = {"StringEquals", "NumericLessThan", "Bool", "ForAnyValue:StringLike",
                                                   "DateGreaterThan"};
        private static final String[] KEYS = {"aws:SourceIp", "aws:PrincipalAccount", "s3:prefix", "aws:MultiFactor"};
        private static final String[] PRINCIPAL_TYPES = {"AWS", "Service", "Federated", "CanonicalUser", "*", "X"};
        private static final String[] NUMBERS = {"0", "-0", "7", "123456789012", "1.5", "-2.5e10", "1E+2", "0.000",
                                                 "12345678901234567890123", "007", "1.", ".5", "-", "+1", "1e"};
        private static final String CHARACTERS = "abcXYZ019 *:/-_\"\\\n\t\u0001\u007f\u00e9\u4e2d\uD83D\uDE00";
        private static final String NOISE = "{}[],:\"\\/*x0-\n \u0000t";

        private Documents() {
        }

        /** The byte encoding a document is read in, or {@code null} to read it as a {@code String}. */
        static Charset encoding(Random r) {
            return switch (r.nextInt(20)) {
                case 0 -> StandardCharsets.UTF_8;
                case 1 -> StandardCharsets.UTF_16BE;
                case 2 -> StandardCharsets.UTF_16LE;
                case 3 -> Charset.forName("UTF-32BE");
                default -> null;
            };
        }

        static String document(Random r) {
            switch (r.nextInt(60)) {
                case 0:
                    return "";
                case 1:
                    return " \n // nothing\n /* here */ ";
                case 2:
                    return value(r);
                default:
                    break;
            }
            String document = policy(r);
            return r.nextInt(5) == 0 ? mutate(document, r) : document;
        }

        private static String policy(Random r) {
            List<String> members = new ArrayList<>();
            maybe(r, 0.75, members, () -> member(r, "Version", string(r)));
            maybe(r, 0.4, members, () -> member(r, "Id", r.nextInt(15) == 0 ? value(r) : string(r)));
            maybe(r, 0.92, members, () -> member(r, "Statement", statements(r)));
            maybe(r, 0.1, members, () -> member(r, "Extra", value(r)));
            return object(r, members);
        }

        private static String statements(Random r) {
            int kind = r.nextInt(20);
            if (kind < 8) {
                return statement(r);
            }
            if (kind < 19) {
                List<String> statements = new ArrayList<>();
                for (int i = r.nextInt(4); i > 0; i--) {
                    statements.add(r.nextInt(25) == 0 ? value(r) : statement(r));
                }
                return array(r, statements);
            }
            return value(r);
        }

        private static String statement(Random r) {
            List<String> members = new ArrayList<>();
            maybe(r, 0.4, members, () -> member(r, "Sid", string(r)));
            maybe(r, 0.93, members, () -> member(r, "Effect", switch (r.nextInt(12)) {
                case 0 -> string(r);
                case 1 -> value(r);
                case 2, 3, 4, 5, 6 -> "\"Allow\"";
                default -> "\"Deny\"";
            }));
            maybe(r, 0.45, members, () -> member(r, "Principal", principals(r)));
            maybe(r, 0.15, members, () -> member(r, "NotPrincipal", principals(r)));
            maybe(r, 0.75, members, () -> member(r, "Action", ids(r)));
            maybe(r, 0.15, members, () -> member(r, "NotAction", ids(r)));
            maybe(r, 0.65, members, () -> member(r, "Resource", ids(r)));
            maybe(r, 0.1, members, () -> member(r, "NotResource", ids(r)));
            maybe(r, 0.45, members, () -> member(r, "Condition", conditions(r)));
            maybe(r, 0.05, members, () -> member(r, "Unknown", value(r)));
            maybe(r, 0.05, members, () -> member(r, "Sid", string(r)));
            java.util.Collections.shuffle(members, r);
            return object(r, members);
        }

        private static String principals(Random r) {
            int kind = r.nextInt(30);
            if (kind < 8) {
                return "\"*\"";
            }
            if (kind < 28) {
                List<String> members = new ArrayList<>();
                for (int i = 1 + r.nextInt(3); i > 0; i--) {
                    String type = PRINCIPAL_TYPES[r.nextInt(PRINCIPAL_TYPES.length)];
                    members.add(member(r, type, principalIds(r)));
                }
                return object(r, members);
            }
            return kind == 28 ? string(r) : value(r);
        }

        private static String principalIds(Random r) {
            int kind = r.nextInt(20);
            if (kind < 7) {
                return r.nextBoolean() ? "\"*\"" : string(r);
            }
            if (kind < 19) {
                List<String> ids = new ArrayList<>();
                for (int i = r.nextInt(4); i > 0; i--) {
                    ids.add(switch (r.nextInt(8)) {
                        case 0 -> number(r);
                        case 1 -> "\"*\"";
                        case 2 -> r.nextInt(5) == 0 ? value(r) : string(r);
                        default -> string(r);
                    });
                }
                return array(r, ids);
            }
            return value(r);
        }

        private static String ids(Random r) {
            int kind = r.nextInt(20);
            if (kind < 10) {
                return string(r);
            }
            if (kind < 19) {
                List<String> ids = new ArrayList<>();
                for (int i = r.nextInt(4); i > 0; i--) {
                    ids.add(r.nextInt(20) == 0 ? value(r) : string(r));
                }
                return array(r, ids);
            }
            return value(r);
        }

        private static String conditions(Random r) {
            if (r.nextInt(30) == 0) {
                return value(r);
            }
            List<String> operators = new ArrayList<>();
            for (int i = 1 + r.nextInt(3); i > 0; i--) {
                List<String> keys = new ArrayList<>();
                for (int j = 1 + r.nextInt(2); j > 0; j--) {
                    keys.add(member(r, KEYS[r.nextInt(KEYS.length)], conditionValues(r)));
                }
                operators.add(member(r, OPERATORS[r.nextInt(OPERATORS.length)],
                                     r.nextInt(30) == 0 ? value(r) : object(r, keys)));
            }
            return object(r, operators);
        }

        private static String conditionValues(Random r) {
            if (r.nextInt(3) == 0) {
                List<String> values = new ArrayList<>();
                for (int i = r.nextInt(4); i > 0; i--) {
                    values.add(conditionValue(r));
                }
                return array(r, values);
            }
            return conditionValue(r);
        }

        private static String conditionValue(Random r) {
            return switch (r.nextInt(12)) {
                case 0, 1 -> number(r);
                case 2 -> r.nextBoolean() ? "true" : "false";
                case 3 -> r.nextInt(4) == 0 ? value(r) : string(r);
                default -> string(r);
            };
        }

        /** Any JSON value, mostly scalars. */
        private static String value(Random r) {
            return switch (r.nextInt(8)) {
                case 0 -> number(r);
                case 1 -> "true";
                case 2 -> "false";
                case 3 -> "null";
                case 4 -> "{" + space(r) + "}";
                case 5 -> "[" + space(r) + "]";
                case 6 -> "[" + string(r) + "," + number(r) + "]";
                default -> string(r);
            };
        }

        private static String number(Random r) {
            return r.nextInt(6) == 0 ? NUMBERS[r.nextInt(NUMBERS.length)] : NUMBERS[r.nextInt(9)];
        }

        /** A JSON string of random characters, each written raw or escaped (short or upper/lower-case escapes). */
        private static String string(Random r) {
            StringBuilder s = new StringBuilder("\"");
            for (int i = r.nextInt(7); i > 0; i--) {
                char c = r.nextInt(30) == 0 ? (char) (0xD800 + r.nextInt(0x800)) : CHARACTERS.charAt(
                    r.nextInt(CHARACTERS.length()));
                boolean mustEscape = c == '"' || c == '\\' || c < 0x20;
                if (mustEscape || r.nextInt(6) == 0) {
                    String hex = String.format("%04x", (int) c);
                    switch (c) {
                        case '"' -> s.append("\\\"");
                        case '\\' -> s.append("\\\\");
                        case '\n' -> s.append(r.nextBoolean() ? "\\n" : "\\u000A");
                        case '\t' -> s.append("\\t");
                        case '/' -> s.append("\\/");
                        default -> s.append("\\u").append(r.nextBoolean() ? hex : hex.toUpperCase());
                    }
                } else {
                    s.append(c);
                }
            }
            return s.append('"').toString();
        }

        private static String member(Random r, String name, String value) {
            return "\"" + name + "\"" + space(r) + ":" + space(r) + value;
        }

        private static String object(Random r, List<String> members) {
            return "{" + space(r) + String.join(space(r) + "," + space(r), members) + space(r) + "}";
        }

        private static String array(Random r, List<String> elements) {
            return "[" + space(r) + String.join(space(r) + "," + space(r), elements) + space(r) + "]";
        }

        private static String space(Random r) {
            return switch (r.nextInt(16)) {
                case 0 -> " ";
                case 1 -> "\n  ";
                case 2 -> "\t";
                case 3 -> "\r\n";
                case 4 -> r.nextInt(4) == 0 ? "/* c */" : "";
                case 5 -> r.nextInt(4) == 0 ? "// c\n" : "";
                default -> "";
            };
        }

        private static void maybe(Random r, double probability, List<String> members,
                                  java.util.function.Supplier<String> member) {
            if (r.nextDouble() < probability) {
                members.add(member.get());
            }
        }

        private static String mutate(String document, Random r) {
            int at = document.isEmpty() ? 0 : r.nextInt(document.length());
            char noise = NOISE.charAt(r.nextInt(NOISE.length()));
            return switch (r.nextInt(8)) {
                case 0 -> document.substring(0, at);
                case 1 -> document.substring(0, at) + document.substring(Math.min(document.length(), at + 1));
                case 2 -> document.substring(0, at) + noise + document.substring(at);
                case 3 -> document.substring(0, at) + noise + document.substring(Math.min(document.length(), at + 1));
                case 4 -> document + (r.nextBoolean() ? " trailing ] garbage" : "}");
                case 5 -> "\uFEFF" + document;
                case 6 -> "/* lead */ " + document + " // tail";
                default -> document.replace(":", r.nextBoolean() ? " : " : ":\u000c");
            };
        }
    }
}
