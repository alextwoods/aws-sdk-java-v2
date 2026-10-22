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

package software.amazon.awssdk.core.schema.internal;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.schema.io.DataStream;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link DataStream} over an {@link AsyncRequestBody}, which it keeps so that the request pipeline can send the body
 * itself. A subscription is a subscription to the body.
 */
@SdkInternalApi
public final class AsyncRequestBodyDataStream implements DataStream {
    private final AsyncRequestBody body;

    public AsyncRequestBodyDataStream(AsyncRequestBody body) {
        this.body = Validate.paramNotNull(body, "body");
    }

    /**
     * Returns the request body this stream was created from.
     */
    public AsyncRequestBody body() {
        return body;
    }

    @Override
    public long contentLength() {
        return body.contentLength().orElse(-1L);
    }

    @Override
    public String contentType() {
        return body.contentType();
    }

    /**
     * Returns true: the SDK resubscribes to an async request body to retry a request, so its bodies publish the content
     * again from the beginning for each subscription.
     */
    @Override
    public boolean isReplayable() {
        return true;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        body.subscribe(subscriber);
    }

    @Override
    public String toString() {
        return "AsyncRequestBodyDataStream(body=" + body.body() + ", contentLength=" + contentLength() + ")";
    }
}
