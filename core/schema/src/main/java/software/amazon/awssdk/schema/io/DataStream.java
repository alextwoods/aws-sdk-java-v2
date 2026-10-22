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

package software.amazon.awssdk.schema.io;

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The bytes of a streaming blob ({@code smithy.api#streaming} on a blob), published as {@link ByteBuffer}s, with the
 * content length and content type when they are known.
 *
 * <p>The schema module sits below {@code sdk-core}, so it cannot name the SDK's request bodies. A data stream is the
 * schema-level handle for them: {@code sdk-core}'s {@code SchemaSerdeSupport.dataStream} adapts a
 * {@code RequestBody} or an {@code AsyncRequestBody} to a data stream without reading or buffering it, and a
 * {@link software.amazon.awssdk.schema.serde.document.Document#of(software.amazon.awssdk.schema.Schema, DataStream)
 * data stream document} holds one at a streaming member of a structure document.
 *
 * <p>Each subscription publishes the bytes from the beginning when the stream {@link #isReplayable() is replayable}.
 *
 * <p>Port of the smithy-java {@code software.amazon.smithy.java.io.datastream.DataStream} contract, with an
 * {@code org.reactivestreams.Publisher} in place of {@code java.util.concurrent.Flow.Publisher} (Java 8). The blocking
 * views ({@code asInputStream}, {@code asByteBuffer}, {@code writeTo}, {@code asChannel}, {@code discard}) and the
 * factories ({@code ofBytes}, {@code ofFile}, ...) are not ported: SDK callers create streams with the
 * {@code RequestBody} and {@code AsyncRequestBody} factories.
 */
@SdkPublicApi
public interface DataStream extends Publisher<ByteBuffer> {

    /**
     * Returns the length of the stream in bytes, if known.
     *
     * @return the content length, or a negative number if it is not known
     */
    long contentLength();

    /**
     * Returns whether the stream has a known content length.
     *
     * @return true if {@link #contentLength()} is not negative
     */
    default boolean hasKnownLength() {
        return contentLength() >= 0;
    }

    /**
     * Returns the media type of the bytes, if known.
     *
     * @return the content type, or null if it is not known
     */
    String contentType();

    /**
     * Returns whether every new subscription publishes the bytes again from the beginning, so that the stream can be
     * read more than once (to sign it, or to retry a request).
     *
     * @return true if the stream can be replayed
     */
    boolean isReplayable();
}
