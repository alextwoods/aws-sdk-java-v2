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
package software.amazon.awssdk.enhanced.dynamodb.internal.async;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Future composition that keeps cancellation connected to the service call. {@link CompletableFuture#thenApply}
 * does not propagate cancellation from the dependent future to its source, so cancelling a future returned by an
 * enhanced async API would otherwise leave the DynamoDB request (and its HTTP exchange) running.
 */
public final class EnhancedFutures {
    private EnhancedFutures() {
    }

    /**
     * {@code source.thenApply(mapper)}, except that cancelling the returned future also cancels {@code source}. A
     * failure of {@code source} or {@code mapper} completes the returned future exceptionally, as {@code thenApply}
     * does.
     */
    public static <T, U> CompletableFuture<U> map(CompletableFuture<T> source, Function<? super T, ? extends U> mapper) {
        CompletableFuture<U> result = source.thenApply(mapper);
        result.whenComplete((ignored, failure) -> {
            if (failure instanceof CancellationException) {
                source.cancel(true);
            }
        });
        return result;
    }
}
