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

package software.amazon.awssdk.test.codegen.support;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.Member;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.ModelSpec;
import software.amazon.awssdk.test.codegen.support.RandomSmithyModels.Shape;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Enriches a {@link RandomSmithyModels} model with the presence, default, constraint, recursion and member-order
 * features that schema fidelity (Property 1) must preserve, and that the base generator produces rarely or never.
 *
 * <p>{@link #enrich} returns a new, still valid {@link ModelSpec}; it never changes the base generator, so the models
 * (and renderings) other properties draw from {@link RandomSmithyModels} are unaffected. It adds:
 * <ul>
 *   <li><b>recursion</b>: up to three clusters of new aggregates forming a cycle of length one to four through any mix
 *       of structures, unions, lists and maps (at least one structure or union per cycle), with extra back edges,
 *       reachable from an operation input or output;</li>
 *   <li><b>requiredness</b>: {@code @required} on structure members whose target cannot reach the owning structure
 *       (so no required cycle is created), including a required member after optional ones, which Smithy Java
 *       reorders first;</li>
 *   <li><b>defaults</b>: {@code @default} values valid for the target (strings, numbers, booleans, blobs, enum and
 *       intEnum values, empty lists and maps) on unconstrained targets, with or without {@code @required};</li>
 *   <li><b>constraints and other runtime traits</b>: member {@code @length}, {@code @range} and {@code @pattern};
 *       shape {@code @uniqueItems}, {@code @sparse}, map {@code @length}, {@code @httpError}, {@code @timestampFormat}
 *       and {@code @mediaType}; member {@code @jsonName}, {@code @xmlName} and {@code @idempotencyToken};</li>
 *   <li><b>design-time traits</b>: {@code @tags} on structures, which schemas must not carry.</li>
 * </ul>
 *
 * <p>The output is a function of the base model and the {@link Random} passed in.
 */
public final class SchemaFidelityModels {

    private static final String DEFAULT = "smithy.api#default";
    private static final String REQUIRED = "smithy.api#required";
    private static final String LENGTH = "smithy.api#length";
    private static final String RANGE = "smithy.api#range";
    private static final String PATTERN = "smithy.api#pattern";
    private static final Set<String> CONSTRAINTS = Set.of(LENGTH, RANGE, PATTERN);
    private static final List<String> REC_NOUNS = List.of("Node", "Tree", "Link", "Chain", "Graph", "Nest");

    private SchemaFidelityModels() {
    }

    /** {@code base} with the features listed in the class documentation added at random. */
    public static ModelSpec enrich(ModelSpec base, Random random) {
        return enrich(base, random, false);
    }

    /**
     * {@link #enrich(ModelSpec, Random)}, and if {@code reachEveryShape}, first makes every data shape of {@code base}
     * part of the service closure through new optional members of new hub structures ({@code RecHub<n>}) referenced
     * from an operation input or output, so that large models produce large closures (for example, partitioned schema
     * holders).
     */
    public static ModelSpec enrich(ModelSpec base, Random random, boolean reachEveryShape) {
        Enricher enricher = new Enricher(base, random);
        if (reachEveryShape) {
            enricher.reachEveryShape();
        }
        return enricher.enrich();
    }

    /** A mutable copy of a {@link Shape}. */
    private static final class Draft {
        final ShapeId id;
        final String type;
        final List<Member> members;
        final Map<String, Node> traits;
        final Shape original;
        String input;
        String output;

        Draft(Shape shape) {
            this.id = shape.id();
            this.type = shape.type();
            this.members = new ArrayList<>(shape.members());
            this.traits = new LinkedHashMap<>(shape.traits());
            this.original = shape;
            this.input = shape.input();
            this.output = shape.output();
        }

        Draft(ShapeId id, String type) {
            this(new Shape(id, type, List.of(), Map.of(), null, null, null, List.of(), List.of(), List.of(),
                           Map.of()));
        }

        Shape freeze() {
            return new Shape(id, type, members, traits, original.version(), input, output, original.errors(),
                             original.operations(), original.resources(), original.identifiers());
        }
    }

    private static final class Enricher {
        private final ModelSpec base;
        private final Random random;
        private final Map<ShapeId, Draft> drafts = new LinkedHashMap<>();
        private final Set<String> names = new TreeSet<>();

        Enricher(ModelSpec base, Random random) {
            this.base = base;
            this.random = random;
            base.shapes().forEach(s -> drafts.put(s.id(), new Draft(s)));
            names.addAll(base.lowerCaseNames());
        }

        ModelSpec enrich() {
            if (random.nextInt(3) > 0) {
                int clusters = 1 + random.nextInt(3);
                for (int i = 0; i < clusters; i++) {
                    recursiveCluster();
                }
            }
            drafts.values().forEach(this::shapeTraits);
            for (Draft draft : List.copyOf(drafts.values())) {
                if (draft.type.equals("structure")) {
                    memberTraits(draft);
                }
            }
            return new ModelSpec(base.service(), base.packageName(),
                                 drafts.values().stream().map(Draft::freeze).toList());
        }

        /** Hub structures of up to 12 members each, targeting every data shape, each reachable from an operation. */
        void reachEveryShape() {
            List<Draft> data = drafts.values().stream()
                                     .filter(d -> !Set.of("service", "operation", "resource").contains(d.type))
                                     .filter(d -> !d.traits.containsKey("smithy.api#error")
                                                  && !d.traits.containsKey("smithy.api#input")
                                                  && !d.traits.containsKey("smithy.api#output"))
                                     .toList();
            for (int start = 0; start < data.size(); start += 12) {
                Draft hub = new Draft(ShapeId.fromParts(base.service().getNamespace(), fresh("RecHub")), "structure");
                for (Draft target : data.subList(start, Math.min(data.size(), start + 12))) {
                    hub.members.add(new Member(freshMember(hub, "m"), target.id.toString(), Map.of()));
                }
                drafts.put(hub.id, hub);
                Draft io = reachableStructure();
                io.members.add(new Member(freshMember(io, "hub"), hub.id.toString(), Map.of()));
            }
        }

        // -------------------------------------------------------------------------------------------------------------
        // Recursion

        /** A cycle of 1-4 new aggregates, at least one a structure or union, reachable from an operation. */
        private void recursiveCluster() {
            String namespace = base.service().getNamespace();
            int length = 1 + random.nextInt(4);
            List<Draft> cycle = new ArrayList<>();
            List<String> kinds = List.of("structure", "structure", "union", "list", "map");
            for (int i = 0; i < length; i++) {
                String kind = kinds.get(random.nextInt(kinds.size()));
                if (i == length - 1 && cycle.stream().noneMatch(Enricher::aggregate) && !aggregate(kind)) {
                    kind = random.nextBoolean() ? "structure" : "union";
                }
                String suffix = switch (kind) {
                    case "list" -> "List";
                    case "map" -> "Map";
                    case "union" -> "Choice";
                    default -> "";
                };
                Draft draft = new Draft(ShapeId.fromParts(namespace, fresh("Rec" + pick(REC_NOUNS) + suffix)), kind);
                drafts.put(draft.id, draft);
                cycle.add(draft);
            }
            for (int i = 0; i < length; i++) {
                Draft shape = cycle.get(i);
                String next = cycle.get((i + 1) % length).id.toString();
                switch (shape.type) {
                    case "list" -> shape.members.add(new Member("member", next, Map.of()));
                    case "map" -> {
                        shape.members.add(new Member("key", "smithy.api#String", Map.of()));
                        shape.members.add(new Member("value", next, Map.of()));
                    }
                    default -> {
                        List<Member> members = new ArrayList<>();
                        members.add(new Member("next", next, Map.of()));
                        // A union needs a non-recursive member, or no instance of it can exist (ShapeRecursion).
                        if (shape.type.equals("union") || random.nextBoolean()) {
                            members.add(new Member("leaf", pick(List.of("smithy.api#String", "smithy.api#Integer",
                                                                        "smithy.api#Boolean")), Map.of()));
                        }
                        if (random.nextInt(3) == 0) {
                            members.add(new Member("back", cycle.get(random.nextInt(i + 1)).id.toString(), Map.of()));
                        }
                        if (random.nextInt(4) == 0) {
                            members.add(new Member("self", shape.id.toString(), Map.of()));
                        }
                        java.util.Collections.shuffle(members, random);
                        shape.members.addAll(members);
                    }
                }
            }
            Draft io = reachableStructure();
            String memberName = freshMember(io, "recursive");
            io.members.add(random.nextInt(io.members.size() + 1),
                           new Member(memberName, cycle.get(random.nextInt(length)).id.toString(), Map.of()));
        }

        private static boolean aggregate(Draft draft) {
            return aggregate(draft.type);
        }

        private static boolean aggregate(String kind) {
            return kind.equals("structure") || kind.equals("union");
        }

        /** The input or output of a random operation, created if the operation has neither. */
        private Draft reachableStructure() {
            List<Draft> operations = drafts.values().stream().filter(d -> d.type.equals("operation")).toList();
            if (operations.isEmpty()) {
                throw new IllegalArgumentException("The base model has no operation: " + base);
            }
            Draft operation = pick(operations);
            List<String> io = new ArrayList<>();
            if (operation.input != null) {
                io.add(operation.input);
            }
            if (operation.output != null) {
                io.add(operation.output);
            }
            if (!io.isEmpty()) {
                return drafts.get(ShapeId.from(pick(io)));
            }
            String name = operation.id.getName() + "Input";
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("Dedicated input name " + name + " is taken in " + base);
            }
            Draft input = new Draft(ShapeId.fromParts(operation.id.getNamespace(), name), "structure");
            input.traits.put("smithy.api#input", Node.objectNode());
            drafts.put(input.id, input);
            operation.input = input.id.toString();
            return input;
        }

        // -------------------------------------------------------------------------------------------------------------
        // Shape traits

        private void shapeTraits(Draft shape) {
            switch (shape.type) {
                case "list" -> {
                    if (uniqueItemsTarget(shape.members.get(0).target()) && random.nextInt(5) == 0) {
                        shape.traits.put("smithy.api#uniqueItems", Node.objectNode());
                    } else if (random.nextInt(6) == 0) {
                        shape.traits.put("smithy.api#sparse", Node.objectNode());
                    }
                }
                case "map" -> {
                    maybe(6, () -> shape.traits.put("smithy.api#sparse", Node.objectNode()));
                    if (!shape.traits.containsKey(LENGTH)) {
                        maybe(5, () -> shape.traits.put(LENGTH, range("min", "max")));
                    }
                }
                case "structure" -> {
                    Node error = shape.traits.get("smithy.api#error");
                    if (error != null && random.nextBoolean()) {
                        boolean client = error.expectStringNode().getValue().equals("client");
                        shape.traits.put("smithy.api#httpError", Node.from((client ? 400 : 500) + random.nextInt(100)));
                    }
                    maybe(8, () -> shape.traits.put("smithy.api#tags", ArrayNode.fromStrings("fidelity", "test")));
                }
                case "timestamp" -> maybe(3, () -> shape.traits.put(
                    "smithy.api#timestampFormat", Node.from(pick(List.of("epoch-seconds", "date-time", "http-date")))));
                case "blob" -> maybe(4, () -> shape.traits.put("smithy.api#mediaType",
                                                               Node.from("application/octet-stream")));
                case "string" -> maybe(5, () -> shape.traits.put("smithy.api#mediaType", Node.from("text/plain")));
                default -> {
                }
            }
        }

        private boolean uniqueItemsTarget(String target) {
            if (Set.of("smithy.api#String", "smithy.api#Integer", "smithy.api#Long", "smithy.api#Short",
                       "smithy.api#Boolean").contains(target)) {
                return true;
            }
            Draft draft = drafts.get(ShapeId.from(target));
            return draft != null && Set.of("string", "enum", "intEnum", "integer", "long").contains(draft.type);
        }

        // -------------------------------------------------------------------------------------------------------------
        // Member traits

        private void memberTraits(Draft structure) {
            boolean token = false;
            for (int i = 0; i < structure.members.size(); i++) {
                Member member = structure.members.get(i);
                if (member.traits().containsKey(REQUIRED)) {
                    // Resource identifier bindings stay as the base generator made them.
                    continue;
                }
                Map<String, Node> traits = new LinkedHashMap<>(member.traits());
                boolean canRequire = !reaches(ShapeId.from(member.target()), structure.id);
                if (canRequire && random.nextInt(3) == 0) {
                    traits.put(REQUIRED, Node.objectNode());
                }
                Node defaultValue = random.nextInt(4) == 0 ? defaultValue(member.target()) : null;
                if (defaultValue != null) {
                    traits.put(DEFAULT, defaultValue);
                } else if (random.nextInt(5) == 0) {
                    constraint(member.target(), traits);
                }
                if (random.nextInt(8) == 0) {
                    traits.put("smithy.api#jsonName", Node.from(member.name() + "_json"));
                }
                if (random.nextInt(10) == 0) {
                    traits.put("smithy.api#xmlName", Node.from("x" + member.name()));
                }
                if (!token && defaultValue == null && stringTarget(member.target()) && random.nextInt(10) == 0) {
                    traits.put("smithy.api#idempotencyToken", Node.objectNode());
                    token = true;
                }
                structure.members.set(i, new Member(member.name(), member.target(), traits));
            }
            // Member-order edge case: a required member after optional ones, which Smithy Java orders first.
            int last = structure.members.size() - 1;
            if (last >= 1 && random.nextBoolean()) {
                Member member = structure.members.get(last);
                boolean optionalBefore = structure.members.subList(0, last).stream()
                                                          .anyMatch(m -> !m.traits().containsKey(REQUIRED));
                if (optionalBefore && !member.traits().containsKey(REQUIRED)
                    && !reaches(ShapeId.from(member.target()), structure.id)) {
                    Map<String, Node> traits = new LinkedHashMap<>(member.traits());
                    traits.put(REQUIRED, Node.objectNode());
                    structure.members.set(last, new Member(member.name(), member.target(), traits));
                }
            }
        }

        /** Whether {@code owner} is reachable from {@code start} through member targets (including start itself). */
        private boolean reaches(ShapeId start, ShapeId owner) {
            Deque<ShapeId> pending = new ArrayDeque<>(List.of(start));
            Set<ShapeId> seen = new HashSet<>();
            while (!pending.isEmpty()) {
                ShapeId id = pending.pop();
                if (id.equals(owner)) {
                    return true;
                }
                Draft draft = drafts.get(id);
                if (draft != null && seen.add(id)) {
                    draft.members.forEach(m -> pending.push(ShapeId.from(m.target())));
                }
            }
            return false;
        }

        /** A valid default for a member targeting {@code target}, or {@code null} if it has none here. */
        private Node defaultValue(String target) {
            switch (target) {
                case "smithy.api#String" -> {
                    return Node.from(pick(List.of("", "dflt", "with space")));
                }
                case "smithy.api#Integer", "smithy.api#Long", "smithy.api#Short" -> {
                    return Node.from(random.nextInt(101));
                }
                case "smithy.api#Boolean" -> {
                    return Node.from(random.nextBoolean());
                }
                case "smithy.api#Double", "smithy.api#BigDecimal" -> {
                    return Node.from(pick(List.of(0, 1.5, 2.25)));
                }
                case "smithy.api#Blob" -> {
                    return Node.from("aGk=");
                }
                default -> {
                }
            }
            Draft draft = drafts.get(ShapeId.from(target));
            if (draft == null || draft.traits.keySet().stream().anyMatch(CONSTRAINTS::contains)) {
                return null;
            }
            return switch (draft.type) {
                case "string" -> Node.from("dflt");
                case "integer", "long" -> Node.from(random.nextInt(101));
                case "boolean" -> Node.from(random.nextBoolean());
                case "bigDecimal" -> Node.from(1.5);
                case "blob" -> Node.from("");
                case "enum", "intEnum" -> draft.members.get(random.nextInt(draft.members.size())).traits()
                                                       .get("smithy.api#enumValue");
                case "list" -> Node.arrayNode();
                case "map" -> Node.objectNode();
                default -> null;
            };
        }

        private void constraint(String target, Map<String, Node> traits) {
            String type = targetType(target);
            switch (type) {
                case "string" -> {
                    traits.put(LENGTH, range("min", "max"));
                    maybe(3, () -> traits.put(PATTERN, Node.from("^[A-Za-z0-9 ]*$")));
                }
                case "blob", "list", "map" -> traits.put(LENGTH, range("min", "max"));
                case "integer", "long", "short", "double", "bigDecimal" -> traits.put(RANGE, range("min", "max"));
                default -> {
                }
            }
        }

        private boolean stringTarget(String target) {
            return targetType(target).equals("string");
        }

        /** The Smithy type name of {@code target}. */
        private String targetType(String target) {
            Draft draft = drafts.get(ShapeId.from(target));
            if (draft != null) {
                return draft.type;
            }
            String name = ShapeId.from(target).getName();
            return name.equals("BigDecimal") ? "bigDecimal" : name.toLowerCase(Locale.ROOT);
        }

        // -------------------------------------------------------------------------------------------------------------

        private Node range(String min, String max) {
            int low = random.nextInt(5);
            return Node.objectNode().withMember(min, low).withMember(max, low + 1 + random.nextInt(50));
        }

        private String fresh(String base) {
            for (int i = 0; ; i++) {
                String candidate = base + i;
                if (names.add(candidate.toLowerCase(Locale.ROOT))) {
                    return candidate;
                }
            }
        }

        private static String freshMember(Draft shape, String base) {
            Set<String> used = new HashSet<>();
            shape.members.forEach(m -> used.add(m.name().toLowerCase(Locale.ROOT)));
            for (int i = 0; ; i++) {
                String candidate = base + i;
                if (!used.contains(candidate.toLowerCase(Locale.ROOT))) {
                    return candidate;
                }
            }
        }

        private void maybe(int oneIn, Runnable action) {
            if (random.nextInt(oneIn) == 0) {
                action.run();
            }
        }

        private <T> T pick(List<T> items) {
            return items.get(random.nextInt(items.size()));
        }
    }
}
