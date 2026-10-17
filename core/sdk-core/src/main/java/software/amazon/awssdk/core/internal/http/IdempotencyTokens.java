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

package software.amazon.awssdk.core.internal.http;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.InterceptingSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

/**
 * Fills an unset {@code @idempotencyToken} member of an operation input on the schema-based request path, for every
 * protocol. The legacy path fills it in each generated marshaller, through the member's
 * {@code DefaultValueTrait.idempotencyToken()}; this is the protocol-independent counterpart (design C.4: "idempotency
 * tokens, through {@code IdempotencyTokenTrait} and an {@code InterceptingSerializer} that writes a token for an unset
 * member").
 *
 * <p>The input is not modified: when the member is null, the protocol receives a view of the input whose
 * {@link SerializableStruct#getMemberValue} returns the token for that member and whose
 * {@link SerializableStruct#serializeMembers} writes the token in member index order. Each call takes a new token from
 * {@link IdempotentUtils#getGenerator()}, so the test hook {@code IdempotentUtils.setGenerator} applies.
 */
@SdkInternalApi
final class IdempotencyTokens {
    private IdempotencyTokens() {
    }

    /**
     * Returns the input to serialize: the input itself when the operation has no idempotency token member or the member
     * is set, else a view of the input with a generated token in that member.
     *
     * @param operation the operation being called
     * @param input the operation input, which is not modified
     * @return the input, or a view of it with the token filled
     */
    static SerializableStruct fill(ApiOperation<?, ?> operation, SerializableStruct input) {
        Schema member = operation.idempotencyTokenMember();
        if (member == null || input.getMemberValue(member) != null) {
            return input;
        }
        return new TokenFilledInput(input, member, IdempotentUtils.getGenerator().get());
    }

    /**
     * The input with the token in the idempotency token member.
     */
    private static final class TokenFilledInput implements SerializableStruct {
        private final SerializableStruct delegate;
        private final Schema tokenMember;
        private final String token;

        private TokenFilledInput(SerializableStruct delegate, Schema tokenMember, String token) {
            this.delegate = delegate;
            this.tokenMember = tokenMember;
            this.token = token;
        }

        @Override
        public Schema sdkSchema() {
            return delegate.sdkSchema();
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            TokenWriter writer = new TokenWriter(serializer);
            delegate.serializeMembers(writer);
            writer.writeTokenIfPending();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            if (member.memberIndex() == tokenMember.memberIndex() && member.memberName().equals(tokenMember.memberName())) {
                return (T) token;
            }
            return delegate.getMemberValue(member);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        /**
         * Passes every member write through, writing the token just before the first member that follows it, so the
         * members stay in index order.
         */
        private final class TokenWriter extends InterceptingSerializer {
            private final ShapeSerializer serializer;
            private boolean written;

            private TokenWriter(ShapeSerializer serializer) {
                this.serializer = serializer;
            }

            @Override
            protected ShapeSerializer before(Schema schema) {
                if (!written && schema.memberIndex() > tokenMember.memberIndex()) {
                    writeTokenIfPending();
                }
                return serializer;
            }

            private void writeTokenIfPending() {
                if (!written) {
                    written = true;
                    serializer.writeString(tokenMember, token);
                }
            }
        }
    }
}
