package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

/**
 * An entity associated with metrics, to allow for finding related telemetry. An entity is typically a resource or
 * service within your system. For example, metrics from an Amazon EC2 instance could be associated with that instance
 * as the entity. Similarly, metrics from a service that you own could be associated with that service as the entity.
 */
@SmithyGenerated
public final class Entity implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.ENTITY;
    private static final Schema $SCHEMA_KEY_ATTRIBUTES = $SCHEMA.member("KeyAttributes");
    private static final Schema $SCHEMA_ATTRIBUTES = $SCHEMA.member("Attributes");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Map<String, String> keyAttributes;
    private final transient Map<String, String> attributes;

    private Entity(Builder builder) {
        this.keyAttributes = builder.keyAttributes == null ? null : Collections.unmodifiableMap(builder.keyAttributes);
        this.attributes = builder.attributes == null ? null : Collections.unmodifiableMap(builder.attributes);
    }

    /**
     * The attributes of the entity which identify the specific entity, as a list of key-value pairs. Entities with the
     * same <code>KeyAttributes</code> are considered to be the same entity. For an entity to be valid, the <code>
     * KeyAttributes</code> must exist and be formatted correctly.
     *
     * <p>There are five allowed attributes (key names): <code>Type</code>, <code>ResourceType</code>, <code>Identifier</code>
     * , <code>Name</code>, and <code>Environment</code>.
     *
     * <p>For details about how to use the key attributes to specify an entity, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/adding-your-own-related-telemetry.html">How to add related information to
     * telemetry</a> in the <i>CloudWatch User Guide</i>.
     */
    public Map<String, String> getKeyAttributes() {
        if (keyAttributes == null) {
            return Collections.emptyMap();
        }
        return keyAttributes;
    }

    public boolean hasKeyAttributes() {
        return keyAttributes != null;
    }

    /**
     * Additional attributes of the entity that are not used to specify the identity of the entity. A list of key-value
     * pairs.
     *
     * <p>For details about how to use the attributes, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/adding-your-own-related-telemetry.html">How to add related information to telemetry</a> in the <i>
     * CloudWatch User Guide</i>.
     */
    public Map<String, String> getAttributes() {
        if (attributes == null) {
            return Collections.emptyMap();
        }
        return attributes;
    }

    public boolean hasAttributes() {
        return attributes != null;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Entity that = (Entity) other;
        return Objects.equals(this.keyAttributes, that.keyAttributes)
               && Objects.equals(this.attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(keyAttributes);
        $hc = 31 * $hc + Objects.hashCode(attributes);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (keyAttributes != null) {
            serializer.writeMap($SCHEMA_KEY_ATTRIBUTES, keyAttributes, keyAttributes.size(), SharedSerde.EntityKeyAttributesMapSerializer.INSTANCE);
        }
        if (attributes != null) {
            serializer.writeMap($SCHEMA_ATTRIBUTES, attributes, attributes.size(), SharedSerde.EntityAttributesMapSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEY_ATTRIBUTES, member, keyAttributes);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES, member, attributes);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link Entity}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.keyAttributes(this.keyAttributes);
        builder.attributes(this.attributes);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Entity}.
     */
    public static final class Builder implements ShapeBuilder<Entity> {
        private Map<String, String> keyAttributes;
        private Map<String, String> attributes;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The attributes of the entity which identify the specific entity, as a list of key-value pairs. Entities with the
         * same <code>KeyAttributes</code> are considered to be the same entity. For an entity to be valid, the <code>
         * KeyAttributes</code> must exist and be formatted correctly.
         *
         * <p>There are five allowed attributes (key names): <code>Type</code>, <code>ResourceType</code>, <code>Identifier</code>
         * , <code>Name</code>, and <code>Environment</code>.
         *
         * <p>For details about how to use the key attributes to specify an entity, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/adding-your-own-related-telemetry.html">How to add related information to
         * telemetry</a> in the <i>CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder keyAttributes(Map<String, String> keyAttributes) {
            this.keyAttributes = keyAttributes;
            return this;
        }

        /**
         * Additional attributes of the entity that are not used to specify the identity of the entity. A list of key-value
         * pairs.
         *
         * <p>For details about how to use the attributes, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/adding-your-own-related-telemetry.html">How to add related information to telemetry</a> in the <i>
         * CloudWatch User Guide</i>.
         *
         * @return this builder.
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        @Override
        public Entity build() {
            return new Entity(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> keyAttributes((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_KEY_ATTRIBUTES, member, value));
                case 1 -> attributes((Map<String, String>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.keyAttributes(SharedSerde.deserializeEntityKeyAttributesMap(member, de));
                    case 1 -> builder.attributes(SharedSerde.deserializeEntityAttributesMap(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
