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

package software.amazon.awssdk.dynamicclient.schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Validate;

/**
 * A structure or union document that is backed by a schema and is also a {@link SerializableStruct}, so it can stand
 * in for an instance of a generated model class.
 *
 * <p>Members are keyed by Smithy member name and each holds a document with its member schema. As for every
 * {@link Document}, {@link #type()} is {@link ShapeType#STRUCTURE} for structures and unions, and
 * {@link #serialize(ShapeSerializer)} writes the document with one {@code writeDocument} call. As a
 * {@link SerializableStruct}, {@link #serializeMembers(ShapeSerializer)} writes each member with its member schema,
 * and {@link #serializeContents(ShapeSerializer)} writes the structure with {@code writeStruct}. A structure document's
 * {@link #getDiscriminator() discriminator} is the structure's ShapeId; a union document has none.
 *
 * <p>Instances are created by {@link #of(Schema, Document)} and by the shape builders of
 * {@link SchemaConverter#createDocumentBuilder(Schema)}. They are immutable.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicschemas.StructDocument}.
 */
@SdkPublicApi
public final class StructDocument implements Document, SerializableStruct {
    private final Schema schema;
    // Structure: member-indexed array. Union: always null (uses unionValue/unionMemberSchema instead).
    private final Document[] values;
    // Union only: the set member's value. Object (not Document) so scalars can be stored raw, skipping the
    // ContentDocument wrapper, which saves an allocation per value in union-dense payloads.
    private final Object unionValue;
    // Union only: the schema of the set member.
    private final Schema unionMemberSchema;
    private volatile Map<String, Document> mapView;

    StructDocument(Schema schema, Document[] values) {
        this.schema = schema;
        if (schema.type() == ShapeType.UNION) {
            // Collapse the per-member array to the single set value; unions never keep the array.
            int idx = findSetMember(values);
            this.values = null;
            this.unionValue = idx >= 0 ? unwrapScalar(values[idx]) : null;
            this.unionMemberSchema = idx >= 0 ? schema.members().get(idx) : null;
        } else {
            this.values = values;
            this.unionValue = null;
            this.unionMemberSchema = null;
        }
    }

    StructDocument(Schema schema, Object unionValue, Schema unionMemberSchema) {
        this.schema = schema;
        this.values = null;
        this.unionValue = unionValue;
        this.unionMemberSchema = unionMemberSchema;
    }

    /**
     * Converts an untyped document to a typed document that can be used in place of a structure or union.
     *
     * <p>Uses the shape ID of the given schema as the service shape ID; see {@link #of(Schema, Document, ShapeId)}.
     *
     * @param schema the structure or union schema to give the document
     * @param delegate the map, structure or union document to convert
     * @return the converted document
     * @throws IllegalArgumentException if the schema is not a structure or union, or the document is not a map,
     *     structure or union
     * @throws software.amazon.awssdk.schema.serde.SerializationException if a member value does not have the type its
     *     member schema needs
     */
    public static StructDocument of(Schema schema, Document delegate) {
        Validate.paramNotNull(schema, "schema");
        return of(schema, delegate, schema.id());
    }

    /**
     * Converts an untyped document to a typed document that can be used in place of a structure or union.
     *
     * <p>Members are matched by Smithy member name; entries that are not members of the schema are ignored. Each
     * member value is converted to its member's type: numbers with the Java primitive conversions (JLS 5.1.2 and
     * 5.1.3), and timestamps from a timestamp document, a number of epoch seconds or a {@code date-time} string.
     *
     * @param schema the structure or union schema to give the document
     * @param delegate the map, structure or union document to convert
     * @param service the shape ID of the service the document belongs to. It is reserved for resolving relative shape
     *     IDs in nested document discriminators, which the conversion does not read yet (as in smithy-java)
     * @return the converted document
     * @throws IllegalArgumentException if the schema is not a structure or union, or the document is not a map,
     *     structure or union
     * @throws software.amazon.awssdk.schema.serde.SerializationException if a member value does not have the type its
     *     member schema needs
     */
    public static StructDocument of(Schema schema, Document delegate, ShapeId service) {
        Validate.paramNotNull(schema, "schema");
        Validate.paramNotNull(delegate, "delegate");
        Validate.paramNotNull(service, "service");
        ShapeType schemaType = schema.type();
        if (schemaType != ShapeType.STRUCTURE && schemaType != ShapeType.UNION) {
            throw new IllegalArgumentException("Schema must be a structure or union, got " + schemaType);
        }
        ShapeType delegateType = delegate.type();
        if (delegateType == ShapeType.MAP || delegateType == ShapeType.STRUCTURE || delegateType == ShapeType.UNION) {
            return (StructDocument) convertDocument(schema, delegate, service);
        }
        throw new IllegalArgumentException("Document must be a map, structure, or union, but got " + delegateType);
    }

