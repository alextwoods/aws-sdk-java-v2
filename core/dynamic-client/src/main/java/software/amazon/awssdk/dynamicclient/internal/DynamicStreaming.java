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

package software.amazon.awssdk.dynamicclient.internal;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.schema.SchemaSerdeSupport;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.dynamicclient.DynamicOperation;
import software.amazon.awssdk.dynamicclient.DynamicResponse;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * The streaming members of dynamic calls (Requirements 31.1, 31.4 and 31.6): the checks a call's input and form must
 * pass before anything is sent, the request bodies of streaming blob inputs, and the response transformers of
 * streaming blob outputs.
 *
 * <p>A streaming blob input member holds a data stream document over a {@link RequestBody} for a synchronous call or an
 * {@link AsyncRequestBody} for an asynchronous call; the call passes the body to the client handler, as a generated
 * client's operation method does, and the request pipeline binds it. A streaming blob output is delivered to the
 * caller's response transformer with the output document.
 *
 * <p>Event streams are not supported yet: the schema-based request pipeline cannot send or receive event streams
 * (task 7.20 stage (c) is deferred), so a call of an operation with an event stream fails before sending, after its
 * input event stream member is checked.
 */
@SdkInternalApi
public final class DynamicStreaming {
    private DynamicStreaming() {
    }

    /**
     * Checks the streaming members of a call before its input is coerced, so that no interceptor runs and nothing is
     * sent when they fail.
     *
     * @param operation the operation
     * @param input the call's input, before coercion
     * @param async whether the call is asynchronous
     * @param transformer whether the call has a response transformer
     * @throws IllegalArgumentException naming the operation and the member, if a streaming member is misused: an event
     *     stream operation called synchronously; a required streaming input member that is absent; a streaming input
     *     member whose value is not the stream document the call form needs; a streaming blob output called without a
     *     response transformer, or a response transformer for an operation without one
     * @throws UnsupportedOperationException for an event stream operation called asynchronously with a valid input
     */
    public static void validate(DynamicOperation operation, Document input, boolean async, boolean transformer) {
        Schema inputStream = operation.inputStreamMember();
        Schema outputStream = operation.outputStreamMember();
        Schema eventStream = isEventStream(inputStream) ? inputStream
                                                        : isEventStream(outputStream) ? outputStream : null;
        if (eventStream != null) {
            if (!async) {
                throw new IllegalArgumentException(
                    "Operation " + operation.schema().id() + " has the event stream member " + eventStream.id()
                    + ", so it can be called only with callAsync. No request was sent.");
            }
            if (eventStream == inputStream) {
                validateInputMember(operation, inputStream, member(input, inputStream), true);
            }
            throw new UnsupportedOperationException(
                "The dynamic client does not support event streams yet: operation " + operation.schema().id()
                + " has the event stream member " + eventStream.id() + ". No request was sent.");
        }
        if (inputStream != null) {
            validateInputMember(operation, inputStream, member(input, inputStream), async);
        }
        if (outputStream != null && !transformer) {
            throw new IllegalArgumentException(
                "Operation " + operation.schema().id() + " has the streaming blob output member " + outputStream.id()
                + ", so it can be called only with a " + (async ? "AsyncResponseTransformer" : "ResponseTransformer")
                + ". No request was sent.");
        }
        if (outputStream == null && transformer) {
            throw new IllegalArgumentException(
                "Operation " + operation.schema().id() + " has no streaming blob output member, so it cannot be "
                + "called with a response transformer. No request was sent.");
        }
    }

    /**
     * Returns the synchronous request body of a coerced input's streaming blob member, if the operation has one and
     * the input sets it. {@link #validate} has checked its type.
     */
    public static Optional<RequestBody> requestBody(DynamicOperation operation, Document input) {
        return dataStream(operation, input).flatMap(SchemaSerdeSupport::requestBody);
    }

    /**
     * Returns the asynchronous request body of a coerced input's streaming blob member, if the operation has one and
     * the input sets it. {@link #validate} has checked its type.
     */
    public static Optional<AsyncRequestBody> asyncRequestBody(DynamicOperation operation, Document input) {
        return dataStream(operation, input).flatMap(SchemaSerdeSupport::asyncRequestBody);
    }

