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

package software.amazon.awssdk.checksums;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;

/** Test fixtures: a map-backed modeled input, request builders, and body readers for both views. */
final class TestSupport {
    private TestSupport() {
    }

    /** A modeled input whose member values are held by member name; serialization is not exercised. */
    static final class MapStruct implements SerializableStruct {
        private final Schema schema;
        private final Map<String, Object> values = new HashMap<>();

        MapStruct(Schema schema) {
            this.schema = schema;
        }

        MapStruct with(String member, Object value) {
            values.put(member, value);
            return this;
        }

        @Override
        public Schema schema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) values.get(member.memberName());
        }
    }

    static SdkHttpRequest.Builder request() {
        return SdkHttpRequest.builder().method("PUT").host("example.com").encodedPath("/key");
    }

    static byte[] readStream(HttpBody body) {
        try (InputStream in = body.streamingBody().openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Collects a publisher with an unbounded request; fails on error. */
    static byte[] readPublisher(Publisher<ByteBuffer> publisher) {
        CompletableFuture<byte[]> result = collect(publisher, Long.MAX_VALUE);
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(e.getCause());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static CompletableFuture<byte[]> collect(Publisher<ByteBuffer> publisher, long demandPerChunk) {
        CompletableFuture<byte[]> result = new CompletableFuture<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        publisher.subscribe(new Subscriber<>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(demandPerChunk);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
                byte[] bytes = new byte[chunk.remaining()];
                chunk.get(bytes);
                out.writeBytes(bytes);
                if (demandPerChunk != Long.MAX_VALUE) {
                    subscription.request(demandPerChunk);
                }
            }

            @Override
            public void onError(Throwable t) {
                result.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                result.complete(out.toByteArray());
            }
        });
        return result;
    }
}
