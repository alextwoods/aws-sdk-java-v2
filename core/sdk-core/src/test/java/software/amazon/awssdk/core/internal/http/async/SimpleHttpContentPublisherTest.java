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

package software.amazon.awssdk.core.internal.http.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.BinaryUtils;

class SimpleHttpContentPublisherTest {
    private static final byte[] BODY = "zero-copy body".getBytes(StandardCharsets.UTF_8);

    @Test
    void providerWithBuffer_publishesBufferOverSameArray() {
        SimpleHttpContentPublisher publisher =
            new SimpleHttpContentPublisher(request(ContentStreamProvider.fromByteArrayUnsafe(BODY)));

        ByteBuffer published = subscribeOnce(publisher);

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(published.hasArray()).isTrue();
        assertThat(published.array()).isSameAs(BODY);
        assertThat(BinaryUtils.copyRemainingBytesFrom(published)).isEqualTo(BODY);
    }

    @Test
    void providerWithBuffer_doesNotReadStream() {
        ContentStreamProvider provider = new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                throw new AssertionError("newStream() must not be called when a buffer is available");
            }

            @Override
            public ByteBuffer contentAsByteBufferOrNull() {
                return ByteBuffer.wrap(BODY);
            }
        };

        ByteBuffer published = subscribeOnce(new SimpleHttpContentPublisher(request(provider)));

        assertThat(published.array()).isSameAs(BODY);
    }

    @Test
    void bufferWithOffset_publishesOnlyRemainingBytes() {
        ContentStreamProvider provider = new ContentStreamProvider() {
            @Override
            public InputStream newStream() {
                return new ByteArrayInputStream(BODY, 5, 4);
            }

            @Override
            public ByteBuffer contentAsByteBufferOrNull() {
                return ByteBuffer.wrap(BODY, 5, 4);
            }
        };
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(request(provider));

        ByteBuffer published = subscribeOnce(publisher);

        assertThat(publisher.contentLength()).hasValue(4L);
        assertThat(published.array()).isSameAs(BODY);
        assertThat(BinaryUtils.copyRemainingBytesFrom(published)).isEqualTo("copy".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void eachSubscription_receivesTheCompleteBody() {
        SimpleHttpContentPublisher publisher =
            new SimpleHttpContentPublisher(request(ContentStreamProvider.fromByteArrayUnsafe(BODY)));

        ByteBuffer first = subscribeOnce(publisher);
        first.position(first.limit());
        ByteBuffer second = subscribeOnce(publisher);

        assertThat(second).isNotSameAs(first);
        assertThat(second.remaining()).isEqualTo(BODY.length);
        assertThat(second.array()).isSameAs(BODY);
    }

    @Test
    void providerWithoutBuffer_readsStream() {
        SimpleHttpContentPublisher publisher =
            new SimpleHttpContentPublisher(request(() -> new ByteArrayInputStream(BODY)));

        ByteBuffer published = subscribeOnce(publisher);

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(BinaryUtils.copyRemainingBytesFrom(published)).isEqualTo(BODY);
    }

    @Test
    void noProvider_publishesEmptyBody() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(request(null));

        assertThat(publisher.contentLength()).hasValue(0L);
        assertThat(subscribeOnce(publisher).remaining()).isZero();
    }

    private static ByteBuffer subscribeOnce(SimpleHttpContentPublisher publisher) {
        List<ByteBuffer> received = new ArrayList<>();
        boolean[] completed = new boolean[1];
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                received.add(byteBuffer);
            }

            @Override
            public void onError(Throwable t) {
                throw new AssertionError(t);
            }

            @Override
            public void onComplete() {
                completed[0] = true;
            }
        });
        assertThat(completed[0]).isTrue();
        assertThat(received).hasSize(1);
        return received.get(0);
    }

    private static SdkHttpFullRequest request(ContentStreamProvider provider) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("https://localhost"))
                                 .method(SdkHttpMethod.POST)
                                 .contentStreamProvider(provider)
                                 .build();
    }
}
