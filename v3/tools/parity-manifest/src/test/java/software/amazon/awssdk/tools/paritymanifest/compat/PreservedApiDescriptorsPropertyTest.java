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
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.BINARY;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.SOURCE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.tools.paritymanifest.BaselineIdentity;
import software.amazon.awssdk.tools.paritymanifest.Classification;
import software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension;
import software.amazon.awssdk.tools.paritymanifest.EntryKind;
import software.amazon.awssdk.tools.paritymanifest.ManifestEntry;
import software.amazon.awssdk.tools.paritymanifest.ParityManifest;
import software.amazon.awssdk.tools.paritymanifest.SourceLocation;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.Kind;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.Target;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.TargetKind;

/**
 * Property 58 (Requirements 17.1, 17.2): for any API entry declared source- or binary-preserved, the candidate's
 * owner FQCN, member name, JVM parameter and return descriptors, overload set, builder shape and required (exception)
 * hierarchy equal the recorded baseline values for that compatibility dimension.
 *
 * <p>Each try generates a small V2 API ({@link Api}) in the shape of a generated V2 service: a marker interface, a
 * two-level exception hierarchy rooted at a random JDK exception, a final model with a nested fluent
 * {@code Builder} ({@code builder()}, {@code toBuilder()}, getters, setters returning the builder, {@code build()}) and
 * an optional static field, and a client interface with a static {@code create()} and operations overloaded by arity.
 * Member names, parameter and return types, static-ness, protected access and supertypes are random. A random subset
 * of the types and members is declared {@code PRESERVED} in a Parity Manifest with a random non-empty subset of the
 * {@code source} and {@code binary} dimensions. The candidate is the baseline with zero to three random mutations
 * (rename, remove or add an overload, change a parameter, return or field type, toggle static-ness, protected access
 * or finality, add or remove a supertype, re-parent an exception). Baseline and candidate are compiled with javac.
 *
 * <p>The code under test is {@link CompatibilityFixtureGenerator} reading the compiled baseline through
 * {@link BaselineClasses}. Its fixtures are executed against the compiled candidate: {@code COMPILE} callers are
 * compiled against it, {@code LINK} callers are compiled against the baseline and linked against it by the JVM, and
 * {@code SIGNATURE}/{@code HIERARCHY} fixtures are evaluated by {@link ShapeExecutor}, which applies the contract of
 * {@link Kind#SIGNATURE} and {@link Kind#HIERARCHY} exactly as {@code ApiShapeCompatibilityTest} in
 * {@code test/codegen-generated-classes-test} does (that executor is not reachable from this module).
 *
 * <p>{@link Oracle} works on the generated API descriptions alone (no class files, no reflection, no generator code):
 * it spells descriptors and entry IDs itself, knows the JDK supertypes of the few JDK types used, and decides per
 * claimed dimension whether the candidate keeps the claim. The property checks that:
 * <ul>
 *   <li>the generator emits exactly the expected fixtures per claimed dimension, recording the baseline owner, owner
 *       kind and finality, member name, JVM descriptor, static-ness and access (the recorded baseline values), and
 *       the required supertypes: JDK supertypes, preserved superclasses of exceptions, and (binary) binary-preserved
 *       supertypes;</li>
 *   <li>every claimed dimension passes all its fixtures exactly when the oracle says the candidate keeps it;</li>
 *   <li>javac and the JVM agree with the oracle on their own: a member's {@code COMPILE} caller compiles exactly when
 *       its source call still resolves (javac additionally accepts boxing, primitive widening and a static member
 *       reached through an instance expression), a member's {@code LINK} caller links exactly when its exact
 *       descriptor, static-ness and access are kept, a type's {@code COMPILE} caller compiles exactly when the type
 *       keeps its source-required supertypes, and a type's {@code LINK} caller always links.</li>
 * </ul>
 */
class PreservedApiDescriptorsPropertyTest {
    private static final JavaCompiler JAVAC = ToolProvider.getSystemJavaCompiler();
    private static final BaselineIdentity BASELINE =
        new BaselineIdentity("2.54.0", "abc123", null, null, "parity-manifest/test");
    /** Parent of the per-try work directories, inside the module's build output (surefire runs in the module). */
    private static final Path TRIALS = Path.of("target", "property58-trials").toAbsolutePath();

    static final String MARKER = "p.Marker";
    static final String BASE_ERROR = "p.BaseError";
    static final String SERVICE_ERROR = "p.ServiceError";
    static final String MODEL = "p.Model";
    static final String BUILDER = "p.Model$Builder";
    static final String CLIENT = "p.Client";

    static final String OBJECT = "java.lang.Object";
    static final String STRING = "java.lang.String";
    static final String CHAR_SEQUENCE = "java.lang.CharSequence";
    static final String LIST = "java.util.List";
    static final String COLLECTION = "java.util.Collection";
    static final String CONSUMER = "java.util.function.Consumer";
    static final String THROWABLE = "java.lang.Throwable";
    static final String EXCEPTION = "java.lang.Exception";
    static final String RUNTIME_EXCEPTION = "java.lang.RuntimeException";
    static final String ILLEGAL_STATE = "java.lang.IllegalStateException";
    static final String SERIALIZABLE = "java.io.Serializable";
    static final String AUTO_CLOSEABLE = "java.lang.AutoCloseable";

    /** Parameter, return and field types: references related by subtyping (in chains) and primitives. */
    static final List<String> VALUE_TYPES =
        List.of(STRING, CHAR_SEQUENCE, OBJECT, LIST, COLLECTION, CONSUMER, MODEL, BUILDER, "int", "long", "boolean");
    static final List<String> RETURN_TYPES = Stream.concat(VALUE_TYPES.stream(), Stream.of("void")).toList();
    static final List<String> EXCEPTION_ROOTS = List.of(RUNTIME_EXCEPTION, ILLEGAL_STATE, EXCEPTION);
    /** Proper reference supertypes of the reference types members use (Object has none). */
    static final Map<String, Set<String>> REFERENCE_SUPERTYPES = Map.of(
        STRING, Set.of(CHAR_SEQUENCE, OBJECT), CHAR_SEQUENCE, Set.of(OBJECT), LIST, Set.of(COLLECTION, OBJECT),
        COLLECTION, Set.of(OBJECT), CONSUMER, Set.of(OBJECT), MODEL, Set.of(OBJECT), BUILDER, Set.of(OBJECT),
        CLIENT, Set.of(OBJECT), OBJECT, Set.of());
    /** Direct superclass and superinterfaces of the JDK types the generated APIs extend. */
    static final Map<String, String> JDK_SUPERCLASS =
        Map.of(EXCEPTION, THROWABLE, RUNTIME_EXCEPTION, EXCEPTION, ILLEGAL_STATE, RUNTIME_EXCEPTION);
    static final Map<String, List<String>> JDK_INTERFACES = Map.of(THROWABLE, List.of(SERIALIZABLE));

