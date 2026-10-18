package software.amazon.awssdk.services.docs.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.document.Document;
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
final class DocsSharedSerde {
  static final SdkFieldsFromSchema SDK_FIELDS = SdkFieldsFromSchema.builder().wireNames(SdkFieldsFromSchema.WireNaming.JSON).build();

  private DocsSharedSerde() {
  }

  /**
   * Writes and reads {@code docs#DocumentList}.
   */
  static final class DocumentListSerde implements BiConsumer<List<Document>, ShapeSerializer>, ShapeDeserializer.ListMemberConsumer<List<Document>> {
    static final DocumentListSerde INSTANCE = new DocumentListSerde();

    static final Schema MEMBER = DocsSchemas.DOCUMENT_LIST.listMember();

    private DocumentListSerde() {
    }

    @Override
    public void accept(List<Document> values, ShapeSerializer serializer) {
      for (Document value : values) {
        if (value == null) {
          serializer.writeNull(MEMBER);
        } else {
          serializer.writeDocument(MEMBER, SchemaSerdeSupport.wrapLegacy(value));
        }
      }
    }

    @Override
    public void accept(List<Document> values, ShapeDeserializer deserializer) {
      values.add(SchemaSerdeSupport.unwrapLegacy(deserializer.readDocument()));
    }

    static List<Document> read(Schema schema, ShapeDeserializer deserializer) {
      List<Document> values = SchemaSerdeSupport.newList(deserializer);
      deserializer.readList(schema, values, INSTANCE);
      return Collections.unmodifiableList(values);
    }
  }

  /**
   * Writes and reads {@code docs#DocumentMap}.
   */
  static final class DocumentMapSerde implements BiConsumer<Map<String, Document>, MapSerializer>, ShapeDeserializer.MapMemberConsumer<String, Map<String, Document>> {
    static final DocumentMapSerde INSTANCE = new DocumentMapSerde();

    static final Schema KEY = DocsSchemas.DOCUMENT_MAP.mapKeyMember();

    static final Schema VALUE = DocsSchemas.DOCUMENT_MAP.mapValueMember();

    private DocumentMapSerde() {
    }

    @Override
    public void accept(Map<String, Document> values, MapSerializer serializer) {
      for (Map.Entry<String, Document> entry : values.entrySet()) {
        serializer.writeEntry(KEY, entry.getKey(), entry.getValue(), ValueWriter.INSTANCE);
      }
    }

    @Override
    public void accept(Map<String, Document> values, String key, ShapeDeserializer deserializer) {
      values.put(key, SchemaSerdeSupport.unwrapLegacy(deserializer.readDocument()));
    }

    static Map<String, Document> read(Schema schema, ShapeDeserializer deserializer) {
      Map<String, Document> values = SchemaSerdeSupport.newMap(deserializer);
      deserializer.readStringMap(schema, values, INSTANCE);
      return Collections.unmodifiableMap(values);
    }

    private static final class ValueWriter implements BiConsumer<Document, ShapeSerializer> {
      static final ValueWriter INSTANCE = new ValueWriter();

      private ValueWriter() {
      }

      @Override
      public void accept(Document value, ShapeSerializer serializer) {
        if (value == null) {
          serializer.writeNull(VALUE);
        } else {
          serializer.writeDocument(VALUE, SchemaSerdeSupport.wrapLegacy(value));
        }
      }
    }
  }
}
