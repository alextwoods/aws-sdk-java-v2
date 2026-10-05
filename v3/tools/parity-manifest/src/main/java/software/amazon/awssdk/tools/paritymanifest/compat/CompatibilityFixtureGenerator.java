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

import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.BEHAVIOR;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.BINARY;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.CONFIGURATION;
import static software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension.SOURCE;
import static software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.CALLER_CLASS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import software.amazon.awssdk.tools.paritymanifest.ApiClassFile;
import software.amazon.awssdk.tools.paritymanifest.Classification;
import software.amazon.awssdk.tools.paritymanifest.CompatibilityDimension;
import software.amazon.awssdk.tools.paritymanifest.EntryGlobs;
import software.amazon.awssdk.tools.paritymanifest.EntryKind;
import software.amazon.awssdk.tools.paritymanifest.ManifestEntry;
import software.amazon.awssdk.tools.paritymanifest.ParityManifest;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.Kind;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.Target;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.TargetKind;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixtures.Note;

/**
 * Generates the source, descriptor, hierarchy and behavioral compatibility fixtures of the Parity Manifest entries in
 * a test scope (Requirements 17.1, 17.2, 17.3, 19.12), per compatibility dimension and classification:
 *
 * <table>
 *   <caption>Fixtures per {@code api:} entry</caption>
 *   <tr><th>Entry</th><th>Fixtures</th></tr>
 *   <tr><td>claims {@code source}</td>
 *       <td>{@link Kind#COMPILE} and a {@code source} {@link Kind#SIGNATURE} (member) or {@link Kind#HIERARCHY}
 *       (type)</td></tr>
 *   <tr><td>claims {@code binary}</td>
 *       <td>{@link Kind#LINK} and a {@code binary} {@link Kind#SIGNATURE} or {@link Kind#HIERARCHY}</td></tr>
 *   <tr><td>{@code ADAPTED}/{@code EXCLUDED} without {@code source}</td><td>{@link Kind#COMPILE_REJECTED}</td></tr>
 *   <tr><td>{@code ADAPTED}/{@code EXCLUDED} without {@code binary}</td><td>{@link Kind#LINK_REJECTED}</td></tr>
 * </table>
 *
 * <p>A non-preserved entry must not be silently usable in a dimension it does not claim: V2 source or bytecode that
 * still compiles or links against the candidate means the surface is either preserved (and the manifest under-claims)
 * or silently changed. A {@code PRESERVED} entry's unclaimed dimensions are merely unverified and get no fixture.
 *
 * <p>Every entry, of any kind, that claims {@code behavior} or {@code configuration} gets a {@link Kind#BEHAVIOR}
 * fixture per claimed dimension; an {@code ADAPTED} entry without such a claim gets one for its verified differences,
 * and a non-{@code api:} {@code EXCLUDED} entry gets one when it names evidence. Behavior fixtures run the entry's
 * evidence tests, so such claims without evidence IDs are problems.
 *
 * <p><b>Callers.</b> Compile callers use the V2 surface the way V2 source does: a call with one typed argument per
 * erased parameter (so overload resolution selects exactly the recorded descriptor), its result assigned to the
 * erased V2 return type, a field read into the erased field type, a constructor invocation, or a class literal plus
 * one assignment to each required supertype. Protected members, and public constructors of abstract classes, are used
 * from an abstract subclass. Link callers are compiled against the baseline and reference the target from bytecode
 * that links it without running it: a method reference (a {@code CONSTANT_MethodHandle} resolved when its
 * {@code invokedynamic} first executes), a field access, or a class literal.
 *
 * <p><b>Required supertypes</b> of a type for a dimension are its transitive V2 supertypes (read from the baseline
 * class files, and from the running JDK for platform types) other than {@code Object} that are (a) JDK platform
 * types, (b) for a {@link Throwable}, superclasses classified {@code PRESERVED} (exception inheritance, 17.1), or (c)
 * for the binary dimension, supertypes claimed binary-preserved themselves. SDK supertypes that are neither are not
 * part of a source-preservation claim.
 *
 * <p>Generation fails, reporting every problem at once, when a scope glob matches no entry, an in-scope entry has no
 * applicable fixture, a behavior claim has no evidence, or the baseline class path lacks an in-scope type, member or
 * supertype. Fixture forms that cannot be expressed (for example a protected member of a class without an accessible
 * constructor) are recorded as {@link Note}s when the entry still has another fixture.
 */
