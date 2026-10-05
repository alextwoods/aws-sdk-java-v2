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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Random valid AWS-style Smithy models and random, semantically equivalent renderings of them as model files.
 *
 * <p>{@link #model} builds a {@link ModelSpec}: one service (an AWS JSON protocol and SigV4, bearer or anonymous
 * auth) with operations bound to the service or to (possibly nested) resources, operation and service errors,
 * explicit {@code @input}/{@code @output} structures, and a pool of data shapes (structures, unions, enums, int enums,
 * lists, maps and simple shapes, possibly recursive through structures and unions, some unreferenced) in one or two
 * namespaces. Shapes and members carry documentation, deprecation, sensitivity, constraint and timestamp-format
 * traits, and unknown traits (absolute IDs without a definition) with arbitrary node values.
 *
 * <p>Shape names are unique case-insensitively, and so are the V2 Java names {@code aws-codegen} derives from them:
 * no two errors share a V2 exception name ({@link #v2ExceptionName}; for example {@code WidgetFault} and
 * {@code WidgetException}), data shapes never take the {@code Exception}, {@code Request} or {@code Response} names of
 * errors and operation inputs and outputs. Models with such collisions are rejected by design (a
 * {@code GENERATED_NAME_COLLISION} diagnostic), so tests that need one construct it explicitly.
 *
 * <p>{@link #render} turns a spec into model files: a random number of files, each Smithy IDL or Smithy JSON AST, at
 * random (lower-case, possibly nested) relative paths; every shape definition is assigned to a random compatible file
 * (an IDL file holds a single namespace) and declared in random order among the file's statements; each movable trait
 * is written either inline or in an {@code apply} statement (single or block form) in any compatible file;
 * set-valued properties (service operations, resources and errors; operation errors; resource operations) are listed
 * in random order; shape and trait references in IDL are randomly relative or absolute. Every rendering of a spec
 * assembles to the same Smithy semantic model; renderings differ in their bytes, so the content-derived model digest
 * and provenance of two renderings differ.
 *
 * <p>Instances are immutable and the output of both methods is a function of the {@link Random} passed in.
 */
public final class RandomSmithyModels {
    public static final String PRELUDE = "smithy.api";
    public static final String DOCUMENTATION = "smithy.api#documentation";
    public static final String UNIT = "smithy.api#Unit";

    /** Traits whose placement stays in the shape or member definition. */
    private static final Set<String> IMMOVABLE = Set.of("smithy.api#input", "smithy.api#output", "smithy.api#error",
                                                        "smithy.api#required", "smithy.api#enumValue");
    /** Traits whose empty-object value may be written as a bare IDL annotation ({@code @sensitive}). */
    private static final Set<String> ANNOTATIONS = Set.of(
        "smithy.api#sensitive", "smithy.api#input", "smithy.api#output", "smithy.api#required", "smithy.api#readonly",
        "smithy.api#idempotent", "smithy.api#retryable", "smithy.api#optionalAuth", "smithy.api#httpBearerAuth",
        "aws.protocols#awsJson1_0", "aws.protocols#awsJson1_1");

    /** Absolute IDs of traits that have no definition anywhere, retained as dynamic traits. */
    public static final List<String> UNKNOWN_TRAITS = List.of("test.meta#tag", "vendor.ext#owner",
                                                              "com.example.custom#flags");

    private static final List<String> PRELUDE_TARGETS = List.of(
        "smithy.api#String", "smithy.api#Integer", "smithy.api#Long", "smithy.api#Boolean", "smithy.api#Timestamp",
        "smithy.api#Blob", "smithy.api#Document", "smithy.api#BigDecimal", "smithy.api#Double", "smithy.api#Short");
    private static final List<String> NOUNS = List.of(
        "Widget", "Gadget", "Bucket", "Report", "Order", "Account", "Region", "Policy", "Queue", "Stream", "Record",
        "Item", "Table", "Metric", "Alarm", "Rule", "Channel", "Ledger", "Vault", "Cluster");
    private static final List<String> VERBS = List.of(
        "Get", "List", "Put", "Delete", "Describe", "Update", "Create", "Start", "Stop", "Tag", "Run", "Batch");
    private static final List<String> SUFFIXES = List.of(
        "", "Summary", "Detail", "Kind", "Status", "Config", "Info", "Entry", "Spec", "Value");
    private static final List<String> MEMBERS = List.of(
        "name", "value", "count", "items", "status", "createdAt", "tags", "next", "description", "size", "enabled",
        "data", "owner", "kind", "class", "SSEKey", "nextToken");
    private static final List<String> ENUM_MEMBERS = List.of(
        "RED", "GREEN", "BLUE", "ACTIVE", "INACTIVE", "PENDING", "ALPHA", "BETA", "V2", "LOW_LATENCY");
    private static final List<String> DOCS = List.of(
        "A shape.", "Uses \"quotes\" and \\ backslashes.", "Unicode: caf\u00e9 \u2615 \u00fc", "Multi\nline text.",
        "Tab\tseparated", "<p>Some <b>HTML</b> documentation.</p>", "Trailing space ", "$dollar and {braces}");
    private static final List<String> NODE_KEYS = List.of("a", "b", "name", "nested", "list", "flag", "with space");
    private static final List<String> VERSIONS = List.of("2006-03-01", "2020-01-01", "2024-11-05");

    private RandomSmithyModels() {
    }

    /**
     * A member of a shape.
     *
     * @param name member name
     * @param target absolute shape ID of the target
     * @param traits trait ID to value, in canonical order
     */
    public record Member(String name, String target, Map<String, Node> traits) {
        public Member {
            traits = Collections.unmodifiableMap(new LinkedHashMap<>(traits));
        }
    }

    /**
     * One top-level shape. Fields that do not apply to the shape type are empty or {@code null}.
     *
     * @param id absolute shape ID
     * @param type Smithy shape type name ({@code structure}, {@code enum}, {@code service}, ...)
     * @param members ordered members (structure, union, enum, intEnum; {@code member} of a list; {@code key} and
     *        {@code value} of a map)
     * @param traits trait ID to value, in canonical order
     * @param version service version
     * @param input operation input
     * @param output operation output
     * @param errors service or operation errors
     * @param operations service or resource operations
     * @param resources service or resource child resources
     * @param identifiers resource identifier name to target
     */
    public record Shape(ShapeId id, String type, List<Member> members, Map<String, Node> traits, String version,
                        String input, String output, List<String> errors, List<String> operations,
                        List<String> resources, Map<String, String> identifiers) {
        public Shape {
            members = List.copyOf(members);
            traits = Collections.unmodifiableMap(new LinkedHashMap<>(traits));
            errors = List.copyOf(errors);
            operations = List.copyOf(operations);
            resources = List.copyOf(resources);
            identifiers = Collections.unmodifiableMap(new LinkedHashMap<>(identifiers));
        }
    }

    /**
     * A generated model.
     *
     * @param service absolute ID of the service
     * @param packageName Java package to generate the service into
     * @param shapes every top-level shape, in generation order
     */
    public record ModelSpec(ShapeId service, String packageName, List<Shape> shapes) {
        public ModelSpec {
            shapes = List.copyOf(shapes);
        }

        public Shape shape(ShapeId id) {
            return shapes.stream().filter(s -> s.id().equals(id)).findFirst()
                         .orElseThrow(() -> new IllegalArgumentException("No shape " + id));
        }

        /** IDs of the shapes of {@code type}, sorted. */
        public List<ShapeId> ofType(String type) {
            return shapes.stream().filter(s -> s.type().equals(type)).map(Shape::id).sorted().toList();
        }

        /** Every namespace with a shape, sorted. */
        public Set<String> namespaces() {
            Set<String> result = new TreeSet<>();
            shapes.forEach(s -> result.add(s.id().getNamespace()));
            return result;
        }

        /** Every shape name, lower-cased, for choosing names that cannot conflict. */
        public Set<String> lowerCaseNames() {
            Set<String> result = new TreeSet<>();
            shapes.forEach(s -> result.add(s.id().getName().toLowerCase(Locale.ROOT)));
            return result;
        }

        @Override
        public String toString() {
            Map<String, Integer> counts = new java.util.TreeMap<>();
            shapes.forEach(s -> counts.merge(s.type(), 1, Integer::sum));
            return "ModelSpec[" + service + ", " + packageName + ", " + counts + "]";
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Models

    /**
     * A random valid model.
     *
     * @param random source of every choice
     * @param operationCount number of operations
     * @param dataShapeCount number of data shapes (structures, unions, enums, collections and simple shapes)
     * @param resourceCount number of resources
     * @param errorCount number of error structures
     * @param twoNamespaces whether some data shapes live in a second namespace
     */
    public static ModelSpec model(Random random, int operationCount, int dataShapeCount, int resourceCount,
                                  int errorCount, boolean twoNamespaces) {
        return new ModelBuilder(random).build(operationCount, dataShapeCount, resourceCount, errorCount,
                                              twoNamespaces);
    }

    /**
     * The V2 class name of a modeled error named {@code errorName} in service {@code serviceName}, derived here
     * independently of {@code aws-codegen}: a trailing {@code Fault} becomes {@code Exception}, {@code Exception} is
     * appended unless present, and {@code Default} is prepended when the result is the service's base exception
     * {@code <Service>Exception}.
     */
    public static String v2ExceptionName(String errorName, String serviceName) {
        String base = errorName.endsWith("Fault") ? errorName.substring(0, errorName.length() - "Fault".length())
                                                  : errorName;
        String exception = base.endsWith("Exception") ? base : base + "Exception";
        return exception.equals(serviceName + "Exception") ? "Default" + exception : exception;
    }

    private static final class Draft {
        final ShapeId id;
        final String type;
        final List<Member> members = new ArrayList<>();
        final Map<String, Node> traits = new LinkedHashMap<>();
        String version;
        String input;
        String output;
        final List<String> errors = new ArrayList<>();
        final List<String> operations = new ArrayList<>();
        final List<String> resources = new ArrayList<>();
        final Map<String, String> identifiers = new LinkedHashMap<>();

        Draft(ShapeId id, String type) {
            this.id = id;
            this.type = type;
        }

        Shape freeze() {
            return new Shape(id, type, members, traits, version, input, output, errors, operations, resources,
                             identifiers);
        }
    }

    private static final class ModelBuilder {
        private final Random random;
        private final Set<String> usedNames = new LinkedHashSet<>();
        /** Lower-cased V2 class names of the errors so far ({@link #v2ExceptionName}). */
        private final Set<String> exceptionNames = new LinkedHashSet<>();
        private final List<Draft> drafts = new ArrayList<>();
        private String serviceName;

        ModelBuilder(Random random) {
            this.random = random;
        }

        ModelSpec build(int operationCount, int dataShapeCount, int resourceCount, int errorCount,
                        boolean twoNamespaces) {
            String main = "example." + pick(NOUNS).toLowerCase(Locale.ROOT) + (random.nextBoolean() ? "" : "s");
            String shared = twoNamespaces ? main + ".shared" : main;
            serviceName = fresh(NOUNS, List.of("", "Service", "Api"));
            ShapeId serviceId = ShapeId.fromParts(main, serviceName);
            String packageName = "software.amazon.awssdk.services." + serviceName.toLowerCase(Locale.ROOT);

            List<Draft> data = dataShapes(main, shared, dataShapeCount);
            List<Draft> errors = new ArrayList<>();
            for (int i = 0; i < errorCount; i++) {
                errors.add(error(main, data));
            }
            List<Draft> operations = new ArrayList<>();
            for (int i = 0; i < operationCount; i++) {
                operations.add(operation(main, data, errors));
            }
            List<Draft> topResources = new ArrayList<>();
            List<Draft> resources = new ArrayList<>();
            for (int i = 0; i < resourceCount; i++) {
                Draft parent = !topResources.isEmpty() && random.nextInt(3) == 0 ? pick(topResources) : null;
                Draft resource = resource(main, parent);
                resources.add(resource);
                if (parent == null) {
                    topResources.add(resource);
                } else {
                    parent.resources.add(resource.id.toString());
                }
            }

            Draft service = new Draft(serviceId, "service");
            service.version = pick(VERSIONS);
            for (Draft operation : operations) {
                if (!resources.isEmpty() && random.nextInt(5) < 2) {
                    bind(pick(resources), operation);
                } else {
                    service.operations.add(operation.id.toString());
                }
            }
            topResources.forEach(r -> service.resources.add(r.id.toString()));
            errors.stream().filter(e -> random.nextInt(3) == 0).forEach(e -> service.errors.add(e.id.toString()));
            serviceTraits(service, serviceName);

            List<Draft> all = new ArrayList<>();
            all.add(service);
            all.addAll(drafts);
            return new ModelSpec(serviceId, packageName, all.stream().map(Draft::freeze).toList());
        }

        private List<Draft> dataShapes(String main, String shared, int count) {
            List<Draft> result = new ArrayList<>();
            List<String> kinds = List.of("structure", "structure", "structure", "union", "enum", "intEnum", "list",
                                         "list", "map", "string", "integer", "long", "timestamp", "blob", "boolean",
                                         "document", "bigDecimal");
            for (int i = 0; i < count; i++) {
                String kind = pick(kinds);
                String namespace = !shared.equals(main) && random.nextInt(3) == 0 ? shared : main;
                String suffix = switch (kind) {
                    case "list" -> "List";
                    case "map" -> "Map";
                    case "enum", "intEnum" -> "Type";
                    default -> pick(SUFFIXES);
                };
                result.add(draft(ShapeId.fromParts(namespace, fresh(NOUNS, List.of(suffix))), kind));
            }
            // Members are chosen after every data shape is named, so structures and unions may refer forward and form
            // cycles. Lists and maps only refer to earlier collections: a cycle must pass through a structure or union.
            for (int i = 0; i < result.size(); i++) {
                Draft shape = result.get(i);
                List<Draft> earlier = result.subList(0, i);
                switch (shape.type) {
                    case "structure" -> addMembers(shape, result, 0, 4);
                    case "union" -> {
                        shape.members.add(new Member(freshMember(shape), pick(PRELUDE_TARGETS), memberTraits(null)));
                        addMembers(shape, result, 0, 2);
                    }
                    case "enum" -> enumMembers(shape, false);
                    case "intEnum" -> enumMembers(shape, true);
                    case "list" -> {
                        String target = collectionTarget(result, earlier);
                        shape.members.add(new Member("member", target, memberTraits(target)));
                        maybe(4, () -> shape.traits.put("smithy.api#length", range("min", "max")));
                    }
                    case "map" -> {
                        List<String> keys = new ArrayList<>(List.of("smithy.api#String"));
                        result.stream().filter(d -> d.type.equals("string") || d.type.equals("enum"))
                              .forEach(d -> keys.add(d.id.toString()));
                        shape.members.add(new Member("key", pick(keys), Map.of()));
                        String value = collectionTarget(result, earlier);
                        shape.members.add(new Member("value", value, memberTraits(value)));
                    }
                    case "string" -> {
                        maybe(3, () -> shape.traits.put("smithy.api#length", range("min", "max")));
                        maybe(4, () -> shape.traits.put("smithy.api#pattern", Node.from("^[a-z0-9-]+$")));
                    }
                    case "integer", "long" ->
                        maybe(3, () -> shape.traits.put("smithy.api#range", range("min", "max")));
                    default -> {
                    }
                }
                commonTraits(shape, true);
            }
            return result;
        }

        /**
         * A modeled error. Its name is also unique among the errors' V2 class names ({@code WidgetFault} and
         * {@code WidgetException} would both be {@code WidgetException}): {@code aws-codegen} rejects such models by
         * design with a {@code GENERATED_NAME_COLLISION} diagnostic, so they are never generated here.
         */
        private Draft error(String namespace, List<Draft> data) {
            String name;
            do {
                name = fresh(NOUNS, List.of("Error", "Fault", "Exception"));
            } while (!exceptionNames.add(v2ExceptionName(name, serviceName).toLowerCase(Locale.ROOT)));
            Draft error = draft(ShapeId.fromParts(namespace, name), "structure");
            error.traits.put("smithy.api#error", Node.from(random.nextBoolean() ? "client" : "server"));
            maybe(4, () -> error.traits.put("smithy.api#retryable", Node.objectNode()));
            error.members.add(new Member("message", "smithy.api#String", memberTraits(null)));
            addMembers(error, data, 0, 2);
            commonTraits(error, false);
            return error;
        }

        private Draft operation(String namespace, List<Draft> data, List<Draft> errors) {
            String name = fresh(VERBS, NOUNS);
            Draft operation = draft(ShapeId.fromParts(namespace, name), "operation");
            if (random.nextInt(4) > 0) {
                operation.input = io(namespace, name + "Input", "smithy.api#input", data).id.toString();
            }
            if (random.nextInt(4) > 0) {
                operation.output = io(namespace, name + "Output", "smithy.api#output", data).id.toString();
            }
            errors.stream().filter(e -> random.nextInt(3) == 0).forEach(e -> operation.errors.add(e.id.toString()));
            maybe(2, () -> operation.traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
            maybe(12, () -> operation.traits.put("smithy.api#deprecated", deprecated()));
            switch (random.nextInt(5)) {
                case 0 -> operation.traits.put("smithy.api#readonly", Node.objectNode());
                case 1 -> operation.traits.put("smithy.api#idempotent", Node.objectNode());
                default -> {
                }
            }
            maybe(10, () -> operation.traits.put("smithy.api#optionalAuth", Node.objectNode()));
            unknownTraits(operation.traits);
            return operation;
        }

        private Draft io(String namespace, String name, String trait, List<Draft> data) {
            usedNames.add(name.toLowerCase(Locale.ROOT));
            Draft structure = draft(ShapeId.fromParts(namespace, name), "structure");
            structure.traits.put(trait, Node.objectNode());
            addMembers(structure, data, 0, 3);
            maybe(4, () -> structure.traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
            unknownTraits(structure.traits);
            return structure;
        }

        private Draft resource(String namespace, Draft parent) {
            String name = fresh(NOUNS, List.of("Resource", "Collection"));
            Draft resource = draft(ShapeId.fromParts(namespace, name), "resource");
            if (parent != null) {
                resource.identifiers.putAll(parent.identifiers);
            }
            resource.identifiers.put(Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Id",
                                     "smithy.api#String");
            maybe(2, () -> resource.traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
            unknownTraits(resource.traits);
            return resource;
        }

        /** Binds {@code operation} to {@code resource} as an instance operation: its input binds every identifier. */
        private void bind(Draft resource, Draft operation) {
            Draft input;
            if (operation.input == null) {
                input = io(operation.id.getNamespace(), operation.id.getName() + "Input", "smithy.api#input",
                           List.of());
                operation.input = input.id.toString();
            } else {
                input = drafts.stream().filter(d -> d.id.toString().equals(operation.input)).findFirst()
                              .orElseThrow();
            }
            List<Member> identifiers = new ArrayList<>();
            resource.identifiers.forEach((name, target) -> {
                Map<String, Node> traits = new LinkedHashMap<>();
                traits.put("smithy.api#required", Node.objectNode());
                maybe(4, () -> traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
                identifiers.add(new Member(name, target, traits));
            });
            input.members.addAll(0, identifiers);
            resource.operations.add(operation.id.toString());
        }

        private void serviceTraits(Draft service, String serviceName) {
            switch (random.nextInt(3)) {
                case 0 -> service.traits.put("aws.protocols#awsJson1_0", Node.objectNode());
                case 1 -> service.traits.put("aws.protocols#awsJson1_1", Node.objectNode());
                default -> {
                    service.traits.put("aws.protocols#awsJson1_1", Node.objectNode());
                    service.traits.put("aws.protocols#awsJson1_0", Node.objectNode());
                }
            }
            ObjectNode sigv4 = Node.objectNode().withMember("name", serviceName.toLowerCase(Locale.ROOT));
            switch (random.nextInt(4)) {
                case 0 -> service.traits.put("aws.auth#sigv4", sigv4);
                case 1 -> {
                    service.traits.put("smithy.api#httpBearerAuth", Node.objectNode());
                    service.traits.put("aws.auth#sigv4", sigv4);
                }
                case 2 -> {
                    service.traits.put("aws.auth#sigv4", sigv4);
                    service.traits.put("smithy.api#httpBearerAuth", Node.objectNode());
                    List<String> priority = new ArrayList<>(List.of("smithy.api#httpBearerAuth", "aws.auth#sigv4"));
                    Collections.shuffle(priority, random);
                    service.traits.put("smithy.api#auth", ArrayNode.fromStrings(priority));
                }
                default -> service.traits.put("smithy.api#auth", Node.arrayNode());
            }
            maybe(2, () -> service.traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
            // The SDK ID names the generated client types as V2 does (DynamoDB -> DynamoDbClient), so it is only
            // declared when its V2 Pascal-case name is the service shape name, which every oracle relies on: one
            // capitalized word with an optional numeric suffix. Such an ID never ends with "Api", "Service" or
            // "Client" (Smithy's SdkServiceId validator).
            if (serviceName.matches("[A-Z][a-z]+[0-9]*")) {
                maybe(3, () -> service.traits.put("aws.api#service",
                                                  Node.objectNode().withMember("sdkId", serviceName)));
            }
            unknownTraits(service.traits);
        }

        private void addMembers(Draft shape, List<Draft> data, int min, int max) {
            List<String> targets = new ArrayList<>(PRELUDE_TARGETS);
            data.forEach(d -> targets.add(d.id.toString()));
            int count = min + random.nextInt(max - min + 1);
            for (int i = 0; i < count; i++) {
                String target = pick(targets);
                shape.members.add(new Member(freshMember(shape), target, memberTraits(target)));
            }
        }

        private String collectionTarget(List<Draft> data, List<Draft> earlier) {
            List<String> targets = new ArrayList<>(PRELUDE_TARGETS);
            for (Draft d : data) {
                boolean collection = d.type.equals("list") || d.type.equals("map");
                if (!collection || earlier.contains(d)) {
                    targets.add(d.id.toString());
                }
            }
            return pick(targets);
        }

        private void enumMembers(Draft shape, boolean intEnum) {
            List<String> names = new ArrayList<>(ENUM_MEMBERS);
            Collections.shuffle(names, random);
            int count = 1 + random.nextInt(4);
            Set<Integer> values = new TreeSet<>();
            for (String name : names.subList(0, count)) {
                Map<String, Node> traits = new LinkedHashMap<>();
                if (intEnum) {
                    int value;
                    do {
                        value = random.nextInt(21) - 5;
                    } while (!values.add(value));
                    traits.put("smithy.api#enumValue", Node.from(value));
                } else {
                    traits.put("smithy.api#enumValue",
                               Node.from(random.nextBoolean() ? name : name.toLowerCase(Locale.ROOT) + "-value"));
                }
                maybe(4, () -> traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
                maybe(8, () -> traits.put(pick(UNKNOWN_TRAITS), randomNode(2)));
                shape.members.add(new Member(name, UNIT, traits));
            }
        }

        private Map<String, Node> memberTraits(String target) {
            Map<String, Node> traits = new LinkedHashMap<>();
            maybe(3, () -> traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
            maybe(15, () -> traits.put("smithy.api#deprecated", deprecated()));
            if ("smithy.api#Timestamp".equals(target)) {
                maybe(3, () -> traits.put("smithy.api#timestampFormat",
                                          Node.from(pick(List.of("epoch-seconds", "date-time", "http-date")))));
            }
            maybe(6, () -> traits.put(pick(UNKNOWN_TRAITS), randomNode(3)));
            return traits;
        }

        private void commonTraits(Draft shape, boolean data) {
            maybe(2, () -> shape.traits.put(DOCUMENTATION, Node.from(pick(DOCS))));
            maybe(10, () -> shape.traits.put("smithy.api#deprecated", deprecated()));
            if (data) {
                maybe(6, () -> shape.traits.put("smithy.api#sensitive", Node.objectNode()));
            }
            unknownTraits(shape.traits);
        }

        private void unknownTraits(Map<String, Node> traits) {
            List<String> ids = new ArrayList<>(UNKNOWN_TRAITS);
            Collections.shuffle(ids, random);
            int count = random.nextInt(4) == 0 ? 1 + random.nextInt(ids.size()) : 0;
            ids.subList(0, count).forEach(id -> traits.put(id, randomNode(3)));
        }

        private Node deprecated() {
            ObjectNode.Builder node = ObjectNode.builder();
            if (random.nextBoolean()) {
                node.withMember("message", pick(DOCS));
            }
            if (random.nextBoolean()) {
                node.withMember("since", pick(VERSIONS));
            }
            return node.build();
        }

        private Node range(String min, String max) {
            int low = random.nextInt(10);
            return Node.objectNode().withMember(min, low).withMember(max, low + 1 + random.nextInt(100));
        }

        /** A random node tree: strings, integers, booleans, null, arrays and objects. */
        private Node randomNode(int depth) {
            int kind = random.nextInt(depth > 0 ? 7 : 4);
            return switch (kind) {
                case 0 -> Node.from(pick(DOCS));
                case 1 -> Node.from(random.nextInt(2001) - 1000);
                case 2 -> Node.from(random.nextBoolean());
                case 3 -> random.nextInt(4) == 0 ? Node.nullNode() : Node.from(pick(NODE_KEYS));
                case 4 -> {
                    List<Node> items = new ArrayList<>();
                    int count = random.nextInt(4);
                    for (int i = 0; i < count; i++) {
                        items.add(randomNode(depth - 1));
                    }
                    yield Node.fromNodes(items);
                }
                default -> {
                    ObjectNode.Builder object = ObjectNode.builder();
                    List<String> keys = new ArrayList<>(NODE_KEYS);
                    Collections.shuffle(keys, random);
                    int count = random.nextInt(4);
                    for (String key : keys.subList(0, count)) {
                        object.withMember(key, randomNode(depth - 1));
                    }
                    yield object.build();
                }
            };
        }

        private Draft draft(ShapeId id, String type) {
            Draft draft = new Draft(id, type);
            drafts.add(draft);
            return draft;
        }

        /** A shape name unique in the model, case-insensitively (Smithy's service-closure conflict rule). */
        private String fresh(List<String> prefixes, List<String> suffixes) {
            for (int attempt = 0; ; attempt++) {
                String candidate = pick(prefixes) + pick(suffixes) + (attempt < 8 ? "" : Integer.toString(attempt));
                if (usedNames.add(candidate.toLowerCase(Locale.ROOT))) {
                    return candidate;
                }
            }
        }

        private String freshMember(Draft shape) {
            Set<String> used = new TreeSet<>();
            shape.members.forEach(m -> used.add(m.name().toLowerCase(Locale.ROOT)));
            for (int attempt = 0; ; attempt++) {
                String candidate = pick(MEMBERS) + (attempt < 6 ? "" : Integer.toString(attempt));
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

    // -----------------------------------------------------------------------------------------------------------------
    // Renderings

    /**
     * A random rendering of {@code model} as model files: relative path ({@code /}-separated, lower-case) to content.
     * The map order is the declaration order of the files; callers choose their creation order.
     */
    public static Map<String, String> render(ModelSpec model, Random random) {
        return new Renderer(model, random).render();
    }

    /**
     * One statement of a rendering: the definition of a shape (with its inline traits), or an {@code apply} of traits
     * to a shape or member.
     */
    private record Statement(Shape definition, ShapeId target, String member, Map<String, Node> traits) {
        String namespace() {
            return target.getNamespace();
        }

        /** Absolute ID a JSON AST keys this statement by. */
        String jsonKey() {
            return member == null ? target.toString() : target + "$" + member;
        }
    }

    private record ModelFile(String path, boolean json, String namespace, List<Statement> statements) {
    }

    private static final class Renderer {
        private final ModelSpec model;
        private final Random random;
        private final String indent;

        Renderer(ModelSpec model, Random random) {
            this.model = model;
            this.random = random;
            this.indent = random.nextBoolean() ? "    " : "  ";
        }

        Map<String, String> render() {
            List<Statement> statements = statements();
            List<ModelFile> files = files(statements.size());
            for (Statement statement : statements) {
                List<ModelFile> compatible = files.stream()
                                                  .filter(f -> f.json() || f.namespace().equals(statement.namespace()))
                                                  .toList();
                compatible.get(random.nextInt(compatible.size())).statements().add(statement);
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (ModelFile file : files) {
                Collections.shuffle(file.statements(), random);
                result.put(file.path(), file.json() ? json(file) : idl(file));
            }
            return result;
        }

        /** Shape definitions with their inline traits, plus one apply statement per trait moved out of them. */
        private List<Statement> statements() {
            boolean applyTraits = random.nextInt(3) > 0;
            List<Statement> result = new ArrayList<>();
            for (Shape shape : model.shapes()) {
                Map<String, Node> inline = new LinkedHashMap<>();
                split(shape.traits(), applyTraits, inline, (trait, value) ->
                    result.add(new Statement(null, shape.id(), null, Map.of(trait, value))));
                List<Member> members = new ArrayList<>();
                for (Member member : shape.members()) {
                    Map<String, Node> memberInline = new LinkedHashMap<>();
                    split(member.traits(), applyTraits, memberInline, (trait, value) ->
                        result.add(new Statement(null, shape.id(), member.name(), Map.of(trait, value))));
                    members.add(new Member(member.name(), member.target(), memberInline));
                }
                Shape definition = new Shape(shape.id(), shape.type(), members, shuffled(inline), shape.version(),
                                             shape.input(), shape.output(), shuffled(shape.errors()),
                                             shuffled(shape.operations()), shuffled(shape.resources()),
                                             shape.identifiers());
                result.add(new Statement(definition, shape.id(), null, Map.of()));
            }
            return result;
        }

        private void split(Map<String, Node> traits, boolean applyTraits, Map<String, Node> inline,
                           java.util.function.BiConsumer<String, Node> applied) {
            traits.forEach((trait, value) -> {
                if (applyTraits && !IMMOVABLE.contains(trait) && random.nextInt(3) == 0) {
                    applied.accept(trait, value);
                } else {
                    inline.put(trait, value);
                }
            });
        }

        /** Between 1 and 6 files; every namespace has at least one compatible file. */
        private List<ModelFile> files(int statementCount) {
            List<String> namespaces = new ArrayList<>(model.namespaces());
            int count = 1 + random.nextInt(Math.min(6, Math.max(1, statementCount)));
            List<ModelFile> files = new ArrayList<>();
            Set<String> paths = new TreeSet<>();
            for (int i = 0; i < count; i++) {
                boolean json = random.nextInt(3) == 0;
                files.add(newFile(paths, json, namespaces.get(random.nextInt(namespaces.size()))));
            }
            for (String namespace : namespaces) {
                if (files.stream().noneMatch(f -> f.json() || f.namespace().equals(namespace))) {
                    files.add(newFile(paths, false, namespace));
                }
            }
            return files;
        }

        private ModelFile newFile(Set<String> paths, boolean json, String namespace) {
            List<String> directories = List.of("model", "shapes", "aws", "ops", "nested", "common", "a-b");
            List<String> names = List.of("main", "service", "types", "errors", "traits", "model", "shared", "x");
            String path;
            do {
                StringBuilder builder = new StringBuilder();
                int depth = random.nextInt(3);
                for (int i = 0; i < depth; i++) {
                    builder.append(directories.get(random.nextInt(directories.size()))).append('/');
                }
                builder.append(names.get(random.nextInt(names.size()))).append('-').append(paths.size())
                       .append(json ? ".json" : ".smithy");
                path = builder.toString();
            } while (!paths.add(path));
            return new ModelFile(path, json, json ? null : namespace, new ArrayList<>());
        }

        // -------------------------------------------------------------------------------------------------------------
        // Smithy IDL

        private String idl(ModelFile file) {
            StringBuilder out = new StringBuilder();
            out.append("$version: \"").append(random.nextBoolean() ? "2" : "2.0").append("\"\n\n");
            out.append("namespace ").append(file.namespace()).append('\n');
            for (Statement statement : file.statements()) {
                out.append('\n');
                if (random.nextInt(5) == 0) {
                    out.append("// ").append(statement.jsonKey()).append('\n');
                }
                if (statement.definition() == null) {
                    applyIdl(out, statement, file.namespace());
                } else {
                    definitionIdl(out, statement.definition(), file.namespace());
                }
            }
            return out.toString();
        }

        private void applyIdl(StringBuilder out, Statement statement, String namespace) {
            String target = ref(statement.target().toString(), namespace)
                            + (statement.member() == null ? "" : "$" + statement.member());
            if (random.nextBoolean()) {
                out.append("apply ").append(target).append(" {\n");
                statement.traits().forEach((trait, value) -> out.append(indent).append(trait(trait, value, namespace))
                                                                .append('\n'));
                out.append("}\n");
            } else {
                statement.traits().forEach((trait, value) -> out.append("apply ").append(target).append(' ')
                                                                .append(trait(trait, value, namespace)).append('\n'));
            }
        }

        private void definitionIdl(StringBuilder out, Shape shape, String namespace) {
            shape.traits().forEach((trait, value) -> out.append(trait(trait, value, namespace)).append('\n'));
            String name = shape.id().getName();
            switch (shape.type()) {
                case "service" -> {
                    out.append("service ").append(name).append(" {\n");
                    out.append(indent).append("version: ").append(Node.printJson(Node.from(shape.version())))
                       .append('\n');
                    list(out, "operations", shape.operations(), namespace);
                    list(out, "resources", shape.resources(), namespace);
                    list(out, "errors", shape.errors(), namespace);
                    out.append("}\n");
                }
                case "operation" -> {
                    out.append("operation ").append(name).append(" {\n");
                    if (shape.input() != null) {
                        out.append(indent).append("input: ").append(ref(shape.input(), namespace)).append('\n');
                    }
                    if (shape.output() != null) {
                        out.append(indent).append("output: ").append(ref(shape.output(), namespace)).append('\n');
                    }
                    list(out, "errors", shape.errors(), namespace);
                    out.append("}\n");
                }
                case "resource" -> {
                    out.append("resource ").append(name).append(" {\n");
                    out.append(indent).append("identifiers: {");
                    List<String> identifiers = new ArrayList<>();
                    shape.identifiers().forEach((id, target) -> identifiers.add(id + ": " + ref(target, namespace)));
                    out.append(' ').append(String.join(separator(), identifiers)).append(" }\n");
                    list(out, "operations", shape.operations(), namespace);
                    list(out, "resources", shape.resources(), namespace);
                    out.append("}\n");
                }
                case "enum", "intEnum" -> {
                    out.append(shape.type()).append(' ').append(name).append(" {\n");
                    for (Member member : shape.members()) {
                        memberTraitsIdl(out, member, namespace);
                        Node value = member.traits().get("smithy.api#enumValue");
                        out.append(indent).append(member.name());
                        boolean implicit = value.isStringNode() && value.expectStringNode().getValue()
                                                                        .equals(member.name());
                        if (!implicit || random.nextBoolean()) {
                            out.append(" = ").append(Node.printJson(value));
                        }
                        out.append('\n');
                    }
                    out.append("}\n");
                }
                case "structure", "union", "list", "map" -> {
                    out.append(shape.type()).append(' ').append(name).append(" {\n");
                    for (Member member : shape.members()) {
                        memberTraitsIdl(out, member, namespace);
                        out.append(indent).append(member.name()).append(": ").append(ref(member.target(), namespace))
                           .append('\n');
                    }
                    out.append("}\n");
                }
                default -> out.append(shape.type()).append(' ').append(name).append('\n');
            }
        }

        private void memberTraitsIdl(StringBuilder out, Member member, String namespace) {
            shuffled(member.traits()).forEach((trait, value) -> {
                if (!trait.equals("smithy.api#enumValue")) {
                    out.append(indent).append(trait(trait, value, namespace)).append('\n');
                }
            });
        }

        private void list(StringBuilder out, String property, List<String> ids, String namespace) {
            if (ids.isEmpty() && random.nextBoolean()) {
                return;
            }
            out.append(indent).append(property).append(": [");
            out.append(String.join(separator(), ids.stream().map(id -> ref(id, namespace)).toList()));
            out.append("]\n");
        }

        /** Commas are whitespace in the Smithy IDL. */
        private String separator() {
            return random.nextBoolean() ? ", " : " ";
        }

        private String trait(String trait, Node value, String namespace) {
            String ref = ref(trait, namespace);
            boolean empty = value.isObjectNode() && value.expectObjectNode().isEmpty();
            if (empty && ANNOTATIONS.contains(trait) && random.nextBoolean()) {
                return "@" + ref;
            }
            return "@" + ref + "(" + Node.printJson(value) + ")";
        }

        /** A shape reference from a file of {@code namespace}: relative where it resolves identically, else absolute. */
        private String ref(String absolute, String namespace) {
            ShapeId id = ShapeId.from(absolute);
            boolean relative = id.getNamespace().equals(namespace)
                               || id.getNamespace().equals(PRELUDE) && !model.lowerCaseNames().contains(
                                   id.getName().toLowerCase(Locale.ROOT));
            return relative && random.nextBoolean() ? id.getName() : id.toString();
        }

        // -------------------------------------------------------------------------------------------------------------
        // Smithy JSON AST

        private String json(ModelFile file) {
            Map<String, ObjectNode> shapes = new LinkedHashMap<>();
            for (Statement statement : file.statements()) {
                ObjectNode node = statement.definition() == null ? apply(statement)
                                                                 : definitionJson(statement.definition());
                // Keys of a JSON object are unique: traits applied in the same file merge into the entry for the ID.
                shapes.merge(statement.jsonKey(), node, Renderer::mergeTraits);
            }
            ObjectNode.Builder shapesNode = ObjectNode.builder();
            shapes.forEach(shapesNode::withMember);
            ObjectNode root = ObjectNode.builder().withMember("smithy", "2.0").withMember("shapes", shapesNode.build())
                                        .build();
            return (random.nextBoolean() ? Node.prettyPrintJson(root) : Node.printJson(root)) + "\n";
        }

        private static ObjectNode mergeTraits(ObjectNode first, ObjectNode second) {
            ObjectNode traits = first.getObjectMember("traits").orElse(Node.objectNode())
                                     .merge(second.getObjectMember("traits").orElse(Node.objectNode()));
            ObjectNode base = first.expectStringMember("type").getValue().equals("apply") ? second : first;
            return base.withMember("traits", traits);
        }

        private ObjectNode apply(Statement statement) {
            return ObjectNode.builder().withMember("type", "apply").withMember("traits", traits(statement.traits()))
                             .build();
        }

        private ObjectNode definitionJson(Shape shape) {
            ObjectNode.Builder node = ObjectNode.builder().withMember("type", shape.type());
            switch (shape.type()) {
                case "service" -> {
                    node.withMember("version", shape.version());
                    targets(node, "operations", shape.operations());
                    targets(node, "resources", shape.resources());
                    targets(node, "errors", shape.errors());
                }
                case "operation" -> {
                    if (shape.input() != null) {
                        node.withMember("input", target(shape.input()));
                    }
                    if (shape.output() != null) {
                        node.withMember("output", target(shape.output()));
                    }
                    targets(node, "errors", shape.errors());
                }
                case "resource" -> {
                    ObjectNode.Builder identifiers = ObjectNode.builder();
                    shape.identifiers().forEach((id, target) -> identifiers.withMember(id, target(target)));
                    node.withMember("identifiers", identifiers.build());
                    targets(node, "operations", shape.operations());
                    targets(node, "resources", shape.resources());
                }
                case "list", "map" -> shape.members().forEach(m -> node.withMember(m.name(), member(m)));
                case "structure", "union", "enum", "intEnum" -> {
                    ObjectNode.Builder members = ObjectNode.builder();
                    shape.members().forEach(m -> members.withMember(m.name(), member(m)));
                    node.withMember("members", members.build());
                }
                default -> {
                }
            }
            if (!shape.traits().isEmpty() || random.nextInt(8) == 0) {
                node.withMember("traits", traits(shape.traits()));
            }
            return node.build();
        }

        private ObjectNode member(Member member) {
            ObjectNode node = target(member.target());
            return member.traits().isEmpty() ? node : node.withMember("traits", traits(shuffled(member.traits())));
        }

        private void targets(ObjectNode.Builder node, String property, List<String> ids) {
            if (!ids.isEmpty() || random.nextBoolean()) {
                node.withMember(property, Node.fromNodes(ids.stream().<Node>map(Renderer::target).toList()));
            }
        }

        private static ObjectNode target(String id) {
            return Node.objectNode().withMember("target", id);
        }

        private static ObjectNode traits(Map<String, Node> traits) {
            ObjectNode.Builder node = ObjectNode.builder();
            traits.forEach(node::withMember);
            return node.build();
        }

        // -------------------------------------------------------------------------------------------------------------

        private <T> List<T> shuffled(List<T> items) {
            List<T> copy = new ArrayList<>(items);
            Collections.shuffle(copy, random);
            return copy;
        }

        private Map<String, Node> shuffled(Map<String, Node> traits) {
            List<String> keys = shuffled(new ArrayList<>(traits.keySet()));
            Map<String, Node> result = new LinkedHashMap<>();
            keys.forEach(k -> result.put(k, Objects.requireNonNull(traits.get(k))));
            return result;
        }
    }
}
