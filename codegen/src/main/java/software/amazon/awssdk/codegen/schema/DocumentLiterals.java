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

package software.amazon.awssdk.codegen.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Renders a smithy-model {@link Node} as a Java expression of the runtime
 * {@code software.amazon.awssdk.schema.serde.document.Document} (port of smithy-java's {@code NodeWriter}, which renders
 * {@code Node} expressions instead).
 *
 * <p>Mapping (the Dynamic_Client's node-to-document conversion in task 13.1 must use the same one, so converted and
 * generated trait values are equal):
 * <ul>
 *     <li>null → {@code null}; boolean → {@code Document.of(boolean)}; string → {@code Document.of(String)}</li>
 *     <li>integral number → {@code Document.of(int)} when it fits an {@code int}, else {@code Document.of(long)}, else
 *     {@code Document.of(BigInteger)}</li>
 *     <li>{@code BigDecimal} number → {@code Document.of(BigDecimal)}; other number → {@code Document.of(double)}</li>
 *     <li>array → {@code Document.of(List)}; object → {@code Document.of(Map)} in member order</li>
 * </ul>
 */
@SdkInternalApi
final class DocumentLiterals {
    private static final ClassName DOCUMENT = SchemaRuntimeType.DOCUMENT;

    private DocumentLiterals() {
    }

    static CodeBlock render(Node node) {
        if (node == null || node.isNullNode()) {
            return CodeBlock.of("null");
        }
        if (node.isBooleanNode()) {
            return CodeBlock.of("$T.of($L)", DOCUMENT, node.expectBooleanNode().getValue());
        }
        if (node.isStringNode()) {
            return CodeBlock.of("$T.of($S)", DOCUMENT, node.expectStringNode().getValue());
        }
        if (node.isNumberNode()) {
            return number(node.expectNumberNode().getValue());
        }
        if (node.isArrayNode()) {
            return array(node.expectArrayNode());
        }
        return object(node.expectObjectNode());
    }

    /**
     * Renders an unmodifiable-size {@code List} of the given element expressions.
     */
    static CodeBlock list(TypeName elementType, List<CodeBlock> elements) {
        if (elements.isEmpty()) {
            return CodeBlock.of("$T.<$T>emptyList()", Collections.class, elementType);
        }
        if (elements.size() == 1) {
            return CodeBlock.of("$T.<$T>singletonList($L)", Collections.class, elementType, elements.get(0));
        }
        return CodeBlock.of("$T.<$T>asList($L)", Arrays.class, elementType, CodeBlock.join(elements, ", "));
    }

    static CodeBlock list(Class<?> elementType, List<CodeBlock> elements) {
        return list(ClassName.get(elementType), elements);
    }

    private static CodeBlock number(Number value) {
        if (value instanceof BigDecimal) {
            return CodeBlock.of("$T.of(new $T($S))", DOCUMENT, BigDecimal.class, value.toString());
        }
        if (value instanceof BigInteger) {
            BigInteger big = (BigInteger) value;
            if (big.bitLength() >= Long.SIZE) {
                return CodeBlock.of("$T.of(new $T($S))", DOCUMENT, BigInteger.class, big.toString());
            }
            return integral(big.longValue());
        }
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return integral(value.longValue());
        }
        double d = value.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new SchemaCodegenException("Cannot render the non-finite number " + value + " as a document");
        }
        return CodeBlock.of("$T.of($L)", DOCUMENT, Double.toString(d));
    }

    private static CodeBlock integral(long value) {
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return CodeBlock.of("$T.of($L)", DOCUMENT, value);
        }
        return CodeBlock.of("$T.of($LL)", DOCUMENT, value);
    }

    private static CodeBlock array(ArrayNode node) {
        List<CodeBlock> elements = new ArrayList<>(node.size());
        for (Node element : node.getElements()) {
            elements.add(render(element));
        }
        return CodeBlock.of("$T.of($L)", DOCUMENT, list(DOCUMENT, elements));
    }

    private static CodeBlock object(ObjectNode node) {
        Map<String, Node> members = node.getStringMap();
        if (members.isEmpty()) {
            return CodeBlock.of("$T.of($T.<$T, $T>emptyMap())", DOCUMENT, Collections.class, String.class, DOCUMENT);
        }
        if (members.size() == 1) {
            Map.Entry<String, Node> entry = members.entrySet().iterator().next();
            return CodeBlock.of("$T.of($T.<$T, $T>singletonMap($S, $L))", DOCUMENT, Collections.class, String.class,
                                DOCUMENT, entry.getKey(), render(entry.getValue()));
        }
        // An ordered, null-tolerant map as one expression: no anonymous class, no JDK 9+ factory. The cast makes the
        // collect call a standalone expression, so Document.of(Map) is selected among the of(...) overloads.
        List<CodeBlock> pairs = new ArrayList<>(members.size());
        for (Map.Entry<String, Node> entry : members.entrySet()) {
            pairs.add(CodeBlock.of("{$S, $L}", entry.getKey(), render(entry.getValue())));
        }
        return CodeBlock.builder()
                        .add("$T.of(($T<$T, $T>) $T.of(new $T[][] {$L})", DOCUMENT, Map.class, String.class, DOCUMENT,
                             Stream.class, Object.class, CodeBlock.join(pairs, ", "))
                        .add(".collect($T<$T, $T>::new, (m, e) -> m.put(($T) e[0], ($T) e[1]), $T::putAll))",
                             LinkedHashMap.class, String.class, DOCUMENT, String.class, DOCUMENT, Map.class)
                        .build();
    }
}
