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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The input of a {@link DynamicClient} call in the SDK request pipeline: the input document of the operation and the
 * call's override configuration.
 *
 * <p>Interceptors see this request as {@code context.request()}. Its {@link #document()} is the input after coercion
 * to the operation's input schema, keyed by Smithy member name. As a {@link SerializableStruct}, the request has the
 * input schema and the members of its document, so a client protocol serializes it like a generated request.
 *
 * <p>The request has no {@link #sdkFields()}: the dynamic client's protocols read members through the schema.
 *
 * <p>SDK addition with no smithy-java counterpart: smithy-java's dynamic client passes documents through its pipeline,
 * while the SDK pipeline needs an {@code SdkRequest}.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class DynamicRequest extends AwsRequest implements SerializableStruct {
    private final StructDocument document;

    private DynamicRequest(BuilderImpl builder) {
        super(builder);
        this.document = Validate.paramNotNull(builder.document, "document");
    }

    /**
     * Creates a builder of requests.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Returns the input document.
     *
     * @return the input, a structure document whose schema is the operation's input schema
     */
    public StructDocument document() {
        return document;
    }

    /**
     * Returns the schema of the input document: the operation's input schema.
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
        DynamicRequest that = (DynamicRequest) o;
        return document.equals(that.document);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + document.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("DynamicRequest")
                       .add("Input", sdkSchema().id())
                       .add("Document", document)
                       .build();
    }

    /**
     * The builder of {@link DynamicRequest}s.
     */
    @NotThreadSafe
    public interface Builder extends AwsRequest.Builder {
        /**
         * Sets the input document. Required.
         *
         * @param document the input, a structure document with the operation's input schema
         * @return this builder
         */
        Builder document(StructDocument document);

        /**
         * Returns the input document set on this builder.
         *
         * @return the document, or null when none is set
         */
        StructDocument document();

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);

        /**
         * Builds a request.
         *
         * @return the request
         * @throws NullPointerException if no document is set
         */
        @Override
        DynamicRequest build();
    }

    private static final class BuilderImpl extends AwsRequest.BuilderImpl implements Builder {
        private StructDocument document;

        private BuilderImpl() {
        }

        private BuilderImpl(DynamicRequest request) {
            super(request);
            this.document = request.document;
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
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public DynamicRequest build() {
            return new DynamicRequest(this);
        }
    }
}
