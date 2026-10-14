package software.amazon.awssdk.services.demo.model;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
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
public final class GetThingRequest extends DemoRequest implements ToCopyableBuilder<GetThingRequest.Builder, GetThingRequest>, SerializableStruct {
  /**
   * The schema of this shape.
   */
  public static final Schema SCHEMA = DemoSchemas.GET_THING_REQUEST;

  private static final Schema M_ID = SCHEMA.member("Id");

  private static final Schema M_TOKEN = SCHEMA.member("Token");

  private static final Schema M_VERSION = SCHEMA.member("Version");

  private static final Schema M_TAGS = SCHEMA.member("Tags");

  private static final Schema M_NAMES = SCHEMA.member("Names");

  private static final Schema M_COLOR = SCHEMA.member("Color");

  private static final Schema M_WHEN = SCHEMA.member("When");

  private static final Schema M_DATA = SCHEMA.member("Data");

  private static final Schema M_NESTED = SCHEMA.member("Nested");

  private final String id;

  private final String token;

  private final Integer version;

  private final Map<String, List<String>> tags;

  private final List<String> names;

  private final String color;

  private final Instant when;

  private final SdkBytes data;

  private final Thing nested;

  private GetThingRequest(BuilderImpl builder) {
    super(builder);
    this.id = builder.id;
    this.token = builder.token;
    this.version = builder.version;
    this.tags = builder.tags;
    this.names = builder.names;
    this.color = builder.color;
    this.when = builder.when;
    this.data = builder.data;
    this.nested = builder.nested;
  }

  /**
   * Returns the value of the Id property for this object.
   * @return The value of the Id property for this object.
   */
  public final String id() {
    return id;
  }

  /**
   * Returns the value of the Token property for this object.
   * @return The value of the Token property for this object.
   */
  public final String token() {
    return token;
  }

  /**
   * Returns the value of the Version property for this object.
   * @return The value of the Version property for this object.
   */
  public final Integer version() {
    return version;
  }

  /**
   * For responses, this returns true if the service returned a value for the Tags property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful because the SDK will never return a null collection or map, but you may need to differentiate between the service returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true if a value for the property was specified in the request builder, and false if a value was not specified.
   */
  public final boolean hasTags() {
    return tags != null && !(tags instanceof SdkAutoConstructMap);
  }

  /**
   * Returns the value of the Tags property for this object.
   * <p>
   * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
   * </p>
   * <p>
   * This method will never return null. If you would like to know whether the service returned this field (so that you can differentiate between null and empty), you can use the {@link #hasTags} method.
   * </p>
   * @return The value of the Tags property for this object.
   */
  public final Map<String, List<String>> tags() {
    return tags;
  }

  /**
   * For responses, this returns true if the service returned a value for the Names property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful because the SDK will never return a null collection or map, but you may need to differentiate between the service returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true if a value for the property was specified in the request builder, and false if a value was not specified.
   */
  public final boolean hasNames() {
    return names != null && !(names instanceof SdkAutoConstructList);
  }

  /**
   * Returns the value of the Names property for this object.
   * <p>
   * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
   * </p>
   * <p>
   * This method will never return null. If you would like to know whether the service returned this field (so that you can differentiate between null and empty), you can use the {@link #hasNames} method.
   * </p>
   * @return The value of the Names property for this object.
   */
  public final List<String> names() {
    return names;
  }

  /**
   * Returns the value of the Color property for this object.
   * <p>
   * If the service returns an enum value that is not available in the current SDK version, {@link #color} will return {@link Color#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from {@link #colorAsString}.
   * </p>
   * @return The value of the Color property for this object.
   * @see Color
   */
  public final Color color() {
    return Color.fromValue(color);
  }

  /**
   * Returns the value of the Color property for this object.
   * <p>
   * If the service returns an enum value that is not available in the current SDK version, {@link #color} will return {@link Color#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from {@link #colorAsString}.
   * </p>
   * @return The value of the Color property for this object.
   * @see Color
   */
  public final String colorAsString() {
    return color;
  }

  /**
   * Returns the value of the When property for this object.
   * @return The value of the When property for this object.
   */
  public final Instant when() {
    return when;
  }

  /**
   * Returns the value of the Data property for this object.
   * @return The value of the Data property for this object.
   */
  public final SdkBytes data() {
    return data;
  }

