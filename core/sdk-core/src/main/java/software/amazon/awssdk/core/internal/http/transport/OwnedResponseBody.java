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

package software.amazon.awssdk.core.internal.http.transport;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * A response body that {@link SdkAsyncHttpClientTransport} aggregated into an array that nothing else references.
 *
 * <p>The transport copies the received body once into that array, sets the response content to a stream over it, and
 * records the body in the call's {@link ExecutionAttributes}. A protocol reading the body (for example
 * {@code ResponseBodies.asByteBuffer} in {@code http-client-protocol}) calls {@link #ownedBuffer} to parse the array
 * directly instead of copying the stream into a new one.
 *
 * <p>{@link #ownedBuffer} only returns the buffer while the response still carries the transport's content stream and
 * nothing has been read from it. If an interceptor replaced or consumed the content, the caller reads the stream as
 * usual.
 */
@SdkProtectedApi
public final class OwnedResponseBody {
    private static final ExecutionAttribute<OwnedResponseBody> OWNED_RESPONSE_BODY =
        new ExecutionAttribute<>("OwnedResponseBody");

    private final byte[] bytes;
    private final int length;
    private final ByteArrayInputStream stream;
    private final AbortableInputStream content;

    private OwnedResponseBody(byte[] bytes, int length) {
        this.bytes = bytes;
        this.length = length;
        this.stream = new ByteArrayInputStream(bytes, 0, length);
        this.content = AbortableInputStream.create(stream);
    }

    static OwnedResponseBody of(byte[] bytes, int length) {
        return new OwnedResponseBody(bytes, length);
    }

    /**
     * Returns the owned body of the response, without copying it, if the response's content is still the unread stream
     * the asynchronous transport set.
     *
     * @param context the attributes of the call
     * @param response the response whose body to read
     * @return a buffer over the owned array, positioned at the start of the body, with a backing array; or null if the
     *     response does not carry an owned body, in which case the caller reads {@code response.content()}
     */
    public static ByteBuffer ownedBuffer(ExecutionAttributes context, SdkHttpFullResponse response) {
        OwnedResponseBody owned = context.getAttribute(OWNED_RESPONSE_BODY);
        if (owned == null || !response.content().isPresent() || response.content().get() != owned.content) {
            return null;
        }
        if (owned.stream.available() != owned.length) {
            return null;
        }
        return ByteBuffer.wrap(owned.bytes, 0, owned.length);
    }

    /**
     * Records this body as the call's owned body, replacing the body of an earlier attempt.
     */
    void attach(ExecutionAttributes context) {
        context.putAttribute(OWNED_RESPONSE_BODY, this);
    }

    /**
     * Clears the call's owned body, for an attempt whose body the transport did not aggregate.
     */
    static void detach(ExecutionAttributes context) {
        context.putAttribute(OWNED_RESPONSE_BODY, null);
    }

    AbortableInputStream content() {
        return content;
    }

    int length() {
        return length;
    }
}
