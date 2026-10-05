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
 * A mutable builder of {@code T}.
 *
 * @param <B> the builder type
 * @param <T> the built type
 */
public interface SdkBuilder<B extends SdkBuilder<B, T>, T> extends Buildable {

    @Override
    T build();

    /** Applies {@code mutator} to this builder and returns it. */
    @SuppressWarnings("unchecked")
    default B applyMutation(Consumer<B> mutator) {
        mutator.accept((B) this);
        return (B) this;
    }
}
