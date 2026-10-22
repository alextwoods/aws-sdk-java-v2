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

package software.amazon.awssdk.core.schema;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.schema.internal.AsyncRequestBodyDataStream;
import software.amazon.awssdk.core.schema.internal.LegacyDocumentAdapter;
import software.amazon.awssdk.core.schema.internal.RequestBodyDataStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Adapters between schema serde values and the {@code sdk-core} types of generated model classes (design A.6). The
 * schema module is below {@code sdk-core}, so it reads and writes blobs as {@link ByteBuffer}s and documents as schema
 * documents; generated classes hold {@link SdkBytes} and Legacy_Documents ({@link Document}).
 */
@SdkProtectedApi
public final class SchemaSerdeSupport {
    private SchemaSerdeSupport() {
    }

    /**
     * Converts a blob read by a {@code ShapeDeserializer} to {@link SdkBytes}. A buffer that wraps an entire, writable
     * array from its start (as a codec returns for a blob it had to copy) is wrapped without another copy; any other
     * buffer, such as a view into the response body, is copied.
     *
     * @param buffer the blob, or null
     * @return the bytes, or null if {@code buffer} is null
     */
    public static SdkBytes toSdkBytes(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        if (buffer.hasArray()
            && buffer.arrayOffset() == 0
            && buffer.position() == 0
            && buffer.remaining() == buffer.array().length) {
            return SdkBytes.fromByteArrayUnsafe(buffer.array());
        }
        return SdkBytes.fromByteArrayUnsafe(BinaryUtils.copyRemainingBytesFrom(buffer.duplicate()));
    }

    /**
     * Creates the list a generated list deserializer fills, sized from {@link ShapeDeserializer#containerSize()} capped
     * at {@link ShapeDeserializer#containerPreAllocationLimit()}. Call it when the deserializer is positioned at the
     * list.
     *
     * @param deserializer the deserializer positioned at a list
     * @param <T> the element type
     * @return a new, empty, mutable list
     */
    public static <T> List<T> newList(ShapeDeserializer deserializer) {
        int size = deserializer.containerSize();
        return size < 0 ? new ArrayList<>() : new ArrayList<>(Math.min(size, deserializer.containerPreAllocationLimit()));
    }

    /**
     * Creates the map a generated map deserializer fills, sized like {@link #newList(ShapeDeserializer)}. The map keeps
     * the order of the entries in the data, as the copiers of generated model classes do.
     *
     * @param deserializer the deserializer positioned at a map
     * @param <V> the value type
     * @return a new, empty, mutable map
     */
    public static <V> Map<String, V> newMap(ShapeDeserializer deserializer) {
        int size = deserializer.containerSize();
        if (size < 0) {
            return new LinkedHashMap<>();
        }
        int expected = Math.min(size, deserializer.containerPreAllocationLimit());
        return new LinkedHashMap<>(expected < 3 ? expected + 1 : (int) (expected / 0.75f + 1.0f));
    }

    /**
     * Returns a schema {@link software.amazon.awssdk.schema.serde.document.Document} view of a Legacy_Document, for
     * writing a {@code document} member with {@code ShapeSerializer.writeDocument}. Nothing is copied: list elements and
     * map values are converted as they are read. A number keeps its {@link software.amazon.awssdk.core.SdkNumber};
     * its {@code type()} is the first of integer, long, bigInteger, double and bigDecimal that holds the value exactly.
     * {@link #unwrapLegacy} of the view returns {@code document}. This is
     * {@link software.amazon.awssdk.core.document.DocumentConverter#toSchemaDocument} with the codecs' null: Java
     * {@code null} instead of a null-valued document, and no exception for a null argument.
     *
     * @param document the Legacy_Document, or null
     * @return the view, or null if {@code document} is null or a null document (a Smithy null)
     */
    public static software.amazon.awssdk.schema.serde.document.Document wrapLegacy(Document document) {
        return LegacyDocumentAdapter.toSchema(document);
    }

