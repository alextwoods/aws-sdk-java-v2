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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.core.internal.http.StreamManagingStage;
import software.amazon.awssdk.core.internal.sync.BufferingContentStreamProvider;
import software.amazon.awssdk.core.internal.sync.CompressionContentStreamProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;

/**
 * Verifies which SDK wrapper {@link ContentStreamProvider}s pass through their delegate's
 * {@link ContentStreamProvider#contentAsByteBufferOrNull()} (the bytes are unchanged) and which drop it (the bytes are
 * transformed).
 */
class ContentStreamProviderByteBufferPropagationTest {
    private static final byte[] BODY = "zero-copy body".getBytes(StandardCharsets.UTF_8);

    @Test
    void streamManagingStage_propagatesDelegateBuffer() throws Exception {
        ContentStreamProvider delegate = ContentStreamProvider.fromByteArrayUnsafe(BODY);
        AtomicReference<ContentStreamProvider> seen = new AtomicReference<>();
        StreamManagingStage<Void> stage = new StreamManagingStage<>((request, context) -> {
            seen.set(request.contentStreamProvider().get());
            return null;
        });

        stage.execute(request(delegate), null);

        assertThat(seen.get()).isNotSameAs(delegate);
        assertBackedByBody(seen.get().contentAsByteBufferOrNull());
    }

    @Test
    void streamManagingStage_delegateWithoutBuffer_returnsNull() throws Exception {
        AtomicReference<ContentStreamProvider> seen = new AtomicReference<>();
        StreamManagingStage<Void> stage = new StreamManagingStage<>((request, context) -> {
            seen.set(request.contentStreamProvider().get());
            return null;
        });

        stage.execute(request(() -> new ByteArrayInputStream(BODY)), null);

        assertThat(seen.get().contentAsByteBufferOrNull()).isNull();
    }

    @Test
    void bufferingProvider_propagatesDelegateBuffer() {
        ContentStreamProvider provider =
            new BufferingContentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(BODY), null);

        assertBackedByBody(provider.contentAsByteBufferOrNull());
    }

    @Test
    void bufferingProvider_delegateWithoutBuffer_returnsNull() {
        ContentStreamProvider provider = new BufferingContentStreamProvider(() -> new ByteArrayInputStream(BODY), null);

        assertThat(provider.contentAsByteBufferOrNull()).isNull();
    }

    @Test
    void compressionProvider_dropsDelegateBuffer() {
        ContentStreamProvider provider =
            new CompressionContentStreamProvider(ContentStreamProvider.fromByteArrayUnsafe(BODY), new GzipCompressor());

        assertThat(provider.contentAsByteBufferOrNull()).isNull();
    }

    @Test
    void checksumCalculatingProvider_dropsDelegateBuffer() {
        ChecksumSpecs specs = ChecksumSpecs.builder()
                                           .algorithm(Algorithm.CRC32)
                                           .headerName("x-amz-checksum-crc32")
                                           .build();
        ContentStreamProvider provider = new HttpChecksumStage.ChecksumCalculatingStreamProvider(
            ContentStreamProvider.fromByteArrayUnsafe(BODY), specs, PayloadChecksumStore.create());

        assertThat(provider.contentAsByteBufferOrNull()).isNull();
    }

    private static void assertBackedByBody(ByteBuffer buffer) {
        assertThat(buffer).isNotNull();
        assertThat(buffer.hasArray()).isTrue();
        assertThat(buffer.array()).isSameAs(BODY);
        assertThat(buffer.position()).isZero();
        assertThat(buffer.remaining()).isEqualTo(BODY.length);
    }

    private static SdkHttpFullRequest request(ContentStreamProvider provider) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("https://localhost"))
                                 .method(SdkHttpMethod.POST)
                                 .contentStreamProvider(provider)
                                 .build();
    }
}
