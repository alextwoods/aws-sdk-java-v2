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

package software.amazon.awssdk.pipeline;

import software.amazon.awssdk.core.context.ContextKey;

/**
 * The dense slot one {@link ContextKey} was assigned in one {@link ContextLayout}, returned by
 * {@link PipelinePlan.Builder#declare(ContextKey)} at plan-build time. Stages hold slots in plan fields and read and
 * write context values through them by array index, without a key lookup. A slot is valid only with contexts of the
 * layout it was assigned in; using it with another plan's context fails.
 *
 * @param <T> the value type
 */
public final class ContextSlot<T> {
    private final ContextKey<T> key;
    private final int index;
    private final Object layoutToken;

    ContextSlot(ContextKey<T> key, int index, Object layoutToken) {
        this.key = key;
        this.index = index;
        this.layoutToken = layoutToken;
    }

    /** The key this slot stores. */
    public ContextKey<T> key() {
        return key;
    }

    /** The slot's index within its scope's value array. */
    public int index() {
        return index;
    }

    Object layoutToken() {
        return layoutToken;
    }

    @Override
    public String toString() {
        return "ContextSlot(" + key.name() + ", " + key.scope() + " #" + index + ")";
    }
}
