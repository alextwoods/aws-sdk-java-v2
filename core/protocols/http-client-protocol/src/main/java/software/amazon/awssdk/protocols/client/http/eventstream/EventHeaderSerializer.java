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

package software.amazon.awssdk.protocols.client.http.eventstream;

import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;

/**
 * Writes {@code eventHeader} members as typed event stream message headers named after the member.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.EventHeaderSerializer}.
 */
@SdkInternalApi
final class EventHeaderSerializer extends SpecificShapeSerializer {
    private final HeadersBuilder headers;

    EventHeaderSerializer(HeadersBuilder headers) {
        this.headers = headers;
    }

    @Override
    public void writeBoolean(Schema schema, boolean value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeShort(Schema schema, short value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeByte(Schema schema, byte value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeInteger(Schema schema, int value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeLong(Schema schema, long value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeString(Schema schema, String value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeBlob(Schema schema, ByteBuffer value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeTimestamp(Schema schema, Instant value) {
        headers.put(schema.memberName(), value);
    }

    @Override
    public void writeStruct(Schema schema, SerializableStruct struct) {
        struct.serializeMembers(this);
    }
}