    // Member name pools are disjoint per type so that no member hides or overrides another; none is a member of
    // Object, Throwable or AutoCloseable.
    static final List<String> BASE_ERROR_METHODS = List.of("detail", "code");
    static final List<String> SERVICE_ERROR_METHODS = List.of("token", "region");
    static final List<String> PROPERTIES = List.of("name", "size", "enabled", "tags");
    static final List<String> OPERATIONS = List.of("get", "put", "list", "describe");
    /** Claim choices per API element: none, source, binary, both (twice as likely). */
    static final int CLAIM_CHOICES = 5;
    static final int MAX_ELEMENTS = 32;

    // Feature: aws-sdk-java-v3, Property 58: Preserved API descriptors match their declared dimension
    // Embedded edge cases are disabled: the combined edge cases of the many generators would use up the tries with
    // near-identical minimal APIs. The statistics coverage check fails the property if the generated mix degrades.
    @Property(tries = 300, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 58: Preserved API descriptors match their declared dimension")
    void preservedApiDescriptorsMatchTheirDeclaredDimension(@ForAll("scenarios") Scenario scenario)
        throws Exception {
        Api v2 = scenario.baseline();
        Api v3 = scenario.candidate();
        SortedMap<String, Claim> claims = scenario.claims();
        Path trial = Files.createTempDirectory(Files.createDirectories(TRIALS), "try-");
        try {
            Path v2Classes = trial.resolve("v2");
            Path v3Classes = trial.resolve("v3");
            assertEquals(List.of(), compileApi(v2, trial.resolve("v2-src"), v2Classes), "the baseline compiles");
            assertEquals(List.of(), compileApi(v3, trial.resolve("v3-src"), v3Classes), "the candidate compiles");

            ParityManifest manifest = new ParityManifest(BASELINE, null, claims.values().stream()
                                                                                .map(c -> preserved(c.id(), c.dims()))
                                                                                .toList());
            CompatibilityFixtures generated =
                new CompatibilityFixtureGenerator(manifest, BaselineClasses.read(List.of(v2Classes)))
                    .generate(List.of("*"));
            List<CompatibilityFixture> fixtures = generated.fixtures();

            // The recorded baseline values: fixture set, targets and required supertypes.
            Map<String, Target> expectedTargets = new TreeMap<>();
            Map<String, List<String>> expectedSupertypes = new TreeMap<>();
            for (Claim claim : claims.values()) {
                for (CompatibilityDimension dim : claim.dims()) {
                    for (Kind kind : Oracle.fixtureKinds(claim, dim)) {
                        String id = CompatibilityFixture.id(claim.id(), kind, dim);
                        expectedTargets.put(id, Oracle.target(claim));
                        if (kind == Kind.HIERARCHY) {
                            expectedSupertypes.put(id, Oracle.requiredSupertypes(v2, claims, claim.owner().name(),
                                                                                 dim));
                        }
                    }
                }
            }
            Map<String, Target> actualTargets = new TreeMap<>();
            Map<String, List<String>> actualSupertypes = new TreeMap<>();
            for (CompatibilityFixture f : fixtures) {
                actualTargets.put(f.id(), f.target());
                if (f.kind() == Kind.HIERARCHY) {
                    actualSupertypes.put(f.id(), f.supertypes());
                }
            }
            assertEquals(expectedTargets, actualTargets, "fixtures and their recorded baseline targets");
            assertEquals(expectedSupertypes, actualSupertypes, "recorded required supertypes");

            // Execute every fixture against the candidate.
            Map<String, Boolean> outcomes = new TreeMap<>();
            outcomes.putAll(Callers.compileOutcomes(of(fixtures, Kind.COMPILE), v3Classes, trial.resolve("compile")));
            outcomes.putAll(Callers.linkOutcomes(of(fixtures, Kind.LINK), v2Classes, v3Classes,
                                                 trial.resolve("link")));
            try (URLClassLoader candidate = loader(v3Classes)) {
                for (CompatibilityFixture f : fixtures) {
                    if (f.kind() == Kind.SIGNATURE || f.kind() == Kind.HIERARCHY) {
                        outcomes.put(f.id(), ShapeExecutor.passes(f, candidate));
                    }
                }
            }

            Map<String, Boolean> expectedVerdicts = new TreeMap<>();
            Map<String, Boolean> actualVerdicts = new TreeMap<>();
            Map<String, Boolean> expectedToolOutcomes = new TreeMap<>();
            Map<String, Boolean> actualToolOutcomes = new TreeMap<>();
            for (Claim claim : claims.values()) {
                for (CompatibilityDimension dim : claim.dims()) {
                    String key = claim.id() + " " + dim.jsonName();
                    expectedVerdicts.put(key, Oracle.kept(claim, dim, v2, v3, claims, false));
                    actualVerdicts.put(key, fixtures.stream()
                                                    .filter(f -> f.entryId().equals(claim.id())
                                                                 && f.dimension() == dim)
                                                    .allMatch(f -> outcomes.get(f.id())));
                }
                String compile = CompatibilityFixture.id(claim.id(), Kind.COMPILE, SOURCE);
                if (outcomes.containsKey(compile)) {
                    expectedToolOutcomes.put(compile, Oracle.kept(claim, SOURCE, v2, v3, claims, true));
                    actualToolOutcomes.put(compile, outcomes.get(compile));
                }
                String link = CompatibilityFixture.id(claim.id(), Kind.LINK, BINARY);
                if (outcomes.containsKey(link)) {
                    expectedToolOutcomes.put(link, claim.member() == null
                                                   || Oracle.kept(claim, BINARY, v2, v3, claims, false));
                    actualToolOutcomes.put(link, outcomes.get(link));
                }
            }
            assertEquals(expectedToolOutcomes, actualToolOutcomes,
                         "javac and JVM outcomes of the generated callers against the candidate");
            assertEquals(expectedVerdicts, actualVerdicts, "claimed dimensions kept by the candidate");

            long broken = expectedVerdicts.values().stream().filter(kept -> !kept).count();
            Statistics.label("candidate").collect(broken == 0 ? "every claim kept" : "some claim broken");
            Statistics.label("candidate").coverage(c -> {
                c.check("every claim kept").percentage(p -> p >= 15);
                c.check("some claim broken").percentage(p -> p >= 15);
            });
        } finally {
            deleteRecursively(trial);
        }
    }

    private static List<CompatibilityFixture> of(List<CompatibilityFixture> fixtures, Kind kind) {
        return fixtures.stream().filter(f -> f.kind() == kind).toList();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generated APIs

    enum MemberKind {
        METHOD,
        CONSTRUCTOR,
        FIELD
    }

    /**
     * A public or protected member.
     *
     * @param parameters parameter types (empty for fields)
     * @param type       return type ({@code void} for constructors) or field type
     */
    record Member(MemberKind kind, String name, List<String> parameters, String type, boolean isStatic,
                  boolean isProtected) {
        Member {
            parameters = List.copyOf(parameters);
        }

        static Member method(String name, List<String> parameters, String returns, boolean isStatic,
                             boolean isProtected) {
            return new Member(MemberKind.METHOD, name, parameters, returns, isStatic, isProtected);
        }

        static Member constructor(String... parameters) {
            return new Member(MemberKind.CONSTRUCTOR, "<init>", List.of(parameters), "void", false, false);
        }

        static Member field(String name, String type, boolean isStatic) {
            return new Member(MemberKind.FIELD, name, List.of(), type, isStatic, false);
        }

        /** Methods are unique by name and arity, fields by name. */
        String key() {
            return kind + " " + name + (kind == MemberKind.FIELD ? "" : "/" + parameters.size());
        }

        Member withName(String newName) {
            return new Member(kind, newName, parameters, type, isStatic, isProtected);
        }

        Member withParameters(List<String> newParameters) {
            return new Member(kind, name, newParameters, type, isStatic, isProtected);
        }

        Member withType(String newType) {
            return new Member(kind, name, parameters, newType, isStatic, isProtected);
        }

        Member withStatic(boolean newStatic) {
            return new Member(kind, name, parameters, type, newStatic, isProtected);
        }

        Member withProtected(boolean newProtected) {
            return new Member(kind, name, parameters, type, isStatic, newProtected);
        }

        @Override
        public String toString() {
            return (isProtected ? "protected " : "") + (isStatic ? "static " : "")
                   + (kind == MemberKind.FIELD ? type + " " + name
                                               : (kind == MemberKind.CONSTRUCTOR ? "" : type + " ") + name + "("
                                                 + String.join(", ", parameters) + ")");
        }
    }

    /** A public type of package {@code p}; nested types use binary names ({@code Outer$Inner}). */
    record Type(String name, boolean isInterface, boolean isFinal, String superclass, List<String> interfaces,
                List<Member> members) {
        Type {
            interfaces = List.copyOf(interfaces);
            members = List.copyOf(members);
        }

        Type withFinal(boolean newFinal) {
            return new Type(name, isInterface, newFinal, superclass, interfaces, members);
        }

        Type withSuperclass(String newSuperclass) {
            return new Type(name, isInterface, isFinal, newSuperclass, interfaces, members);
        }

        Type withInterfaces(List<String> newInterfaces) {
            return new Type(name, isInterface, isFinal, superclass, newInterfaces, members);
        }

        Type withMembers(List<Member> newMembers) {
            return new Type(name, isInterface, isFinal, superclass, interfaces, newMembers);
        }

        @Override
        public String toString() {
            return (isFinal ? "final " : "") + (isInterface ? "interface " : "class ") + name
                   + (superclass == null ? "" : " extends " + superclass)
                   + (interfaces.isEmpty() ? "" : (isInterface ? " extends " : " implements ") + interfaces)
                   + " " + members;
        }
    }

    /** A generated API: its types by binary name. */
    record Api(SortedMap<String, Type> types) {
        Api {
            types = Collections.unmodifiableSortedMap(new TreeMap<>(types));
        }

        Type type(String name) {
            return types.get(name);
        }

        Api with(Type type) {
            SortedMap<String, Type> copy = new TreeMap<>(types);
            copy.put(type.name(), type);
            return new Api(copy);
        }

        @Override
        public String toString() {
            return types.values().stream().map(Type::toString).collect(Collectors.joining("\n  ", "\n  ", ""));
        }
    }

    /** The random parts of a baseline API besides member lists. */
    record Shape(String baseErrorRoot, boolean baseErrorMarker, boolean serviceErrorFinal,
                 boolean serviceErrorMarker, boolean modelSerializable, boolean clientCloseable) {
    }

    static Api baseline(Shape shape, List<Member> baseErrorMethods, List<Member> serviceErrorMethods,
                        List<Tuple2<String, String>> properties, Member modelField, List<Member> operations) {
        SortedMap<String, Type> types = new TreeMap<>();
        types.put(MARKER, new Type(MARKER, true, false, null, List.of(), List.of()));

        List<Member> base = new ArrayList<>(List.of(Member.constructor(STRING)));
        base.addAll(baseErrorMethods);
        types.put(BASE_ERROR, new Type(BASE_ERROR, false, false, shape.baseErrorRoot(),
                                       shape.baseErrorMarker() ? List.of(MARKER) : List.of(), base));

        List<Member> service = new ArrayList<>(List.of(Member.constructor(STRING)));
        // A protected member of a final class has no V2 callers.
        serviceErrorMethods.forEach(m -> service.add(shape.serviceErrorFinal() ? m.withProtected(false) : m));
        types.put(SERVICE_ERROR, new Type(SERVICE_ERROR, false, shape.serviceErrorFinal(), BASE_ERROR,
                                          shape.serviceErrorMarker() ? List.of(MARKER) : List.of(), service));

        List<Member> model = new ArrayList<>();
        model.add(Member.method("builder", List.of(), BUILDER, true, false));
        model.add(Member.method("toBuilder", List.of(), BUILDER, false, false));
        List<Member> builder = new ArrayList<>();
        for (Tuple2<String, String> property : properties) {
            model.add(Member.method(property.get1(), List.of(), property.get2(), false, false));
            builder.add(Member.method(property.get1(), List.of(property.get2()), BUILDER, false, false));
        }
        builder.add(Member.method("build", List.of(), MODEL, false, false));
        if (modelField != null) {
            model.add(modelField);
        }
        types.put(MODEL, new Type(MODEL, false, true, null,
                                  shape.modelSerializable() ? List.of(SERIALIZABLE) : List.of(), model));
        types.put(BUILDER, new Type(BUILDER, true, false, null, List.of(), builder));

        List<Member> client = new ArrayList<>(List.of(Member.method("create", List.of(), CLIENT, true, false)));
        client.addAll(operations);
        types.put(CLIENT, new Type(CLIENT, true, false, null,
                                   shape.clientCloseable() ? List.of(AUTO_CLOSEABLE) : List.of(), client));
        return new Api(types);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Candidate mutations

    enum MutationKind {
        RENAME,
        REMOVE,
        ADD_OVERLOAD,
        CHANGE_PARAMETER,
        CHANGE_TYPE,
        TOGGLE_STATIC,
        TOGGLE_PROTECTED,
        TOGGLE_FINAL,
        REMOVE_SUPERTYPE,
        ADD_SUPERTYPE,
        CHANGE_SUPERCLASS
    }

    /** A mutation, resolved against the API it applies to: indices select the type, member and detail modulo size. */
    record Mutation(MutationKind kind, int type, int member, int detail) {

        Api apply(Api api) {
            List<Type> types = List.copyOf(api.types().values());
            Type t = types.get(type % types.size());
            return switch (kind) {
                case TOGGLE_FINAL -> {
                    if (t.isInterface()
                        || !t.isFinal() && types.stream().anyMatch(o -> t.name().equals(o.superclass()))) {
                        yield api;
                    }
                    yield api.with(t.withFinal(!t.isFinal()));
                }
                case REMOVE_SUPERTYPE -> {
                    if (t.interfaces().isEmpty()) {
                        yield api;
                    }
                    List<String> interfaces = new ArrayList<>(t.interfaces());
                    interfaces.remove(detail % interfaces.size());
                    yield api.with(t.withInterfaces(interfaces));
                }
                case ADD_SUPERTYPE -> {
                    // Marker has no members and AutoCloseable only goes on interfaces nothing implements, so the
                    // candidate still compiles.
                    List<String> options = new ArrayList<>(t.isInterface() ? List.of(MARKER, AUTO_CLOSEABLE)
                                                                           : List.of(MARKER, SERIALIZABLE));
                    options.remove(t.name());
                    if (t.name().equals(MARKER)) {
                        options.clear();
                    }
                    options.removeAll(t.interfaces());
                    if (options.isEmpty()) {
                        yield api;
                    }
                    List<String> interfaces = new ArrayList<>(t.interfaces());
                    interfaces.add(options.get(detail % options.size()));
                    yield api.with(t.withInterfaces(interfaces));
                }
                case CHANGE_SUPERCLASS -> {
                    if (t.superclass() == null) {
                        yield api;
                    }
                    List<String> options = new ArrayList<>(EXCEPTION_ROOTS);
                    if (t.name().equals(SERVICE_ERROR) && !api.type(BASE_ERROR).isFinal()) {
                        options.add(BASE_ERROR);
                    }
                    yield api.with(t.withSuperclass(options.get(detail % options.size())));
                }
                default -> applyToMember(api, t);
            };
        }

        private Api applyToMember(Api api, Type t) {
            List<Member> members = new ArrayList<>(t.members());
            List<Integer> mutable = new ArrayList<>();
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).kind() != MemberKind.CONSTRUCTOR) {
                    mutable.add(i);
                }
            }
            if (mutable.isEmpty()) {
                return api;
            }
            int index = mutable.get(member % mutable.size());
            Member m = members.get(index);
            boolean method = m.kind() == MemberKind.METHOD;
            switch (kind) {
                case RENAME -> members.set(index, m.withName(m.name() + "V3"));
                case REMOVE -> members.remove(index);
                case ADD_OVERLOAD -> {
                    if (!method) {
                        return api;
                    }
                    // An arity no method of the type has, so every (name, arity) stays unique.
                    int arity = t.members().stream().filter(x -> x.kind() == MemberKind.METHOD)
                                 .mapToInt(x -> x.parameters().size()).max().orElse(0) + 1;
                    members.add(m.withParameters(Collections.nCopies(arity, OBJECT)));
                }
                case CHANGE_PARAMETER -> {
                    if (!method || m.parameters().isEmpty()) {
                        return api;
                    }
                    List<String> parameters = new ArrayList<>(m.parameters());
                    parameters.set(detail % parameters.size(),
                                   VALUE_TYPES.get(detail / parameters.size() % VALUE_TYPES.size()));
                    members.set(index, m.withParameters(parameters));
                }
                case CHANGE_TYPE -> {
                    if (m.type().equals("void")) {
                        return api;
                    }
                    members.set(index, m.withType(VALUE_TYPES.get(detail % VALUE_TYPES.size())));
                }
                case TOGGLE_STATIC -> members.set(index, m.withStatic(!m.isStatic()));
                case TOGGLE_PROTECTED -> {
                    if (t.isInterface()) {
                        return api;
                    }
                    members.set(index, m.withProtected(!m.isProtected()));
                }
                default -> throw new IllegalStateException(kind.toString());
            }
            return api.with(t.withMembers(members));
        }

        @Override
        public String toString() {
            return kind + "(" + type + ", " + member + ", " + detail + ")";
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Scenario

    /** One preserved API element: a type ({@code member == null}) or a member, and its claimed dimensions. */
    record Claim(String id, Type owner, Member member, SortedSet<CompatibilityDimension> dims) {
    }

    /**
     * One try: the baseline, a claim choice per baseline element (in type order, each type before its members), and
     * the candidate mutations.
     */
    record Scenario(Api baseline, List<Integer> claimChoices, List<Mutation> mutations) {

        Api candidate() {
            Api api = baseline;
            for (Mutation m : mutations) {
                api = m.apply(api);
            }
            return api;
        }

        SortedMap<String, Claim> claims() {
            SortedMap<String, Claim> claims = new TreeMap<>();
            int i = 0;
            for (Type t : baseline.types().values()) {
                List<Member> elements = new ArrayList<>();
                elements.add(null);
                elements.addAll(t.members());
                for (Member m : elements) {
                    SortedSet<CompatibilityDimension> dims = switch (claimChoices.get(i++ % claimChoices.size())) {
                        case 0 -> new TreeSet<>();
                        case 1 -> new TreeSet<>(Set.of(SOURCE));
                        case 2 -> new TreeSet<>(Set.of(BINARY));
                        default -> new TreeSet<>(Set.of(SOURCE, BINARY));
                    };
                    if (!dims.isEmpty()) {
                        String id = Oracle.entryId(t, m);
                        claims.put(id, new Claim(id, t, m, dims));
                    }
                }
            }
            if (claims.isEmpty()) {
                // The manifest needs at least one entry for the '*' test scope.
                Type marker = baseline.type(MARKER);
                claims.put(Oracle.entryId(marker, null), new Claim(Oracle.entryId(marker, null), marker, null,
                                                                   new TreeSet<>(Set.of(SOURCE))));
            }
            return claims;
        }

        @Override
        public String toString() {
            return "Scenario(claims " + claims().values().stream().map(c -> c.id() + " " + c.dims()).toList()
                   + ", mutations " + mutations + ", baseline" + baseline + "\n candidate" + candidate() + ")";
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Boolean> bool = Arbitraries.of(true, false);
        Arbitrary<Shape> shape = Combinators.combine(Arbitraries.of(EXCEPTION_ROOTS), bool, bool, bool, bool, bool)
                                            .as(Shape::new);
        Arbitrary<List<Member>> baseErrorMethods = methods(BASE_ERROR_METHODS, true).list().ofMaxSize(2)
                                                                                     .uniqueElements(Member::key);
        Arbitrary<List<Member>> serviceErrorMethods = methods(SERVICE_ERROR_METHODS, true).list().ofMaxSize(2)
                                                                                           .uniqueElements(Member::key);
        Arbitrary<List<Tuple2<String, String>>> properties =
            Combinators.combine(Arbitraries.of(PROPERTIES), Arbitraries.of(VALUE_TYPES)).as(Tuple::of)
                       .list().ofMinSize(1).ofMaxSize(3).uniqueElements(Tuple2::get1);
        Arbitrary<Member> modelField = Combinators.combine(Arbitraries.of(VALUE_TYPES), bool)
                                                  .as((type, isStatic) -> Member.field("DEFAULT", type, isStatic))
                                                  .injectNull(0.4);
        Arbitrary<List<Member>> operations =
            Combinators.combine(Arbitraries.of(OPERATIONS), Arbitraries.of(VALUE_TYPES).list().ofMaxSize(2),
                                Arbitraries.of(VALUE_TYPES))
                       .as((name, parameters, returns) -> Member.method(name, parameters, returns, false, false))
                       .list().ofMinSize(1).ofMaxSize(4).uniqueElements(Member::key);
        Arbitrary<Api> baseline = Combinators.combine(shape, baseErrorMethods, serviceErrorMethods, properties,
                                                      modelField, operations)
                                             .as(PreservedApiDescriptorsPropertyTest::baseline);
        Arbitrary<List<Integer>> claims = Arbitraries.integers().between(0, CLAIM_CHOICES - 1).list()
                                                     .ofSize(MAX_ELEMENTS);
        Arbitrary<Integer> index = Arbitraries.integers().between(0, 99);
        Arbitrary<Mutation> mutation = Combinators.combine(Arbitraries.of(MutationKind.class), index, index, index)
                                                  .as(Mutation::new);
        Arbitrary<List<Mutation>> mutations = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(List.of())),
            Tuple.of(3, mutation.list().ofMinSize(1).ofMaxSize(3)));
        return Combinators.combine(baseline, claims, mutations).as(Scenario::new);
    }

    private static Arbitrary<Member> methods(List<String> names, boolean allowProtected) {
        Arbitrary<Boolean> bool = Arbitraries.of(true, false);
        return Combinators.combine(Arbitraries.of(names), Arbitraries.of(VALUE_TYPES).list().ofMaxSize(2),
                                   Arbitraries.of(RETURN_TYPES), bool, allowProtected ? bool : Arbitraries.just(false))
                          .as(Member::method);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracle

    /**
     * Expected fixtures, recorded baseline values and verdicts, computed from the API descriptions alone with the
     * documented rules of {@link CompatibilityFixtureGenerator} and {@link Kind}.
     */
    static final class Oracle {
        private Oracle() {
        }

        static String descriptor(String type) {
            return switch (type) {
                case "int" -> "I";
                case "long" -> "J";
                case "boolean" -> "Z";
                case "void" -> "V";
                default -> "L" + type.replace('.', '/') + ";";
            };
        }

        static String descriptor(Member m) {
            if (m.kind() == MemberKind.FIELD) {
                return descriptor(m.type());
            }
            return m.parameters().stream().map(Oracle::descriptor).collect(Collectors.joining("", "(", ")"))
                   + descriptor(m.type());
        }

        static String entryId(Type owner, Member m) {
            if (m == null) {
                return "api:" + owner.name();
            }
            return "api:" + owner.name() + "#" + m.name() + (m.kind() == MemberKind.FIELD ? ":" : "")
                   + descriptor(m);
        }

        static Target target(Claim claim) {
            Type owner = claim.owner();
            Member m = claim.member();
            String ownerKind = owner.isInterface() ? "interface" : "class";
            if (m == null) {
                return new Target(TargetKind.TYPE, owner.name(), ownerKind, owner.isFinal(), null, null, false, false);
            }
            TargetKind kind = switch (m.kind()) {
                case METHOD -> TargetKind.METHOD;
                case CONSTRUCTOR -> TargetKind.CONSTRUCTOR;
                case FIELD -> TargetKind.FIELD;
            };
            return new Target(kind, owner.name(), ownerKind, owner.isFinal(), m.name(), descriptor(m), m.isStatic(),
                              m.isProtected());
        }

        /**
         * The fixtures of a claimed dimension: the signature (member) or hierarchy (type) comparison, plus a compile
         * caller (source) unless a protected member's owner cannot be subclassed, and a link caller (binary) unless
         * the member is protected or its method reference would be ambiguous.
         */
        static List<Kind> fixtureKinds(Claim claim, CompatibilityDimension dim) {
            Member m = claim.member();
            List<Kind> kinds = new ArrayList<>(List.of(m == null ? Kind.HIERARCHY : Kind.SIGNATURE));
            if (dim == SOURCE && (m == null || !m.isProtected() || !claim.owner().isFinal())) {
                kinds.add(Kind.COMPILE);
            }
            if (dim == BINARY && (m == null || !m.isProtected() && !ambiguousReference(claim.owner(), m))) {
                kinds.add(Kind.LINK);
            }
            return kinds;
        }

        /**
         * Whether {@code Owner::name} would be ambiguous: the reference takes an instance target's receiver as its
         * first parameter, so a public same-named method of the other static-ness with one parameter more (for an
         * instance target) or less (for a static target) is also a candidate (JLS 15.13.1).
         */
        static boolean ambiguousReference(Type owner, Member m) {
            int rivalArity = m.parameters().size() + (m.isStatic() ? -1 : 1);
            return m.kind() == MemberKind.METHOD
                   && owner.members().stream().anyMatch(x -> x.kind() == MemberKind.METHOD && x.name().equals(m.name())
                                                             && x.isStatic() != m.isStatic() && !x.isProtected()
                                                             && x.parameters().size() == rivalArity);
        }

        static String superclass(Api api, String type) {
            Type t = api.type(type);
            return t != null ? t.superclass() : JDK_SUPERCLASS.get(type);
        }

        static List<String> directSupertypes(Api api, String type) {
            List<String> result = new ArrayList<>();
            String superclass = superclass(api, type);
            if (superclass != null) {
                result.add(superclass);
            }
            Type t = api.type(type);
            result.addAll(t != null ? t.interfaces() : JDK_INTERFACES.getOrDefault(type, List.of()));
            return result;
        }

        /** Transitive supertypes other than Object. */
        static Set<String> supertypes(Api api, String type) {
            Set<String> result = new TreeSet<>();
            Deque<String> work = new ArrayDeque<>(List.of(type));
            while (!work.isEmpty()) {
                for (String s : directSupertypes(api, work.removeFirst())) {
                    if (result.add(s)) {
                        work.add(s);
                    }
                }
            }
            return result;
        }

        static List<String> superclassChain(Api api, String type) {
            List<String> chain = new ArrayList<>();
            for (String s = superclass(api, type); s != null; s = superclass(api, s)) {
                chain.add(s);
            }
            return chain;
        }

        /**
         * The supertypes a preserved type keeps: JDK types, for exceptions the preserved superclasses (exception
         * inheritance), and for the binary dimension the binary-preserved supertypes.
         */
        static List<String> requiredSupertypes(Api v2, Map<String, Claim> claims, String type,
                                               CompatibilityDimension dim) {
            List<String> chain = superclassChain(v2, type);
            boolean exception = chain.contains(THROWABLE);
            return supertypes(v2, type).stream()
                                       .filter(s -> s.startsWith("java.")
                                                    || exception && chain.contains(s) && claims.containsKey("api:" + s)
                                                    || dim == BINARY && claims.containsKey("api:" + s)
                                                       && claims.get("api:" + s).dims().contains(BINARY))
                                       .sorted().toList();
        }

        /**
         * Whether the candidate keeps a claimed dimension. With {@code javac}, decides instead whether javac accepts
         * the source caller: it also allows boxing to Object, int-to-long widening and a static member reached
         * through an instance expression.
         */
        static boolean kept(Claim claim, CompatibilityDimension dim, Api v2, Api v3, Map<String, Claim> claims,
                            boolean javac) {
            Type candidate = v3.type(claim.owner().name());
            Member m = claim.member();
            if (m == null) {
                if (dim == BINARY && !claim.owner().isFinal() && candidate.isFinal()) {
                    return false;
                }
                return supertypes(v3, candidate.name()).containsAll(requiredSupertypes(v2, claims, candidate.name(),
                                                                                       dim));
            }
            Member c = candidate.members().stream()
                                .filter(x -> x.kind() == m.kind() && x.name().equals(m.name())
                                             && x.parameters().size() == m.parameters().size())
                                .findFirst().orElse(null);
            // javac accepts a static class member (not a static interface method) reached through an instance.
            if (c == null
                || c.isStatic() != m.isStatic() && !(javac && c.isStatic() && !candidate.isInterface())
                || c.isProtected() && !m.isProtected()) {
                return false;
            }
            if (m.isProtected() && candidate.isFinal()) {
                // The V2 callers of a protected member are subclasses, so the owner must stay subclassable.
                return false;
            }
            if (dim == BINARY) {
                return c.parameters().equals(m.parameters()) && c.type().equals(m.type());
            }
            for (int i = 0; i < m.parameters().size(); i++) {
                if (!assignable(m.parameters().get(i), c.parameters().get(i), javac)) {
                    return false;
                }
            }
            return m.kind() == MemberKind.CONSTRUCTOR || assignable(c.type(), m.type(), javac);
        }

        static boolean assignable(String from, String to, boolean javac) {
            if (from.equals(to) || REFERENCE_SUPERTYPES.getOrDefault(from, Set.of()).contains(to)) {
                return true;
            }
            boolean primitive = !REFERENCE_SUPERTYPES.containsKey(from);
            return javac && (primitive && to.equals(OBJECT) || from.equals("int") && to.equals("long"));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Rendering and compiling APIs

    private static List<String> compileApi(Api api, Path sources, Path classes) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Type t : api.types().values()) {
            if (!t.name().contains("$")) {
                Path file = sources.resolve(t.name().replace('.', '/') + ".java");
                Files.createDirectories(file.getParent());
                Files.writeString(file, "package p;\n\n" + render(api, t, ""), StandardCharsets.UTF_8);
                files.add(file);
            }
        }
        return Javac.compile(files, "", classes).values().stream().flatMap(List::stream).toList();
    }

    private static String render(Api api, Type t, String indent) {
        String simple = t.name().substring(t.name().lastIndexOf('.') + 1);
        simple = simple.substring(simple.lastIndexOf('$') + 1);
        StringBuilder out = new StringBuilder(indent).append("public ").append(t.isFinal() ? "final " : "")
                                                     .append(t.isInterface() ? "interface " : "class ").append(simple);
        List<String> interfaces = t.interfaces().stream().map(PreservedApiDescriptorsPropertyTest::source).toList();
        if (t.superclass() != null) {
            out.append(" extends ").append(source(t.superclass()));
        }
        if (!interfaces.isEmpty()) {
            out.append(t.isInterface() ? " extends " : " implements ").append(String.join(", ", interfaces));
        }
        out.append(" {\n");
        String in = indent + "    ";
        if (!t.isInterface() && t.members().stream().noneMatch(m -> m.kind() == MemberKind.CONSTRUCTOR)) {
            out.append(in).append("private ").append(simple).append("() {\n").append(in).append("}\n");
        }
        for (Member m : t.members()) {
            out.append(in);
            if (!t.isInterface()) {
                out.append(m.isProtected() ? "protected " : "public ");
            }
            out.append(m.isStatic() ? "static " : "");
            List<String> parameters = new ArrayList<>();
            for (int i = 0; i < m.parameters().size(); i++) {
                parameters.add(source(m.parameters().get(i)) + " p" + i);
            }
            String signature = "(" + String.join(", ", parameters) + ")";
            switch (m.kind()) {
                case FIELD -> out.append(source(m.type())).append(' ').append(m.name()).append(";\n");
                case CONSTRUCTOR -> out.append(simple).append(signature).append(" {")
                                       .append(t.superclass() == null ? "" : " super(p0);").append(" }\n");
                case METHOD -> {
                    out.append(source(m.type())).append(' ').append(m.name()).append(signature);
                    if (t.isInterface() && !m.isStatic()) {
                        out.append(";\n");
                    } else {
                        out.append(" {").append(m.type().equals("void") ? "" : " return " + defaultValue(m.type())
                                                                              + ";")
                           .append(" }\n");
                    }
                }
            }
        }
        for (Type nested : api.types().values()) {
            if (nested.name().startsWith(t.name() + "$")
                && nested.name().indexOf('$', t.name().length() + 1) < 0) {
                out.append(render(api, nested, in));
            }
        }
        return out.append(indent).append("}\n").toString();
    }

    private static String source(String binaryName) {
        return binaryName.replace('$', '.');
    }

    private static String defaultValue(String type) {
        return switch (type) {
            case "int", "long" -> "0";
            case "boolean" -> "false";
            default -> "null";
        };
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fixture execution

    /** Compiles and links the generated callers. Each caller is renamed so that callers compile as one batch. */
    static final class Callers {
        private Callers() {
        }

        /** Whether each {@code COMPILE} caller compiles against the candidate. */
        static Map<String, Boolean> compileOutcomes(List<CompatibilityFixture> fixtures, Path candidate, Path work)
            throws IOException {
            Map<String, Boolean> outcomes = new TreeMap<>();
            Map<Path, CompatibilityFixture> pending = write(fixtures, work.resolve("src"));
            // Callers are independent, so a caller with an error fails on its own; javac may stop attributing the
            // remaining callers after an error, so they are compiled again until a batch has no error.
            for (int round = 0; !pending.isEmpty(); round++) {
                Map<Path, List<String>> errors = Javac.compile(List.copyOf(pending.keySet()), candidate.toString(),
                                                               work.resolve("classes-" + round));
                if (errors.containsKey(null)) {
                    throw new AssertionError("javac errors without a source: " + errors.get(null));
                }
                for (Path failed : errors.keySet()) {
                    CompatibilityFixture f = pending.remove(failed);
                    if (f == null) {
                        throw new AssertionError("javac reported errors for an unexpected file " + failed + ": "
                                                 + errors.get(failed));
                    }
                    outcomes.put(f.id(), false);
                }
                if (errors.isEmpty()) {
                    pending.values().forEach(f -> outcomes.put(f.id(), true));
                    pending.clear();
                }
            }
            return outcomes;
        }

        /**
         * Whether each {@code LINK} caller, compiled against the baseline, links against the candidate: its
         * {@code link()} completes, or fails with a {@link NullPointerException} on an instance field read (which
         * happens after the field reference resolved), but not with a {@link LinkageError}.
         */
        static Map<String, Boolean> linkOutcomes(List<CompatibilityFixture> fixtures, Path baseline, Path candidate,
                                                 Path work) throws Exception {
            Map<Path, CompatibilityFixture> callers = write(fixtures, work.resolve("src"));
            Path classes = work.resolve("classes");
            Map<Path, List<String>> errors = Javac.compile(List.copyOf(callers.keySet()), baseline.toString(), classes);
            assertEquals(Map.of(), errors, "link callers are valid V2 code");
            Map<String, Boolean> outcomes = new TreeMap<>();
            for (Map.Entry<Path, CompatibilityFixture> caller : callers.entrySet()) {
                String className = caller.getKey().getFileName().toString().replace(".java", "");
                CompatibilityFixture f = caller.getValue();
                Throwable failure;
                try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL(),
                                                                           candidate.toUri().toURL()},
                                                                ClassLoader.getPlatformClassLoader())) {
                    Class.forName(className, true, loader).getMethod("link").invoke(null);
                    failure = null;
                } catch (InvocationTargetException e) {
                    failure = e.getCause();
                } catch (LinkageError e) {
                    failure = e;
                }
                boolean linked = failure == null
                                 || failure instanceof NullPointerException
                                    && f.target().kind() == TargetKind.FIELD && !f.target().isStatic();
                if (!linked && !(failure instanceof LinkageError)) {
                    throw new AssertionError(f.id() + ": the link caller failed without a linkage error", failure);
                }
                outcomes.put(f.id(), linked);
            }
            return outcomes;
        }

        private static Map<Path, CompatibilityFixture> write(List<CompatibilityFixture> fixtures, Path directory)
            throws IOException {
            Files.createDirectories(directory);
            directory = directory.toAbsolutePath().normalize();
            Map<Path, CompatibilityFixture> files = new LinkedHashMap<>();
            for (int i = 0; i < fixtures.size(); i++) {
                String name = CompatibilityFixture.CALLER_CLASS + i;
                Path file = directory.resolve(name + ".java");
                Files.writeString(file, fixtures.get(i).source().replace(CompatibilityFixture.CALLER_CLASS, name),
                                  StandardCharsets.UTF_8);
                files.put(file, fixtures.get(i));
            }
            return files;
        }
    }

