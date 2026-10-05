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

package software.amazon.awssdk.awscodegen.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Builds a {@link SchemaFilePlan.Code} fragment: literal text, type references written through {@code $T}, and the
 * Java expressions that recreate shape IDs, Smithy nodes and traits at run time without parsing.
 *
 * <p>Every literal is written in ASCII: string literals escape quotes, backslashes and control characters with
 * character or octal escapes, and every non-ASCII character with a Unicode escape (never a line terminator, quote or
 * backslash, which Java would process before lexing). Numbers keep their exact Java type and value.
 */
final class JavaCode {

    private final StringBuilder template = new StringBuilder();
    private final List<Symbol> types = new ArrayList<>();

    /** Appends literal Java text; {@code $} is escaped for the writer. */
    JavaCode text(String text) {
        template.append(text.replace("$", "$$"));
        return this;
    }

    /** Appends a reference to {@code type}, imported by the writer. */
    JavaCode type(Symbol type) {
        template.append("$T");
        types.add(type);
        return this;
    }

    /** Appends another fragment. */
    JavaCode code(SchemaFilePlan.Code code) {
        template.append(code.template());
        types.addAll(code.types());
        return this;
    }

    /** Appends a Java string literal of {@code value}; see {@link SchemaSymbols#javaString}. */
    JavaCode string(String value) {
        return text(stringLiteral(value));
    }

    /** Appends {@code ShapeId.from("<id>")}. */
    JavaCode shapeId(ShapeId id) {
        return type(SchemaRuntimeSymbols.SHAPE_ID).text(".from(").string(id.toString()).text(")");
    }

    /** Appends an expression that creates exactly {@code node}. */
    JavaCode node(Node node) {
        if (node instanceof NullNode) {
            return type(SchemaRuntimeSymbols.NODE).text(".nullNode()");
        } else if (node instanceof BooleanNode bool) {
            return type(SchemaRuntimeSymbols.NODE).text(".from(" + bool.getValue() + ")");
        } else if (node instanceof StringNode string) {
            return type(SchemaRuntimeSymbols.NODE).text(".from(").string(string.getValue()).text(")");
        } else if (node instanceof NumberNode number) {
            type(SchemaRuntimeSymbols.NODE).text(".from(");
            return number(number.getValue()).text(")");
        } else if (node instanceof ArrayNode array) {
            if (array.isEmpty()) {
                return type(SchemaRuntimeSymbols.NODE).text(".arrayNode()");
            }
            type(SchemaRuntimeSymbols.NODE).text(".fromNodes(");
            String separator = "";
            for (Node element : array.getElements()) {
                text(separator).node(element);
                separator = ", ";
            }
            return text(")");
        } else if (node instanceof ObjectNode object) {
            if (object.isEmpty()) {
                return type(SchemaRuntimeSymbols.NODE).text(".objectNode()");
            }
            type(SchemaRuntimeSymbols.NODE).text(".objectNodeBuilder()");
            for (Map.Entry<StringNode, Node> member : object.getMembers().entrySet()) {
                text(".withMember(").string(member.getKey().getValue()).text(", ").node(member.getValue()).text(")");
            }
            return text(".build()");
        }
        throw new AwsCodegenException("Unsupported Smithy node " + node.getClass().getName());
    }

    /**
     * Appends an expression that creates {@code trait} as it applies to {@code target}: an unknown trait as a
     * {@code DynamicTrait} with its exact ID and value, an annotation trait with its no-argument constructor, and any
     * other trait with its {@code TraitService} provider from its exact node value.
     */
    JavaCode trait(Trait trait, ShapeId target) {
        if (trait instanceof DynamicTrait) {
            text("new ").type(SchemaRuntimeSymbols.DYNAMIC_TRAIT).text("(").shapeId(trait.toShapeId()).text(", ");
            return node(trait.toNode()).text(")");
        }
        Symbol type = SchemaRuntimeSymbols.type(trait.getClass());
        if (trait instanceof AnnotationTrait && trait.toNode().expectObjectNode().isEmpty()) {
            return text("new ").type(type).text("()");
        }
        text("new ").type(type).text(".Provider().createTrait(").shapeId(target).text(", ");
        return node(trait.toNode()).text(")");
    }

    SchemaFilePlan.Code build() {
        return new SchemaFilePlan.Code(template.toString(), types);
    }

    private JavaCode number(Number value) {
        if (value instanceof Long l) {
            return text(l + "L");
        } else if (value instanceof Integer i) {
            return text(i.toString());
        } else if (value instanceof Short s) {
            return text("(short) " + s);
        } else if (value instanceof Byte b) {
            return text("(byte) " + b);
        } else if (value instanceof Double d) {
            return text(floatingLiteral(d, Double.toString(d), "Double", "d"));
        } else if (value instanceof Float f) {
            return text(floatingLiteral(f, Float.toString(f), "Float", "f"));
        } else if (value instanceof BigInteger integer) {
            return text("new ").type(SchemaRuntimeSymbols.BIG_INTEGER).text("(").string(integer.toString()).text(")");
        } else if (value instanceof BigDecimal decimal) {
            return text("new ").type(SchemaRuntimeSymbols.BIG_DECIMAL).text("(").string(decimal.toString()).text(")");
        }
        throw new AwsCodegenException("Unsupported Smithy number type " + value.getClass().getName());
    }

    private static String floatingLiteral(double value, String decimal, String type, String suffix) {
        if (Double.isNaN(value)) {
            return type + ".NaN";
        } else if (Double.isInfinite(value)) {
            return type + (value > 0 ? ".POSITIVE_INFINITY" : ".NEGATIVE_INFINITY");
        }
        return decimal + suffix;
    }

    /** A Java string literal of {@code value}, in ASCII. */
    static String stringLiteral(String value) {
        StringBuilder literal = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> literal.append("\\\"");
                case '\\' -> literal.append("\\\\");
                case '\n' -> literal.append("\\n");
                case '\r' -> literal.append("\\r");
                case '\t' -> literal.append("\\t");
                case '\b' -> literal.append("\\b");
                case '\f' -> literal.append("\\f");
                default -> {
                    if (c < 0x20 || c == 0x7F) {
                        literal.append(String.format(Locale.ROOT, "\\%03o", (int) c));
                    } else if (c > 0x7E) {
                        literal.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        literal.append(c);
                    }
                }
            }
        }
        return literal.append('"').toString();
    }
}
