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

package software.amazon.awssdk.testutils.mock;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Validate;

/**
 * A thread-safe queue of {@link MockedResult}s that a {@link MockPlugin} returns to client calls, first in, first out.
 * Each result is consumed by exactly one call (one attempt, when the client retries).
 *
 * <p>Port of smithy-java {@code MockQueue}.
 */
@SdkPublicApi
@ThreadSafe
public final class MockQueue {
    private final Queue<MockedResult> queue = new ConcurrentLinkedQueue<>();

    /**
     * Removes and returns the next result.
     *
     * @return the result, or null if the queue is empty
     */
    public MockedResult poll() {
        return queue.poll();
    }

    /**
     * Returns the number of results that have not been consumed.
     *
     * @return the remaining result count
     */
    public int remaining() {
        return queue.size();
    }

    /**
     * Removes every result.
     *
     * @return this queue
     */
    public MockQueue clear() {
        queue.clear();
        return this;
    }

    /**
     * Enqueues a raw HTTP response, which the client's protocol deserializes unchanged.
     *
     * @param response the response
     * @return this queue
     * @throws NullPointerException if {@code response} is null
     */
    public MockQueue enqueue(SdkHttpFullResponse response) {
        queue.add(MockedResult.response(response));
        return this;
    }

    /**
     * Enqueues an output value or a schema-backed Document, serialized when a call consumes it with the
     * {@link ResponseGenerator} of the client's protocol. A modeled exception can be enqueued here too: its schema has
     * the {@code error} trait, so it becomes an error response.
     *
     * @param output the output to enqueue
     * @return this queue
     * @throws NullPointerException if {@code output} is null
     */
    public MockQueue enqueue(SerializableStruct output) {
        return enqueue(null, output);
    }

    /**
     * Enqueues a schema-backed Document, serialized when a call consumes it as the structure of its schema, with the
     * {@link ResponseGenerator} of the client's protocol. A document whose schema has the {@code error} trait becomes an
     * error response.
     *
     * <p>This is the form for dynamic clients, whose outputs are documents:
     * <pre>{@code
     * queue.enqueueDocument(client.createStruct(ShapeId.from("ns#GetThingOutput"), Document.of(members)));
     * }</pre>
     * {@code Document.of(SerializableShape)} of a generated output is schema-backed as well.
     *
     * @param document a document that is a {@link SerializableStruct}, such as the result of the dynamic client's
     *                 {@code createStruct} or of {@code Document.of(SerializableShape)} for a structure
     * @return this queue
     * @throws NullPointerException if {@code document} is null
     * @throws IllegalArgumentException if {@code document} has no schema, for example because it was created with
     *     {@code Document.of(Map)}
     */
    public MockQueue enqueueDocument(Document document) {
        Validate.paramNotNull(document, "document");
        if (!(document instanceof SerializableStruct)) {
            throw new IllegalArgumentException(
                "Only a schema-backed document can be enqueued, but the " + document.type() + " document has no schema. "
                + "Create it from the output's schema, for example with DynamicClient.createStruct(outputShapeId, "
                + "document) or Document.of(output).");
        }
        return enqueue((SerializableStruct) document);
    }

    /**
     * Enqueues a value serialized with a specific generator.
     *
     * @param generator the generator, or null for the generator of the client's protocol
     * @param output the output to enqueue
     * @return this queue
     * @throws NullPointerException if {@code output} is null
     */
    public MockQueue enqueue(ResponseGenerator generator, SerializableStruct output) {
        queue.add(MockedResult.output(output, generator));
        return this;
    }

    /**
     * Enqueues a modeled exception, serialized into the protocol's error response when a call consumes it. The client
     * deserializes the response and throws an exception of the same modeled type.
     *
     * @param modeledError the modeled error, whose schema has the {@code error} trait
     * @return this queue
     * @throws NullPointerException if {@code modeledError} is null
     * @throws IllegalArgumentException if the schema of {@code modeledError} has no {@code error} trait
     */
    public MockQueue enqueueError(SerializableStruct modeledError) {
        Validate.paramNotNull(modeledError, "modeledError");
        Validate.isTrue(modeledError.sdkSchema().hasTrait(TraitKey.ERROR_TRAIT),
                        "%s is not a modeled error: its schema has no error trait", modeledError.sdkSchema().id());
        queue.add(MockedResult.output(modeledError));
        return this;
    }

    /**
     * Enqueues an exception that the transport throws as it is, without a response.
     *
     * @param failure the exception, for example an {@code SdkClientException} wrapping an {@code IOException}
     * @return this queue
     * @throws NullPointerException if {@code failure} is null
     */
    public MockQueue enqueueFailure(Throwable failure) {
        queue.add(MockedResult.error(failure));
        return this;
    }
}
