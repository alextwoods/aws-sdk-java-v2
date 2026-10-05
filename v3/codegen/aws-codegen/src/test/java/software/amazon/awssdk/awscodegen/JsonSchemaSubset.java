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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Validates JSON documents against the JSON Schema (draft 2020-12) subset used by the {@code aws-codegen} resource
 * schemas: {@code type}, {@code properties}, {@code required}, {@code additionalProperties: false}, {@code items},
 * {@code minItems}, {@code minLength}, {@code pattern}, {@code const}, {@code enum} and local {@code $ref}s into
 * {@code $defs}. A schema that uses any other keyword is rejected, so a schema can never silently rely on a keyword
 * this validator ignores.
 */
final class JsonSchemaSubset {

    private static final Set<String> ANNOTATIONS = Set.of("$schema", "$id", "$defs", "title", "description");
    private static final Set<String> KEYWORDS = Set.of("type", "properties", "required", "additionalProperties",
                                                       "items", "minItems", "minLength", "pattern", "const", "enum",
                                                       "$ref");

    private final ObjectNode root;

    private JsonSchemaSubset(ObjectNode root) {
        this.root = root;
        checkKeywords(root, "#");
    }

    /** Loads a schema shipped as a class-path resource of {@code aws-codegen}. */
    static JsonSchemaSubset load(String resource) {
        try (InputStream in = JsonSchemaSubset.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Schema resource " + resource + " is missing");
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Parses a schema document. */
    static JsonSchemaSubset parse(String schema) {
        return new JsonSchemaSubset(Node.parse(schema).expectObjectNode());
    }

    /** Every violation of {@code json}, as {@code <JSON pointer>: <problem>}; empty if it conforms. */
    List<String> validate(String json) {
        List<String> errors = new ArrayList<>();
        validate(root, Node.parse(json), "", errors);
        return errors;
    }

    private void validate(ObjectNode schema, Node value, String pointer, List<String> errors) {
        String at = pointer.isEmpty() ? "/" : pointer;
        schema.getStringMember("$ref").ifPresent(ref -> validate(resolve(ref.getValue()), value, pointer, errors));
        schema.getStringMember("type").ifPresent(type -> {
            if (!hasType(value, type.getValue())) {
                errors.add(at + ": expected " + type.getValue() + " but was " + value.getType());
            }
        });
        schema.getMember("const").ifPresent(expected -> {
            if (!expected.equals(value)) {
                errors.add(at + ": expected constant " + Node.printJson(expected));
            }
        });
        schema.getArrayMember("enum").ifPresent(values -> {
            if (!values.getElements().contains(value)) {
                errors.add(at + ": " + Node.printJson(value) + " is not one of " + Node.printJson(values));
            }
        });
        if (value.isStringNode()) {
            String string = value.expectStringNode().getValue();
            schema.getStringMember("pattern").ifPresent(pattern -> {
                if (!Pattern.compile(pattern.getValue()).matcher(string).find()) {
                    errors.add(at + ": '" + string + "' does not match " + pattern.getValue());
                }
            });
            schema.getNumberMember("minLength").ifPresent(min -> {
                if (string.codePointCount(0, string.length()) < min.getValue().intValue()) {
                    errors.add(at + ": shorter than " + min.getValue());
                }
            });
        }
        if (value.isArrayNode()) {
            ArrayNode array = value.expectArrayNode();
            schema.getNumberMember("minItems").ifPresent(min -> {
                if (array.size() < min.getValue().intValue()) {
                    errors.add(at + ": fewer than " + min.getValue() + " items");
                }
            });
            schema.getObjectMember("items").ifPresent(items -> {
                for (int i = 0; i < array.size(); i++) {
                    validate(items, array.get(i).orElseThrow(), pointer + "/" + i, errors);
                }
            });
        }
        if (value.isObjectNode()) {
            ObjectNode object = value.expectObjectNode();
            ObjectNode properties = schema.getObjectMember("properties").orElse(Node.objectNode());
            schema.getArrayMember("required").ifPresent(required -> required.getElements().forEach(name -> {
                if (!object.containsMember(name.expectStringNode().getValue())) {
                    errors.add(at + ": missing required member " + name.expectStringNode().getValue());
                }
            }));
            boolean closed = schema.getBooleanMember("additionalProperties").map(b -> !b.getValue()).orElse(false);
            for (Map.Entry<StringNode, Node> member : object.getMembers().entrySet()) {
                String name = member.getKey().getValue();
                String child = pointer + "/" + name;
                if (properties.getObjectMember(name).isPresent()) {
                    validate(properties.expectObjectMember(name), member.getValue(), child, errors);
                } else if (closed) {
                    errors.add(child + ": member is not allowed");
                }
            }
        }
    }

    private ObjectNode resolve(String ref) {
        if (!ref.startsWith("#/$defs/")) {
            throw new IllegalArgumentException("Only local $defs references are supported: " + ref);
        }
        return root.expectObjectMember("$defs").expectObjectMember(ref.substring("#/$defs/".length()));
    }

    private static boolean hasType(Node value, String type) {
        return switch (type) {
            case "object" -> value.isObjectNode();
            case "array" -> value.isArrayNode();
            case "string" -> value.isStringNode();
            case "boolean" -> value.isBooleanNode();
            case "null" -> value.isNullNode();
            case "number" -> value.isNumberNode();
            case "integer" -> value.isNumberNode() && value.expectNumberNode().isNaturalNumber();
            default -> throw new IllegalArgumentException("Unsupported schema type " + type);
        };
    }

    /** Rejects keywords outside the supported subset anywhere in the schema. */
    private static void checkKeywords(ObjectNode schema, String pointer) {
        for (Map.Entry<StringNode, Node> member : schema.getMembers().entrySet()) {
            String keyword = member.getKey().getValue();
            String child = pointer + "/" + keyword;
            if (!ANNOTATIONS.contains(keyword) && !KEYWORDS.contains(keyword)) {
                throw new IllegalArgumentException("Unsupported JSON Schema keyword at " + child);
            }
            if (keyword.equals("additionalProperties") && !member.getValue().isBooleanNode()) {
                throw new IllegalArgumentException("additionalProperties must be a boolean at " + child);
            }
            switch (keyword) {
                case "items" -> checkKeywords(member.getValue().expectObjectNode(), child);
                case "properties", "$defs" -> member.getValue().expectObjectNode().getMembers().forEach(
                    (name, sub) -> checkKeywords(sub.expectObjectNode(), child + "/" + name.getValue()));
                default -> {
                }
            }
        }
    }
}
