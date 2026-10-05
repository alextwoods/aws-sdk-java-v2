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

package software.amazon.awssdk.smithy.runtime.fixture;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.MapSerializer;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;

/**
 * A structure written in the shape that Smithy Java code generation emits for a generated service type: a static
 * {@link Schema} built from shape IDs and runtime trait values, direct member serialization, and a
 * {@link ShapeBuilder} with a schema-driven deserializer. It deliberately uses only the types exposed by the
 * {@code smithy-runtime} boundary.
 */
public final class Widget implements SerializableStruct {

    public static final ShapeId ID = ShapeId.from("software.amazon.awssdk.fixture#Widget");

    static final Schema TAGS_SCHEMA = Schema.listBuilder(ShapeId.from("software.amazon.awssdk.fixture#TagList"))
        .putMember("member", PreludeSchemas.STRING)
        .build();

    static final Schema ATTRIBUTES_SCHEMA = Schema.mapBuilder(ShapeId.from("software.amazon.awssdk.fixture#AttributeMap"))
        .putMember("key", PreludeSchemas.STRING)
        .putMember("value", PreludeSchemas.STRING)
        .build();

    public static final Schema SCHEMA = Schema.structureBuilder(ID)
        .putMember("widgetId",
                   PreludeSchemas.STRING,
                   new RequiredTrait(),
                   new HttpLabelTrait(),
                   new JsonNameTrait("id"),
                   new XmlNameTrait("Id"),
                   LengthTrait.builder().min(1L).max(64L).build())
        .putMember("count",
                   PreludeSchemas.INTEGER,
                   RangeTrait.builder().min(BigDecimal.ZERO).max(BigDecimal.valueOf(1000)).build())
        .putMember("createdAt", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS))
        .putMember("secret", PreludeSchemas.STRING, new SensitiveTrait())
        .putMember("payload", PreludeSchemas.BLOB)
        .putMember("tags", TAGS_SCHEMA)
        .putMember("attributes", ATTRIBUTES_SCHEMA)
        .builderSupplier(Widget::builder)
        .build();

    private static final Schema SCHEMA_WIDGET_ID = SCHEMA.member("widgetId");
    private static final Schema SCHEMA_COUNT = SCHEMA.member("count");
    private static final Schema SCHEMA_CREATED_AT = SCHEMA.member("createdAt");
    private static final Schema SCHEMA_SECRET = SCHEMA.member("secret");
    private static final Schema SCHEMA_PAYLOAD = SCHEMA.member("payload");
    private static final Schema SCHEMA_TAGS = SCHEMA.member("tags");
    private static final Schema SCHEMA_ATTRIBUTES = SCHEMA.member("attributes");
    private static final Schema TAGS_MEMBER = TAGS_SCHEMA.member("member");
    private static final Schema ATTRIBUTES_KEY = ATTRIBUTES_SCHEMA.member("key");
    private static final Schema ATTRIBUTES_VALUE = ATTRIBUTES_SCHEMA.member("value");

    private final String widgetId;
    private final Integer count;
    private final Instant createdAt;
    private final String secret;
    private final ByteBuffer payload;
    private final List<String> tags;
    private final Map<String, String> attributes;

    private Widget(Builder builder) {
        this.widgetId = builder.widgetId;
        this.count = builder.count;
        this.createdAt = builder.createdAt;
        this.secret = builder.secret;
        this.payload = builder.payload;
        this.tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String widgetId() {
        return widgetId;
    }

    public Integer count() {
        return count;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String secret() {
        return secret;
    }

    public ByteBuffer payload() {
        return payload == null ? null : payload.asReadOnlyBuffer();
    }

    public List<String> tags() {
        return tags;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public Schema schema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString(SCHEMA_WIDGET_ID, widgetId);
        if (count != null) {
            serializer.writeInteger(SCHEMA_COUNT, count);
        }
        if (createdAt != null) {
            serializer.writeTimestamp(SCHEMA_CREATED_AT, createdAt);
        }
        if (secret != null) {
            serializer.writeString(SCHEMA_SECRET, secret);
        }
        if (payload != null) {
            serializer.writeBlob(SCHEMA_PAYLOAD, payload.asReadOnlyBuffer());
        }
        if (!tags.isEmpty()) {
            serializer.writeList(SCHEMA_TAGS, tags, tags.size(), Widget::writeTags);
        }
        if (!attributes.isEmpty()) {
            serializer.writeMap(SCHEMA_ATTRIBUTES, attributes, attributes.size(), Widget::writeAttributes);
        }
    }

    private static void writeTags(List<String> values, ShapeSerializer serializer) {
        for (String value : values) {
            serializer.writeString(TAGS_MEMBER, value);
        }
    }

    private static void writeAttributes(Map<String, String> values, MapSerializer serializer) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            serializer.writeEntry(ATTRIBUTES_KEY, entry.getKey(), entry.getValue(), Widget::writeAttributeValue);
        }
    }

    private static void writeAttributeValue(String value, ShapeSerializer serializer) {
        serializer.writeString(ATTRIBUTES_VALUE, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) widgetId;
            case 1 -> (T) count;
            case 2 -> (T) createdAt;
            case 3 -> (T) secret;
            case 4 -> (T) payload;
            case 5 -> (T) tags;
            case 6 -> (T) attributes;
            default -> throw new IllegalArgumentException("Unknown member: " + member.id());
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Widget other)) {
            return false;
        }
        return Objects.equals(widgetId, other.widgetId)
               && Objects.equals(count, other.count)
               && Objects.equals(createdAt, other.createdAt)
               && Objects.equals(secret, other.secret)
               && Objects.equals(payload, other.payload)
               && tags.equals(other.tags)
               && attributes.equals(other.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(widgetId, count, createdAt, secret, payload, tags, attributes);
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    public static final class Builder implements ShapeBuilder<Widget> {
        private String widgetId;
        private Integer count;
        private Instant createdAt;
        private String secret;
        private ByteBuffer payload;
        private List<String> tags = new ArrayList<>();
        private Map<String, String> attributes = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder widgetId(String widgetId) {
            this.widgetId = widgetId;
            return this;
        }

        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder payload(ByteBuffer payload) {
            this.payload = payload;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = new ArrayList<>(tags);
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = new LinkedHashMap<>(attributes);
            return this;
        }

        @Override
        public Schema schema() {
            return SCHEMA;
        }

        @Override
        public Widget build() {
            if (widgetId == null) {
                throw new IllegalStateException("Missing required member: " + SCHEMA_WIDGET_ID.id());
            }
            return new Widget(this);
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct(SCHEMA, this, Builder::readMember);
            return this;
        }

        private static void readMember(Builder builder, Schema member, ShapeDeserializer de) {
            switch (member.memberIndex()) {
                case 0 -> builder.widgetId(de.readString(member));
                case 1 -> builder.count(de.readInteger(member));
                case 2 -> builder.createdAt(de.readTimestamp(member));
                case 3 -> builder.secret(de.readString(member));
                case 4 -> builder.payload(de.readBlob(member));
                case 5 -> {
                    List<String> values = new ArrayList<>();
                    de.readList(member, values, (list, element) -> list.add(element.readString(TAGS_MEMBER)));
                    builder.tags(values);
                }
                case 6 -> {
                    Map<String, String> values = new LinkedHashMap<>();
                    de.readStringMap(member, values,
                                     (map, key, value) -> map.put(key, value.readString(ATTRIBUTES_VALUE)));
                    builder.attributes(values);
                }
                default -> throw new IllegalArgumentException("Unknown member: " + member.id());
            }
        }
    }
}
