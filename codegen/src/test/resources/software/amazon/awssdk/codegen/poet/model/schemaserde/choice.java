package software.amazon.awssdk.services.demo.model;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.ToStringSerializer;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class Choice implements SdkPojo, Serializable, ToCopyableBuilder<Choice.Builder, Choice>, SerializableStruct {
    /**
     * The schema of this shape.
     */
    public static final Schema SCHEMA = DemoSchemas.CHOICE;

    private static final Schema M_TEXT = SCHEMA.member("Text");

    private static final Schema M_NUMBER = SCHEMA.member("Number");

    private static final Schema M_NOTHING = SCHEMA.member("Nothing");

    private static final long serialVersionUID = 1L;

    private final String text;

    private final Integer number;

    private final Unit nothing;

    private final Type type;

    private Choice(BuilderImpl builder) {
        this.text = builder.text;
        this.number = builder.number;
        this.nothing = builder.nothing;
        this.type = builder.type;
    }

    /**
     * Returns the value of the Text property for this object.
     * 
     * @return The value of the Text property for this object.
     */
    public final String text() {
        return text;
    }

    /**
     * Returns the value of the Number property for this object.
     * 
     * @return The value of the Number property for this object.
     */
    public final Integer number() {
        return number;
    }

    /**
     * Returns the value of the Nothing property for this object.
     * 
     * @return The value of the Nothing property for this object.
     */
    public final Unit nothing() {
        return nothing;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(text());
        hashCode = 31 * hashCode + Objects.hashCode(number());
        hashCode = 31 * hashCode + Objects.hashCode(nothing());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Choice)) {
            return false;
        }
        Choice other = (Choice) obj;
        return Objects.equals(text(), other.text()) && Objects.equals(number(), other.number())
                && Objects.equals(nothing(), other.nothing());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToStringSerializer.serialize(this);
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Text":
            return Optional.ofNullable(clazz.cast(text()));
        case "Number":
            return Optional.ofNullable(clazz.cast(number()));
        case "Nothing":
            return Optional.ofNullable(clazz.cast(nothing()));
        default:
            return Optional.empty();
        }
    }

    /**
     * Create an instance of this class with {@link #text()} initialized to the given value.
     *
     * Sets the value of the Text property for this object.
     *
     * @param text
     *        The new value for the Text property for this object.
     */
    public static Choice fromText(String text) {
        return builder().text(text).build();
    }

    /**
     * Create an instance of this class with {@link #number()} initialized to the given value.
     *
     * Sets the value of the Number property for this object.
     *
     * @param number
     *        The new value for the Number property for this object.
     */
    public static Choice fromNumber(Integer number) {
        return builder().number(number).build();
    }

    /**
     * Create an instance of this class with {@link #nothing()} initialized to the given value.
     *
     * Sets the value of the Nothing property for this object.
     *
     * @param nothing
     *        The new value for the Nothing property for this object.
     */
    public static Choice fromNothing(Unit nothing) {
        return builder().nothing(nothing).build();
    }

    /**
     * Create an instance of this class with {@link #nothing()} initialized to the given value.
     *
     * Sets the value of the Nothing property for this object.
     *
     * @param nothing
     *        The new value for the Nothing property for this object.
     */
    public static Choice fromNothing(Consumer<Unit.Builder> nothing) {
        Unit.Builder builder = Unit.builder();
        nothing.accept(builder);
        return fromNothing(builder.build());
    }

    /**
     * Retrieve an enum value representing which member of this object is populated.
     *
     * When this class is returned in a service response, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if the
     * service returned a member that is only known to a newer SDK version.
     *
     * When this class is created directly in your code, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if zero
     * members are set, and {@code null} if more than one member is set.
     */
    public Type type() {
        return type;
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return DemoSharedSerde.SDK_FIELDS.fields(SCHEMA);
    }

    @Override
    public final Map<String, SdkField<?>> sdkFieldNameToField() {
        return DemoSharedSerde.SDK_FIELDS.fieldsByName(SCHEMA);
    }

    @Override
    public Schema sdkSchema() {
        return SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (this.text != null) {
            serializer.writeString(M_TEXT, this.text);
        }
        if (this.number != null) {
            serializer.writeInteger(M_NUMBER, this.number);
        }
        if (this.nothing != null) {
            serializer.writeStruct(M_NOTHING, this.nothing);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        switch (member.memberIndex()) {
        case 0:
            return (T) SchemaUtils.validateSameMember(M_TEXT, member, this.text);
        case 1:
            return (T) SchemaUtils.validateSameMember(M_NUMBER, member, this.number);
        case 2:
            return (T) SchemaUtils.validateSameMember(M_NOTHING, member, this.nothing);
        default:
            throw SchemaUtils.notAMember(SCHEMA, member);
        }
    }

    static ShapeBuilder<Choice> newShapeBuilder() {
        return new BuilderImpl();
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends SdkPojo, CopyableBuilder<Builder, Choice> {
        /**
         * Sets the value of the Text property for this object.
         *
         * @param text
         *        The new value for the Text property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder text(String text);

        /**
         * Sets the value of the Number property for this object.
         *
         * @param number
         *        The new value for the Number property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder number(Integer number);

        /**
         * Sets the value of the Nothing property for this object.
         *
         * @param nothing
         *        The new value for the Nothing property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nothing(Unit nothing);

        /**
         * Sets the value of the Nothing property for this object.
         *
         * This is a convenience method that creates an instance of the {@link Unit.Builder} avoiding the need to create
         * one manually via {@link Unit#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link Unit.Builder#build()} is called immediately and its result is
         * passed to {@link #nothing(Unit)}.
         * 
         * @param nothing
         *        a consumer that will call methods on {@link Unit.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #nothing(Unit)
         */
        default Builder nothing(Consumer<Unit.Builder> nothing) {
            return nothing(Unit.builder().applyMutation(nothing).build());
        }
    }

    static final class BuilderImpl implements Builder, ShapeBuilder<Choice> {
        String text;

        Integer number;

        Unit nothing;

        private Type type = Type.UNKNOWN_TO_SDK_VERSION;

        private Set<Type> setTypes = EnumSet.noneOf(Type.class);

        private BuilderImpl() {
        }

        private BuilderImpl(Choice model) {
            text(model.text);
            number(model.number);
            nothing(model.nothing);
        }

        public final String getText() {
            return text;
        }

        public final void setText(String text) {
            Object oldValue = this.text;
            this.text = text;
            handleUnionValueChange(Type.TEXT, oldValue, this.text);
        }

        @Override
        public final Builder text(String text) {
            Object oldValue = this.text;
            this.text = text;
            handleUnionValueChange(Type.TEXT, oldValue, this.text);
            return this;
        }

        public final Integer getNumber() {
            return number;
        }

        public final void setNumber(Integer number) {
            Object oldValue = this.number;
            this.number = number;
            handleUnionValueChange(Type.NUMBER, oldValue, this.number);
        }

        @Override
        public final Builder number(Integer number) {
            Object oldValue = this.number;
            this.number = number;
            handleUnionValueChange(Type.NUMBER, oldValue, this.number);
            return this;
        }

        public final Unit.Builder getNothing() {
            return nothing != null ? nothing.toBuilder() : null;
        }

        public final void setNothing(Unit.BuilderImpl nothing) {
            Object oldValue = this.nothing;
            this.nothing = nothing != null ? nothing.build() : null;
            handleUnionValueChange(Type.NOTHING, oldValue, this.nothing);
        }

        @Override
        public final Builder nothing(Unit nothing) {
            Object oldValue = this.nothing;
            this.nothing = nothing;
            handleUnionValueChange(Type.NOTHING, oldValue, this.nothing);
            return this;
        }

        @Override
        public Choice build() {
            return new Choice(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return DemoSharedSerde.SDK_FIELDS.fields(SCHEMA);
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return DemoSharedSerde.SDK_FIELDS.fieldsByName(SCHEMA);
        }

        @Override
        public Schema sdkSchema() {
            return SCHEMA;
        }

        @Override
        public BuilderImpl deserialize(ShapeDeserializer deserializer) {
            deserializer.readStruct(SCHEMA, this, InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public BuilderImpl deserializeMember(ShapeDeserializer deserializer, Schema schema) {
            deserializer.readStruct(schema.assertMemberTargetIs(SCHEMA), this, InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
            case 0:
                text((String) SchemaUtils.validateSameMember(M_TEXT, member, value));
                break;
            case 1:
                number((Integer) SchemaUtils.validateSameMember(M_NUMBER, member, value));
                break;
            case 2:
                nothing((Unit) SchemaUtils.validateSameMember(M_NOTHING, member, value));
                break;
            default:
                throw SchemaUtils.notAMember(SCHEMA, member);
            }
        }

        private final void handleUnionValueChange(Type type, Object oldValue, Object newValue) {
            if (this.type == type || oldValue == newValue) {
                return;
            }
            if (newValue == null || newValue instanceof SdkAutoConstructList || newValue instanceof SdkAutoConstructMap) {
                setTypes.remove(type);
            } else if (oldValue == null || oldValue instanceof SdkAutoConstructList || oldValue instanceof SdkAutoConstructMap) {
                setTypes.add(type);
            }
            if (setTypes.size() == 1) {
                this.type = setTypes.iterator().next();
            } else if (setTypes.isEmpty()) {
                this.type = Type.UNKNOWN_TO_SDK_VERSION;
            } else {
                this.type = null;
            }
        }

        private static final class InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<BuilderImpl> {
            static final InnerDeserializer INSTANCE = new InnerDeserializer();

            private InnerDeserializer() {
            }

            @Override
            public void accept(BuilderImpl builder, Schema member, ShapeDeserializer deserializer) {
                switch (member.memberIndex()) {
                case 0:
                    builder.text(deserializer.readString(member));
                    break;
                case 1:
                    builder.number(deserializer.readInteger(member));
                    break;
                case 2:
                    builder.nothing(Unit.newShapeBuilder().deserializeMember(deserializer, member).build());
                    break;
                default:
                    break;
                }
            }
        }
    }

    /**
     * @see Choice#type()
     */
    public enum Type {
        TEXT,

        NUMBER,

        NOTHING,

        UNKNOWN_TO_SDK_VERSION
    }
}
