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

package software.amazon.awssdk.codecs.xml.internal;

import java.nio.charset.StandardCharsets;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design Porting_Map) name.
/**
 * Computes, once per schema, the pre-encoded element names, namespace declarations and attribute/flattened flags the
 * native XML serializer and deserializer use: per member, per structure or union (tables indexed by
 * {@link Schema#memberIndex()}), per list (the item element name) and per map (the entry, key and value element
 * names). The extension honors the {@code xmlName} trait; a codec configured to ignore it computes the same data with
 * {@link #create(Schema, boolean)} instead.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlSchemaExtensions}. Registered as a
 * {@link SchemaExtensionProvider} through {@code META-INF/services}.
 */
@SdkInternalApi
public final class XmlSchemaExtensions implements SchemaExtensionProvider<XmlSchemaExtensions.XmlExtension> {
    // CHECKSTYLE:ON

    /**
     * Extension key for the XML codec data.
     */
    public static final SchemaExtensionKey<XmlExtension> KEY = new SchemaExtensionKey<>();

    @Override
    public SchemaExtensionKey<XmlExtension> key() {
        return KEY;
    }

    @Override
    public XmlExtension provide(Schema schema) {
        return create(schema, true);
    }

    /**
     * Computes the extension of {@code schema}, honoring the {@code xmlName} trait only if {@code useXmlName} is set.
     *
     * @return the extension, or {@code null} for schemas that are not members, structures, unions, lists or maps
     */
    static XmlExtension create(Schema schema, boolean useXmlName) {
        ShapeType type = schema.type();
        if (schema.isMember()) {
            return provideMember(schema, useXmlName);
        } else if (type == ShapeType.STRUCTURE || type == ShapeType.UNION) {
            return provideStruct(schema, useXmlName);
        } else if (type == ShapeType.LIST) {
            return provideList(schema, useXmlName);
        } else if (type == ShapeType.MAP) {
            return provideMap(schema, useXmlName);
        }
        return null;
    }

