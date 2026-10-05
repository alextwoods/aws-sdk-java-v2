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

package software.amazon.awssdk.protocols.core.xml;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

/** Test helpers: dynamic values of a Smithy model, and a canonical {@link Node} rendering of them. */
final class XmlTestValues {
    private XmlTestValues() {
    }

    /** The assembled model of {@code idl}. */
    static Model model(String idl) {
        return Model.assembler().addUnparsedModel("test.smithy", idl).assemble().unwrap();
    }

    /** The schema of the shape {@code shapeId} of {@code model}. */
    static Schema schema(Model model, String shapeId) {
        return new SchemaConverter(model).getSchema(model.expectShape(ShapeId.from(shapeId)));
    }

    /** A fresh builder of the structure {@code shapeId} of {@code model}. */
    static ShapeBuilder<StructDocument> builder(Model model, String shapeId) {
        return SchemaConverter.createDocumentBuilder(schema(model, shapeId));
    }

    /** A value of {@code structure} holding {@code members} (plain Java values, converted per the schema). */
    static StructDocument value(Schema structure, Map<String, ?> members) {
        return StructDocument.of(structure, Document.ofObject(members));
    }

    /**
     * The canonical rendering of a value: structures and maps as objects (member order is insignificant), lists as
     * arrays, strings and booleans as themselves, numbers as numbers, blobs as base64 strings, timestamps as ISO-8601
     * strings, and null as null.
     */
    static Node canonical(Document value) {
        if (value == null) {
            return Node.nullNode();
        }
        return switch (value.type()) {
            case STRUCTURE, UNION -> {
                Map<String, Node> members = new TreeMap<>();
                for (String name : value.getMemberNames()) {
                    members.put(name, canonical(value.getMember(name)));
                }
                yield object(members);
            }
            case MAP -> {
                Map<String, Node> entries = new TreeMap<>();
                value.asStringMap().forEach((key, entry) -> entries.put(key, canonical(entry)));
                yield object(entries);
            }
            case LIST, SET -> {
                List<Node> items = new ArrayList<>();
                for (Document item : value.asList()) {
                    items.add(canonical(item));
                }
                yield Node.fromNodes(items);
            }
            case BOOLEAN -> Node.from(value.asBoolean());
            case STRING, ENUM -> Node.from(value.asString());
            case BLOB -> Node.from(base64(value.asBlob()));
            case TIMESTAMP -> Node.from(value.asTimestamp().toString());
            default -> Node.from(value.asNumber());
        };
    }

    private static Node object(Map<String, Node> members) {
        Map<StringNode, Node> out = new LinkedHashMap<>();
        members.forEach((key, node) -> out.put(Node.from(key), node));
        return Node.objectNode(out);
    }

    private static String base64(ByteBuffer value) {
        ByteBuffer view = value.duplicate();
        byte[] bytes = new byte[view.remaining()];
        view.get(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
