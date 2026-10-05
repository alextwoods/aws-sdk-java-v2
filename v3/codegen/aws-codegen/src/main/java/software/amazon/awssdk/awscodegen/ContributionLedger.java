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

package software.amazon.awssdk.awscodegen;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Records who contributed what to one generation and reports conflicting contributions before anything is published
 * (Requirement 3.7).
 *
 * <p>A <em>contributor</em> is an {@link AwsCodegenIntegration} (by {@link AwsCodegenIntegration#identifier()}) or
 * the core generator, {@value AwsCodegen#GENERATOR_IDENTIFIER}. {@link AttributedIntegration} runs every integration
 * hook, and {@link AwsDirectedCodegen} runs every shape directive, inside a {@linkplain #within scope} naming the
 * contributor and, where known, the shape being rendered, so every file write and writer checkout is attributed.
 * Generation is single-threaded, as {@code CodegenDirector} drives it.
 *
 * <p>Detected conflict kinds, each reported as one {@link Diagnostic} naming every involved contributor and the
 * conflicting shape:
 * <ul>
 *   <li>{@link DiagnosticCode#INTEGRATION_OUTPUT_CONFLICT}: more than one contributor produced the same generated file,
 *       through {@link AwsCodegenContext#fileManifest()} or {@link AwsCodegenContext#writerDelegator()}. Every
 *       generated file has exactly one owner; integrations customize files they do not own only through
 *       {@code CodeInterceptor} sections, which are composed rather than conflicting.</li>
 *   <li>{@link DiagnosticCode#INTEGRATION_SYMBOL_CONFLICT}: more than one integration's
 *       {@code decorateSymbolProvider} changes the Java namespace, name or definition file of the same shape, or the
 *       Java member name of the same member. Changes that keep the symbol identity (for example added symbol
 *       properties or dependencies) are additive and do not conflict.</li>
 *   <li>{@link DiagnosticCode#INTEGRATION_MODEL_CONFLICT}: more than one integration's {@code preprocessModel} changes
 *       the same trait of the same shape, or one changes the definition of a shape (adds, removes, retypes it, or
 *       changes its members, member targets, mixins or other non-trait properties) that another also changes.
 *       Changes to different traits of one shape compose; a change that leaves a value as an earlier integration set
 *       it is no change.</li>
 *   <li>{@link DiagnosticCode#DUPLICATE_GENERATED_FILE}: one contributor wrote the same file twice through the file
 *       manifest.</li>
 * </ul>
 * The shape of an output conflict is the shape being rendered when a claim was made (the lowest such shape ID), else
 * the shape whose symbol is defined in the file, else the generated service.
 */
final class ContributionLedger {

    /** One attributed contribution: who, and for which shape when known. */
    record Contribution(String contributor, ShapeId shape) {
        Contribution {
            Objects.requireNonNull(contributor, "contributor");
        }
    }

    static final String DEFINITION = "definition";

    private static final Contribution GENERATOR = new Contribution(AwsCodegen.GENERATOR_IDENTIFIER, null);

    private final String service;
    private final Deque<Contribution> scopes = new ArrayDeque<>();
    private int flushing;

    /** Absolute output path to its claims, in claim order. */
    private final Map<Path, Set<Contribution>> outputs = new TreeMap<>();
    /** Absolute output path to its path relative to its generated root, as reported. */
    private final Map<Path, String> displayPaths = new TreeMap<>();
    private final Set<Path> duplicateWrites = new TreeSet<>();
    private final List<SymbolLayer> symbolLayers = new ArrayList<>();
    /** Shape to changed aspect ({@value #DEFINITION} or {@code trait <id>}) to integrations, in execution order. */
    private final Map<ShapeId, Map<String, Set<String>>> modelChanges = new TreeMap<>();
    private final List<Diagnostic> settingConflicts = new ArrayList<>();

    private Model model;
    private SymbolProvider symbolProvider;

    private record SymbolLayer(String integration, SymbolProvider input, SymbolProvider output) {
    }

    /**
     * @param service absolute ID of the generated service, reported on every diagnostic
     */
    ContributionLedger(String service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Attribution scopes

    /** Runs {@code action} attributed to {@code contributor} rendering {@code shape} ({@code null} when unknown). */
    <T> T within(String contributor, ShapeId shape, Supplier<T> action) {
        scopes.push(new Contribution(contributor, shape));
        try {
            return action.get();
        } finally {
            scopes.pop();
        }
    }

    void within(String contributor, ShapeId shape, Runnable action) {
        within(contributor, shape, () -> {
            action.run();
            return null;
        });
    }

    /** Runs {@code action} attributed to the current contributor rendering {@code shape}. */
    void withinShape(ShapeId shape, Runnable action) {
        within(current().contributor(), shape, action);
    }

    /** The innermost scope; the core generator when no scope is active. */
    Contribution current() {
        Contribution top = scopes.peek();
        return top == null ? GENERATOR : top;
    }

    /**
     * Runs a writer-delegator flush: the files it writes were already claimed when their writers were checked out,
     * so the manifest writes of the flush claim nothing themselves.
     */
    void flushing(Runnable flush) {
        flushing++;
        try {
            flush.run();
        } finally {
            flushing--;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Recording

    /**
     * Records that the current contributor produces a generated file.
     *
     * @param file absolute, normalized path of the file
     * @param display path of the file relative to its generated root, {@code /}-separated
     */
    synchronized void claimOutput(Path file, String display) {
        if (flushing == 0) {
            displayPaths.putIfAbsent(file, display);
            outputs.computeIfAbsent(file, k -> new LinkedHashSet<>()).add(current());
        }
    }

    /** Records a second physical write of a file to a file manifest (the first content is kept). */
    synchronized void duplicateWrite(Path file, String display) {
        displayPaths.putIfAbsent(file, display);
        duplicateWrites.add(file);
    }

    /** Records that {@code integration} decorated {@code input} into {@code output}. */
    synchronized void symbolLayer(String integration, SymbolProvider input, SymbolProvider output) {
        if (output != input) {
            symbolLayers.add(new SymbolLayer(integration, input, output));
        }
    }

    /** Records every shape aspect that {@code integration}'s {@code preprocessModel} changed. */
    synchronized void modelChange(String integration, Model before, Model after) {
        if (before == after) {
            return;
        }
        changedAspects(before, after).forEach((shape, aspects) -> aspects.forEach(
            aspect -> modelChanges.computeIfAbsent(shape, s -> new TreeMap<>())
                                  .computeIfAbsent(aspect, a -> new LinkedHashSet<>())
                                  .add(integration)));
    }

    /**
     * Binds the final model and symbol provider, and checks the recorded symbol-provider decorations against every
     * shape in the service closure. Called once, when the codegen context is created.
     */
    synchronized void bind(Model finalModel, ServiceShape serviceShape, SymbolProvider finalSymbols) {
        this.model = finalModel;
        this.symbolProvider = finalSymbols;
        settingConflicts.addAll(symbolConflicts(finalModel, serviceShape));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Reporting

    /** Absolute ID of the generated service. */
    String service() {
        return service;
    }

    /**
     * Contributors that claimed the generated file at absolute path {@code file}, sorted; the core generator when no
     * contributor claimed it.
     */
    synchronized List<String> contributors(Path file) {
        List<String> result = outputs.getOrDefault(file, Set.of()).stream().map(Contribution::contributor).distinct()
                                     .sorted().toList();
        return result.isEmpty() ? List.of(AwsCodegen.GENERATOR_IDENTIFIER) : result;
    }

    /** The shape a diagnostic about the generated file at absolute path {@code file} is reported for. */
    synchronized ShapeId shapeOf(Path file, String display) {
        return shapeOf(display, outputs.getOrDefault(file, Set.of()));
    }

    /**
     * The integrations whose {@code decorateSymbolProvider} changed the Java symbol identity (namespace, name or
     * definition file) of {@code shape}, in execution order; empty if the symbol is the core generator's own.
     */
    synchronized List<String> symbolChangers(Shape shape) {
        List<String> result = new ArrayList<>();
        for (SymbolLayer layer : symbolLayers) {
            if (!identity(layer.input().toSymbol(shape)).equals(identity(layer.output().toSymbol(shape)))) {
                result.add(layer.integration());
            }
        }
        return result;
    }

    /** Every conflict recorded so far, sorted. */
    synchronized List<Diagnostic> conflicts() {
        List<Diagnostic> result = new ArrayList<>(settingConflicts);
        result.addAll(modelConflicts());
        result.addAll(outputConflicts());
        result.sort(null);
        return result;
    }

    /** Fails generation with every recorded conflict, if there is any. */
    void requireNoConflicts() {
        List<Diagnostic> conflicts = conflicts();
        if (!conflicts.isEmpty()) {
            throw new AwsCodegenException(
                "Code generation of service " + service + " has conflicting integration output or settings ("
                + conflicts.size() + " diagnostic(s)); no Generated Service Package artifact was produced:",
                conflicts);
        }
    }

    private List<Diagnostic> outputConflicts() {
        List<Diagnostic> result = new ArrayList<>();
        Set<Path> files = new TreeSet<>(outputs.keySet());
        files.addAll(duplicateWrites);
        for (Path file : files) {
            String path = displayPaths.get(file);
            Set<Contribution> claims = outputs.getOrDefault(file, Set.of());
            List<String> contributors = claims.stream().map(Contribution::contributor).distinct().sorted().toList();
            ShapeId shape = shapeOf(path, claims);
            if (contributors.size() > 1) {
                result.add(Diagnostic.forGeneration(
                    DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, service, shape.toString(), contributors,
                    "output " + path,
                    "Generated file " + path + " is produced by " + describe(claims) + "; every generated file has "
                    + "exactly one owner (other contributors customize it only through CodeInterceptor sections)."));
            } else if (duplicateWrites.contains(file)) {
                List<String> writer = contributors.isEmpty() ? List.of(AwsCodegen.GENERATOR_IDENTIFIER) : contributors;
                result.add(Diagnostic.forGeneration(
                    DiagnosticCode.DUPLICATE_GENERATED_FILE, service, shape.toString(), writer,
                    "output " + path,
                    "Generated file " + path + " was written more than once in one generation by " + writer.get(0)
                    + "; the first content was kept and nothing was published."));
            }
        }
        return result;
    }

    private List<Diagnostic> modelConflicts() {
        List<Diagnostic> result = new ArrayList<>();
        modelChanges.forEach((shape, aspects) -> {
            Set<String> definers = aspects.getOrDefault(DEFINITION, Set.of());
            aspects.forEach((aspect, changers) -> {
                Set<String> involved = new LinkedHashSet<>(changers);
                if (!aspect.equals(DEFINITION)) {
                    involved.addAll(definers);
                }
                if (involved.size() > 1) {
                    List<String> integrations = involved.stream().sorted().toList();
                    result.add(Diagnostic.forGeneration(
                        DiagnosticCode.INTEGRATION_MODEL_CONFLICT, service, shape.toString(), integrations,
                        aspect,
                        "Integrations " + integrations + " each change the " + aspect + " of shape " + shape
                        + " in preprocessModel; a later integration would silently override an earlier one."));
                }
            });
        });
        return result;
    }

    private List<Diagnostic> symbolConflicts(Model finalModel, ServiceShape serviceShape) {
        if (symbolLayers.size() < 2) {
            return List.of();
        }
        List<Diagnostic> result = new ArrayList<>();
        List<Shape> closure = new ArrayList<>(new Walker(finalModel).walkShapes(serviceShape));
        closure.sort(Comparator.comparing(Shape::getId));
        for (Shape shape : closure) {
            List<String> symbolChangers = new ArrayList<>();
            List<String> memberNameChangers = new ArrayList<>();
            List<String> symbols = new ArrayList<>();
            for (SymbolLayer layer : symbolLayers) {
                String before = identity(layer.input().toSymbol(shape));
                String after = identity(layer.output().toSymbol(shape));
                if (!before.equals(after)) {
                    symbolChangers.add(layer.integration());
                    symbols.add(layer.integration() + " -> " + after);
                }
                if (shape instanceof MemberShape member
                    && !layer.input().toMemberName(member).equals(layer.output().toMemberName(member))) {
                    memberNameChangers.add(layer.integration());
                }
            }
            if (symbolChangers.size() > 1) {
                result.add(Diagnostic.forGeneration(
                    DiagnosticCode.INTEGRATION_SYMBOL_CONFLICT, service, shape.getId().toString(),
                    symbolChangers.stream().sorted().toList(), "symbol",
                    "Integrations change the Java symbol of shape " + shape.getId() + " in decorateSymbolProvider: "
                    + String.join("; ", symbols) + "."));
            }
            if (memberNameChangers.size() > 1) {
                result.add(Diagnostic.forGeneration(
                    DiagnosticCode.INTEGRATION_SYMBOL_CONFLICT, service, shape.getId().toString(),
                    memberNameChangers.stream().sorted().toList(), "member-name",
                    "Integrations " + memberNameChangers + " each change the Java member name of " + shape.getId()
                    + " in decorateSymbolProvider."));
            }
        }
        return result;
    }

    private ShapeId shapeOf(String path, Set<Contribution> claims) {
        Optional<ShapeId> rendered = claims.stream().map(Contribution::shape).filter(Objects::nonNull).sorted()
                                           .findFirst();
        return rendered.or(() -> definingShape(path)).orElse(ShapeId.from(service));
    }

    /** The lowest shape ID in the service closure whose symbol is defined in {@code path}, if bound. */
    private Optional<ShapeId> definingShape(String path) {
        if (model == null || symbolProvider == null) {
            return Optional.empty();
        }
        return model.getShape(ShapeId.from(service)).map(s -> new Walker(model).walkShapes(s)).orElse(Set.of()).stream()
                    .filter(shape -> !(shape instanceof MemberShape))
                    .filter(shape -> definedIn(shape, path))
                    .map(Shape::getId)
                    .sorted()
                    .findFirst();
    }

    private boolean definedIn(Shape shape, String path) {
        try {
            String file = symbolProvider.toSymbol(shape).getDefinitionFile();
            return !file.isEmpty() && path.equals(StagedFileManifest.portable(Path.of(file).normalize()));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String describe(Set<Contribution> claims) {
        Map<String, Set<String>> byContributor = new TreeMap<>();
        claims.forEach(c -> byContributor.computeIfAbsent(c.contributor(), k -> new TreeSet<>())
                                         .addAll(c.shape() == null ? Set.of() : Set.of(c.shape().toString())));
        return byContributor.entrySet().stream()
                            .map(e -> e.getValue().isEmpty() ? e.getKey()
                                                             : e.getKey() + " (rendering " + String.join(", ", e.getValue())
                                                               + ")")
                            .collect(Collectors.joining(" and "));
    }

    private static String identity(Symbol symbol) {
        return symbol.getFullName() + " in '" + symbol.getDefinitionFile() + "'";
    }

    /**
     * The aspects of every shape that differ between {@code before} and {@code after}: {@value #DEFINITION} when the
     * shape was added, removed or changed apart from its own traits, and {@code trait <id>} for every trait whose
     * value was added, removed or changed. Member shapes are compared individually, so a trait change on a member is
     * not also a definition change of its container.
     */
    static Map<ShapeId, Set<String>> changedAspects(Model before, Model after) {
        Set<ShapeId> ids = new TreeSet<>();
        before.toSet().forEach(shape -> ids.add(shape.getId()));
        after.toSet().forEach(shape -> ids.add(shape.getId()));
        Map<ShapeId, Set<String>> result = new TreeMap<>();
        for (ShapeId id : ids) {
            Shape b = before.getShape(id).orElse(null);
            Shape a = after.getShape(id).orElse(null);
            if (Objects.equals(b, a)) {
                continue;
            }
            Set<String> aspects = new TreeSet<>();
            if (b == null || a == null || definitionChanged(b, a)) {
                aspects.add(DEFINITION);
            }
            if (b != null && a != null) {
                Set<ShapeId> traits = new TreeSet<>(b.getAllTraits().keySet());
                traits.addAll(a.getAllTraits().keySet());
                for (ShapeId trait : traits) {
                    Trait bt = b.getAllTraits().get(trait);
                    Trait at = a.getAllTraits().get(trait);
                    if (!Objects.equals(bt, at)) {
                        aspects.add("trait " + trait);
                    }
                }
            }
            if (!aspects.isEmpty()) {
                result.put(id, aspects);
            }
        }
        return result;
    }

    private static boolean definitionChanged(Shape before, Shape after) {
        if (before.getType() != after.getType() || !before.getMixins().equals(after.getMixins())) {
            return true;
        }
        if (before instanceof MemberShape b && after instanceof MemberShape a) {
            return !b.getTarget().equals(a.getTarget());
        }
        if (!before.members().isEmpty() || !after.members().isEmpty()) {
            return !memberTargets(before).equals(memberTargets(after));
        }
        try {
            return !withoutTraits(before).equals(withoutTraits(after));
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static List<String> memberTargets(Shape shape) {
        return shape.members().stream().map(m -> m.getMemberName() + "=" + m.getTarget()).toList();
    }

    private static Shape withoutTraits(Shape shape) {
        return Shape.shapeToBuilder(shape).clearTraits().build();
    }
}
