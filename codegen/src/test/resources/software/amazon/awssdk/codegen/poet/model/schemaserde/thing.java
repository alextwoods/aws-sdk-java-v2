package software.amazon.awssdk.services.demo.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
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
public final class Thing implements SdkPojo, Serializable, ToCopyableBuilder<Thing.Builder, Thing>, SerializableStruct {
  /**
   * The schema of this shape.
   */
  public static final Schema SCHEMA = DemoSchemas.THING;

  private static final Schema M_NAME = SCHEMA.member("Name");

  private static final Schema M_SECRET = SCHEMA.member("Secret");

  private static final Schema M_COUNT = SCHEMA.member("Count");

  private static final Schema M_ENABLED = SCHEMA.member("Enabled");

  private static final Schema M_CHILDREN = SCHEMA.member("Children");

  private static final long serialVersionUID = 1L;

  private final String name;

  private final String secret;

  private final Long count;

  private final Boolean enabled;

  private final List<Thing> children;

  private Thing(BuilderImpl builder) {
    this.name = builder.name;
    this.secret = builder.secret;
    this.count = builder.count;
    this.enabled = builder.enabled;
    this.children = builder.children;
  }

  /**
   * Returns the value of the Name property for this object.
   * @return The value of the Name property for this object.
   */
  public final String name() {
    return name;
  }

  /**
   * Returns the value of the Secret property for this object.
   * @return The value of the Secret property for this object.
   */
  public final String secret() {
    return secret;
  }

  /**
   * Returns the value of the Count property for this object.
   * @return The value of the Count property for this object.
   */
  public final Long count() {
    return count;
  }

  /**
   * Returns the value of the Enabled property for this object.
   * @return The value of the Enabled property for this object.
   */
  public final Boolean enabled() {
    return enabled;
  }

  /**
   * For responses, this returns true if the service returned a value for the Children property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful because the SDK will never return a null collection or map, but you may need to differentiate between the service returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true if a value for the property was specified in the request builder, and false if a value was not specified.
   */
  public final boolean hasChildren() {
    return children != null && !(children instanceof SdkAutoConstructList);
  }

  /**
   * Returns the value of the Children property for this object.
   * <p>
   * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
   * </p>
   * <p>
   * This method will never return null. If you would like to know whether the service returned this field (so that you can differentiate between null and empty), you can use the {@link #hasChildren} method.
   * </p>
   * @return The value of the Children property for this object.
   */
  public final List<Thing> children() {
    return children;
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
    hashCode = 31 * hashCode + Objects.hashCode(name());
    hashCode = 31 * hashCode + Objects.hashCode(secret());
    hashCode = 31 * hashCode + Objects.hashCode(count());
    hashCode = 31 * hashCode + Objects.hashCode(enabled());
    hashCode = 31 * hashCode + Objects.hashCode(hasChildren() ? children() : null);
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
    if (!(obj instanceof Thing)) {
      return false;
    }
    Thing other = (Thing) obj;
    return Objects.equals(name(), other.name())&&Objects.equals(secret(), other.secret())&&Objects.equals(count(), other.count())&&Objects.equals(enabled(), other.enabled())&&hasChildren() == other.hasChildren() && Objects.equals(children(), other.children());
  }

  /**
   * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be redacted from this string using a placeholder value. 
   */
  @Override
  public final String toString() {
    return ToStringSerializer.serialize(this);
  }

