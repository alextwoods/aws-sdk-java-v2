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

package software.amazon.awssdk.schema.serde.document;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.utils.Validate;

/**
 * A document that holds the {@link DataStream} of a streaming blob member. Its type is {@link ShapeType#BLOB}, but it
 * has no bytes in memory: {@link #asBlob()} throws, and {@link #asDataStream()} and {@link #asObject()} return the
 * stream. Two data stream documents are equal when they hold the same stream instance.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.document.DataStreamDocument}.
 */
@SdkInternalApi
final class DataStreamDocument extends Documents.AbstractDocument {
    private final DataStream dataStream;

    DataStreamDocument(Schema schema, DataStream dataStream) {
        super(Validate.paramNotNull(schema, "schema"));
        this.dataStream = Validate.paramNotNull(dataStream, "dataStream");
    }

    @Override
    public ShapeType type() {
        return ShapeType.BLOB;
    }

    @Override
    public DataStream asDataStream() {
        return dataStream;
    }

    @Override
    public ByteBuffer asBlob() {
        throw new SerializationException("A data stream document has no bytes in memory; read it with asDataStream()");
    }

    @Override
    public Object asObject() {
        return dataStream;
    }

    /**
     * Throws: a data stream has no bytes in memory, and a codec cannot write it. The SDK binds a streaming blob to the
     * request body instead of serializing it (smithy-java writes it with {@code writeDataStream}, which no SDK
     * serializer has).
     */
    @Override
    public void serializeContents(ShapeSerializer serializer) {
        throw new SerializationException("A data stream document cannot be serialized; a streaming blob member is sent "
                                         + "as the request body");
    }

    @Override
    public String toString() {
        return describe("data stream");
    }
}
