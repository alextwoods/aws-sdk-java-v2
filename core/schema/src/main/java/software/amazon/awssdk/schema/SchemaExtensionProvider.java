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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * SPI that computes one kind of extension data for a {@link Schema}.
 *
 * <p>Implementations are discovered once through {@link java.util.ServiceLoader}, using the class loader of this
 * interface. Register them in {@code META-INF/services/software.amazon.awssdk.schema.SchemaExtensionProvider}. Each
 * provider declares a fixed {@link SchemaExtensionKey}; at most one provider may be registered per key, and discovery
 * fails with an {@link IllegalStateException} naming the key and both provider classes otherwise.
 *
 * <p>{@link Schema#getExtension(SchemaExtensionKey)} calls {@link #provide(Schema)} the first time a value is requested
 * for a schema instance and key, caches the result on that schema (including a {@code null} result) and returns the
 * cached instance from then on. If {@link #provide(Schema)} throws, the exception reaches the caller unchanged,
 * nothing is cached, and the next request calls the provider again.
 *
 * <p><b>Concurrency.</b> When several threads request a value that is not cached yet, each of them may call
 * {@link #provide(Schema)}. Only the first result to be published is cached, and every caller, including the threads
 * whose results were discarded, receives that one instance. Implementations must therefore be thread-safe and
 * side-effect free, and should return equivalent values for the same schema. Cached values are published safely, but
 * should still be immutable because every caller shares them.
 *
 * <p>{@link #provide(Schema)} must not depend on locks held by callers of {@link Schema#getExtension}. It may call
 * {@code getExtension} on other schemas, or on the same schema for another key.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.SchemaExtensionProvider}.
 *
 * @param <T> the type of extension data this provider produces
 */
@SdkPublicApi
public interface SchemaExtensionProvider<T> {
    /**
     * Returns the key under which this provider's extension data is stored. Must return the same key on every call.
     *
     * @return the extension key
     */
    SchemaExtensionKey<T> key();

    /**
     * Computes the extension value for a schema.
     *
     * @param schema the schema to compute extension data for
     * @return the extension value, or {@code null} if this provider has no data for the schema
     */
    T provide(Schema schema);
}
