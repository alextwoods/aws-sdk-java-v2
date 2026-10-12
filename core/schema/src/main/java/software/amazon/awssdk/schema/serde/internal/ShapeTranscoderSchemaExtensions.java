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

package software.amazon.awssdk.schema.serde.internal;

import java.util.concurrent.atomic.AtomicReferenceArray;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design Porting_Map) name.
/**
 * Provides bounded, shared member mappings for {@code software.amazon.awssdk.schema.serde.ShapeTranscoder}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.ShapeTranscoderSchemaExtensions}. Registered
 * as a {@link SchemaExtensionProvider} through {@code META-INF/services}.
 */
@SdkInternalApi
public final class ShapeTranscoderSchemaExtensions
    implements SchemaExtensionProvider<ShapeTranscoderSchemaExtensions.TranscodingExtension> {
    // CHECKSTYLE:ON

    public static final SchemaExtensionKey<TranscodingExtension> KEY = new SchemaExtensionKey<>();

    private static final int CACHE_SIZE = 8;
    private static final int CACHE_MASK = CACHE_SIZE - 1;

    @Override
    public SchemaExtensionKey<TranscodingExtension> key() {
        return KEY;
    }

    @Override
    public TranscodingExtension provide(Schema schema) {
        switch (schema.type()) {
            case STRUCTURE:
            case UNION:
                return new TranscodingExtension(schema);
            default:
                return null;
        }
    }

    /**
     * Returns the member mapping from a source structure or union schema to a target schema, cached on the source
     * schema when it has the extension.
     */
    public static MemberMapping mapping(Schema source, Schema target) {
        TranscodingExtension extension = source.getExtension(KEY);
        return extension == null ? new MemberMapping(source, target) : extension.mappingTo(target);
    }

    /**
     * A small cache of member mappings from one source schema to recently used target schemas.
     *
     * <p>Safely published through final fields. Concurrent extension computation may create independent holders,
     * but entries only avoid redundant work and never affect mapping behavior.
     */
    public static final class TranscodingExtension {
        private final Schema source;
        private final AtomicReferenceArray<MemberMapping> mappings = new AtomicReferenceArray<>(CACHE_SIZE);

        private TranscodingExtension(Schema source) {
            this.source = source;
        }

        private MemberMapping mappingTo(Schema target) {
            int index = cacheIndex(target);
            MemberMapping current = mappings.get(index);
            if (current != null && current.matches(source, target)) {
                return current;
            }

            MemberMapping mapping = new MemberMapping(source, target);
            if (mappings.compareAndSet(index, current, mapping)) {
                return mapping;
            }

            current = mappings.get(index);
            return current != null && current.matches(source, target) ? current : mapping;
        }

        private static int cacheIndex(Schema target) {
            int hash = System.identityHashCode(target);
            return (hash ^ (hash >>> 16)) & CACHE_MASK;
        }
    }

    /**
     * Maps each member of a source structure to the target member with the same Smithy member name.
     */
    public static final class MemberMapping {
        private final Schema source;
        private final Schema target;
        private final Schema[] sourceMembers;
        private final Schema[] targetMembers;

        private MemberMapping(Schema source, Schema target) {
            this.source = source;
            this.target = target;
            sourceMembers = source.members().toArray(new Schema[0]);
            targetMembers = new Schema[sourceMembers.length];
            for (int i = 0; i < sourceMembers.length; i++) {
                targetMembers[i] = target.member(sourceMembers[i].memberName());
            }
        }

        /**
         * Returns whether this mapping is from exactly {@code source} to exactly {@code target}.
         */
        public boolean matches(Schema source, Schema target) {
            return this.source == source && this.target == target;
        }

        /**
         * Returns the target member with the same name as a source member, or null if the target has none.
         */
        public Schema targetMember(Schema sourceMember) {
            int index = sourceMember.memberIndex();
            if (index >= 0 && index < sourceMembers.length && sourceMembers[index] == sourceMember) {
                return targetMembers[index];
            }
            return target.member(sourceMember.memberName());
        }
    }
}
