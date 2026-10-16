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

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.codecs.xml.XmlSettings;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * Per-codec caches of the XML names of structures, lists and maps, and the codec's settings that the serializers and
 * deserializers apply ({@code xmlName}, {@code timestampFormat}, unknown union members).
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlInfo}, plus the settings (SDK addition, design A.8).
 */
@SdkInternalApi
final class XmlInfo {

    private final ConcurrentMap<Schema, StructInfo> structInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<Schema, ListMemberInfo> listInfo = new ConcurrentHashMap<>();
    private final ConcurrentMap<Schema, MapMemberInfo> mapInfo = new ConcurrentHashMap<>();
    // Extensions computed without xmlName; only used when useXmlName is false.
    private final ConcurrentMap<Schema, XmlSchemaExtensions.XmlExtension> extensionsWithoutXmlName =
        new ConcurrentHashMap<>();

    private final boolean useXmlName;
    private final boolean useTimestampFormat;
    private final TimestampFormatTrait.Format defaultTimestampFormat;
    private final TimestampFormatter defaultTimestampFormatter;
    private final boolean forbidUnknownUnionMembers;

    XmlInfo() {
        this(XmlSettings.REST_XML);
    }

    XmlInfo(XmlSettings settings) {
        this.useXmlName = settings.useXmlName();
        this.useTimestampFormat = settings.useTimestampFormat();
        this.defaultTimestampFormat = settings.defaultTimestampFormat();
        this.defaultTimestampFormatter = TimestampFormatter.of(defaultTimestampFormat);
        this.forbidUnknownUnionMembers = settings.forbidUnknownUnionMembers();
    }

    // Each lookup reads the map first: Java 8's ConcurrentHashMap.computeIfAbsent locks the bin even on a hit.
    StructInfo getStructInfo(Schema schema) {
        StructInfo result = structInfo.get(schema);
        return result != null ? result : structInfo.computeIfAbsent(schema, s -> new StructInfo(s, useXmlName));
    }

    ListMemberInfo getListInfo(Schema schema) {
        ListMemberInfo result = listInfo.get(schema);
        return result != null ? result : listInfo.computeIfAbsent(schema, s -> new ListMemberInfo(s, useXmlName));
    }

    MapMemberInfo getMapInfo(Schema schema) {
        MapMemberInfo result = mapInfo.get(schema);
        return result != null ? result : mapInfo.computeIfAbsent(schema, s -> new MapMemberInfo(s, useXmlName));
    }

    boolean useXmlName() {
        return useXmlName;
    }

    /**
     * Returns the {@code xmlName} trait of {@code schema} (found through {@link Schema#getTrait}), or {@code null} if it
     * has none or the codec ignores {@code xmlName}.
     */
    XmlNameTrait xmlNameTrait(Schema schema) {
        return useXmlName ? schema.getTrait(TraitKey.XML_NAME_TRAIT) : null;
    }

    /**
     * Returns the {@code xmlName} trait applied directly to {@code schema}, or {@code null} if it has none or the codec
     * ignores {@code xmlName}.
     */
    XmlNameTrait directXmlNameTrait(Schema schema) {
        return useXmlName ? schema.getDirectTrait(TraitKey.XML_NAME_TRAIT) : null;
    }

    /**
     * Returns the XML schema extension of {@code schema}: the registered one when {@code xmlName} is honored, otherwise
     * one computed (once) without it.
     */
    XmlSchemaExtensions.XmlExtension extension(Schema schema) {
        if (useXmlName) {
            return schema.getExtension(XmlSchemaExtensions.KEY);
        }
        XmlSchemaExtensions.XmlExtension result = extensionsWithoutXmlName.get(schema);
        if (result == null) {
            result = XmlSchemaExtensions.create(schema, false);
            if (result != null) {
                XmlSchemaExtensions.XmlExtension previous = extensionsWithoutXmlName.putIfAbsent(schema, result);
                if (previous != null) {
                    result = previous;
                }
            }
        }
        return result;
    }

    /**
     * Returns the formatter of {@code schema}'s timestamps: its {@code timestampFormat} trait when the codec honors it,
     * otherwise the codec's default format.
     */
    TimestampFormatter timestampFormatter(Schema schema) {
        if (useTimestampFormat) {
            TimestampFormatTrait trait = schema.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
            if (trait != null) {
                return TimestampFormatter.of(trait);
            }
        }
        return defaultTimestampFormatter;
    }

    /**
     * Throws if {@code schema} is a union and the codec rejects unknown union members; otherwise reports the member to
     * {@code consumer}.
     */
    <T> void unknownMember(Schema schema, T state, ShapeDeserializer.StructMemberConsumer<T> consumer, String member) {
        if (forbidUnknownUnionMembers && schema.type() == ShapeType.UNION) {
            throw new SerializationException("Unknown member " + member + " encountered for union " + schema.id());
        }
        consumer.unknownMember(state, member);
    }

    private static String getName(Schema schema, boolean useXmlName) {
        return XmlSchemaExtensions.resolveXmlName(schema, useXmlName);
    }

