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

package software.amazon.awssdk.checksums.internal;

import java.nio.ByteBuffer;
import java.util.Objects;
import software.amazon.awssdk.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.checksums.ChecksumMismatchException;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * Passes a response body through unchanged while computing its checksum, and fails the consumption at the end of the
 * body, before end-of-stream or {@code onComplete} is signalled, if the checksum differs from the expected value.
 */
public final class ChecksumValidatingTransform implements ChunkTransform {
    private final SdkChecksum checksum;
    private final String expected;

    public ChecksumValidatingTransform(ChecksumAlgorithm algorithm, String expectedBase64) {
        this.checksum = SdkChecksum.create(algorithm);
        this.expected = Objects.requireNonNull(expectedBase64, "expectedBase64");
    }

    @Override
    public ByteBuffer onChunk(ByteBuffer chunk) {
        checksum.update(chunk.duplicate());
        return chunk;
    }

    @Override
    public ByteBuffer onComplete() {
        String computed = checksum.digestBase64();
        if (!computed.equals(expected)) {
            throw ChecksumMismatchException.create(checksum.algorithm(), expected, computed);
        }
        return ByteBuffer.allocate(0);
    }
}
