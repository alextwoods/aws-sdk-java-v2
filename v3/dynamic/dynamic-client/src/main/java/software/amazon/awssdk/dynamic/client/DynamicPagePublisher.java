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

package software.amazon.awssdk.dynamic.client;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PagePublisher;
import software.amazon.awssdk.dynamic.model.DynamicPagination;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The pages of a paginated dynamic operation, fetched on demand from the async client that created this publisher
 * (Requirement 14.15): the dynamic counterpart of a generated {@code <Operation>Publisher}, on the same sdk-core
 * {@link PagePublisher} runtime.
 *
 * <p>Each subscription calls for no page until downstream demand is positive, calls for one page per unit of demand
 * with at most one call in flight, never prefetches, follows the modeled output token as {@link DynamicPages} does,
 * and completes after the last page. A failed page call fails the subscription with its SDK exception. Each page
 * call is the engine's own call future, so cancelling the subscription cancels the in-flight call through the shared
 * pipeline (its exchange, body subscription or retry delay) and calls no further. Pages are signalled on the
 * client's completion executor.
 *
 * <p>Thread safe; each subscription starts again from the first page.
 */
public final class DynamicPagePublisher implements SdkPublisher<Document> {
    private final DynamicCalls calls;
    private final DynamicPaginators.Paged paged;
    private final AwsRequestOverrideConfiguration override;

    DynamicPagePublisher(DynamicCalls calls, DynamicPaginators.Paged paged,
                         AwsRequestOverrideConfiguration override) {
        this.calls = Objects.requireNonNull(calls, "calls");
        this.paged = Objects.requireNonNull(paged, "paged");
        this.override = override;
    }

    /** The operation's modeled pagination: input and output tokens, items and page size. */
    public DynamicPagination pagination() {
        return paged.pagination();
    }

    @Override
    public void subscribe(Subscriber<? super Document> subscriber) {
        PagePublisher.create(new AsyncPageFetcher<Document>() {
            @Override
            public boolean hasNextPage(Document previousPage) {
                return paged.hasNextPage(previousPage);
            }

            @Override
            @SuppressWarnings("unchecked")
            public CompletableFuture<Document> nextPage(Document previousPage) {
                // The engine's call future itself, so cancelling the subscription cancels the call.
                return (CompletableFuture<Document>) (CompletableFuture<?>) calls.engine().executeAsync(
                    paged.first().plan(), paged.input(previousPage), override, null);
            }
        }).subscribe(subscriber);
    }

    @Override
    public String toString() {
        return "DynamicPagePublisher[" + paged.pagination().operationName() + "]";
    }
}