    /**
     * Converts an untyped document to a structure document with the dynamic client's strict input rules (design C.3,
     * Requirements 28.3 to 28.6, 28.10 and 28.11), where {@link #of(Schema, Document, ShapeId)} is lenient:
     * <ul>
     *     <li>a name that is not a member of its structure fails instead of being ignored;</li>
     *     <li>a value whose document type does not fit its member fails, naming the member;</li>
     *     <li>byte, short, integer, intEnum, long and bigInteger members need an integral number in their range, with
     *     no narrowing;</li>
     *     <li>a timestamp member takes a timestamp, a number of epoch seconds (with the fraction kept down to
     *     milliseconds) or a {@code date-time} string;</li>
     *     <li>a blob member takes only a blob document;</li>
     *     <li>a union may have only one member set.</li>
     * </ul>
     *
     * <p>A document that already has this schema is returned as is.
     *
     * @param schema the structure schema
     * @param value the map or structure document
     * @param service the shape ID of the service the document belongs to, as for
     *     {@link #of(Schema, Document, ShapeId)}
     * @return the structure document
     * @throws IllegalArgumentException if the schema is not a structure schema
     * @throws software.amazon.awssdk.schema.serde.SerializationException if the value cannot be coerced; the message
     *     names the member and never includes the value
     */
    @SdkInternalApi
    public static StructDocument coerce(Schema schema, Document value, ShapeId service) {
        Validate.paramNotNull(schema, "schema");
        Validate.paramNotNull(value, "value");
        Validate.paramNotNull(service, "service");
        if (schema.type() != ShapeType.STRUCTURE) {
            throw new IllegalArgumentException("Schema must be a structure, got " + schema.type());
        }
        if (value instanceof StructDocument && ((StructDocument) value).schema == schema) {
            return (StructDocument) value;
        }
        return StrictDocumentCoercion.coerceStructure(schema, value, service);
    }