    /**
     * Evaluates {@code SIGNATURE} and {@code HIERARCHY} fixtures against candidate classes with the contract of
     * {@link Kind#SIGNATURE} and {@link Kind#HIERARCHY}, as {@code ApiShapeCompatibilityTest} does.
     */
    static final class ShapeExecutor {
        private ShapeExecutor() {
        }

        static boolean passes(CompatibilityFixture fixture, ClassLoader v3) {
            Target target = fixture.target();
            boolean binary = fixture.dimension() == BINARY;
            Class<?> owner = load(target.owner(), v3);
            if (owner == null
                || !(Modifier.isPublic(owner.getModifiers())
                     || target.isProtected() && Modifier.isProtected(owner.getModifiers()))) {
                return false;
            }
            if (fixture.kind() == Kind.HIERARCHY) {
                if (!target.ownerKind().equals(typeKind(owner))
                    || binary && !target.ownerFinal() && Modifier.isFinal(owner.getModifiers())) {
                    return false;
                }
                for (String supertype : fixture.supertypes()) {
                    Class<?> s = load(supertype, v3);
                    if (s == null || !s.isAssignableFrom(owner)) {
                        return false;
                    }
                }
                return true;
            }
            if (binary && !target.ownerKind().equals(typeKind(owner))) {
                return false;
            }
            java.lang.reflect.Member member = target.kind() == TargetKind.FIELD ? field(owner, target, binary, v3)
                                                                                : executable(owner, target, binary,
                                                                                             v3);
            if (member == null || target.isStatic() != Modifier.isStatic(member.getModifiers())) {
                return false;
            }
            int modifiers = member.getModifiers();
            if (!(Modifier.isPublic(modifiers) || target.isProtected() && Modifier.isProtected(modifiers))) {
                return false;
            }
            // The V2 callers of a protected member are subclasses.
            return !(target.isProtected() && !target.ownerFinal() && Modifier.isFinal(owner.getModifiers()));
        }

