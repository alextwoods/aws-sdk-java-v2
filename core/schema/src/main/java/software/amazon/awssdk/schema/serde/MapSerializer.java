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

package software.amazon.awssdk.schema.serde;

import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.Schema;

/**
 * Writes the entries of a map shape. {@link ShapeSerializer#writeMap} passes an instance to its consumer.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.MapSerializer}.
 */
@SdkPublicApi
public interface MapSerializer {
    /**
     * Writes a string key, then calls {@code valueSerializer}, which must write exactly one value for the entry.
     *
     * @param keySchema the schema of the map key; the same schema should be given for every entry of a map
     * @param key the key to write
     * @param state the state to pass to {@code valueSerializer}
     * @param valueSerializer writes the entry's value
     * @param <T> the state type
     */
    <T> void writeEntry(Schema keySchema, String key, T state, BiConsumer<T, ShapeSerializer> valueSerializer);
}
