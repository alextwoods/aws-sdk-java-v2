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

package software.amazon.awssdk.schema.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.TraitKey;

/**
 * Finds, once per operation input or output schema, the members that {@code ApiOperation}'s default methods return:
 * the first member with the {@code idempotencyToken} trait and the first member with the {@code streaming} trait.
 *
 * <p>SDK addition with no smithy-java counterpart: smithy-java's {@code ApiOperation} defaults loop over the members on
 * every call. The results are the same. Registered as a {@link SchemaExtensionProvider} through
 * {@code META-INF/services}.
 */
@SdkInternalApi
public final class OperationMembersExtension implements SchemaExtensionProvider<OperationMembersExtension.Members> {

    public static final SchemaExtensionKey<Members> KEY = new SchemaExtensionKey<>();

    @Override
    public SchemaExtensionKey<Members> key() {
        return KEY;
    }

    @Override
    public Members provide(Schema schema) {
        Schema idempotencyToken = null;
        Schema stream = null;
        for (Schema member : schema.members()) {
            if (idempotencyToken == null && member.hasTrait(TraitKey.IDEMPOTENCY_TOKEN_TRAIT)) {
                idempotencyToken = member;
            }
            if (stream == null && member.hasTrait(TraitKey.STREAMING_TRAIT)) {
                stream = member;
            }
        }
        return new Members(idempotencyToken, stream);
    }

    /**
     * Returns the cached members of an operation input or output schema.
     */
    public static Members of(Schema schema) {
        Members members = schema.getExtension(KEY);
        // Null only when the provider is not registered, for example in a class loader without the services file.
        return members != null ? members : new OperationMembersExtension().provide(schema);
    }

    /**
     * The special members of one input or output structure.
     */
    public static final class Members {
        private final Schema idempotencyTokenMember;
        private final Schema streamMember;

        private Members(Schema idempotencyTokenMember, Schema streamMember) {
            this.idempotencyTokenMember = idempotencyTokenMember;
            this.streamMember = streamMember;
        }

        /**
         * The first member with the {@code idempotencyToken} trait, or null.
         */
        public Schema idempotencyTokenMember() {
            return idempotencyTokenMember;
        }

        /**
         * The first member with the {@code streaming} trait (a streaming blob or an event stream), or null.
         */
        public Schema streamMember() {
            return streamMember;
        }
    }
}
