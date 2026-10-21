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

package software.amazon.awssdk.dynamicclient.schema.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.TraitFactories;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Conversions from smithy-model values to their SDK runtime forms: shape IDs, trait values and traits.
 */
@SdkInternalApi
public final class ModelConversions {
    private ModelConversions() {
    }

    /**
     * Converts a smithy-model shape ID to an SDK shape ID, without parsing it again.
     *
     * @param id the smithy-model shape ID
     * @return the SDK shape ID
     */
    public static software.amazon.awssdk.schema.ShapeId shapeId(ShapeId id) {
        Validate.paramNotNull(id, "id");
        return id.hasMember()
               ? software.amazon.awssdk.schema.ShapeId.fromParts(id.getNamespace(), id.getName(), id.getMember().get())
               : software.amazon.awssdk.schema.ShapeId.fromParts(id.getNamespace(), id.getName());
    }

    /**
     * Converts an SDK shape ID to a smithy-model shape ID.
     *
     * @param id the SDK shape ID
     * @return the smithy-model shape ID
     */
    public static ShapeId smithyShapeId(software.amazon.awssdk.schema.ShapeId id) {
        Validate.paramNotNull(id, "id");
        return id.hasMember()
               ? ShapeId.fromParts(id.getNamespace(), id.getName(), id.getMember().get())
               : ShapeId.fromParts(id.getNamespace(), id.getName());
    }

    /**
     * Converts a smithy-model trait to an SDK runtime trait with {@link TraitFactories#create}: the typed trait when
     * a factory is registered for its ID, else a {@code GenericTrait} holding {@link #document(Node)} of its value
     * (Requirement 25.4).
     *
     * @param trait the smithy-model trait
     * @return the runtime trait
     */
    public static Trait trait(software.amazon.smithy.model.traits.Trait trait) {
        Validate.paramNotNull(trait, "trait");
        return TraitFactories.create(shapeId(trait.toShapeId()), document(trait.toNode()));
    }

    /**
     * Converts a node to a document with the mapping codegen's {@code DocumentLiterals} renders, so that converted and
     * generated trait values are equal:
     * <ul>
     *     <li>null → null; boolean → {@code Document.of(boolean)}; string → {@code Document.of(String)};</li>
     *     <li>an integral number → {@code Document.of(int)} when it fits an {@code int}, else
     *     {@code Document.of(long)}, else {@code Document.of(BigInteger)};</li>
     *     <li>a {@code BigDecimal} → {@code Document.of(BigDecimal)}; any other number → {@code Document.of(double)};
     *     </li>
     *     <li>an array → a list document; an object → a map document in member order.</li>
     * </ul>
     *
     * @param node the node, or null
     * @return the document, or null for a null node
     */
    public static Document document(Node node) {
        if (node == null || node.isNullNode()) {
            return null;
        }
        if (node.isBooleanNode()) {
            return Document.of(node.expectBooleanNode().getValue());
        }
        if (node.isStringNode()) {
            return Document.of(node.expectStringNode().getValue());
        }
        if (node.isNumberNode()) {
            return number(node.expectNumberNode().getValue());
        }
        if (node.isArrayNode()) {
            return array(node.expectArrayNode());
        }
        return object(node.expectObjectNode());
    }

    private static Document number(Number value) {
        if (value instanceof BigDecimal) {
            return Document.of((BigDecimal) value);
        }
        if (value instanceof BigInteger) {
            BigInteger big = (BigInteger) value;
            return big.bitLength() >= Long.SIZE ? Document.of(big) : integral(big.longValue());
        }
        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return integral(value.longValue());
        }
        return Document.of(value.doubleValue());
    }

    private static Document integral(long value) {
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return Document.of((int) value);
        }
        return Document.of(value);
    }

    private static Document array(ArrayNode node) {
        List<Document> elements = new ArrayList<>(node.size());
        for (Node element : node.getElements()) {
            elements.add(document(element));
        }
        return Document.of(Collections.unmodifiableList(elements));
    }

    private static Document object(ObjectNode node) {
        Map<String, Node> members = node.getStringMap();
        Map<String, Document> result = new LinkedHashMap<>(members.size() * 4 / 3 + 1);
        for (Map.Entry<String, Node> entry : members.entrySet()) {
            result.put(entry.getKey(), document(entry.getValue()));
        }
        return Document.of(Collections.unmodifiableMap(result));
    }
}