        private static Field field(Class<?> owner, Target target, boolean binary, ClassLoader v3) {
            Class<?> v2Type = MethodType.fromMethodDescriptorString("()" + target.descriptor(), v3).returnType();
            for (Class<?> type : hierarchy(owner)) {
                for (Field f : type.getDeclaredFields()) {
                    if (f.getName().equals(target.name())
                        && (binary ? f.getType() == v2Type : v2Type.isAssignableFrom(f.getType()))) {
                        return f;
                    }
                }
            }
            return null;
        }

        private static Executable executable(Class<?> owner, Target target, boolean binary, ClassLoader v3) {
            MethodType v2 = MethodType.fromMethodDescriptorString(target.descriptor(), v3);
            List<Executable> candidates = new ArrayList<>();
            if (target.kind() == TargetKind.CONSTRUCTOR) {
                candidates.addAll(List.of(owner.getDeclaredConstructors()));
            } else {
                for (Class<?> type : hierarchy(owner)) {
                    for (Method m : type.getDeclaredMethods()) {
                        if (m.getName().equals(target.name()) && (binary || !m.isBridge())) {
                            candidates.add(m);
                        }
                    }
                }
            }
            return candidates.stream().filter(e -> matches(e, v2, binary)).findFirst().orElse(null);
        }

