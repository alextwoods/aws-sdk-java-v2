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

import java.time.Duration;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.RetryableTrait;
import software.amazon.awssdk.utils.Validate;

/**
 * A modeled error returned to a {@link DynamicClient} call, with its members as a document.
 *
 * <p>When an error response resolves to an error shape that the called operation or its service lists, the call
 * throws (or its future completes with) a {@code DocumentException}. {@link #getContents()} returns the error's
 * members as a structure document whose discriminator is the error's ShapeId, keyed by Smithy member name. Every other
 * value comes from the response, as for a generated client's exception: {@link #statusCode()}, {@link #requestId()},
 * {@link #extendedRequestId()} and {@link #awsErrorDetails()} (error code, message, service name and HTTP response).
 * An error response that does not resolve to such an error shape gives a plain {@link AwsServiceException} instead.
 *
 * <p>An error shape with the {@code smithy.api#retryable} trait is {@linkplain #isRetryableException() retryable}, and
 * with {@code @retryable(throttling: true)} it is also a {@linkplain #isThrottlingException() throttling} error, as for
 * a generated exception.
 *
 * <p>The schema and the contents are not kept by Java serialization: after deserialization, {@link #getContents()} and
 * {@link #sdkSchema()} return null.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicclient.DocumentException}, which extends
 * {@code ModeledException}. This port extends {@link AwsServiceException}, so that code that catches the generic service
 * exception also catches it.
 */
@SdkPublicApi
public final class DocumentException extends AwsServiceException implements SerializableStruct {
    private static final long serialVersionUID = 1L;

    private final transient Schema schema;
    private final ShapeId serviceId;
    private final transient StructDocument contents;

    private DocumentException(SchemaGuidedExceptionBuilder builder, StructDocument contents) {
        super(builder);
        this.schema = builder.schema;
        this.serviceId = builder.serviceId;
        this.contents = contents;
    }

    /**
     * Creates a builder of exceptions of an error shape, for example to create the exception a test stub returns.
     *
     * @param errorSchema the error's structure schema with the {@code smithy.api#error} trait, for example one of
     *     {@link DynamicOperation#errorSchemas()}
     * @return a new builder whose exception has no members until they are set
     * @throws NullPointerException if {@code errorSchema} is null
     * @throws IllegalArgumentException if the schema is not a structure with the {@code smithy.api#error} trait
     */
    public static Builder builder(Schema errorSchema) {
        Validate.paramNotNull(errorSchema, "errorSchema");
        if (errorSchema.type() != ShapeType.STRUCTURE || !errorSchema.hasTrait(TraitKey.ERROR_TRAIT)) {
            throw new IllegalArgumentException("A DocumentException needs the schema of a structure with the "
                                               + "smithy.api#error trait, but " + errorSchema.id() + " is a "
                                               + errorSchema.type()
                                               + (errorSchema.type() == ShapeType.STRUCTURE ? " without it" : ""));
        }
        return new SchemaGuidedExceptionBuilder(errorSchema, errorSchema.id());
    }

    /**
     * Creates the builder the operation's error registry returns for an error shape.
     *
     * @param errorSchema the error's schema, converted from the model
     * @param serviceId the ShapeId of the client's service
     */
    static Builder schemaGuidedBuilder(Schema errorSchema, ShapeId serviceId) {
        return new SchemaGuidedExceptionBuilder(errorSchema, serviceId);
    }

    /**
     * Returns the error's members, read from the response with the error shape's schema.
     *
     * @return a structure document whose discriminator is the error's ShapeId, keyed by Smithy member name; members
     *     absent from the response are absent from the document. Null only after Java deserialization
     */
    public Document getContents() {
        return contents;
    }

