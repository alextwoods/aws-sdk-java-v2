package software.amazon.awssdk.services.demo.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.core.schema.SdkFieldsFromSchema;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Schema serde code shared by the model classes of the service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DemoSharedSerde {
  static final SdkFieldsFromSchema SDK_FIELDS = SdkFieldsFromSchema.builder().wireNames(SdkFieldsFromSchema.WireNaming.JSON).build();

  static final SdkFieldsFromSchema REQUEST_SDK_FIELDS = SdkFieldsFromSchema.builder().wireNames(SdkFieldsFromSchema.WireNaming.JSON).httpBindings(SdkFieldsFromSchema.HttpBindingMode.REQUEST).build();

  static final SdkFieldsFromSchema RESPONSE_SDK_FIELDS = SdkFieldsFromSchema.builder().wireNames(SdkFieldsFromSchema.WireNaming.JSON).httpBindings(SdkFieldsFromSchema.HttpBindingMode.RESPONSE).build();

  private DemoSharedSerde() {
  }

  /**
   * Writes and reads {@code demo#NameList}.
   */
  static final class NameListSerde implements BiConsumer<List<String>, ShapeSerializer>, ShapeDeserializer.ListMemberConsumer<List<String>> {
    static final NameListSerde INSTANCE = new NameListSerde();

    static final Schema MEMBER = DemoSchemas.NAME_LIST.listMember();

    private NameListSerde() {
    }

    @Override
    public void accept(List<String> values, ShapeSerializer serializer) {
      for (String value : values) {
        if (value == null) {
          serializer.writeNull(MEMBER);
        } else {
          serializer.writeString(MEMBER, value);
        }
      }
    }

    @Override
    public void accept(List<String> values, ShapeDeserializer deserializer) {
      if (deserializer.isNull()) {
        values.add(deserializer.readNull());
      } else {
        values.add(deserializer.readString(MEMBER));
      }
    }

    static List<String> read(Schema schema, ShapeDeserializer deserializer) {
      List<String> values = SchemaSerdeSupport.newList(deserializer);
      deserializer.readList(schema, values, INSTANCE);
      return Collections.unmodifiableList(values);
    }
  }

  /**
   * Writes and reads {@code demo#SecretList}.
   */
  static final class SecretListSerde implements BiConsumer<List<String>, ShapeSerializer>, ShapeDeserializer.ListMemberConsumer<List<String>> {
    static final SecretListSerde INSTANCE = new SecretListSerde();

    static final Schema MEMBER = DemoSchemas.SECRET_LIST.listMember();

    private SecretListSerde() {
    }

    @Override
    public void accept(List<String> values, ShapeSerializer serializer) {
      for (String value : values) {
        if (value == null) {
          serializer.writeNull(MEMBER);
        } else {
          serializer.writeString(MEMBER, value);
        }
      }
    }

    @Override
    public void accept(List<String> values, ShapeDeserializer deserializer) {
      if (deserializer.isNull()) {
        values.add(deserializer.readNull());
      } else {
        values.add(deserializer.readString(MEMBER));
      }
    }

    static List<String> read(Schema schema, ShapeDeserializer deserializer) {
      List<String> values = SchemaSerdeSupport.newList(deserializer);
      deserializer.readList(schema, values, INSTANCE);
      return Collections.unmodifiableList(values);
    }
  }

  /**
   * Writes and reads {@code demo#TagMap}.
   */
  static final class TagMapSerde implements BiConsumer<Map<String, List<String>>, MapSerializer>, ShapeDeserializer.MapMemberConsumer<String, Map<String, List<String>>> {
    static final TagMapSerde INSTANCE = new TagMapSerde();

    static final Schema KEY = DemoSchemas.TAG_MAP.mapKeyMember();

    static final Schema VALUE = DemoSchemas.TAG_MAP.mapValueMember();

    private TagMapSerde() {
    }

    @Override
    public void accept(Map<String, List<String>> values, MapSerializer serializer) {
      for (Map.Entry<String, List<String>> entry : values.entrySet()) {
        serializer.writeEntry(KEY, entry.getKey(), entry.getValue(), ValueWriter.INSTANCE);
      }
    }

    @Override
    public void accept(Map<String, List<String>> values, String key,
        ShapeDeserializer deserializer) {
      if (deserializer.isNull()) {
        values.put(key, deserializer.readNull());
      } else {
        values.put(key, NameListSerde.read(VALUE, deserializer));
      }
    }

    static Map<String, List<String>> read(Schema schema, ShapeDeserializer deserializer) {
      Map<String, List<String>> values = SchemaSerdeSupport.newMap(deserializer);
      deserializer.readStringMap(schema, values, INSTANCE);
      return Collections.unmodifiableMap(values);
    }

    private static final class ValueWriter implements BiConsumer<List<String>, ShapeSerializer> {
      static final ValueWriter INSTANCE = new ValueWriter();

      private ValueWriter() {
      }

      @Override
      public void accept(List<String> value, ShapeSerializer serializer) {
        if (value == null) {
          serializer.writeNull(VALUE);
        } else {
          serializer.writeList(VALUE, value, value.size(), NameListSerde.INSTANCE);
        }
      }
    }
  }

  /**
   * Writes and reads {@code demo#ThingList}.
   */
  static final class ThingListSerde implements BiConsumer<List<Thing>, ShapeSerializer>, ShapeDeserializer.ListMemberConsumer<List<Thing>> {
    static final ThingListSerde INSTANCE = new ThingListSerde();

    static final Schema MEMBER = DemoSchemas.THING_LIST.listMember();

    private ThingListSerde() {
    }

    @Override
    public void accept(List<Thing> values, ShapeSerializer serializer) {
      for (Thing value : values) {
        if (value == null) {
          serializer.writeNull(MEMBER);
        } else {
          serializer.writeStruct(MEMBER, value);
        }
      }
    }

    @Override
    public void accept(List<Thing> values, ShapeDeserializer deserializer) {
      if (deserializer.isNull()) {
        values.add(deserializer.readNull());
      } else {
        values.add(Thing.newShapeBuilder().deserializeMember(deserializer, MEMBER).build());
      }
    }

    static List<Thing> read(Schema schema, ShapeDeserializer deserializer) {
      List<Thing> values = SchemaSerdeSupport.newList(deserializer);
      deserializer.readList(schema, values, INSTANCE);
      return Collections.unmodifiableList(values);
    }
  }
}
