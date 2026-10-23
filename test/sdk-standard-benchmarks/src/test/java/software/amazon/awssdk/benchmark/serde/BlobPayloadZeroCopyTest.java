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

package software.amazon.awssdk.benchmark.serde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Pins A3 on the path the JMH marshall benchmarks measure: the generated {@code PutObjectRequest} writes its
 * {@code @httpPayload} {@code Body} as {@code SdkBytes.asByteBuffer()}, and the request built by {@code marshallOnce()}
 * reads that {@link SdkBytes} array in place instead of a copy of it.
 */
class BlobPayloadZeroCopyTest {

    static Stream<Arguments> putObjectCases() {
        return Stream.of(Arguments.of(RestJsonMarshallBenchmark.class, "restJson1_PutObject_S"),
                         Arguments.of(RestJsonMarshallBenchmark.class, "restJson1_PutObject_M"),
                         Arguments.of(RestJsonMarshallBenchmark.class, "restJson1_PutObject_L"),
                         Arguments.of(RestXmlMarshallBenchmark.class, "restXml_PutObject_S"),
                         Arguments.of(RestXmlMarshallBenchmark.class, "restXml_PutObject_M"),
                         Arguments.of(RestXmlMarshallBenchmark.class, "restXml_PutObject_L"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("putObjectCases")
    void marshallOnce_withBlobPayload_sendsTheSdkBytesArrayWithoutCopying(Class<?> type, String testCaseId)
        throws Exception {
        Object benchmark = type.getDeclaredConstructor().newInstance();
        field(type, "testCaseId").set(benchmark, testCaseId);
        type.getMethod("setup").invoke(benchmark);
        Object input = field(type, "request").get(benchmark);
        SdkBytes body = (SdkBytes) input.getClass().getMethod("body").invoke(input);
        byte[] backing = body.asByteArrayUnsafe();

        SdkHttpFullRequest request = marshallOnce(benchmark);
        ContentStreamProvider provider = request.contentStreamProvider().get();

        assertThat(request.firstMatchingHeader("Content-Length")).hasValue(Integer.toString(backing.length));
        assertThat(read(provider)).isEqualTo(backing);
        ByteBuffer view = provider.contentAsByteBufferOrNull();
        assertThat(view).isNotNull();
        assertThat(view.isReadOnly()).isTrue();
        assertThat(view.remaining()).isEqualTo(backing.length);

        byte original = backing[0];
        try {
            backing[0] = (byte) ~original;
            try (InputStream stream = provider.newStream()) {
                assertThat(stream.read()).isEqualTo(backing[0] & 0xFF);
            }
        } finally {
            backing[0] = original;
        }
    }

    private static SdkHttpFullRequest marshallOnce(Object benchmark) {
        if (benchmark instanceof RestJsonMarshallBenchmark) {
            return ((RestJsonMarshallBenchmark) benchmark).marshallOnce();
        }
        return ((RestXmlMarshallBenchmark) benchmark).marshallOnce();
    }

    private static byte[] read(ContentStreamProvider provider) throws Exception {
        try (InputStream stream = provider.newStream()) {
            return IoUtils.toByteArray(stream);
        }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