public final class CompatibilityFixtureGenerator {
    private static final String SUPPRESS =
        "@SuppressWarnings({\"unchecked\", \"rawtypes\", \"deprecation\", \"removal\"})\n";
    private static final String THROWABLE = "java.lang.Throwable";

    private final ParityManifest manifest;
    private final BaselineClasses baseline;
    private final Map<String, ManifestEntry> entries;

    public CompatibilityFixtureGenerator(ParityManifest manifest, BaselineClasses baseline) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.entries = manifest.entries().stream()
                               .collect(Collectors.toMap(ManifestEntry::id, Function.identity(), (a, b) -> a));
    }

    /**
     * Generates the fixtures of every entry whose ID matches one of {@code scope} ({@code *} matches any characters)
     * and none of its exclusions: a scope element {@code !<glob>} removes the entries it matches from the scope (for
     * example a known launch gap that the launch gate reports and no fixture may claim). Every inclusion must match
     * an entry, and every exclusion an included entry.
     *
     * @throws CompatibilityFixtureException with every problem, if there are any
     */
    public CompatibilityFixtures generate(List<String> scope) throws CompatibilityFixtureException {
        List<String> problems = new ArrayList<>();
        List<String> included = scope.stream().filter(s -> !isExclusion(s)).toList();
        List<String> excluded = scope.stream().filter(CompatibilityFixtureGenerator::isExclusion).toList();
        if (included.isEmpty()) {
            problems.add("[scope] the test scope is empty; use '*' for every entry");
        }
        List<Pattern> globs = included.stream().map(CompatibilityFixtureGenerator::glob).toList();
        List<Pattern> exclusions = excluded.stream().map(s -> glob(s.substring(1))).toList();
        for (int i = 0; i < included.size(); i++) {
            Pattern glob = globs.get(i);
            if (manifest.entries().stream().noneMatch(e -> glob.matcher(e.id()).matches())) {
                problems.add("[scope] '" + included.get(i) + "' matches no manifest entry");
            }
        }
        for (int i = 0; i < excluded.size(); i++) {
            Pattern exclusion = exclusions.get(i);
            if (manifest.entries().stream().noneMatch(e -> exclusion.matcher(e.id()).matches()
                                                          && globs.stream().anyMatch(g -> g.matcher(e.id())
                                                                                           .matches()))) {
                problems.add("[scope] '" + excluded.get(i) + "' excludes no entry of the scope");
            }
        }
        List<CompatibilityFixture> fixtures = new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        for (ManifestEntry entry : manifest.entries()) {
            if (inScope(entry.id(), globs, exclusions)) {
                new EntryFixtures(entry).generate(fixtures, notes, problems);
            }
        }
        if (!problems.isEmpty()) {
            throw new CompatibilityFixtureException(problems.stream().distinct().sorted().toList());
        }
        return new CompatibilityFixtures(manifest.baseline().v2Version(), manifest.baseline().gitRevision(), scope,
                                         fixtures, notes);
    }

    /**
     * True if {@code entryId} is selected by {@code scope} ({@link #generate}): matched by an inclusion and by no
     * {@code !} exclusion ({@link EntryGlobs#inScope}).
     */
    public static boolean inScope(String entryId, List<String> scope) {
        return EntryGlobs.inScope(entryId, scope);
    }

    private static boolean inScope(String entryId, List<Pattern> globs, List<Pattern> exclusions) {
        return globs.stream().anyMatch(g -> g.matcher(entryId).matches())
               && exclusions.stream().noneMatch(g -> g.matcher(entryId).matches());
    }

    private static boolean isExclusion(String scopeElement) {
        return EntryGlobs.isExclusion(scopeElement);
    }

    private static Pattern glob(String glob) {
        return EntryGlobs.pattern(glob);
    }

    /** The fixtures of one entry. */
    private final class EntryFixtures {
        private final ManifestEntry entry;
        private final List<CompatibilityFixture> fixtures = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private final List<String> problems = new ArrayList<>();
        private Target target;
        private ApiClassFile owner;
        private ApiClassFile.Member member;

        EntryFixtures(ManifestEntry entry) {
            this.entry = entry;
        }

        void generate(List<CompatibilityFixture> out, List<Note> outNotes, List<String> outProblems) {
            boolean api = entry.kind() == EntryKind.PUBLIC_TYPE || entry.kind() == EntryKind.PUBLIC_MEMBER;
            if (api && resolveTarget()) {
                apiFixtures();
            } else if (!api) {
                for (CompatibilityDimension d : List.of(SOURCE, BINARY)) {
                    if (entry.dimensions().contains(d)) {
                        notes.add("the " + d.jsonName() + " dimension of " + entry.kind() + " entries is not "
                                  + "verified by generated fixtures (launch-surface gate)");
                    }
                }
            }
            behaviorFixtures(api);
            if (fixtures.isEmpty() && problems.isEmpty()) {
                problems.add("no compatibility fixture is applicable to this " + entry.classification() + " "
                             + entry.kind() + " entry" + (notes.isEmpty() ? "" : " (" + String.join("; ", notes) + ")")
                             + "; claim a verifiable dimension, name evidence, or remove it from the test scope");
            }
            if (!problems.isEmpty()) {
                problems.forEach(p -> outProblems.add("[" + entry.id() + "] " + p));
                return;
            }
            out.addAll(fixtures);
            notes.forEach(n -> outNotes.add(new Note(entry.id(), n)));
        }

        private boolean resolveTarget() {
            String spec = entry.id().substring("api:".length());
            int hash = spec.indexOf('#');
            String ownerName = hash < 0 ? spec : spec.substring(0, hash);
            Optional<ApiClassFile> found = baseline.find(ownerName);
            if (found.isEmpty()) {
                problems.add("the baseline class path lacks " + ownerName);
                return false;
            }
            owner = found.get();
            if (hash < 0) {
                target = new Target(TargetKind.TYPE, owner.name(), owner.typeKind(), owner.isFinal(), null, null,
                                    false, owner.isProtected());
                return true;
            }
            String rest = spec.substring(hash + 1);
            int paren = rest.indexOf('(');
            boolean field = paren < 0;
            String name = field ? rest.substring(0, rest.indexOf(':')) : rest.substring(0, paren);
            String descriptor = field ? rest.substring(rest.indexOf(':') + 1) : rest.substring(paren);
            member = (field ? owner.fields() : owner.methods()).stream()
                                                               .filter(m -> m.name().equals(name)
                                                                            && m.descriptor().equals(descriptor))
                                                               .findFirst().orElse(null);
            if (member == null) {
                problems.add("the baseline class " + ownerName + " declares no " + (field ? "field " : "method ")
                             + name + descriptor);
                return false;
            }
            TargetKind kind = field ? TargetKind.FIELD : name.equals("<init>") ? TargetKind.CONSTRUCTOR
                                                                               : TargetKind.METHOD;
            target = new Target(kind, owner.name(), owner.typeKind(), owner.isFinal(), name, descriptor,
                                member.isStatic(), member.isProtected());
            return true;
        }

        private void apiFixtures() {
            boolean preserved = entry.classification() == Classification.PRESERVED;
            Kind shape = target.kind() == TargetKind.TYPE ? Kind.HIERARCHY : Kind.SIGNATURE;
            if (entry.dimensions().contains(SOURCE)) {
                List<String> sourceSupertypes = target.kind() == TargetKind.TYPE ? requiredSupertypes(SOURCE)
                                                                                 : List.of();
                compileCaller(sourceSupertypes).ifPresent(src -> add(Kind.COMPILE, SOURCE, src, List.of(), null,
                                                                     sourceSupertypes));
                add(shape, SOURCE, null, List.of(), null, sourceSupertypes);
            } else if (!preserved) {
                compileCaller(List.of()).ifPresent(src -> add(Kind.COMPILE_REJECTED, null, src, symbols(),
                                                              diagnostic(), List.of()));
            }
            if (entry.dimensions().contains(BINARY)) {
                linkCaller().ifPresent(src -> add(Kind.LINK, BINARY, src, List.of(), null, List.of()));
                add(shape, BINARY, null, List.of(), null,
                    target.kind() == TargetKind.TYPE ? requiredSupertypes(BINARY) : List.of());
            } else if (!preserved) {
                linkCaller().ifPresent(src -> add(Kind.LINK_REJECTED, null, src, List.of(), diagnostic(), List.of()));
            }
        }

        private void behaviorFixtures(boolean api) {
            List<CompatibilityDimension> claimed = entry.dimensions().stream()
                                                        .filter(d -> d == BEHAVIOR || d == CONFIGURATION).toList();
            List<CompatibilityDimension> dimensions = new ArrayList<>(claimed);
            boolean byClassification = claimed.isEmpty()
                                       && (entry.classification() == Classification.ADAPTED
                                           || entry.classification() == Classification.EXCLUDED && !api
                                              && !entry.evidenceIds().isEmpty());
            if (byClassification) {
                dimensions.add(null);
            }
            for (CompatibilityDimension d : dimensions) {
                if (entry.evidenceIds().isEmpty()) {
                    problems.add(d == null
                                 ? "the " + entry.classification() + " differences name no evidence ID, so no "
                                   + "behavior fixture can verify them"
                                 : "the " + d.jsonName() + " claim names no evidence ID, so no behavior fixture can "
                                   + "verify it");
                } else {
                    add(Kind.BEHAVIOR, d, null, List.of(), null, List.of());
                }
            }
        }

        private void add(Kind kind, CompatibilityDimension dimension, String source, List<String> symbols,
                         String diagnostic, List<String> supertypes) {
            fixtures.add(new CompatibilityFixture(CompatibilityFixture.id(entry.id(), kind, dimension), entry.id(),
                                                  entry.classification(), kind, dimension, entry.v2Surface(), target,
                                                  source, symbols, diagnostic, supertypes,
                                                  kind == Kind.BEHAVIOR ? List.copyOf(entry.evidenceIds())
                                                                        : List.of()));
        }

        /** The diagnostic a rejected V2 caller is reported with, linked to the entry. */
        private String diagnostic() {
            return "[" + entry.id() + "] " + entry.classification() + " " + entry.v2Surface() + " -> "
                   + (entry.replacement() == null ? "no V3 replacement" : entry.replacement()) + ": "
                   + entry.migration();
        }

        /** Names from the entry's own surface: owner and descriptor type names and packages, and the member name. */
        private List<String> symbols() {
            Set<String> result = new TreeSet<>();
            addTypeSymbols(owner.name(), result);
            if (member != null) {
                if (!member.name().equals("<init>")) {
                    result.add(member.name());
                }
                List<String> types = new ArrayList<>();
                if (target.kind() == TargetKind.FIELD) {
                    types.add(member.descriptor());
                } else {
                    types.addAll(JvmTypes.parameters(member.descriptor()));
                    types.add(JvmTypes.returnType(member.descriptor()));
                }
                for (String t : types) {
                    String name = JvmTypes.className(t);
                    if (name != null && !JvmTypes.isJdk(name)) {
                        addTypeSymbols(name, result);
                    }
                }
            }
            return List.copyOf(result);
        }

        private void addTypeSymbols(String binaryName, Set<String> out) {
            out.addAll(JvmTypes.simpleNames(binaryName));
            String pkg = JvmTypes.packageName(binaryName);
            if (!pkg.isEmpty()) {
                out.add(pkg);
            }
        }

        // ---- compile callers ----

        private Optional<String> compileCaller(List<String> supertypes) {
            String ownerSource = JvmTypes.sourceName(owner.name());
            if (!publicChain(owner)) {
                return unsupported("compile caller", "the owner type or an enclosing type is not public");
            }
            Optional<String> unusable = unusableDescriptor();
            if (unusable.isPresent()) {
                return unsupported("compile caller", unusable.get());
            }
            if (target.kind() == TargetKind.TYPE) {
                StringBuilder body = new StringBuilder("        java.lang.Class<?> type = " + ownerSource + ".class;\n");
                for (int i = 0; i < supertypes.size(); i++) {
                    body.append("        ").append(JvmTypes.sourceName(supertypes.get(i))).append(" supertype")
                        .append(i).append(" = (").append(ownerSource).append(") null;\n");
                }
                return Optional.of(publicCaller("static void use() throws java.lang.Throwable", body.toString(), null));
            }
            boolean ctor = target.kind() == TargetKind.CONSTRUCTOR;
            boolean subclass = target.isProtected() || ctor && owner.isAbstract();
            if (ctor && owner.isInner()) {
                return unsupported("compile caller", "an inner-class constructor needs an enclosing instance");
            }
            if (!subclass) {
                return Optional.of(publicCaller("static void use() throws java.lang.Throwable",
                                                "        " + statement(ownerSource, "((" + ownerSource + ") null)")
                                                + "\n", null));
            }
            if (!owner.typeKind().equals("class") || owner.isFinal() || owner.isInner()) {
                return unsupported("compile caller", "protected or abstract-class members are used from a subclass, "
                                                     + "and " + owner.name() + " cannot be subclassed");
            }
            String superArguments;
            if (ctor) {
                superArguments = arguments(member.descriptor());
            } else {
                Optional<ApiClassFile.Member> accessible =
                    owner.methods().stream().filter(m -> m.name().equals("<init>") && m.isApi())
                         .min(Comparator.comparing(ApiClassFile.Member::descriptor));
                if (accessible.isEmpty()) {
                    return unsupported("compile caller", "protected members are used from a subclass, and "
                                                         + owner.name() + " has no public or protected constructor");
                }
                superArguments = arguments(accessible.get().descriptor());
            }
            StringBuilder source = new StringBuilder(SUPPRESS)
                .append("public abstract class ").append(CALLER_CLASS).append(" extends ").append(ownerSource)
                .append(" {\n")
                .append("    ").append(CALLER_CLASS).append("() throws java.lang.Throwable {\n")
                .append("        super(").append(superArguments).append(");\n")
                .append("    }\n");
            if (!ctor) {
                source.append("\n    void use() throws java.lang.Throwable {\n")
                      .append("        ").append(statement(ownerSource, "this")).append('\n')
                      .append("    }\n");
            }
            return Optional.of(source.append("}\n").toString());
        }

        /** The statement that uses a method, field or (public, concrete) constructor target. */
        private String statement(String ownerSource, String instance) {
            String name = target.name();
            String descriptor = target.descriptor();
            return switch (target.kind()) {
                case FIELD -> JvmTypes.sourceType(descriptor) + " value = "
                              + (target.isStatic() ? ownerSource : instance) + "." + name + ";";
                case CONSTRUCTOR -> ownerSource + " instance = new " + ownerSource + "(" + arguments(descriptor)
                                    + ");";
                case METHOD -> {
                    String call = (target.isStatic() ? ownerSource : instance) + "." + name + "("
                                  + arguments(descriptor) + ")";
                    String returns = JvmTypes.returnType(descriptor);
                    yield returns.equals("V") ? call + ";" : JvmTypes.sourceType(returns) + " result = " + call + ";";
                }
                case TYPE -> throw new IllegalStateException("types have no statement");
            };
        }

        private String arguments(String methodDescriptor) {
            return JvmTypes.parameters(methodDescriptor).stream().map(JvmTypes::argument)
                           .collect(Collectors.joining(", "));
        }

        // ---- link callers ----

        private Optional<String> linkCaller() {
            String ownerSource = JvmTypes.sourceName(owner.name());
            if (!publicChain(owner)) {
                return unsupported("link caller", "the owner type or an enclosing type is not public");
            }
            if (target.isProtected()) {
                return unsupported("link caller", "javac reaches a protected member through a synthetic lambda, so no "
                                                  + "method handle links it directly; the binary signature fixture "
                                                  + "verifies it");
            }
            Optional<String> unusable = unusableDescriptor();
            if (unusable.isPresent()) {
                return unsupported("link caller", unusable.get() + "; the binary signature fixture verifies it");
            }
            String descriptor = target.descriptor();
            return switch (target.kind()) {
                case TYPE -> Optional.of(publicCaller("public static void link() throws java.lang.Throwable",
                                                      "        java.lang.Class<?> type = " + ownerSource + ".class;\n",
                                                      null));
                case FIELD -> Optional.of(publicCaller(
                    "public static void link() throws java.lang.Throwable",
                    "        java.lang.Object value = " + (target.isStatic() ? ownerSource : "((" + ownerSource
                                                                                           + ") null)")
                    + "." + target.name() + ";\n", null));
                case CONSTRUCTOR -> {
                    if (owner.isAbstract() || !owner.typeKind().equals("class") || owner.isInner()) {
                        yield unsupported("link caller", "a constructor reference needs a concrete, non-inner class");
                    }
                    yield Optional.of(linkByReference(ownerSource + "::new", ownerSource,
                                                      JvmTypes.parameters(descriptor)));
                }
                case METHOD -> {
                    Optional<String> rival = ambiguousReferenceRival();
                    if (rival.isPresent()) {
                        yield unsupported("link caller", "the method reference " + ownerSource + "::"
                                                         + target.name() + " would be ambiguous with "
                                                         + rival.get() + "; the binary signature fixture "
                                                         + "verifies it");
                    }
                    List<String> parameters = new ArrayList<>();
                    if (!target.isStatic()) {
                        parameters.add("L" + owner.name().replace('.', '/') + ";");
                    }
                    parameters.addAll(JvmTypes.parameters(descriptor));
                    yield Optional.of(linkByReference(ownerSource + "::" + target.name(),
                                                      JvmTypes.sourceType(JvmTypes.returnType(descriptor)),
                                                      parameters));
                }
            };
        }

        /**
         * A public method javac could also bind the link caller's {@code Owner::name} reference to. The reference's
         * functional type takes the receiver first for an instance target, so it is ambiguous between an instance
         * method of n parameters and a static method of n + 1 (JLS 15.13.1): a same-named method of the other
         * static-ness and that arity, declared by the owner or inherited from a baseline supertype (static interface
         * methods are not inherited), is a rival whatever its parameter types.
         */
        private Optional<String> ambiguousReferenceRival() {
            int arity = JvmTypes.parameters(target.descriptor()).size();
            int rivalArity = target.isStatic() ? arity - 1 : arity + 1;
            Deque<String> work = new ArrayDeque<>(List.of(owner.name()));
            Set<String> seen = new TreeSet<>(work);
            while (!work.isEmpty()) {
                Optional<ApiClassFile> type = baseline.find(work.removeFirst());
                if (type.isEmpty()) {
                    continue;
                }
                boolean inherited = type.get() != owner;
                for (ApiClassFile.Member m : type.get().methods()) {
                    if (m.name().equals(target.name()) && m.isStatic() != target.isStatic()
                        && m.isApi() && !m.isProtected()
                        && !(inherited && m.isStatic() && type.get().typeKind().equals("interface"))
                        && JvmTypes.parameters(m.descriptor()).size() == rivalArity) {
                        return Optional.of(type.get().name() + "#" + m.name() + m.descriptor());
                    }
                }
                List<String> supers = new ArrayList<>(type.get().interfaces());
                if (type.get().superName() != null) {
                    supers.add(0, type.get().superName());
                }
                for (String s : supers) {
                    if (seen.add(s)) {
                        work.add(s);
                    }
                }
            }
            return Optional.empty();
        }

        private String linkByReference(String reference, String returns, List<String> parameterDescriptors) {
            List<String> parameters = new ArrayList<>();
            for (int i = 0; i < parameterDescriptors.size(); i++) {
                parameters.add(JvmTypes.sourceType(parameterDescriptors.get(i)) + " p" + i);
            }
            String target = "    interface Target {\n"
                            + "        " + returns + " apply(" + String.join(", ", parameters) + ");\n"
                            + "    }\n";
            return publicCaller("public static void link() throws java.lang.Throwable",
                                "        Target target = " + reference + ";\n", target);
        }

        private String publicCaller(String signature, String body, String members) {
            return SUPPRESS
                   + "public final class " + CALLER_CLASS + " {\n"
                   + "    private " + CALLER_CLASS + "() {\n"
                   + "    }\n\n"
                   + "    " + signature + " {\n"
                   + body
                   + "    }\n"
                   + (members == null ? "" : "\n" + members)
                   + "}\n";
        }

        /**
         * Why V2 source cannot name the member's exact descriptor, or empty: a descriptor type that is not accessible
         * outside its package (for example a constructor taking a private builder class), or a same-named generic
         * method of the owner whose erased parameters differ from the member's only by {@code Object} (a method type
         * variable) where the member has a reference type: next to {@code <T> m(X<T>, T)}, a call of
         * {@code m(X<?>, Key)} with typed {@code null} arguments infers {@code T = Key} and is ambiguous.
         */
        private Optional<String> unusableDescriptor() {
            if (member == null) {
                return Optional.empty();
            }
            List<String> types = new ArrayList<>();
            if (target.kind() == TargetKind.FIELD) {
                types.add(member.descriptor());
            } else {
                types.addAll(JvmTypes.parameters(member.descriptor()));
                types.add(JvmTypes.returnType(member.descriptor()));
            }
            for (String t : types) {
                String name = JvmTypes.className(t);
                if (name != null && !JvmTypes.isJdk(name)) {
                    Optional<ApiClassFile> type = baseline.find(name);
                    if (type.isPresent() && !publicChain(type.get())) {
                        return Optional.of("the descriptor type " + name + " is not accessible to V2 callers");
                    }
                }
            }
            if (target.kind() == TargetKind.FIELD) {
                return Optional.empty();
            }
            List<String> ours = JvmTypes.parameters(member.descriptor());
            for (ApiClassFile.Member m : owner.methods()) {
                if (!m.name().equals(member.name()) || m.descriptor().equals(member.descriptor()) || !m.isApi()
                    || m.isStatic() != member.isStatic() || !m.isGenericMethod()) {
                    continue;
                }
                List<String> theirs = JvmTypes.parameters(m.descriptor());
                if (theirs.size() != ours.size()) {
                    continue;
                }
                boolean widened = false;
                boolean rival = true;
                for (int i = 0; i < ours.size() && rival; i++) {
                    String mine = ours.get(i);
                    String other = theirs.get(i);
                    if (other.equals("Ljava/lang/Object;") && !mine.equals(other)
                        && (mine.startsWith("L") || mine.startsWith("["))) {
                        widened = true;
                    } else if (!other.equals(mine)) {
                        rival = false;
                    }
                }
                if (rival && widened) {
                    return Optional.of("a call with typed null arguments is ambiguous with the generic method "
                                       + owner.name() + "#" + m.name() + m.descriptor());
                }
            }
            return Optional.empty();
        }

        private Optional<String> unsupported(String what, String why) {
            notes.add("no " + what + ": " + why);
            return Optional.empty();
        }

        /** True if {@code type} and every enclosing type are public. */
        private boolean publicChain(ApiClassFile type) {
            for (ApiClassFile t = type; t != null; ) {
                if ((t.access() & ApiClassFile.ACC_PUBLIC) == 0) {
                    return false;
                }
                if (t.outerName() == null) {
                    return true;
                }
                t = baseline.find(t.outerName()).orElse(null);
            }
            return false;
        }

        // ---- hierarchy ----

        private List<String> requiredSupertypes(CompatibilityDimension dimension) {
            List<String> superclasses = new ArrayList<>();
            Set<String> all = new LinkedHashSet<>();
            Deque<String> work = new ArrayDeque<>();
            work.add(owner.name());
            Set<String> seen = new TreeSet<>(Set.of(owner.name()));
            while (!work.isEmpty()) {
                String name = work.removeFirst();
                Optional<Supers> supers = supersOf(name);
                if (supers.isEmpty()) {
                    problems.add("the baseline class path lacks " + name + ", a supertype of " + owner.name());
                    continue;
                }
                List<String> direct = new ArrayList<>(supers.get().interfaces());
                if (supers.get().superName() != null) {
                    direct.add(0, supers.get().superName());
                }
                for (String s : direct) {
                    if (seen.add(s)) {
                        all.add(s);
                        work.add(s);
                    }
                }
            }
            for (String s = owner.superName(); s != null; s = supersOf(s).map(Supers::superName).orElse(null)) {
                superclasses.add(s);
            }
            boolean throwable = superclasses.contains(THROWABLE);
            return all.stream()
                      .filter(s -> JvmTypes.isJdk(s)
                                   || throwable && superclasses.contains(s) && preserved(s, null)
                                   || dimension == BINARY && preserved(s, BINARY))
                      .sorted().toList();
        }

        /** True if the manifest classifies type {@code name} PRESERVED (claiming {@code dimension}, if given). */
        private boolean preserved(String name, CompatibilityDimension dimension) {
            ManifestEntry e = entries.get(EntryKind.typeId(name));
            return e != null && e.classification() == Classification.PRESERVED
                   && (dimension == null || e.dimensions().contains(dimension));
        }

        private Optional<Supers> supersOf(String name) {
            Optional<ApiClassFile> c = baseline.find(name);
            if (c.isPresent()) {
                return Optional.of(new Supers(c.get().superName(), c.get().interfaces()));
            }
            if (!JvmTypes.isJdk(name)) {
                return Optional.empty();
            }
            try {
                Class<?> type = Class.forName(name, false, ClassLoader.getPlatformClassLoader());
                Class<?> superclass = type.getSuperclass();
                String superName = superclass == null || superclass == Object.class ? null : superclass.getName();
                List<String> interfaces = Arrays.stream(type.getInterfaces()).map(Class::getName).toList();
                return Optional.of(new Supers(superName, interfaces));
            } catch (ClassNotFoundException e) {
                return Optional.empty();
            }
        }
    }

    private record Supers(String superName, List<String> interfaces) {
    }
}