    /**
     * Returns a Legacy_Document view of a schema {@link software.amazon.awssdk.schema.serde.document.Document}, for the
     * value of a {@code document} member read with {@code ShapeDeserializer.readDocument}. Nothing is copied: list
     * elements and map values are converted as they are read, and the returned lists and maps are the SDK's own
     * {@code ListDocument}/{@code MapDocument}, so {@code equals}, {@code hashCode} and {@code toString} are unchanged.
     * Numbers become {@link software.amazon.awssdk.core.SdkNumber}s of the value's Java type; blobs become base64
     * strings, timestamps epoch seconds with at most three fraction digits, structures and unions maps of their members.
     * A view made by {@link #wrapLegacy} returns the original Legacy_Document. This is
     * {@link software.amazon.awssdk.core.document.DocumentConverter#toLegacy} with a null argument allowed.
     *
     * @param document the schema document, or null
     * @return the Legacy_Document; {@code Document.fromNull()} if {@code document} is null (a Smithy null)
     */
    public static Document unwrapLegacy(software.amazon.awssdk.schema.serde.document.Document document) {
        return LegacyDocumentAdapter.toLegacy(document);
    }

    /**
     * Builds an operation output from a builder that a protocol deserialized, after setting the HTTP response on the
     * builder, so that the built response is not copied through {@code toBuilder()} to attach it. A protocol that also
     * sets response metadata sets it on the same builder before calling this.
     *
     * @param builder the output's builder, which must also be an {@link SdkResponse.Builder}, as every generated
     *     response builder is
     * @param httpResponse the HTTP response
     * @param <T> the output type
     * @return the built output
     * @throws IllegalArgumentException if the builder is not an {@link SdkResponse.Builder}
     */
    public static <T extends SerializableStruct> T buildResponse(ShapeBuilder<T> builder, SdkHttpResponse httpResponse) {
        Validate.paramNotNull(builder, "builder");
        if (!(builder instanceof SdkResponse.Builder)) {
            throw new IllegalArgumentException("The builder of " + builder.sdkSchema().id()
                                               + " is not an SdkResponse.Builder");
        }
        ((SdkResponse.Builder) builder).sdkHttpResponse(httpResponse);
        return builder.build();
    }

    /**
     * Adapts a synchronous request body to the {@link DataStream} of a streaming blob, without reading or buffering it.
     * The stream has the body's content length (negative when the body has none) and content type, and
     * {@link #requestBody(DataStream)} returns the same body back, so the request pipeline sends the body itself. A
     * subscriber to the stream reads a new stream of the body's provider, with blocking reads, in chunks.
     *
     * @param body the request body
     * @return the data stream
     * @throws NullPointerException if {@code body} is null
     */
    public static DataStream dataStream(RequestBody body) {
        return new RequestBodyDataStream(body);
    }

    /**
     * Adapts an asynchronous request body to the {@link DataStream} of a streaming blob, without subscribing to it. The
     * stream has the body's content length (negative when the body has none) and content type, publishes the body's
     * bytes, and {@link #asyncRequestBody(DataStream)} returns the same body back.
     *
     * @param body the request body
     * @return the data stream
     * @throws NullPointerException if {@code body} is null
     */
    public static DataStream dataStream(AsyncRequestBody body) {
        return new AsyncRequestBodyDataStream(body);
    }

    /**
     * Returns the synchronous request body a data stream was created from with {@link #dataStream(RequestBody)}.
     *
     * @param dataStream the data stream
     * @return the request body, or empty if the stream was not created from a synchronous request body
     * @throws NullPointerException if {@code dataStream} is null
     */
    public static Optional<RequestBody> requestBody(DataStream dataStream) {
        Validate.paramNotNull(dataStream, "dataStream");
        return dataStream instanceof RequestBodyDataStream ? Optional.of(((RequestBodyDataStream) dataStream).body())
                                                           : Optional.empty();
    }

    /**
     * Returns the asynchronous request body a data stream was created from with {@link #dataStream(AsyncRequestBody)}.
     *
     * @param dataStream the data stream
     * @return the request body, or empty if the stream was not created from an asynchronous request body
     * @throws NullPointerException if {@code dataStream} is null
     */
    public static Optional<AsyncRequestBody> asyncRequestBody(DataStream dataStream) {
        Validate.paramNotNull(dataStream, "dataStream");
        return dataStream instanceof AsyncRequestBodyDataStream
               ? Optional.of(((AsyncRequestBodyDataStream) dataStream).body())
               : Optional.empty();
    }
}
