package software.amazon.awssdk.services.demo.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaUtils;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.ToStringSerializer;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public final class DeleteThingResponse extends DemoResponse implements ToCopyableBuilder<DeleteThingResponse.Builder, DeleteThingResponse>, SerializableStruct {
  /**
   * The schema of this shape.
   */
  public static final Schema SCHEMA = Schema.structureBuilder(ShapeId.fromParts("demo", "DeleteThingResponse"), UnitTypeTrait.INSTANCE).builderSupplier(DeleteThingResponse::newShapeBuilder).shapeClass(DeleteThingResponse.class).build();

  private DeleteThingResponse(BuilderImpl builder) {
    super(builder);
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
    if (!(obj instanceof DeleteThingResponse)) {
      return false;
    }
    return true;
  }

  /**
   * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be redacted from this string using a placeholder value. 
   */
  @Override
  public final String toString() {
    return ToStringSerializer.serialize(this);
  }

  public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
    return Optional.empty();
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
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getMemberValue(Schema member) {
    throw SchemaUtils.notAMember(SCHEMA, member);
  }

  static ShapeBuilder<DeleteThingResponse> newShapeBuilder() {
    return new BuilderImpl();
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends DemoResponse.Builder, SdkPojo, CopyableBuilder<Builder, DeleteThingResponse> {
  }

  static final class BuilderImpl extends DemoResponse.BuilderImpl implements Builder, ShapeBuilder<DeleteThingResponse> {
    private BuilderImpl() {
    }

    private BuilderImpl(DeleteThingResponse model) {
      super(model);
    }

    @Override
    public DeleteThingResponse build() {
      return new DeleteThingResponse(this);
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
      throw SchemaUtils.notAMember(SCHEMA, member);
    }

    private static final class InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<BuilderImpl> {
      static final InnerDeserializer INSTANCE = new InnerDeserializer();

      private InnerDeserializer() {
      }

      @Override
      public void accept(BuilderImpl builder, Schema member, ShapeDeserializer deserializer) {
      }
    }
  }
}
