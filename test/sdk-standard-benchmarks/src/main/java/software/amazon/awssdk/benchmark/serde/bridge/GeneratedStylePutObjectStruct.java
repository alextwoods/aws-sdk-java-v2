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

package software.amazon.awssdk.benchmark.serde.bridge;

import java.nio.ByteBuffer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Hand-written stand-in for what v2 codegen <b>would emit</b> if it generated smithy-java
 * {@link SerializableStruct}s directly (instead of being wrapped by {@code BridgeStruct} at
 * runtime). The point is to measure the ceiling: a <b>monomorphic, straight-line</b>
 * {@code serializeMembers} with cached static {@link Schema} member constants and direct
 * typed {@code write*} calls — exactly the shape of a generated smithy-java struct, which the
 * JIT can inline flat.
 *
 * <p>It reads its values from the same v2 {@link SdkPojo} the other benchmarks use (via v2's
 * typed accessors by reflection-free getter), so the comparison isolates the serialize path:
 * monomorphic generated-style writes vs. {@code BridgeStruct}'s field-iteration loop.
 *
 * <p>Scoped to the members the {@code restJson1_PutObject_*} cases exercise
 * (Bucket/Key/Body/ContentLength) — enough to be a faithful hot-path comparison.
 */
final class GeneratedStylePutObjectStruct implements SerializableStruct {

    // Cached static member schemas — resolved once, exactly like generated $SCHEMA_* constants.
    private final Schema schema;
    private final Schema bucketMember;
    private final Schema keyMember;
    private final Schema bodyMember;
    private final Schema contentLengthMember;

    // Values, extracted once from the v2 pojo (a generated struct would hold these as fields).
    private final String bucket;
    private final String key;
    private final ByteBuffer body;
    private final Long contentLength;

    GeneratedStylePutObjectStruct(Schema schema, SdkPojo pojo) {
        this.schema = schema;
        this.bucketMember = schema.member("Bucket");
        this.keyMember = schema.member("Key");
        this.bodyMember = schema.member("Body");
        this.contentLengthMember = schema.member("ContentLength");
        // Pull the values the test case sets from the v2 pojo via its SdkField getters.
        this.bucket = (String) read(pojo, "Bucket");
        this.key = (String) read(pojo, "Key");
        Object b = read(pojo, "Body");
        this.body = b == null ? null : ((SdkBytes) b).asByteBuffer();
        this.contentLength = (Long) read(pojo, "ContentLength");
    }

    private static Object read(SdkPojo pojo, String member) {
        return pojo.sdkFields().stream()
                .filter(f -> f.memberName().equals(member))
                .findFirst()
                .map(f -> f.getValueOrDefault(pojo))
                .orElse(null);
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        // Monomorphic, straight-line — the body a generated smithy-java struct emits.
        if (bucket != null) {
            serializer.writeString(bucketMember, bucket);
        }
        if (key != null) {
            serializer.writeString(keyMember, key);
        }
        if (body != null) {
            serializer.writeDataStream(bodyMember, DataStream.ofByteBuffer(body));
        }
        if (contentLength != null) {
            serializer.writeLong(contentLengthMember, contentLength);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberName()) {
            case "Bucket" -> (T) bucket;
            case "Key" -> (T) key;
            case "ContentLength" -> (T) contentLength;
            default -> null;
        };
    }
}
