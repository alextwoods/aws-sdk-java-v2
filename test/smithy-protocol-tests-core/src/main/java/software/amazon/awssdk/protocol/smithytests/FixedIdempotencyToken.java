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

package software.amazon.awssdk.protocol.smithytests;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Fixes the idempotency token that clients generate for unset {@code @idempotencyToken} members, through the
 * test-only {@link IdempotentUtils#setGenerator} hook, until {@link #close()} restores the previous generator.
 *
 * <pre>{@code
 * try (FixedIdempotencyToken token = FixedIdempotencyToken.install()) {
 *     client.createThing(input);     // the token member is serialized as FixedIdempotencyToken.TOKEN
 * }
 * }</pre>
 *
 * <p>The generator is JVM-global: tests that install a fixed token must not run in parallel with tests that expect
 * random tokens.
 */
public final class FixedIdempotencyToken implements SdkAutoCloseable {
    /**
     * The fixed token. The Smithy protocol test cases expect this value, and the protocol test generator installs the
     * same value in its request test classes.
     */
    public static final String TOKEN = "00000000-0000-4000-8000-000000000000";

    private static final Supplier<String> FIXED = () -> TOKEN;

    private final Supplier<String> previous;
    private final AtomicBoolean closed = new AtomicBoolean();

    private FixedIdempotencyToken(Supplier<String> previous) {
        this.previous = previous;
    }

    /**
     * Makes {@link IdempotentUtils#getGenerator()} return {@link #TOKEN} until the returned object is closed.
     */
    public static FixedIdempotencyToken install() {
        FixedIdempotencyToken installed = new FixedIdempotencyToken(IdempotentUtils.getGenerator());
        IdempotentUtils.setGenerator(FIXED);
        return installed;
    }

    /**
     * Restores the generator that was in place when {@link #install()} was called. Later calls do nothing.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            IdempotentUtils.setGenerator(previous);
        }
    }
}
