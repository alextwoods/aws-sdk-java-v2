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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import software.amazon.awssdk.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;

/** An {@link SdkChecksum} over a JDK {@link MessageDigest}; every JDK provides the algorithms used here. */
public final class DigestChecksum implements SdkChecksum {
    private final ChecksumAlgorithm algorithm;
    private final MessageDigest digest;

    public DigestChecksum(ChecksumAlgorithm algorithm, String jdkAlgorithm) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        try {
            this.digest = MessageDigest.getInstance(jdkAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform must provide MD5, SHA-1, SHA-256 and SHA-512 (MessageDigest javadoc).
            throw new IllegalStateException("The JDK does not provide " + jdkAlgorithm, e);
        }
    }

    @Override
    public ChecksumAlgorithm algorithm() {
        return algorithm;
    }

    @Override
    public void update(byte[] bytes, int offset, int length) {
        digest.update(bytes, offset, length);
    }

    @Override
    public void update(ByteBuffer buffer) {
        digest.update(buffer);
    }

    @Override
    public byte[] digest() {
        return digest.digest();
    }

    @Override
    public void reset() {
        digest.reset();
    }
}