        private static boolean matches(Executable e, MethodType v2, boolean binary) {
            Class<?>[] parameters = e.getParameterTypes();
            if (parameters.length != v2.parameterCount()) {
                return false;
            }
            for (int i = 0; i < parameters.length; i++) {
                Class<?> v2Parameter = v2.parameterType(i);
                if (binary ? parameters[i] != v2Parameter
                           : parameters[i].isPrimitive() ? parameters[i] != v2Parameter
                                                         : !parameters[i].isAssignableFrom(v2Parameter)) {
                    return false;
                }
            }
            if (e instanceof Constructor) {
                return true;
            }
            Class<?> returns = ((Method) e).getReturnType();
            return binary ? returns == v2.returnType()
                          : returns == v2.returnType()
                            || !returns.isPrimitive() && v2.returnType().isAssignableFrom(returns);
        }

        /** The type, its superclasses, then every superinterface: the JVM's member resolution order. */
        private static List<Class<?>> hierarchy(Class<?> type) {
            Set<Class<?>> result = new LinkedHashSet<>();
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                result.add(c);
            }
            Deque<Class<?>> work = new ArrayDeque<>(result);
            while (!work.isEmpty()) {
                for (Class<?> i : work.removeFirst().getInterfaces()) {
                    if (result.add(i)) {
                        work.add(i);
                    }
                }
            }
            return List.copyOf(result);
        }

