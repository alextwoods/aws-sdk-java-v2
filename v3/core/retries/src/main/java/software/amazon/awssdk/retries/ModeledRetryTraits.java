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

package software.amazon.awssdk.retries;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.traits.RetryableTrait;

/**
 * The retry traits of a modeled error shape: Smithy {@code @retryable} and its {@code throttling} member
 * (Requirement 13.15). The default classifier combines them with the response metadata of the failed attempt.
 *
 * @param retryable whether the error shape carries {@code @retryable}
 * @param throttling whether {@code @retryable(throttling: true)}; implies {@code retryable}
 */
public record ModeledRetryTraits(boolean retryable, boolean throttling) {
    /** An error shape without retry traits, or an unmodeled failure. */
    public static final ModeledRetryTraits NONE = new ModeledRetryTraits(false, false);

    private static final ModeledRetryTraits RETRYABLE = new ModeledRetryTraits(true, false);
    private static final ModeledRetryTraits THROTTLING = new ModeledRetryTraits(true, true);

    public ModeledRetryTraits {
        if (throttling && !retryable) {
            throw new IllegalArgumentException("A throttling error is retryable.");
        }
    }

    /** The retry traits of the error shape {@code errorSchema}. */
    public static ModeledRetryTraits fromSchema(Schema errorSchema) {
        RetryableTrait retryable = Objects.requireNonNull(errorSchema, "errorSchema")
                                          .getTrait(TraitKey.RETRYABLE_TRAIT);
        if (retryable == null) {
            return NONE;
        }
        return retryable.getThrottling() ? THROTTLING : RETRYABLE;
    }

    /**
     * The retry traits of {@code failure} when it is a modeled error (a generated error type, which is a
     * {@link SerializableStruct} over its error Schema), otherwise {@link #NONE}.
     */
    public static ModeledRetryTraits of(Throwable failure) {
        if (failure instanceof SerializableStruct modeled) {
            Schema schema = modeled.schema();
            return schema == null ? NONE : fromSchema(schema);
        }
        return NONE;
    }
}
