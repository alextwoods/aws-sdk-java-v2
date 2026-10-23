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

package software.amazon.awssdk.schema.serde;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Tests for the {@link ShapeSerializer} default and static methods, and {@code Document.serialize} (Requirements 7.1,
 * 7.4, 18.1).
 */
class ShapeSerializerTest {

    @Test
    void nullSerializer_returnsSameInstance() {
        assertThat(ShapeSerializer.nullSerializer()).isSameAs(ShapeSerializer.nullSerializer());
    }

    @Test
    void nullSerializer_acceptsEveryWriteWithoutFailing() {
        ShapeSerializer serializer = ShapeSerializer.nullSerializer();

        serializer.writeBoolean(PreludeSchemas.BOOLEAN, true);
        serializer.writeString(PreludeSchemas.STRING, "a");
        serializer.writeBlob(PreludeSchemas.BLOB, new byte[] {1});
        serializer.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.EPOCH);
        serializer.writeDocument(PreludeSchemas.DOCUMENT, Document.of(1));
        serializer.writeNull(PreludeSchemas.STRING);
        serializer.writeList(PreludeSchemas.DOCUMENT, Collections.emptyList(), 0,
                             (state, s) -> {
                                 throw new AssertionError("consumer must not be called");
                             });
        serializer.writeMap(PreludeSchemas.DOCUMENT, Collections.emptyMap(), 0,
                            (state, s) -> {
                                throw new AssertionError("consumer must not be called");
                            });
        serializer.flush();
        serializer.close();
    }

    @Test
    void close_byDefault_callsFlushOnce() {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        serializer.close();

        assertThat(serializer.flushCount()).isEqualTo(1);
    }

    @Test
    void writeBlobByteArray_byDefault_passesAReadOnlyViewWithoutCopying() {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();
        byte[] bytes = {1, 2, 3};

        serializer.writeBlob(PreludeSchemas.BLOB, bytes);

        assertThat(serializer.calls()).hasSize(1);
        RecordingShapeSerializer.Call call = serializer.calls().get(0);
        assertThat(call.method()).isEqualTo("writeBlob");
        assertThat(call.schema()).isSameAs(PreludeSchemas.BLOB);
        ByteBuffer buffer = (ByteBuffer) call.value();
        assertThat(buffer.isReadOnly()).isTrue();
        assertThat(buffer.position()).isZero();
        assertThat(buffer.remaining()).isEqualTo(3);
        assertThat(buffer.get(0)).isEqualTo((byte) 1);
        assertThat(buffer.get(1)).isEqualTo((byte) 2);
        assertThat(buffer.get(2)).isEqualTo((byte) 3);
        // The view shares the array: a later change to the array shows through it.
        bytes[0] = 9;
        assertThat(buffer.get(0)).isEqualTo((byte) 9);
    }

    @Test
    void writeBlobByteArray_byDefault_passesAnEmptyReadOnlyViewForAnEmptyArray() {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();

        serializer.writeBlob(PreludeSchemas.BLOB, new byte[0]);

        ByteBuffer buffer = (ByteBuffer) serializer.calls().get(0).value();
        assertThat(buffer.isReadOnly()).isTrue();
        assertThat(buffer.remaining()).isZero();
    }

    @Test
    void documentSerialize_callsWriteDocumentOnceWithPreludeDocumentSchema() {
        RecordingShapeSerializer serializer = new RecordingShapeSerializer();
        Document document = Document.of("a");

        document.serialize(serializer);

        assertThat(serializer.calls()).hasSize(1);
        RecordingShapeSerializer.Call call = serializer.calls().get(0);
        assertThat(call.method()).isEqualTo("writeDocument");
        assertThat(call.schema()).isSameAs(PreludeSchemas.DOCUMENT);
        assertThat(call.value()).isSameAs(document);
    }
}