    /**
     * Returns the error shape's schema.
     *
     * @return the schema, or null after Java deserialization
     */
    @Override
    public Schema sdkSchema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        contents.serializeMembers(serializer);
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        return contents.getMemberValue(member);
    }

    /**
     * Returns true when the error shape has the {@code smithy.api#retryable} trait, otherwise what
     * {@link AwsServiceException} returns.
     */
    @Override
    public boolean isRetryableException() {
        return (schema != null && schema.hasTrait(TraitKey.RETRYABLE_TRAIT)) || super.isRetryableException();
    }

    /**
     * Returns true when the error shape has {@code @retryable(throttling: true)}, otherwise what
     * {@link AwsServiceException} returns (for example true for status code 429 or a throttling error code).
     */
    @Override
    public boolean isThrottlingException() {
        RetryableTrait retryable = schema == null ? null : schema.getTrait(TraitKey.RETRYABLE_TRAIT);
        return (retryable != null && retryable.getThrottling()) || super.isThrottlingException();
    }

    /**
     * Returns a builder with this exception's values and contents, whose {@code build()} returns a
     * {@code DocumentException} of the same error shape.
     */
    @Override
    public Builder toBuilder() {
        return new SchemaGuidedExceptionBuilder(this);
    }

    /**
     * Builds a {@link DocumentException}. As a {@link ShapeBuilder}, it reads the error's members with the error
     * shape's schema; as an {@link AwsServiceException.Builder}, it takes the values a client protocol reads from the
     * response.
     */
    @Mutable
    @NotThreadSafe
    public interface Builder extends AwsServiceException.Builder, ShapeBuilder<DocumentException> {
        @Override
        Builder awsErrorDetails(AwsErrorDetails awsErrorDetails);

        @Override
        Builder clockSkew(Duration timeOffSet);

        /**
         * Sets the message. When no message is set, {@link #message()} returns the error's {@code message} member (the
         * first string member whose name is {@code message} in any case), as for a generated exception.
         */
        @Override
        Builder message(String message);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder requestId(String requestId);

        @Override
        Builder extendedRequestId(String extendedRequestId);

        @Override
        Builder statusCode(int statusCode);

        @Override
        Builder deserialize(ShapeDeserializer decoder);

        @Override
        Builder deserializeMember(ShapeDeserializer decoder, Schema schema);

        @Override
        Builder errorCorrection();

        @Override
        DocumentException build();
    }

    /**
     * Reads the error's members into a {@link StructDocument} with the converter's schema-guided document builder.
     *
     * <p>Port of smithy-java {@code DocumentException.SchemaGuidedExceptionBuilder}. It also carries the
     * {@link AwsServiceException} values, and {@link #message()} falls back to the {@code message} member.
     */
    private static final class SchemaGuidedExceptionBuilder extends AwsServiceException.BuilderImpl implements Builder {
        private final Schema schema;
        private final ShapeId serviceId;
        private final ShapeBuilder<StructDocument> documentBuilder;

        /**
         * The contents copied by {@link DocumentException#toBuilder()}, kept as they are until a member is changed.
         */
        private StructDocument copied;

        private SchemaGuidedExceptionBuilder(Schema schema, ShapeId serviceId) {
            this.schema = schema;
            this.serviceId = serviceId;
            this.documentBuilder = SchemaConverter.createDocumentBuilder(schema, serviceId);
        }

        private SchemaGuidedExceptionBuilder(DocumentException exception) {
            super(exception);
            Validate.validState(exception.schema != null,
                                "The schema of a DocumentException is not kept by Java serialization, so a "
                                + "deserialized DocumentException cannot be copied");
            this.schema = exception.schema;
            this.serviceId = exception.serviceId;
            this.documentBuilder = SchemaConverter.createDocumentBuilder(schema, serviceId);
            this.copied = exception.contents;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            members().deserialize(decoder);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema member) {
            members().deserializeMember(decoder, member);
            return this;
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            members().setMemberValue(member, value);
        }

        @Override
        public Builder errorCorrection() {
            members().errorCorrection();
            return this;
        }

        /**
         * Returns the message set on this builder or, when none is set, the error's {@code message} member.
         */
        @Override
        public String message() {
            if (message != null) {
                return message;
            }
            StructDocument document = copied != null ? copied : documentBuilder.build();
            for (Schema member : schema.members()) {
                if ("message".equalsIgnoreCase(member.memberName())) {
                    Document value = document.getMember(member.memberName());
                    if (value != null && value.isType(ShapeType.STRING)) {
                        return value.asString();
                    }
                }
            }
            return null;
        }

        @Override
        public Builder awsErrorDetails(AwsErrorDetails awsErrorDetails) {
            super.awsErrorDetails(awsErrorDetails);
            return this;
        }

        @Override
        public Builder clockSkew(Duration clockSkew) {
            super.clockSkew(clockSkew);
            return this;
        }

        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public Builder numAttempts(Integer numAttempts) {
            super.numAttempts(numAttempts);
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        @Override
        public Builder writableStackTrace(Boolean writableStackTrace) {
            super.writableStackTrace(writableStackTrace);
            return this;
        }

        @Override
        public Builder requestId(String requestId) {
            super.requestId(requestId);
            return this;
        }

        @Override
        public Builder extendedRequestId(String extendedRequestId) {
            super.extendedRequestId(extendedRequestId);
            return this;
        }

        @Override
        public Builder statusCode(int statusCode) {
            super.statusCode(statusCode);
            return this;
        }

        @Override
        public DocumentException build() {
            return new DocumentException(this, copied != null ? copied : documentBuilder.build());
        }

        /**
         * The document builder, holding the copied contents first if a member is about to change.
         */
        private ShapeBuilder<StructDocument> members() {
            if (copied != null) {
                StructDocument contents = copied;
                copied = null;
                for (Schema member : schema.members()) {
                    Document value = contents.getMember(member.memberName());
                    if (value != null) {
                        documentBuilder.setMemberValue(member, value);
                    }
                }
            }
            return documentBuilder;
        }
    }
}
