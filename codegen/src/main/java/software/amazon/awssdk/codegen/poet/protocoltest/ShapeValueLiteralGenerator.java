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

package software.amazon.awssdk.codegen.poet.protocoltest;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Turns test case {@code params} into Java expressions that build SDK values, by walking the {@link Node} against the
 * Smithy shape it describes (Requirement 41.3: expected values never come from the SDK's own deserializer).
 *
 * <p>Value rules, which follow smithy-java's {@code ProtocolTestDocument}:
 * <ul>
 *     <li>blob: the UTF-8 bytes of the string, as {@link SdkBytes#fromUtf8String}</li>
 *     <li>timestamp: a number is epoch seconds (a fraction is kept to the nanosecond); a string is ISO-8601 parsed with
 *     {@link Instant#parse}</li>
 *     <li>float and double: a number, or one of the strings {@code NaN}, {@code Infinity} and {@code -Infinity}</li>
 *     <li>enum: the wire string, through the member's string setter; int enum: the integer</li>
 *     <li>document: the {@link Document} built from the node, with numbers kept in their source form</li>
 *     <li>list, map: {@code Arrays.<T>asList} and {@code ProtocolTestValues.<V>mapOf} with explicit element types, so
 *     sparse {@code null} elements and nested empty collections type-check</li>
 *     <li>structure, union: the generated builder; a {@code null} member value leaves the member unset</li>
 * </ul>
 *
 * Anything else (a member the generated class does not have, a node of the wrong kind, an integral value out of range)
 * makes the case ungeneratable.
 */
@SdkInternalApi
final class ShapeValueLiteralGenerator {
    private final Model model;
    private final String modelPackage;
    private final Map<ShapeId, ShapeModel> nestedShapes = new HashMap<>();

    ShapeValueLiteralGenerator(IntermediateModel intermediateModel) {
        this.model = intermediateModel.smithyModel();
        this.modelPackage = intermediateModel.getMetadata().getFullModelPackageName();
        for (ShapeModel shape : intermediateModel.getShapes().values()) {
            ShapeId id = shape.shapeId();
            if (id == null) {
                continue;
            }
            ShapeModel existing = nestedShapes.get(id);
            // A structure that is also an operation input or output has a Request or Response class; a member that
            // targets it uses its model class.
            if (existing == null || isTopLevel(existing) && !isTopLevel(shape)) {
                nestedShapes.put(id, shape);
            }
        }
    }

    private static boolean isTopLevel(ShapeModel shape) {
        return shape.getShapeType() == ShapeType.Request || shape.getShapeType() == ShapeType.Response;
    }

    /**
     * The generated class of {@code shape}.
     */
    ClassName modelClass(ShapeModel shape) {
        return ClassName.get(modelPackage, shape.getShapeName());
    }

    /**
     * The Smithy member name of the streaming payload member of {@code shapeModel}, if it has one. Generated request and
     * response classes do not hold that member: the client method takes or returns the stream.
     */
    static Optional<String> streamingMemberName(ShapeModel shapeModel) {
        for (MemberModel member : shapeModel.getMembers()) {
            if (isStreaming(member) && member.memberId() != null) {
                return member.memberId().getMember();
            }
        }
        return Optional.empty();
    }

    private static boolean isStreaming(MemberModel member) {
        return member.getHttp() != null && member.getHttp().getIsStreaming();
    }

    /**
     * An expression that builds {@code shapeModel} from {@code params}.
     *
     * @param smithyShape the Smithy structure or union {@code shapeModel} was built from
     * @param exception   whether the class is a modeled exception, whose {@code message} member is set through the
     *                    exception builder's {@code message} method
     * @param streamArgument whether the client method takes or returns the streaming payload member, so its value is
     *                    left out of the builder; when false, a value for a streaming member makes the case ungeneratable
     * @param path        where {@code params} sits in the test case, for error messages
     */
    CodeBlock structure(ShapeModel shapeModel, Shape smithyShape, ObjectNode params, boolean exception,
                        boolean streamArgument, String path) {
        ClassName type = modelClass(shapeModel);
        CodeBlock.Builder code = CodeBlock.builder().add("$T.builder()", type);
        boolean any = false;
        for (Map.Entry<StringNode, Node> entry : params.getMembers().entrySet()) {
            String name = entry.getKey().getValue();
            Node value = entry.getValue();
            String memberPath = path + "." + name;
            Optional<MemberShape> member = smithyShape.getMember(name);
            if (!member.isPresent()) {
                throw new UngeneratableCaseException(memberPath + " is not a member of " + smithyShape.getId());
            }
            if (value.isNullNode()) {
                continue;
            }
            Optional<MemberModel> memberModel = memberModel(shapeModel, member.get());
            if (memberModel.isPresent() && isStreaming(memberModel.get())) {
                if (!streamArgument) {
                    throw new UngeneratableCaseException(memberPath + " is a streaming member, which the generated "
                                                         + "client method neither takes nor returns");
                }
                continue;
            }
            String setter = setterName(shapeModel, memberModel, member.get(), exception, memberPath);
            if (!any) {
                code.add("$>$>");
                any = true;
            }
            code.add("\n.$N($L)", setter, value(member.get(), value, memberPath));
        }
        code.add(any ? "\n.build()$<$<" : ".build()");
        return code.build();
    }

    private static Optional<MemberModel> memberModel(ShapeModel shapeModel, MemberShape member) {
        for (MemberModel memberModel : shapeModel.getMembers()) {
            if (member.getId().equals(memberModel.memberId())) {
                return Optional.of(memberModel);
            }
        }
        return Optional.empty();
    }

    private static String setterName(ShapeModel shapeModel, Optional<MemberModel> memberModel, MemberShape member,
                                     boolean exception, String path) {
        if (memberModel.isPresent()) {
            return memberModel.get().getFluentSetterMethodName();
        }
        // The exception message is not a generated member; AwsServiceException.Builder#message sets it.
        if (exception && "message".equalsIgnoreCase(member.getMemberName())) {
            return "message";
        }
        throw new UngeneratableCaseException(path + " has no member in generated class " + shapeModel.getShapeName());
    }

    /**
     * An expression of {@link #javaType} of the member's target that holds {@code node}.
     */
    CodeBlock value(MemberShape member, Node node, String path) {
        return value(model.expectShape(member.getTarget()), node, path);
    }

    private CodeBlock value(Shape target, Node node, String path) {
        if (node.isNullNode()) {
            return CodeBlock.of("null");
        }
        switch (target.getType()) {
            case BLOB:
                return CodeBlock.of("$T.fromUtf8String($S)", SdkBytes.class, expectString(node, target, path));
            case BOOLEAN:
                if (!node.isBooleanNode()) {
                    throw mismatch(node, target, path);
                }
                return CodeBlock.of("$L", node.expectBooleanNode().getValue());
            case STRING:
            case ENUM:
                return CodeBlock.of("$S", expectString(node, target, path));
            case TIMESTAMP:
                return timestamp(node, target, path);
            case BYTE:
                return CodeBlock.of("(byte) $L", integral(node, target, path, Byte.MIN_VALUE, Byte.MAX_VALUE));
            case SHORT:
                return CodeBlock.of("(short) $L", integral(node, target, path, Short.MIN_VALUE, Short.MAX_VALUE));
            case INTEGER:
            case INT_ENUM:
                return CodeBlock.of("$L", integral(node, target, path, Integer.MIN_VALUE, Integer.MAX_VALUE));
            case LONG:
                return CodeBlock.of("$LL", integral(node, target, path, Long.MIN_VALUE, Long.MAX_VALUE));
            case FLOAT:
                return floatValue(node, target, path);
            case DOUBLE:
                return doubleValue(node, target, path);
            case BIG_INTEGER:
                return CodeBlock.of("new $T($S)", BigInteger.class, integralValue(node, target, path).toString());
            case BIG_DECIMAL:
                return CodeBlock.of("new $T($S)", BigDecimal.class, decimal(node, target, path).toString());
            case DOCUMENT:
                return document(node);
            case LIST:
            case SET:
                return list((CollectionShape) target, node, path);
            case MAP:
                return map((MapShape) target, node, path);
            case STRUCTURE:
            case UNION:
                if (!node.isObjectNode()) {
                    throw mismatch(node, target, path);
                }
                return structure(nestedShape(target, path), target, node.expectObjectNode(), false, false, path);
            default:
                throw new UngeneratableCaseException(path + " targets " + target.getId() + ", a " + target.getType()
                                                     + " shape, which has no value literal");
        }
    }

    private ShapeModel nestedShape(Shape target, String path) {
        ShapeModel shape = nestedShapes.get(target.getId());
        if (shape == null) {
            throw new UngeneratableCaseException(path + " targets " + target.getId() + ", which has no generated class");
        }
        return shape;
    }

    /**
     * The Java type generated builders take for a value of {@code target}: boxed simple types, {@code String} for
     * enums, {@code List} and {@code Map<String, V>} for collections, and the generated class for structures and unions.
     */
    TypeName javaType(Shape target, String path) {
        switch (target.getType()) {
            case BLOB:
                return ClassName.get(SdkBytes.class);
            case BOOLEAN:
                return ClassName.get(Boolean.class);
            case STRING:
            case ENUM:
                return ClassName.get(String.class);
            case TIMESTAMP:
                return ClassName.get(Instant.class);
            case BYTE:
                return ClassName.get(Byte.class);
            case SHORT:
                return ClassName.get(Short.class);
            case INTEGER:
            case INT_ENUM:
                return ClassName.get(Integer.class);
            case LONG:
                return ClassName.get(Long.class);
            case FLOAT:
                return ClassName.get(Float.class);
            case DOUBLE:
                return ClassName.get(Double.class);
            case BIG_INTEGER:
                return ClassName.get(BigInteger.class);
            case BIG_DECIMAL:
                return ClassName.get(BigDecimal.class);
            case DOCUMENT:
                return ClassName.get(Document.class);
            case LIST:
            case SET:
                return ParameterizedTypeName.get(ClassName.get(List.class),
                                                 javaType(memberTarget(((CollectionShape) target).getMember()), path));
            case MAP:
                return ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                                                 javaType(memberTarget(((MapShape) target).getValue()), path));
            case STRUCTURE:
            case UNION:
                return modelClass(nestedShape(target, path));
            default:
                throw new UngeneratableCaseException(path + " targets " + target.getId() + ", a " + target.getType()
                                                     + " shape, which has no Java value type");
        }
    }

    private Shape memberTarget(MemberShape member) {
        return model.expectShape(member.getTarget());
    }

    private CodeBlock list(CollectionShape target, Node node, String path) {
        if (!node.isArrayNode()) {
            throw mismatch(node, target, path);
        }
        Shape elementShape = memberTarget(target.getMember());
        TypeName elementType = javaType(elementShape, path);
        ArrayNode array = node.expectArrayNode();
        if (array.isEmpty()) {
            return CodeBlock.of("$T.<$T>emptyList()", Collections.class, elementType);
        }
        CodeBlock.Builder code = CodeBlock.builder().add("$T.<$T>asList(", Arrays.class, elementType);
        List<Node> elements = array.getElements();
        for (int i = 0; i < elements.size(); i++) {
            // A sparse list's null element is cast to the element type: a lone uncast null would be passed to asList
            // as the varargs array itself.
            Node element = elements.get(i);
            CodeBlock value = element.isNullNode() ? CodeBlock.of("($T) null", elementType)
                                                   : value(elementShape, element, path + "[" + i + "]");
            code.add(i == 0 ? "$L" : ", $L", value);
        }
        return code.add(")").build();
    }

    private CodeBlock map(MapShape target, Node node, String path) {
        if (!node.isObjectNode()) {
            throw mismatch(node, target, path);
        }
        Shape valueShape = memberTarget(target.getValue());
        CodeBlock.Builder code = CodeBlock.builder().add("$T.<$T>mapOf(", ProtocolTestRuntimeTypes.PROTOCOL_TEST_VALUES,
                                                         javaType(valueShape, path));
        boolean first = true;
        for (Map.Entry<StringNode, Node> entry : node.expectObjectNode().getMembers().entrySet()) {
            String key = entry.getKey().getValue();
            code.add(first ? "$S, $L" : ", $S, $L", key, value(valueShape, entry.getValue(), path + "[" + key + "]"));
            first = false;
        }
        return code.add(")").build();
    }

    private static CodeBlock document(Node node) {
        switch (node.getType()) {
            case NULL:
                return CodeBlock.of("$T.fromNull()", Document.class);
            case BOOLEAN:
                return CodeBlock.of("$T.fromBoolean($L)", Document.class, node.expectBooleanNode().getValue());
            case STRING:
                return CodeBlock.of("$T.fromString($S)", Document.class, node.expectStringNode().getValue());
            case NUMBER:
                return CodeBlock.of("$T.fromNumber($S)", Document.class, numberText(node.expectNumberNode()));
            case ARRAY: {
                List<Node> elements = node.expectArrayNode().getElements();
                if (elements.isEmpty()) {
                    return CodeBlock.of("$T.fromList($T.<$T>emptyList())", Document.class, Collections.class,
                                        Document.class);
                }
                CodeBlock.Builder code = CodeBlock.builder().add("$T.fromList($T.<$T>asList(", Document.class,
                                                                 Arrays.class, Document.class);
                for (int i = 0; i < elements.size(); i++) {
                    code.add(i == 0 ? "$L" : ", $L", document(elements.get(i)));
                }
                return code.add("))").build();
            }
            case OBJECT: {
                CodeBlock.Builder code = CodeBlock.builder().add("$T.fromMap($T.<$T>mapOf(", Document.class,
                                                                 ProtocolTestRuntimeTypes.PROTOCOL_TEST_VALUES,
                                                                 Document.class);
                boolean first = true;
                for (Map.Entry<StringNode, Node> entry : node.expectObjectNode().getMembers().entrySet()) {
                    code.add(first ? "$S, $L" : ", $S, $L", entry.getKey().getValue(), document(entry.getValue()));
                    first = false;
                }
                return code.add("))").build();
            }
            default:
                throw new IllegalStateException("Unknown node type " + node.getType());
        }
    }

    private static String numberText(NumberNode number) {
        Optional<BigDecimal> exact = number.asBigDecimal();
        return exact.map(BigDecimal::toString).orElseGet(() -> number.getValue().toString());
    }

    private static CodeBlock timestamp(Node node, Shape target, String path) {
        if (node.isStringNode()) {
            String text = node.expectStringNode().getValue();
            try {
                Instant.parse(text);
            } catch (RuntimeException e) {
                throw new UngeneratableCaseException(path + " is not an ISO-8601 timestamp: " + text);
            }
            return CodeBlock.of("$T.parse($S)", Instant.class, text);
        }
        BigDecimal value = decimal(node, target, path);
        BigDecimal seconds = value.setScale(0, RoundingMode.FLOOR);
        long nanos = value.subtract(seconds).movePointRight(9).setScale(0, RoundingMode.DOWN).longValueExact();
        long epochSeconds;
        try {
            epochSeconds = seconds.longValueExact();
        } catch (ArithmeticException e) {
            throw new UngeneratableCaseException(path + " is out of the range of a timestamp: " + value);
        }
        if (nanos == 0) {
            return CodeBlock.of("$T.ofEpochSecond($LL)", Instant.class, epochSeconds);
        }
        return CodeBlock.of("$T.ofEpochSecond($LL, $LL)", Instant.class, epochSeconds, nanos);
    }

    private static CodeBlock floatValue(Node node, Shape target, String path) {
        Optional<String> special = specialFloatingPoint(node);
        if (special.isPresent()) {
            return CodeBlock.of("$T.$L", Float.class, special.get());
        }
        float value = decimal(node, target, path).floatValue();
        if (Float.isInfinite(value)) {
            throw new UngeneratableCaseException(path + " is out of the range of a float");
        }
        return CodeBlock.of("$LF", Float.toString(value));
    }

    private static CodeBlock doubleValue(Node node, Shape target, String path) {
        Optional<String> special = specialFloatingPoint(node);
        if (special.isPresent()) {
            return CodeBlock.of("$T.$L", Double.class, special.get());
        }
        double value = decimal(node, target, path).doubleValue();
        if (Double.isInfinite(value)) {
            throw new UngeneratableCaseException(path + " is out of the range of a double");
        }
        return CodeBlock.of("$LD", Double.toString(value));
    }

    /**
     * The constant name for the Smithy string forms of non-finite numbers.
     */
    private static Optional<String> specialFloatingPoint(Node node) {
        if (node.isStringNode()) {
            switch (node.expectStringNode().getValue()) {
                case "NaN":
                    return Optional.of("NaN");
                case "Infinity":
                    return Optional.of("POSITIVE_INFINITY");
                case "-Infinity":
                    return Optional.of("NEGATIVE_INFINITY");
                default:
                    return Optional.empty();
            }
        }
        if (node.isNumberNode()) {
            NumberNode number = node.expectNumberNode();
            if (number.isNaN()) {
                return Optional.of("NaN");
            }
            if (number.isInfinite()) {
                return Optional.of(number.isNegative() ? "NEGATIVE_INFINITY" : "POSITIVE_INFINITY");
            }
        }
        return Optional.empty();
    }

    private static String integral(Node node, Shape target, String path, long min, long max) {
        BigInteger value = integralValue(node, target, path);
        if (value.compareTo(BigInteger.valueOf(min)) < 0 || value.compareTo(BigInteger.valueOf(max)) > 0) {
            throw new UngeneratableCaseException(path + " is out of the range of " + target.getId() + ": " + value);
        }
        return value.toString();
    }

    private static BigInteger integralValue(Node node, Shape target, String path) {
        BigDecimal value = decimal(node, target, path);
        try {
            return value.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new UngeneratableCaseException(path + " is not an integer: " + value);
        }
    }

    private static BigDecimal decimal(Node node, Shape target, String path) {
        if (!node.isNumberNode()) {
            throw mismatch(node, target, path);
        }
        NumberNode number = node.expectNumberNode();
        return number.asBigDecimal()
                     .orElseThrow(() -> new UngeneratableCaseException(path + " is not a finite number: " + number));
    }

    private static String expectString(Node node, Shape target, String path) {
        if (!node.isStringNode()) {
            throw mismatch(node, target, path);
        }
        return node.expectStringNode().getValue();
    }

    private static UngeneratableCaseException mismatch(Node node, Shape target, String path) {
        return new UngeneratableCaseException(path + " is a " + node.getType() + " value, which does not match "
                                              + target.getId() + " (" + target.getType() + ")");
    }
}
