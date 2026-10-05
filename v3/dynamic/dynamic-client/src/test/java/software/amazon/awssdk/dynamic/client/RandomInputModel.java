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

package software.amazon.awssdk.dynamic.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A random runtime service model, as a Smithy JSON AST, for {@link DynamicInputValidationPropertyTest}: one awsJson1_0,
 * SigV4 service {@value #NS}#Gen with one operation {@value #OPERATION} whose input structure exercises every scalar
 * type (byte through bigDecimal, intEnum, enum, string, blob, boolean, timestamp, document, and constrained integer and
 * string shapes whose constraints are left to the service), the recursive {@code Tree} structure (through a member and
 * through a list), and random nested structures, unions, and dense and sparse lists and maps. Members are randomly
 * {@code @required}, with or without {@code @default} or {@code @clientOptional}; the {@code token} member is an
 * {@code @idempotencyToken}; and the input structure is randomly an {@code @input} structure or a plain one.
 *
 * <p>The same seed and size always give the same model. Only the AST is produced here: the property loads it through
 * {@code DynamicModel.fromJsonAst}, and documents and the reference validator traverse the loaded Smithy model.
 */
final class RandomInputModel {
    static final String NS = "gen.validation";
    static final ShapeId SERVICE = ShapeId.from(NS + "#Gen");
    static final String OPERATION = "Invoke";
    static final ShapeId TREE = ShapeId.from(NS + "#Tree");
    static final ShapeId TREE_LIST = ShapeId.from(NS + "#TreeList");

    /** Every scalar target; each is one member of the input. */
    static final List<String> SCALARS = List.of(
        "smithy.api#Byte", "smithy.api#Short", "smithy.api#Integer", "smithy.api#Long", "smithy.api#Float",
        "smithy.api#Double", "smithy.api#BigInteger", "smithy.api#BigDecimal", "smithy.api#Boolean",
        "smithy.api#String", "smithy.api#Blob", "smithy.api#Timestamp", "smithy.api#Document",
        NS + "#Color", NS + "#Level", NS + "#Ranged", NS + "#Patterned");

    private final long seed;
    private final int size;
    private final Random random;
    private final Map<String, Node> shapes = new LinkedHashMap<>();
    /** The kind ("structure", "union", "list" or "map") of every aggregate shape, by absolute ID. */
    private final Map<String, String> kinds = new LinkedHashMap<>();
    private final boolean inputTrait;
    private final String jsonAst;

    private RandomInputModel(long seed, int size) {
        this.seed = seed;
        this.size = size;
        this.random = new Random(seed);
        this.inputTrait = random.nextBoolean();
        build();
        this.jsonAst = Node.prettyPrintJson(Node.objectNodeBuilder()
                                                .withMember("smithy", "2.0")
                                                .withMember("shapes", ObjectNode.objectNode(toNodes(shapes)))
                                                .build());
    }

    /** The model of {@code seed} with {@code 2 + size} random aggregate shapes. */
    static RandomInputModel generate(long seed, int size) {
        return new RandomInputModel(seed, size);
    }

    long seed() {
        return seed;
    }

    String jsonAst() {
        return jsonAst;
    }

    /** Whether the input structure has the {@code @input} trait, which makes its members client optional. */
    boolean inputTrait() {
        return inputTrait;
    }

    ShapeId inputId() {
        return ShapeId.from(NS + (inputTrait ? "#InvokeInput" : "#InvokeRequest"));
    }

    @Override
    public String toString() {
        return "RandomInputModel[seed=" + seed + ", size=" + size + ", @input=" + inputTrait + "]";
    }

    private void build() {
        shapes.put(SERVICE.toString(), Node.objectNodeBuilder()
            .withMember("type", "service")
            .withMember("version", "2024-01-01")
            .withMember("operations", Node.arrayNode(target(NS + "#" + OPERATION)))
            .withMember("traits", Node.objectNodeBuilder()
                .withMember("aws.protocols#awsJson1_0", Node.objectNode())
                .withMember("aws.auth#sigv4", Node.objectNode().withMember("name", "gen"))
                .withMember("aws.api#service", Node.objectNode().withMember("sdkId", "Gen"))
                .build())
            .build());
        shapes.put(NS + "#" + OPERATION, Node.objectNodeBuilder()
            .withMember("type", "operation")
            .withMember("input", target(inputId().toString()))
            .withMember("output", target(NS + "#InvokeOutput"))
            .build());
        Map<String, Node> output = new LinkedHashMap<>();
        output.put("echo", member("smithy.api#String", Node.objectNode()));
        shapes.put(NS + "#InvokeOutput", aggregate("structure", output, traits("smithy.api#output", Node.objectNode())));

        shapes.put(NS + "#Color", enumShape("enum", Map.of("RED", Node.from("RED"), "GREEN", Node.from("GREEN"))));
        shapes.put(NS + "#Level", enumShape("intEnum", Map.of("LOW", Node.from(1), "HIGH", Node.from(10))));
        shapes.put(NS + "#Ranged", Node.objectNodeBuilder()
            .withMember("type", "integer")
            .withMember("traits", traits("smithy.api#range",
                                         Node.objectNode().withMember("min", 0).withMember("max", 10)))
            .build());
        shapes.put(NS + "#Patterned", Node.objectNodeBuilder()
            .withMember("type", "string")
            .withMember("traits", Node.objectNodeBuilder()
                .withMember("smithy.api#length", Node.objectNode().withMember("min", 1).withMember("max", 3))
                .withMember("smithy.api#pattern", "^[a-z]+$")
                .build())
            .build());

        // The recursive structure, through a member and through a list.
        Map<String, Node> tree = new LinkedHashMap<>();
        tree.put("value", member("smithy.api#Integer", traits("smithy.api#required", Node.objectNode())));
        tree.put("label", member("smithy.api#String", Node.objectNode()));
        tree.put("child", member(TREE.toString(), Node.objectNode()));
        tree.put("children", member(TREE_LIST.toString(), Node.objectNode()));
        shapes.put(TREE.toString(), aggregate("structure", tree, null));
        kinds.put(TREE.toString(), "structure");
        shapes.put(TREE_LIST.toString(), Node.objectNodeBuilder().withMember("type", "list")
                                             .withMember("member", target(TREE.toString())).build());
        kinds.put(TREE_LIST.toString(), "list");

        // Random aggregates, each targeting only scalars and earlier aggregates.
        List<String> generated = new ArrayList<>();
        int count = 2 + size;
        for (int i = 0; i < count; i++) {
            String id;
            switch (random.nextInt(6)) {
                case 0 -> {
                    id = NS + "#Struct" + i;
                    Map<String, Node> members = new LinkedHashMap<>();
                    int n = 1 + random.nextInt(4);
                    for (int m = 0; m < n; m++) {
                        String target = pickTarget();
                        members.put("m" + m, member(target, memberTraits(target)));
                    }
                    shapes.put(id, aggregate("structure", members, null));
                    kinds.put(id, "structure");
                }
                case 1 -> {
                    id = NS + "#Union" + i;
                    Map<String, Node> members = new LinkedHashMap<>();
                    int n = 1 + random.nextInt(3);
                    for (int m = 0; m < n; m++) {
                        members.put("u" + m, member(pickTarget(), Node.objectNode()));
                    }
                    shapes.put(id, aggregate("union", members, null));
                    kinds.put(id, "union");
                }
                case 2, 3 -> {
                    boolean sparse = random.nextBoolean();
                    id = NS + (sparse ? "#SparseList" : "#List") + i;
                    ObjectNode.Builder list = Node.objectNodeBuilder().withMember("type", "list")
                                                  .withMember("member", target(pickTarget()));
                    shapes.put(id, sparse(list, sparse));
                    kinds.put(id, "list");
                }
                default -> {
                    boolean sparse = random.nextBoolean();
                    id = NS + (sparse ? "#SparseMap" : "#Map") + i;
                    ObjectNode.Builder map = Node.objectNodeBuilder().withMember("type", "map")
                                                 .withMember("key", target("smithy.api#String"))
                                                 .withMember("value", target(pickTarget()));
                    shapes.put(id, sparse(map, sparse));
                    kinds.put(id, "map");
                }
            }
            generated.add(id);
        }

        // The input: one member per scalar, the recursive tree, every generated aggregate and an idempotency token.
        Map<String, Node> input = new LinkedHashMap<>();
        for (String scalar : SCALARS) {
            input.put("f" + ShapeId.from(scalar).getName(), member(scalar, memberTraits(scalar)));
        }
        input.put("tree", member(TREE.toString(), memberTraits(TREE.toString())));
        for (int i = 0; i < generated.size(); i++) {
            input.put("a" + i, member(generated.get(i), memberTraits(generated.get(i))));
        }
        ObjectNode.Builder token = Node.objectNodeBuilder().withMember("smithy.api#idempotencyToken", Node.objectNode());
        if (random.nextBoolean()) {
            token.withMember("smithy.api#required", Node.objectNode());
        }
        input.put("token", member("smithy.api#String", token.build()));
        shapes.put(inputId().toString(), aggregate("structure", input,
                                                   inputTrait ? traits("smithy.api#input", Node.objectNode()) : null));
    }

    /** A scalar (55%) or an already defined aggregate (45%). */
    private String pickTarget() {
        if (random.nextDouble() < 0.45) {
            List<String> aggregates = new ArrayList<>(kinds.keySet());
            return aggregates.get(random.nextInt(aggregates.size()));
        }
        return SCALARS.get(random.nextInt(SCALARS.size()));
    }

    /** Random structure member traits: optional, or required alone, with a default or client optional. */
    private ObjectNode memberTraits(String target) {
        if (random.nextDouble() >= 0.4) {
            return Node.objectNode();
        }
        ObjectNode.Builder traits = Node.objectNodeBuilder().withMember("smithy.api#required", Node.objectNode());
        double variant = random.nextDouble();
        Node defaultValue = defaultValue(target);
        if (variant < 0.3 && defaultValue != null) {
            traits.withMember("smithy.api#default", defaultValue);
        } else if (variant < 0.55) {
            traits.withMember("smithy.api#clientOptional", Node.objectNode());
        }
        return traits.build();
    }

    /** A valid {@code @default} value of {@code target}, or {@code null} if this generator gives it none. */
    private Node defaultValue(String target) {
        String kind = kinds.get(target);
        if ("list".equals(kind)) {
            return Node.arrayNode();
        }
        if ("map".equals(kind)) {
            return Node.objectNode();
        }
        if (kind != null) {
            return null;
        }
        return switch (ShapeId.from(target).getName()) {
            case "String" -> Node.from("d");
            case "Boolean" -> Node.from(false);
            case "Byte", "Short", "Integer", "Long", "Float", "Double", "BigInteger", "BigDecimal", "Ranged" ->
                Node.from(0);
            case "Color" -> Node.from("RED");
            case "Level" -> Node.from(1);
            default -> null;
        };
    }

    private static ObjectNode target(String id) {
        return Node.objectNode().withMember("target", id);
    }

    private static ObjectNode member(String target, ObjectNode traits) {
        ObjectNode member = target(target);
        return traits.isEmpty() ? member : member.withMember("traits", traits);
    }

    private static ObjectNode traits(String id, Node value) {
        return Node.objectNode().withMember(id, value);
    }

    private static ObjectNode aggregate(String type, Map<String, Node> members, ObjectNode traits) {
        ObjectNode.Builder shape = Node.objectNodeBuilder().withMember("type", type)
                                       .withMember("members", ObjectNode.objectNode(toNodes(members)));
        if (traits != null) {
            shape.withMember("traits", traits);
        }
        return shape.build();
    }

    private static ObjectNode enumShape(String type, Map<String, Node> values) {
        Map<String, Node> members = new LinkedHashMap<>();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(
            e -> members.put(e.getKey(), member("smithy.api#Unit", traits("smithy.api#enumValue", e.getValue()))));
        return aggregate(type, members, null);
    }

    private static ObjectNode sparse(ObjectNode.Builder collection, boolean sparse) {
        if (sparse) {
            collection.withMember("traits", traits("smithy.api#sparse", Node.objectNode()));
        }
        return collection.build();
    }

    private static Map<StringNode, Node> toNodes(Map<String, Node> members) {
        Map<StringNode, Node> nodes = new LinkedHashMap<>();
        members.forEach((name, node) -> nodes.put(Node.from(name), node));
        return nodes;
    }
}