    private static XmlExtension provideMember(Schema schema, boolean useXmlName) {
        String name = resolveXmlName(schema, useXmlName);
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        boolean isAttribute = schema.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT);
        boolean isFlattened = schema.hasTrait(TraitKey.XML_FLATTENED_TRAIT);
        byte[] namespaceBytes = null;
        XmlNamespaceTrait ns = schema.getDirectTrait(TraitKey.XML_NAMESPACE_TRAIT);
        if (ns != null) {
            namespaceBytes = buildNamespaceBytes(ns);
        }
        return new MemberExtension(nameBytes, isAttribute, isFlattened, namespaceBytes);
    }

    private static XmlExtension provideStruct(Schema schema, boolean useXmlName) {
        List<Schema> members = schema.members();
        int maxIndex = 0;
        for (Schema member : members) {
            maxIndex = Math.max(maxIndex, member.memberIndex());
        }

        byte[][] nameTable = new byte[maxIndex + 1][];
        byte[][] nsTable = new byte[maxIndex + 1][];
        boolean[] isAttributeTable = new boolean[maxIndex + 1];
        boolean[] isFlattenedTable = new boolean[maxIndex + 1];
        boolean hasAttributes = false;
        boolean hasNamespaces = false;

        for (Schema member : members) {
            int idx = member.memberIndex();
            nameTable[idx] = resolveXmlName(member, useXmlName).getBytes(StandardCharsets.UTF_8);
            isAttributeTable[idx] = member.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT);
            isFlattenedTable[idx] = member.hasTrait(TraitKey.XML_FLATTENED_TRAIT);
            if (isAttributeTable[idx]) {
                hasAttributes = true;
            }
            XmlNamespaceTrait ns = member.getDirectTrait(TraitKey.XML_NAMESPACE_TRAIT);
            if (ns != null) {
                nsTable[idx] = buildNamespaceBytes(ns);
                hasNamespaces = true;
            }
        }

        String structName;
        XmlNameTrait xmlNameTrait = useXmlName ? schema.getTrait(TraitKey.XML_NAME_TRAIT) : null;
        if (xmlNameTrait != null) {
            structName = xmlNameTrait.getValue();
        } else {
            structName = schema.id().getName();
        }
        byte[] structNameBytes = structName.getBytes(StandardCharsets.UTF_8);

        byte[] namespaceBytes = null;
        XmlNamespaceTrait nsTrait = schema.getTrait(TraitKey.XML_NAMESPACE_TRAIT);
        if (nsTrait != null) {
            namespaceBytes = buildNamespaceBytes(nsTrait);
        }

        return new StructExtension(nameTable,
                                   hasNamespaces ? nsTable : null,
                                   isAttributeTable,
                                   isFlattenedTable,
                                   structNameBytes,
                                   namespaceBytes,
                                   hasAttributes);
    }

    private static XmlExtension provideList(Schema schema, boolean useXmlName) {
        boolean flattened = schema.hasTrait(TraitKey.XML_FLATTENED_TRAIT);
        String memberName;
        if (flattened) {
            memberName = resolveXmlName(schema, useXmlName);
        } else {
            XmlNameTrait memberXmlName = useXmlName ? schema.listMember().getTrait(TraitKey.XML_NAME_TRAIT) : null;
            memberName = memberXmlName != null ? memberXmlName.getValue() : "member";
        }
        return new ListExtension(memberName.getBytes(StandardCharsets.UTF_8));
    }

    private static XmlExtension provideMap(Schema schema, boolean useXmlName) {
        boolean flattened = schema.hasTrait(TraitKey.XML_FLATTENED_TRAIT);
        String xmlName = resolveXmlName(schema, useXmlName);
        String entryName = flattened ? xmlName : "entry";
        String keyName = resolveMapMemberName(schema.mapKeyMember(), useXmlName);
        String valueName = resolveMapMemberName(schema.mapValueMember(), useXmlName);
        return new MapExtension(entryName.getBytes(StandardCharsets.UTF_8),
                                keyName.getBytes(StandardCharsets.UTF_8),
                                valueName.getBytes(StandardCharsets.UTF_8));
    }

    // smithy-java has two identical methods, resolveMemberXmlName and resolveXmlName; one is kept.
    static String resolveXmlName(Schema schema, boolean useXmlName) {
        XmlNameTrait trait = useXmlName ? schema.getDirectTrait(TraitKey.XML_NAME_TRAIT) : null;
        if (trait != null) {
            return trait.getValue();
        } else if (schema.isMember()) {
            return schema.memberName();
        } else {
            return schema.id().getName();
        }
    }

    private static String resolveMapMemberName(Schema schema, boolean useXmlName) {
        XmlNameTrait trait = useXmlName ? schema.getDirectTrait(TraitKey.XML_NAME_TRAIT) : null;
        if (trait != null) {
            return trait.getValue();
        }
        return schema.memberName();
    }

    /**
     * Returns the {@code xmlns} or {@code xmlns:prefix} attribute, with a leading space, declaring {@code ns}.
     */
    static byte[] buildNamespaceBytes(XmlNamespaceTrait ns) {
        String prefix = ns.getPrefix().orElse(null);
        String escapedUri = ns.getUri().replace("&", "&amp;")
                              .replace("\"", "&quot;")
                              .replace("<", "&lt;")
                              .replace(">", "&gt;");
        if (prefix == null || prefix.isEmpty()) {
            return (" xmlns=\"" + escapedUri + "\"").getBytes(StandardCharsets.UTF_8);
        } else {
            return (" xmlns:" + prefix + "=\"" + escapedUri + "\"").getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * The XML data of one schema: a {@link MemberExtension}, {@link StructExtension}, {@link ListExtension} or
     * {@link MapExtension}. The arrays these hold are never modified after construction and are only read by this
     * package.
     */
    public abstract static class XmlExtension {
        XmlExtension() {
        }
    }

    /**
     * The XML data of a member: its element (or attribute) name and namespace declaration.
     */
    public static final class MemberExtension extends XmlExtension {
        private final byte[] nameBytes;
        private final boolean isAttribute;
        private final boolean isFlattened;
        private final byte[] namespaceBytes;

        MemberExtension(byte[] nameBytes, boolean isAttribute, boolean isFlattened, byte[] namespaceBytes) {
            this.nameBytes = nameBytes;
            this.isAttribute = isAttribute;
            this.isFlattened = isFlattened;
            this.namespaceBytes = namespaceBytes;
        }

        byte[] nameBytes() {
            return nameBytes;
        }

        boolean isAttribute() {
            return isAttribute;
        }

        boolean isFlattened() {
            return isFlattened;
        }

        byte[] namespaceBytes() {
            return namespaceBytes;
        }
    }

    /**
     * The XML data of a structure or union: member names, namespace declarations and flags indexed by member index, and
     * the structure's own element name and namespace declaration.
     */
    public static final class StructExtension extends XmlExtension {
        private final byte[][] nameTable;
        private final byte[][] memberNamespaceTable;
        private final boolean[] isAttributeTable;
        private final boolean[] isFlattenedTable;
        private final byte[] structNameBytes;
        private final byte[] namespaceBytes;
        private final boolean hasAttributes;

        StructExtension(byte[][] nameTable, byte[][] memberNamespaceTable, boolean[] isAttributeTable,
                        boolean[] isFlattenedTable, byte[] structNameBytes, byte[] namespaceBytes,
                        boolean hasAttributes) {
            this.nameTable = nameTable;
            this.memberNamespaceTable = memberNamespaceTable;
            this.isAttributeTable = isAttributeTable;
            this.isFlattenedTable = isFlattenedTable;
            this.structNameBytes = structNameBytes;
            this.namespaceBytes = namespaceBytes;
            this.hasAttributes = hasAttributes;
        }

        byte[][] nameTable() {
            return nameTable;
        }

        byte[][] memberNamespaceTable() {
            return memberNamespaceTable;
        }

        boolean[] isAttributeTable() {
            return isAttributeTable;
        }

        boolean[] isFlattenedTable() {
            return isFlattenedTable;
        }

        byte[] structNameBytes() {
            return structNameBytes;
        }

        byte[] namespaceBytes() {
            return namespaceBytes;
        }

        boolean hasAttributes() {
            return hasAttributes;
        }
    }

    /**
     * The XML data of a list: the element name of its items.
     */
    public static final class ListExtension extends XmlExtension {
        private final byte[] memberNameBytes;

        ListExtension(byte[] memberNameBytes) {
            this.memberNameBytes = memberNameBytes;
        }

        byte[] memberNameBytes() {
            return memberNameBytes;
        }
    }

    /**
     * The XML data of a map: the element names of its entries, keys and values.
     */
    public static final class MapExtension extends XmlExtension {
        private final byte[] entryNameBytes;
        private final byte[] keyNameBytes;
        private final byte[] valueNameBytes;

        MapExtension(byte[] entryNameBytes, byte[] keyNameBytes, byte[] valueNameBytes) {
            this.entryNameBytes = entryNameBytes;
            this.keyNameBytes = keyNameBytes;
            this.valueNameBytes = valueNameBytes;
        }

        byte[] entryNameBytes() {
            return entryNameBytes;
        }

        byte[] keyNameBytes() {
            return keyNameBytes;
        }

        byte[] valueNameBytes() {
            return valueNameBytes;
        }
    }
}
