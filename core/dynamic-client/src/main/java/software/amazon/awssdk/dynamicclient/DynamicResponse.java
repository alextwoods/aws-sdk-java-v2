/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.dynamicclient;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The output of a {@link DynamicClient} call in the SDK request pipeline: the output document of the operation, with
 * the HTTP response and the response metadata (such as the request ID) a generated response carries.
 *
 * <p>Interceptors see this response as {@code context.response()}. Its {@link #document()} is a structure document
 * whose schema is the operation's output schema, keyed by Smithy member name, and whose discriminator is the output
 * ShapeId. As a {@link SerializableStruct}, the response has the output schema and the members of its document.
 *
 * <p>The response has no {@link #sdkFields()}: the members are read through the schema.
 *
 * <p>SDK addition with no smithy-java counterpart: smithy-java's dynamic client passes documents through its pipeline,
 * while the SDK pipeline needs an {@code SdkResponse}.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class DynamicResponse extends AwsResponse implements SerializableStruct {
    private final StructDocument document;

    private DynamicResponse(BuilderImpl builder, StructDocument document) {
        super(builder);
        this.document = Validate.paramNotNull(document, "document");
    }

    /**
     * Creates a builder of responses.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new BuilderImpl((ShapeBuilder<StructDocument>) null);
    }

    /**
     * Creates the builder a client protocol deserializes an operation output into: deserialization and member values
     * go to the document builder, and {@code build()} wraps the document it builds.
     *
     * @param documentBuilder the shape builder of the output document
     * @return a new builder that is also an {@link AwsResponse.Builder}, so the protocol attaches the HTTP response
     *     and the response metadata
     */
    static ShapeBuilder<DynamicResponse> deserializingBuilder(ShapeBuilder<StructDocument> documentBuilder) {
        return new BuilderImpl(Validate.paramNotNull(documentBuilder, "documentBuilder"));
    }

    /**
     * Returns the output document.
     *
     * @return the output, a structure document whose schema is the operation's output schema
     */
    public StructDocument document() {
        return document;
    }

    /**
     * Returns the schema of the output document: the operation's output schema.
     */
    @Override
    public Schema sdkSchema() {
        return document.sdkSchema();
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        document.serializeMembers(serializer);
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        return document.getMemberValue(member);
    }

    /**
     * Returns no fields: the members are read through {@link #sdkSchema()}.
     */
    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!super.equals(o)) {
            return false;
        }
        DynamicResponse that = (DynamicResponse) o;
        return document.equals(that.document);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + document.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("DynamicResponse")
                       .add("Output", sdkSchema().id())
                       .add("Document", document)
                       .build();
    }

    /**
     * The builder of {@link DynamicResponse}s.
     */
    @NotThreadSafe
    public interface Builder extends AwsResponse.Builder {
        /**
         * Sets the output document. Required.
         *
         * @param document the output, a structure document with the operation's output schema
         * @return this builder
         */
        Builder document(StructDocument document);

        /**
         * Returns the output document set on this builder.
         *
         * @return the document, or null when none is set
         */
        StructDocument document();

        @Override
        Builder responseMetadata(AwsResponseMetadata metadata);

        @Override
        Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse);

        /**
         * Builds a response.
         *
         * @return the response
         * @throws NullPointerException if no document is set
         */
        @Override
        DynamicResponse build();
    }

    /**
     * The builder of {@link #builder()} and {@link #toBuilder()}, and the deserializing builder of
     * {@link #deserializingBuilder(ShapeBuilder)}. The second has a document builder: until {@link #document} is called,
     * {@code build()} uses the document that builder builds.
     */
    private static final class BuilderImpl extends AwsResponse.BuilderImpl
        implements Builder, ShapeBuilder<DynamicResponse> {
        private final ShapeBuilder<StructDocument> documentBuilder;
        private StructDocument document;

        private BuilderImpl(ShapeBuilder<StructDocument> documentBuilder) {
            this.documentBuilder = documentBuilder;
        }

        private BuilderImpl(DynamicResponse response) {
            super(response);
            this.documentBuilder = null;
            this.document = response.document;
        }

        @Override
        public Builder document(StructDocument document) {
            this.document = document;
            return this;
        }

        @Override
        public StructDocument document() {
            return document;
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata metadata) {
            super.responseMetadata(metadata);
            return this;
        }

        @Override
        public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
            super.sdkHttpResponse(sdkHttpResponse);
            return this;
        }

        @Override
        public Schema sdkSchema() {
            if (document != null) {
                return document.sdkSchema();
            }
            return expectDocumentBuilder().sdkSchema();
        }

        @Override
        public ShapeBuilder<DynamicResponse> deserialize(ShapeDeserializer decoder) {
            expectDocumentBuilder().deserialize(decoder);
            return this;
        }

        @Override
        public ShapeBuilder<DynamicResponse> deserializeMember(ShapeDeserializer decoder, Schema schema) {
            expectDocumentBuilder().deserializeMember(decoder, schema);
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            expectDocumentBuilder().setMemberValue(member, value);
        }

        @Override
        public ShapeBuilder<DynamicResponse> errorCorrection() {
            expectDocumentBuilder().errorCorrection();
            return this;
        }

        @Override
        public DynamicResponse build() {
            StructDocument built = document == null && documentBuilder != null ? documentBuilder.build() : document;
            return new DynamicResponse(this, built);
        }

        private ShapeBuilder<StructDocument> expectDocumentBuilder() {
            if (documentBuilder == null) {
                throw new IllegalStateException("This DynamicResponse builder has no schema to deserialize with; set "
                                                + "the output with document(StructDocument)");
            }
            return documentBuilder;
        }
    }
}
