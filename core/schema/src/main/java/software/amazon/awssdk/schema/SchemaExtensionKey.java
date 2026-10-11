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

package software.amazon.awssdk.schema;

import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A typed key that identifies one kind of extension data stored on {@link Schema} objects.
 *
 * <p>Each key has a unique, dense integer ID used for array-indexed access, so a lookup through
 * {@link Schema#getExtension(SchemaExtensionKey)} neither hashes nor allocates. Keys are compared by identity and
 * should be declared as {@code static final} fields. The value for a key is computed by the
 * {@link SchemaExtensionProvider} whose {@link SchemaExtensionProvider#key()} returns that key.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.SchemaExtensionKey}.
 *
 * @param <T> the type of extension data stored under this key
 */
@SdkPublicApi
public final class SchemaExtensionKey<T> {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    final int id;

    /**
     * Creates a key with the next free ID.
     */
    public SchemaExtensionKey() {
        this.id = NEXT_ID.getAndIncrement();
    }

    @Override
    public String toString() {
        return "SchemaExtensionKey{id=" + id + '}';
    }
}
