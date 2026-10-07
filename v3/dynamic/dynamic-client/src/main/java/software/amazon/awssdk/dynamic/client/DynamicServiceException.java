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

package software.amazon.awssdk.dynamic.client;

import java.util.Objects;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.utils.Redaction;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * A modeled error of a dynamic call: the dynamic counterpart of a generated service's modeled exception type
 * (Requirement 14.10).
 *
 * <p>The protocol resolves a wire error to its modeled error shape exactly as for a generated client, through the
 * operation's errors, the service's type registry and {@code awsQueryError} codes; the dynamic client registers this
 * type for every error structure of the runtime model. The exception carries the error shape's ID and schema, its
 * modeled members as a {@link Document} ({@link #error()}), and the same AWS error details, request IDs, status code
 * and attempt count as any {@link AwsServiceException}. As for generated exceptions, a {@code message} member (in any
 * case) that targets a string is the exception's message.
 *
 * <p>An error response that names no modeled error surfaces as a plain {@link AwsServiceException}, as it does for
 * generated clients.
 */
public final class DynamicServiceException extends AwsServiceException implements SerializableStruct {
    private static final long serialVersionUID = 1L;

    private final transient Schema schema;
    private final transient ShapeId service;
    private final transient StructDocument error;

    private DynamicServiceException(BuilderImpl builder, StructDocument error) {
        super(builder);
        this.schema = builder.schema;
        this.service = builder.service;
        this.error = error;
    }

    /** A builder of the modeled error {@code schema} of the service {@code service}. */
    static Builder builder(Schema schema, ShapeId service) {
        return new BuilderImpl(schema, service);
    }

    /** Absolute shape ID of the modeled error structure. */
    public ShapeId errorShapeId() {
        return schema.id();
    }

    /** The modeled members of the error, as a document of the error structure's schema. */
    public Document error() {
        return error;
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        error.serializeMembers(serializer);
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        return error.getMemberValue(member);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /** Builder of a {@link DynamicServiceException}: the AWS exception builder and the error's shape builder. */
    public interface Builder extends AwsServiceException.Builder, ShapeBuilder<DynamicServiceException> {
        @Override
        Builder awsErrorDetails(AwsErrorDetails awsErrorDetails);

        @Override
        Builder message(String message);

        @Override
        Builder requestId(String requestId);

        @Override
        Builder extendedRequestId(String extendedRequestId);

        @Override
        Builder statusCode(int statusCode);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        Builder deserialize(ShapeDeserializer deserializer);

        @Override
        Builder deserializeMember(ShapeDeserializer deserializer, Schema member);

        @Override
        Builder errorCorrection();

        @Override
        DynamicServiceException build();
    }

    private static final class BuilderImpl extends AwsServiceException.BuilderImpl implements Builder {
        private final Schema schema;
        private final ShapeId service;
        /** The members of a copied exception, until a member is changed. */
        private StructDocument copied;
        private ShapeBuilder<StructDocument> members;

        private BuilderImpl(Schema schema, ShapeId service) {
            this.schema = Objects.requireNonNull(schema, "schema");
            this.service = Objects.requireNonNull(service, "service");
            if (schema.type() != ShapeType.STRUCTURE) {
                throw new IllegalArgumentException("A modeled error is a structure, not " + schema.type() + " "
                                                   + schema.id());
            }
        }

        private BuilderImpl(DynamicServiceException exception) {
            super(exception);
            this.schema = exception.schema;
            this.service = exception.service;
            this.copied = exception.error;
        }

        /** The member builder, seeded with a copied exception's members on first use. */
        private ShapeBuilder<StructDocument> members() {
            if (members == null) {
                members = SchemaConverter.createDocumentBuilder(schema, service);
                if (copied != null) {
                    for (Schema member : schema.members()) {
                        Object value = copied.getMemberValue(member);
                        if (value != null) {
                            members.setMemberValue(member, value);
                        }
                    }
                    copied = null;
                }
            }
            return members;
        }

        private StructDocument document() {
            return copied != null ? copied : members().build();
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public Builder deserialize(ShapeDeserializer deserializer) {
            members().deserialize(deserializer);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer deserializer, Schema member) {
            members().deserializeMember(deserializer, member);
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
        public BuilderImpl extendedRequestId(String extendedRequestId) {
            this.extendedRequestId = extendedRequestId;
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
        public DynamicServiceException build() {
            StructDocument error = document();
            if (message == null) {
                message = modeledMessage(error);
            }
            return new DynamicServiceException(this, error);
        }

        /**
         * The string {@code message} member (in any case) of the error, as generated exceptions hold it; redacted if
         * it is {@code @sensitive} (Requirement 21.10), as the member document still holds the value.
         */
        private String modeledMessage(StructDocument document) {
            for (Schema member : schema.members()) {
                if (member.memberName().equalsIgnoreCase("message") && member.type() == ShapeType.STRING) {
                    Document value = document.getMember(member.memberName());
                    if (value != null) {
                        boolean sensitive = member.hasTrait(TraitKey.SENSITIVE_TRAIT)
                                            || member.memberTarget().hasTrait(TraitKey.SENSITIVE_TRAIT);
                        return sensitive ? Redaction.REDACTED : value.asString();
                    }
                }
            }
            return null;
        }
    }
}
