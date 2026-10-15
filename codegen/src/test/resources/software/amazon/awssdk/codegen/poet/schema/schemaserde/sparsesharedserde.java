package software.amazon.awssdk.services.sparse.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.core.schema.SdkFieldsFromSchema;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Schema serde code shared by the model classes of the service.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class SparseSharedSerde {
  static final SdkFieldsFromSchema SDK_FIELDS = SdkFieldsFromSchema.builder().wireNames(SdkFieldsFromSchema.WireNaming.MEMBER).build();

  private SparseSharedSerde() {
  }

  /**
   * Writes and reads {@code sparse#DenseSetMap}.
   */
  static final class DenseSetMapSerde implements BiConsumer<Map<String, List<String>>, MapSerializer>, ShapeDeserializer.MapMemberConsumer<String, Map<String, List<String>>> {
    static final DenseSetMapSerde INSTANCE = new DenseSetMapSerde();

    static final Schema KEY = SparseSchemas.DENSE_SET_MAP.mapKeyMember();

    static final Schema VALUE = SparseSchemas.DENSE_SET_MAP.mapValueMember();

    private DenseSetMapSerde() {
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
        values.put(key, StringSetSerde.read(VALUE, deserializer));
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
          serializer.writeList(VALUE, value, value.size(), StringSetSerde.INSTANCE);
        }
      }
    }
  }

  /**
   * Writes and reads {@code sparse#SparseMapList}.
   */
  static final class SparseMapListSerde implements BiConsumer<List<Map<String, String>>, ShapeSerializer>, ShapeDeserializer.ListMemberConsumer<List<Map<String, String>>> {
    static final SparseMapListSerde INSTANCE = new SparseMapListSerde();

    static final Schema MEMBER = SparseSchemas.SPARSE_MAP_LIST.listMember();

    private SparseMapListSerde() {
    }

    @Override
    public void accept(List<Map<String, String>> values, ShapeSerializer serializer) {
      for (Map<String, String> value : values) {
        if (value == null || value instanceof SdkAutoConstructMap) {
          serializer.writeNull(MEMBER);
        } else {
          serializer.writeMap(MEMBER, value, value.size(), StringMapSerde.INSTANCE);
        }
      }
    }

    @Override
    public void accept(List<Map<String, String>> values, ShapeDeserializer deserializer) {
      if (deserializer.isNull()) {
        deserializer.readNull();
        values.add(DefaultSdkAutoConstructMap.getInstance());
      } else {
        values.add(StringMapSerde.read(MEMBER, deserializer));
      }
    }

    static List<Map<String, String>> read(Schema schema, ShapeDeserializer deserializer) {
      List<Map<String, String>> values = SchemaSerdeSupport.newList(deserializer);
      deserializer.readList(schema, values, INSTANCE);
      return Collections.unmodifiableList(values);
    }
  }

  /**
   * Writes and reads {@code sparse#SparseSetMap}.
   */
  static final class SparseSetMapSerde implements BiConsumer<Map<String, List<String>>, MapSerializer>, ShapeDeserializer.MapMemberConsumer<String, Map<String, List<String>>> {
    static final SparseSetMapSerde INSTANCE = new SparseSetMapSerde();

    static final Schema KEY = SparseSchemas.SPARSE_SET_MAP.mapKeyMember();

    static final Schema VALUE = SparseSchemas.SPARSE_SET_MAP.mapValueMember();

    private SparseSetMapSerde() {
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
        deserializer.readNull();
        values.put(key, DefaultSdkAutoConstructList.getInstance());
      } else {
        values.put(key, StringSetSerde.read(VALUE, deserializer));
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
        if (value == null || value instanceof SdkAutoConstructList) {
          serializer.writeNull(VALUE);
        } else {
          serializer.writeList(VALUE, value, value.size(), StringSetSerde.INSTANCE);
        }
      }
    }
  }

  /**
   * Writes and reads {@code sparse#SparseStringList}.
   */
  static final class SparseStringListSerde implements BiConsumer<List<String>, ShapeSerializer>, ShapeDeserializer.ListMemberConsumer<List<String>> {
    static final SparseStringListSerde INSTANCE = new SparseStringListSerde();

    static final Schema MEMBER = SparseSchemas.SPARSE_STRING_LIST.listMember();

    private SparseStringListSerde() {
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
   * Writes and reads {@code sparse#StringMap}.
   */
  static final class StringMapSerde implements BiConsumer<Map<String, String>, MapSerializer>, ShapeDeserializer.MapMemberConsumer<String, Map<String, String>> {
    static final StringMapSerde INSTANCE = new StringMapSerde();

    static final Schema KEY = SparseSchemas.STRING_MAP.mapKeyMember();

    static final Schema VALUE = SparseSchemas.STRING_MAP.mapValueMember();

    private StringMapSerde() {
    }

    @Override
    public void accept(Map<String, String> values, MapSerializer serializer) {
      for (Map.Entry<String, String> entry : values.entrySet()) {
        serializer.writeEntry(KEY, entry.getKey(), entry.getValue(), ValueWriter.INSTANCE);
      }
    }

    @Override
    public void accept(Map<String, String> values, String key, ShapeDeserializer deserializer) {
      if (deserializer.isNull()) {
        values.put(key, deserializer.readNull());
      } else {
        values.put(key, deserializer.readString(VALUE));
      }
    }

    static Map<String, String> read(Schema schema, ShapeDeserializer deserializer) {
      Map<String, String> values = SchemaSerdeSupport.newMap(deserializer);
      deserializer.readStringMap(schema, values, INSTANCE);
      return Collections.unmodifiableMap(values);
    }

    private static final class ValueWriter implements BiConsumer<String, ShapeSerializer> {
      static final ValueWriter INSTANCE = new ValueWriter();

      private ValueWriter() {
      }

      @Override
      public void accept(String value, ShapeSerializer serializer) {
        if (value == null) {
          serializer.writeNull(VALUE);
        } else {
          serializer.writeString(VALUE, value);
        }
      }
    }
  }

  /**
   * Writes and reads {@code sparse#StringSet}.
   */
  static final class StringSetSerde implements BiConsumer<List<String>, ShapeSerializer>, ShapeDeserializer.ListMemberConsumer<List<String>> {
    static final StringSetSerde INSTANCE = new StringSetSerde();

    static final Schema MEMBER = SparseSchemas.STRING_SET.listMember();

    private StringSetSerde() {
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
}
