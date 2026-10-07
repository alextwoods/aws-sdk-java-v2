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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Finds generated Java types that would collide, before any file is generated
 * ({@link DiagnosticCode#GENERATED_NAME_COLLISION}).
 *
 * <p>Every generated type is rendered into its symbol's definition file, so two types that share a Java name or a
 * definition file would be written into one source file that does not compile. The {@link ContributionLedger} cannot
 * see this: one contributor (the core generator) renders both. Names derived by the V2 rules collide for example for
 * the errors {@code ClusterFault} and {@code ClusterException} (both {@code ClusterException}), for an error
 * {@code Foo} and a structure {@code FooException}, for a structure {@code GetFooRequest} and the input of operation
 * {@code GetFoo}, or for a structure named like the service's base exception; an integration's
 * {@code decorateSymbolProvider} can also rename a type onto another. The check therefore runs on the final symbol
 * provider, when the codegen context is created.
 *
 * <p>The checked types are those the generator renders: the model type of every structure (including errors,
 * operation inputs and outputs), union, enum and intEnum of the service closure, every operation's descriptor and the
 * service's identity type, each at its symbol's definition file; and the files the service directive emits for the
 * service as a whole (its base exception, schema holder, operation table, type registry and service metadata). Java
 * names and definition files are compared case-insensitively, since generated sources may be written to a
 * case-insensitive file system.
 *
 * <p>Each set of colliding types is reported as one diagnostic for its lowest shape ID, naming every shape involved
 * and the Java type. Its contributors are the integrations whose symbol decorations changed one of the colliding
 * symbols, plus {@value AwsCodegen#GENERATOR_IDENTIFIER} when a colliding symbol is the core generator's own.
 */
final class GeneratedNameCollisions {

    /** Shape types whose symbol the generator renders into the symbol's definition file. */
    private static final Set<ShapeType> RENDERED = Set.of(ShapeType.STRUCTURE, ShapeType.UNION, ShapeType.ENUM,
                                                          ShapeType.INT_ENUM, ShapeType.OPERATION,
                                                          ShapeType.SERVICE);

    /**
     * One generated type.
     *
     * @param shape the shape it is generated for (the service for service-wide files)
     * @param description what it is, for messages
     * @param javaType its fully qualified Java name
     * @param file its definition file, {@code /}-separated
     * @param changers integrations whose symbol decorations changed it; empty for the core generator's own
     */
    private record Entry(ShapeId shape, String description, String javaType, String file, List<String> changers) {
    }

    private GeneratedNameCollisions() {
    }

    /**
     * Every collision among the generated types of {@code service}, sorted; empty if there is none.
     *
     * @param symbols the final symbol provider (after every integration's decoration)
     * @param changers the integrations whose symbol decoration changed the symbol of a shape, in execution order
     */
    static List<Diagnostic> find(Model model, ServiceShape service, SymbolProvider symbols,
                                 AwsCodegenSettings settings, Function<Shape, List<String>> changers) {
        List<Entry> entries = entries(model, service, symbols, settings, changers);

        // Union-find over the entries, joined by case-insensitive Java name and by case-insensitive file.
        int[] parent = new int[entries.size()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
        Map<String, Integer> byName = new HashMap<>();
        Map<String, Integer> byFile = new HashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int index = i;
            union(parent, i, byName.computeIfAbsent(entry.javaType().toLowerCase(Locale.ROOT), k -> index));
            union(parent, i, byFile.computeIfAbsent(entry.file().toLowerCase(Locale.ROOT), k -> index));
        }
        Map<Integer, List<Entry>> groups = new TreeMap<>();
        for (int i = 0; i < entries.size(); i++) {
            groups.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(entries.get(i));
        }

        String serviceId = service.getId().toString();
        List<Diagnostic> result = new ArrayList<>();
        for (List<Entry> group : groups.values()) {
            if (group.size() > 1) {
                result.add(diagnostic(serviceId, group));
            }
        }
        result.sort(null);
        return result;
    }

    /**
     * The simple names of every type this check covers (the generated types of {@code service}), by Java package.
     * Generated files use it to write a {@code java.lang} type that a same-package generated type shadows fully
     * qualified (see {@link JavaImports}).
     */
    static Map<String, Set<String>> typeNamesByPackage(Model model, ServiceShape service, SymbolProvider symbols,
                                                       AwsCodegenSettings settings) {
        Map<String, Set<String>> result = new TreeMap<>();
        for (Entry entry : entries(model, service, symbols, settings, shape -> List.of())) {
            int dot = entry.javaType().lastIndexOf('.');
            String packageName = dot < 0 ? "" : entry.javaType().substring(0, dot);
            result.computeIfAbsent(packageName, p -> new TreeSet<>()).add(entry.javaType().substring(dot + 1));
        }
        return result;
    }

    private static List<Entry> entries(Model model, ServiceShape service, SymbolProvider symbols,
                                       AwsCodegenSettings settings, Function<Shape, List<String>> changers) {
        List<Entry> entries = new ArrayList<>();
        List<Shape> closure = new ArrayList<>(new Walker(model).walkShapes(service));
        closure.sort(Comparator.comparing(Shape::getId));
        for (Shape shape : closure) {
            if (!RENDERED.contains(shape.getType())) {
                continue;
            }
            Symbol symbol = symbols.toSymbol(shape);
            if (!symbol.getDefinitionFile().isEmpty()) {
                entries.add(new Entry(shape.getId(), "shape " + shape.getId(), symbol.getFullName(),
                                      file(symbol), List.copyOf(changers.apply(shape))));
            }
        }
        ShapeId id = service.getId();
        entries.add(fixed(id, "the base exception of service " + id,
                          AwsSymbolProvider.serviceException(service, settings)));
        entries.add(fixed(id, "the base response of service " + id,
                          AwsSymbolProvider.serviceResponse(service, settings)));
        entries.add(fixed(id, "the response metadata of service " + id,
                          AwsSymbolProvider.serviceResponseMetadata(service, settings)));
        entries.add(fixed(id, "the schema holder of service " + id, AwsSymbolProvider.schemaHolder(service, settings)));
        entries.add(fixed(id, "the operation table of service " + id,
                          AwsSymbolProvider.operationTable(service, settings)));
        entries.add(fixed(id, "the type registry of service " + id, AwsSymbolProvider.typeRegistry(service, settings)));
        entries.add(fixed(id, "the service metadata of service " + id,
                          AwsSymbolProvider.serviceMetadata(service, settings)));
        return entries;
    }

    private static Entry fixed(ShapeId service, String description, Symbol symbol) {
        return new Entry(service, description, symbol.getFullName(), file(symbol), List.of());
    }

    private static String file(Symbol symbol) {
        return StagedFileManifest.portable(Path.of(symbol.getDefinitionFile()).normalize());
    }

    private static Diagnostic diagnostic(String service, List<Entry> group) {
        List<Entry> sorted = new ArrayList<>(group);
        sorted.sort(Comparator.comparing(Entry::shape).thenComparing(Entry::javaType).thenComparing(Entry::file));
        Entry first = sorted.get(0);
        Set<String> contributors = new TreeSet<>();
        for (Entry entry : sorted) {
            if (entry.changers().isEmpty()) {
                contributors.add(AwsCodegen.GENERATOR_IDENTIFIER);
            } else {
                contributors.addAll(entry.changers());
            }
        }
        Set<String> shapes = sorted.stream().map(e -> e.shape().toString())
                                   .collect(Collectors.toCollection(TreeSet::new));
        String types = sorted.stream()
                             .map(e -> e.description() + " -> " + e.javaType() + " (" + e.file() + ")")
                             .collect(Collectors.joining(", "));
        return Diagnostic.forGeneration(
            DiagnosticCode.GENERATED_NAME_COLLISION, service, first.shape().toString(), List.copyOf(contributors),
            "type " + first.javaType(),
            (shapes.size() == 1 ? "Shape " : "Shapes ") + String.join(" and ", shapes)
            + " would be generated as the same Java type "
            + first.javaType() + " or into the same source file (compared case-insensitively, since file systems "
            + "may be): " + types + ". Rename a shape (for example with the service's rename map) so that every "
            + "generated type has a distinct name; nothing was generated or published.");
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) {
            parent[Math.max(ra, rb)] = Math.min(ra, rb);
        }
    }
}
