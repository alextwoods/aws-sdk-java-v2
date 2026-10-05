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

package software.amazon.awssdk.tools.paritymanifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * The V2 public API that V2 generates from its codegen sidecars and customizations rather than from the service model
 * alone (Requirements 16.5, 2.4, 2.8), for every V2 service client module whose released jar
 * {@link PublicApiInventorySource} does not scan (its {@value PublicApiInventorySource#MODULES_OPTION} option, by
 * default {@link PublicApiInventorySource#DEFAULT_MODULES}; scanned modules are inventoried exactly from bytecode
 * there). V3 generates only from the canonical Smithy models, so this is the surface a V3 service loses when the
 * canonical model lacks what a V2 sidecar or customization adds:
 *
 * <ul>
 *   <li>paginators ({@code paginators-1.json}): {@code <Operation>Iterable} (unless V2 skips the sync client) and
 *       {@code <Operation>Publisher} in the paginators package, for every paginator V2 generates (input and output
 *       tokens present, output tokens of the form {@code [a-zA-Z.]+}), operation not excluded by
 *       {@code operationModifiers};</li>
 *   <li>renamed members ({@code shapeModifiers} {@code emitPropertyName}): the V2 fluent getter of the renamed
 *       member ({@code <name>AsString[s]} when it is or holds an enum), with its V2 JVM descriptor;</li>
 *   <li>renamed enum values ({@code shapeModifiers} {@code emitEnumName}): the enum constant;</li>
 *   <li>renamed shapes ({@code renameShapes}): the renamed model type (operation inputs and outputs keep their
 *       operation-derived names and are skipped).</li>
 * </ul>
 *
 * <p>Names, packages and descriptors follow V2's {@code DefaultNamingStrategy} (service name from the C2J
 * {@code serviceId}; {@code shareModelConfig} packages; {@code Request}/{@code Response} names of operation inputs and
 * outputs; {@code Exception} names of errors; member types: string, enum, boolean, numeric, timestamp ({@code Instant}),
 * blob ({@code SdkBytes}), list ({@code List}), map ({@code Map}), structure). Items are deprecated when the C2J model
 * or {@code deprecatedOperations}/{@code deprecatedShapes} deprecates the operation or shape. The C2J model is only
 * read to name V2's API; it is never a V3 generation input.
 */
public final class V2CodegenSurfaceInventorySource implements InventorySource {
    static final String ROOT_PACKAGE = "software.amazon.awssdk.services";
    private static final Path CODEGEN_RESOURCES = Path.of("src/main/resources/codegen-resources");
    private static final Pattern OUTPUT_TOKEN = Pattern.compile("[a-zA-Z.]+");
    private static final Map<String, String> SIMPLE_TYPES = Map.ofEntries(
        Map.entry("string", "Ljava/lang/String;"), Map.entry("boolean", "Ljava/lang/Boolean;"),
        Map.entry("integer", "Ljava/lang/Integer;"), Map.entry("long", "Ljava/lang/Long;"),
        Map.entry("short", "Ljava/lang/Short;"), Map.entry("byte", "Ljava/lang/Byte;"),
        Map.entry("float", "Ljava/lang/Float;"), Map.entry("double", "Ljava/lang/Double;"),
        Map.entry("bigdecimal", "Ljava/math/BigDecimal;"), Map.entry("biginteger", "Ljava/math/BigInteger;"),
        Map.entry("timestamp", "Ljava/time/Instant;"), Map.entry("blob", "Lsoftware/amazon/awssdk/core/SdkBytes;"),
        Map.entry("list", "Ljava/util/List;"), Map.entry("map", "Ljava/util/Map;"),
        Map.entry("document", "Lsoftware/amazon/awssdk/core/document/Document;"));

    @Override
    public String name() {
        return "v2-codegen-surface";
    }

    @Override
    public List<InventoryItem> scan(Context context) throws IOException {
        Set<String> scanned = new HashSet<>(PublicApiInventorySource.modules(context));
        Map<String, InventoryItem> items = new TreeMap<>();
        for (V2PomTree.Module m : V2PomTree.modules(context.repositoryRoot())) {
            String path = m.path();
            if (!path.startsWith("services/") || path.indexOf('/', "services/".length()) >= 0
                || scanned.contains(m.artifactId())) {
                continue;
            }
            Path resources = context.repositoryRoot().resolve(path).resolve(CODEGEN_RESOURCES);
            for (Path directory : serviceDirectories(resources)) {
                String location = path + "/" + CODEGEN_RESOURCES.toString().replace('\\', '/')
                                  + (directory.equals(resources) ? "" : "/" + directory.getFileName());
                new Service(m.artifactId(), directory, location).items()
                                                                .forEach(i -> items.putIfAbsent(i.id(), i));
            }
        }
        return List.copyOf(items.values());
    }

    /** The codegen-resources directory itself, or its sub-directories of a module that bundles several services. */
    static List<Path> serviceDirectories(Path resources) throws IOException {
        if (Files.isRegularFile(resources.resolve("service-2.json"))) {
            return List.of(resources);
        }
        if (!Files.isDirectory(resources)) {
            return List.of();
        }
        try (Stream<Path> list = Files.list(resources)) {
            return list.filter(d -> Files.isRegularFile(d.resolve("service-2.json"))).sorted().toList();
        }
    }

    /** One V2 service's C2J model and customization. */
    static final class Service {
        private final String artifact;
        private final String location;
        private final Path directory;
        private final ObjectNode customization;
        private final Map<String, ObjectNode> shapes = new TreeMap<>();
        private final Map<String, String> originalNames = new TreeMap<>();
        private final Map<String, String> inputs = new TreeMap<>();
        private final Map<String, String> outputs = new TreeMap<>();
        private final ObjectNode operations;
        private final String serviceName;
        private final String clientPackage;
        private final String modelPackage;
        private final Set<String> deprecatedOperations = new HashSet<>();
        private final Set<String> deprecatedShapes = new HashSet<>();

        Service(String artifact, Path directory, String location) throws IOException {
            this.artifact = artifact;
            this.directory = directory;
            this.location = location;
            ObjectNode c2j = Node.parse(Files.readString(directory.resolve("service-2.json"))).expectObjectNode();
            Path config = directory.resolve("customization.config");
            customization = Files.isRegularFile(config)
                            ? Node.parseJsonWithComments(Files.readString(config), config.toString()).expectObjectNode()
                            : Node.objectNode();
            ObjectNode renames = customization.getObjectMember("renameShapes").orElse(Node.objectNode());
            c2j.getObjectMember("shapes").orElse(Node.objectNode()).getMembers().forEach((name, shape) -> {
                String renamed = renames.getStringMember(name.getValue()).map(StringNode::getValue)
                                        .orElse(name.getValue());
                shapes.put(renamed, shape.expectObjectNode());
                originalNames.put(renamed, name.getValue());
            });
            operations = c2j.getObjectMember("operations").orElse(Node.objectNode());
            operations.getMembers().forEach((name, operation) -> {
                ObjectNode op = operation.expectObjectNode();
                // Operations reference shapes by their C2J names; V2 renames before naming classes.
                shapeRef(op, "input").map(this::resolve).ifPresent(s -> inputs.putIfAbsent(s, name.getValue()));
                shapeRef(op, "output").map(this::resolve).ifPresent(s -> outputs.putIfAbsent(s, name.getValue()));
                if (op.getBooleanMemberOrDefault("deprecated", false)) {
                    deprecatedOperations.add(name.getValue());
                }
            });
            customization.getArrayMember("deprecatedOperations")
                         .ifPresent(a -> a.getElements().forEach(e -> deprecatedOperations.add(string(e))));
            customization.getArrayMember("deprecatedShapes")
                         .ifPresent(a -> a.getElements().forEach(e -> deprecatedShapes.add(string(e))));
            String serviceId = c2j.expectObjectMember("metadata").getStringMember("serviceId")
                                  .map(StringNode::getValue)
                                  .orElseThrow(() -> new IOException("No metadata.serviceId in " + location));
            serviceName = serviceName(serviceId);
            ObjectNode share = customization.getObjectMember("shareModelConfig").orElse(null);
            String shareWith = share == null ? null : share.getStringMember("shareModelWith")
                                                           .map(StringNode::getValue).orElse(null);
            String clientName = share == null
                                ? serviceName
                                : shareWith + "." + share.getStringMember("packageName").map(StringNode::getValue)
                                                         .orElse(serviceName);
            clientPackage = ROOT_PACKAGE + "." + clientName.toLowerCase(Locale.ROOT);
            modelPackage = ROOT_PACKAGE + "." + (shareWith != null ? shareWith : serviceName).toLowerCase(Locale.ROOT)
                           + ".model";
        }

        List<InventoryItem> items() throws IOException {
            List<InventoryItem> items = new ArrayList<>();
            paginators(items);
            shapeModifiers(items);
            renamedShapes(items);
            return items;
        }

        private void paginators(List<InventoryItem> items) throws IOException {
            Path file = directory.resolve("paginators-1.json");
            if (!Files.isRegularFile(file)) {
                return;
            }
            boolean sync = !customization.getBooleanMemberOrDefault("skipSyncClientGeneration", false);
            ObjectNode modifiers = customization.getObjectMember("operationModifiers").orElse(Node.objectNode());
            ObjectNode pagination = Node.parse(Files.readString(file)).expectObjectNode()
                                        .getObjectMember("pagination").orElse(Node.objectNode());
            String source = location + "/paginators-1.json";
            pagination.getMembers().forEach((key, value) -> {
                String operation = key.getValue();
                boolean excluded = modifiers.getObjectMember(operation)
                                            .map(o -> o.getBooleanMemberOrDefault("exclude", false)).orElse(false);
                if (excluded || !operations.containsMember(operation) || !generated(value.expectObjectNode())) {
                    return;
                }
                boolean deprecated = deprecatedOperations.contains(operation);
                String paginators = clientPackage + ".paginators.";
                if (sync) {
                    items.add(type(paginators + operation + "Iterable", "sync paginator " + operation + "Iterable",
                                   source, deprecated));
                }
                items.add(type(paginators + operation + "Publisher", "async paginator " + operation + "Publisher",
                               source, deprecated));
            });
        }

        /** V2 {@code PaginatorDefinition.hasAllRequiredFields}. */
        private static boolean generated(ObjectNode paginator) {
            List<String> input = tokens(paginator.getMember("input_token").orElse(null));
            List<String> output = tokens(paginator.getMember("output_token").orElse(null));
            return !input.isEmpty() && !output.isEmpty() && output.stream().allMatch(t -> OUTPUT_TOKEN.matcher(t)
                                                                                                    .matches());
        }

        private static List<String> tokens(Node node) {
            if (node == null || node.isNullNode()) {
                return List.of();
            }
            if (node.isArrayNode()) {
                return node.expectArrayNode().getElements().stream().map(V2CodegenSurfaceInventorySource::string)
                           .toList();
            }
            return List.of(string(node));
        }

        private void shapeModifiers(List<InventoryItem> items) {
            String source = location + "/customization.config";
            ObjectNode modifiers = customization.getObjectMember("shapeModifiers").orElse(Node.objectNode());
            modifiers.getMembers().forEach((key, value) -> {
                String shapeName = resolve(key.getValue());
                ObjectNode shape = shapeName == null ? null : shapes.get(shapeName);
                if (shape == null) {
                    return;
                }
                value.expectObjectNode().getArrayMember("modify").ifPresent(modify -> modify.getElements().forEach(
                    element -> element.expectObjectNode().getMembers().forEach((member, change) -> {
                        ObjectNode c = change.isObjectNode() ? change.expectObjectNode() : Node.objectNode();
                        boolean deprecated = deprecated(shapeName);
                        c.getStringMember("emitPropertyName").map(StringNode::getValue).ifPresent(
                            name -> renamedMember(shapeName, shape, member.getValue(), name, source, deprecated,
                                                  items));
                        c.getStringMember("emitEnumName").map(StringNode::getValue).ifPresent(name -> {
                            String type = modelPackage + "." + className(shapeName);
                            items.add(new InventoryItem(EntryKind.fieldId(type, name, "L" + type.replace('.', '/')
                                                                                      + ";"),
                                                        EntryKind.PUBLIC_MEMBER,
                                                        "enum constant " + type + "." + name + " (V2 renames value '"
                                                        + member.getValue() + "')",
                                                        new SourceLocation(artifact, source), deprecated));
                        });
                    })));
            });
        }

        private void renamedMember(String shapeName, ObjectNode shape, String member, String name, String source,
                                   boolean deprecated, List<InventoryItem> items) {
            ObjectNode memberNode = shape.getObjectMember("members").flatMap(m -> m.getObjectMember(member))
                                         .orElse(null);
            String target = memberNode == null ? null : shapeRef(memberNode, "shape").orElse(null);
            ObjectNode targetShape = target == null ? null : shapes.get(resolve(target));
            if (targetShape == null || targetShape.getBooleanMemberOrDefault("streaming", false)) {
                return;
            }
            String descriptor = descriptor(resolve(target), targetShape);
            if (descriptor == null) {
                return;
            }
            String getter = unCapitalize(name);
            if (containsEnum(targetShape, 0)) {
                String type = targetShape.expectStringMember("type").getValue();
                getter += type.equals("list") || type.equals("map") ? "AsStrings" : "AsString";
                if (!type.equals("list") && !type.equals("map")) {
                    descriptor = SIMPLE_TYPES.get("string");
                }
            }
            String owner = modelPackage + "." + className(shapeName);
            items.add(new InventoryItem(EntryKind.methodId(owner, getter, "()" + descriptor), EntryKind.PUBLIC_MEMBER,
                                        "method " + owner + "#" + getter + " (V2 renames member " + shapeName + "."
                                        + member + ")",
                                        new SourceLocation(artifact, source),
                                        deprecated || (memberNode.getBooleanMemberOrDefault("deprecated", false))));
        }

        private void renamedShapes(List<InventoryItem> items) {
            String source = location + "/customization.config";
            customization.getObjectMember("renameShapes").orElse(Node.objectNode()).getMembers()
                         .forEach((original, renamed) -> {
                             String name = string(renamed);
                             if (!shapes.containsKey(name) || inputs.containsKey(name) || outputs.containsKey(name)) {
                                 return;
                             }
                             String type = modelPackage + "." + className(name);
                             items.add(new InventoryItem(EntryKind.typeId(type), EntryKind.PUBLIC_TYPE,
                                                         "type " + type + " (V2 renames shape " + original.getValue()
                                                         + ")",
                                                         new SourceLocation(artifact, source), deprecated(name)));
                         });
        }

        /** The shape {@code name} after renames: itself if it is a (renamed) shape name, else its new name. */
        private String resolve(String name) {
            if (shapes.containsKey(name)) {
                return name;
            }
            return originalNames.entrySet().stream().filter(e -> e.getValue().equals(name)).map(Map.Entry::getKey)
                                .findFirst().orElse(null);
        }

        private boolean deprecated(String shapeName) {
            ObjectNode shape = shapes.get(shapeName);
            String operation = inputs.getOrDefault(shapeName, outputs.get(shapeName));
            return deprecatedShapes.contains(shapeName) || deprecatedShapes.contains(originalNames.get(shapeName))
                   || (shape != null && shape.getBooleanMemberOrDefault("deprecated", false))
                   || (operation != null && deprecatedOperations.contains(operation));
        }

        /** V2's generated class name of a model shape. */
        String className(String shapeName) {
            if (inputs.containsKey(shapeName)) {
                return disambiguate(pascalCase(inputs.get(shapeName)) + "Request", inputs.get(shapeName));
            }
            if (outputs.containsKey(shapeName)) {
                return disambiguate(pascalCase(outputs.get(shapeName)) + "Response", outputs.get(shapeName));
            }
            ObjectNode shape = shapes.get(shapeName);
            if (shape != null && (shape.getBooleanMemberOrDefault("exception", false)
                                  || shape.getBooleanMemberOrDefault("fault", false))) {
                String base = shapeName.endsWith("Fault")
                              ? pascalCase(shapeName.substring(0, shapeName.length() - "Fault".length())) + "Exception"
                              : shapeName.endsWith("Exception") ? pascalCase(shapeName)
                                                                : pascalCase(shapeName) + "Exception";
                return base.equals(serviceName + "Exception") ? "Default" + base : base;
            }
            return capitalize(shapeName);
        }

        private String disambiguate(String name, String operation) {
            return operation.equals(serviceName) ? "Default" + name : name;
        }

        /** The erased V2 getter return descriptor of a member targeting {@code shape}, or {@code null}. */
        private String descriptor(String shapeName, ObjectNode shape) {
            String type = shape.getStringMember("type").map(StringNode::getValue).orElse("");
            if (type.equals("string") && shape.getMember("enum").isPresent()) {
                return "L" + modelPackage.replace('.', '/') + "/" + className(shapeName) + ";";
            }
            if (type.equals("structure")) {
                return "L" + modelPackage.replace('.', '/') + "/" + className(shapeName) + ";";
            }
            return SIMPLE_TYPES.get(type);
        }

        /** V2 {@code Utils.isOrContainsEnumShape}. */
        private boolean containsEnum(ObjectNode shape, int depth) {
            if (shape == null || depth > 8) {
                return false;
            }
            if (shape.getMember("enum").isPresent()) {
                return true;
            }
            String type = shape.getStringMember("type").map(StringNode::getValue).orElse("");
            return switch (type) {
                case "list" -> containsEnum(target(shape, "member"), depth + 1);
                case "map" -> containsEnum(target(shape, "key"), depth + 1)
                              || containsEnum(target(shape, "value"), depth + 1);
                default -> false;
            };
        }

        private ObjectNode target(ObjectNode shape, String member) {
            return shape.getObjectMember(member).flatMap(m -> shapeRef(m, "shape")).map(this::resolve)
                        .map(shapes::get).orElse(null);
        }

        private InventoryItem type(String binaryName, String surface, String source, boolean deprecated) {
            return new InventoryItem(EntryKind.typeId(binaryName), EntryKind.PUBLIC_TYPE, surface,
                                     new SourceLocation(artifact, source), deprecated);
        }
    }

    private static java.util.Optional<String> shapeRef(ObjectNode node, String member) {
        return node.getMember(member).map(n -> n.isObjectNode()
                                                ? n.expectObjectNode().getStringMember("shape")
                                                   .map(StringNode::getValue).orElse(null)
                                                : n.isStringNode() ? n.expectStringNode().getValue() : null);
    }

    private static String string(Node node) {
        return node.expectStringNode().getValue();
    }

    /** V2 {@code DefaultNamingStrategy.getServiceName}: PascalCase service ID without Amazon/AWS/Service. */
    static String serviceName(String serviceId) {
        String name = pascalCase(serviceId);
        name = removeLeading(removeLeading(name, "amazon"), "aws");
        return name.toLowerCase(Locale.ROOT).endsWith("service") ? name.substring(0, name.length() - "service".length())
                                                                 : name;
    }

    /** V2 {@code CodegenNamingUtils.pascalCase}. */
    static String pascalCase(String word) {
        StringBuilder out = new StringBuilder();
        for (String w : splitOnWordBoundaries(word)) {
            if (!w.isEmpty()) {
                String lower = w.toLowerCase(Locale.ROOT);
                out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
        }
        return out.toString();
    }

    /** V2 {@code CodegenNamingUtils.splitOnWordBoundaries}. */
    static String[] splitOnWordBoundaries(String value) {
        String result = value.replaceAll("[^A-Za-z0-9]+", " ");
        result = result.replaceAll("([^a-z]{2,})v([0-9]+)", "$1 v$2 ").replaceAll("([^A-Z]{2,})V([0-9]+)", "$1 V$2 ");
        result = String.join(" ", result.split("(?<=[a-z])(?=[A-Z]([a-zA-Z]|[0-9]))"));
        result = result.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        result = result.replaceAll("([0-9])([a-zA-Z])", "$1 $2");
        return result.replaceAll(" +", " ").trim().split(" ");
    }

    /** V2 {@code Utils.unCapitalize}: lower-cases the first character and a following run of capitals. */
    static String unCapitalize(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        int i = 0;
        do {
            sb.append(Character.toLowerCase(name.charAt(i++)));
        } while (i < name.length() && Character.isUpperCase(name.charAt(i))
                 && !(i < name.length() - 1 && Character.isLowerCase(name.charAt(i + 1))));
        return sb.append(name.substring(i)).toString();
    }

    /** V2 {@code Utils.capitalize}. */
    static String capitalize(String name) {
        return name.length() < 2 ? name.toUpperCase(Locale.ROOT)
                                 : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private static String removeLeading(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
