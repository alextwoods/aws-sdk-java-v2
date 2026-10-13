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

package software.amazon.awssdk.codecs.cbor.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.serde.SerializationException;

/**
 * Thrown for input that is not well-formed CBOR. Stack traces are not filled in unless requested, because malformed
 * input is an expected failure on the read path.
 *
 * <p>Port of smithy-java {@code BadCborException}.
 */
@SdkInternalApi
final class BadCborException extends SerializationException {
    private static final long serialVersionUID = 1L;

    BadCborException(String message) {
        this(message, false);
    }

    BadCborException(String message, boolean includeTrace) {
        super(message);
        if (includeTrace) {
            super.fillInStackTrace();
        }
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