  /**
   * Returns the value of the Nested property for this object.
   * @return The value of the Nested property for this object.
   */
  public final Thing nested() {
    return nested;
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
    hashCode = 31 * hashCode + Objects.hashCode(id());
    hashCode = 31 * hashCode + Objects.hashCode(token());
    hashCode = 31 * hashCode + Objects.hashCode(version());
    hashCode = 31 * hashCode + Objects.hashCode(hasTags() ? tags() : null);
    hashCode = 31 * hashCode + Objects.hashCode(hasNames() ? names() : null);
    hashCode = 31 * hashCode + Objects.hashCode(colorAsString());
    hashCode = 31 * hashCode + Objects.hashCode(when());
    hashCode = 31 * hashCode + Objects.hashCode(data());
    hashCode = 31 * hashCode + Objects.hashCode(nested());
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
    if (!(obj instanceof GetThingRequest)) {
      return false;
    }
    GetThingRequest other = (GetThingRequest) obj;
    return Objects.equals(id(), other.id())&&Objects.equals(token(), other.token())&&Objects.equals(version(), other.version())&&hasTags() == other.hasTags() && Objects.equals(tags(), other.tags())&&hasNames() == other.hasNames() && Objects.equals(names(), other.names())&&Objects.equals(colorAsString(), other.colorAsString())&&Objects.equals(when(), other.when())&&Objects.equals(data(), other.data())&&Objects.equals(nested(), other.nested());
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
      case "Id":return Optional.ofNullable(clazz.cast(id()));
      case "Token":return Optional.ofNullable(clazz.cast(token()));
      case "Version":return Optional.ofNullable(clazz.cast(version()));
      case "Tags":return Optional.ofNullable(clazz.cast(tags()));
      case "Names":return Optional.ofNullable(clazz.cast(names()));
      case "Color":return Optional.ofNullable(clazz.cast(colorAsString()));
      case "When":return Optional.ofNullable(clazz.cast(when()));
      case "Data":return Optional.ofNullable(clazz.cast(data()));
      case "Nested":return Optional.ofNullable(clazz.cast(nested()));
      default:return Optional.empty();
    }
  }

  @Override
  public final List<SdkField<?>> sdkFields() {
    return DemoSharedSerde.REQUEST_SDK_FIELDS.fields(SCHEMA);
  }

  @Override
  public final Map<String, SdkField<?>> sdkFieldNameToField() {
    return DemoSharedSerde.REQUEST_SDK_FIELDS.fieldsByName(SCHEMA);
  }

  @Override
  public Schema sdkSchema() {
    return SCHEMA;
  }

  @Override
  public void serializeMembers(ShapeSerializer serializer) {
    if (this.id != null) {
      serializer.writeString(M_ID, this.id);
    }
    if (this.token != null) {
      serializer.writeString(M_TOKEN, this.token);
    }
    if (this.version != null) {
      serializer.writeInteger(M_VERSION, this.version);
    }
    if (this.tags != null && !(this.tags instanceof SdkAutoConstructMap)) {
      serializer.writeMap(M_TAGS, this.tags, this.tags.size(), DemoSharedSerde.TagMapSerde.INSTANCE);
    }
    if (this.names != null && !(this.names instanceof SdkAutoConstructList)) {
      serializer.writeList(M_NAMES, this.names, this.names.size(), DemoSharedSerde.NameListSerde.INSTANCE);
    }
    if (this.color != null) {
      serializer.writeString(M_COLOR, this.color);
    }
    if (this.when != null) {
      serializer.writeTimestamp(M_WHEN, this.when);
    }
    if (this.data != null) {
      serializer.writeBlob(M_DATA, this.data.asByteBuffer());
    }
    if (this.nested != null) {
      serializer.writeStruct(M_NESTED, this.nested);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getMemberValue(Schema member) {
    switch (member.memberIndex()) {
      case 0:
        return (T) SchemaUtils.validateSameMember(M_ID, member, this.id);
      case 1:
        return (T) SchemaUtils.validateSameMember(M_TOKEN, member, this.token);
      case 2:
        return (T) SchemaUtils.validateSameMember(M_VERSION, member, this.version);
      case 3:
        return (T) SchemaUtils.validateSameMember(M_TAGS, member, this.tags);
      case 4:
        return (T) SchemaUtils.validateSameMember(M_NAMES, member, this.names);
      case 5:
        return (T) SchemaUtils.validateSameMember(M_COLOR, member, this.color);
      case 6:
        return (T) SchemaUtils.validateSameMember(M_WHEN, member, this.when);
      case 7:
        return (T) SchemaUtils.validateSameMember(M_DATA, member, this.data);
      case 8:
        return (T) SchemaUtils.validateSameMember(M_NESTED, member, this.nested);
      default:
        throw SchemaUtils.notAMember(SCHEMA, member);
    }
  }

  static ShapeBuilder<GetThingRequest> newShapeBuilder() {
    return new BuilderImpl();
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends DemoRequest.Builder, SdkPojo, CopyableBuilder<Builder, GetThingRequest> {
    /**
     * Sets the value of the Id property for this object.
     *
     * @param id The new value for the Id property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder id(String id);

    /**
     * Sets the value of the Token property for this object.
     *
     * @param token The new value for the Token property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder token(String token);

    /**
     * Sets the value of the Version property for this object.
     *
     * @param version The new value for the Version property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder version(Integer version);

    /**
     * Sets the value of the Tags property for this object.
     *
     * @param tags The new value for the Tags property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder tags(Map<String, ? extends Collection<String>> tags);

    /**
     * Sets the value of the Names property for this object.
     *
     * @param names The new value for the Names property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder names(Collection<String> names);

    /**
     * Sets the value of the Names property for this object.
     *
     * @param names The new value for the Names property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder names(String... names);

    /**
     * Sets the value of the Color property for this object.
     *
     * @param color The new value for the Color property for this object.
     * @see Color
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see Color
     */
    Builder color(String color);

    /**
     * Sets the value of the Color property for this object.
     *
     * @param color The new value for the Color property for this object.
     * @see Color
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see Color
     */
    Builder color(Color color);

    /**
     * Sets the value of the When property for this object.
     *
     * @param when The new value for the When property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder when(Instant when);

    /**
     * Sets the value of the Data property for this object.
     *
     * @param data The new value for the Data property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder data(SdkBytes data);

    /**
     * Sets the value of the Nested property for this object.
     *
     * @param nested The new value for the Nested property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder nested(Thing nested);

    /**
     * Sets the value of the Nested property for this object.
     *
     * This is a convenience method that creates an instance of the {@link Thing.Builder} avoiding the need to create one manually via {@link Thing#builder()}.
     *
     * <p>When the {@link Consumer} completes, {@link Thing.Builder#build()} is called immediately and its result is passed to {@link #nested(Thing)}.
     * @param nested a consumer that will call methods on {@link Thing.Builder}
     * @return Returns a reference to this object so that method calls can be chained together.
     * @see #nested(Thing)
     */
    default Builder nested(Consumer<Thing.Builder> nested) {
      return nested(Thing.builder().applyMutation(nested).build());
    }

    @Override
    Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

    @Override
    Builder overrideConfiguration(
        Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
  }

  static final class BuilderImpl extends DemoRequest.BuilderImpl implements Builder, ShapeBuilder<GetThingRequest> {
    String id;

    String token;

    Integer version;

    Map<String, List<String>> tags = DefaultSdkAutoConstructMap.getInstance();

    List<String> names = DefaultSdkAutoConstructList.getInstance();

    String color;

    Instant when;

    SdkBytes data;

    Thing nested;

    private BuilderImpl() {
    }

    private BuilderImpl(GetThingRequest model) {
      super(model);id(model.id);
      token(model.token);
      version(model.version);
      tags(model.tags);
      names(model.names);
      color(model.color);
      when(model.when);
      data(model.data);
      nested(model.nested);
    }

    public final String getId() {
      return id;
    }

    public final void setId(String id) {
      this.id = id;
    }

    @Override
    public final Builder id(String id) {
      this.id = id;
      return this;
    }

    public final String getToken() {
      return token;
    }

    public final void setToken(String token) {
      this.token = token;
    }

    @Override
    public final Builder token(String token) {
      this.token = token;
      return this;
    }

    public final Integer getVersion() {
      return version;
    }

    public final void setVersion(Integer version) {
      this.version = version;
    }

    @Override
    public final Builder version(Integer version) {
      this.version = version;
      return this;
    }

    public final Map<String, ? extends Collection<String>> getTags() {
      if (tags instanceof SdkAutoConstructMap) {return null;}return tags;
    }

    public final void setTags(Map<String, ? extends Collection<String>> tags) {
      this.tags = TagMapCopier.copy(tags);
    }

    @Override
    public final Builder tags(Map<String, ? extends Collection<String>> tags) {
      this.tags = TagMapCopier.copy(tags);
      return this;
    }

    public final Collection<String> getNames() {
      if (names instanceof SdkAutoConstructList) {return null;}return names;
    }

    public final void setNames(Collection<String> names) {
      this.names = NameListCopier.copy(names);
    }

    @Override
    public final Builder names(Collection<String> names) {
      this.names = NameListCopier.copy(names);
      return this;
    }

    @Override
    @SafeVarargs
    public final Builder names(String... names) {
      names(Arrays.asList(names));return this;
    }

    public final String getColor() {
      return color;
    }

    public final void setColor(String color) {
      this.color = color;
    }

    @Override
    public final Builder color(String color) {
      this.color = color;
      return this;
    }

    @Override
    public final Builder color(Color color) {
      this.color(color == null ? null : color.toString());
      return this;
    }

    public final Instant getWhen() {
      return when;
    }

    public final void setWhen(Instant when) {
      this.when = when;
    }

    @Override
    public final Builder when(Instant when) {
      this.when = when;
      return this;
    }

    public final ByteBuffer getData() {
      return data == null ? null : data.asByteBuffer();
    }

    public final void setData(ByteBuffer data) {
      data(data == null ? null : SdkBytes.fromByteBuffer(data));
    }

    @Override
    public final Builder data(SdkBytes data) {
      this.data = data;
      return this;
    }

    public final Thing.Builder getNested() {
      return nested != null ? nested.toBuilder() : null;
    }

    public final void setNested(Thing.BuilderImpl nested) {
      this.nested = nested != null ? nested.build() : null;
    }

    @Override
    public final Builder nested(Thing nested) {
      this.nested = nested;
      return this;
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
    public GetThingRequest build() {
      return new GetThingRequest(this);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
      return DemoSharedSerde.REQUEST_SDK_FIELDS.fields(SCHEMA);
    }

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
      return DemoSharedSerde.REQUEST_SDK_FIELDS.fieldsByName(SCHEMA);
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
          id((String) SchemaUtils.validateSameMember(M_ID, member, value));
          break;
        case 1:
          token((String) SchemaUtils.validateSameMember(M_TOKEN, member, value));
          break;
        case 2:
          version((Integer) SchemaUtils.validateSameMember(M_VERSION, member, value));
          break;
        case 3:
          tags((Map<String, ? extends Collection<String>>) SchemaUtils.validateSameMember(M_TAGS, member, value));
          break;
        case 4:
          names((Collection<String>) SchemaUtils.validateSameMember(M_NAMES, member, value));
          break;
        case 5:
          color((String) SchemaUtils.validateSameMember(M_COLOR, member, value));
          break;
        case 6:
          when((Instant) SchemaUtils.validateSameMember(M_WHEN, member, value));
          break;
        case 7:
          data((SdkBytes) SchemaUtils.validateSameMember(M_DATA, member, value));
          break;
        case 8:
          nested((Thing) SchemaUtils.validateSameMember(M_NESTED, member, value));
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
            builder.id = deserializer.readString(member);
            break;
          case 1:
            builder.token = deserializer.readString(member);
            break;
          case 2:
            builder.version = deserializer.readInteger(member);
            break;
          case 3:
            builder.tags = DemoSharedSerde.TagMapSerde.read(member, deserializer);
            break;
          case 4:
            builder.names = DemoSharedSerde.NameListSerde.read(member, deserializer);
            break;
          case 5:
            builder.color = deserializer.readString(member);
            break;
          case 6:
            builder.when = deserializer.readTimestamp(member);
            break;
          case 7:
            builder.data = SchemaSerdeSupport.toSdkBytes(deserializer.readBlob(member));
            break;
          case 8:
            builder.nested = Thing.newShapeBuilder().deserializeMember(deserializer, member).build();
            break;
          default:
            break;
        }
      }
    }
  }
}
