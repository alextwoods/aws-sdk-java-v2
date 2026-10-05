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

import java.util.Iterator;
import java.util.Objects;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;
import software.amazon.awssdk.dynamic.model.DynamicPagination;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The pages of a paginated dynamic operation, fetched lazily from the client that created this iterable
 * (Requirement 14.15): the dynamic counterpart of a generated {@code <Operation>Iterable}, on the same sdk-core
 * {@link SdkIterable#pages} runtime.
 *
 * <p>Each iterator calls the operation for the first page on its first pull, then for the next page with the
 * previous page's modeled output token, and stops when that token is absent or empty. Every page is one call on the
 * same client through the same plan and engine, with the same configuration and override; the iterable creates no
 * client. Pages are documents of the operation's output schema; a failed page call throws its SDK exception from the
 * pull.
 *
 * <p>Thread safe; each iterator is used by one thread.
 */
public final class DynamicPages implements SdkIterable<Document> {
    private final DynamicCalls calls;
    private final DynamicPaginators.Paged paged;
    private final AwsRequestOverrideConfiguration override;

    DynamicPages(DynamicCalls calls, DynamicPaginators.Paged paged, AwsRequestOverrideConfiguration override) {
        this.calls = Objects.requireNonNull(calls, "calls");
        this.paged = Objects.requireNonNull(paged, "paged");
        this.override = override;
    }

    /** The operation's modeled pagination: input and output tokens, items and page size. */
    public DynamicPagination pagination() {
        return paged.pagination();
    }

    @Override
    public Iterator<Document> iterator() {
        return SdkIterable.pages(new SyncPageFetcher<Document>() {
            @Override
            public boolean hasNextPage(Document previousPage) {
                return paged.hasNextPage(previousPage);
            }

            @Override
            public Document nextPage(Document previousPage) {
                return calls.engine().executeSync(paged.first().plan(), paged.input(previousPage), override, null);
            }
        }).iterator();
    }

    /**
     * The modeled items of every page, flattened lazily in page order: a page is fetched only when the previous
     * page's items are exhausted.
     *
     * @throws IllegalStateException if the operation's pagination models no items
     */
    public SdkIterable<Document> items() {
        if (paged.pagination().items().isEmpty()) {
            throw new IllegalStateException("Operation '" + paged.pagination().operationName()
                                            + "' models no paginated items");
        }
        return flatMapIterable(paged::items);
    }

    @Override
    public String toString() {
        return "DynamicPages[" + paged.pagination().operationName() + "]";
    }
}
