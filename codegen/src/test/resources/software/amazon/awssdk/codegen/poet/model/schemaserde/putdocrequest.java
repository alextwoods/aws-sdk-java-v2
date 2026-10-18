package software.amazon.awssdk.services.docs.model;

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
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class PutDocRequest extends DocsRequest implements ToCopyableBuilder<PutDocRequest.Builder, PutDocRequest>, SerializableStruct {
  /**
   * The schema of this shape.
   */
  public static final Schema SCHEMA = DocsSchemas.PUT_DOC_REQUEST;

  private static final Schema M_DOC = SCHEMA.member("Doc");

  private static final Schema M_DOCS = SCHEMA.member("Docs");

  private static final Schema M_DOC_MAP = SCHEMA.member("DocMap");

  private final Document doc;

  private final List<Document> docs;

  private final Map<String, Document> docMap;

  private PutDocRequest(BuilderImpl builder) {
    super(builder);
    this.doc = builder.doc;
    this.docs = builder.docs;
    this.docMap = builder.docMap;
  }

  /**
   * Returns the value of the Doc property for this object.
   * @return The value of the Doc property for this object.
   */
  public final Document doc() {
    return doc;
  }

  /**
   * For responses, this returns true if the service returned a value for the Docs property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful because the SDK will never return a null collection or map, but you may need to differentiate between the service returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true if a value for the property was specified in the request builder, and false if a value was not specified.
   */
  public final boolean hasDocs() {
    return docs != null && !(docs instanceof SdkAutoConstructList);
  }

  /**
   * Returns the value of the Docs property for this object.
   * <p>
   * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
   * </p>
   * <p>
   * This method will never return null. If you would like to know whether the service returned this field (so that you can differentiate between null and empty), you can use the {@link #hasDocs} method.
   * </p>
   * @return The value of the Docs property for this object.
   */
  public final List<Document> docs() {
    return docs;
  }

  /**
   * For responses, this returns true if the service returned a value for the DocMap property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful because the SDK will never return a null collection or map, but you may need to differentiate between the service returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true if a value for the property was specified in the request builder, and false if a value was not specified.
   */
  public final boolean hasDocMap() {
    return docMap != null && !(docMap instanceof SdkAutoConstructMap);
  }

  /**
   * Returns the value of the DocMap property for this object.
   * <p>
   * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
   * </p>
   * <p>
   * This method will never return null. If you would like to know whether the service returned this field (so that you can differentiate between null and empty), you can use the {@link #hasDocMap} method.
   * </p>
   * @return The value of the DocMap property for this object.
   */
  public final Map<String, Document> docMap() {
    return docMap;
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
    hashCode = 31 * hashCode + Objects.hashCode(doc());
    hashCode = 31 * hashCode + Objects.hashCode(hasDocs() ? docs() : null);
    hashCode = 31 * hashCode + Objects.hashCode(hasDocMap() ? docMap() : null);
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
    if (!(obj instanceof PutDocRequest)) {
      return false;
    }
    PutDocRequest other = (PutDocRequest) obj;
    return Objects.equals(doc(), other.doc())&&hasDocs() == other.hasDocs() && Objects.equals(docs(), other.docs())&&hasDocMap() == other.hasDocMap() && Objects.equals(docMap(), other.docMap());
  }

  /**
   * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be redacted from this string using a placeholder value. 
   */
  @Override
  public final String toString() {
    return ToString.builder("PutDocRequest").add("Doc", doc()).add("Docs", hasDocs() ? docs() : null).add("DocMap", hasDocMap() ? docMap() : null).build();
  }

  public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
    switch (fieldName) {
      case "Doc":return Optional.ofNullable(clazz.cast(doc()));
      case "Docs":return Optional.ofNullable(clazz.cast(docs()));
      case "DocMap":return Optional.ofNullable(clazz.cast(docMap()));
      default:return Optional.empty();
    }
  }

  @Override
  public final List<SdkField<?>> sdkFields() {
    return DocsSharedSerde.SDK_FIELDS.fields(SCHEMA);
  }

  @Override
  public final Map<String, SdkField<?>> sdkFieldNameToField() {
    return DocsSharedSerde.SDK_FIELDS.fieldsByName(SCHEMA);
  }

  @Override
  public Schema sdkSchema() {
    return SCHEMA;
  }

  @Override
  public void serializeMembers(ShapeSerializer serializer) {
    if (this.doc != null) {
      serializer.writeDocument(M_DOC, SchemaSerdeSupport.wrapLegacy(this.doc));
    }
    if (this.docs != null && !(this.docs instanceof SdkAutoConstructList)) {
      serializer.writeList(M_DOCS, this.docs, this.docs.size(), DocsSharedSerde.DocumentListSerde.INSTANCE);
    }
    if (this.docMap != null && !(this.docMap instanceof SdkAutoConstructMap)) {
      serializer.writeMap(M_DOC_MAP, this.docMap, this.docMap.size(), DocsSharedSerde.DocumentMapSerde.INSTANCE);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getMemberValue(Schema member) {
    switch (member.memberIndex()) {
      case 0:
        return (T) SchemaUtils.validateSameMember(M_DOC, member, this.doc);
      case 1:
        return (T) SchemaUtils.validateSameMember(M_DOCS, member, this.docs);
      case 2:
        return (T) SchemaUtils.validateSameMember(M_DOC_MAP, member, this.docMap);
      default:
        throw SchemaUtils.notAMember(SCHEMA, member);
    }
  }

  static ShapeBuilder<PutDocRequest> newShapeBuilder() {
    return new BuilderImpl();
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends DocsRequest.Builder, SdkPojo, CopyableBuilder<Builder, PutDocRequest> {
    /**
     * Sets the value of the Doc property for this object.
     *
     * @param doc The new value for the Doc property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder doc(Document doc);

    /**
     * Sets the value of the Docs property for this object.
     *
     * @param docs The new value for the Docs property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder docs(Collection<Document> docs);

    /**
     * Sets the value of the Docs property for this object.
     *
     * @param docs The new value for the Docs property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder docs(Document... docs);

    /**
     * Sets the value of the DocMap property for this object.
     *
     * @param docMap The new value for the DocMap property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder docMap(Map<String, Document> docMap);

    @Override
    Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

    @Override
    Builder overrideConfiguration(
        Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
  }

  static final class BuilderImpl extends DocsRequest.BuilderImpl implements Builder, ShapeBuilder<PutDocRequest> {
    Document doc;

    List<Document> docs = DefaultSdkAutoConstructList.getInstance();

    Map<String, Document> docMap = DefaultSdkAutoConstructMap.getInstance();

    private BuilderImpl() {
    }

    private BuilderImpl(PutDocRequest model) {
      super(model);doc(model.doc);
      docs(model.docs);
      docMap(model.docMap);
    }

    public final Document getDoc() {
      return doc;
    }

    public final void setDoc(Document doc) {
      this.doc = doc;
    }

    @Override
    public final Builder doc(Document doc) {
      this.doc = doc;
      return this;
    }

    public final Collection<Document> getDocs() {
      if (docs instanceof SdkAutoConstructList) {return null;}return docs;
    }

    public final void setDocs(Collection<Document> docs) {
      this.docs = DocumentListCopier.copy(docs);
    }

    @Override
    public final Builder docs(Collection<Document> docs) {
      this.docs = DocumentListCopier.copy(docs);
      return this;
    }

    @Override
    @SafeVarargs
    public final Builder docs(Document... docs) {
      docs(Arrays.asList(docs));return this;
    }

    public final Map<String, Document> getDocMap() {
      if (docMap instanceof SdkAutoConstructMap) {return null;}return docMap;
    }

    public final void setDocMap(Map<String, Document> docMap) {
      this.docMap = DocumentMapCopier.copy(docMap);
    }

    @Override
    public final Builder docMap(Map<String, Document> docMap) {
      this.docMap = DocumentMapCopier.copy(docMap);
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
    public PutDocRequest build() {
      return new PutDocRequest(this);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
      return DocsSharedSerde.SDK_FIELDS.fields(SCHEMA);
    }

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
      return DocsSharedSerde.SDK_FIELDS.fieldsByName(SCHEMA);
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
          doc((Document) SchemaUtils.validateSameMember(M_DOC, member, value));
          break;
        case 1:
          docs((Collection<Document>) SchemaUtils.validateSameMember(M_DOCS, member, value));
          break;
        case 2:
          docMap((Map<String, Document>) SchemaUtils.validateSameMember(M_DOC_MAP, member, value));
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
            builder.doc = SchemaSerdeSupport.unwrapLegacy(deserializer.readDocument());
            break;
          case 1:
            builder.docs = DocsSharedSerde.DocumentListSerde.read(member, deserializer);
            break;
          case 2:
            builder.docMap = DocsSharedSerde.DocumentMapSerde.read(member, deserializer);
            break;
          default:
            break;
        }
      }
    }
  }
}
