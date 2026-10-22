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

package software.amazon.awssdk.dynamicclient;

import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * Creates the documents of streaming input members for {@link DynamicClient} calls.
 *
 * <p>A member that targets a blob with {@code smithy.api#streaming} takes a data stream document over the request
 * body: {@link #dataStream(RequestBody)} for {@link DynamicClient#call call} and
 * {@link #dataStream(AsyncRequestBody)} for {@link DynamicClient#callAsync callAsync}. The client sends the body as
 * the HTTP request body, as a generated client sends the body passed to its operation method; neither method reads the
 * body.
 *
 * <pre>{@code
 * Document input = Document.of(Collections.singletonMap("body", DynamicStreams.dataStream(RequestBody.fromFile(path))));
 * client.call("UploadArchive", input);
 * }</pre>
 *
 * <p>A member that targets a union with {@code smithy.api#streaming} (an event stream) takes an event stream document
 * from {@link #eventStream(Publisher)}.
 */
@SdkPublicApi
public final class DynamicStreams {
    private DynamicStreams() {
    }

    /**
     * Creates the document of a streaming blob member from a synchronous request body, for {@link DynamicClient#call}.
     * It is {@code Document.of(PreludeSchemas.BLOB, dataStream)}, with the data stream adapted from the body by
     * {@link SchemaSerdeSupport#dataStream(RequestBody)}; the call gives it the member's schema.
     *
     * @param body the request body
     * @return the data stream document
     * @throws NullPointerException if {@code body} is null
     */
    public static Document dataStream(RequestBody body) {
        return Document.of(PreludeSchemas.BLOB, SchemaSerdeSupport.dataStream(body));
    }

    /**
     * Creates the document of a streaming blob member from an asynchronous request body, for
     * {@link DynamicClient#callAsync}. It is {@code Document.of(PreludeSchemas.BLOB, dataStream)}, with the data
     * stream adapted from the body by {@link SchemaSerdeSupport#dataStream(AsyncRequestBody)}; the call gives it the
     * member's schema.
     *
     * @param body the request body
     * @return the data stream document
     * @throws NullPointerException if {@code body} is null
     */
    public static Document dataStream(AsyncRequestBody body) {
        return Document.of(PreludeSchemas.BLOB, SchemaSerdeSupport.dataStream(body));
    }

    /**
     * Creates the document of an event stream member from a publisher of events, each a map or structure document with
     * one member set, the event's union member. It is {@code Document.ofEventStream(PreludeSchemas.DOCUMENT, events)}.
     *
     * <p>The dynamic client does not send event streams yet: a call whose operation has an event stream fails before
     * sending (see {@link DynamicClient#callAsync(String, Document)}).
     *
     * @param events the events
     * @return the event stream document
     * @throws NullPointerException if {@code events} is null
     */
    public static Document eventStream(Publisher<Document> events) {
        return Document.ofEventStream(PreludeSchemas.DOCUMENT, events);
    }
}