        private static String typeKind(Class<?> type) {
            if (type.isAnnotation()) {
                return "annotation";
            }
            if (type.isInterface()) {
                return "interface";
            }
            return type.isEnum() ? "enum" : "class";
        }

        private static Class<?> load(String name, ClassLoader v3) {
            try {
                return Class.forName(name, false, v3);
            } catch (ClassNotFoundException | LinkageError e) {
                return null;
            }
        }
    }

    /** In-process javac with errors grouped by source file ({@code null} for errors without one). */
    static final class Javac {
        private Javac() {
        }

        static Map<Path, List<String>> compile(List<Path> files, String classPath, Path output) throws IOException {
            Files.createDirectories(output);
            if (files.isEmpty()) {
                return Map.of();
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager manager = JAVAC.getStandardFileManager(diagnostics, Locale.ROOT,
                                                                                StandardCharsets.UTF_8)) {
                List<String> options = List.of("-proc:none", "--release", "21", "-nowarn", "-Xlint:none",
                                               "-Xmaxerrs", "10000", "-classpath", classPath, "-d",
                                               output.toString());
                JAVAC.getTask(null, manager, diagnostics, options, null, manager.getJavaFileObjectsFromPaths(files))
                     .call();
            }
            Map<Path, List<String>> errors = new LinkedHashMap<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    Path file = d.getSource() == null ? null : Path.of(d.getSource().toUri());
                    errors.computeIfAbsent(file, f -> new ArrayList<>()).add(d.getMessage(Locale.ROOT));
                }
            }
            return errors;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers

    private static URLClassLoader loader(Path classes) throws IOException {
        return new URLClassLoader(new URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }

    private static ManifestEntry preserved(String id, Set<CompatibilityDimension> dims) {
        return new ManifestEntry(id, EntryKind.ofId(id).orElseThrow(), "V2 " + id,
                                 new SourceLocation("software.amazon.awssdk:test", null), false,
                                 Classification.PRESERVED, new TreeSet<>(dims), "because", List.of(), null, null, null,
                                 null, null, new TreeSet<>());
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