    private static int findSetMember(Document[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                return i;
            }
        }
        return -1;
    }

    private static Object unwrapScalar(Document d) {
        if (d instanceof ContentDocument && ((ContentDocument) d).isScalar()) {
            return ((ContentDocument) d).rawValue();
        }
        return d;
    }

    // Cold path: re-wraps a raw scalar union value as a document for the accessor API.
    private Document unionValueAsDocument() {
        Object uv = unionValue;
        if (uv == null) {
            return null;
        }
        if (uv instanceof Document) {
            return (Document) uv;
        }
        return new ContentDocument(unionMemberSchema, uv);
    }

    private static Document convertStructureDocument(Schema schema, Document delegate, ShapeId service) {
        List<Schema> schemaMembers = schema.members();
        if (schema.type() == ShapeType.UNION) {
            for (int i = 0, n = schemaMembers.size(); i < n; i++) {
                Schema member = schemaMembers.get(i);
                Document value = delegate.getMember(member.memberName());
                if (value != null) {
                    return new StructDocument(schema, unwrapScalar(convertDocument(member, value, service)), member);
                }
            }
            return new StructDocument(schema, null, null);
        }
        Document[] result = new Document[schemaMembers.size()];
        for (int i = 0, n = schemaMembers.size(); i < n; i++) {
            Schema member = schemaMembers.get(i);
            Document value = delegate.getMember(member.memberName());
            if (value != null) {
                result[member.memberIndex()] = convertDocument(member, value, service);
            }
        }
        return new StructDocument(schema, result);
    }

    /**
     * Converts a document to the typed document of a schema.
     */
    static Document convertDocument(Schema schema, Document delegate, ShapeId service) {
        switch (schema.type()) {
            case STRUCTURE:
                return convertStructureDocument(schema, delegate, service);
            case UNION:
                rejectStreaming(schema);
                return convertStructureDocument(schema, delegate, service);
            case MAP: {
                Map<String, Document> result = new LinkedHashMap<>();
                Schema valueMember = schema.mapValueMember();
                for (Map.Entry<String, Document> entry : delegate.asMap().entrySet()) {
                    Document value = entry.getValue();
                    result.put(entry.getKey(), value == null ? null : convertDocument(valueMember, value, service));
                }
                return new ContentDocument(schema, result);
            }
            case LIST: {
                List<Document> result = new ArrayList<>();
                Schema valueMember = schema.listMember();
                for (Document value : delegate.asList()) {
                    result.add(value == null ? null : convertDocument(valueMember, value, service));
                }
                return new ContentDocument(schema, result);
            }
            case BOOLEAN:
                return new ContentDocument(schema, delegate.asBoolean());
            case STRING:
            case ENUM:
                return new ContentDocument(schema, delegate.asString());
            case TIMESTAMP:
                return new ContentDocument(schema, convertTimestamp(delegate));
            case BYTE:
                return new ContentDocument(schema, delegate.asByte());
            case SHORT:
                return new ContentDocument(schema, delegate.asShort());
            case INTEGER:
            case INT_ENUM:
                return new ContentDocument(schema, delegate.asInteger());
            case LONG:
                return new ContentDocument(schema, delegate.asLong());
            case FLOAT:
                return new ContentDocument(schema, delegate.asFloat());
            case DOUBLE:
                return new ContentDocument(schema, delegate.asDouble());
            case BIG_INTEGER:
                return new ContentDocument(schema, delegate.asBigInteger());
            case BIG_DECIMAL:
                return new ContentDocument(schema, delegate.asBigDecimal());
            case DOCUMENT:
                return new ContentDocument.SchemaDocument(schema, delegate);
            case BLOB:
                rejectStreaming(schema);
                return new ContentDocument(schema, delegate.asBlob());
            default:
                throw new IllegalArgumentException("Unsupported schema type: " + schema);
        }
    }

    /**
     * Streaming blobs and event streams have no document form yet; the dynamic client's streaming support adds them.
     */
    static void rejectStreaming(Schema schema) {
        if (schema.hasTrait(TraitKey.STREAMING_TRAIT)) {
            throw new UnsupportedOperationException("Streaming member " + schema.id() + " (" + schema.type()
                                                    + ") is not supported by StructDocument");
        }
    }

    /**
     * Coerces an untyped document to an {@link Instant} for a modeled timestamp member.
     *
     * <p>The value is interpreted by its data model type, not by any wire format: a number is epoch seconds and a
     * string is {@code date-time}, as in Smithy model nodes. The member's {@code timestampFormat} applies only when
     * the Instant is serialized.
     */
    private static Instant convertTimestamp(Document delegate) {
        ShapeType type = delegate.type();
        if (type == ShapeType.TIMESTAMP) {
            return delegate.asTimestamp();
        } else if (type != ShapeType.STRING) {
            return TimestampFormatter.Prelude.EPOCH_SECONDS.readFromNumber(delegate.asNumber());
        } else {
            return TimestampFormatter.Prelude.DATE_TIME.readFromString(delegate.asString(), false);
        }
    }

    @Override
    public Schema sdkSchema() {
        return schema;
    }

    /**
     * Writes the document with one {@code writeDocument} call, as every {@link Document} does. Use
     * {@link #serializeContents(ShapeSerializer)} to write it as a structure.
     */
    @Override
    public void serialize(ShapeSerializer serializer) {
        serializer.writeDocument(PreludeSchemas.DOCUMENT, this);
    }

    @Override
    public void serializeContents(ShapeSerializer serializer) {
        serializer.writeStruct(schema, this);
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        Document[] values = this.values;
        if (values != null) {
            List<Schema> members = schema.members();
            for (int i = 0; i < values.length; i++) {
                Document value = values[i];
                if (value != null) {
                    ContentDocument.writeValue(serializer, members.get(i), value);
                }
            }
        } else {
            Object uv = unionValue;
            if (uv instanceof Document) {
                ContentDocument.writeValue(serializer, unionMemberSchema, (Document) uv);
            } else if (uv != null) {
                // A scalar union member stored raw: write it directly with the member schema.
                ContentDocument.serializeScalar(serializer, unionMemberSchema, uv);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        Schema expected = member == null ? null : schema.member(member.memberName());
        if (expected == null) {
            throw SchemaUtils.notAMember(schema, member);
        }
        SchemaUtils.validateSameMember(expected, member, null);
        Object value;
        if (values != null) {
            value = values[expected.memberIndex()];
        } else {
            value = unionMemberSchema != null && expected.memberIndex() == unionMemberSchema.memberIndex()
                    ? unionValue
                    : null;
        }
        if (value == null) {
            return null;
        }
        try {
            // A raw scalar union value is already an asObject() result; a document is converted.
            return (T) (value instanceof Document ? ((Document) value).asObject() : value);
        } catch (ClassCastException e) {
            throw new ClassCastException("Unable to cast document member `" + member.id() + "` from document with "
                                         + "schema `" + schema.id() + "`: " + e.getMessage());
        }
    }

    /**
     * Returns {@link ShapeType#STRUCTURE}, for structures and unions alike.
     */
    @Override
    public ShapeType type() {
        return ShapeType.STRUCTURE;
    }

    /**
     * Returns the structure's ShapeId (the member's target for a nested structure), or null for a union.
     */
    @Override
    public ShapeId getDiscriminator() {
        if (schema.type() != ShapeType.STRUCTURE) {
            return null;
        }
        return schema.isMember() ? schema.memberTarget().id() : schema.id();
    }

    @Override
    public int size() {
        Document[] values = this.values;
        if (values != null) {
            int count = 0;
            for (Document v : values) {
                if (v != null) {
                    count++;
                }
            }
            return count;
        }
        return unionValue != null ? 1 : 0;
    }

    @Override
    public Map<String, Document> asMap() {
        Map<String, Document> result = mapView;
        if (result == null) {
            Document[] values = this.values;
            if (values != null) {
                Map<String, Document> members = new LinkedHashMap<>();
                List<Schema> schemaMembers = schema.members();
                for (int i = 0, n = schemaMembers.size(); i < n; i++) {
                    Document value = values[i];
                    if (value != null) {
                        members.put(schemaMembers.get(i).memberName(), value);
                    }
                }
                result = Collections.unmodifiableMap(members);
            } else {
                result = unionValue != null
                         ? Collections.singletonMap(unionMemberSchema.memberName(), unionValueAsDocument())
                         : Collections.<String, Document>emptyMap();
            }
            // A race builds the view twice and keeps either; both hold the same entries.
            mapView = result;
        }
        return result;
    }

    @Override
    public Document getMember(String memberName) {
        Schema memberSchema = schema.member(memberName);
        if (memberSchema == null) {
            return null;
        }
        int idx = memberSchema.memberIndex();
        Document[] values = this.values;
        if (values != null) {
            return values[idx];
        }
        return unionMemberSchema != null && idx == unionMemberSchema.memberIndex() ? unionValueAsDocument() : null;
    }

    @Override
    public Set<String> getMemberNames() {
        return asMap().keySet();
    }

    @Override
    public boolean equals(Object obj) {
        return Document.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Document.hash(this);
    }

    @Override
    public String toString() {
        return ContentDocument.describe(schema, type(), asMap());
    }
}