    static final class StructInfo {
        final Schema schema;
        final Map<String, Schema> attributes;
        final Map<String, Schema> elements;
        final boolean hasFlattened;
        final XmlMemberLookup memberLookup;

        private StructInfo(Schema schema, boolean useXmlName) {
            if (schema.type() != ShapeType.STRUCTURE && schema.type() != ShapeType.UNION) {
                throw new IllegalArgumentException("Expected a structure or union schema but found " + schema);
            }

            this.schema = schema;

            Map<String, Schema> attributeMap = null;
            Map<String, Schema> elementMap = null;
            boolean flattenedMembers = false;
            for (Schema member : schema.members()) {
                String name = getName(member, useXmlName);
                if (member.hasTrait(TraitKey.XML_ATTRIBUTE_TRAIT)) {
                    if (attributeMap == null) {
                        attributeMap = new HashMap<>();
                    }
                    attributeMap.put(name, member);
                } else {
                    if (elementMap == null) {
                        elementMap = new HashMap<>();
                    }
                    elementMap.put(name, member);
                    if (member.hasTrait(TraitKey.XML_FLATTENED_TRAIT)) {
                        flattenedMembers = true;
                    }
                }
            }

            this.hasFlattened = flattenedMembers;
            this.attributes = attributeMap == null ? Collections.emptyMap() : attributeMap;
            this.elements = elementMap == null ? Collections.emptyMap() : elementMap;
            this.memberLookup = elementMap != null ? new XmlMemberLookup(this.elements) : null;
        }

        // If the shape has flattened members, then prepare a map to store buffered state.
        Map<Schema, Deque<XMLEvent>> createFlattenedState() {
            return hasFlattened ? new HashMap<>() : Collections.emptyMap();
        }

        <T> void readMember(Map<Schema, Deque<XMLEvent>> flatState,
                            XMLEventFactory eventFactory,
                            ShapeDeserializer deserializer,
                            XmlReader reader,
                            T state,
                            ShapeDeserializer.StructMemberConsumer<T> consumer,
                            String elementName,
                            Schema member) throws XMLStreamException {
            if (!hasFlattened || !member.hasTrait(TraitKey.XML_FLATTENED_TRAIT)) {
                consumer.accept(state, member, deserializer);
            } else {
                // Events for flattened members need to be buffered to ensure that all flattened members are
                // deserialized regardless of if they are interspersed with other nodes.
                Deque<XMLEvent> current = flatState.get(member);
                Deque<XMLEvent> buffer = reader.bufferElement(elementName);
                // The just read start event needs to be added back to the event buffer so it can be replayed while
                // parsing the deferred flattened list values.
                QName qname = new QName(elementName);
                StartElement startElement = eventFactory.createStartElement(qname, null, null);
                if (current == null) {
                    buffer.addFirst(startElement);
                    flatState.put(member, buffer);
                } else {
                    current.add(startElement);
                    current.addAll(buffer);
                }
            }
        }

        <T> void finishReadingStruct(Map<Schema, Deque<XMLEvent>> flatState,
                                     XMLEventFactory eventFactory,
                                     XmlInfo decoders,
                                     T state,
                                     ShapeDeserializer.StructMemberConsumer<T> consumer) throws XMLStreamException {
            for (Map.Entry<Schema, Deque<XMLEvent>> entry : flatState.entrySet()) {
                // Use a special flattened deserializer that delegates validation of the encountered element.
                consumer.accept(state,
                                entry.getKey(),
                                XmlDeserializer.flattened(decoders, eventFactory,
                                                          new XmlReader.BufferedReader(entry.getValue())));
            }
        }
    }

    static final class ListMemberInfo {
        final String xmlName;
        final String memberName;
        final boolean flattened;

        ListMemberInfo(Schema schema, boolean useXmlName) {
            if (schema.type() != ShapeType.LIST) {
                throw new IllegalArgumentException("Expected a list schema but found " + schema);
            }

            this.xmlName = getName(schema, useXmlName);
            this.flattened = schema.hasTrait(TraitKey.XML_FLATTENED_TRAIT);

            if (flattened) {
                this.memberName = xmlName;
            } else {
                XmlNameTrait memberXmlName = useXmlName ? schema.listMember().getTrait(TraitKey.XML_NAME_TRAIT) : null;
                this.memberName = memberXmlName != null ? memberXmlName.getValue() : "member";
            }
        }
    }

    static final class MapMemberInfo {
        final String xmlName;
        final String entryName;
        final String keyName;
        final String valueName;
        final boolean flattened;

        MapMemberInfo(Schema schema, boolean useXmlName) {
            if (schema.type() != ShapeType.MAP) {
                throw new IllegalArgumentException("Expected a map schema but found " + schema);
            }

            this.xmlName = getName(schema, useXmlName);
            this.flattened = schema.hasTrait(TraitKey.XML_FLATTENED_TRAIT);
            this.entryName = flattened ? xmlName : "entry";
            this.keyName = getName(schema.mapKeyMember(), useXmlName);
            this.valueName = getName(schema.mapValueMember(), useXmlName);
        }
    }
}
