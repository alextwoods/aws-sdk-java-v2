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

package software.amazon.awssdk.core.async;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** Records signals without requesting anything by itself; tests drive demand through {@link #subscription}. */
final class RecordingSubscriber implements Subscriber<ByteBuffer> {
    final List<ByteBuffer> chunks = new ArrayList<>();
    Subscription subscription;
    Throwable error;
    boolean completed;

    @Override
    public void onSubscribe(Subscription s) {
        subscription = s;
    }

    @Override
    public void onNext(ByteBuffer chunk) {
        chunks.add(chunk);
    }

    @Override
    public void onError(Throwable t) {
        error = t;
    }

    @Override
    public void onComplete() {
        completed = true;
    }

    byte[] bytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (ByteBuffer chunk : chunks) {
            ByteBuffer view = chunk.duplicate();
            byte[] b = new byte[view.remaining()];
            view.get(b);
            out.writeBytes(b);
        }
        return out.toByteArray();
    }

    /** Subscribes to {@code publisher}, requests everything, and returns the recorder (the publishers are synchronous). */
    static RecordingSubscriber drain(org.reactivestreams.Publisher<ByteBuffer> publisher) {
        RecordingSubscriber subscriber = new RecordingSubscriber();
        publisher.subscribe(subscriber);
        subscriber.subscription.request(Long.MAX_VALUE);
        return subscriber;
    }
}
