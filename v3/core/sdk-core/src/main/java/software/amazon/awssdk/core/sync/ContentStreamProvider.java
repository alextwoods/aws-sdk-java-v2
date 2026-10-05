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

package software.amazon.awssdk.core.sync;

import java.io.InputStream;

/**
 * Supplier of a request body's bytes, from the first byte, each time {@link #newStream()} is called. The SDK closes
 * each stream it obtains. A provider that can return only one stream must throw {@link IllegalStateException} on
 * later calls, so a retry fails rather than resending a partial body.
 *
 * <p>ADAPTED from the V2 {@code software.amazon.awssdk.http.ContentStreamProvider}: V3 {@code http-spi} depends on
 * {@code sdk-core}, so the caller-facing provider lives with {@link RequestBody}.
 */
@FunctionalInterface
public interface ContentStreamProvider {
    /** A new stream of the body from its first byte. */
    InputStream newStream();

    /** Diagnostic name of the provider. */
    default String name() {
        return "Unknown";
    }
}
