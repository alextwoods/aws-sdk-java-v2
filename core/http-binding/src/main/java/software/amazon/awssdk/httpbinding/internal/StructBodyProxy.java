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

package software.amazon.awssdk.httpbinding.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.InterceptingSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * A view of a structure that serializes only its body members: the members whose direction-specific binding is
 * {@code BODY}. Every other member is sent to the null serializer, so the codec writes the structure with only the
 * unbound members.
 *
 * <p>This is how the body is written without a {@code MemberSubsetCodec}: any codec that writes a structure by calling
 * {@link SerializableStruct#serializeMembers} works. Port of smithy-java {@code StructBodyProxy}.
 */
@SdkInternalApi
final class StructBodyProxy implements SerializableStruct {
    private final SerializableStruct delegate;
    private final HttpBindingSchemaExtensions.Binding[] bindings;

    /**
     * @param delegate the input, output or error structure
     * @param bindings direction-specific binding kinds indexed by member index
     */
    StructBodyProxy(SerializableStruct delegate, HttpBindingSchemaExtensions.Binding[] bindings) {
        this.delegate = delegate;
        this.bindings = bindings;
    }

    @Override
    public Schema sdkSchema() {
        return delegate.sdkSchema();
    }

    @Override
    public void serializeMembers(ShapeSerializer codecMemberSerializer) {
        delegate.serializeMembers(new BodyMemberSerializer(codecMemberSerializer, bindings));
    }

    /**
     * Returns the delegate's value, or null for a member that is not in the body, so a codec that reads member values
     * directly (for example XML attributes) sees the same members as one that writes through
     * {@link #serializeMembers}.
     */
    @Override
    public <T> T getMemberValue(Schema member) {
        return bindings[member.memberIndex()] == HttpBindingSchemaExtensions.Binding.BODY
               ? delegate.getMemberValue(member)
               : null;
    }

    private static final class BodyMemberSerializer extends InterceptingSerializer {
        private final ShapeSerializer inner;
        private final HttpBindingSchemaExtensions.Binding[] bindings;

        BodyMemberSerializer(ShapeSerializer inner, HttpBindingSchemaExtensions.Binding[] bindings) {
            this.inner = inner;
            this.bindings = bindings;
        }

        @Override
        protected ShapeSerializer before(Schema schema) {
            return bindings[schema.memberIndex()] == HttpBindingSchemaExtensions.Binding.BODY
                   ? inner
                   : ShapeSerializer.nullSerializer();
        }
    }
}
