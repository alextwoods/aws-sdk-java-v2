package software.amazon.awssdk.services.demo.model;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class ThingNotFoundException extends DemoException implements ToCopyableBuilder<ThingNotFoundException.Builder, ThingNotFoundException>, SerializableStruct {
  /**
   * The schema of this shape.
   */
  public static final Schema SCHEMA = DemoSchemas.THING_NOT_FOUND;

  private static final Schema M_MESSAGE = SCHEMA.member("message");

  private static final Schema M_REASON = SCHEMA.member("Reason");

  private static final long serialVersionUID = 1L;

  private final String reason;

  private ThingNotFoundException(BuilderImpl builder) {
    super(builder);
    this.reason = builder.reason;
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

  /**
   * Returns the value of the Reason property for this object.
   * @return The value of the Reason property for this object.
   */
  public String reason() {
    return reason;
  }

  @Override
  public final List<SdkField<?>> sdkFields() {
    return DemoSharedSerde.RESPONSE_SDK_FIELDS.fields(SCHEMA);
  }

  @Override
  public final Map<String, SdkField<?>> sdkFieldNameToField() {
    return DemoSharedSerde.RESPONSE_SDK_FIELDS.fieldsByName(SCHEMA);
  }

  @Override
  public Schema sdkSchema() {
    return SCHEMA;
  }

  @Override
  public void serializeMembers(ShapeSerializer serializer) {
    if (rawMessage() != null) {
      serializer.writeString(M_MESSAGE, rawMessage());
    }
    if (this.reason != null) {
      serializer.writeString(M_REASON, this.reason);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getMemberValue(Schema member) {
    switch (member.memberIndex()) {
      case 0:
        return (T) SchemaUtils.validateSameMember(M_MESSAGE, member, rawMessage());
      case 1:
        return (T) SchemaUtils.validateSameMember(M_REASON, member, this.reason);
      default:
        throw SchemaUtils.notAMember(SCHEMA, member);
    }
  }

  static ShapeBuilder<ThingNotFoundException> newShapeBuilder() {
    return new BuilderImpl();
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends SdkPojo, CopyableBuilder<Builder, ThingNotFoundException>, DemoException.Builder {
    /**
     * Sets the value of the Reason property for this object.
     *
     * @param reason The new value for the Reason property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder reason(String reason);

    @Override
    Builder awsErrorDetails(AwsErrorDetails awsErrorDetails);

    @Override
    Builder message(String message);

    @Override
    Builder requestId(String requestId);

    @Override
    Builder statusCode(int statusCode);

    @Override
    Builder cause(Throwable cause);

    @Override
    Builder writableStackTrace(Boolean writableStackTrace);

    @Override
    Builder numAttempts(Integer numAttempts);
  }

  static final class BuilderImpl extends DemoException.BuilderImpl implements Builder, ShapeBuilder<ThingNotFoundException> {
    String reason;

    private BuilderImpl() {
    }

    private BuilderImpl(ThingNotFoundException model) {
      super(model);reason(model.reason);
    }

    public final String getReason() {
      return reason;
    }

    public final void setReason(String reason) {
      this.reason = reason;
    }

    @Override
    public final Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    @Override
    public BuilderImpl awsErrorDetails(AwsErrorDetails awsErrorDetails) {
      this.awsErrorDetails = awsErrorDetails;
      return this;
    }

    @Override
    public BuilderImpl message(String message) {
      this.message = message;
      return this;
    }

    @Override
    public BuilderImpl requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    @Override
    public BuilderImpl statusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    @Override
    public BuilderImpl cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    @Override
    public BuilderImpl writableStackTrace(Boolean writableStackTrace) {
      this.writableStackTrace = writableStackTrace;
      return this;
    }

    @Override
    public BuilderImpl numAttempts(Integer numAttempts) {
      this.numAttempts = numAttempts;
      return this;
    }

    @Override
    public ThingNotFoundException build() {
      return new ThingNotFoundException(this);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
      return DemoSharedSerde.RESPONSE_SDK_FIELDS.fields(SCHEMA);
    }

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
      return DemoSharedSerde.RESPONSE_SDK_FIELDS.fieldsByName(SCHEMA);
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
          message((String) SchemaUtils.validateSameMember(M_MESSAGE, member, value));
          break;
        case 1:
          reason((String) SchemaUtils.validateSameMember(M_REASON, member, value));
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
            builder.message(deserializer.readString(member));
            break;
          case 1:
            builder.reason = deserializer.readString(member);
            break;
          default:
            break;
        }
      }
    }
  }
}