  public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
    switch (fieldName) {
      case "Name":return Optional.ofNullable(clazz.cast(name()));
      case "Secret":return Optional.ofNullable(clazz.cast(secret()));
      case "Count":return Optional.ofNullable(clazz.cast(count()));
      case "Enabled":return Optional.ofNullable(clazz.cast(enabled()));
      case "Children":return Optional.ofNullable(clazz.cast(children()));
      default:return Optional.empty();
    }
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
    if (this.name != null) {
      serializer.writeString(M_NAME, this.name);
    }
    if (this.secret != null) {
      serializer.writeString(M_SECRET, this.secret);
    }
    if (this.count != null) {
      serializer.writeLong(M_COUNT, this.count);
    }
    if (this.enabled != null) {
      serializer.writeBoolean(M_ENABLED, this.enabled);
    }
    if (this.children != null && !(this.children instanceof SdkAutoConstructList)) {
      serializer.writeList(M_CHILDREN, this.children, this.children.size(), DemoSharedSerde.ThingListSerde.INSTANCE);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getMemberValue(Schema member) {
    switch (member.memberIndex()) {
      case 0:
        return (T) SchemaUtils.validateSameMember(M_NAME, member, this.name);
      case 1:
        return (T) SchemaUtils.validateSameMember(M_SECRET, member, this.secret);
      case 2:
        return (T) SchemaUtils.validateSameMember(M_COUNT, member, this.count);
      case 3:
        return (T) SchemaUtils.validateSameMember(M_ENABLED, member, this.enabled);
      case 4:
        return (T) SchemaUtils.validateSameMember(M_CHILDREN, member, this.children);
      default:
        throw SchemaUtils.notAMember(SCHEMA, member);
    }
  }

  static ShapeBuilder<Thing> newShapeBuilder() {
    return new BuilderImpl();
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends SdkPojo, CopyableBuilder<Builder, Thing> {
    /**
     * Sets the value of the Name property for this object.
     *
     * @param name The new value for the Name property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder name(String name);

    /**
     * Sets the value of the Secret property for this object.
     *
     * @param secret The new value for the Secret property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder secret(String secret);

    /**
     * Sets the value of the Count property for this object.
     *
     * @param count The new value for the Count property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder count(Long count);

    /**
     * Sets the value of the Enabled property for this object.
     *
     * @param enabled The new value for the Enabled property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder enabled(Boolean enabled);

    /**
     * Sets the value of the Children property for this object.
     *
     * @param children The new value for the Children property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder children(Collection<Thing> children);

    /**
     * Sets the value of the Children property for this object.
     *
     * @param children The new value for the Children property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder children(Thing... children);

    /**
     * Sets the value of the Children property for this object.
     *
     * This is a convenience method that creates an instance of the {@link software.amazon.awssdk.services.demo.model.Thing.Builder} avoiding the need to create one manually via {@link software.amazon.awssdk.services.demo.model.Thing#builder()}.
     *
     * <p>When the {@link Consumer} completes, {@link software.amazon.awssdk.services.demo.model.Thing.Builder#build()} is called immediately and its result is passed to {@link #children(List<Thing>)}.
     * @param children a consumer that will call methods on {@link software.amazon.awssdk.services.demo.model.Thing.Builder}
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see #children(java.util.Collection<Thing>)
     */
    Builder children(Consumer<Builder>... children);
  }

  static final class BuilderImpl implements Builder, ShapeBuilder<Thing> {
    String name;

    String secret;

    Long count;

    Boolean enabled;

    List<Thing> children = DefaultSdkAutoConstructList.getInstance();

    private BuilderImpl() {
    }

    private BuilderImpl(Thing model) {
      name(model.name);
      secret(model.secret);
      count(model.count);
      enabled(model.enabled);
      children(model.children);
    }

    public final String getName() {
      return name;
    }

    public final void setName(String name) {
      this.name = name;
    }

    @Override
    public final Builder name(String name) {
      this.name = name;
      return this;
    }

    public final String getSecret() {
      return secret;
    }

    public final void setSecret(String secret) {
      this.secret = secret;
    }

    @Override
    public final Builder secret(String secret) {
      this.secret = secret;
      return this;
    }

    public final Long getCount() {
      return count;
    }

    public final void setCount(Long count) {
      this.count = count;
    }

    @Override
    public final Builder count(Long count) {
      this.count = count;
      return this;
    }

    public final Boolean getEnabled() {
      return enabled;
    }

    public final void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public final Builder enabled(Boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public final List<Builder> getChildren() {
      List<Builder> result = ThingListCopier.copyToBuilder(this.children);if (result instanceof SdkAutoConstructList) {return null;}return result;
    }

    public final void setChildren(Collection<BuilderImpl> children) {
      this.children = ThingListCopier.copyFromBuilder(children);
    }

    @Override
    public final Builder children(Collection<Thing> children) {
      this.children = ThingListCopier.copy(children);
      return this;
    }

    @Override
    @SafeVarargs
    public final Builder children(Thing... children) {
      children(Arrays.asList(children));return this;
    }

    @Override
    @SafeVarargs
    public final Builder children(Consumer<Builder>... children) {
      children(Stream.of(children).map(c -> Thing.builder().applyMutation(c).build()).collect(Collectors.toList()));return this;
    }

    @Override
    public Thing build() {
      return new Thing(this);
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
    @SuppressWarnings("unchecked")
    public void setMemberValue(Schema member, Object value) {
      switch (member.memberIndex()) {
        case 0:
          name((String) SchemaUtils.validateSameMember(M_NAME, member, value));
          break;
        case 1:
          secret((String) SchemaUtils.validateSameMember(M_SECRET, member, value));
          break;
        case 2:
          count((Long) SchemaUtils.validateSameMember(M_COUNT, member, value));
          break;
        case 3:
          enabled((Boolean) SchemaUtils.validateSameMember(M_ENABLED, member, value));
          break;
        case 4:
          children((Collection<Thing>) SchemaUtils.validateSameMember(M_CHILDREN, member, value));
          break;
        default:
          throw SchemaUtils.notAMember(SCHEMA, member);
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
            builder.name = deserializer.readString(member);
            break;
          case 1:
            builder.secret = deserializer.readString(member);
            break;
          case 2:
            builder.count = deserializer.readLong(member);
            break;
          case 3:
            builder.enabled = deserializer.readBoolean(member);
            break;
          case 4:
            builder.children = DemoSharedSerde.ThingListSerde.read(member, deserializer);
            break;
          default:
            break;
        }
      }
    }
  }
}
