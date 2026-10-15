package software.amazon.awssdk.services.shared.model;

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
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.ToStringSerializer;
import software.amazon.awssdk.schema.traits.RequiredTrait;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class EchoRequest extends SharedRequest implements ToCopyableBuilder<EchoRequest.Builder, EchoRequest>, SerializableStruct {
  /**
   * The schema of this shape.
   */
  public static final Schema SCHEMA = Schema.structureBuilder(ShapeId.fromParts("shared", "EchoInputOutput"))
    .putMember("Name", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
    .putMember("Thing", SharedSchemas.THING)
    .putMember("Things", SharedSchemas.THING_LIST)
    .builderSupplier(EchoRequest::newShapeBuilder)
    .shapeClass(EchoRequest.class)
    .build();

  private static final Schema M_NAME = SCHEMA.member("Name");

  private static final Schema M_THING = SCHEMA.member("Thing");

  private static final Schema M_THINGS = SCHEMA.member("Things");

  private final String name;

  private final Thing thing;

  private final List<Thing> things;

  private EchoRequest(BuilderImpl builder) {
    super(builder);
    this.name = builder.name;
    this.thing = builder.thing;
    this.things = builder.things;
  }

  /**
   * Returns the value of the Name property for this object.
   * @return The value of the Name property for this object.
   */
  public final String name() {
    return name;
  }

  /**
   * Returns the value of the Thing property for this object.
   * @return The value of the Thing property for this object.
   */
  public final Thing thing() {
    return thing;
  }

  /**
   * For responses, this returns true if the service returned a value for the Things property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful because the SDK will never return a null collection or map, but you may need to differentiate between the service returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true if a value for the property was specified in the request builder, and false if a value was not specified.
   */
  public final boolean hasThings() {
    return things != null && !(things instanceof SdkAutoConstructList);
  }

  /**
   * Returns the value of the Things property for this object.
   * <p>
   * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
   * </p>
   * <p>
   * This method will never return null. If you would like to know whether the service returned this field (so that you can differentiate between null and empty), you can use the {@link #hasThings} method.
   * </p>
   * @return The value of the Things property for this object.
   */
  public final List<Thing> things() {
    return things;
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
    hashCode = 31 * hashCode + super.hashCode();
    hashCode = 31 * hashCode + Objects.hashCode(name());
    hashCode = 31 * hashCode + Objects.hashCode(thing());
    hashCode = 31 * hashCode + Objects.hashCode(hasThings() ? things() : null);
    return hashCode;
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj) && equalsBySdkFields(obj);
  }

  @Override
  public final boolean equalsBySdkFields(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof EchoRequest)) {
      return false;
    }
    EchoRequest other = (EchoRequest) obj;
    return Objects.equals(name(), other.name())&&Objects.equals(thing(), other.thing())&&hasThings() == other.hasThings() && Objects.equals(things(), other.things());
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
      case "Thing":return Optional.ofNullable(clazz.cast(thing()));
      case "Things":return Optional.ofNullable(clazz.cast(things()));
      default:return Optional.empty();
    }
  }

  @Override
  public final List<SdkField<?>> sdkFields() {
    return SharedSharedSerde.SDK_FIELDS.fields(SCHEMA);
  }

  @Override
  public final Map<String, SdkField<?>> sdkFieldNameToField() {
    return SharedSharedSerde.SDK_FIELDS.fieldsByName(SCHEMA);
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
    if (this.thing != null) {
      serializer.writeStruct(M_THING, this.thing);
    }
    if (this.things != null && !(this.things instanceof SdkAutoConstructList)) {
      serializer.writeList(M_THINGS, this.things, this.things.size(), SharedSharedSerde.ThingListSerde.INSTANCE);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getMemberValue(Schema member) {
    switch (member.memberIndex()) {
      case 0:
        return (T) SchemaUtils.validateSameMember(M_NAME, member, this.name);
      case 1:
        return (T) SchemaUtils.validateSameMember(M_THING, member, this.thing);
      case 2:
        return (T) SchemaUtils.validateSameMember(M_THINGS, member, this.things);
      default:
        throw SchemaUtils.notAMember(SCHEMA, member);
    }
  }

  static ShapeBuilder<EchoRequest> newShapeBuilder() {
    return new BuilderImpl();
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends SharedRequest.Builder, SdkPojo, CopyableBuilder<Builder, EchoRequest> {
    /**
     * Sets the value of the Name property for this object.
     *
     * @param name The new value for the Name property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder name(String name);

    /**
     * Sets the value of the Thing property for this object.
     *
     * @param thing The new value for the Thing property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder thing(Thing thing);

    /**
     * Sets the value of the Thing property for this object.
     *
     * This is a convenience method that creates an instance of the {@link Thing.Builder} avoiding the need to create one manually via {@link Thing#builder()}.
     *
     * <p>When the {@link Consumer} completes, {@link Thing.Builder#build()} is called immediately and its result is passed to {@link #thing(Thing)}.
     * @param thing a consumer that will call methods on {@link Thing.Builder}
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see #thing(Thing)
     */
    default Builder thing(Consumer<Thing.Builder> thing) {
      return thing(Thing.builder().applyMutation(thing).build());
    }

    /**
     * Sets the value of the Things property for this object.
     *
     * @param things The new value for the Things property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder things(Collection<Thing> things);

    /**
     * Sets the value of the Things property for this object.
     *
     * @param things The new value for the Things property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder things(Thing... things);

    /**
     * Sets the value of the Things property for this object.
     *
     * This is a convenience method that creates an instance of the {@link software.amazon.awssdk.services.shared.model.Thing.Builder} avoiding the need to create one manually via {@link software.amazon.awssdk.services.shared.model.Thing#builder()}.
     *
     * <p>When the {@link Consumer} completes, {@link software.amazon.awssdk.services.shared.model.Thing.Builder#build()} is called immediately and its result is passed to {@link #things(List<Thing>)}.
     * @param things a consumer that will call methods on {@link software.amazon.awssdk.services.shared.model.Thing.Builder}
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see #things(java.util.Collection<Thing>)
     */
    Builder things(Consumer<Thing.Builder>... things);

    @Override
    Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

    @Override
    Builder overrideConfiguration(
        Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
  }

  static final class BuilderImpl extends SharedRequest.BuilderImpl implements Builder, ShapeBuilder<EchoRequest> {
    String name;

    Thing thing;

    List<Thing> things = DefaultSdkAutoConstructList.getInstance();

    private BuilderImpl() {
    }

    private BuilderImpl(EchoRequest model) {
      super(model);name(model.name);
      thing(model.thing);
      things(model.things);
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

    public final Thing.Builder getThing() {
      return thing != null ? thing.toBuilder() : null;
    }

    public final void setThing(Thing.BuilderImpl thing) {
      this.thing = thing != null ? thing.build() : null;
    }

    @Override
    public final Builder thing(Thing thing) {
      this.thing = thing;
      return this;
    }

    public final List<Thing.Builder> getThings() {
      List<Thing.Builder> result = ThingListCopier.copyToBuilder(this.things);if (result instanceof SdkAutoConstructList) {return null;}return result;
    }

    public final void setThings(Collection<Thing.BuilderImpl> things) {
      this.things = ThingListCopier.copyFromBuilder(things);
    }

    @Override
    public final Builder things(Collection<Thing> things) {
      this.things = ThingListCopier.copy(things);
      return this;
    }

    @Override
    @SafeVarargs
    public final Builder things(Thing... things) {
      things(Arrays.asList(things));return this;
    }

    @Override
    @SafeVarargs
    public final Builder things(Consumer<Thing.Builder>... things) {
      things(Stream.of(things).map(c -> Thing.builder().applyMutation(c).build()).collect(Collectors.toList()));return this;
    }

    @Override
    public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
      super.overrideConfiguration(overrideConfiguration);
      return this;
    }

    @Override
    public Builder overrideConfiguration(
        Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
      super.overrideConfiguration(builderConsumer);
      return this;
    }

    @Override
    public EchoRequest build() {
      return new EchoRequest(this);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
      return SharedSharedSerde.SDK_FIELDS.fields(SCHEMA);
    }

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
      return SharedSharedSerde.SDK_FIELDS.fieldsByName(SCHEMA);
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
          thing((Thing) SchemaUtils.validateSameMember(M_THING, member, value));
          break;
        case 2:
          things((Collection<Thing>) SchemaUtils.validateSameMember(M_THINGS, member, value));
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
            builder.thing = Thing.newShapeBuilder().deserializeMember(deserializer, member).build();
            break;
          case 2:
            builder.things = SharedSharedSerde.ThingListSerde.read(member, deserializer);
            break;
          default:
            break;
        }
      }
    }
  }
}