    /**
     * Adapts a caller's response transformer of output documents to the response type of the pipeline.
     */
    public static <ReturnT> ResponseTransformer<DynamicResponse, ReturnT> responseTransformer(
        ResponseTransformer<Document, ReturnT> transformer) {
        return new ResponseTransformer<DynamicResponse, ReturnT>() {
            @Override
            public ReturnT transform(DynamicResponse response, AbortableInputStream inputStream) throws Exception {
                return transformer.transform(response.document(), inputStream);
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return transformer.needsConnectionLeftOpen();
            }

            @Override
            public String name() {
                return transformer.name();
            }
        };
    }

    /**
     * Adapts a caller's asynchronous response transformer of output documents to the response type of the pipeline.
     */
    public static <ReturnT> AsyncResponseTransformer<DynamicResponse, ReturnT> asyncResponseTransformer(
        AsyncResponseTransformer<Document, ReturnT> transformer) {
        return new AsyncResponseTransformer<DynamicResponse, ReturnT>() {
            @Override
            public CompletableFuture<ReturnT> prepare() {
                return transformer.prepare();
            }

            @Override
            public void onResponse(DynamicResponse response) {
                transformer.onResponse(response.document());
            }

            @Override
            public void onStream(SdkPublisher<ByteBuffer> publisher) {
                transformer.onStream(publisher);
            }

            @Override
            public void exceptionOccurred(Throwable error) {
                transformer.exceptionOccurred(error);
            }

            @Override
            public String name() {
                return transformer.name();
            }
        };
    }

    private static void validateInputMember(DynamicOperation operation, Schema member, Document value,
                                            boolean async) {
        boolean events = member.type() == ShapeType.UNION;
        String expected = events ? "an event stream document from DynamicStreams.eventStream(Publisher)"
                                 : async ? "a data stream document from DynamicStreams.dataStream(AsyncRequestBody)"
                                         : "a data stream document from DynamicStreams.dataStream(RequestBody)";
        if (value == null) {
            if (member.hasTrait(TraitKey.REQUIRED_TRAIT)) {
                throw new IllegalArgumentException(
                    "Operation " + operation.schema().id() + " needs a value for its required streaming member "
                    + member.id() + ": " + expected + ". No request was sent.");
            }
            return;
        }
        String found = found(value, events, async);
        if (found != null) {
            throw new IllegalArgumentException(
                "Operation " + operation.schema().id() + ": the streaming member " + member.id() + " of "
                + (async ? "callAsync" : "call") + " needs " + expected + ", but found " + found
                + ". No request was sent.");
        }
    }

    /**
     * Describes a streaming member's value that does not fit, by its kind and never its contents, or returns null when
     * it fits.
     */
    private static String found(Document value, boolean events, boolean async) {
        ShapeType type = value.type();
        if (events) {
            return type == ShapeType.UNION && value.asObject() instanceof Publisher ? null : documentOf(type);
        }
        if (type != ShapeType.BLOB || !(value.asObject() instanceof DataStream)) {
            return documentOf(type);
        }
        DataStream stream = (DataStream) value.asObject();
        if (async) {
            return SchemaSerdeSupport.asyncRequestBody(stream).isPresent()
                   ? null
                   : "a data stream that was not created from an AsyncRequestBody";
        }
        return SchemaSerdeSupport.requestBody(stream).isPresent()
               ? null
               : "a data stream that was not created from a RequestBody";
    }

    private static String documentOf(ShapeType type) {
        String name = type.toString();
        return ("aeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name + " document";
    }

    private static Optional<DataStream> dataStream(DynamicOperation operation, Document input) {
        Schema member = operation.inputStreamMember();
        if (member == null || member.type() != ShapeType.BLOB) {
            return Optional.empty();
        }
        Document value = input.getMember(member.memberName());
        return value == null ? Optional.empty() : Optional.of(value.asDataStream());
    }

    /**
     * The value of a streaming member in an input that has not been coerced yet, or null when the input is not a map
     * or structure (coercion then fails) or does not set it.
     */
    private static Document member(Document input, Schema member) {
        ShapeType type = input.type();
        if (type != ShapeType.MAP && type != ShapeType.STRUCTURE && type != ShapeType.UNION) {
            return null;
        }
        return input.getMember(member.memberName());
    }

    private static boolean isEventStream(Schema member) {
        return member != null && member.type() == ShapeType.UNION;
    }
}
