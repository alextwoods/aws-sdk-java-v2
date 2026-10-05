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

package software.amazon.awssdk.smithy.runtime.fixture;

import java.nio.ByteBuffer;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.Codec;

/**
 * A generated-style client whose operation method holds a static operation descriptor reference and performs the
 * schema-driven request/response SERDE that a V3 protocol runtime performs. The "transport" echoes the serialized
 * request bytes as the response body, so every call exercises serialization, deserialization and builders through
 * the configured codec without any HTTP dependency.
 */
public final class WidgetClient {

    private final Codec codec;

    public WidgetClient(Codec codec) {
        this.codec = codec;
    }

    public Widget putWidget(Widget request) {
        return invoke(WidgetService.PUT_WIDGET, request);
    }

    /** Returns the wire bytes the client transmits for a request. */
    public byte[] wireBytes(Widget request) {
        return toArray(codec.serialize(request));
    }

    private <I extends SerializableStruct, O extends SerializableStruct> O invoke(ApiOperation<I, O> operation,
                                                                                  I request) {
        ByteBuffer requestBody = codec.serialize(request);
        ByteBuffer responseBody = requestBody.asReadOnlyBuffer();
        return codec.deserializeShape(responseBody, operation.outputBuilder());
    }

    private static byte[] toArray(ByteBuffer buffer) {
        ByteBuffer copy = buffer.asReadOnlyBuffer();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return bytes;
    }
}
