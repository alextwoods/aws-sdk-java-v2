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

package software.amazon.awssdk.utils.builder;

import java.util.function.Consumer;

/**
 * An immutable object that can be converted to a builder initialized with its state.
 *
 * @param <B> the builder type
 * @param <T> this type
 */
public interface ToCopyableBuilder<B extends CopyableBuilder<B, T>, T extends ToCopyableBuilder<B, T>> {

    /** A new builder initialized with this object's state. */
    B toBuilder();

    /** A copy of this object with {@code modifier} applied to a builder of it. */
    default T copy(Consumer<? super B> modifier) {
        return toBuilder().applyMutation(modifier::accept).build();
    }
}
